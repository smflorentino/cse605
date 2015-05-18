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

    public static void PRINT(int val)
    {
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println(val);
        MemoryAreas.setCurrentArea(outer);
    }

    public static void PRINT(char c)
    {
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println(c);
        MemoryAreas.setCurrentArea(outer);
    }

    public static void PRINT(long l)
    {
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println(l);
        MemoryAreas.setCurrentArea(outer);
    }

    public static void PRINT(byte b)
    {
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println(b);
        MemoryAreas.setCurrentArea(outer);
    }

    public static void PRINT(short s)
    {
        Pointer outer = MemoryAreas.getCurrentArea();
        MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
        System.err.println(s);
        MemoryAreas.setCurrentArea(outer);
    }
}
