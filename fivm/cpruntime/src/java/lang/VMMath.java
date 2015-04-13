package java.lang;

import com.fiji.fivm.r1.*;

final class VMMath {
    private VMMath() {}
    
    static double sin(double value){
	return FCMath.sin(value);
    }
    
   
    static double cos(double value){
	return FCMath.cos(value);
    }
    
    
    static double tan(double value){	
	return FCMath.tan(value);
    }
    
    static double asin(double value){
	return FCMath.asin(value);
    }
    
    
    static double acos(double value){
	return FCMath.acos(value);
    }
    
    
    static double atan(double value){
	return FCMath.atan(value);
    }
    
    
    static double atan2(double a,double b){
	return FCMath.atan2(a, b);
    }
    
    
    static double exp(double value){
	return FCMath.exp(value);
    }
    
    
    static double log(double value){
	return FCMath.log(value);
    }
    
    
    static double sqrt(double value){
	return FCMath.sqrt(value);
    }
    
    
    static double pow(double a,double b){
	return FCMath.pow(a, b);
    }
    
    
    static double fmod(double a,double b){
	return FCMath.fmod(a,b);
    }
    
    static double IEEEremainder(double a,double b) {
	return FCMath.IEEEremainder(a, b);
    }
    
    
    static double ceil(double value){
	return FCMath.ceil(value);
    }
    
    
    static double floor(double value){
	return FCMath.floor(value);
    }
    
    
    static double rint(double value){
	return FCMath.rint(value);
    }

    
    static double cbrt(double value){
	return FCMath.cbrt(value);
    }
    
    
    static double cosh(double value){
	return FCMath.cosh(value);
    }
    
    
    static double sinh(double value){
	return FCMath.sinh(value);
    }
    
    
    static double tanh(double value){
	return FCMath.tanh(value);
    }
    
    
    static double expm1(double value){
	return FCMath.expm1(value);
    }
    
    
    static double hypot(double a,double b){
	return FCMath.hypot(a,b);
    }

    
    static double log10(double value){
	return FCMath.log10(value);
    }
    
   
    static double log1p(double value){
	return FCMath.log1p(value);
    }
}

