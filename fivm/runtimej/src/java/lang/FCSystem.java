/*
 * FCSystem.java
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

import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.fivmRuntime.*;

import com.fiji.fivm.Settings;
import com.fiji.fivm.Time;

import java.io.*;
import java.util.*;

final class FCSystem {
    private FCSystem() {}
    
    static void arraycopy(Object src,int srcStart,
			  Object trg,int trgStart,
			  int len) {
        ArrayHelper.arraycopy(src,srcStart,trg,trgStart,len);
    }
    
    @Inline
    static int identityHashCode(Object o) {
	if (Settings.DUMB_HASH_CODE) {
	    // hack to force predictable execution
	    return 0;
	} else {
	    // NOTE: this assumes non-moving GC
	    return MM.hashCode(o);
	}
    }
    
    static void setIn(InputStream in) {
	objectPutStatic(Magic.addressOfStaticField(System.class,"in"),in,0);
    }
    
    static void setOut(PrintStream out) {
	objectPutStatic(Magic.addressOfStaticField(System.class,"out"),out,0);
    }
    
    static void setErr(PrintStream out) {
	objectPutStatic(Magic.addressOfStaticField(System.class,"err"),out,0);
    }

    public static long currentTimeMillis() { return nanoTime()/1000000L; }

    public static long nanoTime() { return Time.nanoTime(); }
    
    // FIXME!!!
    static native List<?> environ();

    static InputStream makeStandardInputStream() {
	return new BufferedInputStream(new FileInputStream(FileDescriptor.in));
    }
    
    static PrintStream makeStandardOutputStream() {
	return new PrintStream(
	    new BufferedOutputStream(
		new FileOutputStream(FileDescriptor.out)),
	    true);
    }
    
    static PrintStream makeStandardErrorStream() {
	return new PrintStream(
	    new BufferedOutputStream(
		new FileOutputStream(FileDescriptor.err)),
	    true);
    }
    
    static String getenv(String name) {
        return libc.getenv(name);
    }
}

