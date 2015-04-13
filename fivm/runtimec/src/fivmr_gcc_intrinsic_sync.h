/*
 * fivmr_gcc_intrinsic_sync.h
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

/*
 * Synchronization support using GCC's intrinsics.  We currently only use this
 * on x86, because the API is not clearly specified for other architectures.
 * In particular, it's completely unclear if __sync_bool_compare_and_swap is
 * a strong CAS or a weak CAS.  Bummer.
 */

#if !defined(FP_FIVMR_SYSDEP_H)
#error "This file should only be included by fivmr_sysdep.h"
#endif

static inline void fivmr_cas_void(uintptr_t *ptr,
				  uintptr_t comparand,
				  uintptr_t newValue) {
    __sync_bool_compare_and_swap(ptr,comparand,newValue);
}

static inline uintptr_t fivmr_cas_load(uintptr_t *ptr,
				       uintptr_t comparand,
				       uintptr_t newValue) {
    return __sync_val_compare_and_swap(ptr,comparand,newValue);
}

static inline bool fivmr_cas(uintptr_t *ptr,
			     uintptr_t comparand,
			     uintptr_t newValue) {
    return __sync_bool_compare_and_swap(ptr,comparand,newValue);
}

static inline bool fivmr_cas_weak(uintptr_t *ptr,
				  uintptr_t comparand,
				  uintptr_t newValue) {
    return __sync_bool_compare_and_swap(ptr,comparand,newValue);
}

static inline uintptr_t fivmr_xchg_add(uintptr_t *ptr,
				       uintptr_t delta) {
    return __sync_fetch_and_add(ptr,delta);
}

static inline void fivmr_cas32_void(int32_t *ptr,
				    int32_t comparand,
				    int32_t newValue) {
    __sync_bool_compare_and_swap(ptr,comparand,newValue);
}

static inline int32_t fivmr_cas32_load(int32_t *ptr,
				       int32_t comparand,
				       int32_t newValue) {
    return __sync_val_compare_and_swap(ptr,comparand,newValue);
}

static inline bool fivmr_cas32(int32_t *ptr,
			       int32_t comparand,
			       int32_t newValue) {
    return __sync_bool_compare_and_swap(ptr,comparand,newValue);
}

static inline bool fivmr_cas32_weak(int32_t *ptr,
				    int32_t comparand,
				    int32_t newValue) {
    return __sync_bool_compare_and_swap(ptr,comparand,newValue);
}

static inline int32_t fivmr_xchg_add32(int32_t *ptr,
				       int32_t delta) {
    return __sync_fetch_and_add(ptr,delta);
}

/* fences */

static inline void fivmr_compilerFence(void) {
    /* the __volatile__ indicates that we don't want reorderings around this
       line, and "memory" indicates that we're clobbering memory. */
    __asm__ __volatile__ ("# compiler fence\n" : : : "memory");
}

static inline void fivmr_fence(void) {
#if FIVMR_UNIPROCESSOR
    fivmr_compilerFence();
#else
    __sync_synchronize();
#endif
}

/* do not assume that we have DCAS - FIXME */
/* #define FIVMR_HAVE_DCAS */

