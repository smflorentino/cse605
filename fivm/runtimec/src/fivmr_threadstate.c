/*
 * fivmr_threadstate.c
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

fivmr_ThreadState *fivmr_ThreadState_getNullable(fivmr_VM *vm) {
    return (fivmr_ThreadState*)fivmr_ThreadSpecific_get(&vm->curThread);
}

fivmr_ThreadState *fivmr_ThreadState_get(fivmr_VM *vm) {
    fivmr_ThreadState *ts=fivmr_ThreadState_getNullable(vm);
    /* LOG(9,("ts = %p",ts)); */
    fivmr_assert(ts!=NULL);
    fivmr_assert(ts->cookie==0xd1e7c0c0);
    fivmr_assert(fivmr_ThreadState_byId(vm,ts->id)==ts);
    return ts;
}

static void clear(fivmr_ThreadState *ts) {
#if FIVMR_PF_POLLCHECK
    int res;
#endif
    ts->vm->javaThreads[ts->id].vm=ts->vm;
    ts->vm->javaThreads[ts->id].obj=0;
    fivmr_assert(ts->vm->javaThreads[ts->id].prev==NULL);
    fivmr_assert(ts->vm->javaThreads[ts->id].next==NULL);
    fivmr_assert(ts->jumpOnExit==NULL);
    fivmr_assert(ts->thread==fivmr_ThreadHandle_zero());
    ts->index=(uint32_t)-1;
    ts->execFlags=0;
    ts->suspendCount=0;
    ts->suspendReqCount=0;
    ts->typeEpoch=0;
    ts->pollingUnion.s.pollchecksDisabled=0;
    ts->pollingUnion.s.checkBlockNotRequested=!0;
    ts->pollchecksTaken=0;
#if FIVMR_PF_POLLCHECK
    res=mprotect(((char*)ts)-FIVMR_PAGE_SIZE,FIVMR_PAGE_SIZE,PROT_READ|PROT_WRITE);
    fivmr_assert(res==0);
#endif
    ts->isDaemon=false;
    ts->curException=0;
    ts->interrupted=false;
    ts->curExceptionHandle=NULL;
    ts->handlingStackOverflow=false;
    ts->stackHeight=0;
    fivmr_assert(ts->toUnbias==0);
    fivmr_assert(ts->curNF==NULL);
    fivmr_assert(ts->curF==NULL);
    ts->stateBufGCMap=NULL;
    bzero(&ts->rootNF,sizeof(fivmr_NativeFrame));
    bzero(&ts->rootF,sizeof(fivmr_Frame));
    fivmr_assert(ts->freeHandles==NULL);
    fivmr_assert(ts->allocFrame==NULL);
    fivmr_GC_clear(ts);
    fivmr_SA_clear(ts);
    fivmr_assert(!ts->bufInUse);
    if (ts->buf!=NULL) {
	bzero(ts->buf,FIVMR_TS_BUF_SIZE);
    }
    ts->basePrio=FIVMR_TPR_MIN;
    ts->permBoostPrio=FIVMR_TPR_MIN;
    ts->curPrio=FIVMR_TPR_MIN;
    ts->curTempBoostPrio=FIVMR_TPR_MIN;
#if FIVMR_FLOW_LOGGING
    if (ts->flowbuf && ts->flowbuf->entries) {
        fivmr_FlowLog_release(ts->flowbuf);
    }
    ts->flowbuf = NULL;
#endif
    /* deliberately leave deqUnlockCount as is */
    fivmr_assert(!ts->forMonitor.syncHandoffCookie);
    fivmr_assert(ts->forMonitor.holding==NULL);
    fivmr_assert(ts->forMonitor.entering==NULL);
    fivmr_assert(ts->forMonitor.queuedOnReal==NULL);
    fivmr_assert(ts->forMonitor.queuedOnIntended==NULL);
    fivmr_assert(ts->forMonitor.next==NULL);
    fivmr_assert(!ts->performedGuaranteedCommit);
}

fivmr_ThreadState *fivmr_ThreadState_new(fivmr_VM *vm,
                                         uintptr_t initExecFlags) {
    uint32_t i;
    if (vm->exiting) {
        return NULL;
    }
    for (i=2;i<vm->config.maxThreads;++i) {
	fivmr_ThreadState *ts=fivmr_ThreadState_byId(vm,i);
        fivmr_Lock_lock(&vm->lock);
	if (ts->execStatus==FIVMR_TSES_CLEAR) {
            fivmr_assert_cas(&ts->execStatus,
                             FIVMR_TSES_CLEAR,
                             FIVMR_TSES_NEW);
	    clear(ts);
	    ts->execFlags|=initExecFlags;
	    ts->index=vm->numThreads++;
	    vm->threads[ts->index]=ts;
            if (vm->maxThreadID < ts->id) {
                vm->maxThreadID=ts->id;
            }
	    fivmr_Lock_unlock(&vm->lock);
	    LOG(1,("Creating new thread %u, at %p",ts->id,ts));
#if FIVMR_FLOW_LOGGING
            ts->flowbuf = fivmr_malloc(FIVMR_FLOWLOG_BUFFERSIZE);
            memset(ts->flowbuf, 0, FIVMR_FLOWLOG_BUFFERSIZE);
#endif
#if FIVMR_INTERNAL_INST
            fivmr_ii_startThread(ts);
#endif
	    return ts;
	}
        fivmr_Lock_unlock(&vm->lock);
    }
    return NULL;
}

void fivmr_ThreadState_setManual(fivmr_ThreadState *ts,
				 fivmr_ThreadHandle th,
                                 fivmr_TypeContext *ctx) {
    fivmr_VM *vm=ts->vm;
    fivmr_assert(fivmr_ThreadPriority_leRT(ts->curPrio,vm->maxPriority));
    fivmr_assert(fivmr_ThreadPriority_leRT(ts->curTempBoostPrio,vm->maxPriority));
    fivmr_assert(fivmr_ThreadPriority_leRT(ts->basePrio,vm->maxPriority));
    fivmr_assert(ts->vm->exitExits || fivmr_ThreadState_canExitGracefully(ts));
    fivmr_assert(fivmr_ThreadPriority_leRT(fivmr_Thread_getPriority(th),
                                           ts->vm->maxPriority));
    ts->thread=th;
    ts->curNF=NULL;
    ts->freeHandles=NULL;
    fivmr_ThreadState_pushAndInitNF2(ts,&ts->rootNF,NULL,ctx);
    ts->rootF.up=NULL;
    ts->rootF.id=(uintptr_t)(intptr_t)-1;
    ts->curF=&ts->rootF;
    fivmr_ThreadSpecific_setForThread(&vm->curThread,th,(uintptr_t)ts);
    fivmr_Lock_lock(&ts->lock);
    fivmr_ThreadState_moveToStatus(ts,FIVMR_TSES_IN_NATIVE);
    ts->version++;
    fivmr_Lock_unlock(&ts->lock);
    
    ts->primFields=vm->primFields;
    ts->refFields=vm->refFields;
    ts->typeList=vm->payload->typeList;
    ts->stubList=vm->payload->stubList;
    ts->patchRepo=vm->payload->patchRepo;
    
    fivmr_Lock_lock(&ts->lock);
    /* any soft-handshake-related thread initialization that needs to
       be performed before threads start executing Java code should
       be done here */
    ts->typeEpoch=vm->typeEpoch;
    fivmr_GC_startThread(ts);
    fivmr_SA_init(ts);
    fivmr_Lock_unlock(&ts->lock);
    
    fivmr_ThreadState_checkExit(ts);
    
    LOG(1,("Thread %u attached to native thread %p",ts->id,ts->thread));
}

