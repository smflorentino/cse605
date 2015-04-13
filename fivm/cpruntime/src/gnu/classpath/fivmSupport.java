package gnu.classpath;

public final class fivmSupport {
    private fivmSupport() {}
    
    public static int getPtrData(gnu.classpath.Pointer32 p) {
	return p.data;
    }
    
    public static long getPtrData(gnu.classpath.Pointer64 p) {
	return p.data;
    }
}

