package __vm;

public final class FCMagic {
    private FCMagic() {}
    
    public static native FCPtr zero();
    
    public static native FCPtr malloc(int size);

    public static native FCPtr addressOfElement(Object o,int index);
    public static native FCPtr fromIntSignExtend(int value);
    public static native FCPtr fromInt(int value); // defaults to sign-extend
    public static native FCPtr fromIntZeroExtend(int value);
    public static native FCPtr plus(FCPtr a,FCPtr b);
    public static native FCPtr minus(FCPtr a,FCPtr b);
    public static native FCPtr neg(FCPtr a);
    public static native FCPtr times(FCPtr a,FCPtr b);

    public static native byte readByte(FCPtr a);
    public static native void writeByte(FCPtr a,byte value);
    
    public static native void copyToByteArray(byte[] array,int offset,int length,
					      FCPtr ptr);
    public static native void copyFromByteArray(byte[] array,int offset,int length,
						FCPtr ptr);

    public static native void copy(FCPtr to,FCPtr from,FCPtr size);
    public static native void move(FCPtr to,FCPtr from,FCPtr size);
    
    public static native void uncheckedThrow(Throwable e);
    
    public static native boolean allContiguous();
    public static native boolean isContiguous(Object o);
    
    public static native void printStr(String str);
    public static native void printPtr(Object obj);
    public static native void printPtr(FCPtr ptr);
    public static native void printNum(long num);
    public static native void println();
}


