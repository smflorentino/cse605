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


package edu.purdue.scj;

public class VMSupport {

    /*------ Memory ------*/

    /**
     * Enter and run the "logic" in "store". The "store" must be either a scoped
     * memory just created by pushScope() or the immortal memory.
     * 
     * The thread that performs enter() to a scoped memory must be SCJ thread (a
     * thread with SCJ specific features). For the thread entering the immortal
     * memory, it can be a regular Java thread. But note, this is only the case
     * during the start-up phase, which is the period after the main method of
     * the Launcher has been executed and before the very first SCJ thread runs.
     */
    public static native void enter(BackingStoreID store, Runnable logic);

    /**
     * Create a new scope with "size" byte on top of the current thread's
     * backing store stack. Return the ID of the new scope.
     */
    public static native BackingStoreID pushScope(long size);

    /**
     * Pop the scope on top of the current thread's backing store stack. Return
     * the ID.
     */
    public static native void popScope();

    /** Get the object associated with the scope */
    public static native Object getNote(BackingStoreID scope);

    /** Associate the note with scope */
    public static native void setNote(BackingStoreID scope, Object note);

    /** Get the portal associated with the scope */
    public static native Object getPortal(BackingStoreID scope);

    /** Set the portal associated with the scope */
    public static native void setPortal(BackingStoreID scope, Object portal);

    /**
     * Set the scope as current allocation space and return the previous one.
     * The "scope" can be on any one of the currently active threads' scope
     * stack or be the immortal memory.
     */
    public static native BackingStoreID setCurrentArea(BackingStoreID scope);

    /** Get the ID of the scope which serves as current allocation space. */
    public static native BackingStoreID getCurrentArea();

    /** Get the ID of immortal memory */
    public static native BackingStoreID getImmortalArea();

    /** Get the scope where object ref was allocated in */
    public static native BackingStoreID areaOf(Object ref);

    /** As the name suggests */
    public static native long getScopeSize(BackingStoreID scope);

    /** As the name suggests */
    public static native long memoryConsumed(BackingStoreID scope);

    /** As the name suggests */
    public static native long memoryRemaining(BackingStoreID scope);

    // /**
    // * Create and push a "size" byte large scope on the ScopeStack stack of
    // * thread t. Return the ID.
    // */
    // public static native BackingStoreID pushScopeInStackSpace(Thread t,
    // long size);
    //
    // /** Pop the top of thread t's ScopeStack stack. */
    // public static native void popScopeInStackSpace(Thread t);

    /*------ Thread ------*/

    /** the minimum RT priority available for application threads */
    public static native int getMinRTPriority();

    /** the maximum RT priority available for application threads */
    public static native int getMaxRTPriority();
    
    /** set the priority of a thread (works either before, or after, the thread is started) */
    public static native void setThreadPriority(Thread t,int priority);

    /** Not used now */
    public static native void setInterruptable(boolean set);

    /** Not used now */
    public static native void threadSetInterrupted(Thread t, boolean set);

    /**
     * @return 0 if succeed; -1 if sleep time was in the past; 1 if sleep was
     *         interrupted.
     */
    public static native int delayCurrentThreadAbsolute(long nanos);

    /*------ Per-Thread Parameters ------*/

    public static native void setTotalBackingStore(Thread t, long size);

    /** Not used now */
    public static native void setNativeStackSize(Thread t, long size);

    /** Not used now */
    public static native void setJavaStackSize(Thread t, long size);

    /*------ Time -------*/

    /** In nanosecond. */
    public static native long getCurrentTime();

    /** In nanosecond. */
    public static native long getClockResolution();

    /*------ PCE -------*/

    /** Not used now */
    public static native int getPriorityCeiling(Object obj);

    /** Not used now */
    public static native void setPriorityCeiling(Object obj, int ceiling);

    /*------ Size Of -------*/

    public static native long sizeOf(Class clazz);

    public static native long sizeOfReferenceArray(int length);

    public static native long sizeOfPrimitiveArray(int length, Class clazz);

    // static native long sizeOfMonitor();

    /*------ Async Events & Interrupt Handling-------*/

    // static native void waitForInterrupted(int interruptIndex);
    //
    // static native void registerInterruptHandler();
    //	  
    // static native void interruptServed(int interruptIndex);
    //	  
    // static native boolean isMonitoredInterrupt(int interruptIndex);
    //	  
    // static native boolean stopMonitoringInterrupt(int interruptIndex);
    //	  
    // static native boolean startMonitoringInterrupt(int interruptIndex);
    /*------ Debug -------*/
    // TBA
}
