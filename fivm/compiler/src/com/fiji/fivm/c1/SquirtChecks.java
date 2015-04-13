/*
 * SquirtChecks.java
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

import com.fiji.fivm.*;

public class SquirtChecks extends CodePhase {
    public SquirtChecks(Code c) { super(c); }
    
    private void squirtCheckInit(Operation o,VisibleClass c) {
	if ((code.method()==null ||
	     !code.method().getClazz().isSubclassOf(c)) &&
	    c.shouldCallCheckInit()) {
	    o.prepend(
		new ClassInst(
		    o.di(),OpCode.CheckInit,
		    Var.VOID,c));
	    setChangedCode();
	}
    }
    
    public void visitCode() {
        if (Settings.FULL_CHECK_INIT_SEMANTICS &&
            (code.method()==null ||
             (code.method().isInstance() &&
              !code.method().isInitializer() &&
              (code.method().getVisibility()==Visibility.PUBLIC ||
               code.method().getVisibility()==Visibility.PACKAGE))) &&
            code.getOwner().shouldCallCheckInit()) {
            Header h=code.reroot();
            h.append(
                new ClassInst(
                    h.di(),OpCode.CheckInit,
                    Var.VOID,code.getOwner()));
            setChangedCode();
        }
        
	for (Header h : code.headers()) {
	    for (Operation o : h.operations()) {
                if (Global.gc.needsCMStoreBarrier() &&
		    o instanceof HeapAccessInst &&
                    StoreSourceCalc.isBarriered(o)) {
                    HeapAccessInst hai=(HeapAccessInst)o;
                    hai.setMode(hai.mode().withCMStoreBarrier());
                }
		if (o.di().origin().noSafetyChecks()) {
		    continue;
		}
		switch (o.opcode()) {
		case AddressOfStatic: {
		    HeapAccessInst fi=(HeapAccessInst)o;
		    squirtCheckInit(o,((VisibleField)fi.field()).getClazz());
		    break;
		}
		case GetStatic:
		case PutStatic:
		case WeakCASStatic: {
		    HeapAccessInst fi=(HeapAccessInst)o;
		    squirtCheckInit(o,((VisibleField)fi.field()).getClazz());
		    break;
		}
		case InvokeStatic: {
		    MethodInst mi=(MethodInst)o;
		    squirtCheckInit(o,mi.method().getClazz());
		    break;
		}
		case New: {
		    TypeInst ti=(TypeInst)o;
		    squirtCheckInit(o,ti.getType().getClazz());
		}
		default: break;
		}
		switch (o.opcode()) {
		case PutField:
		case GetField:
		case AddressOfField:
		case WeakCASField:
		    if (!((VisibleField)
			  ((HeapAccessInst)o).field()).noNullCheckOnAccess()) {
			o.prepend(
			    new SimpleInst(
				o.di(),OpCode.NullCheck,
				Var.VOID,new Arg[]{o.rhs(0)}));
			setChangedCode();
		    }
		    break;
		case Invoke:
		case InvokeDynamic: {
		    MethodInst mi=(MethodInst)o;
		    if (!mi.method().noNullCheckOnAccess()) {
			mi.prepend(
			    new SimpleInst(
				o.di(),OpCode.NullCheck,
				Var.VOID,new Arg[]{o.rhs(0)}));
			setChangedCode();
		    }
		    VisibleClass vc=mi.method().getClazz();
		    if (vc.isInterface()) {
			Var receiver=code.addVar(vc.asExectype());
			mi.prepend(
			    new TypeCheckInst(
				o.di(),
				receiver,new Arg[]{o.rhs(0)},
				vc.asType(),
				Runtime.incompatibleClassChangeError.asType()));
			mi.rhs[0]=receiver;
			setChangedCode();
		    }
		    break;
		}
		case ArrayLoad:
		case ArrayStore:
		case Throw:
		case ArrayLength:
		case MonitorEnter:
		case MonitorExit:
		case AddressOfElement:
		case WeakCASElement:
		    o.prepend(
			new SimpleInst(
			    o.di(),OpCode.NullCheck,
			    Var.VOID,new Arg[]{o.rhs(0)}));
		    setChangedCode();
		    break;
		default: break;
		}
		switch (o.opcode()) {
		case Div:
		case Mod:
		    if (o.rhs(1).type().effectiveBasetype().isInteger) {
			o.prepend(
			    new SimpleInst(
				o.di(),OpCode.CheckDivisor,
				Var.VOID,new Arg[]{o.rhs(1)}));
			setChangedCode();
		    }
		    break;
		default: break;
		}
		switch (o.opcode()) {
		case ArrayLoad:
		case ArrayStore:
		case WeakCASElement:
		    o.prepend(
			new SimpleInst(
			    o.di(),OpCode.ArrayBoundsCheck,
			    Var.VOID,new Arg[]{o.rhs(0),o.rhs(1)}));
		    setChangedCode();
		    break;
		default: break;
		}
		switch (o.opcode()) {
		case ArrayStore:
		    if (o.rhs(2).type().isObject()) {
			o.prepend(
			    new SimpleInst(
				o.di(),OpCode.ArrayCheckStore,
				Var.VOID,new Arg[]{o.rhs(0),o.rhs(2)}));
			setChangedCode();
		    }
		    break;
		case WeakCASElement:
		    if (o.rhs(3).type().isObject()) {
			o.prepend(
			    new SimpleInst(
				o.di(),OpCode.ArrayCheckStore,
				Var.VOID,new Arg[]{o.rhs(0),o.rhs(3)}));
			setChangedCode();
		    }
		    break;
		default: break;
		}
		if (Settings.HAVE_SCOPE_CHECKS && StoreSourceCalc.isReference(o)) {
                    if (o instanceof HeapAccessInst &&
			!o.di().origin().method().noScopeChecks) {
                        HeapAccessInst hai=(HeapAccessInst)o;
                        hai.setMode(hai.mode().withScopeCheck());
                    }
		    switch (o.opcode()) {
		    case Throw:
			o.prepend(
			    new SimpleInst(
				o.di(),OpCode.InHeapCheck,
				Var.VOID,new Arg[]{
				    StoreSourceCalc.get(o)
				}));
			break;
		    case Return:
			if (code.origin().origin().alloc==AllocationMechanism.STACK_ALLOC) {
			    o.prepend(
				new SimpleInst(
				    o.di(),OpCode.ScopeReturnCheck,
				    Var.VOID,o.rhs()));
			}
			break;
		    default: break;
		    }
		}
	    }
	}
            
	if (changedCode()) code.killAllAnalyses();
    }
}

