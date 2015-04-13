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

import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.test.Util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.lang.reflect.Array;

import javax.realtime.IllegalAssignmentError;
import com.fiji.fivm.ThreadPriority;
import com.fiji.fivm.Settings;

import com.fiji.fivm.r1.*;

/**
 * Most basic tests of VM functionality, with nothing to stress the GC.
 */
public class TestNoGCMain {
    public static enum Mode {
        SMALL_TEST, LARGE_TEST;
    }
    
    public static final String testFile = "__fivmtest.out";
    public static Mode mode;
    
    public static void main(String[] argv) throws Throwable {
	if (argv.length!=0) {
	    System.err.println("This test does not take command-line arguments.  Please rerun without any arguments.");
	    System.exit(1);
	}
	
        mode=Mode.LARGE_TEST;
        doit();
    }
    
    public static void doit() throws Throwable {
	System.out.println("Make sure you assert the contents of this output!");
	
	simpleTests();
	testPrivateMethod();
	testStackTrace();
	testDeepCall();
	testFileIO();
	testHashtable();
	testRandomAccessFile();
	testNewInstance();
	testFloat();
        testFiatSimple();
	testInstanceof();
	testCheckedCast();
	testInterfaceCall();
        testClassWeDelete();
        testClassWeDelete2();
        testClassWeDelete3();
	testExceptions();
	testIntArithmetic();
	testArrayStore();
	testArrayCopy();
	testSysProps();
	testArrayReflect();
        testOOME();
        testNullAccess();
        testStackOverflow1();
        testStackOverflow1();
        testStackOverflow2();
        testStackOverflow2();
        testStackOverflow1();
	testThreads();
        testMonitors();
	testStackAlloc();
	testCAS();
        testFiatAwesome();

	// note that we only delete on successful run.
	deleteStuff();
	
	System.out.println("FIXME: implement more tests!");
	
	System.out.println("SUCCESS (I think)");
    }
    
    public static void deleteStuff() throws Throwable {
	new File(testFile).delete();
	ensure(!new File(testFile).exists());
    }

    public static void simpleTests() {
	System.out.println("Doing simple tests...");
	
	ensureEqual(42,42);
	ensureNotEqual(42,43);
        ensureEqual(null,null);
        ensureNotEqual(null,new Object());
        ensureNotEqual(new Object(),null);

        try {
            ensureNotEqual(42,42);
            throw new Fail("should not get here");
        } catch (Fail e) {}
        
        try {
            ensureEqual(42,43);
            throw new Fail("should not get here");
        } catch (Fail e) {}
        
        try {
            ensureNotEqual(null,null);
            throw new Fail("should not get here");
        } catch (Fail e) {}
        
        try {
            ensureEqual(null,new Object());
            throw new Fail("should not get here");
        } catch (Fail e) {}
        
        try {
            ensureEqual(new Object(),null);
            throw new Fail("should not get here");
        } catch (Fail e) {}
        
        System.out.println("the simple tests didn't fail");
    }
    
    public static void testPrivateMethod() {
	System.out.println("testing private methods...");
	new Superclass().callPrivateMethod();
	new Subclass().callPrivateMethod();
    }
    
    private static void throwException() throws Exception {
	throw new Exception("just a test");
    }

    private static void callMethodThatThrowsException() throws Exception {
	throwException();
    }
    
    public static void testStackTrace() {
	System.out.println("Throwing an exception and trying to dump stack trace...");
	
	try {
	    callMethodThatThrowsException();
	} catch (Exception e) {
	    e.printStackTrace();
            System.out.println("That worked.");
	}
    }
    
    public static void testDeepCall() { testDeepCall1(); }
    static void testDeepCall1() { testDeepCall2(); }
    static void testDeepCall2() { testDeepCall3(); }
    static void testDeepCall3() { testDeepCall4(); }
    static void testDeepCall4() { testDeepCall5(); }
    static void testDeepCall5() { testDeepCall6(); }
    static void testDeepCall6() { testDeepCall7(); }
    static void testDeepCall7() { testDeepCall8(); }
    static void testDeepCall8() { testDeepCall9(); }
    static void testDeepCall9() { testDeepCall10(); }
    
