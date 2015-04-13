/*
 * fivmr_rbtree.c
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
 */

#include "fivmr.h"

void ftree_Tree_init(ftree_Tree *tree,
                     int (*compare) (uintptr_t,uintptr_t)) {
    tree->compare=compare;

    /*  see the comment in the rb_red_blk_tree structure in red_black_tree.h */
    /*  for information on nil and root */
    tree->nil.key=0;
    tree->nil.value=0;
    tree->nil.red=false;
    tree->nil.parent=&tree->nil;
    tree->nil.left=&tree->nil;
    tree->nil.right=&tree->nil;
    tree->root.key=0;
    tree->root.value=0;
    tree->root.red=false;
    tree->root.parent=&tree->nil;
    tree->root.left=&tree->nil;
    tree->root.right=&tree->nil;
}

static void LeftRotate(ftree_Tree* tree, ftree_Node* x) {
    ftree_Node* y;
    ftree_Node* nil=&tree->nil;

    /*  I originally wrote this function to use the sentinel for */
    /*  nil to avoid checking for nil.  However this introduces a */
    /*  very subtle bug because sometimes this function modifies */
    /*  the parent pointer of nil.  This can be a problem if a */
    /*  function which calls LeftRotate also uses the nil sentinel */
    /*  and expects the nil sentinel's parent pointer to be unchanged */
    /*  after calling this function.  For example, when RBDeleteFixUP */
    /*  calls LeftRotate it expects the parent pointer of nil to be */
    /*  unchanged. */

    y=x->right;
    x->right=y->left;

    if (y->left != nil) y->left->parent=x; /* used to use sentinel here */
    /* and do an unconditional assignment instead of testing for nil */
  
    y->parent=x->parent;   

    /* instead of checking if x->parent is the root as in the book, we */
    /* count on the root sentinel to implicitly take care of this case */
    if( x == x->parent->left) {
        x->parent->left=y;
    } else {
        x->parent->right=y;
    }
    y->left=x;
    x->parent=y;
    
    fivmr_assert(!tree->nil.red);
}


/***********************************************************************/
/*  FUNCTION:  RighttRotate */
/**/
/*  INPUTS:  This takes a tree so that it can access the appropriate */
/*           root and nil pointers, and the node to rotate on. */
/**/
/*  OUTPUT:  None */
/**/
/*  Modifies Input?: tree, y */
/**/
/*  EFFECTS:  Rotates as described in _Introduction_To_Algorithms by */
/*            Cormen, Leiserson, Rivest (Chapter 14).  Basically this */
/*            makes the parent of x be to the left of x, x the parent of */
/*            its parent before the rotation and fixes other pointers */
/*            accordingly. */
/***********************************************************************/

static void RightRotate(ftree_Tree* tree, ftree_Node* y) {
    ftree_Node* x;
    ftree_Node* nil=&tree->nil;

    /*  I originally wrote this function to use the sentinel for */
    /*  nil to avoid checking for nil.  However this introduces a */
    /*  very subtle bug because sometimes this function modifies */
    /*  the parent pointer of nil.  This can be a problem if a */
    /*  function which calls LeftRotate also uses the nil sentinel */
    /*  and expects the nil sentinel's parent pointer to be unchanged */
    /*  after calling this function.  For example, when RBDeleteFixUP */
    /*  calls LeftRotate it expects the parent pointer of nil to be */
    /*  unchanged. */

    x=y->left;
    y->left=x->right;

    if (nil != x->right)  x->right->parent=y; /*used to use sentinel here */
    /* and do an unconditional assignment instead of testing for nil */

    /* instead of checking if x->parent is the root as in the book, we */
    /* count on the root sentinel to implicitly take care of this case */
    x->parent=y->parent;
    if( y == y->parent->left) {
        y->parent->left=x;
    } else {
        y->parent->right=x;
    }
    x->right=y;
    y->parent=x;
    
    fivmr_assert(!tree->nil.red);
}

