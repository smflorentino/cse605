/* VMPlainSocketImpl.java -- VM interface for default socket implementation
   Copyright (C) 2009 Fiji Systems LLC.

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

package gnu.java.net;

import java.io.IOException;
import java.net.SocketException;

final class FCNativePlainSocketImpl {
    private FCNativePlainSocketImpl() {}
    
    static native void setOption(int fd, int id, int value)
	throws SocketException;
    
    static native int getOption(int fd, int id) throws SocketException;
    
    /**
     * Native bind function for IPv4 addresses. The addr array must be
     * exactly four bytes long.
     * 
     * VMs without native support need not implement this.
     *
     * @param fd The native file descriptor integer.
     * @param addr The IPv4 address, in network byte order.
     * @param port The port to bind to.
     * @throws IOException
     */
    static native void bind(int fd, byte[] addr, int port)
	throws IOException;
  
    /**
     * Native bind function for IPv6 addresses. The addr array must be
     * exactly sixteen bytes long.
     * 
     * VMs without native support need not implement this.
     *
     * @param fd The native file descriptor integer.
     * @param addr The IPv6 address, in network byte order.
     * @param port The port to bind to.
     * @throws IOException
     */
    static native void bind6(int fd, byte[] addr, int port)
	throws IOException;

    /**
     * Native listen function. VMs without native support need not implement
     * this.
     *
     * @param fd The file descriptor integer.
     * @param backlog The listen backlog size.
     * @throws IOException
     */
    static native void listen(int fd, int backlog) throws IOException;

    static native void shutdownInput(int native_fd) throws IOException;
  
    static native void shutdownOutput(int native_fd) throws IOException;
  
    static native void sendUrgentData(int natfive_fd, int data) throws IOException;
}

