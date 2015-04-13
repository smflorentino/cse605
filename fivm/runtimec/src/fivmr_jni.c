/*
 * fivmr_jni.c
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

#define FIVMR_IN_JNI_MODULE 1

#include "fivmr_jni.h"
#include "fivmr.h"
#include <stdarg.h>

/* NOTE: we're currently doing type checks on arrays and strings, but not
   on jclass.  Seems like a reasonable judgment call (we access jclass way
   more often, so there is a performance argument, and it just feels like
   programmer errors there are less likely given that jclass is rarely
   used as jobject).  But, I should check what the reference implementation
   (i.e. HotSpot) does. */

/* helper for cases where you're IN_NATIVE, you know that the object is non-moving
   and you don't *really* need a handle to it, but JNI requires that we produce
   a handle anyway. */
static fivmr_Handle *produceHandleFromNative(fivmr_ThreadState *ts,
					     fivmr_Object obj) {
    fivmr_Handle *result;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_ThreadState_addHandle(ts,obj);
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static void transferArgVal(fivmr_TypeStub *td,
			   jvalue *arg,
			   va_list lst) {
    /* FIXME: these '#if 1' thingies need to be turned into proper checks against
       something tested by autoconf.  the way this code currently works (i.e. ignoring
       the else cases) should be correct on most C compilers and calling conventions;
       it certainly is for all of the ones I'm familiar with, but that's not to say
       that there won't be some goofy platform where passing a float through a vararg
       preserves the fact that it's a float rather than coercing it to double as is
       done on cdecl. */
    switch (td->name[0]) {
    case 'Z':
#if 1
	arg->z=va_arg(lst,int);
#else
	arg->z=va_arg(lst,jboolean);
#endif
	break;
    case 'B':
#if 1
	arg->b=va_arg(lst,int);
#else
	arg->b=va_arg(lst,jbyte);
#endif
	break;
    case 'C':
#if 1
	arg->c=va_arg(lst,int);
#else
	arg->c=va_arg(lst,jchar);
#endif
	break;
    case 'S':
#if 1
	arg->s=va_arg(lst,int);
#else
	arg->s=va_arg(lst,jshort);
#endif
	break;
    case 'I':
	arg->i=va_arg(lst,jint);
	break;
    case 'J':
	arg->j=va_arg(lst,jlong);
	break;
    case 'F':
#if 1
	arg->f=va_arg(lst,double);
#else
	arg->f=va_arg(lst,jfloat);
#endif
	break;
    case 'D':
	arg->d=va_arg(lst,jdouble);
	break;
    case 'L':
    case '[':
	arg->l=va_arg(lst,jobject);
	break;
    default: fivmr_assert(!"bad type descriptor");
    }
}

static void transferArgVals(fivmr_MethodRec *mr,
			    jvalue *args,
			    va_list lst) {
    int32_t i;
    for (i=0;i<mr->nparams;++i) {
	transferArgVal(mr->params[i],args+i,lst);
    }
}

static bool check_init(fivmr_ThreadState *ts,
                       fivmr_TypeData *td) {
    LOG(12,("check_init called on %p",td));
    LOG(11,("check_init called on %p, which refers to %s",td,td->name));
    fivmr_ReflectLog_use(ts,-1,td);
    if (fivmr_TypeData_resolve(td)) {
        if (fivmr_TypeData_checkInit(ts,td)) {
            return true;
        }
    } else {
        char buf[256];
        snprintf(buf,sizeof(buf),
                 "Could not link and resolve %s",
                 td->name);
        fivmr_throwLinkageError_inJava(ts,buf);
    }
    fivmr_ThreadState_handlifyException(ts);
    return false;
}

static bool check_init_inNative(fivmr_ThreadState *ts,
                                fivmr_TypeData *td) {
    bool result;
    if (td->inited==1) {
        result=true;
    } else {
        fivmr_ThreadState_goToJava(ts);
        result=check_init(ts,td);
        fivmr_ThreadState_goToNative(ts);
    }
    return result;
}

static bool check_init_easy(JNIEnv *env,
			    jclass clazz) {
    bool result;
    fivmr_ThreadState_goToJava(((fivmr_JNIEnv*)env)->ts);
    result=check_init(((fivmr_JNIEnv*)env)->ts,
                      fivmr_TypeData_fromClass(((fivmr_JNIEnv*)env)->ts,
                                               fivmr_Handle_get((fivmr_Handle*)clazz)));
    fivmr_ThreadState_goToNative(((fivmr_JNIEnv*)env)->ts);
    return result;
}

static fivmr_Value virtual_call(jmethodID methodID,
				JNIEnv *env,
				jobject receiver,
				jvalue *args) {
    LOG(8,("virtual call"));
    return fivmr_MethodRec_call(
	(fivmr_MethodRec*)methodID,
	((fivmr_JNIEnv*)env)->ts,
	NULL,
	(void*)receiver,
	(fivmr_Value*)args,
	FIVMR_CM_HANDLES|FIVMR_CM_EXEC_STATUS|FIVMR_CM_DISPATCH|
	FIVMR_CM_NULLCHECK|FIVMR_CM_CLASSCHANGE);
}

static fivmr_Value special_call(jmethodID methodID,
				JNIEnv *env,
				jclass clazz,
				jobject receiver,
				jvalue *args) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Value result;
    LOG(8,("special call"));
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_MethodRec_call(
	(fivmr_MethodRec*)methodID,
	ts,
	fivmr_MethodRec_staticDispatch(
            ts,
	    (fivmr_MethodRec*)methodID,
	    fivmr_TypeData_fromClass(((fivmr_JNIEnv*)env)->ts,
                                     fivmr_Handle_get((fivmr_Handle*)clazz))),
	(void*)receiver,
	(fivmr_Value*)args,
	FIVMR_CM_HANDLES|FIVMR_CM_NULLCHECK|
	FIVMR_CM_CLASSCHANGE);
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static fivmr_Value static_call(jmethodID methodID,
			       JNIEnv *env,
			       jvalue *args) {
    LOG(8,("static call"));
    return fivmr_MethodRec_call(
	(fivmr_MethodRec*)methodID,
	((fivmr_JNIEnv*)env)->ts,
	NULL,
	NULL,
	(fivmr_Value*)args,
	FIVMR_CM_HANDLES|FIVMR_CM_EXEC_STATUS|FIVMR_CM_DISPATCH|
	FIVMR_CM_CHECKINIT);
}

static fivmr_Value virtual_call_v(jmethodID methodID,
				  JNIEnv *env,
				  jobject receiver,
				  va_list lst) {
    jvalue *args;
    size_t size=sizeof(jvalue)*fivmr_MethodRec_numParams((fivmr_MethodRec*)methodID);
    args=alloca(size);
    bzero(args,size);
    transferArgVals((fivmr_MethodRec*)methodID,args,lst);
    return virtual_call(methodID,env,receiver,args);
}

static fivmr_Value special_call_v(jmethodID methodID,
				  JNIEnv *env,
				  jclass clazz,
				  jobject receiver,
				  va_list lst) {
    jvalue *args;
    args=alloca(sizeof(jvalue)*
		fivmr_MethodRec_numParams((fivmr_MethodRec*)methodID));
    transferArgVals((fivmr_MethodRec*)methodID,args,lst);
    return special_call(methodID,env,clazz,receiver,args);
}

static fivmr_Value static_call_v(jmethodID methodID,
				 JNIEnv *env,
				 va_list lst) {
    jvalue *args;
    args=alloca(sizeof(jvalue)*
		fivmr_MethodRec_numParams((fivmr_MethodRec*)methodID));
    transferArgVals((fivmr_MethodRec*)methodID,args,lst);
    return static_call(methodID,env,args);
}

static fivmr_Handle *alloc_array(JNIEnv *env,
				 fivmr_TypeData *td,
				 jsize length) {
    fivmr_ThreadState *ts;
    fivmr_Handle *result;
    ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ReflectLog_alloc(ts,-1,td);
    result=NULL;
    fivmr_ThreadState_goToJava(ts);
    if (length<0) {
	fivmr_throwNegativeSizeRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	result=fivmr_ThreadState_addHandle(
	    ts,
	    fivmr_allocArray(ts,FIVMR_GC_OBJ_SPACE,td,length));
    }
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static jint jni_DestroyJavaVM(JavaVM *vm) {
    return -1;
}

static jint attachCurrentThread(JavaVM *vm_,
				JNIEnv **pEnv,
				void *threadArgs,
				bool isDaemon) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    
    ctx=((fivmr_JNIVM*)vm_)->ctx;
    vm=ctx->vm;
    
    ts=fivmr_ThreadState_getNullable(vm);
    if (ts!=NULL) {
	/* already attached */
	*pEnv=(JNIEnv*)&ts->curNF->jni;
	return 0;
    } else {
	/* not attached - create new thread */
	fivmr_Handle *vmt;
	
	ts=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
        if (ts!=NULL) {
            fivmr_ThreadState_setBasePrio(
                ts,
                fivmr_Thread_getPriority(
                    fivmr_ThreadHandle_current()));
            /* total hack; we don't actually know what the stack height is... */
            fivmr_ThreadState_setStackHeight(
                ts,
                fivmr_Thread_stackHeight()/2-FIVMR_STACK_HEIGHT_HEADROOM);
            fivmr_ThreadState_set(ts,ctx);
            vmt=fivmr_VMThread_create(ts,0,isDaemon);
            fivmr_assert(vmt!=NULL);
            fivmr_assert(ts->curExceptionHandle==NULL);
            if (fivmr_ThreadState_glue(ts,vmt)) {
                
                *pEnv=(JNIEnv*)&ts->curNF->jni;
                return 0;
            }
        }
        return -1; /* ?? */
    }
}

static jint jni_AttachCurrentThread(JavaVM *vm,
				    JNIEnv **pEnv,
				    void *threadArgs) {
    return attachCurrentThread(vm,pEnv,threadArgs,false);
}

static jint jni_AttachCurrentThreadAsDaemon(JavaVM *vm,
					    JNIEnv **pEnv,
					    void *threadArgs) {
    return attachCurrentThread(vm,pEnv,threadArgs,true);
}

static jint jni_DetachCurrentThread(JavaVM *vm_) {
    fivmr_VM *vm;
    fivmr_ThreadPriority prio;
    
    vm=((fivmr_JNIVM*)vm_)->ctx->vm;

    prio=fivmr_ThreadState_terminate(fivmr_ThreadState_get(vm));
    
    fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                             prio);
    return 0;
}

