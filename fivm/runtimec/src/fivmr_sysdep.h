/*
 * fivmr_sysdep.h
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

#ifndef FP_FIVMR_SYSDEP_H
#define FP_FIVMR_SYSDEP_H

#include <fivmr_util.h>

#if !FIVMR_RTEMS && !FIVMR_POSIX && !FIVMR_WIN32
#error "Unsupported OS flavor (expected POSIX, RTEMS, or WIN32)"
#endif

#if FIVMR_RTEMS
#define FIVMR_STRICT_BOOST 1
#else
#define FIVMR_STRICT_BOOST 0
#endif

/* all of the headers needed by all of the compiled code and most of the runtime
   should be put here, so that we can change them around for different systems
   when necessary. */

#if defined(HAVE_SETJMP_H)
#  include <setjmp.h>
#endif

#if defined(HAVE_ALLOCA_H)
#  include <alloca.h>
#endif

#if defined(HAVE_STRINGS_H)
#  include <strings.h>
#endif

#if defined(HAVE_SYS_MMAN_H)
#  include <sys/mman.h>
#endif

#if FIVMR_RTEMS
#  include <rtems.h>
#  include <rtems/score/thread.h>
#endif

#if FIVMR_WIN32
#  include <windows.h>
#endif

#ifdef HAVE_STRING_H
#  include <string.h>
#endif

#ifdef HAVE_PTHREAD_H
#  include <pthread.h>
#endif

#ifdef HAVE_MATH_H
#  include <math.h>
#endif

#ifdef HAVE_STDLIB_H
#  include <stdlib.h>
#endif

#ifdef HAVE_STDARG_H
#  include <stdarg.h>
#endif

#ifdef HAVE_STDIO_H
#  include <stdio.h>
#endif

#ifdef HAVE_ERRNO_H
#  include <errno.h>
#endif

#ifdef HAVE_UNISTD_H
#  include <unistd.h>
#endif

#ifdef HAVE_SYS_TYPES_H
#  include <sys/types.h>
#endif

#ifdef HAVE_FCNTL_H
#  include <fcntl.h>
#endif

#ifdef HAVE_SYS_IOCTL_H
#  include <sys/ioctl.h>
#endif

#ifdef HAVE_SYS_SOCKET_H
#  include <sys/socket.h>
#endif

#ifdef HAVE_SYS_UN_H
#  include <sys/un.h>
#endif

#ifdef HAVE_NETINET_IN_H
#  include <netinet/in.h>
#endif

#ifdef HAVE_NETINET_TCP_H
#  include <netinet/tcp.h>
#endif

#ifdef HAVE_NETDB_H
#  include <netdb.h>
#endif

#ifdef HAVE_ARPA_INET_H
#  include <arpa/inet.h>
#endif

#ifdef HAVE_DIRENT_H
#  include <dirent.h>
#endif

#ifdef HAVE_SYS_STAT_H
#  include <sys/stat.h>
#endif

#ifdef HAVE_IFADDRS_H
#  include <ifaddrs.h>
#endif

#if defined(HAVE_SYS_VFS_H)
#include <sys/vfs.h>
#endif

#if defined(HAVE_SYS_PARAM_H)
#include <sys/param.h>
#endif

#if defined(HAVE_SYS_MOUNT_H)
#include <sys/mount.h>
#endif

#if defined(HAVE_SIGNAL_H)
#include <signal.h>
#endif

#if defined(HAVE_SYS_UCONTEXT_H)
#ifndef __USE_GNU
#define __USE_GNU
#include <sys/ucontext.h>
#undef __USE_GNU
#else
#include <sys/ucontext.h>
#endif
#endif

#if defined(HAVE_SEMAPHORE_H)
#include <semaphore.h>
#endif

/* some stupid defines */

#if defined(HAVE_SCHED_SETSCHEDULER) && \
    defined(HAVE_SCHED_GET_PRIORITY_MIN) && \
    defined(HAVE_SCHED_GET_PRIORITY_MAX)
#define FIVMR_HAVE_POSIX_SCHEDULING
#endif

#if FIVMR_X86
#define FIVMR_HAVE_NATIVE_BACKEND 1
#endif

#define fivmr_offsetof(structname,structfield) \
    ((uintptr_t) (&(((structname*)NULL)->structfield)))

#if 1 /* FIXME: detect if the compiler supports __builtin_expect */
#define fivmr_likely(x) __builtin_expect(!!(x), 1)
#define fivmr_unlikely(x) __builtin_expect(!!(x), 0)
#else
#define fivmr_likely(x) (x)
#define fivmr_unlikely(x) (x)
#endif

#define fivmr_semantically_likely(x) fivmr_likely(x)
#define fivmr_semantically_unlikely(x) fivmr_unlikely(x)

typedef int32_t fivmr_bool32; /* useful as a CAS-able bool */

/* This includes all of the system-dependent stuff. */

#if FIVMR_POSIX
/* need more head room on POSIX because some POSIX platforms may have their own
   "stack guards". */
#define FIVMR_STACK_HEIGHT_HEADROOM 8192*FIVMSYS_PTRSIZE
#else
#define FIVMR_STACK_HEIGHT_HEADROOM 4096*FIVMSYS_PTRSIZE
#endif

/* the following should come from fivmc:
   FIVMSYS_LOG_PAGE_SIZE */

#define FIVMR_PAGE_SIZE (1<<FIVMSYS_LOG_PAGE_SIZE)

/* low-level page allocation. */

/* FIXME: the page allocation methods currently always do page alignment.  but
   this isn't always necessary.  they should take an extra argument indicating
   the desired alignment. */

/* semantics of the following methods:
   - they return a pointer that is page-aligned
   - except for the 'try' methods, these methods will abort if the request
     fails.
   - if isZero is NULL, the result is guaranteed to be zeroed, otherwise
     isZero is set to either true or false depending on whether the result
     is guaranteed to be zeroed. */

/* currently: raises assertion if it cannot allocate!  ensures that the
   returned memory is zero'd.  */
void *fivmr_tryAllocPages_IMPL(uintptr_t numPages,
                               bool *isZero,
                               const char *whereFile,
                               int whereLine);
#define fivmr_tryAllocPages(numPages,isZero)                          \
    (fivmr_tryAllocPages_IMPL((numPages),(isZero),__FILE__,__LINE__))

static inline void *fivmr_tryAllocPage_IMPL(bool *isZero,
                                            const char *whereFile,
                                            int whereLine) {
    return fivmr_tryAllocPages_IMPL(1,isZero,whereFile,whereLine);
}
#define fivmr_tryAllocPage(isZero)                      \
    (fivmr_tryAllocPage_IMPL((isZero),__FILE__,__LINE__))

/* currently: raises assertion if it cannot allocate!  ensures that the
   returned memory is zero'd.  */
void *fivmr_allocPages_IMPL(uintptr_t numPages,
                            bool *isZero,
                            const char *whereFile,
                            int whereLine);
#define fivmr_allocPages(numPages,isZero)                       \
    (fivmr_allocPages_IMPL((numPages),(isZero),__FILE__,__LINE__))

void fivmr_freePages_IMPL(void *begin,uintptr_t numPages,
                          const char *whereFile,
                          int whereLine);
#define fivmr_freePages(begin,numPages)                                 \
    (fivmr_freePages_IMPL((begin),(numPages),__FILE__,__LINE__))

static inline void *fivmr_allocPage_IMPL(bool *isZero,
                                         const char *whereFile,
                                         int whereLine) {
    return fivmr_allocPages_IMPL(1,isZero,whereFile,whereLine);
}
#define fivmr_allocPage(isZero)                       \
    (fivmr_allocPage_IMPL((isZero),__FILE__,__LINE__))

