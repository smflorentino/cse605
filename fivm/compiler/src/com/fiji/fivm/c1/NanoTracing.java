/*
 * NanoTracing.java
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

public class NanoTracing extends CodePhase {
    public NanoTracing(Code c) { super(c); }
    
    public void visitCode() {
        assert !code.isSSA();
        
        // figure out which blocks are "small".  this definition is tweaked
        // to also mean that the blocks don't throw.  if we find a small
        // block, we save its operations.
        
        SimpleLivenessCalc slc=code.getSimpleLiveness();
        
        final int compareLimit=1;
        final int costLimit=0; // tune this?
        
        HashMap< Header, ArrayList< Operation > > small=
            new HashMap< Header, ArrayList< Operation > >();
        for (Header h : code.headers()) {
            int numCompares=0;
            int cost=0;
            boolean badOp=false;
            for (Operation o : h.operations()) {
                switch (o.opcode()) {
                case Eq:
                case Neq:
                case LessThan:
                case ULessThan:
                case LessThanEq:
                case ULessThanEq:
                    numCompares++;
                    break;
                case Add:
                case Sub:
                case Mul:
                case Neg:
                case Shl:
                case Shr:
                case Ushr:
                case Or:
                case And:
                case Xor:
                case Sqrt:
                case NullCheck:
                case PutField:
                case GetField:
                case ArrayStore:
                case ArrayLoad:
                case Store:
                case Load:
                case PutCVar:
                case GetCVar:
                case PutCField:
                case GetCField:
                case Cast:
                case GetCArg:
                case SaveRef:
                case SaveDebugID:
                case GetCVarAddress:
                case GetCArgAddress:
                case LikelyZero:
                case LikelyNonZero:
                case SemanticallyLikelyZero:
                case SemanticallyLikelyNonZero:
                case HardUse:
                    cost++;
                    break;
                case ArrayBoundsCheck:
                    cost+=2;
                    break;
                case Mov:
                case Return:
                case Jump:
                case BranchNonZero:
                case BranchZero:
                case NotReached:
                case PollCheck:
                    break;
                default:
                    // this includes patch points.
                    badOp=true;
                    break;
                }
                if (numCompares>compareLimit || badOp || cost>costLimit) break;
            }
            if (!badOp &&
                cost<=costLimit &&
                (numCompares==0 ||
                 (numCompares==1 &&
                  (h.getFooter().opcode()==OpCode.BranchNonZero ||
                   h.getFooter().opcode()==OpCode.BranchZero)))) {
                if (Global.verbosity>=4) {
                    Global.log.println("Labeling "+h+" as small.");
                }
                ArrayList< Operation > ops=new ArrayList< Operation >();
                for (Operation o : h.operations()) {
                    ops.add(o.copyAndMultiAssign());
                }
                small.put(h,ops);
            }
        }
        
        ThrowingHeadersCalc thc=code.getThrowingHeaders();
	    
        // find jumps to small blocks.  a jump to a small block is replaced
        // by the body of that block.
        // FIXME: if we branch to a small block that branches on something we
        // know something about, then do some merging.  for example, one of our
        // successors could be branching on a predicate that we branched on,
        // but the CFA can't do anything about it because our successor has
        // a different predecessor that doesn't branch on the same predicate.
        for (Header h : code.headers()) {
            if (h.getFooter().opcode()==OpCode.Jump &&
                small.containsKey(h.getFooter().defaultSuccessor()) &&
                h.getFooter().defaultSuccessor()!=h &&
                h.getFooter().defaultSuccessor().getFooter().defaultSuccessor()
                != h.getFooter().defaultSuccessor() &&
                (h.handler()==h.defaultSuccessor().handler() ||
                 thc.doesNotThrow(h.defaultSuccessor()))) {
                if (Global.verbosity>=4) {
                    Global.log.println("Appending "+h.getFooter().next+" to "+
                                       h);
                }
                
                Header h2=h.defaultSuccessor();
                
                ArrayList< Operation > ops=small.get(h2);
                VarSet rejects=slc.liveAtTail(h2).copy();
                rejects.addAll(slc.liveForHandler(h2));
                rejects.addAll(slc.liveForHandler(h));
                HashMap< Var, Var > varMap=new HashMap< Var, Var >();
                
                for (Operation o : ops) {
                    if (o instanceof Instruction) {
                        Instruction i=(Instruction)o;
                        Var v=i.lhs();
                        if (v!=Var.VOID && !rejects.contains(v)) {
                            varMap.put(v,code.addVar(v.type()));
                        }
                    }
                }
                
                for (Operation o : ops) {
                    Operation o2=o.copy();
                    o2.replaceVars(varMap);
                    if (o instanceof Instruction) {
                        h.append((Instruction)o2);
                    } else {
                        h.setFooter((Footer)o2);
                    }
                }
                
                setChangedCode("jammed small block");
            }
        }
        
        if (changedCode()) {
            code.killAllAnalyses();
        }

        // FIXME: we should have an optimization where branches to small blocks
        // that end in a jump get replaced by branches to the successor, provided
        // that the contents of the block can be merged without messing anything
        // up.  example:
        //
        // if (foo) {
        //    x=5;
        // } else {
        //    x=y*100;
        // }
        // use x
        //
        // perhaps it might be better if this was:
        //
        // x=5;
        // if (!foo) {
        //    x=y*100;
        // }
        // use x
	    
    }
}

