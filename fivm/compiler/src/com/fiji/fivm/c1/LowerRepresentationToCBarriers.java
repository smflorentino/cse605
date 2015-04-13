/*
 * LowerRepresentationToCBarriers.java
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


public class LowerRepresentationToCBarriers extends CodePhase {
    public LowerRepresentationToCBarriers(Code c) { super(c); }
    
    public void visitCode() {
	// always inserts barriers on everything
	// the resulting code is C, so we can afford that.
	
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case PutField:
		case GetField:
		case ArrayLoad:
		case ArrayStore:
		case WeakCASField:
		case WeakCASElement:
                case ArrayLength:
                case AddressOfField:
                case AddressOfElement: {
		    Var ptr;
		    assert i.rhs(0).type().isObject();
		    ptr=code.addVar(Exectype.POINTER);
		    i.prepend(
                        new TypeInst(
                            i.di(),OpCode.Cast,
                            ptr,new Arg[]{i.rhs(0)},
                            Type.POINTER));
		    switch (i.opcode()) {
                    case ArrayLength:
                        i.prepend(
                            new CFieldInst(
                                i.di(),OpCode.Call,
                                i.lhs(),new Arg[]{
                                    Arg.THREAD_STATE,
                                    ptr,
                                    IntConst.make(0)
                                },
                                CTypesystemReferences.arrayLength_barrier));
                        break;
                    case AddressOfField: {
                        HeapAccessInst fi=(HeapAccessInst)i;
                        fi.prepend(
                            new CFieldInst(
                                fi.di(),OpCode.Call,
                                fi.lhs(),new Arg[]{
                                    Arg.THREAD_STATE,
                                    ptr,
                                    PointerConst.make(fi.fieldField().offset())
                                },
                                CTypesystemReferences.addressOfField_barrier));
                        break;
                    }
                    case AddressOfElement:
                        i.prepend(
                            new CFieldInst(
                                i.di(),OpCode.Call,
                                i.lhs(),new Arg[]{
                                    Arg.THREAD_STATE,
                                    ptr,
                                    i.rhs(1),
                                    PointerConst.make(
                                        i.rhs(0).type().arrayElement().effectiveBasetype().bytes)
                                },
                                CTypesystemReferences.addressOfElement_barrier));
                        break;
		    case WeakCASField: {
			HeapAccessInst fi=(HeapAccessInst)i;
			// NOTE: currently any field that we see being CASed will
			// exist, but you could imagine a super-fancy analysis in
			// which this would not be the case.
			if (fi.fieldField().shouldExist()) {
			    Function rf=
				CBarriers.weakCASField(
				    fi.fieldField().getType().effectiveBasetype());
			    Arg comparand;
			    Arg value;
			    assert rf.getParam(3)==rf.getParam(4);
			    assert fi.rhs(1).type().effectiveBasetype()
				== fi.rhs(2).type().effectiveBasetype();
			    if (Exectype.make(rf.getParam(3))!=fi.rhs(1).type()) {
				comparand=code.addVar(Exectype.make(rf.getParam(3)));
				value=code.addVar(Exectype.make(rf.getParam(4)));
				fi.prepend(
				    new TypeInst(
					fi.di(),OpCode.Cast,
					(Var)comparand,new Arg[]{fi.rhs(1)},
					Type.make(rf.getParam(3))));
				fi.prepend(
				    new TypeInst(
					fi.di(),OpCode.Cast,
					(Var)value,new Arg[]{fi.rhs(2)},
					Type.make(rf.getParam(4))));
			    } else {
				comparand=fi.rhs(1);
				value=fi.rhs(2);
			    }
			    fi.prepend(
				new CFieldInst(
				    fi.di(),OpCode.Call,
				    fi.lhs(),
				    new Arg[]{
					Arg.THREAD_STATE,
					ptr,
					new PointerConst(fi.fieldField().offset()),
					comparand,
					value,
					new IntConst(fi.fieldField().runtimeFlags())
				    },
				    rf));
			}
			break;
		    }
		    case PutField: {
			HeapAccessInst fi=(HeapAccessInst)i;
			if (fi.fieldField().shouldExist()) {
			    Function rf=
				CBarriers.putField(
				    fi.fieldField().getType().effectiveBasetype());
			    Arg value;
			    if (Exectype.make(rf.getParam(3))!=fi.rhs(1).type()) {
				value=code.addVar(Exectype.make(rf.getParam(3)));
				fi.prepend(
				    new TypeInst(
					fi.di(),OpCode.Cast,
					(Var)value,new Arg[]{fi.rhs(1)},
					Type.make(rf.getParam(3))));
			    } else {
				value=fi.rhs(1);
			    }
			    fi.prepend(
				new CFieldInst(
				    fi.di(),OpCode.Call,
				    Var.VOID,
				    new Arg[]{
					Arg.THREAD_STATE,
					ptr,
					new PointerConst(fi.fieldField().offset()),
					value,
					new IntConst(fi.fieldField().runtimeFlags())
				    },
				    rf));
			}
			break;
		    }
		    case GetField: {
			HeapAccessInst fi=(HeapAccessInst)i;
			assert fi.fieldField().shouldExist()
			    : "For "+fi+" in "+h+" in "+code;
			Function rf=
			    CBarriers.getField(
				fi.fieldField().getType().effectiveBasetype());
			Var value;
			if (Exectype.make(rf.getResult())!=fi.lhs().type()) {
			    value=code.addVar(Exectype.make(rf.getResult()));
			} else {
			    value=fi.lhs();
			}
			fi.prepend(
			    new CFieldInst(
				fi.di(),OpCode.Call,
				value,
				new Arg[]{
				    Arg.THREAD_STATE,
				    ptr,
				    new PointerConst(fi.fieldField().offset()),
				    new IntConst(fi.fieldField().runtimeFlags())
				},
				rf));
			if (value!=fi.lhs()) {
			    fi.prepend(
				new TypeInst(
				    fi.di(),OpCode.Cast,
				    fi.lhs(),new Arg[]{value},
				    fi.lhs().type().asType()));
			}
			break;
		    }
		    case WeakCASElement: {
			Function rf=
			    CBarriers.arrayWeakCAS(
				i.rhs(0).type().arrayElement().effectiveBasetype());
			Arg comparand;
			Arg value;
			assert rf.getParam(3)==rf.getParam(4);
			assert i.rhs(2).type().effectiveBasetype()
			    == i.rhs(3).type().effectiveBasetype();
			if (Exectype.make(rf.getParam(3))!=i.rhs(2).type()) {
			    comparand=code.addVar(Exectype.make(rf.getParam(3)));
			    value=code.addVar(Exectype.make(rf.getParam(4)));
			    i.prepend(
				new TypeInst(
				    i.di(),OpCode.Cast,
				    (Var)comparand,new Arg[]{i.rhs(2)},
				    Type.make(rf.getParam(3))));
			    i.prepend(
				new TypeInst(
				    i.di(),OpCode.Cast,
				    (Var)value,new Arg[]{i.rhs(3)},
				    Type.make(rf.getParam(4))));
			} else {
			    comparand=i.rhs(2);
			    value=i.rhs(3);
			}
			i.prepend(
			    new CFieldInst(
				i.di(),OpCode.Call,
				i.lhs(),
				new Arg[]{
				    Arg.THREAD_STATE,
				    ptr,
				    i.rhs(1),
				    comparand,
				    value,
				    new IntConst(0)
				},
				rf));
			break;
		    }
		    case ArrayStore: {
			Function rf=
			    CBarriers.arrayStore(
				i.rhs(0).type().arrayElement().effectiveBasetype());
			Arg value;
			if (Exectype.make(rf.getParam(3))!=i.rhs(2).type()) {
			    value=code.addVar(Exectype.make(rf.getParam(3)));
			    i.prepend(
				new TypeInst(
				    i.di(),OpCode.Cast,
				    (Var)value,new Arg[]{i.rhs(2)},
				    Type.make(rf.getParam(3))));
			} else {
			    value=i.rhs(2);
			}
			i.prepend(
			    new CFieldInst(
				i.di(),OpCode.Call,
				Var.VOID,
				new Arg[]{
				    Arg.THREAD_STATE,
				    ptr,
				    i.rhs(1),
				    value,
				    new IntConst(0)
				},
				rf));
			break;
		    }
		    case ArrayLoad: {
			Function rf=
			    CBarriers.arrayLoad(
				i.rhs(0).type().arrayElement().effectiveBasetype());
			Var value;
			if (Exectype.make(rf.getResult())!=i.lhs().type()) {
			    value=code.addVar(Exectype.make(rf.getResult()));
			} else {
			    value=i.lhs();
			}
			i.prepend(
			    new CFieldInst(
				i.di(),OpCode.Call,
				value,
				new Arg[]{
				    Arg.THREAD_STATE,
				    ptr,
				    i.rhs(1),
				    new IntConst(0)
				},
				rf));
			if (value!=i.lhs()) {
			    i.prepend(
				new TypeInst(
				    i.di(),OpCode.Cast,
				    i.lhs(),new Arg[]{value},
				    i.lhs().type().asType()));
			}
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
	
	setChangedCode();
	code.killAllAnalyses();
    }
}


