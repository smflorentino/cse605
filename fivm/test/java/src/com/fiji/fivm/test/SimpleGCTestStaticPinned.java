/*
 * SimpleGCTestStaticPinned.java
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

import com.fiji.fivm.r1.MemoryAreas;

import static com.fiji.fivm.test.Util.*;
import com.fiji.fivm.test.Fail;

public class SimpleGCTestStaticPinned {
    public static Integer[][] array=new Integer[1][];

    public static void main(String[] argv) throws Throwable {
	if (argv.length!=3) {
	    System.err.println("This test requires three arguments: <iterations> <array length> <sleep time>");
	    System.exit(1);
	}
	
	if (MemoryAreas.areaOf(array)!=MemoryAreas.getImmortalArea()) {
	    throw new Fail("array not created in immortal memory");
	}
	int iterations = Integer.parseInt(argv[0]);
	int arrayLength = Integer.parseInt(argv[1]);
	int sleepTime = Integer.parseInt(argv[2]);
	
	System.out.println("iterations = "+iterations);
	System.out.println("arrayLength = "+arrayLength);
	System.out.println("sleepTime = "+sleepTime);
	
	System.out.println("total memory = "+Runtime.getRuntime().totalMemory());
	System.out.println("free memory = "+Runtime.getRuntime().freeMemory());
	System.out.println("max memory = "+Runtime.getRuntime().maxMemory());
	
	for (int j=1;iterations<0 || j<=iterations;++j) {
	    System.out.println("Executing iteration #"+j);
	    array[0]=new Integer[arrayLength];
	    for (int i=0;i<array[0].length;++i) {
		array[0][i]=new Integer(i);
	    }
	    System.out.println("   Array populated; waiting.");
	    Thread.sleep(sleepTime);
	    for (int i=0;i<array[0].length;++i) {
		ensureEqual(new Integer(i),array[0][i]);
	    }
	    System.out.println("   Array verified; waiting.");
	    Thread.sleep(sleepTime);
	}
	
	System.out.println("That seems to have worked.");
    }
}


