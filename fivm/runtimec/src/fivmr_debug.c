/*
 * fivmr_debug.c
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

#include <fivmr.h>

uintptr_t fivmr_iterateDebugFrames(fivmr_VM *vm,
                                   fivmr_Frame *f,
				   fivmr_DebugFrameCback cback,
				   uintptr_t arg) {
    uintptr_t result;
    fivmr_MethodRec *lastMR=NULL;

    for (;f!=NULL;f=f->up) {
	LOG(10,("observed f = %p, id = %p",
		f,f->id));
	fivmr_assert(f!=f->up);
	if (f->id!=(uintptr_t)(intptr_t)-1) {
	    fivmr_DebugRec *dr;
	    int32_t lineNumber;
	    
	    dr=fivmr_DebugRec_lookup(vm,f->id);
	    
	    LOG(10,("have dr = %p, dr->ln_rm_c = %p, dr->method = %p",
		    dr,dr->ln_rm_c,dr->method));

	    if ((dr->ln_rm_c&FIVMR_DR_FAT)) {
		lineNumber=fivmr_DebugRec_decodeFatDebugData(dr->ln_rm_c)->lineNumber;
	    } else {
		lineNumber=fivmr_DebugRec_decodeThinLineNumber(dr->ln_rm_c);
	    }
	    
	    if (dr->ln_rm_c&(FIVMR_DR_INLINED)) {
		fivmr_InlineMethodRec *imr;
                bool isPatchPoint;
                isPatchPoint=fivmr_MachineCode_isPatchPoint(dr->method);
                imr=(fivmr_InlineMethodRec*)(dr->method&~(sizeof(void*)-1));
		for (;;) {
                    if (!isPatchPoint ||
                        lastMR!=imr->method) {
                        LOG(10,("calling cback(%p, %s, %d)",
                                arg,fivmr_MethodRec_describe(imr->method),lineNumber));
                        if ((result=cback(vm,
                                          arg,
                                          lastMR=imr->method,
                                          lineNumber))) {
                            return result;
                        }
                    }
                    isPatchPoint=false;
		    lineNumber=fivmr_InlineMethodRec_decodeLineNumber(imr->ln_c);
		    if (imr->ln_c&(FIVMR_IMR_INLINED)) {
			imr=(fivmr_InlineMethodRec*)imr->caller;
		    } else {
			LOG(10,("calling cback(%p, %s, %d)",
				arg,
				fivmr_MachineCode_decodeMethodRec(imr->caller),
				lineNumber));
			if ((result=cback(
                                 vm,
                                 arg,
                                 lastMR=fivmr_MachineCode_decodeMethodRec(imr->caller),
                                 lineNumber))) {
			    return result;
			}
			break;
		    }
		}
	    } else {
                if (!fivmr_MachineCode_isPatchPoint(dr->method) ||
                    lastMR!=fivmr_MachineCode_decodeMethodRec(dr->method)) {
                    LOG(10,("calling cback(%p, %s, %d)",
                            arg,
                            fivmr_MachineCode_decodeMethodRec(dr->method),
                            lineNumber));
                    if ((result=cback(
                             vm,
                             arg,
                             lastMR=fivmr_MachineCode_decodeMethodRec(dr->method),
                             lineNumber))) {
                        return result;
                    }
                }
	    }
	}
	fivmr_assert(f!=f->up);
    }
    
    return 0;
}

static uintptr_t stackDumpCback(fivmr_VM *vm,
                                uintptr_t arg,
				fivmr_MethodRec *mr,
				int32_t lineNumber) {
    const char *filename;
    if (mr->owner->filename==NULL) {
	filename="<unavailable>";
    } else {
	filename=mr->owner->filename;
    }
    fivmr_Log_printf("   %s (%s:%d)\n",
		     fivmr_MethodRec_describe(mr),
		     filename,
		     lineNumber);
    return 0;
}

void fivmr_dumpStackFromNoHeading(fivmr_VM *vm,
                                  fivmr_Frame *f) {
    fivmr_iterateDebugFrames(vm,f,stackDumpCback,0);
    fivmr_Log_printf("   (end stack dump)\n");
}

void fivmr_dumpStackFrom(fivmr_VM *vm,
                         fivmr_Frame *f,
                         const char *msg) {
    fivmr_Log_lock();
    if (msg!=NULL) {
	fivmr_Log_printf("fivmr stack dump for %s:\n",msg);
    } else {
	fivmr_Log_printf("fivmr stack dump:\n");
    }
    fivmr_dumpStackFromNoHeading(vm,f);
    fivmr_Log_unlock();
}

static uintptr_t fivmr_iterateDebugFrames_forJava_cback(fivmr_VM *vm,
                                                        uintptr_t arg,
                                                        fivmr_MethodRec *mr,
                                                        int32_t lineNumber) {
    return fivmr_DumpStackCback_cback(fivmr_ThreadState_get(vm),
                                      arg,
                                      mr,
                                      lineNumber);
}

uintptr_t fivmr_iterateDebugFrames_forJava(fivmr_VM *vm,
                                           fivmr_Frame *f,
					   fivmr_Object cback) {
    return fivmr_iterateDebugFrames(vm,
                                    f,
				    fivmr_iterateDebugFrames_forJava_cback,
				    cback);
}

static uintptr_t methodForStackDepthCback(fivmr_VM *vm,
                                          uintptr_t arg,
					  fivmr_MethodRec *mr,
					  int32_t lineNumber) {
    int32_t *cnt=(int32_t*)arg;
    if ((*cnt)--) {
	return 0;
    } else {
	return (uintptr_t)mr;
    }
}

fivmr_MethodRec *fivmr_methodForStackDepth(fivmr_VM *vm,
                                           fivmr_Frame *f,
					   int32_t depth) {
    return (fivmr_MethodRec*)fivmr_iterateDebugFrames(vm,
                                                      f,
						      methodForStackDepthCback,
						      (uintptr_t)&depth);
}

fivmr_MethodRec *fivmr_findCaller(fivmr_ThreadState *ts,
				  int32_t depth) {
    if (depth<0) {
	return ts->curNF->jni.mr;
    } else {
	return fivmr_methodForStackDepth(ts->vm,ts->curF,depth);
    }
}

static fivmr_ThreadStackTrace *alloc_tst(void) {
    fivmr_ThreadStackTrace *result=
	(fivmr_ThreadStackTrace*)fivmr_mallocAssert(sizeof(fivmr_ThreadStackTrace));
    bzero(result,sizeof(fivmr_ThreadStackTrace));
    return result;
}

static void tst_init1(fivmr_ThreadStackTrace *tst,
		      fivmr_ThreadState *ts) {
    tst->thread=fivmr_newGlobalHandle(fivmr_ThreadState_get(ts->vm),ts->vm->javaThreads+ts->id);
}

static uintptr_t tst_init2_cback(fivmr_VM *vm,
                                 uintptr_t arg,
				 fivmr_MethodRec *mr,
				 int32_t lineNumber) {
    fivmr_ThreadStackTrace *tst;
    fivmr_StackTraceFrame *stf;
    
    tst=(fivmr_ThreadStackTrace*)arg;

    stf=(fivmr_StackTraceFrame*)fivmr_mallocAssert(sizeof(fivmr_StackTraceFrame));

    stf->mr=mr;
    stf->lineNumber=lineNumber;
    stf->next=tst->top;
    tst->top=stf;
    
    tst->depth++;
    
    return 0;
}

static void tst_init2(fivmr_ThreadStackTrace *tst,
		      fivmr_ThreadState *ts) {
    fivmr_StackTraceFrame *stf,*prev;
    
    fivmr_Lock_lock(&ts->lock);
    
    tst->depth=0;
    tst->top=NULL;
    
    fivmr_iterateDebugFrames(ts->vm,ts->curF,tst_init2_cback,(uintptr_t)tst);
    
    /* invert the stack trace */
    stf=tst->top;
    prev=NULL;
    while (stf!=NULL) {
	fivmr_StackTraceFrame *next=stf->next;
	stf->next=prev;
	prev=stf;
	stf=next;
    }
    tst->top=prev;
    
    tst->execStatus=ts->execStatus;
    tst->execFlags=ts->execFlags;
    
    fivmr_Lock_unlock(&ts->lock);
}

