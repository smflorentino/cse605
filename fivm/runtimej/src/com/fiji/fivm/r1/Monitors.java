/*
 * Monitors.java
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
import static com.fiji.fivm.r1.fivmRuntime.*;

@UsesMagic
@NoFlowLog
public class Monitors {
    private Monitors() {}
    
    @Inline @NoPollcheck @NoSafepoint
    public static Pointer queued()     { return Pointer.fromInt(1); }

    @Inline @NoPollcheck @NoSafepoint
    public static Pointer unbiased()   { return Pointer.fromInt(32); }

    @Inline @NoPollcheck @NoSafepoint
    public static int     rcShift()    { return 6; }

    @Inline @NoPollcheck @NoSafepoint
    public static int     tidShift()   { return 16; }
    
    @Inline @NoPollcheck @NoSafepoint
    public static Pointer rcMask() {
        return Pointer.fromInt(1).shl(tidShift()-rcShift()).sub(1).shl(rcShift());
    }
    
    @Inline @NoPollcheck @NoSafepoint
    public static Pointer tidMask() {
        return Pointer.fromInt(1).shl(Pointer.size()*8-tidShift()).sub(1)
            .shl(tidShift());
    }
    
    @Inline @NoPollcheck @NoSafepoint
    public static Pointer clear() {
        return Pointer.zero();
    }
    
    @Inline @NoPollcheck @NoSafepoint
    public static Pointer notHeld() {
        return unbiased();
    }
    
    @Inline @NoPollcheck @NoSafepoint
    public static Pointer invalid() {
        return Pointer.fromInt(1).shl(tidShift()).or(unbiased());
    }
    
    @NoInline
    static void lockSlow(Object o) {
        fivmr_Monitor_lock_slow(MM.objectHeader(Pointer.fromObject(o)),
                                Magic.curThreadState());
    }
    
    @NoInline
    static void unlockSlow(Object o) {
        fivmr_Monitor_unlock_slow(MM.objectHeader(Pointer.fromObject(o)),
                                  Magic.curThreadState());
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    private static Pointer monitorData() {
        return Magic.curThreadState().add(CType.offsetof("fivmr_ThreadState","forMonitor"));
    }

    @Inline
    @NoPollcheck // only because we're pointerifying objects
    @AllowUnsafe
    @Reflect
    public static void lock(Object o) {
        if (Settings.PROFILE_MONITOR_HEAVY) {
            fivmr_SPC_incLock();
        }
        
        Pointer curMonitor = MM.readObjectHeader(MM.objectHeader(Pointer.fromObject(o)));
        boolean fastAcquired=false;
        
        if (Settings.BIASED_LOCKING) {
            Pointer curState = CType.getPointer(curMonitor,
                                                "fivmr_Monitor",
                                                "state");
            if (curState.and(rcMask().not()) ==
                Pointer.fromIntZeroFill(
                    CType.getInt(Magic.curThreadState(),"fivmr_ThreadState","lockingId"))) {
                Pointer newState = curState.add(Pointer.fromInt(1).shl(rcShift()));
                if (newState.and(rcMask())!=Pointer.zero()) {
                    CType.put(curMonitor,"fivmr_Monitor","state",
                              newState);
                    fastAcquired=true;
                }
            }
        }

        if (!fastAcquired &&
            CType.weakCAS(curMonitor,"fivmr_Monitor","state",
                          notHeld(),
                          notHeld().or(
                              Pointer.fromIntZeroFill(
                                  CType.getInt(Magic.curThreadState(),
                                               "fivmr_ThreadState","lockingId"))))) {
            if (Settings.PIP_LOCKING) {
                CType.put(curMonitor,"fivmr_Monitor","next",
                          CType.getPointer(monitorData(),"fivmr_MonitorData","holding"));
                CType.put(monitorData(),"fivmr_MonitorData","holding",curMonitor);
            }
            fastAcquired=true;
        }
        
        if (!fastAcquired) {
            Magic.unlikely();
            lockSlow(o);
        } else if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_MONITOR, FlowLog.SUBTYPE_LOCK_FAST,
			MM.objectHeader(Pointer.fromObject(o)).asLong());
	}
    }

    @Inline
    @NoPollcheck
    @AllowUnsafe
    @Reflect
    public static void unlock(Object o) {
        if (Settings.PROFILE_MONITOR_HEAVY) {
            fivmr_SPC_incUnlock();
        }
        
        Pointer curMonitor = MM.readObjectHeader(MM.objectHeader(Pointer.fromObject(o)));
        boolean fastReleased=false;
        
        if (Settings.BIASED_LOCKING) {
            Pointer curState = CType.getPointer(curMonitor,
                                                "fivmr_Monitor",
                                                "state");
            
            Pointer zeroRC = Pointer.fromIntZeroFill(
                CType.getInt(Magic.curThreadState(),"fivmr_ThreadState","lockingId"));
            
            if (curState.and(rcMask().not()) == zeroRC) {
                if (curState != zeroRC) {
                    CType.put(curMonitor,"fivmr_Monitor","state",
                              curState.sub(Pointer.fromInt(1).shl(rcShift())));
                    fastReleased=true;
                }
            }
        }

        if (!fastReleased) {
            boolean attemptFast=true;
            
            Pointer expected=
                notHeld().or(
                    Pointer.fromIntZeroFill(
                        CType.getInt(Magic.curThreadState(),
                                     "fivmr_ThreadState","lockingId")));
            
            if (Settings.PIP_LOCKING) {
                if (CType.getPointer(curMonitor,"fivmr_Monitor","state")
                    == expected) {
                    // FIXME: we could assert structured locking by checking if
                    // holding is equal to our monitor...  that might be a good thing
                    // to do.
                    CType.put(monitorData(),"fivmr_MonitorData","holding",
                              CType.getPointer(curMonitor,"fivmr_Monitor","next"));
                    // FIXME: is this right??  I mean, I think so, but...
                    CType.put(curMonitor,"fivmr_Monitor","next",Pointer.zero());
                } else {
                    attemptFast=false;
                }
            }
            
            if (attemptFast &&
                CType.weakCAS(curMonitor,"fivmr_Monitor","state",
                              expected,
                              notHeld())) {
                fastReleased=true;
            }
        }
        
        if (!fastReleased) {
            Magic.unlikely();
            unlockSlow(o);
        } else if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_MONITOR, FlowLog.SUBTYPE_UNLOCK_FAST,
			MM.objectHeader(Pointer.fromObject(o)).asLong());
	}
    }
    
    @RuntimeImport
    @NoSafepoint
    private static native Pointer fivmr_Object_curHolder(Pointer vm,
                                                         Object o);
    
    /**
     * Attempts to return the current holder of the lock associated with the given
     * object.  Note, this method has a "safe" race that may cause it to return
     * the wrong thread, if a thread that held the lock dies before this call can
     * return, and another thread subsequently takes that thread's ID.  Hence,
     * this method should be used only for debugging or logging with the understanding
     * that it may sometimes be inaccurate.
     */
    public static Thread curHolder(Object o) {
        // NOTE: this is safe ... the GC won't collect Java thread objects unless
        // they're nulled in the Java thread table.  so even if the thread that
        // held the lock dies after the call to Object_curHolder, we will still
        // not die.  but we may return the wrong thread.
        Pointer ts=fivmr_Object_curHolder(Magic.getVM(),o);
        if (ts!=Pointer.zero()) {
            return java.lang.fivmSupport.threadForThreadState(ts);
        } else {
            return null;
        }
    }
    
    @RuntimeImport
    @NoSafepoint
    private static native int fivmr_Object_recCount(Pointer vm,
                                                    Object o);
    
    public static int recCount(Object o) {
        return fivmr_Object_recCount(Magic.getVM(),o);
    }
    
    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_SPC_incLock();
    
    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_SPC_incUnlock();
}


