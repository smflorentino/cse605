#include <stdio.h>
#include <fivmr.h>

extern fivmr_Payload ticker_payload;
extern fivmr_Payload simplegctest_payload;

static fivmr_TimeSliceManager timeslicer;
static fivmr_TimeSlice *ticker_slice;
static fivmr_TimeSlice *gctest_slice;

static void ticker_main(void *arg) {
    fivmr_VM vm;
    char *args[1];
    int32_t exitCode;
    args[0]="ticker";
    fivmr_VM_resetSettings(&vm,ticker_payload.defConfig);
    fivmr_VM_useTimeSlice(&vm,ticker_slice);
    fivmr_VM_registerPayload(&vm,&ticker_payload);
    fivmr_runRuntime(&vm,1,args);
    fivmr_VM_shutdown(&vm,&exitCode);
    printf("Ticker VM exited with status = %d!\n",exitCode);
    exit(1);
}

static void gctest_main(void *arg) {
    fivmr_VM vm;
    char *args[4];
    int32_t exitCode;
    args[0]="gctest";
    args[1]="-1";
    args[2]="10000000";
    args[3]="10";
    fivmr_VM_resetSettings(&vm,simplegctest_payload.defConfig);
    fivmr_VM_useTimeSlice(&vm,gctest_slice);
    fivmr_VM_registerPayload(&vm,&simplegctest_payload);
    vm.gc.maxPagesUsed=(1000000000>>FIVMSYS_LOG_PAGE_SIZE);
    fivmr_runRuntime(&vm,4,args);
    fivmr_VM_shutdown(&vm,&exitCode);
    printf("GC VM exited with status = %d!\n",exitCode);
    exit(1);
}

int main(int c,char **v) {
    pthread_t t1,t2;
    int32_t exitCode;
    
    fivmr_fakeRTPriorities=true;
    
    fivmr_runBaseTRPartInit();
    
    fivmr_TimeSliceManager_init(&timeslicer,2,FIVMR_TPR_MAX);
    ticker_slice=
        fivmr_TimeSliceManager_initSliceEasy(&timeslicer,0,
                                             1000*1000*1000,
                                             10,
                                             FIVMR_TPR_MAX-1);
    gctest_slice=
        fivmr_TimeSliceManager_initSliceEasy(&timeslicer,1,
                                             1000*1000*1000,
                                             10,
                                             FIVMR_TPR_MAX-1);
    fivmr_assert(fivmr_TimeSliceManager_fullyInitialized(&timeslicer));
    fivmr_TimeSliceManager_start(&timeslicer);
    
    fivmr_assert(ticker_slice->pool!=NULL);
    fivmr_assert(gctest_slice->pool!=NULL);
    
    fivmr_ThreadPool_spawn(ticker_slice->pool,ticker_main,NULL,FIVMR_TPR_MAX-1);
    fivmr_ThreadPool_spawn(gctest_slice->pool,gctest_main,NULL,FIVMR_TPR_MAX-1);
    
    sleep(100);
    printf("Ran for 100 seconds; test done.\n");
    
    exit(0);
    return 0;
}

