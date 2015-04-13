/*
 * JNITrampolineGenerator.java
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
import static com.fiji.fivm.Constants.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.codegen.x86.X86Constants.*;
import com.fiji.fivm.codegen.Assembler.Branch;
import com.fiji.fivm.codegen.*;

public final class JNITrampolineGenerator extends Generator {
    Pointer jniImplementation;
    
    boolean needRefSave;
    
    int ffOffset;
        
    int nfSize;
    int fSize;
        
    int baseFOffset;

    int nfOffset;
    int fOffset;
        
    int fIdOffset;
    int fUpOffset;
    int fRefOffset;
        
    public void initSrc(Pointer methodRec) {
        super.initSrc(methodRec);
        
        // first things first: attempt to resolve the native method
        jniImplementation=resolveNative(methodRec);

        needRefSave=(methodFlags()&MBF_SYNCHRONIZED)!=0;
        
        ffOffset=needRefSave?Pointer.size():0;
        
        nfSize=CType.sizeof("fivmr_NativeFrame").castToInt();
        fSize=CType.sizeof("fivmr_Frame").sub(Pointer.zero()).add(ffOffset).castToInt();
        
        baseFOffset=0;

        nfOffset=baseFOffset-=nfSize;
        fOffset=baseFOffset-=fSize;
        
        fRefOffset=fOffset;
        fOffset+=ffOffset;
        fIdOffset=CType.offsetof("fivmr_Frame","id").castToInt()+fOffset;
        fUpOffset=CType.offsetof("fivmr_Frame","up").castToInt()+fOffset;
        
        while (((-baseFOffset+8)&15)!=0) {
            baseFOffset-=4;
        }
    }
    
    void popFrameAndRet() {
        asm.movMR(R_BP, fUpOffset, R_CX);
        asm.movRM(R_CX, R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt());
        asm.retleave();
    }
    
    // this assumes that the stack is 1 push away from being aligned
    void releaseLock() {
        // this CAN safepoint ... IllegalMonitorStateException.  of course
        // in that case we don't need to scan the stack for whatever
        // references we find here.  but we make this code more general
        // so that if the unlock() code wanted to safepoint for any other
        // reason, it would be able to do so.
        
        // two arguments: the receiver and the thread state
        
        asm.pushABunch(3);
        
        if ((methodFlags()&BF_STATIC)!=0) {
            asm.pushI32(
                CType.getPointer(
                    CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                    "fivmr_TypeData","classObject").castToInt());
        } else {
            asm.pushM(R_BP, 12);
        }
        
        asm.pushR(R_SI);
        
        asm.call(MethodPtrs.unlock);
        
        asm.cmpMI(R_SI,
                  CType.offsetof("fivmr_ThreadState","curException").castToInt(),
                  0);
        Branch b=asm.setBranchSourceAndJcc(J_E);
        
        produceNullReturn();
        popFrameAndRet();
        
        asm.setBranchTarget(b);
        
        asm.popABunch(5);
    }
    
    public void generate() {
        // FIXME: this will have to be converted to do 16-byte alignment instead
        // of 8-byte alignment.  and that will be painful.
        
        // create the two debugid's we'll need
        Pointer noRefs=code.allocDebugID(0,0,0,new int[0]);
        Pointer oneRef=Pointer.zero();

        if (needRefSave) {
            // if the method is synchronized then we have to save either the exception
            // or the return value across calls
            
            oneRef=code.allocDebugID(0,0,1,new int[]{1});
        }
        
        // generate the prologue
        asm.pushR(R_BP);
        asm.movRR(R_SP,R_BP);
        
        // stack is not aligned!
        
        asm.subIR(-baseFOffset,R_SP);
        
        // stack is aligned
        
        asm.movMR(R_BP, 8, R_SI);
        
        // set up frame
        asm.movMR(R_SI, CType.offsetof("fivmr_ThreadState","curF").castToInt(), R_AX);
        asm.movRM(R_AX, R_BP, fUpOffset);
        asm.leaMR(R_BP, fOffset, R_CX);
        asm.movRM(R_CX, R_SI, CType.offsetof("fivmr_ThreadState", "curF").castToInt());
        
        // Frame::id
        asm.movIM(noRefs.castToInt(), R_BP, fIdOffset);

        // stack is aligned
        
        // lock the lock if there is a lock
        
        if ((methodFlags()&MBF_SYNCHRONIZED)!=0) {
            // two arguments: the receiver and the thread state
            
            asm.pushR(R_AX);
            asm.pushR(R_AX);
            
            if ((methodFlags()&BF_STATIC)!=0) {
                asm.pushI32(
                    CType.getPointer(
                        CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                        "fivmr_TypeData","classObject").castToInt());
            } else {
                asm.pushM(R_BP, 12);
            }
            
            asm.pushR(R_SI);
            
            asm.call(MethodPtrs.lock);
            
            asm.cmpMI(R_SI,
                      CType.offsetof("fivmr_ThreadState","curException").castToInt(),
                      0);
            Branch b=asm.setBranchSourceAndJcc(J_E);
            
            produceNullReturn();
            popFrameAndRet();
            
            asm.setBranchTarget(b);
            
            asm.addIR(16, R_SP);
        }
        
        // stack is aligned again
        
        // initialize the native frame
        
        asm.pushR(R_AX); // align for call
        asm.pushI32(methodRec.castToInt());
        asm.leaMR(R_BP, nfOffset, R_CX);
        asm.pushR(R_CX);
        asm.pushR(R_SI);
        asm.call(CVar.addressOf("fivmr_ThreadState_pushAndInitNF"));

        int stackOffset=4;
        
        // now ... marshal the arguments.  for this we need the native frame in hand;
        // that's why it's still in ecx
        
        int nargs=0;
        int nparams=0;
        
        nargs++; // jni env
        nparams++; // thread state
        
        nargs++; // receiver (pass Class object for static methods)
        if ((methodFlags()&BF_STATIC)==0) {
            nparams++; // receiver
        }
        
        for (int i=0;i<fivmr_MethodRec_nparams(methodRec);++i) {
            int cells=Types.cells(
                (char)fivmr_TypeData_name(fivmr_MethodRec_param(methodRec,i)).loadByte());
            nargs+=cells;
            nparams+=cells;
        }
        
        int npop=nargs;
        int npushed=0;

        // make sure that the stack is aligned after the args are pushed
        while ((npop&3)!=0) {
            npop++;
            npushed++;
            stackOffset--;
        }
        
        // push arguments in reverse order, marshalling as necessary
        
        int paramidx=nparams;
        
        for (int i=fivmr_MethodRec_nparams(methodRec)-1;i>=-1;i--) {
            char type;
            
            if (i==-1) {
                type='L';
            } else {
                type=(char)fivmr_TypeData_name(fivmr_MethodRec_param(methodRec,i)).loadByte();
            }
            
            if (Types.ref(type)) {
                // we will be pushing 3 things
                
                // align for the call
                int npopNow=0;
                while (((npushed+3+npopNow)&3)!=0) {
                    npopNow++;
                    stackOffset--;
                }
                
                asm.pushOrPopABunch(stackOffset);
                stackOffset=0;
                
                npopNow+=3;
                
                if (i==-1 && (methodFlags()&BF_STATIC)!=0) {
                    asm.pushI32(
                        CType.getPointer(
                            CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                            "fivmr_TypeData","classObject").castToInt());
                } else {
                    asm.pushM(R_BP, 8+(--paramidx)*4);
                }
                asm.pushR(R_SI);
                asm.leaMR(R_BP, nfOffset, R_CX);
                asm.pushR(R_CX);
                asm.call(CVar.addressOf("fivmr_NativeFrame_addHandle"));
                
                asm.popABunch(npopNow);
                
                asm.pushR(R_AX); // push the handle
                npushed++;
            } else {
                asm.pushOrPopABunch(stackOffset);
                stackOffset=0;
                for (int j=0;j<Types.cells(type);++j) {
                    asm.pushM(R_BP, 8+(--paramidx)*4);
                    npushed++;
                }
            }
        }
        
        // right now the stack is misaligned by 1; use that to our advantage

        // enter native
        asm.pushR(R_SI);
        asm.call(CVar.addressOf("fivmr_ThreadState_goToNative"));
        asm.popR(R_CX);

        // push the final argument
        asm.leaMR(R_BP,
                  nfOffset+CType.offsetof("fivmr_NativeFrame","jni").castToInt(),
                  R_AX);
        asm.pushR(R_AX);
        
        // stack is aligned again.
        
        // call the JNI method
        
        asm.call(jniImplementation);
        
        // save the result while ensuring that the stack is still aligned
        // note, npop must be at least 4!
        
        if (Settings.ASSERTS_ON && npop<4) {
            throw new fivmError("npop has weird value: "+npop);
        }
        
        char exitType=(char)
            fivmr_TypeData_name(fivmr_MethodRec_result(methodRec)).loadByte();
        
        // this leaves stack slots available for leaving native,
        // marshaling, and popping the native frame.
        
        // save result, ensure that the stack is misaligned by 1
        switch (exitType) {
        case 'V':
            asm.popR(R_CX);
            break;
        case 'J':
            asm.popABunch(3);
            asm.pushR(R_AX);
            asm.pushR(R_DX);
            break;
        case 'F':
            asm.popR(R_CX);
            asm.movRR(R_SP, R_AX);
            asm.fstpM32(R_AX, 0);
            break;
        case 'D':
            asm.popR(R_CX);
            asm.movRR(R_SP, R_AX);
            asm.fstpM64(R_AX, 4); // save at aligned address
            break;
        default:
            asm.popABunch(2);
            asm.pushR(R_AX);
            break;
        }
        
        // stack is misaligned by 1
        
        // leave native
        asm.pushR(R_SI);
        asm.call(CVar.addressOf("fivmr_ThreadState_goToJava"));
        asm.popR(R_AX);
        
        // check exception
        
        asm.movMR(R_SI,
                  CType.offsetof("fivmr_ThreadState","curExceptionHandle").castToInt(),
                  R_CX);
        asm.testRR(R_CX, R_CX);
        
        Branch b=asm.setBranchSourceAndJcc(J_NE);
        
        // normal return
        
        // marshal result
        
        if (Types.ref(exitType)) {
            asm.popR(R_DX);
            asm.testRR(R_DX, R_DX);
            Branch nullCase=asm.setBranchSourceAndJccShort(J_E);
            
            asm.movMR(R_DX, CType.offsetof("fivmr_Handle","obj").castToInt(), R_DX);
            
            asm.setBranchTarget(nullCase);
            
            if (needRefSave) {
                asm.pushR(R_AX); // bring the stack back to where it was
                asm.movRM(R_DX, R_BP, fRefOffset);
                asm.movIM(oneRef.castToInt(), R_BP, fIdOffset);
            } else {
                asm.pushR(R_DX);
            }
        }
        
        // pop native frame
        
        asm.pushR(R_SI);
        asm.call(CVar.addressOf("fivmr_ThreadState_popNF"));
        asm.popR(R_AX);

        // release locks
        if ((methodFlags()&MBF_SYNCHRONIZED)!=0) {
            releaseLock();
        }
        
        // restore result
        switch (exitType) {
        case 'V':
            break;
        case 'J':
            asm.popR(R_DX);
            asm.popR(R_AX);
            break;
        case 'F':
            asm.movRR(R_SP, R_AX);
            asm.fldM32(R_AX, 0);
            break;
        case 'D':
            asm.movRR(R_SP, R_AX);
            asm.fldM64(R_AX, 4);
            break;
        default:
            if (needRefSave && Types.ref(exitType)) {
                asm.movMR(R_BP, fRefOffset, R_AX);
            } else {
                asm.popR(R_AX);
            }
            break;
        }

        // ... and return
        popFrameAndRet();
        
        asm.setBranchTarget(b);
        
        // exceptional return
        
        // clear the exception
        asm.movIM(0,
                  R_SI,
                  CType.offsetof("fivmr_ThreadState","curExceptionHandle").castToInt());
        
        // get the object
        asm.movMR(R_CX, CType.offsetof("fivmr_Handle","obj").castToInt(), R_CX);
        
        if ((methodFlags()&MBF_SYNCHRONIZED)!=0) {
            asm.popR(R_AX);
            asm.movRM(R_CX, R_BP, fRefOffset);
            asm.movIM(oneRef.castToInt(), R_BP, fIdOffset);
        } else {
            asm.popR(R_AX);
            asm.popR(R_AX);
            asm.pushR(R_CX);
        }
        
        // pop native frame
        
        asm.pushR(R_SI);
        asm.call(CVar.addressOf("fivmr_ThreadState_popNF"));
        asm.popR(R_AX);

        // release locks
        if ((methodFlags()&MBF_SYNCHRONIZED)!=0) {
            releaseLock();
        }
        
        // restore exception
        if ((methodFlags()&MBF_SYNCHRONIZED)!=0) {
            asm.movMR(R_BP, fRefOffset, R_AX);
        } else {
            asm.popR(R_AX);
        }

        // set the exception
        asm.movRM(R_AX,
                  R_SI,
                  CType.offsetof("fivmr_ThreadState","curException").castToInt());
        
        // return
        produceNullReturn();
        popFrameAndRet();
        
        if (Settings.ASSERTS_ON) {
            asm.breakpoint();
        }
    }
}


