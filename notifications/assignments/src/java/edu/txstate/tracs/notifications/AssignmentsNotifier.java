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
import org.sakaiproject.entity.api.Entity;
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
* Assignment due date approaching (reminder)
Group Assignment - Member of group submits
Turnitin report generated for submission
* Assignment grade released (No email notification option selected in settings)
* Assignment grade released (Email notification option selected in settings)
* Peer evaluation opens on Assignment
Peer evaluation posted for Assignment (non-anonymous evaluation)
Peer evaluation posted for Assignment (anonymous evaluation)
Multiple peer evaluations set on assignment, and assignment is evaluated

TODO: There are no events for turnitin or peer evaluations.
TODO: If you duplicate an assignment, it creates a new one as a draft. When you later save it, 
      the events sent out are asn.revise.assignmentcontent, asn.revise.assignment, and
      asn.revise.{title | access | whateveryouchanged}. Need to capture that somehow to send
      a notification that the new, duplicated assignment is available.
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
        updates.add(AssignmentConstants.EVENT_SUBMIT_GROUP_ASSIGNMENT_SUBMISSION);
        updates.add(AssignmentConstants.EVENT_GRADE_RELEASED);
        eventTrackingService.addObserver(this);
    }

    public void update(Observable o, Object arg) {
        Event event = (Event) arg;
        String eventType = event.getEvent();
        //System.out.println("received "+event.getEvent()+" event for ticket " + event.getResource());

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

                        //Get list of users to notify
                        List<String> userids = getNotifyList(event.getContext(), creatorId, assignment);
                        //for (String uid : userids) System.out.println("userid: "+uid);

                        //Send Notification
                        if (isDraft) {
                            System.out.println("assignment is in draft mode, delete any scheduled notification");
                            notifyUtils.deleteForObject("assignment", assignmentId);
                        }
                        else  {
                            notifyUtils.sendNotification("assignment", "creation", assignmentId, event.getContext(), userids, releaseDate, contenthash, true);

                            //add notification to remind them when assignment is due. SET TO ONE DAY BEFORE
                            Time dueTime = assignment.getDueTime();
                            Calendar dueDate = Calendar.getInstance();
                            dueDate.setTimeInMillis(dueTime.getTime());
                            Calendar reminderDate = (Calendar) dueDate.clone();
                            reminderDate.add(Calendar.DAY_OF_YEAR, -1);
                            String reminderContentHash = notifyUtils.hashContent(assignment.getTitle(), "Assignment due soon");
                            notifyUtils.sendNotification("assignment", "reminder", assignmentId, event.getContext(), userids, reminderDate , reminderContentHash, true);

                            //if the assignment has peer evaulation, notify users when it begins
                            Time closeTime = assignment.getCloseTime();
                            Calendar closeDate = Calendar.getInstance();
                            closeDate.setTimeInMillis(closeTime.getTime());
                            if (assignment.getAllowPeerAssessment() ) {
                                String peerEvalContentHash = notifyUtils.hashContent(assignment.getTitle(), "Peer evaluation open");
                                notifyUtils.sendNotification("assignment", "peer_evaluation", assignmentId, event.getContext(), userids, closeDate, peerEvalContentHash, true);
                            }
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case AssignmentConstants.EVENT_SUBMIT_GROUP_ASSIGNMENT_SUBMISSION:
                    try {
                        String submissionId = ref.getId();
                        String contenthash = notifyUtils.hashContent("Group assignment submitted");
                        String reference = ref.getReference();
                        int index = reference.lastIndexOf(Entity.SEPARATOR);
                        if (index == -1) return;
                        String groupId = reference.substring(index + 1);
                        Group group = siteService.findGroup(groupId);
                        if (null == group) return;
                        Set<Member> groupMembers = group.getMembers();
                        List<String> userids = new ArrayList<String>();
                        String submitterId = event.getUserId();
                        for (Member m : groupMembers) {
                            if (!submitterId.equals(m.getUserId()))
                                userids.add(m.getUserEid());
                        }
                        //TODO: should the object ID be the submission ID or the assignment ID?
                        notifyUtils.sendNotification("assignment", "groupsubmission", submissionId, event.getContext(), userids, Calendar.getInstance(), contenthash, true);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case AssignmentConstants.EVENT_GRADE_RELEASED:
                    try {
                        String submissionId = ref.getId();
                        AssignmentSubmission submission = assignService.getSubmission(submissionId);
                        String assignmentId = submission.getAssignmentId();
                        //TODO: Not sure what to use for content hash here?
                        String contenthash = notifyUtils.hashContent("Your assignment has been graded", submission.getGrade());
                        List<String> submitters = notifyUtils.convertUserIdsInSite(event.getContext(),submission.getSubmitterIds());
                        //TODO: should the object ID here be the assignment or the submission?
                        notifyUtils.sendNotification("assignment", "grade", assignmentId, event.getContext(), submitters, Calendar.getInstance() , contenthash, true);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
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