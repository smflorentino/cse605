package javax.microedition.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * A Marker for Connections that can both read and write data.
 */
@SCJAllowed
public interface StreamConnection extends InputConnection, OutputConnection
{
}
