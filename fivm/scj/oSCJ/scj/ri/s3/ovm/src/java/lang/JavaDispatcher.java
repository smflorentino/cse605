// REALTIME VERSION

package java.lang;
import org.ovmj.java.Opaque;
/**
 * The <tt>JavaDispatcher</tt> provides an API for use by the 
 * {@link VMThreadBase} class and {@link JavaVirtualMachine} to simplify access 
 * to the thread related kernel services of the OVM.
 * <p>This is essentially a marshalling interface that takes care of
 * connecting the <tt>VMThreadBase</tt> methods with the OVM threading
 * system via the {@link LibraryImports} mechanism.
 * However, it also helps provide some of the thread and JVM lifecycle
 * management.
 *
 *
 * <h3>Realtime Characteristics</h3>
 * <p>This version of the dispatcher is RTSJ aware and adapts
 * {@link #bindVMThread} to pass through heap/no-heap status.
 *
 * <h3>To Do</h3>
 * <p>Implement {@link #stopThread}.
 * <p>Maybe Implement {@link #destroyThread}.
 * <p>Maybe Implement {@link #suspendThread} and {@link #resumeThread}.
 *
 * <h3>Notes</h3>
 * <p>There was an earlier idea (and skeletal implementation) to provide
 * thread suspension by using an ultra low priority that meant the thread
 * was visible to the system ( for interrupt, granting of locks etc) but
 * could never get the CPU as a &quot;nop&quot; thread of higher priority
 * would always be ready to run (we're assuming uniprocessor). This scheme
 * has not been implemented due to the problems that would arise with
 * priority inheritance schemes. Consequently, we just let the deprecated
 * methods remain unimplemented.
 *
 * @author David Holmes
 */
class JavaDispatcher {

    /** The singleton instance of this class */
    static final JavaDispatcher instance = new JavaDispatcher();

    /**
     * Return the singleton instance of this class 
     * @return the singleton instance of this class 
     */
    static JavaDispatcher getInstance() {
        return instance;
    }

    /** no construction allowed */
    private JavaDispatcher() {}


    /** 
     * Obtain a reference to the currently executing Java thread.
     * @return a reference to the {@link java.lang.VMThreadBase} object corresponding
     * to the currently executing thread.
     */
    VMThreadBase getCurrentThread() {
        return LibraryImports.getCurrentJavaThread();
    }

    /**
     * Implements the semantics of {@link java.lang.Thread#yield}. 
     */
    void yieldCurrentThread() {
        LibraryImports.yieldCurrentThread();
    }

    /**
     * Implements the semantics of {@link java.lang.Thread#holdsLock}.      
     * @param obj the object to check
     * @return <tt>true</tt> if the current thread holds the monitor lock
     * of <tt>obj</t>, and <tt>false</tt> otherwise.
     */
    boolean holdsLock(Object obj) {
        if (obj == null) {
            return false;
        }
        // we use this rather than comparing the monitor owner with the
        // vmThread of the current thread because during threads finalization
        // the vmThread field has been nulled already.
        return LibraryImports.currentThreadOwnsMonitor(obj);
    }


    /**
     * Puts the current thread to sleep for at least the specified number
     * of nanoseconds, or until the thread is interrupted.
     *
     * @param millis the number of milliseconds to sleep for
     * @param nanos the additional number of nanoseconds to sleep for
     *
     * @return <tt>true</tt> if the sleep completed normally; and 
     * <tt>false</tt> if the sleep was interrupted.
     */
    boolean sleep(long millis, int nanos) {
        return LibraryImports.delayCurrentThread(millis, nanos);
    }

    /**
     * Binds a VM thread to the given {@link Thread} and registers it
     * with the virtual machine.
     * <p>This is phase 1 of a 2 phase thread start-up protocol. We need to
     * use two phases because the actual start process in the VM may need to
     * be overridden by subclasses - such as javax.realtime.RealtimeThread.
     *
     * @param t the Java thread being bound
     */
    void bindVMThread(VMThreadBase t) {
        // assert: t.vmThread == null

        boolean noHeap =
	    t.thread instanceof javax.realtime.NoHeapRealtimeThread;

        // create a new OVMThread bound to t
        Opaque newVMThread = LibraryImports.createVMThread(t, noHeap);
        // bind t to that new thread
        t.setVMThread(newVMThread);
        // register with the JVM instance
        JavaVirtualMachine.getInstance().addThread(t);
    }

    /**
     * Starts the execution of the given thread. The thread must already
     * be {@link #bindVMThread bound}.
     * <p>This is phase 2 of a 2 phase thread start-up protocol. We need to
     * use two phases because the actual start process in the VM may need to
     * be overridden by subclasses - such as javax.realtime.RealtimeThread.

     * @param t the Java thread being started
     */
    void startThread(VMThreadBase t) {     
        // assert: t.vmThread != null   
        LibraryImports.startThread(t.vmThread);
    }


