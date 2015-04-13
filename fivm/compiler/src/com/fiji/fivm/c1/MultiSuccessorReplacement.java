/*
 * MultiSuccessorReplacement.java
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

public class MultiSuccessorReplacement extends Visitor< Void > {
    Gettable< ? super Node, ? extends Node > map;
    
    public MultiSuccessorReplacement(
	Gettable< ? super Node, ? extends Node > map) {
	this.map=map;
    }
    
    public MultiSuccessorReplacement(Map< ?, ? extends Node > map) {
	this.map=Gettable.wrap(map);
    }
    
    public Void visit(Node n) {
	Node newSucc=map.get(n.next);
	if (newSucc!=null) n.next=newSucc;
	return null;
    }
    
    public Void visit(Header h) {
	Node newHandler=map.get(h.handler);
	if (newHandler!=null) h.handler=(ExceptionHandler)newHandler;
	return visit((Node)h);
    }

    public Void visit(ExceptionHandler eh) {
	Node newSucc=map.get(eh.dropsTo);
	if (newSucc!=null) eh.dropsTo=(ExceptionHandler)newSucc;
	return visit((Node)eh);
    }
    
    public Void visit(Branch b) {
	Node newSucc=map.get(b.target);
	if (newSucc!=null) b.target=(Header)newSucc;
	return visit((Control)b);
    }
    
    public Void visit(Switch s) {
	for (int i=0;i<s.targets.length;++i) {
	    Node newSucc=map.get(s.targets[i]);
	    if (newSucc!=null) s.targets[i]=(Header)newSucc;
	}
	return visit((Control)s);
    }
    
    public Void visit(AwesomeJump aj) {
        for (int i=0;i<aj.targets.length;++i) {
            Node newSucc=map.get(aj.targets[i]);
            if (newSucc!=null) aj.targets[i]=(Header)newSucc;
        }
        return visit((Control)aj);
    }
    
    public Void visit(CFieldInst cfi) {
        if (cfi.field() instanceof JumpTable) {
            JumpTable jt=(JumpTable)cfi.field();
            Header[] oldH=jt.headers();
            Header[] newH=new Header[oldH.length];
            for (int i=0;i<oldH.length;++i) {
                Node newSucc=map.get(oldH[i]);
                if (newSucc==null) {
                    newH[i]=oldH[i];
                } else {
                    newH[i]=(Header)newSucc;
                }
            }
            cfi.field=JumpTable.make(jt.cname(),newH);
        }
        return visit((Instruction)cfi);
    }
    
    public void transform(Code code) {
	for (ExceptionHandler eh : code.handlers()) {
	    eh.accept(this);
	}
	for (Header h : code.headers()) {
	    h.accept(this);
	    h.getFooter().accept(this);
	}
    }
}

