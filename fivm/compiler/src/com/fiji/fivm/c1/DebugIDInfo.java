/*
 * DebugIDInfo.java
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
import java.nio.*;

public class DebugIDInfo {
    DebugInfo di;
    BitSet refsLive;
    
    public DebugIDInfo(DebugInfo di,
		       BitSet refsLive) {
	this.di=di;
	this.refsLive=refsLive;
    }
    
    public DebugIDInfo(DebugInfo di,
		       Code c,
		       Operation o) {
	this.di=di;
	refsLive=new BitSet();
	RefAlloc ra=c.getRefAlloc();
	for (Var v : c.getRefsLiveAtSafe().forSafe(o)) {
	    refsLive.set(ra.varAssignment(v));
	}
    }
    
    public DebugInfo di() { return di; }
    public BitSet refsLive() { return refsLive; }
    
    public int hashCode() {
	return di.hashCode()+refsLive.hashCode();
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof DebugIDInfo)) return false;
	DebugIDInfo other=(DebugIDInfo)other_;
	return di.equals(other.di)
	    && refsLive.equals(other.refsLive);
    }
    
    public String toString() {
	return "DebugIDInfo[di = "+di+", refsLive = "+refsLive+"]";
    }
    
    int getNioSize() {
        return 4+4+refsLive.cardinality()*4;
    }
    
    void writeTo(NioContext ctx,
                 ByteBuffer buffer) {
        buffer.putInt(ctx.diCodes.codeFor(di));
        buffer.putInt(refsLive.cardinality());
        for (int i=refsLive.nextSetBit(0);i>=0;i=refsLive.nextSetBit(i+1)) {
            buffer.putInt(i);
        }
    }
    
    static DebugIDInfo readFrom(NioContext ctx,
                                ByteBuffer buffer) {
        DebugInfo di=ctx.diCodes.forCode(buffer.getInt());
        int cardinality=buffer.getInt();
        BitSet refsLive=new BitSet();
        while (cardinality-->0) {
            refsLive.set(buffer.getInt());
        }
        return new DebugIDInfo(di,refsLive);
    }
}


