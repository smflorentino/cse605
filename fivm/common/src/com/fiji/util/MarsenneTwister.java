/*
 * MarsenneTwister.java
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

package com.fiji.util;

/**
 * Pure Java implementation of the Marsenne Twister pseudo-random number generator (PRNG)
 * due to Nishimura and Matsumoto.  Implemented based on the C version
 * called "MT19937 with initialization improved 2002/1/26".  This is a
 * high quality random number generator that can be used as a replacement
 * for java.util.Random.  This is NOT a drop-in replacement for java.util.Random
 * however, since it does not use locking (i.e. one MarsenneTwister instance
 * should not be used from more than one thread) and does not implement all
 * of that class's methods.  It is also not suitable for security-related
 * applications since the Marsenne Twister algorithm is optimized for Monte
 * Carlo, not security.  The Marsenne Twister algorithm is not the fastest
 * PRNG; depending on how locks are implemented, java.util.Random may actually
 * be faster.  But this class will give you more randomness than java.util.Random.
 * <p>
 * Since the implementation of this Java class is a transliteration of
 * Nishimura and Matsumoto's C code, the following notice is included:
 * <p>
 * Copyright (C) 1997 - 2002, Makoto Matsumoto and Takuji Nishimura,
 * All rights reserved.                          
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * <ol>
 * <li> Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 * <li> Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * <li> The names of its contributors may not be used to endorse or promote 
 *      products derived from this software without specific prior written 
 *      permission.
 * </ol>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public final class MarsenneTwister {
    
    private static final int N = 624;
    private static final int M = 397;
    private static final int MATRIX_A = 0x9908b0df;
    private static final int LOWER_MASK = 0x7fffffff;
    private static final int UPPER_MASK = 0x80000000;

    private final int[] mt = new int[N];
    private int mti;
    private int last;
    private int lastLeft;
    
    public MarsenneTwister() {
        // FIXME this should use the long form of nanoTime and pass it as a key...
        // ... or something.  it's not at all clear if that would be better, since
        // I don't really understand the mixing madness that happens in the
        // withKey routine, and whether or not it's any better than the mixing
        // madness that happens in the integer seed routine.
        this((int)System.currentTimeMillis());
    }
    
    public MarsenneTwister(int seed) {
        mt[0] = seed;
        for (mti=1; mti<N; mti++) {
            mt[mti] = 1812433253 * (mt[mti-1] ^ (mt[mti-1] >>> 30)) + mti;
        }
    }
    
    public MarsenneTwister(MarsenneTwister mt) {
        System.arraycopy(mt.mt,0,
                         mt,0,
                         N);
        mti=mt.mti;
        last=mt.last;
        lastLeft=mt.lastLeft;
    }
    
    private MarsenneTwister(boolean unused) {
    }
    
    public static MarsenneTwister withKey(int[] key) {
        MarsenneTwister result=new MarsenneTwister(19650218);
        int i,j,k;
        i=1; j=0;
        k = (N>key.length ? N : key.length);
        for (;k!=0;k--) {
            result.mt[i] =
                (result.mt[i] ^ ((result.mt[i-1] ^ (result.mt[i-1] >>> 30)) * 1664525))
                + key[j] + j;
            i++; j++;
            if (i>=N) {
                result.mt[0] = result.mt[N-1];
                i=1;
            }
            if (j>=key.length) j=0;
        }
        for (k=N-1; k!=0; k--) {
            result.mt[i] = 
                (result.mt[i] ^ ((result.mt[i-1] ^ (result.mt[i-1] >>> 30)) * 1566083941))
                - i;
            i++;
            if (i>=N) {
                result.mt[0] = result.mt[N-1];
                i=1;
            }
        }
        result.mt[0] = 0x80000000;
        return result;
    }
    
    public static MarsenneTwister withTable(int[] table) {
        MarsenneTwister result=new MarsenneTwister(false);
        System.arraycopy(table,0,
                         result.mt,0,
                         N);
        result.mti=N;
        return result;
    }
    
    public int[] getTable() {
        int[] result=new int[N];
        System.arraycopy(mt,0,
                         result,0,
                         N);
        return result;
    }
    
    private static final int[] mag01 = new int[]{0, MATRIX_A};
    private void generateSlow() {
        int kk;
        for (kk=0;kk<N-M;++kk) {
            int y = (mt[kk]&UPPER_MASK)|(mt[kk+1]&LOWER_MASK);
            mt[kk] = mt[kk+M] ^ (y>>>1) ^ mag01[y&1];
        }
        for (;kk<N-1;kk++) {
            int y = (mt[kk]&UPPER_MASK)|(mt[kk+1]&LOWER_MASK);
            mt[kk] = mt[kk+(M-N)] ^ (y>>>1) ^ mag01[y&1];
        }
        int y = (mt[N-1]&UPPER_MASK)|(mt[0]&LOWER_MASK);
        mt[N-1] = mt[M-1] ^ (y>>>1) ^ mag01[y&1];
        mti = 0;
    }
    
    public int nextInt() {
        int y;
        if (mti>=N) {
            generateSlow();
        }
        y=mt[mti++];
        y^=(y>>>11);
        y^=(y<<7)&0x9d2c5680;
        y^=(y<<15)&0xefc60000;
        y^=(y>>>18);
        return y;
    }
    
    public int nextInt(int end) {
        return (nextInt()&0x7fffffff)%end;
    }
    
    public long nextLong() {
        return (((long)nextInt())&0xffffffffl)
            | ((((long)nextInt())&0xffffffffl)<<32);
    }
    
    public byte nextByte() {
        int value;
        byte result;
        if (lastLeft--==0) {
            value=last=nextInt();
            lastLeft=3;
        } else {
            value=last;
        }
        result=(byte)(value&255);
        last=value>>>8;
        return result;
    }
}

