package org.sakaiproject.tool.assessment.entity.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.tool.assessment.entity.impl.data.PublishedAssessmentSimpleData;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacade;
import org.sakaiproject.tool.assessment.services.assessment.PublishedAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.exception.EntityNotFoundException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;

/**
 * Entity Provider impl for samigo PublishedAssessments
 *
 * References to /direct/sam_pub/
 *
 * @author yuanhua@txstate.edu
 */
public class PublishedAssessmentEntityProviderImpl extends AbstractAssessmentEntityProvider {

	private static final String CAN_TAKE = "assessment.takeAssessment";
	private static final String CAN_PUBLISH = "assessment.publishAssessment.any";
	public final static String ENTITY_PREFIX = "sam_pub";

	private static final Logger LOG = LoggerFactory.getLogger(PublishedAssessmentEntityProviderImpl.class);

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	@Override
	public boolean entityExists(String id) {
		boolean rv = false;

		try {
			PublishedAssessmentService service = new PublishedAssessmentService();

			PublishedAssessmentFacade pub = service.getPublishedAssessment(id);
			if (pub != null) {
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
	 * @see org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable#
	 * getEntity(org.sakaiproject.entitybroker.EntityReference)
	 */
	@Override
	public Object getEntity(EntityReference ref) {
		if (ref == null || ref.getId() == null) {
			throw new IllegalArgumentException("ref and id must be set for assessment");
		}

		validateUser();

		PublishedAssessmentFacade pub = null;
		try {
			PublishedAssessmentService service = new PublishedAssessmentService();

			pub = service.getPublishedAssessment(ref.getId());
			if (pub != null) {
				return new PublishedAssessmentSimpleData(pub);
			}
		} catch (Exception e) {
			LOG.error("Assessment " + ref.getId() + " doesn't exist");
		}
		return pub;
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
		Vector results = new Vector();
		List<PublishedAssessmentFacade> publishedActiveAssessments = new ArrayList<PublishedAssessmentFacade>();
		List<PublishedAssessmentFacade> publishedInactiveAssessments = new ArrayList<PublishedAssessmentFacade>();
		String orderBy = "title";
		boolean canPublish = false;
		Date currentDate = new Date();

		// Check what the user can do
		if (securityService.unlock(CAN_PUBLISH, "/site/" + siteId)) {

			publishedActiveAssessments.addAll(publishedAssessmentFacadeQueries
					.getBasicInfoOfAllActivePublishedAssessments(orderBy, siteId, true));
			canPublish = true;
		} else if (securityService.unlock(CAN_TAKE, "/site/" + siteId)) {
			publishedActiveAssessments = publishedAssessmentFacadeQueries
					.getBasicInfoOfAllActivePublishedAssessments(orderBy, siteId, true);
		}
		Iterator publishedActiveAssessmentIterator = publishedActiveAssessments.iterator();
		while (publishedActiveAssessmentIterator.hasNext()) {
			PublishedAssessmentFacade pub = (PublishedAssessmentFacade) publishedActiveAssessmentIterator.next();
			if (canPublish) {
				PublishedAssessmentSimpleData pubData = new PublishedAssessmentSimpleData(pub);
				pubData.setPublishedStatus(PublishedAssessmentSimpleData.ACTIVE);
				results.add(pubData);
			}
		}

		// get Inactive ones
		// Check what the user can do
		if (securityService.unlock(CAN_PUBLISH, "/site/" + siteId)) {

			publishedInactiveAssessments.addAll(publishedAssessmentFacadeQueries
					.getBasicInfoOfAllInActivePublishedAssessments(orderBy, siteId, true));
			canPublish = true;
		} else if (securityService.unlock(CAN_TAKE, "/site/" + siteId)) {
			publishedInactiveAssessments = publishedAssessmentFacadeQueries
					.getBasicInfoOfAllInActivePublishedAssessments(orderBy, siteId, true);
		}
		Iterator publishedInactiveAssessmentIterator = publishedInactiveAssessments.iterator();
		while (publishedInactiveAssessmentIterator.hasNext()) {
			PublishedAssessmentFacade pub = (PublishedAssessmentFacade) publishedInactiveAssessmentIterator.next();
			if (canPublish) {
				PublishedAssessmentSimpleData pubData = new PublishedAssessmentSimpleData(pub);
				pubData.setPublishedStatus(PublishedAssessmentSimpleData.INACTIVE);
				results.add(pubData);
			}
		}

		return results;
	}

	@Override
	public Object getSampleEntity() {
		return new PublishedAssessmentSimpleData();
	}

}
