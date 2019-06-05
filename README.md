# Edit Mode Anywhere - EMA

This plugin intercepts EDIT_MODE API calls in the backend of dotCMS and proxies them to another site/server (hosted elsewhere) to render the EDIT_MODE request.  To enable Edit Mode Anywhere, your app should:

1. Be built to read and deliver dotCMS Page and content as a Service information for specific pages/routes 
2. Be able to do a static/isomorphic rendering of your app at a given page route 
3. Be able to accept POSTs to handle and render the Page as a Service JSON information coming from dotCMS EDIT MODE
4. When accepting these POSTs, add the additional markup dotCMS requires to enable edit mode.  

### How it works
When dotCMS receives an edit mode request for a page on a site with EMA support, dotCMS will `POST` all page data including the template, containers, layout and contents on the page to your EMA site (example payload found at the endpoint `/api/v1/page/json/{path}`).  dotCMS `POSTs` this payload to your EMA renderer as `application/x-www-form-urlencoded` with the page data json in the parameter `dotPageData`.  The remote application or SPA needs to be built to accept this POSTed data, parse the `dotPageData` param and use it to statically render the App server-side in that state our route.  dotCMS will read this rendered state/html and return it to edit mode.

### Build and install the plugin
The EMA plugin is an OSGI based plugin.  To install this plugin all you need to do is build the JAR. To do this, clone this repo and run: `./gradlew jar`.  This will build two jars in the `./build/libs` directory: a bundle fragment (needed to expose 3rd party libraries to dotCMS) and the plugin jar.   Once built, copy the bundle jar files found in the `./build/libs` to your dotCMS's Felix load folder, found under `/dotserver/tomcat-x.xx/webapps/ROOT/WEB-INF/felix/load` or easier, just upload the bundle jars files using the dotCMS UI `> Developer Tools > Dynamic Plugins > Upload Plugin`.

### How to enable
Once installed, dotCMS EMA suport is enabled at the host level.  When the EMA plugin was installed, it adds a text field to your Site content type called "Proxy Edit Mode URL" with a variable name : `proxyEditModeUrl`.  Go to `> System > Sites` and click on the Site you want to enable EMA on. Find the "Proxy Edit Mode URL" field and add the full url, e.g. https://your-react-app.com:8443/editMode to your rendering server and endpoint that dotCMS will be proxying the request to, including the port.  If this value is not set, EMA support is disabled for the host.  If you are using SSL (and you should be), make sure that the certificate on the rendering server is valid or that the java install you are using to run dotCMS has the rendering server's cert installed and trusted in the java keystore.

