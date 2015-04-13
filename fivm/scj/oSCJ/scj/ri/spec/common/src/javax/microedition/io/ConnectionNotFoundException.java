package javax.microedition.io;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * An exception to throw when the connection for a given URL cannot be
 * created because the resources are not available or no factory exists.
 */
@SCJAllowed
public class ConnectionNotFoundException extends Exception
{
  /**
   * ConnectionNotFoundException.java
   */
  private static final long serialVersionUID = -5238132696710282289L;

  /**
   * Create this exception with a text description.
   */
  @SCJAllowed
  public ConnectionNotFoundException(String message)
  {
    super(message);
  }

  /**
   * Create this exception with no description.
   */
  @SCJAllowed
  public ConnectionNotFoundException()
  {
    super();
  }
}
