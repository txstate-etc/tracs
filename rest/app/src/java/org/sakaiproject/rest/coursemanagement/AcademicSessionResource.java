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
import org.sakaiproject.coursemanagement.api.CourseManagementAdministration;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.rest.annotation.AdminOnly;
import org.sakaiproject.rest.coursemanagement.data.AcademicSessionData;
import org.sakaiproject.rest.coursemanagement.data.DateUtils;

@Path("/course-management/academic-session")
@AdminOnly
public class AcademicSessionResource {

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementAdministration")
    protected CourseManagementAdministration cmAdmin;

    @Resource(name="org.sakaiproject.coursemanagement.api.CourseManagementService")
    protected CourseManagementService cmService;

    @GET
    @Path("/{eid}")
    @Produces(MediaType.APPLICATION_JSON)
    public AcademicSessionData get(@PathParam("eid") String eid) {
        AcademicSession session = cmService.getAcademicSession(eid);
        return new AcademicSessionData(session);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOrUpdate(AcademicSessionData data) {
        if (cmService.isAcademicSessionDefined(data.eid)) {
            AcademicSession updated = cmService.getAcademicSession(data.eid);
            updated.setTitle(data.title);
            updated.setDescription(data.description);
            updated.setStartDate(DateUtils.stringToDate(data.startDate));
            updated.setEndDate(DateUtils.stringToDate(data.endDate));

            // TODO: return 404 instead of 500 if there's no session with given eid
            cmAdmin.updateAcademicSession(updated);
        } else {
            cmAdmin.createAcademicSession(
              data.eid,
              data.title,
              data.description,
              DateUtils.stringToDate(data.startDate),
              DateUtils.stringToDate(data.endDate)
            );
        }
    }

    @DELETE
    @Path("/{eid}")
    public void delete(@PathParam("eid") String eid) {
        cmAdmin.removeAcademicSession(eid);
    }

    @PUT
    @Path("/set-current-sessions")
    public void setCurrentSessions(@QueryParam("list") String eidListString) {
        if (eidListString == null) throw new IllegalArgumentException("Missing required parameter: list");
        List<String> eidList = Arrays.asList(eidListString.split("\\s*,\\s*"));
        cmAdmin.setCurrentAcademicSessions(eidList);
    }
}
