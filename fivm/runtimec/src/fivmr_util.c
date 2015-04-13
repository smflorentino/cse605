/*
 * fivmr_util.c
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

#include "fivmr.h"

bool fivmr_abortThrow;

void fivmr_describeException(fivmr_ThreadState *ts,
                             fivmr_Handle *h) {
    fivmr_describeExceptionImpl(ts,h);
    if (ts->curExceptionHandle!=NULL) {
	fivmr_Handle *h2=ts->curExceptionHandle;
	ts->curExceptionHandle=NULL;
	LOG(0,("Thread %u: Caught exception: %s",
	       ts->id,fivmr_TypeData_forObject(&ts->vm->settings,h->obj)->name));
	LOG(0,("Thread %u: Could not print stack trace because: %s",
	       ts->id,fivmr_TypeData_forObject(&ts->vm->settings,h2->obj)->name));
	fivmr_ThreadState_removeHandle(ts,h2);
    }
}

void fivmr_assertNoException(fivmr_ThreadState *ts,
                             const char *context) {
    if (ts->curException!=0 || ts->curExceptionHandle!=NULL) {
	fivmr_Object exception=0;
	fivmr_Log_lockedPrintf("Fatal error in fivm runtime system: %s\n",context);
	if (ts->curException!=0) {
	    fivmr_Handle h;
	    fivmr_assert(ts->curExceptionHandle==NULL);
	    exception=h.obj=ts->curException;
	    ts->curException=0;
	    fivmr_ThreadState_goToNative(ts); /* hack.  should have a way of describing
						 exceptions when we're already
						 IN_JAVA */
	    fivmr_describeException(ts,&h);
	} else if (ts->curExceptionHandle!=NULL) {
	    fivmr_Handle *h;
	    fivmr_assert(ts->curException==0);
	    h=ts->curExceptionHandle;
	    exception=h->obj;
	    ts->curExceptionHandle=NULL;
	    fivmr_describeException(ts,h);
	}
	abort();
    }
}

