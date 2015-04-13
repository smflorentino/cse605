/*
 * FCStackWalker.java
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

package gnu.classpath;

import static com.fiji.fivm.r1.Magic.curFrame;
import static com.fiji.fivm.r1.fivmRuntime.fivmr_MethodRec_owner;
import static com.fiji.fivm.r1.fivmRuntime.fivmr_TypeData_asClass;
import static com.fiji.fivm.r1.fivmRuntime.fivmr_methodForStackDepth;
import static com.fiji.fivm.r1.fivmRuntime.iterateDebugFrames;
import static com.fiji.fivm.r1.Magic.getVM;

import com.fiji.fivm.r1.fivmRuntime.DumpStackCback;

import com.fiji.fivm.r1.*;

@UsesMagic
public final class FCStackWalker {
    @NoInline
    public static Class<?>[] getClassContext(int subtractDepth) {
	final int[] count=new int[1];
	iterateDebugFrames(
	    curFrame(),
	    new DumpStackCback(){
		public com.fiji.fivm.r1.Pointer
		cback(com.fiji.fivm.r1.Pointer mr,
		      int lineNumber) {
		    count[0]++;
		    return com.fiji.fivm.r1.Pointer.zero();
		}
	    });
	final Class<?>[] result=new Class<?>[count[0]-1-subtractDepth];
	count[0]=-1-subtractDepth;
	iterateDebugFrames(
	    curFrame(),
	    new DumpStackCback(){
		public com.fiji.fivm.r1.Pointer
		cback(com.fiji.fivm.r1.Pointer mr,
		      int lineNumber) {
		    if (count[0]>=0) {
			result[count[0]]=
			    fivmr_TypeData_asClass(
				fivmr_MethodRec_owner(mr));
		    }
		    count[0]++;
		    return com.fiji.fivm.r1.Pointer.zero();
		}
	    });
	return result;
    }
    
    public static Class<?>[] getClassContext() {
        return getClassContext(1);
    }
    
    public static Class<?>[] getClassContext2() {
        return getClassContext(2);
    }
    
    @NoInline
    public static Class<?> getCallingClass() {
	return fivmr_TypeData_asClass(
	    fivmr_MethodRec_owner(
		fivmr_methodForStackDepth(getVM(),curFrame(),2)));
    }
    
    @NoInline
    public static Class<?> getCallingClass2() {
	return fivmr_TypeData_asClass(
	    fivmr_MethodRec_owner(
		fivmr_methodForStackDepth(getVM(),curFrame(),3)));
    }
    
    @NoInline
    public static ClassLoader getCallingClassLoader() {
	return fivmr_TypeData_asClass(
	    fivmr_MethodRec_owner(
		fivmr_methodForStackDepth(getVM(),curFrame(),2)))
            .getClassLoader();
    }
    
    public static ClassLoader getCallingClassLoader2() {
	return fivmr_TypeData_asClass(
	    fivmr_MethodRec_owner(
		fivmr_methodForStackDepth(getVM(),curFrame(),3)))
            .getClassLoader();
    }
    
    public static ClassLoader getClassLoader(Class<?> cl) {
	return cl.getClassLoader();
    }
    
    // FIXME
    public static ClassLoader firstNonNullClassLoader() {
	return null;
    }
}

