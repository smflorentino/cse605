/*
 * Kind.java
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

package com.fiji.fivm.c1.x86;

import com.fiji.fivm.c1.*;

/** A kind just describes if a type is integer or floating point.  It is also appropriate
 * for describing the types of temporaries and registers. */
public enum Kind {
    INT, FLOAT;
    
    public boolean isInt() {
        return this==INT;
    }
    
    public boolean isFloat() {
        return this==FLOAT;
    }
    
    public boolean compatibleWith(LType type) {
        if (isInt()) {
            switch (type) {
            case Half:
            case Long:
            case Quad: return true;
            default: return false;
            }
        } else {
            switch (type) {
            case Single:
            case Double: return true;
            default: return false;
            }
        }
    }
    
    public static Kind from(Basetype basetype) {
        if (basetype.isInteger) {
            return INT;
        } else if (basetype.isFloat) {
            return FLOAT;
        } else {
            throw new CompilerException("Bad type: "+basetype);
        }
    }
    
    public static Kind from(Type t) {
        return from(t.effectiveBasetype());
    }
    
    public static Kind from(Exectype t) {
        return from(t.effectiveBasetype());
    }
}

