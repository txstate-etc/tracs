/**

 * TracsLDAPDirectoryProvider
 * We like to keep ldap user out of SAKAI_USER as its default standard behavior.
 */

package edu.amc.sakai.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.user.api.TracsUserDirectoryProvider;

public class TracsJLDAPDirectoryProvider extends JLDAPDirectoryProvider implements TracsUserDirectoryProvider {

	/** Class-specific logger */
	private static Log M_log = LogFactory
			.getLog(TracsJLDAPDirectoryProvider.class);

	public boolean findUserByPlid(String plid)
	{
		try {
			return getUserByPlid(plid, null)!=null;
		} catch ( Exception e ) {
			M_log.error("findUserByPlid(): failed [plid = " + plid + "]");
			M_log.debug("Exception: ", e);
			return false;
		}
	}

	public String getEidByPlid(String plid)
	{
		try {
			return getUserByPlid(plid, null).getEid();
		} catch ( Exception e ) {
			M_log.error("findUserByPlid(): failed [plid = " + plid + "]");
			M_log.debug("Exception: ", e);
			return null;
		}
	}

	public String getPlidByEid(String eid)
	{
		try {
			return getUserByEid(eid, null).getPlid();
		} catch ( Exception e ) {
			M_log.error("findUserByEid(): failed [eid = " + eid + "]");
			M_log.debug("Exception: ", e);
			return null;
		}
	}

}
