/*
 * Ticker.java
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

public class Ticker {
    static Error usage() {
        System.err.println("Usage: ticker <thread priority> <period in nanoseconds> noisy|quiet");
        System.exit(1);
        return null;
    }
    
    public static void main(String[] v) {
        final int prio;
        final long period;
        final boolean noisy;
        
        System.out.println("Ticker starting!");
        
        if (v.length==0) {
            prio=ThreadPriority.FIFO_MAX-1;
            period=100*1000*1000;
            noisy=true;
        } else {
            if (v.length!=3) {
                throw usage();
            }
        
            try {
                prio=ThreadPriority.fromString(v[0]);
                period=Long.parseLong(v[1]);
                if (v[2].equals("noisy")) {
                    noisy=true;
                } else if (v[2].equals("quiet")) {
                    noisy=false;
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                System.err.println("Error: could not parse arguments: "+e);
                throw usage();
            }
        }
        
        Thread t=new Thread() {
                public void run() {
                    try {
                        RateMon rm=new RateMon(period);
                        for (;;) {
                            rm.waitForNextPeriod();
                            if (rm.missedDeadline()) {
                                System.out.println(Time.nanoTime()+"   MISSED "+rm.recentMissedDeadlines()+" DEADLINES!");
                            }
                            if (noisy) {
                                System.out.println(Time.nanoTime()+"   Tick!  "+rm.currentIteration()+"   (latency: "+(Time.nanoTime()-rm.currentIterationStart())+"   misses: "+rm.totalMissedDeadlines()+")");
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            };
        
        t.setPriority(prio);
        t.start();
    }
}


