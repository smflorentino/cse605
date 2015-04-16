/*
 * fivmr_posix_main.c
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

#include "fivmc_compile_info.h"

#if defined(HAVE_SIGNAL_H)
#include <signal.h>
#endif

#define PAYLOAD FIVMR_CONCAT(FIVMC_OUTPUT,_payload)

extern fivmr_Payload PAYLOAD;

static fivmr_VM vm;
static fivmr_Configuration config;

static const char *parseEnvOpt(const char *envName,
			       const char *defaultVal) {
    const char *val=getenv(envName);
    if (val==NULL) {
	return defaultVal;
    } else {
	return val;
    }
}

static int32_t parseEnvIntOpt(const char *envName,
			      int32_t defaultVal) {
    const char *val=getenv(envName);
    if (val!=NULL) {
	int result;
	if (sscanf(val,"%d",&result)!=1) {
	    fprintf(stderr,
		    "fivmr Warning: expected integer value for %s, but "
		    "got %s; using default of %d\n",
		    envName,val,defaultVal);
	    return defaultVal;
	} else {
	    return (int32_t)result;
	}
    } else {
	return defaultVal;
    }
}

static uintptr_t parseEnvSizeOpt2(const char *envName,
                                  uintptr_t defaultVal,
                                  bool *changed) {
    const char *val;
    if (changed!=NULL) {
        *changed=true;
    }
    val=getenv(envName);
    if (val!=NULL) {
	uintptr_t result;
	int res;
	char suffix;
	res=sscanf(val,"%" PRIuPTR "%c",&result,&suffix);
	switch (res) {
	case 1:
	    return result;
	case 2:
	    switch (suffix) {
	    case 'k':
	    case 'K':
		return result*1024;
	    case 'm':
	    case 'M':
		return result*1024*1024;
	    case 'g':
	    case 'G':
		return result*1024*1024*1024;
	    default:
		fprintf(stderr,
			"fivmr Warning: unrecognized suffix '%c' for argument to %s; "
			"using %" PRIuPTR " as the value\n",
			suffix,envName,result);
		return result;
	    }
	    break;
	default:
	    fprintf(stderr,
		    "fivmr Warning: expected size value for %s, but "
		    "got %s; using default of %" PRIuPTR "\n",
		    envName,val,defaultVal);
            if (changed!=NULL) {
                *changed=false;
            }
	    return defaultVal;
	}
    } else {
        if (changed!=NULL) {
            *changed=false;
        }
	return defaultVal;
    }
}

static uintptr_t parseEnvSizeOpt(const char *envName,
				 uintptr_t defaultVal) {
    return parseEnvSizeOpt2(envName,defaultVal,NULL);
}

static fivmr_ThreadPriority parsePriorityOpt(const char *envName,
                                             fivmr_ThreadPriority defaultVal) {
    const char *val=getenv(envName);
    if (val!=NULL) {
        fivmr_ThreadPriority result=
            fivmr_ThreadPriority_parse(val);
        if (result==FIVMR_TPR_INVALID) {
            fprintf(stderr,
                    "fivmr Warning: could not parse priority string '%s' passed as "
                    "argument to %s\n",
                    val,envName);
            return defaultVal;
        } else {
            return result;
        }
    } else {
        return defaultVal;
    }
}

static bool parseEnvBoolOpt(const char *envName,
			    bool defaultVal) {
    const char *val=getenv(envName);
    if (val!=NULL) {
	if (!strcmp(val,"true")) {
	    return true;
	} else if (!strcmp(val,"false")) {
	    return false;
	} else {
	    fprintf(stderr,"fivmr Warning: expected boolean (true or false) value for %s\n",envName);
	    return defaultVal;
	}
    } else {
	return defaultVal;
    }
}

#if defined(HAVE_SIGNAL_H) && defined(HAVE_SIGACTION)
static void my_signal(int sig,void (*handler)(int)) {
    struct sigaction sig_d;
    bzero(&sig_d,sizeof(sig_d));
    sig_d.sa_handler=handler;
    if (sigaction(sig,&sig_d,NULL)<0) {
	fivmr_abortf("sigaction() returned error: %s",
		     strerror(errno));
    }
}
#endif

static int argc;
static char **argv;

static void main_thread(void *arg) {
    fivmr_runRuntime(&vm,argc,argv);
}

int main(int c,char **v) {
    bool maxMemChanged;
    uintptr_t maxMem;
    /* set up our environment so it's not retarded. */
#if defined(HAVE_SETVBUF)
    setvbuf(stdout,NULL,_IONBF,0);
    setvbuf(stderr,NULL,_IONBF,0);
#endif
#if defined(HAVE_SIGNAL_H) && defined(HAVE_SIGACTION)
    my_signal(SIGPIPE,SIG_IGN);
