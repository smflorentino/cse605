/*
 * fivmSupport.java
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

package java.lang;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.r1.Magic.*;
import java.util.*;

public final class fivmSupport {
    private fivmSupport() {}
    
    @NoPollcheck
    @NoSafepoint
    public static Pointer typeDataFromClass(Class<?> c) {
	return Pointer.fromObject(c.vmdata);
    }
    
    @NoPollcheck
    @NoSafepoint
    @NoSafetyChecks
    public static char[] getArray(String s) {
	return s.value;
    }
    
    @NoPollcheck
    @NoSafepoint
    @NoSafetyChecks
    public static int getOffset(String s) {
	return s.offset;
    }
    
    @NoPollcheck
    @NoSafepoint
    @NoSafetyChecks
    public static int getLength(String s) {
	return s.count;
    }
    
    @NoPollcheck
    @NoSafepoint
    public static char charAt(String s,int i) {
        return getArray(s)[getOffset(s)+i];
    }
    
    @Export
    static Pointer String_getArrayPointer(Object o) {
        if (Settings.OM_CONTIGUOUS) {
            return addressOfElement(getArray((String)o),0);
        } else {
            throw abort(
                "the fivmr_String_getArrayPointer method should not be called "+
                "under this object model");
        }
    }
    
    @Export
    static int String_getOffset(Object o) {
	return getOffset((String)o);
    }

    @AllocateAsCaller
    public static String wrap(char[] data, int offset, int count) {
	return new String(data,offset,count,true);
    }
    
    public static Thread threadForVMThread(Object o) {
	return ((VMThread)o).thread;
    }
    
    @RuntimeImport
    static native VMThread fivmr_ThreadState_javaThreadObject(Pointer ts);
    
    public static Thread threadForThreadState(Pointer ts) {
        VMThread vmt=fivmr_ThreadState_javaThreadObject(ts);
        if (vmt!=null) {
            return threadForVMThread(vmt);
        } else {
            return null;
        }
    }

    public static void setBackingStoreSize(Thread t,long size) {
        synchronized (t.vmThread) {
            t.vmThread.backingStoreSize=size;
        }
    }
    
    public static void allocBackingStoreNow() {
        Thread.currentThread().vmThread.allocBackingStore();
    }
    
    public static Pointer threadStateForThread(Thread t) {
	VMThread vmt = t.vmThread;
	return vmt.VMThread_getThreadState();
    }
    
    public static void registerThreadStartHook(Thread t,ThreadStartHook tsh) {
        putField(t,"fiji_runtimeField",tsh);
    }
    
    // FIXME: change the ClassLoader class to create this stuff in the constructor
    // instead of doing it lazily.  we can make the change directly for FijiCore,
    // and do the change in cpruntime for Classpath.
    
    // FIXME #2: have the FCClassLoader populate vmdata with an instance of
    // some internal class, that holds references to the stuff we're interested
    // in.
    
    public static Pointer getClassLoaderData(ClassLoader cl) {
        if (cl==null) {
            return baseTypeContext(0);
        } else {
            Pointer result=Pointer.fromObject(cl.vmdata);
            if (result==Pointer.zero()) {
                lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
                try {
                    result=Pointer.fromObject(cl.vmdata);
                    if (result==Pointer.zero()) {
                        result=fivmr_TypeContext_create(getVM(),
                                                        cl);
                        if (result==Pointer.zero()) {
                            throw new fivmError("Could not create TypeContext for "+cl);
                        }
                        fence();
                        addressOfField(cl,"vmdata").store(result);
                    }
                } finally {
                    unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
                }
            }
            return result;
        }
    }
    
    @NoPollcheck
    public static void setClassLoaderData(ClassLoader cl,
                                          Pointer data) {
        addressOfField(cl,"vmdata").store(data);
    }
    
    static class NullClassLoaderRefs {
        static final HashSet< Object > refs=new HashSet< Object >();
    }
    
    @SuppressWarnings("unchecked")
    public static HashSet< Object > getClassLoaderRefs(ClassLoader cl) {
        if (cl==null) {
            return NullClassLoaderRefs.refs;
        } else {
            HashSet< Object > result=(HashSet< Object >)getObjField(cl,"fiji_refs");
            if (result==null) {
                lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
                try {
                    result=(HashSet< Object >)getObjField(cl,"fiji_refs");
                    if (result==null) {
                        fence();
                        putField(cl,"fiji_refs",result=new HashSet< Object >());
                    }
                } finally {
                    unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
                }
            }
            return result;
        }
    }
    
    @Export
    @NoPollcheck // don't want pollchecks because we're manually using store barriers
    @AllowUnsafe
    @ExcludeUnlessSet({"OPEN_WORLD"})
    public static Class<?> allocateClass(Pointer td) {
        Class<?> result=new Class< Object >(null); 
        // setting vmdata this way to ensure that there aren't any safepoints
        addressOfField(result,"vmdata").store(td);
        Pointer classObjectPtr=td.add(CType.offsetof("fivmr_TypeData","classObject"));
        MM.storeBarrier(null,classObjectPtr,result,0);
        classObjectPtr.store(Pointer.fromObject(result));
        hardUse(result);
        return result;
    }
    
    @NoPollcheck // don't want pollchecks because we're manually using store barriers
    @AllowUnsafe
    public static void linkBytecode(Pointer td,byte[] bytecode) {
        Pointer bytecodePtr=td.add(CType.offsetof("fivmr_TypeData","bytecode"));
        MM.storeBarrier(null,bytecodePtr,bytecode,0);
        bytecodePtr.store(Pointer.fromObject(bytecode));
        hardUse(bytecode);
    }
    
    @NoPollcheck
    @AllowUnsafe
    public static byte[] getBytecode(Pointer td) {
        return (byte[])CType.getPointer(td,"fivmr_TypeData","bytecode").asObject();
    }
    
    public static Class<?> rootLoadClass(String name, boolean resolve)
        throws ClassNotFoundException {
        return FCClassLoader.loadClass(name,resolve);
    }
    
    public static Class<?> loadClass(ClassLoader cl,String name,boolean resolve)
        throws ClassNotFoundException {
        if (cl==null) {
            return rootLoadClass(name,resolve);
        } else {
            return cl.loadClass(name,resolve);
        }
    }
    
    public static Class<?> loadClass(ClassLoader cl,String name)
        throws ClassNotFoundException {
        return loadClass(cl,name,false);
    }
    
    public static void park(boolean isAbsolute, long time) {
	VMThread vmt = Thread.currentThread().vmThread;
	vmt.park(isAbsolute, time);
    }

    public static void unpark(Thread t) {
	VMThread vmt = t.vmThread;
	vmt.unpark();
    }
    
    public static void finalize(Object o) throws Throwable {
        o.finalize();
    }
    
    public static String intern(String s) {
        return FCString.intern(s);
    }
}


