/*
 * fivmr_cmrgc.c
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

#if !FIVMBUILD__SPECIAL_GC || FIVMBUILD__CMRGC || FIVMBUILD__HFGC

#include <fivmr.h>
#include <fivmr_logger.h>

static inline fivmr_Nanos fivmr_debugTime(void) {
    if (true) {
        return 0;
    } else {
        return fivmr_curTime();
    }
}

static inline bool shouldQuit(fivmr_GC *gc) {
    return fivmr_exiting(fivmr_VMfromGC(gc));
}

static uintptr_t mogrifyHeapSize(fivmr_Settings *settings,
                                 uintptr_t size) {
    if (FIVMR_HFGC(settings)) {
        return (10*size)/13;
    } else {
        return size;
    }
}

/* NEVER call this method while holding gcLock */
static void collectInvoluntarilyFromNative(fivmr_GC *gc,
                                           const char *descrIn,
                                           const char *descrWhat) {
    fivmr_Nanos before;
    fivmr_ThreadState *ts;
    bool shouldTrigger;

    if (fivmr_Thread_isInterrupt()) {
	LOG(1,("collectInvoluntarilyFromNative called from an interrupt."));
	return;
    }

    before=fivmr_curTime();
    
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "blocking involuntarily on GC at %u:%u",2,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff));
    }
    if (gc->logSyncGC) {
        fivmr_Log_lockedPrintf("[Thread %u waiting on Sync GC in %s%s%s]\n",
                               ts->id,descrIn,
                               (descrWhat==NULL?"":", allocating "),
                               (descrWhat==NULL?"":descrWhat));
    }
    
    fivmr_Lock_lock(&gc->gcLock);
    
    ts->gc.gcFlags|=FIVMR_GCDF_REQUESTED_GC;
    shouldTrigger=!fivmr_GC_hasBeenTriggered(gc);
    ts->gc.requesterNext=gc->waiterHead;
    gc->waiterHead=ts;
    
    fivmr_Lock_unlock(&gc->gcLock);

    if (shouldTrigger) {
        LOG(11,("Sending GC trigger."));
        fivmr_CritSemaphore_up(&gc->triggerSema);
        LOG(11,("Trigger sent!"));
    }

    fivmr_Lock_lock(&gc->notificationLock);
    while ((ts->gc.gcFlags&FIVMR_GCDF_REQUESTED_GC) && !shouldQuit(gc)) {
        fivmr_Lock_wait(&gc->notificationLock);
    }
    fivmr_Lock_unlock(&gc->notificationLock);
    
    LOG(2,("Thread %u woke up after involuntary synchronous GC request.",ts->id));
    gc->invBlockTime+=fivmr_curTime()-before;
}

static inline bool markUnmarked(fivmr_GC *gc,
                                fivmr_GCHeader **queue,
                                fivmr_GCHeader *hdr,
                                fivmr_GCHeader *oldHdr,
                                uintptr_t curShaded) {
    fivmr_GCHeader newHdr;
    fivmr_GCHeader_set(&newHdr,curShaded,*queue);
    if (fivmr_GCHeader_cas(hdr,oldHdr,&newHdr)) {
        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_xchg_add(&gc->numMarked,1);
        }
        *queue=hdr;
        return true;
    } else {
        fivmr_assert(fivmr_GCHeader_markBits(hdr)&curShaded);
        return false;
    }
}

static inline bool mark(fivmr_GC *gc,
                        fivmr_GCHeader **queue,
			fivmr_GCHeader *hdr) {
    fivmr_GCHeader oldHdr;
    uintptr_t curShaded;
    fivmr_Settings *settings=&fivmr_VMfromGC(gc)->settings;
    fivmr_TypeData *td;
    /* make sure we're marking an object that is either immortal or is in the heap */
    if (FIVMR_ASSERTS_ON &&
        FIVMR_SELF_MANAGE_MEM(settings) &&
        (hdr->word&FIVMR_GC_MARKBITS_MASK)!=FIVMR_GC_MARKBITS_MASK) {
        fivmr_assert((uintptr_t)hdr>=gc->memStart);
        fivmr_assert((uintptr_t)hdr<gc->memEnd);
    }
    /* make sure that the object has a valid-looking type data */
    fivmr_assert(fivmr_TypeData_forObject(
                     settings,
                     fivmr_GCHeader_toObject(
                         settings,
                         hdr))->state
                 ==FIVMR_MS_INVALID);
    oldHdr=*hdr;
    curShaded=gc->curShaded;
    if ((oldHdr.word&curShaded)==0) {
        return markUnmarked(gc,queue,hdr,&oldHdr,curShaded);
    } else {
        return false;
    }
}

static inline void collectorMarkObj(fivmr_GC *gc,
                                    fivmr_GCHeader **queue,
                                    fivmr_Object obj) {
    if (obj) {
        if (0) printf("collector marking obj %p\n",(void*)obj);
	mark(gc,queue,
             fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                       obj));
    }
}

static inline void collectorMarkObjForScan(fivmr_VM *vm,
                                           fivmr_Object *obj,
                                           void *arg) {
    fivmr_GCHeader **queue=(fivmr_GCHeader**)arg;
    collectorMarkObj(&vm->gc,queue,*obj);
}

static inline void collectorMarkHandle(fivmr_GC *gc,
                                       fivmr_GCHeader **queue,
				       fivmr_Handle *h) {
    if (h!=NULL) {
	fivmr_assert(h->obj!=0);
        if (0) printf("collector marking handle %p\n",(void*)h->obj);
	mark(gc,queue,
             fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                       h->obj));
    }
}

static inline bool mutatorMark(fivmr_ThreadState *ts,
			       fivmr_GCHeader *hdr) {
    fivmr_assert(ts->execStatus==FIVMR_TSES_IN_JAVA ||
		 ts->execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK ||
		 (ts->execFlags&FIVMR_TSEF_BLOCKING) ||
		 fivmr_ThreadHandle_current()==ts->vm->gc.thread);
    if (0) printf("mutator marking %p\n",hdr);
    if (mark(&ts->vm->gc,&ts->gc.queue,hdr)) {
	if (ts->gc.queueTail==NULL) {
	    ts->gc.queueTail=hdr;
	}
	return true;
    } else {
	return false;
    }
}

static inline bool mutatorMarkObj(fivmr_ThreadState *ts,
				  fivmr_Object obj) {
    if (obj) {
	return mutatorMark(ts,
                           fivmr_GCHeader_fromObject(&ts->vm->settings,
                                                     obj));
    } else {
	return false;
    }
}

static inline bool mutatorMarkObjNoisy(fivmr_ThreadState *ts,
				       fivmr_Object obj) {
    if (LOGGING(4)) {
	if (obj) {
	    LOG(4,("Thread %u marking %p (of type %s)",
		   ts->id,obj,fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
	} else {
	    LOG(4,("Thread %u marking null",
		   ts->id));
	}
    }
    if (mutatorMarkObj(ts,obj)) {
	LOG(3,("Thread %u marked %p (of type %s)",
	       ts->id,obj,fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
	return true;
    } else {
	return false;
    }
}

static inline bool mutatorMarkObjStackNoisy(fivmr_ThreadState *ts,
					    fivmr_Object obj) {
    fivmr_GCHeader *hdr;
    LOG(3,("Thread %u marking %p",ts->id,obj));
    if (!obj) {
        LOG(4,("Thread %u marking null",
               ts->id));
        return false;
    }
    if (false) fivmr_assert(obj<0x80000000); /* useful assertion on RTEMS */
    hdr=fivmr_GCHeader_fromObject(&ts->vm->settings,
                                  obj);
    if (false) {
        /* make sure the object is in the heap; this is another assertion
           that may or may not be correct in the general case but is useful
           to enable when debugging gnarly GC bugs on RTEMS */
        if (FIVMR_ASSERTS_ON &&
            FIVMR_SELF_MANAGE_MEM(&ts->vm->settings) &&
            (hdr->word&FIVMR_GC_MARKBITS_MASK)!=FIVMR_GC_MARKBITS_MASK) {
            fivmr_assert((uintptr_t)hdr>=ts->vm->gc.memStart);
            fivmr_assert((uintptr_t)hdr<ts->vm->gc.memEnd);
        }
    }
    if (!fivmr_GCHeader_isScopeID(hdr)) {
	if (mutatorMarkObjNoisy(ts,obj)) {
            LOG(3,("Thread %u marked %p",ts->id,obj));
            return true;
        }
    } else {
	LOG(4,("Thread %u skipped %p (of type %s)",
	       ts->id,obj,fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
    }
    return false;
}

static inline void mutatorMarkObjStackNoisyForScan(fivmr_VM *vm,
                                                   fivmr_Object *obj,
                                                   void *arg) {
    fivmr_ThreadState *ts=(fivmr_ThreadState*)arg;
    mutatorMarkObjStackNoisy(ts,*obj);
}

static inline bool mutatorMarkHandle(fivmr_ThreadState *ts,
				     fivmr_Handle *h) {
    if (h!=NULL) {
	fivmr_assert(h->obj!=0);
	return mutatorMark(ts,
                           fivmr_GCHeader_fromObject(&ts->vm->settings,
                                                     h->obj));
    } else {
	return false;
    }
}

static inline void mutatorMarkHandleNoisy(fivmr_ThreadState *ts,
					  fivmr_Handle *h) {
    if (LOGGING(4)) {
	if (h!=NULL) {
	    LOG(4,("Thread %u marking handle %p, pointing to %p (of type %s)",
		   ts->id,h,h->obj,
                   fivmr_TypeData_forObject(&ts->vm->settings,h->obj)->name));
	} else {
	    LOG(4,("Thread %u marking null handle",
		   ts->id));
	}
    }
    if (mutatorMarkHandle(ts,h)) {
	LOG(3,("Thread %u marked handle %p, pointing to %p (of type %s)",
	       ts->id,h,h->obj,
               fivmr_TypeData_forObject(&ts->vm->settings,h->obj)->name));
    }
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_markSlow)(fivmr_ThreadState *ts,
                                               fivmr_Object obj) {
    fivmr_SPC_incBarrierSlowPath();
    
    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "mark trap at %u:%u on %u",3,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff),
               obj);
    }
    
    fivmr_assert(ts->execStatus==FIVMR_TSES_IN_JAVA ||
		 ts->execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK);
    fivmr_assert(obj);

    if (FIVMR_ASSERTS_ON || FIVMR_LOG_LEVEL>=2) {
        fivmr_GCPhase curPhase;
        curPhase=ts->vm->gc.phase;
    
        /* this code is more verbose and careful */
        
        /* should never see stack-allocated objects */
        if (!FIVMR_HFGC_FAIL_FAST_PATHS(&ts->vm->settings)) {
            fivmr_assert(
                fivmr_GCHeader_markBits(
                    fivmr_GCHeader_fromObject(&ts->vm->settings,
                                              obj))!=0);
        }

        /* we don't want to mark when we're idle because during the idle phase
           at the beginning of GC we may be rotating mark bits.  this prevents
           races where two threads end up double-marking an object because they
           see different values of curShaded.
        
           this is particularly why we query ts->gc.tracing.  it's possible that
           a thread will have used stale shade values in its fast path check.
           worse, memory model issues could mean that the value of gc->curShaded
           we see here could be stale.  we don't want to perform marking with a
           stale value of curShaded.  checking ts->gc.tracing, which is set by
           safepoints and hence side-steps cache coherence, ensures that we only
           proceed when we have the latest shade values. */
        if (fivmr_GCHeader_markBits(
                fivmr_GCHeader_fromObject(&ts->vm->settings,
                                          obj))!=0) {
            if (ts->gc.tracing && fivmr_GC_isTracing(curPhase)) {
                if (mutatorMark(ts,
                                fivmr_GCHeader_fromObject(&ts->vm->settings,
                                                          obj))) {
                    LOG(3,("Thread %u barrier marked %p (of type %s)",
                           ts->id,obj,
                           fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
                }
            } else if (curPhase==FIVMR_GCP_PRE_INIT || fivmr_GC_isTracing(curPhase)) {
                LOG(3,("Thread %u barrier would have marked marked %p (of type %s); "
                       "phase = %d (now %d), status = %" PRIuPTR,
                       ts->id,obj,
                       fivmr_TypeData_forObject(&ts->vm->settings,obj)->name,
                       curPhase,ts->vm->gc.phase,ts->execStatus));
            } else if (!FIVMR_HFGC_FAIL_FAST_PATHS(&ts->vm->settings)) {
                LOG(0,("Thread %u barrier incorrectly saw %p (of type %s)",
                       ts->id,obj,
                       fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
                fivmr_abort("barrier error");
            }
        }
    } else {
        /* this code is carefully written to ensure performance, but doesn't
           do the same verification as the above. */
        if (ts->gc.tracing) {
            fivmr_GCHeader *hdr;
            fivmr_GCHeader oldHdr;
            uintptr_t markBits;
            uintptr_t curShaded;
            fivmr_GC *gc;
            
            gc=&ts->vm->gc;
            
            hdr = fivmr_GCHeader_fromObject(&ts->vm->settings,
                                            obj);
            oldHdr = *hdr;
            markBits = fivmr_GCHeader_markBits(&oldHdr);
            curShaded = gc->curShaded;
            
            if (markBits!=0 &&
                (markBits&curShaded)==0 &&
                markUnmarked(gc,
                             &ts->gc.queue,
                             hdr,
                             &oldHdr,
                             curShaded) &&
                ts->gc.queueTail==NULL) {
                ts->gc.queueTail=hdr;
            }
        }
    }
}

static void addPageToFreelist(fivmr_GC *gc,
                              uintptr_t pageAddr) {
    fivmr_FreePage *page=(fivmr_FreePage*)pageAddr;
    LOG(5,("Adding page to freelist: %p",pageAddr));
    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        fivmr_PageTable_setAssert(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                  pageAddr,
                                  FIVMR_GCPS_ZERO,
                                  FIVMR_GCPS_FREE);
        page->next=gc->freePageHead;
        page->prev=NULL;
        if (gc->freePageHead!=NULL) {
            gc->freePageHead->prev=page;
        }
        gc->freePageHead=page;
    } else {
        page->next=gc->freePageHead;
        gc->freePageHead=page;
    }
}

/* call only while holding the lock */
static void addPagesToFreelist(fivmr_GC *gc,
                               void *start,
			       uintptr_t numPages) {
    uintptr_t i;
    
    gc->numFreePages+=numPages;
    fivmr_assert(gc->numPagesUsed>=0);
    fivmr_assert(gc->numFreePages>=0);
    for (i=0;i<numPages;++i) {
	addPageToFreelist(gc,((uintptr_t)start)+FIVMR_PAGE_SIZE*i);
    }
}

/* call only while holding the lock */
static void *allocPagesToFreelist(fivmr_GC *gc,
                                  uintptr_t numPages) {
    void *start;
    start=fivmr_tryAllocPages(numPages,NULL);
    if (start!=NULL) {
        addPagesToFreelist(gc,start,numPages);
    }
    return start;
}

/* FIXME: the current page management system will lead to priority inversion
   if a low-priority thread makes a request for a large amount of memory that
   cannot be immediately satisfied, and a high-priority thread then tries
   to request a small amount of memory, that could have been immediately
   satisfied. */

static fivmr_FreePage *getPage(fivmr_GC *gc) {
    fivmr_FreePage *result=NULL;
    fivmr_ThreadState *ts;
    fivmr_GCSpace space;
    bool asyncRequested=false;
    
    fivmr_Nanos before=fivmr_debugTime();

    result=NULL;
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));

    LOG(6,("used pages = %p, max pages = %p",
	   gc->numPagesUsed,gc->maxPagesUsed));
    
    if (gc->numPagesUsed==(intptr_t)gc->maxPagesUsed) {
	goto done;
    }
    fivmr_assert(gc->numPagesUsed<=(intptr_t)gc->maxPagesUsed);
    if (gc->numPagesUsed!=(intptr_t)gc->maxPagesUsed) {
	result=gc->freePageHead;
	LOG(5,("Removing page from freelist: %p",result));
        if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            if (result==NULL && gc->nextFreePage<gc->memEnd) {
                fivmr_assert((gc->nextFreePage&(FIVMR_PAGE_SIZE-1))==0);
                result=(fivmr_FreePage*)gc->nextFreePage;
                gc->nextFreePage+=FIVMR_PAGE_SIZE;
                fivmr_assert(
                    fivmr_PageTable_get(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                        (uintptr_t)result)==FIVMR_GCPS_ZERO);
                result->dirty=!gc->isZero;
                gc->numFreePages++; /* hack */
            } else {
                if (false && FIVMR_ASSERTS_ON && result==NULL) {
                    uintptr_t curPage;
                    for (curPage=gc->memStart;
                         curPage<gc->memEnd;
                         curPage+=FIVMR_PAGE_SIZE) {
                        fivmr_assert(
                            fivmr_PageTable_get(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                                curPage)!=FIVMR_GCPS_FREE);
                    }
                }
                fivmr_assert(result!=NULL);
                fivmr_PageTable_setAssert(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                          (uintptr_t)result,
                                          FIVMR_GCPS_FREE,
                                          FIVMR_GCPS_ZERO);
                gc->freePageHead=result->next;
                if (result->next!=NULL) {
                    result->next->prev=NULL;
                }
            }
        } else {
            if (result==NULL) {
                if (allocPagesToFreelist(gc,gc->reqPages)==NULL) {
                    goto done;
                }
                result=gc->freePageHead;
                fivmr_assert(result!=NULL);
            }
            gc->freePageHead=result->next;
        }
	if (gc->numPagesUsed==(intptr_t)gc->gcTriggerPages) {
	    fivmr_GC_asyncCollect(gc);
            asyncRequested=true;
	}
	gc->numPagesUsed++;
        fivmr_assert(gc->numPagesUsed<=(intptr_t)gc->maxPagesUsed);
	gc->numFreePages--;
        fivmr_assert(gc->numPagesUsed>=0);
        fivmr_assert(gc->numFreePages>=0);
    } else {
	LOG(1,("Out of memory on small object request: pages used = %p, max pages = %p",
	       gc->numPagesUsed,gc->maxPagesUsed));
    }

    for (space=0;space<FIVMR_GC_NUM_GC_SPACES;++space) {
	fivmr_assert(fivmr_PageTable_get(&gc->spaceData[space].pt,
					 (uintptr_t)result)
		     ==FIVMR_GCPS_ZERO);
    }
    
    if (result!=NULL && result->dirty) {
	bzero(result,FIVMR_PAGE_SIZE);
	fivmr_fence();
    }
    
    if (asyncRequested) {
        LOG(1,("Back in Java code after asynchronous collection request, returning page %p.",
               result));
    }
    LOG(3,("getPage returning %p.",result));
   
    gc->getPageTime+=fivmr_debugTime()-before;

done: 
    return result;
}

static uintptr_t zeroFreeLines(fivmr_FreeLine *head,
                               fivmr_GC *gc) {
    fivmr_FreeLine *cur;
    uintptr_t cnt=0;
    for (cur=head;cur!=NULL;) {
	fivmr_FreeLine *next;
	fivmr_UsedPage *up;
	next=cur->next;
	up=(fivmr_UsedPage*)(((uintptr_t)cur)&~(FIVMR_PAGE_SIZE-1));
	*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings)=FIVMR_GCUPS_ZERO;
	cnt+=cur->size;
	fivmr_FreeLine_zero(cur);
	cur=next;
    }
    return cnt;
}

static void removeFreeLinesInPage(fivmr_GC *gc,
                                  fivmr_UsedPage *up) {
    if ((*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings))
        & FIVMR_GCUPS_FREE_LINES) {
	LOG(3,("Page %p has status %p",
               up,*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings)));
	fivmr_Lock_lock(&gc->gcLock);
	/* recheck the status; it may have changed. */
	if ((*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings))
            & FIVMR_GCUPS_FREE_LINES) {
	    fivmr_FreeLine *first;
	    fivmr_FreeLine *last;
            fivmr_FreeLine *cur;
            uintptr_t totalSize=0;
	    first=(fivmr_FreeLine*)(
                (uintptr_t)up+
                ((*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings))
                 & ~FIVMR_GCUPS_STAT_MASK));
	    last=first->lastOnPage;
            for (cur=first;;cur=cur->next) {
                fivmr_GCSpace i;
                totalSize+=cur->size;
                if (cur==last) break; /* ok! */
                fivmr_assert(cur!=NULL);
                for (i=0;i<FIVMR_GC_NUM_GC_SPACES;++i) {
                    fivmr_assert(cur!=&gc->spaceData[i].freeLineTail);
                }
                fivmr_assert((((uintptr_t)cur)&~(FIVMR_PAGE_SIZE-1))==(uintptr_t)up);
            }
	    first->prev->next=last->next;
	    last->next->prev=first->prev;
            first->prev=NULL;
	    last->next=NULL;
	    *fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings)=FIVMR_GCUPS_ZERO;
	}
	fivmr_Lock_unlock(&gc->gcLock);
    }
}

