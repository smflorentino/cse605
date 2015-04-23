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
        final String MSG = "HEAP: " + msg;
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println(MSG);
        MemoryAreas.setCurrentArea(outer);
    }

    public static void HEAP(final long l)
    {
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println("HEAP: " + l);
        MemoryAreas.setCurrentArea(outer);
    }

    public static void HEAP_ERROR_CODE(final int errorCode)
    {
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println("HEAP: An assertion Failed! Error Code "+ errorCode);
        MemoryAreas.setCurrentArea(outer);
    }

    public static void FAIL(String msg)
    {
        System.out.println("FAIL:" + msg);
    }

}
