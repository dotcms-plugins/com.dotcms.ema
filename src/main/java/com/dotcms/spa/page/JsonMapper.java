package com.dotcms.spa.page;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapper {

  public static final 
  ObjectMapper mapper = new ObjectMapper()
      .addMixIn(Permissionable.class, PermissionableMixIn.class)
      .addMixIn(Contentlet.class, ContentletMixIn.class)
      .addMixIn(HTMLPageAsset.class, ContentletMixIn.class)
      .addMixIn(WebAsset.class, WebAssetMixin.class)
      .addMixIn(Host.class, ContentletMixIn.class);
  
  
}
