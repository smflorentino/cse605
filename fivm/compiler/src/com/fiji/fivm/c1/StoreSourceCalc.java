/*
 * StoreSourceCalc.java
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

/**
 * Calculates, for a given operation that may escape an object, the operand that is the
 * "source" of the object being escaped.
 */
public class StoreSourceCalc {
    private StoreSourceCalc() {}
    
    public static Arg get(Operation o) {
	switch (o.opcode()) {
	case WeakCASField:   return o.rhs(2);
	case PutField:       return o.rhs(1);
	case WeakCASElement: return o.rhs(3);
	case ArrayStore:     return o.rhs(2);
	case PutStatic:      return o.rhs(0);
	case WeakCASStatic:  return o.rhs(1);
	case Return:
	case Throw:          return o.rhs(0);
	default: throw new Error("not a store operation: "+o);
	}
    }
    
    public static Arg getCASComparand(Operation o) {
	switch (o.opcode()) {
	case WeakCASField:   return o.rhs(1);
	case WeakCASElement: return o.rhs(2);
	case WeakCASStatic:  return o.rhs(0);
	default: throw new Error("not a CAS operation: "+o);
	}
    }
    
    public static boolean isTraced(Operation o) {
	switch (o.opcode()) {
	case PutStatic:
	case WeakCASStatic:
	case PutField:
	case WeakCASField:   return ((VisibleField)((HeapAccessInst)o).field()).isTraced();
	case WeakCASElement: return o.rhs(3).type().mayBeObject();
	case ArrayStore:     return o.rhs(2).type().mayBeObject();
	case Return:
	case Throw:          return o.rhs().length>0 && o.rhs(0).type().mayBeObject();
	default:             return false;
	}
    }
    
    public static boolean isReference(Operation o) {
	switch (o.opcode()) {
	case PutStatic:
	case WeakCASStatic:
	case PutField:
	case WeakCASField:   return ((VisibleField)((HeapAccessInst)o).field()).isReference();
	case WeakCASElement: return o.rhs(3).type().mayBeObject();
	case ArrayStore:     return o.rhs(2).type().mayBeObject();
	case Return:
	case Throw:          return o.rhs().length>0 && o.rhs(0).type().mayBeObject();
	default:             return false;
	}
    }
    
    public static boolean isBarriered(Operation o) {
	switch (o.opcode()) {
	case PutStatic:
	case WeakCASStatic:
	case PutField:
	case WeakCASField:   return ((VisibleField)((HeapAccessInst)o).field()).isBarriered();
	case WeakCASElement: return o.rhs(3).type().mayBeObject();
	case ArrayStore:     return o.rhs(2).type().mayBeObject();
	case Return:
	case Throw:          return o.rhs().length>0 && o.rhs(0).type().mayBeObject();
	default:             return false;
	}
    }
}

