package com.fiji.fivm.test;

import com.fiji.fivm.Settings;
import com.fiji.fivm.r1.NoInline;
import com.fiji.fivm.r1.NoScopeChecks;

import edu.purdue.scj.VMSupport;
import edu.purdue.scj.BackingStoreID;

import javax.realtime.IllegalAssignmentError;
import javax.safetycritical.ThrowBoundaryError;

class VMSupportCompleteTest {
    private static final int scopeSize = 40960;
    private static final int totalBacking = scopeSize * 4 + 1024;

    private static class StaticRunnable implements Runnable {
	static BackingStoreID outer;
	static StringBuffer sb;

	static {
	    sb = new StringBuffer();
	}

	public StaticRunnable(BackingStoreID outer) {
	    this.outer = outer;
	}

	public void run() {
    	    System.out.println("Running static runnable");
    	    if (VMSupport.areaOf(sb) != VMSupport.getImmortalArea()) {
    		throw new Fail("Static Object not allocated in immortal area");
    	    }
    	}
    }

    private static class DepthCheck implements Runnable {
    	final BackingStoreID parent;
    	final BackingStoreID mine;
    	final int depth;
    	final int total;

    	public DepthCheck(int depth, BackingStoreID parent,
    			  BackingStoreID mine) {
    	    this(depth, depth, parent, mine);
    	}

    	public DepthCheck(int total, int depth, BackingStoreID parent,
    			  BackingStoreID mine) {
    	    this.parent = parent;
    	    this.mine = mine;
    	    this.depth = depth;
    	    this.total = total;
    	}

    	public void run() {
    	    StringBuffer sb = new StringBuffer();
    	    BackingStoreID current = VMSupport.areaOf(sb);
    	    if (current == parent) {
    		throw new Fail("child scope backing store ("
    			       + current + ") matches parent ("
    			       + parent + ") at depth " + depth);
    	    }
	    if (current != mine) {
		throw new Fail("backing store (" + current
			       + ") does not match declared ("
			       + mine + ") at depth " + depth);
	    }
	    if (depth == 0)
		return;
	    BackingStoreID bsid = VMSupport.pushScope(scopeSize);
	    if (bsid == mine) {
		throw new Fail("newly created backing store ("
			       + bsid + ") matches current ("
			       + mine + ") at depth " + depth);
	    }
	    DepthCheck next = new DepthCheck(total, depth - 1, mine, bsid);
	    VMSupport.enter(bsid, next);
	    VMSupport.popScope();
	}
    }

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
	if (outer != VMSupport.getImmortalArea()) {
	    throw new Fail("Did not start in immortal area");
	}

	VMSupport.setTotalBackingStore(Thread.currentThread(), totalBacking);
        VMSupport.allocBackingStoreNow();

	StringBuffer sb = new StringBuffer();
	if (VMSupport.areaOf(sb) != outer) {
	    throw new Fail("Object created in unpredictable area");
	}

	final BackingStoreID bsid1 = VMSupport.pushScope(scopeSize);
	VMSupport.enter(bsid1, new Runnable() {
		public void run() {
		    BackingStoreID inner = VMSupport.getCurrentArea();
		    if (inner != bsid1) {
			throw new Fail("Inner area does not equal declared area");
		    }
		    if (VMSupport.setCurrentArea(outer) != inner) {
			throw new Fail("setCurrentArea returned incorrect BackingStoreID");
		    }
		    StringBuffer sb = new StringBuffer();
		    if (VMSupport.areaOf(sb) != outer
			|| VMSupport.getCurrentArea() != outer) {
			throw new Fail("setCurrentArea failed");
		    }
		    System.out.println("VMSupport.setCurrentArea() seems to work");
		}
	    });
	VMSupport.popScope();

	final BackingStoreID bsid2 = VMSupport.pushScope(scopeSize);
	VMSupport.enter(bsid2, new Runnable() {
		public void run() {
		    System.out.println("Running anonymous runnable");
		    BackingStoreID inner = VMSupport.getCurrentArea();
		    if (inner == outer) {
			throw new Fail("Private memory allocation occurred in global memory");
		    }
		    if (inner != bsid2) {
			throw new Fail("Inner area does not equal declared area");
		    }
		    StaticRunnable sr = new StaticRunnable(outer);
		    if (VMSupport.areaOf(sr) != inner) {
			throw new Fail("Inner allocation not in inner area");
		    }
		    sr.run();
		}
	    });
	VMSupport.popScope();

	BackingStoreID bsid = VMSupport.pushScope(scopeSize);
	System.out.println("Running nested area check");
	DepthCheck dc = new DepthCheck(3, outer, bsid);
	VMSupport.enter(bsid, dc);
	VMSupport.popScope();

