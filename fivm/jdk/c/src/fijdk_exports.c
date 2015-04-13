/*
 * fijdk_exports.c
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

#include "fivmr_jni.h"
#include "fivmr.h"
#include "fijdk.h"
#include "fijdk_jdkruntime.h"

/* FIXME #2: figure out how to start an OpenJDK-based JVM! */

/* OK: here's how it'll work.  we'll need a VM harness that loads libjava.so
   explicitly, associates it with the root type context, and fires the OnLoad
   once we have the boot thread. */

jint JVM_IHashCode(JNIEnv *env, jobject obj) {
    return fivmr_Handle_hashCode((fivmr_Handle*)obj);
}

void JVM_MonitorWait(JNIEnv *env, jobject obj, jlong ms) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Handle *h=(fivmr_Handle*)obj;
    FijiJDK_JDK_monitorWait(ts,h,ms);
}

void JVM_MonitorNotify(JNIEnv *env, jobject obj) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Handle *h=(fivmr_Handle*)obj;
    FijiJDK_JDK_notify(ts,h);
}

void JVM_MonitorNotifyAll(JNIEnv *env, jobject obj) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Handle *h=(fivmr_Handle*)obj;
    FijiJDK_JDK_notifyAll(ts,h);
}

jobject JVM_Clone(JNIEnv *env, jobject obj) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Handle *h=(fivmr_Handle*)obj;
    return (jobject)FijiJDK_JDK_clone(ts,h);
}

jstring JVM_InternString(JNIEnv *env, jstring str) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Handle *h=(fivmr_Handle*)str;
    return (jstring)FijiJDK_JDK_intern(ts,h);
}

jlong JVM_CurrentTimeMillis(JNIEnv *env, jclass ignored) {
    return fivmr_curTime()/1000/1000;
}

jlong JVM_NanoTime(JNIEnv *env, jclass ignored) {
    return fivmr_curTime();
}

void JVM_ArrayCopy(JNIEnv *env, jclass ignored,
                   jobject src, jint srcPos,
                   jobject dst, jint dstPos,
                   jint length) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_arraycopy(ts,
                          (fivmr_Handle*)src,srcPos,
                          (fivmr_Handle*)dst,dstPos,
                          length);
}

jobject JVM_InitProperties(JNIEnv *env, jobject properties) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Handle *h=(fivmr_Handle*)properties;
    FijiJDK_JDK_initProperties(ts,h);
    return properties;
}

void JVM_OnExit(void (*func)(void)) {
    fivmr_abort("JVM_OnExit not implemented");
}

void JVM_Exit(jint code) {
    fivmr_abort("JVM_Exit not implemented");
}

void JVM_Halt(jint code) {
    fivmr_VM_exit(&fijdk_vm);
}

void JVM_GC(void) {
    fivmr_GC_collectFromNative(&fijdk_vm.gc,"System.gc",NULL);
}

jlong JVM_MaxObjectInspectionAge(void) {
    fivmr_GC *gc=&fijdk_vm.gc;
    fivmr_Nanos result;
    
    fivmr_Lock_lock(&gc->gcLock);
    result=fivmr_curTime()-gc->lastStart;
    fivmr_Lock_unlock(&gc->gcLock);
    
    return result/1000/1000;
}

void JVM_TraceInstructions(jboolean on) {
    LOG(0,("Warning: ignoring JVM_TraceInstructions(%s) call",
           on?"true":"false"));
}

void JVM_TraceMethodCalls(jboolean on) {
    LOG(0,("Warning: ignoring JVM_TraceMethodCalls(%s) call",
           on?"true":"false"));
}

jlong JVM_TotalMemory(void) {
    return fivmr_GC_totalMemory(&fijdk_vm.gc);
}

jlong JVM_FreeMemory(void) {
    return fivmr_GC_freeMemory(&fijdk_vm.gc);
}

jlong JVM_MaxMemory(void) {
    return fivmr_GC_maxMemory(&fijdk_vm.gc);
}

void *JVM_LoadLibrary(const char *name) {
    fivmr_ModuleHandle result;
    result=fivmr_Module_load(name);
    if (result==NULL) {
        char *error=strdup(fivmr_Module_getLastError());
        fivmr_ThreadState *ts=fivmr_ThreadState_get(&fijdk_vm);
        FijiJDK_JDK_throwModuleError(ts,error);
    }
    return result;
}

void JVM_UnloadLibrary(void *handle) {
    if (!fivmr_Module_unload((fivmr_ModuleHandle)handle)) {
        /* as far as I can tell, libjava expects us to swallow this error. */
        LOG(0,("Warning: fivmr_Module_unload(%p) failed: %s",
               handle,fivmr_Module_getLastError(I)));
    }
}

void *JVM_FindLibraryEntry(void *handle,const char *name) {
    return fivmr_Module_lookup(handle,name);
}

jboolean JVM_IsSupportedJNIVersion(jint version) {
    /* the joke's on you! */
    return true;
}

jboolean JVM_IsNaN(jdouble d) {
    return d!=d;
}

/* these next four functions should never be called because they are only
   used by Throwable, and we slot in our own Throwable that gets by without
   needing these functions. */

/* FIXME: why not just route these calls to Java?  that way if the JDK
   calls them from *outside* Throwable, it'll Just Work.  a similar strategy
   might be worthwhile for Thread. */

