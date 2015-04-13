

package javax.microedition.io;

import java.io.*;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class Connector {

  @SCJAllowed
  public final static int READ  = 1;

  @SCJAllowed
  public final static int WRITE = 2;
  
  @SCJAllowed
  public final static int READ_WRITE = (READ|WRITE);


  /**
   * Prevent instantiation of this class.
   */
  private Connector(){}

  @SCJAllowed
  public static Connection open(String name, int mode) throws IOException {
    return null;
  }
  
  @SCJAllowed
  public static Connection open(String name) throws IOException {
    return open(name, READ_WRITE);
  }
  
  @SCJAllowed
  public static InputStream openInputStream(String name) throws IOException {
    return null;
  }
  
  @SCJAllowed
  public static OutputStream openOutputStream(String name) throws IOException {
    return null;
  }
  
// TODO: we don't use DataStream, right?
//    public static DataInputStream openDataInputStream(String name)
//    	throws IOException { 
//    	return null;
//    }
//
//    public static DataOutputStream openDataOutputStream(String name)
//    	throws IOException { 
//    	return null;
//    }
}

