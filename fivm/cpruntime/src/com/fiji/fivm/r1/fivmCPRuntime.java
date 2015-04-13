package com.fiji.fivm.r1;

import static com.fiji.fivm.r1.fivmRuntime.*;

public class fivmCPRuntime {
    private fivmCPRuntime() {}

    @AllocateAsCaller
    public static gnu.classpath.Pointer toCLPtr(Pointer p) {
	if (Pointer.size()==4) {
	    return new gnu.classpath.Pointer32(p.castToInt());
	} else {
	    return new gnu.classpath.Pointer64(p.asLong());
	}
    }
    
    @AllocateAsCaller
    public static Pointer fromCLPtr(gnu.classpath.Pointer p) {
	if (Pointer.size()==4) {
	    return Pointer.fromInt(gnu.classpath.fivmSupport.getPtrData((gnu.classpath.Pointer32)p));
	} else {
	    return Pointer.fromLong(gnu.classpath.fivmSupport.getPtrData((gnu.classpath.Pointer32)p));
	}
    }
}
