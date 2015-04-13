package javax.microedition.io;

import java.io.IOException;
import java.io.InputStream;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * A marker for connections that can input data.
 */
@SCJAllowed
public interface InputConnection extends Connection
{
  /**
   * The method for getting a stream from the connection to input data.
   */
  @SCJAllowed
  public InputStream openInputStream() throws IOException;
}
