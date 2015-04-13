/*
 * Expand32.java
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

import java.util.*;

public class Expand32 extends LCodePhase {
    public Expand32(LCode c) { super(c); }
    
    HashMap< Tmp, Tmp > upperMap;
        
    Tmp upper(Tmp t) {
        Tmp result=upperMap.get(t);
        if (result==null) {
            upperMap.put(t,result=code.addTmp(Kind.INT));
        }
        return result;
    }
    
    LArg upper(LArg a) {
        if (a instanceof Tmp) {
            return upper((Tmp)a);
        } else if (a==Reg.AX) {
            return Reg.DX;
        } else {
            return a.offset(4);
        }
    }
    
    LArg shrink(LArg a) {
        if (a instanceof Immediate) {
            Immediate i=(Immediate)a;
            long newValue=i.value()&0xffffffffl;
            if (newValue==i.value()) {
                return i;
            } else {
                return Immediate.make(newValue);
            }
        } else {
            return a;
        }
    }
    
    public void visitCode() {
        assert Global.pointerSize==4;
        
        upperMap=new HashMap< Tmp, Tmp >();
        
        ArrayList< LOp > postProcess=new ArrayList< LOp >();
        
        // first turn SetXXX into BranchXXX for quads; this is perhaps not optimal
        // but it is simpler!
        for (LHeader h : code.headers2()) {
            for (LOp o : h.instructions2()) {
                if (o.type()==LType.Quad) {
                    switch (o.opcode()) {
                    case SetLessThan:
                    case SetGreaterThan:
                    case SetLTEq:
                    case SetGTEq:
                    case SetULessThan:
                    case SetUGreaterThan:
                    case SetULTEq:
                    case SetUGTEq:
                    case SetAndZero:
                    case SetAndNotZero:
                    case SetEq:
                    case SetNeq: {
                        LOpCode newOpCode;
                        
                        switch (o.opcode()) {
                        case SetLessThan:     newOpCode=LOpCode.BranchLessThan;     break;
                        case SetGreaterThan:  newOpCode=LOpCode.BranchGreaterThan;  break;
                        case SetLTEq:         newOpCode=LOpCode.BranchLTEq;         break;
                        case SetGTEq:         newOpCode=LOpCode.BranchGTEq;         break;
                        case SetULessThan:    newOpCode=LOpCode.BranchULessThan;    break;
                        case SetUGreaterThan: newOpCode=LOpCode.BranchUGreaterThan; break;
                        case SetULTEq:        newOpCode=LOpCode.BranchULTEq;        break;
                        case SetUGTEq:        newOpCode=LOpCode.BranchUGTEq;        break;
                        case SetAndZero:      newOpCode=LOpCode.BranchAndZero;      break;
                        case SetAndNotZero:   newOpCode=LOpCode.BranchAndNotZero;   break;
                        case SetEq:           newOpCode=LOpCode.BranchEq;           break;
                        case SetNeq:          newOpCode=LOpCode.BranchNeq;          break;
                        default: throw new Error("bad opcode");
                        }
                        
                        LHeader cont=h.split(o);
                        
                        LHeader setOne=h.makeSimilar();
                        LHeader setZero=h.makeSimilar();
                        
                        setOne.append(
                            new LOp(
                                LOpCode.Mov,LType.Long,
                                new LArg[]{
                                    Immediate.make(1),
                                    o.rhs(2)
                                }));
                        setOne.setFooter(
                            new LFooter(
                                LOpCode.Jump,LType.Void,
                                LArg.EMPTY,
                                new LHeader[]{
                                    cont
                                }));
                        
                        setZero.append(
                            new LOp(
                                LOpCode.Mov,LType.Long,
                                new LArg[]{
                                    Immediate.make(0),
                                    o.rhs(2)
                                }));
                        setZero.setFooter(
                            new LFooter(
                                LOpCode.Jump,LType.Void,
                                LArg.EMPTY,
                                new LHeader[]{
                                    cont
                                }));
                        
                        h.setFooter(
                            new LFooter(
                                newOpCode,LType.Quad,
                                new LArg[]{
                                    o.rhs(0),
                                    o.rhs(1)
                                },
                                new LHeader[]{
                                    setZero,
                                    setOne
                                }));
                        
                        o.remove();
                        
                        setChangedCode("converted SetXXX Quad to BranchXXX Quad");
                        break;
                    }
                    default: break;
                    }
                }
            }
        }
        
        for (LHeader h : code.headers3()) {
            for (LOp o : h.instructions()) {
                switch (o.opcode()) {
                case SignExtQ:
                    // NOTE the reverse order of appends
                    o.append(
                        new LOp(
                            LOpCode.Shr,LType.Long,
                            new LArg[]{
                                Immediate.make(31),
                                upper(o.rhs(1))
                            }));
                    o.append(
                        new LOp(
                            LOpCode.Mov,LType.Long,
                            new LArg[]{
                                o.rhs(1),
                                upper(o.rhs(1))
                            }));
                    if (o.type()==LType.Long) {
                        o.opcode=LOpCode.Mov;
                    } else {
                        o.opcode=LOpCode.SignExtL;
                    }
                    setChangedCode("converted SignExtQ");
                    break;
                case ZeroExtQ:
                    o.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Long,
                            new LArg[]{
                                Immediate.make(0),
                                upper(o.rhs(1))
                            }));
                    if (o.type()==LType.Long) {
                        o.opcode=LOpCode.Mov;
                    } else {
                        o.opcode=LOpCode.ZeroExtL;
                    }
                    setChangedCode("converted ZeroExtQ");
                    break;
                default:
                    if (o.type()==LType.Quad) {
                        switch (o.opcode()) {
                        case PhantomDef:
                            o.prepend(
                                new LOp(
                                    LOpCode.PhantomDef,LType.Long,
                                    new LArg[]{
                                        upper(o.rhs(0))
                                    }));
                            break;
                        case Mov:
                        case And:
                        case Or:
                        case Xor:
                            o.prepend(
                                new LOp(
                                    o.opcode(),LType.Long,
                                    new LArg[]{
                                        upper(o.rhs(0)),
                                        upper(o.rhs(1))
                                    }));
                            break;
                        case BitNot:
                            o.prepend(
                                new LOp(
                                    o.opcode(),LType.Long,
                                    new LArg[]{
                                        upper(o.rhs(0))
                                    }));
                            break;
                        case First32:
                            o.opcode=LOpCode.Mov;
                            break;
                        case Second32:
                            o.opcode=LOpCode.Mov;
                            o.rhs()[0]=upper(o.rhs(0));
                            break;
                        case Add:
                            o.append(
                                new LOp(
                                    LOpCode.Adc,LType.Long,
                                    new LArg[]{
                                        upper(o.rhs(0)),
                                        upper(o.rhs(1))
                                    }));
                            break;
                        case Sub:
                            o.append(
                                new LOp(
                                    LOpCode.Sbb,LType.Long,
                                    new LArg[]{
                                        upper(o.rhs(0)),
                                        upper(o.rhs(1))
                                    }));
                            break;
                        case Mul: {
                            // FUCK: we should be putting b into AX ... that'll lead to somewhat
                            // less copying.
                        
                            LArg a=Reg.AX;
                        
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Long,
                                    new LArg[]{
                                        o.rhs(0),
                                        Reg.AX
                                    }));
                        
                            Tmp b=tmpify(o,LType.Long,o.rhs(1));
                            Tmp c=tmpify(o,LType.Long,upper(o.rhs(0)));
                            Tmp d=tmpify(o,LType.Long,upper(o.rhs(1)));
                        
                            // we want to do:
                            // b <- a*b
                            // d <- b*c + a*d + upper(a*b)
                        
                            Tmp bCopy=code.addTmp(Kind.INT);
                        
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Long,
                                    new LArg[]{
                                        b,
                                        bCopy
                                    }));
                            
                            o.prepend(
                                new LOp(
                                    LOpCode.Mul,LType.Long,
                                    new LArg[]{
                                        a,
                                        d
                                    }));
                            o.prepend(
                                new LOp(
                                    LOpCode.Mul,LType.Long,
                                    new LArg[]{
                                        c,
                                        bCopy
                                    }));
                        
                            // d = a*d
                            // bCopy = c*b
                        
                            o.prepend(
                                new LOp(
                                    LOpCode.Muld,LType.Long,
                                    new LArg[]{
                                        b
                                    }));
                        
                            // AX = a*b
                            // DX = upper(a*b)
                            // d = a*d
                            // bCopy = c*b
                        
                            o.prepend(
                                new LOp(
                                    LOpCode.Add,LType.Long,
                                    new LArg[]{
                                        bCopy,
                                        d
                                    }));
                            o.prepend(
                                new LOp(
                                    LOpCode.Add,LType.Long,
                                    new LArg[]{
                                        Reg.DX,
                                        d
                                    }));
                        
                            // AX = a*b
                            // d = a*d + c*b + upper(a*b)
                        
                            emitMov(o,LType.Long,Reg.AX,o.rhs(1));
                            emitMov(o,LType.Long,d,upper(o.rhs(1)));
                        
                            o.remove();
                            break;
                        }
                        case Neg:
                            // NOTE: append is reverse order!!
                            o.append(
                                new LOp(
                                    LOpCode.Neg,LType.Long,
                                    new LArg[]{
                                        upper(o.rhs(0))
                                    }));
                            o.append(
                                new LOp(
                                    LOpCode.Adc,LType.Long,
                                    new LArg[]{
                                        Immediate.make(0),
                                        upper(o.rhs(0))
                                    }));
                            break;
                        case Shl:
                        case Shr:
                        case Ushr:
                            // need to post-process.
                            postProcess.add(o);
                            break;
                        case ToSingle:
                        case ToDouble: {
                            ScratchSlot ss=code.addScratch(8);
                            
                            if (o.rhs(0).memory()) {
                                o.prepend(
                                    new LOp(
                                        LOpCode.PushIntToFP,LType.Quad,
                                        new LArg[]{
                                            o.rhs(0)
                                        }));
                            } else {
                                o.prepend(
                                    new LOp(
                                        LOpCode.Mov,LType.Long,
                                        new LArg[]{
                                            o.rhs(0),
                                            new OffScratchSlot(ss,0)
                                        }));
                                o.prepend(
                                    new LOp(
                                        LOpCode.Mov,LType.Long,
                                        new LArg[]{
                                            upper(o.rhs(0)),
                                            new OffScratchSlot(ss,4)
                                        }));
                                o.prepend(
                                    new LOp(
                                        LOpCode.PushIntToFP,LType.Quad,
                                        new LArg[]{
                                            new OffScratchSlot(ss,0)
                                        }));
                            }
                            
                            o.prepend(
                                new LOp(
                                    LOpCode.PopFP,
                                    o.opcode()==LOpCode.ToSingle?LType.Single:LType.Double,
                                    new LArg[]{
                                        new OffScratchSlot(ss,0)
                                    }));
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,
                                    o.opcode()==LOpCode.ToSingle?LType.Single:LType.Double,
                                    new LArg[]{
                                        new OffScratchSlot(ss,0),
                                        o.rhs(1)
                                    }));
                            
                            o.remove();
                            break;
                        }
                        case FiatToInt: {
                            // NOTE: append is reverse order
                            Tmp tmp=code.addTmp(Kind.FLOAT);
                            
                            o.append(
                                new LOp(
                                    LOpCode.FiatToInt,LType.Long,
                                    new LArg[]{
                                        tmp,
                                        upper(o.rhs(1))
                                    }));
                            o.append(
                                new LOp(
                                    LOpCode.UnpckHP,LType.Double,
                                    new LArg[]{
                                        tmp,
                                        tmp
                                    }));
                            o.append(
                                new LOp(
                                    LOpCode.UnpckLP,LType.Single,
                                    new LArg[]{
                                        tmp,
                                        tmp
                                    }));
                            o.append(
                                new LOp(
                                    LOpCode.Mov,LType.Double,
                                    new LArg[]{
                                        o.rhs(0),
                                        tmp
                                    }));
                            break;
                        }
                        case FiatToFloat: {
                            Tmp tmp=code.addTmp(Kind.FLOAT);
                            
                            o.prepend(
                                new LOp(
                                    LOpCode.FiatToFloat,LType.Long,
                                    new LArg[]{
                                        upper(o.rhs(0)),
                                        tmp
                                    }));
                            o.append(
                                new LOp(
                                    LOpCode.UnpckLP,LType.Single,
                                    new LArg[]{
                                        tmp,
                                        o.rhs(1)
                                    }));
                            break;
                        }
                        default: throw new CompilerException("Invalid instruction for Quad: "+o);
                        }
                        o.type=LType.Long;
                        setChangedCode("converted Quad operation to Long operation(s)");
                    }
                }
            }
            
            LFooter f=h.footer();
            if (f.type()==LType.Quad) {
                switch (f.opcode()) {
                case BranchLessThan:
                case BranchLTEq:
                case BranchULessThan:
                case BranchULTEq:
                case BranchGreaterThan:
                case BranchGTEq:
                case BranchUGreaterThan:
                case BranchUGTEq: {
                    // need: less than, if true go to true, if false
                    // test equality, if equal do less than on low, if unequal
                    // then go false.
                    LHeader second=h.makeSimilar();
                    LHeader third=h.makeSimilar();
                    
                    LOpCode firstOp;
                    LOpCode secondOp;
                    switch (f.opcode()) {
                    case BranchLessThan:
                        firstOp=LOpCode.BranchLessThan;
                        secondOp=LOpCode.BranchULessThan;
                        break;
                    case BranchLTEq:
                        firstOp=LOpCode.BranchLessThan;
                        secondOp=LOpCode.BranchULTEq;
                        break;
                    case BranchULessThan:
                        firstOp=LOpCode.BranchULessThan;
                        secondOp=LOpCode.BranchULessThan;
                        break;
                    case BranchULTEq:
                        firstOp=LOpCode.BranchULessThan;
                        secondOp=LOpCode.BranchULTEq;
                        break;
                    case BranchGreaterThan:
                        firstOp=LOpCode.BranchGreaterThan;
                        secondOp=LOpCode.BranchUGreaterThan;
                        break;
                    case BranchGTEq:
                        firstOp=LOpCode.BranchGreaterThan;
                        secondOp=LOpCode.BranchUGTEq;
                        break;
                    case BranchUGreaterThan:
                        firstOp=LOpCode.BranchUGreaterThan;
                        secondOp=LOpCode.BranchUGreaterThan;
                        break;
                    case BranchUGTEq:
                        firstOp=LOpCode.BranchUGreaterThan;
                        secondOp=LOpCode.BranchUGTEq;
                        break;
                    default: throw new Error();
                    }
                    
                    if (upper(f.rhs(1))==Immediate.make(0) &&
                        firstOp==LOpCode.BranchULessThan) {
                        h.setFooter(
                            new LFooter(
                                LOpCode.BranchEq,LType.Long,
                                new LArg[]{
                                    upper(f.rhs(0)),
                                    upper(f.rhs(1))
                                },
                                new LHeader[]{
                                    f.successor(0),
                                    third
                                }));
                    } else {
                        h.setFooter(
                            new LFooter(
                                firstOp,LType.Long,
                                new LArg[]{
                                    upper(f.rhs(0)),
                                    upper(f.rhs(1))
                                },
                                new LHeader[]{
                                    second,
                                    f.successor(1)
                                }));
                        
                        second.setFooter(
                            new LFooter(
                                LOpCode.RebranchEq,LType.Long,
                                LArg.EMPTY,
                                new LHeader[]{
                                    f.successor(0),
                                    third
                                }));
                    }
                    
                    if (shrink(f.rhs(1))==Immediate.make(0) &&
                        secondOp==LOpCode.BranchUGTEq) {
                        third.setFooter(
                            new LFooter(
                                LOpCode.Jump,LType.Void,
                                LArg.EMPTY,new LHeader[]{
                                    f.successor(1)
                                }));
                    } else if (shrink(f.rhs(1))==Immediate.make(0) &&
                               secondOp==LOpCode.BranchULessThan) {
                        third.setFooter(
                            new LFooter(
                                LOpCode.Jump,LType.Void,
                                LArg.EMPTY,new LHeader[]{
                                    f.successor(0)
                                }));
                    } else {
                        third.setFooter(
                            new LFooter(
                                secondOp,LType.Long,
                                new LArg[]{
                                    f.rhs(0),
                                    f.rhs(1)
                                },
                                new LHeader[]{
                                    f.successor(0),
                                    f.successor(1)
                                }));
                    }
                    break;
                }
                case BranchAndZero:
                case BranchAndNotZero:
                case BranchEq:
                case BranchNeq: {
                    boolean inverted;
                    switch (f.opcode()) {
                    case BranchAndZero:
                    case BranchEq:
                        inverted=true;
                        break;
                    default:
                        inverted=false;
                        break;
                    }
                    
                    LHeader second=h.makeSimilar();
                    
                    h.setFooter(
                        new LFooter(
                            f.opcode(),LType.Long,
                            new LArg[]{
                                upper(f.rhs(0)),
                                upper(f.rhs(1))
                            },
                            new LHeader[]{
                                inverted?f.successor(0):second,
                                inverted?second:f.successor(1)
                            }));
                    
                    second.setFooter(
                        new LFooter(
                            f.opcode(),LType.Long,
                            new LArg[]{
                                f.rhs(0),
                                f.rhs(1)
                            },
                            new LHeader[]{
                                f.successor(0),
                                f.successor(1)
                            }));
                    break;
                }
                default: throw new CompilerException("Invalid footer for Quad: "+f);
                }
                setChangedCode("converted Quad operation to Long operation(s)");
            }
        }
        
        for (LOp o : postProcess) {
            switch (o.opcode()) {
            case Shl:
            case Shr:
            case Ushr: {
                if (o.rhs(0) instanceof Immediate) {
                    int shiftAmount=((Immediate)o.rhs(0)).value32();
                    shiftAmount&=63;
                    if (shiftAmount==32) {
                        o.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Long,
                                new LArg[]{
                                    o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1)),
                                    o.opcode()==LOpCode.Shl?upper(o.rhs(1)):o.rhs(1)
                                }));
                        if (o.opcode()==LOpCode.Shr) {
                            o.prepend(
                                new LOp(
                                    LOpCode.Shr,LType.Long,
                                    new LArg[]{
                                        Immediate.make(31),
                                        upper(o.rhs(1))
                                }));
                        } else {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Long,
                                    new LArg[]{
                                        Immediate.make(0),
                                        o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1))
                                    }));
                        }
                    } else if (shiftAmount>32) {
                        o.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Long,
                                new LArg[]{
                                    o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1)),
                                    o.opcode()==LOpCode.Shl?upper(o.rhs(1)):o.rhs(1)
                                }));
                        o.prepend(
                            new LOp(
                                o.opcode(),LType.Long,
                                new LArg[]{
                                    Immediate.make(shiftAmount-32),
                                    o.opcode()==LOpCode.Shl?upper(o.rhs(1)):o.rhs(1)
                                }));
                        if (o.opcode()==LOpCode.Shr) {
                            o.prepend(
                                new LOp(
                                    LOpCode.Shr,LType.Long,
                                    new LArg[]{
                                        Immediate.make(31),
                                        upper(o.rhs(1))
                                    }));
                        } else {
                            o.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Long,
                                    new LArg[]{
                                        Immediate.make(0),
                                        o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1))
                                    }));
                        }
                    } else {
                        o.prepend(
                            new LOp(
                                o.opcode()==LOpCode.Shl?LOpCode.Shld:LOpCode.Shrd,
                                LType.Long,
                                new LArg[]{
                                    Immediate.make(shiftAmount),
                                    o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1)),
                                    o.opcode()==LOpCode.Shl?upper(o.rhs(1)):o.rhs(1)
                                }));
                        o.prepend(
                            new LOp(
                                o.opcode(),LType.Long,
                                new LArg[]{
                                    Immediate.make(shiftAmount),
                                    o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1))
                                }));
                    }
                } else {
                    assert o.rhs(0)==Reg.CX;
                    
                    LHeader orig=o.head();
                    LHeader cont=orig.split(o);
                    
                    LHeader special=orig.makeSimilar();
                        
                    orig.append(
                        new LOp(
                            o.opcode()==LOpCode.Shl?LOpCode.Shld:LOpCode.Shrd,
                            LType.Long,
                            new LArg[]{
                                Reg.CX,
                                o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1)),
                                o.opcode()==LOpCode.Shl?upper(o.rhs(1)):o.rhs(1)
                            }));
                    orig.append(
                        new LOp(
                            o.opcode(),LType.Long,
                            new LArg[]{
                                Reg.CX,
                                o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1))
                            }));
                    
                    orig.setFooter(
                        new LFooter(
                            LOpCode.BranchAndZero,LType.Byte,
                            new LArg[]{
                                Reg.CX,
                                Immediate.make(32)
                            },
                            new LHeader[]{
                                special,
                                cont
                            }));
                    
                    special.append(
                        new LOp(
                            LOpCode.Mov,LType.Long,
                            new LArg[]{
                                o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1)),
                                o.opcode()==LOpCode.Shl?upper(o.rhs(1)):o.rhs(1)
                            }));
                    if (o.opcode()==LOpCode.Shr) {
                        special.append(
                            new LOp(
                                LOpCode.Shr,LType.Long,
                                new LArg[]{
                                    Immediate.make(31),
                                    upper(o.rhs(1))
                                }));
                    } else {
                        special.append(
                            new LOp(
                                LOpCode.Mov,LType.Long,
                                new LArg[]{
                                    Immediate.make(0),
                                    o.opcode()==LOpCode.Shl?o.rhs(1):upper(o.rhs(1))
                                }));
                    }
                    
                    special.setFooter(
                        new LFooter(
                            LOpCode.Jump,LType.Void,
                            LArg.EMPTY,
                            new LHeader[]{
                                cont
                            }));
                }
                
                o.remove();
                break;
            }
            default: throw new CompilerException("bad op in post process list: "+o);
            }
        }
        
        upperMap=null;
        
        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                for (int i=0;i<o.nrhs();++i) {
                    LArg newArg=shrink(o.rhs(i));
                    if (newArg!=o.rhs(i)) {
                        o.rhs[i]=newArg;
                        setChangedCode("Shrunk an argument");
                    }
                }
            }
        }
        
        if (changedCode()) {
            code.killAllAnalyses();
        }
        
        code.expanded32=true;
    }
}

