package org.sakaiproject.webservices;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;

import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.AuthenticationManager;
import org.sakaiproject.user.api.IdPwEvidence;
import org.sakaiproject.user.api.Authentication;
import org.sakaiproject.user.api.AuthenticationException;

public class ValidateLogin extends AbstractWebService {

  private AuthenticationManager authenticationManager;
  public void setAuthenticationManager(AuthenticationManager manager) {
    authenticationManager = manager;
  }

  @GET
  public String validateCredentials(@QueryParam("username") final String username, @QueryParam("password") final String password) {
    IdPwEvidence evidence = new IdPwEvidence() {
      public String getIdentifier() {
        return username;
      }

      public String getPassword() {
        return password;
      }
    };

    try {
      authenticationManager.authenticate(evidence);
    } catch (AuthenticationException e) {
      return "";
    }

    return "success";
  }
}
