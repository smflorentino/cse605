/*
 * fivmr_x86_sync.h
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
 * Synchronization support for i386 and compatible architectures, including
 * Intel64 and AMD64.
 */

#if !defined(FP_FIVMR_SYSDEP_H)
#error "This file should only be included by fivmr_sysdep.h"
#endif

/* CAS instructions.  This currently only supports x86 and x64.  Note that because
   we may support architectures that do not have DCAS, its use is greatly discouraged.
   Note furthermore that because we may support architectures where a strong
   CAS is slow, its use is slightly discouraged; basically, if an algorithm uses
   CAS in a fashion that allows weak CAS to be used, use the weak one.  If you need
   the strong one, just beware that the code will likely run a tad bit slower on
   non-Intel architectures. */

#if FIVMR_UNIPROCESSOR
#define FIVMR_LOCK ""
#else
#define FIVMR_LOCK "lock; "
#endif

struct FIVMRField_s;
typedef struct FIVMRField_s FIVMRField;
struct FIVMRField_s {
    uintptr_t a,b;
};

static inline void fivmr_cas_void(uintptr_t *ptr,
				  uintptr_t comparand,
				  uintptr_t newValue) {
    __asm__ __volatile__ (
	FIVMR_LOCK "cmpxchg %2, %1\n"
	: "+a"(comparand),
	  "+m"(*ptr)
	: "r"(newValue)
	: "memory", "cc"
	);
}

static inline uintptr_t fivmr_cas_load(uintptr_t *ptr,
				       uintptr_t comparand,
				       uintptr_t newValue) {
    __asm__ __volatile__ (
	FIVMR_LOCK "cmpxchg %2, %1\n"
	: "+a"(comparand),
	  "+m"(*ptr)
	: "r"(newValue)
	: "memory", "cc"
	);
    return comparand;
}

static inline bool fivmr_cas(uintptr_t *ptr,
			     uintptr_t comparand,
			     uintptr_t newValue) {
    if (true) {
	char result;
	__asm__ __volatile__ (
	    FIVMR_LOCK "cmpxchg %3, %2\n"
	    "sete %b0\n"
	    : "=q"(result),
	      "+a"(comparand),
	      "+m"(*ptr)
	    : "r"(newValue)
	    : "memory", "cc"
	    );
	return result;
    } else {
	/* NOTE: this version is sometimes faster. */
	return fivmr_cas_load(ptr,comparand,newValue)==comparand;
    }
}

static inline bool fivmr_cas_weak(uintptr_t *ptr,
				  uintptr_t comparand,
				  uintptr_t newValue) {
    return fivmr_cas(ptr,comparand,newValue);
}

static inline uintptr_t fivmr_xchg_add(uintptr_t *ptr,
				       uintptr_t delta) {
#if FIVMR_MY_XCHG_ADD
    for (;;) {
	uintptr_t oldValue = *ptr;
	if (fivmr_cas_weak(ptr,oldValue,oldValue+delta)) {
	    return oldValue;
	}
    }
#else
    uintptr_t tmp=delta;
    __asm__ __volatile__ (
	FIVMR_LOCK "xadd %0, %1\n"
	: "+r"(tmp),
	  "+m"(*ptr)
	:
	: "memory", "cc"
	);
    return tmp;
#endif
}

/* FIXME: AMD64 does not have 128-bit DCAS */

static inline bool fivmr_dcas(void *ptr,
			      uintptr_t comparand_a,
			      uintptr_t comparand_b,
			      uintptr_t value_a,
			      uintptr_t value_b) {
#if FIVMSYS_PTRSIZE==4
    uintptr_t tmp_a=comparand_a;
    uintptr_t tmp_b=comparand_b;
    __asm__ __volatile__ (
	"pushl %%ebx\n"
	"movl %3, %%ebx\n"
	FIVMR_LOCK "cmpxchg8b %2\n"
	"popl %%ebx\n"
	: "+a"(tmp_a),
	  "+d"(tmp_b),
	  "+m"(*(FIVMRField*)ptr)
	: "m"(value_a),
	  "c"(value_b)
	: "memory", "cc"
	);
    return tmp_a==comparand_a
	&& tmp_b==comparand_b;
#else
    char result;
    __asm__ __volatile__ (
	FIVMR_LOCK "cmpxchg16b %3\n"
	"sete %b0\n"
	: "=q"(result),
	  "+a"(comparand_a),
	  "+d"(comparand_b),
	  "+m"(*(FIVMRField*)ptr)
	: "b"(value_a),
	  "c"(value_b)
	: "memory", "cc"
	);
    return result;
#endif
}

