/*
 * VMConfiguration.java
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

package com.fiji.mvm;

import com.fiji.fivm.r1.*;
import com.fiji.fivm.util.*;

public final class VMConfiguration {
    Payload payload;
    String[] args;
    TimeSlice ts;
    int maxThreads;
    
    private static String[] EMPTY_ARGS=new String[0];
    
    public VMConfiguration(Payload payload) {
        if (payload==null) {
            throw new NullPointerException("Cannot create a VMConfiguration with a null payload");
        }
        this.payload=payload;
        this.args=EMPTY_ARGS;
        this.ts=null;
        
        Pointer config=CType.getPointer(payload.payloadPtr,
                                        "fivmr_Payload",
                                        "defConfig");
        
        this.maxThreads=CType.getInt(config,
                                     "fivmr_Configuration",
                                     "maxThreads");
    }
    
    public void setArguments(String[] args) {
        this.args=args;
    }
    
    public String[] getArguments() {
        return args;
    }
    
    public void setTimeSlice(TimeSlice ts) {
        this.ts=ts;
    }
     
    public TimeSlice getTimeSlice() {
        return ts;
    }
    
    public GCType getGCType() {
        return payload.getGCType();
    }
    
    public int getNumInternalVMThreads() {
        return payload.getNumInternalVMThreads();
    }
    
    private static final int NUM_THREADSTATES_RESERVED=2;
    
    public void setMaxThreads(int maxThreads) {
        if (maxThreads<1) {
            throw new IllegalArgumentException("Cannot configure VM for "+maxThreads+" threads; need at least 1");
        }
        this.maxThreads=maxThreads+NUM_THREADSTATES_RESERVED;
    }
    
    public int getMaxThreads() {
        return maxThreads-NUM_THREADSTATES_RESERVED;
    }
    
    public void setMaxVMThreads(int maxThreads) {
        if (maxThreads<payload.getNumInternalVMThreads()) {
            throw new IllegalArgumentException("Cannot configure VM for "+maxThreads+" VM threads; need at least "+(payload.getNumInternalVMThreads()));
        }
        this.maxThreads=maxThreads-payload.getNumInternalVMThreads()+NUM_THREADSTATES_RESERVED;
        if (false) {
            System.out.println("maxThreads = "+maxThreads);
            System.out.println("this.maxThreads = "+this.maxThreads);
        }
    }
    
    public int getMaxVMThreads() {
        return maxThreads+payload.getNumInternalVMThreads()-NUM_THREADSTATES_RESERVED;
    }

    public Payload getPayload() {
        return payload;
    }
}


