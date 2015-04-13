/*
 * Exectype.java
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

/** Represents the typesystem used for bytecode execution.  It's quite goofy.
    Here are some things it does:
    1) Interfaces are blown away.  All interface types become java.lang.Object.
    2) Integer types smaller than 32-bit are turned into 32-bit INT, except
       in the case of arrays of said types. */
public final class Exectype extends TypeImpl< Exectype > {
    
    Type cachedType;
    Exectype cachedArray;
    
    Exectype(Basetype basetype,VisibleClass clazz,int arrayDepth) {
	super(basetype,clazz,arrayDepth);
    }
    
    // FIXME: this could be done without a hashmap!
    // (worry about that only when we need the performance)
    
    private static HashMap< TypeKey, Exectype > hashConsMap;

    private static synchronized Exectype construct(Basetype basetype,
                                                   VisibleClass clazz,
                                                   int arrayDepth) {
	assert basetype!=Basetype.OBJECT ^ clazz!=null;
        TypeKey k=new TypeKey(basetype,clazz,arrayDepth);
        assert !hashConsMap.containsKey(k) : k;
        Exectype result=new Exectype(k.basetype,k.clazz,k.arrayDepth);
        hashConsMap.put(k,result);
	return result;
    }
    
    private static Exectype construct(Basetype basetype) {
        return construct(basetype,null,0);
    }
    
    static Exectype make(Basetype basetype,VisibleClass clazz,int arrayDepth) {
	assert basetype!=Basetype.OBJECT ^ clazz!=null;
        Exectype result;
        if (clazz!=null) {
            synchronized (clazz) {
                if (clazz.myExectype==null) {
                    clazz.myExectype=construct(basetype,clazz,0);
                }
                result=clazz.myExectype;
            }
        } else {
            result=basetype.asExectypeInternal;
        }
        while (arrayDepth-->0) {
            synchronized (result) {
                if (result.cachedArray==null) {
                    result.cachedArray=construct(result.basetype,
                                                 result.clazz,
                                                 result.arrayDepth+1);
                }
                result=result.cachedArray;
            }
        }
        return result;
    }
    
    Exectype make_(Basetype basetype,VisibleClass clazz,int arrayDepth) {
	return make(basetype,clazz,arrayDepth);
    }
    
    public static Basetype translate(Basetype basetype,int arrayDepth) {
	if (arrayDepth==0) {
	    switch (basetype) {
	    case BOOLEAN:
	    case BYTE:
	    case CHAR:
	    case SHORT:
		return Basetype.INT;
	    case VM_FCPTR:
		return Basetype.POINTER;
	    default:
		return basetype;
	    }
	}
	return basetype;
    }
    
    public static Basetype translate(Basetype basetype) {
	return translate(basetype,0);
    }
    
    public static Exectype make(Basetype basetype) {
	return make(basetype,0);
    }
    
    public static Exectype make(Basetype basetype,int arrayDepth) {
	return make(translate(basetype,arrayDepth),null,arrayDepth);
    }
    
    public static Exectype make(VisibleClass clazz) {
	return Type.make(clazz).asExectype();
    }
    
    public static Exectype make(VisibleClass clazz,int arrayDepth) {
	return Type.make(clazz,arrayDepth).asExectype();
    }
    
    public static Exectype make(Type t) {
        synchronized (t) {
            if (t.cachedExectype==null) {
                t.cachedExectype=
                    make(translate(t.basetype,t.arrayDepth),
                         t.clazz!=null?t.clazz.exectypeLub():null,
                         t.arrayDepth);
                assert t.cachedExectype!=null;
            }
            return t.cachedExectype;
        }
    }
    
    private static Exectype lubImpl(Exectype a,Exectype b) {
	try {
	    if (b.isSubtypeOf(a)) {
		return a;
	    }
	    for (Exectype cur=b;cur!=null;cur=cur.supertype()) {
		if (a.isSubtypeOf(cur)) {
		    return cur;
		}
	    }
	    throw new LubFailed("Failed to lub "+a+" and "+b);
	} catch (ResolutionFailed e) {
	    throw new ResolutionFailed(
                e.getClazz(),
                e.getClazz().getResolutionID(),
		"Cannot determine a least upper bound involving unresolved types: "+
		a.jniName()+" and "+b.jniName());
	}
    }
    
