package java.lang;

final class FCEnum {
    public static native <S extends Enum<S>> S valueOf(Class<S> etype, String s);
}