static jint jni_GetEnv(JavaVM *vm_,JNIEnv **penv,jint version) {
    /* FIXME: do something with the version. */
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    vm=((fivmr_JNIVM*)vm_)->ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    if (ts!=NULL) {
	*penv=(JNIEnv*)&ts->curNF->jni;
	return 0;
    } else {
	return -2;
    }
}

static struct JNIInvokeInterface jni_invoke_functions={
    NULL,
    NULL,
    NULL,
    jni_DestroyJavaVM,
    jni_AttachCurrentThread,
    jni_DetachCurrentThread,
    jni_GetEnv,
    jni_AttachCurrentThreadAsDaemon
};

jint JNI_GetDefaultJavaVMInitArgs(void *args) {
    return -1;
}

jint JNI_CreateJavaVM(JavaVM **pvm,void **penv,void *args) {
    return -1;
}

jint JNI_GetCreatedJavaVMs(JavaVM **_1,jsize _2,jsize *_3) {
    return -1;
}

static jint jni_GetVersion(JNIEnv *env_) {
    return JNI_VERSION_1_4;
}

static jclass jni_DefineClass(JNIEnv *env_,
			      jobject loader,
			      const jbyte *buf,
			      jsize bufLen) {
    fivmr_throwInternalError(
        ((fivmr_JNIEnv*)env_)->ts,
        "JNI DefineClass operation is not (yet) supported");
    return NULL;
}

static jclass jni_FindClass(JNIEnv *env_,const char *name) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_TypeData *td;
    fivmr_Handle *result;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState_goToJava(env->ts);
    fivmr_assert(env->ts->curNF!=NULL);
    ctx=env->ctx;
    fivmr_assert(ctx!=NULL);
    td=fivmr_TypeContext_findClass(ctx,name,'/');
    if (td==NULL) {
	LOG(3,("Could not find class %s",name));
	fivmr_throwNoClassDefFoundError_inJava(
            env->ts,name,"in a JNI FindClass call from native code");
	result=NULL;
    } else {
	fivmr_ReflectLog_use(env->ts,-1,td);
	result=fivmr_ThreadState_addHandle(env->ts,
                                           td->classObject);
	LOG(6,("jni_FindClass for %s returning %p (td = %s).",name,(void*)result,td->name));
    }
    fivmr_ThreadState_goToNative(env->ts);
    return (jclass)result;
}

static jclass jni_GetSuperclass(JNIEnv *env_,jclass clazz) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_TypeData *td;
    td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
    if (td->parent==env->ts->vm->payload->td_top /* FIXME */) {
	return NULL;
    } else {
	return (jclass)produceHandleFromNative(env->ts,td->parent->classObject);
    }
}

static jboolean jni_IsAssignableFrom(JNIEnv *env_,
				     jclass clazz1,
				     jclass clazz2) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    /* return true if clazz2 is a subtype of clazz1 */
    fivmr_TypeData *td1=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz1);
    fivmr_TypeData *td2=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz2);
    jboolean result;
    switch (td1->name[0]) {
    case 'S':
	switch (td2->name[0]) {
	case 'B': return true;
	default: break;
	}
	break;
    case 'I':
	switch (td2->name[0]) {
	case 'S':
	case 'B':
	case 'C': return true;
	default: break;
	}
	break;
    case 'L':
	switch (td2->name[0]) {
	case 'I':
	case 'S':
	case 'B':
	case 'C': return true;
	default: break;
	}
	break;
    case 'F':
	switch (td2->name[0]) {
	case 'L':
	case 'I':
	case 'S':
	case 'B':
	case 'C': return true;
	default: break;
	}
	break;
    case 'D':
	switch (td2->name[0]) {
	case 'F':
	case 'L':
	case 'I':
	case 'S':
	case 'B':
	case 'C': return true;
	default: break;
	}
	break;
    default: break;
    }
    fivmr_ThreadState_goToJava(env->ts);
    result=(jboolean)fivmr_TypeData_isSubtypeOf(env->ts,td2,td1);
    fivmr_ThreadState_goToNative(env->ts);
    return result;
}

static jint jni_Throw(JNIEnv *env_,
		      jthrowable obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    int logAtLevel;
    
    if (env->ts->vm->exceptionsFatalReason!=NULL || fivmr_abortThrow) {
        const char *reason;
        reason=env->ts->vm->exceptionsFatalReason;
        if (reason==NULL) {
            reason="Unexpected exception (fivmr_abortThrow=true)";
        }
	fivmr_Log_lockedPrintf("Thread %u: %s: %s (%p)\n",
			       env->ts->id,
			       reason,
			       fivmr_TypeData_forObject(&env->ts->vm->settings,
                                                        ((fivmr_Handle*)obj)->obj)->name,
			       obj);
	fivmr_ThreadState_dumpStackFor(env->ts);
	fivmr_abortf("%s.",reason);
    }
    
    if ((env->ts->vm->flags&FIVMR_VMF_LOG_THROW)) {
        logAtLevel=0;
    } else {
        logAtLevel=3;
    }

    if (LOGGING(logAtLevel)) {
	LOG(logAtLevel,
            ("Thread %u: throwing exception %p with type %s (from native code)",
             env->ts->id,obj,fivmr_TypeData_forObject(&env->ts->vm->settings,
                                                      ((fivmr_Handle*)obj)->obj)->name));
	fivmr_ThreadState_dumpStackFor(env->ts);
    }

    env->ts->curExceptionHandle=(fivmr_Handle*)obj;
    return 0;
}

static jint jni_ThrowNew(JNIEnv *env,
			 jclass clazz,
			 const char *message) {
    jmethodID ctor;
    jobject obj;
    jstring str;
    ctor=(*env)->GetMethodID(env,clazz,"<init>","(Ljava/lang/String;)V");
    if (ctor==NULL) {
	return -1;
    }
    str=(*env)->NewStringUTF(env,message);
    if (str==NULL) {
	return -1;
    }
    obj=(*env)->NewObject(env,clazz,ctor,str);
    if (obj==NULL) {
	return -1;
    }
    return jni_Throw(env,obj);
}

static jthrowable jni_ExceptionOccurred(JNIEnv *env_) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    return (jthrowable)env->ts->curExceptionHandle;
}

static void jni_ExceptionDescribe(JNIEnv *env_) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    /* what if there was no exception? currently it'll just abort. */
    fivmr_describeException(env->ts,env->ts->curExceptionHandle);
}

static void jni_ExceptionClear(JNIEnv *env_) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    env->ts->curExceptionHandle=NULL;
}

static void jni_FatalError(JNIEnv *env_,const char *msg) {
    fivmr_abortf("Fatal error in JNI: %s",msg);
}

static jobject jni_NewGlobalRef(JNIEnv *env_,jobject obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_Handle *result;
    fivmr_ThreadState_goToJava(env->ts);
    result=fivmr_newGlobalHandle(env->ts,(fivmr_Handle*)obj);
    fivmr_ThreadState_goToNative(env->ts);
    return (jobject)result;
}

