/*
 * CodeProcessor.java
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
import com.fiji.fivm.Settings;

public class CodeProcessor {
    private CodeProcessor() {}
    
    public static Collection< Code > process(Collection< Code > codes) {
	for (Code code : codes) {
	    process(code);
	}
	return codes;
    }
    
    public static Code process(Code code) {
        if (Settings.SIMPLE_COMP) {
            new CheckIfVerifiable(code).doit();
        } else {
            code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                     "using full optimizing compiler");
        }
        
        new PredictBranches(code).doit();
        
        if (Global.doInline && !Settings.SIMPLE_COMP /* HACK - should enable inlining in SIMPLE_COMP eventually */) {
            code=InlineFixpoint.doit(
                code,
                new MustInlineFilter(),
                Global.omBlocksMax,
                -1);
            if (!code.mustOptForSize()) {
                code=InlineFixpoint.doit(
                    code,
                    new CallerSizeLimitInlineFilter(Global.maxInlineCallerBlocks),
                    Global.omBlocksMax,
                    Global.maxInlineFixpoint);
            }
            code=InlineFixpoint.doit(
                code,
                new MustInlineFilter(),
                Global.omBlocksMax,
                -1);
        }
        
        new OptCheckInit(code).doit();

        PhaseFixpoint.simplify(code);
        
        if (Settings.SIMPLE_COMP) {
            new CheckIfVerifiable(code).doit();
        }
        
        if (Settings.SIMPLE_COMP && code.isPotentiallyVerifiable()) {
            // do nothing else at this point
        } else {
            new OptABC(code).doit();
            new OptAlgebra(code).doit();

            new KillSSA(code).doit();
            PhaseFixpoint.simplify(code);
            
            if (!code.mustOptForSize()) {
                new UnrollLoops(code,UnrollMode.PEEL_LOOPS).doit();
                new UnrollLoops(code,UnrollMode.UNROLL_LOOPS).doit();
                PhaseFixpoint.simplify(code);
            }

            new SetHeaderFrequency(code).doit();

            new NanoTracing(code).doit();
            
            // AFTER THIS POINT: natural loops are boned.  so don't do anything
            // that relies on that shit.
            
            // not sure if this is the best place for this optimization, but oh well.
            new CompareProp(code).doit();

            new OptCheckInit(code).doit();
            PhaseFixpoint.simplify(code);
        
            new MakeSSA(code).doit();
        
            // optimize the unrolled & peeled loops
            if (Global.rce) {
                // RCE reveals inlining opportunities, which we should catch here.
                // ... and inlining also runs RCE if RCE is enabled.
                code=InlineFixpoint.doit(
                    code,
                    new MustInlineFilter(),
                    Global.omBlocksMax,
                    -1);
            } else {
                new IntraproceduralCFA(code,CFAMode.CFA_FOR_TRANSFORMATION).doit();
                PhaseFixpoint.fullSimplify(code); // need peephole to get rid of dead fields
            }
            new OptABC(code).doit();
            new OptCheckInit(code).doit();
        
            int numblocks=code.headers().size();
            
            if (!Global.noPollcheck) new SquirtPollcheck(code).doit();
            code.pollchecksInserted=true;
            new RemoveOp(code,OpCode.PollcheckFence).doit();
        
            if (!Settings.GC_BLACK_STACK &&
                (Settings.CMRGC || Settings.HFGC) &&
                Settings.OPT_CM_STORE_BARRIERS) {
                
                // this optimization is still somehow borked.  and it doesn't
                // appear to produce any speed-ups.  so ... not worth it.
                new OptGreyStackCMStoreBarriers(code).doit();
            }

            new LowerAllocation(code).doit();
            new LowerMonitors(code).doit();
            
            assert code.headers.size()<=numblocks;
            
            boolean mustOptForSize=code.mustOptForSize();
            
            int blowup=new EstimateOMCodeBlowup(code).getAddlBlocks();
            
            if (Global.verbosity>=3) {
                Global.log.println("size = "+code.headers.size()+", blowup = "+blowup);
            }
            
            if (blowup>0 &&
                code.headers.size() + blowup > Global.omBlocksMax) {
                if (Global.verbosity>=1) {
                    Global.log.println("forcing size-opt due to size = "+code.headers.size()+", blowup = "+blowup+" for "+code);
                }
                mustOptForSize=true;
            }
            
            // guard this with a check to see if the cost of inserting these
            // out-of-line calls (which cause exception guards) is lower than
            // the cost of the usual representational lowering.
            
            if ((mustOptForSize &&
                 Global.om!=ObjectModel.CONTIGUOUS) ||
                Settings.INTERCEPT_ALL_OBJ_ACCESSES) {
                new LowerCompactObjectAccesses(code).doit();
            }
            
            new LowerBarriers(code).doit();
            
            if (Global.doInline &&
                !mustOptForSize) {
                code=InlineFixpoint.doit(
                    code,
                    new MustInlineFilter(),
                    Global.omBlocksMax,
                    -1);
            }

	    // Insert FlowLogging after all inlining is complete
	    if (Settings.FLOW_LOGGING) new SquirtFlowLogging(code).doit();
        }
	    
        return code;
    }
}

