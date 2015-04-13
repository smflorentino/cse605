/*
 * ArrayHelper.java
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

import com.fiji.fivm.Constants;
import static com.fiji.fivm.r1.Magic.*;
import static com.fiji.fivm.r1.fivmOptions.*;
import static com.fiji.fivm.r1.MM.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

public class ArrayHelper {
    private ArrayHelper() {}
    
    @NoInline
    @NoReturn
    static void throwIndexOutOfBoundsException() {
	throw new IndexOutOfBoundsException();
    }
    
    // FIXME: should turn off array bounds checks and nullchecks here.
    @Inline
    public static void slowSafeObjectArrayCopy(Object[] srcArr,int srcStart,
					       Object[] trgArr,int trgStart,
					       int len) {
	if (srcArr==trgArr &&
	    (trgStart>=srcStart && trgStart<srcStart+len)) {
	    // need to do the copy backwards
	    for (int i=len;i-->0;) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	} else {
	    for (int i=0;i<len;++i) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(long[] srcArr,int srcStart,
                                         long[] trgArr,int trgStart,
                                         int len) {
	if (srcArr==trgArr &&
	    (trgStart>=srcStart && trgStart<srcStart+len)) {
	    // need to do the copy backwards
	    for (int i=len;i-->0;) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	} else {
	    for (int i=0;i<len;++i) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(double[] srcArr,int srcStart,
                                         double[] trgArr,int trgStart,
                                         int len) {
        slowSafeArrayCopy((long[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                          (long[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                          len);
    }
    
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(int[] srcArr,int srcStart,
                                         int[] trgArr,int trgStart,
                                         int len) {
	if (srcArr==trgArr &&
	    (trgStart>=srcStart && trgStart<srcStart+len)) {
	    // need to do the copy backwards
	    for (int i=len;i-->0;) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	} else {
	    for (int i=0;i<len;++i) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(float[] srcArr,int srcStart,
                                         float[] trgArr,int trgStart,
                                         int len) {
        slowSafeArrayCopy((int[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                          (int[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                          len);
    }
    
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(short[] srcArr,int srcStart,
                                         short[] trgArr,int trgStart,
                                         int len) {
	if (srcArr==trgArr &&
	    (trgStart>=srcStart && trgStart<srcStart+len)) {
	    // need to do the copy backwards
	    for (int i=len;i-->0;) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	} else {
	    for (int i=0;i<len;++i) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(char[] srcArr,int srcStart,
                                         char[] trgArr,int trgStart,
                                         int len) {
        slowSafeArrayCopy((short[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                          (short[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                          len);
    }
    
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(byte[] srcArr,int srcStart,
                                         byte[] trgArr,int trgStart,
                                         int len) {
	if (srcArr==trgArr &&
	    (trgStart>=srcStart && trgStart<srcStart+len)) {
	    // need to do the copy backwards
	    for (int i=len;i-->0;) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	} else {
	    for (int i=0;i<len;++i) {
		trgArr[i+trgStart]=srcArr[i+srcStart];
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(boolean[] srcArr,int srcStart,
                                         boolean[] trgArr,int trgStart,
                                         int len) {
        slowSafeArrayCopy((byte[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                          (byte[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                          len);
    }
    
    @NoInline
    @NoSafetyChecks
    private static void callSlowSafeObjectArrayCopy(Object[] srcArr,int srcStart,
						    Object[] trgArr,int trgStart,
						    int len) {
	slowSafeObjectArrayCopy(srcArr,srcStart,trgArr,trgStart,len);
    }
    
    // call this only if you know that the types match
    @Inline
    @NoSafetyChecks
    public static void slowSafeArrayCopy(Object srcArr,int srcStart,
                                         Object trgArr,int trgStart,
                                         int len) {
	// FIXME: change this to be a switch statement over the TypeData
	// (it's an optimization ...)
        if (srcArr instanceof byte[] ||
            srcArr instanceof boolean[]) {
            slowSafeArrayCopy((byte[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                              (byte[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                              len);
        } else if (srcArr instanceof char[] ||
                   srcArr instanceof short[]) {
            slowSafeArrayCopy((short[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                              (short[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                              len);
        } else if (srcArr instanceof int[] ||
                   srcArr instanceof float[]) {
            slowSafeArrayCopy((int[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                              (int[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                              len);
        } else if (srcArr instanceof long[] ||
                   srcArr instanceof double[]) {
            slowSafeArrayCopy((long[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                              (long[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                              len);
        } else {
            slowSafeObjectArrayCopy((Object[])Magic.eatTypeCheck(Magic.topCast(srcArr)),srcStart,
                                    (Object[])Magic.eatTypeCheck(Magic.topCast(trgArr)),trgStart,
                                    len);
        }
    }
    
    @NoInline
    public static void callSlowSafeArrayCopy(Object srcArr,int srcStart,
                                             Object trgArr,int trgStart,
                                             int len) {
        // FIXME: this method should not be NoInline; instead it should
        // make an inline call to slowSafeArrayCopy *if* the types are
        // known, otherwise it should make an out-of-line call.
        slowSafeArrayCopy(srcArr,srcStart,
                          trgArr,trgStart,
                          len);
    }    
    
    @Inline
    @NoPollcheck
    @NoSafetyChecks
    public static void
    fastUnsafeArrayCopyWithBarrierNoPollcheckImpl(Object[] srcArr,int srcStart,
						  Object[] trgArr,int trgStart,
						  int len) {
	for (int i=len;i-->0;) {
	    trgArr[i+trgStart]=srcArr[i+srcStart];
	}
    }

    @Inline
    @NoSafetyChecks
    public static void
    fastUnsafeArrayCopyWithBarrierImpl(Object[] srcArr,int srcStart,
				       Object[] trgArr,int trgStart,
				       int len) {
	if (srcArr==trgArr) {
	    callSlowSafeObjectArrayCopy(srcArr,srcStart,
					trgArr,trgStart,
					len);
	} else {
	    final int blockSize=1024;
	    for (int i=0;i<len;i+=blockSize) {
		fastUnsafeArrayCopyWithBarrierNoPollcheckImpl(
		    srcArr,srcStart+i,
		    trgArr,trgStart+i,
		    Math.min(blockSize,len-i));
	    }
	}
    }
    
    @Reflect
    @Inline
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static void fastUnsafeArrayCopy(Object src,int srcStart,
					   Object trg,int trgStart,
					   int len,
					   Pointer eleSize) {
	Magic.nullCheck(src);
	Magic.nullCheck(trg);
	
	if (len==0) {
	    return;
	}
	
	int srcLen=arrayLength(src);
	int trgLen=arrayLength(trg);

	if (len<0 ||
            srcStart<0 ||
            trgStart<0 ||
	    Magic.uLessThan(srcLen,srcStart+len) || // we require srcStart+len <= srcLen
	    Magic.uLessThan(trgLen,trgStart+len)) { // we require trgStart+len <= trgLen
	    throwIndexOutOfBoundsException();
	}
	
	// in all cases where this gets inlined, this check is statically
	// eliminated.
	if (forceSafeRefArrayCopy() && trg instanceof Object[]) {
	    throw new fivmError("got here in error");
	}
	
        if (!MM.contiguousArray(src) ||
            !MM.contiguousArray(trg)) {
            callSlowSafeArrayCopy(src,srcStart,
                                  trg,trgStart,
                                  len);
        } else if (needCMStoreBarrier() && trg instanceof Object[]) {
	    fastUnsafeArrayCopyWithBarrierImpl((Object[])src,srcStart,
					       (Object[])trg,trgStart,
					       len);
	} else {
	    libc.memcpyOrMove(trg!=src,
                              MM.addressOfElement(trg,trgStart,eleSize),
                              MM.addressOfElement(src,srcStart,eleSize),
			      Pointer.fromInt(len).mul(eleSize));
	}
    }
    
    @Inline
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static void fastUnsafeArrayCloneImpl(Object trg,
                                                Object src,
                                                Pointer eleSize) {
	int len=arrayLength(src);
        
        if (len==0) {
            return;
        }

	// in all cases where this gets inlined, this check is statically
	// eliminated.
	if (forceSafeRefArrayCopy() && trg instanceof Object[]) {
	    throw new fivmError("got here in error");
	}
	
        if (!MM.contiguousArray(src) ||
            !MM.contiguousArray(trg)) {
            slowSafeArrayCopy(src,0,
                              trg,0,
                              len);
        } else if (needCMStoreBarrier() && trg instanceof Object[]) {
	    fastUnsafeArrayCopyWithBarrierImpl((Object[])src,0,
					       (Object[])trg,0,
					       len);
	} else {
	    libc.memcpy(MM.addressOfElement(trg,0,eleSize),
                        MM.addressOfElement(src,0,eleSize),
                        Pointer.fromInt(len).mul(eleSize));
	}
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(boolean[] src) {
        boolean[] trg=new boolean[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(1));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(byte[] src) {
        byte[] trg=new byte[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(1));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(char[] src) {
        char[] trg=new char[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(2));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(short[] src) {
        short[] trg=new short[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(2));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(int[] src) {
        int[] trg=new int[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(4));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(long[] src) {
        long[] trg=new long[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(8));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(float[] src) {
        float[] trg=new float[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(4));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(double[] src) {
        double[] trg=new double[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(8));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(Pointer[] src) {
        Pointer[] trg=new Pointer[src.length];
        fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(Pointer.size()));
        return trg;
    }
    
    @Inline
    @Reflect
    @NoPollcheck
    @AllowUnsafe
    @NoSafetyChecks
    public static Object fastUnsafeArrayClone(Object[] src) {
        Object[] trg=(Object[])
            MM.allocArray(Constants.GC_OBJ_SPACE,
                          fivmr_TypeData_forObject(src),
                          src.length);
        if (forceSafeRefArrayCopy()) {
            slowSafeObjectArrayCopy(trg,0,
                                    src,0,
                                    src.length);
        } else {
            fastUnsafeArrayCloneImpl(trg,src,Pointer.fromInt(Pointer.size()));
        }
        return trg;
    }

    public static void arraycopy(Object src,int srcStart,
                                 Object trg,int trgStart,
                                 int len) {
        Magic.nullCheck(src);
        Magic.nullCheck(trg);
	
	// FIXME: do the type checks in a more compiler-friendly way, so that
	// the compiler knows that we're doing a subtype test, and can thus
	// potentially optimize it away.  This will be a big deal for arraycopy
	// being applied to primitive arrays, as well as arraycopy being used
	// immediately following an array allocation (where the target array,
	// having just been allocated, will have an EXACT type bound even if the
	// class is not final).  But, of course, all of this will only be
	// of use if we choose to inline arraycopy.
	Pointer srcTD=fivmr_TypeData_forObject(src);
	Pointer trgTD=fivmr_TypeData_forObject(trg);
	
	if (!fivmr_TypeData_isArray(srcTD) ||
	    !fivmr_TypeData_isArray(trgTD)) {
	    throw new ArrayStoreException("One of the objects passed to arraycopy is not an array");
	}
	
	int srcLen=arrayLength(src);
	int trgLen=arrayLength(trg);

	if (len>0) {
	    if (!Magic.uLessThan(srcStart,srcLen) ||
		!Magic.uLessThan(trgStart,trgLen) ||
		Magic.uLessThan(srcLen,srcStart+len) ||
		Magic.uLessThan(trgLen,trgStart+len)) {
		throw new IndexOutOfBoundsException("start or end positions are out of bounds");
	    }
	} else if (len<0) {
	    throw new IndexOutOfBoundsException("length is negative");
	}
	
        if (forceSafeRefArrayCopy() &&
            src instanceof Object[] &&
            trg instanceof Object[]) {
	    slowSafeObjectArrayCopy((Object[])src,srcStart,
                                    (Object[])trg,trgStart,
                                    len);
	} else if (fivmr_TypeData_isSubtypeOf(curThreadState(),srcTD,trgTD)) {
            if (!MM.contiguousArray(src) ||
                !MM.contiguousArray(trg)) {
                callSlowSafeArrayCopy(src,srcStart,
                                      trg,trgStart,
                                      len);
            } else if (needCMStoreBarrier() && trg instanceof Object[]) {
		fastUnsafeArrayCopyWithBarrierImpl(
		    (Object[])src,srcStart,
		    (Object[])trg,trgStart,
		    len);
	    } else {
		// NB we use memmove in case the arrays overlap
		Pointer eleSize=Pointer.fromInt(fivmr_TypeData_elementSize(srcTD));
		libc.memcpyOrMove(
		    trg!=src,
                    MM.addressOfElement(trg,trgStart,eleSize),
                    MM.addressOfElement(src,srcStart,eleSize),
		    Pointer.fromInt(len).mul(eleSize));
	    }
	} else if (!forceSafeRefArrayCopy() &&
		   src instanceof Object[] &&
		   trg instanceof Object[]) {
	    slowSafeObjectArrayCopy((Object[])src,srcStart,
                                    (Object[])trg,trgStart,
                                    len);
	} else {
	    throw new ArrayStoreException("Attempt to copy between arrays of different kinds");
	}
    }
}

