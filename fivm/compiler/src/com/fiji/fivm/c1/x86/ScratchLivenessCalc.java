/*
 * ScratchLivenessCalc.java
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

public class ScratchLivenessCalc {
    LCode c;
    
    HashSet< ScratchSlot > escapes;
    
    HashSet< Object > empty;
    
    HashMap< LHeader, HashSet< Object > > liveAtHead;
    HashMap< LHeader, HashSet< Object > > liveAtTail;
    
    LPredsCalc preds;
    MyStack< LHeader > worklist;
    
    ScratchLivenessCalc(LCode c) {
        this.c=c;
        empty=new HashSet< Object >();
        liveAtHead=new HashMap< LHeader, HashSet< Object > >();
        liveAtTail=new HashMap< LHeader, HashSet< Object > >();
        
        escapes=new HashSet< ScratchSlot >();
        for (LHeader h : c.headers()) {
            for (LOp o : h.operations()) {
                if (o.opcode==LOpCode.Lea && o.rhs(0) instanceof OffScratchSlot) {
                    escapes.add(((OffScratchSlot)o.rhs(0)).slot());
                }
            }
        }
        
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
            HashSet< Object > curLive=liveAtTail.get(h);
            if (curLive==null) {
                curLive=new HashSet< Object >();
            } else {
                curLive=Util.copy(curLive);
            }
            for (LOp o : h.reverseOperations()) {
                update(o,curLive);
            }
            HashSet< Object > newLiveAtHead=curLive;
            HashSet< Object > oldLiveAtHead=liveAtHead.get(h);
            if (oldLiveAtHead==null || !oldLiveAtHead.equals(newLiveAtHead)) {
                liveAtHead.put(h,newLiveAtHead);
                for (LHeader h2 : preds.preds(h)) {
                    HashSet< Object > oldLiveAtTail=liveAtTail.get(h2);
                    if (oldLiveAtTail==null) {
                        liveAtTail.put(h2,Util.copy(newLiveAtHead));
                        worklist.push(h2);
                    } else if (oldLiveAtTail.addAll(newLiveAtHead)) {
                        worklist.push(h2);
                    }
                }
            }
        }
    }
    
    UpdateState update(LOp o,
                       HashSet< Object > curLive) {
        boolean live=o.opcode().sideEffect() || o.footer();
        
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
        
        for (int i=0;i<o.nrhs();++i) {
            if (o.defsDirectly(i) && o.rhs(i).memory()) {
                if (o.rhs(i) instanceof OffScratchSlot) {
                    OffScratchSlot oss=(OffScratchSlot)o.rhs(i);
                    if (escapes.contains(oss.slot())) {
                        live=true;
                    } else {
                        int start=oss.offset();
                        int size=o.typeOf(i).size();
                        int end=start+size;
                        assert end<=oss.slot().size();
                        for (int j=start;j<end;++j) {
                            if (curLive.remove(new ScratchSlotByte(oss.slot(),j))) {
                                live=true;
                            }
                        }
                    }
                } else {
                    live=true;
                }
            }
        }
        
        for (LArg a : o.defs()) {
            if (curLive.remove(a)) {
                live=true;
            }
        }
        
        if (!live) {
            return UpdateState.DEAD;
        }
        
        for (int i=0;i<o.nrhs();++i) {
            if (o.usesDirectly(i) && o.rhs(i) instanceof OffScratchSlot) {
                OffScratchSlot oss=(OffScratchSlot)o.rhs(i);
                if (!escapes.contains(oss.slot())) {
                    int start=oss.offset();
                    int size=o.typeOf(i).size();
                    int end=start+size;
                    assert end<=oss.slot().size();
                    for (int j=start;j<end;++j) {
                        curLive.add(new ScratchSlotByte(oss.slot(),j));
                    }
                }
            }
        }
        
        for (LArg a : o.uses()) {
            if (a.variable()) {
                curLive.add(a);
            }
        }
        
        return UpdateState.LIVE;
    }
    
    public HashSet< ScratchSlot > escapes() {
        return escapes;
    }
    
    public HashSet< Object > liveAtHead(LHeader h) {
        HashSet< Object > result=liveAtHead.get(h);
        assert result!=null;
        return result;
    }
    
    public boolean liveAtHead(LHeader h,ScratchSlot ss) {
        return liveAtHead(h).contains(ss);
    }
    
    public HashSet< Object > liveAtTail(LHeader h) {
        HashSet< Object > result=liveAtTail.get(h);
        if (result==null) {
            return empty;
        }
        return result;
    }
    
    public class LocalCalc {
        HashSet< Object > curLive;
        
        public LocalCalc(LHeader h) {
            curLive=Util.copy(liveAtTail(h));
        }
        
        public boolean currentlyLive(ScratchSlot ss) {
            return curLive.contains(ss);
        }
        
        public boolean currentlyLive(LArg a) {
            return curLive.contains(a);
        }
        
        public HashSet< Object > currentlyLive() {
            return curLive;
        }
        
        public UpdateState update(LOp o) {
            return ScratchLivenessCalc.this.update(o,curLive);
        }
    }
}

