/*
 * JDKRuntime.java
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

package com.fiji.fivm.jdk;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.fivmRuntime.*;

public final class JDKRuntime {
    // NOTE: all exports get prefixed with 'FijiJDK_'  we need the additional
    // prefix 'JDK_' to distinguish from other symbols exported by our
    // payload.
    
    @Export
    public static void JDK_monitorWait(Object o,long timeout) {
        waitRelative(o,timeout);
    }
    
    @Export
    public static void JDK_notify(Object o) {
        notify(o);
    }
    
    @Export
    public static void JDK_notifyAll(Object o) {
        notifyAll(o);
    }
    
    @Export
    public static Object JDK_clone(Object o) {
        return Magic.callCloneHelper(o);
    }
    
    @Export
    public static String JDK_intern(String s) {
        return java.lang.fivmSupport.intern(s);
    }
    
    @Export
    public static void JDK_arraycopy(Object src, int srcPos,
                                     Object trg, int trgPos,
                                     int length) {
        ArrayHelper.arraycopy(src,srcPos,trg,trgPos,length);
    }
    
    @Export
    public static void JDK_initProperties(Properties properties) {
        setVMProperties(properties);
        setArgumentProperties(properties);
    }
    
    @Export
    public static void JDK_throwModuleError(Pointer cstr) {
        String str=fromCStringFull(cstr);
        free(cstr);
        throw new UnsatisfiedLinkError(str);
    }
    
    @Export
    public static void JDK_startThread(Thread t) {
        t.start();
    }
    
    @Export
    public static void JDK_stopThread(Thread t) {
        t.stop();
    }
    
    @Export
    public static boolean JDK_isThreadAlive(Thread t) {
        return t.isAlive();
    }
    
    @Export
    public static void JDK_suspendThread(Thread t) {
        t.suspend();
    }
    
    @Export
    public static void JDK_resumeThread(Thread t) {
        t.resume();
    }
    
    @Export
    public static void JDK_setThreadPriority(Thread t, int prio) {
        t.setPriority(prio);
    }
    
    @Export
    public static void JDK_sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
    
    @Export
    public static Thread JDK_currentThread() {
        return Thread.currentThread();
    }
    
    @Export
    public static int JDK_countStackFrames(Thread t) {
        return t.countStackFrames();
    }
    
    @Export
    public static void JDK_interrupt(Thread t) {
        t.interrupt();
    }
    
    @Export
    @NoPollcheck
    @AllowUnsafe
    public static boolean JDK_isInterrupted(Thread t, boolean clearInterrupted) {
        Pointer ts=java.lang.threadStateForThread(t);
        if (fivmr_ThreadState_getInterrupted(ts)) {
            if (clearInterrupted) {
                fivmr_ThreadState_setInterrupted(ts,false);
            }
            return true;
        } else {
            return false;
        }
    }
    
    @Export
    public static boolean JDK_holdsLock(Object o) {
        return Thread.holdsLock(o);
    }
    
    @Export
    public static void JDK_dumpAllStacks() {
        // FIXME
    }
    
    @Export
    public static Thread[] JDK_getAllThreads() {
        throw new fivmError("not implemented");
    }
    
    @Export
    public static Thread[] JDK_dumpThreads(Thread[] threads) {
        throw new fivmError("not implemented");
    }
}

