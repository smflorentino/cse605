/* ClassLoader.java -- responsible for loading classes into the VM
   Copyright (C) 1998, 1999, 2001, 2002, 2003, 2004, 2005 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package java.lang;

import gnu.classpath.SystemProperties;
import gnu.classpath.FCStackWalker;
import gnu.java.util.DoubleEnumeration;
import gnu.java.util.EmptyEnumeration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The ClassLoader is a way of customizing the way Java gets its classes
 * and loads them into memory.  The verifier and other standard Java things
 * still run, but the ClassLoader is allowed great flexibility in determining
 * where to get the classfiles and when to load and resolve them. For that
 * matter, a custom ClassLoader can perform on-the-fly code generation or
 * modification!
 *
 * <p>Every classloader has a parent classloader that is consulted before
 * the 'child' classloader when classes or resources should be loaded.
 * This is done to make sure that classes can be loaded from an hierarchy of
 * multiple classloaders and classloaders do not accidentially redefine
 * already loaded classes by classloaders higher in the hierarchy.
 *
 * <p>The grandparent of all classloaders is the bootstrap classloader, which
 * loads all the standard system classes as implemented by GNU Classpath. The
 * other special classloader is the system classloader (also called
 * application classloader) that loads all classes from the CLASSPATH
 * (<code>java.class.path</code> system property). The system classloader
 * is responsible for finding the application classes from the classpath,
 * and delegates all requests for the standard library classes to its parent
 * the bootstrap classloader. Most programs will load all their classes
 * through the system classloaders.
 *
 * <p>The bootstrap classloader in GNU Classpath is implemented as a couple of
 * static (native) methods on the package private class
 * <code>java.lang.VMClassLoader</code>, the system classloader is an
 * anonymous inner class of ClassLoader and a subclass of
 * <code>java.net.URLClassLoader</code>.
 *
 * <p>Users of a <code>ClassLoader</code> will normally just use the methods
 * <ul>
 *  <li> <code>loadClass()</code> to load a class.</li>
 *  <li> <code>getResource()</code> or <code>getResourceAsStream()</code>
 *       to access a resource.</li>
 *  <li> <code>getResources()</code> to get an Enumeration of URLs to all
 *       the resources provided by the classloader and its parents with the
 *       same name.</li>
 * </ul>
 *
 * <p>Subclasses should implement the methods
 * <ul>
 *  <li> <code>findClass()</code> which is called by <code>loadClass()</code>
 *       when the parent classloader cannot provide a named class.</li>
 *  <li> <code>findResource()</code> which is called by
 *       <code>getResource()</code> when the parent classloader cannot provide
 *       a named resource.</li>
 *  <li> <code>findResources()</code> which is called by
 *       <code>getResource()</code> to combine all the resources with the
 *       same name from the classloader and its parents.</li>
 *  <li> <code>findLibrary()</code> which is called by
 *       <code>Runtime.loadLibrary()</code> when a class defined by the
 *       classloader wants to load a native library.</li>
 * </ul>
 *
 * @author John Keiser
 * @author Mark Wielaard
 * @author Eric Blake (ebb9@email.byu.edu)
 * @see Class
 * @since 1.0
 */
public abstract class ClassLoader
{
 

  /**
   * The classloader that is consulted before this classloader.
   * If null then the parent is the bootstrap classloader.
   */
  private final ClassLoader parent;

  /**
   * This is true if this classloader was successfully initialized.
   * This flag is needed to avoid a class loader attack: even if the
   * security manager rejects an attempt to create a class loader, the
   * malicious class could have a finalize method which proceeds to
   * define classes.
   */
  private final boolean initialized;

  static class StaticData
  {
    /**
     * The System Class Loader (a.k.a. Application Class Loader). The one
     * returned by ClassLoader.getSystemClassLoader.
     */
    static final ClassLoader systemClassLoader =
                              FCClassLoader.getSystemClassLoader();
  }

  /**
   * The desired assertion status of classes loaded by this loader, if not
   * overridden by package or class instructions.
   */
  // Package visible for use by Class.
  boolean defaultAssertionStatus = FCClassLoader.defaultAssertionStatus();

  /**
   * The map of package assertion status overrides, or null if no package
   * overrides have been specified yet. The values of the map should be
   * Boolean.TRUE or Boolean.FALSE, and the unnamed package is represented
   * by the null key. This map must be synchronized on this instance.
   */
  // Package visible for use by Class.
 

  /**
   * The map of class assertion status overrides, or null if no class
   * overrides have been specified yet. The values of the map should be
   * Boolean.TRUE or Boolean.FALSE. This map must be synchronized on this
   * instance.
   */
  // Package visible for use by Class.
  Map<String, Boolean> classAssertionStatus;

