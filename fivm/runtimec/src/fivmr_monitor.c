/*
 * fivmr_monitor.c
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

void fivmr_MonState_describe(fivmr_MonState state,
                             char *buf,
                             size_t bufsize) {
    snprintf(buf,bufsize,
             "%s%s%sT%uR%u",
             fivmr_MonState_queued(state)?"Qu":"",
             fivmr_MonState_rtQueued(state)?"Rq":"",
             fivmr_MonState_biased(state)?"Bi":"",
             fivmr_MonState_thread(state),
             fivmr_MonState_realRC(state));
}

void fivmr_Monitor_setStateBit(fivmr_Monitor *monitor,
                               fivmr_MonState bit,
                               bool value) {
    for (;;) {
        fivmr_MonState oldState,newState;
        oldState=monitor->state;
        if (value) {
            newState=oldState|bit;
        } else {
            newState=oldState&~bit;
        }
        if (fivmr_cas_weak(&monitor->state,
                           oldState,
                           newState)) {
            return;
        }
        fivmr_spin_fast();
    }
}

void fivmr_Monitor_pokeRTQueued(fivmr_Monitor *monitor) {
    if (monitor->queues!=NULL) {
        fivmr_ThreadState *ts=fivmr_ThreadQueue_peek(&monitor->queues->entering);
        fivmr_Monitor_setStateBit(
            monitor,
            FIVMR_MS_RT_QUEUED,
            ts!=NULL && fivmr_ThreadPriority_isRT(ts->curPrio));
    }
}

bool fivmr_Monitor_ensureQueues(fivmr_ThreadState *ts,
                                fivmr_Monitor *monitor,
                                fivmr_ObjHeader *head) {
    if (monitor->queues==NULL) {
        fivmr_MonQueues *queues;
        
        if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
            fivmr_MemoryArea *curarea=ts->gc.currentArea;
            fivmr_MemoryArea *objarea=fivmr_MemoryArea_forObject(
                ts,fivmr_ObjHeader_toObject(&ts->vm->settings,head));
            if (curarea!=objarea) {
                fivmr_MemoryArea_setCurrentArea(ts,objarea);
            }
            queues=fivmr_allocRawType(ts,fivmr_MonQueues);
            if (curarea!=objarea) {
                fivmr_MemoryArea_setCurrentArea(ts,curarea);
            }
        } else {
            queues=fivmr_allocRawType(ts,fivmr_MonQueues);
        }
        if (ts->curException) {
            return false;
        }
	fivmr_ThreadQueue_init(&queues->entering);
	fivmr_ThreadQueue_init(&queues->waiting);
        fivmr_fence(); /* just to be sure */
        fivmr_cas_void((uintptr_t*)&monitor->queues,
                       (uintptr_t)NULL,
                       (uintptr_t)queues);
        fivmr_assert(monitor->queues!=NULL);
    }
    fivmr_fence();
    return true;
}

