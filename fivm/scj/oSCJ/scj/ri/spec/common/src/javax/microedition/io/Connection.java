package javax.microedition.io;

import java.io.IOException;

import javax.safetycritical.annotate.SCJAllowed;

/** A generic connection that just provides the ability to be closed. */
@SCJAllowed
public abstract interface Connection
{
  /**
   * Clean up all resources for this connection and make it unusable.
   */
  @SCJAllowed
  public void close() throws IOException;
}
