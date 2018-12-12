package com.dotcms.spa;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.spa.proxy.ProxyResponse;
import com.dotcms.spa.proxy.ProxyTool;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

public class SPAFilter implements Filter {

    public static final String PROXY_EDIT_MODE_URL_VAR = "proxyEditModeUrl";

    private static final ProxyTool proxy = new ProxyTool();
    LanguageAPI lapi = APILocator.getLanguageAPI();

    final String API_CALL = "/api/v1/page/render";

    @Override
    public void init(FilterConfig config) throws ServletException {


    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        PageMode mode = PageMode.get(request);

        Optional<String> proxyUrl = proxyUrl(request);
        if (!proxyUrl.isPresent() || mode == PageMode.LIVE) {
            chain.doFilter(req, res);
            return;
        }
        System.err.println("GOT AN SPA Call -->" + request.getRequestURI());

        try {

            MockHttpCaptureResponse mockResponse = new MockHttpCaptureResponse(response);

            chain.doFilter(req, mockResponse);

            String postJson = new String(mockResponse.getBytes());

            JSONObject json = new JSONObject(postJson);

            Map<String, String> params = ImmutableMap.of("dotPageData", postJson);
            String responseStr = null;
            ProxyResponse pResponse = proxy.sendPost(proxyUrl.get(), params);
            if (pResponse.getResponseCode() == 200) {
                responseStr = new String(pResponse.getResponse());
            }

            json.getJSONObject("entity").getJSONObject("page").put("rendered", responseStr);

            response.setContentType("application/json");

            response.getWriter().write(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void destroy() {
        Logger.info(SPAFilter.class.getName(), "destroy:" + this.getClass().getName());
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
                        host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
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