void fivmr_Monitor_lock_slow(fivmr_ObjHeader *head,
			     fivmr_ThreadState *ts) {
    int32_t cnt;
    fivmr_Monitor *curMonitor;
    fivmr_MonState state;
    
    fivmr_SPC_incLockSlow();
    
    LOG(4,("Thread %u: locking slow %p",ts->id,head));
    
    curMonitor=fivmr_ObjHeader_getMonitor(&ts->vm->settings,head);

    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_BEGIN,
                      (uintptr_t)head);

    state=curMonitor->state;
    
    /* this comes first because we'd really like it to be on the fast path but
       we couldn't fit it in... */
    if (!fivmr_MonState_biased(state) &&
        fivmr_MonState_thread(state)==ts->id) {
        
        fivmr_SPC_incLockSlowRecurse();
        for (;;) {
            fivmr_MonState newState;
            newState=fivmr_MonState_incRC(state);
            fivmr_assert(fivmr_MonState_queued(state)
                         ==fivmr_MonState_queued(newState));
            
            /* it isn't necessary to do this check every time, but it shouldn't hurt,
               plus it makes the code nicer to structure. */
            if (fivmr_MonState_rc(newState)==1 /* rec count overflows to 1 because
                                                  0 is 1 */) {
                fivmr_throwIllegalMonitorStateException_inJava(
                    ts,"Recursion count overflow");
                return;
            }
            
            if (fivmr_cas_weak(&curMonitor->state,
                               state,newState)) {
                fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                                  FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_END,
                                  (uintptr_t)head);
                return;
            }
            
            /* reload the lock state since we failed */
            state=curMonitor->state;

            fivmr_spin_fast();
        }
    }
    
    if (state==FIVMR_MS_INVALID) {
        /* we have a dummy monitor (i.e. TypeData), need to inflate. */
        fivmr_SPC_incLockSlowInflate();
        curMonitor=fivmr_Monitor_inflate(head,ts);
        if (curMonitor==NULL) {
            return; /* fail! */
        }
        /* if successful, try to acquire the lock.  this may either be a
           biased acquisition (if biased locking is enabled) or a fast
           lock acquisition (if it's disabled). */
        state=curMonitor->state;
    }
    
    if (fivmr_MonState_biased(state)) {
        if (fivmr_MonState_thread(state)==ts->id) {
            fivmr_MonState newState;
            fivmr_SPC_incLockSlowRecurse();
            newState=fivmr_MonState_incRC(state);
            fivmr_assert(fivmr_MonState_queued(state)
                         ==fivmr_MonState_queued(newState));
            if (fivmr_MonState_rc(newState)==0) {
                fivmr_throwIllegalMonitorStateException_inJava(
                    ts,"Recursion count overflow");
                return;
            }
            fivmr_assert_cas(&curMonitor->state,
                             state,newState);
            fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                              FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_END,
                              (uintptr_t)head);
            return;
        } else /* oh noes!  biased but not to us! */ {
            fivmr_Nanos before;
            
            before=fivmr_curTime();
                
            do {
                bool wasRunning;
                fivmr_ThreadState *otherTS;
                
                fivmr_assert(fivmr_MonState_thread(state)!=0);
                fivmr_assert(fivmr_MonState_thread(state)!=1);
                
                /* figure out which thread owns the lock and pair handshake with them. */
                otherTS=fivmr_ThreadState_byId(ts->vm,fivmr_MonState_thread(state));
                fivmr_ThreadState_goToNative(ts);
                
                fivmr_Lock_lock(&otherTS->lock);
                if (fivmr_ThreadState_isRunning(otherTS)) {
                    otherTS->toUnbias=fivmr_ObjHeader_toObject(&ts->vm->settings,
                                                               head);
                    
                    /* there is a race here - while we wait, another thread can
                       also attempt to perform a soft handshake on the same target -
                       this is why we have that stupid loop.  really it's kind of
                       a bug in how softPairHandshake works, kind of.  except it
                       appears to be totally harmless. */
                    fivmr_ThreadState_softPairHandshake(otherTS,FIVMR_TSEF_UNBIAS);
                    
                    fivmr_assert(otherTS->toUnbias==0 || ts->vm->exiting);
                } else {
                    /* masquerade as the other thread since it ain't running */
                    fivmr_Monitor_unbiasIfBiasedToMe(curMonitor,otherTS);
                    fivmr_assert(!fivmr_MonState_biased(curMonitor->state));
                }
                fivmr_Lock_unlock(&otherTS->lock);
                
                fivmr_ThreadState_checkExit(ts);
                
                fivmr_ThreadState_goToJava(ts);
            } while (fivmr_MonState_biased(curMonitor->state));
            
            LOG(3,("Unbiasing took %" PRIu64 " ns",fivmr_curTime()-before));
            
            /* now we should be able to acquire, or at least
               contend on, the lock. */
        }
    }
    
    /* this assertion serves two purposes:
       1) if biased locking is disabled, this asserts that the lock cannot be
          biased.
       2) if the lock was biased to someone else, this asserts that we
          succeeded in unbiasing it. */
    if (FIVMR_ASSERTS_ON &&
        fivmr_MonState_biased(curMonitor->state)) {
        LOG(0,("encountered a biased lock unexpectedly.  "
               "old state = %p, cur state = %p",
               state,curMonitor->state));
        fivmr_assert(false);
    }
    
    for (cnt=0;;cnt++) {
        int spinMode=FIVMR_SPIN_FAST;
        state=curMonitor->state;
            
        LOG(5,("Thread %u trying to lock %p (%p), state = %" PRIuPTR ".",
               ts->id,head,curMonitor,state));
            
        fivmr_assert(state!=FIVMR_MS_INVALID);
        fivmr_assert(!fivmr_MonState_biased(state));
        fivmr_assert(fivmr_MonState_thread(state)!=ts->id);
	
        if (fivmr_MonState_available(state)) {
            uintptr_t newState;
            /* try to grab the lock if nobody holds it.  note that this will
               barge in even if the lock's queue is currently being mucked
               with. */
            fivmr_SPC_incLockSlowNotHeld();
            newState=fivmr_MonState_withThread(state,ts->id);
            fivmr_assert(fivmr_MonState_queued(state)
                         ==fivmr_MonState_queued(newState));
            if (fivmr_cas_weak(&curMonitor->state,
                               state,newState)) {
                break;
            }
        } else if (cnt<ts->vm->monitorSpinLimit &&
                   !fivmr_MonState_mustQueue(state) &&
                   !fivmr_ThreadPriority_isRT(ts->curPrio)) {
                
            /* spin around a few times before using heavy locking, but yield the
               thread instead of just using pause. */
            fivmr_SPC_incLockSlowSpin();
            spinMode=FIVMR_SPIN_SLOW;
        } else {
            fivmr_ThreadQueue *queue;
            fivmr_ThreadPriority prio=ts->curPrio;
            bool needToEnqueue = false; /* make compiler happy (real value is set
                                           somewhere below) */
            
            fivmr_SPC_incLockSlowQueue();
            if (!fivmr_Monitor_ensureQueues(ts,curMonitor,head)) {
                /* exception! */
                return;
            }
            
            fivmr_assert(curMonitor->queues!=NULL);
            fivmr_assert(ts->curException==0);
            
            queue=&curMonitor->queues->entering;
            
            fivmr_BoostedSpinLock_lock(ts,&queue->lock);
            
            /* need to indicate that someone is queueing, but also check that the lock
               didn't become available in the process. */
            for (;;) {
                state=curMonitor->state; /* reread the state as it may have changed in
                                            some relevant way */
                    
                if (fivmr_MonState_available(state)) {
                    needToEnqueue=false;
                    break;
                }
                if (fivmr_MonState_queued(state) &&
                    (!fivmr_ThreadPriority_isRT(prio) ||
                     fivmr_MonState_rtQueued(state))) {
                    /* cool - queued is already set, no need to do anything */
                    needToEnqueue=true;
                    break;
                }
                if (fivmr_cas(&curMonitor->state,
                              state,
                              fivmr_MonState_withQueued(
                                  fivmr_MonState_withRTQueued(
                                      state,
                                      (fivmr_MonState_rtQueued(state)|
                                       fivmr_ThreadPriority_isRT(prio))),
                                  true))) {
                    needToEnqueue=true;
                    break;
                }
                fivmr_spin_fast();
            }
            
            if (needToEnqueue) {
                bool syncHandoff;

                fivmr_assert(fivmr_MonState_queued(curMonitor->state));
                
                fivmr_ThreadQueue_enqueue(queue,ts);
                fivmr_assert(fivmr_ThreadState_isOnAQueue(ts));
                fivmr_assert(fivmr_Monitor_queuedShouldBeSet(curMonitor));
                fivmr_assert(fivmr_MonState_queued(curMonitor->state));
                ts->forMonitor.entering=curMonitor;
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
                
                /* we are now queued.  wait to get signaled.  note that we're guaranteed
                   to get signaled, since at the time that we set the QUEUED bit the lock
                   was unavailable. */
                    
                /* ok so this call is very magical.  it will reevaluate our priority
                   based on the fact that we're queued on and entering this lock.  under
                   PIP locking this will lead to other threads' priorities getting
                   boosted. */
                LOG(3,("going to native slow"));
                fivmr_ThreadState_goToNative_slow(ts);
                    
                /* I think we can just down the waiter.  but I'm unsure.  there are
                   some subtle issues at play here. */
                fivmr_Semaphore_down(&ts->waiter);
                
                /* it may have been downed because exit was signaled - so check if
                   we should exit.  in that case checkExit will not return. */
                fivmr_ThreadState_checkExit(ts);
                
                fivmr_assert(!fivmr_ThreadState_isOnAQueue(ts));

                fivmr_ThreadState_goToJava(ts);
                    
                syncHandoff=ts->forMonitor.syncHandoffCookie;
                ts->forMonitor.syncHandoffCookie=false;

                fivmr_BoostedSpinLock_lock(ts,&queue->lock);
                ts->forMonitor.entering=NULL;
                fivmr_ThreadQueue_eueuqne(queue,ts);
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);

                /* check if this was a synchronous handoff */
                if (syncHandoff) {
                    LOG(2,("Received a synchronous monitor handoff in Thread #%u "
                           "on monitor %p",
                           ts->id,curMonitor));
                    state=curMonitor->state;
                    fivmr_assert(fivmr_MonState_thread(state)==ts->id);
                    fivmr_assert(fivmr_MonState_rc(state)==1);
                    break; /* done - the lock is ours */
                }

                /* ok - our turn! */
                spinMode=FIVMR_SPIN_NONE;
            } else {
                /* false alarm!  just loop around and recontend on the lock. */
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
            }
            fivmr_assert(!fivmr_Thread_isCritical());
        }
	
        fivmr_spin(spinMode);
    }
    
    /* ensure that everyone knows that this thread holds this lock.  however note that
       it is possible for that "notification" to have already happened for example due to a
       synchronous handoff.  so we do it conditionally.  I think that's safe. */
    if (FIVMR_PIP_LOCKING(&ts->vm->settings) && ts->forMonitor.holding!=curMonitor) {
        curMonitor->next=ts->forMonitor.holding;
        ts->forMonitor.holding=curMonitor;
    }
    
    LOG(11,("Thread %u: returning from lock slow %p (%p) with state = %p, curException = %p.",
	    ts->id,head,fivmr_ObjHeader_getMonitor(&ts->vm->settings,head),
	    fivmr_ObjHeader_getMonitor(&ts->vm->settings,head)->state,ts->curException));
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_END,
                      (uintptr_t)head);
}

