/*
 * SPC.java
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

package com.fiji.fivm;

import com.fiji.fivm.r1.*;

public class SPC {
    private SPC() {}
    
    private static Pointer[] counts;
    private static String[] names;
    
    @StackAllocation
    private static void prepareSPC() {
        Pointer[] buffer=new Pointer[Math.max(fivmr_SPC_numCounts(),
                                              fivmc_SPC_numCounts())];
        
        fivmc_SPC_getCounts(Magic.addressOfElement(buffer,0));
        System.arraycopy(buffer,0,
                         counts,0,
                         fivmc_SPC_numCounts());
        
        fivmr_SPC_getCounts(Magic.addressOfElement(buffer,0));
        System.arraycopy(buffer,0,
                         counts,fivmc_SPC_numCounts(),
                         fivmr_SPC_numCounts());
        
        fivmc_SPC_getNames(Magic.addressOfElement(buffer,0));
        for (int i=0;i<fivmc_SPC_numCounts();++i) {
            names[i]=fivmRuntime.fromCStringFull(buffer[i]);
        }
        
        fivmr_SPC_getNames(Magic.addressOfElement(buffer,0));
        for (int i=0;i<fivmr_SPC_numCounts();++i) {
            names[i+fivmc_SPC_numCounts()]=fivmRuntime.fromCStringFull(buffer[i]);
        }
    }
    
    static {
        counts=new Pointer[fivmr_SPC_numCounts()+fivmc_SPC_numCounts()];
        names=new String[fivmr_SPC_numCounts()+fivmc_SPC_numCounts()];
        prepareSPC();
    }
    
    @NoSafetyChecks
    public static int[] getCounts() {
        int[] result=new int[counts.length];
        for (int i=0;i<counts.length;++i) {
            result[i]=counts[i].loadPointer().castToInt();
        }
        return result;
    }
    
    public static String[] getNames() {
        return names.clone();
    }
    
    public static String getName(int idx) {
        return names[idx];
    }
    
    @NoSafetyChecks
    public static void resetCounts() {
        for (int i=0;i<counts.length;++i) {
            counts[i].store(Pointer.fromInt(0));
        }
    }
    
    @RuntimeImport
    private static native int fivmr_SPC_numCounts();
    
    @RuntimeImport
    private static native void fivmr_SPC_getCounts(Pointer buffer);
    
    @RuntimeImport
    private static native void fivmr_SPC_getNames(Pointer buffer);
    
    private static int fivmc_SPC_numCounts() {
        if (Settings.SPC_ENABLED) {
            return Helper.fivmc_SPC_numCounts();
        } else {
            return 0;
        }
    }
    
    private static void fivmc_SPC_getCounts(Pointer buffer) {
        if (Settings.SPC_ENABLED) {
            Helper.fivmc_SPC_getCounts(buffer);
        }
    }
    
    private static void fivmc_SPC_getNames(Pointer buffer) {
        if (Settings.SPC_ENABLED) {
            Helper.fivmc_SPC_getNames(buffer);
        }
    }
    
    static class Helper {
        @UnsupUnlessSet({"SPC_ENABLED"})
        @Import
        private static native int fivmc_SPC_numCounts();
        
        @UnsupUnlessSet({"SPC_ENABLED"})
        @Import
        private static native void fivmc_SPC_getCounts(Pointer buffer);
        
        @UnsupUnlessSet({"SPC_ENABLED"})
        @Import
        private static native void fivmc_SPC_getNames(Pointer buffer);
    }
}

