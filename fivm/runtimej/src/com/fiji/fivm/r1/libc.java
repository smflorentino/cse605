/*
 * libc.java
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

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.fiji.fivm.*;

@UsesMagic
public class libc {
    private libc() {}
    
    public static class Safe {
	private Safe() {}
	
	@NoInline
	@Import
	@GodGiven
	@TrustedGodGiven
	@NoThrow
	@NoNativeFrame
	@UseObjectsNotHandles
	public static native void memcpy(Pointer to,
					 Pointer from,
					 Pointer size);

	@NoInline
	@Import
	@GodGiven
        @TrustedGodGiven
	@NoThrow
	@NoNativeFrame
	@UseObjectsNotHandles
	public static native void memmove(Pointer to,
					  Pointer from,
					  Pointer size);
	
	@NoInline
	@Import
	@GodGiven
        @TrustedGodGiven
	@NoThrow
	@NoNativeFrame
	@UseObjectsNotHandles
	public static native void bzero(Pointer ptr,
					Pointer size);
    }
    
    public static class Fast {
	private Fast() {}
	
	@Inline
	public static void memcpy(Pointer to,
                                  Pointer from,
                                  Pointer size) {
            Magic.memcpy(to,from,size);
        }

	@Inline
	@Import
	@GodGiven
        @TrustedGodGiven
	@NoThrow
	@NoExecStatusTransition
	@NoNativeFrame
	@UseObjectsNotHandles
	@NoSafepoint
	public static native void memmove(Pointer to,
					  Pointer from,
					  Pointer size);

	@Inline
	@Import
	@GodGiven
        @TrustedGodGiven
	@NoThrow
	@NoExecStatusTransition
	@NoNativeFrame
	@UseObjectsNotHandles
	@NoSafepoint
	public static native void bzero(Pointer ptr,
					Pointer size);
    }
    
    private static final int THRESHOLD = 4096;
    
    @Inline
    public static void memcpy(Pointer to,
			      Pointer from,
			      Pointer size) {
	if (size.lessThan(THRESHOLD)) {
	    Fast.memcpy(to,from,size);
	} else {
	    Safe.memcpy(to,from,size);
	}
    }
    
    @NoInline
    @NoThrow
    @NoStackOverflowCheck
    public static void memmoveNoInline(Pointer to,
				       Pointer from,
				       Pointer size) {
	memmove(to,from,size);
    }
    
    @Inline
    public static void memmove(Pointer to,
			       Pointer from,
			       Pointer size) {
	if (size.lessThan(THRESHOLD)) {
	    Fast.memmove(to,from,size);
	} else {
	    Safe.memmove(to,from,size);
	}
    }
    
    @NoInline
    @NoThrow
    @NoStackOverflowCheck
    private static void memcpyOrMoveSlowImpl(boolean noOverlap,
					     Pointer to,
					     Pointer from,
					     Pointer size) {
	if (noOverlap) {
	    Safe.memcpy(to,from,size);
	} else {
	    memmove(to,from,size);
	}
    }
    
    @Inline
    @NoPollcheck
    @AllowUnsafe
    public static void memcpyOrMove(boolean noOverlap,
				    Pointer to,
				    Pointer from,
				    Pointer size) {
	if (noOverlap && size.lessThan(THRESHOLD)) {
	    Fast.memcpy(to,from,size);
	} else {
	    memcpyOrMoveSlowImpl(noOverlap,to,from,size);
	}
    }
    
    @Inline
    public static void copyToByteArray(byte[] array,int offset,int length,
                                       Pointer ptr) {
        if (MM.contiguousArray(array)) {
            memcpy(Magic.addressOfElement(array,offset),
                        ptr,
                        Pointer.fromIntZeroFill(length));
        } else {
            for (int i=0;i<length;++i) {
                array[offset+i]=ptr.add(i).loadByte();
            }
        }
    }
    
    @Inline
    public static void copyFromByteArray(byte[] array,int offset,int length,
                                         Pointer ptr) {
        if (MM.contiguousArray(array)) {
            libc.memcpy(ptr,
                        Magic.addressOfElement(array,offset),
                        Pointer.fromIntZeroFill(length));
        } else {
            for (int i=0;i<length;++i) {
                ptr.add(i).store((byte)array[offset+i]);
            }
        }
    }
    
    @Inline
    @NoPollcheck
    @AllowUnsafe
    public static void bzero(Pointer ptr,
			     Pointer size) {
	if (size.lessThan(THRESHOLD)) {
	    Fast.bzero(ptr,size);
	} else {
	    Safe.bzero(ptr,size);
	}
    }
    
    @RuntimeImport
    public static native int strcmp(Pointer a,Pointer b);
    
    // NOTE: using these functions - especiall write() - is really dangerous.
    // consider the following code:
    //
    // int myWrite(int fd, byte[] buf_, int size_) {
    //    Pointer buf=Pointer.fromObject(buf_);
    //    Pointer size=Pointer.fromInt(size_);
    //    return write(fd,buf,size).castToInt();
    // }
    //
    // The danger, if myWrite() gets inlined, is that at write(), buf_ will no
    // longer be live.  Since write() is a safepoint, that's really bad.  Hence,
    // we need Magic.hardUse() as a work-around.  Put a hardUse() right after
    // the call to write() and everyone will be happy.
    
    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    public static native Pointer write(int fd,Pointer buf,Pointer size);
    
    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    public static native Pointer read(int fd,Pointer buf,Pointer size);

    @Inline
    @Import
    @GodGiven
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_SEND"})
    public static native Pointer send(int fd,Pointer buf,Pointer size,int flags);
    
    @Inline
    @Import
    @GodGiven
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_RECV"})
    public static native Pointer recv(int fd,Pointer buf,Pointer size,int flags);

    @Inline
    public static int errno() {
	return CVar.getInt("errno");
    }
    
    @Inline
    public static int EINTR() {
	return CVar.getInt("EINTR");
    }

    
    @Inline
    public static int AF_INET() {
	return CVar.getInt("AF_INET");
    }
    
    @Inline
    public static int AF_INET6() {
	return CVar.getInt("AF_INET6");
    }

    @Inline
    public static int INADDR_ANY() {
	return CVar.getInt("INADDR_ANY");
    }

    
    @NoInline
    @NoReturn
    public static void throwIOException(int errno) throws IOException {
	throw new IOException("Errno = "+errno);
    }
    
    @NoInline
    @NoReturn
    public static void throwIOException() throws IOException {
	throwIOException(errno());
    }
    
    @NoInline
    @NoReturn
    public static void throwSocketException(int errno) throws SocketException {
	throw new SocketException("Errno = "+errno);
    }
    
    @NoInline
    @NoReturn
    public static void throwSocketException() throws SocketException {
	throwSocketException(errno());
    }
    
    @Inline
    public static int checkIO(int res) throws IOException {
	if (res<0) {
	    throwIOException();
	}
	return res;
    }

    @Inline
    public static long checkIO(long res) throws IOException {
	if (res<0) {
	    throwIOException();
	}
	return res;
    }

    @Inline
    public static Pointer checkIO(Pointer res) throws IOException {
	if (res.signedLessThan(Pointer.zero())) {
	    throwIOException();
	}
	return res;
    }

    @Inline
    public static int checkSocket(int res) throws SocketException {
	if (res<0) {
	    throwSocketException();
	}
	return res;
    }

    // some helpers

    @Inline
    @NoSafetyChecks
    public static Pointer writeIgnoreEINTR(int fd,Pointer buf,Pointer size) {
	for (;;) {
	    Pointer res=write(fd,buf,size);
	    if (res.signedLessThan(0) && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static Pointer readIgnoreEINTR(int fd,Pointer buf,Pointer size) {
	for (;;) {
	    Pointer res=read(fd,buf,size);
	    if (res.signedLessThan(0) && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static Pointer sendIgnoreEINTR(int fd,Pointer buf,Pointer size,int flags) {
	for (;;) {
	    Pointer res=send(fd,buf,size,flags);
	    if (res.signedLessThan(0) && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static Pointer recvIgnoreEINTR(int fd,Pointer buf,Pointer size,int flags) {
	for (;;) {
	    Pointer res=recv(fd,buf,size,flags);
	    if (res.signedLessThan(0) && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @Inline
    @NoSafetyChecks
    public static Pointer write(int fd,byte[] array,int offset,int size) {
	if (!MM.contiguousArray(array) ||
	    !MM.willNeverMove(array)) {
	    throw new fivmError("Invalid use of write()");
	}

	Pointer result=write(fd,
			     Magic.addressOfElement(array,offset),
			     Pointer.fromInt(size));
	
	Magic.hardUse(array); /* keep the array alive across the call */
	
	return result;
    }
    
    @Inline
    @NoSafetyChecks
    public static Pointer read(int fd,byte[] array,int offset,int size) {
	if (!MM.contiguousArray(array) ||
	    !MM.willNeverMove(array)) {
	    throw new fivmError("Invalid use of read()");
	}

	Pointer result=read(fd,
			    Magic.addressOfElement(array,offset),
			    Pointer.fromInt(size));
	
	Magic.hardUse(array); /* keep the array alive across the call */
	
	return result;
    }
    
    @Inline
    public static Pointer writeIgnoreEINTR(int fd,byte[] array,int offset,int size) {
	for (;;) {
	    Pointer res=write(fd,array,offset,size);
	    if (res.signedLessThan(0) && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @Inline
    public static Pointer readIgnoreEINTR(int fd,byte[] array,int offset,int size) {
	for (;;) {
	    Pointer res=read(fd,array,offset,size);
	    if (res.signedLessThan(0) && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    private static native int fivmr_readByte(int fd);

    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    private static native int fivmr_writeByte(int fd,byte b);
    
    /** @return -2 on error, -1 on EOF, otherwise the byte */
    @Inline
    public static int readByte(int fd) {
	return fivmr_readByte(fd);
    }
    
    @Inline
    public static int writeByte(int fd,byte b) {
	return fivmr_writeByte(fd,b);
    }
    
    @Inline
    public static int readByteIgnoreEINTR(int fd) {
	for (;;) {
	    int res=readByte(fd);
	    if (res==-2 && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @Inline
    public static int writeByteIgnoreEINTR(int fd,byte b) {
	for (;;) {
	    int res=writeByte(fd,b);
	    if (res<0 && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_RECVFROM"})
    public static native Pointer recvfrom(int fd,Pointer buf,Pointer length,
					  int flags,
					  Pointer address,Pointer addressLenPtr);
    
    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_SENDTO"})
    public static native Pointer sendto(int fd,Pointer buf,Pointer length,
					int flags,
					Pointer address,int addressLen);
    
    @IfPoundDefined({"HAVE_RECVFROM"})
    public static Pointer recvfromIgnoreEINTR(int fd,Pointer buf,Pointer length,
					      int flags,
					      Pointer address,Pointer addressLenPtr) {
	for (;;) {
	    Pointer res=recvfrom(fd,buf,length,flags,address,addressLenPtr);
	    if (res.signedLessThan(0) && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @IfPoundDefined({"HAVE_SENDTO"})
    public static Pointer sendtoIgnoreEINTR(int fd,Pointer buf,Pointer length,
					    int flags,
					    Pointer address,int addressLen) {
	for (;;) {
	    Pointer res=sendto(fd,buf,length,flags,address,addressLen);
	    if (res.signedLessThan(0) && errno()==EINTR()) {
		continue;
	    } else {
		return res;
	    }
	}
    }
    
    @AllocateAsCaller
    @IfPoundDefined({"HAVE_SYS_SELECT_H","HAVE_SYS_TYPES_H","HAVE_SELECT"})
    public static Pointer newFdSet() {
	return MM.indexableStartOfArray(
	    new byte[CType.sizeof("fd_set").castToInt()]);
    }
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_SYS_SELECT_H","HAVE_SYS_TYPES_H","HAVE_SELECT"})
    public static native void fivmr_FD_CLR(int fd,Pointer fdset);
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_SYS_SELECT_H","HAVE_SYS_TYPES_H","HAVE_SELECT"})
    public static native boolean fivmr_FD_ISSET(int fd,Pointer fdset);
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_SYS_SELECT_H","HAVE_SYS_TYPES_H","HAVE_SELECT"})
    public static native void fivmr_FD_SET(int fd,Pointer fdset);
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_SYS_SELECT_H","HAVE_SYS_TYPES_H","HAVE_SELECT"})
    public static native void fivmr_FD_ZERO(Pointer fdset);
    
    public static void FD_CLR(int fd,Pointer fdset) {
	fivmr_FD_CLR(fd,fdset);
    }
    
    public static boolean FD_ISSET(int fd,Pointer fdset) {
	return fivmr_FD_ISSET(fd,fdset);
    }
    
    public static void FD_SET(int fd,Pointer fdset) {
	fivmr_FD_SET(fd,fdset);
    }
    
    public static void FD_ZERO(Pointer fdset) {
	fivmr_FD_ZERO(fdset);
    }
    
    public static int timevalSize() {
	return CType.sizeof("struct timeval").castToInt();
    }
    
    @AllocateAsCaller
    public static Pointer newTimeval() {
	return MM.indexableStartOfArray(
	    new byte[CType.sizeof("struct timeval").castToInt()]);
    }
    
    public static void nanotimeToTimeval(Pointer timeval,
					 long nanos) {
	CType.put(timeval,"struct timeval","tv_sec",(int)(nanos/1000l/1000l/1000l));
	CType.put(timeval,"struct timeval","tv_usec",(int)(nanos/1000l%(1000l*1000l)));
    }
    
    public static long timevalToNanotime(Pointer timeval) {
	long nanos=CType.getInt(timeval,"struct timeval","tv_sec");
	nanos*=1000l;
	nanos*=1000l;
	nanos+=CType.getInt(timeval,"struct timeval","tv_usec");
	nanos*=1000l;
	return nanos;
    }
    
    @AllocateAsCaller
    public static Pointer newTimeval(long nanos) {
	Pointer result=newTimeval();
	nanotimeToTimeval(result,nanos);
	return result;
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_SYS_SELECT_H","HAVE_SYS_TYPES_H","HAVE_SELECT"})
    public static native int select(int nfds,
				    Pointer readfds,
				    Pointer writefds,
				    Pointer errorfds,
				    Pointer timeout);
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_FCNTL_H","HAVE_FCNTL"})
    private static native long fivmr_fcntl(int fd,int cmd,long optArg1);
    
    public static long fcntl(int fd,int cmd,long arg) {
	return fivmr_fcntl(fd,cmd,arg);
    }
    
    public static long fcntl(int fd,int cmd) {
	return fivmr_fcntl(fd,cmd,0);
    }
	
    @RuntimeImport
    @IfPoundDefined({"HAVE_SYS_IOCTL_H","HAVE_IOCTL"})
    private static native int fivmr_ioctl_ptr(int fd,int cmd,Pointer ptr);
	
    @RuntimeImport
    @IfPoundDefined({"HAVE_SYS_IOCTL_H","HAVE_IOCTL"})
    private static native int fivmr_ioctl_void(int fd,int cmd);
	
    @RuntimeImport
    @IfPoundDefined({"HAVE_SYS_IOCTL_H","HAVE_IOCTL"})
    private static native int fivmr_ioctl_int(int fd,int cmd,int val);
	
    public static int ioctl(int fd,int cmd) {
	return fivmr_ioctl_void(fd,cmd);
    }
	
    public static int ioctl(int fd,int cmd,Pointer ptr) {
	return fivmr_ioctl_ptr(fd,cmd,ptr);
    }
	
    public static int ioctl(int fd,int cmd,int val) {
	return fivmr_ioctl_int(fd,cmd,val);
    }
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_SOCKET"})
    public static native int socket(int domain,int type,int protocol);
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_CONNECT"})
    public static native int connect(int socket,Pointer addr,int addrLen);
	
    @RuntimeImport
    @IfPoundDefined({"HAVE_GETSOCKNAME"})
    public static native int getsockname(int socket,Pointer addr,Pointer addrLen);
	
    @RuntimeImport
    @IfPoundDefined({"HAVE_GETPEERNAME"})
    public static native int getpeername(int socket,Pointer addr,Pointer addrLen);
	
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_ACCEPT"})
    public static native int accept(int fd,Pointer addr,Pointer addrLen);
	
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    public static native int open(Pointer filename,int flags,int mode);
    
    @NoInline
    @StackAllocation
    public static int open(String filename,int flags,int mode) {
	Pointer str=fivmRuntime.getCStringFullStack(filename);
	return open(str,flags,mode);
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    public static native long lseek(int fd,long offset,int whence);
	
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    public static native long ftruncate(int fd,long size);
	
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_FSYNC"})
    public static native int fsync(int fd);
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    public static native int close(int fd);
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_SETSOCKOPT"})
    public static native int setsockopt(int fd,int level,int option,
					Pointer value,int length);

    @RuntimeImport
    @IfPoundDefined({"HAVE_GETSOCKOPT"})
    public static native int getsockopt(int fd,int level,int option,
					Pointer value,Pointer length);
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_BIND"})
    public static native int fivmr_bind(int fd,Pointer addr,int addrLen);
    
    public static int bind(int fd,Pointer addr,int addrLen) {
	return fivmr_bind(fd,addr,addrLen);
    }
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_LISTEN"})
    public static native int listen(int sock,int backlog);
    
    @RuntimeImport
    @IfPoundDefined({"HAVE_SHUTDOWN"})
    public static native int shutdown(int fd,int how);
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_ACCESS"})
    public static native int access(Pointer path,int mode);
    
    @NoInline // FIXME it's NoInline but it's not called rarely!
    @StackAllocation
    public static int access(String path,int mode) {
	Pointer str=fivmRuntime.getCStringFullStack(path);
	return access(str,mode);
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_STAT","HAVE_SYS_STAT_H"})
    public static native int stat(Pointer path,Pointer statBuf);
    
    @NoInline
    @StackAllocation
    public static int stat(String path,Pointer statBuf) {
	Pointer str=fivmRuntime.getCStringFullStack(path);
	return stat(str,statBuf);
    }
    
    @StackAllocation
    public static int statGetMode(Pointer path) {
	Pointer statBuf=MM.indexableStartOfArray(
	    new byte[CType.sizeof("struct stat").castToInt()]);
	if (stat(path,statBuf)<0) {
	    return -1;
	}
	return CType.getInt(statBuf,"struct stat","st_mode");
    }
    
    @NoInline
    @StackAllocation
    public static int statGetMode(String path) {
	Pointer str=fivmRuntime.getCStringFullStack(path);
	return statGetMode(str);
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_UNLINK"})
    public static native int unlink(Pointer path);
    
    @NoInline
    @StackAllocation
    public static int unlink(String path) {
	Pointer str=fivmRuntime.getCStringFullStack(path);
	return unlink(str);
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_DIRENT_H","HAVE_OPENDIR"})
    public static native Pointer opendir(Pointer filename);
    
    @StackAllocation
    public static Pointer opendir(String path) {
	Pointer str=fivmRuntime.getCStringFullStack(path);
	return opendir(str);
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_DIRENT_H","HAVE_READDIR_R"})
    public static native int readdir_r(Pointer dirp,Pointer entry,Pointer result);
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_DIRENT_H","HAVE_CLOSEDIR"})
    public static native int closedir(Pointer dirp);
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_REALPATH"})
    public static native Pointer realpath(Pointer orig,Pointer resolved);
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_MKDIR"})
    public static native int mkdir(Pointer path,int mode);
    
    @NoInline
    @StackAllocation
    public static int mkdir(String path,int mode) {
	Pointer str=fivmRuntime.getCStringFullStack(path);
	return mkdir(str,mode);
    }

    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_CHMOD"})
    public static native int chmod(Pointer path,int mode);
    
    @NoInline
    @StackAllocation
    public static int chmod(String path,int mode) {
	Pointer str=fivmRuntime.getCStringFullStack(path);
	return chmod(str,mode);
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_STATFS","HAVE_SYS_VFS_H"})
    public static native int statfs(Pointer path,Pointer buf);
    
    @AllocateAsCaller
    @IfPoundDefined({"HAVE_STATFS","HAVE_SYS_VFS_H"})
    public static Pointer statfs(String path) {
	Pointer result=MM.indexableStartOfArray(
	    new byte[CType.sizeof("struct statfs").castToInt()]);
	Pointer str=fivmRuntime.getCStringFullStack(path);
	if (statfs(str,result)==0) {
	    return result;
	} else {
	    return Pointer.zero();
	}
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_RENAME"})
    public static native int rename(Pointer from,Pointer to);
    
    @StackAllocation
    public static int rename(String from,String to) {
	Pointer fromCstr=fivmRuntime.getCStringFullStack(from);
	Pointer toCstr=fivmRuntime.getCStringFullStack(to);
	return rename(fromCstr,toCstr);
    }


    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_IFADDRS_H","HAVE_GETIFADDRS"})
    public static native int getifaddrs(Pointer ifaddrs);

    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_IFADDRS_H","HAVE_FREEIFADDRS"})
    public static native void freeifaddrs(Pointer ifaddrs);

    
    @IfPoundDefined({"HAVE_IFADDRS_H","HAVE_GETIFADDRS"})
    @StackAllocation
    public static Pointer getVMInterfaces() throws SocketException{
	Pointer[] res = new Pointer[1];
	Pointer ifaddrs = MM.indexableStartOfArray(res);
        int ret = getifaddrs(ifaddrs);
	if(ret != 0){
	    throw new SocketException("Errno = "+errno()); 
	}
	Pointer result = ifaddrs.loadPointer();
	return result;
    }

    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_INET_ATON"})
    public static native int inet_aton(Pointer name, Pointer result); 

    
    private static byte[] allocInetAtonResult() {
        return new byte[4];
    }
    
    @StackAllocation
    public static byte[] inet_aton(String address){
	byte[] result = new byte[CType.sizeof("struct in_addr").castToInt()]; //just inaddr
	Pointer resPtr = MM.indexableStartOfArray(result);
	byte[] returnRes = allocInetAtonResult();
	Pointer addr = fivmRuntime.getCStringFullStack(address);
	if (inet_aton(addr, resPtr) == 0)
            return null;
        else {
            returnRes[0] = resPtr.loadByte();
            returnRes[1] = resPtr.add(1).loadByte();
            returnRes[2] = resPtr.add(2).loadByte();
            returnRes[3] = resPtr.add(3).loadByte();
            return returnRes;
        }
    }
				 
    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_NETDB_H","HAVE_GETHOSTBYNAME"})
    public static native Pointer gethostbyname(Pointer name);

    @IfPoundDefined({"HAVE_NETDB_H","HAVE_GETHOSTBYNAME"})
    private static byte[][] gethostbynameImpl(Pointer str) throws UnknownHostException {
        byte[][] result;
        Pointer res = gethostbyname(str);
        if(res == Pointer.zero())
            throw new UnknownHostException();
        int type = CType.getInt(res,"struct hostent","h_addrtype");
        int len = CType.getInt(res,"struct hostent","h_length");
        Pointer iter = CType.getPointer(res,"struct hostent","h_addr_list");
        if(type == AF_INET()){
            type = 4;
        }
        else if(type == AF_INET6()){
            type = 16;
        }
        else throw new fivmError("Got back unknown/unsupported INET type");
        result =  new byte[len][type];
        for(int i = 0; i < len; i++){
            Pointer iterInner = iter.loadPointer();
            for(int j = 0; j < type; j++){
                result[i][j] = (byte)iterInner.add(j).loadByte();
            }
            iter = iter.add(i);
            i++; 
        }
        return result;
    }
    
    @IfPoundDefined({"HAVE_NETDB_H","HAVE_GETHOSTBYNAME"})
    @StackAllocation
    public static byte[][] gethostbyname(String name)
	throws UnknownHostException {
	Pointer str = fivmRuntime.getCStringFullStack(name);
	return gethostbynameImpl(str);
    }

    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_NETDB_H","HAVE_GETHOSTBYADDR"})
    public static native Pointer gethostbyaddr(Pointer addr, 
					       Pointer len,
					       Pointer type); 

    private static void throwUnknownHostException(byte[] addr,int len)
        throws UnknownHostException {
        throw new UnknownHostException(
            "Could not resolve host for IP: "+
            Arrays.toString(Arrays.copyOfRange(addr,0,len)));
    }

    @IfPoundDefined({"HAVE_NETDB_H","HAVE_GETHOSTBYADDR"})
    @StackAllocation
    public static String gethostbyaddr(byte[] addr,
				       int len)
	throws UnknownHostException, fivmError{
        addr=MemUtil.copy(addr);
        
	int type;
	if(len == 4)
	    type = AF_INET();
	else if(len == 16)
	    type = AF_INET6();
	else
	    throw new fivmError("Invalid type passed to gethostbyaddr");
        
	Pointer res = gethostbyaddr(MM.indexableStartOfArray(addr),
				    Pointer.fromInt(len),
				    Pointer.fromInt(type));
	if(res == Pointer.zero())
	    throwUnknownHostException(addr,len);
	return fivmRuntime.fromCStringFull(
			   CType.getPointer(res,"struct hostent","h_name"));
    }

    @Inline
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_GETHOSTNAME"})
    public static native Pointer gethostname(Pointer buf, Pointer len);

    @StackAllocation
    @IfPoundDefined({"HAVE_GETHOSTNAME"})
    public static String getHostName(){
	byte[] buf = new byte[256];
	Pointer res = gethostname(MM.indexableStartOfArray(buf), 
				  Pointer.fromInt(256));
	if (res.signedLessThan(0)){
	    return "localhost";
	}
	else
	    return fivmRuntime.fromCStringFull(MM.indexableStartOfArray(buf));
    }
    
    @RuntimeImport
    @NoSafepoint
    public static native Pointer getenv(Pointer cstr);
    
    @StackAllocation
    public static String getenv(String name) {
	Pointer cstr=fivmRuntime.getCStringFullStack(name);
	Pointer ptr=getenv(cstr);
	if (ptr!=Pointer.zero()) {
	    return fivmRuntime.fromCStringFullInHeap(ptr);
	} else {
	    return null;
	}
    }
    
    public static String getenv(String name,
				String defVal) {
	String result=getenv(name);
	if (result==null) {
	    return defVal;
	} else {
	    return result;
	}
    }
    
    @Import
    @GodGiven
    @NoThrow
    @NoNativeFrame
    @UseObjectsNotHandles
    @IfPoundDefined({"HAVE_GETCWD"})
    public static native Pointer getcwd(Pointer buf,Pointer size);
    
    @StackAllocation
    public static String getcwd() {
	if (CMacro.defined("HAVE_GETCWD")) {
	    int size;
	    if (CMacro.defined("MAXPATHLEN")) {
		size=CVar.getInt("MAXPATHLEN");
	    } else {
		size=1024;
	    }
	    Pointer buf=MM.indexableStartOfArray(new byte[size]);
	    if (getcwd(buf,Pointer.fromInt(size))!=buf) {
		return null;
	    }
	    return fivmRuntime.fromCStringFullInHeap(buf);
	} else {
	    return "/"; // huh?
	}
    }
    
    public static int stdin_fd() {
	return 0;
    }
    
    public static int stdout_fd() {
	return 1;
    }
    
    public static int stderr_fd() {
	return 2;
    }
    
    public static short netEndianFlip(short value) {
	if (fivmOptions.isBigEndian()) {
	    return value;
	} else {
	    return (short)(((((int)value)&0xff00)>>>8) |
			   ((((int)value)&0x00ff)<<8));
	}
    }
    
    public static int netEndianFlip(int value) {
	if (fivmOptions.isBigEndian()) {
	    return value;
	} else {
	    return ((value&0xff000000)>>>24)
		 | ((value&0x00ff0000)>>>8)
		 | ((value&0x0000ff00)<<8)
		 | ((value&0x000000ff)<<24);
	}
    }
}

