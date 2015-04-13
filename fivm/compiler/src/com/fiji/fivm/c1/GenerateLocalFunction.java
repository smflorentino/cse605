/*
 * GenerateLocalFunction.java
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

public class GenerateLocalFunction extends CodePhase {
    public GenerateLocalFunction(Code c) {
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
	return "var"+v.permID;
    }
    
    static String floatStr(double d) {
        if (d!=d) {
            return "(0.0/0.0)";
        } else if (d*2.0==d && d!=0.0) {
            if (d<0.0) {
                return "(-1.0/0.0)";
            } else {
                return "(1.0/0.0)";
            }
        } else {
            return ""+d;
        }
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
	    return "((float)"+floatStr(((FloatConst)a).value())+")";
	} else if (a instanceof DoubleConst) {
	    return "((double)"+floatStr(((DoubleConst)a).value())+")";
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
    
    static String upFirst(String s) {
        return ""+Character.toUpperCase(s.charAt(0))+s.substring(1);
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
		    }
		}
		return result;
	    }

	    public void generateCode(PrintWriter w) {
                w.println("   /* "+code.shortName()+" */");
		code.recomputeOrder();
                HeaderProximityCalc hpc=new HeaderProximityCalc(code);
                if (Settings.INTERNAL_INST) {
                    w.println("   FIVMR_II_LOCAL_DECLS");
                }
		for (CLocal l : code.locals()) {
		    l.generateDeclaration(w);
		}
		for (Var v : code.vars()) {
		    w.println("   "+v.type().cType().asCCode()+" "+s(v)+";  /* "+v.origType().shortName()+" */");
		}
		PredsCalc pc=code.getPreds();
		for (Header h : code.headersRootFirst()) {
		    if (!pc.normalPredecessors(h).isEmpty()) {
			w.println("B"+h.order+"B:   /* frequency = "+h.frequency()+" */");
                        w.println("   /* proximity: "+hpc.proximity(h)+" */");
                        //w.println("   /* "+h.di().origin()+" */");
                        //w.println("   /* line number: "+h.di().lineNumber()+" */");
		    }
                    DebugInfo lastDI=null;
		    for (Operation o : h.operations()) {
			// w.println("   /* "+o.di()+" */");
                        if (o.di()!=lastDI) {
                            w.println("   /* "+o.di().shortName()+" */");
                            lastDI=o.di();
                        }
			w.print("   ");
			if (o.hasLhs()) {
			    w.print(s(((Instruction)o).lhs())+" = (");
			}
			switch (o.opcode()) {
			case Mov:
			    w.print(s(o.rhs(0)));
			    break;
			case Cast:
			    w.print("("+((TypeInst)o).getType().cType().asCCode()+
				    ")"+s(o.rhs(0)));
			    break;
                        case Fiat:
                            w.print("fivmr_fiat"+upFirst(o.rhs(0).effectiveBasetype().toString)+
                                    "To"+upFirst(((TypeInst)o).getType().effectiveBasetype().toString)+
                                    "("+s(o.rhs(0))+")");
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
			case LessThanEq:
			    w.print(ss(o.rhs(0))+"<="+ss(o.rhs(1)));
			    break;
			case ULessThan:
			    w.print(us(o.rhs(0))+"<"+us(o.rhs(1)));
			    break;
			case ULessThanEq:
			    w.print(us(o.rhs(0))+"<="+us(o.rhs(1)));
			    break;
			case Eq:
			    w.print(s(o.rhs(0))+"=="+s(o.rhs(1)));
			    break;
			case Neq:
			    w.print(s(o.rhs(0))+"!="+s(o.rhs(1)));
			    break;
			case Not:
			    w.print("!"+s(o.rhs(0)));
			    break;
			case Boolify:
			    w.print("!!"+s(o.rhs(0)));
			    break;
			case BitNot:
			    w.print("~"+s(o.rhs(0)));
			    break;
			case Store:
			    w.print("*(("+((MemoryAccessInst)o).getType().cType().asCCode()+
                                    " "+((MemoryAccessInst)o).volatility().asCCode()+"*)"+
				    s(o.rhs(0))+") = ("+
				    ((MemoryAccessInst)o).getType().cType().asCCode()+")"+
                                    s(o.rhs(1)));
			    break;
			case Load:
			    w.print("("+((MemoryAccessInst)o).getExectype().cType().asCCode()+
				    ") *("+((MemoryAccessInst)o).getType().cType().asCCode()+
                                    " "+((MemoryAccessInst)o).volatility().asCCode()+"*)"+
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
			    w.print("(uintptr_t)sizeof("+cti.ctype().asCCode()+")");
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
                        case GetCArg: {
                            ArgInst ai=(ArgInst)o;
                            w.print("("+code.cparam(ai.getIdx()).cType.asCCode()+
                                    ")arg"+ai.getIdx());
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
			    for (int i=0;i<f.getParams().length;++i) {
				if (i!=0) {
				    w.print(", ");
				}
                                if (o.rhs(i) instanceof IntConst) {
                                    // HACK!!
                                    w.print(((IntConst)o.rhs(i)).value());
                                } else {
                                    w.print("("+f.getParam(i).cTypeForCall.asCCode()+")"+s(o.rhs(i)));
                                }
			    }
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
			    for (int i=0;i<cii.params().length;++i) {
				if (i!=0) {
				    w.print(", ");
				}
				w.print("("+cii.param(i).cType.asCCode()+")"+s(o.rhs(i+1)));
			    }
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
                        case Memcpy: {
                            w.print("memcpy("+s(o.rhs(0))+", "+s(o.rhs(1))+", (size_t)("+s(o.rhs(2))+"))");
                            break;
                        }
                        case Sqrt: {
                            switch (o.rhs(0).effectiveBasetype()) {
                            case FLOAT: w.print("sqrtf("+s(o.rhs(0))+")"); break;
                            case DOUBLE: w.print("sqrt("+s(o.rhs(0))+")"); break;
                            default: throw new Error("bad type: "+o.rhs(0).type());
                            }
                            break;
                        }
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
			    w.print("goto B"+((Header)o.next).order+"B");
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
			    w.print(s(o.rhs(0))+")) goto B"+
				    b.target().order+
				    "B; else goto B"+((Header)o.next).order+"B");
			    break;
                        }
			case Switch: {
			    Switch sw=(Switch)o;
			    w.println("switch ("+s(sw.rhs(0))+") {");
			    for (int i=0;i<sw.numCases();++i) {
				w.println("   case "+sw.value(i)+": goto B"+
					  sw.target(i).order+"B;");
			    }
			    w.println("   default: goto B"+
				      ((Header)sw.next).order+"B;");
			    w.println("   }");
			    break;
			}
			default: throw new Error("bad operation: "+o);
			}
			if (o.hasLhs()) {
			    w.print(")");
			}
			if (o.opcode()!=OpCode.Switch) {
			    // do I get style points?
                            w.print(";");
                            if (o instanceof TypeInst) {
                                w.print(" /* "+((TypeInst)o).getOrigType().shortName()+" */");
                            } else if (o instanceof MemoryAccessInst) {
                                w.print(" /* "+((MemoryAccessInst)o).getOrigType().shortName()+" */");
                            }
                            w.println();
			}
		    }
		}
	    }
	};
    }
}

