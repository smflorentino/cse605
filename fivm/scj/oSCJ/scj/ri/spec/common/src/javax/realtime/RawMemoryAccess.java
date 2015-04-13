/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package javax.realtime;

import javax.safetycritical.annotate.SCJAllowed;

import edu.purdue.scj.VMSupport;
import static javax.safetycritical.annotate.Level.LEVEL_0;

/**
 * An instance of RawMemoryAccess models a "raw storage" area as a fixed-size
 * sequence of bytes. One constructor takes parameters that specify the area's
 * memory type and size. Another constructor takes an additional base address
 * parameter specifying a specific physical address. A full complement of getter
 * and setter methods allow the contents of the physical memory area to be
 * accessed through offsets from the base, interpreted as byte, short, int, or
 * long data values, and copied to/from arrays of byte, short, int, or long.
 * 
 * By treating a value as itself an offset, a program can use a physical memory
 * area to contain "references" to other data values in the same area. A
 * subregion of a raw memory area can itself be defined as a raw memory area.
 * The base address and size, and any offset into a physical memory area, are
 * long (64-bit) values. Whether offset addresses the high or low order byte is
 * based on the value of the BYTE_ORDER static boolean variable in class
 * RealtimeSystem. The RawMemoryAccess class allows a real-time program to
 * implement device drivers, memory-mapped I/O, "flash" memory, battery-backed
 * RAM, and similar low-level software.
 * 
 * 
 * Safety: ------ A physical memory area cannot contain references to Java
 * objects. Such a capability would be unsafe (since it could be used to defeat
 * Java's type checking) and error-prone (since it is sensitive the the specific
 * representational choices made by the Java compiler).
 * 
 * 
 * TODO: a huge one: add throwing of the exceptions!
 * 
 * 
 */
@SCJAllowed(LEVEL_0)
public class RawMemoryAccess implements RawIntegralAccess {

    private static final int LENGTH_OF_BYTE = 1;
    private static final int LENGTH_OF_SHORT = 2;
    private static final int LENGTH_OF_INT = 4;
    private static final int LENGTH_OF_LONG = 8;
    private static final long NOT_MAPPED = -1;

    private long physicalBase;
    private final long physicalSize;
    private long mappedBase = NOT_MAPPED;
    private long mappedSize;
    private final Object type;

    
    public static final int O_RDWR = 2;
    
    
    
    @SCJAllowed(LEVEL_0)
    public static final RawMemoryName IO_ACCESS = null;

    @SCJAllowed(LEVEL_0)
    public static final RawMemoryName MEM_ACCESS = null;

    
    
    
    /**
     * Testing Raw Memory access.
     */
    
    public static int open(String file, int flags, int mode) {
        return VMSupport.open(file, flags, mode);
    }
    
    public static void close(int fd) {
        VMSupport.close(fd);
    }

    public static byte readByte(int fd) {
        return (byte) VMSupport.readByte(fd);
    }

    public static int writeByte(int fd, byte b) {
        return VMSupport.writeByte(fd, b);
    }
    
    public static void mmap(int address, int len, int prot, int flags, int fd, int off) {
        VMSupport.mmap(address,len,prot,flags,fd,off);
    }

