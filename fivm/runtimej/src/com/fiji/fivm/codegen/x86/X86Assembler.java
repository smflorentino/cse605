/*
 * X86Assembler.java
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

package com.fiji.fivm.codegen.x86;

import java.nio.*;

import com.fiji.fivm.codegen.*;
import com.fiji.fivm.r1.*;
import com.fiji.fivm.*;

import static com.fiji.fivm.r1.fivmRuntime.*;

import static com.fiji.fivm.codegen.x86.X86Constants.*;

// Taken from Ovm, which is BSD licensed (FIXME: add BSD license language to Fiji license)

/**
 * @author Hiroshi Yamauchi
 **/
public final class X86Assembler
    extends Assembler {



    public X86Assembler(ByteBuffer cb) {
	super(cb);
        cb.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void setBranchSourceAndJcc(Branch b, byte condition) { // backward branch
        jcc(condition, (b.targetPC.sub(getAbsolutePC())).castToInt());
    }
    public Branch setBranchSourceAndJcc(byte condition) { // forward branch
	jcc_unlinked(condition);
	return new Branch(getPC(), Pointer.zero().sub(1));
    }
    public Branch setBranchSourceAndJccShort(byte condition) { // forward branch
	jcc(condition, 0); // short
	return new Branch(getPC(), 0, 8);
    }
    public void setBranchSourceAndJmp(Branch b) { // backward branch
        jmp((b.targetPC.sub(getAbsolutePC())).castToInt());
    }
    public Branch setBranchSourceAndJmp() { // forward branch
	jmp_unlinked();
	return new Branch(getPC(), Pointer.zero().sub(1));
    }
    public Branch setBranchSourceAndJmpShort() { // forward branch
	jmp(0); // short
	return new Branch(getPC(), 0, 8);
    }
    
    public void setBranchTarget(Branch b) { // forward branch
	assert(b.targetPC == Pointer.zero().sub(1));
        if (b.addrAddr!=Pointer.zero()) {
            b.addrAddr.store(getAbsolutePC());
        } else if (b.bits==32) {
            int offset = getPC() - b.sourcePC;
            cb.putInt(b.sourcePC - 4, offset);
        } else if (b.bits==8) {
            int offset = getPC() - b.sourcePC;
            if (offset>127) {
                throw new fivmError("Short branch overflow: offset = "+offset);
            }
            cb.put(b.sourcePC-1, (byte)offset);
        } else {
            throw new fivmError("don't know how to set branch target: "+b);
        }
    }
    public Branch setBranchTarget() { // backward branch
	return new Branch(-1, getAbsolutePC());
    }


    // mod = {MOD_R, MOD_M_32, MOD_M_8, MOD_M}
    private byte modRM(int mod, int regOp, int rm) {
	return (byte)(mod | (regOp << 3) | rm);
    }
    
    // scale = {SS_I_1, SS_I_2, SS_I_4, SS_I_8}
    private byte SIB(int scale, int index, int base) {
	return (byte)(scale | (index << 3) | base);
    }
    
    private int mop(int opcode,
                    int regOp,
                    byte reg,
                    int offset) {
	cb.put((byte)opcode);
        if (offset==0 && reg!=R_BP) {
            cb.put(modRM(MOD_M, (byte)regOp, reg));
            if (reg==R_SP) {
                cb.put((byte)0x24);
                return 3;
            } else {
                return 2;
            }
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, (byte)regOp, reg));
            if (reg==R_SP) {
                cb.put((byte)0x24);
            }
            cb.put((byte)offset);
            return 3+(reg==R_SP?1:0);
        } else {
            cb.put(modRM(MOD_M_32, (byte)regOp, reg));
            if (reg==R_SP) {
                cb.put((byte)0x24);
            }
            cb.putInt(offset);
            return 6+(reg==R_SP?1:0);
        }
    }

    public int breakpoint() {
	cb.put((byte)0xcc);
	return 1;
    }
    
    public int breakpoints(int n) {
        int result=0;
        for (int i=0;i<n;++i) {
            result+=breakpoint();
        }
        return result;
    }
    
    public int breakpointAlign(int multiplier) {
        int result=0;
        while (getAbsolutePC().mod(multiplier)!=Pointer.zero()) {
            result+=breakpoint();
        }
        return result;
    }

    public int cdq() {
	cb.put((byte)0x99);
	return 1;
    }
    
    public int cwde() {
        cb.put((byte)0x98);
        return 1;
    }

    public int cmpxchgRM(boolean LOCKprefix, 
			 byte src, 
			 byte dst,
			 int offset) {
	int locklen = 0;
	if (LOCKprefix) {
	    locklen = 1;
	    cb.put((byte)0xF0);
	}
	cb.put((byte)0x0F);
	cb.put((byte)0xB1);
	if (offset == 0) {
	    cb.put(modRM(MOD_M, src, dst));
	    return locklen + 3;
	} else if (-128 <= offset && offset <= 127) {
	    cb.put(modRM(MOD_M_8, src, dst));
	    cb.put((byte)offset);
	    return locklen + 4;
	} else {
	    cb.put(modRM(MOD_M_32, src, dst));
	    cb.putInt(offset);
	    return locklen + 7;
	}
    }

    public int sete(byte dst) {
	cb.put((byte)0x0f);
	cb.put((byte)0x94);
	cb.put(modRM(MOD_R, 0, dst));
	return 3;
    }
	
    public int addRR(byte src, byte dst) {
	cb.put((byte)0x03);
	cb.put(modRM(MOD_R, dst, src));
	return 2;
    }

    public int l_addRR() {
	return 2;
    }

    public int adcRR(byte src, byte dst) {
	cb.put((byte)0x13);
	cb.put(modRM(MOD_R, dst, src));
	return 2;
    }

    public int addRM(byte src, byte dst, int offset) {
        return mop(0x01,src,dst,offset);
    }

    public int adcRM(byte src, byte dst, int offset) {
        return mop(0x11,src,dst,offset);
    }

    public int addIR(int immediate, byte dst) {
        if (immediate==0) {
            return 0;
        } else if (immediate>=-128 && immediate<=127) {
            cb.put((byte)0x83);
            cb.put(modRM(MOD_R, 0, dst));
            cb.put((byte)immediate);
            return 3;
        } else {
            cb.put((byte)0x81);
            cb.put(modRM(MOD_R, 0, dst));
            cb.putInt(immediate);
            return 6;
        }
    }

    public int l_addIR() {
	return 6;
    }
    
    // add offset(srcreg, indexreg, 4) to destreg
    public int addMRI4(byte src_reg, 
		       byte index_reg, 
		       int offset, 
		       byte dest_reg) {
	cb.put((byte)0x03);
	cb.put(modRM(MOD_M_32, dest_reg, 4));
	cb.put(SIB(SS_I_4, index_reg, src_reg));
	cb.putInt(offset);
	return 7;
    }

    public int l_addMRI4() {
	return 7;
    }

    public int leaMRI4(byte src_reg,
		       byte index_reg,
		       int offset,
		       byte dest_reg) {
	cb.put((byte)0x8D);
	cb.put(modRM(MOD_M_32, dest_reg, 4));
	cb.put(SIB(SS_I_4, index_reg, src_reg));
	cb.putInt(offset);
	return 7;
    }

    public int l_leaMRI4() {
	return 7;
    }

    public int addIM(int immediate, byte dst, int offset) {
        boolean shortImm=(immediate>=-128 && immediate<=127);
        int result=mop(shortImm?0x83:0x81,0,dst,offset);
        if (shortImm) {
            cb.put((byte)immediate);
            return result+1;
        } else {
            cb.putInt(immediate);
            return result+4;
        }
    }

    public int subIR(int immediate, byte dst) {
        if (immediate>=-128 && immediate<=127) {
            cb.put((byte)0x83);
            cb.put((byte)(0xE8 + dst));
            cb.put((byte)immediate);
            return 3;
        } else {
            cb.put((byte)0x81);
            cb.put((byte)(0xE8 + dst));
            cb.putInt(immediate);
            return 6;
        }
    }	

    // sub mem from reg
    public int subMR(byte src, int soffset, byte dst) {
        return mop(0x2b,dst,src,soffset);
    }

    public int sbbMR(byte src, int soffset, byte dst) {
        return mop(0x1b,dst,src,soffset);
    }

    public int subRM(byte src, byte dst, int doffset) {
        return mop(0x29,src,dst,doffset);
    }

    public int subRR(byte src, byte dst) {
	cb.put((byte)0x29);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int sbbRM(byte src, byte dst, int doffset) {
        return mop(0x19,src,dst,doffset);
    }

    public int sbbRR(byte src, byte dst) {
	cb.put((byte)0x19);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int mulRR(byte src, byte dst) {
	cb.put((byte)0x0F);
	cb.put((byte)0xAF);
	cb.put(modRM(MOD_R, dst, src));
	return 3;
    }

    public int l_mulRR() {
	return 3;
    }

    // unsigned mul R_EAX * mem - > R_EDX:R_EAX
    public int umulM(byte src, int offset) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_M_32, 4, src));
	cb.putInt(offset);
	return 6;
    }

    // unsigned mul R_EAX * reg -> R_EDX:R_EAX
    public int umulR(byte src) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_R, 4, src));
	return 2;
    }
    
    // div R_EDX:R_EAX / mem -> R_EAX
    //     R_EDX:R_EAX % mem -> R_EDX
    public int divM(byte src, int offset) {
        return mop(0xf7,7,src,offset);
    }

    // div R_EDX:R_EAX / reg -> R_EAX
    //     R_EDX:R_EAX % reg -> R_EDX
    public int divR(byte src) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_R, 7, src));
	return 2;
    }
    
    public int negR(byte dst) {
        cb.put((byte)0xF7);
        cb.put(modRM(MOD_R, 3, dst));
        return 2;
    }

    public int negM(byte dst,int offset) {
        return mop(0xf7,3,dst,offset);
    }

    public int pushR(byte src) {
	cb.put((byte)(0x50 | src));
	return 1;
    }

    public int l_pushR() {
	return 1;
    }

    public int popR(byte dst) {
	cb.put((byte)(0x58 | dst));
	return 1;
    }

    public int l_popR() {
	return 1;
    }

    public int pushM(byte src, int offset) {
        return mop(0xff,6,src,offset);
    }
    
    public int pushMS(byte src,int offset,byte index,int scale) {
        if (offset==0 && src!=R_BP) {
            cb.put((byte)0xFF);
            cb.put(modRM(MOD_M, 6, R_SP));
            cb.put(SIB(scale, index, src));
            return 3;
        } else if (offset>=-128 && offset<=127) {
            cb.put((byte)0xFF);
            cb.put(modRM(MOD_M_8, 6, R_SP));
            cb.put(SIB(scale, index, src));
            cb.put((byte)offset);
            return 4;
        } else {
            cb.put((byte)0xFF);
            cb.put(modRM(MOD_M_32, 6, R_SP));
            cb.put(SIB(scale, index, src));
            cb.putInt(offset);
            return 7;
        }
    }

    public int pushMS(byte src,byte index,int scale) {
        return pushMS(src,0,index,scale);
    }

    public int pushM_wide(byte src, int offset) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_M_32, 6, src));
	cb.putInt(offset);
	return 6;
    }

    /**
     * @return the pc right after this instruction
     */
    public int pushM_to_be_patched(byte src) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_M_32, 6, src));
	cb.putInt(-1);
	return getPC();
    }

    public int l_pushM(int offset) {
	if (-128 <= offset && offset <= 127) {
	    return 3;
	} else {
	    return 6;
	}
    }
	
    // push [base_reg + 4 * index_reg]
    public int pushMS4(byte base_reg,
		       byte index_reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_M, 6, 4));
	cb.put(SIB(SS_I_4, index_reg, base_reg));
	return 3;
    }

    // push [base_reg + 8 * index_reg]
    public int pushMS8(byte base_reg,
		       byte index_reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_M, 6, 4));
	cb.put(SIB(SS_I_8, index_reg, base_reg));
	return 3;
    }

    public int l_pushMS8() {
	return 3;
    }

    public int popM(byte dst, int offset) {
        return mop(0x8f,0,dst,offset);
    }
    
    public int popMS(byte src, int offset, byte index, int scale) {
        if (offset==0) {
            cb.put((byte)0x8F);
            cb.put(modRM(MOD_M, 0, R_SP));
            cb.put(SIB(scale, index, src));
            return 3;
        } else if (offset>=-128 && offset<=127) {
            cb.put((byte)0x8F);
            cb.put(modRM(MOD_M_8, 0, R_SP));
            cb.put(SIB(scale, index, src));
            cb.put((byte)offset);
            return 4;
        } else {
            cb.put((byte)0x8F);
            cb.put(modRM(MOD_M_32, 0, R_SP));
            cb.put(SIB(scale, index, src));
            cb.putInt(offset);
            return 4;
        }
    }

    public void popMS(byte src, byte index, int scale) {
        cb.put((byte)0x8F);
        cb.put(modRM(MOD_M, 0, R_SP));
        cb.put(SIB(scale, index, src));
    }

    public int pushI32(int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    cb.put((byte)0x6A);
	    cb.put((byte)immediate);
	    return 2;
	} else {
	    cb.put((byte)0x68);
	    cb.putInt(immediate);
	    return 5;
	}
    }

    /**
     *  A version of pushI32 that always uses a 32-bit wide immediate
     *  value.
     **/
    public int pushI32_wide(int immediate) {
	cb.put((byte)0x68);
	cb.putInt(immediate);
	return 5;
    }	

    public int l_pushI32_wide() {
	return 5;
    }

    /**
     * @return the pc right after this instruction
     */
    public int pushI32_to_be_patched() {
	cb.put((byte)0x68);
	cb.putInt(-1);
	return getPC();
    }	

    /**
     * @return the offset of the immediate value from the beginning of
     * the instruction pushI32
     **/
    public int imm_offset_pushI32_wide() {
	return 1;
    }

    public int l_pushI32(int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    return 2;
	} else {
	    return 5;
	}
    }	

    public int pushI64(long immediate) {
	cb.put((byte)0x68);
	cb.putInt((int)((immediate >> 32) & 0xFFFFFFFF));
	cb.put((byte)0x68);
	cb.putInt((int)(immediate & 0xFFFFFFFF));
	return 10;
    }

    public int l_pushI64() {
	return 10;
    }
    
    public int leaMSR(byte src, int offset, byte index, int scale, int dest) {
        if (offset==0) {
            cb.put((byte)0x8d);
            cb.put(modRM(MOD_M, dest, 4));
            cb.put(SIB(scale, index, src));
            return 3;
        } else if (offset>=-128 && offset<=127) {
            cb.put((byte)0x8d);
            cb.put(modRM(MOD_M_8, dest, 4));
            cb.put(SIB(scale, index, src));
            cb.put((byte)offset);
            return 4;
        } else {
            cb.put((byte)0x8d);
            cb.put(modRM(MOD_M_32, dest, 4));
            cb.put(SIB(scale, index, src));
            cb.putInt(offset);
            return 7;
        }
    }
    
    public int leaMSR(byte src, byte index, int scale, int dest) {
        return leaMSR(src,0,index,scale,dest);
    }

    public int leaMR(byte src, int offset, byte dst) {
        return mop(0x8d,dst,src,offset);
    }

    public int movMR(byte src, int offset, byte dst) {
        return mop(0x8b,dst,src,offset);
    }

    public int movMR_wide(byte src, int offset, byte dst) {
	assert(src != R_SP);
	cb.put((byte)0x8B);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(offset);
	return 6;
    }

    /**
     * @return the pc right after this instruction
     */
    public int movMR_to_be_patched(byte src, byte dst) {
	assert(src != R_SP);
	cb.put((byte)0x8B);
	cb.put(modRM(MOD_M_32, dst, src));
	cb.putInt(-1);
	return getPC();
    }

    public int l_movMR(int offset) {
	if (offset == 0) {
	    return 2;
	} else if (-128 <= offset && offset <= 127) {
	    return 3;
	} else {
	    return 6;
	}
    }

    public int movMSR(byte src_reg,
                      int offset,
                      byte index_reg,
                      int scale,
                      byte dest_reg) {
        if (offset==0) {
            cb.put((byte)0x8b);
            cb.put(modRM(MOD_M, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            return 3;
        } else if (offset>=-128 && offset<=127) {
            cb.put((byte)0x8b);
            cb.put(modRM(MOD_M_8, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            cb.put((byte)offset);
            return 4;
        } else {
            cb.put((byte)0x8b);
            cb.put(modRM(MOD_M_32, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            cb.putInt(offset);
            return 7;
        }
    }
    
    public int movMSR(byte src_reg,
                      byte index_reg,
                      int scale,
                      byte dest_reg) {
        return movMSR(src_reg,0,index_reg,scale,dest_reg);
    }
    
    // mov offset(srcreg, indexreg, 4) -> destreg
    public int movMRI4(byte src_reg, 
		       byte index_reg, 
		       int offset, 
		       byte dest_reg) {
	cb.put((byte)0x8B);
	cb.put(modRM(MOD_M_32, dest_reg, 4));
	cb.put(SIB(SS_I_4, index_reg, src_reg));
	cb.putInt(offset);
	return 7;
    }
    
    public int l_movMRI4() {
	return 7;
    }

    public int movRM(byte src, byte dst, int offset) {
        return mop(0x89,src,dst,offset);
    }

    public int movRM_wide(byte src, byte dst, int offset) {
	cb.put((byte)0x89);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(offset);
	return 6;
    }

    /**
     * @return the pc right after this instruction
     */
    public int movRM_to_be_patched(byte src, byte dst) {
	cb.put((byte)0x89);
	cb.put(modRM(MOD_M_32, src, dst));
	cb.putInt(-1);
	return getPC();
    }

    public int l_movRM_wide() {
	return 6;
    }

    public int movRM(byte src, byte dst) {
        return mop(0x89,src,dst,0);
    }

    /*
    public int movIR(VM_Address value, byte dst) {
	cb.put((byte)(0xB8 + dst));
	cb.putInt(value.asInt());
	return 5;
   }
    */

    public int movIR(int value, byte dst) {
	/*
	if (value == 0)
	    return xorRR(dst, dst);
	else {
	*/
	    cb.put((byte)(0xB8 + dst));
	    cb.putInt(value);
	    return 5;
    //}
    }

    public int l_movIR() {
	return 5;
    }

    /**
     * @return the pc right after this instruction
     */
    public int movIR_to_be_patched(byte dst) {
	cb.put((byte)(0xB8 + dst));
	cb.putInt(-1);
	return getPC();
    }	

    public int movIM(int value, byte dst, int offset) {
        int result=mop(0xc7,0,dst,offset);
        cb.putInt(value);
        return result+4;
    }

    public int movIM_wide(int value, byte dst, int offset) {
	cb.put((byte)0xC7);
	cb.put(modRM(MOD_M_32, 0, dst));
	cb.putInt(offset);
	cb.putInt(value);
	return 10;
    }

    public int l_movIM_wide() {
	return 10;
    }

    // Return in which offset, from the start of the instruction, the
    // immediate value is written in the instruction movIM
    public int movIM_Ioffset(int offset) {
	if (-128 <= offset && offset <= 127) {
	    return 3;
	} else {
	    return 6;
	}
    }

    public int movI8M8(byte imm, byte dst_reg, byte offset) {
	assert(dst_reg != R_SP);
	cb.put((byte)0xC6);
	cb.put(modRM(MOD_M_8, 0, dst_reg));
	cb.put(offset);
	cb.put(imm);
	return 4;
    }

    /*
    public int movIM(int value, int address) {
	cb.put((byte)0xC7);
	cb.put((byte)0x24);
	cb.put((byte)0x25);
	cb.putInt(address);
	cb.putInt(value);
	return 11;
    }
    */
    
    public int movzxR8R(byte src,
                        byte dest) {
	cb.put((byte)0x0F);
        cb.put((byte)0xB6);
        cb.put(modRM(MOD_R, dest, src));
        return 3;
    }
    
    public int movsxR8R(byte src,
                        byte dest) {
	cb.put((byte)0x0F);
        cb.put((byte)0xBE);
        cb.put(modRM(MOD_R, dest, src));
        return 3;
    }
    
    public int movsxMS8R(byte src_reg,
                         int offset,
                         byte index_reg,
                         int scale,
                         byte dest_reg) {
	cb.put((byte)0x0F);
	cb.put((byte)0xBE);
        if (offset==0 && src_reg!=R_BP) {
            cb.put(modRM(MOD_M, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            return 4;
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            cb.put((byte)offset);
            return 5;
        } else {
            cb.put(modRM(MOD_M_32, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            cb.putInt(offset);
            return 8;
        }
    }

    public int movsxMS8R(byte src_reg,
                         byte index_reg,
                         int scale,
                         byte dest_reg) {
        return movsxMS8R(src_reg,0,index_reg,scale,dest_reg);
    }

    // Move a sign-extended byte in memory to a register
    public int movsxM8R(byte src, int offset, byte dst) {
	cb.put((byte)0x0F);
        return mop(0xbe,dst,src,offset)+1;
    }

    public int movsxMS16R(byte src_reg,
                          int offset,
                          byte index_reg,
                          int scale,
                          byte dest_reg) {
	cb.put((byte)0x0F);
	cb.put((byte)0xBF);
        if (offset==0 && src_reg!=R_BP) {
            cb.put(modRM(MOD_M, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            return 4;
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            cb.put((byte)offset);
            return 5;
        } else {
            cb.put(modRM(MOD_M_32, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            cb.putInt(offset);
            return 8;
        }
    }
    
    public int movsxMS16R(byte src_reg,
                          byte index_reg,
                          int scale,
                          byte dest_reg) {
        return movsxMS16R(src_reg,0,index_reg,scale,dest_reg);
    }

    // Move a sign-extended short in memory to a register
    public int movsxM16R(byte src, int offset, byte dst) {
	cb.put((byte)0x0F);
        return mop(0xbf,dst,src,offset)+1;
    }
    
    public int movzxR16R(byte src,
                         byte dst) {
	cb.put((byte)0x0F);
	cb.put((byte)0xB7);
        cb.put(modRM(MOD_R, dst, src));
        return 3;
    }

    public int movzxMS16R(byte src_reg,
                          int offset,
                          byte index_reg,
                          int scale,
                          byte dest_reg) {
	cb.put((byte)0x0F);
	cb.put((byte)0xB7);
        if (offset==0 && src_reg!=R_BP) {
            cb.put(modRM(MOD_M, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            return 4;
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            cb.put((byte)offset);
            return 5;
        } else {
            cb.put(modRM(MOD_M_32, dest_reg, 4));
            cb.put(SIB(scale, index_reg, src_reg));
            cb.putInt(offset);
            return 8;
        }
    }
    
    public int movzxMS16R(byte src_reg,
                          byte index_reg,
                          int scale,
                          byte dest_reg) {
        return movzxMS16R(src_reg,0,index_reg,scale,dest_reg);
    }

    // Move a zero-extended short in memory to a register
    public int movzxM16R(byte src, int offset, byte dst) {
	cb.put((byte)0x0F);
        return mop(0xb7,dst,src,offset)+1;
    }

    public int movRR(byte src, byte dst) {
	cb.put((byte)0x89);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    /**
     * Return the size of the macro movRR
     **/
    public int l_movRR() {
	return 2;
    }

    public int movRMS(byte src,byte dst,int offset,byte index,int scale) {
        cb.put((byte)0x89);
        if (offset==0) {
            cb.put(modRM(MOD_M, src, 4));
            cb.put(SIB(scale, index, dst));
            return 3;
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, src, 4));
            cb.put(SIB(scale, index, dst));
            cb.put((byte)offset);
            return 4;
        } else {
            cb.put(modRM(MOD_M_32, src, 4));
            cb.put(SIB(scale, index, dst));
            cb.putInt(offset);
            return 7;
        }
    }
    
    public int movRMS(byte src,byte dst,byte index,int scale) {
        return movRMS(src,dst,0,index,scale);
    }

    public int movR8M(byte src, byte dst, int offset) {
	assert(src == R_AX
	       || src == R_CX
	       || src == R_DX
	       || src == R_BX);
        return mop(0x88,src,dst,offset);
    }
    
    public int movR8MS(byte src,byte dst,int offset,byte index,int scale) {
        cb.put((byte)0x88);
        if (offset==0) {
            cb.put(modRM(MOD_M, src, 4));
            cb.put(SIB(scale, index, dst));
            return 3;
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, src, 4));
            cb.put(SIB(scale, index, dst));
            cb.put((byte)offset);
            return 4;
        } else {
            cb.put(modRM(MOD_M_32, src, 4));
            cb.put(SIB(scale, index, dst));
            cb.putInt(offset);
            return 7;
        }
    }

    public int movR8MS(byte src,byte dst,byte index,int scale) {
        return movR8MS(src,dst,0,index,scale);
    }

    public int movR16M(byte src, byte dst, int offset) {
	assert(src == R_AX
	       || src == R_CX
	       || src == R_DX
	       || src == R_BX);
	cb.put((byte)0x66);
        return mop(0x89,src,dst,offset);
    }

    public int movR16MS(byte src,byte dst,int offset,byte index,int scale) {
        cb.put((byte)0x66);
        cb.put((byte)0x89);
        if (offset==0) {
            cb.put(modRM(MOD_M, src, 4));
            cb.put(SIB(scale, index, dst));
            return 4;
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, src, 4));
            cb.put(SIB(scale, index, dst));
            cb.put((byte)offset);
            return 5;
        } else {
            cb.put(modRM(MOD_M_32, src, 4));
            cb.put(SIB(scale, index, dst));
            cb.putInt(offset);
            return 8;
        }
    }

    public int movR16MS(byte src,byte dst,byte index,int scale) {
        return movR16MS(src,dst,0,index,scale);
    }

    // reg = (reg << width)
    public int shlRI(byte reg, byte width) {
        switch (width) {
        case 0: return 0;
        case 1: return addRR(reg,reg);
        default:
            cb.put((byte)0xC1);
            cb.put(modRM(MOD_R, 4, reg));
            cb.put(width);
            return 3;
        }
    }

    // reg = (reg << CL)
    public int shlR(byte reg) {
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_R, 4, reg));
	return 2;
    }

    // tmem = (tmem << wmem)
    public int shlMM(byte target_reg, 
		     int toffset,
		     byte width_reg, 
		     int woffset) {
	int l1 = pushR(R_CX);
	int l2 = movMR(width_reg, woffset, R_CX);
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_M_32, 4, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_CX);
	return l1 + l2 + 6 + l3;
    }

    // tmem = (tmem << wmem) w/ shift-in from sireg
    // Note sireg != ECX
    public int shldMM(byte target_reg, 
		      int toffset,
		      byte width_reg, 
		      int woffset,
		      byte sireg) {
	assert(sireg != R_CX);
	int l1 = pushR(R_CX);
	int l2 = movMR(width_reg, woffset, R_CX);
	cb.put((byte)0x0F);
	cb.put((byte)0xA5);
	cb.put(modRM(MOD_M_32, sireg, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_CX);
	return l1 + l2 + 7 + l3;
    }

    // treg = (treg << ECX) w/ shift-in from sireg
    // Note sireg != ECX
    public int shldR(byte target_reg, 
		     byte sireg) {
	assert(sireg != R_CX);
	cb.put((byte)0x0F);
	cb.put((byte)0xA5);
	cb.put(modRM(MOD_R, sireg, target_reg));
	return 3;
    }

    // tmem = (tmem >>> wmem)
    public int shrMM(byte target_reg, 
		     int toffset,
		     byte width_reg,
		     int woffset) {
	int l1 = pushR(R_CX);
	int l2 = movMR(width_reg, woffset, R_CX);
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_M_32, 5, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_CX);
	return l1 + l2 + 6 + l3;
    }

    // reg = (reg >>> CL)
    public int shrR(byte reg) {
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_R, 5, reg));
	return 2;
    }
    
    public int shrRI(byte reg,byte width) {
        cb.put((byte)0xC1);
        cb.put(modRM(MOD_R, 5, reg));
        cb.put(width);
        return 3;
    }

    // treg = (treg >> ECX) w/ shift-in from sireg
    // Note sireg != ECX
    public int shrdR(byte target_reg, 
		     byte sireg) {
	assert(sireg != R_CX);
	cb.put((byte)0x0F);
	cb.put((byte)0xAD);
	cb.put(modRM(MOD_R, sireg, target_reg));
	return 3;
    }

    // tmem = (tmem >> wmem) w/ shift-in from sireg
    // Note sireg != ECX
    public int shrdMM(byte target_reg, 
		      int toffset,
		      byte width_reg, 
		      int woffset,
		      byte sireg) {
	assert(sireg != R_CX);
	int l1 = pushR(R_CX);
	int l2 = movMR(width_reg, woffset, R_CX);
	cb.put((byte)0x0F);
	cb.put((byte)0xAD);
	cb.put(modRM(MOD_M_32, sireg, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_CX);
	return l1 + l2 + 7 + l3;
    }

    // tmem = (tmem >> wmem)
    public int sarMM(byte target_reg, 
		     int toffset,
		     byte width_reg,
		     int woffset) {
	int l1 = pushR(R_CX);
	int l2 = movMR(width_reg, woffset, R_CX);
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_M_32, 7, target_reg));
	cb.putInt(toffset);
	int l3 = popR(R_CX);
	return l1 + l2 + 6 + l3;
    }

    // reg = (reg >> CL)
    public int sarR(byte reg) {
	cb.put((byte)0xD3);
	cb.put(modRM(MOD_R, 7, reg));
	return 2;
    }

    // reg = (reg >> width)
    public int sarRI(byte reg, 
		     byte width) {
	cb.put((byte)0xC1);
	cb.put(modRM(MOD_R, 7, reg));
	cb.put(width);
	return 3;
    }

    public int l_sarRI() {
	return 3;
    }

    // mem = (mem >> width)
    public int sarMI(byte target_reg, 
		     int toffset,
		     byte width) {
        int result=mop(0xc1,7,target_reg,toffset);
        cb.put(width);
        return result+1;
    }

    // test imm & mem
    public int testIM(int immediate,
		      byte target_reg,
		      int toffset) {
        int result=mop(0xf7,0,target_reg,toffset);
        cb.putInt(immediate);
        return result+4;
    }

    // test imm & reg
    public int testIR(int immediate,
		      byte reg) {
	cb.put((byte)0xF7);
	cb.put(modRM(MOD_R, 0, reg));
	cb.putInt(immediate);
	return 6;
    }
    
    // test reg & reg
    public int testRR(byte reg1,byte reg2) {
        cb.put((byte)0x85);
	cb.put(modRM(MOD_R, reg1, reg2));
        return 2;
    }

    public int andIR(int immediate,
		     byte reg) {
        if (immediate>=-128 && immediate<=127) {
            cb.put((byte)0x83);
            cb.put(modRM(MOD_R, 4, reg));
            cb.put((byte)immediate);
            return 6;
        } else {
            cb.put((byte)0x81);
            cb.put(modRM(MOD_R, 4, reg));
            cb.putInt(immediate);
            return 6;
        }
    }

    public int andRR(byte src,
		     byte dst) {
	cb.put((byte)0x21);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int andRM(byte src,
		     byte dst,
		     int doffset) {
        return mop(0x21,src,dst,doffset);
    }

    public int andMR(byte src,
		     int soffset,
		     byte dst) {
        return mop(0x23,dst,src,soffset);
    }

    public int orRM(byte src,
		    byte dst,
		    int doffset) {
        return mop(0x09,src,dst,doffset);
    }

    public int orRR(byte src,
		    byte dst) {
	cb.put((byte)0x09);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int orMR(byte src,
		    int soffset,
		    byte dst) {
        return mop(0x0b,dst,src,soffset);
    }

    public int xorRM(byte src,
		     byte dst,
		     int doffset) {
        return mop(0x31,src,dst,doffset);
    }

    public int xorRR(byte src,
		     byte dst) {
	cb.put((byte)0x31);
	cb.put(modRM(MOD_R, src, dst));
	return 2;
    }

    public int xorMR(byte src,
		     int soffset,
		     byte dst) {
        return mop(0x33,dst,src,soffset);
    }

    // converts an integer on memory into a double and pushes it onto FPU stack
    public int fildM32(byte reg, int offset) {
        return mop(0xdb,0,reg,offset);
    }

    // converts an integer on memory into a double and pushes it onto FPU stack
    public int fildM32(byte reg) {
        return fildM32(reg,0);
    }

    // converts a long on memory into a double and pushes it onto FPU stack
    public int fildM64(byte reg, int offset) {
        return mop(0xdf,5,reg,offset);
    }

    // converts a long on memory into a double and pushes it onto FPU stack
    public int fildM64(byte reg) {
        return fildM64(reg,0);
    }

    // converts a double on FPU stack into an integer and stores it on memory
    public int fistM32(byte reg, int offset) {
        return mop(0xdb,3,reg,offset);
    }

    // converts a double on FPU stack into an integer and stores it on memory
    public int fistM32(byte reg) {
        return fistM32(reg,0);
    }

    // converts a double on FPU stack into a long and stores it on memory
    public int fistM64(byte reg, int offset) {
        return mop(0xdf,7,reg,offset);
    }

    // converts a double on FPU stack into a long and stores it on memory
    public int fistM64(byte reg) {
	return fistM64(reg,0);
    }

    public int cmpRR(byte reg1, byte reg2) {
	cb.put((byte)0x39);
	cb.put(modRM(MOD_R, reg2, reg1));
	return 2;
    }

    public int l_cmpRR() {
	return 2;
    }

    public int cmpMR(byte reg1, int offset, byte reg2) {
        return mop(0x39,reg2,reg1,offset);
    }
    
    private int cmprm(byte reg, int offset) {
        if (offset==0 && reg!=R_BP) {
            cb.put(modRM(MOD_M, 7, reg));
            if (reg==R_SP) {
                cb.put((byte)0x24);
                return 2;
            } else {
                return 1;
            }
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, 7, reg));
            if (reg==R_SP) {
                cb.put((byte)0x24);
            }
            cb.put((byte)offset);
            return 2+(reg==R_SP?1:0);
        } else {
	    cb.put(modRM(MOD_M_32, 7, reg));
            if (reg==R_SP) {
                cb.put((byte)0x24);
            }
	    cb.putInt(offset);
            return 5+(reg==R_SP?1:0);
        }
    }

    public int cmpMI(byte reg, int offset, int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    cb.put((byte)0x83);
            int res=cmprm(reg,offset);
	    cb.put((byte)immediate);
	    return res+2;
	} else {
	    cb.put((byte)0x81);
            int res=cmprm(reg,offset);
	    cb.putInt(immediate);
	    return res+5;
	}
    }
    
    public int cmpMI(Pointer addr, int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    cb.put((byte)0x83);
            cb.put(modRM(MOD_M, 7, R_BP));
            cb.putInt(addr.castToInt());
            cb.put((byte)immediate);
	    return 7;
	} else {
	    cb.put((byte)0x81);
            cb.put(modRM(MOD_M, 7, R_BP));
            cb.putInt(addr.castToInt());
	    cb.putInt(immediate);
	    return 10;
	}
    }

    public int cmpM8I(byte reg, int offset, byte immediate) {
        cb.put((byte)0x80);
        int res=cmprm(reg,offset);
        cb.put(immediate);
        return res+2;
    }
    
    public int cmpM8I(Pointer addr, byte immediate) {
        cb.put((byte)0x80);
        cb.put(modRM(MOD_M, 7, R_BP));
        cb.putInt(addr.castToInt());
        cb.put(immediate);
        return 7;
    }

    public int cmpRI(byte reg, int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    cb.put((byte)0x83);
	    cb.put(modRM(MOD_R, 7, reg));
	    cb.put((byte)immediate);
	    return 3;
	} else {
	    cb.put((byte)0x81);
	    cb.put(modRM(MOD_R, 7, reg));
	    cb.putInt(immediate);
	    return 6;
	}
    }

    public int l_cmpIR(int immediate) {
	if (-128 <= immediate && immediate <= 127) {
	    return 3;
	} else {
	    return 6;
	}
    }

    /**
     * Emit a relative unconditional jump
     * @param offset the jump offset excluding the size of the jmp
     * instruction. If positive, forward jump, if negative backward
     * jump.
     **/
    public int jmp(int offset) {
	if (offset < 0) { // backward jump
	    int offset8  = offset - 2;
	    if (-128 <= offset8 && offset8 <= 127) {
		cb.put((byte)0xEB);
		cb.put((byte)offset8);
		return 2;
	    } else {
		int offset32 = offset - 5;
		cb.put((byte)0xE9);
		cb.putInt(offset32);
		return 5;
	    }
	} else { // forward jump
	    if (-128 <= offset && offset <= 127) {
		cb.put((byte)0xEB);
		cb.put((byte)offset);
		return 2;
	    } else {
		cb.put((byte)0xE9);
		cb.putInt(offset);
		return 5;
	    }
	}
    }
    
    public int jmp(Pointer address) {
        return jmp_long(address.sub(getAbsolutePC().add(5)).castToInt());
    }

    /**
     * Return the size of an unconditional jump
     * @param offset the jump offset excluding the size of the jmp
     * instruction. If positive, forward jump, if negative backward
     * jump.
     **/
    public int l_jmp(int offset) {
	if (offset < 0) {
	    int offset8 = offset - 2;
	    if (-128 <= offset8 && offset8 <= 127)
		return 2;
	    else
		return 5;
	} else {
	    if (-128 <= offset && offset <= 127)
		return 2;
	    else 
		return 5;
	}
    }

    public int jmp_long(int offset) {
	if (offset < 0) { // backward jump
	    cb.put((byte)0xE9);
	    cb.putInt(offset);
	    return 5;
	} else { // forward jump
	    cb.put((byte)0xE9);
	    cb.putInt(offset);
	    return 5;
	}
    }	
    
    public int l_jmp_long() {
	return 5;
    }

    /**
     * Emit an unconditional jump whose jump offset is not yet determined.
     * Assume a near jump which has a 32 bit offset.
     **/
    public int jmp_unlinked() {
	cb.put((byte)0xE9);
	cb.putInt(0);
	return 5;
    }

    /**
     * @return the offset of the immediate value from the beginning of 
     * the jump_unlinked instruction
     **/
    public int imm_offset_jmp_unlinked() {
	return 1;
    }

    public int l_jmp_unlinked() {
	return 5;
    }

    public int jmpAbsM(byte reg, int offset) {
        return mop(0xff,4,reg,offset);
    }

    public int jmpAbsR(byte reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_R, 4, reg));
	return 2;
    }

    public int l_jmpAbsR() {
	return 2;
    }

    // jmp reg (scale index base) jmp (base, reg*4)    
    public int jmpAbsMS4(int base,
                         byte index_reg) {
	cb.put((byte)0xFF);
	cb.put((byte)0x24);
	cb.put(modRM(MOD_M_32, index_reg, 5));
	cb.putInt(base);
	return 7;
    }

    public int jmpAbsMS(byte reg,int offset,byte index,int scale) {
	cb.put((byte)0xFF);
        if (offset==0 && reg!=R_BP) {
            cb.put(modRM(MOD_M, 4, 4));
            cb.put(SIB(scale,index,reg));
            return 3;
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, 4, 4));
            cb.put(SIB(scale,index,reg));
            cb.put((byte)offset);
            return 4;
        } else {
            cb.put(modRM(MOD_M_32, 4, 4));
            cb.put(SIB(scale,index,reg));
            cb.putInt(offset);
            return 7;
        }
    }

    public int jmpAbsMS4_unlinked(byte index_reg) {
	cb.put((byte)0xFF);
	cb.put((byte)0x24);
	cb.put(modRM(MOD_M_32, index_reg, 5));
	cb.putInt(0);
	return 7;
    }

    public int l_jmpAbsMS4_unlinked() {
	return 7;
    }

    public int imm_offset_jmpAbsMS4_unlinked() {
	return 3;
    }

    public int call_long(int offset) {
	cb.put((byte)0xE8);
	cb.putInt(offset);
	return 5;
    }

    public int call(int offset) {
        // FIXME: optimize?
        return call_long(offset);
    }

    public int l_call() {
	return 5;
    }

    public int call_imm_offset() {
	return 1;
    }

    public int call_unlinked() {
	cb.put((byte)0xE8);
	cb.putInt(0);
	return 5;
    }

    public int call_unlinked_imm_offset() {
	return 1;
    }
	
    public int callAbsR(byte reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_R, 2, reg));
	return 2;
    }

    public int l_callAbsR() {
	return 2;
    }

    public int callAbsM(byte reg,int offset) {
        return mop(0xff,2,reg,offset);
    }

    public int callAbsMS(byte reg,int offset,byte index,int scale) {
	cb.put((byte)0xFF);
        if (offset==0 && reg!=R_BP) {
            cb.put(modRM(MOD_M, 2, 4));
            cb.put(SIB(scale,index,reg));
            return 3;
        } else if (offset>=-128 && offset<=127) {
            cb.put(modRM(MOD_M_8, 2, 4));
            cb.put(SIB(scale,index,reg));
            cb.put((byte)offset);
            return 4;
        } else {
            cb.put(modRM(MOD_M_32, 2, 4));
            cb.put(SIB(scale,index,reg));
            cb.putInt(offset);
            return 7;
        }
    }

    public void call(Pointer addr) {
	// Compute delta between next PC on return and the function to
	// be called.  (The call instruction will always be 5 bytes
	// long.  We could generate a 4 byte instruction with a 16-bit
	// delta, but we probably shouldn't bother with something gas
	// doesn't do.)
	call(addr.sub(getAbsolutePC().add(5)).castToInt());
    }

    /**
     * Emit a conditional jump
     * @param condition the condition of the jump
     * @param offset the jump offset excluding the size of the jcc
     * instruction. If positive, forward jump, if negative backward
     * jump.
     **/
    public int jcc(byte condition, int offset) {
	if (offset < 0) { // backward jump
	    int offset8  = offset - 2;
	    if (-128 <= offset8 && offset8 <= 127) {
		cb.put((byte)(0x70 + condition));
		cb.put((byte)offset8);
		return 2;
	    } else {
		int offset32 = offset - 6;
		cb.put((byte)0x0F);
		cb.put((byte)(0x80 + condition));
		cb.putInt(offset32);
		return 6;
	    }
	} else { // forward jump
	    if (-128 <= offset && offset <= 127) {
		cb.put((byte)(0x70 + condition));
		cb.put((byte)offset);
		return 2;
	    } else {
		cb.put((byte)0x0F);
		cb.put((byte)(0x80 + condition));
		cb.putInt(offset);
		return 6;
	    }
	}
    }

    /**
     * Return the size of a conditional jump
     * @param offset the jump offset excluding the size of the jcc
     * instruction. If positive, forward jump, if negative backward
     * jump.
     **/
    public int l_jcc(int offset) {
	if (offset < 0) {
	    int offset8 = offset - 2;
	    if (-128 <= offset8 && offset8 <= 127)
		return 2;
	    else
		return 6;
	} else {
	    if (-128 <= offset && offset <= 127)
		return 2;
	    else 
		return 6;
	}
    }

    /**
     * Emit a conditional jump whose jump offset is not yet determined.
     * Assume a near jump which has a 32 bit offset.
     **/
    public int jcc_unlinked(byte condition) {
	cb.put((byte)0x0F);
	cb.put((byte)(0x80 + condition));
	cb.putInt(0);
	return 6;
    }
    
    public int setcc(byte condition, byte dest) {
        cb.put((byte)0x0F);
        cb.put((byte)(0x90 + condition));
        cb.put(modRM(MOD_R, 0, dest));
        return 3;
    }

    public int imm_offset_jcc_unlinked() {
	return 2;
    }

    // fld (32bits) mem -> (FPU reg stack top)
    public int fldM32(byte src, int offset) {
        return mop(0xd9,0,src,offset);
    }

    // fld (32bits) mem -> (FPU reg stack top)
    public int fldM32(byte src) {
        return mop(0xd9,0,src,0);
    }

    // fadd (32 or 64 bits) st(0) = st(0) + st(1)
    public int fadd() {
	cb.put((byte)0xD8);
	cb.put((byte)(0xC0 + 1));
	return 2;
    }
    
    // fadd (32bits) (FPU reg stack top) += mem
    public int faddM32(byte src,
		       int offset) {
        return mop(0xd8,0,src,offset);
    }
    
    public int faddM32(byte src) {
        return faddM32(src,0);
    }

    // fsub (32bits) (FPU reg stack top) -= mem
    public int fsubM32(byte src,
		       int offset) {
        return mop(0xd8,4,src,offset);
    }

    // fsub (32bits) (FPU reg stack top) -= mem
    public int fsubM32(byte src) {
        return mop(0xd8,4,src,0);
    }

    // fmul (32bits) (FPU reg stack top) *= mem
    public int fmulM32(byte src,
		       int offset) {
        return mop(0xd8,1,src,offset);
    }
    
    public int fmulM32(byte src) {
        return fmulM32(src,0);
    }

    // fdiv (32bits) (FPU reg stack top) /= mem
    public int fdivM32(byte src,
		       int offset) {
        return mop(0xd8,6,src,offset);
    }

    // fdiv (32bits) (FPU reg stack top) /= mem
    public int fdivM32(byte src) {
        return mop(0xd8,6,src,0);
    }

    // fstp (32bits) (FPU reg stack top) -> mem
    public int fstpM32(byte dst,
		       int offset) {
        return mop(0xd9,3,dst,offset);
    }

    // fstp (32bits) (FPU reg stack top) -> mem
    public int fstpM32(byte dst) {
        return mop(0xd9,3,dst,0);
    }

    // fchs (changes the sign of (FPU reg stack top)
    public int fchs() {
	cb.put((byte)0xD9);
	cb.put((byte)0xE0);
	return 2;
    }

    // fld (64bits) mem -> (FPU reg stack top)
    public int fldM64(byte src,
		      int offset) {
        return mop(0xdd,0,src,offset);
    }

    // fld (64bits) mem -> (FPU reg stack top)
    public int fldM64(byte src) {
        return mop(0xdd,0,src,0);
    }

    // fadd (64bits) (FPU reg stack top) += mem
    public int faddM64(byte src,
		       int offset) {
        return mop(0xdc,0,src,offset);
    }
    
    public int faddM64(byte src) {
        return faddM64(src,0);
    }

    // fsub (64bits) (FPU reg stack top) -= mem
    public int fsubM64(byte src,
		       int offset) {
        return mop(0xdc,4,src,offset);
    }

    // fsub (64bits) (FPU reg stack top) -= mem
    public int fsubM64(byte src) {
        return mop(0xdc,4,src,0);
    }

    // fmul (64bits) (FPU reg stack top) *= mem
    public int fmulM64(byte src,
		       int offset) {
        return mop(0xdc,1,src,offset);
    }
    
    public int fmulM64(byte src) {
        return fmulM64(src,0);
    }

    // fdiv (64bits) (FPU reg stack top) /= mem
    public int fdivM64(byte src,
		       int offset) {
        return mop(0xdc,6,src,offset);
    }

    // fdiv (64bits) (FPU reg stack top) /= mem
    public int fdivM64(byte src) {
        return mop(0xdc,6,src,0);
    }

    // fstp (64bits) (FPU reg stack top) -> mem
    public int fstpM64(byte src,
		       int offset) {
        return mop(0xdd,3,src,offset);
    }

    // fstp (64bits) (FPU reg stack top) -> mem
    public int fstpM64(byte src) {
        return mop(0xdd,3,src,0);
    }

    // fucompp (fcmp + 2 pops)
    public int fucompp() {
	cb.put((byte)0xDA);
	cb.put((byte)0xE9);
	return 2;
    }

    // fucompp + fstsw + sahf
    public int fcmp() {
	cb.put((byte)0xDA);
	cb.put((byte)0xE9);
	cb.put((byte)0x9B);
	cb.put((byte)0xDF);
	cb.put((byte)0xE0);
	cb.put((byte)0x9E);
	return 6;
    }

    public int fldcw(byte src, int offset) {
        return mop(0xd9,5,src,offset);
    }

    public int fnstcw(byte dst, int offset) {
        return mop(0xd9,7,dst,offset);
    }

    public int nop() {
	cb.put((byte)0x90);
	return 1;
    }
    
    public int nops(int n) {
        int result=0;
        for (int i=0;i<n;++i) {
            result+=nop();
        }
        return result;
    }
    
    public int nopAlign(int multiplier) {
        return nopAlign(multiplier,0);
    }
    
    public int nopAlign(int multiplier,
                        int offset) {
        int result=0;
        while (getAbsolutePC().add(offset).mod(multiplier)!=Pointer.zero()) {
            result+=nop();
        }
        return result;
    }
    
    /**
     * Inserts enough nops so that if you emit something that is <code>width</code>
     * wide, it will fit in an alignment slot <code>multiplier</code> wide.  This
     * is great if you want to be able to atomically patch an instruction that
     * is <code>width</code> wide using a single store that is <code>multiplier</code>
     * wide.
     */
    public int nopAlignToFit(int multiplier,
                             int width) {
        if (width>multiplier) {
            throw new Error("width>multiplier: "+width+", "+multiplier);
        }
        int result=0;
        while (getAbsolutePC().div(multiplier)!=
               getAbsolutePC().add(width-1).div(multiplier)) {
            result+=nop();
        }
        return result;
    }

    public int ret() {
	cb.put((byte)0xC3);
	return 1;
    }
    
    public int leave() {
	cb.put((byte)0xC9);
        return 1;
    }

    public int retleave() {
	cb.put((byte)0xC9);
	cb.put((byte)0xC3);
	return 2;
    }

    public int incR(byte reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_R, 0, reg));
	return 2;
    }

    public int l_incR() {
	return 2;
    }

    public int decR(byte reg) {
	cb.put((byte)0xFF);
	cb.put(modRM(MOD_R, 1, reg));
	return 2;
    }

    public int l_decR() {
	return 2;
    }
    
    public int btIR(byte imm,byte reg) {
        cb.put((byte)0x0f);
        cb.put((byte)0xba);
        cb.put((byte)(0xe0+reg));
        cb.put(imm);
        return 4;
    }

    public int btIM(byte imm,byte reg,int offset) {
        cb.put((byte)0x0f);
        int result=mop(0xba,4,reg,offset);
        cb.put(imm);
        return result+2;
    }

    /**
     * Emits a movMR depending upon the size of the value
     *
     * @param srcReg the base register of the source
     * @param displacement the 32-bit displacement of the source
     * @param destReg1 the destination register 1 (lower word)
     * @param destReg2 the destination register 2 (higher word) untouched unless valueSize == 8
     * @param valueSize the size of the value (1, 2, 4, or 8 bytes)
     * @param signed if true, when valueSize == 2, the value is sign-extended; otherwise zero-extended
     **/
  /* unused
    private void movMRS(byte srcReg, 
			int displacement, 
			byte destReg1, 
			byte destReg2,
			int valueSize,
			boolean signed) {
        switch(valueSize) {
        case 1:
            movsxM8R(srcReg, displacement, destReg1);
            break;
        case 2:
	    if (signed)
		movsxM16R(srcReg, displacement, destReg1);
	    else
		movzxM16R(srcReg, displacement, destReg1);
            break;
        case 4:
	    if (displacement == 0) {
		movMR(srcReg, 0, destReg1);
	    } else {
		movMR(srcReg, displacement, destReg1);
	    }
            break;
	case 8:
	    if (displacement == 0) {
		movMR(srcReg, 0, destReg1);
	    } else {
		movMR(srcReg, displacement, destReg1);
	    }
	    movMR(srcReg, displacement + 4, destReg2);
	    break;
        default:
            throw new Error("Invalid array element size");
	}
    }

    */

    public void movJMSR(char type,
                 byte src_reg,
                 byte index_reg,
                 byte dest_reg) {
        switch (type) {
        case 'Z':
        case 'B':
            movsxMS8R(src_reg,index_reg,SS_I_1,dest_reg);
            break;
        case 'C':
            movzxMS16R(src_reg,index_reg,SS_I_2,dest_reg);
            break;
        case 'S':
            movsxMS16R(src_reg,index_reg,SS_I_2,dest_reg);
            break;
        case 'I':
        case 'F':
        case 'L':
        case '[':
            movMSR(src_reg,index_reg,SS_I_4,dest_reg);
            break;
        default: throw new Error("bad type: "+type);
        }
    }

    public void movJRMS(char type,
                        byte src_reg,
                        byte dest_reg,
                        int offset,
                        byte index_reg) {
        switch (Types.bytes(type)) {
        case 1:
            movR8MS(src_reg,dest_reg,offset,index_reg,SS_I_1);
            break;
        case 2:
            movR16MS(src_reg,dest_reg,offset,index_reg,SS_I_2);
            break;
        case 4:
            movRMS(src_reg,dest_reg,offset,index_reg,SS_I_4);
            break;
        default:
            throw new Error("bad type: "+type);
        }
    }
    
    public void movJRMS(char type,
                        byte src_reg,
                        byte dest_reg,
                        byte index_reg) {
        movJRMS(type,src_reg,dest_reg,0,index_reg);
    }
    
    public void movJRM(char type,
                       byte src_reg,
                       byte dest_reg,
                       int offset) {
        switch (Types.bytes(type)) {
        case 1:
            movR8M(src_reg,dest_reg,offset);
            break;
        case 2:
            movR16M(src_reg,dest_reg,offset);
            break;
        case 4:
            movRM(src_reg,dest_reg,offset);
            break;
        default:
            throw new Error("bad type: "+type);
        }
    }
    
    /** Clobbers ECX in some cases */
    public void pushJMS(char type,
                        byte src_reg,
                        int offset,
                        byte index_reg) {
        switch (type) {
        case 'Z':
        case 'B':
            movsxMS8R(src_reg,offset,index_reg,SS_I_1,R_CX);
            pushR(R_CX);
            break;
        case 'C':
            movzxMS16R(src_reg,offset,index_reg,SS_I_2,R_CX);
            pushR(R_CX);
            break;
        case 'S':
            movsxMS16R(src_reg,offset,index_reg,SS_I_2,R_CX);
            pushR(R_CX);
            break;
        case 'I':
        case 'F':
        case 'L':
        case '[':
            pushMS(src_reg,offset,index_reg,SS_I_4);
            break;
        case 'J':
        case 'D':
            leaMSR(src_reg,offset,index_reg,SS_I_8,R_CX);
            pushM(R_CX,4);
            pushM(R_CX,0);
            break;
        default: throw new Error("bad type: "+type);
        }
    }
    
    public void pushJMS(char type,
                        byte src_reg,
                        byte index_reg) {
        pushJMS(type,src_reg,0,index_reg);
    }
    
    /** Clobbers ECX in some cases */
    public void pushJM(char type,
                       byte src_reg,
                       int offset) {
        switch (type) {
        case 'Z':
        case 'B':
            movsxM8R(src_reg,offset,R_CX); // 3 bytes if offset==0 else 7 bytes, or 4 bytes if offset is small
            pushR(R_CX); // 1 byte
            break;
        case 'C':
            movzxM16R(src_reg,offset,R_CX); // 3 bytes if offset==0 else 7 bytes, or 4 bytes if offset is small
            pushR(R_CX); // 1 byte
            break;
        case 'S':
            movsxM16R(src_reg,offset,R_CX); // 3 bytes if offset==0 else 7 bytes, or 4 bytes if offset is small
            pushR(R_CX); // 1 byte
            break;
        case 'I':
        case 'F':
        case 'L':
        case '[':
            pushM(src_reg,offset); // 2 bytes if offset==0 else 6 bytes, or 3 bytes if offset is small
            break;
        case 'J':
        case 'D':
            pushM(src_reg,offset+4); // 3 bytes if offset==0 else 6 bytes, or 3 bytes if offset is small
            pushM(src_reg,offset+0); // 2 bytes if offset==0 else 6 bytes, or 3 bytes if offset is small
            break;
        default: throw new Error("bad type: "+type);
        }
    }
    
    public void pushJM(char type,
                       byte src_reg) {
        pushJM(type,src_reg,0);
    }
    
    /** Clobbers ECX, EDX */
    public void popJM(char type,
                      byte trg_reg,
                      int offset) {
        if (logLevel>=2) {
            log(X86Assembler.class,2,
                "Doing popJM("+type+","+trg_reg+","+offset+")");
        }
        switch (Types.cells(type)) {
        case 1:
            popR(R_CX); // 1 byte
            movJRM(type,R_CX,trg_reg,offset); // if offset==0 then at most 3 bytes else 7 bytes, or 4 bytes if offset is small
            break;
        case 2:
            popR(R_CX); // 1 byte
            popR(R_DX); // 1 byte
            movRM(R_CX,trg_reg,offset+0); // 2 bytes if offset==0 else 6 bytes, or 3 bytes if offset is small
            movRM(R_DX,trg_reg,offset+4); // 3 bytes if offset==0 else 6 bytes, or 3 bytes if offset is small
            break;
        default:
            throw new Error("bad type: "+type);
        }
    }
    
    public void popJM(char type,
                      byte trg_reg) {
        popJM(type,trg_reg,0);
    }
    
    public void mulIR(int amount,
                      byte trg_reg,
                      byte scratch_reg) {
        if (amount==0) {
            xorRR(trg_reg,trg_reg);
            return;
        }
        
        if (amount==-2147483648) {
            movIR(amount, scratch_reg);
            mulRR(scratch_reg, trg_reg);
            return;
        }
        
        int origamount=amount;
        
        boolean postneg=false;
        if (amount<0) {
            postneg=true;
            amount=-amount;
        }
        
        boolean found=false;
        int postshift=0;
        int mul=amount;
        
    searchloop:
        for (;;) {
            switch (mul) {
            case 0:
                throw new Error("should not get here");
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
                found=true;
                break searchloop;
            default:
                if ((amount&1)==0) {
                    mul>>=1;
                    postshift++;
                    break;
                } else {
                    break searchloop;
                }
            }
        }
        
        if (found) {
            switch (mul) {
            case 1:
                break;
            case 3:
                leaMSR(trg_reg, 0, trg_reg, SS_I_2, trg_reg);
                break;
            case 5:
                leaMSR(trg_reg, 0, trg_reg, SS_I_4, trg_reg);
                break;
            case 7:
                movRR(trg_reg, scratch_reg);
                shlRI(trg_reg, (byte)3);
                subRR(scratch_reg, trg_reg);
                break;
            case 9:
                leaMSR(trg_reg, 0, trg_reg, SS_I_8, trg_reg);
                break;
            }

            shlRI(trg_reg, (byte)postshift);
            if (postneg) {
                negR(trg_reg);
            }
        } else {
            movIR(origamount, scratch_reg);
            mulRR(scratch_reg, trg_reg);
        }
    }

    void pushABunch(int nPush) {
        if (Settings.ASSERTS_ON && nPush<0) {
            throw new fivmError("nPush<0: nPush = "+nPush);
        }
        if (nPush==0) {
        } else if (nPush<3) {
            for (int i=0;i<nPush;++i) {
                pushR(R_AX);
            }
        } else {
            subIR(nPush*4,R_SP);
        }
    }
    
    // may clobber ecx
    void popABunch(int nPop) {
        if (Settings.ASSERTS_ON && nPop<0) {
            throw new fivmError("nPop<0: nPop = "+nPop);
        }
        if (nPop==0) {
        } else if (nPop<3) {
            for (int i=0;i<nPop;++i) {
                popR(R_CX);
            }
        } else {
            addIR(nPop*4,R_SP);
        }
    }
    
    /** Adjust the stack by the given number of words.
        Positive values mean pop, negative values mean push. */
    void pushOrPopABunch(int stackOffset) {
        if (stackOffset>=0) {
            popABunch(stackOffset);
        } else {
            pushABunch(-stackOffset);
        }
    }
}