void fivmr_ThreadState_go__INTERNAL(fivmr_ThreadState *ts) {
    ts->stackHigh=ts->stackStart;
    fivmr_assert(ts->stackHeight!=0);
    if (FIVMR_STACK_GROWS_DOWN) {
        ts->stackLimit=ts->stackStart-ts->stackHeight;
    } else {
        ts->stackLimit=ts->stackStart+ts->stackHeight;
    }
    LOG(1,("Thread %u running on stack %p",ts->id,ts->stackStart));
}

void fivmr_ThreadState_set__INTERNAL(fivmr_ThreadState *ts,
                                     fivmr_TypeContext *ctx) {
    fivmr_ThreadState_setManual(ts,fivmr_Thread_integrate(),ctx);
    fivmr_ThreadState_go__INTERNAL(ts);
}

bool fivmr_ThreadState_glue(fivmr_ThreadState *ts,
			    fivmr_Handle *javaThread) {
    fivmr_ThreadState *curTS;
    fivmr_VM *vm=ts->vm;
    bool isDaemon;
    
    curTS=fivmr_ThreadState_get(vm);
    
    fivmr_assert(javaThread!=NULL);
    
    /* NOTE: if this gets called with the state of the thread being NEW, then
       it means that this function is being called from a *different* thread
       state. */

    fivmr_Lock_lock(&ts->lock);
    if (ts->execStatus==FIVMR_TSES_NEW) {
	LOG(1,("moving to status STARTING"));
	fivmr_ThreadState_moveToStatus(ts,FIVMR_TSES_STARTING);
    }
    fivmr_Lock_unlock(&ts->lock);

    LOG(5,("setting thread state, execStatus = %" PRIuPTR,ts->execStatus));
    fivmr_VMThread_setThreadState(curTS,javaThread,ts);
    LOG(5,("thread state set"));
    fivmr_ThreadState_goToJava(curTS);
    fivmr_assert(fivmr_Handle_get(javaThread)!=0);
    fivmr_Handle_set(vm->javaThreads+ts->id,curTS,fivmr_Handle_get(javaThread));
    LOG(1,("Thread %u attached to Java thread %p",ts->id,javaThread->obj));
    fivmr_ThreadState_goToNative(curTS);

    isDaemon=fivmr_VMThread_isDaemon(curTS,vm->javaThreads+ts->id);
    fivmr_Lock_lock(&vm->deathLock);
    fivmr_Lock_lock(&vm->lock);
    if (ts->vm->exiting) {
        fivmr_Lock_unlock(&vm->lock);
        fivmr_Lock_unlock(&vm->deathLock);
        return false;
    }
    if (isDaemon) {
	ts->isDaemon=true;
	vm->numDaemons++;
    }
    vm->numActive++;
    vm->numRunning++; /* FIXME this is almost certainly wrong!  should do numRunning++
                         in glue */

    fivmr_Lock_unlock(&vm->lock);
    fivmr_Lock_unlock(&vm->deathLock);

    fivmr_VMThread_starting(curTS,vm->javaThreads+ts->id);
    fivmr_assertNoException(ts,"while attempting to set up a Java thread");
    
    return true;
}

void fivmr_ThreadState_commit(fivmr_ThreadState *ts) {
    /* for concurrent GC, we have to merge our queues with the
       collector at this point.  the collector should have one global
       queue onto which we can deposit references.
    
       that's what commitThread() is for. */
    fivmr_GC_commitThread(ts);

    fivmr_ThreadState_guaranteedCommit(ts);
}

void fivmr_ThreadState_guaranteedCommit(fivmr_ThreadState *ts) {
    fivmr_Lock_lock(&ts->lock);
    
#if FIVMR_INTERNAL_INST
    LOG(1,("Calling into fivmr_ii_commitThread()"));
    fivmr_ii_commitThread(ts);
#endif
    
    ts->performedGuaranteedCommit=true;
    fivmr_Lock_unlock(&ts->lock);
}

void fivmr_ThreadState_finalize(fivmr_ThreadState *ts) {
    fivmr_ThreadState_commit(ts);
    fivmr_SA_destroy(ts);

    while (fivmr_ThreadState_popNF(ts)) ;
    while (ts->freeHandles!=NULL) {
	fivmr_Handle *cur=ts->freeHandles;

	fivmr_Lock_lock(&ts->vm->hrLock);
	ts->freeHandles=cur->next;
	cur->next=ts->vm->freeHandles;
	ts->vm->freeHandles=cur;
	fivmr_Lock_unlock(&ts->vm->hrLock);
    }

#if FIVMR_FLOW_LOGGING
    fivmr_FlowLog_release(ts->flowbuf);
    ts->flowbuf = NULL;
#endif

    ts->execFlags|=FIVMR_TSEF_FINALIZED;
}

fivmr_ThreadPriority fivmr_ThreadState_terminate(fivmr_ThreadState *ts) {
    fivmr_VM *vm;
    fivmr_ThreadPriority result;
    bool signalDeath;
    bool signalExit;
    
    vm=ts->vm;
    
    LOG(1,("Thread %u terminating",ts->id));
    
    /* boost myself through termination to ensure that it happens quickly.
       FIXME not sure about this... */
    fivmr_Lock_lock(&ts->lock);
    result=ts->basePrio;
    /* we slam down all of the priorities because we know that we're setting
       the highest priority in the house. */
    ts->curTempBoostPrio=ts->curPrio=ts->permBoostPrio=
        fivmr_ThreadPriority_min(FIVMR_TPR_MAX,ts->vm->maxPriority);
    fivmr_Thread_setPriority(
        ts->thread,
        ts->curTempBoostPrio);
    fivmr_Lock_unlock(&ts->lock);

    fivmr_assert(ts->forMonitor.holding==NULL);
    ts->forMonitor.holding=NULL;
    
    fivmr_assertNoException(ts,"in call to fivmr_ThreadState_terminate()");
    fivmr_VMThread_die(ts,vm->javaThreads+ts->id);
    fivmr_assertNoException(ts,"while attempting to terminate a Java thread");
    
    fivmr_Lock_lock(&ts->lock);
    fivmr_ThreadState_finalize(ts);
    fivmr_ThreadState_moveToStatus(ts,FIVMR_TSES_TERMINATING);
    fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
    fivmr_ThreadState_acknowledgeSoftHandshakeRequest(ts);
    ts->jumpOnExit=NULL;
    ts->exitOnExit=false;
    ts->performedGuaranteedCommit=false;
    fivmr_Lock_broadcast(&ts->lock);
    fivmr_Lock_unlock(&ts->lock);

    fivmr_Lock_lock(&vm->lock);
    vm->threads[ts->index]=vm->threads[--vm->numThreads];
    vm->threads[ts->index]->index=ts->index;
    fivmr_Lock_unlock(&vm->lock);
    
    fivmr_assert(ts->curNF==NULL);
    
    fivmr_assert(ts->curF==&ts->rootF);
    ts->curF=ts->curF->up;
    fivmr_assert(ts->curF==NULL);
    
    fivmr_Lock_lock(&vm->lock);
    if (ts->isDaemon) {
	vm->numDaemons--;
    }
    vm->numActive--;
    signalDeath = (vm->numActive-vm->numDaemons==0);
    fivmr_Lock_unlock(&vm->lock);

    if (signalDeath) {
	fivmr_Lock_lockedBroadcast(&vm->deathLock);
    }

    if (ts->bufInUse) {
	LOG(1,("Thread %u leaking buffer %p",ts->id,ts->buf));
	ts->bufInUse=false;
	ts->buf=NULL;
    }
    
    ts->thread=fivmr_ThreadHandle_zero();
    clear(ts);

    fivmr_Lock_lock(&ts->lock);
    ts->execStatus=FIVMR_TSES_CLEAR;
    fivmr_Lock_unlock(&ts->lock);
    
    LOG(1,("Thread #%u completely done.",ts->id));
    
    fivmr_Lock_lock(&vm->deathLock);
    fivmr_Lock_lock(&vm->lock);
    vm->numRunning--;
    if (vm->numRunning==0) {
        signalExit=true;
    } else {
        signalExit=false;
    }

    fivmr_ThreadSpecific_set(&vm->curThread,0);

    fivmr_Lock_unlock(&vm->lock);
    if (signalExit) {
        fivmr_Lock_broadcast(&vm->deathLock);
    }
    fivmr_Lock_unlock(&vm->deathLock);

    return result;
}

