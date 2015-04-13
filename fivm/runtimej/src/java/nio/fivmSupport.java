/*
 * fivmSupport.java
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

package java.nio;

import com.fiji.fivm.r1.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import __vm.*;

public final class fivmSupport {
    private fivmSupport() {}
    
    @AllocateAsCaller
    public static ByteBuffer wrap(Pointer p,
                                  int capacity,
                                  int limit,
                                  int position) {
	return new ByteBufferImpl(null,
				  FCMagic.fromVMPointer(p),
				  0,
				  capacity,
				  limit,
				  position,
				  -1,false);
    }
    
    @Export
    public static ByteBuffer DirectByteBuffer_wrap(Pointer p,
                                                   int capacity,
                                                   int limit,
                                                   int position) {
        return wrap(p,capacity,limit,position);
    }
    
    public static enum MakeVMBufferMode {
        LIMITED_SIZE, UNLIMITED_SIZE
    }
    
    public static enum ReadWriteMode {
        READ_ONLY {
            public boolean canWrite() { return false; }
        },

        WRITE_ONLY {
            public boolean canRead() { return false; }
        },

        READ_WRITE;
        
        public boolean canRead() { return true; }
        public boolean canWrite() { return true; }
    }
    
    @NoInline
    @AllocateAsCaller
    private static ByteBuffer makeVMBufferImpl(ByteBuffer bb,
                                               MakeVMBufferMode mode,
                                               ReadWriteMode rw) {
        final int threshold=4096;
        
        bb=bb.slice();
        
        int size=bb.remaining();
        ByteBuffer result;
        if (size>threshold && mode==MakeVMBufferMode.LIMITED_SIZE) {
            result=ByteBuffer.allocate(threshold);
            bb.limit(threshold);
            
            // FIXME: pointless assertion?
            if (bb.remaining()!=threshold) {
                throw abort("bb.remaining()!=threshold: "+bb.remaining()+", "+threshold);
            }
        } else {
            result=ByteBuffer.allocate(size);
        }
        if (!result.isVMBuffer()) {
            throw abort("trying to make a VM buffer but got one that isn't - this is usually caused by a call to makeVMBuffer from a method that is not @StackAllocation.");
        }
        if (rw.canRead()) {
            result.put(bb);
            result.flip();
        }
	return result;
    }
    
    @Inline
    @AllocateAsCaller
    public static ByteBuffer makeVMBuffer(ByteBuffer bb,
                                          MakeVMBufferMode mode,
                                          ReadWriteMode rw) {
	if (bb.isVMBuffer()) {
            if (false) logPrint("returning VM buffer\n");
	    return bb;
	} else {
            if (false) logPrint("converting to VM buffer\n");
	    return makeVMBufferImpl(bb,mode,rw);
	}
    }
    
    public static Pointer address(ByteBuffer bb) {
	return FCMagic.toVMPointer(((ByteBufferImpl)bb).address);
    }
    
    public static Pointer positionAddress(ByteBuffer bb,int position) {
	return address(bb).add(position);
    }
    
    public static Pointer positionAddress(ByteBuffer bb) {
	return positionAddress(bb,bb.position());
    }
    
    @Export
    static Pointer DirectByteBuffer_address(Object o) {
	if (!(o instanceof ByteBuffer)) {
	    throw new fivmError("wrong type");
	}
	if (((ByteBuffer)o).isVMBuffer()) {
	    return address((ByteBuffer)o);
	} else {
	    return Pointer.zero();
	}
    }
    
    @Export
    static int DirectByteBuffer_capacity(Object o) {
	if (o instanceof ByteBuffer) {
	    throw new fivmError("wrong type");
	}
	if (((ByteBuffer)o).isVMBuffer()) {
	    return ((ByteBuffer)o).capacity();
	} else {
	    return 0;
	}
    }
    
    @AllocateAsCaller
    public static ByteBuffer wrap(byte[] array,int off,int len) {
        return ByteBuffer.wrap(array,off,len);
    }
    
    @AllocateAsCaller
    public static CharBuffer wrap(char[] array,int off,int len) {
        return CharBuffer.wrap(array,off,len);
    }
    
    @AllocateAsCaller
    public static CharBuffer wrap(CharSequence seq,int off,int len) {
        return CharBuffer.wrap(seq,off,len);
    }
}


