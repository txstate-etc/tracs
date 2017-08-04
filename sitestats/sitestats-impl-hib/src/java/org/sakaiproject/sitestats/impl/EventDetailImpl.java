/**********************************************************************************
 *

 * Added by Yuanhua Qu at Texas State University
 * for bugid:3480 on 11/15/2010
 *
 **********************************************************************************/
package org.sakaiproject.sitestats.impl;

import java.io.Serializable;
import java.util.Date;

import org.sakaiproject.sitestats.api.EventDetail;


public class EventDetailImpl implements EventDetail, Serializable, Comparable<EventDetail> {
	private static final long	serialVersionUID	= 1L;
	private long	id;
	private String	siteId;
	private String	userId;
	private String	eventId;
	private String	toolId;
	private String	itemType;
	private String  itemId;
	private Date	date;
	private long count;

	/** Minimal constructor. */
	public EventDetailImpl() {
	}

	/** Full constructor. */
	public EventDetailImpl(long id, String siteId, String userId, String eventId, String itemType, String itemId, Date date) {
		setId(id);
		setSiteId(siteId);
		setUserId(userId);
		setEventId(eventId);
		setItemType(itemType);
		setItemId(itemId);
		setDate(date);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setId(long)
	 */
	public void setId(long id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getId()
	 */
	public long getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getUserId()
	 */
	public String getUserId() {
		return userId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setUserId(java.lang.String)
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getSiteId()
	 */
	public String getSiteId() {
		return siteId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setSiteId(java.lang.String)
	 */
	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getEventId()
	 */
	public String getEventId() {
		return eventId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setEventId(java.lang.String)
	 */
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	/* (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getToolId()
	 */
	public String getToolId() {
		return toolId;
	}

	/* (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setToolId(java.lang.String)
	 */
	public void setToolId(String toolId) {
		this.toolId = toolId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getItemType()
	 */
	public String getItemType() {
		return itemType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setItemType()
	 */
	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getItemId()
	 */
	public String getItemId() {
		return itemId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setItemId()
	 */
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getCount()
	 */
	public long getCount() {
		return count;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setCount(long)
	 */
	public void setCount(long count) {
		this.count = count;
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#getDate()
	 */
	public Date getDate() {
		return date;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.sitestats.api.EventDetail#setDate(java.util.Date)
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	public int hashCode() {
		if(siteId == null) return Integer.MIN_VALUE;
		String hashStr = this.getClass().getName() + ":"
				+ id
				+ this.getUserId().hashCode()
				+ this.getSiteId().hashCode()
				+ this.getEventId().hashCode()
				+ this.getItemType().hashCode()
				+ this.getItemId().hashCode()
				+ this.getDate().hashCode();
		return hashStr.hashCode();
	}

	public String toString(){
		return siteId + " : " + userId + " : " + eventId + " : " + itemType + " : " + itemId + " : " + date;
	}

	@Override
	public int compareTo(EventDetail other) {
		int val = siteId.compareTo(other.getSiteId());
		if (val != 0) return val;
		val = userId.compareTo(other.getUserId());
		if (val != 0) return val;
		val = eventId.compareTo(other.getEventId());
		if (val != 0) return val;
		val = date.compareTo(other.getDate());
		if (val != 0) return val;
		val = Long.signum(count - other.getCount());
		if (val != 0) return val;
		val = compare(toolId, other.getToolId());
		if (val != 0) return val;
		val = Long.signum(id - other.getId());
		return val;
	}

	private int compare(String one, String two) {
		if (one == null) {
			if (two == null) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (two == null) {
				return -1;
			} else {
				return one.compareTo(two);
			}
		}
	}


}
