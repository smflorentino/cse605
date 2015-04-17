/*
 * fivmr_runtime.c
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
#include "fivmr_asm.h"
#include "fivmr_logger.h"

fivmr_VM *fivmr_vmListHead;
fivmr_Lock fivmr_vmListLock;

#if FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT
static void sigquit_handler(int sig,siginfo_t *info,void *arg) {
    fivmr_VM *vm;
    fivmr_Lock_lock(&fivmr_vmListLock);
    for (vm=fivmr_vmListHead;vm!=NULL;vm=vm->next) {
        fivmr_CritSemaphore_up(&vm->dumpStateSemaphore);
    }
    fivmr_Lock_unlock(&fivmr_vmListLock);
}

static void sigquit_thread(void *arg) {
    fivmr_VM *vm=(fivmr_VM*)arg;
    LOG(1,("SIGQUIT thread running, waiting for signal."));
    for (;;) {
        fivmr_CritSemaphore_down(&vm->dumpStateSemaphore);
        if (vm->exiting) {
            fivmr_Semaphore_up(&vm->dumpStateDone);
            return;
        }
        fivmr_Log_lockedPrintf("-- START fivmr: Dumping stacks from all threads --\n");
        fivmr_Debug_dumpAllStacks(vm);
        fivmr_Log_lockedPrintf("-- END fivmr: Dumping stacks from all threads --\n");
    }
}
#endif

void fivmr_VM_initGlobal(void) {
#if (FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT)
    struct sigaction sa;
#endif

    fivmr_vmListHead=NULL;
    fivmr_Lock_init(&fivmr_vmListLock,
                    FIVMR_PR_CRITICAL); /* FIXME this needs to be made
                                           into a truly critical lock */
    
#if FIVMR_SUPPORT_SIGQUIT
#  if FIVMR_CAN_HANDLE_SIGQUIT
    bzero(&sa,sizeof(sa));
    sa.sa_sigaction=sigquit_handler;
    sigfillset(&sa.sa_mask);
    sa.sa_flags=SA_SIGINFO|SA_RESTART;
    sigaction(SIGQUIT,&sa,NULL);
    LOG(1,("SIGQUIT debug handler initialized."));
#  else
    LOG(1,("SIGQUIT debug handler not initialized because of inadequate OS support."));
#  endif
#endif
}

void fivmr_VM_resetSettings(fivmr_VM *vm,
                            fivmr_Configuration *config) {
    fivmr_assert(vm!=NULL);
    fivmr_assert(config!=NULL);
    
    /* zero everything except for the lock */
    bzero(vm,sizeof(fivmr_VM));
    
    vm->monitorSpinLimit=100;
    vm->config=*config;
    vm->maxPriority=FIVMR_TPR_MAX;
    fivmr_GC_resetSettings(&vm->gc);
}