static inline void fivmr_freePage_IMPL(void *page,
                                       const char *whereFile,
                                       int whereLine) {
    fivmr_freePages_IMPL(page,1,whereFile,whereLine);
}
#define fivmr_freePage(page)                            \
    (fivmr_freePage_IMPL((page),__FILE__,__LINE__))

static inline uintptr_t fivmr_pages(uintptr_t bytes) {
    return (bytes+FIVMR_PAGE_SIZE-1)>>FIVMSYS_LOG_PAGE_SIZE;
}

/* allocates raw pages - so you get page-aligned, zeroed memory, with the
   size rounded up to the next page. */
static inline void *fivmr_tryLargeAlloc_IMPL(uintptr_t bytes,
                                             bool *isZero,
                                             const char *whereFile,
                                             int whereLine) {
    return fivmr_tryAllocPages_IMPL(fivmr_pages(bytes),isZero,whereFile,whereLine);
}
#define fivmr_tryLargeAlloc(bytes,isZero)                             \
    (fivmr_tryLargeAlloc_IMPL((bytes),(isZero),__FILE__,__LINE__))

/* allocates raw pages - so you get page-aligned, zeroed memory, with the
   size rounded up to the next page. */
static inline void *fivmr_largeAlloc_IMPL(uintptr_t bytes,
                                          bool *isZero,
                                          const char *whereFile,
                                          int whereLine) {
    return fivmr_allocPages_IMPL(fivmr_pages(bytes),isZero,whereFile,whereLine);
}
#define fivmr_largeAlloc(bytes,isZero)                        \
    (fivmr_largeAlloc_IMPL((bytes),(isZero),__FILE__,__LINE__))

static inline void fivmr_largeFree_IMPL(void *begin,uintptr_t bytes,
                                        const char *whereFile,
                                        int whereLine) {
    fivmr_freePages_IMPL(begin,fivmr_pages(bytes),whereFile,whereLine);
}

#define fivmr_largeFree(begin,bytes)                            \
    (fivmr_largeFree_IMPL((begin),(bytes),__FILE__,__LINE__))

void fivmr_debugMemory_IMPL(const char *flnm,int line);

#if FIVMR_RTEMS
static inline void *fivmr_allocExecutable(uintptr_t size) {
    return fivmr_malloc(size);
}

static inline bool fivmr_supportDownsizeExec(void) {
    return false;
}

static inline void fivmr_downsizeExecutable(void *ptr,uintptr_t newSize) {
    fivmr_assert(!"downsizeExecutable not supported");
}

static inline void fivmr_freeExecutable(void *ptr) {
    fivmr_free(ptr);
}
#else
bool fivmr_POSIX_logAllocExec;

void fivmr_POSIX_execAllocInit(void);

void *fivmr_allocExecutable(uintptr_t size);

static inline bool fivmr_supportDownsizeExec(void) {
    return true;
}

void fivmr_downsizeExecutable(void *ptr,uintptr_t newSize);

void fivmr_freeExecutable(void *ptr);
#endif

#if 0 /* set to 1 to do memory debugging (currently only works on RTEMS) */
#define fivmr_debugMemory() fivmr_debugMemory_IMPL(__FILE__,__LINE__)
#else
#define fivmr_debugMemory() do {} while(0)
#endif

/* time. */

typedef uint64_t fivmr_Nanos;

fivmr_Nanos fivmr_nanosResolution();
fivmr_Nanos fivmr_curTime(void);
fivmr_Nanos fivmr_curTimePrecise(void);

static inline fivmr_Nanos fivmr_curTimeLogging(int level) {
    if (LOGGING(level)) {
        return fivmr_curTime();
    } else {
        return 0;
    }
}

/* timestamp support */
#define FIVMR_CPU_TIMESTAMP_IS_FAST

#ifdef HAVE_X86_RDTSC
static inline uint64_t fivmr_readCPUTimestamp(void) {
    uint32_t low=0;
    uint32_t high=0;
    __asm__ __volatile__ (
	"rdtsc\n"
	: "=a"(low),
	  "=d"(high)
	);
    return low|((uint64_t)high<<32);
}
#elif defined(HAVE_PPC_MFTB)
static inline uint64_t fivmr_readCPUTimestamp(void) {
    uint32_t low=0;
    uint32_t high=0;
    __asm__ __volatile__ (
	"mftb %0\n"
	"mftbu %1\n"
	: "+r"(low),
	  "+r"(high)
	);
    return low|((uint64_t)high<<32);
}
#else
#undef FIVMR_CPU_TIMESTAMP_IS_FAST
static inline uint64_t fivmr_readCPUTimestamp(void) {
    return fivmr_curTime();
}
#endif

#ifdef FIVMR_CPU_TIMESTAMP_IS_FAST
static inline bool fivmr_cpuTimestampIsFast(void) { return true; }
#else
static inline bool fivmr_cpuTimestampIsFast(void) { return false; }
#endif

/* pause support */

#ifdef HAVE_X86_PAUSE
static inline void fivmr_spin_fast(void) {
    /* indicate that we're spinning due to spurious CAS failure. */
    __asm__ ( "pause\n" );
}
#else
static inline void fivmr_spin_fast(void) {
    /* nothing we can do on this platform */
}
#endif

/* some utilities */

void fivmr_yield(void);

/* indicate that we're spinning due to contention. */
static inline void fivmr_spin_slow(void) {
    fivmr_yield();
}

#define FIVMR_SPIN_NONE    0
#define FIVMR_SPIN_FAST    1
#define FIVMR_SPIN_SLOW    2

static inline void fivmr_spin(int mode) {
    switch (mode) {
    case FIVMR_SPIN_NONE:
	break;
    case FIVMR_SPIN_FAST:
	fivmr_spin_fast();
	break;
    case FIVMR_SPIN_SLOW:
	fivmr_spin_slow();
	break;
    default: fivmr_assert(false);
    }
}

/* CAS and fence support */

static inline void fivmr_cas_void(uintptr_t *ptr,
				  uintptr_t comparand,
				  uintptr_t newValue) FORCE_INLINE_ATTR;
static inline uintptr_t fivmr_cas_load(uintptr_t *ptr,
				       uintptr_t comparand,
				       uintptr_t newValue) FORCE_INLINE_ATTR;
static inline bool fivmr_cas(uintptr_t *ptr,
			     uintptr_t comparand,
			     uintptr_t newValue) FORCE_INLINE_ATTR;
static inline bool fivmr_cas_weak(uintptr_t *ptr,
				  uintptr_t comparand,
				  uintptr_t newValue) FORCE_INLINE_ATTR;
static inline uintptr_t fivmr_xchg_add(uintptr_t *ptr,
				       uintptr_t delta) FORCE_INLINE_ATTR;
static inline void fivmr_cas32_void(int32_t *ptr,
				    int32_t comparand,
				    int32_t newValue) FORCE_INLINE_ATTR;
static inline int32_t fivmr_cas32_load(int32_t *ptr,
				       int32_t comparand,
				       int32_t newValue) FORCE_INLINE_ATTR;
static inline bool fivmr_cas32(int32_t *ptr,
			       int32_t comparand,
			       int32_t newValue) FORCE_INLINE_ATTR;
static inline bool fivmr_cas32_weak(int32_t *ptr,
				    int32_t comparand,
				    int32_t newValue) FORCE_INLINE_ATTR;
static inline int32_t fivmr_xchg_add32(int32_t *ptr,
				       int32_t delta) FORCE_INLINE_ATTR;
