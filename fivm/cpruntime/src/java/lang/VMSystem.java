package java.lang;

import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.Magic.*;
import static com.fiji.fivm.r1.fivmOptions.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

import java.io.*;
import java.util.*;

final class VMSystem {
    private VMSystem() {}
    
    @NoInline
    static void arraycopy(Object src,int srcStart,
			  Object trg,int trgStart,
			  int len) {
	FCSystem.arraycopy(src, srcStart, trg, trgStart, len);
    }
    
    @Inline
    static int identityHashCode(Object o) {
	return FCSystem.identityHashCode(o);
    }
    
    static void setIn(InputStream in){
	FCSystem.setIn(in);
    }
    static void setOut(PrintStream out){
	FCSystem.setOut(out);
    }

    static void setErr(PrintStream out){
	FCSystem.setErr(out);
    }

    public static long currentTimeMillis() {
	return FCSystem.currentTimeMillis();
    }

    public static long nanoTime() {
	return FCSystem.nanoTime();
    }

    static List<?> environ(){
	return FCSystem.environ();
    }

    static InputStream makeStandardInputStream() {
	return FCSystem.makeStandardInputStream();
    }
    
    static PrintStream makeStandardOutputStream() {
	return FCSystem.makeStandardOutputStream();
    }
    
    static PrintStream makeStandardErrorStream() {
	return FCSystem.makeStandardErrorStream();
    }
    
    static String getenv(String name){
	return FCSystem.getenv(name);
    }
}

