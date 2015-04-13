/*
 * LowerGetConstant1.java
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

public class LowerGetConstant1 extends CodePhase {
    public LowerGetConstant1(Code c) { super(c); }
    
    public void visitCode() {
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                if (i.opcode()==OpCode.GetMethodRec) {
                    GetMethodInst gmi=(GetMethodInst)i;
                    if (Global.oneShotPayload || !gmi.method().shouldExist()) {
                        // in the case that the method got inlined away, we use
                        // a RemoteMethodRec which will refer to the CompactMethodRec
                        // this *barely* works; we ensure that methods for which
                        // we need something other than a CompactMethodRec do not
                        // get inlined away.
                        gmi.prepend(
                            new CFieldInst(
                                gmi.di(),OpCode.GetCVarAddress,
                                gmi.lhs(),Arg.EMPTY,
                                new RemoteMethodRec(gmi.method())));
                    } else {
                        Var typeDataPtr=code.addVar(Exectype.POINTER);
                        Var methodsPtr=code.addVar(Exectype.POINTER);
                        Var methodPtrPtr=code.addVar(Exectype.POINTER);
                        gmi.prepend(
                            new TypeInst(
                                gmi.di(),OpCode.GetTypeData,
                                typeDataPtr,Arg.EMPTY,
                                gmi.method().getClazz().asType()));
                        gmi.prepend(
                            new CFieldInst(
                                gmi.di(),OpCode.GetCField,
                                methodsPtr,new Arg[]{typeDataPtr},
                                CTypesystemReferences.TypeData_methods));
                        gmi.prepend(
                            new SimpleInst(
                                gmi.di(),OpCode.Add,
                                methodPtrPtr,new Arg[]{
                                    methodsPtr,
                                    PointerConst.make(
                                        gmi.method().getTypeDataIndex()*Global.pointerSize)
                                }));
                        gmi.prepend(
                            new MemoryAccessInst(
                                gmi.di(),OpCode.Load,
                                gmi.lhs(),new Arg[]{methodPtrPtr},
                                Type.POINTER,
                                Mutability.FINAL,
                                Volatility.NON_VOLATILE));
                    }
                    gmi.remove();
                    setChangedCode();
                }
            }
        }
            
        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
    }
}

