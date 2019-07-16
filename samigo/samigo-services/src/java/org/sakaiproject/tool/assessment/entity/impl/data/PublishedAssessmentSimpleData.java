package org.sakaiproject.tool.assessment.entity.impl.data;


import java.util.Date;


import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacade;

public class PublishedAssessmentSimpleData extends AssessmentSimpleData{

	  public Long publishedAssessmentId;
	  
	  public static final String ACTIVE = "active";
	  public static final String INACTIVE = "inactive";

	  public Long assessmentId;

	  public String startDate;
	  public String dueDate;
	  public String retractDate;
	  public String publishedStatus; //"active" or "inactive"
	  public Integer lateHandling;  // "1"= yes, "2"= no

	  public PublishedAssessmentSimpleData() {}

	  public PublishedAssessmentSimpleData(PublishedAssessmentFacade pub) {
		publishedAssessmentId = pub.getPublishedAssessmentId();
	    title = pub.getTitle();
	    description = pub.getDescription();
	    assessmentId = pub.getAssessmentId();
	    comments = pub.getComments();
	    typeId = pub.getTypeId();
	    createdBy = pub.getCreatedBy();
	    createdDate = formatDate(pub.getCreatedDate());
	    lastModifiedBy = pub.getLastModifiedBy();
	    lastModifiedDate = formatDate(pub.getLastModifiedDate());
	    dueDate = formatDate(pub.getDueDate());
	    retractDate = formatDate(pub.getRetractDate());
	    startDate = formatDate(pub.getStartDate());
	    lateHandling = pub.getLateHandling();
	  }

	 public void setPublishedStatus ( String stats) {
		 this.publishedStatus = stats;
	 }
}
