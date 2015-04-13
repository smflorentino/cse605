/*
 * fivmr_typedata.c
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

/* FIXME: for every type in the root context referenced in the app context, we
   should have a stub in the app context. */

fivmr_TypeData *fivmr_StaticTypeContext_find(fivmr_StaticTypeContext *ctx,
                                             const char *name) {
    int low=ctx->typeOffset;
    int high=low+ctx->nTypes;
    fivmr_TypeData **list=ctx->payload->typeList;
    while (high>low) {
	int mid;
	int cmpResult;
	fivmr_TypeData *cur;
	
	mid=(high-low)/2+low;
	cur=list[mid];
	cmpResult=strcmp(name,cur->name);
	if (cmpResult==0) {
	    return cur;
	} else if (cmpResult>0) {
	    low=mid+1;
	} else /* cmpResult<0 */ {
	    high=mid;
	}
    }
    return NULL;
}

fivmr_TypeStub *fivmr_StaticTypeContext_findStub(fivmr_StaticTypeContext *ctx,
                                                 const char *name) {
    int low=ctx->stubOffset;
    int high=low+ctx->nStubs;
    fivmr_TypeStub *list=ctx->payload->stubList;
    while (high>low) {
	int mid;
	int cmpResult;
	fivmr_TypeStub *cur;
	
	mid=(high-low)/2+low;
	cur=list+mid;
	cmpResult=strcmp(name,cur->name);
	if (cmpResult==0) {
	    return cur;
	} else if (cmpResult>0) {
	    low=mid+1;
	} else /* cmpResult<0 */ {
	    high=mid;
	}
    }
    return NULL;
}

void fivmr_TypeContext_boot(fivmr_VM *vm,
                            fivmr_TypeContext *ctx) {
    fivmr_Lock_init(&ctx->treeLock,
                    fivmr_Priority_bound(FIVMR_PR_INHERIT,
                                         vm->maxPriority));
    fivmr_Lock_init(&ctx->loadSerializerLock,
                    fivmr_Priority_bound(FIVMR_PR_INHERIT,
                                         vm->maxPriority));
    ftree_Tree_init(&ctx->dynamicTypeTree,
                    fivmr_TypeData_compareKey);
    ftree_Tree_init(&ctx->stubTree,
                    fivmr_TypeData_compareKey);
    
    fivmr_JNI_init(ctx,vm);
}

fivmr_TypeContext *fivmr_TypeContext_create(fivmr_VM *vm,
                                            fivmr_Handle *classLoader) {
    fivmr_TypeContext *result;
    fivmr_ThreadState *ts;
    fivmr_Object classLoaderObj;
    
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    
    result=freg_region_create(sizeof(fivmr_TypeContext));
    fivmr_assert(result!=NULL);
    
    bzero(result,sizeof(fivmr_TypeContext));

    result->st.typeOffset=0;
    result->st.nTypes=0;
    result->st.stubOffset=0;
    result->st.nStubs=0;
    result->st.payload=vm->payload;
    
    fivmr_TypeContext_boot(vm,result);
    
    result->vm=vm;
    result->aux=NULL;
    
    fivmr_Lock_lock(&vm->typeDataLock);
    
    if (vm->nDynContexts == vm->dynContextsSize) {
        fivmr_TypeContext **newDynContexts;
        int32_t newDynContextsSize;
        
        newDynContextsSize=(vm->dynContextsSize+1)<<1;
        
        newDynContexts=
            fivmr_reallocAssert(
                vm->dynContexts,
                sizeof(fivmr_TypeContext*)*newDynContextsSize);
        
        memcpy(newDynContexts,vm->dynContexts,
               sizeof(fivmr_TypeContext*)*vm->dynContextsSize);
        vm->dynContexts=newDynContexts;
        vm->dynContextsSize=newDynContextsSize;
    }
    
    vm->dynContexts[vm->nDynContexts++]=result;
    
    fivmr_Lock_unlock(&vm->typeDataLock);

    fivmr_ThreadState_goToJava(ts);
    
    classLoaderObj=fivmr_Handle_get(classLoader);
    fivmr_GC_mark(ts,classLoaderObj);
    result->classLoader=classLoaderObj;
    
    fivmr_ThreadState_goToNative(ts);

    return result;
}

void fivmr_TypeContext_destroy(fivmr_TypeContext *ctx) {
    /* FIXME: bunch of stuff to free that this doesn't include... */
    
    fivmr_Lock_destroy(&ctx->treeLock);
    fivmr_Lock_destroy(&ctx->loadSerializerLock); 
}

fivmr_TypeData *fivmr_TypeContext_findKnown(fivmr_TypeContext *ctx,
                                            const char *name) {
    fivmr_TypeData *result;
    ftree_Node *node;
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    
    LOG(2,("Looking for known type called %s",name));
    
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));

    result=fivmr_StaticTypeContext_find(&ctx->st,name);
    if (result!=NULL) {
        return result;
    }
    
    if (ctx->vm->baseContexts[0] != ctx) {
        result=fivmr_StaticTypeContext_find(&ctx->vm->baseContexts[0]->st,name);
        if (result!=NULL &&
            ((result->flags & FIVMR_TBF_OVERRIDE_ALL) ||
             (ctx->vm->baseContexts[1] == ctx &&
              (FIVMR_ROOT_OVERRIDES_ALL_APP(&vm->settings) ||
               (result->flags & FIVMR_TBF_OVERRIDE_APP))))) {
            LOG(2,("returning %p",result));
            return result;
        }
    }
    
    /* at this point if the type was primitive we would have found it.
       so assert that we're looking for a ref type. */
    
    fivmr_assert(name[0]=='L' || name[0]=='[');
    
    fivmr_ThreadState_goToNative(ts);
    fivmr_Lock_lock(&ctx->treeLock);
    fivmr_ThreadState_goToJava(ts);
    
    node=ftree_Tree_findFast(&ctx->dynamicTypeTree,
                             (uintptr_t)(void*)name,
                             fivmr_TypeData_compareKey);
    LOG(2,("Found node %p in dynamic type tree",node));
    if (node==NULL) {
        node=ftree_Tree_findFast(&ctx->stubTree,
                                 (uintptr_t)(void*)name,
                                 fivmr_TypeData_compareKey);
        LOG(2,("Found node %p in stub tree",node));
        if (node==NULL) {
            result=NULL;
        } else {
            fivmr_TypeStub *st=(fivmr_TypeStub*)(void*)node->value;
            LOG(2,("Found stub %s (%p) in stub tree",st->name,st));
            fivmr_assert(!strcmp(st->name,name));
            result=fivmr_TypeStub_tryGetTypeData(st);
        }
    } else {
        result=(fivmr_TypeData*)(void*)node->value;
    }
    
    fivmr_Lock_unlock(&ctx->treeLock);
    
    if (result==NULL && name[0]=='[') {
        /* try to find the base type */
        
        const char *className;
        int32_t depth;
        fivmr_TypeData *class;
        fivmr_TypeData *cur;
        
        className=name;
        depth=0;
        
        while (className[0]=='[') {
            className++;
            depth++;
        }
        
        fivmr_assert(className[0]!=0);
        fivmr_assert(className[0]!='[');
        
        class=fivmr_TypeContext_findKnown(ctx,className);
        
        if (class!=NULL) {
            cur=class;
            while (depth-->0 && cur!=NULL) {
                cur=cur->arrayType;
            }
            
            result=cur;
        }
    }
    
    LOG(2,("returning %p (2)",result));
    return result;
}

/* NOTE: never hold type context locks or type data locks when calling this */
fivmr_TypeData *fivmr_TypeContext_find(fivmr_TypeContext *ctx,
                                       const char *name) {
    fivmr_TypeData *result;
    fivmr_ThreadState *ts;
    
    ts=fivmr_ThreadState_get(ctx->vm);
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    /* this should not be called before the class loaders are initialized */
    if (FIVMR_ASSERTS_ON) {
        if (ctx->vm->baseContexts[0] == ctx) {
            fivmr_assert(ctx->classLoader==0);
        } else {
            fivmr_assert(ctx->classLoader!=0);
        }
    }
    
    result=fivmr_TypeContext_findKnown(ctx,name);
    if (result==NULL) {
        if (name[0]=='[') {
            /* if the name corresponds to an array type then we must create it
               in the context that its base element type belongs to */
            const char *className;
            int32_t depth;

            className=name;
            depth=0;

            while (className[0]=='[') {
                className++;
                depth++;
            }
            
            fivmr_assert(className[0]!=0);
            fivmr_assert(className[0]!='[');
            
            result=fivmr_TypeContext_find(ctx,className);
            
            if (result!=NULL) {
                fivmr_assert(ts->curException==0);
                while (depth-->0) {
                    result=fivmr_TypeData_makeArray(result);
                    if (result==NULL) {
                        fivmr_throwNoClassDefFoundError_inJava(ts,name,NULL);
                        break;
                    }
                }
            } else {
                fivmr_assert(ts->curException!=0);
            }
        } else if (name[0]=='L') {
            /* if the name corresponds to something else then we should ask
               Java to load it */
            fivmr_Object klass;

            /* FIXME: find a way to do this without holding any locks. */
            
            fivmr_ThreadState_goToNative(ts);
            fivmr_Lock_lock(&ctx->loadSerializerLock);
            fivmr_ThreadState_goToJava(ts);
            
            klass=fivmRuntime_loadClass(ts,ctx,ctx->classLoader,name);
            if (klass!=0) {
                fivmr_TypeStub *myStub;
                bool res;
                fivmr_assert(ts->curException==0);
                result=fivmr_TypeData_fromClass(ts,klass);
                
                myStub=fivmr_TypeContext_findStub(ctx,name);
                res=fivmr_TypeStub_union(myStub,(fivmr_TypeStub*)result);
                fivmr_assert(res);
            } else {
                fivmr_assert(ts->curException!=0);
                result=NULL;
            }
            
            fivmr_Lock_unlock(&ctx->loadSerializerLock);
        } else {
            /* the type is primitive - so we would have found it if it had existed */
            fivmr_assert(false);
        }
    }
    
    fivmr_assert(ts->curExceptionHandle==NULL);
    fivmr_assert((result==NULL)==(ts->curException!=0));
    
    return result;
}

fivmr_TypeStub *fivmr_TypeContext_findStub(fivmr_TypeContext *ctx,
                                           const char *name) {
    fivmr_TypeData *td;
    fivmr_TypeStub *ts;
    ftree_Node *node;
    
    fivmr_assert(fivmr_ThreadState_isInJava(fivmr_ThreadState_get(ctx->vm)));

    td=fivmr_TypeContext_findKnown(ctx,name);
    if (td!=NULL) {
        return (fivmr_TypeStub*)td;
    }
    
    ts=fivmr_StaticTypeContext_findStub(&ctx->st,name);
    if (ts!=NULL) {
        return ts;
    }
    
    fivmr_ThreadState_goToNative(fivmr_ThreadState_get(ctx->vm));
    fivmr_Lock_lock(&ctx->treeLock);
    fivmr_ThreadState_goToJava(fivmr_ThreadState_get(ctx->vm));
    
    node=ftree_Tree_findFast(&ctx->stubTree,
                             (uintptr_t)(void*)name,
                             fivmr_TypeData_compareKey);
    if (node==NULL) {
        ts=fivmr_mallocAssert(sizeof(fivmr_TypeStub));
        ts->state=FIVMR_MS_INVALID;
        ts->forward=NULL;
        ts->flags=FIVMR_TBF_STUB;
        ts->name=strdup(name);
        fivmr_assert(ts->name!=NULL);
        ts->context=&ctx->st;
        ts->inited=0;
        
        node=fivmr_mallocAssert(sizeof(ftree_Node));

        LOG(2,("Adding stub %s (%p) to tree for context %p; node at %p",
               name,ts,ctx,node));
        
        ftree_Node_init(node,
                        (uintptr_t)(void*)ts->name,
                        (uintptr_t)(void*)ts);
        ftree_Tree_add(&ctx->stubTree,node);
    } else {
        ts=(fivmr_TypeStub*)(void*)node->value;
        fivmr_assert(ts!=NULL);
    }
    
    fivmr_Lock_unlock(&ctx->treeLock);
    
    fivmr_assert(ts!=NULL);
    
    return ts;
}

static fivmr_TypeData *findClassImpl(fivmr_TypeContext *ctx,
                                     const char *className,
                                     char packageSeparator,
                                     fivmr_TypeData *(*finder)(fivmr_TypeContext *ctx,
                                                               const char *name)) {
    char *typeName;
    size_t classNameLen=strlen(className);
    size_t typeNameSize=classNameLen+3;
    size_t j;
    typeName=alloca(typeNameSize);
    snprintf(typeName,typeNameSize,"L%s;",className);
    if (packageSeparator!='/') {
	for (j=classNameLen;j-->0;) {
	    if (typeName[j+1]==packageSeparator) {
		typeName[j+1]='/';
	    }
	}
    }
    return finder(ctx,typeName);
}

fivmr_TypeData *fivmr_TypeContext_findClass(fivmr_TypeContext *ctx,
                                            const char *className,
                                            char packageSeparator) {
    return findClassImpl(ctx,className,packageSeparator,
                         fivmr_TypeContext_find);
}

fivmr_TypeData *fivmr_TypeContext_findClassKnown(fivmr_TypeContext *ctx,
                                                 const char *className,
                                                 char packageSeparator) {
    return findClassImpl(ctx,className,packageSeparator,
                         fivmr_TypeContext_findKnown);
}

