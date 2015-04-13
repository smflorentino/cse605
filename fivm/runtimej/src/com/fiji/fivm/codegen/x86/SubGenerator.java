/*
 * SubGenerator.java
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
import com.fiji.fivm.codegen.*;
import static com.fiji.fivm.codegen.x86.X86Constants.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import com.fiji.fivm.codegen.Assembler.Branch;
import com.fiji.asm.Opcodes;

/**
 * The SubGenerator is used for generating snippets of code that implement
 * specific Java functionality.  It's meant to be reused by both the
 * main code generator (MethodImplGenerator) and everyone else, with an
 * emphasis on maintaining compatibility with MethodImplGenerator's
 * conventions.
 */
public abstract class SubGenerator {
    Pointer methodRec;
    X86Assembler asm;
    DebugIDAllocator dida;
    
    public SubGenerator(Pointer methodRec,
                        X86Assembler asm,
                        DebugIDAllocator dida) {
        this.methodRec=methodRec;
        this.asm=asm;
        this.dida=dida;
    }
    
    public int maxStack() {
        return CType.getChar(methodRec,"fivmr_MethodRec","nStack");
    }
    
    public int maxLocals() {
        return CType.getChar(methodRec,"fivmr_MethodRec","nLocals");
    }
    
    public int methodFlags() {
        return fivmr_MethodRec_flags(methodRec);
    }
    
    public abstract boolean debugIDStored();
    
    public Pointer debugID(int stackDelta) {
        return Pointer.zero();
    }
    
    public int offsetToDebugID() {
        return X86Protocols.OFFSET_FROM_EBP_TO_DEBUG_ID;
    }
    
    public int offsetToUp() {
        return X86Protocols.OFFSET_FROM_EBP_TO_UP;
    }
    
    public abstract int offsetOfEsp();
    
    public boolean prologueOrEpilogue() {
        return false;
    }
    
    public boolean saveReceiver() {
        int flags=fivmr_MethodRec_flags(methodRec);
        return (flags&Constants.MBF_SYNCHRONIZED)!=0
            && (flags&Constants.BF_STATIC)==0;
    }
    
    public int nSavedReceivers() {
        return saveReceiver()?1:0;
    }
    
    public int stackHeight() {
        return -(offsetOfEsp()-fivmr_Baseline_offsetToJStack(methodRec))/4;
    }
    
    public int nRoots() {
        return nSavedReceivers()+maxLocals()+stackHeight();
    }
    
    public int nRoots(int stackDelta) {
        return nRoots()+stackDelta;
    }
    
    public int offsetOfEspFromCaller() {
        return offsetOfEsp()-8;
    }
    
    public int offsetOfStackSlot(int stack) {
        return offsetOfEsp()+stack*4;
    }
    
    public int prepForCallNoDebugID(int nArgs,int stackDelta) {
        int nPop=nArgs;
        // achieve 16-byte alignment (required to make OS X happy)
        // FIXME: maybe optimize this to only use 8-byte (or just 4-byte, so no pushes)
        // alignment on systems that don't need 16-byte
        int nPush=0;
        while (((offsetOfEspFromCaller()-stackDelta*4-nPop*4)&15)!=0) {
            nPush++;
            nPop++;
        }
        asm.pushABunch(nPush);
        return nPop;
    }
    
    public int prepForCall(int nArgs,int stackDelta) {
        if (!debugIDStored()) {
            Pointer did=debugID(stackDelta);
            if (Settings.ASSERTS_ON && did==Pointer.zero()) {
                throw new fivmError("debugID("+stackDelta+") return zero");
            }
            asm.movIM(did.castToInt(), R_BP, offsetToDebugID());
        }
        return prepForCallNoDebugID(nArgs,stackDelta);
    }
    
