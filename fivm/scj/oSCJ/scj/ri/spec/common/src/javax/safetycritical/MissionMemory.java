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
import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.realtime.LTMemory;
import javax.realtime.RealtimeThread;
import javax.realtime.SizeEstimator;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * Facts:
 * 
 * 1. Only one thread (launch thread) is possible to enter the mission memory
 * 
 * 2. Can have children of MissionMemory and PrivateMemory
 * 
 * 3. Can be allocated in MissionMemory (sub-mission) or ImmortalMemory (top
 * mission)
 * 
 * 4. All schedulable objects will be allocated in MissionMemory
 */
@SCJAllowed(LEVEL_0)
public final class MissionMemory extends ManagedMemory {


	@SCJAllowed(LEVEL_1)
	public MissionMemory(SizeEstimator size) {
		super(size);
	}

	MissionMemory(long sizeInByte) {
		super(sizeInByte);
		//System.out.println("Creating a Mission memory. long:" + sizeInByte + "\n" + "\t parent is:" + RealtimeThread.getCurrentMemoryArea());
	}

	@SCJAllowed
	public final void enter(Runnable logic) {
		super.enter(logic);
	}

	public void resize(long sizeInByte) {
		setSize(sizeInByte);
	}

	@SCJAllowed
	public MissionManager getManager() {
		//System.out.println("[Mission memory ] get memory manager!!!");
		
		// TODO: check type
		return (MissionManager) getPortal();
	}

	@SCJAllowed(INFRASTRUCTURE)
	void setManager(MissionManager manager) {
		//System.out.println("[Mission memory ] set memory manager!!!");
		
		setPortal(manager);
	}
	
}