static inline bool fivmr_dcas2(void *ptr,
			       FIVMRField comparand,
			       FIVMRField value) {
    return fivmr_dcas(ptr,
		      comparand.a,comparand.b,
		      value.a,value.b);
}

static inline bool fivmr_dcas3(void *ptr,
			       void *comparand,
			       void *value) {
    return fivmr_dcas(ptr,
		      ((FIVMRField*)comparand)->a,((FIVMRField*)comparand)->b,
		      ((FIVMRField*)value)->a,((FIVMRField*)value)->b);
}

static inline bool fivmr_dcas_weak(void *ptr,
				   uintptr_t comparand_a,
				   uintptr_t comparand_b,
				   uintptr_t value_a,
				   uintptr_t value_b) {
    return fivmr_dcas(ptr,comparand_a,comparand_b,value_a,value_b);
}

static inline bool fivmr_dcas_weak2(void *ptr,
				    FIVMRField comparand,
				    FIVMRField value) {
    return fivmr_dcas2(ptr,comparand,value);
}

static inline bool fivmr_dcas_weak3(void *ptr,
				    void *comparand,
				    void *value) {
    return fivmr_dcas3(ptr,comparand,value);
}

static inline void fivmr_dcas_void(void *ptr,
				   uintptr_t comparand_a,
				   uintptr_t comparand_b,
				   uintptr_t value_a,
				   uintptr_t value_b) {
#if FIVMSYS_PTRSIZE==4
    __asm__ __volatile__ (
	"pushl %%ebx\n"
	"movl %3, %%ebx\n"
	FIVMR_LOCK "cmpxchg8b (%%esi)\n"
	"popl %%ebx\n"
	: "+a"(comparand_a),
	  "+d"(comparand_b),
	  "+S"((FIVMRField*)ptr)
	: "m"(value_a),
	  "c"(value_b)
	: "memory", "cc"
	);
#else
    __asm__ __volatile__ (
	FIVMR_LOCK "cmpxchg16b %2\n"
	: "+a"(comparand_a),
	  "+d"(comparand_b),
	  "+m"(*(FIVMRField*)ptr)
	: "b"(value_a),
	  "c"(value_b)
	: "memory", "cc"
	);
#endif
}

static inline void fivmr_dcas_void2(void *ptr,
				    FIVMRField comparand,
				    FIVMRField value) {
    fivmr_dcas_void(ptr,
		    comparand.a,comparand.b,
		    value.a,value.b);
}

static inline bool fivmr_dcas_dload(void *targ,
				    uintptr_t comparand_a,
				    uintptr_t comparand_b,
				    uintptr_t value_a,
				    uintptr_t value_b,
				    uintptr_t *result_a,
				    uintptr_t *result_b) {
#if FIVMSYS_PTRSIZE==4
    uintptr_t tmp_a=comparand_a;
    uintptr_t tmp_b=comparand_b;
    __asm__ __volatile__ (
	"pushl %%ebx\n"
	"movl %3, %%ebx\n"
	FIVMR_LOCK "cmpxchg8b (%%esi)\n"
	"popl %%ebx\n"
	: "+a"(tmp_a),
	  "+d"(tmp_b),
	  "+S"((FIVMRField*)targ)
	: "m"(value_a),
	  "c"(value_b)
	: "memory", "cc"
	);
    *result_a=tmp_a;
    *result_b=tmp_b;
    return tmp_a==comparand_a
	&& tmp_b==comparand_b;
#else
    char result;
    __asm__ __volatile__ (
	FIVMR_LOCK "cmpxchg16b %3\n"
	"sete %b0\n"
	: "=q"(result),
	  "+a"(comparand_a),
	  "+d"(comparand_b),
	  "+m"(*(FIVMRField*)targ)
	: "b"(value_a),
	  "c"(value_b)
	: "memory", "cc"
	);
    *result_a=comparand_a;
    *result_b=comparand_b;
    return result;
#endif
}

