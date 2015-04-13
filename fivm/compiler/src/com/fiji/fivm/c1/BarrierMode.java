/*
 * BarrierMode.java
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

import static com.fiji.fivm.Constants.*;
import com.fiji.fivm.*;
import java.nio.ByteBuffer;

public class BarrierMode {
    int mask;
    
    public BarrierMode() {
	mask = 0;
    }
    
    BarrierMode(int mask) {
	this.mask=mask;
    }
    
    public static final BarrierMode CLEAR=new BarrierMode();
    public static final BarrierMode FULL;
    
    static {
        if (Settings.HAVE_SCOPE_CHECKS) {
            FULL=CLEAR.withCMStoreBarrier().withScopeCheck();
        } else {
            FULL=CLEAR.withCMStoreBarrier();
        }
    }
    
    public static BarrierMode fullFor(HeapAccessInst hai) {
	if (StoreSourceCalc.isBarriered(hai)) {
	    return FULL;
	} else {
	    return CLEAR;
	}
    }
    
    public BarrierMode withCMStoreBarrier() {
	return new BarrierMode(mask|BR_CM_STORE);
    }
    
    public BarrierMode withoutCMStoreBarrier() {
	return new BarrierMode(mask&~BR_CM_STORE);
    }
    
    public boolean hasCMStoreBarrier() {
	return (mask&BR_CM_STORE)!=0;
    }
    
    public BarrierMode withScopeCheck() {
	return new BarrierMode(mask|BR_SCOPE_CHECK);
    }
    
    public BarrierMode withoutScopeCheck() {
	return new BarrierMode(mask&~BR_SCOPE_CHECK);
    }
    
    public boolean hasScopeCheck() {
	return (mask&BR_SCOPE_CHECK)!=0;
    }
    
    public BarrierMode withArrayBoundsCheck() {
        return new BarrierMode(mask|BR_ARRAY_BOUNDS);
    }
    
    public BarrierMode withoutArrayBoundsCheck() {
        return new BarrierMode(mask&~BR_ARRAY_BOUNDS);
    }
    
    public boolean hasArrayBoundsCheck() {
        return (mask&BR_ARRAY_BOUNDS)!=0;
    }
    
    public boolean hasChecks() {
        return hasScopeCheck() || hasArrayBoundsCheck();
    }
    
    public boolean hasAnything() {
	return hasCMStoreBarrier() || hasScopeCheck() || hasArrayBoundsCheck();
    }
    
    public boolean isClear() {
        return !hasAnything();
    }
    
    public int asInt() { return mask; }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof BarrierMode)) return false;
	BarrierMode other=(BarrierMode)other_;
	return mask==other.mask;
    }
    
    public int hashCode() {
	return mask;
    }
    
    public String toString() {
        String result="";
        if (hasAnything()) {
            if (hasArrayBoundsCheck()) {
                result+=" ARRAY_BOUNDS_CHECK";
            }
            if (hasScopeCheck()) {
                result+=" SCOPE_CHECK";
            }
            if (hasCMStoreBarrier()) {
                result+=" CM_STORE";
            }
        } else {
            result+=" NO_BARRIERS";
        }
        return "("+result.substring(1)+")";
    }
    
    int getNioSize() {
        return 4;
    }
    
    void writeTo(ByteBuffer buffer) {
        buffer.putInt(mask);
    }
    
    static BarrierMode readFrom(ByteBuffer buffer) {
        return new BarrierMode(buffer.getInt());
    }
}



