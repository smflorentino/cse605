/*
 * Reg.java
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

package com.fiji.fivm.c1.x86.arg;

import com.fiji.fivm.*;
import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.*;

import java.util.*;

public enum Reg implements LArg {
    AX,
    CX,
    DX,
    BX,
    SP,
    BP,
    SI,
    DI,
    R8,
    R9,
    R10,
    R11,
    R12,
    R13,
    R14,
    R15,
    
    XMM0,
    XMM1,
    XMM2,
    XMM3,
    XMM4,
    XMM5,
    XMM6,
    XMM7,
    XMM8,
    XMM9,
    XMM10,
    XMM11,
    XMM12,
    XMM13,
    XMM14,
    XMM15;
    
    public static Reg intReg(int idx) {
        return table[idx+0];
    }
    
    public static Reg floatReg(int idx) {
        return table[idx+16];
    }
    
    public boolean lvalue() {
        return true;
    }
    
    public boolean memory() {
        return false;
    }
    
    public boolean immediate() {
        return false;
    }
    
    public boolean variable() {
        return true;
    }
    
    public boolean spillable() {
        return false;
    }
    
    public int id() {
        return ordinal();
    }
    
    public boolean compatibleWith(LType type) {
        switch (this) {
        case AX:
        case CX:
        case DX:
        case BX: return type.isInt();
        case SP:
        case BP:
        case SI:
        case DI:
        case R8:
        case R9:
        case R10:
        case R11:
        case R12:
        case R13:
        case R14:
        case R15: return type.isInt() && type!=LType.Byte;
        case XMM0:
        case XMM1:
        case XMM2:
        case XMM3:
        case XMM4:
        case XMM5:
        case XMM6:
        case XMM7:
        case XMM8:
        case XMM9:
        case XMM10:
        case XMM11:
        case XMM12:
        case XMM13:
        case XMM14:
        case XMM15: return type.isFloat();
        default: throw new CompilerException("bad register");
        }
    }
    
    public boolean compatibleWith(Kind kind) {
        return kind()==kind;
    }
    
    public Kind kind() {
        switch (this) {
        case AX:
        case CX:
        case DX:
        case BX:
        case SP:
        case BP:
        case SI:
        case DI:
        case R8:
        case R9:
        case R10:
        case R11:
        case R12:
        case R13:
        case R14:
        case R15: return Kind.INT;
        case XMM0:
        case XMM1:
        case XMM2:
        case XMM3:
        case XMM4:
        case XMM5:
        case XMM6:
        case XMM7:
        case XMM8:
        case XMM9:
        case XMM10:
        case XMM11:
        case XMM12:
        case XMM13:
        case XMM14:
        case XMM15: return Kind.FLOAT;
        default: throw new CompilerException("bad register");
        }
    }
    
    public boolean isInt() {
        return kind().isInt();
    }
    
    public boolean isFloat() {
        return kind().isFloat();
    }
    
    public int code() {
        switch (this) {
        case AX:
        case R8:
        case XMM0:
        case XMM8: return 0;
        case CX:
        case R9:
        case XMM1:
        case XMM9: return 1;
        case DX:
        case R10:
        case XMM2:
        case XMM10: return 2;
        case BX:
        case R11:
        case XMM3:
        case XMM11: return 3;
        case SP:
        case R12:
        case XMM4:
        case XMM12: return 4;
        case BP:
        case R13:
        case XMM5:
        case XMM13: return 5;
        case SI:
        case R14:
        case XMM6:
        case XMM14: return 6;
        case DI:
        case R15:
        case XMM7:
        case XMM15: return 7;
        default: throw new CompilerException("bad register");
        }
    }
    
    public boolean rex() {
        switch (this) {
        case AX:
        case BX:
        case CX:
        case DX:
        case SP:
        case BP:
        case SI:
        case DI:
        case XMM0:
        case XMM1:
        case XMM2:
        case XMM3:
        case XMM4:
        case XMM5:
        case XMM6:
        case XMM7: return false;
        case R8:
        case R9:
        case R10:
        case R11:
        case R12:
        case R13:
        case R14:
        case R15:
        case XMM8:
        case XMM9:
        case XMM10:
        case XMM11:
        case XMM12:
        case XMM13:
        case XMM14:
        case XMM15: return true;
        default: throw new CompilerException("bad register");
        }
    }
    
    public boolean allowed() {
        if (Global.pointerSize==8) {
            return true;
        } else {
            return !rex();
        }
    }
    
    // volatile = caller-save
    public boolean isVolatile() {
        switch (this) {
        case AX: return true;
        case BX: return false;
        case CX:
        case DX: return true;
        case SP: return true;
        case BP: return false;
        case SI:
        case DI: return Global.pointerSize==8;
        case R8:
        case R9:
        case R10:
        case R11: return true;
        case R12:
        case R13:
        case R14:
        case R15: return false;
        case XMM0:
        case XMM1:
        case XMM2:
        case XMM3:
        case XMM4:
        case XMM5:
        case XMM6:
        case XMM7:
        case XMM8:
        case XMM9:
        case XMM10:
        case XMM11:
        case XMM12:
        case XMM13:
        case XMM14:
        case XMM15: return true;
        default: throw new CompilerException("bad register");
        }
    }
    
    public boolean isPretendVolatile() {
        if (isInt()) {
            return isVolatile();
        } else {
            return code()<8-Global.floatPretendPersistent;
        }
    }
    
    // persistent = callee-save
    public boolean isPersistent() {
        return !isVolatile();
    }
    
    public boolean isPretendPersistent() {
        return !isPretendVolatile();
    }
    
    public boolean isUsableVolatile() {
        return isVolatile() && this!=SP;
    }
    
    public boolean isUsablePretendVolatile() {
        return isPretendVolatile() && this!=SP;
    }
    
    public boolean isUsablePersistent() {
        return isPersistent() && (Settings.OMIT_FRAME_POINTER || this!=BP);
    }
    
    public boolean isUsablePretendPersistent() {
        return isPretendPersistent() && (Settings.OMIT_FRAME_POINTER || this!=BP);
    }
    
    public boolean isUsable() {
        return this!=SP && (Settings.OMIT_FRAME_POINTER || this!=BP);
    }
    
    public boolean isReturn() {
        switch (this) {
        case AX: return true;
        case DX: return Global.pointerSize==4;
        case XMM0: return Global.pointerSize==8;
        default: return false;
        }
    }
    
    public static final Reg[] table;
    
    static {
        table=Reg.class.getEnumConstants();
        assert table!=null;
        for (int i=0;i<table.length;++i) {
            assert table[i].ordinal()==i;
        }
    }

    public static final Reg[] volatiles;
    public static final Reg[] persistents;
    public static final Reg[] pretendVolatiles;
    public static final Reg[] pretendPersistents;
    public static final Reg[] ints;
    public static final Reg[] floats;
    public static final Reg[] usableVolatiles;
    public static final Reg[] usablePersistents;
    public static final Reg[] usableInts;
    public static final Reg[] usableFloats;
    public static final Reg[] usableIntVolatiles;
    public static final Reg[] usableIntPersistents;
    public static final Reg[] usableFloatVolatiles;
    public static final Reg[] usableFloatPersistents;
    public static final Reg[] usableFloatPretendVolatiles;
    public static final Reg[] usableFloatPretendPersistents;
    
    public static final Reg[] returnRegs;
    
    static {
        int nVolatiles=0;
        int nPersistents=0;
        int nPretendVolatiles=0;
        int nPretendPersistents=0;
        int nInts=0;
        int nFloats=0;
        int nUsablePersistents=0;
        int nUsableVolatiles=0;
        int nUsableInts=0;
        int nUsableFloats=0;
        int nIntVolatiles=0;
        int nIntPersistents=0;
        int nFloatVolatiles=0;
        int nFloatPersistents=0;
        int nFloatPretendVolatiles=0;
        int nFloatPretendPersistents=0;
        for (Reg r : table) {
            if (r.allowed()) {
                if (r.isVolatile()) {
                    nVolatiles++;
                } else {
                    nPersistents++;
                }
                if (r.isPretendVolatile()) {
                    nPretendVolatiles++;
                } else {
                    nPretendPersistents++;
                }
                if (r.isInt()) {
                    nInts++;
                }
                if (r.isFloat()) {
                    nFloats++;
                    if (r.isUsablePretendPersistent()) {
                        nFloatPretendPersistents++;
                    }
                    if (r.isUsablePretendVolatile()) {
                        nFloatPretendVolatiles++;
                    }
                }
                if (r.isUsable()) {
                    if (r.isInt()) {
                        nUsableInts++;
                    }
                    if (r.isFloat()) {
                        nUsableFloats++;
                    }
                }
                if (r.isUsablePersistent()) {
                    nUsablePersistents++;
                    if (r.isInt()) {
                        nIntPersistents++;
                    }
                    if (r.isFloat()) {
                        nFloatPersistents++;
                    }
                }
                if (r.isUsableVolatile()) {
                    nUsableVolatiles++;
                    if (r.isInt()) {
                        nIntVolatiles++;
                    }
                    if (r.isFloat()) {
                        nFloatVolatiles++;
                    }
                }
            }
        }
        
        volatiles=new Reg[nVolatiles];
        persistents=new Reg[nPersistents];
        pretendVolatiles=new Reg[nPretendVolatiles];
        pretendPersistents=new Reg[nPretendPersistents];
        ints=new Reg[nInts];
        floats=new Reg[nFloats];
        usableVolatiles=new Reg[nUsableVolatiles];
        usablePersistents=new Reg[nUsablePersistents];
        usableInts=new Reg[nUsableInts];
        usableFloats=new Reg[nUsableFloats];
        usableIntVolatiles=new Reg[nIntVolatiles];
        usableFloatVolatiles=new Reg[nFloatVolatiles];
        usableIntPersistents=new Reg[nIntPersistents];
        usableFloatPersistents=new Reg[nFloatPersistents];
        usableFloatPretendVolatiles=new Reg[nFloatPretendVolatiles];
        usableFloatPretendPersistents=new Reg[nFloatPretendPersistents];
        
        int vCnt=0;
        int pCnt=0;
        int pvCnt=0;
        int ppCnt=0;
        int iCnt=0;
        int fCnt=0;
        int uvCnt=0;
        int upCnt=0;
        int uiCnt=0;
        int ufCnt=0;
        int ivCnt=0;
        int ipCnt=0;
        int fvCnt=0;
        int fpCnt=0;
        int fpvCnt=0;
        int fppCnt=0;
        
        for (Reg r : table) {
            if (r.allowed()) {
                if (r.isVolatile()) {
                    volatiles[vCnt++]=r;
                } else {
                    persistents[pCnt++]=r;
                }
                if (r.isPretendVolatile()) {
                    pretendVolatiles[pvCnt++]=r;
                } else {
                    pretendPersistents[ppCnt++]=r;
                }
                if (r.isInt()) {
                    ints[iCnt++]=r;
                }
                if (r.isFloat()) {
                    floats[fCnt++]=r;
                    if (r.isUsablePretendVolatile()) {
                        usableFloatPretendVolatiles[fpvCnt++]=r;
                    } else {
                        usableFloatPretendPersistents[fppCnt++]=r;
                    }
                }
                if (r.isUsable()) {
                    if (r.isInt()) {
                        usableInts[uiCnt++]=r;
                    }
                    if (r.isFloat()) {
                        usableFloats[ufCnt++]=r;
                    }
                }
                if (r.isUsableVolatile()) {
                    usableVolatiles[uvCnt++]=r;
                    if (r.isInt()) {
                        usableIntVolatiles[ivCnt++]=r;
                    }
                    if (r.isFloat()) {
                        usableFloatVolatiles[fvCnt++]=r;
                    }
                }
                if (r.isUsablePersistent()) {
                    usablePersistents[upCnt++]=r;
                    if (r.isInt()) {
                        usableIntPersistents[ipCnt++]=r;
                    }
                    if (r.isFloat()) {
                        usableFloatPersistents[fpCnt++]=r;
                    }
                }
            }
        }
        
        assert vCnt==volatiles.length;
        assert pCnt==persistents.length;
        assert pvCnt==pretendVolatiles.length;
        assert ppCnt==pretendPersistents.length;
        assert iCnt==ints.length;
        assert fCnt==floats.length;
        assert uvCnt==usableVolatiles.length;
        assert upCnt==usablePersistents.length;
        assert uiCnt==usableInts.length;
        assert ufCnt==usableFloats.length;
        assert ivCnt==usableIntVolatiles.length;
        assert ipCnt==usableIntPersistents.length;
        assert fvCnt==usableFloatVolatiles.length;
        assert fpCnt==usableFloatPersistents.length;
        assert fpvCnt==usableFloatPretendVolatiles.length;
        assert fppCnt==usableFloatPretendPersistents.length;
        
        if (Global.pointerSize==4) {
            returnRegs=new Reg[]{Reg.AX,Reg.DX};
        } else {
            returnRegs=new Reg[]{Reg.AX,Reg.XMM0};
        }
    }
    
    public static Reg[] all(Kind kind) {
        switch (kind) {
        case INT: return ints;
        case FLOAT: return floats;
        default: throw new Error();
        }
    }
    
    public static Reg[] usables(Kind kind) {
        switch (kind) {
        case INT: return usableInts;
        case FLOAT: return usableFloats;
        default: throw new Error();
        }
    }
    
    public static Reg[] usableVolatiles(Kind kind) {
        switch (kind) {
        case INT: return usableIntVolatiles;
        case FLOAT: return usableFloatVolatiles;
        default: throw new Error();
        }
    }
    
    public static Reg[] usablePretendVolatiles(Kind kind) {
        switch (kind) {
        case INT: return usableIntVolatiles;
        case FLOAT: return usableFloatPretendVolatiles;
        default: throw new Error();
        }
    }
    
    public static Reg[] usablePersistents(Kind kind) {
        switch (kind) {
        case INT: return usableIntPersistents;
        case FLOAT: return usableFloatPersistents;
        default: throw new Error();
        }
    }
    
    public static Reg[] usablePretendPersistents(Kind kind) {
        switch (kind) {
        case INT: return usableIntPersistents;
        case FLOAT: return usableFloatPretendPersistents;
        default: throw new Error();
        }
    }
    
    public String toString() {
        return "%"+name();
    }
    
    public String asm(LType type) {
        switch (this) {
        case AX:
            switch (type) {
            case Byte: return "al";
            case Half: return "ax";
            case Long: return "eax";
            case Quad: return "rax";
            default: break;
            }
            break;
        case CX:
            switch (type) {
            case Byte: return "cl";
            case Half: return "cx";
            case Long: return "ecx";
            case Quad: return "rcx";
            default: break;
            }
            break;
        case DX:
            switch (type) {
            case Byte: return "dl";
            case Half: return "dx";
            case Long: return "edx";
            case Quad: return "rdx";
            default: break;
            }
            break;
        case BX:
            switch (type) {
            case Byte: return "bl";
            case Half: return "bx";
            case Long: return "ebx";
            case Quad: return "rbx";
            default: break;
            }
            break;
        case SP:
            switch (type) {
            case Half: return "sp";
            case Long: return "esp";
            case Quad: return "rsp";
            default: break;
            }
            break;
        case BP:
            switch (type) {
            case Half: return "bp";
            case Long: return "ebp";
            case Quad: return "rbp";
            default: break;
            }
            break;
        case SI:
            switch (type) {
            case Half: return "si";
            case Long: return "esi";
            case Quad: return "rsi";
            default: break;
            }
            break;
        case DI:
            switch (type) {
            case Half: return "di";
            case Long: return "edi";
            case Quad: return "rdi";
            default: break;
            }
            break;
        case R8:
            switch (type) {
            case Half: return "r8w";
            case Long: return "r8d";
            case Quad: return "r8";
            default: break;
            }
            break;
        case R9:
            switch (type) {
            case Half: return "r9w";
            case Long: return "r9d";
            case Quad: return "r9";
            default: break;
            }
            break;
        case R10:
            switch (type) {
            case Half: return "r10w";
            case Long: return "r10d";
            case Quad: return "r10";
            default: break;
            }
            break;
        case R11:
            switch (type) {
            case Half: return "r11w";
            case Long: return "r11d";
            case Quad: return "r11";
            default: break;
            }
            break;
        case R12:
            switch (type) {
            case Half: return "r12w";
            case Long: return "r12d";
            case Quad: return "r12";
            default: break;
            }
            break;
        case R13:
            switch (type) {
            case Half: return "r13w";
            case Long: return "r13d";
            case Quad: return "r13";
            default: break;
            }
            break;
        case R14:
            switch (type) {
            case Half: return "r14w";
            case Long: return "r14d";
            case Quad: return "r14";
            default: break;
            }
            break;
        case R15:
            switch (type) {
            case Half: return "r15w";
            case Long: return "r15d";
            case Quad: return "r15";
            default: break;
            }
            break;
        case XMM0:
            if (type.isFloat()) {
                return "xmm0";
            }
            break;
        case XMM1:
            if (type.isFloat()) {
                return "xmm1";
            }
            break;
        case XMM2:
            if (type.isFloat()) {
                return "xmm2";
            }
            break;
        case XMM3:
            if (type.isFloat()) {
                return "xmm3";
            }
            break;
        case XMM4:
            if (type.isFloat()) {
                return "xmm4";
            }
            break;
        case XMM5:
            if (type.isFloat()) {
                return "xmm5";
            }
            break;
        case XMM6:
            if (type.isFloat()) {
                return "xmm6";
            }
            break;
        case XMM7:
            if (type.isFloat()) {
                return "xmm7";
            }
            break;
        case XMM8:
            if (type.isFloat()) {
                return "xmm8";
            }
            break;
        case XMM9:
            if (type.isFloat()) {
                return "xmm9";
            }
            break;
        case XMM10:
            if (type.isFloat()) {
                return "xmm10";
            }
            break;
        case XMM11:
            if (type.isFloat()) {
                return "xmm11";
            }
            break;
        case XMM12:
            if (type.isFloat()) {
                return "xmm12";
            }
            break;
        case XMM13:
            if (type.isFloat()) {
                return "xmm13";
            }
            break;
        case XMM14:
            if (type.isFloat()) {
                return "xmm14";
            }
            break;
        case XMM15:
            if (type.isFloat()) {
                return "xmm15";
            }
            break;
        default:
            break;
        }
        // this will be a pain in the arse
        throw new CompilerException("cannot use register "+this+" with type "+type);
    }
    
    public String asm(LType useType,LType memType) {
        return "%"+asm(useType);
    }
    
    public String callAsm(LType useType,LType memType) {
        return "*%"+asm(useType);
    }

    public LArg copy() {
        return this;
    }
    
    public boolean equals(int value) {
        return false;
    }
    
    public boolean doesNotEqual(int value) {
        return false;
    }
    
    public int nUseOnDefVars() {
        return 0;
    }
    public LArg useOnDefVar(int i) {
        return null;
    }
    
    public int nUseOnUseVars() {
        return 1;
    }
    public LArg useOnUseVar(int i) {
        return this;
    }
    
    public LArg offset(int offset) {
        throw new CompilerException("Cannot offset a register");
    }
    
    public void spill(HashMap< Tmp, LArg > spills,LOp op,int i) {
    }

    public LArg map(HashMap< ?, ? extends LArg > map) {
        LArg rep=map.get(this);
        if (rep==null) {
            return this;
        } else {
            return rep;
        }
    }
    
    public Linkable linkable() {
        return null;
    }
    
    public boolean stackPointer() {
        switch (this) {
        case SP: return true;
        case BP: return !Settings.OMIT_FRAME_POINTER;
        default: return false;
        }
    }
    
    public boolean stack() {
        return false;
    }
    
    public static final HashSet< Reg > EMPTY_SET=new HashSet< Reg >();
}

