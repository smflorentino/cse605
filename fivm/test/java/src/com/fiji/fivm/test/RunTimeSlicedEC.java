/*
 * RunTimeSlicedEC.java
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
import java.util.*;

public class RunTimeSlicedEC {
    public static void main(String[] v) throws Exception {
        // make sure we're at high enough priority
        Thread.currentThread().setPriority(ThreadPriority.MAX_PRIORITY);

        long quantum;
        long duration;
        
        if (v.length==0) {
            quantum=50*1000*1000;
            duration=25*1000;
        } else if (v.length==2) {
            quantum=Long.parseLong(v[0]);
            duration=Long.parseLong(v[1]);
        } else {
            System.err.println("Usage: RunTimeSliced [<quantum> <duration>]");
            System.err.println("Without arguments we default to quantum = 500000000, duration = 25000");
            System.exit(1);
            throw new Error(); // make javac happy
        }
        
        List< Payload > payloads = Payload.subPayloads();
        
        System.out.println("Will run "+payloads.size()+" VMs with a "+quantum+" quantum.");

        TimeSliceManager tsm=new TimeSliceManager(payloads.size(),
                                                  ThreadPriority.MAX_PRIORITY);
        
        for (int i=0;i<payloads.size();++i) {
            Payload p=payloads.get(i);
            System.out.println("Creating timeslice for "+p);
            int numThreads=p.getNumInternalVMThreads()+2;
            System.out.println("   Allowing "+numThreads+" threads.");
            tsm.initTimeSlice(i,quantum,numThreads,ThreadPriority.MAX_PRIORITY-1);
        }
        
        tsm.start();

        for (int i_=0;i_<payloads.size();++i_) {
            final int i=i_;
            System.out.println("Launching VM for "+payloads.get(i));
            final VMConfiguration config=new VMConfiguration(payloads.get(i));
            final VMController vmctrl=new VMController();
            tsm.getTimeSlice(i).spawnController(
                new Runnable() {
                    public void run() {
                        vmctrl.spawn(config);
                        if (vmctrl.getLastResult()!=0) {
                            System.err.println("VM #"+i+" FAILED!");
                            System.exit(1);
                        }
                    }
                });
        }
        
        Thread.sleep(duration);
        
        System.out.println("That worked.");
        System.exit(0);
    }
}

