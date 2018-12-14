package com.dotcms.spa;


import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.filters.interceptor.FilterWebInterceptorProvider;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.InterceptorFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Config;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

    private String interceptorName;
    public void start(BundleContext context) throws Exception {

        this.addInterceptor();

        final ContentType host = APILocator.getContentTypeAPI(APILocator.systemUser()).find("host");
        Field proxyUrlField = host.fieldMap().get(SPAWebInterceptor.PROXY_EDIT_MODE_URL_VAR);
        
        if(proxyUrlField==null) {
            System.out.println("Adding Proxy URL Field to Host Structure");
            proxyUrlField = ImmutableTextField.builder()
                    .dataType(DataTypes.TEXT)
                    .name("Proxy Url for Edit Mode")
                    .variable(SPAWebInterceptor.PROXY_EDIT_MODE_URL_VAR)
                    .sortOrder(host.fields().size())
                    .contentTypeId(host.id())
                    .build();
            APILocator.getContentTypeFieldAPI().save(proxyUrlField, APILocator.systemUser());
        }
        
        System.out.println("Installing the SPAFilter");
        

    }

    private void addInterceptor() {

        final FilterWebInterceptorProvider filterWebInterceptorProvider =
                FilterWebInterceptorProvider.getInstance(Config.CONTEXT);

        final WebInterceptorDelegate delegate =
                filterWebInterceptorProvider.getDelegate(InterceptorFilter.class);

        final SPAWebInterceptor preWebInterceptor = new SPAWebInterceptor();
        this.interceptorName = preWebInterceptor.getName();
        delegate.addFirst(preWebInterceptor);

    }

    public void stop(BundleContext context) throws Exception {

        final FilterWebInterceptorProvider filterWebInterceptorProvider =
                FilterWebInterceptorProvider.getInstance(Config.CONTEXT);

        final WebInterceptorDelegate delegate =
                filterWebInterceptorProvider.getDelegate(InterceptorFilter.class);

        delegate.remove(this.interceptorName, true);
        delegate.reverseOrderForPostInvoke(true);
    }

}
