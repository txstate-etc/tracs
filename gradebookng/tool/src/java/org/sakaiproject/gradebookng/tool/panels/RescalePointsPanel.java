package org.sakaiproject.gradebookng.tool.panels;

import org.apache.wicket.markup.html.panel.Panel;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.tool.model.RescaleAnswer;

public class RescalePointsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private final ModalWindow window;

    RescaleAnswer answer;

    public RescalePointsPanel(final String id, final ModalWindow window, RescaleAnswer answer) {
        super(id);
        this.window = window;
        this.answer = answer;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        Form form = new Form("form");

        final GbAjaxButton btnYes = new GbAjaxButton("scaleYes") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                answer.setAnswer("yes");
                window.close(target);
            }

        };
        form.add(btnYes);
        final GbAjaxButton btnNo = new GbAjaxButton("scaleNo") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                answer.setAnswer("no");
                window.close(target);
            }

        };
        form.add(btnNo);

        final GbAjaxButton cancel = new GbAjaxButton("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                answer.setAnswer("cancel");
                window.close(target);
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);
    }

}