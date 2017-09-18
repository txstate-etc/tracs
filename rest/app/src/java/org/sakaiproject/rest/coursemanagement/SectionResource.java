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


import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.coursemanagement.api.CourseManagementAdministration;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.rest.annotation.AdminOnly;
import org.sakaiproject.rest.coursemanagement.data.MembershipData;
import org.sakaiproject.rest.coursemanagement.data.SectionData;

@Path("/course-management/section")
@AdminOnly
public class SectionResource {

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementAdministration")
    protected CourseManagementAdministration cmAdmin;

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementService")
    protected CourseManagementService cmService;

    @GET
    @Path("/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    public SectionData get(@PathParam("eid") String eid) {
        Section section = cmService.getSection(eid);
        return new SectionData(section);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrUpdate(SectionData data) {
        if (cmService.isSectionDefined(data.eid)) {
            Section updated = cmService.getSection(data.eid);
            updated.setTitle(data.title);
            updated.setDescription(data.description);
            updated.setCategory(data.category);
            updated.setEnrollmentSet(cmService.getEnrollmentSet(data.enrollmentSet));
            updated.setMaxSize(data.maxSize);

            Section parent = null;
            if (data.parent != null) parent = cmService.getSection(data.parent);
            updated.setParent(parent);
            
            cmAdmin.updateSection(updated);
        } else {
            cmAdmin.createSection(
              data.eid,
              data.title,
              data.description,
              data.category,
              data.parent,
              data.courseOffering, 
              data.enrollmentSet
            );
        }
    }

    @DELETE
    @Path("/{eid}")
    public void delete(String eid) {
        cmAdmin.removeSection(eid);
    }

    @PUT
    @Path("/{eid}/membership")
    public void addOrUpdateMembership(MembershipData data, @PathParam("eid") String eid) {
        cmAdmin.addOrUpdateSectionMembership(
          data.userId,
          data.role,
          eid,
          data.status
        );
    }

    @DELETE
    @Path("/{eid}/membership/{userId}")
    public void removeMembership(@PathParam("eid") String eid, @PathParam("userId") String userId) {
        cmAdmin.removeSectionMembership(userId, eid);
    }
}
