package com.dotcms.spa.page;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.context.Context;

import com.beust.jcommander.internal.Maps;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rendering.velocity.services.PageContextBuilder;
import com.dotcms.rendering.velocity.viewtools.ContainerWebAPI;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedSerializer;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;

public class SPAResourceAPI {
    private final UserWebAPI userAPI;
    private final TemplateAPI templateAPI;
    private final ContainerAPI containerAPI;
    private final PermissionAPI permissionAPI;
    private final ContentletAPI contentletAPI;
    private final LayoutAPI layoutAPI;
    private final VersionableAPI versionableAPI;
    private final LanguageAPI languageAPI;
    private final ContentTypeAPI cTypeApi;

    public SPAResourceAPI() {
        userAPI = WebAPILocator.getUserWebAPI();
        templateAPI = APILocator.getTemplateAPI();
        containerAPI = APILocator.getContainerAPI();
        permissionAPI = APILocator.getPermissionAPI();
        contentletAPI = APILocator.getContentletAPI();
        layoutAPI = APILocator.getLayoutAPI();
        versionableAPI = APILocator.getVersionableAPI();
        languageAPI = APILocator.getLanguageAPI();
        cTypeApi = APILocator.getContentTypeAPI(APILocator.systemUser());
    }

    public String pageAsJson(final HttpServletRequest request, final HttpServletResponse response, final String uri) throws Exception {

        ObjectMapper mapper = com.dotcms.spa.page.JsonMapper.mapper;


        return mapper.writeValueAsString(pageAsMap(request, response, uri));
    }

    public Map<String, Object> pageAsMap(final HttpServletRequest request, final HttpServletResponse response, final String uri)
            throws Exception {

        final PageMode pageMode = (request.getParameter(WebKeys.PAGE_MODE_PARAMETER) != null)
                ? PageMode.get(request.getParameter(WebKeys.PAGE_MODE_PARAMETER))
                : PageMode.get(request);


        final Language language =
                (request.getParameter("language_id") != null) ? languageAPI.getLanguage(request.getParameter("language_id"))
                        : WebAPILocator.getLanguageWebAPI().getLanguage(request);


        final User user = userAPI.getLoggedInUser(request);
        final Map<String, Object> holder = Maps.newHashMap();

        try {


            final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            final Identifier id = APILocator.getIdentifierAPI().find(host, uri);

            final HTMLPageAsset page = (HTMLPageAsset) APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(id.getId(),
                    language.getId(), pageMode.showLive, user, false);

            final Template template =
                    (pageMode.showLive) ? (Template) APILocator.getVersionableAPI().findLiveVersion(page.getTemplateId(), user, true)
                            : (Template) APILocator.getVersionableAPI().findWorkingVersion(page.getTemplateId(), user, true);

            final TemplateLayout layout = template != null && template.isDrawed() && !LicenseManager.getInstance().isCommunity()
                    ? DotTemplateTool.themeLayout(template.getInode())
                    : null;

            final Table<String, String, Set<String>> pageContents = APILocator.getMultiTreeAPI().getPageMultiTrees(page, pageMode.showLive);

            final Context velocityContext =
                    new PageContextBuilder(page, APILocator.systemUser(), pageMode).addAll(VelocityUtil.getWebContext(request, response));
            final Collection<ContainerRendered> containersRendered =
                    new ContainerRenderedBuilder().getContainersRendered(template, velocityContext, pageMode);

            Map<String, Object> containers = Maps.newHashMap();

            for (final String containerId : pageContents.rowKeySet()) {
                Map<String, Object> containerHolder = Maps.newHashMap();
                Container containerObj = (pageMode.showLive)
                        ? APILocator.getContainerAPI().getLiveContainerById(containerId, user, pageMode.respectAnonPerms)
                        : APILocator.getContainerAPI().getWorkingContainerById(containerId, user, pageMode.respectAnonPerms);

                if (containerObj == null) {
                    continue;
                }


                Map<String, Object> container = Maps.newHashMap();
                container.putAll(BeanUtils.describe(containerObj));;
                container.remove("map");

                ContainerWebAPI cwapi = new ContainerWebAPI();
                cwapi.init(VelocityUtil.getInstance().getContext(request, response));
                container.put("canAdd", cwapi.getBaseContentTypeUserHasPermissionToAdd(containerObj.getInode()));


                if (Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true)) {
                    container.put("canManageContainer", true);
                } else {
                    container.put("canManageContainer", permissionAPI.doesUserHavePermission(containerObj, PERMISSION_READ, user, false));
                }
                Map<String, Object> uuidsRendered = Maps.newHashMap();
                Map<String, Object> uuidsRaw = Maps.newHashMap();
                for (final String uniqueId : pageContents.row(containerId).keySet()) {
                    Map<String, Object> uuidRendered = Maps.newHashMap();
                    Map<String, Object> uuidRaw = Maps.newHashMap();
                    for (ContainerRendered rendered : containersRendered) {
                        if (containerId.equals(rendered.getContainer().getIdentifier())) {
                            uuidRendered.put("rendered", rendered.getRendered().get("uuid-" + uniqueId));
                        }
                    }


                    final Set<String> cons = pageContents.get(containerId, uniqueId);
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
                        map.put("canEdit", permissionAPI.doesUserHavePermission(c, PermissionAPI.PERMISSION_EDIT, user));
                        maps.add(map);
                    }


                    //uuidRaw.put("contentlets", maps);
                    uuidsRendered.put("uuid-" + uniqueId, uuidRendered);
                    uuidsRaw.put("uuid-" + uniqueId, maps);

                }


                final List<ContainerStructure> containerStructures = this.containerAPI.getContainerStructures(containerObj);
                List<String> allowedStructures = Lists.newArrayList();
                allowedStructures.add("WIDGET");
                allowedStructures.add("FORM");

                for (ContainerStructure struc : containerStructures) {

                    allowedStructures.add(cTypeApi.find(struc.getStructureId()).variable());

                }
                container.put("acceptTypes", String.join(",", allowedStructures));
                containerHolder.put("container", container);
                containerHolder.put("rendered", uuidsRendered);
                containerHolder.put("raw", uuidsRaw);
                containers.put(containerId, containerHolder);

            }