void fivmr_ThreadState_lockWithHandshake(fivmr_ThreadState *ts,
                                         fivmr_Lock *lock) {
    fivmr_ThreadState_goToNative(ts);
    fivmr_Lock_lock(lock);
    fivmr_ThreadState_goToJava(ts);
}

void fivmr_ThreadState_waitSuspended(fivmr_ThreadState *ts) {
    for (;;) {
        fivmr_assert(ts->suspendReqCount>=0);
        fivmr_assert(ts->suspendCount>=0);
        
        if (ts->suspendReqCount>0) {
            ts->suspendCount+=ts->suspendReqCount;
            ts->suspendReqCount=0;
            fivmr_Lock_broadcast(&ts->lock);
        }
        
        if (ts->suspendCount==0) {
            break;
        }
        
        fivmr_Lock_unlock(&ts->lock);
        fivmr_Semaphore_down(&ts->waiter);
        fivmr_ThreadState_checkExit(ts);
        fivmr_Lock_lock(&ts->lock);
    }
}

void fivmr_ThreadState_moveToStatus(fivmr_ThreadState *ts,
				    uintptr_t status) {
    fivmr_Lock_lock(&ts->lock);
    fivmr_ThreadState_waitSuspended(ts);
    ts->execStatus=status;
    fivmr_Lock_unlock(&ts->lock);
}

bool fivmr_ThreadState_canExitGracefully(fivmr_ThreadState *ts) {
    return ts->jumpOnExit!=NULL || ts->exitOnExit;
}

bool fivmr_ThreadState_shouldExit(fivmr_ThreadState *ts) {
    if (fivmr_exiting(ts->vm)) {
        bool itsme;
        fivmr_Lock_lock(&ts->vm->lock);
        fivmr_assert(ts->vm->exiting);
        itsme = (ts->vm->exitInitiator==ts);
        fivmr_Lock_unlock(&ts->vm->lock);
        return !itsme;
    } else {
        return false;
    }
}

void fivmr_ThreadState_exitImpl(fivmr_ThreadState *ts) {
    fivmr_assert(fivmr_ThreadState_canExitGracefully(ts));
    bool signalExit=false;
    fivmr_JmpBuf *buf;
    if (ts->jumpOnExit!=NULL) {
        LOG(1,("VM exiting.  Jumping out of Java code in Thread %u...",ts->id));
        buf=ts->jumpOnExit;
        ts->jumpOnExit=NULL;
    } else {
        LOG(1,("VM exiting.  Terminating Thread %u...",ts->id));
        buf=NULL;
    }
#if FIVMR_FLOW_LOGGING
    /* FIXME: flush buffers */
    fivmr_free(ts->flowbuf);
#endif
    fivmr_ThreadState_finalize(ts);
    fivmr_Lock_lock(&ts->vm->deathLock);
    fivmr_Lock_lock(&ts->vm->lock);
    ts->vm->numRunning--;
    if (ts->vm->numRunning==0) {
        signalExit=true;
    }
    fivmr_ThreadSpecific_set(&ts->vm->curThread,0);
    fivmr_Lock_unlock(&ts->vm->lock);
    if (signalExit) {
        fivmr_Lock_broadcast(&ts->vm->deathLock);
    }
    fivmr_Lock_unlock(&ts->vm->deathLock);
    /* at this point none of the VM data structures are valid! */
    if (buf!=NULL) {
        fivmr_JmpBuf_jump(buf,1);
    } else {
        fivmr_Thread_exit();
    }
    fivmr_assert(false);
}

void fivmr_ThreadState_checkExit(fivmr_ThreadState *ts) {
    if (fivmr_ThreadState_shouldExit(ts)) {
        fivmr_ThreadState_exitImpl(ts);
    }
}

void fivmr_ThreadState_checkExitHoldingLock(fivmr_ThreadState *ts,
                                            int32_t times) {
    if (fivmr_ThreadState_shouldExit(ts)) {
        while (times-->0) {
            fivmr_Lock_unlock(&ts->lock);
        }
        fivmr_ThreadState_exitImpl(ts);
    }
}

void fivmr_ThreadState_checkExitHoldingLocks(fivmr_ThreadState *ts,
                                             int32_t n,
                                             fivmr_Lock **locks) {
    if (fivmr_ThreadState_shouldExit(ts)) {
        int32_t i;
        for (i=0;i<n;++i) {
            fivmr_Lock_unlock(locks[i]);
        }
        fivmr_ThreadState_exitImpl(ts);
    }
}

void fivmr_ThreadState_checkExitInHandshake(fivmr_ThreadState *ts) {
    if (fivmr_ThreadState_shouldExit(ts)) {
        if (ts==fivmr_ThreadState_getNullable(ts->vm)) {
            fivmr_Lock_unlock(&ts->lock);
            fivmr_ThreadState_exitImpl(ts);
        } else {
            /* make sure that the thread wakes up from whatever it may be
               blocked on! */
            fivmr_Semaphore_up(&ts->waiter);
            fivmr_Lock_broadcast(&ts->lock);
        }
    }
}

