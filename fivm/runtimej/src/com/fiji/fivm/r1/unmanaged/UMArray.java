package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.AllowUnsafe;
import com.fiji.fivm.r1.CType;
import com.fiji.fivm.r1.Inline;
import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.NoInline;
import com.fiji.fivm.r1.NoPollcheck;
import com.fiji.fivm.r1.NoReturn;
import com.fiji.fivm.r1.NoSafepoint;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.Reflect;
import com.fiji.fivm.r1.fivmRuntime;

import static com.fiji.fivm.r1.fivmRuntime.throwArrayBoundsRTE;
import static com.fiji.fivm.r1.fivmRuntime.throwNegativeSizeRTE;
import static com.fiji.fivm.r1.unmanaged.UMUtils.fivmr_MemoryArea_freeArray;
import static com.fiji.fivm.r1.unmanaged.UMUtils.fivmr_MemoryArea_loadArrayInt;
import static com.fiji.fivm.r1.unmanaged.UMUtils.fivmr_MemoryArea_storeArrayInt;

/**
 * Created by scottflo on 4/21/15.
 */
public class UMArray
{
	public static Pointer allocate(UMArrayType type, int size)
	{
		//We can't be in the Heap OR Immortal Memory
		final Pointer curArea = MemoryAreas.getCurrentArea();
		if(curArea == MemoryAreas.getHeapArea() || curArea == MemoryAreas.getImmortalArea())
		{
			throw new UnsupportedOperationException("Allocation cannot occur in Heap or Immortal Memory");
		}
		//Size must be a positive number
		if (Magic.semanticallyUnlikely(size<=0)) {
			throwNASE();
		}
		//Attempt to allocate array
		return UMUtils.fivmr_MemoryArea_allocateArray(Magic.curThreadState(), type.getVal(), size);
	}

	public static void free(Pointer array)
	{
		//We can't be in the Heap OR Immortal Memory
		final Pointer curArea = MemoryAreas.getCurrentArea();
		if(curArea == MemoryAreas.getHeapArea() || curArea == MemoryAreas.getImmortalArea())
		{
			throw new UnsupportedOperationException("Allocation cannot occur in Heap or Immortal Memory");
		}
		fivmr_MemoryArea_freeArray(MemoryAreas.getCurrentArea(), array);
	}

	@Inline
	@NoSafepoint
	public static int getInt(Pointer array, int index)
	{
		nullCheckAndArrayBoundsCheck(array, index);
		return fivmr_MemoryArea_loadArrayInt(array, index);
	}

	@Inline
	@NoSafepoint
	public static void setInt(Pointer array, int index, int val)
	{
		nullCheckAndArrayBoundsCheck(array, index);
		fivmr_MemoryArea_storeArrayInt(array, index, val);
	}

	@Inline
	@NoPollcheck
	@AllowUnsafe
	public static int length(Pointer array)
	{
		return CType.getInt(array,"fivmr_um_array_header","size");
	}

	public static enum UMArrayType {
		INT(0),
		BOOLEAN(1),
		DOUBLE(2),
		BYTE(3),
		SHORT(4),
		LONG(5),
		CHAR(6),
		FLOAT(7);

		final int intVal;
		UMArrayType(int val) {
			this.intVal = val;
		}

		public int getVal()
		{
			return intVal;
		}
	}

	@Inline
	@NoPollcheck
	public static void nullCheckAndArrayBoundsCheck(Pointer array,int index) {
		nullCheck(array);
		if (!Magic.uLessThan(index, length(array))) {
			throwArrayBoundsRTE();
		}
	}



	/**
	 * Taken from MM.java, where it was private.
	 */
	@NoInline
	@NoReturn
	@Reflect
	static void throwNASE() {
		throwNegativeSizeRTE();
	}

	/**
	 * Taken from MM.java, where it was public but checked for Objects, not Pointers
	 */
	@Inline
	@NoPollcheck
	static void nullCheck(Pointer p)
	{
		if(p == Pointer.zero())
		{
			fivmRuntime.throwNullPointerRTE();
		}
	}

	//TODO get this from C
	private static final int BLOCKSIZE = 64;
	//TODO get this from C
	private static final int POINTER_SIZE = 4;
	//TODO get this from C
	private static final int HEADER_SIZE = BLOCKSIZE;

	/**
	 * @return the amount of scoped memory needed to support array(s) of the specified size, and amount;
	 * with the specified amount being the number of arrays allocated at any one time quantum
	 */
	public static int calculateScopedMemorySize(int elemSize, int elemCount, int arrayCount, int activeArrays)
	{
		assert elemSize == 8; //Right now all array elements are 8 bytes
		return calculateManagedMemorySize(elemSize,elemCount,activeArrays) + calcualteScopedMemoryOverhead(elemSize,elemCount,arrayCount);

	}

	/**
	 * @return the amount of managed memory needed to support array(s) of the specified size, and amount,
	 * with the specified amount being the number of arrays allocated at any one time quantum
	 */
	public static int calculateManagedMemorySize(int elemSize, int elemCount, int activeArrayCount)
	{
		assert elemSize == 8;
		if(elemCount <= 6)
		{
			return HEADER_SIZE; //inlined arrays only have a header
		}
		assert elemSize == 8;
		int elementsPerBlock = BLOCKSIZE / elemSize;
		int neededBlocks = 0;
		if(elemCount % elementsPerBlock != 0)
		{
			neededBlocks++;
		}
		neededBlocks += elemCount / elementsPerBlock;

		int dataSize = neededBlocks * BLOCKSIZE;
		int arraySize = dataSize + HEADER_SIZE;
		return arraySize * activeArrayCount;
	}

	/**
	 * @return the overhead (lost space) from allocate the specified count of arrays in
	 * unmamaged memory.
	 */
	public static int calcualteScopedMemoryOverhead(int elemSize, int elemCount, int totalArrayCount)
	{
		assert elemSize == 8;
		if(elemCount <= 6)
		{
			return 0; //no overhead for inlined arrays
		}
		int elementsPerBlock = BLOCKSIZE / elemSize;
		int neededBlocks = 0;
		if(elemCount % elementsPerBlock != 0)
		{
			neededBlocks++;
		}
		neededBlocks += elemCount / elementsPerBlock;

		int overheadPerArray = neededBlocks * POINTER_SIZE;
		//Align to 32 bytes, to avoid screwing up other allocations that expect a 32-byte boundary.
		overheadPerArray += 32;
		return overheadPerArray * totalArrayCount;

	}

}
