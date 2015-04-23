package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;

import static com.fiji.fivm.r1.unmanaged.UMUtils.fivmr_MemoryArea_allocatePrimitive;
import static com.fiji.fivm.r1.unmanaged.UMUtils.fivmr_MemoryArea_deallocatePrimitive;

/**
 * Created by scottflo on 4/13/15.
 */
public class UMFloat implements UMPrimitive{

    public static float get(Pointer p)
    {
        return p.loadFloat();
    }

    public static void set(Pointer p, float val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive)
    {
        //TODO
        final Pointer curArea = MemoryAreas.getCurrentArea();
        if(curArea == MemoryAreas.getHeapArea() || curArea == MemoryAreas.getImmortalArea())
        {
            throw new UnsupportedOperationException("Deallocation cannot occur in Heap or Immortal Memory");
        }
        //Call to native to Deallocate
        fivmr_MemoryArea_deallocatePrimitive(curArea,primitive);
    }

    public static Pointer allocate(float val)
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