    public void perpForCall(int nArgs,int stackDelta,int addlPop,ReturnMode rm) {
        if (rm==ReturnMode.NON_TERMINATING) {
            asm.breakpoint();
        } else {
            if (rm.canReturn()) {
                // doing this first preserves the property that the stack poppage
                // does not happen if an exception was thrown.  that means that
                // between prepForCall and perpForCall you can decide to call
                // a completely different procedure with a different number of
                // arguments, so long as that different procedure throws an
                // exception, and so long as you arrange to have the stack aligned
                // for the call to that procedure.
                if (rm.canThrow()) {
                    asm.cmpMI(R_SI,
                              CType.offsetof("fivmr_ThreadState","curException").castToInt(),
                              0);
                    Branch b=asm.setBranchSourceAndJccShort(J_E);
                    if (prologueOrEpilogue()) {
                        asm.jmpAbsM(R_SI, CType.offsetof("fivmr_ThreadState","baselineProEpThrowThunk").castToInt());
                    } else {
                        asm.jmpAbsM(R_SI, CType.offsetof("fivmr_ThreadState","baselineThrowThunk").castToInt());
                    }
                    asm.setBranchTarget(b);
                }
                int nPop=nArgs;
                while (((offsetOfEspFromCaller()-stackDelta*4-nPop*4)&15)!=0) {
                    nPop++;
                }
                asm.popABunch(nPop+addlPop);
            } else {
                // nothing to pop since the exception handler will take care of it
                asm.jmpAbsM(R_SI, CType.offsetof("fivmr_ThreadState","baselineThrowThunk").castToInt());
            }
        }
    }
    
    public boolean storeBarrierWorks() {
        return fivmOptions.needCMStoreBarrier() && !Settings.GC_BLACK_STACK && !Generator.forceJitSlowpath();
    }
    
    public boolean needOutOfLineStoreBarrier(char type) {
        return Types.ref(type) && fivmOptions.needCMStoreBarrier() && !storeBarrierWorks();
    }
    
    public boolean needInLineStoreBarrier(char type) {
        return Types.ref(type) && fivmOptions.needCMStoreBarrier() && storeBarrierWorks();
    }
    
    public void storeBarrier(byte dataReg,
                             byte scratchReg,
                             byte[] saveRegs,
                             int stackDelta,
                             int dataType) {
        if (Settings.ASSERTS_ON && !storeBarrierWorks()) {
            throw new fivmError("bad use of storeBarrier");
        }
        
        if (logLevel>=2) {
            log(SubGenerator.class,2,"Generating inline store barrier.");
        }
        
        Branch done1=null;
        Branch done2=null;
        
        if (!ExtendedTypes.isNonNull(dataType)) {
            asm.testRR(dataReg, dataReg);
            done1=asm.setBranchSourceAndJcc(J_E);
        }
        
        asm.movMR(dataReg, -OMData.objectGCOffset(), scratchReg);
        asm.shrRI(scratchReg, (byte)MM.markBitsShift());

        if (Settings.HFGC_FAIL_FAST_PATHS) {
            asm.cmpMR(R_SI,
                      CType.offsetof("fivmr_ThreadState","gc").add(
                          CType.offsetof("fivmr_GCData","zeroCurShaded")).castToInt(),
                      scratchReg);
            done2=asm.setBranchSourceAndJcc(J_E);
        } else {
            asm.cmpMR(R_SI,
                      CType.offsetof("fivmr_ThreadState","gc").add(
                          CType.offsetof("fivmr_GCData","invCurShaded")).castToInt(),
                      scratchReg);
            done2=asm.setBranchSourceAndJcc(J_NE);
        }
        
        for (int i=0;i<saveRegs.length;++i) {
            asm.pushR(saveRegs[i]);
        }
        stackDelta+=saveRegs.length;
        
        prepForCall(2,stackDelta);
        asm.pushR(dataReg);
        asm.pushR(R_SI);
        asm.call(CVar.addressOf("fivmr_GC_markSlow"));
        perpForCall(2,stackDelta,0,ReturnMode.ONLY_RETURN);

        for (int i=saveRegs.length;i-->0;) {
            asm.popR(saveRegs[i]);
        }
        
        if (done1!=null) {
            asm.setBranchTarget(done1);
        }
        if (done2!=null) {
            asm.setBranchTarget(done2);
        }
    }
    
