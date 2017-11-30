package org.sakaiproject.gradebookng.tool.panels.importExport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItemDetail;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItemStatus;
import org.sakaiproject.gradebookng.tool.model.ImportWizardModel;
import org.sakaiproject.gradebookng.tool.pages.ImportExportPage;

import lombok.extern.slf4j.Slf4j;

/**
 * Page to allow the user to select which items in the imported file ar to be imported
 */
@Slf4j
public class GradeItemImportSelectionStep extends Panel {
	private static final long serialVersionUID = 1L;

	private final String panelId;
	private final IModel<ImportWizardModel> model;
	private ListView<ProcessedGradeItemDetail> changeDetails;

	// a count of the items that can be selected
	private int selectableItems = 0;

	private boolean naHidden = false;

	public GradeItemImportSelectionStep(final String id, final IModel<ImportWizardModel> importWizardModel) {
		super(id);
		this.panelId = id;
		this.model = importWizardModel;
	}

	private String createChangesString(List<ProcessedGradeItemDetail> details) {
		String returnString = "";
		int gradeChanges = 0;
		int commentChanges = 0;
		for (ProcessedGradeItemDetail detail : details) {
			if (detail.hasGradeChange()) {
				gradeChanges++;
			}
			if (detail.hasCommentChange()) {
				commentChanges++;
			}
		}

		returnString += String.format("%d %s", gradeChanges, gradeChanges == 1 ? "grade " : "grades ");
		returnString += String.format("%d %s", commentChanges, commentChanges == 1 ? "comment " : "comments ");

		return returnString;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		// unpack model
		final ImportWizardModel importWizardModel = this.model.getObject();

		// get the count of items that are selectable
		GradeItemImportSelectionStep.this.selectableItems = importWizardModel.getProcessedGradeItems().stream().filter(item -> item.getStatus().getStatusCode() != ProcessedGradeItemStatus.STATUS_NA).collect(Collectors.toList()).size();

		// label to show if all items are actually hidden
		final Label allHiddenLabel = new Label("allHiddenLabel", new ResourceModel("importExport.selection.hideitemsallhidden")) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return GradeItemImportSelectionStep.this.naHidden && (GradeItemImportSelectionStep.this.selectableItems == 0);
			}
		};
		allHiddenLabel.setOutputMarkupPlaceholderTag(true);
		add(allHiddenLabel);

		// button to hide NA/no changes items
		final AjaxLink<Void> hideNoChanges = new AjaxLink<Void>("hideNoChanges") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(final AjaxRequestTarget target) {

				// toggle button state
				if(GradeItemImportSelectionStep.this.naHidden) {
					//toggling off
					GradeItemImportSelectionStep.this.naHidden = false;
					this.add(AttributeModifier.replace("class", "button"));
					this.add(AttributeModifier.replace("aria-pressed", "false"));
				} else {
					//toggling on
					GradeItemImportSelectionStep.this.naHidden = true;
					this.add(AttributeModifier.replace("class", "button on"));
					this.add(AttributeModifier.replace("aria-pressed", "true"));
				}
				target.add(this);
				target.add(allHiddenLabel);

				// toggle elements
				target.appendJavaScript("$('.no_changes').toggle();");
				if(GradeItemImportSelectionStep.this.selectableItems == 0) {
					target.appendJavaScript("$('.selection_form').toggle();");
					//TODO show a message
				}
			}
		};
		add(hideNoChanges);

		final WebMarkupContainer detailsTablePanel = new WebMarkupContainer("detailsTable");
		detailsTablePanel.setOutputMarkupId(true);
		detailsTablePanel.setOutputMarkupPlaceholderTag(true);

		changeDetails = new ListView<ProcessedGradeItemDetail>("gradeChangeDetail") {
			@Override
			protected void populateItem(final ListItem<ProcessedGradeItemDetail> itemDetail) {
				ProcessedGradeItemDetail itemModel = itemDetail.getModelObject();
				itemDetail.add(new Label("studentDetail", itemModel.getStudentEid()));

				String gradeChangeString = String.format("%s => %s", 
					itemModel.getPreviousGrade() == null ? "-" : itemModel.getPreviousGrade(), 
					itemModel.getGrade() == null ? "-" : itemModel.getGrade());
				itemDetail.add(new Label("gradeDetail", itemModel.hasGradeChange() ? gradeChangeString : "-"));

				String commentChangeString = String.format("%s => %s", 
					itemModel.getPreviousComment() == null ? "-" : itemModel.getPreviousComment(), 
					itemModel.getComment() == null ? "-" : itemModel.getComment());
				itemDetail.add(new Label("commentDetail", itemModel.hasCommentChange() ? commentChangeString : "-"));
			}
		};
		changeDetails.setOutputMarkupId(true);
		changeDetails.setOutputMarkupPlaceholderTag(true);
		detailsTablePanel.add(changeDetails);
		detailsTablePanel.setVisible(false);
		add(detailsTablePanel);

		final CheckGroup<ProcessedGradeItem> group = new CheckGroup<ProcessedGradeItem>("group", new ArrayList<ProcessedGradeItem>());

		final Form<?> form = new Form("form") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit() {
				boolean validated = true;

				final List<ProcessedGradeItem> selectedGradeItems = (List<ProcessedGradeItem>) group.getModelObject();
				log.debug("Processed items: " + selectedGradeItems.size());

				// this has an odd model so we need to have the validation in the onSubmit.
				if (selectedGradeItems.size() == 0) {
					validated = false;
					error(getString("importExport.selection.noneselected"));
				}

				if (validated) {

					// clear any previous errors
					final ImportExportPage page = (ImportExportPage) getPage();
					page.clearFeedback();

					// Process the selected items into the create/update lists
					final List<ProcessedGradeItem> itemsToUpdate = filterListByStatus(selectedGradeItems,
							Arrays.asList(ProcessedGradeItemStatus.STATUS_UPDATE, ProcessedGradeItemStatus.STATUS_NA));
					final List<ProcessedGradeItem> itemsToCreate = filterListByStatus(selectedGradeItems,
							Arrays.asList(ProcessedGradeItemStatus.STATUS_NEW));
					final List<ProcessedGradeItem> itemsToModify = filterListByStatus(selectedGradeItems,
							Arrays.asList(ProcessedGradeItemStatus.STATUS_MODIFIED));

					log.debug("Filtered Update items: " + itemsToUpdate.size());
					log.debug("Filtered Create items: " + itemsToCreate.size());
					log.debug("Filtered Modify items: " + itemsToModify.size());

					// Don't want comment items here
					// TODO using N/A to indicate this is a comment column? How about an enum...
					itemsToCreate.removeIf(i -> StringUtils.equals("N/A", i.getItemPointValue()));
					log.debug("Actual items to create: " + itemsToCreate.size());

					// repaint panel
					Component newPanel = null;
					importWizardModel.setSelectedGradeItems(selectedGradeItems);
					importWizardModel.setItemsToCreate(itemsToCreate);
					importWizardModel.setItemsToUpdate(itemsToUpdate);
					importWizardModel.setItemsToModify(itemsToModify);

					// create those that need to be created. When finished all, continue.
					if (itemsToCreate.size() > 0) {
						importWizardModel.setStep(1);
						importWizardModel.setTotalSteps(itemsToCreate.size());
						newPanel = new CreateGradeItemStep(GradeItemImportSelectionStep.this.panelId, Model.of(importWizardModel));
					} else {
						newPanel = new GradeImportConfirmationStep(GradeItemImportSelectionStep.this.panelId, Model.of(importWizardModel));
					}
					newPanel.setOutputMarkupId(true);
					GradeItemImportSelectionStep.this.replaceWith(newPanel);
				}

			}
		};
		add(form);
		form.add(group);

		final Button backButton = new Button("backbutton") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit() {

				// clear any previous errors
				final ImportExportPage page = (ImportExportPage) getPage();
				page.clearFeedback();

				final Component newPanel = new MapInputColumnsStep(GradeItemImportSelectionStep.this.panelId, GradeItemImportSelectionStep.this.model);
				newPanel.setOutputMarkupId(true);
				GradeItemImportSelectionStep.this.replaceWith(newPanel);
			}
		};
		backButton.setDefaultFormProcessing(false);
		group.add(backButton);

		final Button cancelButton = new Button("cancelbutton") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit() {
				// clear any previous errors
				final ImportExportPage page = (ImportExportPage) getPage();
				page.clearFeedback();

				setResponsePage(ImportExportPage.class);
			}
		};
		cancelButton.setDefaultFormProcessing(false);
		group.add(cancelButton);

		group.add(new Button("nextbutton"));

		group.add(new CheckGroupSelector("groupselector"));

		final ListView<ProcessedGradeItem> gradeList = new ListView<ProcessedGradeItem>("grades", importWizardModel.getProcessedGradeItems()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<ProcessedGradeItem> item) {

				final ProcessedGradeItem importedItem = item.getModelObject();

				final Check<ProcessedGradeItem> checkbox = new Check<>("checkbox", item.getModel());
				final Label itemTitle = new Label("itemTitle", importedItem.getItemTitle());
				final Label itemPointValue = new Label("itemPointValue", importedItem.getItemPointValue());
				final Label itemStatus = new Label("itemStatus");

				final Button detailButton = new Button("detailButton");
				detailButton.add(new AjaxEventBehavior("onclick") {
        			@Override
        			protected void onEvent(final AjaxRequestTarget target) {
            			ListItem<ProcessedGradeItem> listItem = (ListItem<ProcessedGradeItem>)getComponent().getParent();
            			List<ProcessedGradeItemDetail> changedList = new ArrayList<ProcessedGradeItemDetail>();
            			for (ProcessedGradeItemDetail detail : listItem.getModelObject().getProcessedGradeItemDetails()) {
            				if (detail.hasAnyChange()) {
            					changedList.add(detail);
            				}
            			}
            			changeDetails.setList(changedList);

            			WebMarkupContainer parentContainer = (WebMarkupContainer)changeDetails.getParent();
            			parentContainer.setVisible(true);
            			target.add(parentContainer);
        			}
    			});
				detailButton.setOutputMarkupId(true);

				item.add(checkbox);
				item.add(itemTitle);
				item.add(itemPointValue);
				item.add(itemStatus);
				item.add(detailButton);

				// Determine status label
				final ProcessedGradeItemStatus status = importedItem.getStatus();

				// For external items, set a different label and disable the control
				if (status.getStatusCode() == ProcessedGradeItemStatus.STATUS_EXTERNAL) {
					itemStatus.setDefaultModel(new StringResourceModel("importExport.status." + status.getStatusCode(), Model.of(status), null, status.getStatusValue()));
					item.setEnabled(false);
					item.add(new AttributeModifier("class", "external"));
				} else {

					itemStatus.setDefaultModel(new ResourceModel("importExport.status." + status.getStatusCode()));

					// if no changes, grey it out and remove checkbox
					if (status.getStatusCode() == ProcessedGradeItemStatus.STATUS_NA) {
						checkbox.setVisible(false);
						detailButton.setVisible(false);
						item.add(new AttributeAppender("class", Model.of("no_changes"), " "));
					} else {
						itemStatus.setDefaultModelObject((String)itemStatus.getDefaultModelObject() + String.format(" (%s)", createChangesString(importedItem.getProcessedGradeItemDetails())));
					}

				}

				final String naString = getString("importExport.selection.pointValue.na", new Model(), "N/A");
				if (naString.equals(item.getModelObject().getItemPointValue())) {
					item.add(new AttributeAppender("class", Model.of("comment"), " "));
				}
			}
		};

		gradeList.setReuseItems(true);
		group.add(gradeList);

	}

	private List<ProcessedGradeItem> filterListByStatus(final List<ProcessedGradeItem> gradeList, final List<Integer> statuses) {
		final List<ProcessedGradeItem> filteredList = new ArrayList<ProcessedGradeItem>();
		for (final ProcessedGradeItem gradeItem : gradeList) {
			if (statuses.contains(gradeItem.getStatus().getStatusCode())) {
				filteredList.add(gradeItem);
			}
		}
		return filteredList;
	}

}
