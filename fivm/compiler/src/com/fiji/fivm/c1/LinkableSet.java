/*
 * LinkableSet.java
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

/** A flattened set of linkables.  Invariants:
    <ul>
    <li>Name uniqueness.  Given multiple linkables of the same name, only one of
        them will appear here.</li>
    <li>Local precedence.  If you tried to put both a remote and a local variant
        of a linkable into this set, then the local one is the one that will
	appear.</li>
    <li>Flatness.  If this set contains a Linkable L, then this set will also
        contain all of L.subLinkables().</li>
    </ul> */
public class LinkableSet implements Iterable< Linkable > {
    private LinkedHashMap< String, Linkable > linkables=
	new LinkedHashMap< String, Linkable >();
    
    public LinkableSet() {}
    
    public LinkableSet(Linkable l) {
	add(l);
    }
    
    public LinkableSet(LinkableSet ls) {
	addAll(ls);
    }
    
    public Linkable get(String name) {
        return linkables.get(name);
    }
    
    private boolean addImpl(Linkable l) {
	Linkable old=linkables.get(l.getName());
	if (old==null || (!old.isLocal() && l.isLocal())) {
	    linkables.put(l.getName(),l);
	    return true;
	} else {
	    return false;
	}
    }
    
    public boolean add(Linkable l) {
	boolean result=false;
	result|=addImpl(l);
	result|=addAll(l.subLinkables());
	return result;
    }
    
    public boolean add(Pointerable p) {
	if (p instanceof Linkable) {
	    return add((Linkable)p);
	} else {
	    return addAll(p.subLinkables());
	}
    }
    
    public boolean addAll(LinkableSet s) {
	boolean result=false;
	for (Linkable l : s) {
	    result|=addImpl(l);
	}
	return result;
    }
    
    public boolean addAll(Iterable< ? extends Linkable > i) {
	boolean result=false;
	for (Linkable l : i) {
	    result|=add(l);
	}
	return result;
    }
    
    public Iterator< Linkable > iterator() {
	return linkables.values().iterator();
    }
    
    public int size() {
	return linkables.size();
    }
    
    public boolean isEmpty() {
	return linkables.isEmpty();
    }
    
    public static final LinkableSet EMPTY=new LinkableSet();
}

