/*
 * fivmr_posix_lock.c
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

/*
 * What to do:
 * - we can use pthread_mutex/cond if we like.
 * - these should be prio-ceiling locks.
 * - the problem is with interrupts.  We should have the ability to
 *   create a lock with the ceiling being the "interrupt priority".
 *   when such a lock is acquired, interrupts are disabled.
 * - waiting on an interrupt-priority lock ... how do we do it?
 *   for sure, it only make sense to wait if we're not servicing an
 *   interrupt.
 *
 * What about this for interrupt priority locks:
 * - if servicing an interrupt, lock just disables interrupts and then
 *   we disallow calls to wait().
 * - if not servicing an interrupt, first acquire a lock (with highest
 *   priority allowed by RTEMS) and then disable interrupts.  wait()
 *   causes us to reenable interrupts and release the lock.
 * - does notify() from an interrupt work?  probably not...  but
 *   semaphore_release works.  hmmm...
 *
 * It seems that having a unified approach where all threads have a
 * semaphore for notification would be good.
 */

void fivmr_Lock_init(fivmr_Lock *lock,
		     fivmr_Priority priority) {
    pthread_mutexattr_t attr;
    int res, realprio;
    realprio=priority;
    res=pthread_mutexattr_init(&attr);
    fivmr_assert(res==0);
    if (FIVMR_PR_SUPPORTED) {
#ifdef HAVE_PTHREAD_PIP
        if (priority==FIVMR_PR_INHERIT) {
            LOG(10,("creating priority inheritance lock"));
            res=pthread_mutexattr_setprotocol(&attr,PTHREAD_PRIO_INHERIT);
            fivmr_assert(res==0);
        }
#endif
#ifdef HAVE_PTHREAD_PCEP
        if (priority!=FIVMR_PR_INHERIT && priority!=FIVMR_PR_NONE) {
            LOG(10,("creating priority ceiling lock"));
            fivmr_assert(priority>=FIVMR_PR_MIN);
            fivmr_assert(priority<=FIVMR_PR_CRITICAL);
            if (priority==FIVMR_PR_CRITICAL) {
                LOG(10,("critical lock requested, using max priority for now"));
                realprio=FIVMR_PR_MAX;
            }
            res=pthread_mutexattr_setprotocol(&attr,PTHREAD_PRIO_PROTECT);
            fivmr_assert(res==0);
            res=pthread_mutexattr_setprioceiling(&attr,realprio);
            fivmr_assert(res==0);
        }
#endif
    }
    res=pthread_mutex_init(&(lock->mutex),NULL);
    fivmr_assert(res==0);
    pthread_cond_init(&(lock->cond),NULL);
    lock->holder=fivmr_ThreadHandle_zero();
    lock->recCount=0;
    lock->priority=realprio;
}

void fivmr_Lock_destroy(fivmr_Lock *lock) {
    pthread_mutex_destroy(&lock->mutex);
    pthread_cond_destroy(&lock->cond);
}

bool fivmr_Lock_legalToAcquire(fivmr_Lock *lock) {
    fivmr_assert(!fivmr_Thread_isCritical());
    return true;
}

void fivmr_Lock_lock(fivmr_Lock *lock) {
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    if (lock->holder==me) {
	/* sanity check to eliminate the possibility of infinite lock recursion
	   because of a missing unlock statement. */
	fivmr_assert(lock->recCount<100);
	
	lock->recCount++;
    } else {
	fivmr_assert(!fivmr_Thread_isCritical());
	
	pthread_mutex_lock(&(lock->mutex));
	lock->holder=me;
	lock->recCount=1;
    }
}

void fivmr_Lock_unlock(fivmr_Lock *lock) {
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    fivmr_assert(lock->recCount>=1);
    fivmr_assert(lock->holder==me);
    if (lock->recCount==1) {
	lock->recCount=0;
	lock->holder=fivmr_ThreadHandle_zero();
	pthread_mutex_unlock(&(lock->mutex));
    } else {
	lock->recCount--;
    }
}

void fivmr_Lock_broadcast(fivmr_Lock *lock) {
    pthread_cond_broadcast(&(lock->cond));
}

void fivmr_Lock_lockedBroadcast(fivmr_Lock *lock) {
    fivmr_Lock_lock(lock);
    pthread_cond_broadcast(&(lock->cond));
    fivmr_Lock_unlock(lock);
}

void fivmr_Lock_wait(fivmr_Lock *lock) {
    int32_t savedRecCount;
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();

    fivmr_assert(lock->holder==me);
    fivmr_assert(lock->recCount>=1);

    fivmr_assert(!fivmr_Thread_isCritical());

    savedRecCount=lock->recCount;

    lock->recCount=0;
    lock->holder=fivmr_ThreadHandle_zero();

    pthread_cond_wait(&(lock->cond),&(lock->mutex));
    
    lock->recCount=savedRecCount;
    lock->holder=me;
}

void fivmr_Lock_timedWaitAbs(fivmr_Lock *lock,
			     fivmr_Nanos whenAwake) {
    struct timespec ts;
    int savedRecCount;
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();

    fivmr_assert(lock->holder==me);
    fivmr_assert(lock->recCount>=1);

    fivmr_assert(!fivmr_Thread_isCritical());

    ts.tv_sec = (time_t)(whenAwake/1000000000LL);
    ts.tv_nsec = (long)(whenAwake%1000000000LL);

    savedRecCount=lock->recCount;

    lock->recCount=0;
    lock->holder=fivmr_ThreadHandle_zero();

    pthread_cond_timedwait(&(lock->cond),&(lock->mutex),&ts);

    lock->recCount=savedRecCount;
    lock->holder=me;
}

void fivmr_Lock_timedWaitRel(fivmr_Lock *lock,
			     fivmr_Nanos howLong) {
    fivmr_Lock_timedWaitAbs(lock,fivmr_curTime()+howLong);
}

#endif
