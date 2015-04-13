/*
 * LowerAllocation.java
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

public class LowerAllocation extends CodePhase {
    public LowerAllocation(Code c) { super(c); }
    
    public void visitCode() {
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case New:
		case NewArray: {
		    TypeInst ti=(TypeInst)i;
		    Var resultPtr=code.addVar(Global.root().objectClass.asExectype());
		    Var typePtr=code.addVar(Exectype.POINTER);
                    Var allocSpace=code.addVar(Exectype.INT);
		    i.prepend(
			new TypeInst(
			    i.di(),OpCode.GetTypeData,
			    typePtr,Arg.EMPTY,
			    ti.getType()));
                    i.prepend(
                        new SimpleInst(
                            i.di(),OpCode.GetAllocSpace,
                            allocSpace,Arg.EMPTY));
		    switch (i.opcode()) {
		    case New: {
			VisibleClass vc=ti.getType().getClazz();
                        if (h.unlikely()) {
                            i.prepend(
                                new MethodInst(
                                    i.di(),OpCode.InvokeStatic,
                                    resultPtr,
                                    new Arg[]{
                                        allocSpace, /// FIXME probably this is redundant
                                        typePtr
                                    },
                                    Runtime.allocSimple));
                        } else {
                            i.prepend(
                                new MethodInst(
                                    i.di(),OpCode.InvokeStatic,
                                    resultPtr,
                                    new Arg[]{
                                        allocSpace,
                                        Arg.ALLOC_FRAME,
                                        typePtr,
                                        new PointerConst(vc.alignedPayloadSize()),
                                        new PointerConst(vc.requiredPayloadAlignment())
                                    },
                                    Runtime.alloc));
                        }
                        if (vc.finalizable()) {
                            Var throwaway=code.addVar(Global.root().objectType.asExectype());
                            i.prepend(
                                new MethodInst(
                                    i.di(),OpCode.InvokeStatic,
                                    throwaway,
                                    new Arg[]{
                                        allocSpace,
                                        resultPtr
                                    },
                                    Runtime.addDestructor));
                        }
			break;
		    }
		    case NewArray: {
                        if (h.unlikely()) {
                            i.prepend(
                                new MethodInst(
                                    i.di(),OpCode.InvokeStatic,
                                    resultPtr,
                                    new Arg[]{
                                        allocSpace, /// FIXME probably this is redundant
                                        typePtr,
                                        i.rhs(0)
                                    },
                                    Runtime.allocArraySimple));
                        } else {
                            i.prepend(
                                new MethodInst(
                                    i.di(),OpCode.InvokeStatic,
                                    resultPtr,
                                    new Arg[]{
                                        allocSpace,
                                        Arg.ALLOC_FRAME,
                                        typePtr,
                                        i.rhs(0),
                                        new PointerConst(
                                            ti.getType().arrayElement().effectiveBasetype().bytes)
                                    },
                                    Runtime.allocArray));
                        }
			break;
		    }
		    default: throw new Error();
		    }
                    Var castIntermediate=code.addVar(ti.getType().asExectype());
		    i.prepend(
			new TypeInst(
			    i.di(),OpCode.CastExact,
			    castIntermediate,new Arg[]{resultPtr},
			    ti.getType()));
                    i.prepend(
                        new SimpleInst(
                            i.di(),OpCode.CastNonZero,
                            i.lhs(),new Arg[]{castIntermediate}));
		    i.remove();
		    setChangedCode();
		    break;
		}
		default: break;
		}
	    }
	}
	
	if (changedCode()) code.killIntraBlockAnalyses();
    }
}