static inline bool canSatisfy(fivmr_Settings *settings,
                              fivmr_FreeLine *line,
                              uintptr_t size,
                              uintptr_t alignStart,
                              uintptr_t align) {
    if (size<=line->size) {
        uintptr_t allocStart=(uintptr_t)line+FIVMR_ALLOC_OFFSET(settings);
        uintptr_t allocSize=line->size;
        return (fivmr_alignRaw(allocStart+alignStart,align)
                -alignStart+size)-allocStart <= allocSize;
    } else {
        return false;
    }
}

static inline bool anyOnPageCanSatisfy(fivmr_Settings *settings,
                                       fivmr_FreeLine *head,
                                       uintptr_t size,
                                       uintptr_t alignStart,
                                       uintptr_t align) {
    if (FIVMR_HFGC(settings)) {
        fivmr_assert(size==FIVMR_GC_BLOCK_SIZE);
        fivmr_assert(canSatisfy(settings,head,size,alignStart,align));
        return true;
    } else {
        fivmr_FreeLine *cur;
        for (cur=head;;cur=cur->next) {
            if (canSatisfy(settings,cur,size,alignStart,align)) {
                return true;
            }
            
            if (cur==cur->lastOnPage) {
                break;
            }
        }
        return false;
    }
}

static bool getPageOfFreeLines(fivmr_GC *gc,
                               fivmr_GCSpace space,
			       fivmr_UsedPage **upPtr,
			       fivmr_FreeLine **headPtr,
			       fivmr_FreeLine **tailPtr,
                               uintptr_t size,
                               uintptr_t alignStart,
                               uintptr_t align) {
    bool result=false;
    fivmr_Nanos before=fivmr_debugTime();
    for (;;) {
	fivmr_FreeLine *head;
        int cnt=0;
        head=gc->spaceData[space].freeLineHead.next;
        for (;;) {
            if (head!=&gc->spaceData[space].freeLineTail) {
                fivmr_UsedPage *up;
                fivmr_FreeLine *tail;
                tail=head->lastOnPage;
                fivmr_assert((((uintptr_t)tail)&~(FIVMR_PAGE_SIZE-1))==
                             (((uintptr_t)head)&~(FIVMR_PAGE_SIZE-1)));
                fivmr_assert((((uintptr_t)tail->next)&~(FIVMR_PAGE_SIZE-1))!=
                             (((uintptr_t)tail)&~(FIVMR_PAGE_SIZE-1)));
                if (anyOnPageCanSatisfy(&fivmr_VMfromGC(gc)->settings,
                                        head,size,alignStart,align)) {
                    up=(fivmr_UsedPage*)(((uintptr_t)head)&~(FIVMR_PAGE_SIZE-1));
                    removeFreeLinesInPage(gc,up);
                    for (;;) {
                        uint8_t pageState;
                        pageState=fivmr_PageTable_get(&gc->spaceData[space].pt,
                                                      (uintptr_t)up);
                        if (pageState==FIVMR_GCPS_ZERO) {
                            /* race of some kind?  the interaction between this an reusePage()
                               is bizarre as heck. */
                            LOG(2,("Thread %u skipping page %p because the sweep has freed it.",
                                   fivmr_ThreadState_get(fivmr_VMfromGC(gc))->id,up));
                            break;
                        }
                        /* note: page should not be in RELINQUISHED state, since that
                           would mean that the page cannot have free lines */
                        fivmr_assert(pageState==FIVMR_GCPS_POPULATED ||
                                     pageState==FIVMR_GCPS_SHADED);
                        fivmr_assert(result==false);
                        if (fivmr_PageTable_cas(&gc->spaceData[space].pt,
                                                (uintptr_t)up,
                                                pageState,
                                                FIVMR_GCPS_ZERO)) {
                            result=true;
                            break;
                        }
                    }
                    if (result) {
                        *upPtr=up;
                        *headPtr=head;
                        *tailPtr=tail;
                        LOG(4,("Getting page %p for line allocation",up));
                        goto done;
                    } else {
                        break;
                    }
                } else {
                    /* FIXME: CMR should use a max-heap to store pages of free lines. */
                    cnt++;
                    if (cnt>5) {
                        result=false;
                        goto done;
                    }
                    head=tail->next;
                }
            } else {
                result=false;
                goto done;
            }
        }
    }
done:
    gc->getFreeLinesTime+=fivmr_debugTime()-before;
    return result;
}

typedef struct {
    fivmr_FreePage *head;
    fivmr_FreePage *tail;
    uintptr_t numPages;
} BunchOfPages;

static void initBunchOfPages(BunchOfPages *bop) {
    bop->head=NULL;
    bop->tail=NULL;
    bop->numPages=0;
}

static void reuseBunchOfPages(fivmr_GC *gc,
                              BunchOfPages *bop) {
    if (bop->numPages) {
	fivmr_FreePage *cur;
	uintptr_t count=0;

	fivmr_assert(bop->head!=NULL);
	fivmr_assert(bop->tail!=NULL);
	fivmr_assert(bop->numPages!=1 || bop->head==bop->tail);
	
	fivmr_Lock_lock(&gc->gcLock);
        if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            for (cur=bop->head;cur!=NULL;cur=cur->next) {
                fivmr_assert((cur->next==NULL && cur==bop->tail) ||
                             cur->next->prev==cur);
                fivmr_assert((cur->prev==NULL && cur==bop->head) ||
                             cur->prev->next==cur);
                fivmr_PageTable_setAssert(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                          (uintptr_t)cur,
                                          FIVMR_GCPS_ZERO,
                                          FIVMR_GCPS_FREE);
                count++;
            }
            fivmr_assert(count==bop->numPages);
            fivmr_assert(bop->head->prev==NULL);
            fivmr_assert(bop->tail->next==NULL);
            bop->tail->next=gc->freePageHead;
            if (gc->freePageHead!=NULL) {
                gc->freePageHead->prev=bop->tail;
            }
            gc->freePageHead=bop->head;
        } else {
            bop->tail->next=gc->freePageHead;
            gc->freePageHead=bop->head;
        }
	gc->numFreePages+=bop->numPages;
	gc->numPagesUsed-=bop->numPages;
        fivmr_assert(gc->numPagesUsed>=0);
        fivmr_assert(gc->numFreePages>=0);
	fivmr_Lock_unlock(&gc->gcLock);
	
	initBunchOfPages(bop);
    }
}

static void reusePage(fivmr_GC *gc,
                      BunchOfPages *bop,void *page_) {
    fivmr_UsedPage *usedPage=(fivmr_UsedPage*)page_;
    fivmr_FreePage *page=(fivmr_FreePage*)page_;
    
    removeFreeLinesInPage(gc,usedPage);
    
    page->dirty=true;

    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        page->next=bop->head;
        page->prev=NULL;
        if (bop->head!=NULL) {
            bop->head->prev=page;
        }
        bop->head=page;
    } else {
        page->next=bop->head;
        bop->head=page;
    }
    if (bop->tail==NULL) {
	bop->tail=page;
    }
    bop->numPages++;
    
    if (bop->numPages>gc->reusePages) {
	reuseBunchOfPages(gc,bop);
    }
}

/* helper for OOME logging. */
static void logOOME(fivmr_GC *gc,
                    const char *where,const char *description,
                    uintptr_t bytes,uintptr_t align) {
    if (gc->logGC) {
        fivmr_ThreadState *ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
        fivmr_Log_lockedPrintf("[OOME in %s for Thread %u, allocating %s: %" PRIuPTR " bytes with "
                               "%" PRIuPTR "-byte alignment]\n",
                               where,
                               ts->id,
                               description,
                               bytes,align);
        fivmr_ThreadState_dumpStackFor(ts);
    }
}

/* call only while holding the lock.  does not put the header into the list. 
   note that the allocated space is dirty if we're self-managing and zeroed
   otherwise. */
static fivmr_LargeObjectHeader *attemptAllocLargeObject(fivmr_GC *gc,
                                                        uintptr_t size,
                                                        fivmr_AllocEffort effort) {
    uintptr_t numPages;
    uintptr_t result;
    uintptr_t curPage;
    uintptr_t sizeInPages;
    
    numPages=fivmr_pages(size);
    
    if (gc->numPagesUsed+numPages > gc->maxPagesUsed) {
	LOG(1,("Out of memory on large object request: pages used = %p, "
	       "max pages = %p, pages requrested = %p",
	       gc->numPagesUsed,gc->maxPagesUsed,numPages));
    	return NULL;
    }

    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        sizeInPages=numPages*FIVMR_PAGE_SIZE;
        if (gc->nextFreePage+sizeInPages<=gc->memEnd) {
            result=gc->nextFreePage;
            fivmr_assert(
                fivmr_PageTable_get(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,result)
                ==FIVMR_GCPS_ZERO);
            gc->nextFreePage=result+sizeInPages;
            gc->numFreePages+=numPages; /* hack! */
            return (fivmr_LargeObjectHeader*)result;
        } else {
            /* this is horrifically inefficient.  that's ok for now. */
            for (curPage=gc->memStart;curPage<gc->memEnd;curPage+=FIVMR_PAGE_SIZE) {
                if (fivmr_PageTable_get(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,curPage)
                    ==FIVMR_GCPS_FREE) {
                    uintptr_t curPage2;
                    bool allGood=true;
                    LOG(5,("Attempting to reserve %p pages starting at %p",
                           numPages,curPage));
                    for (curPage2=curPage+FIVMR_PAGE_SIZE;
                         curPage2<curPage+(numPages<<FIVMSYS_LOG_PAGE_SIZE);
                         curPage2+=FIVMR_PAGE_SIZE) {
                        if (curPage2>=gc->memEnd ||
                            fivmr_PageTable_get(
                                &gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                curPage2)
                            !=FIVMR_GCPS_FREE) {
                            curPage=curPage2;
                            allGood=false;
                            break;
                        }
                    }
                    if (allGood) {
                        fivmr_assert(gc->numPagesUsed+numPages <= gc->maxPagesUsed);
                        for (curPage2=curPage;
                             curPage2<curPage+(numPages<<FIVMSYS_LOG_PAGE_SIZE);
                             curPage2+=FIVMR_PAGE_SIZE) {
                            fivmr_FreePage *fp=(fivmr_FreePage*)curPage2;
                            LOG(5,("Grabbing page %p",curPage2));
                            fivmr_PageTable_setAssert(
                                &gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                curPage2,
                                FIVMR_GCPS_FREE,
                                FIVMR_GCPS_ZERO);
                            if (fp->prev==NULL) {
                                gc->freePageHead=fp->next;
                            } else {
                                fp->prev->next=fp->next;
                            }
                            if (fp->next!=NULL) {
                                fp->next->prev=fp->prev;
                            }
                        }
                        return (fivmr_LargeObjectHeader*)curPage;
                    }
                }
            }
        }
        LOG(2,("Out of memory on large object request: pages used = %p, "
               "max pages = %p, pages requrested = %p",
               gc->numPagesUsed,gc->maxPagesUsed,numPages));
        return NULL;
    } else {
        if (effort==FIVMR_AE_CAN_FAIL) {
            return NULL;
        }
        
        result=(uintptr_t)fivmr_malloc(size);
        if (result==0) {
            return NULL;
        }
        fivmr_assert(fivmr_align(result,8)==result);
        return (fivmr_LargeObjectHeader*)result;
    }
}

static void zeroLargeObject(fivmr_LargeObjectHeader *hdr,
			    uintptr_t size) {
    /* FIXME we could be more careful here and omit zeroing sometimes ... but it
       doesn't seem to be worth the effort. */
    bzero(hdr,size);
}

static fivmr_Object allocLargeObject(fivmr_GC *gc,
                                     uintptr_t size,
				     uintptr_t alignStart,
				     uintptr_t align,
                                     fivmr_AllocEffort effort,
                                     const char *description) {
    fivmr_ThreadState *ts;
    uintptr_t offset;
    uintptr_t fullSize;
    fivmr_LargeObjectHeader *hdr;
    uintptr_t result;
    uintptr_t numPages;
    int tries;
    fivmr_Settings *settings;
    fivmr_Nanos before=fivmr_debugTime();
    
    settings=&fivmr_VMfromGC(gc)->settings;
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    fivmr_ThreadState_goToNative(ts);

    offset=
        sizeof(fivmr_LargeObjectHeader)+
        FIVMR_ALLOC_OFFSET(settings);
    offset=fivmr_align(offset+alignStart,align)-alignStart;
    offset=fivmr_align(offset-FIVMR_ALLOC_OFFSET(settings),
                       FIVMR_MIN_OBJ_ALIGN(settings)) + FIVMR_ALLOC_OFFSET(settings);
    fullSize=offset+size-FIVMR_ALLOC_OFFSET(settings);
    numPages=fivmr_pages(fullSize);
    
    if (FIVMR_ASSERTS_ON && offset-FIVMR_ALLOC_OFFSET(settings)+size>fullSize) {
        LOG(0,("offset = %p, size = %p, fullsize = %p",
               offset,size,fullSize));
        fivmr_assert(offset-FIVMR_ALLOC_OFFSET(settings)+size<=fullSize);
    }

    if (FIVMR_PREDICTABLE_OOME(&fivmr_VMfromGC(gc)->settings) &&
        !fivmr_Thread_isCritical()) {
        fivmr_Lock_lock(&gc->requestLock);
    }
    fivmr_Lock_lock(&gc->gcLock);
    
    for (tries=0;;++tries) {
        hdr=attemptAllocLargeObject(gc,fullSize,effort);
        LOG(3,("Allocated large object at %p with size=%p, alignStart=%p, align=%p, "
               "offset=%p, fullSize=%p, ending at %p",
               hdr,size,alignStart,align,offset,fullSize,(uintptr_t)hdr+fullSize));
        if (hdr!=NULL || effort==FIVMR_AE_CAN_FAIL || tries==3) {
            break;
        }
        fivmr_Lock_unlock(&gc->gcLock);
        collectInvoluntarilyFromNative(gc,"LOS",description);
        fivmr_Lock_lock(&gc->gcLock);
    }
    if (hdr!=NULL) {
	if (gc->numPagesUsed <= (intptr_t)gc->gcTriggerPages &&
	    gc->numPagesUsed+(intptr_t)numPages > (intptr_t)gc->gcTriggerPages) {
	    fivmr_GC_asyncCollect(gc);
	}
	gc->numPagesUsed+=numPages;
        fivmr_assert(gc->numPagesUsed <= (intptr_t)gc->maxPagesUsed);
        if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            gc->numFreePages-=numPages;
        }
        fivmr_assert(gc->numPagesUsed>=0);
        fivmr_assert(gc->numFreePages>=0);
    }
    
    fivmr_Lock_unlock(&gc->gcLock);
    if (FIVMR_PREDICTABLE_OOME(&fivmr_VMfromGC(gc)->settings) &&
        !fivmr_Thread_isCritical()) {
	fivmr_Lock_unlock(&gc->requestLock);
    }
    
    if (hdr==NULL) {
	result=0;
	fivmr_ThreadState_goToJava(ts);
    } else {
	zeroLargeObject(hdr,fullSize);
	
	hdr->fullSize=fullSize;
	result=((uintptr_t)hdr)+offset;

        hdr->object=fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                              result);
	
	LOG(4,("Allocated large object with loh = %p, at %p with header at %p",
	       hdr,result,
               fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                         result)));
	
	fivmr_ThreadState_goToJava(ts);

	fivmr_stampGCBits(gc,FIVMR_GC_OBJ_SPACE,NULL,result);
	
	fivmr_Lock_lock(&gc->gcLock);
	hdr->next = gc->largeObjectHead;
	gc->largeObjectHead = hdr;
	fivmr_Lock_unlock(&gc->gcLock);
    }
    
    gc->largeAllocTime+=fivmr_debugTime()-before;
    
    return result;
}

static void freeLargeObject(fivmr_GC *gc,
                            fivmr_LargeObjectHeader *hdr) {
    uintptr_t numPages;
    uintptr_t curPage;

    numPages=fivmr_pages(hdr->fullSize);
    LOG(3,("Freeing %" PRIuPTR " pages of large object %p",
	   numPages,hdr));
    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        /* FIXME: this could be improved - say by releasing the pages in smaller chunks */
        fivmr_Lock_lock(&gc->gcLock);
        for (curPage=(uintptr_t)hdr;
             curPage<((uintptr_t)hdr)+(numPages<<FIVMSYS_LOG_PAGE_SIZE);
             curPage+=FIVMR_PAGE_SIZE) {
            LOG(5,("Returning page %p",curPage));
            ((fivmr_FreePage*)curPage)->dirty=true;
            addPageToFreelist(gc,curPage);
        }
        gc->numPagesUsed -= numPages;
        gc->numFreePages += numPages;
        fivmr_assert(gc->numPagesUsed >= 0);
        fivmr_assert(gc->numFreePages >= 0);
        fivmr_Lock_unlock(&gc->gcLock);
    } else {
        free(hdr);
        fivmr_Lock_lock(&gc->gcLock);
        gc->numPagesUsed-=numPages;
        fivmr_assert(gc->numPagesUsed>=0);
        fivmr_assert(gc->numFreePages>=0);
        fivmr_Lock_unlock(&gc->gcLock);
    }
}

