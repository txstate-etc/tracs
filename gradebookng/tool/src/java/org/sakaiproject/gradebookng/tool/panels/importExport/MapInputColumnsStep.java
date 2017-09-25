package org.sakaiproject.gradebookng.tool.panels.importExport;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.pages.ImportExportPage;
import org.sakaiproject.gradebookng.tool.panels.AddOrEditGradeItemPanelContent;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.gradebookng.business.model.ImportedColumn;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.ImportedSpreadsheetWrapper;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.gradebookng.business.util.ImportGradesHelper;
import org.sakaiproject.gradebookng.tool.model.ImportWizardModel;
import org.sakaiproject.user.api.User;
import org.sakaiproject.gradebookng.business.exception.GbImportCommentMissingItemException;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapInputColumnsStep extends Panel {

	private static final long serialVersionUID = 1L;
	private final String panelId;
	private final IModel<ColumnMap> model;
	private Map<String, String> userMap;
	private ImportedSpreadsheetWrapper spreadSheetWrapper;
	private ListView<ColumnListItem> listItems;
	private List<ImportedColumn> importedColumns;
	private List<Assignment> assignmentList;
	private List<String> columnTypeList;

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;


	public MapInputColumnsStep(final String id, final IModel<ColumnMap> model) {
		super(id);
		panelId = id;
		this.model = model;
	}

	@Override
	public void onInitialize() 
	{
		super.onInitialize();

		final ColumnMap columnMap = this.model.getObject();
		spreadSheetWrapper = columnMap.getWrapper();
		importedColumns = spreadSheetWrapper.getColumns();
		assignmentList = this.GenerateAssignmentList();
		userMap = this.getUserMap();

		columnTypeList = Arrays.asList("Grades", 
			"Comments", 
			"Student ID", 
			"Student Name", 
			"Ignore");

		add(new MappingForm("form"));		       
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

	private Map<String, String> getUserMap() {

		final List<User> users = this.businessService.getUsers(this.businessService.getGradeableUsers());

		final Map<String, String> rval = users.stream().collect(
                Collectors.toMap(User::getEid, User::getId));

		return rval;
	}

	private class MappingForm extends Form<Void> {

		public MappingForm(final String id) {
			super(id);

			ArrayList<ColumnListItem> listViewData = new ArrayList<ColumnListItem>();
				for(ImportedColumn ic : importedColumns) {
				listViewData.add(new ColumnListItem(ic, columnTypeList, assignmentList));
			}

			IChoiceRenderer assignmentRender = new IChoiceRenderer() {
				public Object getDisplayValue(Object object) {
					return ((Assignment)object).getName();
				}

				public String getIdValue(Object object, int index) {
					return ((Assignment)object).getName();
				}

			};

			IChoiceRenderer columnTypeRender = new IChoiceRenderer() {
				public Object getDisplayValue(Object object) {
					return (String)object;
				}

				public String getIdValue(Object object, int index) {
					return (String)object;
				}
			};

			//ListView contains one ListItem per column. 
			//Each ListItem holds a set of controls for the User to adjust column data
			listItems = new ListView<ColumnListItem>("listItems", listViewData) {

				@Override
				public void populateItem(final ListItem<ColumnListItem> item) {
					ColumnListItem newItem = item.getModelObject();

					Label columnDesc = new Label("columnLabel", String.format("* Column %d (%s)", this.size(), newItem.getColumn().getUnparsedTitle()));
					item.add(columnDesc);
				
					DropDownChoice ddcType = new DropDownChoice("columnType", Model.of(newItem.getColumn().getFriendlyType()), newItem.getColumnTypeList()) {
						// @Override
						// protected boolean wantOnSelectionChangedNotifications() {
						// 	return true;
						// }

						// @Override
						// protected void onSelectionChanged(Object newSelection) {
						// 	this.setModel(Model.of((String)newSelection));
						// 	ColumnTypeDropDownChanged();
						// }
					};
					ddcType.setChoiceRenderer(columnTypeRender);
					item.add(ddcType);

					Assignment matchingAssignment = newItem.FindAssignmentByName();					
					DropDownChoice ddcAssignment = new DropDownChoice("columnAssignment", Model.of(matchingAssignment), newItem.getAssignmentList()) {
						// @Override
						// protected boolean wantOnSelectionChangedNotifications() {
						// 	return true;
						// }

						// @Override
						// protected void onSelectionChanged(Object newSelection) {

						// }
					};
					ddcAssignment.setChoiceRenderer(assignmentRender);
					item.add(ddcAssignment);

					// Long longModel;
					// try {
					// 	longModel = Long.parseLong(newItem.getColumn().getPoints());
					// } catch (Exception ex) {
					// 	longModel = 0L;
					// }
					// NumberTextField<Long> pointsTextField = new NumberTextField<Long>("columnPoints", Model.of(longModel), Long.class) {
					// 	@Override
					// 	public boolean isInputNullable() {
					// 		return true;
					// 	}
					// };
     //    			pointsTextField.setStep(10L);
     //    			pointsTextField.add(new NullNumberValidator());
     //    			item.add(pointsTextField);
				}	
			};
			add(listItems);

			final Button submit = new Button("continuebutton") {

            	@Override
           		public void onSubmit() {
           			int studentIdIndex = 0;
					int studentNameIndex = 1;

                	for(int i = 0; i < listItems.size(); i ++) {
                		
                		ImportedColumn currentColumn = importedColumns.get(i);
                		String oldTitle = currentColumn.getColumnTitle();

                		ListItem<ColumnListItem> currentListItem = (ListItem<ColumnListItem>)listItems.get(i);
                		DropDownChoice columnType = (DropDownChoice)currentListItem.get("columnType");
                		DropDownChoice columnAssignment = (DropDownChoice)currentListItem.get("columnAssignment");

						log.info("i = " + Integer.toString(i));
						log.info("ColumnType = " + columnType.getValue());

						

                		switch(columnType.getValue())
                		{
                			case "Grades":
                				spreadSheetWrapper.setRawDataValue(0, i, columnAssignment.getValue());
                				log.info("Set header to " + columnAssignment.getValue());
                				break;

                			case "Comments":
                				spreadSheetWrapper.setRawDataValue(0, i, "* " + columnAssignment.getValue());
                				log.info("Set header to * " + columnAssignment.getValue());
                				break;

                			case "Student ID":
                				studentIdIndex = i;
                				break;

              		  		case "Student Name":
              		  			studentNameIndex = i;
                				break;

                			case "Ignore":
                				spreadSheetWrapper.setRawDataValue(0, i, "# " + oldTitle);
                				break;

                			default:
                				//Should never happen
                				//Error message?
                				break;

                		}
                	}

                	if (studentIdIndex != 0) spreadSheetWrapper.swapRawDataColumns(0, studentIdIndex);
                	if (studentNameIndex != 1) spreadSheetWrapper.swapRawDataColumns(1, studentNameIndex);

                	List<List<String>> modifiedRawData = spreadSheetWrapper.getRawData();
                	spreadSheetWrapper = ImportGradesHelper.parseStringLists(modifiedRawData, userMap);

					assignmentList.remove(0); //To remove the "New Gradebook Item" assignment placeholder
                	final List<GbStudentGradeInfo> grades = MapInputColumnsStep.this.businessService.buildGradeMatrix(assignmentList);

					List<ProcessedGradeItem> processedGradeItems = null;
					try {
						processedGradeItems = ImportGradesHelper.processImportedGrades(spreadSheetWrapper, assignmentList, grades);
					} catch (final GbImportCommentMissingItemException e) {
						error(getString("importExport.error.commentnoitem"));
						return;
					}
					// if empty there are no users
					if (processedGradeItems.isEmpty()) {
						error(getString("importExport.error.empty"));
						return;
					}

					// OK, GO TO NEXT PAGE

					// clear any previous errors
					final ImportExportPage page = (ImportExportPage) getPage();
					page.clearFeedback();

					// repaint panel
					final ImportWizardModel importWizardModel = new ImportWizardModel();
					importWizardModel.setProcessedGradeItems(processedGradeItems);
					final Component newPanel = new GradeItemImportSelectionStep(MapInputColumnsStep.this.panelId, Model.of(importWizardModel));
					newPanel.setOutputMarkupId(true);
					MapInputColumnsStep.this.replaceWith(newPanel);

            	}

            	@Override
            	public void onError() {
                	log.info("Inside onERROR!!!");
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

		private void ColumnTypeDropDownChanged()
		{
			for(int i = 0; i < listItems.size(); i ++) {

				ListItem<ColumnListItem> currentListItem = (ListItem<ColumnListItem>)listItems.get(i);
				DropDownChoice columnType = (DropDownChoice)currentListItem.get("columnType");
                DropDownChoice columnAssignment = (DropDownChoice)currentListItem.get("columnAssignment");
                //NumberTextField columnPoints = (NumberTextField)currentListItem.get("columnPoints");

                switch(columnType.getValue())
        		{
        			case "Grades":
        				columnAssignment.setEnabled(true);
        				//columnPoints.setEnabled(true);
        				break;

        			case "Comments":
        				columnAssignment.setEnabled(true);
        				//columnPoints.setEnabled(false);
        				break;

        			case "Student ID":
        				columnAssignment.setEnabled(false);
        				//columnPoints.setEnabled(false);
        				break;

      		  		case "Student Name":
      		  			columnAssignment.setEnabled(false);
        				//columnPoints.setEnabled(false);
        				break;

        			case "Ignore":
        				columnAssignment.setEnabled(false);
        				//columnPoints.setEnabled(false);
        				break;
        		}
			}
		}

	}
}

class NullNumberValidator implements INullAcceptingValidator<Long> {
	@Override
	public void validate(IValidatable<Long> validatable) {

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


	public Assignment FindAssignmentByName()
	{
		String compString = column.getColumnTitle();
		for (Assignment assignment : assignmentList) {
			if (assignment.getName().equals(compString)) {
				return assignment;
			}
		}
		return null;
	}
}

class ColumnMap implements Serializable {

	private static final long serialVersionUID = 1L;

	public ColumnMap(ImportedSpreadsheetWrapper spreadSheetWrapper, final StringBuilder errorsb) {
		this.wrapper = spreadSheetWrapper;
		this.errorStrings = errorsb;
	}

	public List<ImportedColumn> getColumnList()
	{
		return wrapper.getColumns();
	}

	@Getter
	@Setter
	private StringBuilder errorStrings;

	@Getter
	@Setter
	private ImportedSpreadsheetWrapper wrapper;
}


