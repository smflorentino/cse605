/*
 * LowerCompactObjectAccesses.java
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
 * Compact version of lowering object (array element, field, etc) accesses.
 * Note that this should NOT be used in all cases; it should only be used
 * when code compactness is more important than everything else.  The main
 * case where we use this is for obscenely large methods, like static
 * initializers, that don't need to be fast but are often so complex that
 * doing inline object model conversion results in a blow-up that leads to
 * either pathological behavior (up to and including OOME) in c1, or else
 * pathological behavior in gcc (as in, gcc takes several minutes to compile
 * something that would normally otherwise take a few seconds).
 */
public class LowerCompactObjectAccesses extends CodePhase {
    public LowerCompactObjectAccesses(Code c) { super(c); }
    
    public void visitCode() {
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                // FIXME: this is not a particularly "efficient" implementation
                
                if (i instanceof HeapAccessInst) {
                    HeapAccessInst hai = (HeapAccessInst)i;
                    
                    assert hai.mode().withoutCMStoreBarrier().withoutScopeCheck().isClear() : hai;
                    
                    Type type=hai.fieldType();
                    Type base;
                    
                    if (type.isObject()) {
                        base=Global.root().objectType;
                    } else if (type==Type.VM_FCPTR) {
                        base=Type.POINTER;
                    } else {
                        base=type.effectiveBasetype().asType;
                    }
                    
                    switch (hai.opcode()) {
                    case PutField: {
                        VisibleField f=(VisibleField)hai.field();
                        VisibleMethod vm=
                            Runtime.fivmRuntime.getMethod(
                                new MethodSignature(
                                    Type.VOID,
                                    base.effectiveBasetype().toString+"PutField",
                                    new Type[]{
                                        Global.root().objectType,
                                        Type.POINTER,
                                        base,
                                        Type.INT
                                    }));
                        assert vm!=null : "base = "+base;
                        hai.prepend(
                            new MethodInst(
                                hai.di(),OpCode.InvokeStatic,
                                Var.VOID,new Arg[]{
                                    hai.rhs(0),
                                    PointerConst.make(f.location()),
                                    hai.rhs(1),
                                    IntConst.make(f.runtimeFlags())
                                },
                                vm));
                        hai.remove();
                        setChangedCode();
                        break;
                    }
                    case GetField: {
                        VisibleField f=(VisibleField)hai.field();
                        VisibleMethod vm=
                            Runtime.fivmRuntime.getMethod(
                                new MethodSignature(
                                    base,
                                    base.effectiveBasetype().toString+"GetField",
                                    new Type[]{
                                        Global.root().objectType,
                                        Type.POINTER,
                                        Type.INT
                                    }));
                        Var result=code.addVar(base.asExectype());
                        assert vm!=null : "base = "+base;
                        hai.prepend(
                            new MethodInst(
                                hai.di(),OpCode.InvokeStatic,
                                result,new Arg[]{
                                    hai.rhs(0),
                                    PointerConst.make(f.location()),
                                    IntConst.make(f.runtimeFlags())
                                },
                                vm));
                        Var tmp=code.addVar(result.type());
                        if (f.isNonZero()) {
                            hai.prepend(
                                new SimpleInst(
                                    hai.di(),OpCode.CastNonZero,
                                    tmp,new Arg[]{result}));
                        } else {
                            hai.prepend(
                                new SimpleInst(
                                    hai.di(),OpCode.Mov,
                                    tmp,new Arg[]{result}));
                        }
                        hai.prepend(
                            new TypeInst(
                                hai.di(),OpCode.Cast,
                                hai.lhs(),new Arg[]{tmp},
                                hai.lhs().type().asType()));
                        hai.remove();
                        setChangedCode();
                        break;
                    }
                    case ArrayStore: {
                        VisibleMethod vm=
                            Runtime.fivmRuntime.getMethod(
                                new MethodSignature(
                                    Type.VOID,
                                    base.effectiveBasetype().toString+"ArrayStore",
                                    new Type[]{
                                        Global.root().objectType,
                                        Type.INT,
                                        base,
                                    }));
                        assert vm!=null : "base = "+base;
                        hai.prepend(
                            new MethodInst(
                                hai.di(),OpCode.InvokeStatic,
                                Var.VOID,hai.rhs(),
                                vm));
                        hai.remove();
                        setChangedCode();
                        break;
                    }
                    case ArrayLoad: {
                        VisibleMethod vm=
                            Runtime.fivmRuntime.getMethod(
                                new MethodSignature(
                                    base,
                                    base.effectiveBasetype().toString+"ArrayLoad",
                                    new Type[]{
                                        Global.root().objectType,
                                        Type.INT
                                    }));
                        Var result=code.addVar(base.asExectype());
                        assert vm!=null : "base = "+base;
                        hai.prepend(
                            new MethodInst(
                                hai.di(),OpCode.InvokeStatic,
                                result,hai.rhs(),
                                vm));
                        hai.prepend(
                            new TypeInst(
                                hai.di(),OpCode.Cast,
                                hai.lhs(),new Arg[]{result},
                                hai.lhs().type().asType()));
                        hai.remove();
                        setChangedCode();
                        break;
                    }
                    default: break;
                    }
                }
            }
        }
        
        if (changedCode()) code.killIntraBlockAnalyses();
    }
}

