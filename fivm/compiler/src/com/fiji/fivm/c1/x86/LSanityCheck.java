/*
 * LSanityCheck.java
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
import com.fiji.fivm.c1.x86.arg.LArg;
import com.fiji.fivm.c1.x86.arg.Reg;
import com.fiji.fivm.c1.x86.arg.Tmp;

public class LSanityCheck extends LCodePhase {
    public LSanityCheck(LCode c) { super(c); }
    
    public void visitCode() {
        doit();
    }
    
    HashSet< Tmp > allowedTmps=new HashSet< Tmp >();
    HashSet< LHeader > allowedHeaders=new HashSet< LHeader >();
    HashSet< Integer > headerNumbers=new HashSet< Integer >();
    
    void verifyHeader(LHeader h) {
        assert allowedHeaders.contains(h)==true : h;
        assert h.code==code;
        assert headerNumbers.add(h.order);
        assert h.prev==null;
        assert h.next!=null;
        assert h.next.prev==h;
        assert h.next instanceof LOp;
        HashSet< LNode > set=new HashSet< LNode >();
        for (LNode n=h.next;n!=h.footer;n=n.next) {
            assert n instanceof LOp && !((LOp)n).footer();
            assert ((LOp)n).head==h;
            set.add(n);
        }
        for (LNode n=h.footer.prev;n!=h;n=n.prev) {
            assert n instanceof LOp && !((LOp)n).footer();
            assert ((LOp)n).head==h;
            assert set.remove(n);
        }
        assert set.isEmpty();
    }
    
    void verifyOperation(LOp o) {
        for (int i=0;i<o.rhs().length;++i) {
            for (int j=0;j<o.rhs()[i].nUseOnUseVars();++j) {
                LArg a=o.rhs()[i].useOnUseVar(j);
                assert a.variable();
                if (a instanceof Tmp) {
                    assert !code.registersAllocated;
                    assert allowedTmps.contains((Tmp)a);
                } else {
                    assert a instanceof Reg;
                    assert ((Reg)a).allowed();
                }
            }
        }
        
        o.checkSanity();
    }
    
    void verifyInstruction(LOp i) {
        assert i.prev.next==i;
        assert i.next.prev==i;
        assert i.next instanceof LOp;
        assert i.prev instanceof LHeader
            || i.prev instanceof LOp;
        assert !i.footer();
        
        verifyOperation(i);
    }
    
    void verifyFooter(LFooter f) {
        assert f.prev.next==f;
        assert f.next==null;
        for (LHeader h2 : f.successors()) {
            assert allowedHeaders.contains(h2)==true : "for target "+h2+" from "+f;
        }
        
        switch (f.opcode()) {
        case Return:
        case NotReached:
            assert f.successors.length==0;
            break;
        case Jump:
            assert f.successors.length==1;
            break;
        case BranchLessThan:
        case BranchLTEq:
        case BranchULessThan:
        case BranchULTEq:
        case BranchEq:
        case BranchNeq:
        case BranchAndZero:
        case BranchAndNotZero:
        case BranchCASSucc:
        case BranchCASFail:
        case RebranchLessThan:
        case RebranchLTEq:
        case RebranchULessThan:
        case RebranchULTEq:
        case RebranchEq:
        case RebranchNeq:
        case BranchGreaterThan:
        case BranchGTEq:
        case BranchFGreaterThan:
        case BranchFGTEq:
        case BranchNotFGT:
        case BranchNotFGTEq:
        case BranchUGreaterThan:
        case BranchUGTEq:
        case RebranchGreaterThan:
        case RebranchGTEq:
        case RebranchNotFGT:
        case RebranchNotFGTEq:
        case RebranchUGreaterThan:
        case RebranchUGTEq:
        case BranchFEqOrUnordered:
        case BranchFNeqAndOrdered:
        case RebranchFOrdered:
        case RebranchFUnordered:
            assert f.successors.length==2;
            break;
        case AwesomeJump:
            assert f.successors.length>=1;
            break;
        default:
            assert false;
        }
        
        verifyOperation(f);
    }
    
    public void doit() {
        boolean asserts=false;
        assert asserts=true;
        if (!asserts) {
            throw new Error("assertions must be enabled");
        }
        try {
            code.recomputeOrder();
            for (int i=0;i<code.tmps().size();++i) {
                assert !code.registersAllocated;
                Tmp t=code.tmps().get(i);
                try {
                    assert t.id()==i+33;
                } catch (Throwable e) {
                    throw new SanityCheckFailed(
                        "Sanity checking failed for "+t+", tmps = "+code.tmps(),e);
                }
            }
            
            LVarSet liveAtRoot=new LLivenessCalc(code).liveAtHead(code.root()).copy();
            liveAtRoot.remove(Reg.SP);
            for (Reg r : Reg.persistents) {
                liveAtRoot.remove(r);
            }
            if (Global.pointerSize==4 && !code.expanded32 && code.returnType()==Basetype.LONG) {
                // HACK!  if returning 64-bit values then Reg.DX won't be set until Expand32 runs.
                liveAtRoot.remove(Reg.DX);
            }
            assert liveAtRoot.isEmpty() : liveAtRoot;
            
            allowedTmps.addAll(code.tmps());
            allowedHeaders.addAll(code.headers());
            
            boolean foundRoot=false;
            for (LHeader h : code.headers()) {
                if (h==code.root()) foundRoot=true;
                try {
                    verifyHeader(h);
                    for (LOp o : h.instructions()) {
                        try {
                            verifyInstruction(o);
                        } catch (Throwable e) {
                            throw new SanityCheckFailed(
                                "Sanity check failed for "+o,e);
                        }
                    }
                    try {
                        verifyFooter(h.footer());
                    } catch (Throwable e) {
                        throw new SanityCheckFailed(
                            "Sanity check failed for "+h.footer(),e);
                    }
                } catch (Throwable e) {
                    throw new SanityCheckFailed(
                        "Sanity checking failed for "+h,e);
                }
            }
            assert foundRoot==true : code.headers();
        } catch (Throwable e) {
            throw new SanityCheckFailed("Sanity checking failed for "+code,e);
        }
    }
}

