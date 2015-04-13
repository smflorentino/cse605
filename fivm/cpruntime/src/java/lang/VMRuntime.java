package java.lang;

import static com.fiji.fivm.r1.fivmRuntime.fromCStringFull;
import static com.fiji.fivm.r1.fivmRuntime.getCStringFull;
import static com.fiji.fivm.r1.fivmRuntime.log;
import static com.fiji.fivm.r1.fivmRuntime.logPrint;
import static com.fiji.fivm.r1.fivmRuntime.returnBuffer;

import java.io.File;
import java.io.IOException;

import com.fiji.fivm.r1.GodGiven;
import com.fiji.fivm.r1.Import;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.RuntimeImport;
import com.fiji.fivm.util.DisableEnableExit;

final class VMRuntime {
    private VMRuntime() {}
    
    static int availableProcessors() {
	return FCRuntime.availableProcessors();
    }
    
    static long freeMemory() {
	return FCRuntime.freeMemory();
    }
    
    static long totalMemory() {
	return FCRuntime.totalMemory();
    }
    
    static long maxMemory() {
	return FCRuntime.maxMemory();
    }
    
    static void gc() {
	FCRuntime.gc();
    }
    
    static void runFinalization() {
	FCRuntime.runFinalization();
    }
    
    static void runFinalizationForExit() {
	FCRuntime.runFinalizationForExit();
    }
    
    static void traceInstructions(boolean on) {
	FCRuntime.traceInstructions(on);
    }
    
    static void traceMethodCalls(boolean on) {
	FCRuntime.traceMethodCalls(on);
    }
    
    static void runFinalizersOnExit(boolean value) {
	FCRuntime.runFinalizersOnExit(value);
    }
    
    static void exit(int status) {
	FCRuntime.exit(status);
    }
    
    static int nativeLoad(String filename,
			  ClassLoader loader) {
	return FCRuntime.nativeLoad(filename, loader);
    }
    
    static String mapLibraryName(String libname) {
	return FCRuntime.mapLibraryName(libname);
    }
    
    static Process exec(String[] cmd,String[] env,File dir) throws IOException {
	return FCRuntime.exec(cmd, env, dir);
    }
    
    static void enableShutdownHooks() {
	FCRuntime.enableShutdownHooks();
    }
    
}


