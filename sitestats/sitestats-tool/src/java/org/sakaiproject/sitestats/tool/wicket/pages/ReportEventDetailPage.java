package org.sakaiproject.sitestats.tool.wicket.pages;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.sitestats.api.EventStat;
import org.sakaiproject.sitestats.api.EventDetail;
import org.sakaiproject.sitestats.api.PrefsData;
import org.sakaiproject.sitestats.api.Stat;
import org.sakaiproject.sitestats.api.StatsManager;
import org.sakaiproject.sitestats.api.event.ToolInfo;
import org.sakaiproject.sitestats.api.report.Report;
import org.sakaiproject.sitestats.api.report.ReportDef;
import org.sakaiproject.sitestats.api.report.ReportParams;
import org.sakaiproject.sitestats.tool.facade.SakaiFacade;
import org.sakaiproject.sitestats.tool.wicket.components.ImageWithLink;
import org.sakaiproject.sitestats.tool.wicket.components.LastJobRun;
import org.sakaiproject.sitestats.tool.wicket.components.Menus;
import org.sakaiproject.sitestats.tool.wicket.components.SakaiDataTable;
import org.sakaiproject.sitestats.tool.wicket.models.ReportDefModel;
import org.sakaiproject.sitestats.tool.wicket.providers.ReportsDataProvider;
import org.sakaiproject.sitestats.tool.wicket.SiteStatsApplication;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * @author Yuanhua Qu at Texas State University
 * Description:
 * 		This page is added to show each interested event detail
 * 		for site maintainer to view. Use case started with
 * 		observing students' test taking events.
 * Date: Nov.11,2010
 */
public class ReportEventDetailPage extends BasePage {
	private static final long			serialVersionUID	= 1L;
	private static Logger					LOG					= LoggerFactory.getLogger(ReportEventDetailPage.class);

	/** Inject Sakai facade */
	@SpringBean
	private transient SakaiFacade		facade;

	private String						realSiteId;
	private String						siteId;
	private boolean						inPrintVersion;

	private ReportDefModel				reportDefModel;
	private Report						report;
	private PrefsData					prefsdata;
	private WebPage						returnPage;

	public ReportEventDetailPage(final ReportDefModel reportDef) {
		this(reportDef, null, null);
	}

	public ReportEventDetailPage(final ReportDefModel reportDef, final PageParameters pageParameters) {
		this(reportDef, pageParameters, null);
	}

	public ReportEventDetailPage(final ReportDefModel reportDef, final PageParameters pageParameters, final WebPage returnPage) {
		this.reportDefModel = reportDef;
		realSiteId = getFacade().getToolManager().getCurrentPlacement().getContext();
		if(pageParameters != null) {
			siteId = pageParameters.get("siteId").toString();
			inPrintVersion = pageParameters.get("printVersion").toBoolean();
		}
		if(siteId == null){
			siteId = realSiteId;
		}
		if(returnPage == null) {
			this.returnPage = new ReportDataPage(reportDef,pageParameters);
		}else{
			this.returnPage = returnPage;
		}
		boolean allowed = getFacade().getStatsAuthz().isUserAbleToViewSiteStats(siteId);
		if(allowed) {
			if(reportDef != null && getReportDef() != null && getReportDef().getReportParams() != null) {
				renderBody();
			}else{
				setResponsePage(ReportsPage.class);
			}
		}else{
			setResponsePage(NotAuthorizedPage.class);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forUrl(JQUERYSCRIPT));
	}

