/*
 * DebugAndProfile.java
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

import java.util.*;

import static com.fiji.fivm.r1.fivmRuntime.*;

public class DebugAndProfile {
    private DebugAndProfile() {}
    
    @RuntimeImport
    public static native Pointer fivmr_AllThreadStackTraces_get(Pointer vm);
    
    @RuntimeImport
    public static native void fivmr_AllThreadStackTraces_free(Pointer p);
    
    public static List< ThreadStackTrace > getAllThreadStackTraces() {
	Pointer atstPtr=fivmr_AllThreadStackTraces_get(Magic.getVM());
	try {
	    ArrayList< ThreadStackTrace > atst=new ArrayList< ThreadStackTrace >();
	    for (Pointer tstPtr=CType.getPointer(atstPtr,"fivmr_AllThreadStackTraces","first");
		 tstPtr!=Pointer.zero();
		 tstPtr=CType.getPointer(tstPtr,"fivmr_ThreadStackTrace","next")) {
		ThreadStackTrace tst=new ThreadStackTrace();
		tst.execStatus=CType.getPointer(tstPtr,"fivmr_ThreadStackTrace","execStatus");
		tst.execFlags=CType.getPointer(tstPtr,"fivmr_ThreadStackTrace","execFlags");
		tst.thread=java.lang.fivmSupport.threadForVMThread(
		    fivmr_Handle_get(
			CType.getPointer(tstPtr,"fivmr_ThreadStackTrace","thread")));
		tst.frames=new ArrayList< StackTraceFrame >();
		for (Pointer stfPtr=CType.getPointer(tstPtr,"fivmr_ThreadStackTrace","top");
		     stfPtr!=Pointer.zero();
		     stfPtr=CType.getPointer(stfPtr,"fivmr_StackTraceFrame","next")) {
		    StackTraceFrame stf=new StackTraceFrame();
		    stf.methodRec=CType.getPointer(stfPtr,"fivmr_StackTraceFrame","mr");
		    stf.lineNumber=CType.getInt(stfPtr,"fivmr_StackTraceFrame","lineNumber");
		    tst.frames.add(stf);
		}
		atst.add(tst);
	    }
	    return atst;
	} finally {
	    fivmr_AllThreadStackTraces_free(atstPtr);
	}
    }
}


