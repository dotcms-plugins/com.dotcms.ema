package com.dotcms.ema.serializer;

import java.util.Map;

import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.google.common.collect.Maps;

/**
 * Json serializer of {@link HTMLPageAssetRendered}
 */
public class HTMLPageAssetRenderedSerializer extends PageViewSerializer {

    @Override
    public Map<String, Object> getObjectMap(PageView pageView) {
        final HTMLPageAssetRendered htmlPageAssetRendered = (HTMLPageAssetRendered) pageView;

        final Map<String, Object> objectMap = super.getObjectMap(pageView);

        final Map<String, Object> pageMap = (Map<String, Object>) objectMap.get("page");
        pageMap.put("rendered", htmlPageAssetRendered.getHtml());

        final Map<String, Object> templateMap = (Map<String, Object>) objectMap.get("template");
        templateMap.put("canEdit", htmlPageAssetRendered.isCanEditTemplate());

        final Map<String, Object> responseMap = Maps.newHashMap();
        responseMap.putAll(objectMap);
        responseMap.put("viewAs", htmlPageAssetRendered.getViewAs());
        responseMap.put("canCreateTemplate", htmlPageAssetRendered.isCanCreateTemplate());

        return responseMap;
    }

}
