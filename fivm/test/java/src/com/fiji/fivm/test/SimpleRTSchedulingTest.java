/*
 * SimpleRTSchedulingTest.java
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

package com.fiji.fivm.test;

import com.fiji.fivm.ThreadPriority;
import com.fiji.fivm.util.GCControl;
import com.fiji.rt.Configuration;
import com.fiji.rt.MemoryManager;

public class SimpleRTSchedulingTest {
    public static class BusyWaiter extends Thread {
	private volatile int touches;

	public BusyWaiter() {
	    touches = 0;
	}

	public void run() {
            System.out.println("BusyWaiter running.");
	    while (true) {
		if (Thread.interrupted())
		    return;
		touches++;
	    }
	}

	public int getTouches() {
	    return touches;
	}
    }

    public static class DualPriority implements Runnable {
	private volatile int touches;

	public void run() {
            System.out.println("DualPriority running.");
            
	    BusyWaiter t = new BusyWaiter();

	    t.setPriority(ThreadPriority.NORMAL_MIN);
	    t.start();

	    for (int i = 0; i < 5000000; i++)
		touches++;

	    if (t.getTouches() > 0)
		Fail.abort("Low priority child received CPU time; touches = " + t.getTouches());

	    t.interrupt();

	    System.out.println("Low priority child process got no CPU time");

	    try {
		t.join();
	    } catch (Throwable e) {
		Fail.abort(e);
	    }

	    if (t.getTouches() > 0)
		Fail.abort("Low priority child did not die immediately!");

	    System.out.println("Low priority child died without running");

	    return;
	}
    }

    public static class Config {
    	public static void configure(Configuration c) {
            if (c.getMemoryManager() instanceof MemoryManager.GarbageCollector) {
                ((MemoryManager.GarbageCollector)c.getMemoryManager()).setThreadPriority(1);
            }
    	}
    }

    public static void main(String args[]) {
	System.out.println("Checking GC thread priority");

	if (GCControl.getPriority() != 1) {
	    Fail.abort("GC Thread Priority is "
                       + GCControl.getPriority()
                       + " should be 1");
	}

	Thread dp = new Thread(new DualPriority());

	dp.setPriority(ThreadPriority.FIFO_MAX);

	System.out.println("Starting dual-priority test");

	dp.start();
        System.out.println("DualPriority Thread started.");

	try {
	    dp.join();
	} catch (Throwable e) {
	    Fail.abort(e);
	}

	System.out.println("Dual-priority test successful!");

	System.out.println("Starting timesharing test");

	BusyWaiter b1 = new BusyWaiter();
	BusyWaiter b2 = new BusyWaiter();

	Thread.currentThread().setPriority(ThreadPriority.FIFO_MIN);
	b1.setPriority(ThreadPriority.NORMAL_MAX);
	b2.setPriority(ThreadPriority.NORMAL_MAX);
        
        // FIXME: this is not correct for a fixed priority preemptive system.
        // it works on RTEMS by some miracle that I don't understand.
	b1.start();
        System.out.println("First thread started.");
	b2.start();
        System.out.println("Second thread started.");

        System.out.println("Sleeping...");
	try {
	    Thread.sleep(5000);
	} catch (Throwable e) {
	    Fail.abort(e);
	}
        System.out.println("Done sleeping.");

	b1.interrupt();
	b2.interrupt();
	try {
	    b1.join();
	    b2.join();
	} catch (Throwable e) {
	    Fail.abort(e);
	}

	System.out.println("BusyWaiter 1 touches: " + b1.getTouches());
	System.out.println("BusyWaiter 2 touches: " + b2.getTouches());
	if (b1.getTouches() == 0 || b2.getTouches() == 0)
	    Fail.abort("A BusyWaiter got no CPU time!");

	System.out.println("Timesharing test finished -- ensure that b1 and b2 both got similar time!");

	System.out.println("SUCCESS (I think)");
    }
}
