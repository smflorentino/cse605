/*
 * fivmr_rtems_main.c
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
#if FIVMR_RTEMS

#include "fivmr.h"

#include "fivmr_rtos_config.h"
#include "fivmc_compile_info.h"

#define PAYLOAD FIVMR_CONCAT(FIVMC_OUTPUT,_payload)

extern fivmr_Payload PAYLOAD;

static fivmr_VM vm; /* HACK - FIXME */

#define INTRDEBUG 0

#if INTDEBUG
static rtems_id timerid;

static void mytimer(rtems_id timer,void *arg) {
    if (fivmr_threadById!=NULL) {
        uint32_t i;
        printk("threads entering list\n");
        for (i=0;i<vm.config.maxThreads;++i) {
            if (fivmr_ThreadState_byId(i)->forMonitor.entering!=NULL) {
                printk("   thread %u entering %p\n",i,fivmr_threadById[i]);
            }
        }
        printk("   (list done)\n");
    }
    rtems_timer_fire_after(timerid,100,mytimer,NULL);
}
#endif

static void mainT(void* arg_) {
     /* dummy args for RTEMS */
    char* argv [1];
#if FIVMR_ASSERTS_ON
    rtems_interrupt_level cookie,cookie2;
#endif

    LOG(2, ("Spawning thread complete. Running in Main Task."));

    argv[0] = "fivm"; /* this gets ignored */
    
#if FIVMR_ASSERTS_ON
    rtems_interrupt_disable(cookie);
    fivmr_assert(fivmr_Thread_isCritical());
    rtems_interrupt_disable(cookie2);
    fivmr_assert(fivmr_Thread_isCritical());
    rtems_interrupt_enable(cookie2);
    fivmr_assert(fivmr_Thread_isCritical());
    rtems_interrupt_enable(cookie);
    fivmr_assert(!fivmr_Thread_isCritical());
#endif
    
#if INTRDEBUG
    rtems_timer_create(123,&timerid);
    rtems_timer_fire_after(timerid,100,mytimer,NULL);
#endif
    
    fivmr_runRuntime(&vm,1,argv);
    /* should never get here */
    fivmr_assert(false);
}

rtems_task Init(rtems_task_argument ignored) {
    fivmr_Configuration config;

    fivmr_RTEMS_threadStackSize=FIVMR_THREAD_STACK_SIZE;
    
    fivmr_runBaseInit();
    
    config=*PAYLOAD.defConfig;
    
    fivmr_VM_resetSettings(&vm,&config);
    if (false) {
        /* FIXME: make this work again! */
        vm.config.enableHardRTJ=true;
    }
    fivmr_VM_registerPayload(&vm,&PAYLOAD);
    
    fivmr_logReflect=false;
    
    vm.exitExits=true;
    
#if FIVMR_CMRGC
    vm.gc.gcTriggerPages=vm.config.gcDefTrigger>>FIVMSYS_LOG_PAGE_SIZE;
    vm.gc.maxPagesUsed=vm.config.gcDefMaxMem>>FIVMSYS_LOG_PAGE_SIZE;
#  if FIVMR_SELF_MANAGE_MEM && FIVMR_HEAP_IN_IMAGE
    vm.gc.forceHeapStart=(uintptr_t)(intptr_t)-1;
    vm.gc.forceHeapEnd=(uintptr_t)(intptr_t)-1;
#  endif
#endif
    
    LOG(1,("Minimum stack size = %p",RTEMS_MINIMUM_STACK_SIZE));
    
    if (fivmr_Thread_spawn(mainT,NULL,FIVMR_TPR_NORMAL_MIN)
        == fivmr_ThreadHandle_zero()) {
        fivmr_abort("Could not start main thread");
    }
    
    rtems_task_delete( RTEMS_SELF );
    fivmr_assert(false);
}

#define CONFIGURE_APPLICATION_NEEDS_CONSOLE_DRIVER
#define CONFIGURE_APPLICATION_NEEDS_CLOCK_DRIVER
/* #define CONFIGURE_MINIMUM_STACK_SIZE   FIVMR_THREAD_STACK_SIZE */
#define CONFIGURE_MICROSECONDS_PER_TICK   (FIVMR_NANOS_PER_TICK/1000)
#define CONFIGURE_TICKS_PER_TIMESLICE     FIVMR_TICKS_PER_TIMESLICE
#define CONFIGURE_MAXIMUM_TASKS           FIVMR_MAX_OS_THREADS
#define CONFIGURE_MAXIMUM_PERIODS         FIVMR_MAX_OS_THREADS+3
#define CONFIGURE_MAXIMUM_TIMERS          3
#define CONFIGURE_INTERRUPT_STACK_SIZE  FIVMR_INTERRUPT_STACK_SIZE

/* two for now, gc and main thread 
   TODO : allow for configuration */

#define CONFIGURE_MAXIMUM_SEMAPHORES       FIVMR_MAX_OS_THREADS*10
#define CONFIGURE_RTEMS_INIT_TASKS_TABLE
#define CONFIGURE_EXTRA_TASK_STACKS         (FIVMR_MAX_OS_THREADS*FIVMR_THREAD_STACK_SIZE)
#define CONFIGURE_USE_IMFS_AS_BASE_FILESYSTEM
#define CONFIGURE_LIBIO_MAXIMUM_FILE_DESCRIPTORS FIVMR_MAX_FILE_DESCRIPTORS
#define CONFIGURE_INIT
#include <rtems/confdefs.h>

#endif
