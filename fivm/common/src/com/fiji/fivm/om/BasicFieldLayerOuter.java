/*
 * BasicFieldLayerOuter.java
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

package com.fiji.fivm.om;

import java.util.*;

/** A basic field layer outer that is somewhat configurable through virtual
    methods.  The default is to use native alignment, give each field enough
    space for the size of its effective basetype, and pack pointers first. */
public class BasicFieldLayerOuter extends FieldLayerOuter {
    public void iterateOverFields(OMClass c,FieldVisitor v) {
	for (OMField f : c.omFields()) {
	    if (f.shouldExist() &&
		f.isTraced() &&
		!f.isStatic()) {
		v.visitField(f);
	    }
	}
	for (OMField f : c.omFields()) {
	    if (f.shouldExist() &&
		!f.isTraced() &&
		!f.isStatic()) {
		v.visitField(f);
	    }
	}
    }

    public void layOutFields(final OMClass c) {
	if (!c.hasSuperclass()) {
	    c.setPayloadSize(OMData.totalHeaderSize());
	    c.setRequiredPayloadAlignment(OMData.minimumRequiredAlignment());
	} else {
	    c.setPayloadSize(c.getSuperclass().payloadSize());
	    c.setRequiredPayloadAlignment(c.getSuperclass().requiredPayloadAlignment());
	}
	final LinkedList< OMField > laidOutFields=new LinkedList< OMField >();
	iterateOverFields(c,new FieldVisitor() {
		public void visitField(OMField f) {
		    laidOutFields.add(f);
		    c.setRequiredPayloadAlignment(Math.max(c.requiredPayloadAlignment(),
                                                           fieldAlignment(f)));
		    while (((c.payloadSize()-OMData.totalHeaderSize())%
                            fieldAlignment(f))!=0) {
                        c.setPayloadSize(c.payloadSize() + 1);
                    }
		    f.setLocation(c.payloadSize()-OMData.totalHeaderSize());
		    c.setPayloadSize(c.payloadSize() + fieldWidth(f));
		}
	    });
	OMField[] laidOutFieldsArray=new OMField[laidOutFields.size()];
	laidOutFields.toArray(laidOutFieldsArray);
        c.omSetLaidOutFields(laidOutFieldsArray);
    }
    
    public static abstract class FieldVisitor {
	public abstract void visitField(OMField f);
    }
    
    public String toString() {
        if (getClass()==BasicFieldLayerOuter.class) {
            return "Basic Ref-fields-first Field Layout";
        } else {
            return "Customized Field Layout";
        }
    }
}


