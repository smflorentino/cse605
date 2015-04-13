/*
 * FCNativeChannel.java
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
import java.net.*;
import java.nio.*;
import java.util.*;

import com.fiji.fivm.r1.*;

import static gnu.java.nio.FileChannelImpl.*;

import static com.fiji.fivm.r1.FijiCoreCommon.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

@UsesMagic
final class FCNativeChannel {
    private FCNativeChannel() {}
    
    static int stdin_fd() {
	return libc.stdin_fd();
    }
    
    static int stdout_fd() {
	return libc.stdout_fd();
    }
    
    static int stderr_fd() {
	return libc.stderr_fd();
    }
    
    static boolean setBlocking(int fd,boolean blocking) throws IOException {
	long value=libc.checkIO(libc.fcntl(fd,CVar.getInt("F_GETFL")));
	boolean oldBlocking=(value&CVar.getLong("O_NONBLOCK"))==0;
	if (blocking) {
	    value&=~CVar.getLong("O_NONBLOCK");
	} else {
	    value|=CVar.getLong("O_NONBLOCK");
	}
	libc.checkIO(libc.fcntl(fd,CVar.getInt("F_SETFL"),value));
	return oldBlocking;
    }
    
    @StackAllocation
    static int available(int fd) throws IOException {
	int[] result=new int[1];
	libc.checkIO(
	    libc.ioctl(fd,
		       CVar.getInt("FIONREAD"),
		       Magic.addressOfElement(result,0)));
	return result[0];
    }
    
    // FIXME: this looped read/write nonsense is nonsense!  we should not reloop
    // if any of the reads were incomplete.

    private static int EOF        = 0;
    private static int INCOMPLETE = 1;
    private static int COMPLETE   = 2;

    @StackAllocation 
    private static int readImpl(int fd,ByteBuffer dst_) throws IOException {
	ByteBuffer dst=java.nio.fivmSupport.makeVMBuffer(
            dst_,
            java.nio.fivmSupport.MakeVMBufferMode.LIMITED_SIZE,
            java.nio.fivmSupport.ReadWriteMode.WRITE_ONLY);
        int toRead=dst.remaining();
	int res=
	    libc.checkIO(
		libc.readIgnoreEINTR(
		    fd,
		    java.nio.fivmSupport.positionAddress(dst),
		    Pointer.fromInt(toRead))).castToInt();
        dst.position(dst.position()+res);
        if (dst!=dst_) {
            dst.flip();
            dst_.put(dst);
        }
        if (res==0) {
            return EOF;
        } else if (res<toRead) {
            return INCOMPLETE;
        } else {
            return COMPLETE;
        }
    }
    
    static int read(int fd,ByteBuffer dst) throws IOException {
        int startPos=dst.position();
        int res=INCOMPLETE;
        while (dst.hasRemaining()) {
            res=readImpl(fd,dst);
            if (res!=COMPLETE) {
                break;
            }
        }
        int bytesRead=dst.position()-startPos;
        if (bytesRead==0 && res==EOF) {
            return -1;
        } else {
            return bytesRead;
        }
    }
    
    static int read(int fd) throws IOException {
	int result=libc.readByteIgnoreEINTR(fd);
	if (result==-2) {
	    libc.throwIOException();
	}
	return result;
    }
    
    static long readScattering(int fd,ByteBuffer[] dsts,int offset,int length)
	throws IOException {
	// FIXME: optimize!!  use readv!
	
	long result=0;
	
	for (int i=offset;i<length;++i) {
	    int res=read(fd,dsts[i]);
	    if (res<0) {
		break;
	    }
	    result+=res;
	    if (dsts[i].remaining()>0) {
		break;
	    }
	}
	
	if (result==0) {
	    return -1;
	} else {
	    return result;
	}
    }
    
    private static int universalSaAddrSize() {
	if (CMacro.defined("HAVE_INET6")) {
	    return CType.sizeof("struct sockaddr_in6").castToInt();
	} else {
	    return CType.sizeof("struct sockaddr_in").castToInt();
	}
    }
    
    @AllocateAsCaller
    private static Pointer newUniversalSaAddr() {
	return Magic.addressOfElement(new byte[universalSaAddrSize()],0);
    }
    
    @AllocateAsCaller
    private static Pointer newUniversalSaAddrLen() {
	Pointer addressLen=Magic.addressOfElement(new int[1],0);
	addressLen.store((int)universalSaAddrSize());
	return addressLen;
    }
    
    private static InetSocketAddress makeISA(Pointer address) throws IOException {
	byte[] host;
	Pointer hostPtr;
	short port;
	int family=CType.getByte(address,"struct sockaddr","sa_family");
	if (CMacro.defined("HAVE_INET6") &&
	    family==CVar.getByte("AF_INET6")) {
	    host=new byte[16];
	    hostPtr=address.add(CType.offsetof("struct sockaddr_in6","sin6_addr"));
	    port=CType.getShort(address,"struct sockaddr_in6","sin6_port");
	} else if (family==CVar.getByte("AF_INET")) {
	    host=new byte[4];
	    hostPtr=address.add(CType.offsetof("struct sockaddr_in","sin_addr"));
	    port=CType.getShort(address,"struct sockaddr_in","sin_port");
            if (false) {
                log(FCNativeChannel.class,3,
                    "port = "+port+", flipped = "+libc.netEndianFlip(port));
            }
	} else {
	    throw new IOException("Bad sa_family: "+family);
	}
	for (int i=0;i<host.length;++i) {
	    host[i]=hostPtr.add(i).loadByte();
	}
        if (false) {
            log(FCNativeChannel.class,3,
                "host = "+Arrays.toString(host));
        }
	return new InetSocketAddress(InetAddress.getByAddress(host),
				     libc.netEndianFlip(port)&0xffff);
    }
    
    @StackAllocation
    static void recv(int fd,ByteBuffer dst_)
	throws IOException {
	ByteBuffer dst=java.nio.fivmSupport.makeVMBuffer(
            dst_,
            java.nio.fivmSupport.MakeVMBufferMode.UNLIMITED_SIZE,
            java.nio.fivmSupport.ReadWriteMode.WRITE_ONLY);
	int res=
	    libc.checkIO(
		libc.recvIgnoreEINTR(
		    fd,
		    java.nio.fivmSupport.positionAddress(dst),
		    Pointer.fromInt(dst.remaining()),
                    0)).castToInt();
	dst.position(dst.position()+res);
        if (dst!=dst_) {
            dst.flip();
            dst_.put(dst);
        }
    }
    
    @StackAllocation
    static InetSocketAddress recvfrom(int fd,ByteBuffer dst_)
	throws IOException {
	Pointer address=newUniversalSaAddr();
	Pointer addressLen=newUniversalSaAddrLen();
	ByteBuffer dst=java.nio.fivmSupport.makeVMBuffer(
            dst_,
            java.nio.fivmSupport.MakeVMBufferMode.UNLIMITED_SIZE,
            java.nio.fivmSupport.ReadWriteMode.WRITE_ONLY);
	int res=
	    libc.checkIO(
		libc.recvfromIgnoreEINTR(
		    fd,
		    java.nio.fivmSupport.positionAddress(dst),
		    Pointer.fromInt(dst.remaining()),
		    0,
		    address,
		    addressLen)).castToInt();
	dst.position(dst.position()+res);
        if (dst!=dst_) {
            dst.flip();
            dst_.put(dst);
        }
	return makeISA(address);
    }
    
    @StackAllocation
    private static int writeImpl(int fd,ByteBuffer src_) throws IOException {
	ByteBuffer src=java.nio.fivmSupport.makeVMBuffer(
            src_,
            java.nio.fivmSupport.MakeVMBufferMode.LIMITED_SIZE,
            java.nio.fivmSupport.ReadWriteMode.READ_ONLY);
        int toWrite=src.remaining();
	int res=
	    libc.checkIO(
		libc.writeIgnoreEINTR(
		    fd,
		    java.nio.fivmSupport.positionAddress(src),
		    Pointer.fromInt(toWrite))).castToInt();
	src_.position(src_.position()+res);
        if (res<toWrite) {
            return INCOMPLETE;
        } else {
            return COMPLETE;
        }
    }
    
    static int write(int fd,ByteBuffer src) throws IOException {
        int startPos=src.position();
        while (src.hasRemaining()) {
            int res=writeImpl(fd,src);
            if (res!=COMPLETE) {
                break;
            }
        }
        return src.position()-startPos;
    }

    static long writeGathering(int fd,ByteBuffer[] srcs,int offset,int length)
	throws IOException {
	// FIXME: optimize!!  use writev!
	
	long result=0;
	
	for (int i=offset;i<length;++i) {
	    result+=write(fd,srcs[i]);
	    if (srcs[i].remaining()>0) {
		break;
	    }
	}
	
	return result;
    }
    
    @StackAllocation
    static int send(int fd,ByteBuffer src_) throws IOException {
	ByteBuffer src=java.nio.fivmSupport.makeVMBuffer(
            src_,
            java.nio.fivmSupport.MakeVMBufferMode.UNLIMITED_SIZE,
            java.nio.fivmSupport.ReadWriteMode.READ_ONLY);
	int res=
	    libc.checkIO(
		libc.sendIgnoreEINTR(
		    fd,
		    java.nio.fivmSupport.positionAddress(src),
		    Pointer.fromInt(src.remaining()),
		    0)).castToInt();
	src_.position(src_.position()+res);
	return res;
    }
    
    @StackAllocation
    static int send(int fd,ByteBuffer src_,byte[] host,int port) throws IOException {
	ByteBuffer src=java.nio.fivmSupport.makeVMBuffer(
            src_,
            java.nio.fivmSupport.MakeVMBufferMode.UNLIMITED_SIZE,
            java.nio.fivmSupport.ReadWriteMode.READ_ONLY);
	int res=
	    libc.checkIO(
		libc.sendtoIgnoreEINTR(
		    fd,
		    java.nio.fivmSupport.positionAddress(src),
		    Pointer.fromInt(src.remaining()),
		    0,
		    makeSaAddr(host,port),
		    CType.sizeof("struct sockaddr_in").castToInt())).castToInt();
	src_.position(src_.position()+res);
	return res;
    }
    
    @StackAllocation
    static int send6(int fd,ByteBuffer src_,byte[] host,int port) throws IOException {
	if (!CMacro.defined("HAVE_INET6")) {
	    throw new IOException("Inet6 not supported.");
	}
	ByteBuffer src=java.nio.fivmSupport.makeVMBuffer(
            src_,
            java.nio.fivmSupport.MakeVMBufferMode.UNLIMITED_SIZE,
            java.nio.fivmSupport.ReadWriteMode.READ_ONLY);
	int res=
	    libc.checkIO(
		libc.sendtoIgnoreEINTR(
		    fd,
		    java.nio.fivmSupport.positionAddress(src),
		    Pointer.fromInt(src.remaining()),
		    0,
		    makeSaAddr(host,port),
		    CType.sizeof("struct sockaddr_in6").castToInt())).castToInt();
	src_.position(src_.position()+res);
	return res;
    }
    
    static void write(int fd,int b) throws IOException {
	libc.checkIO(libc.writeByteIgnoreEINTR(fd,(byte)b));
    }
    
    static int socket(boolean stream) throws IOException {
	return
	    libc.checkIO(
		libc.socket(
		    CVar.getInt("AF_INET"),
		    stream?CVar.getInt("SOCK_STREAM"):CVar.getInt("SOCK_DGRAM"),
		    0));
    }
    
    private static void throwSockTimeout() throws IOException {
        throw new SocketTimeoutException();
    }
    
    private static void throwConRefused() throws IOException {
        throw new ConnectException("Connection refused");
    }

    @StackAllocation
    static boolean connectImpl(int fd,Pointer addr,int addrLen,int timeout)
	throws IOException {
	boolean origBlock=false; // make javac happy
	if (timeout>0) {
	    origBlock=setBlocking(fd,false);
	}
	int errno;
	int res;
	for (;;) {
	    res=libc.connect(fd,addr,addrLen);
	    errno=libc.errno();
	    if (res!=-1 || errno!=libc.EINTR()) {
		break;
	    }
	}
	if (timeout>0) {
	    setBlocking(fd,origBlock);
	    if (res<0 && errno==CVar.getInt("EINPROGRESS")) {
		Pointer fdset=libc.newFdSet();
		libc.FD_ZERO(fdset);
		libc.FD_SET(fd,fdset);
		
		res=libc.checkIO(
		    libc.select(fd+1,
				Pointer.zero(),
				fdset,
				Pointer.zero(),
				libc.newTimeval(timeout*1000l*1000l)));
		
		if (res==0) {
                    throwSockTimeout();
		}
		
		return true;
	    }
	}
	
	if (res<0) {
	    if (errno==CVar.getInt("EINPROGRESS")) {
		return false;
	    } else if (errno==CVar.getInt("ECONNREFUSED")) {
                throwConRefused();
	    }
	    libc.throwIOException(errno);
	}
	
	return true;
    }
    
    @StackAllocation
    static boolean connect(int fd,byte[] addr,int port,int timeout) throws IOException {
	return connectImpl(fd,makeSaAddr(addr,port),CType.sizeof("struct sockaddr_in").castToInt(),timeout);
    }
    
    @StackAllocation
    static boolean connect6(int fd,byte[] addr,int port,int timeout) throws IOException {
	if (!CMacro.defined("HAVE_INET6")) {
	    throw new IOException("Inet6 not supported.");
	}
	return connectImpl(fd,makeSaAddr6(addr,port),CType.sizeof("struct sockaddr_in6").castToInt(),timeout);
    }
    
    @StackAllocation
    static void disconnect(int fd) throws IOException {
	Pointer sockaddr=Magic.addressOfElement(
	    new byte[CType.sizeof("struct sockaddr").castToInt()],0);
	CType.put(sockaddr,"struct sockaddr","sa_family",(byte)CVar.getInt("AF_UNSPEC"));
	if (libc.connect(fd,sockaddr,CType.sizeof("struct sockaddr").castToInt())==0) {
	    throw new fivmError("connect() returned 0 on disconnect()");
	}
	if (libc.errno()!=CVar.getInt("EAFNOSUPPORT")) {
	    libc.throwIOException();
	}
    }
    
    @StackAllocation
    static InetSocketAddress getsockname(int fd) throws IOException {
	Pointer address=newUniversalSaAddr();
	Pointer addressLen=newUniversalSaAddrLen();
	libc.checkIO(libc.getsockname(fd,address,addressLen));
	return makeISA(address);
    }
    
    @StackAllocation
    static InetSocketAddress getpeername(int fd) throws IOException {
	Pointer address=newUniversalSaAddr();
	Pointer addressLen=newUniversalSaAddrLen();
        log(FCNativeChannel.class,3,
            "Address = "+address.asLong()+", addressLen = "+addressLen.asLong());
	libc.checkIO(libc.getpeername(fd,address,addressLen));
	return makeISA(address);
    }
    
    @StackAllocation
    static int accept(int fd) throws IOException {
	Pointer address=newUniversalSaAddr();
	Pointer addressLen=newUniversalSaAddrLen();
	int res;
	int errno;
	for (;;) {
            log(FCNativeChannel.class,1,
                "Accepting on "+fd);
	    res=libc.accept(fd,address,addressLen);
	    errno=libc.errno();
	    if (res>=0 || errno!=libc.EINTR()) {
		break;
	    }
	}
	if (res>=0) {
	    return res;
	}
	if (errno==CVar.getInt("EAGAIN") ||
	    (CMacro.defined("EWOULDBLOCK") && errno==CVar.getInt("EWOULDBLOCK"))) {
	    throw new SocketTimeoutException();
	}
	throw new SocketException("Errno = "+errno);
    }
    
    static int open(String path,int mode) throws IOException {
	// mode comes from the mode flags in FileChannelImpl
	
	int flags=0;
	
	if ((mode&(READ|WRITE))==(READ|WRITE)) {
	    flags|=CVar.getInt("O_RDWR");
	} else if ((mode&READ)==READ) {
	    flags|=CVar.getInt("O_RDONLY");
	} else if ((mode&WRITE)==WRITE) {
	    flags|=CVar.getInt("O_WRONLY");
	} else {
	    throw new fivmError("bad mode: "+mode);
	}
	
	if ((mode&WRITE)==WRITE) {
	    flags|=CVar.getInt("O_CREAT");
	    if ((mode&APPEND)==APPEND) {
		flags|=CVar.getInt("O_APPEND");
	    } else if ((mode&READ)==0) {
		flags|=CVar.getInt("O_TRUNC");
	    }
	}
	
	if ((mode&EXCL)==EXCL) {
	    flags|=CVar.getInt("O_EXCL");
	}
	
	if (CMacro.defined("O_SYNC") && (mode&SYNC)==SYNC) {
	    flags|=CVar.getInt("O_SYNC");
	}
	
	return libc.checkIO(libc.open(path,flags,0666));
    }
    
    static long position(int fd) throws IOException {
	return libc.checkIO(libc.lseek(fd,0l,CVar.getInt("SEEK_CUR")));
    }
    
    static void seek(int fd,long pos) throws IOException {
	libc.checkIO(libc.lseek(fd,pos,CVar.getInt("SEEK_SET")));
    }
    
    static long size(int fd) throws IOException {
	long oldPos=position(fd);
	try {
	    return libc.checkIO(libc.lseek(fd,0,CVar.getInt("SEEK_END")));
	} finally {
	    seek(fd,oldPos);
	}
    }
    
    static void truncate(int fd,long len) throws IOException {
	libc.checkIO(libc.ftruncate(fd,len));
    }
    
    static boolean lock(int fd,long pos, long len, boolean shared, boolean wait)
	throws IOException {
	throw new IOException("unsupported");
    }
    
    static void unlock(int fd, long pos, long len) throws IOException {
	throw new IOException("unsupported");
    }
    
    static MappedByteBuffer map(int fd,char mode,long position,int size)
	throws IOException {
	throw new IOException("unsupported");
    }
    
    static boolean flush(int fd,boolean metadata) throws IOException {
	libc.checkIO(libc.fsync(fd));
	return true;
    }
    
    static void close(int fd) throws IOException {
	libc.checkIO(libc.close(fd));
    }
}

