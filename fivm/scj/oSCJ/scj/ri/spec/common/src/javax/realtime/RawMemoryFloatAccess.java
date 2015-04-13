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

import static javax.safetycritical.annotate.Level.LEVEL_0;

import javax.safetycritical.annotate.SCJAllowed;

import edu.purdue.scj.VMSupport;

@SCJAllowed(LEVEL_0)
public class RawMemoryFloatAccess extends RawMemoryAccess
             implements RawRealAccess {

	private static final int LENGTH_OF_FLOAT = 4;
	private static final int LENGTH_OF_DOUBLE = 8;

	
	/**
	 * Create a RawMemoryFloatAccess object corresponding to the specified memory type and
	 * size.  The base address is chosen automatically.
	 *
	 * @param 	type	The attribute(s) of the physical memory needed.  This is either one
	 * 					of the final type objects specified in PhysicalMemoryManager or an
	 * 					array of these objects.
	 *
	 * @param 	size	The number of bytes to be allocated to the immortal memory area.
	 *
	 * @throws 	SecurityException
	 * 			if access is denied to a physical memory type requested
	 *
	 * @throws 	SizeOutOfBoundsException
	 * 			if the memory size specified is invalid
	 *
	 * @throws 	UnsupportedPhysicalMemoryException
	 * 			if the requested memory type is not supported
	 *
	 * @throws 	MemoryTypeConflictException
	 * 			if the memory types specified are not compatible
	 */
	@SCJAllowed(LEVEL_0)
	public RawMemoryFloatAccess(PhysicalMemoryName type, long size) {
		super(type, size);
	}

	/**
	 * Create a RawMemoryFloatAccess object corresponding to the specified memory type and
	 * size begnning at the specified physical address.
	 *
	 * @param 	type	The attribute(s) of the physical memory needed.  This is either one
	 * 					of the final type objects specified in PhysicalMemoryManager or an
	 * 					array of these objects.
	 *
	 * @param 	base	The physical address of the memory region to be mapped as an
	 * 					immortal memory area.
	 *
	 * @param 	size	The number of bytes to be allocated to the immortal memory area.
	 *
	 * @throws 	SecurityException
	 * 			if access is denied to a physical memory type requested or to the range of
	 * 			memory specified
	 *
	 * @throws	OffsetOutOfBoundsError
	 * 			if the base physical address specified is invalid
	 *
	 * @throws 	SizeOutOfBoundsException
	 * 			if the memory size specified is invalid
	 *
	 * @throws 	UnsupportedPhysicalMemoryException
	 * 			if the requested memory type is not supported
	 *
	 * @throws 	MemoryTypeConflictException
	 * 			if the memory types specified are not compatible or if the address range
	 * 			specified does not satisfy the memory types
	 */
	public RawMemoryFloatAccess(PhysicalMemoryName type, long base, long size) {
		super(type,base,size);
	}
	
	/**
	 * Get the double at the given offset.  The load is not guaranteed to be atomic, even if
	 * aligned on a natural 8-byte boundary.
	 *
	 * @param	offset	Offset of the double to read
	 * @return	The double at the given offset
	 *
	 * @throws	OffsetOutOfBoundsException
	 * 			If the offset is negative or falls beyond the size of the mapped area
	 * @throws	SizeOutOfBoundsException
	 * 			If the offset points at an unmapped or otherwise invalid address
	 */
	@SCJAllowed(LEVEL_0)
	public double getDouble(long offset) {
		return VMSupport.getDouble(checkOffset(offset,LENGTH_OF_DOUBLE));
	}

	/**
	 * Get n doubles starting at the given offset in the mapped area, and assigns them
	 * into the double array starting at position low.  There is no guarantee that the load of
	 * the entire array or even a given element will be atomic, even if aligned on an 8-byte
	 * boundary.
	 *
	 * @param	offset	Offset of the doubles to read.
	 * @param	doubles	A destination long array
	 * @param	low		An offset into the destination array to begin storing doubles
	 * @param	n		The number of doubles to read
	 *
	 * @throws	OffsetOutOfBoundsException
	 * 			If the offset is negative or falls beyond the size of the mapped area
	 * @throws	SizeOutOfBoundsException
	 * 			If the offset points at an unmapped or otherwise invalid address
	 * @throws	ArrayIndexOutOfBoundsException
	 * 			If low is negative or low+n is greater than or equal to doubles.length
	 */
	@SCJAllowed(LEVEL_0)
	public void getDoubles(long offset, double[] doubles, int low, int number) {
		checkOffset(offset,number*LENGTH_OF_DOUBLE);
		for (int i=0; i<number; i++, offset += LENGTH_OF_DOUBLE) {
			doubles[low+i] = getDouble(offset);
		}
	}

	/**
	 * Get the float at the given offset.  If the address indicated by the offset falls on a
	 * natural 4-byte boundary, then the value will be retrieved with an atomic load.
	 *
	 * @param	offset	Offset of the float to read
	 * @return	The float at the given offset
	 *
	 * @throws	OffsetOutOfBoundsException
	 * 			If the offset is negative or falls beyond the size of the mapped area
	 * @throws	SizeOutOfBoundsException
	 * 			If the offset points at an unmapped or otherwise invalid address
	 */
	@SCJAllowed(LEVEL_0)
	public float getFloat(long offset) {
		return VMSupport.getFloatAtomic(checkOffset(offset,LENGTH_OF_FLOAT));
	}

	/**
	 * Get n floats starting at the given offset in the mapped area, and assigns them
	 * into the float array starting at position low.  While each of the loads from raw
	 * memory are atomic if naturally aligned, there is no guarantee that the load of
	 * the entire array will be atomic.
	 *
	 * @param	offset	Offset of the floats to read.
	 * @param	floats	A destination float array
	 * @param	low		An offset into the destination array to begin storing floats
	 * @param	n		The number of floats to read
	 *
	 * @throws	OffsetOutOfBoundsException
	 * 			If the offset is negative or falls beyond the size of the mapped area
	 * @throws	SizeOutOfBoundsException
	 * 			If the offset points at an unmapped or otherwise invalid address
	 * @throws	ArrayIndexOutOfBoundsException
	 * 			If low is negative or low+n is greater than or equal to floats.length
	 */
	@SCJAllowed(LEVEL_0)
	public void getFloats(long offset, float[] floats, int low, int number) {
		checkOffset(offset,number*LENGTH_OF_FLOAT);
		for (int i=0; i<number; i++, offset += LENGTH_OF_FLOAT) {
			floats[low+i] = getFloat(offset);
		}
	}

	/**
	 * Set the double at the given offset.  The store is not guaranteed to be atomic, even if
	 * aligned on a natural 8-byte boundary.
	 *
	 * @param	offset	Offset of the double to write
	 * @param	v		Value to write.
	 *
	 * @throws	OffsetOutOfBoundsException
	 * 			If the offset is negative or falls beyond the size of the mapped area
	 * @throws	SizeOutOfBoundsException
	 * 			If the offset points at an unmapped or otherwise invalid address
	 */
	@SCJAllowed(LEVEL_0)
	public void setDouble(long offset, double value) {
		VMSupport.setDouble(checkOffset(offset,LENGTH_OF_DOUBLE), value);
		
	}

	/**
	 * Set n doubles starting at the given offset in this, from the double array
	 * starting at position low.  There is no guarantee that the store of
	 * the entire array or even a given element will be atomic, even if aligned on an 8-byte
	 * boundary.
	 *
	 * @param	offset	Offset of the doubles to read.
	 * @param	doubles	A destination double array
	 * @param	low		An offset into the destination array to begin storing doubles
	 * @param	n		The number of doubles to read
	 *
	 * @throws	OffsetOutOfBoundsException
	 * 			If the offset is negative or falls beyond the size of the mapped area
	 * @throws	SizeOutOfBoundsException
	 * 			If the offset points at an unmapped or otherwise invalid address
	 * @throws	ArrayIndexOutOfBoundsException
	 * 			If low is negative or low+n is greater than or equal to doubles.length
	 */
	@SCJAllowed(LEVEL_0)
	public void setDoubles(long offset, double[] doubles, int low, int number) {
		checkOffset(offset,number*LENGTH_OF_DOUBLE);
		for (int i=0; i<number; i++, offset += LENGTH_OF_DOUBLE) {
			setDouble(offset, doubles[low+i]);
		}
	}

	/**
	 * Set the float at the given offset.  If the address indicated by the offset falls on a
	 * natural 4-byte boundary, then the value will be written with an atomic store.
	 *
	 * @param	offset	Offset of the float to write
	 * @param	v		Value to write.
	 *
	 * @throws	OffsetOutOfBoundsException
	 * 			If the offset is negative or falls beyond the size of the mapped area
	 * @throws	SizeOutOfBoundsException
	 * 			If the offset points at an unmapped or otherwise invalid address
	 */
	@SCJAllowed(LEVEL_0)
	public void setFloat(long offset, float value) {
		VMSupport.setFloatAtomic(checkOffset(offset,LENGTH_OF_FLOAT), value);
	}

	/**
	 * Set n floats starting at the given offset in this, from the float array
	 * starting at position low.  While each of the stores to raw
	 * memory are atomic if naturally aligned, there is no guarantee that the store of
	 * the entire array will be atomic.
	 *
	 * @param	offset	Offset of the floats to read.
	 * @param	floats	A destination float array
	 * @param	low		An offset into the destination array to begin storing floats
	 * @param	n		The number of floats to read
	 *
	 * @throws	OffsetOutOfBoundsException
	 * 			If the offset is negative or falls beyond the size of the mapped area
	 * @throws	SizeOutOfBoundsException
	 * 			If the offset points at an unmapped or otherwise invalid address
	 * @throws	ArrayIndexOutOfBoundsException
	 * 			If low is negative or low+n is greater than or equal to floats.length
	 */
	@SCJAllowed(LEVEL_0)
	public void setFloats(long offset, float[] floats, int low, int number) {
		checkOffset(offset,number*LENGTH_OF_FLOAT);
		for (int i=0; i<number; i++, offset += LENGTH_OF_FLOAT) {
			setFloat(offset, floats[low+i]);
		}
	}

//  public RawMemoryFloatAccess(PhysicalMemoryName type, long size)
///*         throws java.lang.SecurityException,
//                javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException,
//                javax.realtime.UnsupportedPhysicalMemoryException*/
//  {};
//
//
//  public RawMemoryFloatAccess(PhysicalMemoryName type, long base, long size)
///*         throws java.lang.SecurityException,
//                javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException,
//                javax.realtime.UnsupportedPhysicalMemoryException,
//                javax.realtime.MemoryTypeConflictException*/
//                {};
//
//  public static RawScalarAccess createFpAccessInstance(RawMemoryName type,
//                long base, long size)
// /*        throws java.lang.InstantiationException,
//                java.lang.IllegalAccessException,
//                java.lang.reflect.InvocationTargetException */
//                {};
//                
//  public double getDouble(long offset)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {return 0.0; };
//  public void getDoubles(long offset, double[] doubles, int low, int number)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//                
//  public float getFloat(long offset)
// /*        throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {return 0.0; };
//  public void getFloats(long offset, float[] floats, int low, int number)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//                
//  public void setDouble(long offset, double value)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//  public void setDoubles(long offset, double[] doubles, int low, int number)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//                
//  public void setFloat(long offset, float value)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//  public void setFloats(long offset, float[] floats, int low, int number)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//
}
