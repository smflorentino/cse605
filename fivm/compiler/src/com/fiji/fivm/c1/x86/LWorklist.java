/*
 * LWorklist.java
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

package com.fiji.fivm.c1.x86;

import com.fiji.util.MyStack;

public class LWorklist extends MyStack< LHeader > {
    public void push(LHeader h) {
	if (h.onlist==this) {
	    // nothing to do
	} else {
	    assert h.onlist==null;
	    h.onlist=this;
	    super.push(h);
	}
    }
    
    public LHeader pop() {
	LHeader h=super.pop();
	assert h.onlist==this;
	h.onlist=null;
	return h;
    }
    
    public void removeAt(int i) {
	LHeader h=get(i);
	assert h.onlist==this;
	h.onlist=null;
	super.removeAt(i);
    }
    
    public boolean removeFirst(LHeader h) {
	if (super.removeFirst(h)) {
	    assert h.onlist==this;
	    h.onlist=null;
	    return true;
	} else {
	    return false;
	}
    }
    
    public void clear() {
	for (LHeader h : finiteView()) {
	    assert h.onlist==this;
	    h.onlist=null;
	}
	super.clear();
    }
    
    public void set(MyStack< LHeader > other) {
	throw new Error("not supported");
    }
    
    public void shift(int amount) {
	for (int i=0;i<amount;++i) {
	    LHeader h=get(i);
	    assert h.onlist==this;
	    h.onlist=null;
	}
	super.shift(amount);
    }
}


