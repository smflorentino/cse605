package java.net;

/* internal class.  do not use. */
public final class FCSupport {

    private FCSupport() {}
    
    public static SocketImpl getImpl(ServerSocket s) {
        return s.getImpl();
    }
    
}
