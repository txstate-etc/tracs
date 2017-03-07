package org.sakaiproject.rest.resource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlReaderFinishedException;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;

@Path("/session-history")
public class SessionHistoryResource {

    private static Log LOG = LogFactory.getLog(SessionHistoryResource.class);

    @Autowired
    private SqlService sqlService;
    @Autowired
    private UserDirectoryService userDirectoryService;

    private static String SESSIONS_BY_USER_SQL = "select * from SAKAI_SESSION where SESSION_USER = ? order by SESSION_START desc limit 10";

    public class Session {
        public String user;
        public String ip;
        public String userAgent;
        public long start;
        public long end;
        public boolean active;
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Session> getSessionsForCurrentUser() {
        final String userId = userDirectoryService.getCurrentUser().getId();
        return sqlService.dbRead(SESSIONS_BY_USER_SQL, new Object[]{userId}, new SqlReader() {
            @Override
            public Object readSqlResultRecord(ResultSet result) throws SqlReaderFinishedException {
                try {
                    Session session = new Session();
                    session.user = result.getString("SESSION_USER");
                    session.ip = result.getString("SESSION_IP");
                    session.userAgent = result.getString("SESSION_USER_AGENT");
                    session.start = result.getTimestamp("SESSION_START").getTime();
                    session.end = result.getTimestamp("SESSION_END").getTime();
                    session.active = result.getBoolean("SESSION_ACTIVE");
                    return session;
                } catch (SQLException e) {
                    LOG.warn("SQLException getting sessions for user " + userId);
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }
}
