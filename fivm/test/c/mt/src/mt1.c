#include <fivmr.h>
#include <stdio.h>

int main(int c,char **v) {
    int i;
    for (i=1;i<c;++i) {
        fivmr_Random mt;
        uint32_t seed;
        int j;
        if (sscanf(v[i],"%d",&seed)!=1) {
            fprintf(stderr,"Error: cannot parse int from %s\n",v[1]);
        }
        printf("Using seed %d:\n",seed);
        fivmr_Random_initBySeed(&mt,seed);
        printf("Table dump:\n");
        for (j=0;j<(int)FIVMR_RANDMT_N;++j) {
            printf("%d\n",mt.mt[j]);
        }
        printf("Longs:\n");
        for (j=0;j<3000;++j) {
            printf("%" PRId64 "\n",fivmr_Random_generate64(&mt));
        }
        printf("Integers:\n");
        for (j=0;j<3000;++j) {
            printf("%d\n",fivmr_Random_generate32(&mt));
        }
        printf("Bytes:\n");
        for (j=0;j<3000;++j) {
            printf("%d\n",(int32_t)(int8_t)fivmr_Random_generate8(&mt));
        }
    }
    return 0;
}

