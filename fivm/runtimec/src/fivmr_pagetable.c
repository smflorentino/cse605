/*
 * fivmr_pagetable.c
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

void fivmr_PageTable_initFlat(fivmr_PageTable *pt,
                              uintptr_t start,
                              uintptr_t size) {
    uintptr_t ptSize;
    
    LOG(2,("Initializing flat page table at %p",pt));
    
    pt->isFlat=true;
    
    LOGptrconst(2,FIVMR_LOG_PT_BITS_PER_PAGE);
    LOGptrconst(2,FIVMSYS_LOG_PAGE_SIZE);
    LOGptrconst(2,FIVMR_PT_BITS_PER_PAGE);
    LOGptrconst(2,FIVMR_PAGE_SIZE);
    
    pt->u.flat.start=start;
    pt->u.flat.size=(size+FIVMR_PAGE_SIZE-1)&~(FIVMR_PAGE_SIZE-1);
    
    ptSize=(((pt->u.flat.size>>FIVMSYS_LOG_PAGE_SIZE)<<FIVMR_LOG_PT_BITS_PER_PAGE)
            +31)/32*sizeof(uint32_t);
    
    LOG(1,("Allocating flat page table with size = %" PRIuPTR,
           ptSize));

    pt->u.flat.table=(uint32_t*)fivmr_mallocAssert(ptSize);
    bzero(pt->u.flat.table,ptSize);
}

void fivmr_PageTable_initML(fivmr_PageTable *pt,
                            fivmr_Priority prio) {
    LOG(2,("Initializing multi-level page table at %p",pt));
    
    LOGptrconst(2,FIVMR_LOG_PT_BITS_PER_PAGE);
    LOGptrconst(2,FIVMSYS_LOG_PAGE_SIZE);
    LOGptrconst(2,FIVMR_PT_BITS_PER_PAGE);
    LOGptrconst(2,FIVMR_PAGE_SIZE);
    LOGptrconst(2,FIVMR_PAGE_CHUNKS_PER_LIST);
#if FIVMSYS_PTRSIZE==4
    LOGptrconst(2,FIVMR_PT_ADDRBITS_PER_CHUNK);
    LOGptrconst(2,FIVMR_PT_ADDRBITS_PER_SPINE);
    LOGptrconst(2,FIVMR_PT_NUM_SPINE_ELE);
    LOGptrconst(2,FIVMR_PT_SPINE_SHIFT);
    LOGptrconst(2,FIVMR_PT_SPINE_MASK);
    LOGptrconst(2,FIVMR_PT_CHUNK_SHIFT);
    LOGptrconst(2,FIVMR_PT_CHUNK_MASK);
#else
    LOGptrconst(2,FIVMR_PT_OUTER_BITS);
    LOGptrconst(2,FIVMR_PT_MIDDLE_BITS);
    LOGptrconst(2,FIVMR_PT_INNER_BITS);
    LOGptrconst(2,FIVMR_PT_NUM_SPINE_ELE);
    LOGptrconst(2,FIVMR_PT_NUM_MIDDLE_ELE);
    LOGptrconst(2,FIVMR_PT_NUM_INNER_BYTES);
    LOGptrconst(2,FIVMR_PT_OUTER_SHIFT);
    LOGptrconst(2,FIVMR_PT_OUTER_MASK);
    LOGptrconst(2,FIVMR_PT_MIDDLE_SHIFT);
    LOGptrconst(2,FIVMR_PT_MIDDLE_MASK);
    LOGptrconst(2,FIVMR_PT_INNER_SHIFT);
    LOGptrconst(2,FIVMR_PT_INNER_MASK);
#endif
    
    fivmr_assert(sizeof(fivmr_PageChunkList)==FIVMR_PAGE_SIZE);

#if FIVMSYS_PTRSIZE==4
    fivmr_assert(FIVMR_PT_CHUNK_SIZE==FIVMR_PAGE_SIZE);
#endif
#if FIVMSYS_PTRSIZE==8
    fivmr_assert(FIVMR_PT_CHUNK_SIZE==FIVMR_PT_NUM_INNER_BYTES);
#endif
    
    fivmr_Lock_init(&pt->u.ml.lock,prio);
    
    pt->u.ml.chunkListHead=(fivmr_PageChunkList*)fivmr_mallocAssert(FIVMR_PAGE_SIZE);
    bzero(pt->u.ml.chunkListHead,FIVMR_PAGE_SIZE);
    
    bzero(pt->u.ml.table,sizeof(pt->u.ml.table));
}

void fivmr_PageTable_free(fivmr_PageTable *pt) {
    if (FIVMR_PT_IS_FLAT(pt)) {
        uintptr_t ptSize;
        ptSize=(((pt->u.flat.size>>FIVMSYS_LOG_PAGE_SIZE)<<FIVMR_LOG_PT_BITS_PER_PAGE)
                +31)/32*sizeof(uint32_t);
        fivmr_free(pt->u.flat.table);
    } else {
        fivmr_PageChunkList *pcl;
        uintptr_t i;
        
        for (pcl=pt->u.ml.chunkListHead;pcl!=NULL;) {
            fivmr_PageChunkList *next;
            
            next=pcl->next;
            
            for (i=0;i<pcl->numChunks;++i) {
                fivmr_free(pcl->chunks[i].chunk);
            }
            fivmr_free(pcl);
            
            pcl=next;
        }
#if FIVMSYS_PTRSIZE==8
        /* FIXME: should be a better way... */
        for (i=0;i<FIVMR_PT_NUM_SPINE_ELE;++i) {
            if (pt->u.ml.table[i]!=NULL) {
                fivmr_free(pt->u.ml.table[i]);
            }
        }
#endif
    }
}

