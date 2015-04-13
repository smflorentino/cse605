/*
 * VMThread.java
 * Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
 * This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
 * LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
 * available at fivm/LEGAL and can also be found at
 * http://www.fiji-systems.com/FPL3.txt
 * 
 * By installing, reproducing, distributing, and/or using the FIJI VM Software
 * you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
 * rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
 * restrictions stated therein.  Among other conditions and restrictions, the
 * FIJI PUBLIC LICENSE states that:
 * 
 * a. You may only make non-commercial use of the FIJI VM Software.
 * 
 * b. Any adaptation you make must be licensed under the same terms 
 * of the FIJI PUBLIC LICENSE.
 * 
 * c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
 * file, adaptation or output code that you distribute and cause the output code
 * to provide a notice of the FIJI PUBLIC LICENSE. 
 * 
 * d. You must not impose any additional conditions.
 * 
 * e. You must not assert or imply any connection, sponsorship or endorsement by
 * the author of the FIJI VM Software
 * 
 * f. You must take no derogatory action in relation to the FIJI VM Software
 * which would be prejudicial to the FIJI VM Software author's honor or
 * reputation.
 * 
 * 
 * The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
 * representation and provides no warranty of any kind concerning the software.
 * 
 * The FIJI PUBLIC LICENSE and any rights granted therein terminate
 * automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
 */

package java.lang;

import com.fiji.fivm.r1.*;
import com.fiji.fivm.*;

import static com.fiji.fivm.r1.Magic.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

import javax.realtime.RealtimeThread;

public final class VMThread {
    Pointer ts;
    Thread thread;
    boolean running;
    boolean dead;
    int priority;
    long backingStoreSize;

    /* sun.misc.Unsafe allows this to be sloppy, so no synchronization
     * needed. */
    volatile boolean unparked;
    Object parker;
    
    private VMThread(Thread t,int priority) {
	this.thread=t;
	this.priority=priority;
	unparked=false;
	parker=new Object[1];
    }
    
    @Export
    static VMThread VMThread_create(int priority,boolean daemon) {
    	log(VMThread.class,3,"VMThread : VMThread_create called");
    	Thread t=new Thread(new VMThread(null,priority),null,priority,daemon);
	t.vmThread.thread=t;
	t.group=ThreadGroup.root;
	t.priority=priority;
	return t.vmThread;
    }
    
    @Export
    static VMThread VMThread_createRT(int priority,boolean daemon) {
    	log(VMThread.class,3,"VMThread : VMThread_create called");
    	log(VMThread.class,3,"VMThread : Attempt to create RealtimeThread.");

    	Thread t = (Thread) new RealtimeThread(new VMThread(null,priority),"RT-PrimordialThread",priority,daemon);
    	log(VMThread.class,3,"VMThread : Realtime VMThread created.");

    	t.vmThread.thread=t;
    	t.group=ThreadGroup.root;
    	t.priority=priority;
    	return t.vmThread;
    }
    
    @Export
    void VMThread_setThreadState(Pointer ts) {
	log(this,2,("We have thread state at "+ts.asLong()));
	this.ts=ts;
    }
    
    @Export
    Pointer VMThread_getThreadState() {
	return ts;
    }
    
    @Import
    @GodGiven
    static native void fivmr_VMThread_start(Pointer curTS,VMThread self);
    
    @Export
    synchronized void VMThread_starting() {
	running=true;
    }
    
    void allocBackingStore() {
        if (backingStoreSize!=0) {
            synchronized (this) {
                MemoryAreas.allocScopeBacking(Magic.curThreadState(),backingStoreSize);
            }
        }
    }
    
