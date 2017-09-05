package org.sakaiproject.gradebookng.tool.panels.importExport;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.gradebookng.tool.pages.ImportExportPage;
import org.sakaiproject.gradebookng.tool.panels.AddOrEditGradeItemPanelContent;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.gradebookng.business.model.ImportedColumn;
import org.apache.wicket.model.Model;
import lombok.Getter;
import lombok.Setter;

public class MapInputColumnsStep extends Panel {

	private static final long serialVersionUID = 1L;

	private final IModel<ColumnMap> model;
	private final ModalWindow window;

	public MapInputColumnsStep(final String id, final IModel<ColumnMap> model, final ModalWindow window) {
		super(id);
		this.model = model;
		this.window = window;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();
		final ColumnMap columnMap = this.model.getObject();
		List<ImportedColumn> importedColumns = columnMap.getColumnList();
		for (int i = 0; i < importedColumns.size(); i++)
		{
			//add text field for each column (uneditable)
			//set the text of each field to be the corresponding column name
			//add dropdown of Column Types next to each text field
		}

		final GbAjaxButton submit = new GbAjaxButton("submit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                //Close form, return to GradeImportUpdateStep, and proceed to GradeItemImportSelectionStep
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                //TODO: Anything here?
            }
        };
	}
}

class ColumnMap implements Serializable {

	private static final long serialVersionUID = 1L;

	public ColumnMap(final List<ImportedColumn> columns, final StringBuilder errorsb, final List<Assignment> gradebookItems) {
		this.columnList = columns;
		this.errorStrings = errorsb;
		this.gradeBookItems = gradeBookItems;
	}

	@Getter
	@Setter
	private List<ImportedColumn> columnList;

	@Getter
	@Setter
	private StringBuilder errorStrings;

	@Getter
	@Setter
	private List<Assignment> gradeBookItems;
}