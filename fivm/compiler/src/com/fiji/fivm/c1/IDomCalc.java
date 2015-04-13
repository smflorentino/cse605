/*
 * IDomCalc.java
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

public class IDomCalc {
    Code c;
    TwoWayMap< Header, Header > idoms=new TwoWayMap< Header, Header >();
    
    IDomCalc(Code c) {
	this.c=c;
	DominatorCalc dom=c.getDominators();
	
	if (Global.verbosity>=3) {
	    Global.log.println("Dealing with "+c.headers().size()+" blocks in "+c);
	}
	
	Header[] headersByOrder=c.getHeadersByOrder();
	BitSet[] bitIdoms=new BitSet[headersByOrder.length];
	BitSet[] bitDoms=new BitSet[headersByOrder.length];
	
	for (Header h : c.headers()) {
	    BitSet curSet=new BitSet();
	    BitSet domSet=dom.bitDominators(h);
	    curSet.or(domSet);
	    curSet.clear(h.order);
	    bitIdoms[h.order]=curSet;
	    bitDoms[h.order]=domSet;
	}
	
	for (Header h : c.depthFirstHeaders()) {
	    BitSet curSet=bitIdoms[h.order];
	    for (int i=curSet.nextSetBit(0);i>=0;i=curSet.nextSetBit(i+1)) {
		BitSet subSet=bitDoms[i]; /* NOTE: this differs from the Muchnick
					     algorithm.  either the Muchnick algorithm
					     is wrong, or I misunderstood it somehow
					     (for example in regards to the order in
					     which I'm supposed to visit blocks).
					     I tend to believe the former is true. */
		for (int j=subSet.nextSetBit(0);j>=0;j=subSet.nextSetBit(j+1)) {
		    if (i!=j) {
			curSet.clear(j);
		    }
		}
	    }
	}
	
	for (Header h : c.headers()) {
	    BitSet curSet=bitIdoms[h.order];
	    for (int i=curSet.nextSetBit(0);i>=0;i=curSet.nextSetBit(i+1)) {
		idoms.put(h,headersByOrder[i]);
	    }
	}
	
	if (Global.verbosity>=5) {
	    Global.log.println("IDom dump:");
	    for (Header h : c.headers()) {
		Global.log.println("   "+h+": "+iDominates(h));
	    }
	}
    }
    
    public HashSet< Header > iDominators(Header h) {
	return idoms.valuesForKey(h);
    }
    
    public HashSet< Header > iDominates(Header h) {
	return idoms.keysForValue(h);
    }
    
    public Header[] postOrder() {
	int max=c.headers().size();
	Header[] result=new Header[max];
	Header[] headers=new Header[max];
	boolean[] handled=new boolean[max];
	int height=1;
	int cnt=0;
	headers[0]=c.root();
	handled[0]=false;
	while (height>0) {
	    if (handled[height-1]) {
		result[cnt++]=headers[--height];
	    } else {
		handled[height-1]=true;
		for (Header h2 : iDominates(headers[height-1])) {
		    headers[height]=h2;
		    handled[height]=false;
		    ++height;
		}
	    }
	}
	assert cnt==max : Util.dump(result);
	return result;
    }
}


