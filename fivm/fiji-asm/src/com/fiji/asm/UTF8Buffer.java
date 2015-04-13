/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2007 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.fiji.asm;

public final class UTF8Buffer {
    byte[] buffer;
    int position;
    
    public UTF8Buffer() {
        buffer=new byte[8];
        position=0;
    }
    
    public UTF8Buffer(UTF8Sequence seq) {
        buffer=new byte[seq.byteLength()+8];
        position=0;
        append(seq);
    }
    
    public UTF8Sequence get() {
        return new UTF8Sequence(buffer,0,position);
    }
    
    public String toString() {
        return get().toString();
    }
    
    private void enlarge(int amount) {
        byte[] newBuffer=new byte[(position+amount)<<1];
        System.arraycopy(buffer,0,
                         newBuffer,0,
                         position);
        buffer=newBuffer;
    }
    
    public void append(byte c) {
        if (position==buffer.length) {
            enlarge(1);
        }
        buffer[position++]=c;
    }
    
    public void append(byte[] array,int offset,int length) {
        if (position+length>buffer.length) {
            enlarge(length);
        }
        System.arraycopy(array,offset,
                         buffer,position,
                         length);
        position+=length;
    }
    
    public void append(byte[] array) {
        append(array,0,array.length);
    }
    
    public void append(UTF8Sequence sequence) {
        append(sequence.byteArray(),
               sequence.byteStart(),
               sequence.byteLength());
    }
}

