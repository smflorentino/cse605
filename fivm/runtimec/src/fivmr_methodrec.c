/*
 * fivmr_methodrec.c
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

fivmr_MethodRec *fivmr_MethodRec_reresolveSpecial(fivmr_ThreadState *ts,
                                                  fivmr_TypeData *from,
                                                  fivmr_MethodRec *target) {
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    if ((from->flags&FIVMR_TBF_NEW_SUPER_MODE) &&
        fivmr_TypeData_isSubtypeOf(ts,from,target->owner) &&
        from!=target->owner &&
        !fivmr_MethodRec_isInitializer(target)) {
        
        fivmr_assert(from->parent!=NULL);
        fivmr_assert(from->parent!=ts->vm->payload->td_top);
        
        target=fivmr_TypeData_findInstMethodNoIface3(
            ts->vm,from->parent,
            target->name,target->result,target->nparams,target->params);
        
        fivmr_assert(target!=NULL);
    }
    
    return target;
}

void *fivmr_MethodRec_staticDispatch(fivmr_ThreadState *ts,
                                     fivmr_MethodRec *mr,
				     fivmr_TypeData *td) {
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    if (!fivmr_MethodRec_exists(mr)) {
        /* FIXME
           this corresponds to the method not yet having been compiled.  if so
           then we should, like, do that, like, now, or something. */
        
        LOG(3,("method does not exist"));
    
        return NULL;
    }
    
    if (!strcmp(mr->name,"<init>") ||
	(mr->flags&FIVMR_MBF_METHOD_KIND)==FIVMR_MBF_FINAL ||
	(mr->owner->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
	(mr->flags&FIVMR_BF_STATIC)) {
        LOG(3,("returning entrypoint %p",mr->entrypoint));
        return mr->entrypoint;
    } else if ((mr->owner->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION ||
	       (mr->owner->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE) {
        LOG(3,("performing epoch-based itable lookup"));
	return td->epochs[ts->typeEpoch].itable[fivmr_MethodRec_itableIndex(ts,mr)];
    } else {
        LOG(3,("performing vtable lookup"));
	return td->vtable[mr->location];
    }
}

void *fivmr_MethodRec_dispatch(fivmr_ThreadState *ts,
                               fivmr_MethodRec *mr,
			       fivmr_Object object) {
    LOG(3,("dispatching method call to %s (%p)",fivmr_MethodRec_describe(mr),mr));
    if ((mr->flags&FIVMR_BF_STATIC)) {
        LOG(3,("method is static."));
        return fivmr_MethodRec_staticDispatch(ts,mr,NULL);
    } else {
        LOG(3,("method is instance."));
        fivmr_assert(object!=0);
        return fivmr_MethodRec_staticDispatch(
            ts, mr, fivmr_TypeData_forObject(&ts->vm->settings, object));
    }
}

fivmr_Value fivmr_MethodRec_call(fivmr_MethodRec *mr,
				 fivmr_ThreadState *ts,
				 void *methodPtr,
				 void *receiver,
				 fivmr_Value *userArgs,
				 fivmr_CallMode cm) {
    uintptr_t numSysArgs=1;
    uintptr_t receiverArg=0;
    uintptr_t spaceArg=0;
    uintptr_t numUserArgs;
    uintptr_t numAllArgs;
    fivmr_Value *allArgs;
    fivmr_Value result;
    bool calledMethod;
    
#if FIVMR_PROFILE_REFLECTION
    fivmr_Nanos before;
    
    before=fivmr_curTime();
#endif
    
    result=fivmr_NullValue();
    calledMethod=false;
    /* LOG(5,("MRC 1")); */
    if ((cm&FIVMR_CM_EXEC_STATUS)) {
	fivmr_ThreadState_goToJava(ts);
    }
    if (!fivmr_MethodRec_exists(mr)) {
        fivmr_throwNoSuchMethodError_inJava(ts,mr);
    } else {
        /* LOG(5,("MRC 2")); */
        if ((cm&FIVMR_CM_NULLCHECK) && !(mr->flags&FIVMR_BF_STATIC) && receiver==NULL) {
            fivmr_throwNullPointerRTE_inJava(ts);
        } else if ((cm&FIVMR_CM_CHECKINIT) && !fivmr_TypeData_checkInit(ts,mr->owner)) {
            /* oops - exception thrown from checkInit, drop down */
            /* LOG(5,("MRC 3")); */
        } else {
            if ((mr->flags&FIVMR_MBF_ALLOC_AS_CALLER)) {
                numSysArgs++;
            }
	
            if (!(mr->flags&FIVMR_BF_STATIC)) {
                receiverArg=numSysArgs;
                numSysArgs++;
            }

            numUserArgs=mr->nparams;
            numAllArgs=numUserArgs+numSysArgs; /* thread state */
            /* LOG(5,("MRC 4")); */
            allArgs=alloca(sizeof(fivmr_Value)*numAllArgs);
            bzero(allArgs,sizeof(fivmr_Value)*numAllArgs);
            allArgs[0].P=(uintptr_t)ts;
            if ((mr->flags&FIVMR_MBF_ALLOC_AS_CALLER)) {
                allArgs[1].I=FIVMR_GC_OBJ_SPACE; /* FIXME */
            }
            if (!(mr->flags&FIVMR_BF_STATIC)) {
                allArgs[receiverArg].P=(uintptr_t)receiver;
            }
            memcpy(allArgs+numSysArgs,userArgs,sizeof(fivmr_Value)*numUserArgs);
            /* LOG(5,("MRC 5")); */
            if ((cm&FIVMR_CM_RETURN_ARGS_BUF) && userArgs!=NULL) {
                fivmr_ThreadState_returnBuffer(ts,(char*)userArgs);
                userArgs=NULL;
            }
            /* LOG(5,("MRC 6")); */
            if ((cm&FIVMR_CM_HANDLES)) {
                int32_t i;
                if (!(mr->flags&FIVMR_BF_STATIC)) {
                    /* LOG(5,("MRC 6-1")); */
                    allArgs[receiverArg].L=fivmr_Handle_get(allArgs[receiverArg].H);
                }
                for (i=0;i<mr->nparams;++i) {
                    if (mr->params[i]->name[0]=='L' ||
                        mr->params[i]->name[0]=='[') {
                        /* LOG(5,("MRC 6-2")); */
                        allArgs[i+numSysArgs].L=
                            fivmr_Handle_get(allArgs[i+numSysArgs].H);
                    }
                }
            }
            /* LOG(5,("MRC 9")); */
            if ((cm&FIVMR_CM_CLASSCHANGE) && !(mr->flags&FIVMR_BF_STATIC) &&
                !fivmr_TypeData_isSubtypeOf(
                    ts,
                    fivmr_TypeData_forObject(&ts->vm->settings,allArgs[receiverArg].L),
                    mr->owner)) {
                /* LOG(5,("MRC 8")); */
                fivmr_throwClassChangeRTE_inJava(ts);
            } else {
                /* LOG(5,("MRC 10")); */
                if ((cm&FIVMR_CM_DISPATCH)) {
                    methodPtr=fivmr_MethodRec_dispatch(ts,mr,allArgs[receiverArg].L);
                }
                /* LOG(5,("MRC 11")); */
                if (methodPtr==NULL) {
                    /* when will this happen?
                       - when we call a method for which HAS_CODE is false
                       ... and when will that happen?
                       - when we try to do a nonvirtual call on an abstract method
                       in an abstract class. */
                    /* LOG(5,("MRC 7")); */
                    fivmr_throwAbstractMethodError_inJava(ts);
                } else {
#if FIVMR_HAVE_NATIVE_BACKEND
                    char *argTypes,*curArg;
                    int32_t i;
                    
                    argTypes=alloca(numAllArgs+1);
                    bzero(argTypes,numAllArgs+1);
                    curArg=argTypes;

                    *curArg++='P';

                    if ((mr->flags&FIVMR_MBF_ALLOC_AS_CALLER)) {
                        *curArg++='P';
                    }
                    
                    if (!(mr->flags&FIVMR_BF_STATIC)) {
                        *curArg++='P';
                    }
                    
                    for (i=0;i<mr->nparams;++i) {
                        *curArg++=fivmr_pointerifyBasetype(mr->params[i]->name[0]);
                    }
                    
                    calledMethod=true;
                    
                    LOG(5,("Calling method with retType = %c, argTypes = %s",
                           fivmr_pointerifyBasetype(mr->result->name[0]),
                           argTypes));
                    
                    result=fivmr_upcall(
                        methodPtr,
                        fivmr_pointerifyBasetype(mr->result->name[0]),
                        argTypes,
                        allArgs);
#else
                    /* LOG(5,("MRC 11-2")); */
                    calledMethod=true;
                    switch (mr->result->name[0]) {
                    case 'Z': 
                    case 'B':
                        /* LOG(5,("MRC 11-B")); */
                        result.B=
                            ((int8_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'C':
                        /* LOG(5,("MRC 11-C")); */
                        result.C=
                            ((uint16_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'S':
                        /* LOG(5,("MRC 11-S")); */
                        result.S=
                            ((int16_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'I':
                        /* LOG(5,("MRC 11-I")); */
                        result.I=
                            ((int32_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'J':
                        /* LOG(5,("MRC 11-J")); */
                        result.J=
                            ((int64_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'F':
                        /* LOG(5,("MRC 11-F")); */
                        result.F=
                            ((float (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'D':
                        /* LOG(5,("MRC 11-D")); */
                        result.D=
                            ((double (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'P':
                    case 'f':
                        /* LOG(5,("MRC 11-P")); */
                        result.P=
                            ((uintptr_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'L':
                    case '[':
                        /* LOG(5,("MRC 11-[")); */
                        result.L=
                            ((fivmr_Object (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'V':
                        LOG(5,("MRC 11-V"));
                        ((void (*)(void*,fivmr_Value*))mr->upcallPtr)(methodPtr,allArgs);
                        LOG(5,("MRC 11-V done")); 
                        break;
                    default:
                        fivmr_assert(!"bad type descriptor");
                        break;
                    }
#endif
                    if ((cm&FIVMR_CM_HANDLES) &&
                        (mr->result->name[0]=='L' ||
                         mr->result->name[0]=='[')) {
                        /* LOG(5,("MRC 11-[ handles")); */
                        result.H=fivmr_ThreadState_addHandle(ts,result.L);
                    }
                }
            }
        }
        /* LOG(5,("MRC 11-return?")); */
        if ((cm&FIVMR_CM_RETURN_ARGS_BUF) && userArgs!=NULL) {
            /* LOG(5,("MRC 11-return yes")); */
            fivmr_ThreadState_returnBuffer(ts,(char*)userArgs);
        }
        /* LOG(5,("MRC 15")); */
        if (ts->curException && calledMethod && (cm&FIVMR_CM_WRAP_EXCEPTION)) {
            fivmr_Object e=ts->curException;
            ts->curException=0;
            /* LOG(5,("MRC 13")); */
            fivmr_throwReflectiveException_inJava(ts,e);
        }
    }
    
    if ((cm&FIVMR_CM_HANDLES)) {
	/* LOG(5,("MRC 14")); */
	fivmr_ThreadState_handlifyException(ts);
    }
    
    if ((cm&FIVMR_CM_EXEC_STATUS)) {
	fivmr_ThreadState_goToNative(ts);
    }

#if FIVMR_PROFILE_REFLECTION
    fivmr_PR_invokeTime+=fivmr_curTime()-before;
#endif
    
    return result;
}

bool fivmr_MethodRec_matchesSig(fivmr_MethodRec *mr,
				const char *sig) {
    int pi,ci;
    if (sig[0]!='(') {
	return false;
    }
    pi=0;
    ci=1;
    while (pi<mr->nparams) {
	int n=strlen(mr->params[pi]->name);
	if (strncmp(mr->params[pi]->name,
		    sig+ci,
		    n)) {
	    return false;
	}
	pi++;
	ci+=n;
    }
    if (sig[ci++]!=')') {
	return false;
    }
    return !strcmp(sig+ci,mr->result->name);
}

const char *fivmr_MethodRec_describe(fivmr_MethodRec *mr) {
    if (mr==NULL) {
	return "(null mr)";
    } else {
	size_t size=1; /* for null char */
	int32_t i;
	char *result;
	size+=strlen(mr->owner->name);
	size++; /* slash */
	size+=strlen(mr->name);
	size++; /* lparen */
	for (i=0;i<mr->nparams;++i) {
	    size+=strlen(mr->params[i]->name);
	}
	size++; /* rparen */
	size+=strlen(mr->result->name);
	result=fivmr_threadStringBuf(size);
	strcpy(result,mr->owner->name);
	strcat(result,"/");
	strcat(result,mr->name);
	strcat(result,"(");
	for (i=0;i<mr->nparams;++i) {
	    strcat(result,mr->params[i]->name);
	}
	strcat(result,")");
	strcat(result,mr->result->name);
	return result;
    }
}

const char *fivmr_MethodRec_descriptor(fivmr_MethodRec *mr) {
    if (mr==NULL) {
	return "(null mr)";
    } else {
	size_t size=1; /* for null char */
	int32_t i;
	char *result;
	size++; /* lparen */
	for (i=0;i<mr->nparams;++i) {
	    size+=strlen(mr->params[i]->name);
	}
	size++; /* rparen */
	size+=strlen(mr->result->name);
	result=fivmr_threadStringBuf(size);
	strcpy(result,"(");
	for (i=0;i<mr->nparams;++i) {
	    strcat(result,mr->params[i]->name);
	}
	strcat(result,")");
	strcat(result,mr->result->name);
	return result;
    }
}

#if FIVMR_VERBOSE_RUN_METHOD
void fivmr_MethodRec_logEntry(fivmr_ThreadState *ts,
                              fivmr_MethodRec *mr) {
    fivmr_Log_lockedPrintf("Thread %u: entering %s\n",
			   ts->id,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logExit(fivmr_ThreadState *ts,
                             fivmr_MethodRec *mr) {
    if (ts->curException) {
	fivmr_Log_lockedPrintf("Thread %u: throwing out of %s\n",
			       ts->id,
			       fivmr_MethodRec_describe(mr));
    } else {
	fivmr_Log_lockedPrintf("Thread %u: returning from %s\n",
			       ts->id,
			       fivmr_MethodRec_describe(mr));
    }
}

void fivmr_MethodRec_logResultInt(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr,
				  int32_t result) {
    fivmr_Log_lockedPrintf("Thread %u: returning %d from %s\n",
			   ts->id,
			   result,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logResultLong(fivmr_ThreadState *ts,
                                   fivmr_MethodRec *mr,
				   int64_t result) {
    fivmr_Log_lockedPrintf("Thread %u: returning %" PRId64 " from %s\n",
			   ts->id,
			   result,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logResultFloat(fivmr_ThreadState *ts,
                                    fivmr_MethodRec *mr,
				    float result) {
    fivmr_Log_lockedPrintf("Thread %u: returning %lf from %s\n",
			   ts->id,
			   (double)result,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logResultDouble(fivmr_ThreadState *ts,
                                     fivmr_MethodRec *mr,
				     double result) {
    fivmr_Log_lockedPrintf("Thread %u: returning %lf from %s\n",
			   ts->id,
			   result,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logResultPtr(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr,
				  uintptr_t result) {
    fivmr_Log_lockedPrintf("Thread %u: returning Ptr[%p] from %s\n",
			   ts->id,
			   result,
			   fivmr_MethodRec_describe(mr));
}
#endif

void fivmr_MethodRec_registerMC(fivmr_MethodRec *mr,
                                fivmr_MachineCode *mc) {
    fivmr_VM *vm=fivmr_TypeData_getVM(mr->owner);

    fivmr_assert(fivmr_ThreadState_isInNative(fivmr_ThreadState_get(vm)));

    fivmr_MachineCode_up(mc);

    fivmr_Lock_lock(&vm->typeDataLock);
    mc->next=(fivmr_MachineCode*)mr->codePtr;
    mr->codePtr=mc;
    mr->flags|=FIVMR_MBF_DYNAMIC;
    fivmr_Lock_unlock(&vm->typeDataLock);
}

void fivmr_MethodRec_unregisterMC(fivmr_MethodRec *mr,
                                  fivmr_MachineCode *mc) {
    fivmr_VM *vm;
    fivmr_MachineCode **cur;
    bool found=false;
    
    vm=fivmr_TypeData_getVM(mr->owner);
    
    fivmr_assert(fivmr_ThreadState_isInNative(fivmr_ThreadState_get(vm)));

    fivmr_Lock_lock(&vm->typeDataLock);
    
    for (cur=(fivmr_MachineCode**)&mr->codePtr;*cur!=NULL;cur=&(*cur)->next) {
        if (*cur==mc) {
            *cur=mc->next;
            found=true;
            break;
        }
    }
    
    fivmr_assert(found);
    
    fivmr_Lock_unlock(&vm->typeDataLock);
    
    fivmr_MachineCode_down(mc);
}

fivmr_MachineCode *fivmr_MethodRec_findMC(fivmr_MethodRec *mr,
                                          fivmr_MachineCodeFlags mask,
                                          fivmr_MachineCodeFlags expected) {
    fivmr_VM *vm;
    fivmr_MachineCode *mc;

    vm=fivmr_TypeData_getVM(mr->owner);
    
    fivmr_assert(fivmr_ThreadState_isInNative(fivmr_ThreadState_get(vm)));
    
    fivmr_Lock_lock(&vm->typeDataLock);

    if ((mr->flags&FIVMR_MBF_DYNAMIC)) {
        bool found=false;
        
        for (mc=(fivmr_MachineCode*)mr->codePtr;mc!=NULL;mc=mc->next) {
            if ((mc->flags&mask)==expected) {
                fivmr_MachineCode_up(mc);
                found=true;
                break;
            }
        }
        
        fivmr_assert(found==(mc!=NULL));
    } else {
        mc=NULL;
    }

    fivmr_Lock_unlock(&vm->typeDataLock);
    
    return mc;
}

bool fivmr_MethodRec_hasMC(fivmr_MethodRec *mr,
                           fivmr_MachineCodeFlags mask,
                           fivmr_MachineCodeFlags expected) {
    /* FIXME: this could be optimized */
    fivmr_MachineCode *mc=fivmr_MethodRec_findMC(mr,mask,expected);
    
    if (mc!=NULL) {
        fivmr_MachineCode_down(mc);
        return true;
    } else {
        return false;
    }
}


