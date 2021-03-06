/*
 * LocalCompactMethodRec.java
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

import java.io.*;

// NOTE: this is only really used internally by DebugIDRepository.
public class LocalCompactMethodRec extends Linkable {
    VisibleMethod vm;
    
    public LocalCompactMethodRec(VisibleMethod vm) {
	super(Basetype.VOID,MethodRec.linkageName(vm));
	this.vm=vm;
	assert !vm.shouldExist();
    }
    
    public boolean isLocal() { return true; }
    
    public void generateDeclaration(PrintWriter w) {
	w.println("fivmr_CompactMethodRec "+getName()+";");
    }
    
    public void generateDefinition(PrintWriter w) {
	if (vm.getParams().length>0) {
	    w.println("static fivmr_TypeData *"+vm.mangledName()+"_params"+"["+
		      vm.getParams().length+"] = {");
	    for (int i=0;i<vm.getParams().length;++i) {
		w.print("   (fivmr_TypeStub*)"+TypeStub.forType(vm.getParams()[i]).asCCode());
		if (i<vm.getParams().length-1) {
		    w.println(",");
		} else {
		    w.println();
		}
	    }
	    w.println("};");
	}
	w.print("fivmr_CompactMethodRec "+getName()+" = { ");
	w.print(vm.runtimeFlags()+", ");
	w.print("(fivmr_TypeData*)&"+TypeData.linkageName(vm.getClazz().asType())+
		", \""+vm.getName()+"\"");
	w.print(", (fivmr_TypeStub*)"+TypeStub.forType(vm.getType()).asCCode()+", "+
		vm.getParams().length+", ");
	if (vm.getParams().length>0) {
	    w.print(vm.mangledName()+"_params");
	} else {
	    w.print("NULL");
	}
	w.println("};");
    }
    
    public LinkableSet subLinkables() {
	LinkableSet result=new LinkableSet();
	result.add(TypeData.forType(vm.getClazz().asType()));
	result.add(TypeStub.forType(vm.getType()));
	for (Type t : vm.getParams()) {
	    result.add(TypeStub.forType(t));
	}
	return result;
    }
}

