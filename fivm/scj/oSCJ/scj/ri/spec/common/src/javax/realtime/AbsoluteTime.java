/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;


@SCJAllowed
public class AbsoluteTime extends HighResolutionTime {

	public AbsoluteTime() {
		this(0, 0);
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime(AbsoluteTime time) {
		this(getMillisNonNull(time), time._nanoseconds);
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime(long millis, int nanos) {
		super(millis, (long) nanos, null);
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime(long millis, int nanos, Clock clock) {
		super(millis, (long) nanos, clock);
	}

	/**
	 * Construct an absolute time from an absolute time in nanoseconds since the
	 * Epoch.
	 */
	AbsoluteTime(long nanos) {
		super(nanos / NANOS_PER_MILLI, (int) (nanos % NANOS_PER_MILLI),
				Clock.rtc);
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime(Clock clock) {
		this(0, 0, clock);
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime add(long millis, int nanos) {
		return add(millis, nanos, null);
	}

	@SCJAllowed
	 @SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime add(RelativeTime time) {
		if (time == null || time._clock != _clock)
			throw new IllegalArgumentException("null arg or different clock");

		return add(time._milliseconds, time._nanoseconds, null);
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime add(RelativeTime time, AbsoluteTime dest) {
		if (time == null || time._clock != _clock)
			throw new IllegalArgumentException("null arg or different clock");

		return add(time._milliseconds, time._nanoseconds, dest);
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime add(long millis, int nanos, AbsoluteTime dest) {
		return (AbsoluteTime) super.add(millis, nanos,
				dest == null ? new AbsoluteTime(0, 0, _clock) : dest);
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public RelativeTime subtract(AbsoluteTime time) {
		if (time == null || time._clock != _clock)
			throw new IllegalArgumentException("null arg or different clock");

		return (RelativeTime) add(-time._milliseconds, -time._nanoseconds,
				new RelativeTime(0, 0, _clock));
	}

	@SCJAllowed
	@SCJRestricted()
	public AbsoluteTime subtract(RelativeTime time) {
		if (time == null || time._clock != this._clock)
			throw new IllegalArgumentException("null arg or different clock");

		return add(-time._milliseconds, -time._nanoseconds, new AbsoluteTime(0,
				0, _clock));
	}

	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public RelativeTime subtract(AbsoluteTime time, RelativeTime dest) {
		if (time == null || time._clock != this._clock)
			throw new IllegalArgumentException("null arg or different clock");

		if (dest == null)
			dest = new RelativeTime(0, 0, _clock);

		return (RelativeTime) add(-time._milliseconds, -time._nanoseconds, dest);
	}
	
	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	public AbsoluteTime subtract(RelativeTime time, AbsoluteTime absoluteTime) {
	    //TODO:
	    return null;
	}

	public RelativeTime relative(Clock clock) {
		if (clock == null)
			clock = Clock.rtc;
		// can't avoid creating the intermediate AbsoluteTime in general
		// We could avoid for the RTC if it is worth the effort
		return clock.getTime().subtract(this);
	}

	public RelativeTime relative(Clock clock, RelativeTime dest) {
		if (clock == null)
			clock = Clock.rtc;

		if (dest == null)
			dest = new RelativeTime();

		return clock.getTime().subtract(this, dest);
	}

	private static long getMillisNonNull(AbsoluteTime time) {
		if (time == null)
			throw new IllegalArgumentException("null parameter");
		return time._milliseconds;
	}
}
