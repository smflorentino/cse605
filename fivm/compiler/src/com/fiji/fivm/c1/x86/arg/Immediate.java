/*
 * Immediate.java
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

package com.fiji.fivm.c1.x86.arg;

import java.util.*;

import com.fiji.fivm.c1.x86.Kind;
import com.fiji.fivm.c1.x86.LOp;
import com.fiji.fivm.c1.x86.LType;

public class Immediate extends LArgBase {
    long value;
    
    private Immediate(long value) {
        this.value=value;
    }
    
    public static Immediate make(long value) {
        if (value>=-1 && value<=8) {
            return make((int)value);
        } else {
            return new Immediate(value);
        }
    }
    
    public static Immediate make(int value) {
        switch (value) {
        case -1: return _M1;
        case 0: return _0;
        case 1: return _1;
        case 2: return _2;
        case 4: return _4;
        case 8: return _8;
        default: return new Immediate(value);
        }
    }
    
    public boolean compatibleWith(LType type) {
        return type.isInt();
    }
    
    public boolean compatibleWith(Kind kind) {
        return kind.isInt();
    }
    
    public Kind kind() {
        return Kind.INT;
    }
    
    public boolean lvalue() {
        return false;
    }
    
    public boolean immediate() {
        return true;
    }
    
    public boolean is32Bit() {
        return (value>>>32)==0;
    }
    
    public long value() {
        return value;
    }
    
    public int value32() {
        return (int)value;
    }
    
    public boolean equals(int value) {
        return value==this.value;
    }
    
    public boolean doesNotEqual(int value) {
        return value!=this.value;
    }
    
    public int hashCode() {
        return (int) ((value>>32) ^ value);
    }
    
    public boolean equals(Object other_) {
        if (this==other_) return true;
        if (!(other_ instanceof Immediate)) return false;
        Immediate other=(Immediate)other_;
        return value==other.value;
    }
    
    public String toString() {
        return "$"+value;
    }
    
    public String asm(LType useType,LType memType) {
        return toString();
    }
    
    public String callAsm(LType useType,LType memType) {
        return ""+value;
    }

    public void spill(HashMap< Tmp, LArg > spills,LOp op,int i) {
    }

    public LArg map(HashMap< ?, ? extends LArg > map) {
        LArg rep=map.get(this);
        if (rep==null) {
            return this;
        } else {
            return rep;
        }
    }

    public static long offset(long value,int bytes) {
        if (bytes==0) {
            return value;
        } else if (bytes>0) {
            if (bytes>=8) {
                return 0;
            } else {
                return value>>(bytes<<3);
            }
        } else {
            if (bytes<=-8) {
                return 0;
            } else {
                return value<<(bytes<<3);
            }
        }
    }
    
    public Immediate offset(int bytes) {
        long newValue=offset(value,bytes);
        if (newValue==value) {
            return this;
        } else {
            return Immediate.make(newValue);
        }
    }
    
    public static Immediate _M1=new Immediate(-1);
    public static Immediate _0=new Immediate(0);
    public static Immediate _1=new Immediate(1);
    public static Immediate _2=new Immediate(2);
    public static Immediate _4=new Immediate(4);
    public static Immediate _8=new Immediate(8);
}

