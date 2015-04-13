/*
 * MethodImplSnippetGenerator.java
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

public final class MethodImplSnippetGenerator extends MethodImplGenerator {
    int startPC;
    
    public MethodImplSnippetGenerator(int startPC) {
        this.startPC=startPC;
    }
    
    void pushStateBufElement(int index) {
        if (index<Constants.TS_STATE_BUF_LEN) {
            asm.pushM(R_SI,
                      CType.offsetof("fivmr_ThreadState","stateBuf").castToInt()
                      + index*4);
        } else {
            asm.pushM(R_CX, index*4);
        }
    }
    
    public void generate() {
        // the plan:
        // 1) generate patch (prologue from stateBuf)
        // 2) generate snippet
        
        // how this works:
        // - we will only be patched to *after* the lock is acquired for
        //   synchronized methods.  so no need to do that.
        // - we still have to do a stack height check, since this will
        //   be a non-tail call.
        // - we have the complete stateSize in ThreadState::stateSize.  but,
        //   crucially, we have it *right now* not at the time this code
        //   that we generate will run.
        
        generatePreLocalPrologue();
        
        int minStateSize=1+maxLocals();
        int maxStateSize=minStateSize+maxStack();
        int stateSize=CType.getPointer(Magic.curThreadState(),
                                       "fivmr_ThreadState",
                                       "stateSize").castToInt();
        
        if (Settings.ASSERTS_ON && stateSize>maxStateSize) {
            throw new fivmError("stateSize = "+stateSize+", but maxStateSize = "+maxStateSize);
        }
        
        if (Settings.ASSERTS_ON && stateSize<minStateSize) {
            throw new fivmError("stateSize = "+stateSize+", but 1 + maxLocals = "+minStateSize);
        }
        
        if (stateSize>Constants.TS_STATE_BUF_LEN) {
            asm.movMR(R_SI,
                      CType.offsetof("fivmr_ThreadState","stateBufOverflow").castToInt(),
                      R_CX);
        }
        
        int stackHeight=stateSize-minStateSize;
        
        if (saveReceiver()) {
            pushStateBufElement(0);
        }
        
        for (int i=maxLocals();i-->0;) {
            pushStateBufElement(1+i);
        }

        for (int i=0;i<stackHeight;++i) {
            pushStateBufElement(minStateSize+i);
        }
        
        generateStackHeightCheck(stackHeight);
        
        generateForSnippet(startPC);
    }
}

