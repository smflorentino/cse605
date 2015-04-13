/*
 * fivmr_typeaux.c
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

fivmr_TypeAux *fivmr_TypeAux_add(void *region,
                                 fivmr_TypeAux **list,
                                 uintptr_t size,
                                 fivmr_TypeAuxFlags flags) {
    fivmr_TypeAux *result=
        freg_cond_alloc(region,fivmr_alignRaw(sizeof(fivmr_TypeAux),8)+size);    
    fivmr_assert(result!=NULL);
    
    result->size=size;
    result->occupied=0;
    result->flags=flags;
    
    result->next=*list;
    *list=result;

    return result;
}

void fivmr_TypeAux_deleteAll(fivmr_TypeAux **list) {
    while (*list!=NULL) {
        fivmr_TypeAux *cur=*list;
        *list=cur->next;
        fivmr_free(cur);
    }
}

fivmr_TypeAux *fivmr_TypeAux_first(fivmr_TypeAux *list,
                                   fivmr_TypeAuxFlags mask,
                                   fivmr_TypeAuxFlags expected) {
    while (list!=NULL && (list->flags&mask)!=expected) {
        list=list->next;
    }
    return list;
}

fivmr_TypeAux *fivmr_TypeAux_next(fivmr_TypeAux *list,
                                  fivmr_TypeAuxFlags mask,
                                  fivmr_TypeAuxFlags expected) {
    return fivmr_TypeAux_first(list->next,mask,expected);
}

void *fivmr_TypeAux_addElement(void *region,
                               fivmr_TypeAux **list,
                               fivmr_TypeAuxFlags mask,
                               fivmr_TypeAuxFlags expected,
                               fivmr_TypeAuxFlags flags,
                               void *data,
                               size_t size) {
    fivmr_TypeAux *trlist;
    uintptr_t nextIdx;
    uintptr_t next;
    
    fivmr_assert(size<=8);
    
    trlist=fivmr_TypeAux_first(*list,mask,expected);
    if (trlist==NULL || fivmr_alignRaw(trlist->occupied,size)+size>trlist->size) {
        trlist=fivmr_TypeAux_add(region,list,16*sizeof(uintptr_t),flags);
    }
    
    nextIdx=fivmr_alignRaw(trlist->occupied,size);
    trlist->occupied=nextIdx+size;
    
    next=fivmr_TypeAux_data(trlist)+nextIdx;
    memcpy((void*)next,data,size);
    
    return (void*)next;
}

void *fivmr_TypeAux_addUntraced(void *region,
                                fivmr_TypeAux **list,
                                void *data,
                                size_t size) {
    return fivmr_TypeAux_addElement(region,list,
                                    FIVMR_TAF_TRACED,0,0,
                                    data,size);
}

void *fivmr_TypeAux_addUntracedZero(void *region,
                                    fivmr_TypeAux **list,
                                    size_t size) {
    int64_t zero=0;
    return fivmr_TypeAux_addUntraced(region,list,(void*)&zero,size);
}

fivmr_Object *fivmr_TypeAux_addPointer(void *region,
                                       fivmr_TypeAux **list,
                                       fivmr_Object obj) {
    return (fivmr_Object*)
        fivmr_TypeAux_addElement(region,list,
                                 FIVMR_TAF_TRACED,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED,
                                 (void*)&obj,
                                 sizeof(fivmr_Object));
}



