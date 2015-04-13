/*
 * InterceptIntrinsicAfterChecks.java
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

package com.fiji.fivm.c1;

import java.nio.ByteOrder;
import java.util.*;

public class InterceptIntrinsicAfterChecks extends InvocationInterceptor {
    public InterceptIntrinsicAfterChecks(Code code) { super(code,interceptors); }
    
    static HashMap< VisibleMethod, Interceptor > interceptors;
    
    static {
	interceptors=new HashMap< VisibleMethod, Interceptor >();
	
	// two major gotchas:
	// 1) we need SSA
	// 2) because we're running after SquirtChecks, we need to insert
	//    the checks ourselves.  that's safe, since for these
	//    intrinsics, there is no risk of SquirtChecks inserting
	//    checks that we don't need.
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()Z",
		"isBigEndian"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{
				IntConst.make(Global.endianness==ByteOrder.BIG_ENDIAN)
			    }));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()I",
		"getGC"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),
			    new Arg[]{
				IntConst.make(Global.gc.asInt())
			    }));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()Z",
		"needCMStoreBarrier"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),
			    new Arg[]{
				IntConst.make(Global.gc.needsCMStoreBarrier())
			    }));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()Z",
		"blackStack"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),
			    new Arg[]{
				IntConst.make(Global.blackStack)
			    }));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()I",
		"getObjectModel"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),
			    new Arg[]{
				IntConst.make(Global.om.asInt())
			    }));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()I",
		"getHeaderModel"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),
			    new Arg[]{
				IntConst.make(Global.hm.asInt())
			    }));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"()P",
		"curThreadState"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{Arg.THREAD_STATE}));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"()P",
		"curFrame"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{Arg.FRAME}));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"()I",
		"curAllocSpace"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.GetAllocSpace,
			    before.lhs(),Arg.EMPTY));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"()P",
		"curAllocFrame"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{
				Arg.ALLOC_FRAME
			    }));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()I",
		"getOSFlavor"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{
				IntConst.make(Global.osFlavor.asInt())}));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()Z",
		"staticJNI"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{
				IntConst.make(Global.staticJNI)}));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()Z",
		"dynLoading"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{
				IntConst.make(Global.dynLoading)}));
		}
	    });
	
	interceptors.put(
	    Runtime.fivmOptions.getMethod(
		"()Z",
		"dumbHashCode"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{
				IntConst.make(Global.dumbHashCode)}));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Object;)V",
		"nullCheck"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.NullCheck,
			    Var.VOID,before.rhs()));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"()V",
		"fence"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Fence,
			    Var.VOID,Arg.EMPTY));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"()V",
		"compilerFence"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.HardCompilerFence,
			    Var.VOID,Arg.EMPTY));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(II)Z",
		"uLessThan"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.ULessThan,
			    before.lhs(),before.rhs()));
		}
	    });
	
	// FIXME: add stuff, esp from the standard library that would benefit
	// from intrinsics.
    }
}


