package java.lang.reflect;

final class FCArray {
    private FCArray() {}
    
    static native Object createObjectArray(Class<?> componentType, int length);
}

