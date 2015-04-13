/*
 * LowerFeatures2.java
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

import java.io.*;
import com.fiji.fivm.*;
import java.nio.ByteOrder;

public class LowerFeatures2 extends CodePhase {
    public LowerFeatures2(Code code) { super(code); }
    
    // tests if objTypeData is a subtype of the type described by tid and
    // bucketAsPointer
    Arg generateSubtypeTest(Header h,Operation before,
			    Arg objTypeData,
			    Arg tid,
			    Arg bucketAsPointer) {
	Var ptr1=code.addVar(Exectype.POINTER);
	Var ptr2=code.addVar(Exectype.POINTER);
	Var candTid=code.addVar(Exectype.INT);
	Var pred=code.addVar(Exectype.INT);
        Arg epoch=TypeEpoch.load(objTypeData,code,before);
	before.prepend(
	    new CFieldInst(
		before.di(),OpCode.GetCField,
		ptr1,new Arg[]{epoch},
		CTypesystemReferences.TypeEpoch_buckets));
	before.prepend(
	    new SimpleInst(
		before.di(),OpCode.Add,
		ptr2,new Arg[]{ptr1,bucketAsPointer}));
	before.prepend(
	    new MemoryAccessInst(
		before.di(),OpCode.Load,
		candTid,new Arg[]{ptr2},
		Type.BYTE));
	before.prepend(
	    new SimpleInst(
		before.di(),OpCode.Eq,
		pred,new Arg[]{candTid,tid}));
	return pred;
    }
    
    // tests if ObjTypeData is a subtype of type
    Arg generateSubtypeTest(Header h,Operation before,
			    Arg objTypeData,Exectype objType,
			    Type type) {
        // this is SSA safe because it creates new Vars if needed
	if (Type.make(objType).isSubtypeOf(type)) {
	    return new IntConst(1);
	}
	if (!type.hasInstances()) {
	    return new IntConst(0);
	}
	if (type.hasClass() &&
            ((Global.analysis().open() || Settings.USE_TYPE_EPOCHS)?
             (type.isDeclaredFinal()):
             (type.getClazz().knownSubs.isEmpty()))) {
	    Var ourNum=code.addVar(Exectype.INT);
	    Var pred=code.addVar(Exectype.INT);
	    before.prepend(
		new CFieldInst(
		    before.di(),OpCode.GetCField,
		    ourNum,new Arg[]{objTypeData},
		    CTypesystemReferences.TypeData_canonicalNumber));
	    before.prepend(
		new SimpleInst(
		    before.di(),OpCode.Eq,
		    pred,
		    new Arg[]{
			ourNum,
			new IntConst(type.getClazz().canonicalNumber)
		    }));
	    return pred;
	}
        if (Settings.USE_TYPE_EPOCHS) {
            Var typeData=code.addVar(Exectype.POINTER);
            before.prepend(
                new TypeInst(
                    before.di(),OpCode.GetTypeData,
                    typeData,Arg.EMPTY,
                    type));
            return generateSubtypeTest(h,before,objTypeData,typeData);
        } else {
            return generateSubtypeTest(h,before,objTypeData,
                                       new IntConst(type.tid),
                                       new PointerConst(type.bucket));
        }
    }
    
    // tests if objTypeData is a subtype of typeData
    Arg generateSubtypeTest(Header h,Operation before,
			    Arg objTypeData,Arg typeData) {
        // this is SSA safe because it creates new Vars
	Var tid=code.addVar(Exectype.INT);
	Var bucket=code.addVar(Exectype.INT);
	Var bucketAsPointer=code.addVar(Exectype.POINTER);
        Arg epoch=TypeEpoch.load(typeData,code,before);
	before.prepend(
	    new CFieldInst(
		before.di(),OpCode.GetCField,
		tid,new Arg[]{epoch},
		CTypesystemReferences.TypeEpoch_tid));
	before.prepend(
	    new CFieldInst(
		before.di(),OpCode.GetCField,
		bucket,new Arg[]{epoch},
		CTypesystemReferences.TypeEpoch_bucket));
	before.prepend(
	    new TypeInst(
		before.di(),OpCode.Cast,
		bucketAsPointer,new Arg[]{bucket},
		Type.POINTER));
	return generateSubtypeTest(h,before,objTypeData,tid,bucketAsPointer);
    }
    
    static enum NullMode {
	NullPasses, NullFails
    }
    
    static abstract class SubGen {
	abstract void generate(Header h,Operation before,Var lhs);
    }

    // assume that lhs is set in the subGen's generated code ... so we
    // set it with an ipsilon
    void generateWithNullGuard(Header h,Operation before,Arg objVar,
			       NullMode nullMode,Var lhs,SubGen subGen) {
	Header body=h.split(before);
	Header cont=body.split(before);
	Header onNull=cont;
	if (lhs!=null) {
	    onNull=code.addHeader(before.di());
	    if (nullMode==NullMode.NullPasses) {
		onNull.append(new SimpleInst(before.di(),OpCode.Ipsilon,
					     lhs,new Arg[]{new IntConst(1)}));
	    } else {
		onNull.append(new SimpleInst(before.di(),OpCode.Ipsilon,
					     lhs,new Arg[]{new IntConst(0)}));
	    }
	    onNull.setFooter(new Jump(before.di(),cont));
	}
	h.setFooter(new Branch(before.di(),OpCode.BranchNonZero,
			       new Arg[]{objVar},
			       onNull,body));
	subGen.generate(body,body.getFooter(),lhs);
        if (lhs!=null) {
            before.prepend(
                new SimpleInst(
                    before.di(),OpCode.Phi,
                    lhs,new Arg[]{lhs}));
        }
    }
    
    public void visitCode() {
        assert code.isSSA();
        if (Global.analysis().open()) {
            assert Settings.USE_TYPE_EPOCHS;
        }
        
        // determine if we need any overflow buffers for patch points
        CLocal auxStateBufLocal=null;
        if (Settings.OPEN_PATCH_POINTS) {
            int maxState=0;
            for (Header h : code.headers()) {
                for (Instruction i : h.instructions()) {
                    if (i instanceof PatchPoint) {
                        PatchPoint pp=(PatchPoint)i;
                        maxState=Math.max(maxState,pp.stateSize());
                    }
                }
            }
            if (maxState>Constants.TS_STATE_BUF_LEN) {
                final int auxStateBufSize=maxState-Constants.TS_STATE_BUF_LEN;
                auxStateBufLocal=new CLocal(Basetype.VOID,null,"AuxStateBuf") {
                        public void generateDeclaration(PrintWriter w) {
                            w.println("   uintptr_t AuxStateBuf["+auxStateBufSize+"];");
                        }
                        protected int sizeofImpl() {
                            return auxStateBufSize*Global.pointerSize;
                        }
                    };
                code.addLocal(auxStateBufLocal);
            }
        }
        
        // now actually do the lowering
	for (Header h : code.headers2()) {
	    for (final Instruction i : h.instructions2()) {
		switch (i.opcode()) {
		case ArrayBoundsCheck: {
                    if (Settings.INTERNAL_INST) {
                        Var ptr=code.addVar(Exectype.POINTER);
                        i.prepend(
                            new TypeInst(
                                i.di(),OpCode.Cast,
                                ptr,new Arg[]{i.rhs(0)},
                                Type.POINTER));
                        i.prepend(
                            new CFieldInst(
                                i.di(),OpCode.Call,
                                Var.VOID,new Arg[]{
                                    Arg.THREAD_STATE,
                                    Arg.FRAME,
                                    ptr,
                                    i.rhs(1)
                                },
                                Inst.beforeABC));
                        i.append(
                            new CFieldInst(
                                i.di(),OpCode.Call,
                                Var.VOID,new Arg[]{
                                    Arg.THREAD_STATE,
                                    Arg.FRAME,
                                    ptr,
                                    i.rhs(1)
                                },
                                Inst.afterABC));
                    }
		    if (Global.profileArrayBounds) {
			StaticProfileCounterRepo.genInc(code,i,"Num uses of ArrayBoundsCheck");
		    }
		    Var predVar=code.addVar(Exectype.INT);
		    Var arrayLengthVar=code.addVar(Exectype.INT);
		    i.prepend(
			new SimpleInst(
                            i.di(),OpCode.ArrayLength,
                            arrayLengthVar,new Arg[]{i.rhs()[0]}));
		    i.prepend(
			new SimpleInst(
                            i.di(),OpCode.ULessThan,
                            predVar,new Arg[]{i.rhs()[1],arrayLengthVar}));
		    i.prepend(
			new TypeInst(
                            i.di(),OpCode.ThrowRTEOnZero,
                            Var.VOID,new Arg[]{predVar},
                            Type.parse(
                                getContext(),
                                "Ljava/lang/ArrayIndexOutOfBoundsException;").checkResolved()));
		    i.remove();
		    setChangedCode();
		    break;
		}
		case CheckInit: {
		    ClassInst ci=(ClassInst)i;
		    VisibleClass vc=ci.value();
		    assert vc.shouldCallCheckInit();
                    if (!Settings.FULL_CHECK_INIT_SEMANTICS) {
                        assert code.method()==null
                            || !code.method().getClazz().isSubclassOf(vc);
                    }
		    Var typeDataVar=code.addVar(Exectype.POINTER);
		    Var pointerVar=code.addVar(Exectype.POINTER);
		    Var predVar=code.addVar(Exectype.INT);
		    Var offsetVar=code.addVar(Exectype.POINTER);
		    Var offsetVar2=code.addVar(Exectype.POINTER);
		    if (Global.profileCheckInit) {
			StaticProfileCounterRepo.genInc(code,i,"Num uses of CheckInit");
		    }
		    i.prepend(
			new TypeInst(
			    i.di(),OpCode.GetTypeData,
			    typeDataVar,Arg.EMPTY,
			    Type.make(vc)));
		    i.prepend(
			new CFieldInst(
			    i.di(),OpCode.GetCFieldOffset,
			    offsetVar,Arg.EMPTY,
			    CTypesystemReferences.TypeData_inited));
		    i.prepend(
			new SimpleInst(
			    i.di(),OpCode.Add,
			    offsetVar2,new Arg[]{
				offsetVar,
				new PointerConst(
				    Global.endianness==ByteOrder.LITTLE_ENDIAN
				    ?0:3)
			    }));
		    i.prepend(
			new SimpleInst(
			    i.di(),OpCode.Add,
			    pointerVar,
			    new Arg[]{
				typeDataVar,
				offsetVar2
			    }));
		    i.prepend(
			new MemoryAccessInst(
			    i.di(),OpCode.Load,
			    predVar,new Arg[]{pointerVar},
			    Type.BYTE));
		    Header cont=h.split(i);
		    i.remove();
		    
		    Header initer=h.makeSimilar(i.di());
		    // FIXME: the downside here is that we're passing the
		    // ThreadState, but then fivmRuntime is squandering it
		    // during the call to the real checkInit function.
		    initer.append(
			new MethodInst(
			    i.di(),OpCode.InvokeStatic,
			    Var.VOID,new Arg[]{typeDataVar},
			    Runtime.checkInit));
		    initer.setFooter(new Jump(i.di(),cont));
		    
		    h.setFooter(
			new Branch(
			    i.di(),OpCode.BranchNonZero,
			    new Arg[]{predVar},initer,cont,
			    BranchPrediction.PREDICTING_TRUE,
			    PredictionStrength.SEMANTIC_PREDICTION));
		    
		    cont.prepend(
			new SimpleInst(
			    i.di(),OpCode.Fence,
			    Var.VOID,Arg.EMPTY));
		    setChangedCode();
		    break;
		}
		case Instanceof: {
		    if (Global.profileSubtype) {
			StaticProfileCounterRepo.genInc(code,i,"Num uses of Instanceof");
		    }
		    final Type t=((TypeInst)i).getType();
		    generateWithNullGuard(
			h,i,i.rhs()[0],NullMode.NullFails,i.lhs(),
			new SubGen() {
			    void generate(Header h,Operation before,Var lhs) {
				Arg result=generateSubtypeTest(
				    h,before,
				    loadTypeData(i.rhs()[0],before),
				    i.rhs()[0].type(),t);
				before.prepend(
				    new SimpleInst(before.di(),OpCode.Ipsilon,
						   lhs,new Arg[]{result}));
			    }
			});
		    i.remove();
		    setChangedCode();
		    break;
		}
		case ArrayCheckStore: {
		    if (Global.profileArrayStore) {
			StaticProfileCounterRepo.genInc(code,i,"Num uses of ArrayCheckStore");
		    }
		    // NOTE: this can be made more predictable using elementTid
		    // and elementBucket.
		    // NOTE2: this can be made faster on the fast path by caching
		    // the element's canonical number in the array type data
		    // (already done), and doing a comparison here.  it's simple:
		    // if we know that the exact type of the array matches the
		    // static type bound, then the check store is unnecessary.
		    // NOTE3: if the array type has isFinal()==true, take advantage
		    // of that.
		    generateWithNullGuard(
			h,i,i.rhs()[1],NullMode.NullPasses,null,
			new SubGen(){
			    void generate(Header h,Operation before,Var _) {
				Arg objTypeData=loadTypeData(i.rhs()[1],before);
				Arg arrTypeData=loadTypeData(i.rhs()[0],before);
				Var eleTypeData=code.addVar(Exectype.POINTER);
				before.prepend(
				    new CFieldInst(
					before.di(),OpCode.GetCField,
					eleTypeData,new Arg[]{arrTypeData},
					CTypesystemReferences.TypeData_arrayElement));
				Var eqPred=code.addVar(Exectype.INT);
				before.prepend(
				    new SimpleInst(
					before.di(),OpCode.Eq,
					eqPred,
					new Arg[]{
					    objTypeData,
					    eleTypeData
					}));
				Header body=h.split(before);
				Header cont=body.split(before);
				h.setFooter(
				    new Branch(
					before.di(),OpCode.BranchNonZero,
					new Arg[]{eqPred},
					body,cont));
				Arg result=
				    generateSubtypeTest(body,body.getFooter(),
							objTypeData,eleTypeData);
				body.append(
				    new TypeInst(
					before.di(),OpCode.ThrowRTEOnZero,
					Var.VOID,new Arg[]{result},
					Type.parse(
					    getContext(),
					    "Ljava/lang/ArrayStoreException;").checkResolved()));
			    }
			});
		    i.remove();
		    setChangedCode();
		    break;
		}
		case TypeCheck: {
                    if (Settings.INTERNAL_INST) {
                        Var ptr=code.addVar(Exectype.POINTER);
                        Var td=code.addVar(Exectype.POINTER);
                        i.prepend(
                            new TypeInst(
                                i.di(),OpCode.Cast,
                                ptr,new Arg[]{i.rhs(0)},
                                Type.POINTER));
                        i.prepend(
                            new TypeInst(
                                i.di(),OpCode.GetTypeData,
                                td,Arg.EMPTY,
                                ((TypeCheckInst)i).typeToCheck()));
                        i.prepend(
                            new CFieldInst(
                                i.di(),OpCode.Call,
                                Var.VOID,new Arg[]{
                                    Arg.THREAD_STATE,
                                    Arg.FRAME,
                                    ptr,
                                    td
                                },
                                Inst.beforeTC));
                        i.append(
                            new CFieldInst(
                                i.di(),OpCode.Call,
                                Var.VOID,new Arg[]{
                                    Arg.THREAD_STATE,
                                    Arg.FRAME,
                                    ptr,
                                    td
                                },
                                Inst.afterTC));
                    }
		    if (Global.profileSubtype) {
			StaticProfileCounterRepo.genInc(code,i,"Num uses of TypeCheck");
		    }
		    final Type t=((TypeCheckInst)i).typeToCheck();
		    final Type toThrow=((TypeCheckInst)i).typeToThrow();
		    generateWithNullGuard(
			h,i,i.rhs()[0],NullMode.NullPasses,null,
			new SubGen() {
			    void generate(Header h,Operation before,Var lhs) {
				Arg result=generateSubtypeTest(
				    h,before,
				    loadTypeData(i.rhs()[0],before),
				    i.rhs()[0].type(),t);
				before.prepend(
				    new TypeInst(
					i.di(),OpCode.ThrowRTEOnZero,
					Var.VOID,new Arg[]{result},
					toThrow));
			    }
			});
		    i.prepend(
                        new TypeInst(
                            i.di(),OpCode.Cast,
                            i.lhs(),i.rhs(),
                            t));
		    i.remove();
		    setChangedCode();
		    break;
		}
                case PatchPoint: {
                    PatchPoint pp=(PatchPoint)i;
                    if (Settings.CLOSED_PATCH_POINTS) {
                        Var typeName=code.addVar(Global.root().stringType.asExectype());
                        Var str=code.addVar(Global.root().stringType.asExectype());
                        if (pp.neededClass()==null) {
                            pp.prepend(
                                new SimpleInst(
                                    pp.di(),OpCode.Mov,
                                    typeName,new Arg[]{
                                        Arg.NULL
                                    }));
                        } else {
                            pp.prepend(
                                new GetStringInst(
                                    pp.di(),typeName,
                                    pp.neededClass().jniName()));
                        }
                        pp.prepend(
                            new GetStringInst(
                                pp.di(),str,pp.description()));
                        pp.prepend(
                            new MethodInst(
                                pp.di(),OpCode.InvokeStatic,
                                Var.VOID,new Arg[]{
                                    typeName,
                                    str
                                },
                                Runtime.throwNoClassDefFoundError));
                        // this is not reached ... but we indulge the compiler anyway
                        if (pp.lhs()!=Var.VOID) {
                            pp.prepend(
                                new SimpleInst(
                                    pp.di(),OpCode.Mov,
                                    pp.lhs(),
                                    Util.produceZero(
                                        code,
                                        pp.lhs().type().asType(),
                                        pp)));
                        }
                    } else {
                        // store the state
                        Var mainStateBuf=code.addVar(Exectype.POINTER);
                        Var auxStateBuf=null;
                        
                        pp.prepend(
                            new CFieldInst(
                                pp.di(),OpCode.GetCFieldAddress,
                                mainStateBuf,new Arg[]{Arg.THREAD_STATE},
                                CTypesystemReferences.ThreadState_stateBuf));
                        if (pp.stateSize()>Constants.TS_STATE_BUF_LEN) {
                            auxStateBuf=code.addVar(Exectype.POINTER);
                            pp.prepend(
                                new CFieldInst(
                                    pp.di(),OpCode.GetCVarAddress,
                                    auxStateBuf,Arg.EMPTY,
                                    auxStateBufLocal));
                            pp.prepend(
                                new CFieldInst(
                                    pp.di(),OpCode.PutCField,
                                    Var.VOID,new Arg[]{
                                        Arg.THREAD_STATE,
                                        auxStateBuf
                                    },
                                    CTypesystemReferences.ThreadState_stateBufOverflow));
                        }
                        
                        // what comes next *crucially* relies on the fact that we're not
                        // going to insert anymore pollchecks!

                        Arg next=null;
                        for (int j=0;j<pp.rhs().length;++j) {
                            Var bufVar;
                            int offset;
                            Arg value;
                            if (j<Constants.TS_STATE_BUF_LEN) {
                                bufVar=mainStateBuf;
                                offset=j;
                            } else {
                                bufVar=auxStateBuf;
                                offset=j-Constants.TS_STATE_BUF_LEN;
                            }
                            if (next==null) {
                                value=pp.rhs(j);
                            } else {
                                value=next;
                                next=null;
                            }
                            if (value==Arg.NIL) {
                                // nothing to do...
                            } else {
                                if (value.type().effectiveBasetype().cells==2 &&
                                    Global.pointerSize==4) {
                                    // split the value
                                    Var firstHalf=code.addVar(Exectype.POINTER);
                                    Var secondHalf=code.addVar(Exectype.POINTER);
                                    pp.prepend(
                                        new SimpleInst(
                                            pp.di(),OpCode.FirstHalf,
                                            firstHalf,new Arg[]{value}));
                                    pp.prepend(
                                        new SimpleInst(
                                            pp.di(),OpCode.SecondHalf,
                                            secondHalf,new Arg[]{value}));
                                    value=firstHalf;
                                    next=secondHalf;
                                }
                                Var offsetVar=code.addVar(Exectype.POINTER);
                                pp.prepend(
                                    new SimpleInst(
                                        pp.di(),OpCode.Add,
                                        offsetVar,new Arg[]{
                                            bufVar,
                                            PointerConst.make(offset*Global.pointerSize)
                                        }));
                                pp.prepend(
                                    new MemoryAccessInst(
                                        pp.di(),OpCode.Store,
                                        Var.VOID,new Arg[]{
                                            offsetVar,
                                            value
                                        },
                                        value.type().asType()));
                            }
                        }
                        
                        // ok now make the call
                        Var funcPtr=code.addVar(Exectype.POINTER);
                        pp.prepend(
                            new MemoryAccessInst(
                                pp.di(),OpCode.Load,
                                funcPtr,new Arg[]{
                                    PatchThunkRepo.generateIRFor(
                                        code,pp,pp.thunk())
                                },
                                Type.POINTER));
                        
                        Var result;
                        if (pp.lhs()==Var.VOID) {
                            result=Var.VOID;
                        } else {
                            result=code.addVar(
                                pp.lhs().type().effectiveBasetype().pointerifyObject.asExectype);
                        }
                        pp.prepend(
                            new CallIndirectInst(
                                pp.di(),OpCode.CallIndirect,
                                result,new Arg[]{
                                    funcPtr,
                                    Arg.THREAD_STATE
                                },
                                new NativeSignature(
                                    result.effectiveBasetype(),
                                    new Basetype[]{Basetype.POINTER})));
                        pp.prepend(
                            new SimpleInst(
                                pp.di(),OpCode.CheckException,
                                Var.VOID,Arg.EMPTY));
                        if (pp.lhs()!=Var.VOID) {
                            pp.prepend(
                                new TypeInst(
                                    pp.di(),OpCode.Cast,
                                    pp.lhs(),new Arg[]{result},
                                    pp.lhs().type().asType()));
                        }
                    }
                    pp.remove();
                    setChangedCode();
                    break;
                }
                case CompareL:
                case CompareG: {
                    Var pred1=code.addVar(Exectype.INT);
                    Var pred2=code.addVar(Exectype.INT);
                    Var negged=null;
                    if (i.opcode()==OpCode.CompareL) {
                        negged=code.addVar(Exectype.INT);
                    }
                    
                    Header cont=h.split(i);
                    Header first=h.makeSimilar(i.di());
                    Header second=h.makeSimilar(i.di());
                    
                    i.remove();
                    
                    h.append(
                        new SimpleInst(
                            i.di(),OpCode.LessThan,
                            pred1,new Arg[]{
                                i.opcode()==OpCode.CompareG?i.rhs(0):i.rhs(1),
                                i.opcode()==OpCode.CompareG?i.rhs(1):i.rhs(0)
                            }));
                    h.setFooter(
                        new Branch(
                            i.di(),OpCode.BranchNonZero,
                            new Arg[]{ pred1 },
                            second,
                            first));
                    
                    first.append(
                        new SimpleInst(
                            i.di(),OpCode.Ipsilon,
                            i.lhs(),new Arg[]{
                                IntConst.make(i.opcode()==OpCode.CompareG?-1:1)
                            }));
                    first.setFooter(
                        new Jump(
                            i.di(),cont));
                    
                    second.append(
                        new SimpleInst(
                            i.di(),OpCode.Neq,
                            pred2,new Arg[]{
                                i.rhs(0),
                                i.rhs(1)
                            }));
                    if (i.opcode()==OpCode.CompareG) {
                        second.append(
                            new SimpleInst(
                                i.di(),OpCode.Ipsilon,
                                i.lhs(),new Arg[]{ pred2 }));
                    } else {
                        second.append(
                            new SimpleInst(
                                i.di(),OpCode.Neg,
                                negged,new Arg[]{ pred2 }));
                        second.append(
                            new SimpleInst(
                                i.di(),OpCode.Ipsilon,
                                i.lhs(),new Arg[]{ negged }));
                    }
                    second.setFooter(
                        new Jump(
                            i.di(),cont));
                    
                    cont.prepend(
                        new SimpleInst(
                            i.di(),OpCode.Phi,
                            i.lhs(),new Arg[]{ i.lhs() }));
                    
                    setChangedCode();
                    break;
                }
		default: break;
		}
	    }
	}
	if (changedCode()) code.killAllAnalyses();
    }
}

