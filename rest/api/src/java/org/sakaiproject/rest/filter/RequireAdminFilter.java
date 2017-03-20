package org.sakaiproject.rest.filter;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.rest.annotation.AdminOnly;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@AdminOnly
@Priority(Priorities.AUTHENTICATION)
@Provider
public class RequireAdminFilter implements ContainerRequestFilter {

    private static Log LOG = LogFactory.getLog(RequireAdminFilter.class);

  @Autowired
  protected SecurityService securityService;

  @Override
  public void filter(ContainerRequestContext context) throws IOException {
    if (!securityService.isSuperUser()) {
      context.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
    }
  }
}