	System.out.println("Trying to throw a legal exception from inside a scope");

	bsid = VMSupport.pushScope(scopeSize);
	try {
	    final RuntimeException e = new RuntimeException();
	    VMSupport.enter(bsid, new Runnable() {
		    public void run() {
			System.out.println("Throwing RuntimeException");
			throw e;
		    }
		});
	} catch (RuntimeException e) {
	    System.out.println("Caught exception");
	} finally {
	    VMSupport.popScope();
	}

	System.out.println("Trying to throw an illegal exception from inside a scope");
	bsid = VMSupport.pushScope(scopeSize);
	boolean caught = false;
	try {
	    VMSupport.enter(bsid, new Runnable() {
		    public void run() {
			System.out.println("Throwing RuntimeException");
			throw new RuntimeException();
		    }
		});
	} catch (ThrowBoundaryError e) {
	    caught = true;
	    System.out.println("Caught exception as ThrowBoundaryError");
	} finally {
	    VMSupport.popScope();
	}
	if (!caught) {
	    throw new Fail("Did not generate ThrowBoundaryError");
	}

	bsid = VMSupport.pushScope(scopeSize);
	System.out.println("Trying illegal assignment");
	VMSupport.enter(bsid, new Runnable() {
		public void run() {
		    BackingStoreID bsid2 = VMSupport.pushScope(scopeSize);
		    final Object[] a1 = new Object[2];
		    final StringBuffer sb1 = new StringBuffer();
		    a1[0] = sb1;
		    VMSupport.enter(bsid2, new Runnable() {
			    public void run() {
				Object[] a2 = new Object[2];
				StringBuffer sb2 = new StringBuffer();
				boolean caught = false;
				a2[0] = sb1;
				a2[1] = sb2;
				if (VMSupport.areaOf(a1) == VMSupport.areaOf(sb2)) {
				    throw new Fail("Objects are in the same scope");
				}
				try {
				    a1[1] = sb2;
				} catch (IllegalAssignmentError e) {
				    caught = true;
				    System.out.println("Caught illegal assignment");
				}
				if (!caught) {
				    VMSupport.setCurrentArea(outer);
				    throw new Fail("Failed to catch illegal assignment");
				}
			    }
			});
		    VMSupport.popScope();
		}
	    });
	VMSupport.popScope();

	System.out.println("Trying illegal assignment with @NoScopeChecks");
	bsid = VMSupport.pushScope(scopeSize);
	VMSupport.enter(bsid, new Runnable() {
		public void run() {
		    BackingStoreID bsid2 = VMSupport.pushScope(scopeSize);
		    final Object[] a1 = new Object[2];
		    final StringBuffer sb1 = new StringBuffer();
		    a1[0] = sb1;
		    VMSupport.enter(bsid2, new Runnable() {
			    @NoScopeChecks
			    public void run() {
				Object[] a2 = new Object[2];
				StringBuffer sb2 = new StringBuffer();
				boolean caught = false;
				a2[0] = sb1;
				a2[1] = sb2;
				if (VMSupport.areaOf(a1) == VMSupport.areaOf(sb2)) {
				    throw new Fail("Objects are in the same scope");
				}
				try {
				    a1[1] = sb2;
				} catch (IllegalAssignmentError e) {
				    VMSupport.setCurrentArea(outer);
				    throw new Fail("IllegalAssignmentError was raised");				}
			    }
			});
		    VMSupport.popScope();
		}
	    });
	VMSupport.popScope();

	System.out.println("Setting a legal portal");
	final BackingStoreID bsid3 = VMSupport.pushScope(scopeSize);
	VMSupport.enter(bsid3, new Runnable() {
		public void run() {
		    Object o = new Object();
		    if (VMSupport.areaOf(o) != bsid3) {
			throw new Fail("Object allocated in incorrect area");
		    }
		    VMSupport.setPortal(bsid3, o);
		}
	    });
	VMSupport.popScope();

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

	System.out.println("Trying legal return");
	final BackingStoreID lrbsid = VMSupport.pushScope(scopeSize);
	VMSupport.enter(lrbsid, new Runnable() {
		public void run() {
		    BackingStoreID bsid2 = VMSupport.pushScope(scopeSize);
		    VMSupport.enter(bsid2, new Runnable() {
			    @NoInline
			    public Object allocObject() {
				return new Object[1];
			    }

			    public void run() {
				Object a[] = new Object[1];
				VMSupport.setCurrentArea(lrbsid);
				a[0] = allocObject();
			    }
			});
		    VMSupport.popScope();
		}
	    });
	VMSupport.popScope();