fivmr_TypeStub *fivmr_TypeStub_find(fivmr_TypeStub *start) {
    fivmr_TypeStub *cur=start;
    fivmr_TypeStub *second=(fivmr_TypeStub*)cur->forward;
    fivmr_TypeStub *next=second;
    
    LOG(2,("Find from start = %s (%p, %d), second = %p",
           start->name,start,start->flags,second));
    
    for (;;) {
        if (next==cur || next==NULL) {
            break;
        }
        cur=next;
        next=(fivmr_TypeStub*)cur->forward;
    }
    
    fivmr_assert(cur!=NULL);
    
    LOG(2,("Found result = %s (%p, %d)",
           cur->name,cur,cur->flags));

    if (cur!=start && cur!=second) {
        fivmr_cas_weak((uintptr_t*)&start->forward,
                       (uintptr_t)second,
                       (uintptr_t)cur);
    }
    
    return cur;
}

fivmr_TypeStub *fivmr_TypeStub_find2(fivmr_TypeStub **startPtr) {
    fivmr_TypeStub *start=*startPtr;
    fivmr_TypeStub *result=fivmr_TypeStub_find(start);
    if (result!=start) {
        fivmr_cas_weak((uintptr_t*)startPtr,
                       (uintptr_t)start,
                       (uintptr_t)result);
    }
    return result;
}

