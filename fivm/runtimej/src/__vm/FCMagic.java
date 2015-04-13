/*
 * FCMagic.java
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

package __vm;

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;

@UsesMagic
public final class FCMagic {
    private FCMagic() {}
    
    @Intrinsic @NoSafepoint
    public static native Pointer toVMPointer(FCPtr p);
    
    @Intrinsic @NoSafepoint
    public static native FCPtr fromVMPointer(Pointer p);
    
    public static FCPtr zero() {
	return fromVMPointer(Pointer.zero());
    }
    
    public static FCPtr malloc(int size) {
        return fromVMPointer(fivmRuntime.fivmr_malloc(Pointer.fromIntZeroFill(size)));
    }
    
    @Intrinsic @NoSafepoint
    public static native FCPtr addressOfElement(Object o,int index);
    
    public static FCPtr fromIntSignExtend(int value) {
	return fromVMPointer(Pointer.fromIntSignExtend(value));
    }

    public static FCPtr fromInt(int value) {
	return fromVMPointer(Pointer.fromIntSignExtend(value));
    }
    
    public static FCPtr fromIntZeroExtend(int value) {
	return fromVMPointer(Pointer.fromIntZeroFill(value));
    }
    
    public static FCPtr plus(FCPtr a,FCPtr b) {
	return fromVMPointer(toVMPointer(a).add(toVMPointer(b)));
    }
    
    public static FCPtr minus(FCPtr a,FCPtr b) {
	return fromVMPointer(toVMPointer(a).add(toVMPointer(b)));
    }

    public static FCPtr neg(FCPtr a) {
	return fromVMPointer(toVMPointer(a).neg());
    }
    
    public static FCPtr times(FCPtr a,FCPtr b) {
	return fromVMPointer(toVMPointer(a).mul(toVMPointer(b)));
    }

    public static byte readByte(FCPtr a) {
	return toVMPointer(a).loadByte();
    }
    
    public static void writeByte(FCPtr a,byte value) {
	toVMPointer(a).store(value);
    }
    
    @Inline
    public static void copyToByteArray(byte[] array,int offset,int length,
				       FCPtr ptr_) {
        Pointer ptr=toVMPointer(ptr_);
        libc.copyToByteArray(array,offset,length,ptr);
    }
    
    @Inline
    public static void copyFromByteArray(byte[] array,int offset,int length,
					 FCPtr ptr_) {
        Pointer ptr=toVMPointer(ptr_);
        libc.copyFromByteArray(array,offset,length,ptr);
    }

    public static void copy(FCPtr to,FCPtr from,FCPtr size) {
	libc.memcpy(toVMPointer(to),
		    toVMPointer(from),
		    toVMPointer(size));
    }
    
    public static void move(FCPtr to,FCPtr from,FCPtr size) {
	libc.memmove(toVMPointer(to),
		     toVMPointer(from),
		     toVMPointer(size));
    }
    
    @NoReturn
    public static void uncheckedThrow(Throwable e) {
	fivmRuntime.uncheckedThrow(e);
    }
    
    public static boolean allContiguous() {
        return Settings.OM_CONTIGUOUS;
    }
    
    public static boolean isContiguous(Object o) {
        return MM.contiguousArray(o);
    }

    public static void printStr(String str) {
        fivmRuntime.logPrint(str);
    }
    
    public static void printPtr(Object obj) {
        fivmRuntime.fivmr_Log_lock();
        fivmRuntime.fivmr_Log_printHex(Pointer.fromObject(obj).asLong());
        fivmRuntime.fivmr_Log_unlock();
    }

    public static void printPtr(FCPtr ptr) {
        fivmRuntime.fivmr_Log_lock();
        fivmRuntime.fivmr_Log_printHex(toVMPointer(ptr).asLong());
        fivmRuntime.fivmr_Log_unlock();
    }
    
    public static void printNum(long num) {
        fivmRuntime.fivmr_Log_lock();
        fivmRuntime.fivmr_Log_printNum(num);
        fivmRuntime.fivmr_Log_unlock();
    }
    
    public static void println() {
        fivmRuntime.logPrint("\n");
    }
}


