package com.dotcms.spa.page;

import java.util.Map;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.fasterxml.jackson.annotation.JsonIgnore;

abstract class ContentletMixIn {
  @JsonIgnore abstract Permissionable getParentPermissionable(); 
  @JsonIgnore abstract public Structure getStructure() ;
  @JsonIgnore abstract public Map<String, Object> getMap () throws DotStateException, DotDataException, DotSecurityException  ;
  
  @JsonIgnore abstract public ContentType getContentType()  ;
  
}
