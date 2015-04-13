/*
 * SuccessorReplacement.java
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

public class SuccessorReplacement extends Visitor< Void > {
    Node oldSucc;
    Node newSucc;
    
    public SuccessorReplacement(Node oldSucc,
				Node newSucc) {
	this.oldSucc=oldSucc;
	this.newSucc=newSucc;
    }
    
    public Void visit(Node n) {
	if (n.next==oldSucc) n.next=newSucc;
	return null;
    }
    
    public Void visit(ExceptionHandler eh) {
	if (eh.dropsTo==oldSucc) eh.dropsTo=(ExceptionHandler)newSucc;
	return visit((Node)eh);
    }
    
    public Void visit(Branch b) {
	if (b.target==oldSucc) b.target=(Header)newSucc;
	return visit((Control)b);
    }
    
    public Void visit(Switch s) {
	for (int i=0;i<s.targets.length;++i) {
	    if (s.targets[i]==oldSucc) s.targets[i]=(Header)newSucc;
	}
	return visit((Control)s);
    }

    public Void visit(AwesomeJump aj) {
        for (int i=0;i<aj.targets.length;++i) {
            if (aj.targets[i]==oldSucc) {
                aj.targets[i]=(Header)newSucc;
            }
        }
        return visit((Control)aj);
    }
    
    public Void visit(CFieldInst cfi) {
        if (cfi.field() instanceof JumpTable) {
            JumpTable jt=(JumpTable)cfi.field();
            Header[] oldH=jt.headers();
            Header[] newH=new Header[oldH.length];
            for (int i=0;i<oldH.length;++i) {
                if (oldH[i]==oldSucc) {
                    newH[i]=(Header)newSucc;
                } else {
                    newH[i]=oldH[i];
                }
            }
            cfi.field=JumpTable.make(jt.cname(),newH);
        }
        return visit((Instruction)cfi);
    }
}

