package org.sakaiproject.gradebookng.business.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a row of the spreadsheet, contains a few fixed fields and then the map of cells
 */
public class ImportedRow implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String studentEid;

	@Getter
	@Setter
	private String studentUuid;

	@Getter
	@Setter
	private String studentName;

	@Getter
	@Setter
	private Map<String, ImportedCell> cellMap;

	public ImportedRow() {
		this.cellMap = new HashMap<String, ImportedCell>();
	}

	public String PrintRow()
	{
		String returnVal = "\n";

		returnVal += "StudentId = " + studentEid + "\n";
		//returnVal += "Uuid = " + studentUuid + "\n";
		returnVal += "StudentName = " + studentName + "\n";
		returnVal += "GB Items:\n";
		for(String keyString : cellMap.keySet())
		{
			ImportedCell value = cellMap.get(keyString);

			returnVal += String.format("%s : Score: %s ", keyString, value.getPreviousScore());
			if (value.hasScoreChange()) {
				returnVal += String.format("=> %s ", value.getScore());
			} 
			String commentString = value.getPreviousComment() == null ? "-" : value.getPreviousComment();
			returnVal += String.format("Comment: %s ", commentString);
			if (value.hasCommentChange()) {
				commentString = value.getComment() == null ? "-" : value.getComment();
				returnVal += String.format("=> %s ", commentString);
			}
			returnVal += "\n";
		}

		return returnVal;
	}
}
