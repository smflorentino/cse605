#include "fivmr.h"

void fivmr_MemoryAreas_init(fivmr_GC *gc) {
    gc->immortalMemoryArea.scopeID=
        ((uintptr_t)&gc->immortalMemoryArea>>2)|FIVMR_GC_MARKBITS_MASK;
    gc->immortalMemoryArea.scopeID_s.word=
        (uintptr_t)-1;
    gc->immortalMemoryArea.shared=true;
    if (gc->immortalMem) {
        void *mem=fivmr_malloc(gc->immortalMem);
        gc->immortalMemoryArea.bump=gc->immortalMemoryArea.start=
            (uintptr_t)mem+FIVMR_ALLOC_OFFSET(&fivmr_VMfromGC(gc)->settings);
        gc->immortalMemoryArea.size=gc->immortalMem;
    }
    if (FIVMR_RTSJ_SCOPES(&fivmr_VMfromGC(gc)->settings)) {
        gc->heapMemoryArea.scopeID_s.word=
            (uintptr_t)-1;
        fivmr_Lock_init(&gc->sharedAreasLock, FIVMR_PR_INHERIT);
    }
}

static int fivmr_MemoryArea_depth(fivmr_MemoryArea *area) {
    fivmr_MemoryArea *cur;
    int i;
    for (cur=area->parent, i=1; cur; cur=cur->parent, i++) {
        // empty
    }

    return i;
}

static void doSetArea(fivmr_ThreadState *ts,
                      fivmr_MemoryArea *prevArea,
                      fivmr_MemoryArea *newArea)
{
    ts->gc.currentArea=newArea;
    fivmr_assert(newArea);
    if (newArea==prevArea)
        return;
    if (newArea!=&ts->gc.heapMemoryArea)
        fivmr_assert(newArea->scopeID);
    if (!prevArea->shared) {
        prevArea->bump=ts->gc.alloc[0].bump;
        prevArea->start=ts->gc.alloc[0].start;
        prevArea->size=ts->gc.alloc[0].size;
    }
    ts->gc.alloc[0].bump=newArea->bump;
    ts->gc.alloc[0].start=newArea->start;
    ts->gc.alloc[0].size=newArea->size;
}

static fivmr_MemoryArea *normalizeArea(fivmr_ThreadState *ts,
                                       fivmr_MemoryArea *area) {
    /* Take care of the diverted heap area */
    if (area == &ts->vm->gc.heapMemoryArea) {
        return &ts->gc.heapMemoryArea;
    } else {
        return area;
    }
}

static fivmr_MemoryArea *normalizeParentage(fivmr_ThreadState *ts,
                                            fivmr_MemoryArea *area)
{
    if (area->parent==NULL) {
        return NULL;
    } else if (area->parent==&ts->gc.heapMemoryArea) {
        return &ts->vm->gc.immortalMemoryArea;
    } else {
        return area->parent;
    }
}

