/*
 * SplitUnhappyBlocks.java
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

package com.fiji.fivm.c1;

import java.util.*;

public class SplitUnhappyBlocks extends CodePhase {
    public SplitUnhappyBlocks(Code c) { super(c); }
    
    public void visitCode() {
	SimpleLivenessCalc slc=code.getSimpleLiveness();
	
	for (Header h : code.headers3()) {
	    if (h.hasHandlers()) {
		boolean sawThrow=false;
		VarSet liveForHandler=slc.liveForHandler(h);
		ArrayList< Operation > splitPoints=new ArrayList< Operation >();
		for (Operation o : h.operations()) {
		    if (code.getThrows().get(o)) {
			sawThrow=true;
		    }
		    if (o instanceof Instruction) {
			Instruction i=(Instruction)o;
			if (liveForHandler.contains(i.lhs()) && sawThrow) {
			    sawThrow=false;
			    splitPoints.add(i);
			}
		    }
		}
		for (int i=splitPoints.size();i-->0;) {
		    Operation splitPoint=splitPoints.get(i);
		    h.split(splitPoint);
		}
		if (!splitPoints.isEmpty()) {
		    setChangedCode();
		}
	    }
	}
	if (changedCode()) {
	    for (Header h : code.headers()) {
		if (h.hasHandlers()) {
		    boolean doesThrow=false;
		    for (Operation o : h.operations()) {
			if (code.getThrows().get(o)) {
			    doesThrow=true;
			    break;
			}
		    }
		    if (!doesThrow) {
			h.setHandler(null);
		    }
		}
	    }
	    code.killAllAnalyses();
	}
    }
}

