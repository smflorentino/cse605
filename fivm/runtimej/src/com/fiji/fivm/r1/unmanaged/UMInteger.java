package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.Pointer;

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
}