uintptr_t fivmr_MemoryArea_alloc(fivmr_ThreadState *ts, int64_t size,
                                 int32_t shared, fivmr_Object name)
{
    fivmr_MemoryAreaStack *ms;
    fivmr_MemoryArea *area;
    fivmr_TypeData *td;
    uintptr_t newtop;
    int64_t totalsize;

    size = (size+FIVMSYS_PTRSIZE-1)&(~(uintptr_t)(FIVMSYS_PTRSIZE-1));
    totalsize = size + sizeof(fivmr_MemoryAreaStack) + sizeof(fivmr_MemoryArea);

    if (FIVMR_RTSJ_SCOPES(&ts->vm->settings)) {
        ms=fivmr_calloc(1,totalsize);
        fivmr_assert(ms!=NULL);
        area=(fivmr_MemoryArea *)(ms+1);
        area->size=size;
        area->shared=shared?1:0;
        area->start=area->bump=(uintptr_t)(area+1)
            +FIVMR_ALLOC_OFFSET(&ts->vm->settings);
        return (uintptr_t)area;
    }

    if (!ts->gc.scopeBacking) {
        LOG(3,("Thread does not have a backing store."));
        return 0;
    }
    /* NB: This must point to the fivmr_MemoryArea created below */
    fivmr_FlowLog_log_fat(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                          FIVMR_FLOWLOG_SUBTYPE_ALLOC_SCOPE, totalsize,
                          ts->gc.scopeBacking->top
                          + sizeof(fivmr_MemoryAreaStack));
    newtop = ts->gc.scopeBacking->top + totalsize;
    if (FIVMR_SCJ(&ts->vm->settings)) {
        /* This should really be memoized somewhere */
        td=fivmr_StaticTypeContext_find(&ts->vm->baseContexts[0]->st,
                                        "Ledu/purdue/scj/BackingStoreID;");
        fivmr_assert(td);
        newtop += td->size;
    }
    if (newtop - (uintptr_t)&ts->gc.scopeBacking->start
        > ts->gc.scopeBacking->size) {
        LOG(3,("Backing store size exhausted."));
        return 0;
    }
    ms = (fivmr_MemoryAreaStack *)ts->gc.scopeBacking->top;
    area = (fivmr_MemoryArea *)(ms + 1);
    bzero((void *)ts->gc.scopeBacking->top,newtop-ts->gc.scopeBacking->top);
    ts->gc.scopeBacking->top = newtop;
    area->size=size;
    area->shared=shared?1:0;
    area->bump=area->start=(uintptr_t)(area+1)
        +FIVMR_ALLOC_OFFSET(&ts->vm->settings);
    area->top=ts->gc.scopeBacking->top;
    if (FIVMR_SCJ(&ts->vm->settings)) {
        fivmr_MemoryArea *curArea=ts->gc.currentArea;
        area->scopeID=(uintptr_t)-1;
        doSetArea(ts,ts->gc.currentArea,area);
        area->javaArea=fivmr_BackingStoreID_create(ts,name);
        doSetArea(ts,area,curArea);
        area->scopeID=0;
        /* Fix up the start to push the BackingStoreID "out" of the area */
        area->start=area->bump;
    }
    return (uintptr_t)area;
}

void fivmr_MemoryArea_free(fivmr_ThreadState *ts, fivmr_MemoryArea *area) {
    fivmr_MemoryAreaStack *ms=((fivmr_MemoryAreaStack *)area)-1;
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_FREE_SCOPE, (uintptr_t)area);
    if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
        fivmr_assert(ts->gc.scopeBacking->top==ms->area->top);
        ts->gc.scopeBacking->top=(uintptr_t)ms;
    } else if (FIVMR_RTSJ_SCOPES(&ts->vm->settings)) {
        fivmr_free(ms);
    }

}

/* This code is reentrant when called for differing areas
 * simultaneously, but the calling code must ensure that it is never
 * called more than once for the *same area* at the same time. */
