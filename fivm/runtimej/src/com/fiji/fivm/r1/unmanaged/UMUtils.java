package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.NoSafepoint;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.RuntimeImport;


/**
 * Created by scottflo on 4/20/15.
 */
public class UMUtils
{
	@RuntimeImport
	@NoSafepoint
	public static native Pointer fivmr_MemoryArea_allocatePrimitive(Pointer currentArea);

	@RuntimeImport
	@NoSafepoint
	public static native Pointer fivmr_MemoryArea_deallocatePrimitive(Pointer currentArea, Pointer primitive);

	@RuntimeImport
	@NoSafepoint
	public static native Pointer fivmr_MemoryArea_allocateArray(Pointer ts, int type, int elemCount);

	@RuntimeImport
	@NoSafepoint
	public static native void fivmr_MemoryArea_freeArray(Pointer currentArea, Pointer array);

	@RuntimeImport
	@NoSafepoint
	public static native int fivmr_MemoryArea_loadArrayInt(Pointer array, int index);

	@RuntimeImport
	@NoSafepoint
	public static native void fivmr_MemoryArea_storeArrayInt(Pointer array, int index, int value);
}
