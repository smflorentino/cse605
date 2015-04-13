#include <stdio.h>
#include <fivmr.h>

extern fivmr_Payload exit42_payload;

int main(int c,char **v) {
    fivmr_VM vm;
    fivmr_Payload *copy;
    int32_t exitCode;
    
    printf("attempting to run Exit42 program...\n");
    fflush(stdout);
    
    fivmr_runBaseInit();
    
    fivmr_VM_resetSettings(&vm,exit42_payload.defConfig);

    copy=fivmr_Payload_copy(&exit42_payload);
    fivmr_assert(copy!=NULL);
    
    fivmr_VM_registerPayload(&vm,copy);
    fivmr_runRuntime(&vm,c,v);
    fivmr_VM_shutdown(&vm,&exitCode);
    
    printf("VM has exited, status = %d!\n",exitCode);
    fflush(stdout);
    
    if (exitCode!=42) {
        printf("VM failed to exit with status = 42.  ERROR!\n");
        return 1;
    }
    
    return 0;
}

