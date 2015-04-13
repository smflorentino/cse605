/*
 * OpHashCode.java
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

public class OpHashCode extends Visitor< Integer > {
    private static OpHashCode h=new OpHashCode(LHSMode.CARE_ABOUT_LHS);
    private static OpHashCode hNoLhs=new OpHashCode(LHSMode.DONT_CARE_ABOUT_LHS);
    
    public static int get(Operation o) {
	try {
	    return o.accept(h);
	} catch (Throwable e) {
	    throw new CompilerException("Could not get hash code for "+o,e);
	}
    }
    
    public static int getNoLhs(Operation o) {
	try {
	    return o.accept(hNoLhs);
	} catch (Throwable e) {
	    throw new CompilerException("Could not get hash code for "+o,e);
	}
    }
    
    private LHSMode lhsMode;
    
    private OpHashCode(LHSMode lhsMode) {
        this.lhsMode=lhsMode;
    }
    
    int hashHeader(Header h) {
	return h.hashCode();
    }
    
    int hashArg(Arg a) {
	return a.structuralHashCode();
    }
    
    int hashVar(Var v) {
	return hashArg(v);
    }
    
    public Integer visit(Node n) {
	throw new Error("should not get here");
    }
    
    public Integer visit(Operation o) {
	int result=o.opcode().hashCode();
	for (int i=0;i<o.rhs().length;++i) {
	    result=result*31+hashArg(o.rhs(i));
	}
	return result;
    }
    
    public Integer visit(Control c) {
	return hashHeader(c.defaultSuccessor())+5*visit((Operation)c);
    }
    
    public Integer visit(Switch s) {
	int result=0;
	for (int i=0;i<s.numCases();++i) {
	    result=result*43+s.value(i);
	    result=result*11+hashHeader(s.target(i));
	}
	return result+41*visit((Control)s);
    }
    
    public Integer visit(AwesomeJump aj) {
	int result=0;
	for (int i=0;i<aj.numTargets();++i) {
	    result=result*13+hashHeader(aj.target(i));
	}
	return result+43*visit((Control)aj);
    }
    
    public Integer visit(Branch b) {
	return 3*visit((Control)b)+hashHeader(b.target());
    }
    
    public Integer visit(Instruction i) {
        int result=39*visit((Operation)i);
        if (lhsMode==LHSMode.CARE_ABOUT_LHS) {
            result+=hashVar(i.lhs());
        }
	return result;
    }
    
    public Integer visit(PatchPoint p) {
        return (p.neededClass()==null?0:p.neededClass().hashCode()) +
            p.description().hashCode()*3 +
            p.bcOffset() + p.nLocals() + p.nStack() +
            7*visit((Instruction)p);
    }
    
    public Integer visit(PatchPointFooter p) {
        return (p.neededClass()==null?0:p.neededClass().hashCode()) +
            p.description().hashCode()*3 +
            p.bcOffset() + p.nLocals() + p.nStack() +
            7*visit((Footer)p);
    }
    
    public Integer visit(ArgInst a) {
	return 7*visit((Instruction)a)+a.getIdx();
    }
    
    public Integer visit(CFieldInst c) {
	return 123*visit((Instruction)c)+c.field().hashCode();
    }
    
    public Integer visit(CTypeInst c) {
	return 69*visit((Instruction)c)+c.ctype().hashCode();
    }
    
    public Integer visit(CMacroInst c) {
	return 732947*visit((Instruction)c)+c.cmacro().hashCode();
    }
    
    public Integer visit(FieldInst f) {
	return 17*visit((Instruction)f)+f.field().hashCode();
    }
    
    public Integer visit(ClassInst c) {
	return 19*visit((Instruction)c)+c.value().hashCode();
    }
    
    public Integer visit(GetStringInst s) {
	return 7*visit((Instruction)s)+s.value().hashCode();
    }
    
    public Integer visit(GetMethodInst m) {
	return 17*visit((Instruction)m)+37*m.method().hashCode();
    }
    
    public Integer visit(MethodInst m) {
	return 23*visit((Instruction)m)+31*m.method().hashCode()+
	    m.refinement().hashCode();
    }
    
    public Integer visit(ResolvedMethodInst m) {
        return 79*visit((Instruction)m)+49*m.signature().hashCode()+
            m.function().hashCode();
    }
    
    public Integer visit(IndirectMethodInst m) {
        return 71*visit((Instruction)m)+41*m.signature().hashCode();
    }
    
    public Integer visit(MultiNewArrayInst m) {
	return 26*visit((Instruction)m)+3*m.type().hashCode()+m.dim;
    }
    
    public Integer visit(CallIndirectInst c) {
	return 2*visit((Instruction)c)+89*c.result().hashCode()+
	    Util.hashCode(c.params());
    }
    
    public Integer visit(TypeInst t) {
	return 42*visit((Instruction)t)+t.getType().hashCode();
    }
    
    public Integer visit(MemoryAccessInst t) {
	return 43*visit((Instruction)t)+t.getType().hashCode()+
            5*t.mutability().hashCode()+3*t.volatility().hashCode();
    }
    
    public Integer visit(DebugIDInfoInst d) {
	return 93*visit((Instruction)d)+d.didi().hashCode();
    }
    
    public Integer visit(TypeCheckInst t) {
	return 97*visit((Instruction)t)+t.typeToCheck.hashCode()*5+t.typeToThrow.hashCode()*3;
    }
    
    public Integer visit(HeapAccessInst b) {
	return 9901*visit((Instruction)b)+b.mode.hashCode()*3+b.field.hashCode();
    }
}