    @Export
    void VMThread_run() {
        allocBackingStore();
	if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_THREAD, FlowLog.SUBTYPE_RUN, priority);
	}
        ThreadStartHook tsh=(ThreadStartHook)Magic.getObjField(thread,"fiji_runtimeField");
        if (tsh!=null) {
            tsh.go(this,thread);
        }
	thread.run();
    }
    
    @Export
    boolean VMThread_setUncaughtException(Object o) {
	if (o instanceof Throwable) {
	    try {
		thread.getUncaughtExceptionHandler()
		    .uncaughtException(thread,(Throwable)o);
		return true;
	    } catch (Throwable e) {
		log(this,0,"Could not set uncaught exception.");
		return false;
	    }
	} else {
	    throw abort("trying to set uncaught exception to something that "+
			"isn't a throable");
	}
    }
    
    @Export
    void VMThread_die() {
	try {
	    log(this,2,""+this+" dying.");
	} catch (Throwable e) {
	    log(this,0,"Thread dying with VM trouble.");
	}
	try {
	    synchronized (this) {
		running=false;
		dead=true;
		notifyAll();
	    }
	} catch (Throwable e) {
	    log(this,0,"During thread termination: Got exception while attempting to notify joiners.");
	}
	try {
	    thread.die();
	} catch (Throwable e) {
	    log(this,0,"Got exception while attempting to call Thread.die().");
	}
	if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_THREAD, FlowLog.SUBTYPE_EXIT, 0);
	}
	ts=Pointer.zero();
    }
    
    static void create(Thread t,long stacksize) {
	VMThread vmt=new VMThread(t,t.priority);
	t.vmThread=vmt;
	fivmr_VMThread_start(curThreadState(),vmt);
    }
    
    String getName() { return thread.name; }
    void setName(String name) { thread.name=name; }
    
    @Import
    private static native void fivmr_VMThread_priorityChanged(Pointer curTS,
                                                              VMThread self);
    
    @SuppressWarnings("unused")
    @Export
    private int VMThread_getPriority() {
	return getPriority();
    }
    
    void setPriority(int priority) {
	this.priority=priority;
	if (Settings.FLOW_LOGGING) {
	    long data = ((long)fivmr_ThreadState_id(this.ts) << 32) | priority;
	    FlowLog.log(FlowLog.TYPE_THREAD, FlowLog.SUBTYPE_PRIORITY, data);
	}
	fivmr_VMThread_priorityChanged(curThreadState(),this);
    }

    int getPriority() { return priority; }
    
    boolean isDaemon() { return thread.daemon; }
    
    @Export
    boolean VMThread_isDaemon() { return isDaemon(); }
    
    int countStackFrames() { return 0; /* FIXME */ }
    
    synchronized void join(long ms, int ns) throws InterruptedException {
	log(this,2,"Joining on "+this+" from "+currentThread().vmThread+".");
	long whenAwake=ms*1000*1000*1000+ns;
	while ((whenAwake==0 || Time.nanoTime()<whenAwake) &&
	       !dead) {
	    waitAbsolute(this,whenAwake);
	}
	log(this,2,"Done joining on "+this+" from "+currentThread().vmThread+".");
    }

    void stop(Throwable t) {
	// FIXME: implement this eventually
    }
    
    boolean isInterrupted() {
	if (ts==Pointer.zero()) {
	    return false;
	} else {
	    return fivmr_ThreadState_getInterrupted(ts);
	}
    }
    
    void interrupt() {
	if (ts!=Pointer.zero()) {
	    fivmr_ThreadState_setInterrupted(ts,true);
	}
    }
    
    void suspend() {
	// FIXME: implement this eventually
    }
    
    void resume() {
	// FIXME: implement this eventually
    }
    
    @RuntimeImport
    static native VMThread fivmr_ThreadState_javaThreadObject(Pointer ts);

    static Thread currentThread() {
	return fivmr_ThreadState_javaThreadObject(curThreadState()).thread;
    }
    
    static void yield() {
	if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_THREAD, FlowLog.SUBTYPE_YIELD, 0L);
	}
	fivmRuntime.yield();
	if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_THREAD, FlowLog.SUBTYPE_WAKE, 0L);
	}
    }
    
    static void sleep(long ms, int ns) throws InterruptedException {
	if (ms==0 && ns==0) {
	    yield();
	} else {
	    if (Settings.FLOW_LOGGING) {
		long wakeTime = Time.nanoTime()+ms*1000*1000+ns;
		FlowLog.log(FlowLog.TYPE_THREAD, FlowLog.SUBTYPE_SLEEP,
			    wakeTime);
		sleepAbsolute(wakeTime);
		FlowLog.log(FlowLog.TYPE_THREAD, FlowLog.SUBTYPE_WAKE,
			    wakeTime);
	    } else {
		sleepAbsolute(Time.nanoTime()+ms*1000*1000+ns);
	    }
	}
    }
    
    static boolean interrupted() {
	return fivmRuntime.interrupted();
    }
    
    static boolean holdsLock(Object o) {
	return curHolder(o)==curThreadState();
    }
    
    String getState() {
	if (ts==Pointer.zero()) {
	    if (dead) {
		return "TERMINATED";
	    } else {
		return "NEW";
	    }
	} else {
	    return fromCStringFull(fivmr_ThreadState_describeState(ts));
	}
    }
    
    public String toString() {
	Pointer ts=this.ts;
	if (ts==Pointer.zero()) {
	    return "Dead Thread ("+thread+")";
	} else {
	    return "Thread "+fivmr_ThreadState_id(ts)+" ("+thread+")";
	}
    }

    public void park(boolean isAbsolute, long time) {
	if (!unparked) {
	    if (isAbsolute && time > 0) {
		try {
		    fivmRuntime.waitAbsolute(parker, time * 1000000);
		} catch (InterruptedException e) {
		}
	    } else if (time > 0) {
		try {
		    fivmRuntime.waitRelative(parker, time);
		} catch (InterruptedException e) {
		}
	    } else {
		try {
		    fivmRuntime.wait(parker);
		} catch (InterruptedException e) {
		}
	    }
	}
	unparked = false;
    }

    public void unpark() {
	unparked = true;
	fivmRuntime.notify(parker);
    }
}


