/*
 * VisibleMethod.java
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

import java.util.ArrayList;
import java.util.HashSet;

import com.fiji.fivm.Constants;
import com.fiji.fivm.JNIUtils;
import com.fiji.fivm.Settings;
import com.fiji.fivm.ReturnMode;

public class VisibleMethod
    extends VisibleMember
    implements MethodSignaturable, Callable {
    
    static int[] nextMethodID = { -1 };

    MethodKind kind;
    MethodSync sync;
    MethodImpl impl;
    
    int size;
    int numVars;
    boolean hasJsr;
    
    int maxLocals=-1;
    int maxStack=-1;

    /** Local method ID for Flow Logging */
    int methodID=-1;
    
    // FIXME: seriously consider using an int with flags instead of all
    // of these booleans.
    
    ReturnMode returnMode=ReturnMode.RETURN_OR_THROW;
    
    /** True if the method could have any meaningful side-effects. */
    boolean meaningful;
    
    /** Should this method be exported to C?  If this is set to true, a
	C function of the same name and signature (including the receiver if
	it's an instance method) will be created that calls this method
	virtually (again, if it's an instance method). */
    boolean export;
    
    /** Should we omit the exec status transition? */
    boolean noExecStatusTransition;
    
    /** Should we abstain from using handles on native calls to this method? */
    boolean useObjectsNotHandles;
    
    /** Don't create a native frame when calling this native method. */
    boolean noNativeFrame;
    
    /** When importing, should we import using GodGivenFunction instead of
	RemoteFunction, or should we use TrustedGodGivenFunction? */
    ImportMode importMode=ImportMode.REMOTE_IMPORT;
    
    /** Should scope checks be suppressed within this function? */
    boolean noScopeChecks;
    
    /** is this responsible for throwing runtime exceptions? */
    boolean runtimeExceptionThrower;

    /** Does the method allow unsafe things to happen within it? */
    UnsafeMode unsafe = UnsafeMode.DISALLOW_UNSAFE;

    /** Should flow log events be suppressed for this method? */
    boolean noFlowLog;
    
    InlineMode inlineMode = InlineMode.AUTO_INLINE;
    
    SafepointMode safepoint = SafepointMode.MAY_SAFEPOINT;
    SideEffectMode sideEffect = SideEffectMode.CLOBBERS_WORLD;
    PollcheckMode pollcheck = PollcheckMode.IMPLICIT_POLLCHECKS;
    SafetyCheckMode safetyChecks = SafetyCheckMode.IMPLICIT_SAFETY_CHECKS;
    
    AllocationMechanism alloc = AllocationMechanism.DEFAULT_ALLOC;
    
    StackOverflowCheckMode stackOverflowCheckMode = StackOverflowCheckMode.CHECK_STACK_OVERFLOW;
    
    /** Known <i>implementation</i> overrides of this method.  Abstract
	overrides are not included.
	NOTE: do not use this unless the analysis is closed. */
    HashSet< VisibleMethod > knownOverrides=new HashSet< VisibleMethod >();
    
    /** Methods that this is known to override.  Includes this method. */
    HashSet< VisibleMethod > knownOverriddens=new HashSet< VisibleMethod >();
    
    private VisibleMethod () {}

    VisibleMethod(VisibleClass clazz,
		  int index,
		  Binding binding,
		  Visibility visibility,
		  MethodKind kind,
		  MethodSync sync,
		  MethodImpl impl,
		  MethodSignature signature) {
	super(clazz,index,binding,visibility,signature);
	this.kind=kind;
	this.sync=sync;
	this.impl=impl;
	assert (kind==MethodKind.ABSTRACT)==(impl==MethodImpl.STUB);
	knownOverriddens.add(this);
    }
    
    public MethodSignature getSignature() { return (MethodSignature)signature; }

    public Type[] getParams() { return getSignature().getParams(); }
    public Type getParam(int i) { return getParams()[i]; }
    
    Type[] allParams;
    public synchronized Type[] getAllParams() {
	if (allParams==null) {
	    allParams=getParams();
	    if (!isStatic()) {
		Type[] newParams=new Type[allParams.length+1];
		newParams[0]=Type.make(getClazz());
		System.arraycopy(allParams,0,
				 newParams,1,
				 allParams.length);
		allParams=newParams;
	    }
	}
	return allParams;
    }
    
    public int numParams() {
	return getSignature().getParams().length;
    }
    
    public int numAllParams() {
	if (isStatic()) {
	    return numParams();
	} else {
	    return numParams()+1;
	}
    }
    
    public MethodKind getKind() { return kind; }
    public MethodSync getSync() { return sync; }
    public MethodImpl getImpl() { return impl; }
    
    public boolean isFinal() { return kind==MethodKind.FINAL; }
    public boolean isVirtual() { return kind==MethodKind.VIRTUAL; }
    public boolean isAbstract() { return kind==MethodKind.ABSTRACT; }

    public boolean isStub() { return impl==MethodImpl.STUB; }
    public boolean isBytecode() { return impl==MethodImpl.BYTECODE; }
    public boolean isJNI() { return impl==MethodImpl.JNI; }
    public boolean isImport() { return impl==MethodImpl.IMPORT; }
    public boolean isIntrinsic() { return impl==MethodImpl.INTRINSIC; }
    public boolean isSynthetic() { return impl==MethodImpl.SYNTHETIC; }
    public boolean isUnsupported() { return impl==MethodImpl.UNSUPPORTED; }
    public boolean isNative() {
	return impl==MethodImpl.JNI
	    || impl==MethodImpl.IMPORT
	    || impl==MethodImpl.INTRINSIC;
    }
    
    public boolean isInterfaceMethod() {
	return getClazz().isInterface();
    }
    
    public boolean shouldHaveBytecode() {
	return isBytecode() && isExecuted() && !isAbstract();
    }
    
    public int runtimeFlags() {
	return super.runtimeFlags()|getKind().runtimeFlags()|getSync().runtimeFlags()|
	    getImpl().runtimeFlags()|(shouldHaveCode()?Constants.MBF_HAS_CODE:0)|
	    (alloc==AllocationMechanism.ALLOC_AS_CALLER?Constants.MBF_ALLOC_AS_CALLER:0)|
            (shouldExist()?Constants.MBF_EXISTS:0)|
            (runtimeExceptionThrower?Constants.MBF_RT_EXC_THROWER:0)|Constants.MBF_COOKIE;
    }
    
    public boolean shouldHaveNativeGlue() {
	return (isJNI() || isImport()) && isExecuted() && !isAbstract();
    }
    
    public boolean shouldHaveUnsupStub() {
	return isUnsupported() && isExecuted() && !isAbstract();
    }
    
    public boolean shouldHaveSyntheticImpl() {
        return isSynthetic() && isExecuted() && !isAbstract();
    }
    
    public boolean shouldHaveCode() {
	return shouldHaveBytecode()
	    || shouldHaveNativeGlue()
	    || shouldHaveUnsupStub()
            || shouldHaveSyntheticImpl();
    }
    
    public boolean isExecuted() {
	return Global.analysis().isExecuted(this);
    }
    
    public boolean shouldExist() {
	return Global.analysis().isCalled(this);
    }
    
    public boolean shouldExport() {
	return export;
    }
    
    public UnsafeMode unsafe() {
	return unsafe;
    }
    
    public boolean allowUnsafe() {
	return unsafe==UnsafeMode.ALLOW_UNSAFE;
    }
    
    public InlineMode inlineMode() {
	return inlineMode;
    }
    
    public boolean noInline() {
	return inlineMode==InlineMode.NO_INLINE;
    }

    public boolean noFlowLog() {
	return noFlowLog;
    }

    public boolean shouldFlowLog() {
	return !noFlowLog && !getContext().noFlowLog();
    }
    
    public void setSideEffect(SideEffectMode m) {
	sideEffect=m;
    }
    
    public void setSafepoint(SafepointMode m) {
	safepoint=m;
    }
    
    public void setPollcheck(PollcheckMode m) {
	pollcheck=m;
    }

    public PollcheckMode pollcheck() { return pollcheck; }
    public SideEffectMode sideEffect() { return sideEffect; }
    public SafepointMode safepoint() { return safepoint; }
    
    public boolean noSafepoint() {
	return safepoint()==SafepointMode.CANNOT_SAFEPOINT;
    }
    
    public boolean noPollcheck() {
	return pollcheck==PollcheckMode.EXPLICIT_POLLCHECKS_ONLY;
    }
    
    public SafetyCheckMode safetyChecks() { return safetyChecks; }
    
    public boolean noSafetyChecks() {
	return safetyChecks==SafetyCheckMode.EXPLICIT_SAFETY_CHECKS_ONLY;
    }
    
    public StackOverflowCheckMode stackOverflowCheckMode() {
        return stackOverflowCheckMode;
    }
    
    public boolean hasStackOverflowCheck() {
        return stackOverflowCheckMode==StackOverflowCheckMode.CHECK_STACK_OVERFLOW;
    }
    
    public boolean noStackOverflowCheck() {
        return stackOverflowCheckMode==StackOverflowCheckMode.DONT_CHECK_STACK_OVERFLOW;
    }
    
    /** Return an estimate of code size. */
    public int codeSize() {
	if (shouldHaveBytecode()) {
	    return size;
	} else if (shouldHaveNativeGlue()) {
	    int result=0;
	    Type[] params=getAllParams();
	    for (int i=0;i<params.length;++i) {
		if (params[i].isObject() && !useObjectsNotHandles) {
		    result+=8;
		} else {
		    result+=2;
		}
	    }
	    if (getType().isObject() && !useObjectsNotHandles) {
		result+=8;
	    } else {
		result+=2;
	    }
	    if (!noNativeFrame) {
		result+=20;
	    }
	    if (!noExecStatusTransition) {
		result+=10;
	    }
	    if (isJNI()) {
		result+=20;
		if (isStatic()) {
		    result+=5;
		}
	    }
	    return result;
	} else {
	    return 0;
	}
    }
    
    /** Stupidly named method that tells you if the method was effectively
	declared final, by either being directly final, or being a member
	of a final class. */
    public boolean isDeclaredFinal() {
	return isFinal() || getClazz().isFinal() || !hasObjectReceiver();
    }
    
    public boolean isEffectivelyFinal() {
	return isDeclaredFinal() || isInitializer()
	    || (Global.analysis().closed() && knownOverrides.isEmpty());
    }
    
    public boolean runtimeWillCallStatically() {
	return isInitializer() || isDeclaredFinal();
    }
    
    public boolean isSynchronized() { return sync==MethodSync.SYNCHRONIZED; }
    
    public boolean isInitializer() { return getSignature().isInitializer(); }
    
    public boolean saveReceiver() {
        return isSynchronized() && isInstance();
    }
    
    public int nSavedReceivers() {
        return saveReceiver()?1:0;
    }
    
    public ReturnMode returnMode() { return returnMode; }
    public boolean canReturn() { return returnMode.canReturn(); }
    public boolean canThrow() { return returnMode.canThrow(); }
    public boolean doesNotReturn() { return !returnMode.canReturn(); }
    public boolean doesNotThrow() { return !returnMode.canThrow(); }
    
    public AllocationMechanism alloc() { return alloc; }
    
    public void makeMeaningful() {
	if (!meaningful) {
	    meaningful=true;
	    if (isStatic() && isInitializer()) {
		clazz.hasMeaningfulClinit=true;
	    }
	}
    }
    
    public boolean isMeaningful() { return meaningful; }
    
    public String toString() {
        if (this==ANONYMOUS) {
            return "AnonymousMethod";
        } else {
            String result="Method["+clazz+" "+binding+" "+visibility+" "+kind+" "+sync+" "+impl+" "+signature;
            if (!isExecuted()) {
                result+=" (never executed)";
            }
            if (Global.analysis().closed()) {
                result+=" (exec params = "+Util.dump(Global.analysis().paramSetForExec(this))+")";
                result+=" (exec result = "+Global.analysis().returnSetForExec(this)+")";
                result+=" (call params = "+Util.dump(Global.analysis().paramSetForCall(this))+")";
                result+=" (call result = "+Global.analysis().returnSetForCall(this)+")";
            }
            result+=" "+safepoint;
            result+=" "+sideEffect;
            result+=" "+pollcheck;
            result+=" "+safetyChecks;
            result+=" "+alloc;
            result+=" "+stackOverflowCheckMode;
            return result+"]";
        }
    }
    
    public String canonicalName() {
        if (this==ANONYMOUS) {
            return "AnonymousMethod";
        } else {
            return "Method["+clazz.canonicalName()+" "+binding+" "+visibility+" "+kind+" "+sync+" "+impl+" "+signature.canonicalName()+"]";
        }
    }
    
    private String cachedJniName=null;
    public synchronized String jniName() {
        if (this==ANONYMOUS) {
            return "<anonymous>";
        } else {
            if (cachedJniName==null) {
                try {
                    cachedJniName=getClazz().asType().jniName()+"/"+getName()+getSignature().jniSignature();
                } catch (Exception e) {
                    throw new CompilerException(
                        "Could not create JNI name for "+this,e);
                }
            }
            return cachedJniName;
        }
    }
    
    public String shortName() {
        return getContext().description()+":"+jniName();
    }
    
    public String jniFunctionNameShort() {
	StringBuilder funcNameBuf=new StringBuilder();
	funcNameBuf.append("Java_");
	funcNameBuf.append(JNIUtils.jniEscape(getClazz().getName()));
	funcNameBuf.append("_");
	funcNameBuf.append(JNIUtils.jniEscape(getName()));
	return funcNameBuf.toString();
    }
    
    public String jniFunctionNameLong() {
	StringBuilder funcNameBuf=new StringBuilder();
	funcNameBuf.append(jniFunctionNameShort());
	if (numParams()!=0) {
	    funcNameBuf.append("__");
	    for (Type t : getParams()) {
		funcNameBuf.append(JNIUtils.jniEscape(t.jniName()));
	    }
	}
	return funcNameBuf.toString();
    }
    
    MethodTypeSignature msig;
    public synchronized MethodTypeSignature getTypeSignature() {
        if (msig==null) {
            msig=new MethodTypeSignature(this);
        }
        return msig;
    }
    
    public NativeSignature getNativeSignature() {
        return getTypeSignature().getNativeSignature();
    }
    
    // NB: what would happen if you called this on an abstract class that
    // doesn't have instances (hasInstances()==false)?  that could happen
    // for initializers.
    // huh?
    RemoteFunction remoteFunction;
    public synchronized RemoteFunction asRemoteFunction() {
	if (remoteFunction==null) {
	    assert shouldHaveCode()==true : this;
	    remoteFunction=
		new RemoteFunction(
		    mangledName(),
		    getNativeSignature());
	}
	return remoteFunction;
    }
    
    public ArrayList< VisibleMethod > prune(VisibleClass c,TypeBoundMode mode) {
        if (mode==TypeBoundMode.UPPER_BOUND) {
            assert Global.analysis().closed() : "Attempting to prune "+this+" to "+c+" with mode=="+mode+" while the analysis is "+Global.analysis();
        }
	if (isEffectivelyFinal()) {
	    // what is the purpose of this block?  can't the else case just
	    // take care of it?  or is this an optimization of sorts?
	    if (isAbstract()) {
		return EMPTY_AL;
	    } else {
		return Util.makeArray(this);
	    }
	} else {
	    ArrayList< VisibleMethod > result=c.dispatch(mode).getOverridingMethods(this);
	    assert mode==TypeBoundMode.UPPER_BOUND || result.size()<=1 : "method = "+this+", class = "+c+", result = "+result+", mode = "+mode;
	    return result;
	}
    }
    
    public ArrayList< VisibleMethod > prune(ClassBound b) {
	return prune(b.clazz(),b.mode());
    }
    
    /**
     * Returns the method that would be called, if the receiver were the
     * given class.
     */
    public VisibleMethod pruneExactly(VisibleClass c) {
	try {
	    if (isEffectivelyFinal()) {
		if (isAbstract()) {
		    return null;
		} else {
		    return this;
		}
	    } else {
		return c.exactDispatch().getOverridingMethod(getSignature());
	    }
	} catch (Throwable e) {
	    throw new CompilerException("Could not prune exactly for "+this+" on "+c,e);
	}
    }
    
    public ArrayList< VisibleMethod > possibleTargets() {
        assert Global.analysis().closed();
	return prune(getClazz(),TypeBoundMode.UPPER_BOUND);
    }
    
    public int getItableIndex() {
        if (Settings.ITABLE_COMPRESSION) {
            return Global.imethodSigIndex.get(getSignature());
        } else {
            return Global.imethodIndex.get(this);
        }
    }
    
    public boolean hasVtableIndex() {
        return getClazz().hasVtableIndex(getSignature());
    }
    
    public int getVtableIndex() {
	return getClazz().getVtableIndex(getSignature());
    }
    
    public int getTypeDataIndex() {
        int result=0;
        for (VisibleMethod m : getClazz().methods()) {
            if (m.shouldExist()) {
                if (this==m) {
                    return result;
                }
                result++;
            }
        }
        assert false : this;
        return -1;
    }

    public int methodID() {
	if (methodID == -1) {
	    /* Maybe racy */
	    synchronized (nextMethodID) {
		if (methodID == -1) {
		    methodID = nextMethodID[0]++;
		}
	    }
	}
	return methodID;
    }
    
    public void dump() {
	System.out.println("    Method: "+this);
	System.out.println("      Size: "+size);
	System.out.println("      Num Vars: "+numVars);
	System.out.println("      Has JSR: "+hasJsr);
    }
    
    public synchronized void clearNameCaches() {
        cachedJniName=null;
        msig=null;
        remoteFunction=null;
    }
    
    public static abstract class Visitor {
	public abstract void visit(VisibleMethod vm);
    }
    
    public static VisibleMethod[] EMPTY=new VisibleMethod[0];
    public static ArrayList< VisibleMethod > EMPTY_AL=new ArrayList< VisibleMethod >();

    public static VisibleMethod ANONYMOUS = new VisibleMethod();
}

