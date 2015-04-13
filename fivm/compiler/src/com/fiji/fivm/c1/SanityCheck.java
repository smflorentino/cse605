/*
 * SanityCheck.java
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

public class SanityCheck extends CodePhase {
    
    public SanityCheck(Code c) {
	super(c);
    }
    
    void assertNoPollcheckRules(Operation o) {
	assert o.di().origin().allowUnsafe()
	    || !o.di().origin().noPollcheck()
	    : ""+o.di()+" is marked @NoPollcheck but uses "+o.opcode();
    }
    
    void assertNoPollcheckRules(Operation o, Callable call) {
	// FIXME: this is utterly broken.  we should be doing this on CodeOrigin
	// instead, and then also give CodeOrigin a notion of AllowUnsafe.  Then,
	// synthetic methods for downcalls and upcalls (import/export) should be
	// marked AllowUnsafe.
	assert o.di().origin().allowUnsafe()
	    || !o.di().origin().noPollcheck()
	    || call.pollcheck()==PollcheckMode.EXPLICIT_POLLCHECKS_ONLY
	    || call.safepoint()==SafepointMode.CANNOT_SAFEPOINT
	    : ""+o.di()+" is marked @NoPollcheck but calls "+call;
    }
    
    void assertCASableType(Basetype t) {
	assert t==Basetype.INT
	    || t==Basetype.POINTER
	    || t==Basetype.OBJECT;
    }
    
    HashSet< Var > allowedVars=new HashSet< Var >();
    HashSet< Header > allowedHeaders=new HashSet< Header >();
    HashSet< Integer > headerNumbers=new HashSet< Integer >();
    HashSet< ExceptionHandler > allowedHandlers=new HashSet< ExceptionHandler >();
    
    class MyVisitor extends Visitor< Void > {
	public Void visit(ExceptionHandler e) {
	    assert allowedHandlers.contains(e);
	    assert allowedHeaders.contains(e.next)==true : "for handler "+e+" and successor "+e.next;
	    assert e.dropsTo==null || allowedHandlers.contains(e.dropsTo);
	    assert e.prev==null;
	    assert e.next instanceof Header;
	    assert e.handles==null || e.handles.isSubclassOf(Global.root().throwableClass);
	    return null;
	}
	    
	public Void visit(Header h) {
	    assert allowedHeaders.contains(h)==true : h;
	    assert h.handler==null || allowedHandlers.contains(h.handler) : "For header = "+h+" and handler = "+h.handler;
	    assert h.code==code;
	    assert headerNumbers.add(h.order);
	    assert h.prev==null;
	    assert h.next!=null;
	    assert h.next.prev==h;
	    assert h.next instanceof Instruction
		|| h.next instanceof Footer : h.next;
	    HashSet< Node > set=new HashSet< Node >();
	    for (Node n=h.next;n!=h.footer;n=n.next) {
		assert n instanceof Instruction;
		set.add(n);
	    }
	    for (Node n=h.footer.prev;n!=h;n=n.prev) {
		assert n instanceof Instruction;
		assert set.remove(n);
	    }
	    assert set.isEmpty();
	    return null;
	}
	
	public Void visit(Operation o) {
	    for (int i=0;i<o.rhs.length;++i) {
		assert o.rhs[i].type()!=Exectype.VOID;
		assert o.rhs[i].type()!=Exectype.TOP;
		assert o.rhs[i].type()!=Exectype.BOTTOM;
		assert !(o.rhs[i] instanceof Var)
		    || allowedVars.contains((Var)o.rhs[i]);
	    }
	    return null;
	}
	
	public Void visit(Footer f) {
	    assert f.prev.next==f;
	    for (Header h2 : f.successors()) {
		assert allowedHeaders.contains(h2)==true : "for target "+h2+" from "+f;
	    }
	    return visit((Operation)f);
	}
    
	public Void visit(Terminal t) {
	    assert t.next==null;
	    assert t.prev instanceof Header
		|| t.prev instanceof Instruction;
	    switch (t.opcode) {
	    case Return:
		if (code.result()==Type.VOID) {
		    assert t.rhs==Arg.EMPTY;
		} else {
		    assert t.rhs.length==1;
		    assert t.rhs[0].type().isSubtypeOf(
			code.result().asExectype());
		}
		break;
	    case RawReturn:
		if (code.result()==Type.VOID) {
		    assert t.rhs==Arg.EMPTY;
		} else {
		    assert t.rhs.length==1;
		    assert t.rhs[0].type()
			== Exectype.make(
			    code.result().effectiveBasetype().pointerifyObject);
		}
		break;
	    case Throw:
		if (Global.verbosity>=7) {
		    Global.log.println("type of rhs = "+t.rhs[0].type());
		    Global.log.println("type of throwable = "+Global.root().throwableType.asExectype());
		}
		assert t.rhs.length==1;
		assert t.rhs[0].type().isSubtypeOf(
		    Global.root().throwableType.asExectype());
		break;
	    case Rethrow:
	    case NotReached:
		assert t.rhs==Arg.EMPTY;
		break;
	    default:
	        assert false:t.opcode;
		break;
	    }
	    return visit((Footer)t);
	}
        
	public Void visit(Control c) {
            if (c.opcode()!=OpCode.AwesomeJump) {
                assert c.next instanceof Header : c.next;
            }
	    assert c.prev instanceof Header
		|| c.prev instanceof Instruction : c.prev;
	    return visit((Footer)c);
	}
    
	public Void visit(Jump j) {
	    assert j.opcode==OpCode.Jump;
	    assert j.rhs==Arg.EMPTY;
	    return visit((Control)j);
	}
    
	public Void visit(Branch b) {
	    assert b.opcode==OpCode.BranchNonZero
		|| b.opcode==OpCode.BranchZero;
	    assert b.rhs.length==1;
	    assert b.rhs[0].type().effectiveBasetype().isValue;
	    return visit((Control)b);
	}
        
        public Void visit(AwesomeJump a) {
	    assert a.opcode==OpCode.AwesomeJump;
	    assert a.rhs.length==1;
	    assert a.rhs[0].type()==Exectype.POINTER;
            assert a.next==null;
	    return visit((Control)a);
        }
    
	public Void visit(Switch s) {
	    assert s.opcode==OpCode.Switch;
	    assert s.rhs.length==1;
	    assert s.rhs[0].type().effectiveBasetype().isInteger;
	    assert s.rhs[0].type().effectiveBasetype().cells==1;
	    assert s.targets.length==s.values.length;
	    return visit((Control)s);
	}
    
	public Void visit(Instruction i) {
	    assert i.prev.next==i;
	    assert i.next.prev==i;
	    assert i.next instanceof Footer
		|| i.next instanceof Instruction;
	    assert i.prev instanceof Header
		|| i.prev instanceof Instruction;
	    assert i.lhs()==Var.VOID
		|| allowedVars.contains(i.lhs());
	    return null;
	}
    
        public Void visit(PatchPoint p) {
            assert p.opcode()==OpCode.PatchPoint;
            assert p.rhs().length==1+p.nLocals+p.nStack;
            assert p.lhs().type()==p.di().origin().origin().getType().asExectype();
            if (p.lhs().type()==Exectype.VOID) {
                assert p.lhs()==Var.VOID;
            }
            for (int i=0;i<p.rhs.length;++i) {
                if (p.rhs[i].type().effectiveBasetype().cells==2) {
                    assert i+1<p.rhs.length;
                    assert p.rhs[i+1].type()==Exectype.NIL;
                    i++;
                }
            }
            assert p.description()!=null;
            return visit((Instruction)p);
        }
	    
        public Void visit(PatchPointFooter p) {
            assert p.opcode()==OpCode.PatchPointFooter;
            assert p.rhs().length==1+p.nLocals+p.nStack;
            for (int i=0;i<p.rhs.length;++i) {
                if (p.rhs[i].type().effectiveBasetype().cells==2) {
                    assert i+1<p.rhs.length;
                    assert p.rhs[i+1].type()==Exectype.NIL;
                    i++;
                }
            }
            assert p.description()!=null;
            return visit((Footer)p);
        }
	    
	public Void visit(ArgInst a) {
            assert a.argIdx>=0;
            switch (a.opcode) {
            case GetArg:
                assert a.argIdx<code.params().length;
                assert code.param(a.argIdx)
                    .asExectype().isSubtypeOf(a.lhs().type());
                assert a.rhs==Arg.EMPTY;
                break;
            case GetCArg:
                assert a.argIdx<code.cparams().length;
                assert code.cparam(a.argIdx)!=null;
                assert code.cparam(a.argIdx).asExectype!=null;
                assert code.cparam(a.argIdx).asExectype.isSubtypeOf(a.lhs().type())
                    : "argIdx = "+a.argIdx+", cparam = "+code.cparam(a.argIdx)+", lhs = "+a.lhs();
                assert a.rhs==Arg.EMPTY;
                break;
            case GetCArgAddress:
                assert a.argIdx<code.cparams().length;
                assert code.cparam(a.argIdx)!=null;
                assert code.cparam(a.argIdx).asExectype!=null;
                assert a.lhs().type()==Exectype.POINTER;
                assert a.rhs==Arg.EMPTY;
                break;
            case SaveRef:
                // this conditional is a total hack. :-(
                if (code.hasRefAlloc()) {
                    assert a.argIdx<code.getRefAlloc().numRefs();
                }
                assert a.rhs.length==1;
                assert a.lhs()==Var.VOID;
                assert a.rhs[0].type().isObject();
                break;
            default:
                assert false:a.opcode;
                break;
            }
	    return visit((Instruction)a);
	}
	
	public Void visit(CTypeInst c) {
	    switch (c.opcode) {
	    case GetCTypeSize:
		assert c.lhs().type()==Exectype.POINTER;
		assert c.rhs==Arg.EMPTY;
		assert c.ctype()!=null;
		assert c.ctype().asCCode().length()>0;
		break;
	    default:
	        assert false:c.opcode;
	    }
	    return visit((Instruction)c);
	}
    
	public Void visit(CMacroInst c) {
	    switch (c.opcode) {
	    case PoundDefined:
		assert c.lhs().type()==Exectype.INT;
		assert c.rhs==Arg.EMPTY;
		assert c.cmacro()!=null;
		assert c.cmacro().length()>0;
		break;
	    default:
	        assert false:c.opcode;
	    }
	    return visit((Instruction)c);
	}
    
	public Void visit(CFieldInst a) {
	    switch (a.opcode) {
	    case PutCField:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==2;
		assert a.rhs[0].type()==Exectype.POINTER;
		assert a.rhs[1].type()==Exectype.make(a.field.getType());
		assert a.field.getType()!=Basetype.VOID;
		assert a.field instanceof CStructField;
		break;
	    case GetCField:
		assert a.lhs().type()==Exectype.make(a.field.getType());
		assert a.rhs.length==1;
		assert a.rhs[0].type()==Exectype.POINTER;
		assert a.field.getType()!=Basetype.VOID;
		assert a.field instanceof CStructField;
		break;
	    case GetCFieldAddress:
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs.length==1;
		assert a.rhs[0].type()==Exectype.POINTER;
		assert a.field instanceof CStructField;
		break;
	    case GetCFieldOffset:
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs==Arg.EMPTY;
		assert a.field instanceof CStructField;
		break;
	    case PutCVar:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==1;
		assert a.rhs[0].type()==Exectype.make(a.field.getType());
		assert a.field.getType()!=Basetype.VOID;
		break;
	    case GetCVar:
		assert a.lhs().type()==Exectype.make(a.field.getType());
		assert a.rhs==Arg.EMPTY;
		assert a.field.getType()!=Basetype.VOID;
		break;
	    case GetCVarAddress:
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs==Arg.EMPTY;
		break;
	    case Call: {
		assert a.field instanceof Function;
		assertNoPollcheckRules(a,(Function)a.field);
		Function f=(Function)a.field;
		for (int i=0;i<a.rhs.length;++i) {
		    assert a.rhs[i].type()==Exectype.make(f.getParams()[i]);
		}
		assert a.lhs().type()==Exectype.make(f.getResult());
		break;
	    }
	    default:
	        assert false:a.opcode;
		break;
	    }
	    return visit((Instruction)a);
	}
    
	public Void visit(FieldInst a) {
	    switch (a.opcode) {
	    case OffsetOfField:
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs==Arg.EMPTY;
		assert a.field.isInstance();
		break;
	    default:
	        assert false:a.opcode;
		break;
	    }
	    return visit((Instruction)a);
	}
    
	public Void visit(ClassInst a) {
            assert a.value!=null;
	    switch (a.opcode()) {
	    case CheckInit:
		assert a.rhs==Arg.EMPTY;
		assert a.lhs()==Var.VOID;
		assertNoPollcheckRules(a);
		break;
	    default: assert false:a.opcode;
	    }
	    return visit((Instruction)a);
	}
    
	public Void visit(GetStringInst a) {
	    assert a.opcode==OpCode.GetString;
	    assert Global.root().stringType.asExectype().isSubtypeOf(a.lhs().type());
	    assert a.rhs==Arg.EMPTY;
	    return visit((Instruction)a);
	}
	    
        public Void visit(GetMethodInst a) {
            assert a.opcode==OpCode.GetMethodRec;
            assert a.lhs().type()==Exectype.POINTER;
            assert a.rhs==Arg.EMPTY;
            assert a.method!=null;
            return visit((Instruction)a);
        }
    
	public Void visit(MethodInst a) {
	    assert a.opcode==OpCode.InvokeDynamic
		|| a.opcode==OpCode.InvokeStatic
		|| a.opcode==OpCode.Invoke;
	    assertNoPollcheckRules(a,a.method());
	    assert a.method().getType().asExectype().isSubtypeOf(a.lhs().type());
	    assert a.refinement().clazz().isSubclassOf(a.method().getClazz());
	    assert a.rhs.length
		== (a.method().getParams().length+
		    (a.method().getBinding()==Binding.INSTANCE?1:0));
	    int i=0;
	    if (a.method().getBinding()==Binding.INSTANCE) {
		assert a.rhs[i++].type().isSubtypeOf(
		    Exectype.make(a.method().getClazz()));
	    }
	    for (int j=0;j<a.method().getParams().length;++j) {
		assert a.rhs[i].type().isSubtypeOf(
		    a.method().getParams()[j].asExectype())
		    :i;
		i++;
	    }
	    return visit((Instruction)a);
	}
    
	public Void visit(MultiNewArrayInst a) {
	    for (int i=0;i<a.rhs.length;++i) {
		assert a.rhs[i].type()==Exectype.INT;
	    }
	    assert a.opcode==OpCode.MultiNewArray;
	    assert a.type.asExectype().isSubtypeOf(a.lhs().type());
	    assert a.rhs.length==a.dim;
	    assert a.dim>0;
	    assert a.type.getArrayDepth()>=a.dim;
	    return visit((Instruction)a);
	}
	
	public Void visit(CallIndirectInst c) {
	    assert c.opcode==OpCode.CallIndirect;
	    assertNoPollcheckRules(c,c.signature());
	    assert c.rhs.length==c.params().length+1;
	    assert c.rhs[0].type()==Exectype.POINTER;
	    for (int i=1;i<c.rhs.length;++i) {
		assert c.rhs[i].type()==Exectype.make(c.params()[i-1]);
	    }
	    assert c.lhs().type()==Exectype.make(c.result());
	    return visit((Instruction)c);
	}
        
        public Void visit(ResolvedMethodInst m) {
            assert m.opcode==OpCode.InvokeResolved;
            assertNoPollcheckRules(m,m.signature());
            assert m.rhs.length==m.signature().getParams().length;
            for (int i=0;i<m.rhs.length;++i) {
                assert m.rhs[i].type().isSubtypeOf(
                    m.signature().getParams()[i].asExectype()) : i;
            }
            assert m.signature().getResult().asExectype().isSubtypeOf(m.lhs().type());
            return visit((Instruction)m);
        }
        
        public Void visit(IndirectMethodInst m) {
            assert m.opcode==OpCode.InvokeIndirect;
            assertNoPollcheckRules(m,m.signature());
            assert m.rhs.length==m.signature().getParams().length+1;
            assert m.rhs[0].type()==Exectype.POINTER;
            for (int i=1;i<m.rhs.length;++i) {
                assert m.rhs[i].type().isSubtypeOf(
                    m.signature().getParams()[i-1].asExectype());
            }
            assert m.signature().getResult().asExectype().isSubtypeOf(m.lhs().type());
            return visit((Instruction)m);
        }
	    
	public Void visit(SimpleInst a) {
	    switch (a.opcode) {
            case FirstHalf:
            case SecondHalf:
                assert Global.pointerSize==4;
                assert a.rhs.length==1;
                assert a.rhs[0].type()==Exectype.LONG
                    || a.rhs[0].type()==Exectype.DOUBLE;
                assert a.lhs().type()==Exectype.POINTER;
                break;
            case GetAllocSpace:
                assert a.rhs==Arg.EMPTY;
                assert a.lhs().type()==Exectype.INT;
                break;
            case Float0:
                assert a.rhs==Arg.EMPTY;
                assert a.lhs().type()==Exectype.FLOAT;
                break;
            case Double0:
                assert a.rhs==Arg.EMPTY;
                assert a.lhs().type()==Exectype.DOUBLE;
                break;
	    case ScopeReturnCheck:
	    case InHeapCheck:
		assert a.rhs.length==1;
		assert a.lhs()==Var.VOID;
		assert a.rhs[0].type().isObject();
		break;
	    case LikelyZero:
	    case LikelyNonZero:
            case SemanticallyLikelyZero:
            case SemanticallyLikelyNonZero:
		assert a.rhs.length==1;
		assert a.rhs[0].type()==Exectype.INT;
		assert a.lhs().type()==Exectype.INT;
		break;
	    case CheckException:
	    case ClearException:
	    case Fence:
	    case CompilerFence:
            case HardCompilerFence:
            case PollcheckFence:
		assert a.rhs==Arg.EMPTY;
		assert a.lhs()==Var.VOID;
		break;
	    case IntToPointerZeroFill:
		assert a.rhs.length==1;
		assert a.rhs[0].type()==Exectype.INT;
		assert a.lhs().type()==Exectype.POINTER;
		break;
	    case Mov:
	    case Ipsilon:
            case CastNonZero:
		assert a.rhs.length==1;
		assert a.rhs[0].type().isSubtypeOf(a.lhs().type());
		assert a.lhs().type()!=Exectype.VOID;
		break;
	    case Neg:
		assert a.rhs.length==1;
		assert a.lhs().type()==a.rhs[0].type();
		assert a.lhs().type().effectiveBasetype().isNumber;
		break;
	    case Not:
            case Boolify:
	    case BitNot:
		assert a.rhs.length==1;
		assert a.lhs().type()==a.rhs[0].type();
		assert a.lhs().type().effectiveBasetype().isInteger;
		break;
	    case MonitorEnter:
	    case MonitorExit:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==1;
		assert a.rhs[0].type().isObject();
		assertNoPollcheckRules(a);
		break;
	    case ArrayLength:
		assert a.lhs().type()==Exectype.INT;
		assert a.rhs.length==1;
		assert a.rhs[0].type().isArray() || a.rhs[0].type()==Exectype.NULL;
		break;
	    case Add:
	    case Sub:
	    case Mul:
	    case Div:
	    case Mod:
		assert a.rhs.length==2;
		assert a.lhs().type()==a.rhs[0].type();
		assert a.lhs().type()==a.rhs[1].type();
		assert a.lhs().type().effectiveBasetype().isNumber;
		break;
            case FXor:
		assert a.rhs.length==2;
		assert a.lhs().type()==a.rhs[0].type();
		assert a.lhs().type()==a.rhs[1].type();
		assert a.lhs().type().effectiveBasetype().isFloat;
		break;
            case Sqrt:
		assert a.rhs.length==1;
		assert a.lhs().type()==a.rhs[0].type();
		assert a.lhs().type().effectiveBasetype().isFloat;
		break;
	    case Shl:
	    case Shr:
	    case Ushr:
		assert a.rhs.length==2;
		assert a.lhs().type()==a.rhs[0].type();
		assert a.lhs().type().effectiveBasetype().isInteger;
		assert a.rhs[1].type().effectiveBasetype().isInteger;
		assert a.rhs[1].type()==Exectype.INT;
		break;
	    case And:
	    case Or:
	    case Xor:
		assert a.rhs.length==2;
		assert a.lhs().type()==a.rhs[0].type();
		assert a.lhs().type()==a.rhs[1].type();
		assert a.lhs().type().effectiveBasetype().isInteger;
		break;
	    case CompareL:
	    case CompareG:
	    case LessThan:
            case LessThanEq:
	    case ULessThan:
            case ULessThanEq:
		assert a.rhs.length==2;
		assert a.lhs().type()==Exectype.INT;
		assert a.rhs[0].type()==a.rhs[1].type();
		assert a.rhs[0].type().effectiveBasetype().isNumber;
		break;
	    case Eq:
            case Neq:
		assert a.rhs.length==2;
		assert a.rhs[0].type().effectiveBasetype()
		    == a.rhs[1].type().effectiveBasetype();
		assert a.lhs().type()==Exectype.INT;
		break;
	    case NullCheck:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==1;
		assert a.rhs[0].type().isObject();
		break;
	    case CheckDivisor:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==1;
		assert a.rhs[0].type().effectiveBasetype().isNumber;
		break;
	    case PollCheck:
		assert a.lhs()==Var.VOID;
		assert a.rhs==Arg.EMPTY;
		assertNoPollcheckRules(a);
		break;
	    case ArrayBoundsCheck:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==2;
		assert a.rhs[0].type().isArray() || a.rhs(0).type()==Exectype.NULL;
		assert a.rhs[1].type()==Exectype.INT;
		break;
	    case ArrayCheckStore:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==2;
		assert a.rhs[0].type().isArray();
		assert a.rhs[0].type().arrayElement().isObject();
		assert a.rhs[1].type().isObject();
		break;
	    case GetTypeDataForObject:
		assert a.rhs.length==1;
		assert a.rhs[0].type().isObject()
		    || a.rhs[0].type()==Exectype.POINTER;
		assert a.lhs().type()==Exectype.POINTER;
		break;
            case Memcpy:
                assert a.rhs.length==3;
                assert a.lhs()==Var.VOID;
                assert a.rhs[0].type()==Exectype.POINTER;
                assert a.rhs[1].type()==Exectype.POINTER;
                assert a.rhs[2].type()==Exectype.POINTER;
                break;
	    case Phantom:
		// anything goes!
		break;
	    case PhantomCheck:
		assert a.lhs()==Var.VOID;
		break;
	    case HardUse:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==1;
		break;
	    case Phi:
		assert a.rhs.length==1;
		assert a.lhs()==a.rhs[0];
		break;
	    default:
	        assert false: a.opcode;
		break;
	    }
	    return visit((Instruction)a);
	}
        
        public Void visit(MemoryAccessInst a) {
            switch (a.opcode()) {
	    case Load:
		assert a.type.asExectype().isSubtypeOf(a.lhs().type());
		assert a.lhs()!=Var.VOID;
		assert a.rhs.length==1;
		assert a.rhs[0].type()==Exectype.POINTER;
		break;
	    case Store:
		assert a.rhs.length==2;
		assert a.lhs()==Var.VOID;
		assert a.rhs[0].type()==Exectype.POINTER;
		assert a.rhs[1].type()==a.type.asExectype();
		break;
	    case StrongLoadCAS:
		assert a.type==Type.INT || a.type.effectiveBasetype().isReference;
		assert a.type.asExectype().isSubtypeOf(a.lhs().type());
		assert a.rhs.length==3;
		assert a.rhs[0].type()==Exectype.POINTER;
		assert a.rhs[1].type().isSubtypeOf(a.type.asExectype());
		assert a.rhs[2].type().isSubtypeOf(a.type.asExectype());
                assert a.volatility.isVolatile();
		break;
	    case StrongCAS:
	    case WeakCAS:
		assert a.type==Type.INT || a.type.effectiveBasetype().isReference;
		assert a.lhs().type()==Exectype.INT;
		assert a.rhs.length==3;
		assert a.rhs[0].type()==Exectype.POINTER;
		assert a.rhs[1].type().isSubtypeOf(a.type.asExectype());
		assert a.rhs[2].type().isSubtypeOf(a.type.asExectype());
                assert a.volatility.isVolatile();
		break;
	    case StrongVoidCAS:
		assert a.type==Type.INT || a.type.effectiveBasetype().isReference;
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==3;
		assert a.rhs[0].type()==Exectype.POINTER;
		assert a.rhs[1].type().isSubtypeOf(a.type.asExectype());
		assert a.rhs[2].type().isSubtypeOf(a.type.asExectype());
                assert a.volatility.isVolatile();
		break;
            default:
                assert !true: a.opcode;
                break;
            }
	    return visit((Instruction)a);
        }
    
	public Void visit(TypeInst a) {
	    switch (a.opcode) {
	    case ThrowRTEOnZero:
		assert a.type.isSubtypeOf(Global.root().throwableType);
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==1;
		assert a.rhs[0].type().effectiveBasetype().isValue;
		break;
	    case GetType:
		assert Global.root().classType.asExectype().isSubtypeOf(a.lhs().type());
		assert a.rhs==Arg.EMPTY;
		break;
	    case GetTypeData:
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs==Arg.EMPTY;
                assert a.type.resolved();
		break;
	    case New:
		assert a.type.asExectype().isSubtypeOf(a.lhs().type());
		assert a.type.hasClass();
		assert a.rhs==Arg.EMPTY;
		assertNoPollcheckRules(a);
		break;
	    case NewArray:
		assert a.type.asExectype().isSubtypeOf(a.lhs().type());
		assert a.type.isArray();
		assert a.rhs.length==1;
		assert a.rhs[0].type()==Exectype.INT;
		assertNoPollcheckRules(a);
		break;
	    case Cast:
            case CastExact:
		assert a.type.asExectype().isSubtypeOf(a.lhs().type());
		assert a.rhs.length==1;
		assert a.rhs[0].type()!=Exectype.VOID;
                // if casting from a non-integer to an integer, then the target
                // of the cast should not be smaller than 32-bit.  this is a "silly"
                // rule that we institute purely to make the backend's job easier.
                if (a.rhs(0).type().effectiveBasetype().isInteger !=
                    a.type.effectiveBasetype().isInteger) {
                    assert a.type.asExectype().effectiveBasetype()
                        == a.type.effectiveBasetype()
                        : a.type;
                }
		break;
            case Fiat:
                assert a.type.asExectype().isSubtypeOf(a.lhs().type());
                assert a.rhs.length==1;
                assert a.rhs[0].type()!=Exectype.VOID;
                assert !a.rhs[0].type().isObject();
                assert !a.lhs().type().isObject();
                // currently we only support the following fiats:
                // - long to anything
                // - fiats between ints and non-ints of same size
                // this assertion is thus because we just don't have the code to
                // support anything else.
                assert a.rhs[0].type()==Exectype.LONG
                    || (a.rhs[0].effectiveBasetype().isNumber &&
                        a.type.effectiveBasetype().isNumber &&
                        a.rhs[0].isInteger()!=a.type.isInteger() &&
                        a.rhs[0].type()!=Exectype.POINTER &&
                        a.type.asExectype()!=Exectype.POINTER &&
                        a.rhs[0].effectiveBasetype().bytes
                        == a.type.effectiveBasetype().bytes);
                break;
	    case Instanceof:
		assert a.rhs.length==1;
		assert a.rhs[0].type().effectiveBasetype()==Basetype.OBJECT;
		assert a.lhs().type()==Exectype.INT;
		break;
	    case GetException:
		assert a.type.asExectype().isSubtypeOf(a.lhs().type());
		assert a.type.isSubtypeOf(Global.root().throwableType);
		assert a.rhs==Arg.EMPTY;
		break;
	    case OffsetOfElement:
		assert a.type.isArray();
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs.length==1;
		assert a.rhs[0].type()==Exectype.INT;
		break;
	    default:
	        assert false:a.opcode;
		break;
	    }
	    return visit((Instruction)a);
	}
	
	public Void visit(DebugIDInfoInst d) {
            switch (d.opcode()) {
            case SaveDebugID:
                assert d.lhs()==Var.VOID;
                assert d.rhs==Arg.EMPTY;
                break;
            case GetDebugID:
                assert d.lhs().type()==Exectype.POINTER;
                assert d.rhs==Arg.EMPTY;
                assert d.didi.di==d.di;
                break;
            default:
                assert false:d.opcode;
                break;
            }
	    return visit((Instruction)d);
	}
	
	public Void visit(TypeCheckInst t) {
	    assert t.opcode()==OpCode.TypeCheck;
	    assert t.lhs().type().isObject();
	    assert t.rhs.length==1;
	    assert t.rhs[0].type().isObject();
	    assert t.typeToThrow.isSubtypeOf(Global.root().throwableType);
	    assert t.typeToCheck.isObject();
	    assert t.typeToCheck.asExectype().isSubtypeOf(t.lhs().type());
	    return visit((Instruction)t);
	}
	
	public Void visit(HeapAccessInst a) {
	    switch (a.opcode()) {
	    case AddressOfStatic:
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs==Arg.EMPTY;
                assert a.field instanceof VisibleField;
		assert a.field.isStatic();
		break;
	    case AddressOfField:
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs.length==1;
                assert a.field instanceof VisibleField;
		assert a.rhs[0].type().isSubtypeOf(Exectype.make(((VisibleField)a.field).getClazz()));
		assert ((VisibleField)a.field).isInstance();
		break;
	    case GetStatic:
		assert a.rhs==Arg.EMPTY;
		assert a.field instanceof VisibleField;
		assert ((VisibleField)a.field).getType().asExectype().isSubtypeOf(a.lhs().type());
		assert ((VisibleField)a.field).isStatic();
		break;
	    case GetField:
		assert a.rhs.length==1;
		assert a.field instanceof VisibleField;
		assert a.rhs[0].type().isSubtypeOf(Exectype.make(((VisibleField)a.field).getClazz()));
		assert a.fieldType().asExectype().isSubtypeOf(a.lhs().type());
		assert ((VisibleField)a.field).isInstance();
		break;
	    case PutStatic:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==1;
		assert a.field instanceof VisibleField;
		assert a.rhs[0].type().isSubtypeOf(((VisibleField)a.field).getType().asExectype());
		assert ((VisibleField)a.field).isStatic();
		break;
	    case PutField:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==2;
		assert a.field instanceof VisibleField;
		assert a.rhs[0].type().isSubtypeOf(Exectype.make(((VisibleField)a.field).getClazz()));
		assert a.rhs[1].type().isSubtypeOf(((VisibleField)a.field).getType().asExectype());
		assert ((VisibleField)a.field).isInstance();
		break;
	    case WeakCASStatic:
		assert a.lhs().type()==Exectype.INT;
		assert a.rhs.length==2;
		assert a.field instanceof VisibleField;
		assert a.rhs[0].type().isSubtypeOf(((VisibleField)a.field).getType().asExectype());
		assert a.rhs[1].type().isSubtypeOf(((VisibleField)a.field).getType().asExectype());
		assert ((VisibleField)a.field).isStatic();
		assertCASableType(((VisibleField)a.field()).getType().effectiveBasetype());
		break;
	    case WeakCASField:
		assert a.lhs().type()==Exectype.INT;
		assert a.rhs.length==3;
		assert ((VisibleField)a.field).isInstance();
		assert a.rhs[0].type().isSubtypeOf(Exectype.make(((VisibleField)a.field).getClazz()));
		assert a.rhs[1].type().isSubtypeOf(((VisibleField)a.field).getType().asExectype());
		assert a.rhs[2].type().isSubtypeOf(((VisibleField)a.field).getType().asExectype());
		assertCASableType(((VisibleField)a.field()).getType().effectiveBasetype());
		break;
	    case ArrayLoad:
		assert a.rhs.length==2;
                if (a.rhs(0).type()!=Exectype.NULL) {
                    assert a.rhs[0].type().isArray();
                    assert a.rhs[0].type().arrayElement().asExectype().isSubtypeOf(a.lhs().type());
                }
		assert a.rhs[1].type()==Exectype.INT;
		assert a.field==ArrayElementField.INSTANCE;
		break;
	    case ArrayStore:
		assert a.lhs()==Var.VOID;
		assert a.rhs.length==3;
                if (a.rhs(0).type()!=Exectype.NULL) {
                    assert a.rhs[0].type().isArray();
                    assert a.rhs[2].type().effectiveBasetype()==a.rhs[0].type().arrayElement().asExectype().effectiveBasetype();
                }
                assert a.rhs[1].type()==Exectype.INT;
		assert a.field==ArrayElementField.INSTANCE;
		break;
	    case WeakCASElement:
		assert a.lhs().type()==Exectype.INT;
		assert a.rhs.length==4;
		assert a.rhs[1].type()==Exectype.INT;
		assert a.field==ArrayElementField.INSTANCE;
                if (a.rhs(0).type()!=Exectype.NULL) {
                    assert a.rhs[0].type().isArray();
                    assert a.rhs[2].type().effectiveBasetype()==a.rhs[0].type().arrayElement().asExectype().effectiveBasetype();
                    assert a.rhs[3].type().effectiveBasetype()==a.rhs[0].type().arrayElement().asExectype().effectiveBasetype();
                }
                assert a.rhs(2).type().effectiveBasetype()==a.rhs(3).type().effectiveBasetype();
                assertCASableType(a.rhs[2].type().effectiveBasetype());
		break;
	    case AddressOfElement:
		assert a.lhs().type()==Exectype.POINTER;
		assert a.rhs.length==2;
		assert a.rhs[0].type().isArray() || a.rhs(0).type()==Exectype.NULL;
		assert a.rhs[1].type()==Exectype.INT;
                assert a.field==ArrayElementField.INSTANCE;
		break;
	    default:
		assert false: a.opcode;
		break;
	    }
	    return visit((Instruction)a);
	}
    }
    
    MyVisitor v=new MyVisitor();
    
    public void visitCode() {
	doit();
    }
    
    public void doit() {
	// only run if we have assertions turned on
	boolean asserts=false;
	assert asserts=true;
	if (!asserts) {
            throw new Error("assertions must be enabled.");
        }
        try {
            code.recomputeOrder();
            
            HashSet< Integer > permIDs=new HashSet< Integer >();
            
            for (int i=0;i<code.vars().size();++i) {
                Var v=code.vars().get(i);
                try {
                    assert v.type()!=Exectype.VOID;
                    assert v.type()!=Exectype.TOP;
                    if (code.checksInserted) {
                        assert v.type()!=Exectype.BOTTOM;
                    }
                    assert v.id==i : "v.id = "+v.id+", i = "+i;
                    assert v.inst()==null || v.inst().lhs()==v : "v = "+v+", i = "+i;
                    if (code.isSSA()) {
                        assert !v.isMultiAssigned() : v;
                    }
                    assert v.code()==code;
                    assert permIDs.add(v.permID) : "new variable: "+v+", permIDs = "+permIDs;
                } catch (Throwable e) {
                    throw new SanityCheckFailed(
                        "Sanity checking failed for "+v+", vars = "+code.vars(),e);
                }
            }
                
            // this is *intentionally* duplicating AssignCalc
            TwoWayMap< Var, Instruction > vi=new TwoWayMap< Var, Instruction >();
            for (Header h : code.headers()) {
                for (Instruction i : h.instructions()) {
                    if (i.lhs()!=Var.VOID) {
                        vi.put(i.lhs(),i);
                    }
                }
            }

            if (code.isSSA()) {
                // do SSA verification if we're in SSA
                
                PredsCalc pc=new PredsCalc(code);
                
                for (Var v : vi.keySet()) {
                    Set< Instruction > is=vi.valuesForKey(v);
                    if (is.size()==0) {
                        // ok, variable is totally dead
                    } else if (is.size()==1) {
                        Instruction i=is.iterator().next();
                        
                        // have to be careful - any optimizer that terminates basic
                        // blocks early but doesn't eliminate unreachable ones (like
                        // OptConst) may leave lonely Phi functions.  a lonely Phi
                        // function is fine so long as the block it's in is dead.
                        
                        assert i.lhs()==v;
                        assert v.inst()==i;
                        
                        assert i.opcode()!=OpCode.Phi 
                            || pc.isDead(i.head())
                            : "for v = "+v+", is = "+is;
                    } else {
                        assert is.size()>1 : "for v = "+v+", is = "+is;
                        boolean foundPhi=false;
                        for (Instruction i : is) {
                            if (i.opcode()==OpCode.Phi) {
                                assert !foundPhi : "for v = "+v+", is = "+is;
                                foundPhi=true;
                                
                                assert i.lhs()==v;
                                assert v.inst()==i;
                            } else if (i.opcode()==OpCode.Ipsilon) {
                                // ok!
                            } else {
                                assert false : "for v = "+v+", is = "+is;
                            }
                        }
                        assert foundPhi;
                    }
                }
            } else {
                // if we're not in SSA then make sure that any Vars that claim to know
                // their inst() actually do.
                
                for (Var v : vi.keySet()) {
                    Set< Instruction > is=vi.valuesForKey(v);
                    if (is.size()==0) {
                        // ok!
                    } else if (is.size()==1) {
                        Instruction i=is.iterator().next();
                        
                        assert i.lhs()==v : "i = "+i+", v = "+v;
                        assert v.inst()==null || v.inst()==i : "i = "+i+", v = "+v;
                    } else {
                        assert v.inst()==null : "v = "+v+", is = "+is+", v.inst() = "+v.inst();
                        assert v.isMultiAssigned() : "v = "+v+", is = "+is;
                    }
                }
            }
            
            // ensure that there are no variables live at the top of the program.
            VarSet liveAtRoot=new SimpleLivenessCalc(code).liveAtHead(code.root());
            assert liveAtRoot.isEmpty() : liveAtRoot;
                
            allowedVars.addAll(code.vars());
            allowedHeaders.addAll(code.headers());
            allowedHandlers.addAll(code.handlers());
            boolean foundRoot=false;
            for (Header h : code.headers()) {
                if (h==code.root) foundRoot=true;
                try {
                    h.accept(v);
                    for (Operation o : h.operations()) {
                        assert o.head==h : "o = "+o+", h = "+h+", o.head = "+o.head;
                        try {
                            o.accept(v);
                        } catch (ResolutionFailed e) {
                            throw e;
                        } catch (Throwable e) {
                            throw new SanityCheckFailed(
                                "Sanity checking failed for "+o,e);
                        }
                    }
                } catch (ResolutionFailed e) {
                    throw e;
                } catch (Throwable e) {
                    throw new SanityCheckFailed(
                        "Sanity checking failed for "+h,e);
                }
            }
            assert foundRoot==true : code.headers();
            for (ExceptionHandler eh : code.handlers()) {
                try {
                    eh.accept(v);
                } catch (ResolutionFailed e) {
                    throw e;
                } catch (Throwable e) {
                    throw new SanityCheckFailed(
                        "Sanity checking failed for "+eh,e);
                }
            }
        } catch (ResolutionFailed e) {
            throw e;
        } catch (Throwable e) {
            throw new SanityCheckFailed("Sanity checking failed for "+code,e);
        }
    }
    
}


