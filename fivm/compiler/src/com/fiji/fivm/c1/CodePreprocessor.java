/*
 * CodePreprocessor.java
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

public class CodePreprocessor {
    private CodePreprocessor() {}
    
    // FIXME: get rid of verbosityBoost
    
    public static void process(Map< VisibleMethod, Code > codes) {
	process(codes,0);
    }
    
    public static void process(Collection< Code > codes) {
	process(codes,0);
    }
    
    public static void process(Code code) {
	process(code,0);
    }
    
    public static void process(Map< VisibleMethod, Code > codes,
			       int verbosityBoost) {
	process(codes.values(),verbosityBoost);
    }
    
    public static void process(Collection< Code > codes,
			       int verbosityBoost) {
	for (Code code : codes) {
	    process(code,verbosityBoost);
	}
    }
    
    public static void process(Code code,
			       int verbosityBoost) {
	int oldVerbosity=Global.verbosity;
	if (verbosityBoost>Global.verbosity) Global.verbosity=verbosityBoost;
	try {
	    if (Global.coverage) new InstrumentForCoverage(code).doit();
	    new DecorateSynchronizedMethods(code).doit();
	    new CanonicalizeArgs(code).doit();
            new NormalizePatchPoints(code).doit();
	    new LowerFeatures1(code).doit();
	    PhaseFixpoint.simplify(code);
	    new MakeSSA(code).doit();
            
            if (new InterceptPoundDefines(code).doitAndCheckChanged() &&
                Global.pdr!=null) {
		new ProcessPoundDefines(code).doit();
                PhaseFixpoint.fullSimplify(code);
	    }
	    
            // NOTE that this occurs before any intraprocedural CFA.  this is important,
            // because it means that any C structures used by the code will make it into
            // the CTypesystemReferences repo even if the code that used it gets killed.
            // other code takes advantage of this property, so don't change it.
	    new InterceptIntrinsicBeforeChecks(code).doit();
	    new InterceptIntrinsicFields(code).doit();

	    new SquirtChecks(code).doit();
            PhaseFixpoint.simplify(code);
            code.checksInserted=true;
	
	    new InterceptIntrinsicAfterChecks(code).doit();
            new CastEater(code).doit();
	    
	    new OptCheckInit(code).doit(); /* InterceptIntrinsic may throw in new
					      checks, so we kill 'em here */
	    PhaseFixpoint.simplify(code); /* need this in case there is dead code
                                             that gets revealed only after check
                                             insertion. */
            new OptCompare(code).doit();
            new SimpleProp(code).doit();
	    new IntraproceduralCFA(code,CFAMode.CFA_FOR_TRANSFORMATION).doit();
            new OptPeephole(code).doit();
	    PhaseFixpoint.simplify(code);
            if (Global.rce) {
                PhaseFixpoint.rce(code);
            } else {
                new SimpleProp(code).doit();
            }
	    new OptString(code).doit();
            new SimplifyConditionals(code).doit();
	    PhaseFixpoint.simplify(code);
	} finally {
	    Global.verbosity=oldVerbosity;
	}
    }
}