void fivmr_Monitor_unlockInflated(fivmr_Monitor *curMonitor,
				  fivmr_ThreadState *ts) {
    fivmr_MonState state;
    
    /* note that during an attempt to unlock a thin lock it may get inflated. */
    
    /* some other stuff to worry about:
     * - cannot unlock while the lock is in the queueing state, because we may
     *   then miss an enqueued thread that we should have dequeued and signaled.
     * - unlocking in the queued state requires more work.
     * - the queues may disappear during GC.  this shouldn't be a problem here,
     *   but still, need to be careful. */

    fivmr_SPC_incUnlockSlow();
    
    state=curMonitor->state;

    /* error checking */
    if (fivmr_MonState_thread(state)!=ts->id ||
	fivmr_MonState_realRC(state)==0) {
	fivmr_throwIllegalMonitorStateException_inJava(
	    ts,"Thread not holding the lock at monitorexit");
        return;
    }
    
    fivmr_Monitor_assertInflated(curMonitor);
    
    if (fivmr_MonState_biased(state)) {
        fivmr_MonState newState;
        newState=fivmr_MonState_decRC(state);
        fivmr_assert_cas(&curMonitor->state,
                         state,newState);
    } else if (fivmr_MonState_rc(state)>1) {
        for (;;) {
            state=curMonitor->state;
            if (fivmr_cas_weak(&curMonitor->state,
                               state,
                               fivmr_MonState_decRC(state))) {
                LOG(11,("Thread %u: unlocked %p by decrementing (state = %p)",
                        ts->id,curMonitor,state));
                break;
            }
            fivmr_spin_fast();
        }
    } else {
        bool syncHandoff=fivmr_MonState_rtQueued(state);
        bool haveDequeued=false;
        
        fivmr_assert(fivmr_MonState_rc(state)==1);

        /* two possibilities: either the fast path has already removed this lock from
           the holding list or it hasn't.  if not we remove it now. */
        if (FIVMR_PIP_LOCKING(&ts->vm->settings) && ts->forMonitor.holding==curMonitor) {
            ts->forMonitor.holding=curMonitor->next;
            curMonitor->next=NULL;
        }
        
        /* we will do a syncHandoff if there is a high-priority thread waiting */
        
        if (!syncHandoff) {
            for (;;) {
                state=curMonitor->state;
                
                if (fivmr_cas_weak(&curMonitor->state,
                                   state,
                                   fivmr_MonState_withThread(state,0))) {
                    break;
                }
                fivmr_spin_fast();
            }
        }

        /* lock is now released - but it's possible that there is someone
           queued. so we have to let them know that it's time to go. */
                
        /* reread the state of the lock; it might have changed in some
           relevant way since the cas */
        state=curMonitor->state;
                
        if (fivmr_MonState_queued(state)) {
            /* someone is queued.  that's unfortunate. ;-)  acquire the lock
               for the queue, then: if there's someone on the queue, release
               the lock and signal them; if there's not someone on the queue
               then we're racing - so try again */
                    
            fivmr_ThreadQueue *queue;
            fivmr_ThreadState *toAwaken;
            fivmr_ThreadState *toAwaken2;
                    
            /* if someone is queued then there better be some queues for
               them to have been queued on! */
            fivmr_assert(curMonitor->queues!=NULL);
                    
            queue=&curMonitor->queues->entering;
                    
            fivmr_BoostedSpinLock_lock(ts,&queue->lock);
                    
            /* reread the state of the lock, as it could have changed in some
               relevant way between when we spun on the queue lock and now.
               in particular there's a massively convoluted corner case where
               this thread had acquired the lock in a barging fashion while
               some other thread was performing a dequeue.  now this thread
               is releasing the lock that it had barged in on - and only now
               does it realize that the lock had its queued bit set.  this
               crucially relies on the dequeuing thread being descheduled for
               the entire duration of this thread's critical section - but
               this is quite likely on a uniprocessor.  now, we end up trying
               to acquire the spinlock on the queue, which the other thread
               still holds.  this serves as the trigger to get that thread
               scheduled again; once scheduled, that thread will complete
               the dequeue and reset the queued bit.  now we get scheduled,
               we finish acquiring the spinlock, and we have to be smart to
               realize that the queued bit is no longer set. */
            state=curMonitor->state;
                    
            /* we hold the queue lock.  that means that there should be
               coherence between the queued bit and the actual state of the
               queues.  assert that coherence. */
            fivmr_assert(fivmr_MonState_queued(state)==
                         !fivmr_ThreadQueue_empty(queue));
                    
            /* check if the lock is still queued - see rant about convoluted
               race conditions above. */
            if (fivmr_MonState_queued(state)) {
                /* ok - someone is queued on the lock.  figure out who it
                   is. */
                toAwaken=fivmr_ThreadQueue_peek(queue);
                fivmr_assert(fivmr_ThreadState_isValid(toAwaken));
                fivmr_assert(fivmr_ThreadState_isOnAQueue(toAwaken));
                
                if (syncHandoff) {
                    LOG(2,("Performing a synchronous monitor handoff from Thread #%u to "
                           "Thread #%u on monitor %p",
                           ts->id,toAwaken->id,curMonitor));
                    for (;;) {
                        state=curMonitor->state;
                        
                        fivmr_assert(fivmr_MonState_thread(state)==ts->id);
                        fivmr_assert(fivmr_MonState_rc(state)==1);
                        
                        if (fivmr_cas_weak(&curMonitor->state,
                                           state,
                                           fivmr_MonState_withThread(
                                               state,
                                               toAwaken->id))) {
                            break;
                        }
                        fivmr_spin_fast();
                    }
                    toAwaken->forMonitor.syncHandoffCookie=true;
                }
                
                toAwaken2=fivmr_ThreadQueue_dequeue(queue);
                fivmr_assert(toAwaken==toAwaken2);
                fivmr_assert(!fivmr_ThreadState_isOnAQueue(toAwaken));
                        
                /* we know who to unblock.  now make sure that the quued bit
                   is reset, if the queue is now otherwise empty. */
                if (fivmr_ThreadQueue_empty(queue)) {
                    fivmr_Monitor_setStateBit(curMonitor,
                                              FIVMR_MS_QUEUED,
                                              false);
                    fivmr_assert(!fivmr_MonState_queued(curMonitor->state));
                    fivmr_assert(fivmr_ThreadQueue_empty(queue));
                }

                fivmr_Monitor_pokeRTQueued(curMonitor);
                        
                fivmr_assert(fivmr_MonState_queued(curMonitor->state)==
                             !fivmr_ThreadQueue_empty(queue));
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
                fivmr_assert(!fivmr_Thread_isCritical());
                        
                /* ok - we've done our due diligence on the lock.  now we just
                   notify the relevant thread and return. */
                fivmr_Semaphore_up(&toAwaken->waiter);
                
                haveDequeued=true;
                
            } else {
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
            }
        }
        
        if (syncHandoff && !haveDequeued) {
            for (;;) {
                state=curMonitor->state;
                
                fivmr_assert(fivmr_MonState_thread(state)==ts->id);
                
                if (fivmr_cas_weak(&curMonitor->state,
                                   state,
                                   fivmr_MonState_withThread(state,0))) {
                    break;
                }
                fivmr_spin_fast();
            }
        }
                
        fivmr_assert(!fivmr_Thread_isCritical());
    }
    
    /* evaluate our priority as it may have changed due to us releasing a lock that was
       boosted by PIP. */
    fivmr_ThreadState_evalPrio(ts);
    
    LOG(5,("Thread %u: returning from unlock slow %p with state = %p, curException = %p.",
	   ts->id,curMonitor,curMonitor->state,ts->curException));
}

