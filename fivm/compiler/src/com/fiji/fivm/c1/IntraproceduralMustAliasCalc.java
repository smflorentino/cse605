/*
 * IntraproceduralMustAliasCalc.java
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

// is this SSA?  or is it more than SSA?  the key is what happens when you do:
//
// B1:   x = foo()
//       y = x
//       goto B1
//
// at B1, we will remove foo() from x's and y's sets.  well.  but.  for y to be
// live at foo(), there'd have to be a phi function at the top of B1.  god damn
// this is confusing.

// this is completely broken.  we need real phi functions.  using real dominance
// frontiers.  note that by itself, this won't handle the following case:
//
// if (predicate) {
//     x = y
// } else {
//     x = y
// }
//
// but we can fix that by being smart.  we can just have the dominance frontier
// analysis create sets of variables that need merging at the heads of basic
// blocks, and then only insert the phi functions if we observe a conflict.  or
// something.

public class IntraproceduralMustAliasCalc {
    Code c;
    
    HashMap< Header, TwoWayMap< Var, Instruction > > aliasAtHead;
    HashMap< Header, TwoWayMap< Var, Instruction > > aliasAtTail;
    HashMap< Header, TwoWayMap< Var, Instruction > > aliasForHandler;
    HashMap< Operation, Instruction[] > sourcesForOp;
    
    Worklist worklist;
    
    SimpleLivenessCalc slc; // used for pruning sets
    
    int cnt=1;
    
    // this is almost 20% of my execution time
    IntraproceduralMustAliasCalc(Code c) {
	this.c=c;
	
	if (!c.hasPhi()) {
	    throw new CompilerException("Cannot do LocalMustAliasCalc on "+c+" because it doesn't have Phi functions");
	}
	
	aliasAtHead=new HashMap< Header, TwoWayMap< Var, Instruction > >();
	aliasAtTail=new HashMap< Header, TwoWayMap< Var, Instruction > >();
	aliasForHandler=new HashMap< Header, TwoWayMap< Var, Instruction > >();
	sourcesForOp=new HashMap< Operation, Instruction[] >();
	
	worklist=new Worklist();
	
	slc=c.getSimpleLiveness();
	
	worklist.push(c.root());
	aliasAtHead.put(c.root(),new TwoWayMap< Var, Instruction >());
	while (!worklist.empty()) {
	    Header h=worklist.pop();
	    
	    if (Global.verbosity>=3) {
		Global.log.println("In "+h);
	    }
	    
	    LocalCalc lc=new LocalCalc(h);
	    TwoWayMap< Var, Instruction > curAlias=lc.getCurAlias();
	    TwoWayMap< Var, Instruction > curForHandler=null;
	    for (Operation o : h.operations()) {
		if (Global.verbosity>=4) {
		    Global.log.println("At "+o);
		}
		Instruction[] rhs=new Instruction[o.rhs().length];
		for (int i=0;i<rhs.length;++i) {
		    rhs[i]=lc.getSource(o.rhs()[i]);
		}
		sourcesForOp.put(o,rhs);
		if (c.getThrows().get(o)) {
		    curForHandler=
			mergeForHandler(
			    curAlias,curForHandler,slc.liveForHandler(h));
		}
		lc.update(o);
	    }
	    
	    if (Global.verbosity>=5) {
		Global.log.println("done with "+h+" and have "+curAlias);
	    }
	    
	    TwoWayMap< Var, Instruction > oldAliasAtTail=aliasAtTail.get(h);
	    if (oldAliasAtTail==null || !curAlias.equals(oldAliasAtTail)) {
		aliasAtTail.put(h,curAlias);
		for (Header h2 : h.normalSuccessors()) {
		    merge(h,h2,curAlias,MergeMode.NORMAL);
		}
	    }
	    
	    if (h.hasHandlers() && curForHandler!=null) {
		TwoWayMap< Var, Instruction > oldAliasForHandler=aliasForHandler.get(h);
		if (oldAliasForHandler==null || !curForHandler.equals(oldAliasForHandler)) {
		    aliasForHandler.put(h,curForHandler);
		    for (Header h2 : h.exceptionalSuccessors()) {
			merge(h,h2,curForHandler,MergeMode.EXCEPTION);
		    }
		}
	    }
	}
    }
    
    TwoWayMap< Var, Instruction > mergeForHandler(TwoWayMap< Var, Instruction > curAlias,
						  TwoWayMap< Var, Instruction > otherAlias,
						  VarSet liveness) {
	if (otherAlias==null) {
	    TwoWayMap< Var, Instruction > newAlias=
		new TwoWayMap< Var, Instruction >();
	    for (Var v : liveness) {
		newAlias.putMulti(v,curAlias.valuesForKey(v));
	    }
	    return newAlias;
	} else {
	    for (Var v : liveness) {
		HashSet< Instruction > myInstSet=curAlias.valuesForKey(v);
		HashSet< Instruction > otherInstSet=otherAlias.valuesForKey(v);
		assert myInstSet.size()==1 : v;
		if (!otherInstSet.isEmpty()) {
		    Instruction myInst=myInstSet.iterator().next();
		    Instruction otherInst=otherInstSet.iterator().next();
		    if (myInst!=otherInst) {
			otherAlias.killKey(v);
		    }
		}
	    }
	    return otherAlias;
	}
    }
    
    static enum MergeMode {
	NORMAL, EXCEPTION
    }
    
    private void assertMySet(Header h,Header h2,Var v,
			     HashSet< Instruction > myInstSet,
			     MergeMode mode) {
	if (mode==MergeMode.NORMAL) {
	    assert myInstSet.size()==1
		: "for "+v+" we have myInstSet = "+myInstSet+" at merge from "+h+" to "+h2;
	} else {
	    assert myInstSet.size()<=1
		: "for "+v+" we have myInstSet = "+myInstSet+" at merge from "+h+" to "+h2;
	}
    }
    
    void merge(Header h,Header h2,
	       TwoWayMap< Var, Instruction > curAlias,
	       MergeMode mode) {
	TwoWayMap< Var, Instruction > oldAliasAtHead=aliasAtHead.get(h2);
	boolean changed=false;
	if (oldAliasAtHead==null) {
	    TwoWayMap< Var, Instruction > newAliasAtHead=
		new TwoWayMap< Var, Instruction >();
	    for (Var v : slc.liveAtHead(h2)) {
		if (Global.verbosity>=5) {
		    Global.log.println("At "+h2+" adding alias for "+v);
		}
		HashSet< Instruction > myInstSet=curAlias.valuesForKey(v);
		assertMySet(h,h2,v,myInstSet,mode);
		newAliasAtHead.putMulti(v,myInstSet);
	    }
	    aliasAtHead.put(h2,newAliasAtHead);
	    changed=true;
	} else {
	    for (Var v : slc.liveAtHead(h2)) {
		HashSet< Instruction > myInstSet=curAlias.valuesForKey(v);
		HashSet< Instruction > otherInstSet=oldAliasAtHead.valuesForKey(v);
		assertMySet(h,h2,v,myInstSet,mode);
		assert otherInstSet.size()<=1
		    : "for "+v+" at "+h2+" coming from "+h+" we have otherInstSet = "+otherInstSet;
		if (myInstSet.isEmpty() && !otherInstSet.isEmpty()) {
		    oldAliasAtHead.killKey(v);
		    changed=true;
		} else if (!otherInstSet.isEmpty()) {
		    Instruction myInst=myInstSet.iterator().next();
		    Instruction otherInst=otherInstSet.iterator().next();
		    if (myInst!=otherInst) {
			oldAliasAtHead.killKey(v);
			changed=true;
		    }
		}
	    }
	}
	if (changed) {
	    worklist.push(h2);
	}
    }
    
    public TwoWayMap< Var, Instruction > aliasAtHead(Header h) {
	return aliasAtHead.get(h);
    }
    
    public TwoWayMap< Var, Instruction > aliasAtTail(Header h) {
	return aliasAtTail.get(h);
    }
    
    public Instruction[] rhs(Operation o) {
	Instruction[] result=sourcesForOp.get(o);
	assert result!=null : o;
	return result;
    }
    
    public boolean isSource(Instruction i) {
	return i.opcode()!=OpCode.Mov
	    || !(i.rhs()[0] instanceof Var);
    }
    
    public boolean isSource(Operation o) {
	return o instanceof Instruction
	    && isSource((Instruction)o);
    }
    
    public Instruction lhs(Instruction i) {
	if (isSource(i)) {
	    return i;
	} else {
	    return rhs(i)[0];
	}
    }
    
    public class LocalCalc {
	Header h;
	TwoWayMap< Var, Instruction > curAlias;
	SimpleLivenessCalc.ForwardLocalCalc flc;
	
	public LocalCalc(Header h) {
	    this.h=h;
	    curAlias=aliasAtHead.get(h).copy();
	    if (Global.verbosity>=5) {
		Global.log.println("Starting local analysis of "+h+" with curAlias = "+curAlias);
	    }
	    flc=slc.new ForwardLocalCalc(h);
	}
	
	public SimpleLivenessCalc.ForwardLocalCalc getLiveness() { return flc; }
	
	public void update(Instruction i) {
	    curAlias.killValue(i);
	    if (i.opcode()==OpCode.Mov && i.rhs()[0] instanceof Var) {
		Set< Instruction > srcs=curAlias.valuesForKey((Var)i.rhs()[0]);
		assert srcs.size()==1 : "for "+i+" in "+h+"; srcs = "+srcs+"; curAlias = "+curAlias;
		curAlias.killKey(i.lhs());
		curAlias.putMulti(i.lhs(),srcs);
	    } else {
		// value source!
		if (Global.verbosity>=6) {
		    Global.log.println("Creating alias: "+i.lhs()+" -> "+i);
		}
		curAlias.killKey(i.lhs());
		curAlias.put(i.lhs(),i);
	    }
	    for (Var v : flc.deaths(i)) {
		if (!flc.births(i,v)) {
		    if (Global.verbosity>=6) {
			Global.log.println("Killing "+v);
		    }
		    curAlias.killKey(v);
		}
	    }
	    flc.update(i);
	}
	
	public void update(Operation o) {
	    if (o instanceof Instruction) {
		update((Instruction)o);
	    }
	}
	
	public TwoWayMap< Var, Instruction > getCurAlias() {
	    return curAlias;
	}
	
	// may return false if we haven't processed all of the Phi functions
	// at the top of the block.
	public boolean hasSource(Arg v) {
	    return !curAlias.valuesForKey(v).isEmpty();
	}
	
	public Instruction getSource(Arg v) {
	    HashSet< Instruction > sources=curAlias.valuesForKey(v);
	    if (sources.isEmpty()) {
		return null;
	    } else {
		assert sources.size()==1;
		return sources.iterator().next();
	    }
	}
	
	public Instruction forceSource(Arg v) {
	    HashSet< Instruction > sources=curAlias.valuesForKey(v);
	    assert sources.size()==1 : v;
	    return sources.iterator().next();
	}
	
	public HashSet< Var > varsWithSource(Instruction i) {
	    if (i==null) {
		return Var.EMPTY_HS;
	    } else {
		return curAlias.keysForValue(i);
	    }
	}
	
	public HashSet< Var > aliases(Arg v) {
	    return varsWithSource(getSource(v));
	}
	
	public boolean aliased(Arg a,Arg b) {
	    HashSet< Instruction > as=curAlias.valuesForKey(a);
	    HashSet< Instruction > bs=curAlias.valuesForKey(b);
	    return !as.isEmpty()
		&& !bs.isEmpty()
		&& as.equals(bs);
	}
    }
}


