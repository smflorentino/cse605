/*
 * fivmr_stackalloc.c
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

#include <fivmr.h>

void fivmr_SA_init(fivmr_ThreadState *ts) {
    fivmr_GCSpaceAlloc *alloc=ts->gc.alloc+FIVMR_GC_SA_SPACE;
    alloc->bump=alloc->start=
	(uintptr_t)fivmr_mallocAssert(ts->vm->config.stackAllocSize)
	+FIVMR_ALLOC_OFFSET(&ts->vm->settings);
    alloc->size=ts->vm->config.stackAllocSize;
}

void fivmr_SA_destroy(fivmr_ThreadState *ts) {
    fivmr_GCSpaceAlloc *alloc=ts->gc.alloc+FIVMR_GC_SA_SPACE;
    if (alloc->start==0) {
        /* we never initialized or we already destroyed ... either way, assert that this
           is indeed the case.  this scenario is actually likely to come up: a checkExit()
           can occur after the thread is already finalized, in which case it'll be
           finalized again.  We could of course prevent this in threadstate.c, but it
           seems that it's more natural to just require all finalization routines to
           have their own checks for double-finalization.  in particular, the GC already
           effectively has such checks, since for the GC finalization just means
           committing mutator state, and it's already the case that a commit the
           comes immediately after another commit with no interleaved allocation or
           marking activity is no-op.  Thus, this is the only place where a check for
           double finalization is necessary. */
        fivmr_assert(alloc->start==0);
        fivmr_assert(alloc->bump==0);
        fivmr_assert(alloc->size==0);
    } else {
        fivmr_free((void*)(alloc->start-FIVMR_ALLOC_OFFSET(&ts->vm->settings)));
        alloc->start=0;
        alloc->bump=0;
        alloc->size=0;
    }
}

void fivmr_SA_clear(fivmr_ThreadState *ts) {
    fivmr_GCSpaceAlloc *alloc=ts->gc.alloc+FIVMR_GC_SA_SPACE;
    fivmr_assert(alloc->start==0);
    fivmr_assert(alloc->bump==0);
    fivmr_assert(alloc->size==0);
}

