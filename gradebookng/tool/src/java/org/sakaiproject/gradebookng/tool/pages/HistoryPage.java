package org.sakaiproject.gradebookng.tool.pages;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.WebMarkupContainer;

import org.sakaiproject.gradebookng.business.model.GbHistoryLog;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.apache.wicket.ajax.AjaxRequestTarget;

public class HistoryPage extends BasePage {
	private static final long serialVersionUID = 1L;

	private final int resultsPerPage = 50;
	private List<GbHistoryLog> historyLog;
	private int pageCounter = 0;
	Comparator<GbHistoryLog> dateComparator;

	public HistoryPage() {
		disableLink(this.historyPageLink);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();
		// Simple, chronologically descending sort for display purposes
		dateComparator = (item1, item2) -> item2.getDateGraded().compareTo(item1.getDateGraded());

		historyLog = getMoreHistoryLog(pageCounter++);

		WebMarkupContainer tablePanel = new WebMarkupContainer("historyLogTable");

		final ListView<GbHistoryLog> listView = new ListView<GbHistoryLog>("historyLogListView", historyLog) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<GbHistoryLog> item) {
				GbHistoryLog itemModel = item.getModelObject();

				item.add(new Label("dateGraded", FormatHelper.formatDateTime(itemModel.getDateGraded())));
				item.add(new Label("studentName", itemModel.getStudentName()));
				item.add(new Label("studentId", itemModel.getStudentId()));
				item.add(new Label("assignment", itemModel.getAssignmentName()));
				item.add(new Label("updatedBy", itemModel.getGraderDisplayId()));
				item.add(new Label("event", itemModel.getEventText()));
			}
		};
		tablePanel.add(listView);
		tablePanel.setOutputMarkupId(true);
		add(tablePanel);

		final AjaxLink<Void> loadMo = new AjaxLink<Void>("loadMo") {
			@Override
			public void onClick(final AjaxRequestTarget target) {
				historyLog.addAll(getMoreHistoryLog(pageCounter++));
				target.add(tablePanel);
			}
		};
		loadMo.setOutputMarkupId(true);
		add(loadMo);
	}

	private List<GbHistoryLog> getMoreHistoryLog(int currentPageNum) {
		List<GbHistoryLog> moreHistoryLog = this.businessService.getHistoryLog(currentPageNum, resultsPerPage);

		// Sort used to determine previous and current grades for display purposes
		Comparator<GbHistoryLog> logComparator = (item1, item2) -> {
			// First sort by student ASC
			int compValue = item1.getStudentId().compareTo(item2.getStudentId());
			if (compValue != 0) return compValue;

			// Then sort by Assignment ASC
			compValue = item1.getAssignmentName().compareTo(item2.getAssignmentName());
			if (compValue != 0) return compValue;

			// And finally by date ASC
			return item1.getDateGraded().compareTo(item2.getDateGraded());
		};
		Collections.sort(moreHistoryLog, logComparator);


		String currentStudent = "";
		String currentAssignment = "";
		String previousScore = "";
		for (GbHistoryLog logItem : moreHistoryLog) {
			if (!currentStudent.equals(logItem.getStudentId()) || !currentAssignment.equals(logItem.getAssignmentName())) {
				// We have encountered the first item for a new student and/or assignment.
				// Reset current values and set event text for the first item
				currentStudent = logItem.getStudentId();
				currentAssignment = logItem.getAssignmentName();
				logItem.setEventText(createEventText(logItem.getGrade()));
			} else {
				logItem.setEventText(createEventText(previousScore, logItem.getGrade()));
			}

			previousScore = logItem.getGrade();
		}

		Collections.sort(moreHistoryLog, dateComparator);

		return moreHistoryLog;
	}

	private String createEventText(String previousGrade, String currentGrade) {
		previousGrade = StringUtils.isBlank(previousGrade) ? "No Grade" : FormatHelper.formatGrade(previousGrade);
		currentGrade = StringUtils.isBlank(currentGrade) ? "No Grade" : FormatHelper.formatGrade(currentGrade);

		// should not happen, but let's catch it and return early just in case it does...
		if (previousGrade.equals(currentGrade)) {
			return "No Change";
		}

		return "Grade changed from " + previousGrade + " to " + currentGrade;

	}

	private String createEventText(String onlyGrade) {
		onlyGrade = StringUtils.isBlank(onlyGrade) ? "No Grade" : FormatHelper.formatGrade(onlyGrade);
		return "Grade set to " + onlyGrade;
	}
}