void fivmr_Monitor_unlock_slow(fivmr_ObjHeader *head,
			       fivmr_ThreadState *ts) {
    LOG(11,("Thread %u: unlocking slow %p",ts->id,head));
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_UNLOCK_SLOW_BEGIN, (uintptr_t)head);

    fivmr_Monitor_unlockInflated(
        fivmr_ObjHeader_getMonitor(&ts->vm->settings,head),
        ts);

    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_UNLOCK_SLOW_END, (uintptr_t)head);
}

fivmr_Monitor *fivmr_Monitor_inflate(fivmr_ObjHeader *head,
				     fivmr_ThreadState *ts) {
    LOG(3,("Inflating monitor %p for object of type %s (%p) in Thread %u.",
	   head,fivmr_ObjHeader_getMonitor(&ts->vm->settings,head)->forward->name,
	   fivmr_ObjHeader_getMonitor(&ts->vm->settings,head)->forward,ts->id));

    fivmr_SPC_incInflate();
    
    for (;;) {
	fivmr_Monitor *curMonitor=
            fivmr_ObjHeader_getMonitor(&ts->vm->settings,head);
	
	if (curMonitor->state==FIVMR_MS_INVALID) {
	    fivmr_Monitor *newMonitor;
	    uintptr_t state;
	    
	    fivmr_assert(curMonitor==(void*)curMonitor->forward);

            if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
                fivmr_MemoryArea *curarea=ts->gc.currentArea;
                fivmr_MemoryArea *objarea=fivmr_MemoryArea_forObject(
                    ts,fivmr_ObjHeader_toObject(&ts->vm->settings,head));
                if (curarea!=objarea) {
                    /* If the object is stack allocated, we can only inflate
                     * its monitor if we're still in the same frame. */
                    if ((uintptr_t)objarea==fivmr_MemoryArea_getStackArea(ts)) {
                        fivmr_GCHeader *hdr=
                            fivmr_GCHeader_fromObject(
                                &ts->vm->settings,
                                fivmr_ObjHeader_toObject(&ts->vm->settings,head));
                        if (fivmr_GCHeader_frame(hdr)!=ts->allocFrame) {
                            fivmr_throwIllegalMonitorStateException_inJava(
				ts,"Attempted to inflate monitor on stack-allocated object from another Frame");
                            return NULL;
                        }
                    } else {
                        fivmr_MemoryArea_setCurrentArea(ts,objarea);
                    }
                }
                newMonitor=fivmr_allocRawType(ts,fivmr_Monitor);
                if (curarea!=objarea) {
                    fivmr_MemoryArea_setCurrentArea(ts,curarea);
                }
            } else {
                newMonitor=fivmr_allocRawType(ts,fivmr_Monitor);
            }
	    
	    LOG(5,("Thread %u: monitor at = %p",ts->id,newMonitor));
	    
	    if (ts->curException) {
                return NULL;
            }
	    state=FIVMR_MS_NOT_HELD;
	    if (FIVMR_BIASED_LOCKING(&ts->vm->settings)) {
		LOG(6,("(1) Thread %u inflating %p to %p with state = %" PRIuPTR,
		       ts->id,head,newMonitor,state));
		state=fivmr_MonState_withBiased(state,true);
		LOG(6,("(2) Thread %u inflating %p to %p with state = %" PRIuPTR,
		       ts->id,head,newMonitor,state));
		state=fivmr_MonState_withThread(state,ts->id);
		LOG(6,("(3) Thread %u inflating %p to %p with state = %" PRIuPTR,
		       ts->id,head,newMonitor,state));
	    }
	    LOG(6,("(4) Thread %u inflating %p to %p with state = %" PRIuPTR,
		   ts->id,head,newMonitor,state));
	    newMonitor->state=state;
	    newMonitor->forward=(fivmr_TypeData*)curMonitor;
	    
	    LOG(6,("Thread %u: monitor->forward = %p",ts->id,newMonitor->forward));
	    
	    newMonitor->queues=NULL;

	    /* use strong CAS so that we don't waste an allocation due to
	       spurious weak CAS failures. */
	    if (fivmr_ObjHeader_cas(&ts->vm->settings,
                                    head,
				    curMonitor,
				    newMonitor)) {
		LOG(3,("For monitor %p, returning new %p in Thread %u.",
		       head,newMonitor,ts->id));
		return newMonitor;
	    }
	} else {
	    LOG(3,("For monitor %p, returning existing %p in Thread %u.",
		   head,curMonitor,ts->id));
	    return curMonitor;
	}
    }
}

