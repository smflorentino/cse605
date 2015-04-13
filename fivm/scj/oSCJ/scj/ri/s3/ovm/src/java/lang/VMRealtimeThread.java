package java.lang;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;

class VMRealtimeThread extends VMThread {
    private static final Class[] NO_ARGS = new Class[0];
    private static final Method preRun, postRun/*, finalizeThread*/;
    private static final Field initArea;

    static {
        try {
            preRun = RealtimeThread.class.getDeclaredMethod("preRun", NO_ARGS);
            preRun.setAccessible(true);
            postRun = RealtimeThread.class
                    .getDeclaredMethod("postRun", NO_ARGS);
            postRun.setAccessible(true);
//            finalizeThread = RealtimeThread.class.getDeclaredMethod(
//                    "finalizeThread", NO_ARGS);
//            finalizeThread.setAccessible(true);
            initArea = RealtimeThread.class.getDeclaredField("initArea");
            initArea.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new LinkageError(e.toString());
        } catch (NoSuchFieldException e) {
            throw new LinkageError(e.toString());
        }
    }

    VMRealtimeThread(Thread thread) {
        super(thread);
    }

    private void call(Method m) {
        try {
            m.invoke(thread, NO_ARGS);
        } catch (IllegalAccessException e) {
            throw new Error(e.toString());
        } catch (InvocationTargetException wrapper) {
            Throwable e = wrapper.getTargetException();
            if (e instanceof Error)
                throw (Error) e;
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new RuntimeException(e);
        }
    }

    void preRun() {
        call(preRun);
    }

    void postRun() {
        call(postRun);
    }

    void finalizeThread() {
        // Note that this method is called after our scope stack has
        // been torn down. If we are the last thread in our initial
        // area, it no longer has a parent. Switch to the area in
        // which the thread is allocated to avoid store exceptions.
//        Opaque old = LibraryImports.setCurrentArea(LibraryImports.areaOf(this));
//        try {
//            call(finalizeThread);
//        } finally {
//            LibraryImports.setCurrentArea(old);
//        }
    }

    MemoryArea getInitialArea() {
        try {
            return (MemoryArea) initArea.get(thread);
        } catch (IllegalAccessException e) {
            Error err = new InternalError();
            err.initCause(e);
            throw err;
        }
    }

    void setPriority(int priority) {
        System.err.println("VMRealtimeThread.setPriority called!");
    }
}
