/*
 * fivmr_posix_module.c
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
#if FIVMR_DYN_LOADING && FIVMR_POSIX

#include "fivmr.h"
#include <dlfcn.h>

fivmr_Lock fivmr_Module_lock;

void fivmr_Module_init(void) {
    fivmr_Lock_init(&fivmr_Module_lock,
		    FIVMR_PR_CRITICAL);
}

static void setLastError(void) {
    const char *e=dlerror();
    LOG(15,("setLastError has e = %s",e));
    fivmr_tsprintf("%s",e);
}

fivmr_ModuleHandle fivmr_Module_load(const char *path) {
    fivmr_ModuleHandle result=dlopen(path,RTLD_LAZY|RTLD_LOCAL);
    if (result==NULL) {
        setLastError();
	LOG(3,("dlopen on %s failed because: %s",
	       path,fivmr_Module_getLastError()));
    }
    return result;
}

bool fivmr_Module_unload(fivmr_ModuleHandle handle) {
    if (dlclose(handle)!=0) {
        setLastError();
        return false;
    } else {
        return true;
    }
}

void *fivmr_Module_lookup(fivmr_ModuleHandle handle,
                          const char *name) {
    void *result=dlsym(handle,name);
    if (result==NULL) {
        setLastError();
	LOG(3,("dlsym on %p/%s failed because: %s",
	       handle,name,fivmr_Module_getLastError()));
    }
    return result;
}

void *fivmr_Module_getFunction(fivmr_ModuleHandle handle,
			       const char *funcName) {
    /* currently we don't have any underscore-adding hacks, but we may in the
       future. */
    return fivmr_Module_lookup(handle,funcName);
}

const char *fivmr_Module_getLastError(void) {
    return fivmr_getThreadStringBuf();
}

#endif
