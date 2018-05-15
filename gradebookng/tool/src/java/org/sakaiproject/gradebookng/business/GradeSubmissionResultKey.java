package org.sakaiproject.gradebookng.business;

public enum GradeSubmissionResultKey {
	
	I_STATUS("status"),
	S_DATA("data");
	
	private String property;
	
	private GradeSubmissionResultKey(String property) {
		
		this.property = property;
	}
	
	public String getProperty() {

		return property;
	}
}