/*
 * MethodInst.java
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

import java.util.ArrayList;
import java.nio.ByteBuffer;

public final class MethodInst extends Instruction implements MTSInstable, MemberInstable {
    
    VisibleMethod method;

    ClassBound refinement;

    // need to think about how to do this.  we need a fast way of checking
    // if a virtual call will route to a given target...
    ClassBound probRefinement;
    
    MethodInst(DebugInfo di,
	       OpCode opcode,
	       Var lhs,
	       Arg[] rhs,
	       VisibleMethod method) {
	super(di,opcode,lhs,rhs);
        if (method==null) {
            throw new CompilerException("method is null!!");
        }
	this.method=method;
        if (this.method==null) {
            throw new CompilerException("method is null!!");
        }
	rerefine();
    }
    
    MethodInst(DebugInfo di,
               OpCode opcode,
               Var lhs,
               Arg[] rhs,
               VisibleMethod method,
               ClassBound refinement) {
        this(di,opcode,lhs,rhs,method);
        refine(refinement);
    }
    
    public VisibleMethod method() { return method; }
    public VisibleMethod member() { return method; }
    
    public MethodTypeSignature signature() { return method.getTypeSignature(); }
    
    public ClassBound refinement() { return refinement; }
    
    public void rerefine() {
        if (method==null) {
            throw new CompilerException("method is null!!");
        }
	this.refinement=new ClassBound(method.getClazz(),
				       TypeBoundMode.UPPER_BOUND);
    }
    
    public boolean refine(ClassBound b) {
	assert b.canRefine(refinement)==true : "current refinement = "+refinement+", proposed refinement = "+b;
	if (b.isMoreSpecificThan(refinement)) {
	    refinement=b;
	    return true;
	} else {
	    return false;
	}
    }
    
    public boolean refine(VisibleClass c,TypeBoundMode m) {
	return refine(new ClassBound(c,m));
    }
    
    public boolean refinementAllowsForPruning() {
        if (Global.analysis().closed()) {
            return true;
        } else {
            return refinement.isExact();
        }
    }
    
    public boolean dynamicCall() {
	if (opcode!=OpCode.InvokeDynamic) {
	    return false;
	}
	if (method.isFinal()) {
            if (refinementAllowsForPruning()) {
                assert method.prune(refinement).size()==1 : method+", pruned to "+method.prune(refinement);
            }
	    return false;
	}
        if (refinementAllowsForPruning()) {
            return method.prune(refinement).size()!=1;
        } else {
            return true;
        }
    }
    
    public boolean staticCall() {
        return !dynamicCall();
    }
    
    public boolean deadCall() {
        if (opcode==OpCode.InvokeDynamic && refinementAllowsForPruning()) {
            return method.prune(refinement).size()==0;
        } else {
            return false;
        }
    }
    
    /** Returns the method that we know statically that we are calling.  Undefined
	if dynamicCall() returns true. */
    public VisibleMethod staticTarget() {
	if (opcode==OpCode.InvokeDynamic) {
            if (method.isFinal()) {
                return method;
            }
            if (refinementAllowsForPruning()) {
                return method.prune(refinement).iterator().next();
            } else {
                assert false : this;
                return null;
            }
	} else {
	    return method;
	}
    }
    
    public ArrayList< VisibleMethod > possibleTargets() {
	if (opcode==OpCode.InvokeDynamic) {
	    return method.prune(refinement);
	} else {
	    return Util.makeArray(method);
	}
    }
    
    public <T> T accept(Visitor<T> v) {
	return v.visit(this);
    }
    
    int getNioSize() {
        return super.getNioSize()+4+refinement.getNioSize(); //??
    }
    
    void writeTo(NioContext ctx,
                 ByteBuffer buffer) {
        super.writeTo(ctx,buffer);
        buffer.putInt(Global.methodCoding.codeFor(method));
        refinement.writeTo(buffer);
    }
}

