/*
 * SideCrossCalc.java
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

public class SideCrossCalc {
    VarSet sideSet;
    
    private void cross(Operation o,SimpleLivenessCalc.LocalCalc lc,VarSet set) {
        Var clr=null;
        if (o instanceof Instruction) {
            Instruction i=(Instruction)o;
            if (!set.get(i.lhs())) {
                clr=i.lhs();
            }
        }
        set.addAll(lc.currentlyLive());
        if (clr!=null) {
            set.remove(clr);
        }
    }
    
    public SideCrossCalc(Code c) {
        SimpleLivenessCalc slc=c.getSimpleLiveness();
        SideEffectCalc sec=c.getSideEffects();
        
        sideSet=new VarSet(c);
        
        // set of variables that cross any non-immutable operation (side-effect or
        // access to something that could be side-effected)
        VarSet volSideSet=new VarSet(c);
        
        // set of variables whose result comes from a volatile instruction
        VarSet volSet=new VarSet(c);
        
        for (Header h : c.headers()) {
            SimpleLivenessCalc.LocalCalc lc=slc.new LocalCalc(h);
            
            for (Operation o : h.reverseOperations()) {
                if (!sec.immutable(o)) {
                    cross(o,lc,volSideSet);
                }
                
                if (sec.get(o)) {
                    cross(o,lc,sideSet);
                }
                
                if (o instanceof Instruction && 
                    o instanceof Volatilable) {
                    Var lhs=((Instruction)o).lhs();
                    if (lhs!=Var.VOID &&
                        ((Volatilable)o).volatility().isVolatile()) {
                        volSet.add(lhs);
                    }
                }
                
                lc.update(o);
            }
        }
        
        // in volSet, clear variables whose source operation is non-volatile
        volSideSet.retainAll(volSet);
        
        // now volSideSet contains variables that are volatile and cross an
        // impure operation, while sideSet contains variables that cross
        // side-effects.
        
        // add to sideSet the contents of volSideSet
        sideSet.addAll(volSideSet);
    }
    
    public VarSet sideSet() {
        return sideSet;
    }
    
    public boolean hasSide(Var v) {
        return sideSet.get(v);
    }
    
    public boolean hasSideAny(Var... vs) {
        for (Var v : vs) {
            if (hasSide(v)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasSideAny(Var[] vs,int offset,int length) {
        for (int i=0;i<length;++i) {
            if (hasSide(vs[i+offset])) {
                return true;
            }
        }
        return false;
    }
}


