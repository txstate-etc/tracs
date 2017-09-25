package org.sakaiproject.gradebookng.business.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a cell in the imported spreadsheet
 */
@ToString
public class ImportedCell implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String score;
	
	@Getter
	@Setter
	private String comment;

	@Getter
	@Setter
	private String previousScore;

	@Getter
	@Setter
	private String previousComment;

	public boolean hasScoreChange() {
		if (previousScore == null) {
			return score != null;
		} else if (score == null) {
			return true;
		} else {
			Double prevDouble = Double.parseDouble(previousScore);
			Double curDouble = Double.parseDouble(score);
			return prevDouble.doubleValue() != curDouble.doubleValue();
			//return Double.parseDouble(previousScore) != Double.parseDouble(score);
		}
	}

	public boolean hasCommentChange() {
		return !(previousComment == null ? comment == null : previousComment.equals(comment));
	}

	public ImportedCell() {
	}
	

}
