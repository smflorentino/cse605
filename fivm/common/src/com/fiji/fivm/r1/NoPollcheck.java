/*
 * NoPollcheck.java
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

/** Request that the compiler does not introduce pollchecks into the given
    method, and verify that the method does not obviously do anything that
    would cause a safepoint to occur.  This pragma does not indicate that
    the method would never cause a safepoint (and the compiler will not assume,
    for the purpose of code generation and optimization, that calling this
    method would not result in a safepoint).  But it does indicate that the
    correctness of this method is predicated upon safepoints not being
    implicitly inserted into it by the compiler.  Furthermore, the compiler
    will perform the following "sloppy" safety checks, which do not guarantee
    that a safepoint cannot occur during the execution of the method, but
    greatly reduce its chances:
    <ul>
    <li>This method may only call methods that are marked @NoPollcheck or
        @NoSafepoint.</li>
    <li>This method may not cause static initialization.  Operations such
        as static field access, static invocation, and instantiation may
	cause static initialization.  In the case of static invocation and
	field access, this is only permitted if the given class has no
	static initializers, or if the class of the method using the
	@NoPollcheck annotation is a subclass of the class that we would
	have had to initialize.</li>
    <li>Intantiation is not permitted.</li>
    <li>C function calls are only permitted if the C function is
        @NoSafepoint or @NoPollcheck.</li>
    <li>Java synchronization is not permitted</li>
    </ul>
    This fails to catch all problematic cases.  In particular, checked casts,
    null checks, array checks, interface call checks, and transitively, calls
    to other @NoPollcheck methods may cause safepoints to occur - but only if
    the programmer is careless.  Casts, null checks, and other checks only
    cause a safepoint if they fail.  Calls to @NoPollcheck methods only cause
    a safepoint if those methods cause a safepoint.
    <p>
    Sometimes, these checks are too strict.  In that case, you may use the
    @AllowUnsafe pragma to disable checking.  This is dangerous; it results
    in a method that may have safepoints but the compiler won't help you
    catch them. */
public @interface NoPollcheck {
}

