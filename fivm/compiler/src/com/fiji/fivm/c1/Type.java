/*
 * Type.java
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

import java.util.*;
import com.fiji.fivm.*;
import com.fiji.util.*;

/** Represents the typesystem used for method and field resolution. */
public final class Type extends TypeImpl< Type > {

    Exectype cachedExectype;
    
    Type cachedArray;
    
    static int numBuckets;
    byte[] buckets;
    int bucket;
    byte tid;
    
    boolean knownFinal=false;
    
    int perContextIndex;
    int globalIndex;
    int numDescendants;
    
    int uniqueID;

    Type(Basetype basetype,VisibleClass clazz,int arrayDepth) {
	super(basetype,clazz,arrayDepth);
	synchronized (nextID) {
	    uniqueID = nextID[0]++;
	}
    }
    
    static boolean willClose=false;
    static boolean closed=false;
    private static boolean haveCreatedTypes;
    private static HashMap< TypeKey, Type > hashConsMap;
    private static ArrayList< Type > knownRealTypes;
    static HashSet< Type > usedTypes; // types that are live as well as relatives of live types
    
    static int[] nextID = new int[1];

    public static void willClose() {
        assert !haveCreatedTypes;
        willClose=true;
    }
    
    public static void closeSupers(Set< Type > typeSet) {
	MyStack< Type > worklist=new MyStack< Type >();
	for (Type t : typeSet) {
	    worklist.push(t);
	}
	while (!worklist.empty()) {
	    Type t=worklist.pop();
	    if (t.hasConcreteSupertypes()) {
		for (Type t2 : t.supertypes()) {
		    if (typeSet.add(t2)) {
			worklist.push(t2);
		    }
		}
	    }
	}
    }
    
    public static void closeArrayElements(Set< Type > typeSet) {
	MyStack< Type > worklist=new MyStack< Type >();
	for (Type t : typeSet) {
	    if (t.isArray()) {
		worklist.push(t);
	    }
	}
	while (!worklist.empty()) {
	    Type t=worklist.pop();
	    assert t.isArray();
	    Type t2=t.arrayElement();
	    if (typeSet.add(t2) && t2.isArray()) {
		worklist.push(t2);
	    }
	}
    }
    
