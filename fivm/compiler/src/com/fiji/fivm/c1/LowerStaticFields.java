/*
 * LowerStaticFields.java
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

public class LowerStaticFields extends CodePhase {
    public LowerStaticFields(Code c) { super(c); }
    
    public void visitCode() {
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case AddressOfStatic: {
		    HeapAccessInst fi=(HeapAccessInst)i;
		    VisibleField f=(VisibleField)fi.field();
                    i.prepend(
                        new SimpleInst(
                            i.di(),OpCode.Mov,
                            i.lhs(),new Arg[]{
                                StaticFieldRepo.generateIRForAddress(code,i,f)
                            }));
		    i.remove();
		    setChangedCode();
		    break;
		}
		case WeakCASStatic: {
		    HeapAccessInst fi=(HeapAccessInst)i;
                    assert fi.mode().isClear();
		    VisibleField f=(VisibleField)fi.field();
                    if (f.getType().isObject()) {
                        Var comparand=code.addVar(Exectype.POINTER,
                                                  fi.rhs(0).type());
                        Var value=code.addVar(Exectype.POINTER,
                                              fi.rhs(1).type());
                        fi.prepend(
                            new TypeInst(
                                fi.di(),OpCode.Cast,
                                comparand,new Arg[]{fi.rhs()[0]},
                                Type.POINTER));
                        fi.prepend(
                            new TypeInst(
                                fi.di(),OpCode.Cast,
                                value,new Arg[]{fi.rhs()[1]},
                                Type.POINTER));
                        fi.prepend(
                            new MemoryAccessInst(
                                fi.di(),OpCode.WeakCAS,
                                fi.lhs(),new Arg[]{
                                    StaticFieldRepo.generateIRForAddress(code,i,f),
                                    comparand,
                                    value
                                },
                                Type.POINTER,
                                f.getType(),
                                fi.effectiveMutability(),
                                fi.volatility()));
                    } else {
                        fi.prepend(
                            new MemoryAccessInst(
                                fi.di(),OpCode.WeakCAS,
                                fi.lhs(),new Arg[]{
                                    StaticFieldRepo.generateIRForAddress(code,i,f),
                                    fi.rhs(0),
                                    fi.rhs(1)
                                },
                                f.getType(),
                                fi.effectiveMutability(),
                                fi.volatility()));
                    }
		    fi.remove();
		    setChangedCode();
		    break;
		}
		case PutStatic: {
		    HeapAccessInst fi=(HeapAccessInst)i;
                    assert fi.mode().isClear();
		    VisibleField f=(VisibleField)fi.field();
                    if (f.getType().isObject()) {
                        Var value=code.addVar(Exectype.POINTER,
                                              fi.rhs(0).type());
                        fi.prepend(
                            new TypeInst(
                                fi.di(),OpCode.Cast,
                                value,new Arg[]{fi.rhs()[0]},
                                Type.POINTER));
                        fi.prepend(
                            new MemoryAccessInst(
                                fi.di(),OpCode.Store,
                                Var.VOID,new Arg[]{
                                    StaticFieldRepo.generateIRForAddress(code,i,f),
                                    value
                                },
                                Type.POINTER,
                                f.getType(),
                                fi.effectiveMutability(),
                                fi.volatility()));
                    } else {
                        fi.prepend(
                            new MemoryAccessInst(
                                fi.di(),OpCode.Store,
                                Var.VOID,new Arg[]{
                                    StaticFieldRepo.generateIRForAddress(code,i,f),
                                    fi.rhs(0)
                                },
                                f.getType(),
                                fi.effectiveMutability(),
                                fi.volatility()));
                    }
		    fi.remove();
		    setChangedCode();
		    break;
		}
		case GetStatic: {
		    HeapAccessInst fi=(HeapAccessInst)i;
                    assert fi.mode().isClear();
		    VisibleField f=(VisibleField)fi.field();
		    assert f.shouldExist() : "For "+fi+" in "+h+" in "+code;
		    if (f.getType().isObject()) {
			Var value=code.addVar(Exectype.POINTER,
                                              fi.lhs().type());
                        fi.prepend(
                            new MemoryAccessInst(
                                fi.di(),OpCode.Load,
                                value,new Arg[]{
                                    StaticFieldRepo.generateIRForAddress(code,i,f)
                                },
                                Type.POINTER,
                                f.getType(),
                                fi.effectiveMutability(),
                                fi.volatility()));
			fi.prepend(
			    new TypeInst(
				fi.di(),OpCode.Cast,
				fi.lhs(),new Arg[]{value},
				f.getType()));
		    } else {
			fi.prepend(
			    new MemoryAccessInst(
				fi.di(),OpCode.Load,
				fi.lhs(),new Arg[]{
                                    StaticFieldRepo.generateIRForAddress(code,i,f)
                                },
				f.getType(),
                                fi.effectiveMutability(),
                                fi.volatility()));
		    }
		    fi.remove();
		    setChangedCode();
		    break;
		}
		default: break;
		}
	    }
	}
	
	if (changedCode()) code.killIntraBlockAnalyses();
    }
}

