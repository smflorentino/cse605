/*
 * fivmr_payload.c
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

#include <fivmr.h>

static fivmr_TypeStub **copyTypeList(fivmr_TypeStub **tdListOrig,
                                     int32_t listLen,
                                     void *region,
                                     fivmr_OTH *tdForward) {
    fivmr_TypeStub **tdListNew;
    int32_t j;
    
    if (listLen==0) {
        return NULL;
    }
    
    tdListNew=freg_region_alloc(region,
                                sizeof(fivmr_TypeStub*)*listLen);
    fivmr_assert(tdListNew!=NULL);
    
    for (j=0;j<listLen;++j) {
        tdListNew[j]=fivmr_OTH_get(tdForward,tdListOrig[j]);
    }
    
    return tdListNew;
}

static void fixObjHeader(fivmr_Object obj,
                         fivmr_Settings *settings,
                         fivmr_TypeData *td) {
    fivmr_ObjHeader_init(
        settings,
        fivmr_ObjHeader_forObject(settings,
                                  obj),
        (fivmr_Monitor*)td,
        FIVMR_OHF_ZERO);
}

fivmr_Payload *fivmr_Payload_copy(fivmr_Payload *orig) {
    fivmr_Payload *result;
    fivmr_OTH tdForward;
    fivmr_OTH mrForward;
    fivmr_OTH cmrForward;
    int32_t i,j;
    int32_t nMethods;
    uintptr_t curObj;
    uintptr_t objStep;
    uintptr_t stringArrayBaseOld;
    uintptr_t stringArrayBaseNew;
    uintptr_t stringArraySize;
    fivmr_TypeData **tdPtr;
    fivmr_Object stringArray;
    int32_t nTypes,nStubs;
    
    LOG(2,("begin payload copy"));

    fivmr_debugMemory();

    /* NOTE: this function is faster than I had feared but it still needs more
       perf tests and possibly perf optimizations... */
    
    /* assert that this is not a free'd immortal payload or a one-shot payload */
    fivmr_assert(orig->mode!=FIVMR_PL_INVALID);
    fivmr_assert(orig->mode!=FIVMR_PL_IMMORTAL_ONESHOT);
    
    result=freg_region_create(sizeof(fivmr_Payload));
    fivmr_assert(result!=NULL);
    
    memcpy(result,orig,sizeof(fivmr_Payload));
    
    result->mode=FIVMR_PL_COPY;
    
    nTypes=orig->nTypes;
    nStubs=orig->nStubs;

    /* first copy the type contexts */
    result->contexts=freg_region_alloc(result,
                                       sizeof(fivmr_StaticTypeContext)*orig->nContexts);
    fivmr_assert(result->contexts!=NULL);
    
    memcpy(result->contexts,
           orig->contexts,
           sizeof(fivmr_StaticTypeContext)*orig->nContexts);
    
    for (i=0;i<orig->nContexts;++i) {
        result->contexts[i].payload=result;
    }
    
    /* now copy all of the types */
    result->typeList=freg_region_alloc(result,
                                       sizeof(fivmr_TypeData*)*nTypes);
    fivmr_assert(result->typeList!=NULL);
    
    if (nStubs!=0) {
        result->stubList=freg_region_alloc(result,
                                           sizeof(fivmr_TypeData*)*nStubs);
        fivmr_assert(result->typeList!=NULL);
    }
    
    fivmr_OTH_initEasy(&tdForward,nTypes+nStubs);
    
    nMethods=0;
    
    LOG(2,("  begin copy types"));
    
    for (i=0;i<orig->nTypes;++i) {
        /* we copy: TypeDatas, MethodRecs, and FieldRecs */
        
        fivmr_TypeData *tdOrig;
        fivmr_TypeData *tdNew;
        
        tdOrig=orig->typeList[i];
        tdNew=freg_region_alloc(result,
                                fivmr_TypeData_sizeOfTypeData(tdOrig));
        fivmr_assert(tdNew!=NULL);
        result->typeList[i]=tdNew;
        
        memcpy(tdNew,tdOrig,fivmr_TypeData_sizeOfTypeData(tdOrig));
        
        fivmr_OTH_put(&tdForward,tdOrig,tdNew);
        
        /* fixup context pointer now */
        j=tdOrig->context-orig->contexts;
        fivmr_assert(j>=0 && j<orig->nContexts);
        tdNew->context=result->contexts+j;
        
        nMethods+=tdOrig->numMethods;
        
        /* fixup type bodies later */
    }
    
    memcpy(result->stubList,
           orig->stubList,
           sizeof(fivmr_TypeStub)*nStubs);
    
    for (i=0;i<orig->nStubs;++i) {
        fivmr_TypeStub *ts=result->stubList+i;
        j=ts->context-orig->contexts;
        fivmr_assert(j>=0 && j<orig->nContexts);
        ts->context=result->contexts+j;

        fivmr_OTH_put(&tdForward,orig->stubList+i,ts);
    }
    
    LOG(2,("  begin copy intrinsic types"));

    /* FIXME: this might break for some compilers */
    for (tdPtr=&result->td_top;tdPtr<=&result->td_ClassArr;tdPtr++) {
        *tdPtr=fivmr_OTH_get(&tdForward,*tdPtr);
    }
    
    fivmr_OTH_initEasy(&mrForward,nMethods);
    
    LOG(2,("  begin type fixup"));

    for (i=0;i<orig->nTypes;++i) {
        fivmr_TypeData *tdOrig;
        fivmr_TypeData *tdNew;
        
        tdOrig=orig->typeList[i];
        tdNew=result->typeList[i];
        
        tdNew->forward=tdNew;
        tdNew->parent=fivmr_OTH_get(&tdForward,tdNew->parent);
        
        tdNew->superInterfaces=(fivmr_TypeData**)
            copyTypeList((fivmr_TypeStub**)tdNew->superInterfaces,
                         tdNew->nSuperInterfaces,
                         result,
                         &tdForward);
        
        tdNew->ilist=(fivmr_TypeData**)
            copyTypeList((fivmr_TypeStub**)tdNew->ilist,
                         tdNew->ilistSize,
                         result,
                         &tdForward);
        
        tdNew->arrayElement=fivmr_OTH_get(&tdForward,tdNew->arrayElement);
        tdNew->arrayType=fivmr_OTH_get(&tdForward,tdNew->arrayType);
        
        if (tdNew->numMethods!=0) {
            tdNew->methods=
                freg_region_alloc(result,
                                  sizeof(fivmr_MethodRec*)*tdNew->numMethods);
            fivmr_assert(tdNew->methods!=NULL);
            for (j=0;j<tdOrig->numMethods;++j) {
                fivmr_MethodRec *mrOld;
                fivmr_MethodRec *mrNew;
                
                mrOld=tdOrig->methods[j];
                mrNew=freg_region_alloc(result,
                                        sizeof(fivmr_MethodRec));
                fivmr_assert(mrNew!=NULL);
                tdNew->methods[j]=mrNew;
                
                memcpy(mrNew,mrOld,sizeof(fivmr_MethodRec));
                
                fivmr_OTH_put(&mrForward,mrOld,mrNew);
                
                mrNew->owner=tdNew;
                mrNew->result=fivmr_OTH_get(&tdForward,mrNew->result);
                
                mrNew->params=copyTypeList(mrNew->params,
                                           mrNew->nparams,
                                           result,
                                           &tdForward);
            }
        }
        
        if (tdNew->numFields!=0) {
            tdNew->fields=
                freg_region_alloc(result,
                                  sizeof(fivmr_FieldRec)*tdNew->numFields);
            fivmr_assert(tdNew->fields!=NULL);
            
            memcpy(tdNew->fields,tdOrig->fields,
                   sizeof(fivmr_FieldRec)*tdNew->numFields);
            
            for (j=0;j<tdOrig->numFields;++j) {
                fivmr_FieldRec *frOld=tdOrig->fields+j;
                fivmr_FieldRec *frNew=tdNew->fields+j;
                
                frNew->owner=tdNew;
                frNew->type=fivmr_OTH_get(&tdForward,frNew->type);
            }
        }
    }
    
    LOG(2,("  begin handle debug table"));

    /* what we have left to fixup in types: the classObject pointer */
    
    fivmr_OTH_initEasy(&cmrForward,result->nDebugIDs); /* needed for
                                                          CompactMethodRec magic */

    /* copy debug records (and fixup to point to right MethodRecs) */
    result->debugTable=freg_region_alloc(result,
                                         sizeof(fivmr_DebugRec)*result->nDebugIDs);
    fivmr_assert(result->debugTable!=NULL);
    for (i=0;i<result->nDebugIDs;++i) {
        fivmr_DebugRec *drOld;
        fivmr_DebugRec *drNew;
        
        drOld=orig->debugTable+i;
        drNew=result->debugTable+i;
        
        drNew->ln_rm_c=drOld->ln_rm_c; /* thin or fat, we don't need to copy
                                          the FatDebugData */
        
        /* copy the low bits of method */
        /* FIXME: we should have a better way of managing these "special bits" ...
           this approach will get very annoying, very quickly. */
        drNew->method=drOld->method&(sizeof(void*)-1);
        
        if ((drNew->ln_rm_c&FIVMR_DR_INLINED)) {
            /* inlined code */
            fivmr_InlineMethodRec *imrOld;
            uintptr_t *imrNewPtr;
            fivmr_InlineMethodRec *imrNew;
            
            imrOld=(fivmr_InlineMethodRec*)(drOld->method&~(sizeof(void*)-1));
            imrNewPtr=&drNew->method;
            
            for (;;) {
                fivmr_MethodRec *inlinedMR;
                
                imrNew=freg_region_alloc(result,
                                         sizeof(fivmr_InlineMethodRec));
                fivmr_assert(imrNew!=NULL);
                *imrNewPtr|=(uintptr_t)imrNew;
                
                imrNew->ln_c=imrOld->ln_c;
                inlinedMR=fivmr_OTH_get(&mrForward,imrOld->method);
                if (inlinedMR==NULL) {
                    /* imrOld->method must be a CompactMethodRec - so do some magic. */
                    inlinedMR=fivmr_OTH_get(&cmrForward,imrOld->method);
                    if (inlinedMR==NULL) {
                        /* haven't handled this one yet - so create one */
                        inlinedMR=(fivmr_MethodRec*)
                            freg_region_alloc(result,
                                              sizeof(fivmr_CompactMethodRec));
                        fivmr_assert(inlinedMR!=NULL);
                        inlinedMR->owner=fivmr_OTH_get(&tdForward,imrOld->method->owner);
                        inlinedMR->name=imrOld->method->name;
                        inlinedMR->flags=imrOld->method->flags;
                        inlinedMR->result=fivmr_OTH_get(&tdForward,imrOld->method->result);
                        inlinedMR->nparams=imrOld->method->nparams;
                        inlinedMR->params=copyTypeList(imrOld->method->params,
                                                       imrOld->method->nparams,
                                                       result,
                                                       &tdForward);
                        fivmr_OTH_put(&cmrForward,imrOld->method,inlinedMR);
                    }
                }
                imrNew->method=inlinedMR;
                fivmr_assert(imrNew->method!=NULL);
                if ((imrOld->ln_c&FIVMR_IMR_INLINED)) {
                    imrOld=(fivmr_InlineMethodRec*)imrOld->caller;
                    imrNewPtr=(uintptr_t*)&imrNew->caller;
                    *imrNewPtr=0;
                } else {
                    imrNew->caller=(uintptr_t)
                        fivmr_OTH_get(&mrForward,(void*)imrOld->caller);
                    fivmr_assert(imrNew->caller!=0);
                    break;
                }
            }
        } else {
            /* in the outer method */
            drNew->method|=(uintptr_t)
                fivmr_OTH_get(&mrForward,(void*)(drOld->method&~(sizeof(void*)-1)));
            if (FIVMR_ASSERTS_ON && drNew->method==0) {
                LOG(0,("drNew->method=%p, drOld->method=%p, drNew=%p, drOld=%p",
                       drNew->method,
                       drOld->method,
                       drNew,
                       drOld));
                fivmr_assert(drNew->method!=0);
            }
        }
    }
    
    LOG(2,("  begin copy strings"));

    /* copy strings (and fixup objects to point to right TypeDatas) */
    objStep=fivmr_getStringDistance(orig);
    
    result->stringTable=freg_region_alloc(result,
                                          fivmr_alignRaw(objStep*orig->nStrings,
                                                         sizeof(int64_t)));
    fivmr_assert(result->stringTable!=NULL);
    
    memcpy(result->stringTable,
           orig->stringTable,
           fivmr_alignRaw(objStep*orig->nStrings,
                          sizeof(int64_t)));
    
    stringArray=(uintptr_t)orig->stringArray+FIVMR_ALLOC_OFFSET(&orig->settings);
    
    stringArraySize=
        fivmr_Object_size(&orig->settings,
                          stringArray);
    stringArrayBaseOld=(uintptr_t)orig->stringArray;
    stringArrayBaseNew=(uintptr_t)
        freg_region_alloc(result,
                          stringArraySize);
    fivmr_assert(stringArrayBaseNew!=0);
    
    memcpy((void*)stringArrayBaseNew,
           (void*)stringArrayBaseOld,
           stringArraySize);

    result->stringArray=(uintptr_t*)stringArrayBaseNew;
    stringArray=(uintptr_t)result->stringArray+FIVMR_ALLOC_OFFSET(&orig->settings);
    
    /* FIXME: for some libraries or object models, this will not be a charArr */
    fixObjHeader(stringArray,&orig->settings,result->td_charArr);
    
    curObj=fivmr_getFirstString(result);
    
    for (i=0;i<orig->nStrings;++i) {
        fixObjHeader(curObj,&orig->settings,result->td_String);

        *(fivmr_Object*)(curObj+orig->stringArrOffset)=stringArray;
        
        curObj+=objStep;
    }
    
    result->stringIndex=freg_region_alloc(result,
                                          sizeof(fivmr_Object)*orig->nStrings);
    fivmr_assert(result->stringIndex!=0);
    for (i=0;i<orig->nStrings;++i) {
        result->stringIndex[i]=
            orig->stringIndex[i]-(uintptr_t)orig->stringTable+(uintptr_t)result->stringTable;
    }
    
    LOG(2,("  begin copy bytecodes"));
    
    if (FIVMR_CLASSLOADING(&orig->settings)) {
        uintptr_t cur;
        
        result->bytecodeArray=freg_region_alloc(result,orig->bytecodeSize);
        fivmr_assert(result->bytecodeArray!=NULL);
        
        memcpy(result->bytecodeArray,
               orig->bytecodeArray,
               orig->bytecodeSize);
        
        for (cur=(uintptr_t)result->bytecodeArray;
             cur<(uintptr_t)result->bytecodeArray+orig->bytecodeSize;) {
            fivmr_Object obj=cur+FIVMR_ALLOC_OFFSET(&orig->settings);
            
            fixObjHeader(obj,&orig->settings,result->td_byteArr);
            
            cur+=fivmr_Object_size(&orig->settings,obj);
        }
        
        /* fix pointers from types to bytecode */
        for (i=0;i<result->nTypes;++i) {
            result->typeList[i]->bytecode=
                (uintptr_t)result->typeList[i]->bytecode -
                (uintptr_t)orig->bytecodeArray +
                (uintptr_t)result->bytecodeArray;
        }
    }
    
    LOG(2,("  begin copy classes"));

    /* copy classes (and fixup objects to point to right TypeDatas) */
    objStep=fivmr_getClassDistance(orig);
    
    result->classTable=freg_region_alloc(result,
                                         fivmr_alignRaw(objStep*orig->nTypes,
                                                        sizeof(int64_t)));
    fivmr_assert(result->classTable!=NULL);
    
    memcpy(result->classTable,
           orig->classTable,
           fivmr_alignRaw(objStep*orig->nTypes,
                          sizeof(int64_t)));
    
    curObj=fivmr_getFirstClass(result);
    
    for (i=0;i<orig->nTypes;++i) {
        fixObjHeader(curObj,&orig->settings,result->td_Class);
        
        result->typeList[i]->classObject=curObj;
        *(fivmr_TypeData**)(curObj+orig->classTDOffset)=result->typeList[i];
        
        curObj+=objStep;
    }

    /* fixup entrypoint */
    result->entrypoint=fivmr_OTH_get(&tdForward,result->entrypoint);
    
    /* and the patch repo */
    if (orig->patchRepoSize!=0) {
        result->patchRepo=freg_region_alloc(result,
                                            sizeof(void*)*orig->patchRepoSize);
        fivmr_assert(result->patchRepo!=NULL);
        memcpy(result->patchRepo,orig->patchRepo,
               sizeof(void*)*orig->patchRepoSize);
    }
    
    /* cleanup */
    fivmr_OTH_free(&tdForward);
    fivmr_OTH_free(&mrForward);
    fivmr_OTH_free(&cmrForward);
    
    fivmr_debugMemory();

    LOG(2,("  done."));

    return result;
}

