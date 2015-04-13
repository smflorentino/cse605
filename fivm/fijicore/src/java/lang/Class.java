/* Class.java -- Representation of a Java class.
   Copyright (C) 1998, 1999, 2000, 2002, 2003, 2004, 2005, 2006, 2007
   Free Software Foundation

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

import gnu.classpath.FCStackWalker;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;


/**
 * A Class represents a Java type.  There will never be multiple Class
 * objects with identical names and ClassLoaders. Primitive types, array
 * types, and void also have a Class object.
 *
 * <p>Arrays with identical type and number of dimensions share the same class.
 * The array class ClassLoader is the same as the ClassLoader of the element
 * type of the array (which can be null to indicate the bootstrap classloader).
 * The name of an array class is <code>[&lt;signature format&gt;;</code>.
 * <p> For example,
 * String[]'s class is <code>[Ljava.lang.String;</code>. boolean, byte,
 * short, char, int, long, float and double have the "type name" of
 * Z,B,S,C,I,J,F,D for the purposes of array classes.  If it's a
 * multidimensioned array, the same principle applies:
 * <code>int[][][]</code> == <code>[[[I</code>.
 *
 * <p>There is no public constructor - Class objects are obtained only through
 * the virtual machine, as defined in ClassLoaders.
 *
 * @serialData Class objects serialize specially:
 * <code>TC_CLASS ClassDescriptor</code>. For more serialization information,
 * see {@link ObjectStreamClass}.
 *
 * @author John Keiser
 * @author Eric Blake (ebb9@email.byu.edu)
 * @author Tom Tromey (tromey@redhat.com)
 * @author Andrew John Hughes (gnu_andrew@member.fsf.org)
 * @since 1.0
 * @see ClassLoader
 */