void JVM_FillInStackTrace(JNIEnv *env, jobject throwable) {
    fivmr_abort("JVM_FillInStackTrace not implemented");
}

void JVM_PrintStackTrace(JNIEnv *env, jobject throwable, jobject printable) {
    fivmr_abort("JVM_PrintStackTrace not implemented");
}

void JVM_GetStackTraceDepth(JNIEnv *env, jobject throwable) {
    fivmr_abort("JVM_GetStackTraceDepth not implemented");
}

void JVM_GetStackTraceElement(JNIEnv *env, jobject throwable, jint index) {
    fivmr_abort("JVM_GetStackTraceElement not implemented");
}

void JVM_InitializeCompiler(JNIEnv *env, jclass compCls) {
    LOG(1,("Ignoring call to JVM_InitializeCompiler"));
}

jboolean JVM_IsSilentCompiler(JNIEnv *env, jclass compCls) {
    LOG(1,("Ignoring call to JVM_IsSilentCompiler"));
    return true; /* ? */
}

jboolean JVM_CompileClass(JNIEnv *env, jclass compCls, jclass cls) {
    LOG(1,("Ignoring call to JVM_CompileClass"));
    return true;
}

jboolean JVM_CompileClasses(JNIEnv *env, jclass cls, jstring jname) {
    LOG(1,("Ignoring call to JVM_CompileClasses"));
    return true;
}

jobject JVM_CompilerCommand(JNIEnv *env, jclass compCls, jobject arg) {
    LOG(1,("Ignoring call to JVM_CompilerCommand"));
    return NULL;
}

void JVM_EnableCompiler(JNIEnv *env, jclass compCls) {
    LOG(1,("Ignoring call to JVM_EnableCompiler"));
}

void JVM_DisableCompiler(JNIEnv *env, jclass compCls) {
    LOG(1,("Ignoring call to JVM_DisableCompiler"));
}

void JVM_StartThread(JNIEnv *env, jobject thread) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_startThread(ts,(fivmr_Handle*)thread);
}

void JVM_StopThread(JNIEnv *env, jobject thread) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_stopThread(ts,(fivmr_Handle*)thread);
}

jboolean JVM_IsThreadAlive(JNIEnv *env, jobject thread) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return FijiJDK_JDK_isThreadAlive(ts,(fivmr_Handle*)thread);
}

void JVM_SuspendThread(JNIEnv *env, jobject thread) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_suspendThread(ts,(fivmr_Handle*)thread);
}

void JVM_ResumeThread(JNIEnv *env, jobject thread) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_resumeThread(ts,(fivmr_Handle*)thread);
}

void JVM_SetThreadPriority(JNIEnv *env, jobject thread, jint prio) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_setThreadPriority(ts,(fivmr_Handle*)thread, prio);
}

void JVM_Yield(JNIEnv *env, jclass threadClass) {
    fivmr_yield();
}

void JVM_Sleep(JNIEnv *env, jclass threadClass, jlong millis) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_sleep(ts,millis);
}

jobject JVM_CurrentThread(JNIEnv *env, jclass threadClass) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jobject)FijiJDK_JDK_currentThread(ts);
}

jint JVM_CountStackFrames(JNIEnv *env, jobject thread) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return FijiJDK_JDK_countStackFrames(ts,(fivmr_Handle*)thread);
}

void JVM_Interrupt(JNIEnv *env, jobject thread) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_interrupt(ts,(fivmr_Handle*)thread);
}

jboolean JVM_IsInterrupted(JNIEnv *env, jobject thread, jboolean clearInterrupted) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return FijiJDK_JDK_isInterrupted(ts,(fivmr_Handle*)thread,clearInterrupted);
}

jboolean JVM_holdsLock(JNIEnv *env, jclass threadClass, jobject object) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return FijiJDK_JDK_holdsLock(ts,(fivmr_Handle*)object);
}

void JVM_DumpAllStacks(JNIEnv *env, jclass unused) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    FijiJDK_JDK_dumpAllStacks(ts);
}

jobjectArray JVM_GetAllThreads(JNIEnv *env, jclass dummy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jobjectArray)FijiJDK_JDK_getAllThreads(ts);
}

jobjectArray JVM_DumpThreads(JNIEnv *env, jclass threadClass, jobjectArray threads) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jobjectArray)FijiJDK_JDK_dumpThreads(ts,(fivmr_Handle*)threads);
}

/* FIXME:
   we'll need to figure out a way of handling protection domains, access control
   contexts, and privileged frames.  My understanding of this is as follows:
   
   - a call to doPrivileged simply means that the permissions of the code called
     from within doPrivileged should be judged purely by that code itself and
     not by the permissions of any of the callers.

   - from the VM's standpoint, ProtectionDomain is just a cookie associated with
     a class when it's loaded.

   - an AccessControlContext is just an array of ProtectionDomains plus some
     simple meta-data.

   This means that Fiji VM very nearly has everything it needs to support this
   garbage.  Only things we really need are:
   
   - a way of quickly detecting if a frame corresponds to the doPrivileged call.
     should be easy.  just cache the MethodRec for that method somewhere and
     we're done.  heck, we could probably make this work *even if* that method
     was inlined.

   - a place to stash the ProtectionDomain for a class.  currently we ignore it.
     but all we need to do to make this work is just save it.  it's not hard.

   - finally, just implement the support methods.  again, not hard.
*/
