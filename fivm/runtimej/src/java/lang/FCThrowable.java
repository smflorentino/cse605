/*
 * FCThrowable.java
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

import static com.fiji.fivm.Constants.*;
import static com.fiji.fivm.r1.Magic.*;
import static com.fiji.fivm.r1.fivmRuntime.*;

import java.util.LinkedList;

final class FCThrowable {
    static class CompactElement {
	Pointer mr;
	int lineNumber;
	CompactElement next;
	
	CompactElement(Pointer mr,
		       int lineNumber,
		       CompactElement next) {
	    this.mr=mr;
	    this.lineNumber=lineNumber;
	    this.next=next;
	}
    }
    
    /** A compact representation of a trace using a linked list.  Records
	the stack trace from the bottom (as in, the thread start-off method)
	to the top (fillInStackTrace) */
    CompactElement compactTrace;
    
    /** Lazily-computed full stack trace. */
    StackTraceElement[] fullTrace;
    
    private FCThrowable() {}
    
    @NoInline
    static FCThrowable fillInStackTrace(Throwable t) {
	final FCThrowable result=new FCThrowable();
	iterateDebugFrames(
	    curFrame(),
	    new DumpStackCback() {
		public Pointer cback(Pointer mr,
				     int lineNumber) {
		    result.compactTrace=
			new CompactElement(
			    mr,lineNumber,
			    result.compactTrace);
		    return Pointer.zero();
		}
	    });
	fence();
	return result;
    }
    
    synchronized StackTraceElement[] getStackTrace(Throwable t) {
	if (fullTrace==null) {
            // how to do it: don't worry about "performance"; this method only
            // gets called when we are actually printing the trace (not on every
            // throw)
            
            LinkedList< CompactElement > list=new LinkedList< CompactElement >();
            for (CompactElement cur=compactTrace;cur!=null;cur=cur.next) {
                list.addFirst(cur);
            }
            
            if ((CType.getInt(getVM(),"fivmr_VM","flags")
                 & CVar.getInt("FIVMR_VMF_VERBOSE_EXCEPTIONS"))==0) {
                // find the first @RuntimeExceptionThrower
                CompactElement ret=null;
                for (CompactElement cur : list) {
                    if ((fivmr_MethodRec_flags(cur.mr)&MBF_RT_EXC_THROWER)!=0) {
                        ret=cur;
                        break;
                    }
                }
            
                if (ret!=null) {
                    // if we found one, clip off everything before it
                    while (list.getFirst()!=ret) {
                        list.removeFirst();
                    }
                
                    // and remove the continuous set of runtime exception throwers
                    while ((fivmr_MethodRec_flags(list.getFirst().mr)&MBF_RT_EXC_THROWER)!=0) {
                        list.removeFirst();
                    }
                } else {
                    // remove fillInStackTrace calls
                    while (fromCString(fivmr_MethodRec_name(list.getFirst().mr))
                           .equals("fillInStackTrace")) {
                        list.removeFirst();
                    }
                
                    // remove <init> calls on subtypes of Throwable
                    while (fromCString(fivmr_MethodRec_name(list.getFirst().mr))
                           .equals("<init>") &&
                           fivmr_TypeData_isSubtypeOf(
                               curThreadState(),
                               fivmr_MethodRec_owner(list.getFirst().mr),
                               java.lang.fivmSupport.typeDataFromClass(Throwable.class))) {
                        list.removeFirst();
                    }
                }
            }
            
	    fullTrace=new StackTraceElement[list.size()];
            int idx=0;
            for (CompactElement cur : list) {
		Pointer mr=cur.mr;
		Pointer td=fivmr_MethodRec_owner(mr);
		int flags=fivmr_MethodRec_flags(mr);
		boolean isNative=
		    (flags&MBF_METHOD_IMPL)==MBF_JNI ||
		    (flags&MBF_METHOD_IMPL)==MBF_INTRINSIC || // huh?
		    (flags&MBF_METHOD_IMPL)==MBF_IMPORT;
		Pointer flnmCstr=fivmr_TypeData_filename(td);
		String flnm;
		if (flnmCstr==Pointer.zero()) {
		    flnm="<source filename unavailable>";
		} else {
		    flnm=fromCStringFull(flnmCstr);
		}
                if (false) {
                    System.out.println(
                        "td = "+td.asLong()+
                        ", cname = "+fromCStringFull(fivmr_TypeData_name(td))+
                        ", class pointer = "+Pointer.fromObject(fivmr_TypeData_asClass(td)).asLong()+
                        ", class = "+fivmr_TypeData_asClass(td));
                }
		fullTrace[idx++]=
		    new StackTraceElement(
			isNative? null : flnm,
			isNative? -2 : (cur.lineNumber==0? -1 : cur.lineNumber),
			fivmr_TypeData_asClass(td).getName(),
			fromCStringFull(fivmr_MethodRec_name(mr)),
			isNative);
	    }
	}
	return fullTrace;
    }
}

