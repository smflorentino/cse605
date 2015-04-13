/*
 * TwoWayMap.java
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

public class TwoWayMap< T, U > {
    
    HashMap< T, HashSet< U > > forward;
    HashMap< U, HashSet< T > > backward;
    TwoWayMap< U, T > reverse;
    
    public TwoWayMap() {
	forward=new HashMap< T, HashSet< U > >();
	backward=new HashMap< U, HashSet< T > >();
	reverse=new TwoWayMap< U, T >(backward,forward,this);
    }
    
    TwoWayMap(HashMap< T, HashSet< U > > forward,
	      HashMap< U, HashSet< T > > backward,
	      TwoWayMap< U, T > reverse) {
	this.forward=forward;
	this.backward=backward;
	this.reverse=reverse;
    }
    
    public TwoWayMap< U, T > reverse() {
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
    
    // the set returned is immutable!
    @SuppressWarnings("unchecked")
    public HashSet< U > valuesForKey(Object key) {
	HashSet< U > result=forward.get(key);
	if (result==null) {
	    return EMPTY_SET;
	} else {
	    return result;
	}
    }
    
    public U valueForKey(Object key) {
	HashSet< U > result=forward.get(key);
	if (result==null) {
	    return null;
	} else {
	    assert result.size()==1;
	    return result.iterator().next();
	}
    }
    
    public int numValuesForKey(Object key) {
	HashSet< U > result=forward.get(key);
	if (result==null) {
	    return 0;
	} else {
	    return result.size();
	}
    }
    
    public boolean containsKey(Object key) {
	return numValuesForKey(key)>0;
    }
    
    public HashSet< T > keysForValue(Object value) {
	return reverse.valuesForKey(value);
    }
    
    public T keyForValue(Object value) {
	return reverse.valueForKey(value);
    }
    
    public int numKeysForValue(Object value) {
	return reverse.numValuesForKey(value);
    }
    
    public boolean containsValue(Object value) {
	return numKeysForValue(value)>0;
    }
    
    public boolean killKey(T key) {
	HashSet< U > values=forward.get(key);
	if (values!=null) {
	    for (U value : values) {
		HashSet< T > keys=backward.get(value);
		boolean result=keys.remove(key);
		assert result;
		if (keys.isEmpty()) {
		    backward.remove(value);
		}
	    }
	    forward.remove(key);
	    return true;
	} else {
	    return false;
	}
    }
    
    public boolean killValue(U value) {
	return reverse.killKey(value);
    }
    
    boolean killImpl(T key,U value) {
	HashSet< U > values=forward.get(key);
	if (values!=null) {
	    if (values.remove(value)) {
		if (values.isEmpty()) {
		    forward.remove(key);
		}
		return true;
	    }
	}
	return false;
    }
    
    public boolean kill(T key,U value) {
	if (killImpl(key,value)) {
	    boolean result=reverse.killImpl(value,key);
	    assert result;
	    return true;
	} else {
	    assert !reverse.killImpl(value,key);
	    return false;
	}
    }
    
    boolean putImpl(T key,U value) {
	HashSet< U > values=forward.get(key);
	if (values==null) {
	    forward.put(key,values=new HashSet< U >());
	}
	return values.add(value);
    }
    
    public boolean put(T key,U value) {
	if (putImpl(key,value)) {
	    boolean result=reverse.putImpl(value,key);
	    assert result;
	    return true;
	} else {
	    assert !reverse.putImpl(value,key);
	    return false;
	}
    }
    
    public boolean putMulti(T key,Iterable< U > values) {
	boolean changed=false;
	for (U value : values) {
	    changed|=put(key,value);
	}
	return changed;
    }
    
    public boolean killMultiKeys() {
	boolean result=false;
	ArrayList< T > keySnapshot=new ArrayList< T >(keySet());
	for (T key : keySnapshot) {
	    if (valuesForKey(key).size()>1) {
		killKey(key);
		result=true;
	    }
	}
	return result;
    }
    
    public void putAll(TwoWayMap< T, U > other) {
	for (T key : other.keySet()) {
	    putMulti(key,other.valuesForKey(key));
	}
    }
    
    void rebuildImpl() {
	for (T key : new ArrayList< T >(forward.keySet())) {
	    forward.put(key,Util.copy(forward.get(key)));
	}
    }
    
    public void rebuild() {
	rebuildImpl();
	reverse.rebuildImpl();
    }
    
    public TwoWayMap< T, U > copy() {
	TwoWayMap< T, U> result=new TwoWayMap< T, U >();
	result.putAll(this);
	return result;
    }
    
    public int hashCode() {
	return forward.hashCode();
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof TwoWayMap<?,?>)) return false;
	TwoWayMap<?,?> other=(TwoWayMap<?,?>)other_;
	return forward.equals(other.forward);
    }
    
    public String toString() {
	return forward.toString();
    }
    
    @SuppressWarnings("unchecked")
    static HashSet EMPTY_SET=new HashSet();
    
}


