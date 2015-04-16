/*
 * fivmr_posix_log.c
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

static pthread_mutex_t logLock;
int32_t fivmr_logLevel=0;
uint32_t fivmr_debugLevel=0;
const char *fivmr_logFile;
FILE *fivmr_log;

void fivmr_Log_init(void) {
    pthread_mutex_init(&logLock,NULL);
    if (fivmr_log==NULL) {
	fivmr_log=stderr;
	if (fivmr_logFile!=NULL) {
	    FILE *log=fopen(fivmr_logFile,"a");
	    if (log==NULL) {
		LOG(0,("Warning: failed to open log file %s: %s",
		       fivmr_logFile,strerror(errno)));
		LOG(0,("Logging to console instead."));
	    } else {
#if defined(HAVE_SETVBUF)
                setvbuf(log,NULL,_IONBF,0);
#endif
		fivmr_log=log;
	    }
	}
    }
    LOG(1,("%s %s %s, All Rights Reserved",fivmr_name(),fivmr_version(),fivmr_copyright()));
}

void fivmr_Log_lock(void) {
    pthread_mutex_lock(&logLock);
}

void fivmr_Log_unlock(void) {
    pthread_mutex_unlock(&logLock);
}

void fivmr_Log_vprintf(const char *msg,va_list lst) {
    int res;
    res=vfprintf(fivmr_log,msg,lst);
    if (res<0) {
	if (fivmr_log!=stderr) {
	    fivmr_log=stderr;
	    fivmr_logLevel=0;
	    LOG(0,("Could no longer send log output to %s because: %s",
		   fivmr_logFile,strerror(errno)));
	    LOG(0,("Lowered log level to 0, sending further log output to stderr."));
	} else {
	    /* can't print? we're already dead. */
	    abort();
	}
    }
}

#endif
