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


