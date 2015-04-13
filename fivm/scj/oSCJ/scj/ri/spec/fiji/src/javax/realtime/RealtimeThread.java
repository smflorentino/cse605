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

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import edu.purdue.scj.BackingStoreID;
import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

// Provided by FijiVM:
import java.lang.VMThread;
import static com.fiji.fivm.r1.fivmRuntime.*;


/**
 * OVM version of this class differs from fiji version in that we don't have to
 * enter the initArea manually before "run()", OVM will do it for us instead.
 */
@SCJAllowed(LEVEL_1)
public class RealtimeThread extends Thread implements Schedulable {

	/**
	 * Package private class for internal VM threads. These threads don't use
	 * parameter objects at all, typically run at system level priority and can
	 * do anything they want :). There's a no-heap version defined inside NHRT
	 * that extends this one. No Schedulable methods, or public RTT methods
	 * should be called on these threads, but it is okay to call thread methods
	 * like start(), interrupt(), join(), if needed
	 *
	 *
	 * TODO: This class is used by Timer as special system threads. What special
	 * things should we do to it?
	 */
	// static class VMThread extends RealtimeThread {
	//
	// // The memory area aspects of VMThread are still unclear.
	// // It is "transparent" when creating new RTT's or AEH in terms of
	// // parameter objects, but the scope stack is copied
	//
	// VMThread() {
	// setPriority(RealtimeJavaDispatcher.SYSTEM_PRIORITY);
	// // all parameters are null
	// }
	// }

	/** The scheduling parameters for this thread */
	SchedulingParameters _sParams;

	/** The release parameters for this thread */
	ReleaseParameters _rParams;
	//
	// /** The scheduler bound to this thread - we only have one type in OVM */
	// PriorityScheduler _scheduler;

	/**
	 * The initial memory area associated with this thread, or with the
	 * currently executing <code>Schedulable</code>.
	 */
	final MemoryArea initArea;

	/**
	 * The index of the initial memory area in the initial scope stack of this
	 * thread. As returned by getInitialMemoryAreaIndex
	 */
	final int _initAreaIndex;

	/**
	 * The current scope stack. Only accessed directly during construction,
	 * startup.
	 */
	private ScopeStack _scopeStack;

	private Runnable _wrapper = new Runnable() {
		public void run() {
			RealtimeThread.super.run();
		}
	};

	/**
	 * The absolute time this thread should be released (accounting for the
	 * start specified in the release parameters). A periodic thread is
	 * initially scheduled relative to this time.
	 */
	// private long _startTimeNanos = 0;

	/**
	 * Internal locking object. This cannot be made different to the locking
	 * object used in our parent. We use Thread.priority to hold both normal
	 * java priorities and realtime priorities, hence we need to lock the Thread
	 * object in setSchedulingParameters.
	 */
	// private Object _rtLock = this;

	// private boolean _needEnter = true;

	/*
	 * Setting the thread priority by using reflection instead of via
	 * Thread.setPriority() is because the latter does not allow setting
	 * priorities other than normal ones.
	 */
	// private static final Field _rt_priority;

	// static {
	// try {
	// _rt_priority = Thread.class.getDeclaredField("priority");
	// _rt_priority.setAccessible(true);
	// } catch (NoSuchFieldException e) {
	// throw new LinkageError(e.toString());
	// }
	// }


	/** Used in primordial RTThread construction */
	public RealtimeThread(VMThread vmThread,String name, int priority, boolean daemon) {
		super(vmThread,name,priority, daemon);


		initArea = ImmortalMemory.instance();
		_scopeStack = new ScopeStack(this);
		_initAreaIndex = 0;

		//log(RealtimeThread.class,2,"[SCJ] VM RealtimeThread created");
		//System.out.println("[SCJ-DBG] RealtimeThread: RealtimeThreadCreated.");

		//System.out.println("Min rt priority : " + VMSupport.getMinRTPriority());
		//System.out.println("Max rt priority : " + VMSupport.getMaxRTPriority());

		//VMSupport.setThreadPriority(this,VMSupport.getMinRTPriority());
	}



