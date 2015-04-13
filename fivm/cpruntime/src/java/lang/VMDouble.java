package java.lang;

import com.fiji.fivm.r1.RuntimeImport;
import static com.fiji.fivm.r1.fivmRuntime.*;

import sun.misc.*;

final class VMDouble {
    private VMDouble() {}
    
   
    
    static long doubleToRawLongBits(double value) {
	return FCDouble.doubleToRawLongBits(value);
    }
    
    static double longBitsToDouble(long bits) {
	return FCDouble.longBitsToDouble(bits);
    }
    
    static String toString(double d, boolean b) {
        return new FloatingDecimal(d).toJavaFormatString();
    }
    
    static double parseDouble(String s) {
        return FloatingDecimal.readJavaFormatString(s).doubleValue();
    }
}

