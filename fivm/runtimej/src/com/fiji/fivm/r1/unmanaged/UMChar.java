package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.NoSafepoint;
import com.fiji.fivm.r1.RuntimeImport;

import static com.fiji.fivm.r1.unmanaged.UMUtils.fivmr_MemoryArea_allocatePrimitive;

/**
 * Created by scottflo on 4/13/15.
 */
public class UMChar implements UMPrimitive {

    public static char get(Pointer p)
    {
        return p.loadChar();
    }

    public static void set(Pointer p, char val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive) {
        //TODO:
    }

    public static Pointer allocate(char val)
    {
        //We can't be in the Heap OR Immortal Memory
        final Pointer curArea = MemoryAreas.getCurrentArea();
        if(curArea == MemoryAreas.getHeapArea() || curArea == MemoryAreas.getImmortalArea())
        {
            throw new UnsupportedOperationException("Allocation cannot occur in Heap or Immortal Memory");
        }
        Pointer primitivePointer =  fivmr_MemoryArea_allocatePrimitive(curArea);
        primitivePointer.store(val);
        return primitivePointer;
    }
}