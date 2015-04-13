/*
 * fivmr_rbtree.h
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
 * fivmr_rbtree.h -- implementation of a red-black tree.
 * by Filip Pizlo, 2010
 *
 * Based on http://www.mit.edu/~emin/source_code/red_black_tree/red_black_tree.c
 *
 * Which had the following license at the time that I retrieved it:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that neither the name of Emin
 * Martinian nor the names of any contributors are be used to endorse or
 * promote products derived from this software without specific prior
 * written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ---------------------------------------------------------------------------
 *
 * This implements a multi-map, which can contain multiple keys of the
 * same value.  It's meant to be very fast, and never allocates memory.
 * You are responsible for managing memory for nodes, though thoroughly
 * crappy helpers are provided (and should really only be used by the
 * test suite).
 *
 * Note: this API is designed very carefully for the types of uses for
 * which it was designed.  If you don't like it then go cry to your
 * mother.
 */

#ifndef FIJI_RBTREE_H
#define FIJI_RBTREE_H

#include "fivmr_util.h"
#include "fivmr_sysdep.h"

struct ftree_Node_s;
struct ftree_Tree_s;

typedef struct ftree_Node_s ftree_Node;
typedef struct ftree_Tree_s ftree_Tree;

struct ftree_Node_s {
    uintptr_t key;
    uintptr_t value;
    bool red; /* if red=0 then the node is black */
    ftree_Node* left;
    ftree_Node* right;
    ftree_Node* parent;
};


/* Compare(a,b) should return 1 if *a > *b, -1 if *a < *b, and 0 otherwise */
/* Destroy(a) takes a pointer to whatever key might be and frees it accordingly */
struct ftree_Tree_s {
    int (*compare)(uintptr_t a, uintptr_t b); 
    /*  A sentinel is used for root and for nil.  These sentinels are */
    /*  created when RBTreeCreate is caled.  root->left should always */
    /*  point to the node which is the root of the tree.  nil points to a */
    /*  node which should always be black but has aribtrary children and */
    /*  parent and no key or info.  The point of using these sentinels is so */
    /*  that the root and nil nodes do not require special cases in the code */
    ftree_Node root;             
    ftree_Node nil;              
};

static inline void ftree_Node_init(ftree_Node *node,
                                   uintptr_t key,
                                   uintptr_t value) {
    node->key=key;
    node->value=value;
    node->red=false;
    node->left=NULL;
    node->right=NULL;
    node->parent=NULL;
}

static inline ftree_Node *ftree_Node_create(uintptr_t key,
                                            uintptr_t value) {
    ftree_Node *result=fivmr_mallocAssert(sizeof(ftree_Node));
    ftree_Node_init(result,key,value);
    return result;
}

void ftree_Tree_init(ftree_Tree *tree,
                     int (*compare)(uintptr_t a,uintptr_t b));

void ftree_Tree_add(ftree_Tree *tree,
                    ftree_Node *node);

/* asserts that we're adding a new node.  if a node with the same key
   already exists then this will assert. */
void ftree_Tree_addNew(ftree_Tree *tree,
                       ftree_Node *node);

static inline ftree_Node *ftree_Tree_first(ftree_Tree *tree) {
    ftree_Node* nil=&tree->nil;
    ftree_Node* x=tree->root.left;
    ftree_Node *candidate=nil;
    while (x!=nil) {
        candidate=x;
        x=x->left;
    }
    if (candidate==nil) {
        return NULL;
    } else {
        fivmr_assert(candidate!=&tree->nil);
        return candidate;
    }
}

static inline ftree_Node *ftree_Tree_last(ftree_Tree *tree) {
    ftree_Node* nil=&tree->nil;
    ftree_Node* x=tree->root.left;
    ftree_Node *candidate=nil;
    while (x!=nil) {
        candidate=x;
        x=x->right;
    }
    if (x==nil) {
        return NULL;
    } else {
        return x;
    }
}

static inline ftree_Node *ftree_Tree_nextImpl(ftree_Tree *tree,
                                              ftree_Node *x) {
    ftree_Node* y;
    ftree_Node* nil=&tree->nil;
    ftree_Node* root=&tree->root;

    if (nil != (y = x->right)) { /* assignment to y is intentional */
        while(y->left != nil) { /* returns the minium of the right subtree of x */
            y=y->left;
        }
    } else {
        y=x->parent;
        while(x == y->right) { /* sentinel used instead of checking for nil */
            x=y;
            y=y->parent;
        }
        if (y == root) return nil; 
    }
    return y;
}

static inline ftree_Node *ftree_Tree_prevImpl(ftree_Tree *tree,
                                              ftree_Node *x) {
    ftree_Node* y;
    ftree_Node* nil=&tree->nil;
    ftree_Node* root=&tree->root;
    
    if (nil != (y = x->left)) { /* assignment to y is intentional */
        while(y->right != nil) { /* returns the maximum of the left subtree of x */
            y=y->right;
        }
    } else {
        y=x->parent;
        while(x == y->left) { 
            if (y == root) return nil; 
            x=y;
            y=y->parent;
        }
    }
    return(y);
}

static inline ftree_Node *ftree_Tree_next(ftree_Tree *tree,
                                          ftree_Node *x) {
    ftree_Node *result=ftree_Tree_nextImpl(tree,x);
    if (result==&tree->nil) {
        return NULL;
    } else {
        fivmr_assert(result!=&tree->nil);
        return result;
    }
}

static inline ftree_Node *ftree_Tree_prev(ftree_Tree *tree,
                                          ftree_Node *x) {
    ftree_Node *result=ftree_Tree_prevImpl(tree,x);
    if (result==&tree->nil) {
        return NULL;
    } else {
        return result;
    }
}

/* returns one of the a node with the given key or NULL if none exists.
   If there are multiple nodes with the given key then it's up to you
   to locate all of them.  That'll require calling both prev and next. */
static inline ftree_Node *ftree_Tree_findFast(ftree_Tree *tree,
                                              uintptr_t key,
                                              int (*compare)(uintptr_t a,
                                                             uintptr_t b)) {
    ftree_Node* x=tree->root.left;
    ftree_Node* nil=&tree->nil;
    int compVal;
    if (x == nil) return NULL;
    compVal=compare(x->key, key);
    while(0 != compVal) {/*assignemnt*/
        if (compVal>0) { /* x->key > q */
            x=x->left;
        } else {
            x=x->right;
        }
        if ( x == nil) return NULL;
        compVal=compare(x->key,key);
    }
    return(x);
}

ftree_Node *ftree_Tree_find(ftree_Tree *tree,
                            uintptr_t key);

void ftree_Tree_remove(ftree_Tree *tree,
                       ftree_Node *node);

static inline void ftree_Tree_delete(ftree_Tree *tree,
                                     ftree_Node *node) {
    ftree_Tree_remove(tree,node);
    free(node);
}

static inline bool ftree_Tree_findAndDelete(ftree_Tree *tree,
                                            uintptr_t key) {
    ftree_Node *node=ftree_Tree_find(tree,key);
    if (node==NULL) {
        return false;
    } else {
        ftree_Tree_delete(tree,node);
        return true;
    }
}

#endif
