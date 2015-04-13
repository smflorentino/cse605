/*
 * Code.java
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
import java.nio.*;

import com.fiji.util.MyStack;
import com.fiji.fivm.ReturnMode;

/**
 * Representation of Fiji IR code.  This will have one or more basic blocks, zero or
 * more variables, zero or more exception handler blocks, zero or more C locals (used
 * for static stack allocation), and both C and Java type signatures.  A code may
 * originate from a Java mehod directly (it's the Fiji IR for some Java method's
 * bytecode), indirectly (it's synthetic Fiji IR generated as a helper for some
 * actual Java method), or not at all (it's synthetic code that has nothing at all
 * to do with any Java methods).
 * <p>
 * Fiji IR code may be swapped to disk at any time -- this allows for a
 * low-memory-overhead approach to whole-program compilation.  This feature is meant
 * to be somewhat fast, but even if it were wire-speed, it would not be fast enough to
 * be used all the time.  Use it with care and only when you absolutely need to.
 * <p>
 * Fiji IR code will have a variety of lazily-computed and eagerly-destroyed analyses.
 * This includes even essential things like predecessor information.  Modifications to
 * the code do not automatically destroy analyses that were invalidated by the
 * modifications.  Each phase of the compiler will thus take care to destroy any
 * analyses that it invalidated.  If in doubt, just call killAllAnalyses() any time
 * you make a change.  Indeed, many of the compiler phases in this system just call
 * that method out of some combination of intellectual laziness and extreme paranoia.
 */