void fivmr_ThreadState_checkBlock(fivmr_ThreadState *ts) {
    const int loglevel=2;
    fivmr_Nanos before=0,after=0,before2,after2;
#if FIVMR_PF_POLLCHECK
    int res;
#endif
    uintptr_t execStatus;
    fivmr_ThreadState *ts2;
    
    ts->pollchecksTaken++;
    
    fivmr_assert(!fivmr_Thread_isCritical());
    
    LOG(3,("Thread %u in checkBlock",ts->id));

    before2=fivmr_curTimeLogging(loglevel);
    fivmr_Lock_lock(&ts->lock);
    after2=fivmr_curTimeLogging(loglevel);
    ts->pollingUnion.s.checkBlockNotRequested=!0;
#if FIVMR_PF_POLLCHECK
    res=mprotect(((char*)ts)-FIVMR_PAGE_SIZE,FIVMR_PAGE_SIZE,PROT_READ|PROT_WRITE);
    fivmr_assert(res==0);
#endif

    ts->execFlags|=FIVMR_TSEF_BLOCKING;
    ts->execStatus=FIVMR_TSES_IN_JAVA;
    
    /* FIXME: figure out a way of making this NOT require holding a lock,
       since it may be somewhat expensive-ish */
    fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
    fivmr_ThreadState_acknowledgeSoftHandshakeRequest(ts);
    fivmr_Lock_broadcast(&ts->lock);
    
    if (fivmr_ThreadState_shouldSuspend(ts)) {
        /* this entire suspension code path is currently dead code! */
        
        /* this needs to happen *after* servicing soft handshake requests, since the whole
           point is that we want soft handshakes to be priority boosted.  note that the
           fact that we're releasing the lock here means that a subsequent soft handshake
           request may happen.  but that's fine.  we service handshakes again after waiting
           for suspension. */
        ts->execStatus=FIVMR_TSES_IN_NATIVE;

        before=fivmr_curTimeLogging(loglevel);
        fivmr_Lock_unlock(&ts->lock);
        after=fivmr_curTimeLogging(loglevel);
        fivmr_ThreadState_evalPrio(ts);
        fivmr_Lock_lock(&ts->lock);

        fivmr_ThreadState_waitSuspended(ts); /* this may suspend the thread */
        
        ts->execStatus=FIVMR_TSES_IN_JAVA;
        fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
        fivmr_ThreadState_acknowledgeSoftHandshakeRequest(ts);
        fivmr_Lock_broadcast(&ts->lock);
    }
    
    /* don't move this.  see above. */
    ts2=fivmr_ThreadState_evalPrioImpl(ts);
    
    execStatus=ts->execStatus;
    fivmr_assert(execStatus==FIVMR_TSES_IN_JAVA ||
                 execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK);
    ts->execFlags&=~FIVMR_TSEF_BLOCKING;
    fivmr_Lock_broadcast(&ts->lock);
    fivmr_Lock_unlock(&ts->lock);
    
    if (ts2!=NULL) {
        fivmr_ThreadState_evalPrio(ts2);
    }
    
    LOG(loglevel,("checkBlock timings: %u, %u",
                  (uint32_t)(after-before),
                  (uint32_t)(after2-before2)));
    
    LOG(3,("Thread %u exiting checkBlock",ts->id));
}

void fivmr_ThreadState_goToNative_slow(fivmr_ThreadState *ts) {
    int oldErrno=errno;
    fivmr_ThreadState *ts2;
    
    ts->pollchecksTaken++;
    
    LOG(3,("Thread %u in goToNative_slow",ts->id));
    
    fivmr_assert(!fivmr_Thread_isCritical());

    fivmr_Lock_lock(&ts->lock);
    fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
    fivmr_ThreadState_acknowledgeSoftHandshakeRequest(ts);
    /* idea: change our state and send a broadcast; if anyone is waiting
       for us to acknowledge something, they'll instead observe that we've
       disappeared. */
    switch (ts->execStatus) {
    case FIVMR_TSES_IN_JAVA:
	ts->execStatus=FIVMR_TSES_IN_NATIVE;
	break;
    case FIVMR_TSES_IN_JAVA_TO_BLOCK:
	ts->execStatus=FIVMR_TSES_IN_NATIVE_TO_BLOCK;
	break;
    default: fivmr_assert(false);
    }
    fivmr_Lock_broadcast(&ts->lock);
    ts2=fivmr_ThreadState_evalPrioImpl(ts);
    fivmr_Lock_unlock(&ts->lock);
    if (ts2!=NULL) {
        fivmr_ThreadState_evalPrio(ts2);
    }
    errno=oldErrno;
}

void fivmr_ThreadState_goToJava_slow(fivmr_ThreadState *ts) {
    int oldErrno=errno;
    if (FIVMR_ASSERTS_ON) {
	uintptr_t execStatus=ts->execStatus;
	LOG(5,("execStatus = %" PRIuPTR,execStatus));
	fivmr_assert(execStatus==FIVMR_TSES_IN_NATIVE ||
		     execStatus==FIVMR_TSES_IN_NATIVE_TO_BLOCK);
    }
    fivmr_assert(!fivmr_Thread_isCritical());
    fivmr_ThreadState_checkBlock(ts);
    errno=oldErrno;
}

void fivmr_ThreadState_pollcheckSlow(fivmr_ThreadState *ts,
                                     uintptr_t debugID) {
    int oldErrno=errno;
    LOG(3,("Thread %u taking pollcheck at debugID = %p, ts->curF->id = %d",
           ts->id,debugID,ts->curF->id));
    fivmr_assert(!fivmr_Thread_isCritical());
    if (FIVMR_ASSERTS_ON) {
	uintptr_t execStatus=ts->execStatus;
	fivmr_assert(execStatus==FIVMR_TSES_IN_JAVA ||
		     execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK);
    }
    fivmr_ThreadState_setDebugID(ts,debugID);
    fivmr_ThreadState_checkBlock(ts);
    errno=oldErrno;
}

void fivmr_ThreadState_sleepAbsolute(fivmr_ThreadState *ts,
				     fivmr_Nanos whenAwake) {
    fivmr_Lock_lock(&ts->lock);
    while (whenAwake>fivmr_curTime() &&
	   !ts->interrupted) {
	fivmr_Lock_timedWaitAbs(&ts->lock,whenAwake);
        fivmr_ThreadState_checkExitHoldingLock(ts,1);
    }
    fivmr_Lock_unlock(&ts->lock);
}

