/*
 * OpEquals.java
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

public class OpEquals extends Visitor< Boolean > {
    public static boolean get(Operation a,
			      Operation b) {
	return new OpEquals(b,LHSMode.CARE_ABOUT_LHS).get(a);
    }
    
    public static boolean getNoLhs(Operation a,
                                   Operation b) {
        return new OpEquals(b,LHSMode.DONT_CARE_ABOUT_LHS).get(a);
    }
    
    private Operation other_;
    
    private LHSMode lhsMode;

    public OpEquals(Operation other,
                    LHSMode lhsMode) {
	this.other_=other;
        this.lhsMode=lhsMode;
    }
    
    public boolean get(Operation a) {
	if (a.opcode()!=other_.opcode()) return false;
	return a.accept(this);
    }
    
    boolean headerEquals(Header a,Header b) {
	return a==b;
    }
    
    boolean argEquals(Arg a,Arg b) {
	return a.structuralEquals(b);
    }
    
    boolean varEquals(Var a,Var b) {
	return argEquals(a,b);
    }
    
    public Boolean visit(Node n) {
	throw new Error("should not get here");
    }
    
    public Boolean visit(Operation o) {
	if (o.rhs().length!=other_.rhs().length) return false;
	for (int i=0;i<o.rhs().length;++i) {
	    if (!argEquals(o.rhs(i),other_.rhs(i))) return false;
	}
	return true;
    }
    
    public Boolean visit(Control c) {
	Control other=(Control)other_;
	if (c.defaultSuccessor()!=other.defaultSuccessor()) return false;
	return visit((Operation)c);
    }
    
    public Boolean visit(Switch s) {
	Switch other=(Switch)other_;
	if (s.numCases()!=other.numCases()) return false;
	for (int i=0;i<s.numCases();++i) {
	    if (s.value(i)!=other.value(i)) return false;
	    if (!headerEquals(s.target(i),other.target(i))) return false;
	}
	return visit((Control)s);
    }
    
    public Boolean visit(AwesomeJump aj) {
        AwesomeJump other=(AwesomeJump)other_;
        if (aj.numTargets()!=other.numTargets()) return false;
        for (int i=0;i<aj.numTargets();++i) {
            if (!headerEquals(aj.target(i),other.target(i))) return false;
        }
        return visit((Control)aj);
    }
    
    public Boolean visit(Branch b) {
	Branch other=(Branch)other_;
	if (!headerEquals(b.target(),other.target())) return false;
	return visit((Control)b);
    }
    
    public Boolean visit(Instruction i) {
	Instruction other=(Instruction)other_;
        if (lhsMode==LHSMode.CARE_ABOUT_LHS) {
            if (!varEquals(i.lhs(),other.lhs())) return false;
        }
	return visit((Operation)i);
    }
    
    public Boolean visit(PatchPoint p) {
        PatchPoint other=(PatchPoint)other_;
        if (p.neededClass()!=other.neededClass()) return false;
        if (!p.description().equals(other.description())) return false;
        if (p.bcOffset()!=other.bcOffset()) return false;
        if (p.nLocals()!=other.nLocals()) return false;
        if (p.nStack()!=other.nStack()) return false;
        return visit((Instruction)p);
    }
    
    public Boolean visit(PatchPointFooter p) {
        PatchPointFooter other=(PatchPointFooter)other_;
        if (p.neededClass()!=other.neededClass()) return false;
        if (!p.description().equals(other.description())) return false;
        if (p.bcOffset()!=other.bcOffset()) return false;
        if (p.nLocals()!=other.nLocals()) return false;
        if (p.nStack()!=other.nStack()) return false;
        return visit((Footer)p);
    }
    
    public Boolean visit(ArgInst a) {
	ArgInst other=(ArgInst)other_;
	if (a.getIdx()!=other.getIdx()) return false;
	return visit((Instruction)a);
    }
    
    public Boolean visit(CFieldInst c) {
	CFieldInst other=(CFieldInst)other_;
	if (!c.field().equals(other.field())) return false;
	return visit((Instruction)c);
    }
    
    public Boolean visit(CTypeInst c) {
	CTypeInst other=(CTypeInst)other_;
	if (!c.ctype().equals(other.ctype())) return false;
	return visit((Instruction)c);
    }
    
    public Boolean visit(CMacroInst c) {
	CMacroInst other=(CMacroInst)other_;
	if (!c.cmacro().equals(other.cmacro())) return false;
	return visit((Instruction)c);
    }
    
    public Boolean visit(FieldInst f) {
	FieldInst other=(FieldInst)other_;
	if (f.field()!=other.field()) return false;
	return visit((Instruction)f);
    }
    
    public Boolean visit(ClassInst c) {
	ClassInst other=(ClassInst)other_;
	if (c.value()!=other.value()) return false;
	return visit((Instruction)c);
    }
    
    public Boolean visit(GetStringInst s) {
	GetStringInst other=(GetStringInst)other_;
	if (!s.value().equals(other.value())) return false;
	return visit((Instruction)s);
    }
    
    public Boolean visit(GetMethodInst m) {
        GetMethodInst other=(GetMethodInst)other_;
        if (m.method()!=other.method()) return false;
        return visit((Instruction)m);
    }
    
    public Boolean visit(MethodInst m) {
	MethodInst other=(MethodInst)other_;
	if (m.method()!=other.method()) return false;
	if (!m.refinement().equals(other.refinement())) return false;
	return visit((Instruction)m);
    }
    
    public Boolean visit(IndirectMethodInst m) {
	IndirectMethodInst other=(IndirectMethodInst)other_;
	if (!m.signature().equals(other.signature())) return false;
	return visit((Instruction)m);
    }
    
    public Boolean visit(ResolvedMethodInst m) {
	ResolvedMethodInst other=(ResolvedMethodInst)other_;
	if (!m.signature().equals(other.signature())) return false;
        if (!m.function().equals(other.function())) return false;
	return visit((Instruction)m);
    }
    
    public Boolean visit(MultiNewArrayInst m) {
	MultiNewArrayInst other=(MultiNewArrayInst)other_;
	if (m.type()!=other.type()) return false;
	if (m.dimensions()!=other.dimensions()) return false;
	return visit((Instruction)m);
    }
    
    public Boolean visit(CallIndirectInst c) {
	CallIndirectInst other=(CallIndirectInst)other_;
	if (c.result()!=other.result()) return false;
	if (!Util.equals(c.params(),other.params())) return false;
	return visit((Instruction)c);
    }
    
    public Boolean visit(TypeInst t) {
	TypeInst other=(TypeInst)other_;
	if (t.getType()!=other.getType()) return false;
	return visit((Instruction)t);
    }
    
    public Boolean visit(MemoryAccessInst t) {
	MemoryAccessInst other=(MemoryAccessInst)other_;
	if (t.getType()!=other.getType()) return false;
        if (t.mutability()!=other.mutability()) return false;
        if (t.volatility()!=other.volatility()) return false;
	return visit((Instruction)t);
    }
    
    public Boolean visit(DebugIDInfoInst d) {
	DebugIDInfoInst other=(DebugIDInfoInst)other_;
	if (!d.didi().equals(other.didi())) return false;
	return visit((Instruction)d);
    }
    
    public Boolean visit(TypeCheckInst t) {
	TypeCheckInst other=(TypeCheckInst)other_;
	if (t.typeToCheck!=other.typeToCheck) return false;
	if (t.typeToThrow!=other.typeToThrow) return false;
	return visit((Instruction)t);
    }
    
    public Boolean visit(HeapAccessInst b) {
	HeapAccessInst other=(HeapAccessInst)other_;
	if (!b.mode.equals(other.mode)) return false;
	if (!b.field.equals(other.field)) return false;
	return visit((Instruction)b);
    }
}


