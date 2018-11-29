package com.dotcms.spa;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.spa.page.JsonMapper;
import com.dotcms.spa.page.SPAResourceAPI;
import com.dotcms.spa.proxy.ProxyResponse;
import com.dotcms.spa.proxy.ProxyTool;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedSerializer;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

/**
 * This example just receives the userid on a query string parameter an do the autologin based on
 * it.
 */
public class SPAInterceptor implements WebInterceptor {


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
        if (!proxyUrl.isPresent()) {
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


            PageView pageView = new HTMLPageAssetRendered(pageRendered.getSite(), pageRendered.getTemplate(), pageRendered.getContainers(),
                    pageRendered.getPageInfo(), pageRendered.getLayout(), responseStr, pageRendered.isCanCreateTemplate(),
                    pageRendered.isCanEditTemplate(), pageRendered.getViewAs());
            HTMLPageAssetRenderedSerializer serializer = new HTMLPageAssetRenderedSerializer();

            Method getObjectMapMethod = HTMLPageAssetRenderedSerializer.class.getDeclaredMethod("getObjectMap", PageView.class);
            getObjectMapMethod.setAccessible(true);
            Map<String, Object> newMap = (Map<String, Object>) getObjectMapMethod.invoke(serializer, pageView);


            ResponseEntityView view = new ResponseEntityView(newMap);
            response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, " + "Content-Type, " + "Accept, Authorization");

            response.setContentType("application/json");
            ObjectMapper mapper = JsonMapper.mapper;

            response.getWriter().write(mapper.writeValueAsString(view));

            return Result.SKIP_NO_CHAIN;
        } catch (Exception e) {

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
        String proxyUrl = host.getStringProperty("proxyEditModeUrl");


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

}
