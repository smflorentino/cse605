/*
 * LSimplify.java
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

public class LSimplify extends LCodePhase {
    public LSimplify(LCode c) { super(c); }
    
    public boolean runOnce() {
        boolean changed=false;
        LPredsCalc preds=new LPredsCalc(code);
        
        // find dead headers
        
        HashSet< LHeader > seen=new HashSet< LHeader >();
        MyStack< LHeader > worklist=new MyStack< LHeader >();
        
        seen.add(code.root());
        worklist.push(code.root());
        while (!worklist.empty()) {
            LHeader h=worklist.pop();
            for (LHeader h2 : h.successors()) {
                if (seen.add(h2)) {
                    worklist.push(h2);
                }
            }
        }
        
        boolean curChanged=false;
        for (Iterator< LHeader > i=code.headers().iterator();i.hasNext();) {
            LHeader h=i.next();
            if (!seen.contains(h)) {
                changed=curChanged=true;
                setChangedCode("removed dead header");
                i.remove();
            }
        }
        
        if (curChanged) preds=new LPredsCalc(code);
        
        // turn obviously redundant branches into jumps
        curChanged=false;
        for (LHeader h : code.headers()) {
            if (!h.footer().sideEffect() && !h.footer().defsRegs() && !h.footer().terminal()) {
                LHeader succ=h.footer().successors()[0];
                boolean allSame=true;
                for (int i=1;i<h.footer().successors().length;++i) {
                    if (succ!=h.footer().successors()[i]) {
                        allSame=false;
                        break;
                    }
                }
                if (allSame && h.footer().opcode()!=LOpCode.Jump) {
                    h.setFooter(
                        new LFooter(
                            LOpCode.Jump,LType.Void,LArg.EMPTY,
                            new LHeader[]{succ}));
                    changed=curChanged=true;
                    setChangedCode("removed redundant branch");
                }
            }
        }
        
        // find redundant jumps
        
        HashSet< LHeader > killed=new HashSet< LHeader >();
        curChanged=false;
        for (LHeader h : code.headers()) {
            if (!killed.contains(h) &&
                h.footer().opcode()==LOpCode.Jump &&
                !killed.contains(h.successors()[0]) &&
                h.successors()[0]!=code.root()) {
                LHeader h2=h.successors()[0];
                Iterator< LHeader > targPredsI=preds.preds(h2).iterator();
                if (targPredsI.hasNext() &&
                    targPredsI.next()==h &&
                    !targPredsI.hasNext()) {
                    
                    LNode preFooterNode=h.footer.prev;
                    
                    preFooterNode.next=h2.next;
                    h2.next.prev=preFooterNode;
                    h.footer=h2.footer;
                    h.forceOwnership((LOp)h2.next);
                    
                    killed.add(h2);
                    curChanged=changed=true;
                    setChangedCode("removed redundant jump");
                }
            }
        }
        
        if (curChanged) {
            for (Iterator< LHeader > i=code.headers().iterator();i.hasNext();) {
                LHeader h=i.next();
                if (killed.contains(h)) {
                    i.remove();
                }
            }
            preds=new LPredsCalc(code);
        }
        
        // figure out which blocks are redundant - that is, which blocks
        // are empty and end in a jump
        killed=new HashSet< LHeader >();
        HashSet< LHeader > offLimits=new HashSet< LHeader >();
        curChanged=false;
        for (LHeader h : code.headers()) {
            if (!offLimits.contains(h) &&
                !preds.pinned(h) &&
                h.next==h.footer &&
                h.footer.opcode==LOpCode.Jump &&
                h.footer.successors[0]!=h) {
                for (LHeader ph : preds.preds(h)) {
                    for (int i=0;i<ph.footer.successors.length;++i) {
                        if (ph.footer.successors[i]==h) {
                            ph.footer.successors[i]=h.footer.successors[0];
                        }
                    }
                }
                killed.add(h);
                offLimits.add(h.footer.successors[0]);
                changed=curChanged=true;
                setChangedCode("killed redundant block");
            }
        }
        if (curChanged) {
            for (Iterator< LHeader > i=code.headers().iterator();i.hasNext();) {
                LHeader h=i.next();
                if (killed.contains(h)) {
                    i.remove();
                }
            }
        }
        
        // kill totally unused temporaries
        LVarSet used=new LVarSet(code);
        for (LHeader h : code.headers()) {
            for (LOp o : h.operations()) {
                for (LArg a : o.rhs()) {
                    if (a instanceof Tmp) {
                        used.add(a);
                    } else {
                        for (int i=0;i<a.nUseOnUseVars();++i) {
                            LArg a2=a.useOnUseVar(i);
                            if (a2 instanceof Tmp) {
                                used.add(a2);
                            }
                        }
                    }
                }
            }
        }
        
        ArrayList< Tmp > toRemove=new ArrayList< Tmp >();
        for (Tmp t : code.tmps()) {
            if (!used.contains(t)) {
                toRemove.add(t);
            }
        }
        
        for (Tmp t : toRemove) {
            code.delTmp(t);
            setChangedCode("removed temporary");
            changed=true;
        }
        
        return changed;
    }
    
    public void visitCode() {
        int cnt=0;
        while (runOnce()) {
            cnt++;
            if (cnt>1000 && Global.verbosity>=1) {
                Global.log.println("LSimplify fixpoint continues because: "+changedCodeReason);
            }
        }
        
        if (changedCode()) {
            code.killAllAnalyses();
        }
    }
}

