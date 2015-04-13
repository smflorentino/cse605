/*
 * MeasureTimeSlicingGaps.java
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

import com.fiji.mvm.*;
import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;
import java.util.*;

public class MeasureTimeSlicingGaps {
    public static void main(String[] v) throws Exception {
        // make sure we're at high enough priority
        Thread.currentThread().setPriority(ThreadPriority.MAX_PRIORITY);

        long quantum;
        long duration;
        int nvms;
        int counts;

        if (v.length==0) {
            quantum=10000000;
            duration=100000;
            nvms=2;
            counts=10000;
        } else {
            if (v.length!=4) {
                System.err.println("Usage: RunTimeSliced <quantum> <duration> <num VMs> <counts>");
                System.exit(1);
            }
            
            quantum=Long.parseLong(v[0]);
            duration=Long.parseLong(v[1]);
            nvms=Integer.parseInt(v[2]);
            counts=Integer.parseInt(v[3]);
        }
        
        Payload gapMeasurer=Payload.getPayloadByEntrypoint("com/fiji/fivm/test/MeasureTimeGaps");
        if (gapMeasurer==null) {
            throw new Fail("Could not find MeasureTimeGaps payload");
        }
        
        System.out.println("Will run "+nvms+" VMs with a "+quantum+" quantum.");

        TimeSliceManager tsm=new TimeSliceManager(nvms,
                                                  ThreadPriority.MAX_PRIORITY);
        
        for (int i=0;i<nvms;++i) {
            System.out.println("Initializing time slice #"+(i+1));
            tsm.initTimeSlice(i,quantum,
                              gapMeasurer.getNumInternalVMThreads()+2,
                              ThreadPriority.MAX_PRIORITY-1);
        }

        tsm.start();

        for (int i=0;i<nvms;++i) {
            System.out.println("Launching VM #"+(i+1));
            TimeSlice ts=tsm.getTimeSlice(i);
            VMConfiguration config=new VMConfiguration(gapMeasurer);
            config.setArguments(
                new String[]{
                    ""+counts,
                    "-1",
                    "VM#"+(i+1)});
            ts.spawnAutoRestart(config);
        }
        
        System.out.println("VMs running.");

        Thread.sleep(duration);
        
        System.out.println("That worked.");
        System.exit(0);
    }
}

