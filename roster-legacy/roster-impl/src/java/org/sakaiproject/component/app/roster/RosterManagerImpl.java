/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/presence/trunk/presence-api/api/src/java/org/sakaiproject/presence/api/PresenceService.java $
 * $Id: PresenceService.java 7844 2006-04-17 13:06:02Z ggolden@umich.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006, 2007, 2008, 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.component.app.roster;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.app.roster.Participant;
import org.sakaiproject.api.app.roster.RosterFunctions;
import org.sakaiproject.api.app.roster.RosterManager;
import org.sakaiproject.api.privacy.PrivacyManager;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

public class RosterManagerImpl implements RosterManager {
    private static final Log log = LogFactory.getLog(RosterManagerImpl.class);

    private PrivacyManager privacyManager;
    public void setPrivacyManager(PrivacyManager manager) { privacyManager = manager; }

    private SiteService siteService;
    public void setSiteService(SiteService service) { siteService = service; }

    private ToolManager toolManager;
    public void setToolManager(ToolManager manager) { toolManager = manager; }

    private UserDirectoryService userDirectoryService;
    public void setUserDirectoryService(UserDirectoryService service) { userDirectoryService = service; }
    
    private AuthzGroupService authzGroupService;
    public void setAuthzGroupService(AuthzGroupService service) { authzGroupService = service; }

    private SecurityService securityService;
    public void setSecurityService(SecurityService service) { securityService = service; }

    public List<Participant> getRoster(String groupReference) {
        User currentUser = userDirectoryService.getCurrentUser();
        Map<Group, Set<String>> groupMembers = getGroupMembers(groupReference);
        List<Participant> participants = getParticipantsInGroups(currentUser, groupMembers);
        filterHiddenUsers(currentUser, participants, groupMembers);
        return participants;
    }

    /**
     * Gets a Map of a group to the user IDs for the members in the group
     * @return
     */
    private Map<Group, Set<String>> getGroupMembers(String groupReference) {
        Map<Group, Set<String>> groupMembers = new HashMap<Group, Set<String>>();
        Group group = siteService.findGroup(groupReference);
        if(group == null) {
            log.warn("Group " + groupReference + " not found");
            return groupMembers;
        }
        //
        Set<String> userIds = new HashSet<String>();
        Set<Member> members = group.getMembers();
        for(Iterator<Member> memberIter = members.iterator(); memberIter.hasNext();) {
            Member member = memberIter.next();
            userIds.add(member.getUserId());	
        }
        groupMembers.put(group, userIds);
        return groupMembers;
    }

    private void filterHiddenUsers(User currentUser, List<Participant> participants, Map<Group, Set<String>> groupMembers) {
        // If the user has view hidden in the site, don't filter anyone out
        if(userHasSitePermission(currentUser, RosterFunctions.ROSTER_FUNCTION_VIEWHIDDEN)) {
            return;
        }

        // Keep track of the users for which the current user has the group-scoped view hidden permission
        Set<String> visibleMembersForCurrentUser = new HashSet<String>();
        for (Entry<Group, Set<String>> e : groupMembers.entrySet()) {
            if(userHasGroupPermission(currentUser, RosterFunctions.ROSTER_FUNCTION_VIEWHIDDEN, e.getKey().getReference())) {
                visibleMembersForCurrentUser.addAll(e.getValue());
            }
        }

        // Iterate through the participants, removing the hidden ones that are not in visibleMembersForCurrentUser
        Set<String> userIds = new HashSet<String>();
        for(Iterator<Participant> iter = participants.iterator(); iter.hasNext();) {
            Participant participant = iter.next();
            userIds.add(participant.getUser().getId());
        }

        Set<String> hiddenUsers = privacyManager.findHidden("/site/" + getSiteId(), userIds);

        for(Iterator<Participant> iter = participants.iterator(); iter.hasNext();) {
            Participant participant = iter.next();
            String userId = participant.getUser().getId();
            if(hiddenUsers.contains(userId) && ! visibleMembersForCurrentUser.contains(userId)) {
                iter.remove();
            }
        }
    }

    private List<Participant> getParticipantsInGroups(User currentUser, Map<Group, Set<String>> groupMembers) {
        boolean userHasSiteViewAll = userHasSitePermission(currentUser, RosterFunctions.ROSTER_FUNCTION_VIEWALL);
        Set<String> viewableUsers = new HashSet<String>();
        //for(Iterator<Group> iter = groupMembers.keySet().iterator(); iter.hasNext();) {
            //Group group = iter.next();
        for(Entry<Group,Set<String>> e : groupMembers.entrySet()) {
            if(userHasGroupPermission(currentUser, RosterFunctions.ROSTER_FUNCTION_VIEWALL, e.getKey().getReference())
                    || userHasSiteViewAll) {
                viewableUsers.addAll(e.getValue());
            }
        }

        // Build the list of participants

        // Use the site reference because we need to display the site role, not the group role
        Map<String, UserRole> userMap = getUserRoleMap(getSiteReference());
        return buildParticipantList(userMap);
    }

