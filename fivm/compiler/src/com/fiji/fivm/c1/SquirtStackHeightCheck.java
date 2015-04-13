/*
 * SquirtStackHeightCheck.java
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

import com.fiji.fivm.*;

public class SquirtStackHeightCheck extends CodePhase {
    public SquirtStackHeightCheck(Code c) { super(c); }
    
    public void visitCode() {
        if (false) {
            System.out.println("in "+code+": "+code.origin().stackOverflowCheckMode()+
                               ", "+code.origin().origin().stackOverflowCheckMode());
        }
        // FIXME #1: turn this into an extra pollcheck with some magic.  I think we can
        // do this by just (1) manually inserting a pollcheck into throwStackOverflowRTE()
        // using Magic.pollcheck() and adding a check to see if the stack really overflowed,
        // and (2) having triggerBlock make it appear that the stack overflowed by chaging
        // the limit, or something.
        // FIXME #2: consider making this use some manner of protection-based scheme,
        // such as by storing to some large offset to the current stack height...
        if (code.origin().hasSafepoints() && code.origin().hasStackOverflowCheck() &&
            code.getLeafMethodAna().outgoingCallMode()!=OutgoingCallMode.NO_OUTGOING_CALLS) {
            Header h=code.reroot();
            Header oldRoot=h.getFooter().defaultSuccessor();
            
            Var notOverflowed=code.addVar(Exectype.INT);
            Var limit=code.addVar(Exectype.POINTER);
            
            h.append(
                new CFieldInst(
                    h.di(),OpCode.GetCField,
                    limit,new Arg[]{
                        Arg.THREAD_STATE
                    },
                    CTypesystemReferences.ThreadState_stackLimit));
            
            if (Settings.STACK_GROWS_DOWN) {
                h.append(
                    new SimpleInst(
                        h.di(),OpCode.ULessThan,
                        notOverflowed,new Arg[]{
                            limit,
                            Arg.FRAME
                        }));
            } else {
                h.append(
                    new SimpleInst(
                        h.di(),OpCode.ULessThan,
                        notOverflowed,new Arg[]{
                            Arg.FRAME,
                            limit
                        }));
            }
            
            Header throwSO=code.addHeader(h.di());
            
            h.setFooter(
                new Branch(
                    h.di(),OpCode.BranchZero,
                    new Arg[]{notOverflowed},
                    oldRoot,throwSO,
                    BranchPrediction.PREDICTING_FALSE,
                    PredictionStrength.SEMANTIC_PREDICTION));
            
            throwSO.append(
                new MethodInst(
                    h.di(),OpCode.InvokeStatic,
                    Var.VOID,Arg.EMPTY,
                    Runtime.throwStackOverflowRTE));
            
            throwSO.setFooter(
                new Jump(
                    h.di(),oldRoot));
            
            setChangedCode();
            code.killAllAnalyses();
        }
    }
}


