package javax.realtime;

/**
 * The exception thrown on an attempt to make an illegal assignment.
 * For example, this will be thrown on any attempt to assign a reference
 * to an object in scoped memory (an area of memory identified to be an 
 * instance of {@link ScopedMemory} to a field in an object in immortal memory.
 *
 * @spec RTSJ 1.0.1
 */
public class IllegalAssignmentError extends Error {

    private static final long serialVersionUID = -6803556508273380303L;

    /**
     * A constructor for <tt>IllegalAssignmentError</tt>.
     */
    public IllegalAssignmentError() {}

    /**
     * A descriptive constructor for <tt>IllegalAssignmentError</tt>.
     * @param description  Description of the error.
     */
    public IllegalAssignmentError(String description) {
        super(description);
    }
}
