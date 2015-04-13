/*
 * PerformRefAlloc.java
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

// NOTE: this must be performed right after SSA.  see comment in RefAlloc.<init>
public class PerformRefAlloc extends CodePhase {
    public PerformRefAlloc(Code c) { super(c); }
    
    public void visitCode() {
	assert code.isSSA();
	
	// prior to this phase we shouldn't have performed these analyses
	code.killRefsLiveAtSafe();
	code.killRefAlloc();
	
	// FIXME: if two variables are related via a Cast or Mov like:
	// a = Cast<T>(b)
	// then we sould coalesce them here.  or something.
	// here's how we do it: we should have a pass at turns each cast between
	// basetype-compatible variables from:
	// a = Cast<T>(b)
	// to:
	// a = Cast<T>(b)
	// b = Cast<T>(a)
	// this is always correct, and if done prior to SSA, will allow The Right
	// Things to happen.
	
        assert !code.hasRefAlloc();
        assert !code.hasRefsLiveAtSafe();
        
	RefAlloc ra=code.getRefAlloc();
	
	if (ra.numRefs()==0) {
	    return;
	}
	
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		if (ra.isRelevantForAssign(i.lhs())) {
		    // what if this code gets killed later on, but the safepoints
		    // still say that the variable is live?  what then?
		    // answer: the variable we're storing is going to be kept
		    // alive no matter what after this point, unless this code
		    // is proven to be unreachable.
                    i.append(
                        new ArgInst(
                            i.di(),OpCode.SaveRef,
                            Var.VOID,new Arg[]{
                                i.lhs()
                            },
                            ra.varAssignment(i.lhs())));
		}
	    }
	}
	
	// NOTE: after this we keep the RefAlloc analysis alive, even though the
	// code can change substantially - even potentially eliminating some of
	// the variables that we've analyzed (or at least renaming them).  that's
	// fine, because the analysis is only used for creating the Frame and
	// the bitmaps that go into the debug ID info.
	
	code.killAllAnalyses();
	setChangedCode();
    }
}


