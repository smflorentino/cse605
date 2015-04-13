/*
 * BoolResultCalc.java
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

public final class BoolResultCalc {
    Code c;
    
    public BoolResultCalc(Code c) { this.c=c; }
    
    public boolean get(Instruction i) {
        switch (i.opcode()) {
        case GetArg:
            return c.param(((ArgInst)i).getIdx())==Type.BOOLEAN;
        case GetCArg:
            return c.cparam(((ArgInst)i).getIdx())==Basetype.BOOLEAN;
        case Not:
        case Boolify:
        case Eq:
        case Neq:
        case LessThan:
        case LessThanEq:
        case ULessThan:
        case ULessThanEq:
        case Instanceof:
        case WeakCASStatic:
        case WeakCASField:
        case WeakCASElement:
        case StrongCAS:
        case WeakCAS:
        case PoundDefined:
            return true;
        case GetStatic:
        case GetField:
        case ArrayLoad:
            return ((HeapAccessInst)i).fieldType()==Type.BOOLEAN;
        case Load:
            return ((MemoryAccessInst)i).getType()==Type.BOOLEAN;
        case Invoke:
        case InvokeStatic:
        case InvokeDynamic:
        case InvokeResolved:
        case InvokeIndirect:
            return ((MTSInstable)i).signature().getResult()==Type.BOOLEAN;
        case Call:
            return ((Function)((CFieldInst)i).field()).getResult()==Basetype.BOOLEAN;
        case CallIndirect:
            return ((CallIndirectInst)i).result()==Basetype.BOOLEAN;
        case GetCField:
        case GetCVar:
            return ((CFieldInst)i).field().getType()==Basetype.BOOLEAN;
        default:
            return false;
        }
    }
    
    public boolean get(Arg a) {
        Instruction i=a.inst();
        if (i!=null) {
            return get(i);
        } else if (a instanceof Arg.IntConst) {
            long value=((Arg.IntConst)a).longValue();
            return value==0
                || value==1;
        } else {
            return false;
        }
    }
}

