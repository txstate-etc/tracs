/*
* Licensed to The Apereo Foundation under one or more contributor license
* agreements. See the NOTICE file distributed with this work for
* additional information regarding copyright ownership.
*
* The Apereo Foundation licenses this file to you under the Educational 
* Community License, Version 2.0 (the "License"); you may not use this file 
* except in compliance with the License. You may obtain a copy of the 
* License at:
*
* http://opensource.org/licenses/ecl2.txt
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.sakaiproject.calendaring.mocks;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.sakaiproject.calendaring.logic.SakaiProxy;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeBreakdown;
import org.sakaiproject.time.api.TimeRange;
import org.sakaiproject.time.api.TimeService;

/**
 * Mock of SakaiProxy so we can call the main service API
 * 
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
public class MockTimeService implements TimeService {

	public static final String NO_EMAIL_ID = "noEmailPlease";

	@Override
	public Time newTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time newTimeGmt(String value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time newTime(long value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time newTime(GregorianCalendar cal) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time newTimeGmt(int year, int month, int day, int hour, int minute, int second, int millisecond) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time newTimeGmt(TimeBreakdown breakdown) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time newTimeLocal(int year, int month, int day, int hour, int minute, int second, int millisecond) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time newTimeLocal(TimeBreakdown breakdown) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeBreakdown newTimeBreakdown(int year, int month, int day, int hour, int minute, int second,
			int millisecond) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeRange newTimeRange(Time start, Time end, boolean startIncluded, boolean endIncluded) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeRange newTimeRange(String value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeRange newTimeRange(Time startAndEnd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeRange newTimeRange(long start, long duration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeRange newTimeRange(Time start, Time end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeZone getLocalTimeZone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean clearLocalTimeZone(String userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public GregorianCalendar getCalendar(TimeZone zone, int year, int month, int day, int hour, int min, int second,
			int ms) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean different(Time a, Time b) {
		// TODO Auto-generated method stub
		return false;
	}

}