fivmr_TypeData *fivmr_TypeStub_tryGetTypeData(fivmr_TypeStub *start) {
    start=fivmr_TypeStub_find(start);
    if ((start->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_STUB) {
        return NULL;
    } else {
        return (fivmr_TypeData*)start;
    }
}

fivmr_TypeData *fivmr_TypeStub_tryGetTypeData2(fivmr_TypeStub **startPtr) {
    fivmr_TypeStub *cur=fivmr_TypeStub_find2(startPtr);
    if ((cur->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_STUB) {
        return NULL;
    } else {
        return (fivmr_TypeData*)cur;
    }
}

/* FIXME: just use a global lock for unioning!  we'll need it once we implement
   the idea of pushing resolution points up from the children to the root. */
bool fivmr_TypeStub_union(fivmr_TypeStub *aStart,
                          fivmr_TypeStub *bStart) {
    LOG(2,("Attempting to union %s (%p, %d) with %s (%p, %d)",
           aStart->name,aStart,aStart->flags,bStart->name,bStart,bStart->flags));
    
    fivmr_assert(!strcmp(aStart->name,bStart->name));

    for (;;) {
        fivmr_TypeStub *a,*b;
        
        a=fivmr_TypeStub_find(aStart);
        b=fivmr_TypeStub_find(bStart);
        
        LOG(2,("Found %s (%p, %d) and %s (%p, %d)",
               a->name,a,a->flags,b->name,b,b->flags));
        
        fivmr_assert(!strcmp(a->name,b->name));

        if (a==b) {
            return true;
        } else if ((a->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB) {
            if ((b->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB) {
                return false; /* loading constraint cannot be satisfied */
            } else {
                if (fivmr_cas_weak((uintptr_t*)&b->forward,
                                   (uintptr_t)NULL,
                                   (uintptr_t)a)) {
                    return true;
                }
            }
        } else {
            if (fivmr_cas_weak((uintptr_t*)&a->forward,
                               (uintptr_t)NULL,
                               (uintptr_t)b)) {
                return true;
            }
        }
        
        fivmr_spin_fast();
    }
}

bool fivmr_TypeStub_unionParams(int32_t nparams1,fivmr_TypeStub **params1,
                                int32_t nparams2,fivmr_TypeStub **params2) {
    int32_t i;
    if (nparams1!=nparams2) {
        return false;
    }
    for (i=0;i<nparams1;++i) {
        if (!fivmr_TypeStub_union(params1[i],params2[i])) {
            return false;
        }
    }
    return true;
}

bool fivmr_TypeStub_eq(fivmr_TypeStub *aStart,
                       fivmr_TypeStub *bStart) {
    fivmr_TypeStub *aLast=NULL,*bLast=NULL;
    for (;;) {
        fivmr_TypeStub *a,*b;
        a=fivmr_TypeStub_find(aStart);
        b=fivmr_TypeStub_find(bStart);
        if (a==b) {
            return true;
        }
        if (a==aLast && b==bLast) {
            return false;
        }
        aLast=a;
        bLast=b;
        fivmr_fence();
    }
}

bool fivmr_TypeStub_eq2(fivmr_TypeStub *a,fivmr_TypeStub *b) {
    return !strcmp(a->name,b->name);
}

bool fivmr_TypeStub_paramsEq(int32_t nparams1,fivmr_TypeStub **params1,
                             int32_t nparams2,fivmr_TypeStub **params2) {
    int32_t i;
    if (nparams1!=nparams2) {
        return false;
    }
    for (i=0;i<nparams1;++i) {
        if (!fivmr_TypeStub_eq(params1[i],params2[i])) {
            return false;
        }
    }
    return true;
}

bool fivmr_TypeStub_paramsEq2(int32_t nparams1,fivmr_TypeStub **params1,
                              int32_t nparams2,fivmr_TypeStub **params2) {
    int32_t i;
    if (nparams1!=nparams2) {
        return false;
    }
    for (i=0;i<nparams1;++i) {
        if (!fivmr_TypeStub_eq2(params1[i],params2[i])) {
            return false;
        }
    }
    return true;
}

/* NOTE: never hold type context locks or type data locks when calling this */
fivmr_TypeData *fivmr_TypeStub_getTypeData(fivmr_TypeStub *start) {
    fivmr_TypeData *result;
    fivmr_ThreadState *ts;
    
    ts=fivmr_ThreadState_get(fivmr_TypeStub_getContext(start)->vm);
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    result=fivmr_TypeStub_tryGetTypeData(start);
    if (result==NULL) {
        result=fivmr_TypeContext_find(fivmr_TypeStub_getContext(start),
                                      start->name);
        if (result==NULL) {
            fivmr_assert(ts->curException!=0);
            return NULL;
        }
        fivmr_assert(ts->curException==0);
        
        LOG(2,("While looking for %s (%p) in %p, found %s (%p) in %p.",
               start->name,start,fivmr_TypeStub_getContext(start),
               result->name,result,fivmr_TypeData_getContext(result)));
        
        // make sure that this type stub is unioned with whatever we got,
        // and that start->forward points to the typedata (it's an
        // optimization that the JIT will assume we have done).
        if (start->forward!=result) {
            if (fivmr_TypeStub_union(start,(fivmr_TypeStub*)result)) {
                fivmr_TypeStub_find(start); /* make it so that state->forward
                                               points to result */
                fivmr_assert(start->forward==result);
            } else {
                LOG(1,("Union failed between %s (%p) in %p and %s (%p) in %p.",
                       start->name,start,fivmr_TypeStub_getContext(start),
                       result->name,result,fivmr_TypeData_getContext(result)));
                
                result=fivmr_TypeStub_tryGetTypeData(start);
                fivmr_assert(result!=NULL);
            }
        }
    }
    return result;
}

fivmr_TypeData *fivmr_TypeData_forHandle(fivmr_Handle *h) {
    return fivmr_TypeData_forObject(&h->vm->settings,fivmr_Handle_get(h));
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch(fivmr_TypeData *td,
                                                       const char *name,
                                                       const char *sig) {
    int i;
    for (i=0;i<td->numMethods;++i) {
        if (!(td->methods[i]->flags&FIVMR_BF_STATIC) &&
            !strcmp(td->methods[i]->name,name) &&
            fivmr_MethodRec_matchesSig(td->methods[i],sig)) {
            return td->methods[i];
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch2(fivmr_TypeData *td,
                                                        const char *name,
                                                        fivmr_TypeStub *result,
                                                        int32_t nparams,
                                                        fivmr_TypeStub **params) {
    int i;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            fivmr_MethodSig_eq2(mr,name,result,nparams,params)) {
            return mr;
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch3(fivmr_TypeData *td,
                                                        const char *name,
                                                        fivmr_TypeStub *result,
                                                        int32_t nparams,
                                                        fivmr_TypeStub **params) {
    int i;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            fivmr_MethodSig_eq4(mr,name,result,nparams,params)) {
            return mr;
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface(fivmr_VM *vm,
                                                      fivmr_TypeData *td,
                                                      const char *name,
                                                      const char *sig) {
    for (;td!=vm->payload->td_top;td=td->parent) {
        fivmr_MethodRec *result=fivmr_TypeData_findInstMethodNoSearch(td,name,sig);
        if (result!=NULL) {
            return result;
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface2(fivmr_VM *vm,
                                                       fivmr_TypeData *td,
                                                       const char *name,
                                                       fivmr_TypeStub *result,
                                                       int32_t nparams,
                                                       fivmr_TypeStub **params) {
    for (;td!=vm->payload->td_top;td=td->parent) {
        fivmr_MethodRec *mr=
            fivmr_TypeData_findInstMethodNoSearch2(
                td,name,result,nparams,params);
        if (mr!=NULL) {
            return mr;
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface3(fivmr_VM *vm,
                                                       fivmr_TypeData *td,
                                                       const char *name,
                                                       fivmr_TypeStub *result,
                                                       int32_t nparams,
                                                       fivmr_TypeStub **params) {
    for (;td!=vm->payload->td_top;td=td->parent) {
        fivmr_MethodRec *mr=
            fivmr_TypeData_findInstMethodNoSearch3(
                td,name,result,nparams,params);
        if (mr!=NULL) {
            return mr;
        }
    }
    return NULL;
}

typedef struct {
    const char *name;
    const char *sig;
} FindInstMethodInIfaceData;

static uintptr_t findInstMethodInIface_cback(fivmr_TypeData *startTD,
                                             fivmr_TypeData *curTD,
                                             uintptr_t arg) {
    FindInstMethodInIfaceData *fimiidata=(FindInstMethodInIfaceData*)(void*)arg;
    return (uintptr_t)(void*)fivmr_TypeData_findInstMethodNoSearch(
        curTD,fimiidata->name,fimiidata->sig);
}

static fivmr_MethodRec *findInstMethodInIface(fivmr_VM *vm,
                                              fivmr_TypeData *td,
                                              const char *name,
                                              const char *sig) {
    FindInstMethodInIfaceData fimiidata;
    fimiidata.name=name;
    fimiidata.sig=sig;
    return (fivmr_MethodRec*)(void*)fivmr_TypeData_forAllAncestorsInclusive(
        td,findInstMethodInIface_cback,(uintptr_t)(void*)&fimiidata);
}

fivmr_MethodRec *fivmr_TypeData_findInstMethod(fivmr_VM *vm,
                                               fivmr_TypeData *td,
                                               const char *name,
                                               const char *sig) {
    fivmr_MethodRec *result=NULL;
    switch (td->flags&FIVMR_TBF_TYPE_KIND) {
    case FIVMR_TBF_INTERFACE:
    case FIVMR_TBF_ANNOTATION:
        result=findInstMethodInIface(vm,td,name,sig);
        if (result==NULL) {
            result=fivmr_TypeData_findInstMethodNoIface(vm,
                                                        vm->payload->td_Object,
                                                        name,sig);
        }
        break;
    case FIVMR_TBF_ARRAY:
        result=fivmr_TypeData_findInstMethodNoIface(vm,
                                                    vm->payload->td_Object,
                                                    name,sig);
        break;
    case FIVMR_TBF_ABSTRACT:
    case FIVMR_TBF_VIRTUAL:
    case FIVMR_TBF_FINAL:
        result=fivmr_TypeData_findInstMethodNoIface(vm,td,name,sig);
        if (result==NULL) {
            for (;td!=vm->payload->td_top;td=td->parent) {
                unsigned i;
                for (i=0;i<td->nSuperInterfaces;++i) {
                    result=findInstMethodInIface(vm,td->superInterfaces[i],name,sig);
                    if (result!=NULL) {
                        break;
                    }
                }
            }
        }
        break;
    default:
        break;
    }
    return result;
}

fivmr_MethodRec *fivmr_TypeData_findStaticMethod(fivmr_VM *vm,
                                                 fivmr_TypeData *td,
						 const char *name,
						 const char *sig) {
    fivmr_MethodRec *result=fivmr_TypeData_findMethod(vm,td,name,sig);
    if (result!=NULL && (result->flags&FIVMR_BF_STATIC)) {
	return result;
    } else {
	return NULL;
    }
}

fivmr_MethodRec *fivmr_TypeData_findMethod(fivmr_VM *vm,
                                           fivmr_TypeData *td,
					   const char *name,
					   const char *sig) {
    int i;
    for (i=0;i<td->numMethods;++i) {
	if (!strcmp(td->methods[i]->name,name) &&
	    fivmr_MethodRec_matchesSig(td->methods[i],sig)) {
	    return td->methods[i];
	}
    }
    return NULL;
}

fivmr_FieldRec *fivmr_TypeData_findField(fivmr_TypeData *td,
                                         const char *name,
                                         const char *sig) {
    int i;
    for (i=0;i<td->numFields;++i) {
        if (!strcmp(td->fields[i].name,name) &&
            !strcmp(td->fields[i].type->name,sig)) {
            fivmr_FieldRec *result=td->fields+i;
            return result;
        }
    }
    return NULL;
}

fivmr_FieldRec *fivmr_TypeData_findStaticField(fivmr_TypeData *td,
                                               const char *name,
                                               const char *sig) {
    fivmr_FieldRec *result=fivmr_TypeData_findField(td,name,sig);
    if (result==NULL || !(result->flags&FIVMR_BF_STATIC)) {
        return NULL;
    } else {
        return result;
    }
}

fivmr_FieldRec *fivmr_TypeData_findInstFieldNoSearch(fivmr_TypeData *td,
                                                     const char *name,
                                                     const char *sig) {
    fivmr_FieldRec *result=fivmr_TypeData_findField(td,name,sig);
    if (result==NULL || (result->flags&FIVMR_BF_STATIC)) {
        return NULL;
    } else {
        return result;
    }
}

fivmr_FieldRec *fivmr_TypeData_findInstField(fivmr_TypeData *td,
                                             const char *name,
                                             const char *sig) {
    fivmr_TypeData *top=fivmr_TypeData_getVM(td)->payload->td_top;
    for (;td!=top;td=td->parent) {
        fivmr_FieldRec *result=fivmr_TypeData_findInstFieldNoSearch(td,name,sig);
        if (result!=NULL) {
            return result;
        }
    }
    return NULL;
}

static bool checkInit_impl(fivmr_ThreadState *ts,
                           fivmr_TypeData *td) {
    /* FIXME: have an option to log initialization of classes, and make it possible
       to feed that into fivmc and have it generate a preinitialization code. */
    LOG(3,("checkInit for %p, %s",td,td->name));
    
    fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_DONE));
    fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    if (td->curIniter==ts) {
	/* we're currently initializing this type data, pretend as if it's already
	   initialized. */
	LOG(8,("%s currently being initialized.",td->name));
	return true;
    }
    for (;;) {
	if (td->inited==1) {
	    /* already initialized */
	    LOG(8,("%s already initialized.",td->name));
	    return true;
	} else if (td->inited==512) {
	    LOG(8,("%s already failed initialization.",td->name));
	    fivmr_throwNoClassDefFoundError_inJava(ts,td->name,"The class is present but its initialization has already failed with an ExceptionInInitializerError");
	    return false;
	} else if (fivmr_cas32_weak(&td->inited,
				    0,
				    256)) {
	    td->curIniter=ts;
	    fivmr_fence();
	    if (td->parent!=ts->vm->payload->td_top) {
		checkInit_impl(ts,td->parent);
	    }
	    if (!ts->curException) {
		fivmr_MethodRec *clinit;
		int32_t i;
		for (i=0;i<td->nSuperInterfaces;++i) {
		    if (!checkInit_impl(ts,td->superInterfaces[i])) {
			break;
		    }
		}
		if (!ts->curException) {
                    fivmr_MemoryArea *currentArea=NULL; /* make GCC happy */
                    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
                        currentArea=ts->gc.currentArea;
                        if(currentArea!=&ts->vm->gc.immortalMemoryArea) {
                            fivmr_MemoryArea_setCurrentArea(
                                ts, &ts->vm->gc.immortalMemoryArea);
                            fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                                              FIVMR_FLOWLOG_SUBTYPE_ENTER,
                                              (uintptr_t)&ts->vm->gc.immortalMemoryArea);
                        }
                    }
		    clinit=fivmr_TypeData_findStaticMethod(ts->vm,td,"<clinit>","()V");
		    if (clinit==NULL) {
			/* awesome, nothing to do. */
			LOG(8,("%s doesn't have a clinit method (success).",td->name));
			fivmr_fence();
			td->inited=1;
		    } else {
			LOG(3,("calling %s.",fivmr_MethodRec_describe(clinit)));
		
			fivmr_MethodRec_call(
			    clinit,ts,NULL,NULL,NULL,
			    FIVMR_CM_DISPATCH);
			if (ts->curException) {
			    fivmr_Object e=ts->curException;
			    ts->curException=0;
			    LOG(8,("%s threw an exception: %s.",
				   fivmr_MethodRec_describe(clinit),
				   fivmr_TypeData_forObject(&ts->vm->settings,e)->name));
			    fivmr_throwExceptionInInitializerError_inJava(ts,e,td);
			    fivmr_assert(ts->curException!=0);
			} else {
			    LOG(8,("%s returned.",fivmr_MethodRec_describe(clinit)));
			}
		    }
                    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
                        if (currentArea!=ts->gc.currentArea) {
                            fivmr_MemoryArea_setCurrentArea(ts, currentArea);
                            fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                                              FIVMR_FLOWLOG_SUBTYPE_ENTER,
                                              (uintptr_t)currentArea);
                        }
                    }
		}
	    }
	    fivmr_fence();
	    if (ts->curException) {
		LOG(8,("%s failed to initialize, got exception along the way.",td->name));
		td->inited=512;
	    } else {
		LOG(8,("%s initialized.",td->name));
		td->inited=1;
	    }
	    td->curIniter=NULL;
            /* FIXME: should we perhaps use some other lock? */
            fivmr_Lock_lockedBroadcast(&ts->vm->thunkingLock);
	    return !ts->curException;
	} else {
            /* FIXME: need priority boosting... */
            fivmr_ThreadState_goToNative(ts);
            fivmr_Lock_lock(&ts->vm->thunkingLock);
            while (td->inited!=1 && td->inited!=512) {
                fivmr_Lock_wait(&ts->vm->thunkingLock);
            }
            fivmr_Lock_unlock(&ts->vm->thunkingLock);
            fivmr_ThreadState_goToJava(ts);
        }
    }
}

bool fivmr_TypeData_checkInit(fivmr_ThreadState *ts,
                              fivmr_TypeData *td) {
    bool result;
#if FIVMR_PROFILE_REFLECTION
    fivmr_Nanos before=fivmr_curTime();
#endif
    result=checkInit_impl(ts,td);
#if FIVMR_PROFILE_REFLECTION
    fivmr_PR_initTime+=fivmr_curTime()-before;
#endif
    return result;
}

fivmr_TypeData *fivmr_TypeStub_resolve(fivmr_ThreadState *ts,
                                       fivmr_TypeStub *st) {
    if ((st->flags&FIVMR_TBF_RESOLUTION_DONE)) {
        fivmr_TypeData *td=st->forward;
        fivmr_assert(td!=NULL);
        fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB);
        fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_DONE));
        fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
        return td;
    } else {
        fivmr_TypeData *td;
        td=fivmr_TypeStub_getTypeData(st);
        if (td==NULL) {
            fivmr_assert(ts->curException!=0);
            return NULL;
        }
        fivmr_assert(ts->curException==0);
        if (!fivmr_TypeData_resolve(td)) {
            fivmr_throwLinkageError_inJava(
                ts,
                fivmr_tsprintf("Could not link and resolve %s",td->name));
            return NULL;
        }
        
        fivmr_fence();

        fivmr_BitField_setAtomic(&st->flags,
                                 FIVMR_TBF_RESOLUTION_DONE,
                                 FIVMR_TBF_RESOLUTION_DONE);
        return td;
    }
}

fivmr_TypeData *fivmr_TypeStub_checkInit(fivmr_ThreadState *ts,
                                         fivmr_TypeStub *st) {
    if (st->inited==1) {
        fivmr_TypeData *td=st->forward;
        fivmr_assert(td!=NULL);
        fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB);
        fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_DONE));
        fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
        fivmr_assert(td->inited==1);
        return td;
    } else {
        fivmr_TypeData *td=fivmr_TypeStub_getTypeData(st);
        if (td==NULL) {
            fivmr_assert(ts->curException!=0);
            return NULL;
        }
        fivmr_assert(ts->curException==0);
        if (!fivmr_TypeData_resolve(td)) {
            fivmr_throwLinkageError_inJava(
                ts,
                fivmr_tsprintf("Could not link and resolve %s",td->name));
            return NULL;
        }
        if (!fivmr_TypeData_checkInit(ts,td)) {
            return NULL;
        }
        
        fivmr_fence();
        
        fivmr_BitField_setAtomic(&st->flags,
                                 FIVMR_TBF_RESOLUTION_DONE,
                                 FIVMR_TBF_RESOLUTION_DONE);
        st->inited=1;
        return td;
    }
}

void fivmr_TypeData_checkInitEasy(fivmr_ThreadState *ts,
                                  const char *name) {
    fivmr_TypeData *td;
    bool result;
    
    fivmr_ThreadState_goToJava(ts);

    td=fivmr_TypeContext_findClassKnown(ts->vm->baseContexts[0],name,'/');
    if (td==NULL) {
	fprintf(stderr,"Error: cannot find class %s\n",name);
	abort();
    }
    
    result=fivmr_TypeData_checkInit(ts,td);
    fivmr_assertNoException(ts,"while attempting to initialize type during VM startup");
    fivmr_assert(result);
    fivmr_ThreadState_goToNative(ts);
}

fivmr_TypeData *fivmr_TypeData_fromClass_inNative(fivmr_ThreadState *ts,
                                                  fivmr_Handle *h) {
    fivmr_TypeData *result;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_TypeData_fromClass(ts,fivmr_Handle_get(h));
    fivmr_ThreadState_goToNative(ts);
    return result;
}

int32_t fivmr_TypeData_arrayDepth(fivmr_TypeData *td) {
    int32_t result=0;
    while (td->arrayElement!=NULL) {
        td=td->arrayElement;
        result++;
    }
    return result;
}

fivmr_TypeData *fivmr_TypeData_openArray(fivmr_TypeData *td,
                                         int32_t depth) {
    while (depth-->0) {
        td=td->arrayElement;
    }
    return td;
}

fivmr_TypeData *fivmr_TypeData_closeArray(fivmr_TypeData *td,
                                          int32_t depth) {
    while (depth-->0) {
        fivmr_assert(td!=NULL);
        fivmr_assert(td!=td->arrayType);
        td=td->arrayType;
    }
    fivmr_assert(td!=NULL);
    return td;
}

static void prepareShadow(fivmr_VM *vm) {
    if ((uintptr_t)vm->nTypes*2 > vm->othShadow.n) {
        fivmr_OTH_free(&vm->othShadow);
        fivmr_OTH_init(&vm->othShadow,vm->nTypes*2);
    } else {
        fivmr_OTH_clear(&vm->othShadow);
    }
}

static void prepareDown(fivmr_VM *vm) {
    if ((uintptr_t)vm->nTypes*2 > vm->othDown.n) {
        fivmr_OTH_free(&vm->othDown);
        fivmr_OTH_init(&vm->othDown,vm->nTypes*2);
        if (vm->wlDown!=NULL) {
            fivmr_free(vm->wlDown);
        }
        vm->wlDown=fivmr_mallocAssert(vm->nTypes*sizeof(fivmr_TypeData*));
    } else {
        fivmr_OTH_clear(&vm->othDown);
    }
}

static void prepareSortList(fivmr_VM *vm) {
    if (vm->nTypes > vm->sortListSize) {
        if (vm->sortList!=NULL) {
            fivmr_free(vm->sortList);
        }
        vm->sortList=fivmr_mallocAssert(vm->nTypes*sizeof(fivmr_TypeData*));
        vm->sortListSize=vm->nTypes;
    }
}

uintptr_t fivmr_TypeData_forAllAncestors(fivmr_TypeData *td,
                                         uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                            fivmr_TypeData *curTD,
                                                            uintptr_t arg),
                                         uintptr_t arg) {
    int32_t depth;
    fivmr_TypeData *td_;
    fivmr_VM *vm;
    
    LOG(2,("finding ancestors of %s (%p)",td->name,td));
    
    vm=fivmr_TypeData_getVM(td);
    depth=fivmr_TypeData_arrayDepth(td);
    td_=fivmr_TypeData_openArray(td,depth);
    
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ARRAY) {
        int32_t i;
        fivmr_assert(td->ilistSize==0);
        for (i=0;i<depth;++i) {
            uintptr_t result=
                cback(td,fivmr_TypeData_closeArray(vm->payload->td_Serializable,i),arg);
            if (result!=0) {
                return result;
            }
            result=cback(td,fivmr_TypeData_closeArray(vm->payload->td_Cloneable,i),arg);
            if (result!=0) {
                return result;
            }
            result=cback(td,fivmr_TypeData_closeArray(vm->payload->td_Object,i),arg);
            if (result!=0) {
                return result;
            }
        }
    }
    
    LOG(2,("operating over %s (%p)",td_->name,td_));
    
    if (fivmr_TypeData_isBasetype(td_)) {
        /* FIXME: do anything?  probably not. */
    } else {
        if ((td_->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
            (td_->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION) {
            unsigned i;
            uintptr_t result;
            fivmr_assert((td_->ilistSize!=0)==(td_->nSuperInterfaces!=0));
            result=cback(td,fivmr_TypeData_closeArray(vm->payload->td_Object,depth),arg);
            if (result!=0) {
                return result;
            }
            for (i=0;i<td_->ilistSize;++i) {
                fivmr_assert(td_->ilist[i]!=vm->payload->td_Object);
                result=cback(td,fivmr_TypeData_closeArray(td_->ilist[i],depth),arg);
                if (result!=0) {
                    return result;
                }
            }
        } else {
            fivmr_TypeData *cur;
            for (cur=td_;cur!=vm->payload->td_top;cur=cur->parent) {
                if (cur!=td_) {
                    uintptr_t result=cback(td,fivmr_TypeData_closeArray(cur,depth),arg);
                    if (result!=0) {
                        return result;
                    }
                }
                unsigned i;
                for (i=0;i<cur->ilistSize;++i) {
                    uintptr_t result=cback(td,fivmr_TypeData_closeArray(cur->ilist[i],depth),arg);
                    if (result!=0) {
                        return result;
                    }
                }
            }
        }
    }
    
    return 0;
}

uintptr_t fivmr_TypeData_forAllAncestorsInclusive(fivmr_TypeData *td,
                                                  uintptr_t (*cback)(
                                                      fivmr_TypeData *startTD,
                                                      fivmr_TypeData *curTD,
                                                      uintptr_t arg),
                                                  uintptr_t arg) {
    uintptr_t result=cback(td,td,arg);
    if (result!=0) {
        return result;
    }
    return fivmr_TypeData_forAllAncestors(td,cback,arg);
}

typedef struct {
    fivmr_VM *vm;
    int32_t n;
} CollectForSortData;

static uintptr_t collectForSort_cback(fivmr_TypeData *startTD,
                                      fivmr_TypeData *curTD,
                                      uintptr_t arg) {
    CollectForSortData *data=(CollectForSortData*)(void*)arg;
    LOG(2,("Adding type %s (%p)",curTD->name,curTD));
    data->vm->sortList[data->n++]=curTD;
    return 0;
}

static int TypeData_compareDescendantsInverse(const void *a_,
                                              const void *b_) {
    fivmr_TypeData *a=*(fivmr_TypeData**)(void*)a_;
    fivmr_TypeData *b=*(fivmr_TypeData**)(void*)b_;
    if (a->numDescendants>b->numDescendants) {
        return -1;
    } else if (a->numDescendants==b->numDescendants) {
        return 0;
    } else {
        return 1;
    }
}

uintptr_t fivmr_TypeData_forAllAncestorsSorted(fivmr_TypeData *td,
                                               uintptr_t (*cback)(
                                                   fivmr_TypeData *startTD,
                                                   fivmr_TypeData *curTD,
                                                   uintptr_t arg),
                                               uintptr_t arg) {
    fivmr_VM *vm=fivmr_TypeData_getVM(td);
    CollectForSortData data;
    int32_t i;
    prepareSortList(vm);
    data.vm=vm;
    data.n=0;
    fivmr_TypeData_forAllAncestors(td,collectForSort_cback,(uintptr_t)(void*)&data);
    fivmr_sort(vm->sortList,data.n,sizeof(fivmr_TypeData*),
               TypeData_compareDescendantsInverse);
    for (i=0;i<data.n;++i) {
        uintptr_t result;
        result=cback(td,vm->sortList[i],arg);
        if (result!=0) {
            return result;
        }
    }
    return 0;
}

uintptr_t fivmr_TypeData_forAllAncestorsSortedInclusive(fivmr_TypeData *td,
                                                        uintptr_t (*cback)(
                                                            fivmr_TypeData *startTD,
                                                            fivmr_TypeData *curTD,
                                                            uintptr_t arg),
                                                        uintptr_t arg) {
    fivmr_VM *vm=fivmr_TypeData_getVM(td);
    CollectForSortData data;
    int32_t i;
    prepareSortList(vm);
    data.vm=vm;
    data.n=0;
    fivmr_TypeData_forAllAncestorsInclusive(td,collectForSort_cback,(uintptr_t)(void*)&data);
    fivmr_sort(vm->sortList,data.n,sizeof(fivmr_TypeData*),
               TypeData_compareDescendantsInverse);
    for (i=0;i<data.n;++i) {
        uintptr_t result;
        result=cback(td,vm->sortList[i],arg);
        if (result!=0) {
            return result;
        }
    }
    return 0;
}

static void pushSubs(fivmr_OTH *oth,
                     fivmr_TypeData **wl,
                     int32_t *n,
                     fivmr_TypeData *td) {
    unsigned i;
    for (i=0;i<td->nDirectSubs;++i) {
        if (fivmr_OTH_put(oth,td->directSubs[i],(void*)1)) {
            wl[(*n)++]=td->directSubs[i];
        }
    }
}

uintptr_t fivmr_TypeData_forAllDescendants(fivmr_TypeData *td,
                                           uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                              fivmr_TypeData *curTD,
                                                              uintptr_t arg),
                                           uintptr_t arg) {
    fivmr_VM *vm=fivmr_TypeData_getVM(td);
    fivmr_OTH *oth;
    fivmr_TypeData **wl;
    int32_t n;
    uintptr_t result;
    prepareDown(vm);
    oth=&vm->othDown;
    wl=vm->wlDown;
    n=0;
    pushSubs(oth,wl,&n,td);
    while (n>0) {
        fivmr_TypeData *cur;
        uintptr_t result;
        cur=wl[--n];
        result=cback(td,cur,arg);
        if (result!=0) {
            return result;
        }
        pushSubs(oth,wl,&n,cur);
    }
    return 0;
}

uintptr_t fivmr_TypeData_forAllDescendantsInclusive(fivmr_TypeData *td,
                                                    uintptr_t (*cback)(
                                                        fivmr_TypeData *startTD,
                                                        fivmr_TypeData *curTD,
                                                        uintptr_t arg),
                                                    uintptr_t arg) {
    uintptr_t result=cback(td,td,arg);
    if (result!=0) {
        return result;
    }
    return fivmr_TypeData_forAllDescendants(td,cback,arg);
}

typedef struct {
    fivmr_VM *vm;
    uintptr_t (*cback)(fivmr_TypeData *startTD,
                       fivmr_TypeData *curTD,
                       uintptr_t arg);
    fivmr_TypeData *startTD;
    uintptr_t arg;
} CbackData;

uintptr_t shadow_cbackUp(fivmr_TypeData *startTD,
                         fivmr_TypeData *curTD,
                         uintptr_t arg) {
    CbackData *data=(CbackData*)(void*)arg;
    if (fivmr_OTH_put(&data->vm->othShadow,curTD,(void*)1)) {
        data->vm->shadowResult=data->cback(data->startTD,curTD,data->arg);
        if (data->vm->shadowResult!=0) {
            return 1;
        } else {
            return 0;
        }
    } else {
        /* ef me ... what to do here?  this is going to turn into a performance
           pathology.  but I can't return 1. */
        return 0;
    }
}

uintptr_t shadow_cbackDown(fivmr_TypeData *startTD,
                           fivmr_TypeData *curTD,
                           uintptr_t arg) {
    if (curTD->nDirectSubs==0) {
        CbackData *data;
        uintptr_t result;
        bool res;
        data=(CbackData*)(void*)arg;
        res=fivmr_OTH_put(&data->vm->othShadow,curTD,(void*)1);
        fivmr_assert(res);
        data->vm->shadowResult=data->cback(data->startTD,curTD,data->arg);
        if (data->vm->shadowResult!=0) {
            return 1;
        }
        fivmr_TypeData_forAllAncestors(curTD,shadow_cbackUp,arg);
        if (data->vm->shadowResult!=0) {
            return 1;
        } else {
            return 0;
        }
    } else {
        return 0;
    }
}

uintptr_t fivmr_TypeData_forShadow(fivmr_TypeData *td,
                                   uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                      fivmr_TypeData *curTD,
                                                      uintptr_t arg),
                                   uintptr_t arg) {
    CbackData data;
    data.vm=fivmr_TypeData_getVM(td);
    data.cback=cback;
    data.startTD=td;
    data.arg=arg;
    prepareShadow(fivmr_TypeData_getVM(td));
    fivmr_TypeData_forAllDescendantsInclusive(td,shadow_cbackDown,(uintptr_t)(void*)&data);
    return data.vm->shadowResult;
}

uintptr_t fivmr_TypeContext_forAllTypes(fivmr_TypeContext *ctx,
                                        uintptr_t (*cback)(fivmr_TypeData *curTD,
                                                           uintptr_t arg),
                                        uintptr_t arg) {
    int32_t i;
    uintptr_t result;
    ftree_Node *node;
    for (i=0;i<ctx->st.nTypes;++i) {
        result=cback(ctx->vm->payload->typeList[ctx->st.typeOffset+i],arg);
        if (result!=0) {
            return result;
        }
    }
    for (node=ftree_Tree_first(&ctx->dynamicTypeTree);
         node!=NULL;
         node=ftree_Tree_next(&ctx->dynamicTypeTree,node)) {
        fivmr_assert(node!=&ctx->dynamicTypeTree.nil);
        result=cback((fivmr_TypeData*)(void*)node->value,arg);
        if (result!=0) {
            return result;
        }
    }
    return 0;
}

uintptr_t fivmr_VM_forAllTypes(fivmr_VM *vm,
                               uintptr_t (*cback)(fivmr_TypeData *curTD,
                                                  uintptr_t arg),
                               uintptr_t arg) {
    int32_t i;
    uintptr_t result;
    for (i=0;i<vm->payload->nContexts;++i) {
        result=fivmr_TypeContext_forAllTypes(vm->baseContexts[i],cback,arg);
        if (result!=0) {
            return result;
        }
    }
    for (i=0;i<vm->nDynContexts;++i) {
        result=fivmr_TypeContext_forAllTypes(vm->dynContexts[i],cback,arg);
        if (result!=0) {
            return result;
        }
    }
    return 0;
}

static uintptr_t isSubtypeOf_cback(fivmr_TypeData *startTD,
                                   fivmr_TypeData *curTD,
                                   uintptr_t arg) {
    fivmr_TypeData *b=(fivmr_TypeData*)(void*)arg;
    if (curTD==b) {
        return 1;
    } else {
        return 0;
    }
}

bool fivmr_TypeData_isSubtypeOfSlow(fivmr_ThreadState *ts,
                                    fivmr_TypeData *a,
                                    fivmr_TypeData *b) {
    return (bool)fivmr_TypeData_forAllAncestorsInclusive(
        a,isSubtypeOf_cback,(uintptr_t)(void*)b);
}

/* How to do type display update:
   - Check if any type displays have conflicts
   - For (n-1) of the types that have conflicts, do the following:
     - For all types in the type's shadow
       - Shade any buckets used by a type in the shadow
     - Shade any buckets whose tids are full
     - Shade the bucket that the type currently has (redundant with the shadow search)
     - Pick a bucket that isn't shaded or create a new one
     - For all descendants of the type, fix their displays */

/* How to do itable update:
   - Check if any iface methods have conflicts
   - For (n-1) of the iface methods that have conflicts, do the following:
     - For all types in the iface method owner's shadow
       - If a type in the shadow is an interface
         - Shade all itable indices used by iface methods in the iface in the shadow
     - Shade the itable index that we conflicted on
     - Pick an itable index that isn't shaded or create a new one
     - For all descendants of the iface, fix their itables

   This algorithm may have to traverse the shadow too many times.  This
   algorithm may be better:
   - Allocate iface methods to indices one iface at a time
   - If we encounter an iface for which at least one method has a conflict,
     do the following:
     - For all types in the iface's shadow
       - If a type in the shadow is an interface and is not the iface we are implementing
         - Shade all itable indices used by iface methods in the iface in the shadow
     - Shade the itable indices on which we saw conflicts
     - Pick a new set of iface indices for all methods in the iface
     - For all descendants of the iface, fix their itables */

/* global type worklist helpers */
static void initWorklist(fivmr_VM *vm) {
    if ((uintptr_t)vm->nTypes*2 > vm->oth.n) {
        fivmr_OTH_free(&vm->oth);
        fivmr_OTH_init(&vm->oth,vm->nTypes*2);
        fivmr_free(vm->wl);
        vm->wl=fivmr_mallocAssert(sizeof(fivmr_TypeData*)*vm->nTypes);
    } else {
        fivmr_OTH_clear(&vm->oth);
    }
    vm->wlN=0;
}

static bool pushWorklist(fivmr_VM *vm,
                         fivmr_TypeData *td) {
    if (fivmr_OTH_put(&vm->oth,td,(void*)1)) {
        fivmr_assert(vm->wlN<vm->nTypes);
        vm->wl[vm->wlN++]=td;
        return true;
    } else {
        return false;
    }
}

static fivmr_TypeData *popWorklist(fivmr_VM *vm) {
    if (vm->wlN>0) {
        return vm->wl[--vm->wlN];
    } else {
        return NULL;
    }
}

/* call only while holding the tree lock and the type data lock */
static fivmr_TypeData *defineImpl(fivmr_TypeContext *ctx,
                                  fivmr_TypeData *td) {
    fivmr_TypeStub *stub;
    stub=fivmr_TypeContext_findStub(ctx,td->name);
    if (fivmr_TypeStub_union(stub,(fivmr_TypeStub*)td)) {
        fivmr_TypeDataNode *node=fivmr_mallocAssert(sizeof(fivmr_TypeDataNode));
        ftree_Node_init(&node->treeNode,
                        (uintptr_t)(void*)td->name,
                        (uintptr_t)(void*)td);
        node->next=td->node;
        td->node=node;
        
        ftree_Tree_add(&ctx->dynamicTypeTree,&node->treeNode);
        
        LOG(1,("Successfully defined new type %s (%p) in context %p",
               td->name,td,ctx));
        
        ctx->vm->nTypes++;
        
        return td;
    } else {
        fivmr_TypeData *result=fivmr_TypeStub_tryGetTypeData(stub);

        LOG(2,("Failed to define new type %s (%p) in context %p; already have %p",
               td->name,td,ctx,result));
        
        fivmr_assert(result!=NULL);
        return result;
    }
}

fivmr_TypeData *fivmr_TypeData_define(fivmr_TypeContext *ctx,
                                      fivmr_TypeData *td) {
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    fivmr_TypeData baseTD;
    fivmr_TypeDataNode *node;
    fivmr_TypeData *result;
    
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm); 
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    fivmr_assert(fivmr_Settings_canDoClassLoading(&vm->settings));
    
    fivmr_ThreadState_goToNative(ts);
    
    fivmr_Lock_lock(&ctx->treeLock);
    fivmr_Lock_lock(&vm->typeDataLock);
    
    fivmr_ThreadState_goToJava(ts);
    
    result=defineImpl(ctx,td);
    
    fivmr_Lock_unlock(&vm->typeDataLock);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    return result;
}

static void addSubtype(fivmr_TypeData *parent,fivmr_TypeData *child) {
    fivmr_TypeData **newDirectSubs;
    if ((parent->flags&FIVMR_TBF_DIRECT_SUBS_MALLOCED)) {
        newDirectSubs=
            fivmr_reallocAssert(parent->directSubs,
                                sizeof(fivmr_TypeData*)*(parent->nDirectSubs+1));
    } else {
        newDirectSubs=fivmr_mallocAssert(sizeof(fivmr_TypeData*)*(parent->nDirectSubs+1));
        memcpy(newDirectSubs,parent->directSubs,
               sizeof(fivmr_TypeData*)*parent->nDirectSubs);
    }
    newDirectSubs[parent->nDirectSubs]=child;
    parent->nDirectSubs++;
    parent->directSubs=newDirectSubs;
    fivmr_BitField_setAtomic(&parent->flags,
                             FIVMR_TBF_DIRECT_SUBS_MALLOCED,
                             FIVMR_TBF_DIRECT_SUBS_MALLOCED);
}

static uintptr_t incrementDescendants_cback(fivmr_TypeData *startTD,
                                            fivmr_TypeData *curTD,
                                            uintptr_t arg) {
    curTD->numDescendants++;
    return 0;
}

typedef struct {
    uint32_t *usedBuckets;
    int32_t typeEpoch;
} FindBucketsData;

static uintptr_t findBuckets_cback(fivmr_TypeData *startTD,
                                   fivmr_TypeData *curTD,
                                   uintptr_t arg) {
    FindBucketsData *fbdata=(FindBucketsData*)(void*)arg;
    fivmr_assert(curTD->epochs[fbdata->typeEpoch].bucket
                 < fivmr_TypeData_getVM(curTD)->numBuckets);
    LOG(2,("findBuckets dealing with %s (%p)",curTD->name,curTD));
    fivmr_BitVec_set(fbdata->usedBuckets,
                     curTD->epochs[fbdata->typeEpoch].bucket,
                     true);
    return 0;
}

static uintptr_t addOneBucket_cback(fivmr_TypeData *td,
                                    uintptr_t arg) {
    if ((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_PRIMITIVE &&
        (td->flags&FIVMR_TBF_RESOLUTION_DONE)) {
        fivmr_VM *vm;
        int8_t *newBuckets;
        
        vm=(fivmr_VM*)(void*)arg;
        if (fivmr_TypeData_bucketsMalloced(td,vm->typeEpoch^1)) {
            newBuckets=fivmr_reallocAssert(td->epochs[vm->typeEpoch^1].buckets,
                                           sizeof(int8_t)*vm->numBuckets);
        } else {
            newBuckets=fivmr_mallocAssert(sizeof(int8_t)*vm->numBuckets);
            memcpy(newBuckets,
                   td->epochs[vm->typeEpoch^1].buckets,
                   sizeof(int8_t)*(vm->numBuckets-1));
        }
        newBuckets[vm->numBuckets-1]=0;
        td->epochs[vm->typeEpoch^1].buckets=newBuckets;
        fivmr_TypeData_setBucketsMalloced(td,vm->typeEpoch^1,true);
        
        pushWorklist(vm,td);
    }
    return 0;
}

static void addOneBucket(fivmr_VM *vm) {
    uint32_t *newUsedTids;
    
    /* resize tids book-keeping */
    if ((vm->flags&FIVMR_VMF_USED_TIDS_MALLOCED)) {
        newUsedTids=fivmr_reallocAssert(vm->usedTids,
                                        sizeof(uint32_t)*((vm->numBuckets+1)*256/32));
    } else {
        newUsedTids=fivmr_mallocAssert(sizeof(uint32_t)*((vm->numBuckets+1)*256/32));
        memcpy(newUsedTids,
               vm->usedTids,
               sizeof(uint32_t)*(vm->numBuckets*256/32));
    }
    LOG(1,("vm->usedTids = %p, newUsedTids = %p, numBuckets = %p",
           vm->usedTids,newUsedTids,vm->numBuckets));
    bzero(newUsedTids+vm->numBuckets*256/32,
          sizeof(uint32_t)*256/32);
    vm->usedTids=newUsedTids;
    vm->numBuckets++;
    fivmr_BitField_setAtomic(&vm->flags,
                             FIVMR_VMF_USED_TIDS_MALLOCED,
                             FIVMR_VMF_USED_TIDS_MALLOCED);
    
    /* for each type, reallocate the buckets in the new epoch */
    fivmr_VM_forAllTypes(vm,addOneBucket_cback,(uintptr_t)(void*)vm);
}

static void findBucketAndTid(fivmr_VM *vm,
                             uint32_t **usedBuckets,
                             int32_t *foundBucket,
                             int32_t *foundTid) {
    uintptr_t foundBucketOcc=255;
    int32_t i,j;
    *foundBucket=-1;
    *foundTid=-1;
    for (i=vm->numBuckets;i-->0;) {
        if (!fivmr_BitVec_get(*usedBuckets,i)) {
            int32_t tid=-1;
            uintptr_t bucketOcc=0;
            for (j=1;j<256;++j) {
                if (fivmr_BitVec_get(vm->usedTids,i*256+j)) {
                    bucketOcc++;
                } else {
                    tid=j;
                }
            }
            if (bucketOcc<255) {
                fivmr_assert(tid>=1 && tid<=255);
                if (bucketOcc<foundBucketOcc) {
                    foundBucketOcc=bucketOcc;
                    *foundBucket=i;
                    *foundTid=tid;
                }
            } else {
                fivmr_assert(tid==-1);
            }
        }
    }
    
    if (*foundBucket<0) {
        fivmr_assert(*foundBucket==-1);
        fivmr_assert(*foundTid==-1);
        
        addOneBucket(vm);
        *usedBuckets=fivmr_reallocAssert(*usedBuckets,
                                         sizeof(uint32_t)*((vm->numBuckets+31)/32));
        
        *foundBucket=vm->numBuckets-1;
        *foundTid=1;
    
        LOG(1,("setting usedTids: %d, %d",vm->numBuckets-1,1));
        fivmr_BitVec_set(vm->usedTids,(vm->numBuckets-1)*256+1,true);
    } else {
        fivmr_assert(*foundBucket>=0);
        fivmr_assert(*foundTid>=0);
    
        LOG(1,("setting usedTids: %d, %d",*foundBucket,*foundTid));
        fivmr_BitVec_set(vm->usedTids,(*foundBucket)*256+*foundTid,true);
    }
}

static uintptr_t populateBuckets_cback(fivmr_TypeData *startTD,
                                       fivmr_TypeData *curTD,
                                       uintptr_t arg) {
    fivmr_VM *vm;
    int32_t epoch;
    int32_t i;
    
    vm=(fivmr_VM*)(void*)arg;
    epoch=vm->typeEpoch^1;
    
    LOG(1,("Populating bucket %d with %d for %s (%p)",
           curTD->epochs[epoch].bucket,
           curTD->epochs[epoch].tid&0xff,
           curTD->name,curTD));
    
    if (startTD->epochs[epoch].buckets[curTD->epochs[epoch].bucket]==0) {
        startTD->epochs[epoch].buckets[curTD->epochs[epoch].bucket]=
            curTD->epochs[epoch].tid;
        return 0;
    } else {
        /* collision on curTD */
        return (uintptr_t)(void*)curTD;
    }
}

typedef struct {
    fivmr_TypeData *newTD;
    int32_t oldBucket;
    int32_t oldTid;
    int32_t newBucket;
    int32_t newTid;
    int32_t typeEpoch;
} UpdateBucketsData;

static uintptr_t updateBuckets_cback(fivmr_TypeData *startTD,
                                     fivmr_TypeData *curTD,
                                     uintptr_t arg) {
    UpdateBucketsData *ubdata=(UpdateBucketsData*)(void*)arg;
    if (curTD!=ubdata->newTD) {
        fivmr_TypeEpoch *te=curTD->epochs+ubdata->typeEpoch;
        fivmr_assert((((int32_t)te->buckets[ubdata->oldBucket])&0xff)==ubdata->oldTid);
        fivmr_assert(te->buckets[ubdata->newBucket]==0);
        if (!fivmr_TypeData_bucketsMalloced(curTD,ubdata->typeEpoch)) {
            fivmr_VM *vm;
            int8_t *newBuckets;
            vm=fivmr_TypeData_getVM(curTD);
            newBuckets=fivmr_mallocAssert(sizeof(int8_t)*vm->numBuckets);
            memcpy(newBuckets,te->buckets,sizeof(int8_t)*vm->numBuckets);
            te->buckets=newBuckets;
            fivmr_TypeData_setBucketsMalloced(curTD,ubdata->typeEpoch,true);
        }
        LOG(1,("Replacing %d[%d] with %d[%d] in %s (%p)",
               ubdata->oldBucket,ubdata->oldTid&0xff,
               ubdata->newBucket,ubdata->newTid&0xff,
               curTD->name,curTD));
        te->buckets[ubdata->oldBucket]=0;
        te->buckets[ubdata->newBucket]=ubdata->newTid;
        pushWorklist(fivmr_TypeData_getVM(curTD),curTD);
    }
    return 0;
}

static void handleBucketCollision(fivmr_TypeData *td,
                                  fivmr_TypeData *newTD,
                                  uint32_t **usedBuckets) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    int32_t foundBucket;
    int32_t foundTid;
    int32_t oldBucket;
    int32_t oldTid;
    FindBucketsData fbdata;
    UpdateBucketsData ubdata;

    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    
    /* mark the old tid as no longer used */
    oldBucket=td->epochs[vm->typeEpoch^1].bucket;
    oldTid=((int32_t)td->epochs[vm->typeEpoch^1].tid)&0xff;
    LOG(1,("clearing usedTids: %d, %d",oldBucket,oldTid));
    fivmr_BitVec_set(vm->usedTids,
                     oldBucket*256+oldTid,
                     false);
    
    bzero(*usedBuckets,sizeof(int32_t)*((vm->numBuckets+31)/32));
    
    fbdata.usedBuckets=*usedBuckets;
    fbdata.typeEpoch=vm->typeEpoch^1;
    
    LOG(2,("finding buckets for %s (%p)",td->name,td));
    
    fivmr_TypeData_forShadow(td,findBuckets_cback,(uintptr_t)(void*)&fbdata);
    
    findBucketAndTid(vm,usedBuckets,&foundBucket,&foundTid);
    
    LOG(1,("Have tid/bucket for %s (%p) after collision: tid = %d, bucket = %d",
           td->name,td,foundTid,foundBucket));

    td->epochs[vm->typeEpoch^1].bucket=foundBucket;
    td->epochs[vm->typeEpoch^1].tid=foundTid;

    ubdata.newTD=newTD;
    ubdata.oldBucket=oldBucket;
    ubdata.oldTid=oldTid;
    ubdata.newBucket=foundBucket;
    ubdata.newTid=foundTid;
    ubdata.typeEpoch=vm->typeEpoch^1;
    
    fivmr_TypeData_forAllDescendantsInclusive(
        td,updateBuckets_cback,(uintptr_t)(void*)&ubdata);
}

typedef struct {
    uint32_t *usedItables;
    int32_t typeEpoch;
} FindItablesData;

static uintptr_t findItables_cback(fivmr_TypeData *startTD,
                                   fivmr_TypeData *curTD,
                                   uintptr_t arg) {
    FindItablesData *fidata=(FindItablesData*)(void*)arg;
    unsigned i;
    if (curTD!=startTD &&
        ((curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
         (curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION)) {
        for (i=0;i<curTD->numMethods;++i) {
            fivmr_MethodRec *mr=curTD->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                int32_t idx;
                
                idx=fivmr_MethodRec_itableIndexForEpoch(
                        mr,fidata->typeEpoch);
                
                fivmr_BitVec_set(fidata->usedItables,idx,true);
            }
        }
    }
    return 0;
}

static void growItables(fivmr_VM *vm,int32_t amount) {
    int32_t *newItableOcc;
    if ((vm->flags&FIVMR_VMF_ITABLE_OCC_MALLOCED)) {
        newItableOcc=fivmr_reallocAssert(vm->itableOcc,
                                         sizeof(int32_t)*(vm->itableSize+amount));
    } else {
        newItableOcc=fivmr_mallocAssert(sizeof(int32_t)*(vm->itableSize+amount));
        memcpy(newItableOcc,
               vm->itableOcc,
               sizeof(int32_t)*vm->itableSize);
    }
    bzero(newItableOcc+vm->itableSize,
          sizeof(int32_t)*amount);
    vm->itableOcc=newItableOcc;
    vm->itableSize+=amount;
    fivmr_BitField_setAtomic(&vm->flags,
                             FIVMR_VMF_ITABLE_OCC_MALLOCED,
                             FIVMR_VMF_ITABLE_OCC_MALLOCED);
}

typedef struct {
    int32_t index;
    int32_t occupancy;
} ItableEntry;

static int ItableEntry_compare(const void *a_,
                               const void *b_) {
    ItableEntry *a=(ItableEntry*)(void*)a_;
    ItableEntry *b=(ItableEntry*)(void*)b_;
    if (a->occupancy>b->occupancy) {
        return 1;
    } else if (a->occupancy==b->occupancy) {
        return 0;
    } else {
        return -1;
    }
}

static void findItableIndices(fivmr_TypeData *td,uint32_t **usedItables) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    int32_t numIfaceMethods;
    int32_t numFreeSlots;
    unsigned i,j;
    ItableEntry *occupancy;
    
    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
                 (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION);
    
    /* first figure out how many slots we need */
    numIfaceMethods=0;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            numIfaceMethods++;
        }
    }
    
    /* now figure out if we need to resize */
    numFreeSlots=0;
    for (i=0;i<vm->itableSize;++i) {
        if (!fivmr_BitVec_get(*usedItables,i)) {
            numFreeSlots++;
        }
    }
    
    if (numIfaceMethods>numFreeSlots) {
        uintptr_t oldItableSize=vm->itableSize;
        uintptr_t k;
        growItables(vm,numIfaceMethods-numFreeSlots);
        *usedItables=fivmr_reallocAssert(*usedItables,
                                         sizeof(uint32_t)*((vm->itableSize+31)/32));
        for (k=oldItableSize;k<vm->itableSize;++k) {
            fivmr_BitVec_set(*usedItables,k,false);
        }
    }
    
    /* build and sort the occupancy list */
    occupancy=fivmr_mallocAssert(sizeof(ItableEntry)*vm->itableSize);
    for (i=0;i<vm->itableSize;++i) {
        occupancy[i].index=i;
        occupancy[i].occupancy=vm->itableOcc[i];
    }
    
    fivmr_sort(occupancy,
               vm->itableSize,
               sizeof(ItableEntry),
               ItableEntry_compare);
    
    fivmr_assert(occupancy[0].occupancy
                 <= occupancy[vm->itableSize-1].occupancy);
    
    /* assign the indices */
    for (i=0,j=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            /* find first one that is unused */
            while (fivmr_BitVec_get(*usedItables,occupancy[j].index)) {
                j++;
            }
            fivmr_assert(j<vm->itableSize);
            fivmr_MethodRec_setItableIndexForEpoch(
                mr,vm->typeEpoch^1,
                occupancy[j].index);
            vm->itableOcc[occupancy[j].index]++;
            j++;
        }
    }
    
    fivmr_free(occupancy);
}

