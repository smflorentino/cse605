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
     * TODO: The thread that performs enter() to a scoped memory must be SCJ thread (a
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
     * Pop the scope on top of the current thread's backing store stack. 
     * 
     * The recent change to move the BackingStoreID for a fivmr_MemoryArea into
     * the memory allocated for that scope had an unfortunate effect on the
     * VMSupport.pop() method. This method returns the BackingStoreID for the
     * scope which was just popped -- which is no longer valid. popScope now returns void.
     */
    public static native void popScope();
	
    /** Get the object associated with the scope */
    public static native Object getNote(BackingStoreID scope);
	
    /** Associate the note with scope */
    public static native void setNote(BackingStoreID scope, Object note);
	
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
	
    
    /** sets the RT priority to a thread */
    public static native void setThreadPriority(Thread t, int p);
    
    
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
	
    /** In nanosecond. XXX: this does not hold for RTEMS board! 
     *   Time in milliseconds is provided instead!*/
    public static native long getCurrentTime();
	
    /** In nanosecond. */
    public static native long getClockResolution();
	
    /** In nanosecond. XXX: This method call should work when you need a precision in nanoseconds. */
    public static native long getCurrentTimePrecise();
    
    
    
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
	
    // //////////////////////////////////////////////////////////////////////////////////////////////
    // //////////////////////
    // //////////////////////
	
    /* Rapita Support */
	
    public static native void RPT_Init();
	
    public static native void RPT_Ipoint(int i);
	
    public static native void RPT_Output_Trace();
	
    // //////////////////////////////////////////////////////////////////////////////////////////////
    // //////////////////////
    // //////////////////////
	
    /* RawMemory Access */
	
    public static native int open(String filename, int flags, int mode);
    
    public static native void close(int address);
    
    public static native int readByte(int address);
    
    public static native byte getByte(long address, long offset);
    public static native int setByte(long address, long offset, byte value);
    
    public static native int writeByte(int address, int b);
    
    public static native int mmap(long address, long len, int prot, int flags, int fd, long off);
    
    /* RawMemory Access - the original implementation*/
    
    
    
    /**
     * Get the byte at the given address with an atomic load.
     * 
     * @param address
     *            address of the byte to read
     * @return The byte at the given address
     */
    public static native byte getByteAtomic(long address);
	
    /**
     * Set the byte at the given address with an atomic store.
     * 
     * @param address
     *            address of the byte to write
     * @param value
     *            Value to write.
     */
    public static native void setByteAtomic(long address, long value);
	
    /**
     * Get the short at the given address with an atomic load.
     * 
     * @param address
     *            address of the short to read
     * @return The short at the given address
     */
    public static native short getShortAtomic(long address);
	
    /**
     * Set the short at the given address with an atomic store.
     * 
     * @param address
     *            address of the short to write
     * @param value
     *            Value to write.
     */
    public static native void setShortAtomic(long address, short value);
	
    /**
     * Get the int at the given address with an atomic load.
     * 
     * @param address
     *            address of the int to read
     * @return The int at the given address
     */
    public static native int getIntAtomic(long address);
	
    /**
     * Set the int at the given address with an atomic store.
     * 
     * @param address
     *            address of the int to write
     * @param value
     *            Value to write.
     */
    public static native void setIntAtomic(long address, int value);
	
    /**
     * Get the long at the given address
     * 
     * @param address
     *            address of the long to read
     * @return The long at the given address
     */
    public static native long getLong(long address);
	
    /**
     * Set the long at the given address
     * 
     * @param address
     *            address of the long to write
     * @param value
     *            Value to write.
     */
    public static native void setLong(long address, long value);
	
    /**
     * Get the Float at the given address with an atomic load.
     * 
     * @param address
     *            address of the Float to read
     * @return The Float at the given address
     */
    public static native float getFloatAtomic(long address);
	
    /**
     * Set the Float at the given address with an atomic store.
     * 
     * @param address
     *            address of the Float to write
     * @param value
     *            Value to write.
     */
    public static native void setFloatAtomic(long address, float value);
	
    /**
     * Get the Double at the given address
     * 
     * @param address
     *            address of the Double to read
     * @return The Double at the given address
     */
    public static native double getDouble(long address);
	
    /**
     * Set the Double at the given address
     * 
     * @param address
     *            address of the Double to write
     * @param value
     *            Value to write.
     */
    public static native void setDouble(long address, double value);
	
    // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
}
