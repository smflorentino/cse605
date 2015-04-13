/*
 * MeasureTimeGaps.java
 * Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
 * This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
 * LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
 * available at fivm/LEGAL and can also be found at
 * http://www.fiji-systems.com/FPL3.txt
 * 
 * By installing, reproducing, distributing, and/or using the FIJI VM Software
 * you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
 * rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
 * restrictions stated therein.  Among other conditions and restrictions, the
 * FIJI PUBLIC LICENSE states that:
 * 
 * a. You may only make non-commercial use of the FIJI VM Software.
 * 
 * b. Any adaptation you make must be licensed under the same terms 
 * of the FIJI PUBLIC LICENSE.
 * 
 * c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
 * file, adaptation or output code that you distribute and cause the output code
 * to provide a notice of the FIJI PUBLIC LICENSE. 
 * 
 * d. You must not impose any additional conditions.
 * 
 * e. You must not assert or imply any connection, sponsorship or endorsement by
 * the author of the FIJI VM Software
 * 
 * f. You must take no derogatory action in relation to the FIJI VM Software
 * which would be prejudicial to the FIJI VM Software author's honor or
 * reputation.
 * 
 * 
 * The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
 * representation and provides no warranty of any kind concerning the software.
 * 
 * The FIJI PUBLIC LICENSE and any rights granted therein terminate
 * automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
 */

package com.fiji.fivm.test;

import com.fiji.util.*;
import com.fiji.fivm.*;
import com.fiji.fivm.util.*;
import com.fiji.fivm.r1.*;

public class MeasureTimeGaps {
    @RuntimeImport
    static native long fivmr_readCPUTimestamp();
    
    @RuntimeImport
    static native boolean fivmr_cpuTimestampIsFast();
    
    static long preciseTime() {
        if (fivmr_cpuTimestampIsFast()) {
            return fivmr_readCPUTimestamp();
        } else {
            return Time.nanoTimePrecise();
        }
    }
    
    public static void main(String[] v) throws Exception {
        if (v.length!=3) {
            System.err.println("Usage: MeasureTimeGaps <number of samples> <threshold> <name>");
            System.err.println("Warning: only use this from MeasureTimeSlicingGaps");
            System.exit(1);
        }
        
        int n=Integer.parseInt(v[0]);
        long threshold=Long.parseLong(v[1]);
        String name=v[2];
        
        System.out.println(name+" starting!");
        
        double convFactor=1.0;
        
        if (fivmr_cpuTimestampIsFast()) {
            System.out.println(name+" using CPU timestamp; measuring it.");
            long beforeCPU=fivmr_readCPUTimestamp();
            long beforeOS=Time.nanoTimePrecise();
            Thread.sleep(20000);
            long afterCPU=fivmr_readCPUTimestamp();
            long afterOS=Time.nanoTimePrecise();
            convFactor=
                ((double)(afterOS-beforeOS))/
                ((double)(afterCPU-beforeCPU));
            System.out.println("conversion factor (CPU -> real): "+
                               convFactor);
        }
        
        Thread.currentThread().setPriority(ThreadPriority.MAX_PRIORITY);
        
        Addable times;
        Addable preciseTimes;
        
        try {
            times=new HistoStats(0,100000/40,210*40);
            preciseTimes=new HistoStats(0,(long)(100000/40*convFactor),210*40);
        } catch (OutOfMemoryError e) {
            times=new SimpleStats();
            preciseTimes=new SimpleStats();
        }
        
        MixingDebug preciseTimesMix=new MixingDebug(3);
        
        long pollchecksTakenBefore=PollcheckStats.pollchecksTaken();
        
        long lastTime=System.nanoTime();
        long lastTimePrecise=preciseTime();
        
        while (n-->0) {
            long curTime=System.nanoTime();
            
            if (curTime-lastTime >= threshold || threshold<0) {
                times.add(curTime-lastTime);
            }

            lastTime=curTime;
            
            long curTimePrecise=preciseTime();
            
            // creepy horrible work-around for RTEMS time bugs
            if (curTimePrecise<lastTimePrecise) {
                Time.nanoTimePrecise();
                lastTimePrecise=Time.nanoTimePrecise();
                continue;
            }
            
            if (curTimePrecise-lastTimePrecise >= threshold || threshold<0) {
                preciseTimes.add(curTimePrecise-lastTimePrecise);
            }
            
            preciseTimesMix.add(curTimePrecise-lastTimePrecise);
            
            lastTimePrecise=curTimePrecise;
        }

        long pollchecksTakenAfter=PollcheckStats.pollchecksTaken();
        
        System.out.println(name+": Pollchecks taken total so far: "+pollchecksTakenAfter);
        System.out.println(name+": Pollchecks taken during test run: "+(pollchecksTakenAfter-pollchecksTakenBefore));
        System.out.println(name+": Gap statistics for >= "+threshold+": "+times);
        System.out.println(name+": Precise gap statistics for >= "+threshold+" :"+preciseTimes);
        System.out.println(name+": Mixing Debug: "+preciseTimesMix);
    }
}

