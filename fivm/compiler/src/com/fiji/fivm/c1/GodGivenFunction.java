/*
 * GodGivenFunction.java
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

import java.io.*;

public class GodGivenFunction extends Function {
    
    private GodGivenFunction(String name,NativeSignature sig) {
	super(name,sig);
    }
    
    private GodGivenFunction(String name,Basetype result,Basetype[] params) {
	super(name,result,params);
    }
    
    private GodGivenFunction(String name,Basetype result,Basetype[] params,
                             SideEffectMode sideEffect,
                             SafepointMode safepoint) {
	super(name,new NativeSignature(result,params,sideEffect,safepoint));
    }
    
    private GodGivenFunction(String name,Basetype result,Basetype[] params,
                             SafepointMode safepoint) {
	super(name,new NativeSignature(result,params,safepoint));
    }
    
    private GodGivenFunction(String name,Basetype result,Basetype[] params,
                             SideEffectMode sideEffect) {
	super(name,new NativeSignature(result,params,sideEffect));
    }
    
    private GodGivenFunction(String name,Basetype result,Basetype[] params,Callable other) {
	super(name,result,params,other);
    }
    
    public static GodGivenFunction make(String name, NativeSignature sig) {
	return CTypesystemReferences.addGGF(new GodGivenFunction(name, sig));
    }

    public static GodGivenFunction make(String name, Basetype result,
                                        Basetype[] params) {
	return CTypesystemReferences.addGGF(new GodGivenFunction(name, result, params));
    }

    public static GodGivenFunction make(String name, Basetype result,
                                        Basetype[] params,
                                        SideEffectMode sideEffect,
                                        SafepointMode safepoint) {
	return CTypesystemReferences.addGGF(new GodGivenFunction(name, result, params, sideEffect, safepoint));
    }

    public static GodGivenFunction make(String name, Basetype result,
                                        Basetype[] params,
                                        SafepointMode safepoint) {
	return CTypesystemReferences.addGGF(new GodGivenFunction(name, result, params, safepoint));
    }

    public static GodGivenFunction make(String name, Basetype result,
                                        Basetype[] params,
                                        SideEffectMode sideEffect) {
	return CTypesystemReferences.addGGF(new GodGivenFunction(name, result, params, sideEffect));
    }

    public static GodGivenFunction make(String name, Basetype result,
                                        Basetype[] params,
                                        Callable other) {
	return CTypesystemReferences.addGGF(new GodGivenFunction(name, result, params, other));
    }

    public boolean isLocal() { return false; }
    
    // no declaration because it's given to us by God
    public void generateDeclaration(PrintWriter w) {}
    
    public RemoteFunction makeCall() {
        return new RemoteFunction(Global.name+"_call_"+name,
                                  getSignature());
    }
    
    public LocalFunction makeCallLocal() {
        RemoteFunction rf=makeCall();
        return new LocalFunction(rf.getName(),rf.getSignature()){
            public void generateCode(PrintWriter w) {
                w.print("   ");
                if (getResult()!=Basetype.VOID) {
                    w.print("return ");
                }
                w.print(GodGivenFunction.this.name+"(");
                for (int i=0;i<numParams();++i) {
                    if (i!=0) {
                        w.print(",");
                    }
                    w.print("("+getParam(i).cTypeForCall.asCCode()+")arg"+i);
                }
                w.println(");");
            }
        };
    }
}

