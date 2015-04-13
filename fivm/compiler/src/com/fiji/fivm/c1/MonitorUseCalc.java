/*
 * MonitorUseCalc.java
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

// NOTE: this will not give precise results unless you remove nullchecks.

public class MonitorUseCalc {
    
    Code c;
    
    // the final results table
    HashSet< Instruction > multiEnters;
    HashSet< Instruction > escapingEnters;
    HashSet< Instruction > unmatchedExits;
    TwoWayMap< Instruction, Instruction > nestedToEnclosing;
    TwoWayMap< Instruction, Instruction > enterToExit;
    
    MonitorUseCalc(Code c) {
	this.c=c;
	
	assert c.isSSA();
	
	multiEnters=new HashSet< Instruction >();
	escapingEnters=new HashSet< Instruction >();
	unmatchedExits=new HashSet< Instruction >();
	nestedToEnclosing=new TwoWayMap< Instruction, Instruction >();
	enterToExit=new TwoWayMap< Instruction, Instruction >();
	
	boolean isInteresting=false;
	
	for (Header h : c.headers()) {
	    for (Instruction i : h.instructions()) {
		if (i.opcode()==OpCode.MonitorEnter ||
		    i.opcode()==OpCode.MonitorExit) {
		    isInteresting=true;
		    break;
		}
	    }
	}
	
	if (!isInteresting) return;
	
	HashMap< Header, AbstractState > stateAtHead=
	    new HashMap< Header, AbstractState >();
	HashMap< Header, AbstractState > stateAtTail=
	    new HashMap< Header, AbstractState >();
	HashMap< Header, AbstractState > stateForHandler=
	    new HashMap< Header, AbstractState >();
	Worklist worklist=new Worklist();
	
	worklist.push(c.root());
	stateAtHead.put(c.root(),new AbstractState());
	while (!worklist.empty()) {
	    Header h=worklist.pop();
	    
	    AbstractState curState=stateAtHead.get(h).copy();
	    AbstractState curForHandler=null;
	    
	    if (Global.verbosity>=5) {
		Global.log.println("At "+h+" with "+curState);
	    }
	    
	    // FIXME: this currently keeps a stack of monitorenters for
	    // each source, which is overly pessimistic.  we could just
	    // keep a count of the number of enters for each source, since
	    // any enter for the same source is equivalent.
	    
	    for (Operation o : h.operations()) {
		if (c.getThrows().get(o) && o.opcode()!=OpCode.MonitorExit) {
		    curForHandler=curState.mergeTo(curForHandler);
		}
		if (o.opcode()==OpCode.MonitorEnter ||
		    o.opcode()==OpCode.MonitorExit) {
		    Instruction i=(Instruction)o;
		    Var thisSource=(Var)i.rhs(0);
		    MyStack< Instruction > entersForThisSource=curState.stacks.get(thisSource);
		    if (i.opcode()==OpCode.MonitorEnter) {
			if (entersForThisSource==null) {
			    curState.stacks.put(
				thisSource,
				entersForThisSource=new MyStack< Instruction >());
			} else {
			    nestedToEnclosing.putMulti(
				i,entersForThisSource.finiteView());
			}
			if (curState.seen.contains(i)) {
			    multiEnters.add(i);
			    entersForThisSource.clear();
			} else {
			    curState.seen.add(i);
			}
			entersForThisSource.push(i);
		    } else if (i.opcode()==OpCode.MonitorExit) {
			if (entersForThisSource!=null && !entersForThisSource.empty()) {
			    Instruction thisEnter=entersForThisSource.peek();
			    enterToExit.put(thisEnter,i);
			    curState.seen.remove(thisEnter);
			    
			    // this only affects curState, not curForHandler
			    entersForThisSource.pop();
			} else {
			    unmatchedExits.add(i);
			}
		    }
		} else if (o instanceof Footer) {
		    if (EscapingFooterCalc.get(h)) {
			if (Global.verbosity>=5 && !curState.seen.isEmpty()) {
			    Global.log.println("Adding "+curState.seen+" to escaping set at "+h);
			}
			escapingEnters.addAll(curState.seen);
		    }
		} else {
		    curState.stacks.remove(o);
		}
	    }
	    
	    AbstractState myTailState=stateAtTail.get(h);
	    if (myTailState==null || !curState.equals(myTailState)) {
		stateAtTail.put(h,curState);
		for (Header h2 : h.normalSuccessors()) {
		    if (Global.verbosity>=5) {
			Global.log.println("Considering "+h2);
		    }
		    boolean changed=false;
		    AbstractState otherState=stateAtHead.get(h2);
		    if (otherState==null) {
			stateAtHead.put(h2,curState.copy());
			changed=true;
		    } else {
			changed=otherState.mergeWith(curState);
		    }
		    if (changed) {
			if (Global.verbosity>=5) {
			    Global.log.println("Propagating to "+h2);
			}
			worklist.push(h2);
		    }
		}
	    }
	    
	    if (h.hasHandlers() &&
		curForHandler!=null /* may be null if we had a handler due to a
				       MonitorExit but we just proved that it
				       doesn't throw. */) {
		assert curForHandler!=null : "No state for handler in "+h;
		AbstractState myHandlerState=stateForHandler.get(h);
		if (myHandlerState==null || !curForHandler.equals(myHandlerState)) {
		    stateForHandler.put(h,curForHandler);
		    // FIXME: if we don't have a handler for Throwable, then the monitorenters
		    // escape!
		    for (Header h2 : h.exceptionalSuccessors()) {
			if (Global.verbosity>=5) {
			    Global.log.println("Considering exceptional successor "+h2+" in "+h);
			}
			boolean changed=false;
			AbstractState otherState=stateAtHead.get(h2);
			if (otherState==null) {
			    stateAtHead.put(h2,curForHandler.copy());
			    if (Global.verbosity>=5) {
				Global.log.println("Propagating "+curForHandler+" to "+h2);
			    }
			    changed=true;
			} else {
			    changed=otherState.mergeWith(curForHandler);
			    if (Global.verbosity>=5) {
				Global.log.println("Propagating "+otherState+" to "+h2);
			    }
			}
			if (changed) {
			    if (Global.verbosity>=5) {
				Global.log.println("Propagating to "+h2);
			    }
			    worklist.push(h2);
			}
		    }
		}
	    } else if (curForHandler!=null) {
		if (Global.verbosity>=5 && !curState.seen.isEmpty()) {
		    Global.log.println("Adding "+curForHandler.seen+" to escaping set at "+h);
		}
		escapingEnters.addAll(curForHandler.seen);
	    }
	}
	
    }

    static class AbstractState {
	HashMap< Var, MyStack< Instruction > > stacks; // maps value sources to stacks of monitorenters
	HashSet< Instruction > seen; // the monitorenter instructions we've seen, that have not been monitorexited.
	
	AbstractState() {
	    stacks=new HashMap< Var, MyStack< Instruction > >();
	    seen=new HashSet< Instruction >();
	}
	
	AbstractState copy() {
	    AbstractState result=new AbstractState();
	    for (Map.Entry< Var, MyStack< Instruction > > e : stacks.entrySet()) {
		result.stacks.put(e.getKey(),e.getValue().copy());
	    }
	    result.seen.addAll(seen);
	    return result;
	}
	
	boolean equals(AbstractState other) {
	    return stacks.equals(other.stacks)
		&& seen.equals(other.seen);
	}
	
	AbstractState mergeTo(AbstractState other) {
	    if (other==null) {
		return copy();
	    } else {
		other.mergeWith(this);
		return other;
	    }
	}
	
	boolean mergeWith(AbstractState other) {
	    boolean changed=false;
	    for (Var src : stacks.keySet()) {
		MyStack< Instruction > stack=stacks.get(src);
		MyStack< Instruction > otherStack=other.stacks.get(src);
		if (otherStack==null) {
		    if (Global.verbosity>=5) {
			Global.log.println("Clearing stack for "+src);
		    }
		    stack.clear();
		    changed=true;
		} else {
		    int amountMatches=0;
		    for (int i=0;i<Math.min(stack.height(),otherStack.height());++i) {
			if (stack.getFromTop(i)==otherStack.getFromTop(i)) {
			    amountMatches++;
			} else {
			    break;
			}
		    }
		    if (amountMatches!=stack.height()) {
			if (Global.verbosity>=5) {
			    Global.log.println(
				"Shifting stack for "+src+" by "+
				(stack.height()-amountMatches));
			}
			stack.shift(stack.height()-amountMatches);
			changed=true;
		    }
		}
	    }
	    changed|=seen.addAll(other.seen);
	    return changed;
	}
	
	public String toString() {
	    return "State[stacks = "+stacks+", seen = "+seen+"]";
	}
    }
    
    public boolean isMultiEnter(Operation enter) {
	return multiEnters.contains(enter);
    }
    
    public boolean isEscapingEnter(Operation enter) {
	return escapingEnters.contains(enter);
    }
    
    public boolean isUnmatchedExit(Operation exit) {
	return unmatchedExits.contains(exit);
    }
    
    public boolean usesStructuredLocking() {
	return multiEnters.isEmpty()
	    && escapingEnters.isEmpty()
	    && unmatchedExits.isEmpty();
    }
    
    public Set< Instruction > enters() {
	return enterToExit.keySet();
    }
    
    public HashSet< Instruction > enclosingEnters(Operation enter) {
	return nestedToEnclosing.valuesForKey(enter);
    }
    
    public HashSet< Instruction > nestedEnters(Operation enter) {
	return nestedToEnclosing.keysForValue(enter);
    }
    
    public HashSet< Instruction > exitsForEnter(Operation enter) {
	return enterToExit.valuesForKey(enter);
    }
    
    public HashSet< Instruction > entersForExit(Operation exit) {
	return enterToExit.keysForValue(exit);
    }
    
    public Instruction enterForExit(Operation exit) {
	HashSet< Instruction > set=entersForExit(exit);
	assert set.size()==1 : set;
	return set.iterator().next();
    }
    
}


