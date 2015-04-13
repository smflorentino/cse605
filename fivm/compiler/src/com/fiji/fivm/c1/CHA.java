/*
 * CHA.java
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

public class CHA extends TypeBasedAnalysis {
    HashSet< VisibleMethod > methods;
    HashSet< VisibleMethod > execMethods;
    HashSet< VisibleField > fields;
    HashSet< Type > usedTypes;
    HashSet< Type > instTypes;
    
    public CHA() {
	if (Global.verbosity>=1) {
	    Global.log.println("performing class hierarchy analysis...");
	}
	long before=System.currentTimeMillis();
	
	// live-ify types for all classes that are live
	for (VisibleClass c : Global.resolvedClasses()) {
	    c.asType();
	}
	
	usedTypes=new HashSet< Type >();
	for (Type t : Type.allKnownTypes()) {
	    if (!t.isBottomish()) {
		usedTypes.add(t);
	    }
	}
	
	instTypes=new HashSet< Type >();
	methods=new HashSet< VisibleMethod >();
	fields=new HashSet< VisibleField >();
	
	Type.closeArrayElements(usedTypes); // hack to make multinewarray work
	
	// find classes that have instances
	for (VisibleClass c : Global.resolvedClasses()) {
	    if (!c.isAbstract()) {
		instTypes.add(c.asType());
		for (VisibleClass c2 : c.allStrictSupertypes()) {
		    instTypes.add(c2.asType());
		}
	    }
	}
	
	// all array that we know about are presumed to have instances
	for (Type t : usedTypes) {
	    if (t.isArray()) {
		instTypes.add(t);
	    }
	}
	
	// all static methods and fields are live, and all instance methods
	// of instantiated classes are also live.
	for (VisibleClass c : Global.resolvedClasses()) {
	    for (VisibleMethod m : c.methods()) {
		if (m.isStatic() || instTypes.contains(c.asType())) {
		    methods.add(m);
		}
	    }
	    for (VisibleField f : c.fields()) {
		if (f.isStatic() || instTypes.contains(c.asType())) {
		    fields.add(f);
		}
	    }
	}
	
	execMethods=new HashSet< VisibleMethod >();
	for (VisibleMethod m : methods) {
	    if (!m.isAbstract()) {
		execMethods.add(m);
	    }
	}
	
	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) {
	    Global.log.println("did CHA in "+(after-before)+" ms");
	}
    }
    
    public boolean closed() {
	return true;
    }

    public Set< VisibleMethod > calledMethods() { return methods; }
    public Set< VisibleMethod > executedMethods() { return execMethods; }
    public Set< VisibleField > liveFields() { return fields; }
    public Set< Type > usedTypes() { return usedTypes; }
    public Set< Type > instantiatedTypes() { return instTypes; }
    
    public void pruneCalledMethods(Set< VisibleMethod > pruneSet) {
        Util.retain(methods,pruneSet);
    }
    
    public void pruneExecutedMethods(Set< VisibleMethod > pruneSet) {
        Util.retain(execMethods,pruneSet);
    }
}


