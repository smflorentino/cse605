/*
 * SimpleGenerateLocalFunction.java
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

import java.io.PrintWriter;
import com.fiji.fivm.Settings;

public class SimpleGenerateLocalFunction extends CodePhase {
    public SimpleGenerateLocalFunction(Code c) {
        super(c);
    }
    
    public void visitCode() {}
    
    static String s(double value) {
	if (Double.isNaN(value)) {
	    return "(0./0.)";
	} else if (Double.isInfinite(value)) {
	    if (value>0.) {
		return "(1./0.)";
	    } else {
		return "(-1./0.)";
	    }
	} else {
	    return ""+value;
	}
    }
    
    static String s(Var v) {
	return "var"+v.id;
    }
    
    static String s(Arg a) {
	if (a instanceof Var) {
	    return s((Var)a);
	} else if (a instanceof IntConst) {
	    return "INT32_C("+((IntConst)a).value()+")";
	} else if (a instanceof PointerConst) {
	    if (Global.pointerSize==4) {
		return "UINT32_C("+((PointerConst)a).value()+")";
	    } else {
		return "INT64_C("+((PointerConst)a).value()+")";
	    }
        } else if (a instanceof LongConst) {
            return "INT64_C("+((LongConst)a).value()+")";
        } else if (a instanceof FloatConst) {
            return "((float)"+((FloatConst)a).value()+")";
        } else if (a instanceof DoubleConst) {
            return "((double)"+((DoubleConst)a).value()+")";
        } else if (a==Arg.THREAD_STATE) {
            return "((uintptr_t)ts)";
        } else if (a==Arg.NULL) {
            return "((uintptr_t)0)";
	} else {
	    throw new Error("Unrecognized arg: "+a);
	}
    }
    
    static String ss(Arg a) {
	// note that this just works for char because char becomes int
	return "(("+a.effectiveBasetype().signedCType.asCCode()+")"+s(a)+")";
    }
    
    static String us(Arg a) {
	return "(("+a.effectiveBasetype().unsignedCType.asCCode()+")"+s(a)+")";
    }
    
    static String shiftMaskForType(Exectype t) {
	switch (t.effectiveBasetype().preciseIntType) {
	case INT: return "0x1f";
	case LONG: return "0x3f";
	default: throw new Error("wrong type for shifting "+t);
	}
    }
    
    static String shiftMask(String arg,Exectype t) {
	return "("+arg+"&"+shiftMaskForType(t)+")";
    }
    
    static String catchBlock(ExceptionHandler eh) {
        if (eh==null) {
            return "EB";
        } else {
            return "BB"+eh.target().order;
        }
    }
    
    static String catchBlock(Header h) {
        return catchBlock(h.handler());
    }
    
    static String callParams(Basetype[] params,Arg[] args,int argsOffset) {
        StringBuilder buf=new StringBuilder();
        for (int i=argsOffset;i<argsOffset+params.length;++i) {
            if (i!=argsOffset) {
                buf.append(", ");
            }
            if (args[i] instanceof IntConst) {
                buf.append(((IntConst)args[i]).value());
            } else {
                buf.append("("+params[i-argsOffset].cTypeForCall.asCCode()+")"+s(args[i]));
            }
        }
        return buf.toString();
    }
    
    static String callParams(VisibleMethod vm,Arg[] args,int argsOffset) {
        StringBuilder buf=new StringBuilder();
        buf.append("ts");
        if (vm.alloc()==AllocationMechanism.ALLOC_AS_CALLER) {
            buf.append(", 0");
        }
        if (Global.makeBareParams(vm.getAllParams()).length!=0) {
            buf.append(", ");
        }
        buf.append(callParams(Global.makeBareParams(vm.getAllParams()),args,argsOffset));
        return buf.toString();
    }
    
    static void dumpExcCheck(PrintWriter w,Header h,VisibleMethod vm) {
        if (vm.canThrow()) {
            w.println("   if (ts->curException) {");
            w.println("      goto "+catchBlock(h)+";");
            w.println("   }");
        }
    }
    
    static void dumpLhsPart(PrintWriter w,Operation o) {
        w.print("   ");
        if (o.hasLhs()) {
            w.print(s(((Instruction)o).lhs())+" = ");
        }
    }
    
    public LocalFunction localFunction() {
	return new LocalFunction(code.cname(),
				 code.cresult(),
				 code.cparams(),
				 code) {
	    public LinkableSet subLinkables() {
		LinkableSet result=new LinkableSet();
		for (Header h : code.headers()) {
		    for (Instruction i : h.instructions()) {
			if (i instanceof CFieldInst &&
			    ((CFieldInst)i).field() instanceof Linkable) {
			    result.addAll(
				((Linkable)((CFieldInst)i).field()).linkables());
			}
                        switch (i.opcode()) {
                        case TypeCheck: {
                            TypeCheckInst tci=(TypeCheckInst)i;
                            result.add(TypeData.forType(tci.typeToCheck()));
                            break;
                        }
                        case Instanceof:
                        case New:
                        case NewArray:
                        case GetType: {
                            TypeInst ti=(TypeInst)i;
                            result.add(TypeData.forType(ti.getType()));
                            break;
                        }
                        case CheckInit: {
                            ClassInst ci=(ClassInst)i;
                            result.add(TypeData.forType(ci.value().asType()));
                            break;
                        }
                        case Invoke:
                        case InvokeStatic: {
                            MethodInst mi=(MethodInst)i;
                            result.add(mi.method().asRemoteFunction());
                            break;
                        }
                        case InvokeDynamic: {
                            MethodInst mi=(MethodInst)i;
                            if (mi.method().isDeclaredFinal()) {
                                result.add(mi.method().asRemoteFunction());
                            } else if (mi.method().getClazz().isInterface()) {
                                result.add(
                                    new PoundDefine(
                                        Basetype.POINTER,
                                        mi.method().localMangledName()+"_itableIndex",
                                        "("+mi.method().getItableIndex()+")"));
                            } else {
                                result.add(
                                    new PoundDefine(
                                        Basetype.POINTER,
                                        mi.method().localMangledName()+"_vtableIndex",
                                        "("+mi.method().getVtableIndex()+")"));
                            }
                            break;
                        }
                        case WeakCASStatic:
                        case GetStatic:
                        case PutStatic: {
                            HeapAccessInst hai=(HeapAccessInst)i;
                            result.add(
                                new PoundDefine(
                                    Basetype.POINTER,
                                    hai.fieldField().localMangledName()+"_staticFieldOffset",
                                    "("+StaticFieldRepo.offsetForField(hai.fieldField()).asCCode()+")"));
                            break;
                        }
                        case PutField:
                        case GetField:
                        case WeakCASField: {
                            HeapAccessInst hai=(HeapAccessInst)i;
                            result.add(
                                new PoundDefine(
                                    Basetype.POINTER,
                                    hai.fieldField().localMangledName()+"_instFieldOffset",
                                    "("+hai.fieldField().offset()+")"));
                            break;
                        }
                        case GetString: {
                            GetStringInst gsi=(GetStringInst)i;
                            result.add(
                                new PoundDefine(
                                    Basetype.POINTER,
                                    StringRepository.mangledName(gsi.value()),
                                    "("+StringRepository.register(gsi.value())+")"));
                            break;
                        }
                        default: break;
                        }
		    }
		}
		return result;
	    }
            
            String throwFunction(Type t) {
                if (t==Runtime.arithmeticException.asType()) {
                    return "fivmr_throwArithmeticRTE_inJava";
                } else if (t==Runtime.nullPointerException.asType()) {
                    return "fivmr_throwNullPointerRTE_inJava";
                } else if (t==Runtime.arrayIndexOutOfBoundsException.asType()) {
                    return "fivmr_throwArrayBoundsRTE_inJava";
                } else if (t==Runtime.arrayStoreException.asType()) {
                    return "fivmr_throwArrayStoreRTE_inJava";
                } else if (t==Runtime.negativeArraySizeException.asType()) {
                    return "fivmr_throwNegativeSizeRTE_inJava";
                } else if (t==Runtime.classCastException.asType()) {
                    return "fivmr_throwClassCastRTE_inJava";
                } else if (t==Runtime.incompatibleClassChangeError.asType()) {
                    return "fivmr_throwClassChangeRTE_inJava";
                } else if (t==Runtime.illegalAssignmentError.asType()) {
                    return "fivmr_throwIllegalAssignmentError_inJava";
                } else {
                    throw new CompilerException("Unknown RTE type: "+t);
                }
            }

	    public void generateCode(PrintWriter w) {
                w.println("   /* "+code.shortName()+" */");
		code.recomputeOrder();
                if (Settings.INTERNAL_INST) {
                    w.println("   FIVMR_II_LOCAL_DECLS");
                }
		for (CLocal l : code.locals()) {
		    l.generateDeclaration(w);
		}
                w.println("   fivmr_ThreadState *ts;");
		for (Var v : code.vars()) {
		    w.println("   "+v.type().cType().asCCode()+" "+s(v)+";  /* "+v.type().jniName()+" */");
		}
                w.println("   ts=(fivmr_ThreadState*)arg0;");
                if (Settings.STACK_GROWS_DOWN) {
                    w.println("   if (((uintptr_t)&ts) < ts->stackLimit) {");
                } else {
                    w.println("   if (((uintptr_t)&ts) > ts->stackLimit) {");
                }
                w.println("      fivmr_throwStackOverflowRTE_inJava(ts);");
                w.println("      if (ts->curException) {");
                w.println("         goto EB;");
                w.println("      }");
                w.println("   }");
		PredsCalc pc=code.getPreds();
		for (Header h : code.headersRootFirst()) {
		    if (pc.allPredecessors(h).iterator().hasNext()) {
			w.println("BB"+h.order+":  /* "+h.di().origin().owner().getSourceFilename()+":"+h.di().lineNumber()+" */");
		    }
		    for (Operation o : h.operations()) {
			// w.println("   /* "+o.di()+" */");
                        // what we need:
                        // - pollchecks?  insert before we get here, or...?  punt for now (no GC, no stack traces)
                        // - reference maps ... generate here, or before?  punt for now (no GC, no stack traces)
                        // - exceptions, including RTEs: lower in one step.
                        // - type checks: use the C functions for doing it
                        // - method resolution: use the C functions for doing it.
                        w.println("   /* "+h.order+"."+o.order+" "+o.di().origin().owner().getSourceFilename()+":"+o.di().lineNumber+" */");
                        switch (o.opcode()) {
                        case ThrowRTEOnZero: {
                            TypeInst ti=(TypeInst)o;
                            w.println("   if (fivmr_semantically_unlikely(!("+s(o.rhs(0))+"))) {");
                            w.println("      "+throwFunction(ti.getType())+"(ts);");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            break;
                        }
                        case ArrayBoundsCheck:
                            w.println("   if (fivmr_semantically_unlikely(!fivmr_Object_checkArrayBounds(ts,"+s(o.rhs(0))+","+s(o.rhs(1))+"))) {");
                            w.println("      fivmr_throwArrayBoundsRTE_inJava(ts);");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            break;
                        case ArrayCheckStore:
                            w.println("   if (fivmr_semantically_unlikely(!fivmr_Object_isSubtypeOfArrayElementOrNullFast(ts,"+s(o.rhs(1))+","+s(o.rhs(0))+"))) {");
                            w.println("      fivmr_throwArrayStoreRTE_inJava(ts);");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            break;
                        case TypeCheck: {
                            TypeCheckInst tci=(TypeCheckInst)o;
                            w.println("   if (fivmr_semantically_unlikely(!fivmr_Object_isSubtypeOfOrNullFast(ts,"+s(o.rhs(0))+","+TypeData.forType(tci.typeToCheck()).asCCode()+"))) {");
                            w.println("      "+throwFunction(tci.typeToThrow())+"(ts);");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            dumpLhsPart(w,o);
                            w.println(s(o.rhs(0))+";");
                            break;
                        }
                        case CheckInit: {
                            ClassInst ci=(ClassInst)o;
                            w.println("   if (fivmr_semantically_unlikely(!fivmr_TypeData_checkInitFast(ts,"+TypeData.forType(ci.value().asType()).asCCode()+"))) {");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            break;
                        }
                        case PatchPoint: {
                            PatchPoint pp=(PatchPoint)o;
                            w.print("   fivmr_throwNoClassDefFoundError_inJava(ts,");
                            if (pp.neededClass()==null) {
                                w.print("NULL,");
                            } else {
                                w.print("\""+Util.cStringEscape(pp.neededClass().jniName())+"\",");
                            }
                            w.println("\""+Util.cStringEscape(pp.description())+"\");");
                            w.println("   goto "+catchBlock(h)+";");
                            break;
                        }
			case Switch: {
			    Switch sw=(Switch)o;
			    w.println("   switch ("+s(sw.rhs(0))+") {");
			    for (int i=0;i<sw.numCases();++i) {
				w.println("   case "+sw.value(i)+": goto BB"+
					  sw.target(i).order+";");
			    }
			    w.println("   default: goto BB"+
				      ((Header)sw.next).order+";");
			    w.println("   }");
			    break;
                        }
                        case Invoke:
                        case InvokeStatic: {
                            MethodInst mi=(MethodInst)o;
                            VisibleMethod vm=mi.staticTarget();
                            RemoteFunction rf=vm.asRemoteFunction();
                            dumpLhsPart(w,o);
                            w.print(rf.getName()+"(");
                            w.print(callParams(vm,mi.rhs(),0));
                            w.println(");");
                            dumpExcCheck(w,h,vm);
                            break;
                        }
                        case InvokeDynamic: {
                            MethodInst mi=(MethodInst)o;
                            VisibleMethod vm=mi.method();
                            dumpLhsPart(w,o);
                            if (vm.isDeclaredFinal()) {
                                RemoteFunction rf=vm.asRemoteFunction();
                                w.print(rf.getName()+"(");
                            } else if (vm.getClazz().isInterface()) {
                                w.print("(("+vm.getNativeSignature().ctype()+")fivmr_Object_resolveInterfaceCall(ts,"+s(o.rhs(0))+","+vm.localMangledName()+"_itableIndex))(");
                            } else {
                                w.print("(("+vm.getNativeSignature().ctype()+")fivmr_Object_resolveVirtualCall(ts,"+s(o.rhs(0))+","+vm.localMangledName()+"_vtableIndex))(");
                            }
                            w.print(callParams(vm,mi.rhs(),0));
                            w.println(");");
                            dumpExcCheck(w,h,vm);
                            break;
                        }
                        case New: {
                            TypeInst ti=(TypeInst)o;
                            w.println("   if (fivmr_semantically_unlikely(!("+s(ti.lhs())+" = fivmr_alloc(ts,0,"+TypeData.forType(ti.getType()).asCCode()+")))) {");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            break;
                        }
                        case NewArray: {
                            TypeInst ti=(TypeInst)o;
                            w.println("   if (fivmr_semantically_unlikely(!("+s(ti.lhs())+" = fivmr_allocArray(ts,0,"+TypeData.forType(ti.getType()).asCCode()+","+s(ti.rhs(0))+")))) {");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            break;
                        }
                        case MonitorEnter: {
                            w.println("   if (fivmr_semantically_unlikely(!fivmr_Object_lock(ts,"+s(o.rhs(0))+"))) {");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            break;
                        }
                        case MonitorExit: {
                            w.println("   if (fivmr_semantically_unlikely(!fivmr_Object_unlock(ts,"+s(o.rhs(0))+"))) {");
                            w.println("      goto "+catchBlock(h)+";");
                            w.println("   }");
                            break;
                        }
                        case Throw: {
                            w.println("   fivmr_throw(ts,"+s(o.rhs(0))+");");
                            w.println("   goto "+catchBlock(h)+";");
                            break;
                        }
                        default: {
                            w.print("   ");
                            if (o.hasLhs()) {
                                w.print(s(((Instruction)o).lhs())+" = (");
                            }
                            switch (o.opcode()) {
                            case GetArg:
                                w.print("arg"+(((ArgInst)o).getIdx()+1));
                                break;
                            case GetException:
                                w.print("ts->curException");
                                break;
                            case ClearException:
                                w.print("ts->curException=0");
                                break;
                            case GetType: {
                                TypeInst ti=(TypeInst)o;
                                w.print("fivmr_TypeData_asClass("+TypeData.forType(ti.getType()).asCCode()+")");
                                break;
                            }
                            case GetString: {
                                GetStringInst gsi=(GetStringInst)o;
                                w.print("fivmr_getString2(ts,"+StringRepository.mangledName(gsi.value())+")");
                                break;
                            }
                            case Instanceof: {
                                TypeInst ti=(TypeInst)o;
                                w.print("fivmr_Object_isSubtypeOfAndNonNullFast(ts,"+s(o.rhs(0))+","+TypeData.forType(ti.getType()).asCCode()+")");
                                break;
                            }
                            case WeakCASStatic: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.weakCASStatic2(hai.fieldType()).getName()+"(ts,"+hai.fieldField().localMangledName()+"_staticFieldOffset,"+s(o.rhs(0))+","+s(o.rhs(1))+","+hai.runtimeFlags()+")");
                                break;
                            }
                            case GetStatic: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.getStatic2(hai.fieldType()).getName()+"(ts,"+hai.fieldField().localMangledName()+"_staticFieldOffset,"+hai.runtimeFlags()+")");
                                break;
                            }
                            case PutStatic: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.putStatic2(hai.fieldType()).getName()+"(ts,"+hai.fieldField().localMangledName()+"_staticFieldOffset,"+s(o.rhs(0))+","+hai.runtimeFlags()+")");
                                break;
                            }
                            case PutField: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.putField(hai.fieldType()).getName()+"(ts,"+s(o.rhs(0))+","+hai.fieldField().localMangledName()+"_instFieldOffset,"+s(o.rhs(1))+","+hai.runtimeFlags()+")");
                                break;
                            }
                            case GetField: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.getField(hai.fieldType()).getName()+"(ts,"+s(o.rhs(0))+","+hai.fieldField().localMangledName()+"_instFieldOffset,"+hai.runtimeFlags()+")");
                                break;
                            }
                            case WeakCASField: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.weakCASField(hai.fieldType()).getName()+"(ts,"+s(o.rhs(0))+","+hai.fieldField().localMangledName()+"_instFieldOffset,"+s(o.rhs(1))+","+s(o.rhs(2))+","+hai.runtimeFlags()+")");
                                break;
                            }
                            case ArrayLength:
                                w.print(CTypesystemReferences.arrayLength_barrier.getName()+"(ts,"+s(o.rhs(0))+",0)");
                                break;
                            case ArrayLoad: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.arrayLoad(hai.fieldType()).getName()+"(ts,"+s(o.rhs(0))+","+s(o.rhs(1))+","+hai.runtimeFlags()+")");
                                break;
                            }
                            case ArrayStore: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.arrayStore(hai.fieldType()).getName()+"(ts,"+s(o.rhs(0))+","+s(o.rhs(1))+","+s(o.rhs(2))+","+hai.runtimeFlags()+")");
                                break;
                            }
                            case WeakCASElement: {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                w.print(CBarriers.arrayWeakCAS(hai.fieldType()).getName()+"(ts,"+s(o.rhs(0))+","+s(o.rhs(1))+","+s(o.rhs(2))+","+s(o.rhs(3))+","+hai.runtimeFlags());
                                break;
                            }
                            case Mov:
                                w.print(s(o.rhs(0)));
                                break;
                            case Cast:
                                w.print("("+((TypeInst)o).getType().cType().asCCode()+
                                        ")"+s(o.rhs(0)));
                                break;
                            case IntToPointerZeroFill:
                                w.print("(uintptr_t)(uint32_t)"+s(o.rhs(0)));
                                break;
                            case Add:
                                w.print(s(o.rhs(0))+"+"+s(o.rhs(1)));
                                break;
                            case Sub:
                                w.print(s(o.rhs(0))+"-"+s(o.rhs(1)));
                                break;
                            case Mul:
                                w.print(s(o.rhs(0))+"*"+s(o.rhs(1)));
                                break;
                            case Div:
                                w.print(s(o.rhs(0))+"/"+s(o.rhs(1)));
                                break;
                            case Mod:
                                if (o.rhs(0).type()==Exectype.FLOAT) {
                                    w.print("fmodf("+s(o.rhs(0))+","+s(o.rhs(1))+")");
                                } else if (o.rhs(0).type()==Exectype.DOUBLE) {
                                    w.print("fmod("+s(o.rhs(0))+","+s(o.rhs(1))+")");
                                } else {
                                    w.print(s(o.rhs(0))+"%"+s(o.rhs(1)));
                                }
                                break;
                            case Neg:
                                w.print("-"+s(o.rhs(0)));
                                break;
                            case Shl:
                                w.print(s(o.rhs(0))+"<<"+shiftMask(s(o.rhs(1)),o.rhs(0).type()));
                                break;
                            case Shr:
                                w.print(ss(o.rhs(0))+">>"+shiftMask(ss(o.rhs(1)),o.rhs(0).type()));
                                break;
                            case Ushr:
                                w.print(us(o.rhs(0))+">>"+shiftMask(us(o.rhs(1)),o.rhs(0).type()));
                                break;
                            case And:
                                w.print(s(o.rhs(0))+"&"+s(o.rhs(1)));
                                break;
                            case Or:
                                w.print(s(o.rhs(0))+"|"+s(o.rhs(1)));
                                break;
                            case Xor:
                                w.print(s(o.rhs(0))+"^"+s(o.rhs(1)));
                                break;
                            case CompareG:
                                w.print(s(o.rhs(0))+"<"+s(o.rhs(1))+"?-1:("+
                                        s(o.rhs(0))+"=="+s(o.rhs(1))+"?0:1)");
                                break;
                            case CompareL:
                                w.print(s(o.rhs(0))+">"+s(o.rhs(1))+"?1:("+
                                        s(o.rhs(0))+"=="+s(o.rhs(1))+"?0:-1)");
                                break;
                            case LessThan:
                                w.print(ss(o.rhs(0))+"<"+ss(o.rhs(1)));
                                break;
                            case ULessThan:
                                w.print(us(o.rhs(0))+"<"+us(o.rhs(1)));
                                break;
                            case Eq:
                                w.print(s(o.rhs(0))+"=="+s(o.rhs(1)));
                                break;
                            case Not:
                                w.print("!"+s(o.rhs(0)));
                                break;
                            case BitNot:
                                w.print("~"+s(o.rhs(0)));
                                break;
                            case Store:
                                w.print("*(("+((MemoryAccessInst)o).getType().cType().asCCode()+"*)"+
                                        s(o.rhs(0))+") = ("+
                                        ((MemoryAccessInst)o).getType().cType().asCCode()+")"+
                                        s(o.rhs(1)));
                                break;
                            case Load:
                                w.print("("+((MemoryAccessInst)o).getExectype().cType().asCCode()+
                                        ") *("+((MemoryAccessInst)o).getType().cType().asCCode()+"*)"+
                                        s(o.rhs(0)));
                                break;
                            case StrongLoadCAS:
                                switch (((MemoryAccessInst)o).effectiveBasetype()) {
                                case INT:
                                    w.print("fivmr_cas32_load((int32_t*)"+
                                            s(o.rhs(0))+", "+s(o.rhs(1))+", "+
                                            s(o.rhs(2))+")");
                                    break;
                                case POINTER:
                                    w.print("fivmr_cas_load((uintptr_t*)"+
                                            s(o.rhs(0))+", "+s(o.rhs(1))+", "+
                                            s(o.rhs(2))+")");
                                    break;
                                default: throw new Error("bad type for cas: "+o);
                                }
                                break;
                            case StrongCAS:
                                switch (((MemoryAccessInst)o).effectiveBasetype()) {
                                case INT:
                                    w.print("(int32_t)fivmr_cas32((int32_t*)"+
                                            s(o.rhs(0))+", "+s(o.rhs(1))+", "+
                                            s(o.rhs(2))+")");
                                    break;
                                case POINTER:
                                    w.print("(int32_t)fivmr_cas((uintptr_t*)"+
                                            s(o.rhs(0))+", "+s(o.rhs(1))+", "+
                                            s(o.rhs(2))+")");
                                    break;
                                default: throw new Error("bad type for cas: "+o);
                                }
                                break;
                            case WeakCAS:
                                switch (((MemoryAccessInst)o).effectiveBasetype()) {
                                case INT:
                                    w.print("(int32_t)fivmr_cas32_weak((int32_t*)"+
                                            s(o.rhs(0))+", "+s(o.rhs(1))+", "+
                                            s(o.rhs(2))+")");
                                    break;
                                case POINTER:
                                    w.print("(int32_t)fivmr_cas_weak((uintptr_t*)"+
                                            s(o.rhs(0))+", "+s(o.rhs(1))+", "+
                                            s(o.rhs(2))+")");
                                    break;
                                default: throw new Error("bad type for cas: "+o);
                                }
                                break;
                            case StrongVoidCAS:
                                switch (((MemoryAccessInst)o).effectiveBasetype()) {
                                case INT:
                                    w.print("fivmr_cas32_void((int32_t*)"+
                                            s(o.rhs(0))+", "+s(o.rhs(1))+", "+
                                            s(o.rhs(2))+")");
                                    break;
                                case POINTER:
                                    w.print("fivmr_cas_void((uintptr_t*)"+
                                            s(o.rhs(0))+", "+s(o.rhs(1))+", "+
                                            s(o.rhs(2))+")");
                                    break;
                                default: throw new Error("bad type for cas: "+o);
                                }
                                break;
                            case Fence:
                                w.print("fivmr_fence()");
                                break;
                            case HardCompilerFence:
                                w.print("fivmr_compilerFence()");
                                break;
                            case HardUse:
                                break;
                            case PutCField: {
                                CFieldInst cfi=(CFieldInst)o;
                                CStructField f=(CStructField)cfi.field();
                                w.print("(("+f.getStructName()+
                                        "*) "+s(o.rhs(0))+")->"+
                                        f.getName()+" = ("+f.getType().cType.asCCode()+")"+
                                        s(o.rhs(1)));
                                break;
                            }
                            case GetCField: {
                                CFieldInst cfi=(CFieldInst)o;
                                CStructField f=(CStructField)cfi.field();
                                w.print("("+
                                        Exectype.translate(f.getType()).cType.asCCode()+
                                        ") (("+f.getStructName()+
                                        "*) "+s(o.rhs(0))+")->"+f.getName());
                                break;
                            }
                            case GetCFieldAddress: {
                                CFieldInst cfi=(CFieldInst)o;
                                CStructField f=(CStructField)cfi.field();
                                w.print("(uintptr_t) &(("+f.getStructName()+
                                        "*) "+s(o.rhs(0))+")->"+f.getName());
                                break;
                            }
                            case GetCFieldOffset: {
                                CFieldInst cfi=(CFieldInst)o;
                                CStructField f=(CStructField)cfi.field();
                                w.print("fivmr_offsetof("+f.getStructName()+
                                        ", "+f.getName()+")");
                                break;
                            }
                            case GetCTypeSize: {
                                CTypeInst cti=(CTypeInst)o;
                                w.print("(uintptr_t)sizeof("+cti.ctype()+")");
                                break;
                            }
                            case PutCVar: {
                                CFieldInst cfi=(CFieldInst)o;
                                CField f=cfi.field();
                                w.print(f.getName()+" = ("+f.getType().cType.asCCode()+")"+
                                        s(o.rhs(0)));
                                break;
                            }
                            case GetCVar: {
                                CFieldInst cfi=(CFieldInst)o;
                                CField f=cfi.field();
                                w.print("("+
                                        Exectype.translate(f.getType()).cType.asCCode()+
                                        ")"+f.getName());
                                break;
                            }
                            case GetCVarAddress:
                                w.print("(uintptr_t)&"+((CFieldInst)o).getName());
                                break;
                            case Call: {
                                CFieldInst cfi=(CFieldInst)o;
                                Function f=(Function)cfi.field();
                                if (f.getResult()!=Basetype.VOID) {
                                    w.print("("+Exectype.translate(f.getResult()).cType.asCCode()+")");
                                }
                                w.print(f.getName()+"(");
                                w.print(callParams(f.getParams(),o.rhs(),0));
                                w.print(")");
                                break;
                            }
                            case CallIndirect: {
                                CallIndirectInst cii=(CallIndirectInst)o;
                                if (cii.result()!=Basetype.VOID) {
                                    w.print("("+Exectype.translate(cii.result()).cType.asCCode()+")");
                                }
                                w.print("(("+cii.signature().ctype()+")"+
                                        s(o.rhs(0))+")(");
                                w.print(callParams(cii.signature().params(),o.rhs(),1));
                                w.print(")");
                                break;
                            }
                            case FirstHalf: {
                                if (o.rhs(0).type()==Exectype.LONG) {
                                    w.print("(fivmr_firstHalfLong("+s(o.rhs(0))+"))");
                                } else {
                                    w.print("(fivmr_firstHalfDouble("+s(o.rhs(0))+"))");
                                }
                                break;
                            }
                            case SecondHalf: {
                                if (o.rhs(0).type()==Exectype.LONG) {
                                    w.print("(fivmr_secondHalfLong("+s(o.rhs(0))+"))");
                                } else {
                                    w.print("(fivmr_secondHalfDouble("+s(o.rhs(0))+"))");
                                }
                                break;
                            }
                            case Return:
                            case RawReturn:
                                w.print("return");
                                if (o.rhs().length==1) {
                                    w.print(" ("+code.cresult().cType.asCCode()+
                                            ")"+s(o.rhs(0)));
                                }
                                break;
                            case NotReached:
                                w.print("abort()");
                                break;
                            case Jump:
                                w.print("goto BB"+((Header)o.next).order);
                                break;
                            case BranchNonZero:
                            case BranchZero: {
                                Branch b=(Branch)o;
                                w.print("if (");
                                switch (b.prediction()) {
                                case NO_PREDICTION:
                                    break;
                                case PREDICTING_TRUE:
                                    if (b.strength()==PredictionStrength.SEMANTIC_PREDICTION) {
                                        w.print("fivmr_semantically_likely");
                                    } else {
                                        w.print("fivmr_likely");
                                    }
                                    break;
                                case PREDICTING_FALSE:
                                    if (b.strength()==PredictionStrength.SEMANTIC_PREDICTION) {
                                        w.print("fivmr_semantically_unlikely");
                                    } else {
                                        w.print("fivmr_unlikely");
                                    }
                                    break;
                                default: throw new Error();
                                }
                                w.print("(");
                                if (o.opcode()==OpCode.BranchZero) {
                                    w.print("!");
                                }
                                w.print(s(o.rhs(0))+")) goto BB"+
                                        b.target().order+
                                        "; else goto BB"+((Header)o.next).order);
                                break;
                            }
                            default: throw new Error("bad operation: "+o);
                            }
                            if (o.hasLhs()) {
                                w.print(")");
                            }
                            w.println(";");
                            break;
                        }}
		    }
		}
                w.println("EB:");
                if (getResult()==Basetype.VOID) {
                    w.println("   return;");
                } else {
                    w.println("   return 0;");
                }
	    }
	};
    }
}

