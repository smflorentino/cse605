/*
 * fivmr_region.h
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

/*
 * fivmr_region.h -- region memory management API specifically for complex
 *                   objects that consist of lots of small chunks of memory.
 * by Filip Pizlo, 2003, 2004, 2005
 * Copyright (C) 2010 Fiji Systems Inc.
 *
 * This code is originally from TSF; the prefixes were changed from TSF to
 * FREG (Fiji Region)
 *
 * This code will compile as both C and C++.
 */

#ifndef FP_FIJI_REGION_H
#define FP_FIJI_REGION_H

#include "fivmr_util.h"

#include <string.h>
#include <stdlib.h>

#define FREG_NATIVE_ALIGNMENT 8

/* private stuff */
#define FREG_ALIGN(size) \
    ((size+FREG_NATIVE_ALIGNMENT-1)&(~(FREG_NATIVE_ALIGNMENT-1)))

#define FREG_REGION_DEF_SIZE_BITS 10
#define FREG_REGION_DEF_SIZE_UNIT (1<<FREG_REGION_DEF_SIZE_BITS)

/* only pass an aligned size */
#define FREG_INIT_SIZE_FOR_REGION(size_alignment,size_needed)   \
    (FREG_ALIGN((size_needed+size_alignment-1)/(size_alignment)*(size_alignment)))

#define FREG_SIZE_FOR_REGION(region,size_needed)        \
    FREG_INIT_SIZE_FOR_REGION(((freg_region_t*)(region))->step_size,(size_needed))

struct freg_region;
struct freg_region_cback_node;
typedef struct freg_region freg_region_t;
typedef struct freg_region_cback_node freg_region_cback_node_t;

typedef void (*freg_region_cback_t)(void *arg);

struct freg_region_cback_node {
    freg_region_cback_t cback;
    void *arg;
    freg_region_cback_node_t *next;
};

struct freg_region {
    freg_region_t *next;
    uint8_t *alloc;
    size_t size;
    size_t step_size;

    freg_region_cback_node_t *cback_head;

    /* wanna make this structure native aligned. */
#if (((FREG_NATIVE_ALIGNMENT-\
       (SIZEOF_VOID_P*3+SIZEOF_SIZE_T*2))&\
      (FREG_NATIVE_ALIGNMENT-1)) != 0)
    char padding[(FREG_NATIVE_ALIGNMENT-
                  (SIZEOF_VOID_P*3+SIZEOF_SIZE_T*2))&
                 (FREG_NATIVE_ALIGNMENT-1)];
#endif
};

/* FIXME: optimize this by:
   - having the step size vary
   - if you allocate something larger than step size then don't necessarily
     add the step size to it, -or- have some way of recovering that wasted
     memory. */

static inline void *freg_region_create_with_steps(size_t size,
                                                  size_t step_size) {
    freg_region_t *region;
    size_t reg_size;
    
    fivmr_assert(step_size>0);
    
    size=FREG_ALIGN(size);
    reg_size=FREG_INIT_SIZE_FOR_REGION(step_size,size);
    
    /* start a new region and allocate the root object */
    
    region=(freg_region_t*)
        fivmr_malloc(sizeof(freg_region_t)+reg_size);
    if (region==NULL) {
        return NULL;
    }
    
    region->next=region;
    region->alloc=((uint8_t*)(region+1))+size;
    region->size=reg_size;
    region->step_size=step_size;
    
    region->cback_head=NULL;
    
    return region+1;
}

static inline void *freg_region_create(size_t size) {
    return freg_region_create_with_steps(size,FREG_REGION_DEF_SIZE_UNIT);
}