fivmr_ThreadState *fivmr_Monitor_curHolder(fivmr_VM *vm,
                                           fivmr_Monitor *monitor) {
    fivmr_MonState monState=monitor->state;
    if (fivmr_MonState_realRC(monState)==0) {
        return NULL;
    } else {
        uint32_t thrId=fivmr_MonState_thread(monState);
        fivmr_assert(thrId>=2);
        return fivmr_ThreadState_byId(vm,thrId);
    }
}

int32_t fivmr_Monitor_rc(fivmr_Monitor *monitor) {
    return (int32_t)fivmr_MonState_realRC(monitor->state);
}

fivmr_ThreadState *fivmr_Object_curHolder(fivmr_VM *vm,
                                          fivmr_Object obj) {
    return fivmr_Monitor_curHolder(
        vm,
        fivmr_ObjHeader_getMonitor(
            &vm->settings,
            fivmr_ObjHeader_forObject(
                &vm->settings,
                obj)));
}

int32_t fivmr_Object_recCount(fivmr_VM *vm,
                              fivmr_Object obj) {
    return fivmr_Monitor_rc(
        fivmr_ObjHeader_getMonitor(
            &vm->settings,
            fivmr_ObjHeader_forObject(
                &vm->settings,
                obj)));
}

uint32_t fivmr_Monitor_unlockCompletely(fivmr_Monitor *monitor,
					fivmr_ThreadState *ts) {
    uint32_t recCount;
    
    fivmr_Monitor_assertInflated(monitor);
    
    recCount=fivmr_MonState_rc(monitor->state);
    for (;;) {
        fivmr_MonState state=monitor->state;
        if (fivmr_cas_weak(&monitor->state,
                           state,
                           fivmr_MonState_withRC(state,1))) {
            break;
        }
        fivmr_spin_fast();
    }
    fivmr_Monitor_unlockInflated(monitor,ts);
    return recCount;
}

