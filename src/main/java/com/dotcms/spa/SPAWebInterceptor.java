package com.dotcms.spa;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;


public class SPAWebInterceptor implements WebInterceptor {

    public  static final String      PROXY_EDIT_MODE_URL_VAR = "proxyEditModeUrl";
    private static final String      API_CALL                = "/api/v1/page/render";
    private static final ProxyTool   proxy                   = new ProxyTool();
    private final        LanguageAPI languageAPI             = APILocator.getLanguageAPI();

    @Override
    public String[] getFilters() {
        return new String[] {
                API_CALL + "*"
        };
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final PageMode mode             = PageMode.get(request);
        final Optional<String> proxyUrl = this.proxyUrl(request);

        if (!proxyUrl.isPresent() || mode == PageMode.LIVE) {
            return Result.NEXT;
        }

        System.err.println("GOT AN SPA Call -->" + request.getRequestURI());

        return new Result.Builder().wrap(new MockHttpCaptureResponse(response)).next().build();
    }


    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {

        try {

            if (response instanceof MockHttpCaptureResponse) {

                final Optional<String> proxyUrl            = proxyUrl(request);
                final MockHttpCaptureResponse mockResponse = (MockHttpCaptureResponse)response;
                final String postJson                      = new String(mockResponse.getBytes());
                final JSONObject json                      = new JSONObject(postJson);
                final Map<String, String> params           = ImmutableMap.of("dotPageData", postJson);

                String responseStr = null;
                final ProxyResponse pResponse = proxy.sendPost(proxyUrl.get(), params);

                if (pResponse.getResponseCode() == 200) {
                    responseStr = new String(pResponse.getResponse());
                }

                json.getJSONObject("entity").getJSONObject("page").put("rendered", responseStr);

                response.setContentType("application/json");

                response.getWriter().write(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void destroy() {
        Logger.info(this, "destroy:" + this.getClass().getName());
    }

    private Optional<String> proxyUrl(final HttpServletRequest request) {

        final String uri = request.getRequestURI();

        if (!uri.startsWith(API_CALL)) {
            return Optional.empty();
        }

        final Host host       = this.getCurrentHost(request);
        final String proxyUrl = host.getStringProperty(PROXY_EDIT_MODE_URL_VAR);

        return Optional.ofNullable(proxyUrl);
    }

    @CloseDBIfOpened
    public Host getCurrentHost(final HttpServletRequest request) {

        try {

            Host host = null;
            final HttpSession session   = request.getSession(false);
            final UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
            final User systemUser       = APILocator.systemUser();
            final PageMode mode         = PageMode.get(request);
            final boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

            final String pageHostId = request.getParameter("host_id");
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
                    final String serverName = request.getServerName();
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