    // this is an approximate lub.  it assumes a type hierarchy (which we don't have;
    // we have a lattice instead).  this works for us because we allow calls and
    // field acesses that are unsound (for example we can get field foo.x on type
    // Object), and only take advantage of more refined types for devirtualization.
    public static Exectype lub(Exectype a,Exectype b) {
	boolean assertionsEnabled=false;
	assert assertionsEnabled=true;
	if (!assertionsEnabled) {
	    return lubImpl(a,b);
	} else {
	    Exectype result=null;
	    try {
		result=lubImpl(a,b);
	    } catch (ResolutionFailed e) {
		try {
		    result=lubImpl(b,a);
		    assert false
			: "While lubbing "+a+" and "+b+", got "+e+" and "+result;
		} catch (ResolutionFailed e2) {
		    throw e;
		}
	    }
	    assert result!=null;
	    try {
		assert result==lubImpl(b,a)
		    :"While lubbing "+a+" and "+b+", got "+result+" and "+lubImpl(b,a);
	    } catch (ResolutionFailed e) {
		assert false
		    : "While lubbing "+a+" and "+b+", got "+result+" and "+e;
	    }
	    return result;
	}

    }
    
    public Exectype lub(Exectype other) {
	return lub(this,other);
    }
    
    public Type asType() { return Type.make(this); }
    
    public Arg makeZero() {
        return effectiveBasetype().makeZero();
    }
    
    public Arg makeConst(int value) {
        return effectiveBasetype().makeConst(value);
    }
    
    public static Exectype VOID;
    static Exectype BOOLEAN;
    static Exectype BYTE;
    static Exectype CHAR;
    static Exectype SHORT;
    public static Exectype INT;
    public static Exectype LONG;
    public static Exectype FLOAT;
    public static Exectype DOUBLE;
    
    public static Exectype NULL;
    public static Exectype POINTER;
    static Exectype VM_FCPTR;
    public static Exectype TOP;
    public static Exectype BOTTOM;
    public static Exectype NIL;

    private static Exectype initBasetype(Basetype basetype) {
        Exectype result=construct(basetype);
        basetype.asExectypeInternal=result;
        return result;
    }
    
    private static void linkBasetype(Basetype basetype,
                                     Exectype exectype) {
        basetype.asExectype=exectype;
    }
    
    static {
	hashConsMap = new HashMap< TypeKey, Exectype >();

	VOID    = initBasetype(Basetype.VOID);
	BOOLEAN = initBasetype(Basetype.BOOLEAN);
	BYTE    = initBasetype(Basetype.BYTE);
	CHAR    = initBasetype(Basetype.CHAR);
	SHORT   = initBasetype(Basetype.SHORT);
	INT     = initBasetype(Basetype.INT);
	LONG    = initBasetype(Basetype.LONG);
	FLOAT   = initBasetype(Basetype.FLOAT);
	DOUBLE  = initBasetype(Basetype.DOUBLE);
	
	NULL = initBasetype(Basetype.NULL);
	TOP = initBasetype(Basetype.TOP);
	BOTTOM = initBasetype(Basetype.BOTTOM);
        NIL = initBasetype(Basetype.NIL);
	
	POINTER = initBasetype(Basetype.POINTER);
	VM_FCPTR = initBasetype(Basetype.VM_FCPTR);
        
        linkBasetype(Basetype.VOID,      VOID);
        linkBasetype(Basetype.BOOLEAN,   INT);
        linkBasetype(Basetype.BYTE,      INT);
        linkBasetype(Basetype.CHAR,      INT);
        linkBasetype(Basetype.SHORT,     INT);
        linkBasetype(Basetype.INT,       INT);
        linkBasetype(Basetype.LONG,      LONG);
        linkBasetype(Basetype.FLOAT,     FLOAT);
        linkBasetype(Basetype.DOUBLE,    DOUBLE);
        linkBasetype(Basetype.NULL,      NULL);
        linkBasetype(Basetype.TOP,       TOP);
        linkBasetype(Basetype.BOTTOM,    BOTTOM);
        linkBasetype(Basetype.NIL,       NIL);
        linkBasetype(Basetype.POINTER,   POINTER);
        linkBasetype(Basetype.VM_FCPTR,  POINTER);
    }
    
    public static Exectype[] EMPTY=new Exectype[0];
    
}

