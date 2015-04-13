/*
 * fivmr_gc.c
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

#define IMPORT_FUNCS(prefix)                                            \
    void prefix##_init(fivmr_GC *gc);                                   \
    void prefix##_resetStats(fivmr_GC *gc);                             \
    void prefix##_report(fivmr_GC *gc,const char *name);                \
    void prefix##_clear(fivmr_ThreadState *ts);                         \
    void prefix##_startThread(fivmr_ThreadState *ts);                   \
    void prefix##_commitThread(fivmr_ThreadState *ts);                  \
    void prefix##_handleHandshake(fivmr_ThreadState *ts);               \
    int64_t prefix##_numIterationsCompleted(fivmr_GC *gc);              \
    void prefix##_markSlow(fivmr_ThreadState *ts,                       \
                           fivmr_Object obj);                           \
    fivmr_Object prefix##_allocRawSlow(fivmr_ThreadState *ts,           \
                                       fivmr_GCSpace space,             \
                                       uintptr_t size,                  \
                                       uintptr_t alignStart,            \
                                       uintptr_t align,                 \
                                       fivmr_AllocEffort effort,        \
                                       const char *description);        \
    fivmr_Spine prefix##_allocSSSlow(fivmr_ThreadState *ts,             \
                                     uintptr_t spineLength,             \
                                     int32_t numEle,                    \
                                     const char *description);          \
    void prefix##_claimMachineCode(fivmr_ThreadState *ts,               \
                                   fivmr_MachineCode *mc);              \
    int64_t prefix##_freeMemory(fivmr_GC *gc);                          \
    int64_t prefix##_totalMemory(fivmr_GC *gc);                         \
    int64_t prefix##_maxMemory(fivmr_GC *gc);                           \
    void prefix##_asyncCollect(fivmr_GC *gc);                           \
    void prefix##_collectFromJava(fivmr_GC *gc,                         \
                                  const char *descrIn,                  \
                                  const char *descrWhat);               \
    void prefix##_collectFromNative(fivmr_GC *gc,                       \
                                    const char *descrIn,                \
                                    const char *descrWhat);             \
    void prefix##_setPriority(fivmr_GC *gc,                             \
                              fivmr_ThreadPriority prio);               \
    bool prefix##_setMaxHeap(fivmr_GC *gc,                              \
                             int64_t bytes);                            \
    bool prefix##_setTrigger(fivmr_GC *gc,                              \
                             int64_t bytes);                            \
    bool prefix##_getNextDestructor(fivmr_GC *gc,                       \
                                    fivmr_Handle *objCell,              \
                                    bool wait);                         \
    void prefix##_signalExit(fivmr_GC *gc);                             \
    void prefix##_shutdown(fivmr_GC *gc)

#if FIVMBUILD_SPECIAL_GC
#  if FIVMBUILD__NOGC
IMPORT_FUNCS(fivmr_NOGC);
#  elif FIVMBUILD__CMRGC || FIVMBUILD__HFGC
IMPORT_FUNCS(fivmr_SpecialGC);
#  else
#    error "bad GC"
#  endif
#else
IMPORT_FUNCS(fivmr_NOGC);
IMPORT_FUNCS(fivmr_OptCMRGC);
IMPORT_FUNCS(fivmr_OptCMRGCSMM);
IMPORT_FUNCS(fivmr_OptHFGC);
IMPORT_FUNCS(fivmr_GenericGC);
#endif

#define SET_FUNC_PTRS(gc,prefix) do {                                   \
        (gc)->resetStats             = prefix##_resetStats;             \
        (gc)->report                 = prefix##_report;                 \
        (gc)->clear                  = prefix##_clear;                  \
        (gc)->startThread            = prefix##_startThread;            \
        (gc)->commitThread           = prefix##_commitThread;           \
        (gc)->handleHandshake        = prefix##_handleHandshake;        \
        (gc)->numIterationsCompleted = prefix##_numIterationsCompleted; \
        (gc)->markSlow               = prefix##_markSlow;               \
        (gc)->allocRawSlow           = prefix##_allocRawSlow;           \
        (gc)->allocSSSlow            = prefix##_allocSSSlow;            \
        (gc)->claimMachineCode       = prefix##_claimMachineCode;       \
        (gc)->freeMemory             = prefix##_freeMemory;             \
        (gc)->totalMemory            = prefix##_totalMemory;            \
        (gc)->maxMemory              = prefix##_maxMemory;              \
        (gc)->asyncCollect           = prefix##_asyncCollect;           \
        (gc)->collectFromJava        = prefix##_collectFromJava;        \
        (gc)->collectFromNative      = prefix##_collectFromNative;      \
        (gc)->setPriority            = prefix##_setPriority;            \
        (gc)->setMaxHeap             = prefix##_setMaxHeap;             \
        (gc)->setTrigger             = prefix##_setTrigger;             \
        (gc)->getNextDestructor      = prefix##_getNextDestructor;      \
        (gc)->signalExit             = prefix##_signalExit;             \
        (gc)->shutdown               = prefix##_shutdown;               \
    } while (false)


void fivmr_GC_resetSettings(fivmr_GC *gc) {
    fivmr_VM *vm=fivmr_VMfromGC(gc);
    bzero(gc,sizeof(fivmr_GC));
    gc->gcTriggerPages=vm->config.gcDefTrigger>>FIVMSYS_LOG_PAGE_SIZE;
    gc->maxPagesUsed=vm->config.gcDefMaxMem>>FIVMSYS_LOG_PAGE_SIZE;
    gc->immortalMem=fivmr_VMfromGC(gc)->config.gcDefImmortalMem;
    gc->threadPriority=fivmr_ThreadPriority_min(vm->maxPriority,
                                                vm->config.gcDefPriority);
    gc->lastStart=fivmr_curTime();
    gc->lastEnd=fivmr_curTime();
    LOG(2,("configured GC priority: %d",fivmr_VMfromGC(gc)->config.gcDefPriority));
    LOG(2,("VM max priority: %d",fivmr_VMfromGC(gc)->maxPriority));
    LOG(2,("resuling GC priority: %d",gc->threadPriority));
}

void fivmr_GC_registerPayload(fivmr_GC *gc) {
    fivmr_VM *vm=fivmr_VMfromGC(gc);
    gc->logGC=FIVMR_DEF_LOG_GC(&vm->settings);
    gc->logSyncGC=FIVMR_DEF_LOG_SYNC_GC(&vm->settings);
    LOG(1,("GC logging: %s",gc->logGC?"ACTIVATED":"deactivated"));
    LOG(1,("Sync GC logging: %s",gc->logSyncGC?"ACTIVATED":"deactivated"));
}

void fivmr_GC_init(fivmr_GC *gc) {
#if FIVMBUILD_SPECIAL_GC /* are we specializing the runtime for one GC? */
#  if FIVMBUILD__NOGC
    /* we're specializing for the no GC case */
    SET_FUNC_PTRS(gc,fivmr_NOGC);
    fivmr_NOGC_init(gc); 
