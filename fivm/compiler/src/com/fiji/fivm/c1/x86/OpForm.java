/*
 * OpForm.java
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
import com.fiji.fivm.c1.x86.arg.Reg;

public enum OpForm {
    /** Takes one operand, which it uses and defs.  May be memory or register. */
    UNARY,
        
    /** Takes one operand, which it uses. */
    UNARY_LOAD,
    
    /** Takes one operand, which it defs. */
    UNARY_STORE,
    
    /** Takes two operands.  Uses the first, uses and defs the second.  Either
     * operand may be memory. */
    BINARY,
    
    /** Takes two operands.  Uses the first, defs the second.  Either operand
     * may be memory. */
    TRANSFER,
        
    /** Takes two operands.  Uses the first, defs the second.  The first operand
     * may be memory.  The second operand must be a register.  The first operand
     * must be compatible with integers, while the second must be compatible with
     * floats. */
    FIAT_TO_FLOAT,

    /** Takes two operands.  Uses the first, defs the second.  The second
     * operand may be memory.  The first operand must be a register.  The
     * first operand must be compatible with floats, while the second must be
     * compatible with integers. */
    FIAT_TO_INT,
    
    /** Takes two operands.  Uses both, defs the second.  Only the first
     * may be memory. */
    LOAD_BINARY,
    
    /** Takes two operands.  Uses the first, defs the second.  Only the first
     * may be memory. */
    LOAD_TRANSFER,
    
    /** Takes two operands.  Both need to be variables.  Uses both, defs the second*/
    REG_BINARY,
        
    /** Takes two operands.  The first must be a memory operand, whose address
     * is evaluated, and then stored into the second operand. */
    LEA,

    /** One operand.  Uses it, and uses AX, and defs AX and DX. */
    MUL,
        
    /** Takes three operands.  The first one must be an immediate.  The second
     * one may be a register or memory.  The third one must be a register.
     * Uses first two, defines the last. */
    MEGA_MUL,
    
    /** One operand.  Uses it, and uses and defs AX and DX. */
    DIV,
    
    /** Uses and defs its second operand, and uses either CX or an immediate.
     * CX or an immediate must be the first operand. */
    SHIFT,
        
    /** Double-precisions shifts; uses and defs its third operand, and uses either
     * CX or an immediate in the first operand as well as an arbitrary first operand,
     * which must be a variable (not memory). */
    DBL_SHIFT,
    
    /** Takes three operands.  Uses the first two, defs the last.  Of the first two,
     * one can be a memory operand. */
    COMPARE,
        
    /** Takes three operands.  Uses the first two, defs the last.  Of the first
     * two, only the second can be a memory operand. */
    FLOAT_COMPARE,
        
    /* Takes one operand, which it defs. */
    UNARY_DEF,
    
    /** Does not use any operands.  May be a jump, a fence, or a not-reached. */
    EFFECT,
        
    /** Does not use any operands, but uses registers for returning. */
    RETURN,
    
    /** Takes one operand, which it uses.  Defs, but does not use, all volatile
     * registers. */
    CALL,
        
    /** Tkes one operand, which it uses. */
    SWITCH,
    
    /** Takes two operands, which it uses.  Does a conditional control flow
     * transfer.  Either of those operands may be memory. */
    BRANCH,

    /** Takes two operands, which it uses.  Does a conditional control flow
     * transfer.  Of the two operands, only the second can be a memory operand. */
    FLOAT_BRANCH,

    /** Load CAS.  Takes two operand: a memory operand and a new value variable .  Uses the
     * memory operand and the new value, and uses and defs AX.  This also subsumes CAS
     * branches. */
    LOAD_CAS,
    
    /** Test CAS.  Takes three operands: a memory operand, a new value variable, and a
     * register operand.  Uses the memory operand and the new value variable, uses and defs AX, and
     * defs the second operand. */
    TEST_CAS,

    /** Reads AX, writes AX and DX. */
    CDQ,
        
    /** Branches on whatever the last branch branched on.  Can only be used in
     * an empty basic block all of whose predecessors have a compatible branch as
     * their footer. */
    REBRANCH;
    
    // FIXME: add the notion of register clobber
    
    public int nArgs() {
        switch (this) {
        case EFFECT:
        case CDQ:
        case REBRANCH:
        case RETURN:
            return 0;
        case UNARY:
        case UNARY_LOAD:
        case UNARY_STORE:
        case CALL:
        case SWITCH:
        case DIV:
        case MUL:
        case UNARY_DEF:
            return 1;
        case BINARY: 
        case REG_BINARY:
        case TRANSFER:
        case FIAT_TO_FLOAT:
        case FIAT_TO_INT:
        case LOAD_BINARY:
        case LOAD_TRANSFER:
        case SHIFT:
        case BRANCH:
        case FLOAT_BRANCH:
        case LEA:
        case LOAD_CAS:
            return 2;
        case COMPARE:
        case FLOAT_COMPARE:
        case TEST_CAS:
        case DBL_SHIFT:
        case MEGA_MUL:
            return 3;
        default: throw new CompilerException("bad op form");
        }
    }
    
    public boolean defArg(int i) {
        switch (this) {
        case EFFECT:
        case RETURN:
        case CALL:
        case BRANCH:
        case FLOAT_BRANCH:
        case SWITCH:
        case DIV:
        case MUL:
        case CDQ:
        case REBRANCH:
        case UNARY_LOAD:
            return false;
        case UNARY:
        case UNARY_STORE:
        case UNARY_DEF:
            return true;
        case LOAD_CAS:
            return i==0;
        case BINARY: 
        case REG_BINARY:
        case TRANSFER:
        case FIAT_TO_INT:
        case FIAT_TO_FLOAT:
        case LOAD_BINARY:
        case LOAD_TRANSFER:
        case SHIFT:
        case LEA:
            return i==1;
        case COMPARE:
        case FLOAT_COMPARE:
        case DBL_SHIFT:
        case MEGA_MUL:
            return i==2;
        case TEST_CAS:
            return i!=1;
        default:
            throw new CompilerException("bad op form");
        }
    }
    
    public boolean useArg(int i) {
        switch (this) {
        case EFFECT:
        case RETURN:
        case CDQ:
        case REBRANCH:
        case UNARY_STORE:
        case UNARY_DEF:
            return false;
        case CALL:
        case SWITCH:
        case BRANCH:
        case FLOAT_BRANCH:
        case UNARY:
        case UNARY_LOAD:
        case BINARY: 
        case REG_BINARY:
        case LOAD_BINARY:
        case SHIFT:
        case LOAD_CAS:
        case DIV:
        case MUL:
        case DBL_SHIFT:
            return true;
        case TRANSFER:
        case FIAT_TO_INT:
        case FIAT_TO_FLOAT:
        case LOAD_TRANSFER:
        case LEA:
            return i==0;
        case TEST_CAS:
        case COMPARE:
        case FLOAT_COMPARE:
        case MEGA_MUL:
            return i!=2;
        default:
            throw new CompilerException("bad op form: "+this);
        }
    }
    
    public boolean directUseArg(int i) {
        switch (this) {
        case EFFECT:
        case RETURN:
        case LEA:
        case CDQ:
        case REBRANCH:
        case UNARY_STORE:
        case UNARY_DEF:
            return false;
        case CALL:
        case SWITCH:
        case BRANCH:
        case FLOAT_BRANCH:
        case UNARY:
        case UNARY_LOAD:
        case BINARY: 
        case REG_BINARY:
        case LOAD_BINARY:
        case SHIFT:
        case LOAD_CAS:
        case DIV:
        case MUL:
        case DBL_SHIFT:
            return true;
        case TRANSFER:
        case FIAT_TO_INT:
        case FIAT_TO_FLOAT:
        case LOAD_TRANSFER:
            return i==0;
        case TEST_CAS:
        case COMPARE:
        case FLOAT_COMPARE:
        case MEGA_MUL:
            return i!=2;
        default:
            throw new CompilerException("bad op form");
        }
    }
    
    public void verifyRhs(LArg[] rhs) {
        switch (this) {
        case EFFECT:
        case RETURN:
        case CDQ:
        case REBRANCH:
            assert rhs.length==0;
            break;
        case UNARY_LOAD:
        case UNARY_STORE:
            assert rhs.length==1;
            assert rhs[0].memory();
            break;
        case UNARY:
            assert rhs.length==1;
            assert rhs[0].lvalue();
            break;
        case CALL:
        case SWITCH:
            assert rhs.length==1;
            assert rhs[0].compatibleWith(LType.ptr());
            break;
        case BINARY: 
        case TRANSFER:
            assert rhs.length==2;
            assert !(rhs[0].memory() && rhs[1].memory());
            assert rhs[1].lvalue();
            break;
        case FIAT_TO_INT:
            assert rhs.length==2;
            assert rhs[0].variable();
            assert rhs[0].compatibleWith(Kind.FLOAT);
            assert rhs[1].lvalue();
            assert rhs[1].compatibleWith(Kind.INT);
            break;
        case FIAT_TO_FLOAT:
            assert rhs.length==2;
            assert rhs[1].variable();
            assert rhs[1].compatibleWith(Kind.FLOAT);
            assert rhs[0].lvalue();
            assert rhs[0].compatibleWith(Kind.INT);
            break;
        case LOAD_BINARY:
        case LOAD_TRANSFER:
            assert rhs.length==2;
            assert rhs[1].variable();
            break;
        case REG_BINARY:
            assert rhs.length==2;
            assert rhs[0].variable();
            assert rhs[1].variable();
            break;
        case LEA:
            assert rhs.length==2;
            assert rhs[0].memory();
            assert rhs[1].variable();
            break;
        case SHIFT:
            assert rhs.length==2;
            assert rhs[0].compatibleWith(Kind.INT);
            assert rhs[1].compatibleWith(Kind.INT);
            assert rhs[0].immediate() || rhs[0]==Reg.CX;
            assert rhs[1].lvalue();
            break;
        case DBL_SHIFT:
            assert rhs.length==3;
            assert rhs[0].compatibleWith(Kind.INT);
            assert rhs[1].compatibleWith(Kind.INT);
            assert rhs[2].compatibleWith(Kind.INT);
            assert rhs[0].immediate() || rhs[0]==Reg.CX;
            assert rhs[1].variable();
            assert rhs[2].lvalue();
            break;
        case BRANCH:
            assert rhs.length==2;
            assert !(rhs[0].memory() && rhs[1].memory());
            assert rhs[0].lvalue();
            break;
        case FLOAT_BRANCH:
            assert rhs.length==2;
            assert rhs[0].variable();
            assert rhs[1].lvalue();
            break;
        case LOAD_CAS:
            assert rhs.length==2;
            assert rhs[0].compatibleWith(LType.Long) || rhs[0].compatibleWith(LType.ptr());
            assert rhs[0].memory();
            assert rhs[1].variable() && rhs[1].compatibleWith(Kind.INT);
            break;
        case DIV:
        case MUL:
            assert rhs.length==1;
            assert rhs[0].lvalue();
            assert rhs[0].compatibleWith(LType.Long) || rhs[0].compatibleWith(LType.Quad);
            break;
        case COMPARE:
            assert rhs.length==3;
            assert !(rhs[0].memory() && rhs[1].memory());
            assert rhs[0].lvalue();
            assert rhs[2].variable();
            assert rhs[2].compatibleWith(LType.Byte);
            break;
        case FLOAT_COMPARE:
            assert rhs.length==3;
            assert rhs[0].variable();
            assert rhs[1].lvalue();
            assert rhs[2].variable();
            assert rhs[2].compatibleWith(LType.Byte);
            break;
        case UNARY_DEF:
            assert rhs.length==1;
            assert rhs[0].variable();
            break;
        case TEST_CAS:
            assert rhs.length==3;
            assert rhs[0].memory();
            assert rhs[0].compatibleWith(LType.Long) || rhs[0].compatibleWith(LType.ptr());
            assert rhs[1].variable();
            assert rhs[1].compatibleWith(Kind.INT);
            assert rhs[2].variable();
            assert rhs[2].compatibleWith(LType.Byte);
            break;
        case MEGA_MUL:
            assert rhs.length==3;
            assert rhs[0].compatibleWith(Kind.INT);
            assert rhs[1].compatibleWith(LType.Long) || rhs[1].compatibleWith(LType.Quad) || rhs[1].compatibleWith(LType.Half);
            assert rhs[2].compatibleWith(LType.Long) || rhs[2].compatibleWith(LType.Quad) || rhs[2].compatibleWith(LType.Half);
            assert rhs[0].immediate();
            assert rhs[1].lvalue();
            assert rhs[2].variable();
            break;
        default:
            throw new CompilerException("bad op form");
        }
    }
    
    private static final LArg[] AX=new LArg[]{Reg.AX};
    private static final LArg[] AX_DX=new LArg[]{Reg.AX,Reg.DX};
    private static final LArg[] SP=new LArg[]{Reg.SP};
    private static final LArg[] SP_AX=new LArg[]{Reg.SP,Reg.AX};
    private static final LArg[] SP_AX_DX=new LArg[]{Reg.SP,Reg.AX,Reg.DX};
    private static final LArg[] SP_XMM0=new LArg[]{Reg.SP,Reg.XMM0};
    
    // NOTE: these are only valid after we do expansion (Expand32)
    
    public LArg[] implicitDefs() {
        switch (this) {
        case CDQ:
        case DIV:
        case MUL:
            return AX_DX;
        case CALL:
            return Reg.volatiles;
        case LOAD_CAS:
        case TEST_CAS:
            return AX;
        default:
            return LArg.EMPTY;
        }
    }
    
    public LArg[] implicitDefsPretend() {
        if (this==CALL) {
            return Reg.pretendVolatiles;
        } else {
            return implicitDefs();
        }
    }
    
    public LArg[] implicitUses(LCode code) {
        switch (this) {
        case CDQ:
        case MUL:
        case LOAD_CAS:
        case TEST_CAS:
            return AX;
        case DIV:
            return AX_DX;
        case CALL:
            return SP;
        case RETURN: {
            LArg[] resultSimple;
            switch (code.returnType()) {
            case VOID:
                resultSimple=SP;
                break;
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case POINTER:
            case VM_FCPTR:
                resultSimple=SP_AX;
                break;
            case LONG:
                if (Global.pointerSize==4) {
                    resultSimple=SP_AX_DX;
                } else {
                    resultSimple=SP_AX;
                }
                break;
            case FLOAT:
            case DOUBLE:
                if (Global.pointerSize==4) {
                    resultSimple=SP;
                } else {
                    resultSimple=SP_XMM0;
                }
                break;
            default: throw new Error("Unknown return type: "+code.returnType());
            }
            
            if (code.stackBuilt) {
                LArg[] result=new LArg[resultSimple.length+code.persistentsUsed().size()];
                System.arraycopy(resultSimple,0,
                                 result,0,
                                 resultSimple.length);
                int i=resultSimple.length;
                for (Reg r : code.persistentsUsed()) {
                    result[i++]=r;
                }
                return result;
            } else {
                return resultSimple;
            }
        }
        default:
            return LArg.EMPTY;
        }
    }
    
    public boolean canBeMemory(LOp o,int i) {
        switch (this) {
        case EFFECT:
        case RETURN:
        case CDQ:
        case REBRANCH:
            throw new Error("no rhs");
        case UNARY:
        case UNARY_LOAD:
        case UNARY_STORE:
        case CALL:
        case SWITCH:
        case DIV:
        case MUL:
            return true;
        case BINARY: 
        case TRANSFER:
        case BRANCH:
            return !o.rhs()[i^1].memory();
        case COMPARE:
            return i!=2 && !o.rhs()[i^1].memory();
        case LOAD_BINARY:
        case LOAD_TRANSFER:
        case LEA:
        case LOAD_CAS:
        case TEST_CAS:
        case FIAT_TO_FLOAT:
            return i==0;
        case SHIFT:
        case MEGA_MUL:
        case FLOAT_COMPARE:
        case FLOAT_BRANCH:
        case FIAT_TO_INT:
            return i==1;
        case DBL_SHIFT:
            return i==2;
        case REG_BINARY:
        case UNARY_DEF:
            return false;
        default:
            throw new CompilerException("bad op form");
        }
    }
}


