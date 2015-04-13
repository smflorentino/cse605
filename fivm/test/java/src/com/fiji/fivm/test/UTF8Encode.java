/*
 * UTF8Encode.java
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

package com.fiji.fivm.test;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;
import com.fiji.util.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import javax.realtime.*;

public class UTF8Encode {
    static CharBuffer readCharBuffer(String flnm) throws Exception {
        return ByteBuffer.wrap(FileUtils.readCompletely(flnm)).asCharBuffer();
    }
    
    static char[] readCharArray(String flnm) throws Exception {
        CharBuffer cb=readCharBuffer(flnm);
        char[] result=new char[cb.remaining()];
        cb.get(result);
        return result;
    }
    
    static String readString(String flnm) throws Exception {
        return new String(readCharArray(flnm));
    }
    
    static byte[] testField;
    
    @StackAllocation
    static void checkStack(char[] input,byte[] expectedOutput) {
        byte[] stackMethod=UTF8.encode(input);
        Util.ensure(Arrays.equals(stackMethod,expectedOutput));
        try {
            testField=stackMethod;
            Util.ensure(false);
        } catch (IllegalAssignmentError e) {
            // yay!
        }
    }
    
    public static void main(String[] v) throws Exception {
        for (int i=0;i<v.length;i+=2) {
            String inpFlnm=v[i+0];
            String outFlnm=v[i+1];
            
            System.out.println("Testing file "+inpFlnm+" and outputting to "+outFlnm);
            
            // first internally verify that all of the different mechanisms for doing the
            // conversion lead to identical results
            
            // String method
            byte[] stringMethod=readString(inpFlnm).getBytes("UTF-8");
            
            // NIO byte buffer method
            CharsetEncoder enc=Charset.forName("UTF-8").newEncoder();
            enc.onMalformedInput(CodingErrorAction.REPLACE);
            enc.onUnmappableCharacter(CodingErrorAction.REPLACE);
            ByteBuffer result=enc.encode(readCharBuffer(inpFlnm));
            byte[] nioMethod=new byte[result.remaining()];
            result.get(nioMethod);
            
            // our own method
            byte[] ourMethod=UTF8.encode(readCharArray(inpFlnm));
            
            Util.ensure(Arrays.equals(stringMethod,nioMethod));
            Util.ensure(Arrays.equals(stringMethod,ourMethod));
            
            if (System.getProperty("java.vm.name").equals("fivm")) {
                System.out.println("   detected Fiji VM; testing stack allocation");
                checkStack(readCharArray(inpFlnm),ourMethod);
            }
            
            FileUtils.writeCompletely(outFlnm,ourMethod);
            
            System.out.println("   UTF8 encoding is internally consistent; output writen to "+outFlnm);
        }
    }
}

