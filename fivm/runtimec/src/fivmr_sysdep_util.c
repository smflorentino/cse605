/*
 * fivmr_sysdep_util.c
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
#include "fivmr.h"

/* put system-dependent stuff that is common here.  I realize that seems like a
   contradiction but it isn't. */

typedef struct {
    char *buf;
    size_t size;
} ThreadStringBuf;

static fivmr_ThreadSpecific threadStringBuf;
static fivmr_ThreadSpecific threadCanExit;

static void threadStringBuf_destructor(uintptr_t value) {
    ThreadStringBuf *buf=(ThreadStringBuf*)value;
    if (buf!=NULL) {
	free(buf->buf);
    }
}

void fivmr_SysDepUtil_init(void) {
    fivmr_ThreadSpecific_initWithDestructor(&threadStringBuf,
					    threadStringBuf_destructor);
    fivmr_ThreadSpecific_init(&threadCanExit);
}

void fivmr_preinitThreadStringBufFor(fivmr_ThreadHandle th) {
    int size=1024;
    ThreadStringBuf *buf;
    buf=(ThreadStringBuf*)malloc(sizeof(ThreadStringBuf));
    fivmr_assert(buf!=NULL);
    buf->size=size;
    buf->buf=malloc(size);
    fivmr_assert(buf->buf!=NULL);
    fivmr_ThreadSpecific_setForThread(&threadStringBuf,th,(uintptr_t)buf);
}

char *fivmr_threadStringBuf(size_t size) {
    ThreadStringBuf *buf=(ThreadStringBuf*)
	fivmr_ThreadSpecific_get(&threadStringBuf);
    if (buf!=NULL && buf->size<size) {
	if (false) printf("freeing old buf\n");
	free(buf->buf);
    }
    if (buf==NULL) {
	if (false) printf("buf was null\n");
	buf=(ThreadStringBuf*)malloc(sizeof(ThreadStringBuf));
	fivmr_assert(buf!=NULL);
	buf->size=0;
	fivmr_ThreadSpecific_set(&threadStringBuf,(uintptr_t)buf);
    }
    if (size>buf->size) {
	if (false) printf("allocating new buf\n");
	buf->buf=(char*)malloc(size);
	fivmr_assert(buf->buf!=NULL);
	buf->buf[0]=0;
	buf->size=size;
    }
    return buf->buf;
}

const char *fivmr_getThreadStringBuf(void) {
    if (false) printf("in getThreadStringBuf\n");
    return fivmr_threadStringBuf(1);
}

char *fivmr_tsprintf(const char *msg,...) {
    va_list lst;
    size_t size;
    ThreadStringBuf empty;
    ThreadStringBuf *buf;
    char *result;
    empty.buf=NULL;
    empty.size=0;
    buf=(ThreadStringBuf*)fivmr_ThreadSpecific_get(&threadStringBuf);
    if (buf==NULL) {
	if (false) printf("buf is null\n");
	buf=&empty;
    }
    va_start(lst,msg);
    size=vsnprintf(buf->buf,buf->size,msg,lst)+1;
    if (false) printf("buf->size = %d, size = %d\n",(int)buf->size,(int)size);
    va_end(lst);
    if (size<=buf->size) {
	if (false) printf("returning %s\n",buf->buf);
	return buf->buf;
    }
    result=fivmr_threadStringBuf(size);
    va_start(lst,msg);
    vsnprintf(result,size,msg,lst);
    va_end(lst);
    if (false) printf("returning %s\n",result);
    return result;
}

double fivmr_parseDouble(const char *cstr) {
    /* from ovm */
    char* endptr;
    double val = 0.0;
    val = strtod(cstr, &endptr);
    return val;
}

int32_t fivmr_availableProcessors(void) {
    return 2; /* FIXME */
}

void *fivmr_allocPages_IMPL(uintptr_t numPages,
                            bool *isZero,
                            const char *whereFile,
                            int whereLine) {
    void *result=fivmr_tryAllocPages_IMPL(numPages,isZero,whereFile,whereLine);
    if (result==NULL) {
        fivmr_abortf("Could not allocate %p pages at %s:%d.",
                     numPages,whereFile,whereLine);
    }
    return result;
}

const char *fivmr_OS_name(void) {
    return FIVMSYS_OS;
}

const char *fivmr_OS_arch(void) {
    return FIVMSYS_ARCH;
}

bool fivmr_Thread_canExit(void) {
    return fivmr_ThreadSpecific_get(&threadCanExit)==0;
}

void fivmr_Thread_disableExit(void) {
    fivmr_ThreadSpecific_set(&threadCanExit,1);
}


