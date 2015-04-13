/*
 * MethodSignature.java
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

import com.fiji.fivm.*;

public final class MethodSignature
    extends MemberSignature
    implements MethodSignaturable {
    
    Type[] params=Type.EMPTY;
    
    public MethodSignature(Type type,String name) {
	super(type,name);
    }
    
    public MethodSignature(Type type,String name,Type[] params) {
	super(type,name);
	this.params=params;
    }
    
    public static MethodSignature parse(Context c,String desc,String name) {
	if (!desc.startsWith("(")) {
	    throw new BadBytecode(
		"Bad method descriptor in: "+desc+
		" (does not start with a '(')");
	}
	try {
	    MethodSignature result=new MethodSignature(null,name);
            TypeParsing.MethodSigStrings mss=TypeParsing.splitMethodSig(desc);
            for (String param : mss.params()) {
                result.addParam(Type.parse(c,param));
            }
            result.type=Type.parse(c,mss.result());
	    return result;
	} catch (ResolutionFailed e) {
	    throw e;
	} catch (Exception e) {
	    throw new BadBytecode(
		"Bad method descriptor in: "+desc,e);
	}
    }
    
    public static MethodSignature parse(Context c,String desc) {
	int paren=desc.indexOf('(');
	if (paren<0) {
	    throw new BadBytecode("Bad method descriptor: "+desc);
	}
	return parse(c,desc.substring(paren),desc.substring(0,paren));
    }
    
    public MethodSignature getSignature() { return this; }
    
    public void addParam(Type t) {
	Type[] newParams=new Type[params.length+1];
	System.arraycopy(params,0,
			 newParams,0,
			 params.length);
	params=newParams;
	params[params.length-1]=t;
    }
    
    public Type[] getParams() { return params; }
    
    public boolean isInitializer() {
	return getName().equals("<init>")
	    || getName().equals("<clinit>");
    }
    
    public int hashCode() {
	return super.hashCode()+Util.hashCode(params);
    }
    
    public boolean equals(Object obj) {
	if (this==obj) return true;
	if (!(obj instanceof MethodSignature)) return false;
	MethodSignature other=(MethodSignature)obj;
	return type==other.type
	    && name.equals(other.name)
	    && Util.equals(params,other.params);
    }
    
    public String toString() {
	String result="MethodSig["+type+" "+name+"(";
	boolean first=true;
	for (Type param : params) {
	    if (first) {
		first=false;
	    } else {
		result+=", ";
	    }
	    result+=param;
	}
	return result+")]";
    }
    
    public String canonicalName() {
	String result="MethodSig["+type.canonicalName()+" "+name+"(";
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
    
    public MethodSignature checkResolved() {
        try {
            getType().checkResolved();
            for (Type t : params) {
                t.checkResolved();
            }
        } catch (ResolutionFailed e) {
            throw new ResolutionFailed(
                e.getClazz(),
                e.getResolutionID(),
                "Could not resolve method signature "+jniName()+" due to "+
                e.getClazz().jniName(),
                e);
        }
        return this;
    }
    
    public String jniSignature() {
	StringBuilder buf=new StringBuilder();
	buf.append("(");
        buf.append(Type.jniName(params));
	buf.append(")");
	buf.append(getType().jniName());
	return buf.toString();
    }
    
    public String jniName() {
        return getName()+jniSignature();
    }
}