public class Code
    implements Contextable, Callable, NioWritable, NioReadable {
    
    CodeOrigin origin;
    
    Type result;
    Type[] params;
    String cname;
    Basetype cresult;
    
    /** The parameters according to C.  This has calling convention stuff prepended
        to it.  As the code gets lowered, we switch from using the high-level Java
        arguments (via GetArg, to using the C arguments via GetCVar. */
    Basetype[] cparams;
    
    boolean checksInserted;
    boolean hasPhi;
    boolean isSSA;
    
    /** True if we've broken types.  This indicates that we may no longer be
	able to prove certain properties about the code, and therefore should
	not attempt to. */
    boolean typeLowered = false;
    
    boolean pollchecksInserted = false;
    
    /** True if the method is not passed a thread state but instead must get
	it on its own.  Only relevant for the thread-state-passing-style.
        NOTE: this is currently never used and is not supported, but I'm
        leaving it here because I might want to resurrect it... */
    boolean getsOwnThreadState;
    
    /** True if this code may be called directly from native code. */
    boolean mayBeCalledFromNative;
    
    private VerifiabilityMode verifiabilityMode = VerifiabilityMode.VERIFIABLE;
    private VerifiabilityReport verifiabilityReport = new VerifiabilityReport();

    ArrayList< Header > headers=
	new ArrayList< Header >();
    LinkedList< ExceptionHandler > handlers=
	new LinkedList< ExceptionHandler >();
    HashSet< String > localNames=
	new HashSet< String >();
    LinkedList< CLocal > locals=
	new LinkedList< CLocal >();
    
    // NB: weird things may happen if you inline a method into itself
    // NB2: never access directly unless you really know what you're doing
    // NB3: this has the property that vars.get(var.id)==var
    private ArrayList< Var > vars=new ArrayList< Var >();
    
    Header root;
    
    private int varIds;
    
    public Code(CodeOrigin origin,
		Type result,
		Type[] params,
		Basetype cresult,
		Basetype[] cparams,
		String cname) {
	this.origin=origin;
	this.result=result;
	this.params=params;
	this.cname=cname;
	this.cresult=cresult;
	this.cparams=cparams;
    }
    
    public Code(CodeOrigin origin,
		Type result,
		Type[] params,
		String cname) {
	this(origin,result,params,
	     Global.makeResult(result),
	     Global.makeParams(origin.origin().getTypeSignature(),params),
	     cname);
    }
    
    public Code(VisibleMethod method,
		PollcheckMode pollcheck,
		UnsafeMode unsafe,
		SafetyCheckMode safetyChecks,
                ReturnMode returnMode,
                StackOverflowCheckMode stackOverflowCheckMode) {
	this(new CodeOrigin(method.getClazz(),
			    method,
			    method,
			    pollcheck,
			    unsafe,
			    safetyChecks,
                            returnMode,
                            stackOverflowCheckMode,
                            null),
	     method.getType(),
	     method.getAllParams(),
	     method.asRemoteFunction().getName());
    }
    
    public Code(VisibleMethod method) {
	this(method,
	     method.pollcheck(),
	     method.unsafe(),
	     method.safetyChecks(),
             method.returnMode(),
             method.stackOverflowCheckMode());
    }
    
    Code(NioRead r) {}
    
    public Context getContext() { return origin.owner().getContext(); }
    
    public VisibleClass getOwner() { return origin.owner(); }
    
    /** Returns the method whose code this is.  May be null, if this is a
	synthetic method (like for export glue). */
    public VisibleMethod method() { return origin.method(); }

    public CodeOrigin origin() { return origin; }
    
    public boolean mustOptForSize() {
        return origin().origin().isInitializer()
            && origin().origin().isStatic();
    }
    
    // callable methods - currently forward to origin, but we may change that
    // if we start generating code out of thin air
    public SideEffectMode sideEffect() { return origin.sideEffect(); }
    public SafepointMode safepoint() { return origin.safepoint(); }
    public PollcheckMode pollcheck() { return origin.pollcheck(); }
    public ReturnMode returnMode() { return origin.returnMode(); }
    
    public AllocationMechanism alloc() { return origin().origin().alloc(); }
    
    public boolean changeVerifiability(VerifiabilityMode mode,
                                       String reason) {
        assert mode!=VerifiabilityMode.VERIFIABLE;
        if (mode.lessVerifiable(verifiabilityMode)) {
            verifiabilityReport=new VerifiabilityReport();
            verifiabilityReport.add(reason);
            verifiabilityMode=mode;
            return true;
        } else if (mode==verifiabilityMode) {
            return verifiabilityReport.add(reason);
        } else {
            return false;
        }
    }
    
    public boolean simpleCallingConvention() {
        return alloc()==AllocationMechanism.DEFAULT_ALLOC;
    }
    
    public boolean isPotentiallyVerifiable() {
        return verifiabilityMode!=VerifiabilityMode.NOT_VERIFIABLE;
    }
    
    public void reportVerifiability() {
        if (verifiabilityMode==VerifiabilityMode.VERIFIABLE) {
            assert verifiabilityReport.isEmpty() : verifiabilityReport;
            VerifiabilityRepo.put(this,verifiabilityMode,null);
        } else {
            VerifiabilityRepo.put(this,verifiabilityMode,verifiabilityReport);
        }
    }
    
    public Type result() { return result; }
    public Type[] params() { return params; }
    public Type param(int idx) { return params[idx]; }
    
    private PTSet[] paramSets=null;
    
    public PTSet[] paramSets() {
	if (paramSets==null) {
	    if (method()!=null) {
		paramSets=Global.analysis().paramSetForExec(method());
	    } else {
		paramSets=PTSet.bottomArray(params.length);
		for (int i=0;i<params.length;++i) {
		    if (params[i].isObject()) {
			paramSets[i]=new TypeBound(params[i].asExectype().asType());
		    }
		}
	    }
	}
	return paramSets;
    }
    
    public PTSet paramSet(int i) {
	return paramSets()[i];
    }
    
    private ObjectLocation[] paramLocs=null;
    
    public ObjectLocation[] paramLocs() {
	if (paramLocs==null) {
	    if (method()!=null) {
		paramLocs=Global.ola.paramSetForExec(method());
	    } else {
		paramLocs=ObjectLocation.topArray(params.length);
	    }
	}
	return paramLocs;
    }
    
    public ObjectLocation paramLoc(int i) {
	return paramLocs()[i];
    }
    
    public String cname() { return cname; }
    public Basetype cresult() { return cresult; }
    public Basetype[] cparams() { return cparams; }
    public Basetype cparam(int i) { return cparams[i]; }
    
    public RemoteFunction asRemoteFunction() {
        return new RemoteFunction(cname(),
                                  cresult(),
                                  cparams(),
                                  this);
    }
    
    public List< Header > headers() { return headers; }
    public List< CLocal > locals() { return locals; }
    
    /** returns the list of variables.  treat this list as if it were
        immutable. */
    public ArrayList< Var > vars() { return vars; }
    
    public VarSet varSet() {
        VarSet result=new VarSet(this);
        result.addAll(vars());
        return result;
    }
    
    public Var getVar(int id) {
        if (id==-1) {
            return Var.VOID;
        } else {
            return vars.get(id);
        }
    }
    
    public Header root() { return root; }
    
    public List< Header > headersRootFirst() {
	LinkedList< Header > result=new LinkedList< Header >();
	result.add(root());
	for (Header h : headers()) {
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
    public Iterable< Header > headers2() {
	return new Iterable< Header >() {
	    public Iterator< Header > iterator() {
		return new Iterator< Header >() {
		    int i = 0;
		    public boolean hasNext() {
			return i<headers.size();
		    }
		    public Header next() {
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
    public Iterable< Header > headers2Reverse() {
	return new Iterable< Header >() {
	    public Iterator< Header > iterator() {
		return new Iterator< Header >() {
		    int i = headers.size()-1;
		    public boolean hasNext() {
			return i>=0;
		    }
		    public Header next() {
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
    public ArrayList< Header > headers3() {
	return new ArrayList< Header >(headers);
    }
    
    public ArrayList< Var > vars3() {
	return new ArrayList< Var >(vars);
    }
    
    public Header[] getHeadersByOrder() {
	int maxIdx=0;
	for (Header h : headers) {
	    maxIdx=Math.max(h.order,maxIdx);
	}
	Header[] result=new Header[maxIdx+1];
	for (Header h : headers) {
	    result[h.order]=h;
	}
	return result;
    }

    public List< ExceptionHandler > handlers() { return handlers; }
    public Iterable< Header > handlerHeaders() {
        return new Iterable< Header >() {
            public Iterator< Header > iterator() {
                return new Iterator< Header >() {
                    Iterator< ExceptionHandler > i=handlers.iterator();
                    public boolean hasNext() {
                        return i.hasNext();
                    }
                    public Header next() {
                        return i.next().target();
                    }
                    public void remove() {
                        i.remove();
                    }
                };
            }
        };
    }
    
    public boolean hasPhi() { return hasPhi; }
    public boolean isSSA() { return isSSA; }
    
    public boolean getsOwnThreadState() { return getsOwnThreadState; }

    // first add headers, then add handlers
    
    public ExceptionHandler addHandler(DebugInfo di,
				       VisibleClass handles,
				       ExceptionHandler dropsTo,
				       Header target) {
	ExceptionHandler eh=
	    new ExceptionHandler(di,
				 handles,
				 dropsTo,
				 target);
	handlers.add(eh);
	return eh;
    }
    
    public Header addHeader(DebugInfo di) {
	Header h=new Header(di,this);
        if (h.order==-1) {
            h.order=headerCnt++;
        }
	headers.add(h);
	if (Global.verbosity>=8) {
	    Global.log.println("Creating new block: "+h);
	}
	return h;
    }
    
    public Header setRoot(Header h) {
	return root=h;
    }
    
    /**
     * Create a new root block which jumps to the old one.  The new root
     * block will not have any of the properties of the old one (i.e. no
     * exception handlers).  Returns the new root block; the old one can
     * be retrieved using root.getFooter().defaultSuccessor().
     */
    public Header reroot() {
	Header oldRoot=root;
	setRoot(addHeader(oldRoot.di()));
	root().setFooter(
	    new Jump(oldRoot.di(),oldRoot));
	return root();
    }
    
    public void addVar(Var v) {
        v.id=vars.size();
        v.permID=++varIds;
        v.code=this;
        vars.add(v);
        assert v.id==vars.size()-1;
    }
    
    public Var addVar(Exectype t) {
	assert t!=Exectype.VOID;
	Var v=new Var(this,t);
	addVar(v);
	return v;
    }
    
    public Var addVar(Exectype t,Exectype origType) {
	assert t!=Exectype.VOID;
        assert origType!=Exectype.VOID;
        assert origType!=null;
        Var v=new Var(this,t,origType);
        addVar(v);
        return v;
    }
    
    public Var addVar() {
	return addVar(Exectype.BOTTOM);
    }
    
    public void addVars(Iterable< Var > vars) {
        for (Var v : vars) {
            addVar(v);
        }
    }
    
    /**
     * Deletes a variable; if you write code that calls this method, you almost
     * certainly did something very, very wrong.  This method should only be
     * called from KillDead, which together with other parts of the simplification
     * fixpoint effectively "garbage collect" dead variables.  Thus you should
     * never have to call this method.  Moreover, calling this method may lead
     * to horrible badness since calling this method leads to variable
     * renumbering and many analyses rely on variable numbers being static.
     * So for ef's sake, don't call this method.  
     * @param v The variable to delete.  Don't call this method, seriously.
     */
    public void delVar(Var v) {
        Var v2=vars.get(vars.size()-1);
        v2.id=v.id;
        vars.set(v.id,v2);
        vars.remove(vars.size()-1);
    }
    
    public void addLocal(CLocal local) {
	assert !localNames.contains(local.getName());
	locals.add(local);
	localNames.add(local.getName());
    }
    
    public void addLocalWithUniqueName(CLocal local) {
	local.name=Util.uniqueifyName(local.name,localNames);
	addLocal(local);
    }
    
    public void verify(String fromWhere) {
        try {
            if (Global.runSanityCheck) new SanityCheck(this).visitCode();
            if (Global.runWriteReadTest) {
                writeReadTest();
                new SanityCheck(this).visitCode();
            }
        } catch (Throwable e) {
            throw new CompilerException("Verification failed in: "+fromWhere,e);
        }
    }
    
    // always just assume that order is not computed, and call this if
    // you need it computed.  (this is the most sensible strategy since
    // recomputing order is so darn quick.)
    // convention: if we want to number the points *between* operations,
    // use 0 to mean before the beginning of the BB, getFooter().order to mean
    // after the end of the BB, and in general:
    // o.order-1   --> before operation o
    // o.order     --> after operation o
    // this works because o.order for operations is 1-based rather than 0-based.
    int headerCnt=1;
    int handlerCnt=1;
    public void recomputeOrder() {
	if (root.order==-1) {
	    root.order=headerCnt++;
	}
	for (Header h : headers) {
	    if (h.order==-1) {
		h.order=headerCnt++;
	    }
	    int cnt=1;
	    for (Operation i : h.operations()) {
		i.order=cnt++;
	    }
	}
	for (ExceptionHandler eh : handlers) {
	    if (eh.order==-1) {
		eh.order=handlerCnt++;
	    }
	}
    }
    
    public int computeTotalOrder() {
        int cnt=0;
        for (Header h : depthFirstHeadersLikely()) {
            for (Operation o : h.operations()) {
                o.order=++cnt;
            }
        }
        return cnt;
    }
    
    private void dfsPropLikely(MyStack< Header > worklist,
			       HashSet< Header > seen,
                               ArrayList< Header > list) {
	while (!worklist.empty()) {
	    Header h=worklist.pop();
            list.add(h);
            for (Header h2 : h.likelySuccessors()) {
                if (seen.add(h2)) {
                    worklist.push(h2);
                }
            }
	}
    }
    
    public ArrayList< Header > likelyHeaders() {
	MyStack< Header > worklist=new MyStack< Header >();
	HashSet< Header > seen=new HashSet< Header >();
        ArrayList< Header > list=new ArrayList< Header >();
	worklist.push(root);
	seen.add(root);
	dfsPropLikely(worklist,seen,list);
        assert list.size()<=headers.size();
        return list;
    }
    
    public ArrayList< Header > depthFirstHeadersLikely() {
	MyStack< Header > worklist=new MyStack< Header >();
	HashSet< Header > seen=new HashSet< Header >();
        ArrayList< Header > list=new ArrayList< Header >();
	worklist.push(root);
	seen.add(root);
	dfsPropLikely(worklist,seen,list);
	for (Header h : headers()) {
	    if (!seen.contains(h)) {
		list.add(h);
	    }
	}
	assert list.size()==headers.size();
	return list;
    }
    
    public void depthFirstSortLikely() {
	headers=depthFirstHeadersLikely();
    }
    
    public ArrayList< Header > depthFirstHeaders() {
	MyStack< Header > worklist=new MyStack< Header >();
	HashSet< Header > seen=new HashSet< Header >();
        ArrayList< Header > list=new ArrayList< Header >();
	worklist.push(root);
        seen.add(root);
        while (!worklist.empty()) {
            Header h=worklist.pop();
            list.add(h);
            for (Header h2 : h.allSuccessors()) {
                if (seen.add(h2)) {
                    worklist.push(h2);
                }
            }
        }
        assert list.size()==headers.size();
        return list;
    }
    
    public void depthFirstSort() {
        headers=depthFirstHeaders();
    }

    // FIXME: this code is still boned.
    
    static class POHHeader {
        LinkedHashSet< POHHeader > in=new LinkedHashSet< POHHeader >();
        LinkedHashSet< POHHeader > out=new LinkedHashSet< POHHeader >();
        
        Header h;
        
        POHHeader(Header h) {
            this.h=h;
        }
        
        public String toString() {
            return h.toString();
        }
    }
    
    static class POHEdge {
        Header from;
        Header to;
        
        POHEdge(Header from,
                Header to) {
            this.from=from;
            this.to=to;
        }
        
        public int hashCode() {
            return from.hashCode()+3*to.hashCode();
        }
        
        public boolean equals(Object other_) {
            if (this==other_) return true;
            if (!(other_ instanceof POHEdge)) return false;
            POHEdge other=(POHEdge)other_;
            return from==other.from
                && to==other.to;
        }
        
        public String toString() {
            return ""+from+"->"+to;
        }
    }
    
    class POHTopo {
        LinkedHashSet< POHHeader > list=new LinkedHashSet< POHHeader >();
        ArrayList< POHHeader > schedule=new ArrayList< POHHeader >();
        ArrayList< POHEdge > exclude;
        
        POHTopo() {
        }
        
        void doit() {
            MyStack< POHHeader > worklist=new MyStack< POHHeader >();
            LinkedHashSet< POHHeader > seen=new LinkedHashSet< POHHeader >();
            
            for (POHHeader h : list) {
                if (h.in.isEmpty()) {
                    seen.add(h);
                    worklist.push(h);
                }
            }
            
            while (!worklist.empty()) {
                POHHeader h=worklist.pop();
                schedule.add(h);
                list.remove(h);
                for (POHHeader h2 : h.out) {
                    seen.add(h2);
                    if (h2.in.remove(h)) {
                        if (h2.in.isEmpty()) {
                            worklist.push(h2);
                        }
                    }
                }
            }
            if (list.isEmpty()) {
                return;
            } else {
                if (Global.verbosity>=1) {
                    Global.log.println("list = "+list+", seen = "+seen+", schedule = "+schedule);
                }
                
                // we're interested in nodes that have been seen but have not been scheduled
                seen.retainAll(list);
                
                assert !seen.isEmpty();
                
                if (Global.verbosity>=1) {
                    Global.log.println("unaccounted = "+seen);
                }
                
                // FIXME: sort the seen set by index in the schedule that we have
                // come up with so far.
                
                for (POHHeader h : seen) {
                    if (Global.verbosity>=1) {
                        Global.log.println("picked "+h+" with in = "+h.in);
                    }
                    
                    assert list.contains(h);
                    
                    exclude=new ArrayList< POHEdge >();
                    
                    for (POHHeader h2 : h.in) {
                        assert list.contains(h2);
                        assert h2.out.contains(h);
                        exclude.add(new POHEdge(h2.h,h.h));
                    }
                    
                    return;
                }
            }
            
            throw new Error("should not get here");
        }
    }
    
    public ArrayList< Header > programOrderHeaders() {
        // this code is obscene.
        
        try {
            POHTopo topo=null;
            HashSet< POHEdge > exclude=new HashSet< POHEdge >();
            
            for (;;) {
                HashMap< Header, POHHeader > map=new HashMap< Header, POHHeader >();
                
                for (Header h : headers()) {
                    map.put(h,new POHHeader(h));
                }
                
                for (Header h : headers()) {
                    for (Header h2 : h.likelySuccessors()) {
                        map.get(h).out.add(map.get(h2));
                        map.get(h2).in.add(map.get(h));
                    }
                }
                
                for (POHEdge e : exclude) {
                    if (Global.verbosity>=1) {
                        Global.log.println("excluding "+e);
                    }
                    boolean result=map.get(e.from).out.remove(map.get(e.to));
                    assert result;
                    result=map.get(e.to).in.remove(map.get(e.from));
                    assert result;
                }
                
                topo=new POHTopo();
                topo.list.addAll(map.values());
                topo.doit();
                
                if (topo.exclude==null) {
                    break;
                } else {
                    exclude.addAll(topo.exclude);
                }
            }
            
            HashSet< Header > isLikely=new HashSet< Header >(likelyHeaders());
            ArrayList< Header > likelies=new ArrayList< Header >();
            ArrayList< Header > unlikelies=new ArrayList< Header >();
            for (POHHeader poh : topo.schedule) {
                if (isLikely.contains(poh.h)) {
                    likelies.add(poh.h);
                } else {
                    unlikelies.add(poh.h);
                }
            }
            
            likelies.addAll(unlikelies);
            ArrayList< Header > result=likelies;
            
            if (result.size()!=headers.size()) {
                StringBuilder buf=new StringBuilder();
                buf.append("invalid result = "+result);
                
                HashSet< Header > set=new HashSet< Header >();
                ArrayList< Header > dups=new ArrayList< Header >();
                for (Header h : result) {
                    if (!set.add(h)) {
                        dups.add(h);
                    }
                }
                if (!dups.isEmpty()) {
                    buf.append("; duplicates = "+dups);
                }
                
                ArrayList< Header > missing=new ArrayList< Header >();
                for (Header h : headers) {
                    if (!set.contains(h)) {
                        missing.add(h);
                    }
                }
                if (!missing.isEmpty()) {
                    buf.append("; missing = "+missing);
                }
                
                throw new CompilerException(buf.toString());
            }
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            CodeDumper.dump(this,Global.log);
            throw new CompilerException("failed to compute program order of headers",e);
        }
    }
    
    public void programOrderSort() {
        headers=programOrderHeaders();
    }
    
    ThrowsCalc tc;
    SideEffectCalc sec;
    ObservableCalc oc;
    SafepointCalc spc;
    
    public ThrowsCalc getThrows() {
	if (tc==null) {
	    tc=new ThrowsCalc(this);
	}
	return tc;
    }
    
    public SideEffectCalc getSideEffects() {
	if (sec==null) {
	    sec=new SideEffectCalc(this);
	}
	return sec;
    }
    
    public ObservableCalc getObservables() {
	if (oc==null) {
	    oc=new ObservableCalc(this);
	}
	return oc;
    }
    
    public SafepointCalc getSafepoints() {
        if (spc==null) {
            spc=new SafepointCalc(this);
        }
        return spc;
    }
    
    PredsCalc predsCalc;
    public PredsCalc getPreds() {
	if (predsCalc==null) {
	    predsCalc=new PredsCalc(this);
	}
	return predsCalc;
    }
    public void killPreds() {
	predsCalc=null;
    }
    
    HeaderProximityCalc headerProximityCalc;
    public HeaderProximityCalc getHeaderProximity() {
        if (headerProximityCalc==null) {
            headerProximityCalc=new HeaderProximityCalc(this);
        }
        return headerProximityCalc;
    }
    public void killHeaderProximity() {
        headerProximityCalc=null;
    }
    
    ThrowingHeadersCalc throwingHeadersCalc;
    public ThrowingHeadersCalc getThrowingHeaders() {
        if (throwingHeadersCalc==null) {
            throwingHeadersCalc=new ThrowingHeadersCalc(this);
        }
        return throwingHeadersCalc;
    }
    public void killThrowingHeaders() {
        throwingHeadersCalc=null;
    }
    
    LoopCalc loopCalc;
    public LoopCalc getLoops() {
        if (loopCalc==null) {
            loopCalc=new LoopCalc(this);
        }
        return loopCalc;
    }
    public void killLoops() {
        loopCalc=null;
    }
    
    ResultTypeCalc resultTypeCalc;
    public ResultTypeCalc getResultType() {
	if (resultTypeCalc==null) {
	    resultTypeCalc=new ResultTypeCalc(this);
	}
	return resultTypeCalc;
    }
    public void killResultType() {
	resultTypeCalc=null;
    }
    
    BoolResultCalc boolResultCalc;
    public BoolResultCalc getBoolResult() {
	if (boolResultCalc==null) {
	    boolResultCalc=new BoolResultCalc(this);
	}
	return boolResultCalc;
    }
    public void killBoolResult() {
	boolResultCalc=null;
    }
    
    SimpleLivenessCalc simpleLivenessCalc;
    public SimpleLivenessCalc getSimpleLiveness() {
	if (simpleLivenessCalc==null) {
	    simpleLivenessCalc=new SimpleLivenessCalc(this);
	}
	return simpleLivenessCalc;
    }
    public void killSimpleLiveness() {
	simpleLivenessCalc=null;
    }
    
    SideCrossCalc sideCrossCalc;
    public SideCrossCalc getSideCross() {
        if (sideCrossCalc==null) {
            sideCrossCalc=new SideCrossCalc(this);
        }
        return sideCrossCalc;
    }
    public void killSideCross() {
        sideCrossCalc=null;
    }
    
    IntraproceduralMustAliasCalc localMustAliasCalc;
    public IntraproceduralMustAliasCalc getLocalMustAlias() {
	if (localMustAliasCalc==null) {
	    localMustAliasCalc=new IntraproceduralMustAliasCalc(this);
	}
	return localMustAliasCalc;
    }
    public void killLocalMustAlias() {
	if (localMustAliasCalc!=null) {
	    localMustAliasCalc=null;
	}
    }
    
    MonitorUseCalc monitorUseCalc;
    public MonitorUseCalc getMonitorUse() {
	if (monitorUseCalc==null) {
	    monitorUseCalc=new MonitorUseCalc(this);
	}
	return monitorUseCalc;
    }
    public void killMonitorUse() {
	monitorUseCalc=null;
    }
    
    RefsLiveAtSafe refsLiveAtSafe;
    public boolean hasRefsLiveAtSafe() { return refsLiveAtSafe!=null; }
    public RefsLiveAtSafe getRefsLiveAtSafe() {
	if (refsLiveAtSafe==null) {
	    refsLiveAtSafe=new RefsLiveAtSafe(this);
	}
	return refsLiveAtSafe;
    }
    public void killRefsLiveAtSafe() {
	refsLiveAtSafe=null;
    }
    
    
    RefAlloc refAlloc;
    public boolean hasRefAlloc() { return refAlloc!=null; }
    public RefAlloc getRefAlloc() {
	if (refAlloc==null) {
	    refAlloc=new RefAlloc(this);
	}
	return refAlloc;
    }
    public void killRefAlloc() {
	refAlloc=null;
    }
    
    DominatorCalc dominatorCalc;
    public DominatorCalc getDominators() {
	if (dominatorCalc==null) {
	    dominatorCalc=new DominatorCalc(this);
	}
	return dominatorCalc;
    }
    public void killDominators() {
	dominatorCalc=null;
    }
    
    NormalDominatorCalc normalDominatorCalc;
    public NormalDominatorCalc getNormalDominators() {
	if (normalDominatorCalc==null) {
	    normalDominatorCalc=new NormalDominatorCalc(this);
	}
	return normalDominatorCalc;
    }
    public void killNormalDominators() {
	normalDominatorCalc=null;
    }
    
    NormalPreDominatorCalc normalPreDominatorCalc;
    public NormalPreDominatorCalc getNormalPreDominators() {
	if (normalPreDominatorCalc==null) {
	    normalPreDominatorCalc=new NormalPreDominatorCalc(this);
	}
	return normalPreDominatorCalc;
    }
    public void killNormalPreDominators() {
	normalPreDominatorCalc=null;
    }
    
    IDomCalc iDomCalc;
    public IDomCalc getIDoms() {
	if (iDomCalc==null) {
	    iDomCalc=new IDomCalc(this);
	}
	return iDomCalc;
    }
    public void killIDoms() {
	iDomCalc=null;
    }
    
    DomFrontCalc domFrontCalc;
    public DomFrontCalc getDomFront() {
	if (domFrontCalc==null) {
	    domFrontCalc=new DomFrontCalc(this);
	}
	return domFrontCalc;
    }
    public void killDomFront() {
	domFrontCalc=null;
    }
    
    AssignCalc assignCalc;
    public AssignCalc getAssigns() {
	if (assignCalc==null) {
	    assignCalc=new AssignCalc(this);
	}
	return assignCalc;
    }
    public void killAssigns() {
	assignCalc=null;
    }
    
    UseCalc useCalc;
    public UseCalc getUses() {
        if (useCalc==null) {
            useCalc=new UseCalc(this);
        }
        return useCalc;
    }
    public void killUses() {
        useCalc=null;
    }
    
    RankCalc rankCalc;
    public RankCalc getRanks() {
        if (rankCalc==null) {
            rankCalc=new RankCalc(this);
        }
        return rankCalc;
    }
    public void killRanks() {
        rankCalc=null;
    }
    
    MustInlineCalc mustInlineCalc;
    public MustInlineCalc getMustInline() {
	if (mustInlineCalc==null) {
	    mustInlineCalc=new MustInlineCalc(this);
	}
	return mustInlineCalc;
    }
    public void killMustInline() {
	mustInlineCalc=null;
    }
    
    LeafMethodAnalysis leafMethodAna;
    public LeafMethodAnalysis getLeafMethodAna() {
        if (leafMethodAna==null) {
            leafMethodAna=new LeafMethodAnalysis(this);
        }
        return leafMethodAna;
    }
    public void killLeafMethodAna() {
        leafMethodAna=null;
    }
    
    public void killAllAnalyses() {
	killPreds();
        killHeaderProximity();
        killLoops();
        killRanks();
	killDominators();
	killNormalDominators();
	killNormalPreDominators();
	killIDoms();
	killDomFront();
        killIntraBlockAnalyses();
    }
    
    public void killIntraBlockAnalyses() {
	killSimpleLiveness();
	killLocalMustAlias();
	killAssigns();
        killUses();
	killMustInline();
        killLeafMethodAna();
        killSideCross();
        killThrowingHeaders();
    }
    
    public Code copy() {
	assert root!=null;
	// clone all headers, exception handlers, operations, locals, and vars.
	Code result=new Code(origin,this.result,params,cname);
        result.varIds=varIds;
	result.checksInserted=checksInserted;
	result.isSSA=isSSA;
	result.hasPhi=hasPhi;
	result.getsOwnThreadState=getsOwnThreadState;
	result.cresult=cresult;
	result.cparams=cparams;
	result.mustInlineCalc=mustInlineCalc;
	result.headerCnt=headerCnt;
	result.handlerCnt=handlerCnt;
        result.pollchecksInserted=pollchecksInserted;
        result.typeLowered=typeLowered;
        result.mayBeCalledFromNative=mayBeCalledFromNative;
        // FIXME: are we copying all of the fields that we should be copying?
	HashMap< Node, Node > nodeMap=new HashMap< Node, Node >();
	HashMap< CLocal, CLocal > localMap=new HashMap< CLocal, CLocal >();
	for (Header h : headers()) {
	    Header h2=(Header)h.copy();
	    h2.code=result;
	    nodeMap.put(h,h2);
	    result.headers.add(h2);
	    for (Operation o : h.operations()) {
		nodeMap.put(o,o.copy());
	    }
	}
	assert nodeMap.containsKey(root);
	for (ExceptionHandler eh : handlers) {
	    ExceptionHandler eh2=(ExceptionHandler)eh.copy();
	    nodeMap.put(eh,eh2);
	    result.handlers.add(eh2);
	}
	for (CLocal l : locals) {
	    CLocal l2=(CLocal)l.copy();
	    localMap.put(l,l2);
	    result.locals.add(l2);
	}
	result.vars.addAll(vars);
	result.root=(Header)nodeMap.get(root);
	MultiEdgeReplacement mer=new MultiEdgeReplacement(nodeMap);
	for (Node n : nodeMap.values()) {
	    n.accept(mer);
	}
	for (Header h : result.headers()) {
	    h.footer=(Footer)nodeMap.get(h.footer);
	    for (Operation o : h.operations()) {
		if (o instanceof CFieldInst) {
		    ((CFieldInst)o).replaceFields(localMap);
		}
	    }
	}
        result.renameAllVars();
        verify("copy, for original");
        result.verify("copy, for copy");
	return result;
    }
    
    public void renameAllVars() {
	ArrayList< Var > newVars=new ArrayList< Var >();
	HashMap< Var, Var > varMap=new HashMap< Var, Var >();
	for (Var v : vars) {
	    Var v2=v.copy();
            v2.code=this;
            assert v.id==v2.id;
            assert v.permID==v2.permID;
	    newVars.add(v2);
            assert newVars.size()==v.id+1;
	    varMap.put(v,v2);
	}
	vars=newVars;
	Gettable< Arg, Var > varRep=Gettable.wrap(varMap);
	for (Header h : headers()) {
	    for (Operation o : h.operations()) {
		o.replaceVars(varRep);
	    }
	}
        verify("renaming all variables");
    }
    
    public HashMap< Header, Header > duplicateForSpecialization() {
        assert !isSSA();
        
        HashMap< Node, Node > map=new HashMap< Node, Node >();
        
        for (Header h : headers()) {
            Header h2=(Header)h.copy();
            h2.order=headerCnt++;
            map.put(h,h2);
            headers.add(h2);
            for (Operation o : h.operations()) {
                map.put(o,o.copy());
            }
        }
        for (ExceptionHandler eh : handlers) {
            ExceptionHandler eh2=(ExceptionHandler)eh.copy();
            map.put(eh,eh2);
            handlers.add(eh2);
        }
        MultiEdgeReplacement mer=new MultiEdgeReplacement(map);
        for (Node n : map.values()) {
            n.accept(mer);
        }
        
        HashMap< Header, Header > result=new HashMap< Header, Header >();
        for (Map.Entry< Node, Node > e : map.entrySet()) {
            Node orig=e.getKey();
            Node copy=e.getValue();
            if (orig instanceof Header) {
                assert copy instanceof Header;
                result.put((Header)orig,(Header)copy);
            }
        }
        
        return result;
    }
    
    /** multiply the code n times.  n=1 is a nop, n<1 is invalid, n=2 is like duplicate. */
    @SuppressWarnings("unchecked")
    public HashMap< Header, ArrayList< Header > > multiplyForSpecialization(int n) {
        assert !isSSA();
        assert n>=1;
        
        HashMap< Header, ArrayList< Header > > result=
            new HashMap< Header, ArrayList< Header > >();
        for (Header h : headers()) {
            result.put(h,new ArrayList< Header >());
        }
        
        if (n==1) {
            return result;
        }
        
        HashMap< Node, Node >[] map=new HashMap[n-1];
        for (int i=0;i<n-1;++i) {
            map[i]=new HashMap< Node, Node >();
        }
        
        for (Header h : headers()) {
            for (int i=0;i<n-1;++i) {
                Header h2=(Header)h.copy();
                h2.order=headerCnt++;
                map[i].put(h,h2);
                headers.add(h2);
                for (Operation o : h.operations()) {
                    map[i].put(o,o.copy());
                }
            }
        }
        for (ExceptionHandler eh : handlers) {
            for (int i=0;i<n-1;++i) {
                ExceptionHandler eh2=(ExceptionHandler)eh.copy();
                map[i].put(eh,eh2);
                handlers.add(eh2);
            }
        }
        for (int i=0;i<n-1;++i) {
            MultiEdgeReplacement mer=new MultiEdgeReplacement(map[i]);
            for (Node node : map[i].values()) {
                node.accept(mer);
            }
        }
        
        for (int i=0;i<n-1;++i) {
            for (Map.Entry< Node, Node > e : map[i].entrySet()) {
                Node orig=e.getKey();
                Node copy=e.getValue();
                if (orig instanceof Header) {
                    assert copy instanceof Header;
                    result.get((Header)orig).add((Header)copy);
                }
            }
        }
        
        return result;
    }

    // NOTE: the following comment block(s) are for historical purposes.
    
    // planned order of compilation:
    //  1) parse
    //  2) intercept intrinsics
    //  3) stupid features (multi new array)
    //  4) copy prop
    //  5) data flow (null check, checkinit, devirt, etc.)
    //  6) inline
    //  7) cfg simplify
    //     (repeat steps 4..7 as necessary)
    //  8) other optimizations (abcd?  maybe abcd should be before inline?)
    //  9) copy prop (and other simplifications here?)
    // 10) debug ID squirting
    // 11) pollcheck insertion
    // 12) ref alloc (leverages pollchecks, must occur before we start producing code
    //     that casts between objects and pointers)
    // 13) exception lower #1
    // 14) features #2 (casts, instanceof, null, array bounds, static init)
    // 15) calling convention #1 (args, invocation)
    // 16) exception lower #2
    // 17) calling convention #1 (args, invocation - for calls made for exception handling)
    // 18) header model (monitors, hash codes, getting the type data)
    // 19) object representation (object model, array length, fields, array access, alloc, barriers, etc)
    // 20) break types
    // 21) frames (Arg.FRAME)
    // 22) calling convention #2 (Arg.THREAD_STATE)
    // 23) copy prop
    // 24) cfg simplify
    // 25) generate (deals only with the C subset of the IR)
    
    // what about native methods?  we should have something akin to the CodeParser
    // that produces code for native methods.
    
    // note for inline candidates: we should look at asserts.  you could have a method
    // with a large assert statement, which is disabled.  without the assert, it might
    // be an inline candidate, but with the assert, it isn't.
    
    // notes:
    // - need copy prop before ref alloc to reduce number of vars
    // - need representational after ref alloc since it does type erasure
    //   and makes the IR generally more confusing (not really ... breaking of
    //   types happens much later.  but we still want ref alloc before for example
    //   features#2 because we don't want to accidentally put a pollcheck inside
    //   a the code for a "feature", which could happen if features#2 ever decided
    //   to generate a loop, which is a distinct possibility.)
    // - it would be good to have a flow-sensitive must-alias analysis
    //   that feeds into the ref allocator.
    // - what about invocation?  if we blow away the types at the point
    //   of invocation, we could be in trouble.  we need to know when
    //   an array becomes an Object!  we could "reveal" this by introducing
    //   new variables, but that seems somewhat dangerous.  (actually,
    //   I really like that approach.  makes sense to me.)
    // - header model should probably be done later.  like, after calling
    //   convention.
    // - we still need to insert pollchecks!
    // - have to have a good story for monitorenter.  it turns out that
    //   you can use it in a loop.
    
    // optimizations we will absolutely need:
    // - null check removal (done)
    // - checkinit removal (done)
    // - trivial exception handler removal (removal of exception handlers that
    //   just rethrow and nothing else)
    // - monitorenter removal (obvious recursive monitorenter) (done)
    
    // additional optimizations:
    // - copy prop (obviously) (done)
    // - data flow of numbers (abstract set: negative, zero, positive) (done)
    // - array bounds check removal (done, I think, a stupid version for
    //   arrays whose size is well-known)
    // - data flow of types and possibly alloc sites (to remove checked casts,
    //   instanceof tests, invokedynamic checks for interface invocation) (done,
    //   mostly; don't have alloc site info)
    // - loop-invariant code motion
    // - load-store optimization
    // - tail merging (done)
    // - small block eliminatin: if you have a jump to a block that is short,
    //   just copy the contents of the block in place of the jump.  "short" is
    //   a heuristic.  on the conservative end, this would mean, any block that
    //   has an empty body and ends in a jump, branch, return, or NotReached.
    //   a more liberal heuristic might be: any block that contains only Mov,
    //   Phi, and Ipsilon statements and ends in a jump, branch, return, NotReached,
    //   or throw.  the justification for labeling a Mov-only block as "short" is
    //   that those Movs are likely to be eliminated if they are hoisted to their
    //   predecessor. (done, I think, with a fairly liberal heuristic)
    
    // analyses that we will need:
    // - field interference (which fields can a method modify?)
    
    // higher-level optimizations that we'll obviously need:
    // - devirtualization (done)
    // - virtualization (done)
    // - inlining (done)
    // - identification of methods that don't throw, or that always throw (done)
    // - identification of methods that don't need thread state
    
    public String toString() {
	String result="Code[";
	if (method()!=null) {
	    result+="Method = "+method()+", ";
	}
	result+="Owner = "+getOwner()+", Result = "+this.result+", Params = ("+
	    Util.dump(params)+")";
	result+=" checksInserted = "+checksInserted;
	result+="]";
	return result;
    }
    
    public String shortName() {
        return origin().shortName();
    }
    
    public static abstract class Visitor {
	public abstract void visit(Code c);
	public void doneVisiting() {}
    }
    
    // IO support
    
    public void forAllNodes(Node.SimpleVisitor v) {
        for (Header h : headers) {
            if (!v.visit(h)) return;
            for (Operation o : h.operations()) {
                if (!v.visit(o)) return;
            }
        }
        for (ExceptionHandler eh : handlers) {
            if (!v.visit(eh)) return;
        }
    }
    
    public void writeTo(ByteBuffer buffer) {
        // order of write:
        // - the random stuff
        // - codeorigins
        // - debuginfos
        // - vars
        // - locals
        // - number of headers (when reading just create blank ones - root comes first)
        // - exceptionhandlers (complete info)
        // - bodies of headers
        
        // notes:
        // - exception handlers and headers are in the same node coding; exception
        //   handlers come after headers
        
        Util.writeString(buffer,cname);
        buffer.putInt(Global.typeCoding.codeFor(result));
        buffer.putInt(params.length);
        for (Type t : params) {
            buffer.putInt(Global.typeCoding.codeFor(t));
        }
        buffer.put((byte)(checksInserted?1:0));
        buffer.put((byte)(isSSA?1:0));
        buffer.put((byte)(hasPhi?1:0));
        buffer.put((byte)(getsOwnThreadState?1:0));
        buffer.put((byte)(typeLowered?1:0));
        buffer.put((byte)(pollchecksInserted?1:0));
        buffer.putInt(varIds);
        buffer.putInt(verifiabilityMode.ordinal());
        verifiabilityReport.writeTo(buffer);
        buffer.put((byte)cresult.descriptor);
        buffer.putInt(cparams.length);
        for (Basetype b : cparams) {
            buffer.put((byte)b.descriptor);
        }
        buffer.putInt(headerCnt);
        buffer.putInt(handlerCnt);
        
        final NioContext ctx=new NioContext(this);
        
        ctx.extractDebugInfosFromCode();
        
        buffer.putInt(ctx.coCodes.size());
        for (CodeOrigin co : ctx.coCodes.things()) {
            co.writeTo(buffer);
        }
        
        buffer.putInt(ctx.coCodes.codeFor(origin));
        
        buffer.putInt(ctx.diCodes.size());
        for (DebugInfo di : ctx.diCodes.things()) {
            di.writeTo(ctx,buffer);
        }
        
        buffer.putInt(vars.size());
        for (Var v : vars) {
            v.writeVarTo(buffer);
        }
        
        buffer.putInt(locals.size());
        for (CLocal l : locals) {
            l.writeTo(buffer);
        }
        
        for (Header h : headersRootFirst()) {
            ctx.nodeCodes.codeFor(h);
        }
        for (ExceptionHandler eh : handlers) {
            ctx.nodeCodes.codeFor(eh);
        }
        
        buffer.putInt(headers.size());
        buffer.putInt(handlers.size());
        
        for (ExceptionHandler eh : handlers) {
            eh.writeTo(ctx,buffer);
        }
        
        for (Header h : headersRootFirst()) {
            h.writeTo(ctx,buffer);
        }
    }
    
    public void readFrom(ByteBuffer buffer) {
        headers.clear();
        handlers.clear();
        localNames.clear();
        locals.clear();
        vars.clear();
        
        cname=Util.readString(buffer);
        result=Global.typeCoding.forCode(buffer.getInt());
        params=new Type[buffer.getInt()];
        for (int i=0;i<params.length;++i) {
            params[i]=Global.typeCoding.forCode(buffer.getInt());
        }
        checksInserted=buffer.get()!=0;
        isSSA=buffer.get()!=0;
        hasPhi=buffer.get()!=0;
        getsOwnThreadState=buffer.get()!=0;
        typeLowered=buffer.get()!=0;
        pollchecksInserted=buffer.get()!=0;
        varIds=buffer.getInt();
        verifiabilityMode=VerifiabilityMode.values()[buffer.getInt()];
        verifiabilityReport=new VerifiabilityReport();
        verifiabilityReport.readFrom(buffer);
        cresult=Basetype.fromChar((char)buffer.get());
        cparams=new Basetype[buffer.getInt()];
        for (int i=0;i<cparams.length;++i) {
            cparams[i]=Basetype.fromChar((char)buffer.get());
        }
        headerCnt=buffer.getInt();
        handlerCnt=buffer.getInt();
        
        NioContext ctx=new NioContext(this);
        
        int numCos=buffer.getInt();
        for (int i=0;i<numCos;++i) {
            ctx.coCodes.add(CodeOrigin.readFrom(buffer));
        }
        
        origin=ctx.coCodes.forCode(buffer.getInt());
        
        int numDis=buffer.getInt();
        for (int i=0;i<numDis;++i) {
            ctx.diCodes.add(new DebugInfo(NioRead.NIO_READ));
        }
        for (DebugInfo di : ctx.diCodes.things()) {
            di.readFrom(ctx,buffer);
        }
        
        int numVars=buffer.getInt();
        for (int i=0;i<numVars;++i) {
            vars.add(new Var(this,vars.size(),buffer));
        }
        
        int numLocals=buffer.getInt();
        for (int i=0;i<numLocals;++i) {
            addLocal(CLocal.readFrom(buffer));
        }
        
        int numHeaders=buffer.getInt();
        assert numHeaders>0;
        headers.add(new Header(NioRead.NIO_READ,this));
        setRoot(headers.get(0));
        for (int i=1;i<numHeaders;++i) {
            headers.add(new Header(NioRead.NIO_READ,this));
        }
        for (Header h : headersRootFirst()) {
            ctx.nodeCodes.add(h);
        }
        
        int numHandlers=buffer.getInt();
        for (int i=0;i<numHandlers;++i) {
            handlers.add(new ExceptionHandler(NioRead.NIO_READ));
        }
        for (ExceptionHandler eh : handlers) {
            ctx.nodeCodes.add(eh);
        }
        
        for (ExceptionHandler eh : handlers) {
            eh.readFrom(ctx,buffer);
        }
        
        for (Header h : headersRootFirst()) {
            h.readFrom(ctx,buffer);
        }
    }
    
    public static Code readFrom(NioReader reader) {
        Code result=new Code(NioRead.NIO_READ);
        if (reader.read(result)) {
            return result;
        } else {
            return null;
        }
    }
    
    public void writeReadTest() {
        killAllAnalyses();
        ByteBuffer buffer=NioWriter.writeToAndResize(this,null);
        buffer.flip();
        readFrom(buffer);
        assert buffer.remaining()==0;
    }
    
    public static final ArrayList< Code > EMPTY_AL=new ArrayList< Code >();

    public boolean shouldFlowLog() {
	final VisibleMethod vm = origin.origin();
	return vm != null && !vm.noFlowLog() && !getContext().noFlowLog();
    }
}


