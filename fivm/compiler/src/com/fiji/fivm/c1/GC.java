/*
 * GC.java
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

import com.fiji.fivm.Constants;

public enum GC {
    NOGC {
	public int asInt() { return Constants.NOGC; }
	public boolean needsAccurateStackScan() { return false; }
	public int objSpace() { return 0; }
	public int stackAllocSpace() { return 1; }
	public boolean needsCMStoreBarrier() { return false; }
        public boolean hasCMHeader() { return false; }
    },
	
    CMRGC {
	public int asInt() { return Constants.CMRGC; }
	public boolean needsAccurateStackScan() { return true; }
	public int objSpace() { return 0; }
	public int stackAllocSpace() { return 1; }
	public boolean needsCMStoreBarrier() { return true; }
        public boolean hasCMHeader() { return true; }
    },
    
    // FIXME: how to support this?  we should allow stack allocation in the libraries
    // but disallow it elsewhere...  or something.  it seems like it might be easier
    // to just do a global analysis to kill off scope checks.
    CMRGC_PURE {
	public int asInt() { return Constants.CMRGC_PURE; }
	public boolean needsAccurateStackScan() { return true; }
	public int objSpace() { return 0; }
	public int stackAllocSpace() { return 1; }
	public boolean needsCMStoreBarrier() { return true; }
        public boolean hasCMHeader() { return true; }
    },
        
    HFGC {
	public int asInt() { return Constants.CMRGC; }
	public boolean needsAccurateStackScan() { return true; }
	public int objSpace() { return 0; }
	public int stackAllocSpace() { return 1; }
	public boolean needsCMStoreBarrier() { return true; }
        public boolean hasCMHeader() { return true; }
        // lots of FIXMEs here...
    };
    
    public abstract int asInt();
    public abstract boolean needsAccurateStackScan();
    public abstract boolean needsCMStoreBarrier();
    public abstract boolean hasCMHeader();
    public abstract int objSpace();
    public abstract int stackAllocSpace();
    public int headerSize() { return Global.pointerSize; }
    
    public boolean forceSafeArrayCopyFor(Type target,Type source) {
	// FIXME ... for stack alloc
	return false;
    }
}