static void jni_DeleteGlobalRef(JNIEnv *env_,jobject obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_ThreadState_goToJava(env->ts);
    fivmr_deleteGlobalHandle((fivmr_Handle*)obj);
    fivmr_ThreadState_goToNative(env->ts);
}

static void jni_DeleteLocalRef(JNIEnv *env_,jobject obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_ThreadState_goToJava(env->ts);
    fivmr_Handle_remove(&env->ts->freeHandles,(fivmr_Handle*)obj);
    fivmr_ThreadState_goToNative(env->ts);
}

static jboolean jni_IsSameObject(JNIEnv *env_,
				 jobject ref1,
				 jobject ref2) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    bool result;
    /* FIXME if we ever have equality barriers... */
    fivmr_ThreadState_goToJava(env->ts);
    result=
	fivmr_Handle_get((fivmr_Handle*)ref1) ==
	fivmr_Handle_get((fivmr_Handle*)ref2);
    fivmr_ThreadState_goToNative(env->ts);
    return (jboolean)result;
}

static jobject jni_AllocObject(JNIEnv *env_,
			       jclass clazz) {
    /* what does this do?  I think it just does the allocation and
       forces you to make the constructor call yourself... */
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_ThreadState *ts=env->ts;
    fivmr_Handle *result=NULL;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(ts,(fivmr_Handle*)clazz);

    fivmr_ReflectLog_alloc(ts,-1,td);

    fivmr_ThreadState_goToJava(ts);
    if (check_init(ts,td)) {
	result=fivmr_ThreadState_addHandle(
	    ts,
	    fivmr_alloc(ts,FIVMR_GC_OBJ_SPACE,td));
	fivmr_ThreadState_handlifyException(ts);
    }
    fivmr_ThreadState_goToNative(ts);
    
    return (jobject)result;
}

static jobject jni_NewObject(JNIEnv *env,jclass clazz,jmethodID method,...) {
    va_list lst;
    jobject result;
    va_start(lst,method);
    result=(*env)->NewObjectV(env,clazz,method,lst);
    va_end(lst);
    return result;
}

static jobject jni_NewObjectV(JNIEnv *env,jclass clazz,jmethodID method,va_list lst) {
    fivmr_MethodRec *mr=(fivmr_MethodRec*)method;
    jvalue *args;
    args=alloca(sizeof(jvalue)*fivmr_MethodRec_numParams(mr));
    transferArgVals(mr,args,lst);
    return (*env)->NewObjectA(env,clazz,method,args);
}

static jobject jni_NewObjectA(JNIEnv *env,jclass clazz,jmethodID method,jvalue *args) {
    jobject obj;
    obj=(*env)->AllocObject(env,clazz);
    if ((*env)->ExceptionOccurred(env)) {
	return NULL;
    }
    (*env)->CallNonvirtualVoidMethodA(env,obj,clazz,method,args);
    if ((*env)->ExceptionOccurred(env)) {
	return NULL;
    } else {
	return obj;
    }
}

static jclass jni_GetObjectClass(JNIEnv *env_,jobject obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    return (jclass)produceHandleFromNative(
	env->ts,fivmr_TypeData_forHandle((fivmr_Handle*)obj)->classObject);
}

static jboolean jni_IsInstanceOf(JNIEnv *env_,jobject obj,jclass clazz) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    jboolean result;
    fivmr_ThreadState_goToJava(env->ts);
    result=(jboolean)fivmr_TypeData_isSubtypeOf(
        env->ts,
	fivmr_TypeData_forHandle((fivmr_Handle*)obj),
	fivmr_TypeData_fromClass(env->ts,fivmr_Handle_get((fivmr_Handle*)clazz)));
    fivmr_ThreadState_goToNative(env->ts);
    return result;
}

static jmethodID jni_GetMethodID(JNIEnv *env_,
				 jclass clazz,
				 const char *name,
				 const char *sig) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    if (check_init_easy(env_,clazz)) {
	fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
	fivmr_MethodRec *result=
	    fivmr_TypeData_findInstMethod(env->ts->vm,td,name,sig);
	if (result!=NULL && fivmr_MethodRec_exists(result)) {
	    fivmr_ReflectLog_dynamicCall(env->ts,-1,result);
	    return (jmethodID)result;
	} else {
	    LOG(3,("Could not find method %s %s in %s (%p)",name,sig,td->name,td));
	    fivmr_throwNoSuchMethodError(env->ts,name,sig);
	}
    }
    return NULL;
}

static jmethodID jni_GetStaticMethodID(JNIEnv *env_,
				       jclass clazz,
				       const char *name,
				       const char *sig) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    if (check_init_easy(env_,clazz)) {
	fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
	fivmr_MethodRec *result=
	    fivmr_TypeData_findStaticMethod(env->ts->vm,td,name,sig);
	if (result!=NULL) {
	    fivmr_ReflectLog_dynamicCall(env->ts,-1,result);
	    return (jmethodID)result;
	} else {
	    LOG(3,("Could not find static method %s %s in %s (%p)",name,sig,td->name,td));
	    fivmr_throwNoSuchMethodError(env->ts,name,sig);
	}
    }
    return NULL;
}

#define MAKE_CALL_METHOD(rtype,rname,mname)				\
    static rtype jni_Call##mname##Method(JNIEnv *env,			\
					 jobject obj,			\
					 jmethodID methodID,		\
					 ...) {				\
	va_list lst;							\
	rtype result;							\
	va_start(lst,methodID);						\
	result=(rtype)virtual_call_v(methodID,env,obj,lst).rname;	\
	va_end(lst);							\
	return result;							\
    }									\
    static rtype jni_Call##mname##MethodV(JNIEnv *env,			\
					  jobject obj,			\
					  jmethodID methodID,		\
					  va_list lst) {		\
	return (rtype)virtual_call_v(methodID,env,obj,lst).rname;	\
    }									\
    static rtype jni_Call##mname##MethodA(JNIEnv *env,			\
					  jobject obj,			\
					  jmethodID methodID,		\
					  jvalue *args) {		\
	return (rtype)virtual_call(methodID,env,obj,args).rname;	\
    }

MAKE_CALL_METHOD(jobject,H,Object)
MAKE_CALL_METHOD(jboolean,B,Boolean)
MAKE_CALL_METHOD(jbyte,B,Byte)
MAKE_CALL_METHOD(jchar,C,Char)
MAKE_CALL_METHOD(jshort,S,Short)
MAKE_CALL_METHOD(jint,I,Int)
MAKE_CALL_METHOD(jlong,L,Long)
MAKE_CALL_METHOD(jfloat,F,Float)
MAKE_CALL_METHOD(jdouble,D,Double)

static void jni_CallVoidMethod(JNIEnv *env,				
			       jobject obj,			
			       jmethodID methodID,			
			       ...) {				
    va_list lst;							
    va_start(lst,methodID);						
    virtual_call_v(methodID,env,obj,lst);	
    va_end(lst);							
}									

static void jni_CallVoidMethodV(JNIEnv *env,				
				jobject obj,				
				jmethodID methodID,			
				va_list lst) {			
    virtual_call_v(methodID,env,obj,lst);	
}									

static void jni_CallVoidMethodA(JNIEnv *env,				
				jobject obj,			       
				jmethodID methodID,		       
				jvalue *args) {		       
    virtual_call(methodID,env,obj,args);
}

#define MAKE_CALL_NV_METHOD(rtype,rname,mname)				\
    static rtype jni_CallNonvirtual##mname##Method(JNIEnv *env,		\
						   jobject obj,		\
						   jclass clazz,	\
						   jmethodID methodID,	\
						   ...) {		\
	va_list lst;							\
	rtype result;							\
	va_start(lst,methodID);						\
	result=(rtype)special_call_v(methodID,env,clazz,obj,lst).rname;	\
	va_end(lst);							\
	return result;							\
    }									\
    static rtype jni_CallNonvirtual##mname##MethodV(JNIEnv *env,	\
						    jobject obj,	\
						    jclass clazz,	\
						    jmethodID methodID,	\
						    va_list lst) {	\
	return (rtype)special_call_v(methodID,env,clazz,obj,lst).rname;	\
    }									\
    static rtype jni_CallNonvirtual##mname##MethodA(JNIEnv *env,	\
						    jobject obj,	\
						    jclass clazz,	\
						    jmethodID methodID,	\
						    jvalue *args) {	\
	return (rtype)special_call(methodID,env,clazz,obj,args).rname;	\
    }

MAKE_CALL_NV_METHOD(jobject,H,Object)
MAKE_CALL_NV_METHOD(jboolean,B,Boolean)
MAKE_CALL_NV_METHOD(jbyte,B,Byte)
MAKE_CALL_NV_METHOD(jchar,C,Char)
MAKE_CALL_NV_METHOD(jshort,S,Short)
MAKE_CALL_NV_METHOD(jint,I,Int)
MAKE_CALL_NV_METHOD(jlong,L,Long)
MAKE_CALL_NV_METHOD(jfloat,F,Float)
MAKE_CALL_NV_METHOD(jdouble,D,Double)

