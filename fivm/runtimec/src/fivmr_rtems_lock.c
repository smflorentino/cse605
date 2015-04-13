/*
 * fivmr_rtems_lock.c
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

void fivmr_Lock_init(fivmr_Lock *lock,
		     fivmr_Priority priority) {
    /* need to first create the mutex sema in RTEMS */
    /* need to allocate an empty condition variable */
    rtems_id          sema;
    rtems_status_code status;
    LOG(2, ("Initializing lock %p with priority %d",lock,priority));
    rtems_attribute attr=RTEMS_BINARY_SEMAPHORE|RTEMS_PRIORITY;
    rtems_task_priority prio;
    if (priority!=FIVMR_PR_CRITICAL) {
	if (priority==FIVMR_PR_NONE) {
	    prio=RTEMS_NO_PRIORITY;
	} else if (priority==FIVMR_PR_INHERIT) {
	    prio=RTEMS_NO_PRIORITY;
	    attr|=RTEMS_INHERIT_PRIORITY;
	} else {
	    prio=fivmr_Priority_toRTEMS(priority);
	    attr|=RTEMS_PRIORITY_CEILING;
	}
	status = rtems_semaphore_create(
	    fivmr_xchg_add32(&fivmr_nextSemaName,1),
	    1,attr,prio, &sema);
	LOG(2,("Lock has semaphore %d",sema));
	fivmr_assert( status == RTEMS_SUCCESSFUL );
	lock->mutex=sema;
    } else {
	lock->mutex=0;
    }
    lock->cond.head=NULL;
    lock->level=-1;
    lock->mode=-1;
    lock->holder=fivmr_ThreadHandle_zero();
    lock->recCount=0;
    lock->priority=priority;
}

void fivmr_Lock_destroy(fivmr_Lock *lock) {
    rtems_semaphore_delete(lock->mutex);
}

bool fivmr_Lock_legalToAcquire(fivmr_Lock *lock) {
    if (lock->priority==FIVMR_PR_CRITICAL) {
	return true;
    } else {
	return !fivmr_Thread_isCritical();
    }
}

void fivmr_Lock_lock(fivmr_Lock *lock) {
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    rtems_status_code status;
    
    LOG(5, ("Lock lock"));
    
    if (lock->holder==me) {
	
	fivmr_assert(lock->recCount<100);
	
	lock->recCount++;
    } else {
        LOG(5, ("Non-rec lock"));
	if (FIVMR_ASSERTS_ON &&
	    lock->priority!=FIVMR_PR_CRITICAL &&
	    fivmr_Thread_isCritical()) {
	    LOG(0,("error: trying to acquire a non-critical lock with "
		   "interrupts disabled; lock priority = %d, "
		   "ISR level = %d, lock address = %p",
		   lock->priority,_ISR_Get_level(),lock));
	    fivmr_assert(false);
	}
        
	if(lock->priority==FIVMR_PR_CRITICAL) {
	    rtems_interrupt_disable(lock->level);
	} else {
	    status = rtems_semaphore_obtain(lock->mutex, RTEMS_WAIT, RTEMS_NO_TIMEOUT); 
            if (status!=RTEMS_SUCCESSFUL) {
                LOG(0,("Failed to acquire lock."));
                LOG(0,("My priority: %p",fivmr_Thread_getPriority(fivmr_ThreadHandle_current())));
                LOG(0,("Status: %d",status));
                fivmr_abort("failed to acquire lock");
            }
	}
	lock->holder=me;
	lock->recCount=1;
    }
}

void fivmr_Lock_unlock(fivmr_Lock *lock) {
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    rtems_status_code status;
    
    LOG(9, ("Lock unlock"));
    fivmr_assert(lock->recCount>=1);
    fivmr_assert(lock->holder==me);
    if (lock->recCount==1) {
	lock->recCount=0;
	lock->holder=fivmr_ThreadHandle_zero();
	if(lock->priority==FIVMR_PR_CRITICAL) {
	    rtems_interrupt_enable(lock->level); 
	} else {
	    status = rtems_semaphore_release(lock->mutex); 
	    fivmr_assert( status == RTEMS_SUCCESSFUL );
	}
    } else {
	lock->recCount--;
    }
}

void fivmr_Lock_broadcast(fivmr_Lock *lock) {
    fivmr_ThreadCondNode *ptr;
    rtems_status_code status;
    fivmr_assert(!fivmr_Thread_isCritical());
    fivmr_assert(lock->priority!=FIVMR_PR_CRITICAL);
    fivmr_assert(lock->holder==fivmr_ThreadHandle_current());
    fivmr_assert(lock->recCount>=1);
    LOG(2, ("Lock broadcast on %p",lock));
    if (lock->cond.head != NULL){
	LOG(2, ("Lock broadcasting to non-emtpy cond variable"));
	ptr = lock->cond.head;
	while(ptr!=NULL) {
	    fivmr_ThreadCondNode *next=ptr->next;
	    LOG(2, ("notifying %p, ISR level is %d",ptr,_ISR_Get_level()));
	    fivmr_assert(ptr->queuedOn==lock);
	    ptr->queuedOn=NULL;
	    ptr->next=NULL;
	    status = rtems_semaphore_release(ptr->sema);
	    fivmr_assert( status == RTEMS_SUCCESSFUL );
	    ptr=next;
	}
	lock->cond.head = NULL;
    }
    LOG(2, ("done broadcasting on %p.",lock));
}