public final class Class<T> 
  implements Serializable
{
  /**
   * Compatible with JDK 1.0+.
   */
  private static final long serialVersionUID = 3206093459760846163L;

  /**
   * Flag indicating a synthetic member.
   * Note that this duplicates a constant in Modifier.
   */
  private static final int SYNTHETIC = 0x1000;

  /**
   * Flag indiciating an annotation class.
   */
  private static final int ANNOTATION = 0x2000;

  /**
   * Flag indicating an enum constant or an enum class.
   * Note that this duplicates a constant in Modifier.
   */
  private static final int ENUM = 0x4000;

  final transient Object vmdata;

  /**
   * Class is non-instantiable from Java code; only the VM can create
   * instances of this class.
   */
  Class(Object vmdata)
  {
      this.vmdata=vmdata;
  }

  /**
   * Use the classloader of the current class to load, link, and initialize
   * a class. This is equivalent to your code calling
   * <code>Class.forName(name, true, getClass().getClassLoader())</code>.
   *
   * @param name the name of the class to find
   * @return the Class object representing the class
   * @throws ClassNotFoundException if the class was not found by the
   *         classloader
   * @throws LinkageError if linking the class fails
   * @throws ExceptionInInitializerError if the class loads, but an exception
   *         occurs during initialization
   */
  public static Class<?> forName(String name) throws ClassNotFoundException
  {
    return FCClass.forName(name, true, FCStackWalker.getCallingClassLoader());
  }

  /**
   * Use the specified classloader to load and link a class. If the loader
   * is null, this uses the bootstrap class loader (provide the security
   * check succeeds). Unfortunately, this method cannot be used to obtain
   * the Class objects for primitive types or for void, you have to use
   * the fields in the appropriate java.lang wrapper classes.
   *
   * <p>Calls <code>classloader.loadclass(name, initialize)</code>.
   *
   * @param name the name of the class to find
   * @param initialize whether or not to initialize the class at this time
   * @param classloader the classloader to use to find the class; null means
   *        to use the bootstrap class loader
   *
   * @return the class object for the given class
   *
   * @throws ClassNotFoundException if the class was not found by the
   *         classloader
   * @throws LinkageError if linking the class fails
   * @throws ExceptionInInitializerError if the class loads, but an exception
   *         occurs during initialization
   * @throws SecurityException if the <code>classloader</code> argument
   *         is <code>null</code> and the caller does not have the
   *         <code>RuntimePermission("getClassLoader")</code> permission
   * @see ClassLoader
   * @since 1.2
   */
  public static Class<?> forName(String name, boolean initialize,
				 ClassLoader classloader)
    throws ClassNotFoundException
  {
    
    return (Class<?>) FCClass.forName(name, initialize, classloader);
  }
  
  /**
   * Get the ClassLoader that loaded this class.  If the class was loaded
   * by the bootstrap classloader, this method will return null.
   * If there is a security manager, and the caller's class loader is not
   * an ancestor of the requested one, a security check of
   * <code>RuntimePermission("getClassLoader")</code>
   * must first succeed. Primitive types and void return null.
   *
   * @return the ClassLoader that loaded this class
   * @throws SecurityException if the security check fails
   * @see ClassLoader
   * @see RuntimePermission
   */
  public ClassLoader getClassLoader()
  {
    if (isPrimitive())
      return null;

    ClassLoader loader = FCClass.getClassLoader(this);
    // Check if we may get the classloader
    
    return loader;
  }

  /**
   * If this is an array, get the Class representing the type of array.
   * Examples: "[[Ljava.lang.String;" would return "[Ljava.lang.String;", and
   * calling getComponentType on that would give "java.lang.String".  If
   * this is not an array, returns null.
   *
   * @return the array type of this class, or null
   * @see Array
   * @since 1.1
   */
  public Class<?> getComponentType()
  {
    return FCClass.getComponentType (this);
  }

  /**
   * Get the interfaces this class <em>directly</em> implements, in the
   * order that they were declared. This returns an empty array, not null,
   * for Object, primitives, void, and classes or interfaces with no direct
   * superinterface. Array types return Cloneable and Serializable.
   *
   * @return the interfaces this class directly implements
   */
  public Class<?>[] getInterfaces()
  {
    return FCClass.getInterfaces (this);
  }

  /**
   * Get the modifiers of this class.  These can be decoded using Modifier,
   * and is limited to one of public, protected, or private, and any of
   * final, static, abstract, or interface. An array class has the same
   * public, protected, or private modifier as its component type, and is
   * marked final but not an interface. Primitive types and void are marked
   * public and final, but not an interface.
   *
   * @return the modifiers of this class
   * @see Modifier
   * @since 1.1
   */
  public int getModifiers()
  {
    int mod = FCClass.getModifiers (this, false);
    return (mod & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
          Modifier.FINAL | Modifier.STATIC | Modifier.ABSTRACT |
          Modifier.INTERFACE));
  }
  
  /**
   * Get the name of this class, separated by dots for package separators.
   * If the class represents a primitive type, or void, then the
   * name of the type as it appears in the Java programming language
   * is returned.  For instance, <code>Byte.TYPE.getName()</code>
   * returns "byte".
   *
   * Arrays are specially encoded as shown on this table.
   * <pre>
   * array type          [<em>element type</em>
   *                     (note that the element type is encoded per
   *                      this table)
   * boolean             Z
   * byte                B
   * char                C
   * short               S
   * int                 I
   * long                J
   * float               F
   * double              D
   * void                V
   * class or interface, alone: &lt;dotted name&gt;
   * class or interface, as element type: L&lt;dotted name&gt;;
   * </pre>
   *
   * @return the name of this class
   */
  public String getName()
  { 
    return FCClass.getName (this);
  }

  /**
   * Get a resource URL using this class's package using the
   * getClassLoader().getResource() method.  If this class was loaded using
   * the system classloader, ClassLoader.getSystemResource() is used instead.
   *
   * <p>If the name you supply is absolute (it starts with a <code>/</code>),
   * then the leading <code>/</code> is removed and it is passed on to
   * getResource(). If it is relative, the package name is prepended, and
   * <code>.</code>'s are replaced with <code>/</code>.
   *
   * <p>The URL returned is system- and classloader-dependent, and could
   * change across implementations.
   *
   * @param resourceName the name of the resource, generally a path
   * @return the URL to the resource
   * @throws NullPointerException if name is null
   * @since 1.1
   */


  /**
   * Get the direct superclass of this class.  If this is an interface,
   * Object, a primitive type, or void, it will return null. If this is an
   * array type, it will return Object.
   *
   * @return the direct superclass of this class
   */
  public Class<? super T> getSuperclass()
  {
    return FCClass.getSuperclass (this);
  }
  
  /**
   * Return whether this class is an array type.
   *
   * @return whether this class is an array type
   * @since 1.1
   */
  public boolean isArray()
  {
    return FCClass.isArray (this);
  }
  
  /**
   * Discover whether an instance of the Class parameter would be an
   * instance of this Class as well.  Think of doing
   * <code>isInstance(c.newInstance())</code> or even
   * <code>c.newInstance() instanceof (this class)</code>. While this
   * checks widening conversions for objects, it must be exact for primitive
   * types.
   *
   * @param c the class to check
   * @return whether an instance of c would be an instance of this class
   *         as well
   * @throws NullPointerException if c is null
   * @since 1.1
   */
  public boolean isAssignableFrom(Class<?> c)
  {
    return FCClass.isAssignableFrom (this, c);
  }
 
  /**
   * Discover whether an Object is an instance of this Class.  Think of it
   * as almost like <code>o instanceof (this class)</code>.
   *
   * @param o the Object to check
   * @return whether o is an instance of this class
   * @since 1.1
   */
  public boolean isInstance(Object o)
  {
    return FCClass.isInstance (this, o);
  }
  
  /**
   * Check whether this class is an interface or not.  Array types are not
   * interfaces.
   *
   * @return whether this class is an interface or not
   */
  public boolean isInterface()
  {
    return FCClass.isInterface (this);
  }
  
  /**
   * Return whether this class is a primitive type.  A primitive type class
   * is a class representing a kind of "placeholder" for the various
   * primitive types, or void.  You can access the various primitive type
   * classes through java.lang.Boolean.TYPE, java.lang.Integer.TYPE, etc.,
   * or through boolean.class, int.class, etc.
   *
   * @return whether this class is a primitive type
   * @see Boolean#TYPE
   * @see Byte#TYPE
   * @see Character#TYPE
   * @see Short#TYPE
   * @see Integer#TYPE
   * @see Long#TYPE
   * @see Float#TYPE
   * @see Double#TYPE
   * @see Void#TYPE
   * @since 1.1
   */
  public boolean isPrimitive()
  {
    return FCClass.isPrimitive (this);
  }
  
  /**
   * Return the human-readable form of this Object.  For an object, this
   * is either "interface " or "class " followed by <code>getName()</code>,
   * for primitive types and void it is just <code>getName()</code>.
   *
   * @return the human-readable form of this Object
   */
  public String toString()
  {
    if (isPrimitive())
      return getName();
    return (isInterface() ? "interface " : "class ") + getName();
  }

  /**
   * Returns the desired assertion status of this class, if it were to be
   * initialized at this moment. The class assertion status, if set, is
   * returned; the backup is the default package status; then if there is
   * a class loader, that default is returned; and finally the system default
   * is returned. This method seldom needs calling in user code, but exists
   * for compilers to implement the assert statement. Note that there is no
   * guarantee that the result of this method matches the class's actual
   * assertion status.
   *
   * @return the desired assertion status
   * @see ClassLoader#setClassAssertionStatus(String, boolean)
   * @see ClassLoader#setPackageAssertionStatus(String, boolean)
   * @see ClassLoader#setDefaultAssertionStatus(boolean)
   * @since 1.4
   */
  public boolean desiredAssertionStatus()
  {
    
      return FCClassLoader.defaultAssertionStatus();
    
  }

  /**
   * <p>
   * Casts this class to represent a subclass of the specified class.
   * This method is useful for `narrowing' the type of a class so that
   * the class object, and instances of that class, can match the contract
   * of a more restrictive method.  For example, if this class has the
   * static type of <code>Class&lt;Object&gt;</code>, and a dynamic type of
   * <code>Class&lt;Rectangle&gt;</code>, then, assuming <code>Shape</code> is
   * a superclass of <code>Rectangle</code>, this method can be used on
   * this class with the parameter, <code>Class&lt;Shape&gt;</code>, to retain
   * the same instance but with the type
   * <code>Class&lt;? extends Shape&gt;</code>.
   * </p>
   * <p>
   * If this class can be converted to an instance which is parameterised
   * over a subtype of the supplied type, <code>U</code>, then this method
   * returns an appropriately cast reference to this object.  Otherwise,
   * a <code>ClassCastException</code> is thrown.
   * </p>
   * 
   * @param klass the class object, the parameterized type (<code>U</code>) of
   *              which should be a superclass of the parameterized type of
   *              this instance.
   * @return a reference to this object, appropriately cast.
   * @throws ClassCastException if this class can not be converted to one
   *                            which represents a subclass of the specified
   *                            type, <code>U</code>. 
   * @since 1.5
   */
  public <U> Class<? extends U> asSubclass(Class<U> klass)
  {
    if (! klass.isAssignableFrom(this))
      throw new ClassCastException();
    return (Class<? extends U>) this;
  }

  /**
   * Returns the specified object, cast to this <code>Class</code>' type.
   *
   * @param obj the object to cast
   * @throws ClassCastException  if obj is not an instance of this class
   * @since 1.5
   */
  public T cast(Object obj)
  {
    if (obj != null && ! isInstance(obj))
      throw new ClassCastException();
    return (T) obj;
  }

  /**
   * Strip the last portion of the name (after the last dot).
   *
   * @param name the name to get package of
   * @return the package name, or "" if no package
   */
  private static String getPackagePortion(String name)
  {
    int lastInd = name.lastIndexOf('.');
    if (lastInd == -1)
      return "";
    return name.substring(0, lastInd);
  }

  /**
   * Returns the enumeration constants of this class, or
   * null if this class is not an <code>Enum</code>.
   *
   * @return an array of <code>Enum</code> constants
   *         associated with this class, or null if this
   *         class is not an <code>enum</code>.
   * @since 1.5
   */
  public T[] getEnumConstants()
  {
    if (isEnum())
      {
	  throw new Error("not implemented!");
      }
    else
      {
	return null;
      }
  }

  /**
   * Returns true if this class is an <code>Enum</code>.
   *
   * @return true if this is an enumeration class.
   * @since 1.5
   */
  public boolean isEnum()
  {
    int mod = FCClass.getModifiers (this, true);
    return (mod & ENUM) != 0;
  }

  /**
   * Returns true if this class is a synthetic class, generated by
   * the compiler.
   *
   * @return true if this is a synthetic class.
   * @since 1.5
   */
  public boolean isSynthetic()
  {
    int mod = FCClass.getModifiers (this, true);
    return (mod & SYNTHETIC) != 0;
  }

  /**
   * Returns true if this class is an <code>Annotation</code>.
   *
   * @return true if this is an annotation class.
   * @since 1.5
   */
  public boolean isAnnotation()
  {
    int mod = FCClass.getModifiers (this, true);
    return (mod & ANNOTATION) != 0;
  }

  /**
   * Returns the simple name for this class, as used in the source
   * code.  For normal classes, this is the content returned by
   * <code>getName()</code> which follows the last ".".  Anonymous
   * classes have no name, and so the result of calling this method is
   * "".  The simple name of an array consists of the simple name of
   * its component type, followed by "[]".  Thus, an array with the 
   * component type of an anonymous class has a simple name of simply
   * "[]".
   *
   * @return the simple name for this class.
   * @since 1.5
   */
  public String getSimpleName()
  {
    return FCClass.getSimpleName(this);
  }

  /**
   * Get a new instance of this class by calling the no-argument constructor.
   * The class is initialized if it has not been already. A security check
   * may be performed, with <code>checkMemberAccess(this, Member.PUBLIC)</code>
   * as well as <code>checkPackageAccess</code> both having to succeed.
   *
   * @return a new instance of this class
   * @throws InstantiationException if there is not a no-arg constructor
   *         for this class, including interfaces, abstract classes, arrays,
   *         primitive types, and void; or if an exception occurred during
   *         the constructor
   * @throws IllegalAccessException if you are not allowed to access the
   *         no-arg constructor because of scoping reasons
   * @throws SecurityException if the security check fails
   * @throws ExceptionInInitializerError if class initialization caused by
   *         this call fails with an exception
   */
  public T newInstance()
    throws InstantiationException, IllegalAccessException
  {
      return (T)FCClass.newInstance(this);
  }
}
