/*
 * fivmr_util.h
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

#ifndef FP_FIVMR_UTIL_H
#define FP_FIVMR_UTIL_H

#include <fivmr_target.h>
#include <fivmr_config.h>

#ifdef FIVMC_VERSION
#define FIVMR_VERSION FIVMC_VERSION
#else
#define FIVMR_VERSION FIVMR_BASE_VERSION
#endif

#ifdef HAVE_INTTYPES_H
#  include <inttypes.h>
#endif

/* inttypes.h should define this, but if it doesn't ... */
#ifndef PRIuPTR
#define PRIuPTR "lu"
#endif

#ifdef HAVE_STDBOOL_H
#  include <stdbool.h>
#endif

#ifdef HAVE_STDLIB_H
#  include <stdlib.h>
#endif

#ifdef HAVE_STDIO_H
#  include <stdio.h>
#endif

#ifdef HAVE_STDARG_H
#  include <stdarg.h>
#endif

#if FIVMR_POSIX || FIVMR_WIN32
extern int32_t fivmr_logLevel;
#endif

#if FIVMR_POSIX || FIVMR_WIN32
extern uint32_t fivmr_debugLevel;
#endif

#if FIVMR_POSIX || FIVMR_WIN32
extern const char *fivmr_logFile;
#endif
#if FIVMR_POSIX
extern FILE *fivmr_log;
#endif

#if FIVMR_POSIX && FIVMR_FLOW_LOGGING
extern const char *fivmr_flowLogFile;
#endif

#ifndef FALSE
#define FALSE 0
#endif

/* min/max macros ... these should be used carefully since the arguments
   are evaluated more than once. */
#define fivmr_min(a,b) ((a)<(b)?(a):(b))
#define fivmr_max(a,b) ((a)>(b)?(a):(b))

#define FIVMR_CONCAT_IMPL(a,b) a##b
#define FIVMR_CONCAT(a,b) FIVMR_CONCAT_IMPL(a,b)

/* logging, assertions, and aborting. */

/* aborts don't return but we make it seem that they do to make them
   easier to use in certain situations... */
uintptr_t fivmr_abort(const char *message);
uintptr_t fivmr_abortf(const char *fmt,...);

void fivmr_Log_init(void);
void fivmr_Log_lock(void);
void fivmr_Log_unlock(void);
void fivmr_Log_vprintf(const char *msg,va_list lst);
void fivmr_Log_printf(const char *msg,...);
void fivmr_Log_print(const char *msg);
void fivmr_Log_lockedPrintf(const char *msg,...);
void fivmr_Log_lockedPrint(const char *msg);

static inline int fivmr_Log_getLevel(void) {
#if !FIVMR_POSIX
    return FIVMR_LOG_LEVEL;
#else
    if (FIVMR_LOG_LEVEL<fivmr_logLevel) {
        return FIVMR_LOG_LEVEL;
    } else {
        return fivmr_logLevel;
    }
#endif
}

/* FIXME: namespace polution on LOG and LOGGING */

#if !FIVMR_POSIX
#define LOGGING(level) \
    ((level)<=FIVMR_LOG_LEVEL)
#else
#define LOGGING(level) \
    ((level)<=FIVMR_LOG_LEVEL && (level)<=fivmr_logLevel)
#endif

#define LOG(level,printfargs) do {					\
	if (LOGGING(level)) {						\
	    fivmr_Log_lock();						\
	    fivmr_Log_printf("fivmr log: (%s:%d) ",__FILE__,__LINE__);	\
	    fivmr_Log_printf printfargs;				\
	    fivmr_Log_printf("\n");					\
	    fivmr_Log_unlock();						\
	}								\
    } while (0)

#define DB_MEMAREA 0x001

#define DEBUGGING(level) \
    ((level & fivmr_debugLevel))

#define DEBUG(level,printfargs) do {          \
  if (DEBUGGING(level)) {           \
    fivmr_Log_lock();           \
    fivmr_Log_printf("fivmr log: (%s:%d) ",__FILE__,__LINE__);  \
    fivmr_Log_printf printfargs;        \
    fivmr_Log_printf("\n");         \
    fivmr_Log_unlock();           \
  }               \
    } while (0)



