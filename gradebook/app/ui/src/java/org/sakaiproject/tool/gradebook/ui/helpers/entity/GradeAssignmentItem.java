package org.sakaiproject.tool.gradebook.ui.helpers.entity;

import lombok.Data;

import org.sakaiproject.service.gradebook.shared.Assignment;

import java.util.Date;

@Data
public class GradeAssignmentItem {
	protected String userId;
	protected String userName;
	protected String itemName;
	protected Double points;
	protected String grade;
	protected Date dueDate;
	protected String comment;
	protected Date postedDate;

	public GradeAssignmentItem() {
	}

	public GradeAssignmentItem(Assignment assignment) {
		itemName = assignment.getName();
		points = assignment.getPoints();
	}
}
