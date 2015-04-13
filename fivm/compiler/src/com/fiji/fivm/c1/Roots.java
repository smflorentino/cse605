/*
 * Roots.java
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

public class Roots extends DirectContextable {
    // FIXME: need to distinguish between actual calls that happened, and
    // just accesses to a method.  if some JNI code looks up a method but
    // never calls it, we want to ensure that the lookup succeeds, but
    // the call doesn't have to.  The Analysis already knows how to deal with
    // this (with the call vs. execute distinction), but here call means
    // "call and execute directly" while dyncall means "dynamically call
    // and execute all possibles".  so currently, we use the latter for
    // JNI lookups, which is wrong.
    
    HashSet< VisibleMethod > dynMethods=
	new HashSet< VisibleMethod >();
    HashSet< VisibleMethod > methods=
	new HashSet< VisibleMethod >();
    HashSet< VisibleField > fields=
	new HashSet< VisibleField >();
    HashSet< Type > instTypes=
	new HashSet< Type >();
    HashSet< Type > types=
	new HashSet< Type >();
    
    public Roots(Context c) {
	super(c);
    }
    
    public void call(VisibleMethod m) {
	methods.add(m);
    }
    
    public void call(String desc) {
	try {
	    call(getContext().resolveMethod(desc));
	} catch (ResolutionFailed e) {
	    if (Global.verbosity>=2) Global.log.println(e);
	}
    }
    
    public void dynCall(VisibleMethod m) {
	dynMethods.add(m);
    }
    
    public void dynCall(String desc) {
	try {
	    dynCall(getContext().resolveMethod(desc));
	} catch (ResolutionFailed e) {
	    if (Global.verbosity>=2) Global.log.println(e);
	}
    }
    
    public void access(VisibleField f) {
	fields.add(f);
    }
    
    public void access(String desc) {
	try {
	    access(getContext().resolveField(desc));
	} catch (ResolutionFailed e) {
	    if (Global.verbosity>=2) Global.log.println(e);
	}
    }
    
    public void alloc(Type t) {
	assert t.isObject() : t;
	instTypes.add(t);
    }
    
    public void alloc(String desc) {
	try {
	    alloc(Type.parse(getContext(),desc).checkResolved());
	} catch (ResolutionFailed e) {
	    if (Global.verbosity>=2) Global.log.println(e);
	}
    }
    
    public void use(Type t) {
	types.add(t);
    }
    
    public void use(String desc) {
	try {
	    use(Type.parse(getContext(),desc).checkResolved());
	} catch (ResolutionFailed e) {
	    if (Global.verbosity>=2) Global.log.println(e);
	}
    }
    
    public void fullReflect(VisibleClass c) {
        for (VisibleClass c2 : c.allSupertypes()) {
            if (c2.asType().isObject()) {
                alloc(c2.asType());
            }
            use(c2.asType());
            for (VisibleField f : c2.fields()) {
                access(f);
            }
            for (VisibleMethod m : c2.methods()) {
                call(m);
                dynCall(m);
            }
        }
    }
    
    public Set< VisibleMethod > called() { return methods; }
    public Set< VisibleMethod > dynCalled() { return dynMethods; }
    public Set< VisibleField > accessed() { return fields; }
    public Set< Type > alloced() { return instTypes; }
    public Set< Type > used() { return types; }
    
    public void addAll(Roots other) {
	dynMethods.addAll(other.dynMethods);
	methods.addAll(other.methods);
	fields.addAll(other.fields);
	instTypes.addAll(other.instTypes);
	types.addAll(other.types);
    }
    
    public void addCalledMethodsTo(Set< VisibleMethod > methods) {
        methods.addAll(this.methods);
        methods.addAll(this.dynMethods);
    }
    
    public void addExecutedMethodsTo(Set< VisibleMethod > methods) {
        methods.addAll(this.methods);
        for (VisibleMethod m : this.dynMethods) {
            methods.addAll(m.possibleTargets());
        }
    }
    
    public String toString() {
	return "Roots[called = "+methods+", dynCalled = "+dynMethods+
	    ", accessed = "+fields+", alloced = "+instTypes+", used = "+
	    types+"]";
    }
    
    public static Roots EMPTY=new Roots(Global.root());
}

