package java.nio;

import com.fiji.fivm.r1.*;
import com.fiji.fivm.Settings;

import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.r1.fivmCPRuntime.*;

final class VMDirectByteBuffer {
    
    static gnu.classpath.Pointer allocate(int capacity) {
	return toCLPtr(fivmr_malloc(Pointer.fromIntZeroFill(capacity)));
    }
    
    static void free(gnu.classpath.Pointer p) {
	fivmr_free(fromCLPtr(p));
    }
    
    static byte get(gnu.classpath.Pointer p,int index) {
	return fromCLPtr(p).add(index).loadByte();
    }
    
    static void get(gnu.classpath.Pointer p,
		    int index,
		    byte[] dst,
		    int offset,
		    int length) {
        if (Settings.OM_CONTIGUOUS) {
            libc.memcpy(Magic.addressOfElement(dst,offset),
                        fromCLPtr(p).add(index),
                        Pointer.fromInt(length));
        } else {
            throw abort("don't know how to handle non-contiguous arrays yet");
        }
    }
    
    static void put(gnu.classpath.Pointer p,
		    int index,
		    byte value) {
	fromCLPtr(p).add(index).store(value);
    }
    
    static void put(gnu.classpath.Pointer p,
		    int index,
		    byte[] src,
		    int offset,
		    int length) {
        if (Settings.OM_CONTIGUOUS) {
            libc.memcpy(fromCLPtr(p).add(index),
                        Magic.addressOfElement(src,offset),
                        Pointer.fromInt(length));
        } else {
            throw abort("don't know how to handle non-contiguous arrays yet");
        }
    }
    
    static gnu.classpath.Pointer adjustAddress(gnu.classpath.Pointer p,int offset) {
	return toCLPtr(fromCLPtr(p).add(offset));
    }
    
    static void shiftDown(gnu.classpath.Pointer p_,
			  int dst_offset,
			  int src_offset,
			  int count) {
	Pointer p=fromCLPtr(p_);
	for (int i=0;i<count;++i) {
	    p.add(dst_offset+i).store(p.add(src_offset).loadByte());
	}
    }
}

