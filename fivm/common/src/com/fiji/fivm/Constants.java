/*
 * Constants.java
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

/**
 * Constants relevant to the runtime, which are also used in the compiler.  Note
 * that these constants must be in sync with the C runtime's fivmr.h.  Furthermore,
 * note that it is preferrable, when generating code in the compiler, to refer to
 * these constants symbolically except in cases where this would be impossible or
 * excessively complicated.
 */
public interface Constants {
    // thread flags
    
    public static final int TS_STATE_BUF_LEN       = 64;
    
    // binding flags
    
    public static final int BF_CLEAR               = 0;

    public static final int BF_STATIC              = 1;
    
    public static final int BF_VISIBILITY          = 6;
    public static final int BF_PRIVATE             = 0;
    public static final int BF_PACKAGE             = 2;
    public static final int BF_PROTECTED           = 4;
    public static final int BF_PUBLIC              = 6;
    
    public static final int FBF_FINAL              = 8;
    public static final int FBF_VOLATILE           = 16;
    public static final int FBF_TRANSIENT          = 32;
    public static final int FBF_UNTRACED           = 64;
    public static final int FBF_NOT_A_REFERENCE    = 128;
    // NOTE: barrier flags start at 65536
    
    public static final int MBF_METHOD_KIND        = 24;
    public static final int MBF_FINAL              = 0;
    public static final int MBF_VIRTUAL            = 8;
    public static final int MBF_ABSTRACT           = 16;
    
    public static final int MBF_SYNCHRONIZED       = 32;
    
    public static final int MBF_METHOD_IMPL        = 448;
    public static final int MBF_STUB               = 0;
    public static final int MBF_BYTECODE           = 64;
    public static final int MBF_JNI                = 128;
    public static final int MBF_INTRINSIC          = 192;
    public static final int MBF_IMPORT             = 256;
    public static final int MBF_UNSUPPORTED        = 320;
    public static final int MBF_SYNTHETIC          = 384;
    
    public static final int MBF_HAS_CODE           = 512;
    
    public static final int MBF_COV_CALLED         = 1024;
    
    public static final int MBF_ALLOC_AS_CALLER    = 2048;
     
    public static final int MBF_EXISTS             = 4096;
    
    public static final int MBF_DYNAMIC            = 8192;
    
    public static final int MBF_RT_EXC_THROWER     = 16384;
    
    public static final int MBF_COOKIE             = 1073741824;
     
    public static final int TBF_TYPE_KIND          = 120;
    public static final int TBF_PRIMITIVE          = 0;
    public static final int TBF_ARRAY              = 8;
    public static final int TBF_ANNOTATION         = 16;
    public static final int TBF_INTERFACE          = 24;
    public static final int TBF_ABSTRACT           = 32;
    public static final int TBF_VIRTUAL            = 40;
    public static final int TBF_FINAL              = 48;
    public static final int TBF_STUB               = 56;
    
    public static final byte TBF_RESOLUTION_DONE_BIT = 7;
    public static final int TBF_RESOLUTION_DONE    = 1<<TBF_RESOLUTION_DONE_BIT;
    public static final int TBF_RESOLUTION_FAILED  = 258;
    
    public static final int TBF_NEW_SUPER_MODE     = 16384;
    
    public static final int TBF_AOT                = 32768;

    public static final int TBF_OVERRIDE_ALL       = 65536;
    public static final int TBF_OVERRIDE_APP       = 131072;
    
    public static final int TBF_FINALIZABLE        = 262144;
    
    public static final int TBF_SPECIAL_SCAN       = 524288;
    
    // call mode flags
    
    public static final int CM_HANDLES             = 1;
    public static final int CM_EXEC_STATUS         = 2;
    public static final int CM_DISPATCH            = 4;
    public static final int CM_NULLCHECK           = 8;
    public static final int CM_CLASSCHANGE         = 16;
    public static final int CM_CHECKINIT           = 32;
    public static final int CM_RETURN_ARGS_BUF     = 64;
    public static final int CM_WRAP_EXCEPTION      = 128;
    
    // GC map flag(s)
    public static final int GCM_THIN               = 1;
    
    public static final int NOGC                   = 0;
    public static final int CMRGC                  = 1;
    public static final int CMRGC_PURE             = 2;
    public static final int HFGC                   = 3;
    
    public static final int CMR_GC_MARK1           = 1;
    public static final int CMR_GC_MARK2           = 2;
    public static final int CMR_GC_ALWAYS_MARKED   = 3;
    
    public static final int POSIX                  = 0;
    public static final int RTEMS                  = 1;
    public static final int WIN32                  = 2;
    
    public static final int OM_CONTIGUOUS          = 0;
    public static final int OM_FRAGMENTED          = 1;
    
    public static final int HM_NARROW              = 0;
    public static final int HM_POISONED            = 1;
    
    public static final int GC_OBJ_SPACE           = 0;
    
    // barrier flags
    public static final int BR_CM_STORE            = 65536;
    public static final int BR_SCOPE_CHECK         = 131072;
    public static final int BR_ARRAY_BOUNDS        = 262144;
    
    // names of synthetic methods
    public static final String SMN_CLONE_HELPER    = "fiji_cloneHelper";
    
    // alloc effort flags
    public static final int AE_CAN_FAIL            = 0;
    public static final int AE_MUST_SUCCEED        = 1;
    
    // scopeID flags, must match fivmr.h
    public static final int SCOPEID_STACK          = 0;
    public static final int SCOPEID_SCOPE          = 1;
    public static final int SCOPEID_MASK           = 1;
    
    public static final int FAT_PUTFIELD           = 0;
    public static final int FAT_GETFIELD           = 1;
    public static final int FAT_PUTSTATIC          = 2;
    public static final int FAT_GETSTATIC          = 3;
    
    public static final int MCT_INVOKESTATIC       = 0;
    public static final int MCT_INVOKEVIRTUAL      = 1;
    public static final int MCT_INVOKEINTERFACE    = 2;
    public static final int MCT_INVOKESPECIAL      = 3;
    
    public static final int IOT_INSTANCEOF         = 0;
    public static final int IOT_CHECKCAST          = 1;
}
