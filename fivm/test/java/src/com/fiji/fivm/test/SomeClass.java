/*
 * SomeClass.java
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

import com.fiji.fivm.r1.Reflect;

@Reflect
public class SomeClass {
    @Reflect
    static public int snumber=43;
    
    @Reflect
    static public long slnumber=65l;
    
    @Reflect
    static public float sfnumber=5.5f;
    
    @Reflect
    static public double sdnumber=10.4;
    
    @Reflect
    static public Object sobject="bar";

    @Reflect
    public int number;
    
    @Reflect
    public long lnumber;
    
    @Reflect
    public float fnumber;
    
    @Reflect
    public double dnumber;
    
    @Reflect
    public Object object;
    
    @Reflect
    public SomeClass() {
	number=42;
        lnumber=65l;
        fnumber=5.4f;
        dnumber=10.3;
        object="foo";
    }
    
    @Reflect
    public SomeClass(boolean b) {
        this();
	if (!b) {
	    number=53;
	} else {
	    number=64;
	}
    }
    
    @Reflect
    public int method(int arg) {
	return number+arg;
    }
}


