/*
 * UpcallMaker.java
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

import java.io.PrintWriter;

public class UpcallMaker {
    private UpcallMaker() {}
    
    public static String name(VisibleMethod vm) {
        assert !Global.haveNativeBackend();
	return Global.name+"_upcall_"+Util.hash(vm.getNativeSignature().canonicalName());
    }
    
    public static RemoteFunction remote(VisibleMethod vm) {
        assert !Global.haveNativeBackend();
	return new RemoteFunction(name(vm),
				  vm.getNativeSignature().result(),
				  new Basetype[]{
				      // function to call
				      Basetype.POINTER,
				      // array of args of type fivmr_Value
				      Basetype.POINTER
				  });
    }
    
    public static LocalFunction local(VisibleMethod vm) {
        assert !Global.haveNativeBackend();
	final NativeSignature ns=vm.getNativeSignature();
	return new LocalFunction(Global.name+"_upcall_"+Util.hash(ns.canonicalName()),
				 ns.result(),
				 new Basetype[]{
				     // function to call
				     Basetype.POINTER,
				     // array of args of type fivmr_Value
				     Basetype.POINTER
				 }) {
	    public void generateCode(PrintWriter w) {
		w.println("   "+ns.result().cType.asCCode()+" (*func)"+
			  ns.paramList()+" = ("+ns.ctype()+")arg0;");
		w.println("   fivmr_Value *args=(fivmr_Value*)arg1;");
		w.print("   ");
		if (ns.result()!=Basetype.VOID) {
		    w.print("return ");
		}
		w.print("func(");
		for (int i=0;i<ns.params().length;++i) {
		    if (i!=0) {
			w.print(", ");
		    }
		    w.print("args["+i+"]."+ns.param(i).descriptor);
		}
		w.println(");");
	    }
	};
    }

    public static LinkableSet upcalls() {
        assert !Global.haveNativeBackend();
	final LinkableSet result=new LinkableSet();
	Global.forAllMethods(new VisibleMethod.Visitor(){
		public void visit(VisibleMethod m) {
		    result.add(local(m));
		}
	    });
	return result;
    }
}

