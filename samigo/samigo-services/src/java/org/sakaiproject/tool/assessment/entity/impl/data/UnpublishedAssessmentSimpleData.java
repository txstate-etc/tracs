package org.sakaiproject.tool.assessment.entity.impl.data;


import org.sakaiproject.tool.assessment.facade.AssessmentFacade;


public class UnpublishedAssessmentSimpleData extends AssessmentSimpleData {
	  public Long assessmentId;
	  public Boolean isTemplate;
	  public Integer status;
	  public Long assessmentTemplateId; 

	  public UnpublishedAssessmentSimpleData() {}

	  public UnpublishedAssessmentSimpleData(AssessmentFacade unpub) {
	    title = unpub.getTitle();
	    description = unpub.getDescription();
	    assessmentId = unpub.getAssessmentBaseId();
	    assessmentTemplateId = unpub.getAssessmentTemplateId();
	    comments = unpub.getComments();
	    isTemplate = unpub.getIsTemplate();
	    typeId = unpub.getTypeId();
	    status = unpub.getStatus();
	    createdBy = unpub.getCreatedBy();
	    createdDate = formatDate(unpub.getCreatedDate());
	    lastModifiedBy = unpub.getLastModifiedBy();
	    lastModifiedDate = formatDate(unpub.getLastModifiedDate());
	  }

}
