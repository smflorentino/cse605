/*
 * fivmr.h -- main Fiji VM header; include this in any code module that wants to
 *            use internal Fiji VM functionality.
 * Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
 * This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
 * LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
 * available at fivm/LEGAL and can also be found at
 * http://www.fiji-systems.com/FPL3.txt
 * 
 * By installing, reproducing, distributing, and/or using the FIJI VM Software
 * you agree to the termfs of the FIJI PUBLIC LICENSE.  You may exercise the
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

/*
 * DO NOT attempt to split up this file in a well-intentioned attempt to "clean
 * things up".  The inline functions and types defined here have subtle dependency
 * chains and are defined in a very precise order that should not be broken.
 */

#ifndef FP_FIVMR_H
#define FP_FIVMR_H

#include <fivmr_sysdep.h>
#include <fivmr_util.h>
#include <fivmr_region.h>
#include <fivmr_rbtree.h>
#include <fivmr_settings_getters.h>

#if FIVMR_POSIX
#define FIVMR_CAN_HANDLE_SIGQUIT 1
#else
#define FIVMR_CAN_HANDLE_SIGQUIT 0
#endif

#if FIVMR_INTERNAL_INST
#include <fivmr_intinst.h>
#endif

/* runtime profiling */
#if FIVMR_PROFILE_MONITOR_HEAVY
extern uintptr_t fivmr_SPC_lock;
extern uintptr_t fivmr_SPC_unlock;

static inline void fivmr_SPC_incLock(void) { fivmr_SPC_lock++; }
static inline void fivmr_SPC_incUnlock(void) { fivmr_SPC_unlock++; }
#else
#define fivmr_SPC_incLock() do {} while(0)
#define fivmr_SPC_incUnlock() do {} while(0)
#endif

#if FIVMR_PROFILE_MONITOR
extern uintptr_t fivmr_SPC_lockSlow;
extern uintptr_t fivmr_SPC_inflate;
extern uintptr_t fivmr_SPC_unlockSlow;
extern uintptr_t fivmr_SPC_lockSlowNotHeld;
extern uintptr_t fivmr_SPC_lockSlowRecurse;
extern uintptr_t fivmr_SPC_lockSlowInflate;
extern uintptr_t fivmr_SPC_lockSlowSpin;
extern uintptr_t fivmr_SPC_lockSlowQueue;

static inline void fivmr_SPC_incLockSlow(void) { fivmr_SPC_lockSlow++; }
static inline void fivmr_SPC_incInflate(void) { fivmr_SPC_inflate++; }
static inline void fivmr_SPC_incUnlockSlow(void) { fivmr_SPC_unlockSlow++; }
static inline void fivmr_SPC_incLockSlowNotHeld(void) { fivmr_SPC_lockSlowNotHeld++; }
static inline void fivmr_SPC_incLockSlowRecurse(void) { fivmr_SPC_lockSlowRecurse++; }
static inline void fivmr_SPC_incLockSlowInflate(void) { fivmr_SPC_lockSlowInflate++; }
static inline void fivmr_SPC_incLockSlowSpin(void) { fivmr_SPC_lockSlowSpin++; }
static inline void fivmr_SPC_incLockSlowQueue(void) { fivmr_SPC_lockSlowQueue++; }
#else
#define fivmr_SPC_incLockSlow() do {} while(0)
#define fivmr_SPC_incInflate() do {} while(0)
#define fivmr_SPC_incUnlockSlow() do {} while(0)
#define fivmr_SPC_incLockSlowNotHeld() do {} while(0)
#define fivmr_SPC_incLockSlowRecurse() do {} while(0)
#define fivmr_SPC_incLockSlowInflate() do {} while(0)
#define fivmr_SPC_incLockSlowSpin() do {} while(0)
#define fivmr_SPC_incLockSlowQueue() do {} while(0)
#endif

#if FIVMR_PROFILE_GC_HEAVY
extern uintptr_t fivmr_SPC_barrierFastPath;
extern uintptr_t fivmr_SPC_alloc;

static inline void fivmr_SPC_incBarrierFastPath(void) { fivmr_SPC_barrierFastPath++; }
static inline void fivmr_SPC_incAlloc(void) { fivmr_SPC_alloc++; }
#else
#define fivmr_SPC_incBarrierFastPath() do {} while(0)
#define fivmr_SPC_incAlloc() do {} while(0)
#endif

#if FIVMR_PROFILE_GC
extern uintptr_t fivmr_SPC_barrierSlowPath;
extern uintptr_t fivmr_SPC_allocSlowPath;

static inline void fivmr_SPC_incBarrierSlowPath(void) { fivmr_SPC_barrierSlowPath++; }
static inline void fivmr_SPC_incAllocSlowPath(void) { fivmr_SPC_allocSlowPath++; }
#else
#define fivmr_SPC_incBarrierSlowPath() do {} while(0)
#define fivmr_SPC_incAllocSlowPath() do {} while(0)
#endif

extern bool fivmr_logReflect;

#if FIVMR_PROFILE_REFLECTION
extern fivmr_Nanos fivmr_PR_invokeTime;
extern fivmr_Nanos fivmr_PR_initTime;
#endif

/* begin runtime constants... */

#define FIVMR_RANDMT_N ((uint32_t)624)
#define FIVMR_RANDMT_M ((uint32_t)397)
#define FIVMR_RANDMT_MATRIX_A ((uint32_t)0x9908b0dfUL)   /* constant vector a */
#define FIVMR_RANDMT_UPPER_MASK ((uint32_t)0x80000000UL) /* most significant w-r bits */
#define FIVMR_RANDMT_LOWER_MASK ((uint32_t)0x7fffffffUL) /* least significant r bits */

#define FIVMR_TS_MAX_ROOTS 3
#define FIVMR_TS_STATE_BUF_LEN 64

/* this enables 4 bits per page */
#define FIVMR_LOG_PT_BITS_PER_PAGE 2

#define FIVMR_PT_BITS_PER_PAGE (1<<FIVMR_LOG_PT_BITS_PER_PAGE)

#if FIVMSYS_PTRSIZE==4
/* for 32-bit systems, use a two-level layout (spine + chunks) where the
   chunks are one page each for easy allocation. */

/* internal constant - number of bits of an address that can be used to
   offset into a chunk. */
#define FIVMR_PT_ADDRBITS_PER_CHUNK                                     \
    (fivmr_min((FIVMSYS_LOG_PAGE_SIZE+3-FIVMR_LOG_PT_BITS_PER_PAGE),    \
               32-FIVMSYS_LOG_PAGE_SIZE))

#define FIVMR_PT_ADDRBITS_PER_SPINE \
    (32-FIVMSYS_LOG_PAGE_SIZE-FIVMR_PT_ADDRBITS_PER_CHUNK)

#define FIVMR_PT_NUM_SPINE_ELE \
    (1<<FIVMR_PT_ADDRBITS_PER_SPINE)

/* useful constants */
#define FIVMR_PT_SPINE_SHIFT \
    ((FIVMSYS_LOG_PAGE_SIZE+FIVMR_PT_ADDRBITS_PER_CHUNK)&31)

#define FIVMR_PT_SPINE_MASK \
    ((FIVMR_PT_NUM_SPINE_ELE-1)<<FIVMR_PT_SPINE_SHIFT)

#define FIVMR_PT_CHUNK_SHIFT \
    (FIVMSYS_LOG_PAGE_SIZE)

#define FIVMR_PT_CHUNK_MASK \
    (((1<<FIVMR_PT_ADDRBITS_PER_CHUNK)-1)<<FIVMR_PT_CHUNK_SHIFT)

#elif FIVMSYS_PTRSIZE==8
/* for 64-bit systems, use a three-level layout (spine + chunk spines +
   chunks).  the sizes of the levels are optimized in mathematica for
   the case where you only have one chunk, and one chunk spine.  It
   will probably continue to be optimal (or very near optimal) so long
   as the number of chunk spines (known below as "middle") stays very
   close to 1, for any number of chunks. */

#define FIVMR_PT_OUTER_BITS \
    ((58 + FIVMR_LOG_PT_BITS_PER_PAGE - FIVMSYS_LOG_PAGE_SIZE)/3)

#define FIVMR_PT_MIDDLE_BITS \
    ((58 + FIVMR_LOG_PT_BITS_PER_PAGE - FIVMSYS_LOG_PAGE_SIZE + 2)/3)

#define FIVMR_PT_INNER_BITS \
    (64 - FIVMSYS_LOG_PAGE_SIZE - FIVMR_PT_OUTER_BITS - FIVMR_PT_MIDDLE_BITS)

#define FIVMR_PT_ADDRBITS_PER_CHUNK \
    (FIVMR_PT_INNER_BITS)

#define FIVMR_PT_NUM_SPINE_ELE \
    (((uintptr_t)1)<<FIVMR_PT_OUTER_BITS)

#define FIVMR_PT_NUM_MIDDLE_ELE \
    (((uintptr_t)1)<<FIVMR_PT_MIDDLE_BITS)

#define FIVMR_PT_NUM_INNER_BYTES \
    ((((uintptr_t)1)<<(FIVMR_PT_INNER_BITS+FIVMR_LOG_PT_BITS_PER_PAGE))/8)

/* useful constants */
#define FIVMR_PT_OUTER_SHIFT \
    (FIVMSYS_LOG_PAGE_SIZE+FIVMR_PT_INNER_BITS+FIVMR_PT_MIDDLE_BITS)

#define FIVMR_PT_OUTER_MASK \
    (((((uintptr_t)1)<<FIVMR_PT_OUTER_BITS)-1)<<FIVMR_PT_OUTER_SHIFT)

#define FIVMR_PT_MIDDLE_SHIFT \
    (FIVMSYS_LOG_PAGE_SIZE+FIVMR_PT_INNER_BITS)

#define FIVMR_PT_MIDDLE_MASK \
    (((((uintptr_t)1)<<FIVMR_PT_MIDDLE_BITS)-1)<<FIVMR_PT_MIDDLE_SHIFT)

#define FIVMR_PT_INNER_SHIFT \
    (FIVMSYS_LOG_PAGE_SIZE)

#define FIVMR_PT_INNER_MASK \
    (((((uintptr_t)1)<<FIVMR_PT_INNER_BITS)-1)<<FIVMR_PT_INNER_SHIFT)

#else
#error "Wrong pointer size"
#endif

#define FIVMR_PT_CHUNK_LENGTH (1<<(FIVMR_PT_ADDRBITS_PER_CHUNK-5+       \
                                   FIVMR_LOG_PT_BITS_PER_PAGE))

#define FIVMR_PT_CHUNK_SIZE (FIVMR_PT_CHUNK_LENGTH*4)

#define FIVMR_VMS_IDLE                   ((uintptr_t)0)
#define FIVMR_VMS_INITING                ((uintptr_t)1)
#define FIVMR_VMS_RUNNING                ((uintptr_t)2)
#define FIVMR_VMS_EXITING                ((uintptr_t)3)

/* possible values of fivmr_ThreadState::execStatus.
   
   State transitions:
   
   CLEAR -> NEW
   Occurs when a thread ID is chosen.  Simply indicates that the thread ID
   has been selected, but does not mean that the thread is active.

   NEW -> STARTING
   For threads started from Java code, this indicates that a native thread
   has been spawned but is not yet running.  This indicates to the VM runtime
   that the thread is active (i.e. has a VMThread reference),
   but also indicates that the thread does not yet have a native thread ID and
   has not yet executed any Java code.  This transition occurs only when
   creating a native thread from Java: we create the VM thread state and
   initialize it first, then start the native thread and attach it.  While
   the native thread has not yet been started or attached, the ThreadState
   is in the "STARTING" state.

   NEW -> IN_NATIVE
   This transition occurs when a native thread is attached to the VM.  The
   thread is already started, and the native code attaches it.  This means
   that the thread by now has a native thread handle.

   STARTING -> IN_NATIVE
   Occurs when a thread started from Java code actually begins running.
   At this point the thread has a native thread handle.

   IN_NATIVE -> IN_NATIVE_TO_BLOCK
   The thread was not running in Java code but was requested to block before
   entering Java code.  Occurs when the GC requests a handshake.

   IN_NATIVE -> IN_JAVA
   The thread has gone from executing native code to executing Java code.
   The transition occurred along the fast path.

   IN_NATIVE_TO_BLOCK -> IN_JAVA
   The thread acknowledged the handshake request as it was entering Java code,
   and then cleared the handshake request and began executing Java.

   IN_JAVA -> IN_JAVA_TO_BLOCK
   The thread is executing Java code but there is a handshake/block request
   pending.  Either the thread will handle the request at the next pollcheck,
   or else the thread will espace into native code.

   IN_JAVA -> IN_NATIVE
   The thread was executing Java code, and exited into native code along the
   fast path.

   IN_JAVA_TO_BLOCK -> IN_NATIVE_TO_BLOCK
   The thread was executing Java code with a handshake request pending, and
   then exited into native code.  Anyone requesting the handshake was notified
   that the thread exited into native (and thus should not be waited on), but
   the thread exited to native anyway, and is now executing native code
   asynchronously to the VM.

   IN_NATIVE -> TERMINATING
   The thread was executing native code but decided to begin termination.
   The thread is still running, but will not run any more Java code before
   terminating.

   TERMINATING -> CLEAR
   The thread died.

   NB. this should be the complete set of transitions; i.e. if any other
   transition occurs it would be a bug.  And not just a "philosophical" bug.
   The VM is allowed to assume that no other transitions are possible; thus
   if some other kind of transition ever happens it could corrupt the VM's
   state. */
#define FIVMR_TSES_CLEAR                 ((uintptr_t)0)
#define FIVMR_TSES_NEW                   ((uintptr_t)1)
#define FIVMR_TSES_STARTING              ((uintptr_t)2) /* thread is asynchronously being started, but is not yet running. */
#define FIVMR_TSES_IN_JAVA               ((uintptr_t)3)
#define FIVMR_TSES_IN_JAVA_TO_BLOCK      ((uintptr_t)4)
#define FIVMR_TSES_IN_NATIVE             ((uintptr_t)5)
#define FIVMR_TSES_IN_NATIVE_TO_BLOCK    ((uintptr_t)6)
#define FIVMR_TSES_TERMINATING           ((uintptr_t)7) /* the thread will terminate before reaching another safepoint */

/* flags of fivmr_ThreadState::execFlags */
#define FIVMR_TSEF_BLOCKING              ((uintptr_t)1)
#define FIVMR_TSEF_WAITING               ((uintptr_t)2)
#define FIVMR_TSEF_TIMED                 ((uintptr_t)4)

/* soft handshake support in execFlags */
#define FIVMR_TSEF_SOFT_HANDSHAKE        ((uintptr_t)8) /* soft handshake requested */

/* debug/profile stack trace support */
#define FIVMR_TSEF_TRACE_STACK           ((uintptr_t)32)
#define FIVMR_TSEF_DUMP_STACK            ((uintptr_t)64)

/* unbiasing support */
#define FIVMR_TSEF_UNBIAS                ((uintptr_t)128)

/* still have 256 for additional handshake bits */

#define FIVMR_TSEF_GC_MISC               ((uintptr_t)512)
#define FIVMR_TSEF_SCAN_THREAD_ROOTS     ((uintptr_t)1024)
#define FIVMR_TSEF_COMMIT_DESTRUCTORS    ((uintptr_t)2048)
#define FIVMR_TSEF_RELINQUISH_SPACE      ((uintptr_t)4096)

/* GC has bits 512..4096 for soft handshake requests */
#define FIVMR_TSEF_SF_GC_REQ_MASK	 ((uintptr_t)(512+1024+2048+4096))

/* unused: 8192, 16384 */

#define FIVMR_TSEF_PUSH_TYPE_EPOCH       ((uintptr_t)32768)

#define FIVMR_TSEF_SF_REQ_MASK			\
    (FIVMR_TSEF_SF_GC_REQ_MASK|			\
     FIVMR_TSEF_TRACE_STACK|                    \
     FIVMR_TSEF_DUMP_STACK|                     \
     FIVMR_TSEF_PUSH_TYPE_EPOCH)

/* thread mode flags */
#define FIVMR_TSEF_JAVA                  ((uintptr_t)65536)
#define FIVMR_TSEF_HANDSHAKEABLE         ((uintptr_t)131072)
#define FIVMR_TSEF_JAVA_HANDSHAKEABLE \
    (FIVMR_TSEF_JAVA|FIVMR_TSEF_HANDSHAKEABLE)

#define FIVMR_TSEF_FINALIZED             ((uintptr_t)262144)

/* GC blocking and request support in GCData.flags */
#define FIVMR_GCDF_REQUESTED_GC          ((uintptr_t)32768)

/* states for a monitor - note that this gets duplicated to some extent in
   Java-land. */
#define FIVMR_MS_QUEUED                  ((int32_t)1)  /* flag bit: one or more
						  	  threads are waiting to
							  enter.  note that this
                                                          bit may not be modified
                                                          unless the entering
                                                          queue is locked. */
#define FIVMR_MS_RT_QUEUED               ((int32_t)2)  /* flag bit: prevents
                                                          barging in on the
                                                          lock; everyone must
                                                          enter queue.  set if
                                                          one or more of the enqueued
                                                          threads has RT priority. */

/* reserve some bits (4, 8, 16) for future features */

#define FIVMR_MS_UNBIASED                ((int32_t)32) /* lock is unbiased.  if this
                                                          bit is not set, then the
                                                          lock is biased to whatever
                                                          thread is in the TID, and
                                                          RC==0 means the lock is not
                                                          held (otherwise RC==0 with
                                                          non-zero TID means that the
                                                          RC is 1) */

/* fourth bit (value = 8) can be used for PIP/PCEP locking.  we may need
   additional bits, though. */

#define FIVMR_MS_RC_SHIFT                ((int32_t)6)  /* amount to shift to get
							  the rec count */
#define FIVMR_MS_TID_SHIFT               ((int32_t)16) /* amount to shift to get
							  the thread ID */

/* rec count mask */
#define FIVMR_MS_RC_MASK						\
    (((((fivmr_MonState)1)<<(FIVMR_MS_TID_SHIFT-FIVMR_MS_RC_SHIFT))-1)  \
     <<FIVMR_MS_RC_SHIFT)

/* thread id mask */
#define FIVMR_MS_TID_MASK						\
    (((((fivmr_MonState)1)<<(sizeof(uintptr_t)*8-FIVMR_MS_TID_SHIFT))-1) \
     <<FIVMR_MS_TID_SHIFT)

/* the value of the monitor state when the lock is biased but to nobody,
   nobody is queued, the queues are not in use, and nobody holds the lock.
   this is the default state of a lock when biased locking is enabled. */
#define FIVMR_MS_CLEAR                   ((fivmr_MonState)0)

/* the value of the monitor state when there is nobody queued, the queues
   are not in use, and nobody holds the lock.  this is the default state
   of a lock when biased locking is not enabled. */
#define FIVMR_MS_NOT_HELD	         FIVMR_MS_UNBIASED

/* the value of the monitor state for a TypeData.  it indicates that the
   lock is held by an invalid thread, but is unbiased. */
#define FIVMR_MS_INVALID			\
    ((((fivmr_MonState)1)<<FIVMR_MS_TID_SHIFT)|FIVMR_MS_UNBIASED)

/* TypeAux flags */
#define FIVMR_TAF_TRACED                 ((int32_t)1)

/* MachineCode flags */
#define FIVMR_MC_KIND                    ((int32_t)31)
#define FIVMR_MC_BASELINE                ((int32_t)0)
#define FIVMR_MC_JNI_TRAMPOLINE          ((int32_t)1)
#define FIVMR_MC_CLONE_HELPER            ((int32_t)2)
#define FIVMR_MC_EXC_THROW               ((int32_t)3)
#define FIVMR_MC_OPT1                    ((int32_t)6)
#define FIVMR_MC_LOAD_THUNK              ((int32_t)8)
#define FIVMR_MC_BASE_PATCH              ((int32_t)9)
#define FIVMR_MC_FIELD_ACCESS            ((int32_t)10)
#define FIVMR_MC_METHOD_CALL             ((int32_t)11)
#define FIVMR_MC_INTERFACE_RES           ((int32_t)12)
#define FIVMR_MC_EXC_THROW_SUB           ((int32_t)13)
#define FIVMR_MC_ARRAY_ALLOC             ((int32_t)14)
#define FIVMR_MC_OBJECT_ALLOC            ((int32_t)15)
#define FIVMR_MC_INSTANCEOF              ((int32_t)16)

#define FIVMR_MC_POSSIBLE_ENTRYPOINT     ((int32_t)256)

#define FIVMR_MC_GC_OWNED                ((int32_t)1024)   /* is the machine code
                                                              owned by the GC? */
#define FIVMR_MC_GC_MARKED               ((int32_t)2048)

/* Frame type */
#define FIVMR_FT_HENDERSON               ((int32_t)0)
#define FIVMR_FT_BASELINE                ((int32_t)1)

/* VM flags */
#define FIVMR_VMF_USED_TIDS_MALLOCED     ((int32_t)1)
#define FIVMR_VMF_ITABLE_OCC_MALLOCED    ((int32_t)2)

#define FIVMR_VMF_LOG_THROW              ((int32_t)4)
#define FIVMR_VMF_JNI_COVERAGE           ((int32_t)8)
#define FIVMR_VMF_RUN_PROFILER           ((int32_t)16)
#define FIVMR_VMF_LOG_MACHINE_CODE       ((int32_t)32)
#define FIVMR_VMF_FORCE_JIT_SLOWPATH     ((int32_t)64)
#define FIVMR_VMF_VERBOSE_EXCEPTIONS     ((int32_t)128)

/* binding flags */
#define FIVMR_BF_CLEAR                   ((int32_t)0)
#define FIVMR_BF_STATIC                  ((int32_t)1)
#define FIVMR_BF_VISIBILITY              ((int32_t)6)
#define FIVMR_BF_PRIVATE                 ((int32_t)0)
#define FIVMR_BF_PACKAGE                 ((int32_t)2)
#define FIVMR_BF_PROTECTED               ((int32_t)4)
#define FIVMR_BF_PUBLIC                  ((int32_t)6)

#define FIVMR_FBF_FINAL                  ((int32_t)8)
#define FIVMR_FBF_VOLATILE               ((int32_t)16)
#define FIVMR_FBF_TRANSIENT              ((int32_t)32)
#define FIVMR_FBF_UNTRACED               ((int32_t)64)
#define FIVMR_FBF_NOT_A_REFERENCE        ((int32_t)128)

#define FIVMR_MBF_METHOD_KIND            ((int32_t)24)
#define FIVMR_MBF_FINAL                  ((int32_t)0)
#define FIVMR_MBF_VIRTUAL                ((int32_t)8)
#define FIVMR_MBF_ABSTRACT               ((int32_t)16)

#define FIVMR_MBF_SYNCHRONIZED           ((int32_t)32)

#define FIVMR_MBF_METHOD_IMPL            ((int32_t)448)
#define FIVMR_MBF_STUB                   ((int32_t)0)
#define FIVMR_MBF_BYTECODE               ((int32_t)64)
#define FIVMR_MBF_JNI                    ((int32_t)128)
#define FIVMR_MBF_INTRINSIC              ((int32_t)192)
#define FIVMR_MBF_IMPORT                 ((int32_t)256)
#define FIVMR_MBF_UNSUPPORTED            ((int32_t)320)
#define FIVMR_MBF_SYNTHETIC              ((int32_t)384)

#define FIVMR_MBF_HAS_CODE               ((int32_t)512)

#define FIVMR_MBF_COV_CALLED             ((int32_t)1024) /* code coverage support,
							    off by default */

#define FIVMR_MBF_ALLOC_AS_CALLER        ((int32_t)2048)

#define FIVMR_MBF_EXISTS                 ((int32_t)4096)

#define FIVMR_MBF_DYNAMIC                ((int32_t)8192) /* is this method's code
                                                            the result of JIT? */

#define FIVMR_MBF_RT_EXC_THROWER         ((int32_t)16384)

#define FIVMR_MBF_COOKIE                 ((int32_t)1073741824) /* distinguishes this from
                                                                  a MachineCode */

#define FIVMR_TBF_TYPE_KIND              ((int32_t)120)
#define FIVMR_TBF_PRIMITIVE              ((int32_t)0)
#define FIVMR_TBF_ARRAY                  ((int32_t)8)
#define FIVMR_TBF_ANNOTATION             ((int32_t)16)
#define FIVMR_TBF_INTERFACE              ((int32_t)24)
#define FIVMR_TBF_ABSTRACT               ((int32_t)32)
#define FIVMR_TBF_VIRTUAL                ((int32_t)40) /* "virtual" means "not abstract,
                                                          not an interface, and not
                                                          final" - i.e. it's a normal
                                                          class. */
#define FIVMR_TBF_FINAL                  ((int32_t)48)
#define FIVMR_TBF_STUB                   ((int32_t)56) /* this means that this is
                                                          just a type stub */

#define FIVMR_TBF_RESOLUTION_DONE        ((int32_t)128)
#define FIVMR_TBF_RESOLUTION_FAILED      ((int32_t)256)

#define FIVMR_TBF_BUCKETS_MALLOCED_E1    ((int32_t)512)
#define FIVMR_TBF_ITABLE_MALLOCED_E1     ((int32_t)1024)
#define FIVMR_TBF_BUCKETS_MALLOCED_E2    ((int32_t)2048)
#define FIVMR_TBF_ITABLE_MALLOCED_E2     ((int32_t)4096)

#define FIVMR_TBF_DIRECT_SUBS_MALLOCED   ((int32_t)8192)

#define FIVMR_TBF_NEW_SUPER_MODE         ((int32_t)16384)

#define FIVMR_TBF_AOT                    ((int32_t)32768) /* generated by AOT? */

#define FIVMR_TBF_OVERRIDE_ALL           ((int32_t)65536)
#define FIVMR_TBF_OVERRIDE_APP           ((int32_t)131072)

#define FIVMR_TBF_FINALIZABLE            ((int32_t)262144)

#define FIVMR_TBF_SPECIAL_SCAN           ((int32_t)524288)

/* call mode flags */
#define FIVMR_CM_HANDLES                 ((int32_t)1) /* use handles? */
#define FIVMR_CM_EXEC_STATUS             ((int32_t)2) /* change exec status? */
#define FIVMR_CM_DISPATCH                ((int32_t)4) /* do virtual
							 dispatching? */
#define FIVMR_CM_NULLCHECK               ((int32_t)8) /* do null checking on
							 receiver? */
#define FIVMR_CM_CLASSCHANGE             ((int32_t)16) /* do class change
							  check? */
#define FIVMR_CM_CHECKINIT               ((int32_t)32) /* do init checks? */
#define FIVMR_CM_RETURN_ARGS_BUF         ((int32_t)64) /* call returnBuffer on
							  args after parsing
							  them? */
#define FIVMR_CM_WRAP_EXCEPTION          ((int32_t)128) /* wrap exceptions from
							   the method being
							   called in a
							   ReflectiveExc? */

#define FIVMR_DR_FAT                     ((uintptr_t)1)
#define FIVMR_DR_INLINED                 ((uintptr_t)2)

#if FIVMSYS_PTRSIZE==4
#define FIVMR_DR_TRM_NUMBITS             ((uintptr_t)12)
#else
#define FIVMR_DR_TRM_NUMBITS             ((uintptr_t)31)
#endif

#define FIVMR_DR_MFL_PATCH_POINT         ((uintptr_t)1)

#define FIVMR_IMR_INLINED                ((uintptr_t)1)

#define FIVMR_TS_BUF_SIZE                (4096)

#define FIVMR_GC_ALWAYS_MARKED           ((uintptr_t)3)
#define FIVMR_GC_MARKBITS_SHIFT          (FIVMSYS_PTRSIZE*8-2)
#define FIVMR_GC_MARKBITS_MASK           (FIVMR_GC_ALWAYS_MARKED	\
					  <<FIVMR_GC_MARKBITS_SHIFT)

/* GC phases */
#define FIVMR_GCP_IDLE                   ((int32_t)0)
#define FIVMR_GCP_PRE_INIT               ((int32_t)1)
#define FIVMR_GCP_INIT                   ((int32_t)2)
#define FIVMR_GCP_STACK_SCAN             ((int32_t)3)
#define FIVMR_GCP_TRACE                  ((int32_t)4)
#define FIVMR_GCP_SWEEP                  ((int32_t)5)

#define FIVMR_GC_CLEAR                   ((uintptr_t)0)
#define FIVMR_GC_MARK1                   ((uintptr_t)1)
#define FIVMR_GC_MARK2                   ((uintptr_t)2)

#define FIVMR_GC_SH_CLEAR                (FIVMR_GC_CLEAR<<		\
					  FIVMR_GC_MARKBITS_SHIFT)
#define FIVMR_GC_SH_MARK1                (FIVMR_GC_MARK1<<		\
					  FIVMR_GC_MARKBITS_SHIFT)
#define FIVMR_GC_SH_MARK2                (FIVMR_GC_MARK2<<		\
					  FIVMR_GC_MARKBITS_SHIFT)
#define FIVMR_GC_SH_ALWAYS_MARKED        (FIVMR_GC_ALWAYS_MARKED<<	\
					  FIVMR_GC_MARKBITS_SHIFT)

/* page states */
/* note that for populated/shaded pages, and for bumping pages, the page
   has a one-word header with additional state info.  it seems that
   making the populated/shaded distinction in this one word might make
   sense; we don't do it for caching reasons. */
#define FIVMR_GCPS_ZERO                  ((uint8_t)0) /* page is either free
							 or is in use for bump-
							 allocation by some
							 thread.  GC skips over
							 these pages */
#define FIVMR_GCPS_POPULATED             ((uint8_t)1) /* page has objects in
							 it */
#define FIVMR_GCPS_SHADED                ((uint8_t)2) /* page has some shaded
							 objects in it */
#define FIVMR_GCPS_FREE                  ((uint8_t)3) /* page is on a free
							 list.  only used when
                                                         self-managing. */
#define FIVMR_GCPS_RELINQUISHED          ((uint8_t)4) /* page was relinquished
                                                         by allocator during this
                                                         collection */

/* page status bits */
#define FIVMR_GCUPS_ZERO                 ((uint16_t)0) /* page has no free
							  objects */
#define FIVMR_GCUPS_FREE_LINES           ((uint16_t)1) /* page has one or
							  more lines on the
							  line freelist; the
							  rest of the status
							  word tells you the
							  first such line */
#define FIVMR_GCUPS_STAT_MASK            ((uint16_t)1)

#define FIVMR_GC_HEADER_SIZE             ((uintptr_t)sizeof(fivmr_GCHeader))

#define FIVMR_GC_NUM_GC_SPACES           ((int32_t)1)

#define FIVMR_GC_OBJ_SPACE               ((int32_t)0)

#define FIVMR_GC_SA_SPACE                (FIVMR_GC_NUM_GC_SPACES)
#define FIVMR_GC_NUM_SPACES              (FIVMR_GC_NUM_GC_SPACES+(int32_t)1)

/* only needed for fragmented object models */
#define FIVMR_GC_BLOCK_SIZE              ((uintptr_t)(8*sizeof(uintptr_t)))

#define FIVMR_GCM_THIN                   ((uintptr_t)1)

/* object header flags */
#define FIVMR_OHF_ZERO                   ((int32_t)0)

/* field access types */
#define FIVMR_FAT_PUTFIELD               ((int32_t)0)
#define FIVMR_FAT_GETFIELD               ((int32_t)1)
#define FIVMR_FAT_PUTSTATIC              ((int32_t)2)
#define FIVMR_FAT_GETSTATIC              ((int32_t)3)

/* method call types */
#define FIVMR_MCT_INVOKESTATIC           ((int32_t)0)
#define FIVMR_MCT_INVOKEVIRTUAL          ((int32_t)1)
#define FIVMR_MCT_INVOKEINTERFACE        ((int32_t)2)
#define FIVMR_MCT_INVOKESPECIAL          ((int32_t)3)

/* instanceof types */
#define FIVMR_IOT_INSTANCEOF             ((int32_t)0)
#define FIVMR_IOT_CHECKCAST              ((int32_t)1)

/*
 * definitions of offsets:
 *
 * FIVMR_OBJ_GC_OFFSET: offset from the GCHeader field to where an object
 *     pointer points.  i.e. fivmr_Object - FIVMR_OBJ_GC_OFFSET = GCHeader
 *
 * FIVMR_OBJ_TD_OFFSET: offset from the ObjHeader field to where an object
 *     pointer points.
 *
 * FIVMR_OBJ_HF_OFFSET: offset from the fragmented object header to where
 *     an object pointer points.  only valid for fragmented object models.
 *     note that under fragmented object models, the field offset is always
 *     relative to the fragmented object header.
 *
 * FIVMR_ARR_LEN_OFF: offset from where the object pointer points to the
 *     array length field. NOTE: for some object models, this is not the
 *     "complete" array length, i.e. it doesn't tell you the full story.
 *
 * FIVMR_OBJ_PAYLOAD_OFF: offset from where the object pointer points to
 *     the first field (not really useful, because field 'locations'
 *     are always measured from the object pointer, and because in some
 *     object models objects are linked - so this offset is to the first
 *     field but other fields are not found sequentially past this point)
 *
 * FIVMR_ARR_PAYLOAD_OFF(eleSize): offset from where the object pointer points
 *     to the start of the array payload.  NOTE: for some object models,
 *     the array payload is in a linked chunk, so this means nothing.
 *
 * FIVMR_ALLOC_OFFSET: the amount by which the allocation bump pointer is
 *     offset; i.e. when fivmr_allocRaw_slow returns, the start of the
 *     returned chunk lies at result - FIVMR_ALLOC_OFFSET.
 *
 * FIVMR_EXTRA_HEADER: amount of extra header space, on top of GC header
 *     and ObjHeader.
 *
 * FIVMR_SP_FR_OFFSET: offset from the spine forwarding pointer to where
 *     the spine pointer points.  only valid for HFGC.
 *
 * FIVMR_SP_HEADER: spine header size.  only valid for HFGC.
 *
 * FIVMR_MIN_ALLOC_SIZE: minimum size of an object - may be zero to
 *     indicate that it doesn't matter for this object model.
 *
 * FIVMR_OBJ_SIZE_ALIGN: additional alignment required for object sizes
 */

/* constants for contiguous object model */
#define FIVMR_CONT_OBJ_GC_OFFSET              ((uintptr_t)(sizeof(uintptr_t)*2+4))
#define FIVMR_CONT_OBJ_TD_OFFSET              ((uintptr_t)(sizeof(uintptr_t)+4))
#define FIVMR_CONT_ARR_LEN_OFF                ((uintptr_t)-4)
#define FIVMR_CONT_OBJ_PAYLOAD_OFF            ((uintptr_t)-4)
#  if FIVMRSYS_PTRSIZE==4
#define FIVMR_CONT_ARR_PAYLOAD_OFF(eleSize)   ((uintptr_t)0)
#  else
#define FIVMR_CONT_ARR_PAYLOAD_OFF(eleSize)   ((uintptr_t)((eleSize)<=4?0:4))
#  endif
#define FIVMR_CONT_ALLOC_OFFSET               (FIVMR_CONT_OBJ_GC_OFFSET)
#define FIVMR_CONT_EXTRA_HEADER               ((uintptr_t)0)
#define FIVMR_CONT_MIN_ALLOC_SIZE             ((uintptr_t)0)
#define FIVMR_CONT_MIN_OBJ_ALIGN              ((uintptr_t)sizeof(uintptr_t))
#define FIVMR_CONT_OBJ_SIZE_ALIGN             ((uintptr_t)sizeof(uintptr_t))
#define FIVMR_CONT_TOTAL_HEADER_SIZE                                    \
    ((uintptr_t)(sizeof(uintptr_t)*2))

/* constants for fragmented object model */
#define FIVMR_FRAG_OBJ_GC_OFFSET              ((uintptr_t)(-sizeof(uintptr_t)))
#define FIVMR_FRAG_OBJ_TD_OFFSET              ((uintptr_t)(-sizeof(uintptr_t)*2))
#define FIVMR_FRAG_OBJ_FH_OFFSET              ((uintptr_t)0)
#define FIVMR_FRAG_ARR_LEN_OFF                ((uintptr_t)(sizeof(uintptr_t)*3))
#define FIVMR_FRAG_OBJ_PAYLOAD_OFF            ((uintptr_t)(sizeof(uintptr_t)*3))
#define FIVMR_FRAG_ARR_PAYLOAD_OFF(eleSize)                             \
    ((uintptr_t) (fivmr_alignRaw(sizeof(uintptr_t)*3+4,(eleSize))))
#define FIVMR_FRAG_ALLOC_OFFSET               ((uintptr_t)0)
#define FIVMR_FRAG_EXTRA_HEADER               ((uintptr_t)(sizeof(uintptr_t)))
#define FIVMR_FRAG_SP_FR_OFFSET               ((uintptr_t)(sizeof(uintptr_t)*2))
#define FIVMR_FRAG_SP_HEADER                  ((uintptr_t)(sizeof(uintptr_t)*2))
#define FIVMR_FRAG_MAX_INLINE_SPINE                                     \
    ((uintptr_t)((FIVMR_GC_BLOCK_SIZE-sizeof(uintptr_t)*3-4-4)/sizeof(uintptr_t)))
#define FIVMR_FRAG_MIN_ALLOC_SIZE             ((uintptr_t)FIVMR_GC_BLOCK_SIZE)
#define FIVMR_FRAG_MIN_OBJ_ALIGN              ((uintptr_t)FIVMR_GC_BLOCK_SIZE)
#define FIVMR_FRAG_OBJ_SIZE_ALIGN             ((uintptr_t)FIVMR_GC_BLOCK_SIZE)
#define FIVMR_FRAG_TOTAL_HEADER_SIZE            \
    ((uintptr_t)(sizeof(uintptr_t)*3))

/* unified way of getting what you want for any object model; note that (settings)
   will be evaluated multiple times! */
#define FIVMR_OBJ_GC_OFFSET(settings)                          \
    (FIVMR_OM_CONTIGUOUS(settings)?FIVMR_CONT_OBJ_GC_OFFSET:   \
     FIVMR_OM_FRAGMENTED(settings)?FIVMR_FRAG_OBJ_GC_OFFSET:   \
     fivmr_assert(!"bad object model"))
#define FIVMR_OBJ_TD_OFFSET(settings)                           \
    (FIVMR_OM_CONTIGUOUS(settings)?FIVMR_CONT_OBJ_TD_OFFSET:    \
     FIVMR_OM_FRAGMENTED(settings)?FIVMR_FRAG_OBJ_TD_OFFSET:    \
     fivmr_assert(!"bad object model"))
#define FIVMR_ALLOC_OFFSET(settings)                            \
    (FIVMR_OM_CONTIGUOUS(settings)?FIVMR_CONT_ALLOC_OFFSET:     \
     FIVMR_OM_FRAGMENTED(settings)?FIVMR_FRAG_ALLOC_OFFSET:     \
     fivmr_assert(!"bad object model"))
#define FIVMR_EXTRA_HEADER(settings)                            \
    (FIVMR_OM_CONTIGUOUS(settings)?FIVMR_CONT_EXTRA_HEADER:     \
     FIVMR_OM_FRAGMENTED(settings)?FIVMR_FRAG_EXTRA_HEADER:     \
     fivmr_assert(!"bad object model"))
#define FIVMR_MIN_ALLOC_SIZE(settings)                          \
    (FIVMR_OM_CONTIGUOUS(settings)?FIVMR_CONT_MIN_ALLOC_SIZE:   \
     FIVMR_OM_FRAGMENTED(settings)?FIVMR_FRAG_MIN_ALLOC_SIZE:   \
     fivmr_assert(!"bad object model"))
/* alignment of the beginning of objects */
#define FIVMR_MIN_OBJ_ALIGN(settings)                           \
    (FIVMR_OM_CONTIGUOUS(settings)?FIVMR_CONT_MIN_OBJ_ALIGN:    \
     FIVMR_OM_FRAGMENTED(settings)?FIVMR_FRAG_MIN_OBJ_ALIGN:    \
     fivmr_assert(!"bad object model"))
/* alignment of size */
#define FIVMR_OBJ_SIZE_ALIGN(settings)                          \
    (FIVMR_OM_CONTIGUOUS(settings)?FIVMR_CONT_OBJ_SIZE_ALIGN:   \
     FIVMR_OM_FRAGMENTED(settings)?FIVMR_FRAG_OBJ_SIZE_ALIGN:   \
     fivmr_assert(!"bad object model"))
#define FIVMR_RAWTYPE_HEADERSIZE(settings)                      \
    (FIVMR_OM_CONTIGUOUS(settings)?(sizeof(fivmr_GCHeader)):    \
     FIVMR_OM_FRAGMENTED(settings)?(sizeof(fivmr_GCHeader)+     \
         FIVMR_EXTRA_HEADER(settings)):                         \
     fivmr_assert(!"bad object model"))

/* guarantee on alignment of heap-allocated chunk boundaries.  for
   CMR this'll end up being sizeof(uintptr_t), while for HF it'll end
   up being FIVMR_GC_BLOCK_SIZE. */
#define FIVMR_MIN_CHUNK_ALIGN(settings)                                 \
    ((uintptr_t)fivmr_min(FIVMR_MIN_OBJ_ALIGN(settings),                \
                          FIVMR_OBJ_SIZE_ALIGN(settings)))

#define FIVMR_TOTAL_HEADER_SIZE(settings)                               \
    ((uintptr_t)(FIVMR_EXTRA_HEADER(settings)+sizeof(uintptr_t)*2))

#define FIVMR_AE_CAN_FAIL                ((fivmr_AllocEffort)0)
#define FIVMR_AE_MUST_SUCCEED            ((fivmr_AllocEffort)1)

#define FIVMR_PL_IMMORTAL                ((fivmr_PayloadMode)0)
#define FIVMR_PL_IMMORTAL_ONESHOT        ((fivmr_PayloadMode)1)
#define FIVMR_PL_COPY                    ((fivmr_PayloadMode)2)
#define FIVMR_PL_INVALID                 ((fivmr_PayloadMode)3)

/* time for runtime datatypes... */

typedef uintptr_t fivmr_Object; /* direct pointer to an object.  don't use unless
				   you're IN_JAVA and in between safepoints, or
				   else you *really* know what you're doing. */

typedef uintptr_t fivmr_Spine; /* pointer to an HFGC spine.  don't use unless
                                  you're using HFGC and you're IN_JAVA. */

typedef int32_t fivmr_VMFlags;
typedef int32_t fivmr_TypeAuxFlags;
typedef int32_t fivmr_MachineCodeFlags;
typedef int32_t fivmr_FrameType;
typedef int32_t fivmr_BindingFlags;
typedef int32_t fivmr_CallMode;
typedef uintptr_t fivmr_MonState;
typedef int32_t fivmr_GCPhase;
typedef int32_t fivmr_GCSpace;

typedef int32_t fivmr_ObjHeadFlags;

typedef int32_t fivmr_AllocEffort;

typedef int32_t fivmr_PayloadMode;

typedef int32_t fivmr_FieldAccessType;
typedef int32_t fivmr_MethodCallType;
typedef int32_t fivmr_InstanceofType;

struct fivmr_Random_s;
struct fivmr_OTH_s;
struct fivmr_PageTable_s;
struct fivmr_PageChunkDescr_s;
struct fivmr_PageChunkList_s;
struct fivmr_PTIterator_s;
struct fivmr_Monitor_s;
struct fivmr_ObjHeader_s;
struct fivmr_MonQueues_s;
struct fivmr_Boost_s;
struct fivmr_BoostCookie_s;
struct fivmr_BoostedSpinLock_s;
struct fivmr_MonitorData_s;
struct fivmr_ThreadState_s;
struct fivmr_ThreadQueue_s;
struct fivmr_JNIEnv_s;
struct fivmr_JNIVM_s;
struct fivmr_Basepoint_s;
struct fivmr_BaseTryCatch_s;
struct fivmr_MachineCode_s;
struct fivmr_CompactMethodRec_s;
struct fivmr_MethodRec_s;
struct fivmr_FieldRec_s;
struct fivmr_BaseFieldAccess_s;
struct fivmr_BaseMethodCall_s;
struct fivmr_BaseArrayAlloc_s;
struct fivmr_BaseObjectAlloc_s;
struct fivmr_BaseInstanceof_s;
union fivmr_Value_u;
struct fivmr_TypedValue_s;
struct fivmr_FatGCMap_s;
struct fivmr_StaticTypeContext_s;
struct fivmr_TypeContext_s;
struct fivmr_TypeAux_s;
struct fivmr_TypeEpoch_s;
struct fivmr_TypeStub_s;
struct fivmr_TypeData_s;
struct fivmr_TypeDataNode_s;
struct fivmr_Handle_s;
struct fivmr_HandleRegion_s;
struct fivmr_GCHeader_s;
struct fivmr_FreePage_s;
struct fivmr_UsedPage_s;
struct fivmr_GCSpaceData_s;
struct fivmr_FreeLine_s;
struct fivmr_LargeObjectHeader_s;
struct fivmr_SSDescr_s;
struct fivmr_Destructor_s;
struct fivmr_GCSpaceAlloc_s;
struct fivmr_GCData_s;
struct fivmr_ScopeID_s;
struct fivmr_MemoryArea_s;
struct fivmr_ScopeBacking_s;
#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
struct fivmr_JNILib_s;
#endif
struct fivmr_Frame_s;
struct fivmr_NativeFrame_s;
struct fivmr_DebugRec_s;
struct fivmr_FatDebugData_s;
struct fivmr_InlineMethodRec_s;
struct fivmr_Settings_s;
struct fivmr_Configuration_s;
struct fivmr_Payload_s;
struct fivmr_PayloadList_s;
struct fivmr_GC_s;
struct fivmr_VM_s;
struct fivmr_PooledThread_s;
struct fivmr_ThreadPool_s;
struct fivmr_TimeSlice_s;
struct fivmr_TimeSliceManager_s;
#if FIVMR_FLOW_LOGGING
struct fivmr_FlowLogHeader_s;
struct fivmr_FlowLogEvent_s;
#endif
struct fivmr_FlowLogBuffer_s;

typedef struct fivmr_Random_s fivmr_Random;
typedef struct fivmr_OTH_s fivmr_OTH;
typedef struct fivmr_PageTable_s fivmr_PageTable;
typedef struct fivmr_PageChunkDescr_s fivmr_PageChunkDescr;
typedef struct fivmr_PageChunkList_s fivmr_PageChunkList;
typedef struct fivmr_PTIterator_s fivmr_PTIterator;
typedef struct fivmr_GCSpaceAlloc_s fivmr_GCSpaceAlloc;
typedef struct fivmr_GCData_s fivmr_GCData;
typedef struct fivmr_GCHeader_s fivmr_GCHeader;
typedef struct fivmr_ScopeID_s fivmr_ScopeID;
typedef struct fivmr_MemoryArea_s fivmr_MemoryArea;
typedef struct fivmr_MemoryAreaStack_s fivmr_MemoryAreaStack;
typedef struct fivmr_ScopeBacking_s fivmr_ScopeBacking;
typedef struct fivmr_FreePage_s fivmr_FreePage;
typedef struct fivmr_UsedPage_s fivmr_UsedPage;
typedef struct fivmr_GCSpaceData_s fivmr_GCSpaceData;
typedef struct fivmr_FreeLine_s fivmr_FreeLine;
typedef struct fivmr_LargeObjectHeader_s fivmr_LargeObjectHeader;
typedef struct fivmr_SSDescr_s fivmr_SSDescr;
typedef struct fivmr_Destructor_s fivmr_Destructor;
typedef struct fivmr_Monitor_s fivmr_Monitor;
typedef struct fivmr_ObjHeader_s fivmr_ObjHeader;
typedef struct fivmr_MonQueues_s fivmr_MonQueues;
typedef struct fivmr_Boost_s fivmr_Boost;
typedef struct fivmr_BoostCookie_s fivmr_BoostCookie;
typedef struct fivmr_BoostedSpinLock_s fivmr_BoostedSpinLock;
typedef struct fivmr_MonitorData_s fivmr_MonitorData;
typedef struct fivmr_ThreadState_s fivmr_ThreadState;
typedef struct fivmr_ThreadQueue_s fivmr_ThreadQueue;
typedef struct fivmr_JNIEnv_s fivmr_JNIEnv;
typedef struct fivmr_JNIVM_s fivmr_JNIVM;
typedef struct fivmr_Basepoint_s fivmr_Basepoint;
typedef struct fivmr_BaseTryCatch_s fivmr_BaseTryCatch;
typedef struct fivmr_MachineCode_s fivmr_MachineCode;
typedef struct fivmr_CompactMethodRec_s fivmr_CompactMethodRec;
typedef struct fivmr_MethodRec_s fivmr_MethodRec;
typedef struct fivmr_FieldRec_s fivmr_FieldRec;
typedef struct fivmr_BaseFieldAccess_s fivmr_BaseFieldAccess;
typedef struct fivmr_BaseMethodCall_s fivmr_BaseMethodCall;
typedef struct fivmr_BaseArrayAlloc_s fivmr_BaseArrayAlloc;
typedef struct fivmr_BaseObjectAlloc_s fivmr_BaseObjectAlloc;
typedef struct fivmr_BaseInstanceof_s fivmr_BaseInstanceof;
typedef union fivmr_Value_u fivmr_Value;
typedef struct fivmr_TypedValue_s fivmr_TypedValue;
typedef struct fivmr_FatGCMap_s fivmr_FatGCMap;
typedef struct fivmr_StaticTypeContext_s fivmr_StaticTypeContext;
typedef struct fivmr_TypeContext_s fivmr_TypeContext;
typedef struct fivmr_TypeAux_s fivmr_TypeAux;
typedef struct fivmr_TypeEpoch_s fivmr_TypeEpoch;
typedef struct fivmr_TypeStub_s fivmr_TypeStub;
typedef struct fivmr_TypeData_s fivmr_TypeData;
typedef struct fivmr_TypeDataNode_s fivmr_TypeDataNode;
typedef struct fivmr_Handle_s fivmr_Handle;
typedef struct fivmr_HandleRegion_s fivmr_HandleRegion;
#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
typedef struct fivmr_JNILib_s fivmr_JNILib;
#endif
typedef struct fivmr_NativeFrame_s fivmr_NativeFrame;
typedef struct fivmr_Frame_s fivmr_Frame;
typedef struct fivmr_DebugRec_s fivmr_DebugRec;
typedef struct fivmr_FatDebugData_s fivmr_FatDebugData;
typedef struct fivmr_InlineMethodRec_s fivmr_InlineMethodRec;
typedef struct fivmr_Settings_s fivmr_Settings;
typedef struct fivmr_Configuration_s fivmr_Configuration;
typedef struct fivmr_Payload_s fivmr_Payload;
typedef struct fivmr_PayloadList_s fivmr_PayloadList;
typedef struct fivmr_GC_s fivmr_GC;
typedef struct fivmr_VM_s fivmr_VM;
typedef struct fivmr_TimeSlice_s fivmr_TimeSlice;
typedef struct fivmr_TimeSliceManager_s fivmr_TimeSliceManager;
typedef struct fivmr_PooledThread_s fivmr_PooledThread;
typedef struct fivmr_ThreadPool_s fivmr_ThreadPool;
#if FIVMR_FLOW_LOGGING
typedef struct fivmr_FlowLogHeader_s fivmr_FlowLogHeader;
typedef struct fivmr_FlowLogEvent_s fivmr_FlowLogEvent;
#endif
typedef struct fivmr_FlowLogBuffer_s fivmr_FlowLogBuffer;

#if FIVMR_INTERNAL_INST
FIVMR_II_TYPE_DECL_BEFORE
#endif

struct fivmr_Random_s {
    uint32_t mt[FIVMR_RANDMT_N];
    uint32_t mti;
    uint32_t last;
    uint32_t lastLeft;
};

/* OTH = One Time Hashtable; used primarily for doing hash-consing.  intended
   to be as simple and stupid as possible. */
struct fivmr_OTH_s {
    uintptr_t *list;
    uintptr_t n;
};

struct fivmr_PageTable_s {
    bool isFlat;
    union {
        struct {
            uintptr_t start;
            uintptr_t size;
            uint32_t *table;
        } flat;
        struct {
            fivmr_Lock lock;
            /* page table guarantee: additions of *new* pages are done while holding
               the lock.  changes of state to previously existing pages are done
               with the lock potentially not held, but involved a CAS. */
#if FIVMSYS_PTRSIZE==4
            uint32_t *table[FIVMR_PT_NUM_SPINE_ELE];
#else
            uint32_t **table[FIVMR_PT_NUM_SPINE_ELE];
#endif
            fivmr_PageChunkList *chunkListHead;
        } ml; /* ml = multi-level */
    } u;
};

struct fivmr_PageChunkDescr_s {
    uintptr_t baseAddress;
    uint32_t *chunk;
};

#define FIVMR_PAGE_CHUNKS_PER_LIST					\
    ((FIVMR_PAGE_SIZE-2*FIVMSYS_PTRSIZE)/sizeof(fivmr_PageChunkDescr))

struct fivmr_PageChunkList_s {
    /* this struct MUST be page-sized. */
    fivmr_PageChunkList *next;
    uintptr_t numChunks;
    fivmr_PageChunkDescr chunks[FIVMR_PAGE_CHUNKS_PER_LIST];
};

struct fivmr_PTIterator_s {
    fivmr_PageTable *pt;
    union {
        struct {
            bool first;
        } flat;
        struct {
            fivmr_PageChunkList *cur;
            uintptr_t idx;
            uintptr_t limit;
        } ml;
    } u;
    
    /* publicly accessible fields */
    uintptr_t baseAddress;
    uint32_t *chunk;
    uintptr_t chunkLength;
};

struct fivmr_FatGCMap_s {
    int32_t numPtrs;
    int32_t offsets[1]; /* this is a var-length array */
};

/* this is the part of the type context that the AOT compiler may provide.
   for class loading this just represents additional fields of the
   TypeContext */
struct fivmr_StaticTypeContext_s {
    int32_t typeOffset;
    int32_t nTypes;
    int32_t stubOffset;
    int32_t nStubs;
    fivmr_Payload *payload;
};

struct fivmr_JNIVM_s {
    void *functions;
    
    fivmr_TypeContext *ctx;
};

struct fivmr_TypeContext_s {
    /* how should this work?
       1) it should have three maps of classes:
          1.1) one based on a sorted array (for compiler-generated
               contexts and if we wish to do optimizations)
          1.2) one based on a red-black tree.
          1.3) another one based on a red-black tree that caches
               already-known classes.  this will duplicate 1.1 and 1.2.
               since each class (TypeData) may be in multiple caches
               we need to be careful about management of the
               ftree_Node's for this tree.
       2) if a request for a class comes, we should look it up
          first in the cache and then in the other maps.  or
          something like that.
       3) if that request fails, call a user-supplied callback.
       4) cache the result and return it */
    
    fivmr_StaticTypeContext st;

    /* grab *both* locks if you want to add types; types can only be removed
       by blowing away the entire context */
    fivmr_Lock treeLock; /* grab this first; this is the only lock you want if
                            you want to search for types.  use it also for
                            dealing with the TypeAux. */
    
    /* held only to ensure that one class load happens at a time ... FIXME
       this might blow up in our faces if an application starts worker threads
       in loadClass(). */
    fivmr_Lock loadSerializerLock;
    
    /* NOTE: type internals are protected by the global typeDataLock */
    
    ftree_Tree dynamicTypeTree; /* only need treeLock */

    ftree_Tree stubTree; /* the TypeStubs - i.e. loading constraints */
    
    fivmr_Object classLoader;
    
    fivmr_TypeAux *aux;

#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
    fivmr_Lock jniLibLock;
    fivmr_JNILib *jniLibraries;
#endif
    fivmr_JNIVM jniVM;
    
    fivmr_VM *vm;
};

/* convention: this is just the header for some data (i.e. adding sizeof(fivmr_TypeAux)
   bytes takes you to the data), and if the flags indicate that the data is
   traced then the GC will scan it for pointers to the heap.  That'll be useful
   for (a) referenced to classes used by this type and (b) static ref fields */
struct fivmr_TypeAux_s {
    uintptr_t size; /* in bytes */
    uintptr_t occupied; /* in bytes */
    fivmr_TypeAuxFlags flags;
    fivmr_TypeAux *next;
};

struct fivmr_TypeEpoch_s {
    uint16_t itableOff; /* start of itable memory is itable+itableOff */
    uint16_t itableLen; /* end of itable memory is itable+itableOff+itableLen */
    void **itable;
    int8_t *buckets;
    int8_t tid;
    uint16_t bucket;
};

/*
  FIXME: to make class loading work we need to have access to each
  class's constant pool.  without this, we would suffer some serious
  performance issues.
  
  ... or not.  who knows?
  
  The main issue - as far as I can tell - is that if we did have a
  constant pool then we would have a fast way of performing linking.
  We run the resolver on the class and then we get an array, for
  each class, that holds references to all of the things that the
  class is interested in.  That's great.  And then all you've gotta
  do to resolve a field or method reference is to follow the
  constant pool index.  Cool.  Without access to constant pools, though,
  you have to do a string-based resolution for each field or method.
  That's not as good.  Maybe it matters, or maybe not, who knows.
  
  This stupid thing is that we need each TypeData to hold a list of
  references to the other classes it needs, anyway.  So it's super
  weird not to allow that table to be used for resolution caching.

  Ah!  But here's the thing:
  1) Using a table of types alone has the benefit of making the table
     smaller.  I.e. less information duplicated beyond what the
     bytecode already holds.
  2) It's somewhat attractive to use a strategy in which field and
     method signatures are internally resolved before being used
     for lookups.
 */

/* FIXME: references to types from fields and methods should be
   to TypeStubs, and unresolved types should be TypeStubs.   also,
   TypeContexts should have lists of TypeStubs. */
struct fivmr_TypeStub_s {
    fivmr_MonState state; /* always FIVMR_MS_INVALID */
    fivmr_TypeData *forward; /* points to NULL, another TypeStub, or a TypeData */
                                                                
    fivmr_BindingFlags flags; /* FIXME: add the notion of a RESOLVED flag */
    const char *name;						
                                                                
    fivmr_StaticTypeContext *context; /* the context this type  
                                         belongs to */          
    
    int32_t inited;

    /* FIXME #2: add the notion of a list of resolution points (addresses of machine
       code that needs to be updated during this type's resolution).  these
       need to be kept in a linked list form and pushed up to the parent upon
       unioning. - NO NO NO!  don't do that! */
};

#define FIVMR_MAKE_TYPEDATA_BODY()				\
    fivmr_MonState state; /* always FIVMR_MS_INVALID */         \
    fivmr_TypeData *forward; /* always self-pointing */		\
                                                                \
    fivmr_BindingFlags flags;					\
    const char *name;						\
                                                                \
    fivmr_StaticTypeContext *context; /* the context this type  \
                                         belongs to */          \
								\
    /* states:							\
       0 = not initialized					\
       1 = initialized successfully				\
       256 = currently initializing				\
       512 = initialization failed. */				\
    /* NB: it's tempting to change 256 to 257, to make		\
       CheckInit return quickly if the class is currently	\
       being initialized.  but that would be wrong, since it	\
       would make other threads assume that the class was	\
       initialized when it was in fact still in the process	\
       of initializing.  in that case, those other threads	\
       should enter the wait loop in				\
       fivmr_TypeData_checkInit(). */				\
    int32_t inited;						\
    								\
    fivmr_ThreadState *curIniter;				\
    								\
    const char *filename;					\
    fivmr_TypeData *parent;					\
    uint16_t nSuperInterfaces;					\
    uint16_t nDirectSubs;                                       \
    uint16_t ilistSize;                                         \
    fivmr_TypeData **superInterfaces;				\
                                                                \
    /* For classes, this lists all interface ancestors that     \
       aren't also ancestors of my superclass.  For interfaces, \
       this lists all strict interface ancestors.   For arrays, \
       this is empty. */                                        \
    fivmr_TypeData **ilist;                                     \
    fivmr_TypeData **directSubs;                                \
                                                                \
    int32_t canonicalNumber;					\
    int32_t numDescendants;                                     \
    fivmr_TypeEpoch epochs[2];                                  \
								\
    fivmr_TypeData *arrayElement;				\
    fivmr_TypeData *arrayType;                                  \
								\
    int32_t size;                                               \
    int8_t sizeAlignDiff;                                       \
    int8_t requiredAlignment;					\
    int8_t refSize; /* FIXME: store log of refSize instead */	\
								\
    fivmr_Object bytecode;                                      \
    fivmr_Object classObject;					\
                                                                \
    fivmr_TypeDataNode *node;                                   \
                                                                \
    uint16_t numMethods;					\
    uint16_t numFields;						\
    fivmr_MethodRec **methods;					\
    fivmr_FieldRec *fields;					\
								\
    uintptr_t gcMap;						\
								\
    int32_t uniqueID;						\
								\
    int32_t vtableLength

/* NVT = no vtable */
#define FIVMR_MAKE_TYPEDATA_NVT_S(structname)	\
    struct structname {				\
	FIVMR_MAKE_TYPEDATA_BODY();		\
    }

#define FIVMR_MAKE_TYPEDATA_S(structname,vtablesize) \
    struct structname {				     \
	FIVMR_MAKE_TYPEDATA_BODY();		     \
						     \
	void *vtable[vtablesize];		     \
    }

/* NOTE: this is not the declaration of this struct that is used when it
   is defined, because of the vtable.  thus:
   1) sizeof(fivmr_TypeData) is meaningless
   2) we better hope that the C compiler doesn't play any nasty
      tricks with field layout */
FIVMR_MAKE_TYPEDATA_S(fivmr_TypeData_s,1);

struct fivmr_TypeDataNode_s {
    ftree_Node treeNode;
    fivmr_TypeDataNode *next;
};

struct fivmr_Basepoint_s {
    int32_t bytecodePC;
    int32_t stackHeight; /* mainly useful for debugging, I think */
    void *machinecodePC;
    fivmr_Basepoint *next;
};

struct fivmr_BaseTryCatch_s {
    int32_t start;
    int32_t end;
    int32_t target;
    fivmr_TypeStub *type; /* it can be a type stub because we never have to
                             resolve it during exception dispatch ... if we
                             observe a try-catch that has an unresolved stub
                             then we know that the exception are handling
                             cannot be a subtype of it */
    fivmr_BaseTryCatch *next;
};

/* what we need here:
   - pointer to the MethodRec (done)
   - some indication of what purpose this MethodRec serves; i.e. is it
     a patch point, or is it the baseline code, or is it an optimized
     code (done - flags)
   - when we wish to toss a MethodRec we should remove it from the
     MethodRec's list and give it to the GC.  the GC will mark it when
     it encounters it on some thread's stack, and delete it when it is
     not on any thread stacks.  We need some way ot tracking that.
     (done - flags, and reuse next field for list)
   - a MachineCode may use some DebugID's.  we need to know which ones.
     (done)
   - conversely, each DebugRec needs to have a way to point to a
     MachineCode instead of a MethodRec, but of course just for the
     uninlined case (or in the case of inlining, just for the outermost
     MethodRec).  this is necessary since a MethodRec may have multiple
     versions of it, each having a MachineCode, and they may be
     running concurrently. (done - first word of both MethodRec and
     MachineCode is the flags, which can be used to distinguish between
     the two since a MethodRec will have FIVMR_MBF_COOKIE set)
   - a way of patching baseline code at points where resolution would
     have been incomplete.
   - a way of patching from a patch point in non-baseline code back into
     baseline code without having to recompile the entire method.  the
     best way to do that, I think, would be to record a bytecodePC-to-
     machinecodePC mapping, but just for (a) branches and (b) occasionally
     in the code.  we call these basepoints.  this would be mean that to
     patch from an arbitrary point, we invoke the baseline JIT but run it
     up until the next basepoint; at the basepoint it'll emit a jump
     back to the original baseline code.
   FIXME: instead of having full-blown sub-machinecodes, we should also
   have light-weight ones (no region, no type aux, etc). */
struct fivmr_MachineCode_s {
    fivmr_MachineCodeFlags flags;
    int32_t refCount;
    fivmr_MethodRec *mr;
    fivmr_Basepoint *bpList;
    fivmr_BaseTryCatch *btcFirst;
    fivmr_BaseTryCatch *btcLast;
    int32_t size;
    void *code;
    fivmr_MachineCode *next;
    fivmr_MachineCode *sub;
};

struct fivmr_MethodRec_s {
    fivmr_BindingFlags flags;
    fivmr_TypeData *owner;
    const char *name;
    fivmr_TypeStub *result;
    int32_t nparams;
    fivmr_TypeStub **params;
    uint16_t nStack;
    uint16_t nLocals;
    int32_t methodID;
#if !FIVMR_HAVE_NATIVE_BACKEND
    void *upcallPtr;
#endif
    uintptr_t location; /* for static methods: -1
                           for final methods: vtable index if we're overriding
                              something, or -1 if we're not
                           for virtual methods in classes: vtable index
                           for virtual methods in interfaces: two itable
                              indices; the first half contains the index for
                              epoch 0 and the second half contains the index
                              for epoch 1 */
    void *entrypoint; /* field modified at runtime, used to indicate the
                         entrypoint of the method.
                         NOTE: if we're in dynamic mode, every call to the
                         method will have to load from this field to get the
                         function pointer.  This is our TOC. */
    void *codePtr; /* for Java methods and dynamically loaded native methods:
                         linked list of machine codes for this method
                      for native methods in AOT code:
                         pointer to resolved native implementation */
};

struct fivmr_CompactMethodRec_s {
    fivmr_BindingFlags flags;
    fivmr_TypeData *owner;
    const char *name;
    fivmr_TypeStub *result;
    int32_t nparams;
    fivmr_TypeStub **params;
};

struct fivmr_FieldRec_s {
    fivmr_TypeData *owner;
    const char *name;
    fivmr_BindingFlags flags;
    fivmr_TypeStub *type;
    uintptr_t location; /* either the direct pointer (static case under JIT),
                           an offset in the static field table (static case
                           under AOT), or the field location (instance case) */
};

struct fivmr_BaseFieldAccess_s {
    fivmr_FieldAccessType fat;
    fivmr_TypeStub *owner;
    int32_t descAddr;
    int32_t nameAddr;
    int32_t stackHeight;
    int32_t recvType;
    int32_t dataType;
    uintptr_t debugID;
};

struct fivmr_BaseMethodCall_s {
    fivmr_MethodCallType mct;
    fivmr_TypeStub *owner;
    int32_t descAddr;
    int32_t nameAddr;
    int32_t stackHeight;
};

struct fivmr_BaseArrayAlloc_s {
    fivmr_TypeStub *type;
    int32_t stackHeight;
    uintptr_t debugID;
};

struct fivmr_BaseObjectAlloc_s {
    fivmr_TypeStub *type;
    int32_t stackHeight;
    uintptr_t debugID;
};

struct fivmr_BaseInstanceof_s {
    fivmr_InstanceofType iot;
    fivmr_TypeStub *type;
    int32_t stackHeight;
    uintptr_t debugID;
};

union fivmr_Value_u {
    int8_t Z;
    int8_t B;
    uint16_t C;
    int16_t S;
    int32_t I;
    int64_t J;
    float F;
    double D;
    
    /* the following three are guaranteed to be equivalent modulo C typing */
    fivmr_Object L;
    fivmr_Handle *H;
    uintptr_t P;
    uintptr_t f;
};

struct fivmr_TypedValue_s {
    char descriptor;
    fivmr_Value value;
};

struct fivmr_Monitor_s {
    fivmr_MonState state;
    fivmr_TypeData *forward;
    fivmr_MonQueues *queues;
    fivmr_Monitor *next;
};

struct fivmr_ObjHeader_s {
    uintptr_t word;
};

struct fivmr_GCHeader_s {
    /* encoding:
       low-order two bits: rotating mark status
       all other bits: pointer to next fivmr_GCHeader or NULL */
    uintptr_t word;
};

/*
 * fivmr_ScopeID
 *
 * One of these structures must exist for every frame using stack
 * allocation, and for every MemoryArea currently active.  The address
 * of the corresponding structure will be entered in the GCHeader word
 * for each object allocated on the stack or in a MemoryArea.
 *
 * The lowest order bit of the word in this structure must be zero if
 * the structure represents a stack frame allocation, and it must be 1
 * if it represents a memory area allocation.
 *
 * If this structure represents a memory area allocation, the remaining
 * bits are a pointer to the memory area object itself.  If it
 * represents a stack frame, they are undefined.
 */
#define FIVMR_SCOPEID_STACK 0
#define FIVMR_SCOPEID_SCOPE 1
#define FIVMR_SCOPEID_MASK 1

struct fivmr_ScopeID_s {
    uintptr_t word;
};

/* Unmanaged Memory Structures*/
//Size of array primitive element
#define ELEMENT_STORAGE_SIZE sizeof(uint64_t)
//Number of array elements supported per array block
#define ELEMENTS_PER_BLOCK 8
#define ARRAY_BLOCK_SIZE 64
#define UNMANAGED_BLOCK_SIZE 64

typedef struct fivmr_um_node_s fivmr_um_node;
typedef struct fivmr_um_array_header_s fivmr_um_array_header;
typedef struct fivmr_um_array_block_s fivmr_um_array_block;

/* A Linked List to represent unused memory */
struct fivmr_um_node_s {
  /* Pointer to next node */
  fivmr_um_node* next;
  /* Pad to make 64 byte size */
  char zero[60];
};

/* A Primitive Storage Block */
struct fivmr_um_primitive_block {
  /* Pointer to next Primitive Storage Block */
  struct fivmr_um_primitive_block *next;
  /* Bit Vector that determines which elements are free */
  int32_t map;
  /* Primitive Storage */
  uint64_t storage[6];
};

// typedef enum fivmr_um_type_t
// {
//   INT,
//   BYTE,
//   SHORT,
//   LONG,
//   DOUBLE,
//   FLOAT,
//   CHAR,
//   BOOLEAN
// } fivmr_um_type_t;

/*An array storage block */
struct fivmr_um_array_block_s {
  /* The storage available in this block */
  uint64_t storage[8];
};

/* An array header block */
struct fivmr_um_array_header_s {
  /* Points to the Array Spine in Scoped Memory */
  fivmr_um_array_block **spine;
  /* Supports (hypothetically) 2^31-1 elements */
  int32_t size;
  /* Store the array data type */
  int32_t type;
  /* Store the first six elements here */
  uint64_t elem[6];
  /* Zero out to make 64 byte block size*/
  char zero[4];
};

/* Note that any additions that change the size of this struct must be reflected in FIVMR_OFFSETOF_REGSAVE */
struct fivmr_MemoryArea_s {
    uintptr_t start;
    uintptr_t bump;
    uintptr_t size;
    uintptr_t top;

    fivmr_Object javaArea;

    uintptr_t objList;

    uintptr_t shared;
    /* This is the area's ScopeID >> 2, as stored in a GCHeader */
    uintptr_t scopeID;
    /* This is the area's ScopeID for RTSJ */
    fivmr_ScopeID scopeID_s;

    fivmr_MemoryArea *parent;

    /* UnManaged Scoped Memory Support */
    /* The start of scoped memory */ 
    uintptr_t new_start;
    /* The size of unmanaged memory */
    int64_t um_size;
    /* The amount consumed in unmanaged memory */
    int64_t um_consumed;
    /* The head to the Linked List of Free Blocks */
    fivmr_um_node *free_head;
    /* The head to the Linked List of Primitive Blocks with Available Space */
    struct fivmr_um_primitive_block *fr_head;
    /* The head to the Linked List of Full Primitive Blocks */
    struct fivmr_um_primitive_block *nfr_head;
    /* No data structures are needed to track arrays - it's all in the spine! */
};

#define FIVMR_MEMORYAREASTACK_GCINPROGRESS 0x1
#define FIVMR_MEMORYAREASTACK_POP 0x2
#define FIVMR_MEMORYAREASTACK_FREE 0x4
#define FIVMR_MEMORYAREASTACK_WAITING 0x8

struct fivmr_MemoryAreaStack_s {
    fivmr_MemoryArea *area;
    fivmr_MemoryAreaStack *prev;
    fivmr_MemoryAreaStack *next;
    uintptr_t flags;
};

struct fivmr_ScopeBacking_s {
    uintptr_t size;
    uintptr_t top;
    char start[1];
};

struct fivmr_FreeLine_s {
    fivmr_FreeLine *prev;
    fivmr_FreeLine *next;
    fivmr_FreeLine *lastOnPage;
    uintptr_t size; /* size including the FreeLine structure */
};

struct fivmr_LargeObjectHeader_s {
    fivmr_LargeObjectHeader *next;
    uintptr_t fullSize; /* size including the header and any other stuff.
                           i.e. the range of memory required for this object
                           to exist and get accounted is from self to
                           (uintptr_t)self+self->fullSize. */
    fivmr_GCHeader *object;
};

struct fivmr_GCSpaceAlloc_s {
    uintptr_t bump; /* the next allocation pointer.  NOTE that this points
		       to headersize+4 past the beginning of the next
		       object. */
    uintptr_t start; /* the start value of the bump pointer. */
    uintptr_t size; /* the size of the bump area. */
    uintptr_t zero; /* this field is always zero. */

    /* semispace allocation - NB this allocates DOWN */
    uintptr_t ssBump;
    uintptr_t ssEnd;
    uintptr_t ssSize;
    
    /* used for slow path line allocation */
    fivmr_UsedPage *usedPage;
    fivmr_FreeLine *freeHead;
    fivmr_FreeLine *freeTail;
};

struct fivmr_GCData_s {
    /* place these fields first because they're important */
    uintptr_t invCurShaded; /* not shifted! */
    uintptr_t zeroCurShaded; /* not shifted! */
    uintptr_t curShadedAlloc; /* shifted! */
    bool tracing;
    
    /* the first fields of the first allocator are still at <=127 offset from
       beginning of thread state */
    fivmr_GCSpaceAlloc alloc[FIVMR_GC_NUM_SPACES];
    fivmr_Destructor *destructorHead;
    fivmr_Destructor *destructorTail;

    fivmr_GCHeader *queue; /* enqueued by thread itself without locking,
			      all entries moved to global queue while holding
			      global lock upon soft handshake or termination. */
    fivmr_GCHeader *queueTail; /* maintain the tail of the queue to make
				  moving the queue easy. */
    fivmr_ThreadState *requesterNext;
    uintptr_t gcFlags;


    /* Three pointer words, one fivmr_MemoryArea (9 words), a
     * fivmr_MemoryAreaStack (3 words) per thread of overhead when scoped
     * memory is not in use. */
    fivmr_MemoryAreaStack *scopeStack;
    fivmr_MemoryArea *currentArea;
    fivmr_MemoryArea heapMemoryArea; /* FIXME: why is this thread-local? */
    fivmr_MemoryAreaStack baseStackEntry;
    fivmr_ScopeBacking *scopeBacking;
};

struct fivmr_Destructor_s {
    fivmr_Object object;
    fivmr_Destructor *next;
};

struct fivmr_Frame_s {
    uintptr_t id;
    fivmr_Frame *up;
#if FIVMR_INTERNAL_INST
FIVMR_II_FRAME_FIELDS
#endif
    fivmr_Object refs[1]; /* this isn't actually size 1.  the size varies from
			     frame to frame. */
};

struct fivmr_JNIEnv_s {
    void *functions;
    fivmr_MethodRec *mr;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
};

struct fivmr_Handle_s {
    fivmr_VM *vm; /* the VM this handle belongs to */
    fivmr_Object obj; /* direct pointer into the heap */
    fivmr_Handle *prev;
    fivmr_Handle *next;
};

struct fivmr_HandleRegion_s {
    /* dummy head/tail nodes used only to simply adding/removing handles */
    fivmr_Handle head;
    fivmr_Handle tail;
};

struct fivmr_NativeFrame_s {
    fivmr_NativeFrame *up;
    fivmr_HandleRegion hr;
    fivmr_JNIEnv jni;
};

struct fivmr_BoostedSpinLock_s {
    fivmr_SpinLock spinlock;
};

struct fivmr_ThreadQueue_s {
    fivmr_ThreadState *head;
    fivmr_ThreadState *tail;
    int16_t flags; /* not used yet but they don't take up any extra space */
    fivmr_BoostedSpinLock lock; /* this should always be 8 bytes */
};

struct fivmr_MonQueues_s {
    fivmr_ThreadQueue entering;
    fivmr_ThreadQueue waiting;
};

struct fivmr_MonitorData_s {
    int32_t deqUnlockCount;
    bool syncHandoffCookie;
    fivmr_Monitor *entering;
    fivmr_Monitor *holding;
    
    /* indicates if we're on a queue.  this can only be set by the thread
       (or on its behalf if you're in a safepoint), but will be reset
       subject to the queue's lock by whatever thread dequeues this thread.
       all actions on this field are done while holding the queue lock, and
       setting (NULL -> someTS) is done while holding: the queue lock, the
       ThreadState::lock, and while running IN_JAVA. */
    fivmr_ThreadQueue *queuedOnReal;
    
    /* indicates intent to be on a queue.  this can only be set and reset
       by the thread (or on its behalf if you're in a safepoint).  this
       gets reset after the thread acknowledges that it has been dequeued.
       this field may be non-NULL after queudOnReal has already been set to
       NULL.  this is the field you generally want to use if you want to
       find out if the thread is on a queue, but you don't want to grab
       the queue's lock, and you're already holding the ThreadState::lock.
       all operations on this field are done while holding: the queue lock,
       the ThreadState::lock, and while running IN_JAVA. */
    fivmr_ThreadQueue *queuedOnIntended;
    
    fivmr_ThreadState *next;
};

struct fivmr_ThreadState_s {
    /* this comes first because that makes things fast. */
    union {
	struct {
	    int16_t pollchecksDisabled;
	    int16_t checkBlockNotRequested;
	} s;
	int32_t takeFastpath;
    } pollingUnion;
    
    uint32_t cookie;
    
    /* this must be at a fixed offset from the start of the struct */
    fivmr_Object curException;

    fivmr_VM *vm;
    
    /* bunch of stuff accessed frequently so make the offset small */
    fivmr_Frame *curF;

    uint32_t id; /* for real threads this starts at 2 */
    uint32_t lockingId; /* id shifted for locking; FIXME we should be epic
                           smart about this and just make the locking ID be
                           some special bits in the ThreadState pointer. */

    uintptr_t stackLimit; /* the limit of how low or high the stack can go. */

    /* what follows is a function table used by machine code we generate; this
       table can be mucked with but keep it close to the top of the struct.
       Note that none of these are "normal" C functions; they make all manner
       of special assumptions about how they get called and none of them has
       a "sensible" calling convention. */
    void (*baselineThrowThunk)(void);
    void (*baselineProEpThrowThunk)(void);
    void (*pollcheckSlowBaseline)(void);
    void (*nullCheckSlowBaseline)(void);
    void (*abcSlowBaseline)(void);
    void (*resolveFieldAccessThunk)(void);
    void (*resolveMethodCallThunk)(void);
    void (*resolveInvokeInterfaceThunk)(void);
    void (*resolveArrayAllocThunk)(void);
    void (*resolveObjectAllocThunk)(void);
    void (*resolveInstanceofThunk)(void);
    void (*stackHeightSlowBaseline)(void);
    /* -- end function table -- */
    
    fivmr_GCData gc;

    uintptr_t regSave[2]; /* the offset of this must be known */

    fivmr_JmpBuf *jumpOnExit;
    bool exitOnExit;
    
    fivmr_ThreadHandle thread;

    uint32_t index;

    uintptr_t execStatus; /* CAS'd by the thread itself but only for
			     Java<->native transitions, protected by lock
			     for all other accesses. */
    uintptr_t execFlags; /* protected by the lock */
    
    int32_t suspendReqCount;
    int32_t suspendCount; /* if 0, means that the thread may run in Java,
                             if greater than zero, means that the thread
                             should stop. */
    int64_t version;
    
    int32_t typeEpoch;

    int64_t pollchecksTaken;

    uintptr_t stackHeight; /* the amount of stack that Java code is allowed
                              to use.  should be about FIVMR_STACK_HEIGHT_HEADROOM
                              less than the amount actually available. */
    
    uintptr_t stackStart; /* where the stack was when we attached the thread. */
    uintptr_t stackHigh;
    
    bool handlingStackOverflow;

    bool isDaemon;

    fivmr_Lock lock; /* protects most of the thread's state and also serves
                        as a notification mechanism for threads that want
                        to wait on some state change in this thread.  it
                        is also sometimes used by this thread to wait on
                        notifications - but that use is slowly being phased
                        out! */
    fivmr_Semaphore waiter; /* used by this thread to wait for notifications
                               from other threads */

    fivmr_Handle *curExceptionHandle;
    
    fivmr_Object toUnbias;

    bool interrupted;
    
    fivmr_NativeFrame rootNF;
    fivmr_NativeFrame *curNF;
    
    fivmr_Frame rootF;
    
    /* this gets used in misc cases where we need to stash a root somewhere.
       currently slot 0 is used by baseline JIT code whe calling addDestructor.
       other than that, this doesn't get used ever. */
    fivmr_Object roots[FIVMR_TS_MAX_ROOTS];
    
    /* stores the state of the program at a patch point.  might also use it
       for OSR, possibly. */
    uintptr_t stateBuf[FIVMR_TS_STATE_BUF_LEN];
    uintptr_t *stateBufOverflow;
    uint32_t *stateBufGCMap; /* non-NULL only when the state buf should be scanned
                                by the GC */
    uintptr_t stateSize; /* always non-zero when in a patch point since the state
                            always includes at least the receiverish. */
    
    void **patchRepo;
    
    fivmr_Handle *freeHandles;
    
    fivmr_ScopeID *allocFrame;
    
    fivmr_ThreadPriority basePrio;
    fivmr_ThreadPriority permBoostPrio;
    fivmr_ThreadPriority curPrio; /* computed from basePrio, monitors, and permBoostPrio */
    fivmr_ThreadPriority curTempBoostPrio; /* last set priority of the thread; may be higher than curPrio but only until the next safepoint */
    
    fivmr_MonitorData forMonitor;
    
    /* what else do we need?
       - for precise GC, need Henderson pointer (curFP?  or do we need more?)
       - request to stop for GC
       - soft handshake support */
    
    /* buffer used for arrays and strings and such. */
    void *buf;
    bool bufInUse;
    
    bool performedGuaranteedCommit;

    /* note: these two fields are copies from fivmr_VM; we need to keep them
       coherent. */
    int64_t *primFields;
    fivmr_Object *refFields;
    fivmr_TypeData **typeList;
    fivmr_TypeStub *stubList;

    fivmr_FlowLogBuffer *flowbuf;

#if FIVMR_INTERNAL_INST
FIVMR_II_THREADSTATE_FIELDS
#endif
};

#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
struct fivmr_JNILib_s {
    fivmr_JNILib *next;
    const char *filename;
    fivmr_ModuleHandle module;
    void *onLoadHook;
    bool initialized;
};
#endif

/*
   How DebugRec/InlineMethodRec gets used:
   
   Simple example: describing a line of code where no inlining was going on:
   
   1: void foo() {
   2:    safepoint;  // this is what we're describing
   3: }
   
   Note that at the safepoint, one reference is live (the receiver)
   
   DebugRec:
       ln_rm_c = (2<<16)|(1<<2)|(0<<1)|(0<<0)
       method = &fivmc_MethodRec_foo

   Complex example: describing a line of code where inlining was going on:
   
   1: void foo() {
   2:    safepoint;  // this is what we're describing
   3: }
   4: void bar() {
   5:    foo();   // this call got inlined
   6: }
   
   DebugRec:
       ln_rm_c = (2<<16)|(1<<2)|(1<<1)|(0<<0)
       method = &InlineMethodRec:
                    ln_c = (5<<1)|(0<<0)
		    caller = &fivmc_MethodRec_bar
                    method = &fivmc_MethodRec_foo

   Even more complex example: two levels of inlining:

   1: void foo() {
   2:    safepoint;  // this is what we're describing
   3: }
   4: void bar() {
   5:    foo();   // this call got inlined
   6: }
   7: void baz() {
   8:    bar();  // this call got inlined
   9: }
   
   DebugRec:
       ln_rm_c = (2<<16)|(1<<2)|(1<<1)|(0<<0)
       method = &InlineMethodRec:
                    ln_c = (5<<1)|(1<<0)
		    caller = &InlineMethodRec:
		    	         ln_c = (8<<1)|(0<<0)
				 caller = &fivmc_MethodRec_baz
				 method = &fivmc_MethodRec_bar
                    method = &fivmc_MethodRec_foo
*/

struct fivmr_InlineMethodRec_s {
    /* FIXME: at some point we'll want this to include the bytecodePC */
    /*
       Line number in the method we were inlined into, as well as a control
       bit to indicate if that method was inlined into anything else.
       
       The format is:
          31 bits:  line number
	   1 bit:   was the caller inlined into anything else (value = 1), or
	     	    is the caller the outer-most method (value = 0)
    */
    uintptr_t ln_c;
    
    /*
       The method we were inlined into.  This is either a fivmr_MethodRec*
       if that's the outer-most method, or a fivmr_InlineMethodRec* if
       there are still more methods in the chain.
    */
    uintptr_t caller;
    
    /*
       The current method.
    */
    fivmr_MethodRec *method;
};

/* this gets generated as a LocalDataConstant... */
struct fivmr_FatDebugData_s {
    int32_t lineNumber;
    int32_t bytecodePC;
    int32_t refMapSize; /* specifies the number of int32_t words in the refMap,
			   rather than the number of bits */
    int32_t refMap[1]; /* variable-length array */
};

struct fivmr_DebugRec_s {
    /* FIXME: the current bit assignment on 32-bit systems is suboptimal.  we
       probably want a smaller ref map. */
    /*
       Stores the line number, ref map, and control bits.  The formats are:
       
       Thin:   (from high order to low order)
           8 bits:  bytecode PC
          10 bits:  line number
	  12 bits:  ref map bits
	   1 bit:   1 = is in inline code, 0 = not in inline code
	   1 bit:   0 (to indicate that we're in thin form)

       Fat:
          30 bits:  pointer to FatDebugData
	   1 bit:   1 = is in inline code, 0 = not in inline code
	   1 bit:   1 (to indicate that we're in fat form)
	   
       On a 64-bit system we use 15 bits for the bytecode PC, 16 bits for the
       line number, and 31 bits for the ref map.
    */
    uintptr_t ln_rm_c;
    
    /*
       Stores either the method rec pointer (if we're not in inline code)
       or the InlineMethodRec pointer (if we are in inline code).
       This could also be a MachineCode pointer.
       
       The lowest-order bit tells us if this is a patch point.
    */
    uintptr_t method;
};

struct fivmr_StackTraceFrame_s;
typedef struct fivmr_StackTraceFrame_s fivmr_StackTraceFrame;

struct fivmr_ThreadStackTrace_s;
typedef struct fivmr_ThreadStackTrace_s fivmr_ThreadStackTrace;

struct fivmr_AllThreadStackTraces_s;
typedef struct fivmr_AllThreadStackTraces_s fivmr_AllThreadStackTraces;

struct fivmr_StackTraceFrame_s {
    fivmr_MethodRec *mr;
    int32_t lineNumber;
    fivmr_StackTraceFrame *next;
};

struct fivmr_ThreadStackTrace_s {
    fivmr_Handle *thread;
    fivmr_StackTraceFrame *top;
    int32_t depth;
    uintptr_t execStatus;
    uintptr_t execFlags;
    fivmr_ThreadStackTrace *next;
};

struct fivmr_AllThreadStackTraces_s {
    fivmr_ThreadStackTrace *first;
    int32_t numThreads;
};

/* additional constants for GC */
#define FIVMR_UP_BITS_LENGTH(settings)                                  \
    ((FIVMR_PAGE_SIZE-4+4*(1+8*FIVMR_MIN_CHUNK_ALIGN(settings))-1) /    \
     (4*(1+8*FIVMR_MIN_CHUNK_ALIGN(settings))))

#define FIVMR_UP_SIZE(settings)                 \
    (FIVMR_UP_BITS_LENGTH(settings)*4+4)

#define FIVMR_MIN_FREE_LINE_SIZE(settings)                              \
    (sizeof(fivmr_FreeLine)>FIVMR_MIN_ALLOC_SIZE(settings)              \
     ?sizeof(fivmr_FreeLine)                                            \
     :FIVMR_MIN_ALLOC_SIZE(settings))

struct fivmr_UsedPage_s {
    /* NOTE: the contents and size of this struct are variable; in fact the very
       first field is variable.  please don't try to change that - it's kind of on
       purpose.  thus the purpose of this struct is just for typing.  the "fields"
       are virtual and are accessed using inline functions... */
};

/* and some more constants for GC ... */
#define FIVMR_PAGE_HEADER(settings)                                     \
    ((FIVMR_UP_SIZE(settings)+FIVMR_MIN_CHUNK_ALIGN(settings)-1)        \
     &~(FIVMR_MIN_CHUNK_ALIGN(settings)-1))

#define FIVMR_LARGE_ARRAY_THRESHOLD ((uintptr_t)(32*sizeof(uintptr_t)))
#define FIVMR_ARRAY_THREAD_CACHE_SIZE ((uintptr_t)(64*sizeof(uintptr_t)))

struct fivmr_FreePage_s {
    fivmr_FreePage *next;
    fivmr_FreePage *prev;
    bool dirty;
};

struct fivmr_GCSpaceData_s {
    fivmr_PageTable pt;
    fivmr_FreeLine freeLineHead;
    fivmr_FreeLine freeLineTail;
};

struct fivmr_SSDescr_s {
    /* description of the entire space */

    uintptr_t start;
    uintptr_t size; /* in bytes */
    
    /* allocation state.  we allocate black from the top down, and the
       collector allocates white from the bottom up.  these are adjusted
       by FIVMR_SP_FR_OFFSET. */
    
    uintptr_t allocStart;
    uintptr_t allocEnd;
    
    uintptr_t muAlloc; /* moves down */
    uintptr_t gcAlloc; /* moves up */
    
    uintptr_t muLimit;
    
    /* description of what area was used before flip - NOT offset.  this is only
       used for deciding which area of the semi-spaces to zero. */
    uintptr_t muRegionStart;
    uintptr_t muRegionSize;
    uintptr_t gcRegionStart;
    uintptr_t gcRegionSize;
    
    /* tells if a particular mutator is the first to attempt allocating. */
    bool first;
    
    /* queue of objects that have spines that need copying. */
    fivmr_GCHeader *head;
};

/* This structure is for flags and parameters that are baked in at
   compile-time, and cannot be changed at run-time (for a particular
   payload).  They may change from compiled payload to compiled payload,
   but once a program is compiled, the settings cannot change, for that
   program.  These parameters are appropriately constant-propagated
   by both the Java bytecode compiler (i.e. Fiji C1) and the C compiler
   by way of accessor macros (see fivmr_settings_getters.h).

   All of these parameters are declared in
   common/src/java/com/fiji/fivm/Settings.java.  Do not add them here;
   modify that file instead. */
struct fivmr_Settings_s {
#include "fivmr_settings_fields.h" /* generated from Settings.java */
};

/* This structure is for flags and parameters that are set to some default at
   compile-time (possibly by way of arguments to fivmc, or through other means)
   but may be modified at run-time.  In particular, these parameters neither
   affect, nor are affected by, how the program is compiled. */
struct fivmr_Configuration_s {
    int64_t gcDefTrigger;
    int64_t gcDefMaxMem;
    int64_t gcDefImmortalMem;
    uint32_t maxThreads;
    int64_t stackAllocSize;
    fivmr_ThreadPriority gcDefPriority;
    bool enableHardRTJ;
};

struct fivmr_Payload_s {
    /* note that this structure may be the root of a region, IF it is
       a copy.  see fivmr_region.h for info on what it means to be the
       root of a region. */
    
    const char *compilerRevision; /* this MUST be a string constant */
    const char *fivmcHomeDir; /* this MUST be a string constant */
    
    fivmr_Settings settings;
    fivmr_Configuration *defConfig;
    
    fivmr_PayloadMode mode;
    
    fivmr_VM *ownedBy; /* used to ensure that a Payload isn't passed to multiple
                          VMs, unless it is copied first.  note that copying a
                          Payload that had already been VM-registered is not
                          legal. */

    /* type stuff */
    int32_t primFieldsLen;
    int32_t nRefFields;

    int64_t *primFields;
    fivmr_Object *refFields;

    int32_t nDebugIDs;
    int32_t nStrings;
    uintptr_t bytecodeSize;
    int32_t nTypes;
    int32_t nStubs;
    int32_t nContexts;
    int32_t maxCanonicalNumber;

    fivmr_TypeData **typeList;
    fivmr_TypeStub *stubList;
    /* by convention, contexts[0] is the root context */
    fivmr_StaticTypeContext *contexts;

    fivmr_DebugRec *debugTable;
    uintptr_t *stringTable;
    uintptr_t *stringArray; /* single array used by all strings */
    uintptr_t *bytecodeArray; /* single array for storing all bytecode arrays */
    fivmr_Object *stringIndex; /* an array used for binary searching over strings:
                                  it contains pointers into the stringTable,
                                  sorted according to the string's value */
    uintptr_t *classTable; /* the number of classes is exactly the same as the
                              number of types. */
    
    uintptr_t classTDOffset; /* at what offset from the object pointer does a
                                Class object have a pointer to the TypeData? */
    uintptr_t stringArrOffset; /* at what offset from the object pointer does
                                  a String object have a pointer to the char[]?
                                  this is a bit of a hack but allows for a
                                  good deal of object model and library
                                  flexibility. */
    
    const char *name; /* the name of the payload */
    fivmr_TypeData *entrypoint;
    
    uint32_t *usedTids; /* 256 bits per bucket */
    uintptr_t numBuckets;
    
    int32_t *itableOcc;
    uintptr_t itableSize;
    
    void **patchRepo;
    uintptr_t patchRepoSize;
    
    fivmr_PayloadList *subPayloads; /* list of payloads we're managing. */
    
    bool (*postThreadInitCback)(fivmr_ThreadState *ts); /* this is typically
                                                           used for static JNI
                                                           on-loads. */
    
    fivmr_TypeData *td_top;
    fivmr_TypeData *td_void;
    fivmr_TypeData *td_boolean;
    fivmr_TypeData *td_byte;
    fivmr_TypeData *td_char;
    fivmr_TypeData *td_short;
    fivmr_TypeData *td_int;
    fivmr_TypeData *td_long;
    fivmr_TypeData *td_float;
    fivmr_TypeData *td_double;
    fivmr_TypeData *td_pointer;
    fivmr_TypeData *td_vm_FCPtr;
    fivmr_TypeData *td_Object;
    fivmr_TypeData *td_String;
    fivmr_TypeData *td_Class;
    fivmr_TypeData *td_Serializable;
    fivmr_TypeData *td_Cloneable;
    fivmr_TypeData *td_Field;
    fivmr_TypeData *td_Method;
    fivmr_TypeData *td_Constructor;
    fivmr_TypeData *td_WeakReference;
    fivmr_TypeData *td_booleanArr;
    fivmr_TypeData *td_byteArr;
    fivmr_TypeData *td_charArr;
    fivmr_TypeData *td_shortArr;
    fivmr_TypeData *td_intArr;
    fivmr_TypeData *td_longArr;
    fivmr_TypeData *td_floatArr;
    fivmr_TypeData *td_doubleArr;
    fivmr_TypeData *td_pointerArr;
    fivmr_TypeData *td_vm_FCPtrArr;
    fivmr_TypeData *td_ObjectArr;
    fivmr_TypeData *td_StringArr;
    fivmr_TypeData *td_ClassArr;
    
    void (*fivmRuntime_boot)(uintptr_t ts);
    void (*fivmRuntime_initSystemClassLoaders)(uintptr_t ts);
    void (*fivmRuntime_notifyInitialized)(uintptr_t ts);
    uintptr_t (*allocForNative)(uintptr_t ts,
                                int32_t space,
                                uintptr_t td);
    uintptr_t (*allocArrayForNative)(uintptr_t ts,
                                     int32_t space,
                                     uintptr_t td,
                                     int32_t numEle);
    void (*throwInternalError)(uintptr_t ts,
                               uintptr_t reason);
    void (*throwNoClassDefFoundError_inNative)(uintptr_t ts,
                                               uintptr_t className,
                                               uintptr_t fromWhere);
    void (*throwNoClassDefFoundError_inJava)(uintptr_t ts,
                                             uintptr_t className,
                                             uintptr_t fromWhere);
    void (*throwLinkageError_inJava)(uintptr_t ts,
                                     uintptr_t reason);
    void (*throwNullPointerRTE)(uintptr_t ts);
    void (*throwNullPointerRTE_inJava)(uintptr_t ts);
    void (*throwArithmeticRTE)(uintptr_t ts);
    void (*throwArithmeticRTE_inJava)(uintptr_t ts);
    void (*throwStackOverflowRTE)(uintptr_t ts);
    void (*throwStackOverflowRTE_inJava)(uintptr_t ts);
    void (*throwClassChangeRTE_inJava)(uintptr_t ts);
    void (*throwArrayBoundsRTE_inJava)(uintptr_t ts);
    void (*throwArrayStoreRTE_inJava)(uintptr_t ts);
    void (*throwNegativeSizeRTE_inJava)(uintptr_t ts);
    void (*throwAbstractMethodError_inJava)(uintptr_t ts);
    void (*throwClassCastRTE_inJava)(uintptr_t ts);
    void (*throwUnsatisfiedLinkErrorForLoad)(uintptr_t ts,
                                             uintptr_t filename,
                                             uintptr_t error);
    void (*throwNoSuchFieldError)(uintptr_t ts,
                                  uintptr_t name,
                                  uintptr_t sig);
    void (*throwNoSuchMethodError)(uintptr_t ts,
                                   uintptr_t name,
                                   uintptr_t sig);
    void (*throwNoSuchMethodError_inJava)(uintptr_t ts,
                                          uintptr_t mr);
    void (*throwExceptionInInitializerError_inJava)(uintptr_t ts,
                                                    uintptr_t cause,
                                                    uintptr_t td);
    void (*throwReflectiveException_inJava)(uintptr_t ts,
                                            uintptr_t cause);
    void (*throwIllegalMonitorStateException_inJava)(uintptr_t ts,
                                                     uintptr_t msg);
    void (*throwOutOfMemoryError_inJava)(uintptr_t ts);
    void (*throwIllegalAssignmentError)(uintptr_t ts);
    void (*throwIllegalAssignmentError_inJava)(uintptr_t ts);
    void (*describeExceptionImpl)(uintptr_t ts,
                                  uintptr_t h);
    uintptr_t (*fromCStringInHeap)(uintptr_t ts,
                                   uintptr_t str);
    uintptr_t (*fromCStringFullInHeap)(uintptr_t ts,
                                       uintptr_t str);
    uintptr_t (*fromUTF16Sequence)(uintptr_t ts,
                                   uintptr_t chars,
                                   int32_t len);
    int32_t (*stringLength)(uintptr_t ts,
                            uintptr_t h);
    int32_t (*cstringLength)(uintptr_t ts,
                             uintptr_t h);
    uintptr_t (*getUTF16Sequence)(uintptr_t ts,
                                  uintptr_t h);
    uintptr_t (*getCString)(uintptr_t ts,
                            uintptr_t h);
    uintptr_t (*getCStringFull)(uintptr_t ts,
                                uintptr_t h);
    void (*getStringRegion)(uintptr_t ts,
                            uintptr_t h,
                            int32_t start,
                            int32_t len,
                            uintptr_t buf);
    void (*getStringUTFRegion)(uintptr_t ts,
                               uintptr_t h,
                               int32_t start,
                               int32_t len,
                               uintptr_t buf);
    uintptr_t (*String_getArrayPointer)(uintptr_t ts,
                                        uintptr_t h);
    int32_t (*String_getOffset)(uintptr_t ts,
                                uintptr_t h);
    uintptr_t (*getBooleanElements)(uintptr_t ts,
                                    uintptr_t h);
    uintptr_t (*getByteElements)(uintptr_t ts,
                                 uintptr_t h);
    uintptr_t (*getCharElements)(uintptr_t ts,
                                 uintptr_t h);
    uintptr_t (*getShortElements)(uintptr_t ts,
                                  uintptr_t h);
    uintptr_t (*getIntElements)(uintptr_t ts,
                                uintptr_t h);
    uintptr_t (*getLongElements)(uintptr_t ts,
                                 uintptr_t h);
    uintptr_t (*getFloatElements)(uintptr_t ts,
                                  uintptr_t h);
    uintptr_t (*getDoubleElements)(uintptr_t ts,
                                   uintptr_t h);
    void (*returnBooleanElements)(uintptr_t ts,
                                  uintptr_t h,
                                  uintptr_t buf,
                                  int mode);
    void (*returnByteElements)(uintptr_t ts,
                               uintptr_t h,
                               uintptr_t buf,
                               int mode);
    void (*returnCharElements)(uintptr_t ts,
                               uintptr_t h,
                               uintptr_t buf,
                               int mode);
    void (*returnShortElements)(uintptr_t ts,
                                uintptr_t h,
                                uintptr_t buf,
                                int mode);
    void (*returnIntElements)(uintptr_t ts,
                              uintptr_t h,
                              uintptr_t buf,
                              int mode);
    void (*returnLongElements)(uintptr_t ts,
                               uintptr_t h,
                               uintptr_t buf,
                               int mode);
    void (*returnFloatElements)(uintptr_t ts,
                                uintptr_t h,
                                uintptr_t buf,
                                int mode);
    void (*returnDoubleElements)(uintptr_t ts,
                                 uintptr_t h,
                                 uintptr_t buf,
                                 int mode);
    void (*getBooleanRegion)(uintptr_t ts,
                             uintptr_t h,
                             int32_t start,
                             int32_t len,
                             uintptr_t buf);
    void (*getByteRegion)(uintptr_t ts,
                          uintptr_t h,
                          int32_t start,
                          int32_t len,
                          uintptr_t buf);
    void (*getCharRegion)(uintptr_t ts,
                          uintptr_t h,
                          int32_t start,
                          int32_t len,
                          uintptr_t buf);
    void (*getShortRegion)(uintptr_t ts,
                           uintptr_t h,
                           int32_t start,
                           int32_t len,
                           uintptr_t buf);
    void (*getIntRegion)(uintptr_t ts,
                         uintptr_t h,
                         int32_t start,
                         int32_t len,
                         uintptr_t buf);
    void (*getLongRegion)(uintptr_t ts,
                          uintptr_t h,
                          int32_t start,
                          int32_t len,
                          uintptr_t buf);
    void (*getFloatRegion)(uintptr_t ts,
                           uintptr_t h,
                           int32_t start,
                           int32_t len,
                           uintptr_t buf);
    void (*getDoubleRegion)(uintptr_t ts,
                            uintptr_t h,
                            int32_t start,
                            int32_t len,
                            uintptr_t buf);
    void (*setBooleanRegion)(uintptr_t ts,
                             uintptr_t h,
                             int32_t start,
                             int32_t len,
                             uintptr_t buf);
    void (*setByteRegion)(uintptr_t ts,
                          uintptr_t h,
                          int32_t start,
                          int32_t len,
                          uintptr_t buf);
    void (*setCharRegion)(uintptr_t ts,
                          uintptr_t h,
                          int32_t start,
                          int32_t len,
                          uintptr_t buf);
    void (*setShortRegion)(uintptr_t ts,
                           uintptr_t h,
                           int32_t start,
                           int32_t len,
                           uintptr_t buf);
    void (*setIntRegion)(uintptr_t ts,
                         uintptr_t h,
                         int32_t start,
                         int32_t len,
                         uintptr_t buf);
    void (*setLongRegion)(uintptr_t ts,
                          uintptr_t h,
                          int32_t start,
                          int32_t len,
                          uintptr_t buf);
    void (*setFloatRegion)(uintptr_t ts,
                           uintptr_t h,
                           int32_t start,
                           int32_t len,
                           uintptr_t buf);
    void (*setDoubleRegion)(uintptr_t ts,
                            uintptr_t h,
                            int32_t start,
                            int32_t len,
                            uintptr_t buf);
    void (*returnBuffer)(uintptr_t ts,
                         uintptr_t ptr);
    uintptr_t (*DirectByteBuffer_wrap)(uintptr_t ts,
                                       uintptr_t ptr,
                                       int32_t capacity,
                                       int32_t limit,
                                       int32_t position);
    uintptr_t (*DirectByteBuffer_address)(uintptr_t ts,
                                          uintptr_t h);
    int32_t (*DirectByteBuffer_capacity)(uintptr_t ts,
                                         uintptr_t h);
    uintptr_t (*VMThread_create)(uintptr_t ts,
                                 int32_t priority,
                                 bool daemon);
    
     uintptr_t (*VMThread_createRT)(uintptr_t ts,     
                                 int32_t priority,
                                 bool daemon);


    void (*VMThread_setThreadState)(uintptr_t callerTS,
                                    uintptr_t vmt,
                                    uintptr_t tsToSet);
    uintptr_t (*VMThread_getThreadState)(uintptr_t ts,
                                         uintptr_t vmt);
    void (*VMThread_starting)(uintptr_t ts,
                              uintptr_t vmt);
    void (*VMThread_run)(uintptr_t ts,
                         uintptr_t vmt);
    bool (*VMThread_setUncaughtException)(uintptr_t ts,
                                          uintptr_t vmt,
                                          uintptr_t exception);
    void (*VMThread_die)(uintptr_t ts,
                         uintptr_t vmt);
    bool (*VMThread_isDaemon)(uintptr_t ts,
                              uintptr_t vmt);
    int32_t (*VMThread_getPriority)(uintptr_t ts,
                                    uintptr_t vmt);
    uintptr_t (*DumpStackCback_cback)(uintptr_t ts,
                                      uintptr_t cback,
                                      uintptr_t mr,
                                      int32_t lineNumber);
    void (*makeJNIFuncName)(uintptr_t ts,
                            uintptr_t cstr,
                            int32_t len,
                            uintptr_t mr,
                            bool longForm); /* call only IN_JAVA */
    void (*runRunnable)(uintptr_t ts,
                        uintptr_t h); /* call only IN_NATIVE */
    void (*MemoryArea_doRun)(uintptr_t ts,
                             uintptr_t area,
                             uintptr_t logic);
    uintptr_t (*MemoryArea_getBSID)(uintptr_t ts,
                                    uintptr_t area);
    uintptr_t (*fivmRuntime_loadClass)(uintptr_t ts,
                                       uintptr_t ctx,
                                       uintptr_t loader,
                                       uintptr_t name);
    void (*handlePatchPointImpl)(uintptr_t ts,
                                 uintptr_t debugID,
                                 uintptr_t className,
                                 uintptr_t fromWhere,
                                 int32_t bcOffset,
                                 uintptr_t patchThunkPtrPtr,
                                 uintptr_t origPatchThunk);
    void (*handleLoadThunk)(uintptr_t ts,
                            uintptr_t mr);
    void (*allocateClass)(uintptr_t ts,
                          uintptr_t td);
    void (*handleFieldResolution)(uintptr_t ts,
                                  uintptr_t returnAddr,
                                  uintptr_t bfa);
    void (*handleMethodResolution)(uintptr_t ts,
                                   uintptr_t debugID,
                                   uintptr_t returnAddr,
                                   uintptr_t bmc);
    void (*handleArrayAlloc)(uintptr_t ts,
                             uintptr_t returnAddr,
                             uintptr_t baa);
    void (*handleObjectAlloc)(uintptr_t ts,
                              uintptr_t returnAddr,
                              uintptr_t boa);
    void (*handleInstanceof)(uintptr_t ts,
                             uintptr_t returnAddr,
                             uintptr_t bio);
    fivmr_Object (*BackingStoreID_create)(uintptr_t ts,
                                          fivmr_Object name);
    uintptr_t (*processArgs)(uintptr_t ts,
                             int32_t argc,
                             uintptr_t argv); /* call only IN_NATIVE */
    void (*javaExit)(uintptr_t ts,
                     int32_t status);
};

struct fivmr_PayloadList_s {
    uintptr_t nPayloads;
    fivmr_Payload **payloads;
};

struct fivmr_GC_s {
    /* most of what follows is for CMRGC and HFGC */
    int32_t phase;
    uintptr_t curShaded;
    uintptr_t invCurShaded;
    uintptr_t zeroCurShaded;
    uintptr_t curShadedAlloc;

    uintptr_t gcTriggerPages;
    uintptr_t maxPagesUsed;

    fivmr_ThreadPriority threadPriority;
    
    /* begin fields specific to self-manage-mem */
    uintptr_t memStart;
    uintptr_t memEnd;
    uintptr_t nextFreePage; /* this implements the wilderness */
    bool isZero; /* was the heap zeroed at the start? */

    fivmr_GCSpaceData spaceData[FIVMR_GC_NUM_GC_SPACES];

    /* this is an HFGC-specific field */
    fivmr_SSDescr ss;

    fivmr_FreePage *freePageHead;
    intptr_t numFreePages;
    fivmr_GCHeader *globalQueue;
    fivmr_MemoryArea *areaQueue;
    fivmr_LargeObjectHeader *largeObjectHead;
    
    fivmr_MemoryAreaStack *sharedAreas;
    fivmr_Lock sharedAreasLock;

    fivmr_MachineCode *machineCodeHead;

    /* this is used just to help debugging */
    fivmr_Object anthracite;

    fivmr_Destructor *destructorHead; /* protected by gcLock, sort of; the
                                         GC only touches it when NOT
                                         requesting handshakes, while the
                                         mutator only touches it while
                                         holding gcLock *and* responding to
                                         a handshake */
                                         
    fivmr_Destructor *destructorsToRun; /* protected by destructorLock */
    fivmr_Lock destructorLock;

    fivmr_Lock gcLock;
    fivmr_Lock requestLock;
    fivmr_Lock notificationLock; /* used to get notifications of GC completion */
    fivmr_CritSemaphore triggerSema; /* async GC request trigger semaphore */
    
    bool threadDone;
    fivmr_Semaphore doneSema; /* signaled when the GC thread dies */

    fivmr_ThreadState *requesterHead;
    fivmr_ThreadState *waiterHead;
    bool asyncRequested;
    intptr_t numPagesUsed;
    uint64_t iterationCount;
    uint64_t blockedIterationCount;
    uint64_t traceIterationCount;
    fivmr_Nanos lastStart;
    fivmr_Nanos lastEnd;

    uintptr_t maxMaxPagesUsed;
    
    fivmr_ThreadHandle thread;

    /* this is a GC_DEBUG-specific field */
    uintptr_t numMarked;

    fivmr_Nanos blockTime;
    fivmr_Nanos invBlockTime; /* time spent blocking involuntarily */
    fivmr_Nanos gcThreadTime;
    fivmr_Nanos slowPathTime;
    fivmr_Nanos getPageTime;
    fivmr_Nanos getFreeLinesTime;
    fivmr_Nanos largeAllocTime;
    fivmr_Nanos freeLineSearchTime;

    bool noMoreHeapAlloc;

    bool logSyncGC;
    bool logGC;
    bool finalGCReport;
    bool abortOOME;
    bool ignoreSystemGC;
    
    uintptr_t reqPages;
    uintptr_t reusePages;
    
    /* Scoped memory support */
    /* Three fivmr_MemoryAreas (9 words each) and one int64_t of overhead
     * when scoped memory is not enabled */
    int64_t immortalMem;
    fivmr_MemoryArea stackMemoryArea;
    fivmr_MemoryArea immortalMemoryArea;
    /* This is used for representing the heap memory area, the
     * thread-local structure is used for storing allocator status when
     * pushing scopes. */
    fivmr_MemoryArea heapMemoryArea;

    /* GC functions - don't call directly. */
    void (*resetStats)(fivmr_GC *gc);
    void (*report)(fivmr_GC *gc,const char *name);
    void (*clear)(fivmr_ThreadState *ts);
    void (*startThread)(fivmr_ThreadState *ts);
    void (*commitThread)(fivmr_ThreadState *ts);
    void (*handleHandshake)(fivmr_ThreadState *ts);
    int64_t (*numIterationsCompleted)(fivmr_GC *gc);
    void (*markSlow)(fivmr_ThreadState *ts,
                     fivmr_Object obj);
    fivmr_Object (*allocRawSlow)(fivmr_ThreadState *ts,
                                 fivmr_GCSpace space,
                                 uintptr_t size,
                                 uintptr_t alignStart,
                                 uintptr_t align,
                                 fivmr_AllocEffort effort,
                                 const char *description);
    fivmr_Spine (*allocSSSlow)(fivmr_ThreadState *ts,
                               uintptr_t spineLength,
                               int32_t numEle,
                               const char *description);
    void (*claimMachineCode)(fivmr_ThreadState *ts,
                             fivmr_MachineCode *mc);
    int64_t (*freeMemory)(fivmr_GC *gc);
    int64_t (*totalMemory)(fivmr_GC *gc);
    int64_t (*maxMemory)(fivmr_GC *gc);
    void (*asyncCollect)(fivmr_GC *gc);
    void (*collectFromJava)(fivmr_GC *gc,
                            const char *descrIn,
                            const char *descrWhat);
    void (*collectFromNative)(fivmr_GC *gc,
                              const char *descrIn,
                              const char *descrWhat);
    void (*setPriority)(fivmr_GC *gc,
                        fivmr_ThreadPriority prio);
    bool (*setMaxHeap)(fivmr_GC *gc,
                       int64_t bytes);
    bool (*setTrigger)(fivmr_GC *gc,
                       int64_t bytes);
    bool (*getNextDestructor)(fivmr_GC *gc,
                              fivmr_Handle *objCell,
                              bool wait /* wait for a destructor to become
                                           available? */);
    void (*signalExit)(fivmr_GC *gc);
    void (*shutdown)(fivmr_GC *gc);
};

struct fivmr_VM_s {
    /* what follows is a function table, for use by assembly code routines,
       so that they don't have to know if we're in PIC or not.  the assembly
       will be hard-wired with fixed offsets to these - so DO NOT change
       their arrangement without some serious thought! */
    void (*resolveField)(fivmr_ThreadState *ts,
                         uintptr_t returnAddr,
                         fivmr_BaseFieldAccess *bfa);
    void (*resolveMethod)(fivmr_ThreadState *ts,
                          uintptr_t returnAddr,
                          fivmr_BaseMethodCall *bmc);
    void (*baselineThrow)(fivmr_ThreadState *ts,
                          uintptr_t framePtr,
                          uintptr_t *result);
    void (*pollcheckSlow)(fivmr_ThreadState *ts,
                          uintptr_t debugID);
    void (*throwNullPointerRTE_inJava)(fivmr_ThreadState *ts);
    void (*throwArrayBoundsRTE_inJava)(fivmr_ThreadState *ts);
    void (*resolveArrayAlloc)(fivmr_ThreadState *ts,
                              uintptr_t returnAddr,
                              fivmr_BaseArrayAlloc *baa);
    void (*resolveObjectAlloc)(fivmr_ThreadState *ts,
                               uintptr_t returnAddr,
                               fivmr_BaseObjectAlloc *boa);
    void (*resolveInstanceof)(fivmr_ThreadState *ts,
                              uintptr_t returnAddr,
                              fivmr_BaseInstanceof *bio);
    void (*throwStackOverflowRTE_inJava)(fivmr_ThreadState *ts);
    /* -- end function table -- */
    
    fivmr_Lock lock;
    
    char name[256];
    
    uintptr_t state;
    
    fivmr_VM *prev,*next;
    
    fivmr_Settings settings;
    fivmr_Configuration config;
    
    fivmr_Payload *payload;
    
    fivmr_TypeContext **baseContexts;
    
    fivmr_Lock typeDataLock; /* grab this before:
                                   adding contexts,
                                   removing contexts,
                                   modifying TypeEpoch data.
                                this is an inner lock to be grabbed after
                                grabbing context tree locks.  this is
                                because we may execute Java code while
                                holding a context lock.
                                as well, go to native when acquiring this
                                lock. */
    fivmr_Lock thunkingLock; /* outer lock that locks thunking activities */
    
    int32_t nTypes;
    fivmr_TypeContext **dynContexts;
    int32_t nDynContexts;
    int32_t dynContextsSize;
    
    /* type traversal data structures - INTERNAL to TypeData code */
    fivmr_OTH othShadow;
    fivmr_OTH othDown;
    fivmr_TypeData **wlDown;
    uintptr_t shadowResult;
    
    fivmr_TypeData **sortList;
    int32_t sortListSize;

    fivmr_OTH oth;
    fivmr_TypeData **wl;
    int32_t wlN;
    /* end type traversal data structures */
    
    fivmr_VMFlags flags;
    
    uint32_t *usedTids; /* 256 bits per bucket */
    uintptr_t numBuckets;
    
    int32_t *itableOcc;
    uintptr_t itableSize;
    
    int32_t typeEpoch;
    
    int32_t numBucketCollisions;
    int32_t numItableCollisions;

    fivmr_GC gc;
    
    double zero; /* a zero value */

    /* the max priority that any thread can have in this VM */
    fivmr_ThreadPriority maxPriority;
    
    /* if this VM is supposed to use a thread pool for all of its threads, then
       this is a pointer to the thread pool. */
    fivmr_ThreadPool *pool;
    
    /* if this VM is being time-sliced, then this is a pointer to the time slice
       that the VM belongs to. */
    fivmr_TimeSlice *timeSlice;
    
    bool exitCodeSet;
    int32_t exitCode;

    fivmr_Lock deathLock;

    int32_t monitorSpinLimit;

    const char *exceptionsFatalReason;

    fivmr_ThreadSpecific curThread;

    fivmr_HandleRegion hr; /* protected by fivmr_hrLock */
    fivmr_Lock hrLock;
    fivmr_Handle *freeHandles;

    uint32_t numThreads;
    uint32_t maxThreadID;
    fivmr_Handle *javaThreads;
    fivmr_ThreadState **threads;
    fivmr_ThreadState *threadById; /* 0, 1 are not real threads */
    uint32_t numActive,numDaemons;
    uint32_t numRunning; /* this is sort of a reference count on the VM struct;
                            it includes threads that are late enough in the
                            termination sequence that they may want to perform
                            more accesses to the VM. */

    fivmr_ThreadState **handshakeThreads;

    fivmr_Lock handshakeLock;
    fivmr_Semaphore softHandshakeNotifier;
    uint32_t softHandshakeCounter;

    bool exitExits;
    bool exiting;
    fivmr_ThreadState *exitInitiator;
    
#if FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT
    fivmr_CritSemaphore dumpStateSemaphore;
    fivmr_Semaphore dumpStateDone;
#endif

    fivmr_AllThreadStackTraces *atst_result;
    
    /* note: we will need to keep these fields coherent with the ThreadState's */
    int64_t *primFields;
    fivmr_Object *refFields;
};

struct fivmr_PooledThread_s {
    fivmr_ThreadPool *pool;
    fivmr_ThreadHandle thread;
    fivmr_SuspendableThreadHandle susp;
    uintptr_t index;
    bool active;
    fivmr_ThreadPriority activePriority;
    void *arg;
    void (*runner)(void *arg);
};

struct fivmr_ThreadPool_s {
    fivmr_Lock lock;
    fivmr_ThreadPriority defaultPriority;
    uintptr_t nthreads;
    fivmr_PooledThread *threads;
    uintptr_t *freeIndices;
    uintptr_t nfree;
};

struct fivmr_TimeSlice_s {
    bool inited;
    fivmr_Nanos duration;
    fivmr_ThreadPool *pool;
};

struct fivmr_TimeSliceManager_s {
    fivmr_ThreadPriority managerPriority;
    uintptr_t nslices;
    fivmr_TimeSlice *slices;
};

#if FIVMR_FLOW_LOGGING
struct fivmr_FlowLogHeader_s {
    uint32_t magic;
    uint16_t version;
    uint16_t platform;
};

struct fivmr_FlowLogEvent_s {
    uint16_t type;
    uint16_t subtype;
    uint32_t tid;
    uint64_t timestamp;
    uint64_t data;
#if FIVMR_FLOW_LOGGING_FATEVENTS
    uint64_t extdata;
#endif
};

/*
 * A single copy of this structure is linked to a fivmr_ThreadState
 * (that is, next should always be NULL), but a list of them is
 * maintained in the flow logging thread.  The event allocated in this
 * structure is the first of FIVMR_FLOWLOG_BUFFER_ENTRIES events in the
 * allocated structure.
 */
struct fivmr_FlowLogBuffer_s {
    fivmr_FlowLogBuffer *next;
    uint32_t entries;
    fivmr_FlowLogEvent events[1];
};

void fivmr_FlowLog_releaseTS(fivmr_ThreadState *ts);

static inline void fivmr_FlowLog_logIMPL(fivmr_ThreadState *ts, uint16_t type,
                                         uint16_t subtype, uint32_t tid,
                                         uint64_t timestamp, uint64_t data
#if FIVMR_FLOW_LOGGING_FATEVENTS
                                         , uint64_t extdata
#endif
    ) {
    fivmr_FlowLogEvent *event = ts->flowbuf->events + ts->flowbuf->entries++;
    event->type = type;
    event->subtype = subtype;
    event->tid = tid;
    event->timestamp = timestamp;
    event->data = data;
#if FIVMR_FLOW_LOGGING_FATEVENTS
    event->extdata = extdata;
#endif
    if (fivmr_unlikely(ts->flowbuf->entries == FIVMR_FLOWLOG_BUFFER_ENTRIES)) {
        fivmr_FlowLog_releaseTS(ts);
    }
}

void fivmr_FlowLog_platform_write(fivmr_FlowLogBuffer *flowbuf);
#endif /* FIVMR_FLOW_LOGGING */

extern fivmr_VM *fivmr_vmListHead;
extern fivmr_Lock fivmr_vmListLock;

extern bool fivmr_abortThrow;

struct JNINativeInterface;
extern struct JNINativeInterface fivmr_jniFunctions;

#if FIVMR_INTERNAL_INST
FIVMR_II_TYPE_DECL_AFTER
#endif

#if FIVMR_INTERNAL_INST
FIVMR_II_GLOBALS
#endif

static inline fivmr_VM *fivmr_VMfromGC(fivmr_GC *gc) {
    return (fivmr_VM*)(((uintptr_t)gc)-fivmr_offsetof(fivmr_VM,gc));
}

static inline uintptr_t *fivmr_getRefFields(fivmr_VM *vm) {
    return vm->refFields;
}

/* initialize random number generator with time-based seed */
void fivmr_Random_init(fivmr_Random *r);

/* initialize with manual seed */
void fivmr_Random_initBySeed(fivmr_Random *r,uint32_t seed);

/* initialize with key.  NOTE: use this if you want better randomness, but
   stay away from it if you can; if we ever switch random number generators,
   this method will drop. */
void fivmr_Random_initByArray(fivmr_Random *r,uint32_t *initKey,uint32_t keyLength);

void fivmr_Random_generate_slow(fivmr_Random *r);

static inline uint32_t fivmr_Random_generate32(fivmr_Random *r) {
    uint32_t y;
    
    if (r->mti>=FIVMR_RANDMT_N) {
	fivmr_Random_generate_slow(r);
    }
    
    y = r->mt[r->mti++];

    /* Tempering */
    y ^= (y >> 11);
    y ^= (y << 7) & (uint32_t)0x9d2c5680UL;
    y ^= (y << 15) & (uint32_t)0xefc60000UL;
    y ^= (y >> 18);
    
    LOG(7,("fivmr_Random_generate32 returning %u",y));

    return y;
}

static inline uint64_t fivmr_Random_generate64(fivmr_Random *r) {
    return ((uint64_t)fivmr_Random_generate32(r))
	| (((uint64_t)fivmr_Random_generate32(r))<<32);
}

static inline uintptr_t fivmr_Random_generatePtr(fivmr_Random *r) {
    if (FIVMSYS_PTRSIZE==4) {
	return fivmr_Random_generate32(r);
    } else {
	return fivmr_Random_generate64(r);
    }
}

static inline uint8_t fivmr_Random_generate8(fivmr_Random *r) {
    uint8_t result;
    uint32_t value;
    if (!r->lastLeft--) {
	value=r->last=fivmr_Random_generate32(r);
	r->lastLeft=3;
    } else {
	value=r->last;
    }
    result=(value&255);
    r->last=(value>>8);

    LOG(7,("fivmr_Random_generate8 returning %u",result));

    return result;
}

static inline bool fivmr_Settings_canDoClassLoading(fivmr_Settings *settings) {
    return FIVMR_OPEN_WORLD(settings)
        && FIVMR_CLASSLOADING(settings)
        && FIVMR_USE_TYPE_EPOCHS(settings)
        && !FIVMR_ITABLE_COMPRESSION(settings)
        && FIVMR_TRACK_DIRECT_SUBS(settings)
        && !FIVMR_CLOSED_PATCH_POINTS(settings);
}

fivmr_Payload *fivmr_Payload_copy(fivmr_Payload *payload);

/* this is an optional call; registerVM will do it for you. */
bool fivmr_Payload_claim(fivmr_Payload *payload,
                         fivmr_VM *vm);

bool fivmr_Payload_registerVM(fivmr_Payload *payload,
                              fivmr_VM *vm);

static inline bool fivmr_VM_registerPayloadWithName(fivmr_VM *vm,
                                                    fivmr_Payload *payload,
                                                    const char *name) {
    bool result=fivmr_Payload_registerVM(payload,vm);
    if (result) {
        snprintf(vm->name,sizeof(vm->name),"%s",name==NULL?payload->name:name);
        return true;
    } else {
        return false;
    }
}

static inline bool fivmr_VM_registerPayload(fivmr_VM *vm,
                                            fivmr_Payload *payload) {
    return fivmr_VM_registerPayloadWithName(vm,payload,NULL);
}

void fivmr_Payload_free(fivmr_Payload *payload);

static inline int32_t fivmr_BitField_setAtomic(int32_t *field,
                                               int32_t mask,
                                               int32_t newBits) {
    for (;;) {
        int32_t oldValue;
        int32_t newValue;
        
        oldValue=*field;
        newValue=(oldValue&~mask)|newBits;

        if (fivmr_cas32_weak(field,oldValue,newValue)) {
            return oldValue;
        }
        
        fivmr_spin_fast();
    }
}

static inline void fivmr_BitVec_set(uint32_t *bits,
				    uintptr_t index,
				    bool value) {
    if (value) {
	bits[index>>5]|=(1<<(index&31));
    } else {
	bits[index>>5]&=~(1<<(index&31));
    }
}

static inline void fivmr_BitVec_setAtomic(uint32_t *bits,
                                          uintptr_t index,
                                          bool value) {
    uint32_t *ptr=bits+(index>>5);
    for (;;) {
        uint32_t oldWord;
        uint32_t newWord;
        
        newWord=oldWord=*ptr;
        
        if (value) {
            newWord|=(1<<(index&31));
        } else {
            newWord&=~(1<<(index&31));
        }
        
        if (fivmr_cas32_weak((int32_t*)ptr,
                             (int32_t)oldWord,
                             (int32_t)newWord)) {
            return;
        }
        
        fivmr_spin_fast();
    }
}

/* this is a weak cas */
static inline bool fivmr_BitVec_cas(uint32_t *bits,
                                    uintptr_t index,
                                    bool oldValue,
                                    bool newValue) {
    uint32_t *ptr=bits+(index>>5);
    uint32_t mask=(1<<(index&31));
    
    uint32_t oldWord;
    uint32_t newWord;
    
    newWord=oldWord=*ptr;
    
    if (!!(oldWord&mask) != !!oldValue) {
        return false;
    }
    
    if (newValue) {
        newWord|=mask;
    } else {
        newWord&=~mask;
    }
    
    return fivmr_cas32_weak((int32_t*)ptr,
                            (int32_t)oldWord,
                            (int32_t)newWord);
}

static inline bool fivmr_BitVec_get(uint32_t *bits,
				    uintptr_t index) {
    return (bits[index>>5]&(1<<(index&31)))!=0;
}

static inline uintptr_t fivmr_OTH_ptrHash(uintptr_t val) {
    return val*2654435761u;
}

static inline uintptr_t fivmr_OTH_calcSize(uintptr_t len) {
    return len*sizeof(uintptr_t)*2;
}

void fivmr_OTH_init(fivmr_OTH *oth,
                    uintptr_t n);
void fivmr_OTH_initEasy(fivmr_OTH *oth,
                        uintptr_t numEle);

void fivmr_OTH_clear(fivmr_OTH *oth);

void fivmr_OTH_free(fivmr_OTH *oth);

bool fivmr_OTH_put(fivmr_OTH *oth,
                   void *key,
                   void *val);

void *fivmr_OTH_get(fivmr_OTH *oth,
                    void *key);

#ifdef FIVMBUILD_FORCE__SELF_MANAGE_MEM
#define FIVMR_PT_IS_FLAT(pt) (FIVMBUILD__SELF_MANAGE_MEM)
#else
#define FIVMR_PT_IS_FLAT(pt) ((pt)->isFlat)
#endif

void fivmr_PageTable_initFlat(fivmr_PageTable *pt,
                              uintptr_t start,
                              uintptr_t size);

void fivmr_PageTable_initML(fivmr_PageTable *pt,
                            fivmr_Priority prio);

void fivmr_PageTable_free(fivmr_PageTable *pt);
void fivmr_PageTable_freeNonZeroPages(fivmr_PageTable *pt);
void fivmr_PageTable_freePTAndNonZeroPages(fivmr_PageTable *pt);

void fivmr_PageTable_ensure(fivmr_PageTable *pt,
			    uintptr_t address);

static inline bool fivmr_PageTable_cas(fivmr_PageTable *pt,
				       uintptr_t address,
				       uint8_t oldValue,
				       uint8_t newValue) {
    if (FIVMR_PT_IS_FLAT(pt)) {
        uint32_t *bits=pt->u.flat.table;
        uintptr_t idx;
        uint32_t valueMask;
        uint32_t valueShift;
        uint32_t *ptr;
        uint32_t old;
    
        fivmr_assert(address-pt->u.flat.start < pt->u.flat.size);
    
        valueMask=(1<<FIVMR_PT_BITS_PER_PAGE)-1;
    
        idx=(((address-pt->u.flat.start)>>FIVMSYS_LOG_PAGE_SIZE)<<FIVMR_LOG_PT_BITS_PER_PAGE);
        ptr=bits+idx/32;
        valueShift=idx%32;
        old=*ptr;
    
        LOG(13,("CASing page %p, with ptr=%p, from %u to %u",
                address,ptr,oldValue,newValue));
    
        if (((old>>valueShift)&valueMask)==oldValue &&
            fivmr_cas32_weak(
                (int32_t*)ptr,old,
                (old&~(valueMask<<valueShift))|(newValue<<valueShift))) {
            return true;
        } else {
            LOG(12,("Returning false because %u doesn't have %u at index "
                    "%" PRIuPTR ", or else the CAS failed",
                    old,oldValue,idx));
            return false;
        }
    } else {
#if FIVMSYS_PTRSIZE==4
        for (;;) {
            uint32_t *bits=pt->u.ml.table[
                (address&FIVMR_PT_SPINE_MASK)>>FIVMR_PT_SPINE_SHIFT];
            if (bits!=NULL) {
                for (;;) {
                    uintptr_t idx;
                    uint32_t valueMask;
                    uint32_t valueShift;
                    uint32_t *ptr;
                    uint32_t old;
		
                    valueMask=(1<<FIVMR_PT_BITS_PER_PAGE)-1;
		
                    idx=(((address&FIVMR_PT_CHUNK_MASK)>>FIVMR_PT_CHUNK_SHIFT)
                         <<FIVMR_LOG_PT_BITS_PER_PAGE);
                    ptr=bits+idx/32;
                    valueShift=idx%32;
                    old=*ptr;

                    LOG(13,("CASing page %p, with ptr=%p, from %u to %u",
                            address,ptr,oldValue,newValue));
		
                    if (((old>>valueShift)&valueMask)==oldValue &&
                        fivmr_cas32_weak(
                            (int32_t*)ptr,old,
                            (old&~(valueMask<<valueShift))|(newValue<<valueShift))) {
                        return true;
                    } else {
                        LOG(12,("Returning false because %u doesn't have %u at index "
                                "%" PRIuPTR ", or else the CAS failed",
                                old,oldValue,idx));
                        return false;
                    }
                }
            }
            fivmr_PageTable_ensure(pt,address);
        }
#else
        for (;;) {
            uint32_t **middle=pt->u.ml.table[
                (address&FIVMR_PT_OUTER_MASK)>>FIVMR_PT_OUTER_SHIFT];
            if (middle!=NULL) {
                uint32_t *bits=middle[
                    (address&FIVMR_PT_MIDDLE_MASK)>>FIVMR_PT_MIDDLE_SHIFT];
                if (bits!=NULL) {
                    uintptr_t idx;
                    uint32_t valueMask;
                    uint32_t valueShift;
                    uint32_t *ptr;
                    uint32_t old;
		
                    valueMask=(1<<FIVMR_PT_BITS_PER_PAGE)-1;
		
                    idx=(((address&FIVMR_PT_INNER_MASK)>>FIVMR_PT_INNER_SHIFT)
                         <<FIVMR_LOG_PT_BITS_PER_PAGE);
                    ptr=bits+idx/32;
                    valueShift=idx%32;
                    old=*ptr;
		
                    if (((old>>valueShift)&valueMask)==oldValue &&
                        fivmr_cas32_weak(
                            (int32_t*)ptr,
                            (int32_t)old,
                            (int32_t)((old&~(valueMask<<valueShift))
                                      | (newValue<<valueShift)))) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            fivmr_PageTable_ensure(pt,address);
        }
#endif
    }
}

static inline uint32_t *fivmr_PageTable_getWord(fivmr_PageTable *pt,
						uintptr_t address) {
    if (FIVMR_PT_IS_FLAT(pt)) {
        uint32_t *bits=pt->u.flat.table;
        uintptr_t idx;
        uint32_t *ptr;
        
        fivmr_assert(address-pt->u.flat.start < pt->u.flat.size);
        
        idx=(((address-pt->u.flat.start)>>FIVMSYS_LOG_PAGE_SIZE)<<FIVMR_LOG_PT_BITS_PER_PAGE);
        ptr=bits+idx/32;
        
        return ptr;
    } else {
#if FIVMSYS_PTRSIZE==4
        uintptr_t ptIndex=(address&FIVMR_PT_SPINE_MASK)>>FIVMR_PT_SPINE_SHIFT;
        uint32_t *bits=pt->u.ml.table[ptIndex];
        if (bits==NULL) {
            return NULL;
        } else {
            uintptr_t idx;
            uint32_t *ptr;
            
            idx=(((address&FIVMR_PT_CHUNK_MASK)>>FIVMR_PT_CHUNK_SHIFT)<<FIVMR_LOG_PT_BITS_PER_PAGE);
            ptr=bits+idx/32;
            
            return ptr;
        }
#else
        uint32_t **middle=pt->u.ml.table[(address&FIVMR_PT_OUTER_MASK)>>FIVMR_PT_OUTER_SHIFT];
        if (middle!=NULL) {
            uint32_t *bits=middle[(address&FIVMR_PT_MIDDLE_MASK)>>FIVMR_PT_MIDDLE_SHIFT];
            if (bits!=NULL) {
                uintptr_t idx;
                uint32_t *ptr;
                
                idx=(((address&FIVMR_PT_INNER_MASK)>>FIVMR_PT_INNER_SHIFT)
                     <<FIVMR_LOG_PT_BITS_PER_PAGE);
                ptr=bits+idx/32;
                
                return ptr;
            }
        }
        return NULL;
#endif
    }
}

static inline uint8_t fivmr_PageTable_get(fivmr_PageTable *pt,
					  uintptr_t address) {
    if (FIVMR_PT_IS_FLAT(pt)) {
        uint32_t *bits=pt->u.flat.table;
        uintptr_t idx;
        uint32_t valueMask;
        uint32_t valueShift;
        uint32_t *ptr;
        uint8_t result;

        fivmr_assert(address-pt->u.flat.start < pt->u.flat.size);
    
        valueMask=(1<<FIVMR_PT_BITS_PER_PAGE)-1;
    
        idx=(((address-pt->u.flat.start)>>FIVMSYS_LOG_PAGE_SIZE)<<FIVMR_LOG_PT_BITS_PER_PAGE);
        ptr=bits+idx/32;
        valueShift=idx%32;
    
        result=((*ptr)>>valueShift)&valueMask;
    
        LOG(13,("Getting pagetable entry for address %p;"
                "bits = %p, valueMask = %u, idx = %" PRIuPTR
                ", valueShift = %u, ptr = %p, result = %u",
                address,bits,valueMask,idx,valueShift,ptr,result));
    
        return result;
    } else {
#if FIVMSYS_PTRSIZE==4
        uintptr_t ptIndex=(address&FIVMR_PT_SPINE_MASK)>>FIVMR_PT_SPINE_SHIFT;
        uint32_t *bits=pt->u.ml.table[ptIndex];
        if (bits==NULL) {
            return 0;
        } else {
            uintptr_t idx;
            uint32_t valueMask;
            uint32_t valueShift;
            uint32_t *ptr;
            uint8_t result;
	
            valueMask=(1<<FIVMR_PT_BITS_PER_PAGE)-1;
	
            idx=(((address&FIVMR_PT_CHUNK_MASK)>>FIVMR_PT_CHUNK_SHIFT)
                 <<FIVMR_LOG_PT_BITS_PER_PAGE);
            ptr=bits+idx/32;
            valueShift=idx%32;
	
            result=((*ptr)>>valueShift)&valueMask;
	
            LOG(13,("Getting pagetable entry for address %p; ptIndex = %" PRIuPTR
                    ", bits = %p, valueMask = %u, idx = %" PRIuPTR
                    ", valueShift = %u, ptr = %p, result = %u",
                    address,ptIndex,bits,valueMask,idx,valueShift,ptr,result));
	
            return result;
        }
#else
        uint32_t **middle=pt->u.ml.table[(address&FIVMR_PT_OUTER_MASK)>>FIVMR_PT_OUTER_SHIFT];
        if (middle!=NULL) {
            uint32_t *bits=middle[(address&FIVMR_PT_MIDDLE_MASK)>>FIVMR_PT_MIDDLE_SHIFT];
            if (bits!=NULL) {
                uintptr_t idx;
                uint32_t valueMask;
                uint32_t valueShift;
                uint32_t *ptr;
                uint32_t old;
	    
                valueMask=(1<<FIVMR_PT_BITS_PER_PAGE)-1;
	    
                idx=(((address&FIVMR_PT_INNER_MASK)>>FIVMR_PT_INNER_SHIFT)
                     <<FIVMR_LOG_PT_BITS_PER_PAGE);
                ptr=bits+idx/32;
                valueShift=idx%32;
                return ((*ptr)>>valueShift)&valueMask;
            }
        }
        return 0;
#endif
    }
}

static inline void fivmr_PageTable_setAssert(fivmr_PageTable *pt,
					     uintptr_t address,
					     uint8_t expectedOldValue,
					     uint8_t newValue) {
    for (;;) {
	if (FIVMR_ASSERTS_ON) {
	    uint8_t realOldValue=fivmr_PageTable_get(pt,address);
	    if (realOldValue!=expectedOldValue) {
		fivmr_Log_lockedPrintf(
		    "for address %" PRIuPTR ", expected = %u, real = %u, new = %u\n",
		    address,expectedOldValue,realOldValue,newValue);
		fivmr_assert(realOldValue==expectedOldValue);
	    }
	}
	if (fivmr_PageTable_cas(pt,address,expectedOldValue,newValue)) {
	    return;
	}
    }
}

static inline void fivmr_PageTable_set(fivmr_PageTable *pt,
				       uintptr_t address,
				       uint8_t newValue) {
    for (;;) {
	uint8_t oldValue=fivmr_PageTable_get(pt,address);
	if (fivmr_PageTable_cas(pt,address,oldValue,newValue)) {
	    return;
	}
    }
}

/* FIXME: consider out-of-lining all of the PTIterator functions, since the "fast
   path" of iteration is done using pti->chunk, and all of these functions should
   be slow path. */

/* internal function - do not call directly */
static inline void fivmr_PTIterator_setChunkImpl(fivmr_PTIterator *pti) {
    fivmr_assert(!FIVMR_PT_IS_FLAT(pti->pt));
    pti->baseAddress = pti->u.ml.cur->chunks[pti->u.ml.idx].baseAddress;
    pti->chunk       = pti->u.ml.cur->chunks[pti->u.ml.idx].chunk;
    pti->chunkLength = FIVMR_PT_CHUNK_LENGTH;
}

/* internal function - do not call directly */
static inline void fivmr_PTIterator_setPCLImpl(fivmr_PTIterator *pti) {
    fivmr_assert(!FIVMR_PT_IS_FLAT(pti->pt));
    if (pti->u.ml.cur!=NULL) {
        fivmr_assert(pti->u.ml.cur->numChunks!=0);
        pti->u.ml.idx=0;
        pti->u.ml.limit=pti->u.ml.cur->numChunks;
        fivmr_PTIterator_setChunkImpl(pti);
    }
}

void fivmr_PTIterator_init(fivmr_PTIterator *pti,
                           fivmr_PageTable *pt);

static inline bool fivmr_PTIterator_valid(fivmr_PTIterator *pti) {
    if (FIVMR_PT_IS_FLAT(pti->pt)) {
        return pti->u.flat.first;
    } else {
        return pti->u.ml.cur!=NULL;
    }
}

static inline void fivmr_PTIterator_next(fivmr_PTIterator *pti) {
    if (FIVMR_PT_IS_FLAT(pti->pt)) {
        pti->u.flat.first=false;
    } else {
        pti->u.ml.idx++;
        if (pti->u.ml.idx>=pti->u.ml.limit) {
            fivmr_assert(pti->u.ml.idx==pti->u.ml.limit);
            pti->u.ml.cur=pti->u.ml.cur->next;
            fivmr_PTIterator_setPCLImpl(pti);
        } else {
            fivmr_PTIterator_setChunkImpl(pti);
        }
    }
}

static inline uint32_t *fivmr_UsedPage_bits(fivmr_UsedPage *up,
                                            fivmr_Settings *settings) {
    return (uint32_t*)up;
}

static inline uint16_t *fivmr_UsedPage_reserved(fivmr_UsedPage *up,
                                                fivmr_Settings *settings) {
    return (uint16_t*)(((uintptr_t)up)+4*FIVMR_UP_BITS_LENGTH(settings));
}

static inline uint16_t *fivmr_UsedPage_status(fivmr_UsedPage *up,
                                              fivmr_Settings *settings) {
    return (uint16_t*)(((uintptr_t)up)+4*FIVMR_UP_BITS_LENGTH(settings)+2);
}

static inline uintptr_t fivmr_alignRaw(uintptr_t value,
				       uintptr_t align) {
    return (value+align-1)&~(align-1);
}

static inline uintptr_t fivmr_align(uintptr_t address,
				    uintptr_t align) {
    /* FIXME: there are infinitely better ways of doing this. */
    if (align>sizeof(uintptr_t)) {
	return fivmr_alignRaw(address,align);
    } else {
	return address;
    }
}

static inline fivmr_Object fivmr_GCHeader_toObject(fivmr_Settings *settings,
                                                   fivmr_GCHeader *hdr) {
    return ((uintptr_t)hdr)+FIVMR_OBJ_GC_OFFSET(settings);
}

static inline fivmr_GCHeader *fivmr_GCHeader_fromObject(fivmr_Settings *settings,
                                                        fivmr_Object obj) {
    return (fivmr_GCHeader*)(obj-FIVMR_OBJ_GC_OFFSET(settings));
}

static inline bool fivmr_GCHeader_isAlignment(fivmr_GCHeader *hdr) {
    return hdr->word==0;
}

static inline void fivmr_GCHeader_setAlignment(fivmr_GCHeader *hdr) {
    hdr->word=0;
}

static inline void fivmr_GCHeader_setImmortal(fivmr_GCHeader *hdr) {
    hdr->word=(uintptr_t)(intptr_t)-1;
}

static inline uintptr_t fivmr_GCHeader_markBits(fivmr_GCHeader *hdr) {
    return hdr->word&FIVMR_GC_MARKBITS_MASK;
}

static inline bool fivmr_GCHeader_isImmortal(fivmr_GCHeader *hdr) {
    return fivmr_GCHeader_markBits(hdr)==FIVMR_GC_MARKBITS_MASK;
}

static inline fivmr_GCHeader *fivmr_GCHeader_next(fivmr_GCHeader *hdr) {
    return (fivmr_GCHeader*)(hdr->word<<2);
}

/* NOT ATOMIC!! */
static inline void fivmr_GCHeader_setMarkBits(fivmr_GCHeader *hdr,
					      uintptr_t markBits) {
    hdr->word=(hdr->word&~FIVMR_GC_MARKBITS_MASK)|markBits;
    fivmr_assert(!fivmr_GCHeader_isAlignment(hdr));
}

/* NOT ATOMIC!! */
static inline void fivmr_GCHeader_setNext(fivmr_GCHeader *hdr,
					  fivmr_GCHeader *next) {
    hdr->word=(hdr->word&FIVMR_GC_MARKBITS_MASK)|(((uintptr_t)next)>>2);
    fivmr_assert(!fivmr_GCHeader_isAlignment(hdr));
}

static inline void fivmr_GCHeader_set(fivmr_GCHeader *hdr,
				      uintptr_t markBits,
				      fivmr_GCHeader *next) {
    hdr->word=(((uintptr_t)markBits)|(((uintptr_t)next)>>2));
}

static inline void fivmr_GCHeader_setScopeID(fivmr_GCHeader *hdr,
                                             fivmr_ScopeID *s) {
    hdr->word=((uintptr_t)s)>>2;
}

static inline bool fivmr_GCHeader_isScopeID(fivmr_GCHeader *hdr) {
    return fivmr_GCHeader_markBits(hdr)==0;
}

static inline fivmr_ScopeID *fivmr_GCHeader_frame(fivmr_GCHeader *hdr) {
    return (fivmr_ScopeID*)(hdr->word<<2);
}

static inline bool fivmr_GCHeader_cas(fivmr_GCHeader *hdr,
				      fivmr_GCHeader *oldHdr,
				      fivmr_GCHeader *newHdr) {
    return fivmr_cas(&hdr->word,
		     oldHdr->word,
		     newHdr->word);
}

static inline uintptr_t fivmr_GCHeader_canonicalizeScope(fivmr_GCHeader *hdr) {
    if (hdr->word >= ((uintptr_t)1)<<(sizeof(void*)*8-2)) {
        return (uintptr_t)(intptr_t)-1;
    } else {
        return hdr->word;
    }
}

static inline void fivmr_FreeLine_zero(fivmr_FreeLine *line) {
    line->next=NULL;
    line->prev=NULL;
    line->lastOnPage=NULL;
    line->size=0;
}

void fivmr_VM_resetSettings(fivmr_VM *vm,
                            fivmr_Configuration *config);

static inline void fivmr_VM_useThreadPool(fivmr_VM *vm,
                                          fivmr_ThreadPool *pool) {
    vm->pool=pool;
    vm->maxPriority=pool->defaultPriority;
}

static inline void fivmr_VM_useTimeSlice(fivmr_VM *vm,
                                         fivmr_TimeSlice *ts) {
    fivmr_VM_useThreadPool(vm,ts->pool);
    vm->timeSlice=ts;
}

void fivmr_GC_resetSettings(fivmr_GC *gc); /* internal function */
void fivmr_GC_registerPayload(fivmr_GC *gc); /* internal function */

void fivmr_GC_init(fivmr_GC *gc);

static inline void fivmr_GC_signalExit(fivmr_GC *gc) {
    gc->signalExit(gc);
}

static inline void fivmr_GC_shutdown(fivmr_GC *gc) {
    gc->shutdown(gc);
}

static inline void fivmr_GC_resetStats(fivmr_GC *gc) {
    gc->resetStats(gc);
}
static inline void fivmr_GC_report(fivmr_GC *gc,
                                   const char *name) {
    gc->report(gc,name);
}

static inline void fivmr_GC_finalReport(fivmr_GC *gc) {
    if (gc->finalGCReport) {
        fivmr_GC_report(gc,"final");
    }
}

static inline void fivmr_GC_clear(fivmr_ThreadState *ts) {
    ts->vm->gc.clear(ts);
}
/* call when thread lock held */
static inline void fivmr_GC_startThread(fivmr_ThreadState *ts) {
    ts->vm->gc.startThread(ts);
}
/* call when thread lock not held */
static inline void fivmr_GC_commitThread(fivmr_ThreadState *ts) {
    ts->vm->gc.commitThread(ts);
}

/* this gets called while the lock is held. */
static inline void fivmr_GC_handleHandshake(fivmr_ThreadState *ts) {
    ts->vm->gc.handleHandshake(ts);
}

static inline int64_t fivmr_GC_numIterationsCompleted(fivmr_GC *gc) {
    return gc->numIterationsCompleted(gc);
}

static inline bool fivmr_GC_hasBeenTriggered(fivmr_GC *gc) {
    if (FIVMR_NOGC(&fivmr_VMfromGC(gc)->settings)) {
        return false;
    } else {
        return gc->requesterHead!=NULL
            || gc->asyncRequested
            || gc->waiterHead!=NULL;
    }
}

static inline bool fivmr_GC_shouldBeRunning(fivmr_GC *gc) {
    if (FIVMR_NOGC(&fivmr_VMfromGC(gc)->settings)) {
        return false;
    } else {
        return gc->numPagesUsed>(intptr_t)gc->gcTriggerPages
            || fivmr_GC_hasBeenTriggered(gc);
    }
}

static inline bool fivmr_GC_isTracing(fivmr_GCPhase phase) {
    return phase>=FIVMR_GCP_INIT && phase<=FIVMR_GCP_TRACE;
}

static inline bool fivmr_GC_isCollecting(fivmr_GCPhase phase) {
    return phase!=FIVMR_GCP_IDLE;
}

static inline bool fivmr_GC_inProgress(fivmr_GC *gc) {
    if (FIVMR_NOGC(&fivmr_VMfromGC(gc)->settings)) {
        return false;
    } else {
        return fivmr_GC_isCollecting(gc->phase);
    }
}

static inline void fivmr_GC_markSlow(fivmr_ThreadState *ts,
                                     fivmr_Object obj) {
    ts->vm->gc.markSlow(ts,obj);
}

static inline void fivmr_GC_markImpl(fivmr_ThreadState *ts,
				     fivmr_Object obj,
				     uintptr_t invCurShaded) {
    if (!FIVMR_NOGC(&ts->vm->settings)) {
        if (fivmr_unlikely(
                obj &&
                (fivmr_GCHeader_fromObject(&ts->vm->settings,
                                           obj)->word
                 >> FIVMR_GC_MARKBITS_SHIFT)
                == invCurShaded)) {
            fivmr_GC_markSlow(ts,obj);
        }
    }
}

static inline void fivmr_GC_mark(fivmr_ThreadState *ts,
				 fivmr_Object obj) {
    fivmr_GC_markImpl(ts,obj,ts->gc.invCurShaded);
}

static inline void fivmr_GC_storeBarrier(fivmr_ThreadState *ts,
					 fivmr_Object *ptr,
					 fivmr_Object value) {
    if (!FIVMR_NOGC(&ts->vm->settings)) {
        fivmr_Object oldVal;
        uintptr_t invCurShaded;
        invCurShaded=ts->gc.invCurShaded;
        if (FIVMR_GC_BLACK_STACK(&ts->vm->settings)) {
            oldVal=*ptr;
            /* this kills my performance on _209_db. */
            fivmr_GC_markImpl(ts,oldVal,invCurShaded);
        }
        fivmr_GC_markImpl(ts,value,invCurShaded);
    }
}

static inline void fivmr_GC_store(fivmr_ThreadState *ts,
				  fivmr_Object *ptr,
				  fivmr_Object value,
				  int32_t mask) {
    if ((mask&FIVMR_FBF_NOT_A_REFERENCE)==0) {
	fivmr_GC_storeBarrier(ts,ptr,value);
    }
    *ptr=value;
}

static inline void fivmr_GC_storeDefMask(fivmr_ThreadState *ts,
                                         fivmr_Object *ptr,
                                         fivmr_Object value) {
    fivmr_GC_store(ts,ptr,value,0);
}

static inline bool fivmr_GC_weakCAS(fivmr_ThreadState *ts,
				    fivmr_Object *ptr,
				    fivmr_Object comparand,
				    fivmr_Object value,
				    int32_t mask) {
    if ((mask&FIVMR_FBF_NOT_A_REFERENCE)==0) {
	fivmr_GC_storeBarrier(ts,ptr,value);
    }
    return fivmr_cas_weak(ptr,comparand,value);
}

/* how should queues work, if the priority of threads is uncertain? */
void fivmr_ThreadQueue_init(fivmr_ThreadQueue *queue);

void fivmr_ThreadQueue_enqueue(fivmr_ThreadQueue *queue,
                               fivmr_ThreadState *ts);

static inline bool fivmr_ThreadQueue_empty(fivmr_ThreadQueue *queue) {
    fivmr_assert((queue->head==NULL)==(queue->tail==NULL));
    return queue->head==NULL;
}

static inline bool fivmr_ThreadQueue_isQueued(fivmr_ThreadQueue *queue,
					      fivmr_ThreadState *ts) {
    return ts->forMonitor.queuedOnReal==queue;
}

fivmr_ThreadState *fivmr_ThreadQueue_dequeue(fivmr_ThreadQueue *queue);

static inline fivmr_ThreadState*
fivmr_ThreadQueue_peek(fivmr_ThreadQueue *queue) {
    return queue->head;
}

bool fivmr_ThreadQueue_remove(fivmr_ThreadQueue *queue,
			      fivmr_ThreadState *bt);

/* clear intent to be on a queue.  do this after being dequeued or removing
   yourself and before attempting to enqueue again. */
void fivmr_ThreadQueue_eueuqne(fivmr_ThreadQueue *queue,
                               fivmr_ThreadState *bt);

/* internal method - do not use */
void fivmr_ThreadQueue_poke(fivmr_ThreadQueue *queue,
                            fivmr_ThreadState *bt);

static inline const char *fivmr_GCSpace_name(fivmr_GCSpace space) {
    switch (space) {
    case FIVMR_GC_OBJ_SPACE: return "SOS";
    case FIVMR_GC_SA_SPACE: return "SAS";
    default: fivmr_abortf("bad space: %d",space); return NULL;
    }
}

static inline fivmr_ObjHeader *fivmr_ObjHeader_forObject(fivmr_Settings *settings,
                                                         fivmr_Object obj) {
    return (fivmr_ObjHeader*)(obj-FIVMR_OBJ_TD_OFFSET(settings));
}

static inline fivmr_Object fivmr_ObjHeader_toObject(fivmr_Settings *settings,
                                                    fivmr_ObjHeader *h) {
    return ((fivmr_Object)h)+FIVMR_OBJ_TD_OFFSET(settings);
}

static inline void fivmr_ObjHeader_init(fivmr_Settings *settings,
                                        fivmr_ObjHeader *header,
					fivmr_Monitor *monitor,
					fivmr_ObjHeadFlags flags) {
    if (FIVMR_HM_NARROW(settings)) {
        header->word=(uintptr_t)monitor;
    } else if (FIVMR_HM_POISONED(settings)) {
        header->word=((uintptr_t)monitor)+1;
    } else {
        fivmr_assert(!"bad header model");
    }
}

static inline bool fivmr_ObjHeader_cas(fivmr_Settings *settings,
                                       fivmr_ObjHeader *header,
				       fivmr_Monitor *oldMonitor,
				       fivmr_Monitor *newMonitor) {
    if (FIVMR_HM_NARROW(settings)) {
        return fivmr_cas((uintptr_t*)header,
                         (uintptr_t)oldMonitor,
                         (uintptr_t)newMonitor);
    } else if (FIVMR_HM_POISONED(settings)) {
        return fivmr_cas((uintptr_t*)header,
                         ((uintptr_t)oldMonitor)+1,
                         ((uintptr_t)newMonitor)+1);
    } else {
        fivmr_assert(!"bad header model");
        return false;
    }
}

static inline fivmr_Monitor *fivmr_ObjHeader_getMonitor(fivmr_Settings *settings,
                                                        fivmr_ObjHeader *header) {
    if (FIVMR_HM_NARROW(settings)) {
        return (fivmr_Monitor*)header->word;
    } else if (FIVMR_HM_POISONED(settings)) {
        return (fivmr_Monitor*)(header->word-1);
    } else {
        fivmr_assert(!"bad header model");
        return false;
    }
}

static inline fivmr_TypeData *fivmr_ObjHeader_getTypeData(fivmr_Settings *settings,
                                                          fivmr_ObjHeader *header) {
    fivmr_Monitor *m=fivmr_ObjHeader_getMonitor(settings,header);
    fivmr_assert(m->forward==(fivmr_TypeData*)m ^ m->state!=FIVMR_MS_INVALID);
    return m->forward;
}

/* fivmr_stampGCBits() is called only by large object allocation in the
 * C-side slow path.  It is therefore provided only for reference for
 * the NOGC case, since this path does not exist.
 *
 * In addition, since it is used only by large object allocation, we do
 * not need to handle scoped memory here (MemoryAreas have no alternate
 * path for large object allocation). */
static inline void fivmr_stampGCBits(fivmr_GC *gc,
                                     fivmr_GCSpace space,
                                     fivmr_ScopeID *s,
                                     fivmr_Object obj) {
    if (space==FIVMR_GC_OBJ_SPACE) {
        if (FIVMR_NOGC(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_GCHeader_setImmortal(
                fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                          obj));
        } else {
            fivmr_GCHeader_set(
                fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                          obj),
                gc->curShadedAlloc,
                NULL);
        }
    } else {
	fivmr_GCHeader_setScopeID(
	    fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                      obj),
	    s);
    }
}

static inline void fivmr_stampObjHeader(fivmr_Settings *settings,
                                        uintptr_t obj,
                                        fivmr_TypeData *td) {
    fivmr_ObjHeader_init(
        settings,
        fivmr_ObjHeader_forObject(settings,obj),
        (fivmr_Monitor*)td,
        FIVMR_OHF_ZERO);
}

static inline fivmr_Spine *fivmr_Object_getSpinePointer(fivmr_Object o) {
    return (fivmr_Spine*)(o-FIVMR_FRAG_OBJ_FH_OFFSET);
}

static inline fivmr_Spine fivmr_Spine_forObject(fivmr_Object o) {
    return *fivmr_Object_getSpinePointer(o);
}

static inline fivmr_Spine fivmr_Object_getSpine(fivmr_Object o) {
    return fivmr_Spine_forObject(o);
}

static inline void fivmr_Object_setSpine(fivmr_Object o,
                                         fivmr_Spine spine) {
    *fivmr_Object_getSpinePointer(o)=spine;
}

static inline fivmr_Spine *fivmr_Spine_getForwardPointer(fivmr_Spine spine) {
    return (fivmr_Spine*)(spine-FIVMR_FRAG_SP_FR_OFFSET);
}

static inline fivmr_Spine fivmr_Spine_getForward(fivmr_Spine spine) {
    return *fivmr_Spine_getForwardPointer(spine);
}

static inline void fivmr_Spine_setForward(fivmr_Spine spine,
                                          fivmr_Spine forward) {
    *fivmr_Spine_getForwardPointer(spine)=forward;
}

void fivmr_throwOOME(fivmr_ThreadState *ts);

/* low level GC allocation function - don't use directly.
   semantics:
   - allocates the amount of memory you asked for, no more.
   - does not add or stamp GC header.
   - returns the pointer to object base + FIVMR_ALLOC_OFFSET
   - alignStart is offset to alignment from where the object pointer points */
static inline uintptr_t fivmr_GC_allocRawSlow(fivmr_ThreadState *ts,
                                              fivmr_GCSpace space,
                                              uintptr_t size,
                                              uintptr_t alignStart,
                                              uintptr_t align,
                                              fivmr_AllocEffort effort,
                                              const char *description) {
    return ts->vm->gc.allocRawSlow(
        ts,space,size,alignStart,align,effort,description);
}

static inline bool fivmr_MemoryArea_inScope(fivmr_ThreadState *ts) {
    /* Note that this captures @StackAllocation and immortal scope,
     * as well as "regular" scopes, but does not recognize the NOGC
     * "heap" as the immortal scope */
    return ts->gc.currentArea!=ts->gc.baseStackEntry.area;
}

static inline void fivmr_throwOutOfMemoryError_inJava(fivmr_ThreadState *ts);

static inline uintptr_t fivmr_allocRawType_IMPL(fivmr_ThreadState *ts,
                                                uintptr_t size,
                                                const char *typeName) {
    /* NOTE: bump pointer is offset by FIVMR_ALLOC_OFFSET, so we need to
       compensate, which we do below. */
    /* FIXME: we don't need the GCHeader on raw types anymore. */
    uintptr_t result;
    uintptr_t newBump;
    fivmr_GCSpaceAlloc *alloc;
    LOG(13,("fivmr_allocRawType(%p, %p) called",
	    ts,size));
    alloc=ts->gc.alloc+FIVMR_GC_OBJ_SPACE;
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&fivmr_MemoryArea_inScope(ts)
        &&!ts->gc.currentArea->shared) {
        size+=FIVMR_RAWTYPE_HEADERSIZE(&ts->vm->settings);
    }
    size=fivmr_alignRaw(size,sizeof(uintptr_t));
    size=fivmr_alignRaw(size,FIVMR_OBJ_SIZE_ALIGN(&ts->vm->settings));
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&ts->gc.currentArea->shared) {
        for (;;) {
            result=ts->gc.currentArea->bump;
            newBump=result+size;
            if (newBump - ts->gc.currentArea->start > ts->gc.currentArea->size) {
                fivmr_throwOutOfMemoryError_inJava(ts);
            }
            if (fivmr_cas_weak(&ts->gc.currentArea->bump,result,newBump)) {
                bzero((void *)result, newBump - result);
                break;
            }
        }
    } else {
        result=alloc->bump;
        newBump=result+size;
        if (newBump - alloc->start > alloc->size) {
            result=fivmr_GC_allocRawSlow(ts,FIVMR_GC_OBJ_SPACE,size,0,sizeof(uintptr_t),
                                         FIVMR_AE_MUST_SUCCEED,
                                         typeName);
            if (!result) {
                return 0;
            }
        } else {
            alloc->bump=newBump;
        }
    }
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&fivmr_MemoryArea_inScope(ts)
        &&!ts->gc.currentArea->shared) {
        fivmr_GCHeader *hdr=
            (fivmr_GCHeader *)(result-FIVMR_OBJ_GC_OFFSET(&ts->vm->settings));
        /* Fake GCHeader containing 0b01<size> */
        hdr->word=FIVMR_GC_SH_MARK1|size;
        result+=FIVMR_RAWTYPE_HEADERSIZE(&ts->vm->settings);
    }
    result-=FIVMR_ALLOC_OFFSET(&ts->vm->settings);
    LOG(12,("fivmr_allocRawType(%p, %p) returning %p",
	    ts,size,result));
    return result;
}

#define fivmr_allocRawType(ts,type) \
    ((type*)(fivmr_allocRawType_IMPL((ts),sizeof(type),#type)))

static inline uintptr_t fivmr_GC_allocSSSlow(fivmr_ThreadState *ts,
                                             uintptr_t spineLength,
                                             int32_t numEle,
                                             const char *description) {
    return ts->vm->gc.allocSSSlow(ts,spineLength,numEle,description);
}

void fivmr_SA_init(fivmr_ThreadState *ts);
void fivmr_SA_destroy(fivmr_ThreadState *ts);
void fivmr_SA_clear(fivmr_ThreadState *ts);

/* helper for Java code */
static inline fivmr_Destructor *fivmr_allocDestructorSlow(fivmr_ThreadState *ts) {
    return fivmr_allocRawType(ts,fivmr_Destructor);
}

static inline void fivmr_addDestructor(fivmr_ThreadState *ts,
				       fivmr_Object object) {
    if (ts->gc.currentArea==&ts->gc.heapMemoryArea) {
        fivmr_Destructor *d;
        ts->roots[0]=object;
        d=fivmr_allocRawType(ts,fivmr_Destructor);
        ts->roots[0]=0;
        if (d==0) {
            return;
        }
        LOG(1,("allocated destructor %p for object %p",d,object));
        d->object=object;
        d->next=ts->gc.destructorHead;
        ts->gc.destructorHead=d;
        if (ts->gc.destructorTail==NULL) {
            ts->gc.destructorTail=d;
        }
    }
}

static inline uintptr_t fivmr_Frame_getRef(fivmr_Frame *f,
                                           fivmr_FrameType ft,
                                           uintptr_t idx) {
    if (FIVMR_CAN_DO_CLASSLOADING) {
        switch (ft) {
        case FIVMR_FT_HENDERSON:
            return f->refs[idx];
        case FIVMR_FT_BASELINE:
            fivmr_assert(FIVMR_X86);
            return ((uintptr_t*)f)[-idx-1];
        default:
            fivmr_assert(!"bad frame type");
            return 0;
        }
    } else {
        return f->refs[idx];
    }
}

static inline int32_t fivmr_Object_hashCode(fivmr_Settings *settings,
                                            fivmr_Object object) {
    if (FIVMR_DUMB_HASH_CODE(settings)) {
        return 0;
    } else {
        return object/FIVMR_MIN_OBJ_ALIGN(settings);
    }
}

static inline bool fivmr_MonState_queued(fivmr_MonState old) {
    return (old&FIVMR_MS_QUEUED)!=0;
}

static inline bool fivmr_MonState_rtQueued(fivmr_MonState old) {
    return (old&FIVMR_MS_RT_QUEUED)!=0;
}

static inline bool fivmr_MonState_biased(fivmr_MonState old) {
    return (old&FIVMR_MS_UNBIASED)==0;
}

static inline uint32_t fivmr_MonState_thread(fivmr_MonState old) {
    return (old&FIVMR_MS_TID_MASK)>>FIVMR_MS_TID_SHIFT;
}

static inline bool fivmr_MonState_mustQueue(fivmr_MonState old) {
    return fivmr_MonState_rtQueued(old);
}

static inline bool fivmr_MonState_available(fivmr_MonState old) {
    return !fivmr_MonState_biased(old)
        && fivmr_MonState_thread(old)==0
        && !fivmr_MonState_rtQueued(old);
}

/* return the recursion count.  if the lock is biased, then the recursion
   count works as expected (0 = not held, >0 means held).  but if the lock
   is unbiased (fast), then 0 is never returned ... i.e. to check if
   the lock is held compare the thread against 0. */
static inline uint32_t fivmr_MonState_rc(fivmr_MonState old) {
    if (fivmr_MonState_biased(old)) {
        return (old&FIVMR_MS_RC_MASK)>>FIVMR_MS_RC_SHIFT;
    } else {
        fivmr_assert(fivmr_MonState_thread(old)!=0);
        return ((old&FIVMR_MS_RC_MASK)>>FIVMR_MS_RC_SHIFT)+1;
    }
}

static inline uint32_t fivmr_MonState_realRC(fivmr_MonState old) {
    if (fivmr_MonState_thread(old)>=2) {
	return fivmr_MonState_rc(old);
    } else {
	return 0;
    }
}

static inline fivmr_MonState fivmr_MonState_withQueued(fivmr_MonState old,
						       bool queued) {
    fivmr_MonState result;
    if (queued) {
	result=old|FIVMR_MS_QUEUED;
    } else {
	result=old&~FIVMR_MS_QUEUED;
    }
    return result;
}

static inline fivmr_MonState fivmr_MonState_withRTQueued(fivmr_MonState old,
                                                         bool rtQueued) {
    fivmr_MonState result;
    if (rtQueued) {
	result=old|FIVMR_MS_RT_QUEUED;
    } else {
	result=old&~FIVMR_MS_RT_QUEUED;
    }
    return result;
}

static inline fivmr_MonState fivmr_MonState_withThread(fivmr_MonState old,
						       uint32_t threadId) {
    fivmr_MonState result=
        (old&~FIVMR_MS_TID_MASK)
	| (((fivmr_MonState)threadId)<<FIVMR_MS_TID_SHIFT);
    return result;
}

static inline fivmr_MonState fivmr_MonState_withRC(fivmr_MonState old,
						   uint32_t rc) {
    fivmr_MonState result;
    if (fivmr_MonState_biased(old)) {
        result=(old&~FIVMR_MS_RC_MASK)
            | (((fivmr_MonState)rc)<<FIVMR_MS_RC_SHIFT);
    } else {
        result=(old&~FIVMR_MS_RC_MASK)
            | (((fivmr_MonState)(rc-1))<<FIVMR_MS_RC_SHIFT);
    }
    return result;
}

static inline fivmr_MonState fivmr_MonState_incRC(fivmr_MonState old) {
    fivmr_MonState result=old+(((fivmr_MonState)1)<<FIVMR_MS_RC_SHIFT);
    return result;
}

static inline fivmr_MonState fivmr_MonState_decRC(fivmr_MonState old) {
    fivmr_MonState result=old-(((fivmr_MonState)1)<<FIVMR_MS_RC_SHIFT);
    return result;
}

static inline fivmr_MonState fivmr_MonState_withBiased(fivmr_MonState old,
                                                       bool biased) {
    fivmr_MonState result=old;
    if (fivmr_MonState_biased(result)!=biased) {
        if (biased) {
            /* going fast -> biased.  this happens only on initialization. */
            result=old&~FIVMR_MS_UNBIASED;
            if (fivmr_MonState_thread(old)!=0) {
                result=fivmr_MonState_incRC(result);
                fivmr_assert(fivmr_MonState_rc(result)!=0);
            }
        } else {
            /* going biased -> fast */
            result=old|FIVMR_MS_UNBIASED;
            if (fivmr_MonState_realRC(old)!=0) {
                result=fivmr_MonState_decRC(result);
            } else {
                result=fivmr_MonState_withThread(result,0);
            }
        }
    }
    fivmr_assert(fivmr_MonState_realRC(old)==fivmr_MonState_realRC(result));
    LOG(5,("Changing state from %" PRIuPTR " to %" PRIuPTR ".",
           old,result));
    return result;
}

void fivmr_MonState_describe(fivmr_MonState state,
                             char *buf,
                             size_t bufsize);

void fivmr_Monitor_lock_slow(fivmr_ObjHeader *head,
			     fivmr_ThreadState *ts);

void fivmr_Monitor_unlockInflated(fivmr_Monitor *monitor,
				  fivmr_ThreadState *ts);

void fivmr_Monitor_unlock_slow(fivmr_ObjHeader *head,
			       fivmr_ThreadState *ts);

bool fivmr_Object_lock(fivmr_ThreadState *ts,
                       fivmr_Object obj);

bool fivmr_Object_unlock(fivmr_ThreadState *ts,
                         fivmr_Object obj);

/* ensure that we have a real monitor, rather than a dummy one. */
fivmr_Monitor *fivmr_Monitor_inflate(fivmr_ObjHeader *head,
				     fivmr_ThreadState *ts);

static inline void fivmr_Monitor_assertInflated(fivmr_Monitor *monitor) {
    fivmr_assert(monitor->forward!=(fivmr_TypeData*)monitor);
    fivmr_assert(monitor->state!=FIVMR_MS_INVALID);
}

static inline bool fivmr_Monitor_queuedShouldBeSet(fivmr_Monitor *monitor) {
    return monitor->queues!=NULL
	&& (!fivmr_ThreadQueue_empty(&monitor->queues->entering));
}

static inline bool fivmr_Monitor_queuesEmpty(fivmr_Monitor *monitor) {
    return monitor->queues==NULL
	|| (fivmr_ThreadQueue_empty(&monitor->queues->entering) &&
	    fivmr_ThreadQueue_empty(&monitor->queues->waiting));
}

bool fivmr_Monitor_ensureQueues(fivmr_ThreadState *ts,
                                fivmr_Monitor *monitor,
                                fivmr_ObjHeader *head);

fivmr_ThreadState *fivmr_Monitor_curHolder(fivmr_VM *vm,
                                           fivmr_Monitor *monitor);

int32_t fivmr_Monitor_rc(fivmr_Monitor *monitor);

fivmr_ThreadState *fivmr_Object_curHolder(fivmr_VM *vm,
                                          fivmr_Object obj);

int32_t fivmr_Object_recCount(fivmr_VM *vm,
                              fivmr_Object obj);

uint32_t fivmr_Monitor_unlockCompletely(fivmr_Monitor *monitor,
					fivmr_ThreadState *ts);

void fivmr_Monitor_relock(fivmr_ObjHeader *head,
			  fivmr_ThreadState *ts,
			  uint32_t recCount);

void fivmr_Monitor_setStateBit(fivmr_Monitor *monitor,
                               fivmr_MonState bit,
                               bool value);

void fivmr_Monitor_pokeRTQueued(fivmr_Monitor *monitor);

void fivmr_Monitor_unbiasWhenHeld(fivmr_Monitor *monitor,
				  fivmr_ThreadState *ts);

void fivmr_Monitor_unbiasIfBiasedToMe(fivmr_Monitor *monitor,
                                      fivmr_ThreadState *ts);

void fivmr_Monitor_unbiasFromHandshake(fivmr_ThreadState *ts);

void fivmr_Monitor_unbias(fivmr_ObjHeader *head);

/* wait functions.  note that these don't do anything to ts->interrupted
   other than check it. */
void fivmr_Monitor_wait(fivmr_ObjHeader *head,
			fivmr_ThreadState *ts);

void fivmr_Monitor_timedWait(fivmr_ObjHeader *head,
			     fivmr_ThreadState *ts,
			     fivmr_Nanos whenAwake);

/* Notify one thread waiting on the monitor.  Returns true if there was a
   thread that got notified, or false otherwise. */
bool fivmr_Monitor_notify(fivmr_ThreadState *ts,
                          fivmr_Monitor *monitor);

/* This is a convenience function, equivalent to calling notify() until it
   returns false.  Returns true if notify() ever returned true.  I.e. false
   is returned if there was nobody waiting, or true is returned if there
   was one or more thread waiting. */
bool fivmr_Monitor_notifyAll(fivmr_ThreadState *ts,
                             fivmr_Monitor *monitor);

static inline void fivmr_HandleRegion_init(fivmr_HandleRegion *hr) {
    hr->head.obj=0;
    hr->head.prev=NULL;
    hr->head.next=&hr->tail;
    hr->tail.obj=0;
    hr->tail.prev=&hr->head;
    hr->tail.next=NULL;
}

/* call only while IN_JAVA.  that's so that if you're IN_NATIVE, the GC
   can assume that the handle region and freelist are immutable. */
fivmr_Handle *fivmr_HandleRegion_add(fivmr_HandleRegion *hr,
				     fivmr_ThreadState *ts,
				     fivmr_Handle **freelist,
				     fivmr_Object obj);

static void fivmr_Handle_set(fivmr_Handle *h,
                             fivmr_ThreadState *ts,
                             fivmr_Object obj){
    fivmr_GC_mark(ts,obj);
    h->vm=ts->vm;
    h->obj=obj;
}

/* make sure that you have exclusive access to the handle region.  so,
   for handles on the global table, lock the global lock.  also make sure
   that you're IN_JAVA when you do this. */
void fivmr_Handle_remove(fivmr_Handle **freelist,
			 fivmr_Handle *handle);

/* make sure you call this only when you're IN_JAVA */
void fivmr_HandleRegion_removeAll(fivmr_HandleRegion *hr,
				  fivmr_Handle **freelist);

static inline fivmr_Object fivmr_Handle_get(fivmr_Handle *h) {
    if (h==NULL) {
	return 0;
    } else {
	return h->obj;
    }
}

static inline int32_t fivmr_Handle_hashCode(fivmr_Handle *h) {
    /* FIXME: this assumes non-moving */
    return fivmr_Object_hashCode(&h->vm->settings,h->obj);
}

static inline fivmr_VM *fivmr_Handle_getVM(fivmr_Handle *h) {
    return h->vm;
}

static inline fivmr_Value fivmr_Handle_asValue(fivmr_Handle *h) {
    fivmr_Value result;
    result.H=h;
    return result;
}

static inline fivmr_Value fivmr_Object_asValue(fivmr_Object o) {
    fivmr_Value result;
    result.L=o;
    return result;
}

static inline fivmr_Value fivmr_NullValue(void) {
    fivmr_Value result;
    bzero(&result,sizeof(result));
    return result;
}

static inline void fivmr_JNIEnv_init(fivmr_JNIEnv *jni,
                                     fivmr_ThreadState *ts,
                                     fivmr_MethodRec *mr,
                                     fivmr_TypeContext *ctx) {
    jni->functions=(void*)&fivmr_jniFunctions;
    jni->ts=ts;
    jni->mr=mr;
    jni->ctx=ctx;
}

static inline void fivmr_NativeFrame_init(fivmr_NativeFrame *nf,
                                          fivmr_ThreadState *ts,
                                          fivmr_MethodRec *mr,
                                          fivmr_TypeContext *ctx) {
    nf->up=NULL;
    fivmr_HandleRegion_init(&nf->hr);
    fivmr_JNIEnv_init(&nf->jni,ts,mr,ctx);
}

static inline void fivmr_NativeFrame_destroy(fivmr_NativeFrame *nf,
                                             fivmr_ThreadState *ts) {
    /* leave nf->up intact to make life easier */
    fivmr_HandleRegion_removeAll(&nf->hr,
				 &ts->freeHandles);
}

static inline fivmr_Handle *fivmr_NativeFrame_addHandle(fivmr_NativeFrame *nf,
							fivmr_ThreadState *ts,
							fivmr_Object obj) {
    return fivmr_HandleRegion_add(&nf->hr,ts,&ts->freeHandles,obj);
}

/* initialize global data structures needed for creating VMs */
void fivmr_VM_initGlobal(void);

/* initialize the C-side runtime assuming that log and sysdep are
   initialized.  note that if the VM was asynchronously exited during
   the intialization procedure, then we still perform a mock initialization
   but immediately throw the VM into exit mode; thus any operations you
   attempt to perform on the VM will immediately fail and signal that
   exit is in progress. */
void fivmr_VM_init(fivmr_VM *vm);

/* returns true if there are no more threads running but the VM is still active,
   or false if the VM has been force-exited. */
bool fivmr_VM_waitForDeath(fivmr_VM *vm);

/* A boosted spinlock is the safest way of locking shared state in the
   system.  This essentially acquires two locks at once: the lock on the
   given thread (preventing its priority from changing) and the given
   spinlock. */
void fivmr_BoostedSpinLock_init(fivmr_BoostedSpinLock *bsl);
void fivmr_BoostedSpinLock_destroy(fivmr_BoostedSpinLock *bsl);
void fivmr_BoostedSpinLock_lock(fivmr_ThreadState *ts,
                                fivmr_BoostedSpinLock *bsl);
void fivmr_BoostedSpinLock_unlock(fivmr_ThreadState *ts,
                                  fivmr_BoostedSpinLock *bsl);

static inline bool fivmr_ThreadState_isValid(fivmr_ThreadState *ts) {
    return ts->cookie==0xd1e7c0c0;
}

static inline fivmr_ThreadState *fivmr_ThreadState_byId(fivmr_VM *vm,
                                                        uint32_t id) {
    return vm->threadById+id;
}

fivmr_ThreadState *fivmr_ThreadState_getNullable(fivmr_VM *vm);
fivmr_ThreadState *fivmr_ThreadState_get(fivmr_VM *vm);

static inline void fivmr_ThreadState_setThread(fivmr_ThreadState *ts,
                                               fivmr_ThreadHandle th) {
    ts->thread=th;
}

static inline void fivmr_ThreadState_setInitPrio(fivmr_ThreadState *ts,
                                                 fivmr_ThreadPriority pr) {
    fivmr_assert(fivmr_ThreadPriority_leRT(pr,ts->vm->maxPriority));
    ts->curPrio=pr;
    ts->basePrio=pr;
    ts->curTempBoostPrio=pr;
}

static inline void fivmr_ThreadState_setStackHeight(fivmr_ThreadState *ts,
                                                    uintptr_t stackHeight) {
    ts->stackHeight=stackHeight;
}

/* Thread creation sequence:
   
   Case 1: Java-land creates thread (via new Thread, Thread.start)
   
   Thread.start calls VMThread.create.  VMThread.create creates a VMThread
   object, and calls fivmr_VMThread_start.  fivmr_VMThread_start then calls
   fivmr_ThreadState_new, and then fivmr_ThreadState_glue, spawns a native
   thread, and within the native thread body, it calls fivmr_ThreadState_set.
   
   Case 2: native code attaches a Java thread to an existing native thread
   
   Call fivmr_ThreadState_new.  Then call fivmr_ThreadState_set.  Now
   you have a Java "context" but no Java thread object, so you can't yet
   start calling arbitrary Java code, but you can call Java code that
   doesn't use Thread functionality.  So, call fivmr_VMThread_create.
   Then pass that handle to fivmr_ThreadState_glue.  Now you're good to
   go. */

fivmr_ThreadState *fivmr_ThreadState_new(fivmr_VM *vm,
                                         uintptr_t initExecFlags);

/* manually set the thread to the given thread state */
void fivmr_ThreadState_setManual(fivmr_ThreadState *ts,
				 fivmr_ThreadHandle th,
                                 fivmr_TypeContext *ctx);

void fivmr_ThreadState_go__INTERNAL(fivmr_ThreadState *ts);

/* manually let the thread go */
#define fivmr_ThreadState_go(ts) do {		\
	int __TMP_x;				\
	ts->stackStart=(uintptr_t)&__TMP_x;	\
	fivmr_ThreadState_go__INTERNAL(ts);	\
    } while(0)

/* never use this directly.  use the fivmr_ThreadState_set() macro instead. */
void fivmr_ThreadState_set__INTERNAL(fivmr_ThreadState *ts,
                                     fivmr_TypeContext *cts);

/* call from the thread once it's running, and attach the thread state to
   the running thread. */
#define fivmr_ThreadState_set(ts,ctx) do {              \
	int __TMP_x;                                    \
	ts->stackStart=(uintptr_t)&__TMP_x;             \
	fivmr_ThreadState_set__INTERNAL(ts,ctx);	\
    } while(0)

/* make the thread state point to the given VMThread.  also notify the
   VMThread that we're starting. */
bool fivmr_ThreadState_glue(fivmr_ThreadState *ts,
			    fivmr_Handle *javaThread);

/* commit any of the thread's outstanding data - mainly GC data.
   terminate() calls this implicitly; interrupt thread states should
   call this after finishing the interrupt. */
void fivmr_ThreadState_commit(fivmr_ThreadState *ts);

/* internal function which does the subset of ThreadState_commit() that
   is guaranteed to be done, even upon VM death. */
void fivmr_ThreadState_guaranteedCommit(fivmr_ThreadState *ts);

/* internal function which frees resources used by the thread but
   otherwise does not mark it as terminating. */
void fivmr_ThreadState_finalize(fivmr_ThreadState *ts);

/* terminate ("detach") current thread state.  note that this will boost
   the current thread's priority to MAX, and return the old base priority.
   if you're detaching from a Java thread but not killing the native thread,
   you should set the priority back to whatever this returns. */
fivmr_ThreadPriority fivmr_ThreadState_terminate(fivmr_ThreadState *ts);

bool fivmr_ThreadState_canExitGracefully(fivmr_ThreadState *ts);
bool fivmr_ThreadState_shouldExit(fivmr_ThreadState *ts);

void fivmr_ThreadState_exitImpl(fivmr_ThreadState *ts); /* internal */

void fivmr_ThreadState_checkExit(fivmr_ThreadState *ts);
void fivmr_ThreadState_checkExitHoldingLock(fivmr_ThreadState *ts,
                                            int32_t times);
void fivmr_ThreadState_checkExitHoldingLocks(fivmr_ThreadState *ts,
                                             int32_t n,
                                             fivmr_Lock **locks);
void fivmr_ThreadState_checkExitInHandshake(fivmr_ThreadState *ts); /* internal */

void fivmr_ThreadState_checkBlock(fivmr_ThreadState *ts);

void fivmr_ThreadState_performAllGuaranteedCommits(fivmr_VM *vm);

void fivmr_ThreadState_goToNative_slow(fivmr_ThreadState *ts);
void fivmr_ThreadState_goToJava_slow(fivmr_ThreadState *ts);

static inline void fivmr_ThreadState_goToNative(fivmr_ThreadState *ts) {
    if (fivmr_unlikely(
	    !fivmr_cas_weak(&(ts->execStatus),
			    FIVMR_TSES_IN_JAVA,
			    FIVMR_TSES_IN_NATIVE))) {
	fivmr_ThreadState_goToNative_slow(ts);
    }
}

static inline bool fivmr_ThreadState_tryGoToJava(fivmr_ThreadState *ts) {
    return fivmr_cas_weak(&(ts->execStatus),
			  FIVMR_TSES_IN_NATIVE,
			  FIVMR_TSES_IN_JAVA);
}

static inline void fivmr_ThreadState_goToJava(fivmr_ThreadState *ts) {
    if (fivmr_unlikely(!fivmr_ThreadState_tryGoToJava(ts))) {
	fivmr_ThreadState_goToJava_slow(ts);
    }
}

void fivmr_ThreadState_lockWithHandshake(fivmr_ThreadState *ts,
                                         fivmr_Lock *lock);

/* a conservative estimate of whether or not the thread should suspend.
   always correct to return true here, but never correct to return false
   incorrectly. */
static inline bool fivmr_ThreadState_shouldSuspend(fivmr_ThreadState *ts) {
    fivmr_assert(ts->suspendReqCount>=0);
    fivmr_assert(ts->suspendCount>=0);
    
    return ts->suspendReqCount>0 || ts->suspendCount>0;
}

/* if the thread has been asked to suspend, wait until the request is
   cleared.  call only while holding the lock.  this is an internal method! */
void fivmr_ThreadState_waitSuspended(fivmr_ThreadState *ts);

/* change the status of the thread while ensuring that we wait if the
   thread has been asked to suspend. */
void fivmr_ThreadState_moveToStatus(fivmr_ThreadState *ts,
				    uintptr_t status);

void fivmr_ThreadState_pollcheckSlow(fivmr_ThreadState *ts,
                                     uintptr_t debugID);

static inline void fivmr_ThreadState_pollcheck(fivmr_ThreadState *ts,
                                               fivmr_Frame *f,
					       uintptr_t debugID) {
#if FIVMR_INTERNAL_INST
FIVMR_II_LOCAL_DECLS
#if 0 /* deconfuse emacs */
    ;
#endif
#endif

    if (fivmr_unlikely(!ts->pollingUnion.takeFastpath)) {
#if FIVMR_INTERNAL_INST
        FIVMR_II_BEFORE_PC_SLOW(ts,f,debugID);
#endif
	fivmr_ThreadState_pollcheckSlow(ts,debugID);
#if FIVMR_INTERNAL_INST
        FIVMR_II_AFTER_PC_SLOW(ts,f,debugID);
#endif
    }
}

static inline bool fivmr_ThreadState_isOnAQueue(fivmr_ThreadState *ts) {
    return ts->forMonitor.queuedOnReal!=NULL;
}

static inline bool fivmr_ThreadState_wantsToBeOnQueue(fivmr_ThreadState *ts) {
    return ts->forMonitor.queuedOnIntended;
}

/* evaluates the given thread's priority assuming we have control over that
   thread, and we're holding that thread's lock.  if doing so results in the
   need to evaluate the priority of some other thread, that thread is
   returned. */
fivmr_ThreadState *fivmr_ThreadState_evalPrioImpl(fivmr_ThreadState *ts);

/* a "mini version" of evalPrioImpl that just finds the next thread to evaluate. */
fivmr_ThreadState *fivmr_ThreadState_evalPrioMiniImpl(fivmr_ThreadState *ts);

void fivmr_ThreadState_evalPrio(fivmr_ThreadState *ts);

void fivmr_ThreadState_setBasePrio(fivmr_ThreadState *ts,
                                   fivmr_ThreadPriority pr);

void fivmr_ThreadState_setInterrupted(fivmr_ThreadState *ts,
                                      bool value);

static inline bool fivmr_ThreadState_getInterrupted(fivmr_ThreadState *ts) {
    return ts->interrupted;
}

/* call only while IN_NATIVE */
void fivmr_ThreadState_sleepAbsolute(fivmr_ThreadState *ts,
				     fivmr_Nanos whenAwake);

/* do this only while IN_JAVA */
static inline fivmr_Handle *fivmr_ThreadState_addHandle(fivmr_ThreadState *ts,
							fivmr_Object obj) {
    fivmr_assert((ts->execFlags&FIVMR_TSEF_FINALIZED)==0);
    return fivmr_NativeFrame_addHandle(ts->curNF,ts,obj);
}

/* do this only while IN_JAVA */
fivmr_Handle *fivmr_ThreadState_cloneHandle(fivmr_ThreadState *ts,
					    fivmr_Handle *h);

/* do this only while IN_JAVA */
static inline void fivmr_ThreadState_removeHandle(fivmr_ThreadState *ts,
						  fivmr_Handle *h) {
    fivmr_Handle_remove(&ts->freeHandles,h);
}

/* do these only while IN_JAVA */
void fivmr_ThreadState_pushNF(fivmr_ThreadState *ts,
			      fivmr_NativeFrame *nf);
void fivmr_ThreadState_pushAndInitNF(fivmr_ThreadState *ts,
				     fivmr_NativeFrame *nf,
				     fivmr_MethodRec *mr);
void fivmr_ThreadState_pushAndInitNF2(fivmr_ThreadState *ts,
                                      fivmr_NativeFrame *nf,
                                      fivmr_MethodRec *mr,
                                      fivmr_TypeContext *ctx);
bool fivmr_ThreadState_popNF(fivmr_ThreadState *ts);

void fivmr_ThreadState_handlifyException(fivmr_ThreadState *ts);

/* call only while IN_JAVA */
static inline fivmr_Object
fivmr_ThreadState_javaThreadObject(fivmr_ThreadState *ts) {
    return ts->vm->javaThreads[ts->id].obj;
}

const char *fivmr_ThreadState_describeStatusImpl(uintptr_t execStatus);
const char *fivmr_ThreadState_describeFlagsImpl(uintptr_t execFlags);
const char *fivmr_ThreadState_describeStateImpl(uintptr_t execStatus,
						uintptr_t execFlags);

static inline const char *fivmr_ThreadState_describeStatus(fivmr_ThreadState *ts) {
    return fivmr_ThreadState_describeStatusImpl(ts->execStatus);
}

static inline const char *fivmr_ThreadState_describeFlags(fivmr_ThreadState *ts) {
    return fivmr_ThreadState_describeFlagsImpl(ts->execFlags);
}

static inline const char *fivmr_ThreadState_describeState(fivmr_ThreadState *ts) {
    return fivmr_ThreadState_describeStateImpl(ts->execStatus,
					       ts->execFlags);
}

char *fivmr_ThreadState_tryGetBuffer(fivmr_ThreadState *ts,
                                     int32_t size);
bool fivmr_ThreadState_tryReturnBuffer(fivmr_ThreadState *ts,
                                       char *ptr);
bool fivmr_ThreadState_tryClaimBuffer(fivmr_ThreadState *ts,
                                      char *ptr);

void fivmr_ThreadState_returnBuffer(fivmr_ThreadState *ts,
                                    char *ptr); /* this is stupidly duplicated in
						   Java-land. oh well. */

/* Set the current frame's debug ID, provided that the debug ID is not -1.
   The convention is that if you're calling something from a context in which
   debug ID's are relevant, you pass one that is not -1; otherwise you just
   pass -1. */
void fivmr_ThreadState_setDebugID(fivmr_ThreadState *ts,
				  uintptr_t debugID);

/* this is an inherently unsafe method.  use with caution. */
void fivmr_ThreadState_dumpStackFor(fivmr_ThreadState *ts);

static inline int32_t fivmr_ThreadState_id(fivmr_ThreadState *ts) {
    return (int32_t)ts->id;
}

void fivmr_ThreadState_checkHeightSlow(fivmr_ThreadState *ts,
				       uintptr_t newHeight);

static inline void fivmr_ThreadState_checkHeight(fivmr_ThreadState *ts) {
    bool changed;
    uintptr_t newHeight=(uintptr_t)&ts;
    if (FIVMR_STACK_GROWS_DOWN) {
        changed=newHeight<ts->stackHigh;
    } else {
        changed=newHeight>ts->stackHigh;
    }
    if (changed) {
	fivmr_ThreadState_checkHeightSlow(ts,newHeight);
    }
}

static inline bool fivmr_ThreadStatus_isRunning(uintptr_t curStatus) {
    switch (curStatus) {
    case FIVMR_TSES_CLEAR:
    case FIVMR_TSES_NEW:
    case FIVMR_TSES_TERMINATING:
	return false;
    default:
	return true;
    }
}

static inline bool fivmr_ThreadStatus_isInJava(uintptr_t curStatus) {
    switch (curStatus) {
    case FIVMR_TSES_IN_JAVA:
    case FIVMR_TSES_IN_JAVA_TO_BLOCK:
	return true;
    default:
	return false;
    }
}

static inline bool fivmr_ThreadStatus_isInNative(uintptr_t curStatus) {
    switch (curStatus) {
    case FIVMR_TSES_IN_NATIVE:
    case FIVMR_TSES_IN_NATIVE_TO_BLOCK:
	return true;
    default:
	return false;
    }
}

/* is the thread running? */
static inline bool fivmr_ThreadState_isRunning(fivmr_ThreadState *ts) {
    return fivmr_ThreadStatus_isRunning(ts->execStatus);
}

/* is the thread in Java? */
static inline bool fivmr_ThreadState_isInJava(fivmr_ThreadState *ts) {
    return fivmr_ThreadStatus_isInJava(ts->execStatus);
}

/* is the thread in native? */
static inline bool fivmr_ThreadState_isInNative(fivmr_ThreadState *ts) {
    return fivmr_ThreadStatus_isInNative(ts->execStatus);
}

/* trigger the thread to go into checkBlock ASAP, and return the current
   execStatus. */
uintptr_t fivmr_ThreadState_triggerBlock(fivmr_ThreadState *ts);

/* request that the thread becomes suspended; returns the exec status
   at the time of the request.  note that if the thread is not running,
   this will mark the thread slot as suspended; whenever any thread
   claims that thread slot, they will start suspended. */
uintptr_t fivmr_ThreadState_reqSuspend(fivmr_ThreadState *ts);

/* wait until a thread is suspended.  note that this function is really
   tricky.  the thread you asked to suspend may have died, and this
   ts pointer may be pointing to a completely different thread.  one
   way of dealing with this is to guard a call to waitSuspend
   with a check that the thread you were interested in, and the thread
   you're about to call this function on, are the same.  you can use
   the version field for this.  you could also pin the Java thread and
   use that instead. */
uintptr_t fivmr_ThreadState_waitForSuspend(fivmr_ThreadState *ts);

/* calls reqSuspend and then waitSuspend under one lock.  NOTE this is
   NOT intended to be used directly from java.lang.Thread.suspend().
   it's not even clear that Thread.suspend() could be implement on this
   at all. */
uintptr_t fivmr_ThreadState_suspend(fivmr_ThreadState *ts);

/* resume a suspended thread. */
void fivmr_ThreadState_resume(fivmr_ThreadState *ts);

fivmr_TypeData *fivmr_StaticTypeContext_find(fivmr_StaticTypeContext *ctx,
                                             const char *name);

fivmr_TypeStub *fivmr_StaticTypeContext_findStub(fivmr_StaticTypeContext *ctx,
                                                 const char *name);

void fivmr_TypeContext_boot(fivmr_VM *vm,
                            fivmr_TypeContext *ctx);

fivmr_TypeContext *fivmr_TypeContext_create(fivmr_VM *vm,
                                            fivmr_Handle *classLoader);

void fivmr_TypeContext_destroy(fivmr_TypeContext *ctx);

static inline void fivmr_ThreadState_disablePollchecks(fivmr_ThreadState *ts) {
    ts->pollingUnion.s.pollchecksDisabled++;
}

static inline void fivmr_ThreadState_enablePollchecks(fivmr_ThreadState *ts) {
    ts->pollingUnion.s.pollchecksDisabled--;
}

void fivmr_MemoryAreas_init(fivmr_GC *gc);

static inline void fivmr_MemoryArea_doRun(fivmr_ThreadState *ts,
                                          uintptr_t area,
                                          fivmr_Object logic) {
    ts->vm->payload->MemoryArea_doRun((uintptr_t)ts,area,logic);
}

int64_t fivmr_MemoryArea_consumed(fivmr_ThreadState *ts,
                                  fivmr_MemoryArea *area);

static inline fivmr_Object fivmr_MemoryArea_getBSID(fivmr_ThreadState *ts,
                                                    fivmr_MemoryArea *area) {
    return ts->vm->payload->MemoryArea_getBSID((uintptr_t)ts,(uintptr_t)area);
}

static inline fivmr_Object fivmr_BackingStoreID_create(fivmr_ThreadState *ts,
                                                       fivmr_Object name) {
    return ts->vm->payload->BackingStoreID_create((uintptr_t)ts,name);
}

uintptr_t fivmr_MemoryArea_alloc(fivmr_ThreadState *ts, int64_t size,
                                 int32_t shared, fivmr_Object name, int64_t unManagedSize);

void fivmr_MemoryArea_free(fivmr_ThreadState *ts, fivmr_MemoryArea *area);

void fivmr_MemoryArea_push(fivmr_ThreadState *ts,
                           fivmr_MemoryArea *area);

void fivmr_MemoryArea_pop(fivmr_ThreadState *ts, fivmr_VM *vm,
                          fivmr_MemoryArea *area);

void fivmr_MemoryArea_enter(fivmr_ThreadState *ts, fivmr_MemoryArea *area,
                            fivmr_Object logic);

uintptr_t fivmr_MemoryArea_setCurrentArea(fivmr_ThreadState *ts,
                                          fivmr_MemoryArea *area);

static inline fivmr_MemoryArea *fivmr_MemoryArea_forObject(
    fivmr_ThreadState *ts, fivmr_Object o)
{
    fivmr_GCHeader *hdr=fivmr_GCHeader_fromObject(&ts->vm->settings, o);
    uintptr_t marked=hdr->word&FIVMR_GC_MARKBITS_MASK;
    if (hdr->word==(uintptr_t)-1||(marked&&marked^FIVMR_GC_MARKBITS_MASK)) {
        /* -1 is heap for NOGC */
        return &ts->vm->gc.heapMemoryArea;
    } else {
        fivmr_ScopeID *scopeid;
        if (marked==FIVMR_GC_MARKBITS_MASK) {
            /* Freeze-dried objects don't have a scope pointer */
            return &ts->vm->gc.immortalMemoryArea;
        }
        scopeid=fivmr_GCHeader_frame(hdr);
        if ((scopeid->word&FIVMR_SCOPEID_MASK)==FIVMR_SCOPEID_STACK) {
            return &ts->vm->gc.stackMemoryArea;
        } else {
            if (FIVMR_RTSJ_SCOPES(&ts->vm->settings)) {
                return (fivmr_MemoryArea*)(((uintptr_t)scopeid)-fivmr_offsetof(fivmr_MemoryArea,scopeID_s));
                return NULL;
            } else {
                return (fivmr_MemoryArea*)(scopeid->word&~FIVMR_SCOPEID_MASK);
            }
        }
    }
}

// enum fivmr_um_primitive_t {
//   INT = 0,
//   LONG = 1,
//   SHORT = 2,
//   CHAR = 3,
//   DOUBLE = 4,
//   FLOAT = 5,
//   BOOLEAN = 6,
//   BYTE = 7
// };

// typedef enum fivmr_um_primitive_t fivmr_um_primitive_t;

/* Support for Unmanaged Data in Memory Areas */
#define FULL_MAP 63 //0b000111111 (all 6 slots full)

/* Print a number in binary */
// #define PRINT_BINARY(number) { \
//   char buffer [33];\
//   itoa (number,buffer,2);\
//   printf ("binary: %s\n",buffer);\
// }

// void print_binary(int32_t number) {
//   char buffer [33];
//   itoa (number,buffer,2);
//   printf ("binary: %s\n",buffer);
// }

uintptr_t fivmr_MemoryArea_allocatePrimitive(uintptr_t fivmrMemoryArea);
uintptr_t fivmr_MemoryArea_allocateArray(uintptr_t fivmrMemoryArea, int32_t type, int32_t size);

void fivmr_MemoryArea_freeArray(uintptr_t fivmrMemoryArea, uintptr_t arrayHeader);

int32_t fivmr_MemoryArea_loadArrayInt(uintptr_t arrayHeader, int32_t index);
void fivmr_MemoryArea_storeArrayInt(uintptr_t arrayHeader, int32_t index, int32_t value);

int64_t fivmr_MemoryArea_consumedUnmanaged(fivmr_ThreadState *ts, fivmr_MemoryArea *area);

void fivmr_ScopeBacking_alloc(fivmr_ThreadState *ts, uintptr_t size);

void fivmr_ScopeBacking_free(fivmr_ThreadState *ts);

static inline uintptr_t fivmr_MemoryArea_getImmortalArea(
    fivmr_ThreadState *ts) {
    return (uintptr_t)&ts->vm->gc.immortalMemoryArea;
}

static inline uintptr_t fivmr_MemoryArea_getHeapArea(fivmr_ThreadState *ts) {
    return (uintptr_t)&ts->vm->gc.heapMemoryArea;
}

static inline uintptr_t fivmr_MemoryArea_getStackArea(fivmr_ThreadState *ts) {
    return (uintptr_t)&ts->vm->gc.stackMemoryArea;
}

/* a soft handshake that can either be synchronous (wait for completion) or
   asynchronous (do not wait) */
void fivmr_ThreadState_softHandshakeImpl(fivmr_VM *vm,
                                         uintptr_t requiredExecFlags,
                                         uintptr_t execFlagsToSet,
                                         bool shouldWait,
                                         bool ignoreExit);

/* Implements a soft handshake, sometimes known as a ragged safepoint.  The
   algorithm is as follows.  For an atomic snapshot of live threads (i.e. threads
   that are not in the CLEAR state) that have all of the requiredExecFlags
   set (default is to pass FIVMR_TSEF_JAVA_HANDSHAKEABLE), we perform an
   asynchronous soft pair handshake.  The asynchronous soft pair handshakes
   are performed after the atomic snapshot is gathered; i.e. new threads may
   be started or old threads may be stopped during the execution of the
   soft pair handshakes.  An asynchronous soft pair handshake
   first triggers the thread to block (which is an O(1) mostly non-blocking
   operation) and grabs the thread state change lock.  This prevents the
   thread from reentering Java from native or entering native from Java,
   and also prevents the thread from being glued (going from NEW to STARTING)
   being set (going from either NEW or STARTING to IN_NATIVE), commencing
   termination (going from IN_NATIVE or IN_NATIVE_TO_BLOCK to TERMINATING),
   or terminating (going from TERMINATING to CLEAR).  While holding the
   thread state change lock, if the thread was found to have been IN_NATIVE
   or STARTING (i.e. it was not running Java code), we set execFlags specified
   by execFlagsToSet and call serviceHandshakeRequest().  If on the other
   hand it was found to be IN_JAVA, we set the execFlags specified by
   execFlagsToSet as well as FIVMR_TSEF_SOFT_HANDSHAKE, which tells the thread
   to service and acknowledge the handshake request at the next pollcheck or
   the next attempt to go into native.  Threads that are NEW, TERMINATING, or
   CLEAR are ignored (and we will see CLEAR threads even though they were not
   in the atomic snapshot, since they could have terminated in the process).
   We then release the thread's state change lock and proceed to the next
   thread in the snapshot.  After we have looped over all of the threads we wait
   for all threads that were IN_JAVA to acknowledge the pair handshake request.
   This operation is done while holding the one-per-VM handshakeLock, which
   ensures that only one soft handshake can be on-going at any time.  However,
   individual pair handshakes can be done "concurrently" to a soft handshake,
   although one will stall waiting on the other when they hit the same
   thread.

   To use soft handshakes in your code, you should (a) add an exec flag (see
   the FIVMR_TSEF macros higher up in this file) corresponding to your
   request, (b) add a handler for your flag in
   fivmr_ThreadState_serviceSoftHandshakeRequest, and (c) call this function
   with requiredExecFlags=FIVMR_TSEF_JAVA_HANDSHAKEABLE and
   execFlagsToSet=FIVMR_TSEF_YOUR_FLAG.  In some cases it makes sense to
   use soft handshakes with execFlagsToSet=0; this is particularly useful in
   concurrent garbage collection (see fivmr_cmrgc.c, which uses
   execFlagsToSet=0) for ensuring that all threads have reached at least
   one safepoint or else have just started after the call to softHandshake().
   This is great if threads sometimes read some global GC state and then do
   something based on that state and where the read and the action are
   individually atomic but are not atomic together.  If these two actions do
   not have pollchecks between them, and if the GC changes the
   state and cannot proceed until all threads have finished performing the
   relevant action with the previous values of that state, then a soft
   handshake with execFlagsToSet=0 is the right thing to do.

   The upshot of this algorithm is that (1) serviceSoftHandshake will be called
   on each thread that was running prior to the call to this function and that
   did not terminate during the call to this function, (2) serviceSoftHandshake
   will be called either from the current thread (i.e. the caller to this
   function) or by the target thread, (3) serviceSoftHandshake will always be
   called exactly once and while holding the thread's state change lock, (4)
   you cannot rely on threads started during the call to softHandshake to have
   serviceSoftHandshake called on them, and (5) you cannot rely on threads
   started during the call to softHandshake to not have serviceSoftHandshake
   called on them.  Thus, anywhere where you use soft handshakes, you should
   ensure that newly spawned threads either don't matter (such as if you're
   just using this for profiling and debugging) or that the
   fivmr_ThreadState_setManual functions performs whatever actions are required
   to handle new threads.  You should add calls from setManual right under the
   comment block referring to soft handshakes.  Note that any
   actions that you have newly started threads perform (by editing setManual as
   indicated by the guidance inside that function) are guaranteed to be
   performed before the thread starts executing Java code but after the
   beginning of the execution of any soft handshake that did not include that
   thread in its snapshot.  In other words, it's possible for a new thread to
   execute both the action in setManual and the action in serviceSoftHandshake,
   in any arbitrary order but not at the same time, prior to entering Java code.
   Because it is possible for serviceSoftHandshake to be called before the
   setManual initialization code runs, you should make sure that your code in
   serviceSoftHandshake can recognize threads that are not yet initialized.  The
   best way to do this is to have the clear() function in fivmr_threadstate.c
   reset a flag indicating thread initialization hasn't been performed and
   to set that flag in setManual.  Currently the main users of soft and pair
   handshakes (locks and GC) simply either (a) rely on their thread state data
   being zeroed in clear(), or (b) in the case of the GC's marking state, they
   actually set it both in thread initialization and in every soft handshake -
   i.e. every soft handshake "initializes" the part of the thread state that is
   needed by the GC for its soft handshakes to work (see fivmr_cmrgc.c's
   implementation of handleHandshake; the very first handshake handled is the
   FIVMR_TSEF_GC_MISC, which pushes shade values; other handshakes handled by
   that function rely on FIVMR_TSEF_GC_MISC to have run - thus using any GC
   handshake requires also using GC_MISC). */
static inline void fivmr_ThreadState_softHandshake(fivmr_VM *vm,
                                                   uintptr_t requiredExecFlags,
                                                   uintptr_t execFlagsToSet) {
    fivmr_ThreadState_softHandshakeImpl(vm,
                                        requiredExecFlags,
                                        execFlagsToSet,
                                        true,
                                        false);
}

/* asynchronous soft handshake */
static inline void fivmr_ThreadState_asyncSoftHandshake(fivmr_VM *vm,
                                                        uintptr_t requiredExecFlags,
                                                        uintptr_t execFlagsToSet) {
    fivmr_ThreadState_softHandshakeImpl(vm,
                                        requiredExecFlags,
                                        execFlagsToSet,
                                        false,
                                        false);
}

/* soft pair handshake - affects only one thread. */
void fivmr_ThreadState_softPairHandshake(fivmr_ThreadState *ts,
                                         uintptr_t execFlagsToSet);

/* call ONLY when the lock is held */
void fivmr_ThreadState_serviceSoftHandshakeRequest(fivmr_ThreadState *ts);

/* call ONLY when the lock is held, and only AFTER calling
   serviceSoftHandshakeRequest */
void fivmr_ThreadState_acknowledgeSoftHandshakeRequest(fivmr_ThreadState *ts);

void fivmr_VMThread_start(fivmr_ThreadState *curTS,
                          fivmr_Handle *vmt);

void fivmr_VMThread_startPooledThread(fivmr_ThreadState *curTS,
                                      fivmr_ThreadPool *pool,
                                      fivmr_Handle *runnable,
                                      fivmr_ThreadPriority priority);

fivmr_TypeData *fivmr_TypeStub_getTypeData(fivmr_TypeStub *start);

static inline fivmr_TypeData *fivmr_FieldRec_owner(fivmr_FieldRec *fr) {
    return fr->owner;
}

static inline const char *fivmr_FieldRec_name(fivmr_FieldRec *fr) {
    return fr->name;
}

static inline fivmr_BindingFlags fivmr_FieldRec_flags(fivmr_FieldRec *fr) {
    return fr->flags;
}

static inline fivmr_TypeStub *fivmr_FieldRec_type(fivmr_FieldRec *fr) {
    return fr->type;
}

static inline fivmr_TypeData *fivmr_FieldRec_resolvedType(fivmr_FieldRec *fr) {
    return fivmr_TypeStub_getTypeData(fr->type);
}

/*
 * At runtime, we have four ways of identifying a field:
 *
 * fivmr_FieldRec structure (represented as fivmr_FieldRec*)
 * field location (represented as uintptr_t)
 * field offset (represented as uintptr_t, N/A for instance fields)
 * field address (represented as void*)
 *
 * The fivmr_FieldRec* structure contains all of the information about a
 * field, except for the receiver in the case of instance fields.  For
 * static fields, fivmr_FieldRec* tells you everything.  For instance fields,
 * you only need fivmr_FieldRec* and the receiver (fivmr_Object) to find the
 * field.
 *
 * The field location is the information the compiler gives us for computing
 * the field's offset and/or address.  For static fields, this is either an
 * index into one of the field tables, or a pointer to the field.  For
 * instance fields, this is an object-model-specific way of identifying the
 * field.  Note that for instance fields, the location should not be used in
 * pointer arithmetic, because there may be some encoding issues (for example,
 * in the CONTIGUOUS object model, the "location" is the offset from the
 * beginning of the object payload, rather than from where the object
 * points).
 *
 * The field offset is intended to be the offset from the object pointer for
 * instance fields.  It does not apply to static fields.  Note that not all
 * object models support the notion of an offset that can be used for
 * pointer arithmetic.  But, for those object models that have contiguous
 * objects, or fragmented models in which some objects may opportunistically
 * be made contiguous, adding a "field offset" to an object pointer (for a
 * contiguous object!) is guaranteed to result in the address of the field.
 * For object models that lack contiguous objects, or for objects that
 * happened to be conservatively allocated in a fragmented manner, this is nothing
 * more than an alternate way of representing the field location.  But for
 * those non-contiguous object models, even though the offset is just another
 * way of representing location, it is not guaranteed that offset and location
 * will be equal (though they currently are equal in the FRAGMENTED model).
 * Because the distinction between offset and location exists and because this
 * distinction depends on the object model, you should treat offsets and
 * locations separately and perform conversions as necessary.  There are
 * functions provided below for doing such conversions.
 *
 * The field address is the actual pointer to the actual field.  For static
 * fields, this can be obtained by computing from the field location.  For
 * instance fields, this can be computed using the receiver object pointer and
 * the field offset.  Since the field offset can be computed from the field
 * location, and the field location can be computed from the fivmr_FieldRec*,
 * the field address may be computed directly from a fivmr_Object pointer and
 * a fivmr_FieldRec* pointer.
 *
 * Functions are provided below for performing the various conversions.
 */

/* Acquire the field location from a fivmr_FieldRec* */
static inline uintptr_t fivmr_FieldRec_location(fivmr_FieldRec *fr) {
    return fr->location;
}

/* Compute the field offset from a field location. */
static inline uintptr_t fivmr_locationToOffsetFromObj(fivmr_Settings *settings,
                                                      uintptr_t location) {
    if (FIVMR_OM_CONTIGUOUS(settings)) {
        return location-4;
    } else if (FIVMR_OM_FRAGMENTED(settings)) {
        return location;
    } else {
        fivmr_assert(!"bad object model");
        return 0;
    }
}

/* Compute the field offset from a fivmr_FieldRec*.  This is just a convenience
   function; the above two functions can be used for the same purpose. */
static inline uintptr_t fivmr_FieldRec_offsetFromObj(fivmr_Settings *settings,
                                                     fivmr_FieldRec *fr) {
    return fivmr_locationToOffsetFromObj(settings,fr->location);
}

/* Compute the field address of a static field, given a fivmr_FieldRec*.  This
   can only be used for static fields. */
static inline void *fivmr_FieldRec_staticFieldAddress(fivmr_VM *vm,
                                                      fivmr_FieldRec *fr) {
    if ((fr->owner->flags&FIVMR_TBF_AOT)) {
        if ((fr->type->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_PRIMITIVE ||
            (fr->flags&FIVMR_FBF_UNTRACED)!=0) {
            return (void*)((uintptr_t)vm->primFields+fr->location);
        } else {
            /* NOTE: fr->location is a byte offset within the refFields array */
            return (void*)((uintptr_t)vm->refFields+fr->location);
        }
    } else {
        return (void*)fr->location;
    }
}

/* compute the argument to be used for barriers, for a field */
static inline uintptr_t fivmr_FieldRec_barrierArg(fivmr_VM *vm,
                                                  fivmr_FieldRec *fr) {
    if ((fr->flags&FIVMR_BF_STATIC)!=0) {
        return (uintptr_t)fivmr_FieldRec_staticFieldAddress(vm,fr);
    } else {
        return fivmr_FieldRec_location(fr);
    }
}

/* Compute the field address given an object pointer and a field offset.  This
   can only be used for instance fields.  */
static inline void *fivmr_addressOfField(fivmr_Settings *settings,
                                         fivmr_Object obj,
                                         uintptr_t offsetFromObj) {
    if (FIVMR_OM_CONTIGUOUS(settings)) {
        return (void*)(obj+offsetFromObj);
    } else if (FIVMR_OM_FRAGMENTED(settings)) {
        uintptr_t ptr;
        uintptr_t loc;
        ptr=obj-FIVMR_FRAG_OBJ_FH_OFFSET;
        loc=offsetFromObj;
        while (loc>=FIVMR_GC_BLOCK_SIZE) {
            loc-=FIVMR_GC_BLOCK_SIZE;
            ptr=(*(uintptr_t*)ptr)&~((uintptr_t)(FIVMSYS_PTRSIZE-1));
        }
        return (void*)(ptr+loc);
    } else {
        fivmr_assert(!"bad object model");
        return NULL;
    }
}

/* Compute the address of an array element, given an object pointer, an
   array index, and the size of the elements in the array. */
static inline void *fivmr_addressOfElement(fivmr_Settings *settings,
                                           fivmr_Object obj,
                                           int32_t index,
                                           uintptr_t eleSize) {
    if (FIVMR_OM_CONTIGUOUS(settings)) {
        return (void*)(obj+FIVMR_CONT_ARR_PAYLOAD_OFF(eleSize)+index*eleSize);
    } else if (FIVMR_OM_FRAGMENTED(settings)) {
        uintptr_t offset=index*eleSize;
        if (index < *(int32_t*)(obj+FIVMR_FRAG_ARR_LEN_OFF)) {
            if (false) printf("returning address of element %d in array %p the fast way\n",
                              index,(void*)obj);
            return (void*)(obj+FIVMR_FRAG_ARR_PAYLOAD_OFF(eleSize)+offset);
        } else {
            uintptr_t *spine=*(uintptr_t**)(obj-FIVMR_FRAG_OBJ_FH_OFFSET);
            uintptr_t chunk=spine[offset/FIVMR_GC_BLOCK_SIZE];
            if (false) printf("returning address of element %d in array %p the slow way,"
                              "via spine %p and chunk %p\n",
                              index,(void*)obj,spine,(void*)chunk);
            return (void*)(chunk-(offset%FIVMR_GC_BLOCK_SIZE));
        }
    } else {
        fivmr_assert(!"bad object model");
        return NULL;
    }
}

/* Compute the field address given a fivmr_FieldRec* and an object pointer.
   This works for both instance and static fields; for static fields the
   object pointer argument is ignored. */
static inline void *fivmr_FieldRec_fieldAddress(fivmr_VM *vm,
                                                fivmr_FieldRec *fr,
                                                fivmr_Object obj) {
    if ((fr->flags&FIVMR_BF_STATIC)!=0) {
        return fivmr_FieldRec_staticFieldAddress(vm,fr);
    } else {
        return fivmr_addressOfField(
            &vm->settings,
            obj,
            fivmr_FieldRec_offsetFromObj(&vm->settings,fr));
    }
}

/* What follows are the C-side barriers.  These only get used for:
  
   - Internal VM C code that wants access to the heap.
   - JNI code.  (JNI heap access calls bottom out at these barriers.)
   - Java reflection.
   
   I.e. Java code will never be compiled to use these for bytecode-level or
   source-level field or array accesses; the only calls to these from Java will
   be explicit (i.e. if someone does @Import fivmr_byteArrayLoad for example).
   
   Since these barriers are never used on fast paths, they do not have to
   be efficient - they just need to work.
   
   All C code that accesses the Java heap *must* use these barriers.  No
   exceptions!

   Java code has wrappers for these barriers that perform additional safety
   checks; see fivmRuntime.java. */

static inline int32_t fivmr_arrayLengthImpl(fivmr_Settings *settings,
                                            fivmr_Object array,
                                            int32_t mask) {
    if (FIVMR_OM_CONTIGUOUS(settings)) {
        return *(int32_t*)(array-4);
    } else if (FIVMR_OM_FRAGMENTED(settings)) {
        return (*(int32_t**)(array-FIVMR_FRAG_OBJ_FH_OFFSET))[-1];
    } else {
        fivmr_assert(!"bad object model");
        return 0;
    }
}

static inline int32_t fivmr_arrayLength(fivmr_ThreadState *ts,
                                        fivmr_Object array,
                                        int32_t mask) {
    if (FIVMR_OM_CONTIGUOUS(&ts->vm->settings)) {
        return *(int32_t*)(array-4);
    } else if (FIVMR_OM_FRAGMENTED(&ts->vm->settings)) {
        return (*(int32_t**)(array-FIVMR_FRAG_OBJ_FH_OFFSET))[-1];
    } else {
        fivmr_assert(!"bad object model");
        return 0;
    }
}

static inline bool fivmr_Object_checkArrayBounds(fivmr_ThreadState *ts,
                                                 fivmr_Object obj,
                                                 int32_t index) {
    return ((uint32_t)index)<((uint32_t)fivmr_arrayLength(ts,obj,0));
}

static inline bool fivmr_Object_scopeStoreCheck(fivmr_ThreadState *ts,
                                                fivmr_Object trg,
                                                fivmr_Object src) {
    if (src!=0) {
        uintptr_t tage=
            fivmr_GCHeader_canonicalizeScope(
                fivmr_GCHeader_fromObject(&ts->vm->settings,trg));
        uintptr_t sage=
            fivmr_GCHeader_canonicalizeScope(
                fivmr_GCHeader_fromObject(&ts->vm->settings,src));
        return sage>=tage;
    } else {
        return true;
    }
}

static inline bool fivmr_Object_inHeapCheck(fivmr_ThreadState *ts,
                                            fivmr_Object src) {
    return src==0
        || (fivmr_GCHeader_canonicalizeScope(
                fivmr_GCHeader_fromObject(&ts->vm->settings,src))
            == (uintptr_t)(intptr_t)-1);
}

#define fivmr_byteArrayLoad(ts,array,index,mask)	\
    (*(int8_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),1))
#define fivmr_charArrayLoad(ts,array,index,mask)	\
    (*(uint16_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),2))
#define fivmr_shortArrayLoad(ts,array,index,mask)	\
    (*(int16_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),2))
#define fivmr_intArrayLoad(ts,array,index,mask)	\
    (*(int32_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),4))
#define fivmr_longArrayLoad(ts,array,index,mask)	\
    (*(int64_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),8))
#define fivmr_pointerArrayLoad(ts,array,index,mask)	\
    (*(uintptr_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),sizeof(uintptr_t)))
#define fivmr_floatArrayLoad(ts,array,index,mask)	\
    (*(float*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),4))
#define fivmr_doubleArrayLoad(ts,array,index,mask)	\
    (*(double*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),8))
#define fivmr_objectArrayLoad(ts,array,index,mask)	\
    (*(uintptr_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),sizeof(uintptr_t)))

#define fivmr_byteArrayStore(ts,array,index,value,mask)	\
    (*((int8_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),1))=(int8_t)(value))
#define fivmr_charArrayStore(ts,array,index,value,mask)	\
    (*((uint16_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),2))=(uint16_t)(value))
#define fivmr_shortArrayStore(ts,array,index,value,mask)	\
    (*((int16_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),2))=(int16_t)(value))
#define fivmr_intArrayStore(ts,array,index,value,mask)	\
    (*((int32_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),4))=(int32_t)(value))
#define fivmr_longArrayStore(ts,array,index,value,mask)	\
    (*((int64_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),8))=(int64_t)(value))
#define fivmr_pointerArrayStore(ts,array,index,value,mask)	\
    (*((uintptr_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),sizeof(uintptr_t)))=(uintptr_t)(value))
#define fivmr_floatArrayStore(ts,array,index,value,mask)	\
    (*((float*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),4))=(float)(value))
#define fivmr_doubleArrayStore(ts,array,index,value,mask)	\
    (*((double*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),8))=(double)(value))
static inline void fivmr_objectArrayStore(fivmr_ThreadState *ts,
					  fivmr_Object array,
					  int32_t index,
					  fivmr_Object value,
					  int32_t mask) {
    fivmr_GC_store(
        ts,
        (fivmr_Object*)fivmr_addressOfElement(&(ts)->vm->settings,array,index,sizeof(uintptr_t)),
        value,
        mask);
}

#define fivmr_intArrayWeakCAS(ts,array,index,comparand,value,mask)	\
    (fivmr_cas32_weak(((int32_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),4)),(comparand),(value)))
#define fivmr_pointerArrayWeakCAS(ts,array,index,comparand,value,mask)	\
    (fivmr_cas_weak(((uintptr_t*)fivmr_addressOfElement(&((fivmr_ThreadState*)(ts))->vm->settings,(array),(index),sizeof(uintptr_t))),(comparand),(value)))
static inline bool fivmr_objectArrayWeakCAS(fivmr_ThreadState *ts,
					    fivmr_Object array,
					    int32_t index,
					    fivmr_Object comparand,
					    fivmr_Object value,
					    int32_t mask) {
    return fivmr_GC_weakCAS(
        ts,
        (fivmr_Object*)fivmr_addressOfElement(&(ts)->vm->settings,array,index,sizeof(uintptr_t)),
        comparand,
        value,
        mask);
}

#define fivmr_byteGetField(ts,object,offset,mask)		\
    (*(int8_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))
#define fivmr_charGetField(ts,object,offset,mask)		\
    (*(uint16_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))
#define fivmr_shortGetField(ts,object,offset,mask)		\
    (*(int16_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))
#define fivmr_intGetField(ts,object,offset,mask)		\
    (*(int32_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))
#define fivmr_longGetField(ts,object,offset,mask)		\
    (*(int64_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))
#define fivmr_pointerGetField(ts,object,offset,mask)		\
    (*(uintptr_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))
#define fivmr_floatGetField(ts,object,offset,mask)		\
    (*(float*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))
#define fivmr_doubleGetField(ts,object,offset,mask)		\
    (*(double*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))
#define fivmr_objectGetField(ts,object,offset,mask)		\
    (*(uintptr_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)))

#define fivmr_bytePutField(ts,object,offset,value,mask)			\
    (*(int8_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset))=(int8_t)(value))
#define fivmr_charPutField(ts,object,offset,value,mask)			\
    (*(uint16_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset))=(uint16_t)(value))
#define fivmr_shortPutField(ts,object,offset,value,mask)		\
    (*(int16_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset))=(int16_t)(value))
#define fivmr_intPutField(ts,object,offset,value,mask)			\
    (*(int32_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset))=(int32_t)(value))
#define fivmr_longPutField(ts,object,offset,value,mask)			\
    (*(int64_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset))=(int64_t)(value))
#define fivmr_pointerPutField(ts,object,offset,value,mask)		\
    (*(uintptr_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset))=(uintptr_t)(value))
#define fivmr_floatPutField(ts,object,offset,value,mask)		\
    (*(float*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset))=(float)(value))
#define fivmr_doublePutField(ts,object,offset,value,mask)		\
    (*(double*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset))=(double)(value))
static inline void fivmr_objectPutField(fivmr_ThreadState *ts,
					fivmr_Object object,
					uintptr_t offset,
					fivmr_Object value,
					int32_t mask) {
    fivmr_GC_store(ts,(fivmr_Object*)fivmr_addressOfField(&(ts)->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)),value,mask);
}

#define fivmr_intWeakCASField(ts,object,offset,comparand,value,mask)	\
    (fivmr_cas32_weak((int32_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)), \
		      (comparand),(value)))
#define fivmr_pointerWeakCASField(ts,object,offset,comparand,value,mask) \
    (fivmr_cas_weak((uintptr_t*)fivmr_addressOfField(&((fivmr_ThreadState*)(ts))->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)), \
		    (comparand),(value)))
static inline bool fivmr_objectWeakCASField(fivmr_ThreadState *ts,
					    fivmr_Object object,
					    uintptr_t offset,
					    fivmr_Object comparand,
					    fivmr_Object value,
					    int32_t mask) {
    return fivmr_GC_weakCAS(ts,(fivmr_Object*)fivmr_addressOfField(&(ts)->vm->settings,(fivmr_Object)(object),(uintptr_t)(offset)),comparand,value,mask);
}

#define fivmr_objectGetStatic(ts,staticFieldAddr,mask)	\
    (*(uintptr_t*)(staticFieldAddr))

static inline void fivmr_objectPutStatic(fivmr_ThreadState *ts,
					 fivmr_Object *staticFieldAddr,
					 fivmr_Object value,
					 int32_t mask) {
    fivmr_GC_store(ts,staticFieldAddr,value,mask);
}

static inline bool fivmr_objectWeakCASStatic(fivmr_ThreadState *ts,
					     fivmr_Object *staticFieldAddr,
					     fivmr_Object comparand,
					     fivmr_Object value,
					     int32_t mask) {
    return fivmr_GC_weakCAS(ts,staticFieldAddr,comparand,value,mask);
}

#define fivmr_byteGetStatic2(ts,idx,mask)               \
    (*(int8_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)))
#define fivmr_charGetStatic2(ts,idx,mask)               \
    (*(uint16_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)))
#define fivmr_shortGetStatic2(ts,idx,mask)              \
    (*(int16_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)))
#define fivmr_intGetStatic2(ts,idx,mask)                \
    (*(int32_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)))
#define fivmr_longGetStatic2(ts,idx,mask)               \
    (*(int64_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)))
#define fivmr_pointerGetStatic2(ts,idx,mask)            \
    (*(uintptr_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)))
#define fivmr_floatGetStatic2(ts,idx,mask)              \
    (*(float*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)))
#define fivmr_doubleGetStatic2(ts,idx,mask)             \
    (*(double*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)))
static inline fivmr_Object fivmr_objectGetStatic2(fivmr_ThreadState *ts,
                                                  uintptr_t location,
                                                  int32_t mask) {
    if (mask&FIVMR_FBF_UNTRACED) {
        return *(uintptr_t*)((uintptr_t)ts->primFields+location);
    } else {
        return *(uintptr_t*)((uintptr_t)ts->refFields+location);
    }
}

#define fivmr_bytePutStatic2(ts,idx,value,mask)                         \
    (*(int8_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx))=(int8_t)(value))
#define fivmr_charPutStatic2(ts,idx,value,mask)                         \
    (*(uint16_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx))=(uint16_t)(value))
#define fivmr_shortPutStatic2(ts,idx,value,mask)                        \
    (*(int16_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx))=(int16_t)(value))
#define fivmr_intPutStatic2(ts,idx,value,mask)                          \
    (*(int32_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx))=(int32_t)(value))
#define fivmr_longPutStatic2(ts,idx,value,mask)                         \
    (*(int64_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx))=(int64_t)(value))
#define fivmr_pointerPutStatic2(ts,idx,value,mask)                      \
    (*(uintptr_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx))=(uintptr_t)(value))
#define fivmr_floatPutStatic2(ts,idx,value,mask)                        \
    (*(float*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx))=(float)(value))
#define fivmr_doublePutStatic2(ts,idx,value,mask)                       \
    (*(double*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx))=(double)(value))
static inline void fivmr_objectPutStatic2(fivmr_ThreadState *ts,
                                          uintptr_t location,
                                          fivmr_Object value,
                                          int32_t mask) {
    if (mask&FIVMR_FBF_NOT_A_REFERENCE) {
        /* not a reference at all, store directly. */
        *(uintptr_t*)((uintptr_t)ts->primFields+location)=value;
    } else if (mask&FIVMR_FBF_UNTRACED) {
        /* special case! it's a reference but it's not traced by GC, so it's
           in primFields, but it still needs a barrier. */
        fivmr_GC_store(ts,(uintptr_t*)((uintptr_t)ts->primFields+location),value,mask);
    } else {
        fivmr_GC_store(ts,(uintptr_t*)((uintptr_t)ts->refFields+location),value,mask);
    }
}

#define fivmr_intWeakCASStatic2(ts,idx,comparand,value,mask)            \
    (fivmr_cas32_weak((int32_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)),      \
                      (comparand),(value)))
#define fivmr_pointerWeakCASStatic2(ts,idx,comparand,value,mask)        \
    (fivmr_cas_weak((uintptr_t*)((uintptr_t)((fivmr_ThreadState*)ts)->primFields+(idx)),      \
                    (comparand),(value)))
static inline bool fivmr_objectWeakCASStatic2(fivmr_ThreadState *ts,
                                              uintptr_t location,
                                              fivmr_Object comparand,
                                              fivmr_Object value,
                                              int32_t mask) {
    if (mask&FIVMR_FBF_NOT_A_REFERENCE) {
        return fivmr_pointerWeakCASStatic2(ts,location,comparand,value,mask);
    } else if (mask&FIVMR_FBF_UNTRACED) {
        return fivmr_GC_weakCAS(ts,(uintptr_t*)((uintptr_t)ts->primFields+location),
                                comparand,value,mask);
    } else {
        return fivmr_GC_weakCAS(ts,(uintptr_t*)((uintptr_t)ts->refFields+location),
                                comparand,value,mask);
    }
}

fivmr_TypeAux *fivmr_TypeAux_add(void *region,
                                 fivmr_TypeAux **list,
                                 uintptr_t size,
                                 fivmr_TypeAuxFlags flags);

/* don't use this if you're region allocating */
void fivmr_TypeAux_deleteAll(fivmr_TypeAux **list);

fivmr_TypeAux *fivmr_TypeAux_first(fivmr_TypeAux *list,
                                   fivmr_TypeAuxFlags mask,
                                   fivmr_TypeAuxFlags expected);

fivmr_TypeAux *fivmr_TypeAux_next(fivmr_TypeAux *list,
                                  fivmr_TypeAuxFlags mask,
                                  fivmr_TypeAuxFlags expected);

static inline uintptr_t fivmr_TypeAux_data(fivmr_TypeAux *aux) {
    uintptr_t result=((uintptr_t)aux)+fivmr_alignRaw(sizeof(fivmr_TypeAux),8);
    fivmr_assert((result&7)==0);
    return result;
}

void *fivmr_TypeAux_addElement(void *region,
                               fivmr_TypeAux **list,
                               fivmr_TypeAuxFlags mask,
                               fivmr_TypeAuxFlags expected,
                               fivmr_TypeAuxFlags flags,
                               void *data,
                               size_t size);

void *fivmr_TypeAux_addUntraced(void *region,
                                fivmr_TypeAux **list,
                                void *data,
                                size_t size);

void *fivmr_TypeAux_addUntracedZero(void *region,
                                    fivmr_TypeAux **list,
                                    size_t size);

fivmr_Object *fivmr_TypeAux_addPointer(void *region,
                                       fivmr_TypeAux **list,
                                       fivmr_Object obj);

static inline fivmr_TypeContext *fivmr_TypeContext_fromStatic(fivmr_StaticTypeContext *ctx) {
    return (fivmr_TypeContext*)(((uintptr_t)ctx)-fivmr_offsetof(fivmr_TypeContext,st));
}

/* call only IN_JAVA */
fivmr_TypeData *fivmr_TypeContext_find(fivmr_TypeContext *ctx,
                                       const char *name);

fivmr_TypeData *fivmr_TypeContext_findClass(fivmr_TypeContext *ctx,
                                            const char *name,
                                            char packageSeparator);

fivmr_TypeData *fivmr_TypeContext_findKnown(fivmr_TypeContext *ctx,
                                            const char *name);

fivmr_TypeData *fivmr_TypeContext_findClassKnown(fivmr_TypeContext *ctx,
                                                 const char *name,
                                                 char packageSeparator);

/* if we know about the type, it'll return the type; otherwise it'll
   return the type stub. */
fivmr_TypeStub *fivmr_TypeContext_findStub(fivmr_TypeContext *ctx,
                                           const char *name);

static inline bool fivmr_TypeData_bucketsMalloced(fivmr_TypeData *td,
                                                  int32_t epoch) {
    if (epoch==0) {
        return !!(td->flags&FIVMR_TBF_BUCKETS_MALLOCED_E1);
    } else {
        return !!(td->flags&FIVMR_TBF_BUCKETS_MALLOCED_E2);
    }
}

static inline void fivmr_TypeData_setBucketsMalloced(fivmr_TypeData *td,
                                                     int32_t epoch,
                                                     bool value) {
    int32_t mask;
    int32_t bitVal;
    if (epoch==0) {
        mask=FIVMR_TBF_BUCKETS_MALLOCED_E1;
    } else {
        mask=FIVMR_TBF_BUCKETS_MALLOCED_E2;
    }
    if (value) {
        bitVal=mask;
    } else {
        bitVal=0;
    }
    fivmr_BitField_setAtomic(&td->flags,mask,bitVal);
}

static inline bool fivmr_TypeData_itableMalloced(fivmr_TypeData *td,
                                                 int32_t epoch) {
    if (epoch==0) {
        return !!(td->flags&FIVMR_TBF_ITABLE_MALLOCED_E1);
    } else {
        return !!(td->flags&FIVMR_TBF_ITABLE_MALLOCED_E2);
    }
}

static inline void fivmr_TypeData_setItableMalloced(fivmr_TypeData *td,
                                                    int32_t epoch,
                                                    bool value) {
    int32_t mask;
    int32_t bitVal;
    if (epoch==0) {
        mask=FIVMR_TBF_ITABLE_MALLOCED_E1;
    } else {
        mask=FIVMR_TBF_ITABLE_MALLOCED_E2;
    }
    if (value) {
        bitVal=mask;
    } else {
        bitVal=0;
    }
    fivmr_BitField_setAtomic(&td->flags,mask,bitVal);
}

/* this doesn't require holding any locks. */
uintptr_t fivmr_TypeData_forAllAncestors(fivmr_TypeData *td,
                                         uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                            fivmr_TypeData *curTD,
                                                            uintptr_t arg),
                                         uintptr_t arg);

uintptr_t fivmr_TypeData_forAllAncestorsInclusive(fivmr_TypeData *td,
                                                  uintptr_t (*cback)(
                                                      fivmr_TypeData *startTD,
                                                      fivmr_TypeData *curTD,
                                                      uintptr_t arg),
                                                  uintptr_t arg);

/* iterate over ancestors starting with the ones that have the largest
   number of descendants. */
uintptr_t fivmr_TypeData_forAllAncestorsSorted(fivmr_TypeData *td,
                                               uintptr_t (*cback)(
                                                   fivmr_TypeData *startTD,
                                                   fivmr_TypeData *curTD,
                                                   uintptr_t arg),
                                               uintptr_t arg);

uintptr_t fivmr_TypeData_forAllAncestorsSortedInclusive(fivmr_TypeData *td,
                                                        uintptr_t (*cback)(
                                                            fivmr_TypeData *startTD,
                                                            fivmr_TypeData *curTD,
                                                            uintptr_t arg),
                                                        uintptr_t arg);

/* type iteration functions that deal with descendants require holding the
   global TypeData lock */
uintptr_t fivmr_TypeData_forAllDescendants(fivmr_TypeData *td,
                                           uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                              fivmr_TypeData *curTD,
                                                              uintptr_t arg),
                                           uintptr_t arg);

uintptr_t fivmr_TypeData_forAllDescendantsInclusive(fivmr_TypeData *td,
                                                    uintptr_t (*cback)(
                                                        fivmr_TypeData *startTD,
                                                        fivmr_TypeData *curTD,
                                                        uintptr_t arg),
                                                    uintptr_t arg);

/* iterate over all ancestors of all of our descendants */
uintptr_t fivmr_TypeData_forShadow(fivmr_TypeData *td,
                                   uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                      fivmr_TypeData *curTD,
                                                      uintptr_t arg),
                                   uintptr_t arg);

uintptr_t fivmr_TypeContext_forAllTypes(fivmr_TypeContext *ctx,
                                        uintptr_t (*cback)(fivmr_TypeData *curTD,
                                                           uintptr_t arg),
                                        uintptr_t arg);

uintptr_t fivmr_VM_forAllTypes(fivmr_VM *vm,
                               uintptr_t (*cback)(fivmr_TypeData *curTD,
                                                  uintptr_t arg),
                               uintptr_t arg);

void *fivmr_TypeContext_addUntracedField(fivmr_TypeContext *ctx,
                                         int32_t size);

void *fivmr_TypeContext_addTracedField(fivmr_TypeContext *ctx);

fivmr_TypeData *fivmr_TypeData_define(fivmr_TypeContext *ctx,
                                      fivmr_TypeData *td);

bool fivmr_TypeData_resolve(fivmr_TypeData *td);

/* finds all uses of the entrypoint in the itables and vtables.  any
   that are found are replaced.  if none are found, false is returned. */
bool fivmr_TypeData_fixEntrypoint(fivmr_TypeData *td,
                                  void *oldEntrypoint,
                                  void *newEntrypoint);

int32_t fivmr_Baseline_offsetToJStack(fivmr_MethodRec *mr);

int32_t fivmr_Baseline_offsetToSyncReceiver(fivmr_MethodRec *mr);

void fivmr_resolveField(fivmr_ThreadState *ts,
                        uintptr_t returnAddr,
                        fivmr_BaseFieldAccess *bfa);

void fivmr_resolveMethod(fivmr_ThreadState *ts,
                         uintptr_t returnAddr,
                         fivmr_BaseMethodCall *bmc);

void fivmr_resolveArrayAlloc(fivmr_ThreadState *ts,
                             uintptr_t returnAddr,
                             fivmr_BaseArrayAlloc *baa);

void fivmr_resolveObjectAlloc(fivmr_ThreadState *ts,
                              uintptr_t returnAddr,
                              fivmr_BaseObjectAlloc *boa);

void fivmr_resolveInstanceof(fivmr_ThreadState *ts,
                             uintptr_t returnAddr,
                             fivmr_BaseInstanceof *bio);

void fivmr_baselineThrow(fivmr_ThreadState *ts,
                         uintptr_t framePtr,
                         uintptr_t *result);

void fivmr_handlePatchPoint(fivmr_ThreadState *ts,
                            const char *className,
                            const char *fromWhereDescr,
                            int bcOffset,
                            void **patchThunkPtrPtr,
                            void *origPatchThunk);

#if FIVMR_CAN_DO_CLASSLOADING
void fivmr_nullCheckSlowBaseline(void);
void fivmr_abcSlowBaseline(void);
void fivmr_stackHeightSlowBaseline(void);
void fivmr_pollcheckSlowBaseline(void);
void fivmr_baselineThrowThunk(void);
void fivmr_baselineProEpThrowThunk(void);
void fivmr_resolveFieldAccessThunk(void);
void fivmr_resolveMethodCallThunk(void);
void fivmr_resolveInvokeInterfaceThunk(void);
void fivmr_resolveArrayAllocThunk(void);
void fivmr_resolveObjectAllocThunk(void);
void fivmr_resolveInstanceofThunk(void);
#endif

void fivmr_TypeData_free(fivmr_TypeData *td);

/* this'll fail if both sets have TypeDatas */
bool fivmr_TypeStub_union(fivmr_TypeStub *a,
                          fivmr_TypeStub *b);

bool fivmr_TypeStub_unionParams(int32_t nparams1,fivmr_TypeStub **params1,
                                int32_t nparams2,fivmr_TypeStub **params2);

fivmr_TypeStub *fivmr_TypeStub_find(fivmr_TypeStub *start);

/* determines if the two type stubs are related via equality constraints */
bool fivmr_TypeStub_eq(fivmr_TypeStub *a,
                       fivmr_TypeStub *b);

/* determines if the two type stubs have the same name */
bool fivmr_TypeStub_eq2(fivmr_TypeStub *a,
                        fivmr_TypeStub *b);

/* convenience function for comparing lists using fivmr_TypeStub_eq */
bool fivmr_TypeStub_paramsEq(int32_t nparams1,fivmr_TypeStub **params1,
                             int32_t nparams2,fivmr_TypeStub **params2);

/* convenience function for comparing lists using fivmr_TypeStub_eq2 */
bool fivmr_TypeStub_paramsEq2(int32_t nparams1,fivmr_TypeStub **params1,
                              int32_t nparams2,fivmr_TypeStub **params2);

fivmr_TypeStub *fivmr_TypeStub_find2(fivmr_TypeStub **startPtr);

fivmr_TypeData *fivmr_TypeStub_tryGetTypeData(fivmr_TypeStub *start);

fivmr_TypeData *fivmr_TypeStub_tryGetTypeData2(fivmr_TypeStub **startPtr);

static inline fivmr_StaticTypeContext *fivmr_TypeStub_getStaticContext(fivmr_TypeStub *ts) {
    return ts->context;
}

static inline fivmr_TypeContext *fivmr_TypeStub_getContext(fivmr_TypeStub *ts) {
    return fivmr_TypeContext_fromStatic(fivmr_TypeStub_getStaticContext(ts));
}

fivmr_MachineCode *fivmr_MachineCode_create(int32_t size,fivmr_MachineCodeFlags flags);

void fivmr_MachineCode_downsize(fivmr_MachineCode *code,
                                int32_t newSize);

fivmr_MachineCode *fivmr_MachineCode_up(fivmr_MachineCode *code);

void fivmr_MachineCode_down(fivmr_MachineCode *code);

void fivmr_MachineCode_registerMC(fivmr_MachineCode *parent,
                                  fivmr_MachineCode *child);

static inline fivmr_MethodRec *fivmr_MachineCode_decodeMethodRec(uintptr_t pointer) {
    int32_t *flags=(int32_t*)(void*)(pointer&~(sizeof(void*)-1));
    if (((*flags) & FIVMR_MBF_COOKIE)) {
        return (fivmr_MethodRec*)flags;
    } else {
        return ((fivmr_MachineCode*)flags)->mr;
    }
}

static inline bool fivmr_MachineCode_isMachineCode(uintptr_t pointer) {
    int32_t *flags=(int32_t*)(void*)(pointer&~(sizeof(void*)-1));
    if (((*flags) & FIVMR_MBF_COOKIE)) {
        return false;
    } else {
        return true;
    }
}

static inline fivmr_MachineCode *fivmr_MachineCode_decodeMachineCode(uintptr_t pointer) {
    int32_t *flags=(int32_t*)(void*)(pointer&~(sizeof(void*)-1));
    if (((*flags) & FIVMR_MBF_COOKIE)) {
        return NULL;
    } else {
        return (fivmr_MachineCode*)flags;
    }
}

static inline bool fivmr_MachineCode_isPatchPoint(uintptr_t pointer) {
    return (pointer&FIVMR_DR_MFL_PATCH_POINT)!=0;
}

/* this is a strong cas */
static inline bool fivmr_MachineCode_casFlags(fivmr_MachineCode *code,
                                              fivmr_MachineCodeFlags expected,
                                              fivmr_MachineCodeFlags newValue) {
    return fivmr_cas32(&code->flags,expected,newValue);
}

/* this is also a strong cas */
static inline bool fivmr_MachineCode_casFlag(fivmr_MachineCode *code,
                                             fivmr_MachineCodeFlags mask,
                                             fivmr_MachineCodeFlags expected,
                                             fivmr_MachineCodeFlags newValue) {
    fivmr_assert((expected&mask)==expected);
    fivmr_assert((newValue&mask)==newValue);
    for (;;) {
        fivmr_MachineCodeFlags oldValue=code->flags;
        if ((oldValue&mask)==expected) {
            if (fivmr_cas32_weak(&code->flags,
                                 oldValue,
                                 (oldValue&~mask)|newValue)) {
                return true;
            }
        } else {
            /* make an atomic observation of the value of flags - great way
               of avoiding memory model weirdness! */
            if (fivmr_cas32_weak(&code->flags,
                                 oldValue,
                                 oldValue)) {
                return false;
            }
        }
        fivmr_spin_fast();
    }
}

static inline void fivmr_MachineCode_setFlag(fivmr_MachineCode *code,
                                             fivmr_MachineCodeFlags mask,
                                             fivmr_MachineCodeFlags newValue) {
    fivmr_assert((newValue&mask)==newValue);
    for (;;) {
        fivmr_MachineCodeFlags oldValue,toSet;
        oldValue=code->flags;
        toSet=(oldValue&~mask)|newValue;
        if (fivmr_cas32_weak(&code->flags,
                             oldValue,
                             toSet)) {
            return;
        }
        fivmr_spin_fast();
    }
}

static inline fivmr_FrameType fivmr_MachineCode_getFrameType(fivmr_MachineCode *code) {
    switch (code->flags&FIVMR_MC_KIND) {
    case FIVMR_MC_BASELINE:
    case FIVMR_MC_JNI_TRAMPOLINE:
    case FIVMR_MC_CLONE_HELPER:
    case FIVMR_MC_EXC_THROW:
    case FIVMR_MC_BASE_PATCH:
        return FIVMR_FT_BASELINE;
    default:
        fivmr_assert(!"bad machine code kind for DebugRec");
        return 0;
    }
}

void fivmr_MachineCode_appendBasepoint(fivmr_MachineCode *code,
                                       int32_t bytecodePC,
                                       int32_t stackHeight,
                                       void *machinecodePC);

void fivmr_MachineCode_appendBaseTryCatch(fivmr_MachineCode *code,
                                          int32_t start,
                                          int32_t end,
                                          int32_t target,
                                          fivmr_TypeStub *type);

static inline int32_t fivmr_MethodRec_numParams(fivmr_MethodRec *mr) {
    return mr->nparams;
}

/* call only IN_JAVA.  the GC will up the ref count, which means you should
   dec it *after* the call to this function.  thereafter, if the machine code
   is not detected on stack scans it will be downed by the GC. */
static inline void fivmr_GC_claimMachineCode(fivmr_ThreadState *ts,
                                             fivmr_MachineCode *mc) {
    ts->vm->gc.claimMachineCode(ts,mc);
}

static inline int32_t fivmr_MethodRec_numAllParams(fivmr_MethodRec *mr) {
    if (mr->flags&FIVMR_BF_STATIC) {
	return mr->nparams;
    } else {
	return mr->nparams+1;
    }
}

static inline int32_t fivmr_MethodRec_numJNIParams(fivmr_MethodRec *mr) {
    return mr->nparams+1;
}

static inline bool fivmr_MethodRec_hasCode(fivmr_MethodRec *mr) {
    return (mr->flags&FIVMR_MBF_HAS_CODE)!=0;
}

static inline bool fivmr_MethodRec_exists(fivmr_MethodRec *mr) {
    return (mr->flags&FIVMR_MBF_EXISTS)!=0;
}

/* a "MethodSig" is any struct that has the following fields:
     const char *name;
     fivmr_TypeData *result;
     int32_t nparams;
     fivmr_TypeData **params;
   This macro does not quite work like a function in that it will evaluate
   the arguments more than once! */
#define fivmr_MethodSig_eq2(a,name,result,nparams,params)               \
    (!strcmp((a)->name,(name)) &&                                       \
     fivmr_TypeStub_eq((a)->result,(result)) &&                         \
     fivmr_TypeStub_paramsEq((a)->nparams,(a)->params,                  \
                             (nparams),(params)))

#define fivmr_MethodSig_eq(a,b)                                         \
    fivmr_MethodSig_eq2(a,(b)->name,(b)->result,(b)->nparams,(b)->params)

#define fivmr_MethodSig_eq4(a,name,result,nparams,params)               \
    (!strcmp((a)->name,(name)) &&                                       \
     fivmr_TypeStub_eq2((a)->result,(result)) &&                        \
     fivmr_TypeStub_paramsEq2((a)->nparams,(a)->params,                 \
                              (nparams),(params)))

#define fivmr_MethodSig_eq3(a,b)                                         \
    fivmr_MethodSig_eq4(a,(b)->name,(b)->result,(b)->nparams,(b)->params)

static inline int32_t fivmr_MethodRec_itableIndexForEpoch(fivmr_MethodRec *mr,
                                                          int32_t typeEpoch) {
#if FIVMSYS_PTRSIZE==4
    fivmr_assert(sizeof(mr->location)==sizeof(int16_t)*2);
    return ((int16_t*)&mr->location)[typeEpoch];
#else
    fivmr_assert(sizeof(mr->location)==sizeof(int32_t)*2);
    return ((int32_t*)&mr->location)[typeEpoch];
#endif
}

static inline void fivmr_MethodRec_setItableIndexForEpoch(fivmr_MethodRec *mr,
                                                          int32_t typeEpoch,
                                                          int32_t idx) {
#if FIVMSYS_PTRSIZE==4
    fivmr_assert(sizeof(mr->location)==sizeof(int16_t)*2);
    ((int16_t*)&mr->location)[typeEpoch]=idx;
#else
    fivmr_assert(sizeof(mr->location)==sizeof(int32_t)*2);
    ((int32_t*)&mr->location)[typeEpoch]=idx;
#endif
}

static inline int32_t fivmr_MethodRec_itableIndex(fivmr_ThreadState *ts,
                                                  fivmr_MethodRec *mr) {
    return fivmr_MethodRec_itableIndexForEpoch(mr,ts->typeEpoch);
}

/* Re-resolve an invokespecial. */
fivmr_MethodRec *fivmr_MethodRec_reresolveSpecial(fivmr_ThreadState *ts,
                                                  fivmr_TypeData *from,
                                                  fivmr_MethodRec *target);

/* Dispatch the method in the given MethodRec* assuming that the call is
   taking place on an object that has the given TypeData*. */
void *fivmr_MethodRec_staticDispatch(fivmr_ThreadState *ts,
                                     fivmr_MethodRec *mr,
				     fivmr_TypeData *td);

/* Dispatch the method in the given MethodRec* assuming that the call is
   taking place on the given object.  If the method is static, the object
   argument is ignored and can have any value.  If the method is dynamic,
   the object argument must be non-zero (otherwise this method will
   abort). */
void *fivmr_MethodRec_dispatch(fivmr_ThreadState *ts,
                               fivmr_MethodRec *mr,
			       fivmr_Object object);

static inline char fivmr_pointerifyBasetype(char descr) {
    switch (descr) {
    case '[':
    case 'L':
    case 'f':
        descr='P';
        break;
    default:
        break;
    }
    return descr;
}

#if FIVMR_HAVE_NATIVE_BACKEND
int64_t fivmr_upcallImpl(void *func,char retType,char *argTypes,int64_t *args);
#endif

/* Calls the given function pointer with the given arguments and argument types,
   and then returns the result.  Note that the type descriptors must correspond
   to Fiji C1 pointerified basetypes; i.e. 'L' and '[' should turn into 'P'.
   Also, 'f' is not supported; use 'P' instead. */
static inline fivmr_Value fivmr_upcall(void *func,
                                       char retType,
                                       char *argTypes,
                                       fivmr_Value *args) {
    fivmr_Value result;
#if FIVMR_HAVE_NATIVE_BACKEND
    fivmr_assert(sizeof(fivmr_Value)==sizeof(int64_t));
    result.J=fivmr_upcallImpl(func,retType,argTypes,(int64_t*)(void*)args);
#else
    fivmr_abortf("fivmr_upcall() not implemented on this platform");
#endif
    return result;
}

static inline int64_t fivmr_upcallJ(void *func,
                                    int8_t retType,
                                    int8_t *argTypes,
                                    int64_t *args) {
#if FIVMR_HAVE_NATIVE_BACKEND
    return fivmr_upcallImpl(func,(char)retType,(char*)argTypes,args);
#else
    fivmr_abortf("fivmr_upcallJ() not implemented on this platform");
    return 0;
#endif
}

/* Call a method reflectively:
   mr        = the method record that describes the signature
   methodPtr = pointer to the method implementation, from either
               fivmr_MethodRec_staticDispatch or
	       fivmr_MethodRec_dispatch, or NULL if cm has the
	       FIVMR_CM_DISPATCH bit set.
   receiver  = the receiver, either as a fivmr_Object or as a
               fivmr_Handle*.  ignored for static methods.
   args      = the arguments, excluding the receiver, and excluding
               any other calling-convention specific stuff (like
	       the ThreadState) */
fivmr_Value fivmr_MethodRec_call(fivmr_MethodRec *mr,
				 fivmr_ThreadState *ts,
				 void *methodPtr,
				 void *receiver,
				 fivmr_Value *args,
				 fivmr_CallMode cm);

/* hack to allow fivmr_MethodRec_call to work from Java */
static inline int64_t fivmr_MethodRec_callJ(fivmr_MethodRec *mr,
					    fivmr_ThreadState *ts,
					    void *methodPtr,
					    void *receiver,
					    fivmr_Value *args,
					    fivmr_CallMode cm) {
    return fivmr_MethodRec_call(mr,ts,methodPtr,receiver,args,cm).J;
}

bool fivmr_MethodRec_matchesSig(fivmr_MethodRec *mr,
				const char *sig);

/* register a MachineCode with the MethodRec.  this will up it.  call only IN_NATIVE
   because it may want to grab the global type data lock. */
void fivmr_MethodRec_registerMC(fivmr_MethodRec *mr,
                                fivmr_MachineCode *mc);

/* unregister a MachineCode from the MethodRec.  this will down it.  call only IN_NATIVE. */
void fivmr_MethodRec_unregisterMC(fivmr_MethodRec *mr,
                                  fivmr_MachineCode *mc);

/* attempts to find the first registered machine code for which
   (mc->flags&mask)==expected.  if it's found, the ref count is up'd and the
   machine code is returned.  THIS MEANS YOU HAVE TO DOWN THE RC.  if it's
   not found this returns NULL. */
fivmr_MachineCode *fivmr_MethodRec_findMC(fivmr_MethodRec *mr,
                                          fivmr_MachineCodeFlags mask,
                                          fivmr_MachineCodeFlags expected);

bool fivmr_MethodRec_hasMC(fivmr_MethodRec *mr,
                           fivmr_MachineCodeFlags mask,
                           fivmr_MachineCodeFlags expected);

static inline int fivmr_TypeData_compareKey(uintptr_t a,
                                            uintptr_t b) {
    int res;
    res=strcmp((const char*)(void*)a,(const char*)(void*)b);
    LOG(11,("comparison of %s, %s: %d",a,b,res));
    return res;
}

static inline uintptr_t
fivmr_TypeData_sizeOfTypeDataForVTableLength(int32_t vtableLength) {
    return sizeof(fivmr_TypeData)-sizeof(uintptr_t)+sizeof(uintptr_t)*vtableLength;
}

static inline uintptr_t fivmr_TypeData_sizeOfTypeData(fivmr_TypeData *td) {
    return fivmr_TypeData_sizeOfTypeDataForVTableLength(td->vtableLength);
}

bool fivmr_TypeData_isSubtypeOfSlow(fivmr_ThreadState *ts,
                                    fivmr_TypeData *a,
                                    fivmr_TypeData *b);

static inline bool fivmr_TypeData_isSubtypeOfFast(fivmr_ThreadState *ts,
                                                  fivmr_TypeData *a,
                                                  fivmr_TypeData *b) {
    unsigned typeEpoch;
    fivmr_TypeEpoch *ate;
    fivmr_TypeEpoch *bte;
#if FIVMBUILD_FORCE__USE_TYPE_EPOCHS && !FIVMBUILD__USE_TYPE_EPOCHS
    typeEpoch=0;
#else
    typeEpoch=ts->typeEpoch;
#endif
    ate=a->epochs+typeEpoch;
    bte=b->epochs+typeEpoch;
    return ate->buckets[bte->bucket]==bte->tid;
}

/* answers: is 'a' a subtype of 'b'? */
static inline bool fivmr_TypeData_isSubtypeOf(fivmr_ThreadState *ts,
                                              fivmr_TypeData *a,
					      fivmr_TypeData *b) {
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    if ((a->flags&(FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED))
        ==FIVMR_TBF_RESOLUTION_DONE &&
        (b->flags&(FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED))
        ==FIVMR_TBF_RESOLUTION_DONE) {
        return fivmr_TypeData_isSubtypeOfFast(ts,a,b);
    } else {
        return fivmr_TypeData_isSubtypeOfSlow(ts,a,b);
    }
}

static inline fivmr_TypeData *fivmr_TypeData_forObject(fivmr_Settings *settings,
                                                       fivmr_Object o) {
    return fivmr_ObjHeader_getTypeData(
        settings,
        fivmr_ObjHeader_forObject(settings,o));
}

static inline bool fivmr_Object_isSubtypeOfFast(fivmr_ThreadState *ts,
                                                fivmr_Object o,
                                                fivmr_TypeData *td) {
    return 
        fivmr_TypeData_isSubtypeOfFast(
            ts,
            fivmr_TypeData_forObject(&ts->vm->settings,o),
            td);
}

static inline bool fivmr_Object_isSubtypeOfAndNonNullFast(fivmr_ThreadState *ts,
                                                          fivmr_Object o,
                                                          fivmr_TypeData *td) {
    return o!=0 && fivmr_Object_isSubtypeOfFast(ts,o,td);
}

static inline bool fivmr_Object_isSubtypeOfOrNullFast(fivmr_ThreadState *ts,
                                                      fivmr_Object o,
                                                      fivmr_TypeData *td) {
    return o==0 || fivmr_Object_isSubtypeOfFast(ts,o,td);
}

static inline bool fivmr_Object_isSubtypeOfArrayElementOrNullFast(fivmr_ThreadState *ts,
                                                                  fivmr_Object obj,
                                                                  fivmr_Object arr) {
    return fivmr_Object_isSubtypeOfOrNullFast(
        ts,obj,
        fivmr_TypeData_forObject(&ts->vm->settings,arr)->arrayElement);
}

static inline void *fivmr_Object_resolveVirtualCall(fivmr_ThreadState *ts,
                                                    fivmr_Object obj,
                                                    uintptr_t vtableIndex) {
    unsigned typeEpoch;
    fivmr_TypeData *td;
    td=fivmr_TypeData_forObject(&ts->vm->settings,obj);
    return td->vtable[vtableIndex];
}

static inline void *fivmr_Object_resolveInterfaceCall(fivmr_ThreadState *ts,
                                                      fivmr_Object obj,
                                                      uintptr_t itableIndex) {
    unsigned typeEpoch;
    fivmr_TypeEpoch *te;
    fivmr_TypeData *td;
    td=fivmr_TypeData_forObject(&ts->vm->settings,obj);
#if FIVMBUILD_FORCE__USE_TYPE_EPOCHS && !FIVMBUILD__USE_TYPE_EPOCHS
    typeEpoch=0;
#else
    typeEpoch=ts->typeEpoch;
#endif
    te=td->epochs+typeEpoch;
    return te->itable[itableIndex];
}

static inline fivmr_TypeData *fivmr_TypeData_fromClass_unsafe(fivmr_VM *vm,
                                                              fivmr_Object c) {
    return *(fivmr_TypeData**)fivmr_addressOfField(&vm->settings,
                                                   c,
                                                   vm->payload->classTDOffset);
}

static inline fivmr_TypeData *fivmr_TypeData_fromClass(fivmr_ThreadState *ts,
                                                       fivmr_Object c) {
    return (fivmr_TypeData*)(void*)fivmr_objectGetField(ts,c,ts->vm->payload->classTDOffset,0);
}

fivmr_TypeData *fivmr_TypeData_fromClass_inNative(fivmr_ThreadState *ts,
                                                  fivmr_Handle *h);

static inline fivmr_StaticTypeContext *fivmr_TypeData_getStaticContext(fivmr_TypeData *td) {
    return td->context;
}

static inline fivmr_TypeContext *fivmr_TypeData_getContext(fivmr_TypeData *td) {
    return fivmr_TypeContext_fromStatic(fivmr_TypeData_getStaticContext(td));
}

static inline fivmr_VM *fivmr_TypeData_getVM(fivmr_TypeData *td) {
    return fivmr_TypeData_getContext(td)->vm;
}

static inline fivmr_VM *fivmr_MethodRec_getVM(fivmr_MethodRec *mr) {
    return fivmr_TypeData_getVM(mr->owner);
}

static inline fivmr_VM *fivmr_MachineCode_getVM(fivmr_MachineCode *mc) {
    return fivmr_MethodRec_getVM(mc->mr);
}

static inline fivmr_TypeData *fivmr_Object_getTypeData(fivmr_Settings *settings,
                                                       fivmr_Object o) {
    return fivmr_TypeData_forObject(settings,o);
}

fivmr_TypeData *fivmr_TypeData_forHandle(fivmr_Handle *h);

static inline fivmr_Object fivmr_TypeData_asClass(fivmr_TypeData *td) {
    return td->classObject;
}

fivmr_TypeData *fivmr_TypeData_makeArray(fivmr_TypeData *td);

static inline fivmr_TypeData *fivmr_TypeData_arrayElement(fivmr_TypeData *td) {
    return td->arrayElement;
}

static inline int32_t fivmr_TypeData_size(fivmr_TypeData *td) {
    return td->size;
}

static inline int32_t fivmr_TypeData_unalignedSize(fivmr_TypeData *td) {
    return td->size-td->sizeAlignDiff;
}

static inline int32_t fivmr_TypeData_refSize(fivmr_TypeData *td) {
    return td->refSize;
}

int32_t fivmr_basetypeSize(char c);

static inline int32_t fivmr_TypeStub_refSize(fivmr_TypeStub *td) {
    return fivmr_basetypeSize(td->name[0]);
}

static inline int32_t fivmr_TypeData_elementSize(fivmr_TypeData *td) {
    return td->arrayElement->refSize;
}

static inline int32_t fivmr_TypeData_arraySize(fivmr_TypeData *td,
                                               fivmr_Settings *settings,
					       uint32_t length) {
    uintptr_t eleSize=fivmr_TypeData_elementSize(td);
    return fivmr_alignRaw(FIVMR_TOTAL_HEADER_SIZE(settings)+4,eleSize)+eleSize*length;
}

static inline uint16_t fivmr_TypeData_requiredAlignment(fivmr_TypeData *td) {
    return td->requiredAlignment;
}

static inline bool fivmr_TypeData_isArray(fivmr_TypeData *td) {
    return td->arrayElement!=NULL;
}

int32_t fivmr_TypeData_arrayDepth(fivmr_TypeData *td);
fivmr_TypeData *fivmr_TypeData_openArray(fivmr_TypeData *td,int32_t depth);
fivmr_TypeData *fivmr_TypeData_closeArray(fivmr_TypeData *td,int32_t depth);

static inline bool fivmr_TypeData_isInterface(fivmr_TypeData *td) {
    return (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION
	|| (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE;
}

static inline bool fivmr_TypeData_isPrimitive(fivmr_TypeData *td) {
    if (false) {
        printf("td = %p\n",td);
        printf("td->flags = %d\n",td->flags);
        printf("td->flags&FIVMR_TBF_TYPE_KIND = %d\n",td->flags&FIVMR_TBF_TYPE_KIND);
    }
    return (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_PRIMITIVE;
}

static inline bool fivmr_TypeData_isBasetype(fivmr_TypeData *td) {
    bool result=fivmr_TypeData_isPrimitive(td) || td==fivmr_TypeData_getVM(td)->payload->td_Object;
    fivmr_assert(result==(td->parent==fivmr_TypeData_getVM(td)->payload->td_top));
    return result;
}

static inline bool fivmr_TypeData_arrayBase(fivmr_TypeData *td) {
    for (;;) {
        fivmr_TypeData *next=td->arrayElement;
        if (next==NULL) {
            return td;
        }
        td=next;
    }
}

static inline const char *fivmr_TypeData_name(fivmr_TypeData *td) {
    return td->name;
}

static inline const char *fivmr_TypeData_filename(fivmr_TypeData *td) {
    return td->filename;
}

static inline fivmr_BindingFlags fivmr_TypeData_flags(fivmr_TypeData *td) {
    return td->flags;
}

static inline fivmr_TypeData *fivmr_TypeData_parent(fivmr_TypeData *td) {
    return td->parent;
}

static inline int32_t fivmr_TypeData_nSuperInterfaces(fivmr_TypeData *td) {
    return (int32_t)td->nSuperInterfaces;
}

static inline fivmr_TypeData **fivmr_TypeData_superInterfaces(fivmr_TypeData *td) {
    return td->superInterfaces;
}

static inline fivmr_TypeData *fivmr_TypeData_getSuperInterface(fivmr_TypeData *td,
							       int32_t i) {
    return td->superInterfaces[i];
}

static inline int32_t fivmr_TypeData_numMethods(fivmr_TypeData *td) {
    return (int32_t)td->numMethods;
}

static inline int32_t fivmr_TypeData_numFields(fivmr_TypeData *td) {
    return (int32_t)td->numFields;
}

static inline fivmr_MethodRec *fivmr_TypeData_method(fivmr_TypeData *td,
						     int32_t i) {
    return td->methods[i];
}

static inline fivmr_FieldRec *fivmr_TypeData_field(fivmr_TypeData *td,
						   int32_t i) {
    return td->fields+i;
}

/* find an instance method without searching the type hierarchy */
fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch(fivmr_TypeData *td,
                                                       const char *name,
                                                       const char *sig);

/* same as the previous function, but uses symbolic type information rather
   than string signatures */
fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch2(fivmr_TypeData *td,
                                                        const char *name,
                                                        fivmr_TypeStub *result,
                                                        int32_t nparams,
                                                        fivmr_TypeStub **params);

/* same as the previous function, but compares type stub names rather than
   using type stub equality constraints */
fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch3(fivmr_TypeData *td,
                                                        const char *name,
                                                        fivmr_TypeStub *result,
                                                        int32_t nparams,
                                                        fivmr_TypeStub **params);

/* find an instance method, searching the type hierarchy but ignoring
   interfaces.  this method is also not particularly efficient when
   dealing with arrays. */
fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface(fivmr_VM *vm,
                                                      fivmr_TypeData *td,
                                                      const char *name,
                                                      const char *sig);

/* same as the previous function, but uses symbolic type information rather
   than string signatures */
fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface2(fivmr_VM *vm,
                                                       fivmr_TypeData *td,
                                                       const char *name,
                                                       fivmr_TypeStub *result,
                                                       int32_t nparams,
                                                       fivmr_TypeStub **params);

/* same as the previous function, but compares type stub names rather than
   using type stub equality constraints */
fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface3(fivmr_VM *vm,
                                                       fivmr_TypeData *td,
                                                       const char *name,
                                                       fivmr_TypeStub *result,
                                                       int32_t nparams,
                                                       fivmr_TypeStub **params);

/* find an instance method, performing complete resolution including
   interfaces */
fivmr_MethodRec *fivmr_TypeData_findInstMethod(fivmr_VM *vm,
                                               fivmr_TypeData *td,
                                               const char *name,
                                               const char *sig);

/* find a static method, without searching the type hierarchy. */
fivmr_MethodRec *fivmr_TypeData_findStaticMethod(fivmr_VM *vm,
                                                 fivmr_TypeData *td,
						 const char *name,
						 const char *sig);

/* find either an instance of static method, without searching the
   type hierarchy. */
fivmr_MethodRec *fivmr_TypeData_findMethod(fivmr_VM *vm,
                                           fivmr_TypeData *td,
					   const char *name,
					   const char *sig);

/* find a field, without searching the type hierarchy. */
fivmr_FieldRec *fivmr_TypeData_findField(fivmr_TypeData *td,
                                         const char *name,
                                         const char *sig);

/* find a static field */
fivmr_FieldRec *fivmr_TypeData_findStaticField(fivmr_TypeData *td,
                                               const char *name,
                                               const char *sig);

/* find an instance field, without searching the type hierarchy */
fivmr_FieldRec *fivmr_TypeData_findInstFieldNoSearch(fivmr_TypeData *td,
                                                     const char *name,
                                                     const char *sig);

/* find an instance field, searching the class hierarchy as appropriate. */
fivmr_FieldRec *fivmr_TypeData_findInstField(fivmr_TypeData *td,
                                             const char *name,
                                             const char *sig);

fivmr_TypeData *fivmr_TypeStub_checkInit(fivmr_ThreadState *ts,
                                         fivmr_TypeStub *st);

fivmr_TypeData *fivmr_TypeStub_resolve(fivmr_ThreadState *ts,
                                       fivmr_TypeStub *st);

/* initialize the type if needed.  returns true if everything was fine,
   or false if an exception was thrown.  (you don't have to check the
   return value, since curException will be set if there was an
   error.)

   Note: call this when IN_JAVA.  also, the exception will not be
   handlified. */
bool fivmr_TypeData_checkInit(fivmr_ThreadState *ts,
                              fivmr_TypeData *td);

static inline bool fivmr_TypeData_checkInitFast(fivmr_ThreadState *ts,
                                                fivmr_TypeData *td) {
    if (td->inited!=1) {
        return fivmr_TypeData_checkInit(ts,td);
    } else {
        return true;
    }
}

/* initialize the class if needed and abort if we fail.  the class name
   is specified in / form like java/lang/String.

   Note: call this when IN_NATIVE.*/
void fivmr_TypeData_checkInitEasy(fivmr_ThreadState *ts,
                                  const char *name);

static inline fivmr_TypeData **fivmr_TypeData_list(fivmr_VM *vm) {
    return vm->payload->typeList;
}

static inline int32_t fivmr_TypeData_numTypes(fivmr_VM *vm) {
    return vm->payload->nTypes;
}

/*
 * This tells you if the object is contiguous from the standpoint of the GC;
 * i.e. the collector allocated this object in one chunk rather than
 * allocating multiple chunks.
 */
static inline bool fivmr_Object_isContiguous(fivmr_Settings *settings,
                                             fivmr_Object obj) {
    if (FIVMR_OM_CONTIGUOUS(settings) || !FIVMR_HFGC(settings)) {
        return true;
    } else if (FIVMR_OM_FRAGMENTED(settings)) {
        fivmr_TypeData *td=fivmr_TypeData_forObject(settings,obj);
        if (fivmr_TypeData_isArray(td)) {
            int32_t len=*(int32_t*)(obj+FIVMR_FRAG_ARR_LEN_OFF);
            if (len==0) {
                int32_t *spine=*(int32_t**)obj;
                if (spine==NULL) {
                    return false;
                } else {
                    return spine[-1]==0;
                }
            } else {
                return true;
            }
        } else {
            return fivmr_TypeData_size(td)<=(int32_t)FIVMR_GC_BLOCK_SIZE
                || ((*(uintptr_t*)obj)&1)!=0;
        }
    } else {
        fivmr_assert(!"bad object model");
        return false;
    }
}

/*
 * This tells you the object's size provided that it is contiguous from
 * the standpoint of the collector (i.e. the collector had allocated the
 * object in a single chunk; allocating in separate chunks that just
 * so happened to end up being contiguous doesn't count).  If the object
 * is not contiguous, the behavior of this function is undefiend (i.e.
 * it is free to crash, corrupt memory, launch nuclear missiles, etc.).
 * Thus, unless you have "special knowledge", call
 * fivmr_Object_isContiguous() before calling this function.
 */
static inline uintptr_t fivmr_Object_size(fivmr_Settings *settings,
                                          fivmr_Object obj) {
    fivmr_TypeData *td=fivmr_TypeData_forObject(settings,obj);
    if (fivmr_TypeData_isArray(td)) {
        uintptr_t eleSize=fivmr_TypeData_elementSize(td);
        if (FIVMR_OM_CONTIGUOUS(settings)) {
            return fivmr_alignRaw(
                fivmr_alignRaw(FIVMR_CONT_TOTAL_HEADER_SIZE+4,
                               eleSize)+
                fivmr_arrayLengthImpl(settings,obj,0)*eleSize,
                sizeof(uintptr_t));
        } else if (FIVMR_OM_FRAGMENTED(settings)) {
            int32_t len=*(int32_t*)(obj+FIVMR_FRAG_ARR_LEN_OFF);
            if (len==0) {
                if (!FIVMR_HFGC(settings) && FIVMR_FORCE_ARRAYLETS(settings)) {
                    uintptr_t payloadSize;
                    fivmr_assert(*(int32_t**)obj-1 == (int32_t*)(obj+FIVMR_FRAG_ARR_LEN_OFF+4));
                    len=*(int32_t*)(obj+FIVMR_FRAG_ARR_LEN_OFF+4);
                    payloadSize=eleSize*len;
                    return fivmr_alignRaw(
                        FIVMR_FRAG_TOTAL_HEADER_SIZE+4+4+((payloadSize+FIVMR_GC_BLOCK_SIZE-1)&~(FIVMR_GC_BLOCK_SIZE-1))+(payloadSize+FIVMR_GC_BLOCK_SIZE-1)/FIVMR_GC_BLOCK_SIZE*sizeof(uintptr_t),
                        FIVMR_GC_BLOCK_SIZE);
                } else {
                    fivmr_assert((*(int32_t**)obj)[-1]==0);
                    return fivmr_alignRaw(
                        FIVMR_FRAG_TOTAL_HEADER_SIZE+4,
                        FIVMR_GC_BLOCK_SIZE);
                }
            } else {
                return fivmr_alignRaw(
                    fivmr_alignRaw(FIVMR_FRAG_TOTAL_HEADER_SIZE+4,
                                   eleSize)+
                    len*eleSize,
                    FIVMR_FRAG_OBJ_SIZE_ALIGN);
            }
        } else {
            fivmr_abort("bad object model");
            return 0;
        }
    } else {
	return fivmr_alignRaw(
            fivmr_TypeData_size(td),
            FIVMR_OBJ_SIZE_ALIGN(settings));
    }
}

static inline uintptr_t fivmr_Object_getSpineLength(fivmr_Settings *settings,
                                                    fivmr_Object o) {
    return ((fivmr_arrayLengthImpl(settings,o,0)*
             fivmr_TypeData_elementSize(fivmr_TypeData_forObject(settings,o)))+
            FIVMR_GC_BLOCK_SIZE-1)/FIVMR_GC_BLOCK_SIZE;
}

static inline uintptr_t fivmr_Spine_calcSize(uintptr_t spineLength) {
    return spineLength*sizeof(uintptr_t)+FIVMR_FRAG_SP_HEADER;
}

/* this method should only be called for objects, not raw types */
static inline uintptr_t fivmr_GCHeader_chunkStart(fivmr_Settings *settings,
                                                  fivmr_GCHeader *hdr) {
    if (FIVMR_OM_CONTIGUOUS(settings)) {
        return (uintptr_t)hdr;
    } else if (FIVMR_OM_FRAGMENTED(settings)) {
        return ((uintptr_t)hdr)-sizeof(uintptr_t);
    } else {
        fivmr_assert(!"bad object model");
        return 0;
    }
}

/* this method should only be called for objects, not raw types */
static inline fivmr_GCHeader *fivmr_GCHeader_fromStart(fivmr_Settings *settings,
                                                       uintptr_t start) {
    if (FIVMR_OM_CONTIGUOUS(settings)) {
        return (fivmr_GCHeader*)start;
    } else if (FIVMR_OM_FRAGMENTED(settings)) {
        return (fivmr_GCHeader*)(start+sizeof(uintptr_t));
    } else {
        fivmr_assert(!"bad object model");
        return NULL;
    }
}

typedef void (*fivmr_MarkCback)(fivmr_VM *vm,
                                fivmr_Object *reference,
                                void *arg);

typedef void (*fivmr_WeakRefCback)(fivmr_VM *vm,
                                   fivmr_Object weakRef,
                                   void *arg);

typedef struct {
    fivmr_WeakRefCback weakRef;
} fivmr_ScanSpecialHandlers;

void fivmr_Object_specialScan(fivmr_VM *vm,
                              fivmr_Object object,
                              fivmr_TypeData *td,
                              fivmr_MarkCback mark,
                              fivmr_ScanSpecialHandlers *specials,
                              void *arg);

static inline void fivmr_Object_scan(fivmr_VM *vm,
                                     fivmr_Object obj,
                                     fivmr_TypeData *td,
                                     fivmr_MarkCback mark,
                                     fivmr_ScanSpecialHandlers *specials,
                                     void *arg) FORCE_INLINE_ATTR;

static inline void fivmr_Object_scan(fivmr_VM *vm,
                                     fivmr_Object obj,
                                     fivmr_TypeData *td,
                                     fivmr_MarkCback mark,
                                     fivmr_ScanSpecialHandlers *specials,
                                     void *arg) {
    fivmr_Settings *settings=&vm->settings;
    mark(vm,&td->classObject,arg);
    if (fivmr_unlikely((td->flags&FIVMR_TBF_SPECIAL_SCAN))) {
        fivmr_Object_specialScan(vm,obj,td,mark,specials,arg);
    }
    if (FIVMR_OM_CONTIGUOUS(settings)) {
        if (fivmr_TypeData_isArray(td)) {
            /* array case */

            const char *tdName=td->name;
            /* this test can be made more optimal */
            if (tdName[1]=='L' ||
                tdName[1]=='[') {
                int32_t len;
                int32_t i;
                fivmr_Object *ptrPtr;
                len=*(int32_t*)(obj+FIVMR_CONT_ARR_LEN_OFF);
                ptrPtr=(fivmr_Object*)
                    (obj+
                     FIVMR_CONT_ARR_PAYLOAD_OFF(
                         sizeof(uintptr_t)));
                for (i=len;i-->0;) {
                    mark(vm,ptrPtr++,arg);
                }
            }
        } else {
            /* scalar case */

            uintptr_t gcMap;
            gcMap=td->gcMap;
            if ((gcMap&FIVMR_GCM_THIN)) {
                /* this can be SIGNIFICANTLY optimized */
                fivmr_Object *ptrPtr;
                ptrPtr=(fivmr_Object*)
                    (obj+FIVMR_CONT_OBJ_PAYLOAD_OFF);
                for (gcMap>>=1;gcMap;gcMap>>=1) {
                    if ((gcMap&1)) {
                        mark(vm,ptrPtr,arg);
                    }
                    ptrPtr++;
                }
            } else {
                fivmr_FatGCMap *fatGCMap;
                int32_t i;
                uintptr_t fieldStart;
                fieldStart=obj+FIVMR_CONT_OBJ_PAYLOAD_OFF;
                fatGCMap=(fivmr_FatGCMap*)gcMap;
                for (i=fatGCMap->numPtrs;i-->0;) {
                    mark(vm,
                         (fivmr_Object*)(
                             fieldStart+fatGCMap->offsets[i]),
                        arg);
                }
            }
        }
    } else if (FIVMR_OM_FRAGMENTED(settings)) {
        if (fivmr_TypeData_isArray(td)) {
            /* array case */

            const char *tdName=td->name;
            /* this test can be made more optimal */
            if (tdName[1]=='L' ||
                tdName[1]=='[') {
                int32_t len=
                    *(int32_t*)(obj+FIVMR_FRAG_ARR_LEN_OFF);
                if (len!=0) {
                    /* contiguous array */

                    int32_t i;
                    fivmr_Object *ptrPtr;
                    ptrPtr=(fivmr_Object*)
                        (obj+
                         FIVMR_FRAG_ARR_PAYLOAD_OFF(
                             sizeof(uintptr_t)));
                    for (i=len;i-->0;) {
                        mark(vm,ptrPtr++,arg);
                    }
                } else {
                    /* arraylet */

                    int32_t numChunks;
                    int32_t i;
                    int32_t j;
                    uintptr_t *spinePtr;
                    uintptr_t *chunkPtr;

                    spinePtr=*(uintptr_t**)obj;
                    if (spinePtr!=NULL) {
                        len=((int32_t*)spinePtr)[-1];

                        numChunks=
                            (len*sizeof(uintptr_t)+
                             FIVMR_GC_BLOCK_SIZE-1)
                            /FIVMR_GC_BLOCK_SIZE;

                        for (i=numChunks;i-->0;) {
                            chunkPtr=(uintptr_t*)*spinePtr;
                            if (chunkPtr!=NULL) {
                                for (j=(FIVMR_GC_BLOCK_SIZE/
                                        sizeof(uintptr_t));
                                     j-->0;) {
                                    mark(vm,chunkPtr,arg);
                                    chunkPtr--;
                                }
                            }
                            spinePtr++;
                        }
                    }
                }
            }
        } else {
            /* scalar case */

            uintptr_t gcMap;
            gcMap=td->gcMap;
            if ((gcMap&FIVMR_GCM_THIN)) {
                /* this can be SIGNIFICANTLY optimized */
                fivmr_Object *curBase;
                uintptr_t idx=
                    FIVMR_FRAG_OBJ_PAYLOAD_OFF/sizeof(uintptr_t);
                curBase=(fivmr_Object*)obj;
                for (gcMap>>=1;gcMap;gcMap>>=1) {
                    if ((gcMap&1)) {
                        mark(vm,curBase+idx,arg);
                    }
                    idx++;
                    if (idx==(FIVMR_GC_BLOCK_SIZE/
                              sizeof(uintptr_t))) {
                        fivmr_assert((idx%8)==0);
                        /* crossed block boundary ... */
                        curBase=(fivmr_Object*)((*curBase)&~1);
                        if (curBase==NULL) {
                            break;
                        }
                        idx-=FIVMR_GC_BLOCK_SIZE/sizeof(uintptr_t);
                    }
                }
            } else {
                fivmr_FatGCMap *fatGCMap;
                int32_t i;
                uintptr_t curBase;
                int32_t lastIdx=0;
                curBase=obj;
                fatGCMap=(fivmr_FatGCMap*)gcMap;
                for (i=0;i<fatGCMap->numPtrs;++i) {
                    int32_t curIdx;
                    int32_t blockIdx;
                    int32_t blockOff;

                    curIdx=fatGCMap->offsets[i];
                    blockIdx=curIdx/FIVMR_GC_BLOCK_SIZE;
                    while (lastIdx<blockIdx) {
                        curBase=(*(uintptr_t*)curBase)&~1;
                        if (curBase==0) {
                            goto doneScanning;
                        }
                        lastIdx++;
                    }

                    blockOff=curIdx&(FIVMR_GC_BLOCK_SIZE-1);

                    mark(vm,
                         (fivmr_Object*)(curBase+blockOff),
                        arg);
                }
            doneScanning: do {} while(0);
            }
        }
    }
}

/* WARNING: this assumes a non-moving GC or else object pinning!
   FIXME: handles should have a bit to indicate pinning, and the handle
          returned here should have this bit set. */
fivmr_Handle *
fivmr_GateHelpers_installObjectFieldReference(fivmr_ThreadState *ts,
                                              fivmr_Object referent,
                                              uintptr_t fieldOffset,
                                              fivmr_TypeData *td);

fivmr_Handle *
fivmr_GateHelpers_installArrayFieldReference(fivmr_ThreadState *ts,
                                             fivmr_Object referent,
                                             uintptr_t fieldOffset,
                                             fivmr_TypeData *td,
                                             int32_t length);

fivmr_Handle *
fivmr_GateHelpers_installObjectElementReference(fivmr_ThreadState *ts,
                                                fivmr_Object referent,
                                                int32_t index,
                                                fivmr_TypeData *td);

fivmr_Handle *
fivmr_GateHelpers_installArrayElementReference(fivmr_ThreadState *ts,
                                               fivmr_Object referent,
                                               int32_t index,
                                               fivmr_TypeData *td,
                                               int32_t length);

static inline fivmr_TypeData *fivmr_MethodRec_owner(fivmr_MethodRec *mr) {
    return mr->owner;
}

static inline const char *fivmr_MethodRec_name(fivmr_MethodRec *mr) {
    return mr->name;
}

static inline bool fivmr_MethodRec_isConstructor(fivmr_MethodRec *mr) {
    return !strcmp(mr->name,"<init>");
}

static inline bool fivmr_MethodRec_isStaticInit(fivmr_MethodRec *mr) {
    return !strcmp(mr->name,"<clinit>");
}

static inline bool fivmr_MethodRec_isInitializer(fivmr_MethodRec *mr) {
    return fivmr_MethodRec_isConstructor(mr)
	|| fivmr_MethodRec_isStaticInit(mr);
}

static inline int32_t fivmr_MethodRec_flags(fivmr_MethodRec *mr) {
    return mr->flags;
}

static inline fivmr_TypeStub *fivmr_MethodRec_result(fivmr_MethodRec *mr) {
    return mr->result;
}

static inline fivmr_TypeData *fivmr_MethodRec_resolvedResult(fivmr_MethodRec *mr) {
    return fivmr_TypeStub_getTypeData(mr->result);
}

static inline int32_t fivmr_MethodRec_nparams(fivmr_MethodRec *mr) {
    return mr->nparams;
}

static inline fivmr_TypeStub *fivmr_MethodRec_param(fivmr_MethodRec *mr,
						    int32_t i) {
    return mr->params[i];
}

static inline fivmr_TypeData *fivmr_MethodRec_resolvedParam(fivmr_MethodRec *mr,
                                                            int32_t i) {
    return fivmr_TypeStub_getTypeData(mr->params[i]);
}

static inline uintptr_t fivmr_MethodRec_location(fivmr_MethodRec *mr) {
    return mr->location;
}

static inline void *fivmr_MethodRec_entrypoint(fivmr_MethodRec *mr) {
    return mr->entrypoint;
}

#if FIVMR_VERBOSE_RUN_METHOD
void fivmr_MethodRec_logEntry(fivmr_ThreadState *ts,
                              fivmr_MethodRec *mr);
void fivmr_MethodRec_logExit(fivmr_ThreadState *ts,
                             fivmr_MethodRec *mr);

void fivmr_MethodRec_logResultInt(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr,
                                  int32_t result);
void fivmr_MethodRec_logResultLong(fivmr_ThreadState *ts,
                                   fivmr_MethodRec *mr,
                                   int64_t result);
void fivmr_MethodRec_logResultFloat(fivmr_ThreadState *ts,
                                    fivmr_MethodRec *mr,
                                    float result);
void fivmr_MethodRec_logResultDouble(fivmr_ThreadState *ts,
                                     fivmr_MethodRec *mr,
                                     double result);
void fivmr_MethodRec_logResultPtr(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr,
                                  uintptr_t result);
#endif

const char *fivmr_MethodRec_describe(fivmr_MethodRec *mr);

const char *fivmr_MethodRec_descriptor(fivmr_MethodRec *mr);

const char *fivmr_FieldRec_describe(fivmr_FieldRec *fr);

/* Pass the thing being accessed and the depth.  The depth specifies the
   method from which reflection is being used.  If the depth is -1, the
   native method frame on the top of the stack is used.  If the depth is
   non-negative, we walk the Java stack frames.  Note that in most cases,
   using -1 and 0 is equivalent since the top Java frame will also be
   the native frame.  However, using -1 results in this method being found
   more quickly. */
void fivmr_ReflectLog_dynamicCall(fivmr_ThreadState *ts,
                                  int32_t depth,
                                  fivmr_MethodRec *mr);
void fivmr_ReflectLog_call(fivmr_ThreadState *ts,
                           int32_t depth,
                           fivmr_MethodRec *mr);
void fivmr_ReflectLog_access(fivmr_ThreadState *ts,
                             int32_t depth,
                             fivmr_FieldRec *fr);
void fivmr_ReflectLog_alloc(fivmr_ThreadState *ts,
                            int32_t depth,
                            fivmr_TypeData *td);
void fivmr_ReflectLog_use(fivmr_ThreadState *ts,
                          int32_t depth,
                          fivmr_TypeData *td);

/* Same as above, but automatically finds the top user method that called
   into the reflection API. */
void fivmr_ReflectLog_dynamicCallReflect(fivmr_ThreadState *ts,
                                         fivmr_MethodRec *mr);
void fivmr_ReflectLog_callReflect(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr);
void fivmr_ReflectLog_accessReflect(fivmr_ThreadState *ts,
                                    fivmr_FieldRec *fr);
void fivmr_ReflectLog_allocReflect(fivmr_ThreadState *ts,
                                   fivmr_TypeData *td);
void fivmr_ReflectLog_useReflectByName(fivmr_ThreadState *ts,
                                       const char *name);
void fivmr_ReflectLog_useReflect(fivmr_ThreadState *ts,
                                 fivmr_TypeData *td);

static inline fivmr_DebugRec *fivmr_DebugRec_lookup(fivmr_VM *vm,
                                                    uintptr_t id) {
    if (id&1) {
        /* the DebugRec is in a table; this is the case for AOT-generated
           DebugRec's */
        uintptr_t index=id>>1;
        fivmr_assert(index<(uintptr_t)(intptr_t)vm->payload->nDebugIDs);
        return vm->payload->debugTable+index;
    } else {
        /* the DebugRec is dynamically allocated; this is the case for
           JIT-generated DebugRec's */
        return (fivmr_DebugRec*)(void*)id;
    }
}

static inline uintptr_t fivmr_DebugRec_decodeMethod(uintptr_t method) {
    return method&~(sizeof(void*)-1);
}

static inline uintptr_t fivmr_DebugRec_getMethod(fivmr_DebugRec *dr) {
    return fivmr_DebugRec_decodeMethod(dr->method);
}

static inline uintptr_t fivmr_DebugRec_decodeThinLineNumber(uintptr_t ln_rm_c) {
#if FIVMSYS_PTRSIZE==4
    return (ln_rm_c>>14)&((1<<10)-1);
#else
    return (ln_rm_c>>33)&((UINT64_C(1)<<16)-1);
#endif
}

static inline int32_t fivmr_DebugRec_decodeThinBytecodePC(uintptr_t ln_rm_c) {
#if FIVMSYS_PTRSIZE==4
    return (int32_t)((ln_rm_c>>24)&((1<<8)-1));
#else
    return (int32_t)((ln_rm_c>>49)&((UINT64_C(1)<<15)-1));
#endif
}

static inline uintptr_t fivmr_DebugRec_decodeThinRefMap(uintptr_t ln_rm_c) {
#if FIVMSYS_PTRSIZE==4
    return (ln_rm_c>>2)&((1<<12)-1);
#else
    return (ln_rm_c>>2)&((UINT64_C(1)<<31)-1);
#endif
}

static inline fivmr_FatDebugData *fivmr_DebugRec_decodeFatDebugData(uintptr_t ln_rm_c) {
    return (fivmr_FatDebugData*)(ln_rm_c&~3);
}

static inline uintptr_t fivmr_InlineMethodRec_decodeLineNumber(uintptr_t ln_c) {
    return ln_c>>1;
}

static inline int32_t fivmr_DebugRec_getLineNumber(fivmr_DebugRec *dr) {
    if ((dr->ln_rm_c&FIVMR_DR_FAT)) {
        return fivmr_DebugRec_decodeFatDebugData(dr->ln_rm_c)->lineNumber;
    } else {
        return fivmr_DebugRec_decodeThinLineNumber(dr->ln_rm_c);
    }
}

static inline int32_t fivmr_DebugRec_getBytecodePC(fivmr_DebugRec *dr) {
    if ((dr->ln_rm_c&FIVMR_DR_FAT)) {
        return fivmr_DebugRec_decodeFatDebugData(dr->ln_rm_c)->bytecodePC;
    } else {
        return fivmr_DebugRec_decodeThinBytecodePC(dr->ln_rm_c);
    }
}

/* returns either the MethodRec or the MachineCode corresponding to the
   implementation of the method for this DebugRec */
static inline uintptr_t fivmr_DebugRec_getMethodImpl(fivmr_DebugRec *dr) {
    if ((dr->ln_rm_c&FIVMR_DR_INLINED)) {
        fivmr_InlineMethodRec *imr=
            (fivmr_InlineMethodRec*)(dr->method&~(sizeof(void*)-1));
        while ((imr->ln_c&FIVMR_IMR_INLINED)) {
            imr=(fivmr_InlineMethodRec*)imr->caller;
        }
        return imr->caller;
    } else {
        return dr->method;
    }
}

static inline fivmr_FrameType fivmr_DebugRec_getFrameType(fivmr_Settings *settings,
                                                          fivmr_DebugRec *dr) {
    if (FIVMR_CAN_DO_CLASSLOADING && FIVMR_CLASSLOADING(settings)) {
        uintptr_t impl=fivmr_DebugRec_getMethodImpl(dr);
        if (fivmr_MachineCode_isMachineCode(impl)) {
            return fivmr_MachineCode_getFrameType((fivmr_MachineCode*)impl);
        } else {
            return FIVMR_FT_HENDERSON;
        }
    } else {
        return FIVMR_FT_HENDERSON;
    }
}

fivmr_DebugRec *fivmr_DebugRec_withRootSize(fivmr_DebugRec *dr,
                                            void *region,
                                            int32_t rootSize);

/* FIXME: make it so this can supply bytecode PC information */
typedef uintptr_t (*fivmr_DebugFrameCback)(fivmr_VM *vm,
                                           uintptr_t arg,
					   fivmr_MethodRec *mr,
					   int32_t lineNumber);

/* this typedef is just an example, at least for now.  this is what the type
   signature of a patch point thunk would be, if it returned an int.  All of the
   locals and the stack are stored in a thread-local buffer.  If the buffer is
   not large enough, the relevant method will stack-allocate an overflow buffer.
   Note that the locals come first in the buffer, then comes the stack.  Everything
   is stored as it would be according to the JVMS.  There must be a unique thunk
   installed for every distinct PatchPoint, since the thunk must know the
   details: such as how many locals there are, how high the stack is, and what
   the relevant types are.  As well, it must know how to find its location in
   memory so as to replace itself with the compiled code, once the code is
   compiled. */
typedef int32_t (*fivmr_PatchThunkInt)(fivmr_ThreadState *ts);

uintptr_t fivmr_iterateDebugFrames(fivmr_VM *vm,
                                   fivmr_Frame *f,
				   fivmr_DebugFrameCback cback,
				   uintptr_t arg);

/* dumps the stack starting at a frame.  prints the optional message.
   does appropriate log locking. */
void fivmr_dumpStackFrom(fivmr_VM *vm,
                         fivmr_Frame *f,const char *msg);

/* lower-level stack dumping function.  does no locking (you have to
   do it). */
void fivmr_dumpStackFromNoHeading(fivmr_VM *vm,
                                  fivmr_Frame *f);

uintptr_t fivmr_iterateDebugFrames_forJava(fivmr_VM *vm,
                                           fivmr_Frame *f,
					   fivmr_Object obj);

/* Find the method record by walking the stack by the amount specified by
   depth.  If depth is zero, then the Java method on top of the stack is
   returned. */
fivmr_MethodRec *fivmr_methodForStackDepth(fivmr_VM *vm,
                                           fivmr_Frame *f,
					   int32_t depth);

/* Find a method record by either using the fivmr_Frame or fivmr_NativeFrame.
   if the depth is negative, the method in the native frame is returned.
   Otherwise, this calls fivmr_methodForStackDepth(). */
fivmr_MethodRec *fivmr_findCaller(fivmr_ThreadState *ts,
				  int32_t depth);

fivmr_ThreadStackTrace *fivmr_ThreadStackTrace_get(fivmr_ThreadState *ts);

fivmr_AllThreadStackTraces *fivmr_AllThreadStackTraces_get(fivmr_VM *vm);

void fivmr_ThreadStackTrace_free(fivmr_ThreadStackTrace *tst);

void fivmr_AllThreadStackTraces_free(fivmr_AllThreadStackTraces *atst);

/* internal function, used for supporting AllThreadStackTraces_get() */
void fivmr_Debug_traceStack(fivmr_ThreadState *ts);

/* asks all threads to dump their stacks. */
void fivmr_Debug_dumpAllStacks(fivmr_VM *vm);

/* internal function, used for supporting fivmr_Debug_dumpAllStacks() */
void fivmr_Debug_dumpStack(fivmr_ThreadState *ts);

void fivmr_throw(fivmr_ThreadState *ts,
                 fivmr_Object obj);

static inline double fivmr_fiatLongToDouble(int64_t value) {
    union {
	int64_t l;
	double d;
    } u;
    u.l=value;
    return u.d;
}

static inline int64_t fivmr_fiatDoubleToLong(double value) {
    union {
	int64_t l;
	double d;
    } u;
    u.d=value;
    return u.l;
}

static inline float fivmr_fiatIntToFloat(int32_t value) {
    union {
	int32_t i;
	float f;
    } u;
    u.i=value;
    return u.f;
}

static inline int32_t fivmr_fiatFloatToInt(float value) {
    union {
	int32_t i;
	float f;
    } u;
    u.f=value;
    return u.i;
}

static inline int32_t fivmr_fiatLongToInt(int64_t value) {
    union {
	int64_t l;
	int32_t i;
    } u;
    u.l=value;
    return u.i;
}

static inline float fivmr_fiatLongToFloat(int64_t value) {
    union {
	int64_t l;
	float f;
    } u;
    u.l=value;
    return u.f;
}

static inline int16_t fivmr_fiatLongToShort(int64_t value) {
    union {
	int64_t l;
	int16_t s;
    } u;
    u.l=value;
    return u.s;
}

static inline int16_t fivmr_fiatLongToChar(int64_t value) {
    union {
	int64_t l;
	uint16_t c;
    } u;
    u.l=value;
    return u.c;
}

static inline int8_t fivmr_fiatLongToByte(int64_t value) {
    union {
	int64_t l;
	int8_t b;
    } u;
    u.l=value;
    return u.b;
}

static inline bool fivmr_fiatLongToBoolean(int64_t value) {
    union {
	int64_t l;
	bool b;
    } u;
    u.l=value;
    return u.b;
}

static inline uintptr_t fivmr_fiatLongToPointer(int64_t value) {
    union {
	int64_t l;
	uintptr_t p;
    } u;
    u.l=value;
    return u.p;
}

#if FIVMSYS_PTRSIZE==4
static inline uintptr_t fivmr_firstHalfLong(int64_t value) {
    union {
        uintptr_t p[2];
        int64_t value;
    } u;
    u.value=value;
    return u.p[0];
}
static inline uintptr_t fivmr_secondHalfLong(int64_t value) {
    union {
        uintptr_t p[2];
        int64_t value;
    } u;
    u.value=value;
    return u.p[1];
}
static inline uintptr_t fivmr_firstHalfDouble(double value) {
    union {
        uintptr_t p[2];
        double value;
    } u;
    u.value=value;
    return u.p[0];
}
static inline uintptr_t fivmr_secondHalfDouble(double value) {
    union {
        uintptr_t p[2];
        double value;
    } u;
    u.value=value;
    return u.p[1];
}
#endif

/* call only while in Java */
fivmr_Handle *fivmr_newGlobalHandle(fivmr_ThreadState *ts,
				    fivmr_Handle *h);

/* call only while in Java */
void fivmr_deleteGlobalHandle(fivmr_Handle *h);

static inline int64_t fivmr_GC_freeMemory(fivmr_GC *gc) {
    return gc->freeMemory(gc);
}
static inline int64_t fivmr_GC_totalMemory(fivmr_GC *gc) {
    return gc->totalMemory(gc);
}
static inline int64_t fivmr_GC_maxMemory(fivmr_GC *gc) {
    return gc->maxMemory(gc);
}

/* "fast" call to start GC if it isn't already
   going.  this does not block and may be
   called from either IN_JAVA or IN_NATIVE, but
   it does require acquiring a lock - so it
   isn't as fast as it perhaps could be. */
static inline void fivmr_GC_asyncCollect(fivmr_GC *gc) {
    gc->asyncCollect(gc);
}

#define FIVMR_SYSTEM_GC_STR ("System.gc")
static inline void fivmr_GC_collectFromJava(fivmr_GC *gc,
                                            const char *descrIn,
                                            const char *descrWhat) {
    gc->collectFromJava(gc,descrIn,descrWhat);
}
static inline void fivmr_GC_collectFromNative(fivmr_GC *gc,
                                              const char *descrIn,
                                              const char *descrWhat) {
    gc->collectFromNative(gc,descrIn,descrWhat);
}

static inline bool fivmr_getIgnoreSystemGC(fivmr_GC *gc) { return gc->ignoreSystemGC; }

static inline void fivmr_GC_setPriority(fivmr_GC *gc,
                                        fivmr_ThreadPriority prio) {
    gc->setPriority(gc,prio);
}

/* returns true if downsizing the heap was successful, or false if
   it failed, for example if you're already using more of the heap than
   you're asking to resize down to. */
static inline bool fivmr_GC_setMaxHeap(fivmr_GC *gc,
                                       int64_t bytes) {
    return gc->setMaxHeap(gc,bytes);
}

static inline bool fivmr_GC_setTrigger(fivmr_GC *gc,
                                       int64_t bytes) {
    return gc->setTrigger(gc,bytes);
}

/* get the next destructor if there is one.  values returned in parameters.
   call this while IN_JAVA.  first parameter is an Object[] and the second
   parameter is an int[].  both need to be of length at least one, and the
   result is placed into the first element.  this is not checked. */
static inline bool fivmr_GC_getNextDestructor(fivmr_GC *gc,
                                              fivmr_Handle *objCell,
                                              bool wait /* wait for a destructor
                                                           to become available? */) {
    return gc->getNextDestructor(gc,objCell,wait);
}

/* call only IN_NATIVE.  this does not shut down the VM; it only gets all of the
   threads to exit so that they are no longer running.  returns false if the
   VM has already exited. */
bool fivmr_VM_exit(fivmr_VM *vm,int32_t status);

void fivmr_VM_shutdown(fivmr_VM *vm,
                       int32_t *exitCode);

#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
/* load the JNI library, prepend it to the given list, and save the filename
   (using strdup, so you can free the filename you passed).  returns NULL
   on failure.  if the library was already on the list, just returns that
   library.  note that this function does not make use of any Java
   functionality, and so can run either IN_JAVA or IN_NATIVE.  as well, note
   that you must somehow synchronize calls to this function to ensure that
   the list doesn't get corrupted. */
void fivmr_JNILib_load(fivmr_ThreadState *ts,
                       fivmr_JNILib **list,
		       const char *filename,
		       fivmr_JNILib **result,
		       bool *freshlyLoaded);

/* look up the given method implementation in this JNI library only.  must
   be called while IN_JAVA! */
void *fivmr_JNILib_lookupOne(fivmr_ThreadState *ts,
                             fivmr_JNILib *lib,
			     fivmr_MethodRec *mr);

/* perform a lookup of the given method implementation in all of the JNILibs in the
   list.  must be called while IN_JAVA! */
void *fivmr_JNILib_lookup(fivmr_ThreadState *ts,
                          fivmr_JNILib *list,
			  fivmr_MethodRec *mr);
#endif

/* call while IN_JAVA */
void *fivmr_JNI_lookup(fivmr_ThreadState *ts,
                       fivmr_MethodRec *mr);

/* call while IN_NATIVE.  returns true on success and false on failure. */
bool fivmr_JNI_loadLibrary(fivmr_ThreadState *ts,
                           fivmr_TypeContext *ctx,
                           const char *filename);

bool fivmr_JNI_runOnLoad(fivmr_ThreadState *ts,
                         fivmr_TypeContext *ctx,
                         void *onLoadHook);

void fivmr_JNI_init(fivmr_TypeContext *ctx,
                    fivmr_VM *vm);
void fivmr_JNI_destroy(fivmr_TypeContext *ctx);

static inline uintptr_t *fivmr_getStringTable(fivmr_Payload *payload) {
    return payload->stringTable;
}

static inline fivmr_Object fivmr_getFirstString(fivmr_Payload *payload) {
    uintptr_t start=(uintptr_t)payload->stringTable;
    start+=FIVMR_ALLOC_OFFSET(&payload->settings);
    return start;
}

static inline uintptr_t fivmr_getStringDistance(fivmr_Payload *payload) {
    return fivmr_alignRaw(fivmr_TypeData_size(payload->td_String),
                          sizeof(uintptr_t));
}

static inline fivmr_Object fivmr_getString(fivmr_Payload *payload,
                                           int32_t index) {
    return fivmr_getFirstString(payload)+index*fivmr_getStringDistance(payload);
}

static inline fivmr_Object fivmr_getString2(fivmr_ThreadState *ts,
                                            int32_t index) {
    return fivmr_getString(ts->vm->payload,index);
}

static inline int32_t fivmr_getNumStrings(fivmr_Payload *payload) {
    return payload->nStrings;
}

static inline uintptr_t *fivmr_getClassTable(fivmr_Payload *payload) {
    return payload->classTable;
}

static inline fivmr_Object fivmr_getFirstClass(fivmr_Payload *payload) {
    uintptr_t start=(uintptr_t)payload->classTable;
    start+=FIVMR_ALLOC_OFFSET(&payload->settings);
    return start;
}

static inline uintptr_t fivmr_getClassDistance(fivmr_Payload *payload) {
    return fivmr_alignRaw(fivmr_TypeData_size(payload->td_Class),
                          sizeof(uintptr_t));
}

static inline fivmr_Object fivmr_getClass(fivmr_Payload *payload,
                                          int32_t index) {
    return fivmr_getFirstString(payload)+index*fivmr_getStringDistance(payload);
}

static inline int32_t fivmr_getNumClasses(fivmr_Payload *payload) {
    return payload->nTypes;
}

void fivmr_ThreadPool_init(fivmr_ThreadPool *pool,
                           uintptr_t nthreads,
                           fivmr_ThreadPriority defaultPriority);

fivmr_PooledThread *fivmr_ThreadPool_spawn(fivmr_ThreadPool *pool,
                                           void (*runner)(void *arg),
                                           void *arg,
                                           fivmr_ThreadPriority activePriority);

void fivmr_PooledThread_suspend(fivmr_PooledThread *pt);
void fivmr_PooledThread_resume(fivmr_PooledThread *pt);

void fivmr_ThreadPool_suspend(fivmr_ThreadPool *pool);
void fivmr_ThreadPool_resume(fivmr_ThreadPool *pool);

void fivmr_TimeSliceManager_init(fivmr_TimeSliceManager *man,
                                 uintptr_t nslices,
                                 fivmr_ThreadPriority managerPriority);

fivmr_TimeSlice *
fivmr_TimeSliceManager_initSlice(fivmr_TimeSliceManager *man,
                                 uintptr_t sliceIndex,
                                 fivmr_Nanos duration,
                                 fivmr_ThreadPool *pool);

fivmr_TimeSlice *
fivmr_TimeSliceManager_initSliceEasy(fivmr_TimeSliceManager *man,
                                     uintptr_t sliceIndex,
                                     fivmr_Nanos duration,
                                     uintptr_t nthreads,
                                     fivmr_ThreadPriority defaultPriority);

bool fivmr_TimeSliceManager_fullyInitialized(fivmr_TimeSliceManager *man);

static inline fivmr_TimeSlice *
fivmr_TimeSliceManager_getSlice(fivmr_TimeSliceManager *man,
                                uintptr_t sliceIndex) {
    fivmr_assert(sliceIndex<man->nslices);
    fivmr_assert(man->slices[sliceIndex].inited);
    return man->slices+sliceIndex;
}

static inline fivmr_ThreadPool *
fivmr_TimeSliceManager_getPool(fivmr_TimeSliceManager *man,
                               uintptr_t sliceIndex) {
    fivmr_assert(sliceIndex<man->nslices);
    fivmr_assert(man->slices[sliceIndex].inited);
    fivmr_assert(man->slices[sliceIndex].pool!=NULL);
    return man->slices[sliceIndex].pool;
}

void fivmr_TimeSliceManager_start(fivmr_TimeSliceManager *man);

void fivmr_assertNoException(fivmr_ThreadState *ts,
                             const char *context);

static inline bool fivmr_hasException(fivmr_ThreadState *ts) {
    return ts->curException || ts->curExceptionHandle;
}

static inline bool fivmr_exiting(fivmr_VM *vm) {
    return vm->exiting;
}

void fivmr_describeException(fivmr_ThreadState *ts,
                             fivmr_Handle *h);

static inline const char *fivmr_homeDir(fivmr_Payload *payload) {
    return payload->fivmcHomeDir;
}

static inline const char *fivmr_version(void) {
    return FIVMR_VERSION;
}

static inline const char *fivmr_vendor(void) {
    return FIVMR_VENDOR;
}

static inline const char *fivmr_copyright(void) {
    return FIVMR_COPYRIGHT;
}

static inline const char *fivmr_name(void) {
    return "fivm";
}

void fivmr_SPC_dump(void);
int32_t fivmr_SPC_numCounts(void);
void fivmr_SPC_getCounts(uintptr_t **buffer);
void fivmr_SPC_getNames(char const **buffer);

void fivmr_PR_dump(void);

void fivmr_initHardRTJ(fivmr_VM *vm);

/* helper: initialize log, sysdep, other stuff, but not the stuff needed
   for time/resource partitioning */
void fivmr_runBaseInit(void);

/* helper: initialize everything needed for time/resource partitioning.
   note that this also calls fivmr_runBaseInit() */
void fivmr_runBaseTRPartInit(void);

/* initialize and run the runtime assuming that log and sysdep are
   initialized, using the default entrypoint. */
void fivmr_runRuntime(fivmr_VM *vm,
                      int argc,
                      char **argv);

/* initialize and run the runtime assuming that log and sysdep are
   initialized, using a custom entrypoint. */
void fivmr_runRuntimeWithClass(fivmr_VM *vm,
                               fivmr_TypeData *mainClass,
                               int argc,
                               char **argv);

void fivmr_VM_run(fivmr_VM *vm,
                  int argc,
                  char **argv);

void fivmr_VM_runWithClass(fivmr_VM *vm,
                           fivmr_TypeData *mainClass,
                           int argc,
                           char **v);

/* stuff imported from Java */
static inline void fivmRuntime_boot(fivmr_ThreadState *ts) {
    ts->vm->payload->fivmRuntime_boot((uintptr_t)ts);
}
static inline void fivmRuntime_initSystemClassLoaders(fivmr_ThreadState *ts) {
    ts->vm->payload->fivmRuntime_initSystemClassLoaders((uintptr_t)ts);
}
static inline void fivmRuntime_notifyInitialized(fivmr_ThreadState *ts) {
    ts->vm->payload->fivmRuntime_notifyInitialized((uintptr_t)ts);
}

static inline fivmr_Object fivmr_alloc(fivmr_ThreadState *ts,
                                       fivmr_GCSpace space,
                                       fivmr_TypeData *td) {
    return ts->vm->payload->allocForNative((uintptr_t)ts,space,(uintptr_t)td);
}

static inline fivmr_Object fivmr_allocArray(fivmr_ThreadState *ts,
                                            fivmr_GCSpace space,
                                            fivmr_TypeData *td,
                                            int32_t numEle) {
    return ts->vm->payload->allocArrayForNative((uintptr_t)ts,space,(uintptr_t)td,numEle);
}

static inline void fivmr_throwInternalError(fivmr_ThreadState *ts,
                                            const char *reason) {
    ts->vm->payload->throwInternalError((uintptr_t)ts,(uintptr_t)reason);
}
static inline void fivmr_throwNoClassDefFoundError_inNative(fivmr_ThreadState *ts,
                                                            const char *className,
                                                            const char *fromWhere) {
    ts->vm->payload->throwNoClassDefFoundError_inNative((uintptr_t)ts,
                                                        (uintptr_t)className,
                                                        (uintptr_t)fromWhere);
}
static inline void fivmr_throwNoClassDefFoundError_inJava(fivmr_ThreadState *ts,
                                                          const char *className,
                                                          const char *fromWhere) {
    ts->vm->payload->throwNoClassDefFoundError_inJava((uintptr_t)ts,
                                                      (uintptr_t)className,
                                                      (uintptr_t)fromWhere);
}
static inline void fivmr_throwLinkageError_inJava(fivmr_ThreadState *ts,
                                                  const char *reason) {
    ts->vm->payload->throwLinkageError_inJava((uintptr_t)ts,
                                              (uintptr_t)reason);
}
static inline void fivmr_throwNullPointerRTE(fivmr_ThreadState *ts) {
    ts->vm->payload->throwNullPointerRTE((uintptr_t)ts);
}
static inline void fivmr_throwNullPointerRTE_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwNullPointerRTE_inJava((uintptr_t)ts);
}
static inline void fivmr_throwArithmeticRTE(fivmr_ThreadState *ts) {
    ts->vm->payload->throwArithmeticRTE((uintptr_t)ts);
}
static inline void fivmr_throwArithmeticRTE_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwArithmeticRTE_inJava((uintptr_t)ts);
}
static inline void fivmr_throwStackOverflowRTE(fivmr_ThreadState *ts) {
    ts->vm->payload->throwStackOverflowRTE((uintptr_t)ts);
}
static inline void fivmr_throwStackOverflowRTE_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwStackOverflowRTE_inJava((uintptr_t)ts);
}
static inline void fivmr_throwClassChangeRTE_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwClassChangeRTE_inJava((uintptr_t)ts);
}
static inline void fivmr_throwArrayBoundsRTE_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwArrayBoundsRTE_inJava((uintptr_t)ts);
}
static inline void fivmr_throwArrayStoreRTE_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwArrayStoreRTE_inJava((uintptr_t)ts);
}
static inline void fivmr_throwNegativeSizeRTE_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwNegativeSizeRTE_inJava((uintptr_t)ts);
}
static inline void fivmr_throwAbstractMethodError_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwAbstractMethodError_inJava((uintptr_t)ts);
}
static inline void fivmr_throwClassCastRTE_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwClassCastRTE_inJava((uintptr_t)ts);
}
static inline void fivmr_throwUnsatisfiedLinkErrorForLoad(fivmr_ThreadState *ts,
                                                          const char *filename,
                                                          const char *error) {
    ts->vm->payload->throwUnsatisfiedLinkErrorForLoad((uintptr_t)ts,
                                                      (uintptr_t)filename,
                                                      (uintptr_t)error);
}
static inline void fivmr_throwNoSuchFieldError(fivmr_ThreadState *ts,
                                               const char *name,
                                               const char *sig) {
    ts->vm->payload->throwNoSuchFieldError((uintptr_t)ts,
                                           (uintptr_t)name,
                                           (uintptr_t)sig);
}
static inline void fivmr_throwNoSuchMethodError(fivmr_ThreadState *ts,
                                                const char *name,
                                                const char *sig) {
    ts->vm->payload->throwNoSuchMethodError((uintptr_t)ts,
                                            (uintptr_t)name,
                                            (uintptr_t)sig);
}
static inline void fivmr_throwNoSuchMethodError_inJava(fivmr_ThreadState *ts,
                                                       fivmr_MethodRec *mr) {
    ts->vm->payload->throwNoSuchMethodError_inJava((uintptr_t)ts,
                                                   (uintptr_t)mr);
}
static inline void fivmr_throwExceptionInInitializerError_inJava(fivmr_ThreadState *ts,
                                                                 fivmr_Object cause,
                                                                 fivmr_TypeData *td) {
    ts->vm->payload->throwExceptionInInitializerError_inJava((uintptr_t)ts,
                                                             cause,
                                                             (uintptr_t)td);
}
static inline void fivmr_throwReflectiveException_inJava(fivmr_ThreadState *ts,
                                                         fivmr_Object cause) {
    ts->vm->payload->throwReflectiveException_inJava((uintptr_t)ts,
                                                     cause);
}
static inline void fivmr_throwIllegalMonitorStateException_inJava(fivmr_ThreadState *ts,
                                                                  const char *msg) {
    ts->vm->payload->throwIllegalMonitorStateException_inJava((uintptr_t)ts,
                                                              (uintptr_t)msg);
}
static inline void fivmr_throwOutOfMemoryError_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwOutOfMemoryError_inJava((uintptr_t)ts);
}
static inline void fivmr_throwIllegalAssignmentError(fivmr_ThreadState *ts) {
    ts->vm->payload->throwIllegalAssignmentError((uintptr_t)ts);
}
static inline void fivmr_throwIllegalAssignmentError_inJava(fivmr_ThreadState *ts) {
    ts->vm->payload->throwIllegalAssignmentError_inJava((uintptr_t)ts);
}
static inline void fivmr_describeExceptionImpl(fivmr_ThreadState *ts,
                                               fivmr_Handle *h) {
    ts->vm->payload->describeExceptionImpl((uintptr_t)ts,
                                           (uintptr_t)h);
}
static inline fivmr_Handle *fivmr_fromCString(fivmr_ThreadState *ts,
                                              const char *str) {
    return (fivmr_Handle*)ts->vm->payload->fromCStringInHeap((uintptr_t)ts,
                                                             (uintptr_t)str);
}
static inline fivmr_Handle *fivmr_fromCStringFull(fivmr_ThreadState *ts,
                                                  const char *str) {
    return (fivmr_Handle*)ts->vm->payload->fromCStringFullInHeap((uintptr_t)ts,
                                                                 (uintptr_t)str);
}
static inline fivmr_Handle *fivmr_fromUTF16Sequence(fivmr_ThreadState *ts,
                                                    const uint16_t *chars,
                                                    int32_t len) {
    return (fivmr_Handle*)ts->vm->payload->fromUTF16Sequence((uintptr_t)ts,
                                                             (uintptr_t)chars,
                                                             len);
}
static inline int32_t fivmr_stringLength(fivmr_ThreadState *ts,
                                         fivmr_Handle *h) {
    return ts->vm->payload->stringLength((uintptr_t)ts,
                                         (uintptr_t)h);
}
static inline int32_t fivmr_cstringLength(fivmr_ThreadState *ts,
                                          fivmr_Handle *h) {
    return ts->vm->payload->cstringLength((uintptr_t)ts,
                                          (uintptr_t)h);
}
static inline uint16_t *fivmr_getUTF16Sequence(fivmr_ThreadState *ts,
                                               fivmr_Handle *h) {
    return (uint16_t*)ts->vm->payload->getUTF16Sequence((uintptr_t)ts,
                                             (uintptr_t)h);
}
static inline char *fivmr_getCString(fivmr_ThreadState *ts,
                                     fivmr_Handle *h) {
    return (char*)ts->vm->payload->getCString((uintptr_t)ts,
                                              (uintptr_t)h);
}
static inline char *fivmr_getCStringFull(fivmr_ThreadState *ts,
                                         fivmr_Handle *h) {
    return (char*)ts->vm->payload->getCStringFull((uintptr_t)ts,
                                                  (uintptr_t)h);
}
static inline void fivmr_getStringRegion(fivmr_ThreadState *ts,
                                         fivmr_Handle *h,
                                         int32_t start,
                                         int32_t len,
                                         uint16_t *buf) {
    ts->vm->payload->getStringRegion((uintptr_t)ts,
                                     (uintptr_t)h,
                                     start,
                                     len,
                                     (uintptr_t)buf);
}
static inline void fivmr_getStringUTFRegion(fivmr_ThreadState *ts,
                                            fivmr_Handle *h,
                                            int32_t start,
                                            int32_t len,
                                            char *buf) {
    ts->vm->payload->getStringUTFRegion((uintptr_t)ts,
                                        (uintptr_t)h,
                                        start,
                                        len,
                                        (uintptr_t)buf);
}
static inline uint16_t *fivmr_String_getArrayPointer(fivmr_ThreadState *ts,
                                                     fivmr_Handle *h) {
    return (uint16_t*)ts->vm->payload->String_getArrayPointer((uintptr_t)ts,
                                                              (uintptr_t)h);
}
static inline int32_t fivmr_String_getOffset(fivmr_ThreadState *ts,
                                             fivmr_Handle *h) {
    return ts->vm->payload->String_getOffset((uintptr_t)ts,
                                             (uintptr_t)h);
}
static inline int8_t *fivmr_getBooleanElements(fivmr_ThreadState *ts,
                                               fivmr_Handle *h) {
    return (int8_t*)ts->vm->payload->getBooleanElements((uintptr_t)ts,
                                                        (uintptr_t)h);
}
static inline int8_t *fivmr_getByteElements(fivmr_ThreadState *ts,
                                            fivmr_Handle *h) {
    return (int8_t*)ts->vm->payload->getByteElements((uintptr_t)ts,
                                                     (uintptr_t)h);
}
static inline uint16_t *fivmr_getCharElements(fivmr_ThreadState *ts,
                                              fivmr_Handle *h) {
    return (uint16_t*)ts->vm->payload->getCharElements((uintptr_t)ts,
                                                       (uintptr_t)h);
}
static inline int16_t *fivmr_getShortElements(fivmr_ThreadState *ts,
                                              fivmr_Handle *h) {
    return (int16_t*)ts->vm->payload->getShortElements((uintptr_t)ts,
                                                       (uintptr_t)h);
}
static inline int32_t *fivmr_getIntElements(fivmr_ThreadState *ts,
                                            fivmr_Handle *h) {
    return (int32_t*)ts->vm->payload->getIntElements((uintptr_t)ts,
                                                     (uintptr_t)h);
}
static inline int64_t *fivmr_getLongElements(fivmr_ThreadState *ts,
                                             fivmr_Handle *h) {
    return (int64_t*)ts->vm->payload->getLongElements((uintptr_t)ts,
                                                      (uintptr_t)h);
}
static inline float *fivmr_getFloatElements(fivmr_ThreadState *ts,
                                            fivmr_Handle *h) {
    return (float*)ts->vm->payload->getFloatElements((uintptr_t)ts,
                                                     (uintptr_t)h);
}
static inline double *fivmr_getDoubleElements(fivmr_ThreadState *ts,
                                              fivmr_Handle *h) {
    return (double*)ts->vm->payload->getDoubleElements((uintptr_t)ts,
                                                       (uintptr_t)h);
}
static inline void fivmr_returnBooleanElements(fivmr_ThreadState *ts,
                                               fivmr_Handle *h,
                                               int8_t *buf,
                                               int32_t mode) {
    ts->vm->payload->returnBooleanElements((uintptr_t)ts,
                                           (uintptr_t)h,
                                           (uintptr_t)buf,
                                           mode);
}
static inline void fivmr_returnByteElements(fivmr_ThreadState *ts,
                                            fivmr_Handle *h,
                                            int8_t *buf,
                                            int32_t mode) {
    ts->vm->payload->returnByteElements((uintptr_t)ts,
                                        (uintptr_t)h,
                                        (uintptr_t)buf,
                                        mode);
}
static inline void fivmr_returnCharElements(fivmr_ThreadState *ts,
                                            fivmr_Handle *h,
                                            uint16_t *buf,
                                            int32_t mode) {
    ts->vm->payload->returnCharElements((uintptr_t)ts,
                                        (uintptr_t)h,
                                        (uintptr_t)buf,
                                        mode);
}
static inline void fivmr_returnShortElements(fivmr_ThreadState *ts,
                                             fivmr_Handle *h,
                                             int16_t *buf,
                                             int32_t mode) {
    ts->vm->payload->returnShortElements((uintptr_t)ts,
                                         (uintptr_t)h,
                                         (uintptr_t)buf,
                                         mode);
}
static inline void fivmr_returnIntElements(fivmr_ThreadState *ts,
                                           fivmr_Handle *h,
                                           int32_t *buf,
                                           int32_t mode) {
    ts->vm->payload->returnIntElements((uintptr_t)ts,
                                       (uintptr_t)h,
                                       (uintptr_t)buf,
                                       mode);
}
static inline void fivmr_returnLongElements(fivmr_ThreadState *ts,
                                            fivmr_Handle *h,
                                            int64_t *buf,
                                            int32_t mode) {
    ts->vm->payload->returnLongElements((uintptr_t)ts,
                                        (uintptr_t)h,
                                        (uintptr_t)buf,
                                        mode);
}
static inline void fivmr_returnFloatElements(fivmr_ThreadState *ts,
                                             fivmr_Handle *h,
                                             float *buf,
                                             int32_t mode) {
    ts->vm->payload->returnFloatElements((uintptr_t)ts,
                                         (uintptr_t)h,
                                         (uintptr_t)buf,
                                         mode);
}
static inline void fivmr_returnDoubleElements(fivmr_ThreadState *ts,
                                              fivmr_Handle *h,
                                              double *buf,
                                              int32_t mode) {
    ts->vm->payload->returnDoubleElements((uintptr_t)ts,
                                          (uintptr_t)h,
                                          (uintptr_t)buf,
                                          mode);
}
static inline void fivmr_getBooleanRegion(fivmr_ThreadState *ts,
                                          fivmr_Handle *h,
                                          int32_t start,
                                          int32_t len,
                                          int8_t *buf) {
    ts->vm->payload->getBooleanRegion((uintptr_t)ts,
                                      (uintptr_t)h,
                                      start,
                                      len,
                                      (uintptr_t)buf);
}
static inline void fivmr_getByteRegion(fivmr_ThreadState *ts,
                                       fivmr_Handle *h,
                                       int32_t start,
                                       int32_t len,
                                       int8_t *buf) {
    ts->vm->payload->getByteRegion((uintptr_t)ts,
                                   (uintptr_t)h,
                                   start,
                                   len,
                                   (uintptr_t)buf);
}
static inline void fivmr_getCharRegion(fivmr_ThreadState *ts,
                                       fivmr_Handle *h,
                                       int32_t start,
                                       int32_t len,
                                       uint16_t *buf) {
    ts->vm->payload->getCharRegion((uintptr_t)ts,
                                   (uintptr_t)h,
                                   start,
                                   len,
                                   (uintptr_t)buf);
}
static inline void fivmr_getShortRegion(fivmr_ThreadState *ts,
                                        fivmr_Handle *h,
                                        int32_t start,
                                        int32_t len,
                                        int16_t *buf) {
    ts->vm->payload->getShortRegion((uintptr_t)ts,
                                    (uintptr_t)h,
                                    start,
                                    len,
                                    (uintptr_t)buf);
}
static inline void fivmr_getIntRegion(fivmr_ThreadState *ts,
                                      fivmr_Handle *h,
                                      int32_t start,
                                      int32_t len,
                                      int32_t *buf) {
    ts->vm->payload->getIntRegion((uintptr_t)ts,
                                  (uintptr_t)h,
                                  start,
                                  len,
                                  (uintptr_t)buf);
}
static inline void fivmr_getLongRegion(fivmr_ThreadState *ts,
                                       fivmr_Handle *h,
                                       int32_t start,
                                       int32_t len,
                                       int64_t *buf) {
    ts->vm->payload->getLongRegion((uintptr_t)ts,
                                   (uintptr_t)h,
                                   start,
                                   len,
                                   (uintptr_t)buf);
}
static inline void fivmr_getFloatRegion(fivmr_ThreadState *ts,
                                        fivmr_Handle *h,
                                        int32_t start,
                                        int32_t len,
                                        float *buf) {
    ts->vm->payload->getFloatRegion((uintptr_t)ts,
                                    (uintptr_t)h,
                                    start,
                                    len,
                                    (uintptr_t)buf);
}
static inline void fivmr_getDoubleRegion(fivmr_ThreadState *ts,
                                         fivmr_Handle *h,
                                         int32_t start,
                                         int32_t len,
                                         double *buf) {
    ts->vm->payload->getDoubleRegion((uintptr_t)ts,
                                     (uintptr_t)h,
                                     start,
                                     len,
                                     (uintptr_t)buf);
}
static inline void fivmr_setBooleanRegion(fivmr_ThreadState *ts,
                                          fivmr_Handle *h,
                                          int32_t start,
                                          int32_t len,
                                          int8_t *buf) {
    ts->vm->payload->setBooleanRegion((uintptr_t)ts,
                                      (uintptr_t)h,
                                      start,
                                      len,
                                      (uintptr_t)buf);
}
static inline void fivmr_setByteRegion(fivmr_ThreadState *ts,
                                       fivmr_Handle *h,
                                       int32_t start,
                                       int32_t len,
                                       int8_t *buf) {
    ts->vm->payload->setByteRegion((uintptr_t)ts,
                                   (uintptr_t)h,
                                   start,
                                   len,
                                   (uintptr_t)buf);
}
static inline void fivmr_setCharRegion(fivmr_ThreadState *ts,
                                       fivmr_Handle *h,
                                       int32_t start,
                                       int32_t len,
                                       uint16_t *buf) {
    ts->vm->payload->setCharRegion((uintptr_t)ts,
                                   (uintptr_t)h,
                                   start,
                                   len,
                                   (uintptr_t)buf);
}
static inline void fivmr_setShortRegion(fivmr_ThreadState *ts,
                                        fivmr_Handle *h,
                                        int32_t start,
                                        int32_t len,
                                        int16_t *buf) {
    ts->vm->payload->setShortRegion((uintptr_t)ts,
                                    (uintptr_t)h,
                                    start,
                                    len,
                                    (uintptr_t)buf);
}
static inline void fivmr_setIntRegion(fivmr_ThreadState *ts,
                                      fivmr_Handle *h,
                                      int32_t start,
                                      int32_t len,
                                      int32_t *buf) {
    ts->vm->payload->setIntRegion((uintptr_t)ts,
                                  (uintptr_t)h,
                                  start,
                                  len,
                                  (uintptr_t)buf);
}
static inline void fivmr_setLongRegion(fivmr_ThreadState *ts,
                                       fivmr_Handle *h,
                                       int32_t start,
                                       int32_t len,
                                       int64_t *buf) {
    ts->vm->payload->setLongRegion((uintptr_t)ts,
                                   (uintptr_t)h,
                                   start,
                                   len,
                                   (uintptr_t)buf);
}
static inline void fivmr_setFloatRegion(fivmr_ThreadState *ts,
                                        fivmr_Handle *h,
                                        int32_t start,
                                        int32_t len,
                                        float *buf) {
    ts->vm->payload->setFloatRegion((uintptr_t)ts,
                                    (uintptr_t)h,
                                    start,
                                    len,
                                    (uintptr_t)buf);
}
static inline void fivmr_setDoubleRegion(fivmr_ThreadState *ts,
                                         fivmr_Handle *h,
                                         int32_t start,
                                         int32_t len,
                                         double *buf) {
    ts->vm->payload->setDoubleRegion((uintptr_t)ts,
                                     (uintptr_t)h,
                                     start,
                                     len,
                                     (uintptr_t)buf);
}
static inline void fivmr_returnBuffer(fivmr_ThreadState *ts,
                                      void *ptr) {
    ts->vm->payload->returnBuffer((uintptr_t)ts,
                                  (uintptr_t)ptr);
}
static inline fivmr_Handle *fivmr_DirectByteBuffer_wrap(fivmr_ThreadState *ts,
                                                        uintptr_t ptr,
                                                        int32_t capacity,
                                                        int32_t limit,
                                                        int32_t position) {
    return (fivmr_Handle*)ts->vm->payload->DirectByteBuffer_wrap((uintptr_t)ts,
                                                                 ptr,
                                                                 capacity,
                                                                 limit,
                                                                 position);
}
static inline uintptr_t fivmr_DirectByteBuffer_address(fivmr_ThreadState *ts,
                                                       fivmr_Handle *h) {
    return ts->vm->payload->DirectByteBuffer_address((uintptr_t)ts,
                                                     (uintptr_t)h);
}
static inline int32_t fivmr_DirectByteBuffer_capacity(fivmr_ThreadState *ts,
                                                      fivmr_Handle *h) {
    return ts->vm->payload->DirectByteBuffer_capacity((uintptr_t)ts,
                                                      (uintptr_t)h);
}
static inline fivmr_Handle *fivmr_VMThread_create(fivmr_ThreadState *ts,
                                                  int32_t priority,
                                                  bool daemon) {
    return (fivmr_Handle*)ts->vm->payload->VMThread_create((uintptr_t)ts,
                                                           priority,
                                                           daemon);
}

static inline fivmr_Handle *fivmr_VMThread_createRT(fivmr_ThreadState *ts,
                                                  int32_t priority,
                                                  bool daemon) {
    return (fivmr_Handle*)ts->vm->payload->VMThread_createRT((uintptr_t)ts,
                                                           priority,
                                                           daemon);
}


static inline void fivmr_VMThread_setThreadState(fivmr_ThreadState *callerTS,
                                                 fivmr_Handle *vmt,
                                                 fivmr_ThreadState *tsToSet) {
    callerTS->vm->payload->VMThread_setThreadState((uintptr_t)callerTS,
                                                   (uintptr_t)vmt,
                                                   (uintptr_t)tsToSet);
}
static inline fivmr_ThreadState *fivmr_VMThread_getThreadState(fivmr_ThreadState *ts,
                                                               fivmr_Handle *vmt) {
    return (fivmr_ThreadState*)ts->vm->payload->VMThread_getThreadState((uintptr_t)ts,
                                                                        (uintptr_t)vmt);
}
static inline void fivmr_VMThread_starting(fivmr_ThreadState *ts,
                                           fivmr_Handle *vmt) {
    ts->vm->payload->VMThread_starting((uintptr_t)ts,
                                       (uintptr_t)vmt);
}
static inline void fivmr_VMThread_run(fivmr_ThreadState *ts,
                                      fivmr_Handle *vmt) {
    ts->vm->payload->VMThread_run((uintptr_t)ts,
                                  (uintptr_t)vmt);
}
static inline bool fivmr_VMThread_setUncaughtException(fivmr_ThreadState *ts,
                                                       fivmr_Handle *vmt,
                                                       fivmr_Handle *exception) {
    return ts->vm->payload->VMThread_setUncaughtException((uintptr_t)ts,
                                                          (uintptr_t)vmt,
                                                          (uintptr_t)exception);
}
static inline void fivmr_VMThread_die(fivmr_ThreadState *ts,
                                      fivmr_Handle *vmt) {
    ts->vm->payload->VMThread_die((uintptr_t)ts,
                                  (uintptr_t)vmt);
}
static inline bool fivmr_VMThread_isDaemon(fivmr_ThreadState *ts,
                                           fivmr_Handle *vmt) {
    return ts->vm->payload->VMThread_isDaemon((uintptr_t)ts,
                                              (uintptr_t)vmt);
}
static inline int32_t fivmr_VMThread_getPriority(fivmr_ThreadState *ts,
                                                 fivmr_Handle *vmt) {
    return ts->vm->payload->VMThread_getPriority((uintptr_t)ts,
                                                 (uintptr_t)vmt);
}
static inline uintptr_t fivmr_DumpStackCback_cback(fivmr_ThreadState *ts,
                                                   fivmr_Object cback,
                                                   fivmr_MethodRec *mr,
                                                   int32_t lineNumber) {
    return ts->vm->payload->DumpStackCback_cback((uintptr_t)ts,
                                                 cback,
                                                 (uintptr_t)mr,
                                                 lineNumber);
}
static inline void fivmr_makeJNIFuncName(fivmr_ThreadState *ts,
                                         char *cstr,
                                         int32_t len,
                                         fivmr_MethodRec *mr,
                                         bool longForm) { /* call only IN_JAVA */
    ts->vm->payload->makeJNIFuncName((uintptr_t)ts,
                                     (uintptr_t)cstr,
                                     len,
                                     (uintptr_t)mr,
                                     longForm);
}
static inline void fivmr_runRunnable(fivmr_ThreadState *ts,
                                     fivmr_Handle *h) { /* call only IN_NATIVE */
    ts->vm->payload->runRunnable((uintptr_t)ts,
                                 (uintptr_t)h);
}

static inline fivmr_Object fivmRuntime_loadClass(fivmr_ThreadState *ts,
                                                 fivmr_TypeContext *ctx,
                                                 fivmr_Object loader,
                                                 const char *name) {
    return ts->vm->payload->fivmRuntime_loadClass((uintptr_t)ts,
                                                  (uintptr_t)ctx,
                                                  (uintptr_t)loader,
                                                  (uintptr_t)name);
}

static inline void fivmr_handlePatchPointImpl(fivmr_ThreadState *ts,
                                              uintptr_t debugID,
                                              const char *className,
                                              const char *fromWhereDescr,
                                              int bcOffset,
                                              void **patchThunkPtrPtr,
                                              void *origPatchThunk) {
    ts->vm->payload->handlePatchPointImpl((uintptr_t)ts,
                                          debugID,
                                          (uintptr_t)className,
                                          (uintptr_t)fromWhereDescr,
                                          bcOffset,
                                          (uintptr_t)patchThunkPtrPtr,
                                          (uintptr_t)origPatchThunk);
}

static inline void fivmr_handleLoadThunk(fivmr_ThreadState *ts,
                                         fivmr_MethodRec *mr) {
    ts->vm->payload->handleLoadThunk((uintptr_t)ts,
                                     (uintptr_t)mr);
}

static inline void fivmr_allocateClass(fivmr_ThreadState *ts,
                                       fivmr_TypeData *td) {
    ts->vm->payload->allocateClass((uintptr_t)ts,(uintptr_t)td);
}

static inline void fivmr_handleFieldResolution(fivmr_ThreadState *ts,
                                               uintptr_t returnAddr,
                                               fivmr_BaseFieldAccess *bfa) {
    ts->vm->payload->handleFieldResolution((uintptr_t)ts,returnAddr,(uintptr_t)bfa);
}

static inline void fivmr_handleMethodResolution(fivmr_ThreadState *ts,
                                                uintptr_t debugID,
                                                uintptr_t returnAddr,
                                                fivmr_BaseMethodCall *bmc) {
    ts->vm->payload->handleMethodResolution((uintptr_t)ts,debugID,returnAddr,(uintptr_t)bmc);
}

static inline void fivmr_handleArrayAlloc(fivmr_ThreadState *ts,
                                          uintptr_t returnAddr,
                                          fivmr_BaseArrayAlloc *baa) {
    ts->vm->payload->handleArrayAlloc((uintptr_t)ts,returnAddr,(uintptr_t)baa);
}

static inline void fivmr_handleObjectAlloc(fivmr_ThreadState *ts,
                                           uintptr_t returnAddr,
                                           fivmr_BaseObjectAlloc *boa) {
    ts->vm->payload->handleObjectAlloc((uintptr_t)ts,returnAddr,(uintptr_t)boa);
}

static inline void fivmr_handleInstanceof(fivmr_ThreadState *ts,
                                          uintptr_t returnAddr,
                                          fivmr_BaseInstanceof *bio) {
    ts->vm->payload->handleInstanceof((uintptr_t)ts,returnAddr,(uintptr_t)bio);
}

static inline fivmr_Handle *fivmr_processArgs(fivmr_ThreadState *ts,
                                              int32_t argc,
                                              char **argv) { /* call only IN_NATIVE */
    return (fivmr_Handle*)ts->vm->payload->processArgs((uintptr_t)ts,
                                                       argc,
                                                       (uintptr_t)argv);
}

static inline void fivmr_javaExit(fivmr_ThreadState *ts,
                                  int32_t status) {
    ts->vm->payload->javaExit((uintptr_t)ts,
                              status);
}

static inline int32_t fivmr_numInternalVMThreads(void) {
    int32_t result=0;
#if FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT
    result++;
#endif
#if FIVMR_PF_POLLCHECK
    result++;
#endif
    return result;
}

/* arithmetic helpers - note not all of them are compiled in, depending on configuration. */
int32_t fivmr_AH_int_add(int32_t a,int32_t b);
int32_t fivmr_AH_int_sub(int32_t a,int32_t b);
int32_t fivmr_AH_int_mul(int32_t a,int32_t b);
int32_t fivmr_AH_int_div(int32_t a,int32_t b);
int32_t fivmr_AH_int_mod(int32_t a,int32_t b);
int32_t fivmr_AH_int_neg(int32_t a);
int32_t fivmr_AH_int_shl(int32_t a,int32_t b);
int32_t fivmr_AH_int_shr(int32_t a,int32_t b);
int32_t fivmr_AH_int_ushr(int32_t a,int32_t b);
int32_t fivmr_AH_int_and(int32_t a,int32_t b);
int32_t fivmr_AH_int_or(int32_t a,int32_t b);
int32_t fivmr_AH_int_xor(int32_t a,int32_t b);
int32_t fivmr_AH_int_compareG(int32_t a,int32_t b);
int32_t fivmr_AH_int_compareL(int32_t a,int32_t b);
int32_t fivmr_AH_int_lessThan(int32_t a,int32_t b);
int32_t fivmr_AH_int_uLessThan(int32_t a,int32_t b);
int32_t fivmr_AH_int_eq(int32_t a,int32_t b);
int32_t fivmr_AH_int_not(int32_t a);
int32_t fivmr_AH_int_bitNot(int32_t a);
int64_t fivmr_AH_long_add(int64_t a,int64_t b);
int64_t fivmr_AH_long_sub(int64_t a,int64_t b);
int64_t fivmr_AH_long_mul(int64_t a,int64_t b);
int64_t fivmr_AH_long_div(int64_t a,int64_t b);
int64_t fivmr_AH_long_mod(int64_t a,int64_t b);
int64_t fivmr_AH_long_neg(int64_t a);
int64_t fivmr_AH_long_shl(int64_t a,int32_t b);
int64_t fivmr_AH_long_shr(int64_t a,int32_t b);
int64_t fivmr_AH_long_ushr(int64_t a,int32_t b);
int64_t fivmr_AH_long_and(int64_t a,int64_t b);
int64_t fivmr_AH_long_or(int64_t a,int64_t b);
int64_t fivmr_AH_long_xor(int64_t a,int64_t b);
int32_t fivmr_AH_long_compareG(int64_t a,int64_t b);
int32_t fivmr_AH_long_compareL(int64_t a,int64_t b);
int32_t fivmr_AH_long_lessThan(int64_t a,int64_t b);
int32_t fivmr_AH_long_uLessThan(int64_t a,int64_t b);
int32_t fivmr_AH_long_eq(int64_t a,int64_t b);
int64_t fivmr_AH_long_not(int64_t a);
int64_t fivmr_AH_long_bitNot(int64_t a);
float fivmr_AH_float_add(float a,float b);
float fivmr_AH_float_sub(float a,float b);
float fivmr_AH_float_mul(float a,float b);
float fivmr_AH_float_div(float a,float b);
float fivmr_AH_float_mod(float a,float b);
float fivmr_AH_float_neg(float a);
int32_t fivmr_AH_float_compareG(float a,float b);
int32_t fivmr_AH_float_compareL(float a,float b);
int32_t fivmr_AH_float_lessThan(float a,float b);
int32_t fivmr_AH_float_eq(float a,float b);
double fivmr_AH_double_add(double a,double b);
double fivmr_AH_double_sub(double a,double b);
double fivmr_AH_double_mul(double a,double b);
double fivmr_AH_double_div(double a,double b);
double fivmr_AH_double_mod(double a,double b);
double fivmr_AH_double_neg(double a);
int32_t fivmr_AH_double_compareG(double a,double b);
int32_t fivmr_AH_double_compareL(double a,double b);
int32_t fivmr_AH_double_lessThan(double a,double b);
int32_t fivmr_AH_double_eq(double a,double b);
float fivmr_AH_int_to_float(int32_t a);
int32_t fivmr_AH_float_to_int(float a);
double fivmr_AH_int_to_double(int32_t a);
int32_t fivmr_AH_double_to_int(double a);
float fivmr_AH_long_to_float(int64_t a);
int64_t fivmr_AH_float_to_long(float a);
double fivmr_AH_long_to_double(int64_t a);
int64_t fivmr_AH_double_to_long(double a);

#if FIVMR_INTERNAL_INST
void fivmr_ii_start(void);
void fivmr_ii_end(void);
void fivmr_ii_startThread(fivmr_ThreadState *ts);
void fivmr_ii_commitThread(fivmr_ThreadState *ts);

FIVMR_II_FUNCTION_DECLS
#endif

#endif