    static void testDeepCall10() {
	System.out.println("In deep call!");
	new Error("indeed").printStackTrace();
    }
    
    public static void testFileIO() throws Throwable {
	System.out.println("Opening a file...");
	
	deleteStuff();
	
	try {
	    PrintWriter fout=new PrintWriter(new FileOutputStream(testFile));
	    try {
		fout.println("this is just a test");
	    } finally {
		fout.close();
	    }
		
	    BufferedReader fin=new BufferedReader(
		new InputStreamReader(new FileInputStream(testFile)));
	    try {
		ensureEqual(fin.readLine(),"this is just a test");
	    } finally {
		fin.close();
	    }
	    
	    System.out.println("That worked.");
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    public static void testHashtable() {
        // this test is meant to root out bugs in how we compile the Hashtable.
        // hence the use of bad keys.  it would be good to also have a test that
        // uses keys that don't have a broken hashCode().  but even then, this
        // test should remain, unchanged, as it is known to catch bugs.
        
	System.out.println("Testing hashtable...");
	
	final int num=1000;
	
	class BadKey {
	    public int hashCode() {
		return 0;
	    }
	    public boolean equals(Object other) {
		return this==other;
	    }
	}
	
	Hashtable< BadKey, Object > h=new Hashtable< BadKey, Object >();
	
	for (int i=0;i<num;++i) {
	    h.put(new BadKey(),new Object());
	}
	
	ensureEqual(h.size(),num);
	
	for (Map.Entry< BadKey, Object > e : h.entrySet()) {
	    ensure(e.getKey().getClass() == BadKey.class);
	    ensure(e.getValue().getClass() == Object.class);
	}
	
	System.out.println("That worked.");
    }
    
    public static void testRandomAccessFile() throws Throwable {
	System.out.println("Testing random access file...");
	
	deleteStuff();
	
	RandomAccessFile raf=new RandomAccessFile(testFile,"rwd");
	try {
	    raf.writeBytes("hello");
	    raf.seek(0);
	    ensureEqual(raf.read(),(int)'h');
	    ensureEqual(raf.read(),(int)'e');
	    ensureEqual(raf.read(),(int)'l');
	    ensureEqual(raf.read(),(int)'l');
	    ensureEqual(raf.read(),(int)'o');
	    ensureEqual(raf.read(),-1);
	    System.out.println("That worked.");
	} finally {
	    raf.close();
	}
    }
    
    public static void testNewInstance() throws Throwable {
	System.out.println("Testing Class.newInstance...");
	ensureEqual(SomeClass.class.newInstance().number,42);
	System.out.println("That worked.");
    }
    
    static float a=1.5f;
    static float b=1.5f;
    static double c=4.0;
    static double d=9.0;
    
    static float nan=Float.NaN;
    static float pinf=Float.POSITIVE_INFINITY;
    static float ninf=Float.NEGATIVE_INFINITY;
    public static void testFloat() {
	ensure(Math.abs(a-b)<0.00001);
        ensure(a<pinf);
        ensure(b<pinf);
        ensure(a>ninf);
        ensure(b>ninf);
        ensure(a<=b);
        ensure(a>=b);
        ensure(a==b);
        ensure(!(a!=b));
        ensure(!(a<b));
        ensure(!(a>b));
        ensure(a<=pinf);
        ensure(b<=pinf);
        ensure(a>=ninf);
        ensure(b>=ninf);
        ensure(!(a<nan));
        ensure(!(a<=nan));
        ensure(!(a>nan));
        ensure(!(a>=nan));
        ensure(a!=nan);
        ensure(nan!=nan);
        ensure(nan!=pinf);
        ensure(nan!=ninf);
        ensureEqual(""+1.0,"1.0");
        ensureEqual(""+(-1.0),"-1.0");
        ensureEqual(Math.sqrt(4.0),2.0);
        ensureEqual(Math.sqrt(9.0),3.0);
        ensureEqual(Math.sqrt(c),2.0);
        ensureEqual(Math.sqrt(d),3.0);
	System.out.println("Floating point arithmetic seems to not be completely broken.");
    }

    @NoInline
    static boolean doInstanceofSubclass(Object o) {
        return o instanceof Subclass;
    }
    
    @NoInline
    static boolean doInstanceofSuperclass(Object o) {
        return o instanceof Superclass;
    }
    
    @NoInline
    static boolean doInstanceofObject(Object o) {
        return o instanceof Object;
    }
    
    @NoInline
    static boolean doInstanceofSomeInterface(Object o) {
        return o instanceof SomeInterface;
    }
    
    public static void testInstanceof() {
	ensure(new Subclass() instanceof Subclass);
	ensure(new Subclass() instanceof Superclass);
	ensure(new Subclass() instanceof Object);
	ensure(new Subclass() instanceof SomeInterface);
	ensure(!(new Superclass() instanceof Subclass));
	ensure(new Superclass() instanceof Superclass);
	ensure(new Superclass() instanceof Object);
	ensure(!(new Superclass() instanceof SomeInterface));
	ensure(doInstanceofSubclass(new Subclass()));
	ensure(doInstanceofSuperclass(new Subclass()));
	ensure(doInstanceofObject(new Subclass()));
	ensure(doInstanceofSomeInterface(new Subclass()));
	ensure(!doInstanceofSubclass(new Superclass()));
	ensure(doInstanceofSuperclass(new Superclass()));
	ensure(doInstanceofObject(new Superclass()));
	ensure(!doInstanceofSomeInterface(new Superclass()));
	System.out.println("Instanceof seems to work.");
    }
    
    public static void testCheckedCast() {
	try {
	    @SuppressWarnings("unused")
            Subclass o=(Subclass)new Superclass();
	    ensure(false);
	} catch (ClassCastException e) {
	    System.out.println("Casting seems to work.");
	}
    }
    
    @NoInline
    static int callSomeInterfaceMethod(SomeInterface o) {
        return o.someInterfaceMethod();
    }
    
    public static void testInterfaceCall() {
	ensureEqual(new Subclass().someInterfaceMethod(),24);
	ensureEqual(InterfaceEvil.doEvil(new Subclass()).someInterfaceMethod(),24);
	try {
	    ensureEqual(InterfaceEvil.doEvil(new Superclass()).someInterfaceMethod(),24);
	    ensure(false);
	} catch (IncompatibleClassChangeError e) {
	    System.out.println("Error detected correctly.");
	}
        ensureEqual(new SomeInterfaceImpl().someInterfaceMethod(),59);
        ensureEqual(callSomeInterfaceMethod(new Subclass()),24);
        ensureEqual(callSomeInterfaceMethod(new SomeInterfaceImpl()),59);
        System.out.println("Interface calls seem to work.");
    }
    
    public static void testClassWeDelete() {
	try {
	    new ClassWeDelete();
	    ensure(false);
	} catch (NoClassDefFoundError e) {
	    e.printStackTrace();
	    System.out.println("well, that worked out just fine.");
	}
    }
    
    private static ClassWeDelete passThrough(ClassWeDelete o) {
	return o;
    }
    
    public static void testClassWeDelete2() {
	ensure(passThrough(null)==null);
	System.out.println("hooray!");
    }
    
    public static void testClassWeDelete3() {
	try {
	    ensure(ClassThatCantBeLoaded.lub(false,null,null)==null);
            if (!Settings.CLASSLOADING) {
                ensure(false);
            } else {
                System.out.println("We don't have a verifier right now, so this is fine.");
            }
	} catch (NoClassDefFoundError e) {
            if (Settings.CLASSLOADING) {
                // shouldn't get here, so throw
                throw e;
            } else {
                e.printStackTrace();
                System.out.println("well, that worked out just fine.");
            }
	}
	try {
	    ensure(ClassThatCantBeLoaded.lub(true,null,null)==null);
            if (!Settings.CLASSLOADING) {
                ensure(false);
            } else {
                System.out.println("We don't have a verifier right now, so this is fine.");
            }
	} catch (NoClassDefFoundError e) {
            if (Settings.CLASSLOADING) {
                // shouldn't get here, so throw
                throw e;
            } else {
                e.printStackTrace();
                System.out.println("well, that worked out just fine.");
            }
	}
    }
    
    public static void testCAS() {
	SomeOtherClass object=new SomeOtherClass();
	int[] intArray=new int[1];
	Pointer[] pointerArray=new Pointer[1];
	String[] stringArray=new String[1];
	
	// first the false tests
	
	ensureEqual(Magic.weakCAS(object,"a",1,2),false);
	ensureEqual(Magic.weakCAS(object,"b",Pointer.fromInt(1),Pointer.fromInt(2)),false);
	ensureEqual(Magic.weakCAS(object,"c","foo","bar"),false);
	
	ensureEqual(Magic.weakStaticCAS(SomeOtherClass.class,"d",1,2),false);
	ensureEqual(Magic.weakStaticCAS(SomeOtherClass.class,"e",Pointer.fromInt(1),Pointer.fromInt(2)),false);
	ensureEqual(Magic.weakStaticCAS(SomeOtherClass.class,"f","foo","bar"),false);
	
	ensureEqual(Magic.weakCAS(intArray,0,1,2),false);
	ensureEqual(Magic.weakCAS(pointerArray,0,Pointer.fromInt(1),Pointer.fromInt(2)),false);
	ensureEqual(Magic.weakCAS(stringArray,0,"foo","bar"),false);
	
	// now the true tests
	// FIXME: we're using a weak CAS but we're asserting stuff... this is dangerous,
	// but it should just work in practice.
	
	ensureEqual(Magic.weakCAS(object,"a",0,1),true);
	ensureEqual(Magic.weakCAS(object,"b",Pointer.zero(),Pointer.fromInt(1)),true);
	ensureEqual(Magic.weakCAS(object,"c",null,"foo"),true);

	ensureEqual(Magic.weakStaticCAS(SomeOtherClass.class,"d",0,1),true);
	ensureEqual(Magic.weakStaticCAS(SomeOtherClass.class,"e",Pointer.zero(),Pointer.fromInt(1)),true);
	ensureEqual(Magic.weakStaticCAS(SomeOtherClass.class,"f",null,"foo"),true);
	
	ensureEqual(Magic.weakCAS(intArray,0,0,1),true);
	ensureEqual(Magic.weakCAS(pointerArray,0,Pointer.zero(),Pointer.fromInt(1)),true);
	ensureEqual(Magic.weakCAS(stringArray,0,null,"foo"),true);
	
	// assert that the fields/arrays actually changed value
	
	ensureEqual(object.a,1);
	ensure(object.b==Pointer.fromInt(1));
	ensureEqual(object.c,"foo");
	
	ensureEqual(SomeOtherClass.d,1);
	ensure(SomeOtherClass.e==Pointer.fromInt(1));
	ensureEqual(SomeOtherClass.f,"foo");
	
	ensureEqual(intArray[0],1);
	ensure(pointerArray[0]==Pointer.fromInt(1));
	ensureEqual(stringArray[0],"foo");
	
	// now ensure that null checks and array bounds checks work
	try {
            SomeClass object2=null;
	    Magic.weakCAS(object2,"number",1,2);
	    ensure(false);
	} catch (NullPointerException e) {
	    // ok!
	}
	
	try {
            int[] array=null;
	    Magic.weakCAS(array,0,1,2);
	    ensure(false);
	} catch (NullPointerException e) {
	    // ok!
	}
	
	// FIXME: more tests!

	System.out.println("CAS works.");
    }
    
    @NoInline
    static void testExceptionsThrow(int mode)
	throws FileNotFoundException,
	       InterruptedException {
	switch (mode) {
	case 0: return;
	case 1: throw new FileNotFoundException();
	case 2: throw new InterruptedException();
	case 3: throw new ArithmeticException();
	default: throw new Fail("should not get here");
	}
    }
    
    @NoInline
    static int testExceptionsImpl(int mode) {
	try {
	    testExceptionsThrow(mode);
	} catch (FileNotFoundException e) {
	    System.out.println("FileNotFoundException caught!");
	    return 1;
	} catch (InterruptedException e) {
	    System.out.println("InterruptedException caught!");
	    return 2;
	} catch (ArithmeticException e) {
	    System.out.println("ArithmeticException caught!");
	    return 3;
	}
	return 0;
    }
    
    public static void testExceptions() {
	ensureEqual(testExceptionsImpl(0),0);
	ensureEqual(testExceptionsImpl(1),1);
	ensureEqual(testExceptionsImpl(2),2);
	ensureEqual(testExceptionsImpl(3),3);
	System.out.println("Exceptions work!");
    }

    @NoInline
    static int shiftLeft(int a,int b) {
	return a<<b;
    }
    
    @NoInline
    static long shiftLeft(long a,int b) {
	return a<<b;
    }
    
    @NoInline
    static int div4(int x) {
	return x/4;
    }
    
    public static void testIntArithmetic() {
	ensureEqual(shiftLeft(1,32),1);
	ensureEqual(shiftLeft(1l,32),4294967296l);
	ensureEqual(shiftLeft(1l,64),1l);
	ensureEqual(div4(7),1);
	ensureEqual(div4(8),2);
	ensureEqual(div4(10),2);
	ensureEqual(div4(-10),-2);
	ensureEqual(div4(-9),-2);
	System.out.println("Integer arithmetic seems to not be completely broken.");
    }
    
    @NoInline
    static void arrayStore(Object[] a,int i,Object v) {
	a[i]=v;
    }
    
    public static void testArrayStore() {
	Integer[] array=new Integer[1];
	try {
	    arrayStore(array,0,"hello");
	    throw new Fail("didn't get an array store error!");
	} catch (ArrayStoreException e) {
	    // all good!
	}
	Object[] array2=array;
	try {
	    array2[0]="hello";
	    throw new Fail("didn't get an array store error!");
	} catch (ArrayStoreException e) {
	    // all good!
	}
	System.out.println("Array store errors work.");
    }
    
    public static void testArrayCopy() {
	char[] array1=new char[]{'h','e','l','l','o',' ',' '};
	char[] array2=new char[]{' ',' ',' ',' ',' ',' ',' '};
	ensure(Arrays.equals(array1,array1));
	ensure(Arrays.equals(array2,array2));
	ensure(!Arrays.equals(array1,array2));
	ensure(!Arrays.equals(array2,array1));
	System.arraycopy(array1,0,
			 array2,0,
			 array1.length);
	ensure(Arrays.equals(array1,array2));
	ensure(Arrays.equals(array2,array1));
	System.arraycopy(array1,0,
			 array1,1,
			 5);
	ensure(Arrays.equals(array1,
			     new char[]{'h','h','e','l','l','o',' '}));
	ensure(!Arrays.equals(array1,array2));
	ensure(!Arrays.equals(array2,array1));
	System.arraycopy(array1,2,
			 array1,0,
			 4);
	ensure(Arrays.equals(array1,
			     new char[]{'e','l','l','o','l','o',' '}));
	ensure(!Arrays.equals(array1,array2));
	ensure(!Arrays.equals(array2,array1));

	String[] strArrayTrg=new String[1];
	String[] strArraySrc=new String[]{"hello"};
	ensure(!Arrays.equals(strArrayTrg,strArraySrc));
	System.arraycopy(strArraySrc,0,
			 strArrayTrg,0,
			 1);
	ensure(Arrays.equals(strArrayTrg,strArraySrc));
	
	String[] badTrg=new String[5];
	Object[] badSrc=new Object[]{new Integer(1)};
	try {
	    System.arraycopy(badSrc,0,
			     badTrg,0,
			     1);
	    logPrint("copied when we shouldn't have.\n");
	    throw new Fail("copied when we shouldn't have");
	} catch (ArrayStoreException e) {
	    // good!
	}
	
	try {
	    System.arraycopy("foo",0,
			     "bar",0,
			     5);
	    throw new Fail("copied when we shouldn't have");
	} catch (ArrayStoreException e) {
	    // good!
	}
	
	System.out.println("Array copying seems to not be totally wrong.");
    }
    
    public static void testSysProps() {
	if (false) {
	    System.out.println("we know that system properties don't work.  Implement resource bundles!!");
	    return;
	}
	System.out.println("java.library.path = "+System.getProperty("java.library.path"));
	try {
	    System.getProperties().store(System.out,"test");
	    System.out.println("System properties worked!");
	} catch (IOException e) {
	    throw new Fail(e);
	}
    }

    @AllocateAsCaller
    private static Object[] allocateBoxOnStack() {
	return new Object[1];
    }
    
    private static Object[] allocateBoxInHeap() {
	return new Object[1];
    }
    
    private static Object staticField;
    
    @StackAllocation
    private static Object testStackAllocReturn() {
	return new char[100];
    }
    
    @StackAllocation
    public static void testStackAlloc() {
	byte[] array=new byte[100];
	Object[] box1=new Object[1];
	Object[] box2=allocateBoxOnStack();
	Object[] box3=allocateBoxInHeap();

	box1[0]=array;
	System.out.println("Stack alloc test #1: ok");
	
	box2[0]=array;
	System.out.println("Stack alloc test #2: ok");
	
        if (Settings.FULL_STACK_ANALYSIS ||
            Settings.OPEN_STACK_ANALYSIS ||
            Settings.SCJ_STACK_ANALYSIS) {
            try {
                box3[0]=array;
                ensure(false);
            } catch (IllegalAssignmentError e) {
                System.out.println("Stack alloc test #3: ok");
            }
	
            try {
                staticField=array;
                ensure(false);
            } catch (IllegalAssignmentError e) {
                System.out.println("Stack alloc test #4: ok");
            }

            try {
                testStackAllocReturn();
                ensure(false);
            } catch (IllegalAssignmentError e) {
                System.out.println("Stack alloc test #5: ok");
            }
	
            try {
                throw new Error();
            } catch (IllegalAssignmentError e) {
                System.out.println("Stack alloc test #6: ok");
            }
        }
	
	System.out.println("Stack allocation seems to work!");
    }
    
    public static void testArrayReflect() {
	ensureEqual(Array.newInstance(String.class,5).getClass(),
		    new String[5].getClass());
	
	String[] strArray=(String[])Array.newInstance(String.class,5);
	ensure(strArray instanceof String[]);
	ensureEqual(strArray.length,5);
	System.out.println("Array reflection seems unbroken.");
    }
    
    public static void testThreads() {
        switch (mode) {
        case LARGE_TEST:
            LockTest.doit(8,10000000,5);
            if (ThreadPriority.realTimePrioritiesEnabled()) {
                LockTest.doit(8,40000,ThreadPriority.RR_MAX-1);
                LockTest.doit(8,40000,ThreadPriority.FIFO_MAX-1);
            }
            break;
        case SMALL_TEST:
            LockTest.doit(5,40000,5);
            if (ThreadPriority.realTimePrioritiesEnabled()) {
                LockTest.doit(5,10000,ThreadPriority.RR_MAX-1);
                LockTest.doit(5,10000,ThreadPriority.FIFO_MAX-1);
            }
            break;
        default: throw new Fail("bad mode: "+mode);
        }
    }
    
    public static void testOOME() {
        if (Pointer.size()==4) {
            try {
                long[] a = new long[Integer.MAX_VALUE];
                ensure(false);
            } catch (OutOfMemoryError e) {
                System.out.println("OOME for arrays works.");
            }
        } // else the bastard might actually succeed
        try {
            long[] a = new long[-1];
            ensure(false);
        } catch (NegativeArraySizeException e) {
            System.out.println("NASE for arrays works (1).");
        }
        try {
            byte[] a = new byte[-1];
            ensure(false);
        } catch (NegativeArraySizeException e) {
            System.out.println("NASE for arrays works (2).");
        }
    }
    
    public static void testNullAccess() {
        try {
            int[] array=null;
            array[0]=0;
        } catch (NullPointerException e) {
            System.out.println("testNullArray #1: ok");
        }
        try {
            int[] array=null;
            int x=array[0];
        } catch (NullPointerException e) {
            System.out.println("testNullArray #2: ok");
        }
        try {
            SomeClass object=null;
            int x=object.number;
        } catch (NullPointerException e) {
            System.out.println("testNullArray #3: ok");
        }
        try {
            SomeClass object=null;
            object.number=0;
        } catch (NullPointerException e) {
            System.out.println("testNullArray #4: ok");
        }
    }
    
    private static void recursive() {
        recursive();
    }
    
    public static void testStackOverflow1() {
        try {
            recursive();
            throw new Fail("should not get here");
        } catch (StackOverflowError e) {
            System.out.println("testStackOverflow #1: ok");
        }
    }
    
    public static void testStackOverflow2() throws Exception {
        final boolean[] result=new boolean[1];
        Thread t=new Thread(){
                public void run() {
                    try {
                        recursive();
                        throw new Fail("shouldn't happen");
                    } catch (StackOverflowError e) {
                        System.out.println("testStackOverflow #2: seems ok");
                        result[0]=true;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            };
        t.start();
        t.join();
        Util.ensure(result[0]);
        System.out.println("testStackOverflow #2: ok");
    }
    
    public static void testMonitors() {
        Object o=new Object();
        Util.ensureEqual(Monitors.curHolder(o),null);
        Util.ensureEqual(Monitors.recCount(o),0);
        synchronized (o) {
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
            synchronized (o) {
                Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
                Util.ensureEqual(Monitors.recCount(o),2);
            }
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
        }
        Util.ensureEqual(Monitors.curHolder(o),null);
        Util.ensureEqual(Monitors.recCount(o),0);
        System.out.println("Monitors API seems to work!");
    }
    
    static int i2ftest=54527825;
    static long l2dtest=547985217854l;
    static float f2itest=432.463f;
    static double d2ltest=46287429.624264;
    static long l2ztest=0xfd8479d893800l;
    
    public static void testFiatSimple() {
        Util.ensureEqual(""+Float.intBitsToFloat(54527825),"5.6432128E-37");
        Util.ensureEqual(""+Double.longBitsToDouble(547985217854l),"2.707406705705E-312");
        Util.ensureEqual(Float.floatToRawIntBits(432.463f),1138244420);
        Util.ensureEqual(Double.doubleToRawLongBits(46287429.624264),4721481403496037920l);
        Util.ensureEqual(""+Float.intBitsToFloat(i2ftest),"5.6432128E-37");
        Util.ensureEqual(""+Double.longBitsToDouble(l2dtest),"2.707406705705E-312");
        Util.ensureEqual(Float.floatToRawIntBits(f2itest),1138244420);
        Util.ensureEqual(Double.doubleToRawLongBits(d2ltest),4721481403496037920l);
        System.out.println("Fiats work.");
    }
    
    public static void testFiatAwesome() {
        if (Settings.IS_LITTLE_ENDIAN) {
            Util.ensureEqual(""+Magic.fiatLongToFloat(54527825l),"5.6432128E-37");
            Util.ensureEqual(Magic.fiatLongToInt(547985217854l),-1770596034);
            Util.ensureEqual(Magic.fiatLongToChar(547985217854l),(char)55614);
            Util.ensureEqual(Magic.fiatLongToShort(547985217854l),(short)-9922);
            Util.ensureEqual(Magic.fiatLongToByte(547985217854l),(byte)62);
            Util.ensureEqual(Magic.fiatLongToBoolean(0xfd8479d893800l),false);
            Util.ensureEqual(Magic.fiatLongToBoolean(0xfd8479d893801l),true);
            Util.ensureEqual(""+Magic.fiatLongToFloat(i2ftest),"5.6432128E-37");
            Util.ensureEqual(Magic.fiatLongToInt(l2dtest),-1770596034);
            Util.ensureEqual(Magic.fiatLongToChar(l2dtest),(char)55614);
            Util.ensureEqual(Magic.fiatLongToShort(l2dtest),(short)-9922);
            Util.ensureEqual(Magic.fiatLongToByte(l2dtest),(byte)62);
            Util.ensureEqual(Magic.fiatLongToBoolean(l2ztest),false);
            Util.ensureEqual(Magic.fiatLongToBoolean(l2ztest+1l),true);
            System.out.println("Awesome fiats work.");
        } else {
            System.out.println("Not testing awesome fiats work (FIXME).");
        }
    }
}