fivmr_ThreadStackTrace *fivmr_ThreadStackTrace_get(fivmr_ThreadState *ts) {
    fivmr_ThreadStackTrace *tst=alloc_tst();
    tst_init1(tst,ts);
    tst_init2(tst,ts);
    return tst;
}

/* can only be called by a thread that is IN_JAVA */
static void doTraceStackForHandshake(fivmr_ThreadState *ts) {
    fivmr_ThreadStackTrace *tst=fivmr_ThreadStackTrace_get(ts);
    tst->next = ts->vm->atst_result->first;
    ts->vm->atst_result->first = tst;
    ts->vm->atst_result->numThreads++;
}

void fivmr_Debug_traceStack(fivmr_ThreadState *ts) {
    LOG(2,("grabbing debug stack trace for Thread %u",ts->id));
    fivmr_assert((ts->execFlags&FIVMR_TSEF_TRACE_STACK)!=0);
    ts->execFlags&=~FIVMR_TSEF_TRACE_STACK;
    if (fivmr_ThreadState_isRunning(ts)) {
	if (fivmr_ThreadState_isInJava(ts)) {
	    doTraceStackForHandshake(ts);
	} else {
	    fivmr_ThreadState *curts=fivmr_ThreadState_get(ts->vm);
	    fivmr_ThreadState_goToJava(curts);
	    doTraceStackForHandshake(ts);
	    fivmr_ThreadState_goToNative(curts);
	}
    }
    if (LOGGING(2)) {
	fivmr_ThreadState_dumpStackFor(ts);
    }
}

