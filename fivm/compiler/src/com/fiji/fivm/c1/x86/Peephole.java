/*
 * Peephole.java
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

package com.fiji.fivm.c1.x86;

import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

public class Peephole extends LCodePhase {
    public Peephole(LCode c) { super(c); }
    
    public void visitCode() {
        LPredsCalc preds=code.getPreds();

        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                switch (o.opcode()) {
                case ZeroExtQ:
                    if ((Global.pointerSize==8 && o.type()==LType.Long) ||
                        o.type()==LType.Quad) {
                        if (o.rhs(0)==o.rhs(1)) {
                            o.remove();
                            setChangedCode("removed ZeroExtQ<Long> a, a");
                        } else {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Long,
                                    new LArg[]{
                                        o.rhs(0),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("ZeroExtQ<Long> a, b -> Mov a, b");
                        }
                    }
                    break;
                case SignExtQ:
                    if (o.type()==LType.Quad) {
                        if (o.rhs(0)==o.rhs(1)) {
                            o.remove();
                            setChangedCode("removed SignExtQ<Quad> a, a");
                        } else {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Quad,
                                    new LArg[]{
                                        o.rhs(0),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("SignExtQ<Quad> a, b -> Mov a, b");
                        }
                    }
                    break;
                case ZeroExtL:
                case SignExtL:
                    if (o.type()==LType.Long) {
                        if (o.rhs(0)==o.rhs(1)) {
                            o.remove();
                            setChangedCode("removed {Zero,Sign}ExtL<Long> a, b");
                        } else {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Long,
                                    new LArg[]{
                                        o.rhs(0),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("{Zero,Sign}ExtL<Long> a, b -> Mov a, b");
                        }
                    }
                    break;
                case First32:
                    if (Global.pointerSize==8) {
                        if (o.rhs(0)==o.rhs(1)) {
                            o.remove();
                            setChangedCode("removed First32 a, a");
                        } else {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Long,
                                    new LArg[]{
                                        o.rhs(0),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("First32 a, b -> Mov a, b");
                        }
                    }
                    break;
                case Second32:
                    if (Global.pointerSize==8) {
                        if (o.rhs(0)!=o.rhs(1)) {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Quad,
                                    new LArg[]{
                                        o.rhs(0),
                                        o.rhs(1)
                                    }));
                        }
                        o.prepend(
                            new LOp(
                                LOpCode.Ushr,LType.Long,
                                new LArg[]{
                                    Immediate.make(32),
                                    o.rhs(1)
                                }));
                        o.remove();
                        setChangedCode("Second32 a, b -> Mov a, b; Ushr 32, b");
                    }
                    break;
                case ToSingle:
                    if (o.type()==LType.Single) {
                        if (o.rhs(0)==o.rhs(1)) {
                            o.remove();
                            setChangedCode("removed ToSingle<Single> a, a");
                        } else {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Single,
                                    new LArg[]{
                                        o.rhs(0),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("ToSingle<Single> a, b -> Mov a, b");
                        }
                    }
                    break;
                case ToDouble:
                    if (o.type()==LType.Double) {
                        if (o.rhs(0)==o.rhs(1)) {
                            o.remove();
                            setChangedCode("removed ToDouble<Double> a, a");
                        } else {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Double,
                                    new LArg[]{
                                        o.rhs(0),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("ToDouble<Double> a, b -> Mov a, b");
                        }
                    }
                    break;
                case Xor:
                    if (!code.registersAllocated && o.rhs(0).equals(o.rhs(1))) {
                        // help register allocation and spilling
                        o.prepend(
                            new LOp(
                                LOpCode.Mov,o.type(),
                                new LArg[]{
                                    Immediate.make(0),
                                    o.rhs(1)
                                }));
                        o.remove();
                        setChangedCode("Xor a, a -> Mov 0, a");
                    }
                    break;
                case Mov:
                    if (code.registersAllocated &&
                        o.rhs(0)==Immediate.make(0) &&
                        o.rhs(1).variable()) {
                        o.prepend(
                            new LOp(
                                LOpCode.Xor,o.type(),
                                new LArg[]{
                                    o.rhs(1),
                                    o.rhs(1)
                                }));
                        o.remove();
                        setChangedCode("Mov 0, a -> Xor a, a");
                    } else if (o.rhs(0).equals(o.rhs(1))) {
                        o.remove();
                        setChangedCode("removed Mov a, a");
                    } else if (o.rhs(0).stack() || o.rhs(1).stack()) {
                        LNode prevNode=o.prev;
                        if (prevNode instanceof LOp) {
                            LOp prev=(LOp)prevNode;
                            if (prev.opcode()==LOpCode.Mov &&
                                prev.rhs(0).equals(o.rhs(1)) &&
                                prev.rhs(1).equals(o.rhs(0)) &&
                                prev.type()==o.type()) {
                                o.remove();
                                setChangedCode("removed Mov a, (stack) or Mov (stack), a");
                            }
                        }
                    } else if (o.rhs(0) instanceof AbsSymMem) {
                        AbsSymMem asm=(AbsSymMem)o.rhs(0);
                        if (asm.linkable() instanceof StaticCGlobal) {
                            long loadedValue=
                                Immediate.offset(
                                    ((StaticCGlobal)asm.linkable()).fiatToLong(),
                                    asm.offset())
                                & LType.from(Kind.INT,o.type().size()).mask();
                            if (loadedValue==0) {
                                if (o.type().isInt()) {
                                    if (code.registersAllocated) {
                                        o.prepend(
                                            new LOp(
                                                LOpCode.Xor,o.type(),
                                                new LArg[]{
                                                    o.rhs(1),
                                                    o.rhs(1)
                                                }));
                                        o.remove();
                                        setChangedCode("Mov Const0, a -> Xor a, a");
                                    } else {
                                        o.prepend(
                                            new LOp(
                                                LOpCode.Mov,o.type(),
                                                new LArg[]{
                                                    Immediate.make(0),
                                                    o.rhs(1)
                                                }));
                                        o.remove();
                                        setChangedCode("Mov Const0, a -> Mov 0, a");
                                    }
                                } else if (o.type().isFloat() && code.registersAllocated) {
                                    // FIXME: fail!!  this is totally insufficient.
                                    o.prepend(
                                        new LOp(
                                            LOpCode.FXor,o.type(),
                                            new LArg[]{
                                                o.rhs(1),
                                                o.rhs(1)
                                            }));
                                    o.remove();
                                    setChangedCode("Mov Const0.0, a -> FXor a, a");
                                }
                            }
                        }
                    }
                    break;
                    
                    // NOTE: oddly, unlike the Mov 0, a case, the SetEq -> SetAndZero
                    // case (and similar cases) are just taken care of in the final
                    // code generator.
                    
                case Shl:
                    if (o.rhs(0)==Immediate.make(0)) {
                        o.remove();
                        setChangedCode("removed Shl 0, a");
                    } else if (code.registersAllocated && 
                               o.rhs(0)==Immediate.make(1) &&
                               o.rhs(1).variable()) {
                        o.prepend(
                            new LOp(
                                LOpCode.Lea,o.type(),
                                new LArg[]{
                                    new IndexMem(0,o.rhs(1),o.rhs(1),Scale.ONE),
                                    o.rhs(1)
                                }));
                        o.remove();
                        setChangedCode("Shl 1, a -> Add a, a");
                    }
                    break;
                case Add:
                    if (o.rhs(0)==Immediate.make(0)) {
                        o.remove();
                        setChangedCode("removed Add 0, a");
                    }
                    break;
                case Or:
                    if (o.rhs(0).equals(o.rhs(1))) {
                        o.remove();
                        setChangedCode("removed Or a, a");
                    } else if (o.rhs(0)==Immediate.make(0)) {
                        o.remove();
                        setChangedCode("removed Or 0, a");
                    }
                    break;
                case And:
                    if (o.rhs(0).equals(o.rhs(1))) {
                        o.remove();
                        setChangedCode("removed And a, a");
                    } else if (o.rhs(0) instanceof Immediate) {
                        Immediate i=(Immediate)o.rhs(0);
                        long value=i.value();
                        if (value==0) {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,o.type(),
                                    new LArg[]{
                                        Immediate.make(0),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("And 0, a -> Mov 0, a");
                        } else if (value==o.type().mask()) {
                            o.remove();
                            setChangedCode("removed And "+o.type().mask()+", a");
                        }
                    }
                    break;
                case Lea:
                    if (o.rhs(0) instanceof AbsIndexMem) {
                        AbsIndexMem aim=(AbsIndexMem)o.rhs(0);
                        if (aim.offset()==0) {
                            if (aim.scale()==Scale.ONE) {
                                if (aim.index().equals(o.rhs(1))) {
                                    o.remove();
                                    setChangedCode("removed Lea (,a,1), a");
                                } else {
                                    o.prepend(
                                        new LOp(
                                            LOpCode.Mov,o.type(),
                                            new LArg[]{
                                                aim.index(),
                                                o.rhs(1)
                                            }));
                                    o.remove();
                                    setChangedCode("Lea (,a,1), b -> Mov a, b");
                                }
                            } else if (code.registersAllocated) {
                                if (aim.scale()==Scale.TWO) {
                                    if (aim.index().equals(o.rhs(1))) {
                                        o.prepend(
                                            new LOp(
                                                LOpCode.Add,o.type(),
                                                new LArg[]{
                                                    o.rhs(1),
                                                    o.rhs(1)
                                                }));
                                        o.remove();
                                        setChangedCode("Lea (,a,2), a -> Add a, a");
                                    } else {
                                        o.prepend(
                                            new LOp(
                                                LOpCode.Lea,o.type(),
                                                new LArg[]{
                                                    new IndexMem(0,aim.index(),aim.index(),Scale.ONE),
                                                    o.rhs(1)
                                                }));
                                        o.remove();
                                        setChangedCode("Lea (,a,2), b -> Lea (a,a), b");
                                    }
                                } else {
                                    if (!aim.index().equals(o.rhs(1))) {
                                        o.prepend(
                                            new LOp(
                                                LOpCode.Mov,o.type(),
                                                new LArg[]{
                                                    aim.index(),
                                                    o.rhs(1)
                                                }));
                                    }
                                    o.prepend(
                                        new LOp(
                                            LOpCode.Shl,o.type(),
                                            new LArg[]{
                                                Immediate.make(aim.scale().shift()),
                                                o.rhs(1)
                                            }));
                                    o.remove();
                                    setChangedCode("Lea (,a,b), c -> Shl b, c");
                                }
                            }
                        }
                    } else if (o.rhs(0) instanceof IndexMem) {
                        IndexMem im=(IndexMem)o.rhs(0);
                        if (im.offset()==0 && im.scale()==Scale.ONE) {
                            if (im.base().equals(o.rhs(1))) {
                                o.prepend(
                                    new LOp(
                                        LOpCode.Add,o.type(),
                                        new LArg[]{
                                            im.index(),
                                            o.rhs(1)
                                        }));
                                o.remove();
                                setChangedCode("Lea (a,b), a -> Add b, a");
                            } else if (im.index().equals(o.rhs(1))) {
                                o.prepend(
                                    new LOp(
                                        LOpCode.Add,o.type(),
                                        new LArg[]{
                                            im.base(),
                                            o.rhs(1)
                                        }));
                                o.remove();
                                setChangedCode("Lea (a,b), b -> Add a, b");
                            }
                        }
                    } else if (o.rhs(0) instanceof OffMem) {
                        OffMem om=(OffMem)o.rhs(0);
                        if (om.offset()==0) {
                            if (om.base().equals(o.rhs(1))) {
                                o.remove();
                                setChangedCode("removed Lea 0(a), a");
                            } else {
                                o.prepend(
                                    new LOp(
                                        LOpCode.Mov,o.type(),
                                        new LArg[]{
                                            om.base(),
                                            o.rhs(1)
                                        }));
                                o.remove();
                                setChangedCode("Lea 0(a), b -> Mov a, b");
                            }
                        } else if (om.base().equals(o.rhs(1))) {
                            o.prepend(
                                new LOp(
                                    LOpCode.Add,o.type(),
                                    new LArg[]{
                                        Immediate.make(om.offset()),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("Lea a(b), b -> Add a, b");
                        }
                    } else if (o.rhs(0) instanceof OffSymMem) {
                        OffSymMem osm=(OffSymMem)o.rhs(0);
                        if (osm.base().equals(o.rhs(1))) {
                            o.prepend(
                                new LOp(
                                    LOpCode.Add,o.type(),
                                    new LArg[]{
                                        new SymImm(osm.symOffset(),
                                                   osm.offset()),
                                        o.rhs(1)
                                    }));
                            o.remove();
                            setChangedCode("Lea a+b(c), c -> Add a+b, c");
                        }
                    } else if (o.rhs(0) instanceof AbsMem) {
                        AbsMem am=(AbsMem)o.rhs(0);
                        o.prepend(
                            new LOp(
                                LOpCode.Mov,o.type(),
                                new LArg[]{
                                    Immediate.make(am.addr()),
                                    o.rhs(1)
                                }));
                        o.remove();
                        setChangedCode("Lea a, b -> Mov a, b");
                    } else if (o.rhs(0) instanceof AbsSymMem) {
                        AbsSymMem asm=(AbsSymMem)o.rhs(0);
                        o.prepend(
                            new LOp(
                                LOpCode.Mov,o.type(),
                                new LArg[]{
                                    new SymImm(asm.linkable(),
                                               asm.offset()),
                                    o.rhs(1)
                                }));
                        o.remove();
                        setChangedCode("Lea a+b, c -> Mov a+b, c");
                    }
                    break;
                default:
                    break;
                }
                
                for (int i=0;i<o.nrhs();++i) {
                    if (o.rhs(i) instanceof Immediate) {
                        long oldValue=((Immediate)o.rhs(i)).value();
                        long newValue=o.typeOf(i).signExtend(oldValue);
                        if (oldValue!=newValue) {
                            o.rhs[i]=Immediate.make(newValue);
                            setChangedCode("Changed immediate "+oldValue+" to "+newValue+" based on type "+o.typeOf(i));
                        }
                    }
                }
            }
            if (h.first()==h.footer()) {
                switch (h.footer().opcode()) {
                case BranchLessThan:
                case BranchGreaterThan:
                case BranchLTEq:
                case BranchGTEq:
                case BranchFGreaterThan:
                case BranchFGTEq:
                case BranchNotFGT:
                case BranchNotFGTEq:
                case BranchULessThan:
                case BranchUGreaterThan:
                case BranchULTEq:
                case BranchUGTEq:
                case BranchEq:
                case BranchNeq:
                    boolean allgood=true;
                    boolean foundone=false;
                    for (LHeader h2 : preds.preds(h)) {
                        foundone=true;
                        if (h2.footer().opcode()!=h.footer().opcode() ||
                            !h2.footer().rhsEqual(h.footer())) {
                            allgood=false;
                            break;
                        }
                    }
                    // FIXME: support reversal.  so if rhs's are equal when reversed then
                    // reverse the rebranch.
                    if (allgood && foundone) {
                        LOpCode newOpcode;
                        switch (h.footer().opcode()) {
                        case BranchLessThan:     newOpcode=LOpCode.RebranchLessThan;     break;
                        case BranchGreaterThan:  newOpcode=LOpCode.RebranchGreaterThan;  break;
                        case BranchLTEq:         newOpcode=LOpCode.RebranchLTEq;         break;
                        case BranchGTEq:         newOpcode=LOpCode.RebranchGTEq;         break;
                        case BranchFGreaterThan: newOpcode=LOpCode.RebranchFGreaterThan; break;
                        case BranchFGTEq:        newOpcode=LOpCode.RebranchFGTEq;        break;
                        case BranchNotFGT:       newOpcode=LOpCode.RebranchNotFGT;       break;
                        case BranchNotFGTEq:     newOpcode=LOpCode.RebranchNotFGTEq;     break;
                        case BranchULessThan:    newOpcode=LOpCode.RebranchULessThan;    break;
                        case BranchUGreaterThan: newOpcode=LOpCode.RebranchUGreaterThan; break;
                        case BranchULTEq:        newOpcode=LOpCode.RebranchULTEq;        break;
                        case BranchUGTEq:        newOpcode=LOpCode.RebranchUGTEq;        break;
                        case BranchEq:           newOpcode=LOpCode.RebranchEq;           break;
                        case BranchNeq:          newOpcode=LOpCode.RebranchNeq;          break;
                        default: throw new Error(""+h.footer());
                        }
                        h.footer().opcode=newOpcode;
                        h.footer().rhs=LArg.EMPTY;
                        setChangedCode("repeat Branch -> Rebranch");
                    }
                    break;
                default:
                    break;
                }
            }
        }
        
        if (changedCode()) {
            code.killAllAnalyses();
        }
    }
}

