/*
 * OffMem.java
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
import com.fiji.fivm.c1.x86.LOpCode;
import com.fiji.fivm.c1.x86.LType;

public class OffMem extends LArgBase {
    int offset;
    LArg base;
    
    public OffMem(int offset,LArg base) {
        this.offset=offset;
        this.base=base;
        
        assert base.variable();
        assert base.compatibleWith(LType.ptr());
    }
    
    public int offset() {
        return offset;
    }
    
    public LArg base() {
        return base;
    }
    
    public boolean memory() {
        return true;
    }
    
    public String toString() {
        return ""+offset+"("+base.toString()+")";
    }
    
    public String asm(LType useType,LType memType) {
        return ""+offset+"("+base.asm(memType,memType)+")";
    }
    
    public int hashCode() {
        return offset + base.hashCode();
    }
    
    public boolean equals(Object other_) {
        if (this==other_) return true;
        if (!(other_ instanceof OffMem)) return false;
        OffMem other=(OffMem)other_;
        return offset==other.offset
            && base.equals(other.base);
    }
    
    public int nUseOnDefVars() {
        return 1;
    }
    public LArg useOnDefVar(int i) {
        return base;
    }
    
    public int nUseOnUseVars() {
        return 1;
    }
    public LArg useOnUseVar(int i) {
        return base;
    }
    
    public LArg offset(int offset) {
        return new OffMem(this.offset+offset,base);
    }
    
    public void spill(HashMap< Tmp, LArg > spills,LOp op,int i) {
        LArg ss=spills.get(base);
        if (ss!=null) {
            Tmp tmp=op.head().code().addNoSpillTmp(Kind.INT);
            op.prepend(
                new LOp(
                    LOpCode.Mov,op.memType(),
                    new LArg[]{
                        ss,
                        tmp
                    }));
            op.rhs()[i]=new OffMem(offset,tmp);
        }
    }

    public LArg map(HashMap< ?, ? extends LArg > map) {
        LArg rep=map.get(this);
        if (rep==null) {
            rep=map.get(base);
            if (rep==null) {
                return this;
            } else {
                return new OffMem(offset,rep);
            }
        } else {
            return rep;
        }
    }
    
    public boolean stack() {
        return base.stackPointer();
    }
}