fivmr_AllThreadStackTraces *fivmr_AllThreadStackTraces_get(fivmr_VM *vm) {
    fivmr_ThreadState *curts=fivmr_ThreadState_get(vm);
    fivmr_AllThreadStackTraces *result;
    
    fivmr_ThreadState_goToNative(curts);

    fivmr_Lock_lock(&vm->handshakeLock);
    
    vm->atst_result=(fivmr_AllThreadStackTraces*)
	fivmr_mallocAssert(sizeof(fivmr_AllThreadStackTraces));
    vm->atst_result->first=NULL;
    vm->atst_result->numThreads=0;
    
    fivmr_ThreadState_softHandshake(vm,
                                    FIVMR_TSEF_JAVA_HANDSHAKEABLE,
				    FIVMR_TSEF_TRACE_STACK);
    
    result=vm->atst_result;
    vm->atst_result=NULL;
    
    fivmr_Lock_unlock(&vm->handshakeLock);

    fivmr_ThreadState_goToJava(curts);
    
    return result;
}

void fivmr_ThreadStackTrace_free(fivmr_ThreadStackTrace *tst) {
    fivmr_StackTraceFrame *stf;
    for (stf=tst->top;stf!=NULL;) {
	fivmr_StackTraceFrame *next=stf->next;
	fivmr_free(stf);
	stf=next;
    }
    fivmr_deleteGlobalHandle(tst->thread);
    fivmr_free(tst);
}

void fivmr_AllThreadStackTraces_free(fivmr_AllThreadStackTraces *atst) {
    fivmr_ThreadStackTrace *tst;
    for (tst=atst->first;tst!=NULL;) {
	fivmr_ThreadStackTrace *next=tst->next;
	fivmr_ThreadStackTrace_free(tst);
	tst=next;
    }
    fivmr_free(atst);
}

void fivmr_Debug_dumpAllStacks(fivmr_VM *vm) {
    fivmr_ThreadState_softHandshake(vm,
                                    FIVMR_TSEF_JAVA_HANDSHAKEABLE,
				    FIVMR_TSEF_DUMP_STACK);
}

void fivmr_Debug_dumpStack(fivmr_ThreadState *ts) {
    fivmr_Lock_lock(&ts->vm->lock);
    fivmr_ThreadState_dumpStackFor(ts);
    ts->execFlags&=~FIVMR_TSEF_DUMP_STACK;
    fivmr_Lock_unlock(&ts->vm->lock);
}

