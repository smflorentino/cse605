/*
 * Arg.java
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

import java.nio.*;
import java.util.*;

/**
 * A right-hand-side argument to an Operation in the Fiji IR.  Arguments are
 * always non-side-effecting and context-insensitive.  I.e. moving an argument
 * from one part of a Code to another is guaranteed to not change that argument's
 * semantics (though of course if this introduces a new use of a Var you might
 * have other problems).  Arg has a variety of subclasses, many of which are
 * also subclasses of Arg.Singleton.  These include NULL, NIL, TOP, and some
 * special ones like THREAD_STATE and FRAME.  There are also Args for
 * integer and pointer constants (IntConst and PointerConst, respectively).
 * A particularly distinguished kind of argument is Var.  Vars make up the
 * temporaries and local variables of a Code object.
 */
public abstract class Arg {
    
    public abstract Exectype type();
    public Basetype effectiveBasetype() { return type().effectiveBasetype(); }
    
    public boolean isNumber() { return effectiveBasetype().isNumber; }
    public boolean isInteger() { return effectiveBasetype().isInteger; }
    public boolean isFloat() { return effectiveBasetype().isFloat; }
    public boolean isPointer() { return effectiveBasetype().isPointer; }
    public boolean isReference() { return effectiveBasetype().isReference; }
    public boolean isValue() { return effectiveBasetype().isValue; }
    
    public static Arg[] EMPTY = new Arg[0];
    
    public static abstract class Const extends Arg {
        public int rank() {
            return 0;
        }
        
        public abstract Const negate();
        
        public abstract long fiatToLong();
    }
    
    public static abstract class IntConst extends Const {
        public abstract long longValue();
        
        public long fiatToLong() {
            return longValue();
        }
        
        public abstract boolean is32();
        
        public abstract int value32();
        
        public abstract Const makeSimilar(long value);
        
        public abstract int numBits();
    }

    static abstract class Singleton extends Arg {
        static int rankcnt=1;
        
        int rank;
        
        Singleton() {
            synchronized (Singleton.class) {
                rank=rankcnt++;
            }
        }
        
	public boolean structuralEquals(Arg other) {
	    return this==other;
	}
        public int getArgNioSize() {
            return 1;
        }
        
        public int rank() {
            return rank;
        }
    }
    
    // NOTE: it MUST be the case that this is the ONLY non-Var Arg for
    // object types.
    static class Null extends Singleton {
	Null() { assert NULL==null; }
	public Exectype type() { return Exectype.NULL; }
	public String toString() { return "NULL"; }
	public boolean equals(int value) {
	    return value==0;
	}
	public boolean doesNotEqual(int value) {
	    return value!=0;
	}
	public int structuralHashCode() {
	    return 5426427;
	}
        public void writeArgTo(ByteBuffer buffer) {
            buffer.put((byte)1);
        }
    }
    
    public static Null NULL = new Null();
    
    static class Nil extends Singleton {
	Nil() { assert NIL==null; }
	public Exectype type() { return Exectype.NIL; }
	public String toString() { return "NIL"; }
	public boolean equals(int value) {
	    return false;
	}
	public boolean doesNotEqual(int value) {
	    return false;
	}
	public int structuralHashCode() {
	    return 5426421;
	}
        public void writeArgTo(ByteBuffer buffer) {
            buffer.put((byte)11);
        }
    }
    
    public static Nil NIL = new Nil();
    
    static class Top extends Singleton {
	Top() { assert TOP==null; }
	public Exectype type() { return Exectype.TOP; }
	public String toString() { return "TOP"; }
	public int structuralHashCode() {
	    return 967990;
	}
        public void writeArgTo(ByteBuffer buffer) {
            buffer.put((byte)2);
        }
    }
    
    public static Top TOP = new Top();
    
    static class ThreadState extends Singleton {
	ThreadState() { assert THREAD_STATE==null; }
	public Exectype type() { return Exectype.POINTER; }
	public String toString() { return "THREAD_STATE"; }
	public int structuralHashCode() {
	    return 78643289;
	}
        public void writeArgTo(ByteBuffer buffer) {
            buffer.put((byte)3);
        }
    }
    
    public static ThreadState THREAD_STATE = new ThreadState();
    
    static class Frame extends Singleton {
	Frame() { assert FRAME==null; }
	public Exectype type() { return Exectype.POINTER; }
	public String toString() { return "FRAME"; }
	public int structuralHashCode() {
	    return 78643281;
	}
        public void writeArgTo(ByteBuffer buffer) {
            buffer.put((byte)4);
        }
    }
    
    public static Frame FRAME = new Frame();
    
    static class AllocFrame extends Singleton {
	AllocFrame() { assert ALLOC_FRAME==null; }
	public Exectype type() { return Exectype.POINTER; }
	public String toString() { return "ALLOC_FRAME"; }
	public int structuralHashCode() {
	    return 54729827;
	}
        public void writeArgTo(ByteBuffer buffer) {
            buffer.put((byte)5);
        }
    }
    
    public static AllocFrame ALLOC_FRAME = new AllocFrame();
    
    public Instruction inst() {
	return null;
    }
    
    public Header head() {
        return null;
    }
    
    public abstract int rank();
    
    Arg copy() {
	return this;
    }
    
    public boolean equals(int value) {
        if (value==0 && type()==Exectype.NULL) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean doesNotEqual(int value) {
        if (value!=0 && type()==Exectype.NULL) {
            return true;
        } else {
            return false;
        }
    }
    
    public abstract int structuralHashCode();
    public abstract boolean structuralEquals(Arg other);
    
    abstract int getArgNioSize();
    abstract void writeArgTo(ByteBuffer buffer);
    
    static Arg readArgFrom(NioContext ctx,
                           ByteBuffer buffer) {
        switch (buffer.get()) {
        case 0: return ctx.code.getVar(buffer.getInt());
        case 1: return NULL;
        case 2: return TOP;
        case 3: return THREAD_STATE;
        case 4: return FRAME;
        case 5: return ALLOC_FRAME;
        case 6: return com.fiji.fivm.c1.IntConst.make(buffer.getInt());
        case 7: return PointerConst.make(buffer.getLong());
        case 8: return LongConst.make(buffer.getLong());
        case 9: return FloatConst.make(buffer.getFloat());
        case 10: return DoubleConst.make(buffer.getDouble());
        case 11: return NIL;
        default: throw new Error("bad arg type");
        }
    }
    
    public static final HashSet< Arg > EMPTY_SET=new HashSet< Arg >();
}

