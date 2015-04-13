/*
 * LocalLinearScanRA.java
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

import java.util.*;

import com.fiji.util.*;
import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

/**
 * A truly awful register allocator that will result in horrid performance.  But
 * it uses a simple and easy to grok strategy, which should make debugging the
 * rest of the compiler easy.  Use this allocator only when you suspect that the
 * other register allocator is broken (or because the other register allocator
 * hasn't been implemented yet).
 * <p>
 * The principle of this allocator is that any temporary that is live across
 * basic block boundaries gets spilled.  All the rest get allocated using a linear
 * scan strategy internal to basic blocks.  So a temporary that has separate live
 * ranges in separate blocks may get separate registers.  This pass also does
 * spilling, in an iterative style where spilling causes allocation to be repeated.
 * Hence after this pass runs, there should not be any temporaries in the code
 * anymore.
 */
public class LocalLinearScanRA extends LCodePhase {
    public LocalLinearScanRA(LCode c) { super(c); }
    
    LVarSet spills;
    HashMap< LHeader, HashMap< Tmp, TmpInfo > > allocations;
    
    static class TmpInfo {
        int begin=-1;
        int end=-1;
        int reg=-1;
        boolean spilled;
    }
    
    boolean doBirth(Reg[] regs,
                    HashMap< Tmp, TmpInfo > infos,
                    LinkedList< Tmp > deque,
                    IntBag freeRegs,
                    Tmp t) {
        boolean spilled=false;
        
        assert !infos.get(t).spilled;
        assert deque.size()<=regs.length;
        assert deque.size()+freeRegs.size()==regs.length;
        
        if (deque.size()==regs.length) {
            Tmp toSpill=deque.removeFirst();
            TmpInfo spillInfo=infos.get(toSpill);
            
            spills.add(toSpill);
            spillInfo.spilled=true;
            assert spillInfo.reg>=0;
            freeRegs.push(spillInfo.reg);
            spilled=true;
        }
        
        deque.addLast(t);
        infos.get(t).reg=freeRegs.pop();
        
        return spilled;
    }
    
    void doDeath(Reg[] regs,
                 HashMap< Tmp, TmpInfo > infos,
                 LinkedList< Tmp > deque,
                 IntBag freeRegs,
                 Tmp t) {
        if (infos.get(t).spilled) {
            return;
        }
        
        assert deque.size()<=regs.length;
        assert deque.size()+freeRegs.size()==regs.length;
        
        boolean found=false;
        
        for (Iterator< Tmp > i=deque.iterator();
             i.hasNext();) {
            if (i.next()==t) {
                i.remove();
                found=true;
                break;
            }
        }
        
        assert found;
        
        freeRegs.push(infos.get(t).reg);
    }

    boolean allocForBlock(Reg[] regs,Kind kind,LHeader h) {
        boolean spilled=false;
        
        HashMap< Tmp, TmpInfo > infos=new HashMap< Tmp, TmpInfo >();
        
        // determine live ranges
        for (LOp o : h.operations()) {
            for (LArg a : o.defs()) {
                if (a instanceof Tmp && a.kind()==kind) {
                    Tmp t=(Tmp)a;
                    TmpInfo info=infos.get(t);
                    if (info==null) {
                        info=new TmpInfo();
                        info.end=info.begin=o.order();
                        infos.put(t,info);
                    } else {
                        info.end=o.order();
                    }
                }
            }
            
            for (LArg a : o.uses()) {
                if (a instanceof Tmp && a.kind()==kind) {
                    TmpInfo info=infos.get((Tmp)a);
                    assert info!=null : a;
                    info.end=o.order();
                }
            }
        }
        
        // perform allocation
        LinkedList< Tmp > deque=new LinkedList< Tmp >();
        IntBag freeRegs=new IntBag();
        for (int i=0;i<regs.length;++i) {
            freeRegs.add(i);
        }
        
        for (LOp o : h.operations()) {
            HashSet< Tmp > deaths=new HashSet< Tmp >();
            
            for (LArg a : o.defs()) {
                if (a instanceof Tmp && a.kind()==kind) {
                    Tmp t=(Tmp)a;
                    TmpInfo info=infos.get(t);
                    if (info.begin==o.order()) {
                        spilled|=doBirth(regs,infos,deque,freeRegs,t);
                    }
                    if (info.end==o.order()) {
                        deaths.add(t);
                    }
                }
            }
            
            for (LArg a : o.uses()) {
                if (a instanceof Tmp && a.kind()==kind) {
                    Tmp t=(Tmp)a;
                    TmpInfo info=infos.get(t);
                    if (info.end==o.order()) {
                        deaths.add(t);
                    }
                }
            }
            
            for (Tmp t : deaths) {
                doDeath(regs,infos,deque,freeRegs,t);
            }
        }
        
        if (spilled) {
            return false;
        } else {
            allocations.put(h,infos);
            return true;
        }
    }
    
    void addSpills(LVarSet spills,
                   LVarSet live,
                   Kind kind) {
        for (LArg a : live) {
            if (a instanceof Tmp && a.kind()==kind) {
                if (Global.verbosity>=5) {
                    Global.log.println("Spilling "+a);
                }
                spills.add(a);
            }
        }
    }
    
    void allocForKind(Kind kind) {
        Reg[] regs;
        
        if (kind.isInt()) {
            regs=Reg.usableIntPersistents;
        } else {
            assert kind.isFloat();
            regs=Reg.usableFloatVolatiles;
        }
        
        spills=new LVarSet(code);
        allocations=new HashMap< LHeader, HashMap< Tmp, TmpInfo > >();
        
        LLivenessCalc liveness=code.getLiveness();
        
        for (LHeader h : code.headers()) {
            addSpills(spills,liveness.liveAtHead(h),kind);
            if (kind.isFloat()) {
                LLivenessCalc.LocalCalc lc=liveness.new LocalCalc(h);
                for (LOp o : h.reverseOperations()) {
                    if (o.opcode()==LOpCode.Call) {
                        addSpills(spills,lc.currentlyLive(),kind);
                    }
                    lc.update(o);
                }
            }
        }
        
        for (;;) {
            if (!spills.isEmpty()) {
                new Spill(code,spills).doit();
                spills.clear();
            }

            code.recomputeOrder();
            
            boolean succeeded=true;
            for (LHeader h : code.headers()) {
                succeeded&=allocForBlock(regs,kind,h);
            }
            
            if (succeeded) {
                assert spills.isEmpty();
                break;
            }
            
            assert !spills.isEmpty();
        }
        
        for (LHeader h : code.headers()) {
            HashMap< LArg, LArg > map=new HashMap< LArg, LArg >();
            HashMap< Tmp, TmpInfo > infos=allocations.get(h);
            for (Map.Entry< Tmp, TmpInfo > e : infos.entrySet()) {
                assert !e.getValue().spilled;
                
                Reg r=regs[e.getValue().reg];
                map.put(e.getKey(),r);
                if (r.isPersistent()) {
                    code.usePersistent(r);
                }
            }
        
            for (LOp o : h.operations()) {
                o.mapRhs(map);
            }
        }
        
        spills=null;
        allocations=null;
    }
    
    public void visitCode() {
        allocForKind(Kind.INT);
        allocForKind(Kind.FLOAT);
        
        code.delAllTmps();
        code.registersAllocated=true;
        
        setChangedCode();
        code.killIntraBlockAnalyses();
    }
}

