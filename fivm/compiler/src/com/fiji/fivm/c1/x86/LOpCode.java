/*
 * LOpCode.java
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
import com.fiji.fivm.c1.x86.arg.LArg;

public enum LOpCode {
    Mov, // int and float
    PhantomDef,
    Lea,
    SignExtL,
    ZeroExtL,
    SignExtQ,
    ZeroExtQ, // NOTE: zero-extending from Long to Quad in 64-bit mode is just a Long Mov
    FiatToFloat, // type can be Long or Quad
    FiatToInt, // type can be Long or Quad
    First32,
    Second32,
    ToSingle,
    ToDouble,
    Add,
    Adc,
    Sub,
    Sbb,
    Mul,
    Megamul, // the three operand form of IMUL
    Muld,
    Div, // div doubles as mod.
    UDiv,
    Neg, // int and float
    FXor,
    FAdd,
    FSub,
    FMul,
    FDiv,
    FSqrt,
    UnpckLP,
    UnpckHP,
    PushFP,
    PopFP,
    PushIntToFP,
    PushFP0,
    PushFP1,
    Shl,
    Shld,
    Shr,
    Shrd,
    Ushr,
    And,
    Or,
    Xor,
    BitNot,
    Cdq,
    SetLessThan,
    SetGreaterThan,
    SetLTEq,
    SetGTEq,
    SetFGreaterThan, // seta
    SetFGTEq, // setae
    SetNotFGT, // setna
    SetNotFGTEq, // setb
    SetULessThan,
    SetUGreaterThan,
    SetULTEq,
    SetUGTEq,
    SetAndZero,
    SetAndNotZero,
    SetEq,
    SetNeq,
    SetFEqOrUnordered,
    SetFNeqAndOrdered,
    ResetFUnordered,
    ResetFOrdered,
    LoadCAS,
    TestCAS,
    Fence,
    Call,
    
    Return,
    NotReached,
    Jump,
    BranchLessThan,
    BranchGreaterThan,
    BranchLTEq,
    BranchGTEq,
    BranchFGreaterThan,
    BranchFGTEq,
    BranchNotFGT,
    BranchNotFGTEq,
    BranchULessThan,
    BranchUGreaterThan,
    BranchULTEq,
    BranchUGTEq,
    BranchAndZero,
    BranchAndNotZero,
    BranchEq,
    BranchNeq,
    BranchFEqOrUnordered,
    BranchFNeqAndOrdered,
    BranchCASSucc,
    BranchCASFail,
    RebranchLessThan,
    RebranchGreaterThan,
    RebranchLTEq,
    RebranchGTEq,
    RebranchFGreaterThan,
    RebranchFGTEq,
    RebranchNotFGT,
    RebranchNotFGTEq,
    RebranchULessThan,
    RebranchUGreaterThan,
    RebranchULTEq,
    RebranchUGTEq,
    RebranchEq,
    RebranchNeq,
    RebranchFUnordered,
    RebranchFOrdered,
    AwesomeJump;
    
    public boolean footer() {
        switch (this) {
        case Return:
        case NotReached:
        case Jump:
        case BranchLessThan:
        case BranchLTEq:
        case BranchULessThan:
        case BranchULTEq:
        case BranchAndZero:
        case BranchAndNotZero:
        case BranchEq:
        case BranchNeq:
        case BranchCASSucc:
        case BranchCASFail:
        case RebranchLessThan:
        case RebranchLTEq:
        case RebranchULessThan:
        case RebranchULTEq:
        case RebranchEq:
        case RebranchNeq:
        case BranchGreaterThan:
        case BranchGTEq:
        case BranchFGreaterThan:
        case BranchFGTEq:
        case BranchNotFGT:
        case BranchNotFGTEq:
        case BranchUGreaterThan:
        case BranchUGTEq:
        case RebranchGreaterThan:
        case RebranchGTEq:
        case RebranchFGreaterThan:
        case RebranchFGTEq:
        case RebranchNotFGT:
        case RebranchNotFGTEq:
        case RebranchUGreaterThan:
        case RebranchUGTEq:
        case BranchFEqOrUnordered:
        case BranchFNeqAndOrdered:
        case RebranchFUnordered:
        case RebranchFOrdered:
        case AwesomeJump:
            return true;
        default:
            return false;
        }
    }
    
    public boolean reversibleBinaryBranch() {
        if (!footer()) {
            return false;
        }
        
        switch (this) {
        case Return:
        case NotReached:
        case Jump:
        case AwesomeJump:
            return false;
        default:
            return true;
        }
    }
    
    public LOpCode swapRhs() {
        switch (this) {
        case SetLessThan:          return SetGreaterThan;
        case SetULessThan:         return SetUGreaterThan;
        case SetLTEq:              return SetGTEq;
        case SetULTEq:             return SetUGTEq;
        case SetEq:                return SetEq;
        case SetNeq:               return SetNeq;
        case SetFEqOrUnordered:    return SetFEqOrUnordered;
        case SetFNeqAndOrdered:    return SetFNeqAndOrdered;
        case SetAndZero:           return SetAndZero;
        case SetAndNotZero:        return SetAndNotZero;
        case SetGreaterThan:       return SetLessThan;
        case SetGTEq:              return SetLTEq;
        case SetUGreaterThan:      return SetULessThan;
        case SetUGTEq:             return SetULTEq;
        case ResetFUnordered:      return ResetFUnordered;
        case ResetFOrdered:        return ResetFOrdered;
        case BranchLessThan:       return BranchGreaterThan;
        case BranchLTEq:           return BranchGTEq;
        case BranchULessThan:      return BranchUGreaterThan;
        case BranchULTEq:          return BranchUGTEq;
        case BranchAndZero:        return BranchAndZero;
        case BranchAndNotZero:     return BranchAndNotZero;
        case BranchEq:             return BranchEq;
        case BranchNeq:            return BranchNeq;
        case BranchFEqOrUnordered: return BranchFEqOrUnordered;
        case BranchFNeqAndOrdered: return BranchFNeqAndOrdered;
        case RebranchLessThan:     return RebranchGreaterThan;
        case RebranchLTEq:         return RebranchGTEq;
        case RebranchULessThan:    return RebranchUGreaterThan;
        case RebranchULTEq:        return RebranchUGTEq;
        case RebranchEq:           return RebranchEq;
        case RebranchNeq:          return RebranchNeq;
        case BranchGreaterThan:    return BranchLessThan;
        case BranchGTEq:           return BranchLTEq;
        case BranchUGreaterThan:   return BranchULessThan;
        case BranchUGTEq:          return BranchULTEq;
        case RebranchGreaterThan:  return RebranchLessThan;
        case RebranchGTEq:         return RebranchLTEq;
        case RebranchUGreaterThan: return RebranchULessThan;
        case RebranchUGTEq:        return RebranchULTEq;
        case RebranchFUnordered:   return RebranchFUnordered;
        case RebranchFOrdered:     return RebranchFOrdered;
        default:                   return null;
        }
    }
    
    public boolean terminal() {
        switch (this) {
        case Return:
        case NotReached:
            return true;
        default:
            return false;
        }
    }
    
    public boolean dropsDown() {
        switch (this) {
        case Return:
        case NotReached:
        case AwesomeJump:
            return false;
        default:
            return true;
        }
    }
    
    public OpForm form() {
        switch (this) {
        case Mov:
        case First32:
        case Second32:
            return OpForm.TRANSFER;
        case FiatToFloat:
            return OpForm.FIAT_TO_FLOAT;
        case FiatToInt:
            return OpForm.FIAT_TO_INT;
        case SignExtL:
        case ZeroExtL:
        case SignExtQ:
        case ZeroExtQ:
        case FSqrt:
        case ToSingle:
        case ToDouble:
            return OpForm.LOAD_TRANSFER;
        case PushFP:
        case PushIntToFP:
            return OpForm.UNARY_LOAD;
        case PopFP:
            return OpForm.UNARY_STORE;
        case Add:
        case Adc:
        case Sub:
        case Sbb:
        case And:
        case Or:
        case Xor:
            return OpForm.BINARY;
        case Div:
        case UDiv:
            return OpForm.DIV;
        case Muld:
            return OpForm.MUL;
        case FXor:
            return OpForm.REG_BINARY;
        case Neg:
        case BitNot:
            return OpForm.UNARY;
        case Mul:
        case FAdd:
        case FSub:
        case FMul:
        case FDiv:
        case UnpckLP:
        case UnpckHP:
            return OpForm.LOAD_BINARY;
        case Lea:
            return OpForm.LEA;
        case Shl:
        case Shr:
        case Ushr:
            return OpForm.SHIFT;
        case Shld:
        case Shrd:
            return OpForm.DBL_SHIFT;
        case SetLessThan:
        case SetULessThan:
        case SetLTEq:
        case SetULTEq:
        case SetEq:
        case SetNeq:
        case SetAndZero:
        case SetAndNotZero:
        case SetGreaterThan:
        case SetGTEq:
        case SetUGreaterThan:
        case SetUGTEq:
            return OpForm.COMPARE;
        case SetFGreaterThan:
        case SetFGTEq:
        case SetNotFGT:
        case SetNotFGTEq:
        case SetFEqOrUnordered:
        case SetFNeqAndOrdered:
            return OpForm.FLOAT_COMPARE;
        case ResetFUnordered:
        case ResetFOrdered:
        case PhantomDef:
            return OpForm.UNARY_DEF;
        case LoadCAS:
        case BranchCASSucc:
        case BranchCASFail:
            return OpForm.LOAD_CAS;
        case TestCAS:
            return OpForm.TEST_CAS;
        case Fence:
        case NotReached:
        case Jump:
        case PushFP0:
        case PushFP1:
            return OpForm.EFFECT;
        case Return:
            return OpForm.RETURN;
        case Cdq:
            return OpForm.CDQ;
        case Call:
            return OpForm.CALL;
        case AwesomeJump:
            return OpForm.SWITCH;
        case BranchLessThan:
        case BranchLTEq:
        case BranchULessThan:
        case BranchULTEq:
        case BranchAndZero:
        case BranchAndNotZero:
        case BranchEq:
        case BranchNeq:
        case BranchGreaterThan:
        case BranchGTEq:
        case BranchUGreaterThan:
        case BranchUGTEq:
            return OpForm.BRANCH;
        case BranchFGreaterThan:
        case BranchFGTEq:
        case BranchNotFGT:
        case BranchNotFGTEq:
        case BranchFEqOrUnordered:
        case BranchFNeqAndOrdered:
            return OpForm.FLOAT_BRANCH;
        case RebranchLessThan:
        case RebranchLTEq:
        case RebranchULessThan:
        case RebranchULTEq:
        case RebranchEq:
        case RebranchNeq:
        case RebranchGreaterThan:
        case RebranchGTEq:
        case RebranchFGreaterThan:
        case RebranchFGTEq:
        case RebranchNotFGT:
        case RebranchNotFGTEq:
        case RebranchUGreaterThan:
        case RebranchUGTEq:
        case RebranchFUnordered:
        case RebranchFOrdered:
            return OpForm.REBRANCH;
        case Megamul:
            return OpForm.MEGA_MUL;
        default:
            throw new CompilerException("bad opcode: "+this);
        }
    }
    
    // does this operation do anything other than define stuff?
    public boolean sideEffect() {
        switch (this) {
        case LoadCAS:
        case TestCAS:
        case Fence:
        case Call:
        case BranchCASSucc:
        case BranchCASFail:
        case PushFP: // hack
        case PushIntToFP: // hack
        case SetFEqOrUnordered: // hack
        case SetFNeqAndOrdered: // hack
        case PushFP0: // hack
        case PushFP1: // hack
            return true;
        default:
            return false;
        }
    }
    
    // what type does the opcode use at the given rhs index, if it is different from the
    // type of the LOp?
    public LType typeOf(LType opType,int i) {
        switch (this) {
        case SignExtL:
        case ZeroExtL:
            if (i==0) {
                return opType;
            } else {
                return LType.Long;
            }
        case SignExtQ:
        case ZeroExtQ:
            if (i==0) {
                return opType;
            } else {
                return LType.Quad;
            }
        case First32:
        case Second32:
            if (i==0) {
                return LType.Quad;
            } else {
                return LType.Long;
            }
        case ToSingle:
            if (i==0) {
                return opType;
            } else {
                return LType.Single;
            }
        case ToDouble:
            if (i==0) {
                return opType;
            } else {
                return LType.Double;
            }
        case SetLessThan:
        case SetULessThan:
        case SetLTEq:
        case SetULTEq:
        case SetGreaterThan:
        case SetUGreaterThan:
        case SetGTEq:
        case SetUGTEq:
        case SetFGreaterThan:
        case SetFGTEq:
        case SetNotFGT:
        case SetNotFGTEq:
        case SetEq:
        case SetNeq:
        case SetFEqOrUnordered:
        case SetFNeqAndOrdered:
        case SetAndZero:
        case SetAndNotZero:
        case TestCAS:
            if (i==2) {
                return LType.Byte;
            } else {
                return opType;
            }
        case Shl:
        case Shld:
        case Shr:
        case Shrd:
        case Ushr:
            if (i==0) {
                return LType.Byte;
            } else {
                return opType;
            }
        case FiatToInt:
            if (i==0) {
                return LType.from(Kind.FLOAT,opType.size());
            } else {
                return opType;
            }
        case FiatToFloat:
            if (i==0) {
                return opType;
            } else {
                return LType.from(Kind.FLOAT,opType.size());
            }
        default: return opType;
        }
    }
    
    // what type do the memory address operands have for the given opcode, if it is
    // different from ptr(), for the given LOp type?
    public LType memType(LType opType) {
        if (this==Lea) {
            return opType;
        } else {
            return LType.ptr();
        }
    }
    
    public int nArgs() {
        return form().nArgs();
    }
    
    public boolean defArg(int i) {
        return form().defArg(i);
    }
    
    public boolean useArg(int i) {
        return form().useArg(i);
    }
    
    public void verifyRhs(LArg[] rhs) {
        form().verifyRhs(rhs);
    }
    
    public boolean unsigned() {
        switch (this) {
        case ZeroExtL:
        case ZeroExtQ:
        case UDiv:
        case Ushr:
        case SetULessThan:
        case SetULTEq:
        case SetUGreaterThan:
        case SetUGTEq:
        case BranchULessThan:
        case BranchULTEq:
        case RebranchULessThan:
        case RebranchULTEq:
        case BranchUGreaterThan:
        case BranchUGTEq:
        case RebranchUGreaterThan:
        case RebranchUGTEq:
            return true;
        default:
            return false;
        }
    }
    
    public String asm(LType type) {
        switch (this) {
        case Mov:
            if (type.isInt()) {
                return "mov"+type.asm();
            } else {
                return "movs"+type.asm();
            }
        case Lea: return "lea"+type.asm();
        case SignExtL: return "movs"+type.asm()+"l";
        case ZeroExtL: return "movz"+type.asm()+"l";
        case SignExtQ: return "movs"+type.asm()+"q";
        case ZeroExtQ: return "movz"+type.asm()+"q";
        case ToSingle:
            switch (type) {
            case Long: return "cvtsi2ss";
            case Quad: return "cvtsi2ssq";
            case Double: return "cvtsd2ss";
            default: throw new CompilerException("Bad type for ToSingle: "+type);
            }
        case ToDouble:
            switch (type) {
            case Long: return "cvtsi2sd";
            case Quad: return "cvtsi2sdq";
            case Single: return "cvtss2sd";
            default: throw new CompilerException("Bad type for ToDouble: "+type);
            }
        case Add: return "add"+type.asm();
        case Adc: return "adc"+type.asm();
        case Sub: return "sub"+type.asm();
        case Sbb: return "sbb"+type.asm();
        case Mul:
        case Megamul: return "imul"+type.asm();
        case Muld: return "mul"+type.asm();
        case Div: return "idiv"+type.asm();
        case UDiv: return "div"+type.asm();
        case Neg: return "neg"+type.asm();
        case FXor: return "xorp"+type.asm();
        case FAdd: return "adds"+type.asm();
        case FSub: return "subs"+type.asm();
        case FMul: return "muls"+type.asm();
        case FDiv: return "divs"+type.asm();
        case FSqrt: return "sqrts"+type.asm();
        case PushIntToFP:
            switch (type) {
            case Quad: return "fildll";
            default: throw new CompilerException("Bad type for PushIntToFP: "+type);
            }
        case PushFP:
            if (type==LType.Single) {
                return "flds";
            } else {
                return "fldl";
            }
        case PopFP:
            if (type==LType.Single) {
                return "fstps";
            } else {
                return "fstpl";
            }
        case And: return "and"+type.asm();
        case Or: return "or"+type.asm();
        case Xor: return "xor"+type.asm();
        case BitNot: return "not"+type.asm();
        case Cdq: return "cdq";
        case Shl: return "sal"+type.asm();
        case Shld: return "shld"+type.asm();
        case Shr: return "sar"+type.asm();
        case Ushr: return "shr"+type.asm();
        case Shrd: return "shrd"+type.asm();
        case ResetFOrdered: return "setnp";
        case ResetFUnordered: return "setp";
        case FiatToInt:
        case FiatToFloat:
            if (type==LType.Long) {
                return "movd";
            } else {
                return "movdq";
            }
        case UnpckLP: return "unpcklp"+type.asm();
        case UnpckHP: return "unpckhp"+type.asm();
        case PushFP0: return "fldz";
        case PushFP1: return "fld1";
            
            // all other instructions have to be handled specially
            
        default: throw new CompilerException(
            "Cannot create single asm instruction for "+this+" and type "+type);
        }
    }
}

