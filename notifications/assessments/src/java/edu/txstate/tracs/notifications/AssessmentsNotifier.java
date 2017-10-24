package edu.txstate.tracs.notifications;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Observable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.authz.api.Member;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.event.api.EventTrackingService;

import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentAccessControlIfc;
import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentFeedbackIfc;
import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentIfc;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacadeQueriesAPI;
import org.sakaiproject.tool.assessment.services.assessment.PublishedAssessmentService;
import org.sakaiproject.tool.assessment.services.PersistenceService;

/*
Assessments Assessment opens - entire class
Assessments Assessment opens - specific group
Assessments Assessment opens - section
Assessments Assessment due date approaching (reminder)
Assessments Assessment feedback available (after student submits, setting on assignment)
Assessments Assessment feedback available (future date, setting on assignment)
*/

public class AssessmentsNotifier implements Observer {
    public static final String PUBLISH_ASSESSMENT_EVENT = "sam.assessment.publish";
    public static final String SUBMIT_ASSESSMENT_EVENT = "sam.assessment.submit";
    private final Log log = LogFactory.getLog(AssessmentsNotifier.class);
    private List<String> updates;

    private EventTrackingService eventTrackingService;
    public void setEventTrackingService(EventTrackingService service) {
        eventTrackingService = service;
    }

    private EntityManager entityManager;
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private PublishedAssessmentFacadeQueriesAPI pafqa;
    private PublishedAssessmentService publishedAssessmentService;

    private NotifyUtils notifyUtils;
    public void setNotifyUtils(NotifyUtils notifyUtils) {
        this.notifyUtils = notifyUtils;
    }

    private SiteService siteService;
    public void setSiteService(SiteService siteService) {
      this.siteService = siteService;
    }

    public void init() {
        publishedAssessmentService = new PublishedAssessmentService();
        PersistenceService persistenceService = PersistenceService.getInstance();
        pafqa = persistenceService.getPublishedAssessmentFacadeQueries();
        updates = new ArrayList<String>();
        updates.add(PUBLISH_ASSESSMENT_EVENT);
        updates.add(SUBMIT_ASSESSMENT_EVENT);
        eventTrackingService.addObserver(this);
    }

