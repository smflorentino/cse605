/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/Assert.java,v 1.2 2005/07/15 02:32:32 dholmes Exp $
 */
package javax.realtime;

/**
 * package private utility class to do assertion checking.
 *
 * @author David Holmes
 */
class Assert {
    
    /**
     * Checks if the specified condition is true and if not throws
     * an {@link AssertionError} with the given message.
     *
     * @param condition the condition to check
     * @param message the message to use if the condition is false.
     */
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /**
     * Checks if the specified condition is true and if not throws
     * an {@link AssertionError}
     *
     * @param condition the condition to check

     */
    static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }

    public static final String OK = "javax.realtime.Assert.OK";

    // usage: Assert.check( condition ? Assert.OK : "Error message");
    static void check(String msg) {
        if (msg != OK) throw new AssertionError(msg);
    }

    static final boolean ENABLED = true;

}
