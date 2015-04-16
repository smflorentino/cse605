package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.NoSafepoint;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.RuntimeImport;

/**
 * Created by scottflo on 4/13/15.
 */
public class UMInteger implements UMPrimitive {
    public static int get(Pointer p)
    {
        return p.loadInt();
    }

    public static void set(Pointer p, int val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive)
    {
        //TODO
    }

    public static Pointer allocate(int val)
    {
        return fivmr_MemoryArea_allocateInt(val);
    }

    @RuntimeImport
    @NoSafepoint
    public static native Pointer fivmr_MemoryArea_allocateInt(int val);
}
