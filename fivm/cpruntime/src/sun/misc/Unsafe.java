package sun.misc;

import static com.fiji.fivm.r1.fivmRuntime.*;

import com.fiji.fivm.om.OMData;
import com.fiji.fivm.r1.MM;
import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.CVar;
import com.fiji.fivm.r1.Monitors;

import com.fiji.fivm.r1.RuntimeImport;
import com.fiji.fivm.r1.NoSafepoint;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class Unsafe {
    public static final int INVALID_FIELD_OFFSET = -1;

    private Unsafe() { };

    private static final Unsafe unsafe = new Unsafe();

    public int addressSize() {
	return Pointer.size();
    }

    public Object allocateInstance(Class<?> clazz)
	throws InstantiationException {
	return MM.alloc(OMData.objectSpace(),
			java.lang.fivmSupport.typeDataFromClass(clazz));
    }

    public long allocateMemory(long bytes) {
	return fivmr_malloc(Pointer.fromLong(bytes)).asLong();
    }

    /* FIXME: fragile */
    public int arrayBaseOffset(Class<?> arrayClass) {
	return Pointer.size() == 4 ? 0 : 4;
    }

    public int arrayIndexScale(Class<?> arrayClass) {
	return fivmr_TypeData_elementSize(
	    java.lang.fivmSupport.typeDataFromClass(arrayClass));
    }

    public final boolean compareAndSwapInt(Object o, long offset,
					   int expected, int x) {
	return fivmr_intWeakCASField(Magic.curThreadState(), o,
				     Pointer.fromLong(offset), expected,
				     x, 0);
    }

    /* FIXME: only works on 64-bit */
    public final boolean compareAndSwapLong(Object o, long offset,
					    long expected, long x) {
	return fivmr_pointerWeakCASField(Magic.curThreadState(), o,
					 Pointer.fromLong(offset),
					 Pointer.fromLong(expected),
					 Pointer.fromLong(x), 0);
    }

    public final boolean compareAndSwapObject(Object o, long offset,
					      Object expected, Object x) {
	return fivmr_objectWeakCASField(Magic.curThreadState(), o,
					Pointer.fromLong(offset), expected,
					x, 0);
    }

    public void copyMemory(long srcAddress, long destAddress, long bytes) {
	fivmr_memmove(Pointer.fromLong(destAddress),
		      Pointer.fromLong(srcAddress), bytes);
    }

    /* defineClass */

    public int fieldOffset(Field f) {
	if (Modifier.isStatic(f.getModifiers())) {
	    return (int)staticFieldOffset(f);
	} else {
	    return (int)objectFieldOffset(f);
	}
    }

    public void freeMemory(long address) {
	fivmr_free(Pointer.fromLong(address));
    }

    public long getAddress(long address) {
	return Pointer.fromLong(address).loadPointer().asLong();
    }

    public boolean getBoolean(Object o, long offset) {
	return fivmr_byteGetField(Magic.curThreadState(), o,
				  Pointer.fromLong(offset), 0) != 0;
    }

    public boolean getBooleanVolatile(Object o, long offset) {
	return getBoolean(o, offset);
    }

    public byte getByte(long address) {
	return Pointer.fromLong(address).loadByte();
    }

    public byte getByte(Object o, long offset) {
	return fivmr_byteGetField(Magic.curThreadState(), o,
				  Pointer.fromLong(offset), 0);
    }

    public byte getByteVolatile(Object o, long offset) {
	return getByte(o, offset);
    }

    public char getChar(long address) {
	return Pointer.fromLong(address).loadChar();
    }

    public char getChar(Object o, long offset) {
	return fivmr_charGetField(Magic.curThreadState(), o,
				  Pointer.fromLong(offset), 0);
    }

    public char getCharVolatile(Object o, long offset) {
	return getChar(o, offset);
    }

    public double getDouble(long address) {
	return Pointer.fromLong(address).loadDouble();
    }

    public double getDouble(Object o, long offset) {
	return fivmr_doubleGetField(Magic.curThreadState(), o,
				    Pointer.fromLong(offset), 0);
    }

    public double getDoubleVolatile(Object o, long offset) {
	return getDouble(o, offset);
    }

    public float getFloat(long address) {
	return Pointer.fromLong(address).loadFloat();
    }

    public float getFloat(Object o, long offset) {
	return fivmr_floatGetField(Magic.curThreadState(), o,
				   Pointer.fromLong(offset), 0);
    }

    public float getFloatVolatile(Object o, long offset) {
	return getFloat(o, offset);
    }

    public int getInt(long address) {
	return Pointer.fromLong(address).loadInt();
    }

    public int getInt(Object o, long offset) {
	return fivmr_intGetField(Magic.curThreadState(), o,
				 Pointer.fromLong(offset), 0);
    }

    public int getIntVolatile(Object o, long offset) {
	return getInt(o, offset);
    }

    public long getLong(long address) {
	return Pointer.fromLong(address).loadLong();
    }

    public long getLong(Object o, long offset) {
	return fivmr_longGetField(Magic.curThreadState(), o,
				  Pointer.fromLong(offset), 0);
    }

    public long getLongVolatile(Object o, long offset) {
	return getLong(o, offset);
    }

    public Object getObject(Object o, long offset) {
	return fivmr_objectGetField(Magic.curThreadState(), o,
				    Pointer.fromLong(offset), 0);
    }

    public Object getObjectVolatile(Object o, long offset) {
	return getObject(o, offset);
    }

    public short getShort(long address) {
	return Pointer.fromLong(address).loadShort();
    }

    public short getShort(Object o, long offset) {
	return fivmr_shortGetField(Magic.curThreadState(), o,
				   Pointer.fromLong(offset), 0);
    }

    public short getShortVolatile(Object o, long offset) {
	return getShort(o, offset);
    }

    public static Unsafe getUnsafe() {
	return unsafe;
    }

    public void monitorEnter(Object o) {
	Monitors.lock(o);
    }

    public void monitorExit(Object o) {
	Monitors.unlock(o);
    }

    public long objectFieldOffset(Field f) {
	Pointer fr = java.lang.reflect.fivmSupport.getFieldRecFromField(f);
	return fivmr_locationToOffsetFromObj(fivmr_FieldRec_location(fr))
	    .asLong();
    }

    public int pageSize() {
	return CVar.getInt("FIVMR_PAGE_SIZE");
    }

    public void park(boolean isAbsolute, long time) {
	java.lang.fivmSupport.park(isAbsolute, time);
    }

    public void putAddress(long address, long x) {
	Pointer.fromLong(address).store(Pointer.fromLong(x));
    }

    public void putBoolean(Object o, long offset, boolean x) {
	fivmr_bytePutField(Magic.curThreadState(), o,
			   Pointer.fromLong(offset), x?(byte)1:(byte)0, 0);
    }

    public void putBooleanVolatile(Object o, long offset, boolean x) {
	putBoolean(o, offset, x);
    }

    public void putByte(long address, byte x) {
	Pointer.fromLong(address).store(x);
    }

    public void putByte(Object o, long offset, byte x) {
	fivmr_bytePutField(Magic.curThreadState(), o,
			   Pointer.fromLong(offset), x, 0);
    }

    public void putByteVolatile(Object o, long offset, byte x) {
	putByte(o, offset, x);
    }

    public void putChar(long address, char x) {
	Pointer.fromLong(address).store(x);
    }

    public void putChar(Object o, long offset, char x) {
	fivmr_charPutField(Magic.curThreadState(), o,
			   Pointer.fromLong(offset), x, 0);
    }

    public void putCharVolatile(Object o, long offset, char x) {
	putChar(o, offset, x);
    }

    public void putDouble(long address, double x) {
	Pointer.fromLong(address).store(x);
    }

    public void putDouble(Object o, long offset, double x) {
	fivmr_doublePutField(Magic.curThreadState(), o,
			     Pointer.fromLong(offset), x, 0);
    }

    public void putDoubleVolatile(Object o, long offset, double x) {
	putDouble(o, offset, x);
    }

    public void putFloat(long address, float x) {
	Pointer.fromLong(address).store(x);
    }

    public void putFloat(Object o, long offset, float x) {
	fivmr_floatPutField(Magic.curThreadState(), o,
			    Pointer.fromLong(offset), x, 0);
    }

    public void putFloatVolatile(Object o, long offset, float x) {
	putFloat(o, offset, x);
    }

    public void putInt(long address, int x) {
	Pointer.fromLong(address).store(x);
    }

    public void putInt(Object o, long offset, int x) {
	fivmr_intPutField(Magic.curThreadState(), o,
			  Pointer.fromLong(offset), x, 0);
    }

    public void putIntVolatile(Object o, long offset, int x) {
	putInt(o, offset, x);
    }

    public void putLong(long address, long x) {
	Pointer.fromLong(address).store(x);
    }

    public void putLong(Object o, long offset, long x) {
	fivmr_longPutField(Magic.curThreadState(), o,
			   Pointer.fromLong(offset), x, 0);
    }

    public void putLongVolatile(Object o, long offset, long x) {
	putLong(o, offset, x);
    }

    public void putObject(Object o, long offset, Object x) {
	fivmr_objectPutField(Magic.curThreadState(), o,
			     Pointer.fromLong(offset), x, 0);
    }

    public void putObjectVolatile(Object o, long offset, Object x) {
	putObject(o, offset, x);
    }

    public void putOrderedInt(Object o, long offset, int x) {
	fivmr_intPutField(Magic.curThreadState(), o,
			  Pointer.fromLong(offset), x, 0);
    }

    public void putOrderedLong(Object o, long offset, long x) {
	fivmr_longPutField(Magic.curThreadState(), o,
			   Pointer.fromLong(offset), x, 0);
    }

    public void putOrderedObject(Object o, long offset, Object x) {
	fivmr_objectPutField(Magic.curThreadState(), o,
			     Pointer.fromLong(offset), x, 0);
    }

    public void putShort(long address, short x) {
	Pointer.fromLong(address).store(x);
    }

    public void putShort(Object o, long offset, short x) {
	fivmr_shortPutField(Magic.curThreadState(), o,
			    Pointer.fromLong(offset), x, 0);
    }

    public void putShortVolatile(Object o, long offset, short x) {
	putShort(o, offset, x);
    }

    public long reallocateMemory(long address, long bytes) {
	return fivmr_realloc(Pointer.fromLong(address),
			     Pointer.fromLong(bytes)).asLong();
    }

    public void setMemory(long address, long bytes, byte value) {
	fivmr_memset(Pointer.fromLong(address), Pointer.fromLong(bytes),
		     value);
    }

     public Object staticFieldBase(Field f) {
	return null;
    }

    public long staticFieldOffset(Field f) {
	Pointer fr = java.lang.reflect.fivmSupport.getFieldRecFromField(f);
	return fivmr_FieldRec_staticFieldAddress(fr).asLong();
    }

    /* throwException */

    /* tryMonitorEnter */

    public static void unpark(Thread t) {
	java.lang.fivmSupport.unpark(t);
    }

    @RuntimeImport
    @NoSafepoint
    private native void fivmr_memmove(Pointer dst, Pointer src, long size);

    @RuntimeImport
    @NoSafepoint
    private native void fivmr_memset(Pointer area, Pointer size, byte value);

    @RuntimeImport
    @NoSafepoint
    private native boolean fivmr_intWeakCASField(Pointer ts, Object o,
						 Pointer offset,
						 int comparand, int value,
						 int mask);

    @RuntimeImport
    @NoSafepoint
    private native boolean fivmr_pointerWeakCASField(Pointer ts, Object o,
						     Pointer offset,
						     Pointer comparand,
						     Pointer value,
						     int mask);

    @RuntimeImport
    @NoSafepoint
    private native boolean fivmr_objectWeakCASField(Pointer ts, Object o,
						    Pointer offset,
						    Object comparand,
						    Object value,
						    int mask);
}