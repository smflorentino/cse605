/*
 * GateHelpers.java
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
import static com.fiji.fivm.r1.fivmRuntime.*;

// FIXME this class should be REMOVED from public view!
public final class GateHelpers {
    private GateHelpers() {}
    
    @Import
    @GodGiven
    @NoThrow
    private static native Pointer fivmr_addressOfElement(Pointer settings,
                                                         Pointer object,
                                                         int index,
                                                         Pointer eleSize);
    
    public static Pointer addressOfElement(Pointer vm,
                                           Pointer object,
                                           int index,
                                           Pointer eleSize) {
        return fivmr_addressOfElement(vm.add(CType.offsetof("fivmr_VM",
                                                            "settings")),
                                      object,
                                      index,
                                      eleSize);
    }
    
    public static Pointer translateTypeData(Gate gate,
                                            Pointer myTypeData) {
        return gate.typeDataMap[PointerArrays.binarySearch(gate.myTypeDatasSorted,myTypeData)];
    }
    
    @Intrinsic
    public static native void callGateCopyTo(Object source,
                                             Pointer ts,
                                             Pointer target,
                                             Gate gate);
    
    @Import
    @GodGiven
    @NoThrow
    private static native Pointer
    fivmr_GateHelpers_installObjectFieldReference(Pointer remoteTS,
                                                  Pointer referent,
                                                  Pointer fieldOffset,
                                                  Pointer td);
    
    @Import
    @GodGiven
    @NoThrow
    private static native Pointer
    fivmr_GateHelpers_installObjectElementReference(Pointer remoteTS,
                                                    Pointer referent,
                                                    int index,
                                                    Pointer td);
    
    @Import
    @GodGiven
    @NoThrow
    private static native Pointer
    fivmr_GateHelpers_installArrayFieldReference(Pointer remoteTS,
                                                 Pointer referent,
                                                 Pointer fieldOffset,
                                                 Pointer td,
                                                 int length);
    
    @Import
    @GodGiven
    @NoThrow
    private static native Pointer
    fivmr_GateHelpers_installArrayElementReference(Pointer remoteTS,
                                                   Pointer referent,
                                                   int index,
                                                   Pointer td,
                                                   int length);
    
    @Import
    @GodGiven
    @NoThrow
    private static native Pointer fivmr_Handle_get(Pointer handle);
    
    @Import
    @GodGiven
    @NoThrow
    private static native void fivmr_ThreadState_removeHandle(Pointer remoteTS,
                                                              Pointer handle);
    
    public static Pointer installObjectFieldReference(Pointer remoteTS,
                                                      Pointer referent,
                                                      Pointer fieldOffset,
                                                      Pointer typeData) {
        Pointer handle=fivmr_GateHelpers_installObjectFieldReference(remoteTS,
                                                                     referent,
                                                                     fieldOffset,
                                                                     typeData);
        if (handle==Pointer.zero()) {
            throw new Gate.CallFailedException("Failed to allocate object copy in remote VM");
        }
        return handle;
    }
    
    public static Pointer installObjectElementReference(Pointer remoteTS,
                                                        Pointer referent,
                                                        int index,
                                                        Pointer typeData) {
        Pointer handle=fivmr_GateHelpers_installObjectElementReference(remoteTS,
                                                                       referent,
                                                                       index,
                                                                       typeData);
        if (handle==Pointer.zero()) {
            throw new Gate.CallFailedException("Failed to allocate object copy in remote VM");
        }
        return handle;
    }
    
    public static Pointer installArrayFieldReference(Pointer remoteTS,
                                                     Pointer referent,
                                                     Pointer fieldOffset,
                                                     Pointer typeData,
                                                     int length) {
        Pointer handle=fivmr_GateHelpers_installArrayFieldReference(remoteTS,
                                                                    referent,
                                                                    fieldOffset,
                                                                    typeData,
                                                                    length);
        if (handle==Pointer.zero()) {
            throw new Gate.CallFailedException("Failed to allocate object copy in remote VM");
        }
        return handle;
    }
    
    public static Pointer installArrayElementReference(Pointer remoteTS,
                                                       Pointer referent,
                                                       int index,
                                                       Pointer typeData,
                                                       int length) {
        Pointer handle=fivmr_GateHelpers_installArrayElementReference(remoteTS,
                                                                      referent,
                                                                      index,
                                                                      typeData,
                                                                      length);
        if (handle==Pointer.zero()) {
            throw new Gate.CallFailedException("Failed to allocate object copy in remote VM");
        }
        return handle;
    }
    
    public static Pointer getObject(Pointer handle) {
        return fivmr_Handle_get(handle);
    }
    
    public static void removeHandle(Pointer remoteTS,
                                    Pointer handle) {
        fivmr_ThreadState_removeHandle(remoteTS,handle);
    }

    // ugh...  for fields we will know statically if it's an object or an array.
    // but we may have limited knowledge beyond that.
    public static void copyObjectOrArrayFieldReference(Pointer remoteTS,
                                                       Pointer referent,
                                                       Pointer fieldOffset,
                                                       Gate gate,
                                                       Object source) {
        Pointer handle;
        Pointer myTypeData=Magic.typeDataFor(source);
        Pointer remoteTypeData=translateTypeData(gate,myTypeData);
        if (fivmr_TypeData_isArray(myTypeData)) {
            handle=installArrayFieldReference(remoteTS,
                                              referent,
                                              fieldOffset,
                                              remoteTypeData,
                                              MM.arrayLength(source));
        } else {
            handle=installObjectFieldReference(remoteTS,
                                               referent,
                                               fieldOffset,
                                               remoteTypeData);
        }
        callGateCopyTo(source,
                       remoteTS,
                       getObject(handle),
                       gate);
        removeHandle(remoteTS,handle);
    }
    
    public static void copyObjectOrArrayElementReference(Pointer remoteTS,
                                                         Pointer referent,
                                                         int index,
                                                         Gate gate,
                                                         Object source) {
        Pointer handle;
        Pointer myTypeData=Magic.typeDataFor(source);
        Pointer remoteTypeData=translateTypeData(gate,myTypeData);
        if (fivmr_TypeData_isArray(myTypeData)) {
            handle=installArrayElementReference(remoteTS,
                                                referent,
                                                index,
                                                remoteTypeData,
                                                MM.arrayLength(source));
        } else {
            handle=installObjectElementReference(remoteTS,
                                                 referent,
                                                 index,
                                                 remoteTypeData);
        }
        callGateCopyTo(source,
                       remoteTS,
                       getObject(handle),
                       gate);
        removeHandle(remoteTS,handle);
    }
}