/***********************************************************************/
/*  FUNCTION:  TreeInsertHelp  */
/**/
/*  INPUTS:  tree is the tree to insert into and z is the node to insert */
/**/
/*  OUTPUT:  none */
/**/
/*  Modifies Input:  tree, z */
/**/
/*  EFFECTS:  Inserts z into the tree as if it were a regular binary tree */
/*            using the algorithm described in _Introduction_To_Algorithms_ */
/*            by Cormen et al.  This funciton is only intended to be called */
/*            by the RBTreeInsert function and not by the user */
/***********************************************************************/

static void TreeInsertHelp(ftree_Tree* tree, ftree_Node* z) {
    /*  This function should only be called by InsertRBTree (see above) */
    ftree_Node* x;
    ftree_Node* y;
    ftree_Node* nil=&tree->nil;
  
    z->left=z->right=nil;
    y=&tree->root;
    x=tree->root.left;
    while( x != nil) {
        y=x;
        if (tree->compare(x->key,z->key)>0) { /* x.key > z.key */
            x=x->left;
        } else { /* x,key <= z.key */
            x=x->right;
        }
    }
    z->parent=y;
    if ( (y == &tree->root) ||
         (tree->compare(y->key,z->key)>0)) { /* y.key > z.key */
        y->left=z;
    } else {
        y->right=z;
    }

    fivmr_assert(!tree->nil.red);
}

/***********************************************************************/
/*  FUNCTION:  RBTreeInsert */
/**/
/*  INPUTS:  tree is the red-black tree to insert a node which has a key */
/*           pointed to by key and info pointed to by info.  */
/**/
/*  OUTPUT:  This function returns a pointer to the newly inserted node */
/*           which is guarunteed to be valid until this node is deleted. */
/*           What this means is if another data structure stores this */
/*           pointer then the tree does not need to be searched when this */
/*           is to be deleted. */
/**/
/*  Modifies Input: tree */
/**/
/*  EFFECTS:  Creates a node node which contains the appropriate key and */
/*            info pointers and inserts it into the tree. */
/***********************************************************************/

void ftree_Tree_add(ftree_Tree* tree, ftree_Node *x) {
    ftree_Node * y;
    ftree_Node * newNode;

    TreeInsertHelp(tree,x);
    newNode=x;
    x->red=true;
    while(x->parent->red) { /* use sentinel instead of checking for root */
        if (x->parent == x->parent->parent->left) {
            y=x->parent->parent->right;
            if (y->red) {
                x->parent->red=false;
                y->red=false;
                x->parent->parent->red=true;
                x=x->parent->parent;
            } else {
                if (x == x->parent->right) {
                    x=x->parent;
                    LeftRotate(tree,x);
                }
                x->parent->red=false;
                x->parent->parent->red=true;
                RightRotate(tree,x->parent->parent);
            } 
        } else { /* case for x->parent == x->parent->parent->right */
            y=x->parent->parent->left;
            if (y->red) {
                x->parent->red=false;
                y->red=false;
                x->parent->parent->red=true;
                x=x->parent->parent;
            } else {
                if (x == x->parent->left) {
                    x=x->parent;
                    RightRotate(tree,x);
                }
                x->parent->red=false;
                x->parent->parent->red=true;
                LeftRotate(tree,x->parent->parent);
            } 
        }
    }
    tree->root.left->red=false;

    fivmr_assert(!tree->nil.red);
    fivmr_assert(!tree->root.red);
}

void ftree_Tree_addNew(ftree_Tree *tree,
                       ftree_Node *node) {
    fivmr_assert(ftree_Tree_find(tree,node->key)==NULL);
    ftree_Tree_add(tree,node);
}

ftree_Node *ftree_Tree_find(ftree_Tree *tree,
                            uintptr_t key) {
    return ftree_Tree_findFast(tree,key,tree->compare);
}


