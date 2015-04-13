/*
 * Export.java
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
    should be exported via the C ABI.  The details are as follows:
    <ul>
    <li>The method will be exported as a function of the same name.  I.e. the
        name of the class, and the parameters and result type, are <b>not</b>
	included in the name of the C function; only the name of the Java
	method is.</li>
    <li>The parameters of the C function match exactly the parameters of the
        Java method, if the method is static.  If it's an instance method,
	there will be an extra parameter, which will appear first in the
	parameter list, that corresponds to the receiver.  All Java primitive
	types, in both the parameters and result, are converted to C inttypes.
	Object types (including arrays) are going to be fivmr_Handle*.</li>
    <li>The thread state is automatically retrieved by the C function using
        a thread-specific variable.  This step may be omitted by the optimizer
	if the Java method being exported is found to never use the thread
	state (this implementation is not yet implemented, but the fact that
	the retrieval occurs using pure Call should allow this to Just Work
	once we have inlining).  Retrieving the thread state makes the call
	slower than a normal C function call, or even a Java method call, but
	makes using Exported methods easier.</li>
    <li>The execution status is automatically switched to IN_JAVA upon
        invocation of the C function, prior to execution of Java code.  Thus,
	you don't have to worry about the execution status; on the other hand,
	if you change the execution status yourself, this may produce errors.
	Before the C function returns (either due to an exception in Java code,
	or due to a normal return), the execution status is switched back to
	either IN_NATIVE or IN_NATIVE_TO_BLOCK.</li>
    <li>Exceptions result in a dummy value being returned (guaranteed to be the
        equivalent of 0 for your return type), or no value is returned if the
	method is void.  It is possible to query if an exception occurred, and
	to retrieve it, by using the curExceptionHandle field, which will be
	NULL if there was no exception, or will refer to the current exception
	if an exception was raised.</li>
    <li>The class associated with the exported method is initialized before
        the Java code of the exported method runs.  This is true even when
	exporting an instance method.  As the fast path for initialization (the
	check to see if the class is already initialized) is very fast (it just
	involves loading a byte and comparing it to zero), this should not be
	a performance issue in most cases.
    </ul> */
public @interface Export {
}

