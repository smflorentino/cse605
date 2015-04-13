/*
 * PIPTest.java
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

import com.fiji.fivm.*;

public class PIPTest {
    static final long start=System.currentTimeMillis();
    
    static void log(String s) {
        System.err.println(""+(System.currentTimeMillis()-start)+": "+s);
    }
    
    public static void main(String[] v) throws Throwable {
        int niterinner;
        int niterouter;
        if (v.length==0) {
            log("running 25 iterations.");
            niterinner=5;
            niterouter=5;
        } else {
            niterinner=Integer.parseInt(v[0]);
            niterouter=Integer.parseInt(v[1]);
        }

        for (int j=0;j<niterouter;++j) {
            final Object start=new Object();
            final Object lock=new Object();
            for (int i=0;i<niterinner;++i) {
                log("starting iteration "+i+" + "+j+"*5");
                
                final boolean[] lowStop=new boolean[1];
                final boolean[] stop=new boolean[1];
                final long[] timestamp=new long[1];
            
                Thread low,med,high;
            
                synchronized (start) {
                    low=new Thread() {
                            public void run() {
                                try {
                                    synchronized (start) {
                                        log("Low starting");
                                    }
                                    long before=System.nanoTime();
                                    synchronized(lock) {
                                        log("Low acquired lock.  Time to acquire: "+(System.nanoTime()-before)+" ns");
                                        Object o=new Object();
                                        while (!lowStop[0]) {
                                            synchronized (o) {} // force fence
                                        }
                                        timestamp[0]=System.nanoTime();
                                    }
                                    log("Low done.");
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }
                        };
                
                    med=new Thread() {
                            public void run() {
                                try {
                                    synchronized (start) {
                                        log("Med starting");
                                    }
                                    log("Med sleeping for 500ms...");
                                    Thread.sleep(500);
                                    log("Med busy-waiting.");
                                    Object o=new Object();
                                    while (!stop[0]) {
                                        synchronized (o) {} // force fence
                                    }
                                    log("Med done.");
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }
                        };
                
                    high=new Thread() {
                            public void run() {
                                try {
                                    synchronized (start) {
                                        log("High starting");
                                    }
                                    log("High sleeping for 1000ms...");
                                    Thread.sleep(1000);
                                    log("High trying to acquire lock.");
                                    lowStop[0]=true;
                                    synchronized (lock) {
                                        // on RTEMS this takes 831 us
                                        log("Success!  Time to preempt: "+(System.nanoTime()-timestamp[0]));
                                    }
                                    stop[0]=true;
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            }
                        };
                
                    low.setPriority(ThreadPriority.FIFO_MAX-2);
                    med.setPriority(ThreadPriority.FIFO_MAX-1);
                    high.setPriority(ThreadPriority.FIFO_MAX);
                
                    low.start();
                    med.start();
                    high.start();
                
                    log("Threads started, will delay a bit to ensure "+
                        "synchronized start...");
                    Thread.sleep(1000);
                }
            
                log("Threads running.  Joining...");
            
                low.join();
                med.join();
                high.join();
            }
        }
        
        System.out.println("SUCCESS!");
    }
}


