/*
 * BiasedLockExplosion.java
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

import com.fiji.util.*;
import com.fiji.fivm.*;

public class BiasedLockExplosion {
    static class Box {
        // this is deliberately a float; I want it to behave gracefully when it overflows
        // (i.e. I want it to just more-or-less get stuck at some large value) and I
        // want it to be one word so that a 32-bit VM can load it atomically without doing
        // anything special.
        float count;
        
        Box() {
            count=0;
        }
        
        synchronized void doStuff() {
            count++;
        }
    }

    static boolean keepGoing;
    static Box[] array;
    static Thread[] threads;
    static long duration;
    
    static long report() {
        SimpleStats stats=new SimpleStats();
        for (Box b : array) {
            stats.add(b.count);
        }
        System.out.println(stats);
        return (long)stats.getSum();
    }
    
    static long doit() throws Throwable {
        keepGoing=true;
        
        for (int i=0;i<array.length;++i) {
            array[i]=new Box();
        }
        
        final MarsenneTwister masterMT=new MarsenneTwister();
        
        for (int i=0;i<threads.length;++i) {
            threads[i]=new Thread(){
                    public void run() {
                        try {
                            MarsenneTwister mt;
                            synchronized (masterMT) {
                                mt=new MarsenneTwister(masterMT.nextInt());
                            }
                            while (keepGoing) {
                                array[mt.nextInt(array.length)].doStuff();
                            }
                        } catch (Throwable e) {
                            System.err.println("Error in "+this+": "+e);
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                };
        }
        
        for (Thread t : threads) {
            t.start();
        }
        
        long left=duration;
        
        while (left>1000) {
            Thread.sleep(1000);
            left-=1000;
            report();
        }
        
        Thread.sleep(left);
        long result=report();
        
        keepGoing=false;
        
        for (Thread t : threads) {
            t.join();
        }

        System.out.println("done.");
        
        return result;
    }
    
    public static void main(String[] v) throws Throwable {
        if (v.length==0) {
            System.out.println("Arguments: <array size> <threads> <duration ms> <warmup> <bench>");
            System.exit(1);
        }
        
        array=new Box[Integer.parseInt(v[0])];
        threads=new Thread[Integer.parseInt(v[1])];
        duration=Long.parseLong(v[2]);
        
        for (int i=Integer.parseInt(v[3]);i-->0;) {
            System.out.println("Warmup:");
            doit();
        }
        
        SimpleStats resultStats=new SimpleStats();
        
        for (int i=Integer.parseInt(v[4]);i-->0;) {
            System.out.println("Benchmark:");
            long result=doit();
            resultStats.add(result);
        }
        
        System.out.println("RESULT: "+resultStats);
    }
}