static void relinquishSpacesContext(fivmr_ThreadState *ts) {
    fivmr_GCSpaceAlloc *alloc=ts->gc.alloc+FIVMR_GC_OBJ_SPACE;

    /* FIXME: we can make this better by only relinquishing if we know
       that this mutator is still using the old space. */
    
    alloc->ssBump=0;
    alloc->ssEnd=0;
    alloc->ssSize=0;
}

static void relinquishAllocationContextForPage(fivmr_ThreadState *ts,
                                               fivmr_GCSpace space,
                                               uintptr_t ptr) {
    uintptr_t page;
    fivmr_UsedPage *up;
    fivmr_GC *gc;
    fivmr_GCSpaceAlloc *alloc;
    
    gc=&ts->vm->gc;
    alloc=ts->gc.alloc+space;

    LOG(4,("Relinquishing allocation context with bump = %p",
           alloc->bump));
    
    page=ptr&~(FIVMR_PAGE_SIZE-1);
    up=(fivmr_UsedPage*)page;
    *fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings)=FIVMR_GCUPS_ZERO;
    zeroFreeLines(alloc->freeHead,gc);

    /* mark the old page as populated or shaded depending on GC
       phase. */
    if (!fivmr_GC_isCollecting(gc->phase)) {
        LOG(3,("Thread %u (from %p) relinquishing page %p as populated, %s",
               ts->id,fivmr_ThreadHandle_current(),page,
               alloc->usedPage?"when line allocating":"when bump allocating"));
        fivmr_PageTable_setAssert(&gc->spaceData[space].pt,
                                  page,
                                  FIVMR_GCPS_ZERO,
                                  FIVMR_GCPS_POPULATED);
    } else {
        /* GC is not yet done - mark page as relinquished to prevent it from
           getting into the sweep. */
        LOG(3,("Thread %u (from %p) relinquishing page %p as relinquished, %s",
               ts->id,fivmr_ThreadHandle_current(),page,
               alloc->usedPage?"when line allocating":"when bump allocating"));
        fivmr_PageTable_setAssert(&gc->spaceData[space].pt,
                                  page,
                                  FIVMR_GCPS_ZERO,
                                  FIVMR_GCPS_RELINQUISHED);
    }
    
    alloc->bump=0;
    alloc->start=0;
    alloc->size=0;
    alloc->freeHead=NULL;
    alloc->freeTail=NULL;
    alloc->usedPage=NULL;
}

static void relinquishAllocationContext(fivmr_ThreadState *ts,
					fivmr_GCSpace space) {
    fivmr_GC *gc;
    fivmr_GCSpaceAlloc *alloc;
    fivmr_MemoryArea *curarea;
    
    gc=&ts->vm->gc;
    alloc=ts->gc.alloc+space;
    curarea=NULL;
    
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&space==0&&
        ts->gc.currentArea!=ts->gc.baseStackEntry.area) {
        curarea=ts->gc.currentArea;
        fivmr_MemoryArea_setCurrentArea(ts, ts->gc.baseStackEntry.area);
    }
    if (alloc->bump) {
        relinquishAllocationContextForPage(ts,space,alloc->start);
    } else {
	fivmr_assert(alloc->freeHead==NULL);
	fivmr_assert(alloc->freeTail==NULL);
	fivmr_assert(alloc->usedPage==NULL);
    }
    if (curarea) {
        fivmr_MemoryArea_setCurrentArea(ts, curarea);
    }
}

static void relinquishAllocationContexts(fivmr_ThreadState *ts) {
    fivmr_GCSpace i;
    fivmr_MemoryArea *curarea=NULL;
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&
        ts->gc.currentArea!=ts->gc.baseStackEntry.area) {
        curarea=ts->gc.currentArea;
        fivmr_MemoryArea_setCurrentArea(ts, ts->gc.baseStackEntry.area);
    }
    if (FIVMR_HFGC(&ts->vm->settings)) {
        relinquishSpacesContext(ts);
    }
    for (i=0;i<FIVMR_GC_NUM_GC_SPACES;++i) {
	relinquishAllocationContext(ts,i);
    }
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&curarea) {
        fivmr_MemoryArea_setCurrentArea(ts, curarea);
    }
}

static void initSpaces(fivmr_GC *gc) {
    uintptr_t semispaceSize;
    
    bzero(&gc->ss,sizeof(gc->ss));
    
    /* need to compute the max spine space usage for the given
       heap size.  if you change any of the memory model constants,
       this computation must change as well. */
    
    semispaceSize=(3*gc->maxPagesUsed+9)/10;
    
    LOG(1,("Need %" PRIuPTR " pages for both semi-spaces.",
           semispaceSize));
    
    /* FIXME: we may want to change the zeroing policy for the semispace... */
    gc->ss.start=(uintptr_t)fivmr_allocPages(semispaceSize,NULL);
    gc->ss.size=semispaceSize<<FIVMSYS_LOG_PAGE_SIZE;
    
    gc->ss.allocStart = gc->ss.start+FIVMR_FRAG_SP_FR_OFFSET;
    gc->ss.allocEnd = gc->ss.allocStart+gc->ss.size/2;
    
    gc->ss.muAlloc = gc->ss.allocEnd;
    gc->ss.muLimit = gc->ss.allocStart;
    
    gc->ss.gcAlloc = gc->ss.muLimit;
    
    LOG(1,("Semi-space policy initialized over memory range %" PRIuPTR
           " to %" PRIuPTR ".",
           gc->ss.start,gc->ss.start+gc->ss.size-1));
    
    LOG(2,("Allocator will allocate between %" PRIuPTR " and %" PRIuPTR ".",
           gc->ss.allocStart,gc->ss.allocEnd));
}

static void freeSpaces(fivmr_GC *gc) {
    fivmr_freePages((void*)gc->ss.start,gc->ss.size>>FIVMSYS_LOG_PAGE_SIZE);
}

/* NOTE: we should flip spaces before the mutator starts allocating black...
   otherwise we'll have black sentinels in the old space that point to
   spines in the old space; these spines will then never get copied. */
static void flipSpaces(fivmr_GC *gc) {
    uintptr_t mult;
    uintptr_t spaceLeft;
    
    LOG(2,("Performing semi-space flip."));
    
    fivmr_Lock_lock(&gc->gcLock);

    /* let the sweeper know what regions to deal with. */
    
    fivmr_assert(gc->ss.muLimit == gc->ss.gcAlloc);
    
    gc->ss.muRegionStart = gc->ss.muAlloc-FIVMR_FRAG_SP_FR_OFFSET;
    gc->ss.muRegionSize = gc->ss.allocEnd-gc->ss.muAlloc;
    
    gc->ss.gcRegionStart = gc->ss.allocStart-FIVMR_FRAG_SP_FR_OFFSET;
    gc->ss.gcRegionSize = gc->ss.gcAlloc-gc->ss.allocStart;
    
    /* compute how much space is left */
    
    spaceLeft = gc->ss.size/2 - gc->ss.muRegionSize - gc->ss.gcRegionSize;
    
    LOG(2,("Mutator will have %" PRIuPTR " bytes to semi-space allocate until "
           "the GC is done allocating.",spaceLeft));
    
    /* perform the flip. */

    fivmr_assert(((gc->ss.size/2)&(sizeof(uintptr_t)-1)) == 0);
    fivmr_assert(gc->ss.allocStart == gc->ss.start+FIVMR_FRAG_SP_FR_OFFSET ||
                 gc->ss.allocStart == gc->ss.start+gc->ss.size/2+FIVMR_FRAG_SP_FR_OFFSET);
    
    if (gc->ss.allocStart == gc->ss.start+FIVMR_FRAG_SP_FR_OFFSET) {
        mult=1;
    } else {
        mult=0;
    }
    
    LOG(2,("Flipping to space %d.",
           (int)(mult+1)));
    
    gc->ss.allocStart = gc->ss.start+mult*gc->ss.size/2+FIVMR_FRAG_SP_FR_OFFSET;
    gc->ss.allocEnd = gc->ss.allocStart+gc->ss.size/2;
    
    gc->ss.muAlloc = gc->ss.allocEnd;
    gc->ss.gcAlloc = gc->ss.allocStart;
    
    gc->ss.muLimit = gc->ss.muAlloc-spaceLeft;
    
    LOG(2,("Allocator will allocate between %p and %p"
           ", mutator will allocate from %p"
           ", collector will allocate from %p"
           ", with the mutator limit at %p, and the total "
           "semispace area is at %p with a size of %" PRIuPTR ".",
           gc->ss.allocStart,gc->ss.allocEnd,
           gc->ss.muAlloc, gc->ss.gcAlloc, gc->ss.muLimit, gc->ss.start,
           gc->ss.size));
    
    /* finally, ensure that we don't have any objects queued for spine
       copying... */
    fivmr_assert(gc->ss.head==NULL);

    fivmr_Lock_unlock(&gc->gcLock);
}

static void doneGCSpaceAlloc(fivmr_GC *gc) {
    LOG(2,("Signaling that the GC is done semi-space allocating."));

    fivmr_Lock_lock(&gc->gcLock);
    gc->ss.muLimit = gc->ss.gcAlloc;
    gc->ss.first = true;
    fivmr_Lock_unlock(&gc->gcLock);
}

static void sweepOldSpace(fivmr_GC *gc) {
    LOG(2,("Sweeping old semi-space."));

    if (shouldQuit(gc)) return;
    bzero((void*)gc->ss.muRegionStart,gc->ss.muRegionSize);
    if (shouldQuit(gc)) return;
    bzero((void*)gc->ss.gcRegionStart,gc->ss.gcRegionSize);
}

static uintptr_t attemptAllocSSExtent(fivmr_ThreadState *ts,
                                      uintptr_t size) {
    uintptr_t result;
    fivmr_GC *gc;
    
    gc=&ts->vm->gc;
    
    fivmr_Lock_lock(&gc->gcLock);

    result = gc->ss.muAlloc-size;
    if (gc->ss.allocEnd - result > gc->ss.allocEnd - gc->ss.muLimit) {
        result = 0; /* failure */
    } else {
        gc->ss.muAlloc = result;
    }

    fivmr_Lock_unlock(&gc->gcLock);
    
    return result;
}

static uintptr_t allocSSExtent(fivmr_ThreadState *ts,
                               uintptr_t size,
                               const char *description) {
    fivmr_GC *gc;
    uintptr_t result;
    int iterationCountdown;
    
    gc=&ts->vm->gc;
    
    result=0; /* make GCC happy */
    iterationCountdown=3;
    
    for (;;) {
        bool firstToAllocate;
        
        fivmr_Lock_lock(&gc->gcLock);
        if (iterationCountdown) {
            firstToAllocate=false;
            iterationCountdown--;
        } else {
            firstToAllocate=gc->ss.first;
            gc->ss.first=false;
        }
        result=attemptAllocSSExtent(ts,size);
        fivmr_Lock_unlock(&gc->gcLock);
        
        if (firstToAllocate && !result) {
            break;
        }
        
        if (result) {
            break;
        }
        
        fivmr_ThreadState_goToNative(ts);
        collectInvoluntarilyFromNative(gc,"SS",description);
        fivmr_ThreadState_goToJava(ts);
    }
    
    return result;
}

fivmr_Spine FIVMR_CONCAT(FIVMBUILD_GC_NAME,_allocSSSlow)(fivmr_ThreadState *ts,
                                                         uintptr_t spineLength,
                                                         int32_t numEle,
                                                         const char *description) {
    uintptr_t result;
    uintptr_t size;
    fivmr_GCSpaceAlloc *alloc;
    fivmr_GC *gc;

    gc=&ts->vm->gc;
    alloc=ts->gc.alloc+FIVMR_GC_OBJ_SPACE;
    size=fivmr_Spine_calcSize(spineLength);
    
    if (size>FIVMR_LARGE_ARRAY_THRESHOLD) {
        result=allocSSExtent(ts,size,description);
    } else {
        uintptr_t bottom=
            attemptAllocSSExtent(ts,FIVMR_ARRAY_THREAD_CACHE_SIZE);
        if (bottom) {
            alloc->ssBump=alloc->ssEnd=
                bottom+FIVMR_ARRAY_THREAD_CACHE_SIZE;
            alloc->ssSize=FIVMR_ARRAY_THREAD_CACHE_SIZE;

            result=alloc->ssBump-size;
            fivmr_assert(alloc->ssEnd - result <= alloc->ssSize);
            alloc->ssBump=result;
        } else {
            result=allocSSExtent(ts,size,description);
        }
    }
    
    if (!result) {
        logOOME(gc,"SS",description,size,sizeof(uintptr_t));
        fivmr_throwOutOfMemoryError_inJava(ts);
    }
    
    return result;
}

static inline bool isInToSpace(fivmr_GC *gc,
                               fivmr_Spine spine) {
    return spine>=gc->ss.allocStart && spine<gc->ss.allocEnd;
}

static inline void copySpine(fivmr_GC *gc,
                             fivmr_Object obj) {
    fivmr_Spine old;
    fivmr_Spine new;
    uintptr_t spineLength;
    uintptr_t spineSize;
    uintptr_t i;
    
    spineLength=fivmr_Object_getSpineLength(&fivmr_VMfromGC(gc)->settings,obj);
    spineSize=fivmr_Spine_calcSize(spineLength);
    
    old=fivmr_Spine_forObject(obj);
    new=fivmr_Spine_getForward(old);
    
    /* use careful pointer-loop copy to ensure happiness. */
    for (i=0;i<spineLength;++i) {
        uintptr_t ptr=((uintptr_t*)old)[i];
        if (ptr!=0) {
            ((uintptr_t*)new)[i]=ptr;
        }
        if (shouldQuit(gc)) return;
    }
    
    fivmr_Object_setSpine(obj,new);
}

uintptr_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_allocRawSlow)(fivmr_ThreadState *ts,
                                                        fivmr_GCSpace space,
                                                        uintptr_t size,
                                                        uintptr_t alignStart,
                                                        uintptr_t align,
                                                        fivmr_AllocEffort effort,
                                                        const char *description) {
    uintptr_t result=0;
    fivmr_GCSpaceAlloc *alloc;
    uintptr_t alignedSize;
    fivmr_GC *gc;
    fivmr_Settings *settings;
    fivmr_Nanos before;
    
    fivmr_assert(fivmr_ThreadPriority_leRT(fivmr_Thread_getPriority(fivmr_ThreadHandle_current()),
                                           ts->vm->maxPriority));
    
    before=fivmr_debugTime();
    
    gc=&ts->vm->gc;
    settings=&fivmr_VMfromGC(gc)->settings;
    
    fivmr_assert(ts->execStatus!=FIVMR_TSES_CLEAR);
    fivmr_assert(ts->execStatus!=FIVMR_TSES_NEW);
    fivmr_assert(ts->execStatus!=FIVMR_TSES_STARTING);
    fivmr_assert(ts->execStatus!=FIVMR_TSES_TERMINATING);
    
    LOG(11,("fivmr_allocRaw_slow(%p (%u), %p, %p, %s) called",
            ts,ts->id,size,align,description));
    
    if (FIVMR_SCOPED_MEMORY(settings)&&space==FIVMR_GC_OBJ_SPACE
        &&fivmr_MemoryArea_inScope(ts)) {
        fivmr_throwOutOfMemoryError_inJava(ts);
    }

    alloc=ts->gc.alloc+space;

    size=fivmr_alignRaw(size,FIVMR_OBJ_SIZE_ALIGN(settings));
    
    if (FIVMR_HFGC_FAIL_FAST_PATHS(settings) && effort==FIVMR_AE_CAN_FAIL) {
        goto done;
    }
    
    /* figure out if the object will fit on a page */
    alignedSize=size+fivmr_align(alignStart,align)-alignStart;
    if (alignedSize>FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings)) {
        /* nope, not on a page ... allocate large object */
        result=allocLargeObject(gc,size,alignStart,align,effort,description);
        if (result==0 && effort==FIVMR_AE_MUST_SUCCEED) {
            logOOME(gc,"LOS",description,size,align);
            fivmr_throwOutOfMemoryError_inJava(ts);
        }
        goto done;
    } else {
        while ((fivmr_align(alloc->bump+alignStart,align)
                -alignStart+size)-alloc->start > alloc->size) {
            if (alloc->freeHead) {
                /* NOTE: we don't have to relinquish the page that we were bumping
                   on, because if ts->gc.freeHead is set then we weren't page
                   allocating. */
                
                fivmr_FreeLine *line=NULL;
                fivmr_Nanos before2=fivmr_debugTime();

                if (size==FIVMR_MIN_ALLOC_SIZE(settings) && FIVMR_HFGC(settings)) {
                    line=alloc->freeHead;
                    fivmr_assert(canSatisfy(settings,line,size,alignStart,align));
                } else {
                    fivmr_FreeLine *cur;
                    for (cur=alloc->freeHead;;cur=cur->next) {
                        if (false) { /* use first-fit */
                            if (canSatisfy(settings,
                                           cur,size,alignStart,align)) {
                                line=cur;
                                break;
                            }
                        } else { /* use best-fit */
                            if ((line==NULL || cur->size<line->size) &&
                                canSatisfy(settings,cur,
                                           size,alignStart,align)) {
                                line=cur;
                            }
                        }
                        if (cur==cur->lastOnPage) {
                            fivmr_assert(cur->next==NULL);
                            break;
                        }
                    }
                }
                
                gc->freeLineSearchTime+=fivmr_debugTime()-before2;
                
                /* printf("picked line %p in page %p\n",line,alloc->usedPage); */
                
                if (line==NULL) {
                    /* none of the lines on this page can be used for the current
                       allocation request - so drop the page and retry */
                    /* FIXME: we should really be putting this page back onto the
                       freeline list - preferably at the *end* of the list. */
                    relinquishAllocationContextForPage(ts,space,(uintptr_t)alloc->freeHead);
                } else {
                    if (line->prev!=NULL) {
                        line->prev->next=line->next;
                    } else {
                        alloc->freeHead=line->next;
                    }
                    if (line->next!=NULL) {
                        line->next->prev=line->prev;
                    } else {
                        alloc->freeTail=line->prev;
                    }
                    if (line==line->lastOnPage) {
                        fivmr_assert(line->next==NULL);
                        if (line->prev!=NULL) {
                            line->prev->lastOnPage=line->prev;
                        } else {
                            fivmr_assert(alloc->freeHead==NULL);
                            fivmr_assert(alloc->freeTail==NULL);
                        }
                    } else {
                        fivmr_assert(line->next!=NULL);
                    }
                
                    fivmr_assert((alloc->freeHead==NULL)==(alloc->freeTail==NULL));
                
                    fivmr_assert(((uintptr_t)line)+line->size <=
                                 (((uintptr_t)line)&~(FIVMR_PAGE_SIZE-1)) + FIVMR_PAGE_SIZE);
			 
                    alloc->bump=alloc->start=
                        ((uintptr_t)line)+FIVMR_ALLOC_OFFSET(settings);
                    alloc->size=line->size;

                    LOG(3,("Allocating in line %p (size %p, ends at %p)",
                           line,line->size,((uintptr_t)line)+line->size-1));
                    bzero(line,line->size);
                    fivmr_fence();
                    /* reloop to test if our allocation request fits */
                }
            } else {
                fivmr_UsedPage *up=NULL;
                fivmr_FreeLine *flHead=NULL;
                fivmr_FreeLine *flTail=NULL;
                uintptr_t page=0;
                int cnt=0;
                
                /* attempt to get a new allocation context */
                fivmr_ThreadState_goToNative(ts);
                
                if (FIVMR_PREDICTABLE_OOME(settings) &&
                    !fivmr_Thread_isCritical()) {
                    fivmr_Lock_lock(&gc->requestLock);
                }
                fivmr_Lock_lock(&gc->gcLock);
                
                switch (effort) {
                case FIVMR_AE_CAN_FAIL:
                    page=(uintptr_t)getPage(gc);
                    break;
                case FIVMR_AE_MUST_SUCCEED:
                    for (cnt=0;;++cnt) {
                        if (getPageOfFreeLines(gc,
                                               space,
                                               &up,&flHead,&flTail,
                                               size,alignStart,align) ||
                            (page=(uintptr_t)getPage(gc))) {
                            break;
                        }
                        if (cnt==3) {
                            break;
                        }
                        fivmr_Lock_unlock(&gc->gcLock);
                        collectInvoluntarilyFromNative(
                            gc,
                            fivmr_GCSpace_name(space),
                            description);
                        fivmr_Lock_lock(&gc->gcLock);
                    }
                    break;
                default: fivmr_abort("wrong value of effort");
                }
                
                fivmr_Lock_unlock(&gc->gcLock);
                if (FIVMR_PREDICTABLE_OOME(settings) &&
                    !fivmr_Thread_isCritical()) {
                    fivmr_Lock_unlock(&gc->requestLock);
                }

                fivmr_ThreadState_goToJava(ts);
                
                if (page!=0 || up!=NULL) {
                    relinquishAllocationContext(ts,space);
                }
                
                if (page!=0) {
                    alloc->bump=alloc->start=
                        page+FIVMR_PAGE_HEADER(settings)+FIVMR_ALLOC_OFFSET(settings);
                    alloc->size=FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings);
                } else if (up!=NULL) {
                    /* printf("allocating in freelines in %p: %p (%p), %p (%p); in request for %p\n",
                       up,flHead,(void*)flHead->size,flTail,(void*)flTail->size,(void*)size); */
                    alloc->usedPage=up;
                    alloc->freeHead=flHead;
                    alloc->freeTail=flTail;
                } else if (effort==FIVMR_AE_CAN_FAIL) {
                    result=0;
                    goto done;
                } else {
                    logOOME(gc,fivmr_GCSpace_name(space),description,size,align);
                    fivmr_throwOutOfMemoryError_inJava(ts);
                    result=0;
                    goto done;
                }
            }
        }
	
        LOG(10,("for space %d: bump = %p, start = %p, size = %p, align = %p",
                space,alloc->bump,alloc->start,alloc->size,align));
    
        result=
            fivmr_align(alloc->bump+alignStart,align)
            -alignStart;
        alloc->bump=result+size;
    
        LOG(9,("allocRaw_slow returning %p",result));
    
        goto done;
    }

