/*
 * CompactArrayList.java
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

package com.fiji.fivm;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CompactArrayList< T > implements Iterable< T > {
    private static Object[] EMPTY=new Object[0];
    
    Object[] stuff;
    
    public CompactArrayList() {
        stuff=EMPTY;
    }
    
    public void add(T value) {
        Object[] newStuff=new Object[stuff.length+1];
        System.arraycopy(stuff,0,
                         newStuff,0,
                         stuff.length);
        newStuff[stuff.length]=value;
        stuff=newStuff;
    }
    
    @SuppressWarnings({"unchecked"})
    public T get(int idx) {
        return (T)stuff[idx];
    }
    
    public void set(int idx,T value) {
        stuff[idx]=value;
    }
    
    public int size() {
        return stuff.length;
    }
    
    public Iterator< T > iterator() {
        return new Iterator< T >(){
            int idx=0;
            public boolean hasNext() {
                return idx<stuff.length;
            }
            @SuppressWarnings({"unchecked"})
            public T next() {
                try {
                    return (T)stuff[idx++];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append("[");
        for (int i=0;i<stuff.length;++i) {
            if (i!=0) {
                buf.append(", ");
            }
            buf.append(stuff[i]);
        }
        buf.append("]");
        return buf.toString();
    }
}

