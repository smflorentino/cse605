/*
 * LowerExceptions1.java
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

package com.fiji.fivm.c1;

import java.util.*;

/** Prepares the code for the goto-based exception handling approach, by:
    <ul>
    <li>Inserting a header in each exception handler that checks if we're
        seeing the right exception, and then either jumping to the real
	handler block, or dropping to the next handler.</li>
    <li>Making it so that there are no more dropsTo's.  dropsTo is always
        null from here on out.</li>
    <li>The handles field of EH blocks goes away.</li>
    </ul>
    Note that instructions that actually throw exceptions must still be
    handled.  */
// The question is: when we do representational, how do we handle
// throwing exceptions?  The issues are:
// - Throwing an exception from a check requires allocating it.  We don't
//   want that allocation to be inlined.
// - Representational knows about the checks, but not how to throw them,
//   while LowerExceptions knows about how to throw exceptions but not
//   about the checks.
// one approach would be to have a LowerExceptions2, which does the final
// conversion of throwing.  this will run after representational.  we can
// have three types of throw statement:
// Throw: takes a POINTER and "throws it" by putting it into the designated
//     exception field of Context, and either jumping to the relevant
//     handler or returning.
// ThrowRTEOnZero: takes an exception kind as a meta-parameter and an integer.
//     if the integer is zero, it jumps to a check-throwing block associated
//     with the the current exception handler.  that block will then
//     call a Java method that throws the exception.  after that, it will
//     jump to the handler.
// CheckException: check if there is an exception and rethrow if necessary.
//     use this after returning from calls.  note that we can use this after
//     returning from calls into the runtime.
// we can implement this by creating the meta-blocks - for each handler -
// eagerly in LowerExceptions1, and then those that don't get used can get
// dead-code-eliminated after representational.  note that the checked
// exception kinds are:
// - cast
// - stuff having to do with interface calls
// - null
// - bounds check
// - store check
// - ??? anything else?
public class LowerExceptions1 extends CodePhase {
    public LowerExceptions1(Code c) { super(c); }
    
    public void visitCode() {
	// create blocks that deal with dispatching exceptions
	HashMap< ExceptionHandler, Header > dispatchers=
	    new HashMap< ExceptionHandler, Header >();
	for (ExceptionHandler eh : code.handlers()) {
	    Header h=code.addHeader(eh.di());
	    dispatchers.put(eh,h);
	}
	
	// snapshot the headers
	ArrayList< Header > headers=new ArrayList< Header >();
	headers.addAll(code.headers());
	
	Header defaultHandler=code.addHeader(code.root().di());
	defaultHandler.setFooter(
	    new Terminal(
		code.root().di(),OpCode.Rethrow,Arg.EMPTY));

	// create blocks that deal with dispatching exceptions, and clear
	// the dropsTo and handles fields of exception handlers
	for (ExceptionHandler eh : code.handlers()) {
	    ExceptionHandler dropsTo=eh.dropsTo;
	    VisibleClass handles=eh.handles;
	    Header oldH=(Header)eh.next;
	    
	    Header h=dispatchers.get(eh);

	    Var predVar=code.addVar(Exectype.INT);
            Var eVar=code.addVar(Global.root().throwableClass.asExectype());
	    
	    h.append(
		new TypeInst(
		    eh.di(),OpCode.GetException,
		    eVar,Arg.EMPTY,
		    Global.root().throwableType));
	    h.append(
		new TypeInst(
		    eh.di(),OpCode.Instanceof,
		    predVar,new Arg[]{eVar},
		    handles.asType()));
	    Header belowDispatcher=dispatchers.get(dropsTo);
	    if (belowDispatcher==null) {
		belowDispatcher=defaultHandler;
	    }
	    h.setFooter(
		new Branch(
		    eh.di(),OpCode.BranchNonZero,
		    new Arg[]{predVar},
		    belowDispatcher,
		    oldH));
	    
	    eh.next=h;
	    eh.dropsTo=null;
	    eh.handles=null;
	}
	
	code.killAllAnalyses();
	setChangedCode();
    }
}

