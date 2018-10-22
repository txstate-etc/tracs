package org.sakaiproject.gradebookng.tool.panels.importExport;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.component.GbFeedbackPanel;
import org.sakaiproject.gradebookng.tool.pages.ImportExportPage;
import org.sakaiproject.gradebookng.tool.panels.AddOrEditGradeItemPanelContent;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.gradebookng.business.model.ImportedColumn;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.ImportedSpreadsheetWrapper;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.gradebookng.business.util.ImportGradesHelper;
import org.sakaiproject.gradebookng.business.util.MessageHelper;
import org.sakaiproject.gradebookng.tool.model.ImportWizardModel;
import org.sakaiproject.user.api.User;
import org.sakaiproject.gradebookng.business.exception.GbImportCommentMissingItemException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapInputColumnsStep extends Panel {

	private static final long serialVersionUID = 1L;
	private final String panelId;
	private final IModel<ImportWizardModel> model;
	private Map<String, String> userMap;
	private ImportedSpreadsheetWrapper spreadSheetWrapper;
	private ListView<ImportedColumn> listItems;
	private List<ImportedColumn> importedColumns;
	private List<Assignment> assignmentList;
	private List<String> assignmentStringList;
	private List<String> columnTypeList;

	private final String NEW_GB_ASSIGNMENT = "NEW Gradebook Item";

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;


	public MapInputColumnsStep(final String id, final IModel<ImportWizardModel> model) {
		super(id);
		panelId = id;
		this.model = model;
	}

	@Override
	public void onInitialize() 
	{
		super.onInitialize();

		final ImportWizardModel wizardModel = this.model.getObject();
		spreadSheetWrapper = wizardModel.getWrapper();
		importedColumns = spreadSheetWrapper.getColumns();
		assignmentStringList = this.GenerateAssignmentList();
		userMap = this.getUserMap();

		columnTypeList = Arrays.asList("Grades", 
			"Comments", 
			"Student ID", 
			"Student Name", 
			"Ignore");

		add(new MappingForm("form"));		       
	}

	private List<String> GenerateAssignmentList()
	{
		ArrayList<String> returnList = new ArrayList<String>();
		returnList.add(NEW_GB_ASSIGNMENT);
		assignmentList = MapInputColumnsStep.this.businessService.getGradebookAssignments();
		for (Assignment assignment : assignmentList) {
			returnList.add(assignment.getName());
		}

		return returnList;
	}

	private String FindAssignmentByColumn(ImportedColumn column, List<String> assignmentList)
	{
		String compString = column.getColumnTitle();
		if (compString == null) {
			return null;
		}
		for (String assignment : assignmentList) {
			if (assignment.equals(compString)) {
				return assignment;
			}
		}
		return compString.contains(NEW_GB_ASSIGNMENT) ? NEW_GB_ASSIGNMENT : null;
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

			ArrayList<ImportedColumn> listViewData = new ArrayList<ImportedColumn>();
				for(ImportedColumn ic : importedColumns) {
				listViewData.add(ic);
			}

			IChoiceRenderer defaultRenderer = new IChoiceRenderer() {
				public Object getDisplayValue(Object object) {
					return (String)object;
				}

				public String getIdValue(Object object, int index) {
					return (String)object;
				}

			};

			//ListView contains one ListItem per column. 
			//Each ListItem holds a set of controls allowing the User to adjust column data
			listItems = new ListView<ImportedColumn>("listItems", listViewData) {

				@Override
				public void populateItem(final ListItem<ImportedColumn> item) {
					ImportedColumn newItem = item.getModelObject();

					//Label with the Column Number and column header
					Label columnDesc = new Label("columnLabel", String.format("* Column %d (%s)", this.size(), newItem.getUnparsedTitle()));
					item.add(columnDesc);
					
					//Dropdown menu to select Column Type. The value determines visibility status of the next two controls
					DropDownChoice ddcType = new DropDownChoice("columnType", Model.of(newItem.getFriendlyType()), columnTypeList);
					ddcType.setChoiceRenderer(defaultRenderer);
					ddcType.add(new AjaxFormComponentUpdatingBehavior("onchange") {
						@Override
						protected void onUpdate(AjaxRequestTarget target) {
							ListItem<ImportedColumn> listItem = (ListItem<ImportedColumn>)getComponent().getParent();
							setListItemVisibility(listItem);

                			DropDownChoice columnAssignment = (DropDownChoice)listItem.get("columnAssignment");
                			target.add(columnAssignment);
                			NumberTextField columnPoints = (NumberTextField)listItem.get("columnPoints");
                			target.add(columnPoints);
                			DropDownChoice columnNewAssignmentIndex = (DropDownChoice)listItem.get("columnNewAssignmentIndex");
							target.add(columnNewAssignmentIndex);	    		
						}

					});
					item.add(ddcType);

					//Dropdown menu to select the Assignment. Visible only for Columns containing Grades or Comments
					String matchingAssignment = MapInputColumnsStep.this.FindAssignmentByColumn(newItem, assignmentStringList);
					DropDownChoice ddcAssignment = new DropDownChoice("columnAssignment", Model.of(matchingAssignment), assignmentStringList) {
						@Override
						protected String getNullValidDisplayValue() {
							return "Select One...";
						}
					};
					ddcAssignment.setChoiceRenderer(defaultRenderer);
					ddcAssignment.setOutputMarkupId(true);
					ddcAssignment.setOutputMarkupPlaceholderTag(true);
					ddcAssignment.setNullValid(true);
					ddcAssignment.add(new AjaxFormComponentUpdatingBehavior("onchange") {
						@Override
						protected void onUpdate(AjaxRequestTarget target) {
							ListItem<ImportedColumn> listItem = (ListItem<ImportedColumn>)getComponent().getParent();
							DropDownChoice columnAssignment = (DropDownChoice)listItem.get("columnAssignment");
							DropDownChoice newIndex = (DropDownChoice)listItem.get("columnNewAssignmentIndex");

							newIndex.setVisible(columnAssignment.getValue().equals(NEW_GB_ASSIGNMENT));

							target.add(newIndex);
						}
					});
					item.add(ddcAssignment);


					List<Integer> assignmentIndexes = IntStream.rangeClosed(1,importedColumns.size()).boxed().collect(Collectors.toList());
					DropDownChoice ddcNewAssignmentIndex = new DropDownChoice("columnNewAssignmentIndex", Model.of(1), assignmentIndexes);
					ddcNewAssignmentIndex.setOutputMarkupId(true);
					ddcNewAssignmentIndex.setOutputMarkupPlaceholderTag(true);
					ddcNewAssignmentIndex.setVisible(false);
					item.add(ddcNewAssignmentIndex);

					//Text field to enter the Assignment's max points. Visible only for Columns containing Grades.
					Double dModel;
					try {
						dModel = Double.parseDouble(newItem.getPoints());
					} catch (Exception ex) {
						dModel = 0D;
					}
					NumberTextField<Double> pointsTextField = new NumberTextField<Double>("columnPoints", Model.of(dModel), Double.class) {
						@Override
						public boolean isInputNullable() {
							return true;
						}
					};
        			pointsTextField.setOutputMarkupId(true);
        			pointsTextField.setOutputMarkupPlaceholderTag(true);
         			item.add(pointsTextField);

         			setListItemVisibility(item);
				}	
			};
			listItems.setReuseItems(true);
			add(listItems);

			final Button submit = new Button("continuebutton") {

            	@Override
           		public void onSubmit() {
           			if (!validateForm(listItems)) {
						return;
					}
           			int studentIdIndex = -1;
					int studentNameIndex = -1;
					
                	for(int i = 0; i < listItems.size(); i ++) {
                		
                		ImportedColumn currentColumn = importedColumns.get(i);

                		ListItem<ImportedColumn> currentListItem = (ListItem<ImportedColumn>)listItems.get(i);
                		DropDownChoice columnType = (DropDownChoice)currentListItem.get("columnType"); 
                		DropDownChoice columnAssignment = (DropDownChoice)currentListItem.get("columnAssignment");
                		NumberTextField columnPoints = (NumberTextField)currentListItem.get("columnPoints");
                		DropDownChoice columnNewAssignmentIndex = (DropDownChoice)currentListItem.get("columnNewAssignmentIndex");

                		String columnName = columnAssignment.getValue();
                		if (columnName.equals(NEW_GB_ASSIGNMENT)) {
                			columnName = String.format("%s %s", NEW_GB_ASSIGNMENT, columnNewAssignmentIndex.getModelObject());
                		}
                		
                		switch(columnType.getValue())
                		{
                			case "Grades":                				
                				Double pointValue = Double.parseDouble(columnPoints.getValue());
                				pointValue = pointValue == null ? 0D : pointValue;
                				if (pointValue > 0) {
                					columnName += " [" + pointValue.toString() + "]";
                				}
                				spreadSheetWrapper.setRawDataValue(0, i, columnName);                				
                				break;

                			case "Comments":
                				spreadSheetWrapper.setRawDataValue(0, i, "* " + columnName);
                				break;

                			case "Student ID":
                				studentIdIndex = i;
                				break;

              		  		case "Student Name":
              		  			studentNameIndex = i;
                				break;

                			case "Ignore":
                				spreadSheetWrapper.setRawDataValue(0, i, "## " + columnName);
                				break;
                		}
                	}

                	ImportGradesHelper.setUserInfoPositions(studentIdIndex, studentNameIndex);

                	List<List<String>> modifiedRawData = spreadSheetWrapper.getRawData();
                	spreadSheetWrapper = ImportGradesHelper.reParseStringLists(modifiedRawData, userMap);

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

					for (String warningString : ImportGradesHelper.WarningsList) {
						getSession().warn(warningString);
					}
					
					// repaint panel
					final ImportWizardModel importWizardModel = new ImportWizardModel();
					importWizardModel.setProcessedGradeItems(processedGradeItems);
					importWizardModel.setWrapper(spreadSheetWrapper);
					final Component newPanel = new GradeItemImportSelectionStep(MapInputColumnsStep.this.panelId, Model.of(importWizardModel));
					newPanel.setOutputMarkupId(true);
					MapInputColumnsStep.this.replaceWith(newPanel);

            	}
   		 	};
        	add(submit);

        	//add(new GbFeedbackPanel("mapInputColumnsStepFeedback"));

        	final Button cancel = new Button("backbutton") {
        		private static final long serialVersionUID = 1L;
        		
        		@Override
        		public void onSubmit() {
        			// clear any previous errors
				final ImportExportPage page = (ImportExportPage) getPage();
				page.clearFeedback();

				final Component newPanel = new GradeImportUploadStep(MapInputColumnsStep.this.panelId);
				newPanel.setOutputMarkupId(true);
				MapInputColumnsStep.this.replaceWith(newPanel);
        		}
        	};
        add(cancel);

		}

		private boolean validateForm(ListView<ImportedColumn> listView) {
			//TODO: Add more form validation here as needed
			boolean returnVal = true;
			boolean gradesNoAssignment = false;
			boolean commentsNoAssignment = false;
			int studentIdColumnCount = 0;
			int studentNameColumnCount = 0;
			List<String> gradeAssignmentsUsed = new ArrayList<String>();
			List<String> commentAssignmentsUsed = new ArrayList<String>();

			for(int i = 0; i < listView.size(); i ++) {
				ListItem<ImportedColumn> listItem = (ListItem<ImportedColumn>)listView.get(i);
				String typeValue = ((DropDownChoice)listItem.get("columnType")).getValue();
        		String assignmentValue = ((DropDownChoice)listItem.get("columnAssignment")).getValue();
        		String points = ((NumberTextField)listItem.get("columnPoints")).getValue();
        		Long assignmentIndex = Long.parseLong(((DropDownChoice)listItem.get("columnNewAssignmentIndex")).getValue());
        		assignmentIndex++;
        		String fullAssignmentName = assignmentValue == NEW_GB_ASSIGNMENT ? assignmentValue + " " + assignmentIndex : assignmentValue;

				switch(typeValue) {
					case "Student ID":
						studentIdColumnCount++;
						break;

					case "Student Name":
						studentNameColumnCount++;
						break;

					case "Grades":
						if (fullAssignmentName == null || fullAssignmentName == "") {
							gradesNoAssignment = true;
							returnVal = false;
							break;
						}
						if (gradeAssignmentsUsed.contains(fullAssignmentName)) {
							error(MessageHelper.getString("importExport.mapping.duplicategrade", fullAssignmentName));
							returnVal = false;
						}
						gradeAssignmentsUsed.add(fullAssignmentName);
						break;

					case "Comments":
					if (fullAssignmentName == null || fullAssignmentName == "") {
							commentsNoAssignment = true;
							returnVal = false;
							break;
						}
						if (commentAssignmentsUsed.contains(fullAssignmentName)) {
							error(MessageHelper.getString("importExport.mapping.duplicatecomment", fullAssignmentName));
							returnVal = false;
						}
						commentAssignmentsUsed.add(fullAssignmentName);
						break;
				}
			}

			if (studentIdColumnCount != 1) {
				error(MessageHelper.getString("importExport.mapping.duplicateid"));
				returnVal = false;
			}

			if (studentNameColumnCount > 1) {
				error(MessageHelper.getString("importExport.mapping.duplicatename"));
				returnVal = false;
			}

			if (gradesNoAssignment) error(MessageHelper.getString("importExport.mapping.noassignment-grade"));
			if (commentsNoAssignment) error(MessageHelper.getString("importExport.mapping.noassignment-comment"));

			return returnVal;
		}

		private void setListItemVisibility(ListItem<ImportedColumn> listItem) {
			String columnTitle = listItem.getModelObject().getColumnTitle();

			DropDownChoice columnType = (DropDownChoice)listItem.get("columnType");
			NumberTextField columnPoints = (NumberTextField)listItem.get("columnPoints");
			DropDownChoice columnNewAssignmentIndex = (DropDownChoice)listItem.get("columnNewAssignmentIndex");

			DropDownChoice columnAssignment = (DropDownChoice)listItem.get("columnAssignment");
			String assignmentValue = columnAssignment.getValue();
			boolean isNewAssignment = assignmentValue != null && assignmentValue.contains(NEW_GB_ASSIGNMENT);

            switch(columnType.getValue()) {
           		case "Grades":
           			columnAssignment.setVisible(true);
           			columnPoints.setVisible(true);
           			columnNewAssignmentIndex.setVisible(isNewAssignment);
           			if (isNewAssignment) {
           				try {
           					String[] tokens = columnTitle.split(" ");
           					Integer newIndex = Integer.parseInt(tokens[tokens.length - 1]);
           					columnNewAssignmentIndex.setModelObject(newIndex);
           				} catch (NumberFormatException nfe) {
           					//just do nothing and continue
           				}
           				
           			}
                    break;

                case "Comments":
                    columnAssignment.setVisible(true);
                    columnPoints.setVisible(false);
                    columnNewAssignmentIndex.setVisible(isNewAssignment);
                    if (isNewAssignment) {
           				try {
           					String[] tokens = columnTitle.split(" ");
           					Integer newIndex = Integer.parseInt(tokens[tokens.length - 1]);
           					columnNewAssignmentIndex.setModelObject(newIndex);
           				} catch (NumberFormatException nfe) {
           					//just do nothing and continue
           				}
           				
           			}
                    break;

                case "Student ID":
                    columnAssignment.setVisible(false);
                    columnPoints.setVisible(false);
                    columnNewAssignmentIndex.setVisible(false);
                    break;

                case "Student Name":
                    columnAssignment.setVisible(false);
                    columnPoints.setVisible(false);
                    columnNewAssignmentIndex.setVisible(false);
                    break;

                case "Ignore":
                    columnAssignment.setVisible(false);
                    columnPoints.setVisible(false);
                    columnNewAssignmentIndex.setVisible(false);
                    break;
			}              
		}
	}
}