    /**
     * Create a RawMemoryAccess object corresponding to the specified memory
     * type and size.
     * 
     * TODO: how the base address should be chosen? In J9 it is chosen
     * automatically.
     * 
     * @param type
     *            The attribute(s) of the physical memory needed. This is either
     *            one of the final type objects specified in
     *            PhysicalMemoryManager or an array of these objects.
     * 
     * @param size
     *            The number of bytes to be allocated to the immortal memory
     *            area.
     * 
     * @throws SecurityException
     *             if access is denied to a physical memory type requested
     * 
     * @throws SizeOutOfBoundsException
     *             if the memory size specified is invalid
     * 
     * @throws UnsupportedPhysicalMemoryException
     *             if the requested memory type is not supported
     * 
     * @throws MemoryTypeConflictException
     *             if the memory types specified are not compatible
     * 
     * @sthrows java.lang.SecurityException,
     *          javax.realtime.OffsetOutOfBoundsException,
     *          javax.realtime.SizeOutOfBoundsException,
     *          javax.realtime.UnsupportedPhysicalMemoryException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public RawMemoryAccess(PhysicalMemoryName type, long size) {
        // if (size < 0) throw new SizeOutOfBoundsException("Negative size");

        this.type = type;
        this.physicalSize = size;
        this.physicalBase = -1;

        map();

        // TODO: get physicalBase

    }

    /**
     * Create a RawMemoryAccess object corresponding to the specified memory
     * type and size beginning at the specified physical address.
     * 
     * @param type
     *            The attribute(s) of the physical memory needed. This is either
     *            one of the final type objects specified in
     *            PhysicalMemoryManager or an array of these objects.
     * 
     * @param base
     *            The physical address of the memory region to be mapped as a
     *            raw memory access area.
     * 
     * @param size
     *            The number of bytes to be allocated to the raw memory access
     *            area.
     * 
     * @throws SecurityException
     *             if access is denied to a physical memory type requested or to
     *             the range of memory specified
     * 
     * @throws OffsetOutOfBoundsError
     *             if the base physical address specified is invalid
     * 
     * @throws SizeOutOfBoundsException
     *             if the memory size specified is invalid
     * 
     * @throws UnsupportedPhysicalMemoryException
     *             if the requested memory type is not supported
     * 
     * @throws MemoryTypeConflictException
     *             if the memory types specified are not compatible or if the
     *             address range specified does not satisfy the memory types
     *             throws java.lang.SecurityException,
     *             javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException,
     *             javax.realtime.UnsupportedPhysicalMemoryException
     * 
     * 
     */
    @SCJAllowed(LEVEL_0)
    public RawMemoryAccess(PhysicalMemoryName type, long base, long size) {
        // if (size < 0) throw new SizeOutOfBoundsException("Negative size");

        this.physicalSize = size;
        this.type = type;

        // if (base < 0) throw new OffsetOutOfBoundsException();
        this.physicalBase = base;

        map();

    }

    /**
     * 
     * TODO: create RMA Instance...
     * 
     * 
     * @param type
     * @param base
     * @param size
     * @return
     * 
     * @throws java.lang.InstantiationException
     *             , java.lang.IllegalAccessException,
     *             java.lang.reflect.InvocationTargetException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public static RawIntegralAccess createRmaInstance(RawMemoryName type,
            long base, long size) {
        return null;
    }

    /**
     * Map the physical address range into virtual memory. Has no effect if the
     * system doesn't support virtual memory
     * 
     * @return Virtual address mapped to for reference puposes
     * 
     * @throws OutOfMemoryError
     *             If there is insufficient virtual memory to map the physical
     *             memory
     * 
     * 
     * 
     */
    public long map() {
        if (mappedBase != NOT_MAPPED)
            return mappedBase;

        // TODO: check if this will pass through the checker
        // we are Level 0 and PhysicalMemoryManager is Level 1.
        mappedBase = PhysicalMemoryManager
                .map(type, physicalBase, physicalSize);
        mappedSize = physicalSize;

        return mappedBase;
    }

