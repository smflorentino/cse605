/*
 * fivmr_rtems_thread.c
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

#include "fivmr.h"

fivmr_ThreadHandle fivmr_Thread_integrate(void) {
    /* FIXME: take care of notes!  currently this isn't an issue but it will be in
       the future! */

    return fivmr_ThreadHandle_current();
}

typedef struct {
    void (*threadMain)(void *arg);
    void *arg;
} RunData;

static rtems_task runner(rtems_task_argument arg_){
    RunData *rd;
    void (*threadMain)(void *arg);
    void *arg;
    rtems_interrupt_level cookie;
    rtems_id          sema;
    rtems_status_code status;
    fivmr_ThreadCondNode tcn;
    uintptr_t tss[FIVMR_MAX_THREAD_SPECIFICS];
    
    LOG(1,("In new thread %p",fivmr_ThreadHandle_current()));
    
    bzero(tss,sizeof(tss));
    status=rtems_task_set_note(RTEMS_SELF,
                               FIVMR_TS_NOTE,
                               (uint32_t)(uintptr_t)(&tss[0]));
    fivmr_assert(status==RTEMS_SUCCESSFUL);
    
    rd=(RunData*)arg_;
    
    threadMain=rd->threadMain;
    arg=rd->arg;
    
    fivmr_free(rd);
    
    LOG(2,("creating semaphore..."));
    
    /* initial value of 0 so an obtain blocks on the sema for synchronization
       another thread can then wake this thread up by doing a release */
    status = rtems_semaphore_create(fivmr_xchg_add32(&fivmr_nextSemaName,1),
				    0, RTEMS_COUNTING_SEMAPHORE|RTEMS_PRIORITY,
				    RTEMS_NO_PRIORITY, &sema);
    fivmr_assert( status == RTEMS_SUCCESSFUL );
    
    LOG(1,("Thread %p has semaphore %d",fivmr_ThreadHandle_current(),sema));
    
    tcn.sema=sema;
    tcn.next=NULL;
    tcn.queuedOn=NULL;

    fivmr_ThreadSpecific_set(&fivmr_RTEMS_lockData,(uintptr_t)&tcn);

    threadMain(arg);
    
    fivmr_Thread_exit();
}

fivmr_ThreadHandle fivmr_Thread_spawn(void (*threadMain)(void *arg),
				      void *arg,
				      fivmr_ThreadPriority priority) {
    rtems_id            tid;
    rtems_status_code   status;
    rtems_name          name;
    rtems_mode          rmode = 0;
    rtems_task_priority rprio = 0;
    RunData             *rd;
    size_t stackSize = fivmr_RTEMS_threadStackSize;
    char buf[32];

    if (LOGGING(1)) {
        fivmr_ThreadPriority_describe(priority,buf,sizeof(buf));
        LOG(1, ("Spawning thread with priority %s",buf));
    }
    rd=fivmr_mallocAssert(sizeof(RunData));
    rd->threadMain=threadMain;
    rd->arg=arg;

    /* TODO: fix the priority */
    name=fivmr_xchg_add32(&fivmr_nextThreadName,1);
    LOG(2,("Using thread name = %p",name));
    LOG(2,("Stack size = %p",stackSize));
    if (LOGGING(2)) {
        fivmr_ThreadPriority_describe(priority,buf,sizeof(buf));
        LOG(2,("Starting a thread with priority %s",buf));
    }
    fivmr_ThreadPriority_toRTEMS(priority, &rprio, &rmode);
    LOG(2,("RTEMS priority: %d, %d",rprio,rmode));
    status = rtems_task_create(
	name, rprio, stackSize,
	RTEMS_PREEMPT|rmode, RTEMS_FLOATING_POINT, &tid);
    LOG(2,("tid = %d, status = %d",tid,status));
    
    if (status!=RTEMS_SUCCESSFUL) {
        LOG(0,("Failed to create thread; status = %d",status));
        return fivmr_ThreadHandle_zero();
    }

    status = rtems_task_start( tid, runner, (int) rd);
    fivmr_assert( status == RTEMS_SUCCESSFUL );
    LOG(2, ("Spawning thread complete"));
    
    LOG(2,("returning thread: %p",tid));
    
    return tid;
}

void fivmr_Thread_exit(void) {
    fivmr_ThreadCondNode *tcn;
    uintptr_t *tss;
    rtems_status_code status;
    uint32_t tmp;
    int32_t i;
    
    fivmr_assert(fivmr_ThreadHandle_current()!=FIVMR_TH_INTERRUPT);
    fivmr_assert(fivmr_Thread_canExit());

    tcn=(fivmr_ThreadCondNode*)(void*)fivmr_ThreadSpecific_get(&fivmr_RTEMS_lockData);
    
    status=rtems_task_get_note(RTEMS_SELF,FIVMR_TS_NOTE,&tmp);
    tss=(uintptr_t*)(uintptr_t)tmp;

    fivmr_assert(tcn->queuedOn==NULL);
    fivmr_assert(tcn->next==NULL);
    
    rtems_semaphore_delete(tcn->sema);
    
    status=rtems_semaphore_obtain(fivmr_tsSema,RTEMS_WAIT,RTEMS_NO_TIMEOUT);
    fivmr_assert(status==RTEMS_SUCCESSFUL);

    for (i=0;i<FIVMR_MAX_THREAD_SPECIFICS;++i) {
	void (*dest)(uintptr_t arg)=fivmr_tsData[i].destructor;
	if (dest!=NULL && tss[i]!=0) {
            dest(tss[i]);
	}
    }
    
    status=rtems_semaphore_release(fivmr_tsSema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);

    rtems_task_delete( RTEMS_SELF );
    fivmr_assert(false);
}


fivmr_ThreadPriority fivmr_Thread_getPriority(fivmr_ThreadHandle h) {
    if (h==FIVMR_TH_INTERRUPT || (h==fivmr_ThreadHandle_current() && fivmr_Thread_isCritical())) {
        return FIVMR_TPR_CRITICAL;
    } else {
        rtems_status_code status;
        rtems_task_priority oldPrio;
        rtems_mode oldMode;
        status=rtems_task_set_priority(h,RTEMS_NO_PRIORITY,&oldPrio);
        fivmr_assert(status==RTEMS_SUCCESSFUL);
        if (h==fivmr_ThreadHandle_current()) {
            status=rtems_task_mode(0,0,&oldMode);
        } else {
            /* HACKZILLA!  FIXME!  get the real mode! */
            oldMode=RTEMS_PREEMPT;
        }
        return fivmr_ThreadPriority_fromRTEMS(oldPrio,oldMode);
    }
}

#endif
