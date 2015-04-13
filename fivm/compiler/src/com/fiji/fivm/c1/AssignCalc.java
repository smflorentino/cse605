/*
 * AssignCalc.java
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
import com.fiji.fivm.*;

// FIXME: this is BS.  Vars should just point at their Instructions, and
// Instructions should point at their Headers.  This should be eagerly maintained.
// The problem: what if an instruction is killed along but the Var isn't?
public class AssignCalc {
    CompactArrayList< Instruction >[] assignInsts;
    CompactArrayList< Header >[] assignHeads;
    HashSet< Instruction > assigns=
        new HashSet< Instruction >();

    @SuppressWarnings("unchecked")
    AssignCalc(Code c) {
        assignInsts=new CompactArrayList[c.vars().size()];
        assignHeads=new CompactArrayList[c.vars().size()];
	for (Header h : c.headers()) {
	    for (Instruction i : h.instructions()) {
                Var v=i.lhs();
		if (v!=Var.VOID) {
                    int id=v.id;
                    if (assignInsts[id]==null) {
                        assignInsts[id]=new CompactArrayList< Instruction >();
                    }
                    assignInsts[id].add(i);
                    if (assignHeads[id]==null) {
                        assignHeads[id]=new CompactArrayList< Header >();
                    }
                    assignHeads[id].add(h);
                    assigns.add(i);
		}
	    }
	}
    }
    
    HashSet< Instruction > assigns() {
	return assigns;
    }
    
    CompactArrayList< Instruction > assignsFor(Var v) {
        CompactArrayList< Instruction > result = assignInsts[v.id];
        if (result==null) {
            return Instruction.EMPTY_CAL;
        } else {
            return result;
        }
    }
    
    // note this never returns null ... but it may return the empty set.
    CompactArrayList< Instruction > assignsFor(Arg v) {
        if (v instanceof Var) {
            return assignsFor((Var)v);
        } else {
            return Instruction.EMPTY_CAL;
        }
    }
    
    // only call this in SSA
    Instruction assignFor(Arg v) {
	Instruction result=null;
	for (Instruction i : assignsFor(v)) {
	    if (i.opcode()!=OpCode.Ipsilon) {
		assert result==null;
		result=i;
	    }
	}
        if (v instanceof Var) {
            assert ((Var)v).inst()==result;
        }
	return result;
    }
    
    // only call this in SSA
    Arg uniqueSource(Arg v) {
	if (!(v instanceof Var)) {
	    return v;
	}
	CompactArrayList< Instruction > srcs=assignsFor(v);
	if (srcs.size()==1) {
	    Instruction i=srcs.get(0);
	    if (i.opcode()==OpCode.Mov) {
		return i.rhs(0);
	    }
	} else if (srcs.size()>1) {
            Arg result=null;
	    for (Instruction i : srcs) {
		if (i.opcode()==OpCode.Ipsilon) {
                    if (result==null) {
                        result=i.rhs(0);
                    } else if (i.rhs(0)!=result) {
                        return null;
                    }
		} else {
		    assert i.opcode()==OpCode.Phi : "for "+i+", with srcs = "+srcs;
		}
	    }
            return result;
	}
	return null;
    }
    
    CompactArrayList< Header > assignBlocksFor(Var v) {
        CompactArrayList< Header > result=assignHeads[v.id];
        if (result==null) {
            return Header.EMPTY_CAL;
        } else {
            return result;
        }
    }
    
    CompactArrayList< Header > assignBlocksFor(Arg v) {
	if (v instanceof Var) {
            return assignBlocksFor((Var)v);
        } else {
            return Header.EMPTY_CAL;
        }
    }
}

