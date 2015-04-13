/*
 * LLivenessCalc.java
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
import com.fiji.fivm.c1.x86.arg.LArg;

public class LLivenessCalc {
    LCode c;
    
    LVarSet empty;
    
    HashMap< LHeader, LVarSet > liveAtHead;
    HashMap< LHeader, LVarSet > liveAtTail;
    
    LPredsCalc preds;
    MyStack< LHeader > worklist;
    
    LLivenessCalc(LCode c) {
        this.c=c;
        empty=new LVarSet(c);
        liveAtHead=new HashMap< LHeader, LVarSet >();
        liveAtTail=new HashMap< LHeader, LVarSet >();
        
        preds=c.getPreds();
        
        worklist=new MyStack< LHeader >();
        for (LHeader h : preds.terminals()) {
            worklist.push(h);
        }
        
        propagate();
        
	boolean changed=true;
	while (changed) {
	    changed=false;
	    for (LHeader h : c.headers()) {
		if (!liveAtHead.containsKey(h)) {
		    changed=true;
		    if (Global.verbosity>=6) {
			Global.log.println("Repropagating "+h);
		    }
		    worklist.push(h);
		    propagate();
		    break;
		}
	    }
	}
	worklist=null;
	preds=null;
    }
    
    void propagate() {
        while (!worklist.empty()) {
            LHeader h=worklist.pop();
            LVarSet curLive=liveAtTail.get(h);
            if (curLive==null) {
                curLive=new LVarSet(c);
            } else {
                curLive=curLive.copy();
            }
            for (LOp o : h.reverseOperations()) {
                update(o,curLive);
            }
            LVarSet newLiveAtHead=curLive;
            LVarSet oldLiveAtHead=liveAtHead.get(h);
            if (oldLiveAtHead==null || !oldLiveAtHead.equals(newLiveAtHead)) {
                liveAtHead.put(h,newLiveAtHead);
                for (LHeader h2 : preds.preds(h)) {
                    LVarSet oldLiveAtTail=liveAtTail.get(h2);
                    if (oldLiveAtTail==null) {
                        liveAtTail.put(h2,newLiveAtHead.copy());
                        worklist.push(h2);
                    } else if (oldLiveAtTail.addAll(newLiveAtHead)) {
                        worklist.push(h2);
                    }
                }
            }
        }
    }
    
    UpdateState update(LOp o,
                       LVarSet curLive) {
        switch (o.opcode()) {
        case Xor:
        case FXor:
            if (o.rhs(0)==o.rhs(1)) {
                // only defs, does not use
                if (curLive.remove(o.rhs(0))) {
                    if (Global.verbosity>=5) {
                        Global.log.println("Simple live xor: "+o);
                    }
                    return UpdateState.LIVE;
                } else {
                    if (Global.verbosity>=5) {
                        Global.log.println("Simple dead xor: "+o);
                    }
                    return UpdateState.DEAD;
                }
            }
            break;
        default:
            break;
        }
        
        boolean live=o.sideEffect() || o.footer();
        
        for (LArg a : o.defs()) {
            if (Global.verbosity>=5) {
                Global.log.println("Def at "+o+": "+a);
            }
            if (curLive.remove(a)) {
                if (Global.verbosity>=5) {
                    Global.log.println("   it's live.");
                }
                live=true;
            }
        }
        
        if (!live) {
            if (Global.verbosity>=5) {
                Global.log.println("This is dead: "+o);
            }
            return UpdateState.DEAD;
        }
        
        for (LArg a : o.uses()) {
            if (Global.verbosity>=5) {
                Global.log.println("Use at "+o+": "+a);
            }
            if (a.variable()) {
                curLive.add(a);
            }
        }
        
        return UpdateState.LIVE;
    }
    
    public LVarSet liveAtHead(LHeader h) {
        LVarSet result=liveAtHead.get(h);
        assert result!=null;
        return result;
    }
    
    public boolean liveAtHead(LHeader h,LArg a) {
        return liveAtHead(h).contains(a);
    }
    
    public LVarSet liveAtTail(LHeader h) {
        LVarSet result=liveAtTail.get(h);
        if (result==null) {
            return empty;
        }
        return result;
    }
    
    public class LocalCalc {
        LVarSet curLive;
        
        public LocalCalc(LHeader h) {
            curLive=liveAtTail(h).copy();
        }
        
        public boolean currentlyLive(LArg a) {
            return curLive.contains(a);
        }
        
        public LVarSet currentlyLive() {
            return curLive;
        }
        
        public UpdateState update(LOp o) {
            return LLivenessCalc.this.update(o,curLive);
        }
    }
    
    public class ForwardLocalCalc {
        HashMap< LOp, LVarSet > deaths;
        HashMap< LOp, LVarSet > births;
        LVarSet curLive;
        
        public ForwardLocalCalc(LHeader h) {
            deaths=new HashMap< LOp, LVarSet >();
            births=new HashMap< LOp, LVarSet >();
            
            curLive=liveAtTail(h).copy();
            
            LVarSet curDeaths=new LVarSet(c);
            LVarSet curBirths=new LVarSet(c);
            for (LOp o : h.reverseOperations()) {
                for (LArg a : o.defs()) {
                    if (curLive.remove(a)) {
                        curBirths.add(a);
                    }
                }
                for (LArg a : o.uses()) {
                    if (a.variable()) {
                        if (curLive.add(a)) {
                            curDeaths.add(a);
                        }
                    }
                }
                if (!curDeaths.isEmpty()) {
                    deaths.put(o,curDeaths);
                    curDeaths=new LVarSet(c);
                }
                if (!curBirths.isEmpty()) {
                    births.put(o,curBirths);
                    curBirths=new LVarSet(c);
                }
            }
        }
        
        public boolean currentlyLive(LArg a) {
            return curLive.contains(a);
        }
        
        public LVarSet births(LOp o) {
            LVarSet result=births.get(o);
            if (result==null) {
                return empty;
            } else {
                return result;
            }
        }
        
        public LVarSet deaths(LOp o) {
            LVarSet result=deaths.get(o);
            if (result==null) {
                return empty;
            } else {
                return result;
            }
        }
        
        public void update(LOp o) {
            LVarSet curDeaths=deaths.get(o);
            if (curDeaths!=null) {
                curLive.removeAll(curDeaths);
            }
            LVarSet curBirths=births.get(o);
            if (curBirths!=null) {
                curLive.addAll(curBirths);
            }
        }
    }
}

