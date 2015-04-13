package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.Pointer;

/**
 * Created by scottflo on 4/13/15.
 */
public class UMBoolean implements UMPrimitive{

    public static boolean get(Pointer p)
    {
        return p.loadBoolean();
    }

    public static void set(Pointer p, boolean val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive) {

    }
}
