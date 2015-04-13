/*
 * Instruction.java
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

import java.util.Set;
import java.nio.*;
import com.fiji.fivm.*;

/**
 * The base class for instructions: operations that may either produce side-effects
 * or a resulting value that may be stored into a variable, or both.  The execution
 * of an instruction may be followed by the execution of the operation that follows it
 * (stored in the next field inherited from Node), or in control being diverted to an
 * exception handler, or in the method being popped off the stack to allow for exception
 * propagation to happen into the caller.  This class
 * is never instantiated directly (hence it's abstract); there are a bunch of instruction
 * sub-classes that represent various kinds of instructions, and hold additional
 * meta-data (for example, the type for a type check, or the field for a field access,
 * etc.).  Instructions are also distinguished in that they live between the header
 * and footer of a basic block.  A basic block may have a variable number of instructions,
 * and this class has extensive support for adding, deleting, and reordering
 * instructions.  In particular, this class leverages the prev/next fields found in Node
 * to implement this list of instructions as a doubly-linked list.  It is both legal
 * and reasonable to manipulate these fields directly for instruction list transformations
 * that aren't already supported by the methods in this class, in Operation, and in
 * Header.
 */
public abstract class Instruction extends Operation {
    
    private Var lhs;
    
    // internal constructor, used only by Var.  don't use anywhere else.
    Instruction() {
        super(null,null,null);
    }
    
    Instruction(DebugInfo di,
		OpCode opcode,
		Var lhs,
		Arg[] rhs) {
	super(di,opcode,rhs);
	this.lhs=lhs;
        if (opcode!=OpCode.Ipsilon) {
            lhs.setInst(this);
        }
    }
    
    public Var lhs() {
	return lhs;
    }
    
    public void setLhs(Var lhs) {
        if (this.lhs==lhs) {
            return;
        }
        
        if (opcode!=OpCode.Ipsilon) {
            if (this.lhs.inst()==this) {
                this.lhs.clearInst();
            }
            lhs.setInst(this);
        }
        this.lhs=lhs;
    }
    
    public boolean hasLhs() { return lhs!=Var.VOID; }

    /** Remove this instruction. */
    public void remove() {
	prev.next=next;
	next.prev=prev;
    }
    
    public static void swap(Instruction first,Instruction second) {
	Node before=first.prev;
	Node after=second.next;
	before.next=second;
	after.prev=first;
	first.prev=second;
	first.next=after;
	second.prev=before;
	second.next=first;
    }
    
    /** Swap this instruction with the one before it. */
    public void swapWithPrev() {
	swap((Instruction)prev,this);
    }
    
    /** Insert the given instruction after this instruction. */
    public Instruction append(Instruction i) {
	i.next=next;
	i.prev=this;
	next.prev=i;
	next=i;
        i.head=head;
	return i;
    }
    
    public Instruction copy() {
        return (Instruction)super.copy();
    }
    
    public void noticeMultiAssign() {
        lhs.setInst(this);
    }
    
    public Instruction copyAndMultiAssign() {
        Instruction result=(Instruction)super.copyAndMultiAssign();
        if (result.opcode()!=OpCode.Ipsilon) {
            result.noticeMultiAssign();
        }
        return result;
    }
    
    public void replaceVars(Gettable< ? super Arg, ? extends Var > map) {
	super.replaceVars(map);
	Var result=map.get(lhs);
	if (result!=null) {
	    setLhs(result);
	}
    }
    
    public boolean defs(Var v) {
	return v==lhs;
    }
    
    public boolean defsAny(Set< ? > vars) {
	return vars.contains(lhs);
    }
    
    int getNioSize() {
        return super.getNioSize()+4;
    }
    
    void writeTo(NioContext ctx,
                 ByteBuffer buffer) {
        super.writeTo(ctx,buffer);
        buffer.putInt(lhs.id);
    }
    
    public static CompactArrayList< Instruction > EMPTY_CAL=new CompactArrayList< Instruction >();
}


