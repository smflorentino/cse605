/*
 * fivmr_nogc.c
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

#if !FIVMBUILD_SPECIAL_GC || FIVMBUILD__NOGC

#include "fivmr.h"

#include <unistd.h>

#define FIVMR_TLB_SIZE FIVMR_PAGE_SIZE*10
#define FIVMR_LARGE_OBJECT (FIVMR_TLB_SIZE/2)

void fivmr_NOGC_init(fivmr_GC *gc) {
    fivmr_Lock_init(&gc->gcLock,fivmr_Priority_bound(FIVMR_PR_MAX,
                                                     fivmr_VMfromGC(gc)->maxPriority));
}

void fivmr_NOGC_clear(fivmr_ThreadState *ts) {
    ts->gc.alloc[0].bump=0;
    ts->gc.alloc[0].start=0;
    ts->gc.alloc[0].size=0;
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        if (FIVMR_SCJ(&ts->vm->settings)) {
            ts->gc.currentArea=&ts->vm->gc.immortalMemoryArea;
        } else {
            ts->gc.currentArea=&ts->gc.heapMemoryArea;
        }
        ts->gc.baseStackEntry.area=ts->gc.currentArea;
        ts->gc.baseStackEntry.prev=NULL;
        ts->gc.baseStackEntry.next=NULL;
        ts->gc.scopeStack=&ts->gc.baseStackEntry;
        ts->gc.scopeBacking=NULL;
        ts->gc.heapMemoryArea.scopeID=FIVMR_GC_SH_MARK2;
    }
}

void fivmr_NOGC_startThread(fivmr_ThreadState *ts) {
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                          FIVMR_FLOWLOG_SUBTYPE_ENTER,
                          (uintptr_t)ts->gc.currentArea);
    }
}

void fivmr_NOGC_commitThread(fivmr_ThreadState *ts) {
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        while (ts->gc.scopeStack->prev) {
            fivmr_MemoryArea *area=ts->gc.scopeStack->area;
            fivmr_MemoryArea_pop(ts, ts->vm, area);
            fivmr_MemoryArea_free(ts, area);
        }
        if (ts->gc.scopeBacking) {
            fivmr_ScopeBacking_free(ts);
        }
    }
}

static uintptr_t sysAlloc(fivmr_GC *gc,
                          uintptr_t size) {
    uintptr_t result;
    uintptr_t numPages;
    LOG(5,("sysAlloc(%p) called.",size));
    numPages=fivmr_pages(size);
    LOG(5,("will allocate %p pages.",numPages));
    result=(uintptr_t)fivmr_allocPages(numPages,NULL);
    if (result!=0) {
        fivmr_Lock_lock(&gc->gcLock);
        gc->numPagesUsed+=numPages;
        fivmr_Lock_unlock(&gc->gcLock);
    }
    LOG(5,("sysAlloc(%p) returning %p",size,result));
    return result;
}

uintptr_t fivmr_NOGC_allocRawSlow(fivmr_ThreadState *ts,
                                  fivmr_GCSpace space,
                                  uintptr_t size,
                                  uintptr_t alignStart,
                                  uintptr_t align,
                                  fivmr_AllocEffort effort,
                                  const char *description) {
    uintptr_t result;
    
    LOG(11,("fivmr_allocRaw_slow(%p, %p, %p) called",ts,size,align));

    size=fivmr_alignRaw(size,sizeof(uintptr_t));
    
    if ((fivmr_align(ts->gc.alloc[0].bump+alignStart,align)
	 -alignStart+size)-ts->gc.alloc[0].start > ts->gc.alloc[0].size) {
	if (size>=FIVMR_LARGE_OBJECT) {
	    result=sysAlloc(&ts->vm->gc,size+fivmr_align(alignStart,align)-alignStart);
            if (result==0) {
                fivmr_throwOutOfMemoryError_inJava(ts);
                return 0;
            }
	    result+=FIVMR_ALLOC_OFFSET(&ts->vm->settings);
	    result=fivmr_align(result+alignStart,align)-alignStart;
	    LOG(9,("allocRaw_slow returning %p",result));
	    return result;
	} else {
	    uintptr_t newSpace=sysAlloc(&ts->vm->gc,FIVMR_TLB_SIZE);
	    ts->gc.alloc[0].bump=ts->gc.alloc[0].start=
		newSpace+FIVMR_ALLOC_OFFSET(&ts->vm->settings);
	    ts->gc.alloc[0].size=FIVMR_TLB_SIZE;
	}
    }
    
    LOG(10,("bump = %p, start = %p, size = %p, align = %p",
	    ts->gc.alloc[0].bump,ts->gc.alloc[0].start,ts->gc.alloc[0].size,align));

    result=
	fivmr_align(ts->gc.alloc[0].bump+alignStart,align)
	-alignStart;
    ts->gc.alloc[0].bump=result+size;
    
    LOG(9,("allocRaw_slow returning %p",result));
    
    return result;
}

void fivmr_NOGC_handleHandshake(fivmr_ThreadState *ts) {
    /* nothing to do */
}

int64_t fivmr_NOGC_freeMemory(fivmr_GC *gc) {
    return 1048576; /* FIXME */
}

int64_t fivmr_NOGC_totalMemory(fivmr_GC *gc) {
    return gc->numPagesUsed*FIVMR_PAGE_SIZE;
}

int64_t fivmr_NOGC_maxMemory(fivmr_GC *gc) {
    return INT64_C(0xfffffffffffffff); /* FIXME */
}

void fivmr_NOGC_asyncCollect(fivmr_GC *gc) {
    /* nothing to do. */
}

void fivmr_NOGC_collectFromJava(fivmr_GC *gc,
                                const char *descrIn,
                                const char *descrWhat) {
    /* nothing to do. */
}

void fivmr_NOGC_collectFromNative(fivmr_GC *gc,
                                  const char *descrIn,
                                  const char *descrWhat) {
    /* nothing to do. */
}

void fivmr_NOGC_report(fivmr_GC *gc,
                       const char *name) {
}

bool fivmr_NOGC_setMaxHeap(fivmr_GC *gc,
                           int64_t bytes) {
    return false;
}

bool fivmr_NOGC_setTrigger(fivmr_GC *gc,
                           int64_t bytes) {
    return false;
}

void fivmr_NOGC_resetStats(fivmr_GC *gc) {
}

int64_t fivmr_NOGC_numIterationsCompleted(fivmr_GC *gc) {
    return 0;
}

void fivmr_NOGC_markSlow(fivmr_ThreadState *ts,
                         fivmr_Object obj) {
}

fivmr_Spine fivmr_NOGC_allocSSSlow(fivmr_ThreadState *ts,
                                   uintptr_t spineLength,
                                   int32_t numEle,
                                   const char *description) {
    return 0;
}

void fivmr_NOGC_claimMachineCode(fivmr_ThreadState *ts,
                                 fivmr_MachineCode *mc) {
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    fivmr_MachineCode_up(mc);
}

void fivmr_NOGC_setPriority(fivmr_GC *gc,
                            fivmr_ThreadPriority prio) {
}

bool fivmr_NOGC_getNextDestructor(fivmr_GC *gc,
                                  fivmr_Handle *objCell,
                                  bool wait) {
    return false;
}

void fivmr_NOGC_signalExit(fivmr_GC *gc) {
    /* nothing to do */
}

void fivmr_NOGC_shutdown(fivmr_GC *gc) {
    /* nothing to do */
}

#endif