    public static void closeTypes() {
	long before=System.currentTimeMillis();
	
	usedTypes=new HashSet< Type >();
	
	usedTypes.addAll(Global.knownTypesThatMayBeUsed());
	closeArrayElements(usedTypes);
	closeSupers(usedTypes);
        
        for (Context c : Global.contextList()) {
            c.runtimeUsedResolvedTypesSorted=new ArrayList< Type >();
            c.runtimeUsedUnresolvedTypesSorted=new ArrayList< Type >();
        }
        for (Type t : usedTypes) {
            if (!t.isBottomish()) {
                t.getContext().allTypesUsedAtRuntimeFor(t.resolvedState()).add(t);
            }
        }
        for (ResolvedState rs : new ResolvedState[]{ResolvedState.RESOLVED,
                                                    ResolvedState.UNRESOLVED}) {
            int globalIdx=0;
            for (Context c : Global.contextList()) {
                ArrayList< Type > list=c.allTypesUsedAtRuntimeFor(rs);
                Collections.sort(
                    list,
                    new Comparator< Type >(){
                        public int compare(Type a,Type b) {
                            // FIXME - should be sorted according to the strcmp
                            // of the UTF8 form of the jniName
                            return a.jniName().compareTo(b.jniName());
                        }
                    });
                for (int i=0;i<list.size();++i) {
                    list.get(i).perContextIndex=i;
                    list.get(i).globalIndex=globalIdx+i;
                }
                globalIdx+=list.size();
            }
        }
        if (Global.analysis().closed()) {
            for (Type t : usedTypes) {
                if (t.hasConcreteSupertypes()) {
                    t.knownFinal=true;
                } else {
                    t.knownFinal=false;
                }
            }
            MyStack< Type > worklist=new MyStack< Type >();
            HashSet< Type > seen=new HashSet< Type >();
            for (Type t : Global.knownTypesThatMayBeInstantiated()) {
                if (seen.add(t)) {
                    worklist.push(t);
                }
            }
            while (!worklist.empty()) {
                Type t = worklist.pop();
                if (t.hasConcreteSupertypes()) {
                    for (Type t2 : t.supertypes()) {
                        t2.knownFinal=false;
                        if (Global.verbosity>=4) {
                            Global.log.println("marking "+t2+" non-final because of "+t);
                        }
                        if (seen.add(t2)) {
                            worklist.push(t2);
                        }
                    }
                }
            }
        }
	closed=true;

	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) {
	    Global.log.println("Closing the typesystem took "+(after-before)+" ms");
	}
    }
    
    public static boolean closed() {
        return closed;
    }
    
    public static Collection< Type > allKnownTypes() {
	return hashConsMap.values();
    }
    
    public static Collection< Type > allKnownRealTypes() {
        return knownRealTypes;
    }
    
    public static Set< Type > allUsedTypes() {
	return usedTypes;
    }
    
    public int runtimePerContextIndex() {
        return perContextIndex;
    }
    
    public int runtimeGlobalIndex() {
        return globalIndex;
    }
    
    public boolean isUsed() {
        return usedTypes.contains(this);
    }
    
    static class TypeNode extends GraphColoring.Node {
	Type t;
	
	TypeNode(Type t) { this.t=t; }
    }

    public static void buildUnifiedTypeTests() {
        assert closed;
        
	// build buckets using graph coloring.  for each type, cluster it and
	// its ancestors.
	
	long before=System.currentTimeMillis();
	
	LinkedHashMap< Type, LinkedHashSet< Type > > allSupers=
	    new LinkedHashMap< Type, LinkedHashSet< Type > >();
	for (Type t : allUsedTypes()) {
	    if (t.hasConcreteSupertypes() && t.isObject()) {
		LinkedHashSet< Type > supers=new LinkedHashSet< Type >();
		supers.add(t);
		allSupers.put(t,supers);
	    }
	}
	boolean changed=true;
	while (changed) {
	    changed=false;
	    for (Type t : allUsedTypes()) {
		if (t.hasConcreteSupertypes() && t.isObject()) {
		    LinkedHashSet< Type > supers=allSupers.get(t);
		    for (Type t2 : t.supertypes()) {
			changed|=supers.addAll(allSupers.get(t2));
		    }
		}
	    }
	}
        for (Type t : allUsedTypes()) {
            if (t.hasConcreteSupertypes() && t.isObject()) {
                for (Type t2 : allSupers.get(t)) {
                    t2.numDescendants++;
                }
            }
        }
	
	LinkedHashMap< Type, TypeNode > nodes=
	    new LinkedHashMap< Type, TypeNode >();
	for (Type t : allUsedTypes()) {
	    if (t.hasConcreteSupertypes() && t.isObject()) {
		nodes.put(t,new TypeNode(t));
	    }
	}
	
	for (Type t : allUsedTypes()) {
	    if (t.hasConcreteSupertypes() && t.isObject()) {
		ArrayList< TypeNode > curNodes=new ArrayList< TypeNode >();
		for (Type t2 : allSupers.get(t)) {
		    curNodes.add(nodes.get(t2));
		}
		GraphColoring.cluster(curNodes);
	    }
	}
	
	numBuckets=GraphColoring.color(nodes.values())+1;
	
	if (Global.verbosity>=1) {
	    Global.log.println("we have "+numBuckets+" buckets for type inclusion");
	}
	
	// note that this will include Object, and the primitive types, in the foray.
	// that's fine.  it makes subtype-of-object tests fast in the case where
	// you're doing subtype-of-T and you don't know what T is.
	
	ArrayList< Integer > bucketOccupancy=new ArrayList< Integer >();
	int[] bucketReassignment=new int[numBuckets];
	for (int i=0;i<numBuckets;++i) {
	    bucketOccupancy.add(0);
	    bucketReassignment[i]=i;
	}
	for (Type t : allUsedTypes()) {
	    if (t.hasConcreteSupertypes() && t.isObject()) {
		int origBucket=nodes.get(t).color();
		t.bucket=bucketReassignment[origBucket];
		if (bucketOccupancy.get(t.bucket)>=255) {
		    if (Global.verbosity>=1) {
			Global.log.println("Bucket "+t.bucket+" (originally "+origBucket+") overflowed; creating new one.");
		    }
		    int newBucket=bucketOccupancy.size();
		    t.bucket=bucketReassignment[origBucket]=newBucket;
		    bucketOccupancy.add(0);
		}
		bucketOccupancy.set(t.bucket,bucketOccupancy.get(t.bucket)+1);
		t.tid=(byte)(int)bucketOccupancy.get(t.bucket);
	    }
	}
	numBuckets=bucketOccupancy.size();

	if (Global.verbosity>=1) {
	    Global.log.println("we now have "+numBuckets+" buckets for type inclusion");
	}
	
	int numTypes=0;
	for (Type t : allUsedTypes()) {
	    if (t.hasConcreteSupertypes() && t.isObject()) {
		numTypes++;
		t.buckets=new byte[numBuckets];
		for (Type t2 : allSupers.get(t)) {
		    assert t.buckets[t2.bucket]==0;
		    t.buckets[t2.bucket]=t2.tid;
		}
	    }
	}
	
	long after=System.currentTimeMillis();
	
	if (Global.verbosity>=1) {
	    Global.log.println("we have "+numTypes+" types that need buckets");
	    Global.log.println("building type inclusion data took "+(after-before)+" ms");
	}
    }
    
    public static int numBuckets() {
        return numBuckets;
    }
    
    public byte[] buckets() {
        return buckets;
    }
    
    public int bucket() {
        return bucket;
    }
    
    public byte tid() {
        return tid;
    }
    
    public int uniqueID() {
	return uniqueID;
    }

    private static synchronized Type construct(Basetype basetype,
                                               VisibleClass clazz,
                                               int arrayDepth) {
        assert basetype!=Basetype.OBJECT ^ clazz!=null;
        if (willClose) {
            TypeKey k=new TypeKey(basetype,clazz,arrayDepth);
            assert !hashConsMap.containsKey(k) : k;
            if (closed) throw new NoNewTypes("Adding type after typesystem was closed: "+k);
            Type result=new Type(k.basetype,k.clazz,k.arrayDepth);
            hashConsMap.put(k,result);
            if (!result.isBottomish()) {
                knownRealTypes.add(result);
            }
            return result;
        } else {
            haveCreatedTypes=true;
            return new Type(basetype,clazz,arrayDepth);
        }
    }
    
    private static Type construct(Basetype basetype) {
        return construct(basetype,null,0);
    }
    
    static Type make(Basetype basetype,
                     VisibleClass clazz,
                     int arrayDepth) {
	assert basetype!=Basetype.OBJECT ^ clazz!=null;
        Type t;
        if (clazz!=null) {
            synchronized (clazz) {
                if (clazz.myType==null) {
                    clazz.myType=construct(basetype,clazz,0);
                }
                t=clazz.myType;
            }
        } else {
            t=basetype.asType;
        }
        return t.makeArray(arrayDepth);
    }
    
    Type make_(Basetype basetype,
               VisibleClass clazz,
               int arrayDepth) {
	return make(basetype,clazz,arrayDepth);
    }
        
    public static int numTypesCreated() {
        return hashConsMap.size();
    }
    
    public static Type make(Basetype basetype) {
	return make(basetype,null,0);
    }
    
    public static Type[] make(Basetype[] basetypes) {
	Type[] result=new Type[basetypes.length];
	for (int i=0;i<basetypes.length;++i) {
	    result[i]=make(basetypes[i]);
	}
	return result;
    }
    
    public static Type make(Basetype basetype,int arrayDepth) {
	return make(basetype,null,arrayDepth);
    }
    
    public static Type make(VisibleClass clazz) {
	return make(clazz,0);
    }

    public static Type make(VisibleClass clazz,int arrayDepth) {
        if (clazz.unboxed) {
            assert Global.root()!=null;
	    if (clazz==Global.root().pointerClass) {
                return make(Basetype.POINTER,arrayDepth);
            } else if (clazz==Global.root().fcPtrClass) {
                return make(Basetype.VM_FCPTR,arrayDepth);
            } else if (clazz==Global.root().booleanArrClass) {
                return make(Basetype.BOOLEAN,1);
            } else if (clazz==Global.root().byteArrClass) {
                return make(Basetype.BYTE,1);
            } else if (clazz==Global.root().charArrClass) {
                return make(Basetype.CHAR,1);
            } else if (clazz==Global.root().shortArrClass) {
                return make(Basetype.SHORT,1);
            } else if (clazz==Global.root().intArrClass) {
                return make(Basetype.INT,1);
            } else if (clazz==Global.root().longArrClass) {
                return make(Basetype.LONG,1);
            } else if (clazz==Global.root().pointerArrClass) {
                return make(Basetype.POINTER,1);
            } else if (clazz==Global.root().fcPtrArrClass) {
                return make(Basetype.VM_FCPTR,1);
            } else if (clazz==Global.root().floatArrClass) {
                return make(Basetype.FLOAT,1);
            } else if (clazz==Global.root().doubleArrClass) {
                return make(Basetype.DOUBLE,1);
            } else if (clazz==Global.root().objectArrClass) {
                return make(Basetype.OBJECT,Global.root().objectClass,1);
            } else {
                throw new CompilerException("unrecognized unboxed class: "+clazz);
            }
	} else {
	    return make(Basetype.OBJECT,clazz,arrayDepth);
	}
    }
    
    public static Type make(Exectype t) {
        synchronized (t) {
            if (t.cachedType==null) {
                t.cachedType=make(t.basetype,t.clazz,t.arrayDepth);
            }
            return t.cachedType;
        }
    }
    
    public Type makeArray(int arrayDepth) {
        if ((this==VOID ||
             this==NIL ||
             this==NULL) && arrayDepth>0) {
            throw new CompilerException("Cannot have arrays of "+this);
        }
        Type result=this;
        while (arrayDepth-->0) {
            synchronized (result) {
                if (result.cachedArray==null) {
                    result.cachedArray=construct(result.basetype,
                                                 result.clazz,
                                                 result.arrayDepth+1);
                }
                result=result.cachedArray;
            }
        }
        return result;
    }
    
    public boolean arrayTypeCreated() {
        return cachedArray!=null;
    }
    
    public Type arrayTypeIfCreated() {
        return cachedArray;
    }
    
    public Type makeArray() {
	return makeArray(1);
    }

    public Type openArray(int amount) {
	assert arrayDepth-amount>=0
	    : "Array depth = "+arrayDepth+", amount = "+amount+" in "+this;
        Type t=this;
        while (amount-->0) {
            t=arrayElement();
        }
        return t;
    }
    
    public static int skip(String str,int firstIndex) {
        try {
            return TypeParsing.skipType(str,firstIndex);
        } catch (TypeParsing.Failed e) {
            throw new BadBytecode("Bad type descriptor: "+str,e);
        }
    }
    
    public static int skip(String str) {
        return skip(str,0);
    }
    
    public static Type parse(Context c,String str,int firstIndex) {
	try {
	    int i=firstIndex;
	    
	    int arrayDepth=0;
	    while (str.charAt(i)=='[') {
		arrayDepth++;
		i++;
	    }
	    
	    Type result;
	    
            char btc=str.charAt(i++);
            if (btc=='L') {
		int end=str.indexOf(';',i);
		if (end==-1) {
		    throw new BadBytecode(
			"Bad type descriptor in: "+str+
			" (no ';' character at end of reference type)");
		}
		result=make(c.forceClass(str.substring(i,end)),arrayDepth);
		i=end+1;
            } else {
                try {
                    result=Basetype.fromChar(btc).asType.makeArray(arrayDepth);
                } catch (Throwable e) {
                    throw new BadBytecode("Bad basetype character in: "+str,e);
                }
            }
	
	    return result;
	} catch (ResolutionFailed e) {
	    throw e;
	} catch (BadBytecode e) {
	    throw e;
	} catch (Exception e) {
	    throw new BadBytecode(
		"Bad type descriptor in: "+str,e);
	}
    }
    
    public static Type parse(Context c,String str) {
        return parse(c,str,0);
    }

    public static Type parseRefOnly(Context c,String str) {
	if (str.charAt(0)=='[') {
	    return parse(c,str);
	} else {
	    return make(c.forceClass(str));
	}
    }
    
    public Exectype asExectype() { return Exectype.make(this); }
    
    public String mangledName() {
	return Util.hidePunct(simpleName())+"_"+Util.hash(canonicalName());
    }
    
    public static String jniName(Type[] typeArray) {
        StringBuilder buf=new StringBuilder();
	for (Type param : typeArray) {
	    buf.append(param.jniName());
	}
        return buf.toString();
    }
    
    /** Get all of the direct parents of this type.  Note that this returns
	an empty array instead of TOP if you get to a type that doesn't
	otherwise have parents. */
    public Type[] supertypes() {
	if (!hasConcreteSupertypes()) {
	    throw new UndefinedSupertype("For "+this);
	}
	if (basetype==Basetype.OBJECT && !clazz.isObjectRoot()) {
	    Type[] result=new Type[clazz.numDirectSupertypes()];
	    int cur=0;
	    if (clazz.hasSuperclass()) {
		result[cur++]=Type.make(clazz.getSuperclass(),arrayDepth);
	    }
	    for (VisibleClass i : clazz.getSuperInterfaces()) {
		result[cur++]=Type.make(i,arrayDepth);
	    }
	    return result;
	}
	if (arrayDepth>0) {
	    Type[] result=new Type[3];
	    result[0]=Type.make(Global.root().objectClass,arrayDepth-1);
	    result[1]=Type.make(Global.root().serializableClass,arrayDepth-1);
	    result[2]=Type.make(Global.root().cloneableClass,arrayDepth-1);
	    return result;
	}
	return Type.EMPTY;
    }
    
    public Type[] superInterfaces() {
        Type[] supertypes=supertypes();
        if (supertypes.length==0) {
            return Type.EMPTY;
        } else {
            Type supertype=supertype();
            Type[] result=new Type[supertypes.length-1];
            int cnt=0;
            for (Type t : supertypes) {
                if (t!=supertype) {
                    result[cnt++]=t;
                }
            }
            assert cnt==result.length;
            return result;
        }
    }
    
    public ArrayList< Type > directSubtypes() {
        assert willClose;
        assert closed;
        ArrayList< Type > result=new ArrayList< Type >();
        if (basetype==Basetype.OBJECT) {
            
            // check if we're an array supertype
            if (clazz==Global.root().objectClass ||
                clazz==Global.root().serializableClass ||
                clazz==Global.root().cloneableClass) {
                for (Type t : fundamentalFundamentals) {
                    try {
                        result.add(t.makeArray(arrayDepth+1));
                    } catch (NoNewTypes e) {}
                }
            }
            
            // add all class subtypes
            for (VisibleClass c : clazz.knownDirectSubs) {
                try {
                    result.add(c.asType().makeArray(arrayDepth));
                } catch (NoNewTypes e) {}
            }
        }
        return result;
    }
    
    public ArrayList< Type > directUsedSubtypes() {
        ArrayList< Type > result=new ArrayList< Type >();
        for (Type t : directSubtypes()) {
            if (t.isUsed()) {
                result.add(t);
            }
        }
        return result;
    }
    
    public Type lub(Type other) {
	if (isSubtypeOf(other)) {
	    return other;
	}
	if (other.isSubtypeOf(this)) {
	    return this;
	}
	return Type.make(asExectype().lub(other.asExectype()));
    }
    
    public boolean isAssignableFrom(Type other) {
        if (other.isSubtypeOf(this)) {
            return true;
        }
        if (isObject() || other.isObject()) {
            return false;
        }
        return effectiveBasetype().isAssignableFrom(other.effectiveBasetype());
    }
    
    public boolean isDeclaredFinal() {
        switch (basetype) {
        case TOP:
            return false;
        case OBJECT:
            return clazz.isFinal();
        default:
            return true;
        }
    }
    
    public boolean isFinal() {
	if (knownFinal) {
	    return true;
	} else {
            return isDeclaredFinal();
	}
    }
    
    public TypeBoundMode typeBoundMode() {
	if (unresolved()) return TypeBoundMode.UPPER_BOUND;
	else if (isFinal()) return TypeBoundMode.EXACT;
	else return TypeBoundMode.UPPER_BOUND;
    }
    
    public boolean hasInstances() {
	return Global.analysis().hasInstances(this);
    }
    
    public boolean mapsDirectlyToClass() {
        return hasEffectiveClass() && effectiveClass().asType()==this;
    }
    
    public static Type VOID;
    public static Type BOOLEAN;
    public static Type BYTE;
    public static Type CHAR;
    public static Type SHORT;
    public static Type INT;
    public static Type LONG;
    public static Type FLOAT;
    public static Type DOUBLE;
    
    public static Type NULL;
    public static Type TOP;
    public static Type BOTTOM;
    public static Type NIL;
    
    public static Type POINTER;
    public static Type VM_FCPTR;
    
    private static Type initBasetype(Basetype basetype) {
        Type result=construct(basetype);
        basetype.asType=result;
        return result;
    }
    
    static {
	hashConsMap = new HashMap< TypeKey, Type >();
        knownRealTypes = new ArrayList< Type >();

        willClose=true;
        
	VOID    = initBasetype(Basetype.VOID);
	BOOLEAN = initBasetype(Basetype.BOOLEAN);
	BYTE    = initBasetype(Basetype.BYTE);
	CHAR    = initBasetype(Basetype.CHAR);
	SHORT   = initBasetype(Basetype.SHORT);
	INT     = initBasetype(Basetype.INT);
	LONG    = initBasetype(Basetype.LONG);
	FLOAT   = initBasetype(Basetype.FLOAT);
	DOUBLE  = initBasetype(Basetype.DOUBLE);
	
	NULL = initBasetype(Basetype.NULL);
	TOP = initBasetype(Basetype.TOP);
	BOTTOM = initBasetype(Basetype.BOTTOM);
        NIL = initBasetype(Basetype.NIL);
	
	POINTER = initBasetype(Basetype.POINTER);
	VM_FCPTR = initBasetype(Basetype.VM_FCPTR);

        willClose=false;
        haveCreatedTypes=false;
    }
    
    public static Type[] EMPTY = new Type[0];

    private static Type[] fundamentalFundamentals;
    private static ArrayList< Type > fundamentalsInOrder=null;
    private static HashSet< Type > fundamentals=null;
    
    public static void initFundamentals() {
        assert fundamentals==null;
        
        // this mimics the order of td_XXX fields in fivmr_Payload
        
        fundamentalFundamentals=new Type[]{
            Type.BOOLEAN,
            Type.BYTE,
            Type.CHAR,
            Type.SHORT,
            Type.INT,
            Type.LONG,
            Type.FLOAT,
            Type.DOUBLE,
            Type.POINTER,
            Type.VM_FCPTR,
            Global.root().objectType,
            Global.root().stringType,
            Global.root().classType,
        };
        fundamentalsInOrder=new ArrayList< Type >();
        fundamentals=new HashSet< Type >();
        fundamentalsInOrder.add(Type.TOP);
        fundamentalsInOrder.add(Type.VOID);
        for (Type t : fundamentalFundamentals) {
            fundamentalsInOrder.add(t);
        }
        fundamentalsInOrder.add(Global.root().serializableClass.asType());
        fundamentalsInOrder.add(Global.root().cloneableClass.asType());
        if (Runtime.field!=null)       fundamentalsInOrder.add(Runtime.field.asType());
        else                           fundamentalsInOrder.add(null);
        if (Runtime.method!=null)      fundamentalsInOrder.add(Runtime.method.asType());
        else                           fundamentalsInOrder.add(null);
        if (Runtime.constructor!=null) fundamentalsInOrder.add(Runtime.constructor.asType());
        else                           fundamentalsInOrder.add(null);
        fundamentalsInOrder.add(Runtime.weakReference.asType());
        for (Type t : fundamentalFundamentals) {
            fundamentalsInOrder.add(t.makeArray());
        }
        
        for (Type t : fundamentalsInOrder) {
            if (t!=null) {
                fundamentals.add(t);
            }
        }
        
        Linkable.initPredeclared();
    }
    
    public static ArrayList< Type > fundamentalsInOrder() {
        assert fundamentalsInOrder!=null;
	return fundamentalsInOrder;
    }
    
    public static HashSet< Type > fundamentals() {
        assert fundamentals!=null;
	return fundamentals;
    }
}