static inline void fivmr_compilerFence(void) FORCE_INLINE_ATTR;
static inline void fivmr_fence(void) FORCE_INLINE_ATTR;

#if defined(FIVMR_SYNC_GCC_INTRINSIC)
#include "fivmr_gcc_intrinsic_sync.h"
#elif defined(FIVMR_SYNC_X86_GCC)
#include "fivmr_x86_sync.h"
#elif defined(FIVMR_SYNC_PPC_GCC)
#include "fivmr_ppc_sync.h"
#elif defined(FIVMR_SYNC_RTEMS)

#  define fivmr_interrupt_level rtems_interrupt_level
#  define fivmr_interrupt_disable rtems_interrupt_disable
#  define fivmr_interrupt_enable rtems_interrupt_enable
#  include "fivmr_nointr_sync.h"

#else
#error "Unsupported OS/architecture (currently we only support x86 or pcc on Posix platforms; on RTEMS we support everything)"
#endif

/* assertion CAS - if assertions enabled, use CAS otherwise use store */
static inline void fivmr_assert_cas(uintptr_t *ptr,
				    uintptr_t comparand,
				    uintptr_t newValue) {
    if (FIVMR_ASSERTS_ON) {
	fivmr_assert(fivmr_cas(ptr,comparand,newValue));
    } else {
	*ptr=newValue;
    }
}

static inline void fivmr_assert_cas32(int32_t *ptr,
				      int32_t comparand,
				      int32_t newValue) {
    if (FIVMR_ASSERTS_ON) {
	fivmr_assert(fivmr_cas32(ptr,comparand,newValue));
    } else {
	*ptr=newValue;
    }
}

/* weak cas on bytes (and booleans) */
static inline bool fivmr_cas8_weak(int8_t *ptr,
                                   int8_t comparand,
                                   int8_t newValue) {
    uintptr_t bytePtr;
    uintptr_t intPtr;
    uintptr_t shift;
    uintptr_t mask;
    int32_t oldVal;

    bytePtr=(uintptr_t)ptr;
    intPtr=bytePtr&~3;

    fivmr_assert((intPtr&3)==0);
    fivmr_assert(bytePtr-intPtr<=3);

    shift=(bytePtr-intPtr)*8;
    if (FIVMR_IS_BIG_ENDIAN) {
        shift=24-shift;
    }
    mask=0xff<<shift;
    oldVal=*((int32_t*)intPtr);

    /* FIXME this might be broken, sign-wise */
    if ((uint8_t)(uint32_t)((oldVal>>shift)&0xff)==(uint8_t)comparand) {
        int32_t newVal=(oldVal&~mask)|(((int32_t)(uint32_t)(uint8_t)newValue)<<shift);
        if (fivmr_cas32_weak((int32_t*)intPtr,
                             oldVal,
                             newVal)) {
            /* printf("bytePtr=%d, intPtr=%d, shift=%d, oldVal=%d, newVal=%d, *ptr = %d, *intPtr=%d\n",
               bytePtr,intPtr,shift,oldVal,newVal,*ptr,*(int32_t*)intPtr); */
            return true;
        }
    }
    return false;
}

static inline bool fivmr_cas8(int8_t *ptr,
                              int8_t comparand,
                              int8_t newValue) {
    /* could be optimized, but I don't care */
    for (;;) {
        int8_t oldVal=*(volatile int8_t*)ptr;
        if (oldVal==comparand) {
            if (fivmr_cas8_weak(ptr,
                                comparand,
                                newValue)) {
                return true;
            }
        } else {
            /* this is a great way of avoiding memory model weirdness.  it's
               very conservative, not meant to be fast.  basically this ensures
               that you get exactly the behavior of a strong Intel-style CAS on
               failure.  in particular it ensures that if 'false' is returned
               by this function, then there was a moment in time, whose temporal
               semantics are exactly determined by the semantics of a successful
               CAS on the given platform, when the pointed-to value did not
               equal the comparand.  on most platforms a similar effect could
               be achieved with a fence - but the problem is that many different
               kinds of fences exist, and this ensures that you just get exactly
               the fencing style of CAS. */
            if (fivmr_cas8_weak(ptr,
                                oldVal,
                                oldVal)) {
                return false;
            }
        }
        fivmr_spin_fast();
        fivmr_compilerFence();
    }
}

static inline void fivmr_assert_cas8(int8_t *ptr,
                                     int8_t comparand,
                                     int8_t newValue) {
    if (FIVMR_ASSERTS_ON) {
        fivmr_assert(fivmr_cas8(ptr,comparand,newValue));
    } else {
        *ptr=newValue;
    }
}

/* threading and locking. */

/*
 * For RTEMS:
 *
 * fivmr_ThreadHandle should be a pointer to a data structure that contains
 * the actual thread handle and a notification semaphore.
 *
 * The thread handle can be either a pthread_t or an _Object_id, though
 * _Object_id would probably be better; we could also have both.  Note that
 * _Object_id is (I think) the same as rtems_id.
 *
 * For fivmr_ThreadHandle_current(), we should use a thread-local variable.
 * If the thread doesn't have a ThreadHandle, we create it.  This should
 * be the only mechanism for creating ThreadHandles.
 */

/* Notes about semaphores:
 *
 * We have two types of semaphores (CritSemaphore and Semaphore).  CritSemaphore
 * is possibly heavier but can be up'd from within a signal handler or an
 * interrupt, while Semaphore is possibly lighter but gives no such guarantee.
 * On some systems, the two semaphore implementations will be identical.
 *
 * Semaphore semantics:
 * - CritSemaphore can be up'd from interrupt/signal handlers but can only be
 *   down'd from threads.
 * - Semaphore can only be up'd and/or down'd from threads.
 * - You can have an unlimited number of Semaphores.
 * - You can only have a limited number of CritSemaphores (i.e. no more than 10
 *   would be good)
 * - Semaphore overflow is mostly undefined.  The implementation tries to
 *   "ignore" overflows - i.e. if the count is 1 bagdzillion, and you up it,
 *   then the count will still be 1 badzillion (as opposed to 1 badzillion plus 1).
 *   But if the VM commits suicide on overflow then it's not my fault, promise.
 */

#if FIVMR_RTEMS
extern uintptr_t fivmr_RTEMS_threadStackSize;
#endif
#if FIVMR_POSIX
extern uintptr_t fivmr_POSIX_threadStackSize;
#endif

struct fivmr_Lock_s;
struct fivmr_CritSemaphore_s;
struct fivmr_Semaphore_s;
struct fivmr_SpinLock_s;
struct fivmr_JmpBuf_s;

typedef struct fivmr_Lock_s fivmr_Lock;
typedef struct fivmr_CritSemaphore_s fivmr_CritSemaphore;
typedef struct fivmr_Semaphore_s fivmr_Semaphore;
typedef struct fivmr_SpinLock_s fivmr_SpinLock;
typedef struct fivmr_JmpBuf_s fivmr_JmpBuf;

#if FIVMR_RTEMS
typedef int fivmr_CPUSet;
#elif FIVMR_POSIX
#  ifdef HAVE_PTHREAD_SETAFFINITY_NP
typedef cpu_set_t fivmr_CPUSet;
#  else
typedef int fivmr_CPUSet;
#  endif
#endif

#if FIVMR_RTEMS
struct fivmr_ThreadCondNode_s;
typedef struct fivmr_ThreadCondNode_s fivmr_ThreadCondNode;
struct fivmr_ThreadCondNode_s{
    rtems_id sema;
    fivmr_ThreadCondNode *next;
    fivmr_Lock *queuedOn;
};

