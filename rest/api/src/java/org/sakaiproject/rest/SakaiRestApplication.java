package org.sakaiproject.rest;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

import org.springframework.stereotype.Component;

@Component
public class SakaiRestApplication extends ResourceConfig {
    public SakaiRestApplication() {
        packages("org.sakaiproject.rest");
        register(RequestContextFilter.class);
        register(JacksonFeature.class);
    }
}
