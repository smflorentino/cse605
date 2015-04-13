/*
 * LHSMode.java
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
 * Mode for how to handle the left-hand-side of instructions when doing
 * comparisons between instructions.
 */
public enum LHSMode {
    /**
     * In this mode, for two instructions to be equal, the left-hand-sides must
     * be equal.  All of the other parts of the instructions have to be equal as
     * well (arguments, meta-data, etc).  This mode is suitable for determining
     * if one instruction will have exactly the same effect on program execution
     * as another.  It's useful for tail merging, and any other transformation
     * that eliminates "identical" (as opposed to "redundant") code.
     */
    CARE_ABOUT_LHS,

    /**
     * In this mode, the left-hand-sides are ignored for the purposes of instruction
     * equality.  I.e. this mode checks whether the two instructions will produce
     * identical values (to be placed in whatever LHS they have) provided that
     * side-effects are identical.  This mode is appropriate for redundant code
     * elimination, and similar optimizations.  However, this mode alone is
     * insufficient for implementing RCE, since instruction equality tells you
     * nothing about redundancy of side-effects.  Thus you either have to disable
     * RCE for side-effects or implement your own machinery for tracking them.
     */
    DONT_CARE_ABOUT_LHS;
}

