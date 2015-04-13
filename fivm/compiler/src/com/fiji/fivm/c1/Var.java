/*
 * Var.java
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

import java.util.*;
import java.nio.*;

public class Var extends Arg implements Cloneable {
    
    Code code;
    Exectype t;
    Exectype origType;
    int id;
    int permID;
    Object cookie;
    Instruction inst;
    
    private static final Instruction MULTI_ASSIGNED=
        new Instruction() {
            public <T> T accept(Visitor<T> v) {
                throw new Error();
            }
        };
    
    Var(Code code,Exectype t,int id,int permID) {
        this.code=code;
	this.t=t;
	this.id=id;
        this.permID=permID;
    }
    
    Var(Code code,Exectype t,int id,Exectype origType,int permID) {
        this.code=code;
	this.t=t;
	this.id=id;
        this.origType=origType;
        this.permID=permID;
    }
    
    Var(Code code,Exectype t) {
        this.code=code;
	assert t!=Exectype.VOID;
	this.t=t;
	this.id=-1;
        this.permID=-1;
    }
    
    Var(Code code,Exectype t,Exectype origType) {
        this(code,t);
        this.origType=origType;
    }
    
    public Code code() { return code; }
    
    public int id() { return id; }
    
    public int permID() { return permID; }

    public int rank() { return 1000+id; }
    
    public Exectype type() { return t; }
    
    public Exectype origType() {
        if (origType==null) {
            return type();
        } else {
            return origType;
        }
    }
    
    public Instruction inst() {
        if (inst==MULTI_ASSIGNED) {
            return null;
        } else {
            return inst;
        }
    }
    
    public boolean isMultiAssigned() {
        return inst==MULTI_ASSIGNED;
    }
    
    void makeMultiAssigned() {
        inst=MULTI_ASSIGNED;
    }
    
    void setInst(Instruction i) {
        if (code.isSSA() || inst==null || inst==i) {
            inst=i;
        } else {
            inst=MULTI_ASSIGNED;
        }
    }
    
    void clearInst() {
        if (inst!=MULTI_ASSIGNED || code.isSSA()) {
            inst=null;
        }
    }
    
    public Header head() {
        return inst.head();
    }
    
    public boolean lub(Exectype newType) {
	newType=t.lub(newType);
	if (newType==t) {
	    return false;
	} else {
	    t=newType;
	    return true;
	}
    }
    
    public String toString() { return "("+t+")Var["+permID+"]"; }
    
    Var copy() {
	if (this==VOID) {
	    return VOID;
	} else {
	    return new Var(code,t,id,origType,permID);
	}
    }
    
    Var copy(int newPermID) {
	if (this==VOID) {
	    return VOID;
	} else {
	    return new Var(code,t,id,origType,newPermID);
	}
    }
    
    public int structuralHashCode() {
	return id;
    }
    
    public boolean structuralEquals(Arg other) {
	return this==other;
    }
    
    static int getVarNioSize() {
        return 4;
    }
    
    void writeVarTo(ByteBuffer buffer) {
        buffer.putInt(Global.typeCoding.codeFor(t.asType()));
        buffer.putInt(permID);
        buffer.putInt(Global.typeCoding.codeFor(origType==null?null:origType.asType()));
    }
    
    // readVarFrom
    Var(Code code,
        int id,
        ByteBuffer buffer) {
        this.code=code;
        this.t=Global.typeCoding.forCode(buffer.getInt()).asExectype();
        this.id=id;
        this.permID=buffer.getInt();
        Type origTypeType=Global.typeCoding.forCode(buffer.getInt());
        if (origTypeType==null) {
            this.origType=null;
        } else {
            this.origType=origTypeType.asExectype();
        }
    }
    
    int getArgNioSize() {
        return 1+4;
    }
    
    void writeArgTo(ByteBuffer buffer) {
        buffer.put((byte)0);
        buffer.putInt(id);
    }
    
    public static Var VOID = new Var(null,Exectype.VOID,-1,-1){
            {
                inst=MULTI_ASSIGNED;
            }
            void setInst(Instruction i) {}
            void clearInst() {}
        };
    
    public static Var[] EMPTY=new Var[0];
    public static HashSet< Var > EMPTY_HS=new HashSet< Var >();
    
}


