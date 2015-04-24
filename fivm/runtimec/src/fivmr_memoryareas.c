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

/* Initialize the tracking structures for a Memory Area */
static void fivmr_MemoryArea_setupUM(uintptr_t start, int64_t size)
{
    /* Allocate the first block */
    //Create struct to memcpy from
    fivmr_um_node first;
    /* Zero out the everything, including the zero region*/
    memset(&(first), 0, sizeof(fivmr_um_node));
    //Get the block size
    int blockSize = sizeof(fivmr_um_node);
    //Copy into UM region
    memcpy((void*) start,&first,blockSize);
    //Set current node 
    fivmr_um_node* cur = (fivmr_um_node*) start;
    // DEBUG(DB_MEMAREA, ("Block created."));
    //Set next place to copy into
    uintptr_t nextBlock = start + blockSize;
    //Continue until we fill up UM region
    while(nextBlock < (start + size)) {
        //memcpy  into UM region
        memcpy((void*) nextBlock,&first,blockSize);
        //set the next 
        cur->next = (fivmr_um_node *) nextBlock;
        //move the head to the new struct we just copied
        cur = cur->next;
        //set destination to next block 
        nextBlock += blockSize;
        // DEBUG(DB_MEMAREA, ("Block created."));
    }
    //Make sure last block has a NULL next pointer.
    cur->next = NULL;
}

