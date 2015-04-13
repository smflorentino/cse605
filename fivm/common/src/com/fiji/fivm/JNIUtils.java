/*
 * JNIUtils.java
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

package com.fiji.fivm;

import java.io.IOException;

public class JNIUtils {
    private JNIUtils() {}
    
    public static char toHexDigit(int value) {
        if (value<10) {
            return (char)('0'+(char)value);
        } else {
            // ????? do we use lower-case or upper-case?  JNI spec doesn't
            // say.  because the JNI spec is horribly written.
            return (char)('a'+(char)(value-10));
        }
    }

    /**
     * Perform JNI escaping without allocating memory.
     */
    public static void jniEscape(Appendable result,String str,
                                 int start,int end) {
        try {
            for (int i=start;i<end;++i) {
                char c=str.charAt(i);
                switch (c) {
                case '/': result.append("_"); break;
                case '_': result.append("_1"); break;
                case ';': result.append("_2"); break;
                case '[': result.append("_3"); break;
                default: 
                    if ((c>='a' && c<='z') ||
                        (c>='A' && c<='Z') ||
                        (c>='0' && c<='9')) {
                        result.append(c);
                    } else {
                        result.append("_0");
                        
                        // I know.  This code could be nicer.  But it's written
                        // in such a way that it doesn't allocate.
                        
                        char one = toHexDigit(c%16);  c/=16;
                        char two = toHexDigit(c%16);  c/=16;
                        char thr = toHexDigit(c%16);  c/=16;
                        char fou = toHexDigit(c%16);  c/=16;
                        
                        result.append(fou);
                        result.append(thr);
                        result.append(two);
                        result.append(one);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }
    
    public static void jniEscape(Appendable target,
                                 String str) {
        jniEscape(target,str,0,str.length());
    }

    public static String jniEscape(String str) {
	StringBuilder result=new StringBuilder();
        jniEscape(result,str);
	return result.toString();
    }
}

