/*
 * InlineInvocations.java
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

import com.fiji.fivm.Settings;
import com.fiji.fivm.r1.FlowLog;

// run this in a fixpoint as follows:
// InlineInvocations -> SimpleProp -> OptConst -> Simplify

// NOTE: this phase requires simplified SSA code.

public class InlineInvocations extends CodePhase {
    InlineFilter filter;
    
    public InlineInvocations(Code c,InlineFilter filter) {
	super(c);
	this.filter=filter;
    }
    
    private HashSet< Header > unacceptableCallers;

    private HashMap< DebugInfo, DebugInfo > diMemo;
    
    private DebugInfo wrap(MethodInst mi, DebugInfo di) {
	DebugInfo result=diMemo.get(di);
	if (result==null) {
	    diMemo.put(di,result=di.addCaller(mi.di()));
	}
	return result;
    }
    
    private void doInline(Header mih, MethodInst mi, Code subCode) {
	assert subCode.isSSA();

	if (Global.verbosity>=10) {
	    Global.log.println("Inlining "+subCode+" into "+code+":");
	    CodeDumper.dump(subCode,
			    Global.log,
			    CodeDumper.Mode.NO_ANALYSES);
	}
	
	// set up memoization of debug infos
	diMemo=new HashMap< DebugInfo, DebugInfo >();

	// the easy part first: add all variables
	code.addVars(subCode.vars());
	
	// the slightly harder part: add all locals
	for (CLocal l : subCode.locals()) {
	    code.addLocalWithUniqueName(l);
	}
	
	// now it's getting rough: add all headers, but:
	// - fix the handler stacks
	// - fix the DebugInfo stacks
	for (Header h : subCode.headers()) {
	    h.code=code;
	    h.order=-1;
	    if (h.handler==null) {
		h.handler=mih.handler;
	    }
	    h.di=wrap(mi,h.di);
	    for (Operation o : h.operations()) {
		o.di=wrap(mi,o.di);
	    }
	    code.headers.add(h);
	    
	    // prevent chain inlining into the inlinee on this pass.
	    unacceptableCallers.add(h);
	}
	for (ExceptionHandler e : subCode.handlers()) {
	    e.di=wrap(mi,e.di);
	    if (e.dropsTo==null && e.handles!=Global.root().throwableClass) {
		e.dropsTo=mih.handler;
	    }
	    code.handlers.add(e);
	}
	
	// now we need to:
	// - fix non-exceptional control flow
	// - fix data flow (find GetArg and Return ops)
	
	// split the caller block and make the head of it jump to the root
	// of the callee
	Header mihcont=mih.split(mi);

	mi.remove();
	
	// If we're flow logging, add a flow log statement to the end of mih
	final VisibleMethod vm = subCode.origin().origin();
	if (Settings.FLOW_LOGGING &&
	    FlowLogging.shouldLogCall(code, subCode)) {
	    mih.append(
		new CFieldInst(
		    mi.di(),OpCode.Call,
		    Var.VOID,
		    new Arg[]{
			Arg.THREAD_STATE,
			new IntConst(FlowLog.TYPE_METHOD),
			new IntConst(FlowLog.SUBTYPE_ENTER_INLINE),
			new IntConst(vm.methodID())
		    },
		    FlowLogging.log));
	}

	// if the inlined code is not annotation-compatible with the invocation
	// instruction, insert a compiler fence
	if (!mi.di().origin().compatible(subCode.origin()) &&
            !code.pollchecksInserted) {
            assert !code.pollchecksInserted;
	    mih.append(
		new SimpleInst(
		    mi.di(),OpCode.PollcheckFence,
		    Var.VOID,Arg.EMPTY));
	}
	
	mih.setFooter(
	    new Jump(mi.di(),subCode.root()));
	
	// fix the data flow and returns
        boolean sawReturnValue=false;
	for (Header h : subCode.headers()) {
	    for (Operation o : h.operations()) {
		switch (o.opcode()) {
		case GetArg: {
		    ArgInst ai=(ArgInst)o;
		    if (ai.getIdx()==0 &&
			!mi.rhs(0).type().isSubtypeOf(ai.lhs().type())) {
			if (Global.verbosity>=2) {
			    Global.log.println(
				"Warning: need cast on inline call from "+code+" to "+
				subCode);
			}
			ai.prepend(
			    new TypeInst(
				o.di(),OpCode.Cast,
				ai.lhs(),new Arg[]{mi.rhs(0)},
				ai.lhs().type().asType()));
		    } else {
			ai.prepend(
			    new SimpleInst(
				o.di(),OpCode.Mov,
				ai.lhs(),new Arg[]{mi.rhs(ai.getIdx())}));
		    }
		    ai.remove();
		    break;
		}
		case Return: {
		    sawReturnValue=true;
		    if (o.rhs().length==1) {
			if (mi.method().isNonZero()) {
			    Var nonZero=code.addVar(mi.lhs().type());
			    h.append(
				new SimpleInst(
				    o.di(),OpCode.CastNonZero,
				    nonZero,o.rhs()));
			    h.append(
				new SimpleInst(
				    o.di(),OpCode.Ipsilon,
				    mi.lhs(),new Arg[]{nonZero}));
			} else {
			    h.append(
				new SimpleInst(
				    o.di(),OpCode.Ipsilon,
				    mi.lhs(),o.rhs()));
			}
		    }
		    if (!mi.di().origin().compatible(subCode.origin()) &&
                        !code.pollchecksInserted) {
                        assert !code.pollchecksInserted;
			h.append(
			    new SimpleInst(
				o.di(),OpCode.PollcheckFence,
				Var.VOID,Arg.EMPTY));
		    }
		    if (Settings.FLOW_LOGGING &&
			FlowLogging.shouldLogCall(code, subCode)) {
			h.append(
			    new CFieldInst(
				mi.di(),OpCode.Call,
				Var.VOID,
				new Arg[]{
				    Arg.THREAD_STATE,
				    new IntConst(FlowLog.TYPE_METHOD),
				    new IntConst(FlowLog.SUBTYPE_EXIT_INLINE),
				    new IntConst(vm.methodID)
				},
				FlowLogging.log));
		    }
		    h.setFooter(
			new Jump(o.di(),mihcont));
		    break;
		}
		default: break;
		}
	    }
	}

	if (sawReturnValue) {
	    mihcont.prepend(
		new SimpleInst(
		    mi.di(),OpCode.Phi,
		    mi.lhs(),new Arg[]{mi.lhs()}));
	}
    }
    
    public void visitCode() {
	// what this needs to do:
	// 1) find calls that are completely devirtualized
	// 2) of those, find those that refer to inlineable methods
	// 3) perform inlining
	// 4) fixpoint.  we should do a fixed number of fixpoint iterations
	//    of inlining.  (see Context.maxInlineFixpoint)  these iterations
	//    should include going into and out of SSA.  or...  we should do
	//    inlining over SSA.  hmmmm....  (and that's what we're doing)
	
	// what inlining itself has to worry about:
	// a) renumbering variables
	// b) renaming locals
	// c) stacking the exception handlers correctly
	// d) ???
	
	// FIXME1: have a separate inlining fixpoint that always inlines stuff
	// that MustInlineCalc says we should. (DONE)
	
	// FIXME2: inline more heavily in loops.  have separate inlining "levels"
	// depending on loop depth.
	
	assert code.isSSA();
	
	unacceptableCallers=new HashSet< Header >();
	
	// find completely devirtualized calls to inlineable methods, and inline
	// them.
	for (Header h : code.headers2()) {
	    // NB this test prevents chain-inlining via callsites in inlinees.  that
	    // helps preserve the meaning of maxInlineFixpoint (if you want to chain-
	    // inline, increase that number)
	    if (!unacceptableCallers.contains(h)) {
                int maxRecursiveInline=Global.maxRecursiveInline;
                if (h.unlikely()) {
                    maxRecursiveInline=0;
                }
		for (Instruction i : h.instructions2()) {
		    if (i instanceof MethodInst) {
			MethodInst mi=(MethodInst)i;
			assert !mi.deadCall() : "In "+h+" in "+mi+" with di = "+mi.di();
			if (!mi.dynamicCall()) {
			    VisibleMethod vm=mi.staticTarget();
			    if (InlineRepo.has(vm)) {
				Code subCode=InlineRepo.get(vm);
				int numFound=0;
				for (DebugInfo di=mi.di();di!=null;di=di.caller()) {
				    if (di.origin().equals(subCode.origin())) {
					numFound++;
				    }
				}
				if (numFound<=maxRecursiveInline) {
                                    InlineSuggestion s=
                                        filter.shouldInline(code,subCode);
                                    if (s==InlineSuggestion.INLINE_FOR_SIZE_AND_SPEED ||
                                        (!h.unlikely() &&
                                         s!=InlineSuggestion.DONT_INLINE)) {
                                        doInline(h, mi, subCode);
                                        setChangedCode();
                                    }
				}
			    }
			}
		    }
		}
	    }
	}
	
	if (Global.verbosity>=10) {
	    Global.log.println("dumping code right after inlining:");
	    CodeDumper.dump(code,
			    Global.log,
			    CodeDumper.Mode.NO_ANALYSES);
	}
	
	if (changedCode()) {
	    code.recomputeOrder();
	    code.killAllAnalyses();
	}
    }
}


