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

import org.sakaiproject.coursemanagement.api.CourseSet;
import org.sakaiproject.coursemanagement.api.CourseManagementAdministration;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.rest.annotation.AdminOnly;
import org.sakaiproject.rest.coursemanagement.data.CourseSetData;
import org.sakaiproject.rest.coursemanagement.data.MembershipData;

@Path("/course-management/course-set")
@AdminOnly
public class CourseSetResource {

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementAdministration")
    protected CourseManagementAdministration cmAdmin;

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementService")
    protected CourseManagementService cmService;

    @GET
    @Path("/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object get(@PathParam("eid") String eid) {
        CourseSet set = cmService.getCourseSet(eid);
        return new CourseSetData(set);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrUpdate(CourseSetData data) {
        if (cmService.isCourseSetDefined(data.eid)) {
            CourseSet updated = cmService.getCourseSet(data.eid);
            updated.setTitle(data.title);
            updated.setDescription(data.description);
            updated.setCategory(data.category);

            CourseSet parent = null;
            if (data.parent != null) parent = cmService.getCourseSet(data.parent);
            updated.setParent(parent);

            cmAdmin.updateCourseSet(updated);
        } else {
            cmAdmin.createCourseSet(
              data.eid,
              data.title,
              data.description,
              data.category,
              data.parent
            );
        }
    }

    @DELETE
    @Path("/{eid}")
    public void delete(@PathParam("eid") String eid) {
        cmAdmin.removeCourseSet(eid);
    }

    @PUT
    @Path("/{eid}/membership")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addOrUpdateMembership(MembershipData data, @PathParam("eid") String eid) {
        cmAdmin.addOrUpdateCourseSetMembership(
          data.userId,
          data.role,
          eid,
          data.status
        );
    }

    @DELETE
    @Path("/{eid}/membership/{userId}")
    public void removeMembership(@PathParam("eid") String eid, @PathParam("userId") String userId) {
        cmAdmin.removeCourseSetMembership(userId, eid);
    }
}
