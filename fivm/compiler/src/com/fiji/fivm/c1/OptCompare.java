/*
 * OptCompare.java
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

public class OptCompare extends CodePhase {
    public OptCompare(Code c) { super(c); }
    
    public void visitCode() {
        assert code.isSSA();
        
        // FIXME: since we're not pruning findInstruction based on single-use,
        // the offlimits thing ends up being potentially boned.  need to think
        // about that shit.
        
        CompareBU bu = new CompareBU() {
                /** EqCompareG := Eq(CompareG(a, b), $0) */
                protected boolean visitEqCompareG(Operation eq,
                                                  Instruction compareG) {
                    eq.rhs[0]=compareG.rhs[0];
                    eq.rhs[1]=compareG.rhs[0];
                    return true;
                }
                /** NeqCompareG := Neq(CompareG(a, b), $0) */
                protected boolean visitNeqCompareG(Operation neq,
                                                   Instruction compareG) {
                    neq.rhs[0]=compareG.rhs[0];
                    neq.rhs[1]=compareG.rhs[0];
                    return true;
                }
                /** LTCompareG1 := Eq(CompareG(a, b), $-1) */
                protected boolean visitLTCompareG1(Operation eq,
                                                   Instruction compareG) {
                    eq.opcode=OpCode.LessThan;
                    eq.rhs[0]=compareG.rhs[0];
                    eq.rhs[1]=compareG.rhs[1];
                    return true;
                }
                /** LECompareG1 := Neq(CompareG(a, b), $1) */
                protected boolean visitLECompareG1(Operation neq,
                                                   Instruction compareG) {
                    neq.opcode=OpCode.LessThan;
                    neq.rhs[0]=compareG.rhs[0];
                    neq.rhs[1]=compareG.rhs[1];
                    return true;
                }
                /** GTCompareG1 := Eq(CompareG(a, b), $1) */
                protected boolean visitGTCompareG1(Operation eq,
                                                   Instruction compareG) {
                    // we want it to return true if a>b or if
                    // either value is NaN.  this is equivalent to
                    // !(a<=b)
                
                    Var tmp=code.addVar(Exectype.INT);
                
                    eq.prepend(
                        new SimpleInst(
                            eq.di(),OpCode.LessThanEq,
                            tmp,compareG.rhs));
                    eq.prepend(
                        new SimpleInst(
                            eq.di(),OpCode.Not,
                            ((Instruction)eq).lhs(),new Arg[]{
                                tmp
                            }));
                    eq.remove();
                    return true;
                }
                /** GECompareG1 := Neq(CompareG(a, b), $-1) */
                protected boolean visitGECompareG1(Operation neq,
                                                   Instruction compareG) {
                    // return true if a>=b or if a or b is NaN  this is equivalent
                    // to !(a<b)
                
                    Var tmp=code.addVar(Exectype.INT);
                
                    neq.prepend(
                        new SimpleInst(
                            neq.di(),OpCode.LessThan,
                            tmp,compareG.rhs));
                    neq.prepend(
                        new SimpleInst(
                            neq.di(),OpCode.Not,
                            ((Instruction)neq).lhs(),new Arg[]{
                                tmp
                            }));
                    neq.remove();
                    return true;
                }
                /** EqCompareL := Eq(CompareL(a, b), $0) */
                protected boolean visitEqCompareL(Operation eq,
                                                  Instruction compareL) {
                    eq.rhs[0]=compareL.rhs[0];
                    eq.rhs[1]=compareL.rhs[1];
                    return true;
                }
                /** NeqCompareL := Neq(CompareL(a, b), $0) */
                protected boolean visitNeqCompareL(Operation neq,
                                                   Instruction compareL) {
                    neq.rhs[0]=compareL.rhs[0];
                    neq.rhs[1]=compareL.rhs[1];
                    return true;
                }
                /** LTCompareL1 := Eq(CompareL(a, b), $-1) */
                protected boolean visitLTCompareL1(Operation eq,
                                                   Instruction compareL) {
                    // we want this to return true if a<b or if either
                    // value is NaN.  this is equivalent to
                    // !(a>=b) or !(b<=a)
                
                    Var tmp=code.addVar(Exectype.INT);
                
                    eq.prepend(
                        new SimpleInst(
                            eq.di(),OpCode.LessThanEq,
                            tmp,new Arg[]{
                                compareL.rhs(1),
                                compareL.rhs(0)
                            }));
                    eq.prepend(
                        new SimpleInst(
                            eq.di(),OpCode.Not,
                            ((Instruction)eq).lhs(),new Arg[]{
                                tmp
                            }));
                    eq.remove();
                    return true;
                }
                /** LECompareL1 := Neq(CompareL(a, b), $1) */
                protected boolean visitLECompareL1(Operation neq,
                                                   Instruction compareL) {
                    // we want this to return true if a<=b or if a or b are NaN
                    // this is equivalent to !(a>b) or !(b<a)
                
                    Var tmp=code.addVar(Exectype.INT);
                
                    neq.prepend(
                        new SimpleInst(
                            neq.di(),OpCode.LessThan,
                            tmp,new Arg[]{
                                compareL.rhs(1),
                                compareL.rhs(0)
                            }));
                    neq.prepend(
                        new SimpleInst(
                            neq.di(),OpCode.Not,
                            ((Instruction)neq).lhs(),new Arg[]{
                                tmp
                            }));
                    neq.remove();
                    return true;
                }
                /** GTCompareL1 := Eq(CompareL(a, b), $1) */
                protected boolean visitGTCompareL1(Operation eq,
                                                   Instruction compareL) {
                    eq.opcode=OpCode.LessThan;
                    eq.rhs[0]=compareL.rhs[1];
                    eq.rhs[1]=compareL.rhs[0];
                    return true;
                }
                /** GECompareL1 := Neq(CompareL(a, b), $-1) */
                protected boolean visitGECompareL1(Operation neq,
                                                   Instruction compareL) {
                    neq.opcode=OpCode.LessThanEq;
                    neq.rhs[0]=compareL.rhs[1];
                    neq.rhs[1]=compareL.rhs[0];
                    return true;
                }
                /** LTCompareG2 := LessThan(CompareG(a, b), $0) */
                protected boolean visitLTCompareG2(Operation lessThan,
                                                   Instruction compareG) {
                    lessThan.rhs[0]=compareG.rhs[0];
                    lessThan.rhs[1]=compareG.rhs[1];
                    return true;
                }
                /** LECompareG2 := LessThanEq(CompareG(a, b), $0) */
                protected boolean visitLECompareG2(Operation lessThanEq,
                                                   Instruction compareG) {
                    lessThanEq.rhs[0]=compareG.rhs[0];
                    lessThanEq.rhs[1]=compareG.rhs[1];
                    return true;
                }
                /** LTCompareL2 := LessThan(CompareL(a, b), $0) */
                protected boolean visitLTCompareL2(Operation lessThan,
                                                   Instruction compareL) {
                    return visitLTCompareL1(lessThan,compareL);
                }
                /** LECompareL2 := LessThanEq(CompareL(a, b), $0) */
                protected boolean visitLECompareL2(Operation lessThanEq,
                                                   Instruction compareL) {
                    return visitLECompareL1(lessThanEq,compareL);
                }
                /** GTCompareG2 := LessThan($0, CompareG(a, b)) */
                protected boolean visitGTCompareG2(Operation lessThan,
                                                   Instruction compareG) {
                    return visitGTCompareG1(lessThan,compareG);
                }
                /** GECompareG2 := LessThanEq($0, CompareG(a, b)) */
                protected boolean visitGECompareG2(Operation lessThanEq,
                                                   Instruction compareG) {
                    return visitGECompareG1(lessThanEq,compareG);
                }
                /** GTCompareL2 := LessThan($0, CompareL(a, b)) */
                protected boolean visitGTCompareL2(Operation lessThan,
                                                   Instruction compareL) {
                    lessThan.rhs[0]=compareL.rhs[1];
                    lessThan.rhs[1]=compareL.rhs[0];
                    return true;
                }
                /** GECompareL2 := LessThanEq($0, CompareL(a, b)) */
                protected boolean visitGECompareL2(Operation lessThanEq,
                                                   Instruction compareL) {
                    lessThanEq.rhs[0]=compareL.rhs[1];
                    lessThanEq.rhs[1]=compareL.rhs[0];
                    return true;
                }
                /** BZCompareG := BranchZero(CompareG(a, b)) */
                protected boolean visitBZCompareG(Operation branchZero,
                                                  Instruction compareG) {
                    Var tmp=code.addVar(Exectype.INT);
                    branchZero.prepend(
                        new SimpleInst(
                            branchZero.di(),OpCode.Eq,
                            tmp,compareG.rhs()));
                    branchZero.opcode=OpCode.BranchNonZero;
                    branchZero.rhs=new Arg[]{tmp};
                    return true;
                }
                /** BNZCompareG := BranchNonZero(CompareG(a, b)) */
                protected boolean visitBNZCompareG(Operation branchNonZero,
                                                   Instruction compareG) {
                    Var tmp=code.addVar(Exectype.INT);
                    branchNonZero.prepend(
                        new SimpleInst(
                            branchNonZero.di(),OpCode.Neq,
                            tmp,compareG.rhs()));
                    branchNonZero.rhs=new Arg[]{tmp};
                    return true;
                }
                /** BZCompareL := BranchZero(CompareL(a, b)) */
                protected boolean visitBZCompareL(Operation branchZero,
                                                  Instruction compareL) {
                    return visitBZCompareG(branchZero,compareL);
                }
                /** BNZCompareL := BranchNonZero(CompareL(a, b)) */
                protected boolean visitBNZCompareL(Operation branchNonZero,
                                                   Instruction compareL) {
                    return visitBNZCompareG(branchNonZero,compareL);
                }
                /** BoolCompareG := Boolify(CompareG(a, b)) */
                protected boolean visitBoolCompareG(Operation boolify,
                                                    Instruction compareG) {
                    return visitNeqCompareG(boolify,compareG);
                }
                /** NotCompareG := Not(CompareG(a, b)) */
                protected boolean visitNotCompareG(Operation not,
                                                   Instruction compareG) {
                    return visitEqCompareG(not,compareG);
                }
                /** BoolCompareL := Boolify(CompareL(a, b)) */
                protected boolean visitBoolCompareL(Operation boolify,
                                                    Instruction compareL) {
                    return visitNeqCompareL(boolify,compareL);
                }
                /** NotCompareL := Not(CompareL(a, b)) */
                protected boolean visitNotCompareL(Operation not,
                                                   Instruction compareL) {
                    return visitEqCompareL(not,compareL);
                }
            };
        
        for (Header h : code.headers()) {
            for (Operation o : h.operations()) {
                if (bu.accept(o)) {
                    setChangedCode();
                }
            }
        }
        
        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
    }
}

