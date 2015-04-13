/*
 * LType.java
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

public enum LType {
    Void,
    Byte,
    Half,
    Long,
    Quad,
    Single,
    Double;
    
    public boolean isInt() {
        switch (this) {
        case Byte:
        case Half:
        case Long:
        case Quad: return true;
        default: return false;
        }
    }
    
    public boolean isFloat() {
        switch (this) {
        case Single:
        case Double: return true;
        default: return false;
        }
    }
    
    public Kind kind() {
        if (isInt()) {
            return Kind.INT;
        } else if (this!=Void) {
            return Kind.FLOAT;
        } else {
            throw new CompilerException("bad type");
        }
    }
    
    public int size() {
        switch (this) {
        case Byte: return 1;
        case Half: return 2;
        case Long: return 4;
        case Quad: return 8;
        case Single: return 4;
        case Double: return 8;
        default: throw new CompilerException("bad type");
        }
    }
    
    public long mask() {
        switch (this) {
        case Byte: return 255l;
        case Half: return 65535l;
        case Long: return 4294967295l;
        case Quad: return -1;
        default: throw new CompilerException("bad type for mask: "+this);
        }
    }
    
    public long signExtend(long value) {
        switch (this) {
        case Byte: return (byte)value;
        case Half: return (short)value;
        case Long: return (int)value;
        case Quad: return value;
        default: throw new CompilerException("bad type for mask: "+this);
        }
    }
    
    public boolean mayExec() {
        switch (this) {
        case Byte:
        case Half: return false;
        case Long: 
        case Quad: return true;
        case Single:
        case Double: return true;
        default: throw new CompilerException("bad type");
        }
    }

    public boolean isExec() {
        switch (this) {
        case Byte:
        case Half: return false;
        case Long: return true;
        case Quad: return Global.pointerSize==8;
        case Single:
        case Double: return true;
        default: throw new CompilerException("bad type");
        }
    }
    
    public boolean isLikeAddress() {
        return this==Long || this==ptr();
    }
    
    public static LType ptr() {
        if (Global.pointerSize==4) {
            return Long;
        } else {
            return Quad;
        }
    }
    
    public boolean isPtr() {
        switch (this) {
        case Byte:
        case Half: return false;
        case Long: return Global.pointerSize==4;
        case Quad: return Global.pointerSize==8;
        case Single:
        case Double: return false;
        default: throw new CompilerException("bad type");
        }
    }
    
    public static LType from(Basetype base) {
        switch (base) {
        case VOID: return Void;
        case BOOLEAN:
        case BYTE: return Byte;
        case CHAR:
        case SHORT: return Half;
        case INT: return Long;
        case LONG: return Quad;
        case FLOAT: return Single;
        case DOUBLE: return Double;
        case OBJECT:
        case NULL:
        case POINTER:
        case VM_FCPTR: return ptr();
        case TOP:
        case BOTTOM:
        case NIL: throw new CompilerException("cannot convert "+base);
        default: throw new CompilerException("bad basetype");
        }
    }
    
    public static boolean has(Basetype base) {
        switch (base) {
        case VOID:
        case BOOLEAN:
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case OBJECT:
        case NULL:
        case POINTER:
        case VM_FCPTR: return true;
        case TOP:
        case BOTTOM:
        case NIL: return false;
        default: throw new CompilerException("bad basetype");
        }
    }
    
    public static LType from(TypeImpl< ? > type) {
        return from(type.effectiveBasetype());
    }
    
    public static boolean has(TypeImpl< ? > type) {
        return has(type.effectiveBasetype());
    }
    
    public static LType from(Kind kind,int size) {
        switch (kind) {
        case INT:
            if (size<=1) {
                return Byte;
            } else if (size<=2) {
                return Half;
            } else if (size<=4) {
                return Long;
            } else {
                return Quad;
            }
        case FLOAT:
            if (size<=4) {
                return Single;
            } else {
                return Double;
            }
        default: throw new Error("bad kind: "+kind);
        }
    }
    
    public static LType from(Kind kind) {
        switch (kind) {
        case INT:
            if (Global.pointerSize==4) {
                return Long;
            } else {
                return Quad;
            }
        case FLOAT:
            return Double;
        default: throw new Error("bad kind: "+kind);
        }
    }
    
    public Basetype basetype() {
        switch (this) {
        case Void: return Basetype.VOID;
        case Byte: return Basetype.BYTE;
        case Half: return Basetype.SHORT;
        case Long: return Basetype.INT;
        case Quad: return Basetype.LONG;
        case Single: return Basetype.FLOAT;
        case Double: return Basetype.DOUBLE;
        default: throw new CompilerException("bad basetype");
        }
    }
    
    public String asm() {
        switch (this) {
        case Byte: return "b";
        case Half: return "w";
        case Long: return "l";
        case Quad: return "q";
        case Single: return "s";
        case Double: return "d";
        default: throw new CompilerException("do not have asm suffix for "+this);
        }
    }
}

