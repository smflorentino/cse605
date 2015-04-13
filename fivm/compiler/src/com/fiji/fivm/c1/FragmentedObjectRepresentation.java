/*
 * FragmentedObjectRepresentation.java
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

public class FragmentedObjectRepresentation {
    private FragmentedObjectRepresentation() {}
    
    // how this works:
    // - object offsets are zero (we always point at the beginning)
    // - chunk header comes first
    // - chunk header has a low bit that indicates if the object is
    //   actually contiguous.
    // - fields are laid out as per FragmentedFieldLayerOuter
    // - arraylets have a pointer to a spine; array bodies in chunks
    //   are laid out in reverse; spine points at the last element
    //   on each chunk.
    
    // FIXME: what is the header of the spine?  currently it's the "true" array length.
    // but that seems bad ... in that case how does the GC know its size?
    
    public static int logChunkWidth;
    public static int chunkWidth;
    public static int gcHeaderSize;
    public static int chunkHeaderSize;
    public static int arrayHeaderSize;
    
    public static void init() {
        Global.objectTDOffset = -Global.pointerSize*2;
        Global.objectGCOffset = -Global.pointerSize;
        Global.allocOffset = 0;
        
        if (Global.pointerSize==4) {
            logChunkWidth=5;
        } else {
            assert Global.pointerSize==8;
            logChunkWidth=6;
        }
        chunkWidth=1<<logChunkWidth;
        gcHeaderSize=Global.pointerSize;
        chunkHeaderSize=Global.pointerSize;
        arrayHeaderSize=
            chunkHeaderSize+ // pointer to spine is held in this thing
            gcHeaderSize+
            Global.tdHeaderSize()+
            4;
            
        Global.extraHeaderSize = Global.pointerSize;
        
        if (Global.verbosity>=1) {
            Global.log.println("Fragmented object model settings:");
            Global.log.println("   Log Chunk Width:      "+logChunkWidth);
            Global.log.println("   Chunk Width:          "+chunkWidth);
            Global.log.println("   GC Header Size:       "+gcHeaderSize);
            Global.log.println("   Chunk Header Size:    "+chunkHeaderSize);
            Global.log.println("   Array Header Size:    "+arrayHeaderSize);
        }
    }
    
    public static int hopsForField(int offset) {
        return offset/chunkWidth;
    }
    
    public static int chunkOffset(int offset) {
        return offset%chunkWidth;
    }
    
    public static int numChunks(int size) {
        return (size+chunkWidth-1)/chunkWidth;
    }
}

