package edu.txstate.tracs.notifications;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hibernate.proxy.HibernateProxy;

import org.sakaiproject.authz.api.Member;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Shared utility functions for the TRACS notification module
 */
public class NotifyUtils {
    private final Log log = LogFactory.getLog(NotifyUtils.class);
    private MessageDigest sha1;
    private SiteService siteService;
    public void setSiteService(SiteService siteService) {
      this.siteService = siteService;
    }
    private ServerConfigurationService serverConfigurationService;
    public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
      this.serverConfigurationService = serverConfigurationService;
    }
    protected ExecutorService executor;


    public void init() {
      log.info("init()");
      try {
        sha1 = MessageDigest.getInstance("SHA-1");
        executor = Executors.newFixedThreadPool(2);
      } catch (Exception e) {
        // won't happen
      }
    }

    public List<String> getAllUserIdsExcept(String siteid, String useridtoexclude) throws Exception {
      List<String> ret = new ArrayList<String>();
      Site site = siteService.getSite(siteid);
      for (Member m : site.getMembers()) {
        if (!m.getUserId().equals(useridtoexclude)) ret.add(m.getUserEid());
      }
      return ret;
    }

    public List<String> convertUserIdsInSite(String siteid, List<String> userids) throws Exception {
      Map<String,Boolean> usermap = new HashMap<String,Boolean>();
      for (String uid : userids) {
        usermap.put(uid, Boolean.TRUE);
      }

      List<String> ret = new ArrayList<String>();
      Site site = siteService.getSite(siteid);
      for (Member m : site.getMembers()) {
        if (usermap.containsKey(m.getUserId())) ret.add(m.getUserEid());
      }
      return ret;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
      char[] hexChars = new char[bytes.length * 2];
      for ( int j = 0; j < bytes.length; j++ ) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }
      return new String(hexChars);
    }

    public Calendar translateDate(String dt) throws java.text.ParseException {
      Calendar cal = Calendar.getInstance();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
      cal.setTime(sdf.parse(dt+"-0000"));
      return cal;
    }

    public String dateToJson(Calendar c) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
      return sdf.format(c.getTime());
    }

    public String hashContent(String title) {
      return hashContent(title, "", "");
    }
    public String hashContent(String title, String body) {
      return hashContent(title, body, "");
    }
    public String hashContent(String title, String body, String extra) {
      String total = title+":"+body+":"+extra;
      try {
        return bytesToHex(sha1.digest(total.getBytes("UTF-8")));
      } catch (Exception e) {
        e.printStackTrace();
        return "";
      }
    }

    protected String mapToParams(Map<String,String> params) throws Exception {
      List<String> pairs = new ArrayList<String>();
      for (Map.Entry<String,String> entry : params.entrySet()) {
        pairs.add(URLEncoder.encode(entry.getKey(), "UTF-8")+"="+URLEncoder.encode(entry.getValue(), "UTF-8"));
      }
      return StringUtils.join(pairs,"&");
    }

    public void notificationThread(Runnable r) {
      executor.execute(r);
    }
    protected HttpURLConnection getConnection() throws Exception {
      return getConnection(null);
    }
    protected HttpURLConnection getConnection(Map<String,String> params) throws Exception {
      String t = serverConfigurationService.getString("tracs.notifications.scheme", "https");
      t += "://"+serverConfigurationService.getString("tracs.notifications.host");
      t += serverConfigurationService.getString("tracs.notifications.path", "/notifications");
      if (params != null)
        t += "?"+mapToParams(params);
      URL url = new URL(t);
      HttpURLConnection http = (HttpURLConnection) url.openConnection();
      http.setRequestProperty("X-Dispatch-Key", serverConfigurationService.getString("tracs.notifications.secret", ""));
      http.setConnectTimeout(10000);
      return http;
    }

    public void sendNotification(final String objecttype, final String notifytype,
      final String objectid, final String siteid, final List<String> userids, final Calendar notifyafter,
      final String contenthash, final boolean sendupdates) throws Exception {

      notificationThread(new Runnable() {
      @Override
      public void run() {
        try {
          String after = dateToJson(notifyafter);
          List<String> notifications = new ArrayList<String>();
          for (String userid : userids) {
            String jsonBody = "{";
            jsonBody += "\"keys\":{";
            jsonBody += "\"provider_id\":\"tracs\",";
            jsonBody += "\"notification_type\":\""+notifytype+"\",";
            jsonBody += "\"object_type\":\""+objecttype+"\",";
            jsonBody += "\"object_id\":\""+objectid+"\",";
            jsonBody += "\"user_id\":\""+userid+"\"";
            jsonBody += "},";
            jsonBody += "\"other_keys\":{";
            jsonBody += "\"site_id\":\""+siteid+"\"";
            jsonBody += "},";
            jsonBody += "\"content_hash\":\""+contenthash+"\",";
            jsonBody += "\"notify_after\":\""+after+"\",";
            jsonBody += "\"send_updates\":"+(sendupdates?"true":"false");
            jsonBody += "}";
            notifications.add(jsonBody);
          }
          String finalBody = "["+StringUtils.join(notifications, ",")+"]";

          HttpURLConnection http = getConnection();
          http.setRequestMethod("POST");
          http.setDoOutput(true);
          http.setFixedLengthStreamingMode(finalBody.getBytes().length);
          http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
          try (OutputStream os = http.getOutputStream()) {
            os.write(finalBody.getBytes());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }});

    }

    public void delete(final Map<String,String> params) {
      notificationThread(new Runnable() {
      @Override
      public void run() {
        try {
          HttpURLConnection http = getConnection(params);
          http.setRequestMethod("DELETE");
          http.getResponseCode();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }});
    }
    public void deleteForSite(String siteid) {
      Map<String,String> params = new HashMap<String,String>();
      params.put("keys.provider_id", "tracs");
      params.put("other_keys.site_id", siteid);
      delete(params);
    }
    public void deleteForUserInSite(String siteid, String eid) {
      Map<String,String> params = new HashMap<String,String>();
      params.put("keys.provider_id", "tracs");
      params.put("other_keys.site_id", siteid);
      params.put("keys.user_id", eid);
      delete(params);
    }
    public void deleteForObject(String objecttype, String objectid) {
      Map<String,String> params = new HashMap<String,String>();
      params.put("keys.provider_id", "tracs");
      params.put("keys.object_type", objecttype);
      params.put("keys.object_id", objectid);
      delete(params);
    }

    public <T> T unproxy(Object maybeProxy, Class<T> baseClass) throws ClassCastException {
       if (maybeProxy instanceof HibernateProxy) {
          return baseClass.cast(((HibernateProxy) maybeProxy).getHibernateLazyInitializer().getImplementation());
       } else
          return baseClass.cast(maybeProxy);
    }
}
