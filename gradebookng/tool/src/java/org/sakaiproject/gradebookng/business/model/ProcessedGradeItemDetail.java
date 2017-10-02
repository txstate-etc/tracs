package org.sakaiproject.gradebookng.business.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Encapsulates the details of an imported grade for a student and any comment
 *
 * TODO refactor to ProcessedCell?
 */
@ToString
public class ProcessedGradeItemDetail implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String studentEid;

	@Getter
	@Setter
	private String studentUuid;

	@Getter
	@Setter
	private String grade;

	@Getter
	@Setter
	private String comment;

	@Getter
	@Setter
	private String previousGrade;

	@Getter
	@Setter
	private String previousComment;

	public boolean hasGradeChange() {
		if (previousGrade == null) {
			return grade != null;
		} else if (grade == null) {
			return true;
		} else {
			Double prevDouble = Double.parseDouble(previousGrade);
			Double curDouble = Double.parseDouble(grade);
			return prevDouble.doubleValue() != curDouble.doubleValue();
		}
	}

	public boolean hasCommentChange() {
		return previousComment == null ? comment != null : !previousComment.equals(comment);
	}

}
