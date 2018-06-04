package org.sakaiproject.gradebookng.business;

public enum SubmitResultKey {
	//This is a file we added locally for tracs where we store all the keys for the result of grade submission
	//sent from grade submission server
	SUBMIT_SUCCESS("success"),
	SUBMIT_PAGE_URL("url"),
	SERVER_ERROR("error"),
	SESSION_ID("sessionid");

	private String property;

	private SubmitResultKey(String property) {
		this.property = property;
	}

	public String getProperty() {
		return property;
	}
}
