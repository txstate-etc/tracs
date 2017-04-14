package org.sakaiproject.login.filter;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class TicketSessionStore {
    private static final Log log = LogFactory.getLog(TicketSessionStore.class);

    private Map<String, DateTime> timeMap = new ConcurrentHashMap<String, DateTime>();
    private Map<String, String> ticketSessionMap;

    public TicketSessionStore(final long ticketExpireSeconds) {
        final Duration ticketExpireTime = Duration.standardSeconds(ticketExpireSeconds);
        ticketSessionMap = Collections.synchronizedMap(new LinkedHashMap<String, String>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                DateTime insertTime = timeMap.get(eldest.getKey());
                if (insertTime.plus(ticketExpireTime).isBeforeNow()) {
                    log.debug("removing stale session " + eldest.getValue() + " associated with ticket " + eldest.getKey());
                    timeMap.remove(eldest.getKey());
                    return true;
                }
                return false;
            }
        });
    }

    public String addSession(String ticketid, String sessionid) {
        timeMap.put(ticketid, new DateTime());
        return ticketSessionMap.put(ticketid, sessionid);
    }

    public String getSession(String ticketid) {
        return ticketSessionMap.get(ticketid);
    }

    public String removeSession(String ticketid) {
        timeMap.remove(ticketid);
        return ticketSessionMap.remove(ticketid);
    }
}
