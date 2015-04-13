/*
 * fivmr_jnilib.c
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
#if !FIVMR_STATIC_JNI && FIVMR_DYN_LOADING

#include "fivmr.h"

#define SHORTNAME_MAX_LEN 128
#define LONGNAME_MAX_LEN 192

void fivmr_JNILib_load(fivmr_ThreadState *ts,
                       fivmr_JNILib **list,
		       const char *filename,
		       fivmr_JNILib **result,
		       bool *freshlyLoaded) {
    fivmr_ModuleHandle module=fivmr_Module_zero();
    fivmr_JNILib *cur;
    void *onLoadHook=NULL;
    
    /* check if it was already loaded */
    for (cur=*list;cur!=NULL;cur=cur->next) {
	if (!strcmp(cur->filename,filename)) {
	    *result=cur;
	    *freshlyLoaded=false;
	    return;
	}
    }
    
    fivmr_Lock_lock(&fivmr_Module_lock);
    module=fivmr_Module_load(filename);
    if (module!=NULL) {
	onLoadHook=fivmr_Module_getFunction(module,"JNI_OnLoad");
    }
    fivmr_Lock_unlock(&fivmr_Module_lock);
    
    if (module==NULL) {
	*result=NULL;
	*freshlyLoaded=false;
	return;
    }
    
    cur=malloc(sizeof(fivmr_JNILib));
    
    cur->filename=strdup(filename);
    cur->module=module;
    cur->onLoadHook=onLoadHook;
    cur->initialized=false;

    cur->next=*list;
    *list=cur;
    
    *result=cur;
    *freshlyLoaded=true;
}

static void *lookupImpl(fivmr_JNILib *lib,
			const char *shortName,
			const char *longName) {
    void *result;
    LOG(7,("doing lookup of %s and %s in %s",shortName,longName,lib->filename));
    result=fivmr_Module_getFunction(lib->module,shortName);
    if (result==NULL) {
	result=fivmr_Module_getFunction(lib->module,longName);
    }
    return result;
}

void *fivmr_JNILib_lookupOne(fivmr_ThreadState *ts,
                             fivmr_JNILib *lib,
			     fivmr_MethodRec *mr) {
    char shortName[SHORTNAME_MAX_LEN];
    char longName[LONGNAME_MAX_LEN];
    void *result=NULL;
    if (lib->initialized) {
	fivmr_makeJNIFuncName(ts,shortName,sizeof(shortName),mr,false);
	if (fivmr_hasException(ts)) return NULL;
	fivmr_makeJNIFuncName(ts,longName,sizeof(longName),mr,true);
	if (fivmr_hasException(ts)) return NULL;
	LOG(3,("Doing lookupOne of %s/%s in %s",shortName,longName,lib->filename));
	fivmr_Lock_lock(&fivmr_Module_lock);
	result=lookupImpl(lib,shortName,longName);
	fivmr_Lock_unlock(&fivmr_Module_lock);
    }
    return result;
}

void *fivmr_JNILib_lookup(fivmr_ThreadState *ts,
                          fivmr_JNILib *lib,
			  fivmr_MethodRec *mr) {
    char shortName[SHORTNAME_MAX_LEN];
    char longName[LONGNAME_MAX_LEN];
    void *result=NULL;
    fivmr_makeJNIFuncName(ts,shortName,sizeof(shortName),mr,false);
    if (fivmr_hasException(ts)) return NULL;
    fivmr_makeJNIFuncName(ts,longName,sizeof(longName),mr,true);
    if (fivmr_hasException(ts)) return NULL;
    LOG(3,("Doing lookup of %s/%s in %s",shortName,longName,lib->filename));
    fivmr_Lock_lock(&fivmr_Module_lock);
    for (;lib!=NULL;lib=lib->next) {
	if (lib->initialized) {
	    result=lookupImpl(lib,shortName,longName);
	    if (result!=NULL) break;
	}
    }
    fivmr_Lock_unlock(&fivmr_Module_lock);
    return result;
}

#endif

