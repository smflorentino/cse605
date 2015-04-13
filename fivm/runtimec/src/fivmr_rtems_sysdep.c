/*
 * fivmr_rtems_sysdep.c
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
#include <stdarg.h>
#include <rtems.h>
#include <rtems/malloc.h>


fivmr_ThreadSpecific fivmr_RTEMS_lockData;
int32_t fivmr_nextSemaName;
int32_t fivmr_nextThreadName;
int32_t fivmr_nextThreadSpecific;
void (*fivmr_threadSpecificDestructor[16])(uintptr_t arg);
uint32_t fivmr_interruptNotepad[16];

/* FIXME: these should not be global variables if their values
   are always constant.  it's a waste of memory. */
fivmr_Priority FIVMR_PR_NONE;
fivmr_Priority FIVMR_PR_INHERIT;
fivmr_Priority FIVMR_PR_MIN;
fivmr_Priority FIVMR_PR_MAX;
fivmr_Priority FIVMR_PR_CRITICAL;
fivmr_ThreadPriority FIVMR_TPR_NORMAL_MIN;
fivmr_ThreadPriority FIVMR_TPR_NORMAL_MAX;
fivmr_ThreadPriority FIVMR_TPR_FIFO_MIN;
fivmr_ThreadPriority FIVMR_TPR_FIFO_MAX;
fivmr_ThreadPriority FIVMR_TPR_RR_MIN;
fivmr_ThreadPriority FIVMR_TPR_RR_MAX;

rtems_interrupt_level fivmr_interruptISRLevel;

fivmr_ThreadSpecificGlobalData fivmr_tsData[FIVMR_MAX_THREAD_SPECIFICS];
int32_t fivmr_tsFree[FIVMR_MAX_THREAD_SPECIFICS];
int32_t fivmr_numTSFree;
rtems_id fivmr_tsSema;

uintptr_t fivmr_RTEMS_threadStackSize=65536;

void fivmr_SysDep_init(void) {
    rtems_interrupt_level cookie;
    int32_t i;
    rtems_status_code status;

    rtems_interrupt_disable(cookie);
    fivmr_interruptISRLevel=_ISR_Get_level();
    rtems_interrupt_enable(cookie);
    
#if FIVMR_ASSERTS_ON
    fivmr_assert(!fivmr_Thread_isCritical());
    rtems_interrupt_disable(cookie);
    fivmr_assert(fivmr_Thread_isCritical());
    rtems_interrupt_enable(cookie);
    fivmr_assert(!fivmr_Thread_isCritical());
#endif

#if FIVMR_DYN_LOADING
#error "Dynamic Loading Not Supported By RTEMS"
#endif
    FIVMR_PR_NONE = (fivmr_Priority) 1;
    FIVMR_PR_INHERIT = (fivmr_Priority) 2;
    FIVMR_PR_MIN = (fivmr_Priority) 3;
    FIVMR_PR_MAX = (fivmr_Priority) 256;
    FIVMR_PR_CRITICAL = (fivmr_Priority) 257;

    FIVMR_TPR_NORMAL_MIN = FIVMR_THREADPRIORITY(FIVMR_TPR_NORMAL, 1);
    FIVMR_TPR_NORMAL_MAX = FIVMR_THREADPRIORITY(FIVMR_TPR_NORMAL, 1);
    FIVMR_TPR_FIFO_MIN = FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO, 2);
    FIVMR_TPR_FIFO_MAX = FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO, 253);
    FIVMR_TPR_RR_MIN = FIVMR_THREADPRIORITY(FIVMR_TPR_RR, 2);
    FIVMR_TPR_RR_MAX = FIVMR_THREADPRIORITY(FIVMR_TPR_RR, 253);

    fivmr_assert(FIVMR_PR_MIN>=0);
    fivmr_assert(FIVMR_PR_MAX>=0);
    fivmr_assert(FIVMR_PR_CRITICAL>=0);
    fivmr_assert(FIVMR_PAGE_SIZE>=getpagesize());
    fivmr_assert((FIVMR_PAGE_SIZE%getpagesize())==0);
    
    fivmr_nextSemaName = rtems_build_name('s', 'e', 'm', 'a');
    fivmr_nextThreadName = rtems_build_name('f', 'i', 'v', 'm');
    
    status = rtems_semaphore_create(
        fivmr_xchg_add32(&fivmr_nextSemaName,1),
        1,
        RTEMS_BINARY_SEMAPHORE|RTEMS_PRIORITY|RTEMS_PRIORITY_CEILING,
        0 /* max priority */,
        &fivmr_tsSema);
    fivmr_assert( status == RTEMS_SUCCESSFUL );
    
    LOG(2,("Initialized the semaphore at %p",fivmr_tsSema));

    fivmr_numTSFree=FIVMR_MAX_THREAD_SPECIFICS;
    for (i=0;i<FIVMR_MAX_THREAD_SPECIFICS;++i) {
        fivmr_tsFree[i]=i;
        fivmr_tsData[i].destructor=NULL;
        fivmr_tsData[i].interruptValue=0;
    }

    fivmr_SysDepUtil_init();

    fivmr_ThreadSpecific_init(&fivmr_RTEMS_lockData);
}

