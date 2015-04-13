/*
 * DominatorCalc.java
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

public class DominatorCalc {
    Code c;
    HashMap< Header, BitSet > bitDomin=new HashMap< Header, BitSet >();
    HashMap< Header, ArrayList< Header > > dominators=null;
    HashMap< Header, ArrayList< Header > > invDominators=null;
    
    DominatorCalc(Code c) {
	this.c=c;
	PredsCalc preds=c.getPreds();
	
	c.recomputeOrder();
	
	BitSet all=new BitSet();
	for (Header h : c.headers()) {
	    all.set(h.order);
	}
	
        BitSet roots=new BitSet();
        for (Header h : roots(preds,c)) {
            BitSet rootSet=new BitSet();
            rootSet.set(h.order);
            bitDomin.put(h,rootSet);
            roots.set(h.order);
        }
	for (Header h : c.headers()) {
	    if (!roots.get(h.order)) {
		BitSet curSet=new BitSet();
		curSet.or(all);
		bitDomin.put(h,curSet);
	    }
	}
	
	boolean changed=true;
	while (changed) {
	    changed=false;
	    for (Header h : c.headers()) {
		BitSet curSet=bitDomin.get(h);
		int oldCard=curSet.cardinality();
		for (Header h2 : predecessors(preds,h)) {
		    curSet.and(bitDomin.get(h2));
		}
		curSet.set(h.order);
		changed|=(curSet.cardinality()!=oldCard);
	    }
            if (changed) {
                changed=false;
                for (Header h : c.headers2Reverse()) {
                    BitSet curSet=bitDomin.get(h);
                    int oldCard=curSet.cardinality();
                    for (Header h2 : predecessors(preds,h)) {
                        curSet.and(bitDomin.get(h2));
                    }
                    curSet.set(h.order);
                    changed|=(curSet.cardinality()!=oldCard);
                }
            }
	}
    }
    
    protected Iterable< Header > roots(PredsCalc pc,Code c) {
        return Util.singleIterable(c.root());
    }
    
    protected Iterable< Header > predecessors(PredsCalc pc,Header h) {
        return pc.allPredecessors(h);
    }
    
    public BitSet bitDominators(Header h) {
	return bitDomin.get(h);
    }
    
    public void makeDomMap() {
	if (dominators==null) {
	    dominators=new HashMap< Header, ArrayList< Header > >();
            invDominators=new HashMap< Header, ArrayList< Header > >();
            
            for (Header h : c.headers()) {
                dominators.put(h,new ArrayList< Header >());
                invDominators.put(h,new ArrayList< Header >());
            }
            
	    Header[] headersByOrder=c.getHeadersByOrder();
	    
	    for (Header h : c.headers()) {
		BitSet curSet=bitDomin.get(h);
		for (int i=curSet.nextSetBit(0);i>=0;i=curSet.nextSetBit(i+1)) {
                    dominators.get(h).add(headersByOrder[i]);
                    invDominators.get(headersByOrder[i]).add(h);
		}
	    }
	    
	    if (Global.verbosity>=5) {
		Global.log.println("Dominator dump for "+this+":");
		for (Header h : c.headers()) {
		    Global.log.println("   "+h+": "+dominates(h));
		}
	    }
	}
    }
    
    public ArrayList< Header > dominators(Header h) {
	makeDomMap();
	return dominators.get(h);
    }
    
    public ArrayList< Header > dominates(Header h) {
	makeDomMap();
	return invDominators.get(h);
    }
    
    /** Answers the question: does h1 dominate h2? */
    public boolean dominates(Header h1,Header h2) {
	return bitDominators(h2).get(h1.order);
    }
    
    /** Does operation o1 dominate operation o2?  Only valid if the
        order is sane. */
    public boolean dominates(Operation o1,Operation o2) {
        Header h1=o1.head();
        Header h2=o2.head();
        if (h1==h2) {
            return o2.order>o1.order;
        } else {
            return dominates(h1,h2);
        }
    }
}


