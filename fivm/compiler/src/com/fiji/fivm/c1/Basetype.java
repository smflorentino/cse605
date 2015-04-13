/*
 * Basetype.java
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

public enum Basetype implements LocationKey {
    VOID {
        {
            bytes=0;
            cells=0;
            descriptor='V';
            toString="void";
        }
    },
    BOOLEAN {
        {
            bytes=1;
            cells=1;
            descriptor='Z';
            toString="boolean";
            constWrap="INT8_C";
            isValue=false; // huh?
        }
    },
    BYTE {
        {
            bytes=1;
            cells=1;
            descriptor='B';
            toString="byte";
            constWrap="INT8_C";
            isValue=true;
            isJavaInteger=true;
            isInteger=true;
            isNumber=true;
        }
    },
    CHAR {
        {
            bytes=2;
            cells=1;
            descriptor='C';
            toString="char";
            constWrap="UINT16_C";
            isValue=true;
            isJavaInteger=true;
            isInteger=true;
            isNumber=true;
        }
    },
    SHORT {
        {
            bytes=2;
            cells=1;
            descriptor='S';
            toString="short";
            constWrap="INT16_C";
            isValue=true;
            isJavaInteger=true;
            isInteger=true;
            isNumber=true;
        }
    },
    INT {
        {
            bytes=4;
            cells=1;
            descriptor='I';
            toString="int";
            constWrap="INT32_C";
            isValue=true;
            isJavaInteger=true;
            isInteger=true;
            isNumber=true;
        }
    },
    LONG {
        {
            bytes=8;
            cells=2;
            descriptor='J';
            toString="long";
            constWrap="INT64_C";
            isValue=true;
            isJavaInteger=true;
            isInteger=true;
            isNumber=true;
        }
    },
    FLOAT {
        {
            bytes=4;
            cells=1;
            descriptor='F';
            toString="float";
            constWrap="";
            isValue=true;
            isFloat=true;
            isNumber=true;
        }
    },
    DOUBLE {
        {
            bytes=8;
            cells=2;
            descriptor='D';
            toString="double";
            constWrap="";
            isValue=true;
            isFloat=true;
            isNumber=true;
        }
    },
    OBJECT {
        {
            bytes=Global.pointerSize;
            cells=1;
            descriptor='L';
            isReference=true;
            isValue=true;
            toString="object";
            constWrap=ptrConstWrap();
            safeForC=false;
        }
    },
    NULL {
        {
            isInternal=true;
            bytes=Global.pointerSize;
            cells=1;
            descriptor='L';
            isReference=true;
            isValue=true;
            toString="nulltype";
            constWrap=ptrConstWrap();
            safeForC=false;
            isBottomish=true;
        }
    },
    POINTER {
        {
            isInternal=true;
            bytes=Global.pointerSize;
            cells=1;
            descriptor='P';
            isReference=true;
            isPointer=true;
            isNumber=true;
            isInteger=true;
            isValue=true;
            toString="pointer";
            constWrap=ptrConstWrap();
            isUnboxedType=true;
        }
        public VisibleClass getUnboxedClass() { return Global.root().pointerClass; }
    },
    VM_FCPTR {
        {
            isInternal=true;
            bytes=Global.pointerSize;
            cells=1;
            descriptor='f';
            isPointer=true;
            isReference=true;
            isNumber=true;
            isInteger=true;
            isValue=true;
            toString="vm.FCPtr";
            constWrap=ptrConstWrap();
            isUnboxedType=true;
        }
        public VisibleClass getUnboxedClass() { return Global.root().fcPtrClass; }
    },
    TOP {
        {
            isInternal=true;
            descriptor='T';
            toString="top";
            safeForC=false;
            isUsableType=false;
        }
    },
    BOTTOM {
        {
            isInternal=true;
            descriptor='B';
            toString="bottom";
            safeForC=false;
            isUsableType=false;
            isBottomish=true;
        }
    },
    /** NIL is for cases where we would rather not supply an argument.  The only
        case for using it is in PatchPoints. */
    NIL {
        {
            isInternal=true;
            descriptor='n';
            toString="nil";
            safeForC=false;
            isUsableType=false;
            isBottomish=true;
        }
    };

    public boolean isInternal                   = false;
    public boolean isBottomish                  = false;
    public boolean isUsableType                 = true;
    public int bytes;
    public int cells;
    public boolean isJavaInteger                = false;
    public boolean isInteger                    = false;
    public boolean isFloat                      = false;
    public boolean isNumber                     = false;
    public boolean isPointer                    = false;
    public boolean isReference                  = false;
    public boolean isValue                      = false;
    public boolean safeForC                     = true;
    public Basetype pointerifyObject            = this;
    public Basetype preciseIntType              = this;
    public String toString;
    public CType cType;
    public CType cTypeForCall;
    public CType signedCType;
    public CType unsignedCType;
    public String constWrap;
    public char descriptor;
    public boolean isUnboxedType                = false;
    public Type asType;
    public Exectype asExectype;
    public Exectype asExectypeInternal;
    
    public String toString() { return toString; }
    public VisibleClass getUnboxedClass() { return null; }
    
    public Arg makeZero() {
        switch (this) {
        case VOID:
            return null;
        case OBJECT:
        case NULL:
            return Arg.NULL;
        case POINTER:
        case VM_FCPTR:
            return PointerConst.make(0);
        case FLOAT:
            return FloatConst.make(0.0f);
        case DOUBLE:
            return DoubleConst.make(0.0);
        case LONG:
            return LongConst.make(0);
        default:
            return IntConst.make(0);
        }
    }
    
    public Arg makeConst(int value) {
        switch (this) {
        case BOOLEAN:
        case BYTE:
        case SHORT:
        case CHAR:
        case INT:
            return IntConst.make(value);
        case POINTER:
        case VM_FCPTR:
            return PointerConst.make(value);
        case LONG:
            return LongConst.make(value);
        case FLOAT:
            return FloatConst.make(value);
        case DOUBLE:
            return DoubleConst.make(value);
        default:
            throw new CompilerException("bad type: "+this);
        }
    }
    
    public static Basetype[] EMPTY=new Basetype[0];
    
    private static String ptrConstWrap() {
        switch (Global.pointerSize) {
        case 4: return "(uintptr_t)UINT32_C";
        case 8: return "(uintptr_t)UINT64_C";
        default: throw new Error("bad pointer size");
        }
    }
    
    public static Basetype fromChar(char c) {
        switch (c) {
        case 'Z': return BOOLEAN;
        case 'B': return BYTE;
        case 'C': return CHAR;
        case 'S': return SHORT;
        case 'I': return INT;
        case 'J': return LONG;
        case 'F': return FLOAT;
        case 'D': return DOUBLE;
        case 'P': return POINTER;
        case 'f': return VM_FCPTR;
        case 'V': return VOID;
        case 'L':
        case '[': return OBJECT;
        case 'N': return NULL;
        case 'T': return TOP;
        case 'n': return NIL;
        case 'b': return BOTTOM;
        default: throw new BadBytecode("Unrecognized basetype character: "+c);
        }
    }
    
    static boolean inited;
    
    static {
        assert Global.pointerSize>=0;
        
        OBJECT.pointerifyObject=POINTER;
        NULL.pointerifyObject=POINTER;
        
        if (Global.pointerSize==4) {
            POINTER.preciseIntType=INT;
            VM_FCPTR.preciseIntType=INT;
        } else {
            POINTER.preciseIntType=LONG;
            VM_FCPTR.preciseIntType=LONG;
        }
        
        if (CTypesystemReferences.inited) {
            CTypesystemReferences.linkBasetypes();
        }
        
        if (Global.verbosity>=1) {
            Global.log.println("Basetype initialized.");
        }
        inited=true;
    }
    
    public boolean isAssignableFrom(Basetype other) {
        switch (this) {
        case BOOLEAN:
            return other==BOOLEAN;
        case BYTE:
            switch (other) {
            case BOOLEAN:
            case BYTE: return true;
            default: return false;
            }
        case CHAR:
            switch (other) {
            case BOOLEAN:
            case CHAR: return true;
            default: return false;
            }
        case SHORT:
            switch (other) {
            case BOOLEAN:
            case BYTE:
            case SHORT: return true;
            default: return false;
            }
        case INT:
            switch (other) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHAR:
            case INT: return true;
            case POINTER:
            case VM_FCPTR: return Global.pointerSize==4;
            default: return false;
            }
        case LONG:
            switch (other) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case POINTER:
            case VM_FCPTR:
            case LONG: return true;
            default: return false;
            }
        case POINTER:
        case VM_FCPTR:
            switch (other) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case POINTER:
            case VM_FCPTR: return true;
            case LONG: return Global.pointerSize==8;
            default: return false;
            }
        case OBJECT:
            switch (other) {
            case OBJECT:
            case NULL: return true;
            default: return false;
            }
        case TOP:
            return true;
        default:
            return other==this;
        }
    }
};
    
