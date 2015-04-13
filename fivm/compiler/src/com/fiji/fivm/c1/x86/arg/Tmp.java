/*
 * Tmp.java
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
import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.Kind;
import com.fiji.fivm.c1.x86.LOp;
import com.fiji.fivm.c1.x86.LOpCode;
import com.fiji.fivm.c1.x86.LType;

public class Tmp extends LArgBase {
    public int id;
    int permID;
    Kind kind;
    boolean spillable;
    
    public Tmp(int id,int permID,Kind kind,boolean spillable) {
        assert id>=33;
        this.id=id;
        this.permID=permID;
        this.kind=kind;
        this.spillable=spillable;
    }
    
    public boolean compatibleWith(LType type) {
        return kind.compatibleWith(type);
    }
    
    public int id() {
        return id;
    }
    
    public int permID() {
        return permID;
    }
    
    public Kind kind() {
        return kind;
    }
    
    public boolean variable() {
        return true;
    }
    
    public boolean spillable() {
        return spillable;
    }
    
    public int nUseOnUseVars() {
        return 1;
    }
    public LArg useOnUseVar(int i) {
        return this;
    }
    
    public String toString() {
        return "("+kind+")Tmp["+permID+"]";
    }
    
    public String asm(LType useType,LType memType) {
        throw new CompilerException("Cannot generate assembly code for "+this);
    }

    public void spill(HashMap< Tmp, LArg > spills,LOp op,int i) {
        LArg ss=spills.get(this);
        if (ss!=null) {
            if (op.canBeMemory(i)) {
                op.rhs()[i]=ss;
            } else {
                Tmp tmp=op.head().code().addTmp(kind());
                op.rhs()[i]=tmp;
                
                if (op.form().directUseArg(i)) {
                    op.prepend(
                        new LOp(
                            LOpCode.Mov,op.typeOf(i),
                            new LArg[]{
                                ss,
                                tmp
                            }));
                }
                if (op.form().defArg(i)) {
                    op.append(
                        new LOp(
                            LOpCode.Mov,op.typeOf(i),
                            new LArg[]{
                                tmp,
                                ss
                            }));
                }
            }
        }
    }

    public LArg map(HashMap< ?, ? extends LArg > map) {
        LArg rep=map.get(this);
        if (rep==null) {
            return this;
        } else {
            return rep;
        }
    }
}

