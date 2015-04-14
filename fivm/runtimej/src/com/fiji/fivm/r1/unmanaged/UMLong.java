package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;

/**
 * Created by scottflo on 4/13/15.
 */
public class UMLong implements UMPrimitive{

    public long get(Pointer p)
    {
        return p.loadLong();
    }

    public void set(Pointer p, long val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive) {

    }
}