fivmr_ThreadState *fivmr_ThreadState_evalPrioImpl(fivmr_ThreadState *ts) {
    fivmr_ThreadState *result=NULL;
    fivmr_ThreadPriority prio,lastPrio;
    fivmr_Monitor *cur;
    bool rtneq;
    fivmr_ThreadHandle th;
    
    LOG(3,("Evaluating priority of Thread #%u",ts->id));
    
    if (!fivmr_ThreadState_isRunning(ts)) {
        return NULL;
    }
    
    fivmr_assert(ts->thread!=fivmr_ThreadHandle_zero());

    prio=fivmr_ThreadPriority_max(ts->basePrio,
                                  ts->permBoostPrio);
    
    if (FIVMR_PIP_LOCKING(&ts->vm->settings)) {
        for (cur=ts->forMonitor.holding;cur!=NULL;cur=cur->next) {
            if (cur->queues!=NULL) {
                fivmr_ThreadQueue *queue;
                fivmr_ThreadState *ts2;
            
                LOG(3,("holding lock with queues"));
            
                fivmr_fence();
                queue=&cur->queues->entering;
            
                /* prevent the queue from changing until we're done with it */
                fivmr_BoostedSpinLock_lock(ts,&queue->lock);
                ts2=fivmr_ThreadQueue_peek(queue);
            
                if (ts2!=NULL) {
                    fivmr_ThreadPriority prio2;
                    LOG(3,("head of queue: %u",ts2->id));
                
                    prio2=ts2->curPrio;
                
                    if (fivmr_ThreadPriority_isRT(prio2)) {
                        prio=fivmr_ThreadPriority_max(prio,prio2);
                    }
                }
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
            }
            LOG(3,("done with lock %p",cur));
        }
        LOG(3,("done with locks"));
    }
    
    rtneq=!fivmr_ThreadPriority_eqRT(ts->curPrio,prio);
    
    lastPrio=ts->curPrio;
    ts->curTempBoostPrio=ts->curPrio=prio;
    
    if (rtneq || FIVMR_PIP_LOCKING(&ts->vm->settings)) {
        if (ts->forMonitor.queuedOnIntended!=NULL) {
            fivmr_ThreadQueue *queue=ts->forMonitor.queuedOnIntended;
            fivmr_BoostedSpinLock_lock(ts,&queue->lock);
            if (rtneq) {
                fivmr_ThreadQueue_poke(queue,ts);
            }
            if (ts->forMonitor.entering!=NULL) {
                if (rtneq) {
                    fivmr_Monitor_pokeRTQueued(ts->forMonitor.entering);
                }
                if (FIVMR_PIP_LOCKING(&ts->vm->settings)) {
                    result=fivmr_Monitor_curHolder(ts->vm,ts->forMonitor.entering);
                    LOG(3,("Triggering reevaluation of Thread #%u",result->id));
                }
            }
            fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
        } else {
            fivmr_assert(ts->forMonitor.entering==NULL);
        }
    }
    
    if (LOGGING(1) && (ts->curPrio!=lastPrio)) {
        char buf[32],buf2[32],buf3[32];
        fivmr_ThreadPriority_describe(ts->curPrio,buf,sizeof(buf));
        fivmr_ThreadPriority_describe(lastPrio,buf2,sizeof(buf2));
        fivmr_ThreadPriority_describe(ts->basePrio,buf3,sizeof(buf3));
        LOG(1,("Changing priority of Thread %u: %s -> %s, base was %s",ts->id,buf2,buf,buf3));
    }
    
    th=ts->thread;
    fivmr_assert(th!=fivmr_ThreadHandle_zero());
    fivmr_assert(fivmr_ThreadPriority_leRT(ts->curPrio,ts->vm->maxPriority));
    fivmr_Thread_setPriority(th,ts->curPrio);
    
    if (LOGGING(2)) {
        char buf[32];
        fivmr_ThreadPriority_describe(ts->curPrio,buf,sizeof(buf));
        LOG(2,("Priority of Thread %u evaluated and set to %s",ts->id,buf));
    }
    
    return result;
}

fivmr_ThreadState *fivmr_ThreadState_evalPrioMiniImpl(fivmr_ThreadState *ts) {
    fivmr_ThreadState *result=NULL;
    
    if (FIVMR_PIP_LOCKING(&ts->vm->settings) && ts->forMonitor.entering!=NULL) {
        result=fivmr_Monitor_curHolder(ts->vm,ts->forMonitor.entering);
    }
    
    return result;
}

void fivmr_ThreadState_evalPrio(fivmr_ThreadState *startTS) {
    fivmr_ThreadState *curTS=startTS;

    /* variables used for detecting cycles */
    fivmr_ThreadState *curTSlag=startTS;
    bool throttle=false;
    
    do {
        fivmr_ThreadState *myTS;
        
        LOG(3,("In prio eval loop, %u, %u",curTS->id,curTSlag->id));
        
        myTS=curTS;
        curTS=NULL;
        
        fivmr_Lock_lock(&myTS->lock);
        if (fivmr_ThreadState_isRunning(myTS)) {
            if (myTS->thread==fivmr_ThreadHandle_current()) {
                curTS=fivmr_ThreadState_evalPrioImpl(myTS);
            } else {
                fivmr_ThreadState_triggerBlock(myTS);
                if (!fivmr_ThreadState_isInJava(myTS)) {
                    curTS=fivmr_ThreadState_evalPrioImpl(myTS);
                    LOG(4,("got curTS = %p",curTS));
                } /* else the thread in question just got boosted and triggered
                     to block, so it'll evaluate its priority for us at the
                     next safepoint. */
            }
            
            if (curTS!=NULL && throttle) {
                curTSlag=fivmr_ThreadState_evalPrioMiniImpl(curTSlag);
                LOG(4,("got curTSlag = %p",curTSlag));
                if (curTSlag==NULL) {
                    /* something got ef'd up */
                    curTSlag=myTS;
                    throttle=true;
                } else {
                    throttle=false;
                }
            } else {
                throttle=true;
            }
        }
        fivmr_Lock_unlock(&myTS->lock);
        
        if (curTS!=NULL) {
            LOG(4,("Footer of loop, %u, %u",curTS->id,curTSlag->id));
        }
    } while (curTS!=NULL && curTS!=curTSlag);
}

void fivmr_ThreadState_setBasePrio(fivmr_ThreadState *ts,
                                   fivmr_ThreadPriority pr) {
    fivmr_assert(fivmr_ThreadPriority_leRT(pr,ts->vm->maxPriority));
    fivmr_Lock_lock(&ts->lock);
    ts->basePrio=pr;
    fivmr_Lock_unlock(&ts->lock);
    fivmr_ThreadState_evalPrio(ts);
}

fivmr_Handle *fivmr_ThreadState_cloneHandle(fivmr_ThreadState *ts,
					    fivmr_Handle *h) {
    return fivmr_ThreadState_addHandle(ts,fivmr_Handle_get(h));
}

void fivmr_ThreadState_pushNF(fivmr_ThreadState *ts,
			      fivmr_NativeFrame *nf) {
    fivmr_assert((ts->execFlags&FIVMR_TSEF_FINALIZED)==0);
    nf->up=ts->curNF;
    ts->curNF=nf;
}

void fivmr_ThreadState_pushAndInitNF(fivmr_ThreadState *ts,
				     fivmr_NativeFrame *nf,
				     fivmr_MethodRec *mr) {
    int oldErrno=errno;
    fivmr_NativeFrame_init(nf,ts,mr,fivmr_TypeData_getContext(mr->owner));
    fivmr_ThreadState_pushNF(ts,nf);
    errno=oldErrno;
}

void fivmr_ThreadState_pushAndInitNF2(fivmr_ThreadState *ts,
                                      fivmr_NativeFrame *nf,
                                      fivmr_MethodRec *mr,
                                      fivmr_TypeContext *ctx) {
    int oldErrno=errno;
    fivmr_NativeFrame_init(nf,ts,mr,ctx);
    fivmr_ThreadState_pushNF(ts,nf);
    errno=oldErrno;
}

bool fivmr_ThreadState_popNF(fivmr_ThreadState *ts) {
    int oldErrno=errno;
    fivmr_NativeFrame *nf=ts->curNF;
    if (nf==NULL) {
        return false;
    } else {
        fivmr_NativeFrame_destroy(nf,ts);
        ts->curNF=nf->up;
        errno=oldErrno;
        return true;
    }
}

char *fivmr_ThreadState_tryGetBuffer(fivmr_ThreadState *ts,
                                     int32_t size) {
    if (size>FIVMR_TS_BUF_SIZE) {
	return NULL;
    } else {
	if (ts->bufInUse) {
	    return NULL;
	} else {
	    ts->bufInUse=true;
	    if (ts->buf==NULL) {
		ts->buf=fivmr_malloc(FIVMR_TS_BUF_SIZE);
		fivmr_assert(ts->buf!=NULL);
	    }
	    return (char*)ts->buf;
	}
    }
}

