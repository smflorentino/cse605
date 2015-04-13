package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.Pointer;

/**
 * Created by scottflo on 4/13/15.
 */
public class UMFloat implements UMPrimitive{

    public float get(Pointer p)
    {
        return p.loadFloat();
    }

    public void set(Pointer p, float val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive)
    {
        //TODO
    }
}
