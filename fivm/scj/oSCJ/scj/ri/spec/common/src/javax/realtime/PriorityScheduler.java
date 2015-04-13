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

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

/**
 * Priority-based dispatching is supported at Levels 1 and 2. The only access to
 * the priority scheduler is for obtaining the maximum software priority.
 * 
 * @author plsek
 * 
 */
@SCJAllowed(LEVEL_1)
public class PriorityScheduler extends Scheduler {

	public static final int MAX_PRIORITY = 27;
	public static final int NORM_PRIORITY = 13;
	public static final int MIN_PRIORITY = 0;

	protected static final PriorityScheduler _instance = new PriorityScheduler();

	@SCJAllowed(LEVEL_1)
	public static PriorityScheduler instance() {
		return _instance;
	}

	/**
	 * TBD: do we want to distinguish between interrupt-level priorities and
	 * application-level priorities? Consider, for example, the default value
	 * for priority ceiling objects. This should probably be the maximum
	 * application-level priority. I don't think we want the default priority
	 * ceiling to disable hardware interrupts...
	 */

	@SCJAllowed(LEVEL_1)
	@SCJRestricted()
	public int getMaxPriority() {
		return MAX_PRIORITY;
	}

	@SCJAllowed(LEVEL_1)
	@SCJRestricted()
	public int getNormPriority() {
		return NORM_PRIORITY;
	}

	@SCJAllowed(LEVEL_1)
	@SCJRestricted()
	public int getMinPriority() {
		return MIN_PRIORITY;
	}

	/** package method for checking priority validity */
	boolean isValid(int priority) {
		return priority >= MIN_PRIORITY && priority <= MAX_PRIORITY;
	}

	/**
	 * Map scj sense of priority to VM priority. The caller should make sure
	 * that the input is a valid scj priority.
	 */
	static int convertToVMPriority(int priority) {
		return priority - MIN_PRIORITY + RealtimeJavaDispatcher.VM_MIN_PRIORITY;
	}

	/**
	 * TODO: what should the default parameter be according to SCJ spec?
	 * 
	 * This is used at Level 1.
	 * 
	 * @return
	 */
	SchedulingParameters getDefaultSchedulingParameters() {
		return new PriorityParameters(NORM_PRIORITY);
	}
}