fivmr_DebugRec *fivmr_DebugRec_withRootSize(fivmr_DebugRec *dr,
                                            void *region,
                                            int32_t rootSize) {
    uintptr_t ln_rm_c;
    fivmr_DebugRec *result;
    
    fivmr_assert(rootSize>=0);
    
    ln_rm_c=dr->ln_rm_c;
    
    if ((ln_rm_c&1)) {
        fivmr_FatDebugData *fdd;
        int32_t i;
        int32_t lastSetBit=-1;
        
        fdd=(fivmr_FatDebugData*)(ln_rm_c&~3);
        
        for (i=fdd->refMapSize*32;i-->0;) {
            if (fivmr_BitVec_get((uint32_t*)fdd->refMap,i)) {
                lastSetBit=i;
                break;
            }
        }
        
        if (lastSetBit<rootSize) {
            result=dr;
        } else {
            bool canCompress;
            
            lastSetBit=-1;
            
            for (i=rootSize;i-->0;) {
                if (fivmr_BitVec_get((uint32_t*)fdd->refMap,i)) {
                    lastSetBit=i;
                    break;
                }
            }
            
            if (sizeof(void*)==4) {
                canCompress=
                    fdd->lineNumber>=0 && fdd->lineNumber < (1<<10) &&
                    fdd->bytecodePC>=0 && fdd->bytecodePC < (1<<8) &&
                    lastSetBit < (1<<12);
            } else {
                canCompress=
                    fdd->lineNumber>=0 && fdd->lineNumber < (1<<16) &&
                    fdd->bytecodePC>=0 && fdd->bytecodePC < (1<<5) &&
                    lastSetBit < (1<<31);
            }
            
            result=freg_region_alloc(region,sizeof(fivmr_DebugRec));
            fivmr_assert(result!=NULL);
            
            result->method=dr->method;
            
            if (canCompress) {
#if FIVMSYS_PTRSIZE==4
                result->ln_rm_c=
                    (ln_rm_c&2) |
                    (((uintptr_t)(fdd->refMap[0]&((1<<12)-1)))<<2) |
                    (((uintptr_t)fdd->lineNumber)<<14) |
                    (((uintptr_t)fdd->bytecodePC)<<24);
#else
                result->ln_rm_c=
                    (ln_rm_c&2) |
                    (((uintptr_t)(fdd->refMap[0]&((((uintptr_t)1)<<31)-1)))<<2) |
                    (((uintptr_t)fdd->lineNumber)<<33) |
                    (((uintptr_t)fdd->bytecodePC)<<49);
#endif
            } else {
                fivmr_FatDebugData *newFDD;
                
                newFDD=freg_region_alloc(
                    region,
                    sizeof(fivmr_FatDebugData)-sizeof(int32_t)+
                    sizeof(int32_t)*((lastSetBit+32)/32));
                fivmr_assert(newFDD!=NULL);
                
                newFDD->lineNumber=fdd->lineNumber;
                newFDD->bytecodePC=fdd->bytecodePC;
                newFDD->refMapSize=fdd->refMapSize;
                
                bzero(newFDD->refMap,sizeof(int32_t)*((lastSetBit+32)/32));
                
                for (i=0;i<=lastSetBit;++i) {
                    fivmr_BitVec_set(
                        (uint32_t*)newFDD->refMap,
                        i,
                        fivmr_BitVec_get(
                            (uint32_t*)fdd->refMap,
                            i));
                }
                
                result->ln_rm_c=
                    (ln_rm_c&3) |
                    ((uintptr_t)newFDD);
            }
        }
    } else {
        /* thin form */
        int32_t i;
        int32_t lastSetBit=-1;
        
        /* NOTE: rootSize may be greater than DR_TM_NUMBITS ... that means that
           the root set size that we started with was greater than what could be
           expressed in thin mode *but* the last reference is below the thin
           threshold. */
        
        for (i=FIVMR_DR_TRM_NUMBITS;i-->0;) {
            if ((ln_rm_c&(((uintptr_t)1)<<(2+i)))) {
                lastSetBit=i;
                break;
            }
        }
        
        if (lastSetBit<rootSize) {
            result=dr;
        } else {
            result=freg_region_alloc(region,sizeof(fivmr_DebugRec));
            fivmr_assert(result!=NULL);
            *result=*dr;

            /* this loop is crappy. */
            for (i=rootSize;i<=lastSetBit;++i) {
                ln_rm_c&=~(((uintptr_t)1)<<(2+i));
            }
            
            result->ln_rm_c=ln_rm_c;
        }
    }
    return result;
}

