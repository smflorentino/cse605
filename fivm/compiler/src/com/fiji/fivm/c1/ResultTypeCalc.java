/*
 * ResultTypeCalc.java
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

public class ResultTypeCalc {
    Code c;
    
    public ResultTypeCalc(Code c) { this.c=c; }
    
    public Exectype typeForArg(Arg a) { return a.type(); }
    
    public Type exactTypeForArg(Arg a) { return typeForArg(a).asType(); }
    
    public Exectype get(Instruction i) {
	switch (i.opcode()) {
	case GetArg:
	    return c.param(((ArgInst)i).getIdx()).asExectype();
        case GetCArg:
            return c.cparam(((ArgInst)i).getIdx()).asExectype;
	case Cast:
        case CastExact:
        case Fiat:
	case New:
	case NewArray:
	case GetException:
	    return ((TypeInst)i).getType().asExectype();
	case Load:
            return ((MemoryAccessInst)i).getType().asExectype();
	case TypeCheck:
	    return ((TypeCheckInst)i).typeToCheck().asExectype();
	case Add:
	case Sub:
	case Mul:
	case Div:
	case Mod:
	case Neg:
	case Shl:
	case Shr:
	case Ushr:
	case And:
	case Or:
	case Xor:
	case Mov:
        case CastNonZero:
	case Phi:
	case Ipsilon:
	case BitNot:
	case Not:
        case Boolify:
        case FXor:
        case Sqrt:
	    return typeForArg(i.rhs(0));
	case StrongLoadCAS:
	    return typeForArg(i.rhs(1)).lub(typeForArg(i.rhs(2)));
	case StrongCAS:
	case WeakCAS:
	case CompareG:
	case CompareL:
	case LessThan:
	case ULessThan:
	case LessThanEq:
	case ULessThanEq:
	case Eq:
        case Neq:
	case Instanceof:
	case ArrayLength:
	case WeakCASStatic:
	case WeakCASField:
	case WeakCASElement:
        case LikelyZero:
        case LikelyNonZero:
        case SemanticallyLikelyZero:
        case SemanticallyLikelyNonZero:
	case GetDebugID:
	case PoundDefined:
        case GetAllocSpace:
	    return Exectype.INT;
	case MonitorEnter:
	case MonitorExit:
	case PutStatic:
	case PutField:
	case NullCheck:
	case PollCheck:
	case ArrayBoundsCheck:
	case ArrayCheckStore:
	case ArrayStore:
	case Store:
	case PutCField:
	case PutCVar:
	case Return:
	case RawReturn:
	case Throw:
        case PatchPointFooter:
	case NotReached:
	case Jump:
	case BranchNonZero:
	case BranchZero:
	case Switch:
	case StrongVoidCAS:
	case PhantomCheck:
        case AwesomeJump:
        case Memcpy:
	    return Exectype.VOID;
	case PatchPoint:
            return ((PatchPoint)i).di().origin().origin().getType().asExectype();
	case InvokeStatic:
	case Invoke:
	case InvokeDynamic:
        case InvokeResolved:
        case InvokeIndirect:
	    return ((MTSInstable)i).signature().getResult().asExectype();
	case GetStatic:
	case GetField:
	    return ((HeapAccessInst)i).fieldType().asExectype();
	case GetString:
	    return Global.root().stringType.asExectype();
	case GetType:
	    return Global.root().classType.asExectype();
	case MultiNewArray:
	    return ((MultiNewArrayInst)i).type().asExectype();
	case ArrayLoad: {
	    Exectype t=typeForArg(i.rhs()[0]);
	    if (t==Exectype.NULL) {
		if (Global.verbosity>=7) {
		    Global.log.println("Returning BOTTOM for array load on NULL in "+i+" in "+c);
		}
		// NOTE: this could cause the sanity checker to blow up later.
		// but we fix this by simply cutting off the rest of a basic block
		// after a NullCheck(nulltype).  should have a test later!
		return Exectype.BOTTOM;
	    } else {
		return t.arrayElement().asExectype();
	    }
	}
	case GetCField:
	case GetCVar:
	    return Exectype.make(((CFieldInst)i).field().getType());
	case GetCFieldOffset:
	case GetCFieldAddress:
	case GetCVarAddress:
        case GetCArgAddress:
	case GetTypeDataForObject:
	case GetTypeData:
	case GetCTypeSize:
	case IntToPointerZeroFill:
	case AddressOfStatic:
	case AddressOfField:
	case OffsetOfField:
	case AddressOfElement:
	case OffsetOfElement:
        case GetMethodRec:
        case FirstHalf:
        case SecondHalf:
	    return Exectype.POINTER;
	case Call:
	    return Exectype.make(((Function)((CFieldInst)i).field()).getResult());
	case CallIndirect:
	    return Exectype.make(((CallIndirectInst)i).result());
        case Float0:
            return Exectype.FLOAT;
        case Double0:
            return Exectype.DOUBLE;
	default: throw new Error("Unknown opcode: "+i.opcode());
	}
    }
    
    public Exectype get(Arg a) {
        if (a instanceof Var) {
            return get(((Var)a).inst());
        } else {
            return a.type();
        }
    }
    
    public Type getExact(Instruction i) {
	switch (i.opcode()) {
	case GetArg:
	    return c.param(((ArgInst)i).getIdx());
        case GetCArg:
            return c.cparam(((ArgInst)i).getIdx()).asType;
	case Cast:
        case CastExact:
        case Fiat:
	case New:
	case NewArray:
	case GetException:
	    return ((TypeInst)i).getType();
	case Load:
            return ((MemoryAccessInst)i).getType();
	case TypeCheck:
	    return ((TypeCheckInst)i).typeToCheck();
	case Add:
	case Sub:
	case Mul:
	case Div:
	case Mod:
	case Neg:
	case Shl:
	case Shr:
	case Ushr:
	case And:
	case Or:
	case Xor:
	case Mov:
        case CastNonZero:
	case Phi:
	case Ipsilon:
	case BitNot:
	case Not:
        case Boolify:
        case FXor:
        case Sqrt:
	    return exactTypeForArg(i.rhs(0));
	case StrongLoadCAS:
	    return typeForArg(i.rhs(1)).lub(typeForArg(i.rhs(2))).asType();
	case StrongCAS:
	case WeakCAS:
	case LessThan:
	case ULessThan:
	case LessThanEq:
	case ULessThanEq:
	case WeakCASStatic:
	case WeakCASField:
	case WeakCASElement:
	case PoundDefined:
	case Eq:
        case Neq:
	case Instanceof:
            return Type.BOOLEAN;
	case CompareG:
	case CompareL:
            return Type.BYTE;
	case ArrayLength:
        case LikelyZero:
        case LikelyNonZero:
        case SemanticallyLikelyZero:
        case SemanticallyLikelyNonZero:
	case GetDebugID:
        case GetAllocSpace:
	    return Type.INT;
	case MonitorEnter:
	case MonitorExit:
	case PutStatic:
	case PutField:
	case NullCheck:
	case PollCheck:
	case ArrayBoundsCheck:
	case ArrayCheckStore:
	case ArrayStore:
	case Store:
	case PutCField:
	case PutCVar:
	case Return:
	case RawReturn:
	case Throw:
        case PatchPointFooter:
	case NotReached:
	case Jump:
	case BranchNonZero:
	case BranchZero:
	case Switch:
	case StrongVoidCAS:
	case PhantomCheck:
        case AwesomeJump:
        case Memcpy:
	    return Type.VOID;
	case PatchPoint:
            return ((PatchPoint)i).di().origin().origin().getType();
	case InvokeStatic:
	case Invoke:
	case InvokeDynamic:
        case InvokeResolved:
        case InvokeIndirect:
	    return ((MTSInstable)i).signature().getResult();
	case GetStatic:
	case GetField:
	    return ((HeapAccessInst)i).fieldType();
	case GetString:
	    return Global.root().stringType;
	case GetType:
	    return Global.root().classType;
	case MultiNewArray:
	    return ((MultiNewArrayInst)i).type();
	case ArrayLoad: {
	    Exectype t=typeForArg(i.rhs()[0]);
	    if (t==Exectype.NULL) {
		if (Global.verbosity>=7) {
		    Global.log.println("Returning BOTTOM for array load on NULL in "+i+" in "+c);
		}
		// NOTE: this could cause the sanity checker to blow up later.
		// but we fix this by simply cutting off the rest of a basic block
		// after a NullCheck(nulltype).  should have a test later!
		return Type.BOTTOM;
	    } else {
		return t.arrayElement();
	    }
	}
	case GetCField:
	case GetCVar:
	    return ((CFieldInst)i).field().getType().asType;
	case GetCFieldOffset:
	case GetCFieldAddress:
	case GetCVarAddress:
        case GetCArgAddress:
	case GetTypeDataForObject:
	case GetTypeData:
	case GetCTypeSize:
	case IntToPointerZeroFill:
	case AddressOfStatic:
	case AddressOfField:
	case OffsetOfField:
	case AddressOfElement:
	case OffsetOfElement:
        case GetMethodRec:
        case FirstHalf:
        case SecondHalf:
	    return Type.POINTER;
	case Call:
	    return ((Function)((CFieldInst)i).field()).getResult().asType;
	case CallIndirect:
	    return ((CallIndirectInst)i).result().asType;
        case Float0:
            return Type.FLOAT;
        case Double0:
            return Type.DOUBLE;
	default: throw new Error("Unknown opcode: "+i.opcode());
	}
    }
    
    public Type getExact(Arg a) {
        if (a instanceof Var) {
            Instruction i=((Var)a).inst();
            if (i!=null) {
                return getExact(i);
            } else {
                return a.type().asType();
            }
        } else if (a instanceof IntConst) {
            int value=((IntConst)a).value();
            if (value==0 || value==1) {
                return Type.BOOLEAN;
            } else if (value>=-128 && value<=127) {
                return Type.BYTE;
            } else if (value>=0 && value<=65535) {
                return Type.CHAR;
            } else if (value>=-32768 && value<=32767) {
                return Type.SHORT;
            } else {
                return Type.INT;
            }
        } else {
            return a.type().asType();
        }
    }
}


