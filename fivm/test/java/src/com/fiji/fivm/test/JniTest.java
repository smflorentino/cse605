/*
 * JniTest.java
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

public class JniTest {
    static {
        System.loadLibrary("fijitest");
    }
    
    public static class MyException extends RuntimeException {
        public MyException(String s) {
            super(s);
        }
    }
    
    public static native void simpleTest();
    
    public static synchronized native void simpleSyncTest();
    
    public static native Object simpleRetTest(Object obj);
    
    public static synchronized native Object simpleSyncRetTest(Object obj);
    
    public static native void simpleThrowTest();
    
    public static synchronized native void simpleSyncThrowTest();
    
    public static void main(String[] v) {
        System.out.println("Calling simpleTest()...");
        simpleTest();
        
        System.out.println("Calling simpleSyncTest()...");
        simpleSyncTest();
        
        System.out.println("Calling simpleRetTest()...");
        Object o1=new Object();
        Object o2=simpleRetTest(o1);
        Util.ensureIdentical(o1,o2);
        
        System.out.println("Calling simpleSyncRetTest()...");
        o1=new Object();
        o2=simpleSyncRetTest(o1);
        Util.ensureIdentical(o1,o2);
        
        System.out.println("Calling simpleThrowTest()...");
        try {
            simpleThrowTest();
        } catch (MyException e) {
            Util.ensureEqual(e.getClass(),MyException.class);
            Util.ensureEqual(e.getMessage(),
                             "Hi, I'm an exception, thrown from native code!");
            e.printStackTrace(System.out);
        }
        
        System.out.println("Calling simpleSyncThrowTest()...");
        try {
            simpleSyncThrowTest();
        } catch (MyException e) {
            Util.ensureEqual(e.getClass(),MyException.class);
            Util.ensureEqual(e.getMessage(),
                             "Hi, I'm an exception, thrown from native synchronized code!");
            e.printStackTrace(System.out);
        }
        
        System.out.println("That worked!");
    }
}