    private List<Participant> buildParticipantList(Map<String, UserRole> userMap) {
        List<Participant> participants = new ArrayList<Participant>();
        Site site = null;
        try {
			site = siteService.getSite(getSiteId());
		} catch (IdUnusedException e) {
			log.error("getGroupsWithMember: " + e.getMessage(), e);
			return participants;
		}
		Collection<Group> groups = site.getGroups();
		
		Map<String, Boolean> userMemberStatusMap = new HashMap<String, Boolean>();
			userMemberStatusMap = getUserMemberStatusMap(site);
		
        for (Iterator<Entry<String, UserRole>> iter = userMap.entrySet().iterator(); iter.hasNext();) {
            Entry<String, UserRole> entry = iter.next();
            String userId = entry.getKey();
            UserRole userRole = entry.getValue();

            // Profiles may exist for users that have been removed.  If there's a profile
            // for a missing user, skip the profile.  See SAK-10936
            if(userRole == null || userRole.user == null) {
                log.warn("A profile exists for non-existent user " + userId);
                continue;
            }
            
            String groupsString = "";
            boolean memberStatus = false;
            if(null != userMemberStatusMap.get(userId))
            	memberStatus = userMemberStatusMap.get(userId);
            
			StringBuffer sb = new StringBuffer();
            for (Group group : groups)
            {
            	Member member = group.getMember(userId);
            	if (member !=null)
            	{
        			sb.append(group.getTitle() + ", ");
            	}
            }
            
            if (sb.length() > 0)
            {
            	int endIndex = sb.lastIndexOf(", ");
				if(endIndex > 0)
				{
		            		groupsString = sb.substring(0, endIndex);
				} else {
							groupsString = sb.toString();
				}
            }
            ParticipantImpl par = new ParticipantImpl(userRole.user, userRole.role, groupsString);
            String plid;
            try {
				plid = userDirectoryService.getUser(userId).getPlid();
			} catch (UserNotDefinedException e) {
				log.info("User eid for "+ userId + "is not found");
				plid = "N/A";
			}
			par.setPlid(plid);
			par.setActive(memberStatus);
            participants.add(par);
        }
        return participants;
    }

    static class UserRole {
        User user;
        String role;

        UserRole(User user, String role)
        {
            this.user = user;
            this.role = role;
        }
    }

    /**
     * Gets a map of user IDs to UserRole (User + Role) objects.
     *
     * @return
     */
    private Map<String, UserRole> getUserRoleMap(String authzRef) {
        Map<String, UserRole> userMap = new HashMap<String, UserRole>();
        Set<String> userIds = new HashSet<String>();
        Set<Member> members;

        // Get the member set
        try {
            members = authzGroupService.getAuthzGroup(authzRef).getMembers();
        } catch (GroupNotDefinedException e) {
            log.error("getUsersInAllSections: " + e.getMessage(), e);
            return userMap;
        }

        // Build a map of userId to role
        Map<String, String> roleMap = new HashMap<String, String>();
        for(Iterator<Member> iter = members.iterator(); iter.hasNext();)
        {
            Member member = iter.next();
//            if (member.isActive()) {
	            // SAK-17286 Only list users that are 'active' not 'inactive'
            {
				userIds.add(member.getUserId());
	            roleMap.put(member.getUserId(), member.getRole().getId());
			}
        }

        // Get the user objects
        List<User> users = userDirectoryService.getUsers(userIds);
        for (Iterator<User> iter = users.iterator(); iter.hasNext();)
        {
            User user = iter.next();
            String role = roleMap.get(user.getId()); 
            //We had userId inconsistency in db, userId shows as lowercase in the map
            //while, userId show uppercase in realm (so far only found instructors having this symptom)
            //in order to get the right role from realm role map, switch to uppercase userId as in
            //the realm.  -Qu a fix for bugid:4549 8/31/2011
            if (role == null)
            	role = roleMap.get(user.getId().toUpperCase());
            userMap.put(user.getId(), new UserRole(user, role)); 
        }
        return userMap;
    }

    /**
     * Check if given user has the given permission
     *
     * @param user
     * @param permissionName
     * @return boolean
     */
    private boolean userHasSitePermission(User user, String permissionName) {
        if (user == null || permissionName == null) {
            if(log.isDebugEnabled()) log.debug("userHasSitePermission passed a null");
            return false;
        }
        String siteReference = getSiteReference();
        boolean result = securityService.unlock(user, permissionName, siteReference);
        if(log.isDebugEnabled()) log.debug("user " + user.getEid() + ", permission " + permissionName + ", site " + siteReference + " has permission? " + result);
        return result;
    }

    private boolean userHasGroupPermission(User user, String permissionName, String groupReference) {
        if (user == null || permissionName == null || groupReference == null) {
            if(log.isDebugEnabled()) log.debug("userHasGroupPermission passed a null");
            return false;
        }
        boolean result =  authzGroupService.isAllowed(user.getId(), permissionName, groupReference);
        if(log.isDebugEnabled()) log.debug("user " + user.getEid() + ", permission " + permissionName + ", group " + groupReference + " has permission? " + result);
        return result;
    }

    /**
     * @return siteId
     */
    private String getSiteReference() {
        return siteService.siteReference(getSiteId());
    }

    private String getSiteId() {
        return toolManager.getCurrentPlacement().getContext();
    }
    
    //Added by -Qu for bugid:5266 3/7/2013
   public Map<String, Boolean> getUserMemberStatusMap(Site site){
	   Set<Member> members;
	   Map<String, Boolean> map = new HashMap<String, Boolean>();
	   
	try {
		members = authzGroupService.getAuthzGroup(site.getReference()).getMembers();
		for(Member m : members) {
			 map.put(m.getUserId(), m.isActive());
		   }
	} catch (GroupNotDefinedException e) {
		log.info("Group " + site.getReference() + "is not found.");
	}
	   return map;
   }
}
