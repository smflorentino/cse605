package com.fiji.util;

import com.fiji.fivm.r1.*;
import java.nio.*;

class UTF8Support {
    private UTF8Support() {}
    
    @AllocateAsCaller
    public static ByteBuffer allocateByteBuffer(int length) {
        return wrap(new byte[length],0,length);
    }

    @AllocateAsCaller
    public static CharBuffer allocateCharBuffer(int length) {
        return wrap(new char[length],0,length);
    }
    
    @AllocateAsCaller
    public static ByteBuffer wrap(byte[] array,int off,int len) {
        return java.nio.fivmSupport.wrap(array,off,len);
    }
    
    @AllocateAsCaller
    public static CharBuffer wrap(char[] array,int off,int len) {
        return java.nio.fivmSupport.wrap(array,off,len);
    }
    
    @AllocateAsCaller
    public static CharBuffer wrap(CharSequence seq,int off,int len) {
        return java.nio.fivmSupport.wrap(seq,off,len);
    }
}

