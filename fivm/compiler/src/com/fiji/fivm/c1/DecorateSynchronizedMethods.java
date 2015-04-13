/*
 * DecorateSynchronizedMethods.java
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

public class DecorateSynchronizedMethods extends CodePhase {
    public DecorateSynchronizedMethods(Code c) { super(c); }
    
    public void visitCode() {
	// lowering of synchronized
	// the idea for instance methods:
	// - if the receiver is never retrieved, create a new variable
	//   for it
	// - if the receiver is already retrieved but never modified,
	//   use it directly
	// - if the receiver is retrieved but modified, make a copy of
	//   it
	if (code.method()!=null && code.method().isSynchronized()) {
	    // FIXME: this code is bogus.
	    Header root=code.reroot();
	    
	    Var receiver=null;
	    if (code.method().isStatic()) {
		receiver=code.addVar(Global.root().classClass.asExectype());
		root.prepend(
		    new TypeInst(
			root.di(),OpCode.GetType,
			receiver,Arg.EMPTY,
			code.method().getClazz().asType()));
	    } else {
		receiver=code.addVar(code.method().getClazz().asExectype());
		root.append(
		    new ArgInst(root.di(),OpCode.GetArg,receiver,Arg.EMPTY,0));
	    }
	    
	    // add monitorenter
	    root.append(
		new SimpleInst(
		    root.di(),OpCode.MonitorEnter,
		    Var.VOID,new Arg[]{receiver}));
	    
	    // even more fun.  put handlers on all of the blocks after the
	    // root, and point them at our handler, which does a monitorexit.
	    Var e=code.addVar(Global.root().throwableClass.asExectype());
	    Header monitorExitBlock=code.addHeader(root.di());
	    ExceptionHandler monitorExitEH=code.addHandler(
		root.di(),Global.root().throwableClass,null,monitorExitBlock);
	    monitorExitBlock.append(
		new TypeInst(
		    root.di(),OpCode.GetException,
		    e,Arg.EMPTY,Global.root().throwableType));
	    monitorExitBlock.append(
		new SimpleInst(
		    root.di(),OpCode.ClearException,
		    Var.VOID,Arg.EMPTY));
	    monitorExitBlock.append(
		new SimpleInst(
		    root.di(),OpCode.MonitorExit,
		    Var.VOID,new Arg[]{receiver}));
	    monitorExitBlock.setFooter(
		new Terminal(
		    root.di(),OpCode.Throw,new Arg[]{e}));
	    
	    // point headers that don't have handlers to our handler
	    for (Header h : code.headers()) {
		if (h!=root && h!=monitorExitBlock &&
		    h.handler()==null) {
		    h.setHandler(monitorExitEH);
		}
	    }
	    
	    // point exception handlers that don't drop to anything to our
	    // handler
	    for (ExceptionHandler eh : code.handlers()) {
		if (eh!=monitorExitEH && eh.dropsTo==null) {
		    eh.dropsTo=monitorExitEH;
		}
	    }
	    
	    // find all method returns, split the blocks at the point of
	    // return, and create new blocks that do the MonitorExit and then
	    // the return.  (splitting the blocks is needed because the MonitorExit
	    // may throw exceptions)
	    ArrayList< Header > toModify=new ArrayList< Header >();
	    for (Header h : code.headers()) {
		if (h.getFooter().opcode()==OpCode.Return) {
		    toModify.add(h);
		}
	    }
	    for (Header h : toModify) {
		Header hCont=code.addHeader(h.getFooter().di());
		hCont.setFooter(
		    new Terminal(h.getFooter().di(),
				 OpCode.Return,
				 h.getFooter().rhs));
		h.setFooter(
		    new Jump(h.getFooter().di(),hCont));
		hCont.append(
		    new SimpleInst(h.getFooter().di(),OpCode.MonitorExit,
				   Var.VOID,new Arg[]{receiver}));
	    }
            
	    setChangedCode("synchronization");
	    code.killAllAnalyses();

	    if (Global.verbosity>=3) {
		Global.log.println("Code after synchronized method handling:");
		CodeDumper.dump(code,Global.log);
	    }
	}
    }
}

