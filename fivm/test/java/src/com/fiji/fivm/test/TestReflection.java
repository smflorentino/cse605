/*
 * TestReflection.java
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

import java.lang.reflect.*;
import static com.fiji.fivm.test.Util.*;

public class TestReflection {
    
    private TestReflection() {}
    
    public static void main(String[] v) throws Throwable {
        System.out.println("Testing reflection...");
        
        testIntField();
        testLongField();
        testFloatField();
        testDoubleField();
        testObjectField();
        testSimpleInstantiation();
        testComplexInstantiation();
        testMethodCall();
        
        System.out.println("Well, that seemed to work!");
    }
    
    public static void testIntField() throws Throwable {
        SomeClass o=new SomeClass();
        Field f=SomeClass.class.getField("number");
        
        ensureEqual(f.get(o),42);
        ensureEqual(f.getInt(o),42);
        
        f.set(o,15);
        ensureEqual(o.number,15);
        
        f.setInt(o,17);
        ensureEqual(o.number,17);
    }
    
    public static void testLongField() throws Throwable {
        SomeClass o=new SomeClass();
        Field f=SomeClass.class.getField("lnumber");
        
        ensureEqual(f.get(o),65l);
        ensureEqual(f.getLong(o),65l);
        
        f.set(o,15l);
        ensureEqual(o.lnumber,15l);
        
        f.setLong(o,17l);
        ensureEqual(o.lnumber,17l);
    }
    
    public static void testFloatField() throws Throwable {
        SomeClass o=new SomeClass();
        Field f=SomeClass.class.getField("fnumber");
        
        ensureEqual(f.get(o),5.4f);
        ensureEqual(f.getFloat(o),5.4f);
        
        f.set(o,15.0f);
        ensureEqual(o.fnumber,15.0f);
        
        f.setFloat(o,17.0f);
        ensureEqual(o.fnumber,17.0f);
    }
    
    public static void testDoubleField() throws Throwable {
        SomeClass o=new SomeClass();
        Field f=SomeClass.class.getField("dnumber");
        
        ensureEqual(f.get(o),10.3);
        ensureEqual(f.getDouble(o),10.3);
        
        f.set(o,15.0);
        ensureEqual(o.dnumber,15.0);
        
        f.setDouble(o,17.0);
        ensureEqual(o.dnumber,17.0);
    }
    
    public static void testObjectField() throws Throwable {
        SomeClass o=new SomeClass();
        Field f=SomeClass.class.getField("object");
        
        ensureEqual(f.get(o),"foo");
        
        f.set(o,"blah");
        ensureEqual(o.object,"blah");
    }
    
    public static void testSimpleInstantiation() throws Throwable {
        SomeClass o=SomeClass.class.newInstance();
        
        ensureEqual(o.number,42);
        ensureEqual(o.lnumber,65l);
        ensureEqual(o.fnumber,5.4f);
        ensureEqual(o.dnumber,10.3);
        ensureEqual(o.object,"foo");
    }
    
    public static void testComplexInstantiation() throws Throwable {
        Constructor< SomeClass > ctor=
            SomeClass.class.getDeclaredConstructor(boolean.class);
        
        SomeClass o=ctor.newInstance(false);
        
        ensureEqual(o.number,53);
        ensureEqual(o.lnumber,65l);
        ensureEqual(o.fnumber,5.4f);
        ensureEqual(o.dnumber,10.3);
        ensureEqual(o.object,"foo");
        
        o=ctor.newInstance(true);
        
        ensureEqual(o.number,64);
        ensureEqual(o.lnumber,65l);
        ensureEqual(o.fnumber,5.4f);
        ensureEqual(o.dnumber,10.3);
        ensureEqual(o.object,"foo");
    }
    
    public static void testMethodCall() throws Throwable {
        Method meth=SomeClass.class.getDeclaredMethod("method",int.class);
        
        SomeClass o=new SomeClass();
        
        ensureEqual(meth.invoke(o,5),42+5);
    }
}


