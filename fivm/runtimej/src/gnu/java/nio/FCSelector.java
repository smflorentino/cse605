/*
 * FCSelector.java
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

package gnu.java.nio;

import java.io.*;

import com.fiji.fivm.r1.*;

@UsesMagic
public final class FCSelector {
    @AllocateAsCaller
    private static Pointer makeFdSet(int[] fds) {
	if (fds.length==0) {
	    return Pointer.zero();
	} else {
	    return libc.newFdSet();
	}
    }
    
    private static void setFdSet(int[] fds,Pointer fdSet) {
	if (fdSet!=Pointer.zero()) {
	    libc.FD_ZERO(fdSet);
	    for (int i=0;i<fds.length;++i) {
		libc.FD_SET(fds[i],fdSet);
	    }
	}
    }
    
    private static void getFdSet(int[] fds,Pointer fdSet) {
	for (int i=0;i<fds.length;++i) {
	    if (fdSet==Pointer.zero() || !libc.FD_ISSET(fds[i],fdSet)) {
		fds[i]=-1;
	    }
	}
    }
    
    @StackAllocation
    static int select(int[] read,int[] write,int[] except,long timeout)
	throws IOException {
	Pointer readSet=makeFdSet(read);
	Pointer writeSet=makeFdSet(write);
	Pointer exceptSet=makeFdSet(except);
	int res;
	for (;;) {
	    setFdSet(read,readSet);
	    setFdSet(write,writeSet);
	    setFdSet(except,exceptSet);
	    res=libc.select(
		Math.max(read.length,Math.max(write.length,except.length))+1,
		readSet,writeSet,exceptSet,
		libc.newTimeval(timeout*1000l*1000l));
	    if (res>=0 || libc.errno()!=libc.EINTR()) {
		break;
	    }
	}
	libc.checkIO(res);
	getFdSet(read,readSet);
	getFdSet(write,writeSet);
	getFdSet(except,exceptSet);
	return res;
    }
}