struct fivmr_RTEMS_ConditionVar_s{
    fivmr_ThreadCondNode *head;
};
typedef struct fivmr_RTEMS_ConditionVar_s fivmr_RTEMS_ConditionVar;
#endif

#if FIVMR_POSIX
typedef pthread_t fivmr_ThreadHandle;
#elif FIVMR_RTEMS
typedef rtems_id fivmr_ThreadHandle;
#define FIVMR_TH_INTERRUPT ((fivmr_ThreadHandle)-1)
#elif FIVMR_WIN32
typedef HANDLE fivmr_ThreadHandle;
#endif

#if FIVMR_POSIX
struct fivmr_ThreadSpecific_s {
    pthread_key_t key;
};
typedef struct fivmr_ThreadSpecific_s fivmr_ThreadSpecific;
#else
#define FIVMR_MAX_THREAD_SPECIFICS 256
#define FIVMR_TS_NOTE 0

struct fivmr_ThreadSpecificGlobalData_s {
    void (*destructor)(uintptr_t arg);
    uintptr_t interruptValue;
};

typedef struct fivmr_ThreadSpecificGlobalData_s fivmr_ThreadSpecificGlobalData;

extern fivmr_ThreadSpecificGlobalData fivmr_tsData[FIVMR_MAX_THREAD_SPECIFICS];
extern int32_t fivmr_tsFree[FIVMR_MAX_THREAD_SPECIFICS];
extern int32_t fivmr_numTSFree;
extern rtems_id fivmr_tsSema;

typedef int32_t fivmr_ThreadSpecific;
#endif


/* priority is defined as growing up (smaller values mean lower priority) */
typedef int32_t fivmr_Priority;

/* Thread Priorities are composed of a 16-bit scheduler identifier in
   the upper 16 bits, and a 16-bit scheduler priority field in the lower
   16 bits.

   Schedulers are allocated as follows:

   0x0000       Java priorities (they are just another way of expressing
                                 non-realtime priorities)
   0x0001       Non-realtime priorities (Normal)
   0x0002       Round-robin priority queue scheduled realtime threads
   0x0003       FIFO priority queue scheduled realtime threads
   0x0004
     :          Allocated by the platform-specific runtime
   0x7FFD
   0x7FFE       Allocated to the fivm runtime
   0x7FFF       Invalid priority
   0x8000
     :          Reserved (unused)
   0xFFFF

   Within platform-defined scheduler identifiers, the range of
   scheduler-specific priorities is up to the platform glue, but smaller
   integer values must indicate lower priorities.

    3 3                           1 1
    1 0                           6 5                              0
   ------------------------------------------------------------------
   |0|        Scheduler identifier |    Scheduler-specific priority |
   ------------------------------------------------------------------
 */
typedef int32_t fivmr_ThreadPriority;

/* priority constants */
extern fivmr_Priority FIVMR_PR_NONE;
extern fivmr_Priority FIVMR_PR_INHERIT;
extern fivmr_Priority FIVMR_PR_MIN;
extern fivmr_Priority FIVMR_PR_MAX; /* max scheduled priority */
extern fivmr_Priority FIVMR_PR_CRITICAL; /* indicates interrupt priority or
                                            max priority, if there is no such
                                            thing as interrupt priority */

#if FIVMR_RTEMS
#define FIVMR_PR_SUPPORTED ((bool)true)
#else
extern bool FIVMR_PR_SUPPORTED;
#endif

#define FIVMR_TPR_JAVA         ((int32_t)0x00000000)
#define FIVMR_TPR_NORMAL       ((int32_t)0x00010000)
#define FIVMR_TPR_RR           ((int32_t)0x00020000)
#define FIVMR_TPR_FIFO         ((int32_t)0x00030000)
#define FIVMR_TPR_FIVM         ((int32_t)0x7ffe0000)
#define FIVMR_TPR_INVALID      ((int32_t)0x7fff0000)

#define FIVMR_TPR_SCHEDULER(tp) ((tp) & 0x7fff0000)
#define FIVMR_TPR_PRIORITY(tp) ((tp) & 0xffff)

#define FIVMR_THREADPRIORITY(sched, prio) ((sched) | (fivmr_ThreadPriority)(prio))

#define FIVMR_TPR_JAVA_MIN FIVMR_THREADPRIORITY(FIVMR_TPR_JAVA,((int32_t)1))
#define FIVMR_TPR_JAVA_MAX FIVMR_THREADPRIORITY(FIVMR_TPR_JAVA,((int32_t)10))
extern fivmr_ThreadPriority FIVMR_TPR_NORMAL_MIN;
extern fivmr_ThreadPriority FIVMR_TPR_NORMAL_MAX;
extern fivmr_ThreadPriority FIVMR_TPR_FIFO_MIN;
extern fivmr_ThreadPriority FIVMR_TPR_FIFO_MAX;
extern fivmr_ThreadPriority FIVMR_TPR_RR_MIN;
extern fivmr_ThreadPriority FIVMR_TPR_RR_MAX;

#define FIVMR_TPR_MIN FIVMR_TPR_NORMAL_MIN
#define FIVMR_TPR_MAX FIVMR_TPR_FIFO_MAX

#define FIVMR_TPR_CRITICAL     (FIVMR_THREADPRIORITY(FIVMR_TPR_FIVM,2))

static inline int32_t fivmr_ThreadPriority_scheduler(fivmr_ThreadPriority pr) {
    return FIVMR_TPR_SCHEDULER(pr);
}

static inline int32_t fivmr_ThreadPriority_priority(fivmr_ThreadPriority pr) {
    return FIVMR_TPR_PRIORITY(pr);
}

static inline fivmr_ThreadPriority
fivmr_ThreadPriority_withPriority(fivmr_ThreadPriority pr,
                                  int32_t priority) {
    fivmr_assert(priority>=0 && priority<=0xffff);
    return FIVMR_THREADPRIORITY(FIVMR_TPR_SCHEDULER(pr),
                                priority);
}

const char *fivmr_ThreadPriority_schedulerName(fivmr_ThreadPriority pr);

void fivmr_ThreadPriority_describe(fivmr_ThreadPriority pr,
                                   char *buf,
                                   int32_t len);

/* constructs a minimum thread priority for the given textual description
   of a scheduler.  the char* is advanced past the end of the scheduler
   description.  may return 0 (FIVMR_TPR_INVALID) if parsing was not
   successful.  Note that this will not recognize "FIVM" schedulers. */
fivmr_ThreadPriority fivmr_ThreadPriority_parseScheduler(char const **sched);

/* parse the thread priority.  this assumes that the entire string is
   the thread priority.  the syntax is <sched><prio> where <sched> is
   one of Normal, RR, or FIFO, and <prio> is an integer in the
   appropriate range.  if the string fails to follow this syntax in any
   way, this function will return FIVMR_TPR_INVALID. */
fivmr_ThreadPriority fivmr_ThreadPriority_parse(const char *prstr);

int32_t fivmr_ThreadPriority_minPriority(fivmr_ThreadPriority pr);
int32_t fivmr_ThreadPriority_maxPriority(fivmr_ThreadPriority pr);

#if FIVMR_POSIX
/* portable implementation of recursive mutex on top of pthreads.
   note that some implementations of pthreads don't correctly
   implement pthread_cond_wait() for recursive mutexes, so we
   avoid using pthread recursive mutexes entirely by implementing
   recursion ourselves. */
struct fivmr_Lock_s {
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    fivmr_ThreadHandle holder;
    int32_t recCount;
    fivmr_Priority priority;
};

#elif FIVMR_RTEMS
/* use RTEMS binary semas for the mutex
   use FIVMR to create a linked list of RTEMS specific data for the
   condition variable, the rest remains the same as for POSIX */
