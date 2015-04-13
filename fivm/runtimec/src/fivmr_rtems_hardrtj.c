/*
 * fivmr_rtems_hardrtj.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS

#include <fivmr.h>

static fivmr_ThreadState *interruptTS;

static void interruptTrampoline(void *arg) {
    fivmr_NativeFrame nf;
    fivmr_VM *vm;
    fivmr_Handle *h;

    h=(fivmr_Handle*)arg;
    vm=h->vm;
    
    fivmr_assert(fivmr_Thread_isInterrupt());
    fivmr_assert(fivmr_ThreadHandle_current()==FIVMR_TH_INTERRUPT);
    fivmr_assert(fivmr_ThreadState_get(vm)==interruptTS);
    fivmr_ThreadState_go(interruptTS);
    fivmr_ThreadState_pushAndInitNF(interruptTS,&nf,NULL);
    LOG(2,("Running h = %p; type = %s",
           h,
           fivmr_TypeData_forObject(&vm->settings,h->obj)->name));
    fivmr_runRunnable(interruptTS,h);
    if (interruptTS->curExceptionHandle) {
	fivmr_ThreadState_goToJava(interruptTS);
	LOG(0,("Interrupt threw exception: %s",
	       fivmr_TypeData_forObject(
                   &vm->settings,
                   interruptTS->curExceptionHandle->obj)->name));
	interruptTS->curExceptionHandle=NULL;
	fivmr_ThreadState_goToNative(interruptTS);
    }
    fivmr_ThreadState_popNF(interruptTS);
    fivmr_ThreadState_goToJava(interruptTS);
    fivmr_deleteGlobalHandle(h);
    fivmr_ThreadState_goToNative(interruptTS);
    fivmr_ThreadState_commit(interruptTS);
}

static void timerTrampoline(rtems_id timer,void *arg) {
    LOG(2,("in timerTrampoline; arg = %p",arg));
    interruptTrampoline(arg);
}

void fivmr_initHardRTJ(fivmr_VM *vm) {
    fivmr_ThreadState *curTS;
    fivmr_Handle *vmt;
    int i;
    
    fivmr_assert(vm->maxPriority==FIVMR_TPR_CRITICAL);
    
    curTS=fivmr_ThreadState_get(vm);
    interruptTS=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA);
    vmt=fivmr_VMThread_create(curTS,
                              FIVMR_TPR_RR_MAX,true /* daemon */);
    fivmr_assertNoException(curTS,"while creating interrupt Java thread");
    fivmr_assert(vmt!=NULL);
    fivmr_ThreadState_glue(interruptTS,vmt);
    fivmr_ThreadState_setInitPrio(interruptTS,FIVMR_TPR_CRITICAL);
    fivmr_ThreadState_setManual(interruptTS,FIVMR_TH_INTERRUPT,NULL);
    
    /* this is the fun part ... preallocate a bunch of handles. */
    for (i=0;i<100;++i) {
	fivmr_Handle *h=fivmr_mallocAssert(sizeof(fivmr_Handle));
	bzero(h,sizeof(fivmr_Handle));
	h->next=interruptTS->freeHandles;
	interruptTS->freeHandles=h;
    }
    fivmr_Lock_lock(&vm->hrLock);
    for (i=0;i<100;++i) {
	fivmr_Handle *h=fivmr_mallocAssert(sizeof(fivmr_Handle));
	bzero(h,sizeof(fivmr_Handle));
	h->next=vm->freeHandles;
	vm->freeHandles=h;
    }
    fivmr_Lock_unlock(&vm->hrLock);
    
    /* and now ... make sure the thread has a buffer */
    interruptTS->buf=fivmr_mallocAssert(FIVMR_TS_BUF_SIZE);
    
    fivmr_preinitThreadStringBufFor(FIVMR_TH_INTERRUPT);
}

void fivmr_RTEMS_withInterruptsDisabled(fivmr_Handle *h) {
    rtems_mode oldMode;
    rtems_interrupt_level oldLevel;
    rtems_mode dummy;
    rtems_status_code status;
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    
    vm=h->vm;

    fivmr_assert(vm->maxPriority==FIVMR_TPR_CRITICAL);

    ts=fivmr_ThreadState_get(vm);
    
    status=rtems_task_mode(RTEMS_NO_PREEMPT|RTEMS_NO_TIMESLICE,
			   RTEMS_PREEMPT_MASK|RTEMS_TIMESLICE_MASK,
			   &oldMode);
    fivmr_assert( status == RTEMS_SUCCESSFUL ); 
    rtems_interrupt_disable(oldLevel); 
    fivmr_ThreadState_disablePollchecks(ts);
    
    fivmr_runRunnable(ts,h);
    
    fivmr_ThreadState_enablePollchecks(ts);
    rtems_interrupt_enable(oldLevel);
    status=rtems_task_mode(oldMode,
			   RTEMS_PREEMPT_MASK|RTEMS_TIMESLICE_MASK,
			   &dummy);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_RTEMS_printk(const char *str) {
    printk("%s",str);
}

int32_t fivmr_RTEMS_timerFireAfter(int32_t timerId,
				   int32_t ticks,
				   fivmr_Handle *h) {
    fivmr_ThreadState *ts;
    int32_t result;
    fivmr_assert(h->vm->maxPriority==FIVMR_TPR_CRITICAL);
    ts=fivmr_ThreadState_get(h->vm);
    fivmr_ThreadState_goToJava(ts);
    h=fivmr_newGlobalHandle(ts,h);
    fivmr_ThreadState_goToNative(ts);
    result=rtems_timer_fire_after(timerId,ticks,timerTrampoline,h);
    if (result!=RTEMS_SUCCESSFUL) {
	fivmr_ThreadState_goToJava(ts);
	fivmr_deleteGlobalHandle(h);
	fivmr_ThreadState_goToNative(ts);
    }
    LOG(2,("timer registered; timerId = %d, ticks = %d, result = %d, h = %p",
	   timerId,ticks,result,h));
    return result;
}

#endif

