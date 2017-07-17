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

import org.sakaiproject.coursemanagement.api.CanonicalCourse;
import org.sakaiproject.coursemanagement.api.CourseManagementAdministration;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.rest.annotation.AdminOnly;
import org.sakaiproject.rest.coursemanagement.data.CanonicalCourseData;

@Path("/course-management/canonical-course")
@AdminOnly
public class CanonicalCourseResource {

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementAdministration")
    protected CourseManagementAdministration cmAdmin;

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementService")
    protected CourseManagementService cmService;

    @GET
    @Path("/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    public CanonicalCourseData get(@PathParam("eid") String eid) {
        CanonicalCourse course = cmService.getCanonicalCourse(eid);
        return new CanonicalCourseData(course);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrUpdate(CanonicalCourseData data) {
        if (cmService.isCanonicalCourseDefined(data.eid)) {
            CanonicalCourse updated = cmService.getCanonicalCourse(data.eid);
            updated.setTitle(data.title);
            updated.setDescription(data.description);
            cmAdmin.updateCanonicalCourse(updated);
        } else {
            cmAdmin.createCanonicalCourse(data.eid, data.title, data.description);
        }
    }

    @DELETE
    @Path("/{eid}")
    public void delete(@PathParam("eid") String eid) {
        cmAdmin.removeCanonicalCourse(eid);
    }
}
