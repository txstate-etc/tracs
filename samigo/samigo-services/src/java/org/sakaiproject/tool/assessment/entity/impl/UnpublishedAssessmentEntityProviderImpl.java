package org.sakaiproject.tool.assessment.entity.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.tool.assessment.data.dao.assessment.AssessmentData;
import org.sakaiproject.tool.assessment.entity.impl.data.UnpublishedAssessmentSimpleData;
import org.sakaiproject.tool.assessment.facade.AssessmentFacade;
import org.sakaiproject.tool.assessment.services.assessment.AssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.exception.EntityNotFoundException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;

import java.lang.IllegalArgumentException;

/**
 * Entity Provider impl for samigo unpublished Assessments.
 * 
 * References to /direct/sam_unpub/
 *
 * @author yuanhua@txstate.edu
 *
 */

public class UnpublishedAssessmentEntityProviderImpl extends AbstractAssessmentEntityProvider{

	private static final String CAN_TAKE = "assessment.takeAssessment";
	private static final String CAN_PUBLISH = "assessment.publishAssessment.any";
	public final static String ENTITY_PREFIX = "sam_unpub";

	private static final Logger LOG = LoggerFactory.getLogger(UnpublishedAssessmentEntityProviderImpl.class);

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	@Override
	public boolean entityExists(String id) {
		boolean rv = false;
		try {
			AssessmentService service = new AssessmentService();

			AssessmentFacade unpub = service.getAssessment(id);
			if (unpub != null) {
				rv = true;
			}
		} catch (Exception e) {
			rv = false;
		}
		return rv;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable#
	 * getEntity(org.sakaiproject.entitybroker.EntityReference)
	 */
	@Override
	public Object getEntity(EntityReference ref) {
		if (ref == null || ref.getId() == null) {
			throw new IllegalArgumentException("ref and id must be set for assessment");
		}

		validateUser();

		AssessmentFacade unpub = null;
		try {
			AssessmentService service = new AssessmentService();

			unpub = service.getAssessment(ref.getId());
			if (unpub != null) {
				return new UnpublishedAssessmentSimpleData(unpub);
			}
		} catch (Exception e) {
			LOG.error("Assessment " + ref.getId() + " doesn't exist");
		}
		return unpub;
	}

	/**
	 * site/siteId
	 */
	@EntityCustomAction(action = "site", viewKey = EntityView.VIEW_LIST)
	public List<?> getAssessmentsForSite(EntityView view, Map<String, Object> params) {
		String siteId = view.getPathSegment(2);

		// check user can access this site
		Site site;
		try {
			site = siteService.getSiteVisit(siteId);
		} catch (IdUnusedException e) {
			throw new EntityNotFoundException("Invalid siteId: " + siteId, siteId);
		} catch (PermissionException e) {
			throw new EntityNotFoundException("No access to site: " + siteId, siteId);
		}

		Vector<UnpublishedAssessmentSimpleData> results = new Vector<UnpublishedAssessmentSimpleData>();
		List<AssessmentFacade> assessments = new ArrayList<AssessmentFacade>();
		boolean canPublish = false;

		// Check what the user can do
		if (securityService.unlock(CAN_PUBLISH, "/site/" + siteId)) {
			assessments.addAll(assessmentFacadeQueries.getAllActiveAssessmentsByAgent(siteId));
			canPublish = true;
		} else if (securityService.unlock(CAN_TAKE, "/site/" + siteId)) {
			assessments = assessmentFacadeQueries.getAllActiveAssessmentsByAgent(siteId);
		}
		Iterator assessmentIterator = assessments.iterator();
		while (assessmentIterator.hasNext()) {
			AssessmentFacade unpub = new AssessmentFacade((AssessmentData) assessmentIterator.next(), false);
			if (canPublish) {
				results.add(new UnpublishedAssessmentSimpleData(unpub));
			}
		}
		return results;

	}

	@Override
	public Object getSampleEntity() {
		return new UnpublishedAssessmentSimpleData();

	}

}
