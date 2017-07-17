package org.sakaiproject.rest.coursemanagement;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;

import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.coursemanagement.api.Enrollment;
import org.sakaiproject.coursemanagement.api.EnrollmentSet;
import org.sakaiproject.coursemanagement.api.CourseOffering;
import org.sakaiproject.coursemanagement.api.CourseManagementAdministration;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.rest.annotation.AdminOnly;
import org.sakaiproject.rest.coursemanagement.data.EnrollmentData;
import org.sakaiproject.rest.coursemanagement.data.EnrollmentSetData;
import org.sakaiproject.rest.coursemanagement.data.MembershipData;
import org.sakaiproject.rest.coursemanagement.data.DateUtils;

@Path("/course-management/enrollment-set")
@AdminOnly
public class EnrollmentSetResource {

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementAdministration")
    protected CourseManagementAdministration cmAdmin;

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementService")
    protected CourseManagementService cmService;

    @Resource(name="org.sakaiproject.authz.api.AuthzGroupService")
    protected AuthzGroupService authzGroupService;

    @GET
    @Path("/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    public EnrollmentSetData get(@PathParam("eid") String eid) {
        EnrollmentSet set = cmService.getEnrollmentSet(eid);
        return new EnrollmentSetData(set);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrUpdate(EnrollmentSetData data) {
        if (cmService.isEnrollmentSetDefined(data.eid)) {
            EnrollmentSet updated = cmService.getEnrollmentSet(data.eid);
            updated.setTitle(data.title);
            updated.setDescription(data.description);
            updated.setCategory(data.category);
            updated.setDefaultEnrollmentCredits(data.defaultCredits);
            Set<String> instructors = null;
            if (data.officialInstructors != null) {
              instructors = new HashSet<String>(data.officialInstructors);
            }
            updated.setOfficialInstructors(instructors);
            cmAdmin.updateEnrollmentSet(updated);
            updateSitesWithEnrollmentSet(data.eid);
        } else {
            Set<String> instructors = null;
            if (data.officialInstructors != null) {
                instructors = new HashSet<String>(data.officialInstructors);
            }
            cmAdmin.createEnrollmentSet(
              data.eid,
              data.title,
              data.description,
              data.category,
              data.defaultCredits,
              data.courseOffering,
              instructors
            );
        }
    }

    @DELETE
    @Path("/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    public void delete(@PathParam("eid") String eid) {
        cmAdmin.removeEnrollmentSet(eid);
    }

    @GET
    @Path("/{eid}/enrollment/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public EnrollmentData getEnrollment(@PathParam("eid") String eid, @PathParam("userId") String userId) {
        Enrollment enrollment = cmService.findEnrollment(eid, userId);
        if (enrollment == null) return null;
        
        EnrollmentData data = new EnrollmentData(enrollment, userId);
        return data;
    }

    @PUT
    @Path("/{eid}/enrollment")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrUpdateEnrollment(EnrollmentData data, @PathParam("eid") String eid) {
        cmAdmin.addOrUpdateEnrollment(
          data.userId,
          eid,
          data.status,
          data.credits,
          data.gradingScheme,
          DateUtils.stringToDate(data.dropDate)
        );
        
        if (data.dropped) {
            // Removing an enrollment marks it as dropped in the database.
            cmAdmin.removeEnrollment(data.userId, data.enrollmentSet);
        }

        updateSitesWithEnrollment(data.userId, data.enrollmentSet);
    }

    // If the official instructors list changes, instructors might be added to existing sites.
    // TODO: Make this more efficient.
    private void updateSitesWithEnrollmentSet(String enrollmentSetEid) {
        Set<String> groupIds = authzGroupService.getAuthzGroupIds(enrollmentSetEid);
        for (String id : groupIds) {
            try {
                AuthzGroup group = authzGroupService.getAuthzGroup(id);
                authzGroupService.save(group);
            } catch (GroupNotDefinedException ex) {
                // This should never happen since the id was given to us from getAuthzGroupIds.
                ex.printStackTrace();
                throw new RuntimeException("An error occured updating site " + id + " with provider id " + enrollmentSetEid);
            } catch (AuthzPermissionException ex) {
                // This should also never happen since a SecurityException would've been thrown earlier.
                ex.printStackTrace();
                throw new RuntimeException("An error occured updating site " + id + " with provider id " + enrollmentSetEid);
            }
        }
    }

    // TODO: Update memberships individually rather than going through all
    // the memberships to update like the authzGroupService.save does.
    // Right now this is equivalent to our nightly update participants job.
    private void updateSitesWithEnrollment(String userEid, String enrollmentSetEid) {
        Set<String> groupIds = authzGroupService.getAuthzGroupIds(enrollmentSetEid);
        for (String id : groupIds) {
            try {
                AuthzGroup group = authzGroupService.getAuthzGroup(id);
                authzGroupService.save(group);
            } catch (GroupNotDefinedException ex) {
                // This should never happen since the id was given to us from getAuthzGroupIds.
                ex.printStackTrace();
                throw new RuntimeException("An error occured updating site " + id + " with provider id " + enrollmentSetEid);
            } catch (AuthzPermissionException ex) {
                // This should also never happen since a SecurityException would've been thrown earlier.
                ex.printStackTrace();
                throw new RuntimeException("An error occured updating site " + id + " with provider id " + enrollmentSetEid);
            }
        }
    }
}
