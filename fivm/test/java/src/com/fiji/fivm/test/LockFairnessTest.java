/*
 * LockFairnessTest.java
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

import java.util.Arrays;

public class LockFairnessTest {
    static long cnt;
    static Object lock=new Object();
    
    public static void main(String[] v) throws Throwable {
        final long nthreads=Integer.parseInt(v[0]);
        final long niterations=Integer.parseInt(v[1]);
        
        doit(nthreads,niterations);
    }
    
    private static synchronized void runtest(final long[] localCounts,
                                             final long niterations) throws Throwable {
        final Thread[] threads=new Thread[localCounts.length];
        
        cnt=0;
        
        long before;
        synchronized (lock) {
            for (int i=0;i<localCounts.length;++i) {
                final int myId=i;
                threads[i]=new Thread(){
                        public void run() {
                            long myCnt=0;
                            for (;;) {
                                synchronized(lock) {
                                    if (cnt==niterations) {
                                        break;
                                    }
                                    myCnt++;
                                    cnt++;
                                }
                            }
                            localCounts[myId]=myCnt;
                        }
                    };
            }
            
            for (Thread t : threads) {
                t.start();
            }
            
            Thread.sleep(1000);
            
            before=System.currentTimeMillis();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        long after=System.currentTimeMillis();
        
        System.out.println("That took "+(after-before)+" ms");
        
        System.out.println("cnt = "+cnt);

        Util.ensureEqual(cnt,niterations);
    }
    
    public static void doit(long nthreads,
                            long niterations) {
        try {
            System.out.println("Doing locks test with nthreads = "+nthreads+
                               ", total niterations = "+niterations+" ...");

            System.out.println("Warmup...");
            runtest(new long[(int)nthreads],niterations);
            
            System.out.println("And now for real...");
            long[] localCounts=new long[(int)nthreads];
            runtest(localCounts,niterations);
            
            System.out.println("Per-thread counts, most to least:");
            Arrays.sort(localCounts);
            for (int i=(int)nthreads;i-->0;) {
                System.out.println("   "+localCounts[i]);
            }
            
            System.out.println("Fairness ratio: "+((double)localCounts[0])/((double)localCounts[localCounts.length-1]));
        
        } catch (Throwable e) {
            throw new Fail(e);
        }
    }
}