    /**
     * Set the byte at the given offset with an atomic store.
     * 
     * @param offset
     *            Offset of the byte to write
     * @param v
     *            Value to write.
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * 
     *             throws javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public byte getByte(long offset) {
        return VMSupport.getByteAtomic(checkOffset(offset, LENGTH_OF_BYTE));
    }

    /**
     * Get n bytes starting at the given offset in the mapped area, and assigns
     * them into the byte array starting at position low. While each of the
     * loads from raw memory are atomic, there is no guarantee that the load of
     * the entire array will be atomic.
     * 
     * @param offset
     *            Offset of the bytes to read.
     * @param bytes
     *            A destination byte array
     * @param low
     *            An offset into the destination array to begin storing bytes
     * @param number
     *            The number of bytes to read
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws ArrayIndexOutOfBoundsException
     *             If low is negative or low+n is greater than or equal to
     *             bytes.length throws
     *             javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     */
    @SCJAllowed(LEVEL_0)
    public void getBytes(long offset, byte[] bytes, int low, int number) {
        checkOffset(offset, number * LENGTH_OF_BYTE);
        for (int i = 0; i < number; i++, offset += LENGTH_OF_BYTE) {
            bytes[low + i] = getByte(offset);
        }
    }

    /**
     * Get the int at the given offset. If the address indicated by the offset
     * falls on a natural 4-byte boundary, then the value will be retrieved with
     * an atomic load.
     * 
     * @param offset
     *            Offset of the int to read
     * @return The int at the given offset
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * 
     *             /* throws javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public int getInt(long offset) {
        return VMSupport.getIntAtomic(checkOffset(offset, LENGTH_OF_INT));
    }

    /**
     * Get n ints starting at the given offset in the mapped area, and assigns
     * them into the int array starting at position low. While each of the loads
     * from raw memory are atomic if naturally aligned, there is no guarantee
     * that the load of the entire array will be atomic.
     * 
     * @param offset
     *            Offset of the ints to read.
     * @param ints
     *            A destination int array
     * @param low
     *            An offset into the destination array to begin storing ints
     * @param n
     *            The number of ints to read
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws ArrayIndexOutOfBoundsException
     *             If low is negative or low+n is greater than or equal to
     *             ints.length
     * 
     *             /* throws javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     * 
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void getInts(long offset, int[] ints, int low, int number) {
        checkOffset(offset, number * LENGTH_OF_INT);
        for (int i = 0; i < number; i++, offset += LENGTH_OF_INT) {
            ints[low + i] = getInt(offset);
        }
    };

    /**
     * Get the long at the given offset. The load is not guaranteed to be
     * atomic, even if aligned on a natural 8-byte boundary.
     * 
     * @param offset
     *            Offset of the long to read
     * @return The long at the given offset
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address /* throws javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public long getLong(long offset) {
        return VMSupport.getLong(checkOffset(offset, LENGTH_OF_LONG));
    }

    /**
     * Get n longs starting at the given offset in the mapped area, and assigns
     * them into the long array starting at position low. There is no guarantee
     * that the load of the entire array or even a given element will be atomic,
     * even if aligned on an 8-byte boundary.
     * 
     * @param offset
     *            Offset of the longs to read.
     * @param longs
     *            A destination long array
     * @param low
     *            An offset into the destination array to begin storing longs
     * @param n
     *            The number of longs to read
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws ArrayIndexOutOfBoundsException
     *             If low is negative or low+n is greater than or equal to
     *             longs.length /* throws
     *             javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void getLongs(long offset, long[] longs, int low, int number) {
        checkOffset(offset, number * LENGTH_OF_LONG);
        for (int i = 0; i < number; i++, offset += LENGTH_OF_LONG) {
            longs[low + i] = getLong(offset);
        }
    };

    /**
     * Get the short at the given offset. If the address indicated by the offset
     * falls on a natural 2-byte boundary, then the value will be retrieved with
     * an atomic load.
     * 
     * @param offset
     *            Offset of the short to read
     * @return The short at the given offset
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address /* throws javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public short getShort(long offset) {
        return VMSupport.getShortAtomic(checkOffset(offset, LENGTH_OF_SHORT));
    }

    /**
     * Get n shorts starting at the given offset in the mapped area, and assigns
     * them into the short array starting at position low. While each of the
     * loads from raw memory are atomic if naturally aligned, there is no
     * guarantee that the load of the entire array will be atomic.
     * 
     * @param offset
     *            Offset of the shorts to read.
     * @param shorts
     *            A destination short array
     * @param low
     *            An offset into the destination array to begin storing shorts
     * @param n
     *            The number of shorts to read
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws ArrayIndexOutOfBoundsException
     *             If low is negative or low+n is greater than or equal to
     *             shorts.length
     * @throws javax.realtime.OffsetOutOfBoundsException
     *             , javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void getShorts(long offset, short[] shorts, int low, int number) {
        checkOffset(offset, number * LENGTH_OF_SHORT);
        for (int i = 0; i < number; i++, offset += LENGTH_OF_SHORT) {
            shorts[low + i] = getShort(offset);
        }

    }

    /**
     * Set the byte at the given offset with an atomic store.
     * 
     * @param offset
     *            Offset of the byte to write
     * @param value
     *            Value to write.
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws javax.realtime.OffsetOutOfBoundsException
     *             , javax.realtime.SizeOutOfBoundsException
     */
    @SCJAllowed(LEVEL_0)
    public void setByte(long offset, byte value) {
        VMSupport.setByteAtomic(checkOffset(offset, LENGTH_OF_BYTE), value);
    };

