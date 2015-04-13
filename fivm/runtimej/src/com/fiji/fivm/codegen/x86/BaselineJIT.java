/*
 * BaselineJIT.java
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
import com.fiji.fivm.codegen.*;
import com.fiji.fivm.r1.*;

import static com.fiji.fivm.codegen.x86.X86Constants.*;

public final class BaselineJIT extends com.fiji.fivm.codegen.BaselineJIT {
    static class MyJIT extends JITCodeGen {
        Generator g;
        
        MyJIT(Generator g,Pointer methodRec,int flags) {
            super(methodRec,flags);
            this.g=g;
            g.initSrc(methodRec);
        }
        
        MyJIT(Generator g,Pointer methodRec,int startSize,int flags) {
            super(methodRec,startSize,flags);
            this.g=g;
            g.initSrc(methodRec);
        }
        
        protected int generate(MachineCode code) {
            g.initTrg(code);
            g.generate();
            return g.currentSize();
        }
    }
    
    public MachineCode createLoadThunkFor(final Pointer methodRec) {
        return new MyJIT(
            new LoadThunkGenerator(),
            methodRec,
            64 /* force smaller start size */,
            CVar.getInt("FIVMR_MC_LOAD_THUNK")).getCode();
    }
    
    public MachineCode createCodeFor(final Pointer methodRec) {
        return new MyJIT(
            new EntireMethodImplGenerator(),
            methodRec,
            (CVar.getInt("FIVMR_MC_BASELINE")|
             CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"))).getCode();
    }
    
    public MachineCode createInterfaceResolutionFor(final Pointer methodRec) {
        return new MyJIT(
            new InterfaceResolutionGenerator(),
            methodRec,
            CVar.getInt("FIVMR_MC_INTERFACE_RES")).getCode();
    }
    
    public MachineCode createJNITrampoline(final Pointer methodRec) {
        return new MyJIT(
            new JNITrampolineGenerator(),
            methodRec,
            (CVar.getInt("FIVMR_MC_JNI_TRAMPOLINE")|
             CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"))).getCode();
    }
    
    public MachineCode createCloneHelperFor(final Pointer methodRec) {
        return new MyJIT(
            new CloneHelperGenerator(),
            methodRec,
            (CVar.getInt("FIVMR_MC_CLONE_HELPER")|
             CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"))).getCode();
    }
    
    public MachineCode createExceptionThrowSub(Pointer methodRec,
                                               final ExceptionThrower et) {
        return new MyJIT(
            new Generator(){
                public void generate() {
                    // make sure the stack is aligned for the call
                    asm.andIR(-16,R_SP);
                    asm.pushR(R_AX);
                    asm.pushR(R_AX);
                    
                    code.addPointer(et);
                    asm.pushI32(Pointer.fromObject(et).castToInt());
                    asm.pushR(R_SI);
                    
                    // make the call
                    asm.call(MethodPtrs.throwException);

                    // throw it
                    asm.jmpAbsM(R_SI, CType.offsetof("fivmr_ThreadState","baselineThrowThunk").castToInt());
                }
            },
            methodRec,
            32,
            CVar.getInt("FIVMR_MC_EXC_THROW_SUB")).getCode();
    }
    
    public MachineCode createExceptionThrow(Pointer methodRec,
                                            final ExceptionThrower et) {
        return new MyJIT(
            new Generator(){
                public void generate() {
                    asm.pushR(R_BP);
                    asm.movRR(R_SP, R_BP);
                    asm.pushR(R_SI);
                    asm.movMR(R_BP, 8, R_SI);
                    asm.pushM(R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt());
                    asm.leaMR(R_BP, -12, R_CX);
                    asm.movRM(R_CX, R_SI,
                              CType.offsetof("fivmr_ThreadState","curF").castToInt());
                    // stack is aligned
                    asm.pushI32(code.allocDebugID(0,0,0,new int[0]).castToInt());
                    
                    asm.pushR(R_AX);
        
                    code.addPointer(et);
                    asm.pushI32(Pointer.fromObject(et).castToInt());
                    asm.pushR(R_SI);
                    
                    // make the call
                    asm.call(MethodPtrs.throwException);

                    // throw it
                    asm.jmpAbsM(R_SI, CType.offsetof("fivmr_ThreadState","baselineThrowThunk").castToInt());
                }
            },
            methodRec,
            32,
            (CVar.getInt("FIVMR_MC_EXC_THROW")|
             CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"))).getCode();
    }
    
    public MachineCode createPatchToCodeFor(Pointer methodRec,
                                            int bytecodeStartPC) {
        return new MyJIT(
            new MethodImplSnippetGenerator(bytecodeStartPC),
            methodRec,
            CVar.getInt("FIVMR_MC_BASE_PATCH")).getCode();
    }
    
    public MachineCode createFieldAccess(Pointer methodRec,
                                         final int fat,
                                         final Pointer fr,
                                         final Pointer returnAddr,
                                         final int stackHeight,
                                         final int recvType,
                                         final int dataType) {
        return new MyJIT(
            new Generator(){
                public void generate() {
                    new PatchSubGenerator(methodRec,asm,code,stackHeight,Pointer.zero(),true)
                        .squirtFieldAccess(
                            Protocols.opcodeForFat(fat),
                            fr,
                            recvType,
                            dataType);
                    asm.jmp(returnAddr.add(X86Protocols.OFFSET_FROM_FLD_RA_TO_DONE));
                }
            },
            methodRec,
            32,
            CVar.getInt("FIVMR_MC_FIELD_ACCESS")).getCode();
    }
    
    public boolean fieldAccessPatched(Pointer returnAddr) {
        return returnAddr.add(X86Protocols.OFFSET_FROM_FLD_RA_TO_JMP_ADDR).loadInt()!=0;
    }

    public void patchFieldAccessTo(Pointer returnAddr,
                                   Pointer target) {
        Pointer jmpAddr=returnAddr.add(X86Protocols.OFFSET_FROM_FLD_RA_TO_JMP_ADDR);
        jmpAddr.store(target.sub(jmpAddr.add(4)));
    }
    
    public boolean methodCallPatched(Pointer returnAddr,int mct) {
        if (mct==Constants.MCT_INVOKEINTERFACE) {
            return returnAddr.add(X86Protocols.OFFSET_FROM_IFACE_MTH_RA_TO_CALL_ADDR)
                .loadInt()!=2;
        } else {
            return returnAddr.add(X86Protocols.OFFSET_FROM_MTH_RA_TO_JMP_ADDR).loadInt()!=0;
        }
    }
    
    public void patchMethodCallTo(Pointer returnAddr,
                                  int mct,
                                  Pointer target) {
        if (mct==Constants.MCT_INVOKEINTERFACE) {
            Pointer callAddr=returnAddr.add(X86Protocols.OFFSET_FROM_IFACE_MTH_RA_TO_CALL_ADDR);
            callAddr.store(target.sub(callAddr.add(4)));
        } else {
            Pointer jmpAddr=returnAddr.add(X86Protocols.OFFSET_FROM_MTH_RA_TO_JMP_ADDR);
            jmpAddr.store(target.sub(jmpAddr.add(4)));
        }
    }
    
    public MachineCode createMethodCall(final Pointer methodRec,
                                        final int mct,
                                        final Pointer mr,
                                        final Pointer returnAddr,
                                        final int stackHeight) {
        return new MyJIT(
            new Generator(){
                public void generate() {
                    new PatchSubGenerator(methodRec,asm,code,stackHeight,Pointer.zero(),true)
                        .squirtMethodCall(
                            Protocols.opcodeForMct(mct),
                            mr);
                    asm.jmp(returnAddr.add(X86Protocols.OFFSET_FROM_MTH_RA_TO_PERP));
                }
            },
            methodRec,
            32,
            CVar.getInt("FIVMR_MC_METHOD_CALL")).getCode();
    }
    
    public MachineCode createArrayAlloc(final Pointer methodRec,
                                        final Pointer type,
                                        final Pointer returnAddr,
                                        final int stackHeight,
                                        final Pointer debugID) {
        return new MyJIT(
            new Generator(){
                public void generate() {
                    if (Settings.ASSERTS_ON && debugID==Pointer.zero()) {
                        throw new fivmError("debugID is zero");
                    }
                    new PatchSubGenerator(methodRec,asm,code,stackHeight,debugID,false)
                        .squirtAllocArray(type,0);
                    asm.jmp(returnAddr.add(X86Protocols.OFFSET_FROM_AA_RA_TO_DONE));
                }
            },
            methodRec,
            32,
            CVar.getInt("FIVMR_MC_ARRAY_ALLOC")).getCode();
    }

    public boolean arrayAllocPatched(Pointer returnAddr) {
        return returnAddr.add(X86Protocols.OFFSET_FROM_AA_RA_TO_JMP_ADDR).loadInt()!=0;
    }

    public void patchArrayAllocTo(Pointer returnAddr,
                                  Pointer target) {
        Pointer jmpAddr=returnAddr.add(X86Protocols.OFFSET_FROM_AA_RA_TO_JMP_ADDR);
        jmpAddr.store(target.sub(jmpAddr.add(4)));
    }
    
    public MachineCode createObjectAlloc(final Pointer methodRec,
                                         final Pointer type,
                                         final Pointer returnAddr,
                                         final int stackHeight,
                                         final Pointer debugID) {
        return new MyJIT(
            new Generator(){
                public void generate() {
                    if (Settings.ASSERTS_ON && debugID==Pointer.zero()) {
                        throw new fivmError("debugID is zero");
                    }
                    new PatchSubGenerator(methodRec,asm,code,stackHeight,debugID,false)
                        .squirtAllocObject(type,0);
                    asm.jmp(returnAddr.add(X86Protocols.OFFSET_FROM_OA_RA_TO_DONE));
                }
            },
            methodRec,
            32,
            CVar.getInt("FIVMR_MC_OBJECT_ALLOC")).getCode();
    }

    public boolean objectAllocPatched(Pointer returnAddr) {
        return returnAddr.add(X86Protocols.OFFSET_FROM_OA_RA_TO_JMP_ADDR).loadInt()!=0;
    }

    public void patchObjectAllocTo(Pointer returnAddr,
                                   Pointer target) {
        Pointer jmpAddr=returnAddr.add(X86Protocols.OFFSET_FROM_OA_RA_TO_JMP_ADDR);
        jmpAddr.store(target.sub(jmpAddr.add(4)));
    }

    public MachineCode createInstanceof(final Pointer methodRec,
                                        final int iot,
                                        final Pointer type,
                                        final Pointer returnAddr,
                                        final int stackHeight,
                                        final Pointer debugID) {
        return new MyJIT(
            new Generator(){
                public void generate() {
                    if (Settings.ASSERTS_ON && debugID==Pointer.zero()) {
                        throw new fivmError("debugID is zero");
                    }
                    new PatchSubGenerator(methodRec,asm,code,stackHeight,debugID,false)
                        .squirtInstanceof(type,Protocols.opcodeForIot(iot),0);
                    asm.jmp(returnAddr.add(X86Protocols.OFFSET_FROM_IO_RA_TO_DONE));
                }
            },
            methodRec,
            32,
            CVar.getInt("FIVMR_MC_INSTANCEOF")).getCode();
    }

    public boolean instanceofPatched(Pointer returnAddr) {
        return returnAddr.add(X86Protocols.OFFSET_FROM_IO_RA_TO_JMP_ADDR).loadInt()!=0;
    }

    public void patchInstanceofTo(Pointer returnAddr,
                                  Pointer target) {
        Pointer jmpAddr=returnAddr.add(X86Protocols.OFFSET_FROM_IO_RA_TO_JMP_ADDR);
        jmpAddr.store(target.sub(jmpAddr.add(4)));
    }
}

