package java.io;

import gnu.java.nio.FileChannelImpl;

/** Internal class.  Do not use directly. */
public class FCSupport {
    public static FileInputStream createFIS(FileChannelImpl impl) {
	return new FileInputStream(impl);
    }
    
    public static FileOutputStream createFOS(FileChannelImpl impl) {
	return new FileOutputStream(impl);
    }
}


