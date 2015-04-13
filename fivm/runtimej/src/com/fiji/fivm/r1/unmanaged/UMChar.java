package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.Pointer;

/**
 * Created by scottflo on 4/13/15.
 */
public class UMChar implements UMPrimitive {

    public char get(Pointer p)
    {
        return p.loadChar();
    }

    public void set(Pointer p, char val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive) {

    }
}
