package org.sakaiproject.login.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jasig.cas.client.Protocol;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.util.XmlUtils;

/**
 * A simple filter that handles back channel logout requests from CAS.
 */
public class CasSingleSignOutFilter extends AbstractCasFilter {
  public final static String TICKET_PARAMETER_NAME = "ticket";
  public final static String LOGOUT_PARAMETER_NAME = "logoutRequest";

  private SakaiCasLogoutHandler logoutHandler;
  public void setLogoutHandler(SakaiCasLogoutHandler handler) { logoutHandler = handler; }

  public CasSingleSignOutFilter() {
    // By default use CAS2 for compatibility
      this(Protocol.CAS2);
  }

  public CasSingleSignOutFilter(final Protocol protocol) {
      super(protocol);
  }

  public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;

    if (isTokenRequest(request)) {
      String ticketid = CommonUtils.safeGetParameter(request, TICKET_PARAMETER_NAME);
      logoutHandler.handleLoginRequest(ticketid);
    } else if (isBackChannelLogoutRequest(request)) {
      String logoutMessage = CommonUtils.safeGetParameter(request, LOGOUT_PARAMETER_NAME);
      String ticketid = XmlUtils.getTextForElement(logoutMessage, "SessionIndex");
      if (CommonUtils.isNotBlank(ticketid)) {
        logoutHandler.handleLogoutRequest(ticketid);
      }
      return;
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  /**
   * @see org.jasig.cas.client.session.SingleSignOutHandler#isTokenRequest
   */
  private boolean isTokenRequest(final HttpServletRequest request) {
      return CommonUtils.isNotBlank(CommonUtils.safeGetParameter(request, TICKET_PARAMETER_NAME));
  }

  /**
   * @see org.jasig.cas.client.session.SingleSignOutHandler#isBackChannelLogoutRequest
   */
  private boolean isBackChannelLogoutRequest(final HttpServletRequest request) {
      return "POST".equals(request.getMethod())
              && !isMultipartRequest(request)
              && CommonUtils.isNotBlank(CommonUtils.safeGetParameter(request, LOGOUT_PARAMETER_NAME));
  }

  /**
   * @see org.jasig.cas.client.session.SingleSignOutHandler#isMultipartRequest
   */
  private boolean isMultipartRequest(final HttpServletRequest request) {
      return request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart");
  }  
}
