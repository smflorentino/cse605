/*
 * Assembler.java
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

package com.fiji.fivm.codegen;

import java.nio.ByteBuffer;
import com.fiji.fivm.r1.*;

/**
 * An abstract type of assemblers
 *
 * @author Hiroshi Yamauchi
 **/
@ExcludeUnlessSet({"CLASSLOADING"})
public abstract class Assembler {

    protected ByteBuffer cb;

    protected Assembler(ByteBuffer cb) {
	this.cb = cb;
    }

    public int getPC() {
        return cb.position();
    }

    public Pointer getAbsolutePC() {
        return java.nio.fivmSupport.positionAddress(cb);
    }
    
    public Pointer getAbsolutePC(int pc) {
        return java.nio.fivmSupport.positionAddress(cb,pc);
    }

    public int write(byte b) {
	cb.put(b);
	return 1;
    }

    public int write32(int w) {
	cb.putInt(w);
	return 4;
    }
    
    public int write64(long w) {
        cb.putLong(w);
        return 8;
    }

    public int write(byte[] b) {
	for(int i = 0; i < b.length; i++)
	    cb.put(b[i]);
	return b.length;
    }
    
    public int write(Pointer p) {
        if (Pointer.size()==4) {
            return write32(p.castToInt());
        } else {
            return write64(p.asLong());
        }
    }

    /* 
     * Note on the branch mechanism: since calculating branch offsets is tedious,
     * use the following convention:
     * ex.
     * (forward jump)
     *     ...
     *     Branch b = asm.setBranchSourceAndJcc(J_E);
     *     ...
     *     ...
     *     asm.setBranchTarget(b);
     *
     * (backward jump)
     *     ...
     *     Branch b = asm.setBranchTarget();
     *     ...
     *     ...
     *     asm.setBranchSourceAndJmp(b);
     *
     * By this mechanism, programmers do not need to calculate branch offsets.
     * This automatically patches up the jump offset in the jump instructions.
     * The only disadvantage of this approach is that not being to able to use 
     * 1-byte field of branch offsets.
     */
    public static class Branch {
        /*
         * We need to patch the branch instruction at sourcePC.
         * The branch offset location within the instruction is
         * written as code[sourcePC] = sign_mask((targetPC-sourcePC),bits)<<shift;
         */
        public int sourcePC; // The PC of the branch instruction
        public Pointer addrAddr; // the address that should be patched, if different from the branch instruction; this being non-zero implies an absolute jump
        public int shift;
        public int bits;
        public Pointer targetPC; // The PC of the target instruction
        
        private Branch(int sourcePC, int shift, int bits, Pointer targetPC) {
            this.sourcePC = sourcePC;
            this.shift = shift;
            this.bits = bits;
            this.targetPC = targetPC;
        }
        public Branch(int sourcePC, int shift, int bits) {
            this(sourcePC, shift, bits, Pointer.zero().sub(1));
        }
        public Branch(Pointer targetPC) {
            this(-1, 0, 32, targetPC);
        }
        public Branch(int spc, Pointer tpc) {
            this(spc, 0, 32, tpc);
        }
    }
}
