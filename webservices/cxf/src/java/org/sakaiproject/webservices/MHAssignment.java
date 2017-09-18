package org.sakaiproject.webservices;

import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.GradeDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.GradebookExternalAssessmentService;

import java.util.Date;
import java.util.Iterator;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC, use = SOAPBinding.Use.LITERAL)
public class MHAssignment extends AbstractWebService {

    protected GradebookExternalAssessmentService gradebookExternalAssessmentService;

    @WebMethod
    @Path("/test")
    @Produces("text/plain")
    @GET
    /**
     *
     * @return String
     */
    public String test(
        @WebParam(partName = "sessionID")
        @QueryParam("sessionID") String sessionID
    ) throws org.sakaiproject.user.api.UserNotDefinedException
	{
		// Make sure the session is valid.
		establishSession(sessionID);

		String eid;
		try
		{
			eid = userDirectoryService.getUserId(sessionID);
		}
		catch(org.sakaiproject.user.api.UserNotDefinedException e)
		{
			eid = "0fbc1625-95b1-4cd0-b12a-c19e3eeb8252";
		}

		return eid;
	}

	@WebMethod
	@Path("/UpdateScore")
	@Produces("text/plain")
	@GET
    /**
     * Updates user score in the specified assignment by external id.
     *
     * @param String sessionID
     * @param String gradebookID
     * @param String assignmentID
     * @param String userID
     * @param String Points
     * @return void
     * @throws org.sakaiproject.user.api.UserNotDefinedException
     * @throws GradebookNotFoundException
     * @throws AssessmentNotFoundException
     */
	public void UpdateScore(
		@WebParam(partName = "sessionID") @QueryParam("sessionID") String sessionID,
		@WebParam(partName = "gradebookID") @QueryParam("gradebookID") String gradebookID,
		@WebParam(partName = "assignmentID") @QueryParam("assignmentID") String assignmentID,
		@WebParam(partName = "userID") @QueryParam("userID") String userID,
		@WebParam(partName = "points") @QueryParam("points") String Points
    ) throws org.sakaiproject.user.api.UserNotDefinedException
	{
        // Check to make sure session is valid.
        Session s = establishSession(sessionID);

        // Require permission.
        if (!hasPermission(gradebookID, "gradebook.gradeAll"))
        {
 			throw new RuntimeException("Non-permitted user trying to update scores: " + s.getUserId());
        }

        // Get the user id from the directory.
        String uid = getUserId(userID);

        // Update score.
        gradebookExternalAssessmentService.updateExternalAssessmentScore(gradebookID, assignmentID, uid, Points);
	}

	@WebMethod
	@Path("/UpdateScorableItem")
	@Produces("text/plain")
	@GET
    /**
     * Adds/updates MH Campus external assignments.
     *
     * @param String sessionID
     * @param String gradebookID
     * @param String assignmentID
     * @param String addUpdateRemoveAssignment
     * @param String assignment_Title
     * @param Double assignment_maxPoints
     * @param long assignment_dueTime
     * @return void
     */
	public void UpdateScorableItem (
		@WebParam(partName = "sessionID") @QueryParam("sessionID") String sessionID,
		@WebParam(partName = "gradebookID") @QueryParam("gradebookID") String gradebookID,
		@WebParam(partName = "assignmentID") @QueryParam("assignmentID") String assignmentID,
		@WebParam(partName = "addUpdateRemoveAssignment") @QueryParam("addUpdateRemoveAssignment") String addUpdateRemoveAssignment,
		@WebParam(partName = "assignmentTitle") @QueryParam("assignmentTitle") String assignment_Title,
		@WebParam(partName = "assignmentMaxPoints") @QueryParam("assignmentMaxPoints") Double assignment_maxPoints,
		@WebParam(partName = "assignmentDueTime") @QueryParam("assignmentDueTime") long assignment_dueTime
    )
	{
        // Check to make sure session is valid.
        Session s = establishSession(sessionID);

        // Require permission.
        if (!hasPermission(gradebookID, "gradebook.gradeAll"))
        {
 			throw new RuntimeException("Non-permitted user trying to update scorable item: " + s.getUserId());
        }

        // Add assignment.
        if (addUpdateRemoveAssignment.equals("add"))
        {
            if (gradebookExternalAssessmentService.isExternalAssignmentDefined(gradebookID, assignmentID))
            {
                // Update assignment in gradebook.
                gradebookExternalAssessmentService.updateExternalAssessment(
                    gradebookID,
                    assignmentID,
                    "",
                    assignment_Title,
                    assignment_maxPoints,
                    new Date(assignment_dueTime),
                    false
                );
            }
            else
            {
                // Add assignment to gradebook.
                gradebookExternalAssessmentService.addExternalAssessment(
                    gradebookID,
                    assignmentID,
                    "",
                    assignment_Title,
                    assignment_maxPoints,
                    new Date(assignment_dueTime),
                    "MH Gradebook",
                    false
                );

                //release assessment to assignment
                //g.setExternalAssessmentToGradebookAssignment(gradebookID, assignmentID);
            }

        }

        // Remove.
        if (addUpdateRemoveAssignment.equals("remove"))
        {
            // Remove assignment from gradebook.
            if (gradebookExternalAssessmentService.isExternalAssignmentDefined(gradebookID, assignmentID))
            {
				gradebookExternalAssessmentService.removeExternalAssessment(gradebookID, assignmentID);
			}
        }
	}

    @WebMethod(exclude = true)
    public void setGradebookExternalAssessmentService(GradebookExternalAssessmentService gradebookExternalAssessmentService) {
        this.gradebookExternalAssessmentService = gradebookExternalAssessmentService;
    }

    /**
    *
    * @param String gradebookID
    * @param String permission
    * @return boolean
    */
    private boolean hasPermission(String gradebookID, String permission) {
       // Make sure the user has update all scores permission (typically an Instructor).
       User user = userDirectoryService.getCurrentUser();
       String siteRef = siteService.siteReference(gradebookID);

       return securityService.unlock(user, permission, siteRef);
   }

    /**
     * Returns the user id from the direcotry service.
     *
     * @param String userID
     * @return String
     */
    private String getUserId(String userID) {
		String uid;
		try {
			uid = userDirectoryService.getUserId(userID);
		}
		catch(Exception e) {
			uid = userID;
		}
        return uid;
    }

    /**
     * Returns the user eid from the direcotry service.
     *
     * @param String uid
     * @return String
     */
    private String getUserEid(String uid) {
        String eid;
		try {
			eid = userDirectoryService.getUserEid(uid);
		}
		catch(Exception e) {
			eid = uid;
		}
        return eid;
    }

}
