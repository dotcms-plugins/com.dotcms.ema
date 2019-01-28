package com.dotcms.ema.page;

import com.dotmarketing.business.Permissionable;
import com.fasterxml.jackson.annotation.JsonIgnore;

abstract class PermissionableMixIn {
  @JsonIgnore abstract Permissionable getParentPermissionable(); // we don't need it!  
}
