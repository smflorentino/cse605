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

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.BoundAsyncEventHandler;
import javax.realtime.PriorityParameters;
import javax.realtime.ReleaseParameters;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import edu.purdue.scj.VMSupport;

/**
 * 
 * In SCJ, all handlers must be known by the mission manager, hence applications
 * use classes that are based on the ManagedEventHandler class hierarchy. This
 * class hierarchy allows a mission to keep track of all the handlers that are
 * created during the initialization phase. 15 April 2010 Version 0.74 47
 * Confidentiality: Public Distribution Safety Critical Specification for Java
 * Note that the values in parameters classes passed to the constructors are
 * those that will be used by the infrastructure. Changing these values after
 * construction will have no impact on the created event handler.
 * 
 * @author plsek
 * 
 */
@SCJAllowed
public abstract class ManagedEventHandler extends BoundAsyncEventHandler
		implements ManagedSchedulable {

	private String _name;

	private ManagedEventHandler _next = null;
	
	@SCJAllowed(INFRASTRUCTURE)
	@SCJRestricted(INITIALIZATION)
	public ManagedEventHandler(PriorityParameters priority,
			ReleaseParameters release, StorageParameters storage, String name) {
		super(priority, release, null, new PrivateMemory(storage._totalBackingStore), null, true,
				null);
		_name = name;
		MissionManager.getCurrentMissionManager().addEventHandler(this);
		((ManagedMemory)getInitArea()).setOwner(this);
	}
	
	/**
	 * Application developers override this method with code to be executed
	 * whenever the event(s) to which this event handler is bound is fired.
	 */
	@Override
	@SCJAllowed(SUPPORT)
	public abstract void handleAsyncEvent();

	@SCJAllowed(SUPPORT)
	@SCJRestricted(CLEANUP)
	public void cleanUp() {
		// FIXME: is this ever called?????
	}

	@SCJAllowed
	public String getName() {
		return _name;
	}

	PrivateMemory getInitArea() {
		return (PrivateMemory) getInitMemoryArea();
	}

	void join() {
	}

	@SCJAllowed(INFRASTRUCTURE)
	public ManagedEventHandler getNext() {
		return _next;
	}

	@SCJAllowed(INFRASTRUCTURE)
	public void setNext(ManagedEventHandler next) {
		_next = next;
	}
}
