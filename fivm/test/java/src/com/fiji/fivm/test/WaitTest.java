/*
 * WaitTest.java
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

import java.util.*;

public class WaitTest {
    static class Mon {
        boolean wakeup;
        int cnt;
    }
    
    public static void main(String[] v) throws Throwable {
        int ncycles  = Integer.parseInt(v[0]);
        int nthreads = Integer.parseInt(v[1]);
        int ncounts  = Integer.parseInt(v[2]);
        
        final Mon m=new Mon();
        
        for (int i=0;i<ncycles;++i) {
            System.out.println("Doing wait test #"+(i+1)+" ...");
            ArrayList< Thread > threads=new ArrayList< Thread >();
            for (int j=0;j<nthreads;++j) {
                threads.add(new Thread(){
                        public void run() {
                            try {
                                for (;;) {
                                    synchronized (m) {
                                        while (m.wakeup==false) {
                                            m.wait();
                                        }
                                        m.wakeup=false;
                                        m.notifyAll();
                                        m.cnt++;
                                    }
                                }
                            } catch (InterruptedException e) {
                                System.out.println("thread done.");
                            } catch (Throwable e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                        }
                    });
            }
            System.out.println("Starting threads...");
            for (Thread t : threads) {
                t.setDaemon(true);
                t.start();
            }
            for (int j=0;j<ncounts;++j) {
                synchronized(m) {
                    while (m.wakeup==true) {
                        m.wait();
                    }
                    m.wakeup=true;
                    m.notifyAll();
                }
            }
            System.out.println("Almost done; sleeping a bit to make things worse...");
            Thread.sleep(1000);
            for (Thread t : threads) {
                t.interrupt();
            }
            for (Thread t : threads) {
                t.join();
            }
            System.out.println("m.cnt = "+m.cnt);
            Util.ensureEqual(m.cnt,ncounts);
            System.out.println("All threads done successfully.");
            m.cnt=0;
        }
        
        System.out.println("Test concluded successfully.");
    }
}


