package org.sakaiproject.gradebookng.tool.pages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.gradebookng.business.SubmitResultKey;
import org.sakaiproject.gradebookng.business.model.GradeSubmissionResult;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.panels.GradeSubmissionPanel;
import org.sakaiproject.gradebookng.tool.panels.importExport.GradeImportUploadStep;
import org.sakaiproject.tool.gradebook.Gradebook;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SubmitGradesPage extends BasePage {

	private static final long serialVersionUID = 1L;
	
	String externalLink = null;

	public SubmitGradesPage() {
		disableLink(this.submitGradesPageLink);
	}

	public enum GradeSubmitType {
		FINAL("finalGrade"),
		MIDTERM("midTerm"),
		VIEWRECEIPT("viewReceipt");

		private String type;

		GradeSubmitType(final String type) {
			this.type = type;
		}

		public String getType() {
			return this.type;
		}

	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		final Form form = new Form("form");

		//page instruction text
		String instructions = getString("submitgradespage.instructions");
		form.add(new Label("instructions", instructions).setEscapeModelStrings(false));

		//final grade
		String finalGradeWording = getString(GradeSubmitType.FINAL.getType());
		form.add(new Label("finalGradesTitle", MessageFormat.format(getString("submitgradespage.section.title"), finalGradeWording)));
		form.add(new Label("finalGradesInstruction1", MessageFormat.format(getString("submitgradespage.section.instruction1"), finalGradeWording)));
		form.add(new Label("finalGradesInstruction2", MessageFormat.format(getString("submitgradespage.section.instruction2"), finalGradeWording.toLowerCase())).setEscapeModelStrings(false));

		final GbAjaxButton submitFinalGrade = new GbAjaxButton("submitFinalGrades") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

				String gradeSubmitType = GradeSubmitType.FINAL.getType();
				processSubmitGrades(gradeSubmitType);
			}
		};
		form.add(submitFinalGrade);

		// visible control for mid-term grade submission menu
		final boolean allowMidTermSubmit = businessService.allowMidTermGradeSubmission();

		//Mid Term
		String midTermWording = getString(GradeSubmitType.MIDTERM.getType());
		Label label1 = new Label("midTermTitle", MessageFormat.format(getString("submitgradespage.section.title"), midTermWording));
		Label label2 = new Label("midTermInstruction1", MessageFormat.format(getString("submitgradespage.section.instruction1"), midTermWording));
		Label label3 = new Label("midTermInstruction2", MessageFormat.format(getString("submitgradespage.section.instruction2"), midTermWording.toLowerCase()));

		label1.setVisible(allowMidTermSubmit);
		label2.setVisible(allowMidTermSubmit);
		label3.setVisible(allowMidTermSubmit);

		form.add(label1);
		form.add(label2);
		form.add(label3.setEscapeModelStrings(false));

		final GbAjaxButton submitMidTerm = new GbAjaxButton("submitMidTerm") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

				String gradeSubmitType = GradeSubmitType.MIDTERM.getType();
				processSubmitGrades(gradeSubmitType);
			}
		};
		submitMidTerm.setVisible(allowMidTermSubmit);
		form.add(submitMidTerm);

		form.add(new Label("viewReceiptTitle", instructions).setEscapeModelStrings(false));
		form.add(new Label("viewReceiptInstruction", getString("submitgradespage.section.instruction.viewreceipt")).setEscapeModelStrings(false));
		
		final GbAjaxButton submitViewReceipt = new GbAjaxButton("submitViewReceipt") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

				String gradeSubmitType = GradeSubmitType.VIEWRECEIPT.getType();
				processSubmitGrades(gradeSubmitType);
			}
		};
		form.add(submitViewReceipt);


		final Button cancel = new Button("cancel") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit() {
				setResponsePage(GradebookPage.class);
			}
		};
		cancel.setDefaultFormProcessing(false);
		form.add(cancel);
		add(form);

	}

	/* Helper methods */
	private void processSubmitGrades (String gradeSubmitType) {

		final Gradebook gradebook = businessService.getGradebook();
		GradeSubmissionResult gradeSubmissionResult;
		if(gradeSubmitType.equalsIgnoreCase(GradeSubmitType.VIEWRECEIPT.getType()))
			gradeSubmissionResult = businessService.viewSubmissionReceipt(gradebook.getUid());
		else 
			gradeSubmissionResult = businessService.submitGrade(gradebook.getUid(), gradeSubmitType);
	
		String gradeSubmission = getString(gradeSubmitType);
		
		final String data = gradeSubmissionResult.getData();
		final boolean isSuccess = Boolean.parseBoolean((String)getJsonMap(gradeSubmissionResult.getData()).get(SubmitResultKey.SUBMIT_SUCCESS.getProperty()));
		String url = (String)getJsonMap(gradeSubmissionResult.getData()).get(SubmitResultKey.SUBMIT_PAGE_URL.getProperty());

		if (Integer.valueOf(500).compareTo(gradeSubmissionResult.getStatus()) == 0) {
			error(new StringResourceModel("message.gradesubmit.error1", null, new Object[] {gradeSubmission}).getObject());
		} else if (Integer.valueOf(200).compareTo(gradeSubmissionResult.getStatus()) == 0 && !isSuccess) {
			error(new StringResourceModel("message.gradesubmit.error2", null, new Object[] {gradeSubmission}).getObject());
		} else if (Integer.valueOf(200).compareTo(gradeSubmissionResult.getStatus()) == 0 && isSuccess) {
			getSession().success(MessageFormat.format(getString("message.gradesubmit.success"), gradeSubmission));
			Page responsePage = new SubmitGradesConfirmationPage(url, gradeSubmitType);
			setResponsePage(responsePage);
		}
		else {
			error(new StringResourceModel("message.gradesubmit.error", null, new Object[] {gradeSubmission}).getObject());
		}

	}

	//convert json string to Map
	private Map<String, Object> getJsonMap (String jsonDataString) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			map = mapper.readValue(jsonDataString, new TypeReference<Map<String,String>>(){});
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

}
