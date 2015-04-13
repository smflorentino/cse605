/*
 * fivmr_handleregion.c
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

fivmr_Handle *fivmr_HandleRegion_add(fivmr_HandleRegion *hr,
				     fivmr_ThreadState *ts,
				     fivmr_Handle **freelist,
				     fivmr_Object obj) {
    uintptr_t execStatus=ts->execStatus;
    fivmr_assert(execStatus==FIVMR_TSES_IN_JAVA ||
		 execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK);
    if (obj==0) {
	return NULL;
    } else {
	fivmr_Handle *h=NULL;
	int oldErrno=errno;
	
	if (freelist!=NULL) {
	    h=*freelist;
	}
	if (h==NULL) {
	    fivmr_Lock_lock(&ts->vm->hrLock);
	    h=ts->vm->freeHandles;
	    if (h==NULL) {
		fivmr_Lock_unlock(&ts->vm->hrLock);
		h=fivmr_mallocAssert(sizeof(fivmr_Handle));
	    } else {
		ts->vm->freeHandles=h->next;
		fivmr_Lock_unlock(&ts->vm->hrLock);
	    }
	} else {
	    *freelist=h->next;
	}
	
        fivmr_Handle_set(h,ts,obj);

	h->prev=&hr->head;
	h->next=hr->head.next;
	hr->head.next->prev=h;
	hr->head.next=h;
	
	errno=oldErrno;
	return h;
    }
}

void fivmr_Handle_remove(fivmr_Handle **freelist,
			 fivmr_Handle *h) {
    if (h!=NULL) {
	h->prev->next=h->next;
	h->next->prev=h->prev;
	h->obj=0;
	h->prev=NULL;
	h->next=*freelist;
	*freelist=h;
    }
}

void fivmr_HandleRegion_removeAll(fivmr_HandleRegion *hr,
				  fivmr_Handle **freelist) {
    while (hr->head.next!=&hr->tail) {
	fivmr_Handle_remove(freelist,hr->head.next);
    }
}