#  elif FIVMBUILD__CMRGC || FIVMBUILD__HFGC
    /* we're specializing for either CMRGC or HFGC; it would have been compiled
       with the prefix 'SpecialGC' */
    SET_FUNC_PTRS(gc,fivmr_SpecialGC);
    fivmr_SpecialGC_init(gc);
#  else
#    error "bad GC"
#  endif
#else /* using generic runtime, but pick the best GC for the settings */
    fivmr_VM *vm=fivmr_VMfromGC(gc);
    
    if (FIVMR_NOGC(&vm->settings)) {
        /* settings match no GC; just drop that in and we're done. */
        SET_FUNC_PTRS(gc,fivmr_NOGC);
        fivmr_NOGC_init(gc);
    } else if (FIVMR_CMRGC(&vm->settings) &&
               FIVMR_HM_NARROW(&vm->settings) &&
               FIVMR_OM_CONTIGUOUS(&vm->settings) &&
               !FIVMR_FORCE_ARRAYLETS(&vm->settings) &&
               !FIVMR_GC_BLACK_STACK(&vm->settings) &&
               !FIVMR_GC_DEBUG(&vm->settings)) {
        /* settings match standard optimized CMRGC, use that. */
        if (FIVMR_SELF_MANAGE_MEM(&vm->settings)) {
            SET_FUNC_PTRS(gc,fivmr_OptCMRGCSMM);
            fivmr_OptCMRGCSMM_init(gc);
        } else {
            SET_FUNC_PTRS(gc,fivmr_OptCMRGC);
            fivmr_OptCMRGC_init(gc);
        }
    } else if (FIVMR_HFGC(&vm->settings) &&
               FIVMR_HM_NARROW(&vm->settings) &&
               FIVMR_OM_FRAGMENTED(&vm->settings) &&
               !FIVMR_FORCE_ARRAYLETS(&vm->settings) &&
               !FIVMR_GC_BLACK_STACK(&vm->settings) &&
               !FIVMR_GC_DEBUG(&vm->settings) &&
               FIVMR_SELF_MANAGE_MEM(&vm->settings)) {
        /* settings match standard optimized HFGC, use that. */
        SET_FUNC_PTRS(gc,fivmr_OptHFGC);
        fivmr_OptHFGC_init(gc);
    } else if (FIVMR_CMRGC(&vm->settings) ||
               FIVMR_HFGC(&vm->settings)) {
        /* settings match either CMRGC or HFGC but not one that we've specialized;
           use the generic one that supports both (performance will not be
           amazingtacularfantastic, but oh well). */
        SET_FUNC_PTRS(gc,fivmr_GenericGC);
        fivmr_GenericGC_init(gc);
    } else {
        fivmr_assert(!"bad GC");
    }
#endif
}

