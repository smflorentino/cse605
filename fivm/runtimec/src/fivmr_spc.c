/*
 * fivmr_spc.c
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

#include <fivmr.h>

#if FIVMR_PROFILE_MONITOR_HEAVY
uintptr_t fivmr_SPC_lock;
uintptr_t fivmr_SPC_unlock;
#endif

#if FIVMR_PROFILE_MONITOR
uintptr_t fivmr_SPC_lockSlow;
uintptr_t fivmr_SPC_inflate;
uintptr_t fivmr_SPC_unlockSlow;
uintptr_t fivmr_SPC_lockSlowNotHeld;
uintptr_t fivmr_SPC_lockSlowRecurse;
uintptr_t fivmr_SPC_lockSlowInflate;
uintptr_t fivmr_SPC_lockSlowSpin;
uintptr_t fivmr_SPC_lockSlowQueue;
#endif

#if FIVMR_PROFILE_GC
uintptr_t fivmr_SPC_barrierSlowPath;
uintptr_t fivmr_SPC_allocSlowPath;
#endif

#if FIVMR_PROFILE_GC_HEAVY
uintptr_t fivmr_SPC_alloc;
uintptr_t fivmr_SPC_barrierFastPath;
#endif

void fivmr_SPC_dump(void) {
    int32_t n=fivmr_SPC_numCounts();
    if (n!=0) {
        int32_t i;
        uintptr_t **counts;
        char const **names;
        
        fivmr_Log_printf("Runtime Static Profile Counters:\n");
        
        counts=fivmr_mallocAssert(sizeof(uintptr_t*)*n);
        names=fivmr_mallocAssert(sizeof(char const*)*n);
        
        fivmr_SPC_getCounts(counts);
        fivmr_SPC_getNames(names);
        
        fivmr_Log_lock();
        for (i=0;i<n;++i) {
            fivmr_Log_printf("%30s: %" PRIuPTR "\n",
                             names[i],*(counts[i]));
        }
        fivmr_Log_unlock();
    }
}

int32_t fivmr_SPC_numCounts(void) {
    return FIVMR_PROFILE_MONITOR_HEAVY*2
        + FIVMR_PROFILE_MONITOR*8
        + FIVMR_PROFILE_GC*2
        + FIVMR_PROFILE_GC_HEAVY*2;
}

void fivmr_SPC_getCounts(uintptr_t **buffer) {
    int32_t idx=0;
#if FIVMR_PROFILE_MONITOR_HEAVY
    buffer[idx++]=&fivmr_SPC_lock;
    buffer[idx++]=&fivmr_SPC_unlock;
#endif
#if FIVMR_PROFILE_MONITOR
    buffer[idx++]=&fivmr_SPC_lockSlow;
    buffer[idx++]=&fivmr_SPC_inflate;
    buffer[idx++]=&fivmr_SPC_unlockSlow;
    buffer[idx++]=&fivmr_SPC_lockSlowNotHeld;
    buffer[idx++]=&fivmr_SPC_lockSlowRecurse;
    buffer[idx++]=&fivmr_SPC_lockSlowInflate;
    buffer[idx++]=&fivmr_SPC_lockSlowSpin;
    buffer[idx++]=&fivmr_SPC_lockSlowQueue;
#endif
#if FIVMR_PROFILE_GC
    buffer[idx++]=&fivmr_SPC_barrierSlowPath;
    buffer[idx++]=&fivmr_SPC_allocSlowPath;
#endif
#if FIVMR_PROFILE_GC_HEAVY
    buffer[idx++]=&fivmr_SPC_barrierFastPath;
    buffer[idx++]=&fivmr_SPC_alloc;
#endif
}

void fivmr_SPC_getNames(char const **buffer) {
    int32_t idx=0;
#if FIVMR_PROFILE_MONITOR_HEAVY
    buffer[idx++]="Monitor Lock";
    buffer[idx++]="Monitor Unlock";
#endif
#if FIVMR_PROFILE_MONITOR
    buffer[idx++]="Monitor Lock Slow";
    buffer[idx++]="Monitor Inflate";
    buffer[idx++]="Monitor Unlock Slow";
    buffer[idx++]="Monitor Lock Slow Not Held";
    buffer[idx++]="Monitor Lock Slow Recurse";
    buffer[idx++]="Monitor Lock Slow Inflate";
    buffer[idx++]="Monitor Lock Slow Spin";
    buffer[idx++]="Monitor Lock Slow Queue";
#endif
#if FIVMR_PROFILE_GC
    buffer[idx++]="GC Barrier Slow Path";
    buffer[idx++]="GC Alloc Slow Path";
#endif
#if FIVMR_PROFILE_GC_HEAVY
    buffer[idx++]="GC Barrier Fast Path";
    buffer[idx++]="GC Alloc Fast Path";
#endif
}

