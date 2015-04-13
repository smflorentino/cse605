/*
 * fivmr_gatehelpers.c
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

fivmr_Handle *fivmr_GateHelpers_installObjectFieldReference(fivmr_ThreadState *ts,
                                                            fivmr_Object referent,
                                                            uintptr_t fieldOffset,
                                                            fivmr_TypeData *td) {
    fivmr_Object result;
    fivmr_Handle *h;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_alloc(ts,FIVMR_GC_OBJ_SPACE,td);
    if (result) {
        h=fivmr_ThreadState_addHandle(ts,result);
        fivmr_objectPutField(ts,referent,fieldOffset,result,
                             0 /* FIXME currently only works
                                  for default flags */);
    } else {
        ts->curException=0;
        h=NULL;
    }
    fivmr_ThreadState_goToNative(ts);
    return h;
}

fivmr_Handle *fivmr_GateHelpers_installArrayFieldReference(fivmr_ThreadState *ts,
                                                           fivmr_Object referent,
                                                           uintptr_t fieldOffset,
                                                           fivmr_TypeData *td,
                                                           int32_t length) {
    fivmr_Object result;
    fivmr_Handle *h;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_allocArray(ts,FIVMR_GC_OBJ_SPACE,td,length);
    if (result) {
        h=fivmr_ThreadState_addHandle(ts,result);
        fivmr_objectPutField(ts,referent,fieldOffset,result,
                             0 /* FIXME currently only works
                                  for default flags */);
    } else {
        ts->curException=0;
        h=NULL;
    }
    fivmr_ThreadState_goToNative(ts);
    return h;
}

fivmr_Handle *fivmr_GateHelpers_installObjectElementReference(fivmr_ThreadState *ts,
                                                              fivmr_Object referent,
                                                              int32_t index,
                                                              fivmr_TypeData *td) {
    fivmr_Object result;
    fivmr_Handle *h;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_alloc(ts,FIVMR_GC_OBJ_SPACE,td);
    if (result) {
        h=fivmr_ThreadState_addHandle(ts,result);
        fivmr_objectArrayStore(ts,referent,index,result,
                               0 /* FIXME currently only works
                                    for default flags */);
    } else {
        ts->curException=0;
        h=NULL;
    }
    fivmr_ThreadState_goToNative(ts);
    return h;
}

fivmr_Handle *fivmr_GateHelpers_installArrayElementReference(fivmr_ThreadState *ts,
                                                             fivmr_Object referent,
                                                             int32_t index,
                                                             fivmr_TypeData *td,
                                                             int32_t length) {
    fivmr_Object result;
    fivmr_Handle *h;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_allocArray(ts,FIVMR_GC_OBJ_SPACE,td,length);
    if (result) {
        h=fivmr_ThreadState_addHandle(ts,result);
        fivmr_objectArrayStore(ts,referent,index,result,
                               0 /* FIXME currently only works
                                    for default flags */);
    } else {
        ts->curException=0;
        h=NULL;
    }
    fivmr_ThreadState_goToNative(ts);
    return h;
}

