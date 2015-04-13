/*
 * MyStack.java
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

package com.fiji.util;

import java.util.*;

public class MyStack< T > {
    private static Object[] EMPTY=new Object[0];
    
    Object[] elements;
    int height;
    
    public MyStack() {
	elements=EMPTY;
	height=0;
    }
    
    public void push(T value) {
	if (height==elements.length) {
	    Object[] newElements=new Object[(elements.length+1)*2];
	    System.arraycopy(elements,0,
			     newElements,0,
			     height);
	    elements=newElements;
	}
	elements[height++]=value;
    }
    
    @SuppressWarnings("unchecked")
    public T pop() {
	if (height==0) {
	    throw new NoSuchElementException();
	}
	--height;
	T result=(T)elements[height];
	elements[height]=null;
	return result;
    }
    
    @SuppressWarnings("unchecked")
    public T peek() {
	return (T)elements[height-1];
    }
    
    public int height() { return height; }
    
    @SuppressWarnings("unchecked")
    public T get(int i) {
	if (i>=height) {
	    throw new ArrayIndexOutOfBoundsException(
		""+i+" is not less than "+height);
	}
	return (T)elements[i];
    }
    
    @SuppressWarnings("unchecked")
    public T getFromTop(int i) {
	if (i>=height) {
	    throw new ArrayIndexOutOfBoundsException(
		""+i+" is not less than "+height);
	}
	return (T)elements[height-i-1];
    }
    
    public void removeAt(int i) {
	if (i>=height) {
	    throw new ArrayIndexOutOfBoundsException(
		""+i+" is not less than "+height);
	}
	for (int j=i;j<height-1;++j) {
	    elements[j]=elements[j+1];
	}
	elements[--height]=null;
    }
    
    public boolean removeFirst(T value) {
	for (int i=0;i<height;++i) {
	    if (elements[i].equals(value)) {
		removeAt(i);
		return true;
	    }
	}
	return false;
    }
    
    public boolean contains(T value) {
	for (int i=0;i<height;++i) {
	    if (elements[i].equals(value)) return true;
	}
	return false;
    }
    
    public boolean empty() {
	return height==0;
    }
    
    public void clear() {
	for (int i=0;i<height;++i) {
	    elements[i]=null;
	}
	height=0;
    }
    
    public void sort(Comparator<? super T> c) {
        Arrays.sort((T[])elements,0,height,c);
    }
    
    public MyStack< T > copy() {
	MyStack< T > result=new MyStack< T >();
	result.elements=new Object[height];
	System.arraycopy(elements,0,
			 result.elements,0,
			 height);
	result.height=height;
	return result;
    }
    
    public void set(MyStack< T > other) {
	if (other.height>elements.length) {
	    elements=new Object[other.height];
	}
	System.arraycopy(other.elements,0,
			 elements,0,
			 other.height);
	height=other.height;
    }
    
    public void shift(int amount) {
	for (int i=0;i<height-amount;++i) {
	    elements[i]=elements[i+amount];
	}
	for (int i=height-amount;i<height;++i) {
	    elements[i]=null;
	}
	height-=amount;
    }
    
    @SuppressWarnings("unchecked")
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof MyStack)) return false;
	MyStack< T > other=(MyStack< T >)other_;
	if (height!=other.height) return false;
	for (int i=0;i<height;++i) {
	    if (!elements[i].equals(other.elements[i])) return false;
	}
	return true;
    }
    
    public Iterable< T > infiniteView() {
	return new Iterable< T >() {
	    public Iterator< T > iterator() {
		return new Iterator< T >() {
		    public boolean hasNext() {
			return !empty();
		    }
		    public T next() {
			return pop();
		    }
		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    public Iterable< T > finiteView() {
	return new Iterable< T >() {
	    public Iterator< T > iterator() {
		return new Iterator< T >() {
		    int i=0;
		    public boolean hasNext() {
			return i<height;
		    }
		    @SuppressWarnings("unchecked")
                    public T next() {
			if (i>=height) {
			    throw new NoSuchElementException();
			}
			return (T)elements[i++];
		    }
		    public void remove() {
			if (i>=height) {
			    throw new IllegalStateException();
			}
			removeAt(--i);
		    }
		};
	    }
	};
    }
    
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append("Stack[height = ");
        buf.append(height);
        buf.append(": ");
        for (int i=0;i<height;++i) {
            if (i!=0) {
                buf.append(", ");
            }
            buf.append(elements[i]);
        }
        buf.append("]");
        return buf.toString();
    }

}

