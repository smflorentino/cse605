/*
 * ProfilerThread.java
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

package com.fiji.fivm.r1;

import static com.fiji.fivm.r1.fivmRuntime.*;
import java.util.*;

@UsesMagic
public final class ProfilerThread extends Thread {
    
    static boolean running=false;
    
    ProfilerThread() {
	super("fivmr Profiler Thread");
	setDaemon(true);
        
        log(ProfilerThread.class,1,"Profiler thread created.");
        
        // FIXME: priority?

        synchronized (ProfilerThread.class) {
            if (running) {
                throw new fivmError("Cannot have more than one profiler thread running.");
            }
            running=true;
        }
    }
    
    public static synchronized void startProfiler() {
        if (!running) {
            new ProfilerThread().start();
        }
    }
    
    Object lock=new Object();
    HashMap< StackTraceFrame, MutableInt > lineCounts=
	new HashMap< StackTraceFrame, MutableInt >();
    HashMap< PointerBox, MutableInt > methodCounts=
        new HashMap< PointerBox, MutableInt >();
    HashMap< StackTraceFrame, MutableInt > topLineCounts=
        new HashMap< StackTraceFrame, MutableInt >();
    int totalCount;

    static < T > ArrayList< Map.Entry< T, MutableInt > > sort(Map< T, MutableInt > counts) {
        ArrayList< Map.Entry< T, MutableInt > > sorted=
            new ArrayList< Map.Entry< T, MutableInt > >(
                counts.entrySet());
        Collections.sort(
            sorted,
            new Comparator< Map.Entry< T, MutableInt > >() {
                public int compare(
                    Map.Entry< T, MutableInt > a,
                    Map.Entry< T, MutableInt > b) {
                    // sort in descending order!
                    return b.getValue().compareTo(a.getValue());
                }
            });
        return sorted;
    }
    
    public void run() {
	try {
            log(ProfilerThread.class,1,"Profiler thread running.");
            
	    Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
			synchronized (lock) {
			    logPrintFull("Per-line recursive profiling report:  (total counts = "+totalCount+")\n");
			    for (Map.Entry< StackTraceFrame, MutableInt > e : sort(lineCounts)) {
				logPrintFull(
				    "  "+(e.getValue().value()*100/totalCount)+"%  "+e.getKey()+"\n");
			    }
                            logPrintFull("\n");
                            logPrintFull("Per-method recursive profiling report:  (total counts = "+totalCount+")\n");
                            for (Map.Entry< PointerBox, MutableInt > e : sort(methodCounts)) {
				logPrintFull(
				    "  "+(e.getValue().value()*100/totalCount)+"%  "+fivmRuntime.methodRecToString(e.getKey().value())+"\n");
                            }
                            logPrintFull("\n");
                            logPrintFull("Per-line stack-top profiling report:  (total counts = "+totalCount+")\n");
			    for (Map.Entry< StackTraceFrame, MutableInt > e : sort(topLineCounts)) {
				logPrintFull(
				    "  "+(e.getValue().value()*100/totalCount)+"%  "+e.getKey()+"\n");
			    }
			}
		    }
		});

	    for (;;) {
		Thread.sleep(10);
		List< ThreadStackTrace > atst=DebugAndProfile.getAllThreadStackTraces();
		synchronized (lock) {
		    for (ThreadStackTrace tst : atst) {
			if (tst.thread!=this) {
			    for (StackTraceFrame stf :
                                     new HashSet< StackTraceFrame >(tst.frames)) {
				MutableInt count=lineCounts.get(stf);
				if (count==null) {
				    lineCounts.put(stf,count=new MutableInt());
				}
				count.setValue(count.value()+1);
			    }
                            HashSet< PointerBox > methods=new HashSet< PointerBox >();
                            for (StackTraceFrame stf : tst.frames) {
                                methods.add(new PointerBox(stf.methodRec));
                            }
                            for (PointerBox mr : methods) {
				MutableInt count=methodCounts.get(mr);
				if (count==null) {
				    methodCounts.put(mr,count=new MutableInt());
				}
				count.setValue(count.value()+1);
                            }
                            if (!tst.frames.isEmpty()) {
                                MutableInt count=topLineCounts.get(tst.frames.get(0));
                                if (count==null) {
                                    topLineCounts.put(tst.frames.get(0),count=new MutableInt());
                                }
                                count.setValue(count.value()+1);
                            }
			}
		    }
		    totalCount++;
		}
	    }
	} catch (Throwable e) {
	    try {
		e.printStackTrace();
	    } catch (Throwable e2) {}
	    fivmRuntime.abort("Profiler thread got unexpected exception.");
	}
    }
}

