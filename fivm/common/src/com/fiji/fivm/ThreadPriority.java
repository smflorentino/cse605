/*
 * ThreadPriority.java
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

package com.fiji.fivm;

import com.fiji.fivm.r1.CVar;
import com.fiji.fivm.r1.CType;
import com.fiji.fivm.r1.Inline;
import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.RuntimeImport;

public final class ThreadPriority {
    // changed these to constants so that they can be used in case statements.
    public static final int JAVA   = 0x00000000;
    public static final int NORMAL = 0x00010000;
    public static final int RR     = 0x00020000;
    public static final int FIFO   = 0x00030000;
    
    public static final int MAX_PRIORITY = CType.getInt(Magic.getVM(),"fivmr_VM","maxPriority");

    public static final int CRITICAL = CVar.getInt("FIVMR_TPR_CRITICAL");

    public static final int NORMAL_MIN = CVar.getInt("FIVMR_TPR_NORMAL_MIN");
    public static final int NORMAL_MAX = CVar.getInt("FIVMR_TPR_NORMAL_MAX");
    public static final int FIFO_MIN = CVar.getInt("FIVMR_TPR_FIFO_MIN");
    public static final int FIFO_MAX = min(CVar.getInt("FIVMR_TPR_FIFO_MAX"),
                                           MAX_PRIORITY);
    public static final int RR_MIN = CVar.getInt("FIVMR_TPR_RR_MIN");
    public static final int RR_MAX = min(CVar.getInt("FIVMR_TPR_RR_MAX"),
                                         MAX_PRIORITY);
    
    public static final int MIN = CVar.getInt("FIVMR_TPR_MIN"); // equal to NORMAL_MIN

    public static final boolean FAKE_RT = CVar.getBoolean("fivmr_fakeRTPriorities");
    
    @Inline
    public static boolean realTimePrioritiesEnabled() {
        return FAKE_RT || CVar.getBoolean("FIVMR_PR_SUPPORTED");
    }
    
    private static void assertRTEnabled() {
        if (!realTimePrioritiesEnabled()) {
            throw new UnsupportedOperationException(
                "Cannot use real-time priorities because they are either not "+
                "supported on your operating system, or because you don't have "+
                "enough permissions to use them");
        }
    }

    /**
     * Returns the scheduler ID portion of a thread priority.
     *
     * @param  tpr  ThreadPriority to be examined
     * @return      Scheduler ID portion of the specified priority
     */
    public static int scheduler(int tpr) {
	return tpr & 0xffff0000;
    }

    /**
     * Returns the scheduler-specific priority of a thread priority.
     *
     * @param  tpr  ThreadPriority to be examined
     * @return      Priority portion of the specified priority
     */
    public static int priority(int tpr) {
	return tpr & 0x0000ffff;
    }

    /**
     * Raises IllegalArgumentException if the priority can be validated and
     * is invalid.  Otherwise, returns false for a validated priority or
     * true for a priority which cannot be validated.
     *
     * @throws IllegalArgumentException if the priority can be validated and
     *         is invalid
     * @param  tpr  ThreadPriority to be validated
     */
    public static void validate(int tpr) {
	int sched = scheduler(tpr);
	int prio = priority(tpr);
        
	if (sched == JAVA) {
	    if (tpr < Thread.MIN_PRIORITY || tpr > Thread.MAX_PRIORITY)
		throw new IllegalArgumentException("Invalid priority " + prio
						   + " for ThreadPriority.JAVA");
	} else if (sched == NORMAL) {
	    if (tpr < NORMAL_MIN || tpr > NORMAL_MAX)
		throw new IllegalArgumentException("Invalid priority " + prio
						   + " for ThreadPriority.NORMAL");
	} else if (sched == RR) {
            assertRTEnabled();
	    if (tpr < RR_MIN || tpr > RR_MAX)
		throw new IllegalArgumentException("Invalid priority " + prio
						   + " for ThreadPriority.RR");
	} else if (sched == FIFO) {
            assertRTEnabled();
	    if (tpr < FIFO_MIN || tpr > FIFO_MAX)
		throw new IllegalArgumentException("Invalid priority " + prio
						   + " for ThreadPriority.FIFO");
	} else {
            /* Cannot be validated */
            throw new IllegalArgumentException(
                "Cannot validate priority "+prio+"; unrecognized scheduler "+sched);
        }
    }
    
    public static String toString(int prio) {
        String result;
        validate(prio);
        switch (scheduler(prio)) {
        case JAVA: result="Java"; break;
        case NORMAL: result="Normal"; break;
        case FIFO: result="FIFO"; break;
        case RR: result="RR"; break;
        default: throw new Error("totally unexpected priority: "+prio);
        }
        return result+priority(prio);
    }
    
    @RuntimeImport
    private static native boolean fivmr_ThreadPriority_eqRT(int pr1,int pr2);
    
    @RuntimeImport
    private static native boolean fivmr_ThreadPriority_ltRT(int pr1,int pr2);
    
    public static boolean equal(int pr1,int pr2) {
        return fivmr_ThreadPriority_eqRT(pr1,pr2);
    }
    
    public static boolean lessThan(int pr1,int pr2) {
        return fivmr_ThreadPriority_ltRT(pr1,pr2);
    }
    
    public static boolean greaterThan(int pr1,int pr2) {
        return lessThan(pr2,pr1);
    }
    
    public static boolean lessThanOrEqual(int pr1,int pr2) {
        return !greaterThan(pr1,pr2);
    }
    
    public static boolean greaterThanOrEqual(int pr1,int pr2) {
        return !lessThan(pr1,pr2);
    }
    
    @RuntimeImport
    private static native int fivmr_ThreadPriority_max(int pr1,int pr2);
    
    @RuntimeImport
    private static native int fivmr_ThreadPriority_min(int pr1,int pr2);
    
    public static int max(int pr1,int pr2) {
        return fivmr_ThreadPriority_max(pr1,pr2);
    }
    
    public static int min(int pr1,int pr2) {
        return fivmr_ThreadPriority_min(pr1,pr2);
    }
    
    public static int fromString(String str) {
        int result=-1;
        try {
            if (str.startsWith("Java")) {
                result=JAVA|Integer.parseInt(str.substring(4));
            } else if (str.startsWith("Normal")) {
                result=NORMAL|Integer.parseInt(str.substring(6));
            } else if (str.startsWith("FIFO")) {
                result=FIFO|Integer.parseInt(str.substring(4));
            } else if (str.startsWith("RR")) {
                result=RR|Integer.parseInt(str.substring(2));
            } else {
                throw new IllegalArgumentException("Bad priority string: "+str);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad priority string: "+str);
        }
        if (!toString(result).equals(str)) {
            throw new IllegalArgumentException("Bad priority string: "+str);
        }
        return result;
    }

    private ThreadPriority() {
    }
}
