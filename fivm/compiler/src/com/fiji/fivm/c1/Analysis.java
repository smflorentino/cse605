/*
 * Analysis.java
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
import java.io.*;

import com.fiji.fivm.om.OMField;

/**
 * The whole-program analysis.  How this is used:
 * <ol>
 * <li>First, all classes are parsed in (though the bytecode isn't processed),
 *     and the configuration of the Context is locked in.</li>
 * <li>An analysis is picked, and it runs.</li>
 * <li>The Context fills in the blanks - computing such things as knownSubs,
 *     knownOverrides, etc.</li>
 * <li>Compilation runs.</li>
 * </ol>
 */
public abstract class Analysis {
    public abstract boolean closed();
    
    public final boolean open() {
        return !closed();
    }
    
    /** Returns the set of types that can be accessed in some way or
	another.  Can only be called if closed()==true. */
    public abstract Set< Type > usedTypes();
    
    /** Returns the set of types that are instantiated.  Can only be
	called if closed()==true.  */
    public abstract Set< Type > instantiatedTypes();
    
    /** Returns the set of methods that may be called, excluding calls that
	are known to fail (like calls on instance methods of classes never
	instantiated).  Can only be called if closed()==true.  */
    public abstract Set< VisibleMethod > calledMethods();
    
    /** Returns the set of methods that may be executed.  Can only be
	called if closed()==true.  */
    public abstract Set< VisibleMethod > executedMethods();
    
    /** Returns the set of fields that may be accessed.  Can only be
	called if closed()==true.  */
    public abstract Set< VisibleField > liveFields();
    
    private LinkedHashSet< VisibleClass > instClasses;
    public synchronized Set< VisibleClass > instantiatedClasses() {
	if (instClasses==null) {
	    instClasses=new LinkedHashSet< VisibleClass >();
	    for (Type t : instantiatedTypes()) {
		if (t.hasEffectiveClass()) {
		    instClasses.add(t.effectiveClass());
		}
	    }
	}
	return instClasses;
    }
    
    private LinkedHashSet< VisibleClass > liveClasses;
    public synchronized Set< VisibleClass > liveClasses() {
	if (liveClasses==null) {
	    liveClasses=new LinkedHashSet< VisibleClass >();
	    for (Type t : usedTypes()) {
		if (t.hasEffectiveClass()) {
		    liveClasses.add(t.effectiveClass());
		}
	    }
	}
	return liveClasses;
    }
    
    public boolean isUsed(Type c) {
	return usedTypes().contains(c);
    }
    
    public boolean hasInstances(Type t) {
	return instantiatedTypes().contains(t);
    }
    
    public boolean isCalled(VisibleMethod m) {
	return calledMethods().contains(m);
    }
    
    public boolean isExecuted(VisibleMethod m) {
	return executedMethods().contains(m);
    }
    
    public boolean isLive(OMField f) {
	return liveFields().contains(f);
    }
    
    public boolean hasInstances(VisibleClass c) {
	return hasInstances(c.asType());
    }
    
    public void pruneCalledMethods(Set< VisibleMethod > pruneSet) {
    }
    
    public void pruneExecutedMethods(Set< VisibleMethod > pruneSet) {
    }
    
    public PTSet bottom() { return PTSet.bottom(); }
    public abstract PTSet top();
    
    public abstract PTSet setFor(VisibleField f);
    public PTSet setFor(PTSet receiver,VisibleField f) {
	return setFor(f);
    }
    
    public abstract PTSet[] paramSetForExec(VisibleMethod m);
    public PTSet[] paramSetForExec(PTSet receiver,VisibleMethod m) {
	return paramSetForExec(m);
    }
    
    public abstract PTSet returnSetForExec(VisibleMethod m);
    public PTSet returnSetForExec(PTSet receiver,VisibleMethod m) {
	return returnSetForExec(m);
    }
    
    public abstract PTSet[] paramSetForCall(VisibleMethod m);
    public abstract PTSet[] paramSetForCall(PTSet receiver,VisibleMethod m);
    
    public abstract PTSet returnSetForCall(VisibleMethod m);
    public abstract PTSet returnSetForCall(PTSet receiver,VisibleMethod m);
    
    // FIXME: have a story for arrays...

    public void dumpUsedTypes(String filename) throws IOException {
	Util.dumpSortedJNI(filename,usedTypes());
    }
    
    public void dumpInstantiatedTypes(String filename) throws IOException {
	Util.dumpSortedJNI(filename,instantiatedTypes());
    }
    
    public void dumpCalledMethods(String filename) throws IOException {
	Util.dumpSortedJNI(filename,calledMethods());
    }
    
    public void dumpExecutedMethods(String filename) throws IOException {
	Util.dumpSortedJNI(filename,executedMethods());
    }
    
    public void dumpLiveFields(String filename) throws IOException {
	Util.dumpSortedJNI(filename,liveFields());
    }
    
    public void dumpExecutedMethodsDetail(String filename) throws IOException {
	ArrayList< String > list=new ArrayList< String >();
	for (VisibleMethod m : executedMethods()) {
	    list.add(
		m.jniName()+
		" params=("+Util.dump(paramSetForExec(m))+
		") result="+returnSetForExec(m));
	}
	Util.dumpSorted(filename,list);
    }
    
    public void dumpLiveFieldsDetail(String filename) throws IOException {
	ArrayList< String > list=new ArrayList< String >();
	for (VisibleField f : liveFields()) {
	    list.add(
		f.jniName()+
		" value="+setFor(f));
	}
	Util.dumpSorted(filename,list);
    }
    
    public Roots combinedRoots(RootsRepo repo) {
	Roots result=new Roots(null); /* pass null as the Context since these roots
                                         are already resolved (Roots only uses the
                                         context for class/method/field resolution) */
	result.addAll(repo.rootRoots());
	for (VisibleMethod m : executedMethods()) {
	    result.addAll(repo.rootsFor(m));
	}
	return result;
    }
}


