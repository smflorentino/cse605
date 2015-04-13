/*
 * SimpleRTSJWaitForPeriodTest.java
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

import com.fiji.fivm.util.GCControl;
import com.fiji.rt.Configuration;
import com.fiji.rt.MemoryManager;
import javax.realtime.RealtimeThread;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.RelativeTime;
import javax.realtime.AbsoluteTime;

public class SimpleRTSJWaitForPeriodTest {
    public static class PeriodicThread extends RealtimeThread {
	protected int millis;
	protected int periods;

	public PeriodicThread(int millis, int periods) {
	    super();
	    this.periods=periods;
	    this.millis=millis;
	}

	public void run() {
	    int i = 0;
	    RealtimeThread current = RealtimeThread.currentRealtimeThread();
	    long startTime=System.nanoTime()/(millis*1000L)*(millis*1000L)+millis*1000L;
	    current.setReleaseParameters(new PeriodicParameters(new AbsoluteTime(startTime/1000000, (int)(startTime%1000000)), new RelativeTime(millis,0)));
	    current.setSchedulingParameters(new PriorityParameters(PriorityScheduler.getMaxPriority(current)-1));
            System.out.println("PeriodicThread running with period:" + millis);
	    long millisStart = startTime/1000000;
	    long millisEnd = 0;
	    while (i < periods) {
		this.waitForNextPeriod();
                //long before=System.nanoTime();
		millisEnd = System.nanoTime()/1000/1000;
		System.out.println("PeriodicThread running with period:" + millis + " woke up at time" + millisEnd);
                //long after=System.nanoTime();
                //System.out.println("   that took "+(after-before)+" ns.");
		i++;
	    }
	    if((millisEnd - millisStart) > (millis*periods)+11) {
		System.out.println("Start " + millisStart);
		System.out.println("End " + millisEnd);
		Fail.abort("PeriodicThread took longer than (millis*periods)+11: "+(millisEnd - millisStart)+">"+((millis*periods)+11));
	    }
	}
    }

    public static class RelativePeriodicThread extends PeriodicThread {
	private int myID;
	private static int id =0;
	private static Object lock = new Object();

	public RelativePeriodicThread(int millis, int periods) {
	    super(millis, periods);
	    synchronized(lock){
		this.myID = id;
		id++;
	    }
	    long time=System.nanoTime();
	    this.setReleaseParameters(new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(millis,0)));
	}

	public void run() {
	    int i = 0;
	    RealtimeThread current = RealtimeThread.currentRealtimeThread();
	    current.setSchedulingParameters(new PriorityParameters(PriorityScheduler.getMaxPriority(current)-1));
            System.out.println("RelaitvePeriodicThread"+myID+" running with period:" + millis);
	    long millisStart = System.nanoTime()/1000000;
	    long millisEnd = 0; 
	    while (i < periods) {
		this.waitForNextPeriod();
		millisEnd = System.nanoTime()/1000000;
		System.out.println("RelativePeriodicThread"+myID+" running with period:" + millis + " woke up at time" + millisEnd);
		i++;
	    }
	    if((millisEnd - millisStart) > (millis*(periods+1))){
		Fail.abort("RelaitvePeriodicThread"+myID+" took longer than (millis*(periods+1)): "+(millisEnd - millisStart)+">"+(millis*(periods+1)));
	    }
	}
    }

     public static class InterruptablePeriodicThread extends PeriodicThread {
	

	public InterruptablePeriodicThread(int millis, int periods) {
	    super(millis, periods);
	    this.setReleaseParameters(new PeriodicParameters(new RelativeTime(0, 0), new RelativeTime(millis,0)));
	}

	public void run() {
	    int i = 0;
	    RealtimeThread current = RealtimeThread.currentRealtimeThread();
	    current.setSchedulingParameters(new PriorityParameters(PriorityScheduler.getMaxPriority(current)-1));
            System.out.println("InterruptablePeriodicThread running with period:" + millis);
	    while (i < periods) {
		System.out.println("InterruptablePeriodicThread running with period:" + millis + " going to sleep at time" + (System.nanoTime()/1000000));
		try{
		    this.waitForNextPeriodInterruptable();
		}
		catch(InterruptedException e){
		    System.out.println("Interrupted!");
		}
		System.out.println("InterruptablePeriodicThread running with period:" + millis + " woke up at time" + (System.nanoTime()/1000000));
		i++;
		    
	    }
	}
    }


    public static class Config {
    	public static void configure(Configuration c) {
            if (c.getMemoryManager() instanceof MemoryManager.GarbageCollector) {
                ((MemoryManager.GarbageCollector)c.getMemoryManager()).setThreadPriority(1);
            }
    	}
    }

    public static class testExec extends RealtimeThread{

	public testExec(){
	}

	public void run() {
	    System.out.println("Checking GC thread priority");

	    if (GCControl.getPriority() != 1) {
		Fail.abort("GC Thread Priority is "
			   + GCControl.getPriority()
			   + " should be 1");
	    }

	    PeriodicThread p1 = new PeriodicThread(20, 100);
	    PeriodicThread p2 = new PeriodicThread(100, 10);
	    RealtimeThread current = RealtimeThread.currentRealtimeThread();
	    current.setSchedulingParameters(new PriorityParameters(PriorityScheduler.getMaxPriority(current)));

	    p1.start();
	    System.out.println("First thread started.");
	    try {
		p1.join();
	    } catch (Throwable e) {
		Fail.abort(e);
	    }
	    

	    p2.start();
	    System.out.println("Second thread started.");

	    try {
		p2.join();
	    } catch (Throwable e) {
		Fail.abort(e);
	    }


	    RelativePeriodicThread r1 = new RelativePeriodicThread(20, 20);
	    RelativePeriodicThread r2 = new RelativePeriodicThread(40, 10);
	    RelativePeriodicThread r3 = new RelativePeriodicThread(100, 4);

	    r1.start();
	    System.out.println("First Relative thread started.");
	    r2.start();
	    System.out.println("Second Relative thread started.");
	    r3.start();
	    System.out.println("Third Relative thread started.");
	     try {
		 r1.join();
		 r2.join();
		 r3.join();
	    } catch (Throwable e) {
		Fail.abort(e);
	    }


	     System.out.println("Testing thread interupts");
	     r1 = new RelativePeriodicThread(250, 4);
	     r1.start();
	     System.out.println("Relative thread started.");
	     for(int j = 0; j < 9; j++){
		 try{
		     current.sleep(100);
		 }
		 catch(InterruptedException e){
		     Fail.abort(e);
		 }
		 System.out.println("Interrupting.");
		 r1.interrupt();
	     }
	     try {
		 r1.join();
	     } catch (Throwable e) {
		 Fail.abort(e);
	     }
	     System.out.println("SUCCESS (I think)");
	}

    }

    public static void main(String args[]) {
	testExec test = new testExec();
	test.start();
    }
}
