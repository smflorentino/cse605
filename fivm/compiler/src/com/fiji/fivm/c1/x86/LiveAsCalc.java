/*
 * LiveAsCalc.java
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

public class LiveAsCalc {
    LCode c;
    
    HashMap< LArg, Integer > empty;
    
    HashMap< LHeader, HashMap< LArg, Integer > > liveAtHead;
    HashMap< LHeader, HashMap< LArg, Integer > > liveAtTail;
    
    LPredsCalc preds;
    MyStack< LHeader > worklist;
    
    LiveAsCalc(LCode c) {
        this.c=c;
        empty=new HashMap< LArg, Integer >();
        liveAtHead=new HashMap< LHeader, HashMap< LArg, Integer > >();
        liveAtTail=new HashMap< LHeader, HashMap< LArg, Integer > >();
        
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
            HashMap< LArg, Integer > curLive=liveAtTail.get(h);
            if (curLive==null) {
                curLive=new HashMap< LArg, Integer >();
            } else {
                curLive=Util.copy(curLive);
            }
            for (LOp o : h.reverseOperations()) {
                update(o,curLive);
            }
            HashMap< LArg, Integer > newLiveAtHead=curLive;
            HashMap< LArg, Integer > oldLiveAtHead=liveAtHead.get(h);
            if (oldLiveAtHead==null || !oldLiveAtHead.equals(newLiveAtHead)) {
                liveAtHead.put(h,newLiveAtHead);
                for (LHeader h2 : preds.preds(h)) {
                    HashMap< LArg, Integer > oldLiveAtTail=liveAtTail.get(h2);
                    if (oldLiveAtTail==null) {
                        liveAtTail.put(h2,Util.copy(newLiveAtHead));
                        worklist.push(h2);
                    } else {
                        boolean changed=false;
                        for (Map.Entry< LArg, Integer > e : newLiveAtHead.entrySet()) {
                            LArg v=e.getKey();
                            int size=e.getValue();
                            Integer oldSize=oldLiveAtTail.get(v);
                            if (oldSize==null || size>oldSize) {
                                oldLiveAtTail.put(v,size);
                                changed=true;
                            }
                        }
                        if (changed) {
                            worklist.push(h2);
                        }
                    }
                }
            }
        }
    }
    
    void update(LOp o,
                HashMap< LArg, Integer > curLive) {
        switch (o.opcode()) {
        case Xor:
        case FXor:
            if (o.rhs(0)==o.rhs(1)) {
                // only defs, does not use
                if (curLive.remove(o.rhs(0))==null) {
                    if (Global.verbosity>=5) {
                        Global.log.println("Simple live xor: "+o);
                    }
                    return;
                } else {
                    if (Global.verbosity>=5) {
                        Global.log.println("Simple dead xor: "+o);
                    }
                    return;
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
            if (curLive.remove(a)!=null) {
                if (Global.verbosity>=5) {
                    Global.log.println("   it's live.");
                }
                live=true;
            }
        }
        
        if (!live) {
            return;
        }
        
        for (int i=0;i<o.nrhs();++i) {
            if (o.form().useArg(i)) {
                if (o.rhs(i).variable()) {
                    curLive.put(o.rhs(i),o.typeOf(i).size());
                } else {
                    for (int j=o.rhs(i).nUseOnUseVars();j-->0;) {
                        curLive.put(o.rhs(i).useOnUseVar(j),o.memType().size());
                    }
                }
            }
            if (o.form().defArg(i)) {
                // NOTE: if a variable is used as two different types, this
                // one will always be larger.
                for (int j=o.rhs(i).nUseOnDefVars();j-->0;) {
                    curLive.put(o.rhs(i).useOnDefVar(j),o.memType().size());
                }
            }
        }
        
        for (LArg a : o.form().implicitUses(c)) {
            curLive.put(a,LType.from(a.kind()).size());
        }
    }
    
    public HashMap< LArg, Integer > liveAtHead(LHeader h) {
        HashMap< LArg, Integer > result=liveAtHead.get(h);
        assert result!=null;
        return result;
    }
    
    public int liveAtHead(LHeader h,LArg a) {
        Integer size=liveAtHead(h).get(a);
        if (size==null) {
            return 0;
        } else {
            return size;
        }
    }
    
    public HashMap< LArg, Integer > liveAtTail(LHeader h) {
        HashMap< LArg, Integer > result=liveAtTail.get(h);
        if (result==null) {
            return empty;
        }
        return result;
    }
    
    public class LocalCalc {
        HashMap< LArg, Integer > curLive;
        
        public LocalCalc(LHeader h) {
            curLive=Util.copy(liveAtTail(h));
        }
        
        public int currentlyLive(LArg a) {
            Integer size=curLive.get(a);
            if (size==null) {
                return 0;
            } else {
                return size;
            }
        }
        
        public HashMap< LArg, Integer > currentlyLive() {
            return curLive;
        }
        
        public void update(LOp o) {
            LiveAsCalc.this.update(o,curLive);
        }
    }
}

