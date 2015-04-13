/*
 * MemoryAreas.java
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

import com.fiji.fivm.Settings;
import com.fiji.fivm.om.*;

@UsesMagic
public class MemoryAreas {
    /**
     * Alloc the scope backing area for a thread (must currently be
     * Thread.currentThread()).
     */
    @Inline
    public static void allocScopeBacking(Pointer ts, long size) {
	fivmr_ScopeBacking_alloc(ts, size);
    }
    
    public static void allocScopeBacking(long size) {
        allocScopeBacking(Magic.curThreadState(),size);
    }
    
    /**
     * Allocate a MemoryArea from the current thread's scope backing.
     */
    @Inline
    public static Pointer alloc(long size, boolean shared, String name) {
	Pointer area = fivmr_MemoryArea_alloc(Magic.curThreadState(),
					      size, shared?1:0, name);
	if (area == Pointer.zero()) {
	    fivmRuntime.throwOutOfMemoryError_inJava();
	}
	return area;
    }

    /**
     * Free the given memory area and its backing
     */
    @Inline
    public static void free(Pointer area) {
	fivmr_MemoryArea_free(Magic.curThreadState(), area);
    }

    /**
     * Set the Java-side tracking object for a fivmr_MemoryArea.
     */
    @Inline
    public static void setJavaArea(Pointer area, Object o) {
	CType.put(area, "fivmr_MemoryArea", "javaArea", Pointer.fromObject(o));
    }

    /**
     * Get the Java-side tracking object from a fivmr_MemoryArea.
     */
    @Inline
    public static Object getJavaArea(Pointer area) {
	return CType.getPointer(area, "fivmr_MemoryArea",
				"javaArea").asObject();
    }

    /**
     * Push a previously allocated area onto the current thread's scope
     * stack.
     */
    @Inline
    public static void push(Pointer area) {
	fivmr_MemoryArea_push(Magic.curThreadState(), area);
    }

    /**
     * Pop the topmost MemoryArea off the current thread's scope stack.
     */
    @Inline
    public static void pop(Pointer area) {
	fivmr_MemoryArea_pop(Magic.curThreadState(), Magic.getVM(), area);
    }

    /**
     * Enter the given MemoryArea (which must be on the current thread's
     * stack) and execute logic.
     */
    @Inline
    public static void enter(Pointer area, Runnable logic) {
	fivmr_MemoryArea_enter(Magic.curThreadState(), area, logic);
    }

    /**
     * Set the given memory area (which must be on the current thread's
     * stack) to the area for allocation.
     */
    @Inline
    public static Pointer setCurrentArea(Pointer area) {
	return fivmr_MemoryArea_setCurrentArea(Magic.curThreadState(), area);
    }

    /**
     * Return the current thread's current allocation area.
     */
    @Inline
    public static Pointer getCurrentArea() {
	Pointer area = CType.getPointer(
	    Magic.curThreadState().add(
		CType.offsetof("fivmr_ThreadState", "gc")),
	    "fivmr_GCData", "currentArea");
	if (area == Magic.curThreadState().add(
		CType.offsetof("fivmr_ThreadState", "gc").add(
		    CType.offsetof("fivmr_GCData", "heapMemoryArea")))) {
	    return getHeapArea();
	} else {
	    return area;
	}
    }

    /**
     * Return the fivmr_MemoryArea associated with an object.
     */
    @Inline
    @NoPollcheck
    public static Pointer areaOf(Object o) {
	Pointer pscopeID = Pointer.fromObject(o).sub(
	    OMData.objectGCOffset()).loadPointer();
	Pointer mask = CVar.getPointer("FIVMR_GC_MARKBITS_MASK");
	Pointer masked = pscopeID.and(mask);
	if (masked == mask) {
	    return getImmortalArea();
	} else if (masked != Pointer.zero()) {
	    return getHeapArea();
	} else {
	    Pointer scopeID = pscopeID.shl(2).loadPointer();
	    if (scopeID.and(Pointer.fromInt(0x1)) == Pointer.zero()) {
		return getStackArea();
	    } else {
		if (Settings.RTSJ_SCOPES) {
		    return pscopeID.shl(2).sub(CType.offsetof("fivmr_MemoryArea", "scopeID_s"));
		} else {
		    return scopeID.xor(Pointer.fromInt(0x1));
		}
	    }
	}
    }

    /**
     * Return the size of a fivmr_MemoryArea's allocation area.
     */
    @Inline
    public static long size(Pointer area) {
	return CType.getPointer(area, "fivmr_MemoryArea", "size").asLong();
    }

    /**
     * Return true if the fivmr_MemoryArea is shared, false otherwise
     */
    @Inline
    public static boolean isShared(Pointer area) {
	return (CType.getPointer(area, "fivmr_MemoryArea", "shared")
		!= Pointer.zero());
    }

    /**
     * Return true if the fivmr_MemoryArea is the current allocation area,
     * false otherwise.
     */
    @Inline
    public static boolean isCurrent(Pointer area) {
	return area == CType.getPointer(Magic.curThreadState().add(
                                            CType.offsetof("fivmr_ThreadState", "gc")),
					"fivmr_GCData", "currentArea");
    }

    /**
     * Return the amount of memory already allocated from a
     * fivmr_MemoryArea's allocation area.
     */
    @Inline
    public static long consumed(Pointer area) {
        return fivmr_MemoryArea_consumed(Magic.curThreadState(),area);
    }

    /**
     * Return the amount of remaining memory available for allocation in
     * a fivmr_MemoryArea's allocation area.
     */
    @Inline
    public static long available(Pointer area) {
	return size(area) - consumed(area);
    }

    /**
     * Return the top of the scope stack for SCJ-style stacks
     */
    @Inline
    public static Pointer top() {
	if (Settings.SCJ_SCOPES) {
	    return CType.getPointer(
		CType.getPointer(Magic.curThreadState()
				 .add(CType.offsetof("fivmr_ThreadState",
						     "gc")),
				 "fivmr_GCData", "scopeStack"),
		"fivmr_MemoryAreaStack", "area");
	} else {
	    return Pointer.zero();
	}
    }

    @Inline
    @NoPollcheck
    public static Pointer getImmortalArea() {
	return fivmr_MemoryArea_getImmortalArea(Magic.curThreadState());
    }

    @Inline
    @NoPollcheck
    public static Pointer getHeapArea() {
	return fivmr_MemoryArea_getHeapArea(Magic.curThreadState());
    }

    @Inline
    @NoPollcheck
    public static Pointer getStackArea() {
	return fivmr_MemoryArea_getStackArea(Magic.curThreadState());
    }

    @SuppressWarnings("unused")
    @Export
    @UseObjectsNotHandles
    @NoExecStatusTransition
    private static void MemoryArea_doRun(Pointer area, Runnable logic)
	throws Throwable {
	try {
	    logic.run();
	} catch (Throwable e) {
	    if (areaOf(e) == area) {
		Pointer parent = CType.getPointer(area, "fivmr_MemoryArea",
						  "parent");
		// This really should not happen in SCJ
		if (Settings.RTSJ && parent == Pointer.zero()) {
		    setCurrentArea(getHeapArea());
		} else {
		    setCurrentArea(parent);
		}
		if (Settings.SCJ) {
		    throw new javax.safetycritical.ThrowBoundaryError();
		} else {
		    throw new javax.realtime.ThrowBoundaryError();
		}
	    } else {
		throw e;
	    }
	}
    }

    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_ScopeBacking_alloc(Pointer ts, long size);

    @RuntimeImport
    @NoSafepoint
    private static native Pointer fivmr_MemoryArea_alloc(Pointer ts, long size,
							 int shared, Object name);

    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_MemoryArea_free(Pointer ts,
						     Pointer area);

    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_MemoryArea_push(Pointer ts,
						     Pointer area);

    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_MemoryArea_pop(Pointer ts, Pointer vm,
						    Pointer area);

    @Import
    @GodGiven
    @UseObjectsNotHandles
    @NoExecStatusTransition
    @NoNativeFrame
    private static native void fivmr_MemoryArea_enter(Pointer ts,
						      Pointer area,
						      Runnable logic);

    @RuntimeImport
    @NoSafepoint
    private static native Pointer fivmr_MemoryArea_setCurrentArea(
	Pointer ts,
	Pointer area);

    @RuntimeImport
    @NoSafepoint
    private static native Pointer fivmr_MemoryArea_getImmortalArea(
	Pointer ts);

    @RuntimeImport
    @NoSafepoint
    private static native Pointer fivmr_MemoryArea_getHeapArea(
	Pointer ts);

    @RuntimeImport
    @NoSafepoint
    private static native Pointer fivmr_MemoryArea_getStackArea(
        Pointer ts);
    
    @RuntimeImport
    @NoSafepoint
    private static native long fivmr_MemoryArea_consumed(Pointer ts,
                                                         Pointer area);

    static {
	if (Settings.FLOW_LOGGING) {
	    Pointer area = fivmr_MemoryArea_getImmortalArea(Magic.curThreadState());
	    FlowLog.log(FlowLog.TYPE_SCOPE, FlowLog.SUBTYPE_IMMORTAL,
			size(area), area.asLong());
	}
    }
}
