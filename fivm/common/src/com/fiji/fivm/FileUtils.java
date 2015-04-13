/*
 * FileUtils.java
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

import java.io.*;
import java.util.*;

public final class FileUtils {
    private FileUtils() {}

    public static int readCompletely(InputStream in,byte[] data)
	throws IOException {
	int cnt=0;
	while (cnt<data.length) {
	    int res=in.read(data,cnt,data.length-cnt);
	    if (res<0) {
		break;
	    }
	    cnt+=res;
	}
	return cnt;
    }
    
    public static byte[] readCompletely(InputStream in) throws IOException {
	LinkedList< byte[] > chain=new LinkedList< byte[] >();
	for (;;) {
	    byte[] data=new byte[4096];
	    int cnt=readCompletely(in,data);
	    if (cnt<data.length) {
		byte[] result=new byte[chain.size()*data.length+cnt];
		int offset=0;
		for (byte[] ele : chain) {
		    System.arraycopy(ele,0,
				     result,offset,
				     ele.length);
		    offset+=ele.length;
		}
		System.arraycopy(data,0,
				 result,offset,
				 cnt);
		return result;
	    }
	    chain.add(data);
	}
    }
    
    public static byte[] readCompletely(String flnm) throws IOException {
	FileInputStream flin=new FileInputStream(flnm);
	try {
	    return readCompletely(flin);
	} finally {
	    flin.close();
	}
    }
    
    public static byte[] readCompletely(File flnm) throws IOException {
        return readCompletely(flnm.toString());
    }
    
    public static void writeCompletely(String flnm,byte[] data) throws IOException {
        FileOutputStream flout=new FileOutputStream(flnm);
        try {
            flout.write(data);
        } finally {
            flout.close();
        }
    }
}