void fivmr_VM_init(fivmr_VM *vm) {
    uint32_t i;
    int res;
    uint64_t tmp;
    
    fivmr_assert_cas(&vm->state,FIVMR_VMS_IDLE,FIVMR_VMS_INITING);
    
    LOG(1,("Initializing runtime at %p...",vm));
    
    if (vm->gc.logGC) {
        vm->gc.logSyncGC=true;
    }
    
    LOG(2,("logGC = %d, logSyncGC = %d",vm->gc.logGC,vm->gc.logSyncGC));

    tmp=vm->config.maxThreads; /* HACK! prevents compiler pukage on 64-bit builds */
    fivmr_assert((tmp-1)<=(FIVMR_MS_TID_MASK>>FIVMR_MS_TID_SHIFT));
    fivmr_assert((FIVMR_MS_RC_MASK&FIVMR_MS_TID_MASK)==0);
    fivmr_assert((FIVMR_MS_QUEUED&FIVMR_MS_RC_MASK)==0);
    fivmr_assert((FIVMR_MS_QUEUED&FIVMR_MS_TID_MASK)==0);
    fivmr_assert((FIVMR_MS_UNBIASED&FIVMR_MS_QUEUED)==0);
    fivmr_assert((FIVMR_MS_UNBIASED&FIVMR_MS_RC_MASK)==0);
    fivmr_assert((FIVMR_MS_UNBIASED&FIVMR_MS_TID_MASK)==0);
    
    fivmr_Lock_init(&vm->lock,
		    fivmr_Priority_bound(FIVMR_PR_CRITICAL,
                                         vm->maxPriority));
    
    for (i=0;i<(uint32_t)vm->payload->nContexts;++i) {
        fivmr_TypeContext_boot(vm,vm->baseContexts[i]);
    }
    
    fivmr_Lock_init(&vm->typeDataLock,
                    FIVMR_PR_INHERIT);
    fivmr_Lock_init(&vm->thunkingLock,
                    FIVMR_PR_MAX);
    
    fivmr_Lock_init(&vm->deathLock,
		    fivmr_Priority_bound(FIVMR_PR_MAX,
                                         vm->maxPriority));
    fivmr_Semaphore_init(&vm->softHandshakeNotifier);
    fivmr_Lock_init(&vm->handshakeLock,
		    fivmr_Priority_bound(FIVMR_PR_MAX,
                                         vm->maxPriority));
    fivmr_Lock_init(&vm->hrLock,
		    fivmr_Priority_bound(FIVMR_PR_CRITICAL,
                                         vm->maxPriority));
    fivmr_ThreadSpecific_init(&vm->curThread);
    fivmr_HandleRegion_init(&vm->hr);
    vm->freeHandles=NULL;
    
    vm->dynContexts=NULL;
    vm->nDynContexts=0;
    vm->dynContextsSize=0;
    
    vm->zero=0.0;
    
    LOG(1,("Locks, utils, thread-specifics, and handle-regions initialized."));

#if FIVMR_SUPPORT_SIGQUIT
#  if FIVMR_CAN_HANDLE_SIGQUIT
    fivmr_CritSemaphore_init(&vm->dumpStateSemaphore);
    fivmr_Semaphore_init(&vm->dumpStateDone);
    
    if (vm->pool==NULL) {
        fivmr_Thread_spawn(sigquit_thread,vm,FIVMR_TPR_NORMAL_MIN);
    } else {
        fivmr_ThreadPool_spawn(vm->pool,sigquit_thread,vm,FIVMR_TPR_NORMAL_MIN);
    }

    LOG(1,("SIGQUIT debug support initialized."));
#  else
    LOG(1,("SIGQUIT debug support not initialized because of inadequate OS support."));
#  endif
#endif

    LOG(1,("Allocating memory for %u threads.",vm->config.maxThreads));
    vm->threadById=fivmr_mallocAssert(sizeof(fivmr_ThreadState)*vm->config.maxThreads);
    bzero(vm->threadById,
          sizeof(fivmr_ThreadState)*vm->config.maxThreads);
    
    vm->javaThreads=(fivmr_Handle*)fivmr_mallocAssert(
        sizeof(fivmr_Handle)*vm->config.maxThreads);
    vm->threads=(fivmr_ThreadState**)fivmr_mallocAssert(
        sizeof(fivmr_ThreadState*)*vm->config.maxThreads);
    vm->handshakeThreads=(fivmr_ThreadState**)fivmr_mallocAssert(
        sizeof(fivmr_ThreadState*)*vm->config.maxThreads);

    /* NOTE: the offsetof assertions in this file SHOULD NOT BE REMOVED.  they
       are here because our assembly code (just fivmr_asm_x86.S currently) uses
       those offsets directly.  DO NOT ATTEMPT to remove these assertions, and 
       DO NOT ATTEMPT to change their values unless you change the assembly code
       as well.  And if you don't know why it's failing, or if the previous
       few sentences make no sense, then just go cower in some cave and stop
       trying to hack VMs because it's obviously too much for you to handle. */
    
#if FIVMR_CAN_DO_CLASSLOADING
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveField)==0);
    vm->resolveField=fivmr_resolveField;
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveMethod)==sizeof(void*));
    vm->resolveMethod=fivmr_resolveMethod;
    fivmr_assert(fivmr_offsetof(fivmr_VM,baselineThrow)==sizeof(void*)*2);
    vm->baselineThrow=fivmr_baselineThrow;
    fivmr_assert(fivmr_offsetof(fivmr_VM,pollcheckSlow)==sizeof(void*)*3);
    vm->pollcheckSlow=fivmr_ThreadState_pollcheckSlow;
    fivmr_assert(fivmr_offsetof(fivmr_VM,throwNullPointerRTE_inJava)==sizeof(void*)*4);
    vm->throwNullPointerRTE_inJava=fivmr_throwNullPointerRTE_inJava;
    fivmr_assert(fivmr_offsetof(fivmr_VM,throwArrayBoundsRTE_inJava)==sizeof(void*)*5);
    vm->throwArrayBoundsRTE_inJava=fivmr_throwArrayBoundsRTE_inJava;
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveArrayAlloc)==sizeof(void*)*6);
    vm->resolveArrayAlloc=fivmr_resolveArrayAlloc;
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveObjectAlloc)==sizeof(void*)*7);
    vm->resolveObjectAlloc=fivmr_resolveObjectAlloc;
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveInstanceof)==sizeof(void*)*8);
    vm->resolveInstanceof=fivmr_resolveInstanceof;
    fivmr_assert(fivmr_offsetof(fivmr_VM,throwStackOverflowRTE_inJava)==sizeof(void*)*9);
    vm->throwStackOverflowRTE_inJava=fivmr_throwStackOverflowRTE_inJava;
