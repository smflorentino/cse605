/*
 * KillDeadCode.java
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

public class KillDeadCode extends CodePhase {
    private static boolean PARANOID=false;
    
    public KillDeadCode(Code code) { super(code); }
    
    void verifySanity() {
	if (PARANOID) {
	    for (Header h : code.headers()) {
		assert h.next!=null : h;
		assert h.next.prev==h : h;
	    }
	}
    }
    
    public boolean runOnce() {
	boolean changed=false;
	PredsCalc preds=new PredsCalc(code);
	
	verifySanity();

	// figure out which blocks are dead, and remove them
	// a block is dead if it is unreachable by forward control flow.
	HashSet< Header > seen=new HashSet< Header >();
	MyStack< Header > worklist=new MyStack< Header >();
	seen.add(code.root());
	worklist.push(code.root());
	while (!worklist.empty()) {
	    Header h=worklist.pop();
	    for (Header h2 : h.allSuccessors()) {
		if (seen.add(h2)) {
		    worklist.push(h2);
		}
	    }
	}
	boolean curChanged=false;
	for (Iterator< Header > i=code.headers().iterator();i.hasNext();) {
	    Header h=i.next();
	    if (!seen.contains(h)) {
		changed=curChanged=true;
		if (Global.verbosity>=4) {
		    Global.log.println("Killing "+h+" because it is not reachable via control flow");
		}
		i.remove();
	    }
	}
	if (curChanged) preds=new PredsCalc(code);

	verifySanity();
	    
	// remove catches of uninstantiable exceptions from handler lists
	curChanged=false;
	for (Header h : code.headers()) {
	    if (h.handler!=null && h.handler.handles!=null &&
		!h.handler.handles.hasInstances()) {
		if (Global.verbosity>=5) {
		    Global.log.println("Removing "+h.handler+" with type "+
					h.handler.handles+" from "+h+" because the type "+
					"is uninstantiable.");
		}
		h.handler=h.handler.dropsTo;
		changed=curChanged=true;
	    }
	}
	for (ExceptionHandler eh : code.handlers()) {
	    if (eh.dropsTo!=null && eh.dropsTo.handles!=null &&
		!eh.dropsTo.handles.hasInstances()) {
		if (Global.verbosity>=5) {
		    Global.log.println("Removing "+eh.dropsTo+" with type "+
					eh.dropsTo.handles+" from "+eh+" because the type "+
					"is uninstantiable.");
		}
		eh.dropsTo=eh.dropsTo.dropsTo;
		changed=curChanged=true;
	    }
	}
	if (curChanged) preds=new PredsCalc(code);
	    
	verifySanity();
	    
	// figure out which exception handlers are dead, and remove them
	// an exception handler is dead if nobody uses it
	curChanged=false;
	for (Iterator< ExceptionHandler > i=code.handlers().iterator();
	     i.hasNext();) {
	    ExceptionHandler eh=i.next();
	    if (preds.exceptionalPredecessors(eh).isEmpty()) {
		i.remove();
		curChanged=changed=true;
	    }
	}
	if (curChanged) preds=new PredsCalc(code);
        
	verifySanity();
	    
	// figure out which exception handlers on a header's handler stack
	// are unreachable, and drop them.
	curChanged=false;
	for (Header h : code.headers()) {
	    for (ExceptionHandler eh=h.handler;eh!=null;eh=eh.dropsTo) {
		if (eh.handles==Global.root().throwableClass &&
		    eh.dropsTo!=null) {
		    eh.dropsTo=null;
		    changed=curChanged=true;
		    break;
		}
	    }
	}
	if (curChanged) preds=new PredsCalc(code);
	    
	verifySanity();

	return changed;
    }
    
    public void visitCode() {
	boolean changed=true;
	while (changed) {
	    changed=runOnce();

	    if (changed) {
		code.killAllAnalyses();
		setChangedCode();
	    }
	}
    }
}