done:
    
    gc->slowPathTime+=fivmr_debugTime()-before;
    
    return result;
}

static inline uint8_t shadePageAsNecessary(fivmr_GC *gc,
                                           fivmr_GCSpace space,
					   uintptr_t page) {
    for (;;) {
	uint8_t ptState=fivmr_PageTable_get(&gc->spaceData[space].pt,(uintptr_t)page);
	if (ptState==FIVMR_GCPS_POPULATED) {
	    if (fivmr_PageTable_cas(&gc->spaceData[space].pt,
				    (uintptr_t)page,
				    FIVMR_GCPS_POPULATED,
				    FIVMR_GCPS_SHADED)) {
		LOG(3,("Shaded page %p in space %d",page,space));
		return FIVMR_GCPS_POPULATED;
	    }
	} else {
            if (FIVMR_ASSERTS_ON &&
                FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
                fivmr_assert(ptState!=FIVMR_GCPS_FREE);
            }
	    if (ptState!=FIVMR_GCPS_SHADED) {
		LOG(5,("Page %p is in state %u",page,ptState));
	    }
	    return ptState;
	}
    }
}

static inline void shadeChunkAsNecessary(fivmr_GC *gc,
                                         fivmr_GCSpace space,
                                         uintptr_t base,
                                         uintptr_t size) {
    fivmr_UsedPage *up;
    uint8_t pageState;
    fivmr_Settings *settings;
    
    settings=&fivmr_VMfromGC(gc)->settings;
    
    fivmr_assert(fivmr_alignRaw(size,
                                FIVMR_OBJ_SIZE_ALIGN(settings))
                 == size);
    
    up=(fivmr_UsedPage*)(((uintptr_t)base)&~(FIVMR_PAGE_SIZE-1));
    pageState=shadePageAsNecessary(gc,space,(uintptr_t)up);
    switch (pageState) {
    case FIVMR_GCPS_POPULATED: {
        *fivmr_UsedPage_reserved(up,settings)=1;
        bzero(fivmr_UsedPage_bits(up,settings),
              FIVMR_UP_BITS_LENGTH(settings)*4);
        /* drop down to GCPS_SHADED... */
    }
    case FIVMR_GCPS_SHADED: {
        uintptr_t start;
        uintptr_t end;
        uintptr_t index;
                
        fivmr_assert(fivmr_alignRaw(base,FIVMR_MIN_OBJ_ALIGN(settings))==base);
        start=(base-(uintptr_t)up-FIVMR_PAGE_HEADER(settings))/FIVMR_MIN_CHUNK_ALIGN(settings);
        end=start+size/FIVMR_MIN_CHUNK_ALIGN(settings);
                
        for (index=start;index<end;++index) {
            fivmr_BitVec_set(fivmr_UsedPage_bits(up,settings),
                             index,true);
        }
        break;
    }
    default:
        break;
    }
}

static inline void shadeRawTypeAsNecessary(fivmr_GC *gc,
                                           uintptr_t rawType,
                                           uintptr_t size) {
    shadeChunkAsNecessary(
        gc,
        FIVMR_GC_OBJ_SPACE,
        rawType,
        fivmr_alignRaw(size,FIVMR_OBJ_SIZE_ALIGN(&fivmr_VMfromGC(gc)->settings)));
}

static void assertPerThreadQueuesSound(fivmr_GC *gc) {
    if (FIVMR_ASSERTS_ON) {
	LOG(2,("Requesting handshake to assert soundness of per-thread queues."));
	fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                        FIVMR_TSEF_JAVA_HANDSHAKEABLE,
					FIVMR_TSEF_GC_MISC);
	fivmr_assert(gc->globalQueue==NULL);
    }
}

static inline fivmr_TypeData *handleObjectHeader(fivmr_GC *gc,
                                                 fivmr_Object obj) {
    fivmr_TypeData *td;
    fivmr_Monitor *monitor;
    fivmr_Settings *settings;
    
    fivmr_assert(obj!=0);
    
    settings=&fivmr_VMfromGC(gc)->settings;
    
    monitor=fivmr_ObjHeader_getMonitor(settings,
                                       fivmr_ObjHeader_forObject(settings,
                                                                 obj));
    if (monitor->state==FIVMR_MS_INVALID) {
	td=(fivmr_TypeData*)monitor;
    } else {
	td=monitor->forward;

        /* Scoped object monitors et al. are not subject to collection */
        if (FIVMR_SCOPED_MEMORY(settings)) {
            fivmr_GCHeader *hdr=fivmr_GCHeader_fromObject(settings,obj);
            if (fivmr_GCHeader_isImmortal(hdr)||
                fivmr_GCHeader_isScopeID(hdr)) {
                return td;
            }
        }
	shadeRawTypeAsNecessary(gc,(uintptr_t)monitor,sizeof(fivmr_Monitor));
	if (monitor->queues!=NULL) {
	    /* FIXME: free up the queues if they're not in use. */
	    shadeRawTypeAsNecessary(gc,
                                    (uintptr_t)(monitor->queues),
                                    sizeof(fivmr_MonQueues));
	}
    }
    
    return td;
}

static void collectorWeakRefHandler(fivmr_VM *vm,
                                    fivmr_Object weakRef,
                                    void *arg) {
    /* FIXME do something */
}

static fivmr_ScanSpecialHandlers collectorSpecials={
    collectorWeakRefHandler
};

static void mutatorScanObject(fivmr_ThreadState *ts,
                              fivmr_Object obj) {
    
    fivmr_TypeData *td;
    fivmr_Settings *settings;
    
    td=handleObjectHeader(&ts->vm->gc,obj);
    
    fivmr_Object_scan(ts->vm,obj,td,mutatorMarkObjStackNoisyForScan,NULL,ts);
}

static inline void collectorScanObject(fivmr_GC *gc,
                                       fivmr_GCHeader **queue,
                                       fivmr_Object obj) {
    fivmr_TypeData *td;
    fivmr_Settings *settings;
    
    gc->anthracite=obj;
    
    td=handleObjectHeader(gc,obj);
    
    fivmr_Object_scan(fivmr_VMfromGC(gc),obj,td,collectorMarkObjForScan,&collectorSpecials,queue);
}

static void scanTypeAux(fivmr_GC *gc,
                        fivmr_GCHeader **queue,
                        fivmr_TypeAux *cur) {
    for (cur=fivmr_TypeAux_first(cur,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED);
         cur!=NULL;
         cur=fivmr_TypeAux_next(cur,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED)) {
        uintptr_t *ptr;
        uintptr_t i;
        
        if (shouldQuit(gc)) return;
        
        ptr=(uintptr_t*)fivmr_TypeAux_data(cur);
        
        for (i=0;i<cur->occupied;i+=sizeof(uintptr_t)) {
            collectorMarkObj(gc,queue,*ptr++);
        }
    }
}

typedef struct {
    fivmr_GC *gc;
    fivmr_GCHeader **queue;
} ScanTypeData;

static uintptr_t scanType_cback(fivmr_TypeData *td,
                                uintptr_t arg) {
    ScanTypeData *std=(ScanTypeData*)(void*)arg;
    int32_t i;
    
    if (shouldQuit(std->gc)) {
        return 1;
    }
    
    if ((td->flags&FIVMR_TBF_AOT)) {
        fivmr_assert(td->bytecode!=0);
        collectorScanObject(std->gc,std->queue,td->bytecode);
        fivmr_assert(td->classObject!=0);
        collectorScanObject(std->gc,std->queue,td->classObject);
    } else {
        collectorMarkObj(std->gc,std->queue,td->bytecode);
        collectorMarkObj(std->gc,std->queue,td->classObject);
    }
    
    return 0;
}

static void scanContext(fivmr_GC *gc,fivmr_GCHeader **queue,fivmr_TypeContext *ctx) {
    fivmr_VM *vm=fivmr_VMfromGC(gc);
    
    collectorMarkObj(gc,queue,ctx->classLoader);
    
    if (FIVMR_CLASSLOADING(&vm->settings)) {
        fivmr_TypeAux *cur;
        ScanTypeData std;
        
        fivmr_Lock_lock(&ctx->treeLock);
        
        for (cur=fivmr_TypeAux_first(ctx->aux,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED);
             cur!=NULL;
             cur=fivmr_TypeAux_next(cur,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED)) {
            uintptr_t *ptr;
            uintptr_t i;
            
            if (shouldQuit(gc)) break;
            
            ptr=(uintptr_t*)fivmr_TypeAux_data(cur);
            
            for (i=0;i<cur->occupied;i+=sizeof(uintptr_t)) {
                collectorMarkObj(gc,queue,*ptr++);
            }
        }
        
        std.gc=gc;
        std.queue=queue;
        fivmr_TypeContext_forAllTypes(ctx,scanType_cback,(uintptr_t)&std);
        
        fivmr_Lock_unlock(&ctx->treeLock);
    }
}

static void shadeObjectAsNecessary(fivmr_GC *gc,
                                   fivmr_GCHeader *hdr) {
    fivmr_Object obj;
    fivmr_Settings *settings;
    
    settings=&fivmr_VMfromGC(gc)->settings;
    
    obj=fivmr_GCHeader_toObject(settings,hdr);
    
    if (FIVMR_ASSERTS_ON && !FIVMR_HFGC(settings)) {
        fivmr_assert(fivmr_Object_isContiguous(settings,obj));
    }
    
    if (fivmr_Object_isContiguous(settings,obj)) {
        uintptr_t size=fivmr_Object_size(settings,obj);
        
        fivmr_assert(fivmr_alignRaw(size,
                                    FIVMR_MIN_OBJ_ALIGN(settings))
                     == size);
        
        shadeChunkAsNecessary(
            gc,
            FIVMR_GC_OBJ_SPACE,
            fivmr_GCHeader_chunkStart(FIVMR_GC_OBJ_SPACE,hdr),
            size);
    } else {
        fivmr_TypeData *td;
        
        fivmr_assert(FIVMR_HFGC(settings));

        td=fivmr_TypeData_forObject(settings,obj);
        if (fivmr_TypeData_isArray(td)) {
            uintptr_t spineLength;
            uintptr_t spineSize;
            uintptr_t spine;
            uintptr_t i;
            uintptr_t eleSize;
            uintptr_t offsetFromStart;
            
            shadeChunkAsNecessary(gc,
                                  FIVMR_GC_OBJ_SPACE,
                                  obj,FIVMR_GC_BLOCK_SIZE);
            
            spine=fivmr_Spine_forObject(obj);
            if (spine!=0) {
                spineLength=fivmr_Object_getSpineLength(settings,obj);
                spineSize=fivmr_Spine_calcSize(spineLength);
            
                if (spineLength>FIVMR_FRAG_MAX_INLINE_SPINE &&
                    !isInToSpace(gc,spine)) {
                    /* the object is large enough to have gotten its own spine...
                       allocate a new spine and mark it for copying, but don't
                       copy it yet. */
                
                    fivmr_Spine newSpine;
                
                    newSpine=gc->ss.gcAlloc;
                
                    fivmr_assert(newSpine-gc->ss.allocStart
                                 <= gc->ss.muLimit-gc->ss.allocStart);
                
                    gc->ss.gcAlloc=newSpine+spineSize;

                    fivmr_assert(gc->ss.gcAlloc-gc->ss.allocStart
                                 <= gc->ss.muLimit-gc->ss.allocStart);
                    
                    if (false) printf("new space spine at %p, from %p\n",
                                      (void*)newSpine,(void*)spine);
                
                    ((int32_t*)newSpine)[-1]=fivmr_arrayLength(NULL,obj,0);
                    fivmr_Spine_setForward(newSpine,newSpine);
                
                    fivmr_Spine_setForward(spine,newSpine);
                
                    fivmr_GCHeader_setNext(hdr,gc->ss.head);
                    gc->ss.head=hdr;
                }
            
                eleSize=fivmr_TypeData_elementSize(td);
                offsetFromStart=FIVMR_GC_BLOCK_SIZE-eleSize;
                for (i=0;i<spineLength;++i) {
                    uintptr_t cur=((uintptr_t*)spine)[i];
                    if (cur!=0) {
                        shadeChunkAsNecessary(gc,
                                              FIVMR_GC_OBJ_SPACE,
                                              cur-offsetFromStart,
                                              FIVMR_GC_BLOCK_SIZE);
                    }
                }
            }
            
        } else {
            uintptr_t size=
                (fivmr_TypeData_size(td)+FIVMR_GC_BLOCK_SIZE-1)
                &~(FIVMR_GC_BLOCK_SIZE-1);
            uintptr_t cur=obj;
            while (size>0) {
                shadeChunkAsNecessary(gc,
                                      FIVMR_GC_OBJ_SPACE,
                                      cur,FIVMR_GC_BLOCK_SIZE);
                cur=(*(uintptr_t*)cur);
                fivmr_assert((cur&1)==0);
                if (cur==0) {
                    break;
                }
                size-=FIVMR_GC_BLOCK_SIZE;
            }
        }
    }
}

static void transitiveClosure(fivmr_GC *gc,
                              fivmr_GCHeader **queue,
			      uintptr_t *objectsTraced) {
    LOG(2,("Tracing..."));
    do {
        gc->traceIterationCount++;
        
	/* attempt to process our local queue first, then consult the global
	   one. */
	
        /* FIXME: have a page-local queue that we put objects onto if they're
           on the same page as the other object we're currently on.  or
           something. */
        
	while ((*queue)!=NULL) {
	    fivmr_GCHeader *cur;
	    fivmr_Object obj;
	    
            if (shouldQuit(gc)) return;

	    (*objectsTraced)++;
	    
	    cur=*queue;
	    obj=fivmr_GCHeader_toObject(&fivmr_VMfromGC(gc)->settings,
                                        cur);
	    *queue=fivmr_GCHeader_next(cur);

            shadeObjectAsNecessary(gc,cur);
	    collectorScanObject(gc,queue,obj);
	}
	
	/* suck on the global queue. */
	
	fivmr_ThreadState_softHandshake(
            fivmr_VMfromGC(gc),
	    FIVMR_TSEF_JAVA_HANDSHAKEABLE,
	    ((FIVMR_GC_BLACK_STACK(&fivmr_VMfromGC(gc)->settings)
              ?0:FIVMR_TSEF_SCAN_THREAD_ROOTS)|
	     FIVMR_TSEF_GC_MISC));
	
	fivmr_Lock_lock(&gc->gcLock);
	*queue=gc->globalQueue;
	gc->globalQueue=NULL;
	fivmr_Lock_unlock(&gc->gcLock);
    } while ((*queue)!=NULL);
}