  /**
   * VM private data.
   */
  transient Object vmdata;

  /**
   * Create a new ClassLoader with as parent the system classloader. There
   * may be a security check for <code>checkCreateClassLoader</code>.
   *
   * @throws SecurityException if the security check fails
   */
  protected ClassLoader() throws SecurityException
  {
    this(StaticData.systemClassLoader);
  }

  /**
   * Create a new ClassLoader with the specified parent. The parent will
   * be consulted when a class or resource is requested through
   * <code>loadClass()</code> or <code>getResource()</code>. Only when the
   * parent classloader cannot provide the requested class or resource the
   * <code>findClass()</code> or <code>findResource()</code> method
   * of this classloader will be called. There may be a security check for
   * <code>checkCreateClassLoader</code>.
   *
   * @param parent the classloader's parent, or null for the bootstrap
   *        classloader
   * @throws SecurityException if the security check fails
   * @since 1.2
   */
  protected ClassLoader(ClassLoader parent)
  {
    // May we create a new classloader?
    this.parent = parent;
    this.initialized = true;
  }

  /**
   * Load a class using this ClassLoader or its parent, without resolving
   * it. Calls <code>loadClass(name, false)</code>.
   *
   * <p>Subclasses should not override this method but should override
   * <code>findClass()</code> which is called by this method.</p>
   *
   * @param name the name of the class relative to this ClassLoader
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    return loadClass(name, false);
  }

  /**
   * Load a class using this ClassLoader or its parent, possibly resolving
   * it as well using <code>resolveClass()</code>. It first tries to find
   * out if the class has already been loaded through this classloader by
   * calling <code>findLoadedClass()</code>. Then it calls
   * <code>loadClass()</code> on the parent classloader (or when there is
   * no parent it uses the VM bootclassloader). If the class is still
   * not loaded it tries to create a new class by calling
   * <code>findClass()</code>. Finally when <code>resolve</code> is
   * <code>true</code> it also calls <code>resolveClass()</code> on the
   * newly loaded class.
   *
   * <p>Subclasses should not override this method but should override
   * <code>findClass()</code> which is called by this method.</p>
   *
   * @param name the fully qualified name of the class to load
   * @param resolve whether or not to resolve the class
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  protected synchronized Class<?> loadClass(String name, boolean resolve)
      throws ClassNotFoundException {
      // Have we already loaded this class?
      Class<?> c = findLoadedClass(name);
      if (c == null) {
          // Can the class be loaded by a parent?
          try {
              if (parent == null) {
                  c = FCClassLoader.loadClass(name, resolve);
	      } else {
                  return parent.loadClass(name, resolve);
	      }
              if (c != null) {
                  return c;
              }
              
	  } catch (ClassNotFoundException e) {}
          
          // Still not found, we have to do it ourselves.
          c = findClass(name);
          if (c==null) {
              throw new ClassNotFoundException(name);
          }
      }
      if (resolve) {
          resolveClass(c);
      }
      return c;
  }

  /**
   * Called for every class name that is needed but has not yet been
   * defined by this classloader or one of its parents. It is called by
   * <code>loadClass()</code> after both <code>findLoadedClass()</code> and
   * <code>parent.loadClass()</code> couldn't provide the requested class.
   *
   * <p>The default implementation throws a
   * <code>ClassNotFoundException</code>. Subclasses should override this
   * method. An implementation of this method in a subclass should get the
   * class bytes of the class (if it can find them), if the package of the
   * requested class doesn't exist it should define the package and finally
   * it should call define the actual class. It does not have to resolve the
   * class. It should look something like the following:<br>
   *
   * <pre>
   * // Get the bytes that describe the requested class
   * byte[] classBytes = classLoaderSpecificWayToFindClassBytes(name);
   * // Get the package name
   * int lastDot = name.lastIndexOf('.');
   * if (lastDot != -1)
   *   {
   *     String packageName = name.substring(0, lastDot);
   *     // Look if the package already exists
   *     if (getPackage(packageName) == null)
   *       {
   *         // define the package
   *         definePackage(packageName, ...);
   *       }
   *   }
   * // Define and return the class
   *  return defineClass(name, classBytes, 0, classBytes.length);
   * </pre>
   *
   * <p><code>loadClass()</code> makes sure that the <code>Class</code>
   * returned by <code>findClass()</code> will later be returned by
   * <code>findLoadedClass()</code> when the same class name is requested.
   *
   * @param name class name to find (including the package name)
   * @return the requested Class
   * @throws ClassNotFoundException when the class can not be found
   * @since 1.2
   */
  protected Class<?> findClass(String name) throws ClassNotFoundException
  {
    throw new ClassNotFoundException(name);
  }

