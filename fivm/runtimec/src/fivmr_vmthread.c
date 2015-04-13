/*
 * fivmr_vmthread.c
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

static void vmthread_runner(void *arg) {
    fivmr_ThreadState *ts;
    fivmr_JmpBuf volatile jmpbuf;
    
    ts=(fivmr_ThreadState*)arg;
    
    if (ts->vm->pool==NULL) {
        ts->exitOnExit=true;
    } else {
        ts->jumpOnExit=(fivmr_JmpBuf*)&jmpbuf;
        if (fivmr_JmpBuf_label((fivmr_JmpBuf*)&jmpbuf)) {
            LOG(1,("Jumped out of VM; returning. (1)"));
            return;
        }
    }
    
    fivmr_assert(fivmr_ThreadPriority_leRT(fivmr_Thread_getPriority(fivmr_ThreadHandle_current()),
                                           ts->vm->maxPriority));
    
    fivmr_ThreadState_set(ts,NULL);
    
    fivmr_VMThread_run(ts,ts->vm->javaThreads+ts->id);
    
    if (ts->curExceptionHandle!=NULL) {
	fivmr_Handle *e;
	
	fivmr_ThreadState_goToJava(ts);
	e=fivmr_ThreadState_cloneHandle(ts,ts->curExceptionHandle);
	ts->curExceptionHandle=NULL;
	fivmr_ThreadState_goToNative(ts);
	
	if (!fivmr_VMThread_setUncaughtException(ts,ts->vm->javaThreads+ts->id,e)) {
	    LOG(0,("Thread %u terminated with exception; could "
		   "not pass it to uncaught exception handler.",
		   ts->id));
	    fivmr_describeException(ts,e);
	}
    }
    
    fivmr_ThreadState_terminate(ts);
}

void fivmr_VMThread_priorityChanged(fivmr_ThreadState *curTS,
                                    fivmr_Handle *vmt) {
    int32_t javaPrio=fivmr_VMThread_getPriority(curTS,vmt);
    fivmr_ThreadState *ts=fivmr_VMThread_getThreadState(curTS,vmt);
    fivmr_assert(ts->thread!=fivmr_ThreadHandle_zero());
    
    fivmr_assert(fivmr_ThreadPriority_leRT(javaPrio,curTS->vm->maxPriority));
    
    fivmr_ThreadState_setBasePrio(ts,javaPrio);
}

void fivmr_VMThread_start(fivmr_ThreadState *curTS,fivmr_Handle *vmt) {
    fivmr_ThreadState *ts;
    fivmr_ThreadHandle th;
    fivmr_ThreadPriority prio;
    
    ts=fivmr_ThreadState_new(curTS->vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
    if (ts==NULL) {
        LOG(1,("Could not get a new ThreadState; are you sure you aren't starting "
               "more threads than the system can support?  Max threads = %u, "
               "numActive = %u, numDaemons = %u.",
               curTS->vm->config.maxThreads,curTS->vm->numActive,curTS->vm->numDaemons));
        goto error;
    }
    
    if (!fivmr_ThreadState_glue(ts,vmt)) {
        LOG(1,("Could not glue thread state; VM is probably exiting."));
        goto error;
    }
    
    fivmr_FlowLog_log(curTS, FIVMR_FLOWLOG_TYPE_THREAD,
                      FIVMR_FLOWLOG_SUBTYPE_CREATE, ts->id);
    prio=fivmr_VMThread_getPriority(curTS,vmt);
    fivmr_ThreadState_setInitPrio(ts,prio);
    fivmr_ThreadState_setStackHeight(ts,fivmr_Thread_stackHeight()-FIVMR_STACK_HEIGHT_HEADROOM);
    
    if (curTS->vm->pool==NULL) {
        th=fivmr_Thread_spawn(vmthread_runner,ts,prio);
    } else {
        th=fivmr_ThreadPool_spawn(curTS->vm->pool,vmthread_runner,ts,prio)->thread;
    }
    if (th==fivmr_ThreadHandle_zero()) {
        /* FIXME currently we leak a thread state... */
        LOG(1,("Could not create a system thread; are you sure you aren't starting "
               "more threads than the system can support?"));
        goto error;
    }

    fivmr_ThreadState_setThread(ts,th);

#if FIVMR_FLOW_LOGGING
    ts->flowbuf = fivmr_malloc(FIVMR_FLOWLOG_BUFFERSIZE);
    memset(ts->flowbuf, 0, FIVMR_FLOWLOG_BUFFERSIZE);
#endif

    return;
