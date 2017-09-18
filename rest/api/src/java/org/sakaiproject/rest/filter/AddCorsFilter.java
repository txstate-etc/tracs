package org.sakaiproject.rest.filter;

import org.sakaiproject.rest.annotation.AllowOrigin;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import java.io.IOException;

@AllowOrigin
public class AddCorsFilter implements ContainerResponseFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String origin = "*";
        AllowOrigin allowOrigin = resourceInfo.getResourceClass().getAnnotation(AllowOrigin.class);
        if (allowOrigin != null) {
            origin = allowOrigin.value();
        }
        allowOrigin = resourceInfo.getResourceMethod().getAnnotation(AllowOrigin.class);
        if (allowOrigin != null) {
            origin = allowOrigin.value();
        }

        responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
    }
}
