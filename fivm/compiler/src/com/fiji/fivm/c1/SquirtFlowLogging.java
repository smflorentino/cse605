/*
 * SquirtFlowLogging.java
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

import com.fiji.fivm.r1.FlowLog;
import com.fiji.fivm.Settings;

/**
 * Insert flow logging statements into the generated code.
 *
 * This should be called after inlining is complete.
 */
class SquirtFlowLogging extends CodePhase {
    public SquirtFlowLogging(Code c) { super(c); }

    public void visitCode() {
	if (!code.shouldFlowLog()) {
	    return;
	}
	VisibleMethod vm = code.method();
	Header root=code.reroot();
	root.append(
	    new CFieldInst(
		code.root().di(),OpCode.Call,
		Var.VOID,
		new Arg[]{
		    Arg.THREAD_STATE,
		    new IntConst(FlowLog.TYPE_METHOD),
		    new IntConst(FlowLog.SUBTYPE_ENTER),
		    new IntConst(vm.methodID())
		},
		FlowLogging.log));
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		if (i instanceof MethodInst) {
		    MethodInst mi = (MethodInst)i;
		    if (!mi.dynamicCall()) {
			if (FlowLogging.shouldLogCall(code, i.di())) {
			    VisibleMethod target = mi.staticTarget();
			    /* Wrap call with flow logging statements */
			    i.prepend(
				new CFieldInst(
				    code.root().di(),OpCode.Call,
				    Var.VOID,
				    new Arg[]{
					Arg.THREAD_STATE,
					new IntConst(FlowLog.TYPE_METHOD),
					new IntConst(FlowLog.SUBTYPE_ENTER),
					new IntConst(target.methodID())
				    },
				    FlowLogging.log));
			    i.append(
				new CFieldInst(
				    code.root().di(),OpCode.Call,
				    Var.VOID,
				    new Arg[]{
					Arg.THREAD_STATE,
					new IntConst(FlowLog.TYPE_METHOD),
					new IntConst(FlowLog.SUBTYPE_EXIT),
					new IntConst(target.methodID())
				    },
				    FlowLogging.log));
			}
		    }
		} else if (i instanceof HeapAccessInst) {
		    HeapAccessInst hai = (HeapAccessInst)i;
		    VisibleField f = null;
		    if (hai.field() instanceof VisibleField) {
			/* We really only know how to deal with
			 * VisibleFields here */
			f = (VisibleField)hai.field();
		    }
		    /* Used in more than one case */
		    Arg value;
		    Var typedata;
		    Var uniqueid;
		    switch (hai.opcode()) {
		    case PutField:
			if (f == null) {
			    continue;
			}
			Arg ref = hai.rhs()[0];
			value = hai.rhs()[1];
			if (value.effectiveBasetype() == Basetype.OBJECT) {
			    if (Settings.PTRSIZE_32) {
				/* This can be made faster by using
				 * f.getClazz(), which is static at
				 * compile time, rather than pulling the
				 * class from the object.  That loses
				 * the runtime type of the object,
				 * though.  It's unclear to me which is
				 * better. */
				typedata = code.addVar(Exectype.POINTER);
				uniqueid = code.addVar(Exectype.INT);
				i.prepend(
				    new SimpleInst(
					code.root().di(),OpCode.GetTypeDataForObject,
					typedata,new Arg[]{ ref }));
				i.prepend(
				    new CFieldInst(
					code.root().di(),OpCode.GetCField,
					uniqueid,new Arg[]{ typedata },
					CStructField.make(Basetype.INT, "uniqueID",
							  "fivmr_TypeData")));
				i.prepend(
				    new CFieldInst(
					code.root().di(),OpCode.Call,
					Var.VOID,
					new Arg[]{
					    Arg.THREAD_STATE,
					    new IntConst(FlowLog.TYPE_REFERENCE),
					    new IntConst(FlowLog.SUBTYPE_PUTFIELD),
					    ref,
					    value,
					    new IntConst(f.location()),
					    uniqueid
					},
					FlowLogging.log32_fat));
			    } else if (Settings.PTRSIZE_64) {
				i.prepend(
				    new CFieldInst(
					code.root().di(),OpCode.Call,
					Var.VOID,
					new Arg[]{
					    Arg.THREAD_STATE,
					    new IntConst(FlowLog.TYPE_REFERENCE),
					    new IntConst(FlowLog.SUBTYPE_PUTFIELD),
					    ref.copy(),
					    value.copy()
					},
					FlowLogging.log_fat));
			    }
			}
			break;
		    case PutStatic:
			if (f == null) {
			    continue;
			}
			if (Settings.PTRSIZE_32) {
			    i.prepend(
				new CFieldInst(
				    code.root().di(),OpCode.Call,
				    Var.VOID,
				    new Arg[]{
					Arg.THREAD_STATE,
					new IntConst(FlowLog.TYPE_REFERENCE),
					new IntConst(FlowLog.SUBTYPE_PUTSTATIC),
					new IntConst(f.getType().uniqueID()),
					hai.rhs[0],
					new IntConst(0),
					PointerConst.make(StaticFieldRepo.offsetForField(f)),
				    },
				    FlowLogging.log32_fat));
			} else if (Settings.PTRSIZE_64) {
			    i.prepend(
				new CFieldInst(
				    code.root().di(),OpCode.Call,
				    Var.VOID,
				    new Arg[]{
					Arg.THREAD_STATE,
					new IntConst(FlowLog.TYPE_REFERENCE),
					new IntConst(FlowLog.SUBTYPE_PUTSTATIC),
					new IntConst(f.getType().uniqueID()),
					hai.rhs()[0]
				    },
				    FlowLogging.log_fat));
			}
			break;
		    case ArrayStore:
			Arg array = hai.rhs()[0];
			Arg index = hai.rhs()[1];
			value = hai.rhs()[2];
			if (value.effectiveBasetype() == Basetype.OBJECT) {
			    if(Settings.PTRSIZE_32) {
				typedata = code.addVar(Exectype.POINTER);
				uniqueid = code.addVar(Exectype.INT);
				i.prepend(
				    new SimpleInst(
					code.root().di(),OpCode.GetTypeDataForObject,
					typedata,new Arg[]{ array }));
				i.prepend(
				    new CFieldInst(
					code.root().di(),OpCode.GetCField,
					uniqueid,new Arg[]{ typedata },
					CStructField.make(Basetype.INT, "uniqueID",
							  "fivmr_TypeData")));
				i.prepend(
				    new CFieldInst(
					code.root().di(),OpCode.Call,
					Var.VOID,
					new Arg[]{
					    Arg.THREAD_STATE,
					    new IntConst(FlowLog.TYPE_REFERENCE),
					    new IntConst(FlowLog.SUBTYPE_ARRAYSTORE),
					    array,value,uniqueid,index
					},
					FlowLogging.log32_fat));
			    } else if (Settings.PTRSIZE_64) {
				i.prepend(
				    new CFieldInst(
					code.root().di(),OpCode.Call,
					Var.VOID,
					new Arg[]{
					    Arg.THREAD_STATE,
					    new IntConst(FlowLog.TYPE_REFERENCE),
					    new IntConst(FlowLog.SUBTYPE_ARRAYSTORE),
					    array,
					    value
					},
					FlowLogging.log_fat));
			    }
			}
			break;
		    }
		}
	    }
	    if (h.getFooter().opcode() == OpCode.Return) {
		h.append(
		    new CFieldInst(
			code.root().di(),OpCode.Call,
			Var.VOID,
			new Arg[]{
			    Arg.THREAD_STATE,
			    new IntConst(FlowLog.TYPE_METHOD),
			    new IntConst(FlowLog.SUBTYPE_EXIT),
			    new IntConst(vm.methodID())
			},
			FlowLogging.log));
	    }
	}
    }
};
