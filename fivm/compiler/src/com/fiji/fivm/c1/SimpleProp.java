/*
 * SimpleProp.java
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

public class SimpleProp extends CodePhase {
    public SimpleProp(Code c) { super(c); }
    
    public void visitCode() {
	assert code.isSSA();
	
	// any variable that is the LHS of a Mov should have its uses replaced with the
	// RHS of the Mov.  any variable that is the LHS of a Phi with only one matching
	// Ipsilon should be replaced with the RHS of the Ipsilon.
	
	// FIXME: this breaks if we have movs that create permutations over variables.
	// but I'm not sure if that's legal under SSA...  I hope not.
        
        // FIXME: this is the type of thing that you want to run in a fixpoint.
	
	AssignCalc ac=code.getAssigns();
	
	HashMap< Var, Arg > map=new HashMap< Var, Arg >();
	
	for (Var v : code.vars()) {
	    // find real source of this value.  this takes care of both the Mov and
	    // the Ipsilon/Phi case by calling the magical uniqueSource() method.
	    Arg cur=v;
	    while (cur instanceof Var) {
		Arg next=ac.uniqueSource((Var)cur);
		if (next==null) {
		    break;
		}
		cur=next;
	    }
	    if (cur!=v) {
		// FIXME this is a problem.  OptConst infers a stronger type for a
		// variable, and then we do inlining based on that stronger type.
		// now we have type badness, and we could either:
		// 1) not do copy propagation and instead insert a cast, or
		// 2) do a stronger copy propagation where whenever we learn
		//    something new about the type of a variable we create
		//    a new variable, insert a Cast, and propagate.  but this
		//    requires either doing forward flow or coming out of SSA or
		//    both, and may introduce more Phi functions than we would
		//    have had before.
		// for now we go for (1).  it's ugly.  we should fix it at some
		// point.
		assert cur.type().isSubtypeOf(v.type()) : "v = "+v+", cur = "+cur;
		map.put(v,cur);
	    }
	}
        
        if (Global.verbosity>=3) {
            Global.log.println("SimpleProp doing replacement with: "+map);
        }
	
	if (!map.isEmpty()) {
	    for (Header h : code.headers()) {
		for (Operation o : h.operations()) {
		    for (int i=0;i<o.rhs().length;++i) {
			Arg rep=map.get(o.rhs(i));
			if (rep!=null) {
			    o.rhs[i]=rep;
			}
		    }
		    if (o instanceof Instruction) {
			Instruction i=(Instruction)o;
			if (map.containsKey(i.lhs()) && !code.getSideEffects().get(i)) {
			    i.remove();
			}
		    }
		}
	    }
	    
	    setChangedCode();
	    code.killIntraBlockAnalyses();
	}
    }
}