error:
    fivmr_ThreadState_checkExit(curTS);
    fivmr_ThreadState_goToJava(curTS);
    fivmr_throwOutOfMemoryError_inJava(curTS);
    fivmr_ThreadState_goToNative(curTS);
}

struct PooledThreadInfo_s {
    fivmr_ThreadPriority parentPriority;
    fivmr_Handle *h;
    bool ok;
    fivmr_Semaphore inited;
    bool done;
};

typedef struct PooledThreadInfo_s PooledThreadInfo;

static void pooledthread_runner(void *arg) {
    fivmr_Handle *h;
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    PooledThreadInfo *pti;
    bool ok;
    fivmr_ThreadPriority basePrio;
    fivmr_Handle *vmt;
    
    pti=(PooledThreadInfo*)arg;
    h=pti->h;
    vm=h->vm;
    
    ok=false;
    pti->ok=false;
    
    basePrio=
        fivmr_Thread_getPriority(
            fivmr_ThreadHandle_current());
    
    fivmr_assert(fivmr_ThreadPriority_leRT(basePrio,
                                           vm->maxPriority));

    ts=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
    if (ts==NULL) {
        /* error! */
    } else {
        fivmr_ThreadState_setBasePrio(ts,
                                      basePrio);
        fivmr_ThreadState_setStackHeight(
            ts,
            fivmr_Thread_stackHeight()-FIVMR_STACK_HEIGHT_HEADROOM);
        fivmr_ThreadState_set(ts,NULL);
        vmt=fivmr_VMThread_create(ts,0,false);
        if (vmt!=NULL) {
            fivmr_assert(vmt!=NULL);
            fivmr_assert(ts->curExceptionHandle==NULL);
            if (fivmr_ThreadState_glue(ts,vmt)) {
                ok=true;
                pti->ok=true;
            }
            /* FIXME: We don't necessarily have a Java thread at this point */
            fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_THREAD,
                              FIVMR_FLOWLOG_SUBTYPE_CREATE, ts->id);
        }
        
        /* FIXME if an error occurs here we're currently leaking a thread state */
    }
    
    /* let the parent know the status */
    fivmr_Thread_setPriority(
        fivmr_ThreadHandle_current(),
        fivmr_ThreadPriority_max(
            basePrio,
            pti->parentPriority));
    fivmr_Semaphore_up(&pti->inited);
    fivmr_fence();
    pti->done=true;
    
    /* now revert to the priority that we're supposed to be at */
    fivmr_Thread_setPriority(
        fivmr_ThreadHandle_current(),
        basePrio);
    
    LOG(1,("pooled thread %p running with priority %p",fivmr_ThreadHandle_current(),basePrio));
    
    if (ok) {
        fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                                 basePrio);

        fivmr_assert(fivmr_ThreadPriority_leRT(fivmr_Thread_getPriority(fivmr_ThreadHandle_current()),
                                               ts->vm->maxPriority));

        fivmr_runRunnable(ts,h);
        if (ts->curExceptionHandle) {
            fivmr_ThreadState_goToJava(ts);
            LOG(1,("Pooled thread threw exception: %s",
                   fivmr_TypeData_forObject(
                       &vm->settings,
                       ts->curExceptionHandle->obj)->name));
            ts->curExceptionHandle=NULL;
            fivmr_ThreadState_goToNative(ts);
        }
        
        fivmr_ThreadState_terminate(ts);
    }
}

void fivmr_VMThread_startPooledThread(fivmr_ThreadState *curTS,
                                      fivmr_ThreadPool *pool,
                                      fivmr_Handle *runnable,
                                      fivmr_ThreadPriority priority) {
    PooledThreadInfo pti;
    
    fivmr_ThreadState_goToJava(curTS);
    runnable=fivmr_newGlobalHandle(curTS,runnable);
    fivmr_ThreadState_goToNative(curTS);

    pti.parentPriority=fivmr_Thread_getPriority(fivmr_ThreadHandle_current());
    pti.h=runnable;
    pti.ok=false;
    fivmr_Semaphore_init(&pti.inited);
    pti.done=false;
    
    fivmr_ThreadPool_spawn(pool,pooledthread_runner,&pti,priority);
    
    fivmr_Semaphore_down(&pti.inited);
    fivmr_fence();
    while (!pti.done) {
        /* this shouldn't happen unless the OS scheduler is sloppy or
           we're on an SMP */
        fivmr_yield();
        fivmr_fence();
    }
    fivmr_fence();

    if (pti.ok) {
        /* ok! */
    } else {
        fivmr_ThreadState_goToJava(curTS);
        fivmr_throwOutOfMemoryError_inJava(curTS);
        fivmr_ThreadState_goToNative(curTS);
    }
}

