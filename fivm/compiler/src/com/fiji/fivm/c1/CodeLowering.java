/*
 * CodeLowering.java
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

import com.fiji.fivm.*;
import java.util.*;

public class CodeLowering {
    private CodeLowering() {}
    
    // all of these methods returns a *flattened* list of linkables (so you
    // don't have to go and ask for subLinkables)
    
    public static LinkableSet process(Collection< Code > codes) {
	LinkableSet result=new LinkableSet();
	for (Code code : codes) {
	    result.addAll(process(code));
	}
	return result;
    }
    
    public static LinkableSet process(Code code) {
        LinkableSet result=new LinkableSet();
        
        if (Settings.SIMPLE_COMP && code.isPotentiallyVerifiable()) {
            PhaseFixpoint.fullSimplify(code);
            new LowerTrivialChecks(code).doit();
            new LowerGetConstant1(code).doit();
            new LowerGetConstant2(code).doit();
            new RemoveOp(code,OpCode.PollcheckFence).doit();
            new HelpArithmetic(code).doit();
            PhaseFixpoint.simplify(code);
            
            new IntraproceduralCFA(code,CFAMode.CFA_FOR_VERIFICATION).doit();
            
            new KillSSA(code).doit();
            PhaseFixpoint.fullSimplify(code);
            
            code.depthFirstSort();
            SimpleGenerateLocalFunction sglf=new SimpleGenerateLocalFunction(code);
            sglf.doit();
            result.addAll(sglf.localFunction().linkables());
        } else {
            // NOTE: we assume that allocation, barriers, and monitors will
            // not do anything that requires allocation, barriers or monitors.

            // NOTE: from this point on we're kind of C-specific.  If we want to be
            // able to generate good machine code, everything from here on will
            // probably change.

            new SquirtStackHeightCheck(code).doit();
            if (Global.profileStackHeight) new SquirtStackHeightProfiling(code).doit();
            new LowerExceptions1(code).doit();
            // optimize here with SSA because we've introduced Instanceof
            // tests
            PhaseFixpoint.simplify(code);
            if (Global.verbosity>=3) {
                Global.log.println("code has "+code.headers().size()+" blocks");
            }
            new SimpleProp(code).doit();
            new IntraproceduralCFA(code,CFAMode.CFA_FOR_TRANSFORMATION).doit();

            code.typeLowered=true;
            
            if (Global.om==ObjectModel.FRAGMENTED) {
                new JamArrayBoundsChecks(code).doit();
            }
        
            new LowerTrivialChecks(code).doit();
            
            new LowerFeatures2(code).doit();

            // FIXME: coming out of SSA and going back into it here would result
            // in a code size reduction and a speedup.  make this optional for
            // now...
            
            new LowerStackAllocation1(code).doit();

            new LowerObjectRepresentation1Direct(code).doit();
        
            // FIXME really want to do some RCE here...
            // FIXME except that it would be totally wrong.  consider that we
            // have multiple calls to AddressOfField, and a pollcheck between
            // them.  ouch!
            
            switch (Global.om) {
            case CONTIGUOUS:
                new LowerContiguousObjectRepresentation2(code).doit();
                break;
            case FRAGMENTED:
                new LowerFragmentedObjectRepresentation2(code).doit();
                new LowerArrayletRepresentation2(code).doit();
                break;
            default: throw new Error();
            }

            new LowerExceptions2(code).doit();
            new LowerMethodResolution(code).doit();
        
            PhaseFixpoint.simplify(code);
            new SimpleProp(code).doit();
            new IntraproceduralCFA(code,CFAMode.CFA_FOR_TRANSFORMATION).doit();

            new SimpleProp(code).doit();

            PhaseFixpoint.simplify(code);
        
            new PerformRefAlloc(code).doit();
            
            new DecorateForeignCalls(code).doit();

            new LowerThreadStatePassingCallingConvention1(code).doit();

            new LowerStaticFields(code).doit();
            new LowerOneWordHeaderModel(code).doit();

            new LowerExceptions3(code).doit();
            new LowerGetConstant1(code).doit();
            new LowerGetConstant2(code).doit();
            new LowerGetConstant3(code).doit();

            // this needs to happen here because otherwise Pollchecks would be missed in
            // the LeafMethodAna.
            new LowerPollcheck(code).doit();
            
            new SimpleProp(code).doit(); // for good measure
            PhaseFixpoint.simplify(code);

            new LowerStackAllocation2(code).doit();
            new LowerHendersonFrames(code).doit();
            new LowerAllocSpace(code).doit();
            if (Global.verboseRunMethod!=null) {
                new SquirtMethodVerbosity(code).doit();
            }
            new LowerThreadStatePassingCallingConvention2(code).doit();
            new BreakTypes(code).doit();
	    
            new ProcessPoundDefines(code).doit();

            new LowerDebugID(code).doit(); /* lower debug IDs before final optimization;
                                              at least now we would have killed off a
                                              bunch of them. */
            
            if (Global.haveCOffsetsAndSizes) {
                new LowerCTypesAndFields(code).doit();
            }
            if (Global.indirectGodGivens) {
                new IndirectGodGivens(code).doit();
            }

            // FIXME: need more SSA optimizations, including load/store opts, especially
            // ones that obey the ThreadLocalMode of fields.
            PhaseFixpoint.fullSimplify(code);
            new PredictBranches(code).doit();
            new OptAlgebra(code).doit();
            new SimpleProp(code).doit();
            PhaseFixpoint.simplify(code); // FIXME: this is only needed to make CFA happy.
            new IntraproceduralCFA(code,CFAMode.CFA_FOR_TRANSFORMATION).doit();
            new KillCastNonZero(code).doit();
            new KillLikelies(code).doit();
            if (Global.lateRCE) {
                PhaseFixpoint.simplify(code);
                PhaseFixpoint.rce(code);
            } else {
                new SimpleProp(code).doit();
            }
            for (int i=0;i<Global.numForwardPasses;++i) {
                new OptForward(code).doit();
            }
            new KillSSA(code).doit();
            
            new ComparePropLate(code).doit();
            
            // FIXME need a LocalCopyProp and/or LocalConstantProp or more likely both
        
            if (Global.reduceVars) new ReduceVars(code).doit();
            PhaseFixpoint.simplify(code);
            
            // FIXME: this should really be moved into the SSA portion of compilation so
            // that it can take advantage of IntraproceduralCFA.
            new HelpArithmetic(code).doit();
            
            new SimplifyConditionals(code).doit();
            new NanoTracing(code).doit(); // FIXME: this bones the pattern matching.
            
            if (Global.nativeBackend) {
                if (Settings.X86) {
                    new LowerForX86Backend(code).doit();
                } else {
                    throw new CompilerException("Do not have a suitable native backend");
                }
            }
            PhaseFixpoint.simplify(code); // FIXME: only needed for CFA
            new IntraproceduralCFA(code,CFAMode.CFA_FOR_TRANSFORMATION).doit();
            new StrengthReduce(code).doit();
            PhaseFixpoint.fullSimplify(code);
            new RemoveOp(code,OpCode.CompilerFence).doit();
            if (Global.nativeBackend) {
                if (Settings.X86) {
                    com.fiji.fivm.c1.x86.Backend backend=
                        new com.fiji.fivm.c1.x86.Backend(code);
                    backend.doit();
                    result.addAll(backend.localFunction().linkables());
                } else {
                    throw new CompilerException("Do not have a suitable native backend");
                }
            } else {
                if (false) {
                    // FIXME: make this make sense.
                    code.programOrderSort();
                } else {
                    code.depthFirstSortLikely();
                }
                GenerateLocalFunction glf=new GenerateLocalFunction(code);
                glf.doit();
                result.addAll(glf.localFunction().linkables());
            }
        }
        
        code.reportVerifiability();
        
        return result;
    }
}

