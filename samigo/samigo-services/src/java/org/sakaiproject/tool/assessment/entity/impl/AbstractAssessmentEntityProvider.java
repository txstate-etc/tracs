package org.sakaiproject.tool.assessment.entity.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.tool.assessment.facade.AssessmentFacadeQueriesAPI;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacadeQueriesAPI;
import org.sakaiproject.tool.assessment.services.assessment.AssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Setter;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RESTful;
import org.sakaiproject.entitybroker.DeveloperHelperService;

import java.lang.SecurityException;

/**
 * Entity Provider impl for samigo unpublished Assessments.
 * 
 * References to /direct/sam_unpub/
 *
 * @author yuanhua@txstate.edu
 *
 */

public abstract class AbstractAssessmentEntityProvider implements CoreEntityProvider, AutoRegisterEntityProvider, RESTful, Outputable{

	private static final String CAN_PUBLISH = "assessment.publishAssessment.any";

	@Setter
	protected SiteService siteService;

	@Setter
	protected SecurityService securityService;

	@Setter
	protected AssessmentFacadeQueriesAPI assessmentFacadeQueries;
	
	@Setter
	protected PublishedAssessmentFacadeQueriesAPI publishedAssessmentFacadeQueries;

	@Setter
	private DeveloperHelperService developerHelperService;
	
	private static final Logger LOG = LoggerFactory.getLogger(AbstractAssessmentEntityProvider.class);

	//check user permission
	protected void validateUser(String siteId) {
		if (!developerHelperService.isUserAdmin(developerHelperService.getCurrentUserReference()) && !securityService.unlock(CAN_PUBLISH, "/site/" + siteId))
			throw new SecurityException("Current user doesn't have permission to access this resource.");
	}

	public List<String> findEntityRefs(String[] prefixes, String[] name, String[] searchValue, boolean exactMatch) {

		return null;
	}

	public List getEntities(EntityReference ref, Search search) {
		return new ArrayList();
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.XML, Formats.JSON };
	}

	public String[] getHandledInputFormats() {
		return new String[] { Formats.XML, Formats.JSON, Formats.HTML };
	}

	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {
		return null;
	}

	public void updateEntity(EntityReference ref, Object entity, Map<String, Object> params) {

	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {

	}

	@Override
	public Object getSampleEntity() {
		return null;
	}

	@Override
	public Object getEntity(EntityReference ref) {
		return null;
	}

	@Override
	public boolean entityExists(String id) {
		return false;
	}
}