    /** 
     * Terminates the current thread. This method never returns and must never
     * throw an exception.
     * <p>This method should be called as part of thread termination due to the
     * completion (normally or abnormally) of the {@link Thread#run}
     * method.
    */
    void terminateCurrentThread() {
        VMThreadBase current = getCurrentThread();
        try {
            current.vmThread = null; // clear for GC purposes
        }
        catch (Throwable t) {
            VMThreadBase.rawPrint(ASSIGNMENT_ERROR);
        }
        try {
            JavaVirtualMachine.getInstance().removeThread(current);
        }
        catch (Throwable t) {
            VMThreadBase.rawPrint(REMOVE_THREAD_ERROR);
        }
        // NOTE: if this was the last user thread we never reach this point!
        LibraryImports.terminateCurrentThread();
    }

    static final String ASSIGNMENT_ERROR = "current.vmThread = null excepted\n";
    static final String REMOVE_THREAD_ERROR = "JVM.removeThread excepted\n";

    /**
     * Queries if the given thread is still alive. 
     *
     * @param t the thread being queried
     * @return <tt>true</tt> if the corresponding VM thread was still
     * alive at the time of the call, and <tt>false</tt> otherwise.
     */
    boolean isAlive(VMThreadBase t) {
        if (t.vmThread == null) {
            return false;
        }
        else {
            return LibraryImports.isAlive(t.vmThread);
        }
    }

    /**
     * Interrupt the given thread.
     * <p>If the thread is in an interruptable blocking state then it will
     * be woken from that state. Otherwise nothing happens.
     *
     * @param t the thread to be interrupted
     */
    void interruptThread(VMThreadBase t) {
        LibraryImports.interruptThread(t.vmThread);
    }

    /**
     * Destroys the given thread, terminating it abruptly.
     * <p><b>This method is not implemented</b>.
     * @param t the thread to destroy
     */
    void destroyThread(VMThreadBase thread)  {
        // note that the thread is locked at the Java level
        // assert: currentThread.holdsLock(thread)
        // assert: thread.isAlive()
        throw new NoSuchMethodError("destroy not implemented");
    }

    /**
     * Suspends the given thread.
     * <p><b>This method is not implemented</b>.
     * @param t the thread to suspend
     */
    void suspendThread(VMThreadBase t) {
        // note that the thread is locked at the Java level unless 
        // it is the current thread
        // assert: currentThread.holdsLock(thread)|| thread == currentThread
        // assert: thread.isAlive()
        throw new NoSuchMethodError("suspend not implemented");
    }

    /**
     * Resumes the given thread.
     * <p><b>This method is not implemented</b>.
     * @param t the thread to resume
     */
    void resumeThread(VMThreadBase t) {
        // note that the thread is locked at the Java level
        // assert: currentThread.holdsLock(thread)
        // assert: thread.isAlive()
        throw new NoSuchMethodError("resume not implemented");
    }

    /**
     * Sets the priority of the VM thread corresponding to the given
     * thread, based on the given value.
     * <p>In the general case we would query the underlying VM dispatcher
     * to see what priority range we have available to use. In this case
     * we rely on our knowledge that there is a direct mapping between the
     * Java priority value and the VM priority value.
     * 
     * @param t the thread to have its priority set
     * @param prio the new priority value. This is a Java level priority
     * value that will be mapped, as appropriate, to an underlying
     * system priority.
     * Note that this sets the base priority of the thread not necessarily
     * the active priority.
     *
     * @return <tt>true</tt> if the priority was set, and <tt>false</tt> if
     * the thread has already terminated.
     *
     */
    boolean setThreadPriority(VMThreadBase t, int prio) {
        // NOTE: We do not hold the lock on the thread 
        // so it could terminate
        return setThreadPriorityStatic(t, prio);
    }


    static boolean setThreadPriorityStatic(VMThreadBase t, int prio) {
        // NOTE: We do not hold the lock on the thread 
        // so it could terminate
        return LibraryImports.setPriority(t.vmThread, prio);
    }

    /**
     * Queries the priority of the given thread.
     * <p>In the general case we would query the underlying VM dispatcher
     * to see what priority range we have available to use. In this case
     * we rely on our knowledge that there is a direct mapping between the
     * Java priority value and the VM priority value.
     *
     * @param t the thread to be queried
     * @return the Java priority of the given thread. 
     * Note that this queries the base priority of the thread not the
     * active priority.
     */
    int getThreadPriority(VMThreadBase t) {
        // note that the thread is locked at the Java level
        // assert: currentThread.holdsLock(thread)
        // assert: thread.isAlive()
        return LibraryImports.getPriority(t.vmThread);
    }

    /**
     * Causes the specified exception to be thrown asynchronously in the
     * specified thread.
     * <p><b>This method is not implemented</b>.
     *
     * @param t the thread to be stopped
     * @param exc the exception object to be thrown
     */
    void stopThread(java.lang.VMThreadBase thread, Throwable exc)  {
        // note that the thread is locked at the Java level
        // assert: currentThread.holdsLock(thread)
        // assert: thread.isAlive()
        throw new NoSuchMethodError("stop() not implemented yet");
    }

}











