/*
 * Branch.java
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
import java.nio.ByteBuffer;

public final class Branch extends Control {
    
    Header target; // where to branch to on true
    BranchPrediction prediction;
    PredictionStrength strength;
    
    public Branch(DebugInfo di,
		  OpCode opcode,Arg[] rhs,
		  Header next,Header target) {
	super(di,opcode,rhs,next);
	this.target=target;
	this.prediction=BranchPrediction.NO_PREDICTION;
	this.strength=PredictionStrength.HEURISTIC_PREDICTION;
    }
    
    public Branch(DebugInfo di,
		  OpCode opcode,Arg[] rhs,
		  Header next,Header target,
		  BranchPrediction prediction) {
	this(di,opcode,rhs,next,target);
	predict(prediction);
    }
    
    public Branch(DebugInfo di,
                  OpCode opcode,Arg[] rhs,
                  Header next,Header target,
                  BranchPrediction prediction,
                  PredictionStrength strength) {
        this(di,opcode,rhs,next,target,prediction);
        this.strength=strength;
    }

    /**
     * The jump target if the branch condition is true.
     */
    public Header target() { return target; }
    
    public Header nonZeroTarget() {
	switch (opcode) {
	case BranchNonZero: return target();
	case BranchZero: return defaultSuccessor();
	default: throw new Error();
	}
    }
    
    public Header zeroTarget() {
	switch (opcode) {
	case BranchZero: return target();
	case BranchNonZero: return defaultSuccessor();
	default: throw new Error();
	}
    }
    
    public void predict(BranchPrediction prediction) {
        if (strength!=PredictionStrength.SEMANTIC_PREDICTION) {
            this.prediction=prediction;
        }
    }
    
    public BranchPrediction prediction() { return prediction; }
    
    public PredictionStrength strength() { return strength; }
    
    public void setStrength(PredictionStrength strength) {
        this.strength=strength;
    }

    public Iterable< Header > successors() {
	return new Iterable< Header >() {
	    public Iterator< Header > iterator() {
		return new Iterator< Header >() {
		    int i=0;
		    public Header next() {
			Header result;
			switch (i) {
			case 0:
			    result=(Header)next;
			    break;
			case 1:
			    result=target;
			    break;
			default:
			    throw new NoSuchElementException();
			}
			i++;
			return result;
		    }
		    public boolean hasNext() {
			return i<2;
		    }
		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    public Iterable< Header > likelySuccessors() {
        switch (prediction) {
        case NO_PREDICTION: return successors();
        case PREDICTING_TRUE: return Util.singleIterable(target());
        case PREDICTING_FALSE: return Util.singleIterable(defaultSuccessor());
        default: throw new Error(""+prediction);
        }
    }
    
    public <T> T accept(Visitor<T> v) {
	return v.visit(this);
    }
    
    int getNioSize() {
        return super.getNioSize()+4+4+4+4;
    }
    
    void writeTo(NioContext ctx,
                 ByteBuffer buffer) {
        super.writeTo(ctx,buffer);
        buffer.putInt(ctx.nodeCodes.codeFor(next));
        buffer.putInt(ctx.nodeCodes.codeFor(target));
        buffer.putInt(prediction.ordinal());
        buffer.putInt(strength.ordinal());
    }
}


