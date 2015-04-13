package javax.realtime;

/**
 * This error is thrown on an attempt to refer to an object in
 * an inaccessible {@link MemoryArea}. For example this
 * will be thrown if logic in a {@link NoHeapRealtimeThread}
 * attempts to refer to an object in the traditional Java heap.
 *
 * @spec RTSJ 1.0.1
 */
public class MemoryAccessError extends Error {

    /**
     * A constructor for <tt>MemoryAccessError</tt>.
     */
    public MemoryAccessError() {}

    /**
     * A descriptive constructor for <tt>MemoryAccessError</tt>.
     *
     * @param description Description of the error.
     */
    public MemoryAccessError(String description) {
        super(description);
    }
}
