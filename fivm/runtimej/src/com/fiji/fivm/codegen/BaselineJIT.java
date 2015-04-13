/*
 * BaselineJIT.java
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

package com.fiji.fivm.codegen;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;

@ExcludeUnlessSet({"CLASSLOADING"})
public abstract class BaselineJIT {
    @Inline
    public static BaselineJIT getJIT() {
        if (Settings.CLASSLOADING) {
            if (Settings.X86) {
                return new com.fiji.fivm.codegen.x86.BaselineJIT();
            } else {
                throw new UnsupportedPlatformException();
            }
        } else {
            throw new fivmError("Cannot use JIT with class loading disabled.");
        }
    }
    
    public abstract MachineCode createLoadThunkFor(Pointer methodRec);
    
    public abstract MachineCode createCodeFor(Pointer methodRec);
    public abstract MachineCode createJNITrampoline(Pointer methodRec);
    public abstract MachineCode createCloneHelperFor(Pointer methodRec);
    
    public MachineCode createEntrypointFor(Pointer methodRec) {
        switch (fivmRuntime.fivmr_MethodRec_flags(methodRec)&Constants.MBF_METHOD_IMPL) {
        case Constants.MBF_BYTECODE:
            return createCodeFor(methodRec);
        case Constants.MBF_JNI:
            return createJNITrampoline(methodRec);
        case Constants.MBF_SYNTHETIC: {
            String name=fivmRuntime.fromCStringFull(fivmRuntime.fivmr_MethodRec_name(methodRec));
            if (name.equals(Constants.SMN_CLONE_HELPER)) {
                return createCloneHelperFor(methodRec);
            } else {
                throw new fivmError("Unrecognized synthetic method: "+name);
            }
        }
        default:
            throw new fivmError("Cannot handle method with flags: "+
                                fivmRuntime.fivmr_MethodRec_flags(methodRec));
        }
    }
    
    public abstract MachineCode createInterfaceResolutionFor(Pointer methodRec);
    
    public abstract MachineCode createPatchToCodeFor(Pointer methodRec,
                                                     int bytecodeStartPC);
    
    public abstract MachineCode createFieldAccess(Pointer methodRec,
                                                  int fat,
                                                  Pointer fr,
                                                  Pointer returnAddr,
                                                  int stackHeight,
                                                  int recvType,
                                                  int dataType);
    
    public abstract boolean fieldAccessPatched(Pointer returnAddr);

    // On X86 this is easy.  On other platforms it's harder.  PPC gives us
    // a 24-bit (ish) relative long jump, which will work most of the time
    // but certainly not all of the time.  Don't know about ARM or SPARC.
    // But:
    //
    // - The code following the resolver call (the "code" that contains the
    //   encoded owner, type, and name) is unreachable, so we can overwrite
    //   it with a suitable extra-long jump.  (I.e. sequence of load-immediates
    //   to place the jump target into a register and then jump through the
    //   register.)
    //
    // - Then we can patch the long-jump to point at that extra-long-jump
    //   code.
    //
    // - Most of the time we *won't* have to do that, since the slice of
    //   address space that we'll use will be small enough that the normal
    //   long jump will suffice in a large majority of cases.
    public abstract void patchFieldAccessTo(Pointer returnAddr,
                                            Pointer target);
    
    public abstract MachineCode createMethodCall(Pointer methodRec,
                                                 int mct,
                                                 Pointer mr,
                                                 Pointer returnAddr,
                                                 int stackHeight);
    
    public abstract boolean methodCallPatched(Pointer returnAddr,
                                              int mct);
    
    public abstract void patchMethodCallTo(Pointer returnAddr,
                                           int mct,
                                           Pointer target);
    
    /** Create a method implementation that just throws an exception. */
    public abstract MachineCode createExceptionThrow(Pointer methodRec,
                                                     ExceptionThrower et);
    
    /** Create a sub machine code for a method to throw an exception. */
    public abstract MachineCode createExceptionThrowSub(Pointer methodRec,
                                                        ExceptionThrower et);
    
    public abstract MachineCode createArrayAlloc(Pointer methodRec,
                                                 Pointer type,
                                                 Pointer returnAddr,
                                                 int stackHeight,
                                                 Pointer debugID);
    
    public abstract boolean arrayAllocPatched(Pointer returnAddr);
    
    public abstract void patchArrayAllocTo(Pointer returnAddr,
                                           Pointer target);
    
    public abstract MachineCode createObjectAlloc(Pointer methodRec,
                                                  Pointer type,
                                                  Pointer returnAddr,
                                                  int stackHeight,
                                                  Pointer debugID);
    
    public abstract boolean objectAllocPatched(Pointer returnAddr);
    
    public abstract void patchObjectAllocTo(Pointer returnAddr,
                                            Pointer target);
    
    public abstract MachineCode createInstanceof(Pointer methodRec,
                                                 int iot,
                                                 Pointer type,
                                                 Pointer returnAddr,
                                                 int stackHeight,
                                                 Pointer debugID);
    
    public abstract boolean instanceofPatched(Pointer returnAddr);
    
    public abstract void patchInstanceofTo(Pointer returnAddr,
                                           Pointer target);
}


