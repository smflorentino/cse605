/*
 * CloneHelperGenerator.java
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
import com.fiji.fivm.om.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.codegen.x86.X86Constants.*;
import com.fiji.fivm.codegen.Assembler.Branch;
import com.fiji.fivm.codegen.*;

public final class CloneHelperGenerator extends Generator {
    public void generate() {
        Pointer td=fivmr_MethodRec_owner(methodRec);
        
        Pointer noRefs=code.allocDebugID(0,0,0,new int[0]);

        // prologue
        asm.pushR(R_BP);
        asm.movRR(R_SP, R_BP);
        
        asm.pushR(R_SI); // will use ESI for thread state
        asm.pushR(R_DI); // will use EDI for destination object

        // here we are 16-byte aligned

        asm.pushR(R_BX); // will use EBX for source object (receiver)
        
        asm.movMR(R_BP, 8, R_SI);
        
        asm.pushM(R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt());
        
        asm.leaMR(R_BP, -20, R_CX);
        asm.movRM(R_CX, R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt());
        
        asm.pushI32(noRefs.castToInt());
        
        // from here we need one push to be 16-byte aligned, and our calls will
        // be pushing either 4 args (allocation) or 3 args (store barrier).  the
        // stack layout is such that we're 7 words away from the first argument.
        
        // allocate the object.  this involves pushing four arguments.  which means
        // we have to align the stack and then do the pushes
        asm.pushR(R_AX);
        
        // stack is 16-byte aligned
        
        asm.pushI32(td.castToInt());
        asm.pushI32(0);
        asm.pushI32(0);
        asm.pushR(R_SI);
        asm.call(MethodPtrs.alloc);
        
        // check exception
        asm.cmpMI(R_SI,
                  CType.offsetof("fivmr_ThreadState","curException").castToInt(),
                  0);
        Branch b=asm.setBranchSourceAndJcc(J_E);
        asm.jmpAbsM(R_SI,
                    CType.offsetof("fivmr_ThreadState","baselineThrowThunk").castToInt());
        asm.setBranchTarget(b);
        
        // ok, we have the target object in R_EAX.  move it to EDI and pop
        // the stack so that we're three pushes away from being aligned.  that's
        // optimal for invoking the barrier.
        
        // and if we're compiling for OM_FRAGMENTED, we also need to save the value
        // of EAX/EDI because the copying code may clobber it.
        // FIXME: optimize this for:
        // (a) empty objects
        // (b) objects with no refs
        
        if (Settings.OM_FRAGMENTED) {
            asm.addIR(16,R_SP);
            asm.pushR(R_AX);
        } else {
            asm.addIR(12, R_SP);
        }
        asm.movRR(R_AX, R_DI);
        
        // load the source object
        
        asm.movMR(R_BP, 12, R_BX);
        
        // ok - now we can have some fun.  first we decode the GC map.
        int size=CType.getInt(td,"fivmr_TypeData","size");
        int[] gcmap=GCMapBuilder.buildDefault().decodeGCMap(
            new PointerAPIImpl(td.add(CType.offsetof("fivmr_TypeData","gcMap"))),
            size);
        
        if (Settings.OM_CONTIGUOUS) {
            // offset is from the beginning of the object, not where the
            // object points.
            for (int idx=MM.allocOffset()+MM.objectPayloadOffset();
                 idx<size;
                 idx+=Pointer.size()) {
                int off=idx-MM.allocOffset();
                int loc=off-MM.objectPayloadOffset();
                
                asm.movMR(R_BX, off, R_AX);
                if (IntUtil.bit(gcmap,loc/Pointer.size())) {
                    asm.pushR(R_AX);
                    asm.leaMR(R_DI, off, R_CX);
                    asm.pushR(R_CX);
                    asm.pushR(R_SI);
                    asm.call(CVar.addressOf("fivmr_GC_storeDefMask"));
                    asm.addIR(12, R_SP);
                } else {
                    asm.movRM(R_AX, R_DI, off);
                }
            }

            asm.movRR(R_DI, R_AX);
        } else if (Settings.OM_FRAGMENTED) {
            for (int baseIdx=0;
                 baseIdx<size;
                 baseIdx+=OMData.chunkWidth()) {
                for (int idx=
                         baseIdx+(baseIdx==0?
                                  OMData.totalHeaderSize():
                                  OMData.fhHeaderSize());
                     idx<baseIdx+OMData.chunkWidth();
                     idx+=Pointer.size()) {
                    int off=idx-baseIdx;
                    int loc=idx;
                    
                    asm.movMR(R_BX, off, R_AX);
                    if (IntUtil.bit(gcmap,loc/Pointer.size())) {
                        asm.pushR(R_AX);
                        asm.leaMR(R_DI, off, R_CX);
                        asm.pushR(R_CX);
                        asm.pushR(R_SI);
                        asm.call(CVar.addressOf("fivmr_GC_storeDefMask"));
                        asm.addIR(12, R_SP);
                    } else {
                        asm.movRM(R_AX, R_DI, off);
                    }
                }
                if (baseIdx+OMData.chunkWidth()<size) {
                    asm.movMR(R_DI, 0, R_DI);
                    asm.movMR(R_BX, 0, R_BX);
                    asm.andIR(-4, R_DI);
                    asm.andIR(-4, R_BX);
                }
            }
            
            asm.popR(R_AX);
        } else {
            throw new fivmError("bad object model");
        }
        
        // ok all fields copied; return
        asm.movRR(R_BP, R_SP);
        asm.subIR(16, R_SP);
        asm.popM(R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt());
        asm.popR(R_BX);
        asm.popR(R_DI);
        asm.popR(R_SI);
        asm.popR(R_BP);
        asm.ret();
        
        if (Settings.ASSERTS_ON) {
            asm.breakpoint();
        }
    }
}

