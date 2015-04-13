/*
 * SimpleRTSJAehTest.java
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

import javax.realtime.RealtimeThread;
import javax.realtime.AsyncEventHandler;
import javax.realtime.AsyncEvent;
import javax.realtime.PriorityScheduler;
import javax.realtime.PriorityParameters;


public class SimpleRTSJAehTest {
    public static class SimpleHandler implements Runnable{
	private int count;
      
	public SimpleHandler(){
	    this.count = 0;
	}

	public int getCount(){
	    return count;
	}
	
	public void clearCount(){
	    count = 0;
	}

	public void run(){
	    count++;
	}
    }

    public static class SimpleHandlerPrinter extends SimpleHandler implements Runnable{
	private String str;

	public SimpleHandlerPrinter(String str){
	    this.str = str;
	}

	public void run(){
	    super.run();
	    System.out.println(str);
	}
    }

    public static class testExec extends RealtimeThread{
	
	public testExec(){
	}
	
	public void run() {
	    int i;
	    System.out.println("Testing Simple Event and Handler:");
	    SimpleHandler logic = new SimpleHandler();
	    AsyncEvent event = new AsyncEvent();
	    AsyncEventHandler handler = new AsyncEventHandler(logic);
	    handler.setDaemon(true);
	    event.addHandler(handler);
	    event.fire();
	    try {
		RealtimeThread.sleep(5000);
	    } catch (Throwable e) {
		Fail.abort(e);
	    }
	    if(logic.getCount() != 1)
		Fail.abort("Simple Handler did not fire once; logic.getCount() = "+logic.getCount());
	    System.out.println("That seemed to work.");

	    System.out.println("Testing adding a Handler twice:");
	    logic.clearCount();
	    event.addHandler(handler);
	    event.fire();
	    try {
		RealtimeThread.sleep(5000);
	    } catch (Throwable e) {
		Fail.abort(e);
	    }
	    if(logic.getCount() != 1)
		Fail.abort("Adding a Handler twice was not disallowed");
		System.out.println("That seemed to work.");

	    System.out.println("Testing Simple Event and Multiple Handlers:");
	    logic = new SimpleHandler();
	    event = new AsyncEvent();
	    handler = new AsyncEventHandler(logic);
	    handler.setDaemon(true);
	    event.addHandler(handler);
	    handler = new AsyncEventHandler(logic);
	    handler.setDaemon(true);
	    event.addHandler(handler);
	    event.fire();
	    try {
		RealtimeThread.sleep(5000);
	    } catch (Throwable e) {
		Fail.abort(e);
	    }
	    if(logic.getCount() != 2)
		Fail.abort("Multiple Handlers did not fire");
		System.out.println("That seemed to work.");


	    System.out.println("Testing Many Events and One Handler:");
	    logic = new SimpleHandler();
	    handler = new AsyncEventHandler(logic);
	    handler.setDaemon(true);
	    AsyncEvent[] eventArray = new AsyncEvent[10];
	    for(i = 0; i < eventArray.length; i++){
		eventArray[i] = new AsyncEvent();
		eventArray[i].addHandler(handler);
	    }
	    for(i = 0; i < eventArray.length; i++){
		eventArray[i].fire();
	    }
	    try {
		RealtimeThread.sleep(5000);
	    } catch (Throwable e) {
		Fail.abort(e);
	    }
	    if(logic.getCount() != 10)
		Fail.abort("Handlers did not fire the required amount of times");
		System.out.println("That seemed to work.");


	    System.out.println("Testing one Event and priority ordered Handlers:");
	    event = new AsyncEvent();
	    logic = new SimpleHandlerPrinter("Low priority handler");
	    handler = new AsyncEventHandler(new PriorityParameters(10), 
					    null,
					    null,
					    null,
					    null,
					    logic);
	    handler.setDaemon(true);
	    event.addHandler(handler);
	    logic = new SimpleHandlerPrinter("High priority handler");
	    handler = new AsyncEventHandler(new PriorityParameters(255), 
					    null,
					    null,
					    null,
					    null,
					    logic);
	    handler.setDaemon(true);
	    event.addHandler(handler);
	    for(i = 0; i < 10; i++){
		event.fire(); 
		try {
		    RealtimeThread.sleep(10000);
		} catch (Throwable e) {
		    Fail.abort(e);
		}
	    }
	    System.out.println("SUCCESS (I think)");
	}
    }
    public static void main(String args[]) {
	testExec test = new testExec();
	test.start();
    }
}