static inline void *freg_region_alloc(void *root,
                                      size_t size) {
    freg_region_t *region;
    freg_region_t *next;
    void *result;

    fivmr_assert(root!=NULL);
    
    if (size==0) {
        return NULL;
    }
    
    size=FREG_ALIGN(size);
    
    region=((freg_region_t*)root)-1;
    next=region->next;
    
    fivmr_assert(region->step_size>0);
    
    /* allocate new object */
    
    /* we do the comparison this way to avoid overflow! */
    if (next->alloc-((uint8_t*)next)-sizeof(freg_region_t)+size >
        next->size) {
        /* alloc new chunk */
        
        freg_region_t *new_chunk=(freg_region_t*)
            fivmr_malloc(sizeof(freg_region_t)+FREG_SIZE_FOR_REGION(region,size));
        
        if (new_chunk==NULL) {
            return NULL;
        }
        
        new_chunk->next=next;
        new_chunk->alloc=(uint8_t*)(new_chunk+1);
        new_chunk->size=FREG_SIZE_FOR_REGION(region,size);
        new_chunk->step_size=region->step_size;
        next=region->next=new_chunk;
    }
    
    result=next->alloc;
    next->alloc+=size;
    return result;
}

static inline void *freg_region_realloc(void *root,
                                        void *obj,
                                        size_t size) {
    freg_region_t *region;
    freg_region_t *next;

    fivmr_assert(root!=NULL);
    fivmr_assert(obj!=NULL);
    
    size=FREG_ALIGN(size);
    
    region=((freg_region_t*)root)-1;
    next=region->next;
    
    fivmr_assert(region->step_size>0);
    
    /* resize an existing object */
    
    /* we do the comparison this way to avoid overflow! */
    if (((uint8_t*)obj)-((uint8_t*)next)-sizeof(freg_region_t)+size >
        next->size) {
        
        /* check if this is the only object in the chunk. */
        if (next+1==obj) {
            /* if so, simply realloc the chunk.  we do this to prevent
             * O(n^2) memory usage in the case of repeated reallocs. */
            
            next=(freg_region_t*)
                realloc(next,
                        sizeof(freg_region_t)+FREG_SIZE_FOR_REGION(region,size));
            
            if (next==NULL) {
                return NULL;
            }
        } else {
            /* alloc new chunk */
            
            freg_region_t *new_chunk=(freg_region_t*)
                fivmr_malloc(sizeof(freg_region_t)+FREG_SIZE_FOR_REGION(region,size));
            
            if (new_chunk==NULL) {
                return NULL;
            }
            
            new_chunk->next=next;
            new_chunk->size=FREG_SIZE_FOR_REGION(region,size);
            new_chunk->step_size=region->step_size;
            
            memcpy(new_chunk+1,obj,next->alloc-((uint8_t*)obj));
        
            next=new_chunk;
        }
        
        region->next=next;
        obj=next+1;
    }
    
    next->alloc=((uint8_t*)obj)+size;
    return obj;
}

/* allocate either in a region or using straight malloc.  will use
 * malloc if region is NULL, otherwise will call freg_region_alloc.()
 * this cannot create a new region.  this is useful as a convenience
 * function for implementing data structures that need to be used both
 * in a region and with malloc & free. */
static inline void *freg_cond_alloc(void *root,size_t size) {
    if (root==NULL) {
        return fivmr_malloc(size);
    }
    return freg_region_alloc(root,size);
}

static inline bool freg_region_add_cback(void *root,
                                         freg_region_cback_t cback,
                                         void *arg) {
    freg_region_t *region=((freg_region_t*)root)-1;

    freg_region_cback_node_t *node=(freg_region_cback_node_t*)
        freg_region_alloc(root,sizeof(freg_region_cback_node_t));
    if (node==NULL) {
        return false;
    }
    
    node->next=region->cback_head;
    node->cback=cback;
    node->arg=arg;
    
    region->cback_head=node;
    
    return true;
}

/* delete a region rooted at the given object. */
static inline void freg_region_free(void *root) {
    freg_region_t *cur=((freg_region_t*)root)-1;
    freg_region_cback_node_t *cback_node=cur->cback_head;
    while (cback_node!=NULL) {
        cback_node->cback(cback_node->arg);
        cback_node=cback_node->next;
    }
    do {
        freg_region_t *to_free=cur;
        cur=cur->next;
        fivmr_free(to_free);
    } while (cur+1 != root);
}

/* get the total allocatable space occupied by this region */
static inline size_t freg_region_size(void *root) {
    freg_region_t *cur=((freg_region_t*)root)-1;
    size_t result=0;
    do {
        result+=cur->size;
        cur=cur->next;
    } while (cur+1 != root);
    return result;
}

#endif


