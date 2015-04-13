package gnu.classpath;

public final class VMStackWalker {
    public static Class<?>[] getClassContext() {
	return FCStackWalker.getClassContext2();
    }
    
    public static Class<?> getCallingClass() {
	return FCStackWalker.getCallingClass2();
    }
    
    public static ClassLoader getCallingClassLoader() {
	return FCStackWalker.getCallingClassLoader2();
    }
    
    public static ClassLoader getClassLoader(Class<?> cl) {
	return FCStackWalker.getClassLoader(cl);
    }
    
    public static ClassLoader firstNonNullClassLoader() {
	return FCStackWalker.firstNonNullClassLoader();
    }
}

