/*
 * fivmr_reflectlog.c
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

bool fivmr_logReflect;

static void pre(fivmr_ThreadState *ts,
                int32_t depth) {
    fivmr_Log_lock();
    fivmr_Log_print("fivmr reflect log: ");
    fivmr_Log_print(fivmr_MethodRec_describe(fivmr_findCaller(ts,depth)));
}

static void post(fivmr_ThreadState *ts) {
    fivmr_Log_print("\n");
    fivmr_Log_unlock();
}

void fivmr_ReflectLog_dynamicCall(fivmr_ThreadState *ts,
                                  int32_t depth,
                                  fivmr_MethodRec *mr) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" dynamically called ");
	fivmr_Log_print(fivmr_MethodRec_describe(mr));
	post(ts);
    }
}

void fivmr_ReflectLog_call(fivmr_ThreadState *ts,
                           int32_t depth,
                           fivmr_MethodRec *mr) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" called ");
	fivmr_Log_print(fivmr_MethodRec_describe(mr));
	post(ts);
    }
}

void fivmr_ReflectLog_access(fivmr_ThreadState *ts,
                             int32_t depth,
                             fivmr_FieldRec *fr) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" accessed ");
	fivmr_Log_print(fivmr_FieldRec_describe(fr));
	post(ts);
    }
}

void fivmr_ReflectLog_alloc(fivmr_ThreadState *ts,
                            int32_t depth,
                            fivmr_TypeData *td) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" alloced ");
	fivmr_Log_print(td->name);
	post(ts);
    }
}

void fivmr_ReflectLog_use(fivmr_ThreadState *ts,
                          int32_t depth,
                          fivmr_TypeData *td) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" used ");
	fivmr_Log_print(td->name);
	post(ts);
    }
}

/* finds the first call into Class, Method, Field, or Constructor. */
static uintptr_t findFirstCallIntoReflect_cback(fivmr_VM *vm,
                                                uintptr_t arg,
						fivmr_MethodRec *mr,
						int32_t lineNumber) {
    int32_t *state=(int32_t*)arg;
    switch (*state) {
    case 0: /* still looking for public method of Class, Method, Field, or Constructor */
        /* FIXME - need some better way of comparing the owner... */
	if ((mr->owner==vm->payload->td_Constructor ||
             mr->owner==vm->payload->td_Method ||
             mr->owner==vm->payload->td_Field ||
	     mr->owner==vm->payload->td_Class) &&
	    (mr->flags&FIVMR_BF_VISIBILITY)==FIVMR_BF_PUBLIC) {
	    *state=1;
	}
	break;
    case 1: /* looking for method from a class other than Class, Method, Field, or
	       Constructor */
	if (mr->owner!=vm->payload->td_Constructor &&
	    mr->owner!=vm->payload->td_Method &&
	    mr->owner!=vm->payload->td_Field &&
	    mr->owner!=vm->payload->td_Class) {
	    return (uintptr_t)mr;
	}
	break;
    default: fivmr_assert(false);
    }
    return 0;
}

static fivmr_MethodRec *findFirstCallIntoReflect(fivmr_ThreadState *ts) {
    int32_t state=0;
    return (fivmr_MethodRec*)
	fivmr_iterateDebugFrames(ts->vm,
                                 ts->curF,
				 findFirstCallIntoReflect_cback,
				 (uintptr_t)&state);
}

static void preReflect(fivmr_ThreadState *ts) {
    fivmr_Log_lock();
    fivmr_Log_print("fivmr reflect log: ");
    fivmr_Log_print(fivmr_MethodRec_describe(findFirstCallIntoReflect(ts)));
}

void fivmr_ReflectLog_allocReflect(fivmr_ThreadState *ts,
                                   fivmr_TypeData *td) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" alloced ");
	fivmr_Log_print(td->name);
	post(ts);
    }
}

void fivmr_ReflectLog_dynamicCallReflect(fivmr_ThreadState *ts,
                                         fivmr_MethodRec *mr) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" dynamically called ");
	fivmr_Log_print(fivmr_MethodRec_describe(mr));
	post(ts);
    }
}

void fivmr_ReflectLog_callReflect(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" called ");
	fivmr_Log_print(fivmr_MethodRec_describe(mr));
	post(ts);
    }
}

void fivmr_ReflectLog_accessReflect(fivmr_ThreadState *ts,
                                    fivmr_FieldRec *fr) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" accessed ");
	fivmr_Log_print(fivmr_FieldRec_describe(fr));
	post(ts);
    }
}

void fivmr_ReflectLog_useReflectByName(fivmr_ThreadState *ts,
                                       const char *name) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" used ");
	fivmr_Log_print(name);
	post(ts);
    }
}

void fivmr_ReflectLog_useReflect(fivmr_ThreadState *ts,
                                 fivmr_TypeData *td) {
    fivmr_ReflectLog_useReflectByName(ts,td->name);
}

