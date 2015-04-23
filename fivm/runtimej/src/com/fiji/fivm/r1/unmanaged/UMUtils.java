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
}
