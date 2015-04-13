/*
 * GenericRegAlloc.java
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

public class GenericRegAlloc {
    HashMap< Var, RegNode > nodes=new HashMap< Var, RegNode >();
    boolean committed=false;
    int numRegs;
    
    static class RegNode extends GraphColoring.Node {
	Var v;
	
	RegNode(Var v) {
	    this.v=v;
	}
	
	public String description() {
	    return "GenericRegAlloc.RegNode[v = "+v+"]";
	}
    }
    
    public GenericRegAlloc() {
    }
    
    RegNode forVar(Var v) {
	assert v!=null;
	RegNode result=nodes.get(v);
	if (result==null) {
	    nodes.put(v,result=new RegNode(v));
	}
	return result;
    }
    
    public void concurrentlyLive(Iterable< Var > vars) {
	assert !committed;
	ArrayList< RegNode > list=new ArrayList< RegNode >();
	for (Var v : vars) {
	    list.add(forVar(v));
	}
	GraphColoring.cluster(list);
    }
    
    public Set< Var > relevantVars() {
	return nodes.keySet();
    }
    
    public void commit() {
	assert !committed;
	numRegs=GraphColoring.color(nodes.values())+1;
	committed=true;
    }
    
    public int getNumRegs() {
	assert committed;
	return numRegs;
    }
    
    public boolean hasColor(Var v) {
	assert committed;
	return forVar(v).hasColor();
    }
    
    public int getColor(Var v) {
	assert committed;
	RegNode rn=forVar(v);
	assert rn.hasColor() : "for "+v+", rn = "+rn;
	return rn.color();
    }
}

