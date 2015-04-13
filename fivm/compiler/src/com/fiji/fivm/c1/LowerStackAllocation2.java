/*
 * LowerStackAllocation2.java
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

import com.fiji.fivm.*;

public class LowerStackAllocation2 extends CodePhase {
    public LowerStackAllocation2(Code c) { super(c); }
    
    Arg codeForGCSpaceAlloc(Operation before) {
	Var tmp1=code.addVar(Exectype.POINTER);
	Var tmp2=code.addVar(Exectype.POINTER);
	Var tmp3=code.addVar(Exectype.POINTER);
	Var tmp4=code.addVar(Exectype.POINTER);
        Var result=code.addVar(Exectype.POINTER);
	
	// NOTE: all of this code gets constantified (either by GCC, or, once we
	// have struct mapping in the compiler, by us)
	
	before.prepend(
	    new CFieldInst(
		before.di(),OpCode.GetCFieldAddress,
		tmp1,new Arg[]{Arg.THREAD_STATE},
		CTypesystemReferences.ThreadState_gc));
	before.prepend(
	    new CFieldInst(
		before.di(),OpCode.GetCFieldAddress,
		tmp2,new Arg[]{tmp1},
		CTypesystemReferences.GCData_alloc));
	before.prepend(
	    new CTypeInst(
		before.di(),OpCode.GetCTypeSize,
		tmp3,Arg.EMPTY,
		CTypesystemReferences.GCSpaceAlloc_TYPE));
	before.prepend(
	    new SimpleInst(
		before.di(),OpCode.Mul,
		tmp4,new Arg[]{
		    tmp3,
		    PointerConst.make(Global.gc.stackAllocSpace())
		}));
	before.prepend(
	    new SimpleInst(
		before.di(),OpCode.Add,
		result,new Arg[]{tmp2,tmp4}));
	
	return result;
    }
    
    public void visitCode() {
	Arg allocFrame;
	
	switch (code.origin().origin().alloc) {
	case STACK_ALLOC: {
            CLocal allocFrameLocal=new CLocal(CTypesystemReferences.ScopeID_TYPE,"StackID");
            code.addLocal(allocFrameLocal);
            
	    Var saveBump=code.addVar(Exectype.POINTER);
	    Var saveAllocFrame=code.addVar(Exectype.POINTER);
	    
	    Operation insertPoint=code.reroot().first();
	    
            allocFrame=code.addVar(Exectype.POINTER);
            insertPoint.prepend(
                new CFieldInst(
                    insertPoint.di(),OpCode.GetCVarAddress,
                    (Var)allocFrame,Arg.EMPTY,
                    allocFrameLocal));
            insertPoint.prepend(
                new CFieldInst(
                    insertPoint.di(),OpCode.PutCField,
                    Var.VOID,new Arg[]{
                        allocFrame,
                        PointerConst.make(Constants.SCOPEID_STACK)
                    },
                    CTypesystemReferences.ScopeID_word));
            
	    Arg gcSpaceAlloc=codeForGCSpaceAlloc(insertPoint);
	    insertPoint.prepend(
		new CFieldInst(
		    insertPoint.di(),OpCode.GetCField,
		    saveBump,new Arg[]{gcSpaceAlloc},
		    CTypesystemReferences.GCSpaceAlloc_bump));
	    insertPoint.prepend(
		new CFieldInst(
		    insertPoint.di(),OpCode.GetCField,
		    saveAllocFrame,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_allocFrame));
	    insertPoint.prepend(
		new CFieldInst(
		    insertPoint.di(),OpCode.PutCField,
		    Var.VOID,new Arg[]{
			Arg.THREAD_STATE,
			allocFrame
		    },
		    CTypesystemReferences.ThreadState_allocFrame));
	    
	    for (Header h : code.headers()) {
		if (EscapingFooterCalc.get(h)) {
		    Footer f=h.getFooter();
		    
		    gcSpaceAlloc=codeForGCSpaceAlloc(f);
		    f.prepend(
			new CFieldInst(
			    f.di(),OpCode.PutCField,
			    Var.VOID,new Arg[]{
				gcSpaceAlloc,
				saveBump
			    },
			    CTypesystemReferences.GCSpaceAlloc_bump));
		    f.prepend(
			new CFieldInst(
			    insertPoint.di(),OpCode.PutCField,
			    Var.VOID,new Arg[]{
				Arg.THREAD_STATE,
				saveAllocFrame
			    },
			    CTypesystemReferences.ThreadState_allocFrame));
		}
	    }
	    
	    setChangedCode();
	    code.killAllAnalyses();
	    break;
	}
	case DEFAULT_ALLOC: {
	    allocFrame=PointerConst.make(0);
	    break;
	}
	case ALLOC_AS_CALLER: {
	    Var allocFrameVar=code.addVar(Exectype.POINTER);
	    code.reroot().append(
		new CFieldInst(
		    code.root().di(),OpCode.GetCField,
		    allocFrameVar,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_allocFrame));
	    
	    setChangedCode();
            code.killAllAnalyses();
	    
	    allocFrame=allocFrameVar;
	    break;
	}
	default: throw new Error();
	}
	
	for (Header h : code.headers()) {
	    for (Operation o : h.operations()) {
		for (int i=0;i<o.rhs().length;++i) {
		    if (o.rhs(i)==Arg.ALLOC_FRAME) {
			o.rhs[i]=allocFrame;
                        setChangedCode();
		    }
		}
	    }
	}
	
	if (changedCode()) {
            code.killIntraBlockAnalyses();
	}
    }
}


