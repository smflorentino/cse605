/*
 * NativeSignature.java
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

import com.fiji.fivm.ReturnMode;

import com.fiji.config.*;

public class NativeSignature implements Callable {
    Basetype result;
    Basetype[] params;
    
    SafepointMode safepoint;
    SideEffectMode sideEffect;
    PollcheckMode pollcheck;

    public NativeSignature(Basetype result,
			   Basetype[] params,
			   SideEffectMode sideEffect,
			   SafepointMode safepoint,
			   PollcheckMode pollcheck) {
	this.result=result;
	this.params=params;
	this.safepoint=safepoint;
	this.sideEffect=sideEffect;
	this.pollcheck=pollcheck;
    }
    
    public NativeSignature(Basetype result,
			   Basetype[] params,
			   SideEffectMode sideEffect,
			   SafepointMode safepoint) {
	this(result,params,
	     sideEffect,
	     safepoint,
	     PollcheckMode.EXPLICIT_POLLCHECKS_ONLY);
    }

    public NativeSignature(Basetype result,
			   Basetype[] params,
			   SideEffectMode sideEffect) {
	this(result,params,
	     sideEffect,
	     SafepointMode.MAY_SAFEPOINT,
	     PollcheckMode.EXPLICIT_POLLCHECKS_ONLY);
    }

    public NativeSignature(Basetype result,
			   Basetype[] params,
			   SafepointMode safepoint) {
	this(result,params,
	     SideEffectMode.CLOBBERS_WORLD,
	     safepoint,
	     PollcheckMode.EXPLICIT_POLLCHECKS_ONLY);
    }

    public NativeSignature(Basetype result,
			   Basetype[] params) {
	this(result,params,
	     SideEffectMode.CLOBBERS_WORLD,
	     SafepointMode.MAY_SAFEPOINT,
	     PollcheckMode.EXPLICIT_POLLCHECKS_ONLY);
    }
    
    public NativeSignature(Basetype result,
			   Basetype[] params,
			   Callable other) {
	this(result,
	     params,
	     other.sideEffect(),
	     other.safepoint(),
	     other.pollcheck());
    }
    
    public Basetype result() { return result; }
    public Basetype[] params() { return params; }
    public Basetype param(int i) { return params[i]; }
    public int nparams() { return params.length; }
    
    public SafepointMode safepoint() { return safepoint; }
    public SideEffectMode sideEffect() { return sideEffect; }
    public PollcheckMode pollcheck() { return pollcheck; }
    public ReturnMode returnMode() { return ReturnMode.ONLY_RETURN; }
    
    public String ctype() {
	return result().cType.asCCode()+" (*)"+paramList();
    }
    
    public String cvarDecl(String varname) {
	return result().cType.asCCode()+" (*"+varname+")"+paramList();
    }
    
    public String paramList() {
	StringBuffer buf=new StringBuffer();
	buf.append("(");
	if (params.length==0) {
	    buf.append("void");
	} else {
	    for (int i=0;i<params.length;++i) {
		if (i!=0) {
		    buf.append(",");
		}
		buf.append(params[i].cType.asCCode());
		buf.append(" arg");
		buf.append(i);
	    }
	}
	buf.append(")");
	return buf.toString();
    }
    
    public void generateDeclarationBase(PrintWriter w,
					String name) {
	w.print(result.cType.asCCode());
	w.print(" ");
	w.print(name);
	w.print(paramList());
    }
    
    public void generateDeclaration(PrintWriter w,
				    String name) {
	generateDeclarationBase(w,name);
	w.println(";");
    }
    
    public ConfigMapNode asConfigNode() {
        ConfigMapNode ret=new ConfigMapNode();
        ret.put("result",result.name());
        
        ConfigListNode paramNode=new ConfigListNode();
        for (Basetype param : params) {
            paramNode.append(param.name());
        }
        ret.put("params",paramNode);
        
        ret.put("safepoint",safepoint.name());
        ret.put("sideEffect",sideEffect.name());
        ret.put("pollcheck",pollcheck.name());
        
        return ret;
    }
    
    public static NativeSignature fromConfigNode(ConfigMapNode node) {
        ConfigListNode paramNode=node.getList("params");
        Basetype[] params=new Basetype[paramNode.size()];
        for (int i=0;i<params.length;++i) {
            params[i]=Basetype.valueOf(paramNode.getString(i));
        }
        
        return new NativeSignature(Basetype.valueOf(node.getString("result")),
                                   params,
                                   SideEffectMode.valueOf(node.getString("sideEffect")),
                                   SafepointMode.valueOf(node.getString("safepoint")),
                                   PollcheckMode.valueOf(node.getString("pollcheck")));
    }

    public int hashCode() {
	int result=params.length+this.result.hashCode();
	for (Basetype p : params) {
	    result=result*59+p.hashCode();
	}
	return result;
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof NativeSignature)) return false;
	NativeSignature other=(NativeSignature)other_;
	return result==other.result
	    && Util.equals(params,other.params);
    }
    
    public String toString() {
	return "NativeSignature[result = "+result+", params = "+
	    Util.dump(params)+"]";
    }
    
    public String canonicalName() {
	return toString();
    }
}

