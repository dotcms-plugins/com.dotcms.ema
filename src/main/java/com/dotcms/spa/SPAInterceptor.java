package com.dotcms.spa;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.compress.utils.Lists;

import com.beust.jcommander.internal.Maps;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.spa.page.JsonMapper;
import com.dotcms.spa.page.SPAResourceAPI;
import com.dotcms.spa.proxy.ProxyResponse;
import com.dotcms.spa.proxy.ProxyTool;
import com.dotcms.spa.serializer.HTMLPageAssetRenderedSerializer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;

import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;

/**
 * This example just receives the userid on a query string parameter an do the autologin based on
 * it.
 */
public class SPAInterceptor implements WebInterceptor {

    public static final String PROXY_EDIT_MODE_URL_VAR="proxyEditModeUrl";
    private static final long serialVersionUID = -5760933171181804395L;
    private static final ProxyTool proxy = new ProxyTool();
    LanguageAPI lapi = APILocator.getLanguageAPI();

    final String API_CALL = "/api/v1/page/render";


    String[] filters = new String[] {"/api"};

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {


        User user;
        try {
            user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
        PageMode mode = PageMode.get(request);
        String uri = request.getRequestURI();

        Optional<String> proxyUrl = proxyUrl(request);
        if (!proxyUrl.isPresent() || mode == PageMode.LIVE) {
            return Result.NEXT;
        }
        System.err.println("GOT ONE -->" + request.getRequestURI());


        try {

            response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, " + "Content-Type, " + "Accept, Authorization");

            uri = uri.replace(API_CALL, "");

            String postJson = new SPAResourceAPI().pageAsJson(request, response, uri);


            Map<String, String> params = ImmutableMap.of("dotPageData", postJson);
            String responseStr = null;
            ProxyResponse pResponse = proxy.sendPost(proxyUrl.get(), params);
            if (pResponse.getResponseCode() == 200) {
                responseStr = new String(pResponse.getResponse());
            }
            final HTMLPageAssetRendered pageRendered =
                    (HTMLPageAssetRendered) APILocator.getHTMLPageAssetRenderedAPI().getPageRendered(request, response, user, uri, mode);


            HTMLPageAssetRendered pageView = new HTMLPageAssetRendered(pageRendered.getSite(), pageRendered.getTemplate(), pageRendered.getContainers(),
                    pageRendered.getPageInfo(), pageRendered.getLayout(), responseStr, pageRendered.isCanCreateTemplate(),
                    pageRendered.isCanEditTemplate(), pageRendered.getViewAs());
            HTMLPageAssetRenderedSerializer serializer = new HTMLPageAssetRenderedSerializer();


            Map<String, Object> newMap = injectContentsIntoPageView(pageView, user);


            ResponseEntityView view = new ResponseEntityView(newMap);
            response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, " + "Content-Type, " + "Accept, Authorization");

            response.setContentType("application/json");
            ObjectMapper mapper = JsonMapper.mapper;
       
            
            if (request.getParameter("showDotPageData") != null) {
                response.getWriter().write(postJson);
            } else {
                response.getWriter().write(mapper.writeValueAsString(view));
            }


            return Result.SKIP_NO_CHAIN;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return Result.SKIP_NO_CHAIN;

    } // intercept

    @Override
    public String[] getFilters() {

        return filters;
    }


    private Optional<String> proxyUrl(HttpServletRequest request) {


        final String uri = request.getRequestURI();

        if (!uri.startsWith(API_CALL)) {
            return Optional.empty();
        }


        Host host = this.getCurrentHost(request);
        String proxyUrl = host.getStringProperty(PROXY_EDIT_MODE_URL_VAR);


        return Optional.ofNullable(proxyUrl);


    }


    @CloseDBIfOpened
    public Host getCurrentHost(HttpServletRequest request) {
        try {
            Host host = null;
            HttpSession session = request.getSession(false);
            UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
            User systemUser = APILocator.systemUser();
            boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

            PageMode mode = PageMode.get(request);

            String pageHostId = request.getParameter("host_id");
            if (pageHostId != null && mode.isAdmin) {
                host = APILocator.getHostAPI().find(pageHostId, systemUser, respectFrontendRoles);
            } else {
                if (session != null && session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID) != null && mode.isAdmin) {
                    host = APILocator.getHostAPI().find((String) session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID), systemUser, false);
                } else if (session != null && mode.isAdmin && session.getAttribute(WebKeys.CURRENT_HOST) != null) {
                    host = (Host) session.getAttribute(WebKeys.CURRENT_HOST);
                } else if (request.getAttribute(WebKeys.CURRENT_HOST) != null) {
                    host = (Host) request.getAttribute(WebKeys.CURRENT_HOST);
                } else {
                    String serverName = request.getServerName();
                    if (UtilMethods.isSet(serverName)) {
                        host = APILocator.getHostAPI().find(serverName, systemUser, respectFrontendRoles);
                    }
                }
            }

            request.setAttribute(WebKeys.CURRENT_HOST, host);
            if (session != null && mode.isAdmin) {
                session.setAttribute(WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
                session.setAttribute(WebKeys.CURRENT_HOST, host);
            }
            return host;
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }
    
    
    public Map<String, Object> injectContentsIntoPageView(HTMLPageAssetRendered pageView, final User user)
            throws DotDataException, DotSecurityException {


        HTMLPageAssetRenderedSerializer serializer = new HTMLPageAssetRenderedSerializer();


        Map<String, Object> newMap = serializer.getObjectMap(pageView);


        final Table<String, String, Set<String>> pageContents = APILocator.getMultiTreeAPI()
                .getPageMultiTrees(pageView.getPageInfo().getPage(), pageView.getViewAs().getPageMode().showLive);

        if (!pageContents.isEmpty()) {

            for (final String containerId : pageContents.rowKeySet()) {
                Map<String, Object> uuidsRaw = Maps.newHashMap();
                for (final String uniqueId : pageContents.row(containerId).keySet()) {
                    final Set<String> cons = pageContents.get(containerId, uniqueId);

                    Map<String, Object> uuidRaw = Maps.newHashMap();

                    List<Contentlet> contentlets = cons.stream().map(conId -> {
                        try {
                            return APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(conId);
                        } catch (Exception e) {
                            throw new DotStateException(e);
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toList());

                    List<Map<String, Object>> maps = Lists.newArrayList();
                    for (Contentlet c : contentlets) {
                        Map<String, Object> map = Maps.newHashMap();
                        map.putAll(c.getMap());
                        map.put("baseType", c.getContentType().baseType().name());
                        map.put("canEdit", APILocator.getPermissionAPI().doesUserHavePermission(c, PermissionAPI.PERMISSION_EDIT, user));
                        maps.add(map);
                    }


                    // uuidRaw.put("contentlets", maps);

                    uuidsRaw.put("uuid-" + uniqueId, maps);
                }
                Map<String, Object> map =(Map<String, Object>)newMap.get("containers");
                map =(Map<String, Object>)map.get(containerId);
                map.put("raw", uuidsRaw);
                
                //(Map<String, Object>newMap.get("containers")).get(containerId).
                
                
            }
        }


        return newMap;
    }

}
