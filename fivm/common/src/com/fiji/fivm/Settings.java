/*
 * Settings.java
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
 * Set of boolean fields that determine the static configuration of the Fiji VM.
 * In the compiler these fields are set reflectively at compiler start-up based
 * on a configuration file created by fivmc.  For the runtime C code, the compiler
 * generates a header file that sets the respective macros.  For the runtime Java
 * code, these fields are made intrinsic by the compiler.
 */
public class Settings {
    private Settings() {}
    
    // NOTE: these fields are all intrinsic at runtime.
    
    // FIXME: when self-hosting, we need to put this into its own classloader.
    // but when JITing ... we don't have to (or want to) do that.
    
    // FIXME: may way to reconsider putting compiler-specific stuff in here.
    // because if we put it here then we cannot dynamically change it if we're
    // JITing.  and it means that settings put here will persist from AOT to
    // JIT.
    
    // FIXME: some of these settings are system-dependent and so we don't need
    // to allow different VMs in the same address space to have different values
    // for those settings.  We should have an annotation for those...
    
    @SysDepSetting
    public static boolean PTRSIZE_32;
    
    @SysDepSetting
    public static boolean PTRSIZE_64;
    
    @SysDepSetting
    public static boolean ASSERTS_ON;
    
    @SysDepSetting
    public static boolean STACK_GROWS_DOWN;

    @SysDepSetting
    public static boolean PROFILE_MONITOR;

    @SysDepSetting
    public static boolean PROFILE_MONITOR_HEAVY;

    @SysDepSetting
    public static boolean PROFILE_GC;

    @SysDepSetting
    public static boolean PROFILE_GC_HEAVY;

    public static boolean PROFILE_STACK_HEIGHT;

    @SysDepSetting
    public static boolean PROFILE_REFLECTION;
    
    @SysDepSetting
    public static boolean SPC_ENABLED;

    @SysDepSetting
    public static boolean COVERAGE;

    @SysDepSetting
    public static boolean POSIX;

    @SysDepSetting
    public static boolean RTEMS;

    @SysDepSetting
    public static boolean POSIX_IO; // true on both POSIX and RTEMS, currently

    @SysDepSetting
    public static boolean WIN32;
    
    @SysDepSetting
    public static boolean X86;

    @SysDepSetting
    public static boolean CAN_DO_CLASSLOADING;
    
    public static boolean CLASSLOADING;
    
    public static boolean HM_NARROW;

    public static boolean HM_POISONED;

    public static boolean OM_CONTIGUOUS;

    public static boolean OM_FRAGMENTED;

    public static boolean FORCE_ARRAYLETS;

    @SysDepSetting
    public static boolean STATIC_JNI;

    @SysDepSetting
    public static boolean DYN_LOADING;

    public static boolean MAIN_IN_THREAD;

    public static boolean VERBOSE_RUN_METHOD;

    public static boolean DUMB_HASH_CODE;

    @SysDepSetting
    public static boolean UNIPROCESSOR;

    public static boolean BIASED_LOCKING;

    public static boolean PIP_LOCKING;

    public static boolean NOGC;

    public static boolean CMRGC;

    public static boolean HFGC;

    public static boolean SELF_MANAGE_MEM;

    public static boolean PREDICTABLE_OOME;

    public static boolean GC_BLACK_STACK;

    public static boolean GLIBJ;

    public static boolean FIJICORE;

    public static boolean INCLUDE_PROFILER;

    @SysDepSetting
    public static boolean SUPPORT_SIGQUIT;

    public static boolean GC_DEBUG;

    @SysDepSetting
    public static boolean INTERNAL_INST;

    public static boolean HFGC_PREDICT_NO_ARRAYLETS;

    public static boolean HFGC_PREDICT_ARRAYLETS;

    public static boolean HFGC_TRY_CONTIGUOUS_ARRAYS;

    public static boolean HFGC_ALL_ARRAYLETS;

    public static boolean HFGC_FAIL_FAST_PATHS;

    public static boolean INTERCEPT_ALL_OBJ_ACCESSES;

    public static boolean SCOPED_MEMORY;
    
    public static boolean USE_TYPE_EPOCHS;
    
    public static boolean ITABLE_COMPRESSION;
    
    public static boolean TRACK_DIRECT_SUBS;
    
    public static boolean CLOSED_PATCH_POINTS;
    public static boolean OPEN_PATCH_POINTS;
    
    public static boolean OPEN_WORLD;
    
    public static boolean FULL_STACK_ANALYSIS;
    public static boolean OPEN_STACK_ANALYSIS;
    public static boolean SCJ_STACK_ANALYSIS;
    public static boolean TRUSTED_STACK_ANALYSIS;
    
    public static boolean SIMPLE_COMP;

    public static boolean EXCEPTIONS_MAY_BE_NULL;

    @SysDepSetting
    public static boolean PARALLEL_C1;

    @SysDepSetting
    public static boolean LOG_CODE_REPO_ACCESSES;

    @SysDepSetting
    public static boolean IS_LITTLE_ENDIAN;

    @SysDepSetting
    public static boolean IS_BIG_ENDIAN;

    public static boolean DEF_LOG_GC;

    public static boolean DEF_LOG_SYNC_GC;

    @SysDepSetting
    public static boolean FLOW_LOGGING;

    public static boolean FLOW_LOGGING_NO_SMALL_INLINES;

    @SysDepSetting
    public static boolean FLOW_LOGGING_FATEVENTS;

    public static boolean SCJ;
    
    @SysDepSetting
    public static boolean SCJ_L0;    // switch on SCJ Level 0 optimizations
    
    public static boolean RTSJ;

    public static boolean SCJ_SCOPES;
    public static boolean RTSJ_SCOPES;
    
    public static boolean HAVE_SCOPE_CHECKS;
    public static boolean HAVE_SHARED_SCOPES;
    
    public static boolean SEARCH_ROOT_CLASSES_AT_RUNTIME;
    public static boolean ROOT_OVERRIDES_ALL_APP;
    
    public static boolean FULL_CHECK_INIT_SEMANTICS;
    
    public static boolean FINALIZATION_SUPPORTED;
    
    public static boolean OPT_CM_STORE_BARRIERS;
    public static boolean CHECK_OPT_CM_STORE_BARRIERS;
    public static boolean AGGRESSIVE_OPT_CM_STORE_BARRIERS;
    
    public static boolean FILTERED_CM_STORE_BARRIERS;
    
    public static boolean SUPPORT_ENV_BASED_SYS_PROPS;
    
    public static boolean AOT_NATIVE_BACKEND;
    public static boolean OMIT_FRAME_POINTER;
    
    @SysDepSetting
    public static boolean LOG_GC_MARK_TRAPS;
}


