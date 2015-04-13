/*
 * OptABC.java
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

public class OptABC extends CodePhase {
    public OptABC(Code c) { super(c); }

    SimpleLivenessCalc slc;
    
    HashMap< Header, HashMap< Var, AbsValue > > atHead;
    Worklist worklist;

    public void visitCode() {
	slc=code.getSimpleLiveness();
	
	if (Global.verbosity>=3) {
	    Global.log.println("Commencing OptABC");
	}
	
	atHead=new HashMap< Header, HashMap< Var, AbsValue > >();
	worklist=new Worklist();
	
	worklist.push(code.root());
	atHead.put(code.root(),new HashMap< Var, AbsValue >());
	
	// be conservative for exception handlers
	for (ExceptionHandler eh : code.handlers()) {
	    worklist.push(eh.target());
	    HashMap< Var, AbsValue > frame=new HashMap< Var, AbsValue >();
	    for (Var v : slc.liveAtHead(eh.target())) {
		frame.put(v,new AbsValue());
	    }
	    atHead.put(eh.target(),frame);
	}
	
	while (!worklist.empty()) {
	    Header h=worklist.pop();
	    HashMap< Var, AbsValue > frame=atHead.get(h);
	    assert frame!=null : h;
	    frame=Util.copy(frame);
	    
	    if (Global.verbosity>=3) {
		Global.log.println("At top of "+h+" with "+frame);
	    }
	    
	    for (Instruction i : h.instructions()) {
		process(i,frame);
	    }
	    
	    Footer f=h.getFooter();
	    if (f instanceof Branch &&
		f.rhs(0) instanceof Var) {
		Branch b=(Branch)f;
		Instruction lti=f.rhs(0).inst();
		
		HashMap< Var, AbsValue > trueFrame=Util.copy(frame);
		HashMap< Var, AbsValue > falseFrame=Util.copy(frame);

		// do this only if it's an array length
		if (lti.opcode()==OpCode.LessThan) {
		    if (lti.rhs(0) instanceof Var &&
			frame.containsKey(lti.rhs(0)) &&
			isArrayLength(lti.rhs(1))) {
			trueFrame.put(
                            (Var)lti.rhs(0),
                            trueFrame.get(lti.rhs(0)).asWithinBounds(
                                arrayForArrayLength(lti.rhs(1))));
		    }
		    
		    if (lti.rhs(0).equals(0) &&
			lti.rhs(1) instanceof Var &&
			frame.containsKey(lti.rhs(1))) {
			trueFrame.put(
                            (Var)lti.rhs(1),
                            trueFrame.get(lti.rhs(1)).asSign(Sign.POSITIVE));
		    }
		    
		    if (lti.rhs(1).equals(0) &&
			lti.rhs(0) instanceof Var &&
			frame.containsKey(lti.rhs(0))) {
			falseFrame.put(
                            (Var)lti.rhs(0),
                            falseFrame.get(lti.rhs(0)).asSign(Sign.NON_NEGATIVE));
		    }
		}

		handleEdge(h,b.nonZeroTarget(),trueFrame);
		handleEdge(h,b.zeroTarget(),falseFrame);
	    } else {
		handleAllEdges(h,frame);
	    }
	}
	
	// perform the transformation
	for (Header h : code.headers()) {
	    HashMap< Var, AbsValue > frame=Util.copy(atHead.get(h));
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case ArrayBoundsCheck: {
		    AbsValue idxVal=frame.get(i.rhs(1));
		    if (Global.verbosity>=3) {
			Global.log.println("At "+i+" with idxVal = "+idxVal);
		    }
		    if (idxVal!=null &&
			idxVal.sign.nonNegative() &&
			idxVal.withinBounds==i.rhs(0)) {
			if (Global.verbosity>=2) {
			    Global.log.println("removing ABC: "+i);
			}
			i.remove();
			setChangedCode();
		    }
		    break;
		}
		default: break;
		}
		process(i,frame);
	    }
	}
	
	if (Global.verbosity>=3) {
	    Global.log.println("Finished OptABC");
	}

	if (changedCode()) {
	    code.killIntraBlockAnalyses();
	}
    }
    
    boolean isArrayLength(Arg v) {
	if (v!=null) {
	    Instruction i=v.inst();
	    return i!=null && i.opcode()==OpCode.ArrayLength;
	}
	return false;
    }
    
    Arg arrayForArrayLength(Arg v) {
	if (v!=null) {
	    Instruction i=v.inst();
	    if (i!=null && i.opcode()==OpCode.ArrayLength) {
		return i.rhs(0);
	    }
	}
	return null;
    }
    
    void process(Instruction i,
		 HashMap< Var, AbsValue > frame) {
	switch (i.opcode()) {
	case Add: {
	    AbsValue left=frame.get(i.rhs(0));
	    if (left!=null &&
		i.rhs(1).equals(1) &&
		left.sign.nonNegative() &&
		left.withinBounds!=null) {
		frame.put(i.lhs(),left.asWithinBounds(null));
	    } else if (left!=null &&
		       i.rhs(1).equals(-1) &&
		       left.sign.positive()) {
		frame.put(i.lhs(),left.asSign(Sign.NON_NEGATIVE));
	    } else if (i.rhs(1).equals(-1) &&
		       left.arrayLength!=null &&
		       left.sign.positive()) {
		frame.put(i.lhs(),new AbsValue(Sign.NON_NEGATIVE,left.arrayLength,null));
	    } else if (i.rhs(1).equals(-1) &&
		       left.arrayLength!=null) {
		frame.put(i.lhs(),new AbsValue(Sign.TOP,left.arrayLength,null));
	    } else if (left!=null &&
		       i.rhs(1).equals(-1) &&
		       left.sign.nonNegative()) {
		frame.put(i.lhs(),left.asSign(Sign.TOP));
	    } else {
		frame.put(i.lhs(),new AbsValue());
	    }
	    break;
	}
	case Mov:
	case Ipsilon:
	case Phi: {
	    AbsValue val=frame.get(i.rhs(0));
	    if (val!=null) {
		frame.put(i.lhs(),val);
	    } else if (i.rhs(0) instanceof IntConst) {
		IntConst ic=(IntConst)i.rhs(0);
		if (ic.value()>0) {
		    frame.put(i.lhs(),new AbsValue(Sign.POSITIVE,null,null));
		} else if (ic.value()>=0) {
		    frame.put(i.lhs(),new AbsValue(Sign.NON_NEGATIVE,null,null));
		} else {
		    frame.put(i.lhs(),new AbsValue());
		}
	    } else {
		frame.put(i.lhs(),new AbsValue());
	    }
	    break;
	}
	case ArrayLength: {
	    frame.put(i.lhs(),new AbsValue(Sign.NON_NEGATIVE,null,i.rhs(0)));
	    break;
	}
	case ArrayBoundsCheck: {
	    if (i.rhs(1) instanceof Var) {
		frame.put((Var)i.rhs(1),new AbsValue(Sign.NON_NEGATIVE,i.rhs(0),null));
	    }
	    break;
	}
	case LessThan:
	case ULessThan:
	case Eq:
        case Neq:
        case Not:
        case Boolify:
	case Instanceof:
	case WeakCASStatic:
	case WeakCASField:
	case WeakCASElement:
	case PoundDefined:
	case GetAllocSpace:
	case GetDebugID: {
	    frame.put(i.lhs(),new AbsValue(Sign.NON_NEGATIVE,null,null));
	    break;
	}
	default:
	    frame.put(i.lhs(),new AbsValue());
	}
    }
    
    void handleEdge(Header h,Header h2,
		    HashMap< Var, AbsValue > frame) {
	if (Global.verbosity>=3) {
	    Global.log.println("From "+h+" to "+h2+" propagating: "+frame);
	}
	HashMap< Var, AbsValue > h2Frame=atHead.get(h2);
	boolean changed=false;
	if (h2Frame==null) {
	    atHead.put(h2,Util.copy(frame,slc.liveAtHead(h2)));
	    changed=true;
	} else {
	    for (Var v : slc.liveAtHead(h2)) {
		AbsValue oldVal=h2Frame.get(v);
		AbsValue newVal=oldVal.merge(frame.get(v));
		if (newVal!=oldVal) {
		    h2Frame.put(v,newVal);
		    changed=true;
		}
	    }
	}
	if (changed) {
	    worklist.push(h2);
	}
    }
    
    void handleAllEdges(Header h,
			HashMap< Var, AbsValue > frame) {
	for (Header h2 : h.normalSuccessors()) {
	    handleEdge(h,h2,frame);
	}
    }

    static enum Sign {
	TOP,
	    
	NON_NEGATIVE {
	    public Sign merge(Sign other) {
		switch (other) {
		case TOP: return TOP;
		default: return this;
		}
	    }
	    public boolean nonNegative() {
		return true;
	    }
	},

	POSITIVE {
	    public Sign merge(Sign other) {
		switch (other) {
		case NON_NEGATIVE: return NON_NEGATIVE;
		case TOP: return TOP;
		default: return this;
		}
	    }
	    public boolean nonNegative() {
		return true;
	    }
	    public boolean positive() {
		return true;
	    }
	};
	
	public Sign merge(Sign other) {
	    return this;
	}
	
	public boolean nonNegative() {
	    return false;
	}
	
	public boolean positive() {
	    return false;
	}
    }

    static class AbsValue {
	Sign sign;
	Arg withinBounds;
	Arg arrayLength;
	
	AbsValue() {
	    sign=Sign.TOP;
	    withinBounds=null;
	    arrayLength=null;
	}
	
	AbsValue(Sign sign,
		 Arg withinBounds,
		 Arg arrayLength) {
	    this.sign=sign;
	    this.withinBounds=withinBounds;
	    this.arrayLength=arrayLength;
	}
	
	AbsValue asSign(Sign sign) {
	    return new AbsValue(sign,withinBounds,arrayLength);
	}
	
	AbsValue asWithinBounds(Arg v) {
	    return new AbsValue(sign,v,null);
	}
	
	AbsValue asArrayLength(Arg v) {
	    return new AbsValue(sign,null,v);
	}
	
	AbsValue merge(AbsValue other) {
	    AbsValue result=new AbsValue(sign.merge(other.sign),
					 withinBounds==other.withinBounds?withinBounds:null,
					 arrayLength==other.arrayLength?arrayLength:null);
	    if (equals(result)) {
		return this;
	    } else {
		return result;
	    }
	}
	
	public int hashCode() {
	    return withinBounds.hashCode()+arrayLength.hashCode()+sign.hashCode();
	}
	
	public boolean equals(Object other_) {
	    if (this==other_) return true;
	    if (!(other_ instanceof AbsValue)) return false;
	    AbsValue other=(AbsValue)other_;
	    return sign==other.sign
		&& withinBounds==other.withinBounds
		&& arrayLength==other.arrayLength;
	}
	
	public String toString() {
	    StringBuilder result=new StringBuilder("[");
	    result.append(sign);
	    if (withinBounds!=null) {
		result.append(", ");
		result.append("WithinBounds(");
		result.append(withinBounds);
		result.append(")");
	    }
	    if (arrayLength!=null) {
		result.append(", ");
		result.append("ArrayLength(");
		result.append(arrayLength);
		result.append(")");
	    }
	    result.append("]");
	    return result.toString();
	}
    }
}

