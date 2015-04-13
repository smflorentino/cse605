/*
 * VisibleField.java
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

import com.fiji.fivm.om.OMField;

public class VisibleField
    extends VisibleMember
    implements FieldLikeThing, OMField, LocationKey {
    
    Mutability mutability;
    Volatility volatility;
    Serializability serializability;
    TraceMode traceMode;

    /**
     * The object-model-specific "offset" of the field.  Note that in some
     * object models this is NOT an offset that can be used directly for
     * pointer arithmetic.  As well, what this is an offset from is defined
     * by the object model; in some cases it is from where the object pointer
     * points, while in other cases it's from some other location in the
     * object (and in some cases it isn't really an offset at all, as much
     * as an identifier of the field).  Thus: any direct use of this field
     * is likely to be object-model-specific.
     */
    int location=-1;
    
    Object defaultValue=null;
    
    VisibleField(VisibleClass clazz,
		 int index,
		 Binding binding,
		 Visibility visibility,
		 Mutability mutability,
		 Volatility volatility,
		 Serializability serializability,
		 FieldSignature signature) {
	super(clazz,index,binding,visibility,signature);
	this.mutability=mutability;
	this.volatility=volatility;
	this.serializability=serializability;
	this.traceMode=getType().defaultTraceMode();
    }
    
    public FieldSignature getSignature() { return (FieldSignature)signature; }
    
    public Mutability mutability() { return mutability; }
    public Volatility volatility() { return volatility; }
    public Serializability serializability() { return serializability; }
    public TraceMode traceMode() { return traceMode; }
    
    public boolean isTraced() {
        return traceMode().isTraced();
    }
    
    public boolean isReference() {
        return traceMode().isReference();
    }
    
    public boolean isBarriered() {
        return traceMode().isBarriered();
    }
    
    public boolean isFinal() { return mutability==Mutability.FINAL; }
    public boolean isVolatile() { return volatility==Volatility.VOLATILE; }
    public boolean isTransient() { return serializability==Serializability.TRANSIENT; }
    
    public int runtimeFlags() {
	return super.runtimeFlags()|mutability().runtimeFlags()|
	    volatility().runtimeFlags()|serializability.runtimeFlags()|
	    traceMode().runtimeFlags();
    }
    
    public String canonicalName() {
	return "Field["+clazz.canonicalName()+" "+binding+" "+visibility+" "+signature.canonicalName()+"]";
    }
    
    public String toString() {
	String result="Field["+clazz+" "+binding+" "+visibility+" "+mutability+
	    " "+volatility+" "+serializability+" "+signature+" "+traceMode;
	if (Global.analysis().closed()) {
	    result+=" (value = "+Global.analysis().setFor(this)+")";
	}
	return result+"]";
    }
    
    public String jniName() {
	return getClazz().asType().jniName()+"/"+getName()+"/"+getType().jniName();
    }
    
    public String shortName() {
        return getContext().description()+":"+jniName();
    }
    
    public boolean shouldExist() {
	return Global.analysis().isLive(this);
    }

    public int location() {
	assert location>=0 : this;
	return location;
    }
    
    public int offset() {
        return Global.om.locationToOffset(location());
    }
    
    public void setLocation(int location) {
        this.location = location;
    }
    
    public int size() {
        return getType().effectiveBasetype().bytes;
    }
    
    public void clearNameCaches() {
    }
    
    public static final VisibleField[] EMPTY=new VisibleField[0];
}

