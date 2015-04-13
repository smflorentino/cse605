/*
 * SlowPathLiveRangeSplit.java
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

public class SlowPathLiveRangeSplit extends LCodePhase {
    public SlowPathLiveRangeSplit(LCode c) { super(c); }
    
    public void visitCode() {
        boolean hasSlow=false;
        
        for (LHeader h : code.headers()) {
            if (h.unlikely()) {
                hasSlow=true;
                break;
            }
        }
        
        if (!hasSlow) {
            return;
        }
        
        LPredsCalc preds=code.getPreds();
        LLivenessCalc llc=code.getLiveness();
        
        for (LHeader h : preds.pinned()) {
            for (LHeader h2 : preds.preds(h)) {
                if (h.probability()!=h2.probability()) {
                    // NOTE: this will probably never happen, because switches don't have branch
                    // probabilities.  One the other hand, nano tracing could make it happen
                    // quite easily in sufficiently bizarre code.
                    if (Global.verbosity>=1) {
                        Global.log.println("WARNING: Cannot do slow-path live range splitting in "+code.origin().shortName()+" because of odd uses of pinned headers.");
                    }
                    return;
                }
            }
        }
        
        HashMap< Tmp, Tmp > slowMap=new HashMap< Tmp, Tmp >();
        
        for (Tmp t : new ArrayList< Tmp >(code.tmps())) {
            if (t.isInt()) {
                slowMap.put(t,code.addTmp(t.kind()));
            }
        }
        
        HashMap< LHeader, LHeader > landingPadMap=new HashMap< LHeader, LHeader >();
        
        ArrayList< LHeader > headers=code.headers3();
        
        for (LHeader h : headers) {
            boolean needsLandingPad=false;
            for (LHeader h2 : preds.preds(h)) {
                if (h.probability()!=h2.probability()) {
                    needsLandingPad=true;
                }
            }
            if (needsLandingPad) {
                LHeader pad=code.addHeader();
                pad.setProbability(HeaderProbability.UNLIKELY_TO_EXECUTE);
                pad.setFooter(
                    new LFooter(
                        LOpCode.Jump,LType.Void,LArg.EMPTY,
                        new LHeader[]{
                            h
                        }));
                for (LArg a : llc.liveAtHead(h)) {
                    if (a instanceof Tmp) {
                        Tmp t=(Tmp)a;
                        if (t.isInt()) {
                            Tmp t2=slowMap.get(t);
                            switch (h.probability()) {
                            case DEFAULT_PROBABILITY:
                                pad.append(
                                    new LOp(
                                        LOpCode.Mov,LType.ptr(),
                                        new LArg[]{
                                            t2,
                                            t
                                        }));
                                break;
                            case UNLIKELY_TO_EXECUTE:
                                pad.append(
                                    new LOp(
                                        LOpCode.Mov,LType.ptr(),
                                        new LArg[]{
                                            t,
                                            t2
                                        }));
                                break;
                            default: throw new Error();
                            }
                        }
                    }
                }
                landingPadMap.put(h,pad);
            }
        }
        
        for (LHeader h : headers) {
            switch (h.probability()) {
            case DEFAULT_PROBABILITY: {
                for (int i=0;i<h.footer().successors.length;++i) {
                    LHeader h2=h.footer().successors[i];
                    if (h2.probability()==HeaderProbability.UNLIKELY_TO_EXECUTE) {
                        LHeader pad=landingPadMap.get(h2);
                        assert pad!=null;
                        h.footer().successors[i]=pad;
                    }
                }
                break;
            }
            case UNLIKELY_TO_EXECUTE: {
                for (LOp o : h.operations()) {
                    o.mapRhs(slowMap);
                }
                for (int i=0;i<h.footer().successors.length;++i) {
                    LHeader h2=h.footer().successors[i];
                    if (h2.probability()==HeaderProbability.DEFAULT_PROBABILITY) {
                        LHeader pad=landingPadMap.get(h2);
                        assert pad!=null;
                        h.footer().successors[i]=pad;
                    }
                }
                break;
            }
            default: throw new Error();
            }
        }
        
        code.killAllAnalyses();
        setChangedCode();
    }
}

