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
package javax.safetycritical;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

/**
 * 
 * This class permits the automatic periodic execution of code. It is
 * automatically bound to an infrastructure periodic timer in the constructor.
 * This class is abstract, non- abstract sub-classes must implement the methods
 * handleAsyncEvent and cleanup. Note that the values in parameters classes
 * passed to the constructors are those that will be used by the infrastructure.
 * Changing these values after construction will have no impact on the created
 * event handler.
 * 
 * @author plsek
 * 
 */
@SCJAllowed(LEVEL_0)
public abstract class PeriodicEventHandler extends ManagedEventHandler {

	// private Timer _timer;

	@SCJAllowed(LEVEL_0)
	@SCJRestricted(INITIALIZATION)
	public PeriodicEventHandler(PriorityParameters priority,
								PeriodicParameters period, 
								StorageParameters storage) {
		this(priority, period, storage, null);
	}
	
	@SCJAllowed(LEVEL_0)
	@SCJRestricted(INITIALIZATION)
	public PeriodicEventHandler(PriorityParameters priority,
								PeriodicParameters period, 
								StorageParameters storage, 
								String name) {
		super(priority, period, storage, name);
	}
	
	// TODO
	//public final void register()  {
	//	
	//}

	/**
	 * @see javax.safetycritical.ManagedSchedulable#register() Registers this
	 *      event handler with the current mission.
	 */
	@SCJAllowed
	@SCJRestricted(INITIALIZATION)
	public final void register() {
		// TODO:
	}
}
