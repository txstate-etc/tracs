package org.sakaiproject.login.filter;

import java.util.Observer;
import java.util.Observable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;

/**
 * Keeps track of ticket and session ids, so that sessions can be invalidated when a logout request is received.
 * Creates an event to notify other servers of a logout request and observes those events.
 */
public class SakaiCasLogoutHandler implements Observer {
    private static final Log log = LogFactory.getLog(SakaiCasLogoutHandler.class);

    public static String CAS_LOGOUT_EVENT = "cas.logout";

    private EventTrackingService eventTrackingService;
    public void setEventTrackingService(EventTrackingService service) {
        eventTrackingService = service;
        eventTrackingService.addObserver(this);
    }

    private SessionManager sessionManager;
    public void setSessionManager(SessionManager manager) { sessionManager = manager; }

    private TicketSessionStore ticketSessionStore;
    public void setTicketSessionStore(TicketSessionStore store) { ticketSessionStore = store; }

    public void handleLoginRequest(String ticketid) {
        Session session = sessionManager.getCurrentSession();
        log.debug("associating ticket " + ticketid + " with session " + session.getId());
        String oldSessionId = ticketSessionStore.addSession(ticketid, session.getId());

        // In theory it's possible to already have a session associated with the ticket
        if (oldSessionId != null) {
            Session oldSession = sessionManager.getSession(oldSessionId);
            if (oldSession != null) {
                log.warn("destroying existing session " + oldSessionId + " associated with ticket " + ticketid);
                session.invalidate();
            }
        }
    }

    public void handleLogoutRequest(String ticketid) {
        log.debug("creating cas.logout event for ticket " + ticketid);
        eventTrackingService.post(eventTrackingService.newEvent(CAS_LOGOUT_EVENT, ticketid, true));
    }

    public void update(Observable o, Object arg) {
        Event event = (Event) arg;
        if (!CAS_LOGOUT_EVENT.equals(event.getEvent())) return;
        log.debug("received cas.logout event for ticket " + event.getResource());
        destroySession(event.getResource());
    }

    private void destroySession(String ticketid) {
        String sessionid = ticketSessionStore.removeSession(ticketid);
        if (sessionid == null) { return; }

        Session session = sessionManager.getSession(sessionid);
        if (session != null) {
            log.debug("destroying session " + sessionid + " associated with ticket " + ticketid);
            session.invalidate();
        }
    }
}
