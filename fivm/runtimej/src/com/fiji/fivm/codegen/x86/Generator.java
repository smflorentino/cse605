/*
 * Generator.java
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

import static com.fiji.fivm.codegen.x86.X86Constants.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import com.fiji.fivm.codegen.Assembler.Branch;

import com.fiji.fivm.codegen.*;
import com.fiji.fivm.r1.*;
import com.fiji.fivm.om.*;
import com.fiji.fivm.*;

public abstract class Generator {
    // conventions:
    // - follow Linux/x86 cdecl style (which I believe Mac OS X also follows)
    // - keep $esp 8-byte aligned:
    //   - assume that after a call it will be 8-byte aligned plus 4 (i.e. it will
    //     be exactly NOT 8-byte aligned)
    //   - prologue should make it 8-byte aligned, by subtracting 4 if necessary
    //   - stack should always be 8-byte aligned at the point of call
    // - callee saved: $ebp, $ebx, $esi, $edi
    // - caller saved: $eax, $ecx, $edx, $esp (i.e. use these for scratch without having
    //   to save them but a call may trash them)
    
    // stack frame layout:
    //         argN
    //         ...
    //         arg0
    //         return address
    // ebp --> saved ebp
    //         saved esi
    //         fivmr_Frame: up
    //         fivmr_Frame: debugID
    //         receiver copy (if instance synchronized method)
    //         local(N-1)
    //         ...
    //         local0
    //         stack0
    //         ...
    // esp --> stack(N-1)
    
    // note #1: esp floats when we push/pop the stack at heights >= 3
    // note #2: esi holds the thread state pointer
    // note #3: eax, ecx, edx are available for scratch use
    // note #4: ebp is aligned, esp may not be
    
    // what does debugID hold?  we'll store to debugID anytime we map an outgoing
    // call.
    
    // NOTE: at some point, consider register-allocating part of the stack
    
    MachineCode code;
    X86Assembler asm;
    Pointer methodRec;
    
    public void initTrg(MachineCode code) {
        this.code=code;
        this.asm=new X86Assembler(code.getBuffer());
    }
    
    public void initSrc(Pointer methodRec) {
        this.methodRec=methodRec;
    }
    
    public abstract void generate();
    
    public int currentSize() {
        return asm.getPC();
    }
    
    int maxStack() {
        return CType.getChar(methodRec,"fivmr_MethodRec","nStack");
    }
    
    int maxLocals() {
        return CType.getChar(methodRec,"fivmr_MethodRec","nLocals");
    }
    
    int methodFlags() {
        return fivmr_MethodRec_flags(methodRec);
    }
    
    void produceNullReturn() {
        byte exitType=
            fivmr_TypeData_name(fivmr_MethodRec_result(methodRec)).loadByte();
        switch ((char)exitType) {
        case 'V':
            break;
        case 'J':
            asm.movIR(0,R_AX);
            asm.movIR(0,R_DX);
            break;
        case 'F':
        case 'D':
            asm.movIR(Magic.getVM().add(CType.offsetof("fivmr_VM","zero")).castToInt(),
                      R_AX);
            asm.fldM64(R_AX,0);
            break;
        default:
            asm.movIR(0,R_AX);
            break;
        }
    }
    
    void exitNull() {
        produceNullReturn();
        asm.ret();
    }
    
    public static boolean forceJitSlowpath() {
        return (CType.getInt(Magic.getVM(),"fivmr_VM","flags")
                & CVar.getInt("FIVMR_VMF_FORCE_JIT_SLOWPATH"))!=0;
    }
    
    @Inline
    final boolean monitorsWork() {
        return !Settings.PIP_LOCKING && !forceJitSlowpath();
    }
    
    // problem: the fast paths clobber the refReg, which means that we need a fourth
    // scratch register.  or we need to save the refReg somewhere.  it might seem
    // like a good idea to have the fivmr_Monitor point back to the object, except
    // this doesn't work if the object hasn't been locked yet and fivmr_Monitor*
    // points at the TypeData.
    
    /** Generates the fast path code for Monitors.lock().  The slow-path code (i.e. the
        call) should come after this.  The branches returned should have their targets
        set after the slow path call.  R_EAX gets clobbered, in addition to scratchReg1.
        refReg will be replaced with a pointer to the fivmr_Monitor*. */
    Branch[] monitorEnterFastPath(byte refReg,byte scratchReg1) {
        Branch[] result;
        
        if (Settings.BIASED_LOCKING) {
            result=new Branch[2];
        } else {
            result=new Branch[1];
        }
        
        asm.movMR(refReg, -OMData.objectTDOffset(), refReg);
        if (Settings.HM_POISONED) {
            asm.subIR(1, refReg);
        }
        
        asm.movMR(R_SI, CType.offsetof("fivmr_ThreadState","lockingId").castToInt(),
                  scratchReg1);
        
        // refReg = fivmr_Monitor*, scratchReg1 = lockingId
        
        if (Settings.BIASED_LOCKING) {
            asm.cmpMR(refReg,
                      CType.offsetof("fivmr_Monitor","state").castToInt(),
                      scratchReg1);
            
            Branch notBiased=asm.setBranchSourceAndJccShort(J_NE);
            
            asm.addIR(Pointer.fromInt(1).shl(Monitors.rcShift()).castToInt(),
                      scratchReg1);
            
            // scratchReg1 = monitor->state+(1<<rcShift())
            
            asm.movRM(scratchReg1, refReg,
                      CType.offsetof("fivmr_Monitor","state").castToInt());
            
            result[1]=asm.setBranchSourceAndJmpShort();
            
            asm.setBranchTarget(notBiased);
        }
        
        asm.movIR(Monitors.notHeld().castToInt(), R_AX);
        asm.addIR(Monitors.notHeld().castToInt(), scratchReg1);
        
        // R_EAX = notHeld(), scratchReg1 = notHeld()+lockingId
        
        asm.cmpxchgRM(!Settings.UNIPROCESSOR, scratchReg1, refReg,
                      CType.offsetof("fivmr_Monitor","state").castToInt());
        
        result[0]=asm.setBranchSourceAndJcc(J_E);
        
        return result;
    }
    
    Branch[] monitorExitFastPath(byte refReg, byte scratchReg1) {
        Branch[] result;
        
        if (Settings.BIASED_LOCKING) {
            result=new Branch[2];
        } else {
            result=new Branch[1];
        }
        
        asm.movMR(refReg, -OMData.objectTDOffset(), refReg);
        if (Settings.HM_POISONED) {
            asm.subIR(1, refReg);
        }
        
        asm.movMR(R_SI, CType.offsetof("fivmr_ThreadState","lockingId").castToInt(),
                  R_AX);
        
        // refReg = fivmr_Monitor*, R_EAX = lockingId
        
        if (Settings.BIASED_LOCKING) {
            asm.leaMR(R_AX, Pointer.fromInt(1).shl(Monitors.rcShift()).castToInt(),
                      scratchReg1);
            asm.cmpMR(refReg, CType.offsetof("fivmr_Monitor","state").castToInt(),
                      scratchReg1);
            
            Branch notBiased=asm.setBranchSourceAndJccShort(J_NE);
            
            asm.movRM(R_AX, refReg,
                      CType.offsetof("fivmr_Monitor","state").castToInt());
            
            result[1]=asm.setBranchSourceAndJmpShort();
            
            asm.setBranchTarget(notBiased);
        }
        
        asm.addIR(Monitors.notHeld().castToInt(), R_AX);
        asm.movIR(Monitors.notHeld().castToInt(), scratchReg1);
        
        asm.cmpxchgRM(!Settings.UNIPROCESSOR, scratchReg1, refReg,
                      CType.offsetof("fivmr_Monitor","state").castToInt());
        
        result[0]=asm.setBranchSourceAndJcc(J_E);
        
        return result;
    }
}

