/*
 * TimeSlice.java
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

import com.fiji.fivm.r1.*;

public final class TimeSlice {
    Object lock = new Object();
    boolean controllerRunning=false;
    
    Pointer timeSlice;
    
    int priority;
    
    TimeSlice(Pointer timeSlice,
              int priority) {
        this.timeSlice=timeSlice;
        this.priority=priority;
    }
    
    public int getNumThreads() {
        return CType.getPointer(
            CType.getPointer(timeSlice,
                             "fivmr_TimeSlice",
                             "pool"),
            "fivmr_ThreadPool",
            "nthreads").castToInt();
    }
    
    public void spawnOneShot(final VMController vmctrl_,
                             final VMConfiguration config) {
        config.setTimeSlice(this);
        config.setMaxVMThreads(getNumThreads());
        spawnController(new Runnable(){
                @StackAllocation
                public void run() {
                    VMController vmctrl=vmctrl_;
                    if (vmctrl==null) {
                        vmctrl=new VMController();
                    }
                    vmctrl.spawn(config);
                }
            });
    }
    
    public void spawnOneShot(VMConfiguration config) {
        spawnOneShot(null,config);
    }
    
    public void spawnAutoRestart(final VMController vmctrl_,
                                 final VMConfiguration config) {
        config.setTimeSlice(this);
        config.setMaxVMThreads(getNumThreads());
        spawnController(new Runnable(){
                @StackAllocation
                public void run() {
                    VMController vmctrl=vmctrl_;
                    if (vmctrl==null) {
                        vmctrl=new VMController();
                    }
                    for (;;) {
                        vmctrl.spawn(config);
                    }
                }
            });
    }
    
    public void spawnAutoRestart(VMConfiguration config) {
        spawnAutoRestart(null,config);
    }
    
    public void spawn(VMController vmctrl,
                      VMConfiguration config,
                      SpawnMode mode) {
        switch (mode) {
        case SPAWN_ONE_SHOT:
            spawnOneShot(vmctrl,config);
            break;
        case SPAWN_AUTO_RESTART:
            spawnAutoRestart(vmctrl,config);
            break;
        default: throw new IllegalArgumentException(
            "Illegal value for mode: "+mode);
        }
    }
    
    public void spawn(VMConfiguration config,
                      SpawnMode mode) {
        spawn(null,config,mode);
    }

    @Import
    @GodGiven
    private static native void fivmr_VMThread_startPooledThread(Pointer curTS,
                                                                Pointer pool,
                                                                Runnable runnable,
                                                                int priority);
    
    public void spawnController(final Runnable run) {
        // problems:
        // - this has to be a thread that is time sliced.
        // - but if it's time sliced then we've got GC issues: the GC won't be
        //   able to make progress until all of the slices take turns.
        // - it would be nice if we could make this whole thing work without
        //   having any GC whatsoever.
        // - and it would be even nicer if this was GC-friendly
        // - or, rather, if the GC was suspend-friendly.
        // Current solution: don't use GC.  this isn't a mandate as much as
        // a very strong recommendation.  This code will "work" fine, Most Of The
        // Time, if you use a GC.
        
        synchronized (lock) {
            if (controllerRunning) {
                throw new IllegalStateException(
                    "Can only start one time slice controller at a time");
            }

            controllerRunning=true;
            fivmr_VMThread_startPooledThread(
                Magic.curThreadState(),
                CType.getPointer(timeSlice,"fivmr_TimeSlice","pool"),
                new Runnable() {
                    public void run() {
                        try {
                            run.run();
                        } catch (Throwable e) {
                            System.err.println("Time slice controller experienced an error:");
                            e.printStackTrace();
                            System.exit(1);
                        }
                        controllerRunning=false;
                    }
                },
                priority);
        }
    }
}