/***********************************************************************/
/*  FUNCTION:  RBDeleteFixUp */
/**/
/*    INPUTS:  tree is the tree to fix and x is the child of the spliced */
/*             out node in RBTreeDelete. */
/**/
/*    OUTPUT:  none */
/**/
/*    EFFECT:  Performs rotations and changes colors to restore red-black */
/*             properties after a node is deleted */
/**/
/*    Modifies Input: tree, x */
/**/
/*    The algorithm from this function is from _Introduction_To_Algorithms_ */
/***********************************************************************/

static void RBDeleteFixUp(ftree_Tree* tree, ftree_Node* x) {
    ftree_Node* root=tree->root.left;
    ftree_Node* w;

    while( (!x->red) && (root != x)) {
        if (x == x->parent->left) {
            w=x->parent->right;
            if (w->red) {
                w->red=0;
                x->parent->red=1;
                LeftRotate(tree,x->parent);
                w=x->parent->right;
            }
            if ( (!w->right->red) && (!w->left->red) ) { 
                w->red=1;
                x=x->parent;
            } else {
                if (!w->right->red) {
                    w->left->red=0;
                    w->red=1;
                    RightRotate(tree,w);
                    w=x->parent->right;
                }
                w->red=x->parent->red;
                x->parent->red=0;
                w->right->red=0;
                LeftRotate(tree,x->parent);
                x=root; /* this is to exit while loop */
            }
        } else { /* the code below is has left and right switched from above */
            w=x->parent->left;
            if (w->red) {
                w->red=0;
                x->parent->red=1;
                RightRotate(tree,x->parent);
                w=x->parent->left;
            }
            if ( (!w->right->red) && (!w->left->red) ) { 
                w->red=1;
                x=x->parent;
            } else {
                if (!w->left->red) {
                    w->right->red=0;
                    w->red=1;
                    LeftRotate(tree,w);
                    w=x->parent->left;
                }
                w->red=x->parent->red;
                x->parent->red=0;
                w->left->red=0;
                RightRotate(tree,x->parent);
                x=root; /* this is to exit while loop */
            }
        }
    }
    x->red=0;

    fivmr_assert(!tree->nil.red);
}


/***********************************************************************/
/*  FUNCTION:  RBDelete */
/**/
/*    INPUTS:  tree is the tree to delete node z from */
/**/
/*    OUTPUT:  none */
/**/
/*    EFFECT:  Deletes z from tree and frees the key and info of z */
/*             using DestoryKey and DestoryInfo.  Then calls */
/*             RBDeleteFixUp to restore red-black properties */
/**/
/*    Modifies Input: tree, z */
/**/
/*    The algorithm from this function is from _Introduction_To_Algorithms_ */
/***********************************************************************/

void ftree_Tree_remove(ftree_Tree* tree, ftree_Node* z){
    ftree_Node* y;
    ftree_Node* x;
    ftree_Node* nil=&tree->nil;
    ftree_Node* root=&tree->root;

    y= ((z->left == nil) || (z->right == nil)) ? z : ftree_Tree_nextImpl(tree,z);
    x= (y->left == nil) ? y->right : y->left;
    if (root == (x->parent = y->parent)) { /* assignment of y->p to x->p is intentional */
        root->left=x;
    } else {
        if (y == y->parent->left) {
            y->parent->left=x;
        } else {
            y->parent->right=x;
        }
    }
    if (y != z) { /* y should not be nil in this case */

        fivmr_assert( (y!=&tree->nil));
        /* y is the node to splice out and x is its child */

        if (!(y->red)) RBDeleteFixUp(tree,x);
  
        y->left=z->left;
        y->right=z->right;
        y->parent=z->parent;
        y->red=z->red;
        z->left->parent=z->right->parent=y;
        if (z == z->parent->left) {
            z->parent->left=y; 
        } else {
            z->parent->right=y;
        }
    } else {
        if (!(z->red)) RBDeleteFixUp(tree,x);
    }
  
    fivmr_assert(!tree->nil.red);
}

