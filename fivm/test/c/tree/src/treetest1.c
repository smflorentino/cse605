#include <fivmr.h>

static int strcmp_compare(uintptr_t a,
                          uintptr_t b) {
    return strcmp((const char*)(void*)a,
                  (const char*)(void*)b);
}

int main(int c,char **v) {
    ftree_Tree tree;
    ftree_Node *n1;
    int i;
    
    ftree_Tree_init(&tree,strcmp_compare);
    
    fivmr_assert(ftree_Tree_find(&tree,
                                 0)==NULL);
    fivmr_assert(ftree_Tree_find(&tree,
                                 (uintptr_t)(void*)"this")==NULL);
    fivmr_assert(ftree_Tree_find(&tree,
                                 (uintptr_t)(void*)"crap")==NULL);

    n1=ftree_Node_create((uintptr_t)(void*)"this",
                         (uintptr_t)(void*)"that");
    ftree_Tree_add(&tree,n1);
    fivmr_assert(ftree_Tree_find(&tree,
                                 (uintptr_t)(void*)"this")==n1);
    fivmr_assert(ftree_Tree_find(&tree,
                                 (uintptr_t)(void*)"crap")==NULL);
    
    fivmr_assert(ftree_Tree_findAndDelete(&tree,
                                          (uintptr_t)(void*)"this"));
    fivmr_assert(ftree_Tree_find(&tree,
                                 (uintptr_t)(void*)"this")==NULL);
    fivmr_assert(ftree_Tree_find(&tree,
                                 (uintptr_t)(void*)"crap")==NULL);
    
    /* try brutal sorted insertion */
    for (i=0;i<100000;++i) {
        char *str1=(char*)fivmr_mallocAssert(64);
        char *str2=(char*)fivmr_mallocAssert(64);
        char str3[64];
        snprintf(str1,64,"%d",i);
        snprintf(str2,64,"%d",i*3+2);
        n1=ftree_Node_create((uintptr_t)(void*)str1,
                             (uintptr_t)(void*)str2);
        ftree_Tree_add(&tree,n1);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)str1)==n1);
        snprintf(str3,64,"%d",i);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)str3)==n1);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)"this")==NULL);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)"crap")==NULL);
    }
    
    /* now delete the first half of them */
    for (i=0;i<50000;++i) {
        char str[64];
        snprintf(str,64,"%d",i);
        fivmr_assert(ftree_Tree_findAndDelete(&tree,
                                              (uintptr_t)(void*)str));
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)str)==NULL);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)"this")==NULL);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)"crap")==NULL);
    }
    
    /* make sure the other half is still there */
    for (i=50000;i<100000;++i) {
        char str1[64];
        char str2[64];
        snprintf(str1,64,"%d",i);
        snprintf(str2,64,"%d",i*3+2);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)str1)!=NULL);
        fivmr_assert(
            !strcmp(
                str2,
                (char*)(void*)ftree_Tree_find(&tree,
                                              (uintptr_t)(void*)str1)->value));
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)"this")==NULL);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)"crap")==NULL);
    }
    
    /* now delete the other half */
    for (i=50000;i<100000;++i) {
        char str[64];
        snprintf(str,64,"%d",i);
        fivmr_assert(ftree_Tree_findAndDelete(&tree,
                                              (uintptr_t)(void*)str));
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)str)==NULL);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)"this")==NULL);
        fivmr_assert(ftree_Tree_find(&tree,
                                     (uintptr_t)(void*)"crap")==NULL);
    }
    
    /* FIXME implement more tests! */
    
    printf("That worked!\n");
    return 0;
}


