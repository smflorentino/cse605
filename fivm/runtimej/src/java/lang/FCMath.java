/*
 * FCMath.java
 * Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
 * This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
 * LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
 * available at fivm/LEGAL and can also be found at
 * http://www.fiji-systems.com/FPL3.txt
 * 
 * By installing, reproducing, distributing, and/or using the FIJI VM Software
 * you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
 * rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
 * restrictions stated therein.  Among other conditions and restrictions, the
 * FIJI PUBLIC LICENSE states that:
 * 
 * a. You may only make non-commercial use of the FIJI VM Software.
 * 
 * b. Any adaptation you make must be licensed under the same terms 
 * of the FIJI PUBLIC LICENSE.
 * 
 * c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
 * file, adaptation or output code that you distribute and cause the output code
 * to provide a notice of the FIJI PUBLIC LICENSE. 
 * 
 * d. You must not impose any additional conditions.
 * 
 * e. You must not assert or imply any connection, sponsorship or endorsement by
 * the author of the FIJI VM Software
 * 
 * f. You must take no derogatory action in relation to the FIJI VM Software
 * which would be prejudicial to the FIJI VM Software author's honor or
 * reputation.
 * 
 * 
 * The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
 * representation and provides no warranty of any kind concerning the software.
 * 
 * The FIJI PUBLIC LICENSE and any rights granted therein terminate
 * automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
 */

package java.lang;

import com.fiji.fivm.r1.*;

final class FCMath {
    private FCMath() {}
    
    @RuntimeImport
    @TrustedGodGiven
    static native double sin(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double cos(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double tan(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double asin(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double acos(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double atan(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double atan2(double a,double b);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double exp(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double log(double value);
    
    @Inline
    static double sqrt(double value) {
        return Magic.sqrt(value);
    }
    
    @RuntimeImport
    @TrustedGodGiven
    static native double pow(double a,double b);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double fmod(double a,double b);
    
    static double IEEEremainder(double a,double b) {
	return fmod(a,b);
    }
    
    @RuntimeImport
    @TrustedGodGiven
    static native double ceil(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double floor(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double rint(double value);

    @RuntimeImport
    @TrustedGodGiven
    static native double cbrt(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double cosh(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double sinh(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double tanh(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double expm1(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double hypot(double a,double b);

    @RuntimeImport
    @TrustedGodGiven
    static native double log10(double value);
    
    @RuntimeImport
    @TrustedGodGiven
    static native double log1p(double value);
}