struct fivmr_Lock_s {
    rtems_id mutex;
    fivmr_RTEMS_ConditionVar cond;
    fivmr_ThreadHandle holder;
    int32_t recCount;
    fivmr_Priority priority;
    rtems_interrupt_level level;
    rtems_mode mode;
};
#endif

struct fivmr_CritSemaphore_s {
#if FIVMR_POSIX
#  ifdef HAVE_WORKING_SEM_INIT
    sem_t sema;
#  else
    int pipe[2];
#  endif
#elif FIVMR_RTEMS
    rtems_id sema;
#else
#error "Don't know how to make semaphores"
#endif
};

struct fivmr_Semaphore_s {
#if FIVMR_POSIX && !defined(HAVE_WORKING_SEM_INIT)
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    int count;
#else
    fivmr_CritSemaphore sema;
#endif
};

struct fivmr_SpinLock_s {
    int8_t spinword;
};

#if FIVMR_POSIX
struct fivmr_SuspendableThread_s;
typedef struct fivmr_SuspendableThread_s *fivmr_SuspendableThreadHandle;

struct fivmr_SuspendableThread_s {
    uintptr_t magic;
    fivmr_ThreadHandle thread;
    fivmr_Semaphore suspended;
    fivmr_Semaphore wakeUp;
};

typedef struct fivmr_SuspendableThread_s fivmr_SuspensionData;
#elif FIVMR_RTEMS
typedef fivmr_ThreadHandle fivmr_SuspendableThreadHandle;

struct fivmr_SuspensionData_s {
    char ignored; /* some C compilers might get confused if we start having
                     pointers to structs that are empty.  or something. */
};

typedef struct fivmr_SuspensionData_s fivmr_SuspensionData;
#else
#error "Don't know how to suspend and resume threads"
#endif

#if FIVMR_POSIX
extern bool fivmr_fakeRTPriorities;
#elif FIVMR_RTEMS
#define fivmr_fakeRTPriorities ((bool)false)
#endif

fivmr_ThreadPriority
fivmr_ThreadPriority_withScheduler(fivmr_ThreadPriority pr,
                                   fivmr_ThreadPriority sched);

fivmr_ThreadPriority fivmr_ThreadPriority_fromPriority(fivmr_ThreadPriority sched,
                                                       fivmr_Priority prio);

fivmr_Priority fivmr_ThreadPriority_asPriority(fivmr_ThreadPriority prio);

fivmr_Priority fivmr_Priority_bound(fivmr_Priority prio,
                                    fivmr_ThreadPriority maxPrio);

fivmr_ThreadPriority
fivmr_ThreadPriority_canonicalize(fivmr_ThreadPriority pr);

bool fivmr_ThreadPriority_eq(fivmr_ThreadPriority pr1,
                             fivmr_ThreadPriority pr2);

fivmr_ThreadPriority
fivmr_ThreadPriority_canonicalizeRT(fivmr_ThreadPriority pr);

bool fivmr_ThreadPriority_eqRT(fivmr_ThreadPriority pr1,
                               fivmr_ThreadPriority pr2);

bool fivmr_ThreadPriority_ltRT(fivmr_ThreadPriority pr1,
                               fivmr_ThreadPriority pr2);

static inline bool fivmr_ThreadPriority_gtRT(fivmr_ThreadPriority pr1,
                                             fivmr_ThreadPriority pr2) {
    return fivmr_ThreadPriority_ltRT(pr2,pr1);
}

static inline bool fivmr_ThreadPriority_leRT(fivmr_ThreadPriority pr1,
                                             fivmr_ThreadPriority pr2) {
    return !fivmr_ThreadPriority_gtRT(pr1,pr2);
}

static inline bool fivmr_ThreadPriority_geRT(fivmr_ThreadPriority pr1,
                                             fivmr_ThreadPriority pr2) {
    return !fivmr_ThreadPriority_ltRT(pr1,pr2);
}

/* determines the "maximum" of two priorities.  this is a bit more
   subtle than it seems.  In particular, assuming pr1 and pr2 are
   canoncalized (this function will canonicalize them for you if
   they aren't already), the results will be, wlog:
   MAX(NormalX,NormalY)   = NormalMAX(X,Y)
   MAX(NormalX,RRY)       = RRY
   MAX(NormalX,FIFOY)     = FIFOY
   MAX(RRX,FIFOY)         = FIFOMAX(X,Y)
   MAX(RRX,RRY)           = RRMAX(X,Y)
   MAX(FIFOX,FIFOY)       = FIFOMAX(X,Y) */
fivmr_ThreadPriority fivmr_ThreadPriority_max(fivmr_ThreadPriority pr1,
                                              fivmr_ThreadPriority pr2);

/* determines the "minimum" of two priorities.  this is a bit more
   subtle than it seems.  In particular, assuming pr1 and pr2 are
   canoncalized (this function will canonicalize them for you if
   they aren't already), the results will be, wlog:
   MAX(NormalX,NormalY)   = NormalMIN(X,Y)
   MAX(NormalX,RRY)       = NormalX
   MAX(NormalX,FIFOY)     = NormalX
   MAX(RRX,FIFOY)         = RRMIN(X,Y)
   MAX(RRX,RRY)           = RRMIN(X,Y)
   MAX(FIFOX,FIFOY)       = FIFOMIN(X,Y) */
fivmr_ThreadPriority fivmr_ThreadPriority_min(fivmr_ThreadPriority pr1,
                                              fivmr_ThreadPriority pr2);

bool fivmr_ThreadPriority_isRT(fivmr_ThreadPriority pr);

#if FIVMR_POSIX
static inline fivmr_ThreadPriority posix_to_fivmr_ThreadPriority(int policy, int priority) {
    fivmr_ThreadPriority tp;
    switch (policy) {
    case SCHED_OTHER:
        tp = FIVMR_TPR_NORMAL;
        break;
    case SCHED_FIFO:
        tp = FIVMR_TPR_FIFO;
        break;
    case SCHED_RR:
        tp = FIVMR_TPR_RR;
        break;
#ifdef SCHED_BATCH
    case SCHED_BATCH: return FIVMR_TPR_MIN;
#endif
#ifdef SCHED_IDLE
    case SCHED_IDLE: return FIVMR_TPR_MIN;
#endif
    default:
        fivmr_abort("Unknown POSIX scheduling mechanism");
        return tp = 0; /* make the compiler calm down */
    }
    return tp | priority;
}

static inline void fivmr_ThreadPriority_to_posix(fivmr_ThreadPriority fp,
                                                 int *policy,
                                                 int *pp) {
    int32_t scheduler;
    fp=fivmr_ThreadPriority_canonicalize(fp);
    scheduler=FIVMR_TPR_SCHEDULER(fp);
    if (scheduler==FIVMR_TPR_NORMAL) {
        *policy=SCHED_OTHER;
    } else if (scheduler==FIVMR_TPR_FIFO) {
        *policy=SCHED_FIFO;
    } else if (scheduler==FIVMR_TPR_RR) {
        *policy=SCHED_RR;
    } else {
        *policy = SCHED_OTHER; /* Eliminate "policy may be unused" error */
        fivmr_abortf("Invalid FIVMR_TPR_SCHEDULER: %d",fp);
    }

    *pp=FIVMR_TPR_PRIORITY(fp);
}

#elif FIVMR_RTEMS



/* rtems priorities are inverted, we need to account for that later 
   and should be done by 256 - FIVMR_PR when calling anything RTEMS related*/
