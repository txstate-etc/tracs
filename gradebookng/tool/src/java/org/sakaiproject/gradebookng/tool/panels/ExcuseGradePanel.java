package org.sakaiproject.gradebookng.tool.panels;

import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.service.gradebook.shared.Assignment;

public class ExcuseGradePanel extends Panel {

    private static final long serialVersionUID = 1L;

    private final ModalWindow window;

    @SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
    protected GradebookNgBusinessService businessService;

    public ExcuseGradePanel(final String id, final IModel<Map<String, Object>> model, final ModalWindow window) {
        super(id, model);
        this.window = window;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        // unpack model
        final Map<String, Object> modelData = (Map<String, Object>) getDefaultModelObject();
        final Long assignmentId = (Long) modelData.get("assignmentId");
        final String studentUuid = (String) modelData.get("studentUuid");
        final Assignment assignment = businessService.getAssignment(assignmentId);
        final String assignmentName = assignment.getName();

        final Form form = new Form("form");

        form.add(new Label("assignment_name", assignmentName));

        final GbAjaxButton submit = new GbAjaxButton("submit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                //a miracle or black magic occurs and the excuse is saved
                //final Long assignmentId, final String studentUuid, final boolean excludeFromGrade
                businessService.saveExcusedGrade(assignmentId, studentUuid, true);
                ExcuseGradePanel.this.window.close(target);
                setResponsePage(GradebookPage.class);
            }

        };
        form.add(submit);

        final GbAjaxButton cancel = new GbAjaxButton("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                ExcuseGradePanel.this.window.close(target);
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);
    }

}