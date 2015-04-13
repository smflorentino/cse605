/*
 * LockTest.java
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

public class LockTest {
    public static void main(String[] v) throws Throwable {
        final int nthreads1=Integer.parseInt(v[0]);
        final int nthreads2=Integer.parseInt(v[1]);
        final long niterations=Long.parseLong(v[2]);
        final int prio=Integer.parseInt(v[3]);
        
        double[] results=new double[nthreads2-nthreads1+1];
        
        for (int nthreads=nthreads1;nthreads<=nthreads2;++nthreads) {
            results[nthreads-nthreads1]=doit(nthreads,niterations,prio);
        }
        
        System.out.println("Results:");
        for (double value : results) {
            System.out.println(value);
        }
    }
    
    public static double doit(final long nthreads,
                              final long niterations,
                              final int prio) {
        try {
            final double[] throughputCell=new double[1];
        
            Thread t=new Thread() {
                    public void run() {
                        try {
                            System.out.println("Doing locks test with nthreads = "+
                                               nthreads+", niterations = "+
                                               niterations+" ...");
            
                            final long[] cnt=new long[1];
                            final Thread[] threads=new Thread[(int)nthreads];
        
                            for (int i=0;i<nthreads;++i) {
                                threads[i]=new Thread(){
                                        public void run() {
                                            System.out.println("thread starting...");
                                            int lastpercent=0;
                                            System.out.println(this+": 0%");
                                            for (long j=0;j<niterations;++j) {
                                                synchronized(LockTest.class) {
                                                    cnt[0]++;
                                                }
                                                if (j*100/niterations/10!=(long)lastpercent/10) {
                                                    lastpercent=(int)(j*100/niterations);
                                                    System.out.println(this+": "+lastpercent+"%");
                                                }
                                            }
                                            System.out.println(this+" 100%");
                                        }
                                    };
                            }
        
                            long before=System.currentTimeMillis();
            
                            for (int i=0;i<nthreads;++i) {
                                System.out.println("Starting thread with prio = "+
                                                   ThreadPriority.toString(prio));
                                threads[i].setPriority(prio);
                                threads[i].start();
                            }
                            
                            for (;;) {
                                Thread.sleep(2000);
                                // we done saw it poop at 304980507000
                                System.out.println("At "+System.nanoTime()+": current count: "+cnt[0]);
                                
                                boolean anyAlive=false;
                                for (int i=0;i<nthreads;++i) {
                                    if (threads[i].isAlive()) {
                                        anyAlive=true;
                                    }
                                }
                                
                                if (!anyAlive) {
                                    break;
                                }
                            }
        
                            System.out.println("cnt = "+cnt[0]);
            
                            long after=System.currentTimeMillis();
                            double throughput=
                                (((double)(nthreads*niterations))/(after-before));
                            System.out.println("throughput = "+throughput);
            
                            Util.ensureEqual(cnt[0],nthreads*niterations);
            
                            throughputCell[0]=throughput;
                        } catch (Throwable e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                };
        
            System.out.println("I'll run with prio = "+ThreadPriority.toString(prio+1));
            t.setPriority(prio+1);
            t.start();
            t.join();
        
            return throughputCell[0];
        } catch (Throwable e) {
            throw new Fail(e);
        }
    }
}