  /**
   * Helper to define a class using a string of bytes. This version is not
   * secure.
   *
   * @param data the data representing the classfile, in classfile format
   * @param offset the offset into the data where the classfile starts
   * @param len the length of the classfile data in the array
   * @return the class that was defined
   * @throws ClassFormatError if data is not in proper classfile format
   * @throws IndexOutOfBoundsException if offset or len is negative, or
   *         offset + len exceeds data
   * @deprecated use {@link #defineClass(String, byte[], int, int)} instead
   */
  protected final Class<?> defineClass(byte[] data, int offset, int len)
    throws ClassFormatError
  {
    return defineClass(null, data, offset, len);
  }

  /**
   * Helper to define a class using a string of bytes without a
   * ProtectionDomain. Subclasses should call this method from their
   * <code>findClass()</code> implementation. The name should use '.'
   * separators, and discard the trailing ".class".  The default protection
   * domain has the permissions of
   * <code>Policy.getPolicy().getPermissions(new CodeSource(null, null))</code>.
   *
   * @param name the name to give the class, or null if unknown
   * @param data the data representing the classfile, in classfile format
   * @param offset the offset into the data where the classfile starts
   * @param len the length of the classfile data in the array
   * @return the class that was defined
   * @throws ClassFormatError if data is not in proper classfile format
   * @throws IndexOutOfBoundsException if offset or len is negative, or
   *         offset + len exceeds data
   * @throws SecurityException if name starts with "java."
   * @since 1.1
   */
  protected final Class<?> defineClass(String name, byte[] data, int offset,
				       int len) throws ClassFormatError
  {
    checkInitialized();
    
    return FCClassLoader.defineClass(this, name, data, offset, len);
  }

  /**
   * Helper to define a class using the contents of a byte buffer. If
   * the domain is null, the default of
   * <code>Policy.getPolicy().getPermissions(new CodeSource(null,
   * null))</code> is used. Once a class has been defined in a
   * package, all further classes in that package must have the same
   * set of certificates or a SecurityException is thrown.
   *
   * @param name the name to give the class.  null if unknown
   * @param buf a byte buffer containing bytes that form a class.
   * @param domain the ProtectionDomain to give to the class, null for the
   *        default protection domain
   * @return the class that was defined
   * @throws ClassFormatError if data is not in proper classfile format
   * @throws NoClassDefFoundError if the supplied name is not the same as
   *                              the one specified by the byte buffer.
   * @throws SecurityException if name starts with "java.", or if certificates
   *         do not match up
   * @since 1.5
   */
  protected final Class<?> defineClass(String name, ByteBuffer buf)
    throws ClassFormatError
  {
    byte[] data = new byte[buf.remaining()];
    buf.get(data);
    return defineClass(name, data, 0, data.length);
  }

  /**
   * Links the class, if that has not already been done. Linking basically
   * resolves all references to other classes made by this class.
   *
   * @param c the class to resolve
   * @throws NullPointerException if c is null
   * @throws LinkageError if linking fails
   */
  protected final void resolveClass(Class<?> c)
  {
    checkInitialized();
    FCClassLoader.resolveClass(c);
  }

  /**
   * Helper to find a Class using the system classloader, possibly loading it.
   * A subclass usually does not need to call this, if it correctly
   * overrides <code>findClass(String)</code>.
   *
   * @param name the name of the class to find
   * @return the found class
   * @throws ClassNotFoundException if the class cannot be found
   */
  protected final Class<?> findSystemClass(String name)
    throws ClassNotFoundException
  {
    checkInitialized();
    return Class.forName(name, false, StaticData.systemClassLoader);
  }

  /**
   * Returns the parent of this classloader. If the parent of this
   * classloader is the bootstrap classloader then this method returns
   * <code>null</code>. A security check may be performed on
   * <code>RuntimePermission("getClassLoader")</code>.
   *
   * @return the parent <code>ClassLoader</code>
   * @throws SecurityException if the security check fails
   * @since 1.2
   */
  public final ClassLoader getParent()
  {
    // Check if we may return the parent classloader.
   
    return parent;
  }

  /**
   * Helper to find an already-loaded class in this ClassLoader.
   *
   * @param name the name of the class to find
   * @return the found Class, or null if it is not found
   * @since 1.1
   */
  protected final synchronized Class<?> findLoadedClass(String name)
  {
    checkInitialized();
    return FCClassLoader.findLoadedClass(this, name);
  }