static inline rtems_task_priority fivmr_Priority_toRTEMS(fivmr_Priority prio){
    rtems_task_priority result;
    result = 257 - prio;
    fivmr_assert(result>=RTEMS_MINIMUM_PRIORITY);
    fivmr_assert(result<=RTEMS_MAXIMUM_PRIORITY-1u);
    return result;
}

static inline fivmr_ThreadPriority
fivmr_ThreadPriority_fromRTEMS(rtems_task_priority rprio,
                               rtems_mode rmode) {
    if ((rmode&RTEMS_PREEMPT_MASK)==RTEMS_PREEMPT) {
        if ((rmode&RTEMS_TIMESLICE_MASK)==RTEMS_TIMESLICE) {
            if (rprio==254) {
                return FIVMR_TPR_NORMAL_MAX;
            } else if (rprio<=253 &&
                       rprio>=2) {
                return FIVMR_THREADPRIORITY(FIVMR_TPR_RR,255-rprio);
            }
        } else {
            if (rprio<=253 &&
                rprio>=2) {
                return FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO,255-rprio);
            }
        }
    }
    return FIVMR_TPR_INVALID;
}

/* FIVMR_TPR_NORMAL priorities map to timesliced priority 254.
 * FIVMR_TPR_RR and FIVMR_TPR_FIFO priorities map to timesliced and non-
 * timesliced priorities, respectively, from 253 to 2.
 */
static inline void fivmr_ThreadPriority_toRTEMS(fivmr_ThreadPriority prio,
                                                rtems_task_priority *rprio,
                                                rtems_mode *rmode){
    int32_t scheduler;
    rtems_task_priority curprio = 0;
    rtems_mode curmode = 0;
    prio = fivmr_ThreadPriority_canonicalize(prio);
    scheduler = FIVMR_TPR_SCHEDULER(prio);
    if (scheduler == FIVMR_TPR_NORMAL) {
        /* These assertions aren't really required, but will catch errors
         * which might trigger on other platforms. */
        fivmr_assert(prio>=FIVMR_TPR_NORMAL_MIN);
        fivmr_assert(prio<=FIVMR_TPR_NORMAL_MAX);
        *rprio = 254;
        *rmode = RTEMS_TIMESLICE|RTEMS_PREEMPT;
    } else if (scheduler == FIVMR_TPR_RR) {
        fivmr_assert(prio>=FIVMR_TPR_RR_MIN);
        fivmr_assert(prio<=FIVMR_TPR_RR_MAX);
        *rprio = 255 - FIVMR_TPR_PRIORITY(prio);
        *rmode = RTEMS_TIMESLICE|RTEMS_PREEMPT;
    } else if (scheduler == FIVMR_TPR_FIFO) {
        fivmr_assert(prio>=FIVMR_TPR_FIFO_MIN);
        fivmr_assert(prio<=FIVMR_TPR_FIFO_MAX);
        *rprio = 255 - FIVMR_TPR_PRIORITY(prio);
        *rmode = RTEMS_NO_TIMESLICE|RTEMS_PREEMPT;
    } else {
        /* Squash "unused variable" errors */
        *rprio = 255;
        *rmode = 0;
        fivmr_abort("Unknown FIVMR_TPR_SCHEDULER");
    }
    fivmr_assert(*rprio>=RTEMS_MINIMUM_PRIORITY);
    fivmr_assert(*rprio<=RTEMS_MAXIMUM_PRIORITY);
}

#endif


#if FIVMR_RTEMS
extern fivmr_ThreadSpecific fivmr_RTEMS_lockData;
extern int32_t fivmr_nextSemaName;
extern int32_t fivmr_nextThreadName;
extern rtems_interrupt_level fivmr_interruptISRLevel;
#endif

#if FIVMR_POSIX
extern int32_t fivmr_Thread_affinity;
#endif

#if FIVMR_POSIX && defined(HAVE_PTHREAD_SETAFFINITY_NP)
#define FIVMR_CAN_SET_AFFINITY 1
#else
#define FIVMR_CAN_SET_AFFINITY 0
#endif


#if FIVMR_POSIX
static inline fivmr_ThreadHandle fivmr_ThreadHandle_current(void) {
    return pthread_self();
}

static inline fivmr_ThreadHandle fivmr_ThreadHandle_zero(void) {
    return (pthread_t)0;
}

static inline bool fivmr_ThreadHandle_eq(fivmr_ThreadHandle a,
					 fivmr_ThreadHandle b) {
    return pthread_equal(a,b);
}

static inline bool fivmr_ThreadHandle_isInterrupt(fivmr_ThreadHandle th) {
    return false;
}
#elif FIVMR_RTEMS
static inline fivmr_ThreadHandle fivmr_ThreadHandle_current(void) {
    if (rtems_interrupt_is_in_progress()) {
	return FIVMR_TH_INTERRUPT;
    } else {
	fivmr_ThreadHandle result=rtems_task_self();
	fivmr_assert(result!=FIVMR_TH_INTERRUPT);
	return result;
    }
}

static inline fivmr_ThreadHandle fivmr_ThreadHandle_zero(void) {
    return 0;
}

static inline bool fivmr_ThreadHandle_eq(fivmr_ThreadHandle a,
					 fivmr_ThreadHandle b) {
    return a==b;
}

static inline bool fivmr_ThreadHandle_isInterrupt(fivmr_ThreadHandle th) {
    return th==FIVMR_TH_INTERRUPT;
}
#endif

#if FIVMR_RTEMS
static inline void fivmr_Thread_setPriority(fivmr_ThreadHandle h,
                                            fivmr_ThreadPriority p) {
    rtems_status_code status;
    rtems_task_priority oldPrio, newPrio=RTEMS_NO_PRIORITY;
    rtems_mode rmode;
    if (LOGGING(2)) {
        char buf[32];
        fivmr_ThreadPriority_describe(p,buf,sizeof(buf));
        LOG(2,("Changing thread %p's priority to %s",h,buf));
    }
    fivmr_assert(h!=FIVMR_TH_INTERRUPT);
    fivmr_ThreadPriority_toRTEMS(p, &newPrio, &rmode);
    status=rtems_task_set_priority(h,newPrio,&oldPrio);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
    /* FIXME: need to also change the budget algorithm!  otherwise we'll get bizarro
       priority inversion effects. */
}

#else
void fivmr_Thread_setPriority(fivmr_ThreadHandle h,
                              fivmr_ThreadPriority p);
#endif

fivmr_ThreadPriority fivmr_Thread_getPriority(fivmr_ThreadHandle h);

#if FIVMR_RTEMS || !defined(HAVE_PTHREAD_SETAFFINITY_NP)
static inline void fivmr_CPUSet_clear(fivmr_CPUSet *cpuset) {
    /* do nothing */
}

static inline void fivmr_CPUSet_set(fivmr_CPUSet *cpuset,
                                    int32_t cpu) {
    /* do nothing */
}

static inline bool fivmr_Thread_setAffinity(fivmr_ThreadHandle h,
                                            fivmr_CPUSet *cpuset) {
    return false;
}
#else
static inline void fivmr_CPUSet_clear(fivmr_CPUSet *cpuset) {
    CPU_ZERO(cpuset);
}

static inline void fivmr_CPUSet_set(fivmr_CPUSet *cpuset,
                                    int32_t cpu) {
    CPU_SET(cpu,cpuset);
}

bool fivmr_Thread_setAffinity(fivmr_ThreadHandle h,
                              fivmr_CPUSet *cpuset);
#endif

/* Does the current thread represent an interrupt handler? */
static inline bool fivmr_Thread_isInterrupt(void) {
#if FIVMR_RTEMS
    return rtems_interrupt_is_in_progress();
#else
    return false;
#endif
}

