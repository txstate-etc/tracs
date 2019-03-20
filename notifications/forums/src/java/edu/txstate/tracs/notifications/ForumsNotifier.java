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
import org.sakaiproject.tool.cover.SessionManager;

import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.OpenForum;
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
    private List<String> messageEvents;
    private List<String> topicEvents;
    private List<String> forumEvents;

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
      messageEvents = new ArrayList<String>();
      messageEvents.add(DiscussionForumService.EVENT_FORUMS_ADD);
      messageEvents.add(DiscussionForumService.EVENT_FORUMS_RESPONSE);
      messageEvents.add(DiscussionForumService.EVENT_FORUMS_MESSAGE_APPROVE);
      messageEvents.add(DiscussionForumService.EVENT_FORUMS_MESSAGE_DENY);
      messageEvents.add(DiscussionForumService.EVENT_FORUMS_REMOVE);

      topicEvents = new ArrayList<String>();
      topicEvents.add(DiscussionForumService.EVENT_FORUMS_TOPIC_REVISE);

      forumEvents = new ArrayList<String>();
      forumEvents.add(DiscussionForumService.EVENT_FORUMS_REVISE);

      eventTrackingService.addObserver(this);
    }

    public String getMessageId(String siteid, Message m) {
      return "/forums/site/"+siteid+"/forum/"+m.getTopic().getBaseForum().getId()+"/topic/"+m.getTopic().getId()+"/message/"+m.getId();
    }

    //This version is used for the approve and deny moderation events.  For some reason, getBaseForum() returns null for those.
    public String getMessageId(String siteid, OpenForum f, Topic t, Message m){
        return "/forums/site/" + siteid + "/forum/" + f.getId() + "/topic/" + t.getId() + "/message/" + m.getId();
    }

    //Really just guessing here.  I don't know what the app is expecting.
    public String getTopicId(String siteid, Topic t) {
      return "/forums/site/"+siteid+"/forum/"+t.getBaseForum().getId()+"/topic/"+t.getId();
    }

    public void update(Observable o, Object arg) {
        Event event = (Event) arg;
        //If the event pertains to forums
        if (messageEvents.contains(event.getEvent())) {
            String eventType = event.getEvent();
            String siteid = event.getContext();
            //If the tool is hidden (in site info) we don't want to send any notifications
            if(notifyUtils.toolIsHidden(siteid, DiscussionForumService.FORUMS_TOOL_ID)) return;
            switch(eventType){
                case DiscussionForumService.EVENT_FORUMS_ADD:
                    try{
                        long messageId = getMessageIdFromEvent(event);
                        Message m = messageManager.getMessageById(messageId);

                        //get the message's topic
                        DiscussionTopic topic = (DiscussionTopic) m.getTopic();
                        //get the forum that contains the message
                        String forumIdForMessage = discussionForumManager.ForumIdForMessage(messageId);
                        DiscussionForum discussionForum = discussionForumManager.getForumById(Long.parseLong(forumIdForMessage));
                        Calendar releaseDate = getReleaseDate(discussionForum, topic);

                        String contenthash = notifyUtils.hashContent(m.getTitle(), m.getBody());

                        //Get list of users who have read access to this topic, minus the author
                        List<String> usersWithReadAccess = getNotifyList(siteid, topic.getId(), event.getUserId());

                        List<String> sendList = new ArrayList<String>();

                        //If the instructor or TA is creating a new thread, notify users with read access
                        if(isNewThread(m)){
                            if(authorIsInstructor() || authorIsTA()) {
                                sendList = usersWithReadAccess;
                            }
                        }
                        else {
                            //This is not the first message in the thread.  Notify people who have 
                            //already commented in the thread.
                            sendList = getThreadParticipants(siteid, m);
                        }

                        if(sendList.size() == 0) break;

                        //check if either topic or forum is a draft
                        Boolean forumIsDraft = discussionForum.getDraft();
                        Boolean topicIsDraft = topic.getDraft();

                        Calendar now = Calendar.getInstance();

                        if(forumIsDraft || topicIsDraft){
                            //delete any scheduled notifications for this message?  How?
                        }
                        else if(null == m.getApproved()){
                            //notifications should not be sent when a new message is created in a moderated forum
                        }
                        else if(releaseDate.compareTo(now) <= 0){
                            //if the release date is now or in the past, send notification
                            notifyUtils.sendNotification("discussion", "creation", getMessageId(siteid, m), event.getContext(), sendList, releaseDate, contenthash, false);
                        }
                        else if(releaseDate.after(now)){
                            //it's scheduled for the future
                            //don't we still send it and Dispatch will push it out at the appropriate time?
                            //Could this result in the student getting a bunch of messages from the instructor all at once?
                            notifyUtils.sendNotification("discussion", "creation", getMessageId(siteid, m), event.getContext(), sendList, releaseDate, contenthash, false);
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    break;
                case DiscussionForumService.EVENT_FORUMS_RESPONSE:
                    //In this case, the instructor or TA revised someone's post
                    //Can students revise each other's posts?  I don't think they can...
                    try{
                        long messageId = getMessageIdFromEvent(event);
                        Message m = messageManager.getMessageById(messageId);
                        //get the message's topic
                        Topic topic = m.getTopic();
                        List<String> sendList = new ArrayList<String>();
                        sendList.add(m.getCreatedBy());
                        Calendar releaseDate = Calendar.getInstance();
                        String contenthash = notifyUtils.hashContent("Your forums post has been modified");
                        //TODO: This is not working because this event is also sent when someone responds to a post.
                        //We might need to add another new event to tracs, specifically for revisions.
                        //notifyUtils.sendNotification("discussion", "update", getMessageId(siteid, m), event.getContext(), notifyUtils.convertUserIdsInSite(siteid, sendList), releaseDate, contenthash, false);
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
                        String objectId = getMessageId(siteid, topic.getOpenForum(), topic, m);
                        //If a non-admin wrote a message, the topic and forum must be visible already
                        //Admin messages in moderated forums are not held for moderation
                        Calendar releaseDate = Calendar.getInstance();
                        String contenthash = notifyUtils.hashContent(m.getTitle(), m.getBody());
                        List<String> sendList = new ArrayList<String>();
                        sendList.add(m.getCreatedBy());
                        notifyUtils.sendNotification("discussion", "approval", objectId, event.getContext(), notifyUtils.convertUserIdsInSite(siteid, sendList), releaseDate, contenthash, false);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    break;
                case DiscussionForumService.EVENT_FORUMS_MESSAGE_DENY:
                    try {
                        long messageId = getMessageIdFromEvent(event);
                        Message m = messageManager.getMessageById(messageId);
                        Topic topic = m.getTopic();
                        String objectId = getMessageId(siteid, topic.getOpenForum(), topic, m);
                        Calendar releaseDate = Calendar.getInstance();
                        String contenthash = notifyUtils.hashContent(m.getTitle(), m.getBody());
                        List<String> sendList = new ArrayList<String>();
                        sendList.add(m.getCreatedBy());
                        notifyUtils.sendNotification("discussion", "rejection", objectId, event.getContext(), notifyUtils.convertUserIdsInSite(siteid, sendList), releaseDate, contenthash, false);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    break;
                case DiscussionForumService.EVENT_FORUMS_REMOVE:
                    try {
                        long messageId = getMessageIdFromEvent(event);
                        Message m = messageManager.getMessageById(messageId);
                        String loggedInUser = SessionManager.getCurrentSessionUserId();
                        //the instructor doesn't need to be notified when deleting his/her own messages
                        if(!loggedInUser.equals(m.getCreatedBy())){
                            Topic topic = m.getTopic();
                            String objectId = getMessageId(siteid, topic.getOpenForum(), topic, m);
                            Calendar releaseDate = Calendar.getInstance();
                            String contenthash = notifyUtils.hashContent(m.getTitle(), m.getBody());
                            List<String> sendList = new ArrayList<String>();
                            sendList.add(m.getCreatedBy());
                            notifyUtils.sendNotification("discussion", "deletion", objectId, event.getContext(), sendList, releaseDate, contenthash, false);
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                default:
                    //this shouldn't happen
            }
        }
        else if (topicEvents.contains(event.getEvent())) {
            String eventType = event.getEvent();
            String siteid = event.getContext();
            //If the tool is hidden (in site info) we don't want to send any notifications
            if(notifyUtils.toolIsHidden(siteid, DiscussionForumService.FORUMS_TOOL_ID)) return;
            switch(eventType){
                case DiscussionForumService.EVENT_FORUMS_TOPIC_REVISE:
                    //notify users if the topic has postings
                    try {
                        Reference ref = entityManager.newReference(event.getResource());
                        String[] refParts = ref.getReference().split("/");
                        String topicId = refParts[refParts.length-2];
                        List<Message> topicMessages = messageManager.findMessagesByTopicId(Long.parseLong(topicId));
                        if(topicMessages.size() > 0){
                            DiscussionTopic topic = discussionForumManager.getTopicById(Long.parseLong(topicId));
                            Long forumId = topic.getBaseForum().getId();
                            // Commented out because there is ANOTHER hibernate problem and this is not high priority.
                            // There are a few ways to get the forum from the topic, none have worked.
                            // java.lang.ClassCastException: org.sakaiproject.component.app.messageforums.dao.hibernate.OpenForumImpl_$$_jvst44_37 cannot be cast to org.sakaiproject.api.app.messageforums.DiscussionForum
                            // DiscussionForum forum = (DiscussionForum) forumManager.getForumById(true,forumId);
                            // //Don't send notification if forum or topic are in draft mode
                            // if(!(forum.getDraft() || topic.getDraft())){
                            //     String objectId = getTopicId(siteid, topic);
                            //     Calendar releaseDate = Calendar.getInstance();
                            //     String contenthash = notifyUtils.hashContent("Topic " + topic.getTitle() + " has been updated.");
                            //     //Get list of users who have read access to this topic
                            //     List<String> usersWithReadAccess = getNotifyList(siteid, Long.parseLong(topicId), event.getUserId());
                            //     //Not sure what the notification type should actually be
                            //     notifyUtils.sendNotification("discussion_topic", "creation", objectId, event.getContext(), usersWithReadAccess, releaseDate, contenthash, false);
                            // }
                        }
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                default:
                    //this shouldn't happen
            }
        }
        else if (forumEvents.contains(event.getEvent())) {
            String eventType = event.getEvent();
            String siteid = event.getContext();
            //If the tool is hidden (in site info) we don't want to send any notifications
            if(notifyUtils.toolIsHidden(siteid, DiscussionForumService.FORUMS_TOOL_ID)) return;
            switch(eventType){
                case DiscussionForumService.EVENT_FORUMS_REVISE:
                    //Who should get this notification?  There is no forums equivalent of getUsersAllowedForTopic
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

    public List<String> getNotifyList(String siteid, long topicId, String author) throws Exception {
        // get list of users to notify: a set of userIds for the site members who have
        // "read" permission for the given topic
        Set allowedUsers = discussionForumManager.getUsersAllowedForTopic(siteid, topicId, true, false);
        Iterator<String> uit = allowedUsers.iterator();
        //put the allowed users in a list, exclude the author of the post because they don't
        //need to be notified
        List<String> userids = new ArrayList<String>();
        while(uit.hasNext()){
            String userid = uit.next();
            if(!userid.equals(author)){
                userids.add(userid);
            }
        }
        return notifyUtils.convertUserIdsInSite(siteid, userids);
    }

    //returns true if the message is the first one in its thread and false otherwise
    public boolean isNewThread(Message m) {
        if(null == m.getInReplyTo()){
            //The message is not in reply to another message, so it must be the first in the thread
            return true;
        }
        return false;
    }

    public boolean authorIsInstructor(){
        return discussionForumManager.isInstructor();
    }

    public boolean authorIsTA(){
        return discussionForumManager.isSectionTA();
    }

    public List<String> getThreadParticipants(String siteid, Message m) throws Exception {
        List<String> participants = new ArrayList<String>();
        //get the initial message in the thread.  The threadId is the ID of the first message.
        Long threadId = m.getThreadId();
        List<Message> threadMessages = new ArrayList<Message>();
        //the child messages will include replies to replies to replies... not just first level replies
        messageManager.getChildMsgs(threadId, threadMessages);
        //we need the initial message too, not just its descendents
        Message initialMessage = messageManager.getMessageById(threadId);
        threadMessages.add(initialMessage);
        Iterator<Message> messageIterator = threadMessages.iterator();
        while (messageIterator.hasNext()){
            Message mess = messageIterator.next();
            String creator = mess.getCreatedBy();
            if(!creator.equals(m.getCreatedBy()) && !participants.contains(creator)){
                participants.add(creator);
            }
        }
        return notifyUtils.convertUserIdsInSite(siteid, participants);
    }
}
