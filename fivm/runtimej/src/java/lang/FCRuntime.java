/*
 * FCRuntime.java
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

import static com.fiji.fivm.r1.fivmRuntime.fromCStringFull;
import static com.fiji.fivm.r1.fivmRuntime.getCStringFullStack;
import static com.fiji.fivm.r1.fivmRuntime.log;
import static com.fiji.fivm.r1.fivmRuntime.logPrint;

import java.io.File;
import java.io.IOException;

import com.fiji.fivm.r1.*;
import com.fiji.fivm.util.DisableEnableExit;
import com.fiji.fivm.Settings;

final class FCRuntime {
    private FCRuntime() {}
    
    @RuntimeImport
    static native Pointer fivmr_JNI_libPrefix();
    
    @RuntimeImport
    static native Pointer fivmr_JNI_libSuffix();
    
    static final String libPrefix=fromCStringFull(fivmr_JNI_libPrefix(),"lib");
    static final String libSuffix=fromCStringFull(fivmr_JNI_libSuffix(),".so");
    
    @RuntimeImport
    static native int fivmr_availableProcessors();
    
    @RuntimeImport
    static native long fivmr_GC_freeMemory(Pointer gc);
    
    @RuntimeImport
    static native long fivmr_GC_totalMemory(Pointer gc);
    
    @RuntimeImport
    static native long fivmr_GC_maxMemory(Pointer GC);
    
    @RuntimeImport
    static native void fivmr_GC_collectFromJava(Pointer gc,
                                                Pointer descrIn,
                                                Pointer descrWhat);
    
    @RuntimeImport
    static native boolean fivmr_getIgnoreSystemGC(Pointer gc);
    
    @Import
    @GodGiven
    @NoReturn
    static native boolean fivmr_VM_exit(Pointer vm,int status);
    
    // may throw exceptions, and runs IN_NATIVE
    @Import
    @GodGiven
    static native boolean fivmr_JNI_loadLibrary(Pointer ts,
                                                Pointer ctx,
                                                Pointer filename);
    
    static int availableProcessors() {
	return fivmr_availableProcessors();
    }
    
    static long freeMemory() {
	return fivmr_GC_freeMemory(MM.getGC());
    }
    
    static long totalMemory() {
	return fivmr_GC_totalMemory(MM.getGC());
    }
    
    static long maxMemory() {
	return fivmr_GC_maxMemory(MM.getGC());
    }
    
    static void gc() {
	if (fivmr_getIgnoreSystemGC(MM.getGC())) {
	    log(FCRuntime.class,1,"Ignoring call to FCRuntime.gc()");
	} else {
	    fivmr_GC_collectFromJava(MM.getGC(),
                                     CVar.getPointer("FIVMR_SYSTEM_GC_STR"),
                                     Pointer.zero());
	}
    }
    
    static void runFinalization() {
	new FinalizerProcessor().processNextBatchAndLog(false);
    }
    
    static void runFinalizationForExit() {
	// FIXME
    }
    
    static void traceInstructions(boolean on) {
	// FIXME
    }
    
    static void traceMethodCalls(boolean on) {
	// FIXME
    }
    
    static void runFinalizersOnExit(boolean value) {
	// FIXME
    }
    
    static void exit(int status) {
	if(DisableEnableExit.getCanExit())
	    fivmr_VM_exit(Magic.getVM(),status);
    }
    
    @StackAllocation
    static int nativeLoad(String filename,
			  ClassLoader loader) {
	if (Settings.STATIC_JNI || !Settings.DYN_LOADING) {
	    log(FCRuntime.class,2,"Ignoring call to FCRuntime.nativeLoad for "+filename);
	    return 1;
	} else {
	    log(FCRuntime.class,2,"FCRuntime.nativeLoad called for "+filename);
	    Pointer filenameCstr=getCStringFullStack(filename);
	    try {
	        if (fivmr_JNI_loadLibrary(Magic.curThreadState(),
                                          fivmSupport.getClassLoaderData(loader),
                                          filenameCstr)) {
	            return 1;
	        } else {
	            return 0;
	        }
	    } catch (Throwable e) {
	        try {
	            logPrint("Warning: Loading native library "+filename+
	                    " resulted in exception: "+e+"\n");
	            e.printStackTrace();
	        } catch (Throwable e2) {
	            logPrint("Warning: could not report full error about "+
	            "failure in nativeLoad.\n");
	        }
	        return 0;
	    }
	}
    }
    
    static String mapLibraryName(String libname) {
	return libPrefix+libname+libSuffix;
    }
    
    static Process exec(String[] cmd,String[] env,File dir) throws IOException {
	return FCProcess.exec(cmd,env,dir);
    }
    
    static void enableShutdownHooks() {
	// nothing to do.
    }
    
}


