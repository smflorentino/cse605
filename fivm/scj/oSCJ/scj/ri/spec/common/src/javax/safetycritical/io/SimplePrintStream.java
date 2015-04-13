package javax.safetycritical.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.CALLER;

/**
 * A version of {@link OutputStream} that can format {@link CharSequence}s
 * into a UTF-8 byte sequence for writing.
 */
@SCJAllowed(members = true)
public class SimplePrintStream extends OutputStream
{
  /**
   * @param stream to use for output.
   */
  public SimplePrintStream(OutputStream stream)
  {
  }

  /**
   * @return indicates whether or not an error occured.
   */
  public boolean checkError() { return false; }

  /**
   * 
   */
  protected void setError() { }

  /**
   * 
   */
  protected void clearError() {}

  /**
   * The class uses the same modified UTF-8 used by java.io.DataOuputStream.
   * There are two differences between this format and the "standard" UTF-8
   * format:
   * <ol>
   * <li>the null byte '\\u0000' is encoded in two bytes rather than in one, so
   * the encoded string never has any embedded nulls; and </li>
   * <li>only the one, two, and three byte encodings are used. </li>
   * </ol>
   *
   * @throws IOException.
   */
  public synchronized void print(CharSequence sequence) {}

  
  public void println() {}

  @RunsIn(CALLER)
  public void println(CharSequence sequence) {}

  public void write(int b)
    throws IOException
  {
  }
  
  public void write(String str)
{
}
}
