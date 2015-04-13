/*
 * Operation.java
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

import java.util.*;
import java.nio.*;

/**
 * Represents an action that may be performed by the program, which may either be
 * a side-effecting action, an action that produces a value that may be stored into
 * a variable, or a footer action that results in the program's execution being
 * diverted to a dynamically selected basic block.  Side-effects and actions that
 * produce values are represented as Instructions.  Operations that only serve to
 * terminate a basic block, and either terminate the program or the method, or divert
 * control to another basic block, are represented as Footers.  Thus this class is
 * abstract and never gets instantiated directly.  An Operation may always have an
 * Instruction prepended to it (since Footers may always have preceding Instructions,
 * and Instructions may also have preceding Instructions), but in general, an
 * Operation may be preceded by either a basic block Header or an Instruction, and
 * may be succeeded by either an Instruction, a Footer, or the basic block header for
 * the default successor of this basic block.
 */
public abstract class Operation extends Node {

    OpCode opcode;
    Arg[] rhs;
    
    Header head;
    
    Operation(DebugInfo di,
	      OpCode opcode,
	      Arg[] rhs) {
	super(di);
	this.opcode=opcode;
	this.rhs=rhs;
    }
    
    public OpCode opcode() {
	return opcode;
    }
    
    public Arg[] rhs() {
	return rhs;
    }
    
    public int nrhs() {
        return rhs.length;
    }
    
    public Arg rhs(int idx) {
	return rhs[idx];
    }
    
    public boolean hasLhs() { return false; }
    
    public Var lhs() {
        throw new Error("Not an instruction");
    }
    
    /** If it's an instruction, remove it, otherwise throw an error */
    public void remove() {
	throw new Error("Not an instruction");
    }
    
    public Header head() {
        return head;
    }
    
    public String label() {
	return ""+head.order+"."+order;
    }
    
    public Instruction prepend(Instruction i) {
	i.next=this;
	i.prev=prev;
	prev.next=i;
	prev=i;
        i.head=head;
	return i;
    }
    
    public String toString() {
	StringBuffer buf=new StringBuffer();
	if (this instanceof Instruction) {
	    Instruction i=(Instruction)this;
	    if (i.lhs()!=Var.VOID) {
		buf.append(i.lhs()+" = ");
	    }
	}
	buf.append(opcode());
	buf.append(OpMetaDumper.get(this));
	buf.append("(");
	buf.append(Util.dump(rhs()));
	buf.append(")");
	return buf.toString();
    }
    
    public Operation copy() {
	Operation result=(Operation)super.copy();
	if (rhs.length!=0) {
	    result.rhs=new Arg[rhs.length];
	    System.arraycopy(rhs,0,
			     result.rhs,0,
			     rhs.length);
	}
	return result;
    }
    
    public Operation copyAndMultiAssign() {
        return copy();
    }
    
    public void replaceVars(Map< ?, Var > map) {
	replaceVars(Gettable.wrap(map));
    }
    
    public void replaceVars(Gettable< ? super Arg, ? extends Var > map) {
	for (int i=0;i<rhs.length;++i) {
	    Var result=map.get(rhs[i]);
	    if (result!=null) {
		rhs[i]=result;
	    }
	}
    }
    
    public boolean uses(Arg a) {
	for (int i=0;i<rhs.length;++i) {
	    if (rhs[i]==a) {
		return true;
	    }
	}
	return false;
    }
    
    public boolean defs(Var v) {
	return false;
    }
    
    public boolean usesOrDefs(Var v) {
	return uses(v) || defs(v);
    }
    
    public boolean usesAny(Set< ? > vars) {
	for (int i=0;i<rhs.length;++i) {
	    if (vars.contains(rhs[i])) {
		return true;
	    }
	}
	return false;
    }
    
    public boolean defsAny(Set< ? > vars) {
	return false;
    }
    
    public boolean usesOrDefsAny(Set< ? > vars) {
	return usesAny(vars) || defsAny(vars);
    }
    
    public boolean usesAnySame(Operation other) {
        for (int i=0;i<rhs.length;++i) {
            if (other.uses(rhs[i])) {
                return true;
            }
        }
        return false;
    }
    
    public int sameUseIndex(Operation other) {
        for (int i=0;i<rhs.length;++i) {
            if (other.uses(rhs[i])) {
                return i;
            }
        }
        return -1;
    }
    
    public int indexOf(Arg a) {
        for (int i=0;i<rhs.length;++i) {
            if (rhs[i]==a) {
                return i;
            }
        }
        return -1;
    }
    
