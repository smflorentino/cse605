/*
 * fivmr_run.c
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

void fivmr_runBaseInit(void) {
    fivmr_Log_init();

    LOG(1,("Initializing system-dependent library..."));
    
    fivmr_SysDep_init();

    LOG(1,("System-dependent library initialized.  Integrating main thread..."));
    
    fivmr_Thread_integrate();
    
    LOG(1,("Initializing globals..."));
    
    fivmr_VM_initGlobal();

    LOG(1,("Ready to start runtime."));

#if FIVMR_FLOW_LOGGING
    fivmr_FlowLog_init();
#endif
}

void fivmr_runBaseTRPartInit(void) {
    fivmr_runBaseInit();
    
    LOG(1,("Initializing suspension management..."));
    
    fivmr_initSuspensionManager();
    
    LOG(1,("Ready to use time/resource partitioning."));
}

void fivmr_runRuntime(fivmr_VM *vm,
                      int argc,
                      char **argv) {
    fivmr_runRuntimeWithClass(vm,vm->payload->entrypoint,argc,argv);
}

void fivmr_runRuntimeWithClass(fivmr_VM *vm,
                               fivmr_TypeData *mainClass,
                               int argc,
                               char **argv) {
    fivmr_VM_init(vm);
    fivmr_VM_runWithClass(vm,mainClass,argc,argv);
}

void fivmr_VM_run(fivmr_VM *vm,
                  int argc,
                  char **argv) {
    fivmr_VM_runWithClass(vm,vm->payload->entrypoint,argc,argv);
}

void fivmr_VM_runWithClass(fivmr_VM *vm,
                           fivmr_TypeData *mainClass,
                           int argc,
                           char **argv) {
    /* all variables should be volatile due to longjmp/setjmp */
    fivmr_ThreadState * volatile ts;
    fivmr_MethodRec * volatile mainMR;
    fivmr_Handle * volatile args;
    fivmr_Value volatile argArray[1];
    int volatile result=0;
    bool volatile res;
    fivmr_JmpBuf volatile jmpbuf;

    LOG(1,("Creating C-side thread state."));
    
    vm->exceptionsFatalReason="Unexpected exception during system initialization";

    ts=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
    if (ts==NULL && vm->exiting) {
        return;
    }
    fivmr_assert(ts!=NULL);
    
    ts->jumpOnExit=(fivmr_JmpBuf*)&jmpbuf;
    if (fivmr_JmpBuf_label((fivmr_JmpBuf*)&jmpbuf)) {
        LOG(1,("Jumped out of VM; returning. (1)"));
        return;
    }
    
    fivmr_ThreadState_setStackHeight(ts,fivmr_Thread_stackHeight()-FIVMR_STACK_HEIGHT_HEADROOM);
    fivmr_ThreadState_setBasePrio(ts,fivmr_Thread_getPriority(fivmr_ThreadHandle_current()));
    fivmr_ThreadState_set(ts,NULL);
    
    fivmRuntime_boot(ts); /* initialize the Java side of the runtime */

    LOG(1,("Attempting to create main thread."));

#if FIVMBUILD__SCJ
    LOG(1,("SCJ support is enabled."));
    LOG(1,("Creating RT VM Thread.."));
    
    /* FIXME: we're passing FIVMR_TPR_RR_MAX ... but perhaps we should passThread_getPriority. */
    if (!fivmr_ThreadState_glue(
            ts,
            fivmr_VMThread_createRT(
                ts,
                (FIVMR_TPR_FIFO_MIN+FIVMR_TPR_FIFO_MAX)/2,
                false))) {
        return;
    }
#else
    
    /* FIXME: we're passing TPR_NORMAL_MIN ... but perhaps we should pass Thread_getPriority. */
    if (!fivmr_ThreadState_glue(
            ts,
            fivmr_VMThread_create(
                ts,
                FIVMR_TPR_NORMAL_MIN,
                false))) {
        return;
    }
#endif
	
    /* This is the first point at which we have enough VM to flow log */
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_VM, FIVMR_FLOWLOG_SUBTYPE_INIT, 0);

    LOG(1,("We now have a main thread; attempting to run post-thread-init payload callback."));
    
    res=vm->payload->postThreadInitCback(ts); /* this is typically used for
                                                 static JNI on-loads */
    fivmr_assert(res);

    if (vm->config.enableHardRTJ) {
        fivmr_assert(vm->maxPriority==FIVMR_TPR_CRITICAL);
        LOG(1,("Initializing HardRTJ..."));
        
        fivmr_initHardRTJ(vm);
    }

    LOG(1,("Attempting to initialize system classes."));
    
    fivmRuntime_initSystemClassLoaders(ts);
    
    /* initialize enough of the system to be able to use Charsets.  that's important,
       because then we can just use Classpath's Charset support for UTF-8 conversions,
       which we will be doing lots of for stuff like native method lookup.
       
       NB. for safety, this should match the list of things being marked used in
       OneWordHeaderContiguousClasspathObjectModel. */
    fivmr_TypeData_checkInitEasy(ts,"java/lang/System");
    fivmr_TypeData_checkInitEasy(ts,"java/lang/String");
