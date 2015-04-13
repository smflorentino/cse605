/*
 * PointerConst.java
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

public class PointerConst extends Arg.IntConst {
    
    long value;
    
    public PointerConst(long value) {
	this.value=value;
	if (Global.pointerSize==4) {
	    value=(int)value;
	}
    }
    
    public static PointerConst make(long value) {
	if (value==0) {
	    return ZERO;
	} else {
	    return new PointerConst(value);
	}
    }
    
    public static PointerConst make(Pointer value) {
	return make(value.value());
    }
    
    public static PointerConst makeNot(long value) {
        if (Global.pointerSize==8) {
            return new PointerConst(~value);
        } else {
            assert Global.pointerSize<8 && Global.pointerSize>0;
            return new PointerConst((~value)&((1l<<(Global.pointerSize*8))-1l));
        }
    }
    
    public PointerConst negate() {
        return make(-value);
    }
    
    public long longValue() { return value; }
    
    public PointerConst makeSimilar(long value) {
        return make(value);
    }
    
    public long value() { return value; }
    
    public boolean is32() {
        return value>=Integer.MIN_VALUE && value<=Integer.MAX_VALUE;
    }
    
    public int numBits() {
        return Global.pointerSize*8;
    }
    
    public int value32() {
        return (int)value;
    }
    
    public Exectype type() { return Exectype.POINTER; }
    
    public String toString() { return "(pointer)"+value; }
    
    public boolean equals(int value) {
	return (long)value==this.value;
    }
    
    public boolean doesNotEqual(int value) {
	return (long)value!=this.value;
    }
    
    public int structuralHashCode() {
	return 23*(int)value;
    }
    
    public boolean structuralEquals(Arg other) {
	return other instanceof PointerConst
	    && value==((PointerConst)other).value;
    }
    
    public int getArgNioSize() {
        return 1+8;
    }
    
    public void writeArgTo(ByteBuffer buffer) {
        buffer.put((byte)7);
        buffer.putLong(value);
    }

    public static final PointerConst ZERO=new PointerConst(0);
    
    public static final PointerConst PTR_HIGH_MASK=
        PointerConst.makeNot(Global.pointerSize-1);
    
    public static final PointerConst PTR_LOW_MASK=
        PointerConst.make(Global.pointerSize-1);
}

