/*
 * DebugInfo.java
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

/**
 * The meta-data for a node.  Note that this information is not
 * optional.  You should never clear it, and you should take care to ensure that
 * newly generated code carries the debug info that it replaced.
 */
public class DebugInfo implements Contextable {
    CodeOrigin origin;
    int pc;
    int lineNumber;
    DebugInfo caller;
    
    static final int FL_PATCH_POINT = 1;
    
    int flags;

    public DebugInfo(CodeOrigin origin,int pc,int lineNumber) {
	this.origin=origin;
	this.pc=pc;
	this.caller=null;
	this.lineNumber=lineNumber;
        this.flags=0;
    }
    
    public DebugInfo(Code code,int pc,int lineNumber) {
	this.origin=code.origin();
	this.pc=pc;
	this.caller=null;
	this.lineNumber=lineNumber;
        this.flags=0;
    }
    
    public DebugInfo(CodeOrigin origin,int pc,int lineNumber,DebugInfo caller) {
	this.origin=origin;
	this.pc=pc;
	this.caller=caller;
	this.lineNumber=lineNumber;
        this.flags=0;
    }
    
    public DebugInfo(CodeOrigin origin,int pc,int lineNumber,DebugInfo caller,int flags) {
	this.origin=origin;
	this.pc=pc;
	this.caller=caller;
	this.lineNumber=lineNumber;
        this.flags=flags;
    }
    
    public CodeOrigin origin() { return origin; }
    public int pc() { return pc; }
    public int lineNumber() { return lineNumber; }
    public boolean isInlined() { return caller!=null; }
    public DebugInfo caller() { return caller; }
    
    public boolean isPatchPoint() { return (flags&FL_PATCH_POINT)!=0; }
    
    public String sourceDescription() {
        return origin().sourceFilename()+":"+lineNumber()+":"+pc();
    }
    
    public String shortName() {
        return origin().shortName()+" "+sourceDescription();
    }
    
    public DebugInfo withCaller(DebugInfo caller) {
	return new DebugInfo(origin,pc,lineNumber,caller,flags);
    }
    
    public DebugInfo addCaller(DebugInfo caller) {
	if (this.caller==null) {
	    return withCaller(caller);
	} else {
	    return withCaller(this.caller.addCaller(caller));
	}
    }
    
    public DebugInfo asPatchPoint() {
        return new DebugInfo(origin,pc,lineNumber,caller,flags|FL_PATCH_POINT);
    }
    
    public Context getContext() {
        return origin.getContext();
    }
    
    public String toString() {
        String result="DebugInfo["+origin+", "+pc+", "+lineNumber;
        if (caller!=null) {
            result+=", "+caller;
        }
        if ((flags&FL_PATCH_POINT)!=0) {
            result+=", PATCH_POINT";
        }
        result+="]";
        return result;
    }
    
    public int hashCode() {
	int result=origin.hashCode()+lineNumber+flags+pc;
	if (caller!=null) {
	    result*=7;
	    result+=caller.hashCode();
	}
	return result;
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof DebugInfo)) return false;
	DebugInfo other=(DebugInfo)other_;
	return origin.equals(other.origin)
	    && lineNumber==other.lineNumber
	    && (caller==null && other.caller==null) 
	    && (caller==null || caller.equals(other.caller))
            && flags==other.flags
            && pc==other.pc;
    }
    
    int getNioSize() {
        return 4*5;
    }
    
    void writeTo(NioContext ctx,
                 ByteBuffer buffer) {
        buffer.putInt(ctx.coCodes.codeFor(origin));
        buffer.putInt(pc);
        buffer.putInt(lineNumber);
        buffer.putInt(ctx.diCodes.codeFor(caller));
        buffer.putInt(flags);
    }
    
    DebugInfo(NioRead r) {}
    
    void readFrom(NioContext ctx,
                  ByteBuffer buffer) {
        origin     = ctx.coCodes.forCode(buffer.getInt());
        pc         = buffer.getInt();
        lineNumber = buffer.getInt();
        caller     = ctx.diCodes.forCode(buffer.getInt());
        flags      = buffer.getInt();
    }
}

