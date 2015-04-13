/*
 * RunProgram.java
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

package com.fiji.fivm.util;

import com.fiji.asm.*;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.fivmRuntime.*;

public class RunProgram {
    private RunProgram() {}
    
    public static void main(String[] v) throws Throwable {
        if (v.length<1) {
            throw new fivmError("must specify a class to run.");
        }
        
        long before=Time.nanoTime();
        
        log(RunProgram.class,1,
            "Finding type "+v[0]+"...");
        
        // The findType() call will perform a search for the class in the two
        // class loaders: the "null" class loader, which corresponds to the
        // boot classpath, and the AppClassLoader.instance, which corresponds to
        // the CLASSPATH specified by the user.  This proceeds as follows:
        //  1) fivmr_TypeContext_find() is called with the TypeContext that
        //     corresponds to AppClassLoader.instance
        //  2) fivmr_TypeContext_find() calls fivmr_TypeContext_findKnown(),
        //     which checks if the class is already "known" to the type context.
        //     This corresponds to the "findLoadedClass" call in the JDK.
        //     This will search both the App context, and the Root context
        //     (App = type context for AppClassLoader.instance, Root = type
        //     context for the boot classpath).  The Root context is searched
        //     in a "special" way - only those classes that were statically
        //     "known" to the App context at compile-time are admitted.  Only
        //     the RunProgram class is in the App context, therefore
        //     fivmr_TypeContext_findKnown will only consider Root classes
        //     that are statically known to be used by RunProgram.  Therefore,
        //     the fivmr_TypeContext_findKnown() call is pretty much guaranteed
        //     to return NULL, which brings us to the next step.
        //  3) fivmr_TypeContext_find() calls into the loadClass() of its
        //     ClassLoader, which in this case is the AppClassLoader.
        //  4) AppClassLoader does not override loadClass(), so
        //     ClassLoader.loadClass() is called.
        //  5) ClassLoader.loadClass() first tries to see if the class is already
        //     known by calling findLoadedClass(), which bottoms out at
        //     fivmr_TypeContext_findKnown().  This again returns NULL.
        //  6) ClassLoader.loadClass() next observes that the parent of
        //     AppClassLoader is null (i.e. its parent is the boot classpath),
        //     and calls FCClassLoader.loadClass().
        //  7) FCClassLoader.loadClass() first calls fivmr_TypeContext_findKnown()
        //     but this time on the Root type context.  This is almost certain
        //     to return NULL since the user is unlikely to request that we
        //     run a main() method that is internal to Fiji VM (since Fiji VM
        //     probably shouldn't have any internal main() methods).
        //  8) FCClassLoader.loadClass() then uses ClassLocator.ROOT to try
        //     to find bytecode for the requested class.  This will search the
        //     Fiji VM jar files in the Fiji VM lib directory.  This will
        //     almost certainly fail (see #7 above), and will return NULL.
        //  9) ClassLoader.loadClass() next calls AppClassLoader.findClass(),
        //     which uses ClassLocator.APP to search for the class.
        //     ClassLocator.APP uses the CLASSPATH supplied by the user, so if
        //     the user did everything right, this will return the bytecode.
        // 10) AppClassLoader.findClass() then calls fivmRuntime.defineClass()
        //     (as a short-circuit optimization, since ClassLoader.defineClass()
        //     will bottom-out there anyway), which constructs the Class and
        //     an "unresolved" TypeData.  Note, the defineClass() procedure
        //     is quite complex - this is where most of the work happens.
        // 11) The Class corresponding to the "unresolved" TypeData is returned
        //     all the way to the call from fivmr_TypeContext_find() in #3
        //     above.
        // 12) TypeContext_find() gets the TypeData from the Class, does a
        //     Hindley-Milner assertion, and returns.  The td variable below
        //     is the one we get back from TypeContext_find().
        
        long subBefore=Time.nanoTime();
        
        Pointer td=findType(
            java.lang.fivmSupport.getClassLoaderData(AppClassLoader.instance),
            "L"+v[0].replace('.','/')+";");
        
        long subAfter=Time.nanoTime();
        
        log(RunProgram.class,1,
            "findType("+v[0]+") took "+(subAfter-subBefore)+" ns");
        
        if (td==Pointer.zero()) {
            throw new NoClassDefFoundError(v[0]);
        }
        
        log(RunProgram.class,1,
            "Found "+td.asLong()+"; performing resolution and static initialization...");
        
        // This call does both type resolution (which mostly means integrating
        // the TypeData into the type hierarchy, but also includes some further
        // Hindley-Milner assertions) and static initialization.  It bottoms out
        // in fivmr_TypeData_resolve and then fivmr_TypeData_checkInit.  Either
        // method may throw exceptions if bad things happen (though in
        // fivmr_TypeData_resolve's case, it actually returns false, and
        // fivmRuntime.resolveType() throws the exception).  Both calls will
        // do nothing if there is nothing to do -- i.e. if the type is already
        // resolved, if the type does not have static initializers, or if the
        // type is already initialized.  Here, the type is guaranteed not to
        // be resolved, and is guaranteed not to be initialized -- though it
        // may not have static initializers.
        
        resolveAndCheckInit(td);
        
        log(RunProgram.class,1,
            "Locating main([Ljava/lang/String;)V in  "+td.asLong()+"...");
        
        // This call finds the static method, without searching the hierarchy
        // (as per bytecode semantics).
        
        Pointer mr=findStaticMethod(
            td,
            new UTF8Sequence("main"),
            new UTF8Sequence("([Ljava/lang/String;)V"));
        
        if (mr==Pointer.zero()) {
            throw new NoSuchMethodError("Could not find static method main([Ljava/lang/String;)V");
        }
        
        long after=Time.nanoTime();
        
        log(RunProgram.class,1,
            "Class loading of "+v[0]+" took "+(after-before)+" ns");
        
        log(RunProgram.class,1,
            "Found "+mr.asLong()+"; invoking...");
        
        String[] jArgs=new String[v.length-1];
        System.arraycopy(v,1,
                         jArgs,0,
                         jArgs.length);
        
        // Perform the call.  Note that this is where the JIT will be invoked
        // to create the machine code implementation of the main() method.  This
        // happens because the "entrypoint" of the main method will at this
        // point be the load thunk created by defineClass().  That thunk will
        // call into fivmRuntime.handleLoadThunk(), which will call into the
        // JIT to generate the code, and will then replace the method's entrypoint
        // with the JIT's implementation rather than the load thunk.  The load
        // thunk will then tail-call into the new entrypoint.
        
        try {
            reflectiveCall(mr,new Object[]{jArgs});
        } catch (ReflectiveException e) {
            throw e.getCause();
        }
    }
}