	@SCJAllowed(INFRASTRUCTURE)
	/** Used in primordial RTThread construction */
	public RealtimeThread() {
		initArea = ImmortalMemory.instance();
		_scopeStack = new ScopeStack(this);
		_initAreaIndex = 0;

		VMSupport.setThreadPriority(this,VMSupport.getMinRTPriority());
	}

	//sfds

	@SCJAllowed(INFRASTRUCTURE)
	/** Used in primordial RTThread construction */
	public RealtimeThread(long bssize) {
		initArea = ImmortalMemory.instance();
		_scopeStack = new ScopeStack(this);
		_initAreaIndex = 0;
		//VMSupport.setTotalBackingStore(this, bssize);

		VMSupport.setThreadPriority(this,VMSupport.getMinRTPriority());
	}

	@SCJAllowed(INFRASTRUCTURE)
	public RealtimeThread(SchedulingParameters scheduling, MemoryArea area) {
		this(scheduling, null, null, area, null, null);
	}

	/*
	 * TODO: Which of these parameters are allowed by SCJ is unclear. Currently,
	 * we ignore all parameters except "scheduling". We don't support periodic
	 * thread neither.
	 */
	@SCJAllowed(INFRASTRUCTURE)
	public RealtimeThread(SchedulingParameters scheduling,
			ReleaseParameters release, MemoryParameters memory,
			MemoryArea area, ProcessingGroupParameters group, Runnable logic) {
		super(logic);

		// _scheduler = (PriorityScheduler) Scheduler.getDefaultScheduler();
		// if (scheduling == null)
		// scheduling = _scheduler.getDefaultSchedulingParameters();
		// setSchedulingParameters(scheduling);
		_scopeStack = new ScopeStack(this, RealtimeThread
				.currentRealtimeThread().getScopeStack());
		if (area == null) {
			Utils.panic("null init area not allowed");
		}

		initArea = area;
		_initAreaIndex = _scopeStack.getDepth(true);

		_scopeStack.dump();

		VMSupport.setThreadPriority(this,VMSupport.getMinRTPriority());
	}


	@SCJAllowed(Level.LEVEL_2)
	public ReleaseParameters getReleaseParameters() {
		return _rParams;
	}

	@SCJAllowed(Level.LEVEL_2)
	public SchedulingParameters getSchedulingParameters() {
		return _sParams;
	}

	/**
	 * Allocates no memory. Treats the implicit this argument as a variable
	 * residing in scoped memory.
	 */
	@Override
    public void start() {
		//////Utils.debugPrint("[SCJ] RealtimeThread.start() - enter");




		// need to make the MA of 'this' the current MA while we allocate
		// all the associated helper objects.
		// Note: we have a problem with exceptions. If they are created in
		// 'this' area and propagate out then someone can catch an exception
		// from a short-lived scope. We deal with IllegalThreadStateException
		// ourselves - no problem. OOME is also no problem. Anything else is
		// a bug - not sure what the consequences will be.

		// TODO: how to understand the above comments about exception? - lei

		BackingStoreID thisArea = VMSupport.areaOf(this);
		BackingStoreID oldArea = VMSupport.setCurrentArea(thisArea);

		// ////Utils.debugPrint("[SCJ] initial area:" + this.initArea);

		try {
			// synchronized (_rtLock) {
			// we need to ensure we only get called once per thread and
			// we can't hold the lock while invoking super.start() so we
			// use the start time as a flag
			// if (_startTimeNanos != 0) {
			// // we can do this in the right place - the extra set in
			// // the finally clause doesn't hurt
			// VMSupport.setCurrentArea(oldArea);
			// throw new IllegalThreadStateException(
			// "can't start a thread more than once");
			// }
			//
			// AbsoluteTime now = Clock.getRealtimeClock().getTime();
			// _startTimeNanos = now.toNanos();
			// }

			try {
				// release lock before calling super.start
				// Note: this can't throw an exception other than OOME or an
				// internal error of some kind. The former is a not a problem
				// because it's allocated in immortal. For internal errors we
				// don't try to do anything.
				super.start();
			} catch (OutOfMemoryError oome) {
				// _startTimeNanos = 0; // restore to unstarted state
				if (true) // avoid annoying javac warning
					throw oome;
			}
		} finally {
			VMSupport.setCurrentArea(oldArea);
			//////Utils.debugPrint("[SCJ] RealtimeThread.start() - end");
		}
	}