void fivmr_Monitor_relock(fivmr_ObjHeader *head,
			  fivmr_ThreadState *ts,
			  uint32_t recCount) {
    fivmr_Monitor *monitor;
    fivmr_Monitor_lock_slow(head,ts);
    monitor=fivmr_ObjHeader_getMonitor(&ts->vm->settings,head);
    fivmr_Monitor_assertInflated(monitor);
    
    for (;;) {
        fivmr_MonState state=monitor->state;
        if (fivmr_cas_weak(&monitor->state,
                           state,
                           fivmr_MonState_withRC(state,recCount))) {
            break;
        }
        fivmr_spin_fast();
    }
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_RELOCK, (uintptr_t)head);
}

void fivmr_Monitor_unbiasWhenHeld(fivmr_Monitor *monitor,
				  fivmr_ThreadState *ts) {
    fivmr_MonState state;
    
    fivmr_Monitor_assertInflated(monitor);

    /* cannot use setStateBit because MonState_withBiased has special magic in
       it. */
    for (;;) {
        fivmr_MonState state=monitor->state;
        
        fivmr_assert(fivmr_MonState_realRC(state)!=0);
        fivmr_assert(fivmr_MonState_thread(state)==ts->id);

        if (fivmr_cas_weak(&monitor->state,
                           state,
                           fivmr_MonState_withBiased(state,
                                                     false))) {
            break;
        }
        fivmr_spin_fast();
    }
}

