/*
 * fivmr_logger.h -- high-speed logging of real-time events
 * by Filip Pizlo, 2007
 */

#ifndef FP_LOGGER_H
#define FP_LOGGER_H

#include "fivmr_sysdep.h"

#define FIVMR_LOGGER_MAX_THREADS 1024

void fp_log(intptr_t thread_id,
	    const char *format, /* uses unconventional format string, where:
				   %i - pointer-width integer,
				   %u - unsigned pointer-width integer */
	    unsigned num_args, /* specify number of arguments - this
				  prevents me from having to parse the
				  format in real time.  note, you cannot
				  have more than 15 args. */
	    ...);

void fp_commit(FILE *output);

#endif

