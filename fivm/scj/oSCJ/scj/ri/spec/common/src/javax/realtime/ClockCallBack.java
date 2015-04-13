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

import javax.safetycritical.annotate.SCJRestricted;

import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;


/**
 * The ClockEvent interface may be used by subclasses of Clock to indicate to
 * the clock infrastructure that the clock has either reached a designated time,
 * or has experienced a discontinuity.
 *
 * Invocations of the methods in ClockCallBack are serialized.
 * The callback is de-registered before a method in it is invoked,
 * and the Clock blocks any attempt by another thread to register
 * another callback while control is in a callback. 
 */
@SCJAllowed(LEVEL_1)
public interface ClockCallBack {
    
    
    /**
     * Clock has reached the designated time.
     *
     * This clock event is de-registered before this method is invoked. 
     * @param clock the clock that has reached a designated time.
     */
	@SCJAllowed(LEVEL_1)
	@SCJRestricted(maySelfSuspend = false)
	void atTime(Clock clock);
	

	 /**
	   * clock experienced a time discontinuity. (It changed its time
	   * value other than by ticking.) and clock has de-registered this
	   * clock event. 
	   *  
	   * @param clock the clock that has experienced a discontinuity.
	   * @param updatedTime the signed length of the time discontinuity.
	   */
	@SCJAllowed(LEVEL_1)
	@SCJRestricted(maySelfSuspend = false)
	void discontinuity(Clock clock, AbsoluteTime updatedTime);
}
