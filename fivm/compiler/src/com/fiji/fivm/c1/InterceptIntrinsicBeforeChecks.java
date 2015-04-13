/*
 * InterceptIntrinsicBeforeChecks.java
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

import java.util.*;
import com.fiji.fivm.Constants;

public class InterceptIntrinsicBeforeChecks extends InvocationInterceptor {
    public InterceptIntrinsicBeforeChecks(Code c) { super(c,interceptors); }
    
    static HashMap< VisibleMethod, Interceptor > interceptors;
    
    static {
	interceptors=new HashMap< VisibleMethod, Interceptor >();
	
	// Pointer intrinsics
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()P","zero"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{
				PointerConst.ZERO
			    }));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()I","size"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mov,
			    before.lhs(),new Arg[]{
				new IntConst(Global.pointerSize)
			    }));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(Ljava/lang/Object;)P","fromObject"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new TypeInst(
			    before.di(),OpCode.Cast,
			    before.lhs(),before.rhs(),
			    Type.POINTER));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(I)P","fromIntZeroFill"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.IntToPointerZeroFill,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(I)P","fromIntSignExtend"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new TypeInst(
			    before.di(),OpCode.Cast,
			    before.lhs(),before.rhs(),
			    Type.POINTER));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(J)P","fromLong"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new TypeInst(
			    before.di(),OpCode.Cast,
			    before.lhs(),before.rhs(),
			    Type.POINTER));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()Z","loadBoolean"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.BOOLEAN));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()B","loadByte"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.BYTE));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()S","loadShort"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.SHORT));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()C","loadChar"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.CHAR));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()I","loadInt"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.INT));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()J","loadLong"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.LONG));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()F","loadFloat"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.FLOAT));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()D","loadDouble"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.DOUBLE));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()P","loadPointer"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Load,
			    before.lhs(),before.rhs(),
			    Type.POINTER));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(Z)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.BOOLEAN));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(B)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.BYTE));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(S)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.SHORT));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(C)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.CHAR));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(I)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.INT));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(J)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.LONG));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(F)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.FLOAT));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(D)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.DOUBLE));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)V","store"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new MemoryAccessInst(
			    before.di(),OpCode.Store,
			    Var.VOID,before.rhs(),
			    Type.POINTER));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()P","neg"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Neg,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)P","add"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Add,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)P","sub"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Sub,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)P","mul"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mul,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)P","div"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Div,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)P","mod"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Mod,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)P","and"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.And,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)P","or"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Or,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)P","xor"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Xor,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()P","not"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.BitNot,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(I)P","shl"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Shl,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(I)P","shr"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Shr,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(I)P","ushr"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.Ushr,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)Z","lessThan"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.ULessThan,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("(P)Z","signedLessThan"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.LessThan,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()Ljava/lang/Object;","asObject"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new TypeInst(
			    before.di(),OpCode.Cast,
			    before.lhs(),before.rhs(),
			    Global.root().objectType));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()Ljava/lang/String;","asString"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new TypeInst(
			    before.di(),OpCode.Cast,
			    before.lhs(),before.rhs(),
			    Global.root().stringType));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()I","castToInt"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new TypeInst(
			    before.di(),OpCode.Cast,
			    before.lhs(),before.rhs(),
			    Type.INT));
		}
	    });
	
	interceptors.put(
	    Global.root().pointerClass.getMethod("()J","asLong"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new TypeInst(
			    before.di(),OpCode.Cast,
			    before.lhs(),before.rhs(),
			    Type.LONG));
		}
	    });
        
        interceptors.put(
            Global.root().pointerClass.getMethod("(II)Z","weakCAS"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new MemoryAccessInst(
                            before.di(),OpCode.WeakCAS,
                            before.lhs(),before.rhs(),
                            Type.INT,
                            Mutability.MUTABLE,
                            Volatility.VOLATILE));
                }
            });

        interceptors.put(
            Global.root().pointerClass.getMethod("(PP)Z","weakCAS"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new MemoryAccessInst(
                            before.di(),OpCode.WeakCAS,
                            before.lhs(),before.rhs(),
                            Type.POINTER,
                            Mutability.MUTABLE,
                            Volatility.VOLATILE));
                }
            });

	// Magic intrinsics

        interceptors.put(
            Runtime.magic.getMethod(
                "()Ljava/lang/Error;",
                "notReached"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    h.notReachedAfter(before); // because we remove before
                }
            });
        
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Object;Ljava/lang/String;)P",
		"addressOfField"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
                    if (before.rhs(0).type()==Exectype.NULL) {
                        before.prepend(
                            new SimpleInst(
                                before.di(),OpCode.NullCheck,
                                Var.VOID,new Arg[]{Arg.NULL}));
                        before.prepend(
                            new SimpleInst(
                                before.di(),OpCode.Mov,
                                before.lhs(),new Arg[]{PointerConst.make(0)}));
                    } else {
                        before.prepend(
                            new HeapAccessInst(
                                before.di(),OpCode.AddressOfField,
                                before.lhs(),new Arg[]{before.rhs(0)},
                                Util.extractInstField(h,before.rhs(0),before.rhs(1))));
                    }
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Class;Ljava/lang/String;)P",
		"addressOfStaticField"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new HeapAccessInst(
			    before.di(),OpCode.AddressOfStatic,
			    before.lhs(),Arg.EMPTY,
			    Util.extractStaticField(h,before.rhs(0),before.rhs(1))));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Class;Ljava/lang/String;)P",
		"offsetOfField"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new FieldInst(
			    before.di(),OpCode.OffsetOfField,
			    before.lhs(),Arg.EMPTY,
			    Util.extractInstFieldStatic(h,before.rhs(0),before.rhs(1))));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Object;I)P",
		"addressOfElement"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new HeapAccessInst(
			    before.di(),OpCode.AddressOfElement,
			    before.lhs(),before.rhs(),
                            ArrayElementField.INSTANCE));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Class;I)P",
		"offsetOfElement"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new TypeInst(
			    before.di(),OpCode.OffsetOfElement,
			    before.lhs(),new Arg[]{before.rhs(1)},
			    Util.extractType(h,before.rhs(0))));
		}
	    });
        
        interceptors.put(
            Runtime.magic.getMethod(
                "(Ljava/lang/String;)P",
                "getMethodRec"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    try {
                        before.prepend(
                            new GetMethodInst(
                                before.di(),OpCode.GetMethodRec,
                                before.lhs(),Arg.EMPTY,
                                before.getContext().resolveMethod(
                                    Util.extractString(h,before.rhs(0)))));
                    } catch (ResolutionFailed e) {
                        // not sure if this is what we want, but oh well....
                        h.code().getContext().resolutionReport.addUse(
                            e.getResolutionID(),
                            h.code().origin().getResolutionID());
                        before.prepend(
                            new SimpleInst(
                                before.di(),OpCode.Mov,
                                before.lhs(),new Arg[]{ PointerConst.make(0) }));
                    }
                }
            });
	
	Interceptor instWeakCASInterceptor=
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
                    if (before.rhs(0).type()==Exectype.NULL) {
                        before.prepend(
                            new SimpleInst(
                                before.di(),OpCode.NullCheck,
                                Var.VOID,new Arg[]{Arg.NULL}));
                        // hack to make compiler happy until the nullcheck
                        // gets owned.
                        before.prepend(
                            new SimpleInst(
                                before.di(),OpCode.Mov,
                                before.lhs(),new Arg[]{before.rhs(2)}));
                    } else {
                        before.prepend(
                            new HeapAccessInst(
                                before.di(),OpCode.WeakCASField,
                                before.lhs(),new Arg[]{
                                    before.rhs(0),
                                    before.rhs(2),
                                    before.rhs(3)
                                },
                                Util.extractInstField(h,before.rhs(0),before.rhs(1))));
                    }
		}
	    };
        
        interceptors.put(
            Runtime.magic.getMethod(
                "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                "getObjField"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new HeapAccessInst(
                            before.di(),OpCode.GetField,
                            before.lhs(),new Arg[]{
                                before.rhs(0)
                            },
                            Util.extractInstField(h,before.rhs(0),before.rhs(1))));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V",
                "putField"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new HeapAccessInst(
                            before.di(),OpCode.PutField,
                            before.lhs(),new Arg[]{
                                before.rhs(0),
                                before.rhs(2)
                            },
                            Util.extractInstField(h,before.rhs(0),before.rhs(1))));
                }
            });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Object;Ljava/lang/String;II)Z",
		"weakCAS"),
	    instWeakCASInterceptor);
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Object;Ljava/lang/String;PP)Z",
		"weakCAS"),
	    instWeakCASInterceptor);
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Z",
		"weakCAS"),
	    instWeakCASInterceptor);
	
	Interceptor staticWeakCASInterceptor=
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new HeapAccessInst(
			    before.di(),OpCode.WeakCASStatic,
			    before.lhs(),new Arg[]{
				before.rhs(2),
				before.rhs(3)
			    },
			    Util.extractStaticField(h,before.rhs(0),before.rhs(1))));
		}
	    };
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Class;Ljava/lang/String;II)Z",
		"weakStaticCAS"),
	    staticWeakCASInterceptor);
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Class;Ljava/lang/String;PP)Z",
		"weakStaticCAS"),
	    staticWeakCASInterceptor);
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Z",
		"weakStaticCAS"),
	    staticWeakCASInterceptor);
	
	Interceptor arrayWeakCASInterceptor=
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new HeapAccessInst(
			    before.di(),OpCode.WeakCASElement,
			    before.lhs(),before.rhs(),
			    ArrayElementField.INSTANCE));
		}
	    };
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"([IIII)Z",
		"weakCAS"),
	    arrayWeakCASInterceptor);
	interceptors.put(
	    Runtime.magic.getMethod(
		"([PIPP)Z",
		"weakCAS"),
	    arrayWeakCASInterceptor);
	interceptors.put(
	    Runtime.magic.getMethod(
		"([Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;)Z",
		"weakCAS"),
	    arrayWeakCASInterceptor);

	// branch prediction
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Z)Z",
		"likely"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.LikelyNonZero,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Z)Z",
		"unlikely"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.LikelyZero,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Z)Z",
		"semanticallyLikely"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.SemanticallyLikelyNonZero,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Z)Z",
		"semanticallyUnlikely"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.SemanticallyLikelyZero,
			    before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"()V",
		"unlikely"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    h.setProbability(HeaderProbability.UNLIKELY_TO_EXECUTE);
		}
	    });
	
	// hard use
	
	interceptors.put(
	    Runtime.magic.getMethod(
		"(Ljava/lang/Object;)V",
		"hardUse"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(
			    before.di(),OpCode.HardUse,
			    Var.VOID,before.rhs()));
		}
	    });
        
        // fiats

        interceptors.put(
            Runtime.magic.getMethod(
                "(J)D",
                "fiatLongToDouble"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.DOUBLE));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(D)J",
                "fiatDoubleToLong"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.LONG));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(I)F",
                "fiatIntToFloat"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.FLOAT));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(F)I",
                "fiatFloatToInt"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.INT));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(J)I",
                "fiatLongToInt"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.INT));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(J)F",
                "fiatLongToFloat"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.FLOAT));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(J)S",
                "fiatLongToShort"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.SHORT));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(J)C",
                "fiatLongToChar"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.CHAR));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(J)B",
                "fiatLongToByte"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.BYTE));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(J)Z",
                "fiatLongToBoolean"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.BOOLEAN));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(J)P",
                "fiatLongToPointer"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new TypeInst(
                            before.di(),OpCode.Fiat,
                            before.lhs(),before.rhs(),
                            Type.POINTER));
                }
            });
        
        interceptors.put(
            Runtime.magic.getMethod(
                "(D)D",
                "sqrt"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new SimpleInst(
                            before.di(),OpCode.Sqrt,
                            before.lhs(),before.rhs()));
                }
            });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(PPP)V",
                "memcpy"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new SimpleInst(
                            before.di(),OpCode.Memcpy,
                            before.lhs(),before.rhs()));
                }
            });
	
	// C variable access
        
        interceptors.put(
            Runtime.cVar.getMethod(
                "(Ljava/lang/String;)P",
                "addressOf"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    String varName=Util.extractString(h,before.rhs(0));
                    before.prepend(
                        new CFieldInst(
                            before.di(),OpCode.GetCVarAddress,
                            before.lhs(),Arg.EMPTY,
                            GodGivenCVar.make(Basetype.VOID, varName)));
                }
            });
	
	Interceptor cVarGetter=
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    String varName=Util.extractString(h,before.rhs(0));
		    assert !before.lhs().type().isObject();
		    Basetype type=before.lhs().type().effectiveBasetype();
		    assert type!=Basetype.OBJECT;
		    before.prepend(
			new CFieldInst(
			    before.di(),OpCode.GetCVar,
			    before.lhs(),Arg.EMPTY,
			    GodGivenCVar.make(type, varName)));
		}
	    };
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)Z",
		"getBoolean"),
	    cVarGetter);
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)B",
		"getByte"),
	    cVarGetter);
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)C",
		"getChar"),
	    cVarGetter);
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)S",
		"getShort"),
	    cVarGetter);
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)I",
		"getInt"),
	    cVarGetter);
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)J",
		"getLong"),
	    cVarGetter);
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)P",
		"getPointer"),
	    cVarGetter);
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)F",
		"getFloat"),
	    cVarGetter);
	
	interceptors.put(
	    Runtime.cVar.getMethod(
		"(Ljava/lang/String;)D",
		"getDouble"),
	    cVarGetter);
	
	// C field access
	
	Interceptor cFieldGetter=
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    assert !before.lhs().type().isObject();
		    Basetype type=before.method().getType().effectiveBasetype();
		    assert type!=Basetype.OBJECT;
		    before.prepend(
			new CFieldInst(
			    before.di(),OpCode.GetCField,
			    before.lhs(),new Arg[]{before.rhs(0)},
			    CStructField.make(type,
                                              Util.extractString(h,before.rhs(2)),
                                              Util.extractString(h,before.rhs(1)),
                                              before.di())));
		}
	    };
	
	Interceptor cFieldPutter=
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    assert !before.rhs(3).type().isObject();
		    Basetype type=before.method().getParam(3).effectiveBasetype();
		    assert type!=Basetype.OBJECT;
		    before.prepend(
			new CFieldInst(
			    before.di(),OpCode.PutCField,
			    before.lhs(),new Arg[]{
				before.rhs(0),
				before.rhs(3)},
			    CStructField.make(type,
                                              Util.extractString(h,before.rhs(2)),
                                              Util.extractString(h,before.rhs(1)),
                                              before.di())));
		}
	    };
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)Z",
		"getBoolean"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)B",
		"getByte"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)C",
		"getChar"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)S",
		"getShort"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)I",
		"getInt"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)J",
		"getLong"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)P",
		"getPointer"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)F",
		"getFloat"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;)D",
		"getDouble"),
	    cFieldGetter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;Z)V",
		"put"),
	    cFieldPutter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;B)V",
		"put"),
	    cFieldPutter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;C)V",
		"put"),
	    cFieldPutter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;S)V",
		"put"),
	    cFieldPutter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;I)V",
		"put"),
	    cFieldPutter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;J)V",
		"put"),
	    cFieldPutter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;P)V",
		"put"),
	    cFieldPutter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;F)V",
		"put"),
	    cFieldPutter);
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(PLjava/lang/String;Ljava/lang/String;D)V",
		"put"),
	    cFieldPutter);
        
        Interceptor cFieldCaser=
            new Interceptor(){
                protected void prepend(Header h,MethodInst before) {
                    Var tmp=h.code().addVar(Exectype.POINTER);
                    Type type=before.rhs(3).type().asType();
                    before.prepend(
                        new CFieldInst(
                            before.di(),OpCode.GetCFieldAddress,
                            tmp,new Arg[]{before.rhs(0)},
                            CStructField.make(type.effectiveBasetype(),
                                              Util.extractString(h,before.rhs(2)),
                                              Util.extractString(h,before.rhs(1)),
                                              before.di())));
                    before.prepend(
                        new MemoryAccessInst(
                            before.di(),OpCode.WeakCAS,
                            before.lhs(),new Arg[]{
                                tmp,
                                before.rhs(3),
                                before.rhs(4)
                            },
                            type,
                            Mutability.MUTABLE,
                            Volatility.VOLATILE));
                }
            };
        
        interceptors.put(
            Runtime.cType.getMethod(
                "(PLjava/lang/String;Ljava/lang/String;II)Z",
                "weakCAS"),
            cFieldCaser);
	
        interceptors.put(
            Runtime.cType.getMethod(
                "(PLjava/lang/String;Ljava/lang/String;PP)Z",
                "weakCAS"),
            cFieldCaser);
	
	// C type access
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(Ljava/lang/String;)P",
		"sizeof"),
	    new Interceptor(){
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new CTypeInst(
			    before.di(),OpCode.GetCTypeSize,
			    before.lhs(),Arg.EMPTY,
			    CType.forName(Util.extractString(h,before.rhs(0)))));
		}
	    });
	
	interceptors.put(
	    Runtime.cType.getMethod(
		"(Ljava/lang/String;Ljava/lang/String;)P",
		"offsetof"),
	    new Interceptor(){
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new CFieldInst(
			    before.di(),OpCode.GetCFieldOffset,
			    before.lhs(),Arg.EMPTY,
			    CStructField.make(Util.extractString(h,before.rhs(1)),
                                              Util.extractString(h,before.rhs(0)),
                                              before.di())));
		}
	    });

	// FCMagic
	
	interceptors.put(
	    Runtime.fcMagic.getMethod(
		"(f)P",
		"toVMPointer"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(before.di(),OpCode.Mov,
				       before.lhs(),before.rhs()));
		}
	    });
	
	interceptors.put(
	    Runtime.fcMagic.getMethod(
		"(P)f",
		"fromVMPointer"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new SimpleInst(before.di(),OpCode.Mov,
				       before.lhs(),before.rhs()));
		}
	    });

	interceptors.put(
	    Runtime.fcMagic.getMethod(
		"(Ljava/lang/Object;I)f",
		"addressOfElement"),
	    new Interceptor() {
		protected void prepend(Header h,MethodInst before) {
		    before.prepend(
			new HeapAccessInst(
			    before.di(),OpCode.AddressOfElement,
			    before.lhs(),before.rhs(),
                            ArrayElementField.INSTANCE));
		}
	    });
	
        interceptors.put(
            Runtime.magic.getMethod(
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                "callCloneHelper"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new MethodInst(
                            before.di(),OpCode.InvokeDynamic,
                            before.lhs(),before.rhs(),
                            Global.root().resolve(
                                Global.root().objectClass,
                                Global.root().objectClass,
                                new MethodSignature(
                                    Global.root().objectType,
                                    Constants.SMN_CLONE_HELPER,
                                    Type.EMPTY))));
                }
            });

        interceptors.put(
            Runtime.magic.getMethod(
                "()V",
                "pollcheck"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new SimpleInst(
                            before.di(),OpCode.PollCheck,
                            Var.VOID,Arg.EMPTY));
                }
            });
        
        interceptors.put(
            Runtime.magic.getMethod(
                "(Ljava/lang/Object;)P",
                "typeDataFor"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    before.prepend(
                        new SimpleInst(
                            before.di(),OpCode.GetTypeDataForObject,
                            before.lhs(),before.rhs()));
                }
            });
        
        interceptors.put(
            Runtime.gate.getMethod(
                "(Ljava/util/TreeMap;)V",
                "initializeBuilders"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    // NO OP right now!
                }
            });
        
        interceptors.put(
            Runtime.gateHelpers.getMethod(
                "(Ljava/lang/Object;PPLcom/fiji/mvm/Gate;)V",
                "callGateCopyTo"),
            new Interceptor() {
                protected void prepend(Header h,MethodInst before) {
                    // FIXME
                }
            });
    }
}

