/*
 * TimeSliceManager.java
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

package com.fiji.mvm;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;

public final class TimeSliceManager {
    Pointer man;
    TimeSlice[] slices;
    int myPrio;
    Object lock=new Object();
    boolean started;
    
    @Import
    @GodGiven
    private static native void fivmr_TimeSliceManager_init(Pointer man,
                                                           Pointer nslices,
                                                           int managerPriority);
    
    @Import
    @GodGiven
    private static native Pointer fivmr_TimeSliceManager_initSliceEasy(Pointer man,
                                                                       Pointer sliceIndex,
                                                                       long duration,
                                                                       Pointer nthreads,
                                                                       int slicePriority);
    
    @Import
    @GodGiven
    private static native void fivmr_TimeSliceManager_start(Pointer man);
    
    public TimeSliceManager(int numTimeSlices,
                            int managerPriority) {
        if (numTimeSlices<=0) {
            throw new IllegalArgumentException(
                "Illegal value of numTimeSlices: "+numTimeSlices);
        }
        
        ThreadPriority.validate(managerPriority);
        
        this.myPrio=managerPriority;
        
        man=fivmRuntime.fivmr_mallocAssert(CType.sizeof("fivmr_TimeSliceManager"));
        slices=new TimeSlice[numTimeSlices];
        fivmr_TimeSliceManager_init(man,
                                    Pointer.fromInt(numTimeSlices),
                                    managerPriority);
    }
    
    public int numSlices() {
        return slices.length;
    }
    
    public void initTimeSlice(int index,
                              long duration,
                              int nthreads,
                              int priority) {
        synchronized (lock) {
            if (index<0 || index>=slices.length) {
                throw new IllegalArgumentException(
                    "Illegal value for index: "+index);
            }
        
            if (duration<=0) {
                throw new IllegalArgumentException(
                    "Illegal value for duration: "+duration);
            }
        
            if (nthreads<=0) {
                throw new IllegalArgumentException(
                    "Illegal value for nthreads: "+nthreads);
            }
        
            if (slices[index]!=null) {
                throw new IllegalStateException(
                    "Slice at index = "+index+" is already initialized");
            }
        
            ThreadPriority.validate(priority);
        
            if (ThreadPriority.lessThan(myPrio,priority)) {
                throw new IllegalArgumentException(
                    "Cannot pass higher priority for time slices than for the manager");
            }
        
            Pointer slicePtr=
                fivmr_TimeSliceManager_initSliceEasy(man,
                                                     Pointer.fromInt(index),
                                                     duration,
                                                     Pointer.fromInt(nthreads),
                                                     priority);
        
            slices[index]=new TimeSlice(slicePtr,priority);
        }
    }
    
    public TimeSlice getTimeSlice(int index) {
        synchronized (lock) {
            if (!started) {
                throw new IllegalStateException(
                    "Cannot access time slices before the time slice manager is started");
            }
            
            if (index<0 || index>=slices.length) {
                throw new IllegalArgumentException(
                    "Illegal value for index: "+index);
            }
            
            if (slices[index]==null) {
                // should never happen
                throw new Error(
                    "Slice at index = "+index+" is not yet initialized");
            }
            
            return slices[index];
        }
    }
    
    public boolean isFullyInitialized() {
        synchronized (lock) {
            for (int i=0;i<slices.length;++i) {
                if (slices[i]==null) {
                    return false;
                }
            }
            return true;
        }
    }
    
    public void start() {
        synchronized (lock) {
            if (started) {
                throw new IllegalStateException(
                    "The time slice manager is already started");
            }
            if (!isFullyInitialized()) {
                throw new IllegalStateException(
                    "The time slice manager is not fully initialized");
            }
            fivmr_TimeSliceManager_start(man);
            started=true;
        }
    }
}

