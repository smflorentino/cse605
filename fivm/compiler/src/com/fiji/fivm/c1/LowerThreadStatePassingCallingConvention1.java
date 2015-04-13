/*
 * LowerThreadStatePassingCallingConvention1.java
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

/** First part of lowering calling convention in case of thread state passing style.
    This transforms method invocations into C function calls. */
public class LowerThreadStatePassingCallingConvention1 extends CodePhase {
    public LowerThreadStatePassingCallingConvention1(Code code) {
	super(code);
	ThreadStatePassingCallingConvention.init();
    }
    
    public void visitCode() {
        boolean changedCFG=false;
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions2()) {
                switch (i.opcode()) {
                case InvokeResolved:
                case InvokeIndirect: {
                    MethodTypeSignature msig=((MTSInstable)i).signature();
		    if (code.getSafepoints().get(i)) {
                        i.prepend(
                            new DebugIDInfoInst(
                                i.di(),OpCode.SaveDebugID,
                                code,i,Var.VOID));
		    }
                    Arg[] args=new Arg[1+
                                       (msig.alloc()==AllocationMechanism.ALLOC_AS_CALLER?1:0)+
                                       i.rhs().length];
                    int cur=0;
                    int off=0;
                    if (i.opcode()==OpCode.InvokeIndirect) {
                        args[cur++]=i.rhs(off++);
                    }
		    args[cur++]=Arg.THREAD_STATE;
		    if (msig.alloc()==AllocationMechanism.ALLOC_AS_CALLER) {
                        Var allocSpace=code.addVar(Exectype.INT);
                        i.prepend(
                            new SimpleInst(
                                i.di(),OpCode.GetAllocSpace,
                                allocSpace,Arg.EMPTY));
			args[cur++]=allocSpace;
		    }
                    for (int j=off;j<i.rhs().length;++j) {
                        if (i.rhs(j).type().isObject()) {
                            Var tmp=code.addVar(Exectype.POINTER,
                                                i.rhs(j).type());
                            i.prepend(
                                new TypeInst(
                                    i.di(),OpCode.Cast,
                                    tmp,new Arg[]{i.rhs(j)},
                                    Type.POINTER));
                            args[cur++]=tmp;
                        } else {
                            args[cur++]=i.rhs(j);
                        }
                    }
                    Var result;
                    if (i.lhs().type().isObject()) {
                        result=code.addVar(Exectype.POINTER,
                                           i.lhs().type());
                    } else {
                        result=i.lhs();
                    }
                    
                    if (i.opcode()==OpCode.InvokeResolved) {
                        i.prepend(
                            new CFieldInst(
                                i.di(),OpCode.Call,
                                result,args,
                                ((ResolvedMethodInst)i).function()));
                    } else {
                        i.prepend(
                            new CallIndirectInst(
                                i.di(),OpCode.CallIndirect,
                                result,args,
                                msig.getNativeSignature()));
                    }
                    // FIXME at least some of this intelligence should be moved
                    // up to Simplify
		    if (msig.canThrow() && msig.canReturn()) {
			i.prepend(new SimpleInst(i.di(),OpCode.CheckException,
                                                 Var.VOID,Arg.EMPTY));
		    } else if (msig.canReturn()) {
			// do nothing
		    } else if (msig.canThrow()) {
			h.terminateAfter(
			    i,
			    new Terminal(
				i.di(),OpCode.Rethrow,
				Arg.EMPTY));
                        changedCFG=true;
		    } else {
			h.notReachedAfter(i);
                        changedCFG=true;
		    }
                    if (msig.getResult().isObject()) {
                        i.prepend(
                            new TypeInst(
                                i.di(),OpCode.Cast,
                                i.lhs(),new Arg[]{result},
                                msig.getResult()));
                    }
		    i.remove();
		    setChangedCode();
                    break;
                }
                default: break;
                }
	    }
	}
	if (changedCode()) {
            if (changedCFG) {
                code.killAllAnalyses();
            } else {
                code.killIntraBlockAnalyses();
            }
        }
    }
}

