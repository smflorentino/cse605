/*
 * fivmr_posix_sysdep.c
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
#include <stdarg.h>

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
bool FIVMR_PR_SUPPORTED;

bool fivmr_fakeRTPriorities;

static int sysPageSize;

uintptr_t fivmr_POSIX_threadStackSize;

void fivmr_SysDep_init(void) {
    pthread_attr_t tattr;
    size_t size=0;
    
    LOG(1,("VM instance at pid = %d",getpid()));
    
    fivmr_SysDepUtil_init();
    
    pthread_attr_init(&tattr);
    pthread_attr_getstacksize(&tattr,&size);
    fivmr_assert(size!=0);
    fivmr_POSIX_threadStackSize=(uintptr_t)size;
    
    /* OSX defines _POSIX_THREAD_PRIORITY_SCHEDULING, but does not
       several functions which this should indicate.  However, it does
       give us sched_get_priority{min,max}(). */

    FIVMR_PR_SUPPORTED=false; /* assume it doesn't work until we prove it does */
    
#if defined(FIVMR_HAVE_POSIX_SCHEDULING) || defined(_POSIX_THREAD_PRIORITY_SCHEDULING)
    /* POSIX 1003.1b says that the range for mutex priorities is the same
       as that of SCHED_FIFO */
    FIVMR_PR_MIN=sched_get_priority_min(SCHED_FIFO);
    FIVMR_PR_MAX=sched_get_priority_max(SCHED_FIFO);
    if (FIVMR_PR_MIN<0 || FIVMR_PR_MAX<0) {
	LOG(1,("Warning: POSIX scheduling seems not to work right."));
        FIVMR_PR_SUPPORTED=false;
	FIVMR_PR_MIN=1;
	FIVMR_PR_MAX=100;
    } else {
        int oldsched;
        struct sched_param oldparam;
        struct sched_param newparam;
#  if defined(FIVMR_HAVE_POSIX_SCHEDULING)
	LOG(1,("POSIX scheduling detected, min = %d, max = %d",
	       FIVMR_PR_MIN,FIVMR_PR_MAX));
        
        /* now see if it really works */
        oldsched=sched_getscheduler(0);
        if (oldsched>=0) {
            bzero(&oldparam,sizeof(oldparam));
            bzero(&newparam,sizeof(newparam));
            if (sched_getparam(0,&oldparam)==0) {
                newparam.sched_priority=FIVMR_PR_MAX;
                if (sched_setscheduler(0,SCHED_FIFO,&newparam)==0) {
                    LOG(1,("attempt to run at FIFO_MAX priority successful; reverting "
                           "priority/scheduler to %d/%d and continuing.",
                           oldparam.sched_priority,oldsched));
                    FIVMR_PR_SUPPORTED=true;
                    
                    if (sched_setscheduler(0,oldsched,&oldparam)!=0) {
                        fivmr_abort("Could not revert to default scheduler while at FIFO_MAX "
                                    "priority!  Aborting to avoid system deadlock.");
                    }
                } else {
                    LOG(1,("POSIX scheduling doesn't support sched_setscheduler "
                           "-- DEACTIVATING!"));
                }
            } else {
                LOG(1,("POSIX scheduling doesn't support sched_getparam -- DEACTIVATING!"));
            }
        } else {
            LOG(1,("POSIX scheduling doesn't support sched_getscheduler -- DEACTIVATING!"));
        }
#  else
        LOG(1,("POSIX priorities probed but scheduling not completely functional, min = %d, max = %d",
               FIVMR_PR_MIN,FIVMR_PR_MAX));
#  endif
    }
    FIVMR_TPR_NORMAL_MIN=FIVMR_THREADPRIORITY(FIVMR_TPR_NORMAL,
                                              sched_get_priority_min(SCHED_OTHER));
    FIVMR_TPR_NORMAL_MAX=FIVMR_THREADPRIORITY(FIVMR_TPR_NORMAL,
                                              sched_get_priority_max(SCHED_OTHER));
    FIVMR_TPR_FIFO_MIN=FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO,
                                            sched_get_priority_min(SCHED_FIFO));
    FIVMR_TPR_FIFO_MAX=FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO,
                                            sched_get_priority_max(SCHED_FIFO));
    FIVMR_TPR_RR_MIN=FIVMR_THREADPRIORITY(FIVMR_TPR_RR,
                                          sched_get_priority_min(SCHED_RR));
    FIVMR_TPR_RR_MAX=FIVMR_THREADPRIORITY(FIVMR_TPR_RR,
                                          sched_get_priority_max(SCHED_RR));
    fivmr_assert(FIVMR_TPR_NORMAL_MIN<=FIVMR_TPR_NORMAL_MAX);
    fivmr_assert(FIVMR_TPR_FIFO_MIN<FIVMR_TPR_FIFO_MAX);
    fivmr_assert(FIVMR_TPR_RR_MIN<FIVMR_TPR_FIFO_MAX);