    public void squirtFieldAccess(int opcode,
                                  Pointer fr,
                                  int recvType,
                                  int dataType) {
        char type=(char)fivmr_TypeData_name(fivmr_FieldRec_type(fr)).loadByte();
        
        if ((opcode==Opcodes.PUTFIELD ||
             opcode==Opcodes.PUTSTATIC) &&
            needOutOfLineStoreBarrier(type)) {
            
            // need to repush the stack ... that's required for
            // stack maps to work right
            if (opcode==Opcodes.PUTFIELD) {
                asm.pushR(R_AX);
            }
            asm.pushR(R_DX);
            
            prepForCall(opcode==Opcodes.PUTFIELD?5:4,0);
            
            asm.pushI32(fivmr_FieldRec_flags(fr));
            asm.pushR(R_DX);
            asm.pushI32(fivmr_FieldRec_barrierArg(fr).castToInt());
            if (opcode==Opcodes.PUTFIELD) {
                asm.pushR(R_AX);
            }
            asm.pushR(R_SI);
            asm.call(opcode==Opcodes.PUTFIELD
                     ? MethodPtrs.objectPutField
                     : MethodPtrs.objectPutStatic);
            
            perpForCall(opcode==Opcodes.PUTFIELD?5:4,
                        0,opcode==Opcodes.PUTFIELD?2:1,
                        ReturnMode.RETURN_OR_THROW);
        } else {
            if ((opcode==Opcodes.PUTFIELD ||
                 opcode==Opcodes.PUTSTATIC) &&
                needInLineStoreBarrier(type)) {
                storeBarrier(R_DX,R_CX,new byte[]{R_AX,R_DX},-3,dataType);
            }
            
            if (Protocols.isInstance(opcode)) {
                int offset=fivmr_FieldRec_offsetFromObj(fr).castToInt();
                
                if (Settings.OM_FRAGMENTED && offset>=OMData.chunkWidth()) {
                    // some assertions
                    if (Settings.ASSERTS_ON) {
                        int size=fivmr_TypeData_size(fivmr_FieldRec_owner(fr));
                        if (offset+Types.bytes(type)>size) {
                            throw new fivmError("field overflows type: "+fieldRecToString(fr));
                        }
                        if (offset<12) {
                            throw new fivmError("field has too small offset: "+fieldRecToString(fr));
                        }
                        if ((offset%32)<4) {
                            throw new fivmError("field collides with header: "+fieldRecToString(fr));
                        }
                    }
                    
                    // correct EAX and offset
                    int nHops=offset>>>OMData.logChunkWidth();
                    offset-=(nHops<<OMData.logChunkWidth());
                                
                    if (nHops<=4) {
                        // straight line code will be smaller in this case
                        // (or not ... need to revisit this)
                        while (nHops-->0) {
                            asm.movMR(R_AX, 0, R_AX); // 2 bytes
                            asm.andIR(-4, R_AX); // 3 bytes
                        }
                    } else {
                        // emit a loop
                        if (Types.cells(type)==2 && opcode==Opcodes.PUTFIELD) {
                            asm.pushR(R_CX);
                        }
                        asm.movIR(nHops, R_CX); // at most 5 bytes
                        Branch b=asm.setBranchTarget();
                        asm.movMR(R_AX, 0, R_AX); // 2 bytes
                        asm.andIR(-4, R_AX); // 3 bytes
                        asm.subIR(1, R_CX); // 3 bytes
                        asm.testRR(R_CX, R_CX); // 2 bytes
                        asm.setBranchSourceAndJcc(b, J_NE); // 6 bytes (?)
                        if (Types.cells(type)==2 && opcode==Opcodes.PUTFIELD) {
                            asm.popR(R_CX);
                        }
                    }
                }
                
                switch (opcode) {
                case Opcodes.PUTFIELD:
                    if (Types.cells(type)==2) {
                        asm.movRM(R_DX, R_AX, offset+0);
                        asm.movRM(R_CX, R_AX, offset+4);
                    } else {
                        asm.movJRM(type, R_DX, R_AX, offset);
                    }
                    break;
                case Opcodes.GETFIELD:
                    asm.pushJM(type, R_AX, offset); // at most 12 bytes for CONTIGUOUS or 6 bytes for FRAGMENTED
                    break;
                default:
                    throw new Error("bad opcode: "+opcode);
                }
            
            } else {
                asm.movIR(fivmr_FieldRec_staticFieldAddress(Magic.getVM(),
                                                            fr).castToInt(),
                          R_AX); // 5 bytes
                switch (opcode) {
                case Opcodes.PUTSTATIC:
                    if (logLevel>=2) {
                        log(MethodImplGenerator.class,2,
                            "Issuing popJM("+type+","+R_AX+")");
                    }
                    if (Types.cells(type)==2) {
                        asm.movRM(R_DX, R_AX, 0);
                        asm.movRM(R_CX, R_AX, 4);
                    } else {
                        asm.movJRM(type, R_DX, R_AX, 0);
                    }
                    break;
                case Opcodes.GETSTATIC:
                    asm.pushJM(type,R_AX); // at most 5 bytes
                    break;
                default:
                    throw new Error("bad opcode: "+opcode);
                }
            }
        }
    }
    
