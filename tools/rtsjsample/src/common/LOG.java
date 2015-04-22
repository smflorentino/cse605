package common;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;

/**
 * Created by scottflo on 4/15/15.
 */
public class LOG {

    public static void info(String log)
    {
        System.out.println("INFO: " + log);
    }

    public static void HEAP(String msg)
    {
        final String MSG = msg;
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println(MSG);
        MemoryAreas.setCurrentArea(outer);
    }
}
