/*
 * CField.java
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


// could represent a field of a structure, a global variable, a function, or
// a local variable.  basically, anything in C land that has a type and a name.
// note that anything that cannot be loaded from or stored to directly (like an
// array or a struct) would have the type VOID.

// note that all CFields are compared and hashed using identity.  for CField
// instances (rather than instances of CField subclasses), this is ensured using
// hash consing.  subclasses, however, may be instantiated directly (like
// CLocal), in which case it is up to the user to ensure that equality and
// hashing works the way they expect.

public abstract class CField implements Cloneable {
    
    protected Basetype type;
    protected String name;
    protected ThreadLocalMode tlm;
    protected boolean typeUnknown;
    
    protected boolean getAddress;
    protected boolean getOffset;
    protected boolean read;
    protected boolean write;
    protected boolean call;
    
    protected CField(Basetype type,String name) {
	assert type.safeForC;
	this.type=type;
	this.name=name;
	this.tlm=ThreadLocalMode.SHARED;
        this.typeUnknown=false;
    }
    
    protected CField(boolean typeUnknown,Basetype type,String name) {
        this(type,name);
        this.typeUnknown=typeUnknown;
        if (typeUnknown) {
            assert type==Basetype.VOID;
        }
    }
    
    public Basetype getType() { return type; }
    public String getName() { return name; }
    
    public boolean typeKnown() {
        return !typeUnknown;
    }
    
    public boolean typeUnknown() {
        return typeUnknown;
    }

    public ThreadLocalMode getThreadLocalMode() { return tlm; }
    public void setThreadLocalMode(ThreadLocalMode tlm) { this.tlm=tlm; }
    
    public int hashCode() {
	return name.hashCode();
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof CField)) return false;
	CField other=(CField)other_;
	return name.equals(other.name);
    }
    
    public boolean sameTypesAs(CField other) {
        if (this.typeUnknown) {
            return true;
        } else if (other.typeUnknown) {
            return true;
        } else {
            return type==other.type;
        }
    }
    
    public boolean moreSpecificThan(CField other) {
        return typeKnown()
            && other.typeUnknown();
    }
    
    public CField copy() {
	try {
	    return (CField)clone();
	} catch (CloneNotSupportedException e) {
	    throw new Error(e);
	}
    }
    
    public void notifyGetAddress() {
        getAddress=true;
    }
    
    public void notifyGetOffset() {
        getOffset=true;
    }
    
    public void notifyRead() {
        read=true;
    }
    
    public void notifyWrite() {
        write=true;
    }
    
    public void notifyCall() {
        call=true;
    }
    
    public boolean usedForGetAddress() {
        return getAddress;
    }
    
    public boolean usedForGetOffset() {
        return getOffset;
    }
    
    public boolean usedForRead() {
        return read;
    }
    
    public boolean usedForWrite() {
        return write;
    }
    
    public boolean usedForCall() {
        return call;
    }
}