/* Is the current thread running as if in an interrupt handler?  This is true
   if the thread is either an interrupt thread, or if it's a normal thread, but
   has disabled interrupts. */
static inline bool fivmr_Thread_isCritical(void) {
#if FIVMR_RTEMS
    /* FIXME: this needs to be adapted to various BSPs ... _ISR_Get_level may not
       work the same on all of them. */
    return rtems_interrupt_is_in_progress() || _ISR_Get_level()==fivmr_interruptISRLevel;
#else
    return false;
#endif
}

#if FIVMR_POSIX
static inline void
fivmr_ThreadSpecific_initWithDestructor(fivmr_ThreadSpecific *ts,
					void (*dest)(uintptr_t value)) {
    pthread_key_create(&ts->key,(void (*)(void*))dest);
}

static inline void fivmr_ThreadSpecific_destroy(fivmr_ThreadSpecific *ts) {
    pthread_key_delete(ts->key);
}

static inline uintptr_t fivmr_ThreadSpecific_get(fivmr_ThreadSpecific *ts) {
    return (uintptr_t)pthread_getspecific(ts->key);
}

static inline void fivmr_ThreadSpecific_set(fivmr_ThreadSpecific *ts,
					    uintptr_t value) {
    pthread_setspecific(ts->key,(void*)value);
}

static inline void fivmr_ThreadSpecific_setForThread(fivmr_ThreadSpecific *ts,
						     fivmr_ThreadHandle th,
						     uintptr_t value) {
    fivmr_assert(fivmr_ThreadHandle_current()==th);
    fivmr_ThreadSpecific_set(ts,value);
}
#elif FIVMR_RTEMS
void fivmr_ThreadSpecific_initWithDestructor(fivmr_ThreadSpecific *ts,
                                             void (*dest)(uintptr_t value));

void fivmr_ThreadSpecific_destroy(fivmr_ThreadSpecific *ts);

uintptr_t fivmr_ThreadSpecific_get(fivmr_ThreadSpecific *ts);

void fivmr_ThreadSpecific_set(fivmr_ThreadSpecific *ts,
                              uintptr_t value);

void fivmr_ThreadSpecific_setForThread(fivmr_ThreadSpecific *ts,
                                       fivmr_ThreadHandle th,
                                       uintptr_t value);
#endif

static inline void fivmr_ThreadSpecific_init(fivmr_ThreadSpecific *ts) {
    fivmr_ThreadSpecific_initWithDestructor(ts,NULL);
}

void fivmr_Thread_init(void);

/* do magic to the current thread to make it the way we want it.  it's
   safe to call this more than once, but it may be expensive.  note that
   fivmr_ThreadState_set() automagically calls this.  it's highly
   recommended that you call this in any C thread before calling into
   other fivmr functions. */
fivmr_ThreadHandle fivmr_Thread_integrate(void);

/* get the stack height of all threads created by fivmr_Thread_spawn */
static inline uintptr_t fivmr_Thread_stackHeight(void) {
#if FIVMR_RTEMS
    return fivmr_RTEMS_threadStackSize;
#endif
#if FIVMR_POSIX
    return fivmr_POSIX_threadStackSize;
#endif
}

/* spawns an unattached thread. */
/* FIXME: we shouldn't be using unattached threads since we want to know when
   the threads die. */
fivmr_ThreadHandle fivmr_Thread_spawn(void (*threadMain)(void *arg),
				      void *arg,
				      fivmr_ThreadPriority priority);

void fivmr_Thread_exit(void);

bool fivmr_Thread_canExit(void);
void fivmr_Thread_disableExit(void);

/* FIXME: consider having ratemon objects like in rtems... */

/* cancel any previously started period on the current thread. */
void fivmr_Thread_cancelPeriod(void);

/* wait for the previously started period to end and then start a new one of the given
   duration.  if there was no previous period, returns immediately.  if the previous
   period had timed out, returns false; otherwise returns true. */
bool fivmr_Thread_startPeriod(fivmr_Nanos time);

/* If you ever plan on doing thread suspension, call this exactly once before using
   the next three functions. */
void fivmr_initSuspensionManager(void);

/* If you want to make a thread suspendable, call this from within that thread and
   pass a suspension data.  Make sure that nobody will call suspend after the
   data is deallocated. */
fivmr_SuspendableThreadHandle fivmr_Thread_makeSuspendable(fivmr_SuspensionData *data);

/* Suspend a thread.  Threads cannot suspend themselves.  Must be called from a
   different thread. */
void fivmr_Thread_suspend(fivmr_SuspendableThreadHandle th);

/* Resume a thread.  Must be called from the same thread that called suspend. */
void fivmr_Thread_resume(fivmr_SuspendableThreadHandle th);

static inline fivmr_SuspendableThreadHandle fivmr_SuspendableThreadHandle_zero() {
    return (fivmr_SuspendableThreadHandle)0;
}

static inline bool
fivmr_SuspendableThreadHandle_eq(fivmr_SuspendableThreadHandle a,
                                 fivmr_SuspendableThreadHandle b) {
    return a==b;
}

/* Initialize a recursive lock and condition variable pair.  The lock will
   behave as a priority-ceiling lock on platforms that support the notion.
   Note that there is a special priority supported by the lock -
   FIVMR_PR_CRITICAL.  On RT platforms which allow us to register interrupt
   handlers, holding a CRITICAL lock also disables interrupts.  Note that
   a CRITICAL lock held from within a non-interrupt thread can be waited
   on and notified on, while an interrupt thread may only notify but not
   wait. */
void fivmr_Lock_init(fivmr_Lock *lock,
		     fivmr_Priority priority);

void fivmr_Lock_destroy(fivmr_Lock *lock);

/* tells you if your thread's priority is low enough to acquire this lock. */
bool fivmr_Lock_legalToAcquire(fivmr_Lock *lock);

void fivmr_Lock_lock(fivmr_Lock *lock);
void fivmr_Lock_unlock(fivmr_Lock *lock);

void fivmr_Lock_broadcast(fivmr_Lock *lock);

void fivmr_Lock_lockedBroadcast(fivmr_Lock *lock);

void fivmr_Lock_wait(fivmr_Lock *lock);
void fivmr_Lock_timedWaitAbs(fivmr_Lock *lock,
			     fivmr_Nanos whenAwake);
void fivmr_Lock_timedWaitRel(fivmr_Lock *lock,
			     fivmr_Nanos howLong);

void fivmr_CritSemaphore_init(fivmr_CritSemaphore *sema);
void fivmr_CritSemaphore_up(fivmr_CritSemaphore *sema);
void fivmr_CritSemaphore_down(fivmr_CritSemaphore *sema);
/* FIXME: need a timedDownAbs operation */
void fivmr_CritSemaphore_destroy(fivmr_CritSemaphore *sema);

#if FIVMR_POSIX && !defined(HAVE_WORKING_SEM_INIT)
void fivmr_Semaphore_init(fivmr_Semaphore *sema);
void fivmr_Semaphore_up(fivmr_Semaphore *sema);
void fivmr_Semaphore_down(fivmr_Semaphore *sema);
/* FIXME: need a timedDownAbs operation */
void fivmr_Semaphore_destroy(fivmr_Semaphore *sema);
#else
static inline void fivmr_Semaphore_init(fivmr_Semaphore *sema) {
    fivmr_CritSemaphore_init(&sema->sema);
}
static inline void fivmr_Semaphore_up(fivmr_Semaphore *sema) {
    fivmr_CritSemaphore_up(&sema->sema);
}
static inline void fivmr_Semaphore_down(fivmr_Semaphore *sema) {
    fivmr_CritSemaphore_down(&sema->sema);
}
/* FIXME: need a timedDownAbs operation */
static inline void fivmr_Semaphore_destroy(fivmr_Semaphore *sema) {
    fivmr_CritSemaphore_destroy(&sema->sema);
}
#endif

