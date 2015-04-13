/*
 * Timer.java
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

package com.fiji.hardrtj;

import com.fiji.fivm.r1.Import;
import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.NoPollcheck;
import com.fiji.fivm.r1.NoSafepoint;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.RuntimeImport;
import com.fiji.fivm.r1.fivmRuntime;

public class Timer {
    int timerId;
    boolean inited;
    
    @RuntimeImport
    @NoSafepoint
    private static native int rtems_timer_create(int name,
						 Pointer timerPtr);

    @RuntimeImport
    private static native int rtems_timer_delete(int timerId);

    @Import
    private static native int fivmr_RTEMS_timerFireAfter(int timerId,
							 int ticks,
							 Runnable r);
    
    @NoPollcheck
    private void init() {
        HardRT.checkEnabled();
	HardRTError.check(
	    rtems_timer_create(123,Magic.addressOfField(this,"timerId")));
    }
    
    public Timer() {
        HardRT.checkEnabled();
        inited=true;
	init();
	fivmRuntime.log(this,1,"Java-land has a timer: "+timerId);
    }
    
    public void destroy() {
        HardRT.checkEnabled();
	HardRT.withInterruptsDisabled(new Runnable(){
		public void run() {
		    if (timerId!=-1) {
			HardRTError.check(rtems_timer_delete(timerId));
			timerId=-1;
		    }
		}
	    });
    }
    
    public void finalize() {
        if (inited) {
            destroy();
        }
    }
    
    public void fireAfter(int ticks,
			  Runnable r) {
        HardRT.checkEnabled();
	HardRTError.check(
	    fivmr_RTEMS_timerFireAfter(timerId,ticks,r));
    }
}

