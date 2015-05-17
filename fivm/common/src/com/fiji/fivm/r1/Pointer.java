/*
 * Pointer.java
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

package com.fiji.fivm.r1;

/**
 * Represents an unsigned pointer.
 */
@Unboxed
public final class Pointer {
    
    @Intrinsic private Pointer() {}
    
    @Inline @Intrinsic @NoSafepoint public native static Pointer zero();
    
    @Intrinsic @NoSafepoint public native static int size();
    
    @Intrinsic @NoSafepoint public native static Pointer fromObject(Object o);
    
    @Intrinsic @NoSafepoint public native static Pointer fromIntZeroFill(int i);
    @Intrinsic @NoSafepoint public native static Pointer fromIntSignExtend(int i);
    
    @NoSafepoint @NoPollcheck @Inline
    public static Pointer fromInt(int i) {
	return fromIntSignExtend(i);
    }
    
    @Intrinsic public native static Pointer fromLong(long l);
    
    public static Pointer minValue() {
        return zero();
    }
    
    public static Pointer maxValue() {
        return zero().sub(1);
    }
    
    public static Pointer parsePointer(String s) {
        // this is slightly broken.  it will require you to pass negative
        // pointers in 64-bit mode.  ah well.
        
        long value=Long.parseLong(s);
        if (Pointer.size()==4 &&
            (value<minValue().asLong() ||
             value>maxValue().asLong())) {
            throw new NumberFormatException("For input string: \""+s+"\"");
        }
        
        return fromLong(value);
    }
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native boolean loadBoolean();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native byte loadByte();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native short loadShort();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native char loadChar();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native int loadInt();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native long loadLong();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native float loadFloat();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native double loadDouble();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer loadPointer();
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(boolean z);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(byte b);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(short s);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(char c);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(int i);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(long l);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(float f);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(double d);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native void store(Pointer p);
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer neg();

    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer add(Pointer other);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer sub(Pointer other);
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer mul(Pointer other);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer div(Pointer other);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer mod(Pointer other);
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer and(Pointer other);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer or(Pointer other);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer xor(Pointer other);
    
    // NOTE: this is a BitNot
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer not();
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer shl(int other);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer shr(int other);
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Pointer ushr(int other);

    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native boolean lessThan(Pointer other);
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public boolean greaterThan(Pointer other) {
        return other.lessThan(this);
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public boolean lessThanOrEqual(Pointer other) {
        return !greaterThan(other);
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public boolean greaterThanOrEqual(Pointer other) {
        return !lessThan(other);
    }
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native boolean signedLessThan(Pointer other);
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public Pointer add(int other) {
	return add(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public Pointer sub(int other) {
	return sub(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public Pointer mul(int other) {
	return mul(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public Pointer div(int other) {
	return div(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public Pointer mod(int other) {
	return mod(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public boolean lessThan(int other) {
	return lessThan(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public boolean greaterThan(int other) {
	return greaterThan(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public boolean lessThanOrEqual(int other) {
	return lessThanOrEqual(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public boolean greaterThanOrEqual(int other) {
	return greaterThanOrEqual(Pointer.fromInt(other));
    }
    
    @NoSafepoint @NoPollcheck @NoNullCheckOnAccess @Inline
    public boolean signedLessThan(int other) {
	return signedLessThan(Pointer.fromInt(other));
    }

    @Intrinsic @NoSafepoint @NoNullCheckOnAccess @Inline
    public native boolean weakCAS(int comparand,
                                  int value);
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess @Inline
    public native boolean weakCAS(Pointer comparand,
                                  Pointer value);
    
    // FIXME: add other variants of CAS
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native Object asObject();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native String asString();
    
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native int castToInt();
    @Intrinsic @NoSafepoint @NoNullCheckOnAccess public native long asLong();
}