    public void squirtMethodCall(int opcode,
                                 Pointer mr) {
        // what we have:
        // - parameters are set up.
        // - we don't have to worry about the return value
        // - top-most stack location holds the receiver
        //   NOTE: EDX *almost* holds the receiver, if it wasn't for the fact
        //   that the resolver might clobber it...  we could (quite easily)
        //   save EDX in the resolver, but that seems like *effort*.
        
        if (opcode==Opcodes.INVOKESPECIAL) {
            mr=fivmr_MethodRec_reresolveSpecial(Magic.curThreadState(),
                                                fivmr_MethodRec_owner(methodRec),
                                                mr);
        }
        
        switch (opcode) {
        case Opcodes.INVOKESTATIC:
        case Opcodes.INVOKESPECIAL:
            // easy case.  call via indirection on mr's entrypoint field.
            asm.movIR(mr.add(CType.offsetof("fivmr_MethodRec","entrypoint")).castToInt(),
                      R_AX);
            asm.callAbsM(R_AX, 0);
            break;
        case Opcodes.INVOKEVIRTUAL: {
            if ((fivmr_MethodRec_flags(mr)&Constants.MBF_METHOD_KIND)
                ==Constants.MBF_FINAL ||
                (fivmr_TypeData_flags(fivmr_MethodRec_owner(mr))&Constants.TBF_TYPE_KIND)
                ==Constants.TBF_FINAL) {
                // just like static/special
                asm.movIR(mr.add(CType.offsetof("fivmr_MethodRec","entrypoint")).castToInt(),
                          R_AX);
                asm.callAbsM(R_AX, 0);
                break;
            } else {
                // R_EDX has the receiver
                
                asm.movMR(R_DX, -OMData.objectTDOffset(), R_AX);
                if (Settings.HM_POISONED) {
                    asm.subIR(1, R_AX);
                }
            
                // R_EAX now has the monitor
                asm.movMR(R_AX, CType.offsetof("fivmr_Monitor", "forward").castToInt(), R_AX);
            
                // R_EAX now has the typedata, do the call, but lets first make sure everything
                // is a-ok
                Pointer loc=CType.getPointer(mr, "fivmr_MethodRec", "location");
                if (Settings.ASSERTS_ON) {
                    if (loc==Pointer.fromIntSignExtend(-1)) {
                        throw new fivmError("method record's location is -1 "+
                                            "(mr = "+mr.asLong()+", "+methodRecToString(mr)+")");
                    }
                    if (loc.greaterThan(Pointer.fromInt(0x10000000))) {
                        throw new fivmError("method record's location is suspiciously large "+
                                            "(loc = "+loc.asLong()+", mr = "+mr.asLong()+", "+methodRecToString(mr)+")");
                    }
                }
            
                asm.callAbsM(R_AX, 
                             CType.offsetof("fivmr_TypeData","vtable").add(
                                 loc.mul(Pointer.size())).castToInt());
            }
            break;
        }
        case Opcodes.INVOKEINTERFACE:
            // easy!  because the madness is cleverly hidden with awesomeness.
            asm.call(getInterfaceResolutionFor(mr));
            break;
        default:
            throw new Error("invalid opcode: "+opcode);
        }
    }
    
