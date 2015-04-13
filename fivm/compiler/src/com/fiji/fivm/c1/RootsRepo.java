/*
 * RootsRepo.java
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

import com.fiji.fivm.config.Reflect;

public class RootsRepo extends DirectContextable {
    HashSet< VisibleClass > natives=
	new HashSet< VisibleClass >();

    Roots rootRoots;
    HashMap< VisibleMethod, Roots > conditionalRoots=
	new HashMap< VisibleMethod, Roots >();
    
    // FIXME: have the notion of a reflective Jar, where everything in the Jar is
    // treated as if it was marked @Reflect
    
    @SuppressWarnings("unchecked")
    public RootsRepo(Context context,VisibleMethod entrypoint,List< Reflect > reflects) {
	super(context);
	
	if (Global.verbosity>=1) Global.log.println("generating analysis roots repository...");
	long before=System.currentTimeMillis();
	
	rootRoots=new Roots(context);
	if (entrypoint!=null) {
            rootRoots.call(entrypoint);
	}
	for (Type t : Type.fundamentals()) {
	    rootRoots.use(t);
	    if (t.isObject()) {
		rootRoots.alloc(t);
	    }
	}
	
	// not sure if this is needed, but we might as well play it safe...
	rootRoots.alloc(Global.root().stringType);
	rootRoots.alloc(Global.root().classType);
        
        // fix to #56
        for (VisibleField f : InterceptIntrinsicFields.interceptedFields()) {
            rootRoots.access(f);
        }
	
	// parse the configuration's list of reflective methods
	for (Reflect r : reflects) {
	    switch (r.kind()) {
	    case Called:
		ensureRootsFor(r.cause()).call(r.target());
		break;
	    case DynCalled:
		ensureRootsFor(r.cause()).dynCall(r.target());
		break;
	    case Accessed:
		ensureRootsFor(r.cause()).access(r.target());
		break;
	    case Alloced:
		ensureRootsFor(r.cause()).alloc(r.target());
		break;
	    case Used:
		ensureRootsFor(r.cause()).use(r.target());
		break;
	    default: throw new Error("invalid reflect kind: "+r.kind());
	    }
	}

	// anything marked @Export is to be called, anything marked @Reflect
	// is to be alloced, accessed, or called.  as a special case, a
	// field marked @Reflect will have its type alloced.
	for (VisibleClass c : Global.resolvedClasses()) {
	    if (c.hasAnnotation(Runtime.reflect)) {
		rootRoots.alloc(c.asType());
	    }
	    VisibleAnnotation r=c.getAnnotation(Runtime.condReflect);
	    if (r!=null) {
		List< String > causes=(List< String >)r.get("value");
		for (String cause : causes) {
		    ensureRootsFor(cause).alloc(c.asType());
		}
	    }
	    for (VisibleField f : c.fields()) {
		if (f.hasAnnotation(Runtime.reflect)) {
		    rootRoots.access(f);
		    if (f.getType().isObject()) {
			rootRoots.alloc(f.getType());
		    }
		}
		r=f.getAnnotation(Runtime.condReflect);
		if (r!=null) {
		    List< String > causes=(List< String >)r.get("value");
		    for (String cause : causes) {
			ensureRootsFor(cause).access(f);
			if (f.getType().isObject()) {
			    ensureRootsFor(cause).alloc(f.getType());
			}
		    }
		}
	    }
	    for (VisibleMethod m : c.methods()) {
		if (m.hasAnnotation(Global.root().exportClass) ||
		    m.hasAnnotation(Runtime.reflect)) {
		    rootRoots.dynCall(m);
		}
		r=m.getAnnotation(Runtime.condReflect);
		if (r!=null) {
		    List< String > causes=(List< String >)r.get("value");
		    for (String cause : causes) {
			ensureRootsFor(cause).dynCall(m);
		    }
		}
	    }
	}

	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) Global.log.println("generated roots repository in "+(after-before)+" ms");
    }
    
    Roots ensureRootsFor(String desc) {
	if (desc==null) {
	    return rootRoots;
	} else {
	    try {
		return ensureRootsFor(getContext().resolveMethod(desc));
	    } catch (ResolutionFailed e) {
		if (Global.verbosity>=2) Global.log.println(e);
		return new Roots(getContext()); // return dummy throw-away roots
	    }
	}
    }
    
    Roots ensureRootsFor(VisibleMethod m) {
	if (m==null) {
	    return rootRoots;
	} else {
	    Roots result=conditionalRoots.get(m);
	    if (result==null) {
		conditionalRoots.put(m,result=new Roots(getContext()));
	    }
	    return result;
	}
    }
    
    public Roots rootsFor(VisibleMethod m) {
	Roots result=conditionalRoots.get(m);
	if (result==null) {
	    return Roots.EMPTY;
	} else {
	    return result;
	}
    }
    
    public Roots rootRoots() {
	return rootRoots;
    }
}

