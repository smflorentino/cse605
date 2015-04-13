/*
 * SequenceWindow.java
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

public class SequenceWindow extends Addable implements Cloneable {
    long[] window;
    int idx;
    boolean wrapped;
    
    public SequenceWindow(int maxWindowSize) {
        window=new long[maxWindowSize];
    }
    
    public void add(long value) {
        window[idx]=value;
        idx++;
        if (idx==window.length) {
            idx=0;
            wrapped=true;
        }
    }
    
    public Object clone() {
        return copy();
    }
    
    public SequenceWindow copy() {
        SequenceWindow result=new SequenceWindow(window.length);
        result.copyFrom(this);
        return result;
    }
    
    public void copyFrom(SequenceWindow other) {
        if (window.length!=other.window.length) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(other.window,0,
                         this.window,0,
                         window.length);
        this.idx=other.idx;
        this.wrapped=other.wrapped;
    }
    
    public void copyTo(SequenceWindow other) {
        other.copyFrom(this);
    }
    
    public int maxWindowSize() {
        return window.length;
    }
    
    public boolean isEmpty() {
        return windowSize()==0;
    }
    
    public int windowSize() {
        if (wrapped) {
            return window.length;
        } else {
            return idx;
        }
    }
    
    public long get(int idx) {
        if (idx>=windowSize()) {
            throw new IndexOutOfBoundsException();
        }
        if (wrapped) {
            return window[(this.idx+idx)%window.length];
        } else {
            return window[idx];
        }
    }
    
    public long[] getCurrentWindow() {
        long[] result=new long[windowSize()];
        for (int i=0;i<windowSize();++i) {
            result[i]=get(i);
        }
        return result;
    }
    
    public String toString() {
        return Arrays.toString(getCurrentWindow());
    }
}

