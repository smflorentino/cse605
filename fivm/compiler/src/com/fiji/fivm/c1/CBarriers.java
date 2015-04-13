/*
 * CBarriers.java
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

public class CBarriers {
    private CBarriers() {}
    
    public static GodGivenFunction arrayLoad(Basetype t) {
	switch (t) {
	case BOOLEAN:
	case BYTE: return CTypesystemReferences.byteArrayLoad_barrier;
	case CHAR: return CTypesystemReferences.charArrayLoad_barrier;
	case SHORT: return CTypesystemReferences.shortArrayLoad_barrier;
	case INT: return CTypesystemReferences.intArrayLoad_barrier;
	case LONG: return CTypesystemReferences.longArrayLoad_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerArrayLoad_barrier;
	case FLOAT: return CTypesystemReferences.floatArrayLoad_barrier;
	case DOUBLE: return CTypesystemReferences.doubleArrayLoad_barrier;
	case OBJECT: return CTypesystemReferences.objectArrayLoad_barrier;
	default: throw new Error(""+t);
	}
    }
    
    public static GodGivenFunction arrayLoad(Type t) {
        return arrayLoad(t.effectiveBasetype());
    }
    
    public static GodGivenFunction arrayStore(Basetype t) {
	switch (t) {
	case BOOLEAN:
	case BYTE: return CTypesystemReferences.byteArrayStore_barrier;
	case CHAR: return CTypesystemReferences.charArrayStore_barrier;
	case SHORT: return CTypesystemReferences.shortArrayStore_barrier;
	case INT: return CTypesystemReferences.intArrayStore_barrier;
	case LONG: return CTypesystemReferences.longArrayStore_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerArrayStore_barrier;
	case FLOAT: return CTypesystemReferences.floatArrayStore_barrier;
	case DOUBLE: return CTypesystemReferences.doubleArrayStore_barrier;
	case OBJECT: return CTypesystemReferences.objectArrayStore_barrier;
	default: throw new Error(""+t);
	}
    }
    
    public static GodGivenFunction arrayStore(Type t) {
        return arrayStore(t.effectiveBasetype());
    }
    
    public static GodGivenFunction arrayWeakCAS(Basetype t) {
	switch (t) {
	case INT: return CTypesystemReferences.intArrayWeakCAS_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerArrayWeakCAS_barrier;
	case OBJECT: return CTypesystemReferences.objectArrayWeakCAS_barrier;
	default: throw new Error(""+t);
	}
    }
    
    public static GodGivenFunction arrayWeakCAS(Type t) {
        return arrayWeakCAS(t.effectiveBasetype());
    }
    
    public static GodGivenFunction getField(Basetype t) {
	switch (t) {
	case BOOLEAN:
	case BYTE: return CTypesystemReferences.byteGetField_barrier;
	case CHAR: return CTypesystemReferences.charGetField_barrier;
	case SHORT: return CTypesystemReferences.shortGetField_barrier;
	case INT: return CTypesystemReferences.intGetField_barrier;
	case LONG: return CTypesystemReferences.longGetField_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerGetField_barrier;
	case FLOAT: return CTypesystemReferences.floatGetField_barrier;
	case DOUBLE: return CTypesystemReferences.doubleGetField_barrier;
	case OBJECT: return CTypesystemReferences.objectGetField_barrier;
	default: throw new Error(""+t);
	}
    }
    
    public static GodGivenFunction getField(Type t) {
        return getField(t.effectiveBasetype());
    }
    
    public static GodGivenFunction putField(Basetype t) {
	switch (t) {
	case BOOLEAN:
	case BYTE: return CTypesystemReferences.bytePutField_barrier;
	case CHAR: return CTypesystemReferences.charPutField_barrier;
	case SHORT: return CTypesystemReferences.shortPutField_barrier;
	case INT: return CTypesystemReferences.intPutField_barrier;
	case LONG: return CTypesystemReferences.longPutField_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerPutField_barrier;
	case FLOAT: return CTypesystemReferences.floatPutField_barrier;
	case DOUBLE: return CTypesystemReferences.doublePutField_barrier;
	case OBJECT: return CTypesystemReferences.objectPutField_barrier;
	default: throw new Error(""+t);
	}
    }
    
    public static GodGivenFunction putField(Type t) {
        return putField(t.effectiveBasetype());
    }
    
    public static GodGivenFunction weakCASField(Basetype t) {
	switch (t) {
	case INT: return CTypesystemReferences.intWeakCASField_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerWeakCASField_barrier;
	case OBJECT: return CTypesystemReferences.objectWeakCASField_barrier;
	default: throw new Error(""+t);
	}
    }
    
    public static GodGivenFunction weakCASField(Type t) {
        return weakCASField(t.effectiveBasetype());
    }
    
    public static GodGivenFunction getStatic2(Basetype t) {
        switch (t) {
	case BOOLEAN:
	case BYTE: return CTypesystemReferences.byteGetStatic2_barrier;
	case CHAR: return CTypesystemReferences.charGetStatic2_barrier;
	case SHORT: return CTypesystemReferences.shortGetStatic2_barrier;
	case INT: return CTypesystemReferences.intGetStatic2_barrier;
	case LONG: return CTypesystemReferences.longGetStatic2_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerGetStatic2_barrier;
	case FLOAT: return CTypesystemReferences.floatGetStatic2_barrier;
	case DOUBLE: return CTypesystemReferences.doubleGetStatic2_barrier;
	case OBJECT: return CTypesystemReferences.objectGetStatic2_barrier;
	default: throw new Error(""+t);
        }
    }
    
    public static GodGivenFunction getStatic2(Type t) {
        return getStatic2(t.effectiveBasetype());
    }
    
    public static GodGivenFunction putStatic2(Basetype t) {
        switch (t) {
	case BOOLEAN:
	case BYTE: return CTypesystemReferences.bytePutStatic2_barrier;
	case CHAR: return CTypesystemReferences.charPutStatic2_barrier;
	case SHORT: return CTypesystemReferences.shortPutStatic2_barrier;
	case INT: return CTypesystemReferences.intPutStatic2_barrier;
	case LONG: return CTypesystemReferences.longPutStatic2_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerPutStatic2_barrier;
	case FLOAT: return CTypesystemReferences.floatPutStatic2_barrier;
	case DOUBLE: return CTypesystemReferences.doublePutStatic2_barrier;
	case OBJECT: return CTypesystemReferences.objectPutStatic2_barrier;
	default: throw new Error(""+t);
        }
    }
    
    public static GodGivenFunction putStatic2(Type t) {
        return putStatic2(t.effectiveBasetype());
    }
    
    public static GodGivenFunction weakCASStatic2(Basetype t) {
	switch (t) {
	case INT: return CTypesystemReferences.intWeakCASStatic2_barrier;
	case VM_FCPTR:
	case POINTER: return CTypesystemReferences.pointerWeakCASStatic2_barrier;
	case OBJECT: return CTypesystemReferences.objectWeakCASStatic2_barrier;
	default: throw new Error(""+t);
	}
    }
    
    public static GodGivenFunction weakCASStatic2(Type t) {
        return weakCASStatic2(t.effectiveBasetype());
    }
}

