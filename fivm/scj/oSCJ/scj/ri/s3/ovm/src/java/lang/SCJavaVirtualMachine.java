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
 *   @author David Holmes (originally for OVM)
 *   @authors  Lei Zhao, Ales Plsek
 */

package java.lang;

import javax.realtime.ImmortalMemory;
import javax.realtime.NoHeapRealtimeThread;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;

import org.ovmj.java.Opaque;

import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

/**
 * Represents the SCJava Virtual Machine (SCJVM). This extends the
 * {@link JavaVirtualMachine} class to specialize certain aspects for use in a
 * SCJ configuration - such as dealing with no-heap threads and scoped memory.
 * <p>
 * {@inheritDoc}
 * 
 */
public class SCJavaVirtualMachine extends JavaVirtualMachine {

    /*
     * This is the entry point for the JVM at which time class initialization is
     * disabled. Consequently this class can not have any static initialization
     * requirements. At the time of writing, this class manages to escape static
     * initialization until VM shutdown commences!
     * 
     * The RealtimeLauncher invokes our init() method.
     */

   

    /** Direct construction of a JVM is not allowed */
    SCJavaVirtualMachine() {
    }

    /**
     * NOTE: This method can not print to output anything yet!!! 
     *       First, JavaVirtualMachine needs to be initialized!!
     * 
     * 
     */
    // we need to install our singleton instance as the JVM instance
    public static void init() {

        JavaVirtualMachine.init();

        Opaque current = VMSupport.getCurrentArea();
        VMSupport.setCurrentArea(VMSupport.getImmortalArea());
        JavaVirtualMachine.instance = new SCJavaVirtualMachine();

        Utils.debugPrintln("[SCJ] SCJavaVirtualMachine.init() done");
    }

    /** Overrides to start the shutdown thread */
    void initializeVMThreads() {
    	Utils.debugPrintln("[SCJ] SCJavaVirtualMachine.initializeVMThreads() started");

        Opaque current = VMSupport.getCurrentArea();
        VMSupport.setCurrentArea(VMSupport.getImmortalArea());

        Utils.debugPrintln("[SCJ] SCJavaVirtualMachine.initializeVMThreads() done");
    }

    /*
     * FIXME, should probably have a better way of syncing with the constants in
     * javax.realtime.RealtimeJavaDispatcher and in
     * s3.services.java.realtime.RealtimeJavaDispatcherImpl
     */
    protected int getSystemPriority_() {
        return 40;
    }
    
    /**
     * Returns the singleton instance of this class
     * 
     * @return the singleton instance of this class
     * 
     */
    public static SCJavaVirtualMachine getSCJInstance() {
        return (SCJavaVirtualMachine) instance;
    }
    
    /** The method that executes the shutdown sequence. 
     * This sequence is in SCJ executed by the main thread. In RTSJ/OVM there was a special "shutdownThread" for it. */
    public void shutdownSCJ() {
    	// must not hold lock on JVM instance when doing shutdown
        // otherwise the hook threads we join() with can't be started or
        // terminate
    	Utils.debugPrintln("[SCJ] ShutdownThread.run() done");
    	LibraryImports.printString("OVM shutdown initiated by ");
    	SCJavaVirtualMachine.super.performShutdown();
    }
    
}
