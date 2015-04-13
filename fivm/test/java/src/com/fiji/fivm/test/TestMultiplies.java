/*
 * TestNoGCMain.java
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

import com.fiji.fivm.r1.*;

public class TestMultiplies {
    public static void main(String[] v) {
        for (int i=0;i<v.length;i+=2) {
            int x=Integer.parseInt(v[i+0]);
            long y=Long.parseLong(v[i+1]);
        
            System.out.println("Multiplying int "+x+":");

            System.out.println(x*-20);
            System.out.println(x*-19);
            System.out.println(x*-18);
            System.out.println(x*-17);
            System.out.println(x*-16);
            System.out.println(x*-15);
            System.out.println(x*-14);
            System.out.println(x*-13);
            System.out.println(x*-12);
            System.out.println(x*-11);
            System.out.println(x*-10);
            System.out.println(x*-9);
            System.out.println(x*-8);
            System.out.println(x*-7);
            System.out.println(x*-6);
            System.out.println(x*-5);
            System.out.println(x*-4);
            System.out.println(x*-3);
            System.out.println(x*-2);
            System.out.println(x*-1);
            System.out.println(x*0);
            System.out.println(x*1);
            System.out.println(x*2);
            System.out.println(x*3);
            System.out.println(x*4);
            System.out.println(x*5);
            System.out.println(x*6);
            System.out.println(x*7);
            System.out.println(x*8);
            System.out.println(x*9);
            System.out.println(x*10);
            System.out.println(x*11);
            System.out.println(x*12);
            System.out.println(x*13);
            System.out.println(x*14);
            System.out.println(x*15);
            System.out.println(x*16);
            System.out.println(x*17);
            System.out.println(x*18);
            System.out.println(x*19);
            System.out.println(x*20);
        
            System.out.println("Multiplying long "+y+":");
        
            System.out.println(y*-20);
            System.out.println(y*-19);
            System.out.println(y*-18);
            System.out.println(y*-17);
            System.out.println(y*-16);
            System.out.println(y*-15);
            System.out.println(y*-14);
            System.out.println(y*-13);
            System.out.println(y*-12);
            System.out.println(y*-11);
            System.out.println(y*-10);
            System.out.println(y*-9);
            System.out.println(y*-8);
            System.out.println(y*-7);
            System.out.println(y*-6);
            System.out.println(y*-5);
            System.out.println(y*-4);
            System.out.println(y*-3);
            System.out.println(y*-2);
            System.out.println(y*-1);
            System.out.println(y*0);
            System.out.println(y*1);
            System.out.println(y*2);
            System.out.println(y*3);
            System.out.println(y*4);
            System.out.println(y*5);
            System.out.println(y*6);
            System.out.println(y*7);
            System.out.println(y*8);
            System.out.println(y*9);
            System.out.println(y*10);
            System.out.println(y*11);
            System.out.println(y*12);
            System.out.println(y*13);
            System.out.println(y*14);
            System.out.println(y*15);
            System.out.println(y*16);
            System.out.println(y*17);
            System.out.println(y*18);
            System.out.println(y*19);
            System.out.println(y*20);
        }
        
        System.out.println("Done!");
    }
}