	@SuppressWarnings("serial")
	private void renderBody() {
		// reportAction
		if(getReportDef().getTitle() != null && getReportDef().getTitle().trim().length() != 0) {
			String titleStr = null;
			if(getReportDef().isTitleLocalized()) {
				titleStr = (String) new ResourceModel("reportres_title_detailed").getObject();
				titleStr = titleStr.replaceAll("\\$\\{title\\}", (String) new ResourceModel(getReportDef().getTitleBundleKey()).getObject());
			}else{
				titleStr = new StringResourceModel("reportres_title_detailed", this, reportDefModel).getString();
			}
			add(new Label("reportAction", titleStr));
		}else{
			add(new Label("reportAction", new ResourceModel("reportres_title")));
		}

		// model
		setDefaultModel(new CompoundPropertyModel(this));

		// form
		Form form = new Form("form");
		add(form);

		// menu
		add(new Menus("menu", siteId).setVisible(!inPrintVersion));

		// last job run
		add(new LastJobRun("lastJobRun", siteId));

		// print link/info
		WebMarkupContainer toPrintVersion = new WebMarkupContainer("toPrintVersion");
		toPrintVersion.setVisible(!inPrintVersion);
		toPrintVersion.add(new Link("printLink") {
			@Override
			public void onClick() {
				PageParameters params = new PageParameters();
				params.set("printVersion", "true");
				params.set("siteId", siteId);
				setResponsePage(new ReportEventDetailPage(reportDefModel, params));
			}
		});
		add(toPrintVersion);
		add(new WebMarkupContainer("inPrintVersion").setVisible(inPrintVersion));

		ReportDef reportDef = getReportDef();
		reportDef.getReportParams().setShowEventDetailPage(true);
		reportDef.getReportParams().setHowSortBy(StatsManager.T_EVENT_DETAIL);

		// Report data
		final ReportsDataProvider dataProvider = new ReportsDataProvider(getPrefsdata(), reportDef);
		report = dataProvider.getReport();

		// Report: table
		SakaiDataTable reportTable = new SakaiDataTable(
				"detailTable",
				getDetailTableColumns(getFacade(), getReportParams(), true),
				dataProvider,
				!inPrintVersion);
		if(inPrintVersion) {
			reportTable.setItemsPerPage(Integer.MAX_VALUE);
		}
		form.add(reportTable);

		// Report: header (report info)
		WebMarkupContainer trDescription = new WebMarkupContainer("trDescription");
		trDescription.setVisible(getReportDescription() != null);
		trDescription.add(new Label("reportDescription"));
		add(trDescription);

		add(new Label("reportSite"));

		add(new Label("reportActivityBasedOn"));

		WebMarkupContainer trResourceAction = new WebMarkupContainer("trResourceAction");
		trResourceAction.setVisible(getReportResourceAction() != null);
		trResourceAction.add(new Label("reportResourceActionTitle"));
		trResourceAction.add(new Label("reportResourceAction"));
		add(trResourceAction);

		WebMarkupContainer trActivitySelection = new WebMarkupContainer("trActivitySelection");
		trActivitySelection.setVisible(getReportActivitySelection() != null);
		trActivitySelection.add(new Label("reportActivitySelectionTitle"));
		trActivitySelection.add(new Label("reportActivitySelection"));
		add(trActivitySelection);

		add(new Label("reportTimePeriod"));

		add(new Label("reportUserSelectionType"));

		WebMarkupContainer trReportUserSelection = new WebMarkupContainer("trReportUserSelection");
		trReportUserSelection.setVisible(getReportUserSelectionTitle() != null);
		trReportUserSelection.add(new Label("reportUserSelectionTitle"));
		trReportUserSelection.add(new Label("reportUserSelection"));
		add(trReportUserSelection);

		add(new Label("report.localizedReportGenerationDate"));

		// buttons
		form.add(new Button("back") {
			@Override
			public void onSubmit() {
				setResponsePage(returnPage);
				super.onSubmit();
			}
		}.setVisible(!inPrintVersion));
	}

