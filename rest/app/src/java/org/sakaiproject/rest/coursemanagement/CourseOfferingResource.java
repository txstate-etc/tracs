package org.sakaiproject.rest.coursemanagement;

import java.util.Arrays;
import java.util.List;

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

import org.sakaiproject.coursemanagement.api.AcademicSession;
import org.sakaiproject.coursemanagement.api.CourseOffering;
import org.sakaiproject.coursemanagement.api.CourseManagementAdministration;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.rest.annotation.AdminOnly;
import org.sakaiproject.rest.coursemanagement.data.CourseOfferingData;
import org.sakaiproject.rest.coursemanagement.data.MembershipData;
import org.sakaiproject.rest.coursemanagement.data.DateUtils;

@Path("/course-management/course-offering")
@AdminOnly
public class CourseOfferingResource {

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementAdministration")
    protected CourseManagementAdministration cmAdmin;

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementService")
    protected CourseManagementService cmService;

    @GET
    @Path("/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    public CourseOfferingData get(@PathParam("eid") String eid) {
        CourseOffering course = cmService.getCourseOffering(eid);
        return new CourseOfferingData(course);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrUpdate(CourseOfferingData data) {
        if (cmService.isCourseOfferingDefined(data.eid)) {
            CourseOffering updated = cmService.getCourseOffering(data.eid);
            updated.setTitle(data.title);
            updated.setDescription(data.description);
            updated.setStatus(data.status);
            updated.setStartDate(DateUtils.stringToDate(data.startDate));
            updated.setEndDate(DateUtils.stringToDate(data.endDate));

            if (updated.getAcademicSession() == null || updated.getAcademicSession().getEid() != data.academicSession) {
                AcademicSession newSession = cmService.getAcademicSession(data.academicSession);
                updated.setAcademicSession(newSession);
            }

            // TODO: There's no method to set canonical course for CourseOffering. Not sure if this is something
            // that we'll want to implement. It does make sense that you wouldn't ever want to change canonical course
            // since you're basically talking about a completely new course offering at that point.
            cmAdmin.updateCourseOffering(updated);
        } else {
            cmAdmin.createCourseOffering(
              data.eid,
              data.title,
              data.description,
              data.status,
              data.academicSession,
              data.canonicalCourse,
              DateUtils.stringToDate(data.startDate),
              DateUtils.stringToDate(data.endDate)
            );
        }
    }

    @DELETE
    @Path("/{eid}")
    public void delete(@PathParam("eid") String eid) {
        cmAdmin.removeCourseOffering(eid);
    }

    @PUT
    @Path("/{eid}/membership")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addOrUpdateMembership(MembershipData data, @PathParam("eid") String eid) {
        cmAdmin.addOrUpdateCourseOfferingMembership(
          data.userId,
          data.role,
          eid,
          data.status
        );
    }

    @DELETE
    @Path("/{eid}/membership/{userId}")
    public void removeMembership(@PathParam("eid") String eid, @PathParam("userId") String userId) {
        cmAdmin.removeCourseOfferingMembership(userId, eid);
    }
}