static uintptr_t populateItable_cback(fivmr_TypeData *startTD,
                                      fivmr_TypeData *curTD,
                                      uintptr_t arg) {
    fivmr_VM *vm=(fivmr_VM*)(void*)arg;
    int32_t epoch=vm->typeEpoch^1;
    
    if ((curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
        (curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION) {
        unsigned i;
        for (i=0;i<curTD->numMethods;++i) {
            fivmr_MethodRec *mr=curTD->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                int32_t idx;
                fivmr_MethodRec *implMR;
                void *loc;
                
                idx=fivmr_MethodRec_itableIndexForEpoch(mr,epoch);

                /* find the implementation of that interface method */
                implMR=
                    fivmr_TypeData_findInstMethodNoIface2(
                        vm,startTD,mr->name,mr->result,mr->nparams,mr->params);
                
                /* since this isn't abstract it had better implement that method, and
                   we better have code for it */
                fivmr_assert(implMR!=NULL);
                if ((implMR->flags&FIVMR_MBF_METHOD_KIND)!=FIVMR_MBF_ABSTRACT) {
                    fivmr_assert((implMR->flags&FIVMR_MBF_METHOD_KIND)==FIVMR_MBF_VIRTUAL ||
                                 (implMR->flags&FIVMR_MBF_METHOD_KIND)==FIVMR_MBF_FINAL);
                    fivmr_assert((implMR->flags&FIVMR_MBF_HAS_CODE));
                    
                    loc=(void*)implMR->entrypoint;
                    
                    fivmr_assert(loc!=NULL);
                    
                    if (startTD->epochs[epoch].itable[idx]==NULL) {
                        LOG(1,("Populating itable at index %d with %p",idx,loc));
                        startTD->epochs[epoch].itable[idx]=loc;
                    } else {
                        /* oh noes!  collision! */
                        return (uintptr_t)(void*)curTD;
                    }
                } else {
                    fivmr_assert((startTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT);
                }
            }
        }
    }
    
    return 0;
}

static void shrinkItable(fivmr_TypeData *td,
                         int32_t epoch) {
    unsigned newItableOff,newItableLen;
    void **newItable;
    fivmr_TypeEpoch *e;
    unsigned i;
    
    e=td->epochs+epoch;
    
    /* figure out if the itable needs shrinkage */
    newItableOff=e->itableOff;
    newItableLen=e->itableLen;
    
    fivmr_assert(newItableLen>0); /* there must be at least one itable method in there */
    
    while (e->itable[newItableOff]==NULL) {
        newItableOff++;
        newItableLen--;
        if (newItableLen==0) {
            break;
        }
        fivmr_assert(newItableOff<e->itableOff+e->itableLen);
    }
    
    LOG(1,("newItableOff = %u, newItableLen = %u",newItableOff,newItableLen));
    
    if (newItableLen==0) {
        if (fivmr_TypeData_itableMalloced(td,epoch)) {
            fivmr_free(e->itable+e->itableOff);
        }
        fivmr_TypeData_setItableMalloced(td,epoch,true);
        
        e->itableOff=0;
        e->itableLen=0;
        e->itable=NULL;
    } else {
        while (e->itable[newItableOff+newItableLen-1]==NULL) {
            newItableLen--;
            fivmr_assert(newItableLen>0);
        }
    
        if (newItableOff!=e->itableOff ||
            newItableLen!=e->itableLen) {
            /* yup, need to resize */

            newItable=fivmr_mallocAssert(sizeof(void*)*newItableLen);
            newItable-=newItableOff;
        
            for (i=newItableOff;i<newItableOff+newItableLen;++i) {
                fivmr_assert(i>=e->itableOff && i<e->itableOff+e->itableLen);
                newItable[i]=e->itable[i];
            }
        
            if (fivmr_TypeData_itableMalloced(td,epoch)) {
                fivmr_free(e->itable+e->itableOff);
            }
            fivmr_TypeData_setItableMalloced(td,epoch,true);
        
            e->itableOff=newItableOff;
            e->itableLen=newItableLen;
            e->itable=newItable;
        }
    }
}

typedef struct {
    fivmr_TypeData *newTD;
    int32_t minIdx;
    int32_t maxIdx;
    int32_t epoch;
    int32_t *oldIndices;
    void **locBuf;
} UpdateItablesData;

static uintptr_t updateItables_cback(fivmr_TypeData *startTD,
                                     fivmr_TypeData *curTD,
                                     uintptr_t arg) {
    UpdateItablesData *uidata;
    uidata=(UpdateItablesData*)(void*)arg;
    if (curTD!=uidata->newTD &&
        ((curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_VIRTUAL ||
         (curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
         (curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT)) {
        fivmr_TypeEpoch *e;
        unsigned i,j;
        unsigned newItableOff,newItableLen;
        void **newItable;

        e=curTD->epochs+uidata->epoch;

        /* figure out if we need to grow the itable, and if so, grow it.  we also
           reallocate if the only version is from the payload. */
        if (uidata->minIdx<e->itableOff ||
            uidata->maxIdx>=e->itableOff+e->itableLen ||
            !fivmr_TypeData_itableMalloced(curTD,uidata->epoch)) {

            newItableOff=fivmr_min(e->itableOff,uidata->minIdx);
            newItableLen=fivmr_max(e->itableLen,uidata->maxIdx-uidata->minIdx+1);
        
            newItable=fivmr_mallocAssert(sizeof(void*)*newItableLen);
        
            for (i=newItableOff;i<newItableOff+newItableLen;++i) {
                if (i>=e->itableOff && i<e->itableOff+e->itableLen) {
                    newItable[i-newItableOff]=e->itable[i];
                } else {
                    newItable[i-newItableOff]=NULL;
                }
            }
            
            if (fivmr_TypeData_itableMalloced(curTD,uidata->epoch)) {
                fivmr_free(e->itable+e->itableOff);
            }
            fivmr_TypeData_setItableMalloced(curTD,uidata->epoch,true);
        
            e->itableOff=newItableOff;
            e->itableLen=newItableLen;
            e->itable=newItable-newItableOff;
        }
    
        /* now go through our interface methods and clear out the previously used
           itable indices, saving their contents */
        for (i=0,j=0;i<startTD->numMethods;++i) {
            fivmr_MethodRec *mr=startTD->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                int32_t oldIdx;
                
                oldIdx=uidata->oldIndices[j];

                fivmr_assert(oldIdx>=e->itableOff);
                fivmr_assert(oldIdx<e->itableOff+e->itableLen);

                uidata->locBuf[j]=e->itable[oldIdx];
                if ((curTD->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_ABSTRACT) {
                    fivmr_assert(uidata->locBuf[j]!=0);
                }
                e->itable[oldIdx]=0;
                
                j++;
            }
        }

        /* and now place the contents into the new indices. */
        for (i=0,j=0;i<startTD->numMethods;++i) {
            fivmr_MethodRec *mr=startTD->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                int32_t newIdx;
                uintptr_t loc;
                
                newIdx=fivmr_MethodRec_itableIndexForEpoch(mr,uidata->epoch);

                fivmr_assert(newIdx>=e->itableOff);
                fivmr_assert(newIdx<e->itableOff+e->itableLen);

                fivmr_assert(e->itable[newIdx]==0);
            
                e->itable[newIdx]=uidata->locBuf[j];
                
                j++;
            }
        }

        shrinkItable(curTD,uidata->epoch);
        
        pushWorklist(fivmr_TypeData_getVM(curTD),curTD);
    }
    return 0;
}

static void handleItableCollision(fivmr_TypeData *td,
                                  fivmr_TypeData *newTD,
                                  uint32_t **usedItables) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    unsigned i;
    int32_t epoch;
    FindItablesData fidata;
    UpdateItablesData uidata;
    int32_t numIfaceMethods;
    int32_t *oldIndices;
    unsigned j;

    /* what this needs to do:
       - figure out what itable indices are used by any interfaces in our
         shadow
       - switch to using itable indices that aren't used
       - let our descendants know about the change */
    
    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
                 (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION);
    
    epoch=vm->typeEpoch^1;
    
    /* remove our iface methods from the occupancy count, and count the number
       of itable methods we have */
    numIfaceMethods=0;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            vm->itableOcc[fivmr_MethodRec_itableIndexForEpoch(mr,epoch)]--;
            numIfaceMethods++;
        }
    }
    
    /* record the old itable indices */
    oldIndices=fivmr_mallocAssert(sizeof(int32_t)*numIfaceMethods);
    j=0;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            oldIndices[j++]=fivmr_MethodRec_itableIndexForEpoch(mr,epoch);
        }
    }
    
    /* initialize the usedItables set */
    bzero(*usedItables,sizeof(uint32_t)*((vm->itableSize+31)/32));
    
    /* find the itable indices used by any interfaces in our shadow; note that
       findItables_cback will exclude us from the search, which is great,
       because we're quite ok with reusing our own itable indices so long as
       they don't conflict with the *other* itable indices in our shadow */
    fidata.usedItables=*usedItables;
    fidata.typeEpoch=epoch;
    
    fivmr_TypeData_forShadow(td,findItables_cback,(uintptr_t)(void*)&fidata);
    
    /* now find some new itable indices */
    findItableIndices(td,usedItables);
    
    /* and perform the update */
    uidata.newTD=newTD;
    uidata.locBuf=fivmr_mallocAssert(sizeof(void*)*numIfaceMethods);
    uidata.epoch=epoch;
    uidata.oldIndices=oldIndices;
    uidata.minIdx=-1;
    uidata.maxIdx=-1;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            int32_t idx=fivmr_MethodRec_itableIndexForEpoch(mr,epoch);
            if (uidata.minIdx<0) {
                fivmr_assert(uidata.maxIdx<0);
                uidata.minIdx=uidata.maxIdx=idx;
            } else {
                uidata.minIdx=fivmr_min(uidata.minIdx,idx);
                uidata.maxIdx=fivmr_max(uidata.maxIdx,idx);
            }
        }
    }
    
    /* if we don't have any interface methods then there is no way we should be
       in here... */
    fivmr_assert(uidata.minIdx>=0);
    fivmr_assert(uidata.maxIdx>=0);
    fivmr_assert(uidata.maxIdx>=uidata.minIdx);
    
    fivmr_TypeData_forAllDescendantsInclusive(
        td,updateItables_cback,(uintptr_t)(void*)&uidata);
    
    fivmr_free(oldIndices);
    fivmr_free(uidata.locBuf);
    
    pushWorklist(vm,td);
}