	System.out.println("Trying illegal return");
	final BackingStoreID irbsid = VMSupport.pushScope(scopeSize);
	VMSupport.enter(irbsid, new Runnable() {
		@NoInline
		public Object allocObject() {
		    return new Object[1];
		}

		public void run() {
		    VMSupport.setCurrentArea(outer);
		    Object[] a = new Object[1];
		    VMSupport.setCurrentArea(irbsid);
		    boolean caught = false;
		    try {
			a[0] = allocObject();
		    } catch (IllegalAssignmentError e) {
			caught = true;
			System.out.println("Caught illegal return");
		    }
		    if (!caught) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Failed to catch illegal return");
		    }
		}
	    });
	VMSupport.popScope();

	System.out.println("Triggering OOME");
	bsid = VMSupport.pushScope(scopeSize);
	VMSupport.enter(bsid, new Runnable() {
		public void run() {
		    boolean caught = false;
		    try {
			Object[] a = new Object[scopeSize];
		    } catch (OutOfMemoryError e) {
			caught = true;
			System.out.println("Caught OOME");
		    }
		    if (!caught) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("OOME was not generated");
		    }
		}
	    });
	VMSupport.popScope();

	System.out.println("Checking scope size and available memory");
	final BackingStoreID bsid4 = VMSupport.pushScope(scopeSize);
	VMSupport.enter(bsid4, new Runnable() {
		@NoInline
		public void allocCheck() {
		    Object[] a = new Object[1];
		    if (VMSupport.memoryRemaining(bsid4) == scopeSize
			|| VMSupport.memoryConsumed(bsid4) == 0) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Scope memory not consumed by alloc");
		    }
		}
		public void run() {
		    if (VMSupport.getScopeSize(bsid4) != scopeSize) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Scope size is incorrect: "
				       + VMSupport.getScopeSize(bsid4));
		    }
		    if (VMSupport.memoryConsumed(bsid4) != 0) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Scope memory consumed is incorrect: "
				       + VMSupport.memoryConsumed(bsid4));
		    }
		    if (VMSupport.memoryRemaining(bsid4) != scopeSize) {
			VMSupport.setCurrentArea(outer);
			throw new Fail("Scope memory remaning is incorrect: "
				       + VMSupport.memoryRemaining(bsid4));
		    }
		    allocCheck();
		}
	    });
	VMSupport.popScope();

	if (!Settings.HFGC) {
	    System.out.println("Verifying allocation sizes");
	    final BackingStoreID bsid5 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid5, new Runnable() {
		    public void run() {
			PredictableSize ps = new PredictableSize(1, 1.0, true);
			long estimate = VMSupport.sizeOf(PredictableSize.class);
			long actual = VMSupport.memoryConsumed(bsid5);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ")for PredictableSize");
			}
		    }
		});
	    VMSupport.popScope();
	    final BackingStoreID bsid6 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid6, new Runnable() {
		    public void run() {
			Object[] a = new Object[13];
			long estimate = VMSupport.sizeOfReferenceArray(13);
			long actual = VMSupport.memoryConsumed(bsid6);
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
	    final BackingStoreID bsid7 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid, new Runnable() {
		    public void run() {
			byte[] a = new byte[13];
			long estimate = VMSupport.sizeOfPrimitiveArray(13, byte.class);
			long actual = VMSupport.memoryConsumed(bsid7);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ") for byte array");
			}
		    }
		});
	    VMSupport.popScope();
	    final BackingStoreID bsid8 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid8, new Runnable() {
		    public void run() {
			short[] a = new short[13];
			long estimate = VMSupport.sizeOfPrimitiveArray(13, short.class);
			long actual = VMSupport.memoryConsumed(bsid8);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ") for short array");
			}
		    }
		});
	    VMSupport.popScope();
	    final BackingStoreID bsid9 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid9, new Runnable() {
		    public void run() {
			int[] a = new int[13];
			long estimate = VMSupport.sizeOfPrimitiveArray(13, int.class);
			long actual = VMSupport.memoryConsumed(bsid9);
			if (actual > estimate) {
			    VMSupport.setCurrentArea(outer);
			    throw new Fail("Estimated size (" + estimate
					   + ") less than allocated (" + actual
					   + ") for int array");
			}
		    }
		});
	    VMSupport.popScope();
	    final BackingStoreID bsid10 = VMSupport.pushScope(scopeSize);
	    VMSupport.enter(bsid9, new Runnable() {
		    public void run() {
			double[] a = new double[13];
			long estimate = VMSupport.sizeOfPrimitiveArray(13, double.class);
			long actual = VMSupport.memoryConsumed(bsid10);
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
