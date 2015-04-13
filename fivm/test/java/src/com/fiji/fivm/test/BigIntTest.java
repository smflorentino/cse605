/*
 * BigIntTest.java
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

package com.fiji.fivm.test;

import java.math.*;

public class BigIntTest {
    public static void main(String[] v) {
        BigInteger a=new BigInteger(v[0]);
        BigInteger b=new BigInteger(v[1]);
        BigInteger c=new BigInteger(v[2]);
        BigInteger[] all=new BigInteger[]{a,b,c};
        for (BigInteger x : all) {
            System.out.println("Testing unaries on "+x+":");
            System.out.println(x.abs());
            System.out.println(x.doubleValue());
            System.out.println(x.clearBit(0));
            System.out.println(x.clearBit(1));
            System.out.println(x.clearBit(2));
            System.out.println(x.flipBit(0));
            System.out.println(x.flipBit(1));
            System.out.println(x.flipBit(2));
            System.out.println(x.floatValue());
            System.out.println(x.bitCount());
            System.out.println(x.bitLength());
            System.out.println(x.getLowestSetBit());
            //System.out.println(x.hashCode()); // doesn't match JDK
            System.out.println(x.longValue());
            System.out.println(x.negate());
            System.out.println(x.not());
            System.out.println(x.setBit(0));
            System.out.println(x.setBit(1));
            System.out.println(x.setBit(2));
            System.out.println(x.shiftLeft(0));
            System.out.println(x.shiftLeft(1));
            System.out.println(x.shiftLeft(2));
            System.out.println(x.shiftRight(0));
            System.out.println(x.shiftRight(1));
            System.out.println(x.shiftRight(2));
            System.out.println(x.signum());
            System.out.println(x.testBit(0));
            System.out.println(x.testBit(1));
            System.out.println(x.testBit(2));
            System.out.println(x.toString());
            System.out.println(x.toString(16));
            System.out.println(x.toString(8));
            System.out.println(x.pow(0));
            System.out.println(x.pow(1));
            System.out.println(x.pow(2));
            System.out.println(x.pow(3));
            Util.ensure(x.equals(x));
            Util.ensure(x.equals(new BigInteger(x.toString())));
            for (BigInteger y : all) {
                System.out.println("Testing binaries on "+x+", "+y+":");
                System.out.println(x.add(y));
                System.out.println(x.and(y));
                System.out.println(x.andNot(y));
                System.out.println(x.divide(y));
                BigInteger[] dar=x.divideAndRemainder(y);
                Util.ensureEqual(dar.length,2);
                System.out.println(dar[0]);
                System.out.println(dar[1]);
                System.out.println(x.gcd(y));
                System.out.println(x.max(y));
                System.out.println(x.min(y));
                System.out.println(x.mod(y));
                try {
                    System.out.println(x.modInverse(y));
                } catch (Throwable e) {
                    System.out.println(e.getClass());
                }
                System.out.println(x.multiply(y));
                System.out.println(x.or(y));
                System.out.println(x.remainder(y));
                System.out.println(x.subtract(y));
                System.out.println(x.xor(y));
                for (BigInteger z : all) {
                    System.out.println("Testing ternaries on "+x+", "+y+", "+z+":");
                    System.out.println(x.modPow(y,z));
                }
            }
        }
    }
}

