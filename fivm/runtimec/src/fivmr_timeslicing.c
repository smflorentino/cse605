/*
 * fivmr_timeslicing.c
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

void fivmr_TimeSliceManager_init(fivmr_TimeSliceManager *man,
                                 uintptr_t nslices,
                                 fivmr_ThreadPriority managerPriority) {
    uintptr_t i;
    fivmr_assert(man!=NULL);
    fivmr_assert(nslices>0);
    man->managerPriority=managerPriority;
    man->nslices=nslices;
    man->slices=fivmr_mallocAssert(sizeof(fivmr_TimeSlice)*nslices);
    bzero(man->slices,sizeof(fivmr_TimeSlice)*nslices);
    for (i=0;i<nslices;++i) {
        man->slices[i].inited=false;
    }
}

fivmr_TimeSlice *fivmr_TimeSliceManager_initSlice(fivmr_TimeSliceManager *man,
                                                  uintptr_t sliceIndex,
                                                  fivmr_Nanos duration,
                                                  fivmr_ThreadPool *pool) {
    fivmr_TimeSlice *slice;

    fivmr_assert(pool!=NULL);
    fivmr_assert(duration>0);
    fivmr_assert(sliceIndex<man->nslices);
    fivmr_assert(fivmr_ThreadPriority_ltRT(pool->defaultPriority,man->managerPriority));
    
    slice=man->slices+sliceIndex;
    slice->duration=duration;
    slice->pool=pool;
    slice->inited=true;
    
    return slice;
}

fivmr_TimeSlice *fivmr_TimeSliceManager_initSliceEasy(fivmr_TimeSliceManager *man,
                                                      uintptr_t sliceIndex,
                                                      fivmr_Nanos duration,
                                                      uintptr_t nthreads,
                                                      fivmr_ThreadPriority defaultPriority) {
    fivmr_ThreadPool *pool;
    fivmr_assert(duration>0);
    fivmr_assert(fivmr_ThreadPriority_ltRT(defaultPriority,man->managerPriority));
    pool=fivmr_mallocAssert(sizeof(fivmr_ThreadPool));
    fivmr_ThreadPool_init(pool,nthreads,defaultPriority);
    return fivmr_TimeSliceManager_initSlice(man,sliceIndex,duration,pool);
}

bool fivmr_TimeSliceManager_fullyInitialized(fivmr_TimeSliceManager *man) {
    uintptr_t i;
    fivmr_assert(man->nslices>0);
    for (i=0;i<man->nslices;++i) {
        if (!man->slices[i].inited) {
            return false;
        }
        fivmr_assert(man->slices[i].duration>0);
        fivmr_assert(man->slices[i].pool!=NULL);
    }
    return true;
}

static void timeslicemanager_runner(void *arg) {
    /* FIXME: use priority modulation instead! */
    
    fivmr_TimeSliceManager *man;
    uintptr_t i,j;
#if FIVMR_RTEMS
    rtems_id ratemon;
    rtems_status_code stat;
#else
    fivmr_Nanos nextWakeup;
#endif
    fivmr_Lock waitLock;
    
    /* printk("in timeslicemanager_runner\n"); */
        
    man=(fivmr_TimeSliceManager*)arg;
    
#if FIVMR_RTEMS
    stat=rtems_rate_monotonic_create(42,&ratemon);
    fivmr_assert(stat==RTEMS_SUCCESSFUL);
#endif
    
    if (!FIVMR_RTEMS || man->nslices==1) {
        fivmr_Lock_init(&waitLock,FIVMR_PR_INHERIT);
        fivmr_Lock_lock(&waitLock);
    }
    
    if (man->nslices==1) {
        for (;;) fivmr_Lock_wait(&waitLock); /* just wait forever */
    } else {
        fivmr_assert(fivmr_TimeSliceManager_fullyInitialized(man));
        
        for (i=0;i<man->nslices;++i) {
            fivmr_ThreadPool_suspend(fivmr_TimeSliceManager_getPool(man,i));
        }

#if !FIVMR_RTEMS
        nextWakeup=fivmr_curTime()+man->slices[0].duration;
#endif
        for (i=0,j=0;;) {
#if FIVMR_RTEMS
            fivmr_Nanos duration_nanos;
            struct timespec duration_ts;
#endif
#if 0
            printk("resuming at %u\n",(unsigned)fivmr_curTimePrecise());
#endif
            fivmr_ThreadPool_resume(fivmr_TimeSliceManager_getPool(man,i));
#if 0
            printk("resuming done at %u\n",(unsigned)fivmr_curTimePrecise());
#endif
#if FIVMR_RTEMS
            duration_nanos=man->slices[(i+1)%man->nslices].duration;
            duration_ts.tv_sec=duration_nanos/(1000*1000*1000);
            duration_ts.tv_nsec=duration_nanos%(1000*1000*1000);
            stat=rtems_rate_monotonic_period(ratemon,_Timespec_To_ticks(&duration_ts));
#else
            fivmr_Lock_timedWaitAbs(&waitLock,nextWakeup);
            if (false) {
                fivmr_Nanos curTime;
                curTime=fivmr_curTime();
                if (curTime > nextWakeup) {
                    LOG(2,("Time slicer woke up late!  "
                           "Expected wakeup: %u.  Woke up at: %u.  Previous/current slice: %u/%u",
                           nextWakeup,curTime,
                           man->slices[i].duration,
                           man->slices[(i+1)%man->nslices].duration));
                }
            }
#endif
#if 0
            printk("suspending at %u\n",(unsigned)fivmr_curTimePrecise());
#endif
            fivmr_ThreadPool_suspend(fivmr_TimeSliceManager_getPool(man,i));
#if 0
            printk("suspending done at %u\n",(unsigned)fivmr_curTimePrecise());
#endif
            i=(i+1)%man->nslices;
#if !FIVMR_RTEMS
            nextWakeup+=man->slices[i].duration;
#endif
            
#if 0
            if (++j==1000) {
                printk("still slicing!\n");
                j=0;
            }
#endif
        }
    }
}

void fivmr_TimeSliceManager_start(fivmr_TimeSliceManager *man) {
    fivmr_ThreadHandle result;
    fivmr_assert(fivmr_TimeSliceManager_fullyInitialized(man));
    result=fivmr_Thread_spawn(timeslicemanager_runner,man,man->managerPriority);
    fivmr_assert(result!=fivmr_ThreadHandle_zero());
}