static inline bool fivmr_dcas_dload2(void *ptr,
				     FIVMRField comparand,
				     FIVMRField value,
				     FIVMRField *result) {
    return fivmr_dcas_dload(ptr,
			    comparand.a,comparand.b,
			    value.a,value.b,
			    &(result->a),&(result->b));
}

static inline void fivmr_dload(void *ptr,
			       uintptr_t *result_a,
			       uintptr_t *result_b) {
    /* HACK: cmpxchg16b places the result in rax:rdx, with the new value
       coming from rbx:rcx.  we want this op to be as light-weight as
       possible, when it comes to clobbering registers.  and, all we have
       to do is CAS so that newValue = comparand.  so, we move whatever
       values are in rbx:rcx into rax:rdx ... ensuring that we don't
       clobber rbx:rcx, and that newValue = comparand. */
#if FIVMSYS_PTRSIZE==4
    __asm__ __volatile__ (
	"pushl %%ebx\n"
	"movl %%eax, %%ebx\n"
	FIVMR_LOCK "cmpxchg8b (%%esi)\n"
	"popl %%ebx\n"
	: "+S"((FIVMRField*)ptr),
	  "+a"(*result_a),
	  "+d"(*result_b)
	: "c"(*result_b)
	: "memory", "cc"
	);
#else
    __asm__ __volatile__ (
	FIVMR_LOCK "cmpxchg16b %0\n"
	: "+m"(*(FIVMRField*)ptr),
	  "+a"(*result_a),
	  "+d"(*result_b)
	: "b"(*result_a),
	  "c"(*result_b)
	: "memory", "cc"
	);
#endif
}

static inline void fivmr_dload2(void *ptr,
				FIVMRField *result) {
    fivmr_dload(ptr,&(result->a),&(result->b));
}

static inline void fivmr_cas32_void(int32_t *ptr,
				    int32_t comparand,
				    int32_t newValue) {
    __asm__ __volatile__ (
	FIVMR_LOCK "cmpxchg %2, %1\n"
	: "+a"(comparand),
	  "+m"(*ptr)
	: "r"(newValue)
	: "memory", "cc"
	);
}

static inline int32_t fivmr_cas32_load(int32_t *ptr,
				       int32_t comparand,
				       int32_t newValue) {
    __asm__ __volatile__ (
	FIVMR_LOCK "cmpxchg %2, %1\n"
	: "+a"(comparand),
	  "+m"(*ptr)
	: "r"(newValue)
	: "memory", "cc"
	);
    return comparand;
}

static inline bool fivmr_cas32(int32_t *ptr,
			       int32_t comparand,
			       int32_t newValue) {
    if (true) {
	char result;
	__asm__ __volatile__ (
	    FIVMR_LOCK "cmpxchg %3, %2\n"
	    "sete %b0\n"
	    : "=q"(result),
	      "+a"(comparand),
	      "+m"(*ptr)
	    : "r"(newValue)
	    : "memory", "cc"
	    );
	return result;
    } else {
	/* NOTE: this version is sometimes faster. */
	return fivmr_cas32_load(ptr,comparand,newValue)==comparand;
    }
}

static inline bool fivmr_cas32_weak(int32_t *ptr,
				    int32_t comparand,
				    int32_t newValue) {
    return fivmr_cas32(ptr,comparand,newValue);
}

static inline int32_t fivmr_xchg_add32(int32_t *ptr,
				       int32_t delta) {
#if FIVMR_MY_XCHG_ADD
    for (;;) {
	int32_t_t oldValue = *ptr;
	if (fivmr_cas32_weak(ptr,oldValue,oldValue+delta)) {
	    return oldValue;
	}
    }
#else
    int32_t tmp=delta;
    __asm__ __volatile__ (
	FIVMR_LOCK "xadd %0, %1\n"
	: "+r"(tmp),
	  "+m"(*ptr)
	:
	: "memory", "cc"
	);
    return tmp;
#endif
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
    /* FIXME: On some CPUs there may be faster options */
    __asm__ __volatile__ ("mfence" : : : "memory");
#endif
}

/* some constants */

#define FIVMR_HAVE_DCAS