void fivmr_Monitor_unbiasIfBiasedToMe(fivmr_Monitor *monitor,
                                      fivmr_ThreadState *ts) {
    fivmr_MonState state;
    
    state=monitor->state;
    
    if (state==FIVMR_MS_INVALID) {
        return;
    }
    
    if (!fivmr_MonState_biased(state)) {
        return;
    }
    
    fivmr_assert(fivmr_MonState_thread(state)==ts->id);

    state=monitor->state;
    fivmr_assert_cas(&monitor->state,
		     state,
		     fivmr_MonState_withBiased(state,false));
}

void fivmr_Monitor_unbiasFromHandshake(fivmr_ThreadState *ts) {
    if (ts->toUnbias!=0) {
        fivmr_Monitor_unbiasIfBiasedToMe(
            fivmr_ObjHeader_getMonitor(
                &ts->vm->settings,
                fivmr_ObjHeader_forObject(&ts->vm->settings,
                                          ts->toUnbias)),
            ts);
        ts->toUnbias=0;
    }
    ts->execFlags&=~FIVMR_TSEF_UNBIAS;
}

void fivmr_Monitor_unbias(fivmr_ObjHeader *head) {
    fivmr_abort("haven't implemented fivmr_Monitor_unbias yet, even though it's "
                "like the easiest thing to implement.");
}

