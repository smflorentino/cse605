/*
 * FijiCoreCommon.java
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

package com.fiji.fivm.r1;

/**
 * Catch-all for stuff needed to support FCXYZ classes, that needs to be common
 * to all of them, and doesn't belong anywhere else.
 */
public class FijiCoreCommon {
    private FijiCoreCommon() {}

    @AllocateAsCaller
    public static Pointer makeSaAddr(byte[] host,int port) {
	Pointer addr=Magic.addressOfElement(new byte[CType.sizeof("struct sockaddr_in").castToInt()],0);
	CType.put(addr,"struct sockaddr_in","sin_family",(byte)CVar.getInt("AF_INET"));
	CType.put(addr,"struct sockaddr_in","sin_port",libc.netEndianFlip((short)port));
	Pointer rawHost=addr.add(CType.offsetof("struct sockaddr_in","sin_addr"));
	for (int i=0;i<4;++i) {
	    rawHost.add(i).store(host[i]);
	}
	return addr;
    }
    
    @AllocateAsCaller
    public static Pointer makeSaAddr6(byte[] host,int port) {
	if (!CMacro.defined("HAVE_INET6")) {
	    throw new fivmError("Inet6 not supported.");
	}
	Pointer addr=Magic.addressOfElement(new byte[CType.sizeof("struct sockaddr_in6").castToInt()],0);
	CType.put(addr,"struct sockaddr_in6","sin6_family",(byte)CVar.getInt("AF_INET"));
	CType.put(addr,"struct sockaddr_in6","sin6_port",libc.netEndianFlip((short)port));
	Pointer rawHost=addr.add(CType.offsetof("struct sockaddr_in6","sin6_addr"));
	for (int i=0;i<16;++i) {
	    rawHost.add(i).store(host[i]);
	}
	return addr;
    }
    
}

