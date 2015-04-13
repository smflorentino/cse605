/* VMThread -- VM interface for Thread of executable code
   Copyright (C) 2003, 2004, 2005, 2006 Free Software Foundation

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

/**
 * VM interface for Thread of executable code. Holds VM dependent state.
 * It is deliberately package local and final and should only be accessed
 * by the Thread class.
 * <p>
 * This is the GNU Classpath reference implementation, it should be adapted
 * for a specific VM.
 * <p>
 * The following methods must be implemented:
 * <ul>
 * <li>native void start(long stacksize);
 * <li>native void interrupt();
 * <li>native boolean isInterrupted();
 * <li>native void suspend();
 * <li>native void resume();
 * <li>native void nativeSetPriority(int priority);
 * <li>native void nativeStop(Throwable t);
 * <li>native static Thread currentThread();
 * <li>static native void yield();
 * <li>static native boolean interrupted();
 * </ul>
 * All other methods may be implemented to make Thread handling more efficient
 * or to implement some optional (and sometimes deprecated) behaviour. Default
 * implementations are provided but it is highly recommended to optimize them
 * for a specific VM.
 * 
 * @author Jeroen Frijters (jeroen@frijters.net)
 * @author Dalibor Topic (robilad@kaffe.org)
 */
final class FCNativeThread
{
   

   

    /**
     * Returns the number of stack frames in this Thread.
     * Will only be called when when a previous call to suspend() returned true.
     *
     * @deprecated unsafe operation
     */
    native int countStackFrames();

   
    /**
     * Create a native thread on the underlying platform and start it executing
     * on the run method of this object.
     * @param stacksize the requested size of the native thread stack
     */
    native void start(long stacksize);

    /**
     * Interrupt this thread.
     */
    native void interrupt();

    /**
     * Determine whether this Thread has been interrupted, but leave
     * the <i>interrupted status</i> alone in the process.
     *
     * @return whether the Thread has been interrupted
     */
    native boolean isInterrupted();

    /**
     * Suspend this Thread.  It will not come back, ever, unless it is resumed.
     */
    native void suspend();

    /**
     * Resume this Thread.  If the thread is not suspended, this method does
     * nothing.
     */
    native void resume();

    /**
     * Set the priority of the underlying platform thread.
     *
     * @param priority the new priority
     */
    native void nativeSetPriority(int priority);

    /**
     * Asynchronously throw the specified throwable in this Thread.
     *
     * @param t the exception to throw
     */
    native void nativeStop(Throwable t);

    /**
     * Return the Thread object associated with the currently executing
     * thread.
     *
     * @return the currently executing Thread
     */
    static native Thread currentThread();

    /**
     * Yield to another thread. The Thread will not lose any locks it holds
     * during this time. There are no guarantees which thread will be
     * next to run, and it could even be this one, but most VMs will choose
     * the highest priority thread that has been waiting longest.
     */
    static native void yield();

   


    /**
     * Determine whether the current Thread has been interrupted, and clear
     * the <i>interrupted status</i> in the process.
     *
     * @return whether the current Thread has been interrupted
     */
    static native boolean interrupted();


  /**
   * Returns the current state of the thread.
   * The value must be one of "BLOCKED", "NEW",
   * "RUNNABLE", "TERMINATED", "TIMED_WAITING" or
   * "WAITING".
   *
   * @return a string corresponding to one of the 
   *         thread enumeration states specified above.
   */
  native String getState();

}
