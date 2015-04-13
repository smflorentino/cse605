/*
 * LArg.java
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
import com.fiji.fivm.c1.x86.*;

public interface LArg {
    public static LArg[] EMPTY=new LArg[0];
    
    public boolean compatibleWith(LType type);
    public boolean compatibleWith(Kind kind);
    
    public Kind kind();
    
    public boolean isInt();
    public boolean isFloat();
    
    public boolean lvalue();
    
    public boolean memory();
    
    public boolean stackPointer();
    public boolean stack();
    
    public boolean immediate();
    
    public boolean variable();
    
    public boolean spillable();
    
    // only valid for variables
    public int id();
    
    public LArg copy();
    
    public boolean equals(int value);
    
    public boolean doesNotEqual(int value);
    
    public int nUseOnDefVars();
    public LArg useOnDefVar(int i);
    
    public int nUseOnUseVars();
    public LArg useOnUseVar(int i);
    
    public LArg offset(int offset);
    
    /** Performs spilling.  Replaces itself in the rhs of the op,
        and appends and prepends appropriate code. */
    public void spill(HashMap< Tmp, LArg > spills,LOp op,int i);
    
    /** Perform substitution. */
    public LArg map(HashMap< ?, ? extends LArg > map);
    
    public String asm(LType useType,LType memType);
    
    public String callAsm(LType useType,LType memType);
    
    public Linkable linkable();
}

