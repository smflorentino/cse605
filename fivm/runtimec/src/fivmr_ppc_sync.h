/*
 * fivmr_ppc_sync.h
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
 * Synchronization support for PowerPC and compatible architectures.
 */

#if !defined(FP_FIVMR_SYSDEP_H)
#error "This file should only be included by fivmr_sysdep.h"
#endif

#if FIVMSYS_PTRSIZE!=4
#  error "PPC is currently only supported in 32-bit mode"
#endif

/* FIXME... */

static inline bool fivmr_cas32_weak(int32_t *ptr,
				    int32_t comparand,
				    int32_t newValue) {
    /* FIXME: this could be optimized! */
    int32_t tmp;
    int32_t result=0;
    __asm__ __volatile__ (
	"lwarx %3,0,%0\n"
	"cmpw %3,%1\n"
	"bne- 0f\n"
	"stwcx. %2,0,%0\n"
	"bne- 0f\n"
	"li %4,1\n"
	"0:\n"
	: "+r"(ptr),
	  "+r"(comparand),
	  "+r"(newValue),
	  "=r"(tmp),
	  "+r"(result)
	:
	: "memory", "cr0"
	);
    return result;
}

static inline bool fivmr_cas32(int32_t *ptr,
			       int32_t comparand,
			       int32_t newValue) {
    /* FIXME: this could be optimized! */
    int32_t tmp;
    int32_t result=0;
    __asm__ __volatile__ (
	"1:\n"
	"lwarx %3,0,%0\n"
	"cmpw %3,%1\n"
	"bne- 0f\n"
	"stwcx. %2,0,%0\n"
	"bne- 1b\n"
	"li %4,1\n"
	"0:\n"
	: "+r"(ptr),
	  "+r"(comparand),
	  "+r"(newValue),
	  "=r"(tmp),
	  "+r"(result)
	:
	: "memory", "cr0"
	);
    return !!result;
}

static inline void fivmr_cas32_void(int32_t *ptr,
				    int32_t comparand,
				    int32_t newValue) {
    int32_t tmp;
    __asm__ __volatile__ (
	"1:\n"
	"lwarx %3,0,%0\n"
	"cmpw %3,%1\n"
	"bne- 0f\n"
	"stwcx. %2,0,%0\n"
	"bne- 1b\n"
	"0:\n"
	: "+r"(ptr),
	  "+r"(comparand),
	  "+r"(newValue),
	  "=r"(tmp)
	:
	: "memory", "cr0"
	);
}

static inline int32_t fivmr_cas32_load(int32_t *ptr,
				       int32_t comparand,
				       int32_t newValue) {
    int32_t tmp;
    __asm__ __volatile__ (
	"1:\n"
	"lwarx %3,0,%0\n"
	"cmpw %3,%1\n"
	"bne- 0f\n"
	"stwcx. %2,0,%0\n"
	"bne- 1b\n"
	"0:\n"
	: "+r"(ptr),
	  "+r"(comparand),
	  "+r"(newValue),
	  "=r"(tmp)
	:
	: "memory", "cr0"
	);
    return tmp;
}

static inline int32_t fivmr_xchg_add32(int32_t *ptr,
                                       int32_t delta) {
    /* FIXME: this could be optimized! */
    for (;;) {
	int32_t oldValue = *ptr;
	if (fivmr_cas32_weak(ptr,oldValue,oldValue+delta)) {
	    return oldValue;
	}
    }
}

static inline void fivmr_cas_void(uintptr_t *ptr,
				  uintptr_t comparand,
				  uintptr_t newValue) {
    fivmr_cas32_void((int32_t*)ptr,
		     (int32_t)comparand,
		     (int32_t)newValue);
}

static inline uintptr_t fivmr_cas_load(uintptr_t *ptr,
				       uintptr_t comparand,
				       uintptr_t newValue) {
    return (uintptr_t)fivmr_cas32_load((int32_t*)ptr,
				       (int32_t)comparand,
				       (int32_t)newValue);
}

static inline bool fivmr_cas(uintptr_t *ptr,
			     uintptr_t comparand,
			     uintptr_t newValue) {
    return fivmr_cas32((int32_t*)ptr,
		       (int32_t)comparand,
		       (int32_t)newValue);
}

static inline bool fivmr_cas_weak(uintptr_t *ptr,
				  uintptr_t comparand,
				  uintptr_t newValue) {
    return fivmr_cas32_weak((int32_t*)ptr,
			    (int32_t)comparand,
			    (int32_t)newValue);
}

static inline uintptr_t fivmr_xchg_add(uintptr_t *ptr,
				       uintptr_t delta) {
    return (uintptr_t)fivmr_xchg_add32((int32_t*)ptr,
				       (int32_t)delta);
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
    __asm__ __volatile__ ("sync\n" : : : "memory");
#endif
}

/* PPC does NOT have DCAS */
/* #define FIVMR_HAVE_DCAS */


