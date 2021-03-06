package org.sakaiproject.gradebookng.business.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Setter;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sakaiproject.tool.gradebook.GradingEvent;

import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.gradebookng.business.model.GbUser;

import lombok.Getter;

/**
 * DTO for grade log events.
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
public class GbHistoryLog implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	private final Date dateGraded;

	@Getter
	private final String graderDisplayId;

	@Getter
	private final String assignmentName;

	@Getter
	private final String studentId;

	@Getter
	private final String studentName;

	@Getter
	private final String grade;

	// This variable is manipulated by the GB History Page to simplify display logic
	@Getter
	@Setter
	private String eventText;

	public GbHistoryLog(GradingEvent ge, GbUser student, GbUser grader, Assignment assignment) {
		this.dateGraded = ge.getDateGraded();
		this.grade = ge.getGrade();

		this.graderDisplayId = grader.getDisplayId();
		this.studentName = student.getDisplayName();
		this.studentId = student.getDisplayId();

		this.assignmentName = assignment == null ? "NULL" : assignment.getName();
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
