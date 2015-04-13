/*
 * PatchThunk.java
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

public final class PatchThunk {
    VisibleMethod parent;
    int bcOffset;
    int nLocals;
    int nStack;
    BitSet heapPointers;
    VisibleClass neededClass;
    String description;
    
    public PatchThunk(VisibleMethod parent,
                      int bcOffset,
                      int nLocals,
                      int nStack,
                      BitSet heapPointers,
                      VisibleClass neededClass,
                      String description) {
        this.parent=parent;
        this.bcOffset=bcOffset;
        this.nLocals=nLocals;
        this.nStack=nStack;
        this.heapPointers=heapPointers;
        this.neededClass=neededClass;
        this.description=description;
    }
    
    public VisibleMethod parent() { return parent; }
    public int bcOffset() { return bcOffset; }
    public int nLocals() { return nLocals; }
    public int nStack() { return nStack; }
    public int stateSize() { return 1+nLocals+nStack; }
    public BitSet heapPointers() { return heapPointers; }
    public VisibleClass neededClass() { return neededClass; }
    public String description() { return description; }
    
    public int hashCode() {
        return parent.hashCode()+bcOffset+nLocals+nStack+
            neededClass.hashCode()+description.hashCode()+
            heapPointers.hashCode();
    }
    
    public boolean equals(Object other_) {
        if (this==other_) return true;
        if (!(other_ instanceof PatchThunk)) return false;
        PatchThunk other=(PatchThunk)other_;
        return parent==other.parent
            && bcOffset==other.bcOffset
            && nLocals==other.nLocals
            && nStack==other.nStack
            && heapPointers.equals(other.heapPointers())
            && neededClass==other.neededClass
            && description.equals(other.description);
    }
    
    public String toString() {
        return "PatchThunk["+parent+", "+bcOffset+", "+nLocals+", "+nStack+
            ", "+neededClass+", "+description+"]";
    }
}

