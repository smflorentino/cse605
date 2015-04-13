/*
 * SimpleGVN.java
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
 * Global value numbering.
 */
public class SimpleGVN extends CodePhase {
    public SimpleGVN(Code c) { super(c); }
    
    NormalDominatorCalc dc;
    HeaderProximityCalc hpc;
    
    // order = i.order
    // code = the "number" in "global value numbering"
    // code -> order mapping is 1 -> many
    
    Instruction[] instByOrder;
    int[] codeByOrder;
    int[][] ordersByCode;
    
    public void visitCode() {
        dc=code.getNormalDominators();
        if (false) hpc=code.getHeaderProximity();
        
        computeNumbering();
        
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                Instruction repl=bestReplacement(i);
                if (repl!=null) {
                    i.prepend(
                        new SimpleInst(
                            i.di(),OpCode.Mov,
                            i.lhs(),new Arg[]{ repl.lhs() }));
                    i.remove();
                    if (false) Global.log.println("RCE: "+i.opcode()+" "+i.di().shortName());
                    setChangedCode("replaced an "+i.opcode());
                }
            }
        }
        
        code.recomputeOrder();
        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
        
        dc=null;
        instByOrder=null;
        codeByOrder=null;
        ordersByCode=null;
    }
    
    Instruction instByOrder(int order) {
        return instByOrder[order];
    }
    
    int codeByOrder(int order) {
        return codeByOrder[order];
    }
    
    int codeByInst(Instruction i) {
        return codeByOrder(i.order);
    }
    
    static int distance(Operation a,
                        Operation b) {
        return Math.abs(a.order-b.order);
    }
    
    Instruction bestReplacement(Instruction orig) {
        int code=codeByInst(orig);
        if (code>=0) {
            Instruction bestRepl=null;
            int bestDistance=Integer.MAX_VALUE;
            
            int[] orders=ordersByCode[code];
            
            if (orders!=null) {
                for (int order : orders) {
                    Instruction repl=instByOrder(order);
                    if (dc.dominates(repl,orig)) {
                        int dist=distance(orig,repl);
                        if (dist<bestDistance) {
                            bestRepl=repl;
                            bestDistance=dist;
                        }
                    }
                }
            }
            
            if (bestDistance<Global.rceDistance &&
                (true || hpc.proximityIsFinite(bestRepl.head(),orig.head()))) {
                return bestRepl;
            } else {
                return null;
            }
        } else {
            // not an immutable instruction
            return null;
        }
    }
    
    void computeNumbering() {
        int cnt=code.computeTotalOrder()+1;
        
        instByOrder=new Instruction[cnt];
        codeByOrder=new int[cnt];
        ordersByCode=new int[cnt][];
        
        int nextCode=1;
        
        HashMap< NumberHash, Integer > numbers=new HashMap< NumberHash, Integer >();
        
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                if (code.getSideEffects().immutable(i)) {
                    NumberHash nh=new NumberHash(i);
                    Integer codeObj=numbers.get(nh);
                    int code;
                    if (codeObj==null) {
                        code=nextCode++;
                        numbers.put(nh,code);
                    } else {
                        code=codeObj;
                    }
                    assignCode(i,code);
                } else {
                    instByOrder[i.order]=i;
                    codeByOrder[i.order]=-1;
                }
            }
        }
    }
        
    void assignCode(Instruction i,int code) {
        instByOrder[i.order]=i;
        codeByOrder[i.order]=code;
        if (ordersByCode[code]==null) {
            ordersByCode[code]=new int[]{i.order};
        } else {
            int[] oldOrders=ordersByCode[code];
            int[] newOrders=new int[oldOrders.length+1];
            System.arraycopy(oldOrders,0,
                             newOrders,0,
                             oldOrders.length);
            newOrders[oldOrders.length]=i.order;
            ordersByCode[code]=newOrders;
        }
    }
    
    // used for value numbering
    static class NumberHash {
        Instruction inst;
        int hashCode;
        
        NumberHash(Instruction inst) {
            this.inst=inst;
            this.hashCode=OpHashCode.getNoLhs(inst);
        }

        public int hashCode() {
            return hashCode;
        }
        
        public boolean equals(Object other_) {
            if (!(other_ instanceof NumberHash)) return false;
            NumberHash other=(NumberHash)other_;
            return inst.opcode()==other.inst.opcode()
                && OpEquals.getNoLhs(inst,other.inst);
        }
    }
}

