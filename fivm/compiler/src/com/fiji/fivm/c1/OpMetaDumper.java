/*
 * OpMetaDumper.java
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

public class OpMetaDumper extends Visitor< String > {
    private OpMetaDumper() {}
    private static OpMetaDumper d=new OpMetaDumper();
    
    public static String get(Operation o) {
	return o.accept(d);
    }
    
    public String visit(Node n) {
	return "";
    }
    
    public String visit(Switch s) {
	StringBuilder buf=new StringBuilder("<");
	for (int i=0;i<s.values.length;++i) {
	    if (i!=0) {
		buf.append(", ");
	    }
	    buf.append(s.values[i]);
	}
	buf.append(">");
	return buf.toString();
    }
    
    public String visit(PatchPoint p) {
        return "<"+p.neededClass()+", "+p.description()+", bcOffset="+p.bcOffset()+", nLocals="+p.nLocals()+", nStack="+p.nStack()+">";
    }
    
    public String visit(PatchPointFooter p) {
        return "<"+p.neededClass()+", "+p.description()+", bcOffset="+p.bcOffset()+", nLocals="+p.nLocals()+", nStack="+p.nStack()+">";
    }
    
    public String visit(ArgInst a) {
	return "<"+a.argIdx+">";
    }
    
    public String visit(CFieldInst c) {
	return "<"+c.field()+">";
    }
    
    public String visit(CTypeInst c) {
	return "<"+c.ctype()+">";
    }
    
    public String visit(CMacroInst c) {
	return "<"+c.cmacro()+">";
    }
    
    public String visit(FieldInst a) {
	return "<"+a.field()+">";
    }
    
    public String visit(HeapAccessInst a) {
	return "<"+a.field()+", "+a.mode()+">";
    }
    
    public String visit(ClassInst c) {
	return "<"+c.value()+">";
    }
    
    public String visit(GetStringInst g) {
	return "<"+Util.dump(g.value())+">";
    }
    
    public String visit(GetMethodInst m) {
	return "<"+m.method()+">";
    }
    
    public String visit(MethodInst m) {
	return "<"+m.method()+", "+m.refinement()+">";
    }
    
    public String visit(MultiNewArrayInst m) {
	return "<"+m.type()+", "+m.dimensions()+">";
    }
    
    public String visit(CallIndirectInst c) {
	String result="<"+c.result()+" (";
	for (int i=0;i<c.params().length;++i) {
	    if (i!=0) {
		result+=", ";
	    }
	    result+=c.params()[i];
	}
	return result+")>";
    }
    
    public String visit(ResolvedMethodInst m) {
        return "<"+m.signature()+", "+m.function()+">";
    }
    
    public String visit(IndirectMethodInst m) {
        return "<"+m.signature()+">";
    }
    
    public String visit(TypeInst t) {
        StringBuilder buf=new StringBuilder();
        buf.append("<");
        buf.append(t.getType());
        if (t.hasOrigType()) {
            buf.append(" (");
            buf.append(t.getOrigType());
            buf.append(")");
        }
        buf.append(">");
	return buf.toString();
    }
    
    public String visit(MemoryAccessInst t) {
        StringBuilder buf=new StringBuilder();
        buf.append("<");
        buf.append(t.getType());
        if (t.hasOrigType()) {
            buf.append(" (");
            buf.append(t.getOrigType());
            buf.append(")");
        }
        buf.append(", ");
        buf.append(t.mutability());
        buf.append(", ");
        buf.append(t.volatility());
        buf.append(">");
	return buf.toString();
    }
    
    public String visit(DebugIDInfoInst d) {
	return "<"+d.didi()+">";
    }
    
    public String visit(TypeCheckInst t) {
	return "<check = "+t.typeToCheck+", throw = "+t.typeToThrow+">";
    }
}

