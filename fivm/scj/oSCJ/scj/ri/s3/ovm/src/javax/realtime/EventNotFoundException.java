package javax.realtime;

/**
 * {@link PhysicalMemoryManager} throws this exception if an attempt is made
 *  to unregister an insertion or removal handler that was not registered.
 * 
 * @since 1.0.1
 *
 * @spec RTSJ 1.0.1
 */
public class EventNotFoundException extends RuntimeException {
    
    /**
     * A constructor for <code>EventNotFoundException</code>.
     */
    public EventNotFoundException(){ }

    /**
     * A descriptive constructor for <code>EventNotFoundException</code>.
     *
     *  @param  description Description of the exception.
     */
    public EventNotFoundException(String description){
        super(description);
    }
}
