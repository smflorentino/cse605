/*
 * HeaderProximityCalc.java
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

public class HeaderProximityCalc {
    int limit;
    HashMap< Header, HashMap< Header, MutableInt > > proximity;
    
    public HeaderProximityCalc(Code c,int limit) {
        this.limit=limit;
        
        Worklist worklist=new Worklist();
        
        proximity=new HashMap< Header, HashMap< Header, MutableInt > >();
        
        worklist.push(c.root());
        proximity.put(c.root(),new HashMap< Header, MutableInt >());
        
        while (!worklist.empty()) {
            Header h=worklist.pop();
            HashMap< Header, MutableInt > myProx=proximity.get(h);
            for (Header h2 : h.likelySuccessors()) {
                HashMap< Header, MutableInt > theirProx=proximity.get(h2);
                boolean changed=false;
                if (theirProx==null) {
                    theirProx=new HashMap< Header, MutableInt >();
                    for (Map.Entry< Header, MutableInt > e : myProx.entrySet()) {
                        int theirCnt=e.getValue().value;
                        theirCnt++;
                        if (theirCnt<=limit) {
                            theirProx.put(e.getKey(),new MutableInt(theirCnt));
                        }
                    }
                    theirProx.put(h,new MutableInt(1));
                    proximity.put(h2,theirProx);
                    changed=true;
                } else {
                    for (Map.Entry< Header, MutableInt > e : myProx.entrySet()) {
                        int theirCnt=e.getValue().value+1;
                        if (theirCnt<=limit) {
                            MutableInt theirCurCnt=theirProx.get(e.getKey());
                            if (theirCurCnt==null) {
                                theirProx.put(e.getKey(),new MutableInt(theirCnt));
                                changed=true;
                            } else {
                                changed|=theirCurCnt.change(Math.min(theirCnt,theirCurCnt.value));
                            }
                        }
                    }
                    MutableInt theirMyCnt=theirProx.get(h);
                    if (theirMyCnt==null) {
                        theirProx.put(h,new MutableInt(1));
                        changed=true;
                    } else {
                        changed|=theirMyCnt.change(1);
                    }
                }
                if (changed) {
                    worklist.push(h2);
                }
            }
            for (Header h2 : h.allSuccessors()) {
                if (!proximity.containsKey(h2)) {
                    proximity.put(h2,new HashMap< Header, MutableInt >());
                    worklist.push(h2);
                }
            }
        }
    }
    
    public HeaderProximityCalc(Code c) {
        this(c,Global.defaultProxLimit);
    }
    
    public int limit() {
        return limit;
    }
    
    public int proximity(Header from,Header to) {
        if (from==to) {
            return 0;
        } else {
            MutableInt result=proximity.get(to).get(from);
            if (result==null) {
                return -1; // means infinity
            } else {
                return result.value;
            }
        }
    }
    
    public boolean proximityIsFinite(Header from,Header to) {
        if (from==to) {
            return true;
        } else if (proximity.get(to).containsKey(from)) {
            return true;
        } else {
            return false;
        }
    }
    
    public HashMap< Header, MutableInt > proximity(Header to) {
        return proximity.get(to);
    }
}

