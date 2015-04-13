/*
 * NioCoding.java
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

public final class NioCoding< T > {

    private ArrayList< T > list = new ArrayList< T >();
    private Map< T, Integer > map;
    
    public NioCoding(MapMode mode) {
        if (mode==MapMode.IDENTITY_KEYS) {
            map=new IdentityHashMap< T, Integer >();
        } else {
            map=new HashMap< T, Integer >();
        }
    }
    
    public int codeFor(T thing) {
        if (thing==null) {
            return -1;
        } else {
            Integer result=map.get(thing);
            if (result==null) {
                return add(thing);
            } else {
                return result;
            }
        }
    }
    
    public int add(T thing) {
        assert thing!=null;
        assert !map.containsKey(thing);
        int newCode=list.size();
        list.add(thing);
        map.put(thing,newCode);
        return newCode;
    }
    
    public T forCode(int code) {
        if (code==-1) {
            return null;
        } else {
            T result=list.get(code);
            assert result!=null;
            return result;
        }
    }
    
    public int size() {
        assert map.size()==list.size();
        return map.size();
    }
    
    public Iterable< T > things() {
        return list;
    }
}