static bool processDestructors(fivmr_GC *gc,
                               fivmr_GCHeader **queue) {
    fivmr_Destructor **curPtr;
    fivmr_Destructor *cur;
    
    if (!FIVMR_FINALIZATION_SUPPORTED(&fivmr_VMfromGC(gc)->settings)) {
        return false;
    }
    
    LOG(2,("Processing destructors..."));
    
    fivmr_Lock_lock(&gc->destructorLock);
    
    /* figure out which destructors need to be run */
    /* FIXME: this loop is boned.  I think. */
    for (curPtr=&gc->destructorHead;(*curPtr)!=NULL;) {
        fivmr_GCHeader *hdr;
        
        if (shouldQuit(gc)) goto quit;
        
	cur=*curPtr;
        
        if (FIVMR_ASSERTS_ON &&
            FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert((uintptr_t)cur>=gc->memStart);
            fivmr_assert((uintptr_t)cur<gc->memEnd);
        }
	
	fivmr_assert(cur->object!=0);
	
        hdr=fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                      cur->object);
        
        if (FIVMR_ASSERTS_ON &&
            FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert((uintptr_t)hdr>=gc->memStart);
            fivmr_assert((uintptr_t)hdr<gc->memEnd);
        }

	if ((fivmr_GCHeader_markBits(hdr) & gc->curShaded)==0) {
            LOG(2,("marking destructor %p for object %p for execution.",
                   cur,cur->object));
            
	    /* remove from this list */
	    *curPtr=cur->next;
	    
	    /* move to other list */
	    cur->next=gc->destructorsToRun;
	    gc->destructorsToRun=cur;
	} else {
	    /* mark destructor as live */
	    shadeRawTypeAsNecessary(gc,
                                    (uintptr_t)cur,
                                    sizeof(fivmr_Destructor));
            
            curPtr=&(*curPtr)->next;
	}
    }
    
    /* for all destructors that need to be run but have not yet been run,
       shade both the destructor and the object it refers to. */
    for (cur=gc->destructorsToRun;cur!=NULL;cur=cur->next) {
        if (shouldQuit(gc)) goto quit;

        if (FIVMR_ASSERTS_ON &&
            FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert((uintptr_t)cur>=gc->memStart);
            fivmr_assert((uintptr_t)cur<gc->memEnd);
        }
        
	fivmr_assert(cur->object!=0);
	
        LOG(2,("marking finalizable %p",cur->object));
	mark(gc,queue,
             fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                       cur->object));
	shadeRawTypeAsNecessary(gc,
                                (uintptr_t)cur,
                                sizeof(fivmr_Destructor));
    }

    if (gc->destructorsToRun!=NULL) {
	fivmr_Lock_broadcast(&gc->destructorLock);
    }
    fivmr_Lock_unlock(&gc->destructorLock);
    
    /* this is sneaky.  this must return true if after this point, another transitive
       closure should be performed because some objects got revived.  naively, this means
       that we should only return true if we detect that there are live destructors.
       but the reality is more complicated.  there may be an interleaving where some
       finalizer thread dequeues a destructor after the previous transitive closure but
       before this function grabbed the destructor lock.  in that case, that finalizer
       thread would have revived the destructor (by placing it in its root set), but
       we would not have seen it.  so, we must return true whenever we think that any
       destructor processing may be taking place.  in practice that means returning true
       unconditionally. */
    return true;

quit:
    fivmr_Lock_unlock(&gc->destructorLock);
    
    return false;
}

static void performCopying(fivmr_GC *gc) {
    uintptr_t count=0;
    LOG(2,("Performing copying."));
    while (gc->ss.head!=NULL) {
        fivmr_GCHeader *cur=gc->ss.head;
        
        copySpine(gc,
                  fivmr_GCHeader_toObject(&fivmr_VMfromGC(gc)->settings,
                                          cur));
        if (shouldQuit(gc)) return;
        
        gc->ss.head=fivmr_GCHeader_next(cur);
        count++;
    }
    LOG(2,("Copied %" PRIuPTR " objects.",count));
}

static inline void addLineInSweep(fivmr_Settings *settings,
                                  fivmr_FreeLine *head,
				  fivmr_FreeLine *tail,
				  uintptr_t start,
				  uintptr_t end,
				  uintptr_t *lineBytesFreedPtr,
				  bool zero) {
    if (end-start>=FIVMR_MIN_FREE_LINE_SIZE(settings)) {
	fivmr_FreeLine *fl;
	LOG(4,("Creating free line from %p to %p",
	       start,end));
	fl=(fivmr_FreeLine*)start;
	fl->size=end-start;
	fl->next=head->next;
	fl->prev=head;
	head->next->prev=fl;
	head->next=fl;
	fl->lastOnPage=tail->prev;
	(*lineBytesFreedPtr)+=end-start;
	if (zero) {
	    bzero(fl+1,end-start-sizeof(fivmr_FreeLine));
	}
    }
}

static inline void lineSweepPage(fivmr_GC *gc,
                                 uintptr_t pageAddress,
				 fivmr_GCSpace space,
				 uintptr_t *lineBytesFreedPtr) {
    fivmr_UsedPage *page;
    fivmr_FreeLine head;
    fivmr_FreeLine tail;
    uintptr_t index;
    uintptr_t zeroCount=0;
    uintptr_t wordI;
    uint32_t *bits;
    fivmr_Settings *settings;
    
    settings=&fivmr_VMfromGC(gc)->settings;
    
    LOG(3,("Line sweeping %p in space %d",pageAddress,space));
    page=(fivmr_UsedPage*)pageAddress;
    head.next=&tail;
    tail.prev=&head;
    
    fivmr_assert(*fivmr_UsedPage_reserved(page,settings)!=0);
    
    /* pages in the object space should be swept by using the bitvector
       in the page header. */
    
    removeFreeLinesInPage(gc,page);
    fivmr_assert(*fivmr_UsedPage_status(page,settings)
                 == FIVMR_GCUPS_ZERO);
    
    LOG(4,("sweeping bits..."));
    
    bits=fivmr_UsedPage_bits(page,settings);
    
    /* now sweep the clear bits */
    for (wordI=0;
         wordI<FIVMR_UP_BITS_LENGTH(settings);
         ++wordI) {
        uint32_t word=bits[wordI];
        uintptr_t bitI=0;
        if (word!=(uint32_t)-1) {
            bool cont=true;
            
            while (cont) {
                uintptr_t start,end;
		
                while (word&1) {
                    word>>=1;
                    bitI++;
                }

                if (bitI==32) {
                    break;
                }
		    
                fivmr_assert(bitI<32);

                /* found a free one... */
                start=(wordI<<5)+bitI;
                if (start>=((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                            FIVMR_MIN_CHUNK_ALIGN(settings))) {
                    goto doneLineSweep;
                }
		    
                if (!word) {
                    bitI=0;
                    do {
                        word=bits[++wordI];
                    } while (!word);
                    if (wordI==(uint32_t)-1) {
                        cont=false;
                        goto skipBitSearch;
                    }
                }
		    
                while ((word&1)==0) {
                    word>>=1;
                    bitI++;
                }
            skipBitSearch:
		    
                /* found the end */
                end=(wordI<<5)+bitI;
		    
                fivmr_assert(end>start);
                fivmr_assert(start<((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                                    FIVMR_MIN_CHUNK_ALIGN(settings)));
                fivmr_assert(end<=((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                                   FIVMR_MIN_CHUNK_ALIGN(settings)+64));
                if (end>=((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                          FIVMR_MIN_CHUNK_ALIGN(settings))) {
                    end=(FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))
                        / FIVMR_MIN_CHUNK_ALIGN(settings);
                    fivmr_assert(end>start);
                    cont=false;
                } else {
                    fivmr_assert(fivmr_BitVec_get(bits,end));
                }

                fivmr_assert(!fivmr_BitVec_get(bits,end-1));
                if (start>0) {
                    fivmr_assert(fivmr_BitVec_get(bits,start-1));
                }
                fivmr_assert(!fivmr_BitVec_get(bits,start));
                if (FIVMR_ASSERTS_ON) {
                    for (index=start;index<end;++index) {
                        fivmr_assert(!fivmr_BitVec_get(bits,index));
                    }
                }
                LOG(5,("on page %p adding line from %p to %p",page,start,end));

                if (FIVMR_ASSERTS_ON) {
                    zeroCount+=end-start;
                }

                addLineInSweep(
                    settings,
                    &head,&tail,
                    (uintptr_t)page+FIVMR_PAGE_HEADER(settings)+start*FIVMR_MIN_CHUNK_ALIGN(settings),
                    (uintptr_t)page+FIVMR_PAGE_HEADER(settings)+end*FIVMR_MIN_CHUNK_ALIGN(settings),
                    lineBytesFreedPtr,false);
            }
        }
    }
doneLineSweep:
    
    if (false && FIVMR_ASSERTS_ON) {
        uintptr_t verifiedZeroCount=0;
        for (index=0;
             index<((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                    FIVMR_MIN_CHUNK_ALIGN(settings));
             ++index) {
            if (!fivmr_BitVec_get(bits,index)) {
                verifiedZeroCount++;
            }
        }
        fivmr_assert(zeroCount==verifiedZeroCount);
    }

    fivmr_assert(*fivmr_UsedPage_status(page,settings)
                 == FIVMR_GCUPS_ZERO);

    if (head.next!=&tail) {
	LOG(3,("Pushing free lines on page %p from %p to %p",
	       pageAddress,head.next,tail.prev));
	fivmr_assert(head.next->lastOnPage==tail.prev);
	fivmr_Lock_lock(&gc->gcLock);
	*fivmr_UsedPage_status(page,settings)=
	    FIVMR_GCUPS_FREE_LINES|((uintptr_t)head.next-(uintptr_t)page);
	head.next->prev=&gc->spaceData[space].freeLineHead;
	tail.prev->next=gc->spaceData[space].freeLineHead.next;
	gc->spaceData[space].freeLineHead.next->prev=tail.prev;
	gc->spaceData[space].freeLineHead.next=head.next;
	fivmr_Lock_unlock(&gc->gcLock);
    }
    
    fivmr_PageTable_setAssert(&gc->spaceData[space].pt,
                              pageAddress,
                              FIVMR_GCPS_ZERO,
                              FIVMR_GCPS_POPULATED);
}

static inline uintptr_t sweep(fivmr_GC *gc,
                              fivmr_GCSpace space,
			      uintptr_t *pagesFreedPtr,
			      uintptr_t *lineBytesFreedPtr) {
    uintptr_t pagesKept;
    uintptr_t pagesFreed;
    BunchOfPages bop;
    uintptr_t baseAddress;
    uint32_t *chunk;
    uintptr_t wordI;
    uintptr_t wordILimit;
    fivmr_PTIterator iter;

    pagesFreed=0;
    pagesKept=0;
    initBunchOfPages(&bop);

    LOG(2,("Sweeping space %d...",space));

    /* NB this is racing against the page table in the case that new
       chunks get allocated.  I think that's fine.  Note the fences,
       though... they're there for that reason.  Note also that there
       are similar fences in fivmr_pagetable.c. */
    for (fivmr_PTIterator_init(&iter,&gc->spaceData[space].pt);
         fivmr_PTIterator_valid(&iter);
         fivmr_PTIterator_next(&iter)) {
        baseAddress=iter.baseAddress;
        chunk=iter.chunk;
        wordILimit=iter.chunkLength;
        for (wordI=0;
             wordI<wordILimit;
             wordI++) {
            uint32_t oldWord;
            bool toFree=false;
            uintptr_t toKeep;
            if (shouldQuit(gc)) goto quit;

            for (;;) { /* cas loop */
                uint32_t curWord=oldWord=chunk[wordI];
                uint32_t newWord=0;
                toKeep=0;
                
                /* assert that our logic isn't totally broken */
                fivmr_assert(
                    fivmr_PageTable_getWord(
                        &gc->spaceData[space].pt,
                        baseAddress+((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))
                                     <<FIVMSYS_LOG_PAGE_SIZE))
                    ==chunk+wordI);
                
                if (curWord) {
                    uintptr_t bitI;
                    for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                        uint32_t curBits=
                            curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1);
				
                        switch (curBits) {
                        case FIVMR_GCPS_POPULATED:
                            curBits=FIVMR_GCPS_ZERO;
                            toFree=true;
                            break;
                        case FIVMR_GCPS_SHADED:
                            curBits=FIVMR_GCPS_ZERO;
                            toKeep++;
                            break;
                        case FIVMR_GCPS_RELINQUISHED:
                            curBits=FIVMR_GCPS_POPULATED;
                            toKeep++;
                            break;
                        default:
                            break;
                        }
                        curWord>>=FIVMR_PT_BITS_PER_PAGE;
                        newWord|=(curBits<<bitI);
                    }
                    if (fivmr_cas32_weak((int32_t*)(chunk+wordI),
                                         oldWord,newWord)) {
                        break;
                    }
                } else {
                    /* empty set of pages - go to next one */
                    break;
                }
            }
            pagesKept+=toKeep;
            if (toFree) {
                uint32_t curWord=oldWord;
                uintptr_t bitI;
                for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                    uint32_t curBits=
                        curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1);
                    if (curBits==FIVMR_GCPS_POPULATED) {
                        uintptr_t pageAddress=
                            baseAddress+
                            (((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))+
                              (bitI>>FIVMR_LOG_PT_BITS_PER_PAGE))
                             <<FIVMSYS_LOG_PAGE_SIZE);
                        LOG(3,("Freeing page %p with wordI=%" PRIuPTR
                               " and bitI=%" PRIuPTR " in space %d",
                               pageAddress,wordI,bitI,space));
                        reusePage(gc,&bop,(void*)pageAddress);
                        (*pagesFreedPtr)++;
                    }
                    curWord>>=FIVMR_PT_BITS_PER_PAGE;
                }
            }
            LOG(5,("oldWord = %u",oldWord));
            if (oldWord) {
                uint32_t curWord=oldWord;
                uintptr_t bitI;
                LOG(4,("line sweeping with curWord = %u",curWord));
                for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                    uint32_t curBits;
                    uintptr_t pageAddress;
                    curBits=curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1);
                    curWord>>=FIVMR_PT_BITS_PER_PAGE;
                    pageAddress=
                        baseAddress+
                        (((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))+
                          (bitI>>FIVMR_LOG_PT_BITS_PER_PAGE))
                         <<FIVMSYS_LOG_PAGE_SIZE);
                    if (curBits==FIVMR_GCPS_SHADED) {
                        lineSweepPage(gc,pageAddress,space,
                                      lineBytesFreedPtr);
                    } else if (curBits!=0) {
                        LOG(3,("Not line sweeping %p in space %d because curBits = %d",
                               pageAddress,space,curBits));
                    }
                }
            }
        }
    }
	
quit:
    reuseBunchOfPages(gc,&bop);
    
    LOG(2,("Freed %" PRIuPTR " pages, kept %" PRIuPTR" pages, freelines have %" PRIuPTR " bytes.",
	   *pagesFreedPtr,pagesKept,*lineBytesFreedPtr));

    return pagesFreed;
}

static inline void scanSharedArea(fivmr_GC *gc,
                                  fivmr_GCHeader **queue,
                                  fivmr_MemoryArea *area) {
    fivmr_Settings *settings=&fivmr_VMfromGC(gc)->settings;
    uintptr_t scanPoint;
    for (scanPoint=area->objList;scanPoint!=0;scanPoint=*(uintptr_t *)scanPoint) {
        fivmr_Object obj;

        if (shouldQuit(gc)) return;

        obj=scanPoint+sizeof(uintptr_t)+FIVMR_ALLOC_OFFSET(settings);
        fivmr_assert(fivmr_Object_isContiguous(settings,obj));
        collectorScanObject(gc,queue,obj);
    }
}

static inline void cleanPageTable(fivmr_GC *gc,
                                  fivmr_GCSpace space) {
    uintptr_t baseAddress;
    uint32_t *chunk;
    uintptr_t wordI;
    uintptr_t wordILimit;
    fivmr_PTIterator iter;

    for (fivmr_PTIterator_init(&iter,&gc->spaceData[space].pt);
         fivmr_PTIterator_valid(&iter);
         fivmr_PTIterator_next(&iter)) {
        baseAddress=iter.baseAddress;
        chunk=iter.chunk;
        wordILimit=iter.chunkLength;
        for (wordI=0;
             wordI<wordILimit;
             wordI++) {
            uint32_t oldWord;
            if (shouldQuit(gc)) return;
            for (;;) { /* cas loop */
                uint32_t curWord=oldWord=chunk[wordI];
                uint32_t newWord=0;
                    
                /* assert that our logic isn't totally broken */
                fivmr_assert(
                    fivmr_PageTable_getWord(
                        &gc->spaceData[space].pt,
                        baseAddress+((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))
                                     <<FIVMSYS_LOG_PAGE_SIZE))
                    ==chunk+wordI);
                    
                if (curWord) {
                    uintptr_t bitI;
                    for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                        uint32_t curBits=
                            curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1);
                            
                        switch (curBits) {
                        case FIVMR_GCPS_RELINQUISHED:
                            curBits=FIVMR_GCPS_POPULATED;
                            break;
                        default:
                            break;
                        }
                        curWord>>=FIVMR_PT_BITS_PER_PAGE;
                        newWord|=(curBits<<bitI);
                    }
                    if (fivmr_cas32_weak((int32_t*)(chunk+wordI),
                                         oldWord,newWord)) {
                        break;
                    }
                } else {
                    /* empty set of pages - go to next one */
                    break;
                }
            }
        }
    }
}

