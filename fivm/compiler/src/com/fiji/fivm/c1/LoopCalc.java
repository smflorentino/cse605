/*
 * LoopCalc.java
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
import com.fiji.util.*;

public class LoopCalc {
    // this will calculate the loop nesting of basic blocks.
    
    // Maybe we should have a global loop calc?  And maybe we should also have a
    // dynamic profiler?  And then we can compare the results of the two?
    
    // or: have a dynamic profile of Classpath based on running SPEC?  that might
    // be wrong given that SPEC doesn't use the new java.util...
    
    HashMap< Header, Integer > depthMap=new HashMap< Header, Integer >();

    TwoWayMap< Header, Header > headerToFooter=new TwoWayMap< Header, Header >();
    TwoWayMap< Header, Header > headerToBody=new TwoWayMap< Header, Header >();

    public LoopCalc(Code c) {
        PredsCalc preds=c.getPreds();
	DominatorCalc doms=c.getNormalDominators();
	
	// find the loops, but just in terms of the back edges
	for (Header h1 : c.headers()) {
	    for (Header h2 : h1.normalSuccessors()) {
                // h1 = footer
                // h2 = header
		if (doms.dominates(h2,h1)) {
		    headerToFooter.put(h2,h1);
		}
	    }
	}
	
	// find the bodies of the loops
        for (Header h1 : headerToFooter.keySet()) {
            for (Header h2 : headerToFooter.valuesForKey(h1)) {
                // h1 = header
                // h2 = footer
                
                if (h2==h1) {
                    headerToBody.put(h1,h1);
                } else {
                    // search backward from the footer until we get to the header.
                    MyStack< Header > worklist=new MyStack< Header >();
                    HashSet< Header > seen=new HashSet< Header >();
                    seen.add(h1);
                    seen.add(h2);
                    worklist.push(h2);
                    while (!worklist.empty()) {
                        Header h3=worklist.pop();
                        assert h3!=h1;
                        for (Header h4 : preds.normalPredecessors(h3)) {
                            if (seen.add(h4)) {
                                worklist.push(h4);
                            }
                        }
                    }
                    headerToBody.putMulti(h1,seen);
                }
            }
	}
	
	// figure out depth
	for (Header h : c.headers()) {
	    depthMap.put(h,headerToBody.numKeysForValue(h));
	}
    }
    
    public Set< Header > loopsFor(Header h) {
	return headerToBody.keysForValue(h);
    }
    
    public Set< Header > loopBody(Header h) {
	return headerToBody.valuesForKey(h);
    }
    
    public Set< Header > loopHeaders() {
        return headerToBody.keySet();
    }
    
    public int loopDepth(Header h) {
	return depthMap.get(h);
    }
    
    /**
     * Returns a mutable spectrum of loop depths.  You can safely modify this
     * spectrum since it is a copy.
     */
    public Spectrum< Header > loopDepths() {
        Spectrum< Header > result=new Spectrum< Header >();
        for (Map.Entry< Header, Integer > e : depthMap.entrySet()) {
            result.add(e.getKey(),e.getValue());
        }
        return result;
    }
}


