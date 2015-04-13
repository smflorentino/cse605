package javax.realtime;

import java.io.Serializable;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;


/**
 * Kelvin added this on 2/8/11 because it is required by
 * InterruptServiceRoutine.  Did I get the right superclass?
 */
@SCJAllowed
public class DeregistrationException extends RuntimeException
{
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public DeregistrationException() {}

  /**
   * Kelvin wonders why this should be declared to allocate in
   * immortal.  This code was copied from MemoryScopeException.  Why
   * should that allocate in immortal?
   */
  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public DeregistrationException(String description) { super(description); }
}