bool fivmr_ThreadState_tryReturnBuffer(fivmr_ThreadState *ts,
                                       char *ptr) {
    if (ptr==(char*)ts->buf) {
	fivmr_assert(ts->bufInUse);
	ts->bufInUse=false;
	return true;
    } else {
	return false;
    }
}

void fivmr_ThreadState_returnBuffer(fivmr_ThreadState *ts,
                                    char *ptr) {
    if (!fivmr_ThreadState_tryReturnBuffer(ts,ptr)) {
	free(ptr);
    }
}

bool fivmr_ThreadState_tryClaimBuffer(fivmr_ThreadState *ts,
                                      char *ptr) {
    if (ptr==(char*)ts->buf) {
	fivmr_assert(ts->bufInUse);
	ts->bufInUse=false;
	ts->buf=NULL;
	return true;
    } else {
	return false;
    }
}

void fivmr_ThreadState_handlifyException(fivmr_ThreadState *ts) {
    if (ts->curException) {
	ts->curExceptionHandle=
	    fivmr_ThreadState_addHandle(ts,ts->curException);
	ts->curException=0;
    }
}

const char *fivmr_ThreadState_describeStatusImpl(uintptr_t execStatus) {
    switch (execStatus) {
    case FIVMR_TSES_CLEAR: return "CLEAR";
    case FIVMR_TSES_NEW: return "NEW";
    case FIVMR_TSES_STARTING: return "STARTING";
    case FIVMR_TSES_IN_JAVA: return "IN_JAVA";
    case FIVMR_TSES_IN_JAVA_TO_BLOCK: return "IN_JAVA_TO_BLOCK";
    case FIVMR_TSES_IN_NATIVE: return "IN_NATIVE";
    case FIVMR_TSES_IN_NATIVE_TO_BLOCK: return "IN_NATIVE_TO_BLOCK";
    case FIVMR_TSES_TERMINATING: return "TERMINATING";
    default: fivmr_assert(!"bad execStatus value"); return NULL;
    }
}

const char *fivmr_ThreadState_describeFlagsImpl(uintptr_t execFlags) {
    switch (execFlags) {
    case 0: return "";
    case FIVMR_TSEF_BLOCKING: return "-BLOCKING";
    case FIVMR_TSEF_WAITING: return "-WAITING";
    case FIVMR_TSEF_BLOCKING|FIVMR_TSEF_WAITING: return "-BLOCKING-WAITING";
    case FIVMR_TSEF_TIMED: return "-TIMED";
    case FIVMR_TSEF_BLOCKING|FIVMR_TSEF_TIMED: return "-BLOCKING-TIMED";
    case FIVMR_TSEF_WAITING|FIVMR_TSEF_TIMED: return "-WAITING-TIMED";
    case FIVMR_TSEF_BLOCKING|FIVMR_TSEF_WAITING|FIVMR_TSEF_TIMED:
	return "-BLOCKING-WAITING-TIMED";
    default: fivmr_assert(!"bad execFlags value"); return NULL;
    }
}

const char *fivmr_ThreadState_describeStateImpl(uintptr_t execStatus,
						uintptr_t execFlags) {
    switch (execStatus) {
    case FIVMR_TSES_IN_JAVA:
    case FIVMR_TSES_IN_JAVA_TO_BLOCK:
    case FIVMR_TSES_IN_NATIVE:
    case FIVMR_TSES_IN_NATIVE_TO_BLOCK:
	if ((execFlags&FIVMR_TSEF_BLOCKING)) {
	    return "BLOCKED";
	} else if ((execFlags&FIVMR_TSEF_WAITING)) {
	    if ((execFlags&FIVMR_TSEF_TIMED)) {
		return "TIMED_WAITING";
	    } else {
		return "WAITING";
	    }
	} else {
	    return "RUNNABLE";
	}
    case FIVMR_TSES_CLEAR:
    case FIVMR_TSES_TERMINATING:
	return "TERMINATED";
    case FIVMR_TSES_NEW:
    case FIVMR_TSES_STARTING:
	return "NEW";
    default: fivmr_assert(!"bad execStatus value"); return NULL;
    }
}

void fivmr_ThreadState_setDebugID(fivmr_ThreadState *ts,
				  uintptr_t debugID) {
    if (debugID!=(uintptr_t)(intptr_t)-1) {
	ts->curF->id=debugID;
    }
}

void fivmr_ThreadState_dumpStackFor(fivmr_ThreadState *ts) {
    fivmr_Monitor *entering;
    
    fivmr_Log_lock();
    fivmr_Log_printf("fivmr stack dump for Thread %u:\n",ts->id);
    entering=ts->forMonitor.entering;
    if (entering!=NULL) {
        char buf[64];
        fivmr_MonState_describe(entering->state,
                                buf,sizeof(buf));
        fivmr_Log_printf("   (entering monitor %p, state %s)\n",
                         entering,
                         buf);
    }
    fivmr_dumpStackFromNoHeading(ts->vm,ts->curF);
    fivmr_Log_unlock();
}

void fivmr_ThreadState_checkHeightSlow(fivmr_ThreadState *ts,
				       uintptr_t newHeight) {
    uintptr_t heightVal;
    ts->stackHigh=newHeight;
#  if FIVMR_STACK_GROWS_DOWN
    heightVal=ts->stackStart-ts->stackHigh;
#  else
    heightVal=ts->stackHigh-ts->stackStart;
#  endif
    fivmr_Log_lock();
    fivmr_Log_printf("fivmr stack height profile: Thread %u: "
		     "new height is %" PRIuPTR
		     "; stack range = %" PRIuPTR
		     " to %" PRIuPTR
		     "; this function is at = %" PRIuPTR "\n",
		     ts->id,heightVal,
		     ts->stackStart,ts->stackHigh,
		     &heightVal);
    fivmr_Log_unlock();
}

uintptr_t fivmr_ThreadState_triggerBlock(fivmr_ThreadState *ts) {
#if FIVMR_PF_POLLCHECK
    int res;
#endif
    
    ts->pollingUnion.s.checkBlockNotRequested=!1;
#if FIVMR_PF_POLLCHECK
    res=mprotect(((char*)ts)-FIVMR_PAGE_SIZE,FIVMR_PAGE_SIZE,PROT_NONE);
    fivmr_assert(res==0);
#endif
    for (;;) {
	uintptr_t curStatus=ts->execStatus;
	uintptr_t newStatus;
	switch (curStatus) {
	case FIVMR_TSES_IN_JAVA: {
            fivmr_ThreadPriority myPrio;
            
            myPrio=fivmr_Thread_getPriority(fivmr_ThreadHandle_current());
            
            myPrio=fivmr_ThreadPriority_min(FIVMR_TPR_MAX,ts->vm->maxPriority);
            
            fivmr_assert(fivmr_ThreadPriority_geRT(ts->curTempBoostPrio,
                                                   ts->curPrio));
            
            ts->curTempBoostPrio=fivmr_ThreadPriority_max(ts->curTempBoostPrio,
                                                          myPrio);
            
            if (LOGGING(3)) {
                char buf1[32],buf2[32];
                fivmr_ThreadPriority_describe(myPrio,buf1,sizeof(buf1));
                fivmr_ThreadPriority_describe(ts->curTempBoostPrio,buf2,sizeof(buf2));
                LOG(3,("boosting priority of Thread #%u to %s due to triggering "
                       "thread having priority %s",
                       ts->id,buf2,buf1));
            }
            fivmr_Thread_setPriority(
                ts->thread,
                ts->curTempBoostPrio);
            LOG(2,("boosted priority of Thread #%u",ts->id));
    
	    newStatus=FIVMR_TSES_IN_JAVA_TO_BLOCK;
	    break;
        }
	case FIVMR_TSES_IN_NATIVE:
	    newStatus=FIVMR_TSES_IN_NATIVE_TO_BLOCK;
	    break;
	default:
	    newStatus=curStatus;
	    break;
	}
	if (fivmr_cas_weak(&ts->execStatus,curStatus,newStatus)) {
	    return newStatus;
	}
    }
}