#if FIVMR_GLIBJ
    fivmr_TypeData_checkInitEasy(ts,"java/security/VMAccessController");
#endif
    fivmr_TypeData_checkInitEasy(ts,"java/lang/Character");
    fivmr_TypeData_checkInitEasy(ts,"java/lang/Math");
    
    /* these only need to be initialized to make OOME handling cleaner
       (otherwise we get an OOME when logging) */
#if FIVMR_GLIBJ
    fivmr_TypeData_checkInitEasy(ts,"java/lang/VMClass");
#elif FIVMR_FIJICORE
    fivmr_TypeData_checkInitEasy(ts,"java/lang/FCClass");
#endif

    /* notify the runtime that everything needed for Charset usage has been initialized,
       and thus future attempts to convert between C strings and Java strings can use
       Java rather than iconv().
       
       NB. at sufficient log level this will blow up in your face if the right
       chunks of the system have yet to be initialized. */
    fivmRuntime_notifyInitialized(ts);
    
    LOG(1,("System classes initialized; attempting to run user's main method."));

    mainMR=fivmr_TypeData_findStaticMethod(vm,mainClass,"main","([Ljava/lang/String;)V");
    if (mainMR==NULL) {
	fivmr_abortf("Error: cannot find main method in class %s\n",mainClass);
    }
    LOG(1,("Found main method: %s",
	   fivmr_MethodRec_describe(mainMR)));
    args=fivmr_processArgs(ts,argc-1,argv+1);
    
    /* enable normal exception handling */
    vm->exceptionsFatalReason=NULL;
    
    /* Since this is a shortcut around "normal" thread execution, we
     * synthesize a run event here.  This call needs to match the call
     * in VMThread::VMThread_run(). */
    /* FIXME: This really shouldn't be 0, here */
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_THREAD, FIVMR_FLOWLOG_SUBTYPE_RUN, 0);

    argArray[0].H=args;
    fivmr_MethodRec_call(mainMR,ts,NULL,NULL,(fivmr_Value*)argArray,
			 FIVMR_CM_HANDLES|FIVMR_CM_EXEC_STATUS|
			 FIVMR_CM_DISPATCH|FIVMR_CM_CHECKINIT);
    if (ts->curExceptionHandle!=NULL) {
	fivmr_Handle *e=ts->curExceptionHandle;
	ts->curExceptionHandle=NULL;
	fivmr_describeException(ts,e);
	result=1;
    }
    /* Log that we're beginning the shutdown process.  After this point
     * we may not have a convenient ts again. */
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_VM, FIVMR_FLOWLOG_SUBTYPE_EXIT, 0);
    /* terminate the main thread. */
    fivmr_ThreadState_terminate(ts);
    
    /* wait for other threads to die and shutdown the VM. */
    if (!fivmr_VM_waitForDeath(vm)) {
        return;
    }

    /* create a thread to do the honors */
    vm->exceptionsFatalReason="Unexpected exception during system shutdown";
    ts=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
    ts->jumpOnExit=(fivmr_JmpBuf*)&jmpbuf;
    if (fivmr_JmpBuf_label((fivmr_JmpBuf*)&jmpbuf)) {
        LOG(1,("Jumped out of VM; returning. (2)"));
        return;
    }
    LOG(1,("At the point right past the exit label."));
    fivmr_ThreadState_setStackHeight(ts,fivmr_Thread_stackHeight()-FIVMR_STACK_HEIGHT_HEADROOM);
    fivmr_ThreadState_setBasePrio(ts,fivmr_Thread_getPriority(fivmr_ThreadHandle_current()));
    fivmr_ThreadState_set(ts,NULL);
    if (!fivmr_ThreadState_glue(ts,fivmr_VMThread_create(ts,FIVMR_TPR_NORMAL_MIN,false))) {
        return;
    }
    vm->exceptionsFatalReason=NULL;
    
    /* Hack ... there's java code after this point, but we won't be able
     * to log it. */
#if FIVMR_FLOW_LOGGING
    fivmr_FlowLog_finalize();
#endif

    /* actually exit */
    fivmr_javaExit(ts,result);
    LOG(1,("fivmr_javaExit() returned; this could be bad!"));
    fivmr_assertNoException(ts,"while attempting to exit");
    
    /* should never get here. */
    fivmr_assert(false);
}


