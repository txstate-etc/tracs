package edu.txstate.tracs.notifications;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observer;
import java.util.Observable;
import java.util.Set;

import java.util.Iterator;

import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.Event;

import org.sakaiproject.api.app.messageforums.ActorPermissions;
import org.sakaiproject.api.app.messageforums.DBMembershipItem;
import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionForumService;
import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.api.app.messageforums.MessageForumsMessageManager;
import org.sakaiproject.api.app.messageforums.DateRestrictions;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.OpenForum;

import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;

//Forums is the msgcntr module.  That module also handles the private messages tool

/************************************************************************************
 *
 * Notes
 *
 * Forums and Topics can be drafts, Messages can't.
 * The references from the events have the form: /type/ID/siteID/Message/MessageID/userID
 * Forums and Topics can have open and close dates, Messages do not.
 * Students don't need to be notified when a new forum is created because they can't see it
 *     until a topic is added.
 * 
 *
 ***********************************************************************************/

public class ForumsNotifier implements Observer {
    private List<String> updates;

    private EventTrackingService eventTrackingService;
    public void setEventTrackingService(EventTrackingService service) {
        eventTrackingService = service;
    }

    private EntityManager entityManager;
    public void setEntityManager(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    private DiscussionForumService forumsService;
    public void setForumsService(DiscussionForumService forumsService) {
      this.forumsService = forumsService;
    }

    private MessageForumsMessageManager messageManager;
    public void setMessageManager(MessageForumsMessageManager messageManager){
        this.messageManager = messageManager;
    }

    private DiscussionForumManager discussionForumManager;
    public void setDiscussionForumManager(DiscussionForumManager discussionForumManager){
        this.discussionForumManager = discussionForumManager;
    }

    private NotifyUtils notifyUtils;
    public void setNotifyUtils(NotifyUtils notifyUtils) {
      this.notifyUtils = notifyUtils;
    }

    public void init() {
      updates = new ArrayList<String>();
      updates.add(DiscussionForumService.EVENT_FORUMS_ADD); //adding a conversation to a topic
      //This SHOULD be the add topic event, but actually it sends 2 reviseforum events and a revisetopic event
      //The "add topic" event is never sent
      updates.add(DiscussionForumService.EVENT_FORUMS_TOPIC_ADD);

      eventTrackingService.addObserver(this);
    }

    public void update(Observable o, Object arg) {
        Event event = (Event) arg;

        //If the event pertains to forums
        if (updates.contains(event.getEvent())) {
            String eventType = event.getEvent();
            switch(eventType){
                case DiscussionForumService.EVENT_FORUMS_ADD:
                    try{
                        System.out.println("A conversation was added. " + "reference: " +  event.getResource());
                        Reference ref = entityManager.newReference(event.getResource());
                        //There doesn't seem to be any other method to get the message ID
                        String[] refParts = ref.getReference().split("/");
                        String messageId = refParts[refParts.length-2];
                        Message m = messageManager.getMessageById(Long.parseLong(messageId));
                        //need to check topic and forum availability dates.
                        DiscussionTopic topic = (DiscussionTopic) m.getTopic();
                        OpenForum openForum = topic.getOpenForum();
                        Calendar releaseDate = Calendar.getInstance();
                        //get the forum that contains the message
                        String forumIdForMessage = discussionForumManager.ForumIdForMessage(Long.parseLong(messageId));
                        DiscussionForum discussionForum = discussionForumManager.getForumById(Long.parseLong(forumIdForMessage));
                        System.out.println("forum open date: " + discussionForum.getOpenDate());
                        System.out.println("topic open date: " + topic.getOpenDate());
                        if(topic.getOpenDate() != null && discussionForum.getOpenDate() != null){
                            Date laterAvailability = discussionForum.getOpenDate().after(topic.getOpenDate()) ? discussionForum.getOpenDate() : topic.getOpenDate();
                            Calendar available = Calendar.getInstance();
                            available.setTime(laterAvailability);
                            if(available.after(Calendar.getInstance())){
                                releaseDate = available;
                            }
                        }
                        String contenthash = notifyUtils.hashContent(m.getTitle(), m.getBody());
                        System.out.println("content hash = "+contenthash);

                        // get list of users to notify: a set of userIds for the site members who have 
                        // "read" permission for the given topic
                        Set allowedUsers = discussionForumManager.getUsersAllowedForTopic(topic.getId(), true, false);
                        Iterator<String> uit = allowedUsers.iterator();
                        //put the allowed users in a list, exclude the auther of the post because they don't 
                        //need to be notified
                        List<String> userids = new ArrayList<String>();
                        while(uit.hasNext()){
                            String userid = uit.next();
                            if(!userid.equals(event.getUserId()))
                                userids.add(userid);
                        }
                        
                        //check if either topic or forum is a draft
                        Boolean forumIsDraft = discussionForum.getDraft();
                        Boolean topicIsDraft = topic.getDraft();
                        System.out.println("Forum is draft: " + forumIsDraft + ", Topic is draft: " + topicIsDraft);
                        if(forumIsDraft || topicIsDraft){
                            //delete any scheduled notifications for this message?  How?
                        }
                        else if(releaseDate.compareTo(Calendar.getInstance()) <= 0){
                            //if the release date is now or in the past, send notification
                            notifyUtils.sendNotification("discussion", "creation", m.getUuid(), event.getContext(), userids, releaseDate, contenthash);
                        }
                        else if(releaseDate.after(Calendar.getInstance())){
                            //it's scheduled for the future
                            System.out.println("This message will be available after " + notifyUtils.dateToJson(releaseDate));
                            //don't we still send it and Dispatch will push it out at the appropriate time?
                            notifyUtils.sendNotification("discussion", "creation", m.getUuid(), event.getContext(), userids, releaseDate, contenthash);
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    break;
                case DiscussionForumService.EVENT_FORUMS_TOPIC_ADD:
                    //this event never actually happens, even when a new topic is created.
                    //a revisetopic event is sent instead so maybe there is some way to use that
                    break;
                default:
                    //this shouldn't happen
            }
        }
    }
}