static void copyEpoch(fivmr_TypeData *td,int32_t trgEpoch,int32_t srcEpoch) {
    fivmr_TypeEpoch *src,*trg;
    fivmr_VM *vm;
    
    LOG(1,("Copying epoch in %s (%p): %d -> %d",td->name,td,srcEpoch,trgEpoch));
    
    vm=fivmr_TypeData_getVM(td);
    
    /* what we need to copy:
       - buckets/bucket/tid
       - itable/itableOff/itableLen
       - if we're an interface then all interface method indices */
    
    /* do the TypeEpoch structure first */
    src=td->epochs+srcEpoch;
    trg=td->epochs+trgEpoch;

    /* free the old stuff */
    if (fivmr_TypeData_itableMalloced(td,trgEpoch)) {
        if (trg->itableLen==0) {
            fivmr_assert(trg->itable==NULL);
            fivmr_assert(trg->itableOff==0);
        } else {
            fivmr_assert(trg->itable!=NULL);
            fivmr_free(trg->itable+trg->itableOff);
        }
    }
    if (fivmr_TypeData_bucketsMalloced(td,trgEpoch)) {
        fivmr_free(trg->buckets);
    }

    /* copy the fields over */
    *trg=*src;
    
    /* deal with the dynamically allocated parts */
    if (trg->itableLen!=0) {
        trg->itable=fivmr_mallocAssert(sizeof(void*)*trg->itableLen);
        trg->itable-=trg->itableOff;
        memcpy(trg->itable+trg->itableOff,
               src->itable+trg->itableOff,
               sizeof(void*)*trg->itableLen);
    }
    
    trg->buckets=fivmr_mallocAssert(sizeof(int8_t)*vm->numBuckets);
    memcpy(trg->buckets,src->buckets,sizeof(int8_t)*vm->numBuckets);
    
    fivmr_TypeData_setBucketsMalloced(td,trgEpoch,true);
    fivmr_TypeData_setItableMalloced(td,trgEpoch,true);
    
    /* and now if it's an interface then deal with it */
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION) {
        unsigned i;
        for (i=0;i<td->numMethods;++i) {
            fivmr_MethodRec *mr=td->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                fivmr_MethodRec_setItableIndexForEpoch(
                    mr,trgEpoch,
                    fivmr_MethodRec_itableIndexForEpoch(mr,srcEpoch));
            }
        }
    }
}

