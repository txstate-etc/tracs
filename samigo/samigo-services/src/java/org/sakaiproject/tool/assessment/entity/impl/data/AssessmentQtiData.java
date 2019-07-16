package org.sakaiproject.tool.assessment.entity.impl.data;

public class AssessmentQtiData {
	public String qtiXmlData;

	public AssessmentQtiData() {
	}

	public AssessmentQtiData(String xmlString) {
		this.qtiXmlData = xmlString;
	}

	public void setQtiXmlData (String xmlString) {
		this.qtiXmlData = xmlString;
	}
}
