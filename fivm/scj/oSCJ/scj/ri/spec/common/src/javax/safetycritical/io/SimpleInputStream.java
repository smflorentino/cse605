package javax.safetycritical.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * A version of {@link OutputStream} that can format {@link CharSequence}s into
 * a UTF-8 byte sequence for writing.
 */
@SCJAllowed(members = true)
public class SimpleInputStream extends InputStream {
  
    /**
     * @param ins
     *            to use for output.
     */
    public SimpleInputStream(InputStream ins) {
    }

    /**
     * @return indicates whether or not an error occured.
     */
    public boolean checkError() {
        return false;
    }

    /**
   * 
   */
    protected void setError() {
    }

    /**
   * 
   */
    protected void clearError() {
    }

    @Override
    public int read() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

}
