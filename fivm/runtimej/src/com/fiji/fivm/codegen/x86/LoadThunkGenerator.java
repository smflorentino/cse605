/*
 * LoadThunkGenerator.java
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

import com.fiji.fivm.r1.*;
import static com.fiji.fivm.codegen.x86.X86Constants.*;
import com.fiji.fivm.codegen.Assembler.Branch;
import com.fiji.fivm.codegen.*;

public final class LoadThunkGenerator extends Generator {
    public void generate() {
        // what do we do here?  we need to:
        // 1) set up stack frames to save the args so that the GC will see
        //    them; really we just need to do that for GC-able args
        //    NO: we do not need to do that.  the caller is responsible
        //    for putting arguments into GC maps
        // 2) call into compiler
        // 3) check if there was an error
        // 4) call into compiled code (tail-call preferably)
        
        asm.pushR(R_AX); // align the stack to 8-bytes
        
        asm.movRR(R_SP, R_AX);
        asm.movMR(R_AX, 8, R_AX);  // get the first argument (i.e. the ThreadState*)
        
        asm.pushI32(methodRec.castToInt());
        asm.pushR(R_AX); // stack now aligned to 16-bytes, coincidentally

        asm.call(MethodPtrs.handleLoadThunk);
        
        asm.movRR(R_SP, R_AX);
        asm.movMR(R_AX, 16, R_AX);  // reload the ThreadState*
        
        asm.movMR(R_AX,
                  CType.offsetof("fivmr_ThreadState","curException").castToInt(),
                  R_AX); // check if there was an exception
        
        asm.addIR(12, R_SP); // bring the stack to where it was

        asm.testRR(R_AX,R_AX); // is R_EAX zero?
        
        Branch b=asm.setBranchSourceAndJcc(J_E);
        
        exitNull(); // if it's not zero (i.e. an exception occurred) then exit
        
        asm.setBranchTarget(b);
        
        asm.movIR(methodRec.castToInt(), R_AX); // load the MethodRec
        asm.movMR(R_AX,
                  CType.offsetof("fivmr_MethodRec","entrypoint").castToInt(),
                  R_AX); // load the entrypoint that the compiler would have produced
        asm.jmpAbsR(R_AX); // tail call (the called function will see exactly the args we saw)
    }
}