#define LOGptrconst(level,constName) do {			\
	LOG(level,(#constName " = %" PRIuPTR,constName));	\
    } while (0)

static inline uintptr_t fivmr_zero() {
    return 0;
}

/* note that assertions return 0... it's to make them easier to use in
   certain situations. */
#define fivmr_assert(exp)                                               \
    ((!FIVMR_ASSERTS_ON||(exp))?                                        \
     fivmr_zero():fivmr_abortf("(%s:%d) assertion '%s' failed.",        \
			     __FILE__,__LINE__,#exp))

#if FIVMR_FLOW_LOGGING
extern int fivmr_flowLogEnabled;

/* FJFL on big endian, LFJF on little endian */
#define FIVMR_FLOWLOG_MAGIC 0x464A464C

#if FIVMR_FLOW_LOGGING_FATEVENTS
#define FIVMR_FLOWLOG_VERSION 3
#else
#define FIVMR_FLOWLOG_VERSION 2
#endif

/* Top four bits of the platform are pointer size. */
#if FIVMSYS_PTRSIZE == 4
#define FIVMR_FLOWLOG_PLATFORM(x) (0x2000 | (x))
#elif FIVMSYS_PTRSIZE == 8
#define FIVMR_FLOWLOG_PLATFORM(x) (0x3000 | (x))
#else
#error "Flow logging an unknown pointer size"
#endif

#define FIVMR_FLOWLOG_PLATFORM_POSIX FIVMR_FLOWLOG_PLATFORM(1)
#define FIVMR_FLOWLOG_PLATFORM_RTEMS FIVMR_FLOWLOG_PLATFORM(2)

#define FIVMR_FLOWLOG_BUFFERSIZE (sizeof(fivmr_FlowLogBuffer)   \
                                  - sizeof(fivmr_FlowLogEvent)  \
                                  + (sizeof(fivmr_FlowLogEvent) \
                                     * FIVMR_FLOWLOG_BUFFER_ENTRIES))

/* The following constants must be kept perfectly in sync with the
 * constants in fivmcommon.jar com.fiji.fivm.r1.FlowLog */

/* Flow log event types */
#define FIVMR_FLOWLOG_TYPE_VM                   0
#define FIVMR_FLOWLOG_TYPE_METHOD               1
#define FIVMR_FLOWLOG_TYPE_MONITOR              2
#define FIVMR_FLOWLOG_TYPE_PRIORITY             3
#define FIVMR_FLOWLOG_TYPE_THREAD               4
#define FIVMR_FLOWLOG_TYPE_SCOPE                5
#define FIVMR_FLOWLOG_TYPE_ALLOC                6
#define FIVMR_FLOWLOG_TYPE_REFERENCE            7
#define FIVMR_FLOWLOG_TYPE_SCJ                  128

/* Flow log event subtypes */
/* VM */
#define FIVMR_FLOWLOG_SUBTYPE_INIT              1
#define FIVMR_FLOWLOG_SUBTYPE_EXIT              2
/* Method; ENTER or EXIT, inline and regular */
#define FIVMR_FLOWLOG_SUBTYPE_ENTER             1
/*      FIVMR_FLOWLOG_SUBTYPE_EXIT              2 */
#define FIVMR_FLOWLOG_SUBTYPE_ENTER_INLINE      3
#define FIVMR_FLOWLOG_SUBTYPE_EXIT_INLINE       4
/* Monitor */
#define FIVMR_FLOWLOG_SUBTYPE_LOCK_FAST         1
#define FIVMR_FLOWLOG_SUBTYPE_UNLOCK_FAST       2
#define FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_BEGIN   3
#define FIVMR_FLOWLOG_SUBTYPE_UNLOCK_SLOW_BEGIN 4
#define FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_END     5
#define FIVMR_FLOWLOG_SUBTYPE_UNLOCK_SLOW_END   6
#define FIVMR_FLOWLOG_SUBTYPE_UNLOCK_COMPLETE   7
#define FIVMR_FLOWLOG_SUBTYPE_RELOCK            8
#define FIVMR_FLOWLOG_SUBTYPE_WAIT              9
#define FIVMR_FLOWLOG_SUBTYPE_NOTIFY            10
#define FIVMR_FLOWLOG_SUBTYPE_NOTIFY_ALL        11
/*      FIVMR_FLOWLOG_SUBTYPE_WAKE              12 */
/* Threads */
#define FIVMR_FLOWLOG_SUBTYPE_CREATE            1
/*      FIVMR_FLOWLOG_SUBTYPE_EXIT              2 */
#define FIVMR_FLOWLOG_SUBTYPE_RUN               3
#define FIVMR_FLOWLOG_SUBTYPE_YIELD             4
#define FIVMR_FLOWLOG_SUBTYPE_SLEEP             5
#define FIVMR_FLOWLOG_SUBTYPE_PRIORITY          6
#define FIVMR_FLOWLOG_SUBTYPE_WAKE              12 /* To fit monitors */
/* Scopes */
/*      FIVMR_FLOWLOG_SUBTYPE_ENTER             1 */
/*      FIVMR_FLOWLOG_SUBTYPE_EXIT              2 */ /* Nonexistent in SCJ */
#define FIVMR_FLOWLOG_SUBTYPE_ALLOC_BACKING     3
#define FIVMR_FLOWLOG_SUBTYPE_ALLOC_SCOPE       4
#define FIVMR_FLOWLOG_SUBTYPE_FREE_BACKING      5
#define FIVMR_FLOWLOG_SUBTYPE_FREE_SCOPE        6
#define FIVMR_FLOWLOG_SUBTYPE_PUSH              7
#define FIVMR_FLOWLOG_SUBTYPE_POP               8
#define FIVMR_FLOWLOG_SUBTYPE_IMMORTAL          9
/* Alloc */
#define FIVMR_FLOWLOG_SUBTYPE_OBJECT            1
#define FIVMR_FLOWLOG_SUBTYPE_ARRAY             2
/* Reference */
#define FIVMR_FLOWLOG_SUBTYPE_PUTFIELD          1
#define FIVMR_FLOWLOG_SUBTYPE_PUTSTATIC         2
#define FIVMR_FLOWLOG_SUBTYPE_ARRAYSTORE        3
/* SCJ */
#define FIVMR_FLOWLOG_SUBTYPE_PEH_DEADLINE      1
#define FIVMR_FLOWLOG_SUBTYPE_T0                2
#define FIVMR_FLOWLOG_SUBTYPE_CYCLE             3
#define FIVMR_FLOWLOG_SUBTYPE_RELEASE           4

void fivmr_FlowLog_init(void);
void fivmr_FlowLog_finalize(void);

/* The log32 versions are for the convenience of C1 when inserting flow
 * logging events into the IR. */
#if FIVMR_FLOW_LOGGING_FATEVENTS
#define fivmr_FlowLog_log(ts, type, subtype, data) fivmr_FlowLog_logIMPL((fivmr_ThreadState *)ts, type, subtype, ((fivmr_ThreadState *)ts)->id, fivmr_readCPUTimestamp(), (uint64_t)data, (uint64_t)0)
#define fivmr_FlowLog_log_fat(ts, type, subtype, data, extdata) fivmr_FlowLog_logIMPL((fivmr_ThreadState *)ts, type, subtype, ((fivmr_ThreadState *)ts)->id, fivmr_readCPUTimestamp(), (uint64_t)data, (uint64_t)extdata)
#define fivmr_FlowLog_log32(ts, type, subtype, data1, data2) fivmr_FlowLog_log(ts, type, subtype, ((uint64_t)data1 << 32) | ((uint64_t)data2 & 0xffffffff))
#define fivmr_FlowLog_log32_fat(ts, type, subtype, data1, data2, extdata1, extdata2) fivmr_FlowLog_log_fat(ts, type, subtype, ((uint64_t)data1 << 32) | ((uint64_t)data2 & 0xffffffff), ((uint64_t)extdata1 << 32) | ((uint64_t)extdata2 & 0xffffffff))
#else /* FIVMR_FLOW_LOGGING_FATEVENTS */
#define fivmr_FlowLog_log(ts, type, subtype, data) fivmr_FlowLog_logIMPL((fivmr_ThreadState *)ts, type, subtype, ((fivmr_ThreadState *)ts)->id, fivmr_readCPUTimestamp(), (uint64_t)data)
#define fivmr_FlowLog_log_fat(ts, type, subtype, data, extdata) fivmr_FlowLog_logIMPL((fivmr_ThreadState *)ts, type, subtype, ((fivmr_ThreadState *)ts)->id, fivmr_readCPUTimestamp(), (uint64_t)data)
#define fivmr_FlowLog_log32(ts, type, subtype, data1, data2) fivmr_FlowLog_log(ts, type, subtype, ((uint64_t)data1 << 32) | ((uint64_t)data2 & 0xffffffff))
#define fivmr_FlowLog_log32_fat(ts, type, subtype, data1, data2, extdata1, extdata2) fivmr_FlowLog_log(ts, type, subtype, ((uint64_t)data1 << 32) | ((uint64_t)data2 & 0xffff))
#endif /* FIVMR_FLOW_LOGGING_FATEVENTS */

/* Platform-specific Flow Logging functions provided by
 * fivmr_<platform>_flowlog.c. */
void fivmr_FlowLog_lock(void);
void fivmr_FlowLog_unlock(void);

void fivmr_FlowLog_wait(void);
void fivmr_FlowLog_notify(void);

void fivmr_FlowLog_platform_init(void);
void fivmr_FlowLog_platform_finalize(void);


#else /* FIVMR_FLOW_LOGGING */

#define fivmr_FlowLog_log(ts, type, subtype, data)
#define fivmr_FlowLog_log_fat(ts, type, subtype, data, extdata)
#define fivmr_FlowLog_log32(ts, type, subtype, data1, data2)
#define fivmr_FlowLog_log32_fat(ts, type, subtype, data1, data2, extdata1, extdata2)

#endif /* FIVMR_FLOW_LOGGING */

/* utilities to help Java */

static inline void fivmr_Log_printNum(int64_t num) {
    fivmr_Log_printf("%" PRId64,num);
}

static inline void fivmr_Log_printHex(int64_t num) {
    fivmr_Log_printf("%" PRIx64,num);
}

/* sorting */

static inline void fivmr_sort(void *array,size_t nmemb,size_t memsize,
                              int (*compare)(const void *a,const void *b)) {
    qsort(array,nmemb,memsize,compare);
}

/* simple allocation. */

#ifndef FIVMR_MALLOC_LOG_LEVEL
/* change this to 1 if you want detailed malloc debugging, which is great for
   when you've got malloc heap corruption bugs. */
#define FIVMR_MALLOC_LOG_LEVEL 11
#endif

static inline void *fivmr_malloc_IMPL(uintptr_t size,
                                      const char *flnm,
                                      int line) {
    void *result=malloc(size);
    LOG(FIVMR_MALLOC_LOG_LEVEL,("(%s:%d) allocated %p with size %p",flnm,line,result,size));
    return result;

}

#define fivmr_malloc(size) \
    (fivmr_malloc_IMPL((size),__FILE__,__LINE__))

static inline void *fivmr_mallocAssert_IMPL(uintptr_t size,
                                            const char *flnm,
                                            int line) {
    void *result=malloc(size);
    LOG(FIVMR_MALLOC_LOG_LEVEL,("(%s:%d) allocated %p with size %p",flnm,line,result,size));
    if (result==NULL) {
        fivmr_abortf("(%s:%d) malloc(%p) failed.",
                     flnm,line,size);
    }
    return result;
}

#define fivmr_mallocAssert(size) \
    fivmr_mallocAssert_IMPL((size),__FILE__,__LINE__)

static inline void *fivmr_calloc_IMPL(uintptr_t count,
                                      uintptr_t size,
                                      const char *flnm,
                                      int line) {
    void *result=calloc(count,size);
    LOG(FIVMR_MALLOC_LOG_LEVEL,("(%s:%d) calloced %p with size %p*%p",flnm,line,result,count,size));
    return result;
}

#define fivmr_calloc(count,size)                \
    (fivmr_calloc_IMPL((count),(size),__FILE__,__LINE__))

static inline void *fivmr_realloc_IMPL(void *ptr,
                                       uintptr_t size,
                                       const char *flnm,
                                       int line) {
    void *result;
    if (ptr==NULL) {
        result=malloc(size);
    } else {
        result=realloc(ptr, size);
    }
    LOG(FIVMR_MALLOC_LOG_LEVEL,("(%s:%d) reallocated %p with size %p, with original at %p",
            flnm,line,result,size,ptr));
    return result;
}

#define fivmr_realloc(ptr,size)                 \
    (fivmr_realloc_IMPL((ptr),(size),__FILE__,__LINE__))

static inline void *fivmr_reallocAssert_IMPL(void *ptr,
                                             uintptr_t size,
                                             const char *flnm,
                                             int line) {
    void *result;
    if (ptr==NULL) {
        result=malloc(size);
    } else {
        result=realloc(ptr,size);
    }
    LOG(FIVMR_MALLOC_LOG_LEVEL,("(%s:%d) reallocated %p with size %p, with original at %p",flnm,line,result,size,ptr));
    if (result==NULL) {
        fivmr_abortf("(%s:%d) malloc(%p, %p) failed.",
                     flnm,line,ptr,size);
    }
    return result;
}

#define fivmr_reallocAssert(ptr,size)                           \
    fivmr_reallocAssert_IMPL((ptr),(size),__FILE__,__LINE__)

static inline void fivmr_free_IMPL(const void *ptr,
                                   const char *flnm,
                                   int line) {
    LOG(FIVMR_MALLOC_LOG_LEVEL,("(%s:%d) freeing %p",flnm,line,ptr));
    free((void*)(uintptr_t)ptr);
}

#define fivmr_free(ptr) \
    (fivmr_free_IMPL((ptr),__FILE__,__LINE__))

static inline void fivmr_freeIfNotNull(const void *ptr) {
    if (ptr!=NULL) {
        free((void*)(uintptr_t)ptr);
    }
}

#endif