  /**
   * Returns the system classloader. The system classloader (also called
   * the application classloader) is the classloader that is used to
   * load the application classes on the classpath (given by the system
   * property <code>java.class.path</code>. This is set as the context
   * class loader for a thread. The system property
   * <code>java.system.class.loader</code>, if defined, is taken to be the
   * name of the class to use as the system class loader, which must have
   * a public constructor which takes a ClassLoader as a parent. The parent
   * class loader passed in the constructor is the default system class
   * loader.
   *
   * <p>Note that this is different from the bootstrap classloader that
   * actually loads all the real "system" classes.
   *
   * <p>A security check will be performed for
   * <code>RuntimePermission("getClassLoader")</code> if the calling class
   * is not a parent of the system class loader.
   *
   * @return the system class loader
   * @throws SecurityException if the security check fails
   * @throws IllegalStateException if this is called recursively
   * @throws Error if <code>java.system.class.loader</code> fails to load
   * @since 1.2
   */
  public static ClassLoader getSystemClassLoader()
  {
    // Check if we may return the system classloader
   
    return StaticData.systemClassLoader;
  }

  /**
   * Defines a new package and creates a Package object. The package should
   * be defined before any class in the package is defined with
   * <code>defineClass()</code>. The package should not yet be defined
   * before in this classloader or in one of its parents (which means that
   * <code>getPackage()</code> should return <code>null</code>). All
   * parameters except the <code>name</code> of the package may be
   * <code>null</code>.
   *
   * <p>Subclasses should call this method from their <code>findClass()</code>
   * implementation before calling <code>defineClass()</code> on a Class
   * in a not yet defined Package (which can be checked by calling
   * <code>getPackage()</code>).
   *
   * @param name the name of the Package
   * @param specTitle the name of the specification
   * @param specVendor the name of the specification designer
   * @param specVersion the version of this specification
   * @param implTitle the name of the implementation
   * @param implVendor the vendor that wrote this implementation
   * @param implVersion the version of this implementation
   * @param sealed if sealed the origin of the package classes
   * @return the Package object for the specified package
   * @throws IllegalArgumentException if the package name is null or it
   *         was already defined by this classloader or one of its parents
   * @see Package
   * @since 1.2
   */
  

  /**
   * Returns the Package object for the requested package name. It returns
   * null when the package is not defined by this classloader or one of its
   * parents.
   *
   * @param name the package name to find
   * @return the package, if defined
   * @since 1.2
   */
 

  /**
   * Returns all Package objects defined by this classloader and its parents.
   *
   * @return an array of all defined packages
   * @since 1.2
   */
 
  /**
   * Called by <code>Runtime.loadLibrary()</code> to get an absolute path
   * to a (system specific) library that was requested by a class loaded
   * by this classloader. The default implementation returns
   * <code>null</code>. It should be implemented by subclasses when they
   * have a way to find the absolute path to a library. If this method
   * returns null the library is searched for in the default locations
   * (the directories listed in the <code>java.library.path</code> system
   * property).
   *
   * @param name the (system specific) name of the requested library
   * @return the full pathname to the requested library, or null
   * @see Runtime#loadLibrary(String)
   * @since 1.2
   */
  protected String findLibrary(String name)
  {
    return null;
  }

  /**
   * Set the default assertion status for classes loaded by this classloader,
   * used unless overridden by a package or class request.
   *
   * @param enabled true to set the default to enabled
   * @see #setClassAssertionStatus(String, boolean)
   * @see #setPackageAssertionStatus(String, boolean)
   * @see #clearAssertionStatus()
   * @since 1.4
   */
  public void setDefaultAssertionStatus(boolean enabled)
  {
    defaultAssertionStatus = enabled;
  }
  
  /**
   * Set the default assertion status for packages, used unless overridden
   * by a class request. This default also covers subpackages, unless they
   * are also specified. The unnamed package should use null for the name.
   *
   * @param name the package (and subpackages) to affect
   * @param enabled true to set the default to enabled
   * @see #setDefaultAssertionStatus(boolean)
   * @see #setClassAssertionStatus(String, boolean)
   * @see #clearAssertionStatus()
   * @since 1.4
   */
 
  
  /**
   * Return true if this loader is either the specified class loader
   * or an ancestor thereof.
   * @param loader the class loader to check
   */
  final boolean isAncestorOf(ClassLoader loader)
  {
    while (loader != null)
      {
	if (this == loader)
	  return true;
	loader = loader.parent;
      }
    return false;
  }

  

  /**
   * Before doing anything "dangerous" please call this method to make sure
   * this class loader instance was properly constructed (and not obtained
   * by exploiting the finalizer attack)
   * @see #initialized
   */
  private void checkInitialized()
  {
    if (! initialized)
      throw new SecurityException("attempt to use uninitialized class loader");
  }

}
