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

public class UTF8Sequence
    implements CharSequence, Cloneable, Comparable< UTF8Sequence >
{
    byte[] array;
    int start;
    int length;
    
    public UTF8Sequence(byte[] array,
                        int start,
                        int length) {
        this.array=array;
        this.start=start;
        this.length=length;
        if (start+length>array.length) {
            throw new ArrayIndexOutOfBoundsException("start+length is greater than array.length: "+start+" + "+length+" > "+array.length);
        }
    }
    
    // only for UTF8Sequence(String) constructor
    private void enlarge(int n) {
        byte[] newArray=new byte[(array.length+n)<<1];
        System.arraycopy(array,0,
                         newArray,0,
                         array.length);
        array=newArray;
    }
    
    public UTF8Sequence(String s) {
        int charLength = s.length();
        array=new byte[s.length()];
        int len = 0;
        byte[] data = this.array;
        // optimistic algorithm: instead of computing the byte length and then
        // serializing the string (which requires two loops), we assume the byte
        // length is equal to char length (which is the most frequent case), and
        // we start serializing the string right away. During the serialization,
        // if we find that this assumption is wrong, we continue with the
        // general method.
        for (int i = 0; i < charLength; ++i) {
            char c = s.charAt(i);
            if (c >= '\001' && c <= '\177') {
                data[len++] = (byte) c;
            } else {
                int byteLength = i;
                for (int j = i; j < charLength; ++j) {
                    c = s.charAt(j);
                    if (c >= '\001' && c <= '\177') {
                        byteLength++;
                    } else if (c > '\u07FF') {
                        byteLength += 3;
                    } else {
                        byteLength += 2;
                    }
                }
                data[length] = (byte) (byteLength >>> 8);
                data[length + 1] = (byte) byteLength;
                if (length + 2 + byteLength > data.length) {
                    length = len;
                    enlarge(2+byteLength);
                    data = this.array;
                }
                for (int j = i; j < charLength; ++j) {
                    c = s.charAt(j);
                    if (c >= '\001' && c <= '\177') {
                        data[len++] = (byte) c;
                    } else if (c > '\u07FF') {
                        data[len++] = (byte) (0xE0 | c >> 12 & 0xF);
                        data[len++] = (byte) (0x80 | c >> 6 & 0x3F);
                        data[len++] = (byte) (0x80 | c & 0x3F);
                    } else {
                        data[len++] = (byte) (0xC0 | c >> 6 & 0x1F);
                        data[len++] = (byte) (0x80 | c & 0x3F);
                    }
                }
                break;
            }
        }
        length = len;
        cached = s;
        	
        // compactify the byte array, if necessary
        if (array.length>length+(length>>>1)) {
            byte[] newArray=new byte[length];
            System.arraycopy(array,0,
        	             newArray,0,
        	             length);
            array=newArray;
        }
    }
    
    public final int hashCode() {
        // do something stupid for now
        int result=0;
        for (int i=start+length;i-->start;) {
            result+=(result<<3)+(result<<2); // result*=13
            result+=array[i];
        }
        return result;
    }
    
    public final boolean equals(Object other_) {
        if (this==other_) return true;
        if (!(other_ instanceof UTF8Sequence)) return false;
        UTF8Sequence other=(UTF8Sequence)other_;
        if (length!=other.length) return false;
        for (int i=0;i<length;++i) {
            if (array[i+start]!=other.array[i+other.start]) return false;
        }
        return true;
    }
    
    public final byte[] byteArray() { return array; }
    public final int byteStart() { return start; }
    public final int byteLength() { return length; }
    
    public final byte byteAt(int index) {
        return array[start+index];
    }
    
    public UTF8Sequence clone() {
        try {
            return (UTF8Sequence)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }
    
    public final int compareTo(UTF8Sequence other) {
        int minLength=Math.min(byteLength(),other.byteLength());
        for (int i=0;i<minLength;++i) {
            int diff=byteAt(i)-other.byteAt(i);
            if (diff!=0) {
                return diff;
            }
        }
        return byteLength()-other.byteLength();
    }
    
    String cached;
    
    private final String toStringImpl() {
        if (cached==null) {
            char[] buf=new char[length];
            for (;;) {
                int index=-1;
                int endIndex=-1;
                try {
                    try {
                        index = start;
                        endIndex = index + length;
                        byte[] b = array;
                        int strLen = 0;
                        int c, d, e;
                        while (index < endIndex) {
                            c = b[index++] & 0xFF;
                            switch (c >> 4) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                                // 0xxxxxxx
                                buf[strLen++] = (char) c;
                                break;
                            case 12:
                            case 13:
                                // 110x xxxx 10xx xxxx
                                d = b[index++];
                                buf[strLen++] = (char) (((c & 0x1F) << 6) | (d & 0x3F));
                                break;
                            default:
                                // 1110 xxxx 10xx xxxx 10xx xxxx
                                d = b[index++];
                                e = b[index++];
                                buf[strLen++] = (char) (((c & 0x0F) << 12)
                                                        | ((d & 0x3F) << 6) | (e & 0x3F));
                                break;
                            }
                        }
                        return cached=new String(buf, 0, strLen);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // will this ever happen?  who knows, who cares...
                        try {
                            buf=new char[buf.length<<1];
                        } catch (Throwable e2) {
                            throw new Error("Could not reallocate buffer due to "+e2+" after getting an ArrayIndexOutOfBoundsException",e);
                        }
                    }
                } catch (Throwable e) {
                    throw new Error("Could not convert to string: "+array+", "+start+", "+length+", "+index+", "+endIndex,e);
                }
            }
        } else {
            return cached;
        }
    }
    
    // FIXME: toString() should never be used internally by ASM
    public final String toString() {
        if (cached==null) {
            return toStringImpl();
        } else {
            return cached;
        }
    }
    
    public final char charAt(int index) {
        return toString().charAt(index);
    }
    
    public final int length() {
        return toString().length();
    }
    
    public final CharSequence subSequence(int start,int end) {
        return toString().subSequence(start,end);
    }
    
    public boolean isFromBytecode() {
        return false;
    }
    
    public byte[] bytecode() {
        return null;
    }
    
    public final void assertBytecode(byte[] bytecode) {
        if (!isFromBytecode()) {
            throw new RuntimeException("Assertion failure: UTF8Sequence does not originate in bytecode");
        }
        if (bytecode!=bytecode()) {
            throw new RuntimeException("Assertion failure: bytecode of UTF8Sequence does not match expected");
        }
    }
    
    public int bytecodeStringAddress() {
        return -1;
    }
    
    public final UTF8Sequence subseq(int start,int end) {
        return new UTF8Sequence(array,this.start+start,end-start);
    }
    
    public final UTF8Sequence subseq(int start) {
	return subseq(start,byteLength());
    }
    
    public final int indexOf(byte val,int i) {
        for (;i<length;++i) {
            if (byteAt(i)==val) {
                return i;
            }
        }
        return -1;
    }
    
    public final int indexOf(byte val) {
        return indexOf(val,0);
    }
    
    public UTF8Sequence plus(UTF8Sequence a) {
        byte[] result=new byte[byteLength()+a.byteLength()];
        System.arraycopy(byteArray(),byteStart(),
                         result,0,
                         byteLength());
        System.arraycopy(a.byteArray(),a.byteStart(),
                         result,byteLength(),
                         a.byteLength());
        return new UTF8Sequence(result,0,result.length);
    }
    
    public UTF8Sequence plus(UTF8Sequence a,UTF8Sequence b) {
        byte[] result=new byte[byteLength()+a.byteLength()+b.byteLength()];
        System.arraycopy(byteArray(),byteStart(),
                         result,0,
                         byteLength());
        System.arraycopy(a.byteArray(),a.byteStart(),
                         result,byteLength(),
                         a.byteLength());
        System.arraycopy(b.byteArray(),b.byteStart(),
                         result,byteLength()+a.byteLength(),
                         b.byteLength());
        return new UTF8Sequence(result,0,result.length);
    }
    
    public UTF8Sequence plus(UTF8Sequence a,UTF8Sequence b,UTF8Sequence... ca) {
        int clen=0;
        for (UTF8Sequence c : ca) {
            clen+=c.byteLength();
        }
        byte[] result=new byte[byteLength()+a.byteLength()+b.byteLength()+clen];
        System.arraycopy(byteArray(),byteStart(),
                         result,0,
                         byteLength());
        System.arraycopy(a.byteArray(),a.byteStart(),
                         result,byteLength(),
                         a.byteLength());
        System.arraycopy(b.byteArray(),b.byteStart(),
                         result,byteLength()+a.byteLength(),
                         b.byteLength());
        int offset=byteLength()+a.byteLength()+b.byteLength();
        for (UTF8Sequence c : ca) {
            System.arraycopy(c,c.byteStart(),
                             result,offset,
                             c.byteLength());
            offset+=c.byteLength();
        }
        return new UTF8Sequence(result,0,result.length);
    }
    
    public static String[] toString(UTF8Sequence[] array) {
        if (array==null) {
            return null;
        } else {
            String[] result=new String[array.length];
            for (int i=0;i<result.length;++i) {
                result[i]=array[i].toString();
            }
            return result;
        }
    }
    
    public static UTF8Sequence[] fromString(String[] array) {
        if (array==null) {
            return null;
        } else {
            UTF8Sequence[] result=new UTF8Sequence[array.length];
            for (int i=0;i<result.length;++i) {
                result[i]=new UTF8Sequence(array[i]);
            }
            return result;
        }
    }
    
    // common sequences we will be using
    public static final UTF8Sequence EMPTY=new UTF8Sequence("");
    public static final UTF8Sequence _init_=new UTF8Sequence("<init>");
    public static final UTF8Sequence _clinit_=new UTF8Sequence("<clinit>");
    public static final UTF8Sequence java_lang_Object=new UTF8Sequence("java/lang/Object");
    public static final UTF8Sequence java_lang_Throwable=new UTF8Sequence("java/lang/Throwable");
    public static final UTF8Sequence Ljava_lang_Object_=new UTF8Sequence("Ljava/lang/Object;");
    public static final UTF8Sequence Ljava_lang_Throwable_=new UTF8Sequence("Ljava/lang/Throwable;");
    public static final UTF8Sequence L=new UTF8Sequence("L");
    public static final UTF8Sequence LBRAC=new UTF8Sequence("[");
    public static final UTF8Sequence SEMI=new UTF8Sequence(";");
    public static final UTF8Sequence finalize=new UTF8Sequence("finalize");
    public static final UTF8Sequence Thunk=new UTF8Sequence("()V");
}

