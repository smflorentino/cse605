/*
 * LowerExceptions3.java
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
import com.fiji.fivm.Settings;

public class LowerExceptions3 extends CodePhase {
    public LowerExceptions3(Code c) { super(c); }
    
    HashMap< ExceptionHandler, Header > targets=
	new HashMap< ExceptionHandler, Header >();
    public void visitCode() {
	// what this needs to do:
	// Throw: put the exception into THREAD_STATE->curException and jump or return
	// Rethrow: jump or return
	// GetException: fetch the exception from THREAD_STATE->curException
	// ClearException: clear THREAD_STATE->curException
	// CheckException: branch out if there was an exception
	// also ... don't forget to kill off all exception handlers
	
	Header returner=code.addHeader(code.root().di());
	returner.setFooter(
	    new Terminal(
		code.root().di(),OpCode.Return,
		produceZero(code.result(),returner.getFooter())));
	
	targets.put(null,returner);
	for (ExceptionHandler eh : code.handlers()) targets.put(eh,eh.target());
	
	for (Header h : code.headers2()) {
	    for (Instruction i : h.instructions2()) {
		switch (i.opcode()) {
		case GetException: {
		    Var ePtrVar=code.addVar(Exectype.POINTER,
                                            ((TypeInst)i).getExectype());
		    i.prepend(
			new CFieldInst(
			    i.di(),OpCode.GetCField,
			    ePtrVar,new Arg[]{Arg.THREAD_STATE},
			    CTypesystemReferences.ThreadState_curException));
		    i.prepend(
			new TypeInst(
			    i.di(),OpCode.Cast,
			    i.lhs(),new Arg[]{ePtrVar},
			    ((TypeInst)i).getType()));
		    i.remove();
		    break;
		}
		case ClearException: {
		    i.prepend(
			new CFieldInst(
			    i.di(),OpCode.PutCField,
			    Var.VOID,
			    new Arg[]{
				Arg.THREAD_STATE,
				PointerConst.ZERO
			    },
			    CTypesystemReferences.ThreadState_curException));
		    i.remove();
		    break;
		}
		case CheckException: {
		    Header cont=h.split(i);
		    Var ePtrVar=code.addVar(Exectype.POINTER);
		    h.append(
			new CFieldInst(
			    i.di(),OpCode.GetCField,
			    ePtrVar,new Arg[]{Arg.THREAD_STATE},
			    CTypesystemReferences.ThreadState_curException));
		    h.setFooter(
			new Branch(
			    i.di(),OpCode.BranchNonZero,
			    new Arg[]{ePtrVar},
			    cont,targets.get(h.handler()),
			    BranchPrediction.PREDICTING_FALSE,
			    PredictionStrength.SEMANTIC_PREDICTION));
		    i.remove();
		}
		default: break;
		}
	    }
	    Footer f=h.getFooter();
	    if (f.opcode()==OpCode.Throw || f.opcode()==OpCode.Rethrow) {
		if (f.opcode()==OpCode.Throw) {
		    Var ePtrVar=code.addVar(Exectype.POINTER);
		    f.prepend(
			new TypeInst(
			    f.di(),OpCode.Cast,
			    ePtrVar,new Arg[]{f.rhs(0)},
			    Type.POINTER));
                    if (Settings.INTERNAL_INST) {
                        f.prepend(
                            new CFieldInst(
                                f.di(),OpCode.Call,
                                Var.VOID,new Arg[]{
                                    Arg.THREAD_STATE,
                                    Arg.FRAME,
                                    ePtrVar
                                },
                                Inst.throu));
                    }
		    if (Global.throwDebug) {
                        f.prepend(
                            new DebugIDInfoInst(
                                f.di(),OpCode.SaveDebugID,
                                code,f,Var.VOID));
			f.prepend(
			    new CFieldInst(
				f.di(),OpCode.Call,
				Var.VOID,new Arg[]{Arg.THREAD_STATE,
                                                   ePtrVar},
				CTypesystemReferences.throwException));
		    } else {
			f.prepend(
			    new CFieldInst(
				f.di(),OpCode.PutCField,
				Var.VOID,
				new Arg[]{
				    Arg.THREAD_STATE,
				    ePtrVar
				},
				CTypesystemReferences.ThreadState_curException));
		    }
		}
		h.setFooter(
		    new Jump(f.di(),targets.get(h.handler())));
	    }
	    h.setHandler(null);
	}
	code.handlers.clear();
	
	code.killAllAnalyses();
	setChangedCode();
    }
}

