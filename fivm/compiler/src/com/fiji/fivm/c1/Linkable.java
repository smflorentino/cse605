/*
 * Linkable.java
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

import java.io.*;
import java.util.*;

public abstract class Linkable extends CField implements Pointerable {
    
    protected Linkable(Basetype type,String name) {
	super(type,name);
    }
    
    public abstract boolean isLocal();
    
    public String asCCode() {
	return "(&"+getName()+")";
    }
    
    public Arg generateIR(Code code,Operation before) {
        Var result=code.addVar(Exectype.POINTER);
        before.prepend(
            new CFieldInst(
                before.di(),OpCode.GetCVarAddress,
                result,Arg.EMPTY,
                this));
        return result;
    }
    
    public abstract void generateDeclaration(PrintWriter w);
    public void generateDefinition(PrintWriter w) {}
    
    public void generateDefinitionWithSubDefinitions(PrintWriter w) {
	for (Linkable l : linkables()) {
	    l.generateDeclaration(w);
	}
	for (Linkable l : linkables()) {
	    l.generateDefinition(w);
	}
    }
    
    public String asmSection() {
        if (isLocal()) {
            throw new CompilerException("Cannot determine assembly section for "+this);
        } else {
            return ".text";
        }
    }
    
    public void generateAsm(PrintWriter w) {
        if (isLocal()) {
            throw new CompilerException("Cannot generate assembly for "+this);
        }
    }
    
    public int sizeof() {
        throw new CompilerException("Cannot get sizeof "+this);
    }
    
    public void generateRaw(Pointer target) {
        throw new CompilerException("Cannot generate raw "+this);
    }
    
    public int hashCode() {
	return name.hashCode()+(isLocal()?42:0);
    }
    
    public boolean equals(Object other_) {
	if (this==other_) return true;
	if (!(other_ instanceof Linkable)) return false;
	Linkable other=(Linkable)other_;
	return name.equals(other.name)
	    && isLocal()==other.isLocal();
    }
    
    public LinkableSet subLinkables() { return LinkableSet.EMPTY; }
    
    public final LinkableSet linkables() {
	LinkableSet result=new LinkableSet();
	result.add(this); // adds both me and my subLinkables().
	return result;
    }
    
    /**
     * Helper method for identifying the canonical set of Linkables found in
     * a Pointerable.  A Linkable is a Pointerable, but there is an inconsistency:
     * a Pointerable can only tell you the Linkables it refers to, while a
     * Linkable can more generally tell you all of the Linkables that make it up,
     * which includes itself.  I.e. the general mechanism for getting all of the
     * Linkables associated with a Pointerable requires first inspecting if it's
     * a Linkable, and then calling linkables(), otherwise calling subLinkables().
     * In particular, if you just called subLinkables(), you might miss a Linkable:
     * namely, the Pointerable itself if it was a Linkable.
     * <p>
     * This method serves to short-circuit the process; if you give it a Pointerable
     * it will always correctly give you the entire LinkableSet.
     */
    public static LinkableSet linkables(Pointerable ptr) {
        if (ptr instanceof Linkable) {
            return ((Linkable)ptr).linkables();
        } else {
            return ptr.subLinkables();
        }
    }
    
    public String toString() {
	return "Linkable["+getName()+(isLocal()?", local":"")+", "+getClass()+"]";
    }

    public static Linkable[] EMPTY=new Linkable[0];
    
    private static HashSet< String > predeclared;
    
    static void initPredeclared() {
        assert predeclared==null;
        predeclared=new HashSet< String >();
        // FIXME: we may want to get rid of this whole "predeclared" thing since it
        // isn't really relevant no more
    }
    
    public static HashSet< String > predeclared() {
        assert predeclared!=null;
	return predeclared;
    }
}

