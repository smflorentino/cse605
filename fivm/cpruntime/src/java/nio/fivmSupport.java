package java.nio;

import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.r1.fivmCPRuntime.*;

public final class fivmSupport {
    private fivmSupport() {}
    
    @AllocateAsCaller
    public static ByteBuffer wrap(Pointer p,
                                  int capacity,
                                  int limit,
                                  int position) {
	return new DirectByteBufferImpl.ReadWrite(null,
						  toCLPtr(p),
						  capacity,
						  limit,
						  position);
    }
    @Export
    static ByteBuffer DirectByteBuffer_wrap(Pointer p,
                                            int capacity,
                                            int limit,
                                            int position) {
        return wrap(p,capacity,limit,position);
    }
    
    @Export
    static Pointer DirectByteBuffer_address(Object o) {
	if (o instanceof DirectByteBufferImpl) {
	    return fromCLPtr(((DirectByteBufferImpl)o).address);
	} else {
	    return Pointer.zero();
	}
    }
    
    @Export
    static int DirectByteBuffer_capacity(Object o) {
	if (o instanceof DirectByteBufferImpl) {
	    return ((DirectByteBufferImpl)o).capacity();
	} else {
	    return 0;
	}
    }
    
    @AllocateAsCaller
    public static ByteBuffer wrap(byte[] array,int off,int len) {
        return new ByteBufferImpl(array,0,array.length,off+len,off,-1,false);
    }
    
    @AllocateAsCaller
    public static CharBuffer wrap(char[] array,int off,int len) {
        return new CharBufferImpl(array,0,array.length,off+len,off,-1,false);
    }
    
    @AllocateAsCaller
    public static CharBuffer wrap(CharSequence seq,int off,int len) {
        return new CharSequenceBuffer(seq,off,len);
    }
}