void fivmr_MemoryArea_push(fivmr_ThreadState *ts,
                           fivmr_MemoryArea *area)
{
    fivmr_MemoryAreaStack *ms=((fivmr_MemoryAreaStack *)area)-1;
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_PUSH, (uintptr_t)area);
    if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
        /* Enforce a linear stack -- no cactus stacks! */
        fivmr_assert(ts->gc.scopeStack->area==ts->gc.currentArea);
        fivmr_assert(!area->parent);
        ms->area=area;
        ms->prev=ts->gc.scopeStack;
        ts->gc.scopeStack=ms;
        area->parent=ts->gc.currentArea;
    } else if (FIVMR_RTSJ_SCOPES(&ts->vm->settings)) {
        fivmr_MemoryArea *parent=normalizeParentage(ts, area);
        fivmr_MemoryArea *cur=normalizeArea(ts, ts->gc.currentArea);
        fivmr_assert(!parent);
        ms->area=area;
        ms->next=NULL;
        area->parent=cur;
        area->scopeID_s.word=((((uintptr_t)-1)-fivmr_MemoryArea_depth(area))<<1)&~FIVMR_GC_MARKBITS_MASK|FIVMR_SCOPEID_SCOPE;
        area->scopeID=((uintptr_t)&area->scopeID_s)>>2;
        if (!FIVMR_NOGC(&ts->vm->settings)&&area->shared) {
            fivmr_Lock_lock(&ts->vm->gc.sharedAreasLock);
            while (ms->flags&FIVMR_MEMORYAREASTACK_GCINPROGRESS) {
                ms->flags|=FIVMR_MEMORYAREASTACK_WAITING;
                fivmr_Lock_wait(&ts->vm->gc.sharedAreasLock);
            }
            ms->flags&=~FIVMR_MEMORYAREASTACK_WAITING;
            ms->prev=ts->vm->gc.sharedAreas;
            if (ts->vm->gc.sharedAreas) {
                ts->vm->gc.sharedAreas->next=ms;
            }
            ts->vm->gc.sharedAreas=ms;
            fivmr_Lock_unlock(&ts->vm->gc.sharedAreasLock);
        } else if (!FIVMR_NOGC(&ts->vm->settings)) {
            /* Private scope */
            ms->prev=ts->gc.scopeStack;
            ts->gc.scopeStack->next=ms;
            ts->gc.scopeStack=ms;
        }
    }
}

/* This has the same reentrancy requirements as _push.  Additionally,
 * it frees the area for SCJ, but not RTSJ-style scopes. */
void fivmr_MemoryArea_pop(fivmr_ThreadState *ts, fivmr_VM *vm,
                          fivmr_MemoryArea *area)
{
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_POP, (uintptr_t)area);
    if(FIVMR_SCJ_SCOPES(&vm->settings)) {
        fivmr_MemoryAreaStack *ms=ts->gc.scopeStack;
        fivmr_assert(ms);
        if (area==NULL) {
            area=ms->area;
        } else {
            fivmr_assert(ms->area==area);
        }
        fivmr_assert(ms!=&ts->gc.baseStackEntry);
        fivmr_assert(ms->area->scopeID==0);
        ts->gc.scopeStack=ms->prev;
        area->parent=NULL;
    } else if (FIVMR_RTSJ_SCOPES(&vm->settings)) {
        // FIXME: finalizers
        fivmr_MemoryAreaStack *ms=((fivmr_MemoryAreaStack *)area)-1;
        if (ts) {
            fivmr_assert(area!=&ts->gc.heapMemoryArea&&
                         area!=&vm->gc.immortalMemoryArea);
            fivmr_assert(area!=ts->gc.currentArea);
        }
        /* If this first stanza is executed, it might be executed
         * with !ts.  In that case, the lock is already locked,
         * and we know the area is shared... */
        if (!FIVMR_NOGC(&vm->settings)&&area->shared) {
            fivmr_Lock_lock(&vm->gc.sharedAreasLock);
            if (ms->flags&FIVMR_MEMORYAREASTACK_GCINPROGRESS) {
                ms->flags|=FIVMR_MEMORYAREASTACK_POP;
            } else {
                if (ms->prev) {
                    ms->prev->next=ms->next;
                } else {
                    vm->gc.sharedAreas=ms->next;
                }
                if (ms->next) {
                    ms->next->prev=ms->prev;
                }
                area->bump=area->start;
                area->objList=0;
            }
            fivmr_Lock_unlock(&vm->gc.sharedAreasLock);
        } else if (!FIVMR_NOGC(&vm->settings)) {
            /* Private area */
            ms->prev->next=ms->next;
            if (ms->next) {
                ms->next->prev=ms->prev;
            } else {
                ts->gc.scopeStack=ms->prev;
            }
        }
    }
}