    public void update(Observable o, Object arg) {
        Event event = (Event) arg;
        try {
            if (updates.contains(event.getEvent())) {
                if (PUBLISH_ASSESSMENT_EVENT.equals(event.getEvent())) {
                    //resource string looks like this:
                    //siteId=caf34c5d-439c-4b8f-a95d-b455fd0e9012, assessmentId=18, publishedAssessmentId=10
                    String resourceStr = event.getResource();
                    Map<String,String> map = parseResourceString(resourceStr);
                    String assessmentId = map.get("assessmentId");
                    String publishedAssessmentIdStr = map.get("publishedAssessmentId");
                    Long publishedAssessmentId = Long.valueOf(publishedAssessmentIdStr);
                    AssessmentIfc assessment = publishedAssessmentService.getAssessment(publishedAssessmentId);
                    String creatorId = assessment.getCreatedBy();

                    // When will the assessment be open, visible to students
                    AssessmentAccessControlIfc accessControl = assessment.getAssessmentAccessControl();
                    Date startDate = accessControl.getStartDate();
                    if (null == startDate) {
                        return;
                    }
                    Calendar releaseDate = Calendar.getInstance();
                    releaseDate.setTime(startDate);

                    //get the due date
                    Date dueDate = accessControl.getDueDate();
                    Calendar calendarDueDate = Calendar.getInstance();
                    calendarDueDate.setTime(dueDate);

                    // Sending the due date in the content has because people will care if that changes
                    String contentHash = notifyUtils.hashContent(assessment.getTitle(), notifyUtils.dateToJson(calendarDueDate));

                    String releaseTo = accessControl.getReleaseTo();

                    List<String> userids = getNotifyList(event.getContext(), creatorId, publishedAssessmentIdStr, releaseTo);

                    //send notification that the assessment is open
                    notifyUtils.sendNotification("assessment", "creation", publishedAssessmentIdStr, event.getContext(), userids, releaseDate, contentHash, true);

                    //create a reminder notification (1 day before due date)
                    String reminderContentHash = notifyUtils.hashContent(assessment.getTitle(), "Assessment Due Soon");
                    Calendar reminderDate = (Calendar) calendarDueDate.clone();
                    reminderDate.add(Calendar.DAY_OF_YEAR, -1);
                    notifyUtils.sendNotification("assessment", "reminder", publishedAssessmentIdStr, event.getContext(), userids, releaseDate, reminderContentHash, true);

                    //is feedback set to be available on some particular day?
                    AssessmentFeedbackIfc assessmentFeedback = assessment.getAssessmentFeedback();
                    if (assessmentFeedback.getFeedbackDelivery() == AssessmentFeedbackIfc.FEEDBACK_BY_DATE) {
                        Date feedbackDate = accessControl.getFeedbackDate();
                        Calendar feedbackReleaseDate = Calendar.getInstance();
                        feedbackReleaseDate.setTime(feedbackDate);
                        String feedbackContentHash = notifyUtils.hashContent(assessment.getTitle(), "Feedback available on assessment");
                        notifyUtils.sendNotification("assessment", "feedback", publishedAssessmentIdStr, event.getContext(), userids, feedbackReleaseDate, feedbackContentHash, true);
                    }
                }
                else if (SUBMIT_ASSESSMENT_EVENT.equals(event.getEvent())) {
                    //resource string looks like: publishedAssessmentId=17, siteId=caf34c5d-439c-4b8f-a95d-b455fd0e9012, submissionId=3
                    String resourceStr = event.getResource();
                    Map<String,String> map = parseResourceString(resourceStr);
                    String publishedAssessmentId = map.get("publishedAssessmentId");
                    AssessmentIfc assessment = publishedAssessmentService.getAssessment(Long.valueOf(publishedAssessmentId));
                    AssessmentFeedbackIfc assessmentFeedback = assessment.getAssessmentFeedback();
                    String submitter = event.getUserId();
                    if (assessmentFeedback.getFeedbackDelivery() == AssessmentFeedbackIfc.FEEDBACK_ON_SUBMISSION) {
                        Calendar submissionDate = Calendar.getInstance();
                        String contentHash = notifyUtils.hashContent(assessment.getTitle(), "Feedback available on assessment");
                        List<String> userids = new ArrayList<String>();
                        userids.add(submitter);
                        userids = notifyUtils.convertUserIdsInSite(event.getContext(), userids);
                        notifyUtils.sendNotification("assessment", "submission", publishedAssessmentId, event.getContext(), userids, submissionDate, contentHash, true);
                    }
                    //Cancel the reminder notification no more submissions are allowed
                    AssessmentAccessControlIfc accessControl = assessment.getAssessmentAccessControl();
                    if (!accessControl.getUnlimitedSubmissions()) {
                        int totalSubmissionsForUser = publishedAssessmentService.getTotalSubmission(submitter, publishedAssessmentId);
                        int remainingSubmissions = accessControl.getSubmissionsAllowed() - totalSubmissionsForUser;
                        if(remainingSubmissions == 0)
                            notifyUtils.deleteForObjectAndUser("assessment", "reminder", publishedAssessmentId, submitter);
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getNotifyList(String siteid, String creator, String publishedAssessmentId, String releaseTo) throws Exception {
        List<String> userids = new ArrayList<String>();
        switch (releaseTo) {
            case AssessmentAccessControlIfc.RELEASE_TO_SELECTED_GROUPS:
                List<String> groups = pafqa.getReleaseToGroupIdsForPublishedAssessment(publishedAssessmentId);
                Set<String> notifySet = new HashSet<String>();
                for (String groupId : groups) {
                    Group group = siteService.findGroup(groupId);
                    Set<Member> groupMembers = group.getMembers();
                    for (Member m : groupMembers) {
                        notifySet.add(m.getUserEid());
                    }
                }
                userids.addAll(notifySet);
                break;
            case "Anonymous Users":
                //Whom would we notify?
            default :
                //all site members
                userids = notifyUtils.getAllUserIdsExcept(siteid,creator);
        }
        
        return userids;
    }

    public Map<String, String> parseResourceString(String resource) {
        String[] params = resource.split(", ");
        Map<String,String> map = new HashMap<String,String>();
        for (String param : params) {
            map.put(param.split("=")[0], param.split("=")[1]);
        }
        return map;
    }
}