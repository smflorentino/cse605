/*
 * LowerBarriers.java
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

public class LowerBarriers extends CodePhase {
    public LowerBarriers(Code c) { super(c); }
    
    public void visitCode() {
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                if (i instanceof HeapAccessInst) {
                    HeapAccessInst hai=(HeapAccessInst)i;
                    if (hai.mode().hasCMStoreBarrier()) {
                        Var fieldPtr=code.addVar(Exectype.POINTER);
                        Arg target;
                        int fieldFlags;
                        
                        // there is an efficiency concern here, sort of.  not for now,
                        // but there will be, when we have a barrier that cares about
                        // target.add(fieldOffset).

                        if (hai.field() instanceof VisibleField) {
                            VisibleField f=(VisibleField)hai.field();
                            if (f.isStatic()) {
                                target=Arg.NULL;
                                hai.prepend(
                                    new HeapAccessInst(
                                        hai.di(),OpCode.AddressOfStatic,
                                        fieldPtr,Arg.EMPTY,
                                        f));
                            } else {
                                target=hai.rhs(0);
                                hai.prepend(
                                    new HeapAccessInst(
                                        hai.di(),OpCode.AddressOfField,
                                        fieldPtr,new Arg[]{ hai.rhs(0) },
                                        f));
                            }
                            fieldFlags=f.runtimeFlags();
                        } else {
                            target=hai.rhs(0);
                            hai.prepend(
                                new HeapAccessInst(
                                    hai.di(),OpCode.AddressOfElement,
                                    fieldPtr,new Arg[]{
                                        hai.rhs(0),
                                        hai.rhs(1)
                                    },
                                    ArrayElementField.INSTANCE));
                            fieldFlags=0;
                        }
                        
                        // FIXME: we're passing down the BarrierMode flags, but
                        // that information is (a) not used and (b) not consistently
                        // passed down into the storeBarrier method.
                        hai.prepend(
                            new MethodInst(
                                hai.di(),OpCode.InvokeStatic,
                                Var.VOID,new Arg[]{
                                    target,
                                    fieldPtr,
                                    StoreSourceCalc.get(hai),
                                    IntConst.make(hai.mode().asInt()|fieldFlags)
                                },
                                Runtime.storeBarrier));
                            
                        hai.setMode(hai.mode().withoutCMStoreBarrier());
                        setChangedCode();
                    }
                }
            }
        }
        
        if (changedCode()) code.killIntraBlockAnalyses();
    }
}


