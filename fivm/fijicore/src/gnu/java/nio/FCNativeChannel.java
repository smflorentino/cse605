/* FCNativeChannel.java -- Native interface suppling channel operations.
   Copyright (C) 2006 Free Software Foundation, Inc.

This file is part of FijiCore.

FijiCore is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

FijiCore is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with FijiCore; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package gnu.java.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

final class FCNativeChannel {
    private FCNativeChannel() {}
    
    static native int stdin_fd();
    static native int stdout_fd();
    static native int stderr_fd();
  
    static native boolean setBlocking(int fd, boolean blocking)
	throws IOException;
  
    static native int available(int native_fd) throws IOException;

    static native int read(int fd, ByteBuffer dst) throws IOException;
  
    static native int read(int fd) throws IOException;
  
    static native long readScattering(int fd, ByteBuffer[] dsts,
				      int offset, int length)
	throws IOException;
  
    static native void recv (int fd, ByteBuffer dst)
	throws IOException;

    static native InetSocketAddress recvfrom (int fd, ByteBuffer dst)
	throws IOException;

    static native int write(int fd, ByteBuffer src) throws IOException;

    static native long writeGathering(int fd, ByteBuffer[] srcs,
				      int offset, int length)
	throws IOException;
  
    static native int send(int fd, ByteBuffer src)
	throws IOException;
    
    // Send to an IPv4 address.
    static native int send(int fd, ByteBuffer src, byte[] host,int port)
	throws IOException;
    
    // Send to an IPv6 address.
    static native int send6(int fd, ByteBuffer src, byte[] host,int port)
	throws IOException;
  
    static native void write(int fd, int b) throws IOException;
  
    /**
     * Create a new socket, returning the native file descriptor.
     *
     * @param stream Set to true for streaming sockets; false for datagrams.
     * @return The native file descriptor.
     * @throws IOException If creating the socket fails.
     */
    static native int socket(boolean stream) throws IOException;

    static native boolean connect(int fd, byte[] addr, int port, int timeout)
	throws SocketException;
    
    static native boolean connect6(int fd, byte[] addr, int port, int timeout)
	throws SocketException;
  
    static native void disconnect(int fd) throws IOException;
  
    static native InetSocketAddress getsockname(int fd)
	throws IOException;
    
    /*
     * The format here is the peer address, followed by the port number.
     * The returned value is the length of the peer address; thus, there
     * will be LEN + 2 valid bytes put into NAME.
     */
    static native InetSocketAddress getpeername(int fd)
	throws IOException;
  
    static native int accept(int native_fd) throws IOException;

    static native int open(String path, int mode) throws IOException;
    
    static native long position(int fd) throws IOException;
  
    static native void seek(int fd, long pos) throws IOException;

    static native void truncate(int fd, long len) throws IOException;
  
    static native boolean lock(int fd, long pos, long len,
			       boolean shared, boolean wait)
	throws IOException;
  
    static native void unlock(int fd, long pos, long len) throws IOException;
  
    static native long size(int fd) throws IOException;

    static native MappedByteBuffer map(int fd, char mode,
				       long position, int size)
	throws IOException;
  
    static native boolean flush(int fd, boolean metadata) throws IOException;
  
    static native void close(int native_fd) throws IOException;
}

