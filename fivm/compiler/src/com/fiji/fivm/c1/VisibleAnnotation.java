/*
 * VisibleAnnotation.java
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

import com.fiji.fivm.om.OMClass;

public class VisibleAnnotation implements Contextable {
    
    Annotatable a;
    OMClass clazz;
    
    HashMap< String, Object > values=new HashMap< String, Object >();
    
    VisibleAnnotation(Annotatable a,OMClass clazz) {
	this.a=a;
	this.clazz=clazz;
    }
    
    public Annotatable getParent() { return a; }
    public Context getContext() { return a.getContext(); }
    public OMClass getClazz() { return clazz; }
    
    // convention:
    // String is represented as String
    // primitives are represented by the respective java.lang.* boxing classes
    // enums are represented by String (yeah, that's right, we're keeping it simple)
    // class is represented by Type
    // arrays are represented as List< Object >.
    
    public void set(String name,Object value) {
	values.put(name,value);
    }
    
    public Object get(String name) {
	return values.get(name);
    }
}

