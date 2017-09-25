package org.sakaiproject.gradebookng.business.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Wraps an imported file
 */
public class ImportedSpreadsheetWrapper implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private ArrayList<List<String>> rawData;

	@Getter
	@Setter
	private List<ImportedRow> rows;

	@Getter
	@Setter
	private List<ImportedColumn> columns;

	public ImportedSpreadsheetWrapper() {
		this.rows = new ArrayList<>();
		this.columns = new ArrayList<>();
		this.rawData = new ArrayList<List<String>>();
	}

	public enum Type {
		GB_ITEM_WITH_POINTS,
		GB_ITEM_WITHOUT_POINTS,
		COMMENTS,
		USER_ID,
		USER_NAME,
		IGNORE;
	}

	public String getRawDataValue(int row, int column)
	{
		return rawData.get(row).get(column);
	}

	public void setRawDataValue(int row, int column, String value)
	{
		rawData.get(row).set(column, value);
	}

	public void addRawDataRow(String[] stringArray)
	{
		rawData.add(Arrays.asList(stringArray));
	}

	public void swapRawDataColumns(int col1, int col2)
	{
		for(List<String> raw : rawData) {
			Collections.swap(raw, col1, col2);
		}
	}

}
