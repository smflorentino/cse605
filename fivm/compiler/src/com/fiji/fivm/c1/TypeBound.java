/*
 * TypeBound.java
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

/**
 * A points-to set that uses a type bound.  This set can have one of three states:
 * <ul>
 * <li>Exact type: this set contains all instances of a particular type, but
 *     not of any strict subtypes of that tyoe.</li>
 * <li>Upper bound: this set contains all isntances of subtypes of a particular
 *     type.</li>
 * <li>Bottom: this set contains instances of a type that has no instances (hence
 *     it is the empty set).</li>
 * </ul>
 */
public class TypeBound extends PTSet {
    Type t;
    TypeBoundMode mode;
    
    public TypeBound(Type t,
		     TypeBoundMode mode) {
	assert t.isObject();
	this.t=t;
	if (t.resolved() && t.isFinal()) {
	    this.mode=TypeBoundMode.EXACT;
	} else {
	    this.mode=mode;
	}
    }
    
    public TypeBound(Type t) {
	this(t,t.typeBoundMode());
    }
    
    public boolean isExact() {
	return mode.isExact();
    }
    
    public boolean isTypeExact() {
	return isExact();
    }
    
    public TypeBoundMode typeBoundMode() {
	return mode;
    }
    
    public boolean canRefine(TypeBound other) {
	if (other.isExact()) {
	    if (isExact()) {
		return t==other.t;
	    } else {
		return other.t.isSubtypeOf(t);
	    }
	} else {
	    if (isExact()) {
		return t.isSubtypeOf(other.t);
	    } else {
		return true;
	    }
	}
    }
    
    public boolean isMoreSpecificThan(TypeBound other) {
        // QUESTION/FIXME: why is this not saying t.isStrictSubtypeOf(other.t)?  maybe
        // it doesn't matter, but the current impl may have an adverse effect on
        // fixpoint convergence.
	return mode.strictlyMoreSpecific(other.mode)
	    || (mode.moreSpecific(other.mode) &&
		t.isSubtypeOf(other.t));
    }
    
    public Type type() { return t; }
    public TypeBoundMode mode() { return mode; }
    
    public Type leastType() { return t; }
    
    public int hashCode() {
	return t.hashCode()+mode.hashCode();
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof TypeBound)) return false;
	TypeBound other=(TypeBound)other_;
	return t==other.t
	    && mode==other.mode;
    }
    
    public String toString() {
	return "TypeBound["+t+" "+mode+"]";
    }
    
    public boolean isTop() {
	return t.isObjectRoot() && !isExact();
    }
    
    public boolean isBottom() {
	return !t.hasInstances();
    }
    
    public boolean intersects(PTSet other) {
	if (isBottom()) return false;
	if (this==other) return true;
	if (other.isTop()) return true;
	if (other.isBottom()) return false;
	if (isExact()) {
	    if (other.isTypeExact()) {
		return t==other.leastType();
	    } else {
		return t.isSubtypeOf(other.leastType());
	    }
	} else {
	    if (other.isTypeExact()) {
		return other.leastType().isSubtypeOf(t);
	    } else if (t.getUnderlyingClass().isInterface()) {
		// FIXME: we can make this more precise with CHA
		return true;
	    } else {
		if (other.leastType().getUnderlyingClass().isInterface()) {
		    // FIXME: we can make this more precise with CHA
		    return true;
		} else {
		    return t.isSubtypeOf(other.leastType())
			|| other.leastType().isSubtypeOf(t);
		}
	    }
	}
    }
    
    public PTSet union(PTSet other) {
	if (other.isBottom() || equals(other)) {
	    return this;
	} else if (isTop()) {
	    return this;
	} else if (isBottom() || other.isTop()) {
	    return other;
	} else {
	    if (t==other.leastType()) {
		other=new TypeBound(t,mode.merge(other.typeBoundMode()));
	    } else {
		other=new TypeBound(t.lub(other.leastType()));
	    }
	    if (equals(other)) {
		return this;
	    } else {
		return other;
	    }
	}
    }
    
    public PTSet filter(Type other) {
	assert other.isObject();
        TypeBound otb=new TypeBound(other);
        if (intersects(otb)) {
            if (other.isStrictSubtypeOf(t)) {
                return otb;
            } else {
                return this;
            }
        } else {
            return bottom();
        }
    }

    public boolean exactlyBelongs(Type other) {
	if (isExact()) {
	    return t==other;
	} else {
	    return other.isSubtypeOf(t);
	}
    }

    public static final TypeBound OBJECT=
	new TypeBound(Global.root().objectClass.asType());
    public static final TypeBound OBJECT_EXACT=
	new TypeBound(Global.root().objectClass.asType(),
		      TypeBoundMode.EXACT);
}


