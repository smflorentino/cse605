/*
 * Protocols.java
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

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;
import static com.fiji.fivm.codegen.x86.X86Constants.*;
import com.fiji.fivm.codegen.Assembler.Branch;

public final class X86Protocols {
    private X86Protocols() {}
    
    public static final int OFFSET_FROM_EBP_TO_DEBUG_ID = -4-4-4;
    public static final int OFFSET_FROM_EBP_TO_UP = -4-4;

    public static final int OFFSET_FROM_FLD_RA_TO_JMP_ADDR = -12;
    
    public static final int OFFSET_FROM_FLD_RA_TO_DONE = 0;

    public static final int OFFSET_FROM_MTH_RA_TO_JMP_ADDR = -12;
    public static final int OFFSET_FROM_IFACE_MTH_RA_TO_CALL_ADDR = -14;

    public static final int OFFSET_FROM_MTH_RA_TO_PERP = 0;
    
    public static final int OFFSET_FROM_AA_RA_TO_DONE = 0;
    public static final int OFFSET_FROM_AA_RA_TO_JMP_ADDR = -12;
    
    public static final int OFFSET_FROM_OA_RA_TO_DONE = 0;
    public static final int OFFSET_FROM_OA_RA_TO_JMP_ADDR = -12;
    
    public static final int OFFSET_FROM_IO_RA_TO_DONE = 0;
    public static final int OFFSET_FROM_IO_RA_TO_JMP_ADDR = -12;
    
    // these two procedures are probably not needed by anyone
    
    // do not use EDI if you're using this
    public static void prepForCall(X86Assembler asm,int nArgs) {
        asm.pushR(R_DI);
        asm.movRR(R_SP,R_DI);
        asm.andIR(-16,R_SP);
        
        int nPush=nArgs;
        
        while ((nPush&15)!=0) {
            nPush++;
        }
        
        asm.pushABunch(nPush);
    }
    
    public static void perpForCall(X86Assembler asm,int nArgs,ReturnMode rm) {
        if (rm==ReturnMode.NON_TERMINATING) {
            asm.breakpoint();
        } else {
            asm.movRR(R_DI, R_SP);
            asm.popR(R_DI);
            
            if (rm.canReturn()) {
                if (rm.canThrow()) {
                    asm.cmpMI(R_SI,
                              CType.offsetof("fivmr_ThreadState","curException").castToInt(),
                              0);
                    Branch b=asm.setBranchSourceAndJccShort(J_E);
                    asm.jmpAbsM(
                        R_SI,
                        CType.offsetof("fivmr_ThreadState","baselineThrowThunk").castToInt());
                    asm.setBranchTarget(b);
                }
            } else {
                asm.jmpAbsM(
                    R_SI,
                    CType.offsetof("fivmr_ThreadState","baselineThrowThunk").castToInt());
            }
        }
    }
}

