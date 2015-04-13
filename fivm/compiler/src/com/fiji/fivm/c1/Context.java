/*
 * Context.java
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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import com.fiji.fivm.Settings;
import com.fiji.fivm.ReturnMode;

import com.fiji.util.MyStack;

// NOTE NOTE NOTE: when we use this to compile stuff at run-time, we will have one
// context per TypeContext, but *we will not* duplicate the ClassLoader "hierarchy".
// most likely TypeContext won't duplicate it, either.  Each context will have Root
// as its parent, where Root is the precompiled context containing the class library
// and such.  Unless we're JITing something for Root, in which case we won't be
// creating another context for that.  Moreover: we need to rethink the notion of
// Context being Contextable and possibly instead have user-supplied callbacks for:
// 1) resolving classes that we cannot find in tryGetClass
// 2) creating new classes
// in the case of AOT, (1) will go to a parent context else fail, and (2) will create an
// unresolved class.  in the case of JIT, (1) will fail and (2) will
// perform a lookup via TypeContext.

public class Context {
    LinkedHashMap< String, VisibleClass > classes=
	new LinkedHashMap< String, VisibleClass >();
    ArrayList< VisibleClass > resolvedClasses;
    
    ArrayList< Type > runtimeUsedResolvedTypesSorted; // only when the typesystem is closed
    ArrayList< Type > runtimeUsedUnresolvedTypesSorted;
    
    ResolutionReport resolutionReport=new ResolutionReport();
    
    public VisibleClass objectClass;
    public VisibleClass stringClass;
    public VisibleClass classClass;
    public VisibleClass throwableClass;
    public VisibleClass serializableClass;
    public VisibleClass cloneableClass;
    public VisibleClass pointerClass;
    public VisibleClass fcPtrClass;
    
    public VisibleClass booleanArrClass;
    public VisibleClass byteArrClass;
    public VisibleClass charArrClass;
    public VisibleClass shortArrClass;
    public VisibleClass intArrClass;
    public VisibleClass longArrClass;
    public VisibleClass pointerArrClass;
    public VisibleClass fcPtrArrClass;
    public VisibleClass floatArrClass;
    public VisibleClass doubleArrClass;
    public VisibleClass objectArrClass;
    
    public VisibleClass excludeUnlessSetClass;
    public VisibleClass exportClass;
    
    public Type objectType;
    public Type stringType;
    public Type classType;
    public Type throwableType;
    public Type serializableType;
    public Type cloneableType;
    
    boolean allResolved;
    
    String description;
    int index;
    
    UpResolver upResolver;
    ClassCreator classCreator;

    /* If this is set, and flow logging is enabled, various flow log
     * events will not be emitted by this context. */
    boolean noFlowLog;
    
    public Context(String description,
                   UpResolver upResolver,
                   ClassCreator classCreator) {
        this.description=description;
        this.upResolver=upResolver;
        this.classCreator=classCreator;
        
	objectClass=forceClass("java/lang/Object");
	stringClass=forceClass("java/lang/String");
	classClass=forceClass("java/lang/Class");
	throwableClass=forceClass("java/lang/Throwable");
	serializableClass=forceClass("java/io/Serializable");
	cloneableClass=forceClass("java/lang/Cloneable");
	pointerClass=forceClass("com/fiji/fivm/r1/Pointer");
	fcPtrClass=forceClass("__vm/FCPtr");
        
        booleanArrClass=forceClass("com/fiji/fivm/r1/arrays/BooleanArr");
        byteArrClass=forceClass("com/fiji/fivm/r1/arrays/ByteArr");
        charArrClass=forceClass("com/fiji/fivm/r1/arrays/CharArr");
        shortArrClass=forceClass("com/fiji/fivm/r1/arrays/ShortArr");
        intArrClass=forceClass("com/fiji/fivm/r1/arrays/IntArr");
        longArrClass=forceClass("com/fiji/fivm/r1/arrays/LongArr");
        pointerArrClass=forceClass("com/fiji/fivm/r1/arrays/PointerArr");
        fcPtrArrClass=forceClass("com/fiji/fivm/r1/arrays/FCPtrArr");
        floatArrClass=forceClass("com/fiji/fivm/r1/arrays/FloatArr");
        doubleArrClass=forceClass("com/fiji/fivm/r1/arrays/DoubleArr");
        objectArrClass=forceClass("com/fiji/fivm/r1/arrays/ObjectArr");
        
        excludeUnlessSetClass=forceClass("com/fiji/fivm/r1/ExcludeUnlessSet");
        exportClass=forceClass("com/fiji/fivm/r1/Export");
        
        pointerClass.unboxed=true;
        fcPtrClass.unboxed=true;
        booleanArrClass.unboxed=true;
        byteArrClass.unboxed=true;
        charArrClass.unboxed=true;
        shortArrClass.unboxed=true;
        intArrClass.unboxed=true;
        longArrClass.unboxed=true;
        pointerArrClass.unboxed=true;
        fcPtrArrClass.unboxed=true;
        floatArrClass.unboxed=true;
        doubleArrClass.unboxed=true;
        objectArrClass.unboxed=true;
        
	objectType=Type.make(objectClass);
	stringType=Type.make(stringClass);
	classType=Type.make(classClass);
	throwableType=Type.make(throwableClass);
	serializableType=Type.make(serializableClass);
	cloneableType=Type.make(cloneableClass);
    }
    
    public String description() { return description; }
    public int runtimeIndex() { return index; }
    
    public Collection< VisibleClass > resolvedClasses() { return resolvedClasses; }
    
    public void forAllMethods(VisibleMethod.Visitor v) {
        for (VisibleClass c : resolvedClasses()) {
            for (VisibleMethod m : c.methods()) {
                if (Global.analysis().isCalled(m)) {
                    v.visit(m);
                }
            }
        }
    }
    
    public ArrayList< Type > allResolvedTypesUsedAtRuntime() {
        return runtimeUsedResolvedTypesSorted;
    }
    
    public ArrayList< Type > allUnresolvedTypesUsedAtRuntime() {
        return runtimeUsedUnresolvedTypesSorted;
    }
    
    public ArrayList< Type > allTypesUsedAtRuntimeFor(ResolvedState state) {
        switch (state) {
        case RESOLVED: return allResolvedTypesUsedAtRuntime();
        case UNRESOLVED: return allUnresolvedTypesUsedAtRuntime();
        default: throw new Error("unrecognized: "+state);
        }
    }
    
    public ArrayList< Type > allTypesUsedAtRuntime() {
        ArrayList< Type > result=new ArrayList< Type >();
        result.addAll(runtimeUsedResolvedTypesSorted);
        result.addAll(runtimeUsedUnresolvedTypesSorted);
        return result;
    }

    /**
     * Looks up the given class using the current context.  First looks at the
     * current context; if a class by that name exists it is returned.  If one
     * does not exist in the current context, the parent context is recursively
     * searched.  This proceeds up the context parenting chain until it hits
     * the root context.  If none of the contexts along the chain contain the
     * class, null is returned.
     */
    public VisibleClass tryGetClass(String name) {
        // FIXME: have a pre-resolver, to allow for first looking into some other
        // context.
	VisibleClass c=classes.get(name);
        if (c==null) {
            return upResolver.resolveUp(this,name);
        } else {
            return c;
        }
    }
    
    /**
     * Looks up the given class using the current context as in tryGetClass(),
     * but with one major difference: if tryGetClass() would have returned
     * null, this method instead attempts to construct an class and marks it
     * as unresolved.  If this is impossible because the type system is closed,
     * an exception is thrown instead.
     */
    public VisibleClass forceClass(String name) {
	VisibleClass result=tryGetClass(name);
	if (result==null) {
            // ok ... what exactly do we do if we have a parent context?
	    if (Type.closed()) {
		throw new ResolutionFailed(
                    null,
                    new ResolutionID(ClassResolutionMode.UNRESOLVED,description(),name),
                    "Could neither find nor add class "+name);
	    }
	    result=classCreator.createClass(this,name);
	    classes.put(name,result);
	}
	return result;
    }
    
    /**
     * Looks up the given class using the current context only; if it does not
     * exist then a new one is created.  Does not recursively look up classes
     * in parent contexts.
     */
    public VisibleClass createClass(String name) {
        if (Type.closed()) {
            throw new CompilerException(
                "Could not create class "+name+" because the type system is closed");
        }
        VisibleClass result=classes.get(name);
        if (result==null) {
            result=new VisibleClass(this,name);
            classes.put(name,result);
        }
        return result;
    }
    
    /**
     * Looks up the given class using the current context as in tryGetClass(),
     * but with one major difference: if tryGetClass() would have returned
     * null, this method instead throws an exception.
     */
    public VisibleClass getClass(String name) {
	VisibleClass result=tryGetClass(name);
	if (result==null) {
	    throw new ResolutionFailed(
                null,
                new ResolutionID(ClassResolutionMode.UNRESOLVED,description(),name),
                "Could not find class "+name);
	}
	if (!result.hasBytecode()) {
	    throw new ResolutionFailed(
		result,
                result.getResolutionID(),
                "Class "+result.jniName()+" was not supplied to the compiler");
	}
	return result;
    }
    
    /**
     * Looks up the given class using the current context but does not
     * perform class creation and does not search in parent contexts.
     */
    public VisibleClass getClassInThisContext(String name) {
        VisibleClass c=classes.get(name);
        if (c==null) {
            throw new ResolutionFailed(
                null,
                new ResolutionID(ClassResolutionMode.UNRESOLVED,description(),name),
                "Could not find class "+name+" in "+description());
        }
        return c;
    }
    
    public VisibleMethod resolveMethod(String desc) {
	try {
            int nextIdx=Type.skip(desc);
	    Type t=Type.parse(this,desc);
            t.checkResolved();
	    VisibleClass c;
	    if (!t.hasEffectiveClass()) {
		throw new BadBytecode("Bad method descriptor: "+desc);
	    }
	    c=t.effectiveClass();
	    String rest=desc.substring(nextIdx);
	    if (!rest.startsWith("/")) {
		throw new BadBytecode("Bad method descriptor: "+desc);
	    }
	    MethodSignature s=MethodSignature.parse(this,rest.substring(1));
	    return resolve(objectClass,c,s);
	} catch (ResolutionFailed e) {
	    throw e;
	} catch (Throwable e) {
	    throw new BadBytecode("Could not resolve method: "+desc,e);
	}
    }
    
    public VisibleField resolveField(String desc) {
	try {
	    int nextIdx=Type.skip(desc);
	    Type t=Type.parse(this,desc);
	    t.checkResolved();
	    VisibleClass c;
	    if (!t.hasEffectiveClass()) {
		throw new BadBytecode("Bad method descriptor: "+desc);
	    }
	    c=t.effectiveClass();
	    String rest=desc.substring(nextIdx);
	    if (!rest.startsWith("/")) {
		throw new BadBytecode("Bad field descriptor: "+desc);
	    }
	    FieldSignature s=FieldSignature.parse(this,rest.substring(1));
	    return resolve(objectClass,c,s);
	} catch (ResolutionFailed e) {
	    throw e;
	} catch (Throwable e) {
	    throw new BadBytecode("Could not resolve field: "+desc,e);
	}
    }
    
    public void addClass(String name,byte[] bytecode) {
	createClass(name).setBytecode(bytecode);
    }
    
    public ClassFileIterator classFileIterator() {
	return new ClassFileIterator(){
	    public void addClass(String name,byte[] bytecode) {
		Context.this.addClass(name,bytecode);
	    }
	};
    }
    
    // assert that everything is resolved, and do any computation necessary to
    // allow field and method resolutions to work.  this will probably include
    // making some mirandas.
    public void resolveAll() {
        for (VisibleClass c : new ArrayList< VisibleClass >(classes.values())) {
            c.populateFromBytecode(); // this may add classes due to forceClass()
        }
        
        for (VisibleClass c : classes.values()) {
            for (VisibleClass c2 : c.directSupertypes()) {
                if (c2.unresolved() && c.resolved()) {
                    resolutionReport.addUse(c2.getResolutionID(),c.getResolutionID());
                }
            }
        }
        
        fixResolution();

	for (VisibleClass c : resolvedClasses()) {
	    for (VisibleClass c2 : c.directSupertypes()) {
		c.knownSupers.add(c2);
	    }
	}
        
	boolean changed=true;
	while (changed) {
	    changed=false;
	    for (VisibleClass c : resolvedClasses()) {
		HashSet< VisibleClass > toAdd=new HashSet< VisibleClass >();
		for (VisibleClass c2 : c.knownSupers) {
		    for (VisibleClass c3 : c2.knownSupers) {
			if (!c.knownSupers.contains(c3)) {
			    toAdd.add(c3);
			}
		    }
		}
		changed|=c.knownSupers.addAll(toAdd);
	    }
	}
        
        for (VisibleClass c : resolvedClasses()) {
            c.addSyntheticMethods();
            c.addSyntheticFields();
            c.modifySpecialFields();
        }
        
	allResolved=true;
    }
    
    public void fixResolution() {
	boolean changed=true;
	while (changed) {
	    changed=false;
	    for (VisibleClass c : classes.values()) {
		for (VisibleClass c2 : c.directSupertypes()) {
		    if (c2.unresolved() && c.resolved()) {
			if (Global.verbosity>=2) {
			    Global.log.println("Canceling resolution of "+c+" because of "+c2);
			}
			c.cancelResolution();
			changed=true;
		    }
		}
	    }
	}
	resolvedClasses=new ArrayList< VisibleClass >();
	for (VisibleClass c : classes.values()) {
	    if (c.resolved()) {
		resolvedClasses.add(c);
	    }
	}
    }
    
    public void clearNameCaches() {
        for (VisibleClass c : classes.values()) {
            c.clearNameCaches();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void setAttributes() {
	long before=System.currentTimeMillis();
	for (VisibleClass c : resolvedClasses()) {
            c.usesMagic|=c.hasAnnotation(Runtime.usesMagic);
	    for (VisibleMember m : c.members()) {
		m.noNullCheckOnAccess|=m.hasAnnotation(Runtime.noNullCheckOnAccess);
		m.nonZero|=m.hasAnnotation(Runtime.nonZero);
	    }
	    for (VisibleField f : c.fields()) {
		if (f.hasAnnotation(Runtime.notAReference)) {
                    assert !f.hasAnnotation(Runtime.untraced);
		    f.traceMode=TraceMode.NOT_A_REFERENCE;
		}
		if (f.hasAnnotation(Runtime.untraced)) {
                    assert !f.hasAnnotation(Runtime.notAReference);
		    f.traceMode=TraceMode.UNTRACED;
		}
	    }
	    for (VisibleMethod m : c.methods()) {
		if (m.hasAnnotation(Runtime.noSafetyChecks)) {
		    m.safetyChecks=SafetyCheckMode.EXPLICIT_SAFETY_CHECKS_ONLY;
		}
                if (m.hasAnnotation(Runtime.noStackOverflowCheck)) {
                    m.stackOverflowCheckMode=StackOverflowCheckMode.DONT_CHECK_STACK_OVERFLOW;
                }
		if (m.hasAnnotation(Runtime.pure)) {
		    m.sideEffect=SideEffectMode.PURE;
		}
		if (m.hasAnnotation(Runtime.noPollcheck)) {
		    m.pollcheck=PollcheckMode.EXPLICIT_POLLCHECKS_ONLY;
		}
		if (m.hasAnnotation(Runtime.noSafepoint)) {
		    m.safepoint=SafepointMode.CANNOT_SAFEPOINT;
		}
		if (m.hasAnnotation(Runtime.allowUnsafe)) {
		    m.unsafe=UnsafeMode.ALLOW_UNSAFE;
		}
		if (m.hasAnnotation(Runtime.noThrow) ||
		    m.hasAnnotation(Runtime.runtimeImport)) {
		    if (m.hasAnnotation(Runtime.noReturn)) {
			m.returnMode=ReturnMode.NON_TERMINATING;
		    } else {
			m.returnMode=ReturnMode.ONLY_RETURN;
		    }
		} else if (m.hasAnnotation(Runtime.noReturn)) {
		    m.returnMode=ReturnMode.ONLY_THROW;
		}
		if (m.hasAnnotation(Runtime.intrinsic)) {
		    assert m.isDeclaredFinal() || m.isStatic() || m.isInitializer() : m;
		    m.impl=MethodImpl.INTRINSIC;
		}
		if (m.hasAnnotation(Runtime.impord) ||
		    m.hasAnnotation(Runtime.runtimeImport)) {
		    assert m.isDeclaredFinal() || m.isStatic() || m.isInitializer() : m;
		    assert m.impl!=MethodImpl.INTRINSIC : m;
		    m.impl=MethodImpl.IMPORT;
		}
		if (m.hasAnnotation(exportClass)) {
		    assert !m.isImport();
		    m.export=true;
		}
		if (m.hasAnnotation(Runtime.godGiven) ||
		    m.hasAnnotation(Runtime.runtimeImport)) {
		    assert m.isImport();
		    m.importMode=ImportMode.GOD_GIVEN_IMPORT;
		}
                if (m.hasAnnotation(Runtime.trustedGodGiven)) {
                    assert m.isImport();
                    m.importMode=ImportMode.TRUSTED_GOD_GIVEN_IMPORT;
                }
		if (m.hasAnnotation(Runtime.noExecStatusTransition) ||
		    m.hasAnnotation(Runtime.runtimeImport)) {
		    assert m.export || m.isJNI() || m.isImport();
		    m.noExecStatusTransition=true;
		}
		if (m.hasAnnotation(Runtime.useObjectsNotHandles) ||
		    m.hasAnnotation(Runtime.runtimeImport)) {
		    assert m.export || m.isJNI() || m.isImport();
		    m.useObjectsNotHandles=true;
		}
		if (m.hasAnnotation(Runtime.noNativeFrame) ||
		    m.hasAnnotation(Runtime.runtimeImport)) {
		    assert m.isJNI() || m.isImport();
		    m.noNativeFrame=true;
		}
		if (m.hasAnnotation(Runtime.noInline) ||
		    m.getClazz().isSubclassOf(throwableClass) ||
		    m.doesNotReturn()) {
		    assert !m.hasAnnotation(Runtime.inline) : "Method "+m+" is both @NoInline and @Inline";
		    m.inlineMode=InlineMode.NO_INLINE;
		}
		if (m.hasAnnotation(Runtime.inline)) {
		    assert !m.hasAnnotation(Runtime.noInline);
		    m.inlineMode=InlineMode.MUST_INLINE;
		}
		if (m.hasAnnotation(Runtime.stackAllocation)) {
		    assert !m.hasAnnotation(Runtime.allocateAsCaller);
                    assert Global.ola.stackAllowedIn(this) : "Method "+m+" is declared @StackAllocation but is in a context that does not allow for stack allocation.";
		    m.alloc=AllocationMechanism.STACK_ALLOC;
		} else if (m.hasAnnotation(Runtime.allocateAsCaller)) {
		    assert !m.hasAnnotation(Runtime.stackAllocation) : m;
		    assert !m.export : m;
                    assert Global.ola.stackAllowedIn(this) : "Method "+m+" is declared @AllocateAsCaller but is in a context that does not allow for stack allocation.";
		    m.alloc=AllocationMechanism.ALLOC_AS_CALLER;
		}
		if (m.hasAnnotation(Runtime.noScopeChecks)) {
		    assert !m.hasAnnotation(Runtime.stackAllocation);
		    m.noScopeChecks=true;
		}
		if (m.hasAnnotation(Runtime.noFlowLog) ||
		    c.hasAnnotation(Runtime.noFlowLog)) {
		    m.noFlowLog=true;
		}
		if (Global.pdr!=null) {
		    VisibleAnnotation va=m.getAnnotation(Runtime.ifPoundDefined);
		    if (va!=null) {
			for (String macro : (List< String >)va.get("value")) {
			    if (!Global.pdr.defined(macro)) {
				m.impl=MethodImpl.UNSUPPORTED;
			    }
			}
		    }
		}
                VisibleAnnotation va=m.getAnnotation(Runtime.unsupUnlessSet);
                if (va!=null) {
                    for (String setting : (List< String >)va.get("value")) {
                        try {
                            if (!Settings.class.getField(setting).getBoolean(null)) {
                                m.impl=MethodImpl.UNSUPPORTED;
                            }
                        } catch (NoSuchFieldException e) {
                            throw new Error(e);
                        } catch (IllegalAccessException e) {
                            throw new Error(e);
                        }
                    }
                }
                if (m.hasAnnotation(Runtime.runtimeExceptionThrower)) {
                    m.runtimeExceptionThrower=true;
                }
                c.usesMagic|=m.hasAnnotation(Runtime.usesMagic);
	    }
	}
	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) {
	    Global.log.println("setting attributes took "+(after-before)+" ms");
	}
    }
    
    public void hierarchyVerification() {
	if (!Global.runSanityCheck) return;
	int failures=0;
	for (VisibleClass c : resolvedClasses()) {
	    OverrideMap om=new OverrideMap(c.allInstanceMethods());
	    for (VisibleMethod m : c.methods()) {
		if (m.noSafepoint() &&
		    m.isBytecode() &&
		    !m.allowUnsafe() &&
		    !m.noPollcheck()) {
		    Global.log.println("Error: "+m+" is @NoSafepoint but is neither of the following: native, intrinsic, import, marked @AllowUnsafe, or marked @NoPollcheck.");
		    failures++;
		}
                if (!m.isInitializer()) {
                    for (VisibleMethod m2 : om.getStrictOverriddenMethods(m)) {
                        if ((m.alloc==AllocationMechanism.ALLOC_AS_CALLER) !=
                            (m2.alloc==AllocationMechanism.ALLOC_AS_CALLER)) {
                            Global.log.println("Error: "+m+" overrides "+m2+" but one has @AllocAsCaller while the other doesn't.");
                            failures++;
                        }
                        if (!m.noPollcheck() && m2.noPollcheck()) {
                            Global.log.println("Error: "+m+" has pollchecks but overrides "+m2+", which doesn't.");
                            failures++;
                        }
                        if (!m.noSafepoint() && m2.noSafepoint()) {
                            Global.log.println("Error: "+m+" has safepoints but overrides "+m2+", which doesn't.");
                            failures++;
                        }
                        if (m.canReturn() && m2.doesNotReturn()) {
                            Global.log.println("Error: "+m+" can return but overrides "+m2+", which can't.");
                            failures++;
                        }
                        if (m.canThrow() && m2.doesNotThrow()) {
                            Global.log.println("Error: "+m+" can throw but overrides "+m2+", which can't.");
                            failures++;
                        }
                    }
		}
	    }
	}
	if (failures>0) {
	    Global.log.println("Saw "+failures+" failures in class hierarchy verification.");
	    throw new CompilerException("Class hierarchy verification failed.");
	}
    }
    
    // note that in the resolution methods that follow, we have a 'from' parameter
    // that is currently unused.  this may change, if we find that we need it for
    // reasons of visibility.
    
    // another note: early on, I tested resolution caching.  and found that it has
    // no effect whatsoever on performance.  let this be a lesson for future
    // generations.
    
    private VisibleField tryResolveInterface(VisibleClass target,
					     FieldSignature s) {
	MyStack< VisibleClass > worklist=new MyStack< VisibleClass >();
	worklist.push(target);
	while (!worklist.empty()) {
	    VisibleClass c=worklist.pop();
	    VisibleField f=c.getField(s);
	    if (f!=null) {
		return f;
	    }
	    for (VisibleClass c2 : c.getSuperInterfaces()) {
		worklist.push(c2);
	    }
	}
	return null;
    }
    
    private boolean wasResolutionCanceledInterface(VisibleClass target,
                                                   FieldSignature s) {
	MyStack< VisibleClass > worklist=new MyStack< VisibleClass >();
	worklist.push(target);
	while (!worklist.empty()) {
	    VisibleClass c=worklist.pop();
            if (c.wasResolutionCanceled(s)) {
                return true;
            }
	    for (VisibleClass c2 : c.getSuperInterfaces()) {
		worklist.push(c2);
	    }
	}
	return false;
    }
    
    public VisibleField tryResolve(VisibleClass from,
				   VisibleClass target,
				   FieldSignature s) {
	if (target.isInterface()) {
	    VisibleField result=tryResolveInterface(target,s);
	    if (result==null) {
		return tryResolve(from,Global.root().objectClass,s);
	    } else {
		return result;
	    }
	} else {
	    for (VisibleClass cur=target;cur!=null;cur=cur.getSuperclass()) {
		VisibleField f=cur.getField(s);
		if (f!=null) {
		    return f;
		}
		for (VisibleClass i : cur.getSuperInterfaces()) {
		    VisibleField result=tryResolveInterface(i,s);
		    if (result!=null) return result;
		}
	    }
	}
	return null;
    }
    
    public boolean wasResolutionCanceled(VisibleClass from,
                                         VisibleClass target,
                                         FieldSignature s) {
	if (target.isInterface()) {
            if (wasResolutionCanceledInterface(target,s)) {
                return true;
            } else {
                return wasResolutionCanceled(from,Global.root().objectClass,s);
            }
	} else {
	    for (VisibleClass cur=target;cur!=null;cur=cur.getSuperclass()) {
                if (cur.wasResolutionCanceled(s)) {
                    return true;
                }
		for (VisibleClass i : cur.getSuperInterfaces()) {
		    if (wasResolutionCanceledInterface(i,s)) {
                        return true;
                    }
		}
	    }
	}
	return false;
    }
    
    private VisibleField tryResolveInterfaceFieldByName(VisibleClass target,
							String fieldName) {
	MyStack< VisibleClass > worklist=new MyStack< VisibleClass >();
	worklist.push(target);
	while (!worklist.empty()) {
	    VisibleClass c=worklist.pop();
	    VisibleField f=c.getFieldByName(fieldName);
	    if (f!=null) {
		return f;
	    }
	    for (VisibleClass c2 : c.getSuperInterfaces()) {
		worklist.push(c2);
	    }
	}
	return null;
    }
    
    public VisibleField tryResolveFieldByName(VisibleClass from,
					      VisibleClass target,
					      String fieldName) {
	if (target.isInterface()) {
	    VisibleField result=tryResolveInterfaceFieldByName(target,fieldName);
	    if (result==null) {
		return tryResolveFieldByName(from,Global.root().objectClass,fieldName);
	    } else {
		return result;
	    }
	} else {
	    for (VisibleClass cur=target;cur!=null;cur=cur.getSuperclass()) {
		VisibleField f=cur.getFieldByName(fieldName);
		if (f!=null) {
		    return f;
		}
		for (VisibleClass i : cur.getSuperInterfaces()) {
		    VisibleField result=tryResolveInterfaceFieldByName(i,fieldName);
		    if (result!=null) {
			return result;
		    }
		}
	    }
	    return null;
	}
    }
    
    private VisibleMethod tryResolveInterface(VisibleClass target,
					      MethodSignature s) {
	MyStack< VisibleClass > worklist=new MyStack< VisibleClass >();
	worklist.push(target);
	while (!worklist.empty()) {
	    VisibleClass c=worklist.pop();
	    VisibleMethod m=c.getMethod(s);
	    if (m!=null) {
		return m;
	    }
	    for (VisibleClass c2 : c.getSuperInterfaces()) {
		worklist.push(c2);
	    }
	}
	return null;
    }
    
    private boolean wasResolutionCanceledInterface(VisibleClass target,
                                                   MethodSignature s) {
	MyStack< VisibleClass > worklist=new MyStack< VisibleClass >();
	worklist.push(target);
	while (!worklist.empty()) {
	    VisibleClass c=worklist.pop();
            if (c.wasResolutionCanceled(s)) {
                return true;
            }
	    for (VisibleClass c2 : c.getSuperInterfaces()) {
		worklist.push(c2);
	    }
	}
	return false;
    }
    
    // NB: note that this does *not* take into account covariant return.
    // that's because covariant return only comes into play in overriding,
    // not in resolving.
    public VisibleMethod tryResolve(VisibleClass from,
				    VisibleClass target,
				    MethodSignature s) {
	if (target.isInterface()) {
	    VisibleMethod result=tryResolveInterface(target,s);
	    if (result==null) {
		return tryResolve(from,Global.root().objectClass,s);
	    } else {
		return result;
	    }
	} else {
	    for (VisibleClass cur=target;cur!=null;cur=cur.getSuperclass()) {
		VisibleMethod m=cur.getMethod(s);
		if (m!=null) {
		    return m;
		}
	    }
	    for (VisibleClass cur=target;cur!=null;cur=cur.getSuperclass()) {
		for (VisibleClass i : cur.getSuperInterfaces()) {
		    VisibleMethod result=tryResolveInterface(i,s);
		    if (result!=null) return result;
		}
	    }
	}
	return null;
    }
    
    public boolean wasResolutionCanceled(VisibleClass from,
                                         VisibleClass target,
                                         MethodSignature s) {
        if (target.isInterface()) {
            if (wasResolutionCanceledInterface(target,s)) {
                return true;
            } else {
                return wasResolutionCanceled(from,Global.root().objectClass,s);
            }
        } else {
            for (VisibleClass cur=target;cur!=null;cur=cur.getSuperclass()) {
                if (cur.wasResolutionCanceled(s)) {
                    return true;
                }
            }
            for (VisibleClass cur=target;cur!=null;cur=cur.getSuperclass()) {
                for (VisibleClass i : cur.getSuperInterfaces()) {
                    if (wasResolutionCanceledInterface(i,s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public VisibleField resolve(VisibleClass from,
				VisibleClass target,
				FieldSignature s) {
	VisibleField result=tryResolve(from,target,s);
	if (result==null) {
	    throw new ResolutionFailed(
                target,
                s.getResolutionID(target,wasResolutionCanceled(from,target,s)),
                "Failed to resolve "+s.jniName()+" at "+target.jniName()+
                " in a reference from "+from.jniName());
	}
	return result;
    }
    
    public VisibleField resolveFieldByName(VisibleClass from,
					   VisibleClass target,
					   String fieldName) {
	VisibleField result=tryResolveFieldByName(from,target,fieldName);
	if (result==null) {
	    throw new ResolutionFailed(
                target,
                new ResolutionID(target,"field "+fieldName),
                "Failed to resolve "+fieldName+" at "+
                target.jniName()+" in reference from "+from.jniName());
	}
	return result;
    }
    
    public VisibleMethod resolve(VisibleClass from,
				 VisibleClass target,
				 MethodSignature s) {
	VisibleMethod result=tryResolve(from,target,s);
	if (result==null) {
	    throw new ResolutionFailed(
                target,
                s.getResolutionID(target,wasResolutionCanceled(from,target,s)),
                "Failed to resolve "+s.jniName()+" at "+target.jniName()+
                " in a reference from "+from.jniName());
	}
	return result;
    }
    
    public VisibleMethod reresolveSuper(VisibleClass from,
					VisibleMethod method) {
	assert from.isSubclassOf(method.getClazz());
	VisibleMethod result=tryResolve(from,
					from.getSuperclass(),
					method.getSignature());
	if (result==null) {
	    throw new ResolutionFailed(
                from,
                method.getSignature().getResolutionID(from.getSuperclass()),
                "Failed to reresolve "+method.jniName()+" from "+
                from.jniName()+" for an invokespecial");
	}
	return result;
    }
    
    public VisibleMethod resolveSpecial(VisibleClass from,
					VisibleClass target,
					MethodSignature s) {
	VisibleMethod result=resolve(from,target,s);
	if (from.getSuperMode()==SuperMode.NEW_SUPER_MODE &&
	    from.isStrictSubclassOf(result.getClazz()) &&
	    !result.isInitializer()) {
	    return reresolveSuper(from,result);
	} else {
	    return result;
	}
    }
    
    public void dump() {
	System.out.println("Context dump:");
	System.out.println("objectClass = "+objectClass);
	System.out.println("stringClass = "+stringClass);
	System.out.println("throwableClass = "+throwableClass);
	System.out.println("Classes:");
	for (VisibleClass c : classes.values()) {
	    c.dump();
	}
    }
    
    public String toString() {
        return description+"["+classes.size()+"]";
    }
    
    public String canonicalName() {
        return description;
    }

    public boolean noFlowLog() {
	return noFlowLog;
    }

    public void setNoFlowLog(boolean b) {
	noFlowLog = b;
    }
}