	// FIXME: subclass overriding this method can mess up our code. We know
	// users cannot do the bad thing because they are not allowed to subclass
	// RealtimeThread. So what we should do is to clearly document this
	// restriction.
	@Override
	@SCJAllowed(INFRASTRUCTURE)
	public void run() {

		//////Utils.debugPrint("[SCJ][  " + getCurrentMemoryArea()
		//		+ "] RealtimeThread.run()"
		//		+ "\n\t - Attempt to enter initArea " + initArea);

		initArea.preScopeEnter(this);
		RealtimeThread.currentRealtimeThread().getScopeStack().push(initArea);
		initArea.enter(_wrapper);
		RealtimeThread.currentRealtimeThread().getScopeStack().pop();
		initArea.postScopeEnter();
	}


	/**
	 * Allocates no memory. Does not allow this to escape local variables. The re- turned object may reside in scoped memory, within a scope that encloses this.
	 * @return
	 */
	@SCJAllowed(LEVEL_2)
	@SCJRestricted(maySelfSuspend = false, mayAllocate = false)
	public MemoryArea getMemoryArea( ) {
		// TODO : implement this
		//return initArea;
		return null;
	}

	/**
     * Allocates no memory. Returns an object that resides in the current mission�s MissionMemory.
     *
     *
     * @return
     */
	@SCJAllowed(LEVEL_2)
	@SCJRestricted(maySelfSuspend = false, mayAllocate = false)
	public static RealtimeThread currentRealtimeThread() {
		return (RealtimeThread) Thread.currentThread();
	}

	@SCJAllowed(LEVEL_1)
	@SCJRestricted(maySelfSuspend = false, mayAllocate = false)
	public static MemoryArea getCurrentMemoryArea() {
		return MemoryArea.getMemoryAreaObject(VMSupport.getCurrentArea());
	}

	public static int getMemoryAreaStackDepth() {
		return currentRealtimeThread().getScopeStack().getDepth(true);
	}

	public static MemoryArea getOuterMemoryArea(int index) {
		return currentRealtimeThread().getScopeStack().areaAt(index, true);
	}

	// /** NOTE: this is only called in construction */
	// private void setSchedulingParameters(SchedulingParameters sParams) {
	// PriorityParameters pParams = (PriorityParameters) sParams;
	// int prio = pParams.getPriority();
	//
	// if (!_scheduler.isValid(prio))
	// throw new IllegalArgumentException("priority " + prio
	// + " out of range");
	//
	// _sParams = pParams;
	// setPriorityInternal(PriorityScheduler.convertToVMPriority(prio));
	// }

	ScopeStack getScopeStack() {
		return _scopeStack;
	}

	// void setPriorityInternal(int prio) {
	// try {
	// _rt_priority.setInt(this, prio);
	// } catch (IllegalAccessException e) {
	// throw new Error(e);
	// }
	// }

	@SCJAllowed(LEVEL_2)
	@SCJRestricted(maySelfSuspend = true)
	public static void sleep(javax.realtime.HighResolutionTime time)
	    throws InterruptedException {
		//TODO:...
	};

	// DEBUG
	public void dumpInfo() {
		System.out.println("[RealtimeThread] name:" + this.getName());
		System.out.println("\t priority:" + this.getPriority());
		System.out.println("\t id:" + this.getId());
		System.out.println("\t memoryArea:" + this.getMemoryArea());
		System.out.println("\t initArea:" + this.initArea);
		System.out.println("\t stack:" + this.getScopeStack());
		System.out.println("\t memory stack depth:" + this.getMemoryAreaStackDepth());

	}

}
