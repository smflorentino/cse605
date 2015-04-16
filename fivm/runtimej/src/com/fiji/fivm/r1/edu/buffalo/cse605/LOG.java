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
        debugLevel = 0x00000000;
        try
        {
            debugLevel = Integer.parseInt(System.getenv("FIVMR_JAVA_DEBUG_LEVEL"));
        }
        catch(NumberFormatException e)
        {
            //Do nothing, default was already set
        }
        System.out.println("Fiji Java Debug Level: " + debugLevel);
    }

    public static void info(int msgLevel, String msg)
    {
        if((debugLevel & msgLevel) > 0)
        {
            message = msg;
            Pointer outer = MemoryAreas.getCurrentArea();
            MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
            System.out.println("INFO: " + message);
            MemoryAreas.setCurrentArea(outer);
        }
    }

}
