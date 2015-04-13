package com.fiji.fivm.test;

import edu.purdue.scj.VMSupport;
import edu.purdue.scj.BackingStoreID;

import com.fiji.fivm.r1.NoInline;

import com.fiji.fivm.Settings;

import javax.realtime.IllegalAssignmentError;

class VMSupportMemoryTest {
    private static final int scopeSize = 40960;
    private static final int totalBacking = scopeSize * 4 + 1024;

    private static class PredictableSize {
	public int i;
	public double d;
	public boolean b;

	PredictableSize(int i, double d, boolean b) {
	    this.i = i;
	    this.d = d;
	    this.b = b;
	}
    }


    public static void main(String[] args) {
	final BackingStoreID outer = VMSupport.getCurrentArea();
	BackingStoreID bsid;

	VMSupport.setTotalBackingStore(Thread.currentThread(), totalBacking);
        VMSupport.allocBackingStoreNow();

	System.out.println("Setting a legal portal");
	final BackingStoreID bsid1 = VMSupport.pushScope(scopeSize);
	VMSupport.enter(bsid1, new Runnable() {
		public void run() {
		    Object o = new Object();
		    if (VMSupport.areaOf(o) != bsid1) {
			throw new Fail("Object allocated in incorrect area");
		    }
		    VMSupport.setPortal(bsid1, o);
		}
	    });
	VMSupport.popScope();

	boolean caught = false;
	System.out.println("Setting an illegal portal");
	bsid = VMSupport.pushScope(scopeSize);
	try {
	    Object o = new Object();
	    if (VMSupport.areaOf(o) == bsid) {
		throw new Fail("Object allocated in incorrect area");
	    }
	    VMSupport.setPortal(bsid, o);
	} catch (IllegalAssignmentError e) {
	    caught = true;
	} finally {
	    VMSupport.popScope();
	}
	if (!caught) {
	    throw new Fail("setPortal accepted invalid assignment");
	}

		System.out.println("Checking scope size and available memory");
	final BackingStoreID bsid2 = VMSupport.pushScope(scopeSize);
	VMSupport.enter(bsid2, new Runnable() {
		@NoInline
		public void allocCheck() {
		    Object[] a = new Object[1];
		    if (VMSupport.memoryRemaining(bsid2) == scopeSize
			|| VMSupport.memoryConsumed(bsid2) == 0) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Scope memory not consumed by alloc");
		    }
		}
		public void run() {
		    if (VMSupport.getScopeSize(bsid2) != scopeSize) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Scope size is incorrect: "
				       + VMSupport.getScopeSize(bsid2));
		    }
		    if (VMSupport.memoryConsumed(bsid2) != 0) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Scope memory consumed is incorrect: "
				       + VMSupport.memoryConsumed(bsid2));
		    }
		    if (VMSupport.memoryRemaining(bsid2) != scopeSize) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Scope memory remaning is incorrect: "
				       + VMSupport.memoryRemaining(bsid2));
		    }
		    allocCheck();
		}
	    });
	VMSupport.popScope();

	if (!Settings.HFGC) {
	    System.out.println("Verifying allocation sizes");
	    final BackingStoreID bsid3 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid3, new Runnable() {
		    public void run() {
			PredictableSize ps = new PredictableSize(1, 1.0, true);
			long estimate = VMSupport.sizeOf(PredictableSize.class);
			long actual = VMSupport.memoryConsumed(bsid3);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ")for PredictableSize");
			}
		    }
		});
	    VMSupport.popScope();
	    final BackingStoreID bsid4 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid4, new Runnable() {
		    public void run() {
			Object[] a = new Object[13];
			long estimate = VMSupport.sizeOfReferenceArray(13);
			long actual = VMSupport.memoryConsumed(bsid4);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ") for ref array");
			}
		    }
		});
	    VMSupport.popScope();
	    /* FIXME: We should test all of the primitive types here */
	    final BackingStoreID bsid5 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid, new Runnable() {
		    public void run() {
			byte[] a = new byte[13];
			long estimate = VMSupport.sizeOfPrimitiveArray(13, byte.class);
			long actual = VMSupport.memoryConsumed(bsid5);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ") for byte array");
			}
		    }
		});
	    VMSupport.popScope();
	    final BackingStoreID bsid6 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid6, new Runnable() {
		    public void run() {
			short[] a = new short[13];
			long estimate = VMSupport.sizeOfPrimitiveArray(13, short.class);
			long actual = VMSupport.memoryConsumed(bsid6);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ") for short array");
			}
		    }
		});
	    VMSupport.popScope();
	    final BackingStoreID bsid7 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid7, new Runnable() {
		    public void run() {
			int[] a = new int[13];
			long estimate = VMSupport.sizeOfPrimitiveArray(13, int.class);
			long actual = VMSupport.memoryConsumed(bsid7);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ") for int array");
			}
		    }
		});
	    VMSupport.popScope();
	    final BackingStoreID bsid8 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid8, new Runnable() {
		    public void run() {
			double[] a = new double[13];
			long estimate = VMSupport.sizeOfPrimitiveArray(13, double.class);
			long actual = VMSupport.memoryConsumed(bsid8);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ") for double array");
			}
		    }
		});
	    VMSupport.popScope();
	}

	System.out.println("edu.purdue.scj.VMSupport interface seems to work");
    }
}
