/*
 * VMController.java
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

public final class VMController {
    public static enum State {
        VM_IDLE, VM_INITIALIZING, VM_RUNNING, VM_EXITING;
        
        /**
         * Returns whether or not this State corresponds to a spawning
         * VM; i.e. a VM that is either initializing, running, or exiting.
         * @return true if this State corresponds to a spawning VM, false otherwise.
         */
        public boolean isSpawning() {
            return this!=VM_IDLE;
        }
        
        /**
         * Returns whether or not this State corresponds to a VM that is
         * exitable; i.e. a VM that is either initializing or running and has not
         * yet called exit().  Note that an exit call may occur just as you query
         * this state; thus if this returns true then you have no guarantee that
         * the VM will still be exitable when you call exit() or any other method.
         * @return true if this state corresponds to a VM that is exitable, false otheriwse.
         */
        public boolean isExitable() {
            return this==VM_INITIALIZING
                || this==VM_RUNNING;
        }
    }
    
    public interface Observer {
        public void notifyExit(VMController controller,
                               int status);
    }
    
    private static Observer EMPTY_OBSERVER=new Observer(){
            public void notifyExit(VMController controller,
                                   int status) {
                // do nothing
            }
        };
    
    private static Observer FAIL_FAST_OBSERVER=new Observer(){
            public void notifyExit(VMController controller,
                                   int status) {
                if (status!=0) {
                    System.err.println(""+controller+" exited with an error: "+status);
                    System.exit(1);
                }
            }
        };
    
    Object statusLock;
    State state;
    
    Observer observer;
    
    Pointer vmPtr;
    int exitCode;

    @AllocateAsCaller
    public VMController() {
        statusLock=new Object();
        // Allocate the monitor for statusLock immediately, in case we
        // are stack allocating -- this is important for scoped memory
        synchronized (statusLock) {
            observer=EMPTY_OBSERVER;
            state=State.VM_IDLE;
        }
    }
    
    @Import
    @GodGiven
    private static native boolean fivmr_VM_exit(Pointer vm,int exitCode);
    
    @Import
    @GodGiven
    private static native void fivmr_VM_resetSettings(Pointer vm,
                                                      Pointer config);
    
    @Import
    @GodGiven
    private static native void fivmr_VM_useTimeSlice(Pointer vm,
                                                     Pointer ts);
    
    @Import
    @GodGiven
    private static native Pointer fivmr_Payload_copy(Pointer payload);
    
    @Import
    @GodGiven
    private static native boolean fivmr_VM_registerPayload(Pointer vm,
                                                           Pointer payload);
    
    @Import
    @GodGiven
    private static native void fivmr_VM_init(Pointer vm);
    
    @Import
    @GodGiven
    private static native void fivmr_VM_run(Pointer vm,int count,Pointer args);
    
    @Import
    @GodGiven
    private static native void fivmr_VM_shutdown(Pointer vm,Pointer exitCode);

    private void throwSpawnError() {
        throw new IllegalStateException("VMController "+this+" is already spawning");
    }
    
    private void throwInternalStateError() {
        throw new Error("unrecognized state: "+state);
    }
    
    public void setObserver(Observer observer) {
        if (observer==null) {
            this.observer=EMPTY_OBSERVER;
        } else {
            this.observer=observer;
        }
    }
    
    public void makeFailFast() {
        this.observer=FAIL_FAST_OBSERVER;
    }
    
    /**
     * Returns the current state of the VM.
     * @return the current state of the VM.
     */
    public State getState() {
        return state;
    }

    /**
     * Informs you if a VM spawn is in progress.  A VM is "spawning" if we're either
     * about to run it, are running it, or if we're cleaning up after it finished
     * running.
     * @return true if a VM is spawning under this controller, or false otherwise.
     */
    public boolean isSpawning() {
        return state.isSpawning();
    }

    /**
     * Attempts to exit the currently spawning VM.  If this call causes the VM to
     * exit, true is returned.  If the VM was either not yet
     */
    public boolean exit(int exitCode) {
        synchronized (statusLock) {
            switch (state) {
            case VM_IDLE:
            case VM_EXITING:
                return false;
            case VM_INITIALIZING:
                this.exitCode=exitCode;
                state=State.VM_EXITING;
                return true;
            case VM_RUNNING:
                return fivmr_VM_exit(vmPtr,exitCode);
            default:
                throwInternalStateError();
                return false; // make javac happy
            }
        }
    }
    
    // FIXME the current approach of making memory for a VM makes IPC
    // really, really hard.  if the VM dies, the memory goes away...  and that's bad.
    
    @StackAllocation
    public int spawn(VMConfiguration config) {
        synchronized (statusLock) {
            if (state.isSpawning()) {
                throwSpawnError();
            }
            state=State.VM_INITIALIZING;
        }
        vmPtr=MM.indexableStartOfArray(new byte[CType.sizeof("fivmr_VM").castToInt()+7]);
        vmPtr=vmPtr.add(7).and(Pointer.fromIntSignExtend(7).not());
        Pointer payloadPtr=config.getPayload().payloadPtr;
        fivmr_VM_resetSettings(vmPtr,CType.getPointer(payloadPtr,
                                                      "fivmr_Payload",
                                                      "defConfig"));
        Pointer configPtr=vmPtr.add(CType.offsetof("fivmr_VM",
                                                   "config"));
        CType.put(configPtr,"fivmr_Configuration","maxThreads",
                  config.maxThreads);
        TimeSlice ts=config.getTimeSlice();
        if (ts!=null) {
            fivmr_VM_useTimeSlice(vmPtr,ts.timeSlice);
        }
        payloadPtr=fivmr_Payload_copy(payloadPtr);
        // FIXME have a way of registering with a different name
        fivmr_VM_registerPayload(vmPtr,payloadPtr);
        fivmr_VM_init(vmPtr);
        // must create the args as an array of UTF8 strings and pass them down...
        String[] args=config.getArguments();
        Pointer[] argv=new Pointer[args.length];
        for (int i=0;i<args.length;++i) {
            argv[i]=fivmRuntime.getCStringFullStack(args[i]);
        }
        synchronized (statusLock) {
            switch (state) {
            case VM_INITIALIZING:
                state=State.VM_RUNNING;
                break;
            case VM_EXITING:
                break;
            default: throwInternalStateError();
            }
        }
        // at this point the state is either RUNNING or EXITING and it cannot
        // change except if we change it
        switch (state) {
        case VM_RUNNING: {
            fivmr_VM_run(vmPtr,
                         argv.length+1,
                         MM.indexableStartOfArray(argv).sub(Pointer.size()));
            synchronized (statusLock) {
                state=State.VM_EXITING;
            }
            Pointer exitCodePtr=MM.indexableStartOfArray(new int[1]);
            fivmr_VM_shutdown(vmPtr,exitCodePtr);
            exitCode=exitCodePtr.loadInt();
            break;
        }
        case VM_EXITING: {
            fivmr_VM_shutdown(vmPtr,Pointer.zero());
            break;
        }
        default: throwInternalStateError();
        }
        observer.notifyExit(this,exitCode);
        vmPtr=Pointer.zero();
        synchronized (statusLock) {
            state=State.VM_IDLE;
        }
        return exitCode;
    }
    
    public boolean isExitable() {
        synchronized (statusLock) {
            return state.isExitable();
        }
    }
    
    public int getLastResult() {
        return exitCode;
    }
    
    public VM getVM() {
        return null; // FIXME
    }
    
    @RuntimeImport
    private native static int fivmr_numInternalVMThreads();
    
    public static int getMinNumInternalVMThreads() {
        return fivmr_numInternalVMThreads();
    }
}


