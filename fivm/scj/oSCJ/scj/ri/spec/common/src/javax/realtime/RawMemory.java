package javax.realtime;

import static javax.safetycritical.annotate.Level.LEVEL_0;

import java.lang.reflect.InvocationTargetException;

import javax.safetycritical.annotate.SCJAllowed;


/**
 * This class is the hub of a system that constructs special-purpose objects
 *  that ac- cess particular types and ranges of raw memory. 
 *  This facility is supported by the registerAccessFactory and 
 *  createRawIntegralAcessInstance methods. In SCJ, four raw-integral-access 
 *  factories are supported: two for accessing the DEVICE memory 
 *  (called IO PORT MAPPED and IO MEMORY MAPPED), one for accessing memory that 
 *  can be used for DMA (called DMA ACCESS) and the other for accesses to the memory
 *   (called MEM ACCESS). These can be accessed via static methods in the 
 *   RawMemoryAccess class.
 * 
 * 
 * @author plsek
 *
 *	TODO: init the static variables!
 *
 *
 */
@SCJAllowed(LEVEL_0)
public class RawMemory {

	/**
	 * This raw memory name is used to call for access memory using DMA.
	 */
	/*@SCJAllowed(LEVEL_0)*/
	public static final RawMemoryName DMA_ACCESS = null;
	
	/**
	 * This raw memory name is used to call for access memory.
	 */
	/*@SCJAllowed(LEVEL_0)*/ 
	public static final RawMemoryName MEM_ACCESS = null;
	
	
	/**
	 * This raw memory name is used to call for access to all I/O devices that are accessed by special instructions.
	 */
	/*@SCJAllowed(LEVEL_0)*/ 
	public static final RawMemoryName IO_PORT_MAPPED = null;
	
	/**
	 * This raw memory name is used to call for access to devices that are memory mapped.
	 */
	/*@SCJAllowed(LEVEL_0)*/
	public static final RawMemoryName IO_MEM_MAPPED = null;
	

	/**
	 * 
	 * Create (or find) an immortal instance of a class that implements 
	 * RawIntegralAccess and accesses (only) memory of type type in the address 
	 * range described by base and size.
	 * 
	 * @param type
	 * @param base
	 * @param size
	 * @return an object that implements RawIntegralAccess and supports access to the 
	 *  	 	specified range of physical memory.
	 */
	@SCJAllowed
	public static RawIntegralAccess createRawIntegralInstance( RawMemoryName type, 
			long base, long size) throws IllegalArgumentException, 
			SecurityException, /*OffsetOutOfBoundsException, SizeOutOfBoundsException, 
			MemoryTypeConflictException,*/ OutOfMemoryError, InstantiationException, 
			InvocationTargetException {
		
		//TODO:
		return null;
	}
	
	@SCJAllowed
    public static RawIntegralAccess createRawIntegralInstance(String file, RawMemoryName type, 
            long address, long base, long size) throws IllegalArgumentException, 
            SecurityException, /*OffsetOutOfBoundsException, SizeOutOfBoundsException, 
            MemoryTypeConflictException,*/ OutOfMemoryError, InstantiationException, 
            InvocationTargetException {
        
        //TODO:
        return null;
    }
    
	
	public static void registerAccessFactory(RawIntegralAccessFactory factory) {}
	
}