void fivmr_Lock_lockedBroadcast(fivmr_Lock *lock) {
    LOG(2, ("Lock locked broadcast"));	      
    fivmr_assert(!fivmr_Thread_isCritical());
    fivmr_assert(lock->priority!=FIVMR_PR_CRITICAL);
    fivmr_Lock_lock(lock);
    fivmr_Lock_broadcast(lock); /* why duplicate code? should be the same as above */
    fivmr_Lock_unlock(lock);
}

static void rtems_wait_impl(fivmr_Lock *lock,
                            bool hasTimeout,
                            fivmr_Nanos whenAwake) {
    int32_t savedRecCount;
    rtems_status_code status;
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    fivmr_ThreadCondNode *tln;  
    
    fivmr_assert(!fivmr_Thread_isCritical());
    fivmr_assert(lock->priority!=FIVMR_PR_CRITICAL);

    LOG(2, ("Lock Wait on %p by thread %p", lock, me));
    fivmr_assert(lock->holder==me);
    fivmr_assert(lock->recCount>=1);
    savedRecCount=lock->recCount;
    lock->recCount=0;
    lock->holder=fivmr_ThreadHandle_zero();

    tln=(fivmr_ThreadCondNode*)fivmr_ThreadSpecific_get(&fivmr_RTEMS_lockData);
    LOG(2,("Got tln = %p",tln));
    tln->next=lock->cond.head;
    tln->queuedOn=lock;
    lock->cond.head=tln;

    if(lock->priority==FIVMR_PR_CRITICAL) {
	rtems_mode dummy;
	fivmr_assert(lock->level!=(rtems_interrupt_level)-1); /* we have a previous level */
	rtems_interrupt_enable(lock->level); 
	status=rtems_task_mode(lock->mode,
			       RTEMS_PREEMPT_MASK|RTEMS_TIMESLICE_MASK,
			       &dummy);
	fivmr_assert( status == RTEMS_SUCCESSFUL ); 
    } else {
	status=rtems_semaphore_release(lock->mutex);
	fivmr_assert(status==RTEMS_SUCCESSFUL);
    }
    
    LOG(2,("acquiring semaphore %d",tln->sema));
    
    if (hasTimeout) {
        for (;;) {
            fivmr_Nanos now;
            fivmr_Nanos timeLeft;
            struct timespec ts;
            uint32_t ticks;
            /* FIXME this is so wrong and so likely to cause timing skew.  but oddly enough
               it's exactly the way RTEMS itself implements abs-time timeouts.
               weird-o-rific! */
            now=fivmr_curTime();
            if (whenAwake<=now) {
                break;
            }
            timeLeft=whenAwake-now;
            ts.tv_sec=timeLeft/(1000*1000*1000);
            ts.tv_nsec=timeLeft%(1000*1000*1000);
            ticks=_Timespec_To_ticks(&ts);
            LOG(2,("sleeping for %d ticks",ticks));
	    status = rtems_semaphore_obtain(tln->sema, RTEMS_WAIT, ticks);
	    LOG(2,("semaphore for %p returned = %d",lock,status));
	    fivmr_assert(status==RTEMS_SUCCESSFUL ||
			 status==RTEMS_TIMEOUT);
	    if (status==RTEMS_SUCCESSFUL) {
		break;
	    }
	}
    } else {
	status = rtems_semaphore_obtain(tln->sema, RTEMS_WAIT, RTEMS_NO_TIMEOUT); 
	LOG(2,("semaphore for %p returned = %d",lock,status));
	fivmr_assert( status == RTEMS_SUCCESSFUL ); 
    }
    
    if (lock->priority==FIVMR_PR_CRITICAL) {
	status=rtems_task_mode(RTEMS_NO_PREEMPT|RTEMS_NO_TIMESLICE,
			       RTEMS_PREEMPT_MASK|RTEMS_TIMESLICE_MASK,
			       &lock->mode);
	fivmr_assert( status == RTEMS_SUCCESSFUL ); 
	rtems_interrupt_disable(lock->level);
    } else {
	status=rtems_semaphore_obtain(lock->mutex, RTEMS_WAIT, RTEMS_NO_TIMEOUT);
	fivmr_assert(status==RTEMS_SUCCESSFUL);
    }
    if (tln->queuedOn!=NULL) {
	fivmr_assert(tln->queuedOn==lock);
	fivmr_assert(hasTimeout);
	fivmr_assert(lock->cond.head!=NULL);
	fivmr_assert(tln->next!=tln);
	if (lock->cond.head==tln) {
	    LOG(2,("Setting cond.head for %p to %p",lock,tln->next));
	    lock->cond.head=tln->next;
	} else {
	    fivmr_ThreadCondNode *cur;
	    bool found=false;
	    for (cur=lock->cond.head;cur->next!=NULL;cur=cur->next) {
		if (cur->next==tln) {
		    cur->next=tln->next;
		    break;
		}
	    }
	    fivmr_assert(found);
	}
	tln->next=NULL;
	tln->queuedOn=NULL;
    }
    lock->recCount=savedRecCount;
    lock->holder=me;
}

void fivmr_Lock_wait(fivmr_Lock *lock) {
    rtems_wait_impl(lock,false,0);
}

void fivmr_Lock_timedWaitAbs(fivmr_Lock *lock,
			     fivmr_Nanos whenAwake) {
    rtems_wait_impl(lock,true,whenAwake);
}

void fivmr_Lock_timedWaitRel(fivmr_Lock *lock,
			     fivmr_Nanos howLong) {
    LOG(2, ("Lock timed wait release"));
    fivmr_Lock_timedWaitAbs(lock,fivmr_curTime()+howLong);
}

#endif
