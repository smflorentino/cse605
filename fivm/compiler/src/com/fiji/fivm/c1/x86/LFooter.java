/*
 * LFooter.java
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

package com.fiji.fivm.c1.x86;

import com.fiji.fivm.c1.x86.arg.LArg;

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
public final class LFooter extends LOp {
    
    LHeader[] successors;
    
    public LFooter(LOpCode opcode,LType type,LArg[] rhs,LHeader[] successors) {
	super(opcode,type,rhs);
        this.successors=successors;
    }
    
    public boolean terminal() {
        return opcode.terminal();
    }
    
    public LHeader defaultSuccessor() {
        if (opcode.dropsDown()) {
            return successors[0];
        } else {
            return null;
        }
    }
    
    public LHeader conditionalSuccessor() {
        assert successors.length==2;
        assert opcode.reversibleBinaryBranch();
        return successors[1];
    }
    
    public LHeader conditionalSuccessor(boolean reverseBranch) {
        assert successors.length==2;
        assert opcode.reversibleBinaryBranch();
        return successors[reverseBranch?0:1];
    }
    
    public LHeader[] successors() {
        return successors;
    }
    
    public int numSuccessors() {
        return successors.length;
    }
    
    public LHeader successor(int i) {
        return successors[i];
    }
    
    public LHeader[] likelySuccessors() {
        int nLikelies=0;
        for (int i=0;i<successors.length;++i) {
            if (successors[i].likely()) {
                nLikelies++;
            }
        }
        LHeader[] result=new LHeader[nLikelies];
        for (int i=0,j=0;i<successors.length;++i) {
            if (successors[i].likely()) {
                result[j++]=successors[i];
            }
        }
        return result;
    }
    
    public LHeader[] unlikelySuccessors() {
        int nUnlikelies=0;
        for (int i=0;i<successors.length;++i) {
            if (successors[i].unlikely()) {
                nUnlikelies++;
            }
        }
        LHeader[] result=new LHeader[nUnlikelies];
        for (int i=0,j=0;i<successors.length;++i) {
            if (successors[i].unlikely()) {
                result[j++]=successors[i];
            }
        }
        return result;
    }
    
    public LHeader[] conditionalSuccessors() {
        if (successors.length==0 || !opcode.dropsDown()) {
            return successors;
        } else {
            LHeader[] result=new LHeader[successors.length-1];
            System.arraycopy(successors,1,
                             result,0,
                             result.length);
            return result;
        }
    }
    
    public LFooter copy() {
        LFooter result=(LFooter)super.copy();
        if (successors.length!=0) {
            result.successors=new LHeader[successors.length];
            System.arraycopy(successors,0,
                             result.successors,0,
                             successors.length);
        }
        return result;
    }
}