static void abort_impl(void) {
    /* CPU_print_stack(); */
    printk("fivmr dying in thread %p\n",fivmr_ThreadHandle_current());
    if (false) {
	printk("attempting to dumb Java stack...\n");
	fivmr_ThreadState_dumpStack();
    }
    abort();
    exit(1);
}

uintptr_t fivmr_abortf(const char *fmt,...) {
    va_list lst;
    va_start(lst,fmt);
    printk("fivmr ABORT: ");
    vprintk(fmt,lst);
    printk("\n");
    va_end(lst);
    abort_impl();
    return 0;
}

uintptr_t fivmr_abort(const char *message) {
    if (message==NULL) {
	message=
	    "(message was NULL; typically implies Java exception "
	    "during attempt to abort)";
    }
    printk("fivmr ABORT: %s\n",message);
    abort_impl();
    return 0;
}

void fivmr_yield(void) {
    /* no easy way to yield in RTEMS */
    rtems_task_wake_after(RTEMS_YIELD_PROCESSOR);
}

void *fivmr_tryAllocPages_IMPL(uintptr_t numPages,
                               bool *isZero,
                               const char *whereFile,
                               int whereLine) {
    uintptr_t result,mallocResult;
    LOG(2,("fivmr_allocPages(%p) called from %s:%d.",numPages,whereFile,whereLine));
    mallocResult = (uintptr_t)malloc((numPages+1)<<FIVMSYS_LOG_PAGE_SIZE);
    LOG(2,("malloc returned %p",result));
    if (mallocResult==0) {
        LOG(2,("malloc() failed in fivmr_tryAllocPages() for %p pages at %s:%d.\n",
               numPages,whereFile,whereLine));
        return NULL;
    }
    result=(mallocResult+FIVMR_PAGE_SIZE)&~(FIVMR_PAGE_SIZE-1);
    ((uintptr_t*)result)[-1]=mallocResult;

    if (isZero==NULL) {
        bzero((void *)result,FIVMR_PAGE_SIZE*numPages);
    } else {
        *isZero=false;
    }
    
    LOG(2,("fivmr_allocPages(%p) returning %p - %p",
	   numPages,result,result+(numPages<<FIVMSYS_LOG_PAGE_SIZE)-1));

    return (void *)result;
}

void fivmr_freePages_IMPL(void *begin,uintptr_t numPages,
                          const char *whereFile,
                          int whereLine) {
    uintptr_t origAddr;
    LOG(2,("fivmr_freePages(%p,%p) called from %s:%d.",begin,numPages,whereFile,whereLine));
    origAddr=((uintptr_t*)begin)[-1];
    free((void*)origAddr);
}

