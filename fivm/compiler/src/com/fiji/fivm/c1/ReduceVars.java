/*
 * ReduceVars.java
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

public class ReduceVars extends CodePhase {
    public ReduceVars(Code c) { super(c); }
    
    HashMap< Var, Var > varMap;
    
    void liveForType(Exectype t,
		     GenericRegAlloc gra,
		     Iterable< Var > vars) {
	ArrayList< Var > filtered=new ArrayList< Var >();
	for (Var v : vars) {
	    if (v.type()==t) {
		filtered.add(v);
	    }
	}
	gra.concurrentlyLive(filtered);
    }
    
    void forType(Exectype t) {
	SimpleLivenessCalc slc=code.getSimpleLiveness();
	GenericRegAlloc gra=new GenericRegAlloc();

	for (Header h : code.headers()) {
	    SimpleLivenessCalc.LocalCalc lc=
		slc.new LocalCalc(h);
	    for (Operation o : h.reverseOperations()) {
		liveForType(t,gra,lc.currentlyLive());
		lc.update(o);
	    }
	    liveForType(t,gra,lc.currentlyLive());
	}
	
	gra.commit();
	
	Var[] varList=new Var[gra.getNumRegs()];
	for (int i=0;i<varList.length;++i) {
	    varList[i]=code.addVar(t);
	}
	
	for (Var v : code.vars()) {
	    if (gra.hasColor(v)) {
		varMap.put(v,varList[gra.getColor(v)]);
	    }
	}
    }
    
    public void visitCode() {
	varMap=new HashMap< Var, Var >();
	
	forType(Exectype.INT);
	forType(Exectype.LONG);
	forType(Exectype.FLOAT);
	forType(Exectype.DOUBLE);
	forType(Exectype.POINTER);
	
	for (Var v : code.vars()) {
	    assert v.type()==Exectype.INT
		|| v.type()==Exectype.LONG
		|| v.type()==Exectype.FLOAT
		|| v.type()==Exectype.DOUBLE
		|| v.type()==Exectype.POINTER;
	}
	
	for (Header h : code.headers()) {
	    for (Operation o : h.operations()) {
		o.replaceVars(varMap);
	    }
	}
	
	setChangedCode();
	code.killIntraBlockAnalyses();
    }
}



