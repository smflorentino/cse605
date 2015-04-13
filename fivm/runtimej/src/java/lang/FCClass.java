/*
 * FCClass.java
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

package java.lang;

import static com.fiji.fivm.Constants.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.r1.Magic.*;

import static java.lang.fivmSupport.typeDataFromClass;

import java.io.Serializable;
import java.lang.reflect.Modifier;

import com.fiji.fivm.r1.*;

final class FCClass {
    private FCClass() {}
    
    // FIXME this is incomplete.  look for the 'FIXME's to see what needs
    // work.
    
    static boolean isInstance(Class<?> clazz,Object o) {
	fivmr_ReflectLog_useReflect(curThreadState(),typeDataFromClass(clazz));
	return fivmr_TypeData_isSubtypeOf(
            curThreadState(),
	    fivmr_TypeData_forObject(o),
	    typeDataFromClass(clazz));
    }
    
    static boolean isAssignableFrom(Class<?> klass,
				    Class<?> c) {
	return fivmr_TypeData_isSubtypeOf(
            curThreadState(),
	    typeDataFromClass(c),
	    typeDataFromClass(klass));
    }
    
    static boolean isInterface(Class<?> klass) {
	return fivmr_TypeData_isInterface(typeDataFromClass(klass));
    }
    
    static boolean isPrimitive(Class<?> klass) {
	return fivmr_TypeData_isPrimitive(typeDataFromClass(klass));
    }
    
    static String getName(Class<?> klass) {
	Pointer td=typeDataFromClass(klass);
	Pointer cname=fivmr_TypeData_name(td);
        if (false) {
            System.out.println("checking if "+fromCStringFull(cname)+" is primitive");
        }
        boolean isPrimitive=fivmr_TypeData_isPrimitive(td);
        if (false) {
            System.out.println("result = "+isPrimitive);
        }
	if (isPrimitive) {
	    switch ((char)cname.loadByte()) {
	    case 'Z': return "boolean";
	    case 'B': return "byte";
	    case 'C': return "char";
	    case 'I': return "int";
	    case 'J': return "long";
	    case 'F': return "float";
	    case 'D': return "double";
	    case 'P': return "pointer";
	    case 'V': return "void";
	    default: throw abort("bad primitive type name");
	    }
	} else {
	    String result=fromCStringFull(cname).replace('/','.');
	    if (fivmr_TypeData_isArray(td)) {
		return result;
	    } else {
		return result.substring(1,result.length()-1);
	    }
	}
    }
    
    static Class<?> getSuperclass(Class<?> klass) {
	if (klass.isPrimitive() || klass==Object.class) {
	    return null;
	} else {
	    return fivmr_TypeData_asClass(
		fivmr_TypeData_parent(typeDataFromClass(klass)));
	}
    }
    
    static Class<?>[] emptyInterfaces=new Class<?>[0];
    static Class<?>[] arrayInterfaces=new Class<?>[]{Cloneable.class, Serializable.class};
    static Class<?>[] getInterfaces(Class<?> klass) {
	Pointer myTD=typeDataFromClass(klass);
	if (fivmr_TypeData_isPrimitive(myTD)) {
	    return emptyInterfaces;
	} else if (fivmr_TypeData_isArray(myTD)) {
	    return arrayInterfaces;
	} else {
	    Class<?>[] result=new Class<?>[
		fivmr_TypeData_nSuperInterfaces(myTD)];
	    for (int i=0;i<result.length;++i) {
		result[i]=fivmr_TypeData_asClass(
		    fivmr_TypeData_getSuperInterface(myTD,i));
	    }
	    return result;
	}
    }
    
    static Class<?> getComponentType(Class<?> klass) {
	Pointer result=fivmr_TypeData_arrayElement(typeDataFromClass(klass));
	if (result==Pointer.zero()) {
	    return null;
	} else {
	    return fivmr_TypeData_asClass(result);
	}
    }
    
    static int getModifiers(Class<?> clazz, boolean ignoreInnerClassesAttrib) {
	// FIXME: we currently always ignore the inner class attribute
	int myFlags=fivmr_TypeData_flags(typeDataFromClass(clazz));
	if ((myFlags&TBF_TYPE_KIND)==TBF_ARRAY) {
	    return (getModifiers(clazz.getComponentType(),
				 ignoreInnerClassesAttrib) | Modifier.FINAL)
		&~ Modifier.INTERFACE;
	} else {
	    int result=genericModifiersForFlags(myFlags);
	    
	    switch (myFlags&TBF_TYPE_KIND) {
	    case TBF_PRIMITIVE: result|=Modifier.FINAL; break;
	    case TBF_ANNOTATION: break;
	    case TBF_INTERFACE: result|=Modifier.INTERFACE|Modifier.ABSTRACT; break;
	    case TBF_ABSTRACT: result|=Modifier.ABSTRACT; break;
	    case TBF_VIRTUAL: break;
	    case TBF_FINAL: result|=Modifier.FINAL; break;
	    default: abort("bad value of type kind in flags");
	    }
	    
	    return result;
	}
    }

    @NoPollcheck
    static ClassLoader getClassLoader(Class<?> clazz) {
	return getClassLoaderForContext(
            fivmr_TypeData_getContext(
                fivmSupport.typeDataFromClass(
                    clazz)));
    }
    
    static Class<?> forName(String name, boolean initialize,
			    ClassLoader loader)
	throws ClassNotFoundException {
        Class<?> result=null;
        if (loader==null) {
            Pointer td=findKnownType(baseTypeContext(0),
                                     "L"+name.replace('.','/')+";");
            if (td!=Pointer.zero()) {
                result=fivmr_TypeData_asClass(td);
            }
        } else {
            result=loader.loadClass(name);
        }
	if (result==null) {
	    throw new ClassNotFoundException(name);
	}
	if (initialize) {
            resolveAndCheckInit(result);
	}
	return result;
    }
    
    static boolean isArray(Class<?> clazz) {
	return fivmr_TypeData_isArray(typeDataFromClass(clazz));
    }
    
    static void throwException(Throwable t) {
	uncheckedThrow(t);
    }
    
    static String getSimpleName(Class<?> clazz) {
	// FIXME: this is broken and I don't care.
	Pointer td=typeDataFromClass(clazz);
	Pointer cname=fivmr_TypeData_name(td);
	if (fivmr_TypeData_isPrimitive(td)) {
	    switch ((char)cname.loadByte()) {
	    case 'Z': return "boolean";
	    case 'B': return "byte";
	    case 'C': return "char";
	    case 'I': return "int";
	    case 'J': return "long";
	    case 'F': return "float";
	    case 'D': return "double";
	    case 'P': return "pointer";
	    case 'V': return "void";
	    default: throw abort("bad primitive type name");
	    }
	} else if (fivmr_TypeData_isArray(td)) {
	    return clazz.getComponentType().getSimpleName()+"[]";
	} else {
	    String result=fromCStringFull(cname).replace('/','.');
	    return result.substring(1,result.length()-1);
	}
    }
    
    @NoInline
    static void throwInstantiationException(Class<?> klass) throws InstantiationException {
        throw new InstantiationException("no <init>()V method in "+klass);
    }
    
    @NoInline
    @StackAllocation
    static Object newInstance(Class<?> klass) throws InstantiationException {
	// FIXME: epic fail, this will be sooooo slow!
	
	Pointer td=typeDataFromClass(klass);
        
        resolveAndCheckInit(td);
        
	Pointer mr=
	    fivmr_TypeData_findMethod(
                getVM(),
		td,
		getCStringFullStack("<init>"),
		getCStringFullStack("()V"));
	if (mr==Pointer.zero()) {
            throwInstantiationException(klass);
	}
	
	fivmr_ReflectLog_alloc(curThreadState(),2,td);
	fivmr_ReflectLog_dynamicCall(curThreadState(),2,mr);
	
	Object result=MM.alloc(GC_OBJ_SPACE,td);
	
	try {
	    reflectiveCall(mr,result,new Object[0]);
	} catch (ReflectiveException e) {
	    uncheckedThrow(e.getCause());
	}
	
	return result;
    }
}

