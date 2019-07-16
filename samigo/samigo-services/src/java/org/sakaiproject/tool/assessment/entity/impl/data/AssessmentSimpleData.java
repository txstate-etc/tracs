package org.sakaiproject.tool.assessment.entity.impl.data;


import java.text.SimpleDateFormat;
import java.util.Date;


public class AssessmentSimpleData {

	  
	  public String title = "";

	  public String description = "";

	  public String comments;

	  public String createdBy;

	  public String createdDate;

	  public Long typeId;
	  
	  public String lastModifiedBy;
	  public String lastModifiedDate;
	  
	  public AssessmentSimpleData() {}
	  
	  protected String formatDate(Date date) {
		String pattern = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		return date==null?null:simpleDateFormat.format(date);
	  }

}
