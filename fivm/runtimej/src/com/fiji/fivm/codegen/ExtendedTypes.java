/*
 * ExtendedTypes.java
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

import com.fiji.fivm.r1.Inline;
import com.fiji.fivm.r1.NoInline;

public final class ExtendedTypes {
    private ExtendedTypes() {}

    // codes:
    // 
    // 0-255: chars for explicit type values plus the following:
    //    0   = bottom
    //    '.' = undefined
    //    '-' = top
    //
    // 65536-131071: reserved for pointers and arrays of pointers
    //
    // 131072-196607: references to locals at head of basic block
    //
    // 196608-262143: references to stack at head of basic block
    //
    // 262144-MAX_INT: reserved for references to symbolic types
    //
    // -1: lub error
    //
    // the idea is that you can get the "lind" of code by doing >>>16.
    
    public static final int EXPLICIT_TYPE=0;
    public static final int PTR_TYPE=1;
    public static final int LOCAL_REFERENCE=2;
    public static final int STACK_REFERENCE=3;
    public static final int SYMB_TYPE=4;
    
    @Inline
    public static int codeKind(int code) {
        return code>>>16;
    }
    
    @Inline
    public static boolean isExplicitType(int code) {
        return codeKind(code)==EXPLICIT_TYPE;
    }
    
    @Inline
    public static boolean isLocalRef(int code) {
        return codeKind(code)==LOCAL_REFERENCE;
    }
    
    @Inline
    public static boolean isStackRef(int code) {
        return codeKind(code)==STACK_REFERENCE;
    }
    
    @Inline
    public static boolean isStateRef(int code) {
        return (code>>>17)==1;
    }
    
    @Inline
    public static int validateNonStateRef(int code) {
        if (isStateRef(code)) {
            throw new CodeGenException("did not expect state ref: "+code);
        }
        return code;
    }
    
    @Inline
    public static boolean isPtrType(int code) {
        return codeKind(code)==PTR_TYPE;
    }
    
    @Inline
    public static boolean isSymbType(int code) {
        return codeKind(code)==SYMB_TYPE;
    }
    
    @Inline
    public static boolean isRef(int code) {
        switch (codeKind(code)) {
        case EXPLICIT_TYPE:
            switch (code) {
            case '[':
            case 'L':
            case 'R':
                return true;
            default:
                return false;
            }
        case PTR_TYPE:
        case SYMB_TYPE:
            return true;
        default:
            return false;
        }
    }
    
    @Inline
    public static boolean isNonNull(int code) {
        return code=='R';
    }
    
    @Inline
    public static int getPayload(int code) {
        return code&65535;
    }
    
    @Inline
    public static int newCode(int kind,int payload) {
        return payload|(kind<<16);
    }
    
    @Inline
    public static int newLocalRef(int local) {
        return newCode(LOCAL_REFERENCE,local);
    }
    
    @Inline
    public static int newStackRef(int stack) {
        return newCode(STACK_REFERENCE,stack);
    }
    
    @Inline
    public static int newPtrType(int arrayDepth) {
        return newCode(PTR_TYPE,arrayDepth);
    }
    
    @Inline
    public static int newSymbType(int poolRef) {
        return newCode(SYMB_TYPE,poolRef);
    }
    
    @Inline
    public static int tryLub(int a,int b) {
        if (a==b || b==0) {
            return a;
        }
        if (a==0) {
            return b;
        }
        if (isExplicitType(a) && isExplicitType(b)) {
            return Types.lub((char)a,(char)b);
        }
        return tryLubSlow(a,b);
    }
    
    @Inline
    public static int lub(int a,int b) {
        if (a==b) {
            return a;
        }
        if (isExplicitType(a) && isExplicitType(b)) {
            return Types.lub((char)a,(char)b);
        }
        return lubSlow(a,b);
    }
    
    private static int tryToMakeRef(int b) {
        switch (codeKind(b)) {
        case EXPLICIT_TYPE:
            switch (b) {
            case 'L':
            case '[':
            case 'R':
                return 'L';
            default:
                return '-';
            }
        case PTR_TYPE:
            if (getPayload(b)>0) {
                return 'L';
            } else {
                return '-';
            }
        case SYMB_TYPE:
            return 'L';
        default:
            return -1;
        }
    }
    
    private static int tryToMakeTop(int b) {
        switch (codeKind(b)) {
        case EXPLICIT_TYPE:
        case PTR_TYPE:
        case SYMB_TYPE:
            return '-';
        default:
            return -1;
        }
    }
    
    @NoInline
    private static int tryLubSlow(int a,int b) {
        if (false && isSymbType(a) && isSymbType(b)) {
            // FIXME - implement this part eventually
        } else {
            switch (codeKind(a)) {
            case EXPLICIT_TYPE:
                switch (a) {
                case 'L':
                case '[':
                case 'R':
                    return tryToMakeRef(b);
                default:
                    return tryToMakeTop(b);
                }
            case PTR_TYPE:
                if (getPayload(a)>0) {
                    return tryToMakeRef(b);
                } else {
                    return tryToMakeTop(b);
                }
            case SYMB_TYPE:
                return tryToMakeRef(b);
            default:
                break;
            }
        }
        return -1;
    }
    
    @NoInline
    private static int lubSlow(int a,int b) {
        int result=tryLubSlow(a,b);
        if (result<0) {
            throw new CodeGenException("Failed to lub: "+a+" with "+b);
        }
        return result;
    }
}

