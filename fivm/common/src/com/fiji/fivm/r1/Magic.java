/*
 * Magic.java
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

public class Magic {
    private Magic() {}
    
    @Intrinsic
    @NoSafepoint
    public static native boolean uLessThan(int a,int b);
    
    @Intrinsic
    @NoSafepoint
    public static native Pointer addressOfField(Object o,String fieldName);
    
    @Intrinsic
    @NoSafepoint
    public static native Pointer addressOfStaticField(Class<?> c,String fieldName);
    
    @Intrinsic
    @NoSafepoint
    public static native Pointer offsetOfField(Class<?> c,String fieldName);
    
    /**
     * Returns the address of the given element.  Note that this does not do
     * array bounds checks.
     * @return the address of the ith element.
     */
    @Intrinsic
    @NoSafepoint
    public static native Pointer addressOfElement(Object o,int i);
    
    @Intrinsic
    @NoSafepoint
    public static native Pointer offsetOfElement(Class<?> c,int i);
    
    @Intrinsic
    @NoSafepoint
    public static native Pointer getMethodRec(String descriptor);
    
    @Intrinsic
    @NoSafepoint
    public static native Object getObjField(Object o, String fieldName);
    
    @Intrinsic
    @NoSafepoint
    public static native void putField(Object o,String fieldName,
                                       Object value);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakCAS(Object o, String fieldName,
					 int comparand, int newValue);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakCAS(Object o, String fieldName,
					 Pointer comparand, Pointer newValue);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakCAS(Object o, String fieldName,
					 Object comparand, Object newValue);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakStaticCAS(Class<?> c, String fieldName,
					       int comparand, int newValue);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakStaticCAS(Class<?> c, String fieldName,
					       Pointer comparand, Pointer newValue);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakStaticCAS(Class<?> c, String fieldName,
					       Object comparand, Object newValue);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakCAS(int[] array, int i,
					 int comparand, int newValue);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakCAS(Pointer[] array, int i,
					 Pointer comparand, Pointer newValue);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean weakCAS(Object[] array, int i,
					 Object comparand, Object newValue);

    @Intrinsic
    @NoSafepoint
    public static native Pointer typeDataFor(Object o);

    @Intrinsic
    @NoSafepoint
    public static native Pointer curThreadState();
    
    // NB: inlining really screws us over whenever we use this.  always use NoInline for
    // any method that takes a frame.
    @Intrinsic
    @NoSafepoint
    public static native Pointer curFrame();
    
    @Intrinsic
    @NoSafepoint
    public static native int curAllocSpace();

    @Intrinsic
    @NoSafepoint
    public static native Pointer curAllocFrame();

    @Intrinsic
    @NoSafepoint
    public static native void fence();
    
    @Intrinsic
    @NoSafepoint
    public static native void compilerFence();
    
    @Intrinsic
    @NoPollcheck
    public static native void nullCheck(Object o);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean likely(boolean p);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean unlikely(boolean p);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean semanticallyLikely(boolean p);
    
    @Intrinsic
    @NoSafepoint
    public static native boolean semanticallyUnlikely(boolean p);
    
    @Intrinsic
    @NoSafepoint
    public static native void unlikely();
    
    /**
     * Causes the value passed to this method to be live at this point
     * no matter what.  Also acts as a compiler fence.
     */
    @Intrinsic
    @NoSafepoint
    public static native void hardUse(Object o);
    
    // FIXME: do we need hardUse()'s for other types?  currently no...
    
    @Intrinsic
    @NoSafepoint
    public static native Object eatTypeCheck(Object o);
    
    @Intrinsic
    public static native Object callCloneHelper(Object o);
    
    @NoSafepoint
    @NoPollcheck
    @Intrinsic
    public static native Error notReached();
    
    /**
     * Method that returns the object you pass it.  This is useful for
     * confusing javac.
     */
    @Inline
    public static Object id(Object o) {
        return o;
    }
    
    /**
     * Method that forces confusion in the analysis.  Use this when doing
     * any kind of "reinterpret" cast.
     */
    @Inline
    @NoPollcheck
    public static Object topCast(Object o) {
        return Pointer.fromObject(o).asObject();
    }
    
    @Intrinsic
    public static native void pollcheck();
    
    @Intrinsic
    public static native double fiatLongToDouble(long l);
    
    @Intrinsic
    public static native long fiatDoubleToLong(double d);
    
    @Intrinsic
    public static native float fiatIntToFloat(int i);
    
    @Intrinsic
    public static native int fiatFloatToInt(float f);
    
    @Intrinsic
    public static native float fiatLongToFloat(long l);
    
    @Intrinsic
    public static native int fiatLongToInt(long l);
    
    @Intrinsic
    public static native short fiatLongToShort(long l);
    
    @Intrinsic
    public static native char fiatLongToChar(long l);
    
    @Intrinsic
    public static native byte fiatLongToByte(long l);
    
    @Intrinsic
    public static native boolean fiatLongToBoolean(long l);
    
    @Intrinsic
    public static native Pointer fiatLongToPointer(long l);
    
    @Intrinsic
    public static native double sqrt(double value);
    
    @Intrinsic
    public static native void memcpy(Pointer to,Pointer from,Pointer size);

    /**
     * Tells you which VM you are currently running in.
     * Fiji VM is designed around the notion that there may be multiple virtual
     * machine instances in one address space.  All executing Java code knows
     * which VM it belongs to.  This method performs a very simple (single
     * indirection) lookup and returns a pointer to the VM instance that owns
     * the currently running code.
     */
    @Inline
    @NoPollcheck
    @NoSafepoint
    public static Pointer getVM() {
        return CType.getPointer(Magic.curThreadState(),"fivmr_ThreadState","vm");
    }
    
    /**
     * Returns the Payload, which contains the static data used to jump-start
     * the VM.
     */
    @Inline
    @NoPollcheck
    @NoSafepoint
    public static Pointer getPayload() {
        return CType.getPointer(getVM(),"fivmr_VM","payload");
    }
    
    /**
     * Get the Settings object for the current VM.
     */
    @Inline
    @NoPollcheck
    @NoSafepoint
    public static Pointer getSettings() {
        return getVM().add(CType.offsetof("fivmr_VM","settings"));
    }
}

