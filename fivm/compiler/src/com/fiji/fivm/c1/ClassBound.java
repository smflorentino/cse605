/*
 * ClassBound.java
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

import java.nio.ByteBuffer;

public class ClassBound {
    VisibleClass klass;
    TypeBoundMode mode;
    
    public ClassBound(VisibleClass klass,
		      TypeBoundMode mode) {
	this.klass=klass;
	if (klass.isEffectivelyFinal()) {
	    this.mode=TypeBoundMode.EXACT;
	} else {
	    this.mode=mode;
	}
    }
    
    public ClassBound(VisibleClass klass) {
	this(klass,TypeBoundMode.UPPER_BOUND);
    }
    
    public boolean isExact() {
	return mode.isExact();
    }
    
    public boolean canRefine(ClassBound other) {
	if (other.isExact()) {
	    if (isExact()) {
		return klass==other.klass;
	    } else {
		return other.klass.isSubclassOf(klass);
	    }
	} else {
	    if (isExact()) {
		return klass.isSubclassOf(other.klass);
	    } else {
		return true;
	    }
	}
    }
    
    public boolean isMoreSpecificThan(ClassBound other) {
	return mode.strictlyMoreSpecific(other.mode)
	    || (mode.moreSpecific(other.mode) &&
		klass.isStrictSubclassOf(other.klass));
    }
    
    public VisibleClass clazz() { return klass; }
    public TypeBoundMode mode() { return mode; }
    
    public int hashCode() {
	return klass.hashCode()+mode.hashCode();
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof ClassBound)) return false;
	ClassBound other=(ClassBound)other_;
	return klass==other.klass
	    && mode==other.mode;
    }
    
    public String toString() {
	return "ClassBound["+klass+", "+mode+"]";
    }
    
    int getNioSize() {
        return 4+4;
    }
    
    void writeTo(ByteBuffer buffer) {
        buffer.putInt(Global.classCoding.codeFor(klass));
        buffer.putInt(mode.ordinal());
    }
    
    static ClassBound readFrom(ByteBuffer buffer) {
        return new ClassBound(
            Global.classCoding.forCode(buffer.getInt()),
            TypeBoundMode.values()[buffer.getInt()]);
    }
    
    public static final ClassBound OBJECT=
	new ClassBound(Global.root().objectClass);
    public static final ClassBound OBJECT_EXACT=
	new ClassBound(Global.root().objectClass,
		       TypeBoundMode.EXACT);
}


