/*
 * LowerOneWordHeaderModel.java
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


public class LowerOneWordHeaderModel extends CodePhase {
    public LowerOneWordHeaderModel(Code code) {
	super(code);
    }
    
    public void visitCode() {
        // NOTE: this is a somewhat retarded pass
        
	// what does this do?  transforms operations that rely on the header
	// into operations in terms of the header.  need to transform:
	// - get type data
	// - hash codes
	
	assert Global.hm.numHeaderWords()==1;
	
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case GetTypeDataForObject: {
                    Var objPtr=code.addVar(Exectype.POINTER);
		    Var hdrPtr=code.addVar(Exectype.POINTER);
		    Var monPtr=code.addVar(Exectype.POINTER);
                    i.prepend(
                        new TypeInst(
                            i.di(),OpCode.Cast,
                            objPtr,new Arg[]{i.rhs(0)},
                            Type.POINTER));
		    i.prepend(
			new SimpleInst(
			    i.di(),OpCode.Sub,
			    hdrPtr,new Arg[]{
                                objPtr,
                                PointerConst.make(Global.objectTDOffset)
                            }));
		    i.prepend(
			new MemoryAccessInst(
			    i.di(),OpCode.Load,
			    monPtr,new Arg[]{hdrPtr},
			    Type.POINTER));
		    if (Global.hm==HeaderModel.POISONED) {
			Var mask=code.addVar(Exectype.POINTER);
                        Var newMonPtr=code.addVar(Exectype.POINTER);
			i.prepend(
			    new SimpleInst(
				i.di(),OpCode.BitNot,
				mask,new Arg[]{PointerConst.make(1)}));
			i.prepend(
			    new SimpleInst(
				i.di(),OpCode.And,
				newMonPtr,new Arg[]{monPtr,mask}));
                        monPtr=newMonPtr;
		    }
		    i.prepend(
			new CFieldInst(
			    i.di(),OpCode.GetCField,
			    i.lhs(),new Arg[]{monPtr},
			    CTypesystemReferences.Monitor_forward));
		    i.remove();
                    setChangedCode();
		    break;
		}
		default: break;
		}
	    }
	}
	
	if (changedCode()) code.killIntraBlockAnalyses();
    }
}

