/*
 * SquirtPollcheck.java
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

// There is some controversy about where to squirt pollchecks - before inlining
// or after it?  The benefit of doing it before inlining is that you trivially
// know if the method should have pollchecks.  The benefit of doing it after is
// that you get fewer pollchecks - in particular, method entry pollchecks get
// elided at inlined callsites.  Because we have the ability to tell, on a per-
// instruction basis, if pollchecks are allowed, it makes sense to insert them
// after inlining.  That way, we get fewer overall pollchecks with no (yet known)
// side-effects.

// FIXME: should perhaps have a pollcheck pre-squirter that ensures that we
// know, early on, where we will be planning on inserting pollchecks.  this would
// allow us to make PollCheck into a throwing instruction.

public class SquirtPollcheck extends CodePhase {
    public SquirtPollcheck(Code c) { super(c); }
    
    public void visitCode() {
        // this is a two-step process - first squirt pollchecks at the tops of loops,
        // then squirt them based on a cost function.
        
        LoopCalc loops=code.getLoops();
        for (Header h : loops.loopHeaders()) {
            if (h.first().di().origin().pollcheck()==PollcheckMode.IMPLICIT_POLLCHECKS) {
                h.prepend(
                    new SimpleInst(
                        h.di(),OpCode.PollCheck,
                        Var.VOID,Arg.EMPTY));
            }
        }
        
	// FIXME: instead of just using a state composed of a Double, have a state
	// that includes:
	//
	// - the current balance (budget), which is a double (or float)
	//
	// - an enum which can be in one of the following states:
	// 
	//   NORMAL
	//   PROLOGUE
	//   POST_CALL
	//
	//   if we see a call while in PROLOGUE, immediately generate a pollcheck,
	//   reset the balance and reset the state to NORMAL.
	//
	//   after a call, set the state to POST_CALL.  (so, really, the above
	//   should read: reset the state to POST_CALL)
	//
	//   if we return (BUT NOT THROW) while in POST_CALL, generate a pollcheck.
	//
	//   after any other pollcheck insertion, reset the state to NORMAL in
	//   addition to resetting the balance.
	
	HashMap< Header, Double > budgetAtHead=new HashMap< Header, Double >();
	Worklist worklist=new Worklist();
	
	budgetAtHead.put(code.root(),new Double(Global.pollcheckBudget));
	worklist.push(code.root());
	for (ExceptionHandler eh : code.handlers()) {
	    budgetAtHead.put(eh.target(),new Double(0.0));
	    worklist.push(eh.target());
	}
	
	CostFunction cf = Global.costFunction;
	
	while (!worklist.empty()) {
	    Header h = worklist.pop();
	    double budget = budgetAtHead.get(h);
	    
	    // FIXME: this is squirting WAAY too many pollchecks somehow...
	    for (Operation o : h.operations()) {
                if (o.opcode()==OpCode.PollCheck) {
                    budget=Global.pollcheckBudget;
                } else {
                    if (o.di().origin().pollcheck()==PollcheckMode.IMPLICIT_POLLCHECKS) {
                        if (budget<=0.0) {
                            budget = Global.pollcheckBudget;
                        }
                        budget -= cf.cost(o);
                    }
                }
	    }
	    
	    for (Header h2 : h.normalSuccessors()) {
		Double b2 = budgetAtHead.get(h2);
		if (b2==null || b2>budget) {
		    budgetAtHead.put(h2, budget);
		    worklist.push(h2);
		}
	    }
	}
	
	for (Header h : code.headers()) {
	    double budget=budgetAtHead.get(h);
	    for (Operation o : h.operations()) {
                if (o.opcode()==OpCode.PollCheck) {
                    budget=Global.pollcheckBudget;
                } else {
                    if (o.di().origin().pollcheck()==PollcheckMode.IMPLICIT_POLLCHECKS) {
                        if (budget<=0.0) {
                            o.prepend(
                                new SimpleInst(
                                    o.di(),OpCode.PollCheck,
                                    Var.VOID,Arg.EMPTY));
                            budget = Global.pollcheckBudget;
                            setChangedCode();
                        }
                        budget-=cf.cost(o);
                    }
                }
	    }
	}
	
	if (changedCode()) code.killIntraBlockAnalyses();
    }
}


