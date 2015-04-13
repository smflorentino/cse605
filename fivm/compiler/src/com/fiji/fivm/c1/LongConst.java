/*
 * LongConst.java
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

public class LongConst extends Arg.IntConst {
    long value;
    
    public LongConst(long value) {
	this.value=value;
    }
    
    public static LongConst make(long value) {
	if (value==0) {
	    return ZERO;
	} else {
	    return new LongConst(value);
	}
    }
    
    public long longValue() { return value; }
    
    public LongConst makeSimilar(long value) {
        return make(value);
    }
    
    public LongConst negate() {
        return make(-value);
    }
    
    public long value() { return value; }
    
    public boolean is32() {
        return value>=Integer.MIN_VALUE && value<=Integer.MAX_VALUE;
    }
    
    public int numBits() {
        return 64;
    }
    
    public int value32() {
        return (int)value;
    }
    
    public Exectype type() { return Exectype.LONG; }
    
    public String toString() { return "(long)"+value; }
    
    public boolean equals(int value) {
	return (long)value==this.value;
    }
    
    public boolean doesNotEqual(int value) {
	return (long)value!=this.value;
    }
    
    public int structuralHashCode() {
	return 29*(int)value;
    }
    
    public boolean structuralEquals(Arg other) {
	return other instanceof LongConst
	    && value==((LongConst)other).value;
    }
    
    public int getArgNioSize() {
        return 1+8;
    }
    
    public void writeArgTo(ByteBuffer buffer) {
        buffer.put((byte)8);
        buffer.putLong(value);
    }

    public static final LongConst ZERO=new LongConst(0);
}

