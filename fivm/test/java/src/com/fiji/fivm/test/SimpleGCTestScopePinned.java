/*
 * SimpleGCTestScopePinned.java
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
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.Magic;

import static com.fiji.fivm.test.Util.*;
import com.fiji.fivm.test.Fail;

public class SimpleGCTestScopePinned implements Runnable {
    private Pointer area;
    private int iterations;
    private int arrayLength;
    private int sleepTime;

    public SimpleGCTestScopePinned(Pointer area, int iterations,
				   int arrayLength, int sleepTime) {
	this.area = area;
	this.iterations = iterations;
	this.arrayLength = arrayLength;
	this.sleepTime = sleepTime;
    }

    public void run() {
	Integer[][] array = new Integer[1][];

	if (MemoryAreas.areaOf(array) != area) {
	    throw new Fail("array not created in scoped memory");
	}

	MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());

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
	    try {
		Thread.sleep(sleepTime);
	    } catch (InterruptedException e) {
	    }
	    for (int i=0;i<array[0].length;++i) {
		ensureEqual(new Integer(i),array[0][i]);
	    }
	    System.out.println("   Array verified; waiting.");
	    try {
		Thread.sleep(sleepTime);
	    } catch (InterruptedException e) {
	    }
	}
    }

    public static void main(String[] argv) throws Throwable {
	if (argv.length!=4) {
	    System.err.println("This test requires four arguments: <iterations> <array length> <sleep time> <shared>");
	    System.exit(1);
	}

	MemoryAreas.allocScopeBacking(Magic.curThreadState(), 1024*1024);
	Pointer area = MemoryAreas.alloc(512*1024,
					 Integer.parseInt(argv[3])!=0, null);
	MemoryAreas.enter(area, new SimpleGCTestScopePinned(area,
							    Integer.parseInt(argv[0]),
							    Integer.parseInt(argv[1]),
							    Integer.parseInt(argv[2])));
	MemoryAreas.pop(area);
	MemoryAreas.free(area);

	System.out.println("That seems to have worked.");
    }
}


