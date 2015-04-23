package com.fiji.fivm.r1.unmanaged;

import com.fiji.fivm.r1.Inline;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.NoSafepoint;
import com.fiji.fivm.r1.RuntimeImport;

import static com.fiji.fivm.r1.unmanaged.UMUtils.fivmr_MemoryArea_allocatePrimitive;
/**
 * Created by scottflo on 4/13/15.
 */
public class UMDouble implements UMPrimitive{

    public static double get(Pointer p)
    {
        return p.loadDouble();
    }

    public static void set(Pointer p, double val)
    {
        p.store(val);
    }

    public static void free(Pointer primitive) {
        //TODO
        final Pointer curArea = MemoryAreas.getCurrentArea();
        if(curArea == MemoryAreas.getHeapArea() || curArea == MemoryAreas.getImmortalArea())
        {
            throw new UnsupportedOperationException("Deallocation cannot occur in Heap or Immortal Memory");
        }
        //Call to native to Deallocate
        fivmr_MemoryArea_deallocatePrimitive(curArea,primitive);
    }

    public static Pointer allocate(double val)
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