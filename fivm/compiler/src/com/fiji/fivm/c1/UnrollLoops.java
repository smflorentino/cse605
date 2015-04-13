/*
 * UnrollLoops.java
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
 * Implements both loop unrolling and loop peeling.
 */
public class UnrollLoops extends CodePhase {
    UnrollMode mode;
    
    public UnrollLoops(Code c,UnrollMode mode) {
        super(c);
        this.mode=mode;
    }
    
    public void visitCode() {
        assert !code.isSSA();
        
        CostFunction cf=mode.costFunction();
        
        boolean changed=true;
        
        HashSet< Header > globalExclusion=new HashSet< Header >();
        
        while (changed) {
            changed=false;
            
            HashSet< Header > exclusion=new HashSet< Header >();
            
            LoopCalc loops=code.getLoops();
            
            if (Global.verbosity>=3) {
                Global.log.println("Doing iteration of UnrollLoops with mode = "+mode);
                Global.log.println("Loops:");
                for (Header start : loops.loopHeaders()) {
                    Global.log.print("   "+start+":");
                    for (Header body : loops.loopBody(start)) {
                        Global.log.print(" "+body);
                    }
                    Global.log.println();
                }
            }
            
            for (Header oldStart : loops.loopHeaders()) {
                if (!globalExclusion.contains(oldStart)) {
                    Set< Header > body=loops.loopBody(oldStart);
                    if (!Util.intersects(exclusion,body) &&
                        cf.cost(body)<=mode.budget()) {
                    
                        HashMap< Header, Header > map=new HashMap< Header, Header >();
                        for (Header oldh : body) {
                            map.put(oldh,(Header)oldh.makeCopy());
                        }
                    
                        // make jumps back to the oldStart in the old body jump to the
                        // new start
                        HashMap< Header, Header > msrmap=new HashMap< Header, Header >();
                        msrmap.put(oldStart,map.get(oldStart));
                        MultiSuccessorReplacement msr=new MultiSuccessorReplacement(msrmap);
                        for (Header oldh : body) {
                            oldh.getFooter().accept(msr);
                        }
                    
                        // make jumps in the new body go to the new body instead of the
                        // old one, except in the case of unrolling (as opposed to peeling),
                        // in which case the start goes to the new body
                        msrmap=new HashMap< Header, Header >();
                        msrmap.putAll(map);
                        if (mode==UnrollMode.UNROLL_LOOPS) {
                            msrmap.remove(oldStart);
                        }
                        msr=new MultiSuccessorReplacement(msrmap);
                        for (Header newh : map.values()) {
                            newh.getFooter().accept(msr);
                        }
                    
                        exclusion.addAll(body);
                        exclusion.addAll(map.values());
                        if (mode==UnrollMode.PEEL_LOOPS) {
                            globalExclusion.add(map.get(oldStart));
                            for (Header h : new ArrayList< Header >(globalExclusion)) {
                                Header h2=map.get(h);
                                if (h2!=null) {
                                    globalExclusion.add(h2);
                                }
                            }
                        }
                    
                        changed=true;
                    }
                }
            }
            
            if (changed) {
                setChangedCode();
                code.killAllAnalyses();
            }
        }
    }
}

