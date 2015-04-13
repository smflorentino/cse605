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

/**
 * The class is restricted so that it allows the priority to be created and
 * queried but not changed. In SCJ the range of priorities is separated into
 * software priorities and hardware priorities (see Section 4.6.6). Hardware
 * priorities have higher values than software priorities. Schedulable objects
 * can be assigned only software priorities. Ceiling priorities can be either
 * software or hardware priorities.
 * 
 * @author plsek
 * 
 */
@SCJAllowed
public class PriorityParameters extends SchedulingParameters {

	protected int _priority;

	@SCJAllowed
	public PriorityParameters(int priority) {// valid priority is determined by
		// the scheduler of the Schedulable
		// this parameter object is associated with. It will reject this
		// parameter object if this priority is out-of-range
		// _priority = priority;
	}

	@SCJAllowed
	@SCJRestricted()
	public int getPriority() {
		return _priority;
	}

	// not scj allowed
	// TODO: implement this if necessary
	public void setPriority(int priority) throws IllegalArgumentException {
		// int oldPriority = _priority;
		// _priority = priority;
		//
		// int len = schedulables.size();
		// for (int i = 0; i < len; i++) {
		// try {
		// ((Schedulable) schedulables.data[i])
		// .setSchedulingParameters(this);
		// } catch (IllegalThreadStateException itse) {
		// System.err
		// .println("WARNING: Schedulable "
		// + schedulables.data[i]
		// + "was in the wrong state to set the scheduling parameters"
		// + "- it's now inconsistent with those scheduling parameters!");
		// } catch (IllegalArgumentException iae) {
		// // change failed so rollback.
		// // In practice this will fail on the first one but we'll
		// // do the "right thing"
		// _priority = oldPriority;
		// for (int j = i - 1; j >= 0; j--) {
		// try {
		// ((Schedulable) schedulables.data[j])
		// .setSchedulingParameters(this);
		// } catch (IllegalThreadStateException ex) {
		// // if the schedulable is the same as in the main loop
		// // it will be consistent again but in general something
		// // that was set successfully the first time could
		// // fail this time.
		// System.err
		// .println("WARNING: Schedulable "
		// + schedulables.data[i]
		// + "was in the wrong state to set the scheduling parameters"
		// + "- it's now inconsistent with those scheduling parameters!");
		// }
		// }
		// throw iae;
		// }
		// }
	}
}
