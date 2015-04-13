/*
 * fivmr_semaphore.c
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
#include "fivmr.h"

#if FIVMR_POSIX
#  ifdef HAVE_WORKING_SEM_INIT
void fivmr_CritSemaphore_init(fivmr_CritSemaphore *sema) {
    int res;
    res=sem_init(&sema->sema,0,0);
    fivmr_assert(res==0);
}

void fivmr_CritSemaphore_up(fivmr_CritSemaphore *sema) {
    int res;
    res=sem_post(&sema->sema);
    fivmr_assert(res==0);
}

void fivmr_CritSemaphore_down(fivmr_CritSemaphore *sema) {
    int res;
    do {
        res=sem_wait(&sema->sema);
    } while (res!=0 && errno==EINTR);
    fivmr_assert(res==0);
}

void fivmr_CritSemaphore_destroy(fivmr_CritSemaphore *sema) {
    int res;
    res=sem_destroy(&sema->sema);
    fivmr_assert(res==0);
}
#  else
void fivmr_CritSemaphore_init(fivmr_CritSemaphore *sema) {
    int res;
    long oldflags;
    res=pipe(sema->pipe);
    fivmr_assert(res==0);
    res=fcntl(sema->pipe[1],F_GETFL);
    fivmr_assert(res!=-1);
    oldflags=(long)res;
    res=fcntl(sema->pipe[1],F_SETFL,oldflags|O_NONBLOCK);
    fivmr_assert(res==0);
}

void fivmr_CritSemaphore_up(fivmr_CritSemaphore *sema) {
    int res;
    char c;
    res=write(sema->pipe[1],&c,1);
    /* NB we joyfully accept cases where the semaphore "overflows" because the
       pipe's buffer got full. */
    fivmr_assert(res==1 || (res==-1 && (errno==EAGAIN || errno==EWOULDBLOCK)));
}

void fivmr_CritSemaphore_down(fivmr_CritSemaphore *sema) {
    int res;
    char c;
    do {
        res=read(sema->pipe[0],&c,1);
    } while (res!=0 && errno==EINTR);
    fivmr_assert(res==1);
}

void fivmr_CritSemaphore_destroy(fivmr_CritSemaphore *sema) {
    int i;
    for (i=0;i<2;++i) {
        int res;
        res=close(sema->pipe[i]);
        fivmr_assert(res==0);
    }
}
#  endif
#elif FIVMR_RTEMS
void fivmr_CritSemaphore_init(fivmr_CritSemaphore *sema) {
    rtems_status_code status;
    status=rtems_semaphore_create(
        fivmr_xchg_add32(&fivmr_nextSemaName,1),
        0,
        RTEMS_COUNTING_SEMAPHORE|RTEMS_PRIORITY,
        RTEMS_NO_PRIORITY,
        &sema->sema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_CritSemaphore_up(fivmr_CritSemaphore *sema) {
    rtems_status_code status;
    status=rtems_semaphore_release(sema->sema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_CritSemaphore_down(fivmr_CritSemaphore *sema) {
    rtems_status_code status;
    fivmr_assert(!fivmr_Thread_isCritical());
    /* printk("obtaining semaphore %p\n",sema); */
    status=rtems_semaphore_obtain(sema->sema,RTEMS_WAIT,RTEMS_NO_TIMEOUT);
    /* printk("got semaphore %p\n",sema); */
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_CritSemaphore_destroy(fivmr_CritSemaphore *sema) {
    rtems_status_code status;
    status=rtems_semaphore_delete(sema->sema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}
#endif

#if FIVMR_POSIX && !defined(HAVE_WORKING_SEM_INIT)
void fivmr_Semaphore_init(fivmr_Semaphore *sema) {
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
#ifdef HAVE_PTHREAD_PIP
    pthread_mutexattr_setprotocol(&attr,PTHREAD_PRIO_INHERIT);
#endif
    pthread_mutex_init(&sema->mutex,&attr);
    pthread_cond_init(&sema->cond,NULL);
}

void fivmr_Semaphore_up(fivmr_Semaphore *sema) {
    pthread_mutex_lock(&sema->mutex);
    sema->count++;
    pthread_cond_broadcast(&sema->cond); /* use broadcast instead of signal out
                                            of an abundance of caution */
    pthread_mutex_unlock(&sema->mutex);
}

void fivmr_Semaphore_down(fivmr_Semaphore *sema) {
    pthread_mutex_lock(&sema->mutex);
    while (sema->count==0) {
        pthread_cond_wait(&sema->cond,&sema->mutex);
    }
    sema->count--;
    pthread_mutex_unlock(&sema->mutex);
}

void fivmr_Semaphore_destroy(fivmr_Semaphore *sema) {
    pthread_mutex_destroy(&sema->mutex);
    pthread_cond_destroy(&sema->cond);
}
#endif
