package org.sakaiproject.gradebookng.tool.panels.importExport;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.gradebookng.tool.pages.ImportExportPage;
import org.sakaiproject.gradebookng.tool.panels.AddOrEditGradeItemPanelContent;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.gradebookng.business.model.ImportedColumn;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapInputColumnsStep extends Panel {

	private static final long serialVersionUID = 1L;
	private final IModel<ColumnMap> model;

	private List<ImportedColumn> importedColumns;
	public List<ImportedColumn> getImportedColumns() {
		return importedColumns;
	}


	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	public MapInputColumnsStep(final String id, final IModel<ColumnMap> model) {
		super(id);
		this.model = model;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		final ColumnMap columnMap = this.model.getObject();
		importedColumns = columnMap.getColumnList();

		List<Assignment> assignmentList = this.GenerateAssignmentList();

		List<String> columnTypeList = Arrays.asList("Gradebook Item - grades", 
			"Gradebook Item - comments", 
			"Student ID", 
			"Student Name", 
			"Ignore");

		ArrayList<ColumnListItem> listViewData = new ArrayList<ColumnListItem>();
		for(ImportedColumn ic : importedColumns)
		{
			listViewData.add(new ColumnListItem(ic, columnTypeList, assignmentList));
		}

		IChoiceRenderer assignmentRender = new IChoiceRenderer() {
			public Object getDisplayValue(Object object) {
				return ((Assignment)object).getName();
			}

			public String getIdValue(Object object, int index) {
				return Long.toString(((Assignment)object).getId());
			}

		};


		//ListView contains one set of controls per ImportedColumn for the User to adjust
		ListView<ColumnListItem> listItems = new ListView<ColumnListItem>("listItems", listViewData) {
			@Override
			public void populateItem(final ListItem<ColumnListItem> item) {
				ColumnListItem newItem = item.getModelObject();

				Label columnDesc = new Label("columnLabel", String.format("* Column %d (%s)", this.size(), newItem.getColumn().getUnparsedTitle()));
				item.add(columnDesc);
				
				DropDownChoice ddcType = new DropDownChoice("columnType", Model.of(newItem.getColumn().getFriendlyType()), newItem.getColumnTypeList());
				item.add(ddcType);			
				
				DropDownChoice ddcAssignment = new DropDownChoice("columnAssignment", newItem.getAssignmentList());
				ddcAssignment.setChoiceRenderer(assignmentRender);
				item.add(ddcAssignment);
			}
		};

		add(listItems);

		final Button submit = new Button("continuebutton") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {

                for(int i = 0; i < listItems.size(); i += 3)
                {
                	int colIndex = i / 3;
                	ImportedColumn currentColumn = importedColumns.get(colIndex);
                	String oldTitle = currentColumn.getColumnTitle();

                	//listItems.get(i) is the Label. Don't need it. Ignored
                	DropDownChoice columnType = (DropDownChoice)listItems.get(i+1);
                	DropDownChoice columnAssignment = (DropDownChoice)listItems.get(i+2);
                	switch(columnType.getValue())
                	{
                		case "Gradebook Item - grades":
                			currentColumn.setType(ImportedColumn.Type.GB_ITEM_WITHOUT_POINTS);
                			String gbItemName = columnAssignment.getValue();
                			currentColumn.setColumnTitle(gbItemName);
                			break;

                		case "Gradebook Item - comments":
                			currentColumn.setType(ImportedColumn.Type.COMMENTS);
                			currentColumn.setColumnTitle("* " + oldTitle);
                			break;

                		case "Student ID":
                			currentColumn.setType(ImportedColumn.Type.USER_ID);
                			break;

                		case "Student Name":
                			currentColumn.setType(ImportedColumn.Type.USER_NAME);
                			break;

                		case "Ignore":
                			currentColumn.setType(ImportedColumn.Type.IGNORE);
                			currentColumn.setColumnTitle("# " + oldTitle);
                			break;

                		default:
                			//Should never happen
                			//Error message?
                			break;

                	}
                	importedColumns.set(colIndex, currentColumn);
                }





            }

            @Override
            public void onError() {
                //TODO: Anything here?
            }
        };
        add(submit);

        final Button cancel = new Button("backbutton") {
        	@Override
        	public void onSubmit() {
        		
        	}
        };
        add(cancel);
	}

	private List<Assignment> GenerateAssignmentList()
	{
		List<Assignment> list = MapInputColumnsStep.this.businessService.getGradebookAssignments();
		Assignment newAssignment = new Assignment();
		newAssignment.setName("NEW Gradebook Item");
		newAssignment.setId(-1L);
		list.add(0, newAssignment);

		return list;
	}
}

class ColumnListItem implements Serializable {

	public ColumnListItem(ImportedColumn c, List<String> lct, List<Assignment> la) {
		column = c;
		columnTypeList = lct;
		assignmentList = la;
	}

	@Getter
	@Setter
	private ImportedColumn column;

	@Getter
	@Setter
	private List<String> columnTypeList;

	@Getter
	@Setter
	private List<Assignment> assignmentList;
}

class ColumnMap implements Serializable {

	private static final long serialVersionUID = 1L;

	public ColumnMap(final List<ImportedColumn> columns, final StringBuilder errorsb) {
		this.columnList = columns;
		this.errorStrings = errorsb;
	}

	@Getter
	@Setter
	private List<ImportedColumn> columnList;

	@Getter
	@Setter
	private StringBuilder errorStrings;
}