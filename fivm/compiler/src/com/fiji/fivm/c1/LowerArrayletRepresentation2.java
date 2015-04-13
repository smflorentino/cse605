/*
 * LowerArrayletRepresentation2.java
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

import com.fiji.fivm.Settings;

public class LowerArrayletRepresentation2 extends CodePhase {
    public LowerArrayletRepresentation2(Code c) { super(c); }
    
    public void visitCode() {
        // this lowers arraylets but not objects
        
	for (Header h : code.headers2()) {
	    for (Instruction i : h.instructions2()) {
		switch (i.opcode()) {
		case OffsetOfElement: {
                    // this is kind of useless but oh well
		    TypeInst ti=(TypeInst)i;
		    Type t=ti.getType();
		    assert t.isArray();
		    Var ptrIndex=code.addVar(Exectype.POINTER);
		    ti.prepend(
			new TypeInst(
			    ti.di(),OpCode.Cast,
			    ptrIndex,new Arg[]{ti.rhs(0)},
			    Type.POINTER));
		    ti.prepend(
			new SimpleInst(
			    ti.di(),OpCode.Mul,
			    ti.lhs(),new Arg[]{
				ptrIndex,
				PointerConst.make(t.arrayElement().effectiveBasetype().bytes)
			    }));
		    ti.remove();
		    break;
		}
		case ArrayLength:
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
		    case ArrayLength: {
                        Var tmp=code.addVar(Exectype.POINTER);
                        Var tmp2=code.addVar(Exectype.POINTER);

                        i.prepend(
                            new MemoryAccessInst(
                                i.di(),OpCode.Load,
                                tmp,new Arg[]{ptr},
                                Type.POINTER));
                        i.prepend(
                            new SimpleInst(
                                i.di(),OpCode.Sub,
                                tmp2,new Arg[]{
                                    tmp,
                                    PointerConst.make(4)
                                }));
                        i.prepend(
                            new MemoryAccessInst(
                                i.di(),OpCode.Load,
                                i.lhs(),new Arg[]{tmp2},
                                Type.INT,
                                Mutability.FINAL,
                                Volatility.NON_VOLATILE));
                        i.remove();
                        
			break;
		    }
		    case AddressOfElement: {
                        boolean abc=
                            ((HeapAccessInst)i).mode().hasArrayBoundsCheck();
                        
			Exectype t=i.rhs(0).type();
			assert t.isArray();

                        Var fastVal=code.addVar(Exectype.POINTER);
                        Var slowVal=code.addVar(Exectype.POINTER);
                        
                        Var pseudoLength=code.addVar(Exectype.INT);
                        loadAtOffset(pseudoLength,i,ptr,
                                     FragmentedObjectRepresentation.chunkHeaderSize+
                                     FragmentedObjectRepresentation.gcHeaderSize+
                                     Global.tdHeaderSize(),
                                     Type.INT);
                        
                        // FIXME: if we're not doing an ABC, then this can be
                        // optimized quite a bit.
                        Var pred=code.addVar(Exectype.INT);
                        i.prepend(
                            new SimpleInst(
                                i.di(),OpCode.ULessThan,
                                pred,new Arg[]{
                                    i.rhs(1),
                                    pseudoLength
                                }));
                        
                        Header cont=h.split(i);

                        Header fast=h.makeSimilar(i.di());
                        Header slow=h.makeSimilar(i.di());
                        
                        h.setFooter(
                            new Branch(
                                i.di(),OpCode.BranchNonZero,
                                new Arg[]{pred},
                                slow,
                                fast,
                                Settings.HFGC_PREDICT_NO_ARRAYLETS?
                                BranchPrediction.PREDICTING_TRUE:
                                (Settings.HFGC_PREDICT_ARRAYLETS?
                                 BranchPrediction.NO_PREDICTION:
                                 BranchPrediction.PREDICTING_FALSE)));
                        
                        Var tmp8=code.addVar(Exectype.POINTER);
                        Var tmp9=code.addVar(Exectype.POINTER);
                        Var tmp10=code.addVar(Exectype.POINTER);
                        
                        fast.append(
                            new TypeInst(
                                i.di(),OpCode.Cast,
                                tmp8,new Arg[]{i.rhs(1)},
                                Type.POINTER));
                        fast.append(
                            new SimpleInst(
                                i.di(),OpCode.Mul,
                                tmp9,new Arg[]{
                                    tmp8,
                                    PointerConst.make(t.arrayElement().effectiveBasetype().bytes)
                                }));
                        fast.append(
                            new SimpleInst(
                                i.di(),OpCode.Add,
                                tmp10,new Arg[]{
                                    tmp9,
                                    PointerConst.make(
                                        Util.align(
                                            (FragmentedObjectRepresentation.chunkHeaderSize+
                                             FragmentedObjectRepresentation.gcHeaderSize+
                                             Global.tdHeaderSize()+
                                             4),
                                            t.arrayElement().effectiveBasetype().bytes))
                                }));
                        fast.append(
                            new SimpleInst(
                                i.di(),OpCode.Add,
                                fastVal,new Arg[]{
                                    ptr,
                                    tmp10
                                }));
                        fast.append(
                            new SimpleInst(
                                i.di(),OpCode.Ipsilon,
                                i.lhs(),new Arg[]{fastVal}));
                        fast.setFooter(
                            new Jump(i.di(),cont));
                        
                        if (Settings.INTERNAL_INST) {
                            slow.append(
                                new CFieldInst(
                                    i.di(),OpCode.Call,
                                    Var.VOID,new Arg[]{
                                        Arg.THREAD_STATE,
                                        Arg.FRAME,
                                        ptr,
                                        i.rhs(1)
                                    },
                                    Inst.beforeAASlow));
                        }
                        
                        Var spine=code.addVar(Exectype.POINTER);
                        loadAtOffset(spine,slow.getFooter(),ptr,0,Type.POINTER);
                        
                        if (abc) {
                            Var realLength=code.addVar(Exectype.INT);
                            Var realPred=code.addVar(Exectype.INT);

                            loadAtOffset(realLength,slow.getFooter(),spine,-4,Type.INT);

                            slow.append(
                                new SimpleInst(
                                    i.di(),OpCode.ULessThan,
                                    realPred,new Arg[]{
                                        i.rhs(1),
                                        realLength
                                    }));
                            slow.append(
                                new TypeInst(
                                    i.di(),OpCode.ThrowRTEOnZero,
                                    Var.VOID,new Arg[]{realPred},
                                    Runtime.arrayIndexOutOfBoundsException.asType()));
                        }
                        
			Var tmp1=code.addVar(Exectype.POINTER);
			Var tmp2=code.addVar(Exectype.POINTER);
			slow.append(
			    new TypeInst(
				i.di(),OpCode.Cast,
				tmp1,new Arg[]{i.rhs(1)},
				Type.POINTER));
			slow.append(
			    new SimpleInst(
				i.di(),OpCode.Mul,
				tmp2,new Arg[]{
				    tmp1,
				    PointerConst.make(t.arrayElement().effectiveBasetype().bytes)
				}));
                        
                        // now tmp2 holds the offset...  just mask off the lower logChunkWidth
                        // bits.
                        
                        Var tmp3=code.addVar(Exectype.POINTER);
                        Var tmp4=code.addVar(Exectype.POINTER);
                        Var tmp5=code.addVar(Exectype.POINTER);
                        Var tmp6=code.addVar(Exectype.POINTER);
                        Var tmp7=code.addVar(Exectype.POINTER);
                        slow.append(
                            new SimpleInst(
                                i.di(),OpCode.Ushr,
                                tmp3,new Arg[]{
                                    tmp2,
                                    IntConst.make(
                                        FragmentedObjectRepresentation.logChunkWidth)
                                }));
                        slow.append(
                            new SimpleInst(
                                i.di(),OpCode.Mul,
                                tmp4,new Arg[]{
                                    tmp3,
                                    PointerConst.make(
                                        Global.pointerSize)
                                }));
                        slow.append(
                            new SimpleInst(
                                i.di(),OpCode.Add,
                                tmp5,new Arg[]{
                                    tmp4,
                                    spine
                                }));
                        slow.append(
                            new MemoryAccessInst(
                                i.di(),OpCode.Load,
                                tmp6,new Arg[]{tmp5},
                                Type.POINTER,
                                Mutability.FINAL,
                                Volatility.NON_VOLATILE));
                        
                        // we now have a pointer to the *tail* of the chunk containing
                        // the particular arraylet
                        
                        slow.append(
                            new SimpleInst(
                                i.di(),OpCode.And,
                                tmp7,new Arg[]{
                                    tmp2,
                                    PointerConst.make(
                                        FragmentedObjectRepresentation.chunkWidth-1)
                                }));
			slow.append(
			    new SimpleInst(
				i.di(),OpCode.Sub,
				slowVal,new Arg[]{
				    tmp6,
				    tmp7
				}));
                        
                        slow.append(
                            new SimpleInst(
                                i.di(),OpCode.Ipsilon,
                                i.lhs(),new Arg[]{slowVal}));

                        if (Settings.INTERNAL_INST) {
                            slow.append(
                                new CFieldInst(
                                    i.di(),OpCode.Call,
                                    Var.VOID,new Arg[]{
                                        Arg.THREAD_STATE,
                                        Arg.FRAME,
                                        ptr,
                                        i.rhs(1)
                                    },
                                    Inst.afterAASlow));
                        }
                        
                        slow.setFooter(
                            new Jump(i.di(),cont));
                        
                        i.prepend(
                            new SimpleInst(
                                i.di(),OpCode.Phi,
                                i.lhs(),new Arg[]{i.lhs()}));
                        
			i.remove();
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

