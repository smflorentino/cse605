/*
 * ArrayVarMap.java
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

public class ArrayVarMap< T > extends VarMap< T > {
    Code code;
    Object[] array;
    
    public ArrayVarMap(Code code) {
	this.code=code;
	array=new Object[code.vars().size()];
    }
    
    public void put(Var v,T value) {
	array[v.id]=value;
    }
    
    @SuppressWarnings("unchecked")
    public T get(Var v) {
	return (T)array[v.id];
    }
    
    public boolean containsKey(Var v) {
	return array[v.id]!=null;
    }
    
    public Iterable< Var > keys() {
	VarSet result=new VarSet(code);
	for (Var v : code.vars()) {
	    if (array[v.id]!=null) {
		result.add(v);
	    }
	}
	return result;
    }
    
    @SuppressWarnings("unchecked")
    public Iterable< T > values() {
	ArrayList< T > result=new ArrayList< T >();
	for (Object value : array) {
	    if (value!=null) {
		result.add((T)value);
	    }
	}
	return result;
    }
    
    public Iterable< Map.Entry< Var, T > > entries() {
	ArrayList< Map.Entry< Var, T > > result=new ArrayList< Map.Entry< Var, T > >();
	for (Var v_ : code.vars()) {
	    final Var v=v_;
	    if (array[v.id]!=null) {
		result.add(new Map.Entry< Var, T >(){
			public Var getKey() { return v; }
			@SuppressWarnings("unchecked")
			public T getValue() { return (T)array[v.id]; }
			@SuppressWarnings("unchecked")
			public T setValue(T value) {
			    T result=(T)array[v.id];
			    array[v.id]=value;
			    return result;
			}
		    });
	    }
	}
	return result;
    }
}

