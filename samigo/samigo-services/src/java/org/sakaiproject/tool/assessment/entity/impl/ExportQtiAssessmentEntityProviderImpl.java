package org.sakaiproject.tool.assessment.entity.impl;

import org.sakaiproject.tool.assessment.entity.impl.data.AssessmentQtiData;
import org.sakaiproject.tool.assessment.entity.impl.data.UnpublishedAssessmentSimpleData;
import org.sakaiproject.tool.assessment.facade.AssessmentFacade;
import org.sakaiproject.tool.assessment.services.assessment.AssessmentService;
import org.sakaiproject.tool.assessment.shared.api.qti.QTIServiceAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Setter;

import org.sakaiproject.entitybroker.EntityReference;
import java.lang.IllegalArgumentException;

/**
 * Entity Provider impl for samigo unpublished Assessments QTI export data.
 * 
 * References to /direct/sam_export_qti/
 *
 * @author yuanhua@txstate.edu
 *
 */
public class ExportQtiAssessmentEntityProviderImpl extends AbstractAssessmentEntityProvider {

	public final static String ENTITY_PREFIX = "sam_export_qti";
	private static final int QTI_VERSION = 1;

	@Setter
	private QTIServiceAPI qtiService;

	private static final Logger LOG = LoggerFactory.getLogger(ExportQtiAssessmentEntityProviderImpl.class);

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
	 * @see org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable#
	 * getEntity(org.sakaiproject.entitybroker.EntityReference)
	 */
	@Override
	public Object getEntity(EntityReference ref) {
		if (ref == null || ref.getId() == null) {
			throw new IllegalArgumentException("ref and id must be set for assessment");
		}

		validateUser();

		String exportXmlString = null;
		try {

			exportXmlString = qtiService.getExportedAssessmentAsString(ref.getId(), QTI_VERSION);
			if (exportXmlString != null) {
				return new AssessmentQtiData(exportXmlString);
			}
		} catch (Exception e) {
			LOG.error("Error trying to export for " + ref.getId());
		}
		return new AssessmentQtiData(exportXmlString);
	}

	@Override
	public Object getSampleEntity() {
		return new AssessmentQtiData();

	}

}
