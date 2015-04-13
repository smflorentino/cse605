
package javax.safetycritical;

import javax.realtime.AutonomousHappening;

import javax.realtime.AsyncEvent;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

  /**
   *
   */
@SCJAllowed(LEVEL_1)
public class ManagedAutonomousHappening extends AutonomousHappening {

  /**
   * Creates a Happening in the current memory area with a system
   * assigned name and id. 
   */
  @SCJAllowed(LEVEL_1)
  public ManagedAutonomousHappening() {}

  /**
   * Creates a Happening in the current memory area with the specified
   * id and a system-assigned name. 
   */
  @SCJAllowed(LEVEL_1)
  public ManagedAutonomousHappening(int id) {}

  /**
   * Creates a Happening in the current memory area with the name and
   * id given. 
   */
  @SCJAllowed(LEVEL_1)
  public ManagedAutonomousHappening(int id, String name) {}

  /**
   * Creates a Happening in the current memory area with the name name
   * and a system-assigned id. 
   */
  @SCJAllowed(LEVEL_1)
  public ManagedAutonomousHappening(String name) {}

  @Override
  @SCJAllowed(LEVEL_1)
  @SCJRestricted(INITIALIZATION)
  public final void attach(AsyncEvent ae) {}

  @Override
  @SCJAllowed(INFRASTRUCTURE)
  public final void detach(AsyncEvent ae) {}

}
