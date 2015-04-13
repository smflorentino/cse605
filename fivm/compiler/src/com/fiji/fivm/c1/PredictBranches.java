/*
 * PredictBranches.java
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

public class PredictBranches extends CodePhase {
    public PredictBranches(Code c) { super(c); }
    
    public void visitCode() {
	assert code.isSSA();
		
	// step 1: a block that contains a call to a @NoInline is unlikely to
	// execute.  as well, a block that ends in a throw, unreached, or patchpoint
	// is unlikely to execute.
	
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		if ((i instanceof MethodInst &&
                     ((MethodInst)i).method().noInline()) ||
                    i.opcode()==OpCode.PatchPoint) {
		    if (Global.verbosity>=3) {
			Global.log.println(
			    "Marking "+h+" unlikely because of "+i);
		    }
		    h.setProbability(HeaderProbability.UNLIKELY_TO_EXECUTE);
		}
	    }
	    switch (h.getFooter().opcode()) {
	    case Throw:
            case Rethrow:
	    case NotReached:
	    case PatchPointFooter:
		h.setProbability(HeaderProbability.UNLIKELY_TO_EXECUTE);
		break;
	    default:
		break;
	    }
	}
        
	// step 2: a branch whose source is LikelyZero or LikelyNonZero,
	// or that has an unlikely target, should have its prediction
	// adjusted accordingly
	
	for (Header h : code.headers()) {
	    if (h.getFooter() instanceof Branch) {
		Branch b=(Branch)h.getFooter();
		
		// before doing anything else figure out if the branch is subject to a strong prediction.
		// if it already is then we can move on; and if its source is a SemanticallyLikely op
		// then we adjust the prediction as such and move on.
		
		if (b.strength()==PredictionStrength.SEMANTIC_PREDICTION) continue;
		
                Instruction src=b.rhs(0).inst();
                if (src!=null) {
                    switch (b.opcode()) {
                    case BranchNonZero:
                        switch (src.opcode()) {
                        case SemanticallyLikelyZero:
                            b.predict(BranchPrediction.PREDICTING_FALSE);
                            b.setStrength(PredictionStrength.SEMANTIC_PREDICTION);
                            break;
                        case SemanticallyLikelyNonZero:
                            b.predict(BranchPrediction.PREDICTING_TRUE);
                            b.setStrength(PredictionStrength.SEMANTIC_PREDICTION);
                            break;
                        default: break;
                        }
                        break;
                    case BranchZero:
                        switch (src.opcode()) {
                        case SemanticallyLikelyZero:
                            b.predict(BranchPrediction.PREDICTING_TRUE);
                            b.setStrength(PredictionStrength.SEMANTIC_PREDICTION);
                            break;
                        case SemanticallyLikelyNonZero:
                            b.predict(BranchPrediction.PREDICTING_FALSE);
                            b.setStrength(PredictionStrength.SEMANTIC_PREDICTION);
                            break;
                        default: break;
                        }
                        break;
                    default: throw new Error();
                    }
                }
		
                if (b.strength()==PredictionStrength.SEMANTIC_PREDICTION) continue;
                
		boolean trueLikely=true;
		boolean falseLikely=true;
		
		if (Global.verbosity>=3) {
		    Global.log.println(
			"Original prediction: "+b.prediction);
		}

		switch (b.prediction()) {
		case PREDICTING_TRUE:
		    falseLikely=false;
		    break;
		case PREDICTING_FALSE:
		    trueLikely=false;
		    break;
		default:
		    break;
		}
		
		// check source of branch
		if (src!=null) {
		    switch (b.opcode()) {
		    case BranchNonZero:
			switch (src.opcode()) {
			case LikelyZero:
			    if (Global.verbosity>=3) {
				Global.log.println(
				    "Predicting "+h+" will jump to default due to LikelyZero");
			    }
			    trueLikely=false;
			    break;
			case LikelyNonZero:
			    if (Global.verbosity>=3) {
				Global.log.println(
				    "Predicting "+h+" will jump to target due to LikelyNonZero");
			    }
			    falseLikely=false;
			    break;
			default:
			    break;
			}
			break;
		    case BranchZero:
			switch (src.opcode()) {
			case LikelyZero:
			    if (Global.verbosity>=3) {
				Global.log.println(
				    "Predicting "+h+" will jump to target due to LikelyZero");
			    }
			    falseLikely=false;
			    break;
			case LikelyNonZero:
			    if (Global.verbosity>=3) {
				Global.log.println(
				    "Predicting "+h+" will jump to default due to LikelyNonZero");
			    }
			    trueLikely=false;
			    break;
			default:
			    break;
			}
			break;
		    default: throw new Error();
		    }
		}
		
		// check targets of branch
		if (b.target().unlikely() && b.defaultSuccessor().unlikely()) {
		    // special case - both targets are unlikely, so we set
		    // the prediction to UNKNOWN and don't issue a warning
		    b.predict(BranchPrediction.NO_PREDICTION);
		} else {
		    if (b.target().unlikely()) {
			if (Global.verbosity>=3) {
			    Global.log.println(
				"Predicting "+h+" will jump to default due to unlikely target");
			}
			trueLikely=false;
		    }
		    if (b.defaultSuccessor().unlikely()) {
			if (Global.verbosity>=3) {
			    Global.log.println(
				"Predicting "+h+" will jump to target due to unlikely default");
			}
			falseLikely=false;
		    }
		    
		    if (trueLikely) {
			if (falseLikely) {
			    b.predict(BranchPrediction.NO_PREDICTION);
			} else {
			    b.predict(BranchPrediction.PREDICTING_TRUE);
			}
		    } else {
			if (falseLikely) {
			    b.predict(BranchPrediction.PREDICTING_FALSE);
			} else {
			    if (Global.verbosity>=3) {
				Global.log.println(
				    "Branch prediction conflict in "+h+" in "+code);
			    }
			    b.predict(BranchPrediction.NO_PREDICTION);
			}
		    }
		}
	    }
	}
        
        // what about ensuring that blocks that are only reachable via an unlikely
        // branch edge are marked UNLIKELY_TO_EXECUTE?  and that any blocks that they
        // dominate are also UNLIKELY_TO_EXECUTE?
    }
}


