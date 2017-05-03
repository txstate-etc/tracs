package edu.txstate.tracs.notifications;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Observer;
import java.util.Observable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.announcement.api.AnnouncementMessage;
import org.sakaiproject.announcement.api.AnnouncementService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.Event;
/**
 * Watches for announcement changes, POSTs to our mobile notification service
 */
public class AnnouncementsNotifier implements Observer {
    private final Log log = LogFactory.getLog(AnnouncementsNotifier.class);
    private List<String> updates;
    private List<String> deletes;

    private EventTrackingService eventTrackingService;
    public void setEventTrackingService(EventTrackingService service) {
        eventTrackingService = service;
    }

    private EntityManager entityManager;
    public void setEntityManager(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    private AnnouncementService announceService;
    public void setAnnounceService(AnnouncementService announceService) {
      this.announceService = announceService;
    }

    private NotifyUtils notifyUtils;
    public void setNotifyUtils(NotifyUtils notifyUtils) {
      this.notifyUtils = notifyUtils;
    }

    public void init() {
      updates = new ArrayList<String>();
      updates.add(AnnouncementService.SECURE_ANNC_ADD);

      // for the first version we will ignore announcement edits and focus on creations
      // but these are the events for edits
      updates.add(AnnouncementService.SECURE_ANNC_UPDATE_ANY);
      updates.add(AnnouncementService.SECURE_ANNC_UPDATE_OWN);

      deletes = new ArrayList<String>();
      deletes.add(AnnouncementService.SECURE_ANNC_REMOVE_ANY);
      deletes.add(AnnouncementService.SECURE_ANNC_REMOVE_OWN);
      eventTrackingService.addObserver(this);
    }

    public void update(Observable o, Object arg) {
      Event event = (Event) arg;
      System.out.println("received "+event.getEvent()+" event for ticket " + event.getResource());
      try {
        if (updates.contains(event.getEvent())) {
          Reference ref = entityManager.newReference(event.getResource());
          AnnouncementMessage m = (AnnouncementMessage) announceService.getMessage(ref);

          Calendar releaseDate;
          String releaseDateStr = m.getProperties().getProperty(AnnouncementService.RELEASE_DATE);
          if (releaseDateStr != null)
            releaseDate = notifyUtils.translateDate(releaseDateStr);
          else releaseDate = Calendar.getInstance();

          String contenthash = notifyUtils.hashContent(m.getAnnouncementHeader().getSubject(), m.getBody());

          List<String> userids = notifyUtils.getAllUserIdsExcept(event.getContext(), event.getUserId());
          for (String uid : userids) System.out.println("userid: "+uid);

          if (announceService.isMessageViewable(m)) {
            notifyUtils.sendNotification("announcement", "creation", m.getId(), event.getContext(), userids, releaseDate, contenthash, true);
          } else if (m.getHeader().getDraft()) System.out.println("message is in draft mode, delete any scheduled notification");
          else if (releaseDate.after(Calendar.getInstance())) System.out.println("message is scheduled for "+notifyUtils.dateToJson(releaseDate));
          else notifyUtils.deleteForObject("announcement", ref.getId());
        } else if (deletes.contains(event.getEvent())) {
          Reference ref = entityManager.newReference(event.getResource());
          notifyUtils.deleteForObject("announcement", ref.getId());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
}
