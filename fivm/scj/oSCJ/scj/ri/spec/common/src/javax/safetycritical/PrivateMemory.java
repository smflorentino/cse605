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

import javax.realtime.LTMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;   

import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

/**
 * Facts:
 * 
 * 1. Only one thread can enter at a time
 * 
 * 2. Can have children of PrivateMemory
 * 
 * 3. Can be allocated in MissionMemory (initial scope) or PrivateMemory
 * 
 * 4. No schedulable objects can reside in PrivateMemory
 */
@SCJAllowed
public class PrivateMemory extends ManagedMemory {

	// /** already occupied by a thread? */
	// volatile boolean _occupied = false;

	/** locate in MissionMemory */
	private MissionManager _manager;

	/**
	 * The scoped memory where current private memory instance is allocated in.
	 * This can only be entered from there.
	 */
	private ManagedMemory _resideInScope;

	@SCJAllowed
	public PrivateMemory(long size) {
		super(size);

		
		MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
		if (mem instanceof ManagedMemory)
			_resideInScope = (ManagedMemory) mem;
		else {
			Utils.panic("Private memory cannot be created in " + mem);
			// FIXME : throw ScopeMemoryException
		}
			
		_manager = _resideInScope.getManager();
	}

	@SCJAllowed
	public MissionManager getManager() {
		return _manager;
	}

	@SCJAllowed(INFRASTRUCTURE)
	public void enter(Runnable logic) {
		if (_resideInScope != RealtimeThread.getCurrentMemoryArea()) {
			String s = "Attempt to enter private memory from the memory area other than where it is created";
			s += "\nPrivate Memory: " + this;
			s += "\n Current Area: " + RealtimeThread.getCurrentMemoryArea();
			s += "\n Expected Current Area: " + _resideInScope;
			Utils.panic(s);
		}
		super.enter(logic);
	}
}
