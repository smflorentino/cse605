/*
 * SimpleLivenessCalc.java
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

// this is 11% of my exec time
public class SimpleLivenessCalc {
    Code c;
    
    VarSet empty;
    
    HashMap< Header, VarSet > liveAtHead;
    HashMap< Header, VarSet > liveAtTail;
    HashMap< Header, VarSet > liveForHandler;
    
    PredsCalc preds;
    MyStack< Header > worklist; /* use this because there are some goofs where this
                                   will be computed while there are ongoing
                                   computations involving the same headers ... so
                                   Worklist will complain. */

    SimpleLivenessCalc(Code c) {
	if (Global.verbosity>=4) {
	    Global.log.println("In SimpleLivenessCalc");
	}
	this.c=c;
        empty=new VarSet(c);
	liveAtHead=new HashMap< Header, VarSet >();
	liveAtTail=new HashMap< Header, VarSet >();
	liveForHandler=new HashMap< Header, VarSet >();
	preds=c.getPreds();
	worklist=new MyStack< Header >();
	for (Header h : preds.terminals()) {
	    if (Global.verbosity>=6) {
		Global.log.println("Terminal: "+h);
	    }
	    worklist.push(h);
	}
	propagate();
	// check if there are any blocks we didn't get to because this is
	// a non-terminating method.  if so, add them to the worklist and
	// try again
	boolean changed=true;
	while (changed) {
	    changed=false;
	    for (Header h : c.headers()) {
		if (!liveAtHead.containsKey(h)) {
		    changed=true;
		    if (Global.verbosity>=6) {
			Global.log.println("Repropagating "+h);
		    }
		    worklist.push(h);
		    propagate();
		    break;
		}
	    }
	}
	worklist=null;
	preds=null;
	if (Global.verbosity>=4) {
	    Global.log.println("Done with SimpleLivenessCalc");
	}
    }
    
    void propagate() {
	while (!worklist.empty()) {
	    Header h=worklist.pop();
	    VarSet curLive=liveAtTail.get(h);
            if (curLive==null) {
                curLive=new VarSet(c);
            } else {
                curLive=curLive.copy();
            }
            if (Global.verbosity>=6) {
                Global.log.println("Live at tail for "+h+": "+curLive);
            }
	    VarSet curForHandler=liveForHandler.get(h);
	    if (Global.verbosity>=7) {
		for (Node n=h.footer;n!=h;n=n.prev) {
		    Global.log.println("in "+h+" at "+n);
		}
	    }
	    for (Operation o : h.reverseOperations()) {
		update(o,curLive,curForHandler);
	    }
	    VarSet newLiveAtHead=curLive;
	    VarSet oldLiveAtHead=liveAtHead.get(h);
	    if (oldLiveAtHead==null || !oldLiveAtHead.equals(newLiveAtHead)) {
		if (Global.verbosity>=6) {
		    Global.log.println("Live at head for "+h+": "+newLiveAtHead);
		}
		liveAtHead.put(h,newLiveAtHead);
		for (Header h2 : preds.normalPredecessors(h)) {
		    VarSet oldLiveAtTail=liveAtTail.get(h2);
		    if (oldLiveAtTail==null) {
			liveAtTail.put(h2,newLiveAtHead.copy()); /* unnecessary
                                                                    copying? */
			worklist.push(h2);
		    } else if (oldLiveAtTail.addAll(newLiveAtHead)) {
			worklist.push(h2);
		    }
		}
		for (Header h2 : preds.exceptionalPredecessors(h)) {
		    VarSet oldLiveForHandler=liveForHandler.get(h2);
		    if (oldLiveForHandler==null) {
			liveForHandler.put(h2,newLiveAtHead.copy()); /* unnecessary
                                                                        copying? */
			worklist.push(h2);
		    } else if (oldLiveForHandler.addAll(newLiveAtHead)) {
			worklist.push(h2);
		    }
		}
	    }
	}
    }
    
    UpdateState update(Operation o,
		       VarSet curLive,
		       VarSet liveForHandler) {
	if (o instanceof Instruction) {
	    // if the LHS is dead and this op does not cause side effects,
	    // skip it, so that we don't declare the RHS live unnecessarily.
	    Var lhs=((Instruction)o).lhs();
	    if (!curLive.contains(lhs) && !c.getSideEffects().get(o)) {
		return UpdateState.DEAD;
	    }
	    curLive.remove(lhs);
	}
	for (Arg a : o.rhs()) {
	    if (a instanceof Var) {
		curLive.add((Var)a);
	    }
	}
	if (c.getThrows().get(o) && liveForHandler!=null) {
	    curLive.addAll(liveForHandler);
	}
	return UpdateState.LIVE;
    }
    
    public VarSet liveAtHead(Header h) {
	VarSet result=liveAtHead.get(h);
	assert result!=null
	    : "No liveness information at head of "+h;
	return result;
    }
    
    public boolean liveAtHead(Header h,Var v) {
	return liveAtHead(h).contains(v);
    }
    
    public VarSet liveAtTail(Header h) {
	VarSet result=liveAtTail.get(h);
	if (result==null) {
	    return empty;
	}
	return result;
    }
    
    public boolean liveAtTail(Header h,Var v) {
	return liveAtTail(h).contains(v);
    }
    
    public VarSet liveForHandler(Header h) {
	VarSet result=liveForHandler.get(h);
	if (result==null) { 
	    return empty;
	}
	return result;
    }
    
    public boolean liveForHandler(Header h,Var v) {
	return liveForHandler(h).contains(v);
    }
    
    public class LocalCalc {
	VarSet curLiveForHandler;
	VarSet curLive;
	
	public LocalCalc(Header h) {
	    curLive=liveAtTail(h).copy();
            if (Global.verbosity>=5) {
                Global.log.println("LocalCalc: live at tail for "+h+": "+curLive);
            }
	    curLiveForHandler=liveForHandler.get(h);
	}
	
	public boolean currentlyLive(Var v) {
	    return curLive.contains(v);
	}
	
	public VarSet currentlyLive() {
	    return curLive;
	}
	
	public UpdateState update(Operation o) {
	    return SimpleLivenessCalc.this.update(o,curLive,curLiveForHandler);
	}
    }
    
    public class ForwardLocalCalc {
	HashMap< Operation, VarSet > deaths;
	HashSet< Operation > births;
	VarSet curLive;
	
        // FIXME: it's not nice to have to recompute this every time.
	public ForwardLocalCalc(Header h) {
	    deaths=new HashMap< Operation, VarSet >();
	    births=new HashSet< Operation >();
	    VarSet curLiveForHandler=liveForHandler(h);
	    
	    curLive=liveAtTail(h).copy();
	    
            VarSet curDeaths=new VarSet(c);
	    for (Operation o : h.reverseOperations()) {
		if (o instanceof Instruction) {
		    Var lhs=((Instruction)o).lhs();
		    if (curLive.remove(lhs)) {
			births.add(o);
		    }
		}
		for (Arg a : o.rhs()) {
		    if (a instanceof Var) {
			Var v=(Var)a;
			if (curLive.add(v)) {
			    curDeaths.add(v);
			}
		    }
		}
		//for (Var v : curLiveForHandler) {
		//    if (curLive.add(v)) {
		//        curDeaths.add(v);
		//    }
		//}
                curLiveForHandler.cascade(curLive,curDeaths);
		if (!curDeaths.isEmpty()) {
		    deaths.put(o,curDeaths);
                    curDeaths=new VarSet(c);
		}
	    }
	}
	
	public boolean currentlyLive(Var v) {
	    return curLive.contains(v);
	}
	
	/** Answers the question: is this a variable and is it currently live? */
	public boolean currentlyLive(Arg a) {
	    return curLive.contains(a);
	}
        
        /** Is the argument list live?  this differs from currentlyLive(Arg) not
            just in that it takes an array, but also in that it treats non-Var
            args as always being live. */
        public boolean currentlyLive(Arg[] args) {
            for (Arg a : args) {
                if (a instanceof Var &&
                    !currentlyLive((Var)a)) {
                    return false;
                }
            }
            return true;
        }
	
	public boolean birthsLHS(Operation o) {
	    return births.contains(o);
	}
	
	public VarSet deaths(Operation o) {
	    VarSet result=deaths.get(o);
	    if (result==null) {
		return empty;
	    } else {
		return result;
	    }
	}
	
	public boolean kills(Operation o,Var v) {
	    return deaths(o).contains(v);
	}
	
	public boolean births(Operation o,Var v) {
	    return birthsLHS(o) && ((Instruction)o).lhs()==v;
	}
	
	public void update(Operation o) {
	    VarSet curDeaths=deaths.get(o);
	    if (curDeaths!=null) {
		curLive.removeAll(curDeaths);
	    }
	    if (births.contains(o)) {
		curLive.add(((Instruction)o).lhs());
	    }
	}
    }
}