int32_t fivmr_readByte(int fd) {
    uint8_t result;
    int res;
    res=read(fd,&result,1);
    if (res<0) {
	fivmr_assert(res==-1);
	return -2;
    } else if (res==0) {
	return -1;
    } else {
	return (int32_t)(uint32_t)result;
    }
}

int32_t fivmr_writeByte(int fd,int8_t b) {
    return write(fd,&b,1);
}

void fivmr_ThreadSpecific_initWithDestructor(fivmr_ThreadSpecific *ts,
                                             void (*dest)(uintptr_t value)) {
    rtems_status_code status;
    
    LOG(2,("Acquiring the semaphore at %p",fivmr_tsSema));
    status=rtems_semaphore_obtain(fivmr_tsSema,RTEMS_WAIT,RTEMS_NO_TIMEOUT);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
    
    fivmr_assert(fivmr_numTSFree>0);
    *ts=fivmr_tsFree[--fivmr_numTSFree];
    
    fivmr_tsData[*ts].destructor=dest;
    fivmr_tsData[*ts].interruptValue=0;
    
    status=rtems_semaphore_release(fivmr_tsSema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_ThreadSpecific_destroy(fivmr_ThreadSpecific *ts) {
    rtems_status_code status;
    
    status=rtems_semaphore_obtain(fivmr_tsSema,RTEMS_WAIT,RTEMS_NO_TIMEOUT);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
    
    fivmr_assert(fivmr_numTSFree<FIVMR_MAX_THREAD_SPECIFICS);
    fivmr_tsData[*ts].destructor=NULL;
    fivmr_tsData[*ts].interruptValue=0;
    fivmr_tsFree[fivmr_numTSFree++]=*ts;
    
    status=rtems_semaphore_release(fivmr_tsSema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_ThreadSpecific_set(fivmr_ThreadSpecific *ts,
                              uintptr_t value) {
    if (fivmr_ThreadHandle_current()==FIVMR_TH_INTERRUPT) {
        fivmr_tsData[*ts].interruptValue=value;
    } else {
        rtems_status_code status;
        uint32_t result;
        uintptr_t *tss;
        status=rtems_task_get_note(RTEMS_SELF,FIVMR_TS_NOTE,&result);
        fivmr_assert(status==RTEMS_SUCCESSFUL);
        tss=(uintptr_t*)(uintptr_t)result;
        tss[*ts]=value;
    }
}

void fivmr_ThreadSpecific_setForThread(fivmr_ThreadSpecific *ts,
                                       fivmr_ThreadHandle th,
                                       uintptr_t value) {
    if (th==FIVMR_TH_INTERRUPT) {
        fivmr_tsData[*ts].interruptValue=value;
    } else if (th==fivmr_ThreadHandle_current()) {
        fivmr_ThreadSpecific_set(ts,value);
    } else {
        fivmr_assert(!"don't know how to set thread specific for any random thread");
    }
}

uintptr_t fivmr_ThreadSpecific_get(fivmr_ThreadSpecific *ts) {
    if (fivmr_ThreadHandle_current()==FIVMR_TH_INTERRUPT) {
        return fivmr_tsData[*ts].interruptValue;
    } else {
        rtems_status_code status;
        uint32_t result;
        uintptr_t *tss;
        status=rtems_task_get_note(RTEMS_SELF,FIVMR_TS_NOTE,&result);
        fivmr_assert(status==RTEMS_SUCCESSFUL);
        tss=(uintptr_t*)(uintptr_t)result;
        return tss[*ts];
    }
}

void fivmr_debugMemory_IMPL(const char *flnm,
                            int line) {
    rtems_malloc_statistics_t stats;
    malloc_get_statistics(&stats);
    uint32_t m=stats.malloc_calls;
    uint32_t f=stats.free_calls;
    printk("(%s:%d) malloc calls: %lu\n",flnm,line,m);
    printk("(%s:%d) free calls: %lu\n",flnm,line,f);
    printk("(%s:%d) balance: %lu\n",flnm,line,m-f);
}

#endif
