/*
 * Scale.java
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

public enum Scale {
    ONE, TWO, FOUR, EIGHT;

    public static Scale mult(int multiple) {
        switch (multiple) {
        case 1: return ONE;
        case 2: return TWO;
        case 4: return FOUR;
        case 8: return EIGHT;
        default: throw new CompilerException("bad multiple: "+multiple);
        }
    }
    
    public static boolean hasMult(int multiple) {
        switch (multiple) {
        case 1:
        case 2:
        case 4:
        case 8: return true;
        default: return false;
        }
    }

    public static Scale mult(long multiple) {
        switch ((int)multiple) {
        case 1: return ONE;
        case 2: return TWO;
        case 4: return FOUR;
        case 8: return EIGHT;
        default: throw new CompilerException("bad multiple: "+multiple);
        }
    }
    
    public static boolean hasMult(long multiple) {
        if ((multiple>>>32)==0) {
            switch ((int)multiple) {
            case 1:
            case 2:
            case 4:
            case 8: return true;
            default: return false;
            }
        } else {
            return false;
        }
    }

    public static Scale shift(int shift) {
        switch (shift) {
        case 0: return ONE;
        case 1: return TWO;
        case 2: return FOUR;
        case 3: return EIGHT;
        default: throw new CompilerException("bad shift: "+shift);
        }
    }
    
    public static boolean hasShift(int shift) {
        switch (shift) {
        case 0:
        case 1:
        case 2:
        case 3: return true;
        default: return false;
        }
    }
    
    public int mult() {
        switch (this) {
        case ONE: return 1;
        case TWO: return 2;
        case FOUR: return 4;
        case EIGHT: return 8;
        default: throw new CompilerException("bad scale");
        }
    }
    
    public int shift() {
        switch (this) {
        case ONE: return 0;
        case TWO: return 1;
        case FOUR: return 2;
        case EIGHT: return 3;
        default: throw new CompilerException("bad scale");
        }
    }
}