uintptr_t fivmr_MemoryArea_alloc(fivmr_ThreadState *ts, int64_t size,
                                 int32_t shared, fivmr_Object name, int64_t unManagedSize)
{
    fivmr_MemoryAreaStack *ms;
    fivmr_MemoryArea *area;
    fivmr_TypeData *td;
    uintptr_t newtop;
    int64_t totalsize;

    //Allocate for 4 extra pointers in fivmr_MemoryArea
    size += 16;

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
        //TODO unmanaged memory here ...
        return (uintptr_t)area;
    }

    if (!ts->gc.scopeBacking) {
        LOG(3,("Thread does not have a backing store."));
        DEBUG(DB_MEMAREA,("Thread does not have a backing store"));
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
    /* Old Bump is where the UM Region begins - this points to our first free node */
    uintptr_t oldBump = area->bump;
    /* Move the bump up so we can store our UM Region */
    area->bump += unManagedSize;
    /* Scoped Memory now starts outside of UM Region */
    area->new_start = area->bump;
    /* Create our tracking structures */
    fivmr_MemoryArea_setupUM(oldBump, unManagedSize);
    area->free_head = (fivmr_um_node*) oldBump;
    //Save the size for later
    area->um_size = unManagedSize;
    /* Zero out our fields */
    area->um_consumed = 0;
    area->fr_head = NULL;
    area->nfr_head = NULL;

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

int64_t fivmr_MemoryArea_consumedUnmanaged(fivmr_ThreadState *ts, fivmr_MemoryArea *area)
{
    //TODO throw something here
    fivmr_assert(area!=&ts->gc.heapMemoryArea);
        // if (area==&ts->gc.heapMemoryArea) {
        // int64_t result=fivmr_GC_totalMemory(&ts->vm->gc);
        // LOG(3,("Requested consumed memory for heap memory (%p) in Thread #%u, "
        //        "got %p",area,ts->id,(uintptr_t)result));
        // return result;
    if (area==ts->gc.currentArea &&
               !area->shared) {
        uintptr_t result=area->um_consumed;
        LOG(3,("Requested consumed memory for private current memory "
               "area (%p) in Thread #%u, got %p",area,ts->id,(uintptr_t)result));
        return result;
    } else {
        uintptr_t result=area->um_consumed;
        LOG(3,("Requested consumed memory for shared or non-current "
               "memory area (%p) in Thread #%u, got %p",
               area,ts->id,(uintptr_t)result));
        return result;
    }
}

/* Find the index of an available primitive slot in its bit vector.
 * We use the first 6 bits to designate what's available:
 * 000000 = all free
 * 000001 = slot 0 in use, slots 1-5 
 * 000011 = slots 0 and 1 in use, slots 2-5 free
 * 000100 = slot 2 in use, slots 0,1,3-5 free
 * ...
 * 111111 = all slots in use.
 */
static inline int32_t fivmr_MemoryArea_findFreeIndex(int32_t map)
{
    DEBUG(DB_MEMAREA,("Finding free index in map: "));
    // print_binary(map);
    if(map == 0) {
        return 0;
    }
    else if((map & 0x00000002) == 0) {
        return 1;
    }
    else if((map & 0x00000004) == 0) {
        return 2;
    }
    else if((map & 0x00000008) == 0) {
        return 3;
    }
    else if((map & 0x00000010) == 0) {
        return 4;
    }
    else if((map & 0x00000020) == 0) {
        return 5;
    }
    else {
        fivmr_assert(0);
    }
    //Should never get here, per logic in allocatePrimitive
    //TODO throw something
}

static inline fivmr_um_node* fivmr_MemoryArea_getFreeBlock(fivmr_ThreadState *ts, fivmr_MemoryArea *area) {
    //Get current free block:
    fivmr_um_node *block = area->free_head;
    if(block == NULL) {
        //We're out of memory
        //TODO throw OOME
        fivmr_throwOutOfMemoryError_inJava(ts);
        return;
    }
    //Set the head to the next block
    area->free_head = area->free_head->next;
    //De-link the block from the chain
    block->next = NULL;
    DEBUG(DB_MEMAREA, ("Block allocated."));
    area->um_consumed += UNMANAGED_BLOCK_SIZE;
    return block;
}

static inline void fivmr_MemoryArea_freeBlock(fivmr_MemoryArea *area, fivmr_um_node* block) {
    //Zero the block
    memset(block, 0, sizeof(fivmr_um_node));
    //Get the head of the Memory Area:
    fivmr_um_node *head = area->free_head;
    //if this is the last free block, make it the new head
    if(head == NULL) {
        area->free_head = block;
    }
    //else, add it to the head of the list:
    else {
        block->next = area->free_head;
        area->free_head = block;
    }
    area->um_consumed -= UNMANAGED_BLOCK_SIZE;
}

uintptr_t fivmr_MemoryArea_allocatePrimitive(uintptr_t fivmrMemoryArea)
{
    printf("Hello World!!!!!\n");
    //Cast to fivmr_MemoryArea
    fivmr_MemoryArea *area = (fivmr_MemoryArea*) fivmrMemoryArea;
    //If no primitives have been allocated, get one:
    if(area->fr_head == NULL)
    {
        struct fivmr_um_primitive_block *block = (struct fivmr_um_primitive_block*) fivmr_MemoryArea_getFreeBlock(NULL,area);
        area->fr_head = block;
    }
    //If the current head of the primtive list is full (first 6 bits are 111111), move it to the nonfree list:
    if(area->fr_head->map == FULL_MAP) {
        //Move the full block FROM the fr list TO the nfr list.
        //If there are none, make it the head.
        if(area->nfr_head == NULL) {
            //If there are no full blocks, make this one the head of the list
            area->nfr_head = area->fr_head;
            //And update the new head of the fr list:
            area->fr_head = area->fr_head->next;
        }
        //if there are already full primitive blocks, add the full block to that list:
        else {
            //Save the current head of the NFR list:
            struct fivmr_um_primitive_block *old_nfr_head = area->nfr_head;
            //Make the current full block the NFR head:
            area->nfr_head = area->fr_head;
            //Now, update the new head of the FR list:
            area->fr_head = area->fr_head->next;
            //Now, link the block that's full to the rest of the NFR list
            area->nfr_head->next = old_nfr_head;
        }
        //If the FR list is empty, we need to find a free block and add it:
        if(area->fr_head == NULL) {
            struct fivmr_um_primitive_block *block = (struct fivmr_um_primitive_block*) fivmr_MemoryArea_getFreeBlock(NULL,area);
            area->fr_head = block;
        }
    }
    //Now we have a free head node that can store a primitive. Find an index.
    int32_t index = fivmr_MemoryArea_findFreeIndex(area->fr_head->map);
    //Mark that index as used, by setting the bit number of the index to 1:
    area->fr_head->map = ((1 << index) | area->fr_head->map);
    DEBUG(DB_MEMAREA, ("Map is now: "));
    // print_binary(area->fr_head->map);
    //Return the pointer to it:
    return (uintptr_t) &(area->fr_head->storage[index]);
    // int32_t *Int = (int32_t*) malloc(sizeof(int32_t));
    // *Int = (int32_t) val;
    // return (uintptr_t) Int;
}

uintptr_t fivmr_MemoryArea_allocateArray(fivmr_ThreadState *ts, int32_t type, int32_t numElements)
{
    //Exception?
    // fivmr_assert(ts->gc.currentArea != &ts->gc.heapMemoryArea);
    //Get the area:
    fivmr_MemoryArea *area = ts->gc.currentArea;
    fivmr_GCSpaceAlloc *alloc = &(ts->gc.alloc[FIVMR_GC_OBJ_SPACE]);

    //Get a free block for the array header
    fivmr_um_array_header *header = (fivmr_um_array_header*)  fivmr_MemoryArea_getFreeBlock(ts,area);

    //Set the fields in our header:
    header->type = type;
    header->size = numElements;

    // uintptr_t oldBump = area->bump;
    uintptr_t oldBump = alloc->bump;
    //The first 6 elements are inlined, if size allows:
    if(numElements <= 6)
    {
        return (uintptr_t) header;
    }
    //Allocate the spine, by bumping the pointer. The current bump is our spine allocation:
    //Our current implementation has a 12.5% penalty that cannot be reclaimed...
    int64_t spineSize = (numElements / ELEMENTS_PER_BLOCK) * 1;
    //We might need to spill over into another block, if the size is not divisible by 8.
    if(numElements % ELEMENTS_PER_BLOCK != 0)
    {
        spineSize += 1;
    }
    //We need to bump the amount + the amount required to store an array of pointers
    uintptr_t newbump = oldBump + (spineSize * sizeof(void*));
    //See if we have enough space:
    // if(newbump - area->start > area->size) {
    if(newbump - alloc->start > alloc->size) {
        //TODO throw OOME
        fivmr_throwOutOfMemoryError_inJava(ts);
    }
    //If we're good, set the bump:
    // area->bump = newbump;
    alloc->bump = newbump;
    //Allocate the spine:
    int64_t blocksAllocated = 0;
    int64_t blocksNeeded = spineSize;
    header->spine = (fivmr_um_array_block**) oldBump;
    for(blocksAllocated = 0; blocksAllocated < blocksNeeded; blocksAllocated++) {
        fivmr_um_array_block *block = (fivmr_um_array_block*) fivmr_MemoryArea_getFreeBlock(ts,area);
        header->spine[blocksAllocated] = (fivmr_um_array_block*) block;
    }

    return (uintptr_t) header;
}

void fivmr_MemoryArea_freeArray(uintptr_t fivmrMemoryArea, uintptr_t arrayHeader)
{
    fivmr_MemoryArea *area = (fivmr_MemoryArea*) fivmrMemoryArea;
    fivmr_um_array_header *header = (fivmr_um_array_header*) arrayHeader;
    //Free the header, if elements are inlined:
    if(header->size <= 6) {
        fivmr_MemoryArea_freeBlock(area, (fivmr_um_node*) header);
        return;
    }
    //Free the data blocks:
    int64_t blocksAllocated = header->size / ELEMENTS_PER_BLOCK;
    //We might have spilled partially into another block:
    if(header->size % ELEMENTS_PER_BLOCK != 0) {
        blocksAllocated += 1;
    }
    //Lets FREE!
    int64_t blocksFreed = 0;
    for(blocksFreed = 0; blocksFreed < blocksAllocated; blocksFreed++) {
        fivmr_MemoryArea_freeBlock(area, (fivmr_um_node*) header->spine[blocksFreed]);
    }
    //Now free the header.
    fivmr_MemoryArea_freeBlock(area, (fivmr_um_node*) header);
    return;
}

int32_t fivmr_MemoryArea_loadArrayInt(uintptr_t arrayHeader, int32_t index)
{
    fivmr_um_array_header *header = (fivmr_um_array_header*) arrayHeader;
    if(header->size <= 6) {
        return (int32_t) header->elem[index];
    }
    //Get the block from the spine
    fivmr_um_array_block *block = header->spine[index / ELEMENTS_PER_BLOCK];
    //Get the data from the block
    return block->storage[index % ELEMENTS_PER_BLOCK];
}

void fivmr_MemoryArea_storeArrayInt(uintptr_t arrayHeader, int32_t index, int32_t value)
{
    fivmr_um_array_header *header = (fivmr_um_array_header*) arrayHeader;
    if(header->size <= 6) {
        header->elem[index] = value;
        return;
    }
    //Get the block from the spine
    fivmr_um_array_block *block = header->spine[index / ELEMENTS_PER_BLOCK];
    //Set the data from the block
    block->storage[index % ELEMENTS_PER_BLOCK] = value;
}

//Shreyas
void fivmr_MemoryArea_deallocatePrimitive(uintptr_t fivmrMemoryArea, uintptr_t primitiveLoc)
{
    printf("In Deallocation native!\n");
    //Cast the primitiveloc parameter to uintptr_t*
    uintptr_t * primitiveLocation = (uintptr_t *)primitiveLoc;
    //Cast to fivmr_MemoryArea
    fivmr_MemoryArea *areaptr = (fivmr_MemoryArea*) fivmrMemoryArea;
    uintptr_t *result;
    //Subtract MemoryArea's start from it
    int OFFSET = primitiveLocation - (uintptr_t*)areaptr->start;
    //Divide by 64 and take ceiling of it
    OFFSET=ceil(OFFSET/64);
    //Multiply by 64 and add to start of MemoryArea
    result=(OFFSET*64)+(uintptr_t *)areaptr->start;
    //This is the start of the corresponding primitive block
    struct fivmr_um_primitive_block *resultPrimitiveBlock = (struct fivmr_um_primitive_block*) result;
    //Now to find which storage element to zero out
    int PRIMITIVE_INDEX = primitiveLocation - (uintptr_t*)resultPrimitiveBlock;
    PRIMITIVE_INDEX=(PRIMITIVE_INDEX)-2;
    //Zero out the elements from the block pointer by resultPrimitiveBlock
    // printf("%" PRIu64 "\t",resultPrimitiveBlock->storage[PRIMITIVE_INDEX]);

    resultPrimitiveBlock->storage[PRIMITIVE_INDEX]=0;
    //printf("\n%" PRIu64 "\n",resultPrimitiveBlock->storage[PRIMITIVE_INDEX]);    //TODO: remove
    //Change the Bit Vector accordingly
    // index starts from LSB and from 0
    //Save prevous map before mutating....resultPrimitiveBlock-> &= ~(1 << index);
    int prevmap=resultPrimitiveBlock->map;
    resultPrimitiveBlock->map &= ~(1 << PRIMITIVE_INDEX);
    //printf("%d",resultPrimitiveBlock->map);    //TODO: remove
    //Bookkeeping
    //Add this block to the fr_list
    if(prevmap==FULL_MAP)
    {
            //Move the full block FROM the nfr list TO the fr list.
            //If there are none, make it the head.
            if(areaptr->fr_head == NULL) {
                //If there are no half filled blocks, make this one the head of the list
                areaptr->fr_head = (struct fivmr_um_primitive_block * ) resultPrimitiveBlock;
            }
            //if there are already half filled primitive blocks, add this block to that list:
            else {
                //Save the current head of the FR list:
                struct fivmr_um_primitive_block *old_fr_head = areaptr->fr_head;
                //Make the current full block the FR head:
                areaptr->fr_head = resultPrimitiveBlock;
                //Now, link the block that's full to the rest of the NFR list
                areaptr->fr_head->next = old_fr_head;
            }
             //Traverse and remove the nfr block :(
             struct fivmr_um_primitive_block *curptr = areaptr->nfr_head;
             //If theres only one node in the nfr list then do this->nfr_head=NULL
            if(curptr->next==NULL){areaptr->nfr_head=NULL;}
            else{
                //Primitive freeing not in the fr_head node
                while(curptr!=NULL)
                {
                    if(curptr->next->map!=FULL_MAP)
                    {
                        curptr->next=curptr->next->next;
                        break;
                    }
                    curptr=curptr->next;
                }
            }
    }

}
