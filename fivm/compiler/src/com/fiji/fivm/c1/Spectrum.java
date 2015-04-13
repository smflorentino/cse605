/*
 * Spectrum.java
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

public class Spectrum< T > {
    HashMap< T, MutableInt > map;
    
    public Spectrum() {
	map=new HashMap< T, MutableInt >();
    }
    
    public void add(T key, int val) {
        if (val!=0) {
            MutableInt cnt=map.get(key);
            if (cnt==null) {
                map.put(key,new MutableInt(val));
            } else if (cnt==INF) {
                // do nothing
            } else {
                cnt.value+=val;
                if (cnt.value==0) {
                    map.remove(key);
                }
            }
        }
    }
    
    public void add(T key) {
        MutableInt cnt=map.get(key);
        if (cnt==null) {
            map.put(key,new MutableInt(1));
        } else if (cnt==INF) {
            // do nothing
        } else {
            cnt.value++;
        }
    }
    
    public void addInf(T key) {
	map.put(key,INF);
    }
    
    public void setInf(T key) {
        map.put(key,INF);
    }
    
    public boolean remove(T key) {
	MutableInt cnt=map.get(key);
	if (cnt==INF || cnt==null) {
	    return false;
	} else if (--cnt.value==0) {
	    map.remove(key);
	}
	return true;
    }
    
    public void set(T key,int value) {
        if (value==0) {
            map.remove(key);
        } else {
            MutableInt cnt=map.get(key);
            if (cnt==null || cnt==INF) {
                map.put(key,new MutableInt(value));
            } else {
                cnt.value=value;
            }
        }
    }
    
    public boolean isInf(T key) {
	return map.get(key)==INF;
    }
    
    public int count(T key) {
	MutableInt cnt=map.get(key);
	if (cnt==null) {
	    return 0;
	} else {
	    return cnt.value;
	}
    }
    
    public Set< T > keySet() {
	return map.keySet();
    }
    
    static MutableInt INF=new MutableInt(-1);
}