static void collectorThreadMain(void *arg) {
    fivmr_GC *gc;
    uintptr_t maxUsed=0;
    
    gc=(fivmr_GC*)arg;
    
    fivmr_Lock_lock(&fivmr_VMfromGC(gc)->lock);
    gc->thread=fivmr_ThreadHandle_current();
    fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->lock);
    
    LOG(1,("Thread %p operational; stack at %p.",gc->thread,&arg));
    
    for (;;) {
	fivmr_ThreadState *requesterHead;
	uintptr_t i;
	fivmr_Handle *h;
	fivmr_GCHeader *queue;
	fivmr_LargeObjectHeader *loh;
	fivmr_LargeObjectHeader *lohHead;
	fivmr_LargeObjectHeader *lohTail;
        fivmr_MachineCode *mc;
        fivmr_MachineCode *mcHead;
        fivmr_MachineCode *mcTail;
	uintptr_t pagesFreed;
	uintptr_t lineBytesFreed;
	uintptr_t objectsTraced;
	fivmr_Nanos startTime;
	fivmr_Nanos afterThreads;
	fivmr_Nanos afterRoots;
	fivmr_Nanos afterTrace;
	fivmr_Nanos afterCopy;
	fivmr_Nanos afterSmallSweep;
	fivmr_Nanos afterLargeSweep;
        fivmr_Nanos beforePTClean;
        fivmr_Nanos afterPTClean;
        bool shouldBeRunning;
        uintptr_t curString;
        uintptr_t stringStep;
	
	LOG(1,("Waiting for GC request."));
        for (;;) {
            fivmr_Lock_lock(&gc->gcLock);
            shouldBeRunning=fivmr_GC_shouldBeRunning(gc);
            fivmr_Lock_unlock(&gc->gcLock);
            
            if (shouldBeRunning || shouldQuit(gc)) {
                break;
            }
            
            LOG(11,("waiting on trigger in GC"));
            fivmr_CritSemaphore_down(&gc->triggerSema);
            LOG(11,("trigger received!"));
            /* need to reloop.  because someone could have done an 'up'
               spuriously.  I guess I could prevent that from happening.
               but I don't care enough about it, honestly. */
        }
        LOG(11,("GC starting"));
        
        if (shouldQuit(gc)) goto shutdown;

        fivmr_Lock_lock(&gc->gcLock);
	fivmr_assert(fivmr_GC_shouldBeRunning(gc));
	requesterHead=gc->requesterHead;
	gc->requesterHead=NULL;
	gc->asyncRequested=false;
        gc->lastStart=fivmr_curTime();
	fivmr_Lock_unlock(&gc->gcLock);
	
	startTime=fivmr_curTime();
	LOG(1,("Starting collection with %" PRIuPTR " pages used.",
	       gc->numPagesUsed));
	
        if (FIVMR_ASSERTS_ON && FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert(gc->numMarked==0);
        }

	fivmr_assert(gc->globalQueue==NULL);
	objectsTraced=0;
	pagesFreed=0;
	lineBytesFreed=0;
	
	assertPerThreadQueuesSound(gc);
        
        if (FIVMR_LOG_GC_MARK_TRAPS) {
            uint64_t stamp=fivmr_readCPUTimestamp();
            fp_log(0,
                   "collection beginning at %u:%u",2,
                   (uintptr_t)((stamp>>32)&0xffffffff),
                   (uintptr_t)((stamp>>0)&0xffffffff));
        }
	
	LOG(2,("Notifying mutators that collection is beginning..."));
	fivmr_assert(gc->phase==FIVMR_GCP_IDLE);
	gc->phase=FIVMR_GCP_PRE_INIT;

        /* in all configurations, this disables marking while we rotate mark
           bits.  in HFGC, it also flips all mutators to allocating in the
           semi-space to-space. */
        if (FIVMR_HFGC(&fivmr_VMfromGC(gc)->settings)) {
            flipSpaces(gc);
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_RELINQUISH_SPACE|
                                            FIVMR_TSEF_GC_MISC);
        } else {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_GC_MISC);
        }
	
        if (shouldQuit(gc)) goto shutdown;

	LOG(2,("Rotating mark bits..."));
	gc->numMarked=0;

	LOG(2,("old curShaded = %p, old invCurShaded = %p",
               gc->curShaded,gc->invCurShaded));
	gc->curShaded^=FIVMR_GC_SH_ALWAYS_MARKED;
        gc->invCurShaded^=FIVMR_GC_SH_ALWAYS_MARKED;
	
	/* now anything stored into objects will be shaded - but new objects will
	   still be allocated white.  this prevents a race where one thread
	   allocates a black object and another stores a white object into it. */

	LOG(2,("new curShaded = %p, new invCurShaded = %p",
               gc->curShaded,gc->invCurShaded));
        /* is this necessary? */
        if (FIVMR_GC_BLACK_STACK(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_GC_MISC);
        }
	
        if (FIVMR_ASSERTS_ON && FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert(gc->numMarked==0);
        }
	
        if (shouldQuit(gc)) goto shutdown;

	LOG(2,("Notifying mutators of trace start..."));
	gc->phase=FIVMR_GCP_INIT;
        /* make sure that everyone starts marking before we start allocating
           black.  without this, it's possible for a thread to allocate a black
           object and pass it to a thread that isn't marking yet; that thread may
           then store a white object into the black object and then kaboom. */
        if (FIVMR_AGGRESSIVE_OPT_CM_STORE_BARRIERS(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            (FIVMR_TSEF_GC_MISC|
                                             FIVMR_TSEF_SCAN_THREAD_ROOTS));
        } else {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_GC_MISC);
        }

	LOG(2,("Notifying mutators to allocate black..."));
	LOG(2,("old curShadedAlloc = %p",gc->curShadedAlloc));
	gc->curShadedAlloc^=FIVMR_GC_SH_ALWAYS_MARKED;
	/* now objects are allocated black. */
	
        if (shouldQuit(gc)) goto shutdown;

	LOG(2,("new curShadedAlloc = %p",gc->curShadedAlloc));
	fivmr_assert(gc->curShaded==gc->curShadedAlloc);
        if (FIVMR_GC_BLACK_STACK(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_GC_MISC);
        }

        if (shouldQuit(gc)) goto shutdown;

	LOG(2,("Requesting stack scan..."));
	gc->phase=FIVMR_GCP_STACK_SCAN; /* this phase thing is stupid */
        if (FIVMR_AGGRESSIVE_OPT_CM_STORE_BARRIERS(&fivmr_VMfromGC(gc)->settings) &&
            !FIVMR_GC_BLACK_STACK(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_ThreadState_softHandshake(
                fivmr_VMfromGC(gc),
                FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                (FIVMR_TSEF_GC_MISC|
                 FIVMR_TSEF_COMMIT_DESTRUCTORS));
        } else {
            fivmr_ThreadState_softHandshake(
                fivmr_VMfromGC(gc),
                FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                (FIVMR_TSEF_SCAN_THREAD_ROOTS|
                 FIVMR_TSEF_GC_MISC|
                 FIVMR_TSEF_COMMIT_DESTRUCTORS));
        }
	gc->phase=FIVMR_GCP_TRACE;
	
        if (shouldQuit(gc)) goto shutdown;

	afterThreads=fivmr_curTime();
        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            LOG(2,("Marked %" PRIuPTR " objects.",gc->numMarked));
        }

	/* move the global queue into the local one (this gives us thread
	   roots). */
	fivmr_Lock_lock(&gc->gcLock);
	queue=gc->globalQueue;
	gc->globalQueue=NULL;
	fivmr_Lock_unlock(&gc->gcLock);
	
	LOG(2,("We have the results of the stack scan, with head %p.  "
	       "Marking global roots...",
	       queue));
        for (i=0;i<(uintptr_t)fivmr_VMfromGC(gc)->payload->nContexts;++i) {
            if (shouldQuit(gc)) goto shutdown;
            scanContext(gc,&queue,fivmr_VMfromGC(gc)->baseContexts[i]);
        }
        
        if (FIVMR_CLASSLOADING(&fivmr_VMfromGC(gc)->settings)) {
            /* FIXME: to support class unloading, we'll need to remove this. */
            
            fivmr_Lock_lock(&fivmr_VMfromGC(gc)->typeDataLock);
            for (i=0;i<(uintptr_t)fivmr_VMfromGC(gc)->nDynContexts;++i) {
                if (shouldQuit(gc)) break;
                scanContext(gc,&queue,fivmr_VMfromGC(gc)->dynContexts[i]);
            }
            fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->typeDataLock);
            if (shouldQuit(gc)) goto shutdown;
        }
        
        for (i=0;i<=fivmr_VMfromGC(gc)->maxThreadID;i++) {
            if (shouldQuit(gc)) goto shutdown;
            collectorMarkObj(gc,&queue,fivmr_VMfromGC(gc)->javaThreads[i].obj);
        }
        
        for (i=fivmr_VMfromGC(gc)->payload->nRefFields;i-->0;) {
            if (shouldQuit(gc)) goto shutdown;
	    collectorMarkObj(gc,&queue,fivmr_VMfromGC(gc)->refFields[i]);
	}
	
        if (!FIVMR_CLASSLOADING(&fivmr_VMfromGC(gc)->settings)) {
            for (i=fivmr_VMfromGC(gc)->payload->nTypes;i-->0;) {
                fivmr_TypeData *td;
                if (shouldQuit(gc)) goto shutdown;
                td=fivmr_VMfromGC(gc)->payload->typeList[i];
                fivmr_assert(td->classObject!=0);
                collectorScanObject(gc,&queue,td->classObject);
            }
        }
        
        curString=fivmr_getFirstString(fivmr_VMfromGC(gc)->payload);
        stringStep=fivmr_getStringDistance(fivmr_VMfromGC(gc)->payload);
	for (i=fivmr_VMfromGC(gc)->payload->nStrings;i-->0;) {
            if (shouldQuit(gc)) goto shutdown;
	    /* strings may be synchronized on ... hence this nast. */
	    handleObjectHeader(gc,curString);
            curString+=stringStep;
	}
	
	fivmr_Lock_lock(&fivmr_VMfromGC(gc)->hrLock);
	/* FIXME: we're holding the lock for however long it takes...  wouldn't
	   need to do this if handles were GC'd. */
	for (h=fivmr_VMfromGC(gc)->hr.head.next;
             h!=&fivmr_VMfromGC(gc)->hr.tail;
             h=h->next) {
	    collectorMarkHandle(gc,&queue,h);
	}
	fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->hrLock);
	
        if (shouldQuit(gc)) goto shutdown;

        if (FIVMR_SCOPED_MEMORY(&fivmr_VMfromGC(gc)->settings)) {
            /* Scan immortal memory -- we don't have to "enter" this shared
             * area because it cannot go away */
            scanSharedArea(gc,&queue,
                           &fivmr_VMfromGC(gc)->gc.immortalMemoryArea);
            if (shouldQuit(gc)) goto shutdown;
            if (FIVMR_SCJ_SCOPES(&fivmr_VMfromGC(gc)->settings)) {
                /* Scan other shared areas */
                /* FIXME: We should "enter" these, to prevent their
                 * destruction before the scan is finished! */
                while (gc->areaQueue) {
                    fivmr_MemoryArea *area=gc->areaQueue;
                    scanSharedArea(gc,&queue,area);
                    if (shouldQuit(gc)) goto shutdown;
                    gc->areaQueue=(fivmr_MemoryArea *)(area->shared&~0x3);
                    area->shared=1;
                }
            } else if (FIVMR_RTSJ_SCOPES(&fivmr_VMfromGC(gc)->settings)) {
                fivmr_MemoryAreaStack *cur, *prev;
                fivmr_Lock_lock(&gc->sharedAreasLock);
                prev=NULL;
                cur=gc->sharedAreas;
                while (prev||cur) {
                    if (prev) {
                        prev->flags&=~FIVMR_MEMORYAREASTACK_GCINPROGRESS;
                        if (prev->flags&FIVMR_MEMORYAREASTACK_POP) {
                            fivmr_MemoryArea_pop(NULL, fivmr_VMfromGC(gc),
                                                 prev->area);
                            if (prev->flags&FIVMR_MEMORYAREASTACK_WAITING) {
                                fivmr_Lock_broadcast(&gc->sharedAreasLock);
                            }
                        }
                        if (prev->flags&FIVMR_MEMORYAREASTACK_FREE) {
                            fivmr_MemoryArea_free(NULL, prev->area);
                            fivmr_assert(!(prev->flags&FIVMR_MEMORYAREASTACK_WAITING));
                        }
                    }
                    prev=cur;
                    if (cur) {
                        cur->flags|=FIVMR_MEMORYAREASTACK_GCINPROGRESS;
                        fivmr_Lock_unlock(&gc->sharedAreasLock);
                        scanSharedArea(gc,&queue,cur->area);
                        fivmr_Lock_lock(&gc->sharedAreasLock);
                        cur=cur->next;
                    }
                }
                fivmr_Lock_unlock(&gc->sharedAreasLock);
            }
        }


        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            LOG(2,("Marked %" PRIuPTR " objects.",gc->numMarked));
        }
	afterRoots=fivmr_curTime();

	transitiveClosure(gc,&queue,&objectsTraced);
        if (shouldQuit(gc)) goto shutdown;
	if (processDestructors(gc,&queue)) {
	    transitiveClosure(gc,&queue,&objectsTraced);
	}
        if (shouldQuit(gc)) goto shutdown;
        
        if (FIVMR_HFGC(&fivmr_VMfromGC(gc)->settings)) {
            doneGCSpaceAlloc(gc);
            /* NOTE: we don't need a handshake to indicate that forwarding
               pointers have been installed, since transitiveClosure() already
               does a handshake at the end. */
        }
	
        if (shouldQuit(gc)) goto shutdown;

	afterTrace=fivmr_curTime();
	gc->phase=FIVMR_GCP_SWEEP;
        
        if (FIVMR_FILTERED_CM_STORE_BARRIERS(&fivmr_VMfromGC(gc)->settings)) {
            /* set tracing to false in all threads.  this does not require a soft
               handshake. */
            for (i=0;i<=fivmr_VMfromGC(gc)->maxThreadID;++i) {
                fivmr_ThreadState_byId(fivmr_VMfromGC(gc),i)->gc.tracing=false;
            }
        }
        
        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            LOG(2,("Marked %" PRIuPTR " objects.",gc->numMarked));
            fivmr_assert(gc->numMarked==objectsTraced);
            gc->numMarked=0;
        }
	LOG(2,("Traced %" PRIuPTR " objects.",objectsTraced));
	
	/* small page sweep for objects */
        sweep(gc,FIVMR_GC_OBJ_SPACE,&pagesFreed,&lineBytesFreed);
        if (shouldQuit(gc)) goto shutdown;

	afterSmallSweep=fivmr_curTime();

	LOG(2,("Sweeping large object pages..."));
	fivmr_Lock_lock(&gc->gcLock);
	loh=gc->largeObjectHead;
	gc->largeObjectHead=NULL;
	fivmr_Lock_unlock(&gc->gcLock);
	
	lohHead=NULL;
	lohTail=NULL;
	
        /* FIXME: GC is not quit-able in here! */
	while (loh!=NULL) {
	    fivmr_LargeObjectHeader *next=loh->next;
	    fivmr_GCHeader *hdr=loh->object;
	    if (fivmr_GCHeader_markBits(hdr)!=gc->curShaded) {
		uintptr_t numPages;
		numPages=fivmr_pages(loh->fullSize);
		freeLargeObject(gc,loh);
		pagesFreed+=numPages;
	    } else {
		loh->next=lohHead;
		lohHead=loh;
		if (lohTail==NULL) {
		    lohTail=loh;
		}
	    }
	    loh=next;
	}
	
	if (lohHead!=NULL) {
	    fivmr_Lock_lock(&gc->gcLock);
	    lohTail->next=gc->largeObjectHead;
	    gc->largeObjectHead=lohHead;
	    fivmr_Lock_unlock(&gc->gcLock);
	}

        if (shouldQuit(gc)) goto shutdown;
        
        LOG(2,("Sweeping machine codes..."));
        fivmr_Lock_lock(&gc->gcLock);
        mc=gc->machineCodeHead;
        gc->machineCodeHead=NULL;
        fivmr_Lock_unlock(&gc->gcLock);
        
        mcHead=NULL;
        mcTail=NULL;
        
        while (mc!=NULL) {
            fivmr_MachineCode *next=mc->next;
            if (!(mc->flags&FIVMR_MC_GC_MARKED)) {
                fivmr_MachineCode_down(mc);
            } else {
                fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_MARKED,0);
                mc->next=mcHead;
                mcHead=mc;
                if (mcTail==NULL) {
                    mcTail=mc;
                }
            }
            mc=next;
        }
        
        if (mcHead!=NULL) {
            fivmr_Lock_lock(&gc->gcLock);
            mcTail->next=gc->machineCodeHead;
            gc->machineCodeHead=mcHead;
            fivmr_Lock_unlock(&gc->gcLock);
        }
	
        if (shouldQuit(gc)) goto shutdown;
        
	afterLargeSweep=fivmr_curTime();
	
        if (FIVMR_HFGC(&fivmr_VMfromGC(gc)->settings)) {
            performCopying(gc);
            if (shouldQuit(gc)) goto shutdown;
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            0);
            sweepOldSpace(gc);
            if (shouldQuit(gc)) goto shutdown;
        }
        afterCopy=fivmr_curTime();
	
	gc->phase=FIVMR_GCP_IDLE;

        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            LOG(2,("Marked %" PRIuPTR " objects.",gc->numMarked));
            fivmr_assert(gc->numMarked==0);
        }
        if (FIVMR_LOG_GC_MARK_TRAPS) {
            uint64_t stamp=fivmr_readCPUTimestamp();
            fp_log(0,
                   "collection ending at %u:%u",2,
                   (uintptr_t)((stamp>>32)&0xffffffff),
                   (uintptr_t)((stamp>>0)&0xffffffff));
        }
        if (gc->logGC || gc->logSyncGC) {
	    uintptr_t used=(gc->numPagesUsed<<FIVMSYS_LOG_PAGE_SIZE)-lineBytesFreed;
            bool isSync=(requesterHead!=NULL||gc->waiterHead!=NULL);
	    if (used>maxUsed) {
	        maxUsed=used;
	    }
            if (isSync || gc->logGC) {
                fivmr_Log_lockedPrintf("[GC (%s): %ums, %" PRIuPTR" bytes used (%"
                                       PRIuPTR " max)]\n",
                                       isSync?"sync":"async",
                                       (uint32_t)((afterLargeSweep-startTime)/1000/1000),
                                       used,
                                       maxUsed);
            }
        }
#if FIVMR_RTEMS
        /* some OS's, like RTEMS, cannot print 64-bit values */
	LOG(1,("Finished collection with %" PRIuPTR " pages used; freed %" PRIuPTR
	       " pages + %" PRIuPTR" bytes.  Collection took %u+%u+%u+%u+%u+%u=%u ns",
	       gc->numPagesUsed,pagesFreed,lineBytesFreed,
	       (uint32_t)(afterThreads-startTime),
	       (uint32_t)(afterRoots-afterThreads),
	       (uint32_t)(afterTrace-afterRoots),
	       (uint32_t)(afterSmallSweep-afterTrace),
	       (uint32_t)(afterLargeSweep-afterSmallSweep),
	       (uint32_t)(afterCopy-afterLargeSweep),
	       (uint32_t)(afterCopy-startTime)));
#else
	LOG(1,("Finished collection with %" PRIuPTR " pages used; freed %" PRIuPTR
	       " pages + %" PRIuPTR" bytes.  Collection took %" PRIu64 "+%" PRIu64
	       "+%" PRIu64 "+%" PRIu64 "+%" PRIu64 "+%" PRIu64
               "=%" PRIu64 " ns",
	       gc->numPagesUsed,pagesFreed,lineBytesFreed,
	       afterThreads-startTime,
	       afterRoots-afterThreads,
	       afterTrace-afterRoots,
	       afterSmallSweep-afterTrace,
	       afterLargeSweep-afterSmallSweep,
	       afterCopy-afterLargeSweep,
	       afterCopy-startTime));
