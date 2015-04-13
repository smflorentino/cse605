/*
 * LPredsCalc.java
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

import java.util.*;
import com.fiji.util.*;
import com.fiji.fivm.c1.*;

public class LPredsCalc {
    LCode c;
    
    ArrayList< LHeader > terminals;
    HashMap< LHeader, ArrayList< LHeader > > preds;
    HashSet< LHeader > pinned;
    
    public LPredsCalc(LCode c) {
	this.c=c;
	terminals=new ArrayList< LHeader >();
	preds=new HashMap< LHeader, ArrayList< LHeader > >();
        pinned=new HashSet< LHeader >();
	
	if (Global.verbosity>=3) Global.log.println("Doing preds calc on "+c);
        
        // FIXME: maybe this should be somewhere else?
        for (LHeader h : c.headers()) {
            for (LOp o : h.operations()) {
                LinkableSet ls=o.linkableSet();
                if (ls!=null) {
                    for (Linkable l : ls) {
                        if (l instanceof LJumpTable) {
                            for (LHeader h2 : ((LJumpTable)l).headers()) {
                                pinned.add(h2);
                            }
                        }
                    }
                }
            }
        }
	
	MyStack< LHeader > worklist=new MyStack< LHeader >();
	worklist.push(c.root());
	while (!worklist.empty()) {
	    LHeader h=worklist.pop();
	    
	    if (h.getFooter().terminal()) {
		terminals.add(h);
	    }
	    for (LHeader h2 : h.successors()) {
		ArrayList< LHeader > h2Preds=preds.get(h2);
		if (h2Preds==null) {
		    h2Preds=new ArrayList< LHeader >();
		    preds.put(h2,h2Preds);
                    worklist.push(h2);
		}
		h2Preds.add(h);
	    }
	}
    }
    
    public ArrayList< LHeader > terminals() { return terminals; }
    
    public HashSet< LHeader > pinned() { return pinned; }
    
    public boolean pinned(LHeader h) { return pinned.contains(h); }
    
    public ArrayList< LHeader > preds(LHeader h) {
	ArrayList< LHeader > result=preds.get(h);
	if (result==null) {
	    return LHeader.EMPTY_AL;
	}
	return result;
    }
    
    public boolean isDead(LHeader h) {
        return !preds.containsKey(h);
    }
    
}