#endif

    bzero(vm->javaThreads,sizeof(fivmr_Handle)*vm->config.maxThreads);
    bzero(vm->threads,sizeof(fivmr_ThreadState*)*vm->config.maxThreads);
    bzero(vm->handshakeThreads,sizeof(fivmr_ThreadState*)*vm->config.maxThreads);
    
    if (sizeof(void*)==4) {
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,pollingUnion)==0);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,cookie)==4);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,curException)==8);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,vm)==12);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,curF)==16);
        fivmr_assert((intptr_t)&((fivmr_ThreadState*)0)->gc.alloc[0].zero<=127);
        int correct  = fivmr_offsetof(fivmr_ThreadState,regSave);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,regSave)==FIVMR_OFFSETOF_REGSAVE_0);
        
        fivmr_assert(sizeof(fivmr_Frame)==sizeof(uintptr_t)*3);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,id)==0);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,up)==4);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,refs)==8);
    } else {
        fivmr_assert(sizeof(void*)==8);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,pollingUnion)==0);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,cookie)==4);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,curException)==8);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,vm)==16);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,curF)==24);

        fivmr_assert(sizeof(fivmr_Frame)==sizeof(uintptr_t)*3);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,id)==0);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,up)==8);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,refs)==16);
    }
    
    vm->maxThreadID=1;

    for (i=0;i<vm->config.maxThreads;++i) {
        LOG(2,("Initializing thread slot %u",i));
	fivmr_ThreadState_byId(vm,i)->cookie=0xd1e7c0c0;
	fivmr_ThreadState_byId(vm,i)->id=i;
	fivmr_ThreadState_byId(vm,i)->lockingId=i<<FIVMR_MS_TID_SHIFT;
        fivmr_ThreadState_byId(vm,i)->vm=vm;

#if FIVMR_CAN_DO_CLASSLOADING
        fivmr_ThreadState_byId(vm,i)->pollcheckSlowBaseline=fivmr_pollcheckSlowBaseline;
        fivmr_ThreadState_byId(vm,i)->nullCheckSlowBaseline=fivmr_nullCheckSlowBaseline;
        fivmr_ThreadState_byId(vm,i)->abcSlowBaseline=fivmr_abcSlowBaseline;
        fivmr_ThreadState_byId(vm,i)->stackHeightSlowBaseline=fivmr_stackHeightSlowBaseline;
        fivmr_ThreadState_byId(vm,i)->baselineThrowThunk=fivmr_baselineThrowThunk;
        fivmr_ThreadState_byId(vm,i)->baselineProEpThrowThunk=fivmr_baselineProEpThrowThunk;
        fivmr_ThreadState_byId(vm,i)->resolveFieldAccessThunk=fivmr_resolveFieldAccessThunk;
        fivmr_ThreadState_byId(vm,i)->resolveMethodCallThunk=fivmr_resolveMethodCallThunk;
        fivmr_ThreadState_byId(vm,i)->resolveInvokeInterfaceThunk=fivmr_resolveInvokeInterfaceThunk;
        fivmr_ThreadState_byId(vm,i)->resolveArrayAllocThunk=fivmr_resolveArrayAllocThunk;
        fivmr_ThreadState_byId(vm,i)->resolveObjectAllocThunk=fivmr_resolveObjectAllocThunk;
        fivmr_ThreadState_byId(vm,i)->resolveInstanceofThunk=fivmr_resolveInstanceofThunk;
#endif
        
        /* this needs to be MAX for PIP
           to work; but it means we'll have to
           change the way stack scans work to
           go back to having timely soft
           handshakes - AND we'll have to do
           something about hardrtj. */
        /* details: the reason why we need MAX for PIP is that we rely on
           the property that acquiring the lock makes us the highest priority
           thread on a given processor.  we then may end up contending on
           spin locks, which is safe if we're the highest priority in the
           system but unsafe otherwise. */
	fivmr_Lock_init(&fivmr_ThreadState_byId(vm,i)->lock,
			fivmr_Priority_bound(FIVMR_PR_MAX,
                                             vm->maxPriority));
        fivmr_Semaphore_init(&fivmr_ThreadState_byId(vm,i)->waiter);
    }

    LOG(1,("All thread slots ready."));
    
    if (FIVMR_SCOPED_MEMORY(&vm->settings)) {
        fivmr_MemoryAreas_init(&vm->gc);
    }
    fivmr_GC_init(&vm->gc);
    
    fivmr_Lock_lock(&fivmr_vmListLock);
    vm->prev=NULL;
    vm->next=fivmr_vmListHead;
    if (fivmr_vmListHead!=NULL) {
        fivmr_vmListHead->prev=vm;
    }
    fivmr_vmListHead=vm;
    fivmr_Lock_unlock(&fivmr_vmListLock);

