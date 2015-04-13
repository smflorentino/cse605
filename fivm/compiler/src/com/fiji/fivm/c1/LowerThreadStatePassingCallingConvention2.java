/*
 * LowerThreadStatePassingCallingConvention2.java
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

public class LowerThreadStatePassingCallingConvention2 extends CodePhase {
    public LowerThreadStatePassingCallingConvention2(Code c) {
	super(c);
	ThreadStatePassingCallingConvention.init();
    }
    
    private void emitGetCArg(Operation before,
                             int argIndex,
                             Var target) {
        before.prepend(
            new ArgInst(
                before.di(),OpCode.GetCArg,
                target,Arg.EMPTY,argIndex));
    }
    
    public void visitCode() {
	Var threadState=null;
	Var allocSpace=null;
	int numRTArgs=0;
        
        if (!Global.lateRCE) {
            threadState=code.addVar(Exectype.POINTER);
            allocSpace=code.addVar(Exectype.INT);
        }
        
	Header root=code.reroot();
	
	if (code.getsOwnThreadState()) {
            throw new Error("currently not supported");
	} else {
            if (!Global.lateRCE) {
                emitGetCArg(root.footer(),0,threadState);
            }
	    numRTArgs++;
            // FIXME: I don't like this.  we should be smarter about this.  it would
            // be great to be able to use @AllocateAsCaller without the class hierarchy
            // restrictions.  and I don't buy that this is any more efficient than
            // having an @AllocateAsCaller method simply query the current allocation
            // frame, or something.  (of course we cannot query the allocaiton frame,
            // since a call from @StackAllocation to non-@StackAllocation will not
            // reset the allocation frame.  and that is actually quite interesting,
            // because it means that we should be able to support a different mechanism:
            // @StackAllocateAsCaller, which means "allocate on the stack but on the
            // top stack allocation frame".  humf.  need to think this over.)
	    if (code.origin().origin().alloc==AllocationMechanism.ALLOC_AS_CALLER) {
                if (!Global.lateRCE) {
                    emitGetCArg(root.footer(),1,allocSpace);
                }
		numRTArgs++;
	    }
	}
	for (Header h : code.headers()) {
	    for (Operation o : h.operations()) {
		switch (o.opcode()) {
		case GetArg: {
		    ArgInst ai=(ArgInst)o;
		    Var tmp=
			code.addVar(
			    Exectype.make(
				code.param(ai.getIdx()).effectiveBasetype().pointerifyObject),
                            code.param(ai.getIdx()).asExectype());
                    emitGetCArg(o,ai.getIdx()+numRTArgs,tmp);
		    o.prepend(
			new TypeInst(
			    o.di(),OpCode.Cast,
			    ((Instruction)o).lhs(),new Arg[]{tmp},
			    code.param(ai.getIdx()).asExectype().asType()));
		    ((Instruction)o).remove();
                    break;
		}
                case GetAllocSpace: {
                    SimpleInst si=(SimpleInst)o;
                    if (Global.lateRCE) {
                        emitGetCArg(si,1,si.lhs());
                    } else {
                        si.prepend(
                            new SimpleInst(
                                si.di(),OpCode.Mov,
                                si.lhs(),new Arg[]{
                                    allocSpace
                                }));
                    }
                    si.remove();
                    break;
                }
		default: break;
		}
                if (Global.lateRCE) {
                    Var myThreadState=null;
                    for (int i=0;i<o.rhs().length;++i) {
                        if (o.rhs(i)==Arg.THREAD_STATE) {
                            if (myThreadState==null) {
                                myThreadState=code.addVar(Exectype.POINTER);
                                emitGetCArg(o,0,myThreadState);
                            }
                            o.rhs[i]=myThreadState;
                        }
                    }
                } else {
                    for (int i=0;i<o.rhs().length;++i) {
                        if (o.rhs(i)==Arg.THREAD_STATE) {
                            o.rhs[i]=threadState;
                        }
                    }
                }
	    }
	}
	
	code.killAllAnalyses();
	setChangedCode();
    }
}