#endif

    /* actually do stuff. */
    
    if (parseEnvBoolOpt("FIVMR_SHOW_COMPILE_INFO",false)) {
	printf("fivmc %s %s, All Rights Reserved\n",FIVMC_VERSION,FIVMR_COPYRIGHT);
	printf("\n");
	printf("Info for %s:\n",v[0]);
	printf("\n");
	printf("target system name:\n%s\n",FIVMC_SYS_NAME);
	printf("\n");
	printf("compile command:\n%s\n",FIVMC_CMD);
	printf("\n");
	printf("compiled in directory:\n%s\n",FIVMC_DIR);
	printf("\n");
	printf("fivm home directory:\n%s\n",FIVMC_HOMEDIR);
	printf("\n");
	printf("uname -a of compilation host:\n%s\n",FIVMC_HOSTINFO);
	printf("\n");
	printf("date compiled:\n%s\n",FIVMC_DATE);
	printf("\n");
	return 0;
    }
    
    if (parseEnvBoolOpt("FIVMR_SHOW_VERSION",false)) {
        printf("%s %s %s, All Rights Reserved\n",fivmr_name(),fivmr_version(),fivmr_copyright());
        return 0;
    }

    fivmr_logFile=parseEnvOpt("FIVMR_LOG_FILE",NULL);
    fivmr_logLevel=parseEnvIntOpt("FIVMR_LOG_LEVEL",0);
    fivmr_debugLevel=parseEnvIntOpt("FIVMR_DEBUG_LEVEL",0);
    printf("FIVMR Runtime Debug Level %d",fivmr_debugLevel);
    
    fivmr_fakeRTPriorities=parseEnvBoolOpt("FIVMR_FAKE_RT_PRIORITIES",false);
    
    fivmr_POSIX_logAllocExec=parseEnvBoolOpt("FIVMR_LOG_ALLOC_EXEC",false);
    
    fivmr_abortThrow=parseEnvBoolOpt("FIVMR_ABORT_THROW",false);
    
#if FIVMR_FLOW_LOGGING
    fivmr_flowLogFile=parseEnvOpt("FIVMR_FLOWLOG_FILE",NULL);
#endif

    fivmr_Thread_affinity=
	parseEnvIntOpt("FIVMR_THREAD_AFFINITY",0);
    if (fivmr_Thread_affinity && !FIVMR_CAN_SET_AFFINITY) {
	fivmr_abortf("specified affinity of %d, but we don't know "
		     "how to set thread-processor affinity on this system.",
		     fivmr_Thread_affinity);
    }
    
    fivmr_runBaseTRPartInit();
    
    config=*PAYLOAD.defConfig;
    
    fivmr_VM_resetSettings(&vm,&config);
    fivmr_VM_registerPayload(&vm,&PAYLOAD);
    
    fivmr_logReflect=parseEnvBoolOpt("FIVMR_LOG_REFLECT",false);
    if (parseEnvBoolOpt("FIVMR_LOG_THROW",false)) {
        vm.flags|=FIVMR_VMF_LOG_THROW;
    }

    vm.exitExits=parseEnvBoolOpt("FIVMR_EXIT_EXITS",true);
    
    if (parseEnvBoolOpt("FIVMR_LOG_MACHINE_CODE",false)) {
        vm.flags|=FIVMR_VMF_LOG_MACHINE_CODE;
    }

    if (parseEnvBoolOpt("FIVMR_FORCE_JIT_SLOWPATH",false)) {
        vm.flags|=FIVMR_VMF_FORCE_JIT_SLOWPATH;
    }

    vm.gc.logGC=parseEnvBoolOpt("FIVMR_LOG_GC",vm.gc.logGC);
    vm.gc.logSyncGC=parseEnvBoolOpt("FIVMR_LOG_SYNC_GC",vm.gc.logSyncGC);
    vm.gc.finalGCReport=parseEnvBoolOpt("FIVMR_FINAL_GC_REPORT",false);
    vm.gc.abortOOME=parseEnvBoolOpt("FIVMR_ABORT_OOME",false);

    if (parseEnvBoolOpt("FIVMR_JNI_COVERAGE",false)) {
        vm.flags|=FIVMR_VMF_JNI_COVERAGE;
    }
    
    vm.gc.maxPagesUsed=
	(maxMem=parseEnvSizeOpt2("FIVMR_GC_MAX_MEM",config.gcDefMaxMem,&maxMemChanged))
	>>FIVMSYS_LOG_PAGE_SIZE;
    vm.gc.gcTriggerPages=
	parseEnvSizeOpt("FIVMR_GC_TRIGGER",
                        maxMemChanged?(uintptr_t)(maxMem/2):(uintptr_t)config.gcDefTrigger)
	>>FIVMSYS_LOG_PAGE_SIZE;
    vm.gc.immortalMem=
        parseEnvSizeOpt("FIVMR_GC_IMMORTAL_MEM",config.gcDefImmortalMem);
    vm.gc.threadPriority=
        parsePriorityOpt("FIVMR_GC_THREAD_PRIORITY",config.gcDefPriority);

    vm.gc.ignoreSystemGC=
	parseEnvBoolOpt("FIVMR_IGNORE_SYS_GC",false);
    
    if (parseEnvBoolOpt("FIVMR_VERBOSE_EXCEPTIONS",false)) {
        vm.flags|=FIVMR_VMF_VERBOSE_EXCEPTIONS;
    }
    
    if (parseEnvBoolOpt("FIVMR_PROFILER",false)) {
        vm.flags|=FIVMR_VMF_RUN_PROFILER;
    }
    
    vm.monitorSpinLimit=parseEnvIntOpt("FIVMR_MONITOR_SPIN_LIMIT",100);

    if (FIVMR_MAIN_IN_THREAD(&PAYLOAD.settings)) {
        /* this is the default - because on many systems the stack height of a new
           thread we start is different than the stack height of the primordial
           thread. */
        argc=c;
        argv=v;
        fivmr_Thread_spawn(main_thread,NULL,fivmr_Thread_getPriority(fivmr_ThreadHandle_current()));
        for (;;) pause();
    } else {
        fivmr_runRuntime(&vm,c,v);
    }
    
    fivmr_assert(false); /* should never get here */
    return 1;
}

#endif

