/*
 * LowerPollcheck.java
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

public class LowerPollcheck extends CodePhase {
    public LowerPollcheck(Code c) { super(c); }
    
    public void visitCode() {
	for (Header h : code.headers2()) {
	    for (Instruction i : h.instructions2()) {
		if (i.opcode()==OpCode.PollCheck) {
                    // FIXME: instrumentation!!!!  add it!  I don't want that feature
                    // to bit rot.
                    
                    Var pollPtr=code.addVar(Exectype.POINTER);
                    Var predicate=code.addVar(Exectype.INT);
                    
                    i.prepend(
                        new CFieldInst(
                            i.di(),OpCode.GetCFieldAddress,
                            pollPtr,new Arg[]{ Arg.THREAD_STATE },
                            CTypesystemReferences.ThreadState_pollingUnion));
                    i.prepend(
                        new MemoryAccessInst(
                            i.di(),OpCode.Load,
                            predicate,new Arg[]{ pollPtr },
                            Type.INT));
                    
                    Header cont=h.split(i);
                    i.remove();
                    
                    Header pollSlow=h.makeSimilar(i.di());
		    Var did=code.addVar(Exectype.POINTER);
                    pollSlow.append(
                        new DebugIDInfoInst(
                            i.di(),OpCode.GetDebugID,code,i,did));
                    
                    if (Global.optPollcheck) {
                        for (Var v : code.getRefsLiveAtSafe().forSafe(i)) {
                            pollSlow.append(
                                new ArgInst(
                                    i.di(),OpCode.SaveRef,
                                    Var.VOID,new Arg[]{ v },
                                    code.getRefAlloc().varAssignment(v)));
                        }
                    }
                    
		    pollSlow.append(
			new CFieldInst(
			    i.di(),OpCode.Call,
			    Var.VOID,new Arg[]{Arg.THREAD_STATE,did},
			    CTypesystemReferences.ThreadState_pollcheckSlow));
                    
                    h.setFooter(
                        new Branch(
                            i.di(),OpCode.BranchNonZero,
                            new Arg[]{ predicate },
                            pollSlow, cont,
                            BranchPrediction.PREDICTING_TRUE,
                            PredictionStrength.SEMANTIC_PREDICTION));
                    
                    pollSlow.setFooter(
                        new Jump(
                            i.di(), cont));
                    
		    setChangedCode();
		}
	    }
	}
	if (changedCode()) code.killAllAnalyses();
    }
}

