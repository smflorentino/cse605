package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.Inline;
import com.fiji.fivm.r1.Pointer;

/**
 * Created by scottflo on 4/13/15.
 */
public class UMDouble implements UMPrimitive{

    public double get(Pointer p)
    {
        return p.loadDouble();
    }

    public void set(Pointer p, double val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive) {
        //TODO
    }
}
