/*
 * fivmRuntime.java
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

package com.fiji.fivm.r1;

import static com.fiji.fivm.Constants.*;
import static com.fiji.fivm.r1.Magic.*;

import static java.lang.fivmSupport.typeDataFromClass;

import com.fiji.config.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import javax.realtime.IllegalAssignmentError;
import java.nio.ByteBuffer;
import java.util.*;

import com.fiji.fivm.JNIUtils;
import com.fiji.fivm.Settings;
import com.fiji.fivm.Time;
import com.fiji.fivm.CharArrayAppendAdapter;
import com.fiji.fivm.TypeParsing;
import com.fiji.util.UTF8String;
import com.fiji.util.MyStack;

import com.fiji.fivm.codegen.*;
import com.fiji.fivm.om.*;

import com.fiji.asm.ClassReader;
import com.fiji.asm.ClassWriter;
import com.fiji.asm.ClassAdapter;
import com.fiji.asm.MethodVisitor;
import com.fiji.asm.FieldVisitor;
import com.fiji.asm.Opcodes;
import com.fiji.asm.Label;
import com.fiji.asm.UTF8Sequence;
import com.fiji.asm.UTF8BCSequence;
import com.fiji.asm.commons.EmptyVisitor;
import com.fiji.asm.commons.JSRInlinerAdapter;

import com.fiji.fivm.r1.FlowLog;

@UsesMagic
public final class fivmRuntime {
    
    private fivmRuntime() {} // never instantiated

    /** is the system fully initialized?  this means we've initialized threading,
	fivmRuntime itself, and java.lang.System. */
    public static boolean initialized;
    
    @NoPollcheck
    public static Pointer baseTypeContext(int index) {
        return CType.getPointer(Magic.getVM(),
                                "fivmr_VM",
                                "baseContexts")
            .add(Pointer.fromInt(Pointer.size()).mul(index))
            .loadPointer();
    }
    
    @NoPollcheck
    public static ClassLoader getClassLoaderForContext(Pointer ctx) {
        return (ClassLoader)
            CType.getPointer(ctx,
                             "fivmr_TypeContext",
                             "classLoader").asObject();
    }
    
    @NoPollcheck
    public static ClassLoader baseClassLoader(int index) {
        return getClassLoaderForContext(baseTypeContext(index));
    }
    
    @NoPollcheck
    public static void linkContextToLoader(Pointer context,
                                           ClassLoader cl) {
        MM.mark(cl);
        CType.put(context,
                  "fivmr_TypeContext",
                  "classLoader",
                  Pointer.fromObject(cl));
        java.lang.fivmSupport.setClassLoaderData(cl,context);
    }

    private static void initClassLoader(int index,ClassLoader cl) {
        linkContextToLoader(baseTypeContext(index),cl);
    }

    @Export
    public static void fivmRuntime_initSystemClassLoaders() {
        // FIXME: this has to tread carefully.  java.lang.ClassLoader's static init
        // will fire as soon as we try to create class loaders.  unless we're super
        // careful.
        log(fivmRuntime.class,1,"Initializing class loaders.");
        initClassLoader(1,AppClassLoader.instance);
        log(fivmRuntime.class,1,"Class loaders initialized.");
    }
        
    @Export
    public static void fivmRuntime_notifyInitialized() {
	initialized=true;
        
	sun.misc.VM.booted();
	log(fivmRuntime.class,1,"Notified of complete runtime initialization.");
        if (Settings.FINALIZATION_SUPPORTED) {
            FinalizerThread.startFinalizerThread();
        }
	if (Settings.POSIX &&
            Settings.INCLUDE_PROFILER && 
            (CType.getInt(getVM(),"fivmr_VM","flags")
             & CVar.getInt("FIVMR_VMF_RUN_PROFILER"))!=0) {
            ProfilerThread.startProfiler();
	}
    }
    
    // functions
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_abort(Pointer str);
    
    @Import
    public static native void fivmr_yield();

    public static Pointer getBuffer(int size) {
	Pointer result=fivmr_ThreadState_tryGetBuffer(curThreadState(),size);
	if (result==Pointer.zero()) {
	    result=fivmr_malloc(Pointer.fromIntZeroFill(size));
	}
	return result;
    }
    
    @Export
    public static void returnBuffer(Pointer buf) {
	if (!fivmr_ThreadState_tryReturnBuffer(curThreadState(),buf)) {
	    fivmr_free(buf);
	}
    }
    
    public static void claimBuffer(Pointer buf) {
	fivmr_ThreadState_tryClaimBuffer(curThreadState(),buf);
    }
    
    public static void claimOrReturnBuffer(int mode,Pointer buf) {
	if (mode==1) {
	    claimBuffer(buf);
	} else {
	    returnBuffer(buf);
	}
    }
    
    public static int lengthOfCString(Pointer cstr) {
	int len=0;
	for (Pointer cur=cstr;
	     cur.loadByte()!=0;
	     cur=cur.add(1)) len++;
        return len;
    }

    @AllocateAsCaller
    public static Pointer getCStringFullStack(String s) {
        byte[] result=new byte[UTF8String.encodedLength(s)+1];
        UTF8String.encodeCompletely(s,result,0,result.length-1);
        return MM.indexableStartOfArray(result);
    }
    
    @AllocateAsCaller
    public static Pointer getCStringFullStack(UTF8Sequence s) {
        byte[] result=new byte[s.byteLength()+1];
        System.arraycopy(s.byteArray(),s.byteStart(),
                         result,0,
                         s.byteLength());
        result[s.byteLength()]=0;
        return MM.indexableStartOfArray(result);
    }
    
    @AllocateAsCaller
    public static Pointer getCStringStack(String s) {
        byte[] result=new byte[s.length()+1];
        for (int i=0;i<s.length();++i) {
            result[i]=(byte)(s.charAt(i)&0xff);
        }
        return MM.indexableStartOfArray(result);
    }
    
    @AllocateAsCaller
    public static Pointer getCStringStack(String s,boolean full) {
        if (full) {
            return getCStringFullStack(s);
        } else {
            return getCStringStack(s);
        }
    }
    
    /**
     * Get a C string from a Java string.  This places the C string into the
     * per-thread temporary buffer, and must be freed using returnBuffer().
     * @param s The string to convert.
     * @param full Whether or not to do full UTF-8 conversion.  If true, full
     *             UTF-8 conversion is performed, using either C code, if
     *             the Java libraries are not yet fully initialized, or using
     *             getBytes("UTF-8"), if the Java libraries are initialized.
     *             Using C code is slower and may require calls to malloc,
     *             so unnecessary uses of this function prior to initialization
     *             are discouraged.
     */
    @StackAllocation
    public static Pointer getCString(String s,boolean full) {
	if (full) {
            int strLen=UTF8String.encodedLength(s);
            Pointer buf=getBuffer(strLen+1);
            ByteBuffer bb=java.nio.fivmSupport.wrap(buf,strLen,strLen,0);
            UTF8String.encodeCompletely(s,bb);
            buf.add(strLen).store((byte)0);
            return buf;
	} else {
	    Pointer buf=getBuffer(s.length()+1);
	    for (int i=0;i<s.length();++i) {
		buf.add(i).store((byte)s.charAt(i));
	    }
	    buf.add(s.length()).store((byte)0);
	    return buf;
	}
    }
    
    @Export
    public static Pointer getCString(String s) {
	return getCString(s,false);
    }
    
    @Export
    public static Pointer getCStringFull(String s) {
	return getCString(s,true);
    }
    
    public static Pointer getCStringFullCHeap(String s) {
        int strLen=UTF8String.encodedLength(s);
        Pointer buf=malloc(strLen+1);
        ByteBuffer bb=java.nio.fivmSupport.wrap(buf,strLen,strLen,0);
        UTF8String.encodeCompletely(s,bb);
        buf.add(strLen).store((byte)0);
        return buf;
    }
    
    public static Pointer getCStringFullCHeap(UTF8Sequence s) {
        Pointer buf=malloc(s.byteLength()+1);
        libc.copyFromByteArray(s.byteArray(),s.byteStart(),s.byteLength(),
                               buf);
        buf.add(s.byteLength()).store((byte)0);
        return buf;
    }
    
    public static Pointer getCStringFullRegion(Pointer region,String s) {
        int strLen=UTF8String.encodedLength(s);
        Pointer buf=regionAlloc(region,strLen+1);
        ByteBuffer bb=java.nio.fivmSupport.wrap(buf,strLen,strLen,0);
        UTF8String.encodeCompletely(s,bb);
        buf.add(strLen).store((byte)0);
        return buf;
    }
    
    public static Pointer getCStringFullRegion(Pointer region,UTF8Sequence s) {
        Pointer buf=regionAlloc(region,s.byteLength()+1);
        libc.copyFromByteArray(s.byteArray(),s.byteStart(),s.byteLength(),
                               buf);
        buf.add(s.byteLength()).store((byte)0);
        return buf;
    }
    
    /**
     * Get a Java string from a C string.
     * @param cstr The string to convert.
     * @param full Whether or not to do full UTF-8 conversion.  If true, the
     *             C string is treated as UTF-8, which is then properly converted
     *             to the Java encoding, using either C code, if
     *             the Java libraries are not yet fully initialized, or using
     *             new String(bytes,"UTF-8"), if the Java libraries are initialized.
     *             Using C code is slower and may require calls to malloc,
     *             so unnecessary uses of this function prior to initialization
     *             are discouraged.
     */
    @AllocateAsCaller
    public static String fromCString(Pointer cstr,boolean full) {
	if (cstr==Pointer.zero()) {
	    throw abort("cstr==0 in call to fromCString");
	}
	int len=0;
	for (Pointer cur=cstr;
	     cur.loadByte()!=0;
	     cur=cur.add(Pointer.fromIntSignExtend(1))) len++;
        if (full) {
            // FIXME the direct byte buffer ends up leaking into the caller ... we should
            // fix that!
            return UTF8String.decode(
                java.nio.fivmSupport.wrap(cstr,
                                          len,
                                          len,
                                          0));
        } else {
            char[] result=new char[len];
            for (int i=0;i<len;++i) {
                result[i]=(char)(((int)cstr.add(Pointer.fromInt(i)).loadByte())&0xff);
            }
            return java.lang.fivmSupport.wrap(result,0,len);
        }
    }
    
    @AllocateAsCaller
    public static String fromCString(Pointer cstr) {
	return fromCString(cstr,false);
    }
    
    @AllocateAsCaller
    public static String fromCStringFull(Pointer cstr) {
	return fromCString(cstr,true);
    }
    
    @AllocateAsCaller
    public static UTF8Sequence fromCStringToSeq(Pointer cstr) {
	int len=0;
	for (Pointer cur=cstr;
	     cur.loadByte()!=0;
	     cur=cur.add(Pointer.fromIntSignExtend(1))) len++;
        byte[] buffer=new byte[len];
        libc.copyToByteArray(buffer,0,len,
                             cstr);
        return new UTF8Sequence(buffer,0,len);
    }
    
    @Export
    public static String fromCStringInHeap(Pointer cstr) {
        return fromCString(cstr);
    }
    
    @Export
    public static String fromCStringFullInHeap(Pointer cstr) {
        return fromCStringFull(cstr);
    }
    
    @AllocateAsCaller
    public static String fromCStringFull(Pointer cstr,
                                         String def) {
        if (cstr!=Pointer.zero()) {
            return fromCStringFull(cstr);
        } else {
            return def;
        }
    }
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_Log_lock();
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_Log_unlock();
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_Log_print(Pointer msg);
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_Log_printNum(long num);
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_Log_printHex(long num);
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_Log_lockedPrint(Pointer msg);
    
    @RuntimeImport
    @NoSafepoint
    public static native int fivmr_Log_getLevel();
    
    public static int logLevel;

    public static OutOfMemoryError oome;
    public static boolean alreadySawAnOOME;
    
    @Export
    public static void fivmRuntime_boot() {
	logLevel=fivmr_Log_getLevel();
	oome=new OutOfMemoryError(
	    "Could not complete allocation request due to a lack "+
	    "of available memory.  Stack trace is omitted either "+
            "because there isn't enough memory to allocate it, or "+
            "because this isn't the first OutOfMemoryError.");
    }
    
    private static void logPrintImpl(Pointer cstr) {
	try {
	    fivmr_Log_lock();
	    try {
		fivmr_Log_print(cstr);
	    } finally {
		fivmr_Log_unlock();
	    }
	} finally {
	    returnBuffer(cstr);
	}
    }
    
    public static void logPrintFull(String msg) {
	logPrintImpl(getCStringFull(msg));
    }
    
    public static void logPrint(String msg) {
	logPrintImpl(getCString(msg));
    }
    
    public static void log(Class<?> from,int level,String str) {
	if (level<=logLevel) {
	    try {
		logPrint("fivmr log: ("+from.getName()+") "+str+"\n");
	    } catch (Throwable e) {
		try {
		    // fall-back method ... not as pretty but this allows
		    // us to do minimal logging even when we're in an
		    // OOME condition.
		    logPrint("fivmr log: (");
		    fivmr_Log_lockedPrint(
			fivmr_TypeData_name(
			    typeDataFromClass(from)));
		    logPrint("/fb) ");
		    logPrint(str);
		    logPrint("\n");
		} catch (Throwable ee) {
		    throw abort("Cannot log from Java.  The VM is dead.");
		}
	    }
	}
    }
    
    public static void log(Object from,int level,String str) {
	if (level<=logLevel) {
	    log(from.getClass(),level,str);
	}
    }
    
    // any part of fivmRuntime that needs static inits should put them after this
    // part (except for whatever is needed to init logging)

    @RuntimeImport
    public static native Pointer fivmr_ThreadState_tryGetBuffer(Pointer ts,int size);
    
    @RuntimeImport
    public static native boolean fivmr_ThreadState_tryReturnBuffer(Pointer ts,Pointer ptr);
    
    @RuntimeImport
    public static native boolean fivmr_ThreadState_tryClaimBuffer(Pointer ts,Pointer ptr);
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_malloc(Pointer size);
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_mallocAssert(Pointer size);
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_realloc(Pointer ptr, Pointer size);

    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_reallocAssert(Pointer ptr, Pointer size);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_free(Pointer ptr);
    
    public static Pointer malloc(Pointer size) {
        Pointer result=fivmr_malloc(size);
        if (false) {
            log(fivmRuntime.class,1,
                "Allocated "+result.asLong()+" with size "+size.asLong());
        }
        if (result==Pointer.zero()) {
            throw new OutOfMemoryError("Attempting to malloc("+size.asLong()+")");
        }
        return result;
    }
    
    public static Pointer malloc(int size) {
        return malloc(Pointer.fromInt(size));
    }
    
    public static Pointer mallocZeroed(Pointer size) {
        Pointer result=malloc(size);
        libc.bzero(result,size);
        return result;
    }
    
    public static Pointer mallocZeroed(int size) {
        return malloc(Pointer.fromInt(size));
    }
    
    public static void free(Pointer ptr) {
        fivmr_free(ptr);
    }
    
    public static void freeIfNotNull(Pointer ptr) {
        if (ptr!=Pointer.zero()) {
            free(ptr);
        }
    }
    
    public static Pointer realloc(Pointer ptr,
                                  Pointer size) {
        Pointer result=fivmr_realloc(ptr,size);
        if (result==Pointer.zero()) {
            throw new OutOfMemoryError(
                "Attempting to realloc("+ptr.asLong()+", "+size.asLong()+")");
        }
        return result;
    }
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer freg_region_alloc(Pointer root,
                                                   Pointer size);
    
    public static Pointer regionAlloc(Pointer root,
                                      Pointer size) {
        Pointer result=freg_region_alloc(root,size);
        if (result==Pointer.zero()) {
            throw new OutOfMemoryError(
                "Attempting to freg_region_alloc("+root.asLong()+","+size.asLong()+")");
        }
        return result;
    }
    
    public static Pointer regionAlloc(Pointer root,
                                      int size) {
        return regionAlloc(root,Pointer.fromInt(size));
    }
    
    @NoInline
    @NoThrow
    @NoReturn
    @NoSafepoint
    @NoPollcheck
    public static Error abort(String message) {
        // THIS IS WRITTEN SUPER CAREFULLY!  IT CANNOT HAVE SAFEPOINTS!  IT CANNOT
        // ALLOCATE!  IT MUST WORK!  The worst thing in the WORLD is to have a VM
        // that cannot abort cleanly when it wants to.
	try {
	    Pointer cstr=fivmr_malloc(Pointer.fromIntZeroFill(java.lang.fivmSupport.getLength(message)+1));
	    if (cstr!=Pointer.zero()) {
		for (int i=0;i<java.lang.fivmSupport.getLength(message);++i) {
		    cstr.add(Pointer.fromIntSignExtend(i)).store((byte)java.lang.fivmSupport.charAt(message,i));
		}
		cstr.add(Pointer.fromIntSignExtend(java.lang.fivmSupport.getLength(message))).store((byte)0);
	    }
	    fivmr_abort(cstr);
	    return null;
	} catch (Throwable e) {
	    fivmr_abort(Pointer.zero());
	    return null;
	}
    }
    
    @Reflect
    public static void yield() {
	fivmr_yield();
    }
    
    @RuntimeImport
    @NoSafepoint
    @NoThrow
    //@TrustedGodGiven // I wish this was true.
    public static native void fivmr_GC_markSlow(Pointer ts,Object o);
    
    @Import
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @GodGiven
    @NoNativeFrame
    //@TrustedGodGiven // I wish this was true.
    public static native Pointer fivmr_GC_allocRawSlow(Pointer ts,
                                                       int allocSpace,
                                                       Pointer size,
                                                       Pointer alignStart,
                                                       Pointer align,
                                                       int effort,
                                                       Pointer description);
    
    @Import
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @GodGiven
    @NoNativeFrame
    //@TrustedGodGiven // I wish this was true.
    public static native Pointer fivmr_GC_allocSSSlow(Pointer ts,
                                                      Pointer spineLength,
                                                      int numEle,
                                                      Pointer description);
    
    @Import
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @GodGiven
    @NoNativeFrame
    public static native void fivmr_addDestructor(Pointer ts,
                                                  Object object);
    
    @Import
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @GodGiven
    @NoNativeFrame
    @NoPollcheck
    //@TrustedGodGiven // I wish this was true.
    public static native Pointer fivmr_allocDestructorSlow(Pointer ts);
    
    @Import
    @NoExecStatusTransition
    @GodGiven
    @NoThrow
    public static native boolean fivmr_GC_getNextDestructor(Pointer gc,
                                                            Object objCell,
                                                            boolean wait);

    @Reflect
    @NoReturn
    @NoInline
    @RuntimeExceptionThrower
    public static void throwCloneNotSupported() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    @Reflect
    @NoReturn
    // hack to allow compilation
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @Export
    @RuntimeExceptionThrower
    public static void throwArithmeticRTE() {
	throw new ArithmeticException();
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwArithmeticRTE_inJava() {
	throwArithmeticRTE();
    }
    
    @Reflect
    @NoReturn
    // hack to allow compilation
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @Export
    @RuntimeExceptionThrower
    public static void throwNullPointerRTE() {
	throw new NullPointerException();
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwNullPointerRTE_inJava() {
	throwNullPointerRTE();
    }
    
    @Reflect
    @NoStackOverflowCheck
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @Export
    @RuntimeExceptionThrower
    public static void throwStackOverflowRTE() {
        if (CType.getBoolean(Magic.curThreadState(),
                             "fivmr_ThreadState",
                             "handlingStackOverflow")) {
            return;
        }
        CType.put(Magic.curThreadState(),
                  "fivmr_ThreadState",
                  "handlingStackOverflow",
                  true);
        StackOverflowError e=new StackOverflowError();
        CType.put(Magic.curThreadState(),
                  "fivmr_ThreadState",
                  "handlingStackOverflow",
                  false);
        throw e;
    }
    
    @NoStackOverflowCheck
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @Export
    @RuntimeExceptionThrower
    @NoExecStatusTransition
    @UseObjectsNotHandles
    public static void throwStackOverflowRTE_inJava() {
        throwStackOverflowRTE();
    }
    
    @Reflect
    @NoReturn
    // hack to allow compilation
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @RuntimeExceptionThrower
    public static void throwArrayBoundsRTE() {
	throw new ArrayIndexOutOfBoundsException();
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwArrayBoundsRTE_inJava() {
	throwArrayBoundsRTE();
    }
    
    @Reflect
    @NoReturn
    // hack to allow compilation
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @RuntimeExceptionThrower
    public static void throwArrayStoreRTE() {
	throw new ArrayStoreException();
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwArrayStoreRTE_inJava() {
	throwArrayStoreRTE();
    }
    
    @Reflect
    @NoReturn
    // hack to allow compilation
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @RuntimeExceptionThrower
    public static void throwNegativeSizeRTE() {
	throw new NegativeArraySizeException();
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwNegativeSizeRTE_inJava() {
	throwNegativeSizeRTE();
    }
    
    @Reflect
    @NoReturn
    // hack to allow compilation
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @RuntimeExceptionThrower
    public static void throwClassCastRTE() {
	throw new ClassCastException();
    }

    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwClassCastRTE_inJava() {
	throwClassCastRTE();
    }
    
    @Reflect
    @NoReturn
    // hack to allow compilation
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @RuntimeExceptionThrower
    public static void throwClassChangeRTE() {
	throw new IncompatibleClassChangeError();
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwClassChangeRTE_inJava() {
	throwClassChangeRTE();
    }
    
    @Export
    @NoReturn
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwNoClassDefFoundError_inJava(Pointer className,
                                                        Pointer fromWhere) {
        String classNameStr=null;
        String fromWhereStr=null;
        if (className!=Pointer.zero()) {
            classNameStr=fromCStringFull(className);
        }
        if (fromWhere!=Pointer.zero()) {
            fromWhereStr=fromCStringFull(fromWhere);
        }
        throwNoClassDefFoundError(classNameStr,fromWhereStr);
    }

    @Export
    @NoReturn
    @NoInline
    @RuntimeExceptionThrower
    public static void throwNoClassDefFoundError_inNative(Pointer className,
                                                          Pointer fromWhere) {
	throwNoClassDefFoundError_inJava(className,fromWhere);
    }
    
    @NoReturn
    @Reflect
    @NoPollcheck // this is a lie
    @AllowUnsafe
    @NoInline
    @RuntimeExceptionThrower
    public static void throwNoClassDefFoundError(String className,
                                                 String fromWhere) {
        if (className==null) {
            if (fromWhere==null) {
                throw new NoClassDefFoundError();
            } else {
                throw new NoClassDefFoundError(fromWhere);
            }
        } else {
            throw new NoClassDefFoundError(
                "When resolving reference to "+
                className+": "+fromWhere);
        }
    }
    
    @Reflect
    @RuntimeExceptionThrower
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static void throwNoClassDefFoundError_forBaseline() {
        throw new NoClassDefFoundError();
    }

    @Export
    @NoReturn
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwLinkageError_inJava(Pointer reason) {
        if (reason!=Pointer.zero()) {
            throw new LinkageError(fromCStringFull(reason));
        } else {
            throw new LinkageError();
        }
    }
    
    @Reflect
    @NoReturn
    @NoPollcheck
    @AllowUnsafe
    @NoInline
    @Export
    @RuntimeExceptionThrower
    public static void throwIllegalAssignmentError() {
	throw new IllegalAssignmentError();
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwIllegalAssignmentError_inJava() {
        throwIllegalAssignmentError();
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwAbstractMethodError_inJava() {
	throw new AbstractMethodError();
    }
    
    @NoInline
    @RuntimeExceptionThrower
    public static void throwExceptionInInitializerError(Throwable t,
                                                        Pointer td) {
	ExceptionInInitializerError eiie=
            new ExceptionInInitializerError();
        Magic.putField(eiie,"detailMessage",getTypeName(td));
        Magic.putField(eiie,"exception",t);
        Magic.compilerFence();
        throw eiie;
    }
    
    @Reflect
    @NoReturn
    @RuntimeExceptionThrower
    @NoInline
    public static void throwUnsatisfiedLinkErrorForMethodCall(Pointer mr) {
	throw new UnsatisfiedLinkError(
	    "Could not find implementation of JNI native method: "+
	    fromCStringFull(fivmr_MethodRec_name(mr))+" in "+
	    fromCStringFull(fivmr_TypeData_name(fivmr_MethodRec_owner(mr))));
    }
    
    @Export
    @NoInline
    @RuntimeExceptionThrower
    public static void throwUnsatisfiedLinkErrorForLoad(Pointer filename,
                                                        Pointer error) {
	throw new UnsatisfiedLinkError(
	    "Could not load JNI native library: "+fromCStringFull(filename)+
	    " because: "+fromCStringFull(error));
    }
    
    @Reflect
    @NoReturn
    @NoInline
    @RuntimeExceptionThrower
    public static void throwUnsupportedOperationException() {
	throw new UnsupportedOperationException();
    }
    
    @Export
    @UseObjectsNotHandles
    @NoExecStatusTransition
    @NoInline
    @RuntimeExceptionThrower
    public static void throwExceptionInInitializerError_inJava(Object cause,
                                                               Pointer td) {
	if (cause instanceof Throwable) {
            throwExceptionInInitializerError((Throwable)cause,td);
	} else {
	    throw abort("attempt to throw EIIE for something other than a "+
			"throwable");
	}
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwReflectiveException_inJava(Object cause) {
	if (cause instanceof Throwable) {
	    throw new ReflectiveException((Throwable)cause);
	} else {
	    throw abort("attempt to throw ReflectiveException for something other "+
			"than a throwable");
	}
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwOutOfMemoryError_inJava() {
        if (CType.getBoolean(MM.getGC(),"fivmr_GC","abortOOME")) {
            abort("FATAL: out of memory!  (Aborting because FIVMR_ABORT_OOME was set to TRUE.)");
        }
	if (alreadySawAnOOME) {
	    if (oome==null) {
		throw abort("Out of memory error before VM initialization.");
	    } else {
		throw oome;
	    }
	} else {
	    alreadySawAnOOME=true;
	    throw new OutOfMemoryError(
		"Could not complete allocation request due to a lack of "+
		"available memory");
	}
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @RuntimeExceptionThrower
    public static void throwIllegalMonitorStateException_inJava(Pointer msg) {
	throw new IllegalMonitorStateException(fromCString(msg));
    }
    
    @Export
    public static String fromUTF16Sequence(Pointer chars,int len) {
	char[] array=new char[len];
	for (int i=0;i<len;++i) {
	    array[i]=chars.add(Pointer.fromIntSignExtend(i)).loadChar();
	}
	return new String(array);
    }
    
    @Export
    public static int stringLength(Object o) {
	return ((String)o).length();
    }
    
    @Export
    public static int cstringLength(Object o) {
	// turboyuck!!!  there's got to be a better way!
	return ((String)o).getBytes().length;
    }
    
    @Export
    public static Pointer getUTF16Sequence(Object o) {
	String s=(String)o;
	Pointer buf=getBuffer(s.length()*2);
	for (int i=0;i<s.length();++i) {
	    buf.add(Pointer.fromIntSignExtend(i*2)).store(s.charAt(i));
	}
	return buf;
    }
    
    @Export
    public static Pointer getBooleanElements(Object o) {
	boolean[] array=(boolean[])o;
	Pointer buf=getBuffer(array.length);
	for (int i=0;i<array.length;++i) {
	    buf.add(Pointer.fromIntSignExtend(i)).store(array[i]);
	}
	return buf;
    }
    
    @Export
    public static Pointer getByteElements(Object o) {
	byte[] array=(byte[])o;
	Pointer buf=getBuffer(array.length);
	for (int i=0;i<array.length;++i) {
	    buf.add(Pointer.fromIntSignExtend(i)).store(array[i]);
	}
	return buf;
    }
    
    @Export
    public static Pointer getCharElements(Object o) {
	char[] array=(char[])o;
	Pointer buf=getBuffer(array.length*2);
	for (int i=0;i<array.length;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*2)).store(array[i]);
	}
	return buf;
    }
    
    @Export
    public static Pointer getShortElements(Object o) {
	short[] array=(short[])o;
	Pointer buf=getBuffer(array.length*2);
	for (int i=0;i<array.length;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*2)).store(array[i]);
	}
	return buf;
    }
    
    @Export
    public static Pointer getIntElements(Object o) {
	int[] array=(int[])o;
	Pointer buf=getBuffer(array.length*4);
	for (int i=0;i<array.length;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*4)).store(array[i]);
	}
	return buf;
    }
    
    @Export
    public static Pointer getLongElements(Object o) {
	long[] array=(long[])o;
	Pointer buf=getBuffer(array.length*8);
	for (int i=0;i<array.length;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*8)).store(array[i]);
	}
	return buf;
    }
    
    @Export
    public static Pointer getFloatElements(Object o) {
	float[] array=(float[])o;
	Pointer buf=getBuffer(array.length*4);
	for (int i=0;i<array.length;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*4)).store(array[i]);
	}
	return buf;
    }
    
    @Export
    public static Pointer getDoubleElements(Object o) {
	double[] array=(double[])o;
	Pointer buf=getBuffer(array.length*8);
	for (int i=0;i<array.length;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*8)).store(array[i]);
	}
	return buf;
    }
    
    @Export
    public static void returnBooleanElements(Object o,Pointer buf,int mode) {
	if (mode!=2) {
	    boolean[] array=(boolean[])o;
	    for (int i=0;i<array.length;++i) {
		array[i]=buf.add(Pointer.fromIntSignExtend(i)).loadBoolean();
	    }
	}
	claimOrReturnBuffer(mode,buf);
    }
    
    @Export
    public static void returnByteElements(Object o,Pointer buf,int mode) {
	if (mode!=2) {
	    byte[] array=(byte[])o;
	    for (int i=0;i<array.length;++i) {
		array[i]=buf.add(Pointer.fromIntSignExtend(i)).loadByte();
	    }
	}
	claimOrReturnBuffer(mode,buf);
    }
    
    @Export
    public static void returnCharElements(Object o,Pointer buf,int mode) {
	if (mode!=2) {
	    char[] array=(char[])o;
	    for (int i=0;i<array.length;++i) {
		array[i]=buf.add(Pointer.fromIntSignExtend(i*2)).loadChar();
	    }
	}
	claimOrReturnBuffer(mode,buf);
    }
    
    @Export
    public static void returnShortElements(Object o,Pointer buf,int mode) {
	if (mode!=2) {
	    short[] array=(short[])o;
	    for (int i=0;i<array.length;++i) {
		array[i]=buf.add(Pointer.fromIntSignExtend(i*2)).loadShort();
	    }
	}
	claimOrReturnBuffer(mode,buf);
    }
    
    @Export
    public static void returnIntElements(Object o,Pointer buf,int mode) {
	if (mode!=2) {
	    int[] array=(int[])o;
	    for (int i=0;i<array.length;++i) {
		array[i]=buf.add(Pointer.fromIntSignExtend(i*4)).loadInt();
	    }
	}
	claimOrReturnBuffer(mode,buf);
    }
    
    @Export
    public static void returnLongElements(Object o,Pointer buf,int mode) {
	if (mode!=2) {
	    long[] array=(long[])o;
	    for (int i=0;i<array.length;++i) {
		array[i]=buf.add(Pointer.fromIntSignExtend(i*8)).loadLong();
	    }
	}
	claimOrReturnBuffer(mode,buf);
    }
    
    @Export
    public static void returnFloatElements(Object o,Pointer buf,int mode) {
	if (mode!=2) {
	    float[] array=(float[])o;
	    for (int i=0;i<array.length;++i) {
		array[i]=buf.add(Pointer.fromIntSignExtend(i*4)).loadFloat();
	    }
	}
	claimOrReturnBuffer(mode,buf);
    }
    
    @Export
    public static void returnDoubleElements(Object o,Pointer buf,int mode) {
	if (mode!=2) {
	    double[] array=(double[])o;
	    for (int i=0;i<array.length;++i) {
		array[i]=buf.add(Pointer.fromIntSignExtend(i*8)).loadDouble();
	    }
	}
	claimOrReturnBuffer(mode,buf);
    }
    
    @Export
    public static void getBooleanRegion(Object o,
                                        int start,
                                        int len,
                                        Pointer buf) {
	boolean[] array=(boolean[])o;
	for (int i=start;i<start+len;++i) {
	    buf.add(Pointer.fromIntSignExtend(i)).store(array[i]);
	}
    }
    
    @Export
    public static void getByteRegion(Object o,
                                     int start,
                                     int len,
                                     Pointer buf) {
	byte[] array=(byte[])o;
	for (int i=start;i<start+len;++i) {
	    buf.add(Pointer.fromIntSignExtend(i)).store(array[i]);
	}
    }
    
    @Export
    public static void getCharRegion(Object o,
                                     int start,
                                     int len,
                                     Pointer buf) {
	char[] array=(char[])o;
	for (int i=start;i<start+len;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*2)).store(array[i]);
	}
    }
    
    @Export
    public static void getShortRegion(Object o,
                                      int start,
                                      int len,
                                      Pointer buf) {
	short[] array=(short[])o;
	for (int i=start;i<start+len;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*2)).store(array[i]);
	}
    }
    
    @Export
    public static void getIntRegion(Object o,
                                    int start,
                                    int len,
                                    Pointer buf) {
	int[] array=(int[])o;
	for (int i=start;i<start+len;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*4)).store(array[i]);
	}
    }
    
    @Export
    public static void getLongRegion(Object o,
                                     int start,
                                     int len,
                                     Pointer buf) {
	long[] array=(long[])o;
	for (int i=start;i<start+len;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*8)).store(array[i]);
	}
    }
    
    @Export
    public static void getFloatRegion(Object o,
                                      int start,
                                      int len,
                                      Pointer buf) {
	float[] array=(float[])o;
	for (int i=start;i<start+len;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*4)).store(array[i]);
	}
    }
    
    @Export
    public static void getDoubleRegion(Object o,
                                       int start,
                                       int len,
                                       Pointer buf) {
	double[] array=(double[])o;
	for (int i=start;i<start+len;++i) {
	    buf.add(Pointer.fromIntSignExtend(i*8)).store(array[i]);
	}
    }

    @Export
    public static void setBooleanRegion(Object o,
                                        int start,
                                        int len,
                                        Pointer buf) {
	boolean[] array=(boolean[])o;
	for (int i=start;i<start+len;++i) {
	    array[i]=buf.add(Pointer.fromIntSignExtend(i)).loadBoolean();
	}
    }
    
    @Export
    public static void setByteRegion(Object o,
                                     int start,
                                     int len,
                                     Pointer buf) {
	byte[] array=(byte[])o;
	for (int i=start;i<start+len;++i) {
	    array[i]=buf.add(Pointer.fromIntSignExtend(i)).loadByte();
	}
    }
    
    @Export
    public static void setCharRegion(Object o,
                                     int start,
                                     int len,
                                     Pointer buf) {
	char[] array=(char[])o;
	for (int i=start;i<start+len;++i) {
	    array[i]=buf.add(Pointer.fromIntSignExtend(i*2)).loadChar();
	}
    }
    
    @Export
    public static void setShortRegion(Object o,
                                      int start,
                                      int len,
                                      Pointer buf) {
	short[] array=(short[])o;
	for (int i=start;i<start+len;++i) {
	    array[i]=buf.add(Pointer.fromIntSignExtend(i*2)).loadShort();
	}
    }
    
    @Export
    public static void setIntRegion(Object o,
                                    int start,
                                    int len,
                                    Pointer buf) {
	int[] array=(int[])o;
	for (int i=start;i<start+len;++i) {
	    array[i]=buf.add(Pointer.fromIntSignExtend(i*4)).loadInt();
	}
    }
    
    @Export
    public static void setLongRegion(Object o,
                                     int start,
                                     int len,
                                     Pointer buf) {
	long[] array=(long[])o;
	for (int i=start;i<start+len;++i) {
	    array[i]=buf.add(Pointer.fromIntSignExtend(i*8)).loadLong();
	}
    }
    
    @Export
    public static void setFloatRegion(Object o,
                                      int start,
                                      int len,
                                      Pointer buf) {
	float[] array=(float[])o;
	for (int i=start;i<start+len;++i) {
	    array[i]=buf.add(Pointer.fromIntSignExtend(i*4)).loadFloat();
	}
    }
    
    @Export
    public static void setDoubleRegion(Object o,
                                       int start,
                                       int len,
                                       Pointer buf) {
	double[] array=(double[])o;
	for (int i=start;i<start+len;++i) {
	    array[i]=buf.add(Pointer.fromIntSignExtend(i*8)).loadDouble();
	}
    }
    
    @Export
    public static void throwInternalError(Pointer cstr) {
	throw new InternalError(fromCString(cstr));
    }
    
    @Export
    public static void throwNoSuchFieldError(Pointer name,
                                             Pointer sig) {
	throw new NoSuchFieldError("Could not find field "+fromCStringFull(name)+
				   " "+fromCStringFull(sig));
    }
    
    @Export
    public static void throwNoSuchMethodError(Pointer name,
                                              Pointer sig) {
	throw new NoSuchMethodError("Could not find method "+fromCStringFull(name)+
				    " "+fromCStringFull(sig));
    }

    public static String methodRecToString(Pointer mr) {
        return fromCStringFull(fivmr_MethodRec_describe(mr));
    }
    
    public static String fieldRecToString(Pointer fr) {
        return fromCStringFull(fivmr_FieldRec_describe(fr));
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    public static void throwNoSuchMethodError_inJava(Pointer mr) {
        throw new NoSuchMethodError("Could not find method "+methodRecToString(mr));
    }

    @Export
    public static void describeExceptionImpl(Object e_) {
	if (e_ instanceof Throwable) {
	    Throwable e=(Throwable)e_;
	    e.printStackTrace();
	} else {
	    throw abort("Told to describe something that isn't a throwable");
	}
    }
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Object fivmr_Handle_get(Pointer handle);
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_ThreadState_setInterrupted(Pointer ts,
							       boolean value);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_ThreadState_getInterrupted(Pointer ts);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_ObjHeader_forObject(Pointer settings,Object o);
    
    @NoSafepoint
    @NoPollcheck
    public static Pointer fivmr_ObjHeader_forObject(Object o) {
        return fivmr_ObjHeader_forObject(getSettings(),o);
    }
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_ObjHeader_getMonitor(Pointer settings,Pointer header);
    
    @NoSafepoint
    @NoPollcheck
    public static Pointer fivmr_ObjHeader_getMonitor(Pointer header) {
        return fivmr_ObjHeader_getMonitor(getSettings(),header);
    }
    
    @RuntimeImport
    public static native Pointer fivmr_Monitor_inflate(Pointer monitorPtr,
						       Pointer ts);
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_Monitor_curHolder(Pointer vm,
                                                         Pointer monitor);
    
    @RuntimeImport
    @TrustedGodGiven
    public static native void fivmr_Monitor_lock_slow(Pointer monitorPtr,
                                                      Pointer ts);
    
    @RuntimeImport
    @TrustedGodGiven
    public static native void fivmr_Monitor_unlock_slow(Pointer monitorPtr,
                                                        Pointer ts);
    
    @RuntimeImport
    public static native void fivmr_Monitor_wait(Pointer monitorPtr,
						 Pointer ts);
    
    @RuntimeImport
    public static native void fivmr_Monitor_timedWait(Pointer monitorPtr,
						      Pointer ts,
						      long whenAwake);
    
    @RuntimeImport
    public static native boolean fivmr_Monitor_notify(Pointer ts,Pointer monitor);
    
    @RuntimeImport
    public static native boolean fivmr_Monitor_notifyAll(Pointer ts,Pointer monitor);
    
    @NoPollcheck @NoSafepoint
    public static boolean interrupted() {
	if (fivmr_ThreadState_getInterrupted(curThreadState())) {
	    fivmr_ThreadState_setInterrupted(curThreadState(),false);
	    return true;
	} else {
	    return false;
	}
    }
    
    @NoPollcheck @NoSafepoint
    public static Pointer curHolder(Object o) {
	return fivmr_Monitor_curHolder(
            getVM(),
	    fivmr_ObjHeader_getMonitor(
		fivmr_ObjHeader_forObject(o)));
    }
    
    public static Pointer getMonitor(Object o) {
	return fivmr_Monitor_inflate(fivmr_ObjHeader_forObject(o),
				     curThreadState());
    }
    
    public static void lock(Object o) {
        Monitors.lock(o);
    }
    
    public static void unlock(Object o) {
        Monitors.unlock(o);
    }
    
    public static void wait(Object o) throws InterruptedException {
	fivmr_Monitor_wait(fivmr_ObjHeader_forObject(o),curThreadState());
	if (interrupted()) {
	    throw new InterruptedException();
	}
    }
    
    public static void waitAbsolute(Object o,
				    long whenAwake)
	throws InterruptedException {
	if (whenAwake==0) {
	    wait(o);
	} else {
	    fivmr_Monitor_timedWait(fivmr_ObjHeader_forObject(o),
				    curThreadState(),
				    whenAwake);
	    if (interrupted()) {
		throw new InterruptedException();
	    }
	}
    }
    
    public static void waitRelative(Object o,
				    long timeout)
	throws InterruptedException {
	if (timeout==0) {
	    wait(o);
	} else {
	    waitAbsolute(o,Time.nanoTime()+timeout);
	}
    }
    
    public static void notify(Object o) {
	if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_MONITOR, FlowLog.SUBTYPE_NOTIFY,
			MM.objectHeader(Pointer.fromObject(o)).asLong());
	}
	fivmr_Monitor_notify(curThreadState(),getMonitor(o));
    }
    
    public static void notifyAll(Object o) {
	if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_MONITOR, FlowLog.SUBTYPE_NOTIFY_ALL,
			MM.objectHeader(Pointer.fromObject(o)).asLong());
	}
	fivmr_Monitor_notifyAll(curThreadState(),getMonitor(o));
    }
    
    public static void checkHolder(Object o) {
	if (curHolder(o) != curThreadState()) {
	    throw new IllegalMonitorStateException();
	}
    }
    
    public static void sleepAbsolute(long whenAwake) throws InterruptedException {
	Time.sleepAbsolute(whenAwake);
	if (interrupted()) {
	    throw new InterruptedException();
	}
    }

    @RuntimeImport
    public static native void fivmr_ThreadState_lockWithHandshake(Pointer ts,
                                                                  Pointer lock);
    
    @RuntimeImport
    public static native void fivmr_Lock_unlock(Pointer lock);
    
    public static void lockWithHandshake(Pointer lock) {
        fivmr_ThreadState_lockWithHandshake(curThreadState(),lock);
    }
    
    public static void unlock(Pointer lock) {
        fivmr_Lock_unlock(lock);
    }
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_ThreadState_describeState(Pointer ts);
    
    @RuntimeImport
    @NoSafepoint
    public static native int fivmr_ThreadState_id(Pointer ts);

    @Inline
    @RuntimeImport
    @NoSafepoint
    @Pure
    @NoThrow
    public static native int fivmr_arrayLength(Pointer ts,Object o,int mask);
    
    @Inline
    @Pure
    @NoThrow
    @NoPollcheck
    // FIXME: should probably use MM.arrayLength whenever possible...
    public static int arrayLengthFromC(Object o) {
	return fivmr_arrayLength(curThreadState(),o,0);
    }
    
    @RuntimeImport
    @NoSafepoint
    public static native byte fivmr_byteArrayLoad(Pointer ts,Object o, int index, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native char fivmr_charArrayLoad(Pointer ts,Object o, int index, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native short fivmr_shortArrayLoad(Pointer ts,Object o, int index, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native int fivmr_intArrayLoad(Pointer ts,Object o, int index, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native long fivmr_longArrayLoad(Pointer ts,Object o, int index, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_pointerArrayLoad(Pointer ts,Object o, int index, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native float fivmr_floatArrayLoad(Pointer ts,Object o, int index, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native double fivmr_doubleArrayLoad(Pointer ts,Object o, int index, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native Object fivmr_objectArrayLoad(Pointer ts,Object o, int index, int mask);
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_byteArrayStore(Pointer ts,Object o, int index, byte value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_charArrayStore(Pointer ts,Object o, int index, char value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_shortArrayStore(Pointer ts,Object o, int index, short value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_intArrayStore(Pointer ts,Object o, int index, int value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_longArrayStore(Pointer ts,Object o, int index, long value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_pointerArrayStore(Pointer ts,Object o, int index, Pointer value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_floatArrayStore(Pointer ts,Object o, int index, float value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_doubleArrayStore(Pointer ts,Object o, int index, double value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_objectArrayStore(Pointer ts,Object o, int index, Object value, int mask);
    
    @RuntimeImport
    @NoSafepoint
    public static native byte fivmr_byteGetField(Pointer ts,Object o, Pointer offset, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native char fivmr_charGetField(Pointer ts,Object o, Pointer offset, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native short fivmr_shortGetField(Pointer ts,Object o, Pointer offset, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native int fivmr_intGetField(Pointer ts,Object o, Pointer offset, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native long fivmr_longGetField(Pointer ts,Object o, Pointer offset, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_pointerGetField(Pointer ts,Object o, Pointer offset, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native float fivmr_floatGetField(Pointer ts,Object o, Pointer offset, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native double fivmr_doubleGetField(Pointer ts,Object o, Pointer offset, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native Object fivmr_objectGetField(Pointer ts,Object o, Pointer offset, int mask);
    
    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_bytePutField(Pointer ts,Object o, Pointer offset, byte value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_charPutField(Pointer ts,Object o, Pointer offset, char value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_shortPutField(Pointer ts,Object o, Pointer offset, short value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_intPutField(Pointer ts,Object o, Pointer offset, int value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_longPutField(Pointer ts,Object o, Pointer offset, long value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_pointerPutField(Pointer ts,Object o, Pointer offset, Pointer value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_floatPutField(Pointer ts,Object o, Pointer offset, float value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_doublePutField(Pointer ts,Object o, Pointer offset, double value, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_objectPutField(Pointer ts,Object o, Pointer offset, Object value, int mask);
    
    @RuntimeImport
    @NoSafepoint
    public static native Object fivmr_objectGetStatic(Pointer ts,Pointer ptr, int mask);

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_objectPutStatic(Pointer ts,Pointer ptr, Object value, int mask);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    @Inline
    public static native Pointer fivmr_locationToOffsetFromObj(Pointer settings,Pointer location);
    
    @Inline
    @NoSafepoint
    @NoPollcheck
    public static Pointer fivmr_locationToOffsetFromObj(Pointer location) {
        return fivmr_locationToOffsetFromObj(getSettings(),location);
    }
    
    @Inline
    @NoPollcheck
    public static void nullCheck(Object o) {
	if (o==null) {
	    throwNullPointerRTE();
	}
    }
    
    @Inline
    @NoPollcheck
    public static void nullCheckAndArrayBoundsCheck(Object o,int index) {
	nullCheck(o);
	if (!Magic.uLessThan(index,MM.arrayLength(o))) {
	    throwArrayBoundsRTE();
	}
    }
    
    @Inline
    @NoPollcheck
    public static void nullCheckArrayBoundsCheckAndArrayStoreCheck(Object o,
                                                                   int index,
                                                                   Object src) {
	nullCheckAndArrayBoundsCheck(o,index);
	if (src!=null &&
	    !fivmr_TypeData_isSubtypeOfFast(
                Magic.curThreadState(),
		fivmr_TypeData_forObject(src),
		fivmr_TypeData_arrayElement(fivmr_TypeData_forObject(o)))) {
	    throwArrayStoreRTE();
	}
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static boolean booleanArrayLoad(Object o,int index) {
	nullCheckAndArrayBoundsCheck(o,index);
        return byteArrayLoad(o,index)==0?false:true;
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static byte byteArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_byteArrayLoad(curThreadState(),o,index,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static char charArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_charArrayLoad(curThreadState(),o,index,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static short shortArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_shortArrayLoad(curThreadState(),o,index,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static int intArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_intArrayLoad(curThreadState(),o,index,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static long longArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_longArrayLoad(curThreadState(),o,index,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static Pointer pointerArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_pointerArrayLoad(curThreadState(),o,index,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static float floatArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_floatArrayLoad(curThreadState(),o,index,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static double doubleArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_doubleArrayLoad(curThreadState(),o,index,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static Object objectArrayLoad(Object o, int index) {
	nullCheckAndArrayBoundsCheck(o,index);
	return fivmr_objectArrayLoad(curThreadState(),o,index,0);
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static void booleanArrayStore(Object o,int index,boolean value) {
	nullCheckAndArrayBoundsCheck(o,index);
        byteArrayStore(o,index,value?(byte)1:(byte)0);
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static void byteArrayStore(Object o, int index, byte value) {
	nullCheckAndArrayBoundsCheck(o,index);
	fivmr_byteArrayStore(curThreadState(),o,index,value,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void charArrayStore(Object o, int index, char value) {
	nullCheckAndArrayBoundsCheck(o,index);
	fivmr_charArrayStore(curThreadState(),o,index,value,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void shortArrayStore(Object o, int index, short value) {
	nullCheckAndArrayBoundsCheck(o,index);
	fivmr_shortArrayStore(curThreadState(),o,index,value,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void intArrayStore(Object o, int index, int value) {
	nullCheckAndArrayBoundsCheck(o,index);
	fivmr_intArrayStore(curThreadState(),o,index,value,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void longArrayStore(Object o, int index, long value) {
	nullCheckAndArrayBoundsCheck(o,index);
	fivmr_longArrayStore(curThreadState(),o,index,value,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void pointerArrayStore(Object o, int index, Pointer value) {
	nullCheckAndArrayBoundsCheck(o,index);
	fivmr_pointerArrayStore(curThreadState(),o,index,value,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void floatArrayStore(Object o, int index, float value) {
	nullCheckAndArrayBoundsCheck(o,index);
	fivmr_floatArrayStore(curThreadState(),o,index,value,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void doubleArrayStore(Object o, int index, double value) {
	nullCheckAndArrayBoundsCheck(o,index);
	fivmr_doubleArrayStore(curThreadState(),o,index,value,0);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void objectArrayStore(Object o, int index, Object value) {
	nullCheckArrayBoundsCheckAndArrayStoreCheck(o,index,value);
        MM.scopeStoreCheck(o,value);
	fivmr_objectArrayStore(curThreadState(),o,index,value,0);
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static boolean booleanGetField(Object o, Pointer location, int flags) {
	nullCheck(o);
        return byteGetField(o,location,flags)==0?false:true;
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static byte byteGetField(Object o, Pointer location,int flags) {
	nullCheck(o);
	return fivmr_byteGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static char charGetField(Object o, Pointer location,int flags) {
	nullCheck(o);
	return fivmr_charGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static short shortGetField(Object o, Pointer location,int flags) {
	nullCheck(o);
	return fivmr_shortGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static int intGetField(Object o, Pointer location, int flags) {
	nullCheck(o);
	return fivmr_intGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static long longGetField(Object o, Pointer location, int flags) {
	nullCheck(o);
	return fivmr_longGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static Pointer pointerGetField(Object o, Pointer location, int flags) {
	nullCheck(o);
	return fivmr_pointerGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static float floatGetField(Object o, Pointer location, int flags) {
	nullCheck(o);
	return fivmr_floatGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static double doubleGetField(Object o, Pointer location, int flags) {
	nullCheck(o);
	return fivmr_doubleGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static Object objectGetField(Object o, Pointer location, int flags) {
	nullCheck(o);
	return fivmr_objectGetField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), flags);
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static void booleanPutField(Object o, Pointer location, boolean value, int flags) {
	nullCheck(o);
        bytePutField(o,location,value?(byte)1:(byte)0,flags);
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static void bytePutField(Object o, Pointer location, byte value, int flags) {
	nullCheck(o);
	fivmr_bytePutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void charPutField(Object o, Pointer location, char value, int flags) {
	nullCheck(o);
	fivmr_charPutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void shortPutField(Object o, Pointer location, short value, int flags) {
	nullCheck(o);
	fivmr_shortPutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void intPutField(Object o, Pointer location, int value, int flags) {
	nullCheck(o);
	fivmr_intPutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void longPutField(Object o, Pointer location, long value, int flags) {
	nullCheck(o);
	fivmr_longPutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void pointerPutField(Object o, Pointer location, Pointer value, int flags) {
	nullCheck(o);
	fivmr_pointerPutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void floatPutField(Object o, Pointer location, float value, int flags) {
	nullCheck(o);
	fivmr_floatPutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void doublePutField(Object o, Pointer location, double value, int flags) {
	nullCheck(o);
	fivmr_doublePutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }

    @Reflect
    @NoInline
    @NoPollcheck
    public static void objectPutField(Object o, Pointer location, Object value, int flags) {
	nullCheck(o);
        MM.scopeStoreCheck(o,value);
	fivmr_objectPutField(curThreadState(), o, fivmr_locationToOffsetFromObj(location), value, flags);
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static Object objectGetStatic(Pointer ptr, int flags) {
	return fivmr_objectGetStatic(curThreadState(), ptr, flags);
    }
    
    @Reflect
    @NoInline
    @NoPollcheck
    public static void objectPutStatic(Pointer ptr, Object value, int flags) {
        MM.inHeapCheck(value); // FIXME this method should have a better name
	fivmr_objectPutStatic(curThreadState(), ptr, value, flags);
    }
    
    @RuntimeImport
    public static native Pointer fivmr_TypeStub_getTypeData(Pointer ts);

    @RuntimeImport
    public static native Pointer fivmr_TypeStub_tryGetTypeData(Pointer ts);

    @RuntimeImport
    public static native boolean fivmr_TypeStub_union(Pointer ts1,
                                                      Pointer ts2);
    
    public static void unionStubs(Pointer ts1,
                                  Pointer ts2) {
        if (!fivmr_TypeStub_union(ts1,ts2)) {
            throw new LinkageError("Linker constraints violated on "+
                                   fromCStringFull(fivmr_TypeData_name(ts1)));
        }
    }

    @RuntimeImport
    @NoSafepoint
    public static native void fivmr_TypeData_free(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_TypeData_isSubtypeOf(Pointer ts,
                                                            Pointer a,
							    Pointer b);
    
    /** Subtype check, which is only suitable for types that are fully
        resolved. */
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_TypeData_isSubtypeOfFast(Pointer ts,
                                                                Pointer a,
                                                                Pointer b);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_sizeOfTypeData(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_sizeOfTypeDataForVTableLength(int vtableLength);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_forObject(Pointer settings,Object o);
    
    @NoSafepoint
    @NoPollcheck
    public static Pointer fivmr_TypeData_forObject(Object o) {
        return fivmr_TypeData_forObject(getSettings(),o);
    }
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_TypeData_isInterface(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_TypeData_isPrimitive(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_name(Pointer td);
    
    public static String getTypeName(Pointer td) {
        return fromCStringFull(fivmr_TypeData_name(td));
    }
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_filename(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_TypeData_flags(Pointer td);

    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_parent(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_TypeData_nSuperInterfaces(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_getSuperInterface(Pointer td,
								  int i);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_arrayElement(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_TypeData_size(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native char fivmr_TypeData_requiredAlignment(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_TypeData_isArray(Pointer td);
    
    @RuntimeImport
    public static native Pointer fivmr_TypeData_makeArray(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_TypeData_refSize(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_TypeStub_refSize(Pointer ts);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_TypeData_elementSize(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_TypeData_numMethods(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_TypeData_numFields(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_method(Pointer td,
						       int i);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_field(Pointer td,
						      int i);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_TypeData_getContext(Pointer td);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Class<?> fivmr_TypeData_asClass(Pointer td);
    
    @Import
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @GodGiven
    @NoNativeFrame
    public static native boolean fivmr_TypeData_checkInit(Pointer ts,
                                                          Pointer td);
    
    @RuntimeImport
    @Pure
    public static native Pointer fivmr_TypeData_findMethod(Pointer vm,
                                                           Pointer td,
							   Pointer name,
							   Pointer sig);
    
    @RuntimeImport
    @Pure
    public static native Pointer fivmr_TypeData_findStaticField(Pointer td,
                                                                Pointer name,
                                                                Pointer sig);
    
    @RuntimeImport
    @Pure
    public static native Pointer fivmr_TypeData_findInstField(Pointer td,
                                                              Pointer name,
                                                              Pointer sig);
    
    @RuntimeImport
    @Pure
    public static native Pointer fivmr_TypeData_findStaticMethod(Pointer vm,
                                                                 Pointer td,
                                                                 Pointer name,
                                                                 Pointer sig);
    
    @RuntimeImport
    @Pure
    public static native Pointer fivmr_TypeData_findInstMethod(Pointer vm,
                                                               Pointer td,
                                                               Pointer name,
                                                               Pointer sig);
    
    @RuntimeImport
    @Pure
    public static native Pointer fivmr_TypeData_findInstMethodNoIface(Pointer vm,
                                                                      Pointer td,
                                                                      Pointer name,
                                                                      Pointer sig);

    @RuntimeImport
    @Pure
    public static native Pointer fivmr_TypeData_findInstMethodNoIface2(Pointer vm,
                                                                       Pointer td,
                                                                       Pointer name,
                                                                       Pointer result,
                                                                       int nparams,
                                                                       Pointer params);
    
    @RuntimeImport
    public static native boolean fivmr_TypeData_resolve(Pointer td);
    
    public static void resolveType(Pointer td) {
        if (!fivmr_TypeData_resolve(td)) {
            throw new LinkageError("Could not link and resolve "+
                                   fromCStringFull(fivmr_TypeData_name(td)));
        }
    }
    
    @StackAllocation
    public static Pointer findStaticField(Pointer td,
                                          String name,
                                          String sig) {
        return fivmr_TypeData_findStaticField(
            td,getCStringFullStack(name),getCStringFullStack(sig));
    }
    
    @StackAllocation
    public static Pointer findInstField(Pointer td,
                                        String name,
                                        String sig) {
        return fivmr_TypeData_findInstField(
            td,getCStringFullStack(name),getCStringFullStack(sig));
    }
    
    @StackAllocation
    public static Pointer findStaticField(Pointer td,
                                          UTF8Sequence name,
                                          UTF8Sequence sig) {
        return fivmr_TypeData_findStaticField(
            td,getCStringFullStack(name),getCStringFullStack(sig));
    }
    
    @StackAllocation
    public static Pointer findInstField(Pointer td,
                                        UTF8Sequence name,
                                        UTF8Sequence sig) {
        return fivmr_TypeData_findInstField(
            td,getCStringFullStack(name),getCStringFullStack(sig));
    }
    
    @StackAllocation
    public static Pointer findStaticField(Pointer td,
                                          UTF8Sequence name,
                                          Pointer sig) {
        return fivmr_TypeData_findStaticField(
            td,getCStringFullStack(name),sig);
    }
    
    @StackAllocation
    public static Pointer findInstField(Pointer td,
                                        UTF8Sequence name,
                                        Pointer sig) {
        return fivmr_TypeData_findInstField(
            td,getCStringFullStack(name),sig);
    }
    
    @StackAllocation
    public static Pointer findStaticMethod(Pointer td,
                                           UTF8Sequence name,
                                           UTF8Sequence desc) {
        return fivmr_TypeData_findStaticMethod(
            Magic.getVM(),td,getCStringFullStack(name),getCStringFullStack(desc));
    }
    
    @StackAllocation
    public static Pointer findInstMethod(Pointer td,
                                         UTF8Sequence name,
                                         UTF8Sequence desc) {
        return fivmr_TypeData_findInstMethod(
            Magic.getVM(),td,getCStringFullStack(name),getCStringFullStack(desc));
    }
    
    @StackAllocation
    public static Pointer findInstMethodNoIface(Pointer td,
                                                UTF8Sequence name,
                                                UTF8Sequence desc) {
        return fivmr_TypeData_findInstMethodNoIface(
            Magic.getVM(),td,getCStringFullStack(name),getCStringFullStack(desc));
    }
    
    public static Pointer getTopTD() {
        return CType.getPointer(getPayload(),"fivmr_Payload","td_top");
    }
    
    @RuntimeImport
    public static native Pointer fivmr_TypeContext_findKnown(Pointer ctx,
                                                             Pointer name);

    @RuntimeImport
    public static native Pointer fivmr_TypeContext_find(Pointer ctx,
                                                        Pointer name);

    @RuntimeImport
    public static native Pointer fivmr_TypeContext_findStub(Pointer ctx,
                                                            Pointer name);

    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_FieldRec_owner(Pointer fr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_FieldRec_name(Pointer fr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_FieldRec_flags(Pointer fr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_FieldRec_type(Pointer fr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_FieldRec_location(Pointer fr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_FieldRec_staticFieldAddress(Pointer vm,
                                                                   Pointer fr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_FieldRec_barrierArg(Pointer vm,Pointer fr);
    
    @NoSafepoint
    @NoPollcheck
    public static Pointer fivmr_FieldRec_staticFieldAddress(Pointer fr) {
        return fivmr_FieldRec_staticFieldAddress(getVM(),fr);
    }
    
    @NoSafepoint
    @NoPollcheck
    public static Pointer fivmr_FieldRec_barrierArg(Pointer fr) {
        return fivmr_FieldRec_barrierArg(getVM(),fr);
    }
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_FieldRec_offsetFromObj(Pointer vm,
                                                              Pointer fr);
    
    @NoSafepoint
    @NoPollcheck
    public static Pointer fivmr_FieldRec_offsetFromObj(Pointer fr) {
        return fivmr_FieldRec_offsetFromObj(getSettings(),fr);
    }
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_MethodRec_owner(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_MethodRec_name(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_MethodRec_isConstructor(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_MethodRec_isStaticInit(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native boolean fivmr_MethodRec_isInitializer(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_MethodRec_flags(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_MethodRec_result(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_MethodRec_nparams(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_MethodRec_param(Pointer mr,
						       int i);
    
    @RuntimeImport
    @Pure
    public static native Pointer fivmr_MethodRec_reresolveSpecial(Pointer ts,
                                                                  Pointer from,
                                                                  Pointer target);
    
    @Import
    @GodGiven
    public static native void fivmr_MethodRec_registerMC(Pointer mr,
                                                         Pointer mc);
    
    @Import
    @GodGiven
    public static native void fivmr_MethodRec_unregisterMC(Pointer mr,
                                                           Pointer mc);
    
    @Import
    @GodGiven
    public static native Pointer fivmr_MethodRec_findMC(Pointer mr,
                                                        int mask,
                                                        int expected);
    
    @Import
    @GodGiven
    public static native boolean fivmr_MethodRec_hasMC(Pointer mr,
                                                       int mask,
                                                       int expected);
    
    @RuntimeImport
    public static native boolean fivmr_MethodRec_matchesSig(Pointer mr,
                                                            Pointer sig);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_MethodRec_location(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_MethodRec_entrypoint(Pointer mr);

    @RuntimeImport
    @NoSafepoint
    @Pure
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native int fivmr_Baseline_offsetToJStack(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native Pointer fivmr_MachineCode_decodeMethodRec(Pointer method);
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native Pointer fivmr_MachineCode_create(int size,
                                                          int flags);
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native void fivmr_MachineCode_registerMC(Pointer parent,
                                                           Pointer child);
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native void fivmr_MachineCode_appendBasepoint(Pointer code,
                                                                int bytecodePC,
                                                                int stackHeight,
                                                                Pointer machinecodePC);
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native void fivmr_MachineCode_appendBaseTryCatch(Pointer code,
                                                                   int start,
                                                                   int end,
                                                                   int target,
                                                                   Pointer type);
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native void fivmr_MachineCode_downsize(Pointer code,
                                                         int newSize);
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native boolean fivmr_supportDownsizeExec();
    
    @RuntimeImport
    public static native Pointer fivmr_DebugRec_lookup(Pointer vm,
                                                       Pointer di);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native Pointer fivmr_DebugRec_getMethod(Pointer dr);
    
    @RuntimeImport
    @NoSafepoint
    @Pure
    public static native int fivmr_DebugRec_getBytecodePC(Pointer dr);
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_DebugRec_withRootSize(Pointer dr,
                                                             Pointer region,
                                                             int rootSize);
    
    @Reflect
    public static void checkInit(Pointer td) {
	fivmr_TypeData_checkInit(curThreadState(),td);
    }
    
    public static void resolveAndCheckInit(Pointer td) {
        resolveType(td);
        checkInit(td);
    }
    
    public static void checkInit(Class<?> klass) {
        checkInit(typeDataFromClass(klass));
    }
    
    public static void resolveAndCheckInit(Class<?> klass) {
        resolveAndCheckInit(typeDataFromClass(klass));
    }
    
    @Import
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @GodGiven
    @NoNativeFrame
    public static native void fivmr_throw(Pointer ts,Throwable t);
    
    public static void uncheckedThrow(Throwable t) {
	fivmr_throw(curThreadState(),t);
    }
    
    @Import
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @GodGiven
    @NoNativeFrame
    public static native Pointer fivmr_JNI_lookup(Pointer ts,Pointer mr);
    
    @Reflect
    public static Pointer resolveNative(Pointer mr) {
	if (Settings.STATIC_JNI) {
	    throw abort("resolveNative() called in static JNI mode");
	} else if (!Settings.DYN_LOADING) {
	    throwUnsatisfiedLinkErrorForMethodCall(mr);
	    throw abort("should not get here");
	} else {
	    Pointer result=fivmr_JNI_lookup(curThreadState(),mr);
	    if (result==Pointer.zero()) {
		throwUnsatisfiedLinkErrorForMethodCall(mr);
	    }
	    return result;
	}
    }
    
    @StackAllocation
    private static void transcodeCStringToJNI(Appendable target,
                                              Pointer cstr,
                                              int leftClip,
                                              int rightClip) {
        String jstr=fromCStringFull(cstr);
        JNIUtils.jniEscape(target,jstr,leftClip,jstr.length()-rightClip);
    }
    
    /**
     * Copy the *bytes* of a Java string into an already-existing C-string buffer.
     * This does not do UTF-8 conversion - it just casts chars to bytes.  Use
     * with care.
     */
    private static void copyToCStringBuf(Pointer cstr,
                                         int cstrLen,
                                         char[] buf,
                                         int len) {
	if (len+1>cstrLen) {
	    throw new fivmError("function name too long");
	}
	
	for (int i=0;i<len;++i) {
	    cstr.add(i).store((byte)buf[i]);
	}
	cstr.add(len).store((byte)0);
    }
    
    /**
     * Convert a MethodRec to its JNI function name in the form of an ASCII
     * C string.  This does all of the necessary JNI mangling.  Note that this
     * method, and the methods it calls, only allocate on the stack.  This is
     * done to ensure that:
     * <ul>
     * <li>we can perform JNI method calls (even first-time
     *     calls) under an OOME condition.</li>
     * <li>performing a JNI method call does not contribute to allocation
     *     rate.</li>
     * <li>when running with immortal memory (NOGC), performing JNI method
     *     calls does not leak memory.</li>
     * </ul>
     */
    // FIXME: this requires invoking native code. :-(  it'd be better if
    // we could get by using Java-based UTF conversion, since I really
    // don't trust libiconv.
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @StackAllocation
    public static void makeJNIFuncName(Pointer cstr,
                                       int cstrLen,
                                       Pointer methodRec,
                                       boolean longForm) {
	try {
            char[] buf=new char[256];
            CharArrayAppendAdapter caaa=new CharArrayAppendAdapter(buf,0);
	    
	    caaa.append("Java_");
            transcodeCStringToJNI(
                caaa,
                fivmr_TypeData_name(fivmr_MethodRec_owner(methodRec)),
                1,1);
	    caaa.append("_");
            transcodeCStringToJNI(
                caaa,
                fivmr_MethodRec_name(methodRec),
                0,0);
	    
	    if (longForm) {
		caaa.append("__");
		for (int i=0;i<fivmr_MethodRec_nparams(methodRec);++i) {
                    transcodeCStringToJNI(
                        caaa,
                        fivmr_TypeData_name(fivmr_MethodRec_param(methodRec,i)),
                        0,0);
		}
	    }
	    
	    // is this right?  it does no UTF-8 conversion.  yeah, it should be, since
	    // we've jniEscaped everything already.
	    copyToCStringBuf(cstr,cstrLen,
			     buf,caaa.position());
	    
	} catch (VirtualMachineError e) {
	    throw e;
	} catch (Throwable e) {
	    throw new fivmError("Error in makeJNIFuncName: "+e);
	}
    }
    
    @Export
    public static String[] processArgs(int argc,Pointer argv) {
	String[] result=new String[argc];
	for (int i=0;i<argc;++i) {
	    result[i]=fromCStringFull(argv.add(i*Pointer.size()).loadPointer());
	}
	return result;
    }
    
    @StackAllocation
    public static Pointer findKnownType(Pointer ctx,String name) {
	Pointer cstr=getCStringFullStack(name);
	return fivmr_TypeContext_findKnown(ctx,cstr);
    }
    
    @StackAllocation
    public static Pointer findType(Pointer ctx,String name) {
	Pointer cstr=getCStringFullStack(name);
	return fivmr_TypeContext_find(ctx,cstr);
    }
    
    @StackAllocation
    public static Pointer findStub(Pointer ctx,String name) {
	Pointer cstr=getCStringFullStack(name);
	return fivmr_TypeContext_findStub(ctx,cstr);
    }
    
    public static Pointer findStubClass(Pointer ctx,String name) {
        return findStub(ctx,"L"+name+";");
    }
    
    @StackAllocation
    public static Pointer findKnownType(Pointer ctx,UTF8Sequence name) {
	Pointer cstr=getCStringFullStack(name);
	return fivmr_TypeContext_findKnown(ctx,cstr);
    }
    
    @StackAllocation
    public static Pointer findType(Pointer ctx,UTF8Sequence name) {
	Pointer cstr=getCStringFullStack(name);
	return fivmr_TypeContext_find(ctx,cstr);
    }
    
    @StackAllocation
    public static Pointer findStub(Pointer ctx,UTF8Sequence name) {
	Pointer cstr=getCStringFullStack(name);
	return fivmr_TypeContext_findStub(ctx,cstr);
    }
    
    public static Pointer findStubClass(Pointer ctx,UTF8Sequence name) {
        return findStub(ctx,UTF8Sequence.L.plus(name,UTF8Sequence.SEMI));
    }
    
    @Import
    @GodGiven
    public native static Pointer fivmr_TypeContext_addUntracedField(Pointer ctx,
                                                                    int size);
    
    @Import
    @GodGiven
    public native static Pointer fivmr_TypeContext_addTracedField(Pointer ctx);
    
    public static RuntimeException rethrowUnchecked(Throwable t) {
	if (t instanceof RuntimeException) {
	    throw (RuntimeException)t;
	} else if (t instanceof Error) {
	    throw (Error)t;
	} else {
	    throw new Error(t);
	}
    }
    
    @Export
    public static void getStringRegion(Object o,
                                       int start,
                                       int len,
                                       Pointer buf) {
	String s=(String)o;
	for (int i=start;i<len;++i) {
	    buf.add((i-start)*2).store(s.charAt(i));
	}
    }
    
    // NEVER USE THIS.  it's a buffer overflow waiting to happen.  but, JNI
    // stupidly mandates it.
    @Export
    public static void getStringUTFRegion(Object o,
                                          int start,
                                          int len,
                                          Pointer buf) {
        try {
            byte[] bytes=((String)o).substring(start,start+len).getBytes("UTF-8");
            for (int i=0;i<bytes.length;++i) {
                buf.add(i).store(bytes[i]);
            }
        } catch (UnsupportedEncodingException e) {
            throw new fivmError(e);
        }
    }

    @RuntimeImport
    @NoPollcheck
    public static native void fivmr_ThreadState_dumpStackFor(Pointer ts);

    @NoPollcheck
    public static void dumpStack() {
	fivmr_ThreadState_dumpStackFor(curThreadState());
    }
    
    public static abstract class DumpStackCback {
	@Export
	@NoExecStatusTransition
	@UseObjectsNotHandles
	final Pointer DumpStackCback_cback(Pointer mr,int lineNumber) {
	    return cback(mr,lineNumber);
	}
	
	public abstract Pointer cback(Pointer mr,int lineNumber);
    }
    
    @RuntimeImport
    @NoInline
    public static native Pointer fivmr_iterateDebugFrames_forJava(Pointer vm,
                                                                  Pointer f,
								  DumpStackCback cback);
    
    /**
     * Iterate over the debug frames starting at the given frame pointer, and calling
     * the given instance of DumpStackCback, which will process the information.  To
     * ensure deterministic behavior, make sure that the function from which you are
     * getting the starting frame (parameter 'f') is marked @NoInline.
     */
    @NoInline
    public static Pointer iterateDebugFrames(Pointer f,
					     DumpStackCback cback) {
	return fivmr_iterateDebugFrames_forJava(getVM(),f,cback);
    }
    
    
    @RuntimeImport
    @NoInline
    public static native Pointer fivmr_methodForStackDepth(Pointer vm,
                                                           Pointer f,
							   int depth);
    
    /** Returns the parameters of a method, without parenthesis, the way they
	might appear in Java source code. */
    public static String javaParams(Pointer mr) {
	StringBuilder buf=new StringBuilder();
	for (int i=0;i<fivmr_MethodRec_nparams(mr);++i) {
	    if (i!=0) {
		buf.append(", ");
	    }
	    buf.append(getParameterType(mr,i));
	}
	return buf.toString();
    }
    
    private static void flagsToString(StringBuilder buf,
				      int flags) {
	switch (flags&BF_VISIBILITY) {
	case BF_PRIVATE: buf.append("private "); break;
	case BF_PACKAGE: break;
	case BF_PROTECTED: buf.append("protected "); break;
	case BF_PUBLIC: buf.append("public "); break;
	default: throw new fivmError("bad flags: "+flags);
	}
    }
    
    public static String reflectiveMethodToString(Pointer mr) {
	StringBuilder buf=new StringBuilder();
	int flags=fivmr_MethodRec_flags(mr);
	flagsToString(buf,flags);
	if ((flags&MBF_METHOD_KIND)==MBF_ABSTRACT) {
	    buf.append("abstract ");
	}
	if ((flags&BF_STATIC)!=0) {
	    buf.append("static ");
	}
	if ((flags&MBF_METHOD_KIND)==MBF_FINAL) {
	    buf.append("final ");
	}
	if ((flags&MBF_SYNCHRONIZED)!=0) {
	    buf.append("synchronized ");
	}
	if ((flags&MBF_METHOD_IMPL)==MBF_JNI ||
	    (flags&MBF_METHOD_IMPL)==MBF_INTRINSIC ||
            (flags&MBF_METHOD_IMPL)==MBF_SYNTHETIC ||
	    (flags&MBF_METHOD_IMPL)==MBF_IMPORT) {
	    buf.append("native ");
	}
	buf.append(getResultType(mr));
	buf.append(" ");
	buf.append(fivmr_TypeData_asClass(fivmr_MethodRec_owner(mr)));
	if (!fivmr_MethodRec_isConstructor(mr)) {
	    buf.append(".");
	    buf.append(fromCStringFull(fivmr_MethodRec_name(mr)));
	}
	buf.append("(");
	buf.append(javaParams(mr));
	buf.append(")");
	return buf.toString();
    }
    
    public static String reflectiveFieldToString(Pointer fr) {
	StringBuilder buf=new StringBuilder();
	int flags=fivmr_FieldRec_flags(fr);
	flagsToString(buf,flags);
	if ((flags&BF_STATIC)!=0) {
	    buf.append("static ");
	}
	if ((flags&FBF_FINAL)!=0) {
	    buf.append("final ");
	}
	if ((flags&FBF_TRANSIENT)!=0) {
	    buf.append("transient ");
	}
	if ((flags&FBF_VOLATILE)!=0) {
	    buf.append("volatile ");
	}
	buf.append(getFieldType(fr));
	buf.append(" ");
	buf.append(fivmr_TypeData_asClass(fivmr_FieldRec_owner(fr)));
	buf.append(".");
	buf.append(fromCStringFull(fivmr_FieldRec_name(fr)));
	return buf.toString();
    }

    public static int genericModifiersForFlags(int flags) {
	int result=0;
	
	switch (flags&BF_VISIBILITY) {
	case BF_PRIVATE: result|=Modifier.PRIVATE; break;
	case BF_PACKAGE: break;
	case BF_PROTECTED: result|=Modifier.PROTECTED; break;
	case BF_PUBLIC: result|=Modifier.PUBLIC; break;
	default: throw new Error("bad value of visibility in flags: "+flags);
	}
	
	return result;
    }
    
    public static int methodModifiersForFlags(int flags) {
	int result = genericModifiersForFlags(flags);
	
	switch (flags&MBF_METHOD_KIND) {
	case MBF_FINAL: result|=Modifier.FINAL; break;
	case MBF_VIRTUAL: break;
	case MBF_ABSTRACT: result|=Modifier.ABSTRACT; break;
	default: throw new Error("bad value of method kind flags: "+flags);
	}
	
	if ((flags&MBF_SYNCHRONIZED)!=0) {
	    result|=Modifier.SYNCHRONIZED;
	}
	
	switch (flags&MBF_METHOD_IMPL) {
	case MBF_STUB:
	case MBF_BYTECODE: break;
	case MBF_JNI:
	case MBF_INTRINSIC:
	case MBF_UNSUPPORTED:
        case MBF_SYNTHETIC:
	case MBF_IMPORT: result|=Modifier.NATIVE; break;
	default: throw new Error("bad value for method implementation flags: "+flags);
	}
	
	return result;
    }
    
    public static int fieldModifiersForFlags(int flags) {
	int result = genericModifiersForFlags(flags);
	
	if ((flags&FBF_FINAL)!=0) {
	    result|=Modifier.FINAL;
	}
	if ((flags&FBF_VOLATILE)!=0) {
	    result|=Modifier.VOLATILE;
	}
	if ((flags&FBF_TRANSIENT)!=0) {
	    result|=Modifier.TRANSIENT;
	}
	
	return result;
    }
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_MethodRec_describe(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_FieldRec_describe(Pointer mr);
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_MethodRec_descriptor(Pointer mr);
    
    /**
     * Call a method reflectively.  This is tricky.  The caller should
     * use @NoPollcheck.  Note that fivmr_MethodRec_callJ may lead
     * to safepoints, but unless you use some weird options that you
     * shouldn't be using anyway (like CM_EXEC_STATUS),
     * these safepoints will occur <b>after</b> the receiver and args are
     * consumed and <b>before</b> the return value is turned into a
     * long, or else <b>instead of</b> processing the receiver and args,
     * meaning that though a safepoint will occur while pointers have
     * been stored in an unsafe and untracable way, it won't actually
     * matter for correctness.
     */
    @Import
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @GodGiven
    @NoNativeFrame
    public static native long fivmr_MethodRec_callJ(Pointer mr,
						    Pointer ts,
						    Pointer methodPtr,
						    Object receiver,
						    Pointer args,
						    int cm);
    
    private static Object processResult(long result,Pointer mr) {
	switch ((char)fivmr_TypeData_name(fivmr_MethodRec_result(mr)).loadByte()) {
	case 'Z': return new Boolean(fiatLongToBoolean(result));
	case 'B': return new Byte(fiatLongToByte(result));
	case 'C': return new Character(fiatLongToChar(result));
	case 'S': return new Short(fiatLongToShort(result));
	case 'I': return new Integer(fiatLongToInt(result));
	case 'J': return new Long(result);
	case 'F': return new Float(fiatLongToFloat(result));
	case 'D': return new Double(fiatLongToDouble(result));
	case 'L':
	case '[': return fiatLongToPointer(result).asObject();
	case 'V': return null;
	default: throw new fivmError(
	    "Invalid result type for reflective call: "+
	    fromCStringFull(fivmr_TypeData_name(fivmr_MethodRec_result(mr))));
	}
    }
    
    private static IllegalArgumentException makeIAENParams(Pointer mr) {
	return new IllegalArgumentException(
	    "Wrong number of arguments to "+
	    fromCStringFull(fivmr_MethodRec_describe(mr)));
    }
    
    private static IllegalArgumentException makeIAECast(int i,Pointer ptd) {
	return new IllegalArgumentException(
	    "Argument #"+i+" to reflective call, of type "+
	    fivmr_TypeData_asClass(ptd)+", not boxed correctly");
    }
    
    private static IllegalArgumentException makeIAESubtype(int i,Class<?> c,Pointer ptd) {
	return new IllegalArgumentException(
	    "Argument #"+i+" to reflective call is of type "+
	    c+", but expected type "+
	    fivmr_TypeData_asClass(ptd));
    }
    
    /**
     * Perform the reflective call the easy way.  This does null
     * checking, class change checking, static initialization, and
     * optionally, dispatch, in a safe way.
     * @param mr        the MethodRec pointer
     * @param methodPtr the function pointer of the method, or Pointer.zero()
     *                  if dispath should be done automatically
     * @param receiver  the receiver for instance calls; ignored for static
     *                  calls.
     * @param args      the boxed arguments
     * @return          the boxed result
     */
    @NoPollcheck
    @AllowUnsafe
    @StackAllocation
    public static Object reflectiveCall(Pointer mr,
					Pointer methodPtr,
					Object receiver,
					Object[] args) {
	// perform checks
	if (args.length!=fivmr_MethodRec_nparams(mr)) {
	    throw makeIAENParams(mr);
	}
	
	
	Pointer rawArgPtr;
	if (fivmr_MethodRec_nparams(mr)>0) {
	    long[] rawArgs=new long[fivmr_MethodRec_nparams(mr)];
	    
	    // NB. after this point, there will be no safepoints until we make
	    // the reflective call, except if there is an error, in which case
	    // we won't make the reflective call, and rawArgs is thrown away.
	    
	    rawArgPtr=MM.indexableStartOfArray(rawArgs);
	    
	    for (int i=0;i<fivmr_MethodRec_nparams(mr);++i) {
		Pointer ptd=resolveStub(fivmr_MethodRec_param(mr,i));
		if (fivmr_TypeData_isPrimitive(ptd)) {
		    try {
			switch ((char)fivmr_TypeData_name(ptd).loadByte()) {
			case 'Z': rawArgPtr.add(i*8).store((Boolean)args[i]); break;
			case 'B': rawArgPtr.add(i*8).store((Byte)args[i]); break;
			case 'C': rawArgPtr.add(i*8).store((Character)args[i]); break;
			case 'S': rawArgPtr.add(i*8).store((Short)args[i]); break;
			case 'I': rawArgPtr.add(i*8).store((Integer)args[i]); break;
			case 'J': rawArgPtr.add(i*8).store((Long)args[i]); break;
			case 'F': rawArgPtr.add(i*8).store((Float)args[i]); break;
			case 'D': rawArgPtr.add(i*8).store((Double)args[i]); break;
			default: throw fivmError.make();
			}
		    } catch (ClassCastException e) {
			throw makeIAECast(i,ptd);
		    }
		} else {
		    if (!fivmr_TypeData_isSubtypeOf(
                            Magic.curThreadState(),
			    fivmr_TypeData_forObject(args[i]),
			    ptd)) {
			throw makeIAESubtype(i,args[i].getClass(),ptd);
		    }
		    rawArgPtr.add(i*8).store(Pointer.fromObject(args[i]));
		}
	    }
	} else {
	    rawArgPtr=Pointer.zero();
	}
	
	long result=
	    fivmr_MethodRec_callJ(mr,
				  curThreadState(),
				  methodPtr,
				  receiver,
				  rawArgPtr,
				  (methodPtr==Pointer.zero()?CM_DISPATCH:0)|
				  CM_NULLCHECK|
				  CM_CLASSCHANGE|
				  CM_CHECKINIT|
				  CM_WRAP_EXCEPTION);
	
	return processResult(result,mr);
    }
    
    public static Object reflectiveCall(Pointer mr,
					Object receiver,
					Object[] args) {
	return reflectiveCall(mr,Pointer.zero(),receiver,args);
    }
    
    public static Object reflectiveCall(Pointer mr,
					Object[] args) {
	return reflectiveCall(mr,Pointer.zero(),null,args);
    }
    
    public static boolean isAbstract(Pointer td) {
	int tk=(fivmr_TypeData_flags(td)&TBF_TYPE_KIND);
	return tk==TBF_ANNOTATION
	    || tk==TBF_INTERFACE
	    || tk==TBF_ABSTRACT;
    }
    
    public static boolean isClass(Pointer td) {
	int tk=(fivmr_TypeData_flags(td)&TBF_TYPE_KIND);
	return tk==TBF_ANNOTATION
	    || tk==TBF_INTERFACE
	    || tk==TBF_ABSTRACT
	    || tk==TBF_VIRTUAL
	    || tk==TBF_FINAL;
    }
    
    public static Pointer resolveStub(Pointer ts) {
        Pointer td=fivmr_TypeStub_getTypeData(ts);
        if (Settings.ASSERTS_ON && td==Pointer.zero()) {
            throw new fivmError("TypeStub_getTypeData returned null");
        }
        return td;
    }
    
    public static Class<?> getClassForStub(Pointer ts) {
        return fivmr_TypeData_asClass(resolveStub(ts));
    }
    
    public static Class<?> getFieldType(Pointer fr) {
        return getClassForStub(fivmr_FieldRec_type(fr));
    }
    
    public static Class<?> getParameterType(Pointer mr,
                                            int index) {
        return getClassForStub(fivmr_MethodRec_param(mr,index));
    }
    
    public static Class<?>[] getParameterTypes(Pointer mr) {
	Class<?>[] result=new Class<?>[fivmr_MethodRec_nparams(mr)];
	for (int i=0;i<result.length;++i) {
	    result[i]=getParameterType(mr,i);
	}
	return result;
    }
    
    public static Class<?> getResultType(Pointer mr) {
        return getClassForStub(fivmr_MethodRec_result(mr));
    }
    
    @RuntimeImport
    public static native void fivmr_ReflectLog_dynamicCall(Pointer ts,int depth,Pointer mr);
    @RuntimeImport
    public static native void fivmr_ReflectLog_call(Pointer ts,int depth,Pointer mr);
    @RuntimeImport
    public static native void fivmr_ReflectLog_access(Pointer ts,int depth,Pointer fr);
    @RuntimeImport
    public static native void fivmr_ReflectLog_alloc(Pointer ts,int depth,Pointer td);
    @RuntimeImport
    public static native void fivmr_ReflectLog_use(Pointer ts,int depth,Pointer td);
    
    @RuntimeImport
    public static native void fivmr_ReflectLog_dynamicCallReflect(Pointer ts,Pointer mr);
    @RuntimeImport
    public static native void fivmr_ReflectLog_callReflect(Pointer ts,Pointer mr);
    @RuntimeImport
    public static native void fivmr_ReflectLog_accessReflect(Pointer ts,Pointer fr);
    @RuntimeImport
    public static native void fivmr_ReflectLog_allocReflect(Pointer ts,Pointer td);
    @RuntimeImport
    public static native void fivmr_ReflectLog_useReflectByName(Pointer ts,Pointer name);
    @RuntimeImport
    public static native void fivmr_ReflectLog_useReflect(Pointer ts,Pointer td);

    @StackAllocation
    public static void fivmr_ReflectLog_useReflectByName(Pointer ts,
                                                         String name) {
        fivmr_ReflectLog_useReflectByName(ts,getCStringFullStack(name));
    }

    @Export
    public static void runRunnable(Object r) {
	((Runnable)r).run();
    }

    @RuntimeImport
    private static native Pointer fivmr_homeDir(Pointer p);
    
    public static String homeDir() {
	return fromCString(fivmr_homeDir(Magic.getPayload()));
    }
    
    @Import
    @GodGiven
    public static native Pointer fivmr_TypeContext_create(Pointer vm,
                                                          ClassLoader classLoader);
    
    @Export
    @UseObjectsNotHandles
    @NoExecStatusTransition
    @ExcludeUnlessSet({"OPEN_WORLD"})
    public static Class< ? > fivmRuntime_loadClass(Pointer context,
                                                   Object classLoader,
                                                   Pointer nameCstr) {
        String name=fromCStringFull(nameCstr);
        try {
            if (name.charAt(0)!='L' ||
                name.charAt(name.length()-1)!=';') {
                throw new fivmError("Misformatted class name: "+name);
            }
            String jname=name.substring(1,name.length()-1).replace('/','.');
            Class<?> result=java.lang.fivmSupport.loadClass((ClassLoader)classLoader,jname);
            if (result==null) {
                throw new NullPointerException("Class loader returned null when attempting "+
                                               "to load "+name);
            }
            if (!result.getName().equals(jname)) {
                throw new LinkageError("Expecting to load class called "+jname+", but got "+
                                       result.getName());
            }
            return result;
        } catch (ClassNotFoundException e) {
            NoClassDefFoundError e2=new NoClassDefFoundError("Could not find "+name);
            e2.initCause(e);
            throw e2;
        } catch (LinkageError e) {
            throw e;
        } catch (Throwable e) {
            try {
                log(fivmRuntime.class,0,"Error in fivmRuntime_loadClass: "+e);
            } catch (Throwable e2) {
                try {
                    log(fivmRuntime.class,0,"Error printing error in fivmRuntime_loadClass");
                } catch (Throwable e3) {
                    // oh noes!
                }
            }
            try {
                NoClassDefFoundError e2=new NoClassDefFoundError("Could not load "+name);
                e2.initCause(e);
                throw e2;
            } catch (NoClassDefFoundError e2) {
                throw e2;
            } catch (Throwable e2) {
                try {
                    log(fivmRuntime.class,0,"Error in fivmRuntime_loadClass while handling error "+e+": "+e2);
                } catch (Throwable e3) {
                    try {
                        log(fivmRuntime.class,0,"Error printing error in fivmRuntime_loadClass, while attempting to rethrow error");
                    } catch (Throwable e4) {
                        // oh noes!
                    }
                }
                uncheckedThrow(e2);
                return null; // unreached
            }
        }
    }
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    private static native Pointer fivmr_TypeData_define(Pointer ctx,
                                                        Pointer td);

    @ExcludeUnlessSet({"CLASSLOADING"})
    static class BasicFieldRec {
        int flags;
        UTF8Sequence name;
        UTF8Sequence desc;
        BasicFieldRec next;
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    static class BasicMethodRec {
        int flags;
        UTF8Sequence name;
        UTF8Sequence desc;
        BasicMethodRec next;
        int maxLocals=-1;
        int maxStack=-1;
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    static class BasicClassData {
        int flags;
        UTF8Sequence name;
        UTF8Sequence filename;
        UTF8Sequence superclass;
        UTF8Sequence[] superinterfaces;
        int nFields;
        int nMethods;
        BasicFieldRec fields;
        BasicMethodRec methods;
        boolean hasJSR;
        boolean hasFinalize;
        
        void addField(BasicFieldRec bfr) {
            bfr.next=fields;
            fields=bfr;
            nFields++;
        }
        
        void addMethod(BasicMethodRec bmr) {
            bmr.next=methods;
            methods=bmr;
            nMethods++;
            if (Settings.FINALIZATION_SUPPORTED &&
                bmr.name.equals(UTF8Sequence.finalize) &&
                bmr.desc.equals(UTF8Sequence.Thunk)) {
                hasFinalize=true;
            }
        }
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    static class FieldRecOMWrap implements OMField {
        ClassDataOMWrap clazz;
        Pointer fr;
        
        FieldRecOMWrap(ClassDataOMWrap clazz,
                       Pointer fr) {
            this.clazz=clazz;
            this.fr=fr;
        }
        
        public OMClass getClazz() {
            return clazz;
        }
        
        public boolean shouldExist() {
            return true;
        }
        
        public boolean isTraced() {
            return (fivmr_FieldRec_flags(fr)&FBF_UNTRACED)==0;
        }
        
        public boolean isStatic() {
            return (fivmr_FieldRec_flags(fr)&BF_STATIC)!=0;
        }
        
        public int size() {
            return fivmr_TypeStub_refSize(fivmr_FieldRec_type(fr));
        }
        
        public int location() {
            int result=fivmr_FieldRec_location(fr).castToInt();
            if (result<0) {
                throw new fivmError("location is negative");
            }
            return result;
        }
        
        public void setLocation(int location) {
            CType.put(fr,"fivmr_FieldRec","location",Pointer.fromInt(location));
        }
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    static class ClassDataOMWrap implements OMClass {
        List< OMField > fields;
        OMField[] laidOutFields;
        Pointer td;
        
        void checkFields() {
            if (fields==null) {
                fields=new ArrayList< OMField >();
                for (int i=0;i<fivmr_TypeData_numFields(td);++i) {
                    fields.add(new FieldRecOMWrap(this,fivmr_TypeData_field(td,i)));
                }
            }
        }

        ClassDataOMWrap(Pointer td) {
            this.td=td;
        }
        
        public List< OMField > omFields() {
            checkFields();
            return fields;
        }
        
        public OMField[] omLaidOutFields() {
            return laidOutFields;
        }
        
        public void omSetLaidOutFields(OMField[] fields) {
            this.laidOutFields=fields;
        }
        
        public OMField[] omAllLaidOutFields() {
            Pointer td=this.td;
            ArrayList< OMField > resultList=new ArrayList< OMField >();
            while (td!=getTopTD()) {
                for (OMField f : new ClassDataOMWrap(td).omFields()) {
                    if (!f.isStatic()) {
                        resultList.add(f);
                    }
                }
                td=fivmr_TypeData_parent(td);
            }
            Collections.sort(
                resultList,
                new Comparator< OMField >(){
                    public int compare(OMField a,OMField b) {
                        // this could be made more efficient, probably...
                        return new Integer(a.location()).compareTo(b.location());
                    }
                });
            OMField[] result=new OMField[resultList.size()];
            resultList.toArray(result);
            return result;
        }
        
        public boolean hasSuperclass() {
            return fivmr_TypeData_parent(td)!=getTopTD();
        }
        
        public OMClass getSuperclass() {
            if (hasSuperclass()) {
                return new ClassDataOMWrap(fivmr_TypeData_parent(td));
            } else {
                return null;
            }
        }
        
        public void setPayloadSize(int payloadSize) {
            CType.put(td,"fivmr_TypeData","size",payloadSize);
        }
        
        public int payloadSize() {
            return CType.getInt(td,"fivmr_TypeData","size");
        }
        
        public void setRequiredPayloadAlignment(int requiredPayloadAlignment) {
            CType.put(td,"fivmr_TypeData","requiredAlignment",(byte)requiredPayloadAlignment);
        }
        
        public int requiredPayloadAlignment() {
            return CType.getByte(td,"fivmr_TypeData","requiredAlignment");
        }
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    private static boolean equalsAny(UTF8Sequence a,UTF8Sequence[] bs) {
        for (UTF8Sequence b : bs) {
            if (a.equals(b)) {
                return true;
            }
        }
        return false;
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    private static BasicClassData parseBytecode(byte[] bytecode) {
        final BasicClassData bcd=new BasicClassData();
        
        new ClassReader(bytecode).accept(
            new EmptyVisitor() {
                public void visit(int version,
                                  int access,
                                  UTF8Sequence name,
                                  UTF8Sequence signature,
                                  UTF8Sequence superName,
                                  UTF8Sequence[] interfaces) {
                    bcd.name=name;
                    bcd.superclass=superName;
                    bcd.superinterfaces=interfaces;
                    
                    int typeKind=TBF_VIRTUAL;
                    int superMode=0;
                    int visibility=BF_PACKAGE;
                    
                    if ((access&Opcodes.ACC_FINAL)!=0) {
                        typeKind=TBF_FINAL;
                    }
                    if ((access&Opcodes.ACC_ABSTRACT)!=0) {
                        typeKind=TBF_ABSTRACT;
                    }
                    if ((access&Opcodes.ACC_INTERFACE)!=0) {
                        typeKind=TBF_INTERFACE;
                    }
                    if ((access&Opcodes.ACC_ANNOTATION)!=0) {
                        typeKind=TBF_ANNOTATION;
                    }
                    if ((access&Opcodes.ACC_SUPER)!=0) {
                        superMode=TBF_NEW_SUPER_MODE;
                    }
                    if ((access&Opcodes.ACC_PUBLIC)!=0) {
                        visibility=BF_PUBLIC;
                    }
                    
                    bcd.flags=typeKind|superMode|visibility;
                }
                public void visitSource(UTF8Sequence filename,
                                        UTF8Sequence debug) {
                    bcd.filename=filename;
                }
                public FieldVisitor visitField(int access,
                                               UTF8Sequence name,
                                               UTF8Sequence desc,
                                               UTF8Sequence signature,
                                               Object value) {
                    BasicFieldRec bfr=new BasicFieldRec();
                    bfr.name=name;
                    bfr.desc=desc;
                    
                    int binding=0;
                    int visibility=BF_PACKAGE;
                    int mutability=0;
                    int volatility=0;
                    int serializability=0;

		    if ((access&Opcodes.ACC_STATIC)!=0) {
			binding=BF_STATIC;
		    }
		    if ((access&Opcodes.ACC_PUBLIC)!=0) {
			visibility=BF_PUBLIC;
		    }
		    if ((access&Opcodes.ACC_PROTECTED)!=0) {
			visibility=BF_PROTECTED;
		    }
		    if ((access&Opcodes.ACC_PRIVATE)!=0) {
			visibility=BF_PRIVATE;
		    }
		    if ((access&Opcodes.ACC_FINAL)!=0) {
			mutability=FBF_FINAL;
		    }
		    if ((access&Opcodes.ACC_VOLATILE)!=0) {
			volatility=FBF_VOLATILE;
		    }
		    if ((access&Opcodes.ACC_TRANSIENT)!=0) {
			serializability=FBF_TRANSIENT;
		    }
                    
                    bfr.flags=binding|visibility|mutability|volatility|serializability;
                    
                    // FIXME: do we want to deal with Pointer etc here?
                    if (desc.byteAt(0)!='L' && desc.byteAt(0)!='[') {
                        bfr.flags|=FBF_UNTRACED;
                        bfr.flags|=FBF_NOT_A_REFERENCE;
                    }
                    
                    bcd.addField(bfr);
                    
                    return null;
                }
                public MethodVisitor visitMethod(int access,
                                                 UTF8Sequence name,
                                                 UTF8Sequence desc,
                                                 UTF8Sequence signature,
                                                 UTF8Sequence[] exceptions) {
                    final BasicMethodRec bmr=new BasicMethodRec();
                    bmr.name=name;
                    bmr.desc=desc;
                    
                    bmr.flags=TypeParsing.methodBindingFlagsFromBytecodeFlags(access);
                    
                    bcd.addMethod(bmr);
                    
                    return new EmptyVisitor() {
                        public void visitMaxs(int maxStack,int maxLocals) {
                            bmr.maxStack=maxStack;
                            bmr.maxLocals=maxLocals;
                        }
                        public void visitVarInsn(int opcode,int var) {
                            if (opcode==Opcodes.RET) {
                                bcd.hasJSR=true;
                            }
                        }
                        public void visitJumpInsn(int opcode,Label label) {
                            if (opcode==Opcodes.JSR) {
                                bcd.hasJSR=true;
                            }
                        }
                    };
                }
            },
            ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG);
        
        return bcd;
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static Class<?> defineClass(ClassLoader cl,String name,byte[] bytecode) {
        long before=Time.nanoTime();
        
        Pointer ctx=java.lang.fivmSupport.getClassLoaderData(cl);
        
        // extract the info we need
        BasicClassData bcd=parseBytecode(bytecode);
        
        if (bcd.hasJSR) {
            ClassWriter cw=new ClassWriter(0);
            new ClassReader(bytecode).accept(
                new ClassAdapter(cw) {
                    public MethodVisitor visitMethod(int access,
                                                     UTF8Sequence name,
                                                     UTF8Sequence desc,
                                                     UTF8Sequence signature,
                                                     UTF8Sequence[] exceptions) {
                        return new JSRInlinerAdapter(
                            super.visitMethod(
                                access,name,desc,signature,exceptions),
                            access,
                            name.toString(),
                            desc.toString(),
                            signature==null?null:signature.toString(),
                            UTF8Sequence.toString(exceptions));
                    }
                },
                0);
            bytecode=cw.toByteArray();
            bcd=parseBytecode(bytecode);
        }
        
        if (name!=null) {
            // confirm that it's got the right name
            if (!bcd.name.toString().equals(name.replace('.','/'))) {
                throw new NoClassDefFoundError("bad class name; class file reports "+bcd.name+" but "+name+" was provided");
            }
        }

        name=bcd.name.toString();

        // confirm that it's not circular.
        
        if (bcd.name.equals(bcd.superclass) ||
            equalsAny(bcd.name,bcd.superinterfaces)) {
            throw new NoClassDefFoundError("circular type");
        }
        
        // (attempt to) resolve supertypes
        
        Class<?> superclass=null;
        Class<?>[] superinterfaces=new Class<?>[bcd.superinterfaces.length];
        
        try {
            superclass=java.lang.fivmSupport.loadClass(cl,bcd.superclass.toString());
            if (superclass==null) {
                throw new NullPointerException();
            }
            for (int i=0;i<superinterfaces.length;++i) {
                superinterfaces[i]=java.lang.fivmSupport.loadClass(
                    cl,bcd.superinterfaces[i].toString());
                if (superinterfaces[i]==null) {
                    throw new NullPointerException();
                }
            }
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
        
        Pointer parentTD=typeDataFromClass(superclass);
        
        // create a clone helper if the class is cloneable.  this requires
        // figuring out if any of our supertypes are subtypes of cloneable.
        boolean cloneable=Cloneable.class.isAssignableFrom(superclass);
        if (!cloneable) {
            for (int i=0;i<superinterfaces.length;++i) {
                if (Cloneable.class.isAssignableFrom(superinterfaces[i])) {
                    cloneable=true;
                    break;
                }
            }
        }
        
        if (cloneable) {
            BasicMethodRec bmr=new BasicMethodRec();
            bmr.name=new UTF8Sequence(SMN_CLONE_HELPER);
            bmr.desc=new UTF8Sequence("()Ljava/lang/Object;");
            bmr.flags=BF_PUBLIC|MBF_VIRTUAL|MBF_SYNTHETIC|MBF_EXISTS|MBF_HAS_CODE;
            bcd.addMethod(bmr);
        }
        
        if (Settings.FINALIZATION_SUPPORTED &&
            ((fivmr_TypeData_flags(parentTD)&TBF_FINALIZABLE)!=0 ||
             bcd.hasFinalize)) {
            bcd.flags|=TBF_FINALIZABLE;
        }
        
        // figure out the vtable size
        int vtableLength=0;
        
        if ((bcd.flags&TBF_TYPE_KIND)==TBF_VIRTUAL ||
            (bcd.flags&TBF_TYPE_KIND)==TBF_FINAL ||
            (bcd.flags&TBF_TYPE_KIND)==TBF_ABSTRACT) {
            // the parent's vtable
            vtableLength=CType.getInt(parentTD,"fivmr_TypeData","vtableLength");
            
            for (BasicMethodRec bmr=bcd.methods;bmr!=null;bmr=bmr.next) {
                if ((bmr.flags&BF_STATIC)==0 &&
                    !bmr.name.equals(UTF8Sequence._init_)) {
                    
                    log(fivmRuntime.class,1,
                        "Looking for "+bmr.name+", "+bmr.desc);
                    Pointer preMR=findInstMethodNoIface(parentTD,bmr.name,bmr.desc);
                    log(fivmRuntime.class,1,
                        "Got: "+preMR.asLong());
                    
                    if (preMR==Pointer.zero()) {
                        if ((bcd.flags&TBF_TYPE_KIND)==TBF_FINAL ||
                            (bmr.flags&MBF_METHOD_KIND)==MBF_FINAL) {
                            // don't get an index
                        } else {
                            vtableLength++;
                        }
                    }
                }
            }
        }
        
        // allocate TypeData (the size will be the size of the parent typedata
        // plus the number of inst methods times the pointer size) */
        Pointer tdSize=fivmr_TypeData_sizeOfTypeDataForVTableLength(vtableLength);
        log(fivmRuntime.class,1,
            "Allocating TypeData of size "+tdSize.asLong()+
            ", with vtableLength = "+vtableLength);
        Pointer td=mallocZeroed(tdSize);
        try {
            CType.put(td,"fivmr_TypeData","state",Monitors.invalid());
            CType.put(td,"fivmr_TypeData","forward",td);
            CType.put(td,"fivmr_TypeData","flags",bcd.flags);
            CType.put(td,"fivmr_TypeData","name",getCStringFullCHeap("L"+name+";"));
            CType.put(td,"fivmr_TypeData","context",
                      ctx.add(CType.offsetof("fivmr_TypeContext","st")));
            CType.put(td,"fivmr_TypeData","inited",0);
            CType.put(td,"fivmr_TypeData","curIniter",Pointer.zero());
            if (bcd.filename!=null) {
                CType.put(td,"fivmr_TypeData","filename",getCStringFullCHeap(bcd.filename));
            }
            CType.put(td,"fivmr_TypeData","parent",parentTD);
            CType.put(td,"fivmr_TypeData","nSuperInterfaces",(char)superinterfaces.length);
            CType.put(td,"fivmr_TypeData","nDirectSubs",(char)0);
            
            Pointer superinterfacesTD=
                malloc(Pointer.fromInt(Pointer.size()).mul(superinterfaces.length));
            for (int i=0;i<superinterfaces.length;++i) {
                superinterfacesTD.add(Pointer.fromInt(Pointer.size()).mul(i)).store(
                    typeDataFromClass(superinterfaces[i]));
            }
            CType.put(td,"fivmr_TypeData","superInterfaces",superinterfacesTD);
            CType.put(td,"fivmr_TypeData","directSubs",Pointer.zero());
            
            HashSet< PointerBox > ilist=new HashSet< PointerBox >();
            if ((bcd.flags&TBF_TYPE_KIND)==TBF_INTERFACE ||
                (bcd.flags&TBF_TYPE_KIND)==TBF_ANNOTATION) {
                // interface
                
                // find all interfaces
                for (int i=0;i<superinterfaces.length;++i) {
                    Pointer i1=
                        superinterfacesTD.add(Pointer.fromInt(Pointer.size()).mul(i)).loadPointer();
                    ilist.add(new PointerBox(i1));
                    Pointer i2list=CType.getPointer(i1,"fivmr_TypeData","ilist");
                    for (int j=CType.getChar(i1,"fivmr_TypeData","ilistSize");j-->0;) {
                        Pointer i2=
                            i2list.add(Pointer.fromInt(Pointer.size()).mul(j)).loadPointer();
                        ilist.add(new PointerBox(i2));
                    }
                }
            } else {
                // class
                
                // build a hashset of my superclasses' ilists
                HashSet< PointerBox > superIlist=new HashSet< PointerBox >();
                for (Pointer curTD=parentTD;curTD!=getTopTD();curTD=fivmr_TypeData_parent(curTD)) {
                    Pointer parentIlist=CType.getPointer(curTD,"fivmr_TypeData","ilist");
                    for (int i=CType.getChar(curTD,"fivmr_TypeData","ilistSize");i-->0;) {
                        superIlist.add(
                            new PointerBox(
                                parentIlist.add(Pointer.fromInt(Pointer.size()).mul(i)).loadPointer()));
                    }
                }
                
                if (logLevel>=2) {
                    log(fivmRuntime.class,2,"Have superIlist = "+superIlist);
                }
                
                // find all added interfaces
                for (int i=0;i<superinterfaces.length;++i) {
                    Pointer i1=
                        superinterfacesTD.add(Pointer.fromInt(Pointer.size()).mul(i)).loadPointer();
                    if (!superIlist.contains(new PointerBox(i1))) {
                        ilist.add(new PointerBox(i1));
                        Pointer i2list=CType.getPointer(i1,"fivmr_TypeData","ilist");
                        for (int j=CType.getChar(i1,"fivmr_TypeData","ilistSize");j-->0;) {
                            Pointer i2=
                                i2list.add(Pointer.fromInt(Pointer.size()).mul(j)).loadPointer();
                            if (!superIlist.contains(new PointerBox(i2))) {
                                ilist.add(new PointerBox(i2));
                            }
                        }
                    }
                }
            }
            CType.put(td,"fivmr_TypeData","ilistSize",(char)ilist.size());
            
            if (ilist.size()!=0) {
                Pointer ilistTD=
                    malloc(Pointer.fromInt(Pointer.size()).mul(ilist.size()));
                Pointer ilistCur=ilistTD;
                for (PointerBox pb : ilist) {
                    ilistCur.store(pb.value());
                    ilistCur=ilistCur.add(Pointer.size());
                }
                CType.put(td,"fivmr_TypeData","ilist",ilistTD);
            } else {
                CType.put(td,"fivmr_TypeData","ilist",Pointer.zero());
            }
            
            int canonicalNumber;
            for (;;) {
                canonicalNumber=
                    CType.getInt(getPayload(),"fivmr_Payload","maxCanonicalNumber")+1;
                if (CType.weakCAS(getPayload(),"fivmr_Payload","maxCanonicalNumber",
                                  canonicalNumber-1,canonicalNumber)) {
                    break;
                }
            }
            CType.put(td,"fivmr_TypeData","canonicalNumber",canonicalNumber);
            CType.put(td,"fivmr_TypeData","numDescendants",0);
            // leave epochs blank...
            CType.put(td,"fivmr_TypeData","arrayElement",Pointer.zero());
            CType.put(td,"fivmr_TypeData","arrayType",Pointer.zero());
            
            // will correct these two later
            CType.put(td,"fivmr_TypeData","size",
                      CType.getInt(parentTD,"fivmr_TypeData","size")-
                      CType.getByte(parentTD,"fivmr_TypeData","sizeAlignDiff"));
            CType.put(td,"fivmr_TypeData","sizeAlignDiff",(byte)0);
            CType.put(td,"fivmr_TypeData","requiredAlignment",
                      CType.getByte(parentTD,"fivmr_TypeData","requiredAlignment"));
            
            CType.put(td,"fivmr_TypeData","refSize",(byte)Pointer.size());
            CType.put(td,"fivmr_TypeData","node",Pointer.zero());
            
            CType.put(td,"fivmr_TypeData","numMethods",(char)bcd.nMethods);
            CType.put(td,"fivmr_TypeData","numFields",(char)bcd.nFields);

            Pointer methods,fields;

            methods=mallocZeroed(Pointer.fromInt(bcd.nMethods).mul(Pointer.size()));
            CType.put(td,"fivmr_TypeData","methods",methods);
            
            fields=mallocZeroed(
                Pointer.fromInt(bcd.nFields).mul(CType.sizeof("fivmr_FieldRec")));
            CType.put(td,"fivmr_TypeData","fields",fields);
            
            // prep methods
            
            int i=0;
            BasicMethodRec bmr=bcd.methods;
            while (bmr!=null) {
                Pointer mrPtr=methods.add(Pointer.fromInt(Pointer.size()).mul(i));
                
                Pointer mr=mallocZeroed(CType.sizeof("fivmr_MethodRec"));
                
                mrPtr.store(mr);
                
                CType.put(mr,"fivmr_MethodRec","flags",bmr.flags);
                
                if (bmr.maxStack<0 && (bmr.flags&MBF_METHOD_IMPL)==MBF_BYTECODE) {
                    throw new fivmError("Failed to compute maxStack");
                }
                if (bmr.maxLocals<0 && (bmr.flags&MBF_METHOD_IMPL)==MBF_BYTECODE) {
                    throw new fivmError("Failed to compute maxLocals");
                }
                
                CType.put(mr,"fivmr_MethodRec","nStack",(char)bmr.maxStack);
                CType.put(mr,"fivmr_MethodRec","nLocals",(char)bmr.maxLocals);
                
                CType.put(mr,"fivmr_MethodRec","owner",td);
                CType.put(mr,"fivmr_MethodRec","name",getCStringFullCHeap(bmr.name));
                
                TypeParsing.MethodSigSeqs mss=TypeParsing.splitMethodSig(bmr.desc);

                CType.put(mr,"fivmr_MethodRec","result",findStub(ctx,mss.result()));
                CType.put(mr,"fivmr_MethodRec","nparams",mss.params().length);
                
                if (mss.params().length>0) {
                    Pointer params=mallocZeroed(
                        Pointer.fromInt(Pointer.size()).mul(mss.params().length));
                    CType.put(mr,"fivmr_MethodRec","params",params);
                    for (int j=0;j<mss.params().length;++j) {
                        params.add(Pointer.fromInt(j).mul(Pointer.size()))
                            .store(findStub(ctx,mss.params()[j]));
                    }
                } else {
                    CType.put(mr,"fivmr_MethodRec","params",Pointer.zero());
                }

                switch (bmr.flags&MBF_METHOD_IMPL) {
                case MBF_BYTECODE:
                case MBF_JNI:
                case MBF_SYNTHETIC: {
                    MachineCode mc=BaselineJIT.getJIT().createLoadThunkFor(mr);
                    
                    CType.put(mr,"fivmr_MethodRec","entrypoint",mc.getAddress());
                    
                    mc.addToMR(mr);
                    mc.destroy();
                    
                    CType.put(mr,"fivmr_MethodRec","location",Pointer.fromIntSignExtend(-1));
                    break;
                }
                case MBF_STUB: {
                    CType.put(mr,"fivmr_MethodRec","entrypoint",Pointer.zero());
                    CType.put(mr,"fivmr_MethodRec","location",Pointer.fromIntSignExtend(-1));
                    break;
                }
                default: throw new fivmError("unrecognized method flags: "+bmr.flags);
                }

                i++;
                bmr=bmr.next;
            }
            
            if (Settings.ASSERTS_ON && i!=bcd.nMethods) {
                throw new fivmError("nMethods = "+bcd.nMethods+" but i = "+i);
            }
            
            // create fields
            
            BasicFieldRec bfr=bcd.fields;
            i=0;
            while (bfr!=null) {
                Pointer fr=fields.add(CType.sizeof("fivmr_FieldRec").mul(i));
                
                CType.put(fr,"fivmr_FieldRec","owner",td);
                CType.put(fr,"fivmr_FieldRec","name",getCStringFullCHeap(bfr.name));
                CType.put(fr,"fivmr_FieldRec","flags",bfr.flags);
                
                Pointer ftype=findStub(ctx,bfr.desc);
                CType.put(fr,"fivmr_FieldRec","type",ftype);
                
                Pointer loc;
                if ((bfr.flags&BF_STATIC)!=0) {
                    if ((bfr.flags&FBF_UNTRACED)==0) {
                        loc=fivmr_TypeContext_addTracedField(ctx);
                    } else {
                        loc=fivmr_TypeContext_addUntracedField(
                            ctx,fivmr_TypeStub_refSize(ftype));
                    }
                } else {
                    loc=Pointer.fromIntSignExtend(-1);
                }
                
                CType.put(fr,"fivmr_FieldRec","location",loc);
                
                bfr=bfr.next;
                i++;
            }
            
            // perform field layout
            
            FieldLayerOuter.buildDefault().layOutFields(new ClassDataOMWrap(td));
            
            // align size
            
            int unalignedSize=CType.getInt(td,"fivmr_TypeData","size");
            int alignedSize=(unalignedSize+Pointer.size()-1)&~(Pointer.size()-1);
            
            CType.put(td,"fivmr_TypeData","size",alignedSize);
            CType.put(td,"fivmr_TypeData","sizeAlignDiff",(byte)(alignedSize-unalignedSize));
            
            // build GC maps
            
            CType.put(td,"fivmr_TypeData","gcMap",
                      GCMapBuilder.buildDefault().buildGCMap(
                          new ConstantAllocator< PointerBox >(){
                              public PointerBox makePtrConst(long value) {
                                  return new PointerBox(Pointer.fromLong(value));
                              }
                              public PointerBox allocList(String name,int[] values) {
                                  Pointer result=malloc(Pointer.fromInt(Pointer.size()).mul(values.length));
                                  for (int i=0;i<values.length;++i) {
                                      result.add(Pointer.fromInt(i).mul(Pointer.size())).store(
                                          values[i]);
                                  }
                                  return new PointerBox(result);
                              }
                          },
                          new ClassDataOMWrap(td)).value());
            
            // ok now make sure that we init the vtable length and we're DONE
            // FIXME: why aren't we just doing vtable building here?  wouldn't that
            // have been easier?  WHAT EV.

            CType.put(td,"fivmr_TypeData","vtableLength",vtableLength);
        
            // allocate Class instance and link it to the td, and link in the bytecode
            Class<?> klass=java.lang.fivmSupport.allocateClass(td);
            java.lang.fivmSupport.linkBytecode(td,bytecode);
            
            Pointer res;
            
            lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            try {
                res=fivmr_TypeData_define(ctx,td);
            } finally {
                unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            }
            
            if (res==Pointer.zero()) {
                throw new fivmError("Internal error while defining class");
            }
            
            if (res!=td) {
                throw new LinkageError("Class already defined");
            }
            
            Magic.hardUse(klass);
            
            long after=Time.nanoTime();
            log(fivmRuntime.class,1,
                "Defining "+klass+" ("+res.asLong()+") took "+(after-before)+" ns");

            return klass;
        } catch (Throwable e) {
            fivmr_TypeData_free(td);
            uncheckedThrow(e);
            return null; // not actually reached; just making javac happy
        }
    }
    
    @SuppressWarnings("unused")
    @Export
    private static void javaExit(int status) {
	try {
	    log(fivmRuntime.class,1,"Request to exit with status "+status+" from native code.");
	} catch (Throwable e) {
	    log(fivmRuntime.class,1,"Request to exit from native code, with VM trouble.");
	}
	System.exit(status);
    }
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    private static native boolean fivmr_TypeData_fixEntrypoint(Pointer td,
                                                               Pointer oldEntrypoint,
                                                               Pointer newEntrypoint);
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    private static void updateEntrypoint(Pointer startMR,
                                         Pointer newEntrypoint) {
        if (Settings.ASSERTS_ON) {
            if ((fivmr_MethodRec_flags(startMR)&MBF_METHOD_IMPL)==MBF_STUB) {
                throw new fivmError("trying to update the entrypoint of a method stub");
            }
            // FIXME: other useful assertions?
        }
        
        Pointer oldEntrypoint=CType.getPointer(startMR,"fivmr_MethodRec","entrypoint");
        CType.put(startMR,"fivmr_MethodRec","entrypoint",newEntrypoint);

        MyStack< PointerBox > worklist=new MyStack< PointerBox >();
        
        worklist.push(new PointerBox(fivmr_MethodRec_owner(startMR)));
        
        while (!worklist.empty()) {
            Pointer td=worklist.pop().value();
            
            if (fivmr_TypeData_fixEntrypoint(td,oldEntrypoint,newEntrypoint)) {
                // a true return means that oldEntrypoint was found.  if it was,
                // then it means that the method was not overridden, in which case
                // we need to inspect the subtypes as well.
                for (int i=0;
                     i<(int)CType.getChar(td,"fivmr_TypeData","nDirectSubs");
                     ++i) {
                    worklist.push(
                        new PointerBox(
                            CType.getPointer(td,"fivmr_TypeData","directSubs")
                            .add(Pointer.fromInt(i).mul(Pointer.size()))
                            .loadPointer()));
                }
            }
        }
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static void handlePatchPointImpl(Pointer debugID,
                                            Pointer className,
                                            Pointer fromWhereDescr,
                                            int bcOffset,
                                            Pointer patchThunkPtrPtr,
                                            Pointer origPatchThunk) {
        if (!Settings.CLASSLOADING) {
            throwNoClassDefFoundError(fromCString(className),
                                      fromCString(fromWhereDescr));
        } else {
            BaselineJIT jit=BaselineJIT.getJIT();
        
            if (patchThunkPtrPtr.loadPointer()!=origPatchThunk) {
                return;
            }
            
            // what this story should look like:
            // 1) acquire the thunk lock and check if this patch thunk has
            //    been resolved
            // 2) make sure that the baseline JIT has already generated
            //    an entrypoint for the method in question.  if it has not,
            //    then have it generate the code and optionally set it
            //    as the method's entrypoint.  whether or not we do this
            //    should probably depend on whether or not the source of
            //    this call is a patch thunk, which can be determined by
            //    looking at the debugID's pc: if it's zero, then we know
            //    that it's sensible to replace the installed entrypoint.
            // 3) call into the baseline JIT to create the patch
            // 4) link the prologue of the resulting code to the
            //    patchThunkPtrPtr
            
            long before=Time.nanoTime();
            
            Pointer methodRec;

            lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            try {
                // check if it's already resolved
                if (patchThunkPtrPtr.loadPointer()!=origPatchThunk) {
                    return;
                }
                
                // extract debugID and various infos
                Pointer debugRec=fivmr_DebugRec_lookup(getVM(),debugID);
                
                methodRec=
                    fivmr_MachineCode_decodeMethodRec(
                        fivmr_DebugRec_getMethod(debugRec));
                
                int bytecodePC=fivmr_DebugRec_getBytecodePC(debugRec);
                
                // generate a baseline entrypoint if there isn't one already
                if (!fivmr_MethodRec_hasMC(methodRec,
                                           CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"),
                                           CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"))) {
                    MachineCode code=jit.createEntrypointFor(methodRec);
                    
                    if (bytecodePC==0) {
                        // switch entrypoints if we know that this patch will be
                        // hit right from the start
                        updateEntrypoint(methodRec,code.getAddress());
                    }
                    
                    code.addToMR(methodRec);
                    code.destroy();
                }
                
                // create the patch and link it
                MachineCode code=jit.createPatchToCodeFor(methodRec,bytecodePC);
                
                patchThunkPtrPtr.store(code.getAddress());
                
                code.addToMR(methodRec);
                code.destroy();
            } finally {
                unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            }
            
            long after=Time.nanoTime();
            log(fivmRuntime.class,1,
                "Handling patch point in "+fromCStringFull(fivmr_MethodRec_describe(methodRec))+
                " took "+(after-before)+" ns");
        }
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static void handleLoadThunk(Pointer methodRec) {
        BaselineJIT jit=BaselineJIT.getJIT();
        
        // figure out if there is already a code that could be used as a possible
        // entrypoint
        if (fivmr_MethodRec_hasMC(methodRec,
                                  CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"),
                                  CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"))) {
            return;
        }
        
        long before=Time.nanoTime();
        
        resolveAndCheckInit(fivmr_MethodRec_owner(methodRec));
        
        lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
        try {
            if (fivmr_MethodRec_hasMC(methodRec,
                                      CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"),
                                      CVar.getInt("FIVMR_MC_POSSIBLE_ENTRYPOINT"))) {
                return;
            }
            
            MachineCode code;
            try {
                code=jit.createEntrypointFor(methodRec);
            } catch (Throwable e) {
                code=jit.createExceptionThrow(methodRec,
                                              ExceptionThrower.build(e));
            }

            updateEntrypoint(methodRec,code.getAddress());
            
            code.addToMR(methodRec);
            code.destroy();
        } finally {
            unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
        }
        
        long after=Time.nanoTime();
        log(fivmRuntime.class,1,
            "Handling load thunk for "+fromCStringFull(fivmr_MethodRec_describe(methodRec))+
            " took "+(after-before)+" ns");
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static Pointer resolveField(Pointer methodRec,
                                       boolean isStatic,
                                       Pointer ownerType,
                                       UTF8Sequence desc,
                                       UTF8Sequence name) {
        Pointer fr;
        if (isStatic) {
            fr=findStaticField(ownerType,name,desc);
        } else {
            fr=findInstField(ownerType,name,desc);
        }
        
        if (fr==Pointer.zero()) {
            // throw something
            throw new NoSuchFieldError("Could not find field "+
                                       fromCStringFull(fivmr_TypeData_name(ownerType))+
                                       "/"+name+"/"+desc);
        }
        
        // assert linker constraints
        unionStubs(findStub(fivmr_TypeData_getContext(
                                fivmr_MethodRec_owner(methodRec)),
                            desc),
                   fivmr_FieldRec_type(fr));
        
        return fr;
    }
    
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static Pointer resolveMethod(Pointer methodRec,
                                        boolean isStatic,
                                        Pointer ownerType,
                                        UTF8Sequence name,
                                        UTF8Sequence desc) {
        Pointer mr;
        
        if (isStatic) {
            mr=findStaticMethod(ownerType,name,desc);
        } else {
            mr=findInstMethod(ownerType,name,desc);
        }
                
        if (mr==Pointer.zero()) {
            throw new NoSuchMethodError("Could not find method "+
                                        fromCStringFull(fivmr_TypeData_name(ownerType))+
                                        "/"+name+desc);
        }
                
        // assert loader constraints
        TypeParsing.MethodSigSeqs sigs=TypeParsing.splitMethodSig(desc);
        Pointer ctx=fivmr_TypeData_getContext(fivmr_MethodRec_owner(methodRec));
                
        unionStubs(CType.getPointer(mr,"fivmr_MethodRec","result"),
                   findStub(ctx,sigs.result()));
        if (Settings.ASSERTS_ON &&
            sigs.nParams()
            != CType.getInt(mr,"fivmr_MethodRec","nparams")) {
            throw new fivmError("Mismatch in number of parameters");
        }
        Pointer paramlist=
            CType.getPointer(mr,"fivmr_MethodRec","params");
        for (int i=0;i<sigs.nParams();++i) {
            unionStubs(paramlist.add(
                           Pointer.fromInt(i).mul(
                               Pointer.size())).loadPointer(),
                       findStub(ctx,sigs.param(i)));
        }
                
        return mr;
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static void handleFieldResolution(Pointer returnAddr,
                                             Pointer bfa) {
        BaselineJIT jit=BaselineJIT.getJIT();
        
        if (jit.fieldAccessPatched(returnAddr)) {
            return;
        }
        
        long before=Time.nanoTime();
        
        Pointer mr=Pointer.zero();
        
        Pointer debugRec=CType.getPointer(bfa,"fivmr_BaseFieldAccess","debugID");
        
        Pointer method=fivmr_DebugRec_getMethod(debugRec);
        
        int flags=method.loadInt();
        if ((flags&MBF_COOKIE)==MBF_COOKIE) {
            throw new fivmError("code appears not to be generated by baseline JIT.");
        }
        
        Pointer machineCode=method;
        Pointer methodRec=mr=CType.getPointer(machineCode,"fivmr_MachineCode","mr");
        
        byte[] bytecode=java.lang.fivmSupport.getBytecode(
            CType.getPointer(methodRec,"fivmr_MethodRec","owner"));
        
        int fat=CType.getInt(bfa,"fivmr_BaseFieldAccess","fat");
        
        Pointer ownerStub=CType.getPointer(bfa,"fivmr_BaseFieldAccess","owner");
        UTF8Sequence desc=
            new UTF8BCSequence(bytecode,
                               CType.getInt(bfa,"fivmr_BaseFieldAccess","descAddr"));
        UTF8Sequence name=
            new UTF8BCSequence(bytecode,
                               CType.getInt(bfa,"fivmr_BaseFieldAccess","nameAddr"));
        int stackHeight=CType.getInt(bfa,"fivmr_BaseFieldAccess","stackHeight");
        int recvType=CType.getInt(bfa,"fivmr_BaseFieldAccess","recvType");
        int dataType=CType.getInt(bfa,"fivmr_BaseFieldAccess","dataType");
        
        Pointer fr=Pointer.zero();
        Throwable toRethrow=null;
        
        try {
            Pointer ownerType=resolveStub(ownerStub);
            resolveType(ownerType);
            if (Protocols.isStatic(Protocols.opcodeForFat(fat))) {
                checkInit(ownerType);
            }
            
            fr=resolveField(methodRec,
                            Protocols.isStatic(
                                Protocols.opcodeForFat(fat)),
                            ownerType,
                            desc,
                            name);
            
            if (Settings.ASSERTS_ON && fr==Pointer.zero()) {
                throw new fivmError("resolveField returned zero");
            }
        } catch (Throwable e) {
            log(fivmRuntime.class,1,
                "Got an exception while performing field resolution: "+e);
            toRethrow=e;
        }
                
        lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
        try {
            if (jit.fieldAccessPatched(returnAddr)) {
                return;
            }
            
            MachineCode code=null;
        
            if (fr!=Pointer.zero()) {
                try {
                    code=jit.createFieldAccess(methodRec,
                                               fat,
                                               fr,
                                               returnAddr,
                                               stackHeight,
                                               recvType,
                                               dataType);
                } catch (Throwable e) {
                    log(fivmRuntime.class,1,
                        "Got an exception while generating field access code: "+e);
                    toRethrow=e;
                }
            }
            
            if (toRethrow!=null) {
                code=jit.createExceptionThrowSub(
                    methodRec,
                    ExceptionThrower.build(toRethrow));
            }
        
            code.addToMC(machineCode);
        
            // FIXME should do some synchronization

            jit.patchFieldAccessTo(returnAddr, code.getAddress());
            code.destroy();
            
            if (toRethrow!=null) {
                // why? because the first time the user sees this, we want him to
                // get the original exception, which may have more contextual
                // details than the (more space-safe) one in ExceptionThrower.
                // note that we DO NOT do this in MethodImplGenerator, and with
                // good reason: there's no point.  the only time when additional
                // contextual info would be useful is if we're also performing
                // class initialization or resolution, which we only do here.
                uncheckedThrow(toRethrow);
            }
            
        } finally {
            unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            
            long after=Time.nanoTime();
            log(fivmRuntime.class,1,
                "Handling field resolution in "+fromCStringFull(fivmr_MethodRec_describe(mr))+
                " took "+(after-before)+" ns");
        }
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static void handleMethodResolution(Pointer debugID,
                                              Pointer returnAddr,
                                              Pointer bmc) {
        BaselineJIT jit=BaselineJIT.getJIT();
        
        if (Settings.ASSERTS_ON && debugID.and(Pointer.fromInt(1))==Pointer.fromInt(1)) {
            throw new fivmError("code appears not to be generated by baseline JIT");
        }
        
        int mct=CType.getInt(bmc,"fivmr_BaseMethodCall","mct");
        
        if (jit.methodCallPatched(returnAddr,mct)) {
            return;
        }
        
        long before=Time.nanoTime();
        
        Pointer methodRec=Pointer.zero();
        
        Pointer debugRec=debugID;
        
        Pointer method=fivmr_DebugRec_getMethod(debugRec);
        
        int flags=method.loadInt();
        if ((flags&MBF_COOKIE)==MBF_COOKIE) {
            throw new fivmError("code appears not to be generated by baseline JIT.");
        }
        
        Pointer machineCode=method;
        methodRec=CType.getPointer(machineCode,"fivmr_MachineCode","mr");
        
        byte[] bytecode=java.lang.fivmSupport.getBytecode(
            CType.getPointer(methodRec,"fivmr_MethodRec","owner"));
        
        Pointer ownerStub=CType.getPointer(bmc,"fivmr_BaseMethodCall","owner");
        UTF8Sequence desc=
            new UTF8BCSequence(bytecode,
                               CType.getInt(bmc,"fivmr_BaseMethodCall","descAddr"));
        UTF8Sequence name=
            new UTF8BCSequence(bytecode,
                               CType.getInt(bmc,"fivmr_BaseMethodCall","nameAddr"));
        int stackHeight=CType.getInt(bmc,"fivmr_BaseMethodCall","stackHeight");
        
        int opcode=Protocols.opcodeForMct(mct);
        
        Pointer mr=Pointer.zero();
        Throwable toRethrow=null;
        
        try {
            Pointer ownerType=resolveStub(ownerStub);
            resolveAndCheckInit(ownerType);
            
            mr=resolveMethod(methodRec,
                             Protocols.isStatic(opcode),
                             ownerType,
                             name,
                             desc);
            if (Settings.ASSERTS_ON && mr==Pointer.zero()) {
                throw new fivmError("resolveMethod returned zero");
            }
        } catch (Throwable e) {
            log(fivmRuntime.class,1,
                "Got an exception while performing method resolution: "+e);
            toRethrow=e;
        }

        lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
        try {
            if (jit.methodCallPatched(returnAddr,mct)) {
                return;
            }
            
            Pointer ifaceCall=Pointer.zero();
            MachineCode code=null;
            
            if (mr!=Pointer.zero()) {
                try {
                    if (opcode==Opcodes.INVOKEINTERFACE) {
                        ifaceCall=getInterfaceResolutionFor(mr);
                    } else {
                        code=jit.createMethodCall(methodRec,
                                                  mct,
                                                  mr,
                                                  returnAddr,
                                                  stackHeight);
                    }
                } catch (Throwable e) {
                    log(fivmRuntime.class,1,
                        "Got an exception while generating method call code: "+e);
                    toRethrow=e;
                }
            }
            
            if (toRethrow!=null) {
                code=jit.createExceptionThrowSub(
                    methodRec,
                    ExceptionThrower.build(toRethrow));
            }

            if (ifaceCall!=Pointer.zero()) {
                jit.patchMethodCallTo(returnAddr, mct, ifaceCall);
            } else {
                code.addToMC(machineCode);
                
                // FIXME sync!!
                
                // this is kinda goofy ... the code will end up "calling"
                // this code even though it's meant to be jumped to.
                // but that's perfectly OK.
                jit.patchMethodCallTo(returnAddr, mct, code.getAddress());
            }
            
            if (toRethrow!=null) {
                // this has a point, trust me, dude.
                uncheckedThrow(toRethrow);
            }
        } finally {
            unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            
            long after=Time.nanoTime();
            log(fivmRuntime.class,1,
                "Handling method resolution in "+fromCStringFull(fivmr_MethodRec_describe(methodRec))+
                " took "+(after-before)+" ns");
        }
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static void handleArrayAlloc(Pointer returnAddr,
                                        Pointer baa) {
        BaselineJIT jit=BaselineJIT.getJIT();
        
        if (jit.arrayAllocPatched(returnAddr)) {
            return;
        }
        
        long before=Time.nanoTime();
        
        Pointer methodRec=Pointer.zero();
        
        Pointer debugRec=CType.getPointer(baa,"fivmr_BaseArrayAlloc","debugID");
        
        if (Settings.ASSERTS_ON && debugRec==Pointer.zero()) {
            throw new fivmError("debugID is zero");
        }
        
        Pointer method=fivmr_DebugRec_getMethod(debugRec);
        
        int flags=method.loadInt();
        if ((flags&MBF_COOKIE)==MBF_COOKIE) {
            throw new fivmError("code appears not to be generated by baseline JIT.");
        }
        
        Pointer machineCode=method;
        methodRec=CType.getPointer(machineCode,"fivmr_MachineCode","mr");
        
        Pointer typeStub=CType.getPointer(baa,"fivmr_BaseArrayAlloc","type");
        int stackHeight=CType.getInt(baa,"fivmr_BaseArrayAlloc","stackHeight");
        
        Throwable toRethrow=null;
        Pointer type=Pointer.zero();
        
        try {
            type=resolveStub(typeStub);
            resolveType(type);
        } catch (Throwable e) {
            log(fivmRuntime.class,1,
                "Got an exception while performing type resolution for array alloc: "+e);
            toRethrow=e;
        }

        lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
        try {
            if (jit.arrayAllocPatched(returnAddr)) {
                return;
            }
            
            MachineCode code=null;
            
            if (toRethrow==null) {
                try {
                    code=jit.createArrayAlloc(methodRec,
                                              type,
                                              returnAddr,
                                              stackHeight,
                                              debugRec);
                } catch (Throwable e) {
                    log(fivmRuntime.class,1,
                        "Got an exception while emitting array alloc code: "+e);
                    toRethrow=e;
                }
            }
            
            if (toRethrow!=null) {
                code=jit.createExceptionThrowSub(
                    methodRec,
                    ExceptionThrower.build(toRethrow));
            }

            code.addToMC(machineCode);
            
            // FIXME sync!!
            
            jit.patchArrayAllocTo(returnAddr, code.getAddress());
            
            if (toRethrow!=null) {
                // this has a point, trust me, dude.
                uncheckedThrow(toRethrow);
            }
        } finally {
            unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            
            long after=Time.nanoTime();
            log(fivmRuntime.class,1,
                "Handling array alloc in "+fromCStringFull(fivmr_MethodRec_describe(methodRec))+
                " took "+(after-before)+" ns");
        }
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static void handleObjectAlloc(Pointer returnAddr,
                                         Pointer boa) {
        BaselineJIT jit=BaselineJIT.getJIT();
        
        if (jit.objectAllocPatched(returnAddr)) {
            return;
        }
        
        long before=Time.nanoTime();
        
        Pointer methodRec=Pointer.zero();
        
        Pointer debugRec=CType.getPointer(boa,"fivmr_BaseObjectAlloc","debugID");
        
        if (Settings.ASSERTS_ON && debugRec==Pointer.zero()) {
            throw new fivmError("debugID is zero");
        }

        Pointer method=fivmr_DebugRec_getMethod(debugRec);
        
        int flags=method.loadInt();
        if ((flags&MBF_COOKIE)==MBF_COOKIE) {
            throw new fivmError("code appears not to be generated by baseline JIT.");
        }
        
        Pointer machineCode=method;
        methodRec=CType.getPointer(machineCode,"fivmr_MachineCode","mr");
        
        Pointer typeStub=CType.getPointer(boa,"fivmr_BaseObjectAlloc","type");
        int stackHeight=CType.getInt(boa,"fivmr_BaseObjectAlloc","stackHeight");
        
        Pointer type=Pointer.zero();
        Throwable toRethrow=null;
        
        try {
            type=resolveStub(typeStub);
            resolveAndCheckInit(type);
        } catch (Throwable e) {
            log(fivmRuntime.class,1,
                "Got an exception while performing type resolution for object alloc: "+e);
            toRethrow=e;
        }
                
        lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
        try {
            if (jit.objectAllocPatched(returnAddr)) {
                return;
            }
            
            MachineCode code=null;
            
            if (toRethrow==null) {
                try {
                    code=jit.createObjectAlloc(methodRec,
                                               type,
                                               returnAddr,
                                               stackHeight,
                                               debugRec);
                } catch (Throwable e) {
                    log(fivmRuntime.class,1,
                        "Got an exception while emitting object alloc code: "+e);
                    toRethrow=e;
                }
            }
            
            if (toRethrow!=null) {
                code=jit.createExceptionThrowSub(
                    methodRec,
                    ExceptionThrower.build(toRethrow));
            }
            
            code.addToMC(machineCode);
            
            // FIXME sync!!
            
            jit.patchObjectAllocTo(returnAddr, code.getAddress());
            
            if (toRethrow!=null) {
                // this has a point, trust me, dude.
                uncheckedThrow(toRethrow);
            }
        } finally {
            unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            
            long after=Time.nanoTime();
            log(fivmRuntime.class,1,
                "Handling object alloc in "+fromCStringFull(fivmr_MethodRec_describe(methodRec))+
                " took "+(after-before)+" ns");
        }
    }
    
    @Export
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @NoInline
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static void handleInstanceof(Pointer returnAddr,
                                        Pointer bio) {
        BaselineJIT jit=BaselineJIT.getJIT();
        
        if (jit.instanceofPatched(returnAddr)) {
            return;
        }
        
        long before=Time.nanoTime();
        
        Pointer methodRec=Pointer.zero();
        
        Pointer debugRec=CType.getPointer(bio,"fivmr_BaseInstanceof","debugID");
        
        if (Settings.ASSERTS_ON && debugRec==Pointer.zero()) {
            throw new fivmError("debugID is zero");
        }
        
        Pointer method=fivmr_DebugRec_getMethod(debugRec);
        
        int flags=method.loadInt();
        if ((flags&MBF_COOKIE)==MBF_COOKIE) {
            throw new fivmError("code appears not to be generated by baseline JIT.");
        }
        
        Pointer machineCode=method;
        methodRec=CType.getPointer(machineCode,"fivmr_MachineCode","mr");
        
        MachineCode code=null;
        
        Pointer typeStub=CType.getPointer(bio,"fivmr_BaseInstanceof","type");
        int stackHeight=CType.getInt(bio,"fivmr_BaseInstanceof","stackHeight");
        int iot=CType.getInt(bio,"fivmr_BaseInstanceof","iot");
        
        Throwable toRethrow=null;
        Pointer type=Pointer.zero();
        
        try {
            type=resolveStub(typeStub);
            resolveType(type);
        } catch (Throwable e) {
            log(fivmRuntime.class,1,
                "Got an exception while performing type resolution for instanceof: "+e);
            toRethrow=e;
        }

        lockWithHandshake(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
        try {
            if (jit.instanceofPatched(returnAddr)) {
                return;
            }

            if (toRethrow==null) {
                try {
                    code=jit.createInstanceof(methodRec,
                                              iot,
                                              type,
                                              returnAddr,
                                              stackHeight,
                                              debugRec);
                } catch (Throwable e) {
                    log(fivmRuntime.class,1,
                        "Got an exception while emitting code for instanceof: "+e);
                    toRethrow=e;
                }
            }
            
            if (toRethrow!=null) {
                code=jit.createExceptionThrowSub(
                    methodRec,
                    ExceptionThrower.build(toRethrow));
            }
            
            code.addToMC(machineCode);
            
            // FIXME sync!!
            
            jit.patchInstanceofTo(returnAddr, code.getAddress());
            
            if (toRethrow!=null) {
                // this has a point, trust me, dude.
                uncheckedThrow(toRethrow);
            }
        } finally {
            unlock(getVM().add(CType.offsetof("fivmr_VM","thunkingLock")));
            
            long after=Time.nanoTime();
            log(fivmRuntime.class,1,
                "Handling instanceof in "+fromCStringFull(fivmr_MethodRec_describe(methodRec))+
                " took "+(after-before)+" ns");
        }
    }
    
    @RuntimeImport
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static native void fivmr_MachineCode_down(Pointer mc);
    
    // call only while holding the relevant locks
    @ExcludeUnlessSet({"CLASSLOADING"})
    public static Pointer getInterfaceResolutionFor(Pointer mr) {
        BaselineJIT jit=BaselineJIT.getJIT();
        
        Pointer result=fivmr_MethodRec_findMC(mr,
                                              CVar.getInt("FIVMR_MC_KIND"),
                                              CVar.getInt("FIVMR_MC_BASELINE"));
        if (result==Pointer.zero()) {
            long before=Time.nanoTime();
            
            MachineCode code=jit.createInterfaceResolutionFor(mr);
            
            code.addToMR(mr);
            
            result=code.getMachineCode();
            code.destroy();

            long after=Time.nanoTime();
            log(fivmRuntime.class,1,
                "Creating interface method resolver for "+fromCStringFull(fivmr_MethodRec_describe(mr))+
                " took "+(after-before)+" ns");
        } else {
            // findMC ups it, so we down it.
            fivmr_MachineCode_down(result);
        }
        
        return CType.getPointer(result,"fivmr_MachineCode","code");
    }
    
    public static void setVMProperties(Properties properties) {
	properties.setProperty("java.vendor",com.fiji.fivm.Config.VENDOR);
	properties.setProperty("java.vendor.url",com.fiji.fivm.Config.VENDOR_WWW);
	properties.setProperty("java.home",homeDir());
	properties.setProperty("java.vm.version",com.fiji.fivm.Config.VERSION.substring(1));
	properties.setProperty("java.vm.vendor",com.fiji.fivm.Config.VENDOR);
	properties.setProperty("java.vm.name","fivm");
    }
    
    public static void setArgumentProperties(Properties properties) {
	if (Settings.SUPPORT_ENV_BASED_SYS_PROPS) {
            ConfigMapNode props=
                ConfigMapNode.parse(libc.getenv("FIVMR_SYS_PROPS","{}"));
            for (Map.Entry< String, ConfigNode > e : props.entrySet()) {
                properties.setProperty(
                    e.getKey(),
                    e.getValue().asAtom().getString());
            }
        }
    }
}

