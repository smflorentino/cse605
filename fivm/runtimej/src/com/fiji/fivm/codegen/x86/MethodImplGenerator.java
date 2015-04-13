/*
 * MethodImplGenerator.java
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

import com.fiji.util.*;
import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;
import com.fiji.fivm.om.*;
import com.fiji.fivm.codegen.*;
import static com.fiji.fivm.codegen.x86.X86Constants.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import com.fiji.fivm.codegen.Assembler.Branch;
import com.fiji.asm.ClassReader;
import com.fiji.asm.Opcodes;
import com.fiji.asm.Label;
import com.fiji.asm.Type;
import com.fiji.asm.UTF8Sequence;
import java.util.*;

public abstract class MethodImplGenerator extends Generator {
    public void generateForEntireMethod() {
        generatePrologue();
        mbe.extract(instructionCodeGenerator,
                    ClassReader.SKIP_FRAMES);
        if (Settings.ASSERTS_ON && !forwardBranches.isEmpty()) {
            throw new fivmError("forward branches not all cleared");
        }
        
        if (Settings.ASSERTS_ON) {
            // generate a breakpoint in case we fall off the end for some odd
            // reason.
            asm.breakpoint();
        }
    }
    
    MethodBytecodeExtractor mbe;
    SubGenerator sub;
    
    /**
     * Difference between ebp and esp when Java stack is empty.  This will
     * point to the last local.
     */
    int offsetToJStack;
    
    int prologueOrEpilogue=0;
    
    public void initSrc(Pointer methodRec) {
        super.initSrc(methodRec);
        mbe=new MethodBytecodeExtractor(methodRec);
        bta=new BytecodeTypeAnalysis(mbe);
        
        if (Settings.ASSERTS_ON && CType.sizeof("fivmr_Frame")!=Pointer.fromInt(12)) {
            throw new fivmError("bad size of fivmr_Frame, expected 12 bytes");
        }
        
        offsetToJStack=fivmr_Baseline_offsetToJStack(methodRec);
    }
    
    public void initTrg(MachineCode code) {
        super.initTrg(code);
        
        backwardBranches=new HashMap< Integer, Branch >();
        forwardBranches=new MultiArrayMap< Integer, Branch >();
        
        epilogueDebugID=Pointer.zero();
        
        fhc=bta.new ForwardHeavyCalc();
        
        debugIDStored=false;
        bcOffset=0;
        labelHandled=false;
        lineNumber=0;
        
        sub=new SubGenerator(methodRec,asm,code) {
                public boolean debugIDStored() {
                    return debugIDStored;
                }
                public Pointer debugID(int stackDelta) {
                    return Pointer.fromInt(allocDebugID(stackDelta));
                }
                public int offsetOfEsp() {
                    return MethodImplGenerator.this.offsetOfEsp();
                }
                public int stackHeight() {
                    return fhc.stackHeight();
                }
                public boolean prologueOrEpilogue() {
                    if (Settings.ASSERTS_ON && prologueOrEpilogue<0) {
                        throw new fivmError("prologueOrEpilogue got negative value: "+
                                            prologueOrEpilogue);
                    }
                    return prologueOrEpilogue>0;
                }
            };
    }
    
    boolean saveReceiver() {
        return mbe.isSynchronized() && mbe.isInstance();
    }

    int nReceivers() {
        return saveReceiver()?1:0;
    }
    
    int offsetToArg(int argIdx) {
        return 4+4+4*argIdx;
    }
    
    int offsetToFrame() {
        return offsetToDebugID();
    }
    
    int offsetToDebugID() {
        return X86Protocols.OFFSET_FROM_EBP_TO_DEBUG_ID;
    }
    
    int offsetToUp() {
        return X86Protocols.OFFSET_FROM_EBP_TO_UP;
    }
    
    int offsetToSavedReceiver() {
        return -4-4-4-4;
    }
    
    int offsetToLocalReverse(int localReverse) {
        return -4-4-4-4-nReceivers()*4-localReverse*4;
    }
    
    int offsetToLocal(int local) {
        return offsetToLocalReverse(mbe.nLocals()-local-1);
    }
    
    int offsetToEsi() {
        return -4;
    }
    
    /** Indicate that we're beginning generation of prologue/epilogue code. */
    void beginPE() {
        prologueOrEpilogue++;
    }
    
    /** Indicate that we're done with generation of prologue/epilogue code. */
    void endPE() {
        prologueOrEpilogue--;
    }
    
    void generatePreLocalPrologue() {
        beginPE();
        
        asm.pushR(R_BP);
        
        // pushed saved ebp
        
        asm.movRR(R_SP, R_BP); // stack is now 8-byte aligned but 16-byte misaligned
        
        // ebp set up
        
        asm.pushR(R_SI);
        
        // pushed saved esi
        
        if (Settings.ASSERTS_ON && offsetToEsi()!=-4) {
            throw new fivmError("expected offsetToEsi() to be -4");
        }
        
        asm.movMR(R_BP, offsetToArg(0), R_SI);
        
        // esi set up
        
        if (Settings.ASSERTS_ON && offsetToUp()!=-8) {
            throw new fivmError("expected offsetToUp() to be -8");
        }
        
        asm.pushM(R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt());
        
        // pushed fivmr_Frame::up
        
        asm.leaMR(R_BP, offsetToFrame(), R_CX);
        asm.movRM(R_CX, R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt());
        
        // Frame linked into thread state
        
        asm.pushR(R_AX);
        
        // pushed dummy fivmr_Frame::debugID

        endPE();
    }
    
    void generateStackHeightCheck(int stackDelta) {
        beginPE();

        // stack height check
        asm.cmpMR(R_SI, CType.offsetof("fivmr_ThreadState", "stackLimit").castToInt(), R_SP);
        Branch b=asm.setBranchSourceAndJccShort(J_B);
        
        storeDebugID();
        asm.callAbsM(R_SI, CType.offsetof("fivmr_ThreadState","stackHeightSlowBaseline").castToInt());
        
        asm.setBranchTarget(b);

        endPE();
    }
    
    void generateMonitorEnterForPrologue(int stackDelta) {
        beginPE();

        if (mbe.isSynchronized()) {
            if (monitorsWork()) {
                if (mbe.isInstance()) {
                    asm.movMR(R_BP,offsetToSavedReceiver(),R_DX);
                } else {
                    asm.movIR(CType.getPointer(
                                  CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                                  "fivmr_TypeData","classObject").castToInt(),
                              R_DX);
                }
                
                Branch[] bs=monitorEnterFastPath(R_DX,R_CX);
                
                prepForCall(2,stackDelta);
                if (mbe.isInstance()) {
                    asm.pushM(R_BP, offsetToSavedReceiver());
                } else {
                    asm.pushI32(CType.getPointer(
                                    CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                                    "fivmr_TypeData","classObject").castToInt());
                }
                asm.pushR(R_SI);
                asm.call(MethodPtrs.lockSlow);
                perpForCall(2,stackDelta,0,ReturnMode.RETURN_OR_THROW);
                
                for (Branch b : bs) {
                    asm.setBranchTarget(b);
                }
            } else {
                prepForCall(2,stackDelta);
                if (mbe.isInstance()) {
                    asm.pushM(R_BP, offsetToSavedReceiver());
                } else {
                    asm.pushI32(CType.getPointer(
                                    CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                                    "fivmr_TypeData","classObject").castToInt());
                }
                asm.pushR(R_SI);
                asm.call(MethodPtrs.lock);
                perpForCall(2,stackDelta,0,ReturnMode.RETURN_OR_THROW);
            }
        }

        endPE();
    }
    
    void generatePrologue() {
        beginPE();
        
        generatePreLocalPrologue();
        
        if (saveReceiver()) {
            asm.movMR(R_BP, offsetToArg(1), R_DX);
            asm.pushR(R_DX);
            
            // pushed receiver copy, and saved it in R_EDX
        }
        
        // now push locals in reverse
        
        asm.pushABunch(maxLocals()-mbe.nArgs());
        
        // non-arg locals pushed
        
        for (int i=mbe.nArgs();i-->0;) {
            if (i==0 && saveReceiver()) {
                asm.pushR(R_DX);
            } else {
                asm.pushM(R_BP, offsetToArg(1+i));
            }
        }
        
        // locals completely set up -- so we're at offset to JStack
        
        generateStackHeightCheck(0);
        generateMonitorEnterForPrologue(0);
        
        endPE();
    }
    
    void generateEpilogueWithoutMonitorExit() {
        asm.movMR(R_BP, offsetToUp(), R_CX);
        asm.movRM(R_CX, R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt());
        asm.movMR(R_BP, offsetToEsi(), R_SI);
        asm.retleave();
    }
    
    // FIXME: what if the epilogue popped the return?  that would make the implementation
    // of epilogues simpler in the case of synchronized methods.  and probably a bit
    // faster.
    void generateEpilogue(int stackDelta) {
        beginPE();
        
        if (mbe.isSynchronized()) {
            if (epilogueDebugID==Pointer.zero()) {
                int[] refs=new int[1];
                int nRefs=1;
                refs[0]=1;
                if (Types.ref(TypeParsing.getMethodReturn(mbe.descriptor()).charAt(0))) {
                    nRefs++;
                    refs[0]|=2;
                }
                
                epilogueDebugID=code.allocDebugID(0,0,nRefs,refs);
            }
            
            asm.movIM(epilogueDebugID.castToInt(),
                      R_BP, offsetToDebugID());
            
            // we save the return value in the slot right after the receiver;
            // it would be the location of Local0 even if the method has no locals.
            
            // NB if a static synchronized method returns a reference, then there
            // is the concern that it otherwise had no locals or stack.  But this
            // cannot be - to return something the method would have to have a
            // maxStack>=1.

            switch (Types.toExec(TypeParsing.getMethodReturn(mbe.descriptor()).charAt(0))) {
            case 'I':
            case 'L':
                asm.movRM(R_AX, R_BP, offsetToLocalReverse(0));
                break;
            case 'J':
                asm.movRM(R_AX, R_BP, offsetToLocalReverse(1));
                asm.movRM(R_DX, R_BP, offsetToLocalReverse(0));
                break;
            case 'F':
                asm.fstpM32(R_BP, offsetToLocalReverse(0));
                break;
            case 'D':
                asm.fstpM64(R_BP, offsetToLocalReverse(1));
                break;
            case 'V':
                break;
            default:
                if (Settings.ASSERTS_ON) {
                    throw new fivmError("bad return type: "+mbe.descriptor());
                }
                Magic.notReached();
            }
            
            if (monitorsWork()) {
                if (mbe.isInstance()) {
                    asm.movMR(R_BP, offsetToSavedReceiver(), R_DX);
                } else {
                    asm.movIR(CType.getPointer(
                                  CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                                  "fivmr_TypeData","classObject").castToInt(),
                              R_DX);
                }
            
                Branch[] bs=monitorExitFastPath(R_DX,R_CX);
            
                prepForCallNoDebugID(2,stackDelta);
            
                if (mbe.isInstance()) {
                    asm.pushM(R_BP, offsetToSavedReceiver());
                } else {
                    asm.pushI32(CType.getPointer(
                                    CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                                    "fivmr_TypeData","classObject").castToInt());
                }
                asm.pushR(R_SI);
                asm.call(MethodPtrs.unlockSlow);
                perpForCall(2,stackDelta,0,ReturnMode.RETURN_OR_THROW);
            
                for (Branch b : bs) {
                    asm.setBranchTarget(b);
                }
            } else {
                prepForCallNoDebugID(2,stackDelta);
            
                if (mbe.isInstance()) {
                    asm.pushM(R_BP, offsetToSavedReceiver());
                } else {
                    asm.pushI32(CType.getPointer(
                                    CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                                    "fivmr_TypeData","classObject").castToInt());
                }
                asm.pushR(R_SI);
                asm.call(MethodPtrs.unlock);
                perpForCall(2,stackDelta,0,ReturnMode.RETURN_OR_THROW);
            }
            
            // recover the return
            switch (Types.toExec(TypeParsing.getMethodReturn(mbe.descriptor()).charAt(0))) {
            case 'I':
            case 'L':
                asm.movMR(R_BP, offsetToLocalReverse(0), R_AX);
                break;
            case 'J':
                asm.movMR(R_BP, offsetToLocalReverse(1), R_AX);
                asm.movMR(R_BP, offsetToLocalReverse(0), R_DX);
                break;
            case 'F':
                asm.fldM32(R_BP, offsetToLocalReverse(0));
                break;
            case 'D':
                asm.fldM64(R_BP, offsetToLocalReverse(1));
                break;
            case 'V':
                break;
            default:
                if (Settings.ASSERTS_ON) {
                    throw new fivmError("bad return type: "+mbe.descriptor());
                }
                Magic.notReached();
            }
        }
        generateEpilogueWithoutMonitorExit();
        
        endPE();
    }
    
    BytecodeTypeAnalysis bta;
    BytecodeTypeAnalysis.ForwardHeavyCalc fhc;

    int bcOffset;

    int lineNumber;
    
    /** Maps bytecode offsets that we've already seen to Branch objects that
        can be used with the assembler for backward branches. */
    HashMap< Integer, Branch > backwardBranches;
    
    /** Maps bytecode offsets that we haven't seen yet to lists of Branch objects
        that should be patched once that bytecode offset is encountered; this can
        be used for forward branches. */
    MultiArrayMap< Integer, Branch > forwardBranches;

    Pointer epilogueDebugID;
    
    int offsetOfEsp() {
        return offsetToJStack-fhc.stackHeight()*4;
    }
    
    int offsetOfEspFromCaller() {
        return offsetOfEsp()-8;
    }
    
    int offsetOfStackSlotAbs(int stack) {
        return offsetToJStack-4-stack*4;
    }
    
    int offsetOfStackSlot(int stack) {
        return offsetToJStack-4-(fhc.stackHeight()-stack-1)*4;
    }
    
    // NOTE: this returns int, but the int should really be a Pointer.  doing that
    // because I'm always going to use this value as an assembler immediate, and the
    // assembler is going to want an int.
    int allocDebugID(int stackDelta) {
        int nRefs=nReceivers()+maxLocals()+fhc.stackHeight();
        int[] refs=IntUtil.newBitSet(nRefs);
        int i=0;
        if (saveReceiver()) {
            IntUtil.setBit(refs,i++);
        }
        for (int j=0;j<maxLocals();++j) {
            if (fhc.localAtIsRef(j)) {
                IntUtil.setBit(refs,i+maxLocals()-j-1);
            }
        }
        i+=maxLocals();
        for (int j=0;j<fhc.stackHeight()+(stackDelta>0?0:stackDelta);++j,i++) {
            if (ExtendedTypes.isRef(fhc.stackAtAbsolute(j))) {
                IntUtil.setBit(refs,i);
            }
        }
        return code.allocDebugID(bcOffset,lineNumber,nRefs,refs).castToInt();
    }
    
    int allocDebugID() {
        return allocDebugID(0);
    }
    
    boolean debugIDStored=false;
    boolean labelHandled=false;
    
    /**
     * If we have not yet stored the debug ID for this instruction, emits code
     * to store the debug ID into the current fivmr_Frame::id field.  This
     * method is designed to be safe for use inside conditionals -- so even though
     * it stores the debug ID, it does not set the flag indicating that it had
     * been stored, in case the code you're emitting has other paths that may
     * also need to store the debug ID.  What this means is that if the debug ID
     * had not been stored, then future calls to either storeDebugID() or
     * storeDebugIDForInstruction() will also emit code to store the debug ID.
     */
    void storeDebugID(int stackDelta) {
        if (!debugIDStored || stackDelta!=0) {
            asm.movIM(allocDebugID(stackDelta), R_BP, offsetToDebugID());
        }
    }
    
    void storeDebugID() {
        storeDebugID(0);
    }
    
    /**
     * If we have not yet stored the debug ID for this instructions, emits
     * code to store the debug ID and then marks the debug ID as stored.  That
     * way, all future calls to either storeDebugIDForInstruction() or
     * storeDebugID() will end up doing nothing.  Calling this is useful if
     * you know that every path through the code for the instructoin you're
     * operating one will require the debug ID to be stored.
     */
    void storeDebugIDForInstruction() {
        storeDebugID(0);
        debugIDStored=true;
    }
    
    void jumpTo(int bcOffset) {
        Branch b=backwardBranches.get(bcOffset);
        if (b!=null) {
            asm.setBranchSourceAndJmp(b);
        } else {
            forwardBranches.add(bcOffset,asm.setBranchSourceAndJmp());
        }
    }
    
    void jccTo(byte condition,
               int bcOffset) {
        Branch b=backwardBranches.get(bcOffset);
        if (b!=null) {
            asm.setBranchSourceAndJcc(b,condition);
        } else {
            forwardBranches.add(bcOffset,asm.setBranchSourceAndJcc(condition));
        }
    }
    
    int prepForCallNoDebugID(int nArgs,int stackDelta) {
        return sub.prepForCallNoDebugID(nArgs,stackDelta);
    }
    
    int prepForCall(int nArgs,int stackDelta) {
        storeDebugID(stackDelta);
        return prepForCallNoDebugID(nArgs,stackDelta);
    }

    void perpForCall(int nArgs,int stackDelta,int addlPop,ReturnMode rm) {
        sub.perpForCall(nArgs,stackDelta,addlPop,rm);
    }
    
    void perpForCall(int nArgs,int stackDelta,ReturnMode rm) {
        perpForCall(nArgs,stackDelta,0,rm);
    }
    
    void nullCheckAfterCmp(int stackDelta) {
        Branch b=asm.setBranchSourceAndJccShort(J_NE);
        storeDebugID();
        asm.jmpAbsM(R_SI,
                    CType.offsetof("fivmr_ThreadState","nullCheckSlowBaseline").castToInt());
        asm.setBranchTarget(b);
    }

    void nullCheck(byte reg,int stackDelta) {
        asm.testRR(reg,reg);
        nullCheckAfterCmp(stackDelta);
    }
    
    void throwABC() {
        storeDebugID();
        asm.jmpAbsM(R_SI,
                    CType.offsetof("fivmr_ThreadState","abcSlowBaseline").castToInt());
    }
    
    boolean inited(Pointer type) {
        return CType.getInt(type,"fivmr_TypeStub","inited")==1
            // the point of this case is for when we're compiling static inits ... we
            // don't want the static init to try to recursively static init, because
            // although that would be harmless, it would also be adorably retarded.
            || fivmr_TypeStub_tryGetTypeData(type)
            == CType.getPointer(methodRec,"fivmr_MethodRec","owner");
    }
    
    void staticInit(Pointer stub,int stackDelta) {
        if (!inited(stub)) {
            asm.cmpM8I(stub.add(CType.offsetof("fivmr_TypeStub","inited")), (byte)0);
            Branch b=asm.setBranchSourceAndJccShort(J_NE);
            prepForCall(2,stackDelta);
            asm.pushI32(stub.castToInt());
            asm.pushR(R_SI);
            asm.call(CVar.addressOf("fivmr_TypeStub_checkInit"));
            perpForCall(2,stackDelta,ReturnMode.RETURN_OR_THROW);
            asm.setBranchTarget(b);
        }
    }
    
    /** Clobbers idxReg and scratch; makes arrReg point to the array element. */
    void genArrayletSlow(byte idxReg,byte arrReg,byte scratch,char type) {
        asm.movMR(arrReg, 0, arrReg);
        
        // arrReg now points at spine
        
        // second array bounds check
        asm.cmpMR(arrReg, OMData.spineArrayLengthOffset(), idxReg);
        Branch doAccess=asm.setBranchSourceAndJccShort(J_A);
        
        throwABC();
        
        asm.setBranchTarget(doAccess);
        
        // arrReg still holds spine
        
        byte idxReg2=scratch;
        
        asm.movRR(idxReg, idxReg2);
        asm.shrRI(idxReg, (byte)(OMData.logChunkWidth()-Types.logBytes(type)));
        
        // idxReg now holds chunk index, idxReg2 holds the original index
        
        asm.movMSR(arrReg, idxReg, SS_I_4, arrReg);
        
        // arrReg now points to the first element in the chunk
        
        asm.shlRI(idxReg2, (byte)Types.logBytes(type));
        asm.andIR(OMData.chunkWidth()-1, idxReg2);
        
        // idxReg2 now holds in-chunk element offset
        
        asm.subRR(idxReg2, arrReg);
    }
    
    void arrayLoad(char type) {
        byte idxReg=R_AX;
        byte arrReg=R_CX;
        
        asm.popR(idxReg);
        asm.popR(arrReg);
        
        // do a null check
        if (!fhc.stackAtIsNonNull(1)) {
            nullCheck(arrReg,-2);
        }
        
        // perform first array bounds check
        asm.cmpMR(arrReg, OMData.arrayLengthOffset(), idxReg);
        Branch fastAccess=asm.setBranchSourceAndJccShort(J_A);
        
        Branch done=null;
        if (Settings.OM_CONTIGUOUS) {
            throwABC();
        } else if (Settings.OM_FRAGMENTED) {
            genArrayletSlow(idxReg,arrReg,R_DX,type);
            
            // arrReg now points to the element
            
            asm.pushJM(type,arrReg);
            
            done=asm.setBranchSourceAndJmpShort();
        } else {
            Magic.notReached();
            return;
        }
        
        asm.setBranchTarget(fastAccess);
        
        // code for fast access
        asm.pushJMS(type,
                    arrReg,
                    OMData.arrayPayloadOffset(Pointer.fromInt(Types.bytes(type))),
                    idxReg);
            
        if (Settings.OM_FRAGMENTED) {
            asm.setBranchTarget(done);
        }
    }
    
    void arrayStore(char type) {
        byte idxReg=R_AX;
        byte arrReg=R_CX;
        byte dataReg=R_DX;
        
        int valCells=Types.cells(type);
        
        int stackDelta=0;
        
        // NOTE: ref array stores always use the barrier because we need the nasty
        // subtype test anyway
        
        boolean ref=Types.ref(type);
        
        boolean popped=valCells==1 && (Settings.OM_CONTIGUOUS || ref);

        if (!popped) {
            asm.movMR(R_BP, offsetOfStackSlot(valCells+0), idxReg);
            asm.movMR(R_BP, offsetOfStackSlot(valCells+1), arrReg);
        } else {
            asm.popR(dataReg);
            asm.popR(idxReg);
            asm.popR(arrReg);
            stackDelta=-3;
        }
        
        if (ref) {
            prepForCall(4,-3);
            asm.pushR(dataReg);
            asm.pushR(idxReg);
            asm.pushR(arrReg);
            asm.pushR(R_SI);
            asm.call(MethodPtrs.objectArrayStore);
            perpForCall(4,-3,ReturnMode.RETURN_OR_THROW);
        } else {
            // do a null check
            if (!fhc.stackAtIsNonNull(valCells+1)) {
                nullCheck(arrReg,stackDelta);
            }
            
            // perform first array bounds check
            asm.cmpMR(arrReg, OMData.arrayLengthOffset(), idxReg);
            Branch fastAccess=asm.setBranchSourceAndJccShort(J_A);
            
            Branch done=null;
            if (Settings.OM_CONTIGUOUS) {
                throwABC();
            } else if (Settings.OM_FRAGMENTED) {
                genArrayletSlow(idxReg,arrReg,R_DX,type);
                
                // arrReg now points to the element
                
                if (valCells==1) {
                    asm.popR(dataReg);
                    asm.movJRM(type, dataReg, arrReg, 0);
                } else {
                    asm.popR(dataReg);
                    asm.movRM(dataReg, arrReg, 0);
                    asm.popR(dataReg);
                    asm.movRM(dataReg, arrReg, 4);
                }
                
                done=asm.setBranchSourceAndJmpShort();
            } else {
                Magic.notReached();
                return;
            }
            
            asm.setBranchTarget(fastAccess);
            
            // code for fast access
            if (valCells==1) {
                if (!popped) {
                    asm.popR(dataReg);
                }
                asm.movJRMS(type, dataReg, arrReg,
                            OMData.arrayPayloadOffset(Pointer.fromInt(Types.bytes(type))),
                            idxReg);
            } else {
                asm.popR(dataReg);
                asm.movRMS(dataReg, arrReg,
                           OMData.arrayPayloadOffset(Pointer.fromInt(Types.bytes(type)))+0,
                           idxReg, SS_I_8);
                asm.popR(dataReg);
                asm.movRMS(dataReg, arrReg,
                           OMData.arrayPayloadOffset(Pointer.fromInt(Types.bytes(type)))+4,
                           idxReg, SS_I_8);
            }
            
            if (Settings.OM_FRAGMENTED) {
                asm.setBranchTarget(done);
            }
            
            if (!popped) {
                asm.popR(R_AX);
                asm.popR(R_AX);
            }
        }
    }

    // FIXME: get rid of all uses of staticInit and getType ... whenever we would
    // have used this, use patching instead.
    
    // either returns the type, or puts it into EAX
    Pointer staticInitAndGetType(Pointer stub,int stackDelta) {
        if (CType.getInt(stub,"fivmr_TypeStub","inited")==1) {
            Pointer result=fivmr_TypeStub_tryGetTypeData(stub);
            if (Settings.ASSERTS_ON && result==Pointer.zero()) {
                throw new fivmError("type stub "+stub.asLong()+" initialized but could not "+
                                    "get type data");
            }
            return result;
        } else {
            asm.cmpM8I(stub.add(CType.offsetof("fivmr_TypeStub","inited")), (byte)0);

            Branch b1=asm.setBranchSourceAndJccShort(J_NE);

            prepForCall(2,stackDelta);
            asm.pushI32(stub.castToInt());
            asm.pushR(R_SI);
            asm.call(CVar.addressOf("fivmr_TypeStub_checkInit"));
            perpForCall(2,stackDelta,ReturnMode.RETURN_OR_THROW);
            
            // eax now hold the type
            
            Branch b2=asm.setBranchSourceAndJmpShort();
            
            asm.setBranchTarget(b1);
            
            // if it's inited, then forward points to the type, so load it
            asm.movIR(stub.add(CType.offsetof("fivmr_TypeData","forward")).castToInt(),
                      R_AX);
            asm.movMR(R_AX, 0, R_AX);
            
            asm.setBranchTarget(b2);
            
            return Pointer.zero();
        }
    }
    
    // returns zero if the type is not yet resolved, in which case it emits code
    // to put the type into result.  otherwise it returns the type and generates no
    // code
    Pointer getType(Pointer stub, int stackDelta, byte result, boolean saveRegs) {
        Pointer type=fivmr_TypeStub_tryGetTypeData(stub);
        
        if (type!=Pointer.zero()) {
            try {
                resolveType(type);
            } catch (LinkageError e) {
                type=Pointer.zero(); // force this to happen lazily
            }
        }
        
        if (type==Pointer.zero()) {
            asm.movIR(stub.castToInt(),result);
            asm.btIM(Constants.TBF_RESOLUTION_DONE_BIT,
                     result,
                     CType.offsetof("fivmr_TypeStub","flags").castToInt());
            
            Branch resolve=asm.setBranchSourceAndJccShort(J_NC);
            
            asm.movMR(result,
                      CType.offsetof("fivmr_TypeStub","forward").castToInt(),
                      result);
            
            Branch done=asm.setBranchSourceAndJmpShort();
                        
            asm.setBranchTarget(resolve);
            
            if (saveRegs) {
                if (result!=R_AX) {
                    asm.pushR(R_AX);
                    stackDelta++;
                }
                if (result!=R_CX) {
                    asm.pushR(R_CX);
                    stackDelta++;
                }
                if (result!=R_DX) {
                    asm.pushR(R_DX);
                    stackDelta++;
                }
            }
            
            prepForCall(2,stackDelta);
            asm.pushR(result);
            asm.pushR(R_SI);
            asm.call(CVar.addressOf("fivmr_TypeStub_resolve"));
            perpForCall(2,stackDelta,ReturnMode.RETURN_OR_THROW);
            
            if (result!=R_AX) {
                asm.movRR(R_AX, result);
            }
                        
            if (saveRegs) {
                if (result!=R_DX) {
                    asm.popR(R_DX);
                }
                if (result!=R_CX) {
                    asm.popR(R_CX);
                }
                if (result!=R_AX) {
                    asm.popR(R_AX);
                }
            }
            
            asm.setBranchTarget(done);
        }
        
        return type;
    }
    
    Pointer getType(Pointer stub) {
        return getType(stub, 0, R_AX, false);
    }
    
    // generates code to put the type into result
    void getTypeEasy(Pointer stub, int stackDelta, byte result, boolean saveRegs) {
        Pointer type=getType(stub, stackDelta, result, saveRegs);
        if (type!=Pointer.zero()) {
            asm.movIR(type.castToInt(),result);
        }
    }
    
    void getTypeEasy(Pointer stub) {
        getTypeEasy(stub, 0, R_AX, false);
    }
    
    static final class LookupRange extends IntTuple {
        public final Branch branch;
    
        public LookupRange(int a,int b,Branch branch) {
            super(a,b);
            this.branch=branch;
        }
        
        public LookupRange(int a,int b) {
            this(a,b,null);
        }
        
        public LookupRange(IntTuple it,Branch branch) {
            this(it.a,it.b,branch);
        }
        
        public LookupRange(IntTuple it) {
            this(it.a,it.b);
        }
    
        public boolean hasBranch() { return branch!=null; }
        public Branch branch() { return branch; }
    
        public boolean equals(Object other_) {
            if (this==other_) return true;
            if (!(other_ instanceof LookupRange)) return false;
            LookupRange other=(LookupRange)other_;
            return a==other.a
                && b==other.b
                && branch==other.branch;
        }
    
        public String toString() {
            return "LookupRange["+a+", "+b+"]";
        }
    }

    final EmptyMethodVisitor instructionCodeGenerator=new EmptyMethodVisitor() {
            @Override
            public void visitBCOffset(int bcOffset) {
                MethodImplGenerator.this.debugIDStored=false;
                MethodImplGenerator.this.bcOffset=bcOffset;
                MethodImplGenerator.this.labelHandled=false;
                code.clearDebugIDHash();
                fhc.visitBCOffset(bcOffset);
            }
            
            @Override
            public void visitLabel(Label l) {
                fhc.visitLabel(l);
                // NOTE: we currently ignore debug labels
                if ((l.getStatus()&Label.DEBUG)==0 &&
                    !labelHandled) {
                    backwardBranches.put(l.getOffsetWorks(),asm.setBranchTarget());
                    ArrayList< Branch > forwards=forwardBranches.remove(l.getOffsetWorks());
                    if (forwards!=null) {
                        for (Branch b : forwards) {
                            asm.setBranchTarget(b);
                        }
                    }
                    
                    code.addBasepoint(l.getOffsetWorks(),
                                      fhc.stackHeight(),
                                      asm.getAbsolutePC());
                    
                    storeDebugIDForInstruction();
                    
                    asm.cmpMI(R_SI,
                              CType.offsetof("fivmr_ThreadState","pollingUnion").castToInt(),
                              0);
                    Branch b=asm.setBranchSourceAndJccShort(J_NE);
                    
                    asm.callAbsM(
                        R_SI,
                        CType.offsetof("fivmr_ThreadState","pollcheckSlowBaseline").castToInt());
                    
                    asm.setBranchTarget(b);
                    
                    labelHandled=true;
                }
            }
            
            @Override
            public void visitLineNumber(int line, Label start) {
                lineNumber=line;
                debugIDStored=false; // because visitLabel comes before this call
            }
            
            @Override
            public void visitTryCatchBlock(Label start,
                                           Label end,
                                           Label handler,
                                           UTF8Sequence typeName) {
                Pointer stub;
                if (typeName==null) {
                    stub=Pointer.zero();
                } else {
                    stub=findStubClass(mbe.typeContext(), typeName);
                }
                code.addBaseTryCatch(start.getOffsetWorks(),
                                     end.getOffsetWorks(),
                                     handler.getOffsetWorks(),
                                     stub);
            }

            @Override
            public void visitInsn(int opcode) {
                switch (opcode) {
                case Opcodes.NOP:
                    // well, this one's easy. :-)
                    break;
                case Opcodes.ACONST_NULL:
                case Opcodes.ICONST_0:
                case Opcodes.FCONST_0:
                    asm.pushI32(0);
                    break;
                case Opcodes.LCONST_0:
                case Opcodes.DCONST_0:
                    asm.pushI32(0);
                    asm.pushI32(0);
                    break;
                case Opcodes.ICONST_M1:
                    asm.pushI32(-1);
                    break;
                case Opcodes.ICONST_1:
                    asm.pushI32(1);
                    break;
                case Opcodes.ICONST_2:
                    asm.pushI32(2);
                    break;
                case Opcodes.ICONST_3:
                    asm.pushI32(3);
                    break;
                case Opcodes.ICONST_4:
                    asm.pushI32(4);
                    break;
                case Opcodes.ICONST_5:
                    asm.pushI32(5);
                    break;
                case Opcodes.LCONST_1:
                    asm.pushI32(0);
                    asm.pushI32(1);
                    break;
                case Opcodes.FCONST_1:
                    asm.pushI32(Float.floatToRawIntBits(1.0f));
                    break;
                case Opcodes.FCONST_2:
                    asm.pushI32(Float.floatToRawIntBits(2.0f));
                    break;
                case Opcodes.DCONST_1: {
                    long bits=Double.doubleToRawLongBits(1.0);
                    asm.pushI32((int)((bits>>>32)&0xFFFFFFFFl));
                    asm.pushI32((int)(bits&0xFFFFFFFFl));
                    break;
                }
                case Opcodes.IALOAD:
                    arrayLoad('I');
                    break;
                case Opcodes.LALOAD:
                    arrayLoad('J');
                    break;
                case Opcodes.FALOAD:
                    arrayLoad('F');
                    break;
                case Opcodes.DALOAD:
                    arrayLoad('D');
                    break;
                case Opcodes.AALOAD:
                    arrayLoad('L');
                    break;
                case Opcodes.BALOAD:
                    arrayLoad('B');
                    break;
                case Opcodes.CALOAD:
                    arrayLoad('C');
                    break;
                case Opcodes.SALOAD:
                    arrayLoad('S');
                    break;
                case Opcodes.IASTORE:
                    arrayStore('I');
                    break;
                case Opcodes.LASTORE:
                    arrayStore('J');
                    break;
                case Opcodes.FASTORE:
                    arrayStore('F');
                    break;
                case Opcodes.DASTORE:
                    arrayStore('D');
                    break;
                case Opcodes.AASTORE:
                    arrayStore('L');
                    break;
                case Opcodes.BASTORE:
                    arrayStore('B');
                    break;
                case Opcodes.CASTORE:
                    arrayStore('C');
                    break;
                case Opcodes.SASTORE:
                    arrayStore('S');
                    break;
                case Opcodes.POP:
                    asm.popABunch(1);
                    break;
                case Opcodes.POP2:
                    asm.popABunch(2);
                    break;
                case Opcodes.DUP:
                    // FIXME: this might work better as a pushM
                    asm.popR(R_AX);
                    asm.pushR(R_AX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.DUP_X1:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.pushR(R_AX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.DUP_X2:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.popR(R_CX);
                    asm.pushR(R_AX);
                    asm.pushR(R_CX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.DUP2:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.DUP2_X1:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.popR(R_CX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    asm.pushR(R_CX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.DUP2_X2:
                    // this sucks but I don't care
                    
                    // 4 3 2 1 -> 2 1 4 3 2 1
                    
                    // swap 4 & 2
                    asm.movMR(R_BP, offsetOfStackSlot(3), R_AX);
                    asm.movMR(R_BP, offsetOfStackSlot(1), R_CX);
                    asm.movRM(R_AX, R_BP, offsetOfStackSlot(1));
                    asm.movRM(R_CX, R_BP, offsetOfStackSlot(3));
                    
                    // swap 3 & 1
                    asm.movMR(R_BP, offsetOfStackSlot(2), R_AX);
                    asm.movMR(R_BP, offsetOfStackSlot(0), R_DX);
                    asm.movRM(R_AX, R_BP, offsetOfStackSlot(0));
                    asm.movRM(R_DX, R_BP, offsetOfStackSlot(2));
                    
                    // 2 1 4 3 -> 2 1 4 3 2 1
                    // EDX = 1
                    // ECX = 2
                    
                    // push 2 & 1
                    asm.pushR(R_CX);
                    asm.pushR(R_DX);
                    break;
                case Opcodes.SWAP:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.pushR(R_AX);
                    asm.pushR(R_DX);
                    break;
                case Opcodes.IADD:
                    asm.popR(R_AX);
                    asm.addRM(R_AX, R_BP, offsetOfStackSlot(1));
                    break;
                case Opcodes.LADD:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.addRM(R_AX, R_BP, offsetOfStackSlot(2));
                    asm.adcRM(R_DX, R_BP, offsetOfStackSlot(3));
                    break;
                case Opcodes.FADD:
                    asm.fldM32(R_SP);
                    asm.addIR(4, R_SP);
                    asm.faddM32(R_SP);
                    asm.fstpM32(R_SP);
                    break;
                case Opcodes.DADD:
                    asm.fldM64(R_SP);
                    asm.addIR(8, R_SP);
                    asm.faddM64(R_SP);
                    asm.fstpM64(R_SP);
                    break;
                case Opcodes.ISUB:
                    asm.popR(R_AX);
                    asm.subRM(R_AX, R_BP, offsetOfStackSlot(1));
                    break;
                case Opcodes.LSUB:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.subRM(R_AX, R_BP, offsetOfStackSlot(2));
                    asm.sbbRM(R_DX, R_BP, offsetOfStackSlot(3));
                    break;
                case Opcodes.FSUB:
                    asm.fldM32(R_SP, 4);
                    asm.fsubM32(R_SP);
                    asm.addIR(4, R_SP);
                    asm.fstpM32(R_SP);
                    break;
                case Opcodes.DSUB:
                    asm.fldM64(R_SP, 8);
                    asm.fsubM64(R_SP);
                    asm.addIR(8, R_SP);
                    asm.fstpM64(R_SP);
                    break;
                case Opcodes.IMUL:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.mulRR(R_AX,R_DX);
                    asm.pushR(R_DX);
                    break;
                case Opcodes.LMUL:
                    // fun hack: we can make the call on the stack because multiplication
                    // is commutative.  also we know that the helper is super simple,
                    // so no need to align the stack.
                    asm.call(CVar.addressOf("fivmr_AH_long_mul"));
                    asm.popABunch(4);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.FMUL:
                    asm.fldM32(R_SP);
                    asm.addIR(4, R_SP);
                    asm.fmulM32(R_SP);
                    asm.fstpM32(R_SP);
                    break;
                case Opcodes.DMUL:
                    asm.fldM64(R_SP);
                    asm.addIR(8, R_SP);
                    asm.fmulM64(R_SP);
                    asm.fstpM64(R_SP);
                    break;
                case Opcodes.IDIV: {
                    asm.cmpMI(R_BP, offsetOfStackSlot(0), 0);
                    Branch b1=asm.setBranchSourceAndJccShort(J_NE);
                    
                    prepForCall(1,0);
                    asm.pushR(R_SI);
                    asm.call(MethodPtrs.throwArithmeticRTE);
                    perpForCall(1,0,ReturnMode.ONLY_THROW);
                    
                    asm.setBranchTarget(b1);
                    asm.pushM(R_BP, offsetOfStackSlot(1));
                    asm.call(CVar.addressOf("fivmr_AH_int_div"));
                    asm.popABunch(3);
                    asm.pushR(R_AX);
                    break;
                }
                case Opcodes.LDIV: {
                    asm.movMR(R_BP, offsetOfStackSlot(0), R_AX);
                    asm.orMR(R_BP, offsetOfStackSlot(1), R_AX);
                    asm.testRR(R_AX, R_AX);
                    Branch b1=asm.setBranchSourceAndJccShort(J_NE);
                    
                    prepForCall(1,0);
                    asm.pushR(R_SI);
                    asm.call(MethodPtrs.throwArithmeticRTE);
                    perpForCall(1,0,ReturnMode.ONLY_THROW);
                    
                    asm.setBranchTarget(b1);
                    
                    prepForCall(4,0);
                    asm.pushM(R_BP, offsetOfStackSlot(1));
                    asm.pushM(R_BP, offsetOfStackSlot(0));
                    asm.pushM(R_BP, offsetOfStackSlot(3));
                    asm.pushM(R_BP, offsetOfStackSlot(2));
                    asm.call(CVar.addressOf("fivmr_AH_long_div"));
                    perpForCall(4,0,4,ReturnMode.ONLY_RETURN);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                }
                case Opcodes.FDIV:
                    asm.fldM32(R_SP, 4);
                    asm.fdivM32(R_SP);
                    asm.addIR(4, R_SP);
                    asm.fstpM32(R_SP);
                    break;
                case Opcodes.DDIV:
                    asm.fldM64(R_SP, 8);
                    asm.fdivM64(R_SP);
                    asm.addIR(8, R_SP);
                    asm.fstpM64(R_SP);
                    break;
                case Opcodes.IREM: {
                    asm.cmpMI(R_BP, offsetOfStackSlot(0), 0);
                    Branch b1=asm.setBranchSourceAndJccShort(J_NE);
                    
                    prepForCall(1,0);
                    asm.pushR(R_SI);
                    asm.call(MethodPtrs.throwArithmeticRTE);
                    perpForCall(1,0,ReturnMode.ONLY_THROW);
                    
                    asm.setBranchTarget(b1);
                    asm.pushM(R_BP, offsetOfStackSlot(1));
                    asm.call(CVar.addressOf("fivmr_AH_int_mod"));
                    asm.popABunch(3);
                    asm.pushR(R_AX);
                    break;
                }
                case Opcodes.LREM: {
                    asm.movMR(R_BP, offsetOfStackSlot(0), R_AX);
                    asm.orMR(R_BP, offsetOfStackSlot(1), R_AX);
                    asm.testRR(R_AX, R_AX);
                    Branch b1=asm.setBranchSourceAndJccShort(J_NE);
                    
                    prepForCall(1,0);
                    asm.pushR(R_SI);
                    asm.call(MethodPtrs.throwArithmeticRTE);
                    perpForCall(1,0,ReturnMode.ONLY_THROW);
                    
                    asm.setBranchTarget(b1);

                    prepForCall(4,0);
                    asm.pushM(R_BP, offsetOfStackSlot(1));
                    asm.pushM(R_BP, offsetOfStackSlot(0));
                    asm.pushM(R_BP, offsetOfStackSlot(3));
                    asm.pushM(R_BP, offsetOfStackSlot(2));
                    asm.call(CVar.addressOf("fivmr_AH_long_mod"));
                    perpForCall(4,0,4,ReturnMode.ONLY_RETURN);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                }
                case Opcodes.FREM:
                    asm.pushM(R_BP, offsetOfStackSlot(1));
                    // FIXME: maybe this is dangerous?  maybe we should align the
                    // stack for this call?
                    asm.call(CVar.addressOf("fmodf"));
                    asm.popABunch(2);
                    asm.fstpM32(R_SP);
                    break;
                case Opcodes.DREM:
                    asm.pushM(R_BP, offsetOfStackSlot(3));
                    asm.pushM(R_BP, offsetOfStackSlot(2));
                    // FIXME: maybe this is dangerous?  maybe we should align the
                    // stack for this call?
                    asm.call(CVar.addressOf("fmod"));
                    asm.popABunch(4);
                    asm.fstpM64(R_SP);
                    break;
                case Opcodes.INEG:
                    asm.negM(R_BP, offsetOfStackSlot(0));
                    break;
                case Opcodes.LNEG:
                    asm.popR(R_CX);
                    asm.movIR(0, R_AX);
                    asm.subRR(R_CX, R_AX);
                    asm.popR(R_CX);
                    asm.movIR(0, R_DX);
                    asm.sbbRR(R_CX, R_DX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.FNEG:
                    asm.fldM32(R_SP);
                    asm.fchs();
                    asm.fstpM32(R_SP);
                    break;
                case Opcodes.DNEG:
                    asm.fldM64(R_SP);
                    asm.fchs();
                    asm.fstpM64(R_SP);
                    break;
                case Opcodes.ISHL:
                    asm.popR(R_CX);
                    asm.andIR(0x1f, R_CX);
                    asm.popR(R_AX);
                    asm.shlR(R_AX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.LSHL: {
                    asm.popR(R_CX); // width
                    asm.andIR(0x3f, R_CX);
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.shldR(R_DX, R_AX);
                    asm.shlR(R_AX);
                    asm.testIR(32, R_CX);
                    Branch b0 = asm.setBranchSourceAndJccShort(J_E);
                    asm.movRR(R_AX, R_DX);
                    asm.movIR(0, R_AX);
                    asm.setBranchTarget(b0);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                }
                case Opcodes.ISHR:
                    asm.popR(R_CX);
                    asm.andIR(0x1f, R_CX);
                    asm.popR(R_AX);
                    asm.sarR(R_AX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.LSHR: {
                    asm.popR(R_CX); // width
                    asm.andIR(0x3f, R_CX);
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.shrdR(R_AX, R_DX);
                    asm.sarR(R_DX);
                    asm.testIR(32, R_CX);
                    Branch b0 = asm.setBranchSourceAndJccShort(J_E);
                    asm.movRR(R_DX, R_AX);
                    asm.sarRI(R_DX, (byte)31);
                    asm.setBranchTarget(b0);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                }
                case Opcodes.IUSHR:
                    asm.popR(R_CX);
                    asm.andIR(0x1f, R_CX);
                    asm.popR(R_AX);
                    asm.shrR(R_AX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.LUSHR: {
                    asm.popR(R_CX); // width
                    asm.andIR(0x3f, R_CX);
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.shrdR(R_AX, R_DX);
                    asm.shrR(R_DX);
                    asm.testIR(32, R_CX);
                    Branch b0 = asm.setBranchSourceAndJccShort(J_E);
                    asm.movRR(R_DX, R_AX);
                    asm.movIR(0, R_DX);
                    asm.setBranchTarget(b0);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                }
                case Opcodes.IAND:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.andRR(R_DX,R_AX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.LAND:
                    asm.popR(R_AX);
                    asm.popR(R_CX);
                    asm.popR(R_DX);
                    asm.andRR(R_DX, R_AX);
                    asm.popR(R_DX);
                    asm.andRR(R_CX, R_DX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.IOR:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.orRR(R_DX, R_AX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.LOR:
                    asm.popR(R_AX);
                    asm.popR(R_CX);
                    asm.popR(R_DX);
                    asm.orRR(R_DX, R_AX);
                    asm.popR(R_DX);
                    asm.orRR(R_CX, R_DX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.IXOR:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.xorRR(R_DX, R_AX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.LXOR:
                    asm.popR(R_AX);
                    asm.popR(R_CX);
                    asm.popR(R_DX);
                    asm.xorRR(R_DX, R_AX);
                    asm.popR(R_DX);
                    asm.xorRR(R_CX, R_DX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.I2L:
                    asm.popR(R_AX);
                    asm.cdq();
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.I2F:
                    asm.call(CVar.addressOf("fivmr_AH_int_to_float"));
                    asm.movRR(R_SP, R_DX);
                    asm.fstpM32(R_DX);
                    break;
                case Opcodes.I2D:
                    asm.call(CVar.addressOf("fivmr_AH_int_to_double"));
                    asm.pushR(R_CX);
                    asm.movRR(R_SP, R_DX);
                    asm.fstpM64(R_DX);
                    break;
                case Opcodes.L2I:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.L2F:
                    asm.call(CVar.addressOf("fivmr_AH_long_to_float"));
                    asm.popR(R_CX);
                    asm.fstpM32(R_SP);
                    break;
                case Opcodes.L2D:
                    // this function requires us to do proper alignment unfortunately.
                    prepForCall(2,0);
                    asm.pushM(R_BP,offsetOfStackSlot(1));
                    asm.pushM(R_BP,offsetOfStackSlot(0));
                    asm.call(CVar.addressOf("fivmr_AH_long_to_double"));
                    perpForCall(2,0,ReturnMode.ONLY_RETURN);
                    asm.fstpM64(R_SP);
                    break;
                case Opcodes.F2I:
                    asm.call(CVar.addressOf("fivmr_AH_float_to_int"));
                    asm.popR(R_CX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.F2L:
                    asm.call(CVar.addressOf("fivmr_AH_float_to_long"));
                    asm.popR(R_CX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.F2D:
                    asm.fldM32(R_SP);
                    asm.subIR(4, R_SP);
                    asm.fstpM64(R_SP);
                    break;
                case Opcodes.D2I:
                    asm.call(CVar.addressOf("fivmr_AH_double_to_int"));
                    asm.popR(R_CX);
                    asm.popR(R_CX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.D2L:
                    asm.call(CVar.addressOf("fivmr_AH_double_to_long"));
                    asm.popR(R_CX);
                    asm.popR(R_CX);
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case Opcodes.D2F:
                    asm.fldM64(R_SP);
                    asm.addIR(4, R_SP);
                    asm.fstpM32(R_SP);
                    break;
                case Opcodes.I2B:
                    asm.movsxM8R(R_BP, offsetOfStackSlot(0), R_AX);
                    asm.movRM(R_AX, R_BP, offsetOfStackSlot(0));
                    break;
                case Opcodes.I2C:
                    asm.movzxM16R(R_BP, offsetOfStackSlot(0), R_AX);
                    asm.movRM(R_AX, R_BP, offsetOfStackSlot(0));
                    break;
                case Opcodes.I2S:
                    asm.movsxM16R(R_BP, offsetOfStackSlot(0), R_AX);
                    asm.movRM(R_AX, R_BP, offsetOfStackSlot(0));
                    break;
                case Opcodes.LCMP: {
                    asm.popR(R_AX);
                    asm.popR(R_CX);
                    asm.popR(R_DX);
                    asm.cmpMR(R_BP, offsetOfStackSlot(3), R_CX);
                    Branch b1 = asm.setBranchSourceAndJccShort(J_L);
                    Branch b2 = asm.setBranchSourceAndJccShort(J_G);
                    asm.cmpRR(R_DX, R_AX);
                    Branch b3 = asm.setBranchSourceAndJccShort(J_B);
                    Branch b4 = asm.setBranchSourceAndJccShort(J_A);
                    asm.popR(R_AX);
                    asm.pushI32(0);
                    Branch b5 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b1);
                    asm.setBranchTarget(b3);
                    asm.popR(R_AX);
                    asm.pushI32(-1);
                    Branch b6 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b2);
                    asm.setBranchTarget(b4);
                    asm.popR(R_AX);
                    asm.pushI32(1);
                    asm.setBranchTarget(b5);
                    asm.setBranchTarget(b6);
                    break;
                }
                case Opcodes.FCMPL: {
                    asm.fldM32(R_SP);
                    asm.fldM32(R_SP, 4);
                    asm.addIR(8, R_SP);
                    asm.fcmp();
                    Branch b0 = asm.setBranchSourceAndJccShort(J_P);
                    Branch b1 = asm.setBranchSourceAndJccShort(J_C);
                    Branch b2 = asm.setBranchSourceAndJccShort(J_NZ);
                    asm.pushI32(0);
                    Branch b3 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b2);
                    asm.pushI32(1);
                    Branch b4 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b0);
                    asm.setBranchTarget(b1);
                    asm.pushI32(-1);
                    asm.setBranchTarget(b3);
                    asm.setBranchTarget(b4);
                    break;
                }
                case Opcodes.FCMPG: {
                    asm.fldM32(R_SP);
                    asm.fldM32(R_SP, 4);
                    asm.addIR(8, R_SP);
                    asm.fcmp();
                    Branch b0 = asm.setBranchSourceAndJccShort(J_P);
                    Branch b1 = asm.setBranchSourceAndJccShort(J_C);
                    Branch b2 = asm.setBranchSourceAndJccShort(J_NZ);
                    asm.pushI32(0);
                    Branch b3 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b0);
                    asm.setBranchTarget(b2);
                    asm.pushI32(1);
                    Branch b4 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b1);
                    asm.pushI32(-1);
                    asm.setBranchTarget(b3);
                    asm.setBranchTarget(b4);
                    break;
                }
                case Opcodes.DCMPL: {
                    asm.fldM64(R_SP);
                    asm.fldM64(R_SP, 8);
                    asm.addIR(16, R_SP);
                    asm.fcmp();
                    Branch b0 = asm.setBranchSourceAndJccShort(J_P);
                    Branch b1 = asm.setBranchSourceAndJccShort(J_C);
                    Branch b2 = asm.setBranchSourceAndJccShort(J_NZ);
                    asm.pushI32(0);
                    Branch b3 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b2);
                    asm.pushI32(1);
                    Branch b4 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b0);
                    asm.setBranchTarget(b1);
                    asm.pushI32(-1);
                    asm.setBranchTarget(b3);
                    asm.setBranchTarget(b4);
                    break;
                }
                case Opcodes.DCMPG: {
                    asm.fldM64(R_SP);
                    asm.fldM64(R_SP, 8);
                    asm.addIR(16, R_SP);
                    asm.fcmp();
                    Branch b0 = asm.setBranchSourceAndJccShort(J_P);
                    Branch b1 = asm.setBranchSourceAndJccShort(J_C);
                    Branch b2 = asm.setBranchSourceAndJccShort(J_NZ);
                    asm.pushI32(0);
                    Branch b3 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b0);
                    asm.setBranchTarget(b2);
                    asm.pushI32(1);
                    Branch b4 = asm.setBranchSourceAndJmpShort();
                    asm.setBranchTarget(b1);
                    asm.pushI32(-1);
                    asm.setBranchTarget(b3);
                    asm.setBranchTarget(b4);
                    break;
                }
                case Opcodes.IRETURN:
                case Opcodes.ARETURN:
                    asm.popR(R_AX);
                    generateEpilogue(-1);
                    break;
                case Opcodes.LRETURN:
                    asm.popR(R_AX);
                    asm.popR(R_DX);
                    generateEpilogue(-2);
                    break;
                case Opcodes.FRETURN:
                    asm.fldM32(R_SP);
                    generateEpilogue(0);
                    break;
                case Opcodes.DRETURN:
                    asm.fldM64(R_SP);
                    generateEpilogue(0);
                    break;
                case Opcodes.RETURN:
                    generateEpilogue(0);
                    break;
                case Opcodes.ARRAYLENGTH:
                    asm.popR(R_AX);
                    if (!fhc.stackAtIsNonNull(0)) {
                        nullCheck(R_AX,-1);
                    }
                    if (Settings.OM_CONTIGUOUS) {
                        asm.pushM(R_AX, OMData.arrayLengthOffset());
                    } else if (Settings.OM_FRAGMENTED) {
                        asm.movMR(R_AX, -OMData.objectFHOffset(), R_AX);
                        asm.pushM(R_AX, OMData.spineArrayLengthOffset());
                    } else {
                        throw new Error("dude...");
                    }
                    break;
                case Opcodes.ATHROW:
                    asm.popR(R_AX);
                    asm.pushR(R_AX);
                    prepForCall(2,0);
                    asm.pushR(R_AX);
                    asm.pushR(R_SI);
                    asm.call(CVar.addressOf("fivmr_throw"));
                    perpForCall(2,0,ReturnMode.ONLY_THROW);
                    break;
                case Opcodes.MONITORENTER:
                    if (monitorsWork()) {
                        asm.movMR(R_BP, offsetOfStackSlot(0), R_DX);

                        Branch[] bs=monitorEnterFastPath(R_DX,R_CX);
                        
                        asm.popR(R_DX);
                        prepForCall(2,-1);
                        asm.pushR(R_DX);
                        asm.pushR(R_SI);
                        asm.call(MethodPtrs.lockSlow);
                        perpForCall(2,-1,0,ReturnMode.RETURN_OR_THROW);
                        
                        Branch b2=asm.setBranchSourceAndJmpShort();
                        
                        for (Branch b : bs) {
                            asm.setBranchTarget(b);
                        }
                        
                        asm.popR(R_AX);
                        
                        asm.setBranchTarget(b2);
                    } else {
                        asm.popR(R_AX);
                        prepForCall(2,-1);
                        asm.pushR(R_AX);
                        asm.pushR(R_SI);
                        asm.call(MethodPtrs.lock);
                        perpForCall(2,-1,ReturnMode.RETURN_OR_THROW);
                    }
                    break;
                case Opcodes.MONITOREXIT:
                    if (monitorsWork()) {
                        asm.movMR(R_BP, offsetOfStackSlot(0), R_DX);
                        
                        Branch[] bs=monitorExitFastPath(R_DX,R_CX);
                        
                        asm.popR(R_DX);
                        prepForCall(2,-1);
                        asm.pushR(R_DX);
                        asm.pushR(R_SI);
                        asm.call(MethodPtrs.unlockSlow);
                        perpForCall(2,-1,0,ReturnMode.RETURN_OR_THROW);

                        Branch b2=asm.setBranchSourceAndJmpShort();
                        
                        for (Branch b : bs) {
                            asm.setBranchTarget(b);
                        }
                        
                        asm.popR(R_AX);
                        
                        asm.setBranchTarget(b2);
                    } else {
                        asm.popR(R_DX);
                        prepForCall(2,-1);
                        asm.pushR(R_DX);
                        asm.pushR(R_SI);
                        asm.call(MethodPtrs.unlock);
                        perpForCall(2,-1,0,ReturnMode.RETURN_OR_THROW);
                    }
                    break;
                default:
                    if (Settings.ASSERTS_ON) {
                        throw new fivmError("unrecognized opcode: "+opcode);
                    }
                    Magic.notReached();
                }
                fhc.visitInsn(opcode);
            }
            
            @Override
            public void visitFieldInsn(int opcode,
                                       UTF8Sequence owner,
                                       UTF8Sequence name,
                                       UTF8Sequence desc) {
                // step #1: figure out the situation
                boolean isInstance;
                int recvSlot=0;
                char type=Types.toExec((char)desc.byteAt(0));
                int stackDelta;
                
                // protocol:
                // EAX = receiver
                // EDX = value low
                // ECX = value high (64-bit values only)
                
                // FIXME: convert owner to type name?
                Pointer ownerStub=findStubClass(mbe.typeContext(),owner);
                Pointer ownerType=fivmr_TypeStub_tryGetTypeData(ownerStub);
                
                // goofy part: if we know that the field is a reference type
                // then we do a prepForCall and a perpForCall; otherwise
                // we do other stuff.
                boolean needOOLBarrier=
                    (opcode==Opcodes.PUTFIELD ||
                     opcode==Opcodes.PUTSTATIC) &&
                    sub.needOutOfLineStoreBarrier((char)desc.byteAt(0));
                
                switch (opcode) {
                case Opcodes.PUTFIELD:
                case Opcodes.GETFIELD:
                    isInstance=true;
                    break;
                case Opcodes.PUTSTATIC:
                case Opcodes.GETSTATIC:
                    isInstance=false;
                    break;
                default:
                    throw new Error("Bad field opcode: "+opcode);
                }
                
                switch (opcode) {
                case Opcodes.PUTFIELD:
                    recvSlot=Types.cells(type);
                    if (Types.cells(type)==2) {
                        asm.popR(R_DX);
                        asm.popR(R_CX);
                        stackDelta=-3;
                    } else {
                        asm.popR(R_DX);
                        stackDelta=-2;
                    }
                    asm.popR(R_AX);
                    break;
                case Opcodes.GETFIELD:
                    asm.popR(R_AX);
                    stackDelta=-1;
                    recvSlot=0;
                    break;
                case Opcodes.PUTSTATIC:
                    if (Types.cells(type)==2) {
                        asm.popR(R_DX);
                        asm.popR(R_CX);
                        stackDelta=-2;
                    } else {
                        asm.popR(R_DX);
                        stackDelta=-1;
                    }
                    break;
                case Opcodes.GETSTATIC:
                    stackDelta=0;
                    break;
                default:
                    throw new Error("Bad field opcode: "+opcode);
                }
                
                // step #3: do a nullcheck
                if (isInstance) {
                    if (!needOOLBarrier) {
                        if (!fhc.stackAtIsNonNull(recvSlot)) {
                            nullCheck(R_AX, 0);
                        }
                    }
                }
                
                // step #4: do stuff
                
                // FIXME: this check could be optimized.  if it's an instance field,
                // we don't have to init the type.  as it stands, this is "correct"
                // in the sense that the field will be lazily resolved, and the lazy
                // resolver will be smart enough to *not* invoke static initialization.
                if (ownerType==Pointer.zero() || !inited(ownerType)) {
                    // oh noes
                    
                    asm.nopAlign(4,1);
                    
                    asm.jmp_long(0); // no-op jump, to be replaced by the resolver

                    int pcBefore=asm.getPC();
                    Pointer bfa=code.regionAlloc(CType.sizeof("fivmr_BaseFieldAccess"));
                    CType.put(bfa,"fivmr_BaseFieldAccess","fat",Protocols.fatForOpcode(opcode));
                    CType.put(bfa,"fivmr_BaseFieldAccess","owner",ownerStub);
                    desc.assertBytecode(mbe.bytecode());
                    CType.put(bfa,"fivmr_BaseFieldAccess","descAddr",desc.bytecodeStringAddress());
                    name.assertBytecode(mbe.bytecode());
                    CType.put(bfa,"fivmr_BaseFieldAccess","nameAddr",name.bytecodeStringAddress());
                    CType.put(bfa,"fivmr_BaseFieldAccess","stackHeight",fhc.stackHeight());
                    CType.put(bfa,"fivmr_BaseFieldAccess","recvType",fhc.tryStackAt(recvSlot));
                    CType.put(bfa,"fivmr_BaseFieldAccess","dataType",fhc.tryStackAt(Types.cells(type)-1));
                    CType.put(bfa,"fivmr_BaseFieldAccess","debugID",Pointer.fromInt(allocDebugID()));
                    asm.pushI32_wide(bfa.castToInt());
                    
                    asm.callAbsM(R_SI, CType.offsetof("fivmr_ThreadState","resolveFieldAccessThunk").castToInt());
                    int pcAfter=asm.getPC();
                    
                    if (Settings.ASSERTS_ON && pcAfter-pcBefore != 8) {
                        throw new fivmError("did not emit exactly eight bytes for the thunk: "+pcAfter+" - "+pcBefore+" = "+(pcAfter-pcBefore));
                    }
                    
                } else {
                    Pointer fr=Pointer.zero();
                    try {
                        fr=resolveField(methodRec,
                                        !isInstance,
                                        ownerType,
                                        desc,
                                        name);
                        if (Settings.ASSERTS_ON && fr==Pointer.zero()) {
                            throw new fivmError("resolveField returned zero");
                        }
                    } catch (Throwable e) {
                        ExceptionThrower et=ExceptionThrower.build(e);
                        code.addPointer(et);
                        prepForCall(2,stackDelta);
                        asm.pushI32(Pointer.fromObject(et).castToInt());
                        asm.pushR(R_SI);
                        asm.call(MethodPtrs.throwException);
                        perpForCall(2,stackDelta,ReturnMode.ONLY_THROW);
                    }
                    
                    if (fr!=Pointer.zero()) {
                        sub.squirtFieldAccess(opcode,fr,
                                              fhc.tryStackAt(recvSlot),
                                              fhc.tryStackAt(Types.cells(type)-1));
                    }
                }
                
                fhc.visitFieldInsn(opcode,owner,name,desc);
            }
            
            @Override
            public void visitMethodInsn(int opcode,
                                        UTF8Sequence owner,
                                        UTF8Sequence name,
                                        UTF8Sequence desc) {
                TypeParsing.MethodSigSeqs sigs=TypeParsing.splitMethodSig(desc);
                char retType=(char)sigs.result().byteAt(0);
                int paramCells=1; // threadstate
                int recvSlot=0;
                
                for (UTF8Sequence sig : sigs.params()) {
                    int n=Types.cells(Types.toExec((char)sig.byteAt(0)));
                    paramCells+=n;
                    recvSlot+=n;
                }
                if (Protocols.isInstance(opcode)) {
                    paramCells++;
                }
                
                storeDebugIDForInstruction();
                
                int npop=prepForCall(paramCells,0);
                
                // push the arguments in reverse order, with care taken to not flip
                // long and double arguments.
                
                int j=0;
                for (int i=sigs.nParams();i-->0;) {
                    if (Types.cells(Types.toExec((char)sigs.param(i).byteAt(0)))==2) {
                        asm.pushM(R_BP,offsetOfStackSlot(j+1));
                        asm.pushM(R_BP,offsetOfStackSlot(j));
                        j+=2;
                    } else {
                        asm.pushM(R_BP,offsetOfStackSlot(j++));
                    }
                }
                
                if (Protocols.isInstance(opcode)) {
                    asm.movMR(R_BP,offsetOfStackSlot(j),R_DX);
                    asm.pushR(R_DX);
                }
                
                asm.pushR(R_SI);
                
                // now attempt resolution and Do Stuff...
                
                Pointer ownerStub=findStubClass(mbe.typeContext(),owner);
                Pointer ownerType=fivmr_TypeStub_tryGetTypeData(ownerStub);
                
                if (Protocols.isInstance(opcode)) {
                    if (opcode!=Opcodes.INVOKEINTERFACE) {
                        if (!fhc.stackAtIsNonNull(recvSlot)) {
                            nullCheck(R_DX, npop);
                        }
                    }
                }

                if (ownerType==Pointer.zero() || !inited(ownerType)) {
                    asm.nopAlign(4,1);
                    if (opcode==Opcodes.INVOKEINTERFACE) {
                        asm.call_long(2); // this will get patched, but now we jump to the call
                        asm.jmp(8); // jump over the resolution nonsense
                    } else {
                        asm.jmp_long(0);
                    }
                    
                    Pointer bmc=code.regionAlloc(CType.sizeof("fivmr_BaseMethodCall"));
                    CType.put(bmc,"fivmr_BaseMethodCall","mct",Protocols.mctForOpcode(opcode));
                    CType.put(bmc,"fivmr_BaseMethodCall","owner",ownerStub);
                    desc.assertBytecode(mbe.bytecode());
                    CType.put(bmc,"fivmr_BaseMethodCall","descAddr",desc.bytecodeStringAddress());
                    name.assertBytecode(mbe.bytecode());
                    CType.put(bmc,"fivmr_BaseMethodCall","nameAddr",name.bytecodeStringAddress());
                    CType.put(bmc,"fivmr_BaseMethodCall","stackHeight",fhc.stackHeight());
                    asm.pushI32_wide(bmc.castToInt());
                    
                    if (opcode==Opcodes.INVOKEINTERFACE) {
                        asm.callAbsM(R_SI, CType.offsetof("fivmr_ThreadState","resolveInvokeInterfaceThunk").castToInt());
                    } else {
                        asm.callAbsM(R_SI, CType.offsetof("fivmr_ThreadState","resolveMethodCallThunk").castToInt());
                    }
                    
                    // the idea here is that the resolver thunk will always
                    // jump to here, regardless of whether it's a successful
                    // resolution or a failed one.  if it's a failed one it
                    // would be throwing an exception; perpForCall()'s code
                    // will notice that and do the Right Thing.
                    
                } else {
                    // lookup method
                    Pointer mr=Pointer.zero();
                    
                    try {
                        resolveType(ownerType);
                        
                        mr=resolveMethod(methodRec,
                                         !Protocols.isInstance(opcode),
                                         ownerType,
                                         name,
                                         desc);
                        if (Settings.ASSERTS_ON && mr==Pointer.zero()) {
                            throw new fivmError("resolveMethod returned zero");
                        }
                    } catch (Throwable e) {
                        ExceptionThrower et=ExceptionThrower.build(e);
                        code.addPointer(et);
                        prepForCall(2,npop);
                        asm.pushI32(Pointer.fromObject(et).castToInt());
                        asm.pushR(R_SI);
                        asm.call(MethodPtrs.throwException);
                        perpForCall(2,npop,ReturnMode.ONLY_THROW);
                    }
                        
                    if (mr!=Pointer.zero()) {
                        // call squirtMethodCall
                        sub.squirtMethodCall(opcode,mr);
                    }
                }
                
                perpForCall(paramCells,0,paramCells-1,ReturnMode.RETURN_OR_THROW);
                
                switch (retType) {
                case 'B':
                case 'Z':
                    asm.movsxR8R(R_AX,R_AX);
                    asm.pushR(R_AX);
                    break;
                case 'S':
                    asm.cwde();
                    asm.pushR(R_AX);
                    break;
                case 'C':
                    asm.movzxR16R(R_AX,R_AX);
                    asm.pushR(R_AX);
                    break;
                case 'I':
                case 'L':
                case '[':
                    asm.pushR(R_AX);
                    break;
                case 'J':
                    asm.pushR(R_DX);
                    asm.pushR(R_AX);
                    break;
                case 'F':
                    asm.subIR(4,R_SP);
                    asm.fstpM32(R_SP);
                    break;
                case 'D':
                    asm.subIR(8,R_SP);
                    asm.fstpM64(R_SP);
                    break;
                case 'V':
                    // nothing to do
                    break;
                default:
                    throw new Error("unrecognized retType: "+retType);
                }
                
                fhc.visitMethodInsn(opcode,owner,name,desc);
            }
            
            @Override
            public void visitIincInsn(int var,int increment) {
                asm.addIM(increment, R_BP, offsetToLocal(var));
                fhc.visitIincInsn(var,increment);
            }
            
            @Override
            public void visitIntInsn(int opcode,int operand) {
                switch (opcode) {
                case Opcodes.BIPUSH:
                case Opcodes.SIPUSH:
                    asm.pushI32(operand);
                    break;
                case Opcodes.NEWARRAY: {
                    Pointer td;
                    switch (operand) {
                    case Opcodes.T_BOOLEAN:
                        td=CType.getPointer(Magic.getPayload(),"fivmr_Payload","td_booleanArr");
                        break;
                    case Opcodes.T_BYTE:
                        td=CType.getPointer(Magic.getPayload(),"fivmr_Payload","td_byteArr");
                        break;
                    case Opcodes.T_CHAR:
                        td=CType.getPointer(Magic.getPayload(),"fivmr_Payload","td_charArr");
                        break;
                    case Opcodes.T_SHORT:
                        td=CType.getPointer(Magic.getPayload(),"fivmr_Payload","td_shortArr");
                        break;
                    case Opcodes.T_INT:
                        td=CType.getPointer(Magic.getPayload(),"fivmr_Payload","td_intArr");
                        break;
                    case Opcodes.T_LONG:
                        td=CType.getPointer(Magic.getPayload(),"fivmr_Payload","td_longArr");
                        break;
                    case Opcodes.T_FLOAT:
                        td=CType.getPointer(Magic.getPayload(),"fivmr_Payload","td_floatArr");
                        break;
                    case Opcodes.T_DOUBLE:
                        td=CType.getPointer(Magic.getPayload(),"fivmr_Payload","td_doubleArr");
                        break;
                    default:
                        throw new Error("bad type operand: "+operand);
                    }
                    sub.squirtAllocArray(td,0);
                    break;
                }
                default: throw new Error("bad opcode: "+opcode);
                }
                fhc.visitIntInsn(opcode,operand);
            }
            
            @Override
            public void visitVarInsn(int opcode, int var) {
                switch (opcode) {
                case Opcodes.ILOAD:
                case Opcodes.FLOAD:
                case Opcodes.ALOAD:
                    asm.pushM(R_BP, offsetToLocal(var));
                    break;
                case Opcodes.LLOAD:
                case Opcodes.DLOAD:
                    asm.pushM(R_BP, offsetToLocal(var+1));
                    asm.pushM(R_BP, offsetToLocal(var));
                    break;
                case Opcodes.ISTORE:
                case Opcodes.FSTORE:
                case Opcodes.ASTORE:
                    asm.popM(R_BP, offsetToLocal(var));
                    break;
                case Opcodes.LSTORE:
                case Opcodes.DSTORE:
                    asm.popM(R_BP, offsetToLocal(var));
                    asm.popM(R_BP, offsetToLocal(var+1));
                    break;
                default:
                    throw new Error("bad opcode: "+opcode);
                }
                fhc.visitVarInsn(opcode,var);
            }
            
            @Override
            public void visitJumpInsn(int opcode,Label label) {
                switch (opcode) {
                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ACMPNE:
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPNE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT:
                case Opcodes.IF_ICMPLE: {
                    asm.popR(R_DX);
                    asm.popR(R_AX);
                    asm.cmpRR(R_AX,R_DX);
                    byte condition;
                    switch (opcode) {
                    case Opcodes.IF_ACMPEQ:
                    case Opcodes.IF_ICMPEQ:
                        condition=J_E;
                        break;
                    case Opcodes.IF_ACMPNE:
                    case Opcodes.IF_ICMPNE:
                        condition=J_NE;
                        break;
                    case Opcodes.IF_ICMPLT:
                        condition=J_L;
                        break;
                    case Opcodes.IF_ICMPGE:
                        condition=J_GE;
                        break;
                    case Opcodes.IF_ICMPGT:
                        condition=J_G;
                        break;
                    case Opcodes.IF_ICMPLE:
                        condition=J_LE;
                        break;
                    default:
                        throw new Error("should not get here");
                    }
                    jccTo(condition,label.getOffsetWorks());
                    break;
                }
                case Opcodes.IFEQ:
                case Opcodes.IFNE:
                case Opcodes.IFLT:
                case Opcodes.IFGE:
                case Opcodes.IFGT:
                case Opcodes.IFLE:
                case Opcodes.IFNULL:
                case Opcodes.IFNONNULL: {
                    asm.popR(R_AX);
                    asm.testRR(R_AX,R_AX);
                    byte condition;
                    switch (opcode) {
                    case Opcodes.IFEQ:
                    case Opcodes.IFNULL:
                        condition=J_E;
                        break;
                    case Opcodes.IFNE:
                    case Opcodes.IFNONNULL:
                        condition=J_NE;
                        break;
                    case Opcodes.IFLT:
                        condition=J_L;
                        break;
                    case Opcodes.IFGE:
                        condition=J_GE;
                        break;
                    case Opcodes.IFGT:
                        condition=J_G;
                        break;
                    case Opcodes.IFLE:
                        condition=J_LE;
                        break;
                    default:
                        throw new Error("should not get here");
                    }
                    jccTo(condition,label.getOffsetWorks());
                    break;
                }
                case Opcodes.GOTO:
                    jumpTo(label.getOffsetWorks());
                    break;
                default:
                    throw new Error("bad opcode: "+opcode);
                }
                fhc.visitJumpInsn(opcode,label);
            }
            
            @Override
            public void visitLdcInsn(Object cst) {
                if (cst instanceof Integer) {
                    asm.pushI32((Integer)cst);
                } else if (cst instanceof Float) {
                    asm.pushI32(Float.floatToRawIntBits((Float)cst));
                } else if (cst instanceof Long) {
                    long value=(Long)cst;
                    asm.pushI32((int)((value>>32)&0xffffffffl));
                    asm.pushI32((int)(value&0xffffffffl));
                } else if (cst instanceof Double) {
                    long value=Double.doubleToRawLongBits((Double)cst);
                    asm.pushI32((int)((value>>32)&0xffffffffl));
                    asm.pushI32((int)(value&0xffffffffl));
                } else if (cst instanceof String) {
                    String s=((String)cst).intern();
                    code.addPointer(s); /* once we have weak references implemented, this
                                           will be essential for anchoring the String so
                                           that it doesn't get wiped out. */
                    asm.pushI32(Pointer.fromObject(s).castToInt());
                } else if (cst instanceof Type) {
                    // - do we need a type static init check?
                    //   (double-check this and verify that C1 is doing the Right Thing)
                    //   -> no we do not and C1 does it right
                    //
                    // - if it's just a type stub then what?  I think that the best
                    //   would be to have a assembly that does:
                    //
                    // x = type stub address
                    // y = x->forward
                    // if !y.isTypeData
                    //    y = fivmr_TypeStub_getTypeData(x)
                    //    checkException
                    // end
                    // push y->classObject
                    //
                    // note that the use of 'y = fivmr_TypeStub_getTypeData(x)' means
                    // that after 1 or 2 calls, x->forward will point to the TypeData.
                    //
                    // NOTE: the above code isn't *quite* right since x->forward might
                    // crash.
                    
                    Pointer type=
                        getType(findStub(mbe.typeContext(),
                                         ((Type)cst).getDescriptor()));
                    
                    if (type==Pointer.zero()) {
                        asm.pushM(R_AX,
                                  CType.offsetof("fivmr_TypeData","classObject").castToInt());
                    } else {
                        asm.pushI32(
                            Pointer.fromObject(fivmr_TypeData_asClass(type)).castToInt());
                    }
                } else {
                    throw new Error("bad constant pool value: "+cst);
                }
                
                fhc.visitLdcInsn(cst);
            }
            
            @Override
            public void visitTableSwitchInsn(int min,int max,Label dflt,Label[] labels) {
                if (max<min) {
                    throw new fivmError("Invalid table switch: min = "+min+", max = "+max);
                }
                asm.popR(R_AX);
                if (min!=0) {
                    asm.subIR(min, R_AX);
                }
                asm.cmpRI(R_AX, max-min);
                jccTo(J_A, dflt.getOffsetWorks());
                
                Pointer jumptable=
                    code.regionAlloc(Pointer.fromInt(labels.length).mul(4));
                for (int i=0;i<labels.length;++i) {
                    Pointer jumpslot=jumptable.add(Pointer.fromInt(i).mul(4));
                    int offset=labels[i].getOffsetWorks();
                    Branch b=backwardBranches.get(offset);
                    if (b!=null) {
                        jumpslot.store(b.targetPC);
                    } else {
                        b=new Branch(offset,Pointer.zero().sub(1));
                        b.addrAddr=jumpslot;
                        forwardBranches.add(offset,b);
                    }
                }
                asm.jmpAbsMS4(jumptable.castToInt(), R_AX);
                
                fhc.visitTableSwitchInsn(min,max,dflt,labels);
            }
            
            @Override
            public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels) {
                // if this code ends up being correct, then I will laugh.
                // (heh, so far it looks correct.)
                
                // FIXME: what if the list isn't sorted?  is that allowed?

                asm.popR(R_AX);
                
                MyStack< LookupRange > stack=
                    new MyStack< LookupRange >();
                stack.push(new LookupRange(0,keys.length));
                
                while (!stack.empty()) {
                    LookupRange range=stack.pop();
                    
                    if (Settings.ASSERTS_ON && range.diff()==0) {
                        throw new Error("shouldn't happen");
                    }
                    
                    if (range.hasBranch()) {
                        asm.setBranchTarget(range.branch());
                    }
                    
                    // find balanced median
                    int diff=range.b-range.a;
                    int medI=diff/2+range.a;
                    
                    // balance it out
                    if ((diff&3)==2) {
                        medI--;
                    }
                    
                    asm.cmpRI(R_AX, keys[medI]);
                    jccTo(J_E, labels[medI].getOffsetWorks());
                    
                    IntTuple below=new IntTuple(range.a,medI);
                    IntTuple above=new IntTuple(medI+1,range.b);
                    
                    if (below.diff()==0) {
                        if (above.diff()==0) {
                            // ran out of cases; jump to default
                            jumpTo(dflt.getOffsetWorks());
                        } else {
                            // this ensures that the next iteration of this loop
                            // will deal with this case, so we don't need a jump
                            stack.push(new LookupRange(above));
                        }
                    } else {
                        if (above.diff()==0) {
                            // this ensures that the next iteration of this loop
                            // will deal with this case, so we don't need a jump
                            stack.push(new LookupRange(below));
                        } else {
                            Branch b=asm.setBranchSourceAndJcc(J_L);
                            stack.push(new LookupRange(below,b));
                            stack.push(new LookupRange(above));
                        }
                    }
                }
                
                fhc.visitLookupSwitchInsn(dflt,keys,labels);
            }
            
            @Override
            public void visitTypeInsn(int opcode,UTF8Sequence typedesc) {
                switch (opcode) {
                case Opcodes.NEW: {
                    Pointer typeStub=findStubClass(mbe.typeContext(), typedesc);
                    
                    Pointer type=fivmr_TypeStub_tryGetTypeData(typeStub);
                    
                    if (type==Pointer.zero() || !inited(type)) {
                        asm.nopAlign(4,1);
                        asm.jmp_long(0);
                        
                        int pcBefore=asm.getPC();
                        Pointer boa=code.regionAlloc(CType.sizeof("fivmr_BaseObjectAlloc"));
                        CType.put(boa,"fivmr_BaseObjectAlloc","type",typeStub);
                        CType.put(boa,"fivmr_BaseObjectAlloc","stackHeight",fhc.stackHeight());
                        CType.put(boa,"fivmr_BaseObjectAlloc","debugID",Pointer.fromInt(allocDebugID()));
                        asm.pushI32_wide(boa.castToInt());
                        asm.callAbsM(R_SI, CType.offsetof("fivmr_ThreadState","resolveObjectAllocThunk").castToInt());
                        int pcAfter=asm.getPC();
                        if (Settings.ASSERTS_ON && pcAfter-pcBefore != 8) {
                            throw new fivmError("did not emit exactly eight bytes for the thunk: "+pcAfter+" - "+pcBefore+" = "+(pcAfter-pcBefore));
                        }
                    } else {
                        sub.squirtAllocObject(type,0);
                    }
                    break;
                }
                case Opcodes.ANEWARRAY: {
                    Pointer typeStub=findStub(mbe.typeContext(),
                                              UTF8Sequence.LBRAC.plus(
                                                  TypeParsing.purifyRefOnlyType(
                                                      typedesc)));
                    Pointer type=fivmr_TypeStub_tryGetTypeData(typeStub);
                    
                    if (type==Pointer.zero()) {
                        asm.nopAlign(4,1);
                        asm.jmp_long(0);
                        
                        int pcBefore=asm.getPC();
                        Pointer baa=code.regionAlloc(CType.sizeof("fivmr_BaseArrayAlloc"));
                        CType.put(baa,"fivmr_BaseArrayAlloc","type",typeStub);
                        CType.put(baa,"fivmr_BaseArrayAlloc","stackHeight",fhc.stackHeight());
                        CType.put(baa,"fivmr_BaseArrayAlloc","debugID",Pointer.fromInt(allocDebugID()));
                        asm.pushI32_wide(baa.castToInt());
                        asm.callAbsM(R_SI, CType.offsetof("fivmr_ThreadState","resolveArrayAllocThunk").castToInt());
                        int pcAfter=asm.getPC();
                        if (Settings.ASSERTS_ON && pcAfter-pcBefore != 8) {
                            throw new fivmError("did not emit exactly eight bytes for the thunk: "+pcAfter+" - "+pcBefore+" = "+(pcAfter-pcBefore));
                        }
                    } else {
                        boolean ok=false;
                        try {
                            resolveType(type);
                            ok=true;
                        } catch (Throwable e) {
                            ExceptionThrower et=ExceptionThrower.build(e);
                            code.addPointer(et);
                            prepForCall(2,0);
                            asm.pushI32(Pointer.fromObject(et).castToInt());
                            asm.pushR(R_SI);
                            asm.call(MethodPtrs.throwException);
                            perpForCall(2,0,ReturnMode.ONLY_THROW);
                        }
                        
                        if (ok) {
                            sub.squirtAllocArray(type,0);
                        }
                    }
                    break;
                }
                case Opcodes.CHECKCAST:
                case Opcodes.INSTANCEOF: {
                    Pointer typeStub=findStub(mbe.typeContext(),
                                              TypeParsing.purifyRefOnlyType(typedesc));
                    
                    Pointer type=fivmr_TypeStub_tryGetTypeData(typeStub);
                    
                    if (type==Pointer.zero()) {
                        asm.nopAlign(4,1);
                        asm.jmp_long(0);
                        
                        int pcBefore=asm.getPC();
                        Pointer bio=code.regionAlloc(CType.sizeof("fivmr_BaseInstanceof"));
                        CType.put(bio,"fivmr_BaseInstanceof","iot",Protocols.iotForOpcode(opcode));
                        CType.put(bio,"fivmr_BaseInstanceof","type",typeStub);
                        CType.put(bio,"fivmr_BaseInstanceof","stackHeight",fhc.stackHeight());
                        CType.put(bio,"fivmr_BaseInstanceof","debugID",Pointer.fromInt(allocDebugID()));
                        asm.pushI32_wide(bio.castToInt());
                        asm.callAbsM(R_SI, CType.offsetof("fivmr_ThreadState","resolveInstanceofThunk").castToInt());
                        int pcAfter=asm.getPC();
                        if (Settings.ASSERTS_ON && pcAfter-pcBefore != 8) {
                            throw new fivmError("did not emit exactly eight bytes for the thunk: "+pcAfter+" - "+pcBefore+" = "+(pcAfter-pcBefore));
                        }
                    } else {
                        boolean ok=false;
                        try {
                            resolveType(type);
                            ok=true;
                        } catch (Throwable e) {
                            ExceptionThrower et=ExceptionThrower.build(e);
                            code.addPointer(et);
                            prepForCall(2,0);
                            asm.pushI32(Pointer.fromObject(et).castToInt());
                            asm.pushR(R_SI);
                            asm.call(MethodPtrs.throwException);
                            perpForCall(2,0,ReturnMode.ONLY_THROW);
                        }
                        
                        if (ok) {
                            sub.squirtInstanceof(type,opcode,0);
                        }
                    }
                    break;
                }
                default: throw new Error("bad opcode: "+opcode);
                }
                
                fhc.visitTypeInsn(opcode,typedesc);
            }
            
            @Override
            public void visitMultiANewArrayInsn(UTF8Sequence desc,int dims) {
                Pointer type=getType(findStub(mbe.typeContext(), desc));
                
                asm.movRR(R_SP, R_CX);
                prepForCall(4, 0);
                asm.addIR((dims-1)*4, R_CX);
                asm.pushR(R_CX);
                asm.pushI32(dims);
                if (type!=Pointer.zero()) {
                    asm.pushI32(type.castToInt());
                } else {
                    asm.pushR(R_AX);
                }
                asm.pushR(R_SI);
                asm.call(MethodPtrs.multianewarray);
                perpForCall(4, 0, dims, ReturnMode.RETURN_OR_THROW);

                asm.pushR(R_AX);
                
                fhc.visitMultiANewArrayInsn(desc,dims);
            }
        };
    
    // support for snippet compilation
    
    public void generateForSnippet(int startPC) {
        this.startPC=startPC;
        
        // find the appropriate machineCode
        Pointer master=fivmr_MethodRec_findMC(methodRec,
                                              CVar.getInt("FIVMR_MC_KIND"),
                                              CVar.getInt("FIVMR_MC_BASELINE"));
        
        // figure out basepoints
        Pointer lastBasepoint=Pointer.zero();
        for (Pointer bp=CType.getPointer(master,"fivmr_MachineCode","bpList");
             bp!=Pointer.zero();
             bp=CType.getPointer(bp,"fivmr_Basepoint","next")) {
            if (logLevel>=2) {
                log(MethodImplGenerator.class,2,
                    "Examining basepoint "+bp.asLong());
            }
            
            // populate backward branch
            backwardBranches.put(
                CType.getInt(bp,"fivmr_Basepoint","bytecodePC"),
                new Branch(CType.getPointer(bp,"fivmr_Basepoint","machinecodePC")));
            
            // figure out if this one is the last one
            if (CType.getInt(bp,"fivmr_Basepoint","bytecodePC")>=startPC &&
                (lastBasepoint==Pointer.zero() ||
                 CType.getInt(bp,"fivmr_Basepoint","bytecodePC")
                 < CType.getInt(lastBasepoint,"fivmr_Basepoint","bytecodePC"))) {
                lastBasepoint=bp;
            }
        }
        
        if (lastBasepoint==Pointer.zero()) {
            // snippet is at the end of the codestream
            endPC=Integer.MAX_VALUE;
        } else {
            endPC=CType.getInt(lastBasepoint,"fivmr_Basepoint","bytecodePC");
        }
        
        if (logLevel>=2) {
            log(MethodImplGenerator.class,2,
                "Generating snippet from "+startPC+" to "+endPC+" followed by a "+
                (lastBasepoint==Pointer.zero()?"breakpoint":
                 "jump to "+
                 CType.getPointer(lastBasepoint,"fivmr_Basepoint","machinecodePC").asLong()));
        }
        
        mbe.extract(snippetCodeGenWrapper,
                    ClassReader.SKIP_FRAMES);
        
        if (Settings.ASSERTS_ON && !forwardBranches.isEmpty()) {
            throw new fivmError("forward branches not all cleared");
        }
        
        if (lastBasepoint!=Pointer.zero()) {
            // jump to the relevant basepoint
            asm.jmp(CType.getPointer(lastBasepoint,"fivmr_Basepoint","machinecodePC"));
        } else {
            if (Settings.ASSERTS_ON) {
                // the code should have jumped out before getting here
                asm.breakpoint();
            }
        }
    }
    
    int startPC;
    int endPC;
    
    final EmptyMethodVisitor snippetCodeGenWrapper=new EmptyMethodVisitor() {
            int curBCOffset;
            
            @Override
            public void visitBCOffset(int bcOffset) {
                curBCOffset=bcOffset;
                if (bcOffset>=startPC && bcOffset<endPC) {
                    instructionCodeGenerator.visitBCOffset(bcOffset);
                } else {
                    fhc.visitBCOffset(bcOffset);
                }
            }
            
            @Override
            public void visitLabel(Label l) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitLabel(l);
                } else {
                    fhc.visitLabel(l);
                }
            }
            
            @Override
            public void visitLineNumber(int line,Label start) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitLineNumber(line,start);
                } else {
                    fhc.visitLineNumber(line,start);
                }
            }
            
            @Override
            public void visitTryCatchBlock(Label start,
                                           Label end,
                                           Label handler,
                                           UTF8Sequence typeName) {
                // nothing to do: the non-snippet baseline code will already have generated
                // relevant try-catch blocks, and the exception handling helper will
                // use the non-snippet baseline code for exception dispatch
            }
            
            @Override
            public void visitInsn(int opcode) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitInsn(opcode);
                } else {
                    fhc.visitInsn(opcode);
                }
            }
            
            @Override
            public void visitFieldInsn(int opcode,
                                       UTF8Sequence owner,
                                       UTF8Sequence name,
                                       UTF8Sequence desc) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitFieldInsn(opcode,owner,name,desc);
                } else {
                    fhc.visitFieldInsn(opcode,owner,name,desc);
                }
            }
            
            @Override
            public void visitMethodInsn(int opcode,
                                        UTF8Sequence owner,
                                        UTF8Sequence name,
                                        UTF8Sequence desc) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitMethodInsn(opcode,owner,name,desc);
                } else {
                    fhc.visitMethodInsn(opcode,owner,name,desc);
                }
            }
            
            @Override
            public void visitIincInsn(int var,int increment) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitIincInsn(var,increment);
                } else {
                    fhc.visitIincInsn(var,increment);
                }
            }
            
            @Override
            public void visitIntInsn(int opcode,int operand) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitIntInsn(opcode,operand);
                } else {
                    fhc.visitIntInsn(opcode,operand);
                }
            }
            
            @Override
            public void visitVarInsn(int opcode,int var) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitVarInsn(opcode,var);
                } else {
                    fhc.visitVarInsn(opcode,var);
                }
            }
            
            @Override
            public void visitJumpInsn(int opcode,Label label) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitJumpInsn(opcode,label);
                } else {
                    fhc.visitJumpInsn(opcode,label);
                }
            }
            
            @Override
            public void visitLdcInsn(Object cst) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitLdcInsn(cst);
                } else {
                    fhc.visitLdcInsn(cst);
                }
            }
            
            @Override
            public void visitTableSwitchInsn(int min,int max,Label dflt,Label[] labels) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitTableSwitchInsn(min,max,dflt,labels);
                } else {
                    fhc.visitTableSwitchInsn(min,max,dflt,labels);
                }
            }
            
            @Override
            public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitLookupSwitchInsn(dflt,keys,labels);
                } else {
                    fhc.visitLookupSwitchInsn(dflt,keys,labels);
                }
            }
            
            @Override
            public void visitTypeInsn(int opcode,UTF8Sequence typedesc) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitTypeInsn(opcode,typedesc);
                } else {
                    fhc.visitTypeInsn(opcode,typedesc);
                }
            }
            
            @Override
            public void visitMultiANewArrayInsn(UTF8Sequence desc,int dims) {
                if (curBCOffset>=startPC && curBCOffset<endPC) {
                    instructionCodeGenerator.visitMultiANewArrayInsn(desc,dims);
                } else {
                    fhc.visitMultiANewArrayInsn(desc,dims);
                }
            }
        };
}

