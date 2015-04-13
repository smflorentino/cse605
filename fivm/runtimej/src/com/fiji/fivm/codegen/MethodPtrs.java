/*
 * MethodPtrs.java
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

import static com.fiji.fivm.r1.Magic.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import com.fiji.fivm.r1.*;

public final class MethodPtrs {
    private MethodPtrs() {}
    
    private static Pointer entrypoint(Pointer mr) {
        return fivmr_MethodRec_entrypoint(mr);
    }
    
    public static final Pointer handleLoadThunk=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/handleLoadThunk(P)V"));
    
    public static final Pointer throwNullPointerRTE=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/throwNullPointerRTE()V"));
    
    public static final Pointer throwArithmeticRTE=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/throwArithmeticRTE()V"));
    
    public static final Pointer throwArrayBoundsRTE=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/throwArrayBoundsRTE()V"));
    
    public static final Pointer throwClassCastRTE=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/throwClassCastRTE()V"));
    
    public static final Pointer throwClassChangeRTE=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/throwClassChangeRTE()V"));
    
    public static final Pointer throwNoClassDefFoundError_forBaseline=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/"+
                     "throwNoClassDefFoundError_forBaseline()V"));
    
    public static final Pointer throwStackOverflowRTE=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/"+
                     "throwStackOverflowRTE()V"));
    
    public static final Pointer objectArrayStore=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/"+
                     "objectArrayStore(Ljava/lang/Object;ILjava/lang/Object;)V"));
    
    public static final Pointer objectPutField=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/"+
                     "objectPutField(Ljava/lang/Object;PLjava/lang/Object;I)V"));
    
    public static final Pointer objectPutStatic=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/fivmRuntime;/"+
                     "objectPutStatic(PLjava/lang/Object;I)V"));
    
    public static final Pointer lock=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/Monitors;/lock(Ljava/lang/Object;)V"));
        
    public static final Pointer unlock=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/Monitors;/unlock(Ljava/lang/Object;)V"));
    
    public static final Pointer lockSlow=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/Monitors;/lockSlow(Ljava/lang/Object;)V"));
        
    public static final Pointer unlockSlow=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/Monitors;/unlockSlow(Ljava/lang/Object;)V"));
    
    public static final Pointer alloc=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/alloc(IP)Ljava/lang/Object;"));
    
    public static final Pointer allocArray=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/allocArray(IPI)Ljava/lang/Object;"));
    
    public static final Pointer multianewarray=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/multianewarray(PIP)Ljava/lang/Object;"));
    
    public static final Pointer throwException=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/ExceptionThrower;/"+
                     "throwException(Lcom/fiji/fivm/r1/ExceptionThrower;)V"));
    
    public static final Pointer throwOOMEOrNASE=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/throwOOMEOrNASE(I)V"));
    
    public static final Pointer throwOOME=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/throwOOME()V"));
    
    public static final Pointer throwNASE=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/throwNASE()V"));
    
    public static final Pointer allocSlow=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/allocSlow(IP)Ljava/lang/Object;"));
    
    public static final Pointer allocArraySlow=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/allocArraySlow(IPI)Ljava/lang/Object;"));
    
    public static final Pointer addDestructor=entrypoint(
        getMethodRec("Lcom/fiji/fivm/r1/MM;/addDestructor(ILjava/lang/Object;)Ljava/lang/Object;"));
}