#if FIVMR_INTERNAL_INST
    fivmr_ii_start(vm);
#endif

    fivmr_assert_cas(&vm->state,FIVMR_VMS_INITING,FIVMR_VMS_RUNNING);

    fivmr_debugMemory();

    LOG(1,("Runtime initialized."));
}

bool fivmr_VM_waitForDeath(fivmr_VM *vm) {
    LOG(1,("Waiting for all threads to die..."));
    
    fivmr_Lock_lock(&vm->deathLock);
    while (vm->numActive-vm->numDaemons>0 && !vm->exiting) {
	LOG(2,("Waiting for death with active = %u, daemons = %u",
	       vm->numActive,vm->numDaemons));
	fivmr_Lock_wait(&vm->deathLock);
    }
    fivmr_Lock_unlock(&vm->deathLock);

    if (vm->exiting) {
        LOG(1,("VM force-exited."));
        return false;
    } else {
        LOG(1,("No more threads running.  Ready to exit."));
        return true;
    }
}

#if FIVMR_SPC_ENABLED
void fivmc_SPC_dump(void);
#endif

bool fivmr_VM_exit(fivmr_VM *vm,int32_t status) {
    /* NOTE!!!!  there is still Java code running while this is called.
       For a "normal" JVM this is to be expected.  We cannot reasonably
       prevent it.  So don't try to prevent it, and don't stupidly
       assume that it isn't happening. */
    
    LOG(1,("fivmr_VM_exit() called with status = %d",(int)status));

    if (!fivmr_cas(&vm->state,FIVMR_VMS_RUNNING,FIVMR_VMS_EXITING)) {
        return false;
    }
    
    fivmr_Lock_lock(&vm->lock);
    fivmr_assert(!vm->exitCodeSet);
    vm->exitCode=status;
    vm->exitCodeSet=true;
    fivmr_Lock_unlock(&vm->lock);
    
    fivmr_ThreadState_performAllGuaranteedCommits(vm);
    
#if FIVMR_INTERNAL_INST
    fivmr_ii_end(vm);
#endif
    
#if FIVMR_SPC_ENABLED
    fivmc_SPC_dump();
#endif
    fivmr_SPC_dump();
    fivmr_PR_dump();
    
    if (FIVMR_COVERAGE) {
        int i;
        fivmr_Log_printf("Coverage:\n");
        for (i=0;i<vm->payload->nTypes;++i) {
            fivmr_TypeData *td=fivmr_TypeData_list(vm)[i];
            unsigned j;
            for (j=0;j<td->numMethods;++j) {
                fivmr_MethodRec *mr=td->methods[j];
                if ((mr->flags&FIVMR_MBF_COV_CALLED)) {
                    fivmr_Log_printf("   %s\n",fivmr_MethodRec_describe(mr));
                }
            }
        }
    }
    
    fivmr_GC_finalReport(&vm->gc);
    
    if (vm->exitExits) {
#if FIVMR_POSIX
        fivmr_Log_lock();
        fp_commit(fivmr_log);
        fivmr_Log_unlock();
#else
        /* bonk! */
        fp_commit(stderr);
#endif

        exit((int)status);
    } else {
        fivmr_ThreadState *ts;
        
        /* possible solutions:
           - treat thread death as a special exception
           pro: seems simple
           con: Java code can handle exceptions; so this would have to be an
           exception that the code cannot handle (i.e. not Throwable).
           moreover, safepoints currently cannot throw exceptions.  maybe
           it would be good to keep it that way...  indeed, the compiler
           does not take kindly to the idea of a late-inserted op
           throwing exceptions
           - longjmp
           pro: seems simple
           con: may skip over important code.  makes it harder to handle
           VM death */
        /* we'll use the longjmp trick... */
        
        LOG(1,("Triggering VM death."));
        
        ts=fivmr_ThreadState_getNullable(vm);

        fivmr_Lock_lock(&vm->lock);
        vm->exiting=true;
        vm->exitInitiator=ts;
        fivmr_Lock_unlock(&vm->lock);
        
        /* make sure that the GC signals any threads waiting on a GC */
        fivmr_GC_signalExit(&vm->gc);
        
        /* make sure that soft handshakes exit */
        fivmr_Semaphore_up(&vm->softHandshakeNotifier);
        
        /* no new threads can be started */

        fivmr_ThreadState_softHandshakeImpl(vm,0,0,false,true);
        
        /* threads should start exiting */
        
#if FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT
        fivmr_CritSemaphore_up(&vm->dumpStateSemaphore);
#endif
        
        fivmr_Lock_lockedBroadcast(&vm->deathLock);

        if (ts!=NULL) {
            LOG(1,("Exiting the thread that initiated exit..."));
            fivmr_ThreadState_exitImpl(ts);
            fivmr_assert(false);
        }
    }
    
    return true;
}

