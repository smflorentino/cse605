/*
 * OptGreyStackCMStoreBarriers.java
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

public class OptGreyStackCMStoreBarriers extends CodePhase {
    public OptGreyStackCMStoreBarriers(Code c) { super(c); }
    
    VarSet allVars;
    
    public void visitCode() {
        assert !Settings.GC_BLACK_STACK;
        assert Settings.CMRGC || Settings.HFGC;
        assert Settings.OPT_CM_STORE_BARRIERS;
        
        // the plan: if we execute a barrier on a value, then we don't need to
        // execute any barriers on that value again.  note, this works best
        // in SSA but it can work in any rep.  currently this is conservative
        // with respect to exception throw.
        
        HashMap< Header, VarSet > needsBarrierAtHead=new HashMap< Header, VarSet >();
        HashMap< Header, VarSet > needsBarrierAtTail=new HashMap< Header, VarSet >();
        
        allVars=new VarSet(code);
        for (Var v : code.vars()) {
            if (v.type().isObject()) {
                allVars.add(v);
            }
        }
        
        for (Header h : code.handlerHeaders()) {
            needsBarrierAtHead.put(h,allVars.copy());
        }

        needsBarrierAtHead.put(code.root(),new VarSet(code));
        
        Worklist w=new Worklist();
        w.push(code.root());
        for (Header h : code.handlerHeaders()) {
            w.push(h);
        }
        
        while (!w.empty()) {
            Header h=w.pop();
            
            VarSet needsBarrier=needsBarrierAtHead.get(h).copy();
            
            for (Instruction i : h.instructions()) {
                propagate(i,needsBarrier);
            }
            
            if (!needsBarrierAtTail.containsKey(h) ||
                !needsBarrier.equals(needsBarrierAtTail.get(h))) {
                needsBarrierAtTail.put(h,needsBarrier);
                for (Header h2 : h.normalSuccessors()) {
                    VarSet otherNeedsBarrier=needsBarrierAtHead.get(h2);
                    if (otherNeedsBarrier==null) {
                        needsBarrierAtHead.put(h2,needsBarrier.copy());
                        w.push(h2);
                    } else if (otherNeedsBarrier.addAll(needsBarrier)) {
                        w.push(h2);
                    }
                }
            }
        }
        
        // do the transformation
        
        for (Header h : code.headers()) {
            VarSet needsBarrier=needsBarrierAtHead.get(h);
            assert needsBarrier!=null;
            assert needsBarrierAtTail.get(h)!=null;
            for (Instruction i : h.instructions()) {
                if (i instanceof HeapAccessInst) {
                    HeapAccessInst hai=(HeapAccessInst)i;
                    if (hai.mode().hasCMStoreBarrier() &&
                        !needsBarrier.get(StoreSourceCalc.get(hai))) {
                        hai.setMode(hai.mode().withoutCMStoreBarrier());
                        if (Settings.CHECK_OPT_CM_STORE_BARRIERS) {
                            hai.prepend(
                                new MethodInst(
                                    hai.di(),OpCode.InvokeStatic,
                                    Var.VOID,new Arg[]{
                                        StoreSourceCalc.get(hai)
                                    },
                                    Runtime.assertMarked));
                        }
                        if (Global.verbosity>=2) {
                            Global.log.println("eliding barrier in "+hai.di().shortName()+", in "+code.shortName()+" on "+StoreSourceCalc.get(hai));
                        }
                        setChangedCode("removed CM store barrier");
                    }
                }
                propagate(i,needsBarrier);
            }
        }
        
        if (changedCode()) code.killIntraBlockAnalyses();
        
        allVars=null;
    }
    
    void propagate(Instruction i,VarSet needsBarrier) {
        if (i instanceof HeapAccessInst) {
            HeapAccessInst hai=(HeapAccessInst)i;
            if (hai.mode().hasCMStoreBarrier()) {
                // NB this may be buggy.  is it possible that marking this variable
                // as no longer needing a barrier, while also potentially removing
                // this barrier, creates a bug?
                needsBarrier.remove(StoreSourceCalc.get(hai));
                if (Global.verbosity>=5) {
                    Global.log.println("don't need barrier after "+hai.di().shortName()+", in "+code.shortName()+" on "+StoreSourceCalc.get(hai)+" because of "+hai);
                }
            }
        }
        if (code.getSafepoints().getOnNormalReturn(i) &&
            !Settings.AGGRESSIVE_OPT_CM_STORE_BARRIERS) {
            // FIXME: what about variables that contain references to strings and
            // types and such?
            
            // hold on ... do we need this at all?  if the safepoint isn't taken then
            // it doesn't matter.  and if it is taken, then it's possible that we need
            // to remark stuff because of the first soft handshake in the collector.
            // but: what if the soft handshake also requested that roots get scanned?
            needsBarrier.addAll(allVars);
            if (Global.verbosity>=5) {
                Global.log.println("need barriers on all after "+i.di().shortName()+", in "+code.shortName());
            }
        }
        if (i.lhs().type().isObject()) {
            switch (i.opcode()) {
            case Mov:
            case Phi:
            case Ipsilon:
            case Cast:
            case TypeCheck:
            case CastExact:
            case CastNonZero:
                if (i.rhs(0) instanceof Var && i.rhs(0).type().isObject()) {
                    if (Global.verbosity>=5) {
                        Global.log.println("moving "+i.rhs(0)+" = "+needsBarrier.get(i.rhs(0))+" into "+i.lhs()+" in "+i.di().shortName()+", in "+code.shortName());
                    }
                    needsBarrier.set(i.lhs(),needsBarrier.get(i.rhs(0)));
                } else if (i.rhs(0)==Arg.NULL) {
                    if (Global.verbosity>=5) {
                        Global.log.println("don't need barrier after "+i.di().shortName()+", in "+code.shortName()+" on "+i.lhs()+" (assign from null)");
                    }
                    needsBarrier.remove(i.lhs());
                } else {
                    if (Global.verbosity>=5) {
                        Global.log.println("need barrier after "+i.di().shortName()+", in "+code.shortName()+" on "+i.lhs()+" (assign from "+i.rhs(0)+")");
                    }
                    needsBarrier.add(i.lhs());
                }
                break;
            case New:
            case NewArray:
                // why is this necessary?  it's because of sneakiness.  when the
                // collector is switching the world to allocate black, it's possible
                // to allocate a white object and immediately store it into a black
                // one.  that's because we may have objects allocated black by other
                // threads, which we have gotten access to.  nasty.
                
                // there's also an even more interesting scenario that only happens
                // in the HFGC: the allocator may allocate the object sentinel and
                // then hit one or more safepoints while allocating the remaining
                // chunks.  in that case, the returned object may be white even after
                // all of the threads have been switched to allocating black.
                
                if (Global.verbosity>=5) {
                    Global.log.println("need barrier after "+i.di().shortName()+", in "+code.shortName()+" on "+i.lhs()+" (allocation)");
                }

                needsBarrier.add(i.lhs());
                break;
            case GetString:
            case GetType:
                if (Global.verbosity>=5) {
                    Global.log.println("don't need barrier after "+i.di().shortName()+", in "+code.shortName()+" on "+i.lhs()+" (string/type)");
                }

                needsBarrier.remove(i.lhs());
                break;
            default:
                if (Global.verbosity>=5) {
                    Global.log.println("need barrier after "+i.di().shortName()+", in "+code.shortName()+" on "+i.lhs()+" (misc)");
                }
                needsBarrier.add(i.lhs());
                break;
            }
        }
    }
}

