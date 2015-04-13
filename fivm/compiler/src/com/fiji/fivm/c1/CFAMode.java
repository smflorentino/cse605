/*
 * CFAMode.java
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
 * The mode that a control flow analysis (CFA) should run in.  A CFA can either
 * be run for the purpose of transformation (i.e. optimizing the code) or for
 * verification (i.e. proving that the code is type-safe).  The latter is
 * interesting: optimization makes type checking hard because the code can only
 * be proven type-safe if temporal properties are accounted for.  Thus, post
 * optimization the only way to prove type-safety is to perform a CFA that can
 * uncover the temporal properties.
 */
public enum CFAMode {
    /**
     * Use the control flow analysis (CFA) to transform the code.  This mode
     * will result in the code being optimized.
     */
    CFA_FOR_TRANSFORMATION,
    
    /**
     * Use the control flow analysis (CFA) to prove type safety.  After the
     * analysis runs it will set the code.verifiabilityMode to either
     * CONTAINS_UNSAFE (if it was not possible to prove type safety) or to
     * VERIFIABLE (if a proof of type safety was constructed).
     */
    CFA_FOR_VERIFICATION;
    
    public boolean transform() {
        return this==CFA_FOR_TRANSFORMATION;
    }
    
    public boolean verify() {
        return this==CFA_FOR_VERIFICATION;
    }
}

