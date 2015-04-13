#include <stdio.h>
#include <fivmr.h>

extern fivmr_Payload ticker_payload;
extern fivmr_Payload simplegctest_payload;

static void *ticker_main(void *arg) {
    fivmr_VM vm;
    char *args[1];
    int32_t exitCode;
    args[0]="ticker";
    fivmr_VM_resetSettings(&vm,ticker_payload.defConfig);
    fivmr_VM_registerPayload(&vm,&ticker_payload);
    fivmr_runRuntime(&vm,1,args);
    fivmr_VM_shutdown(&vm,&exitCode);
    printf("Ticker VM exited with status = %d!\n",exitCode);
    exit(1);
    return NULL;
}

static void *gctest_main(void *arg) {
    fivmr_VM vm;
    char *args[4];
    int32_t exitCode;
    args[0]="gctest";
    args[1]="-1";
    args[2]="10000000";
    args[3]="10";
    fivmr_VM_resetSettings(&vm,simplegctest_payload.defConfig);
    fivmr_VM_registerPayload(&vm,&simplegctest_payload);
    vm.gc.maxPagesUsed=(1000000000>>FIVMSYS_LOG_PAGE_SIZE);
    fivmr_runRuntime(&vm,4,args);
    fivmr_VM_shutdown(&vm,&exitCode);
    printf("GC VM exited with status = %d!\n",exitCode);
    exit(1);
    return NULL;
}

int main(int c,char **v) {
    pthread_t t1,t2;
    
    fivmr_fakeRTPriorities=true;
    
    fivmr_runBaseInit();
    
    pthread_create(&t1,NULL,ticker_main,NULL);
    pthread_create(&t2,NULL,gctest_main,NULL);
    
    sleep(100);
    printf("Ran for 100 seconds; test done.\n");
    
    exit(0);
    return 0;
}