#endif

	assertPerThreadQueuesSound(gc);
        
	/* GC done.  notify everyone. */
	fivmr_Lock_lock(&gc->gcLock);
	gc->iterationCount++;
        gc->lastEnd=fivmr_curTime();
        if (requesterHead!=NULL || gc->waiterHead!=NULL) {
            gc->blockedIterationCount++;
        }
	while (requesterHead!=NULL) {
	    fivmr_ThreadState *cur=requesterHead;
	    
            if (shouldQuit(gc)) goto shutdown;

	    cur->gc.gcFlags&=~FIVMR_GCDF_REQUESTED_GC;
	    requesterHead=cur->gc.requesterNext;
	    cur->gc.requesterNext=NULL;
	}
        while (gc->waiterHead!=NULL) {
            fivmr_ThreadState *cur=gc->waiterHead;
            
            if (shouldQuit(gc)) goto shutdown;

            cur->gc.gcFlags&=~FIVMR_GCDF_REQUESTED_GC;
            gc->waiterHead=cur->gc.requesterNext;
            cur->gc.requesterNext=NULL;
        }
	fivmr_Lock_unlock(&gc->gcLock);
        fivmr_Lock_lockedBroadcast(&gc->notificationLock);
        
        LOG(1,("Cleaning up page tables."));
        
        if (shouldQuit(gc)) goto shutdown;
        
        beforePTClean=fivmr_curTime();
        cleanPageTable(gc,FIVMR_GC_OBJ_SPACE);
        afterPTClean=fivmr_curTime();
        
        if (shouldQuit(gc)) goto shutdown;

        fivmr_Lock_lock(&gc->gcLock);
        gc->gcThreadTime+=afterPTClean-startTime;
        fivmr_Lock_unlock(&gc->gcLock);
        
#if FIVMR_RTEMS
        LOG(1,("Cleaned page tables in %u ns",
               (uint32_t)(afterPTClean-beforePTClean)));
#else
        LOG(1,("Cleaned page tables in %" PRIu64 " ns",
               afterPTClean-beforePTClean));
#endif
    }

shutdown:
    fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                             fivmr_VMfromGC(gc)->maxPriority);
    fivmr_Lock_lock(&fivmr_VMfromGC(gc)->lock);
    gc->thread=fivmr_ThreadHandle_zero();
    gc->threadDone=true;
    fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->lock);
    fivmr_Semaphore_up(&gc->doneSema);
    LOG(1,("GC thread dying!"));
    fivmr_debugMemory();
    /* anything else? */
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_init)(fivmr_GC *gc) {
    fivmr_GCSpace i;
    fivmr_Settings *settings=&fivmr_VMfromGC(gc)->settings;
    
    fivmr_debugMemory();

    /* do some verification of configuration */
    fivmr_assert(FIVMR_HFGC(settings) ^ FIVMR_CMRGC(settings));
    fivmr_assert(FIVMR_HM_NARROW(settings) ^ FIVMR_HM_POISONED(settings));
    fivmr_assert(FIVMR_OM_CONTIGUOUS(settings) ^ FIVMR_OM_FRAGMENTED(settings));
    
    if (FIVMR_HFGC(settings)) {
        LOG(1,("Initializing Hybrid-Fragmenting Concurrent-Mark-Region collector."));
        /* verify settings */
        fivmr_assert(FIVMR_SELF_MANAGE_MEM(settings));
        fivmr_assert(FIVMR_OM_FRAGMENTED(settings));
    } else {
        LOG(1,("Initializing Concurrent-Mark-Region collector."));
    }
    
    gc->threadDone=false;
    
    gc->curShaded=FIVMR_GC_SH_MARK1;
    gc->invCurShaded=FIVMR_GC_SH_MARK2;
    gc->zeroCurShaded=0;
    gc->curShadedAlloc=FIVMR_GC_SH_MARK1;
    gc->phase=FIVMR_GCP_IDLE;

    gc->blockTime=0;
    gc->invBlockTime=0;
    gc->gcThreadTime=0;
    gc->slowPathTime=0;
    gc->getPageTime=0;
    gc->getFreeLinesTime=0;
    gc->largeAllocTime=0;
    gc->freeLineSearchTime=0;
    
    /* assertion for firstLive */
    fivmr_assert(FIVMSYS_LOG_PAGE_SIZE<=sizeof(uint16_t)*8);

    LOG(1,("Page size = %" PRIuPTR,FIVMR_PAGE_SIZE));
    LOG(1,("UP bits length = %" PRIuPTR,FIVMR_UP_BITS_LENGTH(settings)));
    
    gc->gcTriggerPages=mogrifyHeapSize(settings,gc->gcTriggerPages);
    gc->maxPagesUsed=mogrifyHeapSize(settings,gc->maxPagesUsed);
    gc->maxMaxPagesUsed=gc->maxPagesUsed;
    
    if (FIVMR_SELF_MANAGE_MEM(settings)) {
        gc->reusePages=10; /* reuse 10 pages at a time */
    } else {
        gc->reqPages=10;
        gc->reusePages=1000;
    }

    LOG(1,("Using page trigger = %" PRIuPTR,gc->gcTriggerPages));
    LOG(1,("Max pages = %" PRIuPTR,gc->maxPagesUsed));
    
    if (FIVMR_SELF_MANAGE_MEM(settings)) {
        gc->memStart=(uintptr_t)fivmr_allocPages(gc->maxPagesUsed,&gc->isZero);
        gc->memEnd=gc->memStart+(gc->maxPagesUsed<<FIVMSYS_LOG_PAGE_SIZE);
        
        LOG(1,("Heap starts at %p, ends at %p",
               gc->memStart,gc->memEnd-1));
    }
    
    for (i=0;i<FIVMR_GC_NUM_GC_SPACES;++i) {
        if (FIVMR_SELF_MANAGE_MEM(settings)) {
            fivmr_PageTable_initFlat(&gc->spaceData[i].pt,
                                     gc->memStart,
                                     gc->maxPagesUsed<<FIVMSYS_LOG_PAGE_SIZE);
        } else {
            fivmr_PageTable_initML(&gc->spaceData[i].pt,
                                   fivmr_Priority_bound(FIVMR_PR_MAX,
                                                        fivmr_VMfromGC(gc)->maxPriority));
        }
	gc->spaceData[i].freeLineHead.prev=NULL;
	gc->spaceData[i].freeLineHead.next=&gc->spaceData[i].freeLineTail;
	gc->spaceData[i].freeLineTail.prev=&gc->spaceData[i].freeLineHead;
	gc->spaceData[i].freeLineTail.next=NULL;
    }
    
    if (FIVMR_SELF_MANAGE_MEM(settings)) {
        gc->nextFreePage=gc->memStart;
    }
    
    if (FIVMR_HFGC(settings)) {
        initSpaces(gc);
    }
    
    fivmr_Lock_init(&gc->gcLock,fivmr_Priority_bound(FIVMR_PR_MAX,
                                                     fivmr_VMfromGC(gc)->maxPriority));
    if (FIVMR_PREDICTABLE_OOME(settings)) {
        fivmr_Lock_init(&gc->requestLock,FIVMR_PR_INHERIT);
    }
    
    fivmr_Lock_init(&gc->notificationLock,FIVMR_PR_INHERIT);
    fivmr_CritSemaphore_init(&gc->triggerSema);
    fivmr_Semaphore_init(&gc->doneSema);
    
    /* use PIP locks for destructor processing since we want to enable the GC's
       priority to change. */
    fivmr_Lock_init(&gc->destructorLock,FIVMR_PR_INHERIT);

    gc->threadPriority=fivmr_ThreadPriority_min(gc->threadPriority,
                                                fivmr_VMfromGC(gc)->maxPriority);
    if (fivmr_VMfromGC(gc)->pool==NULL) {
        gc->thread=fivmr_Thread_spawn(collectorThreadMain,gc,gc->threadPriority);
    } else {
        gc->thread=fivmr_ThreadPool_spawn(fivmr_VMfromGC(gc)->pool,
                                          collectorThreadMain,gc,gc->threadPriority)->thread;
    }
    if (gc->thread == fivmr_ThreadHandle_zero()) {
        fivmr_abort("Could not start GC thread.");
    }

    fivmr_debugMemory();
}

static void assertGCDataClear(fivmr_ThreadState *ts) {
    fivmr_GCSpace i;
    fivmr_assert(ts->gc.queue==NULL);
    fivmr_assert(ts->gc.queueTail==NULL);
    if (!shouldQuit(&ts->vm->gc)) {
        fivmr_assert(ts->gc.requesterNext==NULL);
    }
    for (i=0;i<FIVMR_GC_NUM_GC_SPACES;++i) {
	fivmr_assert(ts->gc.alloc[i].bump==0);
	fivmr_assert(ts->gc.alloc[i].start==0);
	fivmr_assert(ts->gc.alloc[i].size==0);
        fivmr_assert(ts->gc.alloc[i].zero==0);
	fivmr_assert(ts->gc.alloc[i].usedPage==NULL);
	fivmr_assert(ts->gc.alloc[i].freeHead==NULL);
	fivmr_assert(ts->gc.alloc[i].freeTail==NULL);
        fivmr_assert(ts->gc.alloc[i].ssBump==0);
        fivmr_assert(ts->gc.alloc[i].ssEnd==0);
        fivmr_assert(ts->gc.alloc[i].ssSize==0);
    }
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        fivmr_assert(ts->gc.currentArea==&ts->gc.heapMemoryArea);
        fivmr_assert(ts->gc.currentArea->shared==0);
        fivmr_assert(ts->gc.baseStackEntry.area==ts->gc.currentArea);
        fivmr_assert(ts->gc.baseStackEntry.prev==NULL);
        fivmr_assert(ts->gc.baseStackEntry.next==NULL);
        fivmr_assert(ts->gc.scopeStack==&ts->gc.baseStackEntry);
        fivmr_assert(ts->gc.scopeBacking==NULL);
    }
    fivmr_assert(ts->gc.destructorHead==NULL);
    fivmr_assert(ts->gc.destructorTail==NULL);
}

static void markMethod(fivmr_ThreadState *ts,
                       uintptr_t pointer) {
    fivmr_MethodRec *mr;
    fivmr_MachineCode *mc;
    
    mr=fivmr_MachineCode_decodeMethodRec(pointer);
    mc=fivmr_MachineCode_decodeMachineCode(pointer);
    
    mutatorMarkObjNoisy(ts,mr->owner->classObject);
    
    if (mc!=NULL &&
        (mc->flags&(FIVMR_MC_GC_OWNED|FIVMR_MC_GC_MARKED))==FIVMR_MC_GC_OWNED) {
        fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_MARKED,FIVMR_MC_GC_MARKED);
    }
}

/* calls should be protected by the thread's lock. */
static void scanThreadRoots(fivmr_ThreadState *ts) {
    fivmr_VM *vm;
    fivmr_Frame *f;
    fivmr_NativeFrame *nf;
    uintptr_t i;
    fivmr_MemoryAreaStack *ms;
    uintptr_t ptr;
    
    vm=ts->vm;
    
    LOG(2,("Scanning thread roots for Thread %u.",ts->id));
    if (LOGGING(4)) {
        fivmr_ThreadState_dumpStackFor(ts);
    }

    if (FIVMR_BIASED_LOCKING(&ts->vm->settings)) {
        /* mark the object we're trying to unbias.  NOTE: unsure about this;
           it may be preferable to handle this using the same trick that
           we use for VMThreads */
        mutatorMarkObjNoisy(ts,ts->toUnbias);
    }
    
    /* if the thread is STARTING then none of the other stuff in this method is
       relevant; moreover, the thread may be initializing those data structures so
       any attempts we make to read them can lead to badness. */
    if (ts->execStatus==FIVMR_TSES_STARTING) {
        return;
    }

    /* stack scan */
    for (f=ts->curF;f!=NULL;f=f->up) {
	fivmr_assert(f!=f->up);
	if (fivmr_likely(f->id!=(uintptr_t)(intptr_t)-1)) {
	    fivmr_DebugRec *dr;
            fivmr_FrameType ft;
            
	    dr=fivmr_DebugRec_lookup(vm,f->id);
            ft=fivmr_DebugRec_getFrameType(&vm->settings,dr);
            
            /* mark reference variables from this stack frame */
            if (fivmr_unlikely((dr->ln_rm_c&FIVMR_DR_FAT))) {
                fivmr_FatDebugData *fdd;
                int32_t j;
                LOG(5,("Scanning fat GC map for id = %p",f->id));
                fdd=fivmr_DebugRec_decodeFatDebugData(dr->ln_rm_c);
                for (j=0;j<fdd->refMapSize;++j) {
                    uint32_t rm=(uint32_t)fdd->refMap[j];
                    uint32_t k;
                    for (k=0;k<32;++k) {
                        if ((rm&(1<<k))) {
                            mutatorMarkObjStackNoisy(
                                ts,
                                fivmr_Frame_getRef(f,ft,j*32+k));
                        }
                    }
                }
            } else {
                uintptr_t refMap;
                uintptr_t k=0;
                refMap=fivmr_DebugRec_decodeThinRefMap(dr->ln_rm_c);
                LOG(5,("Scanning thin GC map for id = %p, refMap = %p",f->id,refMap));
                for (k=0;k<FIVMR_DR_TRM_NUMBITS;++k) {
                    if ((refMap&(1<<k))) {
                        mutatorMarkObjStackNoisy(
                            ts,
                            fivmr_Frame_getRef(f,ft,k));
                    }
                }
            }
            
            /* mark classes corresponding to all methods on this stack frame.  note,
               those classes will then have references to any classes that can be
               referenced from the constant pool of that class. */
            if ((dr->ln_rm_c&FIVMR_DR_INLINED)) {
                fivmr_InlineMethodRec *imr=(fivmr_InlineMethodRec*)(dr->method&~(sizeof(void*)-1));
                for (;;) {
                    markMethod(ts,(uintptr_t)imr->method);
                    if ((imr->ln_c&FIVMR_IMR_INLINED)) {
                        imr=(fivmr_InlineMethodRec*)imr->caller;
                    } else {
                        markMethod(ts,imr->caller);
                        break;
                    }
                }
            } else {
                markMethod(ts,dr->method&~(sizeof(void*)-1));
            }
	}
	fivmr_assert(f!=f->up);
    }

    /* Scopes scan */
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        fivmr_MemoryAreaStack *cur;
        for (cur=ts->gc.scopeStack;cur!=&ts->gc.baseStackEntry;cur=cur->prev) {
            fivmr_MemoryArea *area=cur->area;
            uintptr_t scanPoint;
            if (FIVMR_SCJ_SCOPES(&ts->vm->settings)&&area->shared) {
                /* Continued below */
                break;
            }
            scanPoint=area->start;
            while (scanPoint<area->bump) {
                fivmr_Object obj;
                uintptr_t start;
                obj=scanPoint;
                start=(uintptr_t)fivmr_GCHeader_fromObject(
                    &ts->vm->settings,obj);
                if (*(uintptr_t *)start==0) {
                    scanPoint+=sizeof(uintptr_t);
                } else if (((*(uintptr_t *)start)&FIVMR_GC_MARKBITS_MASK)==
                           FIVMR_GC_SH_MARK1) {
                    /* Raw type */
                    scanPoint+=((*(uintptr_t *)start)&~FIVMR_GC_MARKBITS_MASK);
                } else {
                    fivmr_assert(fivmr_Object_isContiguous(&ts->vm->settings,obj));
                    mutatorScanObject(ts,obj);
                    scanPoint+=fivmr_Object_size(&ts->vm->settings,obj);
                }
            }
        }
        if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
            fivmr_Lock_lock(&ts->vm->gc.gcLock);
            for (;cur!=NULL&&cur->area->shared;cur=cur->prev) {
                if (cur->area->shared&0x2) {
                    continue;
                } else {
                    cur->area->shared=(uintptr_t)ts->vm->gc.areaQueue|0x3;
                    ts->vm->gc.areaQueue=cur->area;
                }
            }
            fivmr_Lock_unlock(&ts->vm->gc.gcLock);
        }
    }

    
    /* mark the misc thread roots */
    for (i=0;i<FIVMR_TS_MAX_ROOTS;++i) {
	mutatorMarkObjStackNoisy(ts,ts->roots[i]);
    }
    
    /* mark the state buffer (this is for patch points and possibly OSR)
       note, this code isn't super-efficient because it doesn't have to be.
       the state buffer should never be large and even if it ever is that
       occurence would be uncommon.  in any case, the size of the state
       buffer will be *way* smaller than the total depth of the stack and
       the complexity of scanning the stack. */
    if (ts->stateBufGCMap!=NULL) {
        for (i=0;i<ts->stateSize;++i) {
            if (fivmr_BitVec_get(ts->stateBufGCMap,i)) {
                if (i<FIVMR_TS_STATE_BUF_LEN) {
                    mutatorMarkObjStackNoisy(ts,ts->stateBuf[i]);
                } else {
                    mutatorMarkObjStackNoisy(ts,ts->stateBufOverflow[i]);
                }
            }
        }
    }

    /* mark the exception */
    /* NOTE: the exception currently cannot be stack allocated... but we're
       doing this out of an abundance of caution */
    mutatorMarkObjStackNoisy(ts,ts->curException);
    
    /* mark handles */
    mutatorMarkHandleNoisy(ts,ts->curExceptionHandle);
    
    for (nf=ts->curNF;nf!=NULL;nf=nf->up) {
	fivmr_Handle *h;
	for (h=nf->hr.head.next;h!=&nf->hr.tail;h=h->next) {
	    mutatorMarkHandleNoisy(ts,h);
	}
    }
    
    /* mark the stack-allocated objects. */
    for (ptr=ts->gc.alloc[FIVMR_GC_SA_SPACE].start;
         ptr<ts->gc.alloc[FIVMR_GC_SA_SPACE].bump;) {
        fivmr_Object obj;
        uintptr_t start;
        obj=ptr;
        start=fivmr_GCHeader_chunkStart(
            &vm->settings,
            fivmr_GCHeader_fromObject(
                &vm->settings,
                obj));
        if (*((uintptr_t*)start)!=0) {
            fivmr_assert(fivmr_Object_isContiguous(&vm->settings,obj));
            mutatorScanObject(ts,obj);
            ptr+=fivmr_Object_size(&vm->settings,obj);
        } else {
            ptr+=sizeof(uintptr_t);
        }
    }

    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        /* mark BackingStoreID objects on the scope stack */
        for (ms=ts->gc.scopeStack;ms!=NULL;ms=ms->prev) {
            /* The heap area does not have a bsid */
            if (ms->area->javaArea) {
                mutatorScanObject(ts,ms->area->javaArea);
            }
        }
    }
}

