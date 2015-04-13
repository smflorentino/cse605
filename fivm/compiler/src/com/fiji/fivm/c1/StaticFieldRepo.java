/*
 * StaticFieldRepo.java
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

import java.util.*;

public class StaticFieldRepo {
    private StaticFieldRepo() {}
    
    static class Accountant {
        LinkedHashMap< VisibleField, Pointer > offsets=
            new LinkedHashMap< VisibleField, Pointer >();
        int tableSize;
        
        synchronized Pointer offsetForField(VisibleField f) {
            Pointer result=offsets.get(f);
            if (result==null) {
                int fieldSize=f.getType().effectiveBasetype().bytes;
                while ((tableSize%fieldSize)!=0) {
                    tableSize++;
                }
                result=new Pointer(tableSize);
                tableSize+=fieldSize;
                offsets.put(f,result);
            }
            return result;
        }
    }

    private static Accountant ref=new Accountant();
    private static Accountant prim=new Accountant();
    
    public static boolean isRefField(VisibleField f) {
        // the idea is that it ends up in the ref field vector if it's traced.
        return f.isTraced();
    }
    
    public static Pointer offsetForField(VisibleField f) {
        if (isRefField(f)) {
            return ref.offsetForField(f);
        } else {
            return prim.offsetForField(f);
        }
    }
    
    public static Arg generateIRForAddress(Code code,
                                           Operation before,
                                           VisibleField f) {
        Var tablePtr=code.addVar(Exectype.POINTER);
        Var resultPtr=code.addVar(Exectype.POINTER);
        if (Global.oneShotPayload) {
            if (isRefField(f)) {
                before.prepend(
                    new CFieldInst(
                        before.di(),OpCode.GetCVarAddress,
                        tablePtr,Arg.EMPTY,
                        CTypesystemReferences.generated_staticRefFields));
            } else {
                before.prepend(
                    new CFieldInst(
                        before.di(),OpCode.GetCVarAddress,
                        tablePtr,Arg.EMPTY,
                        CTypesystemReferences.generated_staticPrimFields));
            }
        } else {
            if (isRefField(f)) {
                before.prepend(
                    new CFieldInst(
                        before.di(),OpCode.GetCField,
                        tablePtr,new Arg[]{
                            Arg.THREAD_STATE
                        },
                        CTypesystemReferences.ThreadState_refFields));
            } else {
                before.prepend(
                    new CFieldInst(
                        before.di(),OpCode.GetCField,
                        tablePtr,new Arg[]{
                            Arg.THREAD_STATE
                        },
                        CTypesystemReferences.ThreadState_primFields));
            }
        }
        before.prepend(
            new SimpleInst(
                before.di(),OpCode.Add,
                resultPtr,new Arg[]{
                    tablePtr,
                    PointerConst.make(offsetForField(f))
                }));
        return resultPtr;
    }
    
    public static int primFieldsSize() {
        return prim.tableSize;
    }
    
    public static int refFieldsSize() {
        return ref.tableSize;
    }
}


