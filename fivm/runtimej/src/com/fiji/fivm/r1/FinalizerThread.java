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

public final class FinalizerThread extends Thread {
    public FinalizerThread() {
        super("fivmr Finalizer Thread");
        if (!Settings.FINALIZATION_SUPPORTED) {
            throw new fivmError("cannot start finalizer thread if finalization is not supported");
        }
        setDaemon(true);

        log(FinalizerThread.class,1,"Finalizer thread created.");
    }
    
    public static void startFinalizerThread() {
        new FinalizerThread().start();
    }
    
    public void run() {
        try {
            log(FinalizerThread.class,1,"Finalizer thread running.");
            
            FinalizerProcessor fp=new FinalizerProcessor();
            
            for (;;) {
                int result=fp.processNextBatchAndLog(true);
                if (Settings.ASSERTS_ON && result<1) {
                    throw new fivmError("processNextBatchAndLog(true) returned "+result);
                }
            }
        } catch (Throwable e) {
            try {
                e.printStackTrace(System.err);
            } catch (Throwable e2) {
                try {
                    abort("Got an exception in the finalizer thread, and failed to print a stack trace: "+e);
                } catch (Throwable e3) {
                    abort("Got an exception in the finalizer thread, and failed to print a stack trace.");
                }
            }
            try {
                abort("Got an exception in the finalizer thread: "+e);
            } catch (Throwable e2) {
                abort("Got an exception in the finalizer thread.");
            }
            Magic.notReached();
        }
    }
}

