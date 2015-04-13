/*
 * PredsCalc.java
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

public class PredsCalc {
    Code c;
    
    ArrayList< Header > terminals;
    HashMap< Header, ArrayList< Header > > normalPreds;
    HashMap< Header, ArrayList< Header > > excPreds;
    HashMap< Header, ArrayList< ExceptionHandler > > handlerToHeaderPreds;
    HashMap< ExceptionHandler, ArrayList< Header > > throwerToHandlerPreds;
    HashMap< ExceptionHandler, ArrayList< ExceptionHandler > > dropPreds;
    
    public PredsCalc(Code c) {
	this.c=c;
	terminals=new ArrayList< Header >();
	normalPreds=new HashMap< Header, ArrayList< Header > >();
	excPreds=new HashMap< Header, ArrayList< Header > >();
	handlerToHeaderPreds=new HashMap< Header, ArrayList< ExceptionHandler > >();
	throwerToHandlerPreds=new HashMap< ExceptionHandler, ArrayList< Header > >();
	dropPreds=new HashMap< ExceptionHandler, ArrayList< ExceptionHandler > >();
	
	if (Global.verbosity>=3) Global.log.println("Doing preds calc on "+c);
	
	MyStack< Header > worklist=new MyStack< Header >();
	worklist.push(c.root());
	while (!worklist.empty()) {
	    Header h=worklist.pop();
	    
	    if (h.getFooter().isTerminal()) {
		terminals.add(h);
	    }
	    for (Header h2 : h.normalSuccessors()) {
		ArrayList< Header > h2Preds=normalPreds.get(h2);
		if (h2Preds==null) {
		    h2Preds=new ArrayList< Header >();
		    normalPreds.put(h2,h2Preds);
		    if (!excPreds.containsKey(h2)) worklist.push(h2);
		}
		h2Preds.add(h);
	    }
	    for (ExceptionHandler eh : h.handlers()) {
		ArrayList< Header > ehPreds=throwerToHandlerPreds.get(eh);
		if (ehPreds==null) {
		    ehPreds=new ArrayList< Header >();
		    throwerToHandlerPreds.put(eh,ehPreds);
		    
		    // saw this ExceptionHandler for the first time, so add
		    // it to its successor's preds list
		    ArrayList< ExceptionHandler > ehhPreds=
			handlerToHeaderPreds.get(eh.next());
		    if (ehhPreds==null) {
			ehhPreds=new ArrayList< ExceptionHandler >();
			handlerToHeaderPreds.put((Header)eh.next(),ehhPreds);
		    }
		    ehhPreds.add(eh);
		}
		ehPreds.add(h);
		
		ArrayList< Header > h2Preds=excPreds.get(eh.next());
		if (h2Preds==null) {
		    h2Preds=new ArrayList< Header >();
		    excPreds.put((Header)eh.next(),h2Preds);
		    if (!normalPreds.containsKey(eh.next()))
			worklist.push((Header)eh.next());
		}
		h2Preds.add(h);
	    }
	}
	
	for (ExceptionHandler eh : c.handlers()) {
	    if (eh.dropsTo()!=null) {
		ArrayList< ExceptionHandler > ehPreds=dropPreds.get(eh.dropsTo());
		if (ehPreds==null) {
		    dropPreds.put(eh.dropsTo(),
				  ehPreds=new ArrayList< ExceptionHandler >());
		}
		ehPreds.add(eh);
	    }
	}
    }
    
    public ArrayList< Header > terminals() { return terminals; }
    
    public ArrayList< Header > normalPredecessors(Header h) {
	ArrayList< Header > result=normalPreds.get(h);
	if (result==null) {
	    return Header.EMPTY_AL;
	}
	return result;
    }
    public boolean isHandler(Header h) {
	return excPreds.containsKey(h);
    }
    public ArrayList< Header > exceptionalPredecessors(Header h) {
	ArrayList< Header > result=excPreds.get(h);
	if (result==null) {
	    return Header.EMPTY_AL;
	}
	return result;
    }
    
    public Iterable< Header > allPredecessors(Header h) {
	return Util.composeIterables(normalPredecessors(h),exceptionalPredecessors(h));
    }
    
    public boolean hasOnePredecessor(Header h) {
        Iterator< Header > i=allPredecessors(h).iterator();
        if (!i.hasNext()) {
            return false;
        }
        i.next();
        if (i.hasNext()) {
            return false;
        }
        return true;
    }
    
    public boolean isDead(Header h) {
        return !normalPreds.containsKey(h) && !excPreds.containsKey(h);
    }
    
    public ArrayList< ExceptionHandler > exceptionHandlerPredecessors(Header h) {
	ArrayList< ExceptionHandler > result=handlerToHeaderPreds.get(h);
	if (result==null) {
	    return ExceptionHandler.EMPTY_AL;
	}
	return result;
    }
    
    public ArrayList< Header > exceptionalPredecessors(ExceptionHandler eh) {
	ArrayList< Header > result=throwerToHandlerPreds.get(eh);
	if (result==null) {
	    return Header.EMPTY_AL;
	}
	return result;
    }
    
    // which exception handlers delegate to this one?
    public ArrayList< ExceptionHandler > dropsToPredecessors(ExceptionHandler eh) {
	ArrayList< ExceptionHandler > result=dropPreds.get(eh);
	if (result==null) {
	    return ExceptionHandler.EMPTY_AL;
	}
	return result;
    }
}


