/*
 * PollcheckMode.java
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
 * Describes the way pollchecks work in the given method.  This enumeration
 * has no effect on how the compiler generates calls to the method (that's
 * what SafepointMode is for), but it does affect two things:
 * <ul>
 * <li>Sanity checking.  Methods marked EXPLICIT_ONLY can only call methods
 *     that are also EXPLICIT_ONLY or else either use SafepointMode.NONE, or
 *     have checking turned off.</li>
 * <li>Pollcheck squirting.  Pollcheck squirting is turned off if you use
 *     EXPLICIT_ONLY.</li>
 * </ul>
 */
public enum PollcheckMode {
    /** The compiler may arbitrarily insert pollchecks and safepoints into
	the given method or function. */
    IMPLICIT_POLLCHECKS,
	
    /** The only pollchecks and safepoints are those invoked directly and presumably
	inteded by the programmer. */
    EXPLICIT_POLLCHECKS_ONLY;
	
    // FIXME: instead of having to use ALLOW_UNSAFE with EXPLICIT_ONLY, we should just
    // have a third mode in here.
    
    public final static PollcheckMode[] table;
    
    static {
        table=PollcheckMode.class.getEnumConstants();
        assert table!=null;
        for (int i=0;i<table.length;++i) {
            assert table[i].ordinal()==i;
        }
    }
}

