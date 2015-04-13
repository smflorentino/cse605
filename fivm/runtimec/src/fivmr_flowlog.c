/*
 * fivmr_flowlog.c
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

#if FIVMR_FLOW_LOGGING

/* The platform init function should set this to TRUE if logging is enabled */
int fivmr_flowLogEnabled;

static fivmr_FlowLogBuffer *fbhead;
static fivmr_FlowLogBuffer *fbtail;

static fivmr_Semaphore sema;
static int should_die;

static void flowLogThreadMain(void *arg);

void fivmr_FlowLog_init(void) {
    fivmr_flowLogEnabled = FALSE;
    fbhead = fbtail = NULL;
    should_die = FALSE;

    fivmr_FlowLog_platform_init();

    /* FIXME: Priorities? */
    fivmr_Semaphore_init(&sema);

    /* It's unclear to me where this thread should come from; possibly
     * the VM, possibly not -- I'm thinking it belongs to the main
     * process, not a VM, so we're fivmr_Thread_spawning here. */
    fivmr_Thread_spawn(flowLogThreadMain, NULL, FIVMR_TPR_NORMAL_MIN);
}

void fivmr_FlowLog_finalize(void) {
    fivmr_FlowLog_lock();
    should_die = 1;
    fivmr_Semaphore_up(&sema);
    fivmr_FlowLog_unlock();
    fivmr_FlowLog_wait();
    fivmr_FlowLog_platform_finalize();
}

void fivmr_FlowLog_release(fivmr_FlowLogBuffer *flowbuf) {
    flowbuf->next = NULL;
    if (!fivmr_flowLogEnabled) {
        fivmr_free(flowbuf);
        return;
    }
    fivmr_FlowLog_lock();
    if (should_die) {
        /* This is unnecessary in some cases, because the mutex has been
         * destroyed.  The whole shutdown sequence is ugly. */
        fivmr_FlowLog_unlock();
        fivmr_free(flowbuf);
        return;
    }
    if (!fbhead) {
        fbhead = fbtail = flowbuf;
    } else {
        fbtail->next = flowbuf;
        fbtail = flowbuf;
    }
    fivmr_Semaphore_up(&sema);
    fivmr_FlowLog_unlock();
}

void fivmr_FlowLog_releaseTS(fivmr_ThreadState *ts) {
    fivmr_assert(ts->flowbuf);
    fivmr_FlowLog_release(ts->flowbuf);
    ts->flowbuf = fivmr_malloc(FIVMR_FLOWLOG_BUFFERSIZE);
    ts->flowbuf->entries = 0;
}

/* FIXME: Final flush and shutdown */
static void flowLogThreadMain(void *arg) {
    fivmr_FlowLogBuffer *flowbuf;
    fivmr_FlowLog_wait();
    for (;;) {
        fivmr_Semaphore_down(&sema);
        fivmr_FlowLog_lock();

        fivmr_assert(fbhead != NULL || should_die);

        if (fbhead == NULL && should_die) {
            fivmr_FlowLog_unlock();
            fivmr_FlowLog_notify();
            fivmr_Thread_exit();
        }

        flowbuf = fbhead;
        if (fbhead->next == NULL) {
            fbhead = fbtail = NULL;
        } else {
            fbhead = fbhead->next;
        }
        fivmr_FlowLog_unlock();

        fivmr_FlowLog_platform_write(flowbuf);
        fivmr_free(flowbuf);
    }
}

#endif /* FIVMR_FLOW_LOGGING */
