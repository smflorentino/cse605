/*
 * fivmr_posix_thread.c
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

int32_t fivmr_Thread_affinity;

fivmr_ThreadHandle fivmr_Thread_integrate(void) {
    if (fivmr_Thread_affinity) {
        fivmr_CPUSet set;
	int i;
        bool res;
        fivmr_CPUSet_clear(&set);
	for (i=0;i<32;++i) {
	    if (fivmr_Thread_affinity&(1<<i)) {
		fivmr_CPUSet_set(&set,i);
	    }
	}
	res=fivmr_Thread_setAffinity(pthread_self(), &set);
	fivmr_assert(res);
    }

    return pthread_self();
}

typedef struct {
    void (*threadMain)(void *arg);
    void *arg;
} RunData;

static void *runner(void *arg_) {
    RunData *rd;
    void (*threadMain)(void *arg);
    void *arg;
    
    rd=(RunData*)arg_;
    
    threadMain=rd->threadMain;
    arg=rd->arg;
    
    free(rd);
    
    fivmr_Thread_integrate();
    
    threadMain(arg);
    
    return NULL;
}

fivmr_ThreadHandle fivmr_Thread_spawn(void (*threadMain)(void *arg),
				      void *arg,
				      fivmr_ThreadPriority priority) {
    pthread_t t;
    pthread_attr_t attr;
    struct sched_param schedparam;
    RunData *rd;
    int res, policy, pprio;
    
    rd=fivmr_mallocAssert(sizeof(RunData));
    rd->threadMain=threadMain;
    rd->arg=arg;
    
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr,PTHREAD_CREATE_DETACHED);

    if (FIVMR_PR_SUPPORTED) {
#ifdef HAVE_PTHREAD_ATTR_SETINHERITSCHED
        res=pthread_attr_setinheritsched(&attr,PTHREAD_EXPLICIT_SCHED);
        fivmr_assert(res==0);
#endif
        fivmr_ThreadPriority_to_posix(priority, &policy, &pprio);
        res=pthread_attr_setschedpolicy(&attr,policy);
        fivmr_assert(res==0);
        schedparam.sched_priority=pprio;
        res=pthread_attr_setschedparam(&attr,&schedparam);
        fivmr_assert(res==0);
    }
    
    res=pthread_create(&t,&attr,runner,rd);
    fivmr_assert(res==0);
    
    return t;
}

void fivmr_Thread_exit(void) {
    fivmr_assert(fivmr_Thread_canExit());
    pthread_exit(NULL);
}

void fivmr_Thread_init() {
    /* nothing to init for pthreads */
}

void fivmr_Thread_setPriority(fivmr_ThreadHandle t,
                              fivmr_ThreadPriority priority) {
    if (FIVMR_PR_SUPPORTED) {
        struct sched_param schedparam;
        int policy, pprio, res;
        if (false) {
            char buf[32];
            fivmr_ThreadPriority_describe(priority,buf,sizeof(buf));
            printf("setting priority to %s\n",buf);
        }
        fivmr_ThreadPriority_to_posix(priority, &policy, &pprio);
        schedparam.sched_priority=pprio;
        res=pthread_setschedparam(t,policy,&schedparam);
        fivmr_assert(res==0);
    }
}

fivmr_ThreadPriority fivmr_Thread_getPriority(fivmr_ThreadHandle t) {
    struct sched_param schedparam;
    int policy;
    int res;
    res=pthread_getschedparam(t,&policy,&schedparam);
    fivmr_assert(res==0);
    return posix_to_fivmr_ThreadPriority(policy,schedparam.sched_priority);
}

#if defined(HAVE_PTHREAD_SETAFFINITY_NP)
bool fivmr_Thread_setAffinity(fivmr_ThreadHandle h,
                              fivmr_CPUSet *cpuset) {
    return pthread_setaffinity_np(h,sizeof(cpu_set_t),cpuset)==0;
}
#endif

#endif
