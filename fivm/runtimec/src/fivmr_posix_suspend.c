/*
 * fivmr_posix_suspend.c
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
#if FIVMR_POSIX

#include "fivmr.h"

static fivmr_ThreadSpecific suspThread;

#define MYSIGNAL SIGUSR1

static void sigusr1_handler(int sig) {
    LOG(2,("signal %d received; suspending thread %p.",
           MYSIGNAL,pthread_self()));
    fivmr_SuspensionData *data=
        (fivmr_SuspensionData*)fivmr_ThreadSpecific_get(&suspThread);
    
    fivmr_assert(data->magic==0x1234babe);
    fivmr_assert(data->thread==fivmr_ThreadHandle_current());
    LOG(11,("data address: %p",data));
    LOG(11,("thread according to me: %p",pthread_self()));
    LOG(11,("thread according to data: %p",data->thread));
    LOG(11,("data's magic: %p",data->magic));

    if (false) {
        /* FIXME: priority inversion! */
        fivmr_Semaphore_up(&data->suspended);
        LOG(11,("thread %p notifying of suspension on %p with data %p",
                pthread_self(),&data->suspended,data));
    }
    fivmr_Semaphore_down(&data->wakeUp);
    LOG(2,("Wake-up received; resuming."));
}

static void destructor(uintptr_t arg) {
    fivmr_SuspensionData *data=(fivmr_SuspensionData*)arg;
    fivmr_Semaphore_destroy(&data->wakeUp);
    fivmr_Semaphore_destroy(&data->suspended);
}

void fivmr_initSuspensionManager(void) {
    struct sigaction sa;

    bzero(&sa,sizeof(sa));
    sa.sa_handler=sigusr1_handler;
    sigfillset(&sa.sa_mask);
    sa.sa_flags=SA_SIGINFO;
    sigaction(MYSIGNAL,&sa,NULL);
    
    fivmr_ThreadSpecific_initWithDestructor(&suspThread,
                                            destructor);
    LOG(1,("signal %d suspension handler initialized and thread-specific created.",
           MYSIGNAL));
}

fivmr_SuspendableThreadHandle fivmr_Thread_makeSuspendable(fivmr_SuspensionData *data) {
    int res;
    fivmr_assert(fivmr_ThreadSpecific_get(&suspThread)==0);
    data->magic=0x1234babe;
    data->thread=fivmr_ThreadHandle_current();
    fivmr_Semaphore_init(&data->wakeUp);
    fivmr_Semaphore_init(&data->suspended);
    fivmr_ThreadSpecific_set(&suspThread,(uintptr_t)data);
    LOG(11,("setting thread specific in %p to %p",
           fivmr_ThreadHandle_current(),
           data));
    LOG(11,("have set thread specific in %p to %p",
           fivmr_ThreadHandle_current(),
           fivmr_ThreadSpecific_get(&suspThread)));
    return data;
}

void fivmr_Thread_suspend(fivmr_SuspendableThreadHandle th) {
    pthread_kill(th->thread,MYSIGNAL);
    /* OK - suspension in progress */
    if (false) {
        LOG(11,("waiting for suspension to happen on %p with data %p for %p",&th->suspended,th,th->thread));
        fivmr_Semaphore_down(&th->suspended);
        LOG(11,("OK, suspended"));
    }
}

void fivmr_Thread_resume(fivmr_SuspendableThreadHandle th) {
    
    fivmr_Semaphore_up(&th->wakeUp);
}

#endif
