/*
 * MakeSSA.java
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

public class MakeSSA extends CodePhase {
    public MakeSSA(Code c) { super(c); }
    
    public void visitCode() {
	// FIXME: add a HardMov instruction.  HardMov is a Mov that is not
	// copy-propped in LMAC.  insert HardMovs just before type checks,
	// or any other kind of instruction that could allow us to learn
	// about the nature of a variable.  this could simplify the implementation
	// of OptConst.  on the other hand, it might end up making it less
	// precise (anywhere that AssignCalc is used).  it will certainly make
	// assign calc less precise, which would be a disaster.  one possibility
	// is to have an optional HardMov inserter.  OptConst won't need it,
	// but the CFA could benefit from it by getting additional type filtering.
	// or not.  who knows.  it's something to consider though.
	
	// but what if there was an alternate formulation of SSA that supported
	// something like HardMov without leading to a loss of precision?
	
	new SplitUnhappyBlocks(code).doit();
	new SplitHandlers(code).doit();
	new PutPhi(code).doit();
	new InsertEdgeNodes(code).doit(); /* isn't this redundant, at least slightly,
					     with what SplitHandlers is doing? */
	
	AssignCalc ac=code.getAssigns();
	IntraproceduralMustAliasCalc lmac=code.getLocalMustAlias();
	PredsCalc pc=code.getPreds();
	ResultTypeCalc rtc=code.getResultType();
	
        ArrayList< Var > oldVars=new ArrayList< Var >(code.vars());
        
	HashMap< Instruction, Var > varMapping=
	    new HashMap< Instruction, Var >();
	for (Instruction i : ac.assigns()) {
	    // FIXME: if rtc.get() yields UNRESOLVED but the LHS is resolved, use the
	    // LHS's type instead.  this change should be made anywhere that we do
	    // type propagation.  the idea: use Exectype.isAsUsefulAs().
	    varMapping.put(i,code.addVar(rtc.get(i)));
	}
	
	if (Global.verbosity>=3) {
	    Global.log.println("varMapping = "+varMapping);
	}
	
	if (Global.verbosity>=3) {
	    Global.log.println("Code right before making SSA but after the pre-processing:");
	    CodeDumper.dump(code,Global.log);
	}
	
	HashMap< Header, ArrayList< SimpleInst > > ipsilons=
	    new HashMap< Header, ArrayList< SimpleInst > >();

	for (Header h : code.headers()) {
	    try {
		IntraproceduralMustAliasCalc.LocalCalc lc=
		    lmac.new LocalCalc(h);
		
		// do two things:
		// 1) ensure that we have Ipsilon functions inserted for Phi functions,
		//    so that we never encounter a Phi without encountering an Ipsilon
		//    just before it.
		// 2) replace variables.
		
		for (Operation o : h.operations()) {
		    assert o.opcode()!=OpCode.Ipsilon;
		    if (o.opcode()==OpCode.Ipsilon) {
			// code added due to processing of another block.  skip it
			// so that the lmac doesn't get confused.
			continue;
		    }
		    
		    Var newLhs=null;
		    Arg[] newRhs;

		    if (o.opcode()==OpCode.Phi) {
			SimpleInst si=(SimpleInst)o;
			
			// this is what SplitHandlers is for.  it ensures this
			// property.  well, actually, SplitUnhappyBlocks also
			// helps with this.
			assert pc.exceptionHandlerPredecessors(h).isEmpty();
			
			Var myVar=varMapping.get(o);
			for (Header h2 : pc.normalPredecessors(h)) {
			    TwoWayMap< Var, Instruction > atTail=lmac.aliasAtTail(h2);
			    ArrayList< SimpleInst > list=ipsilons.get(h2);
			    if (list==null) {
				ipsilons.put(h2,list=new ArrayList< SimpleInst >());
			    }
			    list.add(
				new SimpleInst(
				    h2.getFooter().di(),
				    OpCode.Ipsilon,
				    myVar,
				    new Arg[]{
					varMapping.get(
					    atTail.valueForKey(si.rhs(0)))
				    }));
			}
			
			newLhs=myVar;
			newRhs=new Arg[]{myVar};
		    } else {
			if (o.rhs.length==0) {
			    newRhs=Arg.EMPTY;
			} else {
			    newRhs=new Arg[o.rhs.length];
			    System.arraycopy(o.rhs,0,
					     newRhs,0,
					     o.rhs.length);
			}
			
			for (int i=0;i<o.rhs.length;++i) {
			    if (o.rhs[i] instanceof Var) {
				newRhs[i]=varMapping.get(lc.forceSource(o.rhs[i]));
			    }
			}
			
			if (o instanceof Instruction) {
			    Instruction i=(Instruction)o;
			    if (i.lhs()==Var.VOID) {
				newLhs=Var.VOID;
			    } else {
				newLhs=varMapping.get(i);
			    }
			}
		    }
		    
		    lc.update(o);
		    
		    o.rhs=newRhs;
		    if (o instanceof Instruction) {
			((Instruction)o).setLhs(newLhs);
		    }
		}
	    } catch (Throwable e) {
		throw new CompilerException("While processing "+h,e);
	    }
	}
	
	for (Map.Entry< Header, ArrayList< SimpleInst > > e : ipsilons.entrySet()) {
	    Header h = e.getKey();
	    ArrayList< SimpleInst > list = e.getValue();
	    
	    // I have no idea if this is right.  the problem is what if you had an ipsilon
	    // list like:
	    //
	    // a = Ipsilon(b)
	    // b = Ipsilon(a)
	    //
	    // obviously, we could make that work.  But that seems like it would confuse
	    // SimpleProp as well.  I'll just hope this doesn't come up.
	    
	    Collections.sort(
		list,
		new Comparator< SimpleInst >(){
		    public int compare(SimpleInst a, SimpleInst b) {
			assert a.opcode()==OpCode.Ipsilon;
			assert b.opcode()==OpCode.Ipsilon;
			if (a.lhs()==b.rhs(0)) {
			    return 1;
			}
			if (b.lhs()==a.rhs(0)) {
			    return -1;
			}
			return 0;
		    }
		});
	    
	    for (SimpleInst si : list) {
		h.append(si);
	    }
	}
        
        for (Var v : oldVars) {
            code.delVar(v);
        }
	
	code.killAllAnalyses();
	setChangedCode();
	code.isSSA=true;
	
	if (Global.verbosity>=3) {
	    Global.log.println("Code right after making SSA but before simplification:");
	    code.getPreds(); // forces the dumper to get preds
	    CodeDumper.dump(code,Global.log);
	}
	
	PhaseFixpoint.simplify(code); // undoes most of what SplitHandlers did
    }
}