void fivmr_ThreadState_softHandshakeImpl(fivmr_VM *vm,
                                         uintptr_t requiredExecFlags,
                                         uintptr_t execFlagsToSet,
                                         bool shouldWait,
                                         bool ignoreExit) {
    /* this code is tricky.  and it provides some tricky semantics. */

    unsigned i;
    unsigned numHandshakeThreads;
    
    fivmr_assert(!(shouldWait && ignoreExit));

    LOG(3,("Initiating soft handshake with requiredExecFlags = %" PRIuPTR
	   ", execFlagsToSet = %" PRIuPTR,
	   requiredExecFlags,execFlagsToSet));
    
    fivmr_Lock_lock(&vm->handshakeLock);
    
    if (vm->exiting && !ignoreExit) {
        fivmr_Lock_unlock(&vm->handshakeLock);
        return;
    }
    
    fivmr_assert(vm->softHandshakeCounter==0 || (vm->exiting && !shouldWait));
    
    numHandshakeThreads=0;
    
    /* FIXME: currently the thread table includes preallocated ThreadState
       structures.  but what if we had a table to which ThreadState structures
       could be added and removed?  any way to make this structure lock-free? */
    
    fivmr_Lock_lock(&vm->lock);
    LOG(3,("soft handshake: Checking %u threads...",vm->numThreads));
    for (i=0;i<vm->numThreads;++i) {
	fivmr_ThreadState *ts=vm->threads[i];
	if ((ts->execFlags&requiredExecFlags)==requiredExecFlags &&
	    fivmr_ThreadState_isRunning(ts)) {
            if (FIVMR_ASSERTS_ON && !(vm->exiting && !shouldWait)) {
                uintptr_t execFlags=ts->execFlags;
                if ((execFlags&FIVMR_TSEF_SOFT_HANDSHAKE)!=0) {
                    LOG(0,("Exec flags of Thread #%u is %p",ts->id,execFlags));
                    fivmr_assert((execFlags&FIVMR_TSEF_SOFT_HANDSHAKE)==0);
                }
            }
	    vm->handshakeThreads[numHandshakeThreads++]=ts;
	}
    }
    fivmr_Lock_unlock(&vm->lock);
    
    /* we have a snapshot of previously running threads.  we don't care about
       new threads or threads that are dying. */
    
     /* all previous soft handshake requests should have been accounted for. */
    fivmr_assert(vm->softHandshakeCounter==0 || (vm->exiting && !shouldWait));
    
    LOG(2,("soft handshake: Handshaking %u threads...",numHandshakeThreads));

    for (i=0;i<numHandshakeThreads;++i) {
	fivmr_ThreadState *ts;
	uintptr_t execStatus;
	
	ts=vm->handshakeThreads[i];
	vm->handshakeThreads[i]=NULL;
	
	fivmr_Lock_lock(&ts->lock);
	if (fivmr_ThreadState_isRunning(ts)) {
	    execStatus=fivmr_ThreadState_triggerBlock(ts);
            
	    switch (execStatus) {
	    case FIVMR_TSES_IN_JAVA_TO_BLOCK:
		LOG(2,("soft handshake: Thread %u is in Java; will wait for it.",
		       ts->id));
		ts->execFlags|=(execFlagsToSet|FIVMR_TSEF_SOFT_HANDSHAKE);
                if (shouldWait) {
                    fivmr_xchg_add32((int32_t*)&vm->softHandshakeCounter,1);
                }
		break;
            case FIVMR_TSES_STARTING:
	    case FIVMR_TSES_IN_NATIVE_TO_BLOCK:
		LOG(2,("soft handshake: Thread %u is in native or starting; hijacking it.",
		       ts->id));
		ts->execFlags|=execFlagsToSet;
		fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
		break;
	    default:
		/* nothing to do */
		LOG(2,("soft handshake: ignoring Thread %u in state %" PRIuPTR,
		       ts->id,execStatus));
		break;
	    }
	} else {
	    LOG(2,("soft handshale: Thread %u is not running, status = %" PRIuPTR,
		   ts->id,ts->execStatus));
	}
	fivmr_Lock_unlock(&ts->lock);
    }
    
    LOG(2,("soft handshake: still waiting for %u threads",
	   vm->softHandshakeCounter));
    
    if (shouldWait) {
        /* need a loop, because it's possible that there was a spurious notification */
        while (vm->softHandshakeCounter>0 && (!vm->exiting || ignoreExit)) {
            fivmr_Semaphore_down(&vm->softHandshakeNotifier);
        }
        
        fivmr_assert(vm->softHandshakeCounter==0 || vm->exiting);
    }
    
    fivmr_Lock_unlock(&vm->handshakeLock);

    LOG(3,("Soft handshake complete."));
}

void fivmr_ThreadState_softPairHandshake(fivmr_ThreadState *ts,
                                         uintptr_t execFlagsToSet) {
    LOG(2,("soft pair handshake: Handshaking Thread #%u...",ts->id));

    fivmr_Lock_lock(&ts->lock);
    if (!ts->vm->exiting && fivmr_ThreadState_isRunning(ts)) {
        uintptr_t execStatus=fivmr_ThreadState_triggerBlock(ts);
        switch (execStatus) {
        case FIVMR_TSES_IN_JAVA_TO_BLOCK:
            LOG(2,("soft pair handshake on Thread #%u: in Java, will wait for it.",
                   ts->id));
            ts->execFlags|=execFlagsToSet;
            while ((ts->execFlags&execFlagsToSet)!=0) {
                fivmr_Lock_wait(&ts->lock);
            }
            break;
        case FIVMR_TSES_STARTING:
        case FIVMR_TSES_IN_NATIVE_TO_BLOCK:
            LOG(2,("soft pair handshake on Thread #%u: in native or starting; hijacking it.",
                   ts->id));
            ts->execFlags|=execFlagsToSet;
            fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
            break;
        default:
            LOG(2,("soft handshake on Thread %u: ignoring, in state %" PRIuPTR,
                   ts->id,execStatus));
            break;
        }
    }
    
    fivmr_Lock_unlock(&ts->lock);
}