static void wait_impl(fivmr_ObjHeader *head,
		      fivmr_ThreadState *ts,
		      bool hasTimeout,
		      fivmr_Nanos whenAwake) {
    uint32_t recCount;
    fivmr_Monitor *monitor;
    fivmr_MonState state;

    monitor=fivmr_ObjHeader_getMonitor(&ts->vm->settings,head);
    
    fivmr_Monitor_unbiasWhenHeld(monitor,ts);
    
    fivmr_Lock_lock(&ts->lock);
    ts->execFlags|=FIVMR_TSEF_WAITING;
    if (hasTimeout) {
	ts->execFlags|=FIVMR_TSEF_TIMED;
    }
    fivmr_Lock_unlock(&ts->lock);
    
    if (!fivmr_Monitor_ensureQueues(ts,monitor,head)) return;

    fivmr_BoostedSpinLock_lock(ts,&monitor->queues->waiting.lock);
    fivmr_ThreadQueue_enqueue(&monitor->queues->waiting,ts);
    fivmr_BoostedSpinLock_unlock(ts,&monitor->queues->waiting.lock);

    fivmr_assert(!fivmr_Thread_isCritical());
    
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_UNLOCK_COMPLETE,
                      (uintptr_t)head);
    recCount=fivmr_Monitor_unlockCompletely(monitor,ts);
    
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_WAIT, (uintptr_t)head);

    fivmr_ThreadState_goToNative(ts);
    fivmr_Lock_lock(&ts->lock);
    while (!ts->interrupted &&
	   (!hasTimeout || fivmr_curTime()<whenAwake) &&
	   fivmr_ThreadState_isOnAQueue(ts)) {
        /* FIXME use the semaphore */
	if (hasTimeout) {
	    fivmr_Lock_timedWaitAbs(&ts->lock,whenAwake);
	} else {
	    fivmr_Lock_wait(&ts->lock);
	}
        fivmr_ThreadState_checkExit(ts);
    }
    fivmr_Lock_unlock(&ts->lock);
    fivmr_ThreadState_goToJava(ts);
    
    fivmr_BoostedSpinLock_lock(ts,&monitor->queues->waiting.lock);
    if (fivmr_ThreadState_isOnAQueue(ts)) {
        fivmr_ThreadQueue_remove(&monitor->queues->waiting,ts);
        fivmr_assert(!fivmr_Thread_isCritical());
    }

    fivmr_ThreadQueue_eueuqne(&monitor->queues->waiting,ts);
    fivmr_BoostedSpinLock_unlock(ts,&monitor->queues->waiting.lock);
    
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_WAKE, (uintptr_t)head);

    fivmr_Monitor_relock(head,ts,recCount);
    
    fivmr_Lock_lock(&ts->lock);
    ts->execFlags&=~(FIVMR_TSEF_WAITING|FIVMR_TSEF_TIMED);
    fivmr_Lock_unlock(&ts->lock);
}

void fivmr_Monitor_wait(fivmr_ObjHeader *head,
			fivmr_ThreadState *ts) {
    LOG(3,("Thread %u: waiting on %p",ts->id,head));
    wait_impl(head,ts,false,0);
}

void fivmr_Monitor_timedWait(fivmr_ObjHeader *head,
			     fivmr_ThreadState *ts,
			     fivmr_Nanos whenAwake) {
    LOG(3,("Thread %u: timed waiting on %p",ts->id,head));
    wait_impl(head,ts,true,whenAwake);
}

bool fivmr_Monitor_notify(fivmr_ThreadState *ts,
                          fivmr_Monitor *monitor) {
    fivmr_ThreadState *toAwaken=NULL;
    
    LOG(3,("Thread %u: notifying on %p",ts->id,monitor));

    fivmr_Monitor_unbiasWhenHeld(monitor,ts);
    
    if (monitor->queues!=NULL) {
        fivmr_BoostedSpinLock_lock(ts,
                                   &monitor->queues->waiting.lock);
        
        if (!fivmr_ThreadQueue_empty(&monitor->queues->waiting)) {
            toAwaken=fivmr_ThreadQueue_dequeue(&monitor->queues->waiting);
        }
        
        fivmr_BoostedSpinLock_unlock(ts,
                                     &monitor->queues->waiting.lock);
        
        fivmr_assert(!fivmr_Thread_isCritical());
    }
    
    if (toAwaken!=NULL) {
	fivmr_Lock_lockedBroadcast(&toAwaken->lock);
	return true;
    } else {
	return false;
    }
}

bool fivmr_Monitor_notifyAll(fivmr_ThreadState *ts,
                             fivmr_Monitor *monitor) {
    bool result=false;
    LOG(3,("Thread %u: notifying all on %p",ts->id,monitor));
    while (fivmr_Monitor_notify(ts,monitor)) result=true;
    return result;
}

bool fivmr_Object_lock(fivmr_ThreadState *ts,
                       fivmr_Object obj) {
    fivmr_Monitor_lock_slow(fivmr_ObjHeader_forObject(&ts->vm->settings,obj),ts);
    return ts->curException==0;
}

bool fivmr_Object_unlock(fivmr_ThreadState *ts,
                         fivmr_Object obj) {
    fivmr_Monitor_unlock_slow(fivmr_ObjHeader_forObject(&ts->vm->settings,obj),ts);
    return ts->curException==0;
}




