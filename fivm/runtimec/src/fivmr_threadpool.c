/*
 * fivmr_threadpool.c
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

static void pool_runner(void *arg) {
    fivmr_PooledThread *pt;
    volatile fivmr_SuspensionData sp;

    pt=(fivmr_PooledThread*)arg;
    pt->susp=fivmr_Thread_makeSuspendable((fivmr_SuspensionData*)&sp);
    LOG(1,("in thread %p: pointer to sp = %p, pt->susp = %p",
           fivmr_ThreadHandle_current(),&sp,pt->susp));
    
    /* make damn sure that nobody calls fivmr_Thread_exit */
    fivmr_Thread_disableExit();
    
    for (;;) {
        fivmr_Lock_lock(&pt->pool->lock);
        fivmr_assert(pt->pool->nfree < pt->pool->nthreads);
        pt->active=false;
        pt->runner=NULL;
        pt->arg=NULL;
        pt->activePriority=FIVMR_TPR_MIN;
        pt->pool->freeIndices[pt->pool->nfree]=pt->index;
        pt->pool->nfree++;
        fivmr_Lock_broadcast(&pt->pool->lock);
        while (!pt->active) {
            fivmr_Lock_wait(&pt->pool->lock);
        }
        fivmr_Lock_unlock(&pt->pool->lock);
        
        fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                                 pt->activePriority);
        
        pt->runner(pt->arg);
        
        fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                                 pt->pool->defaultPriority);
    }
}

void fivmr_ThreadPool_init(fivmr_ThreadPool *pool,
                           uintptr_t nthreads,
                           fivmr_ThreadPriority defaultPriority) {
    uintptr_t i;
    bzero(pool,sizeof(fivmr_ThreadPool));
    fivmr_Lock_init(&pool->lock,FIVMR_PR_MAX);
    pool->defaultPriority=defaultPriority;
    pool->nthreads=nthreads;
    pool->threads=fivmr_mallocAssert(sizeof(fivmr_PooledThread)*pool->nthreads);
    bzero(pool->threads,sizeof(fivmr_PooledThread)*pool->nthreads);
    for (i=0;i<nthreads;++i) {
        pool->threads[i].pool=pool;
        pool->threads[i].index=i;
        pool->threads[i].active=false;
        pool->threads[i].arg=NULL;
        pool->threads[i].runner=NULL;
    }
    pool->nfree=0;
    pool->freeIndices=fivmr_mallocAssert(sizeof(uintptr_t)*nthreads);
    bzero(pool->freeIndices,sizeof(uintptr_t)*nthreads);
    for (i=0;i<nthreads;++i) {
        pool->threads[i].thread=
            fivmr_Thread_spawn(pool_runner,pool->threads+i,pool->defaultPriority);
        fivmr_assert(pool->threads[i].thread!=fivmr_ThreadHandle_zero());
    }
    
    /* wait for the pool to become fully active */
    fivmr_Lock_lock(&pool->lock);
    while (pool->nfree<pool->nthreads) {
        fivmr_Lock_wait(&pool->lock);
    }
    fivmr_Lock_unlock(&pool->lock);
}

fivmr_PooledThread *fivmr_ThreadPool_spawn(fivmr_ThreadPool *pool,
                                           void (*runner)(void *arg),
                                           void *arg,
                                           fivmr_ThreadPriority activePriority) {
    fivmr_PooledThread *pt;
    fivmr_assert(pool!=NULL);
    fivmr_assert(fivmr_ThreadPriority_leRT(activePriority,pool->defaultPriority));
    fivmr_assert(activePriority!=0);
    fivmr_Lock_lock(&pool->lock);
    while (pool->nfree==0) {
        fivmr_Lock_wait(&pool->lock);
    }
    pt=pool->threads+pool->freeIndices[--pool->nfree];
    pt->active=true;
    pt->runner=runner;
    pt->arg=arg;
    pt->activePriority=activePriority;
    fivmr_Lock_broadcast(&pool->lock);
    fivmr_Lock_unlock(&pool->lock);
    return pt;
}

void fivmr_PooledThread_suspend(fivmr_PooledThread *pt) {
    fivmr_Thread_suspend(pt->susp);
}

void fivmr_PooledThread_resume(fivmr_PooledThread *pt) {
    fivmr_Thread_resume(pt->susp);
}

void fivmr_ThreadPool_suspend(fivmr_ThreadPool *pool) {
    uintptr_t i;
    for (i=0;i<pool->nthreads;++i) {
        fivmr_PooledThread_suspend(pool->threads+i);
    }
    LOG(2,("Sent %d suspend signals",pool->nthreads));
}

void fivmr_ThreadPool_resume(fivmr_ThreadPool *pool) {
    uintptr_t i;
    for (i=0;i<pool->nthreads;++i) {
        fivmr_PooledThread_resume(pool->threads+i);
    }
    LOG(2,("Sent %d resume signals",pool->nthreads));
}


