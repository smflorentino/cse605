/*
 * CloneTest3.java
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

public class CloneTest3 {
    static class C implements Cloneable {
        public C clone() {
            try {
                return (C)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new Error(e);
            }
        }
        
        int a;
        int b;
        int c;
        int d;
        int e;
        int f;
        int g;
        
        public C(int a,int b,int c,int d,int e,int f,int g) {
            this.a=a;
            this.b=b;
            this.c=c;
            this.d=d;
            this.e=e;
            this.f=f;
            this.g=g;
        }
    }
    
    public static void main(String[] v) throws Throwable {
        C o1=new C(Integer.parseInt(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2]),Integer.parseInt(v[3]),Integer.parseInt(v[4]),Integer.parseInt(v[5]),Integer.parseInt(v[6]));
        System.out.println("o1 = "+o1);
        System.out.println("o1.a = "+o1.a);
        System.out.println("o1.b = "+o1.b);
        System.out.println("o1.c = "+o1.c);
        System.out.println("o1.d = "+o1.d);
        System.out.println("o1.e = "+o1.e);
        System.out.println("o1.f = "+o1.f);
        System.out.println("o1.g = "+o1.g);
        C o2=o1.clone();
        System.out.println("o2 = "+o2);
        System.out.println("o2.a = "+o2.a);
        System.out.println("o2.b = "+o2.b);
        System.out.println("o2.c = "+o2.c);
        System.out.println("o2.d = "+o2.d);
        System.out.println("o2.e = "+o2.e);
        System.out.println("o2.f = "+o2.f);
        System.out.println("o2.g = "+o2.g);
        Util.ensureEqual(o1.getClass(),o2.getClass());
        Util.ensureEqual(o1.a,o2.a);
        Util.ensureEqual(o1.b,o2.b);
        Util.ensureEqual(o1.c,o2.c);
        Util.ensureEqual(o1.d,o2.d);
        Util.ensureEqual(o1.e,o2.e);
        Util.ensureEqual(o1.f,o2.f);
        Util.ensureEqual(o1.g,o2.g);
        Util.ensureNotEqual(o1,o2);
        System.out.println("That worked.");
    }
}


