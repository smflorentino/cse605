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

import edu.purdue.scj.VMSupport;

class RealtimeJavaDispatcher {

    static final int VM_MAX_PRIORITY = VMSupport.getMaxRTPriority();
    static final int VM_MIN_PRIORITY = VMSupport.getMinRTPriority();
    static final int SYSTEM_PRIORITY = VM_MAX_PRIORITY;

    /* a value we keep neeeding all over the place with times */
    static final int NANOS_PER_MILLI = 1000 * 1000;

    /* these are kernel constants that we need to understand */

    /** Absolute sleep caused thread to block */
    static final int ABSOLUTE_NORMAL = 0;

    /** Absolute sleep time was in the past */
    static final int ABSOLUTE_PAST = -1;

    /** Absolute sleep was interrupted */
    static final int ABSOLUTE_INTERRUPTED = 1;

    /** The singleton instance of this class */
    static final RealtimeJavaDispatcher instance = new RealtimeJavaDispatcher();

    /**
     * Return the singleton instance of this class
     * 
     * @return the singleton instance of this class
     */
    static RealtimeJavaDispatcher getInstance() {
        return instance;
    }

    /** no construction allowed */
    private RealtimeJavaDispatcher() {
    }


    // /**
    // * Put the current thread to sleep until the specified time has passed
    // * or until the thread is interrupted.
    // *
    // * @param nanos the time to sleep until, expressed as nanoseconds since
    // * the epoch.
    // *
    // * @return <tt>true</tt> if the thread actually slept, and
    // * <tt>false</tt> if the specified time had already passed.
    // * @throws InterruptedException if the thread was interrupted, in which
    // * case the threads interrupt state is cleared.
    // */
    // boolean sleepAbsolute(long nanos) throws InterruptedException{
    // int rc = VMSupport.delayCurrentThreadAbsolute(nanos);
    // switch (rc) {
    // case ABSOLUTE_NORMAL: return true;
    // case ABSOLUTE_PAST: return false;
    // case ABSOLUTE_INTERRUPTED: {
    // RealtimeThread.currentRealtimeThread().clearInterrupt();
    // throw new InterruptedException();
    // }
    // default: throw new InternalError("invalid return code " + rc);
    // }
    // }

    /**
     * Put the current thread to sleep until the specified time has passed or
     * until the thread is interrupted, but throw no exception upon interrupt.
     * 
     * @param nanos
     *            the time to sleep until, expressed as nanoseconds since the
     *            epoch.
     * 
     * @return An integer code indicating why the call returned: ABSOLUTE_NORMAL
     *         means the sleep elapsed normally, ABSOLUTE_PAST means the sleep
     *         time had already passed, and ABSOLUTE_INTERRUPTED means an
     *         interrupt occurred.
     */
    static int sleepAbsoluteRaw(long nanos) {
        return VMSupport.delayCurrentThreadAbsolute(nanos);
    }

    //
    // /**
    // * Starts the given thread at the given time.
    // * @param t the thread to start
    // * @param startTime the absolute start time in nanoseconds
    // */
    // void startThreadDelayed(RealtimeThread t, long startTime) {
    // Opaque vmThread = VMSupport.getVMThread(t);
    // VMSupport.startThreadDelayed(vmThread, startTime);
    // }

    // /**
    // * Atomically queries the given thread to see if its scheduling priority
    // * can be changed and, if allowed, sets the new priority.
    // * The RTSJ V1.0 says this can only
    // * happen if the thread is not alive, or if blocked in a sleep or
    // * wait. We've changed this to allow changes at any time - which means
    // * this method is redundant and we'll revise this soon.
    // * <p>Note that the thread is locked while this occurs. This should only
    // * be called if the thread is alive.
    // * @param t the thread to be queried
    // * @param newPrio the new priority of the thread
    // *
    // * @return <tt>true</tt> if the thread's scheduling parameters can be
    // * set and the new priority has been set, and <tt>false</tt> otherwise.
    // */
    // boolean canSetSchedulingParameters(RealtimeThread t, int newPrio) {
    // Opaque vmThread = VMSupport.getVMThread(t);
    // return VMSupport.setPriorityIfAllowed(vmThread, newPrio);
    // }
    //

    // /**
    // * Put the current thread to sleep until the specified time has passed.
    // *
    // * @param nanos the time to sleep until, expressed as nanoseconds since
    // * the epoch.
    // *
    // * @return <tt>true</tt> if the thread actually slept, and
    // * <tt>false</tt> if the specified time had already passed.
    // */
    // boolean sleepAbsoluteUninterruptible(long nanos) {
    // int rc = VMSupport.delayCurrentThreadAbsoluteUninterruptible(nanos);
    // switch (rc) {
    // case ABSOLUTE_NORMAL: return true;
    // case ABSOLUTE_PAST: return false;
    // default: throw new InternalError("invalid return code " + rc);
    // }
    // }
}