void fivmr_MemoryArea_enter(fivmr_ThreadState *ts, fivmr_MemoryArea *area,
                            fivmr_Object logic)
{
    fivmr_ScopeID scope={ (uintptr_t)area | FIVMR_SCOPEID_SCOPE };
    fivmr_MemoryArea *prevArea=ts->gc.currentArea;
    if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
        if (!area->parent) {
            fivmr_MemoryArea_push(ts,area);
        }
        fivmr_assert(ts->gc.currentArea==area->parent);
        /* Should be implied by the above */
        fivmr_assert(area->scopeID==0);
        area->scopeID=((uintptr_t)&scope)>>2;
    }
    LOG(3, ("Entering memory area %p in thread %d", area, ts->id));
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_ENTER, (uintptr_t)area);
    doSetArea(ts,prevArea,area);
    fivmr_fence();
    fivmr_MemoryArea_doRun(ts, (uintptr_t)area, logic);
    fivmr_fence();
    LOG(3, ("Exiting memory area %p in thread %d", area, ts->id));
    if (ts->gc.currentArea==area) {
        fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                          FIVMR_FLOWLOG_SUBTYPE_ENTER, (uintptr_t)prevArea);
        doSetArea(ts,area,prevArea);
    }
    if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
        area->scopeID=0;
        area->bump=area->start;
    }
}

uintptr_t fivmr_MemoryArea_setCurrentArea(fivmr_ThreadState *ts,
                                          fivmr_MemoryArea *area)
{
    uintptr_t prev;
    fivmr_assert(area);
    area=normalizeArea(ts,area);
    fivmr_assert(area==&ts->gc.heapMemoryArea||area->scopeID);
    prev=(uintptr_t)ts->gc.currentArea;
    LOG(3, ("Setting current memory area to %p in thread %d", area, ts->id));
    doSetArea(ts,ts->gc.currentArea,area);
    return prev;
}

void fivmr_ScopeBacking_alloc(fivmr_ThreadState *ts, uintptr_t size)
{
    fivmr_assert(ts->gc.scopeBacking==NULL);
    fivmr_assert(size);
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_ALLOC_BACKING, size);
    ts->gc.scopeBacking=fivmr_malloc(sizeof(fivmr_ScopeBacking)+size);
    ts->gc.scopeBacking->size=size;
    ts->gc.scopeBacking->top=(uintptr_t)&ts->gc.scopeBacking->start;
}

void fivmr_ScopeBacking_free(fivmr_ThreadState *ts)
{
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_FREE_BACKING, 0);
    fivmr_assert(ts->gc.scopeBacking!=NULL);
    fivmr_assert(ts->gc.scopeStack==&ts->gc.baseStackEntry);
    fivmr_free(ts->gc.scopeBacking);
    ts->gc.scopeBacking=NULL;
}

int64_t fivmr_MemoryArea_consumed(fivmr_ThreadState *ts,
                                  fivmr_MemoryArea *area) {
    if (area==&ts->gc.heapMemoryArea) {
        int64_t result=fivmr_GC_totalMemory(&ts->vm->gc);
        LOG(3,("Requested consumed memory for heap memory (%p) in Thread #%u, "
               "got %p",area,ts->id,(uintptr_t)result));
        return result;
    } else if (area==ts->gc.currentArea &&
               !area->shared) {
        uintptr_t result=
            ts->gc.alloc[FIVMR_GC_OBJ_SPACE].bump
            - ts->gc.alloc[FIVMR_GC_OBJ_SPACE].start;
        LOG(3,("Requested consumed memory for private current memory "
               "area (%p) in Thread #%u, got %p",area,ts->id,(uintptr_t)result));
        return result;
    } else {
        uintptr_t result=area->bump-area->start;
        LOG(3,("Requested consumed memory for shared or non-current "
               "memory area (%p) in Thread #%u, got %p",
               area,ts->id,(uintptr_t)result));
        return result;
    }
}