    public boolean allocWorks() {
        // FIXME: this predicate is not quite right; I don't think that this
        // code can handle the CMRGC+OM_FRAGMENTED case.  we should really
        // have something like:
        // ((Settings.OM_CONTIGUOUS && Settings.CMRGC) ||
        //  (Settings.OM_FRAGMENTED && Settings.HFGC))
        // or something...
        return !Settings.SCOPED_MEMORY
            && !Settings.HFGC_ALL_ARRAYLETS // FIXME we could do something about this
            && !Settings.HFGC_FAIL_FAST_PATHS
            && !Settings.FORCE_ARRAYLETS
            && (Settings.CMRGC || Settings.HFGC)
            && (Settings.OM_CONTIGUOUS || Settings.OM_FRAGMENTED)
            && (Settings.HM_NARROW || Settings.HM_POISONED)
            && !Generator.forceJitSlowpath();
    }
    
    public void squirtAllocObject(Pointer td,int stackDelta) {
        if (allocWorks()) {
            if (logLevel>=2) {
                log(SubGenerator.class,2,"Generating inline object allocation.");
            }
        
            int size=OMData.alignRaw(
                Pointer.fromInt(fivmr_TypeData_size(td)),
                MM.requiredSizeAlignment()).castToInt();
            int align=fivmr_TypeData_requiredAlignment(td);
            
            Pointer offsetToGCData=
                CType.offsetof("fivmr_ThreadState","gc");
        
            Pointer offsetToAlloc=
                offsetToGCData.add(
                    CType.offsetof("fivmr_GCData","alloc")).add(
                        CType.sizeof("fivmr_GCSpaceAlloc").mul(Constants.GC_OBJ_SPACE));
        
            asm.movMR(R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","bump")).castToInt(),
                      R_AX);
        
            // align as necessary
            if (align > Pointer.size()) {
                // FIXME: this is retarded for HFGC
                asm.addIR(MM.objectAlignmentOffset()+align-1, R_AX);
                asm.andIR(~(align-1), R_AX);
                asm.subIR(MM.objectAlignmentOffset(), R_AX);
            }
        
            // EAX now has the resulting object location
        
            // compute the new bump
        
            asm.leaMR(R_AX, size, R_DX);
            
            // compute the diff
            
            asm.movRR(R_DX, R_CX);
        
            asm.subMR(R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","start")).castToInt(),
                      R_CX);
        
            // ECX now holds alloc->bump-alloc->start
        
            asm.cmpMR(R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","size")).castToInt(),
                      R_CX);
        
            // status bits now hold result of alloc->size <=> (alloc->bump - alloc->start)
            // use result to jump over slow path.  the fast patch success condition is
            // alloc->size >=_{unsigned} (alloc->bump - alloc->start)
        
            Branch fast=asm.setBranchSourceAndJccShort(J_AE);
        
            prepForCall(4,stackDelta);
        
            asm.pushI32(td.castToInt());
            asm.pushI32(0);
            asm.pushI32(0);
            asm.pushR(R_SI);
            asm.call(MethodPtrs.allocSlow);
        
            perpForCall(4,stackDelta,0,ReturnMode.RETURN_OR_THROW);
            
            Branch done=asm.setBranchSourceAndJmpShort();
            asm.setBranchTarget(fast);
            
            asm.movRM(R_DX,
                      R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","bump")).castToInt());
        
            // stamp GC bits
            if (Settings.NOGC) {
                // this won't happen, but we'll support it, anyway
                asm.movIM(-1, R_AX, -OMData.objectGCOffset());
            } else {
                asm.movMR(R_SI,
                          offsetToGCData.add(
                              CType.offsetof("fivmr_GCData","curShadedAlloc")).castToInt(),
                          R_DX);
                asm.movRM(R_DX, R_AX, -OMData.objectGCOffset());
            }
        
            // stamp TD bits
            asm.movIM((Settings.HM_POISONED?td.add(1):td).castToInt(),
                      R_AX, -OMData.objectTDOffset());
            
            if (Settings.OM_FRAGMENTED) {
                if (size<=128) {
                    for (int i=0;
                         i<(size-1)>>OMData.logChunkWidth();
                         ++i) {
                        asm.leaMR(R_AX, 32*(i+1)+1, R_DX);
                        asm.movRM(R_DX, R_AX, 32*i);
                    }
                } else {
                    // this is cleverly written to use two registers, and to
                    // not clobber R_EAX
                    asm.leaMR(R_AX, 1, R_DX);
                    asm.movIR((size-1)>>OMData.logChunkWidth(), R_CX);
                    Branch b=asm.setBranchTarget();
                    asm.addIR(32, R_DX);
                    asm.movRM(R_DX, R_DX, -33);
                    asm.subIR(1, R_CX);
                    asm.testRR(R_CX, R_CX);
                    asm.setBranchSourceAndJcc(b, J_NE);
                }
            }
            
            asm.setBranchTarget(done);
        } else {
            prepForCall(4,stackDelta);
            asm.pushI32(td.castToInt());
            asm.pushI32(0);
            asm.pushI32(0);
            asm.pushR(R_SI);
            asm.call(MethodPtrs.alloc);
            perpForCall(4,stackDelta,0,ReturnMode.RETURN_OR_THROW);
        }
        
        if (Settings.FINALIZATION_SUPPORTED &&
            (fivmr_TypeData_flags(td)&Constants.TBF_FINALIZABLE)!=0) {
            
            prepForCall(3,stackDelta);
            asm.pushR(R_AX);
            asm.pushI32(0);
            asm.pushR(R_SI);
            asm.call(MethodPtrs.addDestructor);
            perpForCall(3,stackDelta,0,ReturnMode.RETURN_OR_THROW);
        }
        
        asm.pushR(R_AX);
    }
    
    public void squirtAllocArray(Pointer td,int stackDelta) {
        // FIXME: need a clever way of constructing a new debugID from an
        // old one if the only thing that changed is that the stack shrunk.

        asm.popR(R_CX);
        stackDelta--;
        if (allocWorks()) {
            if (logLevel>=2) {
                log(SubGenerator.class,2,"Generating inline array allocation.");
            }
        
            int eleSize=fivmr_TypeData_elementSize(td);
        
            int maxArrayLength=MM.maxArrayLength(Constants.GC_OBJ_SPACE,
                                                 Pointer.fromInt(eleSize));
        
            if (maxArrayLength<Integer.MAX_VALUE) {
                asm.cmpRI(R_CX, maxArrayLength);
                Branch b=asm.setBranchSourceAndJccShort(J_B);
            
                prepForCall(2,stackDelta);
                asm.pushR(R_CX);
                asm.pushR(R_SI);
                asm.call(MethodPtrs.throwOOMEOrNASE);
                perpForCall(2,stackDelta,0,ReturnMode.ONLY_THROW);
            
                asm.setBranchTarget(b);
            } else {
                asm.testRR(R_CX, R_CX);
                Branch b=asm.setBranchSourceAndJccShort(J_GE);
            
                prepForCall(1,stackDelta);
                asm.pushR(R_SI);
                asm.call(MethodPtrs.throwOOME);
                perpForCall(1,stackDelta,0,ReturnMode.ONLY_THROW);
            
                asm.setBranchTarget(b);
            }
        
            Pointer offsetToGCData=
                CType.offsetof("fivmr_ThreadState","gc");
        
            Pointer offsetToAlloc=
                offsetToGCData.add(
                    CType.offsetof("fivmr_GCData","alloc")).add(
                        CType.sizeof("fivmr_GCSpaceAlloc").mul(Constants.GC_OBJ_SPACE));
        
            asm.movMR(R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","bump")).castToInt(),
                      R_AX);
        
            // align as necessary
            if (eleSize > Pointer.size()) {
                // FIXME: this is retarded for HFGC
                asm.addIR(MM.arrayAlignmentOffset()+eleSize-1, R_AX);
                asm.andIR(~(eleSize-1), R_AX);
                asm.subIR(MM.arrayAlignmentOffset(), R_AX);
            }
        
            // EAX now has the resulting object location
        
            // compute the new bump
        
            if (Pointer.fromInt(eleSize).lessThan(MM.requiredSizeAlignment())) {
                asm.leaMSR(R_AX,
                           OMData.alignRaw(Pointer.fromInt(OMData.totalHeaderSize()+4),
                                           Pointer.fromInt(eleSize))
                           .add(MM.requiredSizeAlignment().sub(1))
                           .castToInt(),
                           R_CX,
                           X86Constants.scale(eleSize),
                           R_DX);
                asm.andIR(MM.requiredSizeAlignment().sub(1).not().castToInt(),R_DX);
            } else {
                asm.leaMSR(R_AX,
                           OMData.alignRaw(Pointer.fromInt(OMData.totalHeaderSize()+4),
                                           Pointer.fromInt(eleSize)).castToInt(),
                           R_CX,
                           X86Constants.scale(eleSize),
                           R_DX);
            }
        
            // this is kinda goofy, but it "works".  we opportunistically assume that
            // the fast path is going to be successful and stash EDX into alloc->bump.
            // that allows us to reuse EDX to hold alloc->bump-alloc->start.
        
            asm.movRM(R_DX,
                      R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","bump")).castToInt());
        
            // newBump is now in alloc->bump
        
            asm.subMR(R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","start")).castToInt(),
                      R_DX);
        
            // EDX now holds alloc->bump-alloc->start
        
            asm.cmpMR(R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","size")).castToInt(),
                      R_DX);
        
            // status bits now hold result of alloc->size <=> (alloc->bump - alloc->start)
            // use result to jump over slow path.  the fast patch success condition is
            // alloc->size >=_{unsigned} (alloc->bump - alloc->start)
        
            Branch fast=asm.setBranchSourceAndJccShort(J_AE);
        
            // restore bump.
            // NOTE: this is "sort of" leaky for 64-bit arrays on 32-bit CMR.  except
            // not.  because the current bump context will be simply thrown away anyway.
            asm.movRM(R_AX,
                      R_SI,
                      offsetToAlloc.add(CType.offsetof("fivmr_GCSpaceAlloc","bump")).castToInt());
        
            prepForCall(5,stackDelta);
        
            asm.pushR(R_CX);
            asm.pushI32(td.castToInt());
            asm.pushI32(0);
            asm.pushI32(0);
            asm.pushR(R_SI);
            asm.call(MethodPtrs.allocArraySlow);
        
            perpForCall(5,stackDelta,0,ReturnMode.RETURN_OR_THROW);
        
            Branch done=asm.setBranchSourceAndJmpShort();
            asm.setBranchTarget(fast);
        
            // stamp GC bits
            if (Settings.NOGC) {
                // this won't happen, but we'll support it, anyway
                asm.movIM(-1, R_AX, -OMData.objectGCOffset());
            } else {
                asm.movMR(R_SI,
                          offsetToGCData.add(
                              CType.offsetof("fivmr_GCData","curShadedAlloc")).castToInt(),
                          R_DX);
                asm.movRM(R_DX, R_AX, -OMData.objectGCOffset());
            }
        
            // stamp TD bits
            asm.movIM((Settings.HM_POISONED?td.add(1):td).castToInt(),
                      R_AX, -OMData.objectTDOffset());
        
            // stamp array length
            asm.movRM(R_CX, R_AX, OMData.arrayLengthOffset());
        
            if (Settings.OM_FRAGMENTED) {
                // stamp fragmentation header
                asm.leaMR(R_AX, OMData.arrayLengthOffset()+4, R_DX);
                asm.movRM(R_DX, R_AX, -OMData.objectFHOffset());
            }
        
            asm.setBranchTarget(done);
        } else {
            prepForCall(5,stackDelta);
            asm.pushR(R_CX);
            asm.pushI32(td.castToInt());
            asm.pushI32(0);
            asm.pushI32(0);
            asm.pushR(R_SI);
            asm.call(MethodPtrs.allocArray);
            perpForCall(5,stackDelta,0,ReturnMode.RETURN_OR_THROW);
        }
        asm.pushR(R_AX);
    }
    
    public void squirtInstanceof(Pointer td,
                                 int opcode,
                                 int stackDelta) {
        asm.popR(R_CX);
        
        asm.testRR(R_CX,R_CX);
        Branch nonzero=asm.setBranchSourceAndJccShort(J_NE);
                    
        // both CHECKCAST and INSTANCEOF require pushing 0 if the
        // object is null.
        asm.pushI32(0);
                    
        Branch done=asm.setBranchSourceAndJmpShort();
                    
        asm.setBranchTarget(nonzero);
                    
        asm.movMR(R_SI,
                  CType.offsetof("fivmr_ThreadState","typeEpoch").castToInt(),
                  R_DX);
                    
        asm.mulIR(CType.sizeof("fivmr_TypeEpoch").castToInt(), R_DX, R_AX);
        
        asm.movIR(td.castToInt(), R_AX);
                    
        if (opcode==Opcodes.CHECKCAST) {
            // push the object back on
            asm.pushR(R_CX);
        }
                    
        // R_ECX = object, R_EAX = type b, R_EDX = epoch offset
                    
        asm.movMR(R_CX, -OMData.objectTDOffset(), R_CX);
        if (Settings.HM_POISONED) {
            asm.subIR(1, R_CX);
        }
                    
        asm.movMR(R_CX,
                  CType.offsetof("fivmr_Monitor", "forward").castToInt(),
                  R_CX);

        // R_ECX = type a, R_EAX = type b, R_EDX = epoch offset
                    
        asm.leaMSR(R_AX,
                   CType.offsetof("fivmr_TypeData", "epochs").castToInt(),
                   R_DX,
                   SS_I_1,
                   R_AX);
                    
        // R_ECX = type a, R_EAX = type epoch b, R_EDX = dead
                    
        // the only thing we need from type a is the buckets pointer.
                    
        asm.movMSR(R_CX,
                   CType.offsetof("fivmr_TypeData", "epochs").add(
                       CType.offsetof("fivmr_TypeEpoch", "buckets")).castToInt(),
                   R_DX,
                   SS_I_1,
                   R_CX);
                    
        // R_ECX = buckets of type a, R_EAX = type epoch b, R_EDX = dead
                    
        asm.movzxM16R(R_AX,
                      CType.offsetof("fivmr_TypeEpoch", "bucket").castToInt(),
                      R_DX);
                    
        // R_ECX = buckets of type a, R_EAX = type epoch b, R_EDX = bucket b
                    
        asm.movsxMS8R(R_CX, R_DX, SS_I_1, R_DX);
                    
        // R_ECX = buckets of type a, R_EAX = type epoch b,
        // R_EDX = a.buckets[b.bucket]
                    
        asm.movsxM8R(R_AX,
                     CType.offsetof("fivmr_TypeEpoch", "tid").castToInt(),
                     R_CX);
                    
        // R_ECX = b.tid, R_EDX = a.buckets[b.bucket], R_EAX = dead
                    
        if (opcode==Opcodes.INSTANCEOF) {
            asm.xorRR(R_AX, R_AX);
        }
                    
        asm.cmpRR(R_CX, R_DX);
                    
        if (opcode==Opcodes.CHECKCAST) {
            Branch ok=asm.setBranchSourceAndJccShort(J_E);
            
            prepForCall(1, stackDelta+0);
            asm.pushR(R_SI);
            asm.call(MethodPtrs.throwClassCastRTE);
            perpForCall(1, stackDelta+0, 0, ReturnMode.ONLY_THROW);
                        
            asm.setBranchTarget(ok);
        } else {
            asm.setcc(J_E, R_AX);
            asm.pushR(R_AX);
        }
                    
        asm.setBranchTarget(done);
    }
}


