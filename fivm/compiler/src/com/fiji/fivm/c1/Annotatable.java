/*
 * Annotatable.java
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
import com.fiji.asm.AnnotationVisitor;
import com.fiji.asm.UTF8Sequence;
import com.fiji.asm.commons.EmptyVisitor;

import com.fiji.fivm.om.OMClass;

public abstract class Annotatable implements Contextable {
    HashMap< VisibleClass, VisibleAnnotation > annotations=
	new HashMap< VisibleClass, VisibleAnnotation >();
    
    public boolean hasAnnotation(OMClass clazz) {
	return annotations.containsKey(clazz);
    }
    
    public VisibleAnnotation getAnnotation(OMClass clazz) {
	return annotations.get(clazz);
    }
    
    void addAnnotation(VisibleClass clazz,VisibleAnnotation a) {
	annotations.put(clazz,a);
    }
    
    Object processValue(Object value) {
	if (value instanceof com.fiji.asm.Type) {
	    return Type.parse(
		getContext(),
		((com.fiji.asm.Type)value).getDescriptor());
	} else {
	    return value;
	}
    }
    
    // helper for asm
    AnnotationVisitor addAnnotation(String desc,boolean visible) {
	VisibleClass c=Type.parse(getContext(),desc).getClazz();
	final VisibleAnnotation a=new VisibleAnnotation(this,c);
	addAnnotation(c,a);
	return new EmptyVisitor() {
	    public void visit(UTF8Sequence name,Object value) {
		a.set(name.toString(),processValue(value));
	    }
	    public AnnotationVisitor visitArray(UTF8Sequence name) {
		List<Object> array=new ArrayList<Object>();
		a.set(name.toString(),array);
		return new ArrayBuilder(array);
	    }
	    public void visitEnum(UTF8Sequence name,UTF8Sequence desc,UTF8Sequence value) {
		a.set(name.toString(),value.toString());
	    }
	};
    }
    
    class ArrayBuilder extends EmptyVisitor {
	List<Object> array;
	
	ArrayBuilder(List<Object> array) {
	    this.array=array;
	}
	
	public void visit(UTF8Sequence name,Object value) {
	    array.add(processValue(value));
	}
	
	public AnnotationVisitor visitArray(UTF8Sequence name) {
	    List<Object> subArray=new ArrayList<Object>();
	    array.add(subArray);
	    return new ArrayBuilder(subArray);
	}
	
	public void visitEnum(UTF8Sequence name,UTF8Sequence desc,String value) {
	    array.add(value);
	}
    }
    
    class AnnotationAdderVisitor extends EmptyVisitor {
	public AnnotationVisitor visitAnnotation(UTF8Sequence desc,
						 boolean visible) {
	    return addAnnotation(desc.toString(),visible);
	}
    }
}

