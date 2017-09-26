package edu.txstate.tracs.notifications;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Observer;
import java.util.Observable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.authz.api.Member;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.user.api.User;

import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.Assignment.AssignmentAccess;
import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentSubmission;
/*
* Assignment opens - entire class
* Assignment opens - specific group
* Assignment opens - section
* Group Assignment opens
* Assignment due date approaching (reminder) HOW LONG BEFORE DUE DATE?
Group Assignment - Member of group submits
Turnitin report generated for submission
Assignment grade released (No email notification option selected in settings)
Assignment grade released (Email notification option selected in settings)
Peer evaluation opens on Assignment
Peer evaluation posted for Assignment (non-anonymous evaluation)
Peer evaluation posted for Assignment (anonymous evaluation)
Multiple peer evaluations set on assignment, and assignment is evaluated

TODO: There are no events for turnitin or peer evaluations.
*/

public class AssignmentsNotifier implements Observer {
    private final Log log = LogFactory.getLog(AssignmentsNotifier.class);
    private List<String> updates;

    private EventTrackingService eventTrackingService;
    public void setEventTrackingService(EventTrackingService service) {
        eventTrackingService = service;
    }

    private EntityManager entityManager;
    public void setEntityManager(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    private AssignmentService assignService;
    public void setAssignService(AssignmentService assignService) {
      this.assignService = assignService;
    }

    private NotifyUtils notifyUtils;
    public void setNotifyUtils(NotifyUtils notifyUtils) {
      this.notifyUtils = notifyUtils;
    }

    private SiteService siteService;
    public void setSiteService(SiteService siteService) {
      this.siteService = siteService;
    }

    public void init() {
        updates = new ArrayList<String>();
        updates.add(AssignmentConstants.EVENT_ADD_ASSIGNMENT);
        updates.add(AssignmentConstants.EVENT_ADD_ASSIGNMENT_SUBMISSION);
        updates.add(AssignmentConstants.EVENT_SUBMIT_ASSIGNMENT_SUBMISSION);
        updates.add(AssignmentConstants.EVENT_GRADE_ASSIGNMENT_SUBMISSION); //save grade and release to student or dont. same event!
        eventTrackingService.addObserver(this);
    }

    public void update(Observable o, Object arg) {
        Event event = (Event) arg;
        String eventType = event.getEvent();
        System.out.println("received "+event.getEvent()+" event for ticket " + event.getResource());

        if (updates.contains(event.getEvent())) {
            Reference ref = entityManager.newReference(event.getResource());
            switch(eventType){
                case AssignmentConstants.EVENT_ADD_ASSIGNMENT:
                    try {
                        String assignmentId = ref.getId();
                        Assignment assignment = assignService.getAssignment(assignmentId);
                        if (null == assignment) {
                            //not a valid assignment
                            return;
                        }

                        boolean isDraft = assignment.getDraft();

                        String contenthash = notifyUtils.hashContent(assignment.getTitle(), assignment.getContent().getInstructions());

                        String creatorId = assignment.getCreator();

                        //Get the assignment release date
                        Time releaseDateTime = assignment.getOpenTime();
                        Calendar releaseDate = Calendar.getInstance();
                        releaseDate.setTimeInMillis(releaseDateTime.getTime());
                        Calendar now = Calendar.getInstance();
                        //boolean assignmentAvailable = releaseDate.before(now)) || releaseDate.equals(now);

                        //Get list of users to notify
                        List<String> userids = getNotifyList(event.getContext(), creatorId, assignment);
                        for (String uid : userids) System.out.println("userid: "+uid);

                        //Send Notification
                        if (isDraft) {
                            System.out.println("message is in draft mode, delete any scheduled notification");
                            notifyUtils.deleteForObject("assignment", ref.getId());
                        }
                        else  {
                            notifyUtils.sendNotification("assignment", "creation", assignmentId, event.getContext(), userids, releaseDate, contenthash, true);
                            //add notification to remind them when assignment is due. SET TO ONE DAY BEFORE
                            Calendar reminderDate = (Calendar) releaseDate.clone();
                            reminderDate.add(Calendar.DAY_OF_YEAR, -1);
                            notifyUtils.sendNotification("assignment", "creation", assignmentId, event.getContext(), userids, reminderDate , contenthash, true);
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case AssignmentConstants.EVENT_SUBMIT_ASSIGNMENT_SUBMISSION:
                    try {
                        String submissionId = ref.getId();
                        System.out.println("Submission ID is " + submissionId);
                        // *** TODO: This is not working because the student submitting the assignment
                        // does not have permission to access the submission. ***

                        // AssignmentSubmission submission = assignService.getSubmission(submissionId);
                        // Assignment assignment = submission.getAssignment();
                        // if (null == assignment) {
                        //     //not a valid assignment
                        //     return;
                        // }
                        // System.out.println("Submitted Assignment: " + assignment.getTitle());
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case AssignmentConstants.EVENT_GRADE_ASSIGNMENT_SUBMISSION:
                    //TODO: it's the same event whether the grade is released or not. We need another event
                    break;
                default:
            }
        }
    }

    //right now, I think this will also notify the TA
    public List<String> getNotifyList(String siteid, String creator, Assignment assignment) throws Exception {
        AssignmentAccess assignmentAccess = assignment.getAccess();
        List<String> userids = new ArrayList<String>();
        if (AssignmentAccess.GROUPED == assignmentAccess) {
            //use a HashSet to avoid duplicates
            Set<String> notifySet = new HashSet<String>();
            Collection<String> groups = assignment.getGroups();
            Iterator<String> it = groups.iterator();
            while (it.hasNext()) {
                String groupId = it.next();
                Group group = siteService.findGroup(groupId);
                Set<Member> groupMembers = group.getMembers();
                for (Member m : groupMembers) {
                    notifySet.add(m.getUserEid());
                }
            }
            userids.addAll(notifySet);
        }
        else {
            userids = notifyUtils.getAllUserIdsExcept(siteid,creator);
        }
        return userids;
    }

}