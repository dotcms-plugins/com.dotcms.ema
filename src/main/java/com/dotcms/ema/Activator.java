package com.dotcms.ema;


import org.osgi.framework.BundleContext;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.ema.util.FilterOrder;
import com.dotcms.ema.util.TomcatServletFilterUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.osgi.GenericBundleActivator;

public class Activator extends GenericBundleActivator {
    final static String FILTER_NAME = "HubspotFilter";
    public void start(BundleContext context) throws Exception {


        new TomcatServletFilterUtil().addFilter(FILTER_NAME, new EMAFilter(), FilterOrder.FIRST, "/api/v1/page/render/*");
        
        ContentType host = APILocator.getContentTypeAPI(APILocator.systemUser()).find("host");
        Field proxyUrlField = host.fieldMap().get(EMAFilter.PROXY_EDIT_MODE_URL_VAR);
        
        if(proxyUrlField==null) {
            System.out.println("Adding Proxy URL Field to Host Structure");
            proxyUrlField = ImmutableTextField.builder()
                    .dataType(DataTypes.TEXT)
                    .name("Proxy Url for Edit Mode")
                    .hint("Set this value to the full url that will receive the page-as-a-service payload as an HTTP POST, e.g. https://spa.dotcms.com/editMode")
                    .variable(EMAFilter.PROXY_EDIT_MODE_URL_VAR)
                    .sortOrder(host.fields().size())
                    .contentTypeId(host.id())
                    .build();
            APILocator.getContentTypeFieldAPI().save(proxyUrlField, APILocator.systemUser());
        }
        
        System.out.println("Installing the SPAFilter");
        

    }

    public void stop(BundleContext context) throws Exception {

        new TomcatServletFilterUtil().removeFilter(FILTER_NAME);


    }

}
