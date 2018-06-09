package org.sakaiproject.gradebookng.tool.pages;

import java.text.MessageFormat;

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

/**
 * Import Export page
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
public class SubmitGradesConfirmationPage extends BasePage {

	private static final long serialVersionUID = 1L;
	
	String externalLink = null;

	public SubmitGradesConfirmationPage() {
		disableLink(this.submitGradesPageLink);
	}

	public SubmitGradesConfirmationPage(String externalLink, String gradeSubmitType) {


		final Form form = new Form("form");

		form.add(new Label("pageTitle", MessageFormat.format(getString("submitgradespage.section.title"), getString(gradeSubmitType))));

		String url = externalLink;
		final ExternalLink link = new ExternalLink("submitGradesConfirmLink", url) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(final ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("href", url);
				tag.put("target","_blank");
				tag.put("title", "Submit " + gradeSubmitType);
				tag.getAttributes().put("menubar", "yes");
				tag.getAttributes().put("location", "yes");
				tag.getAttributes().put("resizable", "yes");
				tag.getAttributes().put("scrollbars", "yes");
				tag.getAttributes().put("status", "yes");
			}
		};

		form.add(link);

		final Button cancel = new Button("cancel") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit() {
				setResponsePage(SubmitGradesPage.class);
			}
		};
		cancel.setDefaultFormProcessing(false);
		form.add(cancel);
		add(form);

	}

}
