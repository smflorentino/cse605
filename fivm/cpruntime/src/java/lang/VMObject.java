package java.lang;

import com.fiji.fivm.r1.*;

import static com.fiji.fivm.r1.Magic.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

final class VMObject {
    private VMObject() {}
    
    

    static Class<?> getClass(Object o) {
	return FCObject.getClass(o);
    }
    
    static Object clone(Cloneable c) {
	return FCObject.clone(c);
    }
    
    static void notify(Object o) throws IllegalMonitorStateException {
	FCObject.notify(o);
    }
    
    static void notifyAll(Object o) throws IllegalMonitorStateException {
	FCObject.notifyAll(o);
    }
    
    static void wait(Object o, long ms, int ns)
	throws IllegalMonitorStateException, InterruptedException {
	FCObject.wait(o, ms, ns);
    }
}

