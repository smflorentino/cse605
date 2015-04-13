# 1 "fivmr_arith_helpers.c"
/*
 * fivmr_arith_helpers.c
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

/* out-of-line helpers for arithmetic */

#include <fivmr.h>
#include <inttypes.h>
#include <math.h>

/* FIXME: include casts */

/* int helpers */

#if defined(FIVMR_NEED_INT_ADD_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_add(int32_t a,int32_t b) {
    return a+b;
}
#endif

#if defined(FIVMR_NEED_INT_SUB_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_sub(int32_t a,int32_t b) {
    return a-b;
}
#endif

#if defined(FIVMR_NEED_INT_MUL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_mul(int32_t a,int32_t b) {
    return a*b;
}
#endif

#if defined(FIVMR_NEED_INT_DIV_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_div(int32_t a,int32_t b) {
    if (a==-2147483647-1 && b==-1) {
	return -2147483647-1;
    } else {
	return a/b;
    }
}
#endif

#if defined(FIVMR_NEED_INT_MOD_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_mod(int32_t a,int32_t b) {
    if (a==-2147483647-1 && b==-1) {
	return 0;
    } else {
	return a%b;
    }
}
#endif

#if defined(FIVMR_NEED_INT_NEG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_neg(int32_t a) {
    return -a;
}
#endif

#if defined(FIVMR_NEED_INT_SHL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_shl(int32_t a,int32_t b) {
    return a<<(b&0x1f);
}
#endif

#if defined(FIVMR_NEED_INT_SHR_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_shr(int32_t a,int32_t b) {
    return a>>(b&0x1f);
}
#endif

#if defined(FIVMR_NEED_INT_USHR_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_ushr(int32_t a,int32_t b) {
    return (int32_t)(((uint32_t)a)>>((uint32_t)b));
}
#endif

#if defined(FIVMR_NEED_INT_AND_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_and(int32_t a,int32_t b) {
    return a&b;
}
#endif

#if defined(FIVMR_NEED_INT_OR_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_or(int32_t a,int32_t b) {
    return a|b;
}
#endif

#if defined(FIVMR_NEED_INT_XOR_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_xor(int32_t a,int32_t b) {
    return a^b;
}
#endif

#if defined(FIVMR_NEED_INT_COMPAREG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_compareG(int32_t a,int32_t b) {
    if (a<b) {
	return -1;
    } else if (a==b) {
	return 0;
    } else {
	return 1;
    }
}
#endif

#if defined(FIVMR_NEED_INT_COMPAREL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_compareL(int32_t a,int32_t b) {
    if (a>b) {
	return 1;
    } else if (a==b) {
	return 0;
    } else {
	return -1;
    }

}
#endif

#if defined(FIVMR_NEED_INT_LESSTHAN_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_lessThan(int32_t a,int32_t b) {
    return a<b;
}
#endif

#if defined(FIVMR_NEED_INT_ULESSTHAN_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_uLessThan(int32_t a,int32_t b) {
    return (int32_t)(((uint32_t)a)<((uint32_t)b));
}
#endif

#if defined(FIVMR_NEED_INT_EQ_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_eq(int32_t a,int32_t b) {
    return a==b;
}
#endif

#if defined(FIVMR_NEED_INT_NOT_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_not(int32_t a) {
    return !a;
}
#endif

#if defined(FIVMR_NEED_INT_BITNOT_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_int_bitNot(int32_t a) {
    return ~a;
}
#endif

/* long helpers */

#if defined(FIVMR_NEED_LONG_ADD_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_add(int64_t a,int64_t b) {
    return a+b;
}
#endif

#if defined(FIVMR_NEED_LONG_SUB_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_sub(int64_t a,int64_t b) {
    return a-b;
}
#endif

#if defined(FIVMR_NEED_LONG_MUL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_mul(int64_t a,int64_t b) {
    return a*b;
}
#endif

#if defined(FIVMR_NEED_LONG_DIV_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_div(int64_t a,int64_t b) {
    if (a==INT64_C(-9223372036854775807)-INT64_C(1) && b==-1) {
	return a;
    } else {
	return a/b;
    }
}
#endif

#if defined(FIVMR_NEED_LONG_MOD_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_mod(int64_t a,int64_t b) {
    if (a==INT64_C(-9223372036854775807)-INT64_C(1) && b==-1) {
	return 0;
    } else {
	return a%b;
    }
}
#endif

#if defined(FIVMR_NEED_LONG_NEG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_neg(int64_t a) {
    return -a;
}
#endif

#if defined(FIVMR_NEED_LONG_SHL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_shl(int64_t a,int32_t b) {
    return a<<(b&0x1f);
}
#endif

#if defined(FIVMR_NEED_LONG_SHR_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_shr(int64_t a,int32_t b) {
    return a>>(b&0x1f);
}
#endif

#if defined(FIVMR_NEED_LONG_USHR_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_ushr(int64_t a,int32_t b) {
    return (int64_t)(((uint64_t)a)>>((uint64_t)b));
}
#endif

#if defined(FIVMR_NEED_LONG_AND_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_and(int64_t a,int64_t b) {
    return a&b;
}
#endif

#if defined(FIVMR_NEED_LONG_OR_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_or(int64_t a,int64_t b) {
    return a|b;
}
#endif

#if defined(FIVMR_NEED_LONG_XOR_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_xor(int64_t a,int64_t b) {
    return a^b;
}
#endif

#if defined(FIVMR_NEED_LONG_COMPAREG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_long_compareG(int64_t a,int64_t b) {
    if (a<b) {
	return -1;
    } else if (a==b) {
	return 0;
    } else {
	return 1;
    }
}
#endif

#if defined(FIVMR_NEED_LONG_COMPAREL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_long_compareL(int64_t a,int64_t b) {
    if (a>b) {
	return 1;
    } else if (a==b) {
	return 0;
    } else {
	return -1;
    }

}
#endif

#if defined(FIVMR_NEED_LONG_LESSTHAN_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_long_lessThan(int64_t a,int64_t b) {
    return a<b;
}
#endif

#if defined(FIVMR_NEED_LONG_ULESSTHAN_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_long_uLessThan(int64_t a,int64_t b) {
    return (int32_t)(((uint64_t)a)<((uint64_t)b));
}
#endif

#if defined(FIVMR_NEED_LONG_EQ_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_long_eq(int64_t a,int64_t b) {
    return a==b;
}
#endif

#if defined(FIVMR_NEED_LONG_NOT_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_not(int64_t a) {
    return !a;
}
#endif

#if defined(FIVMR_NEED_LONG_BITNOT_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_long_bitNot(int64_t a) {
    return ~a;
}
#endif

/* float helpers */

#if defined(FIVMR_NEED_FLOAT_ADD_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
float fivmr_AH_float_add(float a,float b) {
    return a+b;
}
#endif

#if defined(FIVMR_NEED_FLOAT_SUB_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
float fivmr_AH_float_sub(float a,float b) {
    return a-b;
}
#endif

#if defined(FIVMR_NEED_FLOAT_MUL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
float fivmr_AH_float_mul(float a,float b) {
    return a*b;
}
#endif

#if defined(FIVMR_NEED_FLOAT_DIV_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
float fivmr_AH_float_div(float a,float b) {
    return a/b;
}
#endif

#if defined(FIVMR_NEED_FLOAT_MOD_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
float fivmr_AH_float_mod(float a,float b) {
    return fmodf(a,b);
}
#endif

#if defined(FIVMR_NEED_FLOAT_NEG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
float fivmr_AH_float_neg(float a) {
    return -a;
}
#endif

#if defined(FIVMR_NEED_FLOAT_COMPAREG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_float_compareG(float a,float b) {
    if (a<b) {
	return -1;
    } else if (a==b) {
	return 0;
    } else {
	return 1;
    }
}
#endif

#if defined(FIVMR_NEED_FLOAT_COMPAREL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_float_compareL(float a,float b) {
    if (a>b) {
	return 1;
    } else if (a==b) {
	return 0;
    } else {
	return -1;
    }

}
#endif

#if defined(FIVMR_NEED_FLOAT_LESSTHAN_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_float_lessThan(float a,float b) {
    return a<b;
}
#endif

#if defined(FIVMR_NEED_FLOAT_EQ_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_float_eq(float a,float b) {
    return a==b;
}
#endif

/* double helpers */

#if defined(FIVMR_NEED_DOUBLE_ADD_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
double fivmr_AH_double_add(double a,double b) {
    return a+b;
}
#endif

#if defined(FIVMR_NEED_DOUBLE_SUB_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
double fivmr_AH_double_sub(double a,double b) {
    return a-b;
}
#endif

#if defined(FIVMR_NEED_DOUBLE_MUL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
double fivmr_AH_double_mul(double a,double b) {
    return a*b;
}
#endif

#if defined(FIVMR_NEED_DOUBLE_DIV_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
double fivmr_AH_double_div(double a,double b) {
    return a/b;
}
#endif

#if defined(FIVMR_NEED_DOUBLE_MOD_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
double fivmr_AH_double_mod(double a,double b) {
    return fmod(a,b);
}
#endif

#if defined(FIVMR_NEED_DOUBLE_NEG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
double fivmr_AH_double_neg(double a) {
    return -a;
}
#endif

#if defined(FIVMR_NEED_DOUBLE_COMPAREG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_double_compareG(double a,double b) {
    if (a<b) {
	return -1;
    } else if (a==b) {
	return 0;
    } else {
	return 1;
    }
}
#endif

#if defined(FIVMR_NEED_DOUBLE_COMPAREL_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_double_compareL(double a,double b) {
    if (a>b) {
	return 1;
    } else if (a==b) {
	return 0;
    } else {
	return -1;
    }

}
#endif

#if defined(FIVMR_NEED_DOUBLE_LESSTHAN_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_double_lessThan(double a,double b) {
    return a<b;
}
#endif

#if defined(FIVMR_NEED_DOUBLE_EQ_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_double_eq(double a,double b) {
    return a==b;
}
#endif

#if defined(FIVMR_NEED_INT_TO_FLOAT_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
float fivmr_AH_int_to_float(int32_t a) {
    return (float)a;
}
#endif

#if defined(FIVMR_NEED_FLOAT_TO_INT_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_float_to_int(float a) {
    if (a!=a) {
        return 0;
    } else if (a==1./0.) {
        return INT32_C(2147483647);
    } else if (a==-1./0.) {
        return INT32_C(-2147483647)-1;
    } else {
        return (int32_t)a;
    }
}
#endif

#if defined(FIVMR_NEED_INT_TO_DOUBLE_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
double fivmr_AH_int_to_double(int32_t a) {
    return (double)a;
}
#endif

#if defined(FIVMR_NEED_DOUBLE_TO_INT_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int32_t fivmr_AH_double_to_int(double a) {
    if (a!=a) {
        return 0;
    } else if (a==1./0.) {
        return INT32_C(2147483647);
    } else if (a==-1./0.) {
        return INT32_C(-2147483647)-1;
    } else {
        return (int32_t)a;
    }
}
#endif

#if defined(FIVMR_NEED_LONG_TO_FLOAT_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
float fivmr_AH_long_to_float(int64_t a) {
    return (float)a;
}
#endif

#if defined(FIVMR_NEED_FLOAT_TO_LONG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_float_to_long(float a) {
    if (a!=a) {
        return 0;
    } else if (a==1./0.) {
        return INT64_C(9223372036854775807);
    } else if (a==-1./0.) {
        return INT64_C(-9223372036854775807)-1;
    } else {
        return (int64_t)a;
    }
}
#endif

#if defined(FIVMR_NEED_LONG_TO_DOUBLE_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
double fivmr_AH_long_to_double(int64_t a) {
    return (double)a;
}
#endif

#if defined(FIVMR_NEED_DOUBLE_TO_LONG_HELPER) || defined(FIVMR_NEED_ALL_HELPERS)
int64_t fivmr_AH_double_to_long(double a) {
    if (a!=a) {
        return 0;
    } else if (a==1./0.) {
        return INT64_C(9223372036854775807);
    } else if (a==-1./0.) {
        return INT64_C(-9223372036854775807)-1;
    } else {
        return (int64_t)a;
    }
}
#endif


# 1 "fivmr_cmrgc.c"
/*
 * fivmr_cmrgc.c
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

#include <fivmr_config.h>

#if !FIVMBUILD__SPECIAL_GC || FIVMBUILD__CMRGC || FIVMBUILD__HFGC

#include <fivmr.h>
#include <fivmr_logger.h>

static inline fivmr_Nanos fivmr_debugTime(void) {
    if (true) {
        return 0;
    } else {
        return fivmr_curTime();
    }
}

static inline bool shouldQuit(fivmr_GC *gc) {
    return fivmr_exiting(fivmr_VMfromGC(gc));
}

static uintptr_t mogrifyHeapSize(fivmr_Settings *settings,
                                 uintptr_t size) {
    if (FIVMR_HFGC(settings)) {
        return (10*size)/13;
    } else {
        return size;
    }
}

/* NEVER call this method while holding gcLock */
static void collectInvoluntarilyFromNative(fivmr_GC *gc,
                                           const char *descrIn,
                                           const char *descrWhat) {
    fivmr_Nanos before;
    fivmr_ThreadState *ts;
    bool shouldTrigger;

    if (fivmr_Thread_isInterrupt()) {
	LOG(1,("collectInvoluntarilyFromNative called from an interrupt."));
	return;
    }

    before=fivmr_curTime();
    
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "blocking involuntarily on GC at %u:%u",2,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff));
    }
    if (gc->logSyncGC) {
        fivmr_Log_lockedPrintf("[Thread %u waiting on Sync GC in %s%s%s]\n",
                               ts->id,descrIn,
                               (descrWhat==NULL?"":", allocating "),
                               (descrWhat==NULL?"":descrWhat));
    }
    
    fivmr_Lock_lock(&gc->gcLock);
    
    ts->gc.gcFlags|=FIVMR_GCDF_REQUESTED_GC;
    shouldTrigger=!fivmr_GC_hasBeenTriggered(gc);
    ts->gc.requesterNext=gc->waiterHead;
    gc->waiterHead=ts;
    
    fivmr_Lock_unlock(&gc->gcLock);

    if (shouldTrigger) {
        LOG(11,("Sending GC trigger."));
        fivmr_CritSemaphore_up(&gc->triggerSema);
        LOG(11,("Trigger sent!"));
    }

    fivmr_Lock_lock(&gc->notificationLock);
    while ((ts->gc.gcFlags&FIVMR_GCDF_REQUESTED_GC) && !shouldQuit(gc)) {
        fivmr_Lock_wait(&gc->notificationLock);
    }
    fivmr_Lock_unlock(&gc->notificationLock);
    
    LOG(2,("Thread %u woke up after involuntary synchronous GC request.",ts->id));
    gc->invBlockTime+=fivmr_curTime()-before;
}

static inline bool markUnmarked(fivmr_GC *gc,
                                fivmr_GCHeader **queue,
                                fivmr_GCHeader *hdr,
                                fivmr_GCHeader *oldHdr,
                                uintptr_t curShaded) {
    fivmr_GCHeader newHdr;
    fivmr_GCHeader_set(&newHdr,curShaded,*queue);
    if (fivmr_GCHeader_cas(hdr,oldHdr,&newHdr)) {
        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_xchg_add(&gc->numMarked,1);
        }
        *queue=hdr;
        return true;
    } else {
        fivmr_assert(fivmr_GCHeader_markBits(hdr)&curShaded);
        return false;
    }
}

static inline bool mark(fivmr_GC *gc,
                        fivmr_GCHeader **queue,
			fivmr_GCHeader *hdr) {
    fivmr_GCHeader oldHdr;
    uintptr_t curShaded;
    fivmr_Settings *settings=&fivmr_VMfromGC(gc)->settings;
    fivmr_TypeData *td;
    /* make sure we're marking an object that is either immortal or is in the heap */
    if (FIVMR_ASSERTS_ON &&
        FIVMR_SELF_MANAGE_MEM(settings) &&
        (hdr->word&FIVMR_GC_MARKBITS_MASK)!=FIVMR_GC_MARKBITS_MASK) {
        fivmr_assert((uintptr_t)hdr>=gc->memStart);
        fivmr_assert((uintptr_t)hdr<gc->memEnd);
    }
    /* make sure that the object has a valid-looking type data */
    fivmr_assert(fivmr_TypeData_forObject(
                     settings,
                     fivmr_GCHeader_toObject(
                         settings,
                         hdr))->state
                 ==FIVMR_MS_INVALID);
    oldHdr=*hdr;
    curShaded=gc->curShaded;
    if ((oldHdr.word&curShaded)==0) {
        return markUnmarked(gc,queue,hdr,&oldHdr,curShaded);
    } else {
        return false;
    }
}

static inline void collectorMarkObj(fivmr_GC *gc,
                                    fivmr_GCHeader **queue,
                                    fivmr_Object obj) {
    if (obj) {
        if (0) printf("collector marking obj %p\n",(void*)obj);
	mark(gc,queue,
             fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                       obj));
    }
}

static inline void collectorMarkObjForScan(fivmr_VM *vm,
                                           fivmr_Object *obj,
                                           void *arg) {
    fivmr_GCHeader **queue=(fivmr_GCHeader**)arg;
    collectorMarkObj(&vm->gc,queue,*obj);
}

static inline void collectorMarkHandle(fivmr_GC *gc,
                                       fivmr_GCHeader **queue,
				       fivmr_Handle *h) {
    if (h!=NULL) {
	fivmr_assert(h->obj!=0);
        if (0) printf("collector marking handle %p\n",(void*)h->obj);
	mark(gc,queue,
             fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                       h->obj));
    }
}

static inline bool mutatorMark(fivmr_ThreadState *ts,
			       fivmr_GCHeader *hdr) {
    fivmr_assert(ts->execStatus==FIVMR_TSES_IN_JAVA ||
		 ts->execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK ||
		 (ts->execFlags&FIVMR_TSEF_BLOCKING) ||
		 fivmr_ThreadHandle_current()==ts->vm->gc.thread);
    if (0) printf("mutator marking %p\n",hdr);
    if (mark(&ts->vm->gc,&ts->gc.queue,hdr)) {
	if (ts->gc.queueTail==NULL) {
	    ts->gc.queueTail=hdr;
	}
	return true;
    } else {
	return false;
    }
}

static inline bool mutatorMarkObj(fivmr_ThreadState *ts,
				  fivmr_Object obj) {
    if (obj) {
	return mutatorMark(ts,
                           fivmr_GCHeader_fromObject(&ts->vm->settings,
                                                     obj));
    } else {
	return false;
    }
}

static inline bool mutatorMarkObjNoisy(fivmr_ThreadState *ts,
				       fivmr_Object obj) {
    if (LOGGING(4)) {
	if (obj) {
	    LOG(4,("Thread %u marking %p (of type %s)",
		   ts->id,obj,fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
	} else {
	    LOG(4,("Thread %u marking null",
		   ts->id));
	}
    }
    if (mutatorMarkObj(ts,obj)) {
	LOG(3,("Thread %u marked %p (of type %s)",
	       ts->id,obj,fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
	return true;
    } else {
	return false;
    }
}

static inline bool mutatorMarkObjStackNoisy(fivmr_ThreadState *ts,
					    fivmr_Object obj) {
    fivmr_GCHeader *hdr;
    LOG(3,("Thread %u marking %p",ts->id,obj));
    if (!obj) {
        LOG(4,("Thread %u marking null",
               ts->id));
        return false;
    }
    if (false) fivmr_assert(obj<0x80000000); /* useful assertion on RTEMS */
    hdr=fivmr_GCHeader_fromObject(&ts->vm->settings,
                                  obj);
    if (false) {
        /* make sure the object is in the heap; this is another assertion
           that may or may not be correct in the general case but is useful
           to enable when debugging gnarly GC bugs on RTEMS */
        if (FIVMR_ASSERTS_ON &&
            FIVMR_SELF_MANAGE_MEM(&ts->vm->settings) &&
            (hdr->word&FIVMR_GC_MARKBITS_MASK)!=FIVMR_GC_MARKBITS_MASK) {
            fivmr_assert((uintptr_t)hdr>=ts->vm->gc.memStart);
            fivmr_assert((uintptr_t)hdr<ts->vm->gc.memEnd);
        }
    }
    if (!fivmr_GCHeader_isScopeID(hdr)) {
	if (mutatorMarkObjNoisy(ts,obj)) {
            LOG(3,("Thread %u marked %p",ts->id,obj));
            return true;
        }
    } else {
	LOG(4,("Thread %u skipped %p (of type %s)",
	       ts->id,obj,fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
    }
    return false;
}

static inline void mutatorMarkObjStackNoisyForScan(fivmr_VM *vm,
                                                   fivmr_Object *obj,
                                                   void *arg) {
    fivmr_ThreadState *ts=(fivmr_ThreadState*)arg;
    mutatorMarkObjStackNoisy(ts,*obj);
}

static inline bool mutatorMarkHandle(fivmr_ThreadState *ts,
				     fivmr_Handle *h) {
    if (h!=NULL) {
	fivmr_assert(h->obj!=0);
	return mutatorMark(ts,
                           fivmr_GCHeader_fromObject(&ts->vm->settings,
                                                     h->obj));
    } else {
	return false;
    }
}

static inline void mutatorMarkHandleNoisy(fivmr_ThreadState *ts,
					  fivmr_Handle *h) {
    if (LOGGING(4)) {
	if (h!=NULL) {
	    LOG(4,("Thread %u marking handle %p, pointing to %p (of type %s)",
		   ts->id,h,h->obj,
                   fivmr_TypeData_forObject(&ts->vm->settings,h->obj)->name));
	} else {
	    LOG(4,("Thread %u marking null handle",
		   ts->id));
	}
    }
    if (mutatorMarkHandle(ts,h)) {
	LOG(3,("Thread %u marked handle %p, pointing to %p (of type %s)",
	       ts->id,h,h->obj,
               fivmr_TypeData_forObject(&ts->vm->settings,h->obj)->name));
    }
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_markSlow)(fivmr_ThreadState *ts,
                                               fivmr_Object obj) {
    fivmr_SPC_incBarrierSlowPath();
    
    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "mark trap at %u:%u on %u",3,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff),
               obj);
    }
    
    fivmr_assert(ts->execStatus==FIVMR_TSES_IN_JAVA ||
		 ts->execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK);
    fivmr_assert(obj);

    if (FIVMR_ASSERTS_ON || FIVMR_LOG_LEVEL>=2) {
        fivmr_GCPhase curPhase;
        curPhase=ts->vm->gc.phase;
    
        /* this code is more verbose and careful */
        
        /* should never see stack-allocated objects */
        if (!FIVMR_HFGC_FAIL_FAST_PATHS(&ts->vm->settings)) {
            fivmr_assert(
                fivmr_GCHeader_markBits(
                    fivmr_GCHeader_fromObject(&ts->vm->settings,
                                              obj))!=0);
        }

        /* we don't want to mark when we're idle because during the idle phase
           at the beginning of GC we may be rotating mark bits.  this prevents
           races where two threads end up double-marking an object because they
           see different values of curShaded.
        
           this is particularly why we query ts->gc.tracing.  it's possible that
           a thread will have used stale shade values in its fast path check.
           worse, memory model issues could mean that the value of gc->curShaded
           we see here could be stale.  we don't want to perform marking with a
           stale value of curShaded.  checking ts->gc.tracing, which is set by
           safepoints and hence side-steps cache coherence, ensures that we only
           proceed when we have the latest shade values. */
        if (fivmr_GCHeader_markBits(
                fivmr_GCHeader_fromObject(&ts->vm->settings,
                                          obj))!=0) {
            if (ts->gc.tracing && fivmr_GC_isTracing(curPhase)) {
                if (mutatorMark(ts,
                                fivmr_GCHeader_fromObject(&ts->vm->settings,
                                                          obj))) {
                    LOG(3,("Thread %u barrier marked %p (of type %s)",
                           ts->id,obj,
                           fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
                }
            } else if (curPhase==FIVMR_GCP_PRE_INIT || fivmr_GC_isTracing(curPhase)) {
                LOG(3,("Thread %u barrier would have marked marked %p (of type %s); "
                       "phase = %d (now %d), status = %" PRIuPTR,
                       ts->id,obj,
                       fivmr_TypeData_forObject(&ts->vm->settings,obj)->name,
                       curPhase,ts->vm->gc.phase,ts->execStatus));
            } else if (!FIVMR_HFGC_FAIL_FAST_PATHS(&ts->vm->settings)) {
                LOG(0,("Thread %u barrier incorrectly saw %p (of type %s)",
                       ts->id,obj,
                       fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
                fivmr_abort("barrier error");
            }
        }
    } else {
        /* this code is carefully written to ensure performance, but doesn't
           do the same verification as the above. */
        if (ts->gc.tracing) {
            fivmr_GCHeader *hdr;
            fivmr_GCHeader oldHdr;
            uintptr_t markBits;
            uintptr_t curShaded;
            fivmr_GC *gc;
            
            gc=&ts->vm->gc;
            
            hdr = fivmr_GCHeader_fromObject(&ts->vm->settings,
                                            obj);
            oldHdr = *hdr;
            markBits = fivmr_GCHeader_markBits(&oldHdr);
            curShaded = gc->curShaded;
            
            if (markBits!=0 &&
                (markBits&curShaded)==0 &&
                markUnmarked(gc,
                             &ts->gc.queue,
                             hdr,
                             &oldHdr,
                             curShaded) &&
                ts->gc.queueTail==NULL) {
                ts->gc.queueTail=hdr;
            }
        }
    }
}

static void addPageToFreelist(fivmr_GC *gc,
                              uintptr_t pageAddr) {
    fivmr_FreePage *page=(fivmr_FreePage*)pageAddr;
    LOG(5,("Adding page to freelist: %p",pageAddr));
    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        fivmr_PageTable_setAssert(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                  pageAddr,
                                  FIVMR_GCPS_ZERO,
                                  FIVMR_GCPS_FREE);
        page->next=gc->freePageHead;
        page->prev=NULL;
        if (gc->freePageHead!=NULL) {
            gc->freePageHead->prev=page;
        }
        gc->freePageHead=page;
    } else {
        page->next=gc->freePageHead;
        gc->freePageHead=page;
    }
}

/* call only while holding the lock */
static void addPagesToFreelist(fivmr_GC *gc,
                               void *start,
			       uintptr_t numPages) {
    uintptr_t i;
    
    gc->numFreePages+=numPages;
    fivmr_assert(gc->numPagesUsed>=0);
    fivmr_assert(gc->numFreePages>=0);
    for (i=0;i<numPages;++i) {
	addPageToFreelist(gc,((uintptr_t)start)+FIVMR_PAGE_SIZE*i);
    }
}

/* call only while holding the lock */
static void *allocPagesToFreelist(fivmr_GC *gc,
                                  uintptr_t numPages) {
    void *start;
    start=fivmr_tryAllocPages(numPages,NULL);
    if (start!=NULL) {
        addPagesToFreelist(gc,start,numPages);
    }
    return start;
}

/* FIXME: the current page management system will lead to priority inversion
   if a low-priority thread makes a request for a large amount of memory that
   cannot be immediately satisfied, and a high-priority thread then tries
   to request a small amount of memory, that could have been immediately
   satisfied. */

static fivmr_FreePage *getPage(fivmr_GC *gc) {
    fivmr_FreePage *result=NULL;
    fivmr_ThreadState *ts;
    fivmr_GCSpace space;
    bool asyncRequested=false;
    
    fivmr_Nanos before=fivmr_debugTime();

    result=NULL;
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));

    LOG(6,("used pages = %p, max pages = %p",
	   gc->numPagesUsed,gc->maxPagesUsed));
    
    if (gc->numPagesUsed==(intptr_t)gc->maxPagesUsed) {
	goto done;
    }
    fivmr_assert(gc->numPagesUsed<=(intptr_t)gc->maxPagesUsed);
    if (gc->numPagesUsed!=(intptr_t)gc->maxPagesUsed) {
	result=gc->freePageHead;
	LOG(5,("Removing page from freelist: %p",result));
        if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            if (result==NULL && gc->nextFreePage<gc->memEnd) {
                fivmr_assert((gc->nextFreePage&(FIVMR_PAGE_SIZE-1))==0);
                result=(fivmr_FreePage*)gc->nextFreePage;
                gc->nextFreePage+=FIVMR_PAGE_SIZE;
                fivmr_assert(
                    fivmr_PageTable_get(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                        (uintptr_t)result)==FIVMR_GCPS_ZERO);
                result->dirty=!gc->isZero;
                gc->numFreePages++; /* hack */
            } else {
                if (false && FIVMR_ASSERTS_ON && result==NULL) {
                    uintptr_t curPage;
                    for (curPage=gc->memStart;
                         curPage<gc->memEnd;
                         curPage+=FIVMR_PAGE_SIZE) {
                        fivmr_assert(
                            fivmr_PageTable_get(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                                curPage)!=FIVMR_GCPS_FREE);
                    }
                }
                fivmr_assert(result!=NULL);
                fivmr_PageTable_setAssert(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                          (uintptr_t)result,
                                          FIVMR_GCPS_FREE,
                                          FIVMR_GCPS_ZERO);
                gc->freePageHead=result->next;
                if (result->next!=NULL) {
                    result->next->prev=NULL;
                }
            }
        } else {
            if (result==NULL) {
                if (allocPagesToFreelist(gc,gc->reqPages)==NULL) {
                    goto done;
                }
                result=gc->freePageHead;
                fivmr_assert(result!=NULL);
            }
            gc->freePageHead=result->next;
        }
	if (gc->numPagesUsed==(intptr_t)gc->gcTriggerPages) {
	    fivmr_GC_asyncCollect(gc);
            asyncRequested=true;
	}
	gc->numPagesUsed++;
        fivmr_assert(gc->numPagesUsed<=(intptr_t)gc->maxPagesUsed);
	gc->numFreePages--;
        fivmr_assert(gc->numPagesUsed>=0);
        fivmr_assert(gc->numFreePages>=0);
    } else {
	LOG(1,("Out of memory on small object request: pages used = %p, max pages = %p",
	       gc->numPagesUsed,gc->maxPagesUsed));
    }

    for (space=0;space<FIVMR_GC_NUM_GC_SPACES;++space) {
	fivmr_assert(fivmr_PageTable_get(&gc->spaceData[space].pt,
					 (uintptr_t)result)
		     ==FIVMR_GCPS_ZERO);
    }
    
    if (result!=NULL && result->dirty) {
	bzero(result,FIVMR_PAGE_SIZE);
	fivmr_fence();
    }
    
    if (asyncRequested) {
        LOG(1,("Back in Java code after asynchronous collection request, returning page %p.",
               result));
    }
    LOG(3,("getPage returning %p.",result));
   
    gc->getPageTime+=fivmr_debugTime()-before;

done: 
    return result;
}

static uintptr_t zeroFreeLines(fivmr_FreeLine *head,
                               fivmr_GC *gc) {
    fivmr_FreeLine *cur;
    uintptr_t cnt=0;
    for (cur=head;cur!=NULL;) {
	fivmr_FreeLine *next;
	fivmr_UsedPage *up;
	next=cur->next;
	up=(fivmr_UsedPage*)(((uintptr_t)cur)&~(FIVMR_PAGE_SIZE-1));
	*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings)=FIVMR_GCUPS_ZERO;
	cnt+=cur->size;
	fivmr_FreeLine_zero(cur);
	cur=next;
    }
    return cnt;
}

static void removeFreeLinesInPage(fivmr_GC *gc,
                                  fivmr_UsedPage *up) {
    if ((*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings))
        & FIVMR_GCUPS_FREE_LINES) {
	LOG(3,("Page %p has status %p",
               up,*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings)));
	fivmr_Lock_lock(&gc->gcLock);
	/* recheck the status; it may have changed. */
	if ((*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings))
            & FIVMR_GCUPS_FREE_LINES) {
	    fivmr_FreeLine *first;
	    fivmr_FreeLine *last;
            fivmr_FreeLine *cur;
            uintptr_t totalSize=0;
	    first=(fivmr_FreeLine*)(
                (uintptr_t)up+
                ((*fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings))
                 & ~FIVMR_GCUPS_STAT_MASK));
	    last=first->lastOnPage;
            for (cur=first;;cur=cur->next) {
                fivmr_GCSpace i;
                totalSize+=cur->size;
                if (cur==last) break; /* ok! */
                fivmr_assert(cur!=NULL);
                for (i=0;i<FIVMR_GC_NUM_GC_SPACES;++i) {
                    fivmr_assert(cur!=&gc->spaceData[i].freeLineTail);
                }
                fivmr_assert((((uintptr_t)cur)&~(FIVMR_PAGE_SIZE-1))==(uintptr_t)up);
            }
	    first->prev->next=last->next;
	    last->next->prev=first->prev;
            first->prev=NULL;
	    last->next=NULL;
	    *fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings)=FIVMR_GCUPS_ZERO;
	}
	fivmr_Lock_unlock(&gc->gcLock);
    }
}

static inline bool canSatisfy(fivmr_Settings *settings,
                              fivmr_FreeLine *line,
                              uintptr_t size,
                              uintptr_t alignStart,
                              uintptr_t align) {
    if (size<=line->size) {
        uintptr_t allocStart=(uintptr_t)line+FIVMR_ALLOC_OFFSET(settings);
        uintptr_t allocSize=line->size;
        return (fivmr_alignRaw(allocStart+alignStart,align)
                -alignStart+size)-allocStart <= allocSize;
    } else {
        return false;
    }
}

static inline bool anyOnPageCanSatisfy(fivmr_Settings *settings,
                                       fivmr_FreeLine *head,
                                       uintptr_t size,
                                       uintptr_t alignStart,
                                       uintptr_t align) {
    if (FIVMR_HFGC(settings)) {
        fivmr_assert(size==FIVMR_GC_BLOCK_SIZE);
        fivmr_assert(canSatisfy(settings,head,size,alignStart,align));
        return true;
    } else {
        fivmr_FreeLine *cur;
        for (cur=head;;cur=cur->next) {
            if (canSatisfy(settings,cur,size,alignStart,align)) {
                return true;
            }
            
            if (cur==cur->lastOnPage) {
                break;
            }
        }
        return false;
    }
}

static bool getPageOfFreeLines(fivmr_GC *gc,
                               fivmr_GCSpace space,
			       fivmr_UsedPage **upPtr,
			       fivmr_FreeLine **headPtr,
			       fivmr_FreeLine **tailPtr,
                               uintptr_t size,
                               uintptr_t alignStart,
                               uintptr_t align) {
    bool result=false;
    fivmr_Nanos before=fivmr_debugTime();
    for (;;) {
	fivmr_FreeLine *head;
        int cnt=0;
        head=gc->spaceData[space].freeLineHead.next;
        for (;;) {
            if (head!=&gc->spaceData[space].freeLineTail) {
                fivmr_UsedPage *up;
                fivmr_FreeLine *tail;
                tail=head->lastOnPage;
                fivmr_assert((((uintptr_t)tail)&~(FIVMR_PAGE_SIZE-1))==
                             (((uintptr_t)head)&~(FIVMR_PAGE_SIZE-1)));
                fivmr_assert((((uintptr_t)tail->next)&~(FIVMR_PAGE_SIZE-1))!=
                             (((uintptr_t)tail)&~(FIVMR_PAGE_SIZE-1)));
                if (anyOnPageCanSatisfy(&fivmr_VMfromGC(gc)->settings,
                                        head,size,alignStart,align)) {
                    up=(fivmr_UsedPage*)(((uintptr_t)head)&~(FIVMR_PAGE_SIZE-1));
                    removeFreeLinesInPage(gc,up);
                    for (;;) {
                        uint8_t pageState;
                        pageState=fivmr_PageTable_get(&gc->spaceData[space].pt,
                                                      (uintptr_t)up);
                        if (pageState==FIVMR_GCPS_ZERO) {
                            /* race of some kind?  the interaction between this an reusePage()
                               is bizarre as heck. */
                            LOG(2,("Thread %u skipping page %p because the sweep has freed it.",
                                   fivmr_ThreadState_get(fivmr_VMfromGC(gc))->id,up));
                            break;
                        }
                        /* note: page should not be in RELINQUISHED state, since that
                           would mean that the page cannot have free lines */
                        fivmr_assert(pageState==FIVMR_GCPS_POPULATED ||
                                     pageState==FIVMR_GCPS_SHADED);
                        fivmr_assert(result==false);
                        if (fivmr_PageTable_cas(&gc->spaceData[space].pt,
                                                (uintptr_t)up,
                                                pageState,
                                                FIVMR_GCPS_ZERO)) {
                            result=true;
                            break;
                        }
                    }
                    if (result) {
                        *upPtr=up;
                        *headPtr=head;
                        *tailPtr=tail;
                        LOG(4,("Getting page %p for line allocation",up));
                        goto done;
                    } else {
                        break;
                    }
                } else {
                    /* FIXME: CMR should use a max-heap to store pages of free lines. */
                    cnt++;
                    if (cnt>5) {
                        result=false;
                        goto done;
                    }
                    head=tail->next;
                }
            } else {
                result=false;
                goto done;
            }
        }
    }
done:
    gc->getFreeLinesTime+=fivmr_debugTime()-before;
    return result;
}

typedef struct {
    fivmr_FreePage *head;
    fivmr_FreePage *tail;
    uintptr_t numPages;
} BunchOfPages;

static void initBunchOfPages(BunchOfPages *bop) {
    bop->head=NULL;
    bop->tail=NULL;
    bop->numPages=0;
}

static void reuseBunchOfPages(fivmr_GC *gc,
                              BunchOfPages *bop) {
    if (bop->numPages) {
	fivmr_FreePage *cur;
	uintptr_t count=0;

	fivmr_assert(bop->head!=NULL);
	fivmr_assert(bop->tail!=NULL);
	fivmr_assert(bop->numPages!=1 || bop->head==bop->tail);
	
	fivmr_Lock_lock(&gc->gcLock);
        if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            for (cur=bop->head;cur!=NULL;cur=cur->next) {
                fivmr_assert((cur->next==NULL && cur==bop->tail) ||
                             cur->next->prev==cur);
                fivmr_assert((cur->prev==NULL && cur==bop->head) ||
                             cur->prev->next==cur);
                fivmr_PageTable_setAssert(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                          (uintptr_t)cur,
                                          FIVMR_GCPS_ZERO,
                                          FIVMR_GCPS_FREE);
                count++;
            }
            fivmr_assert(count==bop->numPages);
            fivmr_assert(bop->head->prev==NULL);
            fivmr_assert(bop->tail->next==NULL);
            bop->tail->next=gc->freePageHead;
            if (gc->freePageHead!=NULL) {
                gc->freePageHead->prev=bop->tail;
            }
            gc->freePageHead=bop->head;
        } else {
            bop->tail->next=gc->freePageHead;
            gc->freePageHead=bop->head;
        }
	gc->numFreePages+=bop->numPages;
	gc->numPagesUsed-=bop->numPages;
        fivmr_assert(gc->numPagesUsed>=0);
        fivmr_assert(gc->numFreePages>=0);
	fivmr_Lock_unlock(&gc->gcLock);
	
	initBunchOfPages(bop);
    }
}

static void reusePage(fivmr_GC *gc,
                      BunchOfPages *bop,void *page_) {
    fivmr_UsedPage *usedPage=(fivmr_UsedPage*)page_;
    fivmr_FreePage *page=(fivmr_FreePage*)page_;
    
    removeFreeLinesInPage(gc,usedPage);
    
    page->dirty=true;

    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        page->next=bop->head;
        page->prev=NULL;
        if (bop->head!=NULL) {
            bop->head->prev=page;
        }
        bop->head=page;
    } else {
        page->next=bop->head;
        bop->head=page;
    }
    if (bop->tail==NULL) {
	bop->tail=page;
    }
    bop->numPages++;
    
    if (bop->numPages>gc->reusePages) {
	reuseBunchOfPages(gc,bop);
    }
}

/* helper for OOME logging. */
static void logOOME(fivmr_GC *gc,
                    const char *where,const char *description,
                    uintptr_t bytes,uintptr_t align) {
    if (gc->logGC) {
        fivmr_ThreadState *ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
        fivmr_Log_lockedPrintf("[OOME in %s for Thread %u, allocating %s: %" PRIuPTR " bytes with "
                               "%" PRIuPTR "-byte alignment]\n",
                               where,
                               ts->id,
                               description,
                               bytes,align);
        fivmr_ThreadState_dumpStackFor(ts);
    }
}

/* call only while holding the lock.  does not put the header into the list. 
   note that the allocated space is dirty if we're self-managing and zeroed
   otherwise. */
static fivmr_LargeObjectHeader *attemptAllocLargeObject(fivmr_GC *gc,
                                                        uintptr_t size,
                                                        fivmr_AllocEffort effort) {
    uintptr_t numPages;
    uintptr_t result;
    uintptr_t curPage;
    uintptr_t sizeInPages;
    
    numPages=fivmr_pages(size);
    
    if (gc->numPagesUsed+numPages > gc->maxPagesUsed) {
	LOG(1,("Out of memory on large object request: pages used = %p, "
	       "max pages = %p, pages requrested = %p",
	       gc->numPagesUsed,gc->maxPagesUsed,numPages));
    	return NULL;
    }

    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        sizeInPages=numPages*FIVMR_PAGE_SIZE;
        if (gc->nextFreePage+sizeInPages<=gc->memEnd) {
            result=gc->nextFreePage;
            fivmr_assert(
                fivmr_PageTable_get(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,result)
                ==FIVMR_GCPS_ZERO);
            gc->nextFreePage=result+sizeInPages;
            gc->numFreePages+=numPages; /* hack! */
            return (fivmr_LargeObjectHeader*)result;
        } else {
            /* this is horrifically inefficient.  that's ok for now. */
            for (curPage=gc->memStart;curPage<gc->memEnd;curPage+=FIVMR_PAGE_SIZE) {
                if (fivmr_PageTable_get(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,curPage)
                    ==FIVMR_GCPS_FREE) {
                    uintptr_t curPage2;
                    bool allGood=true;
                    LOG(5,("Attempting to reserve %p pages starting at %p",
                           numPages,curPage));
                    for (curPage2=curPage+FIVMR_PAGE_SIZE;
                         curPage2<curPage+(numPages<<FIVMSYS_LOG_PAGE_SIZE);
                         curPage2+=FIVMR_PAGE_SIZE) {
                        if (curPage2>=gc->memEnd ||
                            fivmr_PageTable_get(
                                &gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                curPage2)
                            !=FIVMR_GCPS_FREE) {
                            curPage=curPage2;
                            allGood=false;
                            break;
                        }
                    }
                    if (allGood) {
                        fivmr_assert(gc->numPagesUsed+numPages <= gc->maxPagesUsed);
                        for (curPage2=curPage;
                             curPage2<curPage+(numPages<<FIVMSYS_LOG_PAGE_SIZE);
                             curPage2+=FIVMR_PAGE_SIZE) {
                            fivmr_FreePage *fp=(fivmr_FreePage*)curPage2;
                            LOG(5,("Grabbing page %p",curPage2));
                            fivmr_PageTable_setAssert(
                                &gc->spaceData[FIVMR_GC_OBJ_SPACE].pt,
                                curPage2,
                                FIVMR_GCPS_FREE,
                                FIVMR_GCPS_ZERO);
                            if (fp->prev==NULL) {
                                gc->freePageHead=fp->next;
                            } else {
                                fp->prev->next=fp->next;
                            }
                            if (fp->next!=NULL) {
                                fp->next->prev=fp->prev;
                            }
                        }
                        return (fivmr_LargeObjectHeader*)curPage;
                    }
                }
            }
        }
        LOG(2,("Out of memory on large object request: pages used = %p, "
               "max pages = %p, pages requrested = %p",
               gc->numPagesUsed,gc->maxPagesUsed,numPages));
        return NULL;
    } else {
        if (effort==FIVMR_AE_CAN_FAIL) {
            return NULL;
        }
        
        result=(uintptr_t)fivmr_malloc(size);
        if (result==0) {
            return NULL;
        }
        fivmr_assert(fivmr_align(result,8)==result);
        return (fivmr_LargeObjectHeader*)result;
    }
}

static void zeroLargeObject(fivmr_LargeObjectHeader *hdr,
			    uintptr_t size) {
    /* FIXME we could be more careful here and omit zeroing sometimes ... but it
       doesn't seem to be worth the effort. */
    bzero(hdr,size);
}

static fivmr_Object allocLargeObject(fivmr_GC *gc,
                                     uintptr_t size,
				     uintptr_t alignStart,
				     uintptr_t align,
                                     fivmr_AllocEffort effort,
                                     const char *description) {
    fivmr_ThreadState *ts;
    uintptr_t offset;
    uintptr_t fullSize;
    fivmr_LargeObjectHeader *hdr;
    uintptr_t result;
    uintptr_t numPages;
    int tries;
    fivmr_Settings *settings;
    fivmr_Nanos before=fivmr_debugTime();
    
    settings=&fivmr_VMfromGC(gc)->settings;
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    fivmr_ThreadState_goToNative(ts);

    offset=
        sizeof(fivmr_LargeObjectHeader)+
        FIVMR_ALLOC_OFFSET(settings);
    offset=fivmr_align(offset+alignStart,align)-alignStart;
    offset=fivmr_align(offset-FIVMR_ALLOC_OFFSET(settings),
                       FIVMR_MIN_OBJ_ALIGN(settings)) + FIVMR_ALLOC_OFFSET(settings);
    fullSize=offset+size-FIVMR_ALLOC_OFFSET(settings);
    numPages=fivmr_pages(fullSize);
    
    if (FIVMR_ASSERTS_ON && offset-FIVMR_ALLOC_OFFSET(settings)+size>fullSize) {
        LOG(0,("offset = %p, size = %p, fullsize = %p",
               offset,size,fullSize));
        fivmr_assert(offset-FIVMR_ALLOC_OFFSET(settings)+size<=fullSize);
    }

    if (FIVMR_PREDICTABLE_OOME(&fivmr_VMfromGC(gc)->settings) &&
        !fivmr_Thread_isCritical()) {
        fivmr_Lock_lock(&gc->requestLock);
    }
    fivmr_Lock_lock(&gc->gcLock);
    
    for (tries=0;;++tries) {
        hdr=attemptAllocLargeObject(gc,fullSize,effort);
        LOG(3,("Allocated large object at %p with size=%p, alignStart=%p, align=%p, "
               "offset=%p, fullSize=%p, ending at %p",
               hdr,size,alignStart,align,offset,fullSize,(uintptr_t)hdr+fullSize));
        if (hdr!=NULL || effort==FIVMR_AE_CAN_FAIL || tries==3) {
            break;
        }
        fivmr_Lock_unlock(&gc->gcLock);
        collectInvoluntarilyFromNative(gc,"LOS",description);
        fivmr_Lock_lock(&gc->gcLock);
    }
    if (hdr!=NULL) {
	if (gc->numPagesUsed <= (intptr_t)gc->gcTriggerPages &&
	    gc->numPagesUsed+(intptr_t)numPages > (intptr_t)gc->gcTriggerPages) {
	    fivmr_GC_asyncCollect(gc);
	}
	gc->numPagesUsed+=numPages;
        fivmr_assert(gc->numPagesUsed <= (intptr_t)gc->maxPagesUsed);
        if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            gc->numFreePages-=numPages;
        }
        fivmr_assert(gc->numPagesUsed>=0);
        fivmr_assert(gc->numFreePages>=0);
    }
    
    fivmr_Lock_unlock(&gc->gcLock);
    if (FIVMR_PREDICTABLE_OOME(&fivmr_VMfromGC(gc)->settings) &&
        !fivmr_Thread_isCritical()) {
	fivmr_Lock_unlock(&gc->requestLock);
    }
    
    if (hdr==NULL) {
	result=0;
	fivmr_ThreadState_goToJava(ts);
    } else {
	zeroLargeObject(hdr,fullSize);
	
	hdr->fullSize=fullSize;
	result=((uintptr_t)hdr)+offset;

        hdr->object=fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                              result);
	
	LOG(4,("Allocated large object with loh = %p, at %p with header at %p",
	       hdr,result,
               fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                         result)));
	
	fivmr_ThreadState_goToJava(ts);

	fivmr_stampGCBits(gc,FIVMR_GC_OBJ_SPACE,NULL,result);
	
	fivmr_Lock_lock(&gc->gcLock);
	hdr->next = gc->largeObjectHead;
	gc->largeObjectHead = hdr;
	fivmr_Lock_unlock(&gc->gcLock);
    }
    
    gc->largeAllocTime+=fivmr_debugTime()-before;
    
    return result;
}

static void freeLargeObject(fivmr_GC *gc,
                            fivmr_LargeObjectHeader *hdr) {
    uintptr_t numPages;
    uintptr_t curPage;

    numPages=fivmr_pages(hdr->fullSize);
    LOG(3,("Freeing %" PRIuPTR " pages of large object %p",
	   numPages,hdr));
    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        /* FIXME: this could be improved - say by releasing the pages in smaller chunks */
        fivmr_Lock_lock(&gc->gcLock);
        for (curPage=(uintptr_t)hdr;
             curPage<((uintptr_t)hdr)+(numPages<<FIVMSYS_LOG_PAGE_SIZE);
             curPage+=FIVMR_PAGE_SIZE) {
            LOG(5,("Returning page %p",curPage));
            ((fivmr_FreePage*)curPage)->dirty=true;
            addPageToFreelist(gc,curPage);
        }
        gc->numPagesUsed -= numPages;
        gc->numFreePages += numPages;
        fivmr_assert(gc->numPagesUsed >= 0);
        fivmr_assert(gc->numFreePages >= 0);
        fivmr_Lock_unlock(&gc->gcLock);
    } else {
        free(hdr);
        fivmr_Lock_lock(&gc->gcLock);
        gc->numPagesUsed-=numPages;
        fivmr_assert(gc->numPagesUsed>=0);
        fivmr_assert(gc->numFreePages>=0);
        fivmr_Lock_unlock(&gc->gcLock);
    }
}

static void relinquishSpacesContext(fivmr_ThreadState *ts) {
    fivmr_GCSpaceAlloc *alloc=ts->gc.alloc+FIVMR_GC_OBJ_SPACE;

    /* FIXME: we can make this better by only relinquishing if we know
       that this mutator is still using the old space. */
    
    alloc->ssBump=0;
    alloc->ssEnd=0;
    alloc->ssSize=0;
}

static void relinquishAllocationContextForPage(fivmr_ThreadState *ts,
                                               fivmr_GCSpace space,
                                               uintptr_t ptr) {
    uintptr_t page;
    fivmr_UsedPage *up;
    fivmr_GC *gc;
    fivmr_GCSpaceAlloc *alloc;
    
    gc=&ts->vm->gc;
    alloc=ts->gc.alloc+space;

    LOG(4,("Relinquishing allocation context with bump = %p",
           alloc->bump));
    
    page=ptr&~(FIVMR_PAGE_SIZE-1);
    up=(fivmr_UsedPage*)page;
    *fivmr_UsedPage_status(up,&fivmr_VMfromGC(gc)->settings)=FIVMR_GCUPS_ZERO;
    zeroFreeLines(alloc->freeHead,gc);

    /* mark the old page as populated or shaded depending on GC
       phase. */
    if (!fivmr_GC_isCollecting(gc->phase)) {
        LOG(3,("Thread %u (from %p) relinquishing page %p as populated, %s",
               ts->id,fivmr_ThreadHandle_current(),page,
               alloc->usedPage?"when line allocating":"when bump allocating"));
        fivmr_PageTable_setAssert(&gc->spaceData[space].pt,
                                  page,
                                  FIVMR_GCPS_ZERO,
                                  FIVMR_GCPS_POPULATED);
    } else {
        /* GC is not yet done - mark page as relinquished to prevent it from
           getting into the sweep. */
        LOG(3,("Thread %u (from %p) relinquishing page %p as relinquished, %s",
               ts->id,fivmr_ThreadHandle_current(),page,
               alloc->usedPage?"when line allocating":"when bump allocating"));
        fivmr_PageTable_setAssert(&gc->spaceData[space].pt,
                                  page,
                                  FIVMR_GCPS_ZERO,
                                  FIVMR_GCPS_RELINQUISHED);
    }
    
    alloc->bump=0;
    alloc->start=0;
    alloc->size=0;
    alloc->freeHead=NULL;
    alloc->freeTail=NULL;
    alloc->usedPage=NULL;
}

static void relinquishAllocationContext(fivmr_ThreadState *ts,
					fivmr_GCSpace space) {
    fivmr_GC *gc;
    fivmr_GCSpaceAlloc *alloc;
    fivmr_MemoryArea *curarea;
    
    gc=&ts->vm->gc;
    alloc=ts->gc.alloc+space;
    curarea=NULL;
    
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&space==0&&
        ts->gc.currentArea!=ts->gc.baseStackEntry.area) {
        curarea=ts->gc.currentArea;
        fivmr_MemoryArea_setCurrentArea(ts, ts->gc.baseStackEntry.area);
    }
    if (alloc->bump) {
        relinquishAllocationContextForPage(ts,space,alloc->start);
    } else {
	fivmr_assert(alloc->freeHead==NULL);
	fivmr_assert(alloc->freeTail==NULL);
	fivmr_assert(alloc->usedPage==NULL);
    }
    if (curarea) {
        fivmr_MemoryArea_setCurrentArea(ts, curarea);
    }
}

static void relinquishAllocationContexts(fivmr_ThreadState *ts) {
    fivmr_GCSpace i;
    fivmr_MemoryArea *curarea=NULL;
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&
        ts->gc.currentArea!=ts->gc.baseStackEntry.area) {
        curarea=ts->gc.currentArea;
        fivmr_MemoryArea_setCurrentArea(ts, ts->gc.baseStackEntry.area);
    }
    if (FIVMR_HFGC(&ts->vm->settings)) {
        relinquishSpacesContext(ts);
    }
    for (i=0;i<FIVMR_GC_NUM_GC_SPACES;++i) {
	relinquishAllocationContext(ts,i);
    }
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)&&curarea) {
        fivmr_MemoryArea_setCurrentArea(ts, curarea);
    }
}

static void initSpaces(fivmr_GC *gc) {
    uintptr_t semispaceSize;
    
    bzero(&gc->ss,sizeof(gc->ss));
    
    /* need to compute the max spine space usage for the given
       heap size.  if you change any of the memory model constants,
       this computation must change as well. */
    
    semispaceSize=(3*gc->maxPagesUsed+9)/10;
    
    LOG(1,("Need %" PRIuPTR " pages for both semi-spaces.",
           semispaceSize));
    
    /* FIXME: we may want to change the zeroing policy for the semispace... */
    gc->ss.start=(uintptr_t)fivmr_allocPages(semispaceSize,NULL);
    gc->ss.size=semispaceSize<<FIVMSYS_LOG_PAGE_SIZE;
    
    gc->ss.allocStart = gc->ss.start+FIVMR_FRAG_SP_FR_OFFSET;
    gc->ss.allocEnd = gc->ss.allocStart+gc->ss.size/2;
    
    gc->ss.muAlloc = gc->ss.allocEnd;
    gc->ss.muLimit = gc->ss.allocStart;
    
    gc->ss.gcAlloc = gc->ss.muLimit;
    
    LOG(1,("Semi-space policy initialized over memory range %" PRIuPTR
           " to %" PRIuPTR ".",
           gc->ss.start,gc->ss.start+gc->ss.size-1));
    
    LOG(2,("Allocator will allocate between %" PRIuPTR " and %" PRIuPTR ".",
           gc->ss.allocStart,gc->ss.allocEnd));
}

static void freeSpaces(fivmr_GC *gc) {
    fivmr_freePages((void*)gc->ss.start,gc->ss.size>>FIVMSYS_LOG_PAGE_SIZE);
}

/* NOTE: we should flip spaces before the mutator starts allocating black...
   otherwise we'll have black sentinels in the old space that point to
   spines in the old space; these spines will then never get copied. */
static void flipSpaces(fivmr_GC *gc) {
    uintptr_t mult;
    uintptr_t spaceLeft;
    
    LOG(2,("Performing semi-space flip."));
    
    fivmr_Lock_lock(&gc->gcLock);

    /* let the sweeper know what regions to deal with. */
    
    fivmr_assert(gc->ss.muLimit == gc->ss.gcAlloc);
    
    gc->ss.muRegionStart = gc->ss.muAlloc-FIVMR_FRAG_SP_FR_OFFSET;
    gc->ss.muRegionSize = gc->ss.allocEnd-gc->ss.muAlloc;
    
    gc->ss.gcRegionStart = gc->ss.allocStart-FIVMR_FRAG_SP_FR_OFFSET;
    gc->ss.gcRegionSize = gc->ss.gcAlloc-gc->ss.allocStart;
    
    /* compute how much space is left */
    
    spaceLeft = gc->ss.size/2 - gc->ss.muRegionSize - gc->ss.gcRegionSize;
    
    LOG(2,("Mutator will have %" PRIuPTR " bytes to semi-space allocate until "
           "the GC is done allocating.",spaceLeft));
    
    /* perform the flip. */

    fivmr_assert(((gc->ss.size/2)&(sizeof(uintptr_t)-1)) == 0);
    fivmr_assert(gc->ss.allocStart == gc->ss.start+FIVMR_FRAG_SP_FR_OFFSET ||
                 gc->ss.allocStart == gc->ss.start+gc->ss.size/2+FIVMR_FRAG_SP_FR_OFFSET);
    
    if (gc->ss.allocStart == gc->ss.start+FIVMR_FRAG_SP_FR_OFFSET) {
        mult=1;
    } else {
        mult=0;
    }
    
    LOG(2,("Flipping to space %d.",
           (int)(mult+1)));
    
    gc->ss.allocStart = gc->ss.start+mult*gc->ss.size/2+FIVMR_FRAG_SP_FR_OFFSET;
    gc->ss.allocEnd = gc->ss.allocStart+gc->ss.size/2;
    
    gc->ss.muAlloc = gc->ss.allocEnd;
    gc->ss.gcAlloc = gc->ss.allocStart;
    
    gc->ss.muLimit = gc->ss.muAlloc-spaceLeft;
    
    LOG(2,("Allocator will allocate between %p and %p"
           ", mutator will allocate from %p"
           ", collector will allocate from %p"
           ", with the mutator limit at %p, and the total "
           "semispace area is at %p with a size of %" PRIuPTR ".",
           gc->ss.allocStart,gc->ss.allocEnd,
           gc->ss.muAlloc, gc->ss.gcAlloc, gc->ss.muLimit, gc->ss.start,
           gc->ss.size));
    
    /* finally, ensure that we don't have any objects queued for spine
       copying... */
    fivmr_assert(gc->ss.head==NULL);

    fivmr_Lock_unlock(&gc->gcLock);
}

static void doneGCSpaceAlloc(fivmr_GC *gc) {
    LOG(2,("Signaling that the GC is done semi-space allocating."));

    fivmr_Lock_lock(&gc->gcLock);
    gc->ss.muLimit = gc->ss.gcAlloc;
    gc->ss.first = true;
    fivmr_Lock_unlock(&gc->gcLock);
}

static void sweepOldSpace(fivmr_GC *gc) {
    LOG(2,("Sweeping old semi-space."));

    if (shouldQuit(gc)) return;
    bzero((void*)gc->ss.muRegionStart,gc->ss.muRegionSize);
    if (shouldQuit(gc)) return;
    bzero((void*)gc->ss.gcRegionStart,gc->ss.gcRegionSize);
}

static uintptr_t attemptAllocSSExtent(fivmr_ThreadState *ts,
                                      uintptr_t size) {
    uintptr_t result;
    fivmr_GC *gc;
    
    gc=&ts->vm->gc;
    
    fivmr_Lock_lock(&gc->gcLock);

    result = gc->ss.muAlloc-size;
    if (gc->ss.allocEnd - result > gc->ss.allocEnd - gc->ss.muLimit) {
        result = 0; /* failure */
    } else {
        gc->ss.muAlloc = result;
    }

    fivmr_Lock_unlock(&gc->gcLock);
    
    return result;
}

static uintptr_t allocSSExtent(fivmr_ThreadState *ts,
                               uintptr_t size,
                               const char *description) {
    fivmr_GC *gc;
    uintptr_t result;
    int iterationCountdown;
    
    gc=&ts->vm->gc;
    
    result=0; /* make GCC happy */
    iterationCountdown=3;
    
    for (;;) {
        bool firstToAllocate;
        
        fivmr_Lock_lock(&gc->gcLock);
        if (iterationCountdown) {
            firstToAllocate=false;
            iterationCountdown--;
        } else {
            firstToAllocate=gc->ss.first;
            gc->ss.first=false;
        }
        result=attemptAllocSSExtent(ts,size);
        fivmr_Lock_unlock(&gc->gcLock);
        
        if (firstToAllocate && !result) {
            break;
        }
        
        if (result) {
            break;
        }
        
        fivmr_ThreadState_goToNative(ts);
        collectInvoluntarilyFromNative(gc,"SS",description);
        fivmr_ThreadState_goToJava(ts);
    }
    
    return result;
}

fivmr_Spine FIVMR_CONCAT(FIVMBUILD_GC_NAME,_allocSSSlow)(fivmr_ThreadState *ts,
                                                         uintptr_t spineLength,
                                                         int32_t numEle,
                                                         const char *description) {
    uintptr_t result;
    uintptr_t size;
    fivmr_GCSpaceAlloc *alloc;
    fivmr_GC *gc;

    gc=&ts->vm->gc;
    alloc=ts->gc.alloc+FIVMR_GC_OBJ_SPACE;
    size=fivmr_Spine_calcSize(spineLength);
    
    if (size>FIVMR_LARGE_ARRAY_THRESHOLD) {
        result=allocSSExtent(ts,size,description);
    } else {
        uintptr_t bottom=
            attemptAllocSSExtent(ts,FIVMR_ARRAY_THREAD_CACHE_SIZE);
        if (bottom) {
            alloc->ssBump=alloc->ssEnd=
                bottom+FIVMR_ARRAY_THREAD_CACHE_SIZE;
            alloc->ssSize=FIVMR_ARRAY_THREAD_CACHE_SIZE;

            result=alloc->ssBump-size;
            fivmr_assert(alloc->ssEnd - result <= alloc->ssSize);
            alloc->ssBump=result;
        } else {
            result=allocSSExtent(ts,size,description);
        }
    }
    
    if (!result) {
        logOOME(gc,"SS",description,size,sizeof(uintptr_t));
        fivmr_throwOutOfMemoryError_inJava(ts);
    }
    
    return result;
}

static inline bool isInToSpace(fivmr_GC *gc,
                               fivmr_Spine spine) {
    return spine>=gc->ss.allocStart && spine<gc->ss.allocEnd;
}

static inline void copySpine(fivmr_GC *gc,
                             fivmr_Object obj) {
    fivmr_Spine old;
    fivmr_Spine new;
    uintptr_t spineLength;
    uintptr_t spineSize;
    uintptr_t i;
    
    spineLength=fivmr_Object_getSpineLength(&fivmr_VMfromGC(gc)->settings,obj);
    spineSize=fivmr_Spine_calcSize(spineLength);
    
    old=fivmr_Spine_forObject(obj);
    new=fivmr_Spine_getForward(old);
    
    /* use careful pointer-loop copy to ensure happiness. */
    for (i=0;i<spineLength;++i) {
        uintptr_t ptr=((uintptr_t*)old)[i];
        if (ptr!=0) {
            ((uintptr_t*)new)[i]=ptr;
        }
        if (shouldQuit(gc)) return;
    }
    
    fivmr_Object_setSpine(obj,new);
}

uintptr_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_allocRawSlow)(fivmr_ThreadState *ts,
                                                        fivmr_GCSpace space,
                                                        uintptr_t size,
                                                        uintptr_t alignStart,
                                                        uintptr_t align,
                                                        fivmr_AllocEffort effort,
                                                        const char *description) {
    uintptr_t result=0;
    fivmr_GCSpaceAlloc *alloc;
    uintptr_t alignedSize;
    fivmr_GC *gc;
    fivmr_Settings *settings;
    fivmr_Nanos before;
    
    fivmr_assert(fivmr_ThreadPriority_leRT(fivmr_Thread_getPriority(fivmr_ThreadHandle_current()),
                                           ts->vm->maxPriority));
    
    before=fivmr_debugTime();
    
    gc=&ts->vm->gc;
    settings=&fivmr_VMfromGC(gc)->settings;
    
    fivmr_assert(ts->execStatus!=FIVMR_TSES_CLEAR);
    fivmr_assert(ts->execStatus!=FIVMR_TSES_NEW);
    fivmr_assert(ts->execStatus!=FIVMR_TSES_STARTING);
    fivmr_assert(ts->execStatus!=FIVMR_TSES_TERMINATING);
    
    LOG(11,("fivmr_allocRaw_slow(%p (%u), %p, %p, %s) called",
            ts,ts->id,size,align,description));
    
    if (FIVMR_SCOPED_MEMORY(settings)&&space==FIVMR_GC_OBJ_SPACE
        &&fivmr_MemoryArea_inScope(ts)) {
        fivmr_throwOutOfMemoryError_inJava(ts);
    }

    alloc=ts->gc.alloc+space;

    size=fivmr_alignRaw(size,FIVMR_OBJ_SIZE_ALIGN(settings));
    
    if (FIVMR_HFGC_FAIL_FAST_PATHS(settings) && effort==FIVMR_AE_CAN_FAIL) {
        goto done;
    }
    
    /* figure out if the object will fit on a page */
    alignedSize=size+fivmr_align(alignStart,align)-alignStart;
    if (alignedSize>FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings)) {
        /* nope, not on a page ... allocate large object */
        result=allocLargeObject(gc,size,alignStart,align,effort,description);
        if (result==0 && effort==FIVMR_AE_MUST_SUCCEED) {
            logOOME(gc,"LOS",description,size,align);
            fivmr_throwOutOfMemoryError_inJava(ts);
        }
        goto done;
    } else {
        while ((fivmr_align(alloc->bump+alignStart,align)
                -alignStart+size)-alloc->start > alloc->size) {
            if (alloc->freeHead) {
                /* NOTE: we don't have to relinquish the page that we were bumping
                   on, because if ts->gc.freeHead is set then we weren't page
                   allocating. */
                
                fivmr_FreeLine *line=NULL;
                fivmr_Nanos before2=fivmr_debugTime();

                if (size==FIVMR_MIN_ALLOC_SIZE(settings) && FIVMR_HFGC(settings)) {
                    line=alloc->freeHead;
                    fivmr_assert(canSatisfy(settings,line,size,alignStart,align));
                } else {
                    fivmr_FreeLine *cur;
                    for (cur=alloc->freeHead;;cur=cur->next) {
                        if (false) { /* use first-fit */
                            if (canSatisfy(settings,
                                           cur,size,alignStart,align)) {
                                line=cur;
                                break;
                            }
                        } else { /* use best-fit */
                            if ((line==NULL || cur->size<line->size) &&
                                canSatisfy(settings,cur,
                                           size,alignStart,align)) {
                                line=cur;
                            }
                        }
                        if (cur==cur->lastOnPage) {
                            fivmr_assert(cur->next==NULL);
                            break;
                        }
                    }
                }
                
                gc->freeLineSearchTime+=fivmr_debugTime()-before2;
                
                /* printf("picked line %p in page %p\n",line,alloc->usedPage); */
                
                if (line==NULL) {
                    /* none of the lines on this page can be used for the current
                       allocation request - so drop the page and retry */
                    /* FIXME: we should really be putting this page back onto the
                       freeline list - preferably at the *end* of the list. */
                    relinquishAllocationContextForPage(ts,space,(uintptr_t)alloc->freeHead);
                } else {
                    if (line->prev!=NULL) {
                        line->prev->next=line->next;
                    } else {
                        alloc->freeHead=line->next;
                    }
                    if (line->next!=NULL) {
                        line->next->prev=line->prev;
                    } else {
                        alloc->freeTail=line->prev;
                    }
                    if (line==line->lastOnPage) {
                        fivmr_assert(line->next==NULL);
                        if (line->prev!=NULL) {
                            line->prev->lastOnPage=line->prev;
                        } else {
                            fivmr_assert(alloc->freeHead==NULL);
                            fivmr_assert(alloc->freeTail==NULL);
                        }
                    } else {
                        fivmr_assert(line->next!=NULL);
                    }
                
                    fivmr_assert((alloc->freeHead==NULL)==(alloc->freeTail==NULL));
                
                    fivmr_assert(((uintptr_t)line)+line->size <=
                                 (((uintptr_t)line)&~(FIVMR_PAGE_SIZE-1)) + FIVMR_PAGE_SIZE);
			 
                    alloc->bump=alloc->start=
                        ((uintptr_t)line)+FIVMR_ALLOC_OFFSET(settings);
                    alloc->size=line->size;

                    LOG(3,("Allocating in line %p (size %p, ends at %p)",
                           line,line->size,((uintptr_t)line)+line->size-1));
                    bzero(line,line->size);
                    fivmr_fence();
                    /* reloop to test if our allocation request fits */
                }
            } else {
                fivmr_UsedPage *up=NULL;
                fivmr_FreeLine *flHead=NULL;
                fivmr_FreeLine *flTail=NULL;
                uintptr_t page=0;
                int cnt=0;
                
                /* attempt to get a new allocation context */
                fivmr_ThreadState_goToNative(ts);
                
                if (FIVMR_PREDICTABLE_OOME(settings) &&
                    !fivmr_Thread_isCritical()) {
                    fivmr_Lock_lock(&gc->requestLock);
                }
                fivmr_Lock_lock(&gc->gcLock);
                
                switch (effort) {
                case FIVMR_AE_CAN_FAIL:
                    page=(uintptr_t)getPage(gc);
                    break;
                case FIVMR_AE_MUST_SUCCEED:
                    for (cnt=0;;++cnt) {
                        if (getPageOfFreeLines(gc,
                                               space,
                                               &up,&flHead,&flTail,
                                               size,alignStart,align) ||
                            (page=(uintptr_t)getPage(gc))) {
                            break;
                        }
                        if (cnt==3) {
                            break;
                        }
                        fivmr_Lock_unlock(&gc->gcLock);
                        collectInvoluntarilyFromNative(
                            gc,
                            fivmr_GCSpace_name(space),
                            description);
                        fivmr_Lock_lock(&gc->gcLock);
                    }
                    break;
                default: fivmr_abort("wrong value of effort");
                }
                
                fivmr_Lock_unlock(&gc->gcLock);
                if (FIVMR_PREDICTABLE_OOME(settings) &&
                    !fivmr_Thread_isCritical()) {
                    fivmr_Lock_unlock(&gc->requestLock);
                }

                fivmr_ThreadState_goToJava(ts);
                
                if (page!=0 || up!=NULL) {
                    relinquishAllocationContext(ts,space);
                }
                
                if (page!=0) {
                    alloc->bump=alloc->start=
                        page+FIVMR_PAGE_HEADER(settings)+FIVMR_ALLOC_OFFSET(settings);
                    alloc->size=FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings);
                } else if (up!=NULL) {
                    /* printf("allocating in freelines in %p: %p (%p), %p (%p); in request for %p\n",
                       up,flHead,(void*)flHead->size,flTail,(void*)flTail->size,(void*)size); */
                    alloc->usedPage=up;
                    alloc->freeHead=flHead;
                    alloc->freeTail=flTail;
                } else if (effort==FIVMR_AE_CAN_FAIL) {
                    result=0;
                    goto done;
                } else {
                    logOOME(gc,fivmr_GCSpace_name(space),description,size,align);
                    fivmr_throwOutOfMemoryError_inJava(ts);
                    result=0;
                    goto done;
                }
            }
        }
	
        LOG(10,("for space %d: bump = %p, start = %p, size = %p, align = %p",
                space,alloc->bump,alloc->start,alloc->size,align));
    
        result=
            fivmr_align(alloc->bump+alignStart,align)
            -alignStart;
        alloc->bump=result+size;
    
        LOG(9,("allocRaw_slow returning %p",result));
    
        goto done;
    }

done:
    
    gc->slowPathTime+=fivmr_debugTime()-before;
    
    return result;
}

static inline uint8_t shadePageAsNecessary(fivmr_GC *gc,
                                           fivmr_GCSpace space,
					   uintptr_t page) {
    for (;;) {
	uint8_t ptState=fivmr_PageTable_get(&gc->spaceData[space].pt,(uintptr_t)page);
	if (ptState==FIVMR_GCPS_POPULATED) {
	    if (fivmr_PageTable_cas(&gc->spaceData[space].pt,
				    (uintptr_t)page,
				    FIVMR_GCPS_POPULATED,
				    FIVMR_GCPS_SHADED)) {
		LOG(3,("Shaded page %p in space %d",page,space));
		return FIVMR_GCPS_POPULATED;
	    }
	} else {
            if (FIVMR_ASSERTS_ON &&
                FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
                fivmr_assert(ptState!=FIVMR_GCPS_FREE);
            }
	    if (ptState!=FIVMR_GCPS_SHADED) {
		LOG(5,("Page %p is in state %u",page,ptState));
	    }
	    return ptState;
	}
    }
}

static inline void shadeChunkAsNecessary(fivmr_GC *gc,
                                         fivmr_GCSpace space,
                                         uintptr_t base,
                                         uintptr_t size) {
    fivmr_UsedPage *up;
    uint8_t pageState;
    fivmr_Settings *settings;
    
    settings=&fivmr_VMfromGC(gc)->settings;
    
    fivmr_assert(fivmr_alignRaw(size,
                                FIVMR_OBJ_SIZE_ALIGN(settings))
                 == size);
    
    up=(fivmr_UsedPage*)(((uintptr_t)base)&~(FIVMR_PAGE_SIZE-1));
    pageState=shadePageAsNecessary(gc,space,(uintptr_t)up);
    switch (pageState) {
    case FIVMR_GCPS_POPULATED: {
        *fivmr_UsedPage_reserved(up,settings)=1;
        bzero(fivmr_UsedPage_bits(up,settings),
              FIVMR_UP_BITS_LENGTH(settings)*4);
        /* drop down to GCPS_SHADED... */
    }
    case FIVMR_GCPS_SHADED: {
        uintptr_t start;
        uintptr_t end;
        uintptr_t index;
                
        fivmr_assert(fivmr_alignRaw(base,FIVMR_MIN_OBJ_ALIGN(settings))==base);
        start=(base-(uintptr_t)up-FIVMR_PAGE_HEADER(settings))/FIVMR_MIN_CHUNK_ALIGN(settings);
        end=start+size/FIVMR_MIN_CHUNK_ALIGN(settings);
                
        for (index=start;index<end;++index) {
            fivmr_BitVec_set(fivmr_UsedPage_bits(up,settings),
                             index,true);
        }
        break;
    }
    default:
        break;
    }
}

static inline void shadeRawTypeAsNecessary(fivmr_GC *gc,
                                           uintptr_t rawType,
                                           uintptr_t size) {
    shadeChunkAsNecessary(
        gc,
        FIVMR_GC_OBJ_SPACE,
        rawType,
        fivmr_alignRaw(size,FIVMR_OBJ_SIZE_ALIGN(&fivmr_VMfromGC(gc)->settings)));
}

static void assertPerThreadQueuesSound(fivmr_GC *gc) {
    if (FIVMR_ASSERTS_ON) {
	LOG(2,("Requesting handshake to assert soundness of per-thread queues."));
	fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                        FIVMR_TSEF_JAVA_HANDSHAKEABLE,
					FIVMR_TSEF_GC_MISC);
	fivmr_assert(gc->globalQueue==NULL);
    }
}

static inline fivmr_TypeData *handleObjectHeader(fivmr_GC *gc,
                                                 fivmr_Object obj) {
    fivmr_TypeData *td;
    fivmr_Monitor *monitor;
    fivmr_Settings *settings;
    
    fivmr_assert(obj!=0);
    
    settings=&fivmr_VMfromGC(gc)->settings;
    
    monitor=fivmr_ObjHeader_getMonitor(settings,
                                       fivmr_ObjHeader_forObject(settings,
                                                                 obj));
    if (monitor->state==FIVMR_MS_INVALID) {
	td=(fivmr_TypeData*)monitor;
    } else {
	td=monitor->forward;

        /* Scoped object monitors et al. are not subject to collection */
        if (FIVMR_SCOPED_MEMORY(settings)) {
            fivmr_GCHeader *hdr=fivmr_GCHeader_fromObject(settings,obj);
            if (fivmr_GCHeader_isImmortal(hdr)||
                fivmr_GCHeader_isScopeID(hdr)) {
                return td;
            }
        }
	shadeRawTypeAsNecessary(gc,(uintptr_t)monitor,sizeof(fivmr_Monitor));
	if (monitor->queues!=NULL) {
	    /* FIXME: free up the queues if they're not in use. */
	    shadeRawTypeAsNecessary(gc,
                                    (uintptr_t)(monitor->queues),
                                    sizeof(fivmr_MonQueues));
	}
    }
    
    return td;
}

static void collectorWeakRefHandler(fivmr_VM *vm,
                                    fivmr_Object weakRef,
                                    void *arg) {
    /* FIXME do something */
}

static fivmr_ScanSpecialHandlers collectorSpecials={
    collectorWeakRefHandler
};

static void mutatorScanObject(fivmr_ThreadState *ts,
                              fivmr_Object obj) {
    
    fivmr_TypeData *td;
    fivmr_Settings *settings;
    
    td=handleObjectHeader(&ts->vm->gc,obj);
    
    fivmr_Object_scan(ts->vm,obj,td,mutatorMarkObjStackNoisyForScan,NULL,ts);
}

static inline void collectorScanObject(fivmr_GC *gc,
                                       fivmr_GCHeader **queue,
                                       fivmr_Object obj) {
    fivmr_TypeData *td;
    fivmr_Settings *settings;
    
    gc->anthracite=obj;
    
    td=handleObjectHeader(gc,obj);
    
    fivmr_Object_scan(fivmr_VMfromGC(gc),obj,td,collectorMarkObjForScan,&collectorSpecials,queue);
}

static void scanTypeAux(fivmr_GC *gc,
                        fivmr_GCHeader **queue,
                        fivmr_TypeAux *cur) {
    for (cur=fivmr_TypeAux_first(cur,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED);
         cur!=NULL;
         cur=fivmr_TypeAux_next(cur,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED)) {
        uintptr_t *ptr;
        uintptr_t i;
        
        if (shouldQuit(gc)) return;
        
        ptr=(uintptr_t*)fivmr_TypeAux_data(cur);
        
        for (i=0;i<cur->occupied;i+=sizeof(uintptr_t)) {
            collectorMarkObj(gc,queue,*ptr++);
        }
    }
}

typedef struct {
    fivmr_GC *gc;
    fivmr_GCHeader **queue;
} ScanTypeData;

static uintptr_t scanType_cback(fivmr_TypeData *td,
                                uintptr_t arg) {
    ScanTypeData *std=(ScanTypeData*)(void*)arg;
    int32_t i;
    
    if (shouldQuit(std->gc)) {
        return 1;
    }
    
    if ((td->flags&FIVMR_TBF_AOT)) {
        fivmr_assert(td->bytecode!=0);
        collectorScanObject(std->gc,std->queue,td->bytecode);
        fivmr_assert(td->classObject!=0);
        collectorScanObject(std->gc,std->queue,td->classObject);
    } else {
        collectorMarkObj(std->gc,std->queue,td->bytecode);
        collectorMarkObj(std->gc,std->queue,td->classObject);
    }
    
    return 0;
}

static void scanContext(fivmr_GC *gc,fivmr_GCHeader **queue,fivmr_TypeContext *ctx) {
    fivmr_VM *vm=fivmr_VMfromGC(gc);
    
    collectorMarkObj(gc,queue,ctx->classLoader);
    
    if (FIVMR_CLASSLOADING(&vm->settings)) {
        fivmr_TypeAux *cur;
        ScanTypeData std;
        
        fivmr_Lock_lock(&ctx->treeLock);
        
        for (cur=fivmr_TypeAux_first(ctx->aux,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED);
             cur!=NULL;
             cur=fivmr_TypeAux_next(cur,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED)) {
            uintptr_t *ptr;
            uintptr_t i;
            
            if (shouldQuit(gc)) break;
            
            ptr=(uintptr_t*)fivmr_TypeAux_data(cur);
            
            for (i=0;i<cur->occupied;i+=sizeof(uintptr_t)) {
                collectorMarkObj(gc,queue,*ptr++);
            }
        }
        
        std.gc=gc;
        std.queue=queue;
        fivmr_TypeContext_forAllTypes(ctx,scanType_cback,(uintptr_t)&std);
        
        fivmr_Lock_unlock(&ctx->treeLock);
    }
}

static void shadeObjectAsNecessary(fivmr_GC *gc,
                                   fivmr_GCHeader *hdr) {
    fivmr_Object obj;
    fivmr_Settings *settings;
    
    settings=&fivmr_VMfromGC(gc)->settings;
    
    obj=fivmr_GCHeader_toObject(settings,hdr);
    
    if (FIVMR_ASSERTS_ON && !FIVMR_HFGC(settings)) {
        fivmr_assert(fivmr_Object_isContiguous(settings,obj));
    }
    
    if (fivmr_Object_isContiguous(settings,obj)) {
        uintptr_t size=fivmr_Object_size(settings,obj);
        
        fivmr_assert(fivmr_alignRaw(size,
                                    FIVMR_MIN_OBJ_ALIGN(settings))
                     == size);
        
        shadeChunkAsNecessary(
            gc,
            FIVMR_GC_OBJ_SPACE,
            fivmr_GCHeader_chunkStart(FIVMR_GC_OBJ_SPACE,hdr),
            size);
    } else {
        fivmr_TypeData *td;
        
        fivmr_assert(FIVMR_HFGC(settings));

        td=fivmr_TypeData_forObject(settings,obj);
        if (fivmr_TypeData_isArray(td)) {
            uintptr_t spineLength;
            uintptr_t spineSize;
            uintptr_t spine;
            uintptr_t i;
            uintptr_t eleSize;
            uintptr_t offsetFromStart;
            
            shadeChunkAsNecessary(gc,
                                  FIVMR_GC_OBJ_SPACE,
                                  obj,FIVMR_GC_BLOCK_SIZE);
            
            spine=fivmr_Spine_forObject(obj);
            if (spine!=0) {
                spineLength=fivmr_Object_getSpineLength(settings,obj);
                spineSize=fivmr_Spine_calcSize(spineLength);
            
                if (spineLength>FIVMR_FRAG_MAX_INLINE_SPINE &&
                    !isInToSpace(gc,spine)) {
                    /* the object is large enough to have gotten its own spine...
                       allocate a new spine and mark it for copying, but don't
                       copy it yet. */
                
                    fivmr_Spine newSpine;
                
                    newSpine=gc->ss.gcAlloc;
                
                    fivmr_assert(newSpine-gc->ss.allocStart
                                 <= gc->ss.muLimit-gc->ss.allocStart);
                
                    gc->ss.gcAlloc=newSpine+spineSize;

                    fivmr_assert(gc->ss.gcAlloc-gc->ss.allocStart
                                 <= gc->ss.muLimit-gc->ss.allocStart);
                    
                    if (false) printf("new space spine at %p, from %p\n",
                                      (void*)newSpine,(void*)spine);
                
                    ((int32_t*)newSpine)[-1]=fivmr_arrayLength(NULL,obj,0);
                    fivmr_Spine_setForward(newSpine,newSpine);
                
                    fivmr_Spine_setForward(spine,newSpine);
                
                    fivmr_GCHeader_setNext(hdr,gc->ss.head);
                    gc->ss.head=hdr;
                }
            
                eleSize=fivmr_TypeData_elementSize(td);
                offsetFromStart=FIVMR_GC_BLOCK_SIZE-eleSize;
                for (i=0;i<spineLength;++i) {
                    uintptr_t cur=((uintptr_t*)spine)[i];
                    if (cur!=0) {
                        shadeChunkAsNecessary(gc,
                                              FIVMR_GC_OBJ_SPACE,
                                              cur-offsetFromStart,
                                              FIVMR_GC_BLOCK_SIZE);
                    }
                }
            }
            
        } else {
            uintptr_t size=
                (fivmr_TypeData_size(td)+FIVMR_GC_BLOCK_SIZE-1)
                &~(FIVMR_GC_BLOCK_SIZE-1);
            uintptr_t cur=obj;
            while (size>0) {
                shadeChunkAsNecessary(gc,
                                      FIVMR_GC_OBJ_SPACE,
                                      cur,FIVMR_GC_BLOCK_SIZE);
                cur=(*(uintptr_t*)cur);
                fivmr_assert((cur&1)==0);
                if (cur==0) {
                    break;
                }
                size-=FIVMR_GC_BLOCK_SIZE;
            }
        }
    }
}

static void transitiveClosure(fivmr_GC *gc,
                              fivmr_GCHeader **queue,
			      uintptr_t *objectsTraced) {
    LOG(2,("Tracing..."));
    do {
        gc->traceIterationCount++;
        
	/* attempt to process our local queue first, then consult the global
	   one. */
	
        /* FIXME: have a page-local queue that we put objects onto if they're
           on the same page as the other object we're currently on.  or
           something. */
        
	while ((*queue)!=NULL) {
	    fivmr_GCHeader *cur;
	    fivmr_Object obj;
	    
            if (shouldQuit(gc)) return;

	    (*objectsTraced)++;
	    
	    cur=*queue;
	    obj=fivmr_GCHeader_toObject(&fivmr_VMfromGC(gc)->settings,
                                        cur);
	    *queue=fivmr_GCHeader_next(cur);

            shadeObjectAsNecessary(gc,cur);
	    collectorScanObject(gc,queue,obj);
	}
	
	/* suck on the global queue. */
	
	fivmr_ThreadState_softHandshake(
            fivmr_VMfromGC(gc),
	    FIVMR_TSEF_JAVA_HANDSHAKEABLE,
	    ((FIVMR_GC_BLACK_STACK(&fivmr_VMfromGC(gc)->settings)
              ?0:FIVMR_TSEF_SCAN_THREAD_ROOTS)|
	     FIVMR_TSEF_GC_MISC));
	
	fivmr_Lock_lock(&gc->gcLock);
	*queue=gc->globalQueue;
	gc->globalQueue=NULL;
	fivmr_Lock_unlock(&gc->gcLock);
    } while ((*queue)!=NULL);
}

static bool processDestructors(fivmr_GC *gc,
                               fivmr_GCHeader **queue) {
    fivmr_Destructor **curPtr;
    fivmr_Destructor *cur;
    
    if (!FIVMR_FINALIZATION_SUPPORTED(&fivmr_VMfromGC(gc)->settings)) {
        return false;
    }
    
    LOG(2,("Processing destructors..."));
    
    fivmr_Lock_lock(&gc->destructorLock);
    
    /* figure out which destructors need to be run */
    /* FIXME: this loop is boned.  I think. */
    for (curPtr=&gc->destructorHead;(*curPtr)!=NULL;) {
        fivmr_GCHeader *hdr;
        
        if (shouldQuit(gc)) goto quit;
        
	cur=*curPtr;
        
        if (FIVMR_ASSERTS_ON &&
            FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert((uintptr_t)cur>=gc->memStart);
            fivmr_assert((uintptr_t)cur<gc->memEnd);
        }
	
	fivmr_assert(cur->object!=0);
	
        hdr=fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                      cur->object);
        
        if (FIVMR_ASSERTS_ON &&
            FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert((uintptr_t)hdr>=gc->memStart);
            fivmr_assert((uintptr_t)hdr<gc->memEnd);
        }

	if ((fivmr_GCHeader_markBits(hdr) & gc->curShaded)==0) {
            LOG(2,("marking destructor %p for object %p for execution.",
                   cur,cur->object));
            
	    /* remove from this list */
	    *curPtr=cur->next;
	    
	    /* move to other list */
	    cur->next=gc->destructorsToRun;
	    gc->destructorsToRun=cur;
	} else {
	    /* mark destructor as live */
	    shadeRawTypeAsNecessary(gc,
                                    (uintptr_t)cur,
                                    sizeof(fivmr_Destructor));
            
            curPtr=&(*curPtr)->next;
	}
    }
    
    /* for all destructors that need to be run but have not yet been run,
       shade both the destructor and the object it refers to. */
    for (cur=gc->destructorsToRun;cur!=NULL;cur=cur->next) {
        if (shouldQuit(gc)) goto quit;

        if (FIVMR_ASSERTS_ON &&
            FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert((uintptr_t)cur>=gc->memStart);
            fivmr_assert((uintptr_t)cur<gc->memEnd);
        }
        
	fivmr_assert(cur->object!=0);
	
        LOG(2,("marking finalizable %p",cur->object));
	mark(gc,queue,
             fivmr_GCHeader_fromObject(&fivmr_VMfromGC(gc)->settings,
                                       cur->object));
	shadeRawTypeAsNecessary(gc,
                                (uintptr_t)cur,
                                sizeof(fivmr_Destructor));
    }

    if (gc->destructorsToRun!=NULL) {
	fivmr_Lock_broadcast(&gc->destructorLock);
    }
    fivmr_Lock_unlock(&gc->destructorLock);
    
    /* this is sneaky.  this must return true if after this point, another transitive
       closure should be performed because some objects got revived.  naively, this means
       that we should only return true if we detect that there are live destructors.
       but the reality is more complicated.  there may be an interleaving where some
       finalizer thread dequeues a destructor after the previous transitive closure but
       before this function grabbed the destructor lock.  in that case, that finalizer
       thread would have revived the destructor (by placing it in its root set), but
       we would not have seen it.  so, we must return true whenever we think that any
       destructor processing may be taking place.  in practice that means returning true
       unconditionally. */
    return true;

quit:
    fivmr_Lock_unlock(&gc->destructorLock);
    
    return false;
}

static void performCopying(fivmr_GC *gc) {
    uintptr_t count=0;
    LOG(2,("Performing copying."));
    while (gc->ss.head!=NULL) {
        fivmr_GCHeader *cur=gc->ss.head;
        
        copySpine(gc,
                  fivmr_GCHeader_toObject(&fivmr_VMfromGC(gc)->settings,
                                          cur));
        if (shouldQuit(gc)) return;
        
        gc->ss.head=fivmr_GCHeader_next(cur);
        count++;
    }
    LOG(2,("Copied %" PRIuPTR " objects.",count));
}

static inline void addLineInSweep(fivmr_Settings *settings,
                                  fivmr_FreeLine *head,
				  fivmr_FreeLine *tail,
				  uintptr_t start,
				  uintptr_t end,
				  uintptr_t *lineBytesFreedPtr,
				  bool zero) {
    if (end-start>=FIVMR_MIN_FREE_LINE_SIZE(settings)) {
	fivmr_FreeLine *fl;
	LOG(4,("Creating free line from %p to %p",
	       start,end));
	fl=(fivmr_FreeLine*)start;
	fl->size=end-start;
	fl->next=head->next;
	fl->prev=head;
	head->next->prev=fl;
	head->next=fl;
	fl->lastOnPage=tail->prev;
	(*lineBytesFreedPtr)+=end-start;
	if (zero) {
	    bzero(fl+1,end-start-sizeof(fivmr_FreeLine));
	}
    }
}

static inline void lineSweepPage(fivmr_GC *gc,
                                 uintptr_t pageAddress,
				 fivmr_GCSpace space,
				 uintptr_t *lineBytesFreedPtr) {
    fivmr_UsedPage *page;
    fivmr_FreeLine head;
    fivmr_FreeLine tail;
    uintptr_t index;
    uintptr_t zeroCount=0;
    uintptr_t wordI;
    uint32_t *bits;
    fivmr_Settings *settings;
    
    settings=&fivmr_VMfromGC(gc)->settings;
    
    LOG(3,("Line sweeping %p in space %d",pageAddress,space));
    page=(fivmr_UsedPage*)pageAddress;
    head.next=&tail;
    tail.prev=&head;
    
    fivmr_assert(*fivmr_UsedPage_reserved(page,settings)!=0);
    
    /* pages in the object space should be swept by using the bitvector
       in the page header. */
    
    removeFreeLinesInPage(gc,page);
    fivmr_assert(*fivmr_UsedPage_status(page,settings)
                 == FIVMR_GCUPS_ZERO);
    
    LOG(4,("sweeping bits..."));
    
    bits=fivmr_UsedPage_bits(page,settings);
    
    /* now sweep the clear bits */
    for (wordI=0;
         wordI<FIVMR_UP_BITS_LENGTH(settings);
         ++wordI) {
        uint32_t word=bits[wordI];
        uintptr_t bitI=0;
        if (word!=(uint32_t)-1) {
            bool cont=true;
            
            while (cont) {
                uintptr_t start,end;
		
                while (word&1) {
                    word>>=1;
                    bitI++;
                }

                if (bitI==32) {
                    break;
                }
		    
                fivmr_assert(bitI<32);

                /* found a free one... */
                start=(wordI<<5)+bitI;
                if (start>=((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                            FIVMR_MIN_CHUNK_ALIGN(settings))) {
                    goto doneLineSweep;
                }
		    
                if (!word) {
                    bitI=0;
                    do {
                        word=bits[++wordI];
                    } while (!word);
                    if (wordI==(uint32_t)-1) {
                        cont=false;
                        goto skipBitSearch;
                    }
                }
		    
                while ((word&1)==0) {
                    word>>=1;
                    bitI++;
                }
            skipBitSearch:
		    
                /* found the end */
                end=(wordI<<5)+bitI;
		    
                fivmr_assert(end>start);
                fivmr_assert(start<((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                                    FIVMR_MIN_CHUNK_ALIGN(settings)));
                fivmr_assert(end<=((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                                   FIVMR_MIN_CHUNK_ALIGN(settings)+64));
                if (end>=((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                          FIVMR_MIN_CHUNK_ALIGN(settings))) {
                    end=(FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))
                        / FIVMR_MIN_CHUNK_ALIGN(settings);
                    fivmr_assert(end>start);
                    cont=false;
                } else {
                    fivmr_assert(fivmr_BitVec_get(bits,end));
                }

                fivmr_assert(!fivmr_BitVec_get(bits,end-1));
                if (start>0) {
                    fivmr_assert(fivmr_BitVec_get(bits,start-1));
                }
                fivmr_assert(!fivmr_BitVec_get(bits,start));
                if (FIVMR_ASSERTS_ON) {
                    for (index=start;index<end;++index) {
                        fivmr_assert(!fivmr_BitVec_get(bits,index));
                    }
                }
                LOG(5,("on page %p adding line from %p to %p",page,start,end));

                if (FIVMR_ASSERTS_ON) {
                    zeroCount+=end-start;
                }

                addLineInSweep(
                    settings,
                    &head,&tail,
                    (uintptr_t)page+FIVMR_PAGE_HEADER(settings)+start*FIVMR_MIN_CHUNK_ALIGN(settings),
                    (uintptr_t)page+FIVMR_PAGE_HEADER(settings)+end*FIVMR_MIN_CHUNK_ALIGN(settings),
                    lineBytesFreedPtr,false);
            }
        }
    }
doneLineSweep:
    
    if (false && FIVMR_ASSERTS_ON) {
        uintptr_t verifiedZeroCount=0;
        for (index=0;
             index<((FIVMR_PAGE_SIZE-FIVMR_PAGE_HEADER(settings))/
                    FIVMR_MIN_CHUNK_ALIGN(settings));
             ++index) {
            if (!fivmr_BitVec_get(bits,index)) {
                verifiedZeroCount++;
            }
        }
        fivmr_assert(zeroCount==verifiedZeroCount);
    }

    fivmr_assert(*fivmr_UsedPage_status(page,settings)
                 == FIVMR_GCUPS_ZERO);

    if (head.next!=&tail) {
	LOG(3,("Pushing free lines on page %p from %p to %p",
	       pageAddress,head.next,tail.prev));
	fivmr_assert(head.next->lastOnPage==tail.prev);
	fivmr_Lock_lock(&gc->gcLock);
	*fivmr_UsedPage_status(page,settings)=
	    FIVMR_GCUPS_FREE_LINES|((uintptr_t)head.next-(uintptr_t)page);
	head.next->prev=&gc->spaceData[space].freeLineHead;
	tail.prev->next=gc->spaceData[space].freeLineHead.next;
	gc->spaceData[space].freeLineHead.next->prev=tail.prev;
	gc->spaceData[space].freeLineHead.next=head.next;
	fivmr_Lock_unlock(&gc->gcLock);
    }
    
    fivmr_PageTable_setAssert(&gc->spaceData[space].pt,
                              pageAddress,
                              FIVMR_GCPS_ZERO,
                              FIVMR_GCPS_POPULATED);
}

static inline uintptr_t sweep(fivmr_GC *gc,
                              fivmr_GCSpace space,
			      uintptr_t *pagesFreedPtr,
			      uintptr_t *lineBytesFreedPtr) {
    uintptr_t pagesKept;
    uintptr_t pagesFreed;
    BunchOfPages bop;
    uintptr_t baseAddress;
    uint32_t *chunk;
    uintptr_t wordI;
    uintptr_t wordILimit;
    fivmr_PTIterator iter;

    pagesFreed=0;
    pagesKept=0;
    initBunchOfPages(&bop);

    LOG(2,("Sweeping space %d...",space));

    /* NB this is racing against the page table in the case that new
       chunks get allocated.  I think that's fine.  Note the fences,
       though... they're there for that reason.  Note also that there
       are similar fences in fivmr_pagetable.c. */
    for (fivmr_PTIterator_init(&iter,&gc->spaceData[space].pt);
         fivmr_PTIterator_valid(&iter);
         fivmr_PTIterator_next(&iter)) {
        baseAddress=iter.baseAddress;
        chunk=iter.chunk;
        wordILimit=iter.chunkLength;
        for (wordI=0;
             wordI<wordILimit;
             wordI++) {
            uint32_t oldWord;
            bool toFree=false;
            uintptr_t toKeep;
            if (shouldQuit(gc)) goto quit;

            for (;;) { /* cas loop */
                uint32_t curWord=oldWord=chunk[wordI];
                uint32_t newWord=0;
                toKeep=0;
                
                /* assert that our logic isn't totally broken */
                fivmr_assert(
                    fivmr_PageTable_getWord(
                        &gc->spaceData[space].pt,
                        baseAddress+((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))
                                     <<FIVMSYS_LOG_PAGE_SIZE))
                    ==chunk+wordI);
                
                if (curWord) {
                    uintptr_t bitI;
                    for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                        uint32_t curBits=
                            curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1);
				
                        switch (curBits) {
                        case FIVMR_GCPS_POPULATED:
                            curBits=FIVMR_GCPS_ZERO;
                            toFree=true;
                            break;
                        case FIVMR_GCPS_SHADED:
                            curBits=FIVMR_GCPS_ZERO;
                            toKeep++;
                            break;
                        case FIVMR_GCPS_RELINQUISHED:
                            curBits=FIVMR_GCPS_POPULATED;
                            toKeep++;
                            break;
                        default:
                            break;
                        }
                        curWord>>=FIVMR_PT_BITS_PER_PAGE;
                        newWord|=(curBits<<bitI);
                    }
                    if (fivmr_cas32_weak((int32_t*)(chunk+wordI),
                                         oldWord,newWord)) {
                        break;
                    }
                } else {
                    /* empty set of pages - go to next one */
                    break;
                }
            }
            pagesKept+=toKeep;
            if (toFree) {
                uint32_t curWord=oldWord;
                uintptr_t bitI;
                for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                    uint32_t curBits=
                        curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1);
                    if (curBits==FIVMR_GCPS_POPULATED) {
                        uintptr_t pageAddress=
                            baseAddress+
                            (((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))+
                              (bitI>>FIVMR_LOG_PT_BITS_PER_PAGE))
                             <<FIVMSYS_LOG_PAGE_SIZE);
                        LOG(3,("Freeing page %p with wordI=%" PRIuPTR
                               " and bitI=%" PRIuPTR " in space %d",
                               pageAddress,wordI,bitI,space));
                        reusePage(gc,&bop,(void*)pageAddress);
                        (*pagesFreedPtr)++;
                    }
                    curWord>>=FIVMR_PT_BITS_PER_PAGE;
                }
            }
            LOG(5,("oldWord = %u",oldWord));
            if (oldWord) {
                uint32_t curWord=oldWord;
                uintptr_t bitI;
                LOG(4,("line sweeping with curWord = %u",curWord));
                for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                    uint32_t curBits;
                    uintptr_t pageAddress;
                    curBits=curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1);
                    curWord>>=FIVMR_PT_BITS_PER_PAGE;
                    pageAddress=
                        baseAddress+
                        (((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))+
                          (bitI>>FIVMR_LOG_PT_BITS_PER_PAGE))
                         <<FIVMSYS_LOG_PAGE_SIZE);
                    if (curBits==FIVMR_GCPS_SHADED) {
                        lineSweepPage(gc,pageAddress,space,
                                      lineBytesFreedPtr);
                    } else if (curBits!=0) {
                        LOG(3,("Not line sweeping %p in space %d because curBits = %d",
                               pageAddress,space,curBits));
                    }
                }
            }
        }
    }
	
quit:
    reuseBunchOfPages(gc,&bop);
    
    LOG(2,("Freed %" PRIuPTR " pages, kept %" PRIuPTR" pages, freelines have %" PRIuPTR " bytes.",
	   *pagesFreedPtr,pagesKept,*lineBytesFreedPtr));

    return pagesFreed;
}

static inline void scanSharedArea(fivmr_GC *gc,
                                  fivmr_GCHeader **queue,
                                  fivmr_MemoryArea *area) {
    fivmr_Settings *settings=&fivmr_VMfromGC(gc)->settings;
    uintptr_t scanPoint;
    for (scanPoint=area->objList;scanPoint!=0;scanPoint=*(uintptr_t *)scanPoint) {
        fivmr_Object obj;

        if (shouldQuit(gc)) return;

        obj=scanPoint+sizeof(uintptr_t)+FIVMR_ALLOC_OFFSET(settings);
        fivmr_assert(fivmr_Object_isContiguous(settings,obj));
        collectorScanObject(gc,queue,obj);
    }
}

static inline void cleanPageTable(fivmr_GC *gc,
                                  fivmr_GCSpace space) {
    uintptr_t baseAddress;
    uint32_t *chunk;
    uintptr_t wordI;
    uintptr_t wordILimit;
    fivmr_PTIterator iter;

    for (fivmr_PTIterator_init(&iter,&gc->spaceData[space].pt);
         fivmr_PTIterator_valid(&iter);
         fivmr_PTIterator_next(&iter)) {
        baseAddress=iter.baseAddress;
        chunk=iter.chunk;
        wordILimit=iter.chunkLength;
        for (wordI=0;
             wordI<wordILimit;
             wordI++) {
            uint32_t oldWord;
            if (shouldQuit(gc)) return;
            for (;;) { /* cas loop */
                uint32_t curWord=oldWord=chunk[wordI];
                uint32_t newWord=0;
                    
                /* assert that our logic isn't totally broken */
                fivmr_assert(
                    fivmr_PageTable_getWord(
                        &gc->spaceData[space].pt,
                        baseAddress+((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))
                                     <<FIVMSYS_LOG_PAGE_SIZE))
                    ==chunk+wordI);
                    
                if (curWord) {
                    uintptr_t bitI;
                    for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                        uint32_t curBits=
                            curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1);
                            
                        switch (curBits) {
                        case FIVMR_GCPS_RELINQUISHED:
                            curBits=FIVMR_GCPS_POPULATED;
                            break;
                        default:
                            break;
                        }
                        curWord>>=FIVMR_PT_BITS_PER_PAGE;
                        newWord|=(curBits<<bitI);
                    }
                    if (fivmr_cas32_weak((int32_t*)(chunk+wordI),
                                         oldWord,newWord)) {
                        break;
                    }
                } else {
                    /* empty set of pages - go to next one */
                    break;
                }
            }
        }
    }
}

static void collectorThreadMain(void *arg) {
    fivmr_GC *gc;
    uintptr_t maxUsed=0;
    
    gc=(fivmr_GC*)arg;
    
    fivmr_Lock_lock(&fivmr_VMfromGC(gc)->lock);
    gc->thread=fivmr_ThreadHandle_current();
    fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->lock);
    
    LOG(1,("Thread %p operational; stack at %p.",gc->thread,&arg));
    
    for (;;) {
	fivmr_ThreadState *requesterHead;
	uintptr_t i;
	fivmr_Handle *h;
	fivmr_GCHeader *queue;
	fivmr_LargeObjectHeader *loh;
	fivmr_LargeObjectHeader *lohHead;
	fivmr_LargeObjectHeader *lohTail;
        fivmr_MachineCode *mc;
        fivmr_MachineCode *mcHead;
        fivmr_MachineCode *mcTail;
	uintptr_t pagesFreed;
	uintptr_t lineBytesFreed;
	uintptr_t objectsTraced;
	fivmr_Nanos startTime;
	fivmr_Nanos afterThreads;
	fivmr_Nanos afterRoots;
	fivmr_Nanos afterTrace;
	fivmr_Nanos afterCopy;
	fivmr_Nanos afterSmallSweep;
	fivmr_Nanos afterLargeSweep;
        fivmr_Nanos beforePTClean;
        fivmr_Nanos afterPTClean;
        bool shouldBeRunning;
        uintptr_t curString;
        uintptr_t stringStep;
	
	LOG(1,("Waiting for GC request."));
        for (;;) {
            fivmr_Lock_lock(&gc->gcLock);
            shouldBeRunning=fivmr_GC_shouldBeRunning(gc);
            fivmr_Lock_unlock(&gc->gcLock);
            
            if (shouldBeRunning || shouldQuit(gc)) {
                break;
            }
            
            LOG(11,("waiting on trigger in GC"));
            fivmr_CritSemaphore_down(&gc->triggerSema);
            LOG(11,("trigger received!"));
            /* need to reloop.  because someone could have done an 'up'
               spuriously.  I guess I could prevent that from happening.
               but I don't care enough about it, honestly. */
        }
        LOG(11,("GC starting"));
        
        if (shouldQuit(gc)) goto shutdown;

        fivmr_Lock_lock(&gc->gcLock);
	fivmr_assert(fivmr_GC_shouldBeRunning(gc));
	requesterHead=gc->requesterHead;
	gc->requesterHead=NULL;
	gc->asyncRequested=false;
        gc->lastStart=fivmr_curTime();
	fivmr_Lock_unlock(&gc->gcLock);
	
	startTime=fivmr_curTime();
	LOG(1,("Starting collection with %" PRIuPTR " pages used.",
	       gc->numPagesUsed));
	
        if (FIVMR_ASSERTS_ON && FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert(gc->numMarked==0);
        }

	fivmr_assert(gc->globalQueue==NULL);
	objectsTraced=0;
	pagesFreed=0;
	lineBytesFreed=0;
	
	assertPerThreadQueuesSound(gc);
        
        if (FIVMR_LOG_GC_MARK_TRAPS) {
            uint64_t stamp=fivmr_readCPUTimestamp();
            fp_log(0,
                   "collection beginning at %u:%u",2,
                   (uintptr_t)((stamp>>32)&0xffffffff),
                   (uintptr_t)((stamp>>0)&0xffffffff));
        }
	
	LOG(2,("Notifying mutators that collection is beginning..."));
	fivmr_assert(gc->phase==FIVMR_GCP_IDLE);
	gc->phase=FIVMR_GCP_PRE_INIT;

        /* in all configurations, this disables marking while we rotate mark
           bits.  in HFGC, it also flips all mutators to allocating in the
           semi-space to-space. */
        if (FIVMR_HFGC(&fivmr_VMfromGC(gc)->settings)) {
            flipSpaces(gc);
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_RELINQUISH_SPACE|
                                            FIVMR_TSEF_GC_MISC);
        } else {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_GC_MISC);
        }
	
        if (shouldQuit(gc)) goto shutdown;

	LOG(2,("Rotating mark bits..."));
	gc->numMarked=0;

	LOG(2,("old curShaded = %p, old invCurShaded = %p",
               gc->curShaded,gc->invCurShaded));
	gc->curShaded^=FIVMR_GC_SH_ALWAYS_MARKED;
        gc->invCurShaded^=FIVMR_GC_SH_ALWAYS_MARKED;
	
	/* now anything stored into objects will be shaded - but new objects will
	   still be allocated white.  this prevents a race where one thread
	   allocates a black object and another stores a white object into it. */

	LOG(2,("new curShaded = %p, new invCurShaded = %p",
               gc->curShaded,gc->invCurShaded));
        /* is this necessary? */
        if (FIVMR_GC_BLACK_STACK(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_GC_MISC);
        }
	
        if (FIVMR_ASSERTS_ON && FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_assert(gc->numMarked==0);
        }
	
        if (shouldQuit(gc)) goto shutdown;

	LOG(2,("Notifying mutators of trace start..."));
	gc->phase=FIVMR_GCP_INIT;
        /* make sure that everyone starts marking before we start allocating
           black.  without this, it's possible for a thread to allocate a black
           object and pass it to a thread that isn't marking yet; that thread may
           then store a white object into the black object and then kaboom. */
        if (FIVMR_AGGRESSIVE_OPT_CM_STORE_BARRIERS(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            (FIVMR_TSEF_GC_MISC|
                                             FIVMR_TSEF_SCAN_THREAD_ROOTS));
        } else {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_GC_MISC);
        }

	LOG(2,("Notifying mutators to allocate black..."));
	LOG(2,("old curShadedAlloc = %p",gc->curShadedAlloc));
	gc->curShadedAlloc^=FIVMR_GC_SH_ALWAYS_MARKED;
	/* now objects are allocated black. */
	
        if (shouldQuit(gc)) goto shutdown;

	LOG(2,("new curShadedAlloc = %p",gc->curShadedAlloc));
	fivmr_assert(gc->curShaded==gc->curShadedAlloc);
        if (FIVMR_GC_BLACK_STACK(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            FIVMR_TSEF_GC_MISC);
        }

        if (shouldQuit(gc)) goto shutdown;

	LOG(2,("Requesting stack scan..."));
	gc->phase=FIVMR_GCP_STACK_SCAN; /* this phase thing is stupid */
        if (FIVMR_AGGRESSIVE_OPT_CM_STORE_BARRIERS(&fivmr_VMfromGC(gc)->settings) &&
            !FIVMR_GC_BLACK_STACK(&fivmr_VMfromGC(gc)->settings)) {
            fivmr_ThreadState_softHandshake(
                fivmr_VMfromGC(gc),
                FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                (FIVMR_TSEF_GC_MISC|
                 FIVMR_TSEF_COMMIT_DESTRUCTORS));
        } else {
            fivmr_ThreadState_softHandshake(
                fivmr_VMfromGC(gc),
                FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                (FIVMR_TSEF_SCAN_THREAD_ROOTS|
                 FIVMR_TSEF_GC_MISC|
                 FIVMR_TSEF_COMMIT_DESTRUCTORS));
        }
	gc->phase=FIVMR_GCP_TRACE;
	
        if (shouldQuit(gc)) goto shutdown;

	afterThreads=fivmr_curTime();
        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            LOG(2,("Marked %" PRIuPTR " objects.",gc->numMarked));
        }

	/* move the global queue into the local one (this gives us thread
	   roots). */
	fivmr_Lock_lock(&gc->gcLock);
	queue=gc->globalQueue;
	gc->globalQueue=NULL;
	fivmr_Lock_unlock(&gc->gcLock);
	
	LOG(2,("We have the results of the stack scan, with head %p.  "
	       "Marking global roots...",
	       queue));
        for (i=0;i<(uintptr_t)fivmr_VMfromGC(gc)->payload->nContexts;++i) {
            if (shouldQuit(gc)) goto shutdown;
            scanContext(gc,&queue,fivmr_VMfromGC(gc)->baseContexts[i]);
        }
        
        if (FIVMR_CLASSLOADING(&fivmr_VMfromGC(gc)->settings)) {
            /* FIXME: to support class unloading, we'll need to remove this. */
            
            fivmr_Lock_lock(&fivmr_VMfromGC(gc)->typeDataLock);
            for (i=0;i<(uintptr_t)fivmr_VMfromGC(gc)->nDynContexts;++i) {
                if (shouldQuit(gc)) break;
                scanContext(gc,&queue,fivmr_VMfromGC(gc)->dynContexts[i]);
            }
            fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->typeDataLock);
            if (shouldQuit(gc)) goto shutdown;
        }
        
        for (i=0;i<=fivmr_VMfromGC(gc)->maxThreadID;i++) {
            if (shouldQuit(gc)) goto shutdown;
            collectorMarkObj(gc,&queue,fivmr_VMfromGC(gc)->javaThreads[i].obj);
        }
        
        for (i=fivmr_VMfromGC(gc)->payload->nRefFields;i-->0;) {
            if (shouldQuit(gc)) goto shutdown;
	    collectorMarkObj(gc,&queue,fivmr_VMfromGC(gc)->refFields[i]);
	}
	
        if (!FIVMR_CLASSLOADING(&fivmr_VMfromGC(gc)->settings)) {
            for (i=fivmr_VMfromGC(gc)->payload->nTypes;i-->0;) {
                fivmr_TypeData *td;
                if (shouldQuit(gc)) goto shutdown;
                td=fivmr_VMfromGC(gc)->payload->typeList[i];
                fivmr_assert(td->classObject!=0);
                collectorScanObject(gc,&queue,td->classObject);
            }
        }
        
        curString=fivmr_getFirstString(fivmr_VMfromGC(gc)->payload);
        stringStep=fivmr_getStringDistance(fivmr_VMfromGC(gc)->payload);
	for (i=fivmr_VMfromGC(gc)->payload->nStrings;i-->0;) {
            if (shouldQuit(gc)) goto shutdown;
	    /* strings may be synchronized on ... hence this nast. */
	    handleObjectHeader(gc,curString);
            curString+=stringStep;
	}
	
	fivmr_Lock_lock(&fivmr_VMfromGC(gc)->hrLock);
	/* FIXME: we're holding the lock for however long it takes...  wouldn't
	   need to do this if handles were GC'd. */
	for (h=fivmr_VMfromGC(gc)->hr.head.next;
             h!=&fivmr_VMfromGC(gc)->hr.tail;
             h=h->next) {
	    collectorMarkHandle(gc,&queue,h);
	}
	fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->hrLock);
	
        if (shouldQuit(gc)) goto shutdown;

        if (FIVMR_SCOPED_MEMORY(&fivmr_VMfromGC(gc)->settings)) {
            /* Scan immortal memory -- we don't have to "enter" this shared
             * area because it cannot go away */
            scanSharedArea(gc,&queue,
                           &fivmr_VMfromGC(gc)->gc.immortalMemoryArea);
            if (shouldQuit(gc)) goto shutdown;
            if (FIVMR_SCJ_SCOPES(&fivmr_VMfromGC(gc)->settings)) {
                /* Scan other shared areas */
                /* FIXME: We should "enter" these, to prevent their
                 * destruction before the scan is finished! */
                while (gc->areaQueue) {
                    fivmr_MemoryArea *area=gc->areaQueue;
                    scanSharedArea(gc,&queue,area);
                    if (shouldQuit(gc)) goto shutdown;
                    gc->areaQueue=(fivmr_MemoryArea *)(area->shared&~0x3);
                    area->shared=1;
                }
            } else if (FIVMR_RTSJ_SCOPES(&fivmr_VMfromGC(gc)->settings)) {
                fivmr_MemoryAreaStack *cur, *prev;
                fivmr_Lock_lock(&gc->sharedAreasLock);
                prev=NULL;
                cur=gc->sharedAreas;
                while (prev||cur) {
                    if (prev) {
                        prev->flags&=~FIVMR_MEMORYAREASTACK_GCINPROGRESS;
                        if (prev->flags&FIVMR_MEMORYAREASTACK_POP) {
                            fivmr_MemoryArea_pop(NULL, fivmr_VMfromGC(gc),
                                                 prev->area);
                            if (prev->flags&FIVMR_MEMORYAREASTACK_WAITING) {
                                fivmr_Lock_broadcast(&gc->sharedAreasLock);
                            }
                        }
                        if (prev->flags&FIVMR_MEMORYAREASTACK_FREE) {
                            fivmr_MemoryArea_free(NULL, prev->area);
                            fivmr_assert(!(prev->flags&FIVMR_MEMORYAREASTACK_WAITING));
                        }
                    }
                    prev=cur;
                    if (cur) {
                        cur->flags|=FIVMR_MEMORYAREASTACK_GCINPROGRESS;
                        fivmr_Lock_unlock(&gc->sharedAreasLock);
                        scanSharedArea(gc,&queue,cur->area);
                        fivmr_Lock_lock(&gc->sharedAreasLock);
                        cur=cur->next;
                    }
                }
                fivmr_Lock_unlock(&gc->sharedAreasLock);
            }
        }


        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            LOG(2,("Marked %" PRIuPTR " objects.",gc->numMarked));
        }
	afterRoots=fivmr_curTime();

	transitiveClosure(gc,&queue,&objectsTraced);
        if (shouldQuit(gc)) goto shutdown;
	if (processDestructors(gc,&queue)) {
	    transitiveClosure(gc,&queue,&objectsTraced);
	}
        if (shouldQuit(gc)) goto shutdown;
        
        if (FIVMR_HFGC(&fivmr_VMfromGC(gc)->settings)) {
            doneGCSpaceAlloc(gc);
            /* NOTE: we don't need a handshake to indicate that forwarding
               pointers have been installed, since transitiveClosure() already
               does a handshake at the end. */
        }
	
        if (shouldQuit(gc)) goto shutdown;

	afterTrace=fivmr_curTime();
	gc->phase=FIVMR_GCP_SWEEP;
        
        if (FIVMR_FILTERED_CM_STORE_BARRIERS(&fivmr_VMfromGC(gc)->settings)) {
            /* set tracing to false in all threads.  this does not require a soft
               handshake. */
            for (i=0;i<=fivmr_VMfromGC(gc)->maxThreadID;++i) {
                fivmr_ThreadState_byId(fivmr_VMfromGC(gc),i)->gc.tracing=false;
            }
        }
        
        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            LOG(2,("Marked %" PRIuPTR " objects.",gc->numMarked));
            fivmr_assert(gc->numMarked==objectsTraced);
            gc->numMarked=0;
        }
	LOG(2,("Traced %" PRIuPTR " objects.",objectsTraced));
	
	/* small page sweep for objects */
        sweep(gc,FIVMR_GC_OBJ_SPACE,&pagesFreed,&lineBytesFreed);
        if (shouldQuit(gc)) goto shutdown;

	afterSmallSweep=fivmr_curTime();

	LOG(2,("Sweeping large object pages..."));
	fivmr_Lock_lock(&gc->gcLock);
	loh=gc->largeObjectHead;
	gc->largeObjectHead=NULL;
	fivmr_Lock_unlock(&gc->gcLock);
	
	lohHead=NULL;
	lohTail=NULL;
	
        /* FIXME: GC is not quit-able in here! */
	while (loh!=NULL) {
	    fivmr_LargeObjectHeader *next=loh->next;
	    fivmr_GCHeader *hdr=loh->object;
	    if (fivmr_GCHeader_markBits(hdr)!=gc->curShaded) {
		uintptr_t numPages;
		numPages=fivmr_pages(loh->fullSize);
		freeLargeObject(gc,loh);
		pagesFreed+=numPages;
	    } else {
		loh->next=lohHead;
		lohHead=loh;
		if (lohTail==NULL) {
		    lohTail=loh;
		}
	    }
	    loh=next;
	}
	
	if (lohHead!=NULL) {
	    fivmr_Lock_lock(&gc->gcLock);
	    lohTail->next=gc->largeObjectHead;
	    gc->largeObjectHead=lohHead;
	    fivmr_Lock_unlock(&gc->gcLock);
	}

        if (shouldQuit(gc)) goto shutdown;
        
        LOG(2,("Sweeping machine codes..."));
        fivmr_Lock_lock(&gc->gcLock);
        mc=gc->machineCodeHead;
        gc->machineCodeHead=NULL;
        fivmr_Lock_unlock(&gc->gcLock);
        
        mcHead=NULL;
        mcTail=NULL;
        
        while (mc!=NULL) {
            fivmr_MachineCode *next=mc->next;
            if (!(mc->flags&FIVMR_MC_GC_MARKED)) {
                fivmr_MachineCode_down(mc);
            } else {
                fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_MARKED,0);
                mc->next=mcHead;
                mcHead=mc;
                if (mcTail==NULL) {
                    mcTail=mc;
                }
            }
            mc=next;
        }
        
        if (mcHead!=NULL) {
            fivmr_Lock_lock(&gc->gcLock);
            mcTail->next=gc->machineCodeHead;
            gc->machineCodeHead=mcHead;
            fivmr_Lock_unlock(&gc->gcLock);
        }
	
        if (shouldQuit(gc)) goto shutdown;
        
	afterLargeSweep=fivmr_curTime();
	
        if (FIVMR_HFGC(&fivmr_VMfromGC(gc)->settings)) {
            performCopying(gc);
            if (shouldQuit(gc)) goto shutdown;
            fivmr_ThreadState_softHandshake(fivmr_VMfromGC(gc),
                                            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
                                            0);
            sweepOldSpace(gc);
            if (shouldQuit(gc)) goto shutdown;
        }
        afterCopy=fivmr_curTime();
	
	gc->phase=FIVMR_GCP_IDLE;

        if (FIVMR_GC_DEBUG(&fivmr_VMfromGC(gc)->settings)) {
            LOG(2,("Marked %" PRIuPTR " objects.",gc->numMarked));
            fivmr_assert(gc->numMarked==0);
        }
        if (FIVMR_LOG_GC_MARK_TRAPS) {
            uint64_t stamp=fivmr_readCPUTimestamp();
            fp_log(0,
                   "collection ending at %u:%u",2,
                   (uintptr_t)((stamp>>32)&0xffffffff),
                   (uintptr_t)((stamp>>0)&0xffffffff));
        }
        if (gc->logGC || gc->logSyncGC) {
	    uintptr_t used=(gc->numPagesUsed<<FIVMSYS_LOG_PAGE_SIZE)-lineBytesFreed;
            bool isSync=(requesterHead!=NULL||gc->waiterHead!=NULL);
	    if (used>maxUsed) {
	        maxUsed=used;
	    }
            if (isSync || gc->logGC) {
                fivmr_Log_lockedPrintf("[GC (%s): %ums, %" PRIuPTR" bytes used (%"
                                       PRIuPTR " max)]\n",
                                       isSync?"sync":"async",
                                       (uint32_t)((afterLargeSweep-startTime)/1000/1000),
                                       used,
                                       maxUsed);
            }
        }
#if FIVMR_RTEMS
        /* some OS's, like RTEMS, cannot print 64-bit values */
	LOG(1,("Finished collection with %" PRIuPTR " pages used; freed %" PRIuPTR
	       " pages + %" PRIuPTR" bytes.  Collection took %u+%u+%u+%u+%u+%u=%u ns",
	       gc->numPagesUsed,pagesFreed,lineBytesFreed,
	       (uint32_t)(afterThreads-startTime),
	       (uint32_t)(afterRoots-afterThreads),
	       (uint32_t)(afterTrace-afterRoots),
	       (uint32_t)(afterSmallSweep-afterTrace),
	       (uint32_t)(afterLargeSweep-afterSmallSweep),
	       (uint32_t)(afterCopy-afterLargeSweep),
	       (uint32_t)(afterCopy-startTime)));
#else
	LOG(1,("Finished collection with %" PRIuPTR " pages used; freed %" PRIuPTR
	       " pages + %" PRIuPTR" bytes.  Collection took %" PRIu64 "+%" PRIu64
	       "+%" PRIu64 "+%" PRIu64 "+%" PRIu64 "+%" PRIu64
               "=%" PRIu64 " ns",
	       gc->numPagesUsed,pagesFreed,lineBytesFreed,
	       afterThreads-startTime,
	       afterRoots-afterThreads,
	       afterTrace-afterRoots,
	       afterSmallSweep-afterTrace,
	       afterLargeSweep-afterSmallSweep,
	       afterCopy-afterLargeSweep,
	       afterCopy-startTime));
#endif

	assertPerThreadQueuesSound(gc);
        
	/* GC done.  notify everyone. */
	fivmr_Lock_lock(&gc->gcLock);
	gc->iterationCount++;
        gc->lastEnd=fivmr_curTime();
        if (requesterHead!=NULL || gc->waiterHead!=NULL) {
            gc->blockedIterationCount++;
        }
	while (requesterHead!=NULL) {
	    fivmr_ThreadState *cur=requesterHead;
	    
            if (shouldQuit(gc)) goto shutdown;

	    cur->gc.gcFlags&=~FIVMR_GCDF_REQUESTED_GC;
	    requesterHead=cur->gc.requesterNext;
	    cur->gc.requesterNext=NULL;
	}
        while (gc->waiterHead!=NULL) {
            fivmr_ThreadState *cur=gc->waiterHead;
            
            if (shouldQuit(gc)) goto shutdown;

            cur->gc.gcFlags&=~FIVMR_GCDF_REQUESTED_GC;
            gc->waiterHead=cur->gc.requesterNext;
            cur->gc.requesterNext=NULL;
        }
	fivmr_Lock_unlock(&gc->gcLock);
        fivmr_Lock_lockedBroadcast(&gc->notificationLock);
        
        LOG(1,("Cleaning up page tables."));
        
        if (shouldQuit(gc)) goto shutdown;
        
        beforePTClean=fivmr_curTime();
        cleanPageTable(gc,FIVMR_GC_OBJ_SPACE);
        afterPTClean=fivmr_curTime();
        
        if (shouldQuit(gc)) goto shutdown;

        fivmr_Lock_lock(&gc->gcLock);
        gc->gcThreadTime+=afterPTClean-startTime;
        fivmr_Lock_unlock(&gc->gcLock);
        
#if FIVMR_RTEMS
        LOG(1,("Cleaned page tables in %u ns",
               (uint32_t)(afterPTClean-beforePTClean)));
#else
        LOG(1,("Cleaned page tables in %" PRIu64 " ns",
               afterPTClean-beforePTClean));
#endif
    }

shutdown:
    fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                             fivmr_VMfromGC(gc)->maxPriority);
    fivmr_Lock_lock(&fivmr_VMfromGC(gc)->lock);
    gc->thread=fivmr_ThreadHandle_zero();
    gc->threadDone=true;
    fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->lock);
    fivmr_Semaphore_up(&gc->doneSema);
    LOG(1,("GC thread dying!"));
    fivmr_debugMemory();
    /* anything else? */
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_init)(fivmr_GC *gc) {
    fivmr_GCSpace i;
    fivmr_Settings *settings=&fivmr_VMfromGC(gc)->settings;
    
    fivmr_debugMemory();

    /* do some verification of configuration */
    fivmr_assert(FIVMR_HFGC(settings) ^ FIVMR_CMRGC(settings));
    fivmr_assert(FIVMR_HM_NARROW(settings) ^ FIVMR_HM_POISONED(settings));
    fivmr_assert(FIVMR_OM_CONTIGUOUS(settings) ^ FIVMR_OM_FRAGMENTED(settings));
    
    if (FIVMR_HFGC(settings)) {
        LOG(1,("Initializing Hybrid-Fragmenting Concurrent-Mark-Region collector."));
        /* verify settings */
        fivmr_assert(FIVMR_SELF_MANAGE_MEM(settings));
        fivmr_assert(FIVMR_OM_FRAGMENTED(settings));
    } else {
        LOG(1,("Initializing Concurrent-Mark-Region collector."));
    }
    
    gc->threadDone=false;
    
    gc->curShaded=FIVMR_GC_SH_MARK1;
    gc->invCurShaded=FIVMR_GC_SH_MARK2;
    gc->zeroCurShaded=0;
    gc->curShadedAlloc=FIVMR_GC_SH_MARK1;
    gc->phase=FIVMR_GCP_IDLE;

    gc->blockTime=0;
    gc->invBlockTime=0;
    gc->gcThreadTime=0;
    gc->slowPathTime=0;
    gc->getPageTime=0;
    gc->getFreeLinesTime=0;
    gc->largeAllocTime=0;
    gc->freeLineSearchTime=0;
    
    /* assertion for firstLive */
    fivmr_assert(FIVMSYS_LOG_PAGE_SIZE<=sizeof(uint16_t)*8);

    LOG(1,("Page size = %" PRIuPTR,FIVMR_PAGE_SIZE));
    LOG(1,("UP bits length = %" PRIuPTR,FIVMR_UP_BITS_LENGTH(settings)));
    
    gc->gcTriggerPages=mogrifyHeapSize(settings,gc->gcTriggerPages);
    gc->maxPagesUsed=mogrifyHeapSize(settings,gc->maxPagesUsed);
    gc->maxMaxPagesUsed=gc->maxPagesUsed;
    
    if (FIVMR_SELF_MANAGE_MEM(settings)) {
        gc->reusePages=10; /* reuse 10 pages at a time */
    } else {
        gc->reqPages=10;
        gc->reusePages=1000;
    }

    LOG(1,("Using page trigger = %" PRIuPTR,gc->gcTriggerPages));
    LOG(1,("Max pages = %" PRIuPTR,gc->maxPagesUsed));
    
    if (FIVMR_SELF_MANAGE_MEM(settings)) {
        gc->memStart=(uintptr_t)fivmr_allocPages(gc->maxPagesUsed,&gc->isZero);
        gc->memEnd=gc->memStart+(gc->maxPagesUsed<<FIVMSYS_LOG_PAGE_SIZE);
        
        LOG(1,("Heap starts at %p, ends at %p",
               gc->memStart,gc->memEnd-1));
    }
    
    for (i=0;i<FIVMR_GC_NUM_GC_SPACES;++i) {
        if (FIVMR_SELF_MANAGE_MEM(settings)) {
            fivmr_PageTable_initFlat(&gc->spaceData[i].pt,
                                     gc->memStart,
                                     gc->maxPagesUsed<<FIVMSYS_LOG_PAGE_SIZE);
        } else {
            fivmr_PageTable_initML(&gc->spaceData[i].pt,
                                   fivmr_Priority_bound(FIVMR_PR_MAX,
                                                        fivmr_VMfromGC(gc)->maxPriority));
        }
	gc->spaceData[i].freeLineHead.prev=NULL;
	gc->spaceData[i].freeLineHead.next=&gc->spaceData[i].freeLineTail;
	gc->spaceData[i].freeLineTail.prev=&gc->spaceData[i].freeLineHead;
	gc->spaceData[i].freeLineTail.next=NULL;
    }
    
    if (FIVMR_SELF_MANAGE_MEM(settings)) {
        gc->nextFreePage=gc->memStart;
    }
    
    if (FIVMR_HFGC(settings)) {
        initSpaces(gc);
    }
    
    fivmr_Lock_init(&gc->gcLock,fivmr_Priority_bound(FIVMR_PR_MAX,
                                                     fivmr_VMfromGC(gc)->maxPriority));
    if (FIVMR_PREDICTABLE_OOME(settings)) {
        fivmr_Lock_init(&gc->requestLock,FIVMR_PR_INHERIT);
    }
    
    fivmr_Lock_init(&gc->notificationLock,FIVMR_PR_INHERIT);
    fivmr_CritSemaphore_init(&gc->triggerSema);
    fivmr_Semaphore_init(&gc->doneSema);
    
    /* use PIP locks for destructor processing since we want to enable the GC's
       priority to change. */
    fivmr_Lock_init(&gc->destructorLock,FIVMR_PR_INHERIT);

    gc->threadPriority=fivmr_ThreadPriority_min(gc->threadPriority,
                                                fivmr_VMfromGC(gc)->maxPriority);
    if (fivmr_VMfromGC(gc)->pool==NULL) {
        gc->thread=fivmr_Thread_spawn(collectorThreadMain,gc,gc->threadPriority);
    } else {
        gc->thread=fivmr_ThreadPool_spawn(fivmr_VMfromGC(gc)->pool,
                                          collectorThreadMain,gc,gc->threadPriority)->thread;
    }
    if (gc->thread == fivmr_ThreadHandle_zero()) {
        fivmr_abort("Could not start GC thread.");
    }

    fivmr_debugMemory();
}

static void assertGCDataClear(fivmr_ThreadState *ts) {
    fivmr_GCSpace i;
    fivmr_assert(ts->gc.queue==NULL);
    fivmr_assert(ts->gc.queueTail==NULL);
    if (!shouldQuit(&ts->vm->gc)) {
        fivmr_assert(ts->gc.requesterNext==NULL);
    }
    for (i=0;i<FIVMR_GC_NUM_GC_SPACES;++i) {
	fivmr_assert(ts->gc.alloc[i].bump==0);
	fivmr_assert(ts->gc.alloc[i].start==0);
	fivmr_assert(ts->gc.alloc[i].size==0);
        fivmr_assert(ts->gc.alloc[i].zero==0);
	fivmr_assert(ts->gc.alloc[i].usedPage==NULL);
	fivmr_assert(ts->gc.alloc[i].freeHead==NULL);
	fivmr_assert(ts->gc.alloc[i].freeTail==NULL);
        fivmr_assert(ts->gc.alloc[i].ssBump==0);
        fivmr_assert(ts->gc.alloc[i].ssEnd==0);
        fivmr_assert(ts->gc.alloc[i].ssSize==0);
    }
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        fivmr_assert(ts->gc.currentArea==&ts->gc.heapMemoryArea);
        fivmr_assert(ts->gc.currentArea->shared==0);
        fivmr_assert(ts->gc.baseStackEntry.area==ts->gc.currentArea);
        fivmr_assert(ts->gc.baseStackEntry.prev==NULL);
        fivmr_assert(ts->gc.baseStackEntry.next==NULL);
        fivmr_assert(ts->gc.scopeStack==&ts->gc.baseStackEntry);
        fivmr_assert(ts->gc.scopeBacking==NULL);
    }
    fivmr_assert(ts->gc.destructorHead==NULL);
    fivmr_assert(ts->gc.destructorTail==NULL);
}

static void markMethod(fivmr_ThreadState *ts,
                       uintptr_t pointer) {
    fivmr_MethodRec *mr;
    fivmr_MachineCode *mc;
    
    mr=fivmr_MachineCode_decodeMethodRec(pointer);
    mc=fivmr_MachineCode_decodeMachineCode(pointer);
    
    mutatorMarkObjNoisy(ts,mr->owner->classObject);
    
    if (mc!=NULL &&
        (mc->flags&(FIVMR_MC_GC_OWNED|FIVMR_MC_GC_MARKED))==FIVMR_MC_GC_OWNED) {
        fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_MARKED,FIVMR_MC_GC_MARKED);
    }
}

/* calls should be protected by the thread's lock. */
static void scanThreadRoots(fivmr_ThreadState *ts) {
    fivmr_VM *vm;
    fivmr_Frame *f;
    fivmr_NativeFrame *nf;
    uintptr_t i;
    fivmr_MemoryAreaStack *ms;
    uintptr_t ptr;
    
    vm=ts->vm;
    
    LOG(2,("Scanning thread roots for Thread %u.",ts->id));
    if (LOGGING(4)) {
        fivmr_ThreadState_dumpStackFor(ts);
    }

    if (FIVMR_BIASED_LOCKING(&ts->vm->settings)) {
        /* mark the object we're trying to unbias.  NOTE: unsure about this;
           it may be preferable to handle this using the same trick that
           we use for VMThreads */
        mutatorMarkObjNoisy(ts,ts->toUnbias);
    }
    
    /* if the thread is STARTING then none of the other stuff in this method is
       relevant; moreover, the thread may be initializing those data structures so
       any attempts we make to read them can lead to badness. */
    if (ts->execStatus==FIVMR_TSES_STARTING) {
        return;
    }

    /* stack scan */
    for (f=ts->curF;f!=NULL;f=f->up) {
	fivmr_assert(f!=f->up);
	if (fivmr_likely(f->id!=(uintptr_t)(intptr_t)-1)) {
	    fivmr_DebugRec *dr;
            fivmr_FrameType ft;
            
	    dr=fivmr_DebugRec_lookup(vm,f->id);
            ft=fivmr_DebugRec_getFrameType(&vm->settings,dr);
            
            /* mark reference variables from this stack frame */
            if (fivmr_unlikely((dr->ln_rm_c&FIVMR_DR_FAT))) {
                fivmr_FatDebugData *fdd;
                int32_t j;
                LOG(5,("Scanning fat GC map for id = %p",f->id));
                fdd=fivmr_DebugRec_decodeFatDebugData(dr->ln_rm_c);
                for (j=0;j<fdd->refMapSize;++j) {
                    uint32_t rm=(uint32_t)fdd->refMap[j];
                    uint32_t k;
                    for (k=0;k<32;++k) {
                        if ((rm&(1<<k))) {
                            mutatorMarkObjStackNoisy(
                                ts,
                                fivmr_Frame_getRef(f,ft,j*32+k));
                        }
                    }
                }
            } else {
                uintptr_t refMap;
                uintptr_t k=0;
                refMap=fivmr_DebugRec_decodeThinRefMap(dr->ln_rm_c);
                LOG(5,("Scanning thin GC map for id = %p, refMap = %p",f->id,refMap));
                for (k=0;k<FIVMR_DR_TRM_NUMBITS;++k) {
                    if ((refMap&(1<<k))) {
                        mutatorMarkObjStackNoisy(
                            ts,
                            fivmr_Frame_getRef(f,ft,k));
                    }
                }
            }
            
            /* mark classes corresponding to all methods on this stack frame.  note,
               those classes will then have references to any classes that can be
               referenced from the constant pool of that class. */
            if ((dr->ln_rm_c&FIVMR_DR_INLINED)) {
                fivmr_InlineMethodRec *imr=(fivmr_InlineMethodRec*)(dr->method&~(sizeof(void*)-1));
                for (;;) {
                    markMethod(ts,(uintptr_t)imr->method);
                    if ((imr->ln_c&FIVMR_IMR_INLINED)) {
                        imr=(fivmr_InlineMethodRec*)imr->caller;
                    } else {
                        markMethod(ts,imr->caller);
                        break;
                    }
                }
            } else {
                markMethod(ts,dr->method&~(sizeof(void*)-1));
            }
	}
	fivmr_assert(f!=f->up);
    }

    /* Scopes scan */
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        fivmr_MemoryAreaStack *cur;
        for (cur=ts->gc.scopeStack;cur!=&ts->gc.baseStackEntry;cur=cur->prev) {
            fivmr_MemoryArea *area=cur->area;
            uintptr_t scanPoint;
            if (FIVMR_SCJ_SCOPES(&ts->vm->settings)&&area->shared) {
                /* Continued below */
                break;
            }
            scanPoint=area->start;
            while (scanPoint<area->bump) {
                fivmr_Object obj;
                uintptr_t start;
                obj=scanPoint;
                start=(uintptr_t)fivmr_GCHeader_fromObject(
                    &ts->vm->settings,obj);
                if (*(uintptr_t *)start==0) {
                    scanPoint+=sizeof(uintptr_t);
                } else if (((*(uintptr_t *)start)&FIVMR_GC_MARKBITS_MASK)==
                           FIVMR_GC_SH_MARK1) {
                    /* Raw type */
                    scanPoint+=((*(uintptr_t *)start)&~FIVMR_GC_MARKBITS_MASK);
                } else {
                    fivmr_assert(fivmr_Object_isContiguous(&ts->vm->settings,obj));
                    mutatorScanObject(ts,obj);
                    scanPoint+=fivmr_Object_size(&ts->vm->settings,obj);
                }
            }
        }
        if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
            fivmr_Lock_lock(&ts->vm->gc.gcLock);
            for (;cur!=NULL&&cur->area->shared;cur=cur->prev) {
                if (cur->area->shared&0x2) {
                    continue;
                } else {
                    cur->area->shared=(uintptr_t)ts->vm->gc.areaQueue|0x3;
                    ts->vm->gc.areaQueue=cur->area;
                }
            }
            fivmr_Lock_unlock(&ts->vm->gc.gcLock);
        }
    }

    
    /* mark the misc thread roots */
    for (i=0;i<FIVMR_TS_MAX_ROOTS;++i) {
	mutatorMarkObjStackNoisy(ts,ts->roots[i]);
    }
    
    /* mark the state buffer (this is for patch points and possibly OSR)
       note, this code isn't super-efficient because it doesn't have to be.
       the state buffer should never be large and even if it ever is that
       occurence would be uncommon.  in any case, the size of the state
       buffer will be *way* smaller than the total depth of the stack and
       the complexity of scanning the stack. */
    if (ts->stateBufGCMap!=NULL) {
        for (i=0;i<ts->stateSize;++i) {
            if (fivmr_BitVec_get(ts->stateBufGCMap,i)) {
                if (i<FIVMR_TS_STATE_BUF_LEN) {
                    mutatorMarkObjStackNoisy(ts,ts->stateBuf[i]);
                } else {
                    mutatorMarkObjStackNoisy(ts,ts->stateBufOverflow[i]);
                }
            }
        }
    }

    /* mark the exception */
    /* NOTE: the exception currently cannot be stack allocated... but we're
       doing this out of an abundance of caution */
    mutatorMarkObjStackNoisy(ts,ts->curException);
    
    /* mark handles */
    mutatorMarkHandleNoisy(ts,ts->curExceptionHandle);
    
    for (nf=ts->curNF;nf!=NULL;nf=nf->up) {
	fivmr_Handle *h;
	for (h=nf->hr.head.next;h!=&nf->hr.tail;h=h->next) {
	    mutatorMarkHandleNoisy(ts,h);
	}
    }
    
    /* mark the stack-allocated objects. */
    for (ptr=ts->gc.alloc[FIVMR_GC_SA_SPACE].start;
         ptr<ts->gc.alloc[FIVMR_GC_SA_SPACE].bump;) {
        fivmr_Object obj;
        uintptr_t start;
        obj=ptr;
        start=fivmr_GCHeader_chunkStart(
            &vm->settings,
            fivmr_GCHeader_fromObject(
                &vm->settings,
                obj));
        if (*((uintptr_t*)start)!=0) {
            fivmr_assert(fivmr_Object_isContiguous(&vm->settings,obj));
            mutatorScanObject(ts,obj);
            ptr+=fivmr_Object_size(&vm->settings,obj);
        } else {
            ptr+=sizeof(uintptr_t);
        }
    }

    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        /* mark BackingStoreID objects on the scope stack */
        for (ms=ts->gc.scopeStack;ms!=NULL;ms=ms->prev) {
            /* The heap area does not have a bsid */
            if (ms->area->javaArea) {
                mutatorScanObject(ts,ms->area->javaArea);
            }
        }
    }
}

static void pushShadeValues(fivmr_ThreadState *ts) {
    fivmr_GC *gc;
    gc=&ts->vm->gc;
    
    ts->gc.invCurShaded=gc->invCurShaded>>FIVMR_GC_MARKBITS_SHIFT;
    ts->gc.curShadedAlloc=gc->curShadedAlloc;
    ts->gc.zeroCurShaded=0;
    ts->gc.tracing=fivmr_GC_isTracing(gc->phase);
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        ts->gc.heapMemoryArea.scopeID=gc->curShadedAlloc|(uintptr_t)&ts->vm->gc.heapMemoryArea.scopeID_s;
    }
}

/* calls should be protected by the thread's lock. */
static void commitQueue(fivmr_ThreadState *ts) {
    fivmr_GC *gc;
    gc=&ts->vm->gc;
    
    if (ts->gc.queue!=NULL) {
	fivmr_Lock_lock(&gc->gcLock);
	LOG(3,("Thread %u committing queue with head %p and tail %p, old global "
	       "head is %p",
	       ts->id,ts->gc.queue,ts->gc.queueTail,gc->globalQueue));
	fivmr_GCHeader_setNext(ts->gc.queueTail,
			       gc->globalQueue);
	gc->globalQueue=ts->gc.queue;
	fivmr_Lock_unlock(&gc->gcLock);
	ts->gc.queue=NULL;
	ts->gc.queueTail=NULL;
    }
}

/* calls should be protected by thread's lock. */
static void commitDestructors(fivmr_ThreadState *ts) {
    fivmr_GC *gc;
    gc=&ts->vm->gc;
    
    if (ts->gc.destructorHead!=NULL) {
	fivmr_assert(ts->gc.destructorTail!=NULL);
        
	fivmr_Lock_lock(&gc->destructorLock);
	LOG(3,("Thread %u committing destructors with head %p and tail %p, old "
	       "global head is %p",
	       ts->id,ts->gc.destructorHead,ts->gc.destructorTail,
	       gc->destructorHead));
	ts->gc.destructorTail->next=gc->destructorHead;
	gc->destructorHead=ts->gc.destructorHead;
	fivmr_Lock_unlock(&gc->destructorLock);
	ts->gc.destructorHead=NULL;
	ts->gc.destructorTail=NULL;
    }
    fivmr_assert(ts->gc.destructorHead==NULL);
    fivmr_assert(ts->gc.destructorTail==NULL);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_handleHandshake)(fivmr_ThreadState *ts) {
    if ((ts->execFlags&FIVMR_TSEF_GC_MISC)!=0) {
	LOG(2,("Thread %u pushing new shade values.",ts->id));
	pushShadeValues(ts);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_SCAN_THREAD_ROOTS)!=0) {
	LOG(2,("Thread %u scanning thread roots.",ts->id));
	scanThreadRoots(ts);
	ts->execFlags&=~FIVMR_TSEF_SCAN_THREAD_ROOTS;
    }
    
    if ((ts->execFlags&FIVMR_TSEF_GC_MISC)!=0) {
	LOG(2,("Thread %u committing queue.",ts->id));
	commitQueue(ts);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_COMMIT_DESTRUCTORS)!=0) {
	LOG(2,("Thread %u committing destructors.",ts->id));
	commitDestructors(ts);
	ts->execFlags&=~FIVMR_TSEF_COMMIT_DESTRUCTORS;
    }

    if ((ts->execFlags&FIVMR_TSEF_RELINQUISH_SPACE)!=0) {
        fivmr_assert(FIVMR_HFGC(&ts->vm->settings));
        relinquishSpacesContext(ts);
        ts->execFlags&=~FIVMR_TSEF_RELINQUISH_SPACE;
    }

    ts->execFlags&=~FIVMR_TSEF_GC_MISC;
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_clear)(fivmr_ThreadState *ts) {
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        ts->gc.currentArea=&ts->gc.heapMemoryArea;
        ts->gc.baseStackEntry.area=ts->gc.currentArea;
        ts->gc.baseStackEntry.prev=NULL;
        ts->gc.baseStackEntry.next=NULL;
        ts->gc.scopeStack=&ts->gc.baseStackEntry;
        ts->gc.scopeBacking=NULL;
    }

    /* all the rest of this stuff should have been cleared by the thread
       tear down. */
    assertGCDataClear(ts);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_startThread)(fivmr_ThreadState *ts) {
    assertGCDataClear(ts);
    pushShadeValues(ts);
    
    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "starting thread at %u:%u",2,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff));
    }
    
    /* anything else? */
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_commitThread)(fivmr_ThreadState *ts) {
    LOG(2,("Thread %u committing; committing its queue and relinquishing its page.",ts->id));

    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "committing thread at %u:%u",2,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff));
    }
    
    commitQueue(ts);
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        while (ts->gc.scopeStack->prev) {
            fivmr_MemoryArea *area=ts->gc.scopeStack->area;
            fivmr_MemoryArea_pop(ts, ts->vm, area);
            fivmr_MemoryArea_free(ts, area);
        }
        if (ts->gc.scopeBacking) {
            fivmr_ScopeBacking_free(ts);
        }
    }
    relinquishAllocationContexts(ts);
    commitDestructors(ts);
    assertGCDataClear(ts);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_asyncCollect)(fivmr_GC *gc) {
    bool shouldTrigger;
    
    if (gc->logGC) {
        fivmr_Log_lockedPrintf("[Thread %u requesting Async GC]\n",
                               fivmr_ThreadState_get(fivmr_VMfromGC(gc))->id);
    }
    LOG(1,("Thread %u requesting GC asynchronously.",
	   fivmr_ThreadState_get(fivmr_VMfromGC(gc))->id));
    fivmr_Lock_lock(&gc->gcLock);
    shouldTrigger=!fivmr_GC_hasBeenTriggered(gc);
    gc->asyncRequested=true;
    fivmr_Lock_unlock(&gc->gcLock);
    
    if (shouldTrigger) {
        fivmr_CritSemaphore_up(&gc->triggerSema);
    }
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_collectFromNative)(fivmr_GC *gc,
                                                        const char *descrIn,
                                                        const char *descrWhat) {
    fivmr_ThreadState *ts;
    fivmr_Nanos before=0;
    bool shouldTrigger;
    
    if (fivmr_Thread_isInterrupt()) {
	LOG(1,("collectFromNative called from an interrupt."));
	return;
    }

    before=fivmr_curTime();

    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    if (FIVMR_LOG_GC_MARK_TRAPS) {
        uint64_t stamp=fivmr_readCPUTimestamp();
        fp_log(ts->id,
               "blocking voluntarily on GC at %u:%u",2,
               (uintptr_t)((stamp>>32)&0xffffffff),
               (uintptr_t)((stamp>>0)&0xffffffff));
    }
    if (gc->logSyncGC) {
        fivmr_Log_lockedPrintf("[Thread %u requesting Sync GC in %s%s%s]\n",
                               ts->id,descrIn,
                               (descrWhat==NULL?"":", allocating "),
                               (descrWhat==NULL?"":descrWhat));
    }
    LOG(1,("Thread %u requesting GC synchronously.",ts->id));
    fivmr_Lock_lock(&gc->gcLock);
    ts->gc.gcFlags|=FIVMR_GCDF_REQUESTED_GC;
    shouldTrigger=!fivmr_GC_hasBeenTriggered(gc);
    ts->gc.requesterNext=gc->requesterHead;
    gc->requesterHead=ts;
    fivmr_Lock_unlock(&gc->gcLock);
    
    if (shouldTrigger) {
        fivmr_CritSemaphore_up(&gc->triggerSema);
    }
    
    fivmr_Lock_lock(&gc->notificationLock);
    while ((ts->gc.gcFlags&FIVMR_GCDF_REQUESTED_GC) && !shouldQuit(&ts->vm->gc)) {
	fivmr_Lock_wait(&gc->notificationLock);
    }
    fivmr_Lock_unlock(&gc->notificationLock);

    LOG(2,("Thread %u woke up after synchronous GC request.",ts->id));
    
    gc->blockTime+=fivmr_curTime()-before;
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_collectFromJava)(fivmr_GC *gc,
                                                      const char *descrIn,
                                                      const char *descrWhat) {
    fivmr_ThreadState *ts;
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    fivmr_ThreadState_goToNative(ts);
    fivmr_GC_collectFromNative(gc,descrIn,descrWhat);
    fivmr_ThreadState_goToJava(ts);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_claimMachineCode)(fivmr_ThreadState *ts,
                                                       fivmr_MachineCode *mc) {
    fivmr_GC *gc;
    
    gc=&ts->vm->gc;
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    fivmr_assert((mc->flags&FIVMR_MC_GC_OWNED)==0);
    
    fivmr_MachineCode_up(mc);
    
    fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_OWNED,FIVMR_MC_GC_OWNED);
    
    if (fivmr_GC_isCollecting(gc->phase)) {
        fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_MARKED,FIVMR_MC_GC_MARKED);
    } else {
        fivmr_MachineCode_setFlag(mc,FIVMR_MC_GC_MARKED,0);
    }
    
    fivmr_Lock_lock(&gc->gcLock);
    mc->next=gc->machineCodeHead;
    gc->machineCodeHead=mc;
    fivmr_Lock_unlock(&gc->gcLock);
}

bool FIVMR_CONCAT(FIVMBUILD_GC_NAME,_getNextDestructor)(fivmr_GC *gc,
                                                        fivmr_Handle *objCell,
                                                        bool wait) {
    fivmr_ThreadState *ts;
    
    if (!FIVMR_FINALIZATION_SUPPORTED(&fivmr_VMfromGC(gc)->settings)) {
        return false;
    }
    
    ts=fivmr_ThreadState_get(fivmr_VMfromGC(gc));
    
    for (;;) {
	/* go to native so that we don't stall GC while waiting for destructors.
	   note that even acquiring the destructor lock may block, so we need to
	   be careful. */
	fivmr_ThreadState_goToNative(ts);
	
	fivmr_Lock_lock(&gc->destructorLock);

	if (gc->destructorsToRun==NULL && !wait) {
	    fivmr_Lock_unlock(&gc->destructorLock);
	    fivmr_ThreadState_goToJava(ts);
	    return false;
	}
	
	while (gc->destructorsToRun==NULL) {
            fivmr_Lock *lock=&gc->destructorLock;
            fivmr_assert(wait);
            fivmr_ThreadState_checkExitHoldingLocks(ts,1,&lock);
            fivmr_Lock_wait(&gc->destructorLock);
	}
        
        fivmr_assert(gc->destructorsToRun!=NULL);

	/* this is tricky ... we cannot block on going to Java while holding the
	   destructor lock, as this could stall the GC.  so, we optimistically
	   try to go to Java the fast way, and if we fail, then we simply
	   retry the whole thing. */
	
	if (fivmr_ThreadState_tryGoToJava(ts)) {
	    /* went into Java the easy way, we can now examine the object and
	       remove it from the list.  note that this needs to be dealt with
	       care because as soon as the destructor is removed from the list
	       (and the destructor lock is released), the destructor itself
	       may die, and the object will die at the next safepoint unless
	       it is rooted somewhere. */
            LOG(2,("dequeueing finalizable %p",gc->destructorsToRun->object));
	    fivmr_objectArrayStore(ts,objCell->obj,0,gc->destructorsToRun->object,0);
	    gc->destructorsToRun=gc->destructorsToRun->next;
	    fivmr_Lock_unlock(&gc->destructorLock);
            if (FIVMR_ASSERTS_ON) {
                fivmr_assertNoException(ts,"in fivmr_GC_getNextDestructor()");
            }
	    return true;
	} else {
	    /* need to take slow path to go into Java.  first release the
	       destructor lock, then go to Java, and then reloop and try again. */
	    fivmr_Lock_unlock(&gc->destructorLock);
	    fivmr_ThreadState_goToJava(ts);
	}
    }
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_resetStats)(fivmr_GC *gc) {
    fivmr_Lock_lock(&gc->gcLock);
    gc->iterationCount=0;
    gc->blockedIterationCount=0;
    gc->blockTime=0;
    gc->invBlockTime=0;
    gc->gcThreadTime=0;
    gc->slowPathTime=0;
    gc->getPageTime=0;
    gc->getFreeLinesTime=0;
    gc->largeAllocTime=0;
    gc->freeLineSearchTime=0;
    fivmr_Lock_unlock(&gc->gcLock);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_report)(fivmr_GC *gc,
                                             const char *name) {
    fivmr_Lock_lock(&gc->gcLock);
    fivmr_Log_lockedPrintf(
        "GC %s report:\n"
        "                   Number of iterations completed: %" PRIu64 "\n"
        "       Number of synchronous iterations completed: %" PRIu64 "\n"
        "             Number of trace iterations completed: %" PRIu64 "\n"
        "                Time threads spent blocking on GC: %" PRIu64 " ns\n"
        "  Time threads spent blocking involuntarily on GC: %" PRIu64 " ns\n"
        "               Time that the GC thread was active: %" PRIu64 " ns\n"
        "        Total time spent in allocation slow paths: %" PRIu64 " ns\n"
        "                Total time spent allocating pages: %" PRIu64 " ns\n"
        "              Total time spent getting free lines: %" PRIu64 " ns\n"
        "        Total time spent allocating large objects: %" PRIu64 " ns\n"
        "            Total time spent searching free lines: %" PRIu64 " ns\n",
        name,
        gc->iterationCount,
        gc->blockedIterationCount,
        gc->traceIterationCount,
        gc->blockTime,
        gc->invBlockTime,
        gc->gcThreadTime,
        gc->slowPathTime,
        gc->getPageTime,
        gc->getFreeLinesTime,
        gc->largeAllocTime,
        gc->freeLineSearchTime);
    fivmr_Lock_unlock(&gc->gcLock);
}

/* FIXME: these computations are wrong!   (or maybe not.  not sure.  maybe I fixed
   them.) */

int64_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_freeMemory)(fivmr_GC *gc) {
    int64_t result;
    uintptr_t free=gc->numFreePages;
    /* printf("free pages = %p\n",free); */
    result=(free<<FIVMSYS_LOG_PAGE_SIZE);
    /* printf("returning = %u\n",(unsigned)result); */
    fivmr_assert(result>=0);
    return result;
}

int64_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_totalMemory)(fivmr_GC *gc) {
    int64_t result;
    uintptr_t used=gc->numPagesUsed;
    uintptr_t free=gc->numFreePages;
    /* printf("used + free = %p + %p = %p\n",used,free,used+free); */
    result=((used+free)<<FIVMSYS_LOG_PAGE_SIZE);
    /* printf("returning = %u\n",(unsigned)result); */
    fivmr_assert(result>=0);
    return result;
}

int64_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_maxMemory)(fivmr_GC *gc) {
    return gc->maxPagesUsed<<FIVMSYS_LOG_PAGE_SIZE;
}

int64_t FIVMR_CONCAT(FIVMBUILD_GC_NAME,_numIterationsCompleted)(fivmr_GC *gc) {
    int64_t result;
    fivmr_Lock_lock(&gc->gcLock);
    result=(int64_t)gc->iterationCount;
    fivmr_Lock_unlock(&gc->gcLock);
    return result;
}

bool FIVMR_CONCAT(FIVMBUILD_GC_NAME,_setMaxHeap)(fivmr_GC *gc,
                                                 int64_t bytes) {
    bool result;
    uintptr_t pages;
    
    pages=mogrifyHeapSize(&fivmr_VMfromGC(gc)->settings,
                          bytes/FIVMR_PAGE_SIZE);
    
    fivmr_Lock_lock(&gc->gcLock);
    if (pages < (uintptr_t)gc->numPagesUsed || pages > gc->maxMaxPagesUsed) {
        result=false;
    } else {
        gc->maxPagesUsed = pages;
        result=true;
    }
    fivmr_Lock_unlock(&gc->gcLock);
    
    return result;
}

bool FIVMR_CONCAT(FIVMBUILD_GC_NAME,_setTrigger)(fivmr_GC *gc,
                                                 int64_t bytes) {
    uintptr_t pages;
    
    pages=mogrifyHeapSize(&fivmr_VMfromGC(gc)->settings,
                          bytes/FIVMR_PAGE_SIZE);
    
    if (pages==0) {
        return false;
    }
    
    fivmr_Lock_lock(&gc->gcLock);
    gc->gcTriggerPages=pages;
    fivmr_GC_asyncCollect(gc);
    fivmr_Lock_unlock(&gc->gcLock);
    
    return true;
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_setPriority)(fivmr_GC *gc,
                                                  fivmr_ThreadPriority prio) {
    prio=fivmr_ThreadPriority_min(prio,fivmr_VMfromGC(gc)->maxPriority);
    fivmr_Thread_setPriority(gc->thread,gc->threadPriority=prio);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_signalExit)(fivmr_GC *gc) {
    LOG(1,("Signaling GC exit"));
    fivmr_Lock_lockedBroadcast(&gc->notificationLock);
    fivmr_Lock_lockedBroadcast(&gc->destructorLock);
}

void FIVMR_CONCAT(FIVMBUILD_GC_NAME,_shutdown)(fivmr_GC *gc) {
    LOG(1,("Telling GC thread to die."));
    
    fivmr_debugMemory();

    fivmr_Lock_lock(&fivmr_VMfromGC(gc)->lock);
    /* check if the GC thread is already done; if it's already done then
       we don't have to tell it anything and anyway gc->thread will be NULL */
    if (!gc->threadDone) {
        fivmr_Thread_setPriority(gc->thread,fivmr_VMfromGC(gc)->maxPriority);
        fivmr_CritSemaphore_up(&gc->triggerSema);
    }
    fivmr_Lock_unlock(&fivmr_VMfromGC(gc)->lock);
    
    /* await death (might have already happened) */
    fivmr_Semaphore_down(&gc->doneSema);
    fivmr_assert(gc->threadDone);
    LOG(1,("GC thread has died!"));

    fivmr_debugMemory();

    if (FIVMR_HFGC(&fivmr_VMfromGC(gc)->settings)) {
        freeSpaces(gc);
    }

    if (FIVMR_SELF_MANAGE_MEM(&fivmr_VMfromGC(gc)->settings)) {
        fivmr_freePages((void*)gc->memStart,gc->maxPagesUsed);
    } else {
        fivmr_LargeObjectHeader *cur;

        /* free the freelist */
        while (gc->freePageHead!=NULL) {
            fivmr_FreePage *next=gc->freePageHead->next;
            fivmr_freePage(gc->freePageHead);
            gc->freePageHead=next;
        }
        
        /* free any other pages known to the GC (i.e. occupied non-free ones).
           this includes pages that had been used by the mutator since those
           would have been relinquished in the mutator commit that happened
           during exit. */
        fivmr_PageTable_freeNonZeroPages(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt);
        
        /* free large objects */
        cur=gc->largeObjectHead;
        while (cur!=NULL) {
            fivmr_LargeObjectHeader *next=cur->next;
            free(cur);
            cur=next;
        }
    }

    fivmr_PageTable_free(&gc->spaceData[FIVMR_GC_OBJ_SPACE].pt);
    
    fivmr_Lock_destroy(&gc->gcLock);
    if (FIVMR_PREDICTABLE_OOME(&fivmr_VMfromGC(gc)->settings)) {
        fivmr_Lock_destroy(&gc->requestLock);
    }
    fivmr_Lock_destroy(&gc->notificationLock);
    fivmr_CritSemaphore_destroy(&gc->triggerSema);
    fivmr_Semaphore_destroy(&gc->doneSema);
    fivmr_Lock_destroy(&gc->destructorLock);
    
    fivmr_debugMemory();

    /* FIXME is that it?  or do we have some other allocation as well? */
}

#endif
# 1 "fivmr_debug.c"
/*
 * fivmr_debug.c
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

uintptr_t fivmr_iterateDebugFrames(fivmr_VM *vm,
                                   fivmr_Frame *f,
				   fivmr_DebugFrameCback cback,
				   uintptr_t arg) {
    uintptr_t result;
    fivmr_MethodRec *lastMR=NULL;

    for (;f!=NULL;f=f->up) {
	LOG(10,("observed f = %p, id = %p",
		f,f->id));
	fivmr_assert(f!=f->up);
	if (f->id!=(uintptr_t)(intptr_t)-1) {
	    fivmr_DebugRec *dr;
	    int32_t lineNumber;
	    
	    dr=fivmr_DebugRec_lookup(vm,f->id);
	    
	    LOG(10,("have dr = %p, dr->ln_rm_c = %p, dr->method = %p",
		    dr,dr->ln_rm_c,dr->method));

	    if ((dr->ln_rm_c&FIVMR_DR_FAT)) {
		lineNumber=fivmr_DebugRec_decodeFatDebugData(dr->ln_rm_c)->lineNumber;
	    } else {
		lineNumber=fivmr_DebugRec_decodeThinLineNumber(dr->ln_rm_c);
	    }
	    
	    if (dr->ln_rm_c&(FIVMR_DR_INLINED)) {
		fivmr_InlineMethodRec *imr;
                bool isPatchPoint;
                isPatchPoint=fivmr_MachineCode_isPatchPoint(dr->method);
                imr=(fivmr_InlineMethodRec*)(dr->method&~(sizeof(void*)-1));
		for (;;) {
                    if (!isPatchPoint ||
                        lastMR!=imr->method) {
                        LOG(10,("calling cback(%p, %s, %d)",
                                arg,fivmr_MethodRec_describe(imr->method),lineNumber));
                        if ((result=cback(vm,
                                          arg,
                                          lastMR=imr->method,
                                          lineNumber))) {
                            return result;
                        }
                    }
                    isPatchPoint=false;
		    lineNumber=fivmr_InlineMethodRec_decodeLineNumber(imr->ln_c);
		    if (imr->ln_c&(FIVMR_IMR_INLINED)) {
			imr=(fivmr_InlineMethodRec*)imr->caller;
		    } else {
			LOG(10,("calling cback(%p, %s, %d)",
				arg,
				fivmr_MachineCode_decodeMethodRec(imr->caller),
				lineNumber));
			if ((result=cback(
                                 vm,
                                 arg,
                                 lastMR=fivmr_MachineCode_decodeMethodRec(imr->caller),
                                 lineNumber))) {
			    return result;
			}
			break;
		    }
		}
	    } else {
                if (!fivmr_MachineCode_isPatchPoint(dr->method) ||
                    lastMR!=fivmr_MachineCode_decodeMethodRec(dr->method)) {
                    LOG(10,("calling cback(%p, %s, %d)",
                            arg,
                            fivmr_MachineCode_decodeMethodRec(dr->method),
                            lineNumber));
                    if ((result=cback(
                             vm,
                             arg,
                             lastMR=fivmr_MachineCode_decodeMethodRec(dr->method),
                             lineNumber))) {
                        return result;
                    }
                }
	    }
	}
	fivmr_assert(f!=f->up);
    }
    
    return 0;
}

static uintptr_t stackDumpCback(fivmr_VM *vm,
                                uintptr_t arg,
				fivmr_MethodRec *mr,
				int32_t lineNumber) {
    const char *filename;
    if (mr->owner->filename==NULL) {
	filename="<unavailable>";
    } else {
	filename=mr->owner->filename;
    }
    fivmr_Log_printf("   %s (%s:%d)\n",
		     fivmr_MethodRec_describe(mr),
		     filename,
		     lineNumber);
    return 0;
}

void fivmr_dumpStackFromNoHeading(fivmr_VM *vm,
                                  fivmr_Frame *f) {
    fivmr_iterateDebugFrames(vm,f,stackDumpCback,0);
    fivmr_Log_printf("   (end stack dump)\n");
}

void fivmr_dumpStackFrom(fivmr_VM *vm,
                         fivmr_Frame *f,
                         const char *msg) {
    fivmr_Log_lock();
    if (msg!=NULL) {
	fivmr_Log_printf("fivmr stack dump for %s:\n",msg);
    } else {
	fivmr_Log_printf("fivmr stack dump:\n");
    }
    fivmr_dumpStackFromNoHeading(vm,f);
    fivmr_Log_unlock();
}

static uintptr_t fivmr_iterateDebugFrames_forJava_cback(fivmr_VM *vm,
                                                        uintptr_t arg,
                                                        fivmr_MethodRec *mr,
                                                        int32_t lineNumber) {
    return fivmr_DumpStackCback_cback(fivmr_ThreadState_get(vm),
                                      arg,
                                      mr,
                                      lineNumber);
}

uintptr_t fivmr_iterateDebugFrames_forJava(fivmr_VM *vm,
                                           fivmr_Frame *f,
					   fivmr_Object cback) {
    return fivmr_iterateDebugFrames(vm,
                                    f,
				    fivmr_iterateDebugFrames_forJava_cback,
				    cback);
}

static uintptr_t methodForStackDepthCback(fivmr_VM *vm,
                                          uintptr_t arg,
					  fivmr_MethodRec *mr,
					  int32_t lineNumber) {
    int32_t *cnt=(int32_t*)arg;
    if ((*cnt)--) {
	return 0;
    } else {
	return (uintptr_t)mr;
    }
}

fivmr_MethodRec *fivmr_methodForStackDepth(fivmr_VM *vm,
                                           fivmr_Frame *f,
					   int32_t depth) {
    return (fivmr_MethodRec*)fivmr_iterateDebugFrames(vm,
                                                      f,
						      methodForStackDepthCback,
						      (uintptr_t)&depth);
}

fivmr_MethodRec *fivmr_findCaller(fivmr_ThreadState *ts,
				  int32_t depth) {
    if (depth<0) {
	return ts->curNF->jni.mr;
    } else {
	return fivmr_methodForStackDepth(ts->vm,ts->curF,depth);
    }
}

static fivmr_ThreadStackTrace *alloc_tst(void) {
    fivmr_ThreadStackTrace *result=
	(fivmr_ThreadStackTrace*)fivmr_mallocAssert(sizeof(fivmr_ThreadStackTrace));
    bzero(result,sizeof(fivmr_ThreadStackTrace));
    return result;
}

static void tst_init1(fivmr_ThreadStackTrace *tst,
		      fivmr_ThreadState *ts) {
    tst->thread=fivmr_newGlobalHandle(fivmr_ThreadState_get(ts->vm),ts->vm->javaThreads+ts->id);
}

static uintptr_t tst_init2_cback(fivmr_VM *vm,
                                 uintptr_t arg,
				 fivmr_MethodRec *mr,
				 int32_t lineNumber) {
    fivmr_ThreadStackTrace *tst;
    fivmr_StackTraceFrame *stf;
    
    tst=(fivmr_ThreadStackTrace*)arg;

    stf=(fivmr_StackTraceFrame*)fivmr_mallocAssert(sizeof(fivmr_StackTraceFrame));

    stf->mr=mr;
    stf->lineNumber=lineNumber;
    stf->next=tst->top;
    tst->top=stf;
    
    tst->depth++;
    
    return 0;
}

static void tst_init2(fivmr_ThreadStackTrace *tst,
		      fivmr_ThreadState *ts) {
    fivmr_StackTraceFrame *stf,*prev;
    
    fivmr_Lock_lock(&ts->lock);
    
    tst->depth=0;
    tst->top=NULL;
    
    fivmr_iterateDebugFrames(ts->vm,ts->curF,tst_init2_cback,(uintptr_t)tst);
    
    /* invert the stack trace */
    stf=tst->top;
    prev=NULL;
    while (stf!=NULL) {
	fivmr_StackTraceFrame *next=stf->next;
	stf->next=prev;
	prev=stf;
	stf=next;
    }
    tst->top=prev;
    
    tst->execStatus=ts->execStatus;
    tst->execFlags=ts->execFlags;
    
    fivmr_Lock_unlock(&ts->lock);
}

fivmr_ThreadStackTrace *fivmr_ThreadStackTrace_get(fivmr_ThreadState *ts) {
    fivmr_ThreadStackTrace *tst=alloc_tst();
    tst_init1(tst,ts);
    tst_init2(tst,ts);
    return tst;
}

/* can only be called by a thread that is IN_JAVA */
static void doTraceStackForHandshake(fivmr_ThreadState *ts) {
    fivmr_ThreadStackTrace *tst=fivmr_ThreadStackTrace_get(ts);
    tst->next = ts->vm->atst_result->first;
    ts->vm->atst_result->first = tst;
    ts->vm->atst_result->numThreads++;
}

void fivmr_Debug_traceStack(fivmr_ThreadState *ts) {
    LOG(2,("grabbing debug stack trace for Thread %u",ts->id));
    fivmr_assert((ts->execFlags&FIVMR_TSEF_TRACE_STACK)!=0);
    ts->execFlags&=~FIVMR_TSEF_TRACE_STACK;
    if (fivmr_ThreadState_isRunning(ts)) {
	if (fivmr_ThreadState_isInJava(ts)) {
	    doTraceStackForHandshake(ts);
	} else {
	    fivmr_ThreadState *curts=fivmr_ThreadState_get(ts->vm);
	    fivmr_ThreadState_goToJava(curts);
	    doTraceStackForHandshake(ts);
	    fivmr_ThreadState_goToNative(curts);
	}
    }
    if (LOGGING(2)) {
	fivmr_ThreadState_dumpStackFor(ts);
    }
}

fivmr_AllThreadStackTraces *fivmr_AllThreadStackTraces_get(fivmr_VM *vm) {
    fivmr_ThreadState *curts=fivmr_ThreadState_get(vm);
    fivmr_AllThreadStackTraces *result;
    
    fivmr_ThreadState_goToNative(curts);

    fivmr_Lock_lock(&vm->handshakeLock);
    
    vm->atst_result=(fivmr_AllThreadStackTraces*)
	fivmr_mallocAssert(sizeof(fivmr_AllThreadStackTraces));
    vm->atst_result->first=NULL;
    vm->atst_result->numThreads=0;
    
    fivmr_ThreadState_softHandshake(vm,
                                    FIVMR_TSEF_JAVA_HANDSHAKEABLE,
				    FIVMR_TSEF_TRACE_STACK);
    
    result=vm->atst_result;
    vm->atst_result=NULL;
    
    fivmr_Lock_unlock(&vm->handshakeLock);

    fivmr_ThreadState_goToJava(curts);
    
    return result;
}

void fivmr_ThreadStackTrace_free(fivmr_ThreadStackTrace *tst) {
    fivmr_StackTraceFrame *stf;
    for (stf=tst->top;stf!=NULL;) {
	fivmr_StackTraceFrame *next=stf->next;
	fivmr_free(stf);
	stf=next;
    }
    fivmr_deleteGlobalHandle(tst->thread);
    fivmr_free(tst);
}

void fivmr_AllThreadStackTraces_free(fivmr_AllThreadStackTraces *atst) {
    fivmr_ThreadStackTrace *tst;
    for (tst=atst->first;tst!=NULL;) {
	fivmr_ThreadStackTrace *next=tst->next;
	fivmr_ThreadStackTrace_free(tst);
	tst=next;
    }
    fivmr_free(atst);
}

void fivmr_Debug_dumpAllStacks(fivmr_VM *vm) {
    fivmr_ThreadState_softHandshake(vm,
                                    FIVMR_TSEF_JAVA_HANDSHAKEABLE,
				    FIVMR_TSEF_DUMP_STACK);
}

void fivmr_Debug_dumpStack(fivmr_ThreadState *ts) {
    fivmr_Lock_lock(&ts->vm->lock);
    fivmr_ThreadState_dumpStackFor(ts);
    ts->execFlags&=~FIVMR_TSEF_DUMP_STACK;
    fivmr_Lock_unlock(&ts->vm->lock);
}

fivmr_DebugRec *fivmr_DebugRec_withRootSize(fivmr_DebugRec *dr,
                                            void *region,
                                            int32_t rootSize) {
    uintptr_t ln_rm_c;
    fivmr_DebugRec *result;
    
    fivmr_assert(rootSize>=0);
    
    ln_rm_c=dr->ln_rm_c;
    
    if ((ln_rm_c&1)) {
        fivmr_FatDebugData *fdd;
        int32_t i;
        int32_t lastSetBit=-1;
        
        fdd=(fivmr_FatDebugData*)(ln_rm_c&~3);
        
        for (i=fdd->refMapSize*32;i-->0;) {
            if (fivmr_BitVec_get((uint32_t*)fdd->refMap,i)) {
                lastSetBit=i;
                break;
            }
        }
        
        if (lastSetBit<rootSize) {
            result=dr;
        } else {
            bool canCompress;
            
            lastSetBit=-1;
            
            for (i=rootSize;i-->0;) {
                if (fivmr_BitVec_get((uint32_t*)fdd->refMap,i)) {
                    lastSetBit=i;
                    break;
                }
            }
            
            if (sizeof(void*)==4) {
                canCompress=
                    fdd->lineNumber>=0 && fdd->lineNumber < (1<<10) &&
                    fdd->bytecodePC>=0 && fdd->bytecodePC < (1<<8) &&
                    lastSetBit < (1<<12);
            } else {
                canCompress=
                    fdd->lineNumber>=0 && fdd->lineNumber < (1<<16) &&
                    fdd->bytecodePC>=0 && fdd->bytecodePC < (1<<5) &&
                    lastSetBit < (1<<31);
            }
            
            result=freg_region_alloc(region,sizeof(fivmr_DebugRec));
            fivmr_assert(result!=NULL);
            
            result->method=dr->method;
            
            if (canCompress) {
#if FIVMSYS_PTRSIZE==4
                result->ln_rm_c=
                    (ln_rm_c&2) |
                    (((uintptr_t)(fdd->refMap[0]&((1<<12)-1)))<<2) |
                    (((uintptr_t)fdd->lineNumber)<<14) |
                    (((uintptr_t)fdd->bytecodePC)<<24);
#else
                result->ln_rm_c=
                    (ln_rm_c&2) |
                    (((uintptr_t)(fdd->refMap[0]&((((uintptr_t)1)<<31)-1)))<<2) |
                    (((uintptr_t)fdd->lineNumber)<<33) |
                    (((uintptr_t)fdd->bytecodePC)<<49);
#endif
            } else {
                fivmr_FatDebugData *newFDD;
                
                newFDD=freg_region_alloc(
                    region,
                    sizeof(fivmr_FatDebugData)-sizeof(int32_t)+
                    sizeof(int32_t)*((lastSetBit+32)/32));
                fivmr_assert(newFDD!=NULL);
                
                newFDD->lineNumber=fdd->lineNumber;
                newFDD->bytecodePC=fdd->bytecodePC;
                newFDD->refMapSize=fdd->refMapSize;
                
                bzero(newFDD->refMap,sizeof(int32_t)*((lastSetBit+32)/32));
                
                for (i=0;i<=lastSetBit;++i) {
                    fivmr_BitVec_set(
                        (uint32_t*)newFDD->refMap,
                        i,
                        fivmr_BitVec_get(
                            (uint32_t*)fdd->refMap,
                            i));
                }
                
                result->ln_rm_c=
                    (ln_rm_c&3) |
                    ((uintptr_t)newFDD);
            }
        }
    } else {
        /* thin form */
        int32_t i;
        int32_t lastSetBit=-1;
        
        /* NOTE: rootSize may be greater than DR_TM_NUMBITS ... that means that
           the root set size that we started with was greater than what could be
           expressed in thin mode *but* the last reference is below the thin
           threshold. */
        
        for (i=FIVMR_DR_TRM_NUMBITS;i-->0;) {
            if ((ln_rm_c&(((uintptr_t)1)<<(2+i)))) {
                lastSetBit=i;
                break;
            }
        }
        
        if (lastSetBit<rootSize) {
            result=dr;
        } else {
            result=freg_region_alloc(region,sizeof(fivmr_DebugRec));
            fivmr_assert(result!=NULL);
            *result=*dr;

            /* this loop is crappy. */
            for (i=rootSize;i<=lastSetBit;++i) {
                ln_rm_c&=~(((uintptr_t)1)<<(2+i));
            }
            
            result->ln_rm_c=ln_rm_c;
        }
    }
    return result;
}

# 1 "fivmr_fieldrec.c"
/*
 * fivmr_fieldrec.c
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

const char *fivmr_FieldRec_describe(fivmr_FieldRec *fr) {
    if (fr==NULL) {
	return "(null fr)";
    } else {
	size_t size=1; /* for null char */
	char *result;
	size+=strlen(fr->owner->name);
	size++; /* slash */
	size+=strlen(fr->name);
	size++; /* slash */
	size+=strlen(fr->type->name);
	result=fivmr_threadStringBuf(size);
	strcpy(result,fr->owner->name);
	strcat(result,"/");
	strcat(result,fr->name);
	strcat(result,"/");
	strcat(result,fr->type->name);
	return result;
    }
}


# 1 "fivmr_flowlog.c"
/*
 * fivmr_flowlog.c
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

#include "fivmr.h"

#if FIVMR_FLOW_LOGGING

/* The platform init function should set this to TRUE if logging is enabled */
int fivmr_flowLogEnabled;

static fivmr_FlowLogBuffer *fbhead;
static fivmr_FlowLogBuffer *fbtail;

static fivmr_Semaphore sema;
static int should_die;

static void flowLogThreadMain(void *arg);

void fivmr_FlowLog_init(void) {
    fivmr_flowLogEnabled = FALSE;
    fbhead = fbtail = NULL;
    should_die = FALSE;

    fivmr_FlowLog_platform_init();

    /* FIXME: Priorities? */
    fivmr_Semaphore_init(&sema);

    /* It's unclear to me where this thread should come from; possibly
     * the VM, possibly not -- I'm thinking it belongs to the main
     * process, not a VM, so we're fivmr_Thread_spawning here. */
    fivmr_Thread_spawn(flowLogThreadMain, NULL, FIVMR_TPR_NORMAL_MIN);
}

void fivmr_FlowLog_finalize(void) {
    fivmr_FlowLog_lock();
    should_die = 1;
    fivmr_Semaphore_up(&sema);
    fivmr_FlowLog_unlock();
    fivmr_FlowLog_wait();
    fivmr_FlowLog_platform_finalize();
}

void fivmr_FlowLog_release(fivmr_FlowLogBuffer *flowbuf) {
    flowbuf->next = NULL;
    if (!fivmr_flowLogEnabled) {
        fivmr_free(flowbuf);
        return;
    }
    fivmr_FlowLog_lock();
    if (should_die) {
        /* This is unnecessary in some cases, because the mutex has been
         * destroyed.  The whole shutdown sequence is ugly. */
        fivmr_FlowLog_unlock();
        fivmr_free(flowbuf);
        return;
    }
    if (!fbhead) {
        fbhead = fbtail = flowbuf;
    } else {
        fbtail->next = flowbuf;
        fbtail = flowbuf;
    }
    fivmr_Semaphore_up(&sema);
    fivmr_FlowLog_unlock();
}

void fivmr_FlowLog_releaseTS(fivmr_ThreadState *ts) {
    fivmr_assert(ts->flowbuf);
    fivmr_FlowLog_release(ts->flowbuf);
    ts->flowbuf = fivmr_malloc(FIVMR_FLOWLOG_BUFFERSIZE);
    ts->flowbuf->entries = 0;
}

/* FIXME: Final flush and shutdown */
static void flowLogThreadMain(void *arg) {
    fivmr_FlowLogBuffer *flowbuf;
    fivmr_FlowLog_wait();
    for (;;) {
        fivmr_Semaphore_down(&sema);
        fivmr_FlowLog_lock();

        fivmr_assert(fbhead != NULL || should_die);

        if (fbhead == NULL && should_die) {
            fivmr_FlowLog_unlock();
            fivmr_FlowLog_notify();
            fivmr_Thread_exit();
        }

        flowbuf = fbhead;
        if (fbhead->next == NULL) {
            fbhead = fbtail = NULL;
        } else {
            fbhead = fbhead->next;
        }
        fivmr_FlowLog_unlock();

        fivmr_FlowLog_platform_write(flowbuf);
        fivmr_free(flowbuf);
    }
}

#endif /* FIVMR_FLOW_LOGGING */
# 1 "fivmr_gatehelpers.c"
/*
 * fivmr_gatehelpers.c
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

fivmr_Handle *fivmr_GateHelpers_installObjectFieldReference(fivmr_ThreadState *ts,
                                                            fivmr_Object referent,
                                                            uintptr_t fieldOffset,
                                                            fivmr_TypeData *td) {
    fivmr_Object result;
    fivmr_Handle *h;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_alloc(ts,FIVMR_GC_OBJ_SPACE,td);
    if (result) {
        h=fivmr_ThreadState_addHandle(ts,result);
        fivmr_objectPutField(ts,referent,fieldOffset,result,
                             0 /* FIXME currently only works
                                  for default flags */);
    } else {
        ts->curException=0;
        h=NULL;
    }
    fivmr_ThreadState_goToNative(ts);
    return h;
}

fivmr_Handle *fivmr_GateHelpers_installArrayFieldReference(fivmr_ThreadState *ts,
                                                           fivmr_Object referent,
                                                           uintptr_t fieldOffset,
                                                           fivmr_TypeData *td,
                                                           int32_t length) {
    fivmr_Object result;
    fivmr_Handle *h;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_allocArray(ts,FIVMR_GC_OBJ_SPACE,td,length);
    if (result) {
        h=fivmr_ThreadState_addHandle(ts,result);
        fivmr_objectPutField(ts,referent,fieldOffset,result,
                             0 /* FIXME currently only works
                                  for default flags */);
    } else {
        ts->curException=0;
        h=NULL;
    }
    fivmr_ThreadState_goToNative(ts);
    return h;
}

fivmr_Handle *fivmr_GateHelpers_installObjectElementReference(fivmr_ThreadState *ts,
                                                              fivmr_Object referent,
                                                              int32_t index,
                                                              fivmr_TypeData *td) {
    fivmr_Object result;
    fivmr_Handle *h;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_alloc(ts,FIVMR_GC_OBJ_SPACE,td);
    if (result) {
        h=fivmr_ThreadState_addHandle(ts,result);
        fivmr_objectArrayStore(ts,referent,index,result,
                               0 /* FIXME currently only works
                                    for default flags */);
    } else {
        ts->curException=0;
        h=NULL;
    }
    fivmr_ThreadState_goToNative(ts);
    return h;
}

fivmr_Handle *fivmr_GateHelpers_installArrayElementReference(fivmr_ThreadState *ts,
                                                             fivmr_Object referent,
                                                             int32_t index,
                                                             fivmr_TypeData *td,
                                                             int32_t length) {
    fivmr_Object result;
    fivmr_Handle *h;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_allocArray(ts,FIVMR_GC_OBJ_SPACE,td,length);
    if (result) {
        h=fivmr_ThreadState_addHandle(ts,result);
        fivmr_objectArrayStore(ts,referent,index,result,
                               0 /* FIXME currently only works
                                    for default flags */);
    } else {
        ts->curException=0;
        h=NULL;
    }
    fivmr_ThreadState_goToNative(ts);
    return h;
}

# 1 "fivmr_gc.c"
/*
 * fivmr_gc.c
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

#include "fivmr.h"

#define IMPORT_FUNCS(prefix)                                            \
    void prefix##_init(fivmr_GC *gc);                                   \
    void prefix##_resetStats(fivmr_GC *gc);                             \
    void prefix##_report(fivmr_GC *gc,const char *name);                \
    void prefix##_clear(fivmr_ThreadState *ts);                         \
    void prefix##_startThread(fivmr_ThreadState *ts);                   \
    void prefix##_commitThread(fivmr_ThreadState *ts);                  \
    void prefix##_handleHandshake(fivmr_ThreadState *ts);               \
    int64_t prefix##_numIterationsCompleted(fivmr_GC *gc);              \
    void prefix##_markSlow(fivmr_ThreadState *ts,                       \
                           fivmr_Object obj);                           \
    fivmr_Object prefix##_allocRawSlow(fivmr_ThreadState *ts,           \
                                       fivmr_GCSpace space,             \
                                       uintptr_t size,                  \
                                       uintptr_t alignStart,            \
                                       uintptr_t align,                 \
                                       fivmr_AllocEffort effort,        \
                                       const char *description);        \
    fivmr_Spine prefix##_allocSSSlow(fivmr_ThreadState *ts,             \
                                     uintptr_t spineLength,             \
                                     int32_t numEle,                    \
                                     const char *description);          \
    void prefix##_claimMachineCode(fivmr_ThreadState *ts,               \
                                   fivmr_MachineCode *mc);              \
    int64_t prefix##_freeMemory(fivmr_GC *gc);                          \
    int64_t prefix##_totalMemory(fivmr_GC *gc);                         \
    int64_t prefix##_maxMemory(fivmr_GC *gc);                           \
    void prefix##_asyncCollect(fivmr_GC *gc);                           \
    void prefix##_collectFromJava(fivmr_GC *gc,                         \
                                  const char *descrIn,                  \
                                  const char *descrWhat);               \
    void prefix##_collectFromNative(fivmr_GC *gc,                       \
                                    const char *descrIn,                \
                                    const char *descrWhat);             \
    void prefix##_setPriority(fivmr_GC *gc,                             \
                              fivmr_ThreadPriority prio);               \
    bool prefix##_setMaxHeap(fivmr_GC *gc,                              \
                             int64_t bytes);                            \
    bool prefix##_setTrigger(fivmr_GC *gc,                              \
                             int64_t bytes);                            \
    bool prefix##_getNextDestructor(fivmr_GC *gc,                       \
                                    fivmr_Handle *objCell,              \
                                    bool wait);                         \
    void prefix##_signalExit(fivmr_GC *gc);                             \
    void prefix##_shutdown(fivmr_GC *gc)

#if FIVMBUILD_SPECIAL_GC
#  if FIVMBUILD__NOGC
IMPORT_FUNCS(fivmr_NOGC);
#  elif FIVMBUILD__CMRGC || FIVMBUILD__HFGC
IMPORT_FUNCS(fivmr_SpecialGC);
#  else
#    error "bad GC"
#  endif
#else
IMPORT_FUNCS(fivmr_NOGC);
IMPORT_FUNCS(fivmr_OptCMRGC);
IMPORT_FUNCS(fivmr_OptCMRGCSMM);
IMPORT_FUNCS(fivmr_OptHFGC);
IMPORT_FUNCS(fivmr_GenericGC);
#endif

#define SET_FUNC_PTRS(gc,prefix) do {                                   \
        (gc)->resetStats             = prefix##_resetStats;             \
        (gc)->report                 = prefix##_report;                 \
        (gc)->clear                  = prefix##_clear;                  \
        (gc)->startThread            = prefix##_startThread;            \
        (gc)->commitThread           = prefix##_commitThread;           \
        (gc)->handleHandshake        = prefix##_handleHandshake;        \
        (gc)->numIterationsCompleted = prefix##_numIterationsCompleted; \
        (gc)->markSlow               = prefix##_markSlow;               \
        (gc)->allocRawSlow           = prefix##_allocRawSlow;           \
        (gc)->allocSSSlow            = prefix##_allocSSSlow;            \
        (gc)->claimMachineCode       = prefix##_claimMachineCode;       \
        (gc)->freeMemory             = prefix##_freeMemory;             \
        (gc)->totalMemory            = prefix##_totalMemory;            \
        (gc)->maxMemory              = prefix##_maxMemory;              \
        (gc)->asyncCollect           = prefix##_asyncCollect;           \
        (gc)->collectFromJava        = prefix##_collectFromJava;        \
        (gc)->collectFromNative      = prefix##_collectFromNative;      \
        (gc)->setPriority            = prefix##_setPriority;            \
        (gc)->setMaxHeap             = prefix##_setMaxHeap;             \
        (gc)->setTrigger             = prefix##_setTrigger;             \
        (gc)->getNextDestructor      = prefix##_getNextDestructor;      \
        (gc)->signalExit             = prefix##_signalExit;             \
        (gc)->shutdown               = prefix##_shutdown;               \
    } while (false)


void fivmr_GC_resetSettings(fivmr_GC *gc) {
    fivmr_VM *vm=fivmr_VMfromGC(gc);
    bzero(gc,sizeof(fivmr_GC));
    gc->gcTriggerPages=vm->config.gcDefTrigger>>FIVMSYS_LOG_PAGE_SIZE;
    gc->maxPagesUsed=vm->config.gcDefMaxMem>>FIVMSYS_LOG_PAGE_SIZE;
    gc->immortalMem=fivmr_VMfromGC(gc)->config.gcDefImmortalMem;
    gc->threadPriority=fivmr_ThreadPriority_min(vm->maxPriority,
                                                vm->config.gcDefPriority);
    gc->lastStart=fivmr_curTime();
    gc->lastEnd=fivmr_curTime();
    LOG(2,("configured GC priority: %d",fivmr_VMfromGC(gc)->config.gcDefPriority));
    LOG(2,("VM max priority: %d",fivmr_VMfromGC(gc)->maxPriority));
    LOG(2,("resuling GC priority: %d",gc->threadPriority));
}

void fivmr_GC_registerPayload(fivmr_GC *gc) {
    fivmr_VM *vm=fivmr_VMfromGC(gc);
    gc->logGC=FIVMR_DEF_LOG_GC(&vm->settings);
    gc->logSyncGC=FIVMR_DEF_LOG_SYNC_GC(&vm->settings);
    LOG(1,("GC logging: %s",gc->logGC?"ACTIVATED":"deactivated"));
    LOG(1,("Sync GC logging: %s",gc->logSyncGC?"ACTIVATED":"deactivated"));
}

void fivmr_GC_init(fivmr_GC *gc) {
#if FIVMBUILD_SPECIAL_GC /* are we specializing the runtime for one GC? */
#  if FIVMBUILD__NOGC
    /* we're specializing for the no GC case */
    SET_FUNC_PTRS(gc,fivmr_NOGC);
    fivmr_NOGC_init(gc); 
#  elif FIVMBUILD__CMRGC || FIVMBUILD__HFGC
    /* we're specializing for either CMRGC or HFGC; it would have been compiled
       with the prefix 'SpecialGC' */
    SET_FUNC_PTRS(gc,fivmr_SpecialGC);
    fivmr_SpecialGC_init(gc);
#  else
#    error "bad GC"
#  endif
#else /* using generic runtime, but pick the best GC for the settings */
    fivmr_VM *vm=fivmr_VMfromGC(gc);
    
    if (FIVMR_NOGC(&vm->settings)) {
        /* settings match no GC; just drop that in and we're done. */
        SET_FUNC_PTRS(gc,fivmr_NOGC);
        fivmr_NOGC_init(gc);
    } else if (FIVMR_CMRGC(&vm->settings) &&
               FIVMR_HM_NARROW(&vm->settings) &&
               FIVMR_OM_CONTIGUOUS(&vm->settings) &&
               !FIVMR_FORCE_ARRAYLETS(&vm->settings) &&
               !FIVMR_GC_BLACK_STACK(&vm->settings) &&
               !FIVMR_GC_DEBUG(&vm->settings)) {
        /* settings match standard optimized CMRGC, use that. */
        if (FIVMR_SELF_MANAGE_MEM(&vm->settings)) {
            SET_FUNC_PTRS(gc,fivmr_OptCMRGCSMM);
            fivmr_OptCMRGCSMM_init(gc);
        } else {
            SET_FUNC_PTRS(gc,fivmr_OptCMRGC);
            fivmr_OptCMRGC_init(gc);
        }
    } else if (FIVMR_HFGC(&vm->settings) &&
               FIVMR_HM_NARROW(&vm->settings) &&
               FIVMR_OM_FRAGMENTED(&vm->settings) &&
               !FIVMR_FORCE_ARRAYLETS(&vm->settings) &&
               !FIVMR_GC_BLACK_STACK(&vm->settings) &&
               !FIVMR_GC_DEBUG(&vm->settings) &&
               FIVMR_SELF_MANAGE_MEM(&vm->settings)) {
        /* settings match standard optimized HFGC, use that. */
        SET_FUNC_PTRS(gc,fivmr_OptHFGC);
        fivmr_OptHFGC_init(gc);
    } else if (FIVMR_CMRGC(&vm->settings) ||
               FIVMR_HFGC(&vm->settings)) {
        /* settings match either CMRGC or HFGC but not one that we've specialized;
           use the generic one that supports both (performance will not be
           amazingtacularfantastic, but oh well). */
        SET_FUNC_PTRS(gc,fivmr_GenericGC);
        fivmr_GenericGC_init(gc);
    } else {
        fivmr_assert(!"bad GC");
    }
#endif
}

# 1 "fivmr_handleregion.c"
/*
 * fivmr_handleregion.c
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

#include "fivmr.h"

fivmr_Handle *fivmr_HandleRegion_add(fivmr_HandleRegion *hr,
				     fivmr_ThreadState *ts,
				     fivmr_Handle **freelist,
				     fivmr_Object obj) {
    uintptr_t execStatus=ts->execStatus;
    fivmr_assert(execStatus==FIVMR_TSES_IN_JAVA ||
		 execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK);
    if (obj==0) {
	return NULL;
    } else {
	fivmr_Handle *h=NULL;
	int oldErrno=errno;
	
	if (freelist!=NULL) {
	    h=*freelist;
	}
	if (h==NULL) {
	    fivmr_Lock_lock(&ts->vm->hrLock);
	    h=ts->vm->freeHandles;
	    if (h==NULL) {
		fivmr_Lock_unlock(&ts->vm->hrLock);
		h=fivmr_mallocAssert(sizeof(fivmr_Handle));
	    } else {
		ts->vm->freeHandles=h->next;
		fivmr_Lock_unlock(&ts->vm->hrLock);
	    }
	} else {
	    *freelist=h->next;
	}
	
        fivmr_Handle_set(h,ts,obj);

	h->prev=&hr->head;
	h->next=hr->head.next;
	hr->head.next->prev=h;
	hr->head.next=h;
	
	errno=oldErrno;
	return h;
    }
}

void fivmr_Handle_remove(fivmr_Handle **freelist,
			 fivmr_Handle *h) {
    if (h!=NULL) {
	h->prev->next=h->next;
	h->next->prev=h->prev;
	h->obj=0;
	h->prev=NULL;
	h->next=*freelist;
	*freelist=h;
    }
}

void fivmr_HandleRegion_removeAll(fivmr_HandleRegion *hr,
				  fivmr_Handle **freelist) {
    while (hr->head.next!=&hr->tail) {
	fivmr_Handle_remove(freelist,hr->head.next);
    }
}


# 1 "fivmr_jni.c"
/*
 * fivmr_jni.c
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

#define FIVMR_IN_JNI_MODULE 1

#include "fivmr_jni.h"
#include "fivmr.h"
#include <stdarg.h>

/* NOTE: we're currently doing type checks on arrays and strings, but not
   on jclass.  Seems like a reasonable judgment call (we access jclass way
   more often, so there is a performance argument, and it just feels like
   programmer errors there are less likely given that jclass is rarely
   used as jobject).  But, I should check what the reference implementation
   (i.e. HotSpot) does. */

/* helper for cases where you're IN_NATIVE, you know that the object is non-moving
   and you don't *really* need a handle to it, but JNI requires that we produce
   a handle anyway. */
static fivmr_Handle *produceHandleFromNative(fivmr_ThreadState *ts,
					     fivmr_Object obj) {
    fivmr_Handle *result;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_ThreadState_addHandle(ts,obj);
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static void transferArgVal(fivmr_TypeStub *td,
			   jvalue *arg,
			   va_list lst) {
    /* FIXME: these '#if 1' thingies need to be turned into proper checks against
       something tested by autoconf.  the way this code currently works (i.e. ignoring
       the else cases) should be correct on most C compilers and calling conventions;
       it certainly is for all of the ones I'm familiar with, but that's not to say
       that there won't be some goofy platform where passing a float through a vararg
       preserves the fact that it's a float rather than coercing it to double as is
       done on cdecl. */
    switch (td->name[0]) {
    case 'Z':
#if 1
	arg->z=va_arg(lst,int);
#else
	arg->z=va_arg(lst,jboolean);
#endif
	break;
    case 'B':
#if 1
	arg->b=va_arg(lst,int);
#else
	arg->b=va_arg(lst,jbyte);
#endif
	break;
    case 'C':
#if 1
	arg->c=va_arg(lst,int);
#else
	arg->c=va_arg(lst,jchar);
#endif
	break;
    case 'S':
#if 1
	arg->s=va_arg(lst,int);
#else
	arg->s=va_arg(lst,jshort);
#endif
	break;
    case 'I':
	arg->i=va_arg(lst,jint);
	break;
    case 'J':
	arg->j=va_arg(lst,jlong);
	break;
    case 'F':
#if 1
	arg->f=va_arg(lst,double);
#else
	arg->f=va_arg(lst,jfloat);
#endif
	break;
    case 'D':
	arg->d=va_arg(lst,jdouble);
	break;
    case 'L':
    case '[':
	arg->l=va_arg(lst,jobject);
	break;
    default: fivmr_assert(!"bad type descriptor");
    }
}

static void transferArgVals(fivmr_MethodRec *mr,
			    jvalue *args,
			    va_list lst) {
    int32_t i;
    for (i=0;i<mr->nparams;++i) {
	transferArgVal(mr->params[i],args+i,lst);
    }
}

static bool check_init(fivmr_ThreadState *ts,
                       fivmr_TypeData *td) {
    LOG(12,("check_init called on %p",td));
    LOG(11,("check_init called on %p, which refers to %s",td,td->name));
    fivmr_ReflectLog_use(ts,-1,td);
    if (fivmr_TypeData_resolve(td)) {
        if (fivmr_TypeData_checkInit(ts,td)) {
            return true;
        }
    } else {
        char buf[256];
        snprintf(buf,sizeof(buf),
                 "Could not link and resolve %s",
                 td->name);
        fivmr_throwLinkageError_inJava(ts,buf);
    }
    fivmr_ThreadState_handlifyException(ts);
    return false;
}

static bool check_init_inNative(fivmr_ThreadState *ts,
                                fivmr_TypeData *td) {
    bool result;
    if (td->inited==1) {
        result=true;
    } else {
        fivmr_ThreadState_goToJava(ts);
        result=check_init(ts,td);
        fivmr_ThreadState_goToNative(ts);
    }
    return result;
}

static bool check_init_easy(JNIEnv *env,
			    jclass clazz) {
    bool result;
    fivmr_ThreadState_goToJava(((fivmr_JNIEnv*)env)->ts);
    result=check_init(((fivmr_JNIEnv*)env)->ts,
                      fivmr_TypeData_fromClass(((fivmr_JNIEnv*)env)->ts,
                                               fivmr_Handle_get((fivmr_Handle*)clazz)));
    fivmr_ThreadState_goToNative(((fivmr_JNIEnv*)env)->ts);
    return result;
}

static fivmr_Value virtual_call(jmethodID methodID,
				JNIEnv *env,
				jobject receiver,
				jvalue *args) {
    LOG(8,("virtual call"));
    return fivmr_MethodRec_call(
	(fivmr_MethodRec*)methodID,
	((fivmr_JNIEnv*)env)->ts,
	NULL,
	(void*)receiver,
	(fivmr_Value*)args,
	FIVMR_CM_HANDLES|FIVMR_CM_EXEC_STATUS|FIVMR_CM_DISPATCH|
	FIVMR_CM_NULLCHECK|FIVMR_CM_CLASSCHANGE);
}

static fivmr_Value special_call(jmethodID methodID,
				JNIEnv *env,
				jclass clazz,
				jobject receiver,
				jvalue *args) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Value result;
    LOG(8,("special call"));
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_MethodRec_call(
	(fivmr_MethodRec*)methodID,
	ts,
	fivmr_MethodRec_staticDispatch(
            ts,
	    (fivmr_MethodRec*)methodID,
	    fivmr_TypeData_fromClass(((fivmr_JNIEnv*)env)->ts,
                                     fivmr_Handle_get((fivmr_Handle*)clazz))),
	(void*)receiver,
	(fivmr_Value*)args,
	FIVMR_CM_HANDLES|FIVMR_CM_NULLCHECK|
	FIVMR_CM_CLASSCHANGE);
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static fivmr_Value static_call(jmethodID methodID,
			       JNIEnv *env,
			       jvalue *args) {
    LOG(8,("static call"));
    return fivmr_MethodRec_call(
	(fivmr_MethodRec*)methodID,
	((fivmr_JNIEnv*)env)->ts,
	NULL,
	NULL,
	(fivmr_Value*)args,
	FIVMR_CM_HANDLES|FIVMR_CM_EXEC_STATUS|FIVMR_CM_DISPATCH|
	FIVMR_CM_CHECKINIT);
}

static fivmr_Value virtual_call_v(jmethodID methodID,
				  JNIEnv *env,
				  jobject receiver,
				  va_list lst) {
    jvalue *args;
    size_t size=sizeof(jvalue)*fivmr_MethodRec_numParams((fivmr_MethodRec*)methodID);
    args=alloca(size);
    bzero(args,size);
    transferArgVals((fivmr_MethodRec*)methodID,args,lst);
    return virtual_call(methodID,env,receiver,args);
}

static fivmr_Value special_call_v(jmethodID methodID,
				  JNIEnv *env,
				  jclass clazz,
				  jobject receiver,
				  va_list lst) {
    jvalue *args;
    args=alloca(sizeof(jvalue)*
		fivmr_MethodRec_numParams((fivmr_MethodRec*)methodID));
    transferArgVals((fivmr_MethodRec*)methodID,args,lst);
    return special_call(methodID,env,clazz,receiver,args);
}

static fivmr_Value static_call_v(jmethodID methodID,
				 JNIEnv *env,
				 va_list lst) {
    jvalue *args;
    args=alloca(sizeof(jvalue)*
		fivmr_MethodRec_numParams((fivmr_MethodRec*)methodID));
    transferArgVals((fivmr_MethodRec*)methodID,args,lst);
    return static_call(methodID,env,args);
}

static fivmr_Handle *alloc_array(JNIEnv *env,
				 fivmr_TypeData *td,
				 jsize length) {
    fivmr_ThreadState *ts;
    fivmr_Handle *result;
    ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ReflectLog_alloc(ts,-1,td);
    result=NULL;
    fivmr_ThreadState_goToJava(ts);
    if (length<0) {
	fivmr_throwNegativeSizeRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	result=fivmr_ThreadState_addHandle(
	    ts,
	    fivmr_allocArray(ts,FIVMR_GC_OBJ_SPACE,td,length));
    }
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static jint jni_DestroyJavaVM(JavaVM *vm) {
    return -1;
}

static jint attachCurrentThread(JavaVM *vm_,
				JNIEnv **pEnv,
				void *threadArgs,
				bool isDaemon) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    
    ctx=((fivmr_JNIVM*)vm_)->ctx;
    vm=ctx->vm;
    
    ts=fivmr_ThreadState_getNullable(vm);
    if (ts!=NULL) {
	/* already attached */
	*pEnv=(JNIEnv*)&ts->curNF->jni;
	return 0;
    } else {
	/* not attached - create new thread */
	fivmr_Handle *vmt;
	
	ts=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
        if (ts!=NULL) {
            fivmr_ThreadState_setBasePrio(
                ts,
                fivmr_Thread_getPriority(
                    fivmr_ThreadHandle_current()));
            /* total hack; we don't actually know what the stack height is... */
            fivmr_ThreadState_setStackHeight(
                ts,
                fivmr_Thread_stackHeight()/2-FIVMR_STACK_HEIGHT_HEADROOM);
            fivmr_ThreadState_set(ts,ctx);
            vmt=fivmr_VMThread_create(ts,0,isDaemon);
            fivmr_assert(vmt!=NULL);
            fivmr_assert(ts->curExceptionHandle==NULL);
            if (fivmr_ThreadState_glue(ts,vmt)) {
                
                *pEnv=(JNIEnv*)&ts->curNF->jni;
                return 0;
            }
        }
        return -1; /* ?? */
    }
}

static jint jni_AttachCurrentThread(JavaVM *vm,
				    JNIEnv **pEnv,
				    void *threadArgs) {
    return attachCurrentThread(vm,pEnv,threadArgs,false);
}

static jint jni_AttachCurrentThreadAsDaemon(JavaVM *vm,
					    JNIEnv **pEnv,
					    void *threadArgs) {
    return attachCurrentThread(vm,pEnv,threadArgs,true);
}

static jint jni_DetachCurrentThread(JavaVM *vm_) {
    fivmr_VM *vm;
    fivmr_ThreadPriority prio;
    
    vm=((fivmr_JNIVM*)vm_)->ctx->vm;

    prio=fivmr_ThreadState_terminate(fivmr_ThreadState_get(vm));
    
    fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                             prio);
    return 0;
}

static jint jni_GetEnv(JavaVM *vm_,JNIEnv **penv,jint version) {
    /* FIXME: do something with the version. */
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    vm=((fivmr_JNIVM*)vm_)->ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    if (ts!=NULL) {
	*penv=(JNIEnv*)&ts->curNF->jni;
	return 0;
    } else {
	return -2;
    }
}

static struct JNIInvokeInterface jni_invoke_functions={
    NULL,
    NULL,
    NULL,
    jni_DestroyJavaVM,
    jni_AttachCurrentThread,
    jni_DetachCurrentThread,
    jni_GetEnv,
    jni_AttachCurrentThreadAsDaemon
};

jint JNI_GetDefaultJavaVMInitArgs(void *args) {
    return -1;
}

jint JNI_CreateJavaVM(JavaVM **pvm,void **penv,void *args) {
    return -1;
}

jint JNI_GetCreatedJavaVMs(JavaVM **_1,jsize _2,jsize *_3) {
    return -1;
}

static jint jni_GetVersion(JNIEnv *env_) {
    return JNI_VERSION_1_4;
}

static jclass jni_DefineClass(JNIEnv *env_,
			      jobject loader,
			      const jbyte *buf,
			      jsize bufLen) {
    fivmr_throwInternalError(
        ((fivmr_JNIEnv*)env_)->ts,
        "JNI DefineClass operation is not (yet) supported");
    return NULL;
}

static jclass jni_FindClass(JNIEnv *env_,const char *name) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_TypeData *td;
    fivmr_Handle *result;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState_goToJava(env->ts);
    fivmr_assert(env->ts->curNF!=NULL);
    ctx=env->ctx;
    fivmr_assert(ctx!=NULL);
    td=fivmr_TypeContext_findClass(ctx,name,'/');
    if (td==NULL) {
	LOG(3,("Could not find class %s",name));
	fivmr_throwNoClassDefFoundError_inJava(
            env->ts,name,"in a JNI FindClass call from native code");
	result=NULL;
    } else {
	fivmr_ReflectLog_use(env->ts,-1,td);
	result=fivmr_ThreadState_addHandle(env->ts,
                                           td->classObject);
	LOG(6,("jni_FindClass for %s returning %p (td = %s).",name,(void*)result,td->name));
    }
    fivmr_ThreadState_goToNative(env->ts);
    return (jclass)result;
}

static jclass jni_GetSuperclass(JNIEnv *env_,jclass clazz) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_TypeData *td;
    td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
    if (td->parent==env->ts->vm->payload->td_top /* FIXME */) {
	return NULL;
    } else {
	return (jclass)produceHandleFromNative(env->ts,td->parent->classObject);
    }
}

static jboolean jni_IsAssignableFrom(JNIEnv *env_,
				     jclass clazz1,
				     jclass clazz2) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    /* return true if clazz2 is a subtype of clazz1 */
    fivmr_TypeData *td1=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz1);
    fivmr_TypeData *td2=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz2);
    jboolean result;
    switch (td1->name[0]) {
    case 'S':
	switch (td2->name[0]) {
	case 'B': return true;
	default: break;
	}
	break;
    case 'I':
	switch (td2->name[0]) {
	case 'S':
	case 'B':
	case 'C': return true;
	default: break;
	}
	break;
    case 'L':
	switch (td2->name[0]) {
	case 'I':
	case 'S':
	case 'B':
	case 'C': return true;
	default: break;
	}
	break;
    case 'F':
	switch (td2->name[0]) {
	case 'L':
	case 'I':
	case 'S':
	case 'B':
	case 'C': return true;
	default: break;
	}
	break;
    case 'D':
	switch (td2->name[0]) {
	case 'F':
	case 'L':
	case 'I':
	case 'S':
	case 'B':
	case 'C': return true;
	default: break;
	}
	break;
    default: break;
    }
    fivmr_ThreadState_goToJava(env->ts);
    result=(jboolean)fivmr_TypeData_isSubtypeOf(env->ts,td2,td1);
    fivmr_ThreadState_goToNative(env->ts);
    return result;
}

static jint jni_Throw(JNIEnv *env_,
		      jthrowable obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    int logAtLevel;
    
    if (env->ts->vm->exceptionsFatalReason!=NULL || fivmr_abortThrow) {
        const char *reason;
        reason=env->ts->vm->exceptionsFatalReason;
        if (reason==NULL) {
            reason="Unexpected exception (fivmr_abortThrow=true)";
        }
	fivmr_Log_lockedPrintf("Thread %u: %s: %s (%p)\n",
			       env->ts->id,
			       reason,
			       fivmr_TypeData_forObject(&env->ts->vm->settings,
                                                        ((fivmr_Handle*)obj)->obj)->name,
			       obj);
	fivmr_ThreadState_dumpStackFor(env->ts);
	fivmr_abortf("%s.",reason);
    }
    
    if ((env->ts->vm->flags&FIVMR_VMF_LOG_THROW)) {
        logAtLevel=0;
    } else {
        logAtLevel=3;
    }

    if (LOGGING(logAtLevel)) {
	LOG(logAtLevel,
            ("Thread %u: throwing exception %p with type %s (from native code)",
             env->ts->id,obj,fivmr_TypeData_forObject(&env->ts->vm->settings,
                                                      ((fivmr_Handle*)obj)->obj)->name));
	fivmr_ThreadState_dumpStackFor(env->ts);
    }

    env->ts->curExceptionHandle=(fivmr_Handle*)obj;
    return 0;
}

static jint jni_ThrowNew(JNIEnv *env,
			 jclass clazz,
			 const char *message) {
    jmethodID ctor;
    jobject obj;
    jstring str;
    ctor=(*env)->GetMethodID(env,clazz,"<init>","(Ljava/lang/String;)V");
    if (ctor==NULL) {
	return -1;
    }
    str=(*env)->NewStringUTF(env,message);
    if (str==NULL) {
	return -1;
    }
    obj=(*env)->NewObject(env,clazz,ctor,str);
    if (obj==NULL) {
	return -1;
    }
    return jni_Throw(env,obj);
}

static jthrowable jni_ExceptionOccurred(JNIEnv *env_) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    return (jthrowable)env->ts->curExceptionHandle;
}

static void jni_ExceptionDescribe(JNIEnv *env_) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    /* what if there was no exception? currently it'll just abort. */
    fivmr_describeException(env->ts,env->ts->curExceptionHandle);
}

static void jni_ExceptionClear(JNIEnv *env_) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    env->ts->curExceptionHandle=NULL;
}

static void jni_FatalError(JNIEnv *env_,const char *msg) {
    fivmr_abortf("Fatal error in JNI: %s",msg);
}

static jobject jni_NewGlobalRef(JNIEnv *env_,jobject obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_Handle *result;
    fivmr_ThreadState_goToJava(env->ts);
    result=fivmr_newGlobalHandle(env->ts,(fivmr_Handle*)obj);
    fivmr_ThreadState_goToNative(env->ts);
    return (jobject)result;
}

static void jni_DeleteGlobalRef(JNIEnv *env_,jobject obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_ThreadState_goToJava(env->ts);
    fivmr_deleteGlobalHandle((fivmr_Handle*)obj);
    fivmr_ThreadState_goToNative(env->ts);
}

static void jni_DeleteLocalRef(JNIEnv *env_,jobject obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_ThreadState_goToJava(env->ts);
    fivmr_Handle_remove(&env->ts->freeHandles,(fivmr_Handle*)obj);
    fivmr_ThreadState_goToNative(env->ts);
}

static jboolean jni_IsSameObject(JNIEnv *env_,
				 jobject ref1,
				 jobject ref2) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    bool result;
    /* FIXME if we ever have equality barriers... */
    fivmr_ThreadState_goToJava(env->ts);
    result=
	fivmr_Handle_get((fivmr_Handle*)ref1) ==
	fivmr_Handle_get((fivmr_Handle*)ref2);
    fivmr_ThreadState_goToNative(env->ts);
    return (jboolean)result;
}

static jobject jni_AllocObject(JNIEnv *env_,
			       jclass clazz) {
    /* what does this do?  I think it just does the allocation and
       forces you to make the constructor call yourself... */
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_ThreadState *ts=env->ts;
    fivmr_Handle *result=NULL;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(ts,(fivmr_Handle*)clazz);

    fivmr_ReflectLog_alloc(ts,-1,td);

    fivmr_ThreadState_goToJava(ts);
    if (check_init(ts,td)) {
	result=fivmr_ThreadState_addHandle(
	    ts,
	    fivmr_alloc(ts,FIVMR_GC_OBJ_SPACE,td));
	fivmr_ThreadState_handlifyException(ts);
    }
    fivmr_ThreadState_goToNative(ts);
    
    return (jobject)result;
}

static jobject jni_NewObject(JNIEnv *env,jclass clazz,jmethodID method,...) {
    va_list lst;
    jobject result;
    va_start(lst,method);
    result=(*env)->NewObjectV(env,clazz,method,lst);
    va_end(lst);
    return result;
}

static jobject jni_NewObjectV(JNIEnv *env,jclass clazz,jmethodID method,va_list lst) {
    fivmr_MethodRec *mr=(fivmr_MethodRec*)method;
    jvalue *args;
    args=alloca(sizeof(jvalue)*fivmr_MethodRec_numParams(mr));
    transferArgVals(mr,args,lst);
    return (*env)->NewObjectA(env,clazz,method,args);
}

static jobject jni_NewObjectA(JNIEnv *env,jclass clazz,jmethodID method,jvalue *args) {
    jobject obj;
    obj=(*env)->AllocObject(env,clazz);
    if ((*env)->ExceptionOccurred(env)) {
	return NULL;
    }
    (*env)->CallNonvirtualVoidMethodA(env,obj,clazz,method,args);
    if ((*env)->ExceptionOccurred(env)) {
	return NULL;
    } else {
	return obj;
    }
}

static jclass jni_GetObjectClass(JNIEnv *env_,jobject obj) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    return (jclass)produceHandleFromNative(
	env->ts,fivmr_TypeData_forHandle((fivmr_Handle*)obj)->classObject);
}

static jboolean jni_IsInstanceOf(JNIEnv *env_,jobject obj,jclass clazz) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    jboolean result;
    fivmr_ThreadState_goToJava(env->ts);
    result=(jboolean)fivmr_TypeData_isSubtypeOf(
        env->ts,
	fivmr_TypeData_forHandle((fivmr_Handle*)obj),
	fivmr_TypeData_fromClass(env->ts,fivmr_Handle_get((fivmr_Handle*)clazz)));
    fivmr_ThreadState_goToNative(env->ts);
    return result;
}

static jmethodID jni_GetMethodID(JNIEnv *env_,
				 jclass clazz,
				 const char *name,
				 const char *sig) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    if (check_init_easy(env_,clazz)) {
	fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
	fivmr_MethodRec *result=
	    fivmr_TypeData_findInstMethod(env->ts->vm,td,name,sig);
	if (result!=NULL && fivmr_MethodRec_exists(result)) {
	    fivmr_ReflectLog_dynamicCall(env->ts,-1,result);
	    return (jmethodID)result;
	} else {
	    LOG(3,("Could not find method %s %s in %s (%p)",name,sig,td->name,td));
	    fivmr_throwNoSuchMethodError(env->ts,name,sig);
	}
    }
    return NULL;
}

static jmethodID jni_GetStaticMethodID(JNIEnv *env_,
				       jclass clazz,
				       const char *name,
				       const char *sig) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    if (check_init_easy(env_,clazz)) {
	fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
	fivmr_MethodRec *result=
	    fivmr_TypeData_findStaticMethod(env->ts->vm,td,name,sig);
	if (result!=NULL) {
	    fivmr_ReflectLog_dynamicCall(env->ts,-1,result);
	    return (jmethodID)result;
	} else {
	    LOG(3,("Could not find static method %s %s in %s (%p)",name,sig,td->name,td));
	    fivmr_throwNoSuchMethodError(env->ts,name,sig);
	}
    }
    return NULL;
}

#define MAKE_CALL_METHOD(rtype,rname,mname)				\
    static rtype jni_Call##mname##Method(JNIEnv *env,			\
					 jobject obj,			\
					 jmethodID methodID,		\
					 ...) {				\
	va_list lst;							\
	rtype result;							\
	va_start(lst,methodID);						\
	result=(rtype)virtual_call_v(methodID,env,obj,lst).rname;	\
	va_end(lst);							\
	return result;							\
    }									\
    static rtype jni_Call##mname##MethodV(JNIEnv *env,			\
					  jobject obj,			\
					  jmethodID methodID,		\
					  va_list lst) {		\
	return (rtype)virtual_call_v(methodID,env,obj,lst).rname;	\
    }									\
    static rtype jni_Call##mname##MethodA(JNIEnv *env,			\
					  jobject obj,			\
					  jmethodID methodID,		\
					  jvalue *args) {		\
	return (rtype)virtual_call(methodID,env,obj,args).rname;	\
    }

MAKE_CALL_METHOD(jobject,H,Object)
MAKE_CALL_METHOD(jboolean,B,Boolean)
MAKE_CALL_METHOD(jbyte,B,Byte)
MAKE_CALL_METHOD(jchar,C,Char)
MAKE_CALL_METHOD(jshort,S,Short)
MAKE_CALL_METHOD(jint,I,Int)
MAKE_CALL_METHOD(jlong,L,Long)
MAKE_CALL_METHOD(jfloat,F,Float)
MAKE_CALL_METHOD(jdouble,D,Double)

static void jni_CallVoidMethod(JNIEnv *env,				
			       jobject obj,			
			       jmethodID methodID,			
			       ...) {				
    va_list lst;							
    va_start(lst,methodID);						
    virtual_call_v(methodID,env,obj,lst);	
    va_end(lst);							
}									

static void jni_CallVoidMethodV(JNIEnv *env,				
				jobject obj,				
				jmethodID methodID,			
				va_list lst) {			
    virtual_call_v(methodID,env,obj,lst);	
}									

static void jni_CallVoidMethodA(JNIEnv *env,				
				jobject obj,			       
				jmethodID methodID,		       
				jvalue *args) {		       
    virtual_call(methodID,env,obj,args);
}

#define MAKE_CALL_NV_METHOD(rtype,rname,mname)				\
    static rtype jni_CallNonvirtual##mname##Method(JNIEnv *env,		\
						   jobject obj,		\
						   jclass clazz,	\
						   jmethodID methodID,	\
						   ...) {		\
	va_list lst;							\
	rtype result;							\
	va_start(lst,methodID);						\
	result=(rtype)special_call_v(methodID,env,clazz,obj,lst).rname;	\
	va_end(lst);							\
	return result;							\
    }									\
    static rtype jni_CallNonvirtual##mname##MethodV(JNIEnv *env,	\
						    jobject obj,	\
						    jclass clazz,	\
						    jmethodID methodID,	\
						    va_list lst) {	\
	return (rtype)special_call_v(methodID,env,clazz,obj,lst).rname;	\
    }									\
    static rtype jni_CallNonvirtual##mname##MethodA(JNIEnv *env,	\
						    jobject obj,	\
						    jclass clazz,	\
						    jmethodID methodID,	\
						    jvalue *args) {	\
	return (rtype)special_call(methodID,env,clazz,obj,args).rname;	\
    }

MAKE_CALL_NV_METHOD(jobject,H,Object)
MAKE_CALL_NV_METHOD(jboolean,B,Boolean)
MAKE_CALL_NV_METHOD(jbyte,B,Byte)
MAKE_CALL_NV_METHOD(jchar,C,Char)
MAKE_CALL_NV_METHOD(jshort,S,Short)
MAKE_CALL_NV_METHOD(jint,I,Int)
MAKE_CALL_NV_METHOD(jlong,L,Long)
MAKE_CALL_NV_METHOD(jfloat,F,Float)
MAKE_CALL_NV_METHOD(jdouble,D,Double)

static void jni_CallNonvirtualVoidMethod(JNIEnv *env,				
					 jobject obj,			
					 jclass clazz,
					 jmethodID methodID,			
					 ...) {				
    va_list lst;							
    va_start(lst,methodID);						
    special_call_v(methodID,env,clazz,obj,lst);	
    va_end(lst);							
}									

static void jni_CallNonvirtualVoidMethodV(JNIEnv *env,				
					  jobject obj,				
					  jclass clazz,
					  jmethodID methodID,			
					  va_list lst) {			
    special_call_v(methodID,env,clazz,obj,lst);	
}					
				
static void jni_CallNonvirtualVoidMethodA(JNIEnv *env,				
					  jobject obj,			       
					  jclass clazz,
					  jmethodID methodID,		       
					  jvalue *args) {		       
    special_call(methodID,env,clazz,obj,args);
}

#define MAKE_CALL_S_METHOD(rtype,rname,mname)				\
    static rtype jni_CallStatic##mname##Method(JNIEnv *env,		\
					       jclass clazz,		\
					       jmethodID methodID,	\
					       ...) {			\
	va_list lst;							\
	rtype result;							\
	va_start(lst,methodID);						\
	result=(rtype)static_call_v(methodID,env,lst).rname;		\
	va_end(lst);							\
	return result;							\
    }									\
    static rtype jni_CallStatic##mname##MethodV(JNIEnv *env,		\
						jclass clazz,		\
						jmethodID methodID,	\
						va_list lst) {		\
	return (rtype)static_call_v(methodID,env,lst).rname;		\
    }									\
    static rtype jni_CallStatic##mname##MethodA(JNIEnv *env,		\
						jclass clazz,		\
						jmethodID methodID,	\
						jvalue *args) {		\
	return (rtype)static_call(methodID,env,args).rname;		\
    }

MAKE_CALL_S_METHOD(jobject,H,Object)
MAKE_CALL_S_METHOD(jboolean,B,Boolean)
MAKE_CALL_S_METHOD(jbyte,B,Byte)
MAKE_CALL_S_METHOD(jchar,C,Char)
MAKE_CALL_S_METHOD(jshort,S,Short)
MAKE_CALL_S_METHOD(jint,I,Int)
MAKE_CALL_S_METHOD(jlong,L,Long)
MAKE_CALL_S_METHOD(jfloat,F,Float)
MAKE_CALL_S_METHOD(jdouble,D,Double)

static void jni_CallStaticVoidMethod(JNIEnv *env,	
				     jclass clazz,
				     jmethodID methodID,			
				     ...) {				
    va_list lst;							
    va_start(lst,methodID);						
    static_call_v(methodID,env,lst);	
    va_end(lst);							
}									

static void jni_CallStaticVoidMethodV(JNIEnv *env,				
				      jclass clazz,
				      jmethodID methodID,			
				      va_list lst) {			
    static_call_v(methodID,env,lst);	
}					
				
static void jni_CallStaticVoidMethodA(JNIEnv *env,				
				      jclass clazz,
				      jmethodID methodID,		       
				      jvalue *args) {		       
    static_call(methodID,env,args);
}

static jfieldID jni_GetFieldID(JNIEnv *env_,
			       jclass clazz,
			       const char *name,
			       const char *sig) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
    LOG(8,("from class handle %p, with obj %p, got td = %d",
	   clazz,((fivmr_Handle*)clazz)->obj,td));
    if (check_init_inNative(env->ts,td)) {
        fivmr_FieldRec *result=fivmr_TypeData_findInstField(td,name,sig);
        if (result!=NULL) {
            fivmr_ReflectLog_access(env->ts,-1,result);
            return (jfieldID)result;
	} else {
            LOG(3,("Could not find field %s %s in %s (%p)",
                   name,sig,
                   fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz)->name,
                   fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz)));
            fivmr_throwNoSuchFieldError(env->ts,name,sig);
        }
    }
    return NULL;
}

static jfieldID jni_GetStaticFieldID(JNIEnv *env_,
				     jclass clazz,
				     const char *name,
				     const char *sig) {
    fivmr_JNIEnv *env=(fivmr_JNIEnv*)env_;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(env->ts,(fivmr_Handle*)clazz);
    if (check_init_inNative(env->ts,td)) {
        fivmr_FieldRec *result=fivmr_TypeData_findStaticField(td,name,sig);
        if (result!=NULL) {
            fivmr_ReflectLog_access(env->ts,-1,result);
            return (jfieldID)result;
	} else {
            LOG(3,("Could not find static field %s %s in %s (%p)",name,sig,td->name,td));
            fivmr_throwNoSuchMethodError(env->ts,name,sig);
        }
    }
    return NULL;
}

static jobject jni_GetObjectField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return NULL;
    } else {
	jobject result;
	fivmr_ThreadState_goToJava(ts);
	result=(jobject)
	    fivmr_ThreadState_addHandle(
		ts,
		fivmr_objectGetField(ts,
				     fivmr_Handle_get((fivmr_Handle*)obj),
				     fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                                  (fivmr_FieldRec*)fieldID),
				     ((fivmr_FieldRec*)fieldID)->flags));
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jboolean jni_GetBooleanField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jboolean result;
	fivmr_ThreadState_goToJava(ts);
	result=(jboolean)
	    fivmr_byteGetField(ts,
			       fivmr_Handle_get((fivmr_Handle*)obj),
			       fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                            (fivmr_FieldRec*)fieldID),
			       ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jbyte jni_GetByteField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jbyte result;
	fivmr_ThreadState_goToJava(ts);
	result=(jbyte)
	    fivmr_byteGetField(ts,
			       fivmr_Handle_get((fivmr_Handle*)obj),
			       fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                            (fivmr_FieldRec*)fieldID),
			       ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jchar jni_GetCharField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jchar result;
	fivmr_ThreadState_goToJava(ts);
	result=(jchar)
	    fivmr_charGetField(ts,
			       fivmr_Handle_get((fivmr_Handle*)obj),
			       fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                            (fivmr_FieldRec*)fieldID),
			       ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jshort jni_GetShortField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jshort result;
	fivmr_ThreadState_goToJava(ts);
	result=(jshort)
	    fivmr_shortGetField(ts,
				fivmr_Handle_get((fivmr_Handle*)obj),
				fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                             (fivmr_FieldRec*)fieldID),
				((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jint jni_GetIntField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jint result;
	fivmr_ThreadState_goToJava(ts);
	result=(jint)
	    fivmr_intGetField(ts,
			      fivmr_Handle_get((fivmr_Handle*)obj),
			      fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                           (fivmr_FieldRec*)fieldID),
			      ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jlong jni_GetLongField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0;
    } else {
	jlong result;
	fivmr_ThreadState_goToJava(ts);
	result=(jlong)
	    fivmr_longGetField(ts,
			       fivmr_Handle_get((fivmr_Handle*)obj),
			       fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                            (fivmr_FieldRec*)fieldID),
			       ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jfloat jni_GetFloatField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0.0;
    } else {
	jfloat result;
	fivmr_ThreadState_goToJava(ts);
	result=(jfloat)
	    fivmr_floatGetField(ts,
				fivmr_Handle_get((fivmr_Handle*)obj),
				fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                             (fivmr_FieldRec*)fieldID),
				((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static jdouble jni_GetDoubleField(JNIEnv *env,jobject obj,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
	return 0.0;
    } else {
	jdouble result;
	fivmr_ThreadState_goToJava(ts);
	result=(jdouble)
	    fivmr_doubleGetField(ts,
				 fivmr_Handle_get((fivmr_Handle*)obj),
				 fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                              (fivmr_FieldRec*)fieldID),
				 ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
	return result;
    }
}

static void jni_SetObjectField(JNIEnv *env,
			       jobject obj,
			       jfieldID fieldID,
			       jobject value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_objectPutField(ts,
			     fivmr_Handle_get((fivmr_Handle*)obj),
			     fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                          (fivmr_FieldRec*)fieldID),
			     fivmr_Handle_get((fivmr_Handle*)value),
			     ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetBooleanField(JNIEnv *env,
				jobject obj,
				jfieldID fieldID,
				jboolean value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_bytePutField(ts,
			   fivmr_Handle_get((fivmr_Handle*)obj),
			   fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                        (fivmr_FieldRec*)fieldID),
			   value,
			   ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetByteField(JNIEnv *env,
			     jobject obj,
			     jfieldID fieldID,
			     jbyte value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_bytePutField(ts,
			   fivmr_Handle_get((fivmr_Handle*)obj),
			   fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                        (fivmr_FieldRec*)fieldID),
			   value,
			   ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetCharField(JNIEnv *env,
			     jobject obj,
			     jfieldID fieldID,
			     jchar value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_charPutField(ts,
			   fivmr_Handle_get((fivmr_Handle*)obj),
			   fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                        (fivmr_FieldRec*)fieldID),
			   value,
			   ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetShortField(JNIEnv *env,
			      jobject obj,
			      jfieldID fieldID,
			      jshort value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_shortPutField(ts,
			    fivmr_Handle_get((fivmr_Handle*)obj),
			    fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                         (fivmr_FieldRec*)fieldID),
			    value,
			    ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetIntField(JNIEnv *env,
			    jobject obj,
			    jfieldID fieldID,
			    jint value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_intPutField(ts,
			  fivmr_Handle_get((fivmr_Handle*)obj),
			  fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                       (fivmr_FieldRec*)fieldID),
			  value,
			  ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetLongField(JNIEnv *env,
			     jobject obj,
			     jfieldID fieldID,
			     jlong value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_longPutField(ts,
			   fivmr_Handle_get((fivmr_Handle*)obj),
			   fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                        (fivmr_FieldRec*)fieldID),
			   value,
			   ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetFloatField(JNIEnv *env,
			      jobject obj,
			      jfieldID fieldID,
			      jfloat value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_floatPutField(ts,
			    fivmr_Handle_get((fivmr_Handle*)obj),
			    fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                         (fivmr_FieldRec*)fieldID),
			    value,
			    ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static void jni_SetDoubleField(JNIEnv *env,
			       jobject obj,
			       jfieldID fieldID,
			       jdouble value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (obj==NULL) {
	fivmr_throwNullPointerRTE(ts);
    } else {
	fivmr_ThreadState_goToJava(ts);
	fivmr_doublePutField(ts,
			     fivmr_Handle_get((fivmr_Handle*)obj),
			     fivmr_FieldRec_offsetFromObj(&ts->vm->settings,
                                                          (fivmr_FieldRec*)fieldID),
			     value,
			     ((fivmr_FieldRec*)fieldID)->flags);
	fivmr_ThreadState_goToNative(ts);
    }
}

static jobject jni_GetStaticObjectField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_FieldRec *fr=(fivmr_FieldRec*)fieldID;
    jobject result=NULL;
    fivmr_ThreadState_goToJava(ts);
    if (check_init(ts,((fivmr_FieldRec*)fieldID)->owner)) {
	result=(jobject)
	    fivmr_ThreadState_addHandle(
		ts,
		fivmr_objectGetStatic(
		    ts,
		    (fivmr_Object*)fivmr_FieldRec_staticFieldAddress(ts->vm,fr),
		    fr->flags));
    }
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static void jni_SetStaticObjectField(JNIEnv *env,
				     jclass clazz,
				     jfieldID fieldID,
				     jobject value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ThreadState_goToJava(ts);
    if (check_init(ts,((fivmr_FieldRec*)fieldID)->owner)) {
	fivmr_objectPutStatic(
	    ts,
	    (fivmr_Object*)fivmr_FieldRec_staticFieldAddress(
                ts->vm,
		(fivmr_FieldRec*)fieldID),
	    fivmr_Handle_get((fivmr_Handle*)value),
	    0);
    }
    fivmr_ThreadState_goToNative(ts);
}

static jboolean jni_GetStaticBooleanField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jboolean*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jbyte jni_GetStaticByteField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jbyte*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jchar jni_GetStaticCharField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jchar*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jshort jni_GetStaticShortField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jshort*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jint jni_GetStaticIntField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jint*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jlong jni_GetStaticLongField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jlong*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0;
    }
}

static jfloat jni_GetStaticFloatField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jfloat*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0.0;
    }
}

static jdouble jni_GetStaticDoubleField(JNIEnv *env,jclass clazz,jfieldID fieldID) {
    if (check_init_easy(env,clazz)) {
	return *((jdouble*)fivmr_FieldRec_staticFieldAddress(
                     ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID));
    } else {
	return 0.0;
    }
}

/* FIXME: convert these to use C89-compliant lvalues. */

static void jni_SetStaticBooleanField(JNIEnv *env,
				      jclass clazz,
				      jfieldID fieldID,
				      jboolean value) {
    if (check_init_easy(env,clazz)) {
	*((jboolean*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticByteField(JNIEnv *env,
				   jclass clazz,
				   jfieldID fieldID,
				   jbyte value) {
    if (check_init_easy(env,clazz)) {
	*((jbyte*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticCharField(JNIEnv *env,
				   jclass clazz,
				   jfieldID fieldID,
				   jchar value) {
    if (check_init_easy(env,clazz)) {
	*((jchar*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticShortField(JNIEnv *env,
				    jclass clazz,
				    jfieldID fieldID,
				    jshort value) {
    if (check_init_easy(env,clazz)) {
	*((jshort*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticIntField(JNIEnv *env,
				  jclass clazz,
				  jfieldID fieldID,
				  jint value) {
    if (check_init_easy(env,clazz)) {
	*((jint*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticLongField(JNIEnv *env,
				   jclass clazz,
				   jfieldID fieldID,
				   jlong value) {
    if (check_init_easy(env,clazz)) {
	*((jlong*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticFloatField(JNIEnv *env,
				    jclass clazz,
				    jfieldID fieldID,
				    jfloat value) {
    if (check_init_easy(env,clazz)) {
	*((jfloat*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static void jni_SetStaticDoubleField(JNIEnv *env,
				     jclass clazz,
				     jfieldID fieldID,
				     jdouble value) {
    if (check_init_easy(env,clazz)) {
	*((jdouble*)fivmr_FieldRec_staticFieldAddress(
              ((fivmr_JNIEnv*)env)->ts->vm,(fivmr_FieldRec*)fieldID))=value;
    }
}

static jstring jni_NewString(JNIEnv *env,
			     const jchar *unicodeChars,
			     jsize len) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jstring)fivmr_fromUTF16Sequence(ts,unicodeChars,len);
}

static jsize jni_GetStringLength(JNIEnv *env,jstring str) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return fivmr_stringLength(ts,(fivmr_Handle*)str);
}

static const jchar *jni_GetStringChars(JNIEnv *env,jstring string,jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return fivmr_getUTF16Sequence(ts,(fivmr_Handle*)string);
}

static void jni_ReleaseStringChars(JNIEnv *env,jstring string,const jchar *chars) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnBuffer(ts,(void*)(uintptr_t)chars);
}

static jstring jni_NewStringUTF(JNIEnv *env,
				const char *bytes) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jstring)fivmr_fromCStringFull(ts,bytes);
}

static jsize jni_GetStringUTFLength(JNIEnv *env,jstring str) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return fivmr_cstringLength(ts,(fivmr_Handle*)str);
}

static const char *jni_GetStringUTFChars(JNIEnv *env,
					  jstring string,
					  jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (char*)fivmr_getCStringFull(ts,(fivmr_Handle*)string);
}

static void jni_ReleaseStringUTFChars(JNIEnv *env,jstring string,const char *utf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnBuffer(ts,(void*)(uintptr_t)utf);
}

static jsize jni_GetArrayLength(JNIEnv *env,jarray array) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    int32_t result=0;
    fivmr_ThreadState_goToJava(ts);
    if (array==NULL) {
	fivmr_throwNullPointerRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	fivmr_Object arrObj=fivmr_Handle_get((fivmr_Handle*)array);
	fivmr_TypeData *arrTD=fivmr_TypeData_forObject(&ts->vm->settings,arrObj);
	if (arrTD->name[0]!='[') {
	    fivmr_throwClassCastRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else {
	    result=fivmr_arrayLength(ts,fivmr_Handle_get((fivmr_Handle*)array),0);
	}
    }
    fivmr_ThreadState_goToNative(ts);
    return result;
}

static jobjectArray jni_NewObjectArray(JNIEnv *env,
				       jsize length,
				       jclass elementClass,
				       jobject initialElement) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_TypeData *td;
    fivmr_Object arr;
    fivmr_Object ele;
    fivmr_Handle *result=NULL;
    int32_t n;
    fivmr_ThreadState_goToJava(ts);
    if (length<0) {
	fivmr_throwNegativeSizeRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	td=fivmr_TypeData_makeArray(
            fivmr_TypeData_fromClass(ts,fivmr_Handle_get((fivmr_Handle*)elementClass)));
	fivmr_ReflectLog_alloc(ts,-1,td);
	ele=fivmr_Handle_get((fivmr_Handle*)initialElement);
	arr=fivmr_allocArray(ts,FIVMR_GC_OBJ_SPACE,td,length);
	for (n=length;n-->0;) {
	    fivmr_objectArrayStore(ts,arr,n,ele,0);
	}
	result=fivmr_ThreadState_addHandle(ts,arr);
    }
    fivmr_ThreadState_goToNative(ts);
    return (jobjectArray)result;
}

static jobject jni_GetObjectArrayElement(JNIEnv *env,
					 jobjectArray array,
					 jsize index) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_Handle *result=NULL;
    fivmr_ThreadState_goToJava(ts);
    if (array==NULL) {
	fivmr_throwNullPointerRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	fivmr_Object arrObj=fivmr_Handle_get((fivmr_Handle*)array);
	fivmr_TypeData *arrTD=fivmr_TypeData_forObject(&ts->vm->settings,arrObj);
	if (arrTD->name[0]!='[' ||
	    arrTD->name[1]!='L') {
	    fivmr_throwClassCastRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else if ((uint32_t)index>=(uint32_t)fivmr_arrayLength(ts,arrObj,0)) {
	    fivmr_throwArrayBoundsRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else {
	    result=fivmr_ThreadState_addHandle(
		ts,fivmr_objectArrayLoad(ts,arrObj,index,0));
	}
    }
    fivmr_ThreadState_goToNative(ts);
    return (jobject)result;
}

static void jni_SetObjectArrayElement(JNIEnv *env,
				      jobjectArray array,
				      jsize index,
				      jobject value) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ThreadState_goToJava(ts);
    if (array==NULL) {
	fivmr_throwNullPointerRTE_inJava(ts);
	fivmr_ThreadState_handlifyException(ts);
    } else {
	fivmr_Object arrObj=fivmr_Handle_get((fivmr_Handle*)array);
	fivmr_Object eleObj=fivmr_Handle_get((fivmr_Handle*)value);
	fivmr_TypeData *arrTD=fivmr_TypeData_forObject(&ts->vm->settings,arrObj);
	if (arrTD->name[0]!='[' ||
	    arrTD->name[1]!='L') {
	    fivmr_throwClassCastRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else if ((uint32_t)index>=(uint32_t)fivmr_arrayLength(ts,arrObj,0)) {
	    fivmr_throwArrayBoundsRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else if (eleObj!=0 &&
		   !fivmr_TypeData_isSubtypeOf(
                       ts,
		       fivmr_TypeData_forObject(&ts->vm->settings,eleObj),
		       arrTD->arrayElement)) {
	    fivmr_throwArrayStoreRTE_inJava(ts);
	    fivmr_ThreadState_handlifyException(ts);
	} else {
	    fivmr_objectArrayStore(ts,arrObj,index,eleObj,0);
	}
    }
    fivmr_ThreadState_goToNative(ts);
}

static jbooleanArray jni_NewBooleanArray(JNIEnv *env,jsize length) {
    return (jbooleanArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_booleanArr,length);
}

static jbyteArray jni_NewByteArray(JNIEnv *env,jsize length) {
    return (jbyteArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_byteArr,length);
}

static jcharArray jni_NewCharArray(JNIEnv *env,jsize length) {
    return (jcharArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_charArr,length);
}

static jshortArray jni_NewShortArray(JNIEnv *env,jsize length) {
    return (jshortArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_shortArr,length);
}

static jintArray jni_NewIntArray(JNIEnv *env,jsize length) {
    return (jintArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_intArr,length);
}

static jlongArray jni_NewLongArray(JNIEnv *env,jsize length) {
    return (jlongArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_longArr,length);
}

static jfloatArray jni_NewFloatArray(JNIEnv *env,jsize length) {
    return (jfloatArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_floatArr,length);
}

static jdoubleArray jni_NewDoubleArray(JNIEnv *env,jsize length) {
    return (jdoubleArray)alloc_array(
	env,((fivmr_JNIEnv*)env)->ts->vm->payload->td_doubleArr,length);
}

static jboolean *jni_GetBooleanArrayElements(JNIEnv *env,
					     jbooleanArray array,
					     jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jboolean*)fivmr_getBooleanElements(ts,(fivmr_Handle*)array);
}

static jbyte *jni_GetByteArrayElements(JNIEnv *env,
				       jbyteArray array,
				       jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jbyte*)fivmr_getByteElements(ts,(fivmr_Handle*)array);
}

static jchar *jni_GetCharArrayElements(JNIEnv *env,
				       jcharArray array,
				       jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jchar*)fivmr_getCharElements(ts,(fivmr_Handle*)array);
}

static jshort *jni_GetShortArrayElements(JNIEnv *env,
					 jshortArray array,
					 jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jshort*)fivmr_getShortElements(ts,(fivmr_Handle*)array);
}

static jint *jni_GetIntArrayElements(JNIEnv *env,
				     jintArray array,
				     jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jint*)fivmr_getIntElements(ts,(fivmr_Handle*)array);
}

static jlong *jni_GetLongArrayElements(JNIEnv *env,
				       jlongArray array,
				       jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jlong*)fivmr_getLongElements(ts,(fivmr_Handle*)array);
}

static jfloat *jni_GetFloatArrayElements(JNIEnv *env,
					 jfloatArray array,
					 jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jfloat*)fivmr_getFloatElements(ts,(fivmr_Handle*)array);
}

static jdouble *jni_GetDoubleArrayElements(JNIEnv *env,
					   jdoubleArray array,
					   jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (isCopy) *isCopy=true;
    return (jdouble*)fivmr_getDoubleElements(ts,(fivmr_Handle*)array);
}

static void jni_ReleaseBooleanArrayElements(JNIEnv *env,
					    jbooleanArray array,
					    jboolean *elems,
					    jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnBooleanElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseByteArrayElements(JNIEnv *env,
					 jbooleanArray array,
					 jboolean *elems,
					 jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnByteElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseCharArrayElements(JNIEnv *env,
					 jcharArray array,
					 jchar *elems,
					 jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnCharElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseShortArrayElements(JNIEnv *env,
					  jshortArray array,
					  jshort *elems,
					  jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnShortElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseIntArrayElements(JNIEnv *env,
					jintArray array,
					jint *elems,
					jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnIntElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseLongArrayElements(JNIEnv *env,
					 jlongArray array,
					 jlong *elems,
					 jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnLongElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseFloatArrayElements(JNIEnv *env,
					  jfloatArray array,
					  jfloat *elems,
					  jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnFloatElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_ReleaseDoubleArrayElements(JNIEnv *env,
					   jdoubleArray array,
					   jdouble *elems,
					   jint mode) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_returnDoubleElements(ts,(fivmr_Handle*)array,elems,mode);
}

static void jni_GetBooleanArrayRegion(JNIEnv *env,
				      jbooleanArray array,
				      jsize start,
				      jsize len,
				      jboolean *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getBooleanRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetByteArrayRegion(JNIEnv *env,
				   jbyteArray array,
				   jsize start,
				   jsize len,
				   jbyte *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getByteRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetCharArrayRegion(JNIEnv *env,
				   jcharArray array,
				   jsize start,
				   jsize len,
				   jchar *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getCharRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetShortArrayRegion(JNIEnv *env,
				    jshortArray array,
				    jsize start,
				    jsize len,
				    jshort *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getShortRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetIntArrayRegion(JNIEnv *env,
				  jintArray array,
				  jsize start,
				  jsize len,
				  jint *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getIntRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetLongArrayRegion(JNIEnv *env,
				   jlongArray array,
				   jsize start,
				   jsize len,
				   jlong *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getLongRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetFloatArrayRegion(JNIEnv *env,
				    jfloatArray array,
				    jsize start,
				    jsize len,
				    jfloat *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getFloatRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_GetDoubleArrayRegion(JNIEnv *env,
				     jdoubleArray array,
				     jsize start,
				     jsize len,
				     jdouble *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getDoubleRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetBooleanArrayRegion(JNIEnv *env,
				      jbooleanArray array,
				      jsize start,
				      jsize len,
				      jboolean *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setBooleanRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetByteArrayRegion(JNIEnv *env,
				   jbyteArray array,
				   jsize start,
				   jsize len,
				   jbyte *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setByteRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetCharArrayRegion(JNIEnv *env,
				   jcharArray array,
				   jsize start,
				   jsize len,
				   jchar *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setCharRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetShortArrayRegion(JNIEnv *env,
				    jshortArray array,
				    jsize start,
				    jsize len,
				    jshort *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setShortRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetIntArrayRegion(JNIEnv *env,
				  jintArray array,
				  jsize start,
				  jsize len,
				  jint *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setIntRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetLongArrayRegion(JNIEnv *env,
				   jlongArray array,
				   jsize start,
				   jsize len,
				   jlong *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setLongRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetFloatArrayRegion(JNIEnv *env,
				    jfloatArray array,
				    jsize start,
				    jsize len,
				    jfloat *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setFloatRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static void jni_SetDoubleArrayRegion(JNIEnv *env,
				     jdoubleArray array,
				     jsize start,
				     jsize len,
				     jdouble *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_setDoubleRegion(ts,(fivmr_Handle*)array,start,len,buf);
}

static jint jni_RegisterNatives(JNIEnv *env,
				jclass clazz,
				const JNINativeMethod *methods,
				jint nmethods) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(ts,(fivmr_Handle*)clazz);
    int32_t i;
    jint result=0;
    for (i=0;i<nmethods;++i) {
	fivmr_MethodRec *mr=fivmr_TypeData_findMethod(ts->vm,
                                                      td,
						      methods[i].name,
						      methods[i].signature);
	if (mr==NULL ||
	    (mr->flags&FIVMR_MBF_METHOD_IMPL)!=FIVMR_MBF_JNI) {
	    LOG(1,("Failed to register method %s %s on class %s.",
		   methods[i].name,methods[i].signature,td->name));
	    /* FIXME: what if the method got axed by 0CFA? */
	    result=-1;
	} else {
	    mr->codePtr=methods[i].fnPtr;
	    LOG(1,("Registered method %s %s on class %s (%p).",
		   methods[i].name,methods[i].signature,td->name,methods[i].fnPtr));
	}
    }
    return 0;
}

static jint jni_UnregisterNatives(JNIEnv *env,
				  jclass clazz) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_TypeData *td=fivmr_TypeData_fromClass_inNative(ts,(fivmr_Handle*)clazz);
    int32_t i;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if ((mr->flags&FIVMR_MBF_METHOD_IMPL)==FIVMR_MBF_JNI) {
            td->methods[i]->codePtr=NULL;
        }
    }
    return 0;
}

static jint jni_MonitorEnter(JNIEnv *env,jobject obj) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ThreadState_goToJava(ts);
    fivmr_Monitor_lock_slow(
	fivmr_ObjHeader_forObject(
            &ts->vm->settings,
	    fivmr_Handle_get((fivmr_Handle*)obj)),
	ts);
    fivmr_ThreadState_goToNative(ts);
    return 0;
}

static jint jni_MonitorExit(JNIEnv *env,jobject obj) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_ThreadState_goToJava(ts);
    fivmr_Monitor_unlock_slow(
	fivmr_ObjHeader_forObject(
            &ts->vm->settings,
	    fivmr_Handle_get((fivmr_Handle*)obj)),
	ts);
    fivmr_ThreadState_goToNative(ts);
    return 0;
}

static jint jni_GetJavaVM(JNIEnv *env,JavaVM **vm) {
    *vm=(JavaVM*) &((fivmr_JNIEnv*)env)->ctx->jniVM;
    return 0;
}

static void jni_GetStringRegion(JNIEnv *env,
				jstring str,
				jsize start,
				jsize len,
				jchar *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getStringRegion(ts,(fivmr_Handle*)str,start,len,buf);
}

/* NEVER USE THIS.  JNI mandates it but that's because it's dumb.  it's a buffer
   overflow waiting to happen. */
static void jni_GetStringUTFRegion(JNIEnv *env,
				   jstring str,
				   jsize start,
				   jsize len,
				   char *buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    fivmr_getStringUTFRegion(ts,(fivmr_Handle*)str,start,len,buf);
}

static void *jni_GetPrimitiveArrayCritical(JNIEnv *env,jarray array,jboolean *isCopy) {
    /* FIXME: this is specific to our current way of handling arrays.  this will
       have to be changed once we have arraylets. */
    /* FIXME FIX FIX FIX!!! */
    if (isCopy) *isCopy=false;
    return (void*)((fivmr_Handle*)array)->obj;
}

static void jni_ReleasePrimitiveArrayCritical(JNIEnv *env,
					      jarray array,
					      void *carray,
					      jint mode) {
    /* do nothing. */
    /* FIXME: see above though */
}

static const jchar *jni_GetStringCritical(JNIEnv *env,
					  jstring string,
					  jboolean *isCopy) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (FIVMR_OM_CONTIGUOUS(&ts->vm->settings)) {
        if (isCopy) *isCopy=false;
        return fivmr_String_getArrayPointer(ts,(fivmr_Handle*)string)
            + fivmr_String_getOffset(ts,(fivmr_Handle*)string);
    } else {
        if (isCopy) *isCopy=true;
        return fivmr_getUTF16Sequence(ts,(fivmr_Handle*)string);
    }
}

static void jni_ReleaseStringCritical(JNIEnv *env,
				      jstring str,
				      const jchar *carray) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    if (FIVMR_OM_CONTIGUOUS(&ts->vm->settings)) {
        /* do nothing. */
    } else {
        fivmr_returnBuffer(ts,(void*)(uintptr_t)carray);
    }
}

static jweak jni_NewWeakGlobalRef(JNIEnv *env,jobject obj) {
    return NULL; /* FIXME actually implement this at some point. */
}

static void jni_DeleteWeakGlobalRef(JNIEnv *env,jweak obj) {
    /* do nothing for now */
    /* FIXME actually implement this at some point. */
}

static jboolean jni_ExceptionCheck(JNIEnv *env) {
    return ((fivmr_JNIEnv*)env)->ts->curExceptionHandle!=NULL;
}

static jobject jni_NewDirectByteBuffer(JNIEnv *env,void *address,jlong capacity) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jobject)fivmr_DirectByteBuffer_wrap(ts,
                                                (uintptr_t)address,
						(int32_t)capacity,
						(int32_t)capacity,
						0);
}

static void *jni_GetDirectBufferAddress(JNIEnv *env,jobject buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (void*)fivmr_DirectByteBuffer_address(ts,(fivmr_Handle*)buf);
}

static jlong jni_GetDirectBufferCapacity(JNIEnv *env,jobject buf) {
    fivmr_ThreadState *ts=((fivmr_JNIEnv*)env)->ts;
    return (jlong)fivmr_DirectByteBuffer_capacity(ts,(fivmr_Handle*)buf);
}

struct JNINativeInterface fivmr_jniFunctions={
    NULL,
    NULL,
    NULL,
    NULL,
    jni_GetVersion,
    jni_DefineClass,
    jni_FindClass,
    NULL,
    NULL,
    NULL,
    jni_GetSuperclass,
    jni_IsAssignableFrom,
    NULL,
    jni_Throw,
    jni_ThrowNew,
    jni_ExceptionOccurred,
    jni_ExceptionDescribe,
    jni_ExceptionClear,
    jni_FatalError,
    NULL,
    NULL,
    jni_NewGlobalRef,
    jni_DeleteGlobalRef,
    jni_DeleteLocalRef,
    jni_IsSameObject,
    NULL,
    NULL,
    jni_AllocObject,
    jni_NewObject,
    jni_NewObjectA,
    jni_NewObjectV,
    jni_GetObjectClass,
    jni_IsInstanceOf,
    jni_GetMethodID,
    jni_CallObjectMethod,
    jni_CallObjectMethodA,
    jni_CallObjectMethodV,
    jni_CallBooleanMethod,
    jni_CallBooleanMethodA,
    jni_CallBooleanMethodV,
    jni_CallByteMethod,
    jni_CallByteMethodA,
    jni_CallByteMethodV,
    jni_CallCharMethod,
    jni_CallCharMethodA,
    jni_CallCharMethodV,
    jni_CallShortMethod,
    jni_CallShortMethodA,
    jni_CallShortMethodV,
    jni_CallIntMethod,
    jni_CallIntMethodA,
    jni_CallIntMethodV,
    jni_CallLongMethod,
    jni_CallLongMethodA,
    jni_CallLongMethodV,
    jni_CallFloatMethod,
    jni_CallFloatMethodA,
    jni_CallFloatMethodV,
    jni_CallDoubleMethod,
    jni_CallDoubleMethodA,
    jni_CallDoubleMethodV,
    jni_CallVoidMethod,
    jni_CallVoidMethodA,
    jni_CallVoidMethodV,
    jni_CallNonvirtualObjectMethod,
    jni_CallNonvirtualObjectMethodA,
    jni_CallNonvirtualObjectMethodV,
    jni_CallNonvirtualBooleanMethod,
    jni_CallNonvirtualBooleanMethodA,
    jni_CallNonvirtualBooleanMethodV,
    jni_CallNonvirtualByteMethod,
    jni_CallNonvirtualByteMethodA,
    jni_CallNonvirtualByteMethodV,
    jni_CallNonvirtualCharMethod,
    jni_CallNonvirtualCharMethodA,
    jni_CallNonvirtualCharMethodV,
    jni_CallNonvirtualShortMethod,
    jni_CallNonvirtualShortMethodA,
    jni_CallNonvirtualShortMethodV,
    jni_CallNonvirtualIntMethod,
    jni_CallNonvirtualIntMethodA,
    jni_CallNonvirtualIntMethodV,
    jni_CallNonvirtualLongMethod,
    jni_CallNonvirtualLongMethodA,
    jni_CallNonvirtualLongMethodV,
    jni_CallNonvirtualFloatMethod,
    jni_CallNonvirtualFloatMethodA,
    jni_CallNonvirtualFloatMethodV,
    jni_CallNonvirtualDoubleMethod,
    jni_CallNonvirtualDoubleMethodA,
    jni_CallNonvirtualDoubleMethodV,
    jni_CallNonvirtualVoidMethod,
    jni_CallNonvirtualVoidMethodA,
    jni_CallNonvirtualVoidMethodV,
    jni_GetFieldID,
    jni_GetObjectField,
    jni_GetBooleanField,
    jni_GetByteField,
    jni_GetCharField,
    jni_GetShortField,
    jni_GetIntField,
    jni_GetLongField,
    jni_GetFloatField,
    jni_GetDoubleField,
    jni_SetObjectField,
    jni_SetBooleanField,
    jni_SetByteField,
    jni_SetCharField,
    jni_SetShortField,
    jni_SetIntField,
    jni_SetLongField,
    jni_SetFloatField,
    jni_SetDoubleField,
    jni_GetStaticMethodID,
    jni_CallStaticObjectMethod,
    jni_CallStaticObjectMethodA,
    jni_CallStaticObjectMethodV,
    jni_CallStaticBooleanMethod,
    jni_CallStaticBooleanMethodA,
    jni_CallStaticBooleanMethodV,
    jni_CallStaticByteMethod,
    jni_CallStaticByteMethodA,
    jni_CallStaticByteMethodV,
    jni_CallStaticCharMethod,
    jni_CallStaticCharMethodA,
    jni_CallStaticCharMethodV,
    jni_CallStaticShortMethod,
    jni_CallStaticShortMethodA,
    jni_CallStaticShortMethodV,
    jni_CallStaticIntMethod,
    jni_CallStaticIntMethodA,
    jni_CallStaticIntMethodV,
    jni_CallStaticLongMethod,
    jni_CallStaticLongMethodA,
    jni_CallStaticLongMethodV,
    jni_CallStaticFloatMethod,
    jni_CallStaticFloatMethodA,
    jni_CallStaticFloatMethodV,
    jni_CallStaticDoubleMethod,
    jni_CallStaticDoubleMethodA,
    jni_CallStaticDoubleMethodV,
    jni_CallStaticVoidMethod,
    jni_CallStaticVoidMethodA,
    jni_CallStaticVoidMethodV,
    jni_GetStaticFieldID,
    jni_GetStaticObjectField,
    jni_GetStaticBooleanField,
    jni_GetStaticByteField,
    jni_GetStaticCharField,
    jni_GetStaticShortField,
    jni_GetStaticIntField,
    jni_GetStaticLongField,
    jni_GetStaticFloatField,
    jni_GetStaticDoubleField,
    jni_SetStaticObjectField,
    jni_SetStaticBooleanField,
    jni_SetStaticByteField,
    jni_SetStaticCharField,
    jni_SetStaticShortField,
    jni_SetStaticIntField,
    jni_SetStaticLongField,
    jni_SetStaticFloatField,
    jni_SetStaticDoubleField,
    jni_NewString,
    jni_GetStringLength,
    jni_GetStringChars,
    jni_ReleaseStringChars,
    jni_NewStringUTF,
    jni_GetStringUTFLength,
    jni_GetStringUTFChars,
    jni_ReleaseStringUTFChars,
    jni_GetArrayLength,
    jni_NewObjectArray,
    jni_GetObjectArrayElement,
    jni_SetObjectArrayElement,
    jni_NewBooleanArray,
    jni_NewByteArray,
    jni_NewCharArray,
    jni_NewShortArray,
    jni_NewIntArray,
    jni_NewLongArray,
    jni_NewFloatArray,
    jni_NewDoubleArray,
    jni_GetBooleanArrayElements,
    jni_GetByteArrayElements,
    jni_GetCharArrayElements,
    jni_GetShortArrayElements,
    jni_GetIntArrayElements,
    jni_GetLongArrayElements,
    jni_GetFloatArrayElements,
    jni_GetDoubleArrayElements,
    jni_ReleaseBooleanArrayElements,
    jni_ReleaseByteArrayElements,
    jni_ReleaseCharArrayElements,
    jni_ReleaseShortArrayElements,
    jni_ReleaseIntArrayElements,
    jni_ReleaseLongArrayElements,
    jni_ReleaseFloatArrayElements,
    jni_ReleaseDoubleArrayElements,
    jni_GetBooleanArrayRegion,
    jni_GetByteArrayRegion,
    jni_GetCharArrayRegion,
    jni_GetShortArrayRegion,
    jni_GetIntArrayRegion,
    jni_GetLongArrayRegion,
    jni_GetFloatArrayRegion,
    jni_GetDoubleArrayRegion,
    jni_SetBooleanArrayRegion,
    jni_SetByteArrayRegion,
    jni_SetCharArrayRegion,
    jni_SetShortArrayRegion,
    jni_SetIntArrayRegion,
    jni_SetLongArrayRegion,
    jni_SetFloatArrayRegion,
    jni_SetDoubleArrayRegion,
    jni_RegisterNatives,
    jni_UnregisterNatives,
    jni_MonitorEnter,
    jni_MonitorExit,
    jni_GetJavaVM,
    jni_GetStringRegion,
    jni_GetStringUTFRegion,
    jni_GetPrimitiveArrayCritical,
    jni_ReleasePrimitiveArrayCritical,
    jni_GetStringCritical,
    jni_ReleaseStringCritical,
    jni_NewWeakGlobalRef,
    jni_DeleteWeakGlobalRef,
    jni_ExceptionCheck,
    jni_NewDirectByteBuffer,
    jni_GetDirectBufferAddress,
    jni_GetDirectBufferCapacity
};

#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
fivmr_JNILib *fivmr_JNI_libraries;
fivmr_Lock fivmr_JNI_libLock;
#endif

/* FIXME: maybe this should be run IN_NATIVE? */
void *fivmr_JNI_lookup(fivmr_ThreadState *ts,
                       fivmr_MethodRec *mr) {
#if !FIVMR_STATIC_JNI && FIVMR_DYN_LOADING
    /* FIXME: synchronization?  probably doesn't matter too much for now...  but
       you could get a weird situation if you have multiple threads doing resolution
       and library loading. */
    void *result;
    LOG(3,("Performing lookup of native method %s.",
	   fivmr_MethodRec_describe(mr)));
    fivmr_assert((mr->flags&FIVMR_MBF_METHOD_IMPL)==FIVMR_MBF_JNI);
    
    result=fivmr_JNILib_lookup(ts,fivmr_TypeData_getContext(mr->owner)->jniLibraries,mr);
    
    if (!(mr->flags&FIVMR_MBF_DYNAMIC)) {
        mr->codePtr=result;
    } /* else in the dynamic case we have no need to do caching since we are
         generating the trampolines on the fly */
    
    if (result!=NULL) {
	LOG(1,("Lookup of %s successful: %p",
	       fivmr_MethodRec_describe(mr),result));
	if ((ts->vm->flags&FIVMR_VMF_JNI_COVERAGE)) {
	    fivmr_Log_lockedPrintf("fivmr JNI coverage: %s\n",
				   fivmr_MethodRec_describe(mr));
	}
    } else {
	/* NOTE: at this point we may have an exception pending! */
	LOG(1,("Lookup of %s failed.",
	       fivmr_MethodRec_describe(mr)));
    }
    return result;
#else
    fivmr_assert(false);
#endif
}

bool fivmr_JNI_runOnLoad(fivmr_ThreadState *ts,
                         fivmr_TypeContext *ctx,
                         void *onLoadHook) {
    ((jint (*)(JavaVM *,void *))onLoadHook)((JavaVM*) &ctx->jniVM,NULL);
    return ts->curExceptionHandle==NULL;
}

bool fivmr_JNI_loadLibrary(fivmr_ThreadState *ts,
                           fivmr_TypeContext *ctx,
                           const char *filename) {
#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
    fivmr_JNILib *lib;
    bool freshlyLoaded;
    if (!fivmr_Lock_legalToAcquire(&ctx->jniLibLock)) {
	/* interrupt handlers, etc, cannot acquire this lock. */
	return false;
    }
    LOG(2,("Attempting to load library %s.",filename));
    fivmr_Lock_lock(&ctx->jniLibLock);
    fivmr_JNILib_load(ts,
                      &ctx->jniLibraries,
		      filename,
		      &lib,
		      &freshlyLoaded);
    fivmr_Lock_unlock(&ctx->jniLibLock);
    
    /* FIXME: the sync here is begging for priority inversion. */
    
    if (freshlyLoaded) {
	LOG(1,("Successfully loaded native library %s.",filename));
	if (lib->onLoadHook!=NULL) {
	    /* FIXME: do something with the version number. */
	    LOG(2,("Calling on-load hook for %s.",filename));
	    if (fivmr_JNI_runOnLoad(ts,ctx,lib->onLoadHook)) {
		LOG(2,("Returned from on-load hook for %s, library is ready.",
                       filename));
	    } else {
		LOG(2,("On-load hook for %s threw exception.",filename));
		return false;
	    }
	} else {
            LOG(2,("No on-load hook for %s, library is ready.",filename));
        }
        fivmr_Lock_lock(&ctx->jniLibLock);
        lib->initialized=true; /* now the library can be used. */
        fivmr_Lock_broadcast(&ctx->jniLibLock);
        fivmr_Lock_unlock(&ctx->jniLibLock);
    } else if (lib!=NULL) {
	LOG(2,("%s was already previously loaded.",filename));
	fivmr_Lock_lock(&ctx->jniLibLock);
	while (!lib->initialized) {
	    LOG(2,("waiting on %s to be initialized.",filename));
	    fivmr_Lock_wait(&ctx->jniLibLock);
	}
	fivmr_Lock_unlock(&ctx->jniLibLock);
    } else {
	LOG(2,("Failed to load %s.",filename));
    }
    return lib!=NULL;
#else
    fivmr_assert(false);
#endif
}

void fivmr_JNI_init(fivmr_TypeContext *ctx,
                    fivmr_VM *vm) {
#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
    fivmr_Lock_init(&ctx->jniLibLock,fivmr_Priority_bound(FIVMR_PR_MAX,
                                                          vm->maxPriority));
#endif
    ctx->jniVM.ctx=ctx;
    ctx->jniVM.functions=&jni_invoke_functions;
}

void fivmr_JNI_destroy(fivmr_TypeContext *ctx) {
#if FIVMR_DYN_LOADING && !FIVMR_STATIC_JNI
    fivmr_Lock_destroy(&ctx->jniLibLock);
#endif
    
    /* FIXME delete more stuff, if JNI loading actually occurred */
}


# 1 "fivmr_jnilib.c"
/*
 * fivmr_jnilib.c
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

#include <fivmr_config.h>
#if !FIVMR_STATIC_JNI && FIVMR_DYN_LOADING

#include "fivmr.h"

#define SHORTNAME_MAX_LEN 128
#define LONGNAME_MAX_LEN 192

void fivmr_JNILib_load(fivmr_ThreadState *ts,
                       fivmr_JNILib **list,
		       const char *filename,
		       fivmr_JNILib **result,
		       bool *freshlyLoaded) {
    fivmr_ModuleHandle module=fivmr_Module_zero();
    fivmr_JNILib *cur;
    void *onLoadHook=NULL;
    
    /* check if it was already loaded */
    for (cur=*list;cur!=NULL;cur=cur->next) {
	if (!strcmp(cur->filename,filename)) {
	    *result=cur;
	    *freshlyLoaded=false;
	    return;
	}
    }
    
    fivmr_Lock_lock(&fivmr_Module_lock);
    module=fivmr_Module_load(filename);
    if (module!=NULL) {
	onLoadHook=fivmr_Module_getFunction(module,"JNI_OnLoad");
    }
    fivmr_Lock_unlock(&fivmr_Module_lock);
    
    if (module==NULL) {
	*result=NULL;
	*freshlyLoaded=false;
	return;
    }
    
    cur=malloc(sizeof(fivmr_JNILib));
    
    cur->filename=strdup(filename);
    cur->module=module;
    cur->onLoadHook=onLoadHook;
    cur->initialized=false;

    cur->next=*list;
    *list=cur;
    
    *result=cur;
    *freshlyLoaded=true;
}

static void *lookupImpl(fivmr_JNILib *lib,
			const char *shortName,
			const char *longName) {
    void *result;
    LOG(7,("doing lookup of %s and %s in %s",shortName,longName,lib->filename));
    result=fivmr_Module_getFunction(lib->module,shortName);
    if (result==NULL) {
	result=fivmr_Module_getFunction(lib->module,longName);
    }
    return result;
}

void *fivmr_JNILib_lookupOne(fivmr_ThreadState *ts,
                             fivmr_JNILib *lib,
			     fivmr_MethodRec *mr) {
    char shortName[SHORTNAME_MAX_LEN];
    char longName[LONGNAME_MAX_LEN];
    void *result=NULL;
    if (lib->initialized) {
	fivmr_makeJNIFuncName(ts,shortName,sizeof(shortName),mr,false);
	if (fivmr_hasException(ts)) return NULL;
	fivmr_makeJNIFuncName(ts,longName,sizeof(longName),mr,true);
	if (fivmr_hasException(ts)) return NULL;
	LOG(3,("Doing lookupOne of %s/%s in %s",shortName,longName,lib->filename));
	fivmr_Lock_lock(&fivmr_Module_lock);
	result=lookupImpl(lib,shortName,longName);
	fivmr_Lock_unlock(&fivmr_Module_lock);
    }
    return result;
}

void *fivmr_JNILib_lookup(fivmr_ThreadState *ts,
                          fivmr_JNILib *lib,
			  fivmr_MethodRec *mr) {
    char shortName[SHORTNAME_MAX_LEN];
    char longName[LONGNAME_MAX_LEN];
    void *result=NULL;
    fivmr_makeJNIFuncName(ts,shortName,sizeof(shortName),mr,false);
    if (fivmr_hasException(ts)) return NULL;
    fivmr_makeJNIFuncName(ts,longName,sizeof(longName),mr,true);
    if (fivmr_hasException(ts)) return NULL;
    LOG(3,("Doing lookup of %s/%s in %s",shortName,longName,lib->filename));
    fivmr_Lock_lock(&fivmr_Module_lock);
    for (;lib!=NULL;lib=lib->next) {
	if (lib->initialized) {
	    result=lookupImpl(lib,shortName,longName);
	    if (result!=NULL) break;
	}
    }
    fivmr_Lock_unlock(&fivmr_Module_lock);
    return result;
}

#endif

# 1 "fivmr_log.c"
/*
 * fivmr_log.c
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

void fivmr_Log_printf(const char *msg,...) {
    va_list lst;
    va_start(lst,msg);
    fivmr_Log_vprintf(msg,lst);
    va_end(lst);
}

void fivmr_Log_print(const char *msg) {
    fivmr_Log_printf("%s",msg);
}

void fivmr_Log_lockedPrintf(const char *msg,...) {
    va_list lst;
    fivmr_Log_lock();
    va_start(lst,msg);
    fivmr_Log_vprintf(msg,lst);
    va_end(lst);
    fivmr_Log_unlock();
}

void fivmr_Log_lockedPrint(const char *msg) {
    fivmr_Log_lockedPrintf("%s",msg);
}
# 1 "fivmr_logger.c"
/*
 * logger.c
 * by Filip Pizlo, 2007
 */

#include "fivmr.h"
#include "fivmr_logger.h"
#include <stdarg.h>
#include <stdio.h>

#define NUM_EVENTS 1000000

#define MAX_NUM_EVENTS (NUM_EVENTS*10)

struct Latch_s {
    intptr_t latch;
    intptr_t padding[64];
};

struct Event_s {
    intptr_t timestamp;
    const char *format;
    unsigned num_args;
    intptr_t args[10];
};

typedef struct Event_s Event;
typedef struct Latch_s Latch;

static Latch latch[FIVMR_LOGGER_MAX_THREADS];
static Event *events[FIVMR_LOGGER_MAX_THREADS];
static uintptr_t num_events[FIVMR_LOGGER_MAX_THREADS];
static uintptr_t events_cap[FIVMR_LOGGER_MAX_THREADS];

static intptr_t counter;

void fp_log(intptr_t thread_id,
	    const char *format,
	    unsigned num_args,
	    ...) {
    unsigned i;
    va_list lst;
    
    while (!fivmr_cas((uintptr_t*)&latch[thread_id].latch,0,1)) ;
    
    if (num_events[thread_id]==MAX_NUM_EVENTS) {
	latch[thread_id].latch=0;
	fprintf(stderr,"logger: aborting because of too many events.\n");
	fp_commit(stderr);
	abort();
    }
    
    if (num_events[thread_id]==events_cap[thread_id]) {
	if (events[thread_id]==NULL) {
	    events[thread_id]=
		malloc(sizeof(Event)*NUM_EVENTS);
	} else {
	    events[thread_id]=
		realloc(events[thread_id],
			sizeof(Event)*(num_events[thread_id]+NUM_EVENTS));
	}
	events_cap[thread_id]+=NUM_EVENTS;
    }
    
    events[thread_id][num_events[thread_id]].timestamp=fivmr_xchg_add((uintptr_t*)&counter,1);
    events[thread_id][num_events[thread_id]].format=format;
    events[thread_id][num_events[thread_id]].num_args=num_args;
    va_start(lst,num_args);
    for (i=0;i<num_args;++i) {
	events[thread_id][num_events[thread_id]].args[i]=va_arg(lst,intptr_t);
    }
    va_end(lst);
    num_events[thread_id]++;
    latch[thread_id].latch=0;
}

void fp_commit(FILE *out) {
    unsigned i;
    uintptr_t j;
    unsigned k;
    for (i=0;i<FIVMR_LOGGER_MAX_THREADS;++i) {
	while (!fivmr_cas((uintptr_t*)&latch[i].latch,0,1)) ;
    }
    for (i=0;i<FIVMR_LOGGER_MAX_THREADS;++i) {
	if (events[i]!=NULL) {
	    fprintf(out,"Thread #%u:\n",i);
	    for (j=0;j<num_events[i];++j) {
		Event *e=events[i]+j;
		unsigned arg=0;
		fprintf(out,"\tTs.%" PRIdPTR ": ",e->timestamp);
		for (k=0;e->format[k];++k) {
		    if (e->format[k]=='%') {
			++k;
			if (e->format[k]=='i') {
			    fprintf(out,"%" PRIdPTR,e->args[arg++]);
			} else if (e->format[k]=='u') {
			    fprintf(out,"%" PRIuPTR,e->args[arg++]);
			} else {
			    fputc(e->format[k],out);
			}
		    } else {
			fputc(e->format[k],out);
		    }
		}
		fprintf(out,"\n");
	    }
	}
    }
}


# 1 "fivmr_machinecode.c"
/*
 * fivmr_machinecode.c
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

fivmr_MachineCode *fivmr_MachineCode_create(int32_t size,
                                            fivmr_MachineCodeFlags flags) {
    fivmr_MachineCode *result;
    
    result=freg_region_create_with_steps(sizeof(fivmr_MachineCode),
                                         fivmr_max(
                                             fivmr_min(256,size),
                                             32));
    if (result==NULL) {
        return NULL;
    }
    
    fivmr_assert((flags&FIVMR_MC_GC_OWNED)==0);
    fivmr_assert((flags&FIVMR_MC_GC_MARKED)==0);
    
    bzero(result,sizeof(fivmr_MachineCode));
    
    result->refCount=1;
    result->size=size;
    result->next=NULL;
    result->flags=flags;
    
    result->code=fivmr_allocExecutable(size);
    if (result->code==NULL) {
        fivmr_free(result);
        return NULL;
    }
    
    return result;
}

void fivmr_MachineCode_downsize(fivmr_MachineCode *code,
                                int32_t newSize) {
    if (fivmr_supportDownsizeExec()) {
        fivmr_downsizeExecutable(code->code,newSize);
        code->size=newSize;
    }
}

fivmr_MachineCode *fivmr_MachineCode_up(fivmr_MachineCode *mc) {
    int32_t result=fivmr_xchg_add32(&mc->refCount,1);
    fivmr_assert(result>0);
    return mc;
}

void fivmr_MachineCode_down(fivmr_MachineCode *mc) {
    int32_t result=fivmr_xchg_add32(&mc->refCount,-1);
    fivmr_assert(result>0);
    if (result==1) {
        fivmr_MachineCode *subMC;
        for (subMC=mc->sub;subMC!=NULL;) {
            fivmr_MachineCode *next=subMC->next;
            fivmr_MachineCode_down(subMC);
            subMC=next;
        }
        fivmr_freeExecutable(mc->code);
        freg_region_free(mc);
    }
}

void fivmr_MachineCode_registerMC(fivmr_MachineCode *parent,
                                  fivmr_MachineCode *child) {
    fivmr_VM *vm=fivmr_MachineCode_getVM(parent);
    fivmr_assert(vm==fivmr_MachineCode_getVM(child));
    fivmr_MachineCode_up(child);
    fivmr_Lock_lock(&vm->typeDataLock);
    child->next=parent->sub;
    parent->sub=child;
    fivmr_Lock_unlock(&vm->typeDataLock);
}

void fivmr_MachineCode_appendBasepoint(fivmr_MachineCode *code,
                                       int32_t bytecodePC,
                                       int32_t stackHeight,
                                       void *machinecodePC) {
    fivmr_Basepoint *bp;
    
    LOG(1,("Adding basepoint to %p: bytecodePC = %d, stackHeight = %d, machinecodePC = %p",
           code,bytecodePC,stackHeight,machinecodePC));
    
    bp=freg_region_alloc(code,sizeof(fivmr_Basepoint));
    fivmr_assert(bp!=NULL);
    
    bp->bytecodePC=bytecodePC;
    bp->stackHeight=stackHeight;
    bp->machinecodePC=machinecodePC;
    bp->next=code->bpList;
    
    code->bpList=bp;
}

void fivmr_MachineCode_appendBaseTryCatch(fivmr_MachineCode *code,
                                          int32_t start,
                                          int32_t end,
                                          int32_t target,
                                          fivmr_TypeStub *type) {
    fivmr_BaseTryCatch *btc=freg_region_alloc(code,sizeof(fivmr_BaseTryCatch));
    fivmr_assert(btc!=NULL);
    
    btc->start=start;
    btc->end=end;
    btc->target=target;
    btc->type=type;
    
    btc->next=NULL;
    
    fivmr_assert((code->btcFirst==NULL)==(code->btcLast==NULL));
    
    if (code->btcFirst==NULL) {
        code->btcFirst=btc;
        code->btcLast=btc;
    } else {
        code->btcLast->next=btc;
        code->btcLast=btc;
    }
}


# 1 "fivmr_memoryareas.c"
#include "fivmr.h"

void fivmr_MemoryAreas_init(fivmr_GC *gc) {
    gc->immortalMemoryArea.scopeID=
        ((uintptr_t)&gc->immortalMemoryArea>>2)|FIVMR_GC_MARKBITS_MASK;
    gc->immortalMemoryArea.scopeID_s.word=
        (uintptr_t)-1;
    gc->immortalMemoryArea.shared=true;
    if (gc->immortalMem) {
        void *mem=fivmr_malloc(gc->immortalMem);
        gc->immortalMemoryArea.bump=gc->immortalMemoryArea.start=
            (uintptr_t)mem+FIVMR_ALLOC_OFFSET(&fivmr_VMfromGC(gc)->settings);
        gc->immortalMemoryArea.size=gc->immortalMem;
    }
    if (FIVMR_RTSJ_SCOPES(&fivmr_VMfromGC(gc)->settings)) {
        gc->heapMemoryArea.scopeID_s.word=
            (uintptr_t)-1;
        fivmr_Lock_init(&gc->sharedAreasLock, FIVMR_PR_INHERIT);
    }
}

static int fivmr_MemoryArea_depth(fivmr_MemoryArea *area) {
    fivmr_MemoryArea *cur;
    int i;
    for (cur=area->parent, i=1; cur; cur=cur->parent, i++) {
        // empty
    }

    return i;
}

static void doSetArea(fivmr_ThreadState *ts,
                      fivmr_MemoryArea *prevArea,
                      fivmr_MemoryArea *newArea)
{
    ts->gc.currentArea=newArea;
    fivmr_assert(newArea);
    if (newArea==prevArea)
        return;
    if (newArea!=&ts->gc.heapMemoryArea)
        fivmr_assert(newArea->scopeID);
    if (!prevArea->shared) {
        prevArea->bump=ts->gc.alloc[0].bump;
        prevArea->start=ts->gc.alloc[0].start;
        prevArea->size=ts->gc.alloc[0].size;
    }
    ts->gc.alloc[0].bump=newArea->bump;
    ts->gc.alloc[0].start=newArea->start;
    ts->gc.alloc[0].size=newArea->size;
}

static fivmr_MemoryArea *normalizeArea(fivmr_ThreadState *ts,
                                       fivmr_MemoryArea *area) {
    /* Take care of the diverted heap area */
    if (area == &ts->vm->gc.heapMemoryArea) {
        return &ts->gc.heapMemoryArea;
    } else {
        return area;
    }
}

static fivmr_MemoryArea *normalizeParentage(fivmr_ThreadState *ts,
                                            fivmr_MemoryArea *area)
{
    if (area->parent==NULL) {
        return NULL;
    } else if (area->parent==&ts->gc.heapMemoryArea) {
        return &ts->vm->gc.immortalMemoryArea;
    } else {
        return area->parent;
    }
}

uintptr_t fivmr_MemoryArea_alloc(fivmr_ThreadState *ts, int64_t size,
                                 int32_t shared, fivmr_Object name)
{
    fivmr_MemoryAreaStack *ms;
    fivmr_MemoryArea *area;
    fivmr_TypeData *td;
    uintptr_t newtop;
    int64_t totalsize;

    size = (size+FIVMSYS_PTRSIZE-1)&(~(uintptr_t)(FIVMSYS_PTRSIZE-1));
    totalsize = size + sizeof(fivmr_MemoryAreaStack) + sizeof(fivmr_MemoryArea);

    if (FIVMR_RTSJ_SCOPES(&ts->vm->settings)) {
        ms=fivmr_calloc(1,totalsize);
        fivmr_assert(ms!=NULL);
        area=(fivmr_MemoryArea *)(ms+1);
        area->size=size;
        area->shared=shared?1:0;
        area->start=area->bump=(uintptr_t)(area+1)
            +FIVMR_ALLOC_OFFSET(&ts->vm->settings);
        return (uintptr_t)area;
    }

    if (!ts->gc.scopeBacking) {
        LOG(3,("Thread does not have a backing store."));
        return 0;
    }
    /* NB: This must point to the fivmr_MemoryArea created below */
    fivmr_FlowLog_log_fat(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                          FIVMR_FLOWLOG_SUBTYPE_ALLOC_SCOPE, totalsize,
                          ts->gc.scopeBacking->top
                          + sizeof(fivmr_MemoryAreaStack));
    newtop = ts->gc.scopeBacking->top + totalsize;
    if (FIVMR_SCJ(&ts->vm->settings)) {
        /* This should really be memoized somewhere */
        td=fivmr_StaticTypeContext_find(&ts->vm->baseContexts[0]->st,
                                        "Ledu/purdue/scj/BackingStoreID;");
        fivmr_assert(td);
        newtop += td->size;
    }
    if (newtop - (uintptr_t)&ts->gc.scopeBacking->start
        > ts->gc.scopeBacking->size) {
        LOG(3,("Backing store size exhausted."));
        return 0;
    }
    ms = (fivmr_MemoryAreaStack *)ts->gc.scopeBacking->top;
    area = (fivmr_MemoryArea *)(ms + 1);
    bzero((void *)ts->gc.scopeBacking->top,newtop-ts->gc.scopeBacking->top);
    ts->gc.scopeBacking->top = newtop;
    area->size=size;
    area->shared=shared?1:0;
    area->bump=area->start=(uintptr_t)(area+1)
        +FIVMR_ALLOC_OFFSET(&ts->vm->settings);
    area->top=ts->gc.scopeBacking->top;
    if (FIVMR_SCJ(&ts->vm->settings)) {
        fivmr_MemoryArea *curArea=ts->gc.currentArea;
        area->scopeID=(uintptr_t)-1;
        doSetArea(ts,ts->gc.currentArea,area);
        area->javaArea=fivmr_BackingStoreID_create(ts,name);
        doSetArea(ts,area,curArea);
        area->scopeID=0;
        /* Fix up the start to push the BackingStoreID "out" of the area */
        area->start=area->bump;
    }
    return (uintptr_t)area;
}

void fivmr_MemoryArea_free(fivmr_ThreadState *ts, fivmr_MemoryArea *area) {
    fivmr_MemoryAreaStack *ms=((fivmr_MemoryAreaStack *)area)-1;
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_FREE_SCOPE, (uintptr_t)area);
    if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
        fivmr_assert(ts->gc.scopeBacking->top==ms->area->top);
        ts->gc.scopeBacking->top=(uintptr_t)ms;
    } else if (FIVMR_RTSJ_SCOPES(&ts->vm->settings)) {
        fivmr_free(ms);
    }

}

/* This code is reentrant when called for differing areas
 * simultaneously, but the calling code must ensure that it is never
 * called more than once for the *same area* at the same time. */
void fivmr_MemoryArea_push(fivmr_ThreadState *ts,
                           fivmr_MemoryArea *area)
{
    fivmr_MemoryAreaStack *ms=((fivmr_MemoryAreaStack *)area)-1;
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_PUSH, (uintptr_t)area);
    if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
        /* Enforce a linear stack -- no cactus stacks! */
        fivmr_assert(ts->gc.scopeStack->area==ts->gc.currentArea);
        fivmr_assert(!area->parent);
        ms->area=area;
        ms->prev=ts->gc.scopeStack;
        ts->gc.scopeStack=ms;
        area->parent=ts->gc.currentArea;
    } else if (FIVMR_RTSJ_SCOPES(&ts->vm->settings)) {
        fivmr_MemoryArea *parent=normalizeParentage(ts, area);
        fivmr_MemoryArea *cur=normalizeArea(ts, ts->gc.currentArea);
        fivmr_assert(!parent);
        ms->area=area;
        ms->next=NULL;
        area->parent=cur;
        area->scopeID_s.word=((((uintptr_t)-1)-fivmr_MemoryArea_depth(area))<<1)&~FIVMR_GC_MARKBITS_MASK|FIVMR_SCOPEID_SCOPE;
        area->scopeID=((uintptr_t)&area->scopeID_s)>>2;
        if (!FIVMR_NOGC(&ts->vm->settings)&&area->shared) {
            fivmr_Lock_lock(&ts->vm->gc.sharedAreasLock);
            while (ms->flags&FIVMR_MEMORYAREASTACK_GCINPROGRESS) {
                ms->flags|=FIVMR_MEMORYAREASTACK_WAITING;
                fivmr_Lock_wait(&ts->vm->gc.sharedAreasLock);
            }
            ms->flags&=~FIVMR_MEMORYAREASTACK_WAITING;
            ms->prev=ts->vm->gc.sharedAreas;
            if (ts->vm->gc.sharedAreas) {
                ts->vm->gc.sharedAreas->next=ms;
            }
            ts->vm->gc.sharedAreas=ms;
            fivmr_Lock_unlock(&ts->vm->gc.sharedAreasLock);
        } else if (!FIVMR_NOGC(&ts->vm->settings)) {
            /* Private scope */
            ms->prev=ts->gc.scopeStack;
            ts->gc.scopeStack->next=ms;
            ts->gc.scopeStack=ms;
        }
    }
}

/* This has the same reentrancy requirements as _push.  Additionally,
 * it frees the area for SCJ, but not RTSJ-style scopes. */
void fivmr_MemoryArea_pop(fivmr_ThreadState *ts, fivmr_VM *vm,
                          fivmr_MemoryArea *area)
{
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_POP, (uintptr_t)area);
    if(FIVMR_SCJ_SCOPES(&vm->settings)) {
        fivmr_MemoryAreaStack *ms=ts->gc.scopeStack;
        fivmr_assert(ms);
        if (area==NULL) {
            area=ms->area;
        } else {
            fivmr_assert(ms->area==area);
        }
        fivmr_assert(ms!=&ts->gc.baseStackEntry);
        fivmr_assert(ms->area->scopeID==0);
        ts->gc.scopeStack=ms->prev;
        area->parent=NULL;
    } else if (FIVMR_RTSJ_SCOPES(&vm->settings)) {
        // FIXME: finalizers
        fivmr_MemoryAreaStack *ms=((fivmr_MemoryAreaStack *)area)-1;
        if (ts) {
            fivmr_assert(area!=&ts->gc.heapMemoryArea&&
                         area!=&vm->gc.immortalMemoryArea);
            fivmr_assert(area!=ts->gc.currentArea);
        }
        /* If this first stanza is executed, it might be executed
         * with !ts.  In that case, the lock is already locked,
         * and we know the area is shared... */
        if (!FIVMR_NOGC(&vm->settings)&&area->shared) {
            fivmr_Lock_lock(&vm->gc.sharedAreasLock);
            if (ms->flags&FIVMR_MEMORYAREASTACK_GCINPROGRESS) {
                ms->flags|=FIVMR_MEMORYAREASTACK_POP;
            } else {
                if (ms->prev) {
                    ms->prev->next=ms->next;
                } else {
                    vm->gc.sharedAreas=ms->next;
                }
                if (ms->next) {
                    ms->next->prev=ms->prev;
                }
                area->bump=area->start;
                area->objList=0;
            }
            fivmr_Lock_unlock(&vm->gc.sharedAreasLock);
        } else if (!FIVMR_NOGC(&vm->settings)) {
            /* Private area */
            ms->prev->next=ms->next;
            if (ms->next) {
                ms->next->prev=ms->prev;
            } else {
                ts->gc.scopeStack=ms->prev;
            }
        }
    }
}

void fivmr_MemoryArea_enter(fivmr_ThreadState *ts, fivmr_MemoryArea *area,
                            fivmr_Object logic)
{
    fivmr_ScopeID scope={ (uintptr_t)area | FIVMR_SCOPEID_SCOPE };
    fivmr_MemoryArea *prevArea=ts->gc.currentArea;
    if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
        if (!area->parent) {
            fivmr_MemoryArea_push(ts,area);
        }
        fivmr_assert(ts->gc.currentArea==area->parent);
        /* Should be implied by the above */
        fivmr_assert(area->scopeID==0);
        area->scopeID=((uintptr_t)&scope)>>2;
    }
    LOG(3, ("Entering memory area %p in thread %d", area, ts->id));
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_ENTER, (uintptr_t)area);
    doSetArea(ts,prevArea,area);
    fivmr_fence();
    fivmr_MemoryArea_doRun(ts, (uintptr_t)area, logic);
    fivmr_fence();
    LOG(3, ("Exiting memory area %p in thread %d", area, ts->id));
    if (ts->gc.currentArea==area) {
        fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                          FIVMR_FLOWLOG_SUBTYPE_ENTER, (uintptr_t)prevArea);
        doSetArea(ts,area,prevArea);
    }
    if (FIVMR_SCJ_SCOPES(&ts->vm->settings)) {
        area->scopeID=0;
        area->bump=area->start;
    }
}

uintptr_t fivmr_MemoryArea_setCurrentArea(fivmr_ThreadState *ts,
                                          fivmr_MemoryArea *area)
{
    uintptr_t prev;
    fivmr_assert(area);
    area=normalizeArea(ts,area);
    fivmr_assert(area==&ts->gc.heapMemoryArea||area->scopeID);
    prev=(uintptr_t)ts->gc.currentArea;
    LOG(3, ("Setting current memory area to %p in thread %d", area, ts->id));
    doSetArea(ts,ts->gc.currentArea,area);
    return prev;
}

void fivmr_ScopeBacking_alloc(fivmr_ThreadState *ts, uintptr_t size)
{
    fivmr_assert(ts->gc.scopeBacking==NULL);
    fivmr_assert(size);
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_ALLOC_BACKING, size);
    ts->gc.scopeBacking=fivmr_malloc(sizeof(fivmr_ScopeBacking)+size);
    ts->gc.scopeBacking->size=size;
    ts->gc.scopeBacking->top=(uintptr_t)&ts->gc.scopeBacking->start;
}

void fivmr_ScopeBacking_free(fivmr_ThreadState *ts)
{
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                      FIVMR_FLOWLOG_SUBTYPE_FREE_BACKING, 0);
    fivmr_assert(ts->gc.scopeBacking!=NULL);
    fivmr_assert(ts->gc.scopeStack==&ts->gc.baseStackEntry);
    fivmr_free(ts->gc.scopeBacking);
    ts->gc.scopeBacking=NULL;
}

int64_t fivmr_MemoryArea_consumed(fivmr_ThreadState *ts,
                                  fivmr_MemoryArea *area) {
    if (area==&ts->gc.heapMemoryArea) {
        int64_t result=fivmr_GC_totalMemory(&ts->vm->gc);
        LOG(3,("Requested consumed memory for heap memory (%p) in Thread #%u, "
               "got %p",area,ts->id,(uintptr_t)result));
        return result;
    } else if (area==ts->gc.currentArea &&
               !area->shared) {
        uintptr_t result=
            ts->gc.alloc[FIVMR_GC_OBJ_SPACE].bump
            - ts->gc.alloc[FIVMR_GC_OBJ_SPACE].start;
        LOG(3,("Requested consumed memory for private current memory "
               "area (%p) in Thread #%u, got %p",area,ts->id,(uintptr_t)result));
        return result;
    } else {
        uintptr_t result=area->bump-area->start;
        LOG(3,("Requested consumed memory for shared or non-current "
               "memory area (%p) in Thread #%u, got %p",
               area,ts->id,(uintptr_t)result));
        return result;
    }
}


# 1 "fivmr_methodrec.c"
/*
 * fivmr_methodrec.c
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

#include "fivmr.h"

fivmr_MethodRec *fivmr_MethodRec_reresolveSpecial(fivmr_ThreadState *ts,
                                                  fivmr_TypeData *from,
                                                  fivmr_MethodRec *target) {
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    if ((from->flags&FIVMR_TBF_NEW_SUPER_MODE) &&
        fivmr_TypeData_isSubtypeOf(ts,from,target->owner) &&
        from!=target->owner &&
        !fivmr_MethodRec_isInitializer(target)) {
        
        fivmr_assert(from->parent!=NULL);
        fivmr_assert(from->parent!=ts->vm->payload->td_top);
        
        target=fivmr_TypeData_findInstMethodNoIface3(
            ts->vm,from->parent,
            target->name,target->result,target->nparams,target->params);
        
        fivmr_assert(target!=NULL);
    }
    
    return target;
}

void *fivmr_MethodRec_staticDispatch(fivmr_ThreadState *ts,
                                     fivmr_MethodRec *mr,
				     fivmr_TypeData *td) {
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    if (!fivmr_MethodRec_exists(mr)) {
        /* FIXME
           this corresponds to the method not yet having been compiled.  if so
           then we should, like, do that, like, now, or something. */
        
        LOG(3,("method does not exist"));
    
        return NULL;
    }
    
    if (!strcmp(mr->name,"<init>") ||
	(mr->flags&FIVMR_MBF_METHOD_KIND)==FIVMR_MBF_FINAL ||
	(mr->owner->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
	(mr->flags&FIVMR_BF_STATIC)) {
        LOG(3,("returning entrypoint %p",mr->entrypoint));
        return mr->entrypoint;
    } else if ((mr->owner->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION ||
	       (mr->owner->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE) {
        LOG(3,("performing epoch-based itable lookup"));
	return td->epochs[ts->typeEpoch].itable[fivmr_MethodRec_itableIndex(ts,mr)];
    } else {
        LOG(3,("performing vtable lookup"));
	return td->vtable[mr->location];
    }
}

void *fivmr_MethodRec_dispatch(fivmr_ThreadState *ts,
                               fivmr_MethodRec *mr,
			       fivmr_Object object) {
    LOG(3,("dispatching method call to %s (%p)",fivmr_MethodRec_describe(mr),mr));
    if ((mr->flags&FIVMR_BF_STATIC)) {
        LOG(3,("method is static."));
        return fivmr_MethodRec_staticDispatch(ts,mr,NULL);
    } else {
        LOG(3,("method is instance."));
        fivmr_assert(object!=0);
        return fivmr_MethodRec_staticDispatch(
            ts, mr, fivmr_TypeData_forObject(&ts->vm->settings, object));
    }
}

fivmr_Value fivmr_MethodRec_call(fivmr_MethodRec *mr,
				 fivmr_ThreadState *ts,
				 void *methodPtr,
				 void *receiver,
				 fivmr_Value *userArgs,
				 fivmr_CallMode cm) {
    uintptr_t numSysArgs=1;
    uintptr_t receiverArg=0;
    uintptr_t spaceArg=0;
    uintptr_t numUserArgs;
    uintptr_t numAllArgs;
    fivmr_Value *allArgs;
    fivmr_Value result;
    bool calledMethod;
    
#if FIVMR_PROFILE_REFLECTION
    fivmr_Nanos before;
    
    before=fivmr_curTime();
#endif
    
    result=fivmr_NullValue();
    calledMethod=false;
    /* LOG(5,("MRC 1")); */
    if ((cm&FIVMR_CM_EXEC_STATUS)) {
	fivmr_ThreadState_goToJava(ts);
    }
    if (!fivmr_MethodRec_exists(mr)) {
        fivmr_throwNoSuchMethodError_inJava(ts,mr);
    } else {
        /* LOG(5,("MRC 2")); */
        if ((cm&FIVMR_CM_NULLCHECK) && !(mr->flags&FIVMR_BF_STATIC) && receiver==NULL) {
            fivmr_throwNullPointerRTE_inJava(ts);
        } else if ((cm&FIVMR_CM_CHECKINIT) && !fivmr_TypeData_checkInit(ts,mr->owner)) {
            /* oops - exception thrown from checkInit, drop down */
            /* LOG(5,("MRC 3")); */
        } else {
            if ((mr->flags&FIVMR_MBF_ALLOC_AS_CALLER)) {
                numSysArgs++;
            }
	
            if (!(mr->flags&FIVMR_BF_STATIC)) {
                receiverArg=numSysArgs;
                numSysArgs++;
            }

            numUserArgs=mr->nparams;
            numAllArgs=numUserArgs+numSysArgs; /* thread state */
            /* LOG(5,("MRC 4")); */
            allArgs=alloca(sizeof(fivmr_Value)*numAllArgs);
            bzero(allArgs,sizeof(fivmr_Value)*numAllArgs);
            allArgs[0].P=(uintptr_t)ts;
            if ((mr->flags&FIVMR_MBF_ALLOC_AS_CALLER)) {
                allArgs[1].I=FIVMR_GC_OBJ_SPACE; /* FIXME */
            }
            if (!(mr->flags&FIVMR_BF_STATIC)) {
                allArgs[receiverArg].P=(uintptr_t)receiver;
            }
            memcpy(allArgs+numSysArgs,userArgs,sizeof(fivmr_Value)*numUserArgs);
            /* LOG(5,("MRC 5")); */
            if ((cm&FIVMR_CM_RETURN_ARGS_BUF) && userArgs!=NULL) {
                fivmr_ThreadState_returnBuffer(ts,(char*)userArgs);
                userArgs=NULL;
            }
            /* LOG(5,("MRC 6")); */
            if ((cm&FIVMR_CM_HANDLES)) {
                int32_t i;
                if (!(mr->flags&FIVMR_BF_STATIC)) {
                    /* LOG(5,("MRC 6-1")); */
                    allArgs[receiverArg].L=fivmr_Handle_get(allArgs[receiverArg].H);
                }
                for (i=0;i<mr->nparams;++i) {
                    if (mr->params[i]->name[0]=='L' ||
                        mr->params[i]->name[0]=='[') {
                        /* LOG(5,("MRC 6-2")); */
                        allArgs[i+numSysArgs].L=
                            fivmr_Handle_get(allArgs[i+numSysArgs].H);
                    }
                }
            }
            /* LOG(5,("MRC 9")); */
            if ((cm&FIVMR_CM_CLASSCHANGE) && !(mr->flags&FIVMR_BF_STATIC) &&
                !fivmr_TypeData_isSubtypeOf(
                    ts,
                    fivmr_TypeData_forObject(&ts->vm->settings,allArgs[receiverArg].L),
                    mr->owner)) {
                /* LOG(5,("MRC 8")); */
                fivmr_throwClassChangeRTE_inJava(ts);
            } else {
                /* LOG(5,("MRC 10")); */
                if ((cm&FIVMR_CM_DISPATCH)) {
                    methodPtr=fivmr_MethodRec_dispatch(ts,mr,allArgs[receiverArg].L);
                }
                /* LOG(5,("MRC 11")); */
                if (methodPtr==NULL) {
                    /* when will this happen?
                       - when we call a method for which HAS_CODE is false
                       ... and when will that happen?
                       - when we try to do a nonvirtual call on an abstract method
                       in an abstract class. */
                    /* LOG(5,("MRC 7")); */
                    fivmr_throwAbstractMethodError_inJava(ts);
                } else {
#if FIVMR_HAVE_NATIVE_BACKEND
                    char *argTypes,*curArg;
                    int32_t i;
                    
                    argTypes=alloca(numAllArgs+1);
                    bzero(argTypes,numAllArgs+1);
                    curArg=argTypes;

                    *curArg++='P';

                    if ((mr->flags&FIVMR_MBF_ALLOC_AS_CALLER)) {
                        *curArg++='P';
                    }
                    
                    if (!(mr->flags&FIVMR_BF_STATIC)) {
                        *curArg++='P';
                    }
                    
                    for (i=0;i<mr->nparams;++i) {
                        *curArg++=fivmr_pointerifyBasetype(mr->params[i]->name[0]);
                    }
                    
                    calledMethod=true;
                    
                    LOG(5,("Calling method with retType = %c, argTypes = %s",
                           fivmr_pointerifyBasetype(mr->result->name[0]),
                           argTypes));
                    
                    result=fivmr_upcall(
                        methodPtr,
                        fivmr_pointerifyBasetype(mr->result->name[0]),
                        argTypes,
                        allArgs);
#else
                    /* LOG(5,("MRC 11-2")); */
                    calledMethod=true;
                    switch (mr->result->name[0]) {
                    case 'Z': 
                    case 'B':
                        /* LOG(5,("MRC 11-B")); */
                        result.B=
                            ((int8_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'C':
                        /* LOG(5,("MRC 11-C")); */
                        result.C=
                            ((uint16_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'S':
                        /* LOG(5,("MRC 11-S")); */
                        result.S=
                            ((int16_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'I':
                        /* LOG(5,("MRC 11-I")); */
                        result.I=
                            ((int32_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'J':
                        /* LOG(5,("MRC 11-J")); */
                        result.J=
                            ((int64_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'F':
                        /* LOG(5,("MRC 11-F")); */
                        result.F=
                            ((float (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'D':
                        /* LOG(5,("MRC 11-D")); */
                        result.D=
                            ((double (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'P':
                    case 'f':
                        /* LOG(5,("MRC 11-P")); */
                        result.P=
                            ((uintptr_t (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'L':
                    case '[':
                        /* LOG(5,("MRC 11-[")); */
                        result.L=
                            ((fivmr_Object (*)(void*,fivmr_Value*))mr->upcallPtr)(
                                methodPtr,allArgs);
                        break;
                    case 'V':
                        LOG(5,("MRC 11-V"));
                        ((void (*)(void*,fivmr_Value*))mr->upcallPtr)(methodPtr,allArgs);
                        LOG(5,("MRC 11-V done")); 
                        break;
                    default:
                        fivmr_assert(!"bad type descriptor");
                        break;
                    }
#endif
                    if ((cm&FIVMR_CM_HANDLES) &&
                        (mr->result->name[0]=='L' ||
                         mr->result->name[0]=='[')) {
                        /* LOG(5,("MRC 11-[ handles")); */
                        result.H=fivmr_ThreadState_addHandle(ts,result.L);
                    }
                }
            }
        }
        /* LOG(5,("MRC 11-return?")); */
        if ((cm&FIVMR_CM_RETURN_ARGS_BUF) && userArgs!=NULL) {
            /* LOG(5,("MRC 11-return yes")); */
            fivmr_ThreadState_returnBuffer(ts,(char*)userArgs);
        }
        /* LOG(5,("MRC 15")); */
        if (ts->curException && calledMethod && (cm&FIVMR_CM_WRAP_EXCEPTION)) {
            fivmr_Object e=ts->curException;
            ts->curException=0;
            /* LOG(5,("MRC 13")); */
            fivmr_throwReflectiveException_inJava(ts,e);
        }
    }
    
    if ((cm&FIVMR_CM_HANDLES)) {
	/* LOG(5,("MRC 14")); */
	fivmr_ThreadState_handlifyException(ts);
    }
    
    if ((cm&FIVMR_CM_EXEC_STATUS)) {
	fivmr_ThreadState_goToNative(ts);
    }

#if FIVMR_PROFILE_REFLECTION
    fivmr_PR_invokeTime+=fivmr_curTime()-before;
#endif
    
    return result;
}

bool fivmr_MethodRec_matchesSig(fivmr_MethodRec *mr,
				const char *sig) {
    int pi,ci;
    if (sig[0]!='(') {
	return false;
    }
    pi=0;
    ci=1;
    while (pi<mr->nparams) {
	int n=strlen(mr->params[pi]->name);
	if (strncmp(mr->params[pi]->name,
		    sig+ci,
		    n)) {
	    return false;
	}
	pi++;
	ci+=n;
    }
    if (sig[ci++]!=')') {
	return false;
    }
    return !strcmp(sig+ci,mr->result->name);
}

const char *fivmr_MethodRec_describe(fivmr_MethodRec *mr) {
    if (mr==NULL) {
	return "(null mr)";
    } else {
	size_t size=1; /* for null char */
	int32_t i;
	char *result;
	size+=strlen(mr->owner->name);
	size++; /* slash */
	size+=strlen(mr->name);
	size++; /* lparen */
	for (i=0;i<mr->nparams;++i) {
	    size+=strlen(mr->params[i]->name);
	}
	size++; /* rparen */
	size+=strlen(mr->result->name);
	result=fivmr_threadStringBuf(size);
	strcpy(result,mr->owner->name);
	strcat(result,"/");
	strcat(result,mr->name);
	strcat(result,"(");
	for (i=0;i<mr->nparams;++i) {
	    strcat(result,mr->params[i]->name);
	}
	strcat(result,")");
	strcat(result,mr->result->name);
	return result;
    }
}

const char *fivmr_MethodRec_descriptor(fivmr_MethodRec *mr) {
    if (mr==NULL) {
	return "(null mr)";
    } else {
	size_t size=1; /* for null char */
	int32_t i;
	char *result;
	size++; /* lparen */
	for (i=0;i<mr->nparams;++i) {
	    size+=strlen(mr->params[i]->name);
	}
	size++; /* rparen */
	size+=strlen(mr->result->name);
	result=fivmr_threadStringBuf(size);
	strcpy(result,"(");
	for (i=0;i<mr->nparams;++i) {
	    strcat(result,mr->params[i]->name);
	}
	strcat(result,")");
	strcat(result,mr->result->name);
	return result;
    }
}

#if FIVMR_VERBOSE_RUN_METHOD
void fivmr_MethodRec_logEntry(fivmr_ThreadState *ts,
                              fivmr_MethodRec *mr) {
    fivmr_Log_lockedPrintf("Thread %u: entering %s\n",
			   ts->id,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logExit(fivmr_ThreadState *ts,
                             fivmr_MethodRec *mr) {
    if (ts->curException) {
	fivmr_Log_lockedPrintf("Thread %u: throwing out of %s\n",
			       ts->id,
			       fivmr_MethodRec_describe(mr));
    } else {
	fivmr_Log_lockedPrintf("Thread %u: returning from %s\n",
			       ts->id,
			       fivmr_MethodRec_describe(mr));
    }
}

void fivmr_MethodRec_logResultInt(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr,
				  int32_t result) {
    fivmr_Log_lockedPrintf("Thread %u: returning %d from %s\n",
			   ts->id,
			   result,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logResultLong(fivmr_ThreadState *ts,
                                   fivmr_MethodRec *mr,
				   int64_t result) {
    fivmr_Log_lockedPrintf("Thread %u: returning %" PRId64 " from %s\n",
			   ts->id,
			   result,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logResultFloat(fivmr_ThreadState *ts,
                                    fivmr_MethodRec *mr,
				    float result) {
    fivmr_Log_lockedPrintf("Thread %u: returning %lf from %s\n",
			   ts->id,
			   (double)result,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logResultDouble(fivmr_ThreadState *ts,
                                     fivmr_MethodRec *mr,
				     double result) {
    fivmr_Log_lockedPrintf("Thread %u: returning %lf from %s\n",
			   ts->id,
			   result,
			   fivmr_MethodRec_describe(mr));
}

void fivmr_MethodRec_logResultPtr(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr,
				  uintptr_t result) {
    fivmr_Log_lockedPrintf("Thread %u: returning Ptr[%p] from %s\n",
			   ts->id,
			   result,
			   fivmr_MethodRec_describe(mr));
}
#endif

void fivmr_MethodRec_registerMC(fivmr_MethodRec *mr,
                                fivmr_MachineCode *mc) {
    fivmr_VM *vm=fivmr_TypeData_getVM(mr->owner);

    fivmr_assert(fivmr_ThreadState_isInNative(fivmr_ThreadState_get(vm)));

    fivmr_MachineCode_up(mc);

    fivmr_Lock_lock(&vm->typeDataLock);
    mc->next=(fivmr_MachineCode*)mr->codePtr;
    mr->codePtr=mc;
    mr->flags|=FIVMR_MBF_DYNAMIC;
    fivmr_Lock_unlock(&vm->typeDataLock);
}

void fivmr_MethodRec_unregisterMC(fivmr_MethodRec *mr,
                                  fivmr_MachineCode *mc) {
    fivmr_VM *vm;
    fivmr_MachineCode **cur;
    bool found=false;
    
    vm=fivmr_TypeData_getVM(mr->owner);
    
    fivmr_assert(fivmr_ThreadState_isInNative(fivmr_ThreadState_get(vm)));

    fivmr_Lock_lock(&vm->typeDataLock);
    
    for (cur=(fivmr_MachineCode**)&mr->codePtr;*cur!=NULL;cur=&(*cur)->next) {
        if (*cur==mc) {
            *cur=mc->next;
            found=true;
            break;
        }
    }
    
    fivmr_assert(found);
    
    fivmr_Lock_unlock(&vm->typeDataLock);
    
    fivmr_MachineCode_down(mc);
}

fivmr_MachineCode *fivmr_MethodRec_findMC(fivmr_MethodRec *mr,
                                          fivmr_MachineCodeFlags mask,
                                          fivmr_MachineCodeFlags expected) {
    fivmr_VM *vm;
    fivmr_MachineCode *mc;

    vm=fivmr_TypeData_getVM(mr->owner);
    
    fivmr_assert(fivmr_ThreadState_isInNative(fivmr_ThreadState_get(vm)));
    
    fivmr_Lock_lock(&vm->typeDataLock);

    if ((mr->flags&FIVMR_MBF_DYNAMIC)) {
        bool found=false;
        
        for (mc=(fivmr_MachineCode*)mr->codePtr;mc!=NULL;mc=mc->next) {
            if ((mc->flags&mask)==expected) {
                fivmr_MachineCode_up(mc);
                found=true;
                break;
            }
        }
        
        fivmr_assert(found==(mc!=NULL));
    } else {
        mc=NULL;
    }

    fivmr_Lock_unlock(&vm->typeDataLock);
    
    return mc;
}

bool fivmr_MethodRec_hasMC(fivmr_MethodRec *mr,
                           fivmr_MachineCodeFlags mask,
                           fivmr_MachineCodeFlags expected) {
    /* FIXME: this could be optimized */
    fivmr_MachineCode *mc=fivmr_MethodRec_findMC(mr,mask,expected);
    
    if (mc!=NULL) {
        fivmr_MachineCode_down(mc);
        return true;
    } else {
        return false;
    }
}


# 1 "fivmr_monitor.c"
/*
 * fivmr_monitor.c
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

#include "fivmr.h"

void fivmr_MonState_describe(fivmr_MonState state,
                             char *buf,
                             size_t bufsize) {
    snprintf(buf,bufsize,
             "%s%s%sT%uR%u",
             fivmr_MonState_queued(state)?"Qu":"",
             fivmr_MonState_rtQueued(state)?"Rq":"",
             fivmr_MonState_biased(state)?"Bi":"",
             fivmr_MonState_thread(state),
             fivmr_MonState_realRC(state));
}

void fivmr_Monitor_setStateBit(fivmr_Monitor *monitor,
                               fivmr_MonState bit,
                               bool value) {
    for (;;) {
        fivmr_MonState oldState,newState;
        oldState=monitor->state;
        if (value) {
            newState=oldState|bit;
        } else {
            newState=oldState&~bit;
        }
        if (fivmr_cas_weak(&monitor->state,
                           oldState,
                           newState)) {
            return;
        }
        fivmr_spin_fast();
    }
}

void fivmr_Monitor_pokeRTQueued(fivmr_Monitor *monitor) {
    if (monitor->queues!=NULL) {
        fivmr_ThreadState *ts=fivmr_ThreadQueue_peek(&monitor->queues->entering);
        fivmr_Monitor_setStateBit(
            monitor,
            FIVMR_MS_RT_QUEUED,
            ts!=NULL && fivmr_ThreadPriority_isRT(ts->curPrio));
    }
}

bool fivmr_Monitor_ensureQueues(fivmr_ThreadState *ts,
                                fivmr_Monitor *monitor,
                                fivmr_ObjHeader *head) {
    if (monitor->queues==NULL) {
        fivmr_MonQueues *queues;
        
        if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
            fivmr_MemoryArea *curarea=ts->gc.currentArea;
            fivmr_MemoryArea *objarea=fivmr_MemoryArea_forObject(
                ts,fivmr_ObjHeader_toObject(&ts->vm->settings,head));
            if (curarea!=objarea) {
                fivmr_MemoryArea_setCurrentArea(ts,objarea);
            }
            queues=fivmr_allocRawType(ts,fivmr_MonQueues);
            if (curarea!=objarea) {
                fivmr_MemoryArea_setCurrentArea(ts,curarea);
            }
        } else {
            queues=fivmr_allocRawType(ts,fivmr_MonQueues);
        }
        if (ts->curException) {
            return false;
        }
	fivmr_ThreadQueue_init(&queues->entering);
	fivmr_ThreadQueue_init(&queues->waiting);
        fivmr_fence(); /* just to be sure */
        fivmr_cas_void((uintptr_t*)&monitor->queues,
                       (uintptr_t)NULL,
                       (uintptr_t)queues);
        fivmr_assert(monitor->queues!=NULL);
    }
    fivmr_fence();
    return true;
}

void fivmr_Monitor_lock_slow(fivmr_ObjHeader *head,
			     fivmr_ThreadState *ts) {
    int32_t cnt;
    fivmr_Monitor *curMonitor;
    fivmr_MonState state;
    
    fivmr_SPC_incLockSlow();
    
    LOG(4,("Thread %u: locking slow %p",ts->id,head));
    
    curMonitor=fivmr_ObjHeader_getMonitor(&ts->vm->settings,head);

    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_BEGIN,
                      (uintptr_t)head);

    state=curMonitor->state;
    
    /* this comes first because we'd really like it to be on the fast path but
       we couldn't fit it in... */
    if (!fivmr_MonState_biased(state) &&
        fivmr_MonState_thread(state)==ts->id) {
        
        fivmr_SPC_incLockSlowRecurse();
        for (;;) {
            fivmr_MonState newState;
            newState=fivmr_MonState_incRC(state);
            fivmr_assert(fivmr_MonState_queued(state)
                         ==fivmr_MonState_queued(newState));
            
            /* it isn't necessary to do this check every time, but it shouldn't hurt,
               plus it makes the code nicer to structure. */
            if (fivmr_MonState_rc(newState)==1 /* rec count overflows to 1 because
                                                  0 is 1 */) {
                fivmr_throwIllegalMonitorStateException_inJava(
                    ts,"Recursion count overflow");
                return;
            }
            
            if (fivmr_cas_weak(&curMonitor->state,
                               state,newState)) {
                fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                                  FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_END,
                                  (uintptr_t)head);
                return;
            }
            
            /* reload the lock state since we failed */
            state=curMonitor->state;

            fivmr_spin_fast();
        }
    }
    
    if (state==FIVMR_MS_INVALID) {
        /* we have a dummy monitor (i.e. TypeData), need to inflate. */
        fivmr_SPC_incLockSlowInflate();
        curMonitor=fivmr_Monitor_inflate(head,ts);
        if (curMonitor==NULL) {
            return; /* fail! */
        }
        /* if successful, try to acquire the lock.  this may either be a
           biased acquisition (if biased locking is enabled) or a fast
           lock acquisition (if it's disabled). */
        state=curMonitor->state;
    }
    
    if (fivmr_MonState_biased(state)) {
        if (fivmr_MonState_thread(state)==ts->id) {
            fivmr_MonState newState;
            fivmr_SPC_incLockSlowRecurse();
            newState=fivmr_MonState_incRC(state);
            fivmr_assert(fivmr_MonState_queued(state)
                         ==fivmr_MonState_queued(newState));
            if (fivmr_MonState_rc(newState)==0) {
                fivmr_throwIllegalMonitorStateException_inJava(
                    ts,"Recursion count overflow");
                return;
            }
            fivmr_assert_cas(&curMonitor->state,
                             state,newState);
            fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                              FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_END,
                              (uintptr_t)head);
            return;
        } else /* oh noes!  biased but not to us! */ {
            fivmr_Nanos before;
            
            before=fivmr_curTime();
                
            do {
                bool wasRunning;
                fivmr_ThreadState *otherTS;
                
                fivmr_assert(fivmr_MonState_thread(state)!=0);
                fivmr_assert(fivmr_MonState_thread(state)!=1);
                
                /* figure out which thread owns the lock and pair handshake with them. */
                otherTS=fivmr_ThreadState_byId(ts->vm,fivmr_MonState_thread(state));
                fivmr_ThreadState_goToNative(ts);
                
                fivmr_Lock_lock(&otherTS->lock);
                if (fivmr_ThreadState_isRunning(otherTS)) {
                    otherTS->toUnbias=fivmr_ObjHeader_toObject(&ts->vm->settings,
                                                               head);
                    
                    /* there is a race here - while we wait, another thread can
                       also attempt to perform a soft handshake on the same target -
                       this is why we have that stupid loop.  really it's kind of
                       a bug in how softPairHandshake works, kind of.  except it
                       appears to be totally harmless. */
                    fivmr_ThreadState_softPairHandshake(otherTS,FIVMR_TSEF_UNBIAS);
                    
                    fivmr_assert(otherTS->toUnbias==0 || ts->vm->exiting);
                } else {
                    /* masquerade as the other thread since it ain't running */
                    fivmr_Monitor_unbiasIfBiasedToMe(curMonitor,otherTS);
                    fivmr_assert(!fivmr_MonState_biased(curMonitor->state));
                }
                fivmr_Lock_unlock(&otherTS->lock);
                
                fivmr_ThreadState_checkExit(ts);
                
                fivmr_ThreadState_goToJava(ts);
            } while (fivmr_MonState_biased(curMonitor->state));
            
            LOG(3,("Unbiasing took %" PRIu64 " ns",fivmr_curTime()-before));
            
            /* now we should be able to acquire, or at least
               contend on, the lock. */
        }
    }
    
    /* this assertion serves two purposes:
       1) if biased locking is disabled, this asserts that the lock cannot be
          biased.
       2) if the lock was biased to someone else, this asserts that we
          succeeded in unbiasing it. */
    if (FIVMR_ASSERTS_ON &&
        fivmr_MonState_biased(curMonitor->state)) {
        LOG(0,("encountered a biased lock unexpectedly.  "
               "old state = %p, cur state = %p",
               state,curMonitor->state));
        fivmr_assert(false);
    }
    
    for (cnt=0;;cnt++) {
        int spinMode=FIVMR_SPIN_FAST;
        state=curMonitor->state;
            
        LOG(5,("Thread %u trying to lock %p (%p), state = %" PRIuPTR ".",
               ts->id,head,curMonitor,state));
            
        fivmr_assert(state!=FIVMR_MS_INVALID);
        fivmr_assert(!fivmr_MonState_biased(state));
        fivmr_assert(fivmr_MonState_thread(state)!=ts->id);
	
        if (fivmr_MonState_available(state)) {
            uintptr_t newState;
            /* try to grab the lock if nobody holds it.  note that this will
               barge in even if the lock's queue is currently being mucked
               with. */
            fivmr_SPC_incLockSlowNotHeld();
            newState=fivmr_MonState_withThread(state,ts->id);
            fivmr_assert(fivmr_MonState_queued(state)
                         ==fivmr_MonState_queued(newState));
            if (fivmr_cas_weak(&curMonitor->state,
                               state,newState)) {
                break;
            }
        } else if (cnt<ts->vm->monitorSpinLimit &&
                   !fivmr_MonState_mustQueue(state) &&
                   !fivmr_ThreadPriority_isRT(ts->curPrio)) {
                
            /* spin around a few times before using heavy locking, but yield the
               thread instead of just using pause. */
            fivmr_SPC_incLockSlowSpin();
            spinMode=FIVMR_SPIN_SLOW;
        } else {
            fivmr_ThreadQueue *queue;
            fivmr_ThreadPriority prio=ts->curPrio;
            bool needToEnqueue = false; /* make compiler happy (real value is set
                                           somewhere below) */
            
            fivmr_SPC_incLockSlowQueue();
            if (!fivmr_Monitor_ensureQueues(ts,curMonitor,head)) {
                /* exception! */
                return;
            }
            
            fivmr_assert(curMonitor->queues!=NULL);
            fivmr_assert(ts->curException==0);
            
            queue=&curMonitor->queues->entering;
            
            fivmr_BoostedSpinLock_lock(ts,&queue->lock);
            
            /* need to indicate that someone is queueing, but also check that the lock
               didn't become available in the process. */
            for (;;) {
                state=curMonitor->state; /* reread the state as it may have changed in
                                            some relevant way */
                    
                if (fivmr_MonState_available(state)) {
                    needToEnqueue=false;
                    break;
                }
                if (fivmr_MonState_queued(state) &&
                    (!fivmr_ThreadPriority_isRT(prio) ||
                     fivmr_MonState_rtQueued(state))) {
                    /* cool - queued is already set, no need to do anything */
                    needToEnqueue=true;
                    break;
                }
                if (fivmr_cas(&curMonitor->state,
                              state,
                              fivmr_MonState_withQueued(
                                  fivmr_MonState_withRTQueued(
                                      state,
                                      (fivmr_MonState_rtQueued(state)|
                                       fivmr_ThreadPriority_isRT(prio))),
                                  true))) {
                    needToEnqueue=true;
                    break;
                }
                fivmr_spin_fast();
            }
            
            if (needToEnqueue) {
                bool syncHandoff;

                fivmr_assert(fivmr_MonState_queued(curMonitor->state));
                
                fivmr_ThreadQueue_enqueue(queue,ts);
                fivmr_assert(fivmr_ThreadState_isOnAQueue(ts));
                fivmr_assert(fivmr_Monitor_queuedShouldBeSet(curMonitor));
                fivmr_assert(fivmr_MonState_queued(curMonitor->state));
                ts->forMonitor.entering=curMonitor;
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
                
                /* we are now queued.  wait to get signaled.  note that we're guaranteed
                   to get signaled, since at the time that we set the QUEUED bit the lock
                   was unavailable. */
                    
                /* ok so this call is very magical.  it will reevaluate our priority
                   based on the fact that we're queued on and entering this lock.  under
                   PIP locking this will lead to other threads' priorities getting
                   boosted. */
                LOG(3,("going to native slow"));
                fivmr_ThreadState_goToNative_slow(ts);
                    
                /* I think we can just down the waiter.  but I'm unsure.  there are
                   some subtle issues at play here. */
                fivmr_Semaphore_down(&ts->waiter);
                
                /* it may have been downed because exit was signaled - so check if
                   we should exit.  in that case checkExit will not return. */
                fivmr_ThreadState_checkExit(ts);
                
                fivmr_assert(!fivmr_ThreadState_isOnAQueue(ts));

                fivmr_ThreadState_goToJava(ts);
                    
                syncHandoff=ts->forMonitor.syncHandoffCookie;
                ts->forMonitor.syncHandoffCookie=false;

                fivmr_BoostedSpinLock_lock(ts,&queue->lock);
                ts->forMonitor.entering=NULL;
                fivmr_ThreadQueue_eueuqne(queue,ts);
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);

                /* check if this was a synchronous handoff */
                if (syncHandoff) {
                    LOG(2,("Received a synchronous monitor handoff in Thread #%u "
                           "on monitor %p",
                           ts->id,curMonitor));
                    state=curMonitor->state;
                    fivmr_assert(fivmr_MonState_thread(state)==ts->id);
                    fivmr_assert(fivmr_MonState_rc(state)==1);
                    break; /* done - the lock is ours */
                }

                /* ok - our turn! */
                spinMode=FIVMR_SPIN_NONE;
            } else {
                /* false alarm!  just loop around and recontend on the lock. */
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
            }
            fivmr_assert(!fivmr_Thread_isCritical());
        }
	
        fivmr_spin(spinMode);
    }
    
    /* ensure that everyone knows that this thread holds this lock.  however note that
       it is possible for that "notification" to have already happened for example due to a
       synchronous handoff.  so we do it conditionally.  I think that's safe. */
    if (FIVMR_PIP_LOCKING(&ts->vm->settings) && ts->forMonitor.holding!=curMonitor) {
        curMonitor->next=ts->forMonitor.holding;
        ts->forMonitor.holding=curMonitor;
    }
    
    LOG(11,("Thread %u: returning from lock slow %p (%p) with state = %p, curException = %p.",
	    ts->id,head,fivmr_ObjHeader_getMonitor(&ts->vm->settings,head),
	    fivmr_ObjHeader_getMonitor(&ts->vm->settings,head)->state,ts->curException));
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_LOCK_SLOW_END,
                      (uintptr_t)head);
}

void fivmr_Monitor_unlockInflated(fivmr_Monitor *curMonitor,
				  fivmr_ThreadState *ts) {
    fivmr_MonState state;
    
    /* note that during an attempt to unlock a thin lock it may get inflated. */
    
    /* some other stuff to worry about:
     * - cannot unlock while the lock is in the queueing state, because we may
     *   then miss an enqueued thread that we should have dequeued and signaled.
     * - unlocking in the queued state requires more work.
     * - the queues may disappear during GC.  this shouldn't be a problem here,
     *   but still, need to be careful. */

    fivmr_SPC_incUnlockSlow();
    
    state=curMonitor->state;

    /* error checking */
    if (fivmr_MonState_thread(state)!=ts->id ||
	fivmr_MonState_realRC(state)==0) {
	fivmr_throwIllegalMonitorStateException_inJava(
	    ts,"Thread not holding the lock at monitorexit");
        return;
    }
    
    fivmr_Monitor_assertInflated(curMonitor);
    
    if (fivmr_MonState_biased(state)) {
        fivmr_MonState newState;
        newState=fivmr_MonState_decRC(state);
        fivmr_assert_cas(&curMonitor->state,
                         state,newState);
    } else if (fivmr_MonState_rc(state)>1) {
        for (;;) {
            state=curMonitor->state;
            if (fivmr_cas_weak(&curMonitor->state,
                               state,
                               fivmr_MonState_decRC(state))) {
                LOG(11,("Thread %u: unlocked %p by decrementing (state = %p)",
                        ts->id,curMonitor,state));
                break;
            }
            fivmr_spin_fast();
        }
    } else {
        bool syncHandoff=fivmr_MonState_rtQueued(state);
        bool haveDequeued=false;
        
        fivmr_assert(fivmr_MonState_rc(state)==1);

        /* two possibilities: either the fast path has already removed this lock from
           the holding list or it hasn't.  if not we remove it now. */
        if (FIVMR_PIP_LOCKING(&ts->vm->settings) && ts->forMonitor.holding==curMonitor) {
            ts->forMonitor.holding=curMonitor->next;
            curMonitor->next=NULL;
        }
        
        /* we will do a syncHandoff if there is a high-priority thread waiting */
        
        if (!syncHandoff) {
            for (;;) {
                state=curMonitor->state;
                
                if (fivmr_cas_weak(&curMonitor->state,
                                   state,
                                   fivmr_MonState_withThread(state,0))) {
                    break;
                }
                fivmr_spin_fast();
            }
        }

        /* lock is now released - but it's possible that there is someone
           queued. so we have to let them know that it's time to go. */
                
        /* reread the state of the lock; it might have changed in some
           relevant way since the cas */
        state=curMonitor->state;
                
        if (fivmr_MonState_queued(state)) {
            /* someone is queued.  that's unfortunate. ;-)  acquire the lock
               for the queue, then: if there's someone on the queue, release
               the lock and signal them; if there's not someone on the queue
               then we're racing - so try again */
                    
            fivmr_ThreadQueue *queue;
            fivmr_ThreadState *toAwaken;
            fivmr_ThreadState *toAwaken2;
                    
            /* if someone is queued then there better be some queues for
               them to have been queued on! */
            fivmr_assert(curMonitor->queues!=NULL);
                    
            queue=&curMonitor->queues->entering;
                    
            fivmr_BoostedSpinLock_lock(ts,&queue->lock);
                    
            /* reread the state of the lock, as it could have changed in some
               relevant way between when we spun on the queue lock and now.
               in particular there's a massively convoluted corner case where
               this thread had acquired the lock in a barging fashion while
               some other thread was performing a dequeue.  now this thread
               is releasing the lock that it had barged in on - and only now
               does it realize that the lock had its queued bit set.  this
               crucially relies on the dequeuing thread being descheduled for
               the entire duration of this thread's critical section - but
               this is quite likely on a uniprocessor.  now, we end up trying
               to acquire the spinlock on the queue, which the other thread
               still holds.  this serves as the trigger to get that thread
               scheduled again; once scheduled, that thread will complete
               the dequeue and reset the queued bit.  now we get scheduled,
               we finish acquiring the spinlock, and we have to be smart to
               realize that the queued bit is no longer set. */
            state=curMonitor->state;
                    
            /* we hold the queue lock.  that means that there should be
               coherence between the queued bit and the actual state of the
               queues.  assert that coherence. */
            fivmr_assert(fivmr_MonState_queued(state)==
                         !fivmr_ThreadQueue_empty(queue));
                    
            /* check if the lock is still queued - see rant about convoluted
               race conditions above. */
            if (fivmr_MonState_queued(state)) {
                /* ok - someone is queued on the lock.  figure out who it
                   is. */
                toAwaken=fivmr_ThreadQueue_peek(queue);
                fivmr_assert(fivmr_ThreadState_isValid(toAwaken));
                fivmr_assert(fivmr_ThreadState_isOnAQueue(toAwaken));
                
                if (syncHandoff) {
                    LOG(2,("Performing a synchronous monitor handoff from Thread #%u to "
                           "Thread #%u on monitor %p",
                           ts->id,toAwaken->id,curMonitor));
                    for (;;) {
                        state=curMonitor->state;
                        
                        fivmr_assert(fivmr_MonState_thread(state)==ts->id);
                        fivmr_assert(fivmr_MonState_rc(state)==1);
                        
                        if (fivmr_cas_weak(&curMonitor->state,
                                           state,
                                           fivmr_MonState_withThread(
                                               state,
                                               toAwaken->id))) {
                            break;
                        }
                        fivmr_spin_fast();
                    }
                    toAwaken->forMonitor.syncHandoffCookie=true;
                }
                
                toAwaken2=fivmr_ThreadQueue_dequeue(queue);
                fivmr_assert(toAwaken==toAwaken2);
                fivmr_assert(!fivmr_ThreadState_isOnAQueue(toAwaken));
                        
                /* we know who to unblock.  now make sure that the quued bit
                   is reset, if the queue is now otherwise empty. */
                if (fivmr_ThreadQueue_empty(queue)) {
                    fivmr_Monitor_setStateBit(curMonitor,
                                              FIVMR_MS_QUEUED,
                                              false);
                    fivmr_assert(!fivmr_MonState_queued(curMonitor->state));
                    fivmr_assert(fivmr_ThreadQueue_empty(queue));
                }

                fivmr_Monitor_pokeRTQueued(curMonitor);
                        
                fivmr_assert(fivmr_MonState_queued(curMonitor->state)==
                             !fivmr_ThreadQueue_empty(queue));
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
                fivmr_assert(!fivmr_Thread_isCritical());
                        
                /* ok - we've done our due diligence on the lock.  now we just
                   notify the relevant thread and return. */
                fivmr_Semaphore_up(&toAwaken->waiter);
                
                haveDequeued=true;
                
            } else {
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
            }
        }
        
        if (syncHandoff && !haveDequeued) {
            for (;;) {
                state=curMonitor->state;
                
                fivmr_assert(fivmr_MonState_thread(state)==ts->id);
                
                if (fivmr_cas_weak(&curMonitor->state,
                                   state,
                                   fivmr_MonState_withThread(state,0))) {
                    break;
                }
                fivmr_spin_fast();
            }
        }
                
        fivmr_assert(!fivmr_Thread_isCritical());
    }
    
    /* evaluate our priority as it may have changed due to us releasing a lock that was
       boosted by PIP. */
    fivmr_ThreadState_evalPrio(ts);
    
    LOG(5,("Thread %u: returning from unlock slow %p with state = %p, curException = %p.",
	   ts->id,curMonitor,curMonitor->state,ts->curException));
}

void fivmr_Monitor_unlock_slow(fivmr_ObjHeader *head,
			       fivmr_ThreadState *ts) {
    LOG(11,("Thread %u: unlocking slow %p",ts->id,head));
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_UNLOCK_SLOW_BEGIN, (uintptr_t)head);

    fivmr_Monitor_unlockInflated(
        fivmr_ObjHeader_getMonitor(&ts->vm->settings,head),
        ts);

    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_UNLOCK_SLOW_END, (uintptr_t)head);
}

fivmr_Monitor *fivmr_Monitor_inflate(fivmr_ObjHeader *head,
				     fivmr_ThreadState *ts) {
    LOG(3,("Inflating monitor %p for object of type %s (%p) in Thread %u.",
	   head,fivmr_ObjHeader_getMonitor(&ts->vm->settings,head)->forward->name,
	   fivmr_ObjHeader_getMonitor(&ts->vm->settings,head)->forward,ts->id));

    fivmr_SPC_incInflate();
    
    for (;;) {
	fivmr_Monitor *curMonitor=
            fivmr_ObjHeader_getMonitor(&ts->vm->settings,head);
	
	if (curMonitor->state==FIVMR_MS_INVALID) {
	    fivmr_Monitor *newMonitor;
	    uintptr_t state;
	    
	    fivmr_assert(curMonitor==(void*)curMonitor->forward);

            if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
                fivmr_MemoryArea *curarea=ts->gc.currentArea;
                fivmr_MemoryArea *objarea=fivmr_MemoryArea_forObject(
                    ts,fivmr_ObjHeader_toObject(&ts->vm->settings,head));
                if (curarea!=objarea) {
                    /* If the object is stack allocated, we can only inflate
                     * its monitor if we're still in the same frame. */
                    if ((uintptr_t)objarea==fivmr_MemoryArea_getStackArea(ts)) {
                        fivmr_GCHeader *hdr=
                            fivmr_GCHeader_fromObject(
                                &ts->vm->settings,
                                fivmr_ObjHeader_toObject(&ts->vm->settings,head));
                        if (fivmr_GCHeader_frame(hdr)!=ts->allocFrame) {
                            fivmr_throwIllegalMonitorStateException_inJava(
				ts,"Attempted to inflate monitor on stack-allocated object from another Frame");
                            return NULL;
                        }
                    } else {
                        fivmr_MemoryArea_setCurrentArea(ts,objarea);
                    }
                }
                newMonitor=fivmr_allocRawType(ts,fivmr_Monitor);
                if (curarea!=objarea) {
                    fivmr_MemoryArea_setCurrentArea(ts,curarea);
                }
            } else {
                newMonitor=fivmr_allocRawType(ts,fivmr_Monitor);
            }
	    
	    LOG(5,("Thread %u: monitor at = %p",ts->id,newMonitor));
	    
	    if (ts->curException) {
                return NULL;
            }
	    state=FIVMR_MS_NOT_HELD;
	    if (FIVMR_BIASED_LOCKING(&ts->vm->settings)) {
		LOG(6,("(1) Thread %u inflating %p to %p with state = %" PRIuPTR,
		       ts->id,head,newMonitor,state));
		state=fivmr_MonState_withBiased(state,true);
		LOG(6,("(2) Thread %u inflating %p to %p with state = %" PRIuPTR,
		       ts->id,head,newMonitor,state));
		state=fivmr_MonState_withThread(state,ts->id);
		LOG(6,("(3) Thread %u inflating %p to %p with state = %" PRIuPTR,
		       ts->id,head,newMonitor,state));
	    }
	    LOG(6,("(4) Thread %u inflating %p to %p with state = %" PRIuPTR,
		   ts->id,head,newMonitor,state));
	    newMonitor->state=state;
	    newMonitor->forward=(fivmr_TypeData*)curMonitor;
	    
	    LOG(6,("Thread %u: monitor->forward = %p",ts->id,newMonitor->forward));
	    
	    newMonitor->queues=NULL;

	    /* use strong CAS so that we don't waste an allocation due to
	       spurious weak CAS failures. */
	    if (fivmr_ObjHeader_cas(&ts->vm->settings,
                                    head,
				    curMonitor,
				    newMonitor)) {
		LOG(3,("For monitor %p, returning new %p in Thread %u.",
		       head,newMonitor,ts->id));
		return newMonitor;
	    }
	} else {
	    LOG(3,("For monitor %p, returning existing %p in Thread %u.",
		   head,curMonitor,ts->id));
	    return curMonitor;
	}
    }
}

fivmr_ThreadState *fivmr_Monitor_curHolder(fivmr_VM *vm,
                                           fivmr_Monitor *monitor) {
    fivmr_MonState monState=monitor->state;
    if (fivmr_MonState_realRC(monState)==0) {
        return NULL;
    } else {
        uint32_t thrId=fivmr_MonState_thread(monState);
        fivmr_assert(thrId>=2);
        return fivmr_ThreadState_byId(vm,thrId);
    }
}

int32_t fivmr_Monitor_rc(fivmr_Monitor *monitor) {
    return (int32_t)fivmr_MonState_realRC(monitor->state);
}

fivmr_ThreadState *fivmr_Object_curHolder(fivmr_VM *vm,
                                          fivmr_Object obj) {
    return fivmr_Monitor_curHolder(
        vm,
        fivmr_ObjHeader_getMonitor(
            &vm->settings,
            fivmr_ObjHeader_forObject(
                &vm->settings,
                obj)));
}

int32_t fivmr_Object_recCount(fivmr_VM *vm,
                              fivmr_Object obj) {
    return fivmr_Monitor_rc(
        fivmr_ObjHeader_getMonitor(
            &vm->settings,
            fivmr_ObjHeader_forObject(
                &vm->settings,
                obj)));
}

uint32_t fivmr_Monitor_unlockCompletely(fivmr_Monitor *monitor,
					fivmr_ThreadState *ts) {
    uint32_t recCount;
    
    fivmr_Monitor_assertInflated(monitor);
    
    recCount=fivmr_MonState_rc(monitor->state);
    for (;;) {
        fivmr_MonState state=monitor->state;
        if (fivmr_cas_weak(&monitor->state,
                           state,
                           fivmr_MonState_withRC(state,1))) {
            break;
        }
        fivmr_spin_fast();
    }
    fivmr_Monitor_unlockInflated(monitor,ts);
    return recCount;
}

void fivmr_Monitor_relock(fivmr_ObjHeader *head,
			  fivmr_ThreadState *ts,
			  uint32_t recCount) {
    fivmr_Monitor *monitor;
    fivmr_Monitor_lock_slow(head,ts);
    monitor=fivmr_ObjHeader_getMonitor(&ts->vm->settings,head);
    fivmr_Monitor_assertInflated(monitor);
    
    for (;;) {
        fivmr_MonState state=monitor->state;
        if (fivmr_cas_weak(&monitor->state,
                           state,
                           fivmr_MonState_withRC(state,recCount))) {
            break;
        }
        fivmr_spin_fast();
    }
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_RELOCK, (uintptr_t)head);
}

void fivmr_Monitor_unbiasWhenHeld(fivmr_Monitor *monitor,
				  fivmr_ThreadState *ts) {
    fivmr_MonState state;
    
    fivmr_Monitor_assertInflated(monitor);

    /* cannot use setStateBit because MonState_withBiased has special magic in
       it. */
    for (;;) {
        fivmr_MonState state=monitor->state;
        
        fivmr_assert(fivmr_MonState_realRC(state)!=0);
        fivmr_assert(fivmr_MonState_thread(state)==ts->id);

        if (fivmr_cas_weak(&monitor->state,
                           state,
                           fivmr_MonState_withBiased(state,
                                                     false))) {
            break;
        }
        fivmr_spin_fast();
    }
}

void fivmr_Monitor_unbiasIfBiasedToMe(fivmr_Monitor *monitor,
                                      fivmr_ThreadState *ts) {
    fivmr_MonState state;
    
    state=monitor->state;
    
    if (state==FIVMR_MS_INVALID) {
        return;
    }
    
    if (!fivmr_MonState_biased(state)) {
        return;
    }
    
    fivmr_assert(fivmr_MonState_thread(state)==ts->id);

    state=monitor->state;
    fivmr_assert_cas(&monitor->state,
		     state,
		     fivmr_MonState_withBiased(state,false));
}

void fivmr_Monitor_unbiasFromHandshake(fivmr_ThreadState *ts) {
    if (ts->toUnbias!=0) {
        fivmr_Monitor_unbiasIfBiasedToMe(
            fivmr_ObjHeader_getMonitor(
                &ts->vm->settings,
                fivmr_ObjHeader_forObject(&ts->vm->settings,
                                          ts->toUnbias)),
            ts);
        ts->toUnbias=0;
    }
    ts->execFlags&=~FIVMR_TSEF_UNBIAS;
}

void fivmr_Monitor_unbias(fivmr_ObjHeader *head) {
    fivmr_abort("haven't implemented fivmr_Monitor_unbias yet, even though it's "
                "like the easiest thing to implement.");
}

static void wait_impl(fivmr_ObjHeader *head,
		      fivmr_ThreadState *ts,
		      bool hasTimeout,
		      fivmr_Nanos whenAwake) {
    uint32_t recCount;
    fivmr_Monitor *monitor;
    fivmr_MonState state;

    monitor=fivmr_ObjHeader_getMonitor(&ts->vm->settings,head);
    
    fivmr_Monitor_unbiasWhenHeld(monitor,ts);
    
    fivmr_Lock_lock(&ts->lock);
    ts->execFlags|=FIVMR_TSEF_WAITING;
    if (hasTimeout) {
	ts->execFlags|=FIVMR_TSEF_TIMED;
    }
    fivmr_Lock_unlock(&ts->lock);
    
    if (!fivmr_Monitor_ensureQueues(ts,monitor,head)) return;

    fivmr_BoostedSpinLock_lock(ts,&monitor->queues->waiting.lock);
    fivmr_ThreadQueue_enqueue(&monitor->queues->waiting,ts);
    fivmr_BoostedSpinLock_unlock(ts,&monitor->queues->waiting.lock);

    fivmr_assert(!fivmr_Thread_isCritical());
    
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_UNLOCK_COMPLETE,
                      (uintptr_t)head);
    recCount=fivmr_Monitor_unlockCompletely(monitor,ts);
    
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_WAIT, (uintptr_t)head);

    fivmr_ThreadState_goToNative(ts);
    fivmr_Lock_lock(&ts->lock);
    while (!ts->interrupted &&
	   (!hasTimeout || fivmr_curTime()<whenAwake) &&
	   fivmr_ThreadState_isOnAQueue(ts)) {
        /* FIXME use the semaphore */
	if (hasTimeout) {
	    fivmr_Lock_timedWaitAbs(&ts->lock,whenAwake);
	} else {
	    fivmr_Lock_wait(&ts->lock);
	}
        fivmr_ThreadState_checkExit(ts);
    }
    fivmr_Lock_unlock(&ts->lock);
    fivmr_ThreadState_goToJava(ts);
    
    fivmr_BoostedSpinLock_lock(ts,&monitor->queues->waiting.lock);
    if (fivmr_ThreadState_isOnAQueue(ts)) {
        fivmr_ThreadQueue_remove(&monitor->queues->waiting,ts);
        fivmr_assert(!fivmr_Thread_isCritical());
    }

    fivmr_ThreadQueue_eueuqne(&monitor->queues->waiting,ts);
    fivmr_BoostedSpinLock_unlock(ts,&monitor->queues->waiting.lock);
    
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_MONITOR,
                      FIVMR_FLOWLOG_SUBTYPE_WAKE, (uintptr_t)head);

    fivmr_Monitor_relock(head,ts,recCount);
    
    fivmr_Lock_lock(&ts->lock);
    ts->execFlags&=~(FIVMR_TSEF_WAITING|FIVMR_TSEF_TIMED);
    fivmr_Lock_unlock(&ts->lock);
}

void fivmr_Monitor_wait(fivmr_ObjHeader *head,
			fivmr_ThreadState *ts) {
    LOG(3,("Thread %u: waiting on %p",ts->id,head));
    wait_impl(head,ts,false,0);
}

void fivmr_Monitor_timedWait(fivmr_ObjHeader *head,
			     fivmr_ThreadState *ts,
			     fivmr_Nanos whenAwake) {
    LOG(3,("Thread %u: timed waiting on %p",ts->id,head));
    wait_impl(head,ts,true,whenAwake);
}

bool fivmr_Monitor_notify(fivmr_ThreadState *ts,
                          fivmr_Monitor *monitor) {
    fivmr_ThreadState *toAwaken=NULL;
    
    LOG(3,("Thread %u: notifying on %p",ts->id,monitor));

    fivmr_Monitor_unbiasWhenHeld(monitor,ts);
    
    if (monitor->queues!=NULL) {
        fivmr_BoostedSpinLock_lock(ts,
                                   &monitor->queues->waiting.lock);
        
        if (!fivmr_ThreadQueue_empty(&monitor->queues->waiting)) {
            toAwaken=fivmr_ThreadQueue_dequeue(&monitor->queues->waiting);
        }
        
        fivmr_BoostedSpinLock_unlock(ts,
                                     &monitor->queues->waiting.lock);
        
        fivmr_assert(!fivmr_Thread_isCritical());
    }
    
    if (toAwaken!=NULL) {
	fivmr_Lock_lockedBroadcast(&toAwaken->lock);
	return true;
    } else {
	return false;
    }
}

bool fivmr_Monitor_notifyAll(fivmr_ThreadState *ts,
                             fivmr_Monitor *monitor) {
    bool result=false;
    LOG(3,("Thread %u: notifying all on %p",ts->id,monitor));
    while (fivmr_Monitor_notify(ts,monitor)) result=true;
    return result;
}

bool fivmr_Object_lock(fivmr_ThreadState *ts,
                       fivmr_Object obj) {
    fivmr_Monitor_lock_slow(fivmr_ObjHeader_forObject(&ts->vm->settings,obj),ts);
    return ts->curException==0;
}

bool fivmr_Object_unlock(fivmr_ThreadState *ts,
                         fivmr_Object obj) {
    fivmr_Monitor_unlock_slow(fivmr_ObjHeader_forObject(&ts->vm->settings,obj),ts);
    return ts->curException==0;
}




# 1 "fivmr_nogc.c"
/*
 * fivmr_nogc.c
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

#include <fivmr_config.h>

#if !FIVMBUILD_SPECIAL_GC || FIVMBUILD__NOGC

#include "fivmr.h"

#include <unistd.h>

#define FIVMR_TLB_SIZE FIVMR_PAGE_SIZE*10
#define FIVMR_LARGE_OBJECT (FIVMR_TLB_SIZE/2)

void fivmr_NOGC_init(fivmr_GC *gc) {
    fivmr_Lock_init(&gc->gcLock,fivmr_Priority_bound(FIVMR_PR_MAX,
                                                     fivmr_VMfromGC(gc)->maxPriority));
}

void fivmr_NOGC_clear(fivmr_ThreadState *ts) {
    ts->gc.alloc[0].bump=0;
    ts->gc.alloc[0].start=0;
    ts->gc.alloc[0].size=0;
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        if (FIVMR_SCJ(&ts->vm->settings)) {
            ts->gc.currentArea=&ts->vm->gc.immortalMemoryArea;
        } else {
            ts->gc.currentArea=&ts->gc.heapMemoryArea;
        }
        ts->gc.baseStackEntry.area=ts->gc.currentArea;
        ts->gc.baseStackEntry.prev=NULL;
        ts->gc.baseStackEntry.next=NULL;
        ts->gc.scopeStack=&ts->gc.baseStackEntry;
        ts->gc.scopeBacking=NULL;
        ts->gc.heapMemoryArea.scopeID=FIVMR_GC_SH_MARK2;
    }
}

void fivmr_NOGC_startThread(fivmr_ThreadState *ts) {
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                          FIVMR_FLOWLOG_SUBTYPE_ENTER,
                          (uintptr_t)ts->gc.currentArea);
    }
}

void fivmr_NOGC_commitThread(fivmr_ThreadState *ts) {
    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
        while (ts->gc.scopeStack->prev) {
            fivmr_MemoryArea *area=ts->gc.scopeStack->area;
            fivmr_MemoryArea_pop(ts, ts->vm, area);
            fivmr_MemoryArea_free(ts, area);
        }
        if (ts->gc.scopeBacking) {
            fivmr_ScopeBacking_free(ts);
        }
    }
}

static uintptr_t sysAlloc(fivmr_GC *gc,
                          uintptr_t size) {
    uintptr_t result;
    uintptr_t numPages;
    LOG(5,("sysAlloc(%p) called.",size));
    numPages=fivmr_pages(size);
    LOG(5,("will allocate %p pages.",numPages));
    result=(uintptr_t)fivmr_allocPages(numPages,NULL);
    if (result!=0) {
        fivmr_Lock_lock(&gc->gcLock);
        gc->numPagesUsed+=numPages;
        fivmr_Lock_unlock(&gc->gcLock);
    }
    LOG(5,("sysAlloc(%p) returning %p",size,result));
    return result;
}

uintptr_t fivmr_NOGC_allocRawSlow(fivmr_ThreadState *ts,
                                  fivmr_GCSpace space,
                                  uintptr_t size,
                                  uintptr_t alignStart,
                                  uintptr_t align,
                                  fivmr_AllocEffort effort,
                                  const char *description) {
    uintptr_t result;
    
    LOG(11,("fivmr_allocRaw_slow(%p, %p, %p) called",ts,size,align));

    size=fivmr_alignRaw(size,sizeof(uintptr_t));
    
    if ((fivmr_align(ts->gc.alloc[0].bump+alignStart,align)
	 -alignStart+size)-ts->gc.alloc[0].start > ts->gc.alloc[0].size) {
	if (size>=FIVMR_LARGE_OBJECT) {
	    result=sysAlloc(&ts->vm->gc,size+fivmr_align(alignStart,align)-alignStart);
            if (result==0) {
                fivmr_throwOutOfMemoryError_inJava(ts);
                return 0;
            }
	    result+=FIVMR_ALLOC_OFFSET(&ts->vm->settings);
	    result=fivmr_align(result+alignStart,align)-alignStart;
	    LOG(9,("allocRaw_slow returning %p",result));
	    return result;
	} else {
	    uintptr_t newSpace=sysAlloc(&ts->vm->gc,FIVMR_TLB_SIZE);
	    ts->gc.alloc[0].bump=ts->gc.alloc[0].start=
		newSpace+FIVMR_ALLOC_OFFSET(&ts->vm->settings);
	    ts->gc.alloc[0].size=FIVMR_TLB_SIZE;
	}
    }
    
    LOG(10,("bump = %p, start = %p, size = %p, align = %p",
	    ts->gc.alloc[0].bump,ts->gc.alloc[0].start,ts->gc.alloc[0].size,align));

    result=
	fivmr_align(ts->gc.alloc[0].bump+alignStart,align)
	-alignStart;
    ts->gc.alloc[0].bump=result+size;
    
    LOG(9,("allocRaw_slow returning %p",result));
    
    return result;
}

void fivmr_NOGC_handleHandshake(fivmr_ThreadState *ts) {
    /* nothing to do */
}

int64_t fivmr_NOGC_freeMemory(fivmr_GC *gc) {
    return 1048576; /* FIXME */
}

int64_t fivmr_NOGC_totalMemory(fivmr_GC *gc) {
    return gc->numPagesUsed*FIVMR_PAGE_SIZE;
}

int64_t fivmr_NOGC_maxMemory(fivmr_GC *gc) {
    return INT64_C(0xfffffffffffffff); /* FIXME */
}

void fivmr_NOGC_asyncCollect(fivmr_GC *gc) {
    /* nothing to do. */
}

void fivmr_NOGC_collectFromJava(fivmr_GC *gc,
                                const char *descrIn,
                                const char *descrWhat) {
    /* nothing to do. */
}

void fivmr_NOGC_collectFromNative(fivmr_GC *gc,
                                  const char *descrIn,
                                  const char *descrWhat) {
    /* nothing to do. */
}

void fivmr_NOGC_report(fivmr_GC *gc,
                       const char *name) {
}

bool fivmr_NOGC_setMaxHeap(fivmr_GC *gc,
                           int64_t bytes) {
    return false;
}

bool fivmr_NOGC_setTrigger(fivmr_GC *gc,
                           int64_t bytes) {
    return false;
}

void fivmr_NOGC_resetStats(fivmr_GC *gc) {
}

int64_t fivmr_NOGC_numIterationsCompleted(fivmr_GC *gc) {
    return 0;
}

void fivmr_NOGC_markSlow(fivmr_ThreadState *ts,
                         fivmr_Object obj) {
}

fivmr_Spine fivmr_NOGC_allocSSSlow(fivmr_ThreadState *ts,
                                   uintptr_t spineLength,
                                   int32_t numEle,
                                   const char *description) {
    return 0;
}

void fivmr_NOGC_claimMachineCode(fivmr_ThreadState *ts,
                                 fivmr_MachineCode *mc) {
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    fivmr_MachineCode_up(mc);
}

void fivmr_NOGC_setPriority(fivmr_GC *gc,
                            fivmr_ThreadPriority prio) {
}

bool fivmr_NOGC_getNextDestructor(fivmr_GC *gc,
                                  fivmr_Handle *objCell,
                                  bool wait) {
    return false;
}

void fivmr_NOGC_signalExit(fivmr_GC *gc) {
    /* nothing to do */
}

void fivmr_NOGC_shutdown(fivmr_GC *gc) {
    /* nothing to do */
}

#endif
# 1 "fivmr_object.c"
/*
 * fivmr_typedata.c
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

#include "fivmr.h"

void fivmr_Object_specialScan(fivmr_VM *vm,
                              fivmr_Object object,
                              fivmr_TypeData *td,
                              fivmr_MarkCback mark,
                              fivmr_ScanSpecialHandlers *specials,
                              void *arg) {
    if (td==vm->payload->td_Class) {
        /* do stuff to the class */
    } else if (td==vm->payload->td_WeakReference) {
        if (specials!=NULL) {
            specials->weakRef(vm,object,arg);
        }
    }
}

# 1 "fivmr_pagetable.c"
/*
 * fivmr_pagetable.c
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

void fivmr_PageTable_initFlat(fivmr_PageTable *pt,
                              uintptr_t start,
                              uintptr_t size) {
    uintptr_t ptSize;
    
    LOG(2,("Initializing flat page table at %p",pt));
    
    pt->isFlat=true;
    
    LOGptrconst(2,FIVMR_LOG_PT_BITS_PER_PAGE);
    LOGptrconst(2,FIVMSYS_LOG_PAGE_SIZE);
    LOGptrconst(2,FIVMR_PT_BITS_PER_PAGE);
    LOGptrconst(2,FIVMR_PAGE_SIZE);
    
    pt->u.flat.start=start;
    pt->u.flat.size=(size+FIVMR_PAGE_SIZE-1)&~(FIVMR_PAGE_SIZE-1);
    
    ptSize=(((pt->u.flat.size>>FIVMSYS_LOG_PAGE_SIZE)<<FIVMR_LOG_PT_BITS_PER_PAGE)
            +31)/32*sizeof(uint32_t);
    
    LOG(1,("Allocating flat page table with size = %" PRIuPTR,
           ptSize));

    pt->u.flat.table=(uint32_t*)fivmr_mallocAssert(ptSize);
    bzero(pt->u.flat.table,ptSize);
}

void fivmr_PageTable_initML(fivmr_PageTable *pt,
                            fivmr_Priority prio) {
    LOG(2,("Initializing multi-level page table at %p",pt));
    
    LOGptrconst(2,FIVMR_LOG_PT_BITS_PER_PAGE);
    LOGptrconst(2,FIVMSYS_LOG_PAGE_SIZE);
    LOGptrconst(2,FIVMR_PT_BITS_PER_PAGE);
    LOGptrconst(2,FIVMR_PAGE_SIZE);
    LOGptrconst(2,FIVMR_PAGE_CHUNKS_PER_LIST);
#if FIVMSYS_PTRSIZE==4
    LOGptrconst(2,FIVMR_PT_ADDRBITS_PER_CHUNK);
    LOGptrconst(2,FIVMR_PT_ADDRBITS_PER_SPINE);
    LOGptrconst(2,FIVMR_PT_NUM_SPINE_ELE);
    LOGptrconst(2,FIVMR_PT_SPINE_SHIFT);
    LOGptrconst(2,FIVMR_PT_SPINE_MASK);
    LOGptrconst(2,FIVMR_PT_CHUNK_SHIFT);
    LOGptrconst(2,FIVMR_PT_CHUNK_MASK);
#else
    LOGptrconst(2,FIVMR_PT_OUTER_BITS);
    LOGptrconst(2,FIVMR_PT_MIDDLE_BITS);
    LOGptrconst(2,FIVMR_PT_INNER_BITS);
    LOGptrconst(2,FIVMR_PT_NUM_SPINE_ELE);
    LOGptrconst(2,FIVMR_PT_NUM_MIDDLE_ELE);
    LOGptrconst(2,FIVMR_PT_NUM_INNER_BYTES);
    LOGptrconst(2,FIVMR_PT_OUTER_SHIFT);
    LOGptrconst(2,FIVMR_PT_OUTER_MASK);
    LOGptrconst(2,FIVMR_PT_MIDDLE_SHIFT);
    LOGptrconst(2,FIVMR_PT_MIDDLE_MASK);
    LOGptrconst(2,FIVMR_PT_INNER_SHIFT);
    LOGptrconst(2,FIVMR_PT_INNER_MASK);
#endif
    
    fivmr_assert(sizeof(fivmr_PageChunkList)==FIVMR_PAGE_SIZE);

#if FIVMSYS_PTRSIZE==4
    fivmr_assert(FIVMR_PT_CHUNK_SIZE==FIVMR_PAGE_SIZE);
#endif
#if FIVMSYS_PTRSIZE==8
    fivmr_assert(FIVMR_PT_CHUNK_SIZE==FIVMR_PT_NUM_INNER_BYTES);
#endif
    
    fivmr_Lock_init(&pt->u.ml.lock,prio);
    
    pt->u.ml.chunkListHead=(fivmr_PageChunkList*)fivmr_mallocAssert(FIVMR_PAGE_SIZE);
    bzero(pt->u.ml.chunkListHead,FIVMR_PAGE_SIZE);
    
    bzero(pt->u.ml.table,sizeof(pt->u.ml.table));
}

void fivmr_PageTable_free(fivmr_PageTable *pt) {
    if (FIVMR_PT_IS_FLAT(pt)) {
        uintptr_t ptSize;
        ptSize=(((pt->u.flat.size>>FIVMSYS_LOG_PAGE_SIZE)<<FIVMR_LOG_PT_BITS_PER_PAGE)
                +31)/32*sizeof(uint32_t);
        fivmr_free(pt->u.flat.table);
    } else {
        fivmr_PageChunkList *pcl;
        uintptr_t i;
        
        for (pcl=pt->u.ml.chunkListHead;pcl!=NULL;) {
            fivmr_PageChunkList *next;
            
            next=pcl->next;
            
            for (i=0;i<pcl->numChunks;++i) {
                fivmr_free(pcl->chunks[i].chunk);
            }
            fivmr_free(pcl);
            
            pcl=next;
        }
#if FIVMSYS_PTRSIZE==8
        /* FIXME: should be a better way... */
        for (i=0;i<FIVMR_PT_NUM_SPINE_ELE;++i) {
            if (pt->u.ml.table[i]!=NULL) {
                fivmr_free(pt->u.ml.table[i]);
            }
        }
#endif
    }
}

void fivmr_PageTable_freeNonZeroPages(fivmr_PageTable *pt) {
    fivmr_PTIterator iter;
    for (fivmr_PTIterator_init(&iter,pt);
         fivmr_PTIterator_valid(&iter);
         fivmr_PTIterator_next(&iter)) {
        uintptr_t baseAddr;
        uintptr_t wordI;
        uintptr_t wordILimit;
        uint32_t *chunk;
        baseAddr=iter.baseAddress;
        chunk=iter.chunk;
        wordILimit=iter.chunkLength;
        for (wordI=0;wordI<wordILimit;wordI++) {
            uint32_t curWord=chunk[wordI];
            if (curWord) {
                uintptr_t bitI;
                for (bitI=0;bitI<32;bitI+=FIVMR_PT_BITS_PER_PAGE) {
                    if (( curWord&((1<<FIVMR_PT_BITS_PER_PAGE)-1) )) {
                        uintptr_t curAddr=
                            baseAddr+
                            (((wordI<<(5-FIVMR_LOG_PT_BITS_PER_PAGE))+
                              (bitI>>FIVMR_LOG_PT_BITS_PER_PAGE))
                             <<FIVMSYS_LOG_PAGE_SIZE);
                        fivmr_assert(fivmr_PageTable_get(pt,curAddr)!=0);
                        fivmr_assert((curAddr&(FIVMR_PAGE_SIZE-1))==0);
                        fivmr_freePage((void*)curAddr);
                    }
                    curWord>>=FIVMR_PT_BITS_PER_PAGE;
                }
            }
        }
    }
}

void fivmr_PageTable_freePTAndNonZeroPages(fivmr_PageTable *pt) {
    fivmr_PageTable_freeNonZeroPages(pt);
    fivmr_PageTable_free(pt);
}

/* call this only while holding the global lock. */
static void addChunk(fivmr_PageTable *pt,
		     uintptr_t baseAddress,
		     uint32_t *chunk) {
    fivmr_PageChunkList *myList;
    
    /* NB. the fences allow people to scan the chunk lists without locking
       against the page table. */

    if (pt->u.ml.chunkListHead->numChunks==FIVMR_PAGE_CHUNKS_PER_LIST) {
	fivmr_PageChunkList *newList=(fivmr_PageChunkList*)fivmr_mallocAssert(FIVMR_PAGE_SIZE);
        bzero(newList,FIVMR_PAGE_SIZE);
	newList->next=pt->u.ml.chunkListHead;
	fivmr_fence();
	pt->u.ml.chunkListHead=newList;
    }
    
    myList=pt->u.ml.chunkListHead;
    
    myList->chunks[myList->numChunks].baseAddress=baseAddress;
    myList->chunks[myList->numChunks].chunk=chunk;
    fivmr_fence();
    myList->numChunks++;
}

void fivmr_PageTable_ensure(fivmr_PageTable *pt,
			    uintptr_t address) {
    uintptr_t bitsIdx;
#if FIVMSYS_PTRSIZE==8
    uint32_t **middle;
#endif
    
    fivmr_assert(!pt->isFlat);

    fivmr_Lock_lock(&pt->u.ml.lock);
#if FIVMSYS_PTRSIZE==4
    bitsIdx=(address&FIVMR_PT_SPINE_MASK)>>FIVMR_PT_SPINE_SHIFT;
    if (pt->u.ml.table[bitsIdx]==NULL) {
        uint32_t *subTable=(uint32_t*)fivmr_mallocAssert(FIVMR_PAGE_SIZE);
        bzero(subTable,FIVMR_PAGE_SIZE);
        fivmr_fence();
	pt->u.ml.table[bitsIdx]=subTable;
	addChunk(pt,address&FIVMR_PT_SPINE_MASK,pt->u.ml.table[bitsIdx]);
    }
#else
    bitsIdx=(address&FIVMR_PT_OUTER_MASK)>>FIVMR_PT_OUTER_SHIFT;
    if (pt->u.ml.table[bitsIdx]==NULL) {
        uint32_t **midTable=(uint32_t**)
            fivmr_mallocAssert(FIVMR_PT_NUM_MIDDLE_ELE*FIVMSYS_PTRSIZE);
        bzero(midTable,FIVMR_PT_NUM_MIDDLE_ELE*FIVMSYS_PTRSIZE);
        fivmr_fence();
	pt->u.ml.table[bitsIdx]=midTable;
    }
    middle=pt->u.ml.table[bitsIdx];
    bitsIdx=(address&FIVMR_PT_MIDDLE_MASK)>>FIVMR_PT_MIDDLE_SHIFT;
    if (middle[bitsIdx]==NULL) {
        uint32_t *subTable=(uint32_t*)fivmr_mallocAssert(FIVMR_PT_NUM_INNER_BYTES);
        bzero(subTable,FIVMR_PT_NUM_INNER_BYTES);
        fivmr_fence();
	middle[bitsIdx]=subTable;
	addChunk(pt,
		 address&(FIVMR_PT_OUTER_MASK|FIVMR_PT_MIDDLE_MASK),
		 middle[bitsIdx]);
    }
#endif
    fivmr_Lock_unlock(&pt->u.ml.lock);
}


/* this function *has* to be out-of-line otherwise GCC goes bananas. */
void fivmr_PTIterator_init(fivmr_PTIterator *pti,
                           fivmr_PageTable *pt) {
    bzero(pti,sizeof(fivmr_PTIterator));
    
    pti->pt=pt;
    if (FIVMR_PT_IS_FLAT(pt)) {
        pti->u.flat.first=true;
        pti->baseAddress=pt->u.flat.start;
        pti->chunk=pt->u.flat.table;
        pti->chunkLength=
            (((pt->u.flat.size>>FIVMSYS_LOG_PAGE_SIZE)
              <<FIVMR_LOG_PT_BITS_PER_PAGE)
             +31)
            /32;
    } else {
        pti->u.ml.cur=pt->u.ml.chunkListHead;
        fivmr_PTIterator_setPCLImpl(pti);
    }
}

# 1 "fivmr_payload.c"
/*
 * fivmr_payload.c
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

static fivmr_TypeStub **copyTypeList(fivmr_TypeStub **tdListOrig,
                                     int32_t listLen,
                                     void *region,
                                     fivmr_OTH *tdForward) {
    fivmr_TypeStub **tdListNew;
    int32_t j;
    
    if (listLen==0) {
        return NULL;
    }
    
    tdListNew=freg_region_alloc(region,
                                sizeof(fivmr_TypeStub*)*listLen);
    fivmr_assert(tdListNew!=NULL);
    
    for (j=0;j<listLen;++j) {
        tdListNew[j]=fivmr_OTH_get(tdForward,tdListOrig[j]);
    }
    
    return tdListNew;
}

static void fixObjHeader(fivmr_Object obj,
                         fivmr_Settings *settings,
                         fivmr_TypeData *td) {
    fivmr_ObjHeader_init(
        settings,
        fivmr_ObjHeader_forObject(settings,
                                  obj),
        (fivmr_Monitor*)td,
        FIVMR_OHF_ZERO);
}

fivmr_Payload *fivmr_Payload_copy(fivmr_Payload *orig) {
    fivmr_Payload *result;
    fivmr_OTH tdForward;
    fivmr_OTH mrForward;
    fivmr_OTH cmrForward;
    int32_t i,j;
    int32_t nMethods;
    uintptr_t curObj;
    uintptr_t objStep;
    uintptr_t stringArrayBaseOld;
    uintptr_t stringArrayBaseNew;
    uintptr_t stringArraySize;
    fivmr_TypeData **tdPtr;
    fivmr_Object stringArray;
    int32_t nTypes,nStubs;
    
    LOG(2,("begin payload copy"));

    fivmr_debugMemory();

    /* NOTE: this function is faster than I had feared but it still needs more
       perf tests and possibly perf optimizations... */
    
    /* assert that this is not a free'd immortal payload or a one-shot payload */
    fivmr_assert(orig->mode!=FIVMR_PL_INVALID);
    fivmr_assert(orig->mode!=FIVMR_PL_IMMORTAL_ONESHOT);
    
    result=freg_region_create(sizeof(fivmr_Payload));
    fivmr_assert(result!=NULL);
    
    memcpy(result,orig,sizeof(fivmr_Payload));
    
    result->mode=FIVMR_PL_COPY;
    
    nTypes=orig->nTypes;
    nStubs=orig->nStubs;

    /* first copy the type contexts */
    result->contexts=freg_region_alloc(result,
                                       sizeof(fivmr_StaticTypeContext)*orig->nContexts);
    fivmr_assert(result->contexts!=NULL);
    
    memcpy(result->contexts,
           orig->contexts,
           sizeof(fivmr_StaticTypeContext)*orig->nContexts);
    
    for (i=0;i<orig->nContexts;++i) {
        result->contexts[i].payload=result;
    }
    
    /* now copy all of the types */
    result->typeList=freg_region_alloc(result,
                                       sizeof(fivmr_TypeData*)*nTypes);
    fivmr_assert(result->typeList!=NULL);
    
    if (nStubs!=0) {
        result->stubList=freg_region_alloc(result,
                                           sizeof(fivmr_TypeData*)*nStubs);
        fivmr_assert(result->typeList!=NULL);
    }
    
    fivmr_OTH_initEasy(&tdForward,nTypes+nStubs);
    
    nMethods=0;
    
    LOG(2,("  begin copy types"));
    
    for (i=0;i<orig->nTypes;++i) {
        /* we copy: TypeDatas, MethodRecs, and FieldRecs */
        
        fivmr_TypeData *tdOrig;
        fivmr_TypeData *tdNew;
        
        tdOrig=orig->typeList[i];
        tdNew=freg_region_alloc(result,
                                fivmr_TypeData_sizeOfTypeData(tdOrig));
        fivmr_assert(tdNew!=NULL);
        result->typeList[i]=tdNew;
        
        memcpy(tdNew,tdOrig,fivmr_TypeData_sizeOfTypeData(tdOrig));
        
        fivmr_OTH_put(&tdForward,tdOrig,tdNew);
        
        /* fixup context pointer now */
        j=tdOrig->context-orig->contexts;
        fivmr_assert(j>=0 && j<orig->nContexts);
        tdNew->context=result->contexts+j;
        
        nMethods+=tdOrig->numMethods;
        
        /* fixup type bodies later */
    }
    
    memcpy(result->stubList,
           orig->stubList,
           sizeof(fivmr_TypeStub)*nStubs);
    
    for (i=0;i<orig->nStubs;++i) {
        fivmr_TypeStub *ts=result->stubList+i;
        j=ts->context-orig->contexts;
        fivmr_assert(j>=0 && j<orig->nContexts);
        ts->context=result->contexts+j;

        fivmr_OTH_put(&tdForward,orig->stubList+i,ts);
    }
    
    LOG(2,("  begin copy intrinsic types"));

    /* FIXME: this might break for some compilers */
    for (tdPtr=&result->td_top;tdPtr<=&result->td_ClassArr;tdPtr++) {
        *tdPtr=fivmr_OTH_get(&tdForward,*tdPtr);
    }
    
    fivmr_OTH_initEasy(&mrForward,nMethods);
    
    LOG(2,("  begin type fixup"));

    for (i=0;i<orig->nTypes;++i) {
        fivmr_TypeData *tdOrig;
        fivmr_TypeData *tdNew;
        
        tdOrig=orig->typeList[i];
        tdNew=result->typeList[i];
        
        tdNew->forward=tdNew;
        tdNew->parent=fivmr_OTH_get(&tdForward,tdNew->parent);
        
        tdNew->superInterfaces=(fivmr_TypeData**)
            copyTypeList((fivmr_TypeStub**)tdNew->superInterfaces,
                         tdNew->nSuperInterfaces,
                         result,
                         &tdForward);
        
        tdNew->ilist=(fivmr_TypeData**)
            copyTypeList((fivmr_TypeStub**)tdNew->ilist,
                         tdNew->ilistSize,
                         result,
                         &tdForward);
        
        tdNew->arrayElement=fivmr_OTH_get(&tdForward,tdNew->arrayElement);
        tdNew->arrayType=fivmr_OTH_get(&tdForward,tdNew->arrayType);
        
        if (tdNew->numMethods!=0) {
            tdNew->methods=
                freg_region_alloc(result,
                                  sizeof(fivmr_MethodRec*)*tdNew->numMethods);
            fivmr_assert(tdNew->methods!=NULL);
            for (j=0;j<tdOrig->numMethods;++j) {
                fivmr_MethodRec *mrOld;
                fivmr_MethodRec *mrNew;
                
                mrOld=tdOrig->methods[j];
                mrNew=freg_region_alloc(result,
                                        sizeof(fivmr_MethodRec));
                fivmr_assert(mrNew!=NULL);
                tdNew->methods[j]=mrNew;
                
                memcpy(mrNew,mrOld,sizeof(fivmr_MethodRec));
                
                fivmr_OTH_put(&mrForward,mrOld,mrNew);
                
                mrNew->owner=tdNew;
                mrNew->result=fivmr_OTH_get(&tdForward,mrNew->result);
                
                mrNew->params=copyTypeList(mrNew->params,
                                           mrNew->nparams,
                                           result,
                                           &tdForward);
            }
        }
        
        if (tdNew->numFields!=0) {
            tdNew->fields=
                freg_region_alloc(result,
                                  sizeof(fivmr_FieldRec)*tdNew->numFields);
            fivmr_assert(tdNew->fields!=NULL);
            
            memcpy(tdNew->fields,tdOrig->fields,
                   sizeof(fivmr_FieldRec)*tdNew->numFields);
            
            for (j=0;j<tdOrig->numFields;++j) {
                fivmr_FieldRec *frOld=tdOrig->fields+j;
                fivmr_FieldRec *frNew=tdNew->fields+j;
                
                frNew->owner=tdNew;
                frNew->type=fivmr_OTH_get(&tdForward,frNew->type);
            }
        }
    }
    
    LOG(2,("  begin handle debug table"));

    /* what we have left to fixup in types: the classObject pointer */
    
    fivmr_OTH_initEasy(&cmrForward,result->nDebugIDs); /* needed for
                                                          CompactMethodRec magic */

    /* copy debug records (and fixup to point to right MethodRecs) */
    result->debugTable=freg_region_alloc(result,
                                         sizeof(fivmr_DebugRec)*result->nDebugIDs);
    fivmr_assert(result->debugTable!=NULL);
    for (i=0;i<result->nDebugIDs;++i) {
        fivmr_DebugRec *drOld;
        fivmr_DebugRec *drNew;
        
        drOld=orig->debugTable+i;
        drNew=result->debugTable+i;
        
        drNew->ln_rm_c=drOld->ln_rm_c; /* thin or fat, we don't need to copy
                                          the FatDebugData */
        
        /* copy the low bits of method */
        /* FIXME: we should have a better way of managing these "special bits" ...
           this approach will get very annoying, very quickly. */
        drNew->method=drOld->method&(sizeof(void*)-1);
        
        if ((drNew->ln_rm_c&FIVMR_DR_INLINED)) {
            /* inlined code */
            fivmr_InlineMethodRec *imrOld;
            uintptr_t *imrNewPtr;
            fivmr_InlineMethodRec *imrNew;
            
            imrOld=(fivmr_InlineMethodRec*)(drOld->method&~(sizeof(void*)-1));
            imrNewPtr=&drNew->method;
            
            for (;;) {
                fivmr_MethodRec *inlinedMR;
                
                imrNew=freg_region_alloc(result,
                                         sizeof(fivmr_InlineMethodRec));
                fivmr_assert(imrNew!=NULL);
                *imrNewPtr|=(uintptr_t)imrNew;
                
                imrNew->ln_c=imrOld->ln_c;
                inlinedMR=fivmr_OTH_get(&mrForward,imrOld->method);
                if (inlinedMR==NULL) {
                    /* imrOld->method must be a CompactMethodRec - so do some magic. */
                    inlinedMR=fivmr_OTH_get(&cmrForward,imrOld->method);
                    if (inlinedMR==NULL) {
                        /* haven't handled this one yet - so create one */
                        inlinedMR=(fivmr_MethodRec*)
                            freg_region_alloc(result,
                                              sizeof(fivmr_CompactMethodRec));
                        fivmr_assert(inlinedMR!=NULL);
                        inlinedMR->owner=fivmr_OTH_get(&tdForward,imrOld->method->owner);
                        inlinedMR->name=imrOld->method->name;
                        inlinedMR->flags=imrOld->method->flags;
                        inlinedMR->result=fivmr_OTH_get(&tdForward,imrOld->method->result);
                        inlinedMR->nparams=imrOld->method->nparams;
                        inlinedMR->params=copyTypeList(imrOld->method->params,
                                                       imrOld->method->nparams,
                                                       result,
                                                       &tdForward);
                        fivmr_OTH_put(&cmrForward,imrOld->method,inlinedMR);
                    }
                }
                imrNew->method=inlinedMR;
                fivmr_assert(imrNew->method!=NULL);
                if ((imrOld->ln_c&FIVMR_IMR_INLINED)) {
                    imrOld=(fivmr_InlineMethodRec*)imrOld->caller;
                    imrNewPtr=(uintptr_t*)&imrNew->caller;
                    *imrNewPtr=0;
                } else {
                    imrNew->caller=(uintptr_t)
                        fivmr_OTH_get(&mrForward,(void*)imrOld->caller);
                    fivmr_assert(imrNew->caller!=0);
                    break;
                }
            }
        } else {
            /* in the outer method */
            drNew->method|=(uintptr_t)
                fivmr_OTH_get(&mrForward,(void*)(drOld->method&~(sizeof(void*)-1)));
            if (FIVMR_ASSERTS_ON && drNew->method==0) {
                LOG(0,("drNew->method=%p, drOld->method=%p, drNew=%p, drOld=%p",
                       drNew->method,
                       drOld->method,
                       drNew,
                       drOld));
                fivmr_assert(drNew->method!=0);
            }
        }
    }
    
    LOG(2,("  begin copy strings"));

    /* copy strings (and fixup objects to point to right TypeDatas) */
    objStep=fivmr_getStringDistance(orig);
    
    result->stringTable=freg_region_alloc(result,
                                          fivmr_alignRaw(objStep*orig->nStrings,
                                                         sizeof(int64_t)));
    fivmr_assert(result->stringTable!=NULL);
    
    memcpy(result->stringTable,
           orig->stringTable,
           fivmr_alignRaw(objStep*orig->nStrings,
                          sizeof(int64_t)));
    
    stringArray=(uintptr_t)orig->stringArray+FIVMR_ALLOC_OFFSET(&orig->settings);
    
    stringArraySize=
        fivmr_Object_size(&orig->settings,
                          stringArray);
    stringArrayBaseOld=(uintptr_t)orig->stringArray;
    stringArrayBaseNew=(uintptr_t)
        freg_region_alloc(result,
                          stringArraySize);
    fivmr_assert(stringArrayBaseNew!=0);
    
    memcpy((void*)stringArrayBaseNew,
           (void*)stringArrayBaseOld,
           stringArraySize);

    result->stringArray=(uintptr_t*)stringArrayBaseNew;
    stringArray=(uintptr_t)result->stringArray+FIVMR_ALLOC_OFFSET(&orig->settings);
    
    /* FIXME: for some libraries or object models, this will not be a charArr */
    fixObjHeader(stringArray,&orig->settings,result->td_charArr);
    
    curObj=fivmr_getFirstString(result);
    
    for (i=0;i<orig->nStrings;++i) {
        fixObjHeader(curObj,&orig->settings,result->td_String);

        *(fivmr_Object*)(curObj+orig->stringArrOffset)=stringArray;
        
        curObj+=objStep;
    }
    
    result->stringIndex=freg_region_alloc(result,
                                          sizeof(fivmr_Object)*orig->nStrings);
    fivmr_assert(result->stringIndex!=0);
    for (i=0;i<orig->nStrings;++i) {
        result->stringIndex[i]=
            orig->stringIndex[i]-(uintptr_t)orig->stringTable+(uintptr_t)result->stringTable;
    }
    
    LOG(2,("  begin copy bytecodes"));
    
    if (FIVMR_CLASSLOADING(&orig->settings)) {
        uintptr_t cur;
        
        result->bytecodeArray=freg_region_alloc(result,orig->bytecodeSize);
        fivmr_assert(result->bytecodeArray!=NULL);
        
        memcpy(result->bytecodeArray,
               orig->bytecodeArray,
               orig->bytecodeSize);
        
        for (cur=(uintptr_t)result->bytecodeArray;
             cur<(uintptr_t)result->bytecodeArray+orig->bytecodeSize;) {
            fivmr_Object obj=cur+FIVMR_ALLOC_OFFSET(&orig->settings);
            
            fixObjHeader(obj,&orig->settings,result->td_byteArr);
            
            cur+=fivmr_Object_size(&orig->settings,obj);
        }
        
        /* fix pointers from types to bytecode */
        for (i=0;i<result->nTypes;++i) {
            result->typeList[i]->bytecode=
                (uintptr_t)result->typeList[i]->bytecode -
                (uintptr_t)orig->bytecodeArray +
                (uintptr_t)result->bytecodeArray;
        }
    }
    
    LOG(2,("  begin copy classes"));

    /* copy classes (and fixup objects to point to right TypeDatas) */
    objStep=fivmr_getClassDistance(orig);
    
    result->classTable=freg_region_alloc(result,
                                         fivmr_alignRaw(objStep*orig->nTypes,
                                                        sizeof(int64_t)));
    fivmr_assert(result->classTable!=NULL);
    
    memcpy(result->classTable,
           orig->classTable,
           fivmr_alignRaw(objStep*orig->nTypes,
                          sizeof(int64_t)));
    
    curObj=fivmr_getFirstClass(result);
    
    for (i=0;i<orig->nTypes;++i) {
        fixObjHeader(curObj,&orig->settings,result->td_Class);
        
        result->typeList[i]->classObject=curObj;
        *(fivmr_TypeData**)(curObj+orig->classTDOffset)=result->typeList[i];
        
        curObj+=objStep;
    }

    /* fixup entrypoint */
    result->entrypoint=fivmr_OTH_get(&tdForward,result->entrypoint);
    
    /* and the patch repo */
    if (orig->patchRepoSize!=0) {
        result->patchRepo=freg_region_alloc(result,
                                            sizeof(void*)*orig->patchRepoSize);
        fivmr_assert(result->patchRepo!=NULL);
        memcpy(result->patchRepo,orig->patchRepo,
               sizeof(void*)*orig->patchRepoSize);
    }
    
    /* cleanup */
    fivmr_OTH_free(&tdForward);
    fivmr_OTH_free(&mrForward);
    fivmr_OTH_free(&cmrForward);
    
    fivmr_debugMemory();

    LOG(2,("  done."));

    return result;
}

bool fivmr_Payload_claim(fivmr_Payload *payload,
                         fivmr_VM *vm) {
    fivmr_assert(payload->mode!=FIVMR_PL_INVALID);
    if (payload->ownedBy==vm) {
        return true;
    } else {
        return fivmr_cas((uintptr_t*)&payload->ownedBy,
                         0,
                         (uintptr_t)vm);
    }
}

bool fivmr_Payload_registerVM(fivmr_Payload *payload,
                              fivmr_VM *vm) {
    int32_t i;
    
    if (!fivmr_Payload_claim(payload,vm)) {
        return false;
    }
    
    vm->payload=payload;
    if (payload->mode==FIVMR_PL_IMMORTAL_ONESHOT) {
        fivmr_assert(payload->primFields!=NULL);
        fivmr_assert(payload->refFields!=NULL);
        vm->primFields=payload->primFields;
        vm->refFields=payload->refFields;
    } else {
        fivmr_assert(payload->primFields==NULL);
        fivmr_assert(payload->refFields==NULL);
        vm->primFields=fivmr_mallocAssert(sizeof(int64_t)*payload->primFieldsLen);
        vm->refFields=fivmr_mallocAssert(sizeof(fivmr_Object)*payload->nRefFields);
    }
    
    vm->nTypes=payload->nTypes;
    
    vm->baseContexts=fivmr_mallocAssert(sizeof(fivmr_TypeContext*)*payload->nContexts);
    for (i=0;i<payload->nContexts;++i) {
        if (FIVMR_CLASSLOADING(&payload->settings)) {
            vm->baseContexts[i]=freg_region_create(sizeof(fivmr_TypeContext));
            fivmr_assert(vm->baseContexts[i]!=NULL);
        } else {
            vm->baseContexts[i]=fivmr_mallocAssert(sizeof(fivmr_TypeContext));
        }
        bzero(vm->baseContexts[i],
              sizeof(fivmr_TypeContext));
        memcpy(&vm->baseContexts[i]->st,
               payload->contexts+i,
               sizeof(fivmr_StaticTypeContext));
        vm->baseContexts[i]->vm=vm;
    }
    
    bzero(vm->primFields,sizeof(int64_t)*payload->primFieldsLen);
    bzero(vm->refFields,sizeof(fivmr_Object)*payload->nRefFields);
    
    for (i=0;i<payload->nTypes;++i) {
        uintptr_t j=payload->typeList[i]->context-payload->contexts;
        fivmr_assert(j<(uintptr_t)payload->nContexts);
        payload->typeList[i]->context=&vm->baseContexts[j]->st;
    }
    
    for (i=0;i<payload->nStubs;++i) {
        uintptr_t j=payload->stubList[i].context-payload->contexts;
        fivmr_assert(j<(uintptr_t)payload->nContexts);
        payload->stubList[i].context=&vm->baseContexts[j]->st;
    }
    
    vm->numBuckets=payload->numBuckets;
    vm->usedTids=fivmr_mallocAssert(sizeof(uint32_t)*(vm->numBuckets*256/32));
    memcpy(vm->usedTids,payload->usedTids,sizeof(uint32_t)*(vm->numBuckets*256/32));
    
    vm->itableSize=payload->itableSize;
    vm->itableOcc=fivmr_mallocAssert(sizeof(int32_t)*vm->itableSize);
    memcpy(vm->itableOcc,payload->itableOcc,sizeof(int32_t)*vm->itableSize);
    
    vm->flags|=FIVMR_VMF_USED_TIDS_MALLOCED|FIVMR_VMF_ITABLE_OCC_MALLOCED;
    
    memcpy(&vm->settings,
           &payload->settings,
           sizeof(fivmr_Settings));
    
    fivmr_GC_registerPayload(&vm->gc);
    
    return true;
}

void fivmr_Payload_free(fivmr_Payload *payload) {
    fivmr_assert(payload->mode!=FIVMR_PL_INVALID);
    if (payload->mode==FIVMR_PL_COPY) {
        freg_region_free(payload);
    } else {
        payload->mode=FIVMR_PL_INVALID;
    }
}


# 1 "fivmr_posix_allocexec.c"
/*
 * fivmr_posix_allocexec.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX

#include "fivmr.h"

/* derived from the bootMalloc in pizMalloc, by Filip Pizlo
   This is an extremely simple (and not particularly efficient!) first-fit
   allocator, originally used as a bootstrap allocator in my pizMalloc
   (which itself used BBoP segregated free lists and was very nice).  I've
   adapted it here as a very trivial and easy-to-validate allocator that
   has one noteworthy feature: memory returned by it can be used by a JIT,
   since the PROT_EXEC permission is set.

   It is possible, though unlikely, that we would have to replace this
   allocator at some point. But I doubt it.  It will perform very well if
   there is very little freeing activity, while still performing respectably
   if frees do occur.  That is what you'd expect, in the one scenario where
   this allocator is used: namely, malloc upon JITing and free on either
   code replacement or class unloading.  Thus free will be rare and clustured,
   a scenario in which this code will not suck too badly. */

bool fivmr_POSIX_logAllocExec;

#define ALIGNMENT 8
#define BOOT_MIN 16
#define HANDLE_ZERO 1
#define GRANULARITY (FIVMR_PAGE_SIZE*8) /* must be power of two! */

#define P(x) ((void*)(x))
#define S(x) ((char*)(x))
#define nop() do {} while(false)

static uintptr_t total;

static void *pizPageAllocP(size_t *needed) {
    void *result;
    /* printf("needed = %p, granularity = %p\n",(void*)*needed,(void*)GRANULARITY); */
    *needed=((*needed)+GRANULARITY-1)&~(GRANULARITY-1);
    if (fivmr_POSIX_logAllocExec) {
        fivmr_Log_printf("[AE: requesting %" PRIdPTR " bytes from OS (%" PRIdPTR " total)]\n",
                         *needed,total+*needed);
    }
    result=mmap(NULL,*needed,
		PROT_READ|PROT_WRITE|PROT_EXEC,
		MAP_ANON|MAP_PRIVATE,
		-1,0);
    if (result==(void*)-1) {
	fivmr_abortf("mmap in pizPageAllocP");
    }
    total+=*needed;
    return result;
}

static inline size_t *bootSize(void *ptr) {
    return (size_t*)ptr;
}

static inline void **bootNext(void *ptr) {
    return ((void**)ptr)+1;
}

#define bootHdr(x) (P(S(x)-sizeof(void*)))
#define bootObj(x) (P(S(x)+sizeof(void*)))

/* sorted singly-linked freelist */
static void *g_bootHead=NULL;
static fivmr_Lock g_lock;

void fivmr_POSIX_execAllocInit(void) {
    fivmr_Lock_init(&g_lock,FIVMR_PR_CRITICAL);
}

static void bootFree(void *ptr) {
    void *cur;
    ptr=bootHdr(ptr);
    if (S(ptr)+*bootSize(ptr)==g_bootHead) {
	/* coalesce with head (coalesce right) */
	*bootSize(ptr)+=*bootSize(g_bootHead);
	*bootNext(ptr)=*bootNext(g_bootHead);
	g_bootHead=ptr;
    } else if (ptr<g_bootHead || g_bootHead==NULL) {
	/* insert as new head */
	*bootNext(ptr)=g_bootHead;
	g_bootHead=ptr;
    } else {
	cur=g_bootHead;
	while (cur!=NULL) {
	    void *next=*bootNext(cur);
	    if (S(cur)+*bootSize(cur)==ptr) {
		if (S(ptr)+*bootSize(ptr)==next && next!=NULL) {
		    /* coalesce with me and next (coalesce both ways) */
		    *bootSize(cur)+=*bootSize(ptr)+*bootSize(next);
		    *bootNext(cur)=*bootNext(next);
		} else {
		    /* coalesce with me (coalesce left) */
		    *bootSize(cur)+=*bootSize(ptr);
		}
	    } else if (S(ptr)+*bootSize(ptr)==next && next!=NULL) {
		/* coalesce with next (coalesce right) */
		*bootNext(cur)=ptr;
		*bootSize(ptr)+=*bootSize(next);
		*bootNext(ptr)=*bootNext(next);
	    } else if (ptr<next || next==NULL) {
		/* insert here */
		*bootNext(cur)=ptr;
		*bootNext(ptr)=next;
	    } else {
		cur=next;
		continue;
	    }
	    break;
	}
    }
}

static void bootSysAlloc(size_t needed) {
    void *result=pizPageAllocP(&needed);
    *bootSize(result)=needed;
    bootFree(bootObj(result));
}

static void *bootRemove(void **ptr,size_t size) {
    size_t oldSize=*bootSize(*ptr);
    void *result=*ptr;
    if (oldSize-size>=BOOT_MIN) {
	*ptr=S(result)+size;
	*bootSize(*ptr)=oldSize-size;
	*bootNext(*ptr)=*bootNext(result);
	*bootSize(result)=size;
    } else {
	*ptr=*bootNext(result);
    }
    return result;
}

static size_t bootObjectSize(size_t size) {
    size=((size+ALIGNMENT-1)&~(ALIGNMENT-1))+sizeof(void*);
    if (size<BOOT_MIN) {
	size=BOOT_MIN;
    }
    return size;
}

static void *bootMalloc(size_t size) {
    void **cur;
    if (HANDLE_ZERO && size==0) {
	return NULL;
    }
    size=bootObjectSize(size);
redo:
    cur=&g_bootHead;
    while (*cur!=NULL) {
	if (*bootSize(*cur)>=size) {
	    return bootObj(bootRemove(cur,size));
	}
	cur=bootNext(*cur);
    }
    bootSysAlloc(size);
    goto redo;
}

static void bootDownsize(void *ptr,size_t newSize) {
    newSize=bootObjectSize(newSize);
    ptr=bootHdr(ptr);
    if (*bootSize(ptr)-newSize >= BOOT_MIN) {
        *bootSize(S(ptr)+newSize)=*bootSize(ptr)-newSize;
        bootFree(bootObj(S(ptr)+newSize));
    }
}

void *fivmr_allocExecutable(uintptr_t size) {
    void *result;
    if (false && fivmr_POSIX_logAllocExec) {
        fivmr_Log_printf("[AE: allocating %" PRIdPTR " bytes]\n",size);
    }
    fivmr_Lock_lock(&g_lock);
    result=bootMalloc((size_t)size);
    fivmr_Lock_unlock(&g_lock);
    return result;
}

void fivmr_downsizeExecutable(void *ptr,uintptr_t newSize) {
    fivmr_Lock_lock(&g_lock);
    bootDownsize(ptr,newSize);
    fivmr_Lock_unlock(&g_lock);
}

void fivmr_freeExecutable(void *ptr) {
    if (false && fivmr_POSIX_logAllocExec) {
        fivmr_Log_printf("[AE: freeing %" PRIdPTR " bytes]\n",*bootSize(bootHdr(ptr)));
    }
    fivmr_Lock_lock(&g_lock);
    bootFree(ptr);
    fivmr_Lock_unlock(&g_lock);
}

#endif

# 1 "fivmr_posix_flowlog.c"
/*
 * fivmr_posix_flowlog.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX && FIVMR_FLOW_LOGGING
#include "fivmr.h"

const char *fivmr_flowLogFile;

static FILE *flowLog;
static pthread_mutex_t flowLogLock;
static pthread_mutex_t flowLogDone;

void fivmr_FlowLog_platform_init(void) {
    fivmr_FlowLogHeader header = { FIVMR_FLOWLOG_MAGIC,
                                   FIVMR_FLOWLOG_VERSION,
                                   FIVMR_FLOWLOG_PLATFORM_POSIX };

    if (!fivmr_flowLogFile)
        return;

    /* FIXME: Priority? */
    pthread_mutex_init(&flowLogLock, NULL);
    pthread_mutex_init(&flowLogDone, NULL);

    flowLog = fopen(fivmr_flowLogFile, "w");
    if (flowLog == NULL) {
        LOG(0,("Warning: failed to open flow log file %s: %s",
               fivmr_flowLogFile, strerror(errno)));
        LOG(0,("Flow logging is disabled."));
        return;
    }

    fivmr_flowLogEnabled = 1;
    LOG(1,("Flow logging enabled, logging to %s", fivmr_flowLogFile));

    fwrite(&header, sizeof(header), 1, flowLog);
    fflush(flowLog);
}

void fivmr_FlowLog_platform_finalize(void) {
    /* FIXME: Iterate threads, flushing their flow log content */
    if (flowLog != NULL) {
        fclose(flowLog);
    }
    pthread_mutex_destroy(&flowLogLock);
    pthread_mutex_destroy(&flowLogDone);
}

void fivmr_FlowLog_lock(void) {
    pthread_mutex_lock(&flowLogLock);
}

void fivmr_FlowLog_unlock(void) {
    pthread_mutex_unlock(&flowLogLock);
}

/*
 * This is purely for shutdown synchronization.  The main flowlog thread
 * waits on this when it starts up, and then finalize waits on it to be
 * released at shutdown.
 */
void fivmr_FlowLog_wait(void) {
    pthread_mutex_lock(&flowLogDone);
}

void fivmr_FlowLog_notify(void) {
    pthread_mutex_unlock(&flowLogDone);
}

void fivmr_FlowLog_platform_write(fivmr_FlowLogBuffer *flowbuf) {
    if (!flowLog)
        return;
    fwrite(flowbuf->events, sizeof(fivmr_FlowLogEvent), flowbuf->entries, flowLog);
}

#endif /* FIVMR_POSIX */
# 1 "fivmr_posix_hardrtj.c"
/*
 * fivmr_posix_hardrtj.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX

#include <fivmr.h>

void fivmr_initHardRTJ(fivmr_VM *vm) {
    /* not supported - nothing to do */
}

#endif


# 1 "fivmr_posix_lock.c"
/*
 * fivmr_posix_lock.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX

#include "fivmr.h"

/*
 * What to do:
 * - we can use pthread_mutex/cond if we like.
 * - these should be prio-ceiling locks.
 * - the problem is with interrupts.  We should have the ability to
 *   create a lock with the ceiling being the "interrupt priority".
 *   when such a lock is acquired, interrupts are disabled.
 * - waiting on an interrupt-priority lock ... how do we do it?
 *   for sure, it only make sense to wait if we're not servicing an
 *   interrupt.
 *
 * What about this for interrupt priority locks:
 * - if servicing an interrupt, lock just disables interrupts and then
 *   we disallow calls to wait().
 * - if not servicing an interrupt, first acquire a lock (with highest
 *   priority allowed by RTEMS) and then disable interrupts.  wait()
 *   causes us to reenable interrupts and release the lock.
 * - does notify() from an interrupt work?  probably not...  but
 *   semaphore_release works.  hmmm...
 *
 * It seems that having a unified approach where all threads have a
 * semaphore for notification would be good.
 */

void fivmr_Lock_init(fivmr_Lock *lock,
		     fivmr_Priority priority) {
    pthread_mutexattr_t attr;
    int res, realprio;
    realprio=priority;
    res=pthread_mutexattr_init(&attr);
    fivmr_assert(res==0);
    if (FIVMR_PR_SUPPORTED) {
#ifdef HAVE_PTHREAD_PIP
        if (priority==FIVMR_PR_INHERIT) {
            LOG(10,("creating priority inheritance lock"));
            res=pthread_mutexattr_setprotocol(&attr,PTHREAD_PRIO_INHERIT);
            fivmr_assert(res==0);
        }
#endif
#ifdef HAVE_PTHREAD_PCEP
        if (priority!=FIVMR_PR_INHERIT && priority!=FIVMR_PR_NONE) {
            LOG(10,("creating priority ceiling lock"));
            fivmr_assert(priority>=FIVMR_PR_MIN);
            fivmr_assert(priority<=FIVMR_PR_CRITICAL);
            if (priority==FIVMR_PR_CRITICAL) {
                LOG(10,("critical lock requested, using max priority for now"));
                realprio=FIVMR_PR_MAX;
            }
            res=pthread_mutexattr_setprotocol(&attr,PTHREAD_PRIO_PROTECT);
            fivmr_assert(res==0);
            res=pthread_mutexattr_setprioceiling(&attr,realprio);
            fivmr_assert(res==0);
        }
#endif
    }
    res=pthread_mutex_init(&(lock->mutex),NULL);
    fivmr_assert(res==0);
    pthread_cond_init(&(lock->cond),NULL);
    lock->holder=fivmr_ThreadHandle_zero();
    lock->recCount=0;
    lock->priority=realprio;
}

void fivmr_Lock_destroy(fivmr_Lock *lock) {
    pthread_mutex_destroy(&lock->mutex);
    pthread_cond_destroy(&lock->cond);
}

bool fivmr_Lock_legalToAcquire(fivmr_Lock *lock) {
    fivmr_assert(!fivmr_Thread_isCritical());
    return true;
}

void fivmr_Lock_lock(fivmr_Lock *lock) {
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    if (lock->holder==me) {
	/* sanity check to eliminate the possibility of infinite lock recursion
	   because of a missing unlock statement. */
	fivmr_assert(lock->recCount<100);
	
	lock->recCount++;
    } else {
	fivmr_assert(!fivmr_Thread_isCritical());
	
	pthread_mutex_lock(&(lock->mutex));
	lock->holder=me;
	lock->recCount=1;
    }
}

void fivmr_Lock_unlock(fivmr_Lock *lock) {
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    fivmr_assert(lock->recCount>=1);
    fivmr_assert(lock->holder==me);
    if (lock->recCount==1) {
	lock->recCount=0;
	lock->holder=fivmr_ThreadHandle_zero();
	pthread_mutex_unlock(&(lock->mutex));
    } else {
	lock->recCount--;
    }
}

void fivmr_Lock_broadcast(fivmr_Lock *lock) {
    pthread_cond_broadcast(&(lock->cond));
}

void fivmr_Lock_lockedBroadcast(fivmr_Lock *lock) {
    fivmr_Lock_lock(lock);
    pthread_cond_broadcast(&(lock->cond));
    fivmr_Lock_unlock(lock);
}

void fivmr_Lock_wait(fivmr_Lock *lock) {
    int32_t savedRecCount;
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();

    fivmr_assert(lock->holder==me);
    fivmr_assert(lock->recCount>=1);

    fivmr_assert(!fivmr_Thread_isCritical());

    savedRecCount=lock->recCount;

    lock->recCount=0;
    lock->holder=fivmr_ThreadHandle_zero();

    pthread_cond_wait(&(lock->cond),&(lock->mutex));
    
    lock->recCount=savedRecCount;
    lock->holder=me;
}

void fivmr_Lock_timedWaitAbs(fivmr_Lock *lock,
			     fivmr_Nanos whenAwake) {
    struct timespec ts;
    int savedRecCount;
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();

    fivmr_assert(lock->holder==me);
    fivmr_assert(lock->recCount>=1);

    fivmr_assert(!fivmr_Thread_isCritical());

    ts.tv_sec = (time_t)(whenAwake/1000000000LL);
    ts.tv_nsec = (long)(whenAwake%1000000000LL);

    savedRecCount=lock->recCount;

    lock->recCount=0;
    lock->holder=fivmr_ThreadHandle_zero();

    pthread_cond_timedwait(&(lock->cond),&(lock->mutex),&ts);

    lock->recCount=savedRecCount;
    lock->holder=me;
}

void fivmr_Lock_timedWaitRel(fivmr_Lock *lock,
			     fivmr_Nanos howLong) {
    fivmr_Lock_timedWaitAbs(lock,fivmr_curTime()+howLong);
}

#endif
# 1 "fivmr_posix_log.c"
/*
 * fivmr_posix_log.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX
#include "fivmr.h"

static pthread_mutex_t logLock;
int32_t fivmr_logLevel=0;
const char *fivmr_logFile;
FILE *fivmr_log;

void fivmr_Log_init(void) {
    pthread_mutex_init(&logLock,NULL);
    if (fivmr_log==NULL) {
	fivmr_log=stderr;
	if (fivmr_logFile!=NULL) {
	    FILE *log=fopen(fivmr_logFile,"a");
	    if (log==NULL) {
		LOG(0,("Warning: failed to open log file %s: %s",
		       fivmr_logFile,strerror(errno)));
		LOG(0,("Logging to console instead."));
	    } else {
#if defined(HAVE_SETVBUF)
                setvbuf(log,NULL,_IONBF,0);
#endif
		fivmr_log=log;
	    }
	}
    }
    LOG(1,("%s %s %s, All Rights Reserved",fivmr_name(),fivmr_version(),fivmr_copyright()));
}

void fivmr_Log_lock(void) {
    pthread_mutex_lock(&logLock);
}

void fivmr_Log_unlock(void) {
    pthread_mutex_unlock(&logLock);
}

void fivmr_Log_vprintf(const char *msg,va_list lst) {
    int res;
    res=vfprintf(fivmr_log,msg,lst);
    if (res<0) {
	if (fivmr_log!=stderr) {
	    fivmr_log=stderr;
	    fivmr_logLevel=0;
	    LOG(0,("Could no longer send log output to %s because: %s",
		   fivmr_logFile,strerror(errno)));
	    LOG(0,("Lowered log level to 0, sending further log output to stderr."));
	} else {
	    /* can't print? we're already dead. */
	    abort();
	}
    }
}

#endif
# 1 "fivmr_posix_module.c"
/*
 * fivmr_posix_module.c
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

#include <fivmr_config.h>
#if FIVMR_DYN_LOADING && FIVMR_POSIX

#include "fivmr.h"
#include <dlfcn.h>

fivmr_Lock fivmr_Module_lock;

void fivmr_Module_init(void) {
    fivmr_Lock_init(&fivmr_Module_lock,
		    FIVMR_PR_CRITICAL);
}

static void setLastError(void) {
    const char *e=dlerror();
    LOG(15,("setLastError has e = %s",e));
    fivmr_tsprintf("%s",e);
}

fivmr_ModuleHandle fivmr_Module_load(const char *path) {
    fivmr_ModuleHandle result=dlopen(path,RTLD_LAZY|RTLD_LOCAL);
    if (result==NULL) {
        setLastError();
	LOG(3,("dlopen on %s failed because: %s",
	       path,fivmr_Module_getLastError()));
    }
    return result;
}

bool fivmr_Module_unload(fivmr_ModuleHandle handle) {
    if (dlclose(handle)!=0) {
        setLastError();
        return false;
    } else {
        return true;
    }
}

void *fivmr_Module_lookup(fivmr_ModuleHandle handle,
                          const char *name) {
    void *result=dlsym(handle,name);
    if (result==NULL) {
        setLastError();
	LOG(3,("dlsym on %p/%s failed because: %s",
	       handle,name,fivmr_Module_getLastError()));
    }
    return result;
}

void *fivmr_Module_getFunction(fivmr_ModuleHandle handle,
			       const char *funcName) {
    /* currently we don't have any underscore-adding hacks, but we may in the
       future. */
    return fivmr_Module_lookup(handle,funcName);
}

const char *fivmr_Module_getLastError(void) {
    return fivmr_getThreadStringBuf();
}

#endif
# 1 "fivmr_posix_nanos.c"
/*
 * fivmr_posix_nanos.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX

#include "fivmr.h"
#include <sys/time.h>

fivmr_Nanos fivmr_nanosResolution() {
#ifdef HAVE_CLOCK_GETTIME
    struct timespec ts;

    clock_getres(CLOCK_REALTIME, &ts);

    return ts.tv_nsec;
#else
    return 1000;
#endif
}

fivmr_Nanos fivmr_curTimePrecise(void) {
    fivmr_Nanos result;

#ifdef HAVE_CLOCK_GETTIME
    struct timespec ts;

    clock_gettime(CLOCK_REALTIME, &ts);
    
    result=ts.tv_sec;
    result*=1000;
    result*=1000;
    result*=1000;
    result+=ts.tv_nsec;
#else
    struct timeval tv;

    gettimeofday(&tv,NULL);
    
    result=tv.tv_sec;
    result*=1000;
    result*=1000;
    result+=tv.tv_usec;
    result*=1000;
#endif
    
    return result;
}

fivmr_Nanos fivmr_curTime(void) {
    return fivmr_curTimePrecise();
}

#endif
# 1 "fivmr_posix_suspend.c"
/*
 * fivmr_posix_suspend.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX

#include "fivmr.h"

static fivmr_ThreadSpecific suspThread;

#define MYSIGNAL SIGUSR1

static void sigusr1_handler(int sig) {
    LOG(2,("signal %d received; suspending thread %p.",
           MYSIGNAL,pthread_self()));
    fivmr_SuspensionData *data=
        (fivmr_SuspensionData*)fivmr_ThreadSpecific_get(&suspThread);
    
    fivmr_assert(data->magic==0x1234babe);
    fivmr_assert(data->thread==fivmr_ThreadHandle_current());
    LOG(11,("data address: %p",data));
    LOG(11,("thread according to me: %p",pthread_self()));
    LOG(11,("thread according to data: %p",data->thread));
    LOG(11,("data's magic: %p",data->magic));

    if (false) {
        /* FIXME: priority inversion! */
        fivmr_Semaphore_up(&data->suspended);
        LOG(11,("thread %p notifying of suspension on %p with data %p",
                pthread_self(),&data->suspended,data));
    }
    fivmr_Semaphore_down(&data->wakeUp);
    LOG(2,("Wake-up received; resuming."));
}

static void destructor(uintptr_t arg) {
    fivmr_SuspensionData *data=(fivmr_SuspensionData*)arg;
    fivmr_Semaphore_destroy(&data->wakeUp);
    fivmr_Semaphore_destroy(&data->suspended);
}

void fivmr_initSuspensionManager(void) {
    struct sigaction sa;

    bzero(&sa,sizeof(sa));
    sa.sa_handler=sigusr1_handler;
    sigfillset(&sa.sa_mask);
    sa.sa_flags=SA_SIGINFO;
    sigaction(MYSIGNAL,&sa,NULL);
    
    fivmr_ThreadSpecific_initWithDestructor(&suspThread,
                                            destructor);
    LOG(1,("signal %d suspension handler initialized and thread-specific created.",
           MYSIGNAL));
}

fivmr_SuspendableThreadHandle fivmr_Thread_makeSuspendable(fivmr_SuspensionData *data) {
    int res;
    fivmr_assert(fivmr_ThreadSpecific_get(&suspThread)==0);
    data->magic=0x1234babe;
    data->thread=fivmr_ThreadHandle_current();
    fivmr_Semaphore_init(&data->wakeUp);
    fivmr_Semaphore_init(&data->suspended);
    fivmr_ThreadSpecific_set(&suspThread,(uintptr_t)data);
    LOG(11,("setting thread specific in %p to %p",
           fivmr_ThreadHandle_current(),
           data));
    LOG(11,("have set thread specific in %p to %p",
           fivmr_ThreadHandle_current(),
           fivmr_ThreadSpecific_get(&suspThread)));
    return data;
}

void fivmr_Thread_suspend(fivmr_SuspendableThreadHandle th) {
    pthread_kill(th->thread,MYSIGNAL);
    /* OK - suspension in progress */
    if (false) {
        LOG(11,("waiting for suspension to happen on %p with data %p for %p",&th->suspended,th,th->thread));
        fivmr_Semaphore_down(&th->suspended);
        LOG(11,("OK, suspended"));
    }
}

void fivmr_Thread_resume(fivmr_SuspendableThreadHandle th) {
    
    fivmr_Semaphore_up(&th->wakeUp);
}

#endif
# 1 "fivmr_posix_sysdep.c"
/*
 * fivmr_posix_sysdep.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX
#include "fivmr.h"
#include <stdarg.h>

fivmr_Priority FIVMR_PR_NONE;
fivmr_Priority FIVMR_PR_INHERIT;
fivmr_Priority FIVMR_PR_MIN;
fivmr_Priority FIVMR_PR_MAX;
fivmr_Priority FIVMR_PR_CRITICAL;
fivmr_ThreadPriority FIVMR_TPR_NORMAL_MIN;
fivmr_ThreadPriority FIVMR_TPR_NORMAL_MAX;
fivmr_ThreadPriority FIVMR_TPR_FIFO_MIN;
fivmr_ThreadPriority FIVMR_TPR_FIFO_MAX;
fivmr_ThreadPriority FIVMR_TPR_RR_MIN;
fivmr_ThreadPriority FIVMR_TPR_RR_MAX;
bool FIVMR_PR_SUPPORTED;

bool fivmr_fakeRTPriorities;

static int sysPageSize;

uintptr_t fivmr_POSIX_threadStackSize;

void fivmr_SysDep_init(void) {
    pthread_attr_t tattr;
    size_t size=0;
    
    LOG(1,("VM instance at pid = %d",getpid()));
    
    fivmr_SysDepUtil_init();
    
    pthread_attr_init(&tattr);
    pthread_attr_getstacksize(&tattr,&size);
    fivmr_assert(size!=0);
    fivmr_POSIX_threadStackSize=(uintptr_t)size;
    
    /* OSX defines _POSIX_THREAD_PRIORITY_SCHEDULING, but does not
       several functions which this should indicate.  However, it does
       give us sched_get_priority{min,max}(). */

    FIVMR_PR_SUPPORTED=false; /* assume it doesn't work until we prove it does */
    
#if defined(FIVMR_HAVE_POSIX_SCHEDULING) || defined(_POSIX_THREAD_PRIORITY_SCHEDULING)
    /* POSIX 1003.1b says that the range for mutex priorities is the same
       as that of SCHED_FIFO */
    FIVMR_PR_MIN=sched_get_priority_min(SCHED_FIFO);
    FIVMR_PR_MAX=sched_get_priority_max(SCHED_FIFO);
    if (FIVMR_PR_MIN<0 || FIVMR_PR_MAX<0) {
	LOG(1,("Warning: POSIX scheduling seems not to work right."));
        FIVMR_PR_SUPPORTED=false;
	FIVMR_PR_MIN=1;
	FIVMR_PR_MAX=100;
    } else {
        int oldsched;
        struct sched_param oldparam;
        struct sched_param newparam;
#  if defined(FIVMR_HAVE_POSIX_SCHEDULING)
	LOG(1,("POSIX scheduling detected, min = %d, max = %d",
	       FIVMR_PR_MIN,FIVMR_PR_MAX));
        
        /* now see if it really works */
        oldsched=sched_getscheduler(0);
        if (oldsched>=0) {
            bzero(&oldparam,sizeof(oldparam));
            bzero(&newparam,sizeof(newparam));
            if (sched_getparam(0,&oldparam)==0) {
                newparam.sched_priority=FIVMR_PR_MAX;
                if (sched_setscheduler(0,SCHED_FIFO,&newparam)==0) {
                    LOG(1,("attempt to run at FIFO_MAX priority successful; reverting "
                           "priority/scheduler to %d/%d and continuing.",
                           oldparam.sched_priority,oldsched));
                    FIVMR_PR_SUPPORTED=true;
                    
                    if (sched_setscheduler(0,oldsched,&oldparam)!=0) {
                        fivmr_abort("Could not revert to default scheduler while at FIFO_MAX "
                                    "priority!  Aborting to avoid system deadlock.");
                    }
                } else {
                    LOG(1,("POSIX scheduling doesn't support sched_setscheduler "
                           "-- DEACTIVATING!"));
                }
            } else {
                LOG(1,("POSIX scheduling doesn't support sched_getparam -- DEACTIVATING!"));
            }
        } else {
            LOG(1,("POSIX scheduling doesn't support sched_getscheduler -- DEACTIVATING!"));
        }
#  else
        LOG(1,("POSIX priorities probed but scheduling not completely functional, min = %d, max = %d",
               FIVMR_PR_MIN,FIVMR_PR_MAX));
#  endif
    }
    FIVMR_TPR_NORMAL_MIN=FIVMR_THREADPRIORITY(FIVMR_TPR_NORMAL,
                                              sched_get_priority_min(SCHED_OTHER));
    FIVMR_TPR_NORMAL_MAX=FIVMR_THREADPRIORITY(FIVMR_TPR_NORMAL,
                                              sched_get_priority_max(SCHED_OTHER));
    FIVMR_TPR_FIFO_MIN=FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO,
                                            sched_get_priority_min(SCHED_FIFO));
    FIVMR_TPR_FIFO_MAX=FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO,
                                            sched_get_priority_max(SCHED_FIFO));
    FIVMR_TPR_RR_MIN=FIVMR_THREADPRIORITY(FIVMR_TPR_RR,
                                          sched_get_priority_min(SCHED_RR));
    FIVMR_TPR_RR_MAX=FIVMR_THREADPRIORITY(FIVMR_TPR_RR,
                                          sched_get_priority_max(SCHED_RR));
    fivmr_assert(FIVMR_TPR_NORMAL_MIN<=FIVMR_TPR_NORMAL_MAX);
    fivmr_assert(FIVMR_TPR_FIFO_MIN<FIVMR_TPR_FIFO_MAX);
    fivmr_assert(FIVMR_TPR_RR_MIN<FIVMR_TPR_FIFO_MAX);
#else
    LOG(1,("Warning: we don't have POSIX scheduling."));
    FIVMR_PR_SUPPORTED=false;
    FIVMR_PR_MIN=1;
    FIVMR_PR_MAX=100;
    /* FIXME set the TPR levels to something?  so that does computations on them doesn't
       break? */
#endif
    
    FIVMR_PR_CRITICAL=FIVMR_PR_MAX+1;
    fivmr_assert(FIVMR_PR_MIN>=0);
    fivmr_assert(FIVMR_PR_MAX>=0);
    fivmr_assert(FIVMR_PR_CRITICAL>=0);
    FIVMR_PR_INHERIT=FIVMR_PR_MIN-1;
    FIVMR_PR_NONE=FIVMR_PR_MIN-2;
    
#if FIVMR_DYN_LOADING
    fivmr_Module_init();
#endif
    fivmr_Thread_init();
    
    sysPageSize=getpagesize();
    
    fivmr_assert(FIVMR_PAGE_SIZE>=sysPageSize);
    fivmr_assert((FIVMR_PAGE_SIZE%sysPageSize)==0);
    
    LOG(1,("System page size is %p; our page size is %p.",
           sysPageSize,FIVMR_PAGE_SIZE));
    
    if (FIVMR_PAGE_SIZE>sysPageSize) {
        LOG(1,("WARNING: System page size is smaller than our page size.  Virtual "
               "memory will be wasted to achieve alignment.  Suggest building with "
               "--g-self-man-mem."));
    }
    
    fivmr_POSIX_execAllocInit();
    
    LOG(1,("Executable allocator initialized."));
}

uintptr_t fivmr_abortf(const char *fmt,...) {
    va_list lst;
    va_start(lst,fmt);
    fprintf(stderr,"fivmr ABORT: ");
    vfprintf(stderr,fmt,lst);
    fprintf(stderr,"\n");
    va_end(lst);
    fprintf(stderr,"fivmr VM instance at pid = %d dumping core.\n",getpid());
    abort();
    return 0;
}

uintptr_t fivmr_abort(const char *message) {
    if (message==NULL) {
	message=
	    "(message was NULL; typically implies Java exception "
	    "during attempt to abort)";
    }
    fprintf(stderr,"fivmr ABORT: %s\n",message);
    fprintf(stderr,"fivmr VM instance at pid = %d dumping core.\n",getpid());
    abort();
    return 0;
}

void fivmr_yield(void) {
    sched_yield();
}

void *fivmr_tryAllocPages_IMPL(uintptr_t numPages,
                               bool *isZero,
                               const char *whereFile,
                               int whereLine) {
    void *result;

    LOG(6,("fivmr_tryAllocPages(%p) called from %s:%d.",numPages,whereFile,whereLine));
    if (sysPageSize==FIVMR_PAGE_SIZE) {
        result=mmap(NULL,numPages<<FIVMSYS_LOG_PAGE_SIZE,
                    PROT_READ|PROT_WRITE,MAP_ANON|MAP_PRIVATE,
                    -1,0);
        if (result==(void*)-1) {
            LOG(5,("fivmr_tryAllocPages(%p) from %s:%d failing.",numPages,whereFile,whereLine));
            return NULL;
        }
    } else {
        void *alloced;
        alloced=mmap(NULL,(numPages+1)<<FIVMSYS_LOG_PAGE_SIZE,
                     PROT_READ|PROT_WRITE,MAP_ANON|MAP_PRIVATE,
                     -1,0);
        if (alloced==(void*)-1) {
            LOG(5,("fivmr_tryAllocPages(%p) from %s:%d failing.",numPages,whereFile,whereLine));
            return NULL;
        }
        result=(void*)((((uintptr_t)alloced)+FIVMR_PAGE_SIZE)&~(FIVMR_PAGE_SIZE-1));
        ((void**)result)[-1]=alloced;
    }
    
    if (isZero!=NULL) {
        *isZero=true;
    }
    
    LOG(5,("fivmr_tryAllocPages(%p) returning %p",numPages,result));

    return result;
}

void fivmr_freePages_IMPL(void *begin,uintptr_t numPages,
                          const char *whereFile,
                          int whereLine) {
    int res;
    LOG(6,("fivmr_freePages(%p,%p) called from %s:%d.",begin,numPages,whereFile,whereLine));
    if (sysPageSize==FIVMR_PAGE_SIZE) {
        LOG(5,("calling munmap(%p,%" PRIuPTR ")",
               begin,numPages<<FIVMSYS_LOG_PAGE_SIZE));
        res=munmap(begin,numPages<<FIVMSYS_LOG_PAGE_SIZE);
        fivmr_assert(res==0);
    } else {
        void *alloced=((void**)begin)[-1];
        LOG(5,("calling munmap(%p,%" PRIuPTR ")",
               alloced,(numPages+1)<<FIVMSYS_LOG_PAGE_SIZE));
        res=munmap(alloced,(numPages+1)<<FIVMSYS_LOG_PAGE_SIZE);
        fivmr_assert(res==0);
    }
}

int32_t fivmr_readByte(int fd) {
    uint8_t result;
    int res;
    res=read(fd,&result,1);
    if (res<0) {
	fivmr_assert(res==-1);
	return -2;
    } else if (res==0) {
	return -1;
    } else {
	return (int32_t)(uint32_t)result;
    }
}

int32_t fivmr_writeByte(int fd,int8_t b) {
    return write(fd,&b,1);
}

void fivmr_debugMemory_IMPL(const char *flnm,
                            int line) {
    /* don't know how to do it */
}

#endif

# 1 "fivmr_posix_thread.c"
/*
 * fivmr_posix_thread.c
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

#include <fivmr_config.h>
#if FIVMR_POSIX

#include "fivmr.h"

int32_t fivmr_Thread_affinity;

fivmr_ThreadHandle fivmr_Thread_integrate(void) {
    if (fivmr_Thread_affinity) {
        fivmr_CPUSet set;
	int i;
        bool res;
        fivmr_CPUSet_clear(&set);
	for (i=0;i<32;++i) {
	    if (fivmr_Thread_affinity&(1<<i)) {
		fivmr_CPUSet_set(&set,i);
	    }
	}
	res=fivmr_Thread_setAffinity(pthread_self(), &set);
	fivmr_assert(res);
    }

    return pthread_self();
}

typedef struct {
    void (*threadMain)(void *arg);
    void *arg;
} RunData;

static void *runner(void *arg_) {
    RunData *rd;
    void (*threadMain)(void *arg);
    void *arg;
    
    rd=(RunData*)arg_;
    
    threadMain=rd->threadMain;
    arg=rd->arg;
    
    free(rd);
    
    fivmr_Thread_integrate();
    
    threadMain(arg);
    
    return NULL;
}

fivmr_ThreadHandle fivmr_Thread_spawn(void (*threadMain)(void *arg),
				      void *arg,
				      fivmr_ThreadPriority priority) {
    pthread_t t;
    pthread_attr_t attr;
    struct sched_param schedparam;
    RunData *rd;
    int res, policy, pprio;
    
    rd=fivmr_mallocAssert(sizeof(RunData));
    rd->threadMain=threadMain;
    rd->arg=arg;
    
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr,PTHREAD_CREATE_DETACHED);

    if (FIVMR_PR_SUPPORTED) {
#ifdef HAVE_PTHREAD_ATTR_SETINHERITSCHED
        res=pthread_attr_setinheritsched(&attr,PTHREAD_EXPLICIT_SCHED);
        fivmr_assert(res==0);
#endif
        fivmr_ThreadPriority_to_posix(priority, &policy, &pprio);
        res=pthread_attr_setschedpolicy(&attr,policy);
        fivmr_assert(res==0);
        schedparam.sched_priority=pprio;
        res=pthread_attr_setschedparam(&attr,&schedparam);
        fivmr_assert(res==0);
    }
    
    res=pthread_create(&t,&attr,runner,rd);
    fivmr_assert(res==0);
    
    return t;
}

void fivmr_Thread_exit(void) {
    fivmr_assert(fivmr_Thread_canExit());
    pthread_exit(NULL);
}

void fivmr_Thread_init() {
    /* nothing to init for pthreads */
}

void fivmr_Thread_setPriority(fivmr_ThreadHandle t,
                              fivmr_ThreadPriority priority) {
    if (FIVMR_PR_SUPPORTED) {
        struct sched_param schedparam;
        int policy, pprio, res;
        if (false) {
            char buf[32];
            fivmr_ThreadPriority_describe(priority,buf,sizeof(buf));
            printf("setting priority to %s\n",buf);
        }
        fivmr_ThreadPriority_to_posix(priority, &policy, &pprio);
        schedparam.sched_priority=pprio;
        res=pthread_setschedparam(t,policy,&schedparam);
        fivmr_assert(res==0);
    }
}

fivmr_ThreadPriority fivmr_Thread_getPriority(fivmr_ThreadHandle t) {
    struct sched_param schedparam;
    int policy;
    int res;
    res=pthread_getschedparam(t,&policy,&schedparam);
    fivmr_assert(res==0);
    return posix_to_fivmr_ThreadPriority(policy,schedparam.sched_priority);
}

#if defined(HAVE_PTHREAD_SETAFFINITY_NP)
bool fivmr_Thread_setAffinity(fivmr_ThreadHandle h,
                              fivmr_CPUSet *cpuset) {
    return pthread_setaffinity_np(h,sizeof(cpu_set_t),cpuset)==0;
}
#endif

#endif
# 1 "fivmr_profile.c"
/*
 * fivmr_profile.c
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

#if FIVMR_PROFILE_REFLECTION
fivmr_Nanos fivmr_PR_invokeTime;
fivmr_Nanos fivmr_PR_initTime;
#endif

void fivmr_PR_dump(void) {
#if FIVMR_PROFILE_REFLECTION
    fivmr_Log_printf("Time spent in reflection:\n");
    fivmr_Log_printf("   Time spent invoking: %" PRIu64 " ns\n",
                     fivmr_PR_invokeTime);
    fivmr_Log_printf("    Time spent initing: %" PRIu64 " ns\n",
                     fivmr_PR_initTime);
#endif
}

# 1 "fivmr_random.c"
/*
 * fivmr_random.c
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

/* implementation of Marsenne Twister.

   Based on:
   
   A C-program for MT19937, with initialization improved 2002/1/26.
   Coded by Takuji Nishimura and Makoto Matsumoto.

   Before using, initialize the state by using init_genrand(seed)  
   or init_by_array(init_key, key_length).

   Copyright (C) 1997 - 2002, Makoto Matsumoto and Takuji Nishimura,
   All rights reserved.                          

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:

     1. Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.

     2. Redistributions in binary form must reproduce the above copyright
        notice, this list of conditions and the following disclaimer in the
        documentation and/or other materials provided with the distribution.

     3. The names of its contributors may not be used to endorse or promote 
        products derived from this software without specific prior written 
        permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
   CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


   Any feedback is very welcome.
   http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html
   email: m-mat @ math.sci.hiroshima-u.ac.jp (remove space) */

#include <fivmr.h>

static void init_base(fivmr_Random *r) {
    r->last=0;
    r->lastLeft=0;
}

void fivmr_Random_init(fivmr_Random *r) {
    fivmr_Random_initBySeed(r,(uint32_t)(fivmr_curTime()/1000000));
}

void fivmr_Random_initBySeed(fivmr_Random *r,uint32_t s) {
    init_base(r);
    r->mt[0]= s & (uint32_t)0xffffffffUL;
    for (r->mti=1; r->mti<FIVMR_RANDMT_N; r->mti++) {
        r->mt[r->mti] = 
	    ((uint32_t)1812433253UL * (r->mt[r->mti-1] ^ (r->mt[r->mti-1] >> 30)) + r->mti); 
        /* See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier. */
        /* In the previous versions, MSBs of the seed affect   */
        /* only MSBs of the array mt[].                        */
        /* 2002/01/09 modified by Makoto Matsumoto             */
    }
}

void fivmr_Random_initByArray(fivmr_Random *r,uint32_t *initKey,uint32_t keyLength) {
    uint32_t i, j, k;
    fivmr_Random_initBySeed(r,(uint32_t)19650218UL);
    i=1; j=0;
    k = (FIVMR_RANDMT_N>keyLength ? FIVMR_RANDMT_N : keyLength);
    for (; k; k--) {
        r->mt[i] = (r->mt[i] ^ ((r->mt[i-1] ^ (r->mt[i-1] >> 30)) * 1664525UL))
          + initKey[j] + j; /* non linear */
        i++; j++;
        if (i>=FIVMR_RANDMT_N) { r->mt[0] = r->mt[FIVMR_RANDMT_N-1]; i=1; }
        if (j>=keyLength) j=0;
    }
    for (k=FIVMR_RANDMT_N-1; k; k--) {
        r->mt[i] = (r->mt[i] ^ ((r->mt[i-1] ^ (r->mt[i-1] >> 30)) * (uint32_t)1566083941UL))
          - i; /* non linear */
        i++;
        if (i>=FIVMR_RANDMT_N) { r->mt[0] = r->mt[FIVMR_RANDMT_N-1]; i=1; }
    }

    r->mt[0] = 0x80000000UL; /* MSB is 1; assuring non-zero initial array */ 
}

void fivmr_Random_generate_slow(fivmr_Random *r) {
    unsigned long y;
    static unsigned long mag01[2]={0x0UL, FIVMR_RANDMT_MATRIX_A};
    
    uint32_t kk;
    
    fivmr_assert(r->mti <= FIVMR_RANDMT_N);
    
    for (kk=0;kk<FIVMR_RANDMT_N-FIVMR_RANDMT_M;kk++) {
	y = (r->mt[kk]&FIVMR_RANDMT_UPPER_MASK)|(r->mt[kk+1]&FIVMR_RANDMT_LOWER_MASK);
	r->mt[kk] = r->mt[kk+FIVMR_RANDMT_M] ^ (y >> 1) ^ mag01[y & 0x1UL];
    }
    for (;kk<FIVMR_RANDMT_N-1;kk++) {
	y = (r->mt[kk]&FIVMR_RANDMT_UPPER_MASK)|(r->mt[kk+1]&FIVMR_RANDMT_LOWER_MASK);
	r->mt[kk] = r->mt[kk+(FIVMR_RANDMT_M-FIVMR_RANDMT_N)] ^ (y >> 1) ^ mag01[y & 0x1UL];
    }
    y = (r->mt[FIVMR_RANDMT_N-1]&FIVMR_RANDMT_UPPER_MASK)|(r->mt[0]&FIVMR_RANDMT_LOWER_MASK);
    r->mt[FIVMR_RANDMT_N-1] = r->mt[FIVMR_RANDMT_M-1] ^ (y >> 1) ^ mag01[y & 0x1UL];
    
    r->mti = 0;
}

# 1 "fivmr_rbtree.c"
/*
 * fivmr_rbtree.c
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

/*
 * fivmr_rbtree.h -- implementation of a red-black tree.
 * by Filip Pizlo, 2010
 *
 * Based on http://www.mit.edu/~emin/source_code/red_black_tree/red_black_tree.c
 *
 * Which had the following license at the time that I retrieved it:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that neither the name of Emin
 * Martinian nor the names of any contributors are be used to endorse or
 * promote products derived from this software without specific prior
 * written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "fivmr.h"

void ftree_Tree_init(ftree_Tree *tree,
                     int (*compare) (uintptr_t,uintptr_t)) {
    tree->compare=compare;

    /*  see the comment in the rb_red_blk_tree structure in red_black_tree.h */
    /*  for information on nil and root */
    tree->nil.key=0;
    tree->nil.value=0;
    tree->nil.red=false;
    tree->nil.parent=&tree->nil;
    tree->nil.left=&tree->nil;
    tree->nil.right=&tree->nil;
    tree->root.key=0;
    tree->root.value=0;
    tree->root.red=false;
    tree->root.parent=&tree->nil;
    tree->root.left=&tree->nil;
    tree->root.right=&tree->nil;
}

static void LeftRotate(ftree_Tree* tree, ftree_Node* x) {
    ftree_Node* y;
    ftree_Node* nil=&tree->nil;

    /*  I originally wrote this function to use the sentinel for */
    /*  nil to avoid checking for nil.  However this introduces a */
    /*  very subtle bug because sometimes this function modifies */
    /*  the parent pointer of nil.  This can be a problem if a */
    /*  function which calls LeftRotate also uses the nil sentinel */
    /*  and expects the nil sentinel's parent pointer to be unchanged */
    /*  after calling this function.  For example, when RBDeleteFixUP */
    /*  calls LeftRotate it expects the parent pointer of nil to be */
    /*  unchanged. */

    y=x->right;
    x->right=y->left;

    if (y->left != nil) y->left->parent=x; /* used to use sentinel here */
    /* and do an unconditional assignment instead of testing for nil */
  
    y->parent=x->parent;   

    /* instead of checking if x->parent is the root as in the book, we */
    /* count on the root sentinel to implicitly take care of this case */
    if( x == x->parent->left) {
        x->parent->left=y;
    } else {
        x->parent->right=y;
    }
    y->left=x;
    x->parent=y;
    
    fivmr_assert(!tree->nil.red);
}


/***********************************************************************/
/*  FUNCTION:  RighttRotate */
/**/
/*  INPUTS:  This takes a tree so that it can access the appropriate */
/*           root and nil pointers, and the node to rotate on. */
/**/
/*  OUTPUT:  None */
/**/
/*  Modifies Input?: tree, y */
/**/
/*  EFFECTS:  Rotates as described in _Introduction_To_Algorithms by */
/*            Cormen, Leiserson, Rivest (Chapter 14).  Basically this */
/*            makes the parent of x be to the left of x, x the parent of */
/*            its parent before the rotation and fixes other pointers */
/*            accordingly. */
/***********************************************************************/

static void RightRotate(ftree_Tree* tree, ftree_Node* y) {
    ftree_Node* x;
    ftree_Node* nil=&tree->nil;

    /*  I originally wrote this function to use the sentinel for */
    /*  nil to avoid checking for nil.  However this introduces a */
    /*  very subtle bug because sometimes this function modifies */
    /*  the parent pointer of nil.  This can be a problem if a */
    /*  function which calls LeftRotate also uses the nil sentinel */
    /*  and expects the nil sentinel's parent pointer to be unchanged */
    /*  after calling this function.  For example, when RBDeleteFixUP */
    /*  calls LeftRotate it expects the parent pointer of nil to be */
    /*  unchanged. */

    x=y->left;
    y->left=x->right;

    if (nil != x->right)  x->right->parent=y; /*used to use sentinel here */
    /* and do an unconditional assignment instead of testing for nil */

    /* instead of checking if x->parent is the root as in the book, we */
    /* count on the root sentinel to implicitly take care of this case */
    x->parent=y->parent;
    if( y == y->parent->left) {
        y->parent->left=x;
    } else {
        y->parent->right=x;
    }
    x->right=y;
    y->parent=x;
    
    fivmr_assert(!tree->nil.red);
}

/***********************************************************************/
/*  FUNCTION:  TreeInsertHelp  */
/**/
/*  INPUTS:  tree is the tree to insert into and z is the node to insert */
/**/
/*  OUTPUT:  none */
/**/
/*  Modifies Input:  tree, z */
/**/
/*  EFFECTS:  Inserts z into the tree as if it were a regular binary tree */
/*            using the algorithm described in _Introduction_To_Algorithms_ */
/*            by Cormen et al.  This funciton is only intended to be called */
/*            by the RBTreeInsert function and not by the user */
/***********************************************************************/

static void TreeInsertHelp(ftree_Tree* tree, ftree_Node* z) {
    /*  This function should only be called by InsertRBTree (see above) */
    ftree_Node* x;
    ftree_Node* y;
    ftree_Node* nil=&tree->nil;
  
    z->left=z->right=nil;
    y=&tree->root;
    x=tree->root.left;
    while( x != nil) {
        y=x;
        if (tree->compare(x->key,z->key)>0) { /* x.key > z.key */
            x=x->left;
        } else { /* x,key <= z.key */
            x=x->right;
        }
    }
    z->parent=y;
    if ( (y == &tree->root) ||
         (tree->compare(y->key,z->key)>0)) { /* y.key > z.key */
        y->left=z;
    } else {
        y->right=z;
    }

    fivmr_assert(!tree->nil.red);
}

/***********************************************************************/
/*  FUNCTION:  RBTreeInsert */
/**/
/*  INPUTS:  tree is the red-black tree to insert a node which has a key */
/*           pointed to by key and info pointed to by info.  */
/**/
/*  OUTPUT:  This function returns a pointer to the newly inserted node */
/*           which is guarunteed to be valid until this node is deleted. */
/*           What this means is if another data structure stores this */
/*           pointer then the tree does not need to be searched when this */
/*           is to be deleted. */
/**/
/*  Modifies Input: tree */
/**/
/*  EFFECTS:  Creates a node node which contains the appropriate key and */
/*            info pointers and inserts it into the tree. */
/***********************************************************************/

void ftree_Tree_add(ftree_Tree* tree, ftree_Node *x) {
    ftree_Node * y;
    ftree_Node * newNode;

    TreeInsertHelp(tree,x);
    newNode=x;
    x->red=true;
    while(x->parent->red) { /* use sentinel instead of checking for root */
        if (x->parent == x->parent->parent->left) {
            y=x->parent->parent->right;
            if (y->red) {
                x->parent->red=false;
                y->red=false;
                x->parent->parent->red=true;
                x=x->parent->parent;
            } else {
                if (x == x->parent->right) {
                    x=x->parent;
                    LeftRotate(tree,x);
                }
                x->parent->red=false;
                x->parent->parent->red=true;
                RightRotate(tree,x->parent->parent);
            } 
        } else { /* case for x->parent == x->parent->parent->right */
            y=x->parent->parent->left;
            if (y->red) {
                x->parent->red=false;
                y->red=false;
                x->parent->parent->red=true;
                x=x->parent->parent;
            } else {
                if (x == x->parent->left) {
                    x=x->parent;
                    RightRotate(tree,x);
                }
                x->parent->red=false;
                x->parent->parent->red=true;
                LeftRotate(tree,x->parent->parent);
            } 
        }
    }
    tree->root.left->red=false;

    fivmr_assert(!tree->nil.red);
    fivmr_assert(!tree->root.red);
}

void ftree_Tree_addNew(ftree_Tree *tree,
                       ftree_Node *node) {
    fivmr_assert(ftree_Tree_find(tree,node->key)==NULL);
    ftree_Tree_add(tree,node);
}

ftree_Node *ftree_Tree_find(ftree_Tree *tree,
                            uintptr_t key) {
    return ftree_Tree_findFast(tree,key,tree->compare);
}


/***********************************************************************/
/*  FUNCTION:  RBDeleteFixUp */
/**/
/*    INPUTS:  tree is the tree to fix and x is the child of the spliced */
/*             out node in RBTreeDelete. */
/**/
/*    OUTPUT:  none */
/**/
/*    EFFECT:  Performs rotations and changes colors to restore red-black */
/*             properties after a node is deleted */
/**/
/*    Modifies Input: tree, x */
/**/
/*    The algorithm from this function is from _Introduction_To_Algorithms_ */
/***********************************************************************/

static void RBDeleteFixUp(ftree_Tree* tree, ftree_Node* x) {
    ftree_Node* root=tree->root.left;
    ftree_Node* w;

    while( (!x->red) && (root != x)) {
        if (x == x->parent->left) {
            w=x->parent->right;
            if (w->red) {
                w->red=0;
                x->parent->red=1;
                LeftRotate(tree,x->parent);
                w=x->parent->right;
            }
            if ( (!w->right->red) && (!w->left->red) ) { 
                w->red=1;
                x=x->parent;
            } else {
                if (!w->right->red) {
                    w->left->red=0;
                    w->red=1;
                    RightRotate(tree,w);
                    w=x->parent->right;
                }
                w->red=x->parent->red;
                x->parent->red=0;
                w->right->red=0;
                LeftRotate(tree,x->parent);
                x=root; /* this is to exit while loop */
            }
        } else { /* the code below is has left and right switched from above */
            w=x->parent->left;
            if (w->red) {
                w->red=0;
                x->parent->red=1;
                RightRotate(tree,x->parent);
                w=x->parent->left;
            }
            if ( (!w->right->red) && (!w->left->red) ) { 
                w->red=1;
                x=x->parent;
            } else {
                if (!w->left->red) {
                    w->right->red=0;
                    w->red=1;
                    LeftRotate(tree,w);
                    w=x->parent->left;
                }
                w->red=x->parent->red;
                x->parent->red=0;
                w->left->red=0;
                RightRotate(tree,x->parent);
                x=root; /* this is to exit while loop */
            }
        }
    }
    x->red=0;

    fivmr_assert(!tree->nil.red);
}


/***********************************************************************/
/*  FUNCTION:  RBDelete */
/**/
/*    INPUTS:  tree is the tree to delete node z from */
/**/
/*    OUTPUT:  none */
/**/
/*    EFFECT:  Deletes z from tree and frees the key and info of z */
/*             using DestoryKey and DestoryInfo.  Then calls */
/*             RBDeleteFixUp to restore red-black properties */
/**/
/*    Modifies Input: tree, z */
/**/
/*    The algorithm from this function is from _Introduction_To_Algorithms_ */
/***********************************************************************/

void ftree_Tree_remove(ftree_Tree* tree, ftree_Node* z){
    ftree_Node* y;
    ftree_Node* x;
    ftree_Node* nil=&tree->nil;
    ftree_Node* root=&tree->root;

    y= ((z->left == nil) || (z->right == nil)) ? z : ftree_Tree_nextImpl(tree,z);
    x= (y->left == nil) ? y->right : y->left;
    if (root == (x->parent = y->parent)) { /* assignment of y->p to x->p is intentional */
        root->left=x;
    } else {
        if (y == y->parent->left) {
            y->parent->left=x;
        } else {
            y->parent->right=x;
        }
    }
    if (y != z) { /* y should not be nil in this case */

        fivmr_assert( (y!=&tree->nil));
        /* y is the node to splice out and x is its child */

        if (!(y->red)) RBDeleteFixUp(tree,x);
  
        y->left=z->left;
        y->right=z->right;
        y->parent=z->parent;
        y->red=z->red;
        z->left->parent=z->right->parent=y;
        if (z == z->parent->left) {
            z->parent->left=y; 
        } else {
            z->parent->right=y;
        }
    } else {
        if (!(z->red)) RBDeleteFixUp(tree,x);
    }
  
    fivmr_assert(!tree->nil.red);
}

# 1 "fivmr_reflectlog.c"
/*
 * fivmr_reflectlog.c
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

bool fivmr_logReflect;

static void pre(fivmr_ThreadState *ts,
                int32_t depth) {
    fivmr_Log_lock();
    fivmr_Log_print("fivmr reflect log: ");
    fivmr_Log_print(fivmr_MethodRec_describe(fivmr_findCaller(ts,depth)));
}

static void post(fivmr_ThreadState *ts) {
    fivmr_Log_print("\n");
    fivmr_Log_unlock();
}

void fivmr_ReflectLog_dynamicCall(fivmr_ThreadState *ts,
                                  int32_t depth,
                                  fivmr_MethodRec *mr) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" dynamically called ");
	fivmr_Log_print(fivmr_MethodRec_describe(mr));
	post(ts);
    }
}

void fivmr_ReflectLog_call(fivmr_ThreadState *ts,
                           int32_t depth,
                           fivmr_MethodRec *mr) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" called ");
	fivmr_Log_print(fivmr_MethodRec_describe(mr));
	post(ts);
    }
}

void fivmr_ReflectLog_access(fivmr_ThreadState *ts,
                             int32_t depth,
                             fivmr_FieldRec *fr) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" accessed ");
	fivmr_Log_print(fivmr_FieldRec_describe(fr));
	post(ts);
    }
}

void fivmr_ReflectLog_alloc(fivmr_ThreadState *ts,
                            int32_t depth,
                            fivmr_TypeData *td) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" alloced ");
	fivmr_Log_print(td->name);
	post(ts);
    }
}

void fivmr_ReflectLog_use(fivmr_ThreadState *ts,
                          int32_t depth,
                          fivmr_TypeData *td) {
    if (fivmr_logReflect) {
	pre(ts,depth);
	fivmr_Log_print(" used ");
	fivmr_Log_print(td->name);
	post(ts);
    }
}

/* finds the first call into Class, Method, Field, or Constructor. */
static uintptr_t findFirstCallIntoReflect_cback(fivmr_VM *vm,
                                                uintptr_t arg,
						fivmr_MethodRec *mr,
						int32_t lineNumber) {
    int32_t *state=(int32_t*)arg;
    switch (*state) {
    case 0: /* still looking for public method of Class, Method, Field, or Constructor */
        /* FIXME - need some better way of comparing the owner... */
	if ((mr->owner==vm->payload->td_Constructor ||
             mr->owner==vm->payload->td_Method ||
             mr->owner==vm->payload->td_Field ||
	     mr->owner==vm->payload->td_Class) &&
	    (mr->flags&FIVMR_BF_VISIBILITY)==FIVMR_BF_PUBLIC) {
	    *state=1;
	}
	break;
    case 1: /* looking for method from a class other than Class, Method, Field, or
	       Constructor */
	if (mr->owner!=vm->payload->td_Constructor &&
	    mr->owner!=vm->payload->td_Method &&
	    mr->owner!=vm->payload->td_Field &&
	    mr->owner!=vm->payload->td_Class) {
	    return (uintptr_t)mr;
	}
	break;
    default: fivmr_assert(false);
    }
    return 0;
}

static fivmr_MethodRec *findFirstCallIntoReflect(fivmr_ThreadState *ts) {
    int32_t state=0;
    return (fivmr_MethodRec*)
	fivmr_iterateDebugFrames(ts->vm,
                                 ts->curF,
				 findFirstCallIntoReflect_cback,
				 (uintptr_t)&state);
}

static void preReflect(fivmr_ThreadState *ts) {
    fivmr_Log_lock();
    fivmr_Log_print("fivmr reflect log: ");
    fivmr_Log_print(fivmr_MethodRec_describe(findFirstCallIntoReflect(ts)));
}

void fivmr_ReflectLog_allocReflect(fivmr_ThreadState *ts,
                                   fivmr_TypeData *td) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" alloced ");
	fivmr_Log_print(td->name);
	post(ts);
    }
}

void fivmr_ReflectLog_dynamicCallReflect(fivmr_ThreadState *ts,
                                         fivmr_MethodRec *mr) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" dynamically called ");
	fivmr_Log_print(fivmr_MethodRec_describe(mr));
	post(ts);
    }
}

void fivmr_ReflectLog_callReflect(fivmr_ThreadState *ts,
                                  fivmr_MethodRec *mr) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" called ");
	fivmr_Log_print(fivmr_MethodRec_describe(mr));
	post(ts);
    }
}

void fivmr_ReflectLog_accessReflect(fivmr_ThreadState *ts,
                                    fivmr_FieldRec *fr) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" accessed ");
	fivmr_Log_print(fivmr_FieldRec_describe(fr));
	post(ts);
    }
}

void fivmr_ReflectLog_useReflectByName(fivmr_ThreadState *ts,
                                       const char *name) {
    if (fivmr_logReflect) {
	preReflect(ts);
	fivmr_Log_print(" used ");
	fivmr_Log_print(name);
	post(ts);
    }
}

void fivmr_ReflectLog_useReflect(fivmr_ThreadState *ts,
                                 fivmr_TypeData *td) {
    fivmr_ReflectLog_useReflectByName(ts,td->name);
}

# 1 "fivmr_rtems_hardrtj.c"
/*
 * fivmr_rtems_hardrtj.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS

#include <fivmr.h>

static fivmr_ThreadState *interruptTS;

static void interruptTrampoline(void *arg) {
    fivmr_NativeFrame nf;
    fivmr_VM *vm;
    fivmr_Handle *h;

    h=(fivmr_Handle*)arg;
    vm=h->vm;
    
    fivmr_assert(fivmr_Thread_isInterrupt());
    fivmr_assert(fivmr_ThreadHandle_current()==FIVMR_TH_INTERRUPT);
    fivmr_assert(fivmr_ThreadState_get(vm)==interruptTS);
    fivmr_ThreadState_go(interruptTS);
    fivmr_ThreadState_pushAndInitNF(interruptTS,&nf,NULL);
    LOG(2,("Running h = %p; type = %s",
           h,
           fivmr_TypeData_forObject(&vm->settings,h->obj)->name));
    fivmr_runRunnable(interruptTS,h);
    if (interruptTS->curExceptionHandle) {
	fivmr_ThreadState_goToJava(interruptTS);
	LOG(0,("Interrupt threw exception: %s",
	       fivmr_TypeData_forObject(
                   &vm->settings,
                   interruptTS->curExceptionHandle->obj)->name));
	interruptTS->curExceptionHandle=NULL;
	fivmr_ThreadState_goToNative(interruptTS);
    }
    fivmr_ThreadState_popNF(interruptTS);
    fivmr_ThreadState_goToJava(interruptTS);
    fivmr_deleteGlobalHandle(h);
    fivmr_ThreadState_goToNative(interruptTS);
    fivmr_ThreadState_commit(interruptTS);
}

static void timerTrampoline(rtems_id timer,void *arg) {
    LOG(2,("in timerTrampoline; arg = %p",arg));
    interruptTrampoline(arg);
}

void fivmr_initHardRTJ(fivmr_VM *vm) {
    fivmr_ThreadState *curTS;
    fivmr_Handle *vmt;
    int i;
    
    fivmr_assert(vm->maxPriority==FIVMR_TPR_CRITICAL);
    
    curTS=fivmr_ThreadState_get(vm);
    interruptTS=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA);
    vmt=fivmr_VMThread_create(curTS,
                              FIVMR_TPR_RR_MAX,true /* daemon */);
    fivmr_assertNoException(curTS,"while creating interrupt Java thread");
    fivmr_assert(vmt!=NULL);
    fivmr_ThreadState_glue(interruptTS,vmt);
    fivmr_ThreadState_setInitPrio(interruptTS,FIVMR_TPR_CRITICAL);
    fivmr_ThreadState_setManual(interruptTS,FIVMR_TH_INTERRUPT,NULL);
    
    /* this is the fun part ... preallocate a bunch of handles. */
    for (i=0;i<100;++i) {
	fivmr_Handle *h=fivmr_mallocAssert(sizeof(fivmr_Handle));
	bzero(h,sizeof(fivmr_Handle));
	h->next=interruptTS->freeHandles;
	interruptTS->freeHandles=h;
    }
    fivmr_Lock_lock(&vm->hrLock);
    for (i=0;i<100;++i) {
	fivmr_Handle *h=fivmr_mallocAssert(sizeof(fivmr_Handle));
	bzero(h,sizeof(fivmr_Handle));
	h->next=vm->freeHandles;
	vm->freeHandles=h;
    }
    fivmr_Lock_unlock(&vm->hrLock);
    
    /* and now ... make sure the thread has a buffer */
    interruptTS->buf=fivmr_mallocAssert(FIVMR_TS_BUF_SIZE);
    
    fivmr_preinitThreadStringBufFor(FIVMR_TH_INTERRUPT);
}

void fivmr_RTEMS_withInterruptsDisabled(fivmr_Handle *h) {
    rtems_mode oldMode;
    rtems_interrupt_level oldLevel;
    rtems_mode dummy;
    rtems_status_code status;
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    
    vm=h->vm;

    fivmr_assert(vm->maxPriority==FIVMR_TPR_CRITICAL);

    ts=fivmr_ThreadState_get(vm);
    
    status=rtems_task_mode(RTEMS_NO_PREEMPT|RTEMS_NO_TIMESLICE,
			   RTEMS_PREEMPT_MASK|RTEMS_TIMESLICE_MASK,
			   &oldMode);
    fivmr_assert( status == RTEMS_SUCCESSFUL ); 
    rtems_interrupt_disable(oldLevel); 
    fivmr_ThreadState_disablePollchecks(ts);
    
    fivmr_runRunnable(ts,h);
    
    fivmr_ThreadState_enablePollchecks(ts);
    rtems_interrupt_enable(oldLevel);
    status=rtems_task_mode(oldMode,
			   RTEMS_PREEMPT_MASK|RTEMS_TIMESLICE_MASK,
			   &dummy);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_RTEMS_printk(const char *str) {
    printk("%s",str);
}

int32_t fivmr_RTEMS_timerFireAfter(int32_t timerId,
				   int32_t ticks,
				   fivmr_Handle *h) {
    fivmr_ThreadState *ts;
    int32_t result;
    fivmr_assert(h->vm->maxPriority==FIVMR_TPR_CRITICAL);
    ts=fivmr_ThreadState_get(h->vm);
    fivmr_ThreadState_goToJava(ts);
    h=fivmr_newGlobalHandle(ts,h);
    fivmr_ThreadState_goToNative(ts);
    result=rtems_timer_fire_after(timerId,ticks,timerTrampoline,h);
    if (result!=RTEMS_SUCCESSFUL) {
	fivmr_ThreadState_goToJava(ts);
	fivmr_deleteGlobalHandle(h);
	fivmr_ThreadState_goToNative(ts);
    }
    LOG(2,("timer registered; timerId = %d, ticks = %d, result = %d, h = %p",
	   timerId,ticks,result,h));
    return result;
}

#endif

# 1 "fivmr_rtems_lock.c"
/*
 * fivmr_rtems_lock.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS

#include "fivmr.h"

void fivmr_Lock_init(fivmr_Lock *lock,
		     fivmr_Priority priority) {
    /* need to first create the mutex sema in RTEMS */
    /* need to allocate an empty condition variable */
    rtems_id          sema;
    rtems_status_code status;
    LOG(2, ("Initializing lock %p with priority %d",lock,priority));
    rtems_attribute attr=RTEMS_BINARY_SEMAPHORE|RTEMS_PRIORITY;
    rtems_task_priority prio;
    if (priority!=FIVMR_PR_CRITICAL) {
	if (priority==FIVMR_PR_NONE) {
	    prio=RTEMS_NO_PRIORITY;
	} else if (priority==FIVMR_PR_INHERIT) {
	    prio=RTEMS_NO_PRIORITY;
	    attr|=RTEMS_INHERIT_PRIORITY;
	} else {
	    prio=fivmr_Priority_toRTEMS(priority);
	    attr|=RTEMS_PRIORITY_CEILING;
	}
	status = rtems_semaphore_create(
	    fivmr_xchg_add32(&fivmr_nextSemaName,1),
	    1,attr,prio, &sema);
	LOG(2,("Lock has semaphore %d",sema));
	fivmr_assert( status == RTEMS_SUCCESSFUL );
	lock->mutex=sema;
    } else {
	lock->mutex=0;
    }
    lock->cond.head=NULL;
    lock->level=-1;
    lock->mode=-1;
    lock->holder=fivmr_ThreadHandle_zero();
    lock->recCount=0;
    lock->priority=priority;
}

void fivmr_Lock_destroy(fivmr_Lock *lock) {
    rtems_semaphore_delete(lock->mutex);
}

bool fivmr_Lock_legalToAcquire(fivmr_Lock *lock) {
    if (lock->priority==FIVMR_PR_CRITICAL) {
	return true;
    } else {
	return !fivmr_Thread_isCritical();
    }
}

void fivmr_Lock_lock(fivmr_Lock *lock) {
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    rtems_status_code status;
    
    LOG(5, ("Lock lock"));
    
    if (lock->holder==me) {
	
	fivmr_assert(lock->recCount<100);
	
	lock->recCount++;
    } else {
        LOG(5, ("Non-rec lock"));
	if (FIVMR_ASSERTS_ON &&
	    lock->priority!=FIVMR_PR_CRITICAL &&
	    fivmr_Thread_isCritical()) {
	    LOG(0,("error: trying to acquire a non-critical lock with "
		   "interrupts disabled; lock priority = %d, "
		   "ISR level = %d, lock address = %p",
		   lock->priority,_ISR_Get_level(),lock));
	    fivmr_assert(false);
	}
        
	if(lock->priority==FIVMR_PR_CRITICAL) {
	    rtems_interrupt_disable(lock->level);
	} else {
	    status = rtems_semaphore_obtain(lock->mutex, RTEMS_WAIT, RTEMS_NO_TIMEOUT); 
            if (status!=RTEMS_SUCCESSFUL) {
                LOG(0,("Failed to acquire lock."));
                LOG(0,("My priority: %p",fivmr_Thread_getPriority(fivmr_ThreadHandle_current())));
                LOG(0,("Status: %d",status));
                fivmr_abort("failed to acquire lock");
            }
	}
	lock->holder=me;
	lock->recCount=1;
    }
}

void fivmr_Lock_unlock(fivmr_Lock *lock) {
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    rtems_status_code status;
    
    LOG(9, ("Lock unlock"));
    fivmr_assert(lock->recCount>=1);
    fivmr_assert(lock->holder==me);
    if (lock->recCount==1) {
	lock->recCount=0;
	lock->holder=fivmr_ThreadHandle_zero();
	if(lock->priority==FIVMR_PR_CRITICAL) {
	    rtems_interrupt_enable(lock->level); 
	} else {
	    status = rtems_semaphore_release(lock->mutex); 
	    fivmr_assert( status == RTEMS_SUCCESSFUL );
	}
    } else {
	lock->recCount--;
    }
}

void fivmr_Lock_broadcast(fivmr_Lock *lock) {
    fivmr_ThreadCondNode *ptr;
    rtems_status_code status;
    fivmr_assert(!fivmr_Thread_isCritical());
    fivmr_assert(lock->priority!=FIVMR_PR_CRITICAL);
    fivmr_assert(lock->holder==fivmr_ThreadHandle_current());
    fivmr_assert(lock->recCount>=1);
    LOG(2, ("Lock broadcast on %p",lock));
    if (lock->cond.head != NULL){
	LOG(2, ("Lock broadcasting to non-emtpy cond variable"));
	ptr = lock->cond.head;
	while(ptr!=NULL) {
	    fivmr_ThreadCondNode *next=ptr->next;
	    LOG(2, ("notifying %p, ISR level is %d",ptr,_ISR_Get_level()));
	    fivmr_assert(ptr->queuedOn==lock);
	    ptr->queuedOn=NULL;
	    ptr->next=NULL;
	    status = rtems_semaphore_release(ptr->sema);
	    fivmr_assert( status == RTEMS_SUCCESSFUL );
	    ptr=next;
	}
	lock->cond.head = NULL;
    }
    LOG(2, ("done broadcasting on %p.",lock));
}

void fivmr_Lock_lockedBroadcast(fivmr_Lock *lock) {
    LOG(2, ("Lock locked broadcast"));	      
    fivmr_assert(!fivmr_Thread_isCritical());
    fivmr_assert(lock->priority!=FIVMR_PR_CRITICAL);
    fivmr_Lock_lock(lock);
    fivmr_Lock_broadcast(lock); /* why duplicate code? should be the same as above */
    fivmr_Lock_unlock(lock);
}

static void rtems_wait_impl(fivmr_Lock *lock,
                            bool hasTimeout,
                            fivmr_Nanos whenAwake) {
    int32_t savedRecCount;
    rtems_status_code status;
    fivmr_ThreadHandle me=fivmr_ThreadHandle_current();
    fivmr_ThreadCondNode *tln;  
    
    fivmr_assert(!fivmr_Thread_isCritical());
    fivmr_assert(lock->priority!=FIVMR_PR_CRITICAL);

    LOG(2, ("Lock Wait on %p by thread %p", lock, me));
    fivmr_assert(lock->holder==me);
    fivmr_assert(lock->recCount>=1);
    savedRecCount=lock->recCount;
    lock->recCount=0;
    lock->holder=fivmr_ThreadHandle_zero();

    tln=(fivmr_ThreadCondNode*)fivmr_ThreadSpecific_get(&fivmr_RTEMS_lockData);
    LOG(2,("Got tln = %p",tln));
    tln->next=lock->cond.head;
    tln->queuedOn=lock;
    lock->cond.head=tln;

    if(lock->priority==FIVMR_PR_CRITICAL) {
	rtems_mode dummy;
	fivmr_assert(lock->level!=(rtems_interrupt_level)-1); /* we have a previous level */
	rtems_interrupt_enable(lock->level); 
	status=rtems_task_mode(lock->mode,
			       RTEMS_PREEMPT_MASK|RTEMS_TIMESLICE_MASK,
			       &dummy);
	fivmr_assert( status == RTEMS_SUCCESSFUL ); 
    } else {
	status=rtems_semaphore_release(lock->mutex);
	fivmr_assert(status==RTEMS_SUCCESSFUL);
    }
    
    LOG(2,("acquiring semaphore %d",tln->sema));
    
    if (hasTimeout) {
        for (;;) {
            fivmr_Nanos now;
            fivmr_Nanos timeLeft;
            struct timespec ts;
            uint32_t ticks;
            /* FIXME this is so wrong and so likely to cause timing skew.  but oddly enough
               it's exactly the way RTEMS itself implements abs-time timeouts.
               weird-o-rific! */
            now=fivmr_curTime();
            if (whenAwake<=now) {
                break;
            }
            timeLeft=whenAwake-now;
            ts.tv_sec=timeLeft/(1000*1000*1000);
            ts.tv_nsec=timeLeft%(1000*1000*1000);
            ticks=_Timespec_To_ticks(&ts);
            LOG(2,("sleeping for %d ticks",ticks));
	    status = rtems_semaphore_obtain(tln->sema, RTEMS_WAIT, ticks);
	    LOG(2,("semaphore for %p returned = %d",lock,status));
	    fivmr_assert(status==RTEMS_SUCCESSFUL ||
			 status==RTEMS_TIMEOUT);
	    if (status==RTEMS_SUCCESSFUL) {
		break;
	    }
	}
    } else {
	status = rtems_semaphore_obtain(tln->sema, RTEMS_WAIT, RTEMS_NO_TIMEOUT); 
	LOG(2,("semaphore for %p returned = %d",lock,status));
	fivmr_assert( status == RTEMS_SUCCESSFUL ); 
    }
    
    if (lock->priority==FIVMR_PR_CRITICAL) {
	status=rtems_task_mode(RTEMS_NO_PREEMPT|RTEMS_NO_TIMESLICE,
			       RTEMS_PREEMPT_MASK|RTEMS_TIMESLICE_MASK,
			       &lock->mode);
	fivmr_assert( status == RTEMS_SUCCESSFUL ); 
	rtems_interrupt_disable(lock->level);
    } else {
	status=rtems_semaphore_obtain(lock->mutex, RTEMS_WAIT, RTEMS_NO_TIMEOUT);
	fivmr_assert(status==RTEMS_SUCCESSFUL);
    }
    if (tln->queuedOn!=NULL) {
	fivmr_assert(tln->queuedOn==lock);
	fivmr_assert(hasTimeout);
	fivmr_assert(lock->cond.head!=NULL);
	fivmr_assert(tln->next!=tln);
	if (lock->cond.head==tln) {
	    LOG(2,("Setting cond.head for %p to %p",lock,tln->next));
	    lock->cond.head=tln->next;
	} else {
	    fivmr_ThreadCondNode *cur;
	    bool found=false;
	    for (cur=lock->cond.head;cur->next!=NULL;cur=cur->next) {
		if (cur->next==tln) {
		    cur->next=tln->next;
		    break;
		}
	    }
	    fivmr_assert(found);
	}
	tln->next=NULL;
	tln->queuedOn=NULL;
    }
    lock->recCount=savedRecCount;
    lock->holder=me;
}

void fivmr_Lock_wait(fivmr_Lock *lock) {
    rtems_wait_impl(lock,false,0);
}

void fivmr_Lock_timedWaitAbs(fivmr_Lock *lock,
			     fivmr_Nanos whenAwake) {
    rtems_wait_impl(lock,true,whenAwake);
}

void fivmr_Lock_timedWaitRel(fivmr_Lock *lock,
			     fivmr_Nanos howLong) {
    LOG(2, ("Lock timed wait release"));
    fivmr_Lock_timedWaitAbs(lock,fivmr_curTime()+howLong);
}

#endif
# 1 "fivmr_rtems_log.c"
/*
 * fivmr_rtems_log.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS
#include "fivmr.h"


void fivmr_Log_init(void) {
    LOG(1,("%s %s %s, All Rights Reserved",fivmr_name(),fivmr_version(),fivmr_copyright()));
}

void fivmr_Log_lock(void) {
}

void fivmr_Log_unlock(void) {
}

void fivmr_Log_vprintf(const char *msg,va_list lst) {
    vprintk(msg,lst);  
}

#endif
# 1 "fivmr_rtems_nanos.c"
/*
 * fivmr_rtems_nanos.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS

#include "fivmr.h"
#include <sys/time.h>

fivmr_Nanos fivmr_nanosResolution() {
    /* FIXME: How can we get this resolution?  Doesn't seem to be
     * rtems_clock_get_ticks_per_second() */
    return 1000;
}

fivmr_Nanos fivmr_curTimePrecise(void) {
    if (true) {
        fivmr_Nanos result;
        struct timespec ts;
        rtems_status_code status;
        
        status=rtems_clock_get_uptime(&ts);
        fivmr_assert( status == RTEMS_SUCCESSFUL ); 
        
        result=ts.tv_sec;
        result*=1000;
        result*=1000;
        result*=1000;
        result+=ts.tv_nsec;
        
        return result;
    } else {
        /* this doesn't seem to work */
        
        fivmr_Nanos result;
        struct timeval tv;
        rtems_status_code status;
        status=rtems_clock_get_tod_timeval(&tv);
        fivmr_assert(status==RTEMS_SUCCESSFUL);
        
        result=tv.tv_sec;
        result*=1000;
        result*=1000;
        result+=tv.tv_usec;
        result*=1000;
        
        return result;
    }
}

fivmr_Nanos fivmr_curTime(void) {
    if (false) {
        return fivmr_curTimePrecise();
    } else {
        fivmr_Nanos result;
        struct timespec ts;
        
        rtems_interrupt_level cookie;
        rtems_interrupt_disable(cookie);
        ts=_TOD_Uptime;
        rtems_interrupt_enable(cookie);

        result=ts.tv_sec;
        result*=1000;
        result*=1000;
        result*=1000;
        result+=ts.tv_nsec;

        return result;
    }
}

#endif
# 1 "fivmr_rtems_suspend.c"
/*
 * fivmr_rtems_suspend.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS

#include "fivmr.h"

void fivmr_initSuspensionManager(void) {
    /* nothing to do because RTEMS isn't stupid */
}

fivmr_SuspendableThreadHandle fivmr_Thread_makeSuspendable(fivmr_SuspensionData *data) {
    return fivmr_ThreadHandle_current();
}

void fivmr_Thread_suspend(fivmr_SuspendableThreadHandle th) {
    rtems_status_code res;
    res=rtems_task_suspend(th);
    fivmr_assert(res==RTEMS_SUCCESSFUL);
}

void fivmr_Thread_resume(fivmr_SuspendableThreadHandle th) {
    rtems_status_code res;
    res=rtems_task_resume(th);
    fivmr_assert(res==RTEMS_SUCCESSFUL);
}

#endif
# 1 "fivmr_rtems_sysdep.c"
/*
 * fivmr_rtems_sysdep.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS

#include "fivmr.h"
#include <stdarg.h>
#include <rtems.h>
#include <rtems/malloc.h>


fivmr_ThreadSpecific fivmr_RTEMS_lockData;
int32_t fivmr_nextSemaName;
int32_t fivmr_nextThreadName;
int32_t fivmr_nextThreadSpecific;
void (*fivmr_threadSpecificDestructor[16])(uintptr_t arg);
uint32_t fivmr_interruptNotepad[16];

/* FIXME: these should not be global variables if their values
   are always constant.  it's a waste of memory. */
fivmr_Priority FIVMR_PR_NONE;
fivmr_Priority FIVMR_PR_INHERIT;
fivmr_Priority FIVMR_PR_MIN;
fivmr_Priority FIVMR_PR_MAX;
fivmr_Priority FIVMR_PR_CRITICAL;
fivmr_ThreadPriority FIVMR_TPR_NORMAL_MIN;
fivmr_ThreadPriority FIVMR_TPR_NORMAL_MAX;
fivmr_ThreadPriority FIVMR_TPR_FIFO_MIN;
fivmr_ThreadPriority FIVMR_TPR_FIFO_MAX;
fivmr_ThreadPriority FIVMR_TPR_RR_MIN;
fivmr_ThreadPriority FIVMR_TPR_RR_MAX;

rtems_interrupt_level fivmr_interruptISRLevel;

fivmr_ThreadSpecificGlobalData fivmr_tsData[FIVMR_MAX_THREAD_SPECIFICS];
int32_t fivmr_tsFree[FIVMR_MAX_THREAD_SPECIFICS];
int32_t fivmr_numTSFree;
rtems_id fivmr_tsSema;

uintptr_t fivmr_RTEMS_threadStackSize=65536;

void fivmr_SysDep_init(void) {
    rtems_interrupt_level cookie;
    int32_t i;
    rtems_status_code status;

    rtems_interrupt_disable(cookie);
    fivmr_interruptISRLevel=_ISR_Get_level();
    rtems_interrupt_enable(cookie);
    
#if FIVMR_ASSERTS_ON
    fivmr_assert(!fivmr_Thread_isCritical());
    rtems_interrupt_disable(cookie);
    fivmr_assert(fivmr_Thread_isCritical());
    rtems_interrupt_enable(cookie);
    fivmr_assert(!fivmr_Thread_isCritical());
#endif

#if FIVMR_DYN_LOADING
#error "Dynamic Loading Not Supported By RTEMS"
#endif
    FIVMR_PR_NONE = (fivmr_Priority) 1;
    FIVMR_PR_INHERIT = (fivmr_Priority) 2;
    FIVMR_PR_MIN = (fivmr_Priority) 3;
    FIVMR_PR_MAX = (fivmr_Priority) 256;
    FIVMR_PR_CRITICAL = (fivmr_Priority) 257;

    FIVMR_TPR_NORMAL_MIN = FIVMR_THREADPRIORITY(FIVMR_TPR_NORMAL, 1);
    FIVMR_TPR_NORMAL_MAX = FIVMR_THREADPRIORITY(FIVMR_TPR_NORMAL, 1);
    FIVMR_TPR_FIFO_MIN = FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO, 2);
    FIVMR_TPR_FIFO_MAX = FIVMR_THREADPRIORITY(FIVMR_TPR_FIFO, 253);
    FIVMR_TPR_RR_MIN = FIVMR_THREADPRIORITY(FIVMR_TPR_RR, 2);
    FIVMR_TPR_RR_MAX = FIVMR_THREADPRIORITY(FIVMR_TPR_RR, 253);

    fivmr_assert(FIVMR_PR_MIN>=0);
    fivmr_assert(FIVMR_PR_MAX>=0);
    fivmr_assert(FIVMR_PR_CRITICAL>=0);
    fivmr_assert(FIVMR_PAGE_SIZE>=getpagesize());
    fivmr_assert((FIVMR_PAGE_SIZE%getpagesize())==0);
    
    fivmr_nextSemaName = rtems_build_name('s', 'e', 'm', 'a');
    fivmr_nextThreadName = rtems_build_name('f', 'i', 'v', 'm');
    
    status = rtems_semaphore_create(
        fivmr_xchg_add32(&fivmr_nextSemaName,1),
        1,
        RTEMS_BINARY_SEMAPHORE|RTEMS_PRIORITY|RTEMS_PRIORITY_CEILING,
        0 /* max priority */,
        &fivmr_tsSema);
    fivmr_assert( status == RTEMS_SUCCESSFUL );
    
    LOG(2,("Initialized the semaphore at %p",fivmr_tsSema));

    fivmr_numTSFree=FIVMR_MAX_THREAD_SPECIFICS;
    for (i=0;i<FIVMR_MAX_THREAD_SPECIFICS;++i) {
        fivmr_tsFree[i]=i;
        fivmr_tsData[i].destructor=NULL;
        fivmr_tsData[i].interruptValue=0;
    }

    fivmr_SysDepUtil_init();

    fivmr_ThreadSpecific_init(&fivmr_RTEMS_lockData);
}

static void abort_impl(void) {
    /* CPU_print_stack(); */
    printk("fivmr dying in thread %p\n",fivmr_ThreadHandle_current());
    if (false) {
	printk("attempting to dumb Java stack...\n");
	fivmr_ThreadState_dumpStack();
    }
    abort();
    exit(1);
}

uintptr_t fivmr_abortf(const char *fmt,...) {
    va_list lst;
    va_start(lst,fmt);
    printk("fivmr ABORT: ");
    vprintk(fmt,lst);
    printk("\n");
    va_end(lst);
    abort_impl();
    return 0;
}

uintptr_t fivmr_abort(const char *message) {
    if (message==NULL) {
	message=
	    "(message was NULL; typically implies Java exception "
	    "during attempt to abort)";
    }
    printk("fivmr ABORT: %s\n",message);
    abort_impl();
    return 0;
}

void fivmr_yield(void) {
    /* no easy way to yield in RTEMS */
    rtems_task_wake_after(RTEMS_YIELD_PROCESSOR);
}

void *fivmr_tryAllocPages_IMPL(uintptr_t numPages,
                               bool *isZero,
                               const char *whereFile,
                               int whereLine) {
    uintptr_t result,mallocResult;
    LOG(2,("fivmr_allocPages(%p) called from %s:%d.",numPages,whereFile,whereLine));
    mallocResult = (uintptr_t)malloc((numPages+1)<<FIVMSYS_LOG_PAGE_SIZE);
    LOG(2,("malloc returned %p",result));
    if (mallocResult==0) {
        LOG(2,("malloc() failed in fivmr_tryAllocPages() for %p pages at %s:%d.\n",
               numPages,whereFile,whereLine));
        return NULL;
    }
    result=(mallocResult+FIVMR_PAGE_SIZE)&~(FIVMR_PAGE_SIZE-1);
    ((uintptr_t*)result)[-1]=mallocResult;

    if (isZero==NULL) {
        bzero((void *)result,FIVMR_PAGE_SIZE*numPages);
    } else {
        *isZero=false;
    }
    
    LOG(2,("fivmr_allocPages(%p) returning %p - %p",
	   numPages,result,result+(numPages<<FIVMSYS_LOG_PAGE_SIZE)-1));

    return (void *)result;
}

void fivmr_freePages_IMPL(void *begin,uintptr_t numPages,
                          const char *whereFile,
                          int whereLine) {
    uintptr_t origAddr;
    LOG(2,("fivmr_freePages(%p,%p) called from %s:%d.",begin,numPages,whereFile,whereLine));
    origAddr=((uintptr_t*)begin)[-1];
    free((void*)origAddr);
}

int32_t fivmr_readByte(int fd) {
    uint8_t result;
    int res;
    res=read(fd,&result,1);
    if (res<0) {
	fivmr_assert(res==-1);
	return -2;
    } else if (res==0) {
	return -1;
    } else {
	return (int32_t)(uint32_t)result;
    }
}

int32_t fivmr_writeByte(int fd,int8_t b) {
    return write(fd,&b,1);
}

void fivmr_ThreadSpecific_initWithDestructor(fivmr_ThreadSpecific *ts,
                                             void (*dest)(uintptr_t value)) {
    rtems_status_code status;
    
    LOG(2,("Acquiring the semaphore at %p",fivmr_tsSema));
    status=rtems_semaphore_obtain(fivmr_tsSema,RTEMS_WAIT,RTEMS_NO_TIMEOUT);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
    
    fivmr_assert(fivmr_numTSFree>0);
    *ts=fivmr_tsFree[--fivmr_numTSFree];
    
    fivmr_tsData[*ts].destructor=dest;
    fivmr_tsData[*ts].interruptValue=0;
    
    status=rtems_semaphore_release(fivmr_tsSema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_ThreadSpecific_destroy(fivmr_ThreadSpecific *ts) {
    rtems_status_code status;
    
    status=rtems_semaphore_obtain(fivmr_tsSema,RTEMS_WAIT,RTEMS_NO_TIMEOUT);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
    
    fivmr_assert(fivmr_numTSFree<FIVMR_MAX_THREAD_SPECIFICS);
    fivmr_tsData[*ts].destructor=NULL;
    fivmr_tsData[*ts].interruptValue=0;
    fivmr_tsFree[fivmr_numTSFree++]=*ts;
    
    status=rtems_semaphore_release(fivmr_tsSema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_ThreadSpecific_set(fivmr_ThreadSpecific *ts,
                              uintptr_t value) {
    if (fivmr_ThreadHandle_current()==FIVMR_TH_INTERRUPT) {
        fivmr_tsData[*ts].interruptValue=value;
    } else {
        rtems_status_code status;
        uint32_t result;
        uintptr_t *tss;
        status=rtems_task_get_note(RTEMS_SELF,FIVMR_TS_NOTE,&result);
        fivmr_assert(status==RTEMS_SUCCESSFUL);
        tss=(uintptr_t*)(uintptr_t)result;
        tss[*ts]=value;
    }
}

void fivmr_ThreadSpecific_setForThread(fivmr_ThreadSpecific *ts,
                                       fivmr_ThreadHandle th,
                                       uintptr_t value) {
    if (th==FIVMR_TH_INTERRUPT) {
        fivmr_tsData[*ts].interruptValue=value;
    } else if (th==fivmr_ThreadHandle_current()) {
        fivmr_ThreadSpecific_set(ts,value);
    } else {
        fivmr_assert(!"don't know how to set thread specific for any random thread");
    }
}

uintptr_t fivmr_ThreadSpecific_get(fivmr_ThreadSpecific *ts) {
    if (fivmr_ThreadHandle_current()==FIVMR_TH_INTERRUPT) {
        return fivmr_tsData[*ts].interruptValue;
    } else {
        rtems_status_code status;
        uint32_t result;
        uintptr_t *tss;
        status=rtems_task_get_note(RTEMS_SELF,FIVMR_TS_NOTE,&result);
        fivmr_assert(status==RTEMS_SUCCESSFUL);
        tss=(uintptr_t*)(uintptr_t)result;
        return tss[*ts];
    }
}

void fivmr_debugMemory_IMPL(const char *flnm,
                            int line) {
    rtems_malloc_statistics_t stats;
    malloc_get_statistics(&stats);
    uint32_t m=stats.malloc_calls;
    uint32_t f=stats.free_calls;
    printk("(%s:%d) malloc calls: %lu\n",flnm,line,m);
    printk("(%s:%d) free calls: %lu\n",flnm,line,f);
    printk("(%s:%d) balance: %lu\n",flnm,line,m-f);
}

#endif
# 1 "fivmr_rtems_thread.c"
/*
 * fivmr_rtems_thread.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS

#include "fivmr.h"

fivmr_ThreadHandle fivmr_Thread_integrate(void) {
    /* FIXME: take care of notes!  currently this isn't an issue but it will be in
       the future! */

    return fivmr_ThreadHandle_current();
}

typedef struct {
    void (*threadMain)(void *arg);
    void *arg;
} RunData;

static rtems_task runner(rtems_task_argument arg_){
    RunData *rd;
    void (*threadMain)(void *arg);
    void *arg;
    rtems_interrupt_level cookie;
    rtems_id          sema;
    rtems_status_code status;
    fivmr_ThreadCondNode tcn;
    uintptr_t tss[FIVMR_MAX_THREAD_SPECIFICS];
    
    LOG(1,("In new thread %p",fivmr_ThreadHandle_current()));
    
    bzero(tss,sizeof(tss));
    status=rtems_task_set_note(RTEMS_SELF,
                               FIVMR_TS_NOTE,
                               (uint32_t)(uintptr_t)(&tss[0]));
    fivmr_assert(status==RTEMS_SUCCESSFUL);
    
    rd=(RunData*)arg_;
    
    threadMain=rd->threadMain;
    arg=rd->arg;
    
    fivmr_free(rd);
    
    LOG(2,("creating semaphore..."));
    
    /* initial value of 0 so an obtain blocks on the sema for synchronization
       another thread can then wake this thread up by doing a release */
    status = rtems_semaphore_create(fivmr_xchg_add32(&fivmr_nextSemaName,1),
				    0, RTEMS_COUNTING_SEMAPHORE|RTEMS_PRIORITY,
				    RTEMS_NO_PRIORITY, &sema);
    fivmr_assert( status == RTEMS_SUCCESSFUL );
    
    LOG(1,("Thread %p has semaphore %d",fivmr_ThreadHandle_current(),sema));
    
    tcn.sema=sema;
    tcn.next=NULL;
    tcn.queuedOn=NULL;

    fivmr_ThreadSpecific_set(&fivmr_RTEMS_lockData,(uintptr_t)&tcn);

    threadMain(arg);
    
    fivmr_Thread_exit();
}

fivmr_ThreadHandle fivmr_Thread_spawn(void (*threadMain)(void *arg),
				      void *arg,
				      fivmr_ThreadPriority priority) {
    rtems_id            tid;
    rtems_status_code   status;
    rtems_name          name;
    rtems_mode          rmode = 0;
    rtems_task_priority rprio = 0;
    RunData             *rd;
    size_t stackSize = fivmr_RTEMS_threadStackSize;
    char buf[32];

    if (LOGGING(1)) {
        fivmr_ThreadPriority_describe(priority,buf,sizeof(buf));
        LOG(1, ("Spawning thread with priority %s",buf));
    }
    rd=fivmr_mallocAssert(sizeof(RunData));
    rd->threadMain=threadMain;
    rd->arg=arg;

    /* TODO: fix the priority */
    name=fivmr_xchg_add32(&fivmr_nextThreadName,1);
    LOG(2,("Using thread name = %p",name));
    LOG(2,("Stack size = %p",stackSize));
    if (LOGGING(2)) {
        fivmr_ThreadPriority_describe(priority,buf,sizeof(buf));
        LOG(2,("Starting a thread with priority %s",buf));
    }
    fivmr_ThreadPriority_toRTEMS(priority, &rprio, &rmode);
    LOG(2,("RTEMS priority: %d, %d",rprio,rmode));
    status = rtems_task_create(
	name, rprio, stackSize,
	RTEMS_PREEMPT|rmode, RTEMS_FLOATING_POINT, &tid);
    LOG(2,("tid = %d, status = %d",tid,status));
    
    if (status!=RTEMS_SUCCESSFUL) {
        LOG(0,("Failed to create thread; status = %d",status));
        return fivmr_ThreadHandle_zero();
    }

    status = rtems_task_start( tid, runner, (int) rd);
    fivmr_assert( status == RTEMS_SUCCESSFUL );
    LOG(2, ("Spawning thread complete"));
    
    LOG(2,("returning thread: %p",tid));
    
    return tid;
}

void fivmr_Thread_exit(void) {
    fivmr_ThreadCondNode *tcn;
    uintptr_t *tss;
    rtems_status_code status;
    uint32_t tmp;
    int32_t i;
    
    fivmr_assert(fivmr_ThreadHandle_current()!=FIVMR_TH_INTERRUPT);
    fivmr_assert(fivmr_Thread_canExit());

    tcn=(fivmr_ThreadCondNode*)(void*)fivmr_ThreadSpecific_get(&fivmr_RTEMS_lockData);
    
    status=rtems_task_get_note(RTEMS_SELF,FIVMR_TS_NOTE,&tmp);
    tss=(uintptr_t*)(uintptr_t)tmp;

    fivmr_assert(tcn->queuedOn==NULL);
    fivmr_assert(tcn->next==NULL);
    
    rtems_semaphore_delete(tcn->sema);
    
    status=rtems_semaphore_obtain(fivmr_tsSema,RTEMS_WAIT,RTEMS_NO_TIMEOUT);
    fivmr_assert(status==RTEMS_SUCCESSFUL);

    for (i=0;i<FIVMR_MAX_THREAD_SPECIFICS;++i) {
	void (*dest)(uintptr_t arg)=fivmr_tsData[i].destructor;
	if (dest!=NULL && tss[i]!=0) {
            dest(tss[i]);
	}
    }
    
    status=rtems_semaphore_release(fivmr_tsSema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);

    rtems_task_delete( RTEMS_SELF );
    fivmr_assert(false);
}


fivmr_ThreadPriority fivmr_Thread_getPriority(fivmr_ThreadHandle h) {
    if (h==FIVMR_TH_INTERRUPT || (h==fivmr_ThreadHandle_current() && fivmr_Thread_isCritical())) {
        return FIVMR_TPR_CRITICAL;
    } else {
        rtems_status_code status;
        rtems_task_priority oldPrio;
        rtems_mode oldMode;
        status=rtems_task_set_priority(h,RTEMS_NO_PRIORITY,&oldPrio);
        fivmr_assert(status==RTEMS_SUCCESSFUL);
        if (h==fivmr_ThreadHandle_current()) {
            status=rtems_task_mode(0,0,&oldMode);
        } else {
            /* HACKZILLA!  FIXME!  get the real mode! */
            oldMode=RTEMS_PREEMPT;
        }
        return fivmr_ThreadPriority_fromRTEMS(oldPrio,oldMode);
    }
}

#endif
# 1 "fivmr_run.c"
/*
 * fivmr_run.c
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

#include "fivmr.h"

void fivmr_runBaseInit(void) {
    fivmr_Log_init();

    LOG(1,("Initializing system-dependent library..."));
    
    fivmr_SysDep_init();

    LOG(1,("System-dependent library initialized.  Integrating main thread..."));
    
    fivmr_Thread_integrate();
    
    LOG(1,("Initializing globals..."));
    
    fivmr_VM_initGlobal();

    LOG(1,("Ready to start runtime."));

#if FIVMR_FLOW_LOGGING
    fivmr_FlowLog_init();
#endif
}

void fivmr_runBaseTRPartInit(void) {
    fivmr_runBaseInit();
    
    LOG(1,("Initializing suspension management..."));
    
    fivmr_initSuspensionManager();
    
    LOG(1,("Ready to use time/resource partitioning."));
}

void fivmr_runRuntime(fivmr_VM *vm,
                      int argc,
                      char **argv) {
    fivmr_runRuntimeWithClass(vm,vm->payload->entrypoint,argc,argv);
}

void fivmr_runRuntimeWithClass(fivmr_VM *vm,
                               fivmr_TypeData *mainClass,
                               int argc,
                               char **argv) {
    fivmr_VM_init(vm);
    fivmr_VM_runWithClass(vm,mainClass,argc,argv);
}

void fivmr_VM_run(fivmr_VM *vm,
                  int argc,
                  char **argv) {
    fivmr_VM_runWithClass(vm,vm->payload->entrypoint,argc,argv);
}

void fivmr_VM_runWithClass(fivmr_VM *vm,
                           fivmr_TypeData *mainClass,
                           int argc,
                           char **argv) {
    /* all variables should be volatile due to longjmp/setjmp */
    fivmr_ThreadState * volatile ts;
    fivmr_MethodRec * volatile mainMR;
    fivmr_Handle * volatile args;
    fivmr_Value volatile argArray[1];
    int volatile result=0;
    bool volatile res;
    fivmr_JmpBuf volatile jmpbuf;

    LOG(1,("Creating C-side thread state."));
    
    vm->exceptionsFatalReason="Unexpected exception during system initialization";

    ts=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
    if (ts==NULL && vm->exiting) {
        return;
    }
    fivmr_assert(ts!=NULL);
    
    ts->jumpOnExit=(fivmr_JmpBuf*)&jmpbuf;
    if (fivmr_JmpBuf_label((fivmr_JmpBuf*)&jmpbuf)) {
        LOG(1,("Jumped out of VM; returning. (1)"));
        return;
    }
    
    fivmr_ThreadState_setStackHeight(ts,fivmr_Thread_stackHeight()-FIVMR_STACK_HEIGHT_HEADROOM);
    fivmr_ThreadState_setBasePrio(ts,fivmr_Thread_getPriority(fivmr_ThreadHandle_current()));
    fivmr_ThreadState_set(ts,NULL);
    
    fivmRuntime_boot(ts); /* initialize the Java side of the runtime */

    LOG(1,("Attempting to create main thread."));

#if FIVMBUILD__SCJ
    LOG(1,("SCJ support is enabled."));
    LOG(1,("Creating RT VM Thread.."));
    
    /* FIXME: we're passing FIVMR_TPR_RR_MAX ... but perhaps we should passThread_getPriority. */
    if (!fivmr_ThreadState_glue(
            ts,
            fivmr_VMThread_createRT(
                ts,
                (FIVMR_TPR_FIFO_MIN+FIVMR_TPR_FIFO_MAX)/2,
                false))) {
        return;
    }
#else
    
    /* FIXME: we're passing TPR_NORMAL_MIN ... but perhaps we should pass Thread_getPriority. */
    if (!fivmr_ThreadState_glue(
            ts,
            fivmr_VMThread_create(
                ts,
                FIVMR_TPR_NORMAL_MIN,
                false))) {
        return;
    }
#endif
	
    /* This is the first point at which we have enough VM to flow log */
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_VM, FIVMR_FLOWLOG_SUBTYPE_INIT, 0);

    LOG(1,("We now have a main thread; attempting to run post-thread-init payload callback."));
    
    res=vm->payload->postThreadInitCback(ts); /* this is typically used for
                                                 static JNI on-loads */
    fivmr_assert(res);

    if (vm->config.enableHardRTJ) {
        fivmr_assert(vm->maxPriority==FIVMR_TPR_CRITICAL);
        LOG(1,("Initializing HardRTJ..."));
        
        fivmr_initHardRTJ(vm);
    }

    LOG(1,("Attempting to initialize system classes."));
    
    fivmRuntime_initSystemClassLoaders(ts);
    
    /* initialize enough of the system to be able to use Charsets.  that's important,
       because then we can just use Classpath's Charset support for UTF-8 conversions,
       which we will be doing lots of for stuff like native method lookup.
       
       NB. for safety, this should match the list of things being marked used in
       OneWordHeaderContiguousClasspathObjectModel. */
    fivmr_TypeData_checkInitEasy(ts,"java/lang/System");
    fivmr_TypeData_checkInitEasy(ts,"java/lang/String");
#if FIVMR_GLIBJ
    fivmr_TypeData_checkInitEasy(ts,"java/security/VMAccessController");
#endif
    fivmr_TypeData_checkInitEasy(ts,"java/lang/Character");
    fivmr_TypeData_checkInitEasy(ts,"java/lang/Math");
    
    /* these only need to be initialized to make OOME handling cleaner
       (otherwise we get an OOME when logging) */
#if FIVMR_GLIBJ
    fivmr_TypeData_checkInitEasy(ts,"java/lang/VMClass");
#elif FIVMR_FIJICORE
    fivmr_TypeData_checkInitEasy(ts,"java/lang/FCClass");
#endif

    /* notify the runtime that everything needed for Charset usage has been initialized,
       and thus future attempts to convert between C strings and Java strings can use
       Java rather than iconv().
       
       NB. at sufficient log level this will blow up in your face if the right
       chunks of the system have yet to be initialized. */
    fivmRuntime_notifyInitialized(ts);
    
    LOG(1,("System classes initialized; attempting to run user's main method."));

    mainMR=fivmr_TypeData_findStaticMethod(vm,mainClass,"main","([Ljava/lang/String;)V");
    if (mainMR==NULL) {
	fivmr_abortf("Error: cannot find main method in class %s\n",mainClass);
    }
    LOG(1,("Found main method: %s",
	   fivmr_MethodRec_describe(mainMR)));
    args=fivmr_processArgs(ts,argc-1,argv+1);
    
    /* enable normal exception handling */
    vm->exceptionsFatalReason=NULL;
    
    /* Since this is a shortcut around "normal" thread execution, we
     * synthesize a run event here.  This call needs to match the call
     * in VMThread::VMThread_run(). */
    /* FIXME: This really shouldn't be 0, here */
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_THREAD, FIVMR_FLOWLOG_SUBTYPE_RUN, 0);

    argArray[0].H=args;
    fivmr_MethodRec_call(mainMR,ts,NULL,NULL,(fivmr_Value*)argArray,
			 FIVMR_CM_HANDLES|FIVMR_CM_EXEC_STATUS|
			 FIVMR_CM_DISPATCH|FIVMR_CM_CHECKINIT);
    if (ts->curExceptionHandle!=NULL) {
	fivmr_Handle *e=ts->curExceptionHandle;
	ts->curExceptionHandle=NULL;
	fivmr_describeException(ts,e);
	result=1;
    }
    /* Log that we're beginning the shutdown process.  After this point
     * we may not have a convenient ts again. */
    fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_VM, FIVMR_FLOWLOG_SUBTYPE_EXIT, 0);
    /* terminate the main thread. */
    fivmr_ThreadState_terminate(ts);
    
    /* wait for other threads to die and shutdown the VM. */
    if (!fivmr_VM_waitForDeath(vm)) {
        return;
    }

    /* create a thread to do the honors */
    vm->exceptionsFatalReason="Unexpected exception during system shutdown";
    ts=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
    ts->jumpOnExit=(fivmr_JmpBuf*)&jmpbuf;
    if (fivmr_JmpBuf_label((fivmr_JmpBuf*)&jmpbuf)) {
        LOG(1,("Jumped out of VM; returning. (2)"));
        return;
    }
    LOG(1,("At the point right past the exit label."));
    fivmr_ThreadState_setStackHeight(ts,fivmr_Thread_stackHeight()-FIVMR_STACK_HEIGHT_HEADROOM);
    fivmr_ThreadState_setBasePrio(ts,fivmr_Thread_getPriority(fivmr_ThreadHandle_current()));
    fivmr_ThreadState_set(ts,NULL);
    if (!fivmr_ThreadState_glue(ts,fivmr_VMThread_create(ts,FIVMR_TPR_NORMAL_MIN,false))) {
        return;
    }
    vm->exceptionsFatalReason=NULL;
    
    /* Hack ... there's java code after this point, but we won't be able
     * to log it. */
#if FIVMR_FLOW_LOGGING
    fivmr_FlowLog_finalize();
#endif

    /* actually exit */
    fivmr_javaExit(ts,result);
    LOG(1,("fivmr_javaExit() returned; this could be bad!"));
    fivmr_assertNoException(ts,"while attempting to exit");
    
    /* should never get here. */
    fivmr_assert(false);
}


# 1 "fivmr_runtime.c"
/*
 * fivmr_runtime.c
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

#include "fivmr.h"
#include "fivmr_asm.h"
#include "fivmr_logger.h"

fivmr_VM *fivmr_vmListHead;
fivmr_Lock fivmr_vmListLock;

#if FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT
static void sigquit_handler(int sig,siginfo_t *info,void *arg) {
    fivmr_VM *vm;
    fivmr_Lock_lock(&fivmr_vmListLock);
    for (vm=fivmr_vmListHead;vm!=NULL;vm=vm->next) {
        fivmr_CritSemaphore_up(&vm->dumpStateSemaphore);
    }
    fivmr_Lock_unlock(&fivmr_vmListLock);
}

static void sigquit_thread(void *arg) {
    fivmr_VM *vm=(fivmr_VM*)arg;
    LOG(1,("SIGQUIT thread running, waiting for signal."));
    for (;;) {
        fivmr_CritSemaphore_down(&vm->dumpStateSemaphore);
        if (vm->exiting) {
            fivmr_Semaphore_up(&vm->dumpStateDone);
            return;
        }
        fivmr_Log_lockedPrintf("-- START fivmr: Dumping stacks from all threads --\n");
        fivmr_Debug_dumpAllStacks(vm);
        fivmr_Log_lockedPrintf("-- END fivmr: Dumping stacks from all threads --\n");
    }
}
#endif

void fivmr_VM_initGlobal(void) {
#if (FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT)
    struct sigaction sa;
#endif

    fivmr_vmListHead=NULL;
    fivmr_Lock_init(&fivmr_vmListLock,
                    FIVMR_PR_CRITICAL); /* FIXME this needs to be made
                                           into a truly critical lock */
    
#if FIVMR_SUPPORT_SIGQUIT
#  if FIVMR_CAN_HANDLE_SIGQUIT
    bzero(&sa,sizeof(sa));
    sa.sa_sigaction=sigquit_handler;
    sigfillset(&sa.sa_mask);
    sa.sa_flags=SA_SIGINFO|SA_RESTART;
    sigaction(SIGQUIT,&sa,NULL);
    LOG(1,("SIGQUIT debug handler initialized."));
#  else
    LOG(1,("SIGQUIT debug handler not initialized because of inadequate OS support."));
#  endif
#endif
}

void fivmr_VM_resetSettings(fivmr_VM *vm,
                            fivmr_Configuration *config) {
    fivmr_assert(vm!=NULL);
    fivmr_assert(config!=NULL);
    
    /* zero everything except for the lock */
    bzero(vm,sizeof(fivmr_VM));
    
    vm->monitorSpinLimit=100;
    vm->config=*config;
    vm->maxPriority=FIVMR_TPR_MAX;
    fivmr_GC_resetSettings(&vm->gc);
}

void fivmr_VM_init(fivmr_VM *vm) {
    uint32_t i;
    int res;
    uint64_t tmp;
    
    fivmr_assert_cas(&vm->state,FIVMR_VMS_IDLE,FIVMR_VMS_INITING);
    
    LOG(1,("Initializing runtime at %p...",vm));
    
    if (vm->gc.logGC) {
        vm->gc.logSyncGC=true;
    }
    
    LOG(2,("logGC = %d, logSyncGC = %d",vm->gc.logGC,vm->gc.logSyncGC));

    tmp=vm->config.maxThreads; /* HACK! prevents compiler pukage on 64-bit builds */
    fivmr_assert((tmp-1)<=(FIVMR_MS_TID_MASK>>FIVMR_MS_TID_SHIFT));
    fivmr_assert((FIVMR_MS_RC_MASK&FIVMR_MS_TID_MASK)==0);
    fivmr_assert((FIVMR_MS_QUEUED&FIVMR_MS_RC_MASK)==0);
    fivmr_assert((FIVMR_MS_QUEUED&FIVMR_MS_TID_MASK)==0);
    fivmr_assert((FIVMR_MS_UNBIASED&FIVMR_MS_QUEUED)==0);
    fivmr_assert((FIVMR_MS_UNBIASED&FIVMR_MS_RC_MASK)==0);
    fivmr_assert((FIVMR_MS_UNBIASED&FIVMR_MS_TID_MASK)==0);
    
    fivmr_Lock_init(&vm->lock,
		    fivmr_Priority_bound(FIVMR_PR_CRITICAL,
                                         vm->maxPriority));
    
    for (i=0;i<(uint32_t)vm->payload->nContexts;++i) {
        fivmr_TypeContext_boot(vm,vm->baseContexts[i]);
    }
    
    fivmr_Lock_init(&vm->typeDataLock,
                    FIVMR_PR_INHERIT);
    fivmr_Lock_init(&vm->thunkingLock,
                    FIVMR_PR_MAX);
    
    fivmr_Lock_init(&vm->deathLock,
		    fivmr_Priority_bound(FIVMR_PR_MAX,
                                         vm->maxPriority));
    fivmr_Semaphore_init(&vm->softHandshakeNotifier);
    fivmr_Lock_init(&vm->handshakeLock,
		    fivmr_Priority_bound(FIVMR_PR_MAX,
                                         vm->maxPriority));
    fivmr_Lock_init(&vm->hrLock,
		    fivmr_Priority_bound(FIVMR_PR_CRITICAL,
                                         vm->maxPriority));
    fivmr_ThreadSpecific_init(&vm->curThread);
    fivmr_HandleRegion_init(&vm->hr);
    vm->freeHandles=NULL;
    
    vm->dynContexts=NULL;
    vm->nDynContexts=0;
    vm->dynContextsSize=0;
    
    vm->zero=0.0;
    
    LOG(1,("Locks, utils, thread-specifics, and handle-regions initialized."));

#if FIVMR_SUPPORT_SIGQUIT
#  if FIVMR_CAN_HANDLE_SIGQUIT
    fivmr_CritSemaphore_init(&vm->dumpStateSemaphore);
    fivmr_Semaphore_init(&vm->dumpStateDone);
    
    if (vm->pool==NULL) {
        fivmr_Thread_spawn(sigquit_thread,vm,FIVMR_TPR_NORMAL_MIN);
    } else {
        fivmr_ThreadPool_spawn(vm->pool,sigquit_thread,vm,FIVMR_TPR_NORMAL_MIN);
    }

    LOG(1,("SIGQUIT debug support initialized."));
#  else
    LOG(1,("SIGQUIT debug support not initialized because of inadequate OS support."));
#  endif
#endif

    LOG(1,("Allocating memory for %u threads.",vm->config.maxThreads));
    vm->threadById=fivmr_mallocAssert(sizeof(fivmr_ThreadState)*vm->config.maxThreads);
    bzero(vm->threadById,
          sizeof(fivmr_ThreadState)*vm->config.maxThreads);
    
    vm->javaThreads=(fivmr_Handle*)fivmr_mallocAssert(
        sizeof(fivmr_Handle)*vm->config.maxThreads);
    vm->threads=(fivmr_ThreadState**)fivmr_mallocAssert(
        sizeof(fivmr_ThreadState*)*vm->config.maxThreads);
    vm->handshakeThreads=(fivmr_ThreadState**)fivmr_mallocAssert(
        sizeof(fivmr_ThreadState*)*vm->config.maxThreads);

    /* NOTE: the offsetof assertions in this file SHOULD NOT BE REMOVED.  they
       are here because our assembly code (just fivmr_asm_x86.S currently) uses
       those offsets directly.  DO NOT ATTEMPT to remove these assertions, and 
       DO NOT ATTEMPT to change their values unless you change the assembly code
       as well.  And if you don't know why it's failing, or if the previous
       few sentences make no sense, then just go cower in some cave and stop
       trying to hack VMs because it's obviously too much for you to handle. */
    
#if FIVMR_CAN_DO_CLASSLOADING
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveField)==0);
    vm->resolveField=fivmr_resolveField;
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveMethod)==sizeof(void*));
    vm->resolveMethod=fivmr_resolveMethod;
    fivmr_assert(fivmr_offsetof(fivmr_VM,baselineThrow)==sizeof(void*)*2);
    vm->baselineThrow=fivmr_baselineThrow;
    fivmr_assert(fivmr_offsetof(fivmr_VM,pollcheckSlow)==sizeof(void*)*3);
    vm->pollcheckSlow=fivmr_ThreadState_pollcheckSlow;
    fivmr_assert(fivmr_offsetof(fivmr_VM,throwNullPointerRTE_inJava)==sizeof(void*)*4);
    vm->throwNullPointerRTE_inJava=fivmr_throwNullPointerRTE_inJava;
    fivmr_assert(fivmr_offsetof(fivmr_VM,throwArrayBoundsRTE_inJava)==sizeof(void*)*5);
    vm->throwArrayBoundsRTE_inJava=fivmr_throwArrayBoundsRTE_inJava;
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveArrayAlloc)==sizeof(void*)*6);
    vm->resolveArrayAlloc=fivmr_resolveArrayAlloc;
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveObjectAlloc)==sizeof(void*)*7);
    vm->resolveObjectAlloc=fivmr_resolveObjectAlloc;
    fivmr_assert(fivmr_offsetof(fivmr_VM,resolveInstanceof)==sizeof(void*)*8);
    vm->resolveInstanceof=fivmr_resolveInstanceof;
    fivmr_assert(fivmr_offsetof(fivmr_VM,throwStackOverflowRTE_inJava)==sizeof(void*)*9);
    vm->throwStackOverflowRTE_inJava=fivmr_throwStackOverflowRTE_inJava;
#endif

    bzero(vm->javaThreads,sizeof(fivmr_Handle)*vm->config.maxThreads);
    bzero(vm->threads,sizeof(fivmr_ThreadState*)*vm->config.maxThreads);
    bzero(vm->handshakeThreads,sizeof(fivmr_ThreadState*)*vm->config.maxThreads);
    
    if (sizeof(void*)==4) {
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,pollingUnion)==0);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,cookie)==4);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,curException)==8);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,vm)==12);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,curF)==16);
        fivmr_assert((intptr_t)&((fivmr_ThreadState*)0)->gc.alloc[0].zero<=127);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,regSave)==FIVMR_OFFSETOF_REGSAVE_0);
        
        fivmr_assert(sizeof(fivmr_Frame)==sizeof(uintptr_t)*3);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,id)==0);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,up)==4);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,refs)==8);
    } else {
        fivmr_assert(sizeof(void*)==8);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,pollingUnion)==0);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,cookie)==4);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,curException)==8);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,vm)==16);
        fivmr_assert(fivmr_offsetof(fivmr_ThreadState,curF)==24);

        fivmr_assert(sizeof(fivmr_Frame)==sizeof(uintptr_t)*3);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,id)==0);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,up)==8);
        fivmr_assert(fivmr_offsetof(fivmr_Frame,refs)==16);
    }
    
    vm->maxThreadID=1;

    for (i=0;i<vm->config.maxThreads;++i) {
        LOG(2,("Initializing thread slot %u",i));
	fivmr_ThreadState_byId(vm,i)->cookie=0xd1e7c0c0;
	fivmr_ThreadState_byId(vm,i)->id=i;
	fivmr_ThreadState_byId(vm,i)->lockingId=i<<FIVMR_MS_TID_SHIFT;
        fivmr_ThreadState_byId(vm,i)->vm=vm;

#if FIVMR_CAN_DO_CLASSLOADING
        fivmr_ThreadState_byId(vm,i)->pollcheckSlowBaseline=fivmr_pollcheckSlowBaseline;
        fivmr_ThreadState_byId(vm,i)->nullCheckSlowBaseline=fivmr_nullCheckSlowBaseline;
        fivmr_ThreadState_byId(vm,i)->abcSlowBaseline=fivmr_abcSlowBaseline;
        fivmr_ThreadState_byId(vm,i)->stackHeightSlowBaseline=fivmr_stackHeightSlowBaseline;
        fivmr_ThreadState_byId(vm,i)->baselineThrowThunk=fivmr_baselineThrowThunk;
        fivmr_ThreadState_byId(vm,i)->baselineProEpThrowThunk=fivmr_baselineProEpThrowThunk;
        fivmr_ThreadState_byId(vm,i)->resolveFieldAccessThunk=fivmr_resolveFieldAccessThunk;
        fivmr_ThreadState_byId(vm,i)->resolveMethodCallThunk=fivmr_resolveMethodCallThunk;
        fivmr_ThreadState_byId(vm,i)->resolveInvokeInterfaceThunk=fivmr_resolveInvokeInterfaceThunk;
        fivmr_ThreadState_byId(vm,i)->resolveArrayAllocThunk=fivmr_resolveArrayAllocThunk;
        fivmr_ThreadState_byId(vm,i)->resolveObjectAllocThunk=fivmr_resolveObjectAllocThunk;
        fivmr_ThreadState_byId(vm,i)->resolveInstanceofThunk=fivmr_resolveInstanceofThunk;
#endif
        
        /* this needs to be MAX for PIP
           to work; but it means we'll have to
           change the way stack scans work to
           go back to having timely soft
           handshakes - AND we'll have to do
           something about hardrtj. */
        /* details: the reason why we need MAX for PIP is that we rely on
           the property that acquiring the lock makes us the highest priority
           thread on a given processor.  we then may end up contending on
           spin locks, which is safe if we're the highest priority in the
           system but unsafe otherwise. */
	fivmr_Lock_init(&fivmr_ThreadState_byId(vm,i)->lock,
			fivmr_Priority_bound(FIVMR_PR_MAX,
                                             vm->maxPriority));
        fivmr_Semaphore_init(&fivmr_ThreadState_byId(vm,i)->waiter);
    }

    LOG(1,("All thread slots ready."));
    
    if (FIVMR_SCOPED_MEMORY(&vm->settings)) {
        fivmr_MemoryAreas_init(&vm->gc);
    }
    fivmr_GC_init(&vm->gc);
    
    fivmr_Lock_lock(&fivmr_vmListLock);
    vm->prev=NULL;
    vm->next=fivmr_vmListHead;
    if (fivmr_vmListHead!=NULL) {
        fivmr_vmListHead->prev=vm;
    }
    fivmr_vmListHead=vm;
    fivmr_Lock_unlock(&fivmr_vmListLock);

#if FIVMR_INTERNAL_INST
    fivmr_ii_start(vm);
#endif

    fivmr_assert_cas(&vm->state,FIVMR_VMS_INITING,FIVMR_VMS_RUNNING);

    fivmr_debugMemory();

    LOG(1,("Runtime initialized."));
}

bool fivmr_VM_waitForDeath(fivmr_VM *vm) {
    LOG(1,("Waiting for all threads to die..."));
    
    fivmr_Lock_lock(&vm->deathLock);
    while (vm->numActive-vm->numDaemons>0 && !vm->exiting) {
	LOG(2,("Waiting for death with active = %u, daemons = %u",
	       vm->numActive,vm->numDaemons));
	fivmr_Lock_wait(&vm->deathLock);
    }
    fivmr_Lock_unlock(&vm->deathLock);

    if (vm->exiting) {
        LOG(1,("VM force-exited."));
        return false;
    } else {
        LOG(1,("No more threads running.  Ready to exit."));
        return true;
    }
}

#if FIVMR_SPC_ENABLED
void fivmc_SPC_dump(void);
#endif

bool fivmr_VM_exit(fivmr_VM *vm,int32_t status) {
    /* NOTE!!!!  there is still Java code running while this is called.
       For a "normal" JVM this is to be expected.  We cannot reasonably
       prevent it.  So don't try to prevent it, and don't stupidly
       assume that it isn't happening. */
    
    LOG(1,("fivmr_VM_exit() called with status = %d",(int)status));

    if (!fivmr_cas(&vm->state,FIVMR_VMS_RUNNING,FIVMR_VMS_EXITING)) {
        return false;
    }
    
    fivmr_Lock_lock(&vm->lock);
    fivmr_assert(!vm->exitCodeSet);
    vm->exitCode=status;
    vm->exitCodeSet=true;
    fivmr_Lock_unlock(&vm->lock);
    
    fivmr_ThreadState_performAllGuaranteedCommits(vm);
    
#if FIVMR_INTERNAL_INST
    fivmr_ii_end(vm);
#endif
    
#if FIVMR_SPC_ENABLED
    fivmc_SPC_dump();
#endif
    fivmr_SPC_dump();
    fivmr_PR_dump();
    
    if (FIVMR_COVERAGE) {
        int i;
        fivmr_Log_printf("Coverage:\n");
        for (i=0;i<vm->payload->nTypes;++i) {
            fivmr_TypeData *td=fivmr_TypeData_list(vm)[i];
            unsigned j;
            for (j=0;j<td->numMethods;++j) {
                fivmr_MethodRec *mr=td->methods[j];
                if ((mr->flags&FIVMR_MBF_COV_CALLED)) {
                    fivmr_Log_printf("   %s\n",fivmr_MethodRec_describe(mr));
                }
            }
        }
    }
    
    fivmr_GC_finalReport(&vm->gc);
    
    if (vm->exitExits) {
#if FIVMR_POSIX
        fivmr_Log_lock();
        fp_commit(fivmr_log);
        fivmr_Log_unlock();
#else
        /* bonk! */
        fp_commit(stderr);
#endif

        exit((int)status);
    } else {
        fivmr_ThreadState *ts;
        
        /* possible solutions:
           - treat thread death as a special exception
           pro: seems simple
           con: Java code can handle exceptions; so this would have to be an
           exception that the code cannot handle (i.e. not Throwable).
           moreover, safepoints currently cannot throw exceptions.  maybe
           it would be good to keep it that way...  indeed, the compiler
           does not take kindly to the idea of a late-inserted op
           throwing exceptions
           - longjmp
           pro: seems simple
           con: may skip over important code.  makes it harder to handle
           VM death */
        /* we'll use the longjmp trick... */
        
        LOG(1,("Triggering VM death."));
        
        ts=fivmr_ThreadState_getNullable(vm);

        fivmr_Lock_lock(&vm->lock);
        vm->exiting=true;
        vm->exitInitiator=ts;
        fivmr_Lock_unlock(&vm->lock);
        
        /* make sure that the GC signals any threads waiting on a GC */
        fivmr_GC_signalExit(&vm->gc);
        
        /* make sure that soft handshakes exit */
        fivmr_Semaphore_up(&vm->softHandshakeNotifier);
        
        /* no new threads can be started */

        fivmr_ThreadState_softHandshakeImpl(vm,0,0,false,true);
        
        /* threads should start exiting */
        
#if FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT
        fivmr_CritSemaphore_up(&vm->dumpStateSemaphore);
#endif
        
        fivmr_Lock_lockedBroadcast(&vm->deathLock);

        if (ts!=NULL) {
            LOG(1,("Exiting the thread that initiated exit..."));
            fivmr_ThreadState_exitImpl(ts);
            fivmr_assert(false);
        }
    }
    
    return true;
}

void fivmr_VM_shutdown(fivmr_VM *vm,
                       int32_t *exitCode) {
    uint32_t i;
    
    if (FIVMR_ASSERTS_ON) {
        fivmr_assert(vm->state==FIVMR_VMS_EXITING);

        fivmr_Lock_lock(&vm->lock);
        fivmr_assert(vm->exitCodeSet);
        fivmr_Lock_unlock(&vm->lock);
    }
    
    fivmr_Lock_lock(&vm->deathLock);
    while (vm->numRunning>0) {
        fivmr_Lock_wait(&vm->deathLock);
    }
    fivmr_Lock_unlock(&vm->deathLock);
    
    fivmr_Lock_lock(&vm->typeDataLock);
    fivmr_Lock_unlock(&vm->typeDataLock);
    
#if FIVMR_SUPPORT_SIGQUIT && FIVMR_CAN_HANDLE_SIGQUIT
    fivmr_Semaphore_down(&vm->dumpStateDone);
#endif
    
    fivmr_GC_shutdown(&vm->gc);

    fivmr_free(vm->javaThreads);
    fivmr_free(vm->threads);
    fivmr_free(vm->handshakeThreads);
    
    if (vm->payload->mode!=FIVMR_PL_IMMORTAL_ONESHOT) {
        fivmr_free(vm->primFields);
        fivmr_free(vm->refFields);
    }
    
    for (i=0;i<(uint32_t)vm->payload->nContexts;++i) {
        fivmr_TypeContext_destroy(vm->baseContexts[i]);
   }
    /* FIXME: free the TypeDataNode's */
    
    fivmr_Payload_free(vm->payload);
    
    for (i=0;i<vm->config.maxThreads;++i) {
        fivmr_Lock_destroy(&fivmr_ThreadState_byId(vm,i)->lock);
        fivmr_Semaphore_destroy(&fivmr_ThreadState_byId(vm,i)->waiter);
        if (fivmr_ThreadState_byId(vm,i)->buf!=NULL) {
            fivmr_free(fivmr_ThreadState_byId(vm,i)->buf);
        }
    }
    
    fivmr_free(vm->threadById);
    fivmr_free(vm->usedTids);
    fivmr_free(vm->itableOcc);
    
    fivmr_Lock_destroy(&vm->lock);
    fivmr_Lock_destroy(&vm->deathLock);
    fivmr_Semaphore_destroy(&vm->softHandshakeNotifier);
    fivmr_Lock_destroy(&vm->handshakeLock);
    fivmr_Lock_destroy(&vm->hrLock);
    fivmr_ThreadSpecific_destroy(&vm->curThread); /* FIXME this doesn't work yet on
                                                     RTEMS */
    fivmr_Lock_destroy(&vm->typeDataLock);
    fivmr_Lock_destroy(&vm->thunkingLock);
    
    while (vm->freeHandles!=NULL) {
        fivmr_Handle *next=vm->freeHandles->next;
        free(vm->freeHandles);
        vm->freeHandles=next;
    }
    
    fivmr_Lock_lock(&fivmr_vmListLock);
    if (vm->prev==NULL) {
        fivmr_vmListHead=vm->next;
    } else {
        vm->prev->next=vm->next;
    }
    if (vm->next!=NULL) {
        vm->next->prev=vm->prev;
    }
    fivmr_Lock_unlock(&fivmr_vmListLock);
    
    if (exitCode!=NULL) {
        fivmr_assert(vm->exitCodeSet);
        *exitCode=vm->exitCode;
    }
    
    fivmr_debugMemory();

    LOG(1,("Shut down runtime at %p.",vm));
}



# 1 "fivmr_semaphore.c"
/*
 * fivmr_semaphore.c
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

#include <fivmr_config.h>
#include "fivmr.h"

#if FIVMR_POSIX
#  ifdef HAVE_WORKING_SEM_INIT
void fivmr_CritSemaphore_init(fivmr_CritSemaphore *sema) {
    int res;
    res=sem_init(&sema->sema,0,0);
    fivmr_assert(res==0);
}

void fivmr_CritSemaphore_up(fivmr_CritSemaphore *sema) {
    int res;
    res=sem_post(&sema->sema);
    fivmr_assert(res==0);
}

void fivmr_CritSemaphore_down(fivmr_CritSemaphore *sema) {
    int res;
    do {
        res=sem_wait(&sema->sema);
    } while (res!=0 && errno==EINTR);
    fivmr_assert(res==0);
}

void fivmr_CritSemaphore_destroy(fivmr_CritSemaphore *sema) {
    int res;
    res=sem_destroy(&sema->sema);
    fivmr_assert(res==0);
}
#  else
void fivmr_CritSemaphore_init(fivmr_CritSemaphore *sema) {
    int res;
    long oldflags;
    res=pipe(sema->pipe);
    fivmr_assert(res==0);
    res=fcntl(sema->pipe[1],F_GETFL);
    fivmr_assert(res!=-1);
    oldflags=(long)res;
    res=fcntl(sema->pipe[1],F_SETFL,oldflags|O_NONBLOCK);
    fivmr_assert(res==0);
}

void fivmr_CritSemaphore_up(fivmr_CritSemaphore *sema) {
    int res;
    char c;
    res=write(sema->pipe[1],&c,1);
    /* NB we joyfully accept cases where the semaphore "overflows" because the
       pipe's buffer got full. */
    fivmr_assert(res==1 || (res==-1 && (errno==EAGAIN || errno==EWOULDBLOCK)));
}

void fivmr_CritSemaphore_down(fivmr_CritSemaphore *sema) {
    int res;
    char c;
    do {
        res=read(sema->pipe[0],&c,1);
    } while (res!=0 && errno==EINTR);
    fivmr_assert(res==1);
}

void fivmr_CritSemaphore_destroy(fivmr_CritSemaphore *sema) {
    int i;
    for (i=0;i<2;++i) {
        int res;
        res=close(sema->pipe[i]);
        fivmr_assert(res==0);
    }
}
#  endif
#elif FIVMR_RTEMS
void fivmr_CritSemaphore_init(fivmr_CritSemaphore *sema) {
    rtems_status_code status;
    status=rtems_semaphore_create(
        fivmr_xchg_add32(&fivmr_nextSemaName,1),
        0,
        RTEMS_COUNTING_SEMAPHORE|RTEMS_PRIORITY,
        RTEMS_NO_PRIORITY,
        &sema->sema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_CritSemaphore_up(fivmr_CritSemaphore *sema) {
    rtems_status_code status;
    status=rtems_semaphore_release(sema->sema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_CritSemaphore_down(fivmr_CritSemaphore *sema) {
    rtems_status_code status;
    fivmr_assert(!fivmr_Thread_isCritical());
    /* printk("obtaining semaphore %p\n",sema); */
    status=rtems_semaphore_obtain(sema->sema,RTEMS_WAIT,RTEMS_NO_TIMEOUT);
    /* printk("got semaphore %p\n",sema); */
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}

void fivmr_CritSemaphore_destroy(fivmr_CritSemaphore *sema) {
    rtems_status_code status;
    status=rtems_semaphore_delete(sema->sema);
    fivmr_assert(status==RTEMS_SUCCESSFUL);
}
#endif

#if FIVMR_POSIX && !defined(HAVE_WORKING_SEM_INIT)
void fivmr_Semaphore_init(fivmr_Semaphore *sema) {
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
#ifdef HAVE_PTHREAD_PIP
    pthread_mutexattr_setprotocol(&attr,PTHREAD_PRIO_INHERIT);
#endif
    pthread_mutex_init(&sema->mutex,&attr);
    pthread_cond_init(&sema->cond,NULL);
}

void fivmr_Semaphore_up(fivmr_Semaphore *sema) {
    pthread_mutex_lock(&sema->mutex);
    sema->count++;
    pthread_cond_broadcast(&sema->cond); /* use broadcast instead of signal out
                                            of an abundance of caution */
    pthread_mutex_unlock(&sema->mutex);
}

void fivmr_Semaphore_down(fivmr_Semaphore *sema) {
    pthread_mutex_lock(&sema->mutex);
    while (sema->count==0) {
        pthread_cond_wait(&sema->cond,&sema->mutex);
    }
    sema->count--;
    pthread_mutex_unlock(&sema->mutex);
}

void fivmr_Semaphore_destroy(fivmr_Semaphore *sema) {
    pthread_mutex_destroy(&sema->mutex);
    pthread_cond_destroy(&sema->cond);
}
#endif
# 1 "fivmr_spc.c"
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

# 1 "fivmr_stackalloc.c"
/*
 * fivmr_stackalloc.c
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

void fivmr_SA_init(fivmr_ThreadState *ts) {
    fivmr_GCSpaceAlloc *alloc=ts->gc.alloc+FIVMR_GC_SA_SPACE;
    alloc->bump=alloc->start=
	(uintptr_t)fivmr_mallocAssert(ts->vm->config.stackAllocSize)
	+FIVMR_ALLOC_OFFSET(&ts->vm->settings);
    alloc->size=ts->vm->config.stackAllocSize;
}

void fivmr_SA_destroy(fivmr_ThreadState *ts) {
    fivmr_GCSpaceAlloc *alloc=ts->gc.alloc+FIVMR_GC_SA_SPACE;
    if (alloc->start==0) {
        /* we never initialized or we already destroyed ... either way, assert that this
           is indeed the case.  this scenario is actually likely to come up: a checkExit()
           can occur after the thread is already finalized, in which case it'll be
           finalized again.  We could of course prevent this in threadstate.c, but it
           seems that it's more natural to just require all finalization routines to
           have their own checks for double-finalization.  in particular, the GC already
           effectively has such checks, since for the GC finalization just means
           committing mutator state, and it's already the case that a commit the
           comes immediately after another commit with no interleaved allocation or
           marking activity is no-op.  Thus, this is the only place where a check for
           double finalization is necessary. */
        fivmr_assert(alloc->start==0);
        fivmr_assert(alloc->bump==0);
        fivmr_assert(alloc->size==0);
    } else {
        fivmr_free((void*)(alloc->start-FIVMR_ALLOC_OFFSET(&ts->vm->settings)));
        alloc->start=0;
        alloc->bump=0;
        alloc->size=0;
    }
}

void fivmr_SA_clear(fivmr_ThreadState *ts) {
    fivmr_GCSpaceAlloc *alloc=ts->gc.alloc+FIVMR_GC_SA_SPACE;
    fivmr_assert(alloc->start==0);
    fivmr_assert(alloc->bump==0);
    fivmr_assert(alloc->size==0);
}

# 1 "fivmr_sysdep_priority.c"
/*
 * fivmr_sysdep_priority.c
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

#include <fivmr_config.h>
#include "fivmr.h"

const char *fivmr_ThreadPriority_schedulerName(fivmr_ThreadPriority pr) {
    switch (fivmr_ThreadPriority_scheduler(pr)) {
    case FIVMR_TPR_JAVA: return "Java";
    case FIVMR_TPR_NORMAL: return "Normal";
    case FIVMR_TPR_RR: return "RR";
    case FIVMR_TPR_FIFO: return "FIFO";
    case FIVMR_TPR_FIVM: return "FIVM";
    default:
        fivmr_abortf("Invalid scheduler: %d",fivmr_ThreadPriority_scheduler(pr));
        return NULL;
    }
}

void fivmr_ThreadPriority_describe(fivmr_ThreadPriority pr,
                                   char *buf,
                                   int32_t len) {
    snprintf(buf,len,"%s%d",
             fivmr_ThreadPriority_schedulerName(pr),
             fivmr_ThreadPriority_priority(pr));
}

fivmr_ThreadPriority fivmr_ThreadPriority_parseScheduler(char const **sched) {
    /* use memcmp because it's more likely to be portable */
    
    if (!memcmp(*sched,"Java",4)) {
        (*sched)+=4;
        return FIVMR_TPR_JAVA;
    } else if (!memcmp(*sched,"Normal",6)) {
        (*sched)+=6;
        return FIVMR_TPR_NORMAL;
    } else if (!memcmp(*sched,"RR",2)) {
        (*sched)+=2;
        return FIVMR_TPR_RR;
    } else if (!memcmp(*sched,"FIFO",4)) {
        (*sched)+=4;
        return FIVMR_TPR_FIFO;
    } else {
        return FIVMR_TPR_INVALID;
    }
}

fivmr_ThreadPriority fivmr_ThreadPriority_parse(const char *prstr) {
    const char *str;
    fivmr_ThreadPriority result;
    int32_t prio;
    char buf[32];
    
    str=prstr;
    result=fivmr_ThreadPriority_parseScheduler(&str);
    
    if (result==FIVMR_TPR_INVALID) {
        LOG(2,("failed to parse %s because the scheduler wasn't valid",prstr));
        return FIVMR_TPR_INVALID;
    }
    
    if (sscanf(str,"%d",&prio)!=1 ||
        prio<0 || prio>0xffff) {
        LOG(2,("failed to parse %s because the priority wasn't valid",prstr));
        return FIVMR_TPR_INVALID;
    }
    
    snprintf(buf,sizeof(buf),"%s%d",
             fivmr_ThreadPriority_schedulerName(result),
             prio);
    
    if (strcmp(buf,prstr)) {
        LOG(2,("failed to parse %s because %s != %s",prstr,buf,prstr));
        return FIVMR_TPR_INVALID;
    }
    
    result=fivmr_ThreadPriority_withPriority(result,prio);

    if (result<fivmr_ThreadPriority_minPriority(result) ||
        result>fivmr_ThreadPriority_maxPriority(result)) {
        LOG(2,("failed to parse %s because the priority is out of range",prstr));
        return FIVMR_TPR_INVALID;
    }
    
    return result;
}

int32_t fivmr_ThreadPriority_minPriority(fivmr_ThreadPriority pr) {
    switch (fivmr_ThreadPriority_scheduler(pr)) {
    case FIVMR_TPR_JAVA: return FIVMR_TPR_JAVA_MIN;
    case FIVMR_TPR_NORMAL: return FIVMR_TPR_NORMAL_MIN;
    case FIVMR_TPR_RR: return FIVMR_TPR_RR_MIN;
    case FIVMR_TPR_FIFO: return FIVMR_TPR_FIFO_MIN;
    default:
        fivmr_abortf("Invalid scheduler for minPriority: %d",pr);
        return 0;
    }
}

int32_t fivmr_ThreadPriority_maxPriority(fivmr_ThreadPriority pr) {
    switch (fivmr_ThreadPriority_scheduler(pr)) {
    case FIVMR_TPR_JAVA: return FIVMR_TPR_JAVA_MAX;
    case FIVMR_TPR_NORMAL: return FIVMR_TPR_NORMAL_MAX;
    case FIVMR_TPR_RR: return FIVMR_TPR_RR_MAX;
    case FIVMR_TPR_FIFO: return FIVMR_TPR_FIFO_MAX;
    default:
        fivmr_abortf("Invalid scheduler for maxPriority: %d",pr);
        return 0;
    }
}

fivmr_ThreadPriority fivmr_ThreadPriority_withScheduler(fivmr_ThreadPriority pr,
                                                        fivmr_ThreadPriority sched) {
    fivmr_ThreadPriority oldMin=fivmr_ThreadPriority_minPriority(pr);
    fivmr_ThreadPriority newMin=fivmr_ThreadPriority_minPriority(sched);
    fivmr_ThreadPriority oldMax=fivmr_ThreadPriority_maxPriority(pr);
    fivmr_ThreadPriority newMax=fivmr_ThreadPriority_maxPriority(sched);
    fivmr_assert(pr>=oldMin && pr<=oldMax);
    return newMin+(pr-oldMin)*(newMax-newMin+1)/(oldMax-oldMin+1);
}

fivmr_ThreadPriority fivmr_ThreadPriority_fromPriority(fivmr_ThreadPriority sched,
                                                       fivmr_Priority prio) {
    fivmr_ThreadPriority oldMin=FIVMR_PR_MIN;
    fivmr_ThreadPriority newMin=fivmr_ThreadPriority_minPriority(sched);
    fivmr_ThreadPriority oldMax=FIVMR_PR_MAX;
    fivmr_ThreadPriority newMax=fivmr_ThreadPriority_maxPriority(sched);
    fivmr_assert(prio>=oldMin && prio<=oldMax);
    return newMin+(prio-oldMin)*(newMax-newMin+1)/(oldMax-oldMin+1);
}

fivmr_Priority fivmr_ThreadPriority_asPriority(fivmr_ThreadPriority prio) {
    fivmr_ThreadPriority oldMin=fivmr_ThreadPriority_minPriority(prio);
    fivmr_ThreadPriority newMin=FIVMR_PR_MIN;
    fivmr_ThreadPriority oldMax=fivmr_ThreadPriority_maxPriority(prio);
    fivmr_ThreadPriority newMax=FIVMR_PR_MAX;
    fivmr_assert(prio>=oldMin && prio<=oldMax);
    return newMin+(prio-oldMin)*(newMax-newMin+1)/(oldMax-oldMin+1);
}

fivmr_Priority fivmr_Priority_bound(fivmr_Priority prio,
                                    fivmr_ThreadPriority maxPrio) {
    fivmr_Priority result;
    if (maxPrio==FIVMR_TPR_CRITICAL) {
        result=prio;
    } else {
        fivmr_ThreadPriority sched=fivmr_ThreadPriority_scheduler(maxPrio);
        if (sched==FIVMR_TPR_RR || sched==FIVMR_TPR_FIFO) {
            result=fivmr_min(prio,fivmr_ThreadPriority_asPriority(maxPrio));
        } else if (sched==FIVMR_TPR_JAVA || sched==FIVMR_TPR_NORMAL) {
            result=FIVMR_PR_NONE;
        } else {
            fivmr_assert(!"bad priority");
            result=0; /* make GCC happy */
        }
    }
    LOG(6,("given priority %d and max prio %d returning %d",
           prio,maxPrio,result));
    return result;
}

fivmr_ThreadPriority fivmr_ThreadPriority_canonicalize(fivmr_ThreadPriority pr) {
    if (fivmr_ThreadPriority_scheduler(pr)==FIVMR_TPR_JAVA) {
        return fivmr_ThreadPriority_withScheduler(pr,FIVMR_TPR_NORMAL);
    } else {
        return pr;
    }
}

bool fivmr_ThreadPriority_eq(fivmr_ThreadPriority pr1,
                             fivmr_ThreadPriority pr2) {
    return fivmr_ThreadPriority_canonicalize(pr1)
        == fivmr_ThreadPriority_canonicalize(pr2);
}

fivmr_ThreadPriority fivmr_ThreadPriority_canonicalizeRT(fivmr_ThreadPriority pr) {
    switch (fivmr_ThreadPriority_scheduler(pr)) {
    case FIVMR_TPR_NORMAL:
    case FIVMR_TPR_JAVA: return FIVMR_TPR_MIN;
    default: return pr;
    }
}

bool fivmr_ThreadPriority_eqRT(fivmr_ThreadPriority pr1,
                               fivmr_ThreadPriority pr2) {
    return fivmr_ThreadPriority_canonicalizeRT(pr1)
        == fivmr_ThreadPriority_canonicalizeRT(pr2);
}

bool fivmr_ThreadPriority_ltRT(fivmr_ThreadPriority pr1,
                               fivmr_ThreadPriority pr2) {
    pr1=fivmr_ThreadPriority_canonicalizeRT(pr1);
    pr2=fivmr_ThreadPriority_canonicalizeRT(pr2);
    if (pr2==FIVMR_TPR_MIN) {
        return false;
    }
    if (pr1==FIVMR_TPR_MIN) {
        return true;
    }
    if (pr1==FIVMR_TPR_CRITICAL) {
        return false;
    }
    if (pr2==FIVMR_TPR_CRITICAL) {
        return true;
    }
    if (FIVMR_TPR_PRIORITY(pr1)<FIVMR_TPR_PRIORITY(pr2)) {
        return true;
    }
    /* currently we assume, for the purposes of sorting, that two priorities are
       equal if the priority numbers are equal but even if the schedulers are
       different.  this assumption is actually used throughout the system.  so
       we shouldn't change it. */
    return false && FIVMR_TPR_SCHEDULER(pr1)<FIVMR_TPR_SCHEDULER(pr2);
}

fivmr_ThreadPriority fivmr_ThreadPriority_max(fivmr_ThreadPriority pr1,
                                              fivmr_ThreadPriority pr2) {
    pr1=fivmr_ThreadPriority_canonicalize(pr1);
    pr2=fivmr_ThreadPriority_canonicalize(pr2);
    if (pr1==FIVMR_TPR_CRITICAL || pr2==FIVMR_TPR_CRITICAL) {
        return FIVMR_TPR_CRITICAL;
    }
    switch (fivmr_ThreadPriority_scheduler(pr1)) {
    case FIVMR_TPR_NORMAL:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return fivmr_max(pr1,pr2);
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return pr2;
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    case FIVMR_TPR_RR:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return pr1;
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return fivmr_ThreadPriority_withPriority(
                pr2,
                fivmr_max(fivmr_ThreadPriority_priority(pr1),
                          fivmr_ThreadPriority_priority(pr2)));
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    case FIVMR_TPR_FIFO:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return pr1;
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return fivmr_ThreadPriority_withPriority(
                pr1,
                fivmr_max(fivmr_ThreadPriority_priority(pr1),
                          fivmr_ThreadPriority_priority(pr2)));
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    default:
        fivmr_abortf("bad priority");
        return 0;
    }
}

fivmr_ThreadPriority fivmr_ThreadPriority_min(fivmr_ThreadPriority pr1_,
                                              fivmr_ThreadPriority pr2_) {
    fivmr_ThreadPriority pr1,pr2;
    pr1=fivmr_ThreadPriority_canonicalize(pr1_);
    pr2=fivmr_ThreadPriority_canonicalize(pr2_);
    if (pr1==FIVMR_TPR_CRITICAL) {
        return pr2;
    } else if (pr2==FIVMR_TPR_CRITICAL) {
        return pr1;
    }
    switch (fivmr_ThreadPriority_scheduler(pr1)) {
    case FIVMR_TPR_NORMAL:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return fivmr_min(pr1,pr2);
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return pr1_;
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    case FIVMR_TPR_RR:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return pr2_;
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return fivmr_ThreadPriority_withPriority(
                pr1,
                fivmr_min(fivmr_ThreadPriority_priority(pr1),
                          fivmr_ThreadPriority_priority(pr2)));
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    case FIVMR_TPR_FIFO:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return pr2_;
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return fivmr_ThreadPriority_withPriority(
                pr2,
                fivmr_min(fivmr_ThreadPriority_priority(pr1),
                          fivmr_ThreadPriority_priority(pr2)));
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    default:
        fivmr_abortf("bad priority");
        return 0;
    }
}

bool fivmr_ThreadPriority_isRT(fivmr_ThreadPriority pr) {
    return FIVMR_PR_SUPPORTED
        && (FIVMR_TPR_SCHEDULER(fivmr_ThreadPriority_canonicalizeRT(pr))
            != FIVMR_TPR_NORMAL);
}




# 1 "fivmr_sysdep_util.c"
/*
 * fivmr_sysdep_util.c
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

#include <fivmr_config.h>
#include "fivmr.h"

/* put system-dependent stuff that is common here.  I realize that seems like a
   contradiction but it isn't. */

typedef struct {
    char *buf;
    size_t size;
} ThreadStringBuf;

static fivmr_ThreadSpecific threadStringBuf;
static fivmr_ThreadSpecific threadCanExit;

static void threadStringBuf_destructor(uintptr_t value) {
    ThreadStringBuf *buf=(ThreadStringBuf*)value;
    if (buf!=NULL) {
	free(buf->buf);
    }
}

void fivmr_SysDepUtil_init(void) {
    fivmr_ThreadSpecific_initWithDestructor(&threadStringBuf,
					    threadStringBuf_destructor);
    fivmr_ThreadSpecific_init(&threadCanExit);
}

void fivmr_preinitThreadStringBufFor(fivmr_ThreadHandle th) {
    int size=1024;
    ThreadStringBuf *buf;
    buf=(ThreadStringBuf*)malloc(sizeof(ThreadStringBuf));
    fivmr_assert(buf!=NULL);
    buf->size=size;
    buf->buf=malloc(size);
    fivmr_assert(buf->buf!=NULL);
    fivmr_ThreadSpecific_setForThread(&threadStringBuf,th,(uintptr_t)buf);
}

char *fivmr_threadStringBuf(size_t size) {
    ThreadStringBuf *buf=(ThreadStringBuf*)
	fivmr_ThreadSpecific_get(&threadStringBuf);
    if (buf!=NULL && buf->size<size) {
	if (false) printf("freeing old buf\n");
	free(buf->buf);
    }
    if (buf==NULL) {
	if (false) printf("buf was null\n");
	buf=(ThreadStringBuf*)malloc(sizeof(ThreadStringBuf));
	fivmr_assert(buf!=NULL);
	buf->size=0;
	fivmr_ThreadSpecific_set(&threadStringBuf,(uintptr_t)buf);
    }
    if (size>buf->size) {
	if (false) printf("allocating new buf\n");
	buf->buf=(char*)malloc(size);
	fivmr_assert(buf->buf!=NULL);
	buf->buf[0]=0;
	buf->size=size;
    }
    return buf->buf;
}

const char *fivmr_getThreadStringBuf(void) {
    if (false) printf("in getThreadStringBuf\n");
    return fivmr_threadStringBuf(1);
}

char *fivmr_tsprintf(const char *msg,...) {
    va_list lst;
    size_t size;
    ThreadStringBuf empty;
    ThreadStringBuf *buf;
    char *result;
    empty.buf=NULL;
    empty.size=0;
    buf=(ThreadStringBuf*)fivmr_ThreadSpecific_get(&threadStringBuf);
    if (buf==NULL) {
	if (false) printf("buf is null\n");
	buf=&empty;
    }
    va_start(lst,msg);
    size=vsnprintf(buf->buf,buf->size,msg,lst)+1;
    if (false) printf("buf->size = %d, size = %d\n",(int)buf->size,(int)size);
    va_end(lst);
    if (size<=buf->size) {
	if (false) printf("returning %s\n",buf->buf);
	return buf->buf;
    }
    result=fivmr_threadStringBuf(size);
    va_start(lst,msg);
    vsnprintf(result,size,msg,lst);
    va_end(lst);
    if (false) printf("returning %s\n",result);
    return result;
}

double fivmr_parseDouble(const char *cstr) {
    /* from ovm */
    char* endptr;
    double val = 0.0;
    val = strtod(cstr, &endptr);
    return val;
}

int32_t fivmr_availableProcessors(void) {
    return 2; /* FIXME */
}

void *fivmr_allocPages_IMPL(uintptr_t numPages,
                            bool *isZero,
                            const char *whereFile,
                            int whereLine) {
    void *result=fivmr_tryAllocPages_IMPL(numPages,isZero,whereFile,whereLine);
    if (result==NULL) {
        fivmr_abortf("Could not allocate %p pages at %s:%d.",
                     numPages,whereFile,whereLine);
    }
    return result;
}

const char *fivmr_OS_name(void) {
    return FIVMSYS_OS;
}

const char *fivmr_OS_arch(void) {
    return FIVMSYS_ARCH;
}

bool fivmr_Thread_canExit(void) {
    return fivmr_ThreadSpecific_get(&threadCanExit)==0;
}

void fivmr_Thread_disableExit(void) {
    fivmr_ThreadSpecific_set(&threadCanExit,1);
}


# 1 "fivmr_threadpool.c"
/*
 * fivmr_threadpool.c
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

static void pool_runner(void *arg) {
    fivmr_PooledThread *pt;
    volatile fivmr_SuspensionData sp;

    pt=(fivmr_PooledThread*)arg;
    pt->susp=fivmr_Thread_makeSuspendable((fivmr_SuspensionData*)&sp);
    LOG(1,("in thread %p: pointer to sp = %p, pt->susp = %p",
           fivmr_ThreadHandle_current(),&sp,pt->susp));
    
    /* make damn sure that nobody calls fivmr_Thread_exit */
    fivmr_Thread_disableExit();
    
    for (;;) {
        fivmr_Lock_lock(&pt->pool->lock);
        fivmr_assert(pt->pool->nfree < pt->pool->nthreads);
        pt->active=false;
        pt->runner=NULL;
        pt->arg=NULL;
        pt->activePriority=FIVMR_TPR_MIN;
        pt->pool->freeIndices[pt->pool->nfree]=pt->index;
        pt->pool->nfree++;
        fivmr_Lock_broadcast(&pt->pool->lock);
        while (!pt->active) {
            fivmr_Lock_wait(&pt->pool->lock);
        }
        fivmr_Lock_unlock(&pt->pool->lock);
        
        fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                                 pt->activePriority);
        
        pt->runner(pt->arg);
        
        fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                                 pt->pool->defaultPriority);
    }
}

void fivmr_ThreadPool_init(fivmr_ThreadPool *pool,
                           uintptr_t nthreads,
                           fivmr_ThreadPriority defaultPriority) {
    uintptr_t i;
    bzero(pool,sizeof(fivmr_ThreadPool));
    fivmr_Lock_init(&pool->lock,FIVMR_PR_MAX);
    pool->defaultPriority=defaultPriority;
    pool->nthreads=nthreads;
    pool->threads=fivmr_mallocAssert(sizeof(fivmr_PooledThread)*pool->nthreads);
    bzero(pool->threads,sizeof(fivmr_PooledThread)*pool->nthreads);
    for (i=0;i<nthreads;++i) {
        pool->threads[i].pool=pool;
        pool->threads[i].index=i;
        pool->threads[i].active=false;
        pool->threads[i].arg=NULL;
        pool->threads[i].runner=NULL;
    }
    pool->nfree=0;
    pool->freeIndices=fivmr_mallocAssert(sizeof(uintptr_t)*nthreads);
    bzero(pool->freeIndices,sizeof(uintptr_t)*nthreads);
    for (i=0;i<nthreads;++i) {
        pool->threads[i].thread=
            fivmr_Thread_spawn(pool_runner,pool->threads+i,pool->defaultPriority);
        fivmr_assert(pool->threads[i].thread!=fivmr_ThreadHandle_zero());
    }
    
    /* wait for the pool to become fully active */
    fivmr_Lock_lock(&pool->lock);
    while (pool->nfree<pool->nthreads) {
        fivmr_Lock_wait(&pool->lock);
    }
    fivmr_Lock_unlock(&pool->lock);
}

fivmr_PooledThread *fivmr_ThreadPool_spawn(fivmr_ThreadPool *pool,
                                           void (*runner)(void *arg),
                                           void *arg,
                                           fivmr_ThreadPriority activePriority) {
    fivmr_PooledThread *pt;
    fivmr_assert(pool!=NULL);
    fivmr_assert(fivmr_ThreadPriority_leRT(activePriority,pool->defaultPriority));
    fivmr_assert(activePriority!=0);
    fivmr_Lock_lock(&pool->lock);
    while (pool->nfree==0) {
        fivmr_Lock_wait(&pool->lock);
    }
    pt=pool->threads+pool->freeIndices[--pool->nfree];
    pt->active=true;
    pt->runner=runner;
    pt->arg=arg;
    pt->activePriority=activePriority;
    fivmr_Lock_broadcast(&pool->lock);
    fivmr_Lock_unlock(&pool->lock);
    return pt;
}

void fivmr_PooledThread_suspend(fivmr_PooledThread *pt) {
    fivmr_Thread_suspend(pt->susp);
}

void fivmr_PooledThread_resume(fivmr_PooledThread *pt) {
    fivmr_Thread_resume(pt->susp);
}

void fivmr_ThreadPool_suspend(fivmr_ThreadPool *pool) {
    uintptr_t i;
    for (i=0;i<pool->nthreads;++i) {
        fivmr_PooledThread_suspend(pool->threads+i);
    }
    LOG(2,("Sent %d suspend signals",pool->nthreads));
}

void fivmr_ThreadPool_resume(fivmr_ThreadPool *pool) {
    uintptr_t i;
    for (i=0;i<pool->nthreads;++i) {
        fivmr_PooledThread_resume(pool->threads+i);
    }
    LOG(2,("Sent %d resume signals",pool->nthreads));
}


# 1 "fivmr_threadqueue.c"
/*
 * fivmr_threadqueue.c
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

#include "fivmr.h"

void fivmr_ThreadQueue_init(fivmr_ThreadQueue *queue) {
    queue->head=NULL;
    queue->tail=NULL;
    queue->flags=0;
    fivmr_BoostedSpinLock_init(&queue->lock);
}

static void verifyQueue(fivmr_ThreadQueue *queue) {
    int cnt=0;
    fivmr_ThreadState *cur;
    for (cur=queue->head;
         cur!=NULL;
         cur=cur->forMonitor.next) {
        cnt++;
        if (cur->forMonitor.next==NULL) {
            fivmr_assert(queue->tail==cur);
        }
    }
    if (cnt==0) {
        fivmr_assert(queue->head==NULL);
        fivmr_assert(queue->tail==NULL);
    }
    if (cnt==1) {
        fivmr_assert(queue->head==queue->tail);
    }
    if (cnt==2) {
        fivmr_assert(queue->head->forMonitor.next==queue->tail);
    }
}

static void enqueue(fivmr_ThreadQueue *queue,
                    fivmr_ThreadState *ts) {
    fivmr_ThreadPriority myprio;
    bool inserted=false;
    
    fivmr_assert(ts->forMonitor.next==NULL);

    myprio=ts->curPrio;
    
    if (fivmr_ThreadPriority_isRT(myprio)) {
        fivmr_ThreadState **cur;
        for (cur=&queue->head;
             (*cur)!=NULL;
             cur=&(*cur)->forMonitor.next) {
            if (fivmr_ThreadPriority_gtRT(
                    myprio,
                    (*cur)->curPrio)) {
                ts->forMonitor.next=*cur;
                *cur=ts;
                inserted=true;
                break;
            }
        }
    }
    
    if (!inserted) {
        if (queue->head==NULL) {
            queue->head=ts;
            queue->tail=ts;
        } else {
            queue->tail->forMonitor.next=ts;
            queue->tail=ts;
        }
    }
}

void fivmr_ThreadQueue_enqueue(fivmr_ThreadQueue *queue,
                               fivmr_ThreadState *ts) {
    fivmr_assert(ts->forMonitor.queuedOnReal==NULL);
    fivmr_assert(ts->forMonitor.queuedOnIntended==NULL);
    
    enqueue(queue,ts);
    
    ts->forMonitor.queuedOnReal=queue;
    ts->forMonitor.queuedOnIntended=queue;
}

fivmr_ThreadState *fivmr_ThreadQueue_dequeue(fivmr_ThreadQueue *queue) {
    fivmr_ThreadState *result;
    fivmr_ThreadState *next;
    
    result=queue->head;
    fivmr_assert(result!=NULL);
    next=result->forMonitor.next;
    if (next==NULL) {
	queue->head=NULL;
	queue->tail=NULL;
    } else {
	queue->head=next;
	result->forMonitor.next=NULL;
    }
    
    fivmr_assert(result->forMonitor.queuedOnReal==queue);
    fivmr_assert(result->forMonitor.queuedOnIntended==queue);
    result->forMonitor.queuedOnReal=NULL;
    
    return result;
}

static bool removeFromQueue(fivmr_ThreadQueue *queue,
                            fivmr_ThreadState *ts) {
    bool result=false;
    fivmr_ThreadState *last=NULL;
    fivmr_ThreadState **cur;
    for (cur=&queue->head;
         (*cur)!=NULL;
         cur=&((*cur)->forMonitor.next)) {
        if ((*cur)==ts) {
            (*cur)=ts->forMonitor.next;
            ts->forMonitor.next=NULL;
            result=true;
            break;
        } else {
            last=*cur;
        }
    }
    if (result && queue->tail==ts) {
        queue->tail=last;
    }
    if (false && FIVMR_ASSERTS_ON) verifyQueue(queue);
    return result;
}

bool fivmr_ThreadQueue_remove(fivmr_ThreadQueue *queue,
                              fivmr_ThreadState *ts) {
    if (fivmr_ThreadQueue_isQueued(queue,ts)) {
        bool result=removeFromQueue(queue,ts);
        fivmr_assert(result);
        ts->forMonitor.queuedOnReal=NULL;
        return result;
    } else {
	return false;
    }
}

void fivmr_ThreadQueue_eueuqne(fivmr_ThreadQueue *queue,
                               fivmr_ThreadState *ts) {
    fivmr_assert(ts->forMonitor.queuedOnIntended==queue);
    ts->forMonitor.queuedOnIntended=NULL;
}

void fivmr_ThreadQueue_poke(fivmr_ThreadQueue *queue,
                            fivmr_ThreadState *ts) {
    bool result;
    if (FIVMR_ASSERTS_ON) {
        fivmr_ThreadQueue *qor=ts->forMonitor.queuedOnReal;
        fivmr_assert(qor==queue || qor==NULL);
    }
    fivmr_assert(ts->forMonitor.queuedOnIntended==queue);
    
    if (ts->forMonitor.queuedOnReal==queue) {
        result=removeFromQueue(queue,ts);
        fivmr_assert(result);
        enqueue(queue,ts);
        
        if (false && FIVMR_ASSERTS_ON) verifyQueue(queue);
    } /* else we *just* got dequeued */
    
    if (FIVMR_ASSERTS_ON) {
        fivmr_ThreadQueue *qor=ts->forMonitor.queuedOnReal;
        fivmr_assert(qor==queue || qor==NULL);
    }
    fivmr_assert(ts->forMonitor.queuedOnIntended==queue);
}

# 1 "fivmr_threadstate.c"
/*
 * fivmr_threadstate.c
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

#include "fivmr.h"

fivmr_ThreadState *fivmr_ThreadState_getNullable(fivmr_VM *vm) {
    return (fivmr_ThreadState*)fivmr_ThreadSpecific_get(&vm->curThread);
}

fivmr_ThreadState *fivmr_ThreadState_get(fivmr_VM *vm) {
    fivmr_ThreadState *ts=fivmr_ThreadState_getNullable(vm);
    /* LOG(9,("ts = %p",ts)); */
    fivmr_assert(ts!=NULL);
    fivmr_assert(ts->cookie==0xd1e7c0c0);
    fivmr_assert(fivmr_ThreadState_byId(vm,ts->id)==ts);
    return ts;
}

static void clear(fivmr_ThreadState *ts) {
#if FIVMR_PF_POLLCHECK
    int res;
#endif
    ts->vm->javaThreads[ts->id].vm=ts->vm;
    ts->vm->javaThreads[ts->id].obj=0;
    fivmr_assert(ts->vm->javaThreads[ts->id].prev==NULL);
    fivmr_assert(ts->vm->javaThreads[ts->id].next==NULL);
    fivmr_assert(ts->jumpOnExit==NULL);
    fivmr_assert(ts->thread==fivmr_ThreadHandle_zero());
    ts->index=(uint32_t)-1;
    ts->execFlags=0;
    ts->suspendCount=0;
    ts->suspendReqCount=0;
    ts->typeEpoch=0;
    ts->pollingUnion.s.pollchecksDisabled=0;
    ts->pollingUnion.s.checkBlockNotRequested=!0;
    ts->pollchecksTaken=0;
#if FIVMR_PF_POLLCHECK
    res=mprotect(((char*)ts)-FIVMR_PAGE_SIZE,FIVMR_PAGE_SIZE,PROT_READ|PROT_WRITE);
    fivmr_assert(res==0);
#endif
    ts->isDaemon=false;
    ts->curException=0;
    ts->interrupted=false;
    ts->curExceptionHandle=NULL;
    ts->handlingStackOverflow=false;
    ts->stackHeight=0;
    fivmr_assert(ts->toUnbias==0);
    fivmr_assert(ts->curNF==NULL);
    fivmr_assert(ts->curF==NULL);
    ts->stateBufGCMap=NULL;
    bzero(&ts->rootNF,sizeof(fivmr_NativeFrame));
    bzero(&ts->rootF,sizeof(fivmr_Frame));
    fivmr_assert(ts->freeHandles==NULL);
    fivmr_assert(ts->allocFrame==NULL);
    fivmr_GC_clear(ts);
    fivmr_SA_clear(ts);
    fivmr_assert(!ts->bufInUse);
    if (ts->buf!=NULL) {
	bzero(ts->buf,FIVMR_TS_BUF_SIZE);
    }
    ts->basePrio=FIVMR_TPR_MIN;
    ts->permBoostPrio=FIVMR_TPR_MIN;
    ts->curPrio=FIVMR_TPR_MIN;
    ts->curTempBoostPrio=FIVMR_TPR_MIN;
#if FIVMR_FLOW_LOGGING
    if (ts->flowbuf && ts->flowbuf->entries) {
        fivmr_FlowLog_release(ts->flowbuf);
    }
    ts->flowbuf = NULL;
#endif
    /* deliberately leave deqUnlockCount as is */
    fivmr_assert(!ts->forMonitor.syncHandoffCookie);
    fivmr_assert(ts->forMonitor.holding==NULL);
    fivmr_assert(ts->forMonitor.entering==NULL);
    fivmr_assert(ts->forMonitor.queuedOnReal==NULL);
    fivmr_assert(ts->forMonitor.queuedOnIntended==NULL);
    fivmr_assert(ts->forMonitor.next==NULL);
    fivmr_assert(!ts->performedGuaranteedCommit);
}

fivmr_ThreadState *fivmr_ThreadState_new(fivmr_VM *vm,
                                         uintptr_t initExecFlags) {
    uint32_t i;
    if (vm->exiting) {
        return NULL;
    }
    for (i=2;i<vm->config.maxThreads;++i) {
	fivmr_ThreadState *ts=fivmr_ThreadState_byId(vm,i);
        fivmr_Lock_lock(&vm->lock);
	if (ts->execStatus==FIVMR_TSES_CLEAR) {
            fivmr_assert_cas(&ts->execStatus,
                             FIVMR_TSES_CLEAR,
                             FIVMR_TSES_NEW);
	    clear(ts);
	    ts->execFlags|=initExecFlags;
	    ts->index=vm->numThreads++;
	    vm->threads[ts->index]=ts;
            if (vm->maxThreadID < ts->id) {
                vm->maxThreadID=ts->id;
            }
	    fivmr_Lock_unlock(&vm->lock);
	    LOG(1,("Creating new thread %u, at %p",ts->id,ts));
#if FIVMR_FLOW_LOGGING
            ts->flowbuf = fivmr_malloc(FIVMR_FLOWLOG_BUFFERSIZE);
            memset(ts->flowbuf, 0, FIVMR_FLOWLOG_BUFFERSIZE);
#endif
#if FIVMR_INTERNAL_INST
            fivmr_ii_startThread(ts);
#endif
	    return ts;
	}
        fivmr_Lock_unlock(&vm->lock);
    }
    return NULL;
}

void fivmr_ThreadState_setManual(fivmr_ThreadState *ts,
				 fivmr_ThreadHandle th,
                                 fivmr_TypeContext *ctx) {
    fivmr_VM *vm=ts->vm;
    fivmr_assert(fivmr_ThreadPriority_leRT(ts->curPrio,vm->maxPriority));
    fivmr_assert(fivmr_ThreadPriority_leRT(ts->curTempBoostPrio,vm->maxPriority));
    fivmr_assert(fivmr_ThreadPriority_leRT(ts->basePrio,vm->maxPriority));
    fivmr_assert(ts->vm->exitExits || fivmr_ThreadState_canExitGracefully(ts));
    fivmr_assert(fivmr_ThreadPriority_leRT(fivmr_Thread_getPriority(th),
                                           ts->vm->maxPriority));
    ts->thread=th;
    ts->curNF=NULL;
    ts->freeHandles=NULL;
    fivmr_ThreadState_pushAndInitNF2(ts,&ts->rootNF,NULL,ctx);
    ts->rootF.up=NULL;
    ts->rootF.id=(uintptr_t)(intptr_t)-1;
    ts->curF=&ts->rootF;
    fivmr_ThreadSpecific_setForThread(&vm->curThread,th,(uintptr_t)ts);
    fivmr_Lock_lock(&ts->lock);
    fivmr_ThreadState_moveToStatus(ts,FIVMR_TSES_IN_NATIVE);
    ts->version++;
    fivmr_Lock_unlock(&ts->lock);
    
    ts->primFields=vm->primFields;
    ts->refFields=vm->refFields;
    ts->typeList=vm->payload->typeList;
    ts->stubList=vm->payload->stubList;
    ts->patchRepo=vm->payload->patchRepo;
    
    fivmr_Lock_lock(&ts->lock);
    /* any soft-handshake-related thread initialization that needs to
       be performed before threads start executing Java code should
       be done here */
    ts->typeEpoch=vm->typeEpoch;
    fivmr_GC_startThread(ts);
    fivmr_SA_init(ts);
    fivmr_Lock_unlock(&ts->lock);
    
    fivmr_ThreadState_checkExit(ts);
    
    LOG(1,("Thread %u attached to native thread %p",ts->id,ts->thread));
}

void fivmr_ThreadState_go__INTERNAL(fivmr_ThreadState *ts) {
    ts->stackHigh=ts->stackStart;
    fivmr_assert(ts->stackHeight!=0);
    if (FIVMR_STACK_GROWS_DOWN) {
        ts->stackLimit=ts->stackStart-ts->stackHeight;
    } else {
        ts->stackLimit=ts->stackStart+ts->stackHeight;
    }
    LOG(1,("Thread %u running on stack %p",ts->id,ts->stackStart));
}

void fivmr_ThreadState_set__INTERNAL(fivmr_ThreadState *ts,
                                     fivmr_TypeContext *ctx) {
    fivmr_ThreadState_setManual(ts,fivmr_Thread_integrate(),ctx);
    fivmr_ThreadState_go__INTERNAL(ts);
}

bool fivmr_ThreadState_glue(fivmr_ThreadState *ts,
			    fivmr_Handle *javaThread) {
    fivmr_ThreadState *curTS;
    fivmr_VM *vm=ts->vm;
    bool isDaemon;
    
    curTS=fivmr_ThreadState_get(vm);
    
    fivmr_assert(javaThread!=NULL);
    
    /* NOTE: if this gets called with the state of the thread being NEW, then
       it means that this function is being called from a *different* thread
       state. */

    fivmr_Lock_lock(&ts->lock);
    if (ts->execStatus==FIVMR_TSES_NEW) {
	LOG(1,("moving to status STARTING"));
	fivmr_ThreadState_moveToStatus(ts,FIVMR_TSES_STARTING);
    }
    fivmr_Lock_unlock(&ts->lock);

    LOG(5,("setting thread state, execStatus = %" PRIuPTR,ts->execStatus));
    fivmr_VMThread_setThreadState(curTS,javaThread,ts);
    LOG(5,("thread state set"));
    fivmr_ThreadState_goToJava(curTS);
    fivmr_assert(fivmr_Handle_get(javaThread)!=0);
    fivmr_Handle_set(vm->javaThreads+ts->id,curTS,fivmr_Handle_get(javaThread));
    LOG(1,("Thread %u attached to Java thread %p",ts->id,javaThread->obj));
    fivmr_ThreadState_goToNative(curTS);

    isDaemon=fivmr_VMThread_isDaemon(curTS,vm->javaThreads+ts->id);
    fivmr_Lock_lock(&vm->deathLock);
    fivmr_Lock_lock(&vm->lock);
    if (ts->vm->exiting) {
        fivmr_Lock_unlock(&vm->lock);
        fivmr_Lock_unlock(&vm->deathLock);
        return false;
    }
    if (isDaemon) {
	ts->isDaemon=true;
	vm->numDaemons++;
    }
    vm->numActive++;
    vm->numRunning++; /* FIXME this is almost certainly wrong!  should do numRunning++
                         in glue */

    fivmr_Lock_unlock(&vm->lock);
    fivmr_Lock_unlock(&vm->deathLock);

    fivmr_VMThread_starting(curTS,vm->javaThreads+ts->id);
    fivmr_assertNoException(ts,"while attempting to set up a Java thread");
    
    return true;
}

void fivmr_ThreadState_commit(fivmr_ThreadState *ts) {
    /* for concurrent GC, we have to merge our queues with the
       collector at this point.  the collector should have one global
       queue onto which we can deposit references.
    
       that's what commitThread() is for. */
    fivmr_GC_commitThread(ts);

    fivmr_ThreadState_guaranteedCommit(ts);
}

void fivmr_ThreadState_guaranteedCommit(fivmr_ThreadState *ts) {
    fivmr_Lock_lock(&ts->lock);
    
#if FIVMR_INTERNAL_INST
    LOG(1,("Calling into fivmr_ii_commitThread()"));
    fivmr_ii_commitThread(ts);
#endif
    
    ts->performedGuaranteedCommit=true;
    fivmr_Lock_unlock(&ts->lock);
}

void fivmr_ThreadState_finalize(fivmr_ThreadState *ts) {
    fivmr_ThreadState_commit(ts);
    fivmr_SA_destroy(ts);

    while (fivmr_ThreadState_popNF(ts)) ;
    while (ts->freeHandles!=NULL) {
	fivmr_Handle *cur=ts->freeHandles;

	fivmr_Lock_lock(&ts->vm->hrLock);
	ts->freeHandles=cur->next;
	cur->next=ts->vm->freeHandles;
	ts->vm->freeHandles=cur;
	fivmr_Lock_unlock(&ts->vm->hrLock);
    }

#if FIVMR_FLOW_LOGGING
    fivmr_FlowLog_release(ts->flowbuf);
    ts->flowbuf = NULL;
#endif

    ts->execFlags|=FIVMR_TSEF_FINALIZED;
}

fivmr_ThreadPriority fivmr_ThreadState_terminate(fivmr_ThreadState *ts) {
    fivmr_VM *vm;
    fivmr_ThreadPriority result;
    bool signalDeath;
    bool signalExit;
    
    vm=ts->vm;
    
    LOG(1,("Thread %u terminating",ts->id));
    
    /* boost myself through termination to ensure that it happens quickly.
       FIXME not sure about this... */
    fivmr_Lock_lock(&ts->lock);
    result=ts->basePrio;
    /* we slam down all of the priorities because we know that we're setting
       the highest priority in the house. */
    ts->curTempBoostPrio=ts->curPrio=ts->permBoostPrio=
        fivmr_ThreadPriority_min(FIVMR_TPR_MAX,ts->vm->maxPriority);
    fivmr_Thread_setPriority(
        ts->thread,
        ts->curTempBoostPrio);
    fivmr_Lock_unlock(&ts->lock);

    fivmr_assert(ts->forMonitor.holding==NULL);
    ts->forMonitor.holding=NULL;
    
    fivmr_assertNoException(ts,"in call to fivmr_ThreadState_terminate()");
    fivmr_VMThread_die(ts,vm->javaThreads+ts->id);
    fivmr_assertNoException(ts,"while attempting to terminate a Java thread");
    
    fivmr_Lock_lock(&ts->lock);
    fivmr_ThreadState_finalize(ts);
    fivmr_ThreadState_moveToStatus(ts,FIVMR_TSES_TERMINATING);
    fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
    fivmr_ThreadState_acknowledgeSoftHandshakeRequest(ts);
    ts->jumpOnExit=NULL;
    ts->exitOnExit=false;
    ts->performedGuaranteedCommit=false;
    fivmr_Lock_broadcast(&ts->lock);
    fivmr_Lock_unlock(&ts->lock);

    fivmr_Lock_lock(&vm->lock);
    vm->threads[ts->index]=vm->threads[--vm->numThreads];
    vm->threads[ts->index]->index=ts->index;
    fivmr_Lock_unlock(&vm->lock);
    
    fivmr_assert(ts->curNF==NULL);
    
    fivmr_assert(ts->curF==&ts->rootF);
    ts->curF=ts->curF->up;
    fivmr_assert(ts->curF==NULL);
    
    fivmr_Lock_lock(&vm->lock);
    if (ts->isDaemon) {
	vm->numDaemons--;
    }
    vm->numActive--;
    signalDeath = (vm->numActive-vm->numDaemons==0);
    fivmr_Lock_unlock(&vm->lock);

    if (signalDeath) {
	fivmr_Lock_lockedBroadcast(&vm->deathLock);
    }

    if (ts->bufInUse) {
	LOG(1,("Thread %u leaking buffer %p",ts->id,ts->buf));
	ts->bufInUse=false;
	ts->buf=NULL;
    }
    
    ts->thread=fivmr_ThreadHandle_zero();
    clear(ts);

    fivmr_Lock_lock(&ts->lock);
    ts->execStatus=FIVMR_TSES_CLEAR;
    fivmr_Lock_unlock(&ts->lock);
    
    LOG(1,("Thread #%u completely done.",ts->id));
    
    fivmr_Lock_lock(&vm->deathLock);
    fivmr_Lock_lock(&vm->lock);
    vm->numRunning--;
    if (vm->numRunning==0) {
        signalExit=true;
    } else {
        signalExit=false;
    }

    fivmr_ThreadSpecific_set(&vm->curThread,0);

    fivmr_Lock_unlock(&vm->lock);
    if (signalExit) {
        fivmr_Lock_broadcast(&vm->deathLock);
    }
    fivmr_Lock_unlock(&vm->deathLock);

    return result;
}

void fivmr_ThreadState_lockWithHandshake(fivmr_ThreadState *ts,
                                         fivmr_Lock *lock) {
    fivmr_ThreadState_goToNative(ts);
    fivmr_Lock_lock(lock);
    fivmr_ThreadState_goToJava(ts);
}

void fivmr_ThreadState_waitSuspended(fivmr_ThreadState *ts) {
    for (;;) {
        fivmr_assert(ts->suspendReqCount>=0);
        fivmr_assert(ts->suspendCount>=0);
        
        if (ts->suspendReqCount>0) {
            ts->suspendCount+=ts->suspendReqCount;
            ts->suspendReqCount=0;
            fivmr_Lock_broadcast(&ts->lock);
        }
        
        if (ts->suspendCount==0) {
            break;
        }
        
        fivmr_Lock_unlock(&ts->lock);
        fivmr_Semaphore_down(&ts->waiter);
        fivmr_ThreadState_checkExit(ts);
        fivmr_Lock_lock(&ts->lock);
    }
}

void fivmr_ThreadState_moveToStatus(fivmr_ThreadState *ts,
				    uintptr_t status) {
    fivmr_Lock_lock(&ts->lock);
    fivmr_ThreadState_waitSuspended(ts);
    ts->execStatus=status;
    fivmr_Lock_unlock(&ts->lock);
}

bool fivmr_ThreadState_canExitGracefully(fivmr_ThreadState *ts) {
    return ts->jumpOnExit!=NULL || ts->exitOnExit;
}

bool fivmr_ThreadState_shouldExit(fivmr_ThreadState *ts) {
    if (fivmr_exiting(ts->vm)) {
        bool itsme;
        fivmr_Lock_lock(&ts->vm->lock);
        fivmr_assert(ts->vm->exiting);
        itsme = (ts->vm->exitInitiator==ts);
        fivmr_Lock_unlock(&ts->vm->lock);
        return !itsme;
    } else {
        return false;
    }
}

void fivmr_ThreadState_exitImpl(fivmr_ThreadState *ts) {
    fivmr_assert(fivmr_ThreadState_canExitGracefully(ts));
    bool signalExit=false;
    fivmr_JmpBuf *buf;
    if (ts->jumpOnExit!=NULL) {
        LOG(1,("VM exiting.  Jumping out of Java code in Thread %u...",ts->id));
        buf=ts->jumpOnExit;
        ts->jumpOnExit=NULL;
    } else {
        LOG(1,("VM exiting.  Terminating Thread %u...",ts->id));
        buf=NULL;
    }
#if FIVMR_FLOW_LOGGING
    /* FIXME: flush buffers */
    fivmr_free(ts->flowbuf);
#endif
    fivmr_ThreadState_finalize(ts);
    fivmr_Lock_lock(&ts->vm->deathLock);
    fivmr_Lock_lock(&ts->vm->lock);
    ts->vm->numRunning--;
    if (ts->vm->numRunning==0) {
        signalExit=true;
    }
    fivmr_ThreadSpecific_set(&ts->vm->curThread,0);
    fivmr_Lock_unlock(&ts->vm->lock);
    if (signalExit) {
        fivmr_Lock_broadcast(&ts->vm->deathLock);
    }
    fivmr_Lock_unlock(&ts->vm->deathLock);
    /* at this point none of the VM data structures are valid! */
    if (buf!=NULL) {
        fivmr_JmpBuf_jump(buf,1);
    } else {
        fivmr_Thread_exit();
    }
    fivmr_assert(false);
}

void fivmr_ThreadState_checkExit(fivmr_ThreadState *ts) {
    if (fivmr_ThreadState_shouldExit(ts)) {
        fivmr_ThreadState_exitImpl(ts);
    }
}

void fivmr_ThreadState_checkExitHoldingLock(fivmr_ThreadState *ts,
                                            int32_t times) {
    if (fivmr_ThreadState_shouldExit(ts)) {
        while (times-->0) {
            fivmr_Lock_unlock(&ts->lock);
        }
        fivmr_ThreadState_exitImpl(ts);
    }
}

void fivmr_ThreadState_checkExitHoldingLocks(fivmr_ThreadState *ts,
                                             int32_t n,
                                             fivmr_Lock **locks) {
    if (fivmr_ThreadState_shouldExit(ts)) {
        int32_t i;
        for (i=0;i<n;++i) {
            fivmr_Lock_unlock(locks[i]);
        }
        fivmr_ThreadState_exitImpl(ts);
    }
}

void fivmr_ThreadState_checkExitInHandshake(fivmr_ThreadState *ts) {
    if (fivmr_ThreadState_shouldExit(ts)) {
        if (ts==fivmr_ThreadState_getNullable(ts->vm)) {
            fivmr_Lock_unlock(&ts->lock);
            fivmr_ThreadState_exitImpl(ts);
        } else {
            /* make sure that the thread wakes up from whatever it may be
               blocked on! */
            fivmr_Semaphore_up(&ts->waiter);
            fivmr_Lock_broadcast(&ts->lock);
        }
    }
}

void fivmr_ThreadState_checkBlock(fivmr_ThreadState *ts) {
    const int loglevel=2;
    fivmr_Nanos before=0,after=0,before2,after2;
#if FIVMR_PF_POLLCHECK
    int res;
#endif
    uintptr_t execStatus;
    fivmr_ThreadState *ts2;
    
    ts->pollchecksTaken++;
    
    fivmr_assert(!fivmr_Thread_isCritical());
    
    LOG(3,("Thread %u in checkBlock",ts->id));

    before2=fivmr_curTimeLogging(loglevel);
    fivmr_Lock_lock(&ts->lock);
    after2=fivmr_curTimeLogging(loglevel);
    ts->pollingUnion.s.checkBlockNotRequested=!0;
#if FIVMR_PF_POLLCHECK
    res=mprotect(((char*)ts)-FIVMR_PAGE_SIZE,FIVMR_PAGE_SIZE,PROT_READ|PROT_WRITE);
    fivmr_assert(res==0);
#endif

    ts->execFlags|=FIVMR_TSEF_BLOCKING;
    ts->execStatus=FIVMR_TSES_IN_JAVA;
    
    /* FIXME: figure out a way of making this NOT require holding a lock,
       since it may be somewhat expensive-ish */
    fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
    fivmr_ThreadState_acknowledgeSoftHandshakeRequest(ts);
    fivmr_Lock_broadcast(&ts->lock);
    
    if (fivmr_ThreadState_shouldSuspend(ts)) {
        /* this entire suspension code path is currently dead code! */
        
        /* this needs to happen *after* servicing soft handshake requests, since the whole
           point is that we want soft handshakes to be priority boosted.  note that the
           fact that we're releasing the lock here means that a subsequent soft handshake
           request may happen.  but that's fine.  we service handshakes again after waiting
           for suspension. */
        ts->execStatus=FIVMR_TSES_IN_NATIVE;

        before=fivmr_curTimeLogging(loglevel);
        fivmr_Lock_unlock(&ts->lock);
        after=fivmr_curTimeLogging(loglevel);
        fivmr_ThreadState_evalPrio(ts);
        fivmr_Lock_lock(&ts->lock);

        fivmr_ThreadState_waitSuspended(ts); /* this may suspend the thread */
        
        ts->execStatus=FIVMR_TSES_IN_JAVA;
        fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
        fivmr_ThreadState_acknowledgeSoftHandshakeRequest(ts);
        fivmr_Lock_broadcast(&ts->lock);
    }
    
    /* don't move this.  see above. */
    ts2=fivmr_ThreadState_evalPrioImpl(ts);
    
    execStatus=ts->execStatus;
    fivmr_assert(execStatus==FIVMR_TSES_IN_JAVA ||
                 execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK);
    ts->execFlags&=~FIVMR_TSEF_BLOCKING;
    fivmr_Lock_broadcast(&ts->lock);
    fivmr_Lock_unlock(&ts->lock);
    
    if (ts2!=NULL) {
        fivmr_ThreadState_evalPrio(ts2);
    }
    
    LOG(loglevel,("checkBlock timings: %u, %u",
                  (uint32_t)(after-before),
                  (uint32_t)(after2-before2)));
    
    LOG(3,("Thread %u exiting checkBlock",ts->id));
}

void fivmr_ThreadState_goToNative_slow(fivmr_ThreadState *ts) {
    int oldErrno=errno;
    fivmr_ThreadState *ts2;
    
    ts->pollchecksTaken++;
    
    LOG(3,("Thread %u in goToNative_slow",ts->id));
    
    fivmr_assert(!fivmr_Thread_isCritical());

    fivmr_Lock_lock(&ts->lock);
    fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
    fivmr_ThreadState_acknowledgeSoftHandshakeRequest(ts);
    /* idea: change our state and send a broadcast; if anyone is waiting
       for us to acknowledge something, they'll instead observe that we've
       disappeared. */
    switch (ts->execStatus) {
    case FIVMR_TSES_IN_JAVA:
	ts->execStatus=FIVMR_TSES_IN_NATIVE;
	break;
    case FIVMR_TSES_IN_JAVA_TO_BLOCK:
	ts->execStatus=FIVMR_TSES_IN_NATIVE_TO_BLOCK;
	break;
    default: fivmr_assert(false);
    }
    fivmr_Lock_broadcast(&ts->lock);
    ts2=fivmr_ThreadState_evalPrioImpl(ts);
    fivmr_Lock_unlock(&ts->lock);
    if (ts2!=NULL) {
        fivmr_ThreadState_evalPrio(ts2);
    }
    errno=oldErrno;
}

void fivmr_ThreadState_goToJava_slow(fivmr_ThreadState *ts) {
    int oldErrno=errno;
    if (FIVMR_ASSERTS_ON) {
	uintptr_t execStatus=ts->execStatus;
	LOG(5,("execStatus = %" PRIuPTR,execStatus));
	fivmr_assert(execStatus==FIVMR_TSES_IN_NATIVE ||
		     execStatus==FIVMR_TSES_IN_NATIVE_TO_BLOCK);
    }
    fivmr_assert(!fivmr_Thread_isCritical());
    fivmr_ThreadState_checkBlock(ts);
    errno=oldErrno;
}

void fivmr_ThreadState_pollcheckSlow(fivmr_ThreadState *ts,
                                     uintptr_t debugID) {
    int oldErrno=errno;
    LOG(3,("Thread %u taking pollcheck at debugID = %p, ts->curF->id = %d",
           ts->id,debugID,ts->curF->id));
    fivmr_assert(!fivmr_Thread_isCritical());
    if (FIVMR_ASSERTS_ON) {
	uintptr_t execStatus=ts->execStatus;
	fivmr_assert(execStatus==FIVMR_TSES_IN_JAVA ||
		     execStatus==FIVMR_TSES_IN_JAVA_TO_BLOCK);
    }
    fivmr_ThreadState_setDebugID(ts,debugID);
    fivmr_ThreadState_checkBlock(ts);
    errno=oldErrno;
}

void fivmr_ThreadState_sleepAbsolute(fivmr_ThreadState *ts,
				     fivmr_Nanos whenAwake) {
    fivmr_Lock_lock(&ts->lock);
    while (whenAwake>fivmr_curTime() &&
	   !ts->interrupted) {
	fivmr_Lock_timedWaitAbs(&ts->lock,whenAwake);
        fivmr_ThreadState_checkExitHoldingLock(ts,1);
    }
    fivmr_Lock_unlock(&ts->lock);
}

fivmr_ThreadState *fivmr_ThreadState_evalPrioImpl(fivmr_ThreadState *ts) {
    fivmr_ThreadState *result=NULL;
    fivmr_ThreadPriority prio,lastPrio;
    fivmr_Monitor *cur;
    bool rtneq;
    fivmr_ThreadHandle th;
    
    LOG(3,("Evaluating priority of Thread #%u",ts->id));
    
    if (!fivmr_ThreadState_isRunning(ts)) {
        return NULL;
    }
    
    fivmr_assert(ts->thread!=fivmr_ThreadHandle_zero());

    prio=fivmr_ThreadPriority_max(ts->basePrio,
                                  ts->permBoostPrio);
    
    if (FIVMR_PIP_LOCKING(&ts->vm->settings)) {
        for (cur=ts->forMonitor.holding;cur!=NULL;cur=cur->next) {
            if (cur->queues!=NULL) {
                fivmr_ThreadQueue *queue;
                fivmr_ThreadState *ts2;
            
                LOG(3,("holding lock with queues"));
            
                fivmr_fence();
                queue=&cur->queues->entering;
            
                /* prevent the queue from changing until we're done with it */
                fivmr_BoostedSpinLock_lock(ts,&queue->lock);
                ts2=fivmr_ThreadQueue_peek(queue);
            
                if (ts2!=NULL) {
                    fivmr_ThreadPriority prio2;
                    LOG(3,("head of queue: %u",ts2->id));
                
                    prio2=ts2->curPrio;
                
                    if (fivmr_ThreadPriority_isRT(prio2)) {
                        prio=fivmr_ThreadPriority_max(prio,prio2);
                    }
                }
                fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
            }
            LOG(3,("done with lock %p",cur));
        }
        LOG(3,("done with locks"));
    }
    
    rtneq=!fivmr_ThreadPriority_eqRT(ts->curPrio,prio);
    
    lastPrio=ts->curPrio;
    ts->curTempBoostPrio=ts->curPrio=prio;
    
    if (rtneq || FIVMR_PIP_LOCKING(&ts->vm->settings)) {
        if (ts->forMonitor.queuedOnIntended!=NULL) {
            fivmr_ThreadQueue *queue=ts->forMonitor.queuedOnIntended;
            fivmr_BoostedSpinLock_lock(ts,&queue->lock);
            if (rtneq) {
                fivmr_ThreadQueue_poke(queue,ts);
            }
            if (ts->forMonitor.entering!=NULL) {
                if (rtneq) {
                    fivmr_Monitor_pokeRTQueued(ts->forMonitor.entering);
                }
                if (FIVMR_PIP_LOCKING(&ts->vm->settings)) {
                    result=fivmr_Monitor_curHolder(ts->vm,ts->forMonitor.entering);
                    LOG(3,("Triggering reevaluation of Thread #%u",result->id));
                }
            }
            fivmr_BoostedSpinLock_unlock(ts,&queue->lock);
        } else {
            fivmr_assert(ts->forMonitor.entering==NULL);
        }
    }
    
    if (LOGGING(1) && (ts->curPrio!=lastPrio)) {
        char buf[32],buf2[32],buf3[32];
        fivmr_ThreadPriority_describe(ts->curPrio,buf,sizeof(buf));
        fivmr_ThreadPriority_describe(lastPrio,buf2,sizeof(buf2));
        fivmr_ThreadPriority_describe(ts->basePrio,buf3,sizeof(buf3));
        LOG(1,("Changing priority of Thread %u: %s -> %s, base was %s",ts->id,buf2,buf,buf3));
    }
    
    th=ts->thread;
    fivmr_assert(th!=fivmr_ThreadHandle_zero());
    fivmr_assert(fivmr_ThreadPriority_leRT(ts->curPrio,ts->vm->maxPriority));
    fivmr_Thread_setPriority(th,ts->curPrio);
    
    if (LOGGING(2)) {
        char buf[32];
        fivmr_ThreadPriority_describe(ts->curPrio,buf,sizeof(buf));
        LOG(2,("Priority of Thread %u evaluated and set to %s",ts->id,buf));
    }
    
    return result;
}

fivmr_ThreadState *fivmr_ThreadState_evalPrioMiniImpl(fivmr_ThreadState *ts) {
    fivmr_ThreadState *result=NULL;
    
    if (FIVMR_PIP_LOCKING(&ts->vm->settings) && ts->forMonitor.entering!=NULL) {
        result=fivmr_Monitor_curHolder(ts->vm,ts->forMonitor.entering);
    }
    
    return result;
}

void fivmr_ThreadState_evalPrio(fivmr_ThreadState *startTS) {
    fivmr_ThreadState *curTS=startTS;

    /* variables used for detecting cycles */
    fivmr_ThreadState *curTSlag=startTS;
    bool throttle=false;
    
    do {
        fivmr_ThreadState *myTS;
        
        LOG(3,("In prio eval loop, %u, %u",curTS->id,curTSlag->id));
        
        myTS=curTS;
        curTS=NULL;
        
        fivmr_Lock_lock(&myTS->lock);
        if (fivmr_ThreadState_isRunning(myTS)) {
            if (myTS->thread==fivmr_ThreadHandle_current()) {
                curTS=fivmr_ThreadState_evalPrioImpl(myTS);
            } else {
                fivmr_ThreadState_triggerBlock(myTS);
                if (!fivmr_ThreadState_isInJava(myTS)) {
                    curTS=fivmr_ThreadState_evalPrioImpl(myTS);
                    LOG(4,("got curTS = %p",curTS));
                } /* else the thread in question just got boosted and triggered
                     to block, so it'll evaluate its priority for us at the
                     next safepoint. */
            }
            
            if (curTS!=NULL && throttle) {
                curTSlag=fivmr_ThreadState_evalPrioMiniImpl(curTSlag);
                LOG(4,("got curTSlag = %p",curTSlag));
                if (curTSlag==NULL) {
                    /* something got ef'd up */
                    curTSlag=myTS;
                    throttle=true;
                } else {
                    throttle=false;
                }
            } else {
                throttle=true;
            }
        }
        fivmr_Lock_unlock(&myTS->lock);
        
        if (curTS!=NULL) {
            LOG(4,("Footer of loop, %u, %u",curTS->id,curTSlag->id));
        }
    } while (curTS!=NULL && curTS!=curTSlag);
}

void fivmr_ThreadState_setBasePrio(fivmr_ThreadState *ts,
                                   fivmr_ThreadPriority pr) {
    fivmr_assert(fivmr_ThreadPriority_leRT(pr,ts->vm->maxPriority));
    fivmr_Lock_lock(&ts->lock);
    ts->basePrio=pr;
    fivmr_Lock_unlock(&ts->lock);
    fivmr_ThreadState_evalPrio(ts);
}

fivmr_Handle *fivmr_ThreadState_cloneHandle(fivmr_ThreadState *ts,
					    fivmr_Handle *h) {
    return fivmr_ThreadState_addHandle(ts,fivmr_Handle_get(h));
}

void fivmr_ThreadState_pushNF(fivmr_ThreadState *ts,
			      fivmr_NativeFrame *nf) {
    fivmr_assert((ts->execFlags&FIVMR_TSEF_FINALIZED)==0);
    nf->up=ts->curNF;
    ts->curNF=nf;
}

void fivmr_ThreadState_pushAndInitNF(fivmr_ThreadState *ts,
				     fivmr_NativeFrame *nf,
				     fivmr_MethodRec *mr) {
    int oldErrno=errno;
    fivmr_NativeFrame_init(nf,ts,mr,fivmr_TypeData_getContext(mr->owner));
    fivmr_ThreadState_pushNF(ts,nf);
    errno=oldErrno;
}

void fivmr_ThreadState_pushAndInitNF2(fivmr_ThreadState *ts,
                                      fivmr_NativeFrame *nf,
                                      fivmr_MethodRec *mr,
                                      fivmr_TypeContext *ctx) {
    int oldErrno=errno;
    fivmr_NativeFrame_init(nf,ts,mr,ctx);
    fivmr_ThreadState_pushNF(ts,nf);
    errno=oldErrno;
}

bool fivmr_ThreadState_popNF(fivmr_ThreadState *ts) {
    int oldErrno=errno;
    fivmr_NativeFrame *nf=ts->curNF;
    if (nf==NULL) {
        return false;
    } else {
        fivmr_NativeFrame_destroy(nf,ts);
        ts->curNF=nf->up;
        errno=oldErrno;
        return true;
    }
}

char *fivmr_ThreadState_tryGetBuffer(fivmr_ThreadState *ts,
                                     int32_t size) {
    if (size>FIVMR_TS_BUF_SIZE) {
	return NULL;
    } else {
	if (ts->bufInUse) {
	    return NULL;
	} else {
	    ts->bufInUse=true;
	    if (ts->buf==NULL) {
		ts->buf=fivmr_malloc(FIVMR_TS_BUF_SIZE);
		fivmr_assert(ts->buf!=NULL);
	    }
	    return (char*)ts->buf;
	}
    }
}

bool fivmr_ThreadState_tryReturnBuffer(fivmr_ThreadState *ts,
                                       char *ptr) {
    if (ptr==(char*)ts->buf) {
	fivmr_assert(ts->bufInUse);
	ts->bufInUse=false;
	return true;
    } else {
	return false;
    }
}

void fivmr_ThreadState_returnBuffer(fivmr_ThreadState *ts,
                                    char *ptr) {
    if (!fivmr_ThreadState_tryReturnBuffer(ts,ptr)) {
	free(ptr);
    }
}

bool fivmr_ThreadState_tryClaimBuffer(fivmr_ThreadState *ts,
                                      char *ptr) {
    if (ptr==(char*)ts->buf) {
	fivmr_assert(ts->bufInUse);
	ts->bufInUse=false;
	ts->buf=NULL;
	return true;
    } else {
	return false;
    }
}

void fivmr_ThreadState_handlifyException(fivmr_ThreadState *ts) {
    if (ts->curException) {
	ts->curExceptionHandle=
	    fivmr_ThreadState_addHandle(ts,ts->curException);
	ts->curException=0;
    }
}

const char *fivmr_ThreadState_describeStatusImpl(uintptr_t execStatus) {
    switch (execStatus) {
    case FIVMR_TSES_CLEAR: return "CLEAR";
    case FIVMR_TSES_NEW: return "NEW";
    case FIVMR_TSES_STARTING: return "STARTING";
    case FIVMR_TSES_IN_JAVA: return "IN_JAVA";
    case FIVMR_TSES_IN_JAVA_TO_BLOCK: return "IN_JAVA_TO_BLOCK";
    case FIVMR_TSES_IN_NATIVE: return "IN_NATIVE";
    case FIVMR_TSES_IN_NATIVE_TO_BLOCK: return "IN_NATIVE_TO_BLOCK";
    case FIVMR_TSES_TERMINATING: return "TERMINATING";
    default: fivmr_assert(!"bad execStatus value"); return NULL;
    }
}

const char *fivmr_ThreadState_describeFlagsImpl(uintptr_t execFlags) {
    switch (execFlags) {
    case 0: return "";
    case FIVMR_TSEF_BLOCKING: return "-BLOCKING";
    case FIVMR_TSEF_WAITING: return "-WAITING";
    case FIVMR_TSEF_BLOCKING|FIVMR_TSEF_WAITING: return "-BLOCKING-WAITING";
    case FIVMR_TSEF_TIMED: return "-TIMED";
    case FIVMR_TSEF_BLOCKING|FIVMR_TSEF_TIMED: return "-BLOCKING-TIMED";
    case FIVMR_TSEF_WAITING|FIVMR_TSEF_TIMED: return "-WAITING-TIMED";
    case FIVMR_TSEF_BLOCKING|FIVMR_TSEF_WAITING|FIVMR_TSEF_TIMED:
	return "-BLOCKING-WAITING-TIMED";
    default: fivmr_assert(!"bad execFlags value"); return NULL;
    }
}

const char *fivmr_ThreadState_describeStateImpl(uintptr_t execStatus,
						uintptr_t execFlags) {
    switch (execStatus) {
    case FIVMR_TSES_IN_JAVA:
    case FIVMR_TSES_IN_JAVA_TO_BLOCK:
    case FIVMR_TSES_IN_NATIVE:
    case FIVMR_TSES_IN_NATIVE_TO_BLOCK:
	if ((execFlags&FIVMR_TSEF_BLOCKING)) {
	    return "BLOCKED";
	} else if ((execFlags&FIVMR_TSEF_WAITING)) {
	    if ((execFlags&FIVMR_TSEF_TIMED)) {
		return "TIMED_WAITING";
	    } else {
		return "WAITING";
	    }
	} else {
	    return "RUNNABLE";
	}
    case FIVMR_TSES_CLEAR:
    case FIVMR_TSES_TERMINATING:
	return "TERMINATED";
    case FIVMR_TSES_NEW:
    case FIVMR_TSES_STARTING:
	return "NEW";
    default: fivmr_assert(!"bad execStatus value"); return NULL;
    }
}

void fivmr_ThreadState_setDebugID(fivmr_ThreadState *ts,
				  uintptr_t debugID) {
    if (debugID!=(uintptr_t)(intptr_t)-1) {
	ts->curF->id=debugID;
    }
}

void fivmr_ThreadState_dumpStackFor(fivmr_ThreadState *ts) {
    fivmr_Monitor *entering;
    
    fivmr_Log_lock();
    fivmr_Log_printf("fivmr stack dump for Thread %u:\n",ts->id);
    entering=ts->forMonitor.entering;
    if (entering!=NULL) {
        char buf[64];
        fivmr_MonState_describe(entering->state,
                                buf,sizeof(buf));
        fivmr_Log_printf("   (entering monitor %p, state %s)\n",
                         entering,
                         buf);
    }
    fivmr_dumpStackFromNoHeading(ts->vm,ts->curF);
    fivmr_Log_unlock();
}

void fivmr_ThreadState_checkHeightSlow(fivmr_ThreadState *ts,
				       uintptr_t newHeight) {
    uintptr_t heightVal;
    ts->stackHigh=newHeight;
#  if FIVMR_STACK_GROWS_DOWN
    heightVal=ts->stackStart-ts->stackHigh;
#  else
    heightVal=ts->stackHigh-ts->stackStart;
#  endif
    fivmr_Log_lock();
    fivmr_Log_printf("fivmr stack height profile: Thread %u: "
		     "new height is %" PRIuPTR
		     "; stack range = %" PRIuPTR
		     " to %" PRIuPTR
		     "; this function is at = %" PRIuPTR "\n",
		     ts->id,heightVal,
		     ts->stackStart,ts->stackHigh,
		     &heightVal);
    fivmr_Log_unlock();
}

uintptr_t fivmr_ThreadState_triggerBlock(fivmr_ThreadState *ts) {
#if FIVMR_PF_POLLCHECK
    int res;
#endif
    
    ts->pollingUnion.s.checkBlockNotRequested=!1;
#if FIVMR_PF_POLLCHECK
    res=mprotect(((char*)ts)-FIVMR_PAGE_SIZE,FIVMR_PAGE_SIZE,PROT_NONE);
    fivmr_assert(res==0);
#endif
    for (;;) {
	uintptr_t curStatus=ts->execStatus;
	uintptr_t newStatus;
	switch (curStatus) {
	case FIVMR_TSES_IN_JAVA: {
            fivmr_ThreadPriority myPrio;
            
            myPrio=fivmr_Thread_getPriority(fivmr_ThreadHandle_current());
            
            myPrio=fivmr_ThreadPriority_min(FIVMR_TPR_MAX,ts->vm->maxPriority);
            
            fivmr_assert(fivmr_ThreadPriority_geRT(ts->curTempBoostPrio,
                                                   ts->curPrio));
            
            ts->curTempBoostPrio=fivmr_ThreadPriority_max(ts->curTempBoostPrio,
                                                          myPrio);
            
            if (LOGGING(3)) {
                char buf1[32],buf2[32];
                fivmr_ThreadPriority_describe(myPrio,buf1,sizeof(buf1));
                fivmr_ThreadPriority_describe(ts->curTempBoostPrio,buf2,sizeof(buf2));
                LOG(3,("boosting priority of Thread #%u to %s due to triggering "
                       "thread having priority %s",
                       ts->id,buf2,buf1));
            }
            fivmr_Thread_setPriority(
                ts->thread,
                ts->curTempBoostPrio);
            LOG(2,("boosted priority of Thread #%u",ts->id));
    
	    newStatus=FIVMR_TSES_IN_JAVA_TO_BLOCK;
	    break;
        }
	case FIVMR_TSES_IN_NATIVE:
	    newStatus=FIVMR_TSES_IN_NATIVE_TO_BLOCK;
	    break;
	default:
	    newStatus=curStatus;
	    break;
	}
	if (fivmr_cas_weak(&ts->execStatus,curStatus,newStatus)) {
	    return newStatus;
	}
    }
}

void fivmr_ThreadState_softHandshakeImpl(fivmr_VM *vm,
                                         uintptr_t requiredExecFlags,
                                         uintptr_t execFlagsToSet,
                                         bool shouldWait,
                                         bool ignoreExit) {
    /* this code is tricky.  and it provides some tricky semantics. */

    unsigned i;
    unsigned numHandshakeThreads;
    
    fivmr_assert(!(shouldWait && ignoreExit));

    LOG(3,("Initiating soft handshake with requiredExecFlags = %" PRIuPTR
	   ", execFlagsToSet = %" PRIuPTR,
	   requiredExecFlags,execFlagsToSet));
    
    fivmr_Lock_lock(&vm->handshakeLock);
    
    if (vm->exiting && !ignoreExit) {
        fivmr_Lock_unlock(&vm->handshakeLock);
        return;
    }
    
    fivmr_assert(vm->softHandshakeCounter==0 || (vm->exiting && !shouldWait));
    
    numHandshakeThreads=0;
    
    /* FIXME: currently the thread table includes preallocated ThreadState
       structures.  but what if we had a table to which ThreadState structures
       could be added and removed?  any way to make this structure lock-free? */
    
    fivmr_Lock_lock(&vm->lock);
    LOG(3,("soft handshake: Checking %u threads...",vm->numThreads));
    for (i=0;i<vm->numThreads;++i) {
	fivmr_ThreadState *ts=vm->threads[i];
	if ((ts->execFlags&requiredExecFlags)==requiredExecFlags &&
	    fivmr_ThreadState_isRunning(ts)) {
            if (FIVMR_ASSERTS_ON && !(vm->exiting && !shouldWait)) {
                uintptr_t execFlags=ts->execFlags;
                if ((execFlags&FIVMR_TSEF_SOFT_HANDSHAKE)!=0) {
                    LOG(0,("Exec flags of Thread #%u is %p",ts->id,execFlags));
                    fivmr_assert((execFlags&FIVMR_TSEF_SOFT_HANDSHAKE)==0);
                }
            }
	    vm->handshakeThreads[numHandshakeThreads++]=ts;
	}
    }
    fivmr_Lock_unlock(&vm->lock);
    
    /* we have a snapshot of previously running threads.  we don't care about
       new threads or threads that are dying. */
    
     /* all previous soft handshake requests should have been accounted for. */
    fivmr_assert(vm->softHandshakeCounter==0 || (vm->exiting && !shouldWait));
    
    LOG(2,("soft handshake: Handshaking %u threads...",numHandshakeThreads));

    for (i=0;i<numHandshakeThreads;++i) {
	fivmr_ThreadState *ts;
	uintptr_t execStatus;
	
	ts=vm->handshakeThreads[i];
	vm->handshakeThreads[i]=NULL;
	
	fivmr_Lock_lock(&ts->lock);
	if (fivmr_ThreadState_isRunning(ts)) {
	    execStatus=fivmr_ThreadState_triggerBlock(ts);
            
	    switch (execStatus) {
	    case FIVMR_TSES_IN_JAVA_TO_BLOCK:
		LOG(2,("soft handshake: Thread %u is in Java; will wait for it.",
		       ts->id));
		ts->execFlags|=(execFlagsToSet|FIVMR_TSEF_SOFT_HANDSHAKE);
                if (shouldWait) {
                    fivmr_xchg_add32((int32_t*)&vm->softHandshakeCounter,1);
                }
		break;
            case FIVMR_TSES_STARTING:
	    case FIVMR_TSES_IN_NATIVE_TO_BLOCK:
		LOG(2,("soft handshake: Thread %u is in native or starting; hijacking it.",
		       ts->id));
		ts->execFlags|=execFlagsToSet;
		fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
		break;
	    default:
		/* nothing to do */
		LOG(2,("soft handshake: ignoring Thread %u in state %" PRIuPTR,
		       ts->id,execStatus));
		break;
	    }
	} else {
	    LOG(2,("soft handshale: Thread %u is not running, status = %" PRIuPTR,
		   ts->id,ts->execStatus));
	}
	fivmr_Lock_unlock(&ts->lock);
    }
    
    LOG(2,("soft handshake: still waiting for %u threads",
	   vm->softHandshakeCounter));
    
    if (shouldWait) {
        /* need a loop, because it's possible that there was a spurious notification */
        while (vm->softHandshakeCounter>0 && (!vm->exiting || ignoreExit)) {
            fivmr_Semaphore_down(&vm->softHandshakeNotifier);
        }
        
        fivmr_assert(vm->softHandshakeCounter==0 || vm->exiting);
    }
    
    fivmr_Lock_unlock(&vm->handshakeLock);

    LOG(3,("Soft handshake complete."));
}

void fivmr_ThreadState_softPairHandshake(fivmr_ThreadState *ts,
                                         uintptr_t execFlagsToSet) {
    LOG(2,("soft pair handshake: Handshaking Thread #%u...",ts->id));

    fivmr_Lock_lock(&ts->lock);
    if (!ts->vm->exiting && fivmr_ThreadState_isRunning(ts)) {
        uintptr_t execStatus=fivmr_ThreadState_triggerBlock(ts);
        switch (execStatus) {
        case FIVMR_TSES_IN_JAVA_TO_BLOCK:
            LOG(2,("soft pair handshake on Thread #%u: in Java, will wait for it.",
                   ts->id));
            ts->execFlags|=execFlagsToSet;
            while ((ts->execFlags&execFlagsToSet)!=0) {
                fivmr_Lock_wait(&ts->lock);
            }
            break;
        case FIVMR_TSES_STARTING:
        case FIVMR_TSES_IN_NATIVE_TO_BLOCK:
            LOG(2,("soft pair handshake on Thread #%u: in native or starting; hijacking it.",
                   ts->id));
            ts->execFlags|=execFlagsToSet;
            fivmr_ThreadState_serviceSoftHandshakeRequest(ts);
            break;
        default:
            LOG(2,("soft handshake on Thread %u: ignoring, in state %" PRIuPTR,
                   ts->id,execStatus));
            break;
        }
    }
    
    fivmr_Lock_unlock(&ts->lock);
}

/* this will be called from the following contexts:
   1) ts == current thread, status is TERMINATING
   2) ts == current thread, status is Java (pollcheck, native transitions)
   3) ts == soft handshake request thread, status is whatever it was when
            the soft handshake was initiated
*/
void fivmr_ThreadState_serviceSoftHandshakeRequest(fivmr_ThreadState *ts) {
    fivmr_ThreadState_checkExitInHandshake(ts);

    if ((ts->execFlags&FIVMR_TSEF_PUSH_TYPE_EPOCH)!=0) {
        ts->typeEpoch=ts->vm->typeEpoch;
        ts->execFlags&=~FIVMR_TSEF_PUSH_TYPE_EPOCH;
    }
    
    if ((ts->execFlags&FIVMR_TSEF_SF_GC_REQ_MASK)!=0) {
	fivmr_GC_handleHandshake(ts);
	fivmr_assert((ts->execFlags&FIVMR_TSEF_SF_GC_REQ_MASK)==0);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_TRACE_STACK)!=0) {
	fivmr_Debug_traceStack(ts);
	fivmr_assert((ts->execFlags&FIVMR_TSEF_TRACE_STACK)==0);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_DUMP_STACK)!=0) {
	fivmr_Debug_dumpStack(ts);
	fivmr_assert((ts->execFlags&FIVMR_TSEF_DUMP_STACK)==0);
    }
    
    if ((ts->execFlags&FIVMR_TSEF_UNBIAS)!=0) {
        fivmr_Monitor_unbiasFromHandshake(ts);
	fivmr_assert((ts->execFlags&FIVMR_TSEF_UNBIAS)==0);
    }
    
    fivmr_assert((ts->execFlags&FIVMR_TSEF_SF_REQ_MASK)==0); /* assert that we serviced
								all of them. */
}

void fivmr_ThreadState_acknowledgeSoftHandshakeRequest(fivmr_ThreadState *ts) {
    bool doit=false;
    doit=(ts->execFlags&FIVMR_TSEF_SOFT_HANDSHAKE)!=0;
    ts->execFlags&=~FIVMR_TSEF_SOFT_HANDSHAKE;
    fivmr_assert((ts->execFlags&FIVMR_TSEF_SF_REQ_MASK)==0); /* assert that we serviced
								all of them. */
    
    if (doit) {
	LOG(3,("Thread %u acknowledging soft handshake.",ts->id));
	if (fivmr_xchg_add32((int32_t*)&ts->vm->softHandshakeCounter,-1)==1) {
            fivmr_Semaphore_up(&ts->vm->softHandshakeNotifier);
	}
    }
}

uintptr_t fivmr_ThreadState_reqSuspend(fivmr_ThreadState *ts) {
    uintptr_t result;
    LOG(1,("Thread %u requesting suspension of Thread %u.",
	   fivmr_ThreadState_get(ts->vm)->id,ts->id));
    fivmr_Lock_lock(&ts->lock);
    if (fivmr_ThreadState_isRunning(ts)) {
        result=fivmr_ThreadState_triggerBlock(ts); /* this will do priority boosting */
    } else {
        result=ts->execStatus;
    }
    ts->suspendReqCount++;
    fivmr_Lock_unlock(&ts->lock);
    return result;
}

uintptr_t fivmr_ThreadState_waitForSuspend(fivmr_ThreadState *ts) {
    uintptr_t result;
    int64_t version;
    
    LOG(1,("Thread %u waiting for suspension of Thread %u.",
	   fivmr_ThreadState_get(ts->vm)->id,ts->id));

    /* FIXME: one way of dealing with the possibility of this being a new
       thread is to return something special if the suspendReqCount is
       zero.... */
    
    fivmr_Lock_lock(&ts->lock);
    
    fivmr_assert(ts->suspendReqCount>0 ||
                 ts->suspendCount>0);
    
    version=ts->version;
    while (fivmr_ThreadState_isInJava(ts) && ts->version==version) {
        fivmr_Lock_wait(&ts->lock);
    }
    if (ts->version==version) {
        result=ts->execStatus;
    } else {
        result=FIVMR_TSES_CLEAR;
    }
    fivmr_Lock_unlock(&ts->lock);
    
    LOG(1,("Thread %u done waiting for suspension of Thread %u, result = %"
	   PRIuPTR ".",
	   fivmr_ThreadState_get(ts->vm)->id,ts->id,result));

    return result;
}

uintptr_t fivmr_ThreadState_suspend(fivmr_ThreadState *ts) {
    uintptr_t result;
    fivmr_Lock_lock(&ts->lock);
    result=fivmr_ThreadState_reqSuspend(ts);
    if (fivmr_ThreadStatus_isRunning(result)) {
        result=fivmr_ThreadState_waitForSuspend(ts);
    } else {
        result=ts->execStatus;
    }
    fivmr_Lock_unlock(&ts->lock);
    return result;
}

void fivmr_ThreadState_resume(fivmr_ThreadState *ts) {
    LOG(1,("Thread %u resuming Thread %u.",
	   fivmr_ThreadState_get(ts->vm)->id,ts->id));

    fivmr_Lock_lock(&ts->lock);
    if (ts->suspendCount==0) {
        ts->suspendReqCount--;
    } else {
        ts->suspendCount--;
    }
    fivmr_assert(ts->suspendReqCount>=0);
    fivmr_assert(ts->suspendCount>=0);
    fivmr_Lock_unlock(&ts->lock);
    fivmr_Semaphore_up(&ts->waiter); /* wake the thread up */
}

/* WARNING: this code is not guaranteed to be race-free */
void fivmr_ThreadState_performAllGuaranteedCommits(fivmr_VM *vm) {
    unsigned i;
    for (i=2;i<vm->config.maxThreads;++i) {
        fivmr_ThreadState *ts=fivmr_ThreadState_byId(vm,i);
        fivmr_Lock_lock(&ts->lock);
        if (fivmr_ThreadState_isRunning(ts)) {
            fivmr_ThreadState_guaranteedCommit(ts);
        }
        fivmr_Lock_unlock(&ts->lock);
    }
}

void fivmr_ThreadState_setInterrupted(fivmr_ThreadState *ts,
                                      bool value) {
    fivmr_Lock_lock(&ts->lock);
    if (ts->interrupted!=value) {
        ts->interrupted=value;
        fivmr_Lock_broadcast(&ts->lock);
    }
    fivmr_Lock_unlock(&ts->lock);
}

/* boosted spin lock support - which is closely tied to ThreadState */

void fivmr_BoostedSpinLock_init(fivmr_BoostedSpinLock *bsl) {
    fivmr_SpinLock_init(&bsl->spinlock);
}

void fivmr_BoostedSpinLock_destroy(fivmr_BoostedSpinLock *bsl) {
    fivmr_SpinLock_destroy(&bsl->spinlock);
}

void fivmr_BoostedSpinLock_lock(fivmr_ThreadState *ts,
                                fivmr_BoostedSpinLock *bsl) {
    /* FIXME: this should first boost the thread's priority then acquire the lock,
       or something.  probably first acquiring the lock, then boosting priority
       is better. */
    for (;;) {
        fivmr_Lock_lock(&ts->lock);
        
        if (FIVMR_STRICT_BOOST) {
            fivmr_SpinLock_assertLock(&bsl->spinlock);
            break;
        }
        
        if (fivmr_SpinLock_tryLock(&bsl->spinlock)) {
            break;
        }
        
        fivmr_Lock_unlock(&ts->lock);
    }
}

void fivmr_BoostedSpinLock_unlock(fivmr_ThreadState *ts,
                                  fivmr_BoostedSpinLock *bsl) {
    fivmr_SpinLock_unlock(&bsl->spinlock);
    fivmr_Lock_unlock(&ts->lock);
}


# 1 "fivmr_timeslicing.c"
/*
 * fivmr_timeslicing.c
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

void fivmr_TimeSliceManager_init(fivmr_TimeSliceManager *man,
                                 uintptr_t nslices,
                                 fivmr_ThreadPriority managerPriority) {
    uintptr_t i;
    fivmr_assert(man!=NULL);
    fivmr_assert(nslices>0);
    man->managerPriority=managerPriority;
    man->nslices=nslices;
    man->slices=fivmr_mallocAssert(sizeof(fivmr_TimeSlice)*nslices);
    bzero(man->slices,sizeof(fivmr_TimeSlice)*nslices);
    for (i=0;i<nslices;++i) {
        man->slices[i].inited=false;
    }
}

fivmr_TimeSlice *fivmr_TimeSliceManager_initSlice(fivmr_TimeSliceManager *man,
                                                  uintptr_t sliceIndex,
                                                  fivmr_Nanos duration,
                                                  fivmr_ThreadPool *pool) {
    fivmr_TimeSlice *slice;

    fivmr_assert(pool!=NULL);
    fivmr_assert(duration>0);
    fivmr_assert(sliceIndex<man->nslices);
    fivmr_assert(fivmr_ThreadPriority_ltRT(pool->defaultPriority,man->managerPriority));
    
    slice=man->slices+sliceIndex;
    slice->duration=duration;
    slice->pool=pool;
    slice->inited=true;
    
    return slice;
}

fivmr_TimeSlice *fivmr_TimeSliceManager_initSliceEasy(fivmr_TimeSliceManager *man,
                                                      uintptr_t sliceIndex,
                                                      fivmr_Nanos duration,
                                                      uintptr_t nthreads,
                                                      fivmr_ThreadPriority defaultPriority) {
    fivmr_ThreadPool *pool;
    fivmr_assert(duration>0);
    fivmr_assert(fivmr_ThreadPriority_ltRT(defaultPriority,man->managerPriority));
    pool=fivmr_mallocAssert(sizeof(fivmr_ThreadPool));
    fivmr_ThreadPool_init(pool,nthreads,defaultPriority);
    return fivmr_TimeSliceManager_initSlice(man,sliceIndex,duration,pool);
}

bool fivmr_TimeSliceManager_fullyInitialized(fivmr_TimeSliceManager *man) {
    uintptr_t i;
    fivmr_assert(man->nslices>0);
    for (i=0;i<man->nslices;++i) {
        if (!man->slices[i].inited) {
            return false;
        }
        fivmr_assert(man->slices[i].duration>0);
        fivmr_assert(man->slices[i].pool!=NULL);
    }
    return true;
}

static void timeslicemanager_runner(void *arg) {
    /* FIXME: use priority modulation instead! */
    
    fivmr_TimeSliceManager *man;
    uintptr_t i,j;
#if FIVMR_RTEMS
    rtems_id ratemon;
    rtems_status_code stat;
#else
    fivmr_Nanos nextWakeup;
#endif
    fivmr_Lock waitLock;
    
    /* printk("in timeslicemanager_runner\n"); */
        
    man=(fivmr_TimeSliceManager*)arg;
    
#if FIVMR_RTEMS
    stat=rtems_rate_monotonic_create(42,&ratemon);
    fivmr_assert(stat==RTEMS_SUCCESSFUL);
#endif
    
    if (!FIVMR_RTEMS || man->nslices==1) {
        fivmr_Lock_init(&waitLock,FIVMR_PR_INHERIT);
        fivmr_Lock_lock(&waitLock);
    }
    
    if (man->nslices==1) {
        for (;;) fivmr_Lock_wait(&waitLock); /* just wait forever */
    } else {
        fivmr_assert(fivmr_TimeSliceManager_fullyInitialized(man));
        
        for (i=0;i<man->nslices;++i) {
            fivmr_ThreadPool_suspend(fivmr_TimeSliceManager_getPool(man,i));
        }

#if !FIVMR_RTEMS
        nextWakeup=fivmr_curTime()+man->slices[0].duration;
#endif
        for (i=0,j=0;;) {
#if FIVMR_RTEMS
            fivmr_Nanos duration_nanos;
            struct timespec duration_ts;
#endif
#if 0
            printk("resuming at %u\n",(unsigned)fivmr_curTimePrecise());
#endif
            fivmr_ThreadPool_resume(fivmr_TimeSliceManager_getPool(man,i));
#if 0
            printk("resuming done at %u\n",(unsigned)fivmr_curTimePrecise());
#endif
#if FIVMR_RTEMS
            duration_nanos=man->slices[(i+1)%man->nslices].duration;
            duration_ts.tv_sec=duration_nanos/(1000*1000*1000);
            duration_ts.tv_nsec=duration_nanos%(1000*1000*1000);
            stat=rtems_rate_monotonic_period(ratemon,_Timespec_To_ticks(&duration_ts));
#else
            fivmr_Lock_timedWaitAbs(&waitLock,nextWakeup);
            if (false) {
                fivmr_Nanos curTime;
                curTime=fivmr_curTime();
                if (curTime > nextWakeup) {
                    LOG(2,("Time slicer woke up late!  "
                           "Expected wakeup: %u.  Woke up at: %u.  Previous/current slice: %u/%u",
                           nextWakeup,curTime,
                           man->slices[i].duration,
                           man->slices[(i+1)%man->nslices].duration));
                }
            }
#endif
#if 0
            printk("suspending at %u\n",(unsigned)fivmr_curTimePrecise());
#endif
            fivmr_ThreadPool_suspend(fivmr_TimeSliceManager_getPool(man,i));
#if 0
            printk("suspending done at %u\n",(unsigned)fivmr_curTimePrecise());
#endif
            i=(i+1)%man->nslices;
#if !FIVMR_RTEMS
            nextWakeup+=man->slices[i].duration;
#endif
            
#if 0
            if (++j==1000) {
                printk("still slicing!\n");
                j=0;
            }
#endif
        }
    }
}

void fivmr_TimeSliceManager_start(fivmr_TimeSliceManager *man) {
    fivmr_ThreadHandle result;
    fivmr_assert(fivmr_TimeSliceManager_fullyInitialized(man));
    result=fivmr_Thread_spawn(timeslicemanager_runner,man,man->managerPriority);
    fivmr_assert(result!=fivmr_ThreadHandle_zero());
}

# 1 "fivmr_typeaux.c"
/*
 * fivmr_typeaux.c
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

#include "fivmr.h"

fivmr_TypeAux *fivmr_TypeAux_add(void *region,
                                 fivmr_TypeAux **list,
                                 uintptr_t size,
                                 fivmr_TypeAuxFlags flags) {
    fivmr_TypeAux *result=
        freg_cond_alloc(region,fivmr_alignRaw(sizeof(fivmr_TypeAux),8)+size);    
    fivmr_assert(result!=NULL);
    
    result->size=size;
    result->occupied=0;
    result->flags=flags;
    
    result->next=*list;
    *list=result;

    return result;
}

void fivmr_TypeAux_deleteAll(fivmr_TypeAux **list) {
    while (*list!=NULL) {
        fivmr_TypeAux *cur=*list;
        *list=cur->next;
        fivmr_free(cur);
    }
}

fivmr_TypeAux *fivmr_TypeAux_first(fivmr_TypeAux *list,
                                   fivmr_TypeAuxFlags mask,
                                   fivmr_TypeAuxFlags expected) {
    while (list!=NULL && (list->flags&mask)!=expected) {
        list=list->next;
    }
    return list;
}

fivmr_TypeAux *fivmr_TypeAux_next(fivmr_TypeAux *list,
                                  fivmr_TypeAuxFlags mask,
                                  fivmr_TypeAuxFlags expected) {
    return fivmr_TypeAux_first(list->next,mask,expected);
}

void *fivmr_TypeAux_addElement(void *region,
                               fivmr_TypeAux **list,
                               fivmr_TypeAuxFlags mask,
                               fivmr_TypeAuxFlags expected,
                               fivmr_TypeAuxFlags flags,
                               void *data,
                               size_t size) {
    fivmr_TypeAux *trlist;
    uintptr_t nextIdx;
    uintptr_t next;
    
    fivmr_assert(size<=8);
    
    trlist=fivmr_TypeAux_first(*list,mask,expected);
    if (trlist==NULL || fivmr_alignRaw(trlist->occupied,size)+size>trlist->size) {
        trlist=fivmr_TypeAux_add(region,list,16*sizeof(uintptr_t),flags);
    }
    
    nextIdx=fivmr_alignRaw(trlist->occupied,size);
    trlist->occupied=nextIdx+size;
    
    next=fivmr_TypeAux_data(trlist)+nextIdx;
    memcpy((void*)next,data,size);
    
    return (void*)next;
}

void *fivmr_TypeAux_addUntraced(void *region,
                                fivmr_TypeAux **list,
                                void *data,
                                size_t size) {
    return fivmr_TypeAux_addElement(region,list,
                                    FIVMR_TAF_TRACED,0,0,
                                    data,size);
}

void *fivmr_TypeAux_addUntracedZero(void *region,
                                    fivmr_TypeAux **list,
                                    size_t size) {
    int64_t zero=0;
    return fivmr_TypeAux_addUntraced(region,list,(void*)&zero,size);
}

fivmr_Object *fivmr_TypeAux_addPointer(void *region,
                                       fivmr_TypeAux **list,
                                       fivmr_Object obj) {
    return (fivmr_Object*)
        fivmr_TypeAux_addElement(region,list,
                                 FIVMR_TAF_TRACED,FIVMR_TAF_TRACED,FIVMR_TAF_TRACED,
                                 (void*)&obj,
                                 sizeof(fivmr_Object));
}



# 1 "fivmr_typedata.c"
/*
 * fivmr_typedata.c
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

#include "fivmr.h"

/* FIXME: for every type in the root context referenced in the app context, we
   should have a stub in the app context. */

fivmr_TypeData *fivmr_StaticTypeContext_find(fivmr_StaticTypeContext *ctx,
                                             const char *name) {
    int low=ctx->typeOffset;
    int high=low+ctx->nTypes;
    fivmr_TypeData **list=ctx->payload->typeList;
    while (high>low) {
	int mid;
	int cmpResult;
	fivmr_TypeData *cur;
	
	mid=(high-low)/2+low;
	cur=list[mid];
	cmpResult=strcmp(name,cur->name);
	if (cmpResult==0) {
	    return cur;
	} else if (cmpResult>0) {
	    low=mid+1;
	} else /* cmpResult<0 */ {
	    high=mid;
	}
    }
    return NULL;
}

fivmr_TypeStub *fivmr_StaticTypeContext_findStub(fivmr_StaticTypeContext *ctx,
                                                 const char *name) {
    int low=ctx->stubOffset;
    int high=low+ctx->nStubs;
    fivmr_TypeStub *list=ctx->payload->stubList;
    while (high>low) {
	int mid;
	int cmpResult;
	fivmr_TypeStub *cur;
	
	mid=(high-low)/2+low;
	cur=list+mid;
	cmpResult=strcmp(name,cur->name);
	if (cmpResult==0) {
	    return cur;
	} else if (cmpResult>0) {
	    low=mid+1;
	} else /* cmpResult<0 */ {
	    high=mid;
	}
    }
    return NULL;
}

void fivmr_TypeContext_boot(fivmr_VM *vm,
                            fivmr_TypeContext *ctx) {
    fivmr_Lock_init(&ctx->treeLock,
                    fivmr_Priority_bound(FIVMR_PR_INHERIT,
                                         vm->maxPriority));
    fivmr_Lock_init(&ctx->loadSerializerLock,
                    fivmr_Priority_bound(FIVMR_PR_INHERIT,
                                         vm->maxPriority));
    ftree_Tree_init(&ctx->dynamicTypeTree,
                    fivmr_TypeData_compareKey);
    ftree_Tree_init(&ctx->stubTree,
                    fivmr_TypeData_compareKey);
    
    fivmr_JNI_init(ctx,vm);
}

fivmr_TypeContext *fivmr_TypeContext_create(fivmr_VM *vm,
                                            fivmr_Handle *classLoader) {
    fivmr_TypeContext *result;
    fivmr_ThreadState *ts;
    fivmr_Object classLoaderObj;
    
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    
    result=freg_region_create(sizeof(fivmr_TypeContext));
    fivmr_assert(result!=NULL);
    
    bzero(result,sizeof(fivmr_TypeContext));

    result->st.typeOffset=0;
    result->st.nTypes=0;
    result->st.stubOffset=0;
    result->st.nStubs=0;
    result->st.payload=vm->payload;
    
    fivmr_TypeContext_boot(vm,result);
    
    result->vm=vm;
    result->aux=NULL;
    
    fivmr_Lock_lock(&vm->typeDataLock);
    
    if (vm->nDynContexts == vm->dynContextsSize) {
        fivmr_TypeContext **newDynContexts;
        int32_t newDynContextsSize;
        
        newDynContextsSize=(vm->dynContextsSize+1)<<1;
        
        newDynContexts=
            fivmr_reallocAssert(
                vm->dynContexts,
                sizeof(fivmr_TypeContext*)*newDynContextsSize);
        
        memcpy(newDynContexts,vm->dynContexts,
               sizeof(fivmr_TypeContext*)*vm->dynContextsSize);
        vm->dynContexts=newDynContexts;
        vm->dynContextsSize=newDynContextsSize;
    }
    
    vm->dynContexts[vm->nDynContexts++]=result;
    
    fivmr_Lock_unlock(&vm->typeDataLock);

    fivmr_ThreadState_goToJava(ts);
    
    classLoaderObj=fivmr_Handle_get(classLoader);
    fivmr_GC_mark(ts,classLoaderObj);
    result->classLoader=classLoaderObj;
    
    fivmr_ThreadState_goToNative(ts);

    return result;
}

void fivmr_TypeContext_destroy(fivmr_TypeContext *ctx) {
    /* FIXME: bunch of stuff to free that this doesn't include... */
    
    fivmr_Lock_destroy(&ctx->treeLock);
    fivmr_Lock_destroy(&ctx->loadSerializerLock); 
}

fivmr_TypeData *fivmr_TypeContext_findKnown(fivmr_TypeContext *ctx,
                                            const char *name) {
    fivmr_TypeData *result;
    ftree_Node *node;
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    
    LOG(2,("Looking for known type called %s",name));
    
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));

    result=fivmr_StaticTypeContext_find(&ctx->st,name);
    if (result!=NULL) {
        return result;
    }
    
    if (ctx->vm->baseContexts[0] != ctx) {
        result=fivmr_StaticTypeContext_find(&ctx->vm->baseContexts[0]->st,name);
        if (result!=NULL &&
            ((result->flags & FIVMR_TBF_OVERRIDE_ALL) ||
             (ctx->vm->baseContexts[1] == ctx &&
              (FIVMR_ROOT_OVERRIDES_ALL_APP(&vm->settings) ||
               (result->flags & FIVMR_TBF_OVERRIDE_APP))))) {
            LOG(2,("returning %p",result));
            return result;
        }
    }
    
    /* at this point if the type was primitive we would have found it.
       so assert that we're looking for a ref type. */
    
    fivmr_assert(name[0]=='L' || name[0]=='[');
    
    fivmr_ThreadState_goToNative(ts);
    fivmr_Lock_lock(&ctx->treeLock);
    fivmr_ThreadState_goToJava(ts);
    
    node=ftree_Tree_findFast(&ctx->dynamicTypeTree,
                             (uintptr_t)(void*)name,
                             fivmr_TypeData_compareKey);
    LOG(2,("Found node %p in dynamic type tree",node));
    if (node==NULL) {
        node=ftree_Tree_findFast(&ctx->stubTree,
                                 (uintptr_t)(void*)name,
                                 fivmr_TypeData_compareKey);
        LOG(2,("Found node %p in stub tree",node));
        if (node==NULL) {
            result=NULL;
        } else {
            fivmr_TypeStub *st=(fivmr_TypeStub*)(void*)node->value;
            LOG(2,("Found stub %s (%p) in stub tree",st->name,st));
            fivmr_assert(!strcmp(st->name,name));
            result=fivmr_TypeStub_tryGetTypeData(st);
        }
    } else {
        result=(fivmr_TypeData*)(void*)node->value;
    }
    
    fivmr_Lock_unlock(&ctx->treeLock);
    
    if (result==NULL && name[0]=='[') {
        /* try to find the base type */
        
        const char *className;
        int32_t depth;
        fivmr_TypeData *class;
        fivmr_TypeData *cur;
        
        className=name;
        depth=0;
        
        while (className[0]=='[') {
            className++;
            depth++;
        }
        
        fivmr_assert(className[0]!=0);
        fivmr_assert(className[0]!='[');
        
        class=fivmr_TypeContext_findKnown(ctx,className);
        
        if (class!=NULL) {
            cur=class;
            while (depth-->0 && cur!=NULL) {
                cur=cur->arrayType;
            }
            
            result=cur;
        }
    }
    
    LOG(2,("returning %p (2)",result));
    return result;
}

/* NOTE: never hold type context locks or type data locks when calling this */
fivmr_TypeData *fivmr_TypeContext_find(fivmr_TypeContext *ctx,
                                       const char *name) {
    fivmr_TypeData *result;
    fivmr_ThreadState *ts;
    
    ts=fivmr_ThreadState_get(ctx->vm);
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    /* this should not be called before the class loaders are initialized */
    if (FIVMR_ASSERTS_ON) {
        if (ctx->vm->baseContexts[0] == ctx) {
            fivmr_assert(ctx->classLoader==0);
        } else {
            fivmr_assert(ctx->classLoader!=0);
        }
    }
    
    result=fivmr_TypeContext_findKnown(ctx,name);
    if (result==NULL) {
        if (name[0]=='[') {
            /* if the name corresponds to an array type then we must create it
               in the context that its base element type belongs to */
            const char *className;
            int32_t depth;

            className=name;
            depth=0;

            while (className[0]=='[') {
                className++;
                depth++;
            }
            
            fivmr_assert(className[0]!=0);
            fivmr_assert(className[0]!='[');
            
            result=fivmr_TypeContext_find(ctx,className);
            
            if (result!=NULL) {
                fivmr_assert(ts->curException==0);
                while (depth-->0) {
                    result=fivmr_TypeData_makeArray(result);
                    if (result==NULL) {
                        fivmr_throwNoClassDefFoundError_inJava(ts,name,NULL);
                        break;
                    }
                }
            } else {
                fivmr_assert(ts->curException!=0);
            }
        } else if (name[0]=='L') {
            /* if the name corresponds to something else then we should ask
               Java to load it */
            fivmr_Object klass;

            /* FIXME: find a way to do this without holding any locks. */
            
            fivmr_ThreadState_goToNative(ts);
            fivmr_Lock_lock(&ctx->loadSerializerLock);
            fivmr_ThreadState_goToJava(ts);
            
            klass=fivmRuntime_loadClass(ts,ctx,ctx->classLoader,name);
            if (klass!=0) {
                fivmr_TypeStub *myStub;
                bool res;
                fivmr_assert(ts->curException==0);
                result=fivmr_TypeData_fromClass(ts,klass);
                
                myStub=fivmr_TypeContext_findStub(ctx,name);
                res=fivmr_TypeStub_union(myStub,(fivmr_TypeStub*)result);
                fivmr_assert(res);
            } else {
                fivmr_assert(ts->curException!=0);
                result=NULL;
            }
            
            fivmr_Lock_unlock(&ctx->loadSerializerLock);
        } else {
            /* the type is primitive - so we would have found it if it had existed */
            fivmr_assert(false);
        }
    }
    
    fivmr_assert(ts->curExceptionHandle==NULL);
    fivmr_assert((result==NULL)==(ts->curException!=0));
    
    return result;
}

fivmr_TypeStub *fivmr_TypeContext_findStub(fivmr_TypeContext *ctx,
                                           const char *name) {
    fivmr_TypeData *td;
    fivmr_TypeStub *ts;
    ftree_Node *node;
    
    fivmr_assert(fivmr_ThreadState_isInJava(fivmr_ThreadState_get(ctx->vm)));

    td=fivmr_TypeContext_findKnown(ctx,name);
    if (td!=NULL) {
        return (fivmr_TypeStub*)td;
    }
    
    ts=fivmr_StaticTypeContext_findStub(&ctx->st,name);
    if (ts!=NULL) {
        return ts;
    }
    
    fivmr_ThreadState_goToNative(fivmr_ThreadState_get(ctx->vm));
    fivmr_Lock_lock(&ctx->treeLock);
    fivmr_ThreadState_goToJava(fivmr_ThreadState_get(ctx->vm));
    
    node=ftree_Tree_findFast(&ctx->stubTree,
                             (uintptr_t)(void*)name,
                             fivmr_TypeData_compareKey);
    if (node==NULL) {
        ts=fivmr_mallocAssert(sizeof(fivmr_TypeStub));
        ts->state=FIVMR_MS_INVALID;
        ts->forward=NULL;
        ts->flags=FIVMR_TBF_STUB;
        ts->name=strdup(name);
        fivmr_assert(ts->name!=NULL);
        ts->context=&ctx->st;
        ts->inited=0;
        
        node=fivmr_mallocAssert(sizeof(ftree_Node));

        LOG(2,("Adding stub %s (%p) to tree for context %p; node at %p",
               name,ts,ctx,node));
        
        ftree_Node_init(node,
                        (uintptr_t)(void*)ts->name,
                        (uintptr_t)(void*)ts);
        ftree_Tree_add(&ctx->stubTree,node);
    } else {
        ts=(fivmr_TypeStub*)(void*)node->value;
        fivmr_assert(ts!=NULL);
    }
    
    fivmr_Lock_unlock(&ctx->treeLock);
    
    fivmr_assert(ts!=NULL);
    
    return ts;
}

static fivmr_TypeData *findClassImpl(fivmr_TypeContext *ctx,
                                     const char *className,
                                     char packageSeparator,
                                     fivmr_TypeData *(*finder)(fivmr_TypeContext *ctx,
                                                               const char *name)) {
    char *typeName;
    size_t classNameLen=strlen(className);
    size_t typeNameSize=classNameLen+3;
    size_t j;
    typeName=alloca(typeNameSize);
    snprintf(typeName,typeNameSize,"L%s;",className);
    if (packageSeparator!='/') {
	for (j=classNameLen;j-->0;) {
	    if (typeName[j+1]==packageSeparator) {
		typeName[j+1]='/';
	    }
	}
    }
    return finder(ctx,typeName);
}

fivmr_TypeData *fivmr_TypeContext_findClass(fivmr_TypeContext *ctx,
                                            const char *className,
                                            char packageSeparator) {
    return findClassImpl(ctx,className,packageSeparator,
                         fivmr_TypeContext_find);
}

fivmr_TypeData *fivmr_TypeContext_findClassKnown(fivmr_TypeContext *ctx,
                                                 const char *className,
                                                 char packageSeparator) {
    return findClassImpl(ctx,className,packageSeparator,
                         fivmr_TypeContext_findKnown);
}

fivmr_TypeStub *fivmr_TypeStub_find(fivmr_TypeStub *start) {
    fivmr_TypeStub *cur=start;
    fivmr_TypeStub *second=(fivmr_TypeStub*)cur->forward;
    fivmr_TypeStub *next=second;
    
    LOG(2,("Find from start = %s (%p, %d), second = %p",
           start->name,start,start->flags,second));
    
    for (;;) {
        if (next==cur || next==NULL) {
            break;
        }
        cur=next;
        next=(fivmr_TypeStub*)cur->forward;
    }
    
    fivmr_assert(cur!=NULL);
    
    LOG(2,("Found result = %s (%p, %d)",
           cur->name,cur,cur->flags));

    if (cur!=start && cur!=second) {
        fivmr_cas_weak((uintptr_t*)&start->forward,
                       (uintptr_t)second,
                       (uintptr_t)cur);
    }
    
    return cur;
}

fivmr_TypeStub *fivmr_TypeStub_find2(fivmr_TypeStub **startPtr) {
    fivmr_TypeStub *start=*startPtr;
    fivmr_TypeStub *result=fivmr_TypeStub_find(start);
    if (result!=start) {
        fivmr_cas_weak((uintptr_t*)startPtr,
                       (uintptr_t)start,
                       (uintptr_t)result);
    }
    return result;
}

fivmr_TypeData *fivmr_TypeStub_tryGetTypeData(fivmr_TypeStub *start) {
    start=fivmr_TypeStub_find(start);
    if ((start->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_STUB) {
        return NULL;
    } else {
        return (fivmr_TypeData*)start;
    }
}

fivmr_TypeData *fivmr_TypeStub_tryGetTypeData2(fivmr_TypeStub **startPtr) {
    fivmr_TypeStub *cur=fivmr_TypeStub_find2(startPtr);
    if ((cur->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_STUB) {
        return NULL;
    } else {
        return (fivmr_TypeData*)cur;
    }
}

/* FIXME: just use a global lock for unioning!  we'll need it once we implement
   the idea of pushing resolution points up from the children to the root. */
bool fivmr_TypeStub_union(fivmr_TypeStub *aStart,
                          fivmr_TypeStub *bStart) {
    LOG(2,("Attempting to union %s (%p, %d) with %s (%p, %d)",
           aStart->name,aStart,aStart->flags,bStart->name,bStart,bStart->flags));
    
    fivmr_assert(!strcmp(aStart->name,bStart->name));

    for (;;) {
        fivmr_TypeStub *a,*b;
        
        a=fivmr_TypeStub_find(aStart);
        b=fivmr_TypeStub_find(bStart);
        
        LOG(2,("Found %s (%p, %d) and %s (%p, %d)",
               a->name,a,a->flags,b->name,b,b->flags));
        
        fivmr_assert(!strcmp(a->name,b->name));

        if (a==b) {
            return true;
        } else if ((a->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB) {
            if ((b->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB) {
                return false; /* loading constraint cannot be satisfied */
            } else {
                if (fivmr_cas_weak((uintptr_t*)&b->forward,
                                   (uintptr_t)NULL,
                                   (uintptr_t)a)) {
                    return true;
                }
            }
        } else {
            if (fivmr_cas_weak((uintptr_t*)&a->forward,
                               (uintptr_t)NULL,
                               (uintptr_t)b)) {
                return true;
            }
        }
        
        fivmr_spin_fast();
    }
}

bool fivmr_TypeStub_unionParams(int32_t nparams1,fivmr_TypeStub **params1,
                                int32_t nparams2,fivmr_TypeStub **params2) {
    int32_t i;
    if (nparams1!=nparams2) {
        return false;
    }
    for (i=0;i<nparams1;++i) {
        if (!fivmr_TypeStub_union(params1[i],params2[i])) {
            return false;
        }
    }
    return true;
}

bool fivmr_TypeStub_eq(fivmr_TypeStub *aStart,
                       fivmr_TypeStub *bStart) {
    fivmr_TypeStub *aLast=NULL,*bLast=NULL;
    for (;;) {
        fivmr_TypeStub *a,*b;
        a=fivmr_TypeStub_find(aStart);
        b=fivmr_TypeStub_find(bStart);
        if (a==b) {
            return true;
        }
        if (a==aLast && b==bLast) {
            return false;
        }
        aLast=a;
        bLast=b;
        fivmr_fence();
    }
}

bool fivmr_TypeStub_eq2(fivmr_TypeStub *a,fivmr_TypeStub *b) {
    return !strcmp(a->name,b->name);
}

bool fivmr_TypeStub_paramsEq(int32_t nparams1,fivmr_TypeStub **params1,
                             int32_t nparams2,fivmr_TypeStub **params2) {
    int32_t i;
    if (nparams1!=nparams2) {
        return false;
    }
    for (i=0;i<nparams1;++i) {
        if (!fivmr_TypeStub_eq(params1[i],params2[i])) {
            return false;
        }
    }
    return true;
}

bool fivmr_TypeStub_paramsEq2(int32_t nparams1,fivmr_TypeStub **params1,
                              int32_t nparams2,fivmr_TypeStub **params2) {
    int32_t i;
    if (nparams1!=nparams2) {
        return false;
    }
    for (i=0;i<nparams1;++i) {
        if (!fivmr_TypeStub_eq2(params1[i],params2[i])) {
            return false;
        }
    }
    return true;
}

/* NOTE: never hold type context locks or type data locks when calling this */
fivmr_TypeData *fivmr_TypeStub_getTypeData(fivmr_TypeStub *start) {
    fivmr_TypeData *result;
    fivmr_ThreadState *ts;
    
    ts=fivmr_ThreadState_get(fivmr_TypeStub_getContext(start)->vm);
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    result=fivmr_TypeStub_tryGetTypeData(start);
    if (result==NULL) {
        result=fivmr_TypeContext_find(fivmr_TypeStub_getContext(start),
                                      start->name);
        if (result==NULL) {
            fivmr_assert(ts->curException!=0);
            return NULL;
        }
        fivmr_assert(ts->curException==0);
        
        LOG(2,("While looking for %s (%p) in %p, found %s (%p) in %p.",
               start->name,start,fivmr_TypeStub_getContext(start),
               result->name,result,fivmr_TypeData_getContext(result)));
        
        // make sure that this type stub is unioned with whatever we got,
        // and that start->forward points to the typedata (it's an
        // optimization that the JIT will assume we have done).
        if (start->forward!=result) {
            if (fivmr_TypeStub_union(start,(fivmr_TypeStub*)result)) {
                fivmr_TypeStub_find(start); /* make it so that state->forward
                                               points to result */
                fivmr_assert(start->forward==result);
            } else {
                LOG(1,("Union failed between %s (%p) in %p and %s (%p) in %p.",
                       start->name,start,fivmr_TypeStub_getContext(start),
                       result->name,result,fivmr_TypeData_getContext(result)));
                
                result=fivmr_TypeStub_tryGetTypeData(start);
                fivmr_assert(result!=NULL);
            }
        }
    }
    return result;
}

fivmr_TypeData *fivmr_TypeData_forHandle(fivmr_Handle *h) {
    return fivmr_TypeData_forObject(&h->vm->settings,fivmr_Handle_get(h));
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch(fivmr_TypeData *td,
                                                       const char *name,
                                                       const char *sig) {
    int i;
    for (i=0;i<td->numMethods;++i) {
        if (!(td->methods[i]->flags&FIVMR_BF_STATIC) &&
            !strcmp(td->methods[i]->name,name) &&
            fivmr_MethodRec_matchesSig(td->methods[i],sig)) {
            return td->methods[i];
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch2(fivmr_TypeData *td,
                                                        const char *name,
                                                        fivmr_TypeStub *result,
                                                        int32_t nparams,
                                                        fivmr_TypeStub **params) {
    int i;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            fivmr_MethodSig_eq2(mr,name,result,nparams,params)) {
            return mr;
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoSearch3(fivmr_TypeData *td,
                                                        const char *name,
                                                        fivmr_TypeStub *result,
                                                        int32_t nparams,
                                                        fivmr_TypeStub **params) {
    int i;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            fivmr_MethodSig_eq4(mr,name,result,nparams,params)) {
            return mr;
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface(fivmr_VM *vm,
                                                      fivmr_TypeData *td,
                                                      const char *name,
                                                      const char *sig) {
    for (;td!=vm->payload->td_top;td=td->parent) {
        fivmr_MethodRec *result=fivmr_TypeData_findInstMethodNoSearch(td,name,sig);
        if (result!=NULL) {
            return result;
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface2(fivmr_VM *vm,
                                                       fivmr_TypeData *td,
                                                       const char *name,
                                                       fivmr_TypeStub *result,
                                                       int32_t nparams,
                                                       fivmr_TypeStub **params) {
    for (;td!=vm->payload->td_top;td=td->parent) {
        fivmr_MethodRec *mr=
            fivmr_TypeData_findInstMethodNoSearch2(
                td,name,result,nparams,params);
        if (mr!=NULL) {
            return mr;
        }
    }
    return NULL;
}

fivmr_MethodRec *fivmr_TypeData_findInstMethodNoIface3(fivmr_VM *vm,
                                                       fivmr_TypeData *td,
                                                       const char *name,
                                                       fivmr_TypeStub *result,
                                                       int32_t nparams,
                                                       fivmr_TypeStub **params) {
    for (;td!=vm->payload->td_top;td=td->parent) {
        fivmr_MethodRec *mr=
            fivmr_TypeData_findInstMethodNoSearch3(
                td,name,result,nparams,params);
        if (mr!=NULL) {
            return mr;
        }
    }
    return NULL;
}

typedef struct {
    const char *name;
    const char *sig;
} FindInstMethodInIfaceData;

static uintptr_t findInstMethodInIface_cback(fivmr_TypeData *startTD,
                                             fivmr_TypeData *curTD,
                                             uintptr_t arg) {
    FindInstMethodInIfaceData *fimiidata=(FindInstMethodInIfaceData*)(void*)arg;
    return (uintptr_t)(void*)fivmr_TypeData_findInstMethodNoSearch(
        curTD,fimiidata->name,fimiidata->sig);
}

static fivmr_MethodRec *findInstMethodInIface(fivmr_VM *vm,
                                              fivmr_TypeData *td,
                                              const char *name,
                                              const char *sig) {
    FindInstMethodInIfaceData fimiidata;
    fimiidata.name=name;
    fimiidata.sig=sig;
    return (fivmr_MethodRec*)(void*)fivmr_TypeData_forAllAncestorsInclusive(
        td,findInstMethodInIface_cback,(uintptr_t)(void*)&fimiidata);
}

fivmr_MethodRec *fivmr_TypeData_findInstMethod(fivmr_VM *vm,
                                               fivmr_TypeData *td,
                                               const char *name,
                                               const char *sig) {
    fivmr_MethodRec *result=NULL;
    switch (td->flags&FIVMR_TBF_TYPE_KIND) {
    case FIVMR_TBF_INTERFACE:
    case FIVMR_TBF_ANNOTATION:
        result=findInstMethodInIface(vm,td,name,sig);
        if (result==NULL) {
            result=fivmr_TypeData_findInstMethodNoIface(vm,
                                                        vm->payload->td_Object,
                                                        name,sig);
        }
        break;
    case FIVMR_TBF_ARRAY:
        result=fivmr_TypeData_findInstMethodNoIface(vm,
                                                    vm->payload->td_Object,
                                                    name,sig);
        break;
    case FIVMR_TBF_ABSTRACT:
    case FIVMR_TBF_VIRTUAL:
    case FIVMR_TBF_FINAL:
        result=fivmr_TypeData_findInstMethodNoIface(vm,td,name,sig);
        if (result==NULL) {
            for (;td!=vm->payload->td_top;td=td->parent) {
                unsigned i;
                for (i=0;i<td->nSuperInterfaces;++i) {
                    result=findInstMethodInIface(vm,td->superInterfaces[i],name,sig);
                    if (result!=NULL) {
                        break;
                    }
                }
            }
        }
        break;
    default:
        break;
    }
    return result;
}

fivmr_MethodRec *fivmr_TypeData_findStaticMethod(fivmr_VM *vm,
                                                 fivmr_TypeData *td,
						 const char *name,
						 const char *sig) {
    fivmr_MethodRec *result=fivmr_TypeData_findMethod(vm,td,name,sig);
    if (result!=NULL && (result->flags&FIVMR_BF_STATIC)) {
	return result;
    } else {
	return NULL;
    }
}

fivmr_MethodRec *fivmr_TypeData_findMethod(fivmr_VM *vm,
                                           fivmr_TypeData *td,
					   const char *name,
					   const char *sig) {
    int i;
    for (i=0;i<td->numMethods;++i) {
	if (!strcmp(td->methods[i]->name,name) &&
	    fivmr_MethodRec_matchesSig(td->methods[i],sig)) {
	    return td->methods[i];
	}
    }
    return NULL;
}

fivmr_FieldRec *fivmr_TypeData_findField(fivmr_TypeData *td,
                                         const char *name,
                                         const char *sig) {
    int i;
    for (i=0;i<td->numFields;++i) {
        if (!strcmp(td->fields[i].name,name) &&
            !strcmp(td->fields[i].type->name,sig)) {
            fivmr_FieldRec *result=td->fields+i;
            return result;
        }
    }
    return NULL;
}

fivmr_FieldRec *fivmr_TypeData_findStaticField(fivmr_TypeData *td,
                                               const char *name,
                                               const char *sig) {
    fivmr_FieldRec *result=fivmr_TypeData_findField(td,name,sig);
    if (result==NULL || !(result->flags&FIVMR_BF_STATIC)) {
        return NULL;
    } else {
        return result;
    }
}

fivmr_FieldRec *fivmr_TypeData_findInstFieldNoSearch(fivmr_TypeData *td,
                                                     const char *name,
                                                     const char *sig) {
    fivmr_FieldRec *result=fivmr_TypeData_findField(td,name,sig);
    if (result==NULL || (result->flags&FIVMR_BF_STATIC)) {
        return NULL;
    } else {
        return result;
    }
}

fivmr_FieldRec *fivmr_TypeData_findInstField(fivmr_TypeData *td,
                                             const char *name,
                                             const char *sig) {
    fivmr_TypeData *top=fivmr_TypeData_getVM(td)->payload->td_top;
    for (;td!=top;td=td->parent) {
        fivmr_FieldRec *result=fivmr_TypeData_findInstFieldNoSearch(td,name,sig);
        if (result!=NULL) {
            return result;
        }
    }
    return NULL;
}

static bool checkInit_impl(fivmr_ThreadState *ts,
                           fivmr_TypeData *td) {
    /* FIXME: have an option to log initialization of classes, and make it possible
       to feed that into fivmc and have it generate a preinitialization code. */
    LOG(3,("checkInit for %p, %s",td,td->name));
    
    fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_DONE));
    fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    if (td->curIniter==ts) {
	/* we're currently initializing this type data, pretend as if it's already
	   initialized. */
	LOG(8,("%s currently being initialized.",td->name));
	return true;
    }
    for (;;) {
	if (td->inited==1) {
	    /* already initialized */
	    LOG(8,("%s already initialized.",td->name));
	    return true;
	} else if (td->inited==512) {
	    LOG(8,("%s already failed initialization.",td->name));
	    fivmr_throwNoClassDefFoundError_inJava(ts,td->name,"The class is present but its initialization has already failed with an ExceptionInInitializerError");
	    return false;
	} else if (fivmr_cas32_weak(&td->inited,
				    0,
				    256)) {
	    td->curIniter=ts;
	    fivmr_fence();
	    if (td->parent!=ts->vm->payload->td_top) {
		checkInit_impl(ts,td->parent);
	    }
	    if (!ts->curException) {
		fivmr_MethodRec *clinit;
		int32_t i;
		for (i=0;i<td->nSuperInterfaces;++i) {
		    if (!checkInit_impl(ts,td->superInterfaces[i])) {
			break;
		    }
		}
		if (!ts->curException) {
                    fivmr_MemoryArea *currentArea=NULL; /* make GCC happy */
                    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
                        currentArea=ts->gc.currentArea;
                        if(currentArea!=&ts->vm->gc.immortalMemoryArea) {
                            fivmr_MemoryArea_setCurrentArea(
                                ts, &ts->vm->gc.immortalMemoryArea);
                            fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                                              FIVMR_FLOWLOG_SUBTYPE_ENTER,
                                              (uintptr_t)&ts->vm->gc.immortalMemoryArea);
                        }
                    }
		    clinit=fivmr_TypeData_findStaticMethod(ts->vm,td,"<clinit>","()V");
		    if (clinit==NULL) {
			/* awesome, nothing to do. */
			LOG(8,("%s doesn't have a clinit method (success).",td->name));
			fivmr_fence();
			td->inited=1;
		    } else {
			LOG(3,("calling %s.",fivmr_MethodRec_describe(clinit)));
		
			fivmr_MethodRec_call(
			    clinit,ts,NULL,NULL,NULL,
			    FIVMR_CM_DISPATCH);
			if (ts->curException) {
			    fivmr_Object e=ts->curException;
			    ts->curException=0;
			    LOG(8,("%s threw an exception: %s.",
				   fivmr_MethodRec_describe(clinit),
				   fivmr_TypeData_forObject(&ts->vm->settings,e)->name));
			    fivmr_throwExceptionInInitializerError_inJava(ts,e,td);
			    fivmr_assert(ts->curException!=0);
			} else {
			    LOG(8,("%s returned.",fivmr_MethodRec_describe(clinit)));
			}
		    }
                    if (FIVMR_SCOPED_MEMORY(&ts->vm->settings)) {
                        if (currentArea!=ts->gc.currentArea) {
                            fivmr_MemoryArea_setCurrentArea(ts, currentArea);
                            fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_SCOPE,
                                              FIVMR_FLOWLOG_SUBTYPE_ENTER,
                                              (uintptr_t)currentArea);
                        }
                    }
		}
	    }
	    fivmr_fence();
	    if (ts->curException) {
		LOG(8,("%s failed to initialize, got exception along the way.",td->name));
		td->inited=512;
	    } else {
		LOG(8,("%s initialized.",td->name));
		td->inited=1;
	    }
	    td->curIniter=NULL;
            /* FIXME: should we perhaps use some other lock? */
            fivmr_Lock_lockedBroadcast(&ts->vm->thunkingLock);
	    return !ts->curException;
	} else {
            /* FIXME: need priority boosting... */
            fivmr_ThreadState_goToNative(ts);
            fivmr_Lock_lock(&ts->vm->thunkingLock);
            while (td->inited!=1 && td->inited!=512) {
                fivmr_Lock_wait(&ts->vm->thunkingLock);
            }
            fivmr_Lock_unlock(&ts->vm->thunkingLock);
            fivmr_ThreadState_goToJava(ts);
        }
    }
}

bool fivmr_TypeData_checkInit(fivmr_ThreadState *ts,
                              fivmr_TypeData *td) {
    bool result;
#if FIVMR_PROFILE_REFLECTION
    fivmr_Nanos before=fivmr_curTime();
#endif
    result=checkInit_impl(ts,td);
#if FIVMR_PROFILE_REFLECTION
    fivmr_PR_initTime+=fivmr_curTime()-before;
#endif
    return result;
}

fivmr_TypeData *fivmr_TypeStub_resolve(fivmr_ThreadState *ts,
                                       fivmr_TypeStub *st) {
    if ((st->flags&FIVMR_TBF_RESOLUTION_DONE)) {
        fivmr_TypeData *td=st->forward;
        fivmr_assert(td!=NULL);
        fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB);
        fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_DONE));
        fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
        return td;
    } else {
        fivmr_TypeData *td;
        td=fivmr_TypeStub_getTypeData(st);
        if (td==NULL) {
            fivmr_assert(ts->curException!=0);
            return NULL;
        }
        fivmr_assert(ts->curException==0);
        if (!fivmr_TypeData_resolve(td)) {
            fivmr_throwLinkageError_inJava(
                ts,
                fivmr_tsprintf("Could not link and resolve %s",td->name));
            return NULL;
        }
        
        fivmr_fence();

        fivmr_BitField_setAtomic(&st->flags,
                                 FIVMR_TBF_RESOLUTION_DONE,
                                 FIVMR_TBF_RESOLUTION_DONE);
        return td;
    }
}

fivmr_TypeData *fivmr_TypeStub_checkInit(fivmr_ThreadState *ts,
                                         fivmr_TypeStub *st) {
    if (st->inited==1) {
        fivmr_TypeData *td=st->forward;
        fivmr_assert(td!=NULL);
        fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB);
        fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_DONE));
        fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
        fivmr_assert(td->inited==1);
        return td;
    } else {
        fivmr_TypeData *td=fivmr_TypeStub_getTypeData(st);
        if (td==NULL) {
            fivmr_assert(ts->curException!=0);
            return NULL;
        }
        fivmr_assert(ts->curException==0);
        if (!fivmr_TypeData_resolve(td)) {
            fivmr_throwLinkageError_inJava(
                ts,
                fivmr_tsprintf("Could not link and resolve %s",td->name));
            return NULL;
        }
        if (!fivmr_TypeData_checkInit(ts,td)) {
            return NULL;
        }
        
        fivmr_fence();
        
        fivmr_BitField_setAtomic(&st->flags,
                                 FIVMR_TBF_RESOLUTION_DONE,
                                 FIVMR_TBF_RESOLUTION_DONE);
        st->inited=1;
        return td;
    }
}

void fivmr_TypeData_checkInitEasy(fivmr_ThreadState *ts,
                                  const char *name) {
    fivmr_TypeData *td;
    bool result;
    
    fivmr_ThreadState_goToJava(ts);

    td=fivmr_TypeContext_findClassKnown(ts->vm->baseContexts[0],name,'/');
    if (td==NULL) {
	fprintf(stderr,"Error: cannot find class %s\n",name);
	abort();
    }
    
    result=fivmr_TypeData_checkInit(ts,td);
    fivmr_assertNoException(ts,"while attempting to initialize type during VM startup");
    fivmr_assert(result);
    fivmr_ThreadState_goToNative(ts);
}

fivmr_TypeData *fivmr_TypeData_fromClass_inNative(fivmr_ThreadState *ts,
                                                  fivmr_Handle *h) {
    fivmr_TypeData *result;
    fivmr_ThreadState_goToJava(ts);
    result=fivmr_TypeData_fromClass(ts,fivmr_Handle_get(h));
    fivmr_ThreadState_goToNative(ts);
    return result;
}

int32_t fivmr_TypeData_arrayDepth(fivmr_TypeData *td) {
    int32_t result=0;
    while (td->arrayElement!=NULL) {
        td=td->arrayElement;
        result++;
    }
    return result;
}

fivmr_TypeData *fivmr_TypeData_openArray(fivmr_TypeData *td,
                                         int32_t depth) {
    while (depth-->0) {
        td=td->arrayElement;
    }
    return td;
}

fivmr_TypeData *fivmr_TypeData_closeArray(fivmr_TypeData *td,
                                          int32_t depth) {
    while (depth-->0) {
        fivmr_assert(td!=NULL);
        fivmr_assert(td!=td->arrayType);
        td=td->arrayType;
    }
    fivmr_assert(td!=NULL);
    return td;
}

static void prepareShadow(fivmr_VM *vm) {
    if ((uintptr_t)vm->nTypes*2 > vm->othShadow.n) {
        fivmr_OTH_free(&vm->othShadow);
        fivmr_OTH_init(&vm->othShadow,vm->nTypes*2);
    } else {
        fivmr_OTH_clear(&vm->othShadow);
    }
}

static void prepareDown(fivmr_VM *vm) {
    if ((uintptr_t)vm->nTypes*2 > vm->othDown.n) {
        fivmr_OTH_free(&vm->othDown);
        fivmr_OTH_init(&vm->othDown,vm->nTypes*2);
        if (vm->wlDown!=NULL) {
            fivmr_free(vm->wlDown);
        }
        vm->wlDown=fivmr_mallocAssert(vm->nTypes*sizeof(fivmr_TypeData*));
    } else {
        fivmr_OTH_clear(&vm->othDown);
    }
}

static void prepareSortList(fivmr_VM *vm) {
    if (vm->nTypes > vm->sortListSize) {
        if (vm->sortList!=NULL) {
            fivmr_free(vm->sortList);
        }
        vm->sortList=fivmr_mallocAssert(vm->nTypes*sizeof(fivmr_TypeData*));
        vm->sortListSize=vm->nTypes;
    }
}

uintptr_t fivmr_TypeData_forAllAncestors(fivmr_TypeData *td,
                                         uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                            fivmr_TypeData *curTD,
                                                            uintptr_t arg),
                                         uintptr_t arg) {
    int32_t depth;
    fivmr_TypeData *td_;
    fivmr_VM *vm;
    
    LOG(2,("finding ancestors of %s (%p)",td->name,td));
    
    vm=fivmr_TypeData_getVM(td);
    depth=fivmr_TypeData_arrayDepth(td);
    td_=fivmr_TypeData_openArray(td,depth);
    
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ARRAY) {
        int32_t i;
        fivmr_assert(td->ilistSize==0);
        for (i=0;i<depth;++i) {
            uintptr_t result=
                cback(td,fivmr_TypeData_closeArray(vm->payload->td_Serializable,i),arg);
            if (result!=0) {
                return result;
            }
            result=cback(td,fivmr_TypeData_closeArray(vm->payload->td_Cloneable,i),arg);
            if (result!=0) {
                return result;
            }
            result=cback(td,fivmr_TypeData_closeArray(vm->payload->td_Object,i),arg);
            if (result!=0) {
                return result;
            }
        }
    }
    
    LOG(2,("operating over %s (%p)",td_->name,td_));
    
    if (fivmr_TypeData_isBasetype(td_)) {
        /* FIXME: do anything?  probably not. */
    } else {
        if ((td_->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
            (td_->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION) {
            unsigned i;
            uintptr_t result;
            fivmr_assert((td_->ilistSize!=0)==(td_->nSuperInterfaces!=0));
            result=cback(td,fivmr_TypeData_closeArray(vm->payload->td_Object,depth),arg);
            if (result!=0) {
                return result;
            }
            for (i=0;i<td_->ilistSize;++i) {
                fivmr_assert(td_->ilist[i]!=vm->payload->td_Object);
                result=cback(td,fivmr_TypeData_closeArray(td_->ilist[i],depth),arg);
                if (result!=0) {
                    return result;
                }
            }
        } else {
            fivmr_TypeData *cur;
            for (cur=td_;cur!=vm->payload->td_top;cur=cur->parent) {
                if (cur!=td_) {
                    uintptr_t result=cback(td,fivmr_TypeData_closeArray(cur,depth),arg);
                    if (result!=0) {
                        return result;
                    }
                }
                unsigned i;
                for (i=0;i<cur->ilistSize;++i) {
                    uintptr_t result=cback(td,fivmr_TypeData_closeArray(cur->ilist[i],depth),arg);
                    if (result!=0) {
                        return result;
                    }
                }
            }
        }
    }
    
    return 0;
}

uintptr_t fivmr_TypeData_forAllAncestorsInclusive(fivmr_TypeData *td,
                                                  uintptr_t (*cback)(
                                                      fivmr_TypeData *startTD,
                                                      fivmr_TypeData *curTD,
                                                      uintptr_t arg),
                                                  uintptr_t arg) {
    uintptr_t result=cback(td,td,arg);
    if (result!=0) {
        return result;
    }
    return fivmr_TypeData_forAllAncestors(td,cback,arg);
}

typedef struct {
    fivmr_VM *vm;
    int32_t n;
} CollectForSortData;

static uintptr_t collectForSort_cback(fivmr_TypeData *startTD,
                                      fivmr_TypeData *curTD,
                                      uintptr_t arg) {
    CollectForSortData *data=(CollectForSortData*)(void*)arg;
    LOG(2,("Adding type %s (%p)",curTD->name,curTD));
    data->vm->sortList[data->n++]=curTD;
    return 0;
}

static int TypeData_compareDescendantsInverse(const void *a_,
                                              const void *b_) {
    fivmr_TypeData *a=*(fivmr_TypeData**)(void*)a_;
    fivmr_TypeData *b=*(fivmr_TypeData**)(void*)b_;
    if (a->numDescendants>b->numDescendants) {
        return -1;
    } else if (a->numDescendants==b->numDescendants) {
        return 0;
    } else {
        return 1;
    }
}

uintptr_t fivmr_TypeData_forAllAncestorsSorted(fivmr_TypeData *td,
                                               uintptr_t (*cback)(
                                                   fivmr_TypeData *startTD,
                                                   fivmr_TypeData *curTD,
                                                   uintptr_t arg),
                                               uintptr_t arg) {
    fivmr_VM *vm=fivmr_TypeData_getVM(td);
    CollectForSortData data;
    int32_t i;
    prepareSortList(vm);
    data.vm=vm;
    data.n=0;
    fivmr_TypeData_forAllAncestors(td,collectForSort_cback,(uintptr_t)(void*)&data);
    fivmr_sort(vm->sortList,data.n,sizeof(fivmr_TypeData*),
               TypeData_compareDescendantsInverse);
    for (i=0;i<data.n;++i) {
        uintptr_t result;
        result=cback(td,vm->sortList[i],arg);
        if (result!=0) {
            return result;
        }
    }
    return 0;
}

uintptr_t fivmr_TypeData_forAllAncestorsSortedInclusive(fivmr_TypeData *td,
                                                        uintptr_t (*cback)(
                                                            fivmr_TypeData *startTD,
                                                            fivmr_TypeData *curTD,
                                                            uintptr_t arg),
                                                        uintptr_t arg) {
    fivmr_VM *vm=fivmr_TypeData_getVM(td);
    CollectForSortData data;
    int32_t i;
    prepareSortList(vm);
    data.vm=vm;
    data.n=0;
    fivmr_TypeData_forAllAncestorsInclusive(td,collectForSort_cback,(uintptr_t)(void*)&data);
    fivmr_sort(vm->sortList,data.n,sizeof(fivmr_TypeData*),
               TypeData_compareDescendantsInverse);
    for (i=0;i<data.n;++i) {
        uintptr_t result;
        result=cback(td,vm->sortList[i],arg);
        if (result!=0) {
            return result;
        }
    }
    return 0;
}

static void pushSubs(fivmr_OTH *oth,
                     fivmr_TypeData **wl,
                     int32_t *n,
                     fivmr_TypeData *td) {
    unsigned i;
    for (i=0;i<td->nDirectSubs;++i) {
        if (fivmr_OTH_put(oth,td->directSubs[i],(void*)1)) {
            wl[(*n)++]=td->directSubs[i];
        }
    }
}

uintptr_t fivmr_TypeData_forAllDescendants(fivmr_TypeData *td,
                                           uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                              fivmr_TypeData *curTD,
                                                              uintptr_t arg),
                                           uintptr_t arg) {
    fivmr_VM *vm=fivmr_TypeData_getVM(td);
    fivmr_OTH *oth;
    fivmr_TypeData **wl;
    int32_t n;
    uintptr_t result;
    prepareDown(vm);
    oth=&vm->othDown;
    wl=vm->wlDown;
    n=0;
    pushSubs(oth,wl,&n,td);
    while (n>0) {
        fivmr_TypeData *cur;
        uintptr_t result;
        cur=wl[--n];
        result=cback(td,cur,arg);
        if (result!=0) {
            return result;
        }
        pushSubs(oth,wl,&n,cur);
    }
    return 0;
}

uintptr_t fivmr_TypeData_forAllDescendantsInclusive(fivmr_TypeData *td,
                                                    uintptr_t (*cback)(
                                                        fivmr_TypeData *startTD,
                                                        fivmr_TypeData *curTD,
                                                        uintptr_t arg),
                                                    uintptr_t arg) {
    uintptr_t result=cback(td,td,arg);
    if (result!=0) {
        return result;
    }
    return fivmr_TypeData_forAllDescendants(td,cback,arg);
}

typedef struct {
    fivmr_VM *vm;
    uintptr_t (*cback)(fivmr_TypeData *startTD,
                       fivmr_TypeData *curTD,
                       uintptr_t arg);
    fivmr_TypeData *startTD;
    uintptr_t arg;
} CbackData;

uintptr_t shadow_cbackUp(fivmr_TypeData *startTD,
                         fivmr_TypeData *curTD,
                         uintptr_t arg) {
    CbackData *data=(CbackData*)(void*)arg;
    if (fivmr_OTH_put(&data->vm->othShadow,curTD,(void*)1)) {
        data->vm->shadowResult=data->cback(data->startTD,curTD,data->arg);
        if (data->vm->shadowResult!=0) {
            return 1;
        } else {
            return 0;
        }
    } else {
        /* ef me ... what to do here?  this is going to turn into a performance
           pathology.  but I can't return 1. */
        return 0;
    }
}

uintptr_t shadow_cbackDown(fivmr_TypeData *startTD,
                           fivmr_TypeData *curTD,
                           uintptr_t arg) {
    if (curTD->nDirectSubs==0) {
        CbackData *data;
        uintptr_t result;
        bool res;
        data=(CbackData*)(void*)arg;
        res=fivmr_OTH_put(&data->vm->othShadow,curTD,(void*)1);
        fivmr_assert(res);
        data->vm->shadowResult=data->cback(data->startTD,curTD,data->arg);
        if (data->vm->shadowResult!=0) {
            return 1;
        }
        fivmr_TypeData_forAllAncestors(curTD,shadow_cbackUp,arg);
        if (data->vm->shadowResult!=0) {
            return 1;
        } else {
            return 0;
        }
    } else {
        return 0;
    }
}

uintptr_t fivmr_TypeData_forShadow(fivmr_TypeData *td,
                                   uintptr_t (*cback)(fivmr_TypeData *startTD,
                                                      fivmr_TypeData *curTD,
                                                      uintptr_t arg),
                                   uintptr_t arg) {
    CbackData data;
    data.vm=fivmr_TypeData_getVM(td);
    data.cback=cback;
    data.startTD=td;
    data.arg=arg;
    prepareShadow(fivmr_TypeData_getVM(td));
    fivmr_TypeData_forAllDescendantsInclusive(td,shadow_cbackDown,(uintptr_t)(void*)&data);
    return data.vm->shadowResult;
}

uintptr_t fivmr_TypeContext_forAllTypes(fivmr_TypeContext *ctx,
                                        uintptr_t (*cback)(fivmr_TypeData *curTD,
                                                           uintptr_t arg),
                                        uintptr_t arg) {
    int32_t i;
    uintptr_t result;
    ftree_Node *node;
    for (i=0;i<ctx->st.nTypes;++i) {
        result=cback(ctx->vm->payload->typeList[ctx->st.typeOffset+i],arg);
        if (result!=0) {
            return result;
        }
    }
    for (node=ftree_Tree_first(&ctx->dynamicTypeTree);
         node!=NULL;
         node=ftree_Tree_next(&ctx->dynamicTypeTree,node)) {
        fivmr_assert(node!=&ctx->dynamicTypeTree.nil);
        result=cback((fivmr_TypeData*)(void*)node->value,arg);
        if (result!=0) {
            return result;
        }
    }
    return 0;
}

uintptr_t fivmr_VM_forAllTypes(fivmr_VM *vm,
                               uintptr_t (*cback)(fivmr_TypeData *curTD,
                                                  uintptr_t arg),
                               uintptr_t arg) {
    int32_t i;
    uintptr_t result;
    for (i=0;i<vm->payload->nContexts;++i) {
        result=fivmr_TypeContext_forAllTypes(vm->baseContexts[i],cback,arg);
        if (result!=0) {
            return result;
        }
    }
    for (i=0;i<vm->nDynContexts;++i) {
        result=fivmr_TypeContext_forAllTypes(vm->dynContexts[i],cback,arg);
        if (result!=0) {
            return result;
        }
    }
    return 0;
}

static uintptr_t isSubtypeOf_cback(fivmr_TypeData *startTD,
                                   fivmr_TypeData *curTD,
                                   uintptr_t arg) {
    fivmr_TypeData *b=(fivmr_TypeData*)(void*)arg;
    if (curTD==b) {
        return 1;
    } else {
        return 0;
    }
}

bool fivmr_TypeData_isSubtypeOfSlow(fivmr_ThreadState *ts,
                                    fivmr_TypeData *a,
                                    fivmr_TypeData *b) {
    return (bool)fivmr_TypeData_forAllAncestorsInclusive(
        a,isSubtypeOf_cback,(uintptr_t)(void*)b);
}

/* How to do type display update:
   - Check if any type displays have conflicts
   - For (n-1) of the types that have conflicts, do the following:
     - For all types in the type's shadow
       - Shade any buckets used by a type in the shadow
     - Shade any buckets whose tids are full
     - Shade the bucket that the type currently has (redundant with the shadow search)
     - Pick a bucket that isn't shaded or create a new one
     - For all descendants of the type, fix their displays */

/* How to do itable update:
   - Check if any iface methods have conflicts
   - For (n-1) of the iface methods that have conflicts, do the following:
     - For all types in the iface method owner's shadow
       - If a type in the shadow is an interface
         - Shade all itable indices used by iface methods in the iface in the shadow
     - Shade the itable index that we conflicted on
     - Pick an itable index that isn't shaded or create a new one
     - For all descendants of the iface, fix their itables

   This algorithm may have to traverse the shadow too many times.  This
   algorithm may be better:
   - Allocate iface methods to indices one iface at a time
   - If we encounter an iface for which at least one method has a conflict,
     do the following:
     - For all types in the iface's shadow
       - If a type in the shadow is an interface and is not the iface we are implementing
         - Shade all itable indices used by iface methods in the iface in the shadow
     - Shade the itable indices on which we saw conflicts
     - Pick a new set of iface indices for all methods in the iface
     - For all descendants of the iface, fix their itables */

/* global type worklist helpers */
static void initWorklist(fivmr_VM *vm) {
    if ((uintptr_t)vm->nTypes*2 > vm->oth.n) {
        fivmr_OTH_free(&vm->oth);
        fivmr_OTH_init(&vm->oth,vm->nTypes*2);
        fivmr_free(vm->wl);
        vm->wl=fivmr_mallocAssert(sizeof(fivmr_TypeData*)*vm->nTypes);
    } else {
        fivmr_OTH_clear(&vm->oth);
    }
    vm->wlN=0;
}

static bool pushWorklist(fivmr_VM *vm,
                         fivmr_TypeData *td) {
    if (fivmr_OTH_put(&vm->oth,td,(void*)1)) {
        fivmr_assert(vm->wlN<vm->nTypes);
        vm->wl[vm->wlN++]=td;
        return true;
    } else {
        return false;
    }
}

static fivmr_TypeData *popWorklist(fivmr_VM *vm) {
    if (vm->wlN>0) {
        return vm->wl[--vm->wlN];
    } else {
        return NULL;
    }
}

/* call only while holding the tree lock and the type data lock */
static fivmr_TypeData *defineImpl(fivmr_TypeContext *ctx,
                                  fivmr_TypeData *td) {
    fivmr_TypeStub *stub;
    stub=fivmr_TypeContext_findStub(ctx,td->name);
    if (fivmr_TypeStub_union(stub,(fivmr_TypeStub*)td)) {
        fivmr_TypeDataNode *node=fivmr_mallocAssert(sizeof(fivmr_TypeDataNode));
        ftree_Node_init(&node->treeNode,
                        (uintptr_t)(void*)td->name,
                        (uintptr_t)(void*)td);
        node->next=td->node;
        td->node=node;
        
        ftree_Tree_add(&ctx->dynamicTypeTree,&node->treeNode);
        
        LOG(1,("Successfully defined new type %s (%p) in context %p",
               td->name,td,ctx));
        
        ctx->vm->nTypes++;
        
        return td;
    } else {
        fivmr_TypeData *result=fivmr_TypeStub_tryGetTypeData(stub);

        LOG(2,("Failed to define new type %s (%p) in context %p; already have %p",
               td->name,td,ctx,result));
        
        fivmr_assert(result!=NULL);
        return result;
    }
}

fivmr_TypeData *fivmr_TypeData_define(fivmr_TypeContext *ctx,
                                      fivmr_TypeData *td) {
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    fivmr_TypeData baseTD;
    fivmr_TypeDataNode *node;
    fivmr_TypeData *result;
    
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm); 
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    fivmr_assert(fivmr_Settings_canDoClassLoading(&vm->settings));
    
    fivmr_ThreadState_goToNative(ts);
    
    fivmr_Lock_lock(&ctx->treeLock);
    fivmr_Lock_lock(&vm->typeDataLock);
    
    fivmr_ThreadState_goToJava(ts);
    
    result=defineImpl(ctx,td);
    
    fivmr_Lock_unlock(&vm->typeDataLock);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    return result;
}

static void addSubtype(fivmr_TypeData *parent,fivmr_TypeData *child) {
    fivmr_TypeData **newDirectSubs;
    if ((parent->flags&FIVMR_TBF_DIRECT_SUBS_MALLOCED)) {
        newDirectSubs=
            fivmr_reallocAssert(parent->directSubs,
                                sizeof(fivmr_TypeData*)*(parent->nDirectSubs+1));
    } else {
        newDirectSubs=fivmr_mallocAssert(sizeof(fivmr_TypeData*)*(parent->nDirectSubs+1));
        memcpy(newDirectSubs,parent->directSubs,
               sizeof(fivmr_TypeData*)*parent->nDirectSubs);
    }
    newDirectSubs[parent->nDirectSubs]=child;
    parent->nDirectSubs++;
    parent->directSubs=newDirectSubs;
    fivmr_BitField_setAtomic(&parent->flags,
                             FIVMR_TBF_DIRECT_SUBS_MALLOCED,
                             FIVMR_TBF_DIRECT_SUBS_MALLOCED);
}

static uintptr_t incrementDescendants_cback(fivmr_TypeData *startTD,
                                            fivmr_TypeData *curTD,
                                            uintptr_t arg) {
    curTD->numDescendants++;
    return 0;
}

typedef struct {
    uint32_t *usedBuckets;
    int32_t typeEpoch;
} FindBucketsData;

static uintptr_t findBuckets_cback(fivmr_TypeData *startTD,
                                   fivmr_TypeData *curTD,
                                   uintptr_t arg) {
    FindBucketsData *fbdata=(FindBucketsData*)(void*)arg;
    fivmr_assert(curTD->epochs[fbdata->typeEpoch].bucket
                 < fivmr_TypeData_getVM(curTD)->numBuckets);
    LOG(2,("findBuckets dealing with %s (%p)",curTD->name,curTD));
    fivmr_BitVec_set(fbdata->usedBuckets,
                     curTD->epochs[fbdata->typeEpoch].bucket,
                     true);
    return 0;
}

static uintptr_t addOneBucket_cback(fivmr_TypeData *td,
                                    uintptr_t arg) {
    if ((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_PRIMITIVE &&
        (td->flags&FIVMR_TBF_RESOLUTION_DONE)) {
        fivmr_VM *vm;
        int8_t *newBuckets;
        
        vm=(fivmr_VM*)(void*)arg;
        if (fivmr_TypeData_bucketsMalloced(td,vm->typeEpoch^1)) {
            newBuckets=fivmr_reallocAssert(td->epochs[vm->typeEpoch^1].buckets,
                                           sizeof(int8_t)*vm->numBuckets);
        } else {
            newBuckets=fivmr_mallocAssert(sizeof(int8_t)*vm->numBuckets);
            memcpy(newBuckets,
                   td->epochs[vm->typeEpoch^1].buckets,
                   sizeof(int8_t)*(vm->numBuckets-1));
        }
        newBuckets[vm->numBuckets-1]=0;
        td->epochs[vm->typeEpoch^1].buckets=newBuckets;
        fivmr_TypeData_setBucketsMalloced(td,vm->typeEpoch^1,true);
        
        pushWorklist(vm,td);
    }
    return 0;
}

static void addOneBucket(fivmr_VM *vm) {
    uint32_t *newUsedTids;
    
    /* resize tids book-keeping */
    if ((vm->flags&FIVMR_VMF_USED_TIDS_MALLOCED)) {
        newUsedTids=fivmr_reallocAssert(vm->usedTids,
                                        sizeof(uint32_t)*((vm->numBuckets+1)*256/32));
    } else {
        newUsedTids=fivmr_mallocAssert(sizeof(uint32_t)*((vm->numBuckets+1)*256/32));
        memcpy(newUsedTids,
               vm->usedTids,
               sizeof(uint32_t)*(vm->numBuckets*256/32));
    }
    LOG(1,("vm->usedTids = %p, newUsedTids = %p, numBuckets = %p",
           vm->usedTids,newUsedTids,vm->numBuckets));
    bzero(newUsedTids+vm->numBuckets*256/32,
          sizeof(uint32_t)*256/32);
    vm->usedTids=newUsedTids;
    vm->numBuckets++;
    fivmr_BitField_setAtomic(&vm->flags,
                             FIVMR_VMF_USED_TIDS_MALLOCED,
                             FIVMR_VMF_USED_TIDS_MALLOCED);
    
    /* for each type, reallocate the buckets in the new epoch */
    fivmr_VM_forAllTypes(vm,addOneBucket_cback,(uintptr_t)(void*)vm);
}

static void findBucketAndTid(fivmr_VM *vm,
                             uint32_t **usedBuckets,
                             int32_t *foundBucket,
                             int32_t *foundTid) {
    uintptr_t foundBucketOcc=255;
    int32_t i,j;
    *foundBucket=-1;
    *foundTid=-1;
    for (i=vm->numBuckets;i-->0;) {
        if (!fivmr_BitVec_get(*usedBuckets,i)) {
            int32_t tid=-1;
            uintptr_t bucketOcc=0;
            for (j=1;j<256;++j) {
                if (fivmr_BitVec_get(vm->usedTids,i*256+j)) {
                    bucketOcc++;
                } else {
                    tid=j;
                }
            }
            if (bucketOcc<255) {
                fivmr_assert(tid>=1 && tid<=255);
                if (bucketOcc<foundBucketOcc) {
                    foundBucketOcc=bucketOcc;
                    *foundBucket=i;
                    *foundTid=tid;
                }
            } else {
                fivmr_assert(tid==-1);
            }
        }
    }
    
    if (*foundBucket<0) {
        fivmr_assert(*foundBucket==-1);
        fivmr_assert(*foundTid==-1);
        
        addOneBucket(vm);
        *usedBuckets=fivmr_reallocAssert(*usedBuckets,
                                         sizeof(uint32_t)*((vm->numBuckets+31)/32));
        
        *foundBucket=vm->numBuckets-1;
        *foundTid=1;
    
        LOG(1,("setting usedTids: %d, %d",vm->numBuckets-1,1));
        fivmr_BitVec_set(vm->usedTids,(vm->numBuckets-1)*256+1,true);
    } else {
        fivmr_assert(*foundBucket>=0);
        fivmr_assert(*foundTid>=0);
    
        LOG(1,("setting usedTids: %d, %d",*foundBucket,*foundTid));
        fivmr_BitVec_set(vm->usedTids,(*foundBucket)*256+*foundTid,true);
    }
}

static uintptr_t populateBuckets_cback(fivmr_TypeData *startTD,
                                       fivmr_TypeData *curTD,
                                       uintptr_t arg) {
    fivmr_VM *vm;
    int32_t epoch;
    int32_t i;
    
    vm=(fivmr_VM*)(void*)arg;
    epoch=vm->typeEpoch^1;
    
    LOG(1,("Populating bucket %d with %d for %s (%p)",
           curTD->epochs[epoch].bucket,
           curTD->epochs[epoch].tid&0xff,
           curTD->name,curTD));
    
    if (startTD->epochs[epoch].buckets[curTD->epochs[epoch].bucket]==0) {
        startTD->epochs[epoch].buckets[curTD->epochs[epoch].bucket]=
            curTD->epochs[epoch].tid;
        return 0;
    } else {
        /* collision on curTD */
        return (uintptr_t)(void*)curTD;
    }
}

typedef struct {
    fivmr_TypeData *newTD;
    int32_t oldBucket;
    int32_t oldTid;
    int32_t newBucket;
    int32_t newTid;
    int32_t typeEpoch;
} UpdateBucketsData;

static uintptr_t updateBuckets_cback(fivmr_TypeData *startTD,
                                     fivmr_TypeData *curTD,
                                     uintptr_t arg) {
    UpdateBucketsData *ubdata=(UpdateBucketsData*)(void*)arg;
    if (curTD!=ubdata->newTD) {
        fivmr_TypeEpoch *te=curTD->epochs+ubdata->typeEpoch;
        fivmr_assert((((int32_t)te->buckets[ubdata->oldBucket])&0xff)==ubdata->oldTid);
        fivmr_assert(te->buckets[ubdata->newBucket]==0);
        if (!fivmr_TypeData_bucketsMalloced(curTD,ubdata->typeEpoch)) {
            fivmr_VM *vm;
            int8_t *newBuckets;
            vm=fivmr_TypeData_getVM(curTD);
            newBuckets=fivmr_mallocAssert(sizeof(int8_t)*vm->numBuckets);
            memcpy(newBuckets,te->buckets,sizeof(int8_t)*vm->numBuckets);
            te->buckets=newBuckets;
            fivmr_TypeData_setBucketsMalloced(curTD,ubdata->typeEpoch,true);
        }
        LOG(1,("Replacing %d[%d] with %d[%d] in %s (%p)",
               ubdata->oldBucket,ubdata->oldTid&0xff,
               ubdata->newBucket,ubdata->newTid&0xff,
               curTD->name,curTD));
        te->buckets[ubdata->oldBucket]=0;
        te->buckets[ubdata->newBucket]=ubdata->newTid;
        pushWorklist(fivmr_TypeData_getVM(curTD),curTD);
    }
    return 0;
}

static void handleBucketCollision(fivmr_TypeData *td,
                                  fivmr_TypeData *newTD,
                                  uint32_t **usedBuckets) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    int32_t foundBucket;
    int32_t foundTid;
    int32_t oldBucket;
    int32_t oldTid;
    FindBucketsData fbdata;
    UpdateBucketsData ubdata;

    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    
    /* mark the old tid as no longer used */
    oldBucket=td->epochs[vm->typeEpoch^1].bucket;
    oldTid=((int32_t)td->epochs[vm->typeEpoch^1].tid)&0xff;
    LOG(1,("clearing usedTids: %d, %d",oldBucket,oldTid));
    fivmr_BitVec_set(vm->usedTids,
                     oldBucket*256+oldTid,
                     false);
    
    bzero(*usedBuckets,sizeof(int32_t)*((vm->numBuckets+31)/32));
    
    fbdata.usedBuckets=*usedBuckets;
    fbdata.typeEpoch=vm->typeEpoch^1;
    
    LOG(2,("finding buckets for %s (%p)",td->name,td));
    
    fivmr_TypeData_forShadow(td,findBuckets_cback,(uintptr_t)(void*)&fbdata);
    
    findBucketAndTid(vm,usedBuckets,&foundBucket,&foundTid);
    
    LOG(1,("Have tid/bucket for %s (%p) after collision: tid = %d, bucket = %d",
           td->name,td,foundTid,foundBucket));

    td->epochs[vm->typeEpoch^1].bucket=foundBucket;
    td->epochs[vm->typeEpoch^1].tid=foundTid;

    ubdata.newTD=newTD;
    ubdata.oldBucket=oldBucket;
    ubdata.oldTid=oldTid;
    ubdata.newBucket=foundBucket;
    ubdata.newTid=foundTid;
    ubdata.typeEpoch=vm->typeEpoch^1;
    
    fivmr_TypeData_forAllDescendantsInclusive(
        td,updateBuckets_cback,(uintptr_t)(void*)&ubdata);
}

typedef struct {
    uint32_t *usedItables;
    int32_t typeEpoch;
} FindItablesData;

static uintptr_t findItables_cback(fivmr_TypeData *startTD,
                                   fivmr_TypeData *curTD,
                                   uintptr_t arg) {
    FindItablesData *fidata=(FindItablesData*)(void*)arg;
    unsigned i;
    if (curTD!=startTD &&
        ((curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
         (curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION)) {
        for (i=0;i<curTD->numMethods;++i) {
            fivmr_MethodRec *mr=curTD->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                int32_t idx;
                
                idx=fivmr_MethodRec_itableIndexForEpoch(
                        mr,fidata->typeEpoch);
                
                fivmr_BitVec_set(fidata->usedItables,idx,true);
            }
        }
    }
    return 0;
}

static void growItables(fivmr_VM *vm,int32_t amount) {
    int32_t *newItableOcc;
    if ((vm->flags&FIVMR_VMF_ITABLE_OCC_MALLOCED)) {
        newItableOcc=fivmr_reallocAssert(vm->itableOcc,
                                         sizeof(int32_t)*(vm->itableSize+amount));
    } else {
        newItableOcc=fivmr_mallocAssert(sizeof(int32_t)*(vm->itableSize+amount));
        memcpy(newItableOcc,
               vm->itableOcc,
               sizeof(int32_t)*vm->itableSize);
    }
    bzero(newItableOcc+vm->itableSize,
          sizeof(int32_t)*amount);
    vm->itableOcc=newItableOcc;
    vm->itableSize+=amount;
    fivmr_BitField_setAtomic(&vm->flags,
                             FIVMR_VMF_ITABLE_OCC_MALLOCED,
                             FIVMR_VMF_ITABLE_OCC_MALLOCED);
}

typedef struct {
    int32_t index;
    int32_t occupancy;
} ItableEntry;

static int ItableEntry_compare(const void *a_,
                               const void *b_) {
    ItableEntry *a=(ItableEntry*)(void*)a_;
    ItableEntry *b=(ItableEntry*)(void*)b_;
    if (a->occupancy>b->occupancy) {
        return 1;
    } else if (a->occupancy==b->occupancy) {
        return 0;
    } else {
        return -1;
    }
}

static void findItableIndices(fivmr_TypeData *td,uint32_t **usedItables) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    int32_t numIfaceMethods;
    int32_t numFreeSlots;
    unsigned i,j;
    ItableEntry *occupancy;
    
    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
                 (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION);
    
    /* first figure out how many slots we need */
    numIfaceMethods=0;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            numIfaceMethods++;
        }
    }
    
    /* now figure out if we need to resize */
    numFreeSlots=0;
    for (i=0;i<vm->itableSize;++i) {
        if (!fivmr_BitVec_get(*usedItables,i)) {
            numFreeSlots++;
        }
    }
    
    if (numIfaceMethods>numFreeSlots) {
        uintptr_t oldItableSize=vm->itableSize;
        uintptr_t k;
        growItables(vm,numIfaceMethods-numFreeSlots);
        *usedItables=fivmr_reallocAssert(*usedItables,
                                         sizeof(uint32_t)*((vm->itableSize+31)/32));
        for (k=oldItableSize;k<vm->itableSize;++k) {
            fivmr_BitVec_set(*usedItables,k,false);
        }
    }
    
    /* build and sort the occupancy list */
    occupancy=fivmr_mallocAssert(sizeof(ItableEntry)*vm->itableSize);
    for (i=0;i<vm->itableSize;++i) {
        occupancy[i].index=i;
        occupancy[i].occupancy=vm->itableOcc[i];
    }
    
    fivmr_sort(occupancy,
               vm->itableSize,
               sizeof(ItableEntry),
               ItableEntry_compare);
    
    fivmr_assert(occupancy[0].occupancy
                 <= occupancy[vm->itableSize-1].occupancy);
    
    /* assign the indices */
    for (i=0,j=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            /* find first one that is unused */
            while (fivmr_BitVec_get(*usedItables,occupancy[j].index)) {
                j++;
            }
            fivmr_assert(j<vm->itableSize);
            fivmr_MethodRec_setItableIndexForEpoch(
                mr,vm->typeEpoch^1,
                occupancy[j].index);
            vm->itableOcc[occupancy[j].index]++;
            j++;
        }
    }
    
    fivmr_free(occupancy);
}

static uintptr_t populateItable_cback(fivmr_TypeData *startTD,
                                      fivmr_TypeData *curTD,
                                      uintptr_t arg) {
    fivmr_VM *vm=(fivmr_VM*)(void*)arg;
    int32_t epoch=vm->typeEpoch^1;
    
    if ((curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
        (curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION) {
        unsigned i;
        for (i=0;i<curTD->numMethods;++i) {
            fivmr_MethodRec *mr=curTD->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                int32_t idx;
                fivmr_MethodRec *implMR;
                void *loc;
                
                idx=fivmr_MethodRec_itableIndexForEpoch(mr,epoch);

                /* find the implementation of that interface method */
                implMR=
                    fivmr_TypeData_findInstMethodNoIface2(
                        vm,startTD,mr->name,mr->result,mr->nparams,mr->params);
                
                /* since this isn't abstract it had better implement that method, and
                   we better have code for it */
                fivmr_assert(implMR!=NULL);
                if ((implMR->flags&FIVMR_MBF_METHOD_KIND)!=FIVMR_MBF_ABSTRACT) {
                    fivmr_assert((implMR->flags&FIVMR_MBF_METHOD_KIND)==FIVMR_MBF_VIRTUAL ||
                                 (implMR->flags&FIVMR_MBF_METHOD_KIND)==FIVMR_MBF_FINAL);
                    fivmr_assert((implMR->flags&FIVMR_MBF_HAS_CODE));
                    
                    loc=(void*)implMR->entrypoint;
                    
                    fivmr_assert(loc!=NULL);
                    
                    if (startTD->epochs[epoch].itable[idx]==NULL) {
                        LOG(1,("Populating itable at index %d with %p",idx,loc));
                        startTD->epochs[epoch].itable[idx]=loc;
                    } else {
                        /* oh noes!  collision! */
                        return (uintptr_t)(void*)curTD;
                    }
                } else {
                    fivmr_assert((startTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT);
                }
            }
        }
    }
    
    return 0;
}

static void shrinkItable(fivmr_TypeData *td,
                         int32_t epoch) {
    unsigned newItableOff,newItableLen;
    void **newItable;
    fivmr_TypeEpoch *e;
    unsigned i;
    
    e=td->epochs+epoch;
    
    /* figure out if the itable needs shrinkage */
    newItableOff=e->itableOff;
    newItableLen=e->itableLen;
    
    fivmr_assert(newItableLen>0); /* there must be at least one itable method in there */
    
    while (e->itable[newItableOff]==NULL) {
        newItableOff++;
        newItableLen--;
        if (newItableLen==0) {
            break;
        }
        fivmr_assert(newItableOff<e->itableOff+e->itableLen);
    }
    
    LOG(1,("newItableOff = %u, newItableLen = %u",newItableOff,newItableLen));
    
    if (newItableLen==0) {
        if (fivmr_TypeData_itableMalloced(td,epoch)) {
            fivmr_free(e->itable+e->itableOff);
        }
        fivmr_TypeData_setItableMalloced(td,epoch,true);
        
        e->itableOff=0;
        e->itableLen=0;
        e->itable=NULL;
    } else {
        while (e->itable[newItableOff+newItableLen-1]==NULL) {
            newItableLen--;
            fivmr_assert(newItableLen>0);
        }
    
        if (newItableOff!=e->itableOff ||
            newItableLen!=e->itableLen) {
            /* yup, need to resize */

            newItable=fivmr_mallocAssert(sizeof(void*)*newItableLen);
            newItable-=newItableOff;
        
            for (i=newItableOff;i<newItableOff+newItableLen;++i) {
                fivmr_assert(i>=e->itableOff && i<e->itableOff+e->itableLen);
                newItable[i]=e->itable[i];
            }
        
            if (fivmr_TypeData_itableMalloced(td,epoch)) {
                fivmr_free(e->itable+e->itableOff);
            }
            fivmr_TypeData_setItableMalloced(td,epoch,true);
        
            e->itableOff=newItableOff;
            e->itableLen=newItableLen;
            e->itable=newItable;
        }
    }
}

typedef struct {
    fivmr_TypeData *newTD;
    int32_t minIdx;
    int32_t maxIdx;
    int32_t epoch;
    int32_t *oldIndices;
    void **locBuf;
} UpdateItablesData;

static uintptr_t updateItables_cback(fivmr_TypeData *startTD,
                                     fivmr_TypeData *curTD,
                                     uintptr_t arg) {
    UpdateItablesData *uidata;
    uidata=(UpdateItablesData*)(void*)arg;
    if (curTD!=uidata->newTD &&
        ((curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_VIRTUAL ||
         (curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
         (curTD->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT)) {
        fivmr_TypeEpoch *e;
        unsigned i,j;
        unsigned newItableOff,newItableLen;
        void **newItable;

        e=curTD->epochs+uidata->epoch;

        /* figure out if we need to grow the itable, and if so, grow it.  we also
           reallocate if the only version is from the payload. */
        if (uidata->minIdx<e->itableOff ||
            uidata->maxIdx>=e->itableOff+e->itableLen ||
            !fivmr_TypeData_itableMalloced(curTD,uidata->epoch)) {

            newItableOff=fivmr_min(e->itableOff,uidata->minIdx);
            newItableLen=fivmr_max(e->itableLen,uidata->maxIdx-uidata->minIdx+1);
        
            newItable=fivmr_mallocAssert(sizeof(void*)*newItableLen);
        
            for (i=newItableOff;i<newItableOff+newItableLen;++i) {
                if (i>=e->itableOff && i<e->itableOff+e->itableLen) {
                    newItable[i-newItableOff]=e->itable[i];
                } else {
                    newItable[i-newItableOff]=NULL;
                }
            }
            
            if (fivmr_TypeData_itableMalloced(curTD,uidata->epoch)) {
                fivmr_free(e->itable+e->itableOff);
            }
            fivmr_TypeData_setItableMalloced(curTD,uidata->epoch,true);
        
            e->itableOff=newItableOff;
            e->itableLen=newItableLen;
            e->itable=newItable-newItableOff;
        }
    
        /* now go through our interface methods and clear out the previously used
           itable indices, saving their contents */
        for (i=0,j=0;i<startTD->numMethods;++i) {
            fivmr_MethodRec *mr=startTD->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                int32_t oldIdx;
                
                oldIdx=uidata->oldIndices[j];

                fivmr_assert(oldIdx>=e->itableOff);
                fivmr_assert(oldIdx<e->itableOff+e->itableLen);

                uidata->locBuf[j]=e->itable[oldIdx];
                if ((curTD->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_ABSTRACT) {
                    fivmr_assert(uidata->locBuf[j]!=0);
                }
                e->itable[oldIdx]=0;
                
                j++;
            }
        }

        /* and now place the contents into the new indices. */
        for (i=0,j=0;i<startTD->numMethods;++i) {
            fivmr_MethodRec *mr=startTD->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                int32_t newIdx;
                uintptr_t loc;
                
                newIdx=fivmr_MethodRec_itableIndexForEpoch(mr,uidata->epoch);

                fivmr_assert(newIdx>=e->itableOff);
                fivmr_assert(newIdx<e->itableOff+e->itableLen);

                fivmr_assert(e->itable[newIdx]==0);
            
                e->itable[newIdx]=uidata->locBuf[j];
                
                j++;
            }
        }

        shrinkItable(curTD,uidata->epoch);
        
        pushWorklist(fivmr_TypeData_getVM(curTD),curTD);
    }
    return 0;
}

static void handleItableCollision(fivmr_TypeData *td,
                                  fivmr_TypeData *newTD,
                                  uint32_t **usedItables) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    unsigned i;
    int32_t epoch;
    FindItablesData fidata;
    UpdateItablesData uidata;
    int32_t numIfaceMethods;
    int32_t *oldIndices;
    unsigned j;

    /* what this needs to do:
       - figure out what itable indices are used by any interfaces in our
         shadow
       - switch to using itable indices that aren't used
       - let our descendants know about the change */
    
    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
                 (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION);
    
    epoch=vm->typeEpoch^1;
    
    /* remove our iface methods from the occupancy count, and count the number
       of itable methods we have */
    numIfaceMethods=0;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            vm->itableOcc[fivmr_MethodRec_itableIndexForEpoch(mr,epoch)]--;
            numIfaceMethods++;
        }
    }
    
    /* record the old itable indices */
    oldIndices=fivmr_mallocAssert(sizeof(int32_t)*numIfaceMethods);
    j=0;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            oldIndices[j++]=fivmr_MethodRec_itableIndexForEpoch(mr,epoch);
        }
    }
    
    /* initialize the usedItables set */
    bzero(*usedItables,sizeof(uint32_t)*((vm->itableSize+31)/32));
    
    /* find the itable indices used by any interfaces in our shadow; note that
       findItables_cback will exclude us from the search, which is great,
       because we're quite ok with reusing our own itable indices so long as
       they don't conflict with the *other* itable indices in our shadow */
    fidata.usedItables=*usedItables;
    fidata.typeEpoch=epoch;
    
    fivmr_TypeData_forShadow(td,findItables_cback,(uintptr_t)(void*)&fidata);
    
    /* now find some new itable indices */
    findItableIndices(td,usedItables);
    
    /* and perform the update */
    uidata.newTD=newTD;
    uidata.locBuf=fivmr_mallocAssert(sizeof(void*)*numIfaceMethods);
    uidata.epoch=epoch;
    uidata.oldIndices=oldIndices;
    uidata.minIdx=-1;
    uidata.maxIdx=-1;
    for (i=0;i<td->numMethods;++i) {
        fivmr_MethodRec *mr=td->methods[i];
        if (!(mr->flags&FIVMR_BF_STATIC) &&
            strcmp(mr->name,"<init>")) {
            int32_t idx=fivmr_MethodRec_itableIndexForEpoch(mr,epoch);
            if (uidata.minIdx<0) {
                fivmr_assert(uidata.maxIdx<0);
                uidata.minIdx=uidata.maxIdx=idx;
            } else {
                uidata.minIdx=fivmr_min(uidata.minIdx,idx);
                uidata.maxIdx=fivmr_max(uidata.maxIdx,idx);
            }
        }
    }
    
    /* if we don't have any interface methods then there is no way we should be
       in here... */
    fivmr_assert(uidata.minIdx>=0);
    fivmr_assert(uidata.maxIdx>=0);
    fivmr_assert(uidata.maxIdx>=uidata.minIdx);
    
    fivmr_TypeData_forAllDescendantsInclusive(
        td,updateItables_cback,(uintptr_t)(void*)&uidata);
    
    fivmr_free(oldIndices);
    fivmr_free(uidata.locBuf);
    
    pushWorklist(vm,td);
}

static void copyEpoch(fivmr_TypeData *td,int32_t trgEpoch,int32_t srcEpoch) {
    fivmr_TypeEpoch *src,*trg;
    fivmr_VM *vm;
    
    LOG(1,("Copying epoch in %s (%p): %d -> %d",td->name,td,srcEpoch,trgEpoch));
    
    vm=fivmr_TypeData_getVM(td);
    
    /* what we need to copy:
       - buckets/bucket/tid
       - itable/itableOff/itableLen
       - if we're an interface then all interface method indices */
    
    /* do the TypeEpoch structure first */
    src=td->epochs+srcEpoch;
    trg=td->epochs+trgEpoch;

    /* free the old stuff */
    if (fivmr_TypeData_itableMalloced(td,trgEpoch)) {
        if (trg->itableLen==0) {
            fivmr_assert(trg->itable==NULL);
            fivmr_assert(trg->itableOff==0);
        } else {
            fivmr_assert(trg->itable!=NULL);
            fivmr_free(trg->itable+trg->itableOff);
        }
    }
    if (fivmr_TypeData_bucketsMalloced(td,trgEpoch)) {
        fivmr_free(trg->buckets);
    }

    /* copy the fields over */
    *trg=*src;
    
    /* deal with the dynamically allocated parts */
    if (trg->itableLen!=0) {
        trg->itable=fivmr_mallocAssert(sizeof(void*)*trg->itableLen);
        trg->itable-=trg->itableOff;
        memcpy(trg->itable+trg->itableOff,
               src->itable+trg->itableOff,
               sizeof(void*)*trg->itableLen);
    }
    
    trg->buckets=fivmr_mallocAssert(sizeof(int8_t)*vm->numBuckets);
    memcpy(trg->buckets,src->buckets,sizeof(int8_t)*vm->numBuckets);
    
    fivmr_TypeData_setBucketsMalloced(td,trgEpoch,true);
    fivmr_TypeData_setItableMalloced(td,trgEpoch,true);
    
    /* and now if it's an interface then deal with it */
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION) {
        unsigned i;
        for (i=0;i<td->numMethods;++i) {
            fivmr_MethodRec *mr=td->methods[i];
            if (!(mr->flags&FIVMR_BF_STATIC) &&
                strcmp(mr->name,"<init>")) {
                fivmr_MethodRec_setItableIndexForEpoch(
                    mr,trgEpoch,
                    fivmr_MethodRec_itableIndexForEpoch(mr,srcEpoch));
            }
        }
    }
}

static void integrate(fivmr_TypeData *td) {
    bool usingNewEpochs=false;
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    uint32_t *usedBuckets;
    uint32_t *usedItables;
    FindBucketsData fbdata;
    FindItablesData fidata;
    int32_t i,j;
    int32_t foundBucket;
    int32_t foundTid;
    uintptr_t result;
    void **itable;
    
    LOG(1,("Integrating type %s (%p)",td->name,td));
    
    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInNative(ts));
    fivmr_assert(fivmr_Settings_canDoClassLoading(&vm->settings));
    
    initWorklist(vm);
    pushWorklist(vm,td);
    
    /* policy: we know that Payload_copy does not copy displays or itables.  Thus,
       before any modifications are made to pre-existing displays or itables, we
       must make a copy. */
    
    LOG(2,("Adding %s (%p) to subtype lists of supertypes",td->name,td));
    
    addSubtype(td->parent,td);
    for (i=0;i<td->nSuperInterfaces;++i) {
        addSubtype(td->superInterfaces[i],td);
    }
    
    LOG(2,("Incrementing descendant counts of ancestors of %s (%p)",td->name,td));
    
    fivmr_TypeData_forAllAncestorsInclusive(td,incrementDescendants_cback,0);
    
    LOG(2,("Assigning tid and bucket for %s (%p)",td->name,td));
    
    usedBuckets=fivmr_malloc(sizeof(uint32_t)*((vm->numBuckets+31)/32));
    bzero(usedBuckets,sizeof(uint32_t)*((vm->numBuckets+31)/32));
    
    fbdata.usedBuckets=usedBuckets;
    fbdata.typeEpoch=ts->typeEpoch^1;
    
    fivmr_TypeData_forAllAncestors(td,findBuckets_cback,(uintptr_t)(void*)&fbdata);
    
    findBucketAndTid(vm,&usedBuckets,&foundBucket,&foundTid);
    
    for (i=0;i<2;++i) {
        td->epochs[i].bucket=foundBucket;
        td->epochs[i].tid=foundTid;
    }
    
    LOG(1,("Have tid/bucket for %s (%p): tid = %d, bucket = %d",
           td->name,td,foundTid,foundBucket));
    
    LOG(2,("Building type display for %s (%p)",td->name,td));
    
    for (i=0;i<2;++i) {
        td->epochs[i].buckets=fivmr_mallocAssert(sizeof(int8_t)*vm->numBuckets);
        fivmr_TypeData_setBucketsMalloced(td,i,true);
    }

    /* iterate over all ancestors and populate the buckets; this will fail if
       there is a bucket collision, in which case it'll handle that collision and
       try again. */
    for (;;) {
        td->epochs[vm->typeEpoch^1].buckets=
            fivmr_reallocAssert(td->epochs[vm->typeEpoch^1].buckets,
                                sizeof(int8_t)*vm->numBuckets);
        bzero(td->epochs[vm->typeEpoch^1].buckets,
              sizeof(int8_t)*vm->numBuckets);
        result=fivmr_TypeData_forAllAncestorsSortedInclusive(
            td,populateBuckets_cback,(uintptr_t)(void*)vm);
        if (result==0) {
            /* success!  no collisions */
            break;
        }
        
        LOG(1,("Detected collision in type displays of %s (%p): on type %s (%p)",
               td->name,td,((fivmr_TypeData*)(void*)result)->name,result));
        
        vm->numBucketCollisions++;
        
        handleBucketCollision((fivmr_TypeData*)(void*)result,
                              td,
                              &usedBuckets /* just reusing memory */);
    }
    
    fivmr_free(usedBuckets);
    
    LOG(2,("Type display built for %s (%p); now handling interfaces",td->name,td));
    
    /* ok - now handle ifaces.  if this is an iface, then pick itable indices for
       all of the methods such that we don't overlap with any iface methods from
       our superclasses -and- we use the least occupied itable entries. */
    
    usedItables=fivmr_mallocAssert(sizeof(uint32_t)*((vm->itableSize+31)/32));
    bzero(usedItables,sizeof(uint32_t)*((vm->itableSize+31)/32));
    
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION) {
        LOG(2,("Finding itable indices for %s (%p)",td->name,td));
        
        fidata.usedItables=usedItables;
        fidata.typeEpoch=vm->typeEpoch^1;
        
        fivmr_TypeData_forAllAncestors(td,findItables_cback,(uintptr_t)(void*)&fidata);
        
        findItableIndices(td,&usedItables);

        LOG(2,("Itable indices found for %s (%p)",td->name,td));
    }
    
    /* init the itables to zero; anyone going and editing itables will thus not be
       confused. */
    for (i=0;i<2;++i) {
        td->epochs[i].itable=NULL;
        td->epochs[i].itableOff=0;
        td->epochs[i].itableLen=0;
        fivmr_TypeData_setItableMalloced(td,i,true);
    }
    
    /* if it's a class (abstract or not) then populate the itables */
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_VIRTUAL ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT) {
        LOG(2,("Populating itable for %s (%p)",td->name,td));
    
        for (;;) {
            td->epochs[vm->typeEpoch^1].itable=
                fivmr_reallocAssert(td->epochs[vm->typeEpoch^1].itable,
                                    sizeof(void*)*vm->itableSize);
            td->epochs[vm->typeEpoch^1].itableOff=0;
            td->epochs[vm->typeEpoch^1].itableLen=vm->itableSize;
            bzero(td->epochs[vm->typeEpoch^1].itable,
                  sizeof(void*)*vm->itableSize);
            
            result=fivmr_TypeData_forAllAncestorsSorted(td,populateItable_cback,(uintptr_t)(void*)vm);
            if (result==0) {
                /* success!  no collisions */
                break;
            }
            
            LOG(1,("Detected collision in itable of %s (%p): on type %s (%p)",
                   td->name,td,((fivmr_TypeData*)(void*)result)->name,result));
            
            vm->numItableCollisions++;
    
            handleItableCollision((fivmr_TypeData*)(void*)result,
                                  td,
                                  &usedItables);
        }
        
        LOG(2,("Itable populated for %s (%p)",td->name,td));
        
        shrinkItable(td,vm->typeEpoch^1);

        LOG(2,("Itable shrunk for %s (%p)",td->name,td));
    }
    
    fivmr_free(usedItables);
    
    /* increment the epoch and perform a soft handshake if we modified any tables
       other than our own */
    
    if (vm->wlN==1) {
        LOG(1,("We only modified one type while integrating %s (%p); epochs copied",td->name,td));

        fivmr_assert(vm->wl[0]==td);
        
        copyEpoch(td,vm->typeEpoch,vm->typeEpoch^1);
    } else {
        LOG(1,("We modified %d types while integrating %s (%p); performing soft handshake",
               vm->wlN,td->name,td));

        vm->typeEpoch^=1;
        
        fivmr_ThreadState_softHandshake(
            vm,
            FIVMR_TSEF_JAVA_HANDSHAKEABLE,
            FIVMR_TSEF_PUSH_TYPE_EPOCH);
        
        for (i=0;i<vm->wlN;++i) {
            copyEpoch(vm->wl[i],vm->typeEpoch^1,vm->typeEpoch);
        }
    }
    
    if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_VIRTUAL ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT) {
        int32_t vtableIdx;
        
        LOG(2,("Creating vtable for %s (%p)",td->name,td));
        
        /* copy vtable from parent */
        memcpy(td->vtable,td->parent->vtable,
               sizeof(void*)*td->parent->vtableLength);

        vtableIdx=td->parent->vtableLength;

        /* pick vtable indices for our methods */
        for (i=0;i<td->numMethods;++i) {
            fivmr_MethodRec *myMR=td->methods[i];
            fivmr_assert(myMR->location==(uintptr_t)(intptr_t)-1);
            if (!(myMR->flags&FIVMR_BF_STATIC) &&
                strcmp(myMR->name,"<init>")) {
                
                fivmr_MethodRec *preMR;
                
                preMR=fivmr_TypeData_findInstMethodNoIface2(
                    vm,td->parent,
                    myMR->name,myMR->result,myMR->nparams,myMR->params);
                
                if (preMR==NULL) {
                    /* this is a new method signature and should get a new vtable
                       index, if the method is not declared final */
                    if ((myMR->flags&FIVMR_MBF_METHOD_KIND)==FIVMR_MBF_FINAL ||
                        (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL) {
                        /* don't get a vtable index */
                    } else {
                        /* assign new vtable index */
                        myMR->location=vtableIdx++;
                    }
                } else {
                    /* simple - get parent's vtable index */
                    myMR->location=preMR->location;
                }
                
                if (myMR->location!=(uintptr_t)(intptr_t)-1) {
                    /* we gave ourselves a vtable slot, so populate it */
                    td->vtable[myMR->location]=myMR->entrypoint;
                }
            }
        }
        
        /* make sure we agree on the vtable length */
        fivmr_assert(vtableIdx==td->vtableLength);
    }

    LOG(2,("Integration complete for %s (%p)",td->name,td));
}

static uintptr_t findUnresolved_cback(fivmr_TypeData *startTD,
                                      fivmr_TypeData *curTD,
                                      uintptr_t arg) {
    if (!(curTD->flags&FIVMR_TBF_RESOLUTION_DONE) &&
        (curTD->parent->flags&FIVMR_TBF_RESOLUTION_DONE)) {
        unsigned i;
        for (i=0;i<curTD->nSuperInterfaces;++i) {
            if (!(curTD->superInterfaces[i]->flags&FIVMR_TBF_RESOLUTION_DONE)) {
                return 0;
            }
        }
        return (uintptr_t)(void*)curTD;
    }
    return 0;
}

bool fivmr_TypeData_resolve(fivmr_TypeData *td) {
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    unsigned i;
    bool result;

    ctx=fivmr_TypeContext_fromStatic(td->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);
    
    fivmr_assert(fivmr_ThreadState_isInJava(ts));

    if ((td->flags&(FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED))) {
        fivmr_fence();
        result=!(td->flags&FIVMR_TBF_RESOLUTION_FAILED);
    } else {
        
        fivmr_Nanos before,after;
        before=fivmr_curTime();
        
        result=true;
        
        /* figure out if there are any ancestors that are unresolved but
           whose ancestors are resolved */
        for (;;) {
            fivmr_ThreadState_goToNative(ts);
            fivmr_Lock_lock(&vm->typeDataLock);
            
            fivmr_TypeData *td2=(fivmr_TypeData*)(void*)
                fivmr_TypeData_forAllAncestors(td,findUnresolved_cback,0);
            
            fivmr_Lock_unlock(&vm->typeDataLock);
            fivmr_ThreadState_goToJava(ts);
            
            if (td2==NULL) {
                break;
            }
            if (!fivmr_TypeData_resolve(td2)) {
                result=false;
            }
        }
        
        /* same for element types */
        if (result) {
            for (;;) {
                bool cont=false;
                fivmr_TypeData *td2;
                for (td2=td->arrayElement;td2!=NULL;td2=td2->arrayElement) {
                    if (!(td2->flags&FIVMR_TBF_RESOLUTION_DONE) &&
                        (td2->arrayElement==NULL ||
                         (td2->arrayElement->flags&FIVMR_TBF_RESOLUTION_DONE))) {
                        if (fivmr_TypeData_resolve(td2)) {
                            cont=true;
                        } else {
                            result=false;
                            cont=false;
                        }
                        break;
                    }
                }
                if (!cont) {
                    break;
                }
            }
        }

        /* assert linker constraints */
        if (result) {
            for (i=0;i<td->numMethods;++i) {
                fivmr_MethodRec *myMR=td->methods[i];
                if (!(myMR->flags&FIVMR_BF_STATIC) &&
                    strcmp(myMR->name,"<init>")) {
                    
                    fivmr_MethodRec *preMR;
                    
                    preMR=fivmr_TypeData_findInstMethodNoIface3(
                        vm,td->parent,
                        myMR->name,myMR->result,myMR->nparams,myMR->params);
                    
                    if (preMR!=NULL &&
                        (!fivmr_TypeStub_union(myMR->result,preMR->result) ||
                         !fivmr_TypeStub_unionParams(myMR->nparams,myMR->params,
                                                     preMR->nparams,preMR->params))) {
                        result=false;
                        break;
                    }
                }
            }
        }
        
        fivmr_ThreadState_goToNative(ts);
        
        fivmr_Lock_lock(&vm->typeDataLock);
        
        if ((td->flags&(FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED))) {
            if (result) {
                fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
                fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_DONE));
            } else {
                fivmr_assert((td->flags&FIVMR_TBF_RESOLUTION_FAILED));
                fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_DONE));
            }
        } else {
            int32_t oldFlags;
            
            if (result) {
                /* do this in native since we may want to trigger handshakes */
                /* make sure all of our supertypes have us in their subtype list */
                /* integrate this type into the type epochs and build the vtable */
                integrate(td);
                
                /* check if the type is already effectively initialized. */
                if ((td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ABSTRACT ||
                    (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_VIRTUAL ||
                    (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_FINAL ||
                    (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_ANNOTATION ||
                    (td->flags&FIVMR_TBF_TYPE_KIND)==FIVMR_TBF_INTERFACE) {
                    bool parentsInited=(td->parent->inited==1);
                    
                    if (parentsInited) {
                        int32_t i;
                        for (i=0;i<td->nSuperInterfaces;++i) {
                            if (td->parent->inited!=1) {
                                parentsInited=false;
                                break;
                            }
                        }
                    }
                    
                    if (parentsInited) {
                        if (fivmr_TypeData_findStaticMethod(vm,td,"<clinit>","()V")
                            == NULL) {
                            td->inited=1;
                        } else {
                            /* we have a clinit - so the type will have to go
                               through checkInit */
                        }
                    } else {
                        /* do nothing - the type will have to go through
                           checkInit because its supertypes are not initialized or
                           else experienced an error during initialization */
                    }
                } else {
                    /* arrays are always initialized
                       FIXME: that might not be totally true, especially for arrays
                       that have backing classes.  but those arrays are currently
                       part of the executable, so the initial initialization state
                       is the compiler's problem (i.e. this case is irrelevant here). */
                    td->inited=1;
                }
                
                oldFlags=fivmr_BitField_setAtomic(
                    &td->flags,
                    FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED,
                    FIVMR_TBF_RESOLUTION_DONE);
            } else {
                oldFlags=fivmr_BitField_setAtomic(
                    &td->flags,
                    FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_FAILED,
                    FIVMR_TBF_RESOLUTION_FAILED);
            }
            
            fivmr_assert(!(oldFlags&FIVMR_TBF_RESOLUTION_DONE));
            fivmr_assert(!(oldFlags&FIVMR_TBF_RESOLUTION_FAILED));
        }
        
        fivmr_Lock_unlock(&vm->typeDataLock);
        
        fivmr_ThreadState_goToJava(ts);

        /* indicate that we've already done resolution on this class */
        fivmr_fence();
        
        fivmr_assert((td->flags&(FIVMR_TBF_RESOLUTION_DONE|FIVMR_TBF_RESOLUTION_DONE)));
        
        after=fivmr_curTime();
        LOG(1,("Resolving %s took %u ns",
               td->name,(unsigned)(after-before)));
    }
    
    return result;
}

static fivmr_TypeData *findDefined(fivmr_TypeContext *ctx,
                                   const char *name) {
    fivmr_TypeData *result;
    ftree_Node *node;
    
    result=fivmr_StaticTypeContext_find(&ctx->st,name);
    if (result!=NULL) {
        return result;
    }
    
    node=ftree_Tree_findFast(&ctx->dynamicTypeTree,
                             (uintptr_t)(void*)name,
                             fivmr_TypeData_compareKey);
    if (node==NULL) {
        return NULL;
    } else {
        return (fivmr_TypeData*)(void*)node->value;
    }
}

static fivmr_TypeData *findArrayInNative(fivmr_TypeData *td) {
    int len;
    char *name;
    fivmr_TypeData *result;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    
    fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB);
    if (td->arrayType!=NULL) {
        return td->arrayType;
    }
    
    /* FIXME: is the rest of this necessary? */
    
    ts=fivmr_ThreadState_get(fivmr_TypeData_getVM(td));
    fivmr_assert(fivmr_ThreadState_isInNative(ts));

    ctx=fivmr_TypeContext_fromStatic(td->context);

    len=strlen(td->name)+2;
    name=alloca(len);
    snprintf(name,len,"[%s",td->name);

    fivmr_Lock_lock(&ctx->treeLock);
    result=findDefined(ctx,name);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    return result;
}

static fivmr_TypeData *findArrayInJava(fivmr_TypeData *td) {
    fivmr_TypeData *result;
    fivmr_ThreadState *ts;
    fivmr_assert((td->flags&FIVMR_TBF_TYPE_KIND)!=FIVMR_TBF_STUB);
    if (td->arrayType!=NULL) {
        return td->arrayType;
    }
    ts=fivmr_ThreadState_get(fivmr_TypeData_getVM(td));
    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    fivmr_ThreadState_goToNative(ts);
    result=findArrayInNative(td);
    fivmr_ThreadState_goToJava(ts);
    return result;
}

static fivmr_TypeData *makeArrayImpl(fivmr_TypeData *eleTD) {
    int len;
    char *name;
    fivmr_VM *vm;
    fivmr_TypeContext *ctx;
    fivmr_ThreadState *ts;
    fivmr_TypeData *td;
    ftree_Node *node;
    fivmr_TypeData *oldTD;
    unsigned i;
    
    len=strlen(eleTD->name)+2;
    name=alloca(len);
    snprintf(name,len,"[%s",eleTD->name);

    ctx=fivmr_TypeContext_fromStatic(eleTD->context);
    vm=ctx->vm;
    ts=fivmr_ThreadState_get(vm);

    fivmr_assert(fivmr_ThreadState_isInJava(ts));
    
    /* do this in native since we may want to trigger handshakes */
    fivmr_ThreadState_goToNative(ts);
    
    fivmr_Lock_lock(&ctx->treeLock);
    fivmr_Lock_lock(&vm->typeDataLock);
    
    td=findDefined(ctx,name);
    if (td==NULL) {
        td=fivmr_mallocAssert(fivmr_TypeData_sizeOfTypeData(vm->payload->td_Object));
        
        /* do some random stuff to mark this as a "blank" array type, including
           linking it to its element type and locating its supertypes */
        
        memcpy(td,
               vm->payload->td_Object,
               fivmr_TypeData_sizeOfTypeData(vm->payload->td_Object));
        
        td->state=FIVMR_MS_INVALID;
        td->forward=td;
        td->context=&ctx->st;
        td->inited=1; /* initialized successfully */
        td->curIniter=NULL;
        td->name=strdup(name);
        fivmr_assert(td->name!=NULL);
        td->filename=NULL;
        td->flags=eleTD->flags;
        td->flags&=~FIVMR_TBF_TYPE_KIND;
        td->flags|=FIVMR_TBF_ARRAY;
        td->flags&=~FIVMR_TBF_RESOLUTION_DONE;
        fivmr_assert(!(td->flags&FIVMR_TBF_RESOLUTION_FAILED));
        td->parent=findArrayInNative(eleTD->parent);
        fivmr_assert(td->parent!=NULL);
        td->nSuperInterfaces=eleTD->nSuperInterfaces;
        td->superInterfaces=fivmr_mallocAssert(
            sizeof(fivmr_TypeData*)*td->nSuperInterfaces);
        for (i=0;i<td->nSuperInterfaces;++i) {
            td->superInterfaces[i]=findArrayInNative(eleTD->superInterfaces[i]);
            fivmr_assert(td->superInterfaces[i]!=NULL);
        }
        td->nDirectSubs=0;
        td->directSubs=NULL;
        td->ilistSize=0;
        td->ilist=NULL;
        td->canonicalNumber=0;
        td->numDescendants=0; /* integrate will increment this */
        bzero(td->epochs,sizeof(fivmr_TypeEpoch)*2);
        td->arrayElement=eleTD;
        td->arrayType=NULL;
        td->size=0;
        td->requiredAlignment=0;
        td->refSize=FIVMSYS_PTRSIZE;
        td->bytecode=0;
        td->classObject=0;
        td->node=NULL; /* this will be set by defineImpl */
        td->numMethods=0;
        td->numFields=0;
        td->methods=NULL;
        td->fields=NULL;
        td->gcMap=0;
        
        /* is that really it? */
        
        fivmr_allocateClass(ts,td);
        
        /* note: the Class cannot be GC'd here because we're returning a handle
           in some native context.  FIXME: figure out the native context story
           so as to ensure that these temporary handles get freed eventually. */
        
        if (ts->curExceptionHandle==NULL) {
            fivmr_assert(eleTD->arrayType==NULL);

            /* need to go to Java because that's what defineImpl expects */
            fivmr_ThreadState_goToJava(ts);
    
            oldTD=defineImpl(ctx,td);
            
            if (oldTD!=td) {
                /* the only way for defineImpl to have failed is if linker constraints
                   had been violated, in which case defineImpl would return a
                   different array type. */
                fivmr_assert(oldTD->arrayElement!=td->arrayElement);
                
                fivmr_TypeData_free(td);
                td=NULL;
            } else {
                eleTD->arrayType=td;
            }
            
        } else {
            ts->curExceptionHandle=NULL;
            fivmr_TypeData_free(td);
            td=NULL;
        }
    }
    
    fivmr_Lock_unlock(&vm->typeDataLock);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    if (fivmr_ThreadState_isInNative(ts)) {
        fivmr_ThreadState_goToJava(ts);
    }
    
    return td;
}

static uintptr_t findArraysToMake_cback(fivmr_TypeData *startTD,
                                        fivmr_TypeData *curTD,
                                        uintptr_t arg) {
    LOG(2,("Considering making array for %s (%p)",curTD->name,curTD));
    if (findArrayInJava(curTD)==NULL) {
        if (findArrayInJava(curTD->parent)!=NULL) {
            unsigned i;
            for (i=0;i<curTD->nSuperInterfaces;++i) {
                if (findArrayInJava(curTD->superInterfaces[i])==NULL) {
                    LOG(2,("Rejecting %s (%p) because at least one of its superinterfaces (%s (%p)) lacks an array.",curTD->name,curTD,curTD->superInterfaces[i]->name,curTD->superInterfaces[i]));
                    return 0;
                }
            }
            /* ok - this guy doesn't have an array but all of his ancestors
               do.  so he's the target. */
            return (uintptr_t)(void*)curTD;
        } else {
            LOG(2,("Rejecting %s (%p) because its supertype (%s (%p)) lacks an array.",curTD->name,curTD,curTD->parent->name,curTD->parent));
        }
    } else {
        LOG(2,("Rejecting %s (%p) because it already has an array.",curTD->name,curTD));
    }
    return 0;
}

fivmr_TypeData *fivmr_TypeData_makeArray(fivmr_TypeData *td) {
    fivmr_TypeData *result;
    
    result=findArrayInJava(td);
    
    if (result==NULL) {
        /* this is a horrid non-recursive way of ensuring that all supertype
           arrays are made, as well.  I'm doing it this way to avoid
           recursion. */
        
        for (;;) {
            LOG(2,("Trying to figure out how to make an array for %s (%p)",td->name,td));
            fivmr_TypeData *target=(fivmr_TypeData*)(void*)
                fivmr_TypeData_forAllAncestorsInclusive(
                    td,findArraysToMake_cback,0);
            if (target==NULL) {
                break;
            }
            LOG(2,("Making array for %s (%p)",target->name,target));
            result=makeArrayImpl(target);
            if (result==NULL) {
                return NULL;
            }
        }
        
        result=findArrayInJava(td);
        fivmr_assert(result!=NULL);
    }
    
    return result;
}

void fivmr_TypeData_free(fivmr_TypeData *td) {
    fivmr_FieldRec *fields;
    fivmr_MethodRec **methods;
    unsigned i;
    
    fivmr_freeIfNotNull(td->name);
    fivmr_freeIfNotNull(td->superInterfaces);
    fivmr_freeIfNotNull(td->directSubs);
    fivmr_freeIfNotNull(td->ilist);
    fivmr_freeIfNotNull(td->filename);
    
    fields=td->fields;
    if (fields!=NULL) {
        for (i=0;i<td->numFields;++i) {
            fivmr_FieldRec *fr=fields+i;
            fivmr_freeIfNotNull(fr->name);
        }
        fivmr_free(fields);
    }
    
    methods=td->methods;
    if (methods!=NULL) {
        for (i=0;i<td->numMethods;++i) {
            fivmr_MethodRec *mr=methods[i];
            if (mr!=NULL) {
                fivmr_freeIfNotNull(mr->name);
                fivmr_freeIfNotNull(mr->params);
                fivmr_free(mr);
            }
        }
        fivmr_free(methods);
    }
    
    fivmr_free(td);
}

bool fivmr_TypeData_fixEntrypoint(fivmr_TypeData *td,
                                  void *oldEntrypoint,
                                  void *newEntrypoint) {
    /* NOTE: we would not benefit at all from knowing the original MethodRec's
       location, since there may be multiple MethodRecs that this one overrides
       or implements.  so we trade-off speed for a reduction in necessary
       book-keeping. */
    
    int i,j;
    bool result=false;
    
    for (i=0;i<td->vtableLength;++i) {
        if (td->vtable[i]==oldEntrypoint) {
            td->vtable[i]=newEntrypoint;
            result=true;
        }
    }
    
    for (j=0;j<2;++j) {
        fivmr_TypeEpoch *e=td->epochs+j;
        for (i=e->itableOff;i<e->itableOff+e->itableLen;++i) {
            if (e->itable[i]==oldEntrypoint) {
                e->itable[i]=newEntrypoint;
                result=true;
            }
        }
    }
    
    return true;
}

void *fivmr_TypeContext_addUntracedField(fivmr_TypeContext *ctx,
                                         int32_t size) {
    void *result;
    
    if (FIVMR_ASSERTS_ON) {
        fivmr_ThreadState *ts;
        ts=fivmr_ThreadState_get(ctx->vm);
        fivmr_assert(fivmr_ThreadState_isInNative(ts));
    }
    
    fivmr_Lock_lock(&ctx->treeLock);
    result=fivmr_TypeAux_addUntracedZero(ctx,&ctx->aux,(size_t)size);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    return result;
}

void *fivmr_TypeContext_addTracedField(fivmr_TypeContext *ctx) {
    void *result;

    if (FIVMR_ASSERTS_ON) {
        fivmr_ThreadState *ts;
        ts=fivmr_ThreadState_get(ctx->vm);
        fivmr_assert(fivmr_ThreadState_isInNative(ts));
    }
    
    fivmr_Lock_lock(&ctx->treeLock);
    result=fivmr_TypeAux_addPointer(ctx,&ctx->aux,0);
    fivmr_Lock_unlock(&ctx->treeLock);
    
    return result;
}

# 1 "fivmr_util.c"
/*
 * fivmr_util.c
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

#include "fivmr.h"

bool fivmr_abortThrow;

void fivmr_describeException(fivmr_ThreadState *ts,
                             fivmr_Handle *h) {
    fivmr_describeExceptionImpl(ts,h);
    if (ts->curExceptionHandle!=NULL) {
	fivmr_Handle *h2=ts->curExceptionHandle;
	ts->curExceptionHandle=NULL;
	LOG(0,("Thread %u: Caught exception: %s",
	       ts->id,fivmr_TypeData_forObject(&ts->vm->settings,h->obj)->name));
	LOG(0,("Thread %u: Could not print stack trace because: %s",
	       ts->id,fivmr_TypeData_forObject(&ts->vm->settings,h2->obj)->name));
	fivmr_ThreadState_removeHandle(ts,h2);
    }
}

void fivmr_assertNoException(fivmr_ThreadState *ts,
                             const char *context) {
    if (ts->curException!=0 || ts->curExceptionHandle!=NULL) {
	fivmr_Object exception=0;
	fivmr_Log_lockedPrintf("Fatal error in fivm runtime system: %s\n",context);
	if (ts->curException!=0) {
	    fivmr_Handle h;
	    fivmr_assert(ts->curExceptionHandle==NULL);
	    exception=h.obj=ts->curException;
	    ts->curException=0;
	    fivmr_ThreadState_goToNative(ts); /* hack.  should have a way of describing
						 exceptions when we're already
						 IN_JAVA */
	    fivmr_describeException(ts,&h);
	} else if (ts->curExceptionHandle!=NULL) {
	    fivmr_Handle *h;
	    fivmr_assert(ts->curException==0);
	    h=ts->curExceptionHandle;
	    exception=h->obj;
	    ts->curExceptionHandle=NULL;
	    fivmr_describeException(ts,h);
	}
	abort();
    }
}

void fivmr_throw(fivmr_ThreadState *ts,
                 fivmr_Object obj) {
    int logAtLevel;
    
    if (obj==0) {
        fivmr_throwNullPointerRTE_inJava(ts);
        return;
    }
    
    if (ts->vm->exceptionsFatalReason!=NULL || fivmr_abortThrow) {
        const char *reason;
        reason=ts->vm->exceptionsFatalReason;
        if (reason==NULL) {
            reason="Unexpected exception (fivmr_abortThrow=true)";
        }
	fivmr_Log_lockedPrintf("Thread %u: %s: %s (%p)\n",
			       ts->id,
			       reason,
			       fivmr_TypeData_forObject(&ts->vm->settings,obj)->name,
			       obj);
	fivmr_ThreadState_dumpStackFor(ts);
	fivmr_abortf("%s.",reason);
    }
    
    if ((ts->vm->flags&FIVMR_VMF_LOG_THROW)) {
        logAtLevel=0;
    } else {
        logAtLevel=3;
    }

    if (LOGGING(logAtLevel)) {
	LOG(logAtLevel,
            ("Thread %u: throwing exception %p with type %s",
             ts->id,obj,fivmr_TypeData_forObject(&ts->vm->settings,obj)->name));
	fivmr_ThreadState_dumpStackFor(ts);
    }
    ts->curException=obj;
}

void fivmr_throwOOME(fivmr_ThreadState *ts) {
    fivmr_throwOutOfMemoryError_inJava(ts);
}

fivmr_Handle *fivmr_newGlobalHandle(fivmr_ThreadState *ts,
				    fivmr_Handle *h) {
    fivmr_Handle *result;
    fivmr_Lock_lock(&ts->vm->hrLock);
    result=fivmr_HandleRegion_add(&ts->vm->hr,
				  ts,
				  &ts->vm->freeHandles,
				  fivmr_Handle_get(h));
    fivmr_Lock_unlock(&ts->vm->hrLock);
    return result;
}

void fivmr_deleteGlobalHandle(fivmr_Handle *h) {
    fivmr_Lock_lock(&h->vm->hrLock);
    fivmr_Handle_remove(&h->vm->freeHandles,h);
    fivmr_Lock_unlock(&h->vm->hrLock);
}

void fivmr_OTH_init(fivmr_OTH *oth,
                    uintptr_t n) {
    uintptr_t size;
    oth->n=n;
    size=fivmr_OTH_calcSize(n);
    oth->list=fivmr_mallocAssert(size);
    bzero(oth->list,size);
}

void fivmr_OTH_initEasy(fivmr_OTH *oth,
                        uintptr_t numEle) {
    fivmr_OTH_init(oth,numEle*3/2);
}

void fivmr_OTH_free(fivmr_OTH *oth) {
    if (oth->n>0) {
        fivmr_assert(oth->list!=NULL);
        fivmr_free(oth->list);
    } else {
        fivmr_assert(oth->list==NULL);
    }
}

void fivmr_OTH_clear(fivmr_OTH *oth) {
    bzero(oth->list,fivmr_OTH_calcSize(oth->n));
}

bool fivmr_OTH_put(fivmr_OTH *oth,
                   void *key,
                   void *val) {
    uintptr_t i;
    uintptr_t start;
    fivmr_assert(key!=0);
    start=fivmr_OTH_ptrHash((uintptr_t)key);
    i=start%oth->n;
    for (;;) {
        uintptr_t pkey=oth->list[i*2];
        if (pkey==(uintptr_t)key) {
            oth->list[i*2+1]=(uintptr_t)val;
            return false;
        } else if (pkey==0) {
            oth->list[i*2]=(uintptr_t)key;
            oth->list[i*2+1]=(uintptr_t)val;
            return true;
        }
        i++;
        if (i==oth->n) i=0;
        fivmr_assert(i!=(start%oth->n)); /* assert that hashtable is not full */
    }
    fivmr_assert(!"not reached");
    return false;
}

void *fivmr_OTH_get(fivmr_OTH *oth,
                    void *key) {
    uintptr_t i;
    uintptr_t start;
    start=fivmr_OTH_ptrHash((uintptr_t)key);
    i=start%oth->n;
    for (;;) {
        uintptr_t pkey=oth->list[i*2];
        if (pkey==0) {
            return NULL;
        } else if (pkey==(uintptr_t)key) {
            return (void*)oth->list[i*2+1];
        }
        i++;
        if (i==oth->n) i=0;
        fivmr_assert(i!=(start%oth->n)); /* assert that hashtable is not full */
    }
    fivmr_assert(!"not reached");
    return NULL;
}

int32_t fivmr_basetypeSize(char c) {
    switch (c) {
    case 'V':
        return 0;
    case 'Z':
    case 'B':
        return 1;
    case 'S':
    case 'C':
        return 2;
    case 'I':
    case 'F':
        return 4;
    case 'J':
    case 'D':
        return 8;
    case 'P':
    case 'f':
    case 'L':
    case '[':
        return sizeof(void*);
    default:
        fivmr_abortf("Wrong basetype character: '%d'",(int)c);
        return 0;
    }
}

int32_t fivmr_Baseline_offsetToJStack(fivmr_MethodRec *mr) {
    int32_t result;
    
    fivmr_assert(FIVMR_X86);
    
    result=0;
    
    result-=4; /* esi */
    result-=4; /* Frame::up */
    result-=4; /* Frame::debugID */
    
    if ((mr->flags&FIVMR_MBF_SYNCHRONIZED) &&
        !(mr->flags&FIVMR_BF_STATIC)) {
        result-=4; /* receiver */
    }
    
    result-=4*mr->nLocals;
    
    return result;
}

int32_t fivmr_Baseline_offsetToSyncReceiver(fivmr_MethodRec *mr) {
    int32_t result;
    
    fivmr_assert(FIVMR_X86);
    
    result=0;
    
    result-=4; /* esi */
    result-=4; /* Frame::up */
    result-=4; /* Frame::debugID */
    
    result-=4; /* and the receiver */
    
    return result;
}

/* FIXME: maybe it'd be better if we retried resolution every time, like we
   do patch points?  instead of installing a thunk that throws an exception? */

void fivmr_resolveField(fivmr_ThreadState *ts,
                        uintptr_t returnAddr,
                        fivmr_BaseFieldAccess *bfa) {
    ts->curF->id=bfa->debugID;
    fivmr_handleFieldResolution(ts,returnAddr,bfa);
}

void fivmr_resolveMethod(fivmr_ThreadState *ts,
                         uintptr_t returnAddr,
                         fivmr_BaseMethodCall *bmc) {
    /* this function has a purpose.  if you think that it doesn't, and try to
       "factor it out", then you are a fool.  so look carefully, and study
       hard, before making a retarded decision that someone smarter than you
       will have to back out later. */
    fivmr_handleMethodResolution(ts,ts->curF->id,returnAddr,bmc);
}

void fivmr_resolveArrayAlloc(fivmr_ThreadState *ts,
                             uintptr_t returnAddr,
                             fivmr_BaseArrayAlloc *baa) {
    ts->curF->id=baa->debugID;
    fivmr_handleArrayAlloc(ts,returnAddr,baa);
}

void fivmr_resolveObjectAlloc(fivmr_ThreadState *ts,
                              uintptr_t returnAddr,
                              fivmr_BaseObjectAlloc *boa) {
    ts->curF->id=boa->debugID;
    fivmr_handleObjectAlloc(ts,returnAddr,boa);
}

void fivmr_resolveInstanceof(fivmr_ThreadState *ts,
                             uintptr_t returnAddr,
                             fivmr_BaseInstanceof *bio) {
    ts->curF->id=bio->debugID;
    fivmr_handleInstanceof(ts,returnAddr,bio);
}

void fivmr_handlePatchPoint(fivmr_ThreadState *ts,
                            const char *className,
                            const char *fromWhereDescr,
                            int bcOffset,
                            void **patchThunkPtrPtr,
                            void *origPatchThunk) {
    fivmr_handlePatchPointImpl(ts,ts->curF->id,className,fromWhereDescr,
                               bcOffset,patchThunkPtrPtr,origPatchThunk);
}

void fivmr_baselineThrow(fivmr_ThreadState *ts,
                         uintptr_t framePtr,
                         uintptr_t *result) {
    /* what this does:
       1) gets the MachineCode and MethodRec for the current method
       2) gets the try catch metadata
       3) searches for a handler
       4a) if one is found, finds the machinecode address, and sets
           result appropriately
       4b) if one is not found, sets the result to indicate return */
    
    fivmr_DebugRec *dr;
    fivmr_MachineCode *mc;
    fivmr_MethodRec *mr;
    fivmr_BaseTryCatch *btc;
    fivmr_BaseTryCatch *correctBTC;
    int32_t bytecodePC;
    fivmr_Object exc;
    fivmr_TypeData *excType;
    
    fivmr_assert(FIVMR_CAN_DO_CLASSLOADING);
    
    LOG(2,("Attempting to dispatch exception in baseline code; "
           "Thread #%u, framePtr = %p, resultPtr = %p",
           ts,framePtr,result));
    
    fivmr_assert(ts->curException!=0);
    fivmr_assert(ts->curExceptionHandle==NULL);
    
    /* find the MethodRec */
    
    dr=fivmr_DebugRec_lookup(ts->vm,ts->curF->id);
    
    LOG(2,("have DebugRec at %p",dr));
    LOG(2,("line number = %d, pc = %d",
           fivmr_DebugRec_getLineNumber(dr),
           fivmr_DebugRec_getBytecodePC(dr)));
    
    fivmr_assert(fivmr_MachineCode_isMachineCode(dr->method));
    
    mr=fivmr_MachineCode_decodeMethodRec(dr->method);
    
    LOG(2,("have MethodRec at %p",mr));
    LOG(2,("method is %s",fivmr_MethodRec_describe(mr)));
    
    /* find the canonical baseline machinecode.  this is intentionally different
       than the MachineCode we found in the DebugRec, because that MachineCode
       may be a BASE_PATCH. */
    
    fivmr_ThreadState_goToNative(ts);
    mc=fivmr_MethodRec_findMC(mr,FIVMR_MC_KIND,FIVMR_MC_BASELINE);
    fivmr_ThreadState_goToJava(ts);

    fivmr_assert(mc!=NULL);
    fivmr_assert((mc->flags&FIVMR_MC_KIND)==FIVMR_MC_BASELINE);
    fivmr_assert((mc->flags&FIVMR_MC_POSSIBLE_ENTRYPOINT));
    
    /* figure out the bytecode PC */
    
    bytecodePC=fivmr_DebugRec_getBytecodePC(dr);
    
    /* figure out what the exception is, and its type */
    
    exc=ts->curException;
    excType=fivmr_Object_getTypeData(&ts->vm->settings,exc);
    
    /* figure out which catch block, if any, applies */
    correctBTC=NULL;
    for (btc=mc->btcFirst;btc!=NULL;btc=btc->next) {
        if (bytecodePC>=btc->start &&
            bytecodePC<btc->end) {
            /* possible candidate; check if the type is resolved.  if it isn't
               then we know that the exception cannot be a subtype of this catch
               block's type since there is no way to allocate something of
               unresolved type. */
            fivmr_TypeData *catchType=NULL; /* make GCC happy */
            if (btc->type!=NULL) {
                catchType=fivmr_TypeStub_tryGetTypeData(btc->type);
            }
            if (btc->type==NULL ||
                (catchType!=NULL &&
                 fivmr_TypeData_isSubtypeOf(ts,excType,catchType))) {
                correctBTC=btc;
                break;
            }
        }
    }
    
    if (correctBTC==NULL) {
        fivmr_Object receiver;
        
        LOG(2,("Telling method to return with exception"));
        
        /* easy case: we just tell the method to return with an exception */
        result[0]=0;
        result[1]=0;
        
        /* release any locks */
        if ((mr->flags&FIVMR_MBF_SYNCHRONIZED)) {
            if ((mr->flags&FIVMR_BF_STATIC)) {
                receiver=mr->owner->classObject;
            } else {
                receiver=*(fivmr_Object*)(
                    framePtr+fivmr_Baseline_offsetToSyncReceiver(mr));
            }
            
            fivmr_Object_unlock(ts,receiver);
        }
        
        /* pop the frame */
        ts->curF=ts->curF->up;
        
        /* done */
    } else {
        fivmr_Basepoint *bp;
        fivmr_Basepoint *correctBP;
        
        /* hard case: find the machinecode PC to jump to */
        correctBP=NULL;
        for (bp=mc->bpList;bp!=NULL;bp=bp->next) {
            if (bp->bytecodePC == correctBTC->target) {
                correctBP=bp;
                break;
            }
        }
        
        /* we have to be able to find the basepoint.  the compiler will
           ensure this. */
        fivmr_assert(correctBP!=NULL);
        
        result[0]=(uintptr_t)fivmr_Baseline_offsetToJStack(mr);
        result[1]=(uintptr_t)correctBP->machinecodePC;
        
        LOG(2,("Telling method to jump to %p and set stack to FP + %p",
               result[1],result[0]));

        /* done */
    }
    
    fivmr_MachineCode_down(mc);
}

# 1 "fivmr_vmthread.c"
/*
 * fivmr_vmthread.c
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

static void vmthread_runner(void *arg) {
    fivmr_ThreadState *ts;
    fivmr_JmpBuf volatile jmpbuf;
    
    ts=(fivmr_ThreadState*)arg;
    
    if (ts->vm->pool==NULL) {
        ts->exitOnExit=true;
    } else {
        ts->jumpOnExit=(fivmr_JmpBuf*)&jmpbuf;
        if (fivmr_JmpBuf_label((fivmr_JmpBuf*)&jmpbuf)) {
            LOG(1,("Jumped out of VM; returning. (1)"));
            return;
        }
    }
    
    fivmr_assert(fivmr_ThreadPriority_leRT(fivmr_Thread_getPriority(fivmr_ThreadHandle_current()),
                                           ts->vm->maxPriority));
    
    fivmr_ThreadState_set(ts,NULL);
    
    fivmr_VMThread_run(ts,ts->vm->javaThreads+ts->id);
    
    if (ts->curExceptionHandle!=NULL) {
	fivmr_Handle *e;
	
	fivmr_ThreadState_goToJava(ts);
	e=fivmr_ThreadState_cloneHandle(ts,ts->curExceptionHandle);
	ts->curExceptionHandle=NULL;
	fivmr_ThreadState_goToNative(ts);
	
	if (!fivmr_VMThread_setUncaughtException(ts,ts->vm->javaThreads+ts->id,e)) {
	    LOG(0,("Thread %u terminated with exception; could "
		   "not pass it to uncaught exception handler.",
		   ts->id));
	    fivmr_describeException(ts,e);
	}
    }
    
    fivmr_ThreadState_terminate(ts);
}

void fivmr_VMThread_priorityChanged(fivmr_ThreadState *curTS,
                                    fivmr_Handle *vmt) {
    int32_t javaPrio=fivmr_VMThread_getPriority(curTS,vmt);
    fivmr_ThreadState *ts=fivmr_VMThread_getThreadState(curTS,vmt);
    fivmr_assert(ts->thread!=fivmr_ThreadHandle_zero());
    
    fivmr_assert(fivmr_ThreadPriority_leRT(javaPrio,curTS->vm->maxPriority));
    
    fivmr_ThreadState_setBasePrio(ts,javaPrio);
}

void fivmr_VMThread_start(fivmr_ThreadState *curTS,fivmr_Handle *vmt) {
    fivmr_ThreadState *ts;
    fivmr_ThreadHandle th;
    fivmr_ThreadPriority prio;
    
    ts=fivmr_ThreadState_new(curTS->vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
    if (ts==NULL) {
        LOG(1,("Could not get a new ThreadState; are you sure you aren't starting "
               "more threads than the system can support?  Max threads = %u, "
               "numActive = %u, numDaemons = %u.",
               curTS->vm->config.maxThreads,curTS->vm->numActive,curTS->vm->numDaemons));
        goto error;
    }
    
    if (!fivmr_ThreadState_glue(ts,vmt)) {
        LOG(1,("Could not glue thread state; VM is probably exiting."));
        goto error;
    }
    
    fivmr_FlowLog_log(curTS, FIVMR_FLOWLOG_TYPE_THREAD,
                      FIVMR_FLOWLOG_SUBTYPE_CREATE, ts->id);
    prio=fivmr_VMThread_getPriority(curTS,vmt);
    fivmr_ThreadState_setInitPrio(ts,prio);
    fivmr_ThreadState_setStackHeight(ts,fivmr_Thread_stackHeight()-FIVMR_STACK_HEIGHT_HEADROOM);
    
    if (curTS->vm->pool==NULL) {
        th=fivmr_Thread_spawn(vmthread_runner,ts,prio);
    } else {
        th=fivmr_ThreadPool_spawn(curTS->vm->pool,vmthread_runner,ts,prio)->thread;
    }
    if (th==fivmr_ThreadHandle_zero()) {
        /* FIXME currently we leak a thread state... */
        LOG(1,("Could not create a system thread; are you sure you aren't starting "
               "more threads than the system can support?"));
        goto error;
    }

    fivmr_ThreadState_setThread(ts,th);

#if FIVMR_FLOW_LOGGING
    ts->flowbuf = fivmr_malloc(FIVMR_FLOWLOG_BUFFERSIZE);
    memset(ts->flowbuf, 0, FIVMR_FLOWLOG_BUFFERSIZE);
#endif

    return;
error:
    fivmr_ThreadState_checkExit(curTS);
    fivmr_ThreadState_goToJava(curTS);
    fivmr_throwOutOfMemoryError_inJava(curTS);
    fivmr_ThreadState_goToNative(curTS);
}

struct PooledThreadInfo_s {
    fivmr_ThreadPriority parentPriority;
    fivmr_Handle *h;
    bool ok;
    fivmr_Semaphore inited;
    bool done;
};

typedef struct PooledThreadInfo_s PooledThreadInfo;

static void pooledthread_runner(void *arg) {
    fivmr_Handle *h;
    fivmr_VM *vm;
    fivmr_ThreadState *ts;
    PooledThreadInfo *pti;
    bool ok;
    fivmr_ThreadPriority basePrio;
    fivmr_Handle *vmt;
    
    pti=(PooledThreadInfo*)arg;
    h=pti->h;
    vm=h->vm;
    
    ok=false;
    pti->ok=false;
    
    basePrio=
        fivmr_Thread_getPriority(
            fivmr_ThreadHandle_current());
    
    fivmr_assert(fivmr_ThreadPriority_leRT(basePrio,
                                           vm->maxPriority));

    ts=fivmr_ThreadState_new(vm,FIVMR_TSEF_JAVA_HANDSHAKEABLE);
    if (ts==NULL) {
        /* error! */
    } else {
        fivmr_ThreadState_setBasePrio(ts,
                                      basePrio);
        fivmr_ThreadState_setStackHeight(
            ts,
            fivmr_Thread_stackHeight()-FIVMR_STACK_HEIGHT_HEADROOM);
        fivmr_ThreadState_set(ts,NULL);
        vmt=fivmr_VMThread_create(ts,0,false);
        if (vmt!=NULL) {
            fivmr_assert(vmt!=NULL);
            fivmr_assert(ts->curExceptionHandle==NULL);
            if (fivmr_ThreadState_glue(ts,vmt)) {
                ok=true;
                pti->ok=true;
            }
            /* FIXME: We don't necessarily have a Java thread at this point */
            fivmr_FlowLog_log(ts, FIVMR_FLOWLOG_TYPE_THREAD,
                              FIVMR_FLOWLOG_SUBTYPE_CREATE, ts->id);
        }
        
        /* FIXME if an error occurs here we're currently leaking a thread state */
    }
    
    /* let the parent know the status */
    fivmr_Thread_setPriority(
        fivmr_ThreadHandle_current(),
        fivmr_ThreadPriority_max(
            basePrio,
            pti->parentPriority));
    fivmr_Semaphore_up(&pti->inited);
    fivmr_fence();
    pti->done=true;
    
    /* now revert to the priority that we're supposed to be at */
    fivmr_Thread_setPriority(
        fivmr_ThreadHandle_current(),
        basePrio);
    
    LOG(1,("pooled thread %p running with priority %p",fivmr_ThreadHandle_current(),basePrio));
    
    if (ok) {
        fivmr_Thread_setPriority(fivmr_ThreadHandle_current(),
                                 basePrio);

        fivmr_assert(fivmr_ThreadPriority_leRT(fivmr_Thread_getPriority(fivmr_ThreadHandle_current()),
                                               ts->vm->maxPriority));

        fivmr_runRunnable(ts,h);
        if (ts->curExceptionHandle) {
            fivmr_ThreadState_goToJava(ts);
            LOG(1,("Pooled thread threw exception: %s",
                   fivmr_TypeData_forObject(
                       &vm->settings,
                       ts->curExceptionHandle->obj)->name));
            ts->curExceptionHandle=NULL;
            fivmr_ThreadState_goToNative(ts);
        }
        
        fivmr_ThreadState_terminate(ts);
    }
}

void fivmr_VMThread_startPooledThread(fivmr_ThreadState *curTS,
                                      fivmr_ThreadPool *pool,
                                      fivmr_Handle *runnable,
                                      fivmr_ThreadPriority priority) {
    PooledThreadInfo pti;
    
    fivmr_ThreadState_goToJava(curTS);
    runnable=fivmr_newGlobalHandle(curTS,runnable);
    fivmr_ThreadState_goToNative(curTS);

    pti.parentPriority=fivmr_Thread_getPriority(fivmr_ThreadHandle_current());
    pti.h=runnable;
    pti.ok=false;
    fivmr_Semaphore_init(&pti.inited);
    pti.done=false;
    
    fivmr_ThreadPool_spawn(pool,pooledthread_runner,&pti,priority);
    
    fivmr_Semaphore_down(&pti.inited);
    fivmr_fence();
    while (!pti.done) {
        /* this shouldn't happen unless the OS scheduler is sloppy or
           we're on an SMP */
        fivmr_yield();
        fivmr_fence();
    }
    fivmr_fence();

    if (pti.ok) {
        /* ok! */
    } else {
        fivmr_ThreadState_goToJava(curTS);
        fivmr_throwOutOfMemoryError_inJava(curTS);
        fivmr_ThreadState_goToNative(curTS);
    }
}

