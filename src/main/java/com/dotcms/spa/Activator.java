package com.dotcms.spa;


import org.osgi.framework.BundleContext;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.filters.interceptor.FilterWebInterceptorProvider;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.AutoLoginFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Config;

public class Activator extends GenericBundleActivator {

    public void start(BundleContext context) throws Exception {

        final FilterWebInterceptorProvider filterWebInterceptorProvider = FilterWebInterceptorProvider.getInstance(Config.CONTEXT);
        final WebInterceptorDelegate delegate = filterWebInterceptorProvider.getDelegate(AutoLoginFilter.class);

        if (null != delegate) {
            System.out.println("Adding the SPAInterceptor");
            delegate.addFirst(new SPAInterceptor());
        }

        
        ContentType host = APILocator.getContentTypeAPI(APILocator.systemUser()).find("host");
        Field proxyUrlField = host.fieldMap().get(SPAInterceptor.PROXY_EDIT_MODE_URL_VAR);
        
        if(proxyUrlField==null) {
            System.out.println("Adding Proxy URL Field to Host Structure");
            proxyUrlField = ImmutableTextField.builder()
                    .dataType(DataTypes.TEXT)
                    .name("Proxy Url for Edit Mode")
                    .variable(SPAInterceptor.PROXY_EDIT_MODE_URL_VAR)
                    .sortOrder(host.fields().size())
                    .contentTypeId(host.id())
                    .build();
            APILocator.getContentTypeFieldAPI().save(proxyUrlField, APILocator.systemUser());
        }
        
        
        

    }

    public void stop(BundleContext context) throws Exception {

        final FilterWebInterceptorProvider filterWebInterceptorProvider = FilterWebInterceptorProvider.getInstance(Config.CONTEXT);
        final WebInterceptorDelegate delegate = filterWebInterceptorProvider.getDelegate(AutoLoginFilter.class);

        if (null != delegate) {
            System.out.println("Removing the SPAInterceptor");
            delegate.remove(SPAInterceptor.class.getName(), true);
        }
    }

}
