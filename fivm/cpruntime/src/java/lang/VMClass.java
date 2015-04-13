package java.lang;

import static com.fiji.fivm.Constants.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

import static java.lang.fivmSupport.typeDataFromClass;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.fiji.fivm.r1.Pointer;

final class VMClass {
    private VMClass() {}
    
    // FIXME this is incomplete.  look for the 'FIXME's to see what needs
    // work.
    
    static boolean isInstance(Class<?> clazz,Object o) {
	return FCClass.isInstance(clazz, o);
    }
    
    static boolean isAssignableFrom(Class<?> klass,
				    Class<?> c) {
	return FCClass.isAssignableFrom(klass, c);
    }
    
    static boolean isInterface(Class<?> klass) {
	return FCClass.isInterface(klass);
    }
    
    static boolean isPrimitive(Class<?> klass) {
	return FCClass.isPrimitive(klass);
    }
    
    static String getName(Class<?> klass) {
	return FCClass.getName(klass);
    }
    
    static Class<?> getSuperclass(Class<?> klass) {
	return FCClass.getSuperclass(klass);
    }
    
    static Class<?>[] getInterfaces(Class<?> klass) {
	return FCClass.getInterfaces(klass);
    }
    
    static Class<?> getComponentType(Class<?> klass) {
	return FCClass.getComponentType(klass);
    }
    
    static int getModifiers(Class<?> clazz, boolean ignoreInnerClassesAttrib) {
	return FCClass.getModifiers(clazz, ignoreInnerClassesAttrib);
    }
    
    static Class<?> getDeclaringClass(Class<?> clazz) {
	// FIXME.  there's a better way.  but I don't feel like implementing
	// it right now. :-)
	if (clazz.isArray() || clazz.isPrimitive()) return null;
	String name=clazz.getName();
	int dollar=name.indexOf('$');
	int dot=name.lastIndexOf('.');
	if (dollar<0 || dollar<dot) {
	    return null;
	} else {
	    try {
		return Class.forName(name.substring(0,dollar));
	    } catch (Throwable e) {
		throw new Error(e);
	    }
	}
    }
    
    static Class<?> getEnclosingClass(Class<?> clazz) {
	// FIXME.  there's a better way.  but I don't feel like implementing
	// it right now. :-)
	if (clazz.isArray() || clazz.isPrimitive()) return null;
	String name=clazz.getName();
	int dollar=name.lastIndexOf('$');
	int dot=name.lastIndexOf('.');
	if (dollar<0 || dollar<dot) {
	    return null;
	} else {
	    try {
		return Class.forName(name.substring(0,dollar));
	    } catch (Throwable e) {
		throw new Error(e);
	    }
	}
    }
    
    static Class<?>[] getDeclaredClasses(Class<?> clazz,boolean publicOnly) {
	return new Class[0]; // FIXME
    }
    
    static Field[] getDeclaredFields(Class<?> clazz,boolean publiconly) {
	Pointer td=typeDataFromClass(clazz);
	Field[] result=new Field[fivmr_TypeData_numFields(td)];
	for (int i=0;i<fivmr_TypeData_numFields(td);++i) {
	    result[i]=
		java.lang.reflect.fivmSupport.newField(
		    fivmr_TypeData_field(td,i));
	}
	return result;
    }
    
    static Method[] getDeclaredMethods(Class<?> clazz,boolean publicOnly) {
	Pointer td=typeDataFromClass(clazz);
	int nmethods=0;
	for (int i=0;i<fivmr_TypeData_numMethods(td);++i) {
	    if (!fivmr_MethodRec_isInitializer(fivmr_TypeData_method(td,i))) {
		nmethods++;
	    }
	}
	Method[] result=new Method[nmethods];
	int cnt=0;
	for (int i=0;i<fivmr_TypeData_numMethods(td);++i) {
	    if (!fivmr_MethodRec_isInitializer(fivmr_TypeData_method(td,i))) {
		result[cnt++]=
		    java.lang.reflect.fivmSupport.newMethod(
			fivmr_TypeData_method(td,i));
	    }
	}
	return result;
    }
    
    static Constructor<?>[] getDeclaredConstructors(Class<?> clazz,boolean publicOnly) {
	Pointer td=typeDataFromClass(clazz);
	int nctors=0;
	for (int i=0;i<fivmr_TypeData_numMethods(td);++i) {
	    if (fivmr_MethodRec_isConstructor(fivmr_TypeData_method(td,i))) {
		nctors++;
	    }
	}
	Constructor<?>[] result=new Constructor[nctors];
	int cnt=0;
	for (int i=0;i<fivmr_TypeData_numMethods(td);++i) {
	    if (fivmr_MethodRec_isConstructor(fivmr_TypeData_method(td,i))) {
		result[cnt++]=
		    java.lang.reflect.fivmSupport.newConstructor(
			fivmr_TypeData_method(td,i));
	    }
	}
	return result;
    }
    
    static ClassLoader getClassLoader(Class<?> clazz) {
	return FCClass.getClassLoader(clazz);
    }
    
    static Class<?> forName(String name, boolean initialize,
			    ClassLoader loader)
	throws ClassNotFoundException {
	return FCClass.forName(name, initialize, loader);
    }
    
    static boolean isArray(Class<?> clazz) {
	return FCClass.isArray(clazz);
    }
    
    static void throwException(Throwable t) {
	FCClass.throwException(t);
    }
    
    static String getSimpleName(Class<?> clazz) {
	return FCClass.getSimpleName(clazz);
    }
    
    static String getCanonicalName(Class<?> clazz) {
	return null; // FIXME
    }
    
    static Annotation[] getDeclaredAnnotations(Class<?> clazz) {
	return null; // FIXME
    }
    
    static Constructor<?> getEnclosingConstructor(Class<?> clazz) {
	return null; // FIXME
    }
    
    static Method getEnclosingMethod(Class<?> clazz) {
	return null; // FIXME
    }
    
    static String getClassSignature(Class<?> clazz) {
	return null; // FIXME
    }
    
    static boolean isAnonymousClass(Class<?> clazz) {
	// FIXME this is wrong.
	String name=clazz.getName();
	return !clazz.isArray()
	    && !clazz.isPrimitive()
	    && Character.isDigit(name.charAt(name.lastIndexOf('$')+1));
    }
    
    static boolean isLocalClass(Class<?> clazz) {
	// FIXME this is SOOO wrong
	return isAnonymousClass(clazz);
    }
    
    static boolean isMemberClass(Class<?> clazz) {
	return getEnclosingClass(clazz)!=null;
    }
}

