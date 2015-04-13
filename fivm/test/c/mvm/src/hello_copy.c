#include <stdio.h>
#include <fivmr.h>

extern fivmr_Payload hello_payload;

int main(int c,char **v) {
    fivmr_VM vm;
    fivmr_Payload *copy;
    int32_t exitCode;
    
    printf("attempting to run hello world program...\n");
    fflush(stdout);
    
    /* fivmr_logLevel=2; */
    
    fivmr_runBaseInit();
    
    fivmr_VM_resetSettings(&vm,hello_payload.defConfig);

    /* to have multiple VM's running the same payload, the payloads must be copied.
       this tests our ability to copy payloads.  if the fivmr_Payload_copy()
       routine is broken, then the system will fail either:
       
       - when copying
       - when registering the payload
       - most likely: when running.
       
       note that this does not test that copying a payload indeed eliminates all
       sharing (i.e. there may be some state that gets shared between the original
       payload and the copy, which would be bad, unless that state is immutable).
       to iron out that case, don't use this test. */
    copy=fivmr_Payload_copy(&hello_payload);
    fivmr_assert(copy!=NULL);
    
    fivmr_VM_registerPayload(&vm,copy);
    fivmr_runRuntime(&vm,c,v);
    fivmr_VM_shutdown(&vm,&exitCode);
    
    printf("VM has exited, status = %d!\n",exitCode);
    fflush(stdout);
    
    return exitCode;
}

