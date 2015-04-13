/*
 * TypeImpl.java
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

import com.fiji.fivm.Constants;

public abstract class TypeImpl < T extends TypeImpl< T > >
    implements Contextable, JNINameable {

    Basetype basetype;
    VisibleClass clazz; // null if basetype!=OBJECT
    int arrayDepth;
    
    Type arrayElementCached;
    
    TypeImpl(Basetype basetype,VisibleClass clazz,int arrayDepth) {
	this.basetype=basetype;
	this.clazz=clazz;
	this.arrayDepth=arrayDepth;
    }
    
    abstract T make_(Basetype basetype,
                     VisibleClass clazz,
                     int arrayDepth);
    
    public Basetype getUnderlyingBasetype() {
	return basetype;
    }
    
    public boolean isBottomish() {
        return basetype.isBottomish;
    }
    
    public boolean isArray() {
	return arrayDepth!=0;
    }
    
    public int getArrayDepth() {
	return arrayDepth;
    }
    
    public boolean hasUnderlyingClass() {
	return basetype==Basetype.OBJECT;
    }
    
    public VisibleClass getUnderlyingClass() {
	assert basetype==Basetype.OBJECT;
	return clazz;
    }
    
    public Basetype effectiveBasetype() {
	if (arrayDepth>0 || basetype==Basetype.NULL) {
	    return Basetype.OBJECT;
	} else {
	    return basetype;
	}
    }
    
    public boolean hasClass() {
	return basetype==Basetype.OBJECT && arrayDepth==0;
    }
    
    public VisibleClass getClazz() {
	assert arrayDepth==0 && basetype==Basetype.OBJECT;
	return clazz;
    }
    
    public Context getContext() {
        if (hasUnderlyingClass()) {
            return getUnderlyingClass().getContext();
        } else {
            return Global.root();
        }
    }
    
    public String toString() {
	if (isArray()) return "Array["+arrayElement().toString()+"]";
	else if (hasClass()) return clazz.toString();
	else return basetype.toString();
    }
    
    public String canonicalName() {
	if (isArray()) return "Array["+arrayElement().canonicalName()+"]";
	else if (hasClass()) return clazz.canonicalName();
	else return basetype.toString();
    }
    
    public String javaName() {
	if (isArray()) return arrayElement().javaName()+"[]";
	if (hasClass()) return clazz.getName().replace('/','.');
	return basetype.toString();
    }
    
    public String simpleName() {
	if (isArray()) return arrayElement().simpleName()+"Arr";
	if (hasClass()) return clazz.simpleName();
	return basetype.toString();
    }
    
    public String jniName() {
	if (isArray()) return "["+arrayElement().jniName();
	if (hasClass()) return "L"+clazz.getName()+";";
	return ""+basetype.descriptor;
    }
    
    public String shortName() {
        return getContext().description()+":"+jniName();
    }
    
    public boolean isObjectRoot() {
	return arrayDepth==0 && basetype==Basetype.OBJECT && clazz.isObjectRoot();
    }
    
    public boolean isObject() {
	return effectiveBasetype()==Basetype.OBJECT;
    }
    
    public boolean mayBeObject() {
	return isObject() || basetype==Basetype.BOTTOM;
    }
    
    public boolean isPrimitive() {
	return !isObject();
    }
    
    public boolean isRealObject() {
	return effectiveBasetype()==Basetype.OBJECT && basetype!=Basetype.NULL;
    }
    
    // don't arrays effectively have their own "class" that have
    // their own methods for a bunch of random crap?  no.  that's handled by
    // other magic.  like, Object.toString() calls into the VM to get the
    // stringification.
    public VisibleClass effectiveClass() {
        if (isArray()) {
            assert arrayDepth>=1;
            if (arrayDepth>1) {
                assert arrayDepth>=2;
                return Global.root().objectArrClass;
            } else {
                assert arrayDepth==1;
                switch (basetype) {
                case BOOLEAN:  return Global.root().booleanArrClass;
                case BYTE:     return Global.root().byteArrClass;
                case CHAR:     return Global.root().charArrClass;
                case SHORT:    return Global.root().shortArrClass;
                case INT:      return Global.root().intArrClass;
                case LONG:     return Global.root().longArrClass;
                case POINTER:  return Global.root().pointerArrClass;
                case VM_FCPTR: return Global.root().fcPtrArrClass;
                case FLOAT:    return Global.root().floatArrClass;
                case DOUBLE:   return Global.root().doubleArrClass;
                case OBJECT:   return Global.root().objectArrClass;
                default: throw new Error("unrecognized basetype: "+basetype+" for type: "+this);
                }
            }
        } else if (effectiveBasetype().isUnboxedType) {
	    return effectiveBasetype().getUnboxedClass();
	} else {
	    assert isObject();
	    if (hasClass()) return clazz;
	    else return Global.root().objectClass;
	}
    }
    
    public boolean hasEffectiveClass() {
	return effectiveBasetype().isUnboxedType
	    || isObject();
    }
    
    public ClassBound getClassBound(TypeBoundMode mode) {
	assert isObject();
        return effectiveClass().bound(mode);
    }
    
    public ClassBound getClassBound() {
	return getClassBound(TypeBoundMode.UPPER_BOUND);
    }
    
    public boolean mayBeArray() {
	return isArray() || isObjectRoot();
    }

    // these next two methods return Type instead of Exectype, because
    // Type governs what is in the heap, not Exectype, which only governs
    // what is in locals
    public Type arrayBase() {
	if (arrayDepth==0) {
	    return Type.TOP;
	} else {
	    return Type.make(basetype,clazz,0);
	}
    }
    
    public Type arrayElement() {
        assert arrayDepth>0;
	if (arrayElementCached==null) {
            arrayElementCached=Type.make(basetype,clazz,arrayDepth-1);
        }
        return arrayElementCached;
    }
    
    public boolean hasConcreteSupertypes() {
	switch (basetype) {
	case NULL:
	case BOTTOM:
	    return false;
	case OBJECT:
	    return clazz.resolved();
	default:
	    return true;
	}
    }
    
    public boolean resolved() {
	if (basetype==Basetype.OBJECT) {
	    return clazz.resolved();
	} else {
	    return true;
	}
    }
    
    public ResolvedState resolvedState() {
        if (resolved()) {
            return ResolvedState.RESOLVED;
        } else {
            return ResolvedState.UNRESOLVED;
        }
    }
    
    @SuppressWarnings("unchecked")
    public T checkResolved() {
	if (basetype==Basetype.OBJECT) {
	    clazz.checkResolved();
	}
        return (T)this;
    }
    
    public boolean unresolved() {
	return !resolved();
    }
    
    public void assertResolved() {
	if (unresolved()) {
	    throw new ResolutionFailed(
                getUnderlyingClass(),
                getUnderlyingClass().getResolutionID(),
                "Invalid use of unresolved type");
	}
    }
    
    public T supertype() {
	assertResolved();
	if (!hasConcreteSupertypes()) {
	    throw new UndefinedSupertype("For "+this);
	}
	if (basetype==Basetype.OBJECT && clazz.hasSuperclass()) {
	    return make_(basetype,clazz.getSuperclass(),arrayDepth);
	}
	if (arrayDepth>0) {
	    return make_(Basetype.OBJECT,Global.root().objectClass,arrayDepth-1);
	}
	return make_(Basetype.TOP,null,0);
    }
    
    public boolean isSubtypeOf(T other) {
	if (this==other) {
	    return true;
	}
	if (other.basetype==Basetype.TOP) {
	    return true;
	}
	if (basetype==Basetype.BOTTOM) {
	    return true;
	}
	if (basetype==Basetype.NULL) {
	    return other.effectiveBasetype()==Basetype.OBJECT;
	}
	if (isArray()) {
	    return (arrayDepth>other.arrayDepth &&
		    other.clazz!=null &&
		    other.clazz.isArraySupertype())
		|| (arrayDepth==other.arrayDepth &&
		    arrayBase().isSubtypeOf(other.arrayBase()));
	}
	if (other.isArray()) {
	    return false;
	}
	if (basetype!=other.basetype) {
	    return false;
	}
	if (basetype==Basetype.OBJECT) {
	    if (resolved()) {
		if (other.resolved()) {
		    return clazz.isSubclassOf(other.clazz);
		} else {
		    throw new ResolutionFailed(
                        other.getUnderlyingClass(),
                        other.getUnderlyingClass().getResolutionID(),
			"Cannot determine if resolved type "+this.jniName()+" is a subtype of "+
			"unresolved type "+other.jniName());
		}
	    } else {
		if (other.resolved()) {
		    if (other.isObjectRoot()) {
			return true;
		    } else {
			throw new ResolutionFailed(
                            this.getUnderlyingClass(),
                            this.getUnderlyingClass().getResolutionID(),
			    "Cannot determine if unresolved type "+this.jniName()+
			    " is a subtype of non-root resolved type "+other.jniName());
		    }
		} else {
		    throw new ResolutionFailed(
                        this.getUnderlyingClass(),
                        this.getUnderlyingClass().getResolutionID(),
			"Cannot determine if unresolved type "+this.jniName()+
			" is a subtype of unresolved type "+other.jniName());
		}
	    }
	}
	return true;
    }
    
    public boolean isStrictSubtypeOf(T other) {
        return this!=other && isSubtypeOf(other);
    }
    
    public CType cType() {
	return effectiveBasetype().cType;
    }
    
    public TraceMode defaultTraceMode() {
	if (isObject()) { 
	    return TraceMode.TRACED;
	} else {
	    return TraceMode.NOT_A_REFERENCE;
	}
    }
    
    public int runtimeFlags() {
        int flags=Constants.TBF_RESOLUTION_DONE;
	if (unresolved()) {
	    flags|=Constants.TBF_STUB;
	} else if (hasClass()) {
	    flags|=getClazz().runtimeFlags();
	} else {
	    // is this right?
	    flags|=Visibility.PUBLIC.runtimeFlags();
	    if (isPrimitive()) {
		flags|=Constants.TBF_PRIMITIVE|Constants.TBF_OVERRIDE_ALL;
	    } else if (isArray()) {
		flags|=Constants.TBF_ARRAY;
	    } else {
		throw new Error("unrecognized type for runtime flags: "+this);
	    }
	}
        return flags;
    }
    
    public int valueOfRuntimeInitedField() {
        if (unresolved()) {
            return 0;
        } else if (hasClass()) {
            return getClazz().valueOfRuntimeInitedField();
        } else {
            return 1;
        }
    }
    
    public Arg makeConst(int value) {
        return effectiveBasetype().makeConst(value);
    }
    
    public boolean isInteger() {
        return effectiveBasetype().isInteger;
    }
    
    public boolean isFloat() {
        return effectiveBasetype().isFloat;
    }
    
    public int cells() {
        return effectiveBasetype().cells;
    }
    
    public int bytes() {
        return effectiveBasetype().bytes;
    }
}