    /**
     * Set n bytes starting at the given offset in this, from the byte array
     * starting at position low. While each of the stores to raw memory are
     * atomic, there is no guarantee that the store of the entire array will be
     * atomic.
     * 
     * @param offset
     *            Offset of the bytes to write.
     * @param bytes
     *            A source byte array
     * @param low
     *            An offset into the source array to begin reading bytes
     * @param n
     *            The number of bytes to write
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws ArrayIndexOutOfBoundsException
     *             If low is negative or low+n is greater than or equal to
     *             bytes.length
     * @throws javax.realtime.OffsetOutOfBoundsException
     *             , javax.realtime.SizeOutOfBoundsExceptions
     * 
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void setBytes(long offset, byte[] bytes, int low, int number) {
        checkOffset(offset, number * LENGTH_OF_BYTE);
        for (int i = 0; i < number; i++, offset += LENGTH_OF_BYTE) {
            setByte(offset, bytes[low + i]);
        }
    };

    /**
     * Set the int at the given offset. If the address indicated by the offset
     * falls on a natural 4-byte boundary, then the value will be written with
     * an atomic store.
     * 
     * @param offset
     *            Offset of the int to write
     * @param v
     *            Value to write.
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws javax.realtime.OffsetOutOfBoundsException
     *             , javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void setInt(long offset, int value) {
        VMSupport.setIntAtomic(checkOffset(offset, LENGTH_OF_INT), value);
    };

    /**
     * Set n ints starting at the given offset in this, from the int array
     * starting at position low. While each of the stores to raw memory are
     * atomic if naturally aligned, there is no guarantee that the store of the
     * entire array will be atomic.
     * 
     * @param offset
     *            Offset of the ints to read.
     * @param ints
     *            A destination int array
     * @param low
     *            An offset into the destination array to begin storing ints
     * @param n
     *            The number of ints to read
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws ArrayIndexOutOfBoundsException
     *             If low is negative or low+n is greater than or equal to
     *             ints.length
     * 
     * @throws javax.realtime.OffsetOutOfBoundsException
     *             , javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void setInts(long offset, int[] ints, int low, int number) {
        checkOffset(offset, number * LENGTH_OF_INT);
        for (int i = 0; i < number; i++, offset += LENGTH_OF_INT) {
            setInt(offset, ints[low + i]);
        }
    };

    /**
     * Set the byte at the given offset with an atomic store.
     * 
     * @param offset
     *            Offset of the byte to write
     * @param v
     *            Value to write.
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * 
     *             TODO: why value is long?? Should not it be "byte"?
     * 
     * @throws throws javax.realtime.OffsetOutOfBoundsException,
     *         javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void setByte(long offset, long value) {
        VMSupport.setByteAtomic(checkOffset(offset, LENGTH_OF_BYTE), value);
    }

    /**
     * Set the long at the given offset. The store is not guaranteed to be
     * atomic, even if aligned on a natural 8-byte boundary.
     * 
     * @param offset
     *            Offset of the long to write
     * @param v
     *            Value to write.
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     */
    @SCJAllowed(LEVEL_0)
    public void setLong(long offset, long v) /*
                                              * throws
                                              * OffsetOutOfBoundsException
                                              */{
        VMSupport.setLong(checkOffset(offset, LENGTH_OF_LONG), v);
    }

