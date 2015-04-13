/*
 * IntUtil.java
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

package com.fiji.fivm;

import java.math.BigInteger;
import java.util.Arrays;

public class IntUtil {
    private IntUtil() {}
    
    public static boolean uLessThan(int a,int b) {
        if (a==b || (b>=0 && a<0)) {
            return false;
        } else if (b<0 && a>=0) {
            return true;
        } else if ((b>=0 && a>=0) || (b<0 && a<0)) {
            return a<b;
        } else {
            throw new Error();
        }
    }
    
    public static boolean uLessThan(long a,long b) {
        if (a==b || (b>=0 && a<0)) {
            return false;
        } else if (b<0 && a>=0) {
            return true;
        } else if ((b>=0 && a>=0) || (b<0 && a<0)) {
            return a<b;
        } else {
            throw new Error();
        }
    }
    
    public static int countOneBits(int x) {
	int result=0;
	while (x!=0) {
	    ++result;
	    
	    // this eliminates the lowest-order 1 bit
	    x&=(x-1);
	}
	return result;
    }
    
    public static int countOneBits(long x) {
	int result=0;
	while (x!=0) {
	    ++result;
	    
	    // this eliminates the lowest-order 1 bit
	    x&=(x-1);
	}
	return result;
    }
    
    public static int logBase2(int x) {
	int result=-1;
	while (x!=0) {
	    x/=2;
	    result++;
	}
	return result;
    }
    
    public static int logBase2(long x) {
	int result=-1;
	while (x!=0) {
	    x/=2;
	    result++;
	}
	return result;
    }
    
    public static int firstSetBit(int x) {
        int result=0;
        while ((x&1)!=0) {
            x>>=1;
            result++;
        }
        return result;
    }
    
    public static int udiv(int a,int b) {
        return (int)((a&0xffffffffl)/(b&0xffffffffl));
    }
    
    public static int umod(int a,int b) {
        return (int)((a&0xffffffffl)%(b&0xffffffffl));
    }
    
    public static long udiv(long a,long b) {
        BigInteger ba=new BigInteger(Long.toHexString(a),16);
        BigInteger bb=new BigInteger(Long.toHexString(b),16);
        return ba.divide(bb).longValue();
    }
    
    public static long umod(long a,long b) {
        BigInteger ba=new BigInteger(Long.toHexString(a),16);
        BigInteger bb=new BigInteger(Long.toHexString(b),16);
        return ba.remainder(bb).longValue();
    }
    
    public static int bitSetArrayLength(int nbits) {
        return (nbits+31)>>5;
    }

    public static int[] newBitSet(int nbits) {
        return new int[bitSetArrayLength(nbits)];
    }
    
    public static boolean setBit(int[] vec,int idx) {
        int o=vec[idx>>5];
        int n=o|(1<<(idx&31));
        vec[idx>>5]=n;
        return o!=n;
    }
    
    public static boolean clrBit(int[] vec,int idx) {
        int o=vec[idx>>5];
        int n=o&~(1<<(idx&31));
        vec[idx>>5]=n;
        return o!=n;
    }
    
    public static boolean setBit(int[] vec,int idx,boolean value) {
        if (value) {
            return setBit(vec,idx);
        } else {
            return clrBit(vec,idx);
        }
    }
    
    public static boolean bit(int[] vec,int idx) {
        return (vec[idx>>5]&(1<<(idx&31)))!=0;
    }
    
    public static boolean or(int[] vec1,int[] vec2) {
        boolean result=false;
        for (int i=0;i<vec1.length;++i) {
            int o=vec1[i];
            int n=o|vec2[i];
            vec1[i]=n;
            result|=(o!=n);
        }
        return result;
    }
    
    /**
     * equivalent to:
     * <pre>
     * for (int i=0;i<nbits;++i) {
     *    if (bit(vec1,i) &amp;&amp; setBit(vec2,i)) {
     *        setBit(vec3,i);
     *    }
     * }
     * </pre>
     */
    public static void cascade(int[] vec1,int[] vec2,int[] vec3) {
        for (int i=0;i<vec1.length;++i) {
            int o=vec2[i];
            int n=o|vec1[i];
            vec2[i]=n;
            vec3[i]|=o^n;
        }
    }
    
    public static boolean and(int[] vec1,int[] vec2) {
        boolean result=false;
        for (int i=0;i<vec1.length;++i) {
            int o=vec1[i];
            int n=o&vec2[i];
            vec1[i]=n;
            result|=o!=n;
        }
        return result;
    }
    
    public static boolean andNot(int[] vec1,int[] vec2) {
        boolean result=false;
        for (int i=0;i<vec1.length;++i) {
            int o=vec1[i];
            int n=o&~vec2[i];
            vec1[i]=n;
            result|=o!=n;
        }
        return result;
    }
    
    public static void canonicalize(int[] vec,int nbits) {
        assert vec.length==(nbits+31)>>5;
        int lastOff=nbits&31;
        if (lastOff!=0) {
            vec[nbits>>5]&=(1<<lastOff)-1;
        }
    }
    
    public static void not(int[] vec,int nbits) {
        for (int i=0;i<vec.length;++i) {
            vec[i]=~vec[i];
        }
        canonicalize(vec,nbits);
    }
    
    public static void clear(int[] vec) {
        Arrays.fill(vec,0);
    }
    
    public static int[] copy(int[] vec) {
        int[] result=new int[vec.length];
        System.arraycopy(vec,0,
                         result,0,
                         vec.length);
        return result;
    }
    
    public static boolean setsEqual(int[] vec1,int[] vec2) {
        return Arrays.equals(vec1,vec2);
    }
    
    public static int setHashCode(int[] vec,int nbits) {
        return Arrays.hashCode(vec)+nbits;
    }
    
    public static int nextSetBit(int[] vec,int idx,int nbits) {
        if (idx>=nbits) return idx;
        for (;;) {
            while (vec[idx>>5]==0) {
                idx=(idx+32)&~31;
                if (idx>=nbits) return idx;
            }
            int val=vec[idx>>5];
            int off=1<<(idx&31);
            while (off!=0) {
                if ((val&off)!=0) return idx;
                off<<=1;
                idx++;
                if (idx>=nbits) return idx;
            }
        }
    }
    
    public static int cardinality(int[] vec,int nbits) {
        int result=0;
        for (int i=0;i<vec.length;++i) {
            result+=countOneBits(vec[i]);
        }
        return result;
    }
    
    public static boolean isEmpty(int[] vec,int nbits) {
        for (int i=0;i<vec.length;++i) {
            if (vec[i]!=0) {
                return false;
            }
        }
        return true;
    }
}

