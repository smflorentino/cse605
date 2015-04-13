/*
 * OptString.java
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

import java.util.HashSet;
import java.util.ArrayList;

public class OptString extends CodePhase {
    public OptString(Code c) { super(c); }
    
    static final VisibleClass stringBuffer=
	Global.root().forceClass("java/lang/StringBuffer");
    static final VisibleClass stringBuilder=
	Global.root().forceClass("java/lang/StringBuilder");
    
    static boolean appendMethodCall(Operation o) {
	if (o instanceof MethodInst) {
	    MethodInst mi=(MethodInst)o;
	    if (mi.method().getClazz()==stringBuffer &&
		mi.method().getName().equals("append") &&
		mi.rhs(0) instanceof Var &&
		mi.lhs().type()==stringBuffer.asExectype()) {
		return true;
	    }
	}
	return false;
    }
    
    static boolean constructorCall(Operation o) {
	if (o.opcode()==OpCode.Invoke) {
	    MethodInst mi=(MethodInst)o;
	    if (mi.method().getClazz()==stringBuffer &&
		mi.method().isInitializer() &&
		mi.rhs(0) instanceof Var) {
		return true;
	    }
	}
	return false;
    }
    
    public boolean toStringCall(Operation o) {
	if (o instanceof MethodInst) {
	    MethodInst mi=(MethodInst)o;
	    if (mi.method().getClazz()==stringBuffer &&
		mi.method().getName().equals("toString") &&
		mi.rhs(0) instanceof Var) {
		return true;
	    }
	}
	return false;
    }
    
    public void visitCode() {
	assert code.isSSA();
	
	// find all instantiations of StringBuffer
	
	HashSet< Var > sbufVar=new HashSet< Var >();
	
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		if (i.opcode()==OpCode.New) {
		    TypeInst ti=(TypeInst)i;
		    if (ti.getType()==stringBuffer.asType()) {
			sbufVar.add(ti.lhs());
		    }
		}
	    }
	}
	
	if (sbufVar.isEmpty()) {
	    return;
	}
	
	// build up a list of variables that are known to be aliased to the instantiation.
	// this is necessary because you may have code like:
	// new StringBuffer().append(foo).append(bar).toString();
	// so we need to know that the result of append() is must-aliased to the receiver.
	
	// the keys are the instantiation variables, plus some others possibly (it's
	// essentially a must-alias set which may or may not store transitive relationships
	// until we fix it below)
	TwoWayMap< Var, Var > aliases=new TwoWayMap< Var, Var >();
	
	for (Header h : code.depthFirstHeaders()) {
	    for (Instruction i : h.instructions()) {
		if (appendMethodCall(i)) {
		    MethodInst mi=(MethodInst)i;
		    assert mi.rhs(0)!=i.lhs();
		    aliases.put((Var)mi.rhs(0),i.lhs());
		}
	    }
	}
	
	// close the aliases list
	
	boolean changed=true;
	while (changed) {
	    changed=false;
	    TwoWayMap< Var, Var > newAliases=new TwoWayMap< Var, Var >();
	    for (Var key : aliases.keySet()) {
		if (aliases.containsValue(key)) {
		    newAliases.putMulti(aliases.keyForValue(key),
					aliases.valuesForKey(key));
		    changed=true;
		} else {
		    newAliases.putMulti(key,
					aliases.valuesForKey(key));
		}
	    }
	    if (changed) {
		aliases=newAliases;
	    }
	}
	
	// make it non-strict
	
	for (Var key : new ArrayList< Var >(aliases.keySet())) {
	    aliases.put(key,key);
	}
	
	// kill off keys that aren't instantiated by us
	
	for (Var key : new ArrayList< Var >(aliases.keySet())) {
	    if (!sbufVar.contains(key)) {
		aliases.killKey(key);
	    }
	}
	
	// figure out if there are any escapes
	
	for (Header h : code.headers()) {
	    for (Operation o : h.operations()) {
		if (o.usesAny(aliases.valueSet()) &&
		    !(appendMethodCall(o) ||
		      constructorCall(o) ||
		      toStringCall(o))) {
		    for (Arg a : o.rhs()) {
			aliases.killKey(aliases.keyForValue(a));
		    }
		}
	    }
	}
	
	// do the conversion
	
	if (aliases.isEmpty()) {
	    return;
	}
	
	if (Global.verbosity>=2) {
	    Global.log.println("Introduced StringBuilder to "+code);
	}
	
	for (Var v : aliases.valueSet()) {
	    assert v.type()==stringBuffer.asExectype();
	    v.t=stringBuilder.asExectype();
	}
	
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case New: {
		    TypeInst ti=(TypeInst)i;
		    if (aliases.containsKey(ti.lhs())) {
			ti.type=stringBuilder.asType();
		    }
		    break;
		}
		case Invoke:
		case InvokeDynamic: {
		    MethodInst mi=(MethodInst)i;
		    if (aliases.containsValue(mi.rhs(0))) {
			MethodSignature ms=mi.method().getSignature();
			if (ms.getType()==stringBuffer.asType()) {
			    ms=new MethodSignature(stringBuilder.asType(),
						   ms.getName(),
						   ms.getParams());
			}
			VisibleMethod vm=
			    stringBuilder.getMethod(ms);
			assert vm!=null : "for "+mi+" in "+h;
			mi.prepend(
			    new MethodInst(
				mi.di(),mi.opcode(),
				mi.lhs(),mi.rhs(),
				vm));
			mi.remove();
		    }
		    break;
		}
		default: break;
		}
	    }
	}
	
	setChangedCode();
	code.killAllAnalyses();
    }
}


