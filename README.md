# dotCMS Edit Mode Anywhere - EMA
This plugin enables content management for SPAs (Single Page Apps) or remote apps (.NET, php) that have been built to pull content from dotCMS.  The plugin intercepts a user's dotCMS EDIT_MODE API calls in the backend of dotCMS, POSTs the dotCMS page API data to another site/server (hosted elsewhere) and then proxies the remote response back to the dotCMS user, which allows dotCMS to render the EDIT_MODE request.  To enable Edit Mode Anywhere, your app should:

1. Be built to read and deliver dotCMS Page and content as a Service information for specific pages/routes 
2. Be able to do a static/isomorphic rendering of your app at a given page route 
3. Be able to accept POSTs, which can  handle and render the Page as a Service JSON information coming from dotCMS EDIT MODE
4. When accepting these POSTs, add the additional markup dotCMS requires to enable edit mode.  

### How it works
When dotCMS receives an edit mode request for a page on a site with EMA support, dotCMS will `POST` the "Page as a Service" data including the template, containers, layout and contents on the page to your EMA site (example payload found at the endpoint `/api/v1/page/json/{path}`).  dotCMS `POSTs` this payload to your EMA renderer with the content type  of `application/x-www-form-urlencoded` and the "Page as a Service" data json in the parameter `dotPageData`.  The remote application or SPA needs to be built to accept this POSTed data, parse the `dotPageData` param and use it to statically render your App server-side in that state our route.  dotCMS will read this rendered state/html and return it to edit mode.

## Routes
dotCMS EMA supports two kinds of routes -  page routes and slug routes.  This is because the page API supports both page urls and slug urls, and when a link is followed in the dotCMS SPA, the page API data is just requested based on the url being managed . Both these are valid API requests for page data
```https://demo.dotcms.com/api/v1/page/json/about-us/index```
and
```https://demo.dotcms.com/api/v1/page/json/blogs/your-blog-slug-url-mapped-title```

### Slug Content
When a slug route is requested dotCMS will include the slug content for the route in a property called `urlContentMap`, which  contains the entire slug mapped content for that route.

### Adding/Removing Routes
To add a new route for an SPA in dotCMS all you need to do is add a page or a new Slug based content.  The new route will automatically be included in calls to the Navigation API and or the content apis which dictation valid routes for your application.  Unpublishing/deleting a route will automatically remove the route from your SPA/app

> Note: While page routes can have distinct, manageable content and layouts, slug routes generally cannot. Changes made to a page routes' layouts/content will only affect the page being edited.  This is different for slug based routes.  Because all slug content of a type shares the same detail page, changes made to the slug detail page/layout will affect all the slug routes for that specific content type.  

## Edit Mode JS Events
dotCMS expects certian events to fire actions while in Edit Mode.  These are important if you want to build a custom application or SPA that supports Edit Mode Anywhere.  These include:

1. Internal User Navigation
This forces a page refresh in your APP and should fire when a user clicks an internal link in your SPA 

2. Reorder Navigation
This opens the reorder nav modal and should fire when an editing user clicks the "reorder nav" link

3. Edit Contentlet
This opens a contentlet up for editing in a modal editor window.

4. ...TBD

## EMA Limitations
Current implementaion requires an authoring environment that is capible of handling the POSTed Page api data.  Our current dotCMS SPA implementaion requires server side / isomorphic rendering to be installed in the authoring environment. This server side implementation accpets the posted data and wraps the content objects in the SPA with specific custom components that add attributes dotCMS looks for when rendering a page in edit mode.  Some version of this would likely be a requirement for any 3rd party SPAs or apps looking to support EMA.


#Installation

### Build and install the plugin
The EMA plugin is an OSGI based plugin.  To install this plugin all you need to do is build the JAR. To do this, clone this repo, `git clone https://github.com/dotcms-plugins/com.dotcms.ema` cd into the cloned directory and run: `./gradlew jar`.  This will build two jars in the `./build/libs` directory: 
1. a bundle fragment (needed to expose 3rd party libraries to dotCMS) and 
2. the plugin jar.   
Once built, copy the bundle jar files found in the `./build/libs` to your dotCMS's Felix load folder, found under `/dotserver/tomcat-x.xx/webapps/ROOT/WEB-INF/felix/load` or easier, just upload the bundle jars files using the dotCMS UI `> Developer Tools > Dynamic Plugins > Upload Plugin`.

### How to enable
Once installed, dotCMS EMA suport is enabled at the host level.  When the EMA plugin was installed, it adds a text field to your Site content type called "Proxy Edit Mode URL" with a variable name : `proxyEditModeUrl`.  Go to `> System > Sites` and click on the Site you want to enable EMA on. Find the "Proxy Edit Mode URL" field and add the full url, e.g. https://your-react-app.com:8443/editMode to your rendering server and endpoint that dotCMS will be proxying the request to, including the port.  If this value is not set, EMA support is disabled for the host.  If you are using SSL (and you should be), make sure that the certificate on the rendering server is valid or that the java install you are using to run dotCMS has the rendering server's cert installed and trusted in the java keystore.
