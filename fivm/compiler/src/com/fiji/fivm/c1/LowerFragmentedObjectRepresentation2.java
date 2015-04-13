/*
 * LowerFragmentedObjectRepresentation2.java
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

public class LowerFragmentedObjectRepresentation2 extends CodePhase {
    
    public LowerFragmentedObjectRepresentation2(Code c) { super(c); }
    
    public void visitCode() {
        // this phase deals only with objects, not arrays.  well, it also deals with
        // things common to both.
        
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                switch (i.opcode()) {
		case OffsetOfField: {
		    FieldInst fi=(FieldInst)i;
		    fi.prepend(
			new SimpleInst(
			    fi.di(),OpCode.Mov,
			    fi.lhs(),new Arg[]{
				new PointerConst(fi.field().location()),
			    }));
		    fi.remove();
		    break;
		}
		case AddressOfField: {
		    Var ptr;
		    assert i.rhs(0).type().isObject();
		    ptr=code.addVar(Exectype.POINTER);
		    i.prepend(
                        new TypeInst(
                            i.di(),OpCode.Cast,
                            ptr,new Arg[]{i.rhs(0)},
                            Type.POINTER));
                    HeapAccessInst fi=(HeapAccessInst)i;
                    for (int n=FragmentedObjectRepresentation.hopsForField(
                             ((OMField)fi.field()).location());
                         n-->0;) {
                        Var tmp=code.addVar(Exectype.POINTER);
                        Var newPtr=code.addVar(Exectype.POINTER);
                        fi.prepend(
                            new MemoryAccessInst(
                                fi.di(),OpCode.Load,
                                tmp,new Arg[]{ ptr },
                                Type.POINTER,
                                Mutability.FINAL,
                                Volatility.NON_VOLATILE));
                        fi.prepend(
                            new SimpleInst(
                                fi.di(),OpCode.And,
                                newPtr,
                                new Arg[]{
                                    tmp,
                                    PointerConst.PTR_HIGH_MASK
                                }));
                        ptr=newPtr;
                    }
                    fi.prepend(
                        new SimpleInst(
                            fi.di(),OpCode.Add,
                            fi.lhs(),new Arg[]{
                                ptr,
                                PointerConst.make(
                                    FragmentedObjectRepresentation.chunkOffset(
                                        ((OMField)fi.field()).location()))
                            }));
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