static inline void fivmr_SpinLock_init(fivmr_SpinLock *spinlock) {
    spinlock->spinword=(int8_t)0;
}

static inline void fivmr_SpinLock_destroy(fivmr_SpinLock *spinlock) {
    fivmr_assert(spinlock->spinword==0);
}

static inline bool fivmr_SpinLock_tryLock(fivmr_SpinLock *spinlock) {
    /* in this case we might as well give it an honest try and use a
       strong cas ... may want to revisit this decision in the future. */
    if (fivmr_cas8(&spinlock->spinword,0,1)) {
        fivmr_assert(spinlock->spinword==1);
        return true;
    } else {
        return false;
    }
}

/* this is a "hard" lock assertion - even with RT_ASSERTIONS off we still
   do the assertion. */
static inline void fivmr_SpinLock_assertLock(fivmr_SpinLock *spinlock) {
    if (!fivmr_SpinLock_tryLock(spinlock)) {
        fivmr_abortf("Could not acquire spinlock at %p even though it should "
                     "have been available.",spinlock);
    }
}

static inline void fivmr_SpinLock_lock(fivmr_SpinLock *spinlock) {
    for (;;) {
	if (fivmr_cas8_weak(&spinlock->spinword,0,1)) {
	    break;
	}
        fivmr_spin_slow();
    }
    fivmr_assert(spinlock->spinword==1);
}

static inline void fivmr_SpinLock_unlock(fivmr_SpinLock *spinlock) {
    fivmr_fence();
    fivmr_assert_cas8(&spinlock->spinword,1,0);
}

#if FIVMR_DYN_LOADING
/* dynamic libraries */

typedef void *fivmr_ModuleHandle;
extern fivmr_Lock fivmr_Module_lock;

void fivmr_Module_init(void);

static inline fivmr_ModuleHandle fivmr_Module_zero(void) {
    return NULL;
}

fivmr_ModuleHandle fivmr_Module_load(const char *path);

/* true on success, false otherwise */
bool fivmr_Module_unload(fivmr_ModuleHandle handle);

void *fivmr_Module_lookup(fivmr_ModuleHandle handle,
                          const char *name);

/* get a function given its underscore-free name */
void *fivmr_Module_getFunction(fivmr_ModuleHandle handle,
			       const char *funcName);

/* returns thread-owned string describing the last error, or some
   undefined string if there was none. */
const char *fivmr_Module_getLastError(void);
#endif

/* system-dependent JNI stuff */

static inline const char *fivmr_JNI_libPrefix(void) {
#ifdef FIVMSYS_JNIPRE
    return FIVMSYS_JNIPRE;
#else
    return NULL;
#endif
}

static inline const char *fivmr_JNI_libSuffix(void) {
#ifdef FIVMSYS_JNISUF
    return FIVMSYS_JNISUF;
#else
    return NULL;
#endif
}

/* description of the system.  note that these do NOT have to be static inlines,
   and do NOT have to return string constants.  however, the strings they return
   are guaranteed to be used only until the next call to sysdep functions, so
   it's safe to just have a thread-local buffer for this stuff.  in reality,
   though, these functions are currently only being called during bootstrapping,
   so a global variable will do. */

const char *fivmr_OS_name(void);
const char *fivmr_OS_arch(void);

static inline const char *fivmr_OS_version(void) {
    return "unknown";
}

static inline bool fivmr_isBigEndian(void) {
    /* awesome. */
    /* FIXME: just use an ifdef... */
    union {
	int16_t s;
	int8_t b[2];
    } u;
    u.s=(int16_t)1;
    return u.b[1];
}

/* some really stupid utilities */

int32_t fivmr_readByte(int fd);
int32_t fivmr_writeByte(int fd,int8_t b);

/* why does this exist? because on some platforms
   sizeof(int)!=sizeof(int32_t) */
static inline int32_t fivmr_pipe(int32_t *array) {
    int pipes[2];
    int res;
    res=pipe(pipes);
    if (res<0) {
	return res;
    } else {
	array[0]=pipes[0];
	array[1]=pipes[1];
        return 0;
    }
}

static inline int64_t fivmr_fcntl(int32_t fd,int32_t cmd,int64_t optArg1) {
#if defined(HAVE_FCNTL_H) && defined(HAVE_FCNTL)
    switch (cmd) {
    case F_GETFL:
	return (int64_t)fcntl(fd,cmd);
    case F_SETFL:
	return (int64_t)fcntl(fd,cmd,(long)optArg1);
    default: fivmr_assert(false);
    }
    return -1;
#else
    errno=ENOTSUP;
    return -1;
#endif
}

static inline int32_t fivmr_ioctl_ptr(int32_t fd,int32_t cmd,void *arg) {
#if defined(HAVE_SYS_IOCTL_H) && defined(HAVE_IOCTL)
    return ioctl(fd,cmd,arg);
#else
    errno=ENOTSUP;
    return -1;
#endif
}

static inline int32_t fivmr_ioctl_void(int32_t fd,int32_t cmd) {
#if defined(HAVE_SYS_IOCTL_H) && defined(HAVE_IOCTL)
    return ioctl(fd,cmd);
#else
    errno=ENOTSUP;
    return -1;
#endif
}

static inline int32_t fivmr_ioctl_int(int32_t fd,int32_t cmd,int32_t val) {
#if defined(HAVE_SYS_IOCTL_H) && defined(HAVE_IOCTL)
    return ioctl(fd,cmd,(int)val);
#else
    errno=ENOTSUP;
    return -1;
#endif
}

#ifdef HAVE_BIND
#define fivmr_bind(fd,addr,addrLen) \
    bind((fd),(const struct sockaddr*)(addr),(addrLen))
#else
#define fivmr_bind(fd,addr,addrLen) (errno=ENOTSUP, -1)
#endif

#if defined(FD_CLR) || defined(HAVE_FD_CLR)
#define fivmr_FD_CLR(fd,fdset) \
    FD_CLR((int)(fd),(fd_set*)(fdset))
#endif

#if defined(FD_ISSET) || defined(HAVE_FD_ISSET)
#define fivmr_FD_ISSET(fd,fdset) \
    FD_ISSET((int)(fd),(fd_set*)(fdset))
#endif

#if defined(FD_SET) || defined(HAVE_FD_SET)
#define fivmr_FD_SET(fd,fdset) \
    FD_SET((int)(fd),(fd_set*)(fdset))
#endif

#if defined(FD_ZERO) || defined(HAVE_FD_ZERO)
#define fivmr_FD_ZERO(fdset) \
    FD_ZERO((fd_set*)(fdset))
#endif

/* jumping */

struct fivmr_JmpBuf_s {
    jmp_buf env;
};

#define fivmr_JmpBuf_label(buf)                 \
    ((int32_t)setjmp((buf)->env))

#define fivmr_JmpBuf_jump(buf,value)            \
    (longjmp(buf->env,(int)value))

/* and some other stuff... */

void fivmr_preinitThreadStringBufFor(fivmr_ThreadHandle th);
char *fivmr_threadStringBuf(size_t size);
const char *fivmr_getThreadStringBuf(void);
char *fivmr_tsprintf(const char *msg,...);

double fivmr_parseDouble(const char *cstr);

int32_t fivmr_availableProcessors(void);

/* global sysdep functions */

void fivmr_SysDepUtil_init(void); /* internal function */

void fivmr_SysDep_init(void);

#endif