void fivmr_throw(fivmr_ThreadState *ts,
                 fivmr_Object obj) {
    int logAtLevel;
    
    if (obj==0) {
        fivmr_throwNullPointerRTE_inJava(ts);
        return;
    }
    
    if (ts->vm->exceptionsFatalReason!=NULL || fivmr_abortThrow) {
        const char *reason;
        reason=ts->vm->exceptionsFatalReason;
        if (reason==NULL) {
            reason="Unexpected exception (fivmr_abortThrow=true)";
        }
	fivmr_Log_lockedPrintf("Thread %u: %s: %s (%p)\n",
			       ts->id,
			       reason,
			       fivmr_TypeData_forObject(&ts->vm->settings,obj)->name,
			       obj);
	fivmr_ThreadState_dumpStackFor(ts);
	fivmr_abortf("%s.",reason);
    }
    
    if ((ts->vm->flags&FIVMR_VMF_LOG_THROW)) {
        logAtLevel=0;
    } else {
        logAtLevel=3;
    }

    if (LOGGING(logAtLevel)) {
	LOG(logAtLevel,
            ("Thread %u: throwing exception %p with type %s",
             ts->id,obj,fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
	fivmr_ThreadState_dumpStackFor(ts);
    }
    ts->curException=obj;
}

void fivmr_throwOOME(fivmr_ThreadState *ts) {
    fivmr_throwOutOfMemoryError_inJava(ts);
}

fivmr_Handle *fivmr_newGlobalHandle(fivmr_ThreadState *ts,
				    fivmr_Handle *h) {
    fivmr_Handle *result;
    fivmr_Lock_lock(&ts->vm->hrLock);
    result=fivmr_HandleRegion_add(&ts->vm->hr,
				  ts,
				  &ts->vm->freeHandles,
				  fivmr_Handle_get(h));
    fivmr_Lock_unlock(&ts->vm->hrLock);
    return result;
}

void fivmr_deleteGlobalHandle(fivmr_Handle *h) {
    fivmr_Lock_lock(&h->vm->hrLock);
    fivmr_Handle_remove(&h->vm->freeHandles,h);
    fivmr_Lock_unlock(&h->vm->hrLock);
}

void fivmr_OTH_init(fivmr_OTH *oth,
                    uintptr_t n) {
    uintptr_t size;
    oth->n=n;
    size=fivmr_OTH_calcSize(n);
    oth->list=fivmr_mallocAssert(size);
    bzero(oth->list,size);
}

void fivmr_OTH_initEasy(fivmr_OTH *oth,
                        uintptr_t numEle) {
    fivmr_OTH_init(oth,numEle*3/2);
}

void fivmr_OTH_free(fivmr_OTH *oth) {
    if (oth->n>0) {
        fivmr_assert(oth->list!=NULL);
        fivmr_free(oth->list);
    } else {
        fivmr_assert(oth->list==NULL);
    }
}

void fivmr_OTH_clear(fivmr_OTH *oth) {
    bzero(oth->list,fivmr_OTH_calcSize(oth->n));
}

bool fivmr_OTH_put(fivmr_OTH *oth,
                   void *key,
                   void *val) {
    uintptr_t i;
    uintptr_t start;
    fivmr_assert(key!=0);
    start=fivmr_OTH_ptrHash((uintptr_t)key);
    i=start%oth->n;
    for (;;) {
        uintptr_t pkey=oth->list[i*2];
        if (pkey==(uintptr_t)key) {
            oth->list[i*2+1]=(uintptr_t)val;
            return false;
        } else if (pkey==0) {
            oth->list[i*2]=(uintptr_t)key;
            oth->list[i*2+1]=(uintptr_t)val;
            return true;
        }
        i++;
        if (i==oth->n) i=0;
        fivmr_assert(i!=(start%oth->n)); /* assert that hashtable is not full */
    }
    fivmr_assert(!"not reached");
    return false;
}

void *fivmr_OTH_get(fivmr_OTH *oth,
                    void *key) {
    uintptr_t i;
    uintptr_t start;
    start=fivmr_OTH_ptrHash((uintptr_t)key);
    i=start%oth->n;
    for (;;) {
        uintptr_t pkey=oth->list[i*2];
        if (pkey==0) {
            return NULL;
        } else if (pkey==(uintptr_t)key) {
            return (void*)oth->list[i*2+1];
        }
        i++;
        if (i==oth->n) i=0;
        fivmr_assert(i!=(start%oth->n)); /* assert that hashtable is not full */
    }
    fivmr_assert(!"not reached");
    return NULL;
}

int32_t fivmr_basetypeSize(char c) {
    switch (c) {
    case 'V':
        return 0;
    case 'Z':
    case 'B':
        return 1;
    case 'S':
    case 'C':
        return 2;
    case 'I':
    case 'F':
        return 4;
    case 'J':
    case 'D':
        return 8;
    case 'P':
    case 'f':
    case 'L':
    case '[':
        return sizeof(void*);
    default:
        fivmr_abortf("Wrong basetype character: '%d'",(int)c);
        return 0;
    }
}

int32_t fivmr_Baseline_offsetToJStack(fivmr_MethodRec *mr) {
    int32_t result;
    
    fivmr_assert(FIVMR_X86);
    
    result=0;
    
    result-=4; /* esi */
    result-=4; /* Frame::up */
    result-=4; /* Frame::debugID */
    
    if ((mr->flags&FIVMR_MBF_SYNCHRONIZED) &&
        !(mr->flags&FIVMR_BF_STATIC)) {
        result-=4; /* receiver */
    }
    
    result-=4*mr->nLocals;
    
    return result;
}

int32_t fivmr_Baseline_offsetToSyncReceiver(fivmr_MethodRec *mr) {
    int32_t result;
    
    fivmr_assert(FIVMR_X86);
    
    result=0;
    
    result-=4; /* esi */
    result-=4; /* Frame::up */
    result-=4; /* Frame::debugID */
    
    result-=4; /* and the receiver */
    
    return result;
}

/* FIXME: maybe it'd be better if we retried resolution every time, like we
   do patch points?  instead of installing a thunk that throws an exception? */

void fivmr_resolveField(fivmr_ThreadState *ts,
                        uintptr_t returnAddr,
                        fivmr_BaseFieldAccess *bfa) {
    ts->curF->id=bfa->debugID;
    fivmr_handleFieldResolution(ts,returnAddr,bfa);
}

void fivmr_resolveMethod(fivmr_ThreadState *ts,
                         uintptr_t returnAddr,
                         fivmr_BaseMethodCall *bmc) {
    /* this function has a purpose.  if you think that it doesn't, and try to
       "factor it out", then you are a fool.  so look carefully, and study
       hard, before making a retarded decision that someone smarter than you
       will have to back out later. */
    fivmr_handleMethodResolution(ts,ts->curF->id,returnAddr,bmc);
}

void fivmr_resolveArrayAlloc(fivmr_ThreadState *ts,
                             uintptr_t returnAddr,
                             fivmr_BaseArrayAlloc *baa) {
    ts->curF->id=baa->debugID;
    fivmr_handleArrayAlloc(ts,returnAddr,baa);
}

void fivmr_resolveObjectAlloc(fivmr_ThreadState *ts,
                              uintptr_t returnAddr,
                              fivmr_BaseObjectAlloc *boa) {
    ts->curF->id=boa->debugID;
    fivmr_handleObjectAlloc(ts,returnAddr,boa);
}

void fivmr_resolveInstanceof(fivmr_ThreadState *ts,
                             uintptr_t returnAddr,
                             fivmr_BaseInstanceof *bio) {
    ts->curF->id=bio->debugID;
    fivmr_handleInstanceof(ts,returnAddr,bio);
}

void fivmr_handlePatchPoint(fivmr_ThreadState *ts,
                            const char *className,
                            const char *fromWhereDescr,
                            int bcOffset,
                            void **patchThunkPtrPtr,
                            void *origPatchThunk) {
    fivmr_handlePatchPointImpl(ts,ts->curF->id,className,fromWhereDescr,
                               bcOffset,patchThunkPtrPtr,origPatchThunk);
}

void fivmr_baselineThrow(fivmr_ThreadState *ts,
                         uintptr_t framePtr,
                         uintptr_t *result) {
    /* what this does:
       1) gets the MachineCode and MethodRec for the current method
       2) gets the try catch metadata
       3) searches for a handler
       4a) if one is found, finds the machinecode address, and sets
           result appropriately
       4b) if one is not found, sets the result to indicate return */
    
    fivmr_DebugRec *dr;
    fivmr_MachineCode *mc;
    fivmr_MethodRec *mr;
    fivmr_BaseTryCatch *btc;
    fivmr_BaseTryCatch *correctBTC;
    int32_t bytecodePC;
    fivmr_Object exc;
    fivmr_TypeData *excType;
    
    fivmr_assert(FIVMR_CAN_DO_CLASSLOADING);
    
    LOG(2,("Attempting to dispatch exception in baseline code; "
           "Thread #%u, framePtr = %p, resultPtr = %p",
           ts,framePtr,result));
    
    fivmr_assert(ts->curException!=0);
    fivmr_assert(ts->curExceptionHandle==NULL);
    
    /* find the MethodRec */
    
    dr=fivmr_DebugRec_lookup(ts->vm,ts->curF->id);
    
    LOG(2,("have DebugRec at %p",dr));
    LOG(2,("line number = %d, pc = %d",
           fivmr_DebugRec_getLineNumber(dr),
           fivmr_DebugRec_getBytecodePC(dr)));
    
    fivmr_assert(fivmr_MachineCode_isMachineCode(dr->method));
    
    mr=fivmr_MachineCode_decodeMethodRec(dr->method);
    
    LOG(2,("have MethodRec at %p",mr));
    LOG(2,("method is %s",fivmr_MethodRec_describe(mr)));
    
    /* find the canonical baseline machinecode.  this is intentionally different
       than the MachineCode we found in the DebugRec, because that MachineCode
       may be a BASE_PATCH. */
    
    fivmr_ThreadState_goToNative(ts);
    mc=fivmr_MethodRec_findMC(mr,FIVMR_MC_KIND,FIVMR_MC_BASELINE);
    fivmr_ThreadState_goToJava(ts);

    fivmr_assert(mc!=NULL);
    fivmr_assert((mc->flags&FIVMR_MC_KIND)==FIVMR_MC_BASELINE);
    fivmr_assert((mc->flags&FIVMR_MC_POSSIBLE_ENTRYPOINT));
    
    /* figure out the bytecode PC */
    
    bytecodePC=fivmr_DebugRec_getBytecodePC(dr);
    
    /* figure out what the exception is, and its type */
    
    exc=ts->curException;
    excType=fivmr_Object_getTypeData(&ts->vm->settings,exc);
    
    /* figure out which catch block, if any, applies */
    correctBTC=NULL;
    for (btc=mc->btcFirst;btc!=NULL;btc=btc->next) {
        if (bytecodePC>=btc->start &&
            bytecodePC<btc->end) {
            /* possible candidate; check if the type is resolved.  if it isn't
               then we know that the exception cannot be a subtype of this catch
               block's type since there is no way to allocate something of
               unresolved type. */
            fivmr_TypeData *catchType=NULL; /* make GCC happy */
            if (btc->type!=NULL) {
                catchType=fivmr_TypeStub_tryGetTypeData(btc->type);
            }
            if (btc->type==NULL ||
                (catchType!=NULL &&
                 fivmr_TypeData_isSubtypeOf(ts,excType,catchType))) {
                correctBTC=btc;
                break;
            }
        }
    }
    
    if (correctBTC==NULL) {
        fivmr_Object receiver;
        
        LOG(2,("Telling method to return with exception"));
        
        /* easy case: we just tell the method to return with an exception */
        result[0]=0;
        result[1]=0;
        
        /* release any locks */
        if ((mr->flags&FIVMR_MBF_SYNCHRONIZED)) {
            if ((mr->flags&FIVMR_BF_STATIC)) {
                receiver=mr->owner->classObject;
            } else {
                receiver=*(fivmr_Object*)(
                    framePtr+fivmr_Baseline_offsetToSyncReceiver(mr));
            }
            
            fivmr_Object_unlock(ts,receiver);
        }
        
        /* pop the frame */
        ts->curF=ts->curF->up;
        
        /* done */
    } else {
        fivmr_Basepoint *bp;
        fivmr_Basepoint *correctBP;
        
        /* hard case: find the machinecode PC to jump to */
        correctBP=NULL;
        for (bp=mc->bpList;bp!=NULL;bp=bp->next) {
            if (bp->bytecodePC == correctBTC->target) {
                correctBP=bp;
                break;
            }
        }
        
        /* we have to be able to find the basepoint.  the compiler will
           ensure this. */
        fivmr_assert(correctBP!=NULL);
        
        result[0]=(uintptr_t)fivmr_Baseline_offsetToJStack(mr);
        result[1]=(uintptr_t)correctBP->machinecodePC;
        
        LOG(2,("Telling method to jump to %p and set stack to FP + %p",
               result[1],result[0]));

        /* done */
    }
    
    fivmr_MachineCode_down(mc);
}

