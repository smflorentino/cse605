/*
 * fivmr_posix_flowlog.c
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
#if FIVMR_POSIX && FIVMR_FLOW_LOGGING
#include "fivmr.h"

const char *fivmr_flowLogFile;

static FILE *flowLog;
static pthread_mutex_t flowLogLock;
static pthread_mutex_t flowLogDone;

void fivmr_FlowLog_platform_init(void) {
    fivmr_FlowLogHeader header = { FIVMR_FLOWLOG_MAGIC,
                                   FIVMR_FLOWLOG_VERSION,
                                   FIVMR_FLOWLOG_PLATFORM_POSIX };

    if (!fivmr_flowLogFile)
        return;

    /* FIXME: Priority? */
    pthread_mutex_init(&flowLogLock, NULL);
    pthread_mutex_init(&flowLogDone, NULL);

    flowLog = fopen(fivmr_flowLogFile, "w");
    if (flowLog == NULL) {
        LOG(0,("Warning: failed to open flow log file %s: %s",
               fivmr_flowLogFile, strerror(errno)));
        LOG(0,("Flow logging is disabled."));
        return;
    }

    fivmr_flowLogEnabled = 1;
    LOG(1,("Flow logging enabled, logging to %s", fivmr_flowLogFile));

    fwrite(&header, sizeof(header), 1, flowLog);
    fflush(flowLog);
}

void fivmr_FlowLog_platform_finalize(void) {
    /* FIXME: Iterate threads, flushing their flow log content */
    if (flowLog != NULL) {
        fclose(flowLog);
    }
    pthread_mutex_destroy(&flowLogLock);
    pthread_mutex_destroy(&flowLogDone);
}

void fivmr_FlowLog_lock(void) {
    pthread_mutex_lock(&flowLogLock);
}

void fivmr_FlowLog_unlock(void) {
    pthread_mutex_unlock(&flowLogLock);
}

/*
 * This is purely for shutdown synchronization.  The main flowlog thread
 * waits on this when it starts up, and then finalize waits on it to be
 * released at shutdown.
 */
void fivmr_FlowLog_wait(void) {
    pthread_mutex_lock(&flowLogDone);
}

void fivmr_FlowLog_notify(void) {
    pthread_mutex_unlock(&flowLogDone);
}

void fivmr_FlowLog_platform_write(fivmr_FlowLogBuffer *flowbuf) {
    if (!flowLog)
        return;
    fwrite(flowbuf->events, sizeof(fivmr_FlowLogEvent), flowbuf->entries, flowLog);
}

#endif /* FIVMR_POSIX */
