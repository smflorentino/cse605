/*
 * logger.c
 * by Filip Pizlo, 2007
 */

#include "fivmr.h"
#include "fivmr_logger.h"
#include <stdarg.h>
#include <stdio.h>

#define NUM_EVENTS 1000000

#define MAX_NUM_EVENTS (NUM_EVENTS*10)

struct Latch_s {
    intptr_t latch;
    intptr_t padding[64];
};

struct Event_s {
    intptr_t timestamp;
    const char *format;
    unsigned num_args;
    intptr_t args[10];
};

typedef struct Event_s Event;
typedef struct Latch_s Latch;

static Latch latch[FIVMR_LOGGER_MAX_THREADS];
static Event *events[FIVMR_LOGGER_MAX_THREADS];
static uintptr_t num_events[FIVMR_LOGGER_MAX_THREADS];
static uintptr_t events_cap[FIVMR_LOGGER_MAX_THREADS];

static intptr_t counter;

void fp_log(intptr_t thread_id,
	    const char *format,
	    unsigned num_args,
	    ...) {
    unsigned i;
    va_list lst;
    
    while (!fivmr_cas((uintptr_t*)&latch[thread_id].latch,0,1)) ;
    
    if (num_events[thread_id]==MAX_NUM_EVENTS) {
	latch[thread_id].latch=0;
	fprintf(stderr,"logger: aborting because of too many events.\n");
	fp_commit(stderr);
	abort();
    }
    
    if (num_events[thread_id]==events_cap[thread_id]) {
	if (events[thread_id]==NULL) {
	    events[thread_id]=
		malloc(sizeof(Event)*NUM_EVENTS);
	} else {
	    events[thread_id]=
		realloc(events[thread_id],
			sizeof(Event)*(num_events[thread_id]+NUM_EVENTS));
	}
	events_cap[thread_id]+=NUM_EVENTS;
    }
    
    events[thread_id][num_events[thread_id]].timestamp=fivmr_xchg_add((uintptr_t*)&counter,1);
    events[thread_id][num_events[thread_id]].format=format;
    events[thread_id][num_events[thread_id]].num_args=num_args;
    va_start(lst,num_args);
    for (i=0;i<num_args;++i) {
	events[thread_id][num_events[thread_id]].args[i]=va_arg(lst,intptr_t);
    }
    va_end(lst);
    num_events[thread_id]++;
    latch[thread_id].latch=0;
}

void fp_commit(FILE *out) {
    unsigned i;
    uintptr_t j;
    unsigned k;
    for (i=0;i<FIVMR_LOGGER_MAX_THREADS;++i) {
	while (!fivmr_cas((uintptr_t*)&latch[i].latch,0,1)) ;
    }
    for (i=0;i<FIVMR_LOGGER_MAX_THREADS;++i) {
	if (events[i]!=NULL) {
	    fprintf(out,"Thread #%u:\n",i);
	    for (j=0;j<num_events[i];++j) {
		Event *e=events[i]+j;
		unsigned arg=0;
		fprintf(out,"\tTs.%" PRIdPTR ": ",e->timestamp);
		for (k=0;e->format[k];++k) {
		    if (e->format[k]=='%') {
			++k;
			if (e->format[k]=='i') {
			    fprintf(out,"%" PRIdPTR,e->args[arg++]);
			} else if (e->format[k]=='u') {
			    fprintf(out,"%" PRIuPTR,e->args[arg++]);
			} else {
			    fputc(e->format[k],out);
			}
		    } else {
			fputc(e->format[k],out);
		    }
		}
		fprintf(out,"\n");
	    }
	}
    }
}


