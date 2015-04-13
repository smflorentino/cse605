package java.lang.reflect;

import com.fiji.fivm.r1.*;

public class fivmSupport {
    public static Method newMethod(Pointer mr) {
	return new Method(mr);
    }
    
    @SuppressWarnings("unchecked")
    public static Constructor<?> newConstructor(Pointer mr) {
	return new Constructor(mr);
    }
    
    public static Field newField(Pointer fr) {
	return new Field(fr);
    }

    public static Pointer getFieldRecFromField(Field f) {
	return f.fr;
    }
}