	@SuppressWarnings("serial")
	public static List<IColumn> getDetailTableColumns(
			final SakaiFacade facade, final ReportParams reportParams, final boolean columnsSortable
		) {
		List<IColumn> columns = new ArrayList<IColumn>();

		final Map<String,ToolInfo> eventIdToolMap = facade.getEventRegistryService().getEventIdToolMap();

		// site
		if(facade.getReportManager().isReportColumnAvailable(reportParams, StatsManager.T_SITE)) {
			columns.add(new PropertyColumn(new ResourceModel("th_site"), columnsSortable ? ReportsDataProvider.COL_SITE : null, ReportsDataProvider.COL_SITE) {
				@Override
				public void populateItem(Item item, String componentId, IModel model) {
					final String site = ((Stat) model.getObject()).getSiteId();
					String lbl = "", href = "";
					Site s = null;
					try{
						s = facade.getSiteService().getSite(site);
						lbl = s.getTitle();
						href = s.getUrl();
					}catch(IdUnusedException e){
						lbl = (String) new ResourceModel("site_unknown").getObject();
						href = null;
					}
					item.add(new ImageWithLink(componentId, null, href, lbl, "_parent"));
				}
			});
		}
		// user
			columns.add(new PropertyColumn(new ResourceModel("th_id"), columnsSortable ? ReportsDataProvider.COL_USERID : null, ReportsDataProvider.COL_USERID) {
				@Override
				public void populateItem(Item item, String componentId, IModel model) {
					final String userId = ((Stat) model.getObject()).getUserId();
					String name = null;
					if (userId != null) {
						if(("-").equals(userId) || ("?").equals(userId)) {
							name = "-";
						}else{
							try{
								name = facade.getUserDirectoryService().getUser(userId).getDisplayId();
							}catch(UserNotDefinedException e1){
								name = userId;
							}
						}
					}else{
						name = (String) new ResourceModel("user_unknown").getObject();
					}
					item.add(new Label(componentId, name));
				}
			});
			columns.add(new PropertyColumn(new ResourceModel("th_user"), columnsSortable ? ReportsDataProvider.COL_USERNAME : null, ReportsDataProvider.COL_USERNAME) {
				@Override
				public void populateItem(Item item, String componentId, IModel model) {
					final String userId = ((Stat) model.getObject()).getUserId();
					String name = null;
					if (userId != null) {
						if(("-").equals(userId)) {
							name = (String) new ResourceModel("user_anonymous").getObject();
						}else if(("?").equals(userId)) {
							name = (String) new ResourceModel("user_anonymous_access").getObject();
						}else{
							try{
								name = facade.getUserDirectoryService().getUser(userId).getDisplayName();
							}catch(UserNotDefinedException e1){
								name = (String) new ResourceModel("user_unknown").getObject();
							}
						}
					}else{
						name = (String) new ResourceModel("user_unknown").getObject();
					}
					item.add(new Label(componentId, name));
				}
			});
		// tool
		if(facade.getReportManager().isReportColumnAvailable(reportParams, StatsManager.T_TOOL)) {
			columns.add(new PropertyColumn(new ResourceModel("th_tool"), columnsSortable ? ReportsDataProvider.COL_TOOL : null, ReportsDataProvider.COL_TOOL) {
				@Override
				public void populateItem(Item item, String componentId, IModel model) {
					final String toolId = ((EventDetail) model.getObject()).getToolId();
					String toolName = "";
					if(!"".equals(toolId)){
						toolName = facade.getEventRegistryService().getToolName(toolId);
					}
					Label toolLabel = new Label(componentId, toolName);
					String toolIconClass = "toolIcon";
					String toolIconPath = "url(" + facade.getEventRegistryService().getToolIcon(toolId) + ")";
					toolLabel.add(new AttributeModifier("class", new Model(toolIconClass)));
					toolLabel.add(new AttributeModifier("style", new Model("background-image: "+toolIconPath)));
					toolLabel.add(new AttributeModifier("title", new Model(toolName)));
					item.add(toolLabel);
				}
			});
		}
		// event
			columns.add(new PropertyColumn(new ResourceModel("th_event"), columnsSortable ? ReportsDataProvider.COL_EVENT_DETAIL : null, ReportsDataProvider.COL_EVENT_DETAIL) {
				@Override
				public void populateItem(Item item, String componentId, IModel model) {
					final String eventId = ((EventDetail) model.getObject()).getEventId();
					String eventName = "";
					if(!"".equals(eventId)){
						eventName = facade.getEventRegistryService().getEventName(eventId);
					}
					Label eventLabel = new Label(componentId, eventName);
					ToolInfo toolInfo = eventIdToolMap.get(eventId);
					if(toolInfo != null) {
						String toolId = toolInfo.getToolId();
						String toolName = facade.getEventRegistryService().getToolName(toolId);
						String toolIconClass = "toolIcon";
						String toolIconPath = "url(" + facade.getEventRegistryService().getToolIcon(toolId) + ")";
						eventLabel.add(new AttributeModifier("class", new Model(toolIconClass)));
						eventLabel.add(new AttributeModifier("style", new Model("background-image: "+toolIconPath)));
						eventLabel.add(new AttributeModifier("title", new Model(toolName)));
					}
					item.add(eventLabel);
				}
			});

			columns.add(new PropertyColumn(new ResourceModel("th_date"), columnsSortable ? ReportsDataProvider.COL_DATE : null, ReportsDataProvider.COL_DATE) {
				@Override
				public void populateItem(Item item, String componentId, IModel model) {
					final Date date = ((Stat) model.getObject()).getDate();
					SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d yyyy HH:mm:ss");
					item.add(new Label(componentId, sdf.format(date)));
				}
			});

		//show event related item all the time
		columns.add(new PropertyColumn(new ResourceModel("th_item"), columnsSortable ? ReportsDataProvider.COL_ITEM : null, ReportsDataProvider.COL_ITEM){
			public void populateItem(Item item, String componentId, IModel model) {
				final String itemId = ((EventDetail) model.getObject()).getItemId();
				String itemType = ((EventDetail) model.getObject()).getItemType();
				String itemName ="-";
				try {
					if (itemType.equalsIgnoreCase(EventDetail.ITEM_TYPE_SAMIGO_PUBASSES)) {
						itemName = facade.getPublishedAssessmentService().getPublishedAssessment(itemId).getTitle();
					}
					else if (itemType.equalsIgnoreCase(EventDetail.ITEM_TYPE_SAMIGO_ASSES)) {
						itemName = facade.getAssessmentService().getAssessment(itemId).getTitle();
					}
				}
				catch(Exception e){
					LOG.info("Error for getting title from getPublishedAssessment(itemId) with itemId = " + itemId);
				}
				Label itemLabel = new Label(componentId, itemName);
				item.add(itemLabel);
			}}
		);

		return columns;
	}

