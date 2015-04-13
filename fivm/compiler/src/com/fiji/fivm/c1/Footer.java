/*
 * Footer.java
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

/**
 * The footer of a basic block.  This tells you about what happens to the control
 * flow when program execution reaches the end of the basic block.  Footers are
 * part of the Operation list of a basic block, and are also specially referenced
 * by the Header via Header.footer.  The Footer's Node.prev pointer points either
 * to the last Instruction in the block, or to the Header if there are no
 * Instructions.  The Footer's Node.next pointer is either null if the Footer is
 * a terminal, or points to the Footer's default successor.  The default successor
 * is either the jump target for a Jump footer, or the fall-through target for Branch
 * and Switch.  For Branch it is the block that the program execution jumps to if
 * the branch condition is false, while for Switch it is the block that the program
 * jumps to under the "default" case.
 */
public abstract class Footer extends Operation {
    
    Footer(DebugInfo di,OpCode opcode,Arg[] rhs) {
	super(di,opcode,rhs);
    }
    
    public void replace(Footer other) {
        head().setFooter(other);
    }
    
    public abstract boolean isTerminal();
    
    public boolean hasDefaultSuccessor() {
        return next!=null;
    }
    
    public Header defaultSuccessor() {
	return (Header)next;
    }
    
    /** Returns the successors.  Note that by convention, the default successor
	for conditionals comes first.  So, the target of a branch will come
	<i>after</i> the drop-down of the branch.  If you don't like this
	convention, use conditionalSuccessors() to get just the non-default
	ones. */
    public abstract Iterable< Header > successors();
    
    public Iterable< Header > likelySuccessors() {
        return successors();
    }
    
    public ArrayList< Header > successorList() {
        ArrayList< Header > result=new ArrayList< Header >();
        for (Header h : successors()) {
            result.add(h);
        }
        return result;
    }
    
    public Header[] successorArray() {
        int cnt=0;
        for (Header h : successors()) {
            cnt++;
        }
        Header[] result=new Header[cnt];
        cnt=0;
        for (Header h : successors()) {
            result[cnt++]=h;
        }
        assert cnt==result.length;
        return result;
    }
    
    public LinkedList< Header > conditionalSuccessors() {
	LinkedList< Header > result=new LinkedList< Header >();
	boolean seenDef=false;
	for (Header h : successors()) {
	    if (h!=next || seenDef) {
		result.add(h);
	    }
	    if (h==next) {
		seenDef=true;
	    }
	}
	return result;
    }
}


