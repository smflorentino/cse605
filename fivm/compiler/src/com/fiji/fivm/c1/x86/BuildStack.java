/*
 * BuildStack.java
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
import com.fiji.fivm.*;
import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

public class BuildStack extends LCodePhase {
    static final int stackAlignment;
    
    static {
        if (Global.pointerSize==4) {
            stackAlignment=16;
        } else {
            stackAlignment=16; // ??
        }
    }
    
    public BuildStack(LCode c) { super(c); }
    
    HashSet< ScratchSlot > rejects;
    int stackSize;
    
    static class ScratchNode extends GraphColoring.Node {
        ScratchSlot ss;
        
        ScratchNode(ScratchSlot ss) {
            this.ss=ss;
        }
    }
        
    class ScratchAlloc {
        int size;
        HashMap< ScratchSlot, ScratchNode > nodes=new HashMap< ScratchSlot, ScratchNode >();
        int numSlots;
        
        ScratchAlloc(int size) {
            this.size=size;
        }
        
        ScratchNode forSS(ScratchSlot ss) {
            ScratchNode node=nodes.get(ss);
            if (node==null) {
                nodes.put(ss,node=new ScratchNode(ss));
            }
            return node;
        }
        
        void currentlyLive(Iterable< Object > slots) {
            HashSet< ScratchSlot > seen=new HashSet< ScratchSlot >();
            ArrayList< ScratchNode > list=new ArrayList< ScratchNode >();
            for (Object something : slots) {
                if (something instanceof ScratchSlotByte) {
                    ScratchSlotByte ssb=(ScratchSlotByte)something;
                    if (seen.add(ssb.slot())) {
                        if (ssb.slot().size()==size && !rejects.contains(ssb.slot())) {
                            list.add(forSS(ssb.slot()));
                        }
                    }
                }
            }
            GraphColoring.cluster(list);
        }
        
        void commit() {
            numSlots=GraphColoring.color(nodes.values())+1;
        }
        
        int color(ScratchSlot ss) {
            return forSS(ss).color();
        }
        
        void force(ScratchSlot ss) {
            forSS(ss).forceColor(numSlots++);
        }
    }
    
    LArg stackMem(int offset) {
        if (Settings.OMIT_FRAME_POINTER || (offset<-128 && offset+stackSize<=127)) {
            return new OffMem(offset+stackSize,Reg.SP);
        } else {
            return new OffMem(offset,Reg.BP);
        }
    }
    
    public void visitCode() {
        // this does graph coloring of scratch slots (at least those that aren't
        // subject to Lea) and then lays out the stack.
        
        ScratchLivenessCalc scratchLive=code.getScratchLiveness();
        
        rejects=scratchLive.escapes();
        
        HashMap< Integer, ScratchAlloc > forSize=new HashMap< Integer, ScratchAlloc >();
        
        for (ScratchSlot ss : code.scratches()) {
            if (!forSize.containsKey(ss.size())) {
                forSize.put(ss.size(),new ScratchAlloc(ss.size()));
            }
        }
        
        for (LHeader h : code.headers()) {
            ScratchLivenessCalc.LocalCalc lc=scratchLive.new LocalCalc(h);
            
            for (ScratchAlloc a : forSize.values()) {
                a.currentlyLive(lc.currentlyLive());
            }
            for (LOp o : h.reverseOperations()) {
                lc.update(o);
                for (ScratchAlloc a : forSize.values()) {
                    a.currentlyLive(lc.currentlyLive());
                }
            }
        }
        
        for (ScratchAlloc sa : forSize.values()) {
            sa.commit();
        }
        
        for (ScratchSlot ss : rejects) {
            forSize.get(ss.size()).force(ss);
        }
        
        int[] sizes=new int[forSize.size()];
        int cnt=0;
        for (Integer size : forSize.keySet()) {
            sizes[cnt++]=size;
        }
        
        Arrays.sort(sizes);
        
        int scratchAreaSize=0;
        HashMap< ScratchSlot, Integer > scratchOffsets=new HashMap< ScratchSlot, Integer >();
        for (int j=sizes.length;j-->0;) {
            int size=sizes[j];
            for (Map.Entry< ScratchSlot, ScratchNode > e : forSize.get(size).nodes.entrySet()) {
                assert e.getValue().hasColor() : e;
                scratchOffsets.put(e.getKey(),scratchAreaSize+e.getValue().color()*size);
            }
            scratchAreaSize+=size*forSize.get(size).numSlots;
        }
        
        // round up to 8
        scratchAreaSize=(scratchAreaSize+7)&~7;
        
        // ok.  need to allocate the stack.  the stack grows down (or left, if that's easier
        // to think about).  esp points at the bottom.  ebp points at the top.  ebp is 8-byte
        // misaligned.
        
        int savesSize=0; // does not count ebp
        int paramsSize=0;
        
        savesSize=((code.persistentsUsed().size()*Global.pointerSize)+7)&~7;
        
        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                for (int i=0;i<o.nrhs();++i) {
                    if (o.rhs(i) instanceof OutParamSlot) {
                        OutParamSlot ops=(OutParamSlot)o.rhs(i);
                        paramsSize=Math.max(
                            paramsSize,
                            ops.offset()+o.typeOf(i).size());
                    }
                }
            }
        }
        
        paramsSize=(paramsSize+7)&~7;
        
        stackSize=savesSize+scratchAreaSize+paramsSize;
        
        boolean leaf=(stackSize==0);
        
        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                if (o.opcode()==LOpCode.Call) {
                    leaf=false;
                }
            }
        }
        
        if (leaf) {
            assert code.persistentsUsed().isEmpty();
        }
        
        if (!leaf) {
            if (Settings.OMIT_FRAME_POINTER) {
                stackSize=((stackSize+4+stackAlignment-1)&~(stackAlignment-1))-4;
            } else {
                // NOTE: the establishing of the frame pointer is done implicitly in LIR.
                // at this point BP=SP and BP has been pushed.
                
                stackSize=((stackSize+8+stackAlignment-1)&~(stackAlignment-1))-8;
            }
        }

        if (!leaf) {
            LHeader root=code.reroot();
            
            root.append(
                new LOp(
                    LOpCode.Sub,LType.ptr(),
                    new LArg[]{
                        Immediate.make(stackSize),
                        Reg.SP
                    }));
            
            int offset=0;
            for (Reg r : code.persistentsUsed()) {
                offset-=Global.pointerSize;
                root.append(
                    new LOp(
                        LOpCode.Mov,LType.ptr(),
                        new LArg[]{
                            r,
                            stackMem(offset)
                        }));
            }
        }
        
        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                try {
                    for (int i=0;i<o.nrhs();++i) {
                        if (o.rhs()[i] instanceof InParamSlot) {
                            if (Settings.OMIT_FRAME_POINTER) {
                                o.rhs()[i]=new OffMem(
                                    stackSize+Global.pointerSize+((InParamSlot)o.rhs()[i]).offset(),
                                    Reg.SP);
                            } else {
                                o.rhs()[i]=new OffMem(
                                    Global.pointerSize*2+((InParamSlot)o.rhs()[i]).offset(),
                                    Reg.BP);
                            }
                        } else if (o.rhs()[i] instanceof OutParamSlot) {
                            assert !leaf;
                            o.rhs()[i]=stackMem(-stackSize+((OutParamSlot)o.rhs()[i]).offset());
                        } else if (o.rhs()[i] instanceof OffScratchSlot) {
                            assert !leaf;
                            OffScratchSlot oss=(OffScratchSlot)o.rhs()[i];
                            o.rhs()[i]=stackMem(
                                -stackSize
                                +paramsSize
                                +scratchOffsets.get(oss.slot())
                                +oss.offset());
                        }
                    }
                } catch (Throwable e) {
                    throw new CompilerException("Failed to convert "+o,e);
                }
            }
            
            if (h.footer().opcode()==LOpCode.Return) {
                int offset=0;
                for (Reg r : code.persistentsUsed()) {
                    assert !leaf;
                    offset-=Global.pointerSize;
                    h.append(
                        new LOp(
                            LOpCode.Mov,LType.ptr(),
                            new LArg[]{
                                stackMem(offset),
                                r
                            }));
                }
                if (Settings.OMIT_FRAME_POINTER && !leaf) {
                    h.append(
                        new LOp(
                            LOpCode.Add,LType.ptr(),
                            new LArg[]{
                                Immediate.make(stackSize),
                                Reg.SP
                            }));
                }
            }
        }

        setChangedCode();
        code.killAllAnalyses();
        
        code.stackBuilt=true;
        
        rejects=null;
    }
}

