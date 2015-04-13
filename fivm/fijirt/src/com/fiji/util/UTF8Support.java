package com.fiji.util;

import com.fiji.fivm.r1.*;
import java.nio.*;

class UTF8Support {
    private UTF8Support() {}
    
    @AllocateAsCaller
    public static ByteBuffer allocateByteBuffer(int length) {
        return ByteBuffer.allocate(length);
    }

    @AllocateAsCaller
    public static CharBuffer allocateCharBuffer(int length) {
        return CharBuffer.allocate(length);
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

