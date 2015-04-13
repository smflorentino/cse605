package java.lang.reflect;

import static com.fiji.fivm.Constants.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.r1.Magic.*;

import java.lang.annotation.Annotation;

import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.fivmRuntime;

public final class Field
    extends AccessibleObject
    implements Member {
    
    Pointer fr;
    
    Field(Pointer fr) {
	this.fr=fr;
	fivmr_ReflectLog_accessReflect(curThreadState(),fr);
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof Field)) return false;
	Field other=(Field)other_;
	return fr==other.fr;
    }
    
    public int hashCode() {
	return fr.castToInt();
    }
    
    public <T extends Annotation > T getAnnotation(Class< T > annotationClass) {
	return null; // FIXME
    }
    
    public Annotation[] getDeclaredAnnotations() {
	return new Annotation[0]; // FIXME
    }
    
    public Class< ? > getDeclaringClass() {
	return fivmr_TypeData_asClass(fivmr_FieldRec_owner(fr));
    }
    
    public String getName() {
	return fromCStringFull(fivmr_FieldRec_name(fr));
    }
    
    public int getModifiers() {
	return fieldModifiersForFlags(fivmr_FieldRec_flags(fr));
    }
    
    public boolean isSynthetic() {
	return false; // FIXME
    }
    
    public boolean isEnumConstant() {
	return false; // FIXME
    }
    
    public Class< ? > getType() {
	return getFieldType(fr);
    }
    
    public String toString() {
	return reflectiveFieldToString(fr);
    }
    
    public String toGenericString() {
	return toString(); // FIXME
    }
    
    private void checkInit() {
	fivmRuntime.checkInit(fivmr_FieldRec_owner(fr));
    }
    
    public Object get(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'L':
	    case '[': return objectGetStatic(fivmr_FieldRec_staticFieldAddress(fr),
					     flags);
	    case 'Z': return new Boolean(loc.loadBoolean());
	    case 'B': return new Byte(loc.loadByte());
	    case 'C': return new Character(loc.loadChar());
	    case 'S': return new Short(loc.loadShort());
	    case 'I': return new Integer(loc.loadInt());
	    case 'J': return new Long(loc.loadLong());
	    case 'F': return new Float(loc.loadFloat());
	    case 'D': return new Double(loc.loadDouble());
	    default: break;
	    }
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load instance field from null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'L':
	    case '[': return objectGetField(o,loc,flags);
	    case 'Z': return new Boolean(byteGetField(o,loc,flags)!=0);
	    case 'B': return new Byte(byteGetField(o,loc,flags));
	    case 'C': return new Character(charGetField(o,loc,flags));
	    case 'S': return new Short(shortGetField(o,loc,flags));
	    case 'I': return new Integer(intGetField(o,loc,flags));
	    case 'J': return new Long(longGetField(o,loc,flags));
	    case 'F': return new Float(floatGetField(o,loc,flags));
	    case 'D': return new Double(doubleGetField(o,loc,flags));
	    default: break;
	    }
	}
	throw new IllegalArgumentException(
		"Bad field type: "+fromCStringFull(fivmr_TypeData_name(td)));
    }
    
    public boolean getBoolean(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	if (fivmr_TypeData_name(td).loadByte()!='Z') {
	    throw new IllegalArgumentException(
		"Wrong field type for getBoolean");
	}
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    return loc.loadBoolean();
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load from field on null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    return byteGetField(o,loc,flags)!=0;
	}
    }
    
    public byte getByte(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	if (fivmr_TypeData_name(td).loadByte()!='B') {
	    throw new IllegalArgumentException(
		"Wrong field type for getByte");
	}
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    return loc.loadByte();
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load from field on null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    return byteGetField(o,loc,flags);
	}
    }
    
    public char getChar(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	if (fivmr_TypeData_name(td).loadByte()!='C') {
	    throw new IllegalArgumentException(
		"Wrong field type for getChar");
	}
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    return loc.loadChar();
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load from field on null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    return charGetField(o,loc,flags);
	}
    }
    
    public short getShort(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return loc.loadByte();
	    case 'S': return loc.loadShort();
	    default: break;
	    }
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load from field on null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return byteGetField(o,loc,flags);
	    case 'S': return shortGetField(o,loc,flags);
	    default: break;
	    }
	}
	throw new IllegalArgumentException(
		"Bad field type: "+fromCStringFull(fivmr_TypeData_name(td)));
    }
    
    public int getInt(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return loc.loadByte();
	    case 'C': return loc.loadChar();
	    case 'S': return loc.loadShort();
	    case 'I': return loc.loadInt();
	    default: break;
	    }
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load from field on null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return byteGetField(o,loc,flags);
	    case 'C': return charGetField(o,loc,flags);
	    case 'S': return shortGetField(o,loc,flags);
	    case 'I': return intGetField(o,loc,flags);
	    default: break;
	    }
	}
	throw new IllegalArgumentException(
		"Bad field type: "+fromCStringFull(fivmr_TypeData_name(td)));
    }
    
    public long getLong(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return loc.loadByte();
	    case 'C': return loc.loadChar();
	    case 'S': return loc.loadShort();
	    case 'I': return loc.loadInt();
	    case 'J': return loc.loadLong();
	    default: break;
	    }
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load from field on null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return byteGetField(o,loc,flags);
	    case 'C': return charGetField(o,loc,flags);
	    case 'S': return shortGetField(o,loc,flags);
	    case 'I': return intGetField(o,loc,flags);
	    case 'J': return longGetField(o,loc,flags);
	    default: break;
	    }
	}
	throw new IllegalArgumentException(
		"Bad field type: "+fromCStringFull(fivmr_TypeData_name(td)));
    }
    
    public float getFloat(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return loc.loadByte();
	    case 'C': return loc.loadChar();
	    case 'S': return loc.loadShort();
	    case 'I': return loc.loadInt();
	    case 'J': return loc.loadLong();
	    case 'F': return loc.loadFloat();
	    default: break;
	    }
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load from field on null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return byteGetField(o,loc,flags);
	    case 'C': return charGetField(o,loc,flags);
	    case 'S': return shortGetField(o,loc,flags);
	    case 'I': return intGetField(o,loc,flags);
	    case 'J': return longGetField(o,loc,flags);
	    case 'F': return floatGetField(o,loc,flags);
	    default: break;
	    }
	}
	throw new IllegalArgumentException(
		"Bad field type: "+fromCStringFull(fivmr_TypeData_name(td)));
    }
    
    public double getDouble(Object o) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    checkInit();
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return loc.loadByte();
	    case 'C': return loc.loadChar();
	    case 'S': return loc.loadShort();
	    case 'I': return loc.loadInt();
	    case 'J': return loc.loadLong();
	    case 'F': return loc.loadFloat();
	    case 'D': return loc.loadDouble();
	    default: break;
	    }
	} else {
	    if (o==null) {
		throw new NullPointerException("Attempt to load from field on null");
	    }
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(o),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Object does not have the right type for field");
	    }
	    switch (fivmr_TypeData_name(td).loadByte()) {
	    case 'B': return byteGetField(o,loc,flags);
	    case 'C': return charGetField(o,loc,flags);
	    case 'S': return shortGetField(o,loc,flags);
	    case 'I': return intGetField(o,loc,flags);
	    case 'J': return longGetField(o,loc,flags);
	    case 'F': return floatGetField(o,loc,flags);
	    case 'D': return doubleGetField(o,loc,flags);
	    default: break;
	    }
	}
	throw new IllegalArgumentException(
		"Bad field type: "+fromCStringFull(fivmr_TypeData_name(td)));
    }
    
    // FIXME: we currently allow setting of final fields no matter what...
    
    private void setChecks(Object obj) {
	if ((fivmr_FieldRec_flags(fr)&BF_STATIC)!=0) {
	    checkInit();
	} else {
	    if (!fivmr_TypeData_isSubtypeOf(
                    curThreadState(),
		    fivmr_TypeData_forObject(obj),
		    fivmr_FieldRec_owner(fr))) {
		throw new IllegalArgumentException(
		    "Type mismatch on receiver");
	    }
	    if (obj==null) {
		throw new NullPointerException("Attempt to store to field with null receiver");
	    }
	}
    }
    
    // the setImpl methods assume that there's no unboxing or widening to perform, and
    // that the basetype descriptor has already been checked.
    
    private void setImpl(Object o, Object value) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	setChecks(o);
	if (value!=null &&
	    !fivmr_TypeData_isSubtypeOf(
                curThreadState(),
		fivmr_TypeData_forObject(value),
		td)) {
	    throw new IllegalArgumentException(
		"Given object has the wrong type for field");
	}
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    objectPutStatic(fivmr_FieldRec_staticFieldAddress(fr),value,flags);
	} else {
	    objectPutField(o,fivmr_FieldRec_barrierArg(fr),value,flags);
	}
    }
    
    private void setImpl(Object o, boolean value) {
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	setChecks(o);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    loc.store((byte)(value?1:0));
	} else {
	    bytePutField(o,loc,(byte)(value?1:0),flags);
	}
    }
    
    private void setImpl(Object o, byte value) {
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	setChecks(o);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    loc.store(value);
	} else {
	    bytePutField(o,loc,value,flags);
	}
    }
    
    private void setImpl(Object o, char value) {
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	setChecks(o);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    loc.store(value);
	} else {
	    charPutField(o,loc,value,flags);
	}
    }
    
    private void setImpl(Object o, short value) {
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	setChecks(o);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    loc.store(value);
	} else {
	    shortPutField(o,loc,value,flags);
	}
    }
    
    private void setImpl(Object o, int value) {
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	setChecks(o);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    loc.store(value);
	} else {
	    intPutField(o,loc,value,flags);
	}
    }
    
    private void setImpl(Object o, long value) {
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	setChecks(o);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    loc.store(value);
	} else {
	    longPutField(o,loc,value,flags);
	}
    }
    
    private void setImpl(Object o, float value) {
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	setChecks(o);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    loc.store(value);
	} else {
	    floatPutField(o,loc,value,flags);
	}
    }
    
    private void setImpl(Object o, double value) {
	Pointer loc=fivmr_FieldRec_barrierArg(fr);
	setChecks(o);
	int flags=fivmr_FieldRec_flags(fr);
	if ((flags&BF_STATIC)!=0) {
	    loc.store(value);
	} else {
	    doublePutField(o,loc,value,flags);
	}
    }
    
    public void set(Object obj, Object value) {
	Pointer td=resolveStub(fivmr_FieldRec_type(fr));
	switch (fivmr_TypeData_name(td).loadByte()) {
	case 'L':
	case '[':
	    setImpl(obj,value);
	    return;
	case 'Z':
	    if (value instanceof Boolean) {
		setImpl(obj, (boolean)(Boolean)value);
		return;
	    } else break;
	case 'B':
	    if (value instanceof Byte) {
		setImpl(obj, (byte)(Byte)value);
		return;
	    } else break;
	case 'C':
	    if (value instanceof Character) {
		setImpl(obj, (char)(Character)value);
		return;
	    } else break;
	case 'S':
	    if (value instanceof Short) {
		setImpl(obj, (short)(Short)value);
	    } else if (value instanceof Byte) {
		setImpl(obj, (short)(Byte)value);
	    } else break;
	    return;
	case 'I':
	    if (value instanceof Integer) {
		setImpl(obj, (int)(Integer)value);
	    } else if (value instanceof Short) {
		setImpl(obj, (int)(Short)value);
	    } else if (value instanceof Character) {
		setImpl(obj, (int)(Character)value);
	    } else if (value instanceof Byte) {
		setImpl(obj, (int)(Byte)value);
	    } else break;
	    return;
	case 'J':
	    if (value instanceof Long) {
		setImpl(obj, (long)(Long)value);
	    } else if (value instanceof Integer) {
		setImpl(obj, (long)(Integer)value);
	    } else if (value instanceof Short) {
		setImpl(obj, (long)(Short)value);
	    } else if (value instanceof Character) {
		setImpl(obj, (long)(Character)value);
	    } else if (value instanceof Byte) {
		setImpl(obj, (long)(Byte)value);
	    } else break;
	    return;
	case 'F':
	    if (value instanceof Float) {
		setImpl(obj, (float)(Float)value);
	    } else if (value instanceof Long) {
		setImpl(obj, (float)(Long)value);
	    } else if (value instanceof Integer) {
		setImpl(obj, (float)(Integer)value);
	    } else if (value instanceof Short) {
		setImpl(obj, (float)(Short)value);
	    } else if (value instanceof Character) {
		setImpl(obj, (float)(Character)value);
	    } else if (value instanceof Byte) {
		setImpl(obj, (float)(Byte)value);
	    } else break;
	    return;
	case 'D':
	    if (value instanceof Double) {
		setImpl(obj, (double)(Double)value);
	    } else if (value instanceof Float) {
		setImpl(obj, (double)(Float)value);
	    } else if (value instanceof Long) {
		setImpl(obj, (double)(Long)value);
	    } else if (value instanceof Integer) {
		setImpl(obj, (double)(Integer)value);
	    } else if (value instanceof Short) {
		setImpl(obj, (double)(Short)value);
	    } else if (value instanceof Character) {
		setImpl(obj, (double)(Character)value);
	    } else if (value instanceof Byte) {
		setImpl(obj, (double)(Byte)value);
	    } else break;
	    return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(td))+" for value "+value);
    }
    
    public void setBoolean(Object obj, boolean value) {
	switch (fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr))).loadByte()) {
	case 'L': setImpl(obj, new Boolean(value)); return;
	case 'Z': setImpl(obj, value); return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr)))));
    }
    
    public void setByte(Object obj, byte value) {
	switch (fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr))).loadByte()) {
	case 'L': setImpl(obj, new Byte(value)); return;
	case 'B': setImpl(obj, value); return;
	case 'S': setImpl(obj, (short)value); return;
	case 'I': setImpl(obj, (int)value); return;
	case 'J': setImpl(obj, (long)value); return;
	case 'F': setImpl(obj, (float)value); return;
	case 'D': setImpl(obj, (double)value); return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr)))));
    }
    
    public void setChar(Object obj, char value) {
	switch (fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr))).loadByte()) {
	case 'L': setImpl(obj, new Character(value)); return;
	case 'C': setImpl(obj, value); return;
	case 'I': setImpl(obj, (int)value); return;
	case 'J': setImpl(obj, (long)value); return;
	case 'F': setImpl(obj, (float)value); return;
	case 'D': setImpl(obj, (double)value); return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr)))));
    }
    
    public void setShort(Object obj, short value) {
	switch (fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr))).loadByte()) {
	case 'L': setImpl(obj, new Short(value)); return;
	case 'S': setImpl(obj, value); return;
	case 'I': setImpl(obj, (int)value); return;
	case 'J': setImpl(obj, (long)value); return;
	case 'F': setImpl(obj, (float)value); return;
	case 'D': setImpl(obj, (double)value); return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr)))));
    }
    
    public void setInt(Object obj, int value) {
	switch (fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr))).loadByte()) {
	case 'L': setImpl(obj, new Integer(value)); return;
	case 'I': setImpl(obj, value); return;
	case 'J': setImpl(obj, (long)value); return;
	case 'F': setImpl(obj, (float)value); return;
	case 'D': setImpl(obj, (double)value); return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr)))));
    }
    
    public void setLong(Object obj, long value) {
	switch (fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr))).loadByte()) {
	case 'L': setImpl(obj, new Long(value)); return;
	case 'J': setImpl(obj, value); return;
	case 'F': setImpl(obj, (float)value); return;
	case 'D': setImpl(obj, (double)value); return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr)))));
    }
    
    public void setFloat(Object obj, float value) {
	switch (fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr))).loadByte()) {
	case 'L': setImpl(obj, new Float(value)); return;
	case 'F': setImpl(obj, value); return;
	case 'D': setImpl(obj, (double)value); return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr)))));
    }
    
    public void setDouble(Object obj, double value) {
	switch (fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr))).loadByte()) {
	case 'L': setImpl(obj, new Double(value)); return;
	case 'D': setImpl(obj, value); return;
	default: break;
	}
	throw new IllegalArgumentException(
	    "Bad field type "+fromCStringFull(fivmr_TypeData_name(resolveStub(fivmr_FieldRec_type(fr)))));
    }
    
    public Type getGenericType() {
	return null; // FIXME
    }
}

