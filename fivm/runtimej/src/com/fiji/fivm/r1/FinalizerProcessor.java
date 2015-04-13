/*
 * FinalizerThread.java
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

import com.fiji.fivm.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

public final class FinalizerProcessor {
    Object[] objCell=new Object[1];

    public FinalizerProcessor() {
        reset();
    }
    
    private void reset() {
        objCell[0]=null;
    }

    public boolean processOneObject(boolean wait) {
        if (!Settings.FINALIZATION_SUPPORTED) {
            return false;
        }
        try {
            boolean result=fivmr_GC_getNextDestructor(MM.getGC(),
                                                      objCell,
                                                      wait);
            if (Settings.ASSERTS_ON && wait && !result) {
                throw new fivmError("getNextDestructor returned false with wait==true");
            }
            
            if (!result) {
                return false;
            }
            
            if (Settings.ASSERTS_ON && objCell[0]==null) {
                throw new fivmError("getNextDestructor gave a null object");
            }
            
            if (logLevel>=2) {
                try {
                    log(FinalizerThread.class,2,"Finalizing "+objCell[0]);
                } catch (Throwable e) {}
            }
            try {
                java.lang.fivmSupport.finalize(objCell[0]);
            } catch (Throwable e) {
                // log finalizer failure
                e.printStackTrace();
            }
            
            return true;
        } finally {
            reset();
        }
    }
    
    public int processAllAvailable() {
        if (!Settings.FINALIZATION_SUPPORTED) {
            return 0;
        }
        for (int cnt=0;;cnt++) {
            if (!processOneObject(false)) {
                return cnt;
            }
        }
    }
    
    public int processNextBatchAndLog(boolean wait) {
        if (!Settings.FINALIZATION_SUPPORTED) {
            return 0;
        }
        boolean result=processOneObject(wait);
        if (Settings.ASSERTS_ON && wait && !result) {
            throw new fivmError("processOneObject(true) return false");
        }
        
        if (!result) {
            return 0;
        }
        
        if (CType.getBoolean(MM.getGC(),"fivmr_GC","logGC")) {
            logPrint("[FP: commencing finalization]\n");
        }

        int count=1+processAllAvailable();
        
        if (CType.getBoolean(MM.getGC(),"fivmr_GC","logGC")) {
            logPrint("[FP: finalized "+count+" objects]\n");
        }
        
        return count;
    }
}
    