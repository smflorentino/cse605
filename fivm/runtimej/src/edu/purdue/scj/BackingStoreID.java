/*
 * BackingStoreID.java
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

package edu.purdue.scj;

import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.MemoryAreas;

import com.fiji.fivm.r1.Export;
import com.fiji.fivm.r1.NoScopeChecks;
import com.fiji.fivm.r1.NoExecStatusTransition;
import com.fiji.fivm.r1.UseObjectsNotHandles;

import javax.realtime.IllegalAssignmentError;

public class BackingStoreID {
    private Pointer area = Pointer.zero();
    private Object note = null;
    private Object portal = null;
    private int threadSpecific = NOT_THREADSPECIFIC;
    private final String name;

    public static final int NOT_THREADSPECIFIC = 0;
    public static final int HEAP = 1;

    public BackingStoreID(String name) {
	this.name = name;
    }

    public String getName() {
	return name;
    }

    public void setThreadSpecific(int areaid) {
	threadSpecific = areaid;
    }

    public Object getNote() {
	return note;
    }

    /* FIXME: This really shouldn't be NoScopeChecks, but because
     * BackingStoreIDs pretend to be immortal and this might be called
     * before enter() on the associated MemoryArea, it's not clear exactly
     * what check should be performed here. */
    @NoScopeChecks
    public void setNote(Object note) {
	this.note = note;
    }

    public Object getPortal() {
	return portal;
    }

    @NoScopeChecks
    public void setPortal(Object portal) {
	if (VMSupport.areaOf(portal) != this || getArea() == Pointer.zero()) {
	    throw new IllegalAssignmentError();
	}
	this.portal = portal;
    }

    public void setArea(Pointer area) {
	this.area = area;
    }

    public Pointer getArea() {
	if (threadSpecific != NOT_THREADSPECIFIC) {
	    switch (threadSpecific) {
	    case HEAP:
		return MemoryAreas.getHeapArea();
	    }
	    /* Shouldn't be reached */
	    return area;
	} else {
	    return area;
	}
    }

    @SuppressWarnings("unused")
    @NoExecStatusTransition
    @UseObjectsNotHandles
    @Export
    private static Object BackingStoreID_create(String name) {
	return new BackingStoreID(name);
    }
}
