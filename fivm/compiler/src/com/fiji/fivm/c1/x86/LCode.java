/*
 * LCode.java
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

public class LCode {
    CodeOrigin origin;
    
    String cname;
    
    ArrayList< LHeader > headers=new ArrayList< LHeader >();
    ArrayList< ScratchSlot > scratches=new ArrayList< ScratchSlot >();
    ArrayList< Tmp > tmps=new ArrayList< Tmp >();
    
    LHeader root;
    
    boolean expanded32;
    boolean registersAllocated;
    boolean stackBuilt;
    private LinkedHashSet< Reg > persistentsUsed=new LinkedHashSet< Reg >();
    
    private int tmpIds;
    
    Basetype returnType;
    
    public LCode(String cname,Basetype returnType,CodeOrigin origin) {
        this.cname=cname;
        this.returnType=returnType;
        this.origin=origin;
    }
    
    public String cname() {
        return cname;
    }
    
    public Basetype returnType() {
        return returnType;
    }
    
    public CodeOrigin origin() {
        return origin;
    }
    
    public List< LHeader > headers() { return headers; }
    public List< ScratchSlot > scratches() { return scratches; }
    public List< Tmp > tmps() { return tmps; }
    
    public LVarSet tmpSet() {
        LVarSet result=new LVarSet(this);
        result.addAll(tmps());
        return result;
    }
    
    public LVarSet varSet() {
        LVarSet result=tmpSet();
        for (Reg r : Reg.table) {
            result.add(r);
        }
        return result;
    }
    
    public LArg getVar(int id) {
        if (id<33) {
            return Reg.table[id];
        } else {
            return tmps.get(id-33);
        }
    }
    
    public int numVars() {
        return tmps.size()+33;
    }
    
    public Tmp getTmp(int id) {
        return tmps.get(id-33);
    }
    
    public void usePersistent(Reg r) {
        assert r.isUsablePersistent();
        persistentsUsed.add(r);
    }
    
    public Set< Reg > persistentsUsed() {
        return persistentsUsed;
    }
    
    public boolean persistentUsed(Reg r) {
        return persistentsUsed.contains(r);
    }
    
    public LHeader root() { return root; }
    
    public List< LHeader > headersRootFirst() {
        ArrayList< LHeader > result=new ArrayList< LHeader >();
        result.add(root());
        for (LHeader h : headers()) {
            if (h!=root()) {
                result.add(h);
            }
        }
        return result;
    }
    
    /** like headers(), but allows you to append to the list of headers
	as you iterate over it.  this is great if you're doing a transformation
	that involves sometimes splitting headers.  it's also great if you
	want to remove stuff from the list of headers the "good way" - that is,
	in a way where you don't care about the resulting order. */
    public Iterable< LHeader > headers2() {
	return new Iterable< LHeader >() {
	    public Iterator< LHeader > iterator() {
		return new Iterator< LHeader >() {
		    int i = 0;
		    public boolean hasNext() {
			return i<headers.size();
		    }
		    public LHeader next() {
			if (i>=headers.size()) {
			    throw new NoSuchElementException();
			}
			return headers.get(i++);
		    }
                    // NOTE: this operation breaks order.  that's fine most of the
                    // time but you should be aware of it.
		    public void remove() {
			if (i<1) {
			    throw new NoSuchElementException();
			}
			i--;
			headers.set(i,headers.get(headers.size()-1));
			headers.remove(headers.size()-1);
		    }
		};
	    }
	};
    }
    
    /** like headers2(), but in reverse */
    public Iterable< LHeader > headers2Reverse() {
	return new Iterable< LHeader >() {
	    public Iterator< LHeader > iterator() {
		return new Iterator< LHeader >() {
		    int i = headers.size()-1;
		    public boolean hasNext() {
			return i>=0;
		    }
		    public LHeader next() {
			if (i<0) {
			    throw new NoSuchElementException();
			}
			return headers.get(i--);
		    }
		    public void remove() {
                        // too lazy to implement a method I won't use. ;-)
                        throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    /** like headers(), but returns a snapshot, so that if you change the
	list of headers, this will be unaffected. */
    public ArrayList< LHeader > headers3() {
	return new ArrayList< LHeader >(headers);
    }
    
    public ArrayList< LHeader > depthFirstHeaders() {
        MyStack< LHeader > fastWorklist=new MyStack< LHeader >();
        MyStack< LHeader > slowWorklist=new MyStack< LHeader >();
        HashSet< LHeader > seen=new HashSet< LHeader >();
        ArrayList< LHeader > list=new ArrayList< LHeader >();
        fastWorklist.push(root);
        seen.add(root);
        while (!fastWorklist.empty() || !slowWorklist.empty()) {
            LHeader h;
            if (!fastWorklist.empty()) {
                h=fastWorklist.pop();
            } else {
                h=slowWorklist.pop();
            }
            list.add(h);
            LHeader[] theSuccessors=h.successors();
            LHeader[] successors=new LHeader[theSuccessors.length];
            System.arraycopy(theSuccessors,0,
                             successors,0,
                             successors.length);
            Arrays.sort(successors,LHeader.FREQUENCY_COMPARATOR);
            for (int i=successors.length;i-->0;) {
                LHeader h2=successors[i];
                if (seen.add(h2)) {
                    if (h2.likely()) {
                        fastWorklist.push(h2);
                    } else {
                        slowWorklist.push(h2);
                    }
                }
            }
        }
        return list;
    }
    
    public ArrayList< Tmp > tmps3() {
	return new ArrayList< Tmp >(tmps);
    }
    
    int headerCnt=1;

    public LHeader addHeader() {
        LHeader h=new LHeader(this);
        if (h.order==-1) {
            h.order=headerCnt++;
        }
        headers.add(h);
        return h;
    }
    
    public LHeader setRoot(LHeader h) {
        return root=h;
    }
    
    public LHeader reroot() {
        LHeader oldRoot=root;
        setRoot(addHeader());
        root().setFooter(
            new LFooter(
                LOpCode.Jump,LType.Void,
                LArg.EMPTY,new LHeader[]{oldRoot}));
        return root();
    }
    
    public Tmp addTmp(Kind kind,boolean spillable) {
        assert !registersAllocated;
        Tmp t=new Tmp(tmps.size()+33,++tmpIds,kind,spillable);
        tmps.add(t);
        return t;
    }
    
    public Tmp addTmp(Kind kind) {
        return addTmp(kind,true);
    }
    
    public Tmp addNoSpillTmp(Kind kind) {
        return addTmp(kind,false);
    }
    
    public void delTmp(Tmp t) {
        assert tmps.get(t.id()-33)==t;
        Tmp t2=tmps.get(tmps.size()-1);
        t2.id=t.id();
        tmps.set(t.id()-33,t2);
        tmps.remove(tmps.size()-1);
    }
    
    public void delAllTmps() {
        tmps.clear();
    }
    
    public ScratchSlot addScratch(int size) {
        ScratchSlot ss=new ScratchSlot(scratches.size(),size);
        scratches.add(ss);
        return ss;
    }
    
    public void recomputeOrder() {
	if (root.order==-1) {
	    root.order=headerCnt++;
	}
	for (LHeader h : headers) {
            assert h.order>=0;
	    int cnt=1;
	    for (LOp o : h.operations()) {
		o.order=cnt++;
	    }
	}
    }
    
    LPredsCalc preds=null;
    public LPredsCalc getPreds() {
        if (preds==null) {
            preds=new LPredsCalc(this);
        }
        return preds;
    }
    public void killPreds() {
        preds=null;
    }
    
    TrivialUseDef tud=null;
    public TrivialUseDef getTrivialUseDef() {
        if (tud==null) {
            tud=new TrivialUseDef(this);
        }
        return tud;
    }
    public void killTrivialUseDef() {
        tud=null;
    }

    LLivenessCalc liveness=null;
    public LLivenessCalc getLiveness() {
        if (liveness==null) {
            liveness=new LLivenessCalc(this);
        }
        return liveness;
    }
    public void killLiveness() {
        liveness=null;
    }
    
    LiveAsCalc liveAs=null;
    public LiveAsCalc getLiveAs() {
        if (liveAs==null) {
            liveAs=new LiveAsCalc(this);
        }
        return liveAs;
    }
    public void killLiveAs() {
        liveAs=null;
    }
    
    ScratchLivenessCalc scratchLiveness=null;
    public ScratchLivenessCalc getScratchLiveness() {
        if (scratchLiveness==null) {
            scratchLiveness=new ScratchLivenessCalc(this);
        }
        return scratchLiveness;
    }
    public void killScratchLiveness() {
        scratchLiveness=null;
    }
    
    public void killIntraBlockAnalyses() {
        killTrivialUseDef();
        killLiveness();
        killLiveAs();
        killScratchLiveness();
    }
    
    public void killAllAnalyses() {
        killPreds();
        killIntraBlockAnalyses();
    }

    public void verify(String fromWhere) {
        try {
            if (Global.runSanityCheck) new LSanityCheck(this).visitCode();
        } catch (Throwable e) {
            throw new CompilerException("Verification failed in: "+fromWhere,e);
        }
    }
    
    public String toString() {
        return "LCode["+cname+", "+origin()+"]";
    }
}