static void pushShadeValues(fivmr_ThreadState *ts) {
    fivmr_GC *gc;
    gc=&ts->vm->gc;
    
    ts->gc.invCurShaded=gc->invCurShaded>>FIVMR_GC_MARKBITS_SHIFT;
    ts->gc.curShadedAlloc=gc->curShadedAlloc;
    ts->gc.zeroCurShaded=0;
    ts->gc.tracing=fivmr_GC_isTracing(gc->phase);
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        ts->gc.heapMemoryArea.scopeID=gc->curShadedAlloc|(uintptr_t)&ts->vm->gc.heapMemoryArea.scopeID_s;
    }
}

/* calls should be protected by the thread's lock. */
static void commitQueue(fivmr_ThreadState *ts) {
    fivmr_GC *gc;
    gc=&ts->vm->gc;
    
    if (ts->gc.queue!=NULL) {
	fivmr_Lock_lock(&gc->gcLock);
	LOG(3,("Thread %u committing queue with head %p and tail %p, old global "
	       "head is %p",
	       ts->id,ts->gc.queue,ts->gc.queueTail,gc->globalQueue));
	fivmr_GCHeader_setNext(ts->gc.queueTail,
			       gc->globalQueue);
	gc->globalQueue=ts->gc.queue;
	fivmr_Lock_unlock(&gc->gcLock);
	ts->gc.queue=NULL;
	ts->gc.queueTail=NULL;
    }
}

/* calls should be protected by thread's lock. */
static void commitDestructors(fivmr_ThreadState *ts) {
    fivmr_GC *gc;
    gc=&ts->vm->gc;
    
    if (ts->gc.destructorHead!=NULL) {
	fivmr_assert(ts->gc.destructorTail!=NULL);
        
	fivmr_Lock_lock(&gc->destructorLock);
	LOG(3,("Thread %u committing destructors with head %p and tail %p, old "
	       "global head is %p",
	       ts->id,ts->gc.destructorHead,ts->gc.destructorTail,
	       gc->destructorHead));
	ts->gc.destructorTail->next=gc->destructorHead;
	gc->destructorHead=ts->gc.destructorHead;
	fivmr_Lock_unlock(&gc->destructorLock);
	ts->gc.destructorHead=NULL;
	ts->gc.destructorTail=NULL;
    }
    fivmr_assert(ts->gc.destructorHead==NULL);
    fivmr_assert(ts->gc.destructorTail==NULL);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_handleHandshake)(fivmr_ThreadState *ts) {
    if ((ts->execFlags&FIVMR_TSEF_GC_MISC)!=0) {
	LOG(2,("Thread %u pushing new shade values.",ts->id));
	pushShadeValues(ts);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_SCAN_THREAD_ROOTS)!=0) {
	LOG(2,("Thread %u scanning thread roots.",ts->id));
	scanThreadRoots(ts);
	ts->execFlags&=~FIVMR_TSEF_SCAN_THREAD_ROOTS;
    }
    
    if ((ts->execFlags&FIVMR_TSEF_GC_MISC)!=0) {
	LOG(2,("Thread %u committing queue.",ts->id));
	commitQueue(ts);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_COMMIT_DESTRUCTORS)!=0) {
	LOG(2,("Thread %u committing destructors.",ts->id));
	commitDestructors(ts);
	ts->execFlags&=~FIVMR_TSEF_COMMIT_DESTRUCTORS;
    }

    if ((ts->execFlags&FIVMR_TSEF_RELINQUISH_SPACE)!=0) {
        fivmr_assert(FIVMR_HFGC(&ts->vm->settings));
        relinquishSpacesContext(ts);
        ts->execFlags&=~FIVMR_TSEF_RELINQUISH_SPACE;
    }

    ts->execFlags&=~FIVMR_TSEF_GC_MISC;
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_clear)(fivmr_ThreadState *ts) {
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        ts->gc.currentArea=&ts->gc.heapMemoryArea;
        ts->gc.baseStackEntry.area=ts->gc.currentArea;
        ts->gc.baseStackEntry.prev=NULL;
        ts->gc.baseStackEntry.next=NULL;
        ts->gc.scopeStack=&ts->gc.baseStackEntry;
        ts->gc.scopeBacking=NULL;
    }

    /* all the rest of this stuff should have been cleared by the thread
       tear down. */
    assertGCDataClear(ts);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_startThread)(fivmr_ThreadState *ts) {
    assertGCDataClear(ts);
    pushShadeValues(ts);
    
    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "starting thread at %u:%u",2,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff));
    }
    
    /* anything else? */
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_commitThread)(fivmr_ThreadState *ts) {
    LOG(2,("Thread %u committing; committing its queue and relinquishing its page.",ts->id));

    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "committing thread at %u:%u",2,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff));
    }
    
    commitQueue(ts);
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        while (ts->gc.scopeStack->prev) {
            fivmr_MemoryArea *area=ts->gc.scopeStack->area;
            fivmr_MemoryArea_pop(ts, ts->vm, area);
            fivmr_MemoryArea_free(ts, area);
        }
        if (ts->gc.scopeBacking) {
            fivmr_ScopeBacking_free(ts);
        }
    }
    relinquishAllocationContexts(ts);
    commitDestructors(ts);
    assertGCDataClear(ts);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_asyncCollect)(fivmr_GC *gc) {
    bool shouldTrigger;
    
    if (gc->logGC) {
        fivmr_Log_lockedPrintf("[Thread %u requesting Async GC]\n",
                               fivmr_ThreadState_get(fivmr_VMfromGC(gc))->id);
    }
    LOG(1,("Thread %u requesting GC asynchronously.",
	   fivmr_ThreadState_get(fivmr_VMfromGC(gc))->id));
    fivmr_Lock_lock(&gc->gcLock);
    shouldTrigger=!fivmr_GC_hasBeenTriggered(gc);
    gc->asyncRequested=true;
    fivmr_Lock_unlock(&gc->gcLock);
    
    if (shouldTrigger) {
        fivmr_CritSemaphore_up(&gc->triggerSema);
    }
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_collectFromNative)(fivmr_GC *gc,
                                                        const char *descrIn,
                                                        const char *descrWhat) {
    fivmr_ThreadState *ts;
    fivmr_Nanos before=0;
    bool shouldTrigger;
    
    if (fivmr_Thread_isInterrupt()) {
	LOG(1,("collectFromNative called from an interrupt."));
	return;
    }

    before=fivmr_curTime();

    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "blocking voluntarily on GC at %u:%u",2,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff));
    }
    if (gc->logSyncGC) {
        fivmr_Log_lockedPrintf("[Thread %u requesting Sync GC in %s%s%s]\n",
                               ts->id,descrIn,
                               (descrWhat==NULL?"":", allocating "),
                               (descrWhat==NULL?"":descrWhat));
    }
    LOG(1,("Thread %u requesting GC synchronously.",ts->id));
    fivmr_Lock_lock(&gc->gcLock);
    ts->gc.gcFlags|=FIVMR_GCDF_REQUESTED_GC;
    shouldTrigger=!fivmr_GC_hasBeenTriggered(gc);
    ts->gc.requesterNext=gc->requesterHead;
    gc->requesterHead=ts;
    fivmr_Lock_unlock(&gc->gcLock);
    
    if (shouldTrigger) {
        fivmr_CritSemaphore_up(&gc->triggerSema);
    }
    
    fivmr_Lock_lock(&gc->notificationLock);
    while ((ts->gc.gcFlags&FIVMR_GCDF_REQUESTED_GC) && !shouldQuit(&ts->vm->gc)) {
	fivmr_Lock_wait(&gc->notificationLock);
    }
    fivmr_Lock_unlock(&gc->notificationLock);

    LOG(2,("Thread %u woke up after synchronous GC request.",ts->id));
    
    gc->blockTime+=fivmr_curTime()-before;
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_collectFromJava)(fivmr_GC *gc,
                                                      const char *descrIn,
                                                      const char *descrWhat) {
    fivmr_ThreadState *ts;
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    fivmr_ThreadState_goToNative(ts);
    fivmr_GC_collectFromNative(gc,descrIn,descrWhat);
    fivmr_ThreadState_goToJava(ts);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_claimMachineCode)(fivmr_ThreadState *ts,
                                                       fivmr_MachineCode *mc) {
    fivmr_GC *gc;
    
    gc=&ts->vm->gc;
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    fivmr_assert((mc->flags&FIVMR_MC_GC_OWNED)==0);
    
    fivmr_MachineCode_up(mc);
    
    fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_OWNED,FIVMR_MC_GC_OWNED);
    
    if (fivmr_GC_isCollecting(gc->phase)) {
        fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_MARKED,FIVMR_MC_GC_MARKED);
    } else {
        fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_MARKED,0);
    }
    
    fivmr_Lock_lock(&gc->gcLock);
    mc->next=gc->machineCodeHead;
    gc->machineCodeHead=mc;
    fivmr_Lock_unlock(&gc->gcLock);
}

bool FIVMR_CONCAT(FIVMBUILD_GC_NAME,_getNextDestructor)(fivmr_GC *gc,
                                                        fivmr_Handle *objCell,
                                                        bool wait) {
    fivmr_ThreadState *ts;
    
    if (!FIVMR_FINALIZATION_SUPPORTED(&fivmr_VMfromGC(gc)->settings)) {
        return false;
    }
    
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    
    for (;;) {
	/* go to native so that we don't stall GC while waiting for destructors.
	   note that even acquiring the destructor lock may block, so we need to
	   be careful. */
	fivmr_ThreadState_goToNative(ts);
	
	fivmr_Lock_lock(&gc->destructorLock);

	if (gc->destructorsToRun==NULL && !wait) {
	    fivmr_Lock_unlock(&gc->destructorLock);
	    fivmr_ThreadState_goToJava(ts);
	    return false;
	}
	
	while (gc->destructorsToRun==NULL) {
            fivmr_Lock *lock=&gc->destructorLock;
            fivmr_assert(wait);
            fivmr_ThreadState_checkExitHoldingLocks(ts,1,&lock);
            fivmr_Lock_wait(&gc->destructorLock);
	}
        
        fivmr_assert(gc->destructorsToRun!=NULL);

	/* this is tricky ... we cannot block on going to Java while holding the
	   destructor lock, as this could stall the GC.  so, we optimistically
	   try to go to Java the fast way, and if we fail, then we simply
	   retry the whole thing. */
	
	if (fivmr_ThreadState_tryGoToJava(ts)) {
	    /* went into Java the easy way, we can now examine the object and
	       remove it from the list.  note that this needs to be dealt with
	       care because as soon as the destructor is removed from the list
	       (and the destructor lock is released), the destructor itself
	       may die, and the object will die at the next safepoint unless
	       it is rooted somewhere. */
            LOG(2,("dequeueing finalizable %p",gc->destructorsToRun->object));
	    fivmr_objectArrayStore(ts,objCell->obj,0,gc->destructorsToRun->object,0);
	    gc->destructorsToRun=gc->destructorsToRun->next;
	    fivmr_Lock_unlock(&gc->destructorLock);
            if (FIVMR_ASSERTS_ON) {
                fivmr_assertNoException(ts,"in fivmr_GC_getNextDestructor()");
            }
	    return true;
	} else {
	    /* need to take slow path to go into Java.  first release the
	       destructor lock, then go to Java, and then reloop and try again. */
	    fivmr_Lock_unlock(&gc->destructorLock);
	    fivmr_ThreadState_goToJava(ts);
	}
    }
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_resetStats)(fivmr_GC *gc) {
    fivmr_Lock_lock(&gc->gcLock);
    gc->iterationCount=0;
    gc->blockedIterationCount=0;
    gc->blockTime=0;
    gc->invBlockTime=0;
    gc->gcThreadTime=0;
    gc->slowPathTime=0;
    gc->getPageTime=0;
    gc->getFreeLinesTime=0;
    gc->largeAllocTime=0;
    gc->freeLineSearchTime=0;
    fivmr_Lock_unlock(&gc->gcLock);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_report)(fivmr_GC *gc,
                                             const char *name) {
    fivmr_Lock_lock(&gc->gcLock);
    fivmr_Log_lockedPrintf(
        "GC %s report:\n"
        "                   Number of iterations completed: %" PRIu64 "\n"
        "       Number of synchronous iterations completed: %" PRIu64 "\n"
        "             Number of trace iterations completed: %" PRIu64 "\n"
        "                Time threads spent blocking on GC: %" PRIu64 " ns\n"
        "  Time threads spent blocking involuntarily on GC: %" PRIu64 " ns\n"
        "               Time that the GC thread was active: %" PRIu64 " ns\n"
        "        Total time spent in allocation slow paths: %" PRIu64 " ns\n"
        "                Total time spent allocating pages: %" PRIu64 " ns\n"
        "              Total time spent getting free lines: %" PRIu64 " ns\n"
        "        Total time spent allocating large objects: %" PRIu64 " ns\n"
        "            Total time spent searching free lines: %" PRIu64 " ns\n",
        name,
        gc->iterationCount,
        gc->blockedIterationCount,
        gc->traceIterationCount,
        gc->blockTime,
        gc->invBlockTime,
        gc->gcThreadTime,
        gc->slowPathTime,
        gc->getPageTime,
        gc->getFreeLinesTime,
        gc->largeAllocTime,
        gc->freeLineSearchTime);
    fivmr_Lock_unlock(&gc->gcLock);
}

/* FIXME: these computations are wrong!   (or maybe not.  not sure.  maybe I fixed
   them.) */

int64_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_freeMemory)(fivmr_GC *gc) {
    int64_t result;
    uintptr_t free=gc->numFreePages;
    /* printf("free pages = %p\n",free); */
    result=(free<<FIVMSYS_LOG_PAGE_SIZE);
    /* printf("returning = %u\n",(unsigned)result); */
    fivmr_assert(result>=0);
    return result;
}

int64_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_totalMemory)(fivmr_GC *gc) {
    int64_t result;
    uintptr_t used=gc->numPagesUsed;
    uintptr_t free=gc->numFreePages;
    /* printf("used + free = %p + %p = %p\n",used,free,used+free); */
    result=((used+free)<<FIVMSYS_LOG_PAGE_SIZE);
    /* printf("returning = %u\n",(unsigned)result); */
    fivmr_assert(result>=0);
    return result;
}

int64_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_maxMemory)(fivmr_GC *gc) {
    return gc->maxPagesUsed<<FIVMSYS_LOG_PAGE_SIZE;
}

int64_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_numIterationsCompleted)(fivmr_GC *gc) {
    int64_t result;
    fivmr_Lock_lock(&gc->gcLock);
    result=(int64_t)gc->iterationCount;
    fivmr_Lock_unlock(&gc->gcLock);
    return result;
}

bool FIVMR_CONCAT(FIVMBUILD_GC_NAME,_setMaxHeap)(fivmr_GC *gc,
                                                 int64_t bytes) {
    bool result;
    uintptr_t pages;
    
    pages=mogrifyHeapSize(&fivmr_VMfromGC(gc)->settings,
                          bytes/FIVMR_PAGE_SIZE);
    
    fivmr_Lock_lock(&gc->gcLock);
    if (pages < (uintptr_t)gc->numPagesUsed || pages > gc->maxMaxPagesUsed) {
        result=false;
    } else {
        gc->maxPagesUsed = pages;
        result=true;
    }
    fivmr_Lock_unlock(&gc->gcLock);
    
    return result;
}

bool FIVMR_CONCAT(FIVMBUILD_GC_NAME,_setTrigger)(fivmr_GC *gc,
                                                 int64_t bytes) {
    uintptr_t pages;
    
    pages=mogrifyHeapSize(&fivmr_VMfromGC(gc)->settings,
                          bytes/FIVMR_PAGE_SIZE);
    
    if (pages==0) {
        return false;
    }
    
    fivmr_Lock_lock(&gc->gcLock);
    gc->gcTriggerPages=pages;
    fivmr_GC_asyncCollect(gc);
    fivmr_Lock_unlock(&gc->gcLock);
    
    return true;
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_setPriority)(fivmr_GC *gc,
                                                  fivmr_ThreadPriority prio) {
    prio=fivmr_ThreadPriority_min(prio,fivmr_VMfromGC(gc)->maxPriority);
    fivmr_Thread_setPriority(gc->thread,gc->threadPriority=prio);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_signalExit)(fivmr_GC *gc) {
    LOG(1,("Signaling GC exit"));
    fivmr_Lock_lockedBroadcast(&gc->notificationLock);
    fivmr_Lock_lockedBroadcast(&gc->destructorLock);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_shutdown)(fivmr_GC *gc) {
    LOG(1,("Telling GC thread to die."));
    
    fivmr_debugMemory();

    fivmr_Lock_lock(&fivmr_VMfromGC(gc)->lock);
    /* check if the GC thread is already done; if it's already done then
       we don't have to tell it anything and anyway gc->thread will be NULL */
    if (!gc->threadDone) {
        fivmr_Thread_setPriority(gc->thread,fivmr_VMfromGC(gc)->maxPriority);
        fivmr_CritSemaphore_up(&gc->triggerSema);
    }
    fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->lock);
    
    /* await death (might have already happened) */
    fivmr_Semaphore_down(&gc->doneSema);
    fivmr_assert(gc->threadDone);
    LOG(1,("GC thread has died!"));

    fivmr_debugMemory();

    if (FIVMR_HFGC(&fivmr_VMfromGC(gc)->settings)) {
        freeSpaces(gc);
    }

    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        fivmr_freePages((void*)gc->memStart,gc->maxPagesUsed);
    } else {
        fivmr_LargeObjectHeader *cur;

        /* free the freelist */
        while (gc->freePageHead!=NULL) {
            fivmr_FreePage *next=gc->freePageHead->next;
            fivmr_freePage(gc->freePageHead);
            gc->freePageHead=next;
        }
        
        /* free any other pages known to the GC (i.e. occupied non-free ones).
           this includes pages that had been used by the mutator since those
           would have been relinquished in the mutator commit that happened
           during exit. */
        fivmr_PageTable_freeNonZeroPages(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt);
        
        /* free large objects */
        cur=gc->largeObjectHead;
        while (cur!=NULL) {
            fivmr_LargeObjectHeader *next=cur->next;
            free(cur);
            cur=next;
        }
    }

    fivmr_PageTable_free(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt);
    
    fivmr_Lock_destroy(&gc->gcLock);
    if (FIVMR_PREDICTABLE_OOME(&fivmr_VMfromGC(gc)->settings)) {
        fivmr_Lock_destroy(&gc->requestLock);
    }
    fivmr_Lock_destroy(&gc->notificationLock);
    fivmr_CritSemaphore_destroy(&gc->triggerSema);
    fivmr_Semaphore_destroy(&gc->doneSema);
    fivmr_Lock_destroy(&gc->destructorLock);
    
    fivmr_debugMemory();

    /* FIXME is that it?  or do we have some other allocation as well? */
}

#endif
