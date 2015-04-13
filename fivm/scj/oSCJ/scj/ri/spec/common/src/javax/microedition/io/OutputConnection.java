package javax.microedition.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * A marker for connections that can output data.
 */
@SCJAllowed
public interface OutputConnection extends Connection
{
  /**
   * The method for getting a stream from a connection to output
   * data.
   */
  @SCJAllowed
  public OutputStream openOutputStream() throws IOException;
}
