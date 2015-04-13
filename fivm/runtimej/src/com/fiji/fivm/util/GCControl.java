/*
 * GCControl.java
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

package com.fiji.fivm.util;

import com.fiji.fivm.r1.*;
import com.fiji.fivm.*;

@UsesMagic
public class GCControl {
    
    private GCControl() {}

    @Import
    @GodGiven
    @NoThrow
    private static native boolean fivmr_GC_setMaxHeap(Pointer gc,
                                                      long bytes);
    
    @Import
    @GodGiven
    @NoThrow
    private static native boolean fivmr_GC_setTrigger(Pointer gc,
                                                      long bytes);
    
    @Import
    @GodGiven
    @NoThrow
    private static native void fivmr_GC_asyncCollect(Pointer gc);
    
    @Import
    @GodGiven
    @NoThrow
    private static native void fivmr_GC_setPriority(Pointer gc,
                                                    int prio);
    
    public static void setMaxHeap(long bytes) {
        if (!fivmr_GC_setMaxHeap(MM.getGC(),bytes)) {
            throw new fivmError(
                "Cannot resize heap to "+bytes+" bytes; already using more "+
                "than that.");
        }
    }
    
    public static void setTrigger(long bytes) {
        if (bytes/CVar.getLong("FIVMR_PAGE_SIZE") <= 0) {
            throw new fivmError(
                "Could not change the trigger to "+bytes+" bytes because the "+
                "trigger must be at least "+CVar.getLong("FIVMR_PAGE_SIZE")+" bytes.");
        }
        if (!fivmr_GC_setTrigger(MM.getGC(),bytes)) {
            throw new fivmError(
                "Could not change the trigger to "+bytes+" bytes due to an "+
                "unknown error.");
        }
    }
    
    public static void triggerNow() {
        fivmr_GC_asyncCollect(MM.getGC());
    }
    
    public static void setPriority(int priority) {
        ThreadPriority.validate(priority);
        fivmr_GC_setPriority(MM.getGC(),priority);
    }

    public static int getPriority() {
	return CType.getInt(MM.getGC(),"fivmr_GC","threadPriority");
    }
}

