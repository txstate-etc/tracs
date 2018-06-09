/**********************************************************************************
 * Implementation of Texas Satet University
 * This is used to connect to grade submission server in order to submit grades
 * from Gradebookng to Texas State Banner system.
 * It packs the student grades in json format and send the payload to the grade
 * submission server.
 **********************************************************************************/

package org.sakaiproject.gradebookng.business;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.management.openmbean.InvalidKeyException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.gradebookng.business.model.GradeSubmissionResult;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.user.api.UserDirectoryService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TxstateInstitutionalAdvisor {

	private static final Log log = LogFactory.getLog(TxstateInstitutionalAdvisor.class);

	//Banner uses gradetype to indicate final grade or mid term; in gradebook, it is more of a gradeSubmitType
	//Params used to communicate with grade submission server; all are lowercase
	public static final String GRADE_TYPE = "gradetype";
	public static final String FINAL_GRADE = "finalgrade";
	public static final String MID_TERM = "midterm";
	public static final String GRADE_TYPE_DISPLAY = "gradetypedisplay";

	//Define variables that needed for grade submission server script input parameters
	private final String SUBMIT_SITE = "tracssite";
	private final String SUBMIT_SUBMITTER = "user";
	private final String SUBMIT_SECTIONS = "sections";
	private final String SUBMIT_GRADES = "grades";
	private final String SUBMIT_ACTION = "action";
	private final String GRADES_SUBMISSION = "gradesubmit";
	private final String VIEW_RECEIPTS = "viewreceipts";
	private final String SUBMIT_APP = "app";
	private final String SUBMIT_SIG = "sig";
	private String SUBMIT_APPKEY;
	private String SUBMIT_SIGKEY;
	//official final grades from Registar
	private String[] validFinalGrades = {"A","AU","AUX","AX","AY","B","BX","BY","C","CPT","CR","CRX","CRY","CX","CY","D","DL","DX","DY","F","FX","FY","I","IF","IX","IY","N","P","PR","PRX","PRY","PX","PY","RF","RI","RN","RP","RU","U","UX","UY","W","WX","WY","Z"};

	// Final Grade Submission Status (FGSS)
	private final String FGSS_BANNER_MESSAGE = "The final grade process has begun";
	private final String FGSS_DIALOG_MESSAGE = "The final grade process has begun.  Please contact all course graders before making any Gradebook changes.";

	String finalGradeSubmissionPath = null;

	private SiteService siteService = null;
	private ToolManager toolManager = null;
	private AuthzGroupService authzGroupService = null;
	private UserDirectoryService userDirectoryService;
	private EventTrackingService eventTrackingService;
	private ServerConfigurationService configService;

	public List<String> getExportCourseManagementSetEids(Group group) {
		if(null == group) {
			log.error("ERROR : Group is null");
			return null;
		}
		if(null == group.getProviderGroupId()) {
			log.warn("Group Provider Id is null");
			return null;
		}
		return Arrays.asList(group.getProviderGroupId().split("\\+"));
	}

	public String getExportCourseManagementId(String userEid, Group group, List<String> enrollmentSetEids) {

		if (null == group) {
			log.error("ERROR : Group is null");
			return null;
		}

		if (null == group.getContainingSite()) {
			log.warn("Containing site is null");
			return null;
		}

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(group.getContainingSite().getTitle());
		stringBuilder.append(" : ");
		stringBuilder.append(group.getTitle());

		return stringBuilder.toString();
	}

	public boolean isLearner(Member member) {
		String role = member.getRole() == null ? "" : member.getRole().getId();

		return (role.equalsIgnoreCase("Student")
				|| role.equalsIgnoreCase("Open Campus")
				|| role.equalsIgnoreCase("Access"))
		&& member.isActive();
	}

	public boolean isExportCourseManagementIdByGroup() {
		return false;
	}

	public boolean isValidOverrideGrade(String grade, String learnerEid, String learnerDisplayId, Gradebook gradebook, Set<String> scaledGrades) {

		if (scaledGrades.contains(grade))
			return true;
		//allow override if they meet registra's final grade types
		else if (new HashSet<String>(Arrays.asList(validFinalGrades )).contains(grade))
			return true;

		return false;
	}

	public GradeSubmissionResult submitGrade(Map<String, String> studentsGrades, String gradebookUid, String gradeSubmitType){
		return submitToExtGradesSubmissionServer(studentsGrades, gradebookUid, GRADES_SUBMISSION, gradeSubmitType);
	}

	//The submission server will show the previous submission's receipt;
	public GradeSubmissionResult viewSubmissionReceipt(String gradebookUid){
		return submitToExtGradesSubmissionServer(null, gradebookUid, VIEW_RECEIPTS, null);
	}

	public GradeSubmissionResult submitToExtGradesSubmissionServer(Map<String, String> studentsGrades, String gradebookUid, String action, String gradeSubmitType){
		//get keys
		SUBMIT_APPKEY = configService.getString("grade.submit.app.key");
		SUBMIT_SIGKEY = configService.getString("grade.submit.sig.key");

		//get data
		String submitGradesJsonData = getSubmissionJsonData(studentsGrades,SUBMIT_APPKEY, action, gradeSubmitType);

		byte[] signature = getDataSigned(submitGradesJsonData,SUBMIT_SIGKEY);

		log.debug("\n\nsubmitGradesJsonData:" + submitGradesJsonData +"\n\n" + "singature is: " + signature+ "\n\n");

		GradeSubmissionResult gradeSubmissionResult = new GradeSubmissionResult();

		Map<String, String>  resultMap = new HashMap<String, String>();

		HttpsURLConnection connection = getSSLConnection();

		boolean submitResult = false;
		String sessionId = null;
		try {

			//payload=<url encoded json object>&sig=<hex encoded hmac-sha1 of payload and key>

			String query = "payload=" + URLEncoder.encode(submitGradesJsonData);
			query += "&";
			query += "sig=" + Hex.encodeHexString(signature);

			log.debug("\n\n"+query+"\n");

			//DEFINE CONNECTION.
			if (connection == null)
				gradeSubmissionResult.setStatus(500);

			connection.setDoOutput     (true);
			connection.setDoInput      (true);
			connection.setRequestMethod("POST");

			//CREATE REQUEST
			OutputStream outPut = connection.getOutputStream();
			OutputStreamWriter wout = new OutputStreamWriter(outPut);
			wout.write(query);
			wout.flush();
			wout.close();

			//READ RESPONSE
			InputStream in = connection.getInputStream();

			int c;
			String res = "";
			while ((c = in.read()) != -1) { res += (char) c; }
			HashMap<String,Object> result = new ObjectMapper().readValue(res, HashMap.class);

			//Can be simplified to be
			//HashMap<String, Object> result = new ObjectMapper().readValue(in, HashMap.class);

			submitResult = ((Boolean)result.get(SubmitResultKey.SUBMIT_SUCCESS.getProperty())).booleanValue();

			//handling successful response
			if(submitResult){
				sessionId = result.get(SubmitResultKey.SESSION_ID.getProperty()).toString();
				String message = "gradebook2." + action;

				//We only get url when successfully connected
				String url = result.get(SubmitResultKey.SUBMIT_PAGE_URL.getProperty()).toString();
				log.debug("\n\nurl="+url+ "\n\n" );

				String ref = "externalConnSessionId="+sessionId;
				Event event = eventTrackingService.newEvent(message, ref, true);
				eventTrackingService.post(event);

				resultMap.put(SubmitResultKey.SUBMIT_PAGE_URL.getProperty(), url);
				resultMap.put(SubmitResultKey.SUBMIT_SUCCESS.getProperty(), submitResult?"true":"false");
				resultMap.put(SubmitResultKey.SESSION_ID.getProperty(), sessionId);
				gradeSubmissionResult.setStatus(200);
			}else{
				String error = result.get("error").toString();
				resultMap.put(SubmitResultKey.SERVER_ERROR.getProperty(), error);
				gradeSubmissionResult.setStatus(500);
				log.info("Failed to connect server with this error: " + error);
			}

			log.debug("\n\nsubmitResult "+submitResult+ "\n\nsessionId "+sessionId+ "\n\n" );
			log.debug("\n\nfinalGradeSubmissionResult "+ toJson(resultMap, true) + "\n\n");

			gradeSubmissionResult.setData(toJson(resultMap, true));

			//CLOSE ALL.
			in .close();
			outPut.close();
			connection.disconnect();
			return gradeSubmissionResult;

		}
		catch (IOException e) {
			gradeSubmissionResult.setStatus(500);
			e.printStackTrace();
			log.error("Caught IO exception at submission: " + e.getMessage());
			return null;
		}
		catch (Exception e) {
			gradeSubmissionResult.setStatus(500);
			e.printStackTrace();
			return null;
		}

	}

	/*
	 * Helper Methods
	 */

	private String getSubmissionJsonData (Map<String, String> studentsGrades, String appKey, String action, String gradeSubmitType){
		String siteId = null;

		try {
			Site site = siteService.getSite(toolManager.getCurrentPlacement().getContext());
			siteId = site.getId();
		} catch (IdUnusedException e) {
			log.error("EXCEPTION: Wasn't able to get the siteId");
		}

		String realmId = siteService.siteReference(siteId);
		String submitter = userDirectoryService.getCurrentUser().getEid();
		Set<String> sections = authzGroupService.getProviderIds(realmId);

		//Now we need to aggregate all data into the hashMap
		Map<String, Object>  wrapper = new HashMap<String, Object>();
		wrapper.put(SUBMIT_SUBMITTER, submitter);
		wrapper.put(SUBMIT_SECTIONS, sections);
		wrapper.put(SUBMIT_APP, appKey);
		wrapper.put(SUBMIT_SITE, siteId);
		if (action.equals(GRADES_SUBMISSION)){
			wrapper.put(GRADE_TYPE, gradeSubmitType.toLowerCase());
			wrapper.put(SUBMIT_GRADES, studentsGrades);
			wrapper.put(SUBMIT_ACTION, GRADES_SUBMISSION);
		}
		else
			//no need to wrap grades if only see submission receipt
			wrapper.put(SUBMIT_ACTION, VIEW_RECEIPTS);

		String jsonData = toJson(wrapper, true);

		return jsonData;
	}

	protected String toJson(Object o, boolean pretty) {
		ObjectMapper mapper = new ObjectMapper();

		if (pretty)
		{
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		}

		StringWriter w = new StringWriter();
		try {
			mapper.writeValue(w, o);
		} catch (Exception e) {
			log.error("Caught an exception serializing to JSON: ", e);
		}

		return w.toString();
	}

	private byte[] getDataSigned(String jsonData, String sigKey){
		byte[] signature = null;
		try {
			byte[] keyBytes = sigKey.getBytes();
			SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");

			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);
			signature = mac.doFinal(jsonData.getBytes());
		}
		catch(NoSuchAlgorithmException e){
			log.info("No HmaSHA1 algorithm available");
		}
		catch(InvalidKeyException e){
			log.info("The key is not valid");
		}
		catch(Exception e){
			log.info("Here is whatever exception that causes non functional");
		}
		return signature;
	}

	private void trustAllCerts (){
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// Trust always
				}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
					// Trust always
				}
			}
		};

		// Create empty HostnameVerifier
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}
		};

		// Install the all-trusting trust manager
		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (NoSuchAlgorithmException e) {
			log.info("Failed at getting SSLContext object that follows the SSL protocol");
			e.printStackTrace();
		} catch (KeyManagementException e) {
			log.info("Failed at initalize the SSLContext object.");
			e.printStackTrace();
		}
	}

	private HttpsURLConnection getSSLConnection() {
		trustAllCerts();
		HttpsURLConnection conn = null;
		String server = configService.getString("grade.submission.server.url");
		try {
			conn = (HttpsURLConnection) ( new URL(server).openConnection() );
		} catch (MalformedURLException e) {
			log.info("URL " + server + " is not in a right format");
		} catch (IOException e) {
			log.info("Open connection to the server is failed.");
		}
		return conn;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}

	public String getDisplaySectionId(String enrollmentSetEid) {
		return "DisplayId for eid: " + enrollmentSetEid;
	}

	public String getPrimarySectionEid(List<String> eids) {
		if(null == eids || eids.isEmpty()) {
			return "";
		}
		else {
			return eids.get(0);
		}
	}

	public void setAuthzGroupService(AuthzGroupService authzGroupService) {
		this.authzGroupService = authzGroupService;
	}

	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}

	public void setEventTrackingService(EventTrackingService eventTrackingService) {
		this.eventTrackingService = eventTrackingService;
	}

	public void setConfigService(ServerConfigurationService configService) {
		this.configService = configService;
	}
}