static void integrate(fivmr_TypeData *td) {
    bool usingNewEpochs=false;
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    uint32_t *usedBuckets;
    uint32_t *usedItables;
    FindBucketsData fbdata;
    FindItablesData fidata;
    int32_t i,j;
    int32_t foundBucket;
    int32_t foundTid;
    uintptr_t result;
    void **itable;
    
    LOG(1,("Integrating type %s (%p)",td->name,td));
    
    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    fivmr_assert(fivmr_Settings_canDoClassLoading(&vm->settings));
    
    initWorklist(vm);
    pushWorklist(vm,td);
    
    /* policy: we know that Payload_copy does not copy displays or itables.  Thus,
       before any modifications are made to pre-existing displays or itables, we
       must make a copy. */
    
    LOG(2,("Adding %s (%p) to subtype lists of supertypes",td->name,td));
    
    addSubtype(td->parent,td);
    for (i=0;i<td->nSuperInterfaces;++i) {
        addSubtype(td->superInterfaces[i],td);
    }
    
    LOG(2,("Incrementing descendant counts of ancestors of %s (%p)",td->name,td));
    
    fivmr_TypeData_forAllAncestorsInclusive(td,incrementDescendants_cback,0);
    
    LOG(2,("Assigning tid and bucket for %s (%p)",td->name,td));
    
    usedBuckets=fivmr_malloc(sizeof(uint32_t)*((vm->numBuckets+31)/32));
    bzero(usedBuckets,sizeof(uint32_t)*((vm->numBuckets+31)/32));
    
    fbdata.usedBuckets=usedBuckets;
    fbdata.typeEpoch=ts->typeEpoch^1;
    
    fivmr_TypeData_forAllAncestors(td,findBuckets_cback,(uintptr_t)(void*)&fbdata);
    
    findBucketAndTid(vm,&usedBuckets,&foundBucket,&foundTid);
    
    for (i=0;i<2;++i) {
        td->epochs[i].bucket=foundBucket;
        td->epochs[i].tid=foundTid;
    }
    
    LOG(1,("Have tid/bucket for %s (%p): tid = %d, bucket = %d",
           td->name,td,foundTid,foundBucket));
    
    LOG(2,("Building type display for %s (%p)",td->name,td));
    
    for (i=0;i<2;++i) {
        td->epochs[i].buckets=fivmr_mallocAssert(sizeof(int8_t)*vm->numBuckets);
        fivmr_TypeData_setBucketsMalloced(td,i,true);
    }

    /* iterate over all ancestors and populate the buckets; this will fail if
       there is a bucket collision, in which case it'll handle that collision and
       try again. */
    for (;;) {
        td->epochs[vm->typeEpoch^1].buckets=
            fivmr_reallocAssert(td->epochs[vm->typeEpoch^1].buckets,
                                sizeof(int8_t)*vm->numBuckets);
        bzero(td->epochs[vm->typeEpoch^1].buckets,
              sizeof(int8_t)*vm->numBuckets);
        result=fivmr_TypeData_forAllAncestorsSortedInclusive(
            td,populateBuckets_cback,(uintptr_t)(void*)vm);
        if (result==0) {
            /* success!  no collisions */
            break;
        }
        
        LOG(1,("Detected collision in type displays of %s (%p): on type %s (%p)",
               td->name,td,((fivmr_TypeData*)(void*)result)->name,result));
        
        vm->numBucketCollisions++;
        
        handleBucketCollision((fivmr_TypeData*)(void*)result,
                              td,
                              &usedBuckets /* just reusing memory */);
    }
    
    fivmr_free(usedBuckets);
    
    LOG(2,("Type display built for %s (%p); now handling interfaces",td->name,td));
    
    /* ok - now handle ifaces.  if this is an iface, then pick itable indices for
       all of the methods such that we don't overlap with any iface methods from
       our superclasses -and- we use the least occupied itable entries. */
    
    usedItables=fivmr_mallocAssert(sizeof(uint32_t)*((vm->itableSize+31)/32));
    bzero(usedItables,sizeof(uint32_t)*((vm->itableSize+31)/32));
    
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION) {
        LOG(2,("Finding itable indices for %s (%p)",td->name,td));
        
        fidata.usedItables=usedItables;
        fidata.typeEpoch=vm->typeEpoch^1;
        
        fivmr_TypeData_forAllAncestors(td,findItables_cback,(uintptr_t)(void*)&fidata);
        
        findItableIndices(td,&usedItables);

        LOG(2,("Itable indices found for %s (%p)",td->name,td));
    }
    
    /* init the itables to zero; anyone going and editing itables will thus not be
       confused. */
    for (i=0;i<2;++i) {
        td->epochs[i].itable=NULL;
        td->epochs[i].itableOff=0;
        td->epochs[i].itableLen=0;
        fivmr_TypeData_setItableMalloced(td,i,true);
    }
    
    /* if it's a class (abstract or not) then populate the itables */
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_VIRTUAL ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT) {
        LOG(2,("Populating itable for %s (%p)",td->name,td));
    
        for (;;) {
            td->epochs[vm->typeEpoch^1].itable=
                fivmr_reallocAssert(td->epochs[vm->typeEpoch^1].itable,
                                    sizeof(void*)*vm->itableSize);
            td->epochs[vm->typeEpoch^1].itableOff=0;
            td->epochs[vm->typeEpoch^1].itableLen=vm->itableSize;
            bzero(td->epochs[vm->typeEpoch^1].itable,
                  sizeof(void*)*vm->itableSize);
            
            result=fivmr_TypeData_forAllAncestorsSorted(td,populateItable_cback,(uintptr_t)(void*)vm);
            if (result==0) {
                /* success!  no collisions */
                break;
            }
            
            LOG(1,("Detected collision in itable of %s (%p): on type %s (%p)",
                   td->name,td,((fivmr_TypeData*)(void*)result)->name,result));
            
            vm->numItableCollisions++;
    
            handleItableCollision((fivmr_TypeData*)(void*)result,
                                  td,
                                  &usedItables);
        }
        
        LOG(2,("Itable populated for %s (%p)",td->name,td));
        
        shrinkItable(td,vm->typeEpoch^1);

        LOG(2,("Itable shrunk for %s (%p)",td->name,td));
    }
    
    fivmr_free(usedItables);
    
    /* increment the epoch and perform a soft handshake if we modified any tables
       other than our own */
    
    if (vm->wlN==1) {
        LOG(1,("We only modified one type while integrating %s (%p); epochs copied",td->name,td));

        fivmr_assert(vm->wl[0]==td);
        
        copyEpoch(td,vm->typeEpoch,vm->typeEpoch^1);
    } else {
        LOG(1,("We modified %d types while integrating %s (%p); performing soft handshake",
               vm->wlN,td->name,td));

        vm->typeEpoch^=1;
        
        fivmr_ThreadState_softHandshake(
            vm,
            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
            FIVMR_TSEF_PUSH_TYPE_EPOCH);
        
        for (i=0;i<vm->wlN;++i) {
            copyEpoch(vm->wl[i],vm->typeEpoch^1,vm->typeEpoch);
        }
    }
    
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_VIRTUAL ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT) {
        int32_t vtableIdx;
        
        LOG(2,("Creating vtable for %s (%p)",td->name,td));
        
        /* copy vtable from parent */
        memcpy(td->vtable,td->parent->vtable,
               sizeof(void*)*td->parent->vtableLength);

        vtableIdx=td->parent->vtableLength;

        /* pick vtable indices for our methods */
        for (i=0;i<td->numMethods;++i) {
            fivmr_MethodRec *myMR=td->methods[i];
            fivmr_assert(myMR->location==(uintptr_t)(intptr_t)-1);
            if (!(myMR->flags&FIVMR_BF_STATIC) &&
                strcmp(myMR->name,"<init>")) {
                
                fivmr_MethodRec *preMR;
                
                preMR=fivmr_TypeData_findInstMethodNoIface2(
                    vm,td->parent,
                    myMR->name,myMR->result,myMR->nparams,myMR->params);
                
                if (preMR==NULL) {
                    /* this is a new method signature and should get a new vtable
                       index, if the method is not declared final */
                    if ((myMR->flags&FIVMR_MBF_METHOD_KIND)==FIVMR_MBF_FINAL ||
                        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL) {
                        /* don't get a vtable index */
                    } else {
                        /* assign new vtable index */
                        myMR->location=vtableIdx++;
                    }
                } else {
                    /* simple - get parent's vtable index */
                    myMR->location=preMR->location;
                }
                
                if (myMR->location!=(uintptr_t)(intptr_t)-1) {
                    /* we gave ourselves a vtable slot, so populate it */
                    td->vtable[myMR->location]=myMR->entrypoint;
                }
            }
        }
        
        /* make sure we agree on the vtable length */
        fivmr_assert(vtableIdx==td->vtableLength);
    }

    LOG(2,("Integration complete for %s (%p)",td->name,td));
}

