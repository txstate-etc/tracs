/**********************************************************************************
 *
 * This file was added for mapping users and their enrollment status from CM provider
 * As per request in Texas State University
 * - Adding inactive/active students to a site from a cm provider reflecting their
 * enrollment status.
 * (For those students who are not currently enrolled and not dropped yet should show
 * up in site as inactive instaed of active as of current sakai 11.3)
 *
 **********************************************************************************/
package org.sakaiproject.coursemanagement.impl.provider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Enrollment;
import org.sakaiproject.coursemanagement.api.EnrollmentSet;
import org.sakaiproject.coursemanagement.api.Membership;
import org.sakaiproject.coursemanagement.api.Section;

/**
 * Resolves user status in sections.
 *
 * author <a href="mailto:yq12@txstate.edu">Yuanhua Qu</a>
 *
 */
public class SectionStatusResolver  {
	private static final Log log = LogFactory.getLog(SectionStatusResolver.class);

	protected Map<String, Object> configuration;

	/** The Sakai roles to use for official enrollments in EnrollmentSets, keyed on the enrollment status */
	protected Map<String, String> enrollmentStatusRoleMap;

	// Configuration keys.
	public static final String ENROLLMENT_STATUS_TO_SITE_ROLE = "enrollmentStatusToSiteRole";

	/**
	 * Internal configuration.
	 */
	public void init() {
		if (configuration != null) {
			if (log.isDebugEnabled()) log.debug("Using configuration object;  enrollment status role map=" + configuration.get(ENROLLMENT_STATUS_TO_SITE_ROLE));
			setEnrollmentStatusRoleMap((Map<String, String>)configuration.get(ENROLLMENT_STATUS_TO_SITE_ROLE));
		} else {
			if (log.isDebugEnabled()) log.debug("Not using configuration object");
		}
	}


  //This method returns student -->status mapping from enrollments for each course section, keyed on userEid
	//like ar1096 instead uuid.
	public Map<String, String> getUserStatusFromEnrollment(CourseManagementService cmService, Section section) {
		Map<String, String> userStatusMap = new HashMap<String, String>();

		EnrollmentSet enrSet = section.getEnrollmentSet();
		if(log.isDebugEnabled()) log.debug( "EnrollmentSet  " + enrSet + " is attached to section " + section.getEid());
		if(enrSet != null) {

			// Check for official instructors
			Set<String> officialInstructors = cmService.getInstructorsOfRecordIds(enrSet.getEid());
			for(Iterator<String> iter = officialInstructors.iterator(); iter.hasNext();) {
				userStatusMap.put(iter.next(), "true");
			}

			// Check for enrollments
			Set enrollments = cmService.getEnrollments(section.getEnrollmentSet().getEid());
			for(Iterator iter = enrollments.iterator(); iter.hasNext();) {
				Enrollment enr = (Enrollment)iter.next();
				if(enr.isDropped()) {
					continue;
				}
				String roleFromEnrollmentStatus = (String)enrollmentStatusRoleMap.get(enr.getEnrollmentStatus());

				// Only add the enrollment if it's not dropped and it has an enrollment role mapping
				if( ! userStatusMap.containsKey(enr.getUserId()) && roleFromEnrollmentStatus != null &&  ! enr.isDropped()) {
					userStatusMap.put(enr.getUserId(), enr.getEnrollmentStatus());
				}
			}
		}

		Set memberships = cmService.getSectionMemberships(section.getEid());
		for(Iterator iter = memberships.iterator(); iter.hasNext();) {
			Membership membership = (Membership)iter.next();
			// Only add the membership status if the user isn't enrolled or an official instructor(these take precedence)
			if( ! userStatusMap.containsKey(membership.getUserId())) {
				String status = membership.getStatus();
				if(status != null) {
					userStatusMap.put(membership.getUserId(), status);
				}
			}
		}
		return userStatusMap;
	}

	public void setEnrollmentStatusRoleMap(Map<String, String> enrollmentStatusRoleMap) {
		this.enrollmentStatusRoleMap = enrollmentStatusRoleMap;
	}

	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}
}
