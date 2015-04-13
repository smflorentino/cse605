package java.lang.reflect;

import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.Magic.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import static java.lang.fivmSupport.*;
import java.lang.annotation.*;

public class Constructor< T >
    extends AccessibleObject
    implements AnnotatedElement, GenericDeclaration, Member {
    
    Pointer mr;
    
    Constructor(Pointer mr) {
	this.mr=mr;
	fivmr_ReflectLog_dynamicCallReflect(curThreadState(),mr);
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof Constructor<?>)) return false;
	Constructor<?> other=(Constructor<?>)other_;
	return mr==other.mr;
    }
    
    public < U extends Annotation > U getAnnotation(Class< U > annotationClass) {
	return null; // FIXME
    }
    
    public Annotation[] getDeclaredAnnotations() {
	return new Annotation[0]; // FIXME
    }

    @SuppressWarnings("unchecked")
    public Class< T > getDeclaringClass() {
	return (Class< T >)fivmr_TypeData_asClass(fivmr_MethodRec_owner(mr));
    }
    
    public Class< ? >[] getExceptionTypes() {
	return EMPTY_CA;
    }
    
    public Type[] getGenericExceptionTypes() {
	return new Type[0];
    }
    
    public int getModifiers() {
	return methodModifiersForFlags(fivmr_MethodRec_flags(mr));
    }
    
    public String getName() {
	return getDeclaringClass().getSimpleName();
    }
    
    public Class< ? >[] getParameterTypes() {
	return fivmRuntime.getParameterTypes(mr);
    }
    
    public Type[] getGenericParameterTypes() {
	return new Type[0];
    }
    
    public int hashCode() {
	return mr.castToInt();
    }
    
    public boolean isSynthetic() {
	return false;
    }
    
    public boolean isVarArgs() {
	return false; // FIXME
    }
    
    @SuppressWarnings("unchecked")
    public T newInstance(Object... initargs)
	throws InvocationTargetException,
	       InstantiationException {
	
	Pointer td=fivmr_MethodRec_owner(mr);
	
	if (!isClass(td)) {
	    throw new fivmError("Trying to use Constructor to instantiate something "+
				"that isn't even a class, interface, or annotation: "+
				fromCStringFull(fivmr_TypeData_name(td))+", using "+
				"constructor "+
				fromCStringFull(fivmr_MethodRec_describe(mr)));
	}
	
	if (isAbstract(td)) {
	    throw new InstantiationException(
		"Attempt to instantiate abstract class or interface "+
		fivmr_TypeData_asClass(td));
	}
	
	fivmr_ReflectLog_allocReflect(curThreadState(),td);
	
	Object result=MM.alloc(curAllocSpace(),td);
	
	try {
	    reflectiveCall(mr,result,initargs);
	} catch (ReflectiveException e) {
	    throw new InvocationTargetException(e.getCause());
	}
	
	return (T)result;
    }
    
    public String toGenericString() {
	return toString(); // FIXME
    }
    
    public String toString() {
	return reflectiveMethodToString(mr);
    }
    
    @SuppressWarnings("unchecked")
    public TypeVariable< Constructor< T > >[] getTypeParameters() {
	return new TypeVariable[0];
    }
    
    public static Class<?>[] EMPTY_CA=new Class<?>[0];
}


