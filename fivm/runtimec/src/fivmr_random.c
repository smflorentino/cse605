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

