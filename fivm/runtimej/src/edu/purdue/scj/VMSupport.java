/*
 * VMSupport.java
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

package edu.purdue.scj;

import com.fiji.fivm.ThreadPriority;
import com.fiji.fivm.Time;
import com.fiji.fivm.Settings;
import com.fiji.fivm.om.OMData;
import com.fiji.fivm.r1.fivmRuntime;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.Export;
import com.fiji.fivm.r1.RuntimeImport;
import com.fiji.fivm.r1.NoExecStatusTransition;
import com.fiji.fivm.r1.UseObjectsNotHandles;

import java.lang.fivmSupport;

public class VMSupport {
    private static BackingStoreID immortalStoreID =
	new BackingStoreID("Immortal");
    private static BackingStoreID heapStoreID = new BackingStoreID("Heap");
    private static BackingStoreID stackStoreID = new BackingStoreID("Stack");

    static {
	if (!Settings.SCJ) {
	    throw new Error("VMSupport requires --scj");
	}
	immortalStoreID.setArea(MemoryAreas.getImmortalArea());
	heapStoreID.setThreadSpecific(BackingStoreID.HEAP);
	stackStoreID.setArea(MemoryAreas.getStackArea());
	MemoryAreas.setJavaArea(MemoryAreas.getImmortalArea(),
				immortalStoreID);
	MemoryAreas.setJavaArea(MemoryAreas.getHeapArea(),
				heapStoreID);
	MemoryAreas.setJavaArea(MemoryAreas.getStackArea(),
				stackStoreID);
    }

    public static void enter(BackingStoreID store, Runnable logic) {
	MemoryAreas.enter(store.getArea(), logic);
    }

    public static BackingStoreID pushScope(long size, boolean shared) {
	Magic.fence();
	Pointer area = MemoryAreas.alloc(size, shared, "Private");
	MemoryAreas.push(area);
	Magic.fence();
	BackingStoreID bsid = (BackingStoreID)MemoryArea_getBSID(area);
	bsid.setArea(area);
	return bsid;
    }

    public static BackingStoreID pushScope(long size) {
	return pushScope(size, false);
    }

    public static void popScope() {
	Pointer area = MemoryAreas.top();
	Magic.fence();
	MemoryAreas.pop(area);
	MemoryAreas.free(area);
	Magic.fence();
    }

    public static Object getNote(BackingStoreID scope) {
	return scope.getNote();
    }

    public static void setNote(BackingStoreID scope, Object note) {
	scope.setNote(note);
    }

    /** Get the portal associated with the scope */
    public static Object getPortal(BackingStoreID bsid) {
	return bsid.getPortal();
    }

    /** Set the portal associated with the scope */
    public static void setPortal(BackingStoreID bsid, Object portal) {
	bsid.setPortal(portal);
    }

    public static BackingStoreID setCurrentArea(BackingStoreID scope) {
	Pointer area = MemoryAreas.setCurrentArea(scope.getArea());
	return (BackingStoreID)MemoryArea_getBSID(area);
    }

    public static BackingStoreID getCurrentArea() {
	return (BackingStoreID) MemoryArea_getBSID(
	    MemoryAreas.getCurrentArea());
    }

    public static BackingStoreID getImmortalArea() {
	return immortalStoreID;
    }

    public static BackingStoreID getHeapArea() {
	return heapStoreID;
    }

    public static BackingStoreID areaOf(Object ref) {
	return (BackingStoreID)MemoryArea_getBSID(MemoryAreas.areaOf(ref));
    }

    public static long getScopeSize(BackingStoreID scope) {
	return MemoryAreas.size(scope.getArea());
    }

    public static long memoryConsumed(BackingStoreID scope) {
	return MemoryAreas.consumed(scope.getArea());
    }

    public static long memoryRemaining(BackingStoreID scope) {
	return MemoryAreas.available(scope.getArea());
    }

    public static int getMinRTPriority() {
	return ThreadPriority.FIFO | ThreadPriority.FIFO_MIN;
    }

    public static int getMaxRTPriority() {
	return ThreadPriority.FIFO | ThreadPriority.FIFO_MAX;
    }
    
    public static void setThreadPriority(Thread t,int priority) {
        t.setPriority(priority);
    }

    public static int delayCurrentThreadAbsolute(long nanos) {
	try {
	    fivmRuntime.sleepAbsolute(nanos);
	} catch (InterruptedException e) {
	    return -1;
	}
	return 0;
    }

    public static void setTotalBackingStore(Thread t, long size) {
        java.lang.fivmSupport.setBackingStoreSize(t,size);
    }
    
    public static void allocBackingStoreNow() {
        java.lang.fivmSupport.allocBackingStoreNow();
    }

    public static long getCurrentTime() {
	return Time.nanoTime();
    }

    public static long getCurrentTimePrecise() {
	return Time.nanoTimePrecise();
    }

    public static long getClockResolution() {
	return fivmr_nanosResolution();
    }

    /* FIXME: Does not appear to be correct for HFGC ... we probably
     * don't care right now, either. */
    public static long sizeOf(Class<?> clazz) {
	Pointer td = fivmSupport.typeDataFromClass(clazz);
	return alignSize(fivmRuntime.fivmr_TypeData_size(td),
			 fivmRuntime.fivmr_TypeData_requiredAlignment(td));
    }

    /* FIXME */
    public static long sizeOfReferenceArray(int length) {
	Pointer td = fivmSupport.typeDataFromClass(Object.class);
	int size = fivmRuntime.fivmr_TypeData_refSize(td);
	return alignSize(OMData.totalHeaderSize() + 4 + size * length,
			 Pointer.size());
    }

    /* FIXME */
    public static long sizeOfPrimitiveArray(int length, Class<?> clazz) {
	int size, align;
	if (clazz == byte.class || clazz == boolean.class) {
	    size = 1;
	} else if (clazz == char.class || clazz == short.class) {
	    size = 2;
	} else if (clazz == int.class || clazz == float.class) {
	    size = 4;
	} else if (clazz == long.class || clazz == double.class) {
	    size = 8;
	} else {
	    throw new IllegalArgumentException("type is not primitive");
	}
	align = (size <= 4) ? Pointer.size() : 8;
	/* This + 4 seems to be due to double alignment requirements, but
	 * I'm not entirely sure it's accurate.  This whole thing needs a
	 * double-check -- but it should be conservative. */
	return alignSize(OMData.totalHeaderSize() + 4, align) +
	    alignSize(size * length, align) + ((size == 8) ? 4 : 0);
    }

    private static int alignSize(int size, int align) {
	return ((size+align-1) & ~(align-1));
    }

    @Export
    @UseObjectsNotHandles
    @NoExecStatusTransition
    private static Object MemoryArea_getBSID(Pointer area) {
	return MemoryAreas.getJavaArea(area);
    }

    @RuntimeImport
    private static native long fivmr_nanosResolution();
}