/* this will be called from the following contexts:
   1) ts == current thread, status is TERMINATING
   2) ts == current thread, status is Java (pollcheck, native transitions)
   3) ts == soft handshake request thread, status is whatever it was when
            the soft handshake was initiated
*/
void fivmr_ThreadState_serviceSoftHandshakeRequest(fivmr_ThreadState *ts) {
    fivmr_ThreadState_checkExitInHandshake(ts);

    if ((ts->execFlags&FIVMR_TSEF_PUSH_TYPE_EPOCH)!=0) {
        ts->typeEpoch=ts->vm->typeEpoch;
        ts->execFlags&=~FIVMR_TSEF_PUSH_TYPE_EPOCH;
    }
    
    if ((ts->execFlags&FIVMR_TSEF_SF_GC_REQ_MASK)!=0) {
	fivmr_GC_handleHandshake(ts);
	fivmr_assert((ts->execFlags&FIVMR_TSEF_SF_GC_REQ_MASK)==0);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_TRACE_STACK)!=0) {
	fivmr_Debug_traceStack(ts);
	fivmr_assert((ts->execFlags&FIVMR_TSEF_TRACE_STACK)==0);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_DUMP_STACK)!=0) {
	fivmr_Debug_dumpStack(ts);
	fivmr_assert((ts->execFlags&FIVMR_TSEF_DUMP_STACK)==0);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_UNBIAS)!=0) {
        fivmr_Monitor_unbiasFromHandshake(ts);
	fivmr_assert((ts->execFlags&FIVMR_TSEF_UNBIAS)==0);
    }
    
    fivmr_assert((ts->execFlags&FIVMR_TSEF_SF_REQ_MASK)==0); /* assert that we serviced
								all of them. */
}

void fivmr_ThreadState_acknowledgeSoftHandshakeRequest(fivmr_ThreadState *ts) {
    bool doit=false;
    doit=(ts->execFlags&FIVMR_TSEF_SOFT_HANDSHAKE)!=0;
    ts->execFlags&=~FIVMR_TSEF_SOFT_HANDSHAKE;
    fivmr_assert((ts->execFlags&FIVMR_TSEF_SF_REQ_MASK)==0); /* assert that we serviced
								all of them. */
    
    if (doit) {
	LOG(3,("Thread %u acknowledging soft handshake.",ts->id));
	if (fivmr_xchg_add32((int32_t*)&ts->vm->softHandshakeCounter,-1)==1) {
            fivmr_Semaphore_up(&ts->vm->softHandshakeNotifier);
	}
    }
}

uintptr_t fivmr_ThreadState_reqSuspend(fivmr_ThreadState *ts) {
    uintptr_t result;
    LOG(1,("Thread %u requesting suspension of Thread %u.",
	   fivmr_ThreadState_get(ts->vm)->id,ts->id));
    fivmr_Lock_lock(&ts->lock);
    if (fivmr_ThreadState_isRunning(ts)) {
        result=fivmr_ThreadState_triggerBlock(ts); /* this will do priority boosting */
    } else {
        result=ts->execStatus;
    }
    ts->suspendReqCount++;
    fivmr_Lock_unlock(&ts->lock);
    return result;
}

uintptr_t fivmr_ThreadState_waitForSuspend(fivmr_ThreadState *ts) {
    uintptr_t result;
    int64_t version;
    
    LOG(1,("Thread %u waiting for suspension of Thread %u.",
	   fivmr_ThreadState_get(ts->vm)->id,ts->id));

    /* FIXME: one way of dealing with the possibility of this being a new
       thread is to return something special if the suspendReqCount is
       zero.... */
    
    fivmr_Lock_lock(&ts->lock);
    
    fivmr_assert(ts->suspendReqCount>0 ||
                 ts->suspendCount>0);
    
    version=ts->version;
    while (fivmr_ThreadState_isInJava(ts) && ts->version==version) {
        fivmr_Lock_wait(&ts->lock);
    }
    if (ts->version==version) {
        result=ts->execStatus;
    } else {
        result=FIVMR_TSES_CLEAR;
    }
    fivmr_Lock_unlock(&ts->lock);
    
    LOG(1,("Thread %u done waiting for suspension of Thread %u, result = %"
	   PRIuPTR ".",
	   fivmr_ThreadState_get(ts->vm)->id,ts->id,result));

    return result;
}

uintptr_t fivmr_ThreadState_suspend(fivmr_ThreadState *ts) {
    uintptr_t result;
    fivmr_Lock_lock(&ts->lock);
    result=fivmr_ThreadState_reqSuspend(ts);
    if (fivmr_ThreadStatus_isRunning(result)) {
        result=fivmr_ThreadState_waitForSuspend(ts);
    } else {
        result=ts->execStatus;
    }
    fivmr_Lock_unlock(&ts->lock);
    return result;
}

void fivmr_ThreadState_resume(fivmr_ThreadState *ts) {
    LOG(1,("Thread %u resuming Thread %u.",
	   fivmr_ThreadState_get(ts->vm)->id,ts->id));

    fivmr_Lock_lock(&ts->lock);
    if (ts->suspendCount==0) {
        ts->suspendReqCount--;
    } else {
        ts->suspendCount--;
    }
    fivmr_assert(ts->suspendReqCount>=0);
    fivmr_assert(ts->suspendCount>=0);
    fivmr_Lock_unlock(&ts->lock);
    fivmr_Semaphore_up(&ts->waiter); /* wake the thread up */
}

/* WARNING: this code is not guaranteed to be race-free */
void fivmr_ThreadState_performAllGuaranteedCommits(fivmr_VM *vm) {
    unsigned i;
    for (i=2;i<vm->config.maxThreads;++i) {
        fivmr_ThreadState *ts=fivmr_ThreadState_byId(vm,i);
        fivmr_Lock_lock(&ts->lock);
        if (fivmr_ThreadState_isRunning(ts)) {
            fivmr_ThreadState_guaranteedCommit(ts);
        }
        fivmr_Lock_unlock(&ts->lock);
    }
}

void fivmr_ThreadState_setInterrupted(fivmr_ThreadState *ts,
                                      bool value) {
    fivmr_Lock_lock(&ts->lock);
    if (ts->interrupted!=value) {
        ts->interrupted=value;
        fivmr_Lock_broadcast(&ts->lock);
    }
    fivmr_Lock_unlock(&ts->lock);
}

/* boosted spin lock support - which is closely tied to ThreadState */

void fivmr_BoostedSpinLock_init(fivmr_BoostedSpinLock *bsl) {
    fivmr_SpinLock_init(&bsl->spinlock);
}

void fivmr_BoostedSpinLock_destroy(fivmr_BoostedSpinLock *bsl) {
    fivmr_SpinLock_destroy(&bsl->spinlock);
}

void fivmr_BoostedSpinLock_lock(fivmr_ThreadState *ts,
                                fivmr_BoostedSpinLock *bsl) {
    /* FIXME: this should first boost the thread's priority then acquire the lock,
       or something.  probably first acquiring the lock, then boosting priority
       is better. */
    for (;;) {
        fivmr_Lock_lock(&ts->lock);
        
        if (FIVMR_STRICT_BOOST) {
            fivmr_SpinLock_assertLock(&bsl->spinlock);
            break;
        }
        
        if (fivmr_SpinLock_tryLock(&bsl->spinlock)) {
            break;
        }
        
        fivmr_Lock_unlock(&ts->lock);
    }
}

void fivmr_BoostedSpinLock_unlock(fivmr_ThreadState *ts,
                                  fivmr_BoostedSpinLock *bsl) {
    fivmr_SpinLock_unlock(&bsl->spinlock);
    fivmr_Lock_unlock(&ts->lock);
}


