/*
 * CachingCodeRepo.java
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

public class CachingCodeRepo extends CodeRepo {
    // NB: cannot use WeakHashMap because the keys are kept alive elsewhere,
    // and perhaps more importantly, are transitively referred to by the
    // values.  (carefully read the javadoc for WeakHashMap if you don't
    // understand.)
    HashMap< VisibleClass, ArrayList< Code > > cache=
	new HashMap< VisibleClass, ArrayList< Code > >();

    static void refreshCode(Code c) {
        PhaseFixpoint.simplify(c);
        new SimpleProp(c).doit();
        new IntraproceduralCFA(c,CFAMode.CFA_FOR_TRANSFORMATION).doit();
        PhaseFixpoint.simplify(c);
    }

    static ArrayList< Code > prepare(ArrayList< Code > cfc,
                                     Set< MethodSignature > methods,
                                     boolean copyCode,
                                     boolean refreshCode) {
	ArrayList< Code > result=new ArrayList< Code >();
        for (Code code : cfc) {
	    if ((code.method()==null ||
		 Global.analysis().isExecuted(code.method())) &&
		(methods==null ||
                 methods.contains(code.origin().origin().getSignature()))) {
		Code codeCopy=code;
		if (copyCode) {
                    code.verify("CachingCodeRepo.prepare");
		    codeCopy=code.copy();
                }
                if (refreshCode) {
                    refreshCode(codeCopy);
		}
		result.add(codeCopy);
	    }
	}
	return result;
    }
    
    protected ArrayList< Code > parseCodeForClass(VisibleClass klass) {
        return Util.codeForClass(klass,null);
    }
    
    private synchronized ArrayList< Code > codeForClass(VisibleClass c,
                                                        Set< MethodSignature > methods,
                                                        boolean remove) {
	ArrayList< Code > cfc;
        if (remove) {
            cfc=cache.remove(c);
        } else {
            cfc=cache.get(c);
        }
	boolean fresh;
	if (cfc==null) {
	    cfc=parseCodeForClass(c);
	    ArrayList< Code > saved=new ArrayList< Code >();
	    for (Code code : cfc) {
		// this implicitly ensures that we don't save analyses artifacts
		// in the cache.
		saved.add(code.copy());
	    }
	    cache.put(c,saved);
	    fresh=true;
	} else {
	    fresh=false;
	}
        
        return prepare(cfc,methods,!fresh,!fresh);
    }
    
    public ArrayList< Code > codeForClass(VisibleClass c,
                                          Set< MethodSignature > methods) {
        return codeForClass(c,methods,false);
    }
    
    public ArrayList< Code > removeCodeForClass(VisibleClass c) {
        return codeForClass(c,null,true);
    }
}

