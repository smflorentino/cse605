/*
 * GodGivenCVar.java
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

import java.io.PrintWriter;

public class GodGivenCVar extends CField {
    private GodGivenCVar(boolean typeUnknown, Basetype type,String name) {
	super(type,name);
    }
    
    public static GodGivenCVar make(Basetype type, String name) {
	return CTypesystemReferences.addGGCV(new GodGivenCVar(false, type, name));
    }

    public static GodGivenCVar make(String name) {
	return CTypesystemReferences.addGGCV(new GodGivenCVar(true, Basetype.VOID, name));
    }

    public void generateDeclaration(PrintWriter w) {
	// nothing to do
    }
    
    public String toString() {
	return "GodGivenCVar["+type+" "+name+"]";
    }
    
    public RemoteFunction makeGetAddress() {
        return new RemoteFunction(
            Global.name+"_getAddress_"+name,
            new NativeSignature(Basetype.POINTER,
                                Basetype.EMPTY,
                                SideEffectMode.PURE,
                                SafepointMode.CANNOT_SAFEPOINT,
                                PollcheckMode.EXPLICIT_POLLCHECKS_ONLY));
    }
    
    public RemoteFunction makeWrite() {
        return new RemoteFunction(
            Global.name+"_write_"+name,
            new NativeSignature(Basetype.VOID,
                                new Basetype[]{
                                    type
                                },
                                SideEffectMode.CLOBBERS_WORLD,
                                SafepointMode.CANNOT_SAFEPOINT,
                                PollcheckMode.EXPLICIT_POLLCHECKS_ONLY));
    }
    
    public RemoteFunction makeRead() {
        return new RemoteFunction(
            Global.name+"_read_"+name,
            new NativeSignature(type,
                                Basetype.EMPTY,
                                SideEffectMode.PURE,
                                SafepointMode.CANNOT_SAFEPOINT,
                                PollcheckMode.EXPLICIT_POLLCHECKS_ONLY));
    }
    
    public LocalFunction makeGetAddressLocal() {
        RemoteFunction rf=makeGetAddress();
        return new LocalFunction(rf.getName(),rf.getSignature()){
            public void generateCode(PrintWriter w) {
                w.println("   return (uintptr_t)&"+GodGivenCVar.this.name+";");
            }
        };
    }
    
    public LocalFunction makeWriteLocal() {
        RemoteFunction rf=makeWrite();
        return new LocalFunction(rf.getName(),rf.getSignature()){
            public void generateCode(PrintWriter w) {
                w.println("   "+GodGivenCVar.this.name+" = arg0;");
            }
        };
    }
    
    public LocalFunction makeReadLocal() {
        RemoteFunction rf=makeRead();
        return new LocalFunction(rf.getName(),rf.getSignature()){
            public void generateCode(PrintWriter w) {
                w.println("   return "+GodGivenCVar.this.name+";");
            }
        };
    }
}

