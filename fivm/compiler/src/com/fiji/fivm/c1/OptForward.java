/*
 * OptForward.java
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

/** Undo some of what OptRCE did. */
public class OptForward extends CodePhase {
    public OptForward(Code c) { super(c); }
    
    public void visitCode() {
        assert code.isSSA();
        
        SimpleLivenessCalc slc=code.getSimpleLiveness();
        
        int numVars=code.vars().size();
        
        code.recomputeOrder();
        
        // strategy:
        // - if an operation uses a value that is computed using an instruction
        //   whose cost is less than infinity and the sources of that instruction
        //   are still live, then:
        // - if the source instruction is in a different block or is separated
        //   by a call, then forward it
        // - if the source instruction is in the same block then forward it
        //   only if the distance (according to o.order) is greater than the
        
        // NOTE: this won't unravel the nastiness of access-at-stack-offset
        // type operations.  for that we need either an operation or an arg
        // that corresponds to stack addressing.
        
        // NOTE #2: this won't handle the following case:
        // c = Add(a, b)
        // e = Add(c, d)
        // use(e)
        // ... bunch of stuff ...
        // use(e) // a, b, d still live but c dead, i.e. potentially profitable
        //        // to recompute .... maybe, ish.
        
        ArrayList< DeferredRhsUpdate > deferredUpdates=
            new ArrayList< DeferredRhsUpdate >();
            
        for (Header h : code.headers()) {
            int lastCall=0;
            SimpleLivenessCalc.ForwardLocalCalc flc=
                slc.new ForwardLocalCalc(h);
            for (Operation o : h.operations()) {
                for (int i=0;i<o.rhs.length;++i) {
                    Arg a=o.rhs[i];
                    if (a instanceof Var) {
                        Var v=(Var)a;
                        Instruction src=v.inst();
                        int cost=cost(src);
                        
                        // FIXME: could be more conservative about this.  might be
                        // good for compiler performance.
                        // FIXME!!! this adds 30% to my compile time.  balls.
                        // FIXME!!! this is actually totally dumb ... if the variable
                        // is one we added, it's guaranteed not to pass the liveness
                        // test.
                        for (Arg a2 : src.rhs()) {
                            if (a2 instanceof Var &&
                                ((Var)a2).id() >= numVars) {
                                // need to recompute analyses.
                                code.killIntraBlockAnalyses();
                                slc=code.getSimpleLiveness();
                                flc=slc.new ForwardLocalCalc(h);
                                for (Operation o2 : h.operations()) {
                                    if (o2==o) {
                                        break;
                                    }
                                    flc.update(o2);
                                }
                                numVars=code.vars().size();
                            }
                        }
                        
                        if (forwardable(cost) &&
                            flc.currentlyLive(src.rhs())) {
                            Header hSrc=src.head();
                            
                            // FIXME: employ total instruction ordering as an
                            // estimate of distance.
                            
                            if (hSrc==h) {
                                assert o.order-src.order >= 1
                                    : "for o = "+o+", src = "+src;
                            }
                            if (hSrc!=h ||
                                src.order<lastCall ||
                                (o.order-src.order)>cost) {
                                Instruction copy=(Instruction)src.copy();
                                copy.setLhs(code.addVar(src.lhs().type()));
                                o.prepend(copy);
                                deferredUpdates.add(
                                    new DeferredRhsUpdate(
                                        o.rhs,
                                        i,
                                        copy.lhs()));
                                setChangedCode("forwarded a "+src.opcode());
                            }
                        }
                    }
                }
                if (call(o)) {
                    lastCall=o.order;
                }
                
                flc.update(o);
                
                if (!deferredUpdates.isEmpty()) {
                    for (DeferredRhsUpdate dru : deferredUpdates) {
                        dru.run();
                    }
                    deferredUpdates.clear();
                }
            }
        }
        
        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
    }
    
    static class DeferredRhsUpdate {
        Arg[] rhs;
        int index;
        Var newVar;
        
        DeferredRhsUpdate(Arg[] rhs,
                          int index,
                          Var newVar) {
            this.rhs=rhs;
            this.index=index;
            this.newVar=newVar;
        }
        
        void run() {
            rhs[index]=newVar;
        }
    }
    
    static final int MAX_COST=10000;
    
    static boolean forwardable(int cost) {
        return cost<MAX_COST;
    }
    
    static int cost(Instruction i) {
        switch (i.opcode()) {
        case GetCTypeSize:
        case GetCVarAddress:
        case GetCFieldOffset:
            return 1;
        case Add:
        case GetCFieldAddress:
            // this heuristic is very Intel-specific, where add operations
            // are super cheap.
            return 5;
        case IntToPointerZeroFill:
        case FirstHalf:
        case SecondHalf:
        case LessThan:
        case ULessThan:
        case LessThanEq:
        case ULessThanEq:
        case Eq:
        case Neq:
        case Not:
        case Boolify:
            return 8;
        case BitNot:
        case Sub:
        case Neg:
        case Shl:
        case Shr:
        case Ushr:
        case And:
        case Or:
        case Xor:
            return 13;
        case Cast: {
            TypeInst ti=(TypeInst)i;
            if (ti.getType().effectiveBasetype().isFloat
                == ti.rhs(0).type().effectiveBasetype().isFloat) {
                return 8;
            } else {
                return MAX_COST;
            }
        }
        default:
            return MAX_COST;
        }
    }
    
    // tells us if the given op requires spilling registers
    static boolean call(Operation i) {
        switch (i.opcode()) {
        case Call:
        case CallIndirect:
        case StrongLoadCAS:
        case StrongCAS:
        case StrongVoidCAS:
        case WeakCAS:
            return true;
        default:
            return false;
        }
    }
}


