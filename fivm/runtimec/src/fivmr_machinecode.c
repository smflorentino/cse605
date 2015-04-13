/*
 * fivmr_machinecode.c
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

fivmr_MachineCode *fivmr_MachineCode_create(int32_t size,
                                            fivmr_MachineCodeFlags flags) {
    fivmr_MachineCode *result;
    
    result=freg_region_create_with_steps(sizeof(fivmr_MachineCode),
                                         fivmr_max(
                                             fivmr_min(256,size),
                                             32));
    if (result==NULL) {
        return NULL;
    }
    
    fivmr_assert((flags&FIVMR_MC_GC_OWNED)==0);
    fivmr_assert((flags&FIVMR_MC_GC_MARKED)==0);
    
    bzero(result,sizeof(fivmr_MachineCode));
    
    result->refCount=1;
    result->size=size;
    result->next=NULL;
    result->flags=flags;
    
    result->code=fivmr_allocExecutable(size);
    if (result->code==NULL) {
        fivmr_free(result);
        return NULL;
    }
    
    return result;
}

void fivmr_MachineCode_downsize(fivmr_MachineCode *code,
                                int32_t newSize) {
    if (fivmr_supportDownsizeExec()) {
        fivmr_downsizeExecutable(code->code,newSize);
        code->size=newSize;
    }
}

fivmr_MachineCode *fivmr_MachineCode_up(fivmr_MachineCode *mc) {
    int32_t result=fivmr_xchg_add32(&mc->refCount,1);
    fivmr_assert(result>0);
    return mc;
}

void fivmr_MachineCode_down(fivmr_MachineCode *mc) {
    int32_t result=fivmr_xchg_add32(&mc->refCount,-1);
    fivmr_assert(result>0);
    if (result==1) {
        fivmr_MachineCode *subMC;
        for (subMC=mc->sub;subMC!=NULL;) {
            fivmr_MachineCode *next=subMC->next;
            fivmr_MachineCode_down(subMC);
            subMC=next;
        }
        fivmr_freeExecutable(mc->code);
        freg_region_free(mc);
    }
}

void fivmr_MachineCode_registerMC(fivmr_MachineCode *parent,
                                  fivmr_MachineCode *child) {
    fivmr_VM *vm=fivmr_MachineCode_getVM(parent);
    fivmr_assert(vm==fivmr_MachineCode_getVM(child));
    fivmr_MachineCode_up(child);
    fivmr_Lock_lock(&vm->typeDataLock);
    child->next=parent->sub;
    parent->sub=child;
    fivmr_Lock_unlock(&vm->typeDataLock);
}

void fivmr_MachineCode_appendBasepoint(fivmr_MachineCode *code,
                                       int32_t bytecodePC,
                                       int32_t stackHeight,
                                       void *machinecodePC) {
    fivmr_Basepoint *bp;
    
    LOG(1,("Adding basepoint to %p: bytecodePC = %d, stackHeight = %d, machinecodePC = %p",
           code,bytecodePC,stackHeight,machinecodePC));
    
    bp=freg_region_alloc(code,sizeof(fivmr_Basepoint));
    fivmr_assert(bp!=NULL);
    
    bp->bytecodePC=bytecodePC;
    bp->stackHeight=stackHeight;
    bp->machinecodePC=machinecodePC;
    bp->next=code->bpList;
    
    code->bpList=bp;
}

void fivmr_MachineCode_appendBaseTryCatch(fivmr_MachineCode *code,
                                          int32_t start,
                                          int32_t end,
                                          int32_t target,
                                          fivmr_TypeStub *type) {
    fivmr_BaseTryCatch *btc=freg_region_alloc(code,sizeof(fivmr_BaseTryCatch));
    fivmr_assert(btc!=NULL);
    
    btc->start=start;
    btc->end=end;
    btc->target=target;
    btc->type=type;
    
    btc->next=NULL;
    
    fivmr_assert((code->btcFirst==NULL)==(code->btcLast==NULL));
    
    if (code->btcFirst==NULL) {
        code->btcFirst=btc;
        code->btcLast=btc;
    } else {
        code->btcLast->next=btc;
        code->btcLast=btc;
    }
}


