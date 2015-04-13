/*
 * EvilClinit3.java
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

public class EvilClinit3 {
    static Runnable r;
    
    static class evil implements Runnable {
        static int x;
        
        static {
            try {
                synchronized (EvilClinit3.class) {
                    r=new evil();
                    EvilClinit3.class.notifyAll();
                }
                System.out.println("Runnable created.");
                Thread.sleep(1000);
                x=42;
                System.out.println("Evil initialized.");
            } catch (Throwable e) {
                Util.fail(e);
            }
        }
        
        public void run() {
            if (x!=42) {
                throw new Error("saw wrong value of x");
            }
            System.out.println("Running in evil.");
            System.out.println("I ran, x = "+x);
        }
    }
    
    public static void main(String[] v) {
        new Thread() {
            public void run() {
                try {
                    synchronized (EvilClinit3.class) {
                        while (r==null) {
                            EvilClinit3.class.wait();
                        }
                    }
                    System.out.println("Calling run()");
                    r.run();
                    Util.fail("run() ran successfully.");
                } catch (NoClassDefFoundError e) {
                    System.out.println("Thread caught "+e);
                    e.printStackTrace(System.out);
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }.start();
        try {
            System.out.println("x should be = "+evil.x);
            Util.fail("evil.x returned successfully.");
        } catch (ExceptionInInitializerError e) {
            System.out.println("Main method caught "+e);
            e.printStackTrace(System.out);
        }
    }
}

