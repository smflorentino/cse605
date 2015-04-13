#include <stdio.h>
#include <fivmr.h>

extern fivmr_Payload hello_payload;

int main(int c,char **v) {
    fivmr_VM vm;
    int32_t exitCode;
    
    printf("attempting to run hello world program...\n");
    fflush(stdout);
    
    if (false) {
        fivmr_logLevel=3;
    }

    fivmr_runBaseInit();
    
    fivmr_VM_resetSettings(&vm,hello_payload.defConfig);
    fivmr_VM_registerPayload(&vm,&hello_payload);
    fivmr_runRuntime(&vm,c,v);
    fivmr_VM_shutdown(&vm,&exitCode);
    
    printf("VM has exited, status = %d!\n",exitCode);
    fflush(stdout);
    
    return exitCode;
}

