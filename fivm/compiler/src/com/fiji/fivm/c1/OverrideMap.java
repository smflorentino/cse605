/*
 * OverrideMap.java
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

// FIXME: this was relevant when we had support for covariant return.  now, it's
// quite likely that this class, and the way it's being used, is superfluous.

// FIXME2: what about private methods?  they are INVOKESPECIAL'd.  so that works.
// but maybe we should have a story for when javac doesn't do the right thing,
// or when we're passed bad bytecode.

/** A data structure for figuring out which methods override which methods and
    signatures.  Useful for figuring out the vtable and itable of a class, and
    also quite useful for doing devirtualization. */
public final class OverrideMap {
    HashMap< MethodSignature, HashSet< VisibleMethod > > map=
	new HashMap< MethodSignature, HashSet< VisibleMethod > >();
    
    public OverrideMap() {}
    
    public OverrideMap(Iterable< VisibleMethod > i) {
	addAll(i);
    }
    
    public void addAll(Iterable< VisibleMethod > i) {
	for (VisibleMethod m : i) {
	    add(m);
	}
    }
    
    public void add(VisibleMethod m) {
	HashSet< VisibleMethod > a=map.get(m.getSignature());
	if (a==null) map.put(m.getSignature(),a=new HashSet< VisibleMethod >());
	a.add(m);
    }
    
    public boolean hasStrictOverriddenMethods(VisibleMethod m) {
	// could optimize this!
	return !getStrictOverriddenMethods(m).isEmpty();
    }
    
    // reads: does a strictly override b?
    private boolean strictlyOverrides(VisibleMethod a,VisibleMethod b) {
	if (a==b) return false;
	if (!a.getSignature().equals(b.getSignature())) return false;
	if (!b.getClazz().isInterface()) {
	    if (!a.getClazz().isSubclassOf(b.getClazz())) return false;
	}
	return true;
    }
    
    public ArrayList< VisibleMethod > getStrictOverriddenMethods(VisibleMethod m) {
	ArrayList< VisibleMethod > result=new ArrayList< VisibleMethod >();
	HashSet< VisibleMethod > a=map.get(m.getSignature());
	if (a!=null) {
	    for (VisibleMethod m2 : a) {
		if (strictlyOverrides(m,m2)) {
		    result.add(m2);
		}
	    }
	}
	return result;
    }
    
    public boolean hasStrictOverridingMethods(VisibleMethod m) {
	// could optimize this!
	return !getStrictOverridingMethods(m).isEmpty();
    }
    
    public ArrayList< VisibleMethod > getStrictOverridingMethods(VisibleMethod m) {
	ArrayList< VisibleMethod > result=new ArrayList< VisibleMethod >();
	HashSet< VisibleMethod > a=map.get(m.getSignature());
	if (a!=null) {
	    for (VisibleMethod m2 : a) {
		if (strictlyOverrides(m2,m)) {
		    result.add(m2);
		}
	    }
	}
	return result;
    }
    
    public boolean hasOverridingMethods(VisibleMethod m) {
	// FIXME optimize this
	return !getOverridingMethods(m).isEmpty();
    }
    
    public ArrayList< VisibleMethod > getOverridingMethods(VisibleMethod m) {
	ArrayList< VisibleMethod > result=new ArrayList< VisibleMethod >();
	HashSet< VisibleMethod > a=map.get(m.getSignature());
	if (a!=null) {
	    for (VisibleMethod m2 : a) {
                if (false) System.out.println("examining "+m2.jniName()+" as override for "+m.jniName());
		if (m==m2 || strictlyOverrides(m2,m)) {
                    if (false) System.out.println("    win!");
		    result.add(m2);
		}
	    }
	}
	return result;
    }
    
    public boolean hasOverridingMethods(MethodSignature s) {
	// could optimize this!
	return !getOverridingMethods(s).isEmpty();
    }
    
    public ArrayList< VisibleMethod > getOverridingMethods(MethodSignature s) {
	ArrayList< VisibleMethod > result=new ArrayList< VisibleMethod >();
	HashSet< VisibleMethod > a=map.get(s);
	if (a!=null) {
	    result.addAll(a);
	}
	return result;
    }
    
    public VisibleMethod getOverridingMethod(MethodSignature s) {
	try {
	    HashSet< VisibleMethod > a=map.get(s);
	    if (a==null || a.isEmpty()) {
		return null;
	    } else {
		assert a.size()==1;
		return a.iterator().next();
	    }
	} catch (Throwable e) {
	    throw new CompilerException("Could not get overriding method for "+s,e);
	}
    }
    
    public static VisibleMethod mostSpecific(Collection< VisibleMethod > methods) {
	VisibleMethod result=null;
	for (VisibleMethod m : methods) {
            if (false) System.out.println("   considering "+m.jniName());
	    if (!m.isAbstract() &&
		(result==null ||
		 m.getClazz().isSubclassOf(result.getClazz()))) {
                if (false) System.out.println("      win!");
		result=m;
	    }
	}
	return result;
    }
    
    public HashSet< VisibleMethod > computeDynables() {
	HashSet< VisibleMethod > result=new HashSet< VisibleMethod >();
	for (MethodSignature s : map.keySet()) {
            if (false) System.out.println("getting most specific for "+s.getName()+s.jniSignature());
	    VisibleMethod vm=mostSpecific(map.get(s));
	    if (vm!=null) {
		result.add(vm);
	    }
	}
	return result;
    }
    
    public VisibleMethod mostSpecific(MethodSignature s) {
	return mostSpecific(map.get(s));
    }
    
    public HashSet< VisibleMethod > forSignature(MethodSignature s) {
	return map.get(s);
    }
    
    public String toString() {
        return map.toString();
    }

    // compute overriddens as well?
    
    /** For the given class, compute the set of methods that may be invoked
	as a result of any InvokeDynamic where the actual receiver is an
	instance of exactly this class. */
    public static HashSet< VisibleMethod > computeDynables(VisibleClass c) {
	return new OverrideMap(c.allInstanceMethods()).computeDynables();
    }
}

