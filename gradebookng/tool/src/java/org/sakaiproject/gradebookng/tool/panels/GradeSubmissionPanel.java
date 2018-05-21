package org.sakaiproject.gradebookng.tool.panels;


import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GbGradingType;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.SubmitResultKey;
import org.sakaiproject.gradebookng.business.model.GradeSubmissionResult;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.component.GbFeedbackPanel;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.AssignmentHasIllegalPointsException;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.ConflictingExternalIdException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.tool.gradebook.Gradebook;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.gradebookng.tool.model.RescaleAnswer;

/**
 * The panel for submit final and midTerm (Initial Academic Feedback) grades window
 *
 * @author Yuanhua Qu (Txstate University)
 *
 */
public class GradeSubmissionPanel extends Panel {

	private static final long serialVersionUID = 1L;

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	/**
	 * How this panel is rendered
	 */
	enum Mode {
		FINAL,
		MIDTERM;
	}

	Mode mode;
	ModalWindow window;
	String gradeSubmitType;
	String url = null;

	public GradeSubmissionPanel(final String id, final ModalWindow window, final String gradeSubmitType) {
		super(id);
		this.window = window;
		// determine mode
		if (gradeSubmitType.equalsIgnoreCase(getString("finalGrade"))) {
			this.mode = Mode.FINAL;
		} else if (gradeSubmitType.equalsIgnoreCase(getString("midTerm"))){
			this.mode = Mode.MIDTERM;
		}
		this.gradeSubmitType = gradeSubmitType;
	};

	@Override
	protected void onInitialize() {
		super.onInitialize();

		final Gradebook gradebook = this.businessService.getGradebook();
		GradeSubmissionResult gradeSubmissionResult = GradeSubmissionPanel.this.businessService.submitGrade(gradebook.getUid(), gradeSubmitType);

		final Form form = new Form ("gradeSubmissionForm");

		form.add(new Label("gradeSubmissionWarning1", MessageFormat.format(getString("label.gradesubmit.warning1"), gradeSubmitType.toLowerCase())));
		form.add(new Label("gradeSubmissionWarning2", MessageFormat.format(getString("label.gradesubmit.warning2"), gradeSubmitType)));

		final Label gradeSubmissionWarning3 = new Label("gradeSubmissionWarning3", new StringResourceModel("label.gradesubmit.warning3", null, new Object[] { gradeSubmitType }).getString());
		gradeSubmissionWarning3.setEscapeModelStrings(false);
		form.add(gradeSubmissionWarning3);

		final String data = gradeSubmissionResult.getData();
		final boolean isSuccess = Boolean.parseBoolean((String)getJsonMap(gradeSubmissionResult.getData()).get(SubmitResultKey.SUBMIT_SUCCESS.getProperty()));
		url = (String)getJsonMap(gradeSubmissionResult.getData()).get(SubmitResultKey.SUBMIT_PAGE_URL.getProperty());

		String gradeSubmission;
		if(mode.equals(Mode.FINAL)) {
			gradeSubmission = getString("finalGrade");
		} else if (mode.equals(Mode.MIDTERM)) {
			gradeSubmission = getString("midTerm");
		} else {
			gradeSubmission = getString("notDefinedGradeSubmissionType");
		}

		if (Integer.valueOf(500).compareTo(gradeSubmissionResult.getStatus()) == 0) {
			error(new StringResourceModel("message.gradesubmit.error1", null, new Object[] {gradeSubmission}).getObject());
			form.add(new GbFeedbackPanel("addGradeSubmitFeedback"));
		} else if (Integer.valueOf(200).compareTo(gradeSubmissionResult.getStatus()) == 0 && !isSuccess) {
			error(new StringResourceModel("message.gradesubmit.error2", null, new Object[] {gradeSubmission}).getObject());
			form.add(new GbFeedbackPanel("addGradeSubmitFeedback"));
		} else if (Integer.valueOf(200).compareTo(gradeSubmissionResult.getStatus()) == 0 && isSuccess) {
//			getSession().success(MessageFormat.format(getString("message.gradesubmit.success"), gradeSubmission));
//			setResponsePage(getPage().getPageClass());
//			form.add(new GbFeedbackPanel("addGradeSubmitFeedback"));
		}
		else {
			error(new StringResourceModel("message.gradesubmit.error", null, new Object[] {gradeSubmission}).getObject());
			form.add(new GbFeedbackPanel("addGradeSubmitFeedback"));
		}

		final ExternalLink link = new ExternalLink("gradeSubmitLink", url) {

			@Override
			protected void onComponentTag(final ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("target","_blank");
				tag.put("title", "Submit " + gradeSubmitType);
				tag.getAttributes().put("menubar", "yes");
				tag.getAttributes().put("location", "yes");
				tag.getAttributes().put("resizable", "yes");
				tag.getAttributes().put("scrollbars", "yes");
				tag.getAttributes().put("status", "yes");
			}

		};
		// submit button label
//		keep following in case in future we may present customized button for diff submit
//		link.add(new Label("submitLabel", getSubmitButtonLabel()));
		link.add(new Label("submitLabel", getString("button.submit")));
		form.add(link);

		// cancel button
		final GbAjaxButton cancel = new GbAjaxButton("cancel") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
				window.close(target);
			}
		};
		cancel.setDefaultFormProcessing(false);
		form.add(cancel);

		add(form);
	};

	/**
	 * Helper to get the model for the button
	 *
	 * @return
	 */
	private ResourceModel getSubmitButtonLabel() {
		if (this.mode == Mode.FINAL) {
			return new ResourceModel("button.submit");
		} else {
			return new ResourceModel("button.create");
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