void fivmr_PageTable_freeNonZeroPages(fivmr_PageTable *pt) {
    fivmr_PTIterator iter;
    for (fivmr_PTIterator_init(&iter,pt);
         fivmr_PTIterator_valid(&iter);
         fivmr_PTIterator_next(&iter)) {
        uintptr_t baseAddr;
        uintptr_t wordI;
        uintptr_t wordILimit;
        uint32_t *chunk;
        baseAddr=iter.baseAddress;
        chunk=iter.chunk;
        wordILimit=iter.chunkLength;
        for (wordI=0;wordI<wordILimit;wordI++) {
            uint32_t curWord=chunk[wordI];
            if (curWord) {
                uintptr_t bitI;
                for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                    if (( curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1) )) {
                        uintptr_t curAddr=
                            baseAddr+
                            (((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))+
                              (bitI>>FIVMR_LOG_PT_BITS_PER_PAGE))
                             <<FIVMSYS_LOG_PAGE_SIZE);
                        fivmr_assert(fivmr_PageTable_get(pt,curAddr)!=0);
                        fivmr_assert((curAddr&(FIVMR_PAGE_SIZE-1))==0);
                        fivmr_freePage((void*)curAddr);
                    }
                    curWord>>=FIVMR_PT_BITS_PER_PAGE;
                }
            }
        }
    }
}

void fivmr_PageTable_freePTAndNonZeroPages(fivmr_PageTable *pt) {
    fivmr_PageTable_freeNonZeroPages(pt);
    fivmr_PageTable_free(pt);
}

/* call this only while holding the global lock. */
static void addChunk(fivmr_PageTable *pt,
		     uintptr_t baseAddress,
		     uint32_t *chunk) {
    fivmr_PageChunkList *myList;
    
    /* NB. the fences allow people to scan the chunk lists without locking
       against the page table. */

    if (pt->u.ml.chunkListHead->numChunks==FIVMR_PAGE_CHUNKS_PER_LIST) {
	fivmr_PageChunkList *newList=(fivmr_PageChunkList*)fivmr_mallocAssert(FIVMR_PAGE_SIZE);
        bzero(newList,FIVMR_PAGE_SIZE);
	newList->next=pt->u.ml.chunkListHead;
	fivmr_fence();
	pt->u.ml.chunkListHead=newList;
    }
    
    myList=pt->u.ml.chunkListHead;
    
    myList->chunks[myList->numChunks].baseAddress=baseAddress;
    myList->chunks[myList->numChunks].chunk=chunk;
    fivmr_fence();
    myList->numChunks++;
}

void fivmr_PageTable_ensure(fivmr_PageTable *pt,
			    uintptr_t address) {
    uintptr_t bitsIdx;
#if FIVMSYS_PTRSIZE==8
    uint32_t **middle;
#endif
    
    fivmr_assert(!pt->isFlat);

    fivmr_Lock_lock(&pt->u.ml.lock);
#if FIVMSYS_PTRSIZE==4
    bitsIdx=(address&FIVMR_PT_SPINE_MASK)>>FIVMR_PT_SPINE_SHIFT;
    if (pt->u.ml.table[bitsIdx]==NULL) {
        uint32_t *subTable=(uint32_t*)fivmr_mallocAssert(FIVMR_PAGE_SIZE);
        bzero(subTable,FIVMR_PAGE_SIZE);
        fivmr_fence();
	pt->u.ml.table[bitsIdx]=subTable;
	addChunk(pt,address&FIVMR_PT_SPINE_MASK,pt->u.ml.table[bitsIdx]);
    }
#else
    bitsIdx=(address&FIVMR_PT_OUTER_MASK)>>FIVMR_PT_OUTER_SHIFT;
    if (pt->u.ml.table[bitsIdx]==NULL) {
        uint32_t **midTable=(uint32_t**)
            fivmr_mallocAssert(FIVMR_PT_NUM_MIDDLE_ELE*FIVMSYS_PTRSIZE);
        bzero(midTable,FIVMR_PT_NUM_MIDDLE_ELE*FIVMSYS_PTRSIZE);
        fivmr_fence();
	pt->u.ml.table[bitsIdx]=midTable;
    }
    middle=pt->u.ml.table[bitsIdx];
    bitsIdx=(address&FIVMR_PT_MIDDLE_MASK)>>FIVMR_PT_MIDDLE_SHIFT;
    if (middle[bitsIdx]==NULL) {
        uint32_t *subTable=(uint32_t*)fivmr_mallocAssert(FIVMR_PT_NUM_INNER_BYTES);
        bzero(subTable,FIVMR_PT_NUM_INNER_BYTES);
        fivmr_fence();
	middle[bitsIdx]=subTable;
	addChunk(pt,
		 address&(FIVMR_PT_OUTER_MASK|FIVMR_PT_MIDDLE_MASK),
		 middle[bitsIdx]);
    }
#endif
    fivmr_Lock_unlock(&pt->u.ml.lock);
}


/* this function *has* to be out-of-line otherwise GCC goes bananas. */
void fivmr_PTIterator_init(fivmr_PTIterator *pti,
                           fivmr_PageTable *pt) {
    bzero(pti,sizeof(fivmr_PTIterator));
    
    pti->pt=pt;
    if (FIVMR_PT_IS_FLAT(pt)) {
        pti->u.flat.first=true;
        pti->baseAddress=pt->u.flat.start;
        pti->chunk=pt->u.flat.table;
        pti->chunkLength=
            (((pt->u.flat.size>>FIVMSYS_LOG_PAGE_SIZE)
              <<FIVMR_LOG_PT_BITS_PER_PAGE)
             +31)
            /32;
    } else {
        pti->u.ml.cur=pt->u.ml.chunkListHead;
        fivmr_PTIterator_setPCLImpl(pti);
    }
}

