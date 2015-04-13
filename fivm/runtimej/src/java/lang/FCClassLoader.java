/*
 * FCClassLoader.java
 * Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
 * This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
 * LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
 * available at fivm/LEGAL and can also be found at
 * http://www.fiji-systems.com/FPL3.txt
 * 
 * By installing, reproducing, distributing, and/or using the FIJI VM Software
 * you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
 * rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
 * restrictions stated therein.  Among other conditions and restrictions, the
 * FIJI PUBLIC LICENSE states that:
 * 
 * a. You may only make non-commercial use of the FIJI VM Software.
 * 
 * b. Any adaptation you make must be licensed under the same terms 
 * of the FIJI PUBLIC LICENSE.
 * 
 * c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
 * file, adaptation or output code that you distribute and cause the output code
 * to provide a notice of the FIJI PUBLIC LICENSE. 
 * 
 * d. You must not impose any additional conditions.
 * 
 * e. You must not assert or imply any connection, sponsorship or endorsement by
 * the author of the FIJI VM Software
 * 
 * f. You must take no derogatory action in relation to the FIJI VM Software
 * which would be prejudicial to the FIJI VM Software author's honor or
 * reputation.
 * 
 * 
 * The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
 * representation and provides no warranty of any kind concerning the software.
 * 
 * The FIJI PUBLIC LICENSE and any rights granted therein terminate
 * automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
 */

package java.lang;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.fivmRuntime.*;


final class FCClassLoader {
    static final Class<?> defineClass(ClassLoader cl,String name,
				      byte[] data, int offset, int len) {
        if (!Settings.CLASSLOADING) {
            throw new ClassFormatError("defineClass not supported");
        } else {
            byte[] copy=new byte[len];
            System.arraycopy(data,offset,
                             copy,0,
                             len);
            return fivmRuntime.defineClass(cl,name,copy);
        }
    }
    
    static final void resolveClass(Class<?> c) {
	resolveType(fivmSupport.typeDataFromClass(c));
    }
    
    static final Class<?> loadClass(String name, boolean resolve)
	throws ClassNotFoundException {
        String typename="L"+name.replace('.','/')+";";
        fivmr_ReflectLog_useReflectByName(Magic.curThreadState(),typename);
        Class<?> result=findLoadedType(null,typename);
	if (result==null) {
            if (!Settings.CLASSLOADING) {
                throw new ClassNotFoundException(name);
            } else {
                log(FCClassLoader.class,1,
                    "Attempting to load "+name+" from system");
                byte[] bytecode=
                    ClassLocator.ROOT.attemptToLoadClassCompletely(name);
                if (bytecode==null) {
                    log(FCClassLoader.class,1,
                        "Could not find "+name);
                    throw new ClassNotFoundException(name);
                }
                result=fivmRuntime.defineClass(null,null,bytecode);
                log(FCClassLoader.class,1,
                    "Loaded "+name+": "+result);
            }
	}
        if (resolve) {
            resolveClass(result);
        }
	return result;
    }
    
    static Class<?> getPrimitiveClass(char type) {
	Pointer td=findKnownType(baseTypeContext(0),""+type);
	if (td!=Pointer.zero()) {
	    return fivmr_TypeData_asClass(td);
	} else {
	    return null;
	}
    }
    
    static boolean defaultAssertionStatus() {
	return true; // FIXME this should be handled by compiler magic
    }
    
    static ClassLoader getSystemClassLoader() {
	return AppClassLoader.instance;
    }
    
    private static Class<?> findLoadedType(ClassLoader cl,String typename) {
	Pointer result=findKnownType(fivmSupport.getClassLoaderData(cl),typename);
        if (result==Pointer.zero()) {
            return null;
        } else {
            return fivmr_TypeData_asClass(result);
        }
    }
    
    static Class<?> findLoadedClass(ClassLoader cl,String name) {
        String typename="L"+name.replace('.','/')+";";
        fivmr_ReflectLog_useReflectByName(Magic.curThreadState(),typename);
        return findLoadedType(cl,typename);
    }
}

