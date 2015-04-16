package com.fiji.fivm.r1.edu.buffalo.cse605;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;

/**
* Created by scottflo on 4/15/15.
*/
public class LOG {
    public static final int DEBUG_MM = 0x000000001;
    private static String message;

    private static int debugLevel;

    static {
        initialize();
    }

    public static void initialize()
    {
        debugLevel = 0x00000000;
        String debugEnv = System.getenv("FIVMR_JAVA_DEBUG_LEVEL");
        if(debugEnv == null || debugEnv.equals(""))
        {
            debugEnv = "0";
        }
        try
        {
            debugLevel = Integer.parseInt(debugEnv);
        }
        catch(NumberFormatException e)
        {
            //Do nothing, default was already set
        }
        System.out.println("Fiji Java Debug Level: " + debugLevel);
    }

    public static void info(int msgLevel, String msg)
    {
//        fivmr_Log_javaLockedPrint(msgLevel, msg);
        if((debugLevel & msgLevel) > 0)
        {
            message = msg;
            Pointer outer = MemoryAreas.getCurrentArea();
            MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
            System.out.println("INFO: " + message);
            MemoryAreas.setCurrentArea(outer);
        }
    }

//    @RuntimeImport
//    public static native void fivmr_Log_javaLockedPrint(final int level, final String msg);
}
