package org.sakaiproject.gradebookng.tool.pages;

import java.util.List;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang.time.DateUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.WebMarkupContainer;

import org.sakaiproject.gradebookng.business.model.GbHistoryLog;
import org.sakaiproject.gradebookng.business.util.FormatHelper;

public class HistoryPage extends BasePage {
	private static final long serialVersionUID = 1L;

	public HistoryPage() {
		disableLink(this.historyPageLink);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();
		Date historyDate = DateUtils.addMonths(new Date(), -6);
		final List<GbHistoryLog> historyLog = this.businessService.getHistoryLog(historyDate);
		Comparator<GbHistoryLog> dateComparator = new Comparator<GbHistoryLog>() {
			@Override
			public int compare(GbHistoryLog item1, GbHistoryLog item2) {
				return item2.getDateGraded().compareTo(item1.getDateGraded());
			}
		};
		Collections.sort(historyLog, dateComparator);

		final WebMarkupContainer tablePanel = new WebMarkupContainer("historyLogTable");

		final ListView<GbHistoryLog> listView = new ListView<GbHistoryLog>("historyLogListView", historyLog) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<GbHistoryLog> item) {
				GbHistoryLog itemModel = item.getModelObject();

				item.add(new Label("dateGraded", FormatHelper.formatDateTime(itemModel.getDateGraded())));
				item.add(new Label("student", itemModel.getStudentName()));
				item.add(new Label("assignment", itemModel.getAssignmentName()));
				item.add(new Label("updatedBy", itemModel.getGraderId()));
				item.add(new Label("event", "Score set to " + FormatHelper.formatGrade(itemModel.getGrade())));
			}
		};
		tablePanel.add(listView);
		add(tablePanel);
	}
}