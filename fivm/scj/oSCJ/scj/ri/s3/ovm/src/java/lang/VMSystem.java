/* VMSystem.java -- helper for java.lang.system
   Copyright (C) 1998, 2002 Free Software Foundation

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

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

package java.lang;

import java.util.Properties;
import java.io.*;
import javax.realtime.MemoryArea;
import org.ovmj.java.NativeConstants;
import org.ovmj.java.io.BufferingPrintStream;

/**
 * This is the Realtime version of VMSystem.
 * VMSystem is a package-private helper class for System that the
 * VM must implement.
 *
 * <h3>To-Do</h3>
 * <p>Implement
 * <ul>
 * <li><tt>setIn</tt></li>
 * <li><tt>setOut</tt></li>
 * <li><tt>setErr</tt></li>
 * </ul>
 *
 * @author David Holmes (OVM)
 */
final class VMSystem {

  /**
   * Copy one array onto another from <code>src[srcStart]</code> ...
   * <code>src[srcStart+len-1]</code> to <code>dest[destStart]</code> ...
   * <code>dest[destStart+len-1]</code>. First, the arguments are validated:
   * neither array may be null, they must be of compatible types, and the
   * start and length must fit within both arrays. Then the copying starts,
   * and proceeds through increasing slots.  If src and dest are the same
   * array, this will appear to copy the data to a temporary location first.
   * An ArrayStoreException in the middle of copying will leave earlier
   * elements copied, but later elements unchanged.
   *
   * @param src the array to copy elements from
   * @param srcStart the starting position in src
   * @param dest the array to copy elements to
   * @param destStart the starting position in dest
   * @param len the number of elements to copy
   * @throws NullPointerException if src or dest is null
   * @throws ArrayStoreException if src or dest is not an array, if they are
   *         not compatible array types, or if an incompatible runtime type
   *         is stored in dest
   * @throws IndexOutOfBoundsException if len is negative, or if the start or
   *         end copy position in either array is out of bounds
   */

    public static void arraycopy(Object src, int src_position,
                                 Object dst, int dst_position,
                                 int length) {
	Class sc = src.getClass();
	Class dc = dst.getClass();

	if (!dc.isArray())
	    throw new ArrayStoreException();

	// We can use block copy operations on primitive arrays
	if ((dc == sc && dc.getComponentType().isPrimitive())
	    // and on type-compatible object arrays in the same MemoryArea
	    || ((dc == sc || dc.isAssignableFrom(sc))
		&& (MemoryArea.getMemoryArea(src)
		    == MemoryArea.getMemoryArea(dst)))) {

	    // a subtype of dc is also an array type
	    if (src_position < 0 || dst_position < 0 || length < 0
		|| src_position + length > LibraryImports.arrayLength(src)
		|| dst_position + length > LibraryImports.arrayLength(dst))
		throw new IndexOutOfBoundsException();

	    if (src == dst)
		LibraryImports.copyOverlapping(src, src_position,
					       dst_position, length);
	    else
		LibraryImports.copyArrayElements(src, src_position,
						 dst, dst_position,
						 length);
	} else {
	    if (!sc.isArray()
		|| sc.getComponentType().isPrimitive()
		|| dc.getComponentType().isPrimitive())
		throw new ArrayStoreException();

            // when src and dest are reference arrays in different memory areas
            // we always get here, so that the necessary store checks are
            // performed.
	    Object[] sa = (Object[]) src;
	    Object[] da = (Object[]) dst;
	    while (length-- > 0)
		da[dst_position++] = sa[src_position++];
	}
    }

    /**
     * Get a hash code computed by the VM for the Object. This hash code will
     * be the same as Object's hashCode() method.  It is usually some
     * convolution of the pointer to the Object internal to the VM.  It
     * follows standard hash code rules, in that it will remain the same for a
     * given Object for the lifetime of that Object.
     *
     * @param o the Object to get the hash code for
     * @return the VM-dependent hash code for this Object
     */
    static int identityHashCode(Object o) {
        return LibraryImports.identityHashCode(o);
    }

    /**
     * Detect big-endian systems.
     *
     * @return true if the system is big-endian.
     */
    static boolean isWordsBigEndian() {
        return NativeConstants.BYTE_ORDER == NativeConstants.BIG_ENDIAN;
    }

    /**
     * Convert a library name to its platform-specific variant.
     *
     * @param libname the library name, as used in <code>loadLibrary</code>
     * @return the platform-specific mangling of the name
     * @XXX Add this method
     static native String mapLibraryName(String libname);
    */
    
    /**
     * Set {@link #in} to a new InputStream.
     *
     * @param in the new InputStream
     * @see #setIn(InputStream)
     */
    static void setIn(InputStream in) {
        LibraryImports.setIn(in);
    }

    /**
     * Set {@link #out} to a new PrintStream.
     *
     * @param out the new PrintStream
     * @see #setOut(PrintStream)
     */
    static void setOut(PrintStream out) {
        LibraryImports.setOut(out);
    }

    /**
     * Set {@link #err} to a new PrintStream.
     *
     * @param err the new PrintStream
     * @see #setErr(PrintStream)
     */
    static void setErr(PrintStream err) {
        LibraryImports.setErr(err);
    }

    /**
     * Get the current time, measured in the number of milliseconds from the
     * beginning of Jan. 1, 1970. This is gathered from the system clock, with
     * any attendant incorrectness (it may be timezone dependent).
     *
     * @return the current time
     * @see java.util.Date
     */
    static long currentTimeMillis() {
        return LibraryImports.getCurrentTime() / (1000*1000);
    }


    static long nanoTime() {
        return LibraryImports.getCurrentTime();
    }

    /**
     * Helper method which creates the standard input stream.
     * VM implementors may choose to construct these streams differently.
     * This method can also return null if the stream is created somewhere 
     * else in the VM startup sequence.
     */
    static InputStream makeStandardInputStream() {
	return new FileInputStream(FileDescriptor.in);
    }

    /** control whether we use the Classpath printstream for std-io or the
        custom OVM buffering print stream. This usual setting is false so that
        we use the custom stream that doesn't leak in scoped memory. The true
        setting is only for experimentation purposes.
    */
    static final boolean CLASSPATH_STDIO = false;

    /**
     * Helper method which creates the standard output stream.
     * VM implementors may choose to construct these streams differently.
     * This method can also return null if the stream is created somewhere 
     * else in the VM startup sequence.
     */
    static PrintStream makeStandardOutputStream() {
        if (CLASSPATH_STDIO)
            return new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out)), true);
        else
            return new BufferingPrintStream(new FileOutputStream(FileDescriptor.out), true);
    }

    /**
     * Helper method which creates the standard error stream.
     * VM implementors may choose to construct these streams differently.
     * This method can also return null if the stream is created somewhere 
     * else in the VM startup sequence.
     */
    static PrintStream makeStandardErrorStream() {
        if (CLASSPATH_STDIO)
            return new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.err)), true);        
        else 
            return new BufferingPrintStream(new FileOutputStream(FileDescriptor.err), true);
    }

    /**
     * Gets the value of an environment variable.
     * Always returning null is a valid (but not very useful) implementation.
     *
     * @param name The name of the environment variable (will not be null).
     * @return The string value of the variable or null when the
     *         environment variable is not defined.
     */
    static String getenv(String name) {
	// FIXME: implement getenv
	return null;
    }
}
