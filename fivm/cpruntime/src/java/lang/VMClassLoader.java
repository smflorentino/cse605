package java.lang;

import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.fivmRuntime.*;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.lang.instrument.*;

final class VMClassLoader {
    private static final String[] RESOURCES_LOCATIONS =
	new String[]{homeDir()+"/lib/resources"};
    
    static final Class<?> defineClass(ClassLoader cl,String name,
				      byte[] data, int offset, int len,
				      ProtectionDomain pd) {
	return FCClassLoader.defineClass(cl, name, data, offset, len);
    }
    
    static final void resolveClass(Class<?> c) {
	FCClassLoader.resolveClass(c);
    }
    
    static final Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException{
	return FCClassLoader.loadClass(name, resolve);
    }
    
    static URL getResource(String name) {
	Enumeration< URL > e=getResources(name);
	if (e.hasMoreElements()) {
	    return e.nextElement();
	} else {
	    return null;
	}
    }
    
    static Enumeration< URL > getResources(String name) {
	log(VMClassLoader.class,2,"getResources("+name+") called.");
	try {
	    Vector< URL > v=new Vector< URL >();
	    for (String l : RESOURCES_LOCATIONS) {
		File f=new File(l+"/"+name);
		if (f.exists()) {
		    v.add(f.toURL());
		}
	    }
	    return v.elements();
	} catch (Throwable t) {
	    throw new fivmError(t);
	}
    }
    
    static Package getPackage(String name) {
	return null; // FIXME
    }
    
    static Package[] getPackages() {
	return new Package[0]; // FIXME
    }
    
    static Class<?> getPrimitiveClass(char type) {
	return FCClassLoader.getPrimitiveClass(type);
    }
    
    static boolean defaultAssertionStatus() {
	return FCClassLoader.defaultAssertionStatus();
    }
    
    @SuppressWarnings("unchecked")
    static Map<?,?> packageAssertionStatus() {
	return new TreeMap();
    }
    
    @SuppressWarnings("unchecked")
    static Map<?,?> classAssertionStatus() {
	return new TreeMap();
    }
    
    static ClassLoader getSystemClassLoader() {
	return FCClassLoader.getSystemClassLoader();
    }
    
    static Class<?> findLoadedClass(ClassLoader cl,String name) {
	return FCClassLoader.findLoadedClass(cl, name);
    }
    
    static final Instrumentation instrumenter = null;
    
    static final Class<?> defineClassWithTransformers(ClassLoader loader,
						      String name,
						      byte[] data,
						      int offset,
						      int len,
						      ProtectionDomain pd) {
	throw new ClassFormatError("defineClassWithTransformers not supported");
    }
}