#else
    LOG(1,("Warning: we don't have POSIX scheduling."));
    FIVMR_PR_SUPPORTED=false;
    FIVMR_PR_MIN=1;
    FIVMR_PR_MAX=100;
    /* FIXME set the TPR levels to something?  so that does computations on them doesn't
       break? */
#endif
    
    FIVMR_PR_CRITICAL=FIVMR_PR_MAX+1;
    fivmr_assert(FIVMR_PR_MIN>=0);
    fivmr_assert(FIVMR_PR_MAX>=0);
    fivmr_assert(FIVMR_PR_CRITICAL>=0);
    FIVMR_PR_INHERIT=FIVMR_PR_MIN-1;
    FIVMR_PR_NONE=FIVMR_PR_MIN-2;
    
#if FIVMR_DYN_LOADING
    fivmr_Module_init();
#endif
    fivmr_Thread_init();
    
    sysPageSize=getpagesize();
    
    fivmr_assert(FIVMR_PAGE_SIZE>=sysPageSize);
    fivmr_assert((FIVMR_PAGE_SIZE%sysPageSize)==0);
    
    LOG(1,("System page size is %p; our page size is %p.",
           sysPageSize,FIVMR_PAGE_SIZE));
    
    if (FIVMR_PAGE_SIZE>sysPageSize) {
        LOG(1,("WARNING: System page size is smaller than our page size.  Virtual "
               "memory will be wasted to achieve alignment.  Suggest building with "
               "--g-self-man-mem."));
    }
    
    fivmr_POSIX_execAllocInit();
    
    LOG(1,("Executable allocator initialized."));
}

uintptr_t fivmr_abortf(const char *fmt,...) {
    va_list lst;
    va_start(lst,fmt);
    fprintf(stderr,"fivmr ABORT: ");
    vfprintf(stderr,fmt,lst);
    fprintf(stderr,"\n");
    va_end(lst);
    fprintf(stderr,"fivmr VM instance at pid = %d dumping core.\n",getpid());
    abort();
    return 0;
}

uintptr_t fivmr_abort(const char *message) {
    if (message==NULL) {
	message=
	    "(message was NULL; typically implies Java exception "
	    "during attempt to abort)";
    }
    fprintf(stderr,"fivmr ABORT: %s\n",message);
    fprintf(stderr,"fivmr VM instance at pid = %d dumping core.\n",getpid());
    abort();
    return 0;
}

void fivmr_yield(void) {
    sched_yield();
}

void *fivmr_tryAllocPages_IMPL(uintptr_t numPages,
                               bool *isZero,
                               const char *whereFile,
                               int whereLine) {
    void *result;

    LOG(6,("fivmr_tryAllocPages(%p) called from %s:%d.",numPages,whereFile,whereLine));
    if (sysPageSize==FIVMR_PAGE_SIZE) {
        result=mmap(NULL,numPages<<FIVMSYS_LOG_PAGE_SIZE,
                    PROT_READ|PROT_WRITE,MAP_ANON|MAP_PRIVATE,
                    -1,0);
        if (result==(void*)-1) {
            LOG(5,("fivmr_tryAllocPages(%p) from %s:%d failing.",numPages,whereFile,whereLine));
            return NULL;
        }
    } else {
        void *alloced;
        alloced=mmap(NULL,(numPages+1)<<FIVMSYS_LOG_PAGE_SIZE,
                     PROT_READ|PROT_WRITE,MAP_ANON|MAP_PRIVATE,
                     -1,0);
        if (alloced==(void*)-1) {
            LOG(5,("fivmr_tryAllocPages(%p) from %s:%d failing.",numPages,whereFile,whereLine));
            return NULL;
        }
        result=(void*)((((uintptr_t)alloced)+FIVMR_PAGE_SIZE)&~(FIVMR_PAGE_SIZE-1));
        ((void**)result)[-1]=alloced;
    }
    
    if (isZero!=NULL) {
        *isZero=true;
    }
    
    LOG(5,("fivmr_tryAllocPages(%p) returning %p",numPages,result));

    return result;
}

void fivmr_freePages_IMPL(void *begin,uintptr_t numPages,
                          const char *whereFile,
                          int whereLine) {
    int res;
    LOG(6,("fivmr_freePages(%p,%p) called from %s:%d.",begin,numPages,whereFile,whereLine));
    if (sysPageSize==FIVMR_PAGE_SIZE) {
        LOG(5,("calling munmap(%p,%" PRIuPTR ")",
               begin,numPages<<FIVMSYS_LOG_PAGE_SIZE));
        res=munmap(begin,numPages<<FIVMSYS_LOG_PAGE_SIZE);
        fivmr_assert(res==0);
    } else {
        void *alloced=((void**)begin)[-1];
        LOG(5,("calling munmap(%p,%" PRIuPTR ")",
               alloced,(numPages+1)<<FIVMSYS_LOG_PAGE_SIZE));
        res=munmap(alloced,(numPages+1)<<FIVMSYS_LOG_PAGE_SIZE);
        fivmr_assert(res==0);
    }
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

void fivmr_debugMemory_IMPL(const char *flnm,
                            int line) {
    /* don't know how to do it */
}

#endif