static void jni_CallNonvirtualVoidMethod(JNIEnv *env,				
					 jobject obj,			
					 jclass clazz,
					 jmethodID methodID,			
					 ...) {				
    va_list lst;							
    va_start(lst,methodID);						
    special_call_v(methodID,env,clazz,obj,lst);	
    va_end(lst);							
}									

static void jni_CallNonvirtualVoidMethodV(JNIEnv *env,				
					  jobject obj,				
					  jclass clazz,
					  jmethodID methodID,			
					  va_list lst) {			
    special_call_v(methodID,env,clazz,obj,lst);	
}					
				
static void jni_CallNonvirtualVoidMethodA(JNIEnv *env,				
					  jobject obj,			       
					  jclass clazz,
					  jmethodID methodID,		       
					  jvalue *args) {		       
    special_call(methodID,env,clazz,obj,args);
}

#define MAKE_CALL_S_METHOD(rtype,rname,mname)				\
    static rtype jni_CallStatic##mname##Method(JNIEnv *env,		\
					       jclass clazz,		\
					       jmethodID methodID,	\
					       ...) {			\
	va_list lst;							\
	rtype result;							\
	va_start(lst,methodID);						\
	result=(rtype)static_call_v(methodID,env,lst).rname;		\
	va_end(lst);							\
	return result;							\
    }									\
    static rtype jni_CallStatic##mname##MethodV(JNIEnv *env,		\
						jclass clazz,		\
						jmethodID methodID,	\
						va_list lst) {		\
	return (rtype)static_call_v(methodID,env,lst).rname;		\
    }									\
    static rtype jni_CallStatic##mname##MethodA(JNIEnv *env,		\
						jclass clazz,		\
						jmethodID methodID,	\
						jvalue *args) {		\
	return (rtype)static_call(methodID,env,args).rname;		\
    }

MAKE_CALL_S_METHOD(jobject,H,Object)
MAKE_CALL_S_METHOD(jboolean,B,Boolean)
MAKE_CALL_S_METHOD(jbyte,B,Byte)
MAKE_CALL_S_METHOD(jchar,C,Char)
MAKE_CALL_S_METHOD(jshort,S,Short)
MAKE_CALL_S_METHOD(jint,I,Int)
MAKE_CALL_S_METHOD(jlong,L,Long)
MAKE_CALL_S_METHOD(jfloat,F,Float)
MAKE_CALL_S_METHOD(jdouble,D,Double)

static void jni_CallStaticVoidMethod(JNIEnv *env,	
				     jclass clazz,
				     jmethodID methodID,			
				     ...) {				
    va_list lst;							
    va_start(lst,methodID);						
    static_call_v(methodID,env,lst);	
    va_end(lst);							
}									

static void jni_CallStaticVoidMethodV(JNIEnv *env,				
				      jclass clazz,
				      jmethodID methodID,			
				      va_list lst) {			
    static_call_v(methodID,env,lst);	
}					
				
static void jni_CallStaticVoidMethodA(JNIEnv *env,				
				      jclass clazz,
				      jmethodID methodID,		       
				      jvalue *args) {		       
    static_call(methodID,env,args);
}

static jfieldID jni_GetFieldID(JNIEnv *env_,
			       jclass clazz,
			       const char *name,
			       const char *sig) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
    LOG(8,("from class handle %p, with obj %p, got td = %d",
	   clazz,((fivmr_Handle*)clazz)->obj,td));
    if (check_init_inNative(env->ts,td)) {
        fivmr_FieldRec *result=fivmr_TypeData_findInstField(td,name,sig);
        if (result!=NULL) {
            fivmr_ReflectLog_access(env->ts,-1,result);
            return (jfieldID)result;
	} else {
            LOG(3,("Could not find field %s %s in %s (%p)",
                   name,sig,
                   fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz)->name,
                   fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz)));
            fivmr_throwNoSuchFieldError(env->ts,name,sig);
        }
    }
    return NULL;
}

static jfieldID jni_GetStaticFieldID(JNIEnv *env_,
				     jclass clazz,
				     const char *name,
				     const char *sig) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
    if (check_init_inNative(env->ts,td)) {
        fivmr_FieldRec *result=fivmr_TypeData_findStaticField(td,name,sig);
        if (result!=NULL) {
            fivmr_ReflectLog_access(env->ts,-1,result);
            return (jfieldID)result;
	} else {
            LOG(3,("Could not find static field %s %s in %s (%p)",name,sig,td->name,td));
            fivmr_throwNoSuchMethodError(env->ts,name,sig);
        }
    }
    return NULL;
}

