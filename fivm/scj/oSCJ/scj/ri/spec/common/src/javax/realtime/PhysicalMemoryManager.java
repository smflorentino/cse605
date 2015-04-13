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
import static javax.safetycritical.annotate.Level.LEVEL_1;

/**
 * Class used to manage physical memory. 
 * 		--> This is a simple "virtual memory" implementation for the physical raw memory
 * 
 * Basically it will implement the mapping method that reserves a chunk of memory
 *  starting at "physicalBase" and having "physicalSize" size.
 * 
 * TODO: is this really at LEVEL 1?
 * 
 * 
 * TODO: implement
 * 
 * 
 * @author plsek
 *
 */
@SCJAllowed(LEVEL_1)
public final class PhysicalMemoryManager {
	/*
	 *
	 */

	public static final PhysicalMemoryName ALIGNED = null;

	/*
	 *
	 */  

	public static final PhysicalMemoryName BYTESWAP = null;

	/*
	 *
	 */
	@SCJAllowed(LEVEL_1)
	public static final PhysicalMemoryName DEVICE = null;

	/*
	 *
	 */
	@SCJAllowed(LEVEL_1)
	public static final PhysicalMemoryName DMA = null;

	/*
	 *
	 */
	@SCJAllowed(LEVEL_1)
	public static final PhysicalMemoryName IO_PAGE = null;

	/*
	 *
	 */
	@SCJAllowed(LEVEL_1)
	public static final PhysicalMemoryName SHARED = null;

	
	
	private static final int PMEM_IN_USE = -1;
	private static final int VMEM_IN_USE = -2;
	private static final int BASE_NOT_OKAY = -3;
	private static final int VBASE_NOT_OKAY = -4;
	private static final int PMEM_NOT_FOUND = -5;
	private static final int VMEM_NOT_FOUND = -6;
	private static final int MEM_NOT_PRESENT = -7;


	/**
	 * TODO : No public default constructor ??? How is it defined in SCJ??
	 */
	PhysicalMemoryManager(){}
	
	/**
	 * Map the physical address range into virtual memory.  Has no effect if the
	 * system doesn't support virtual memory
	 *
	 * @return  Virtual address mapped to for reference puposes
	 *
	 * @throws 	OutOfMemoryError
	 * 			If there is insufficient virtual memory to map the physical memory
	 */
	public static long map(Object type, long base, long size) {
		long virt = PMEM_IN_USE;
		
		// Check the requested address has not been used
		if (base >= 0) {
			//int i = findOverlap(plist, base, size);
			//if (i >= 0) throw new MemoryInUseException("request "+rangeToString(base,size)+" overlaps "+rangeToString(plist[i],plist[i+1])+" from "+pmflist[i]);
		}
		
		// TODO : implement the PhysicalMemoryManager.map()
		
		// TODO: some book-keeping of requested/mapped memory areas is needed!
		
		
		//if (virt == PMEM_IN_USE) throw new MemoryInUseException("in physical memory: request "/*+rangeToString(base,size)+" "+filter*/);
		//if (virt == VMEM_IN_USE) throw new MemoryInUseException("in virtual memory: request "/*+rangeToString(base,size)+" "+filter*/);
		//if (virt == PMEM_NOT_FOUND) throw new OutOfMemoryError("in physical memory: request "/*+rangeToString(base,size)+" "+filter*/);
		//if (virt == VMEM_NOT_FOUND) throw new OutOfMemoryError("in virtual memory: request "/*+rangeToString(base,size)+" "+filter*/);
		//if (virt == BASE_NOT_OKAY) throw new MemoryTypeConflictException("physical base "/*+rangeToString(base,size)+" "+filter*/);
		//if (virt == VBASE_NOT_OKAY) throw new MemoryTypeConflictException("virtual base "/*+rangeToString(base,size)+" "+filter*/);
		//if (virt == MEM_NOT_PRESENT) throw new MemoryTypeConflictException("memory not present "/*+rangeToString(base,size)+" not present in "+filter*/);
		
		
		return virt;
	}
}