    /**
     * Set n longs starting at the given offset in this, from the long array
     * starting at position low. There is no guarantee that the store of the
     * entire array or even a given element will be atomic, even if aligned on
     * an 8-byte boundary.
     * 
     * @param offset
     *            Offset of the longs to read.
     * @param longs
     *            A destination long array
     * @param low
     *            An offset into the destination array to begin storing longs
     * @param n
     *            The number of longs to read
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws ArrayIndexOutOfBoundsException
     *             If low is negative or low+n is greater than or equal to
     *             longs.length throws
     *             javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void setLongs(long offset, long[] longs, int low, int number) {
        checkOffset(offset, number * LENGTH_OF_LONG);
        for (int i = 0; i < number; i++, offset += LENGTH_OF_LONG) {
            setLong(offset, longs[low + i]);
        }
    };

    /**
     * Set the short at the given offset. If the address indicated by the offset
     * falls on a natural 2-byte boundary, then the value will be written with
     * an atomic store.
     * 
     * @param offset
     *            Offset of the short to write
     * @param v
     *            Value to write.
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address /* throws javax.realtime.OffsetOutOfBoundsException,
     *             javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void setShort(long offset, short value) {
        VMSupport.setShortAtomic(checkOffset(offset, LENGTH_OF_SHORT), value);
    };

    /**
     * Set n shorts starting at the given offset in this, from the short array
     * starting at position low. While each of the stores to raw memory are
     * atomic if naturally aligned, there is no guarantee that the store of the
     * entire array will be atomic.
     * 
     * @param offset
     *            Offset of the shorts to read.
     * @param shorts
     *            A destination short array
     * @param low
     *            An offset into the destination array to begin storing shorts
     * @param n
     *            The number of shorts to read
     * 
     * @throws OffsetOutOfBoundsException
     *             If the offset is negative or falls beyond the size of the
     *             mapped area
     * @throws SizeOutOfBoundsException
     *             If the offset points at an unmapped or otherwise invalid
     *             address
     * @throws ArrayIndexOutOfBoundsException
     *             If low is negative or low+n is greater than or equal to
     *             shorts.length
     * @throws javax.realtime.OffsetOutOfBoundsException
     *             , javax.realtime.SizeOutOfBoundsException
     * 
     */
    @SCJAllowed(LEVEL_0)
    public void setShorts(long offset, short[] shorts, int low, int number) {
        checkOffset(offset, number * LENGTH_OF_SHORT);
        for (int i = 0; i < number; i++, offset += LENGTH_OF_SHORT) {
            setShort(offset, shorts[low + i]);
        }
    };

    /**
     * Helper method - must not be protected as not user accessible but package
     * private so RawMemoryFloatAccess can use it
     * 
     * @param offset
     *            into RawMemoryAccess area
     * @param length
     *            of object to be accessed
     * @return virtual address to use
     */
    long checkOffset(long offset, long length) {
        long base = mappedBase;
        /*
         * if (base == NOT_MAPPED) throw new SizeOutOfBoundsException();
         * 
         * if (offset < 0 || offset >= physicalSize) throw new
         * OffsetOutOfBoundsException(offset+":"+physicalSize);
         * 
         * if (offset + length > mappedSize) throw new
         * SizeOutOfBoundsException(offset+"+"+length+":"+mappedSize);
         */

        return base + offset;
    }

    public void close() {
        // TODO Auto-generated method stub
    }
}
