

package javax.realtime;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;

import java.util.ArrayList;

/**
 * The base level class used to support first level handling.
 * Application-defined subclasses override the <code>handle</code> method to
 * define the first-level service routine
 *
 */
@SCJAllowed(LEVEL_1)
public abstract class InterruptServiceRoutine
{
  /** Structure to hold InterruptServiceRoutines */
  private static final ArrayList<InterruptServiceRoutine> _routines_ =
      new ArrayList<InterruptServiceRoutine>();

  /** A unique name for this signal. */
  private String name_;

  /** A unique id for this signal, when registered. */
  private int id_;

  /**
   * Creates an interrupt signal with the given name and associated
   * with a given interrupt.  
   *
   * @param name is a system dependent designator for the interrupt
   */
  @SCJAllowed(LEVEL_1)
  public InterruptServiceRoutine(String name)
  {
    name_ = name;
    id_ = -1;
  }

  /**
   * The code to execute for first level interrupt handling.
   * A subclass defines this to give the proper behavior.  No code
   * that could block or call dynamic methods may be called here.
   */
  @SCJAllowed(SUPPORT)  // LEVEL_1
  @SCJRestricted(maySelfSuspend = false)
  protected abstract void handle();

  /**
   * Get the name of this signal.
   *
   * @return the name of this signal.
   */
  @SCJAllowed(LEVEL_1)
  public final String getName()
  {
    return name_;
  }

  /**
   * Get a numeric ID for dispatching to this service routine.
   *
   * @return the id of this signal.
   */
  @SCJAllowed(LEVEL_1)
  public final int getId()
  {
    return id_;
  }

  /**
   * Register this ISR with the system so that it can be triggered.  The
   * <code>name</code> parameter of the constructor uses a system dependent naming
   * scheme to link this signal to a system interrupt.
   *
   * @throws RegistrationException when this signal is already
   *         registered
   */
  @SCJAllowed(LEVEL_1)
  public void register()
    throws RegistrationException
  {
    synchronized (_routines_)
    {
      if (contains()) throw new RegistrationException();
      else id_ = add();
    }
  }

  /**
   * Unregister this ISR with the system so that it can no longer be
   * triggered.
   *
   * @throws DeregistrationException when this signal is not
   *         registered
   */
  @SCJAllowed(LEVEL_1)
  public void unregister()
    throws DeregistrationException
  {
    synchronized (_routines_)
    {
      if (isRegistered()) remove();
      else throw new DeregistrationException();
    }
  }

  /**
   * Check to see is this ISR is registered.
   *
   * @return true when registered, otherwise false.
   */
  @SCJAllowed(LEVEL_1)
  public final boolean isRegistered()
  {
    return (id_ > -1) && (_routines_.get(id_) != null);
  }

  private boolean contains()
  {
    for (InterruptServiceRoutine routine: _routines_)
    {
      if (routine.getName().equals(this)) return true;
    }
    return false;
  }

  private int add()
  {
    if (id_ == -1)
    {
      _routines_.add(this);
      return _routines_.size();
    }
    else
    {
      _routines_.set(id_, this);
      return id_;
    }
  }

  private void remove()
  {
    _routines_.set(id_, null);
  }
}
