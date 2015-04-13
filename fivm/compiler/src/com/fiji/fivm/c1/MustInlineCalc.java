/*
 * MustInlineCalc.java
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

/**
 * Calculates if a piece of Code must be inlined.  Code for which shouldDefinitelyInline()
 * returns true should always be inlined no matter what.
 */
public class MustInlineCalc {
    double cost;
    boolean mustInline;
    
    public MustInlineCalc(Code c) {
	if (c.origin().origin().inlineMode==InlineMode.MUST_INLINE) {
	    mustInline=true;
	}
	
	double opCost=0.0; // amount of arithmetic
	double branchCost=0.0; // amount of branching
	double callCost=0.0; // amount of calling and "heavy stuff" like side effects
	boolean foundBad=false;
	
	for (Header h : c.headers()) {
	    for (Operation o : h.operations()) {
		switch (o.opcode()) {
		case Return:
		case RawReturn:
		case GetDebugID:
		case GetArg:
		    // free
		    break;
		case Mov:
		case Ipsilon:
		case Phi:
		case GetCFieldOffset:
		case GetCTypeSize:
		case PutCVar:
		case GetCVar:
		case GetCVarAddress:
                case CastNonZero:
		    opCost+=0.2;
		    break;
		case Cast:
                case CastExact: {
		    Basetype src=o.rhs(0).type().effectiveBasetype();
		    Basetype trg=((TypeInst)o).getType().effectiveBasetype();
		    if (src==trg) {
			opCost+=0.2;
		    } else if (src.isFloat!=trg.isFloat) {
			opCost+=2.0;
		    } else if (src.isFloat) {
			opCost+=1.5;
		    } else if (src.bytes!=trg.bytes) {
			opCost+=1.0;
		    } else {
			opCost+=0.2; /* it's an int-to-ptr cast on 32-bit or a
					long-to-ptr cast on 64-bit */
		    }
		    break;
		}
		case IntToPointerZeroFill:
		    if (Global.pointerSize==4) {
			opCost+=0.2;
		    } else {
			opCost++;
		    }
		    break;
		case Instanceof:
		    opCost+=2.0;
		    branchCost+=0.5;
		    break;
		case TypeCheck:
		case ArrayCheckStore:
		    opCost+=2.0;
		    branchCost+=0.9;
		    break;
		case CheckDivisor:
		case ArrayBoundsCheck:
		    branchCost+=0.9;
		    break;
		case NullCheck:
		case CheckInit:
		    // for these we assume that they may get eliminated from inlining
		    branchCost+=0.3;
		    break;
		case Add:
		case Sub:
		case Mul:
		case Div:
		case Mod:
		case Neg:
		case Shl:
		case Shr:
		case Ushr:
		case And:
		case Or:
		case Xor:
		case CompareG:
		case CompareL:
		case LessThan:
		case ULessThan:
		case LessThanEq:
		case ULessThanEq:
		case Eq:
                case Neq:
		case Not:
                case Boolify:
		case BitNot:
		    switch (o.rhs(0).type().effectiveBasetype()) {
		    case BOOLEAN:
		    case BYTE:
		    case CHAR:
		    case SHORT:
		    case INT:
		    case POINTER:
		    case OBJECT:
			opCost++;
			break;
		    case LONG:
			if (Global.pointerSize==4) {
			    opCost+=2;
			} else {
			    opCost++;
			}
			break;
		    case FLOAT:
		    case DOUBLE:
			opCost+=1.5;
			break;
		    default: throw new Error(
			"bad basetype for op: "+o);
		    }
		    break;
		case Call:
		case CallIndirect:
		    callCost+=0.4;
		    break;
		case InvokeStatic:
		case Invoke:
		case New:
		case NewArray:
		    callCost+=0.5;
		    break;
		case InvokeDynamic:
		    callCost+=0.6;
		    break;
		case PutField:
		case GetField:
		case PutStatic:
		case GetStatic:
		case Load:
		case Store:
		case StrongLoadCAS:
		case StrongCAS:
		case StrongVoidCAS:
		case WeakCAS:
		case PutCField:
		case GetCField:
		case GetCFieldAddress:
		case GetTypeData:
		case GetTypeDataForObject:
		case Phantom:
		case NotReached:
		case Jump:
		    opCost++;
		    break;
		case BranchNonZero:
		case BranchZero:
		    branchCost++;
		    break;
		case Switch:
		    branchCost+=((Switch)o).numCases()+1;
		    break;
                case AwesomeJump:
                    branchCost+=1.5;
                    break;
		default:
		    // NOTE: this currently includes patch points...
		    foundBad=true;
		    break;
		}
		if (c.getSideEffects().get(o)) {
		    callCost+=0.1;
		}
		if (c.getSafepoints().get(o)) {
		    callCost+=0.4;
		}
	    }
	}

	if (foundBad) {
	    cost=Double.POSITIVE_INFINITY;
	} else {
	    cost=Math.sqrt(
		Math.pow(opCost/10.0,2.0) +
		Math.pow(branchCost/1.5,2.0) +
		Math.pow(callCost/1.2,2.0));
	}
    }
    
    /**
     * Returns the cost of inlining.  If the cost is 1, then the inlined code will
     * be just as expensive as the original call - so for <=1 we should definitely
     * inline.
     */
    public double getCost() { return cost; }
    
    public boolean shouldInlineForSize() { return getCost()<=1; }
    
    public boolean shouldInline() { return getCost()<=1.0 || mustInline; }
}

