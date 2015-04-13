/*
 * RefsLiveAtSafe.java
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

public class RefsLiveAtSafe {
    HashSet< Operation > knownOperations=new HashSet< Operation >();
    TwoWayMap< Operation, Var > map=new TwoWayMap< Operation, Var >();
    HashSet< Var > relevantOnlyForNonPC;
    
    public RefsLiveAtSafe(Code c) {
	assert c.isSSA();
	
        if (Global.verbosity>=3) {
            Global.log.println("Doing RefsLiveAtSafe analysis for "+c);
        }
        
        SimpleLivenessCalc slc=c.getSimpleLiveness();
	for (Header h : c.headers()) {
	    SimpleLivenessCalc.LocalCalc lc=
		slc.new LocalCalc(h);
	    for (Operation o : h.reverseOperations()) {
		lc.update(o);
		if (c.getSafepoints().get(o)) {
                    knownOperations.add(o);
                    if (Global.verbosity>=3) {
                        Global.log.println("Considering safepoint "+o+" with live: "+lc.currentlyLive());
                    }
		    for (Var v : lc.currentlyLive()) {
			assert v!=null;
			if (v.type().isObject() &&
			    (!Global.doRefMapOpt ||
                             c.mayBeCalledFromNative ||
			     v.inst().opcode()!=OpCode.GetArg)) {
                            if (Global.verbosity>=4) {
                                Global.log.println("   adding var "+v);
                            }
			    map.put(o,v);
			} else {
                            if (Global.verbosity>=4) {
                                Global.log.println("   ignoring var "+v);
                            }
                        }
		    }
		}
	    }
	}
        
        if (Global.verbosity>=3) {
            Global.log.println("RefsLiveAtSafe results: "+map);
        }
    }
    
    public Set< Var > forSafe(Operation o) {
        assert knownOperations.contains(o) : o;
	return map.valuesForKey(o);
    }
    
    /**
     * @return The set of safepoints that have a non-empty set of live variables.
     */
    public Set< Operation > relevantSafepoints() {
	return map.keySet();
    }
    
    /**
     * @return The set of variables that are live at safepoints.
     */
    public Set< Var > relevantVars() {
	return map.valueSet();
    }
    
    public boolean isRelevant(Operation o) {
        return map.containsKey(o);
    }
    
    public boolean isRelevant(Var v) {
        return map.containsValue(v);
    }
    
    private void buildNonPC() {
        if (relevantOnlyForNonPC==null) {
            relevantOnlyForNonPC=new HashSet< Var >();
            for (Var v : map.valueSet()) {
                boolean found=false;
                for (Operation o : map.keysForValue(v)) {
                    if (o.opcode()!=OpCode.PollCheck) {
                        found=true;
                        break;
                    }
                }
                if (found) {
                    relevantOnlyForNonPC.add(v);
                }
            }
        }
    }
    
    public Set< Var > relevantVarsOnlyForNonPC() {
        buildNonPC();
        return relevantOnlyForNonPC;
    }
    
    public boolean isRelevantOnlyForNonPC(Var v) {
        buildNonPC();
        return relevantOnlyForNonPC.contains(v);
    }
    
    public boolean isRelevantForAssign(Var v) {
        if (Global.optPollcheck) {
            return isRelevantOnlyForNonPC(v);
        } else {
            return isRelevant(v);
        }
    }
}


