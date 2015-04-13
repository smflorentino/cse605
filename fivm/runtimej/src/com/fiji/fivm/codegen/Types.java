/*
 * Types.java
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

package com.fiji.fivm.codegen;

import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.Inline;
import com.fiji.fivm.r1.NoInline;
import com.fiji.fivm.r1.NoReturn;

public final class Types {
    private Types() {}

    @Inline
    public static char lub(char a,char b) {
        if (a==b) {
            return a;
        } else {
            switch (a) {
            case 0:
                return b;
            case '-':
                return '-';
            case 'L':
            case '[':
            case 'R':
                switch (b) {
                case 'L':
                case '[':
                case 'R':
                    return 'L';
                default:
                    return '-';
                }
            default:
                if (b==0) {
                    return a;
                } else {
                    return '-';
                }
            }
        }
    }

    @NoInline
    @NoReturn
    static private void badType(char type) {
        throw new CodeGenException("Unrecognized basetype: "+type);
    }

    @Inline
    public static char toExec(char type) {
        switch (type) {
        case 'Z':
        case 'B':
        case 'C':
        case 'S':
        case 'I':
            return 'I';
        case 'J':
            return 'J';
        case 'F':
            return 'F';
        case 'D':
            return 'D';
        case 'L':
        case '[':
        case 'R':
            return 'L';
        case 'V':
            return 'V';
        default:
            badType(type);
            return 0; // make javac happy
        }
    }
    
    @Inline
    public static int bytes(char type) {
        switch (type) {
        case 'Z':
        case 'B':
            return 1;
        case 'S':
        case 'C':
            return 2;
        case 'I':
        case 'F':
            return 4;
        case 'J':
        case 'D':
            return 8;
        case 'L':
        case '[':
        case 'R':
            return Pointer.size();
        case 'V':
            return 0;
        default:
            badType(type);
            return 0; // make javac happy
        }
    }

    @Inline
    public static int logBytes(char type) {
        switch (type) {
        case 'Z':
        case 'B':
            return 0;
        case 'S':
        case 'C':
            return 1;
        case 'I':
        case 'F':
            return 2;
        case 'J':
        case 'D':
            return 3;
        case 'L':
        case '[':
        case 'R':
            if (Pointer.size()==4) {
                return 2;
            } else {
                return 3;
            }
        default:
            badType(type);
            return 0; // make javac happy
        }
    }

    @Inline
    public static int cells(char type) {
        switch (type) {
        case 'J':
        case 'D':
            return 2;
        case 'V':
            return 0;
        default:
            return 1;
        }
    }
    
    @Inline
    public static boolean ref(char type) {
        switch (type) {
        case 'L':
        case '[':
        case 'R':
            return true;
        default:
            return false;
        }
    }
}

