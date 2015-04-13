/*
 * VisibleClass.java
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;

import com.fiji.asm.AnnotationVisitor;
import com.fiji.asm.ClassReader;
import com.fiji.asm.ClassWriter;
import com.fiji.asm.ClassAdapter;
import com.fiji.asm.FieldVisitor;
import com.fiji.asm.Label;
import com.fiji.asm.MethodVisitor;
import com.fiji.asm.Opcodes;
import com.fiji.asm.UTF8Sequence;
import com.fiji.asm.commons.EmptyVisitor;
import com.fiji.asm.commons.JSRInlinerAdapter;

import com.fiji.util.MyStack;

import com.fiji.fivm.Constants;
import com.fiji.fivm.Settings;
import com.fiji.fivm.om.FieldLayerOuter;
import com.fiji.fivm.om.OMClass;
import com.fiji.fivm.om.OMField;

import com.fiji.config.*;

public class VisibleClass extends Annotatable implements JNINameable, OMClass {
    
    Context context;
    
    byte[] bytecode;
    boolean resolutionCanceled=false;
    String sourceFilename;
    
    Visibility visibility;
    ClassKind kind;
    SuperMode smode;
    String name;
    String packaje;
    
    boolean unboxed;
    
    boolean hasJsr;
    
    boolean usesMagic;
    
    LinkedHashMap< FieldSignature, VisibleField > fields=
	new LinkedHashMap< FieldSignature, VisibleField >();
    LinkedHashMap< MethodSignature, VisibleMethod > methods=
	new LinkedHashMap< MethodSignature, VisibleMethod >();
    
    HashSet< MemberSignature > resolutionsCanceled=
        new HashSet< MemberSignature >();
    
    private VisibleClass extendz;
    ArrayList< VisibleClass > implementz=
	new ArrayList< VisibleClass >();

    HashSet< VisibleClass > knownSupers=
	new HashSet< VisibleClass >();
    
    HashSet< VisibleClass > knownDirectSubs=
	new HashSet< VisibleClass >();
    /** The known strict subclasses of this class.  NB: DO NOT access this unless (a) you're prepared for the fact that
     * the results will be inaccurate and completely utterly unsound and cannot ever under any circumstance be relied upon
     * in any way, or (b) you guard the check with Analysis.closed(); if that returns true then this set is sound. */
    HashSet< VisibleClass > knownSubs=
	new HashSet< VisibleClass >();
    
    /** The vtable for this class.  For each index, tells you which actual
	method will be invoked.  Note that it may be null if we don't have
	an implementation (i.e. this is a miranda or our implementation is
	abstract).  Note that abstract classes have vtables. */
    VisibleMethod[] vtable;
    
    /** Signature table. */
    MethodSignature[] vtableSigs;
    
    public VisibleMethod[] itable;
    public int minITableIndex;
    public int maxITableIndex;
    
    OverrideMap resolution;
    
    HashSet< VisibleMethod > allInstanceMethods;

    /** The set of methods that may get called dynamically if an exact
	instance of this class were to be the receiver. */
    HashSet< VisibleMethod > dynableMethods;
    
    /** An OverrideMap for computing which method would be called for a given
        method or method signature if an exact instance of this class were to
        be the receiver. */
    OverrideMap exactDispatch;
    
    /** An OverrideMap for computing which methods would be called for a given
        method or method signature if any subclass of this class were to be
        the receiver. */
    OverrideMap dispatch;
    
    /** Maps methods to their vtable indices.  Note that this maps *all*
	of the methods from this class and its superinterfaces to vtable
	indices, even ones that are being overridden.  Furthermore, this
	table includes only those mappings that are not already included
	in the superclass's mapping.
	<p>
	You can use this mapping to do a variety of things:
	<ul>
	<li>Virtualizing interface calls.  If class type C flows into an
            InvokeDynamic on an interface method, simply look up that
	    interface method in this table (or the tables of this class's
            superclasses) to find the vtable index; then just find the
	    VisibleMethod at that index in the vtable.</li>
	<li>Implementing virtual method calls.  Simply look up the method
	    in this table to get the vtable offset.</li>
	</ul> */
    HashMap< MethodSignature, Integer > vtableIndices;
    
    // only applies when ITABLE_COMPRESSION is false
    HashMap< MethodSignature, Integer > itableIndices;
    
    /** The fields of this class (NOT of super classes).  Will be
	present for abstract classes as well as concrete ones. */
    private OMField[] laidOutFields;
    
    /** The payload size of this type.  Does *not* include the header. */
    private int payloadSize;
    
    /** The required alignment of the payload of this type. */
    private int requiredPayloadAlignment;
    
    static int canonicalNumberCnt=0;
    int canonicalNumber;
    
    ClassBound exactBound;
    ClassBound bound;
    
    boolean hasMeaningfulClinit=false;
    boolean hasShouldCallCheckInit=false;
    boolean shouldCallCheckInit;
    
    Type myType;
    Exectype myExectype;
    
    public int extraFlags;
    
    VisibleClass(Context c,String name) {
	this.context=c;
	this.name=name.intern();
	int lastSlash=name.lastIndexOf('/');
	if (lastSlash>=0) {
	    packaje=name.substring(0,lastSlash).intern();
	} else {
	    packaje="";
	}
	synchronized (VisibleClass.class) {
	    canonicalNumber=canonicalNumberCnt++; // FIXME
	}
    }
    
    public static int maxCanonicalNumber() {
        return canonicalNumberCnt-1;
    }
    
    public boolean hasBytecode() { return bytecode!=null; }
    public boolean resolved() { return hasBytecode() && !resolutionCanceled; }
    public boolean unresolved() { return !resolved(); }
    
    public void cancelResolution() {
        resolutionCanceled=true;
    }
    
    public ClassResolutionMode resolutionMode() {
        if (hasBytecode()) {
            if (resolutionCanceled) {
                return ClassResolutionMode.RESOLUTION_CANCELED;
            } else {
                return ClassResolutionMode.RESOLVED;
            }
        } else {
            return ClassResolutionMode.UNRESOLVED;
        }
    }
    
    public void checkResolved() {
	if (unresolved()) {
	    throw new ResolutionFailed(
                this,
                getResolutionID(),
                "Attempting to use an unresolved class "+this.jniName());
	}
    }
    
    public Context getContext() { return context; }

    public String getName() { return name; }
    
    public String jniName() { return name; }
    
    public String shortName() {
        return getContext().description()+":"+jniName();
    }
    
    public String getPackage() { return packaje; }
    public Visibility getVisibility() {
	checkResolved();
	return visibility;
    }
    public ClassKind getKind() {
	checkResolved();
	return kind;
    }
    
    public String getSourceFilename() {
        return sourceFilename;
    }
    
    public boolean finalizable() {
        return Settings.FINALIZATION_SUPPORTED
            && (getContext().resolve(this,this,Runtime.finalize.getSignature())
                != Runtime.finalize);
    }
    
    public boolean specialScan() {
        return this==Global.root().classClass
            || this==Runtime.weakReference;
    }
    
    public int runtimeFlags() {
	return getVisibility().runtimeFlags()
            | getKind().runtimeFlags()
            | smode.runtimeFlags()
            | Constants.TBF_AOT
            | extraFlags
            | (finalizable()?Constants.TBF_FINALIZABLE:0)
            | (specialScan()?Constants.TBF_SPECIAL_SCAN:0);
    }
    
    public SuperMode getSuperMode() { return smode; }
    
    public boolean isInterface() {
	checkResolved();
	return kind==ClassKind.INTERFACE
	    || kind==ClassKind.ANNOTATION;
    }
    
    public boolean isAbstract() {
	checkResolved();
	return isInterface()
	    || kind==ClassKind.ABSTRACT;
    }
    
    public boolean isFinal() {
	checkResolved();
	return kind==ClassKind.FINAL;
    }
    
    public boolean isEffectivelyFinal() {
	checkResolved();
	return isFinal() || (Global.analysis().closed() && knownSubs.isEmpty());
    }
    
    public boolean hasSuperclass() {
	return extendz!=null;
    }
    public VisibleClass getSuperclass() {
	return extendz;
    }
    
    public int numSuperInterfaces() {
	return implementz.size();
    }
    public List< VisibleClass > getSuperInterfaces() {
	return implementz;
    }
    public OMClass getSuperInterface(int i) {
	return implementz.get(i);
    }
    
    public int numDirectSupertypes() {
	return (hasSuperclass()?1:0)+getSuperInterfaces().size();
    }
    
    public Iterable< VisibleClass > directSupertypes() {
	if (hasSuperclass()) {
	    return Util.pushIterable(getSuperclass(),getSuperInterfaces());
	} else {
	    return getSuperInterfaces();
	}
    }
    
    public Collection< VisibleField > fields() {
	checkResolved();
	return fields.values();
    }
    
    /**
     * Returns an ordered list starting with java.lang.Object listing
     * all of the classes (NOT interfaces) that this class derives from, in
     * order, up to this class.  Therefore the last element will always be
     * this class.
     */
    public List< VisibleClass > chainFromObjectRoot() {
        LinkedList< VisibleClass > result=new LinkedList< VisibleClass >();
        for (VisibleClass cur=this;cur!=null;cur=cur.extendz) {
            result.addFirst(cur);
        }
        return result;
    }
    
    public List< OMField > omFields() {
        ArrayList< OMField > result=new ArrayList< OMField >();
        result.addAll(fields.values());
        return result;
    }
    
    public OMField[] omLaidOutFields() {
        return laidOutFields;
    }
    
    public void omSetLaidOutFields(OMField[] fields) {
        laidOutFields=fields;
    }
    
    public OMField[] omAllLaidOutFields() {
        return allLaidOutFields();
    }
    
    /**
     * Returns an ordered list of all fields that an instance of this class
     * will have, including, in order, the fields of any superclasses.
     */
    public List< VisibleField > allFields() {
        ArrayList< VisibleField > result=new ArrayList< VisibleField >();
        for (VisibleClass vc : chainFromObjectRoot()) {
            result.addAll(vc.fields());
        }
        return result;
    }
    
    public VisibleMethod getMethod(MethodSignature sig) {
	checkResolved();
	return methods.get(sig);
    }
    
    public boolean hasMethod(MethodSignature sig) {
	checkResolved();
	return methods.containsKey(sig);
    }
    
    // convenience only.  use with care, since you cannot control which
    // context this resolves in!
    public VisibleMethod getMethod(String desc,String name) {
	checkResolved();
	return methods.get(MethodSignature.parse(getContext(),desc,name));
    }
    
    public VisibleField getField(FieldSignature sig) {
	checkResolved();
	return fields.get(sig);
    }
    
    // convenience only.  use with care, since you cannot control which
    // context this resolves in!
    public OMField getField(String desc,String name) {
	checkResolved();
	return fields.get(FieldSignature.parse(getContext(),desc,name));
    }
    
    // gets the field using the name only.  fails if there are multiple
    // such fields.  this is an O(n) lookup.
    public VisibleField getFieldByName(String name) {
	checkResolved();
	VisibleField result=null;
        if (wasFieldResolutionCanceledByName(name)) {
            throw new ResolutionFailed(
                this,
                new ResolutionID(getResolutionID(),"field "+name),
                "in getFieldByName: "+this.jniName()+
                " has multiple fields called "+name);
        }
	for (VisibleField vf : fields.values()) {
	    if (vf.getName().equals(name)) {
		if (result!=null) {
		    throw new ResolutionFailed(
                        this,
                        new ResolutionID(getResolutionID(),"field "+name),
                        "in getFieldByName: "+this.jniName()+
                        " has multiple fields called "+name);
		}
		result=vf;
	    }
	}
	return result;
    }
    
    // gets the method without caring about its return type.  fails if there are
    // multiple methods.  this is an O(n) lookup
    public VisibleMethod getMethodIgnoreResult(String name,Type[] params) {
	checkResolved();
	VisibleMethod result=null;
        if (wasMethodResolutionCanceledIgnoreResult(name,params)) {
            throw new ResolutionFailed(
                this,
                new ResolutionID(getResolutionID(),
                                 "field "+name+" with params "+Type.jniName(params)),
                "in getMethodIgnoreResult: "+this.jniName()+
                " has multiple methods called "+name+
                " with parameters "+Type.jniName(params));
        }
	for (VisibleMethod vm : methods.values()) {
	    if (vm.getName().equals(name) &&
		Util.equals(vm.getParams(),params)) {
		if (result!=null) {
		    throw new ResolutionFailed(
                        this,
                        new ResolutionID(getResolutionID(),
                                         "field "+name+" with params "+Type.jniName(params)),
			"in getMethodIgnoreResult: "+this.jniName()+
                        " has multiple methods called "+name+
			" with parameters "+Type.jniName(params));
		}
		result=vm;
	    }
	}
	return result;
    }
    
    public VisibleMethod getMethodIgnoreResult(MethodSignature ms) {
	return getMethodIgnoreResult(ms.getName(),ms.getParams());
    }
    
    public boolean wasResolutionCanceled(MemberSignature ms) {
        return resolutionsCanceled.contains(ms);
    }
    
    private boolean wasFieldResolutionCanceledByName(String name) {
        for (MemberSignature ms : resolutionsCanceled) {
            if (ms instanceof FieldSignature && ms.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean wasMethodResolutionCanceledIgnoreResult(String name,
                                                            Type[] params) {
        for (MemberSignature mems: resolutionsCanceled) {
            if (mems instanceof MethodSignature) {
                MethodSignature ms=(MethodSignature)mems;
                if (ms.getName().equals(name) &&
                    Util.equals(ms.getParams(),params)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isObjectRoot() {
	checkResolved();
        if (extendz==null) {
            assert this==Global.root().objectClass;
            return true;
        } else {
            assert this!=Global.root().objectClass;
            return false;
        }
    }
    
    public boolean isArraySupertype() {
	return this == Global.root().objectClass
	    || this == Global.root().serializableClass
	    || this == Global.root().cloneableClass;
    }
    
    public Set< VisibleClass > allStrictSupertypes() {
	checkResolved();
	if (false) System.out.println("known supers for "+this+": "+knownSupers);
	return knownSupers;
    }
    
    public Iterable< VisibleClass > allSupertypes() {
	checkResolved();
	return Util.pushIterable(this,allStrictSupertypes());
    }
    
    public boolean isStrictSubclassOf(OMClass other) {
	checkResolved();
	return allStrictSupertypes().contains(other);
    }
    
    public boolean isSubclassOf(OMClass other) {
	checkResolved();
	if (Global.verbosity>=101) {
	    Global.log.println("Is "+this+" a subtype of "+other+"?");
	}
	return this==other || isStrictSubclassOf(other);
    }
    
    public boolean isDirectSubclassOf(VisibleClass other) {
	checkResolved();
	return other.knownDirectSubs.contains(this);
    }
    
    public Set< VisibleClass > allKnownStrictSubclasses() {
	checkResolved();
	return knownSubs;
    }
    
    public Iterable< VisibleClass > allKnownSubclasses() {
	checkResolved();
	return Util.pushIterable(this,allKnownStrictSubclasses());
    }
    
    void forAllSupertypes(Visitor v) {
	checkResolved();
	HashSet< VisibleClass > seen=new HashSet< VisibleClass >();
	MyStack< VisibleClass > worklist=new MyStack< VisibleClass >();
	seen.add(this);
	worklist.push(this);
	while (!worklist.empty()) {
	    VisibleClass c=worklist.pop();
	    if (!v.visit(c)) {
		break;
	    }
	    for (VisibleClass c2 : c.directSupertypes()) {
		if (seen.add(c2)) {
		    worklist.push(c2);
		}
	    }
	}
    }
    
    public VisibleClass exectypeLub() {
	if (resolved() && isInterface()) {
	    return Global.root().objectClass;
	} else {
	    return this;
	}
    }
    
    public Type asType() {
	return Type.make(this);
    }
    
    public Exectype asExectype() {
	return Exectype.make(this);
    }
    
    public String canonicalName() {
	if (resolved()) {
	    if (isInterface()) {
		return "Interface["+name+" "+getContext().canonicalName()+"]";
	    } else {
		return "Class["+name+" "+getContext().canonicalName()+"]";
	    }
	} else {
	    return "Unresolved["+name+"]";
	}
    }
    
    public String toString() {
	String result;
	if (resolved()) {
	    if (isInterface()) {
		result="Interface["+name+" "+getContext();
	    } else {
		result="Class["+name+" "+getContext();
		if (isAbstract()) {
		    result+=" (abstract)";
		}
	    }
	    if (!hasInstances()) {
		result+=" (no instances)";
	    }
	    if (isEffectivelyFinal()) {
		result+=" (effectively final)";
	    }
	} else {
	    result="Unresolved["+name;
	}
	return result+"]";
    }
    
    public String simpleName() {
	return name.substring(name.lastIndexOf('/')+1);
    }
    
    public VisibleField[] allLaidOutFields() {
        try {
            checkResolved();
            if (!hasInstances()) {
                return VisibleField.EMPTY;
            } else {
                int totalSize=0;
                for (VisibleClass cur=this;cur!=null;cur=cur.extendz) {
                    totalSize+=cur.laidOutFields.length;
                }
                VisibleField[] result=new VisibleField[totalSize];
                int curOffset=result.length;
                for (VisibleClass cur=this;cur!=null;cur=cur.extendz) {
                    curOffset-=cur.laidOutFields.length;
                    System.arraycopy(cur.laidOutFields,0,
                                     result,curOffset,
                                     cur.laidOutFields.length);
                }
                return result;
            }
        } catch (Throwable e) {
            throw new CompilerException("failed to get laid out fields for "+this,e);
        }
    }
    
    public int alignedPayloadSize() {
	checkResolved();
	return (payloadSize()+Global.pointerSize-1)&~(Global.pointerSize-1);
    }
    
    // NOTE: these next two methods have vtableIndices!=null to allow
    // these calls to work on interfaces.  this is great in case you
    // have a virtual call on Object that gets refined to be a call on
    // an interface.
    public boolean hasVtableIndex(MethodSignature s) {
	checkResolved();
	return (vtableIndices!=null && vtableIndices.containsKey(s))
	    || (extendz!=null && extendz.hasVtableIndex(s));
    }
    
    public int getVtableIndex(MethodSignature s) {
	checkResolved();
	try {
	    Integer result=null;
	    if (vtableIndices!=null) {
		result=vtableIndices.get(s);
	    }
	    if (result==null && extendz!=null) {
		result=extendz.getVtableIndex(s);
	    }
	    return result;
	} catch (Throwable e) {
	    throw new CompilerException(
		"Could not get the vtable index of "+s+" in "+this,e);
	}
    }
    
    public Collection< VisibleMethod > methods() {
	checkResolved();
	return methods.values();
    }
    
    public Iterable< VisibleMember > members() {
	checkResolved();
	return Util.composeIterables(methods(),fields());
    }
    
    public int numExistingFields() {
	return Util.countExisting(fields());
    }
    
    public int numExistingMethods() {
	return Util.countExisting(methods());
    }
    
    public int numExistingMembers() {
	return Util.countExisting(members());
    }
    
    public int numIntrinsics() {
        int result=0;
        for (VisibleMethod m : methods.values()) {
            if (m.isIntrinsic()) {
                result++;
            }
        }
        return result;
    }
    
    public boolean hasIntrinsics() {
        return numIntrinsics()>0;
    }
    
    public boolean usesMagic() {
        return hasIntrinsics() || usesMagic;
    }
    
    public synchronized HashSet< VisibleMethod > allInstanceMethods() {
	checkResolved();
	if (allInstanceMethods==null) {
	    HashSet< VisibleMethod > result=new HashSet< VisibleMethod >();
	    // FIXME: rewrite using allSupertypes()
	    HashSet< VisibleClass > seen=new HashSet< VisibleClass >();
	    MyStack< VisibleClass > worklist=new MyStack< VisibleClass >();
	    worklist.push(this);
	    seen.add(this);
	    while (!worklist.empty()) {
		VisibleClass c=worklist.pop();
		for (VisibleMethod m : c.methods()) {
		    if (!m.isStatic() && Global.analysis().isCalled(m)) {
			result.add(m);
		    }
		}
		if (c.hasSuperclass() &&
		    !seen.contains(c.getSuperclass())) {
		    seen.add(c.getSuperclass());
		    worklist.push(c.getSuperclass());
		}
		for (VisibleClass i : c.getSuperInterfaces()) {
		    if (!seen.contains(i)) {
			seen.add(i);
			worklist.push(i);
		    }
		}
	    }
	    allInstanceMethods=result;
	}
	return allInstanceMethods;
    }
    
    public HashSet< VisibleMethod > allMethodImplementations() {
	checkResolved();
	HashSet< VisibleMethod > result=new HashSet< VisibleMethod >();
	for (VisibleClass c=this;c!=null;c=c.getSuperclass()) {
	    for (VisibleMethod m : c.methods.values()) {
		if (!m.isAbstract() && Global.analysis().isExecuted(m)) {
		    result.add(m);
		}
	    }
	}
	return result;
    }
    
    public HashSet< VisibleMethod > allInterfaceMethods() {
	checkResolved();
	HashSet< VisibleMethod > result=new HashSet< VisibleMethod >();
	// FIXME: rewrite using allSupertypes
	HashSet< VisibleClass > seen=new HashSet< VisibleClass >();
	MyStack< VisibleClass > worklist=new MyStack< VisibleClass >();
	worklist.push(this);
	seen.add(this);
	while (!worklist.empty()) {
	    VisibleClass c=worklist.pop();
	    if (c.isInterface()) {
		for (VisibleMethod m : c.methods()) {
		    if (!m.isStatic() && Global.analysis().isCalled(m)) {
			result.add(m);
		    }
		}
	    }
	    if (c.hasSuperclass() &&
		!seen.contains(c.getSuperclass())) {
		seen.add(c.getSuperclass());
		worklist.push(c.getSuperclass());
	    }
	    for (VisibleClass i : c.getSuperInterfaces()) {
		if (!seen.contains(i)) {
		    seen.add(i);
		    worklist.push(i);
		}
	    }
	}
	return result;
    }
    
    synchronized void ensureResolution() {
	checkResolved();
	if (resolution==null) {
	    resolution=new OverrideMap(allInstanceMethods());
            if (false) System.out.println("computing dynable methods for "+this.getName());
	    dynableMethods=resolution.computeDynables();
	}
    }
    
    public OverrideMap resolution() {
	checkResolved();
	ensureResolution();
	return resolution;
    }
    
    public HashSet< VisibleMethod > allExactDynableMethods() {
	checkResolved();
	ensureResolution();
        assert dynableMethods!=null;
	return dynableMethods;
    }
    
    public HashSet< VisibleMethod > allDynableMethods() {
	checkResolved();
	assert Global.analysis().closed();
	HashSet< VisibleMethod > result=Util.copy(allExactDynableMethods());
	for (VisibleClass c : knownSubs) {
            if (false) {
                System.out.println("known sub of "+this.getName()+": "+c.getName());
                for (VisibleMethod m : c.allExactDynableMethods()) {
                    System.out.println("   exact dynable: "+m.jniName());
                }
            }
            result.addAll(c.allExactDynableMethods());
	}
	return result;
    }
    
    public synchronized OverrideMap exactDispatch() {
	checkResolved();
	if (exactDispatch==null) {
	    exactDispatch=new OverrideMap();
	    for (VisibleMethod m : allExactDynableMethods()) {
		exactDispatch.add(m);
	    }
	}
	return exactDispatch;
    }
    
    public synchronized OverrideMap dispatch() {
	checkResolved();
	if (dispatch==null) {
	    dispatch=new OverrideMap();
	    for (VisibleMethod m : allDynableMethods()) {
		dispatch.add(m);
	    }
	}
	return dispatch;
    }
    
    public OverrideMap dispatch(TypeBoundMode mode) {
	checkResolved();
	if (mode==TypeBoundMode.EXACT) return exactDispatch();
	else return dispatch();
    }
    
    public ClassBound bound(TypeBoundMode mode) {
	checkResolved();
        // no need to synchronize ... we don't really care if there are multiple
        // ClassBounds flying around.
	if (mode.isExact()) {
	    if (exactBound==null) {
		exactBound=new ClassBound(this,TypeBoundMode.EXACT);
	    }
	    return exactBound;
	} else {
	    if (bound==null) {
		bound=new ClassBound(this);
	    }
	    return bound;
	}
    }
    
    public boolean hasInstances() {
	checkResolved();
	return asType().hasInstances();
    }
    
    public boolean hasMeaningfulClinit() {
	checkResolved();
	return hasMeaningfulClinit;
    }
    
    public synchronized boolean shouldCallCheckInit() {
	checkResolved();
	if (!hasShouldCallCheckInit) {
	    shouldCallCheckInit=hasMeaningfulClinit();
	    if (!shouldCallCheckInit) {
		for (VisibleClass c : allStrictSupertypes()) {
		    if (c.hasMeaningfulClinit()) {
			shouldCallCheckInit=true;
			break;
		    }
		}
	    }
	    hasShouldCallCheckInit=true;
	}
	return shouldCallCheckInit;
    }
    
    public boolean initialized() {
        return !shouldCallCheckInit();
    }
    
    public int valueOfRuntimeInitedField() {
        if (resolved() && initialized()) {
            return 1;
        } else {
            return 0;
        }
    }
    
    public HashSet< VisibleMethod > relevantStaticInits() {
	checkResolved();
	MethodSignature s=new MethodSignature(Type.VOID,"<clinit>");
	HashSet< VisibleMethod > result=new HashSet< VisibleMethod >();
	for (VisibleClass c : allSupertypes()) {
	    VisibleMethod m=c.getMethod(s);
	    if (m!=null) {
		assert m.isStatic();
		result.add(m);
	    }
	}
	return result;
    }
    
    private void recomputeMemberIndices(
	Iterable< ? extends VisibleMember > members) {
	checkResolved();
	int cnt=0;
	for (VisibleMember m : members) {
	    if (m.shouldExist()) {
		m.index=cnt++;
	    }
	}
    }
    
    public void recomputeMemberIndices() {
	checkResolved();
	recomputeMemberIndices(methods.values()); 
	recomputeMemberIndices(fields.values()); 
    }
    
    void setBytecode(byte[] bytecode) {
        assert bytecode!=null;
	if (this.bytecode!=null) {
	    if (Global.verbosity>=2) {
		Global.log.println("Warning: Multiple definitions of "+this+"; using original one.");
	    }
	    return;
	}
	this.bytecode=bytecode;
    }
    
    void populateFromBytecode() {
        if (bytecode==null) {
            return;
        }
	new ClassReader(bytecode).accept(
	    new EmptyVisitor(){
		public void visit(int version,
				  int access,
				  UTF8Sequence name,
				  UTF8Sequence signature,
				  UTF8Sequence superName,
				  UTF8Sequence[] interfaces) {
		    if (!VisibleClass.this.name.equals(name.toString())) {
			throw new BadBytecode(
			    "Name mismatch in "+VisibleClass.this+": bytecode "+
			    "claims that the name is actually "+name);
		    }
		    kind=ClassKind.VIRTUAL;
		    smode=SuperMode.OLD_SUPER_MODE;
		    visibility=Visibility.PACKAGE;
		    if ((access&Opcodes.ACC_FINAL)!=0) {
			kind=ClassKind.FINAL;
		    }
		    if ((access&Opcodes.ACC_ABSTRACT)!=0) {
			kind=ClassKind.ABSTRACT;
		    }
		    if ((access&Opcodes.ACC_INTERFACE)!=0) {
			kind=ClassKind.INTERFACE;
		    }
		    if ((access&Opcodes.ACC_ANNOTATION)!=0) {
			kind=ClassKind.ANNOTATION;
		    }
		    if ((access&Opcodes.ACC_SUPER)!=0) {
			smode=SuperMode.NEW_SUPER_MODE;
		    }
		    if ((access&Opcodes.ACC_PUBLIC)!=0) {
			visibility=Visibility.PUBLIC;
		    }
		    if (superName==null) {
                        if (VisibleClass.this!=Global.root().objectClass) {
                            throw new BadBytecode(
                                "Cannot have class with no superclass except if it's "+
                                "the ROOT java.lang.Object");
                        }
                    } else {
			extendz=context.forceClass(superName.toString());
		    }
		    for (UTF8Sequence interfase : interfaces) {
			implementz.add(context.forceClass(interfase.toString()));
		    }
		}
		public void visitSource(UTF8Sequence source,
					UTF8Sequence debug) {
		    sourceFilename=source.toString();
		}
		public AnnotationVisitor visitAnnotation(UTF8Sequence desc,
							 boolean visible) {
		    return addAnnotation(desc.toString(),visible);
		}
		public FieldVisitor visitField(int access,
					       UTF8Sequence name,
					       UTF8Sequence desc,
					       UTF8Sequence signature,
					       Object value) {
		    Binding b=Binding.INSTANCE;
		    Visibility v=Visibility.PACKAGE;
		    Mutability m=Mutability.MUTABLE;
		    Volatility o=Volatility.NON_VOLATILE;
		    Serializability e=Serializability.PERSISTANT;
		    if ((access&Opcodes.ACC_STATIC)!=0) {
			b=Binding.STATIC;
		    }
		    if ((access&Opcodes.ACC_PUBLIC)!=0) {
			v=Visibility.PUBLIC;
		    }
		    if ((access&Opcodes.ACC_PROTECTED)!=0) {
			v=Visibility.PROTECTED;
		    }
		    if ((access&Opcodes.ACC_PRIVATE)!=0) {
			v=Visibility.PRIVATE;
		    }
		    if ((access&Opcodes.ACC_FINAL)!=0) {
			m=Mutability.FINAL;
		    }
		    if ((access&Opcodes.ACC_VOLATILE)!=0) {
			o=Volatility.VOLATILE;
		    }
		    if ((access&Opcodes.ACC_TRANSIENT)!=0) {
			e=Serializability.TRANSIENT;
		    }
		    FieldSignature s=FieldSignature.parse(context,
                                                          desc.toString(),
                                                          name.toString());
		    final VisibleField f=
			new VisibleField(VisibleClass.this,
					 fields.size(),
					 b,v,m,o,e,s);
		    fields.put(s,f);
		    return f.new AnnotationAdderVisitor();
		}
		public MethodVisitor visitMethod(int access,
						 UTF8Sequence name,
						 UTF8Sequence desc,
						 UTF8Sequence signature,
						 UTF8Sequence[] exceptions) {
		    Binding b=Binding.INSTANCE;
		    MethodKind k=MethodKind.VIRTUAL;
		    MethodSync s=MethodSync.UNSYNCHRONIZED;
		    MethodImpl i=MethodImpl.BYTECODE;
		    Visibility v=Visibility.PACKAGE;
		    if ((access&Opcodes.ACC_STATIC)!=0) {
			b=Binding.STATIC;
		    }
		    if ((access&Opcodes.ACC_PUBLIC)!=0) {
			v=Visibility.PUBLIC;
		    }
		    if ((access&Opcodes.ACC_PROTECTED)!=0) {
			v=Visibility.PROTECTED;
		    }
		    if ((access&Opcodes.ACC_PRIVATE)!=0) {
			v=Visibility.PRIVATE;
		    }
		    if ((access&Opcodes.ACC_FINAL)!=0) {
			k=MethodKind.FINAL;
		    }
		    if ((access&Opcodes.ACC_ABSTRACT)!=0) {
			k=MethodKind.ABSTRACT;
			i=MethodImpl.STUB;
		    }
		    if ((access&Opcodes.ACC_SYNCHRONIZED)!=0) {
			s=MethodSync.SYNCHRONIZED;
		    }
		    if ((access&Opcodes.ACC_NATIVE)!=0) {
			i=MethodImpl.JNI;
		    }
		    if (exceptions!=null) {
			for (UTF8Sequence e : exceptions) {
			    context.forceClass(e.toString());
			}
		    }
		    MethodSignature sig=MethodSignature.parse(context,
                                                              desc.toString(),
                                                              name.toString());
		    final VisibleMethod m=
			new VisibleMethod(VisibleClass.this,
					  methods.size(),
					  b,v,k,s,i,sig);
		    methods.put(sig,m);
		    return m.new AnnotationAdderVisitor() {
			    public void visitFieldInsn(int opcode,
						       UTF8Sequence owner,
						       UTF8Sequence name,
						       UTF8Sequence desc) {
				context.forceClass(owner.toString());
				FieldSignature.parse(context,desc.toString(),name.toString());
				switch (opcode) {
				case Opcodes.GETSTATIC:
				    m.size+=2;
				    break;
				case Opcodes.PUTSTATIC:
				    if (desc.charAt(0)=='L') {
					m.size+=5;
				    } else {
					m.size+=2;
				    }
				    m.makeMeaningful();
				    break;
				case Opcodes.GETFIELD:
				    m.size+=3;
				    break;
				case Opcodes.PUTFIELD:
				    if (desc.charAt(0)=='L') {
					m.size+=6;
				    } else {
					m.size+=3;
				    }
				    m.makeMeaningful();
				    break;
				default:
				    throw new Error("bad opcode");
				}
			    }
			    public void visitIincInsn(int var,int increment) {
				m.size++;
			    }
			    public void visitInsn(int opcode) {
				switch (opcode) {
				case Opcodes.FADD:
				case Opcodes.DADD:
				case Opcodes.FSUB:
				case Opcodes.DSUB:
				case Opcodes.FMUL:
				case Opcodes.DMUL:
				case Opcodes.FDIV:
				case Opcodes.DDIV:
				case Opcodes.FNEG:
				case Opcodes.DNEG:
				case Opcodes.I2F:
				case Opcodes.I2D:
				case Opcodes.L2F:
				case Opcodes.L2D:
				case Opcodes.F2D:
				case Opcodes.D2I:
				case Opcodes.D2L:
				case Opcodes.D2F:
				case Opcodes.F2I:
				case Opcodes.F2L:
				case Opcodes.FCMPL:
				case Opcodes.FCMPG:
				case Opcodes.DCMPL:
				case Opcodes.DCMPG:
				case Opcodes.ARRAYLENGTH:
				case Opcodes.FREM:
				case Opcodes.DREM:
				    m.size+=2;
				    break;
				case Opcodes.MONITORENTER:
				case Opcodes.MONITOREXIT:
				    m.size+=20;
				    // should I call makeMeaningful()?
				    break;
				case Opcodes.IALOAD:
				case Opcodes.LALOAD:
				case Opcodes.FALOAD:
				case Opcodes.DALOAD:
				case Opcodes.AALOAD:
				case Opcodes.BALOAD:
				case Opcodes.CALOAD:
				case Opcodes.SALOAD:
				    m.size+=3;
				    break;
				case Opcodes.IASTORE:
				case Opcodes.LASTORE:
				case Opcodes.FASTORE:
				case Opcodes.DASTORE:
				case Opcodes.BASTORE:
				case Opcodes.CASTORE:
				case Opcodes.SASTORE:
				    m.size+=3;
				    m.makeMeaningful();
				    break;
				case Opcodes.AASTORE:
				    m.size+=6;
				    m.makeMeaningful();
				    break;
				case Opcodes.ATHROW:
				    m.makeMeaningful();
				    m.size+=2;
				    break;
				default:
				    m.size++;
				}
			    }
			    public void visitIntInsn(int opcode,int operand) {
				switch (opcode) {
				case Opcodes.NEWARRAY:
				    m.size+=20;
				    break;
				default:
				    m.size++;
				}
			    }
			    public void visitJumpInsn(int opcode,Label label) {
				if (opcode==Opcodes.JSR) {
				    m.size+=40;
				    m.hasJsr=true;
                                    hasJsr=true;
				} else {
				    m.size++;
				}
			    }
			    public void visitLdcInsn(Object cst) {
				if (cst instanceof String) {
				    m.size+=2;
				} else if (cst instanceof com.fiji.asm.Type) {
				    Type.parse(
					context,
					((com.fiji.asm.Type)cst).getDescriptor());
				    m.size+=2;
				} else {
				    m.size++;
				}
			    }
			    public void visitLookupSwitchInsn(Label dflt,
							      int[] keys,
							      Label[] labels) {
				m.size+=keys.length*2;
			    }
			    public void visitMaxs(int maxStack,int maxLocals) {
				m.size+=maxStack+maxLocals;
				m.numVars+=maxLocals;
                                m.maxStack=maxStack;
                                m.maxLocals=maxLocals;
			    }
			    public void visitMethodInsn(int opcode,
							UTF8Sequence owner,
							UTF8Sequence name,
							UTF8Sequence desc) {
				MethodSignature.parse(context,
                                                      desc.toString(),
                                                      name.toString());
				switch (opcode) {
				case Opcodes.INVOKEINTERFACE:
				    m.size+=15;
				    break;
				default:
				    m.size+=10;
				    break;
				}
				m.makeMeaningful();
			    }
			    public void visitMultiANewArrayInsn(UTF8Sequence desc,int dims) {
				// FIXME: this is actually broken, as we don't mark
				// the element array types that also get instantiated.
				// CHA handles it by closing the array elements, and
				// 0CFA handles it by seeing the post-processed code
				// in which these are blown away.
				Type.parse(context,desc.toString());
				m.size+=20*dims;
			    }
			    public void visitTableSwitchInsn(int min,
							     int max,
							     Label dflt,
							     Label[] labels) {
				m.size+=(max-min+1)*2;
			    }
			    public void visitTryCatchBlock(Label start,
							   Label end,
							   Label handler,
							   UTF8Sequence type) {
				m.size+=12;
			    }
			    public void visitTypeInsn(int opcode,
						      UTF8Sequence type) {
				Type t=Type.parseRefOnly(context,type.toString());
				switch (opcode) {
				case Opcodes.NEW:
				    m.size+=14;
				    break;
				case Opcodes.ANEWARRAY:
				    t.makeArray();
				    m.size+=15;
				    break;
				default:
				    m.size+=10;
				    break;
				}
			    }
			    public void visitVarInsn(int opcode,
						     int var) {
				m.size++;
			    }
			};
		}
	    },
	    ClassReader.SKIP_FRAMES);
        
        if (shouldExclude(this)) {
            cancelResolution();
            return;
        }
        
        ArrayList< FieldSignature > fieldSigsToRemove=new ArrayList< FieldSignature >();
        for (Map.Entry< FieldSignature, VisibleField > e : fields.entrySet()) {
            if (shouldExclude(e.getValue())) {
                fieldSigsToRemove.add(e.getKey());
            }
        }
        for (FieldSignature fs : fieldSigsToRemove) {
            fields.remove(fs);
            resolutionsCanceled.add(fs);
        }
        
        ArrayList< MethodSignature > methodSigsToRemove=new ArrayList< MethodSignature >();
        for (Map.Entry< MethodSignature, VisibleMethod > e : methods.entrySet()) {
            if (shouldExclude(e.getValue())) {
                methodSigsToRemove.add(e.getKey());
                if (e.getValue().hasAnnotation(Global.root().exportClass)) {
                    Payload.exportMethodResolutionCanceled(e.getValue());
                }
            }
        }
        for (MethodSignature ms : methodSigsToRemove) {
            methods.remove(ms);
            resolutionsCanceled.add(ms);
        }
    }
        
    private boolean shouldExclude(Annotatable a) {
        VisibleAnnotation va=a.getAnnotation(Global.root().excludeUnlessSetClass);
        if (va!=null) {
            for (String setting : (List< String >)va.get("value")) {
                try {
                    if (!Settings.class.getField(setting).getBoolean(null)) {
                        return true;
                    }
                } catch (NoSuchFieldException e) {
                    throw new Error(e);
                } catch (IllegalAccessException e) {
                    throw new Error(e);
                }
            }
        }
        return false;
    }
    
    public byte[] purifiedBytecode() {
        if (hasJsr) {
            ClassWriter cw=new ClassWriter(0);
            new ClassReader(bytecode).accept(
                new ClassAdapter(cw) {
                    public MethodVisitor visitMethod(int access,
                                                     UTF8Sequence name,
                                                     UTF8Sequence desc,
                                                     UTF8Sequence signature,
                                                     UTF8Sequence[] exceptions) {
                        return new JSRInlinerAdapter(
                            super.visitMethod(
                                access,name,desc,signature,exceptions),
                            access,
                            name.toString(),
                            desc.toString(),
                            signature==null?null:signature.toString(),
                            UTF8Sequence.toString(exceptions));
                    }
                },
                0);
            return cw.toByteArray();
        } else {
            return bytecode;
        }
    }
    
    // horrific hack; I don't remember why it's here
    private static MethodSignature cloneHelperSig_;
    static synchronized MethodSignature cloneHelperSig() {
        if (cloneHelperSig_==null) {
            cloneHelperSig_=new MethodSignature(
                Global.root().objectType,
                Constants.SMN_CLONE_HELPER,
                Type.EMPTY);
        }
        return cloneHelperSig_;
    }
    
    void addSyntheticMethods() {
        checkResolved();
        if ((isObjectRoot() ||
             isSubclassOf(Global.root().cloneableClass)) &&
            !isInterface() &&
            !hasMethod(cloneHelperSig())) {
            addCloneHelper();
        }
    }
    
    void addSyntheticFields() {
        checkResolved();
        if (getContext()==Global.root()) {
            if (getName().equals("java/lang/Thread")) {
                addRTThreadField();
            }
            if (getName().equals("java/lang/ClassLoader")) {
                addCLRefsField();
            }
        }
    }
    
    void modifySpecialFields() {
        checkResolved();
        if (getContext()==Global.root()) {
            if (Global.lib==Library.FIJICORE ||
                Global.lib==Library.GLIBJ) {
                if (getName().equals("java/lang/Class") ||
                    getName().equals("java/lang/ClassLoader")) {
                    modifyVMDataField();
                }
            }
        }
    }
    
    private void addCloneHelper() {
        int cost=0;
        for (VisibleField f : allFields()) {
            if (f.isBarriered()) {
                cost+=8;
            } else {
                cost+=2;
            }
        }
        
        VisibleMethod vm=
            new VisibleMethod(
                this,
                methods.size(),
                Binding.INSTANCE,
                Visibility.PUBLIC,
                MethodKind.VIRTUAL,
                MethodSync.UNSYNCHRONIZED,
                MethodImpl.SYNTHETIC,
                cloneHelperSig());
        
        vm.size=cost;
        vm.makeMeaningful();
        
        methods.put(cloneHelperSig(),vm);
    }
    
    private void addRTThreadField() {
        FieldSignature fs=new FieldSignature(Global.root().objectType,
                                             "fiji_runtimeField");
        fields.put(fs,
                   new VisibleField(this,
                                    fields.size(),
                                    Binding.INSTANCE,
                                    Visibility.PUBLIC,
                                    Mutability.MUTABLE,
                                    Volatility.NON_VOLATILE,
                                    Serializability.PERSISTANT,
                                    fs));
    }
    
    private void addCLRefsField() {
        FieldSignature fs=new FieldSignature(Global.root().objectType,
                                             "fiji_refs");
        fields.put(fs,
                   new VisibleField(this,
                                    fields.size(),
                                    Binding.INSTANCE,
                                    Visibility.PUBLIC,
                                    Mutability.MUTABLE,
                                    Volatility.NON_VOLATILE,
                                    Serializability.PERSISTANT,
                                    fs));
    }
    
    private void modifyVMDataField() {
        getFieldByName("vmdata").traceMode=TraceMode.NOT_A_REFERENCE;
    }
    
    void buildVTable() {
	checkResolved();

	if (vtable!=null) return;
	if (isInterface()) return;
	
	MethodSignature[] parentVTableSigs;
	if (hasSuperclass()) {
	    getSuperclass().buildVTable();
	    parentVTableSigs=getSuperclass().vtableSigs;
	} else {
	    parentVTableSigs=new MethodSignature[0];
	}
	
	// figure out which methods we add to the situation, and assign them
	// indices.  for now the only point is to just get a vtable built.
	HashSet< VisibleMethod > allMethods=allInstanceMethods();
	vtableIndices=new HashMap< MethodSignature, Integer >();
	int vtableSize=parentVTableSigs.length;
	for (VisibleMethod m : allMethods) {
	    // NOTE: maybe use isEffectivelyFinal here?
	    if (!m.isInitializer() && !m.isDeclaredFinal() &&
		!hasVtableIndex(m.getSignature())) {
		vtableIndices.put(m.getSignature(),
				  vtableSize++);
	    }
	}
	
	// build the vtable signatures
	vtableSigs=new MethodSignature[vtableSize];
	System.arraycopy(parentVTableSigs,0,
			 vtableSigs,0,
			 parentVTableSigs.length);
	for (Map.Entry< MethodSignature, Integer > e : vtableIndices.entrySet()) {
	    vtableSigs[e.getValue()]=e.getKey();
	}
	
	// create a mapping that we can use for finding overridden methods
	OverrideMap overrideMap=resolution();
	for (VisibleMethod m : allMethods) {
	    overrideMap.add(m);
	}
	
	// for each vtable slot, figure out which method goes there, using the
	// friendly override map.  also add the resulting method to the dynable
	// set.
	vtable=new VisibleMethod[vtableSize];
	for (int i=0;i<vtableSize;++i) {
	    vtable[i]=OverrideMap.mostSpecific(
		overrideMap.getOverridingMethods(vtableSigs[i]));
	}
	
	// for each method that we have access to, figure out which methods override
	// it.  this is essentially CHA, unless you only invoke buildVTable()
	// on classes that you know to be live, in which case it becomes something
	// like RTA.  in reality for all of our closed analyses we do this in the
	// CHA style, but then mark methods as not executable as appropriate.
	for (VisibleMethod m : allMethods) {
	    if (!m.isAbstract()) {
		for (VisibleMethod m2 : overrideMap.getStrictOverriddenMethods(m)) {
		    m2.knownOverrides.add(m);
		    m.knownOverriddens.add(m2);
		    if (m2.knownOverrides.size()==10 && Global.verbosity>=3) {
			Global.log.println(m2+" has 10+ overrides.");
		    }
		}
	    }
	}
    }
    
    void buildITable() {
	checkResolved();

	if (isInterface() || !hasInstances()) return;
	
        if (Settings.ITABLE_COMPRESSION) {
            OverrideMap om=new OverrideMap();
            HashSet< MethodSignature > ifaceMethodSigs=new HashSet< MethodSignature >();
            for (VisibleMethod m : allInstanceMethods()) {
                if (m.getClazz().isInterface()) {
                    assert Global.analysis().isCalled(m) : "Got a method that isn't called: "+m;
                    ifaceMethodSigs.add(m.getSignature());
                }
                om.add(m);
            }
            if (Global.verbosity>=3) {
                Global.log.println("Class "+this+" has interface method signatures: "+ifaceMethodSigs);
            }
            itable=new VisibleMethod[Global.maxImethodIndex+1];
            minITableIndex=Integer.MAX_VALUE;
            maxITableIndex=-1;
            for (MethodSignature s : ifaceMethodSigs) {
                VisibleMethod result=
                    OverrideMap.mostSpecific(
                        om.getOverridingMethods(s));
                if (result!=null) {
                    try {
                        int index=Global.imethodSigIndex.get(s);
                        assert itable[index]==null;
                        itable[index]=result;
                        minITableIndex=Math.min(minITableIndex,index);
                        maxITableIndex=Math.max(maxITableIndex,index);
                    } catch (Throwable e) {
                        throw new CompilerException("Failed to compute interface method index for "+s,e);
                    }
                }
            }
        } else {
            OverrideMap om=new OverrideMap(allInstanceMethods());
            itable=new VisibleMethod[Global.maxImethodIndex+1];
            minITableIndex=Integer.MAX_VALUE;
            maxITableIndex=-1;
            for (VisibleMethod im : allInterfaceMethods()) {
                VisibleMethod dm=OverrideMap.mostSpecific(
                    om.getOverridingMethods(im.getSignature()));
                if (dm!=null) {
                    try {
                        int index=Global.imethodIndex.get(im);
                        assert itable[index]==null;
                        itable[index]=dm;
                        minITableIndex=Math.min(minITableIndex,index);
                        maxITableIndex=Math.max(maxITableIndex,index);
                    } catch (Throwable e) {
                        throw new CompilerException("Failed to compute interface methods index for "+im,e);
                    }
                }
            }
        }
    }
    
    void layOutFields(FieldLayerOuter flo) {
	checkResolved();
	if (laidOutFields!=null) return;
	if (extendz!=null) {
	    extendz.layOutFields(flo);
	}
	flo.layOutFields(this);
    }
    
    public void dump() {
	System.out.println("Class name: "+name);
	System.out.println("  Visibility: "+visibility);
	System.out.println("  Kind: "+kind);
	System.out.println("  Super Mode: "+smode);
	System.out.println("  Extends: "+extendz);
	System.out.print("  Implements:");
	for (OMClass interfase : implementz) {
	    System.out.print(" "+interfase);
	}
	System.out.println();
	System.out.print("  Known subs:");
	for (OMClass c : knownSubs) {
	    System.out.print(" "+c);
	}
	System.out.println();
	System.out.print("  Fields:");
	for (OMField f : fields.values()) {
	    System.out.print(" "+f);
	}
	System.out.println();
	System.out.println("  Methods:");
	for (VisibleMethod m : methods.values()) {
	    m.dump();
	}
    }
    
    public ResolutionID getResolutionID() {
        return new ResolutionID(resolutionMode(),
                                getContext().description(),
                                getName());
    }
    
    public ConfigNode getClassDescriptor() {
        return getResolutionID().asConfigNode();
    }
    
    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    public int payloadSize() {
        return payloadSize;
    }

    public void setRequiredPayloadAlignment(int requiredPayloadAlignment) {
        this.requiredPayloadAlignment = requiredPayloadAlignment;
    }

    public int requiredPayloadAlignment() {
        return requiredPayloadAlignment;
    }
    
    public void clearNameCaches() {
        for (VisibleMethod m : methods.values()) {
            m.clearNameCaches();
        }
        for (VisibleField f : fields.values()) {
            f.clearNameCaches();
        }
    }

    public static abstract class Visitor {
	// return false if we should stop iterating
	public abstract boolean visit(VisibleClass c);
    }
    
    public static VisibleClass[] EMPTY=new VisibleClass[0];
}

