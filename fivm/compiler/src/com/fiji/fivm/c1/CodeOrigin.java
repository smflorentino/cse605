/*
 * CodeOrigin.java
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

import java.nio.*;

import com.fiji.fivm.ReturnMode;

public class CodeOrigin implements Callable, JNINameable, Contextable {
    VisibleClass owner;
    
    // FIXME: this should have a reference to MethodTypeSignature ... or not,
    // who knows...
    
    VisibleMethod method;
    VisibleMethod origin;
    
    PollcheckMode pollcheck;
    UnsafeMode unsafe;
    
    SafetyCheckMode safetyChecks;
    
    ReturnMode returnMode;
    
    StackOverflowCheckMode stackOverflowCheckMode;
    
    String anonymousID;
    
    public CodeOrigin(VisibleClass owner,
		      VisibleMethod method,
		      VisibleMethod origin,
		      PollcheckMode pollcheck,
		      UnsafeMode unsafe,
		      SafetyCheckMode safetyChecks,
                      ReturnMode returnMode,
                      StackOverflowCheckMode stackOverflowCheckMode,
                      String anonymousID) {
	this.owner=owner;
	this.method=method;
	this.origin=origin;
	this.pollcheck=pollcheck;
	this.unsafe=unsafe;
	this.safetyChecks=safetyChecks;
        this.returnMode=returnMode;
        this.stackOverflowCheckMode=stackOverflowCheckMode;
        this.anonymousID=anonymousID;
        assert pollcheck!=null;
        assert unsafe!=null;
        assert safetyChecks!=null;
        assert returnMode!=null;
        assert stackOverflowCheckMode!=null;
        if (method==null) {
            assert anonymousID!=null;
        } else {
            assert anonymousID==null;
        }
    }
    
    /** The class from which this code originated. */
    public VisibleClass owner() { return owner; }
    
    public String sourceFilename() {
        String result=owner().getSourceFilename();
        if (result==null) {
            return "<unknown>";
        } else {
            return result;
        }
    }
    
    public Context getContext() { return owner.getContext(); }
    
    /** The method whose code this is.  May be null if this is a synthetic
	method. */
    public VisibleMethod method() { return method; }
    
    /** The method that caused this code to be generated.  Currently, even
	synthetic methods always originate with some actual method, so this
	will be non-null - but that may change in the future. */
    public VisibleMethod origin() { return origin; }
    
    public ResolutionID getResolutionID() {
        if (method!=null) {
            return method.getResolutionID();
        }
        if (origin!=null) {
            return origin.getResolutionID();
        }
        return owner.getResolutionID();
    }
    
    public SideEffectMode sideEffect() { return origin.sideEffect(); }
    public SafepointMode safepoint() { return origin.safepoint(); }
    
    public boolean hasSafepoints() { return safepoint()==SafepointMode.MAY_SAFEPOINT; }
    public boolean noSafepoints() { return safepoint()==SafepointMode.CANNOT_SAFEPOINT; }
    
    public PollcheckMode pollcheck() { return pollcheck; }
    public UnsafeMode unsafe() { return unsafe; }
    
    public boolean noPollcheck() { return pollcheck==PollcheckMode.EXPLICIT_POLLCHECKS_ONLY; }
    public boolean allowUnsafe() { return unsafe==UnsafeMode.ALLOW_UNSAFE; }
    
    public SafetyCheckMode safetyChecks() { return safetyChecks; }
    
    public boolean hasSafetyChecks() { return safetyChecks==SafetyCheckMode.IMPLICIT_SAFETY_CHECKS; }
    public boolean noSafetyChecks() { return safetyChecks==SafetyCheckMode.EXPLICIT_SAFETY_CHECKS_ONLY; }
    
    public ReturnMode returnMode() { return returnMode; }
    
    public StackOverflowCheckMode stackOverflowCheckMode() { return stackOverflowCheckMode; }
    
    public boolean hasStackOverflowCheck() {
        return stackOverflowCheckMode==StackOverflowCheckMode.CHECK_STACK_OVERFLOW;
    }
    
    public boolean noStackOverflowCheck() {
        return stackOverflowCheckMode==StackOverflowCheckMode.DONT_CHECK_STACK_OVERFLOW;
    }
    
    public boolean compatible(CodeOrigin other) {
	return CallableUtil.compatible(this,other);
    }
    
    public int hashCode() {
	return owner.hashCode()+(method==null?0:method.hashCode())+
	    origin.hashCode()+pollcheck.hashCode()+unsafe.hashCode();
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof CodeOrigin)) return false;
	CodeOrigin other=(CodeOrigin)other_;
	return owner==other.owner
	    && method==other.method
	    && origin==other.origin
	    && pollcheck==other.pollcheck
	    && unsafe==other.unsafe
            && (anonymousID==null)==(other.anonymousID==null)
            && (anonymousID==null || anonymousID.equals(other.anonymousID));
    }
    
    String cachedToString=null;
    public String toString() {
        if (cachedToString==null) {
            cachedToString="CodeOrigin[";
            if (owner!=origin.getClazz()) {
                cachedToString+=owner+" ";
            }
            if (method!=null) {
                cachedToString+=method+" ";
            } else {
                cachedToString+="<synthetic> ";
            }
            if (origin!=method) {
                cachedToString+=origin+" ";
            }
            cachedToString+=pollcheck+" "+unsafe+" "+safetyChecks;
            cachedToString+="]";
        }
        return cachedToString;
    }
    
    public String jniName() {
        if (method()!=null) {
            return method().jniName();
        }
        if (origin()!=null) {
            return "[Synthetic '"+anonymousID+"', origin = "+origin().jniName()+"]";
        }
        return "[Anonymous '"+anonymousID+"', owner = "+owner().jniName()+"]";
    }
    
    public String shortName() {
        if (method()!=null) {
            return method().shortName();
        }
        if (origin()!=null) {
            return "[Synthetic '"+anonymousID+"', origin = "+origin().shortName()+"]";
        }
        return "[Anonymous '"+anonymousID+"', owner = "+owner().shortName()+"]";
    }
    
    public int getNioSize() {
        return 4+4+4+4+4+4;
    }
    
    public void writeTo(ByteBuffer buffer) {
        buffer.putInt(Global.classCoding.codeFor(owner));
        buffer.putInt(Global.methodCoding.codeFor(method));
        buffer.putInt(Global.methodCoding.codeFor(origin));
        buffer.putInt(pollcheck.ordinal());
        buffer.putInt(unsafe.ordinal());
        buffer.putInt(safetyChecks.ordinal());
        buffer.putInt(stackOverflowCheckMode.ordinal());
        buffer.putInt(Global.anonymousIDCoding.codeFor(anonymousID));
    }
    
    private CodeOrigin() {}

    public static CodeOrigin readFrom(ByteBuffer buffer) {
        CodeOrigin result=new CodeOrigin();
        result.owner        = Global.classCoding.forCode(buffer.getInt());
        result.method       = Global.methodCoding.forCode(buffer.getInt());
        result.origin       = Global.methodCoding.forCode(buffer.getInt());
        result.pollcheck    = PollcheckMode.table[buffer.getInt()];
        result.unsafe       = UnsafeMode.table[buffer.getInt()];
        result.safetyChecks = SafetyCheckMode.table[buffer.getInt()];
        result.stackOverflowCheckMode = StackOverflowCheckMode.table[buffer.getInt()];
        result.anonymousID  = Global.anonymousIDCoding.forCode(buffer.getInt());
        return result;
    }
}


