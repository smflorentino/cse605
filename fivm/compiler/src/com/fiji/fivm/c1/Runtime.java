/*
 * Runtime.java
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

package com.fiji.fivm.c1;

// this class is weird.  don't dare access it until Context.root is
// initialized.
public class Runtime {
    private Runtime() {}
    
    public static final VisibleClass arithmeticException=
	Global.root().forceClass("java/lang/ArithmeticException");
    public static final VisibleClass nullPointerException=
	Global.root().forceClass("java/lang/NullPointerException");
    public static final VisibleClass stackOverflowError=
	Global.root().forceClass("java/lang/StackOverflowError");
    public static final VisibleClass arrayIndexOutOfBoundsException=
	Global.root().forceClass("java/lang/ArrayIndexOutOfBoundsException");
    public static final VisibleClass arrayStoreException=
	Global.root().forceClass("java/lang/ArrayStoreException");
    public static final VisibleClass negativeArraySizeException=
	Global.root().forceClass("java/lang/NegativeArraySizeException");
    public static final VisibleClass classCastException=
	Global.root().forceClass("java/lang/ClassCastException");
    public static final VisibleClass incompatibleClassChangeError=
	Global.root().forceClass("java/lang/IncompatibleClassChangeError");
    public static final VisibleClass noClassDefFoundError=
	Global.root().forceClass("java/lang/NoClassDefFoundError");
    public static final VisibleClass illegalAssignmentError=
	Global.root().forceClass("javax/realtime/IllegalAssignmentError");
    
    public static final VisibleClass fcMagic=
	Global.root().forceClass("__vm/FCMagic");
    
    public static final VisibleClass system=
	Global.root().forceClass("java/lang/System");
    public static final VisibleClass fcSystem=
	Global.root().forceClass("java/lang/FCSystem");

    public static final VisibleClass fivmSupport=
	Global.root().forceClass("java/lang/fivmSupport");
    public static final VisibleClass fivmRuntime=
	Global.root().forceClass("com/fiji/fivm/r1/fivmRuntime");
    public static final VisibleClass settings=
	Global.root().forceClass("com/fiji/fivm/Settings");
    public static final VisibleClass mm=
	Global.root().forceClass("com/fiji/fivm/r1/MM");
    public static final VisibleClass monitors=
	Global.root().forceClass("com/fiji/fivm/r1/Monitors");
    public static final VisibleClass fivmOptions=
	Global.root().forceClass("com/fiji/fivm/r1/fivmOptions");
    public static final VisibleClass magic=
	Global.root().forceClass("com/fiji/fivm/r1/Magic");
    public static final VisibleClass cVar=
	Global.root().forceClass("com/fiji/fivm/r1/CVar");
    public static final VisibleClass cType=
	Global.root().forceClass("com/fiji/fivm/r1/CType");
    public static final VisibleClass cMacro=
	Global.root().forceClass("com/fiji/fivm/r1/CMacro");
    public static final VisibleClass arrayHelper=
	Global.root().forceClass("com/fiji/fivm/r1/ArrayHelper");
    public static final VisibleClass noThrow=
	Global.root().forceClass("com/fiji/fivm/r1/NoThrow");
    public static final VisibleClass noReturn=
	Global.root().forceClass("com/fiji/fivm/r1/NoReturn");
    public static final VisibleClass noNullCheckOnAccess=
	Global.root().forceClass("com/fiji/fivm/r1/NoNullCheckOnAccess");
    public static final VisibleClass noSafetyChecks=
	Global.root().forceClass("com/fiji/fivm/r1/NoSafetyChecks");
    public static final VisibleClass intrinsic=
	Global.root().forceClass("com/fiji/fivm/r1/Intrinsic");
    public static final VisibleClass impord=
	Global.root().forceClass("com/fiji/fivm/r1/Import");
    public static final VisibleClass runtimeImport=
	Global.root().forceClass("com/fiji/fivm/r1/RuntimeImport");
    public static final VisibleClass godGiven=
	Global.root().forceClass("com/fiji/fivm/r1/GodGiven");
    public static final VisibleClass trustedGodGiven=
	Global.root().forceClass("com/fiji/fivm/r1/TrustedGodGiven");
    public static final VisibleClass noExecStatusTransition=
	Global.root().forceClass("com/fiji/fivm/r1/NoExecStatusTransition");
    public static final VisibleClass useObjectsNotHandles=
	Global.root().forceClass("com/fiji/fivm/r1/UseObjectsNotHandles");
    public static final VisibleClass noNativeFrame=
	Global.root().forceClass("com/fiji/fivm/r1/NoNativeFrame");
    public static final VisibleClass reflect=
	Global.root().forceClass("com/fiji/fivm/r1/Reflect");
    public static final VisibleClass condReflect=
	Global.root().forceClass("com/fiji/fivm/r1/CondReflect");
    public static final VisibleClass noPollcheck=
	Global.root().forceClass("com/fiji/fivm/r1/NoPollcheck");
    public static final VisibleClass allowUnsafe=
	Global.root().forceClass("com/fiji/fivm/r1/AllowUnsafe");
    public static final VisibleClass noSafepoint=
	Global.root().forceClass("com/fiji/fivm/r1/NoSafepoint");
    public static final VisibleClass pure=
	Global.root().forceClass("com/fiji/fivm/r1/Pure");
    public static final VisibleClass noInline=
	Global.root().forceClass("com/fiji/fivm/r1/NoInline");
    public static final VisibleClass inline=
	Global.root().forceClass("com/fiji/fivm/r1/Inline");
    public static final VisibleClass notAReference=
	Global.root().forceClass("com/fiji/fivm/r1/NotAReference");
    public static final VisibleClass untraced=
	Global.root().forceClass("com/fiji/fivm/r1/Untraced");
    public static final VisibleClass stackAllocation=
	Global.root().forceClass("com/fiji/fivm/r1/StackAllocation");
    public static final VisibleClass allocateAsCaller=
	Global.root().forceClass("com/fiji/fivm/r1/AllocateAsCaller");
    public static final VisibleClass ifPoundDefined=
	Global.root().forceClass("com/fiji/fivm/r1/IfPoundDefined");
    public static final VisibleClass unsupUnlessSet=
	Global.root().forceClass("com/fiji/fivm/r1/UnsupUnlessSet");
    public static final VisibleClass nonZero=
	Global.root().forceClass("com/fiji/fivm/r1/NonZero");
    public static final VisibleClass unboxed=
	Global.root().forceClass("com/fiji/fivm/r1/Unboxed");
    public static final VisibleClass noScopeChecks=
	Global.root().forceClass("com/fiji/fivm/r1/NoScopeChecks");
    public static final VisibleClass noStackOverflowCheck=
	Global.root().forceClass("com/fiji/fivm/r1/NoStackOverflowCheck");
    public static final VisibleClass runtimeExceptionThrower=
        Global.root().forceClass("com/fiji/fivm/r1/RuntimeExceptionThrower");
    public static final VisibleClass usesMagic=
        Global.root().forceClass("com/fiji/fivm/r1/UsesMagic");
    public static final VisibleClass noFlowLog=
	Global.root().forceClass("com/fiji/fivm/r1/NoFlowLog");
    
    public static final VisibleClass gate=
        Global.root().forceClass("com/fiji/mvm/Gate");
    public static final VisibleClass gateHelpers=
        Global.root().forceClass("com/fiji/mvm/GateHelpers");

    public static final VisibleMethod yield=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "yield",
				 Type.EMPTY));
    
    public static final VisibleMethod throwCloneNotSupported=
        Global.root().resolve(Global.root().objectClass,
                             fivmRuntime,
                             new MethodSignature(
                                 Type.VOID,
                                 "throwCloneNotSupported",
                                 Type.EMPTY));
    
    public static final VisibleMethod throwArithmeticRTE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwArithmeticRTE",
				 Type.EMPTY));
    
    public static final VisibleMethod throwNullPointerRTE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwNullPointerRTE",
				 Type.EMPTY));
    
    public static final VisibleMethod throwStackOverflowRTE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwStackOverflowRTE",
				 Type.EMPTY));
    
    public static final VisibleMethod throwArrayBoundsRTE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwArrayBoundsRTE",
				 Type.EMPTY));
    
    public static final VisibleMethod throwArrayStoreRTE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwArrayStoreRTE",
				 Type.EMPTY));
    
    public static final VisibleMethod throwNegativeSizeRTE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwNegativeSizeRTE",
				 Type.EMPTY));
    
    public static final VisibleMethod throwClassCastRTE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwClassCastRTE",
				 Type.EMPTY));
    
    public static final VisibleMethod throwClassChangeRTE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwClassChangeRTE",
				 Type.EMPTY));
    
    public static final VisibleMethod throwNoClassDefFoundError=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwNoClassDefFoundError",
				 new Type[]{
                                     Global.root().stringType,
                                     Global.root().stringType
                                 }));
    
    public static final VisibleMethod throwIllegalAssignmentError=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwIllegalAssignmentError",
				 Type.EMPTY));
    
    public static final VisibleMethod throwUOE=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "throwUnsupportedOperationException",
				 Type.EMPTY));
    
    public static final VisibleMethod resolveNative=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.POINTER,
				 "resolveNative",
				 new Type[]{
				     Type.POINTER
				 }));
    
    public static final VisibleMethod checkInit=
	Global.root().resolve(Global.root().objectClass,
			     fivmRuntime,
			     new MethodSignature(
				 Type.VOID,
				 "checkInit",
				 new Type[]{
				     Type.POINTER
				 }));
    
    public static final VisibleMethod eatTypeCheck=
        Global.root().resolve(Global.root().objectClass,
                             magic,
                             new MethodSignature(
                                 Global.root().objectType,
                                 "eatTypeCheck",
                                 new Type[]{
                                     Global.root().objectType
                                 }));
    
    public static final VisibleMethod arraycopy=
	Global.root().resolve(Global.root().objectClass,
			     system,
			     new MethodSignature(
				 Type.VOID,
				 "arraycopy",
				 new Type[]{
				     Global.root().objectType,
				     Type.INT,
				     Global.root().objectType,
				     Type.INT,
				     Type.INT
				 }));
    
    public static final VisibleMethod fcArraycopy=
	Global.root().resolve(Global.root().objectClass,
			     fcSystem,
			     new MethodSignature(
				 Type.VOID,
				 "arraycopy",
				 new Type[]{
				     Global.root().objectType,
				     Type.INT,
				     Global.root().objectType,
				     Type.INT,
				     Type.INT
				 }));
    
    public static final VisibleMethod fastUnsafeArrayCopy=
	Global.root().resolve(Global.root().objectClass,
			     arrayHelper,
			     new MethodSignature(
				 Type.VOID,
				 "fastUnsafeArrayCopy",
				 new Type[]{
				     Global.root().objectType,
				     Type.INT,
				     Global.root().objectType,
				     Type.INT,
				     Type.INT,
				     Type.POINTER
				 }));
    
    public static final VisibleMethod alloc=
        Global.root().resolve(Global.root().objectClass,
                             mm,
                             new MethodSignature(
                                 Global.root().objectType,
                                 "alloc",
                                 new Type[]{
                                     Type.INT,
                                     Type.POINTER,
                                     Type.POINTER,
                                     Type.POINTER,
                                     Type.POINTER
                                 }));
    public static final VisibleMethod allocArray=
        Global.root().resolve(Global.root().objectClass,
                             mm,
                             new MethodSignature(
                                 Global.root().objectType,
                                 "allocArray",
                                 new Type[]{
                                     Type.INT,
                                     Type.POINTER,
                                     Type.POINTER,
                                     Type.INT,
                                     Type.POINTER
                                 }));
    public static final VisibleMethod allocSimple=
        Global.root().resolve(Global.root().objectClass,
                             mm,
                             new MethodSignature(
                                 Global.root().objectType,
                                 "alloc",
                                 new Type[]{
                                     Type.INT,
                                     Type.POINTER
                                 }));
    public static final VisibleMethod allocArraySimple=
        Global.root().resolve(Global.root().objectClass,
                             mm,
                             new MethodSignature(
                                 Global.root().objectType,
                                 "allocArray",
                                 new Type[]{
                                     Type.INT,
                                     Type.POINTER,
                                     Type.INT
                                 }));
    public static final VisibleMethod addDestructor=
        Global.root().resolve(Global.root().objectClass,
                              mm,
                              new MethodSignature(
                                  Global.root().objectType,
                                  "addDestructor",
                                  new Type[]{
                                      Type.INT,
                                      Global.root().objectType
                                  }));
    public static final VisibleMethod storeBarrier=
        Global.root().resolve(Global.root().objectClass,
                             mm,
                             new MethodSignature(
                                 Type.VOID,
                                 "storeBarrier",
                                 new Type[]{
                                     Global.root().objectType,
                                     Type.POINTER,
                                     Global.root().objectType,
                                     Type.INT
                                 }));
    public static final VisibleMethod assertMarked=
        Global.root().resolve(Global.root().objectClass,
                              mm,
                              new MethodSignature(
                                  Type.VOID,
                                  "assertMarked",
                                  new Type[]{
                                      Global.root().objectType
                                  }));
    public static final VisibleMethod lock=
        Global.root().resolve(Global.root().objectClass,
                              monitors,
                              new MethodSignature(
                                  Type.VOID,
                                  "lock",
                                  new Type[]{
                                      Global.root().objectType,
                                  }));
    public static final VisibleMethod unlock=
        Global.root().resolve(Global.root().objectClass,
                              monitors,
                              new MethodSignature(
                                  Type.VOID,
                                  "unlock",
                                  new Type[]{
                                      Global.root().objectType,
                                  }));
    
    public static final VisibleMethod finalize=
        Global.root().resolve(Global.root().objectClass,
                              Global.root().objectClass,
                              new MethodSignature(
                                  Type.VOID,
                                  "finalize",
                                  Type.EMPTY));

    public static final VisibleClass weakReference=
        Global.root().forceClass("java/lang/ref/WeakReference");
    
    public static final VisibleClass constructor;
    public static final VisibleClass method;
    public static final VisibleClass field;
    public static final VisibleClass vmSystem;

    public static final VisibleMethod vmArraycopy;
    
    static {
	if (Global.lib==Library.GLIBJ) {
	    constructor=
		Global.root().forceClass("java/lang/reflect/Constructor");
	    method=
		Global.root().forceClass("java/lang/reflect/Method");
	    field=
		Global.root().forceClass("java/lang/reflect/Field");
	    vmSystem=
		Global.root().forceClass("java/lang/VMSystem");

	    vmArraycopy=
		Global.root().resolve(Global.root().objectClass,
				     vmSystem,
				     new MethodSignature(
					 Type.VOID,
					 "arraycopy",
					 new Type[]{
					     Global.root().objectType,
					     Type.INT,
					     Global.root().objectType,
					     Type.INT,
					     Type.INT
					 }));
	} else {
	    // make javac happy
	    constructor=null;
	    method=null;
	    field=null;
	    vmSystem=null;
	    vmArraycopy=null;
	}
    }
}

