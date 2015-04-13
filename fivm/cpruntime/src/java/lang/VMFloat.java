package java.lang;

import com.fiji.fivm.r1.RuntimeImport;

import sun.misc.*;

final class VMFloat {
    private VMFloat() {}
    
    
    static int floatToRawIntBits(float value) {
	return FCFloat.floatToRawIntBits(value);
    }
    
    static float intBitsToFloat(int bits) {
	return FCFloat.intBitsToFloat(bits);
    }
    
    static String toString(float f) {
        return new FloatingDecimal(f).toJavaFormatString();
    }
    
    static float parseFloat(String s) {
        return FloatingDecimal.readJavaFormatString(s).floatValue();
    }
}

