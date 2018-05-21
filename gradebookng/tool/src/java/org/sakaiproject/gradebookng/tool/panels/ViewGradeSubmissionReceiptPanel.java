package org.sakaiproject.gradebookng.tool.panels;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.SubmitResultKey;
import org.sakaiproject.gradebookng.business.model.GradeSubmissionResult;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.component.GbFeedbackPanel;
import org.sakaiproject.tool.gradebook.Gradebook;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The panel for submit final and midTerm (Initial Academic Feedback) grades window
 *
 * @author Yuanhua Qu (Txstate University)
 *
 */
public class ViewGradeSubmissionReceiptPanel extends Panel {

	private static final long serialVersionUID = 1L;

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	ModalWindow window;
	String url = null;

	public ViewGradeSubmissionReceiptPanel(final String id, final ModalWindow window) {
		super(id);
		this.window = window;
	};

	@Override
	protected void onInitialize() {
		super.onInitialize();

		final Gradebook gradebook = this.businessService.getGradebook();
		GradeSubmissionResult gradeSubmissionResult = ViewGradeSubmissionReceiptPanel.this.businessService.viewSubmissionReceipt(gradebook.getId().toString());

		final String data = gradeSubmissionResult.getData();
		final boolean isSuccess = Boolean.parseBoolean((String)getJsonMap(gradeSubmissionResult.getData()).get(SubmitResultKey.SUBMIT_SUCCESS.getProperty()));
		url = (String)getJsonMap(gradeSubmissionResult.getData()).get(SubmitResultKey.SUBMIT_PAGE_URL.getProperty());


		if (Integer.valueOf(500).compareTo(gradeSubmissionResult.getStatus()) == 0) {
			error(getString("message.viewreceipt.error1"));
		} else if (Integer.valueOf(200).compareTo(gradeSubmissionResult.getStatus()) == 0 && !isSuccess) {
			error(getString("message.viewreceipt.error2"));
		} else if (Integer.valueOf(200).compareTo(gradeSubmissionResult.getStatus()) == 0 && isSuccess) {
//			getSession().success(getString("message.viewreceipt.success"));
//			setResponsePage(getPage().getPageClass());
		}
		else {
			error(getString("message.viewreceipt.error2"));
		}

		add(new GbFeedbackPanel("addViewSubRcptFeedback"));
		
		final ExternalLink link = new ExternalLink("viewSubRcptLink", url) {

			@Override
			protected void onComponentTag(final ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("target","_blank");
				tag.put("title", "Submit View Receipt");
				tag.getAttributes().put("menubar", "yes");
				tag.getAttributes().put("location", "yes");
				tag.getAttributes().put("resizable", "yes");
				tag.getAttributes().put("scrollbars", "yes");
				tag.getAttributes().put("status", "yes");
			}

		};
		add(link);

		// cancel button
		final GbAjaxButton cancel = new GbAjaxButton("cancel") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
				window.close(target);
			}
		};
		cancel.setDefaultFormProcessing(false);
		add(cancel);

	};

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