            // holder.put("site", host.getMap());
            holder.put("page", page.getMap());
            // holder.put("template", template);
            holder.put("layout", layout);

            holder.put("containers", containers);
            holder.put("viewAs", getViewAs(pageMode, request));


        } catch (Exception e) {
            Logger.warn(this.getClass(), e.getMessage(), e);
            holder.put("error", e.getMessage());

        }
        return holder;
    }

    private Map<String, Object> getViewAs(PageMode pageMode, HttpServletRequest request) throws DotDataException {
        Map<String, Object> map = Maps.newHashMap();
        map.put("persona", getCurrentPersona(request));
        map.put("device", getCurrentDevice(request));
        map.put("pageMode", pageMode);
        map.put("language", WebAPILocator.getLanguageWebAPI().getLanguage(request));
        return map;
    }

    private IPersona getCurrentPersona(final HttpServletRequest request) {
        final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request);
        return visitor.isPresent() && visitor.get().getPersona() != null ? visitor.get().getPersona() : null;
    }

    private Contentlet getCurrentDevice(final HttpServletRequest request) throws DotDataException {
        final String deviceInode = (String) request.getSession().getAttribute(WebKeys.CURRENT_DEVICE);
        Contentlet currentDevice = null;
        
        try {
            final User user = userAPI.getLoggedInUser(request);
            final String currentDeviceId =
                    deviceInode == null ? (String) request.getSession().getAttribute(WebKeys.CURRENT_DEVICE) : deviceInode;

            if (currentDeviceId != null) {
                currentDevice = contentletAPI.find(currentDeviceId, user, false);

                if (currentDevice == null) {
                    request.getSession().removeAttribute(WebKeys.CURRENT_DEVICE);
                }
            }
        } catch (Exception e) {
            Logger.debug(this.getClass(), "Exception on createViewAsMap exception message: " + e.getMessage(), e);
        }

        return currentDevice;
    }


    public ObjectNode getPageView(final HttpServletRequest request, final HttpServletResponse response, final String uri) throws Exception {
        final PageMode mode = (request.getParameter(WebKeys.PAGE_MODE_PARAMETER) != null)
                ? PageMode.get(request.getParameter(WebKeys.PAGE_MODE_PARAMETER))
                : PageMode.get(request);


        final long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();


        final User user = userAPI.getLoggedInUser(request);

        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final Identifier id = APILocator.getIdentifierAPI().find(host, uri);

        final HTMLPageAsset page = (HTMLPageAsset) APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(id.getId(), languageId,
                mode.showLive, user, mode.respectAnonPerms);

        final HTMLPageAssetRendered pageRendered =
                (HTMLPageAssetRendered) APILocator.getHTMLPageAssetRenderedAPI().getPageRendered(request, response, user, page, mode);

        PageView pageView = new HTMLPageAssetRendered(pageRendered.getSite(), pageRendered.getTemplate(), pageRendered.getContainers(),
                pageRendered.getPageInfo(), pageRendered.getLayout(), "test", pageRendered.isCanCreateTemplate(),
                pageRendered.isCanEditTemplate(), pageRendered.getViewAs());
        HTMLPageAssetRenderedSerializer serializer = new HTMLPageAssetRenderedSerializer();
        Method getObjectMapMethod = HTMLPageAssetRenderedSerializer.class.getDeclaredMethod("getObjectMap", PageView.class);
        getObjectMapMethod.setAccessible(true);
        Map<String, Object> newMap = (Map<String, Object>) getObjectMapMethod.invoke(serializer, pageView);

        return JsonMapper.mapper.valueToTree(newMap);

    }

}