bool fivmr_Payload_claim(fivmr_Payload *payload,
                         fivmr_VM *vm) {
    fivmr_assert(payload->mode!=FIVMR_PL_INVALID);
    if (payload->ownedBy==vm) {
        return true;
    } else {
        return fivmr_cas((uintptr_t*)&payload->ownedBy,
                         0,
                         (uintptr_t)vm);
    }
}

bool fivmr_Payload_registerVM(fivmr_Payload *payload,
                              fivmr_VM *vm) {
    int32_t i;
    
    if (!fivmr_Payload_claim(payload,vm)) {
        return false;
    }
    
    vm->payload=payload;
    if (payload->mode==FIVMR_PL_IMMORTAL_ONESHOT) {
        fivmr_assert(payload->primFields!=NULL);
        fivmr_assert(payload->refFields!=NULL);
        vm->primFields=payload->primFields;
        vm->refFields=payload->refFields;
    } else {
        fivmr_assert(payload->primFields==NULL);
        fivmr_assert(payload->refFields==NULL);
        vm->primFields=fivmr_mallocAssert(sizeof(int64_t)*payload->primFieldsLen);
        vm->refFields=fivmr_mallocAssert(sizeof(fivmr_Object)*payload->nRefFields);
    }
    
    vm->nTypes=payload->nTypes;
    
    vm->baseContexts=fivmr_mallocAssert(sizeof(fivmr_TypeContext*)*payload->nContexts);
    for (i=0;i<payload->nContexts;++i) {
        if (FIVMR_CLASSLOADING(&payload->settings)) {
            vm->baseContexts[i]=freg_region_create(sizeof(fivmr_TypeContext));
            fivmr_assert(vm->baseContexts[i]!=NULL);
        } else {
            vm->baseContexts[i]=fivmr_mallocAssert(sizeof(fivmr_TypeContext));
        }
        bzero(vm->baseContexts[i],
              sizeof(fivmr_TypeContext));
        memcpy(&vm->baseContexts[i]->st,
               payload->contexts+i,
               sizeof(fivmr_StaticTypeContext));
        vm->baseContexts[i]->vm=vm;
    }
    
    bzero(vm->primFields,sizeof(int64_t)*payload->primFieldsLen);
    bzero(vm->refFields,sizeof(fivmr_Object)*payload->nRefFields);
    
    for (i=0;i<payload->nTypes;++i) {
        uintptr_t j=payload->typeList[i]->context-payload->contexts;
        fivmr_assert(j<(uintptr_t)payload->nContexts);
        payload->typeList[i]->context=&vm->baseContexts[j]->st;
    }
    
    for (i=0;i<payload->nStubs;++i) {
        uintptr_t j=payload->stubList[i].context-payload->contexts;
        fivmr_assert(j<(uintptr_t)payload->nContexts);
        payload->stubList[i].context=&vm->baseContexts[j]->st;
    }
    
    vm->numBuckets=payload->numBuckets;
    vm->usedTids=fivmr_mallocAssert(sizeof(uint32_t)*(vm->numBuckets*256/32));
    memcpy(vm->usedTids,payload->usedTids,sizeof(uint32_t)*(vm->numBuckets*256/32));
    
    vm->itableSize=payload->itableSize;
    vm->itableOcc=fivmr_mallocAssert(sizeof(int32_t)*vm->itableSize);
    memcpy(vm->itableOcc,payload->itableOcc,sizeof(int32_t)*vm->itableSize);
    
    vm->flags|=FIVMR_VMF_USED_TIDS_MALLOCED|FIVMR_VMF_ITABLE_OCC_MALLOCED;
    
    memcpy(&vm->settings,
           &payload->settings,
           sizeof(fivmr_Settings));
    
    fivmr_GC_registerPayload(&vm->gc);
    
    return true;
}

void fivmr_Payload_free(fivmr_Payload *payload) {
    fivmr_assert(payload->mode!=FIVMR_PL_INVALID);
    if (payload->mode==FIVMR_PL_COPY) {
        freg_region_free(payload);
    } else {
        payload->mode=FIVMR_PL_INVALID;
    }
}