	private SakaiFacade getFacade() {
		if(facade == null) {
			facade = ((SiteStatsApplication) Application.get()).getFacade();
		}
		return facade;
	}

	private PrefsData getPrefsdata() {
		if(prefsdata == null) {
			prefsdata = getFacade().getStatsManager().getPreferences(siteId, true);
		}
		return prefsdata;
	}

	public void setReportDef(ReportDef reportDef) {
		this.reportDefModel.setObject(reportDef);
	}

	public ReportDef getReportDef() {
		return (ReportDef) this.reportDefModel.getObject();
	}

	public void setReportParams(ReportParams reportParams) {
		getReportDef().setReportParams(reportParams);
	}

	public ReportParams getReportParams() {
		return getReportDef().getReportParams();
	}

	// ######################################################################################
	// Report results: SUMMARY
	// ######################################################################################
	public String getReportDescription() {
		return getFacade().getReportManager().getReportFormattedParams().getReportDescription(report);
	}

	public String getReportSite() {
		return getFacade().getReportManager().getReportFormattedParams().getReportSite(report);
	}

	public String getReportGenerationDate() {
		return getFacade().getReportManager().getReportFormattedParams().getReportGenerationDate(report);
	}

	public String getReportActivityBasedOn() {
		return getFacade().getReportManager().getReportFormattedParams().getReportActivityBasedOn(report);
	}

	public String getReportActivitySelectionTitle() {
		return getFacade().getReportManager().getReportFormattedParams().getReportActivitySelectionTitle(report);
	}

	public String getReportActivitySelection() {
		return getFacade().getReportManager().getReportFormattedParams().getReportActivitySelection(report);
	}

	public String getReportResourceActionTitle() {
		return getFacade().getReportManager().getReportFormattedParams().getReportResourceActionTitle(report);
	}

	public String getReportResourceAction() {
		return getFacade().getReportManager().getReportFormattedParams().getReportResourceAction(report);
	}

	public String getReportTimePeriod() {
		return getFacade().getReportManager().getReportFormattedParams().getReportTimePeriod(report);
	}

	public String getReportUserSelectionType() {
		return getFacade().getReportManager().getReportFormattedParams().getReportUserSelectionType(report);
	}

	public String getReportUserSelectionTitle() {
		return getFacade().getReportManager().getReportFormattedParams().getReportUserSelectionTitle(report);
	}

	public String getReportUserSelection() {
		return getFacade().getReportManager().getReportFormattedParams().getReportUserSelection(report);
	}

}

