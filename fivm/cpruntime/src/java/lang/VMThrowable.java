package java.lang;

import com.fiji.fivm.r1.*;

import static com.fiji.fivm.Constants.*;
import static com.fiji.fivm.r1.Magic.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

final class VMThrowable {
    FCThrowable fct;
    
    private VMThrowable() {
    }
    
    static VMThrowable fillInStackTrace(Throwable t) {
	final VMThrowable result=new VMThrowable();
	result.fct = FCThrowable.fillInStackTrace(t);
	return result;
    }
    
    StackTraceElement[] getStackTrace(Throwable t) {
	return fct.getStackTrace(t);
    }
}

