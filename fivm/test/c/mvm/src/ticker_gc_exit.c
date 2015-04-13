#include <stdio.h>
#include <fivmr.h>

extern fivmr_Payload ticker_payload;
extern fivmr_Payload simplegctest_payload;

static fivmr_VM ticker_vm;
static fivmr_VM gctest_vm;

static void *ticker_main(void *arg) {
    char *args[1];
    args[0]="ticker";
    fivmr_runRuntime(&ticker_vm,1,args);
    printf("Ticker VM exited!\n");
    return NULL;
}

static void *gctest_main(void *arg) {
    char *args[4];
    args[0]="gctest";
    args[1]="-1";
    args[2]="10000000";
    args[3]="10";
    fivmr_runRuntime(&gctest_vm,4,args);
    printf("GC test VM exited!\n");
    return NULL;
}

int main(int c,char **v) {
    pthread_t t1,t2;
    int32_t exitCode;
    
    fivmr_fakeRTPriorities=true;
    if (false) fivmr_logLevel=1;
    
    fivmr_runBaseInit();
    
    fivmr_VM_resetSettings(&ticker_vm,ticker_payload.defConfig);
    fivmr_VM_registerPayload(&ticker_vm,&ticker_payload);

    fivmr_VM_resetSettings(&gctest_vm,simplegctest_payload.defConfig);
    fivmr_VM_registerPayload(&gctest_vm,&simplegctest_payload);
    gctest_vm.gc.maxPagesUsed=(1000000000>>FIVMSYS_LOG_PAGE_SIZE);

    printf("Both VMs initialized; starting threads...\n");
    
    pthread_create(&t1,NULL,ticker_main,NULL);
    pthread_create(&t2,NULL,gctest_main,NULL);
    
    sleep(3);
    printf("Ran for 3 seconds; test done.  Exiting ticker VM...\n");
    
    fivmr_VM_exit(&ticker_vm,4201);
    printf("Ticker VM exited.  Exiting GC test VM...\n");
    fivmr_VM_exit(&gctest_vm,2403);
    
    printf("Both VMs exited.  Waiting for ticker threads...\n");
    pthread_join(t1,NULL);
    printf("Waiting for GC test threads...\n");
    pthread_join(t2,NULL);
    
    printf("Done.  Shutting down both VMs...\n");
    
    fivmr_VM_shutdown(&ticker_vm,&exitCode);
    if (exitCode!=4201) {
        printf("Bad exit code on Ticker VM: %d (expected 4201)\n",exitCode);
        return 1;
    }
    fivmr_VM_shutdown(&gctest_vm,&exitCode);
    if (exitCode!=2403) {
        printf("Bad exit code on GC VM: %d (expected 2403)\n",exitCode);
        return 1;
    }
    
    printf("Both VMs shutdown.\n");

    return 0;
}

