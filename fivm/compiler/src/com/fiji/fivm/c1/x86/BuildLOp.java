/*
 * BuildLOp.java
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

import com.fiji.fivm.*;
import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

import java.util.*;

public class BuildLOp extends MainLoweringBU {
    ToLIR context;
    
    LHeader lh;
    LOp prev;
    
    // operations for which we have already emitted code.
    HashSet< Operation > offlimits=new HashSet< Operation >();
    
    // variables that no longer exist.
    HashSet< Var > skipped=new HashSet< Var >();
    
    public BuildLOp(ToLIR context) {
        this.context=context;
    }

    protected Instruction findInstruction(Var v) {
        if (context.uc.usedOnce(v) && !context.alreadyUsed.contains(v)) {
            return v.inst();
        } else {
            return null;
        }
    }
    
    protected void acceptedOperation(Operation o) {
        offlimits.add(o);
    }
    
    // FIXME: should I be inferring which variables are really bytes?  and then doing
    // register allocation on those somewhat separately?  ugh!
    
    private boolean sideCross() {
        return context.scc.hasSideAny(opVars,0,numOpVars);
    }
    
    private LArg address(Arg arg) {
        LArg result=context.baa.build(arg);
        if (result==null) {
            result=new OffMem(0,context.tmpForVar(arg));
        }
        return result;
    }
    
    private boolean isImm(Arg val) {
        return val instanceof Arg.IntConst
            && ((Arg.IntConst)val).is32();
    }
    
    private int imm(Arg val) {
        return ((Arg.IntConst)val).value32();
    }
    
    private LArg tmpOrImmFromVar(Arg val) {
        assert val instanceof Var;
        Var var=(Var)val;
            
        // I could have done this with a separate visitor, but it's simple enough that
        // it's not really worth it.
            
        Instruction inst=var.inst();
        if (inst!=null) {
            switch (inst.opcode()) {
            case Add: {
                if (inst.rhs(0) instanceof Var &&
                    inst.rhs(1) instanceof PointerConst) {
                    Instruction inst2=((Var)inst.rhs(0)).inst();
                    PointerConst pc=(PointerConst)inst.rhs(1);
                    if (inst2!=null &&
                        inst2.opcode()==OpCode.GetCVarAddress &&
                        pc.is32()) {
                        CFieldInst cfi=(CFieldInst)inst2;
                        if (!(cfi.field() instanceof CLocal)) {
                            return new SymImm((Linkable)context.translateField(cfi.field()),
                                              pc.value32());
                        }
                    }
                }
                break;
            }
            case GetCVarAddress: {
                CFieldInst cfi=(CFieldInst)inst;
                if (!(cfi.field() instanceof CLocal)) {
                    return new SymImm((Linkable)context.translateField(cfi.field()),0);
                }
                break;
            }
            default: break;
            }
        }
            
        return context.tmpForVar(var);
    }
    
    private LArg value(LType type,
                       Arg val,
                       int idx) {
        if (val instanceof Arg.IntConst) {
            Arg.IntConst cnst=(Arg.IntConst)val;
            if (cnst.is32()) {
                return Immediate.make(cnst.value32());
            } else {
                Tmp tmp=context.lc.addTmp(Kind.INT);
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,type,
                        new LArg[]{
                            Immediate.make(cnst.longValue()),
                            tmp
                        }));
                return tmp;
            }
        } else if (val instanceof Arg.Const) {
            // it's a float const
            assert val.type().effectiveBasetype().isFloat;
            throw new CompilerException("Should not see non-integer constants: "+val);
        } else if (type==LType.Byte) {
            Reg result;
            switch (idx) {
            case 0:
                result=Reg.AX;
                break;
            case 1:
                result=Reg.DX;
                break;
            case 2:
                result=Reg.CX;
                break;
            default:
                throw new Error("bad idx: "+idx);
            }
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Long, // use long because that results in the most compact instruction
                    new LArg[]{
                        context.tmpForVar(val),
                        result
                    }));
            return result;
        } else {
            return tmpOrImmFromVar(val);
        }
    }
    
    private LArg value(Basetype type,
                       Arg val,
                       int idx) {
        return value(LType.from(type),val,idx);
    }
    
    private LArg value(Type type,
                       Arg val,
                       int idx) {
        return value(type.effectiveBasetype(),val,idx);
    }
    
    private LArg value(Exectype type,
                       Arg val,
                       int idx) {
        return value(type.effectiveBasetype(),val,idx);
    }

    private LArg value(Arg val,int idx) {
        return value(val.type(),val,idx);
    }
    
    private LArg intTmpValue(LType type,Arg arg) {
        Tmp value;
        if (arg instanceof Arg.IntConst) {
            value=context.lc.addTmp(Kind.INT);
            set(type,value,arg);
        } else {
            value=(Tmp)context.tmpForVar(arg);
        }
        return value;
    }
    
    private void set(LType type,
                     LArg trg,
                     Arg val) {
        if (val instanceof Arg.IntConst) {
            prev.prepend(
                new LOp(
                    LOpCode.Mov,type,
                    new LArg[]{
                        Immediate.make(((Arg.IntConst)val).longValue()),
                        trg
                    }));
        } else {
            prev.prepend(
                new LOp(
                    LOpCode.Mov,type,
                    new LArg[]{
                        tmpOrImmFromVar(val),
                        trg
                    }));
        }
    }
    
    private LArg value(LType type,
                       Reg reg,
                       Arg val) {
        if (val instanceof Arg.IntConst) {
            Arg.IntConst cnst=(Arg.IntConst)val;
            if (cnst.is32()) {
                return Immediate.make(cnst.value32());
            } else {
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,type,
                        new LArg[]{
                            Immediate.make(cnst.longValue()),
                            reg
                        }));
            }
        } else {
            prev.prepend(
                new LOp(
                    LOpCode.Mov,type,
                    new LArg[]{
                        context.tmpForVar(val),
                        reg
                    }));
        }
        return reg;
    }
    
    private LArg simplifyAddr(LArg a) {
        if (a instanceof AbsIndexMem || a instanceof IndexMem) {
            Tmp result=context.lc.addTmp(Kind.INT);
            prev.prepend(
                new LOp(
                    LOpCode.Lea,LType.ptr(),
                    new LArg[]{
                        a,
                        result
                    }));
            return new OffMem(0,result);
        } else {
            return a;
        }
    }
    
    private boolean lea(boolean result,Operation o) {
        if (result) {
            LArg arg=context.baa.result;
            assert arg!=null;
            context.baa.result=null;
            Instruction i=(Instruction)o;
            LType type=LType.from(i.lhs().type());
            if (type.isExec() && type.isInt()) {
                prev.prepend(
                    new LOp(
                        LOpCode.Lea,type,
                        new LArg[]{
                            arg,
                            context.tmpForVar(i.lhs())
                        }));
                return true;
            }
        }
        return false;
    }
    
    private boolean doLoadOpStore(Operation store_,
                                  Operation load_,
                                  Arg value,
                                  LOpCode opcode) {
        if (sideCross()) {
            return false;
        }
        
        MemoryAccessInst store=(MemoryAccessInst)store_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        if (store.getType()==load.getType() &&
            store.getType().effectiveBasetype().isInteger) {
            LArg val;
            switch (opcode) {
            case Shl:
            case Shld:
            case Shr:
            case Shrd:
            case Ushr:
                val=value(LType.from(store.getType()),Reg.CX,value);
                break;
            default:
                val=value(store.getType(),value,0);
                break;
            }
            
            prev.prepend(
                new LOp(
                    opcode,LType.from(store.getType()),
                    new LArg[]{
                        val,
                        address(store.rhs(0))
                    }));
            return true;
        } else {
            return false;
        }
    }
    
    private boolean doLoadOpStore(Operation store_,
                                  Operation load_,
                                  LOpCode opcode) {
        if (sideCross()) {
            return false;
        }
        
        MemoryAccessInst store=(MemoryAccessInst)store_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        if (store.getType()==load.getType() &&
            store.getType().effectiveBasetype().isInteger) {
            prev.prepend(
                new LOp(
                    opcode,LType.from(store.getType()),
                    new LArg[]{
                        address(store.rhs(0))
                    }));
            return true;
        } else {
            return false;
        }
    }
    
    private void doLoad(Operation load,Basetype type,Tmp targ) {
        switch (type) {
        case BOOLEAN:
        case BYTE:
            prev.prepend(
                new LOp(
                    LOpCode.SignExtL,LType.Byte,
                    new LArg[]{
                        address(load.rhs(0)),
                        targ
                    }));
            break;
        case SHORT:
            prev.prepend(
                new LOp(
                    LOpCode.SignExtL,LType.Half,
                    new LArg[]{
                        address(load.rhs(0)),
                        targ
                    }));
            break;
        case CHAR:
            prev.prepend(
                new LOp(
                    LOpCode.ZeroExtL,LType.Half,
                    new LArg[]{
                        address(load.rhs(0)),
                        targ
                    }));
            break;
        default:
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.from(type),
                    new LArg[]{
                        address(load.rhs(0)),
                        targ
                    }));
            break;
        }
    }
    
    private void doLoad(Operation load_,Tmp targ) {
        MemoryAccessInst load=(MemoryAccessInst)load_;
        doLoad(load,load.effectiveBasetype(),targ);
    }
    
    /** Perform an operation of the form Op(value, Load(ptr)), or if it's
        commutative, Op(Load(ptr), value).  For non-commutative operations
        we assume that the Load(ptr) supplies the second operand. */
    private boolean doLoadOp(Operation op_,
                             Instruction load_,
                             Arg value,
                             LOpCode opcode,
                             Commutativity commutativity) {
        if (sideCross()) {
            // NOTE: this is only necessary because we're too stupid to schedule the
            // op by the load.  instead this stupid code generator would have moved
            // a side-crossed Load down to where the op was, causing an epic coherence
            // fail.
            return false;
        }
        
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        Instruction op=(Instruction)op_;
        
        Tmp res=context.tmpForVar(op.lhs());
        
        if (isImm(value) && commutativity.commutable()) {
            // this case eases register allocation ... it reduces the
            // number of places where we have to do register coalescing or
            // scheduling and, probably also buys a register if our allocator
            // does not do scheduling to relieve pressure.  in most cases
            // though, a commutative operation will be handled in a way
            // that does not introduce additional temporaries other than
            // just ones that then need to be coalesced -- so really this
            // is just to reduce the amount of coalescing work.  On the other
            // hand, if this operation wasn't commutative to begin with,
            // then the case else-case of this if statement does the best
            // that it can do.
            //
            // the alternatives are:
            //
            // 1) use the case below and have value() place the immediate in a
            //    temporary.  that's fine, except that: (a) Mov from an immediate
            //    takes more machine code than placing the immediate inside the
            //    operation itself, (b) it results in a non-immediate operation
            //    which may be slower sometimes.
            //
            // 2) fail on this pattern.  but this results in the Load going into
            //    its own temporary, which in the best case gets Mov'd into the
            //    result of the operation prior to the operation's execution.
            //    it's possible that if we're not as smart, we end up requiring
            //    two temporaries (one for the result of the load and one for
            //    the immediate).
            
            doLoad(load,res);
            
            prev.prepend(
                new LOp(
                    opcode,LType.from(op.lhs().type()),
                    new LArg[]{
                        Immediate.make(imm(value)),
                        res
                    }));
        } else {
            switch (load.effectiveBasetype()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHAR:
                if (commutativity.commutable()) {
                    // save ourselves a mov
                    
                    doLoad(load,res);
                    
                    LType type=LType.from(op.lhs().type());
                    
                    prev.prepend(
                        new LOp(
                            opcode,type,
                            new LArg[]{
                                value(type,value,0),
                                res
                            }));
                    return true;
                }
                return false;
            default:
                break;
            }

            set(LType.from(op.lhs().type()),res,value);
            
            prev.prepend(
                new LOp(
                    opcode,LType.from(op.lhs().type()),
                    new LArg[]{
                        address(load.rhs(0)),
                        res
                    }));
        }
        
        return true;
    }
    
    private boolean doDiv(Instruction i,
                          Arg numerator,
                          LArg denominator) {
        assert i.lhs().effectiveBasetype().bytes<=Global.pointerSize : i;
        assert i.lhs().effectiveBasetype().isInteger : i;
        
        LType type=LType.from(i.lhs().type());
        
        set(type,Reg.DX,numerator);
        
        prev.prepend(
            new LOp(
                LOpCode.Mov,type,
                new LArg[]{
                    Reg.DX,
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.Shr,type,
                new LArg[]{
                    Immediate.make(31),
                    Reg.DX
                }));
        
        prev.prepend(
            new LOp(
                LOpCode.Div,type,
                new LArg[]{
                    denominator
                }));
        
        switch (i.opcode()) {
        case Div:
            prev.prepend(
                new LOp(
                    LOpCode.Mov,type,
                    new LArg[]{
                        Reg.AX,
                        context.tmpForVar(i.lhs())
                    }));
            break;
        case Mod:
            prev.prepend(
                new LOp(
                    LOpCode.Mov,type,
                    new LArg[]{
                        Reg.DX,
                        context.tmpForVar(i.lhs())
                    }));
            break;
        default:
            throw new CompilerException("bad op: "+i);
        }
        
        return true;
    }
    
    private boolean doLoadDiv(Operation op_,
                              Instruction load_,
                              Arg value) {
        if (sideCross()) {
            return false;
        }
        
        Instruction op=(Instruction)op_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        switch (load.effectiveBasetype()) {
        case BOOLEAN:
        case BYTE:
        case SHORT:
        case CHAR:
            return false;
        default:
            break;
        }
        
        return doDiv(op,
                     value,
                     address(load.rhs(0)));
    }
    
    private boolean doBoolAndLoad(Operation resOp_,
                                  Instruction load_,
                                  Arg value,
                                  boolean not) {
        Instruction resOp=(Instruction)resOp_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        Tmp res=context.tmpForVar(resOp.lhs());
        
        if (context.code.getBoolResult().get(value) ||
            load.getType()==Type.BOOLEAN) {
            
            doLoad(load,res);
            
            LType type=LType.from(load.getExectype());
            
            prev.prepend(
                new LOp(
                    LOpCode.And,type,
                    new LArg[]{
                        value(type, value, 0),
                        res
                    }));
            
            if (not) {
                prev.prepend(
                    new LOp(
                        LOpCode.Xor,type,
                        new LArg[]{
                            Immediate.make(1),
                            res
                        }));
            }
        } else {
            // Is this correct for when the type is byte, short, or char?
            // Probably for char, it would be, but I'm not taking any chances.
            
            switch (load.effectiveBasetype()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHAR:
                return false;
            default:
                break;
            }
            
            LType type=LType.from(load.getType());
            
            prev.prepend(
                new LOp(
                    not?LOpCode.SetAndZero:LOpCode.SetAndNotZero,
                    type,
                    new LArg[]{
                        address(load.rhs(0)),
                        value(type, value, 0),
                        Reg.AX
                    }));
            prev.prepend(
                new LOp(
                    LOpCode.ZeroExtL,LType.Byte,
                    new LArg[]{
                        Reg.AX,
                        res
                    }));
        }
        
        return true;
    }
    
    private boolean doBoolAnd(Operation resOp_,
                              Instruction and,
                              boolean not) {
        Instruction resOp=(Instruction)resOp_;
        
        LType type=LType.from(and.lhs().type());
        Tmp res=context.tmpForVar(resOp.lhs());
            
        if (context.code.getBoolResult().get(and.rhs(0)) ||
            context.code.getBoolResult().get(and.rhs(1))) {
            prev.prepend(
                new LOp(
                    LOpCode.Mov,type,
                    new LArg[]{
                        value(type,and.rhs(0),0),
                        res
                    }));
            prev.prepend(
                new LOp(
                    LOpCode.And,type,
                    new LArg[]{
                        value(type,and.rhs(1),1),
                        res
                    }));
            
            if (not) {
                prev.prepend(
                    new LOp(
                        LOpCode.Xor,type,
                        new LArg[]{
                            Immediate.make(1),
                            res
                        }));
            }
        } else {
            
            LArg aa=value(type,and.rhs(0),0);
            LArg ba=value(type,and.rhs(1),1);
            
            if (aa.immediate()) {
                LArg tmp=aa;
                aa=ba;
                ba=tmp;
            }
            
            prev.prepend(
                new LOp(
                    not?LOpCode.SetAndZero:LOpCode.SetAndNotZero,
                    type,
                    new LArg[]{
                        value(type,and.rhs(0),0),
                        value(type,and.rhs(1),1),
                        Reg.AX
                    }));
            prev.prepend(
                new LOp(
                    LOpCode.ZeroExtL,LType.Byte,
                    new LArg[]{
                        Reg.AX,
                        res
                    }));
        }
        
        return true;
    }
    
    private boolean doOp(Operation o,LOpCode opcode) {
        Instruction i=(Instruction)o;
        Tmp res=context.tmpForVar(i.lhs());
        LType type=LType.from(i.lhs().type());
        
        set(type,res,i.rhs(0));
        
        switch (i.nrhs()) {
        case 1:
            prev.prepend(
                new LOp(
                    opcode,type,
                    new LArg[]{
                        res
                    }));
            break;
        case 2:
            prev.prepend(
                new LOp(
                    opcode,type,
                    new LArg[]{
                        value(type,i.rhs(1),0),
                        res
                    }));
            break;
        default:
            throw new CompilerException("bad op: "+i);
        }
        
        return true;
    }
    
    private boolean doShift(Operation shift_,LOpCode opcode) {
        Instruction shift=(Instruction)shift_;
        Tmp res=context.tmpForVar(shift.lhs());
        LType type=LType.from(shift.lhs().type());
        LArg shiftAmount;
        
        if (isImm(shift.rhs(1))) {
            int shiftImm=imm(shift.rhs(1));
            
            if (opcode==LOpCode.Shl && shiftImm==1 && type.isLikeAddress()) {
                LArg value=value(type,shift.rhs(0),0);
                
                prev.prepend(
                    new LOp(
                        LOpCode.Lea,type,
                        new LArg[]{
                            new IndexMem(0,value,value,Scale.ONE),
                            res
                        }));
                return true;
            }
            
            shiftAmount=Immediate.make(shiftImm);
        } else {
            set(LType.Long,Reg.CX,shift.rhs(1));
            shiftAmount=Reg.CX;
        }
        
        set(type,res,shift.rhs(0));
        
        prev.prepend(
            new LOp(
                opcode,type,
                new LArg[]{
                    shiftAmount,
                    res
                }));
        
        return true;
    }
    
    private boolean doBranchCmp(Operation branch_,
                                Arg a,
                                Arg b,
                                LOpCode branchOp) {
        Branch branch=(Branch)branch_;
        
        assert a.type()==b.type();
        
        LType type=LType.from(a.type());
        
        LArg aa=value(type,a,0);
        LArg ba=value(type,b,1);
        
        if (aa.immediate()) {
            LArg tmp=aa;
            aa=ba;
            ba=tmp;
            branchOp=branchOp.swapRhs();
            assert branchOp!=null;
        }
        
        lh.setFooter(
            new LFooter(
                branchOp,type,
                new LArg[]{
                    aa,
                    ba
                },
                new LHeader[]{
                    context.headMap.get(branch.defaultSuccessor()),
                    context.headMap.get(branch.target())
                }));
        
        return true;
    }
    
    private boolean doBranchCmpAddrVal(Operation branch_,
                                       Instruction load_,
                                       Arg value,
                                       LOpCode branchOp) {
        if (sideCross()) {
            return false;
        }
        
        Branch branch=(Branch)branch_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        switch (load.effectiveBasetype()) {
        case BOOLEAN:
        case BYTE:
        case SHORT:
        case CHAR:
            if (!load.effectiveBasetype().isAssignableFrom(
                    context.rtc.getExact(value).effectiveBasetype())) {
                return false;
            }
        default:
            break;
        }
        
        LType type=LType.from(load.getType());
        
        if (branchOp.form()==OpForm.FLOAT_BRANCH) {
            assert branchOp.swapRhs()==null;
            return false;
        } else {
            assert branchOp.form()==OpForm.BRANCH;
            
            lh.setFooter(
                new LFooter(
                    branchOp,type,
                    new LArg[]{
                        address(load.rhs(0)),
                        value(type,value,0)
                    },
                    new LHeader[]{
                        context.headMap.get(branch.defaultSuccessor()),
                        context.headMap.get(branch.target())
                    }));
        }
        
        return true;
    }
    
    private boolean doBranchCmpValAddr(Operation branch_,
                                       Instruction load_,
                                       Arg value,
                                       LOpCode branchOp) {
        if (sideCross()) {
            return false;
        }
        
        Branch branch=(Branch)branch_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        switch (load.effectiveBasetype()) {
        case BOOLEAN:
        case BYTE:
        case SHORT:
        case CHAR:
            if (!load.effectiveBasetype().isAssignableFrom(
                    context.rtc.getExact(value).effectiveBasetype())) {
                return false;
            }
        default:
            break;
        }

        LType type=LType.from(load.getType());
        
        if (branchOp.form()==OpForm.FLOAT_BRANCH) {
            lh.setFooter(
                new LFooter(
                    branchOp,type,
                    new LArg[]{
                        value(type,value,0),
                        address(load.rhs(0))
                    },
                    new LHeader[]{
                        context.headMap.get(branch.defaultSuccessor()),
                        context.headMap.get(branch.target())
                    }));
        } else {
            assert branchOp.form()==OpForm.BRANCH;
            
            lh.setFooter(
                new LFooter(
                    branchOp.swapRhs(),type,
                    new LArg[]{
                        address(load.rhs(0)),
                        value(type,value,0)
                    },
                    new LHeader[]{
                        context.headMap.get(branch.defaultSuccessor()),
                        context.headMap.get(branch.target())
                    }));
        }
        
        return true;
    }
    
    private boolean doBranchFEqLoad(Operation branch_,
                                    Instruction load_,
                                    Arg value) {
        if (sideCross()) {
            return false;
        }
        
        Branch branch=(Branch)branch_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        LType type=LType.from(load.getType());
        
        LHeader orderCheck=lh.makeSimilar();
        
        lh.setFooter(
            new LFooter(
                LOpCode.BranchFEqOrUnordered,type,
                new LArg[]{
                    value(type,value,0),
                    address(load.rhs(0))
                },
                new LHeader[]{
                    context.headMap.get(branch.defaultSuccessor()),
                    orderCheck
                }));
        
        orderCheck.setFooter(
            new LFooter(
                LOpCode.RebranchFOrdered,type,
                LArg.EMPTY,
                new LHeader[]{
                    context.headMap.get(branch.defaultSuccessor()),
                    context.headMap.get(branch.target())
                }));
        
        return true;
    }
    
    private boolean doBranchFNeqLoad(Operation branch_,
                                     Instruction load_,
                                     Arg value) {
        if (sideCross()) {
            return false;
        }
        
        Branch branch=(Branch)branch_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        LType type=LType.from(load.getType());
        
        LHeader orderCheck=lh.makeSimilar();
        
        lh.setFooter(
            new LFooter(
                LOpCode.BranchFNeqAndOrdered,type,
                new LArg[]{
                    value(type,value,0),
                    address(load.rhs(0))
                },
                new LHeader[]{
                    orderCheck,
                    context.headMap.get(branch.target())
                }));
        
        orderCheck.setFooter(
            new LFooter(
                LOpCode.RebranchFUnordered,type,
                LArg.EMPTY,
                new LHeader[]{
                    context.headMap.get(branch.defaultSuccessor()),
                    context.headMap.get(branch.target())
                }));
        
        return true;
    }
    
    private boolean doBranchFEq(Operation branch_,
                                Arg a,
                                Arg b) {
        Branch branch=(Branch)branch_;
        
        LType type=LType.from(a.type());
        
        LHeader orderCheck=lh.makeSimilar();
        
        lh.setFooter(
            new LFooter(
                LOpCode.BranchFEqOrUnordered,type,
                new LArg[]{
                    value(type,a,0),
                    value(type,b,1)
                },
                new LHeader[]{
                    context.headMap.get(branch.defaultSuccessor()),
                    orderCheck
                }));
        
        orderCheck.setFooter(
            new LFooter(
                LOpCode.RebranchFOrdered,type,
                LArg.EMPTY,
                new LHeader[]{
                    context.headMap.get(branch.defaultSuccessor()),
                    context.headMap.get(branch.target())
                }));
        
        return true;
    }
    
    private boolean doBranchFNeq(Operation branch_,
                                 Arg a,
                                 Arg b) {
        Branch branch=(Branch)branch_;
        
        LType type=LType.from(a.type());
        
        LHeader orderCheck=lh.makeSimilar();
        
        lh.setFooter(
            new LFooter(
                LOpCode.BranchFNeqAndOrdered,type,
                new LArg[]{
                    value(type,a,0),
                    value(type,b,1)
                },
                new LHeader[]{
                    orderCheck,
                    context.headMap.get(branch.target()),
                }));
        
        orderCheck.setFooter(
            new LFooter(
                LOpCode.RebranchFUnordered,type,
                LArg.EMPTY,
                new LHeader[]{
                    context.headMap.get(branch.defaultSuccessor()),
                    context.headMap.get(branch.target())
                }));
        
        return true;
    }
    
    private boolean doBranchCAS(Operation branch_,
                                Instruction cas_,
                                boolean succ) {
        if (sideCross()) {
            return false;
        }
        
        Branch branch=(Branch)branch_;
        MemoryAccessInst cas=(MemoryAccessInst)cas_;
        LType type=LType.from(cas.effectiveBasetype());
        
        set(type,Reg.AX,cas.rhs(1));
        
        lh.setFooter(
            new LFooter(
                succ?LOpCode.BranchCASSucc:LOpCode.BranchCASFail,type,
                new LArg[]{
                    address(cas.rhs(0)),
                    intTmpValue(type,cas.rhs(2))
                },
                new LHeader[]{
                    context.headMap.get(branch.defaultSuccessor()),
                    context.headMap.get(branch.target())
                }));
        
        return true;
    }
    
    private boolean doSetCmpAddrVal(Operation cmp_,
                                    Instruction load_,
                                    Arg value,
                                    LOpCode setOp) {
        if (sideCross()) {
            return false;
        }
        
        Instruction cmp=(Instruction)cmp_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        switch (load.effectiveBasetype()) {
        case BOOLEAN:
        case BYTE:
        case SHORT:
        case CHAR:
            if (!load.effectiveBasetype().isAssignableFrom(
                    context.rtc.getExact(value).effectiveBasetype())) {
                return false;
            }
        default:
            break;
        }
        
        LType type=LType.from(load.getType());
        
        if (setOp.form()==OpForm.FLOAT_COMPARE) {
            assert setOp.swapRhs()==null;
            return false;
        } else {
            assert setOp.form()==OpForm.COMPARE;
            
            prev.prepend(
                new LOp(
                    setOp,type,
                    new LArg[]{
                        address(load.rhs(0)),
                        value(type,value,0),
                        Reg.AX
                    }));
        }
        prev.prepend(
            new LOp(
                LOpCode.SignExtL,LType.Byte,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cmp.lhs())
                }));
        
        return true;
    }

    private boolean doSetCmpValAddr(Operation cmp_,
                                    Instruction load_,
                                    Arg value,
                                    LOpCode setOp) {
        if (sideCross()) {
            return false;
        }
        
        Instruction cmp=(Instruction)cmp_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        switch (load.effectiveBasetype()) {
        case BOOLEAN:
        case BYTE:
        case SHORT:
        case CHAR:
            if (!load.effectiveBasetype().isAssignableFrom(
                    context.rtc.getExact(value).effectiveBasetype())) {
                return false;
            }
        default:
            break;
        }
        
        LType type=LType.from(load.getType());
        
        if (setOp.form()==OpForm.FLOAT_COMPARE) {
            prev.prepend(
                new LOp(
                    setOp,type,
                    new LArg[]{
                        value(type,value,0),
                        address(load.rhs(0)),
                        Reg.AX
                    }));
        } else {
            assert setOp.form()==OpForm.COMPARE;

            prev.prepend(
                new LOp(
                    setOp.swapRhs(),type,
                    new LArg[]{
                        address(load.rhs(0)),
                        value(type,value,0),
                        Reg.AX
                    }));
        }
        prev.prepend(
            new LOp(
                LOpCode.SignExtL,LType.Byte,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cmp.lhs())
                }));
        
        return true;
    }
    
    private boolean doSetCmp(Operation cmp_,
                             Arg a,
                             Arg b,
                             LOpCode setOp) {
        Instruction cmp=(Instruction)cmp_;

        LType type=LType.from(a.type());
        
        LArg aa=value(type,a,0);
        LArg ba=value(type,b,1);
        
        if (aa.immediate()) {
            LArg tmp=aa;
            aa=ba;
            ba=tmp;
            setOp=setOp.swapRhs();
            assert setOp!=null;
        }
        
        prev.prepend(
            new LOp(
                setOp,type,
                new LArg[]{
                    aa,
                    ba,
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.SignExtL,LType.Byte,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cmp.lhs())
                }));
        
        return true;
    }
    
    private boolean doSetFEqLoad(Operation cmp_,
                                 Instruction load_,
                                 Arg value) {
        if (sideCross()) {
            return false;
        }
        
        Instruction cmp=(Instruction)cmp_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        LType type=LType.from(load.getType());
        
        prev.prepend(
            new LOp(
                LOpCode.SetFEqOrUnordered,type,
                new LArg[]{
                    value(type,value,0),
                    address(load.rhs(0)),
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.ResetFOrdered,LType.Byte,
                new LArg[]{
                    Reg.DX
                }));
        prev.prepend(
            new LOp(
                LOpCode.And,LType.Byte,
                new LArg[]{
                    Reg.DX,
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.SignExtL,LType.Byte,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cmp.lhs())
                }));
        
        return true;
    }
    
    private boolean doSetFNeqLoad(Operation cmp_,
                                  Instruction load_,
                                  Arg value) {
        if (sideCross()) {
            return false;
        }
        
        Instruction cmp=(Instruction)cmp_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        LType type=LType.from(load.getType());
        
        prev.prepend(
            new LOp(
                LOpCode.SetFNeqAndOrdered,type,
                new LArg[]{
                    value(type,value,0),
                    address(load.rhs(0)),
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.ResetFUnordered,LType.Byte,
                new LArg[]{
                    Reg.DX
                }));
        prev.prepend(
            new LOp(
                LOpCode.Or,LType.Byte,
                new LArg[]{
                    Reg.DX,
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.SignExtL,LType.Byte,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cmp.lhs())
                }));
        
        return true;
    }
    
    private boolean doSetFEq(Operation cmp_,
                             Arg a,
                             Arg b) {
        Instruction cmp=(Instruction)cmp_;
        
        LType type=LType.from(a.type());
        
        prev.prepend(
            new LOp(
                LOpCode.SetFEqOrUnordered,type,
                new LArg[]{
                    value(type,a,0),
                    value(type,b,1),
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.ResetFOrdered,LType.Byte,
                new LArg[]{
                    Reg.DX
                }));
        prev.prepend(
            new LOp(
                LOpCode.And,LType.Byte,
                new LArg[]{
                    Reg.DX,
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.SignExtL,LType.Byte,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cmp.lhs())
                }));
        
        return true;
    }
    
    private boolean doSetFNeq(Operation cmp_,
                              Arg a,
                              Arg b) {
        Instruction cmp=(Instruction)cmp_;
        
        LType type=LType.from(a.type());
        
        prev.prepend(
            new LOp(
                LOpCode.SetFNeqAndOrdered,type,
                new LArg[]{
                    value(type,a,0),
                    value(type,b,1),
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.ResetFUnordered,LType.Byte,
                new LArg[]{
                    Reg.DX
                }));
        prev.prepend(
            new LOp(
                LOpCode.Or,LType.Byte,
                new LArg[]{
                    Reg.DX,
                    Reg.AX
                }));
        prev.prepend(
            new LOp(
                LOpCode.SignExtL,LType.Byte,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cmp.lhs())
                }));
        
        return true;
    }
    
    private void pushArgs(NativeSignature sig,
                          Instruction call,
                          int firstArgIdx) {
        int offset=0;
        for (int i=0;i<sig.nparams();++i) {
            Basetype bt=sig.param(i);
            Arg a=call.rhs(firstArgIdx+i);
            
            if (bt.isFloat && a instanceof Arg.Const) {
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,LType.from(Kind.INT,bt.bytes),
                        new LArg[]{
                            Immediate.make(((Arg.Const)a).fiatToLong()),
                            new OutParamSlot(offset)
                        }));
            } else {
                set(LType.from(bt.asExectype),
                    new OutParamSlot(offset),
                    a);
            }
            
            if (bt.bytes < Global.pointerSize) {
                offset+=Global.pointerSize;
            } else {
                offset+=bt.bytes;
            }
        }
    }
    
    private boolean doMov(Operation mov_) {
        Instruction mov=(Instruction)mov_;
        set(LType.from(mov.lhs().type()),
            context.tmpForVar(mov.lhs()),
            mov.rhs(0));
        return true;
    }
    
    private void popRes(NativeSignature sig,
                        Instruction call) {
        if (context.uc.notUsed(call.lhs())) {
            return;
        }
        
        switch (sig.result()) {
        case VOID:
            break;
        case BOOLEAN:
        case BYTE:
            prev.prepend(
                new LOp(
                    LOpCode.SignExtL,LType.Byte,
                    new LArg[]{
                        Reg.AX,
                        context.tmpForVar(call.lhs())
                    }));
            break;
        case SHORT:
            prev.prepend(
                new LOp(
                    LOpCode.SignExtL,LType.Half,
                    new LArg[]{
                        Reg.AX,
                        context.tmpForVar(call.lhs())
                    }));
            break;
        case CHAR:
            prev.prepend(
                new LOp(
                    LOpCode.ZeroExtL,LType.Half,
                    new LArg[]{
                        Reg.AX,
                        context.tmpForVar(call.lhs())
                    }));
            break;
        case INT:
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Long,
                    new LArg[]{
                        Reg.AX,
                        context.tmpForVar(call.lhs())
                    }));
            break;
        case LONG:
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Quad,
                    new LArg[]{
                        Reg.AX,
                        context.tmpForVar(call.lhs())
                    }));
            break;
        case POINTER:
        case VM_FCPTR:
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.ptr(),
                    new LArg[]{
                        Reg.AX,
                        context.tmpForVar(call.lhs())
                    }));
            break;
        case FLOAT:
            if (Global.pointerSize==4) {
                prev.prepend(
                    new LOp(
                        LOpCode.PopFP,LType.Single,
                        new LArg[]{
                            new OffScratchSlot(context.memBounce(),0)
                        }));
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,LType.Single,
                        new LArg[]{
                            new OffScratchSlot(context.memBounce(),0),
                            context.tmpForVar(call.lhs())
                        }));
            } else {
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,LType.Single,
                        new LArg[]{
                            Reg.XMM0,
                            context.tmpForVar(call.lhs())
                        }));
            }
            break;
        case DOUBLE:
            if (Global.pointerSize==4) {
                prev.prepend(
                    new LOp(
                        LOpCode.PopFP,LType.Double,
                        new LArg[]{
                            new OffScratchSlot(context.memBounce(),0)
                        }));
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,LType.Double,
                        new LArg[]{
                            new OffScratchSlot(context.memBounce(),0),
                            context.tmpForVar(call.lhs())
                        }));
            } else {
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,LType.Double,
                        new LArg[]{
                            Reg.XMM0,
                            context.tmpForVar(call.lhs())
                        }));
            }
            break;
        default: throw new CompilerException("bad type: "+sig.result());
        }
    }

    /** loadAddStore1 := Store(a, Add(Load(a), b)) */
    protected boolean visitLoadAddStore1(Operation store,
                                         Instruction add,
                                         Instruction load) {
        return doLoadOpStore(store,load,add.rhs(1),LOpCode.Add);
    }
    /** loadAddStore2 := Store(a, Add(!%b, Load(a))) */
    protected boolean visitLoadAddStore2(Operation store,
                                         Instruction add,
                                         Instruction load) {
        return doLoadOpStore(store,load,add.rhs(0),LOpCode.Add);
    }
    /** loadSubStore := Store(a, Sub(Load(a), b)) */
    protected boolean visitLoadSubStore(Operation store,
                                        Instruction sub,
                                        Instruction load) {
        return doLoadOpStore(store,load,sub.rhs(1),LOpCode.Sub);
    }
    /** loadNegStore := Store(a, Neg(Load(a))) */
    protected boolean visitLoadNegStore(Operation store,
                                        Instruction neg,
                                        Instruction load) {
        return doLoadOpStore(store,load,LOpCode.Neg);
    }
    /** loadShlStore := Store(a, Shl(Load(a), b)) */
    protected boolean visitLoadShlStore(Operation store,
                                        Instruction shl,
                                        Instruction load) {
        return doLoadOpStore(store,load,shl.rhs(1),LOpCode.Shl);
    }
    /** loadShrStore := Store(a, Shr(Load(a), b)) */
    protected boolean visitLoadShrStore(Operation store,
                                        Instruction shr,
                                        Instruction load) {
        return doLoadOpStore(store,load,shr.rhs(1),LOpCode.Shr);
    }
    /** loadUshrStore := Store(a, Ushr(Load(a), b)) */
    protected boolean visitLoadUshrStore(Operation store,
                                         Instruction ushr,
                                         Instruction load) {
        return doLoadOpStore(store,load,ushr.rhs(1),LOpCode.Ushr);
    }
    /** loadOrStore1 := Store(a, Or(Load(a), b)) */
    protected boolean visitLoadOrStore1(Operation store,
                                        Instruction or,
                                        Instruction load) {
        return doLoadOpStore(store,load,or.rhs(1),LOpCode.Or);
    }
    /** loadOrStore2 := Store(a, Or(!%b, Load(a))) */
    protected boolean visitLoadOrStore2(Operation store,
                                        Instruction or,
                                        Instruction load) {
        return doLoadOpStore(store,load,or.rhs(0),LOpCode.Or);
    }
    /** loadXorStore1 := Store(a, Xor(Load(a), b)) */
    protected boolean visitLoadXorStore1(Operation store,
                                         Instruction xor,
                                         Instruction load) {
        return doLoadOpStore(store,load,xor.rhs(1),LOpCode.Xor);
    }
    /** loadXorStore2 := Store(a, Xor(!%b, Load(a))) */
    protected boolean visitLoadXorStore2(Operation store,
                                         Instruction xor,
                                         Instruction load) {
        return doLoadOpStore(store,load,xor.rhs(0),LOpCode.Xor);
    }
    /** loadAndStore1 := Store(a, And(Load(a), b)) */
    protected boolean visitLoadAndStore1(Operation store,
                                         Instruction and,
                                         Instruction load) {
        return doLoadOpStore(store,load,and.rhs(1),LOpCode.And);
    }
    /** loadAndStore2 := Store(a, And(!%b, Load(a))) */
    protected boolean visitLoadAndStore2(Operation store,
                                         Instruction and,
                                         Instruction load) {
        return doLoadOpStore(store,load,and.rhs(0),LOpCode.And);
    }
    /** loadBitNotStore := Store(a, BitNot(Load(a))) */
    protected boolean visitLoadBitNotStore(Operation store,
                                           Instruction bitNot,
                                           Instruction load) {
        return doLoadOpStore(store,load,LOpCode.BitNot);
    }
    /** loadAdd1 := Add(Load(a), b) */
    protected boolean visitLoadAdd1(Operation add,
                                    Instruction load) {
        if (((Instruction)add).lhs().type().effectiveBasetype().isFloat) {
            return doLoadOp(add,load,add.rhs(1),LOpCode.FAdd,Commutativity.COMMUTABLE);
        } else {
            return doLoadOp(add,load,add.rhs(1),LOpCode.Add,Commutativity.COMMUTABLE);
        }
    }
    /** loadMul1 := Mul(Load(a), b) */
    protected boolean visitLoadMul1(Operation mul,
                                    Instruction load) {
        if (((Instruction)mul).lhs().type().effectiveBasetype().isFloat) {
            return doLoadOp(mul,load,mul.rhs(1),LOpCode.FMul,Commutativity.COMMUTABLE);
        } else {
            return doLoadOp(mul,load,mul.rhs(1),LOpCode.Mul,Commutativity.COMMUTABLE);
        }
    }
    /** cVar := GetCVarAddress() */
    protected boolean visitCVar(Operation getCVarAddress) {
        // this is bizarre as heck.  we want to be careful here:
        //    this should emit a mov instruction rather than a lea instruction
        //    in some cases: in 64-bit mode, we want a lea for 32-bit immediates
        //    and a mov for 64-bit immediates.  in 32-bit mode, we should always
        //    emit a mov since it is shorter.
        // ... or not.  revisit this later.
        
        CFieldInst cfi=(CFieldInst)getCVarAddress;
        
        if (cfi.field() instanceof CLocal) {
            prev.prepend(
                new LOp(
                    LOpCode.Lea,LType.from(cfi.lhs().type()),
                    new LArg[]{
                        new OffScratchSlot(context.localMap.get(cfi.field()),0),
                        context.tmpForVar(cfi.lhs())
                    }));
        } else {
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.from(cfi.lhs().type()),
                    new LArg[]{
                        new SymImm((Linkable)context.translateField(cfi.field()),0),
                        context.tmpForVar(cfi.lhs())
                    }));
        }
        
        return true;
    }
    /** addCVar := Add(GetCVarAddress(), a) */
    protected boolean visitAddCVar(Operation add,
                                   Instruction getCVarAddress) {
        return lea(context.baa.visitAddCVar(add,getCVarAddress),add);
    }
    /** addAddCVar := add1=Add(add2=Add(GetCVarAddress(), %a), $b) */
    protected boolean visitAddAddCVar(Operation add1,
                                      Instruction add2,
                                      Instruction getCVarAddress) {
        return lea(context.baa.visitAddAddCVar(add1,add2,getCVarAddress),add1);
    }
    /** cArg := GetCArgAddress() */
    protected boolean visitCArg(Operation getCArgAddress) {
        return lea(context.baa.visitCArg(getCArgAddress),getCArgAddress);
    }
    /** addCArg := Add(GetCArgAddress(),$a) */
    protected boolean visitAddCArg(Operation add,
                                   Instruction getCArgAddress) {
        return lea(context.baa.visitAddCArg(add,getCArgAddress),add);
    }
    /** addAddAdd := add1=Add(add2=Add(add3=Add(!%a, $b), $c), $d) */
    protected boolean visitAddAddAdd(Operation add1,
                                     Instruction add2,
                                     Instruction add3) {
        return lea(context.baa.visitAddAddAdd(add1,add2,add3),add1);
    }
    /** addAddShl1 := add1=Add(add2=Add(Shl(!%a, $b), %c), $d) */
    protected boolean visitAddAddShl1(Operation add1,
                                      Instruction add2,
                                      Instruction shl) {
        return lea(context.baa.visitAddAddShl1(add1,add2,shl),add1);
    }
    /** addAddShl2 := add1=Add(add2=Add(!%a, Shl(!%b, $c)), $d) */
    protected boolean visitAddAddShl2(Operation add1,
                                      Instruction add2,
                                      Instruction shl) {
        return lea(context.baa.visitAddAddShl2(add1,add2,shl),add1);
    }
    /** addAddShl3 := add1=Add(add2=Add(!%a, $b), Shl(!%c, $d)) */
    protected boolean visitAddAddShl3(Operation add1,
                                      Instruction add2,
                                      Instruction shl) {
        return lea(context.baa.visitAddAddShl3(add1,add2,shl),add1);
    }
    /** addShl1 := Add(Shl(!%a, $b), $c) */
    protected boolean visitAddShl1(Operation add,
                                   Instruction shl) {
        return lea(context.baa.visitAddShl1(add,shl),add);
    }
    /** addAdd1 := add1=Add(add2=Add(!%a, %b), $c) */
    protected boolean visitAddAdd1(Operation add1,
                                   Instruction add2) {
        return lea(context.baa.visitAddAdd1(add1,add2),add1);
    }
    /** addShl2 := Add(Shl(!%a, $b), %c) */
    protected boolean visitAddShl2(Operation add,
                                   Instruction shl) {
        return lea(context.baa.visitAddShl2(add,shl),add);
    }
    /** addAdd2 := add1=Add(add2=Add(!%a, $b), $c) */
    protected boolean visitAddAdd2(Operation add1,
                                   Instruction add2) {
        return lea(context.baa.visitAddAdd2(add1,add2),add1);
    }
    /** addShl3 := Add(!%a, Shl(!%b, $c)) */
    protected boolean visitAddShl3(Operation add,
                                   Instruction shl) {
        return lea(context.baa.visitAddShl3(add,shl),add);
    }
    /** add := Add(a, b) */
    protected boolean visitAdd(Operation add) {
        if (lea(context.baa.visitAdd(add),add)) {
            return true;
        } else {
            Instruction i=(Instruction)add;
            LOpCode opcode;
            if (i.lhs().type().effectiveBasetype().isFloat) {
                opcode=LOpCode.FAdd;
            } else {
                opcode=LOpCode.Add;
            }
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.from(i.lhs().type()),
                    new LArg[]{
                        context.tmpForVar(i.rhs(0)),
                        context.tmpForVar(i.lhs())
                    }));
            prev.prepend(
                new LOp(
                    opcode,LType.from(i.lhs().type()),
                    new LArg[]{
                        value(i.lhs().type(),i.rhs(1),0),
                        context.tmpForVar(i.lhs())
                    }));
            return true;
        }
    }
    /** loadAdd2 := Add(!%a, Load(b)) */
    protected boolean visitLoadAdd2(Operation add,
                                    Instruction load) {
        if (((Instruction)add).lhs().type().effectiveBasetype().isFloat) {
            return doLoadOp(add,load,add.rhs(0),LOpCode.FAdd,Commutativity.COMMUTABLE);
        } else {
            return doLoadOp(add,load,add.rhs(0),LOpCode.Add,Commutativity.COMMUTABLE);
        }
    }
    /** loadMul2 := Mul(!%a, Load(b)) */
    protected boolean visitLoadMul2(Operation mul,
                                    Instruction load) {
        if (((Instruction)mul).lhs().type().effectiveBasetype().isFloat) {
            return doLoadOp(mul,load,mul.rhs(0),LOpCode.FMul,Commutativity.COMMUTABLE);
        } else {
            return doLoadOp(mul,load,mul.rhs(0),LOpCode.Mul,Commutativity.COMMUTABLE);
        }
    }
    /** loadSub := Sub(a, Load(b)) */
    protected boolean visitLoadSub(Operation sub,
                                   Instruction load) {
        if (((Instruction)sub).lhs().type().effectiveBasetype().isFloat) {
            return doLoadOp(sub,load,sub.rhs(0),LOpCode.FSub,Commutativity.NOT_COMMUTABLE);
        } else {
            return doLoadOp(sub,load,sub.rhs(0),LOpCode.Sub,Commutativity.NOT_COMMUTABLE);
        }
    }
    /** loadDiv := Div(a, Load(b)) */
    protected boolean visitLoadDiv(Operation div,
                                   Instruction load) {
        if (((Instruction)div).lhs().type().effectiveBasetype().isFloat) {
            return doLoadOp(div,load,div.rhs(0),LOpCode.FDiv,Commutativity.NOT_COMMUTABLE);
        } else {
            return doLoadDiv(div,load,div.rhs(0));
        }
    }
    /** loadMod := Mod(a, Load(b)) */
    protected boolean visitLoadMod(Operation mod,
                                   Instruction load) {
        return doLoadDiv(mod,load,mod.rhs(0));
    }
    /** loadOr1 := Or(Load(a), b) */
    protected boolean visitLoadOr1(Operation or,
                                   Instruction load) {
        return doLoadOp(or,load,or.rhs(1),LOpCode.Or,Commutativity.COMMUTABLE);
    }
    /** loadOr2 := Or(a, Load(b)) */
    protected boolean visitLoadOr2(Operation or,
                                   Instruction load) {
        return doLoadOp(or,load,or.rhs(0),LOpCode.Or,Commutativity.COMMUTABLE);
    }
    /** loadXor1 := Xor(Load(a), b) */
    protected boolean visitLoadXor1(Operation xor,
                                    Instruction load) {
        return doLoadOp(xor,load,xor.rhs(1),LOpCode.Xor,Commutativity.COMMUTABLE);
    }
    /** loadXor2 := Xor(a, Load(b)) */
    protected boolean visitLoadXor2(Operation xor,
                                    Instruction load) {
        return doLoadOp(xor,load,xor.rhs(0),LOpCode.Xor,Commutativity.COMMUTABLE);
    }
    /** loadAnd1 := And(Load(a), b) */
    protected boolean visitLoadAnd1(Operation and,
                                    Instruction load) {
        return doLoadOp(and,load,and.rhs(1),LOpCode.And,Commutativity.COMMUTABLE);
    }
    /** loadAnd2 := And(a, Load(b)) */
    protected boolean visitLoadAnd2(Operation and,
                                    Instruction load) {
        return doLoadOp(and,load,and.rhs(0),LOpCode.And,Commutativity.COMMUTABLE);
    }
    /** loadNot := Not(Load(a)) */
    protected boolean visitLoadNot(Operation not_,
                                   Instruction load_) {
        if (sideCross()) {
            return false;
        }
        
        Instruction not=(Instruction)not_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        // this is not particularly fun...
        // we should have a special case for loading a boolean.  if that happens,
        // then the Not can be executed as a Xor.  this is important, since we
        // don't want OptPeephole to worry about such things.

        Tmp res=context.tmpForVar(not.lhs());
        
        if (load.getType()==Type.BOOLEAN) {

            doLoad(load,res);

            prev.prepend(
                new LOp(
                    LOpCode.Xor,LType.Long,
                    new LArg[]{
                        Immediate.make(1),
                        res
                    }));
        } else {
            // ok.  we need to emit a compare/set thingy but first we
            // need to move 0 into the result.  the size of the 0 should be
            // the size of the type.  but it gets worse: we can only use
            // EAX and stuff...
            
            // here's how we do it: punt on the Xor optimization for now.  just
            // always use AX as the result of the compare, and then use ZeroExt
            // to move it into the result.
            
            prev.prepend(
                new LOp(
                    LOpCode.SetEq,LType.from(load.getType()),
                    new LArg[]{
                        address(load.rhs(0)),
                        Immediate.make(0),
                        Reg.AX
                    }));
            prev.prepend(
                new LOp(
                    load.getType()==Type.LONG?LOpCode.ZeroExtQ:LOpCode.ZeroExtL,LType.Byte,
                    new LArg[]{
                        Reg.AX,
                        res
                    }));
        }
        
        return true;
    }
    /** loadBoolify := Boolify(Load(a)) */
    protected boolean visitLoadBoolify(Operation boolify_,
                                       Instruction load_) {
        if (sideCross()) {
            return false;
        }
        
        Instruction boolify=(Instruction)boolify_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        Tmp res=context.tmpForVar(boolify.lhs());
        
        if (load.getType()==Type.BOOLEAN) {

            doLoad(load,res);
        } else {
            prev.prepend(
                new LOp(
                    LOpCode.SetNeq,LType.from(load.getType()),
                    new LArg[]{
                        address(load.rhs(0)),
                        Immediate.make(0),
                        Reg.AX
                    }));
            prev.prepend(
                new LOp(
                    load.getType()==Type.LONG?LOpCode.ZeroExtQ:LOpCode.ZeroExtL,LType.Byte,
                    new LArg[]{
                        Reg.AX,
                        res
                    }));
        }
        
        return true;
    }
    /** loadSqrt := Sqrt(Load(a)) */
    protected boolean visitLoadSqrt(Operation sqrt_,
                                    Instruction load_) {
        if (sideCross()) {
            return false;
        }
        
        MemoryAccessInst load=(MemoryAccessInst)load_;
        Instruction sqrt=(Instruction)sqrt_;
        
        Tmp res=context.tmpForVar(sqrt.lhs());
        
        prev.prepend(
            new LOp(
                LOpCode.FSqrt,LType.from(sqrt.lhs().type()),
                new LArg[]{
                    address(load.rhs(0)),
                    res
                }));
        
        return true;
    }
    /** load := Load(a) */
    protected boolean visitLoad(Operation load) {
        doLoad(load,context.tmpForVar(((Instruction)load).lhs()));
        return true;
    }
    /** store := Store(a, b) */
    protected boolean visitStore(Operation store_) {
        MemoryAccessInst store=(MemoryAccessInst)store_;
        
        switch (store.effectiveBasetype()) {
        case BOOLEAN:
        case BYTE:
            if (store.rhs(1) instanceof Arg.IntConst) {
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,LType.Byte,
                        new LArg[]{
                            Immediate.make(((Arg.IntConst)store.rhs(1)).value32()),
                            address(store.rhs(0))
                        }));
            } else {
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,LType.Long,
                        new LArg[]{
                            context.tmpForVar(store.rhs(1)),
                            Reg.AX
                        }));
                prev.prepend(
                    new LOp(
                        LOpCode.Mov,LType.Byte,
                        new LArg[]{
                            Reg.AX,
                            address(store.rhs(0))
                        }));
            }
            break;
        default:
            set(LType.from(store.getType()),
                address(store.rhs(0)),
                store.rhs(1));
            break;
        }
        
        return true;
    }
    /** strongLoadCAS := StrongLoadCAS(a, b, c) */
    protected boolean visitStrongLoadCAS(Operation strongLoadCAS) {
        MemoryAccessInst cas=(MemoryAccessInst)strongLoadCAS;
        LType type=LType.from(cas.getType());

        set(type, Reg.AX, cas.rhs(1));

        prev.prepend(
            new LOp(
                LOpCode.LoadCAS,type,
                new LArg[]{
                    address(cas.rhs(0)),
                    intTmpValue(type, cas.rhs(2))
                }));
        
        prev.prepend(
            new LOp(
                LOpCode.Mov, type,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cas.lhs())
                }));
        
        return true;
    }
    /** strongCAS := StrongCAS(a, b, c) */
    protected boolean visitStrongCAS(Operation strongCAS) {
        MemoryAccessInst cas=(MemoryAccessInst)strongCAS;
        LType type=LType.from(cas.getType());

        set(type, Reg.AX, cas.rhs(1));

        prev.prepend(
            new LOp(
                LOpCode.TestCAS,type,
                new LArg[]{
                    address(cas.rhs(0)),
                    intTmpValue(type, cas.rhs(2)),
                    Reg.AX
                }));
        
        prev.prepend(
            new LOp(
                LOpCode.ZeroExtL, LType.Byte,
                new LArg[]{
                    Reg.AX,
                    context.tmpForVar(cas.lhs())
                }));
        
        return true;
    }
    /** strongVoidCAS := StrongVoidCAS(a, b, c) */
    protected boolean visitStrongVoidCAS(Operation strongVoidCAS) {
        MemoryAccessInst cas=(MemoryAccessInst)strongVoidCAS;
        LType type=LType.from(cas.getType());

        set(type, Reg.AX, cas.rhs(1));

        prev.prepend(
            new LOp(
                LOpCode.LoadCAS,type,
                new LArg[]{
                    address(cas.rhs(0)),
                    intTmpValue(type, cas.rhs(2))
                }));
        
        return true;
    }
    /** weakCAS := WeakCAS(a, b, c) */
    protected boolean visitWeakCAS(Operation weakCAS) {
        return visitStrongCAS(weakCAS);
    }
    /** boolifyAndLoad1 := Boolify(And(Load(a), b)) */
    protected boolean visitBoolifyAndLoad1(Operation boolify,
                                           Instruction and,
                                           Instruction load) {
        return doBoolAndLoad(boolify,load,and.rhs(1),false);
    }
    /** boolifyAndLoad2 := Boolify(And(a, Load(b))) */
    protected boolean visitBoolifyAndLoad2(Operation boolify,
                                           Instruction and,
                                           Instruction load) {
        return doBoolAndLoad(boolify,load,and.rhs(0),false);
    }
    /** boolifyAnd := Boolify(And(a, b)) */
    protected boolean visitBoolifyAnd(Operation boolify,
                                      Instruction and) {
        return doBoolAnd(boolify,and,false);
    }
    /** notAndLoad1 := Not(And(Load(a), b)) */
    protected boolean visitNotAndLoad1(Operation not,
                                       Instruction and,
                                       Instruction load) {
        return doBoolAndLoad(not,load,and.rhs(1),true);
    }
    /** notAndLoad2 := Not(And(a, Load(b))) */
    protected boolean visitNotAndLoad2(Operation not,
                                       Instruction and,
                                       Instruction load) {
        return doBoolAndLoad(not,load,and.rhs(0),true);
    }
    /** notAnd := Not(And(a, b)) */
    protected boolean visitNotAnd(Operation not,
                                  Instruction and) {
        return doBoolAnd(not,and,true);
    }
    /** eqZeroAndLoad1 := Eq(And(Load(a), b), $0) */
    protected boolean visitEqZeroAndLoad1(Operation eq,
                                          Instruction and,
                                          Instruction load) {
        return doBoolAndLoad(eq,load,and.rhs(1),true);
    }
    /** eqContentsAndLoad := Eq(And(Load(a), $b), $b) */
    protected boolean visitEqContentsAndLoad(Operation eq,
                                             Instruction and,
                                             Instruction load) {
        Arg.IntConst cnst=(Arg.IntConst)and.rhs(1);
        if (IntUtil.countOneBits(cnst.longValue())==1) {
            return doBoolAndLoad(eq,load,and.rhs(1),false);
        }
        return false;
    }
    /** eqZeroAndLoad2 := Eq(And(a, Load(b)), $0) */
    protected boolean visitEqZeroAndLoad2(Operation eq,
                                          Instruction and,
                                          Instruction load) {
        return doBoolAndLoad(eq,load,and.rhs(0),true);
    }
    /** eqZeroAnd := Eq(And(a, b), $0) */
    protected boolean visitEqZeroAnd(Operation eq,
                                     Instruction and) {
        return doBoolAnd(eq,and,true);
    }
    /** eqContentsAnd := Eq(And(a, $b), $b) */
    protected boolean visitEqContentsAnd(Operation eq,
                                         Instruction and) {
        Arg.IntConst cnst=(Arg.IntConst)and.rhs(1);
        if (IntUtil.countOneBits(cnst.longValue())==1) {
            return doBoolAnd(eq,and,false);
        }
        return false;
    }
    /** neqZeroAndLoad1 := Neq(And(Load(a), b), $0) */
    protected boolean visitNeqZeroAndLoad1(Operation neq,
                                           Instruction and,
                                           Instruction load) {
        return doBoolAndLoad(neq,load,and.rhs(1),false);
    }
    /** neqZeroAndLoad2 := Neq(And(a, Load(b)), $0) */
    protected boolean visitNeqZeroAndLoad2(Operation neq,
                                           Instruction and,
                                           Instruction load) {
        return doBoolAndLoad(neq,load,and.rhs(0),false);
    }
    /** neqZeroAnd := Neq(And(a, b), $0) */
    protected boolean visitNeqZeroAnd(Operation neq,
                                      Instruction and) {
        return doBoolAnd(neq,and,false);
    }
    /** mul := Mul(a, b) */
    protected boolean visitMul(Operation mul_) {
        Instruction mul=(Instruction)mul_;
        if (mul.lhs().type().isFloat()) {
            return doOp(mul,LOpCode.FMul);
        } else if ((Global.pointerSize==8 ||
                    mul.lhs().type()!=Exectype.LONG) &&
                   mul.rhs(1) instanceof Arg.IntConst &&
                   ((Arg.IntConst)mul.rhs(1)).is32()) {
            LType type=LType.from(mul.lhs().type());
            
            prev.prepend(
                new LOp(
                    LOpCode.Megamul,type,
                    new LArg[]{
                        Immediate.make(((Arg.IntConst)mul.rhs(1)).value32()),
                        value(type,mul.rhs(0),0),
                        context.tmpForVar(mul.lhs())
                    }));
            
            return true;
        } else {
            return doOp(mul,LOpCode.Mul);
        }
    }
    /** sub := Sub(a, b) */
    protected boolean visitSub(Operation sub) {
        if (sub.rhs(0).type().isFloat()) {
            return doOp(sub,LOpCode.FSub);
        } else {
            return doOp(sub,LOpCode.Sub);
        }
    }
    /** div := Div(a, b) */
    protected boolean visitDiv(Operation div_) {
        Instruction div=(Instruction)div_;
        if (div.rhs(0).type().isFloat()) {
            return doOp(div,LOpCode.FDiv);
        } else {
            return doDiv(div,div.rhs(0),intTmpValue(LType.Long,div.rhs(1)));
        }
    }
    /** mod := Mod(a, b) */
    protected boolean visitMod(Operation mod) {
        return doDiv((Instruction)mod,mod.rhs(0),intTmpValue(LType.Long,mod.rhs(1)));
    }
    /** neg := Neg(a) */
    protected boolean visitNeg(Operation neg) {
        assert !neg.lhs().isFloat();
        return doOp(neg,LOpCode.Neg);
    }
    /** or := Or(a, b) */
    protected boolean visitOr(Operation or) {
        return doOp(or,LOpCode.Or);
    }
    /** xor := Xor(a, b) */
    protected boolean visitXor(Operation xor) {
        return doOp(xor,LOpCode.Xor);
    }
    /** and := And(a, b) */
    protected boolean visitAnd(Operation and) {
        return doOp(and,LOpCode.And);
    }
    /** bitNot := BitNot(a) */
    protected boolean visitBitNot(Operation bitNot) {
        return doOp(bitNot,LOpCode.BitNot);
    }
    /** not := Not(a) */
    protected boolean visitNot(Operation not_) {
        Instruction not=(Instruction)not_;
        
        Tmp res=context.tmpForVar(not.lhs());
        LType type=LType.from(not.lhs().type());
        
        if (context.code.getBoolResult().get(not.rhs(0))) {
            set(type,res,not.rhs(0));
            
            prev.prepend(
                new LOp(
                    LOpCode.Xor,type,
                    new LArg[]{
                        Immediate.make(1),
                        res
                    }));
        } else {
            prev.prepend(
                new LOp(
                    LOpCode.SetEq,type,
                    new LArg[]{
                        value(type,not.rhs(0),0),
                        Immediate.make(0),
                        Reg.AX
                    }));
            prev.prepend(
                new LOp(
                    not.lhs().type()==Exectype.LONG?LOpCode.ZeroExtQ:LOpCode.ZeroExtL,LType.Byte,
                    new LArg[]{
                        Reg.AX,
                        res
                    }));
        }
        return true;
    }
    /** boolify := Boolify(a) */
    protected boolean visitBoolify(Operation boolify_) {
        Instruction boolify=(Instruction)boolify_;

        Tmp res=context.tmpForVar(boolify.lhs());
        LType type=LType.from(boolify.lhs().type());
        
        if (context.code.getBoolResult().get(boolify.rhs(0))) {
            set(type,res,boolify.rhs(0));
        } else {
            prev.prepend(
                new LOp(
                    LOpCode.SetNeq,type,
                    new LArg[]{
                        value(type,boolify.rhs(0),0),
                        Immediate.make(0),
                        Reg.AX
                    }));
            prev.prepend(
                new LOp(
                    boolify.lhs().type()==Exectype.LONG?LOpCode.ZeroExtQ:LOpCode.ZeroExtL,LType.Byte,
                    new LArg[]{
                        Reg.AX,
                        res
                    }));
        }
        return true;
    }
    /** shl := Shl(a, b) */
    protected boolean visitShl(Operation shl) {
        return doShift(shl,LOpCode.Shl);
    }
    /** shr := Shr(a, b) */
    protected boolean visitShr(Operation shr) {
        return doShift(shr,LOpCode.Shr);
    }
    /** ushr := Ushr(a, b) */
    protected boolean visitUshr(Operation ushr) {
        return doShift(ushr,LOpCode.Ushr);
    }
    /** fXor := FXor(a, b) */
    protected boolean visitFXor(Operation fXor_) {
        Instruction fXor=(Instruction)fXor_;
        LType type=LType.from(fXor.lhs().type());
        
        set(type,context.tmpForVar(fXor.lhs()),fXor.rhs(0));
        
        prev.prepend(
            new LOp(
                LOpCode.FXor,type,
                new LArg[]{
                    context.tmpForVar(fXor.rhs(1)),
                    context.tmpForVar(fXor.lhs())
                }));
        
        return true;
    }
    /** float0 := Float0() */
    protected boolean visitFloat0(Operation float0) {
        prev.prepend(
            new LOp(
                LOpCode.FXor,LType.Single,
                new LArg[]{
                    context.tmpForVar(float0.lhs()),
                    context.tmpForVar(float0.lhs())
                }));
        return true;
    }
    /** double0 := Double0() */
    protected boolean visitDouble0(Operation double0) {
        prev.prepend(
            new LOp(
                LOpCode.FXor,LType.Double,
                new LArg[]{
                    context.tmpForVar(double0.lhs()),
                    context.tmpForVar(double0.lhs())
                }));
        return true;
    }
    /** sqrt := Sqrt(a) */
    protected boolean visitSqrt(Operation sqrt) {
        prev.prepend(
            new LOp(
                LOpCode.FSqrt,LType.from(sqrt.lhs().type()),
                new LArg[]{
                    context.tmpForVar(sqrt.rhs(0)),
                    context.tmpForVar(sqrt.lhs())
                }));
        return true;
    }
    /** getArg := GetCArg() */
    protected boolean visitGetArg(Operation getCArg) {
        prev.prepend(
            new LOp(
                LOpCode.Mov,context.argType(getCArg),
                new LArg[]{
                    context.argSlot(getCArg),
                    context.tmpForVar(((ArgInst)getCArg).lhs())
                }));

        return true;
    }
    /** castLoad := Cast(Load(a)) */
    protected boolean visitCastLoad(Operation cast_,
                                    Instruction load_) {
        if (sideCross()) {
            return false;
        }
        
        // we will never see float/double to int/long casts here, as those
        // get turned into function calls.  as well, we will not see casts
        // from shorter-than-int types, like boolean, byte, short, or char.
        // we will also never see boolean in either to or from, but we
        // may see it in the load.
        
        // for certain casts, the Cast(Load) combo cannot be further
        // optimized.  but for some other casts, it makes a lot of sense.
        // for instance: Cast<byte>(Load<int>(ptr)) can be done with a
        // Load<Byte> (ptr), res.  Cast<long>(Load<int>(ptr)) can be done with
        // a SignExt<Long> (ptr), res.  Cast<float>(Load<int>(ptr)) can be
        // done with a ToSingle (ptr), res.
        
        TypeInst cast=(TypeInst)cast_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        if (load.volatility().isVolatile()) {
            return false;
        }
        
        Tmp res=context.tmpForVar(cast.lhs());
        
        if (cast.getType()==load.getType()) {
            // no-op cast
            doLoad(load,res);
            return true;
        }
        
        if (cast.getType().isInteger() && load.getType().isInteger()) {
            switch (cast.getType().effectiveBasetype()) {
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
            case POINTER:
                // NOTE: is there weirdness when either the load type or the
                // cast type are CHAR?  seems like you'll get sign extension/zeroing
                // craziness.
                //
                // Cast<CHAR>(Load<BYTE>): byte gets sign-extended, but then the
                //    lower 16-bits of the sign-extension get zero-extended.
                //
                // Cast<CHAR>(Load<SHORT>): short gets sign-extended, but then
                //    zero-extended --> equivalent to zero-extending load.
                //
                // Cast<CHAR>(Load<INT>): int gets sign-extended, but then
                //    zero-extended on the 16-bits --> equivalent to zero-extending
                //    load.
                
                // the code emitted here only handles the cases where the
                // load width is greater than the cast width.  the cases where that's
                // not true are weirder.  but we'll never cast to INT unless the
                // load is a LONG.  this means that this only comes up when loading
                // a BYTE and casting to SHORT or CHAR.
                
                switch (load.effectiveBasetype()) {
                case BOOLEAN:
                    // no-op cast; easy to handle because the source is either zero
                    // or one.
                    doLoad(load,res);
                    return true;
                case BYTE:
                    switch (cast.effectiveBasetype()) {
                    case SHORT:
                        // no-op cast
                        doLoad(load,res);
                        return true;
                    case CHAR:
                        // can't handle this - need to load and cast separately,
                        // because we're first sign-extending to 32-bits and then
                        // zero-extending the lower 16 bits.
                        return false;
                    default:
                        // remaining cases are casting to pointer.
                        break;
                    }
                    break;
                case POINTER:
                    // if we're casting *from* a pointer then we can just load
                    // the smaller type.
                    break;
                default:
                    // other cases are loading a short or int.  if
                    // loading a short, we will never cast to int because that
                    // would have been peepholed out - so we'll only cast shorts
                    // to bytes or chars.  if loading an int, we'll only cast
                    // to things smaller.
                    break;
                }
                
                switch (cast.effectiveBasetype()) {
                case POINTER:
                    switch (load.effectiveBasetype()) {
                    case CHAR:
                        // zero-extend to 32-bit and then sign-extend to pointer;
                        // we can only handle that on 32-bit platforms.
                        if (Global.pointerSize==8) {
                            return false;
                        }
                        break;
                    default:
                        // always just sign-extend.  the remaining cases are
                        // loading a byte, short, or int.  so just sign-extend.
                        break;
                    }
                    
                    // can handle sign-ext using doLoad
                    doLoad(load,res);
                    return true;
                default:
                    break;
                }
                
                assert load.getType().effectiveBasetype().bytes>=cast.getType().effectiveBasetype().bytes;
                doLoad(load,cast.getType().effectiveBasetype(),res);
                return true;
            case LONG:
                switch (load.effectiveBasetype()) {
                case CHAR:
                case POINTER:
                    prev.prepend(
                        new LOp(
                            LOpCode.ZeroExtQ,LType.from(load.getType()),
                            new LArg[]{
                                address(load.rhs(0)),
                                res
                            }));
                    break;
                default:
                    prev.prepend(
                        new LOp(
                            LOpCode.SignExtQ,LType.from(load.getType()),
                            new LArg[]{
                                address(load.rhs(0)),
                                res
                            }));
                    break;
                }
                break;
            default:
                throw new CompilerException("bad type for cast/load: "+cast+", "+load);
            }
        }
        
        // remaining cases are float->double, double->float, int->float, long->float,
        // int->double, long->double
        
        switch (cast.effectiveBasetype()) {
        case FLOAT:
            switch (load.effectiveBasetype()) {
            case DOUBLE:
            case INT:
            case LONG:
                prev.prepend(
                    new LOp(
                        LOpCode.ToSingle,LType.from(load.effectiveBasetype()),
                        new LArg[]{
                            address(load.rhs(0)),
                            res
                        }));
                return true;
            default:
                break;
            }
            break;
        case DOUBLE:
            switch (load.effectiveBasetype()) {
            case FLOAT:
            case INT:
            case LONG:
                prev.prepend(
                    new LOp(
                        LOpCode.ToDouble,LType.from(load.effectiveBasetype()),
                        new LArg[]{
                            address(load.rhs(0)),
                            res
                        }));
                return true;
            default:
                break;
            }
            break;
        default:
            break;
        }
        
        return false;
    }
    /** cast := Cast(a) */
    protected boolean visitCast(Operation cast_) {
        TypeInst cast=(TypeInst)cast_;
        Tmp res=context.tmpForVar(cast.lhs());
        LArg val=value(cast.rhs(0).type(),cast.rhs(0),0);
        switch (cast.effectiveBasetype()) {
        case BYTE:
            switch (cast.rhs(0).type().effectiveBasetype()) {
            case INT:
                if (val instanceof Tmp) {
                    prev.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Long,
                            new LArg[]{
                                val,
                                Reg.AX
                            }));
                    val=Reg.AX;
                }
                prev.prepend(
                    new LOp(
                        LOpCode.SignExtL,LType.Byte,
                        new LArg[]{
                            val,
                            res
                        }));
                return true;
            default:
                break;
            }
            break;
        case CHAR:
            switch (cast.rhs(0).type().effectiveBasetype()) {
            case INT:
                prev.prepend(
                    new LOp(
                        LOpCode.ZeroExtL,LType.Half,
                        new LArg[]{
                            val,
                            res
                        }));
                return true;
            default:
                break;
            }
            break;
        case SHORT:
            switch (cast.rhs(0).type().effectiveBasetype()) {
            case INT:
                prev.prepend(
                    new LOp(
                        LOpCode.SignExtL,LType.Half,
                        new LArg[]{
                            val,
                            res
                        }));
                return true;
            default:
                break;
            }
            break;
        case INT:
            switch (cast.rhs(0).type().effectiveBasetype()) {
            case POINTER:
                if (Global.pointerSize==4) {
                    prev.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Long,
                            new LArg[]{
                                val,
                                res
                            }));
                } else {
                    prev.prepend(
                        new LOp(
                            LOpCode.First32,LType.Quad,
                            new LArg[]{
                                val,
                                res
                            }));
                }
                return true;
            case LONG:
                prev.prepend(
                    new LOp(
                        LOpCode.First32,LType.Quad,
                        new LArg[]{
                            val,
                            res
                        }));
                return true;
            default:
                break;
            }
            break;
        case LONG:
            switch (cast.rhs(0).type().effectiveBasetype()) {
            case INT:
                prev.prepend(
                    new LOp(
                        LOpCode.SignExtQ,LType.Long,
                        new LArg[]{
                            val,
                            res
                        }));
                return true;
            case POINTER:
                if (Global.pointerSize==4) {
                    prev.prepend(
                        new LOp(
                            LOpCode.ZeroExtQ,LType.Long,
                            new LArg[]{
                                val,
                                res
                            }));
                } else {
                    prev.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Quad,
                            new LArg[]{
                                val,
                                res
                            }));
                }
                return true;
            default:
                break;
            }
            break;
        case POINTER:
            if (Global.pointerSize==4) {
                switch (cast.rhs(0).type().effectiveBasetype()) {
                case INT:
                    prev.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Long,
                            new LArg[]{
                                val,
                                res
                            }));
                    return true;
                case LONG:
                    prev.prepend(
                        new LOp(
                            LOpCode.First32,LType.Quad,
                            new LArg[]{
                                val,
                                res
                            }));
                    return true;
                default:
                    break;
                }
            } else {
                switch (cast.rhs(0).type().effectiveBasetype()) {
                case INT:
                    prev.prepend(
                        new LOp(
                            LOpCode.SignExtQ,LType.Long,
                            new LArg[]{
                                val,
                                res
                            }));
                    return true;
                case LONG:
                    prev.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Quad,
                            new LArg[]{
                                val,
                                res
                            }));
                    return true;
                default:
                    break;
                }
            }
            break;
        case FLOAT:
            switch (cast.rhs(0).type().effectiveBasetype()) {
            case INT:
            case LONG:
            case DOUBLE:
                prev.prepend(
                    new LOp(
                        LOpCode.ToSingle,LType.from(cast.rhs(0).type()),
                        new LArg[]{
                            val,
                            res
                        }));
                return true;
            default:
                break;
            }
            break;
        case DOUBLE:
            switch (cast.rhs(0).type().effectiveBasetype()) {
            case INT:
            case LONG:
            case FLOAT:
                prev.prepend(
                    new LOp(
                        LOpCode.ToDouble,LType.from(cast.rhs(0).type()),
                        new LArg[]{
                            val,
                            res
                        }));
                return true;
            default:
                break;
            }
            break;
        default:
            break;
        }
        return false;
    }
    /** fiat := Fiat(a) */
    protected boolean visitFiat(Operation fiat_) {
        TypeInst fiat=(TypeInst)fiat_;
        switch (fiat.rhs(0).effectiveBasetype()) {
        case INT:
            assert fiat.type()==Type.FLOAT;
            prev.prepend(
                new LOp(
                    LOpCode.FiatToFloat,LType.Long,
                    new LArg[]{
                        intTmpValue(LType.Long,fiat.rhs(0)),
                        context.tmpForVar(fiat.lhs())
                    }));
            return true;
        case FLOAT:
            assert fiat.type()==Type.INT;
            prev.prepend(
                new LOp(
                    LOpCode.FiatToInt,LType.Long,
                    new LArg[]{
                        context.tmpForVar(fiat.rhs(0)),
                        context.tmpForVar(fiat.lhs())
                    }));
            return true;
        case LONG:
            switch (fiat.effectiveBasetype()) {
            case BOOLEAN:
                prev.prepend(
                    new LOp(
                        LOpCode.First32,LType.Quad,
                        new LArg[]{
                            value(fiat.rhs(0),0),
                            Reg.AX
                        }));
                prev.prepend(
                    new LOp(
                        LOpCode.ZeroExtL,LType.Byte,
                        new LArg[]{
                            Reg.AX,
                            context.tmpForVar(fiat.lhs())
                        }));
                return true;
            case BYTE:
                prev.prepend(
                    new LOp(
                        LOpCode.First32,LType.Quad,
                        new LArg[]{
                            value(fiat.rhs(0),0),
                            Reg.AX
                        }));
                prev.prepend(
                    new LOp(
                        LOpCode.SignExtL,LType.Byte,
                        new LArg[]{
                            Reg.AX,
                            context.tmpForVar(fiat.lhs())
                        }));
                return true;
            case CHAR:
                prev.prepend(
                    new LOp(
                        LOpCode.First32,LType.Quad,
                        new LArg[]{
                            value(fiat.rhs(0),0),
                            context.tmpForVar(fiat.lhs())
                        }));
                prev.prepend(
                    new LOp(
                        LOpCode.ZeroExtL,LType.Half,
                        new LArg[]{
                            context.tmpForVar(fiat.lhs()),
                            context.tmpForVar(fiat.lhs())
                        }));
                return true;
            case SHORT:
                prev.prepend(
                    new LOp(
                        LOpCode.First32,LType.Quad,
                        new LArg[]{
                            value(fiat.rhs(0),0),
                            context.tmpForVar(fiat.lhs())
                        }));
                prev.prepend(
                    new LOp(
                        LOpCode.SignExtL,LType.Half,
                        new LArg[]{
                            context.tmpForVar(fiat.lhs()),
                            context.tmpForVar(fiat.lhs())
                        }));
                return true;
            case INT:
                prev.prepend(
                    new LOp(
                        LOpCode.First32,LType.Quad,
                        new LArg[]{
                            value(fiat.rhs(0),0),
                            context.tmpForVar(fiat.lhs())
                        }));
                return true;
            case POINTER:
                if (Global.pointerSize==4) {
                    prev.prepend(
                        new LOp(
                            LOpCode.First32,LType.Quad,
                            new LArg[]{
                                value(fiat.rhs(0),0),
                                context.tmpForVar(fiat.lhs())
                            }));
                    return true;
                } else {
                    prev.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Quad,
                            new LArg[]{
                                value(fiat.rhs(0),0),
                                context.tmpForVar(fiat.lhs())
                            }));
                    return true;
                }
            case FLOAT: {
                Tmp tmp=context.lc.addTmp(Kind.INT);
                prev.prepend(
                    new LOp(
                        LOpCode.First32,LType.Quad,
                        new LArg[]{
                            value(fiat.rhs(0),0),
                            tmp
                        }));
                prev.prepend(
                    new LOp(
                        LOpCode.FiatToFloat,LType.Long,
                        new LArg[]{
                            tmp,
                            context.tmpForVar(fiat.lhs())
                        }));
                return true;
            }
            case DOUBLE:
                prev.prepend(
                    new LOp(
                        LOpCode.FiatToFloat,LType.Quad,
                        new LArg[]{
                            intTmpValue(LType.Quad,fiat.rhs(0)),
                            context.tmpForVar(fiat.lhs())
                        }));
                return true;
            }
            break;
        case DOUBLE:
            assert fiat.type()==Type.LONG;
            prev.prepend(
                new LOp(
                    LOpCode.FiatToInt,LType.Quad,
                    new LArg[]{
                        context.tmpForVar(fiat.rhs(0)),
                        context.tmpForVar(fiat.lhs())
                    }));
            return true;
        default:
            break;
        }
        return false;
    }
    /** zeroExtendLoad := IntToPointerZeroFill(Load(a)) */
    protected boolean visitZeroExtendLoad(Operation intToPointerZeroFill,
                                          Instruction load) {
        if (sideCross()) {
            return false;
        }
        
        Tmp res=context.tmpForVar(((Instruction)intToPointerZeroFill).lhs());
        if (Global.pointerSize==4) {
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Long,
                    new LArg[]{
                        address(load.rhs(0)),
                        res
                    }));
        } else {
            prev.prepend(
                new LOp(
                    LOpCode.ZeroExtQ,LType.Long,
                    new LArg[]{
                        address(load.rhs(0)),
                        res
                    }));
        }
        return true;
    }
    /** zeroExtend := IntToPointerZeroFill(a) */
    protected boolean visitZeroExtend(Operation intToPointerZeroFill) {
        Tmp res=context.tmpForVar(((Instruction)intToPointerZeroFill).lhs());
        if (Global.pointerSize==4) {
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Long,
                    new LArg[]{
                        value(LType.Long,intToPointerZeroFill.rhs(0),0),
                        res
                    }));
        } else {
            prev.prepend(
                new LOp(
                    LOpCode.ZeroExtQ,LType.Long,
                    new LArg[]{
                        value(LType.Long,intToPointerZeroFill.rhs(0),0),
                        res
                    }));
        }
        return true;
    }
    /** firstHalfLoad := FirstHalf(Load(a)) */
    protected boolean visitFirstHalfLoad(Operation firstHalf,
                                         Instruction load) {
        if (sideCross()) {
            return false;
        }
        
        Tmp res=context.tmpForVar(((Instruction)firstHalf).lhs());
        prev.prepend(
            new LOp(
                LOpCode.Mov,LType.Long,
                new LArg[]{
                    address(load.rhs(0)),
                    res
                }));
        return true;
    }
    /** firstHalf := FirstHalf(a) */
    protected boolean visitFirstHalf(Operation firstHalf_) {
        Instruction firstHalf=(Instruction)firstHalf_;
        Tmp res=context.tmpForVar(firstHalf.lhs());
        switch (firstHalf.rhs(0).type().effectiveBasetype()) {
        case LONG:
            prev.prepend(
                new LOp(
                    LOpCode.First32,LType.Quad,
                    new LArg[]{
                        value(LType.Quad,firstHalf.rhs(0),0),
                        res
                    }));
            return true;
        case DOUBLE:
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Double,
                    new LArg[]{
                        value(LType.Double,firstHalf.rhs(0),0),
                        new OffScratchSlot(context.memBounce(),0)
                    }));
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Long,
                    new LArg[]{
                        new OffScratchSlot(context.memBounce(),0),
                        res
                    }));
            return true;
        default:
            break;
        }
        return false;
    }
    /** secondHalfLoad := SecondHalf(Load(a)) */
    protected boolean visitSecondHalfLoad(Operation secondHalf,
                                          Instruction load) {
        if (sideCross()) {
            return false;
        }
        
        Tmp res=context.tmpForVar(((Instruction)secondHalf).lhs());
        prev.prepend(
            new LOp(
                LOpCode.Mov,LType.Long,
                new LArg[]{
                    address(load.rhs(0)).offset(4),
                    res
                }));
        return true;
    }
    /** secondHalf := SecondHalf(a) */
    protected boolean visitSecondHalf(Operation secondHalf_) {
        Instruction secondHalf=(Instruction)secondHalf_;
        Tmp res=context.tmpForVar(secondHalf.lhs());
        switch (secondHalf.rhs(0).type().effectiveBasetype()) {
        case LONG:
            prev.prepend(
                new LOp(
                    LOpCode.Second32,LType.Quad,
                    new LArg[]{
                        value(LType.Quad,secondHalf.rhs(0),0),
                        res
                    }));
            return true;
        case DOUBLE:
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Double,
                    new LArg[]{
                        value(LType.Double,secondHalf.rhs(0),0),
                        new OffScratchSlot(context.memBounce(),0)
                    }));
            prev.prepend(
                new LOp(
                    LOpCode.Mov,LType.Long,
                    new LArg[]{
                        new OffScratchSlot(context.memBounce(),4),
                        res
                    }));
            return true;
        default:
            break;
        }
        return false;
    }
    /** branchAndZeroLoad1 := BranchZero(And(Load(a), b)) */
    protected boolean visitBranchAndZeroLoad1(Operation branchZero,
                                              Instruction and,
                                              Instruction load) {
        return doBranchCmpAddrVal(branchZero,load,and.rhs(1),LOpCode.BranchAndZero);
    }
    /** branchAndZeroLoad2 := BranchZero(And(a, Load(b))) */
    protected boolean visitBranchAndZeroLoad2(Operation branchZero,
                                              Instruction and,
                                              Instruction load) {
        return doBranchCmpAddrVal(branchZero,load,and.rhs(0),LOpCode.BranchAndZero);
    }
    /** branchAndNotZeroLoad1 := BranchNonZero(And(Load(a), b)) */
    protected boolean visitBranchAndNotZeroLoad1(Operation branchNonZero,
                                                 Instruction and,
                                                 Instruction load) {
        return doBranchCmpAddrVal(branchNonZero,load,and.rhs(1),LOpCode.BranchAndNotZero);
    }
    /** branchAndNotZeroLoad2 := BranchNonZero(And(a, Load(b))) */
    protected boolean visitBranchAndNotZeroLoad2(Operation branchNonZero,
                                                 Instruction and,
                                                 Instruction load) {
        return doBranchCmpAddrVal(branchNonZero,load,and.rhs(0),LOpCode.BranchAndNotZero);
    }
    /** branchAndZero := BranchZero(And(a, b)) */
    protected boolean visitBranchAndZero(Operation branchZero,
                                         Instruction and) {
        return doBranchCmp(branchZero,and.rhs(0),and.rhs(1),LOpCode.BranchAndZero);
    }
    /** branchAndNotZero := BranchNonZero(And(a, b)) */
    protected boolean visitBranchAndNotZero(Operation branchNonZero,
                                            Instruction and) {
        return doBranchCmp(branchNonZero,and.rhs(0),and.rhs(1),LOpCode.BranchAndNotZero);
    }
    /** branchEq1ContentsAndLoad := BranchNonZero(Eq(And(Load(a), $b), $b)) */
    protected boolean visitBranchEq1ContentsAndLoad(Operation branchNonZero,
                                                    Instruction eq,
                                                    Instruction and,
                                                    Instruction load) {
        Arg.IntConst cnst=(Arg.IntConst)and.rhs(1);
        if (IntUtil.countOneBits(cnst.longValue())==1) {
            return doBranchCmpAddrVal(branchNonZero,load,and.rhs(1),LOpCode.BranchAndNotZero);
        }
        return false;
    }
    /** branchEq2ContentsAndLoad := BranchZero(Neq(And(Load(a), $b), $b)) */
    protected boolean visitBranchEq2ContentsAndLoad(Operation branchZero,
                                                    Instruction neq,
                                                    Instruction and,
                                                    Instruction load) {
        Arg.IntConst cnst=(Arg.IntConst)and.rhs(1);
        if (IntUtil.countOneBits(cnst.longValue())==1) {
            return doBranchCmpAddrVal(branchZero,load,and.rhs(1),LOpCode.BranchAndNotZero);
        }
        return false;
    }
    /** branchEq1ContentsAnd := BranchNonZero(Eq(And(a, $b), $b)) */
    protected boolean visitBranchEq1ContentsAnd(Operation branchNonZero,
                                                Instruction eq,
                                                Instruction and) {
        Arg.IntConst cnst=(Arg.IntConst)and.rhs(1);
        if (IntUtil.countOneBits(cnst.longValue())==1) {
            return doBranchCmp(branchNonZero,and.rhs(0),and.rhs(1),LOpCode.BranchAndNotZero);
        }
        return false;
    }
    /** branchEq2ContentsAnd := BranchZero(Neq(And(a, $b), $b)) */
    protected boolean visitBranchEq2ContentsAnd(Operation branchZero,
                                                Instruction neq,
                                                Instruction and) {
        Arg.IntConst cnst=(Arg.IntConst)and.rhs(1);
        if (IntUtil.countOneBits(cnst.longValue())==1) {
            return doBranchCmp(branchZero,and.rhs(0),and.rhs(1),LOpCode.BranchAndNotZero);
        }
        return false;
    }
    /** branchEq1Load1 := BranchNonZero(Eq(Load(a), b)) */
    protected boolean visitBranchEq1Load1(Operation branchNonZero,
                                          Instruction eq,
                                          Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchFEqLoad(branchNonZero,load,eq.rhs(1));
        } else {
            return doBranchCmpAddrVal(branchNonZero,load,eq.rhs(1),LOpCode.BranchEq);
        }
    }
    /** branchEq2Load1 := BranchZero(Neq(Load(a), b)) */
    protected boolean visitBranchEq2Load1(Operation branchZero,
                                          Instruction neq,
                                          Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchFEqLoad(branchZero,load,neq.rhs(1));
        } else {
            return doBranchCmpAddrVal(branchZero,load,neq.rhs(1),LOpCode.BranchEq);
        }
    }
    /** branchNeq1Load1 := BranchNonZero(Neq(Load(a), b)) */
    protected boolean visitBranchNeq1Load1(Operation branchNonZero,
                                           Instruction neq,
                                           Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchFNeqLoad(branchNonZero,load,neq.rhs(1));
        } else {
            return doBranchCmpAddrVal(branchNonZero,load,neq.rhs(1),LOpCode.BranchNeq);
        }
    }
    /** branchNeq2Load1 := BranchZero(Eq(Load(a), b)) */
    protected boolean visitBranchNeq2Load1(Operation branchZero,
                                           Instruction eq,
                                           Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchFNeqLoad(branchZero,load,eq.rhs(1));
        } else {
            return doBranchCmpAddrVal(branchZero,load,eq.rhs(1),LOpCode.BranchNeq);
        }
    }
    /** branchLTLoad1 := BranchNonZero(LessThan(Load(a), b)) */
    protected boolean visitBranchLTLoad1(Operation branchNonZero,
                                         Instruction lessThan,
                                         Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchCmpValAddr(branchNonZero,load,lessThan.rhs(1),LOpCode.BranchFGreaterThan);
        } else {
            return doBranchCmpAddrVal(branchNonZero,load,lessThan.rhs(1),LOpCode.BranchLessThan);
        }
    }
    /** branchGELoad1 := BranchZero(LessThan(Load(a), b)) */
    protected boolean visitBranchGELoad1(Operation branchZero,
                                         Instruction lessThan,
                                         Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doBranchCmpValAddr(branchZero,load,lessThan.rhs(1),LOpCode.BranchNotFGT);
        } else {
            return doBranchCmpValAddr(branchZero,load,lessThan.rhs(1),LOpCode.BranchLTEq);
        }
    }
    /** branchLELoad1 := BranchNonZero(LessThanEq(Load(a), b)) */
    protected boolean visitBranchLELoad1(Operation branchNonZero,
                                         Instruction lessThanEq,
                                         Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doBranchCmpValAddr(branchNonZero,load,lessThanEq.rhs(1),LOpCode.BranchFGTEq);
        } else {
            return doBranchCmpAddrVal(branchNonZero,load,lessThanEq.rhs(1),LOpCode.BranchLTEq);
        }
    }
    /** branchGTLoad1 := BranchZero(LessThanEq(Load(a), b)) */
    protected boolean visitBranchGTLoad1(Operation branchZero,
                                         Instruction lessThanEq,
                                         Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doBranchCmpValAddr(branchZero,load,lessThanEq.rhs(1),LOpCode.BranchNotFGTEq);
        } else {
            return doBranchCmpValAddr(branchZero,load,lessThanEq.rhs(1),LOpCode.BranchLessThan);
        }
    }
    /** branchULTLoad1 := BranchNonZero(ULessThan(Load(a), b)) */
    protected boolean visitBranchULTLoad1(Operation branchNonZero,
                                          Instruction uLessThan,
                                          Instruction load) {
        return doBranchCmpAddrVal(branchNonZero,load,uLessThan.rhs(1),LOpCode.BranchULessThan);
    }
    /** branchUGELoad1 := BranchZero(ULessThan(Load(a), b)) */
    protected boolean visitBranchUGELoad1(Operation branchZero,
                                          Instruction uLessThan,
                                          Instruction load) {
        return doBranchCmpValAddr(branchZero,load,uLessThan.rhs(1),LOpCode.BranchULTEq);
    }
    /** branchULELoad1 := BranchNonZero(ULessThanEq(Load(a), b)) */
    protected boolean visitBranchULELoad1(Operation branchNonZero,
                                          Instruction uLessThanEq,
                                          Instruction load) {
        return doBranchCmpAddrVal(branchNonZero,load,uLessThanEq.rhs(1),LOpCode.BranchULTEq);
    }
    /** branchUGTLoad1 := BranchZero(ULessThanEq(Load(a), b)) */
    protected boolean visitBranchUGTLoad1(Operation branchZero,
                                          Instruction uLessThanEq,
                                          Instruction load) {
        return doBranchCmpValAddr(branchZero,load,uLessThanEq.rhs(1),LOpCode.BranchULessThan);
    }
    /** branchEq1Load2 := BranchNonZero(Eq(a, Load(b))) */
    protected boolean visitBranchEq1Load2(Operation branchNonZero,
                                          Instruction eq,
                                          Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchFEqLoad(branchNonZero,load,eq.rhs(0));
        } else {
            return doBranchCmpAddrVal(branchNonZero,load,eq.rhs(0),LOpCode.BranchEq);
        }
    }
    /** branchEq2Load2 := BranchZero(Neq(a, Load(b))) */
    protected boolean visitBranchEq2Load2(Operation branchZero,
                                          Instruction neq,
                                          Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchFEqLoad(branchZero,load,neq.rhs(0));
        } else {
            return doBranchCmpAddrVal(branchZero,load,neq.rhs(0),LOpCode.BranchEq);
        }
    }
    /** branchNeq1Load2 := BranchNonZero(Neq(a, Load(b))) */
    protected boolean visitBranchNeq1Load2(Operation branchNonZero,
                                           Instruction neq,
                                           Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchFNeqLoad(branchNonZero,load,neq.rhs(0));
        } else {
            return doBranchCmpAddrVal(branchNonZero,load,neq.rhs(0),LOpCode.BranchNeq);
        }
    }
    /** branchNeq2Load2 := BranchZero(Eq(a, Load(b))) */
    protected boolean visitBranchNeq2Load2(Operation branchZero,
                                           Instruction eq,
                                           Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchFNeqLoad(branchZero,load,eq.rhs(0));
        } else {
            return doBranchCmpAddrVal(branchZero,load,eq.rhs(0),LOpCode.BranchNeq);
        }
    }
    /** branchLTLoad2 := BranchNonZero(LessThan(a, Load(b))) */
    protected boolean visitBranchLTLoad2(Operation branchNonZero,
                                         Instruction lessThan,
                                         Instruction load) {
        if (load.lhs().isFloat()) {
            return doBranchCmpAddrVal(branchNonZero,load,lessThan.rhs(0),LOpCode.BranchFGreaterThan);
        } else {
            return doBranchCmpValAddr(branchNonZero,load,lessThan.rhs(0),LOpCode.BranchLessThan);
        }
    }
    /** branchGELoad2 := BranchZero(LessThan(a, Load(b))) */
    protected boolean visitBranchGELoad2(Operation branchZero,
                                         Instruction lessThan,
                                         Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doBranchCmpAddrVal(branchZero,load,lessThan.rhs(0),LOpCode.BranchNotFGT);
        } else {
            return doBranchCmpAddrVal(branchZero,load,lessThan.rhs(0),LOpCode.BranchLTEq);
        }
    }
    /** branchLELoad2 := BranchNonZero(LessThanEq(a, Load(b))) */
    protected boolean visitBranchLELoad2(Operation branchNonZero,
                                         Instruction lessThanEq,
                                         Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doBranchCmpAddrVal(branchNonZero,load,lessThanEq.rhs(0),LOpCode.BranchFGTEq);
        } else {
            return doBranchCmpValAddr(branchNonZero,load,lessThanEq.rhs(0),LOpCode.BranchLTEq);
        }
    }
    /** branchGTLoad2 := BranchZero(LessThanEq(a, Load(b))) */
    protected boolean visitBranchGTLoad2(Operation branchZero,
                                         Instruction lessThanEq,
                                         Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doBranchCmpAddrVal(branchZero,load,lessThanEq.rhs(0),LOpCode.BranchNotFGTEq);
        } else {
            return doBranchCmpAddrVal(branchZero,load,lessThanEq.rhs(0),LOpCode.BranchLessThan);
        }
    }
    /** branchULTLoad2 := BranchNonZero(ULessThan(a, Load(b))) */
    protected boolean visitBranchULTLoad2(Operation branchNonZero,
                                          Instruction uLessThan,
                                          Instruction load) {
        return doBranchCmpValAddr(branchNonZero,load,uLessThan.rhs(0),LOpCode.BranchULessThan);
    }
    /** branchUGELoad2 := BranchZero(ULessThan(a, Load(b))) */
    protected boolean visitBranchUGELoad2(Operation branchZero,
                                          Instruction uLessThan,
                                          Instruction load) {
        return doBranchCmpAddrVal(branchZero,load,uLessThan.rhs(0),LOpCode.BranchULTEq);
    }
    /** branchULELoad2 := BranchNonZero(ULessThanEq(a, Load(b))) */
    protected boolean visitBranchULELoad2(Operation branchNonZero,
                                          Instruction uLessThanEq,
                                          Instruction load) {
        return doBranchCmpValAddr(branchNonZero,load,uLessThanEq.rhs(0),LOpCode.BranchULTEq);
    }
    /** branchUGTLoad2 := BranchZero(ULessThanEq(a, Load(b))) */
    protected boolean visitBranchUGTLoad2(Operation branchZero,
                                          Instruction uLessThanEq,
                                          Instruction load) {
        return doBranchCmpAddrVal(branchZero,load,uLessThanEq.rhs(0),LOpCode.BranchULessThan);
    }
    /** branchEq1 := BranchNonZero(Eq(a, b)) */
    protected boolean visitBranchEq1(Operation branchNonZero,
                                     Instruction eq) {
        if (eq.rhs(0).isFloat()) {
            return doBranchFEq(branchNonZero,eq.rhs(0),eq.rhs(1));
        } else {
            return doBranchCmp(branchNonZero,eq.rhs(0),eq.rhs(1),LOpCode.BranchEq);
        }
    }
    /** branchEq2 := BranchZero(Neq(a, b)) */
    protected boolean visitBranchEq2(Operation branchZero,
                                     Instruction neq) {
        if (neq.rhs(0).isFloat()) {
            return doBranchFEq(branchZero,neq.rhs(0),neq.rhs(1));
        } else {
            return doBranchCmp(branchZero,neq.rhs(0),neq.rhs(1),LOpCode.BranchEq);
        }
    }
    /** branchNeq1 := BranchNonZero(Neq(a, b)) */
    protected boolean visitBranchNeq1(Operation branchNonZero,
                                      Instruction neq) {
        if (neq.rhs(0).isFloat()) {
            return doBranchFNeq(branchNonZero,neq.rhs(0),neq.rhs(1));
        } else {
            return doBranchCmp(branchNonZero,neq.rhs(0),neq.rhs(1),LOpCode.BranchNeq);
        }
    }
    /** branchNeq2 := BranchZero(Eq(a, b)) */
    protected boolean visitBranchNeq2(Operation branchZero,
                                      Instruction eq) {
        if (eq.rhs(0).isFloat()) {
            return doBranchFNeq(branchZero,eq.rhs(0),eq.rhs(1));
        } else {
            return doBranchCmp(branchZero,eq.rhs(0),eq.rhs(1),LOpCode.BranchNeq);
        }
    }
    /** branchLT := BranchNonZero(LessThan(a, b)) */
    protected boolean visitBranchLT(Operation branchNonZero,
                                    Instruction lessThan) {
        if (lessThan.rhs(0).isFloat()) {
            return doBranchCmp(branchNonZero,lessThan.rhs(1),lessThan.rhs(0),LOpCode.BranchFGreaterThan);
        } else {
            return doBranchCmp(branchNonZero,lessThan.rhs(0),lessThan.rhs(1),LOpCode.BranchLessThan);
        }
    }
    /** branchGE := BranchZero(LessThan(a, b)) */
    protected boolean visitBranchGE(Operation branchZero,
                                    Instruction lessThan) {
        if (lessThan.rhs(0).type().isFloat()) {
            return doBranchCmp(branchZero,lessThan.rhs(1),lessThan.rhs(0),LOpCode.BranchNotFGT);
        } else {
            return doBranchCmp(branchZero,lessThan.rhs(1),lessThan.rhs(0),LOpCode.BranchLTEq);
        }
    }
    /** branchLE := BranchNonZero(LessThanEq(a, b)) */
    protected boolean visitBranchLE(Operation branchNonZero,
                                    Instruction lessThanEq) {
        if (lessThanEq.rhs(0).isFloat()) {
            return doBranchCmp(branchNonZero,lessThanEq.rhs(1),lessThanEq.rhs(0),LOpCode.BranchFGTEq);
        } else {
            return doBranchCmp(branchNonZero,lessThanEq.rhs(0),lessThanEq.rhs(1),LOpCode.BranchLTEq);
        }
    }
    /** branchGT := BranchZero(LessThanEq(a, b)) */
    protected boolean visitBranchGT(Operation branchZero,
                                    Instruction lessThanEq) {
        if (lessThanEq.rhs(0).type().isFloat()) {
            return doBranchCmp(branchZero,lessThanEq.rhs(1),lessThanEq.rhs(0),LOpCode.BranchNotFGTEq);
        } else {
            return doBranchCmp(branchZero,lessThanEq.rhs(1),lessThanEq.rhs(0),LOpCode.BranchLessThan);
        }
    }
    /** branchULT := BranchNonZero(ULessThan(a, b)) */
    protected boolean visitBranchULT(Operation branchNonZero,
                                     Instruction uLessThan) {
        return doBranchCmp(branchNonZero,uLessThan.rhs(0),uLessThan.rhs(1),LOpCode.BranchULessThan);
    }
    /** branchUGE := BranchZero(ULessThan(a, b)) */
    protected boolean visitBranchUGE(Operation branchZero,
                                     Instruction uLessThan) {
        return doBranchCmp(branchZero,uLessThan.rhs(1),uLessThan.rhs(0),LOpCode.BranchULTEq);
    }
    /** branchULE := BranchNonZero(ULessThanEq(a, b)) */
    protected boolean visitBranchULE(Operation branchNonZero,
                                     Instruction uLessThanEq) {
        return doBranchCmp(branchNonZero,uLessThanEq.rhs(0),uLessThanEq.rhs(1),LOpCode.BranchULTEq);
    }
    /** branchUGT := BranchZero(ULessThanEq(a, b)) */
    protected boolean visitBranchUGT(Operation branchZero,
                                     Instruction uLessThanEq) {
        return doBranchCmp(branchZero,uLessThanEq.rhs(1),uLessThanEq.rhs(0),LOpCode.BranchULessThan);
    }
    /** branchCASSucc1 := BranchNonZero(StrongCAS(a, b, c)) */
    protected boolean visitBranchCASSucc1(Operation branchNonZero,
                                          Instruction strongCAS) {
        return doBranchCAS(branchNonZero,strongCAS,true);
    }
    /** branchCASFail1 := BranchZero(StrongCAS(a, b, c)) */
    protected boolean visitBranchCASFail1(Operation branchZero,
                                          Instruction strongCAS) {
        return doBranchCAS(branchZero,strongCAS,false);
    }
    /** branchCASSucc2 := BranchNonZero(WeakCAS(a, b, c)) */
    protected boolean visitBranchCASSucc2(Operation branchNonZero,
                                          Instruction weakCAS) {
        return doBranchCAS(branchNonZero,weakCAS,true);
    }
    /** branchCASFail2 := BranchZero(WeakCAS(a, b, c)) */
    protected boolean visitBranchCASFail2(Operation branchZero,
                                          Instruction weakCAS) {
        return doBranchCAS(branchZero,weakCAS,false);
    }
    /** branchCASSucc3 := BranchNonZero(Eq(StrongLoadCAS(a, b, c), b)) */
    protected boolean visitBranchCASSucc3(Operation branchNonZero,
                                          Instruction eq,
                                          Instruction strongLoadCAS) {
        return doBranchCAS(branchNonZero,strongLoadCAS,true);
    }
    /** branchCASFail3 := BranchZero(Eq(StrongLoadCAS(a, b, c), b)) */
    protected boolean visitBranchCASFail3(Operation branchZero,
                                          Instruction eq,
                                          Instruction strongLoadCAS) {
        return doBranchCAS(branchZero,strongLoadCAS,false);
    }
    /** branchCASSucc4 := BranchZero(Neq(StrongLoadCAS(a, b, c), b)) */
    protected boolean visitBranchCASSucc4(Operation branchZero,
                                          Instruction neq,
                                          Instruction strongLoadCAS) {
        return doBranchCAS(branchZero,strongLoadCAS,true);
    }
    /** branchCASFail4 := BranchNonZero(Neq(StrongLoadCAS(a, b, c), b)) */
    protected boolean visitBranchCASFail4(Operation branchNonZero,
                                          Instruction neq,
                                          Instruction strongLoadCAS) {
        return doBranchCAS(branchNonZero,strongLoadCAS,false);
    }
    /** branchCASSucc5 := BranchNonZero(Eq(b, StrongLoadCAS(a, b, c))) */
    protected boolean visitBranchCASSucc5(Operation branchNonZero,
                                          Instruction eq,
                                          Instruction strongLoadCAS) {
        return doBranchCAS(branchNonZero,strongLoadCAS,true);
    }
    /** branchCASFail5 := BranchZero(Eq(b, StrongLoadCAS(a, b, c))) */
    protected boolean visitBranchCASFail5(Operation branchZero,
                                          Instruction eq,
                                          Instruction strongLoadCAS) {
        return doBranchCAS(branchZero,strongLoadCAS,false);
    }
    /** branchCASSucc6 := BranchZero(Neq(b, StrongLoadCAS(a, b, c))) */
    protected boolean visitBranchCASSucc6(Operation branchZero,
                                          Instruction neq,
                                          Instruction strongLoadCAS) {
        return doBranchCAS(branchZero,strongLoadCAS,true);
    }
    /** branchCASFail6 := BranchNonZero(Neq(b, StrongLoadCAS(a, b, c))) */
    protected boolean visitBranchCASFail6(Operation branchNonZero,
                                          Instruction neq,
                                          Instruction strongLoadCAS) {
        return doBranchCAS(branchNonZero,strongLoadCAS,false);
    }
    /** branchNZLoad := BranchNonZero(Load(a)) */
    protected boolean visitBranchNZLoad(Operation branchNonZero,
                                        Instruction load) {
        return doBranchCmpAddrVal(branchNonZero,load,load.lhs().type().makeZero(),LOpCode.BranchNeq);
    }
    /** branchZLoad := BranchZero(Load(a)) */
    protected boolean visitBranchZLoad(Operation branchZero,
                                       Instruction load) {
        return doBranchCmpAddrVal(branchZero,load,load.lhs().type().makeZero(),LOpCode.BranchEq);
    }
    /** branchNonZero := BranchNonZero(a) */
    protected boolean visitBranchNonZero(Operation branchNonZero) {
        return doBranchCmp(branchNonZero,branchNonZero.rhs(0),branchNonZero.rhs(0).type().makeZero(),LOpCode.BranchNeq);
    }
    /** branchZero := BranchZero(a) */
    protected boolean visitBranchZero(Operation branchZero) {
        return doBranchCmp(branchZero,branchZero.rhs(0),branchZero.rhs(0).type().makeZero(),LOpCode.BranchEq);
    }
    /** eqLoad1 := Eq(Load(a), b) */
    protected boolean visitEqLoad1(Operation eq,
                                   Instruction load) {
        if (load.lhs().isFloat()) {
            return doSetFEqLoad(eq,load,eq.rhs(1));
        } else {
            return doSetCmpAddrVal(eq,load,eq.rhs(1),LOpCode.SetEq);
        }
    }
    /** eqLoad2 := Eq(a, Load(b)) */
    protected boolean visitEqLoad2(Operation eq,
                                   Instruction load) {
        if (load.lhs().isFloat()) {
            return doSetFEqLoad(eq,load,eq.rhs(0));
        } else {
            return doSetCmpAddrVal(eq,load,eq.rhs(0),LOpCode.SetEq);
        }
    }
    /** eq := Eq(a, b) */
    protected boolean visitEq(Operation eq) {
        if (eq.rhs(0).isFloat()) {
            return doSetFEq(eq,eq.rhs(0),eq.rhs(1));
        } else {
            return doSetCmp(eq,eq.rhs(0),eq.rhs(1),LOpCode.SetEq);
        }
    }
    /** neqLoad1 := Neq(Load(a), b) */
    protected boolean visitNeqLoad1(Operation neq,
                                    Instruction load) {
        if (load.lhs().isFloat()) {
            return doSetFNeqLoad(neq,load,neq.rhs(1));
        } else {
            return doSetCmpAddrVal(neq,load,neq.rhs(1),LOpCode.SetNeq);
        }
    }
    /** neqLoad2 := Neq(a, Load(b)) */
    protected boolean visitNeqLoad2(Operation neq,
                                    Instruction load) {
        if (load.lhs().isFloat()) {
            return doSetFNeqLoad(neq,load,neq.rhs(0));
        } else {
            return doSetCmpAddrVal(neq,load,neq.rhs(0),LOpCode.SetNeq);
        }
    }
    /** neq := Neq(a, b) */
    protected boolean visitNeq(Operation neq) {
        if (neq.rhs(0).isFloat()) {
            return doSetFNeq(neq,neq.rhs(0),neq.rhs(1));
        } else {
            return doSetCmp(neq,neq.rhs(0),neq.rhs(1),LOpCode.SetNeq);
        }
    }
    /** lessThanLoad1 := LessThan(Load(a), b) */
    protected boolean visitLessThanLoad1(Operation lessThan,
                                         Instruction load) {
        if (load.lhs().isFloat()) {
            return doSetCmpValAddr(lessThan,load,lessThan.rhs(1),LOpCode.SetFGreaterThan);
        } else {
            return doSetCmpAddrVal(lessThan,load,lessThan.rhs(1),LOpCode.SetLessThan);
        }
    }
    /** lessThanLoad2 := LessThan(a, Load(b)) */
    protected boolean visitLessThanLoad2(Operation lessThan,
                                         Instruction load) {
        if (load.lhs().isFloat()) {
            return doSetCmpAddrVal(lessThan,load,lessThan.rhs(0),LOpCode.SetFGreaterThan);
        } else {
            return doSetCmpValAddr(lessThan,load,lessThan.rhs(0),LOpCode.SetLessThan);
        }
    }
    /** lessThan := LessThan(a, b) */
    protected boolean visitLessThan(Operation lessThan) {
        if (lessThan.rhs(0).isFloat()) {
            return doSetCmp(lessThan,lessThan.rhs(1),lessThan.rhs(0),LOpCode.SetFGreaterThan);
        } else {
            return doSetCmp(lessThan,lessThan.rhs(0),lessThan.rhs(1),LOpCode.SetLessThan);
        }
    }
    /** uLessThanLoad1 := ULessThan(Load(a), b) */
    protected boolean visitULessThanLoad1(Operation uLessThan,
                                          Instruction load) {
        return doSetCmpAddrVal(uLessThan,load,uLessThan.rhs(1),LOpCode.SetULessThan);
    }
    /** uLessThanLoad2 := ULessThan(a, Load(b)) */
    protected boolean visitULessThanLoad2(Operation uLessThan,
                                          Instruction load) {
        return doSetCmpValAddr(uLessThan,load,uLessThan.rhs(0),LOpCode.SetULessThan);
    }
    /** uLessThan := ULessThan(a, b) */
    protected boolean visitULessThan(Operation uLessThan) {
        return doSetCmp(uLessThan,uLessThan.rhs(0),uLessThan.rhs(1),LOpCode.SetULessThan);
    }
    /** lessThanEqLoad1 := LessThanEq(Load(a), b) */
    protected boolean visitLessThanEqLoad1(Operation lessThanEq,
                                           Instruction load) {
        if (load.lhs().isFloat()) {
            return doSetCmpValAddr(lessThanEq,load,lessThanEq.rhs(1),LOpCode.SetFGTEq);
        } else {
            return doSetCmpAddrVal(lessThanEq,load,lessThanEq.rhs(1),LOpCode.SetLTEq);
        }
    }
    /** lessThanEqLoad2 := LessThanEq(a, Load(b)) */
    protected boolean visitLessThanEqLoad2(Operation lessThanEq,
                                           Instruction load) {
        if (load.lhs().isFloat()) {
            return doSetCmpAddrVal(lessThanEq,load,lessThanEq.rhs(0),LOpCode.SetFGTEq);
        } else {
            return doSetCmpValAddr(lessThanEq,load,lessThanEq.rhs(0),LOpCode.SetLTEq);
        }
    }
    /** lessThanEq := LessThanEq(a, b) */
    protected boolean visitLessThanEq(Operation lessThanEq) {
        if (lessThanEq.rhs(0).isFloat()) {
            return doSetCmp(lessThanEq,lessThanEq.rhs(1),lessThanEq.rhs(0),LOpCode.SetFGTEq);
        } else {
            return doSetCmp(lessThanEq,lessThanEq.rhs(0),lessThanEq.rhs(1),LOpCode.SetLTEq);
        }
    }
    /** uLessThanEqLoad1 := ULessThanEq(Load(a), b) */
    protected boolean visitULessThanEqLoad1(Operation uLessThanEq,
                                            Instruction load) {
        return doSetCmpAddrVal(uLessThanEq,load,uLessThanEq.rhs(1),LOpCode.SetULTEq);
    }
    /** uLessThanEqLoad2 := ULessThanEq(a, Load(b)) */
    protected boolean visitULessThanEqLoad2(Operation uLessThanEq,
                                            Instruction load) {
        return doSetCmpValAddr(uLessThanEq,load,uLessThanEq.rhs(0),LOpCode.SetULTEq);
    }
    /** uLessThanEq := ULessThanEq(a, b) */
    protected boolean visitULessThanEq(Operation uLessThanEq) {
        return doSetCmp(uLessThanEq,uLessThanEq.rhs(0),uLessThanEq.rhs(1),LOpCode.SetULTEq);
    }
    /** notLessThanLoad1 := Not(LessThan(Load(a), b)) */
    protected boolean visitNotLessThanLoad1(Operation not,
                                            Instruction lessThan,
                                            Instruction load) {
        // NOTE: this should always be float.  but oh well.
        if (load.lhs().type().isFloat()) {
            return doSetCmpValAddr(not,load,lessThan.rhs(1),LOpCode.SetNotFGT);
        } else {
            return doSetCmpValAddr(not,load,lessThan.rhs(1),LOpCode.SetLTEq);
        }
    }
    /** notLessThanLoad2 := Not(LessThan(a, Load(b))) */
    protected boolean visitNotLessThanLoad2(Operation not,
                                            Instruction lessThan,
                                            Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doSetCmpAddrVal(not,load,lessThan.rhs(0),LOpCode.SetNotFGT);
        } else {
            return doSetCmpAddrVal(not,load,lessThan.rhs(0),LOpCode.SetLTEq);
        }
    }
    /** notLessThan := Not(LessThan(a, b)) */
    protected boolean visitNotLessThan(Operation not,
                                       Instruction lessThan) {
        if (lessThan.rhs(0).type().isFloat()) {
            return doSetCmp(not,lessThan.rhs(1),lessThan.rhs(0),LOpCode.SetNotFGT);
        } else {
            return doSetCmp(not,lessThan.rhs(1),lessThan.rhs(0),LOpCode.SetLTEq);
        }
    }
    /** notLessThanEqLoad1 := Not(LessThanEq(Load(a), b)) */
    protected boolean visitNotLessThanEqLoad1(Operation not,
                                              Instruction lessThanEq,
                                              Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doSetCmpValAddr(not,load,lessThanEq.rhs(1),LOpCode.SetNotFGTEq);
        } else {
            return doSetCmpValAddr(not,load,lessThanEq.rhs(1),LOpCode.SetLessThan);
        }
    }
    /** notLessThanEqLoad2 := Not(LessThanEq(a, Load(b))) */
    protected boolean visitNotLessThanEqLoad2(Operation not,
                                              Instruction lessThanEq,
                                              Instruction load) {
        if (load.lhs().type().isFloat()) {
            return doSetCmpAddrVal(not,load,lessThanEq.rhs(0),LOpCode.SetNotFGTEq);
        } else {
            return doSetCmpAddrVal(not,load,lessThanEq.rhs(0),LOpCode.SetLessThan);
        }
    }
    /** notLessThanEq := Not(LessThanEq(a, b)) */
    protected boolean visitNotLessThanEq(Operation not,
                                         Instruction lessThanEq) {
        if (lessThanEq.rhs(0).type().isFloat()) {
            return doSetCmp(not,lessThanEq.rhs(1),lessThanEq.rhs(0),LOpCode.SetNotFGTEq);
        } else {
            return doSetCmp(not,lessThanEq.rhs(1),lessThanEq.rhs(0),LOpCode.SetLessThan);
        }
    }
    /** memcpy := Memcpy(a,b,c) */
    protected boolean visitMemcpy(Operation memcpy) {
        if (memcpy.rhs(2) instanceof PointerConst) {
            PointerConst pc=(PointerConst)memcpy.rhs(2);
            if (pc.value()>=0 && pc.value()<=80) {
                LArg dst=address(memcpy.rhs(0));
                LArg src=address(memcpy.rhs(1));
                
                if (pc.value()>Global.pointerSize*2) {
                    dst=simplifyAddr(dst);
                    src=simplifyAddr(src);
                }
                
                for (int i=0;i<pc.value();) {
                    int toCopy=(int)pc.value()-i;

                    if (toCopy>Global.pointerSize) {
                        toCopy=Global.pointerSize;
                    }

                    switch (toCopy) {
                    case 8:
                        prev.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Quad,
                                new LArg[]{
                                    src.offset(i),
                                    Reg.AX
                                }));
                        prev.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Quad,
                                new LArg[]{
                                    Reg.AX,
                                    dst.offset(i)
                                }));
                        break;
                    case 7:
                    case 6:
                    case 5:
                    case 4:
                        toCopy=4;
                        prev.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Long,
                                new LArg[]{
                                    src.offset(i),
                                    Reg.AX
                                }));
                        prev.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Long,
                                new LArg[]{
                                    Reg.AX,
                                    dst.offset(i)
                                }));
                        break;
                    case 3:
                    case 2:
                        toCopy=2;
                        prev.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Half,
                                new LArg[]{
                                    src.offset(i),
                                    Reg.AX
                                }));
                        prev.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Half,
                                new LArg[]{
                                    Reg.AX,
                                    dst.offset(i)
                                }));
                        break;
                    case 1:
                        prev.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Byte,
                                new LArg[]{
                                    src.offset(i),
                                    Reg.AX
                                }));
                        prev.prepend(
                            new LOp(
                                LOpCode.Mov,LType.Byte,
                                new LArg[]{
                                    Reg.AX,
                                    dst.offset(i)
                                }));
                        break;
                    default:
                        throw new Error("toCopy = "+toCopy);
                    }
                    
                    i+=toCopy;
                }
                
                return true;
            }
        }
        
        set(LType.ptr(),
            new OutParamSlot(0),
            memcpy.rhs(0));
        set(LType.ptr(),
            new OutParamSlot(Global.pointerSize),
            memcpy.rhs(1));
        set(LType.ptr(),
            new OutParamSlot(Global.pointerSize*2),
            memcpy.rhs(2));
        prev.prepend(
            new LOp(
                LOpCode.Call,LType.ptr(),
                new LArg[]{
                    new SymImm(CTypesystemReferences.memcpy,0)
                }));
        
        return true;
    }
    /** call := Call() */
    protected boolean visitCall(Operation call_) {
        CFieldInst call=(CFieldInst)call_;
        Function f=(Function)context.translateField(call.function());
        
        pushArgs(f.getSignature(),call,0);
        
        prev.prepend(
            new LOp(
                LOpCode.Call,LType.ptr(),
                new LArg[]{
                    new SymImm(f,0)
                }));
        
        popRes(f.getSignature(),call);
        
        return true;
    }
    /** callIndirectLoad := CallIndirect(Load(a)) */
    protected boolean visitCallIndirectLoad(Operation callIndirect_,
                                            Instruction load_) {
        CallIndirectInst call=(CallIndirectInst)callIndirect_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        pushArgs(call.signature(),call,1);
        
        prev.prepend(
            new LOp(
                LOpCode.Call,LType.ptr(),
                new LArg[]{
                    address(load.rhs(0))
                }));
        
        popRes(call.signature(),call);
        
        return true;
    }
    /** callIndirect := CallIndirect(a) */
    protected boolean visitCallIndirect(Operation callIndirect_) {
        CallIndirectInst call=(CallIndirectInst)callIndirect_;
        
        pushArgs(call.signature(),call,1);
        
        prev.prepend(
            new LOp(
                LOpCode.Call,LType.ptr(),
                new LArg[]{
                    value(LType.ptr(),call.rhs(0),0)
                }));
        
        popRes(call.signature(),call);
        
        return true;
    }
    /** rawReturn := RawReturn() */
    protected boolean visitRawReturn(Operation rawReturn) {
        if (rawReturn.nrhs()==1) {
            Basetype cresult=rawReturn.rhs(0).type().effectiveBasetype();
            LType lresult=LType.from(cresult);
        
            switch (cresult) {
            case VOID:
                break;
            case INT:
            case POINTER:
            case LONG:
                set(lresult,Reg.AX,rawReturn.rhs(0));
                break;
            case FLOAT:
                if (Global.pointerSize==4) {
                    if (rawReturn.rhs(0) instanceof FloatConst) {
                        FloatConst fc=(FloatConst)rawReturn.rhs(0);
                        if (fc.value()==0.0f) {
                            prev.prepend(
                                new LOp(
                                    LOpCode.PushFP0,LType.Double,
                                    LArg.EMPTY));
                        } else if (fc.value()==1.0f) {
                            prev.prepend(
                                new LOp(
                                    LOpCode.PushFP1,LType.Double,
                                    LArg.EMPTY));
                        } else {
                            return false;
                        }
                    } else {
                        Instruction i=findInstruction((Var)rawReturn.rhs(0));
                        if (i!=null && i.opcode()==OpCode.Load && !context.scc.hasSide(i.lhs())) {
                            // FIXME: currently this case pretty much never works.
                            // between the user's Load and the RawReturn there will
                            // be side-effects; specifically, the epilogue generated
                            // by the henderson frames pass.
                            acceptedOperation(i);
                            prev.prepend(
                                new LOp(
                                    LOpCode.PushFP,LType.Single,
                                    new LArg[]{
                                        address(i.rhs(0))
                                    }));
                        } else {
                            prev.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Single,
                                    new LArg[]{
                                        value(LType.Single,rawReturn.rhs(0),0),
                                        new OffScratchSlot(context.memBounce(),0)
                                    }));
                            prev.prepend(
                                new LOp(
                                    LOpCode.PushFP,LType.Single,
                                    new LArg[]{
                                        new OffScratchSlot(context.memBounce(),0)
                                    }));
                        }
                    }
                } else {
                    prev.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Single,
                            new LArg[]{
                                value(LType.Single,rawReturn.rhs(0),0),
                                Reg.XMM0
                            }));
                }
                break;
            case DOUBLE:
                if (Global.pointerSize==4) {
                    if (rawReturn.rhs(0) instanceof DoubleConst) {
                        DoubleConst dc=(DoubleConst)rawReturn.rhs(0);
                        if (dc.value()==0.0f) {
                            prev.prepend(
                                new LOp(
                                    LOpCode.PushFP0,LType.Double,
                                    LArg.EMPTY));
                        } else if (dc.value()==1.0f) {
                            prev.prepend(
                                new LOp(
                                    LOpCode.PushFP1,LType.Double,
                                    LArg.EMPTY));
                        } else {
                            return false;
                        }
                    } else {
                        Instruction i=findInstruction((Var)rawReturn.rhs(0));
                        if (i!=null && i.opcode()==OpCode.Load && !context.scc.hasSide(i.lhs())) {
                            acceptedOperation(i);
                            prev.prepend(
                                new LOp(
                                    LOpCode.PushFP,LType.Double,
                                    new LArg[]{
                                        address(i.rhs(0))
                                    }));
                        } else {
                            prev.prepend(
                                new LOp(
                                    LOpCode.Mov,LType.Double,
                                    new LArg[]{
                                        value(LType.Double,rawReturn.rhs(0),0),
                                        new OffScratchSlot(context.memBounce(),0)
                                    }));
                            prev.prepend(
                                new LOp(
                                    LOpCode.PushFP,LType.Double,
                                    new LArg[]{
                                        new OffScratchSlot(context.memBounce(),0)
                                    }));
                        }
                    }
                } else {
                    prev.prepend(
                        new LOp(
                            LOpCode.Mov,LType.Double,
                            new LArg[]{
                                value(LType.Double,rawReturn.rhs(0),0),
                                Reg.XMM0
                            }));
                }
                break;
            default:
                throw new CompilerException("bad type: "+cresult);
            }
        }
        
        lh.setFooter(
            new LFooter(
                LOpCode.Return,LType.Void,
                LArg.EMPTY,LHeader.EMPTY));
        
        return true;
    }
    /** notReached := NotReached() */
    protected boolean visitNotReached(Operation notReached) {
        lh.setFooter(
            new LFooter(
                LOpCode.NotReached,LType.Void,
                LArg.EMPTY,LHeader.EMPTY));
        return true;
    }
    /** jump := Jump() */
    protected boolean visitJump(Operation jump) {
        lh.setFooter(
            new LFooter(
                LOpCode.Jump,LType.Void,
                LArg.EMPTY,new LHeader[]{
                    context.headMap.get(((Jump)jump).defaultSuccessor())
                }));
        return true;
    }
    /** awesomeJumpLoad := AwesomeJump(Load(a)) */
    protected boolean visitAwesomeJumpLoad(Operation awesomeJump_,
                                           Instruction load_) {
        if (sideCross()) {
            return false;
        }
        
        AwesomeJump awesomeJump=(AwesomeJump)awesomeJump_;
        MemoryAccessInst load=(MemoryAccessInst)load_;
        
        LHeader[] targs=new LHeader[awesomeJump.numTargets()];
        for (int i=0;i<awesomeJump.numTargets();++i) {
            targs[i]=context.headMap.get(awesomeJump.target(i));
        }
        
        lh.setFooter(
            new LFooter(
                LOpCode.AwesomeJump,LType.ptr(),
                new LArg[]{
                    address(load.rhs(0))
                },
                targs));
        
        return true;
    }
    /** awesomeJump := AwesomeJump(a) */
    protected boolean visitAwesomeJump(Operation awesomeJump_) {
        AwesomeJump awesomeJump=(AwesomeJump)awesomeJump_;
        
        LHeader[] targs=new LHeader[awesomeJump.numTargets()];
        for (int i=0;i<awesomeJump.numTargets();++i) {
            targs[i]=context.headMap.get(awesomeJump.target(i));
        }
        
        lh.setFooter(
            new LFooter(
                LOpCode.AwesomeJump,LType.ptr(),
                new LArg[]{
                    value(LType.ptr(),awesomeJump.rhs(0),0)
                },
                targs));
        
        return true;
    }
    /** mov := Mov(a) */
    protected boolean visitMov(Operation mov) {
        return doMov(mov);
    }
    /** phi := Phi(a) */
    protected boolean visitPhi(Operation phi) {
        return doMov(phi);
    }
    /** ipsilon := Ipsilon(a) */
    protected boolean visitIpsilon(Operation ipsilon) {
        return doMov(ipsilon);
    }
    /** fence := Fence() */
    protected boolean visitFence(Operation fence) {
        prev.prepend(
            new LOp(
                LOpCode.Fence,LType.Void,
                LArg.EMPTY));
        return true;
    }
    /** hardCompilerFence := HardCompilerFence() */
    protected boolean visitHardCompilerFence(Operation fence) {
        prev.prepend(
            new LOp(
                LOpCode.Fence,LType.Void,
                LArg.EMPTY));
        return true;
    }
    /** hardUse := HardUse(a) */
    protected boolean visitHardUse(Operation hardUse) {
        // ignore
        return true;
    }
    
    public void build(LHeader lh,
                      Operation o) {
        if (!offlimits.contains(o)) {
            this.lh=lh;
            this.prev=(LOp)lh.next;
            if (!accept(o)) {
                throw new CompilerException("Unrecognized operation: "+o);
            }
            for (int i=0;i<numOpVars;++i) {
                skipped.add(opVars[i]);
            }
        }
    }
}

