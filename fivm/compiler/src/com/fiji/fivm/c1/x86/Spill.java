/*
 * Peephole.java
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

import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

public class Spill extends LCodePhase {
    HashSet< Tmp > spillTmps;
    
    public Spill(LCode c,HashSet< Tmp > spills) {
        super(c);
        this.spillTmps=spills;
    }
    
    public Spill(LCode c,LVarSet spills) {
        this(c,spills.toTmpSet());
    }
    
    public Spill(LCode c,LArg[] args) {
        this(c,new LVarSet(c,args));
    }
    
    private void upSize(HashMap< Tmp, Integer > varSizes,
                        Tmp t,
                        int size) {
        Integer oldSize=varSizes.get(t);
        if (oldSize==null) {
            varSizes.put(t,size);
        } else if (oldSize>=size) {
            // do nothing
        } else {
            varSizes.put(t,size);
        }
    }
    
    public void visitCode() {
        HashMap< Tmp, Integer > varSizes=new HashMap< Tmp, Integer >();
        
        HashMap< InParamSlot, Tmp > paramTmps=new HashMap< InParamSlot, Tmp >();
        HashSet< InParamSlot > weirdParams=new HashSet< InParamSlot >();
        
        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                if (o.opcode()==LOpCode.Mov &&
                    o.rhs(0) instanceof InParamSlot &&
                    o.rhs(1) instanceof Tmp &&
                    !weirdParams.contains(o.rhs(0))) {
                    if (paramTmps.containsKey(o.rhs(0))) {
                        paramTmps.remove(o.rhs(0));
                        weirdParams.add((InParamSlot)o.rhs(0));
                    } else {
                        paramTmps.put((InParamSlot)o.rhs(0),(Tmp)o.rhs(1));
                    }
                } else {
                    for (LArg a : o.rhs()) {
                        if (a instanceof InParamSlot) {
                            weirdParams.add((InParamSlot)a);
                            paramTmps.remove((InParamSlot)a);
                        }
                    }
                }
                
                for (int i=0;i<o.nrhs();++i) {
                    LArg a=o.rhs(i);
                    if (a instanceof Tmp) {
                        Tmp t=(Tmp)a;
                        if (spillTmps.contains(t)) {
                            upSize(varSizes,t,o.typeOf(i).size());
                        }
                    } else {
                        for (int j=0;j<a.nUseOnUseVars();++j) {
                            LArg a2=a.useOnUseVar(j);
                            if (spillTmps.contains(a2)) {
                                upSize(varSizes,(Tmp)a2,o.memType().size());
                            }
                        }
                    }
                }
            }
        }
        
        HashMap< Tmp, LArg > spills=new HashMap< Tmp, LArg >();
        
        for (Map.Entry< InParamSlot, Tmp > e : paramTmps.entrySet()) {
            if (spillTmps.contains(e.getValue())) {
                spills.put(e.getValue(),e.getKey());
            }
        }
        
        for (Tmp t : spillTmps) {
            Integer size=varSizes.get(t);
            if (size!=null) {
                if (!spills.containsKey(t)) {
                    spills.put(t,new OffScratchSlot(code.addScratch(size),0));
                }
            }
        }
        
        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                boolean didit=false;
                
                switch (o.opcode()) {
                case Xor:
                case FXor:
                    if (o.rhs(0)==o.rhs(1)) {
                        LArg ss=spills.get(o.rhs(0));
                        if (ss!=null) {
                            Tmp t=code.addTmp(o.rhs(0).kind());
                            o.rhs[0]=t;
                            o.rhs[1]=t;
                            o.append(
                                new LOp(
                                    LOpCode.Mov,o.type(),
                                    new LArg[]{
                                        t,
                                        ss
                                    }));
                            didit=true;
                        }
                    }
                    break;
                default:
                    break;
                }
                
                if (!didit) {
                    for (int i=0;i<o.nrhs();++i) {
                        o.rhs(i).spill(spills,o,i);
                    }
                }
            }
        }
        
        if (Global.verbosity>=5) {
            Global.log.println("Removing temporaries: "+spillTmps);
        }

        for (Tmp t : spillTmps) {
            code.delTmp(t);
        }
        
        setChangedCode();
        code.killIntraBlockAnalyses();
    }
}

