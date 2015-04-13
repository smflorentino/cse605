/*
 * IntConst.java
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

package com.fiji.fivm.c1;

import java.nio.*;

public class IntConst extends Arg.IntConst {
    
    int value;
    
    public IntConst(int value) {
	this.value=value;
    }
    
    public static IntConst make(int value) {
	switch (value) {
	case 0: return ZERO;
	case 1: return ONE;
	default: return new com.fiji.fivm.c1.IntConst(value);
	}
    }
    
    public static IntConst make(boolean value) {
	if (value) {
	    return ONE;
	} else {
	    return ZERO;
	}
    }
    
    public IntConst negate() {
        return make(-value);
    }
    
    public long longValue() { return value; }
    
    public IntConst makeSimilar(long value) {
        return make((int)value);
    }
    
    public int value() { return value; }
    
    public int numBits() {
        return 32;
    }
    
    public boolean is32() {
        return true;
    }
    
    public int value32() {
        return value;
    }
    
    public Exectype type() { return Exectype.INT; }
    
    public String toString() {
	return ""+value;
    }
    
    public boolean equals(int value) {
	return value==this.value;
    }
    
    public boolean doesNotEqual(int value) {
	return value!=this.value;
    }
    
    public int structuralHashCode() {
	return 29*value;
    }
    
    public boolean structuralEquals(Arg other) {
	return other instanceof com.fiji.fivm.c1.IntConst
	    && value==((com.fiji.fivm.c1.IntConst)other).value;
    }
    
    int getArgNioSize() {
        return 1+4;
    }
    
    void writeArgTo(ByteBuffer buffer) {
        buffer.put((byte)6);
        buffer.putInt(value);
    }

    public static IntConst ZERO=new com.fiji.fivm.c1.IntConst(0);
    public static IntConst ONE=new com.fiji.fivm.c1.IntConst(1);
}


