/*
 * MethodTypeSignature.java
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

import com.fiji.fivm.ReturnMode;

public final class MethodTypeSignature implements Callable {
    
    Type result;
    Type[] params=Type.EMPTY;

    SideEffectMode sideEffect;
    SafepointMode safepoint;
    PollcheckMode pollcheck;
    ReturnMode returnMode;
    AllocationMechanism alloc;
    
    public MethodTypeSignature(Type result,Type[] params,
                               SideEffectMode sideEffect,
                               SafepointMode safepoint,
                               PollcheckMode pollcheck,
                               ReturnMode returnMode,
                               AllocationMechanism alloc) {
        this.result=result;
	this.params=params;
        this.safepoint=safepoint;
        this.sideEffect=sideEffect;
        this.pollcheck=pollcheck;
        this.returnMode=returnMode;
        this.alloc=alloc;
    }
    
    MethodTypeSignature(VisibleMethod vm) {
        this(vm.getType(),
             vm.getAllParams(),
             vm.sideEffect(),
             vm.safepoint(),
             vm.pollcheck(),
             vm.returnMode(),
             vm.alloc);
    }
    
    public void addParam(Type t) {
	Type[] newParams=new Type[params.length+1];
	System.arraycopy(params,0,
			 newParams,0,
			 params.length);
	params=newParams;
	params[params.length-1]=t;
    }
    
    public Type getResult() { return result; }
    public Type[] getParams() { return params; }
    
    public SideEffectMode sideEffect() { return sideEffect; }
    public SafepointMode safepoint() { return safepoint; }
    public PollcheckMode pollcheck() { return pollcheck; }
    public ReturnMode returnMode() { return returnMode; }
    public AllocationMechanism alloc() { return alloc; }
    
    public boolean canReturn() { return returnMode.canReturn(); }
    public boolean canThrow() { return returnMode.canThrow(); }
    public boolean doesNotReturn() { return !returnMode.canReturn(); }
    public boolean doesNotThrow() { return !returnMode.canThrow(); }

    public int hashCode() {
	return result.hashCode()+Util.hashCode(params)+sideEffect.hashCode()+
            safepoint.hashCode()+pollcheck.hashCode()+returnMode.hashCode()+
            alloc.hashCode();
    }
    
    public boolean equals(Object obj) {
	if (this==obj) return true;
	if (!(obj instanceof MethodTypeSignature)) return false;
	MethodTypeSignature other=(MethodTypeSignature)obj;
	return result==other.result
	    && Util.equals(params,other.params)
            && sideEffect==other.sideEffect
            && safepoint==other.safepoint
            && pollcheck==other.pollcheck
            && returnMode==other.returnMode
            && alloc==other.alloc;
    }
    
    public String toString() {
	String result="MethodTypeSig["+this.result+" (";
	boolean first=true;
	for (Type param : params) {
	    if (first) {
		first=false;
	    } else {
		result+=", ";
	    }
	    result+=param;
	}
	result+=")";
        result+=" "+sideEffect;
        result+=" "+safepoint;
        result+=" "+pollcheck;
        result+=" "+returnMode;
        result+=" "+alloc;
        return result+"]";
    }
    
    public String canonicalName() {
	String result="MethodTypeSig["+this.result.canonicalName()+"(";
	boolean first=true;
	for (Type param : params) {
	    if (first) {
		first=false;
	    } else {
		result+=", ";
	    }
	    result+=param.canonicalName();
	}
	return result+")]";
    }
    
    public String jniSignature() {
	StringBuffer buf=new StringBuffer();
	buf.append("(");
	for (Type param : params) {
	    buf.append(param.jniName());
	}
	buf.append(")");
	buf.append(getResult().jniName());
	return buf.toString();
    }
    
    NativeSignature nsig;
    public synchronized NativeSignature getNativeSignature() {
        if (nsig==null) {
            nsig=Global.makeNativeSig(this,getResult(),getParams());
        }
        return nsig;
    }
}


