/*
 * InterfaceResolutionGenerator.java
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

/**
 * Generates the interface method resolution stub for an interface method.
 * This is a code stub that is attached to the MethodRec for an interface
 * method.  If you want to call an interface method from baseline code (or
 * from any code that follows baseline conventions, including basepoints,
 * baseline try/catch, and thread state in ESI) then you retrieve that
 * interface method's MachineCode for which the FIVMR_MC_KIND is
 * FIVMR_MC_INTERFACE_RES.  Typically you do this by calling
 * fivmRuntime.getIntefaceResolutionFor(methodRec), which will generate
 * this stub for you (by eventually calling into this class by way of
 * several levels of indirection) if it wasn't already generated.
 */
public final class InterfaceResolutionGenerator extends Generator {
    public void generate() {
        // what this code does:
        // - null check
        // - interface type check
        // - interface resolution and tail invocation
        
        // what it needs:
        // - 5 registers - we use EAX, EBX, ECX, EDX, and EDI
        // - that means we need to save (and then restore) EBX and EDI
        // - note we want to avoid using EBP and ESI since they're special
        //   in baseline code.
        
        // what it can assume:
        // - stack alignment
        // - this means that if we have to manufacture a call then
        //   we at least know what the starting alignment is
        
        // stack: we are aligned minus 4 bytes
        
        asm.pushR(R_BX);
        asm.pushR(R_DI);
        
        // stack: now we are aligned minus 12 bytes
        
        asm.movMR(R_SI,
                  CType.offsetof("fivmr_ThreadState", "typeEpoch").castToInt(),
                  R_DI);
        asm.movRR(R_DI, R_DX);
        
        asm.mulIR(CType.sizeof("fivmr_TypeEpoch").castToInt(), R_DX, R_AX);
        
        asm.movIR(fivmr_MethodRec_owner(methodRec).castToInt(), R_AX);
        
        // EDI = type epoch, EDX = type epoch offset, EAX = type b
        
        asm.movRR(R_SP, R_BX);
        asm.movMR(R_BX,
                  4 /* saved edi */ +
                  4 /* saved ebx */ +
                  4 /* return address */ +
                  4 /* thread state */,
                  R_BX);
        
        // EDI = type epoch, EDX = type epoch offset, EAX = type b, EBX = receiver
        
        // do the null check
        
        asm.testRR(R_BX, R_BX);
        
        Branch nonNull=asm.setBranchSourceAndJcc(J_NE);
        
        asm.popR(R_DI);
        asm.popR(R_BX);
        asm.popR(R_AX); // pop return address
        // call NullPointerRTE with the interface method's arguments. ;-)
        asm.call(MethodPtrs.throwNullPointerRTE);
        asm.jmpAbsM(R_SI, CType.offsetof("fivmr_ThreadState", "baselineThrowThunk").castToInt());
        
        asm.setBranchTarget(nonNull);
        
        // at this point we don't need the receiver - we just need its type
        
        asm.movMR(R_BX, -OMData.objectTDOffset(), R_BX);
        if (Settings.HM_POISONED) {
            asm.subIR(1, R_BX);
        }
        
        asm.movMR(R_BX,
                  CType.offsetof("fivmr_Monitor", "forward").castToInt(),
                  R_BX);
        
        // EDI = type epoch, EDX = type epoch offset, EAX = type b, EBX = type a
        
        asm.leaMSR(R_AX,
                   CType.offsetof("fivmr_TypeData", "epochs").castToInt(),
                   R_DX,
                   SS_I_1,
                   R_AX);

        // EDI = type epoch, EDX = type epoch offset, EAX = type epoch b, EBX = type a
        
        // and really, we don't even care about the receivers type - just its
        // type epoch.  and after this we don't need the type epoch offset
        
        asm.leaMSR(R_BX,
                   CType.offsetof("fivmr_TypeData","epochs").castToInt(),
                   R_DX,
                   SS_I_1,
                   R_BX);

        // EDI = type epoch, EAX = type epoch b, EBX = type epoch a
        
        // from this point forward we need to keep EDI and EBX unclobbered, but
        // we have three other registers to play with
        
        asm.movMR(R_BX,
                  CType.offsetof("fivmr_TypeEpoch","buckets").castToInt(),
                  R_CX);
        
        // EDI = type epoch, EAX = type epoch b, EBX = type epoch a, ECX = buckets of type a
        
        asm.movzxM16R(R_AX,
                      CType.offsetof("fivmr_TypeEpoch","bucket").castToInt(),
                      R_DX);
        
        // EDI = type epoch, EAX = type epoch b, EBX = type epoch a, ECX = buckets of type a,
        // EDX = type b's bucket
        
        // we don't need type epoch b after this
        
        asm.movsxM8R(R_AX,
                     CType.offsetof("fivmr_TypeEpoch","tid").castToInt(),
                     R_AX);

        // EDI = type epoch, EAX = type b's tid, EBX = type epoch a, ECX = buckets of type a,
        // EDX = type b's bucket
        
        // we don't need buckets of type a after this.  we also don't need type b's
        // bucket
        
        asm.movsxMS8R(R_CX, R_DX, SS_I_1, R_CX);
        
        // EDI = type epoch, EAX = type b's tid, EBX = type epoch a,
        // ECX = type a's tid for type b's bucket
        
        asm.cmpRR(R_CX, R_AX);
        
        Branch typeOK=asm.setBranchSourceAndJcc(J_E);
        
        asm.popR(R_DI);
        asm.popR(R_BX);
        asm.popR(R_AX);
        // call ClassChangeRTE with the interface method's arguments. ;-)
        asm.call(MethodPtrs.throwClassChangeRTE);
        asm.jmpAbsM(R_SI, CType.offsetof("fivmr_ThreadState", "baselineThrowThunk").castToInt());
        
        asm.setBranchTarget(typeOK);
        
        // EDI = type epoch, EBX = type epoch a
        
        asm.movIR(methodRec.add(CType.offsetof("fivmr_MethodRec","location")).castToInt(),
                  R_CX);
        
        // EDI = type epoch, EBX = type epoch a, ECX = address of location field
        
        asm.movzxMS16R(R_CX, R_DI, SS_I_2, R_CX);
        
        // EBX = type epoch a, ECX = itable index
        
        asm.movMR(R_BX,
                  CType.offsetof("fivmr_TypeEpoch","itable").castToInt(),
                  R_AX);
        
        // R_EAX = itable, ECX = itable index
        
        // restore EBX, EDI, and restore the stack to where the interface
        // method would have wanted
        asm.popR(R_DI);
        asm.popR(R_BX);
        
        // make the call!
        asm.jmpAbsMS(R_AX, 0, R_CX, SS_I_4);
        
        if (Settings.ASSERTS_ON) {
            // be safe...
            asm.breakpoint();
        }
    }
}

