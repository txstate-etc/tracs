package edu.txstate.tracs.notifications;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Observer;
import java.util.Observable;
import java.util.Set;

import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.Event;

import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionForumService;
import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.api.app.messageforums.MessageForumsMessageManager;
import org.sakaiproject.api.app.messageforums.DateRestrictions;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.Topic;

import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;

//Forums is the msgcntr module.  That module also handles the private messages tool

/************************************************************************************
 *
 * Notes
 *
 * Forums and Topics can be drafts, Messages can't.
 * The references from the events have the form: /type/ID/siteID/Message/MessageID/userID
 * Forums and Topics can have open and close dates, Messages do not.
 * The open and close dates are rounded to the nearest 5 minutes.  So an open date can be
 *     5:30 or 10:25, but it can't be 4:31 or 9:37
 * Students don't need to be notified when a new forum is created because they can't see it
 *     until a topic is added.
 * Sakai 10 does not have an event for 'New Topic,' but Sakai 11 does
 *
 *
 * TODO: Smarter logic for sending notifications.  Notify them if it is a new discussion,
 * if they have previously commented in a discussion, or if the instructor makes a comment
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
      updates.add(DiscussionForumService.EVENT_FORUMS_TOPIC_ADD); //event not sent in TRACS 10, sent in TRACS 11
      updates.add(DiscussionForumService.EVENT_FORUMS_MESSAGE_APPROVE);

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
                        long messageId = getMessageIdFromEvent(event);
                        Message m = messageManager.getMessageById(messageId);

                        //get the message's topic
                        DiscussionTopic topic = (DiscussionTopic) m.getTopic();
                        //get the forum that contains the message
                        String forumIdForMessage = discussionForumManager.ForumIdForMessage(messageId);
                        DiscussionForum discussionForum = discussionForumManager.getForumById(Long.parseLong(forumIdForMessage));
                        Calendar releaseDate = getReleaseDate(discussionForum, topic);

                        //This will send a notification for every single message.
                        //Should contenthash just contain "You have a new message in forums?"
                        String contenthash = notifyUtils.hashContent(m.getTitle(), m.getBody());

                        //Get list of users to notify
                        List<String> userids = getNotifyList(topic.getId(), event.getUserId());

                        //check if either topic or forum is a draft
                        Boolean forumIsDraft = discussionForum.getDraft();
                        Boolean topicIsDraft = topic.getDraft();

                        Calendar now = Calendar.getInstance();

                        if(forumIsDraft || topicIsDraft){
                            //delete any scheduled notifications for this message?  How?
                        }
                        else if(null == m.getApproved()){
                            System.out.println("*** Message not approved yet ***");
                            //notifications should not be sent when a new message is created in a moderated forum
                        }
                        else if(releaseDate.compareTo(now) <= 0){
                            //if the release date is now or in the past, send notification
                            notifyUtils.sendNotification("discussion", "creation", m.getUuid(), event.getContext(), userids, releaseDate, contenthash, false);
                        }
                        else if(releaseDate.after(now)){
                            //it's scheduled for the future
                            //don't we still send it and Dispatch will push it out at the appropriate time?
                            notifyUtils.sendNotification("discussion", "creation", m.getUuid(), event.getContext(), userids, releaseDate, contenthash, false);
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    break;
                case DiscussionForumService.EVENT_FORUMS_MESSAGE_APPROVE:
                    try{
                        long messageId = getMessageIdFromEvent(event);
                        Message m = messageManager.getMessageById(messageId);
                        Topic topic = m.getTopic();
                        //If a non-admin wrote a message, the topic and forum must be visible already
                        //Admin messages in moderated forums are not held for moderation
                        Calendar releaseDate = Calendar.getInstance();

                        String contenthash = notifyUtils.hashContent(m.getTitle(), m.getBody());

                        //Note: In order to make this work, I had to set lazy loading to false on the Topic in
                        //$TRACS_REPO_PATH/msgcntr/messageforums-hbm/src/java/org/sakaiproject/component/app/messageforums/dao/hibernate/MessageImpl.java
                        //because a hibernate proxy was being returned for the Topic instead of a usable object
                        List<String> userids = getNotifyList(topic.getId(), event.getUserId());

                        notifyUtils.sendNotification("discussion", "creation", m.getUuid(), event.getContext(), userids, releaseDate, contenthash, false);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    break;
                case DiscussionForumService.EVENT_FORUMS_TOPIC_ADD:
                    System.out.println("new topic added");

                    break;
                default:
                    //this shouldn't happen
            }
        }
    }

    public long getMessageIdFromEvent(Event event) throws Exception{
        Reference ref = entityManager.newReference(event.getResource());
        //There doesn't seem to be any other method to get the message ID
        String[] refParts = ref.getReference().split("/");
        String messageId = refParts[refParts.length-2];
        if(null == messageId){
            throw new Exception("Message ID not found");
        }
        return Long.parseLong(messageId);
    }

    public Calendar getReleaseDate(DiscussionForum forum, DiscussionTopic topic){
        Calendar releaseDate = Calendar.getInstance();
        if(topic.getOpenDate() != null && forum.getOpenDate() != null){
            Date laterAvailability = forum.getOpenDate().after(topic.getOpenDate()) ? forum.getOpenDate() : topic.getOpenDate();
            Calendar available = Calendar.getInstance();
            available.setTime(laterAvailability);
            if(available.after(Calendar.getInstance())){
                releaseDate = available;
            }
        }
        return releaseDate;
    }

    public List<String> getNotifyList(long topicId, String author){
        // get list of users to notify: a set of userIds for the site members who have
        // "read" permission for the given topic
        Set allowedUsers = discussionForumManager.getUsersAllowedForTopic(topicId, true, false);
        Iterator<String> uit = allowedUsers.iterator();
        //put the allowed users in a list, exclude the auther of the post because they don't
        //need to be notified
        List<String> userids = new ArrayList<String>();
        while(uit.hasNext()){
            String userid = uit.next();
            if(!userid.equals(author))
                userids.add(userid);
        }
        return userids;
    }
}
