/*
 * PTSet.java
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

/** Points-to set. */
// FIXME: should have some way of identifying allocation sites.
public abstract class PTSet {
    /** Is this the top set? */
    public abstract boolean isTop();
    
    /** Is this the bottom set? */
    public abstract boolean isBottom();
    
    /** Does this set have a non-empty intersection with the other set?  May
	conservatively return true. */
    public abstract boolean intersects(PTSet other);
    
    /** What is the type that constitutes the least upper bound of this set?
	Cannot call if isBottom()==true. */
    public abstract Type leastType();
    
    /** Does the type returned by leastType() constitute an exact type for
	this set?  I.e., do all of the objects in this set have exactly that
	type rather than a strict subtype of that type?  Cannot call if
	isBottom()==true. */
    public abstract boolean isTypeExact();
    
    /** The type bound mode for the least type.
	Cannot call if isBottom()==true. */
    public TypeBoundMode typeBoundMode() {
	if (isTypeExact()) {
	    return TypeBoundMode.EXACT;
	} else {
	    return TypeBoundMode.UPPER_BOUND;
	}
    }
    
    /** If we're intending to call the given target, what are the possible
	method implementations that we may end up calling?  Can only call
	if Context.analysis().closed()==true. */
    public ArrayList< VisibleMethod > prune(VisibleMethod target) {
	assert Global.analysis().closed();
	return target.prune(asClassBound());
    }
    
    public boolean exactlyBelongs(Type t) {
	return intersects(new TypeBound(t,TypeBoundMode.EXACT));
    }
    
    /** Return a new set that corresponds to a union of this one and the
	other one. */
    public abstract PTSet union(PTSet other);
    
    /** Attempt to constrain the set by type.  In analyses that support such
	a notion, this will returns the subset of this set that only contains
	objects that are subtypes of the given type.  However - a totally
	correct implementation of this would be to just return the receiver
	no matter what. */
    public abstract PTSet filter(Type t);
    
    public ClassBound asClassBound() {
	return leastType().getClassBound(typeBoundMode());
    }
    
    public static PTSet top() {
	return Global.analysis().top();
    }
    
    public static PTSet bottom() {
	return BOTTOM;
    }
    
    public static PTSet[] bottomArray(int size) {
	PTSet[] result=new PTSet[size];
	for (int i=0;i<size;++i) {
	    result[i]=bottom();
	}
	return result;
    }
    
    public static boolean union(PTSet[] target,PTSet[] src) {
	assert target.length==src.length;
	boolean result=false;
	for (int i=0;i<target.length;++i) {
	    PTSet oldTrg=target[i];
	    PTSet newTrg=oldTrg.union(src[i]);
	    if (oldTrg!=newTrg) {
		target[i]=newTrg;
		result=true;
	    }
	}
	return result;
    }
    
    public static void union(PTSet[] target,PTSet[] src1,PTSet[] src2) {
	assert target.length==src1.length;
	assert target.length==src2.length;
	for (int i=0;i<target.length;++i) {
	    target[i]=src1[i].union(src2[i]);
	}
    }
    
    static class Bottom extends PTSet {
	public boolean isTop() { return false; }
	public boolean isBottom() { return true; }
	public boolean intersects(PTSet other) { return false; }
	public Type leastType() { throw new Error(); }
	public boolean isTypeExact() { throw new Error(); }
	public ArrayList< VisibleMethod > prune(VisibleMethod target) {
	    return VisibleMethod.EMPTY_AL;
	}
	public boolean exactlyBelongs(Type t) {
	    return false;
	}
	public PTSet union(PTSet other) {
	    if (other.isBottom()) {
		return this;
	    } else {
		return other;
	    }
	}
	public PTSet filter(Type t) {
	    return this;
	}
	public String toString() {
	    return "PTSet.BOTTOM";
	}
    }
    
    static Bottom BOTTOM=new Bottom();
}
