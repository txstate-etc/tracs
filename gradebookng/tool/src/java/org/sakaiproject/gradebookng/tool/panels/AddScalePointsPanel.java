package org.sakaiproject.gradebookng.tool.panels;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GbGradingType;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.component.GbFeedbackPanel;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.validator.RangeValidator;
import lombok.Getter;
import lombok.Setter;
import org.sakaiproject.service.gradebook.shared.GraderPermission;
import org.sakaiproject.service.gradebook.shared.PermissionDefinition;

/**
 *
 * Panel for the modal window that allows an instructor to add points to all grades for an assignment
 *
 */
public class AddScalePointsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    @SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
    protected GradebookNgBusinessService businessService;
    private final IModel<Long> model;
    private final ModalWindow window;
    private static final double DEFAULT_VALUE = 0;

    public AddScalePointsPanel(final String id, final IModel<Long> model, final ModalWindow window) {
        super(id);
        this.model = model;
        this.window = window;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        final Long assignmentId = this.model.getObject();
        final Assignment assignment = this.businessService.getAssignment(assignmentId.longValue());

        final GbGradingType gradeType = GbGradingType.valueOf(this.businessService.getGradebook().getGrade_type());

        final GradeScale gradeScale = new GradeScale();
        gradeScale.setPoints(String.valueOf(DEFAULT_VALUE));
        final CompoundPropertyModel<GradeScale> formModel = new CompoundPropertyModel<GradeScale>(gradeScale);

        final Form<GradeScale> form = new Form<GradeScale>("form", formModel);

        Model<Double> pointsTextFieldModel = Model.of(0.0);
        final NumberTextField<Double> pointsTextField = new NumberTextField<Double>("points", pointsTextFieldModel, Double.class);        
        pointsTextField.setStep(1.0);

        final GbAjaxButton submit = new GbAjaxButton("submit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                final boolean success = AddScalePointsPanel.this.businessService.addScalePoints(assignment.getId(), assignment.getPoints(), Double.parseDouble(pointsTextField.getValue()));

                if (success) {
                    AddScalePointsPanel.this.window.close(target);
                    setResponsePage(GradebookPage.class);
                } else {
                    target.addChildren(form, GbFeedbackPanel.class);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.addChildren(form, GbFeedbackPanel.class);
            }
        };
        form.add(submit);

        final GbAjaxButton cancel = new GbAjaxButton("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                AddScalePointsPanel.this.window.close(target);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        form.add(pointsTextField.setRequired(true));
        String suffixString = gradeType == GbGradingType.PERCENTAGE ? "%" : "points";
        form.add(new Label("pointsSuffix", suffixString));

        add(form);
        
        form.add(new GbFeedbackPanel("addScalePointsFeedback"));
    }

    /**
     * Model for this form
     */
    class GradeScale implements Serializable {

        private static final long serialVersionUID = 1L;

        @Getter
        @Setter
        private String points;
    }
}
