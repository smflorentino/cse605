/*
 * InterceptIntrinsicFields.java
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
import java.lang.reflect.*;

import com.fiji.fivm.Settings;

public class InterceptIntrinsicFields extends CodePhase {
    public InterceptIntrinsicFields(Code code) { super(code); }
    
    protected static abstract class Interceptor {
	protected abstract void prepend(Header h,HeapAccessInst before);
    }
    
    static class TypeField extends Interceptor {
	Type t;
	TypeField(Type t) { this.t=t; }
	protected void prepend(Header h,HeapAccessInst before) {
	    before.prepend(
		new TypeInst(
		    before.di(),OpCode.GetType,
		    before.lhs(),Arg.EMPTY,
		    t));
	}
    }
    
    static HashMap< VisibleField, Interceptor > interceptors=
        new HashMap< VisibleField, Interceptor >();
    
    static {
	interceptors.put(
	    Global.root().resolveField("Ljava/lang/Void;/TYPE/Ljava/lang/Class;"),
	    new TypeField(Type.VOID));
	interceptors.put(
	    Global.root().resolveField("Ljava/lang/Byte;/TYPE/Ljava/lang/Class;"),
	    new TypeField(Type.BYTE));
	interceptors.put(
	    Global.root().resolveField("Ljava/lang/Character;/TYPE/Ljava/lang/Class;"),
	    new TypeField(Type.CHAR));
	interceptors.put(
	    Global.root().resolveField("Ljava/lang/Short;/TYPE/Ljava/lang/Class;"),
	    new TypeField(Type.SHORT));
	interceptors.put(
	    Global.root().resolveField("Ljava/lang/Integer;/TYPE/Ljava/lang/Class;"),
	    new TypeField(Type.INT));
	interceptors.put(
	    Global.root().resolveField("Ljava/lang/Long;/TYPE/Ljava/lang/Class;"),
	    new TypeField(Type.LONG));
	interceptors.put(
	    Global.root().resolveField("Ljava/lang/Float;/TYPE/Ljava/lang/Class;"),
	    new TypeField(Type.FLOAT));
	interceptors.put(
	    Global.root().resolveField("Ljava/lang/Double;/TYPE/Ljava/lang/Class;"),
	    new TypeField(Type.DOUBLE));
        
        for (Field f_ : Settings.class.getFields()) {
            final Field f=f_;
            if (!f.isSynthetic()) {
                interceptors.put(
                    Runtime.settings.getFieldByName(f.getName()),
                    new Interceptor(){
                        protected void prepend(Header h,HeapAccessInst before) {
                            try {
                                before.prepend(
                                    new SimpleInst(
                                        before.di(),OpCode.Mov,
                                        before.lhs(),new Arg[]{
                                            IntConst.make(f.getBoolean(null))
                                        }));
                            } catch (Throwable t) {
                                Util.rethrow(t);
                            }
                        }
                    });
            }
        }
        
        interceptors.put(
            Global.root().resolveField("Lcom/fiji/fivm/Detector;/IS_FIJI/Z;"),
            new Interceptor(){
                protected void prepend(Header h,HeapAccessInst before) {
                    before.prepend(
                        new SimpleInst(
                            before.di(),OpCode.Mov,
                            before.lhs(),new Arg[]{
                                IntConst.make(true)
                            }));
                }
            });
    }
    
    public static Set< VisibleField > interceptedFields() {
        return interceptors.keySet();
    }
    
    public void visitCode() {
	assert code.isSSA();
	
	for (Header h : code.headers2()) {
	    for (Instruction i : h.instructions2()) {
		if (i.opcode()==OpCode.GetField ||
		    i.opcode()==OpCode.GetStatic) {
		    HeapAccessInst fi=(HeapAccessInst)i;
		    Interceptor interceptor=interceptors.get(fi.field());
		    if (interceptor!=null) {
			try {
			    interceptor.prepend(h,fi);
			} catch (Throwable e) {
			    throw new CompilerException(
				"Could not compile intrinsic field "+fi+" in "+h,e);
			}
			fi.remove();
			setChangedCode();
		    }
		}
	    }
	}
	if (changedCode()) code.killIntraBlockAnalyses();
    }
}


