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

import javax.realtime.AperiodicParameters;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed(LEVEL_1)
public abstract class AperiodicEventHandler extends ManagedEventHandler {

	@SCJAllowed(LEVEL_1)
	@SCJRestricted(INITIALIZATION)
	public AperiodicEventHandler(PriorityParameters priority,
			AperiodicParameters release,
			StorageParameters storage) {
		this(priority, release, storage, null);
	}

	@SCJAllowed(LEVEL_1)
	@SCJRestricted(INITIALIZATION)
	public AperiodicEventHandler(PriorityParameters priority,
			AperiodicParameters release,
			StorageParameters storage, String name) {
		super(priority, release, storage, name);
	}
	
	/**
	   * @see javax.safetycritical.ManagedSchedulable#register()
	   * Registers this event handler with the current mission and attaches
	   * this handler to all the aperiodic events passed during construction.
	   * Registers all the aperiodic events passed during constructions.
	   */
	  //@Override
	  @SCJAllowed
	  @SCJRestricted(INITIALIZATION)
	  public final void register() {
		  //TODO:
	  }
}
