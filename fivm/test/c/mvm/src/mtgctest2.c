#include <stdio.h>
#include <fivmr.h>

extern fivmr_Payload mtgctest2_payload;

int main(int c,char **v) {
    fivmr_VM vm;
    int32_t exitCode;
    
    printf("attempting to run MTGCTest2 program...\n");
    fflush(stdout);
    
    if (false) {
        fivmr_logLevel=1;
    }

    fivmr_runBaseInit();
    
    fivmr_VM_resetSettings(&vm,mtgctest2_payload.defConfig);
    vm.gc.maxPagesUsed=(1024u*1024u*1024u)>>FIVMSYS_LOG_PAGE_SIZE;
    fivmr_VM_registerPayload(&vm,&mtgctest2_payload);
    fivmr_runRuntime(&vm,c,v);
    fivmr_VM_shutdown(&vm,&exitCode);
    
    printf("VM has exited, status = %d!\n",exitCode);
    fflush(stdout);
    
    return exitCode;
}

