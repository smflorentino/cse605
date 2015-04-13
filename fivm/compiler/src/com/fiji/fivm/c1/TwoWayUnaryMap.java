/*
 * TwoWayUnaryMap.java
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

public class TwoWayUnaryMap< T, U > {
    HashMap< T, U > forward;
    HashMap< U, T > backward;
    TwoWayUnaryMap< U, T > reverse;
    
    public TwoWayUnaryMap() {
        forward=new HashMap< T, U >();
        backward=new HashMap< U, T >();
        reverse=new TwoWayUnaryMap< U, T >(backward,forward,this);
    }
    
    private TwoWayUnaryMap(HashMap< T, U > forward,
                           HashMap< U, T > backward,
                           TwoWayUnaryMap< U, T > reverse) {
        this.forward=forward;
        this.backward=backward;
        this.reverse=reverse;
    }
    
    public TwoWayUnaryMap< U, T > reverse() {
        return reverse;
    }
    
    public Set< T > keySet() {
	return forward.keySet();
    }
    
    public Set< U > valueSet() {
	return backward.keySet();
    }

    public int numKeys() {
	return keySet().size();
    }
    
    public int numValues() {
	return valueSet().size();
    }

    public boolean isEmpty() {
	if (forward.isEmpty()) {
	    assert backward.isEmpty();
	    return true;
	} else {
	    assert !backward.isEmpty();
	    return false;
	}
    }
    
    public U valueForKey(Object key) {
        return forward.get(key);
    }
    
    public T keyForValue(Object value) {
        return backward.get(value);
    }
    
    public boolean containsKey(Object key) {
        return forward.containsKey(key);
    }
    
    public boolean containsValue(Object value) {
        return backward.containsKey(value);
    }
    
    public boolean killKey(T key) {
        U value=forward.remove(key);
        if (value!=null) {
            backward.remove(value);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean killValue(U value) {
        return reverse.killKey(value);
    }
    
    public void put(T key,U value) {
        assert key!=null;
        assert value!=null;
        // remove previous associations
        killKey(key);
        killValue(value);
        // establish new associations
        forward.put(key,value);
        backward.put(value,key);
    }
    
    public void putAll(TwoWayUnaryMap< T, U > other) {
        for (T key : other.keySet()) {
            put(key,other.valueForKey(key));
        }
    }
    
    public TwoWayUnaryMap< T, U > copy() {
        TwoWayUnaryMap< T, U > result=new TwoWayUnaryMap< T, U >();
        result.forward.putAll(forward);
        result.backward.putAll(backward);
        return result;
    }
    
    public int hashCode() {
        return forward.hashCode();
    }
    
    public boolean equals(Object other_) {
        if (this==other_) return true;
        if (!(other_ instanceof TwoWayUnaryMap< ?, ? >)) return false;
        TwoWayUnaryMap< ?, ? > other=(TwoWayUnaryMap< ?, ? >)other_;
        return forward.equals(other.forward);
    }
    
    public String toString() {
        return forward.toString();
    }
}

