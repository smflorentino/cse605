/*
 * FCNativePlainSocketImpl.java
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

package gnu.java.net;

import static java.net.SocketOptions.*;

import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;

import com.fiji.fivm.r1.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

@UsesMagic
final class FCNativePlainSocketImpl {
    private FCNativePlainSocketImpl() {}
    
    @NoInline
    @StackAllocation
    private static void setsockopt(int fd,int level,int option,int value)
	throws SocketException {
	libc.checkSocket(
	    libc.setsockopt(fd,level,option,
			    Magic.addressOfElement(new int[]{value},0),
			    4));
    }
    
    @NoInline
    @StackAllocation
    private static void setsockoptTime(int fd,int level,int option,long nanos)
	throws SocketException {
	libc.checkSocket(
	    libc.setsockopt(fd,level,option,
			    libc.newTimeval(nanos),
			    libc.timevalSize()));
    }
    
    @NoInline
    @StackAllocation
    private static void setsockoptLinger(int fd,int level,int option,int value)
	throws SocketException {
	Pointer linger=Magic.addressOfElement(
	    new byte[CType.sizeof("struct linger").castToInt()],0);
	if (value<0) {
	    CType.put(linger,"struct linger","l_onoff",(int)0);
	} else {
	    CType.put(linger,"struct linger","l_onoff",(int)1);
	    CType.put(linger,"struct linger","l_linger",(int)value);
	}
	libc.checkSocket(
	    libc.setsockopt(fd,level,option,
			    linger,
			    CType.sizeof("struct linger").castToInt()));
    }
    
    static void setOption(int fd,int id,int value) throws SocketException {
	switch (id) {
	case SO_KEEPALIVE:
	    setsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_LINGER"),value);
	    return;
	case SO_LINGER:
	    setsockoptLinger(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_LINGER"),value);
	    return;
	case SO_TIMEOUT:
	    setsockoptTime(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_RCVTIMEO"),
			   value*1000l*1000l);
	    return;
	case SO_SNDBUF:
	    setsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_SNDBUF"),value);
	    return;
	case SO_RCVBUF:
	    setsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_RCVBUF"),value);
	    return;
	case SO_REUSEADDR:
	    setsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_REUSEADDR"),value);
	    return;
	case SO_BROADCAST:
	    setsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_BROADCAST"),value);
	    return;
	case SO_OOBINLINE:
	    setsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_OOBINLINE"),value);
	    return;
	case TCP_NODELAY:
	    setsockopt(fd,CVar.getInt("IPPROTO_TCP"),CVar.getInt("TCP_NODELAY"),value);
	    return;
	case IP_TOS:
	    setsockopt(fd,CVar.getInt("IPPROTO_IP"),CVar.getInt("IP_TOS"),value);
	    return;
	default:
	    throw new SocketException("bad option id = "+id);
	}
    }
    
    @NoInline
    @StackAllocation
    private static int getsockopt(int fd,int level,int option) throws SocketException {
	int[] box=new int[1];
	libc.checkSocket(
	    libc.getsockopt(
		fd,level,option,
		Magic.addressOfElement(box,0),
		Magic.addressOfElement(new int[]{4},0)));
	return box[0];
    }
    
    @NoInline
    @StackAllocation
    private static long getsockoptTime(int fd,int level,int option)
	throws SocketException {
	Pointer time=libc.newTimeval();
	libc.checkSocket(
	    libc.getsockopt(
		fd,level,option,
		time,
		Magic.addressOfElement(new int[]{libc.timevalSize()},0)));
	return libc.timevalToNanotime(time);
    }
    
    @NoInline
    @StackAllocation
    private static int getsockoptLinger(int fd,int level,int option)
	throws SocketException {
	Pointer linger=Magic.addressOfElement(
	    new byte[CType.sizeof("struct linger").castToInt()],0);
	libc.checkSocket(
	    libc.getsockopt(
		fd,level,option,
		linger,
		Magic.addressOfElement(
		    new int[]{CType.sizeof("struct linger").castToInt()},0)));
	if (CType.getInt(linger,"struct linger","l_onoff")!=0) {
	    return CType.getInt(linger,"struct linger","l_linger");
	} else {
	    return -1;
	}
    }
    
    static int getOption(int fd,int id) throws SocketException {
	switch (id) {
	case SO_KEEPALIVE:
	    return getsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_LINGER"));
	case SO_LINGER:
	    return getsockoptLinger(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_LINGER"));
	case SO_TIMEOUT:
	    return (int)
		(getsockoptTime(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_RCVTIMEO"))
		 /1000l/1000l);
	case SO_SNDBUF:
	    return getsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_SNDBUF"));
	case SO_RCVBUF:
	    return getsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_RCVBUF"));
	case SO_REUSEADDR:
	    return getsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_REUSEADDR"));
	case SO_BROADCAST:
	    return getsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_BROADCAST"));
	case SO_OOBINLINE:
	    return getsockopt(fd,CVar.getInt("SOL_SOCKET"),CVar.getInt("SO_OOBINLINE"));
	case TCP_NODELAY:
	    return getsockopt(fd,CVar.getInt("IPPROTO_TCP"),CVar.getInt("TCP_NODELAY"));
	case IP_TOS:
	    return getsockopt(fd,CVar.getInt("IPPROTO_IP"),CVar.getInt("IP_TOS"));
	default:
	    throw new SocketException("bad option id = "+id);
	}
    }
    
    @StackAllocation
    static void bind(int fd,byte[] addr,int port) throws IOException {
        log(FCNativePlainSocketImpl.class,1,
            "Binding on "+fd+" with addr = "+Arrays.toString(addr)+
            ", port = "+port);
        Pointer addrPtr=FijiCoreCommon.makeSaAddr(addr,port);
        log(FCNativePlainSocketImpl.class,3,
            "Address of address = "+addrPtr.asLong());
	libc.checkIO(
	    libc.bind(fd,
		      addrPtr,
		      CType.sizeof("struct sockaddr_in").castToInt()));
    }
    
    @StackAllocation
    static void bind6(int fd,byte[] addr,int port) throws IOException {
	if (!CMacro.defined("HAVE_INET6")) {
	    throw new IOException("Inet6 not supported.");
	}
        log(FCNativePlainSocketImpl.class,1,
            "Binding on "+fd+" with addr = "+Arrays.toString(addr)+
            ", port = "+port);
	libc.checkIO(
	    libc.bind(fd,
		      FijiCoreCommon.makeSaAddr6(addr,port),
		      CType.sizeof("struct sockaddr_in6").castToInt()));
    }
    
    static void listen(int fd,int backlog) throws IOException {
        log(FCNativePlainSocketImpl.class,1,
            "Listening on "+fd+" with backlog = "+backlog);
	libc.checkIO(libc.listen(fd,backlog));
    }
    
    static void shutdownInput(int fd) throws IOException {
	libc.checkIO(libc.shutdown(fd,CVar.getInt("SHUT_RD")));
    }
    
    static void shutdownOutput(int fd) throws IOException {
	libc.checkIO(libc.shutdown(fd,CVar.getInt("SHUT_WR")));
    }
    
    @StackAllocation
    static void sendUrgentData(int fd,int data) throws IOException {
	libc.checkIO(
	    libc.send(fd,
		      Magic.addressOfElement(new byte[]{(byte)data},0),
		      Pointer.fromInt(1),
		      CVar.getInt("MSG_OOB")));
    }
}


