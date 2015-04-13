/*
 * FlowLog.java
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

import com.fiji.fivm.Settings;

/* FIXME: Ugly class name, but we use this along side r1.FlowLog, so ... */
/**
 * Helper functions for flow logging from the compiler.
 */
class FlowLogging {
    public static final GodGivenFunction log =
	GodGivenFunction.make("fivmr_FlowLog_log",
			      Basetype.VOID,
			      new Basetype[]{
				  Basetype.POINTER,
				  Basetype.SHORT,
				  Basetype.SHORT,
				  Basetype.LONG
			      },
			      SafepointMode.CANNOT_SAFEPOINT);

    public static final GodGivenFunction log_fat =
	GodGivenFunction.make("fivmr_FlowLog_log_fat",
			      Basetype.VOID,
			      new Basetype[]{
				  Basetype.POINTER,
				  Basetype.SHORT,
				  Basetype.SHORT,
				  Basetype.LONG,
				  Basetype.LONG
			      },
			      SafepointMode.CANNOT_SAFEPOINT);

    public static final GodGivenFunction log32 =
	GodGivenFunction.make("fivmr_FlowLog_log32",
			      Basetype.VOID,
			      new Basetype[]{
				  Basetype.POINTER,
				  Basetype.SHORT,
				  Basetype.SHORT,
				  Basetype.INT,
				  Basetype.INT
			      },
			      SafepointMode.CANNOT_SAFEPOINT);

    public static final GodGivenFunction log32_fat =
	GodGivenFunction.make("fivmr_FlowLog_log32_fat",
			      Basetype.VOID,
			      new Basetype[]{
				  Basetype.POINTER,
				  Basetype.SHORT,
				  Basetype.SHORT,
				  Basetype.INT,
				  Basetype.INT,
				  Basetype.INT,
				  Basetype.INT
			      },
			      SafepointMode.CANNOT_SAFEPOINT);

    /**
     * Determines whether a call from outer to inner should be logged
     * if inner is inlined into outer.
     *
     * This will return true if inner would have had flow logging
     * statements inserted within it were it a normal method call, or if
     * all of the following are true:
     *
     * <ul>
     *     <li>outer should be flow logged</li>
     *     <li>outer is calling inner directly (i.e., not through
     *         another inlined method)</li>
     *     <li>inner's context is not flow logged</li>
     *     <li>inner is not explicitly marked @NoFlowLog</li>
     * </ul>
     */
    public static boolean shouldLogCall(Code outer, Code inner) {
	if (Settings.FLOW_LOGGING_NO_SMALL_INLINES &&
	    inner.getMustInline().shouldInlineForSize()) {
	    return false;
	}
	final CodeOrigin oo = outer.origin();
	final VisibleMethod vm = inner.origin().origin();
	final CodeOrigin caller = inner.root().di().caller().origin();
	return inner.shouldFlowLog() ||
	    (outer.shouldFlowLog() && oo == caller && !vm.noFlowLog());
    }

    /**
     * Determines whether a call to di().origin() should be logged by
     * caller.
     *
     * The rules are very similar to shouldLogCall(Code, Code), but
     * inlined method calls must be handled here.
     *
     * FIXME: Some inlined calls are logged here that should not be
     * logged.
     */
    public static boolean shouldLogCall(Code caller, DebugInfo di) {
	final VisibleMethod inner = di.origin().origin();
	final VisibleMethod outer;
	if (di.caller() != null) {
	    outer = di.caller().origin().origin();
	} else {
	    outer = caller.origin().origin();
	}
	return outer.shouldFlowLog() && !inner.shouldFlowLog() &&
	    !inner.noFlowLog();
    }
}
