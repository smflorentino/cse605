/*
 * fivmr_threadqueue.c
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

void fivmr_ThreadQueue_init(fivmr_ThreadQueue *queue) {
    queue->head=NULL;
    queue->tail=NULL;
    queue->flags=0;
    fivmr_BoostedSpinLock_init(&queue->lock);
}

static void verifyQueue(fivmr_ThreadQueue *queue) {
    int cnt=0;
    fivmr_ThreadState *cur;
    for (cur=queue->head;
         cur!=NULL;
         cur=cur->forMonitor.next) {
        cnt++;
        if (cur->forMonitor.next==NULL) {
            fivmr_assert(queue->tail==cur);
        }
    }
    if (cnt==0) {
        fivmr_assert(queue->head==NULL);
        fivmr_assert(queue->tail==NULL);
    }
    if (cnt==1) {
        fivmr_assert(queue->head==queue->tail);
    }
    if (cnt==2) {
        fivmr_assert(queue->head->forMonitor.next==queue->tail);
    }
}

static void enqueue(fivmr_ThreadQueue *queue,
                    fivmr_ThreadState *ts) {
    fivmr_ThreadPriority myprio;
    bool inserted=false;
    
    fivmr_assert(ts->forMonitor.next==NULL);

    myprio=ts->curPrio;
    
    if (fivmr_ThreadPriority_isRT(myprio)) {
        fivmr_ThreadState **cur;
        for (cur=&queue->head;
             (*cur)!=NULL;
             cur=&(*cur)->forMonitor.next) {
            if (fivmr_ThreadPriority_gtRT(
                    myprio,
                    (*cur)->curPrio)) {
                ts->forMonitor.next=*cur;
                *cur=ts;
                inserted=true;
                break;
            }
        }
    }
    
    if (!inserted) {
        if (queue->head==NULL) {
            queue->head=ts;
            queue->tail=ts;
        } else {
            queue->tail->forMonitor.next=ts;
            queue->tail=ts;
        }
    }
}

void fivmr_ThreadQueue_enqueue(fivmr_ThreadQueue *queue,
                               fivmr_ThreadState *ts) {
    fivmr_assert(ts->forMonitor.queuedOnReal==NULL);
    fivmr_assert(ts->forMonitor.queuedOnIntended==NULL);
    
    enqueue(queue,ts);
    
    ts->forMonitor.queuedOnReal=queue;
    ts->forMonitor.queuedOnIntended=queue;
}

fivmr_ThreadState *fivmr_ThreadQueue_dequeue(fivmr_ThreadQueue *queue) {
    fivmr_ThreadState *result;
    fivmr_ThreadState *next;
    
    result=queue->head;
    fivmr_assert(result!=NULL);
    next=result->forMonitor.next;
    if (next==NULL) {
	queue->head=NULL;
	queue->tail=NULL;
    } else {
	queue->head=next;
	result->forMonitor.next=NULL;
    }
    
    fivmr_assert(result->forMonitor.queuedOnReal==queue);
    fivmr_assert(result->forMonitor.queuedOnIntended==queue);
    result->forMonitor.queuedOnReal=NULL;
    
    return result;
}

static bool removeFromQueue(fivmr_ThreadQueue *queue,
                            fivmr_ThreadState *ts) {
    bool result=false;
    fivmr_ThreadState *last=NULL;
    fivmr_ThreadState **cur;
    for (cur=&queue->head;
         (*cur)!=NULL;
         cur=&((*cur)->forMonitor.next)) {
        if ((*cur)==ts) {
            (*cur)=ts->forMonitor.next;
            ts->forMonitor.next=NULL;
            result=true;
            break;
        } else {
            last=*cur;
        }
    }
    if (result && queue->tail==ts) {
        queue->tail=last;
    }
    if (false && FIVMR_ASSERTS_ON) verifyQueue(queue);
    return result;
}

bool fivmr_ThreadQueue_remove(fivmr_ThreadQueue *queue,
                              fivmr_ThreadState *ts) {
    if (fivmr_ThreadQueue_isQueued(queue,ts)) {
        bool result=removeFromQueue(queue,ts);
        fivmr_assert(result);
        ts->forMonitor.queuedOnReal=NULL;
        return result;
    } else {
	return false;
    }
}

void fivmr_ThreadQueue_eueuqne(fivmr_ThreadQueue *queue,
                               fivmr_ThreadState *ts) {
    fivmr_assert(ts->forMonitor.queuedOnIntended==queue);
    ts->forMonitor.queuedOnIntended=NULL;
}

void fivmr_ThreadQueue_poke(fivmr_ThreadQueue *queue,
                            fivmr_ThreadState *ts) {
    bool result;
    if (FIVMR_ASSERTS_ON) {
        fivmr_ThreadQueue *qor=ts->forMonitor.queuedOnReal;
        fivmr_assert(qor==queue || qor==NULL);
    }
    fivmr_assert(ts->forMonitor.queuedOnIntended==queue);
    
    if (ts->forMonitor.queuedOnReal==queue) {
        result=removeFromQueue(queue,ts);
        fivmr_assert(result);
        enqueue(queue,ts);
        
        if (false && FIVMR_ASSERTS_ON) verifyQueue(queue);
    } /* else we *just* got dequeued */
    
    if (FIVMR_ASSERTS_ON) {
        fivmr_ThreadQueue *qor=ts->forMonitor.queuedOnReal;
        fivmr_assert(qor==queue || qor==NULL);
    }
    fivmr_assert(ts->forMonitor.queuedOnIntended==queue);
}