static jobject jni_GetObjectField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return NULL;
    } else {
	jobject result;
	fivmr_ThreadState_goToJava(ts);
	result=(jobject)
	    fivmr_ThreadState_addHandle(
		ts,
		fivmr_objectGetField(ts,
				     fivmr_Handle_get((fivmr_Handle*)obj),
				     fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                                  (fivmr_FieldRec*)fieldID),
				     ((fivmr_FieldRec*)fieldID)->flags));
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jboolean jni_GetBooleanField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jboolean result;
	fivmr_ThreadState_goToJava(ts);
	result=(jboolean)
	    fivmr_byteGetField(ts,
			       fivmr_Handle_get((fivmr_Handle*)obj),
			       fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                            (fivmr_FieldRec*)fieldID),
			       ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jbyte jni_GetByteField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jbyte result;
	fivmr_ThreadState_goToJava(ts);
	result=(jbyte)
	    fivmr_byteGetField(ts,
			       fivmr_Handle_get((fivmr_Handle*)obj),
			       fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                            (fivmr_FieldRec*)fieldID),
			       ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jchar jni_GetCharField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jchar result;
	fivmr_ThreadState_goToJava(ts);
	result=(jchar)
	    fivmr_charGetField(ts,
			       fivmr_Handle_get((fivmr_Handle*)obj),
			       fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                            (fivmr_FieldRec*)fieldID),
			       ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jshort jni_GetShortField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jshort result;
	fivmr_ThreadState_goToJava(ts);
	result=(jshort)
	    fivmr_shortGetField(ts,
				fivmr_Handle_get((fivmr_Handle*)obj),
				fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                             (fivmr_FieldRec*)fieldID),
				((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jint jni_GetIntField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jint result;
	fivmr_ThreadState_goToJava(ts);
	result=(jint)
	    fivmr_intGetField(ts,
			      fivmr_Handle_get((fivmr_Handle*)obj),
			      fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                           (fivmr_FieldRec*)fieldID),
			      ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jlong jni_GetLongField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jlong result;
	fivmr_ThreadState_goToJava(ts);
	result=(jlong)
	    fivmr_longGetField(ts,
			       fivmr_Handle_get((fivmr_Handle*)obj),
			       fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                            (fivmr_FieldRec*)fieldID),
			       ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jfloat jni_GetFloatField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0.0;
    } else {
	jfloat result;
	fivmr_ThreadState_goToJava(ts);
	result=(jfloat)
	    fivmr_floatGetField(ts,
				fivmr_Handle_get((fivmr_Handle*)obj),
				fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                             (fivmr_FieldRec*)fieldID),
				((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jdouble jni_GetDoubleField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0.0;
    } else {
	jdouble result;
	fivmr_ThreadState_goToJava(ts);
	result=(jdouble)
	    fivmr_doubleGetField(ts,
				 fivmr_Handle_get((fivmr_Handle*)obj),
				 fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                              (fivmr_FieldRec*)fieldID),
				 ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static void jni_SetObjectField(JNIEnv *env,
			       jobject obj,
			       jfieldID fieldID,
			       jobject value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_objectPutField(ts,
			     fivmr_Handle_get((fivmr_Handle*)obj),
			     fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                          (fivmr_FieldRec*)fieldID),
			     fivmr_Handle_get((fivmr_Handle*)value),
			     ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetBooleanField(JNIEnv *env,
				jobject obj,
				jfieldID fieldID,
				jboolean value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_bytePutField(ts,
			   fivmr_Handle_get((fivmr_Handle*)obj),
			   fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                        (fivmr_FieldRec*)fieldID),
			   value,
			   ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetByteField(JNIEnv *env,
			     jobject obj,
			     jfieldID fieldID,
			     jbyte value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_bytePutField(ts,
			   fivmr_Handle_get((fivmr_Handle*)obj),
			   fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                        (fivmr_FieldRec*)fieldID),
			   value,
			   ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetCharField(JNIEnv *env,
			     jobject obj,
			     jfieldID fieldID,
			     jchar value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_charPutField(ts,
			   fivmr_Handle_get((fivmr_Handle*)obj),
			   fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                        (fivmr_FieldRec*)fieldID),
			   value,
			   ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetShortField(JNIEnv *env,
			      jobject obj,
			      jfieldID fieldID,
			      jshort value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_shortPutField(ts,
			    fivmr_Handle_get((fivmr_Handle*)obj),
			    fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                         (fivmr_FieldRec*)fieldID),
			    value,
			    ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetIntField(JNIEnv *env,
			    jobject obj,
			    jfieldID fieldID,
			    jint value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_intPutField(ts,
			  fivmr_Handle_get((fivmr_Handle*)obj),
			  fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                       (fivmr_FieldRec*)fieldID),
			  value,
			  ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetLongField(JNIEnv *env,
			     jobject obj,
			     jfieldID fieldID,
			     jlong value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_longPutField(ts,
			   fivmr_Handle_get((fivmr_Handle*)obj),
			   fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                        (fivmr_FieldRec*)fieldID),
			   value,
			   ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetFloatField(JNIEnv *env,
			      jobject obj,
			      jfieldID fieldID,
			      jfloat value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_floatPutField(ts,
			    fivmr_Handle_get((fivmr_Handle*)obj),
			    fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                         (fivmr_FieldRec*)fieldID),
			    value,
			    ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetDoubleField(JNIEnv *env,
			       jobject obj,
			       jfieldID fieldID,
			       jdouble value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_doublePutField(ts,
			     fivmr_Handle_get((fivmr_Handle*)obj),
			     fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                          (fivmr_FieldRec*)fieldID),
			     value,
			     ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static jobject jni_GetStaticObjectField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_FieldRec *fr=(fivmr_FieldRec*)fieldID;
    jobject result=NULL;
    fivmr_ThreadState_goToJava(ts);
    if (check_init(ts,((fivmr_FieldRec*)fieldID)->owner)) {
	result=(jobject)
	    fivmr_ThreadState_addHandle(
		ts,
		fivmr_objectGetStatic(
		    ts,
		    (fivmr_Object*)fivmr_FieldRec_staticFieldAddress(ts->vm,fr),
		    fr->flags));
    }
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static void jni_SetStaticObjectField(JNIEnv *env,
				     jclass clazz,
				     jfieldID fieldID,
				     jobject value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ThreadState_goToJava(ts);
    if (check_init(ts,((fivmr_FieldRec*)fieldID)->owner)) {
	fivmr_objectPutStatic(
	    ts,
	    (fivmr_Object*)fivmr_FieldRec_staticFieldAddress(
                ts->vm,
		(fivmr_FieldRec*)fieldID),
	    fivmr_Handle_get((fivmr_Handle*)value),
	    0);
    }
    fivmr_ThreadState_goToNative(ts);
}

static jboolean jni_GetStaticBooleanField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jboolean*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jbyte jni_GetStaticByteField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jbyte*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jchar jni_GetStaticCharField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jchar*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jshort jni_GetStaticShortField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jshort*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jint jni_GetStaticIntField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jint*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jlong jni_GetStaticLongField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jlong*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jfloat jni_GetStaticFloatField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jfloat*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0.0;
    }
}

static jdouble jni_GetStaticDoubleField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jdouble*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0.0;
    }
}

/* FIXME: convert these to use C89-compliant lvalues. */

static void jni_SetStaticBooleanField(JNIEnv *env,
				      jclass clazz,
				      jfieldID fieldID,
				      jboolean value) {
    if (check_init_easy(env,clazz)) {
	*((jboolean*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticByteField(JNIEnv *env,
				   jclass clazz,
				   jfieldID fieldID,
				   jbyte value) {
    if (check_init_easy(env,clazz)) {
	*((jbyte*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticCharField(JNIEnv *env,
				   jclass clazz,
				   jfieldID fieldID,
				   jchar value) {
    if (check_init_easy(env,clazz)) {
	*((jchar*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticShortField(JNIEnv *env,
				    jclass clazz,
				    jfieldID fieldID,
				    jshort value) {
    if (check_init_easy(env,clazz)) {
	*((jshort*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticIntField(JNIEnv *env,
				  jclass clazz,
				  jfieldID fieldID,
				  jint value) {
    if (check_init_easy(env,clazz)) {
	*((jint*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticLongField(JNIEnv *env,
				   jclass clazz,
				   jfieldID fieldID,
				   jlong value) {
    if (check_init_easy(env,clazz)) {
	*((jlong*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticFloatField(JNIEnv *env,
				    jclass clazz,
				    jfieldID fieldID,
				    jfloat value) {
    if (check_init_easy(env,clazz)) {
	*((jfloat*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticDoubleField(JNIEnv *env,
				     jclass clazz,
				     jfieldID fieldID,
				     jdouble value) {
    if (check_init_easy(env,clazz)) {
	*((jdouble*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static jstring jni_NewString(JNIEnv *env,
			     const jchar *unicodeChars,
			     jsize len) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jstring)fivmr_fromUTF16Sequence(ts,unicodeChars,len);
}

static jsize jni_GetStringLength(JNIEnv *env,jstring str) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return fivmr_stringLength(ts,(fivmr_Handle*)str);
}

static const jchar *jni_GetStringChars(JNIEnv *env,jstring string,jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return fivmr_getUTF16Sequence(ts,(fivmr_Handle*)string);
}

static void jni_ReleaseStringChars(JNIEnv *env,jstring string,const jchar *chars) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnBuffer(ts,(void*)(uintptr_t)chars);
}

static jstring jni_NewStringUTF(JNIEnv *env,
				const char *bytes) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jstring)fivmr_fromCStringFull(ts,bytes);
}

static jsize jni_GetStringUTFLength(JNIEnv *env,jstring str) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return fivmr_cstringLength(ts,(fivmr_Handle*)str);
}

static const char *jni_GetStringUTFChars(JNIEnv *env,
					  jstring string,
					  jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (char*)fivmr_getCStringFull(ts,(fivmr_Handle*)string);
}

static void jni_ReleaseStringUTFChars(JNIEnv *env,jstring string,const char *utf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnBuffer(ts,(void*)(uintptr_t)utf);
}

static jsize jni_GetArrayLength(JNIEnv *env,jarray array) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    int32_t result=0;
    fivmr_ThreadState_goToJava(ts);
    if (array==NULL) {
	fivmr_throwNullPointerRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	fivmr_Object arrObj=fivmr_Handle_get((fivmr_Handle*)array);
	fivmr_TypeData *arrTD=fivmr_TypeData_forObject(&ts->vm->settings,arrObj);
	if (arrTD->name[0]!='[') {
	    fivmr_throwClassCastRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else {
	    result=fivmr_arrayLength(ts,fivmr_Handle_get((fivmr_Handle*)array),0);
	}
    }
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static jobjectArray jni_NewObjectArray(JNIEnv *env,
				       jsize length,
				       jclass elementClass,
				       jobject initialElement) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_TypeData *td;
    fivmr_Object arr;
    fivmr_Object ele;
    fivmr_Handle *result=NULL;
    int32_t n;
    fivmr_ThreadState_goToJava(ts);
    if (length<0) {
	fivmr_throwNegativeSizeRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	td=fivmr_TypeData_makeArray(
            fivmr_TypeData_fromClass(ts,fivmr_Handle_get((fivmr_Handle*)elementClass)));
	fivmr_ReflectLog_alloc(ts,-1,td);
	ele=fivmr_Handle_get((fivmr_Handle*)initialElement);
	arr=fivmr_allocArray(ts,FIVMR_GC_OBJ_SPACE,td,length);
	for (n=length;n-->0;) {
	    fivmr_objectArrayStore(ts,arr,n,ele,0);
	}
	result=fivmr_ThreadState_addHandle(ts,arr);
    }
    fivmr_ThreadState_goToNative(ts);
    return (jobjectArray)result;
}

static jobject jni_GetObjectArrayElement(JNIEnv *env,
					 jobjectArray array,
					 jsize index) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Handle *result=NULL;
    fivmr_ThreadState_goToJava(ts);
    if (array==NULL) {
	fivmr_throwNullPointerRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	fivmr_Object arrObj=fivmr_Handle_get((fivmr_Handle*)array);
	fivmr_TypeData *arrTD=fivmr_TypeData_forObject(&ts->vm->settings,arrObj);
	if (arrTD->name[0]!='[' ||
	    arrTD->name[1]!='L') {
	    fivmr_throwClassCastRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else if ((uint32_t)index>=(uint32_t)fivmr_arrayLength(ts,arrObj,0)) {
	    fivmr_throwArrayBoundsRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else {
	    result=fivmr_ThreadState_addHandle(
		ts,fivmr_objectArrayLoad(ts,arrObj,index,0));
	}
    }
    fivmr_ThreadState_goToNative(ts);
    return (jobject)result;
}

static void jni_SetObjectArrayElement(JNIEnv *env,
				      jobjectArray array,
				      jsize index,
				      jobject value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ThreadState_goToJava(ts);
    if (array==NULL) {
	fivmr_throwNullPointerRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	fivmr_Object arrObj=fivmr_Handle_get((fivmr_Handle*)array);
	fivmr_Object eleObj=fivmr_Handle_get((fivmr_Handle*)value);
	fivmr_TypeData *arrTD=fivmr_TypeData_forObject(&ts->vm->settings,arrObj);
	if (arrTD->name[0]!='[' ||
	    arrTD->name[1]!='L') {
	    fivmr_throwClassCastRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else if ((uint32_t)index>=(uint32_t)fivmr_arrayLength(ts,arrObj,0)) {
	    fivmr_throwArrayBoundsRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else if (eleObj!=0 &&
		   !fivmr_TypeData_isSubtypeOf(
                       ts,
		       fivmr_TypeData_forObject(&ts->vm->settings,eleObj),
		       arrTD->arrayElement)) {
	    fivmr_throwArrayStoreRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else {
	    fivmr_objectArrayStore(ts,arrObj,index,eleObj,0);
	}
    }
    fivmr_ThreadState_goToNative(ts);
}

static jbooleanArray jni_NewBooleanArray(JNIEnv *env,jsize length) {
    return (jbooleanArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_booleanArr,length);
}

static jbyteArray jni_NewByteArray(JNIEnv *env,jsize length) {
    return (jbyteArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_byteArr,length);
}

static jcharArray jni_NewCharArray(JNIEnv *env,jsize length) {
    return (jcharArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_charArr,length);
}

static jshortArray jni_NewShortArray(JNIEnv *env,jsize length) {
    return (jshortArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_shortArr,length);
}

static jintArray jni_NewIntArray(JNIEnv *env,jsize length) {
    return (jintArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_intArr,length);
}

static jlongArray jni_NewLongArray(JNIEnv *env,jsize length) {
    return (jlongArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_longArr,length);
}

static jfloatArray jni_NewFloatArray(JNIEnv *env,jsize length) {
    return (jfloatArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_floatArr,length);
}

static jdoubleArray jni_NewDoubleArray(JNIEnv *env,jsize length) {
    return (jdoubleArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_doubleArr,length);
}

static jboolean *jni_GetBooleanArrayElements(JNIEnv *env,
					     jbooleanArray array,
					     jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jboolean*)fivmr_getBooleanElements(ts,(fivmr_Handle*)array);
}

static jbyte *jni_GetByteArrayElements(JNIEnv *env,
				       jbyteArray array,
				       jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jbyte*)fivmr_getByteElements(ts,(fivmr_Handle*)array);
}

static jchar *jni_GetCharArrayElements(JNIEnv *env,
				       jcharArray array,
				       jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jchar*)fivmr_getCharElements(ts,(fivmr_Handle*)array);
}

static jshort *jni_GetShortArrayElements(JNIEnv *env,
					 jshortArray array,
					 jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jshort*)fivmr_getShortElements(ts,(fivmr_Handle*)array);
}

static jint *jni_GetIntArrayElements(JNIEnv *env,
				     jintArray array,
				     jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jint*)fivmr_getIntElements(ts,(fivmr_Handle*)array);
}

static jlong *jni_GetLongArrayElements(JNIEnv *env,
				       jlongArray array,
				       jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jlong*)fivmr_getLongElements(ts,(fivmr_Handle*)array);
}

static jfloat *jni_GetFloatArrayElements(JNIEnv *env,
					 jfloatArray array,
					 jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jfloat*)fivmr_getFloatElements(ts,(fivmr_Handle*)array);
}

static jdouble *jni_GetDoubleArrayElements(JNIEnv *env,
					   jdoubleArray array,
					   jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jdouble*)fivmr_getDoubleElements(ts,(fivmr_Handle*)array);
}

static void jni_ReleaseBooleanArrayElements(JNIEnv *env,
					    jbooleanArray array,
					    jboolean *elems,
					    jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnBooleanElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseByteArrayElements(JNIEnv *env,
					 jbooleanArray array,
					 jboolean *elems,
					 jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnByteElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseCharArrayElements(JNIEnv *env,
					 jcharArray array,
					 jchar *elems,
					 jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnCharElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseShortArrayElements(JNIEnv *env,
					  jshortArray array,
					  jshort *elems,
					  jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnShortElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseIntArrayElements(JNIEnv *env,
					jintArray array,
					jint *elems,
					jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnIntElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseLongArrayElements(JNIEnv *env,
					 jlongArray array,
					 jlong *elems,
					 jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnLongElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseFloatArrayElements(JNIEnv *env,
					  jfloatArray array,
					  jfloat *elems,
					  jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnFloatElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseDoubleArrayElements(JNIEnv *env,
					   jdoubleArray array,
					   jdouble *elems,
					   jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnDoubleElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_GetBooleanArrayRegion(JNIEnv *env,
				      jbooleanArray array,
				      jsize start,
				      jsize len,
				      jboolean *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getBooleanRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetByteArrayRegion(JNIEnv *env,
				   jbyteArray array,
				   jsize start,
				   jsize len,
				   jbyte *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getByteRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetCharArrayRegion(JNIEnv *env,
				   jcharArray array,
				   jsize start,
				   jsize len,
				   jchar *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getCharRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetShortArrayRegion(JNIEnv *env,
				    jshortArray array,
				    jsize start,
				    jsize len,
				    jshort *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getShortRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetIntArrayRegion(JNIEnv *env,
				  jintArray array,
				  jsize start,
				  jsize len,
				  jint *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getIntRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetLongArrayRegion(JNIEnv *env,
				   jlongArray array,
				   jsize start,
				   jsize len,
				   jlong *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getLongRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetFloatArrayRegion(JNIEnv *env,
				    jfloatArray array,
				    jsize start,
				    jsize len,
				    jfloat *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getFloatRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetDoubleArrayRegion(JNIEnv *env,
				     jdoubleArray array,
				     jsize start,
				     jsize len,
				     jdouble *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getDoubleRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetBooleanArrayRegion(JNIEnv *env,
				      jbooleanArray array,
				      jsize start,
				      jsize len,
				      jboolean *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setBooleanRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetByteArrayRegion(JNIEnv *env,
				   jbyteArray array,
				   jsize start,
				   jsize len,
				   jbyte *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setByteRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetCharArrayRegion(JNIEnv *env,
				   jcharArray array,
				   jsize start,
				   jsize len,
				   jchar *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setCharRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetShortArrayRegion(JNIEnv *env,
				    jshortArray array,
				    jsize start,
				    jsize len,
				    jshort *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setShortRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetIntArrayRegion(JNIEnv *env,
				  jintArray array,
				  jsize start,
				  jsize len,
				  jint *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setIntRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetLongArrayRegion(JNIEnv *env,
				   jlongArray array,
				   jsize start,
				   jsize len,
				   jlong *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setLongRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetFloatArrayRegion(JNIEnv *env,
				    jfloatArray array,
				    jsize start,
				    jsize len,
				    jfloat *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setFloatRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetDoubleArrayRegion(JNIEnv *env,
				     jdoubleArray array,
				     jsize start,
				     jsize len,
				     jdouble *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setDoubleRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static jint jni_RegisterNatives(JNIEnv *env,
				jclass clazz,
				const JNINativeMethod *methods,
				jint nmethods) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(ts,(fivmr_Handle*)clazz);
    int32_t i;
    jint result=0;
    for (i=0;i<nmethods;++i) {
	fivmr_MethodRec *mr=fivmr_TypeData_findMethod(ts->vm,
                                                      td,
						      methods[i].name,
						      methods[i].signature);
	if (mr==NULL ||
	    (mr->flags&FIVMR_MBF_METHOD_IMPL)!=FIVMR_MBF_JNI) {
	    LOG(1,("Failed to register method %s %s on class %s.",
		   methods[i].name,methods[i].signature,td->name));
	    /* FIXME: what if the method got axed by 0CFA? */
	    result=-1;
	} else {
	    mr->codePtr=methods[i].fnPtr;
	    LOG(1,("Registered method %s %s on class %s (%p).",
		   methods[i].name,methods[i].signature,td->name,methods[i].fnPtr));
	}
    }
    return 0;
}

static jint jni_UnregisterNatives(JNIEnv *env,
				  jclass clazz) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(ts,(fivmr_Handle*)clazz);
    int32_t i;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if ((mr->flags&FIVMR_MBF_METHOD_IMPL)==FIVMR_MBF_JNI) {
            td->methods[i]->codePtr=NULL;
        }
    }
    return 0;
}

static jint jni_MonitorEnter(JNIEnv *env,jobject obj) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ThreadState_goToJava(ts);
    fivmr_Monitor_lock_slow(
	fivmr_ObjHeader_forObject(
            &ts->vm->settings,
	    fivmr_Handle_get((fivmr_Handle*)obj)),
	ts);
    fivmr_ThreadState_goToNative(ts);
    return 0;
}

static jint jni_MonitorExit(JNIEnv *env,jobject obj) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ThreadState_goToJava(ts);
    fivmr_Monitor_unlock_slow(
	fivmr_ObjHeader_forObject(
            &ts->vm->settings,
	    fivmr_Handle_get((fivmr_Handle*)obj)),
	ts);
    fivmr_ThreadState_goToNative(ts);
    return 0;
}

static jint jni_GetJavaVM(JNIEnv *env,JavaVM **vm) {
    *vm=(JavaVM*) &((fivmr_JNIEnv*)env)->ctx->jniVM;
    return 0;
}

static void jni_GetStringRegion(JNIEnv *env,
				jstring str,
				jsize start,
				jsize len,
				jchar *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getStringRegion(ts,(fivmr_Handle*)str,start,len,buf);
}

/* NEVER USE THIS.  JNI mandates it but that's because it's dumb.  it's a buffer
   overflow waiting to happen. */
static void jni_GetStringUTFRegion(JNIEnv *env,
				   jstring str,
				   jsize start,
				   jsize len,
				   char *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getStringUTFRegion(ts,(fivmr_Handle*)str,start,len,buf);
}

static void *jni_GetPrimitiveArrayCritical(JNIEnv *env,jarray array,jboolean *isCopy) {
    /* FIXME: this is specific to our current way of handling arrays.  this will
       have to be changed once we have arraylets. */
    /* FIXME FIX FIX FIX!!! */
    if (isCopy) *isCopy=false;
    return (void*)((fivmr_Handle*)array)->obj;
}

static void jni_ReleasePrimitiveArrayCritical(JNIEnv *env,
					      jarray array,
					      void *carray,
					      jint mode) {
    /* do nothing. */
    /* FIXME: see above though */
}

static const jchar *jni_GetStringCritical(JNIEnv *env,
					  jstring string,
					  jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (FIVMR_OM_CONTIGUOUS(&ts->vm->settings)) {
        if (isCopy) *isCopy=false;
        return fivmr_String_getArrayPointer(ts,(fivmr_Handle*)string)
            + fivmr_String_getOffset(ts,(fivmr_Handle*)string);
    } else {
        if (isCopy) *isCopy=true;
        return fivmr_getUTF16Sequence(ts,(fivmr_Handle*)string);
    }
}

static void jni_ReleaseStringCritical(JNIEnv *env,
				      jstring str,
				      const jchar *carray) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (FIVMR_OM_CONTIGUOUS(&ts->vm->settings)) {
        /* do nothing. */
    } else {
        fivmr_returnBuffer(ts,(void*)(uintptr_t)carray);
    }
}

static jweak jni_NewWeakGlobalRef(JNIEnv *env,jobject obj) {
    return NULL; /* FIXME actually implement this at some point. */
}

static void jni_DeleteWeakGlobalRef(JNIEnv *env,jweak obj) {
    /* do nothing for now */
    /* FIXME actually implement this at some point. */
}

static jboolean jni_ExceptionCheck(JNIEnv *env) {
    return ((fivmr_JNIEnv*)env)->ts->curExceptionHandle!=NULL;
}

static jobject jni_NewDirectByteBuffer(JNIEnv *env,void *address,jlong capacity) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jobject)fivmr_DirectByteBuffer_wrap(ts,
                                                (uintptr_t)address,
						(int32_t)capacity,
						(int32_t)capacity,
						0);
}

static void *jni_GetDirectBufferAddress(JNIEnv *env,jobject buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (void*)fivmr_DirectByteBuffer_address(ts,(fivmr_Handle*)buf);
}

static jlong jni_GetDirectBufferCapacity(JNIEnv *env,jobject buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jlong)fivmr_DirectByteBuffer_capacity(ts,(fivmr_Handle*)buf);
}

struct JNINativeInterface fivmr_jniFunctions={
    NULL,
    NULL,
    NULL,
    NULL,
    jni_GetVersion,
    jni_DefineClass,
    jni_FindClass,
    NULL,
    NULL,
    NULL,
    jni_GetSuperclass,
    jni_IsAssignableFrom,
    NULL,
    jni_Throw,
    jni_ThrowNew,
    jni_ExceptionOccurred,
    jni_ExceptionDescribe,
    jni_ExceptionClear,
    jni_FatalError,
    NULL,
    NULL,
    jni_NewGlobalRef,
    jni_DeleteGlobalRef,
    jni_DeleteLocalRef,
    jni_IsSameObject,
    NULL,
    NULL,
    jni_AllocObject,
    jni_NewObject,
    jni_NewObjectA,
    jni_NewObjectV,
    jni_GetObjectClass,
    jni_IsInstanceOf,
    jni_GetMethodID,
    jni_CallObjectMethod,
    jni_CallObjectMethodA,
    jni_CallObjectMethodV,
    jni_CallBooleanMethod,
    jni_CallBooleanMethodA,
    jni_CallBooleanMethodV,
    jni_CallByteMethod,
    jni_CallByteMethodA,
    jni_CallByteMethodV,
    jni_CallCharMethod,
    jni_CallCharMethodA,
    jni_CallCharMethodV,
    jni_CallShortMethod,
    jni_CallShortMethodA,
    jni_CallShortMethodV,
    jni_CallIntMethod,
    jni_CallIntMethodA,
    jni_CallIntMethodV,
    jni_CallLongMethod,
    jni_CallLongMethodA,
    jni_CallLongMethodV,
    jni_CallFloatMethod,
    jni_CallFloatMethodA,
    jni_CallFloatMethodV,
    jni_CallDoubleMethod,
    jni_CallDoubleMethodA,
    jni_CallDoubleMethodV,
    jni_CallVoidMethod,
    jni_CallVoidMethodA,
    jni_CallVoidMethodV,
    jni_CallNonvirtualObjectMethod,
    jni_CallNonvirtualObjectMethodA,
    jni_CallNonvirtualObjectMethodV,
    jni_CallNonvirtualBooleanMethod,
    jni_CallNonvirtualBooleanMethodA,
    jni_CallNonvirtualBooleanMethodV,
    jni_CallNonvirtualByteMethod,
    jni_CallNonvirtualByteMethodA,
    jni_CallNonvirtualByteMethodV,
    jni_CallNonvirtualCharMethod,
    jni_CallNonvirtualCharMethodA,
    jni_CallNonvirtualCharMethodV,
    jni_CallNonvirtualShortMethod,
    jni_CallNonvirtualShortMethodA,
    jni_CallNonvirtualShortMethodV,
    jni_CallNonvirtualIntMethod,
    jni_CallNonvirtualIntMethodA,
    jni_CallNonvirtualIntMethodV,
    jni_CallNonvirtualLongMethod,
    jni_CallNonvirtualLongMethodA,
    jni_CallNonvirtualLongMethodV,
    jni_CallNonvirtualFloatMethod,
    jni_CallNonvirtualFloatMethodA,
    jni_CallNonvirtualFloatMethodV,
    jni_CallNonvirtualDoubleMethod,
    jni_CallNonvirtualDoubleMethodA,
    jni_CallNonvirtualDoubleMethodV,
    jni_CallNonvirtualVoidMethod,
    jni_CallNonvirtualVoidMethodA,
    jni_CallNonvirtualVoidMethodV,
    jni_GetFieldID,
    jni_GetObjectField,
    jni_GetBooleanField,
    jni_GetByteField,
    jni_GetCharField,
    jni_GetShortField,
    jni_GetIntField,
    jni_GetLongField,
    jni_GetFloatField,
    jni_GetDoubleField,
    jni_SetObjectField,
    jni_SetBooleanField,
    jni_SetByteField,
    jni_SetCharField,
    jni_SetShortField,
    jni_SetIntField,
    jni_SetLongField,
    jni_SetFloatField,
    jni_SetDoubleField,
    jni_GetStaticMethodID,
    jni_CallStaticObjectMethod,
    jni_CallStaticObjectMethodA,
    jni_CallStaticObjectMethodV,
    jni_CallStaticBooleanMethod,
    jni_CallStaticBooleanMethodA,
    jni_CallStaticBooleanMethodV,
    jni_CallStaticByteMethod,
    jni_CallStaticByteMethodA,
    jni_CallStaticByteMethodV,
    jni_CallStaticCharMethod,
    jni_CallStaticCharMethodA,
    jni_CallStaticCharMethodV,
    jni_CallStaticShortMethod,
    jni_CallStaticShortMethodA,
    jni_CallStaticShortMethodV,
    jni_CallStaticIntMethod,
    jni_CallStaticIntMethodA,
    jni_CallStaticIntMethodV,
    jni_CallStaticLongMethod,
    jni_CallStaticLongMethodA,
    jni_CallStaticLongMethodV,
    jni_CallStaticFloatMethod,
    jni_CallStaticFloatMethodA,
    jni_CallStaticFloatMethodV,
    jni_CallStaticDoubleMethod,
    jni_CallStaticDoubleMethodA,
    jni_CallStaticDoubleMethodV,
    jni_CallStaticVoidMethod,
    jni_CallStaticVoidMethodA,
    jni_CallStaticVoidMethodV,
    jni_GetStaticFieldID,
    jni_GetStaticObjectField,
    jni_GetStaticBooleanField,
    jni_GetStaticByteField,
    jni_GetStaticCharField,
    jni_GetStaticShortField,
    jni_GetStaticIntField,
    jni_GetStaticLongField,
    jni_GetStaticFloatField,
    jni_GetStaticDoubleField,
    jni_SetStaticObjectField,
    jni_SetStaticBooleanField,
    jni_SetStaticByteField,
    jni_SetStaticCharField,
    jni_SetStaticShortField,
    jni_SetStaticIntField,
    jni_SetStaticLongField,
    jni_SetStaticFloatField,
    jni_SetStaticDoubleField,
    jni_NewString,
    jni_GetStringLength,
    jni_GetStringChars,
    jni_ReleaseStringChars,
    jni_NewStringUTF,
    jni_GetStringUTFLength,
    jni_GetStringUTFChars,
    jni_ReleaseStringUTFChars,
    jni_GetArrayLength,
    jni_NewObjectArray,
    jni_GetObjectArrayElement,
    jni_SetObjectArrayElement,
    jni_NewBooleanArray,
    jni_NewByteArray,
    jni_NewCharArray,
    jni_NewShortArray,
    jni_NewIntArray,
    jni_NewLongArray,
    jni_NewFloatArray,
    jni_NewDoubleArray,
    jni_GetBooleanArrayElements,
    jni_GetByteArrayElements,
    jni_GetCharArrayElements,
    jni_GetShortArrayElements,
    jni_GetIntArrayElements,
    jni_GetLongArrayElements,
    jni_GetFloatArrayElements,
    jni_GetDoubleArrayElements,
    jni_ReleaseBooleanArrayElements,
    jni_ReleaseByteArrayElements,
    jni_ReleaseCharArrayElements,
    jni_ReleaseShortArrayElements,
    jni_ReleaseIntArrayElements,
    jni_ReleaseLongArrayElements,
    jni_ReleaseFloatArrayElements,
    jni_ReleaseDoubleArrayElements,
    jni_GetBooleanArrayRegion,
    jni_GetByteArrayRegion,
    jni_GetCharArrayRegion,
    jni_GetShortArrayRegion,
    jni_GetIntArrayRegion,
    jni_GetLongArrayRegion,
    jni_GetFloatArrayRegion,
    jni_GetDoubleArrayRegion,
    jni_SetBooleanArrayRegion,
    jni_SetByteArrayRegion,
    jni_SetCharArrayRegion,
    jni_SetShortArrayRegion,
    jni_SetIntArrayRegion,
    jni_SetLongArrayRegion,
    jni_SetFloatArrayRegion,
    jni_SetDoubleArrayRegion,
    jni_RegisterNatives,
    jni_UnregisterNatives,
    jni_MonitorEnter,
    jni_MonitorExit,
    jni_GetJavaVM,
    jni_GetStringRegion,
    jni_GetStringUTFRegion,
    jni_GetPrimitiveArrayCritical,
    jni_ReleasePrimitiveArrayCritical,
    jni_GetStringCritical,
    jni_ReleaseStringCritical,
    jni_NewWeakGlobalRef,
    jni_DeleteWeakGlobalRef,
    jni_ExceptionCheck,
    jni_NewDirectByteBuffer,
    jni_GetDirectBufferAddress,
    jni_GetDirectBufferCapacity
};

#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
fivmr_JNILib *fivmr_JNI_libraries;
fivmr_Lock fivmr_JNI_libLock;
#endif

/* FIXME: maybe this should be run IN_NATIVE? */
void *fivmr_JNI_lookup(fivmr_ThreadState *ts,
                       fivmr_MethodRec *mr) {
#if !FIVMR_STATIC_JNI && FIVMR_DYN_LOADING
    /* FIXME: synchronization?  probably doesn't matter too much for now...  but
       you could get a weird situation if you have multiple threads doing resolution
       and library loading. */
    void *result;
    LOG(3,("Performing lookup of native method %s.",
	   fivmr_MethodRec_describe(mr)));
    fivmr_assert((mr->flags&FIVMR_MBF_METHOD_IMPL)==FIVMR_MBF_JNI);
    
    result=fivmr_JNILib_lookup(ts,fivmr_TypeData_getContext(mr->owner)->jniLibraries,mr);
    
    if (!(mr->flags&FIVMR_MBF_DYNAMIC)) {
        mr->codePtr=result;
    } /* else in the dynamic case we have no need to do caching since we are
         generating the trampolines on the fly */
    
    if (result!=NULL) {
	LOG(1,("Lookup of %s successful: %p",
	       fivmr_MethodRec_describe(mr),result));
	if ((ts->vm->flags&FIVMR_VMF_JNI_COVERAGE)) {
	    fivmr_Log_lockedPrintf("fivmr JNI coverage: %s\n",
				   fivmr_MethodRec_describe(mr));
	}
    } else {
	/* NOTE: at this point we may have an exception pending! */
	LOG(1,("Lookup of %s failed.",
	       fivmr_MethodRec_describe(mr)));
    }
    return result;
#else
    fivmr_assert(false);
#endif
}

bool fivmr_JNI_runOnLoad(fivmr_ThreadState *ts,
                         fivmr_TypeContext *ctx,
                         void *onLoadHook) {
    ((jint (*)(JavaVM *,void *))onLoadHook)((JavaVM*) &ctx->jniVM,NULL);
    return ts->curExceptionHandle==NULL;
}

bool fivmr_JNI_loadLibrary(fivmr_ThreadState *ts,
                           fivmr_TypeContext *ctx,
                           const char *filename) {
#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
    fivmr_JNILib *lib;
    bool freshlyLoaded;
    if (!fivmr_Lock_legalToAcquire(&ctx->jniLibLock)) {
	/* interrupt handlers, etc, cannot acquire this lock. */
	return false;
    }
    LOG(2,("Attempting to load library %s.",filename));
    fivmr_Lock_lock(&ctx->jniLibLock);
    fivmr_JNILib_load(ts,
                      &ctx->jniLibraries,
		      filename,
		      &lib,
		      &freshlyLoaded);
    fivmr_Lock_unlock(&ctx->jniLibLock);
    
    /* FIXME: the sync here is begging for priority inversion. */
    
    if (freshlyLoaded) {
	LOG(1,("Successfully loaded native library %s.",filename));
	if (lib->onLoadHook!=NULL) {
	    /* FIXME: do something with the version number. */
	    LOG(2,("Calling on-load hook for %s.",filename));
	    if (fivmr_JNI_runOnLoad(ts,ctx,lib->onLoadHook)) {
		LOG(2,("Returned from on-load hook for %s, library is ready.",
                       filename));
	    } else {
		LOG(2,("On-load hook for %s threw exception.",filename));
		return false;
	    }
	} else {
            LOG(2,("No on-load hook for %s, library is ready.",filename));
        }
        fivmr_Lock_lock(&ctx->jniLibLock);
        lib->initialized=true; /* now the library can be used. */
        fivmr_Lock_broadcast(&ctx->jniLibLock);
        fivmr_Lock_unlock(&ctx->jniLibLock);
    } else if (lib!=NULL) {
	LOG(2,("%s was already previously loaded.",filename));
	fivmr_Lock_lock(&ctx->jniLibLock);
	while (!lib->initialized) {
	    LOG(2,("waiting on %s to be initialized.",filename));
	    fivmr_Lock_wait(&ctx->jniLibLock);
	}
	fivmr_Lock_unlock(&ctx->jniLibLock);
    } else {
	LOG(2,("Failed to load %s.",filename));
    }
    return lib!=NULL;
#else
    fivmr_assert(false);
#endif
}

void fivmr_JNI_init(fivmr_TypeContext *ctx,
                    fivmr_VM *vm) {
#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
    fivmr_Lock_init(&ctx->jniLibLock,fivmr_Priority_bound(FIVMR_PR_MAX,
                                                          vm->maxPriority));
#endif
    ctx->jniVM.ctx=ctx;
    ctx->jniVM.functions=&jni_invoke_functions;
}

void fivmr_JNI_destroy(fivmr_TypeContext *ctx) {
#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
    fivmr_Lock_destroy(&ctx->jniLibLock);
#endif
    
    /* FIXME delete more stuff, if JNI loading actually occurred */
}


