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
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.realtime.LTMemory;
import javax.realtime.RealtimeThread;
import javax.realtime.SizeEstimator;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed
public abstract class ManagedMemory extends LTMemory {

	private MissionManager _manager;
	private ManagedSchedulable _owner;
	static ManagedMemory _current;

	private ManagedMemory _child = null;

	/**
	 * The scoped memory where current managed memory instance is allocated in.
	 * This Memory can only be entered from there.
	 */
	// private ManagedMemory _resideInScope;

	public ManagedMemory(long size) {
		super(size);
	}

	public ManagedMemory(SizeEstimator size) {
		super(size);
	}

	/**
	 *
	 * TODO: this is not defined by SCJ
	 *
	 * @return
	 */
	@SCJAllowed
	public MissionManager getManager() {
		return _manager;
	}

	/**
	 * @Returns the current managed memory.
	 *
	 * @Throws IllegaleStateException when called from immortal.
	 */
	@SCJAllowed
	public static ManagedMemory getCurrentManagedMemory()
			throws IllegalStateException {
		// TODO: can we do this??
		return (ManagedMemory) RealtimeThread.getCurrentMemoryArea();
	}

	/**
	 * Restrictions: this method can be called only by the infrastructure
	 */
	@Override
    @SCJAllowed(INFRASTRUCTURE)
	public void enter(Runnable logic) {
		super.enter(logic);
		destroyChild();
	}

	/**
	 * Will destroy the child when leaving from the parent's ManagedMemory!!
	 * IMPLICATION: The child will live as long as the parents' memory is living
	 * so we can re-enter the child from there as many times we want and there
	 * will be any leak.
	 */
	@SCJAllowed(INFRASTRUCTURE)
	protected void destroyChild() {
		if (_child != null) {
			_child = null;
		}
	}

	/**
	 *
	 * [IN GENERAL] : If private memory does not exist, create one; otherwise
	 * set its size; then, enter the private memory. The actual memory area will
	 * be physically allocated upon "enter()." and will be freed when "enter" is
	 * left. The ManagedMemory object representing this area will be however
	 * allocated first-time the enterPrivateMemory is called and will be
	 * destroyed when the parent ManagedMemory is being left.
	 * @see destroyChild
	 *
	 *  On enterPrivateMemory in MissionMemory class:
	 *  	During the initialization phase of a mission, it is legal to invoke
	 *      enterPrivateMemory on the current MissionMemory object (which we
	 *      obtain by calling getCurrentMemory). This gives you a temporary
	 *      memory area for doing some computation that can be thrown away after
	 *      initialization. Once the initialization phase is completed the
	 *      method will throw an exception. [jan]
	 *
	 * _child object is holding reference the _child ManagedMemory which is however allocated in the parent memory, while
	 * the _child field of "this" is residing in the MissionMemory - this is scope-check violation and we need to use @NoScopeCheck
	 * annotation.
	 *
	 * @throws IllegaleStateException
	 *             when called from another memory area or from a thread that
	 *             does not own the current managed memory.
	 *
	 *
	 *
	 * @param size
	 * @param logic
	 */
	@SCJAllowed
	@RunsIn(CALLER)
	@com.fiji.fivm.r1.NoScopeChecks
	public void enterPrivateMemory(long size,@Scope(UNKNOWN) Runnable logic) {
		if (size < 0)
			throw new IllegalArgumentException(
					"Mission memory size must be non-negative");

		if (_child == null) {
			_child = new PrivateMemory(size);
			_child.setOwner(_owner);
		} else
			_child.setSize(size);
		// TODO FIXME : test that when we re-enter the private memory
		//              with the same owner!! with the same thread!!
		// FIXME: how to figure out who is the current SCHEdulable object??? its not the same as the currentThread()

		// FIXME: check that the scope is not already entered!!!

		_child.enter(logic);
	}

	/**
	 * This method requires that the "this" argument reside in a scope that encloses the scope of the "logic" argument.
	 * 
	 * @param logic
	 */
	@SCJAllowed
	@SCJRestricted(maySelfSuspend = false)
	@RunsIn(CALLER)
	public void executeInArea(@Scope(UNKNOWN) Runnable logic) { 
	    super.executeInArea(logic);
	}

	/**
	 *
	 * @return Returns the ManagedSchedulable that owns this managed memory.
	 */
	@SCJAllowed
	public ManagedSchedulable getOwner() {
		return _owner;
	}

	@SCJAllowed(INFRASTRUCTURE)
	public void setOwner(ManagedSchedulable owner) {
		_owner = owner;

	}

	/**
	 * TODO: This is a dynamic guard method needed by the SCJ Checker.
	 *
	 * @param r
	 * @param cs
	 * @return
	 */
	@RunsIn(CALLER)
	@SCJAllowed
	public static boolean allocatedInSame(@Scope(UNKNOWN) Object obj1, @Scope(UNKNOWN) Object obj2) {
		// TODO Auto-generated method stub
		return false;
	}


	/**
	 * TODO: This is a dynamic guard method needed by the SCJ Checker.
	 *
	 * @param r
	 * @param cs
	 * @return
	 */
	@RunsIn(CALLER)
	@SCJAllowed
	public static boolean allocatedInParent(@Scope(UNKNOWN) Object obj1, @Scope(UNKNOWN) Object obj2) {
		// TODO Auto-generated method stub
		return false;
	}
}