static uintptr_t findUnresolved_cback(fivmr_TypeData *startTD,
                                      fivmr_TypeData *curTD,
                                      uintptr_t arg) {
    if (!(curTD->flags&FIVMR_TBF_RESOLUTION_DONE) &&
        (curTD->parent->flags&FIVMR_TBF_RESOLUTION_DONE)) {
        unsigned i;
        for (i=0;i<curTD->nSuperInterfaces;++i) {
            if (!(curTD->superInterfaces[i]->flags&FIVMR_TBF_RESOLUTION_DONE)) {
                return 0;
            }
        }
        return (uintptr_t)(void*)curTD;
    }
    return 0;
}

bool fivmr_TypeData_resolve(fivmr_TypeData *td) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    unsigned i;
    bool result;

    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));

    if ((td->flags&(FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED))) {
        fivmr_fence();
        result=!(td->flags&FIVMR_TBF_RESOLUTION_FAILED);
    } else {
        
        fivmr_Nanos before,after;
        before=fivmr_curTime();
        
        result=true;
        
        /* figure out if there are any ancestors that are unresolved but
           whose ancestors are resolved */
        for (;;) {
            fivmr_ThreadState_goToNative(ts);
            fivmr_Lock_lock(&vm->typeDataLock);
            
            fivmr_TypeData *td2=(fivmr_TypeData*)(void*)
                fivmr_TypeData_forAllAncestors(td,findUnresolved_cback,0);
            
            fivmr_Lock_unlock(&vm->typeDataLock);
            fivmr_ThreadState_goToJava(ts);
            
            if (td2==NULL) {
                break;
            }
            if (!fivmr_TypeData_resolve(td2)) {
                result=false;
            }
        }
        
        /* same for element types */
        if (result) {
            for (;;) {
                bool cont=false;
                fivmr_TypeData *td2;
                for (td2=td->arrayElement;td2!=NULL;td2=td2->arrayElement) {
                    if (!(td2->flags&FIVMR_TBF_RESOLUTION_DONE) &&
                        (td2->arrayElement==NULL ||
                         (td2->arrayElement->flags&FIVMR_TBF_RESOLUTION_DONE))) {
                        if (fivmr_TypeData_resolve(td2)) {
                            cont=true;
                        } else {
                            result=false;
                            cont=false;
                        }
                        break;
                    }
                }
                if (!cont) {
                    break;
                }
            }
        }

        /* assert linker constraints */
        if (result) {
            for (i=0;i<td->numMethods;++i) {
                fivmr_MethodRec *myMR=td->methods[i];
                if (!(myMR->flags&FIVMR_BF_STATIC) &&
                    strcmp(myMR->name,"<init>")) {
                    
                    fivmr_MethodRec *preMR;
                    
                    preMR=fivmr_TypeData_findInstMethodNoIface3(
                        vm,td->parent,
                        myMR->name,myMR->result,myMR->nparams,myMR->params);
                    
                    if (preMR!=NULL &&
                        (!fivmr_TypeStub_union(myMR->result,preMR->result) ||
                         !fivmr_TypeStub_unionParams(myMR->nparams,myMR->params,
                                                     preMR->nparams,preMR->params))) {
                        result=false;
                        break;
                    }
                }
            }
        }
        
        fivmr_ThreadState_goToNative(ts);
        
        fivmr_Lock_lock(&vm->typeDataLock);
        
        if ((td->flags&(FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED))) {
            if (result) {
                fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
                fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_DONE));
            } else {
                fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_FAILED));
                fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_DONE));
            }
        } else {
            int32_t oldFlags;
            
            if (result) {
                /* do this in native since we may want to trigger handshakes */
                /* make sure all of our supertypes have us in their subtype list */
                /* integrate this type into the type epochs and build the vtable */
                integrate(td);
                
                /* check if the type is already effectively initialized. */
                if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT ||
                    (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_VIRTUAL ||
                    (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
                    (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION ||
                    (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE) {
                    bool parentsInited=(td->parent->inited==1);
                    
                    if (parentsInited) {
                        int32_t i;
                        for (i=0;i<td->nSuperInterfaces;++i) {
                            if (td->parent->inited!=1) {
                                parentsInited=false;
                                break;
                            }
                        }
                    }
                    
                    if (parentsInited) {
                        if (fivmr_TypeData_findStaticMethod(vm,td,"<clinit>","()V")
                            == NULL) {
                            td->inited=1;
                        } else {
                            /* we have a clinit - so the type will have to go
                               through checkInit */
                        }
                    } else {
                        /* do nothing - the type will have to go through
                           checkInit because its supertypes are not initialized or
                           else experienced an error during initialization */
                    }
                } else {
                    /* arrays are always initialized
                       FIXME: that might not be totally true, especially for arrays
                       that have backing classes.  but those arrays are currently
                       part of the executable, so the initial initialization state
                       is the compiler's problem (i.e. this case is irrelevant here). */
                    td->inited=1;
                }
                
                oldFlags=fivmr_BitField_setAtomic(
                    &td->flags,
                    FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED,
                    FIVMR_TBF_RESOLUTION_DONE);
            } else {
                oldFlags=fivmr_BitField_setAtomic(
                    &td->flags,
                    FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED,
                    FIVMR_TBF_RESOLUTION_FAILED);
            }
            
            fivmr_assert(!(oldFlags&FIVMR_TBF_RESOLUTION_DONE));
            fivmr_assert(!(oldFlags&FIVMR_TBF_RESOLUTION_FAILED));
        }
        
        fivmr_Lock_unlock(&vm->typeDataLock);
        
        fivmr_ThreadState_goToJava(ts);

        /* indicate that we've already done resolution on this class */
        fivmr_fence();
        
        fivmr_assert((td->flags&(FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_DONE)));
        
        after=fivmr_curTime();
        LOG(1,("Resolving %s took %u ns",
               td->name,(unsigned)(after-before)));
    }
    
    return result;
}

static fivmr_TypeData *findDefined(fivmr_TypeContext *ctx,
                                   const char *name) {
    fivmr_TypeData *result;
    ftree_Node *node;
    
    result=fivmr_StaticTypeContext_find(&ctx->st,name);
    if (result!=NULL) {
        return result;
    }
    
    node=ftree_Tree_findFast(&ctx->dynamicTypeTree,
                             (uintptr_t)(void*)name,
                             fivmr_TypeData_compareKey);
    if (node==NULL) {
        return NULL;
    } else {
        return (fivmr_TypeData*)(void*)node->value;
    }
}

static fivmr_TypeData *findArrayInNative(fivmr_TypeData *td) {
    int len;
    char *name;
    fivmr_TypeData *result;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    
    fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB);
    if (td->arrayType!=NULL) {
        return td->arrayType;
    }
    
    /* FIXME: is the rest of this necessary? */
    
    ts=fivmr_ThreadState_get(fivmr_TypeData_getVM(td));
    fivmr_assert(fivmr_ThreadState_isInNative(ts));

    ctx=fivmr_TypeContext_fromStatic(td->context);

    len=strlen(td->name)+2;
    name=alloca(len);
    snprintf(name,len,"[%s",td->name);

    fivmr_Lock_lock(&ctx->treeLock);
    result=findDefined(ctx,name);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    return result;
}

