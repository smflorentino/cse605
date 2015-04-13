/*
 * Simplify.java
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

public class OptPeephole extends CodePhase {
    public OptPeephole(Code c) { super(c); }
    
    private static OpCode invertLT(OpCode op) {
        switch (op) {
        case LessThan:
            return OpCode.LessThanEq;
        case ULessThan:
            return OpCode.ULessThanEq;
        case LessThanEq:
            return OpCode.LessThan;
        case ULessThanEq:
            return OpCode.ULessThan;
        default:
            throw new Error();
        }
    }

    public void visitCode() {
        boolean changed=false;
        String lastChange=null;
        
        // perform simplifications to basic blocks themselves.
        UseCalc uc=new UseCalc(code);
        for (Header h : code.headers()) {
        instFor:
            for (Instruction i : h.instructions()) {
                if (i instanceof HeapAccessInst) {
                    HeapAccessInst hai=(HeapAccessInst)i;
                    if (hai.isInstance() &&
                        hai.rhs(0).equals(0)) {
                        // FIXME: is this logic sufficient?
                        // NB we use ThrowRTEOnZero rather than the more declarative
                        // NullCheck because this might run after LowerFeatures2.
                        // this makes me think that it might be worthwhile to just
                        // kill off NullCheck entirely...  currently its only value
                        // is that it's quicker to insert and requires less memory.
                        hai.prepend(
                            new TypeInst(
                                i.di(),OpCode.ThrowRTEOnZero,
                                Var.VOID,new Arg[]{i.rhs(0)},
                                // FIXME for ef's sake, we shouldn't be doing a
                                // Type.parse ... we have a reference to NPE
                                // somewhere...
                                Type.parse(getContext(),
                                           "Ljava/lang/NullPointerException;").checkResolved()));
                        if (changed=h.notReached(hai)) {
                            lastChange="obviously null heap access";
                        }
                        break instFor;
                    } else if (hai.field() instanceof VisibleField &&
                               !((VisibleField)hai.field()).shouldExist()) {
                        // FIXME this could be really dangerous...  what if
                        // there were checks that we should have done?
                        if (hai.lhs()!=Var.VOID) {
                            hai.prepend(
                                new SimpleInst(
                                    hai.di(),OpCode.Mov,
                                    hai.lhs(),
                                    produceZero(hai.lhs().type().asType(),hai)));
                        }
                        hai.remove();
                        changed=true;
                        lastChange="non-existant field access";
                    }
                } else {
                    String ccResult=CanonicalizeCommutables.canonicalize(i);
                    if (ccResult!=null) {
                        changed=true;
                        lastChange=ccResult;
                    }

                    switch (i.opcode()) {
                    case Eq:
                        if (i.rhs(1).equals(0) && i.rhs(1).type()==Exectype.INT) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Not,
                                    i.lhs(),new Arg[]{ i.rhs(0) }));
                            i.remove();
                            changed=true;
                            lastChange="Eq(a,0) to Not(a)";
                        } else if (i.rhs(0).structuralEquals(i.rhs(1)) &&
                                   !i.rhs(0).type().effectiveBasetype().isFloat) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{ IntConst.make(1) }));
                            i.remove();
                            changed=true;
                            lastChange="Eq(a,a) to true";
                        } else if (i.rhs(0) instanceof Arg.IntConst &&
                                   i.rhs(1) instanceof Arg.IntConst) {
                            Arg.IntConst a=(Arg.IntConst)i.rhs(0);
                            Arg.IntConst b=(Arg.IntConst)i.rhs(1);
                            if (a.longValue()!=b.longValue()) {
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Mov,
                                        i.lhs(),new Arg[]{ IntConst.make(false) }));
                                i.remove();
                                changed=true;
                                lastChange="Eq(a,b) where a!=b to false";
                            }
                        }
                        break;
                    case Neq:
                        if (i.rhs(1).equals(0) && i.rhs(1).type()==Exectype.INT) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Boolify,
                                    i.lhs(),new Arg[]{ i.rhs(0) }));
                            i.remove();
                            changed=true;
                            lastChange="Neq(a,0) to Boolify(a)";
                        } else if (i.rhs(0).structuralEquals(i.rhs(1)) &&
                                   !i.rhs(0).type().effectiveBasetype().isFloat) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{ IntConst.make(0) }));
                            i.remove();
                            changed=true;
                            lastChange="Neq(a,a) to false";
                        } else if (i.rhs(0) instanceof Arg.IntConst &&
                                   i.rhs(1) instanceof Arg.IntConst) {
                            Arg.IntConst a=(Arg.IntConst)i.rhs(0);
                            Arg.IntConst b=(Arg.IntConst)i.rhs(1);
                            if (a.longValue()!=b.longValue()) {
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Mov,
                                        i.lhs(),new Arg[]{ IntConst.make(true) }));
                                i.remove();
                                changed=true;
                                lastChange="Neq(a,b) where a!=b to true";
                            }
                        }
                        break;
                    case ULessThan:
                        if (i.rhs(0).equals(0) && i.rhs(1).doesNotEqual(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{ IntConst.make(1) }));
                            i.remove();
                            changed=true;
                            lastChange="ULessThan(0,~0) to true";
                            break;
                        } else if (i.rhs(1).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{ IntConst.make(0) }));
                            i.remove();
                            changed=true;
                            lastChange="ULessThan(a,0) to false";
                            break;
                        }
                    case LessThan:
                        if (i.rhs(0).structuralEquals(i.rhs(1))) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{ IntConst.make(0) }));
                            i.remove();
                            changed=true;
                            lastChange="[U]LessThan(a,a) to false";
                        }
                        break;
                    case ULessThanEq:
                        if (i.rhs(0).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{ IntConst.make(1) }));
                            i.remove();
                            changed=true;
                            lastChange="ULessThanEq(0,a) to true";
                            break;
                        }
                    case LessThanEq:
                        if (i.rhs(0).structuralEquals(i.rhs(1)) &&
                            !i.rhs(0).isFloat()) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{ IntConst.make(1) }));
                            i.remove();
                            changed=true;
                            lastChange="[U]LessThanEq(a,a) to true";
                        }
                        break;
                    case Not:
                        if (i.prev instanceof Instruction) {
                            Instruction iprev=(Instruction)i.prev;
                            if (iprev.lhs()==i.rhs(0) &&
                                uc.usedOnce(iprev.lhs())) {
                                switch (iprev.opcode()) {
                                case Eq:
                                case Neq: {
                                    i.prepend(
                                        new SimpleInst(
                                            i.di(),(iprev.opcode()==OpCode.Eq ?
                                                    OpCode.Neq : OpCode.Eq),
                                            i.lhs(),new Arg[]{
                                                iprev.rhs(0),
                                                iprev.rhs(1)
                                            }));
                                    i.remove();
                                        
                                    changed=true;
                                    lastChange="Not(Eq/Neq) to Neq/Eq";
                                    break;
                                }
                                case LessThan:
                                case ULessThan:
                                case LessThanEq:
                                case ULessThanEq: {
                                    if (iprev.rhs(0).type().isInteger()) {
                                        // !(a<b) = a>=b
                                        //        = b<=a
                                        i.prepend(
                                            new SimpleInst(
                                                i.di(),invertLT(iprev.opcode()),
                                                i.lhs(),new Arg[]{
                                                    iprev.rhs(1),
                                                    iprev.rhs(0)
                                                }));
                                        i.remove();
                                            
                                        changed=true;
                                        lastChange="Not(LessThan) to LessThanEq";
                                    }
                                    break;
                                }
                                default:
                                    
                                    break;
                                }
                            }
                        }
                        break;
                    case ArrayLength:
                        if (i.rhs(0).equals(0)) {
                            if (changed=h.notReached(i)) {
                                lastChange="null array length access";
                            }
                            break instFor;
                        }
                        break;
                    case Mov:
                        if (i.lhs()==i.rhs(0)) {
                            i.remove();
                            changed=true;
                            lastChange="redundant Mov";
                        }
                        break;
                    case Add:
                    case Or:
                    case Xor: {
                        if (i.rhs(0).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(1)}));
                            i.remove();
                            changed=true;
                            lastChange="Add/Or/Xor with zero immediate (1)";
                        } else if (i.rhs(1).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="Add/Or/Xor with zero immediate (2)";
                        } else if (i.rhs(0)==i.rhs(1)) {
                            switch (i.opcode()) {
                            case Xor:
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Mov,
                                        i.lhs(),new Arg[]{
                                            i.lhs().type().makeZero()
                                        }));
                                i.remove();
                                changed=true;
                                lastChange="Xor(a,a) with zero";
                                break;
                            case Or:
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Mov,
                                        i.lhs(),new Arg[]{i.rhs(0)}));
                                i.remove();
                                changed=true;
                                lastChange="Or(a,a) with Mov(a)";
                                break;
                            case Add:
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Shl,
                                        i.lhs(),new Arg[]{
                                            i.rhs(0),
                                            IntConst.make(1)
                                        }));
                                i.remove();
                                changed=true;
                                lastChange="Add(a,a) with Shl(a,1)";
                                break;
                            default:
                                break;
                            }
                        }
                        break;
                    }
                    case And: {
                        if (i.rhs(0).equals(0) ||
                            i.rhs(0)==i.rhs(1)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="And with zero immediate or args equal (1)";
                        } else if (i.rhs(1).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(1)}));
                            i.remove();
                            lastChange="And with zero immediate (2)";
                            changed=true;
                        } else if (i.rhs(1) instanceof IntConst &&
                                   ((IntConst)i.rhs(1)).value()==-1) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="And with -1 immediate";
                        }
                        break;
                    }
                    case Sub: {
                        if (i.rhs(0).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Neg,
                                    i.lhs(),new Arg[]{i.rhs(1)}));
                            i.remove();
                            changed=true;
                            lastChange="Sub with zero immediate (1)";
                        } else if (i.rhs(1).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="Sub with zero immediate (2)";
                        } else if (i.rhs(1) instanceof Arg.Const) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Add,
                                    i.lhs(),new Arg[]{ i.rhs(0), ((Arg.Const)i.rhs(1)).negate() }));
                            i.remove();
                            changed=true;
                            lastChange="Sub(var,const) with Add(var,-const)";
                        }
                        break;
                    }
                    case Mul: {
                        if (i.rhs(0).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="Mul with zero immediate (1)";
                        } else if (i.rhs(0).equals(1)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(1)}));
                            i.remove();
                            changed=true;
                            lastChange="Mul with unary immediate (1)";
                        } else if (i.rhs(1).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(1)}));
                            i.remove();
                            changed=true;
                            lastChange="Mul with zero immediate (2)";
                        } else if (i.rhs(1).equals(1)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="Mul with unary immediate (2)";
                        }
                        break;
                    }
                    case Div: {
                        if (i.rhs(0).equals(0) ||
                            i.rhs(1).equals(1)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="Div with zero numerator or unary denominator";
                        }
                        break;
                    }
                    case Mod: {
                        if (i.rhs(0).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="Mod with zero numerator";
                        }
                        break;
                    }
                    case Shr:
                    case Shl:
                    case Ushr: {
                        if (i.rhs(1).equals(0)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{i.rhs(0)}));
                            i.remove();
                            changed=true;
                            lastChange="Shr/Shl/Ushr with zero shift";
                        }
                        break;
                    }
                    case NullCheck:
                    case ThrowRTEOnZero:
                        if (i.rhs()[0].doesNotEqual(0)) {
                            i.remove();
                            changed=true;
                            lastChange="obviously unnecessary NullCheck/ThrowRTEOnZero";
                        } else if (i.rhs()[0].equals(0)) {
                            if (changed=h.notReachedAfter(i)) {
                                lastChange="obviously failing NullCheck/ThrowRTEOnZero";
                            }
                            break instFor;
                        }
                        break;
                    case Cast: {
                        TypeInst ti=(TypeInst)i;
                        if (ti.getType()==ti.rhs(0).type().asType() ||
                            (!ti.getType().effectiveBasetype().isNumber &&
                             ti.rhs(0).type().isSubtypeOf(
                                 ti.getType().asExectype()))) {
                            ti.prepend(
                                new SimpleInst(ti.di(),OpCode.Mov,
                                               ti.lhs(),ti.rhs()));
                            ti.remove();
                            changed=true;
                            lastChange="unnecessary Cast";
                        }
                        break;
                    }
                    case CastExact: {
                        TypeInst ti=(TypeInst)i;
                        if (ti.getType()==ti.rhs(0).type().asType() &&
                            ti.getType().effectiveBasetype().isNumber) {
                            ti.prepend(
                                new SimpleInst(ti.di(),OpCode.Mov,
                                               ti.lhs(),ti.rhs()));
                            ti.remove();
                            changed=true;
                            lastChange="unnecessary CastExact";
                        }
                        break;
                    }
                    case Fiat: {
                        TypeInst ti=(TypeInst)i;
                        if (ti.getType()==ti.rhs(0).type().asType()) {
                            ti.prepend(
                                new SimpleInst(
                                    ti.di(),OpCode.Mov,
                                    ti.lhs(),ti.rhs()));
                            ti.remove();
                            changed=true;
                            lastChange="unnecessary Fiat";
                        }
                        break;
                    }
                    case CompilerFence:
                    case Fence:
                    case HardCompilerFence:
                    case PollcheckFence:
                    case ClearException:
                    case CheckException: {
                        if (i.prev instanceof SimpleInst) {
                            SimpleInst prev=(SimpleInst)i.prev;
                            if (prev.opcode()==i.opcode()) {
                                i.remove();
                                changed=true;
                                lastChange="redundant fence of exception op";
                            }
                        }
                        break;
                    }
                    default: break;
                    }
                }
            }
            switch (h.getFooter().opcode()) {
            case BranchNonZero:
                if (h.getFooter().rhs()[0].equals(0)) {
                    h.setFooter(
                        new Jump(h.getFooter().di(),
                                 h.getFooter().defaultSuccessor()));
                    changed=true;
                    lastChange="obviously dropped BranchNonZero";
                } else if (h.getFooter().rhs()[0].doesNotEqual(0)) {
                    h.setFooter(
                        new Jump(h.getFooter().di(),
                                 ((Branch)h.getFooter()).target()));
                    changed=true;
                    lastChange="obviously taken BranchNonZero";
                } else if (h.getFooter().defaultSuccessor()==
                           ((Branch)h.getFooter()).target()) {
                    h.setFooter(
                        new Jump(h.getFooter().di(),
                                 h.getFooter().defaultSuccessor()));
                    changed=true;
                    lastChange="obviously unnecessary BranchNonZero";
                } else if (h.getFooter().prev instanceof Instruction) {
                    Instruction prebranch=(Instruction)h.getFooter().prev;
                    if (prebranch.lhs()==h.getFooter().rhs(0) &&
                        !prebranch.uses(prebranch.lhs())) {
                        switch (prebranch.opcode()) {
                        case Not:
                            h.getFooter().opcode=OpCode.BranchZero;
                            h.getFooter().rhs[0]=prebranch.rhs(0);
                            changed=true;
                            lastChange="BranchNonZero(Not) -> BranchZero";
                            break;
                        case Boolify:
                            h.getFooter().rhs[0]=prebranch.rhs(0);
                            changed=true;
                            lastChange="BranchNonZero(Boolify) -> BranchNonZero";
                            break;
                        case Eq:
                        case Neq:
                            if (prebranch.rhs(0).equals(0) ||
                                prebranch.rhs(1).equals(0)) {
                                if (prebranch.opcode()==OpCode.Eq) {
                                    h.getFooter().opcode=OpCode.BranchZero;
                                } else {
                                    h.getFooter().opcode=OpCode.BranchNonZero;
                                }
                                if (prebranch.rhs(0).equals(0)) {
                                    h.getFooter().rhs[0]=prebranch.rhs(1);
                                } else {
                                    h.getFooter().rhs[0]=prebranch.rhs(0);
                                }
                                changed=true;
                                lastChange="simplified BranchNonZero(Eq(x,0))";
                            }
                            break;
                        default:
                            break;
                        }
                    }
                }
                break;
            case BranchZero:
                if (h.getFooter().rhs()[0].doesNotEqual(0)) {
                    h.setFooter(
                        new Jump(h.getFooter().di(),
                                 h.getFooter().defaultSuccessor()));
                    changed=true;
                    lastChange="obviously dropped BranchZero";
                } else if (h.getFooter().rhs()[0].equals(0)) {
                    h.setFooter(
                        new Jump(h.getFooter().di(),
                                 ((Branch)h.getFooter()).target()));
                    changed=true;
                    lastChange="obviously taken BranchZero";
                } else if (h.getFooter().defaultSuccessor()==
                           ((Branch)h.getFooter()).target()) {
                    h.setFooter(
                        new Jump(h.getFooter().di(),
                                 h.getFooter().defaultSuccessor()));
                    changed=true;
                    lastChange="obviously unnecessary BranchZero";
                } else if (h.getFooter().prev instanceof Instruction) {
                    Instruction prebranch=(Instruction)h.getFooter().prev;
                    if (prebranch.lhs()==h.getFooter().rhs(0) &&
                        !prebranch.uses(prebranch.lhs())) {
                        switch (prebranch.opcode()) {
                        case Not:
                            h.getFooter().opcode=OpCode.BranchNonZero;
                            h.getFooter().rhs[0]=prebranch.rhs(0);
                            changed=true;
                            lastChange="BranchZero(Not) -> BranchNonZero";
                            break;
                        case Boolify:
                            h.getFooter().rhs[0]=prebranch.rhs(0);
                            changed=true;
                            lastChange="BranchZero(Boolify) -> BranchZero";
                            break;
                        case Eq:
                        case Neq:
                            if (prebranch.rhs(0).equals(0) ||
                                prebranch.rhs(1).equals(0)) {
                                if (prebranch.opcode()==OpCode.Eq) {
                                    h.getFooter().opcode=OpCode.BranchNonZero;
                                } else {
                                    h.getFooter().opcode=OpCode.BranchZero;
                                }
                                if (prebranch.rhs(0).equals(0)) {
                                    h.getFooter().rhs[0]=prebranch.rhs(1);
                                } else {
                                    h.getFooter().rhs[0]=prebranch.rhs(0);
                                }
                                changed=true;
                                lastChange="simplified BranchZero(Eq(x,0))";
                            }
                            break;
                        default:
                            break;
                        }
                    }
                }
                break;
            case Switch: {
                Switch s=(Switch)h.getFooter();
                boolean noMatches=true;
                for (int i=0;i<s.numCases();++i) {
                    if (s.rhs()[0].equals(s.value(i))) {
                        h.setFooter(new Jump(s.di(),s.target(i)));
                        noMatches=false;
                        changed=true;
                        lastChange="obviously taken Switch";
                        break;
                    } else if (!s.rhs()[0].doesNotEqual(s.value(i))) {
                        noMatches=false;
                    }
                }
                if (noMatches) {
                    h.setFooter(new Jump(s.di(),s.defaultSuccessor()));
                    changed=true;
                    lastChange="obviously dropped Switch";
                } else if (s.numCases()==1) {
                    Var pred=code.addVar(Exectype.INT);
                    h.append(
                        new SimpleInst(
                            s.di(),OpCode.Eq,
                            pred,new Arg[]{
                                s.rhs(0),
                                IntConst.make(s.value(0))
                            }));
                    h.setFooter(
                        new Branch(
                            s.di(),OpCode.BranchNonZero,
                            new Arg[]{ pred },
                            s.defaultSuccessor(),
                            s.target(0)));
                    changed=true;
                    lastChange="Switch that should be Eq/Branch";
                } else {
                    HashSet< Integer > targetsToRemove=new HashSet< Integer >();
                    for (int i=0;i<s.numCases();++i) {
                        if (s.target(i)==s.defaultSuccessor()) {
                            targetsToRemove.add(i);
                        }
                    }
                    if (!targetsToRemove.isEmpty()) {
                        Header[] newTargets=new Header[s.numCases()-targetsToRemove.size()];
                        int[] newValues=new int[s.numCases()-targetsToRemove.size()];
                        int count=0;
                        for (int i=0;i<s.numCases();++i) {
                            if (!targetsToRemove.contains(i)) {
                                newTargets[count]=s.target(i);
                                newValues[count]=s.value(i);
                                count++;
                            }
                        }
                        assert count==newTargets.length;
                        assert count==newValues.length;
                        s.targets=newTargets;
                        s.values=newValues;
                        changed=true;
                        lastChange="removed redundant cases in Switch";
                    }
                }
                break;
            }
            default: break;
            }
        }
	    
        if (code.isSSA()) {
            uc=new UseCalc(code);
            for (Header h : code.headers()) {
                for (Instruction i : h.instructions()) {
                    switch (i.opcode()) {
                    case Shl:
                    case Shr:
                    case Ushr: {
                        int mask=(i.rhs(0).effectiveBasetype().bytes<<3)-1;
                        Instruction i2=i.rhs(1).inst();
                        if (i2!=null &&
                            i2.opcode()==OpCode.And &&
                            i2.rhs(1) instanceof IntConst &&
                            uc.usedOnce(i2.lhs())) {
                            int andMask=((IntConst)i2.rhs(1)).value();
                            if ((mask&andMask)==mask) {
                                i.rhs[1]=i2.rhs[0];
                                changed=true;
                                lastChange="[Shl|Shr|Ushr](a,And(b,c)) -> [Shl|Shr|Ushr](a,b)";
                            }
                        }
                        break;
                    }
                    case Store: {
                        MemoryAccessInst mai=(MemoryAccessInst)i;
                        Instruction i2=mai.rhs(1).inst();
                        if (i2!=null) {
                            switch (i2.opcode()) {
                            case Cast: {
                                TypeInst cast=(TypeInst)i2;
                                if (cast.getType()==mai.getType() &&
                                    cast.getExectype()==cast.rhs(0).type() &&
                                    uc.usedOnce(cast.lhs())) {
                                    mai.rhs[1]=cast.rhs(0);
                                    changed=true;
                                    lastChange="Store(Cast(a)) -> Store(a)";
                                }
                                break;
                            }
                            case And: {
                                if (i2.rhs(1) instanceof IntConst &&
                                    uc.usedOnce(i2.lhs())) {
                                    int bytes;
                                    switch (((IntConst)i2.rhs(1)).value()) {
                                    case 255: bytes=1; break;
                                    case 65535: bytes=2; break;
                                    case 16777215: bytes=3; break;
                                    default: bytes=-1; break;
                                    }
                                    if (mai.effectiveBasetype().bytes<=bytes) {
                                        i.rhs[1]=i2.rhs[0];
                                        changed=true;
                                        lastChange="Store(And(a,b)) -> Store(a)";
                                    }
                                }
                                break;
                            }
                            case Fiat: {
                                TypeInst fiat=(TypeInst)i2;
                                if (fiat.effectiveBasetype().bytes
                                    ==fiat.rhs(0).effectiveBasetype().bytes &&
                                    uc.usedOnce(fiat.lhs())) {
                                    mai.rhs[1]=fiat.rhs(0);
                                    mai.type=fiat.rhs(0).type().asType();
                                    changed=true;
                                    lastChange="Store(Fiat(a)) -> Store(a)";
                                }
                                break;
                            }
                            default: break;
                            }
                        }
                        break;
                    }
                    case Fiat: {
                        TypeInst ti=(TypeInst)i;
                        assert ti.effectiveBasetype().bytes<=ti.rhs(0).effectiveBasetype().bytes;
                        Instruction i2=ti.rhs(0).inst();
                        if (i2!=null) {
                            switch (i2.opcode()) {
                            case Load: {
                                MemoryAccessInst mai=(MemoryAccessInst)i2;
                                if (uc.usedOnce(mai.lhs())) {
                                    Var tmp=code.addVar(ti.lhs().type());
                                    mai.type=ti.type;
                                    mai.setLhs(tmp);
                                    ti.prepend(
                                        new SimpleInst(
                                            ti.di(),OpCode.Mov,
                                            ti.lhs(),new Arg[]{ tmp }));
                                    ti.remove();
                                    changed=true;
                                    lastChange="Fiat(Load(a)) -> Load(a)";
                                }
                                break;
                            }
                            default: break;
                            }
                        }
                        break;
                    }
                    case Cast: {
                        TypeInst ti=(TypeInst)i;
                        if (ti.getExectype()==ti.rhs(0).type()) {
                            if (ti.getType().isAssignableFrom(code.getResultType().getExact(ti.rhs(0)))) {
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Mov,
                                        i.lhs(),new Arg[]{ i.rhs(0) }));
                                i.remove();
                                changed=true;
                                lastChange="Cast(a) -> Mov(a)";
                            } else {
                                Instruction i2=ti.rhs(0).inst();
                                if (i2!=null &&
                                    i2.opcode()==OpCode.And &&
                                    i2.rhs(1) instanceof IntConst) {
                                    int bytes;
                                    switch (((IntConst)i2.rhs(1)).value()) {
                                    case 255: bytes=1; break;
                                    case 65535: bytes=2; break;
                                    case 16777251: bytes=3; break;
                                    default: bytes=-1; break;
                                    }
                                    if (bytes>=0) {
                                        if (ti.effectiveBasetype().bytes>bytes) {
                                            // we're either sign- or zero-extending something
                                            // that is already zero in the high bits
                                            i.prepend(
                                                new SimpleInst(
                                                    i.di(),OpCode.Mov,
                                                    i.lhs(),new Arg[]{ i.rhs(0) }));
                                            i.remove();
                                            changed=true;
                                            lastChange="Cast(And(a,b)) -> And(a,b)";
                                        } else if (uc.usedOnce(i2.lhs())) {
                                            i.rhs[0]=i2.rhs[0];
                                            changed=true;
                                            lastChange="Cast(And(a,b)) -> Cast(a)";
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case Neg: {
                        Instruction i2=i.rhs(0).inst();
                        if (i2!=null && i2.opcode()==OpCode.Neg) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{
                                        i2.rhs(0)
                                    }));
                            i.remove();
                            changed=true;
                            lastChange="eliminated Neg(Neg) (SSA)";
                        }
                        break;
                    }
                    case Boolify: {
                        Instruction i2=i.rhs(0).inst();
                        if (i2!=null && code.getBoolResult().get(i2)) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{
                                        i.rhs(0)
                                    }));
                            i.remove();
                            changed=true;
                            lastChange="eliminated Boolify(bool) (SSA)";
                        }
                        break;
                    }
                    case Not: {
                        Instruction i2=i.rhs(0).inst();
                        if (i2!=null) {
                            switch (i2.opcode()) {
                            case Not: {
                                Instruction i3=i2.rhs(0).inst();
                                if (i3!=null && code.getBoolResult().get(i3)) {
                                    i.prepend(
                                        new SimpleInst(
                                            i.di(),OpCode.Mov,
                                            i.lhs(),new Arg[]{
                                                i3.lhs()
                                            }));
                                    i.remove();
                                    changed=true;
                                    lastChange="eliminated Not(Not(a)) (SSA)";
                                } else {
                                    i.prepend(
                                        new SimpleInst(
                                            i.di(),OpCode.Boolify,
                                            i.lhs(),new Arg[]{
                                                i2.rhs(0),
                                            }));
                                    i.remove();
                                    changed=true;
                                    lastChange="changed Not(Not(a)) to Boolify (SSA)";
                                }
                                break;
                            }
                            case Eq: {
                                if (uc.usedOnce(i2.lhs()) ||
                                    !(i2.rhs(1) instanceof Var)) {
                                    i.prepend(
                                        new SimpleInst(
                                            i.di(),OpCode.Neq,
                                            i.lhs(),new Arg[]{
                                                i2.rhs(0),
                                                i2.rhs(1)
                                            }));
                                    i.remove();
                                    changed=true;
                                    lastChange="changed Not(Eq(v,c)) to Neq(v,c) (SSA)";
                                }
                                break;
                            }
                            case Neq: {
                                if (uc.usedOnce(i2.lhs()) ||
                                    !(i2.rhs(1) instanceof Var)) {
                                    i.prepend(
                                        new SimpleInst(
                                            i.di(),OpCode.Eq,
                                            i.lhs(),new Arg[]{
                                                i2.rhs(0),
                                                i2.rhs(1)
                                            }));
                                    i.remove();
                                    changed=true;
                                    lastChange="changed Not(Neq(v,c)) to Eq(v,c) (SSA)";
                                }
                                break;
                            }
                            case LessThan:
                            case ULessThan:
                            case LessThanEq:
                            case ULessThanEq: {
                                if (i2.rhs(0).type().effectiveBasetype().isInteger &&
                                    (uc.usedOnce(i2.lhs()) ||
                                     !(i2.rhs(0) instanceof Var) ||
                                     !(i2.rhs(1) instanceof Var))) {
                                    i.prepend(
                                        new SimpleInst(
                                            i.di(),invertLT(i2.opcode()),
                                            i.lhs(),new Arg[]{
                                                i2.rhs(1),
                                                i2.rhs(0)
                                            }));
                                    i.remove();
                                        
                                    changed=true;
                                    lastChange="Not(LessThan) to LessThanEq (SSA)";
                                }
                                break;
                            }
                            default:
                                // NOTE: this is boned.  it totally confuses subsequent
                                // peephole opts.  that's why we do this at MIR->LIR
                                // conversion.
                                if (false && code.getBoolResult().get(i2)) {
                                    i.prepend(
                                        new SimpleInst(
                                            i.di(),OpCode.Xor,
                                            i.lhs(),new Arg[]{
                                                i.rhs(0),
                                                i.rhs(0).type().makeConst(1)
                                            }));
                                    i.remove();
                                    
                                    changed=true;
                                    lastChange="Not(bool) to Xor(bool,1) (SSA)";
                                }
                                break;
                            }
                        }
                        break;
                    }
                    default: break;
                    }
                }
                // NOTE: this is somewhat redundant with another simplification phase,
                // but that's ok...
                if (h.getFooter() instanceof Branch) {
                    Branch b=(Branch)h.getFooter();
                    Instruction src=b.rhs(0).inst();
                    if (src!=null && src.opcode()==OpCode.Not) {
                        b.rhs[0]=src.rhs[0];
                        if (b.opcode()==OpCode.BranchNonZero) {
                            b.opcode=OpCode.BranchZero;
                        } else {
                            b.opcode=OpCode.BranchNonZero;
                        }
                        changed=true;
                        lastChange="swapped Branch(Not) (SSA)";
                    }
                }
            }
        } else {
            TwoWayMap< Var, Instruction > vi=new TwoWayMap< Var, Instruction >();
            for (Header h : code.headers()) {
                for (Instruction i : h.instructions()) {
                    if (i.lhs()!=Var.VOID) {
                        vi.put(i.lhs(),i);
                    }
                }
            }
            
            for (Var v : vi.keySet()) {
                Set< Instruction > is=vi.valuesForKey(v);
                switch (is.size()) {
                case 0:
                    if (v.inst!=null) {
                        v.inst=null;
                        changed=true;
                        lastChange="reset Var.inst";
                    }
                    break;
                case 1: {
                    Instruction i=is.iterator().next();
                    if (v.inst!=i) {
                        v.inst=i;
                        changed=true;
                        lastChange="set Var.inst";
                    }
                    break;
                }
                default:
                    assert v.isMultiAssigned();
                    break;
                }
            }
        }
        
        if (changed) {
            setChangedCode(lastChange);
            code.killAllAnalyses();
        }
    }
}



