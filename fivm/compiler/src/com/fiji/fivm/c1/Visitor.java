/*
 * Visitor.java
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

public class Visitor<T> {
    
    public T visit(Node n) { return null; }
    
    public T visit(ExceptionHandler e) {
	return visit((Node)e);
    }
    
    public T visit(Header h) {
	return visit((Node)h);
    }
    
    public T visit(Operation o) {
	return visit((Node)o);
    }
    
    public T visit(Footer f) {
	return visit((Operation)f);
    }
    
    public T visit(Terminal t) {
	return visit((Footer)t);
    }
    
    public T visit(Control c) {
	return visit((Footer)c);
    }
    
    public T visit(Jump j) {
	return visit((Control)j);
    }
    
    public T visit(Branch b) {
	return visit((Control)b);
    }
    
    public T visit(Switch s) {
	return visit((Control)s);
    }
    
    public T visit(AwesomeJump a) {
	return visit((Control)a);
    }
    
    public T visit(Instruction i) {
	return visit((Operation)i);
    }
    
    public T visit(PatchPoint t) {
	return visit((Instruction)t);
    }
    
    public T visit(PatchPointFooter t) {
	return visit((Footer)t);
    }
    
    public T visit(ArgInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(CFieldInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(CTypeInst c) {
	return visit((Instruction)c);
    }
    
    public T visit(CMacroInst c) {
	return visit((Instruction)c);
    }
    
    public T visit(CallIndirectInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(ResolvedMethodInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(IndirectMethodInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(DebugIDInfoInst d) {
	return visit((Instruction)d);
    }
    
    public T visit(FieldInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(ClassInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(GetStringInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(GetMethodInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(MethodInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(MultiNewArrayInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(SimpleInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(TypeInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(MemoryAccessInst a) {
	return visit((Instruction)a);
    }
    
    public T visit(TypeCheckInst t) {
	return visit((Instruction)t);
    }
    
    public T visit(HeapAccessInst b) {
	return visit((Instruction)b);
    }
}