static fivmr_TypeData *findArrayInJava(fivmr_TypeData *td) {
    fivmr_TypeData *result;
    fivmr_ThreadState *ts;
    fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB);
    if (td->arrayType!=NULL) {
        return td->arrayType;
    }
    ts=fivmr_ThreadState_get(fivmr_TypeData_getVM(td));
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    fivmr_ThreadState_goToNative(ts);
    result=findArrayInNative(td);
    fivmr_ThreadState_goToJava(ts);
    return result;
}

static fivmr_TypeData *makeArrayImpl(fivmr_TypeData *eleTD) {
    int len;
    char *name;
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    fivmr_TypeData *td;
    ftree_Node *node;
    fivmr_TypeData *oldTD;
    unsigned i;
    
    len=strlen(eleTD->name)+2;
    name=alloca(len);
    snprintf(name,len,"[%s",eleTD->name);

    ctx=fivmr_TypeContext_fromStatic(eleTD->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);

    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    /* do this in native since we may want to trigger handshakes */
    fivmr_ThreadState_goToNative(ts);
    
    fivmr_Lock_lock(&ctx->treeLock);
    fivmr_Lock_lock(&vm->typeDataLock);
    
    td=findDefined(ctx,name);
    if (td==NULL) {
        td=fivmr_mallocAssert(fivmr_TypeData_sizeOfTypeData(vm->payload->td_Object));
        
        /* do some random stuff to mark this as a "blank" array type, including
           linking it to its element type and locating its supertypes */
        
        memcpy(td,
               vm->payload->td_Object,
               fivmr_TypeData_sizeOfTypeData(vm->payload->td_Object));
        
        td->state=FIVMR_MS_INVALID;
        td->forward=td;
        td->context=&ctx->st;
        td->inited=1; /* initialized successfully */
        td->curIniter=NULL;
        td->name=strdup(name);
        fivmr_assert(td->name!=NULL);
        td->filename=NULL;
        td->flags=eleTD->flags;
        td->flags&=~FIVMR_TBF_TYPE_KIND;
        td->flags|=FIVMR_TBF_ARRAY;
        td->flags&=~FIVMR_TBF_RESOLUTION_DONE;
        fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
        td->parent=findArrayInNative(eleTD->parent);
        fivmr_assert(td->parent!=NULL);
        td->nSuperInterfaces=eleTD->nSuperInterfaces;
        td->superInterfaces=fivmr_mallocAssert(
            sizeof(fivmr_TypeData*)*td->nSuperInterfaces);
        for (i=0;i<td->nSuperInterfaces;++i) {
            td->superInterfaces[i]=findArrayInNative(eleTD->superInterfaces[i]);
            fivmr_assert(td->superInterfaces[i]!=NULL);
        }
        td->nDirectSubs=0;
        td->directSubs=NULL;
        td->ilistSize=0;
        td->ilist=NULL;
        td->canonicalNumber=0;
        td->numDescendants=0; /* integrate will increment this */
        bzero(td->epochs,sizeof(fivmr_TypeEpoch)*2);
        td->arrayElement=eleTD;
        td->arrayType=NULL;
        td->size=0;
        td->requiredAlignment=0;
        td->refSize=FIVMSYS_PTRSIZE;
        td->bytecode=0;
        td->classObject=0;
        td->node=NULL; /* this will be set by defineImpl */
        td->numMethods=0;
        td->numFields=0;
        td->methods=NULL;
        td->fields=NULL;
        td->gcMap=0;
        
        /* is that really it? */
        
        fivmr_allocateClass(ts,td);
        
        /* note: the Class cannot be GC'd here because we're returning a handle
           in some native context.  FIXME: figure out the native context story
           so as to ensure that these temporary handles get freed eventually. */
        
        if (ts->curExceptionHandle==NULL) {
            fivmr_assert(eleTD->arrayType==NULL);

            /* need to go to Java because that's what defineImpl expects */
            fivmr_ThreadState_goToJava(ts);
    
            oldTD=defineImpl(ctx,td);
            
            if (oldTD!=td) {
                /* the only way for defineImpl to have failed is if linker constraints
                   had been violated, in which case defineImpl would return a
                   different array type. */
                fivmr_assert(oldTD->arrayElement!=td->arrayElement);
                
                fivmr_TypeData_free(td);
                td=NULL;
            } else {
                eleTD->arrayType=td;
            }
            
        } else {
            ts->curExceptionHandle=NULL;
            fivmr_TypeData_free(td);
            td=NULL;
        }
    }
    
    fivmr_Lock_unlock(&vm->typeDataLock);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    if (fivmr_ThreadState_isInNative(ts)) {
        fivmr_ThreadState_goToJava(ts);
    }
    
    return td;
}

static uintptr_t findArraysToMake_cback(fivmr_TypeData *startTD,
                                        fivmr_TypeData *curTD,
                                        uintptr_t arg) {
    LOG(2,("Considering making array for %s (%p)",curTD->name,curTD));
    if (findArrayInJava(curTD)==NULL) {
        if (findArrayInJava(curTD->parent)!=NULL) {
            unsigned i;
            for (i=0;i<curTD->nSuperInterfaces;++i) {
                if (findArrayInJava(curTD->superInterfaces[i])==NULL) {
                    LOG(2,("Rejecting %s (%p) because at least one of its superinterfaces (%s (%p)) lacks an array.",curTD->name,curTD,curTD->superInterfaces[i]->name,curTD->superInterfaces[i]));
                    return 0;
                }
            }
            /* ok - this guy doesn't have an array but all of his ancestors
               do.  so he's the target. */
            return (uintptr_t)(void*)curTD;
        } else {
            LOG(2,("Rejecting %s (%p) because its supertype (%s (%p)) lacks an array.",curTD->name,curTD,curTD->parent->name,curTD->parent));
        }
    } else {
        LOG(2,("Rejecting %s (%p) because it already has an array.",curTD->name,curTD));
    }
    return 0;
}

fivmr_TypeData *fivmr_TypeData_makeArray(fivmr_TypeData *td) {
    fivmr_TypeData *result;
    
    result=findArrayInJava(td);
    
    if (result==NULL) {
        /* this is a horrid non-recursive way of ensuring that all supertype
           arrays are made, as well.  I'm doing it this way to avoid
           recursion. */
        
        for (;;) {
            LOG(2,("Trying to figure out how to make an array for %s (%p)",td->name,td));
            fivmr_TypeData *target=(fivmr_TypeData*)(void*)
                fivmr_TypeData_forAllAncestorsInclusive(
                    td,findArraysToMake_cback,0);
            if (target==NULL) {
                break;
            }
            LOG(2,("Making array for %s (%p)",target->name,target));
            result=makeArrayImpl(target);
            if (result==NULL) {
                return NULL;
            }
        }
        
        result=findArrayInJava(td);
        fivmr_assert(result!=NULL);
    }
    
    return result;
}

void fivmr_TypeData_free(fivmr_TypeData *td) {
    fivmr_FieldRec *fields;
    fivmr_MethodRec **methods;
    unsigned i;
    
    fivmr_freeIfNotNull(td->name);
    fivmr_freeIfNotNull(td->superInterfaces);
    fivmr_freeIfNotNull(td->directSubs);
    fivmr_freeIfNotNull(td->ilist);
    fivmr_freeIfNotNull(td->filename);
    
    fields=td->fields;
    if (fields!=NULL) {
        for (i=0;i<td->numFields;++i) {
            fivmr_FieldRec *fr=fields+i;
            fivmr_freeIfNotNull(fr->name);
        }
        fivmr_free(fields);
    }
    
    methods=td->methods;
    if (methods!=NULL) {
        for (i=0;i<td->numMethods;++i) {
            fivmr_MethodRec *mr=methods[i];
            if (mr!=NULL) {
                fivmr_freeIfNotNull(mr->name);
                fivmr_freeIfNotNull(mr->params);
                fivmr_free(mr);
            }
        }
        fivmr_free(methods);
    }
    
    fivmr_free(td);
}

bool fivmr_TypeData_fixEntrypoint(fivmr_TypeData *td,
                                  void *oldEntrypoint,
                                  void *newEntrypoint) {
    /* NOTE: we would not benefit at all from knowing the original MethodRec's
       location, since there may be multiple MethodRecs that this one overrides
       or implements.  so we trade-off speed for a reduction in necessary
       book-keeping. */
    
    int i,j;
    bool result=false;
    
    for (i=0;i<td->vtableLength;++i) {
        if (td->vtable[i]==oldEntrypoint) {
            td->vtable[i]=newEntrypoint;
            result=true;
        }
    }
    
    for (j=0;j<2;++j) {
        fivmr_TypeEpoch *e=td->epochs+j;
        for (i=e->itableOff;i<e->itableOff+e->itableLen;++i) {
            if (e->itable[i]==oldEntrypoint) {
                e->itable[i]=newEntrypoint;
                result=true;
            }
        }
    }
    
    return true;
}

void *fivmr_TypeContext_addUntracedField(fivmr_TypeContext *ctx,
                                         int32_t size) {
    void *result;
    
    if (FIVMR_ASSERTS_ON) {
        fivmr_ThreadState *ts;
        ts=fivmr_ThreadState_get(ctx->vm);
        fivmr_assert(fivmr_ThreadState_isInNative(ts));
    }
    
    fivmr_Lock_lock(&ctx->treeLock);
    result=fivmr_TypeAux_addUntracedZero(ctx,&ctx->aux,(size_t)size);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    return result;
}

void *fivmr_TypeContext_addTracedField(fivmr_TypeContext *ctx) {
    void *result;

    if (FIVMR_ASSERTS_ON) {
        fivmr_ThreadState *ts;
        ts=fivmr_ThreadState_get(ctx->vm);
        fivmr_assert(fivmr_ThreadState_isInNative(ts));
    }
    
    fivmr_Lock_lock(&ctx->treeLock);
    result=fivmr_TypeAux_addPointer(ctx,&ctx->aux,0);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    return result;
}

