/*
 * LowerContiguousObjectRepresentation2.java
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

import com.fiji.fivm.om.OMField;


public class LowerContiguousObjectRepresentation2 extends CodePhase {
    public LowerContiguousObjectRepresentation2(Code c) { super(c); }
    
    public void visitCode() {
	// always inserts barriers on everything except array length and GetHeaderAddress.
	// the resulting code is C, so we can afford that.
	
	// object pointers point to the first array element (or where the first array
	// element would be if the object is not an array)
	
	assert Global.om==ObjectModel.CONTIGUOUS;
	
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case OffsetOfField: {
		    FieldInst fi=(FieldInst)i;
		    fi.prepend(
			new SimpleInst(
			    fi.di(),OpCode.Mov,
			    fi.lhs(),new Arg[]{
				PointerConst.make(fi.field().location()-4),
			    }));
		    fi.remove();
                    setChangedCode();
		    break;
		}
		case OffsetOfElement: {
		    TypeInst ti=(TypeInst)i;
		    Type t=ti.getType();
		    assert t.isArray();
		    Var ptrIndex=code.addVar(Exectype.POINTER);
                    Var result=code.addVar(Exectype.POINTER);
		    ti.prepend(
			new TypeInst(
			    ti.di(),OpCode.Cast,
			    ptrIndex,new Arg[]{ti.rhs(0)},
			    Type.POINTER));
		    ti.prepend(
			new SimpleInst(
			    ti.di(),OpCode.Mul,
			    result,new Arg[]{
				ptrIndex,
				PointerConst.make(t.arrayElement().effectiveBasetype().bytes)
			    }));
                    if (Global.pointerSize==4 ||
                        t.arrayElement().effectiveBasetype().bytes<=4) {
                        ti.prepend(
                            new SimpleInst(
                                ti.di(),OpCode.Mov,
                                ti.lhs(),new Arg[]{result}));
                    } else {
                        ti.prepend(
                            new SimpleInst(
                                ti.di(),OpCode.Add,
                                ti.lhs(),
                                new Arg[]{
                                    result,
                                    PointerConst.make(4)
                                }));
                    }
		    ti.remove();
                    setChangedCode();
		    break;
		}
		case ArrayLength:
		case AddressOfField:
		case AddressOfElement: {
		    Var ptr;
		    assert i.rhs(0).type().isObject();
		    ptr=code.addVar(Exectype.POINTER);
		    i.prepend(new TypeInst(i.di(),OpCode.Cast,
					   ptr,new Arg[]{i.rhs(0)},
					   Type.POINTER));
		    switch (i.opcode()) {
		    case ArrayLength: {
			loadAtOffset(i.lhs(),i,ptr,-4,Type.INT);
                        setChangedCode();
			break;
		    }
		    case AddressOfField: {
			HeapAccessInst fi=(HeapAccessInst)i;
			fi.prepend(
			    new SimpleInst(
				fi.di(),OpCode.Add,
				fi.lhs(),new Arg[]{
				    ptr,
				    PointerConst.make(((OMField)fi.field()).location()-4)
				}));
                        setChangedCode();
			break;
		    }
		    case AddressOfElement: {
			Exectype t=i.rhs(0).type();
			assert t.isArray();
			Var tmp1=code.addVar(Exectype.POINTER);
			Var tmp2=code.addVar(Exectype.POINTER);
			Var tmp3=code.addVar(Exectype.POINTER);
                        if (Global.pointerSize==4 ||
                            t.arrayElement().effectiveBasetype().bytes<=4) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    tmp1,new Arg[]{ptr}));
                        } else {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Add,
                                    tmp1,
                                    new Arg[]{
                                        ptr,
                                        PointerConst.make(4)
                                    }));
                        }
			i.prepend(
			    new TypeInst(
				i.di(),OpCode.Cast,
				tmp2,new Arg[]{i.rhs(1)},
				Type.POINTER));
			i.prepend(
			    new SimpleInst(
				i.di(),OpCode.Mul,
				tmp3,new Arg[]{
				    tmp2,
				    PointerConst.make(t.arrayElement().effectiveBasetype().bytes)
				}));
			i.prepend(
			    new SimpleInst(
				i.di(),OpCode.Add,
				i.lhs(),new Arg[]{
				    tmp1,
				    tmp3
				}));
			i.remove();
                        setChangedCode();
			break;
		    }
		    default: throw new Error(""+i);
		    }
		    i.remove();
		    break;
		}
		default: break;
		}
	    }
	}
	
	if (changedCode()) code.killIntraBlockAnalyses();
    }
}


