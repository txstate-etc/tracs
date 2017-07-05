
package org.sakaiproject.user.api;

/**
 * TracsUserDirectoryProvider feeds TRACS external user information to the UserDirectoryService.
 */
public interface TracsUserDirectoryProvider extends UserDirectoryProvider
{

	boolean findUserByPlid(String plid);

	String getEidByPlid(String plid);
	String getPlidByEid(String eid);

}
