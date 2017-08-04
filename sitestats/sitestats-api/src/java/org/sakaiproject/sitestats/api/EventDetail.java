/**********************************************************************************
 *
 Added by Yuanhua Qu at Texas State University
 * for bugid:3480 on 11/15/2010
 *
 **********************************************************************************/
package org.sakaiproject.sitestats.api;

/**
 * Represents a record from the SST_EVENT_DETAIL table.
 *
 */
public interface EventDetail extends Stat {

	public final static String ITEM_TYPE_SAMIGO_PUBASSES = "publishedAssessmentId";
	public final static String ITEM_TYPE_SAMIGO_ASSES    = "assessmentId";

	/** Get the the event Id (eg. 'content.read') this record refers to. */
	public String getEventId();
	/** Set the the event Id (eg. 'content.read') this record refers to. */
	public void setEventId(String eventId);

	/** Get the the item Id (eg. '5665') this item type refers to. */
	public String getItemId();
	/** Set the the item Id (eg. '5665') this item type refers to. */
	public void setItemId(String itemId);

	/** Get the item Type (eg. 'publishedAssessmentId' )that associated with item Id */
	public String getItemType();
	/** Set the item Type (eg. 'publishedAssessmentId' )that associated with item Id */
	public void setItemType(String itemType);

	/** Get the the tool Id (eg. 'sakai.chat') this record refers to. */
	public String getToolId();
	/** Set the the tool Id (eg. 'sakai.chat') this record refers to. */
	public void setToolId(String toolId);

}