    int getNioSize() {
        int result=4+4+4;
        for (Arg a : rhs) {
            result+=a.getArgNioSize();
        }
        return result;
    }
    
    void writeTo(NioContext ctx,
                 ByteBuffer buffer) {
        buffer.putInt(opcode.ordinal());
        buffer.putInt(ctx.diCodes.codeFor(di));
        buffer.putInt(rhs.length);
        for (Arg a : rhs) {
            a.writeArgTo(buffer);
        }
    }
    
    static Operation readFrom(NioContext ctx,
                              ByteBuffer buffer) {
        OpCode opcode=OpCode.table[buffer.getInt()];
        try {
            DebugInfo di=ctx.diCodes.forCode(buffer.getInt());
            int rhsLength=buffer.getInt();
            Arg[] rhs;
            if (rhsLength==0) {
                rhs=Arg.EMPTY;
            } else {
                rhs=new Arg[rhsLength];
                for (int i=0;i<rhs.length;++i) {
                    rhs[i]=Arg.readArgFrom(ctx,buffer);
                }
            }
            switch (opcode) {
            case Jump:
                return new Jump(di,(Header)ctx.nodeCodes.forCode(buffer.getInt()));
            case BranchNonZero:
            case BranchZero:
                return new Branch(di,opcode,rhs,
                                  (Header)ctx.nodeCodes.forCode(buffer.getInt()),
                                  (Header)ctx.nodeCodes.forCode(buffer.getInt()),
                                  BranchPrediction.values()[buffer.getInt()],
                                  PredictionStrength.values()[buffer.getInt()]);
            case Switch: {
                Header next=(Header)ctx.nodeCodes.forCode(buffer.getInt());
                int numCases=buffer.getInt();
                Header[] targets=new Header[numCases];
                int[] values=new int[numCases];
                for (int i=0;i<numCases;++i) {
                    targets[i]=(Header)ctx.nodeCodes.forCode(buffer.getInt());
                    values[i]=buffer.getInt();
                }
                return new Switch(di,rhs,next,targets,values);
            }
            case AwesomeJump: {
                int numTargets=buffer.getInt();
                Header[] targets=new Header[numTargets];
                for (int i=0;i<numTargets;++i) {
                    targets[i]=(Header)ctx.nodeCodes.forCode(buffer.getInt());
                }
                return new AwesomeJump(di,rhs,targets);
            }
            case Return:
            case RawReturn:
            case Throw:
            case Rethrow:
            case NotReached:
                return new Terminal(di,opcode,rhs);
            case PatchPointFooter:
                return new PatchPointFooter(di,opcode,rhs,
                                            buffer.getInt(),buffer.getInt(),buffer.getInt(),
                                            Global.classCoding.forCode(buffer.getInt()),
                                            Util.readString(buffer));
            default: {
                // it's an instruction
                Var lhs=ctx.code.getVar(buffer.getInt());
                switch (opcode) {
                case PatchPoint:
                    return new PatchPoint(di,opcode,lhs,rhs,
                                          buffer.getInt(),buffer.getInt(),buffer.getInt(),
                                          Global.classCoding.forCode(buffer.getInt()),
                                          Util.readString(buffer));
                case GetAllocSpace:
                case ScopeReturnCheck:
                case InHeapCheck:
                case LikelyZero:
                case LikelyNonZero:
                case SemanticallyLikelyZero:
                case SemanticallyLikelyNonZero:
                case CheckException:
                case ClearException:
                case Fence:
                case CompilerFence:
                case PollcheckFence:
                case HardCompilerFence:
                case IntToPointerZeroFill:
                case Mov:
                case Ipsilon:
                case CastNonZero:
                case Neg:
                case Not:
                case Boolify:
                case BitNot:
                case MonitorEnter:
                case MonitorExit:
                case ArrayLength:
                case Add:
                case Sub:
                case Mul:
                case Div:
                case Mod:
                case Shl:
                case Shr:
                case Ushr:
                case And:
                case Or:
                case Xor:
                case Sqrt:
                case CompareL:
                case CompareG:
                case LessThan:
                case ULessThan:
                case LessThanEq:
                case ULessThanEq:
                case Eq:
                case Neq:
                case NullCheck:
                case CheckDivisor:
                case PollCheck:
                case ArrayBoundsCheck:
                case ArrayCheckStore:
                case GetTypeDataForObject:
                case Memcpy:
                case Phantom:
                case PhantomCheck:
                case HardUse:
                case Phi:
                case FirstHalf:
                case SecondHalf:
                    return new SimpleInst(di,opcode,lhs,rhs);
                case GetMethodRec:
                    return new GetMethodInst(di,opcode,lhs,rhs,
                                             Global.methodCoding.forCode(buffer.getInt()));
                case ThrowRTEOnZero:
                case GetType:
                case GetTypeData:
                case New:
                case NewArray:
                case Cast:
                case CastExact:
                case Fiat:
                case Instanceof:
                case GetException:
                case OffsetOfElement:
                    return new TypeInst(
                        di,opcode,lhs,rhs,
                        Global.typeCoding.forCode(buffer.getInt()),
                        Global.typeCoding.forCode(buffer.getInt()));
                case Load:
                case Store:
                case StrongLoadCAS:
                case StrongCAS:
                case WeakCAS:
                case StrongVoidCAS:
                    return new MemoryAccessInst(
                        di,opcode,lhs,rhs,
                        Global.typeCoding.forCode(buffer.getInt()),
                        Global.typeCoding.forCode(buffer.getInt()),
                        Mutability.table[buffer.get()],
                        Volatility.table[buffer.get()]);
                case GetDebugID:
                case SaveDebugID:
                    return new DebugIDInfoInst(di,opcode,lhs,DebugIDInfo.readFrom(ctx,buffer));
                case TypeCheck:
                    return new TypeCheckInst(
                        di,lhs,rhs,
                        Global.typeCoding.forCode(buffer.getInt()),
                        Global.typeCoding.forCode(buffer.getInt()));
                case AddressOfStatic:
                case AddressOfField:
                case GetStatic:
                case GetField:
                case PutStatic:
                case PutField:
                case WeakCASStatic:
                case WeakCASField:
                case ArrayLoad:
                case ArrayStore:
                case WeakCASElement:
                case AddressOfElement:
                    return new HeapAccessInst(
                        di,opcode,lhs,rhs,
                        BarrierMode.readFrom(buffer),
                        Global.fieldCoding.forCode(buffer.getInt()));
                case CallIndirect:
                    return new CallIndirectInst(
                        di,opcode,lhs,rhs,
                        Global.nsigCoding.forCode(buffer.getInt()));
                case InvokeResolved:
                    return new ResolvedMethodInst(
                        di,opcode,lhs,rhs,
                        Global.msigCoding.forCode(buffer.getInt()),
                        (Function)Global.cfieldCoding.forCode(buffer.getInt()));
                case InvokeIndirect:
                    return new IndirectMethodInst(
                        di,opcode,lhs,rhs,
                        Global.msigCoding.forCode(buffer.getInt()));
                case MultiNewArray:
                    return new MultiNewArrayInst(
                        di,lhs,rhs,
                        Global.typeCoding.forCode(buffer.getInt()),
                        buffer.getInt());
                case Invoke:
                case InvokeDynamic:
                case InvokeStatic:
                    return new MethodInst(
                        di,opcode,lhs,rhs,
                        Global.methodCoding.forCode(buffer.getInt()),
                        ClassBound.readFrom(buffer));
                case GetString:
                    return new GetStringInst(
                        di,lhs,Util.readString(buffer));
                case CheckInit:
                    return new ClassInst(
                        di,opcode,lhs,
                        Global.classCoding.forCode(buffer.getInt()));
                case OffsetOfField:
                    return new FieldInst(
                        di,opcode,lhs,rhs,
                        (VisibleField)Global.fieldCoding.forCode(buffer.getInt()));
                case PutCField:
                case GetCField:
                case GetCFieldAddress:
                case GetCFieldOffset:
                case PutCVar:
                case GetCVar:
                case GetCVarAddress:
                case Call:
                    return new CFieldInst(
                        di,opcode,lhs,rhs,
                        Global.cfieldCoding.forCode(buffer.getInt()));
                case PoundDefined:
                    return new CMacroInst(
                        di,opcode,lhs,rhs,
                        Util.readString(buffer));
                case GetCTypeSize:
                    return new CTypeInst(
                        di,opcode,lhs,rhs,
                        Global.ctypeCoding.forCode(buffer.getInt()));
                case GetArg:
                case GetCArg:
                case SaveRef:
                    return new ArgInst(
                        di,opcode,lhs,rhs,buffer.getInt());
                default: throw new Error("bad opcode: "+opcode);
                }
            }}
        } catch (Throwable e) {
            throw new CompilerException("Could not parse "+opcode+" (ordinal "+opcode.ordinal()+")",e);
        }
    }
}