void fivmr_VM_shutdown(fivmr_VM *vm,
                       int32_t *exitCode) {
    uint32_t i;
    
    if (FIVMR_ASSERTS_ON) {
        fivmr_assert(vm->state==FIVMR_VMS_EXITING);

        fivmr_Lock_lock(&vm->lock);
        fivmr_assert(vm->exitCodeSet);
        fivmr_Lock_unlock(&vm->lock);
    }
    
    fivmr_Lock_lock(&vm->deathLock);
    while (vm->numRunning>0) {
        fivmr_Lock_wait(&vm->deathLock);
    }
    fivmr_Lock_unlock(&vm->deathLock);
    
    fivmr_Lock_lock(&vm->typeDataLock);
    fivmr_Lock_unlock(&vm->typeDataLock);
    
#if FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT
    fivmr_Semaphore_down(&vm->dumpStateDone);
#endif
    
    fivmr_GC_shutdown(&vm->gc);

    fivmr_free(vm->javaThreads);
    fivmr_free(vm->threads);
    fivmr_free(vm->handshakeThreads);
    
    if (vm->payload->mode!=FIVMR_PL_IMMORTAL_ONESHOT) {
        fivmr_free(vm->primFields);
        fivmr_free(vm->refFields);
    }
    
    for (i=0;i<(uint32_t)vm->payload->nContexts;++i) {
        fivmr_TypeContext_destroy(vm->baseContexts[i]);
   }
    /* FIXME: free the TypeDataNode's */
    
    fivmr_Payload_free(vm->payload);
    
    for (i=0;i<vm->config.maxThreads;++i) {
        fivmr_Lock_destroy(&fivmr_ThreadState_byId(vm,i)->lock);
        fivmr_Semaphore_destroy(&fivmr_ThreadState_byId(vm,i)->waiter);
        if (fivmr_ThreadState_byId(vm,i)->buf!=NULL) {
            fivmr_free(fivmr_ThreadState_byId(vm,i)->buf);
        }
    }
    
    fivmr_free(vm->threadById);
    fivmr_free(vm->usedTids);
    fivmr_free(vm->itableOcc);
    
    fivmr_Lock_destroy(&vm->lock);
    fivmr_Lock_destroy(&vm->deathLock);
    fivmr_Semaphore_destroy(&vm->softHandshakeNotifier);
    fivmr_Lock_destroy(&vm->handshakeLock);
    fivmr_Lock_destroy(&vm->hrLock);
    fivmr_ThreadSpecific_destroy(&vm->curThread); /* FIXME this doesn't work yet on
                                                     RTEMS */
    fivmr_Lock_destroy(&vm->typeDataLock);
    fivmr_Lock_destroy(&vm->thunkingLock);
    
    while (vm->freeHandles!=NULL) {
        fivmr_Handle *next=vm->freeHandles->next;
        free(vm->freeHandles);
        vm->freeHandles=next;
    }
    
    fivmr_Lock_lock(&fivmr_vmListLock);
    if (vm->prev==NULL) {
        fivmr_vmListHead=vm->next;
    } else {
        vm->prev->next=vm->next;
    }
    if (vm->next!=NULL) {
        vm->next->prev=vm->prev;
    }
    fivmr_Lock_unlock(&fivmr_vmListLock);
    
    if (exitCode!=NULL) {
        fivmr_assert(vm->exitCodeSet);
        *exitCode=vm->exitCode;
    }
    
    fivmr_debugMemory();

    LOG(1,("Shut down runtime at %p.",vm));
}



