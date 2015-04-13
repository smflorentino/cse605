package java.lang.reflect;

import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.r1.Magic.*;

import java.lang.annotation.Annotation;

import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.ReflectiveException;
import com.fiji.fivm.r1.fivmRuntime;

public class Method
    extends AccessibleObject
    implements AnnotatedElement,
               GenericDeclaration,
               Member {
    
    Pointer mr;
    
    Method(Pointer mr) {
	this.mr=mr;
	fivmr_ReflectLog_dynamicCallReflect(curThreadState(),mr);
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof Method)) return false;
	Method other=(Method)other_;
	return mr==other.mr;
    }
    
    public < T extends Annotation > T getAnnotation(Class< T > annotationClass) {
	return null; // FIXME
    }
    
    public Annotation[] getDeclaredAnnotations() {
	return new Annotation[0]; // FIXME
    }
    
    public Class< ? > getDeclaringClass() {
	return fivmr_TypeData_asClass(fivmr_MethodRec_owner(mr));
    }
    
    public Object getDefaultValue() {
	return null; // FIXME
    }
    
    public Class< ? >[] getExceptionTypes() {
	return new Class[0]; // FIXME
    }
    
    public Type[] getGenericExceptionTypes() {
	return new Type[0]; // FIXME
    }
    
    public Type[] getGenericParameterTypes() {
	return new Type[0]; // FIXME
    }
    
    public Type getGenericReturnType() {
	return null; // FIXME
    }
    
    public int getModifiers() {
	return methodModifiersForFlags(fivmr_MethodRec_flags(mr));
    }
    
    public String getName() {
	return fromCStringFull(fivmr_MethodRec_name(mr));
    }
    
    public Annotation[][] getParameterAnnotations() {
	return new Annotation[fivmr_MethodRec_nparams(mr)][0];
    }
    
    public Class< ? >[] getParameterTypes() {
	return fivmRuntime.getParameterTypes(mr);
    }
    
    public Class< ? > getReturnType() {
        return getResultType(mr);
    }
    
    @SuppressWarnings("unchecked")
    public TypeVariable< Method >[] getTypeParameters() {
	return new TypeVariable[0]; // FIXME
    }
    
    public int hashCode() {
	return mr.castToInt();
    }
    
    public boolean isBridge() {
	return false; // FIXME
    }
    
    public boolean isVarArgs() {
	return false; // FIXME
    }
    
    public boolean isSynthetic() {
	return false; // FIXME
    }
    
    public String toString() {
	return reflectiveMethodToString(mr);
    }
    
    public String toGenericString() {
	return toString(); // FIXME
    }
    
    public Object invoke(Object obj,Object... args)
	throws IllegalArgumentException,
	       InvocationTargetException {
	try {
	    return reflectiveCall(mr,obj,args);
	} catch (ReflectiveException e) {
	    throw new InvocationTargetException(e.getCause());
	}
    }
}