### Payload in EMA
```json
{
	"layout": {
		"pageWidth": null,
		"width": null,
		"layout": null,
		"title": "anonymouslayout1543451733724",
		"header": true,
		"footer": true,
		"body": {
			"rows": [{
				"columns": [{
					"containers": [{
						"identifier": "56bd55ea-b04b-480d-9e37-5d6f9217dcc3",
						"uuid": "1"
					}],
					"widthPercent": 50,
					"leftOffset": 1,
					"preview": false,
					"width": 6,
					"left": 0
				}, {
					"containers": [{
						"identifier": "56bd55ea-b04b-480d-9e37-5d6f9217dcc3",
						"uuid": "2"
					}],
					"widthPercent": 50,
					"leftOffset": 7,
					"preview": false,
					"width": 6,
					"left": 6
				}],
				"identifier": 0,
				"value": null,
				"id": null
			}]
		},
		"sidebar": {
			"containers": [],
			"location": "",
			"width": "small",
			"widthPercent": 20,
			"preview": false
		}
	},
	"containers": {
		"56bd55ea-b04b-480d-9e37-5d6f9217dcc3": {
			"container": {
				"permissionId": "56bd55ea-b04b-480d-9e37-5d6f9217dcc3",
				"modDate": "2018-04-06 13:52:54.0",
				"code": "",
				"notes": "    Large Column:\r\n    - Blog\r\n    - Events\r\n    - Generic\r\n    - Location\r\n    - Media\r\n    - News\r\n    - Documents\r\n    - Products",
				"canAdd": "CONTENT,WIDGET,FORM",
				"luceneQuery": "",
				"title": "Large Column (lg-1)",
				"type": "containers",
				"showOnMenu": "false",
				"inode": "e58e92b3-7135-461b-b56b-04ff143a389b",
				"archived": "false",
				"preLoop": "<div class=\"large-column\">",
				"working": "true",
				"locked": "true",
				"friendlyName": "Large body column container",
				"live": "true",
				"iDate": "2018-04-06 13:52:54.0",
				"owner": null,
				"useDiv": "false",
				"versionType": "containers",
				"identifier": "56bd55ea-b04b-480d-9e37-5d6f9217dcc3",
				"new": "false",
				"permissionType": "com.dotmarketing.portlets.containers.model.Container",
				"staticify": "false",
				"canManageContainer": true,
				"sortContentletsBy": "",
				"maxContentlets": "25",
				"acceptTypes": "WIDGET,FORM,Document,Blog,webPageContent,Products,News,Media,calendarEvent,Location",
				"IDate": "2018-04-06 13:52:54.0",
				"versionId": "56bd55ea-b04b-480d-9e37-5d6f9217dcc3",
				"deleted": "false",
				"parentPermissionable": "false",
				"modUser": "dotcms.org.1",
				"sortOrder": "0",
				"name": "Large Column (lg-1)",
				"categoryId": "e58e92b3-7135-461b-b56b-04ff143a389b",
				"postLoop": "</div>"
			},
			"uuids": {
				"uuid-1": {
					"contentlets": [{
						"owner": "dotcms.org.1",
						"identifier": "735c44a5-3e6b-4047-a8c3-8ea9146e1762",
						"nullProperties": [],
						"modDate": 1532523929000,
						"canEdit": true,
						"languageId": 1,
						"title": "Bear Mountain Lodge Description",
						"body": "<p>Bear Mountain in Colorado offers the finest skiing conditions in the world. We call it Private Powder and it's here exclusively for our Members, their families, clients and invited guests.</p>\n<p>Arguably, the most exclusive ski resort in the world, offering something you can't get anywhere else. You can be super-rich and super-safe all at the same time. It's the only place in the world where you can ski without bodyguards. That's not to say there aren't any here; resort security is handled by former US Secret Service Agents. While most resorts have 'mountain huts' we have 'Sugar Shacks', where you can tuck in to homemade sweet treats and gourmet coffee.</p>\n<p>Apres-ski: Take a private snow cat dinner excursion to Timberline Lodge or combine dinner with a spot of night skiing at Rainbow Lodge.</p>\n<hr />\n<h3>Getting Here</h3>\n<div class=\"row\">\n<div class=\"col-md-5 d-none d-md-block d-lg-block d-xl-block\">\n<div style=\"text-align: center;\"><img class=\"img-fluid\" src=\"/bear-mountain/images/airport.jpg\" /></div>\n</div>\n<div class=\"col-md-7 col-sm-12\"><address><strong>Bear Lodge Private Airport</strong><br /> 2300 Claw Circle<br />Bear Moutain, CO 80487<br /> <abbr title=\"Phone\">P:</abbr> 615.620.2050</address></div>\n</div>",
						"baseType": "CONTENT",
						"inode": "2587dd81-fe55-4ac1-82c6-35706dd665fb",
						"folder": "SYSTEM_FOLDER",
						"__DOTNAME__": "Bear Mountain Lodge Description",
						"disabledWYSIWYG": [],
						"sortOrder": 0,
						"modUser": "dotcms.org.1",
						"host": "48190c8c-42c4-46af-8d1a-0cd5db894797",
						"lastReview": 1532523929000,
						"stInode": "2a3e91e4-fbbf-4876-8c5b-2233c1739b05"
					}]
				},
				"uuid-2": {
					"contentlets": [{
						"owner": "dotcms.org.1",
						"identifier": "767509b1-2392-4661-a16b-e0e31ce27719",
						"nullProperties": [],
						"modDate": 1532452228000,
						"canEdit": true,
						"languageId": 1,
						"title": "About Quest",
						"body": "<h2>About Us</h2>\n<p><img src=\"/dA/7de092d3-d051/300w/custom-house.jpg\" class=\"img-fluid\" width=\"300\" height=\"177\" style=\"float: right; margin: 10px;\" /></p>\n<p>Neque sit amet fermentum vulputate, arcu augue eleifend diam, malesuada molestie quam nibh at neque. In non risus at felis adipiscing molestie ac sed diam. Vivamus sit amet purus at libero pellentesque sagittis. Integer a enim turpis, vitae dignissim dui. Nulla eu leo id sapien facilisis pulvinar non quis justo. Morbi tempor, est quis elementum euismod, nibh metus faucibus enim, a viverra mi massa sit amet dui. Aenean id sapien mi, vel dapibus enim. Duis diam erat, malesuada sed fringilla non, rhoncus eget mauris. Praesent sit amet orci purus. Mauris hendrerit lectus ut justo aliquam eleifend. Curabitur bibendum congue luctus.</p>\n<p>Nulla rutrum facilisis odio sed interdum. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Phasellus risus libero, cursus nec iaculis eget, pretium et augue. Proin ultricies dapibus elit et ornare. Phasellus feugiat suscipit leo. Morbi eu mi volutpat quam aliquam fringilla vitae vitae libero. Duis convallis dapibus molestie. In egestas lorem vitae eros varius adipiscing. &mdash;&nbsp;Timothy Brigham, CEO Quest Financial</p>\n<p></p>",
						"baseType": "CONTENT",
						"inode": "f6406747-0220-41fb-86e4-32bce21a8822",
						"folder": "SYSTEM_FOLDER",
						"__DOTNAME__": "About Quest",
						"disabledWYSIWYG": [],
						"sortOrder": 0,
						"modUser": "dotcms.org.1",
						"host": "48190c8c-42c4-46af-8d1a-0cd5db894797",
						"lastReview": 1532028912000,
						"stInode": "2a3e91e4-fbbf-4876-8c5b-2233c1739b05"
					}]
				}
			}
		}
	},
	"page": {
		"template": "0d0a8c6b-bcf9-4f16-bbda-c7f22c24fcbf",
		"modDate": 1543451770000,
		"extension": "page",
		"cachettl": "15",
		"contentEditable": false,
		"pageURI": "/pages/demo-page",
		"description": "Demo",
		"mimeType": "application/dotpage",
		"title": "Demo",
		"type": "htmlpage",
		"httpsRequired": false,
		"inode": "7ae34848-a606-499d-91eb-22072a5b4f5d",
		"disabledWYSIWYG": [],
		"permissions": [1, 2, 4, 8, 16],
		"countryCode": "US",
		"isLocked": false,
		"host": "75a7e2c2-d5e2-481f-81f0-ac9a6e551de9",
		"lastReview": 1543443163000,
		"working": true,
		"locked": false,
		"stInode": "c541abb1-69b3-4bc5-8430-5e09e5239cc8",
		"friendlyName": "Demo",
		"live": true,
		"owner": "dotcms.org.1",
		"identifier": "60da4d73-a7b0-44c2-a5d1-482705704628",
		"nullProperties": [],
		"friendlyname": "Demo",
		"isContentlet": true,
		"wfActionMapList": [{
			"name": "Unpublish",
			"icon": "workflowIcon",
			"hasPushPublishActionlet": false,
			"id": "38efc763-d78f-4e4b-b092-59cd8c579b93",
			"requiresCheckout": false,
			"wfActionNameStr": "Unpublish",
			"commentable": false,
			"assignable": false
		}, {
			"name": "Copy",
			"icon": "workflowIcon",
			"hasPushPublishActionlet": false,
			"id": "134a50d3-782d-43de-8877-42c0be1c86a4",
			"requiresCheckout": false,
			"wfActionNameStr": "Copy",
			"commentable": false,
			"assignable": false
		}],
		"languageId": 1,
		"statusIcons": "<span class='greyDotIcon' style='opacity:.4'></span><span class='liveIcon'></span>",
		"languageCode": "en",
		"url": "demo-page",
		"languageFlag": "en_US",
		"modUserName": "Admin User",
		"hasLiveVersion": true,
		"folder": "f6282c7a-484c-4733-a8ab-1f3a3ce1b5d6",
		"deleted": false,
		"sortOrder": 0,
		"modUser": "dotcms.org.1",
		"name": "demo-page",
		"pageUrl": "demo-page"
	}
}
```




