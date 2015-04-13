/*
 * HelpArithmetic.java
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

public class HelpArithmetic extends CodePhase {
    public HelpArithmetic(Code c) { super(c); }
    
    void handleDiv(Header h,
                   Instruction i,
                   Arg.IntConst caseOne,
                   Arg.IntConst caseTwo,
                   Arg.IntConst special) {
        Var lhs=i.lhs();
        Var res1=code.addVar(i.lhs().type());
        
        Var pred1=code.addVar(Exectype.INT);
        
        i.prepend(
            new SimpleInst(
                i.di(),OpCode.Eq,
                pred1,new Arg[]{
                    i.rhs(0),
                    caseOne
                }));
        
        Header cont=h.split(i);
        
        Header slow1=h.makeSimilar(i.di());
        Header fast=h.makeSimilar(i.di());
        
        h.setFooter(
            new Branch(
                i.di(),OpCode.BranchNonZero,
                new Arg[]{pred1},
                fast,slow1,
                BranchPrediction.PREDICTING_FALSE));
        
        i.remove();
        fast.append(i);

        if (code.isSSA()) {
            i.setLhs(res1);
            fast.append(
                new SimpleInst(
                    i.di(),OpCode.Ipsilon,
                    lhs,new Arg[]{ res1 }));
        }
        
        fast.setFooter(
            new Jump(
                i.di(),cont));
        
        Var pred2=code.addVar(Exectype.INT);
        
        slow1.append(
            new SimpleInst(
                i.di(),OpCode.Eq,
                pred2,new Arg[]{
                    i.rhs(1),
                    caseTwo
                }));
        
        Header slow2=h.makeSimilar(i.di());
        
        slow1.setFooter(
            new Branch(
                i.di(),OpCode.BranchNonZero,
                new Arg[]{pred2},
                fast,slow2,
                BranchPrediction.PREDICTING_FALSE));
        
        slow2.append(
            new SimpleInst(
                i.di(),code.isSSA()?OpCode.Ipsilon:OpCode.Mov,
                lhs,new Arg[]{ special }));
        
        slow2.setFooter(
            new Jump(
                i.di(),cont));

        if (code.isSSA()) {
            cont.prepend(
                new SimpleInst(
                    i.di(),OpCode.Phi,
                    lhs,new Arg[]{ lhs }));
        }
        
        setChangedCode("helped arithmetic");
    }
    
    public void visitCode() {
        HashSet< Instruction > offlimits=new HashSet< Instruction >();
        
        for (Header h : code.headers2()) {
            for (Instruction i : h.instructions2()) {
                if (!offlimits.contains(i)) {
                    switch (i.opcode()) {
                    case Div:
                        switch (i.rhs(0).type().effectiveBasetype()) {
                        case INT:
                            handleDiv(h,i,
                                      IntConst.make(-2147483647-1),
                                      IntConst.make(-1),
                                      IntConst.make(-2147483647-1));
                            offlimits.add(i);
                            break;
                        case LONG:
                            if (Global.pointerSize==4) {
                                i.prepend(
                                    new CFieldInst(
                                        i.di(),OpCode.Call,
                                        i.lhs(),i.rhs(),
                                        CTypesystemReferences.AH_long_div));
                                i.remove();
                                setChangedCode("helped long arithmetic");
                            } else {
                                handleDiv(h,i,
                                          LongConst.make(-9223372036854775807l),
                                          LongConst.make(-1l),
                                          LongConst.make(-9223372036854775807l));
                                offlimits.add(i);
                            }
                            break;
                        default:
                            break;
                        }
                        break;
                    case Mod:
                        switch (i.rhs(0).type().effectiveBasetype()) {
                        case INT:
                            handleDiv(h,i,
                                      IntConst.make(-2147483647-1),
                                      IntConst.make(-1),
                                      IntConst.make(0));
                            offlimits.add(i);
                            break;
                        case LONG:
                            if (Global.pointerSize==4) {
                                i.prepend(
                                    new CFieldInst(
                                        i.di(),OpCode.Call,
                                        i.lhs(),i.rhs(),
                                        CTypesystemReferences.AH_long_mod));
                                i.remove();
                                setChangedCode("helped long arithmetic");
                            } else {
                                handleDiv(h,i,
                                          LongConst.make(-9223372036854775807l),
                                          LongConst.make(-1),
                                          LongConst.make(0l));
                                offlimits.add(i);
                            }
                            break;
                        case FLOAT:
                            if (Global.nativeBackend) {
                                i.prepend(
                                    new CFieldInst(
                                        i.di(),OpCode.Call,
                                        i.lhs(),i.rhs(),
                                        CTypesystemReferences.AH_float_mod));
                                i.remove();
                                setChangedCode("helped float arithmetic");
                            }
                            break;
                        case DOUBLE:
                            if (Global.nativeBackend) {
                                i.prepend(
                                    new CFieldInst(
                                        i.di(),OpCode.Call,
                                        i.lhs(),i.rhs(),
                                        CTypesystemReferences.AH_double_mod));
                                i.remove();
                                setChangedCode("helped double arithmetic");
                            }
                            break;
                        default:
                            break;
                        }
                        break;
                    case Cast:
                        switch (i.rhs(0).type().effectiveBasetype()) {
                        case FLOAT:
                            switch (((TypeInst)i).getType().effectiveBasetype()) {
                            case INT:
                                i.prepend(
                                    new CFieldInst(
                                        i.di(),OpCode.Call,
                                        i.lhs(),i.rhs(),
                                        CTypesystemReferences.AH_float_to_int));
                                i.remove();
                                setChangedCode("helped cast");
                                break;
                            case LONG:
                                i.prepend(
                                    new CFieldInst(
                                        i.di(),OpCode.Call,
                                        i.lhs(),i.rhs(),
                                        CTypesystemReferences.AH_float_to_long));
                                i.remove();
                                setChangedCode("helped cast");
                                break;
                            default:
                                break;
                            }
                            break;
                        case DOUBLE:
                            switch (((TypeInst)i).getType().effectiveBasetype()) {
                            case INT:
                                i.prepend(
                                    new CFieldInst(
                                        i.di(),OpCode.Call,
                                        i.lhs(),i.rhs(),
                                        CTypesystemReferences.AH_double_to_int));
                                i.remove();
                                setChangedCode("helped cast");
                                break;
                            case LONG:
                                i.prepend(
                                    new CFieldInst(
                                        i.di(),OpCode.Call,
                                        i.lhs(),i.rhs(),
                                        CTypesystemReferences.AH_double_to_long));
                                i.remove();
                                setChangedCode("helped cast");
                                break;
                            default:
                                break;
                            }
                            break;
                        default:
                            break;
                        }
                        break;
                    default:
                        break;
                    }
                }
            }
        }
        
	if (changedCode()) {
	    code.killAllAnalyses();
	}
    }
}


