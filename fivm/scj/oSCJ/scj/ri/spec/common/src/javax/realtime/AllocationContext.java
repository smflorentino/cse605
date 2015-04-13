/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010
 *   @authors  Lei Zhao, Ales Plsek
 */
package javax.realtime;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import java.lang.reflect.InvocationTargetException;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;

/**
 * This is the base interface for all memory areas.  It is a generalization
 * of the Java Heap to allow for alternate forms of memory management.  All
 * memory areas implement this interface.
 *
 *
 */
@SCJAllowed
public interface AllocationContext
{
  /**
   * Make this memory area the allocation context of the current Schedulable
   * for the duration of the execution of the run() method of the instance of
   * Runnable given in the constructor.  During this period of execution, this
   * memory area becomes the default allocation context until another allocation
   * context is selected using {@link #enter}, or
   * {@link #executeInArea(Runnable)}
   * or the runnable's run method exits.
   *
   * @param logic is the {@link Runnable} which defines what actions to be
   *        carried out in the memory area.
   *
   * @throws IllegalThreadStateException when the caller is a Java thread.
   *
   * @throws IllegalArgumentException when the caller is a schedulable object
   *         and no nonnull value for logic was supplied when the memory
   *         area was constructed.
   * @throws ThrowBoundaryError when the JVM needs to propagate an exception
   *         allocated in this scope to (or through) the memory area of
   *         the caller.  Storing a reference to that exception would
   *         cause an {@link IllegalAssignmentError}, so the JVM cannot
   *         be permitted to deliver the exception.  The exception is
   *         allocated in the current allocation context and contains
   *         information about the exception it replaces.
   */
    @SCJAllowed(INFRASTRUCTURE)
  public void enter(Runnable logic)
   throws IllegalThreadStateException,
          IllegalArgumentException,
          ThrowBoundaryError;

  /**
   * Get the amount of allocated memory in this memory area.
   *
   * @return the amount of memory consumed.
   */
    @SCJAllowed
  public long memoryConsumed();

  /**
   * Get the amount of memory available for allocation in this memory area.
   *
   * @return the amount of memory remaining.
   */
    @SCJAllowed
  public long memoryRemaining();

  /**
   * Create a new array of the given type in this memory area.  This method may
   * be concurrently used by multiple threads.
   *
   * @param type is the class of object this memory area should hold.
   *        An array of a primitive type can be created using a type
   *        such as Integer.TYPE, which would create an array of the int
   *        type.
   *
   * @param number is the number of elements the array should have.
   *
   * @return the new array of type <code>type</code> and size
   *         <code>number</code>.
   *
   * @throws IllegalArgumentException when <code>number</code> is less than
   *                                  zero.
 * @throws IllegalAccessException
   *
   *
   */
  @RunsIn(CALLER)
  @SCJAllowed
  public Object newArray(Class type, int number)
    throws IllegalArgumentException, IllegalAccessException;

  /**
   * Create a new instance of a class in this memory area using its default
   * constructor.
   *
   * @param type is the class of the object to be created
   *
   * @return a new instance of the given class.
   *
   * @throws ExceptionInInitializerError when an unexpected exception has
   *         occurred in a static initializer.
   *
   * @throws IllegalAccessException when the class or initializer is
   *         inaccessible under Java access control.
   *
   * @throws InstantiationException when the specified class object could not
   *         be instantiated.  Possible causes are the class is an
   *         interface, abstract class, or array.
   *
   * @throws InvocationTargetException when the underlying constructor throws
   *         an exception.
   *
  public Object newInstance(Class type)
    throws ExceptionInInitializerError,
           IllegalAccessException,
           InstantiationException,
           InvocationTargetException;
*/
  /**
   * Create a new instance of a class in this memory area using the chosen
   * constructor.
   *
   * @param constructor to use.
   *
   * @param arguments the arguments required by the chosen constructor.
   *
   * @return the new object.
   *
   * @throws ExceptionInInitializerError when an unexpected exception has
   *         occurred in a static initializer.
   *
   * @throws IllegalAccessException when the class or initializer is
   *         inaccessible under Java access control.
   *
   * @throws IllegalArgumentException when constructor is null, or the argument
   *         array does not contain the number of arguments required by
   *         constructor.  A null value for arguments is treated like an
   *         array of length 0.
   *
   * @throws InstantiationException when the specified class object could not
   *         be instantiated.  Possible causes are the class is an
   *         interface, abstract class, or array.
   *
   * @throws InvocationTargetException when the underlying constructor throws
   *         an exception.
   *
  public Object newInstance(Constructor constructor, Object[] arguments)
    throws ExceptionInInitializerError,
           IllegalAccessException,
           IllegalArgumentException,
           InstantiationException,
           InvocationTargetException;
*/
  /**
   * Get the size of this memory area.
   *
   * @return the current size of this memory area.
   */
  @SCJAllowed
  public long size();

  /**
   * Execute some logic with this memory area as the default allocation context.
   * The effect on the scope stack is specified in the implementing classes.
   *
   * @param logic is the runnable to execute in this memory area.
   *
  public void executeInArea(Runnable logic);
*/
  /**
   * Perform an action on all children scopes of this memory area, so long as
   * the {@link ChildScopeVisitor#visit(ScopedAllocationContext)} method
   * returns null.  When that method returns an object, the visit is terminated
   * and that object is returned by this method,
   * <p>
   * The set of children may be concurrently modified by other tasks, but the
   * view seen by the visitor might not be updated to reflect those changes.
   * The guarantees when the set is disturbed by other tasks are
   * <ul>
   * <li> the visitor shall visit no member more than once,</li>
   * <li> it shall visit only scopes that were a member of the set at some time
   *      during the enumeration of the set, and
   * <li> it shall visit all the scopes that are not deleted during the
   *      execution of the visitor.
   * </ul>
   *
   * @param visitor determines the action to be performed on each of the
   *        children scopes.
   *
   * @return null when all elements where visited and some object when the
   *         visit is forced to terminate at the end of visiting some
   *         element.
   *
   * @throws IllegalArgumentException when visitor is null.
   *
   * @throws RuntimeException when the visitor method throws such an exception
   *         or any runtime error, it is thrown here; thereby
   *         terminating the visit.
   *
  public Object visitScopedChildren(ChildScopeVisitor visitor)
    throws IllegalArgumentException, RuntimeException;
    */

  /**
   * Execute logic with this memory area as the current allocation context.
   */
  @SCJAllowed(INFRASTRUCTURE)
  public void executeInArea(Runnable logic) ;

  /**
   * Create an object of class type in this memory area.
   * @param type
   * @return
 * @throws InstantiationException
 * @throws IllegalAccessException
   */
  @RunsIn(CALLER)
  @SCJAllowed
  public Object newInstance(Class type) throws IllegalAccessException, InstantiationException,
  	ExceptionInInitializerError, InvocationTargetException;


}

