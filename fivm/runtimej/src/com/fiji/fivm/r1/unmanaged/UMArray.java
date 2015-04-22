package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.AllowUnsafe;
import com.fiji.fivm.r1.CType;
import com.fiji.fivm.r1.Inline;
import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.NoInline;
import com.fiji.fivm.r1.NoPollcheck;
import com.fiji.fivm.r1.NoReturn;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.Reflect;
import com.fiji.fivm.r1.fivmRuntime;

import static com.fiji.fivm.r1.fivmRuntime.throwArrayBoundsRTE;
import static com.fiji.fivm.r1.fivmRuntime.throwNegativeSizeRTE;
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
		return UMUtils.fivmr_MemoryArea_allocateArray(curArea, type.getVal(), size);
	}

	public static int getInt(Pointer array, int index)
	{
		nullCheckAndArrayBoundsCheck(array, index);
		//Type check
		if(Magic.semanticallyUnlikely(CType.getInt(array,"fivmr_um_array_header","type") != 0)) {
			throw new ClassCastException();
		}
		return fivmr_MemoryArea_loadArrayInt(array, index);
	}

	public static void setInt(Pointer array, int index, int val)
	{
		nullCheckAndArrayBoundsCheck(array, index);
		//Type check
		if(Magic.semanticallyUnlikely(CType.getInt(array,"fivmr_um_array_header","type") != 0)) {
			throw new ClassCastException();
		}
		fivmr_MemoryArea_storeArrayInt(array, index, val);
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

	@Inline
	@NoPollcheck
	@AllowUnsafe
	private static int length(Pointer array)
	{
		return CType.getInt(array,"fivmr_um_array_header","size");
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
}
