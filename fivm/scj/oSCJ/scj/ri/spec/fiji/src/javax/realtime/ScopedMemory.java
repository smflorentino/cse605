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

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed
public abstract class ScopedMemory extends MemoryArea implements ScopedAllocationContext {

	/**
	 * The primordial scope acts as the parent for any scope for which no scoped
	 * memory appears above it in the current scope stack.
	 */
	static final ScopedMemory _primordialScope = new ScopedMemory(0) {
		@Override
        public String toString() {
			return "PrimordialScope";
		}

		//@Override
		public void resize(long size) throws IllegalStateException {
			// TODO: resizing Scoped Memory, should we implement this?
		}
	};

	/**
	 * The joiner for this scoped memory area. All synchronization is performed
	 * on this object. Scopes can only be entered or exited when the lock on
	 * this object is held.
	 */
	//Object _joiner = new Object();
	ScopedMemory _parent;
	private Object _portal;

	@SCJAllowed(INFRASTRUCTURE)
	@SCJRestricted(maySelfSuspend = true)
	public ScopedMemory(long size) {
		super(size);
	}

	@SCJAllowed(INFRASTRUCTURE)
	@SCJRestricted(maySelfSuspend = true)
	public ScopedMemory(SizeEstimator estimator) {
		super(estimator.getEstimate());
	}

	/*
	 * we have to override most of the public methods of MemoryArea to declare
	 * the right exceptions
	 */
	@Override
	public void enter(Runnable logic) {
		if (logic == null)
			throw new IllegalArgumentException("null logic not permitted");
		RealtimeThread current = RealtimeThread.currentRealtimeThread();
		enterImpl(current, logic);
	}

	@Override
	@RunsIn(CALLER)
	public void executeInArea(@Scope(UNKNOWN) Runnable logic) {
		if (logic == null)
			throw new IllegalArgumentException("null logic not permitted");
		RealtimeThread current = RealtimeThread.currentRealtimeThread();
		checkAccessible(current);
		execInAreaImpl(current, logic);
	}

	@Override
	@SCJAllowed
	@RunsIn(CALLER)
	public Object newArray(Class type, int number)
			throws NegativeArraySizeException, IllegalAccessException {
		RealtimeThread current = RealtimeThread.currentRealtimeThread();
		checkAccessible(current);
		return newArrayImpl(current, type, number);
	}

	@Override
	@RunsIn(CALLER)
	@SCJAllowed
	public Object newInstance(Class klass) throws InstantiationException,
			IllegalAccessException {
		RealtimeThread current = RealtimeThread.currentRealtimeThread();
		checkAccessible(current);
		return newInstanceImpl(current, klass);
	}

	public void join() throws InterruptedException {
		//synchronized (_joiner) {
		//	_joiner.wait();
		//}
	}

	@SCJAllowed
	@RunsIn(CALLER)
	public Object getPortal() {
		// TODO: checks all commented out now

		// have to check if the portal could be assigned to a field of an
		// object allocated in the current allocation context.
		// As the portal object must be allocated in this memory area, then
		// the only place it can be stored is in an inner memory area. Which
		// means that the current area is either 'this' or a descendent of
		// this.
		// FIXME: what if currentMA is not VM_ScopedArea? then we get
		// an ED class cast exception
		// Opaque currentMA = LibraryImports.getCurrentArea();
		// if (!SUPPORT_SCOPE_AREA_OF || currentMA == this.area
		// || LibraryImports.isProperDescendant(currentMA, this.area)) {

		return doGetPortal();
		// }
		// throw new IllegalAssignmentError("portal object inaccessible");
	}

	@SCJAllowed
	@RunsIn(CALLER)
	public void setPortal(@Scope(UNKNOWN) Object o) {
		// TODO: checks all commented out now

		// RealtimeThread current = RealtimeThread.currentRealtimeThread();
		// checkAccessible(current);
		// if (o == null) return; // nulls mean a no-op
		// if (SUPPORT_SCOPE_AREA_OF && getMemoryArea(o) != this) {
		// throw new
		// IllegalAssignmentError("portal object not allocated in target memory area");
		// }
		doSetPortal(o);
	}

	@com.fiji.fivm.r1.NoScopeChecks
	Object doGetPortal() {
		return _portal;
	}

	@com.fiji.fivm.r1.NoScopeChecks
	void doSetPortal(Object o) {
		_portal = o;
	}

	private void checkAccessible(RealtimeThread thread) {
		if (thread.getScopeStack().getIndex(this, true) < 0)
			throw new InaccessibleAreaException();
	}

	@Override
	protected void preScopeEnter(RealtimeThread t) {
		//synchronized (_joiner) {

		// DEBUG:
		// System.out.println("scope stack: " + RealtimeThread.currentRealtimeThread().getScopeStack());
    	// ScopeStack sstack = RealtimeThread.currentRealtimeThread().getScopeStack();
    	// System.out.println("current stack area : " + sstack.getCurrentArea());

    	_parent = RealtimeThread.currentRealtimeThread().getScopeStack()
					.getTopScopedMemory(true);
		//}
	}

	@Override
	protected void postScopeEnter() {
		//synchronized (_joiner) {
			_parent = null;
			//_joiner.notifyAll();
		//}
	}
}
