/*
 * fivmr_posix_allocexec.c
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

/* derived from the bootMalloc in pizMalloc, by Filip Pizlo
   This is an extremely simple (and not particularly efficient!) first-fit
   allocator, originally used as a bootstrap allocator in my pizMalloc
   (which itself used BBoP segregated free lists and was very nice).  I've
   adapted it here as a very trivial and easy-to-validate allocator that
   has one noteworthy feature: memory returned by it can be used by a JIT,
   since the PROT_EXEC permission is set.

   It is possible, though unlikely, that we would have to replace this
   allocator at some point. But I doubt it.  It will perform very well if
   there is very little freeing activity, while still performing respectably
   if frees do occur.  That is what you'd expect, in the one scenario where
   this allocator is used: namely, malloc upon JITing and free on either
   code replacement or class unloading.  Thus free will be rare and clustured,
   a scenario in which this code will not suck too badly. */

bool fivmr_POSIX_logAllocExec;

#define ALIGNMENT 8
#define BOOT_MIN 16
#define HANDLE_ZERO 1
#define GRANULARITY (FIVMR_PAGE_SIZE*8) /* must be power of two! */

#define P(x) ((void*)(x))
#define S(x) ((char*)(x))
#define nop() do {} while(false)

static uintptr_t total;

static void *pizPageAllocP(size_t *needed) {
    void *result;
    /* printf("needed = %p, granularity = %p\n",(void*)*needed,(void*)GRANULARITY); */
    *needed=((*needed)+GRANULARITY-1)&~(GRANULARITY-1);
    if (fivmr_POSIX_logAllocExec) {
        fivmr_Log_printf("[AE: requesting %" PRIdPTR " bytes from OS (%" PRIdPTR " total)]\n",
                         *needed,total+*needed);
    }
    result=mmap(NULL,*needed,
		PROT_READ|PROT_WRITE|PROT_EXEC,
		MAP_ANON|MAP_PRIVATE,
		-1,0);
    if (result==(void*)-1) {
	fivmr_abortf("mmap in pizPageAllocP");
    }
    total+=*needed;
    return result;
}

static inline size_t *bootSize(void *ptr) {
    return (size_t*)ptr;
}

static inline void **bootNext(void *ptr) {
    return ((void**)ptr)+1;
}

#define bootHdr(x) (P(S(x)-sizeof(void*)))
#define bootObj(x) (P(S(x)+sizeof(void*)))

/* sorted singly-linked freelist */
static void *g_bootHead=NULL;
static fivmr_Lock g_lock;

void fivmr_POSIX_execAllocInit(void) {
    fivmr_Lock_init(&g_lock,FIVMR_PR_CRITICAL);
}

static void bootFree(void *ptr) {
    void *cur;
    ptr=bootHdr(ptr);
    if (S(ptr)+*bootSize(ptr)==g_bootHead) {
	/* coalesce with head (coalesce right) */
	*bootSize(ptr)+=*bootSize(g_bootHead);
	*bootNext(ptr)=*bootNext(g_bootHead);
	g_bootHead=ptr;
    } else if (ptr<g_bootHead || g_bootHead==NULL) {
	/* insert as new head */
	*bootNext(ptr)=g_bootHead;
	g_bootHead=ptr;
    } else {
	cur=g_bootHead;
	while (cur!=NULL) {
	    void *next=*bootNext(cur);
	    if (S(cur)+*bootSize(cur)==ptr) {
		if (S(ptr)+*bootSize(ptr)==next && next!=NULL) {
		    /* coalesce with me and next (coalesce both ways) */
		    *bootSize(cur)+=*bootSize(ptr)+*bootSize(next);
		    *bootNext(cur)=*bootNext(next);
		} else {
		    /* coalesce with me (coalesce left) */
		    *bootSize(cur)+=*bootSize(ptr);
		}
	    } else if (S(ptr)+*bootSize(ptr)==next && next!=NULL) {
		/* coalesce with next (coalesce right) */
		*bootNext(cur)=ptr;
		*bootSize(ptr)+=*bootSize(next);
		*bootNext(ptr)=*bootNext(next);
	    } else if (ptr<next || next==NULL) {
		/* insert here */
		*bootNext(cur)=ptr;
		*bootNext(ptr)=next;
	    } else {
		cur=next;
		continue;
	    }
	    break;
	}
    }
}

static void bootSysAlloc(size_t needed) {
    void *result=pizPageAllocP(&needed);
    *bootSize(result)=needed;
    bootFree(bootObj(result));
}

static void *bootRemove(void **ptr,size_t size) {
    size_t oldSize=*bootSize(*ptr);
    void *result=*ptr;
    if (oldSize-size>=BOOT_MIN) {
	*ptr=S(result)+size;
	*bootSize(*ptr)=oldSize-size;
	*bootNext(*ptr)=*bootNext(result);
	*bootSize(result)=size;
    } else {
	*ptr=*bootNext(result);
    }
    return result;
}

static size_t bootObjectSize(size_t size) {
    size=((size+ALIGNMENT-1)&~(ALIGNMENT-1))+sizeof(void*);
    if (size<BOOT_MIN) {
	size=BOOT_MIN;
    }
    return size;
}

static void *bootMalloc(size_t size) {
    void **cur;
    if (HANDLE_ZERO && size==0) {
	return NULL;
    }
    size=bootObjectSize(size);
redo:
    cur=&g_bootHead;
    while (*cur!=NULL) {
	if (*bootSize(*cur)>=size) {
	    return bootObj(bootRemove(cur,size));
	}
	cur=bootNext(*cur);
    }
    bootSysAlloc(size);
    goto redo;
}

static void bootDownsize(void *ptr,size_t newSize) {
    newSize=bootObjectSize(newSize);
    ptr=bootHdr(ptr);
    if (*bootSize(ptr)-newSize >= BOOT_MIN) {
        *bootSize(S(ptr)+newSize)=*bootSize(ptr)-newSize;
        bootFree(bootObj(S(ptr)+newSize));
    }
}

void *fivmr_allocExecutable(uintptr_t size) {
    void *result;
    if (false && fivmr_POSIX_logAllocExec) {
        fivmr_Log_printf("[AE: allocating %" PRIdPTR " bytes]\n",size);
    }
    fivmr_Lock_lock(&g_lock);
    result=bootMalloc((size_t)size);
    fivmr_Lock_unlock(&g_lock);
    return result;
}

void fivmr_downsizeExecutable(void *ptr,uintptr_t newSize) {
    fivmr_Lock_lock(&g_lock);
    bootDownsize(ptr,newSize);
    fivmr_Lock_unlock(&g_lock);
}

void fivmr_freeExecutable(void *ptr) {
    if (false && fivmr_POSIX_logAllocExec) {
        fivmr_Log_printf("[AE: freeing %" PRIdPTR " bytes]\n",*bootSize(bootHdr(ptr)));
    }
    fivmr_Lock_lock(&g_lock);
    bootFree(ptr);
    fivmr_Lock_unlock(&g_lock);
}

#endif

