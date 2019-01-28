package com.dotcms.ema;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.osgi.framework.BundleContext;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.filters.interceptor.FilterWebInterceptorProvider;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.AutoLoginFilter;
import com.dotmarketing.loggers.Log4jUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Config;

public class Activator extends GenericBundleActivator {
    private LoggerContext pluginLoggerContext;
    
    private final EMAWebInterceptor eMAWebInterceptor= new EMAWebInterceptor();
    
    public void start(BundleContext context) throws Exception {


        initializeServices(context);
        // Initializing log4j...
        LoggerContext dotcmsLoggerContext = Log4jUtil.getLoggerContext();
        // Initialing the log4j context of this plugin based on the dotCMS logger context
        pluginLoggerContext = (LoggerContext) LogManager.getContext(this.getClass().getClassLoader(), false, dotcmsLoggerContext,
                dotcmsLoggerContext.getConfigLocation());
        
        final FilterWebInterceptorProvider filterWebInterceptorProvider = FilterWebInterceptorProvider.getInstance(Config.CONTEXT);

        final WebInterceptorDelegate delegate = filterWebInterceptorProvider.getDelegate(AutoLoginFilter.class);

        delegate.addFirst(eMAWebInterceptor);
        
        ContentType host = APILocator.getContentTypeAPI(APILocator.systemUser()).find("host");
        Field proxyUrlField = host.fieldMap().get(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR);
        
        if(proxyUrlField==null) {
            System.out.println("Adding Proxy URL Field to Host Structure");
            proxyUrlField = ImmutableTextField.builder()
                    .dataType(DataTypes.TEXT)
                    .name("Proxy Url for Edit Mode")
                    .hint("Set this value to the full url that will receive the page-as-a-service payload as an HTTP POST, e.g. https://spa.dotcms.com/editMode")
                    .variable(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR)
                    .sortOrder(host.fields().size())
                    .contentTypeId(host.id())
                    .build();
            APILocator.getContentTypeFieldAPI().save(proxyUrlField, APILocator.systemUser());
        }
        
        System.out.println("Installing the SPAFilter");
        

    }

    public void stop(BundleContext context) throws Exception {
        
        final FilterWebInterceptorProvider filterWebInterceptorProvider = FilterWebInterceptorProvider.getInstance(Config.CONTEXT);

        final WebInterceptorDelegate delegate = filterWebInterceptorProvider.getDelegate(AutoLoginFilter.class);

        delegate.remove(eMAWebInterceptor.getName(), true);
        Log4jUtil.shutdown(pluginLoggerContext);

    }

}
