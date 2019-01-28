package com.dotcms.ema.page;

import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.annotation.JsonIgnore;

abstract class WebAssetMixin {

  @JsonIgnore abstract public Map<String, Object> getMap () throws DotStateException, DotDataException, DotSecurityException  ;

  
}
