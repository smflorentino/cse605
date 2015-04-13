/*
 * OMData.java
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

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;

// FIXME #1: put more stuff in here

// FIXME #2: make the C1 compiler use this more extensively instead of using its own mechanisms.

public final class OMData {
    private OMData() {}
    
    @Inline @NoSafepoint @NoPollcheck
    public static int pointerSize() {
        if (Settings.PTRSIZE_32) {
            return 4;
        } else if (Settings.PTRSIZE_64) {
            return 8;
        } else {
            Magic.notReached();
            return -1;
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static Pointer alignRaw(Pointer value,
                                   Pointer align) {
        return (value.add(align).sub(1)).and(align.sub(1).not());
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static Pointer align(Pointer address,
                                Pointer align) {
        if (align.greaterThan(Pointer.size())) {
            return alignRaw(address,align);
        } else {
            return address;
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static Pointer alignCoeff(Pointer value,
                                     Pointer alignment,
                                     Pointer coefficient) {
        if (coefficient.lessThan(alignment)) {
            return alignRaw(value,alignment);
        } else {
            return value;
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int gcHeaderSize() {
        // common for all collectors right now
        return pointerSize();
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int tdHeaderSize() {
        // common for all collectors right now
        return pointerSize();
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int fhHeaderSize() {
        if (!Settings.OM_FRAGMENTED) {
            Magic.notReached();
            return -1;
        }
        return pointerSize();
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int totalHeaderSize() {
        if (Settings.OM_CONTIGUOUS) {
            return gcHeaderSize()+tdHeaderSize();
        } else if (Settings.OM_FRAGMENTED) {
            return fhHeaderSize()+gcHeaderSize()+tdHeaderSize();
        } else {
            Magic.notReached();
            return -1;
        }
    }
    
    /**
     * Returns the offset from the TypeData pointer to where the object points. 
     * @return the offset from the TypeData pointer to where the object points.
     */
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int objectTDOffset() {
        if (Settings.OM_CONTIGUOUS) {
            return Pointer.size()+4;
        } else if (Settings.OM_FRAGMENTED) {
            return -Pointer.size()*2;
        } else {
            Magic.notReached();
            return -1;
        }
    }
    
    /**
     * Returns the offset from the GC header to where the object points.
     * @return the offset from the GC header to where the object points.
     */
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int objectGCOffset() {
        if (Settings.OM_CONTIGUOUS) {
            return objectTDOffset()+gcHeaderSize();
        } else if (Settings.OM_FRAGMENTED) {
            return -Pointer.size();
        } else {
            Magic.notReached();
            return -1;
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int objectFHOffset() {
        if (Settings.OM_FRAGMENTED) {
            return 0;
        } else {
            Magic.notReached();
            return -1;
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int spineArrayLengthOffset() {
        return -4;
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int spineForwardOffset() {
        return -Pointer.size()*2;
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int objectSpace() {
	return 0;
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int stackAllocSpace() {
        return 1;
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int minimumRequiredAlignment() {
        return pointerSize();
    }

    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int logChunkWidth() {
        if (!Settings.OM_FRAGMENTED) {
            Magic.notReached();
            return -1;
        }
        if (Settings.PTRSIZE_32) {
            return 5;
        } else if (Settings.PTRSIZE_64) {
            return 6;
        } else {
            Magic.notReached();
            return -1;
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int arrayLengthOffset() {
        if (Settings.OM_CONTIGUOUS) {
            return -4;
        } else if (Settings.OM_FRAGMENTED) {
            return totalHeaderSize();
        } else {
            Magic.notReached();
            return -1;
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int arrayPayloadOffset(Pointer eleSize) {
        if (Settings.OM_CONTIGUOUS) {
            if (Pointer.size()==4 || eleSize.lessThanOrEqual(Pointer.fromInt(4))) {
                return 0;
            } else {
                return 4;
            }
        } else if (Settings.OM_FRAGMENTED) {
            return alignRaw(Pointer.fromInt(totalHeaderSize()+4),eleSize).castToInt();
        } else {
            Magic.notReached();
            return -1;
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int chunkWidth() {
        return 1<<logChunkWidth();
    }
}

