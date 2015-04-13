/*
 * LowerExceptions2.java
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

/** Lowers ThrowRTEOnZero into branches and calls to appropriate runtime methods. */
public class LowerExceptions2 extends CodePhase {
    static HashMap< Type, VisibleMethod > rteMethodForType=
	new HashMap< Type, VisibleMethod >();
    
    static {
	rteMethodForType.put(
	    Runtime.arithmeticException.asType(),
	    Runtime.throwArithmeticRTE);
	rteMethodForType.put(
	    Runtime.nullPointerException.asType(),
	    Runtime.throwNullPointerRTE);
	rteMethodForType.put(
	    Runtime.arrayIndexOutOfBoundsException.asType(),
	    Runtime.throwArrayBoundsRTE);
	rteMethodForType.put(
	    Runtime.arrayStoreException.asType(),
	    Runtime.throwArrayStoreRTE);
	rteMethodForType.put(
	    Runtime.negativeArraySizeException.asType(),
	    Runtime.throwNegativeSizeRTE);
	rteMethodForType.put(
	    Runtime.classCastException.asType(),
	    Runtime.throwClassCastRTE);
	rteMethodForType.put(
	    Runtime.incompatibleClassChangeError.asType(),
	    Runtime.throwClassChangeRTE);
	rteMethodForType.put(
	    Runtime.illegalAssignmentError.asType(),
	    Runtime.throwIllegalAssignmentError);
    }

    public LowerExceptions2(Code c) { super(c); }
    
    HashMap< ExceptionHandler, HashMap< Type, Header > > rteMakers=
	new HashMap< ExceptionHandler, HashMap< Type, Header > >();
    
    Header createRTEMaker(DebugInfo di,
			  ExceptionHandler eh,
			  VisibleMethod makerMethod) {
	Header h=code.addHeader(di);
	h.setHandler(eh);
	h.append(
	    new MethodInst(
		di,OpCode.InvokeStatic,
		Var.VOID,new Arg[]{},
		makerMethod));
	// this method [that we're generating a call to] should never return
	return h;
    }
    
    void createRTEMakers(ExceptionHandler eh) {
	DebugInfo di=(eh==null?code.root():eh).di();
	HashMap< Type, Header > makers=new HashMap< Type, Header >();
	for (Map.Entry< Type, VisibleMethod > e : rteMethodForType.entrySet()) {
	    makers.put(e.getKey(),createRTEMaker(di,eh,e.getValue()));
	}
	rteMakers.put(eh,makers);
    }
    
    public void visitCode() {
	if (Global.rteMode==RTEMode.RTE_PER_CATCH) {
	    createRTEMakers(null);
	    for (ExceptionHandler eh : code.handlers()) createRTEMakers(eh);
	}
	
	for (Header h : code.headers2()) {
	    for (Instruction i : h.instructions2()) {
		switch (i.opcode()) {
		case ThrowRTEOnZero: {
                    // FIXME: not sure about this.  this really efs things up.  my
                    // basic blocks end up being super short...
		    Header cont=h.split(i);
		    Header thrower;
		    switch (Global.rteMode) {
		    case RTE_PER_CATCH: {
			thrower=rteMakers.get(h.handler()).get(((TypeInst)i).getType());
			break;
		    }
		    case RTE_PER_THROW: {
			thrower=createRTEMaker(
			    i.di(),h.handler(),
			    rteMethodForType.get(((TypeInst)i).getType()));
			break;
		    }
		    default: throw new Error();
		    }
		    h.setFooter(
			new Branch(
			    i.di(),OpCode.BranchZero,
			    i.rhs(),
			    cont,
			    thrower,
			    BranchPrediction.PREDICTING_FALSE,
			    PredictionStrength.SEMANTIC_PREDICTION));
		    i.remove();
		}
		default: break;
		}
	    }
	}
	
	code.killAllAnalyses();
	setChangedCode();
    }
}

