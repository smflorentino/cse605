/*
 * Import.java
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

/** Annotation on methods and constructors used to indicate that the method
    is implemented in native C code, and should be implemented by making a
    direct call to that function using the C ABI.  The details are as follows:
    <ul>
    <li>The method will be imported as a function of the same name.  I.e. the
        name of the class, and the parameters and result type, are <b>not</b>
	included in the name of the C function; only the name of the Java
	method is.</li>
    <li>The parameters of the C function match exactly the parameters of the
        Java method, if the method is static.  If it's an instance method,
	there will be an extra parameter, which will appear first in the
	parameter list, that corresponds to the receiver.  All Java primitive
	types, in both the parameters and result, are converted to C inttypes.
	Object types (including arrays) are going to be fivmr_Handle*.</li>
    <li>The C function is responsible for retrieving the thread state on its
        own; it is not passed as one of the arguments.  That's not usually
	a problem, since the thread state is only typically passed along for
	performance; you can just as easily fetch it from a thread-specific
	variable.  But, some functions that use Import will add the thread
	state as one of the arguments for performance reasons.</li>
    <li>The execution status is automatically switched to IN_NATIVE (or
        IN_NATIVE_TO_BLOCK) upon invocation of the C function, prior to execution
	of that function's C code.  Thus, you don't have to worry about the
	execution status; on the other hand, if you change the execution status
	yourself, this may produce errors.  Right after the C function returns, the
	execution status is switched back to IN_JAVA.</li>
    <li>Exceptions can be thrown by the C function by using
        ThreadState::curExceptionHandle.</li>
    </ul> */
public @interface Import {
}

