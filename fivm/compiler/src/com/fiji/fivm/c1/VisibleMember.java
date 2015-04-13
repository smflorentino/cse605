/*
 * VisibleMember.java
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


public abstract class VisibleMember
    extends Annotatable
    implements JNINameable, MemberLikeThing {
    
    VisibleClass clazz;
    
    int index;
    
    Binding binding;
    Visibility visibility;
    MemberSignature signature;

    boolean noNullCheckOnAccess;
    boolean nonZero;
    
    protected VisibleMember() {}

    VisibleMember(VisibleClass clazz,
		  int index,
		  Binding binding,
		  Visibility visibility,
		  MemberSignature signature) {
	this.clazz=clazz;
	this.index=index;
	this.binding=binding;
	this.visibility=visibility;
	this.signature=signature;
    }
    
    public Context getContext() { return clazz.getContext(); }
    
    public VisibleClass getClazz() { return clazz; }
    
    public int getIndex() { return index; }
    
    public Binding getBinding() { return binding; }
    public Visibility getVisibility() { return visibility; }
    
    public ActualBinding getActualBinding() {
        if (isStatic()) {
            return ActualBinding.STATIC;
        } else if (hasObjectReceiver()) {
            return ActualBinding.INSTANCE;
        } else {
            return ActualBinding.INSTANCE_UNBOXED;
        }
    }

    public boolean isStatic() { return binding==Binding.STATIC; }
    public boolean isInstance() { return !isStatic(); }
    
    public MemberSignature getSignature() { return signature; }
    
    public Type getType() { return signature.getType(); }
    public String getName() { return signature.getName(); }
    
    public abstract boolean shouldExist();
    
    public int runtimeFlags() {
	return getBinding().runtimeFlags()|getVisibility().runtimeFlags();
    }
    
    public boolean isVisibleFrom(VisibleClass from) {
	if (visibility==Visibility.PUBLIC) {
	    return true;
	}
	if (visibility==Visibility.PRIVATE) {
	    return getClazz()==from;
	}
	if (visibility==Visibility.PROTECTED &&
	    from.isSubclassOf(getClazz())) {
	    return true;
	}
	return from.getPackage()==getClazz().getPackage();
    }
    
    public boolean hasObjectReceiver() {
	return isInstance()
	    && getClazz().asType().isObject();
    }
    
    public boolean doNullCheckOnAccess() { return !noNullCheckOnAccess && hasObjectReceiver(); }
    public boolean noNullCheckOnAccess() { return !doNullCheckOnAccess(); }
    
    public boolean isNonZero() {
	return nonZero;
    }
    
    public abstract String canonicalName();
    
    public String localMangledName() {
        return Util.hidePunct(getClazz().simpleName())+"_"+Util.hidePunct(getName())+"_"+Util.hash(Global.name+"/"+canonicalName());
    }
    
    public String mangledName() {
	return Global.name+"_"+localMangledName();
    }
    
    public ResolutionID getResolutionID() {
        return getSignature().getResolutionID(getClazz());
    }
    
    public abstract void clearNameCaches();
}

