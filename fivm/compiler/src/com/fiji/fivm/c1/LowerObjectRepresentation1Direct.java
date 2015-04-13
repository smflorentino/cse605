/*
 * LowerObjectRepresentation1Direct.java
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


public class LowerObjectRepresentation1Direct extends CodePhase {
    public LowerObjectRepresentation1Direct(Code c) { super(c); }
    
    public void visitCode() {
	// never insert barriers; assume that they were already inserted.
	
	assert Global.om==ObjectModel.CONTIGUOUS
            || Global.om==ObjectModel.FRAGMENTED;
	
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
                if (i instanceof HeapAccessInst &&
		    ((HeapAccessInst)i).isInstance()) {
                    HeapAccessInst fi=(HeapAccessInst)i;
                    assert fi.mode().withoutArrayBoundsCheck().isClear()
                        : ("fi = "+fi+", h = "+h+", code = "+code.shortName()+
                           ", di = "+fi.di().shortName()+" = "+fi.di());
                    
                    Var fieldPtr=code.addVar(Exectype.POINTER);
                    Type fieldType;
                    Type origFieldType;
                    
                    if (fi.field() instanceof VisibleField) {
                        VisibleField f=(VisibleField)fi.field();
                        fi.prepend(
                            new HeapAccessInst(
                                fi.di(),OpCode.AddressOfField,
                                fieldPtr,new Arg[]{ fi.rhs(0) },
                                fi.mode(),
                                f));
                        fieldType=f.getType();
                    } else {
                        fi.prepend(
                            new HeapAccessInst(
                                fi.di(),OpCode.AddressOfElement,
                                fieldPtr,new Arg[]{
                                    fi.rhs(0),
                                    fi.rhs(1)
                                },
                                fi.mode(),
                                ArrayElementField.INSTANCE));
                        fieldType=fi.rhs(0).type().arrayElement();
                    }
                    
                    origFieldType=fieldType;
                    
                    if (fieldType.isObject()) {
                        fieldType=Type.POINTER;
                    }

                    switch (i.opcode()) {
                    case WeakCASField:
		    case WeakCASElement: {
                        Arg comparand=Util.pointerifyObject(
			    code,fi,StoreSourceCalc.getCASComparand(fi));
                        Arg value=Util.pointerifyObject(
			    code,fi,StoreSourceCalc.get(fi));
                        assert comparand.type().effectiveBasetype()
                            == value.type().effectiveBasetype();
                        fi.prepend(
                            new MemoryAccessInst(
                                fi.di(),OpCode.WeakCAS,
                                fi.lhs(),new Arg[]{
                                    fieldPtr,
                                    comparand,
                                    value
                                },
                                fieldType,
                                origFieldType,
                                fi.effectiveMutability(),
                                fi.volatility()));
                        
                        fi.remove();
                        setChangedCode();
                        break;
                    }
                    case PutField:
		    case ArrayStore: {
			Arg value=Util.pointerifyObject(
			    code,fi,StoreSourceCalc.get(fi));
			fi.prepend(
			    new MemoryAccessInst(
				fi.di(),OpCode.Store,
				Var.VOID,
				new Arg[]{
				    fieldPtr,
				    value
				},
				fieldType,
                                origFieldType,
                                fi.effectiveMutability(),
                                fi.volatility()));
                        fi.remove();
                        setChangedCode();
                        break;
                    }
                    case GetField:
		    case ArrayLoad: {
                        Var value;
                        if (fi.lhs().type().isObject()) {
                            value=code.addVar(Exectype.POINTER,
                                              fi.lhs().type());
                        } else {
                            value=fi.lhs();
                        }
                        fi.prepend(
                            new MemoryAccessInst(
                                fi.di(),OpCode.Load,
                                value,
                                new Arg[]{
                                    fieldPtr
                                },
                                fieldType,
                                origFieldType,
                                fi.effectiveMutability(),
                                fi.volatility()));
                        if (fi.lhs().type().isObject()) {
                            fi.prepend(
                                new TypeInst(
                                    fi.di(),OpCode.Cast,
                                    fi.lhs(),new Arg[]{value},
                                    fi.lhs().type().asType()));
                        }
                        fi.remove();
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


