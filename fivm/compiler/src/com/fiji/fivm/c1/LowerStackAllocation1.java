/*
 * LowerStackAllocation1.java
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

public class LowerStackAllocation1 extends CodePhase {
    public LowerStackAllocation1(Code c) { super(c); }
    
    // some notes:
    //
    // - The inline filters should not inline a @StackAllocation method that allocates
    //   unless it was marked @Inline.  A method marked @AllocateAsCaller should be
    //   must-inlined.  FIXME: still need some way of handling the @AllocateAsCaller
    //   case ... currently we don't do this right.
    //
    //   FIXME: we should never use @Inline when we mean @AllocateAsCaller (i.e.
    //   @Inline should not be used for making allocation decisions).  furthermore,
    //   we should have a separate stack for stack allocation.  and, @AllocateAsCaller
    //   methods should dynamically query the caller's allocation mode (using FRAME)
    //   and choose either stack or heap allocation.  We should have separate
    //   OpCodes for stack allocation.  and, we should have opcodes for FRAME
    //   operations, like:  GetCaller(pointer) and AllocationMode(pointer).
    //   AllocationMode(FRAME) should be a constant (OptConst can take care of it).
    //   And, inlining should change GetCaller(FRAME) to Mov(FRAME).
    //
    // - Store checks seem like they will require an extra header word, because otherwise
    //   we have no way of knowing if two objects were allocated in the same frame or
    //   not.  If that weren't a problem, the store checks should be implementable with
    //   two loads and a compare as follows, provided that the architecture uses a
    //   downward-growing stack:
    //
    //   storeCheck(Object trg, Object src) {
    //       trgAge = (loadLowOrderHeaderBit(trg)<<31>>31) | (trg>>1)
    //       srcAge = (loadLowOrderHeaderBit(src)<<31>>31) | (src>>1)
    //       if (srcAge < trgAge) {
    //           error!
    //       }
    //   }
    //
    //   Note that the idea is that we turn the age into all '1' bits (max int value)
    //   using a sign extension, if the object is in the heap (that's what the tag
    //   bit is for), and otherwise we don't use an extra header word.
    //
    //   If we use an extra header word, then we should have the following store
    //   check:
    //
    //   storeCheck(Object trg,Object src) {
    //       trgAge = GCHeader(trg).next()
    //       srcAge = GCHeader(src).next()
    //       if (srcAge < trgAge) {
    //           error !
    //       }
    //   }
    
    void loadAgeRaw(Header h,Var age,Arg obj) {
	Var tmp1=code.addVar(Exectype.POINTER);
	Var tmp2=code.addVar(Exectype.POINTER);
	
	h.append(
	    new TypeInst(
		h.di(),OpCode.Cast,
		tmp1,new Arg[]{obj},
		Type.POINTER));
	h.append(
	    new SimpleInst(
		h.di(),OpCode.Sub,
		tmp2,new Arg[]{
		    tmp1,
		    PointerConst.make(Global.objectGCOffset)
		}));
	h.append(
	    new MemoryAccessInst(
		h.di(),OpCode.Load,
		age,new Arg[]{tmp2},
		Type.POINTER));
    }
    
    void loadAge(Header h,Var age,Arg obj) {
        Var tmp0=code.addVar(Exectype.POINTER);
	loadAgeRaw(h,tmp0,obj);
	
	// need additional bit twiddling to extend the GC mark bits
	// to fill the whole word (so heap objects will be UINTPTR_MAX)
	
	Var tmp1=code.addVar(Exectype.POINTER);
	Var tmp2=code.addVar(Exectype.POINTER);
	Var tmp3=code.addVar(Exectype.POINTER);
	Var tmp4=code.addVar(Exectype.POINTER);
	
	h.append(
	    new SimpleInst(
		h.di(),OpCode.Shr,
		tmp1,new Arg[]{tmp0,IntConst.make(Global.pointerSize*8-1)}));
	h.append(
	    new SimpleInst(
		h.di(),OpCode.Or,
		tmp2,new Arg[]{tmp0,tmp1}));
	h.append(
	    new SimpleInst(
		h.di(),OpCode.Shl,
		tmp3,new Arg[]{tmp2,IntConst.make(1)}));
	h.append(
	    new SimpleInst(
		h.di(),OpCode.Shr,
		tmp4,new Arg[]{tmp3,IntConst.make(Global.pointerSize*8-1)}));
	h.append(
	    new SimpleInst(
		h.di(),OpCode.Or,
		age,new Arg[]{tmp2,tmp4}));
    }
    
    boolean hasStoreCheck(Instruction i) {
        return i instanceof HeapAccessInst
            && ((HeapAccessInst)i).isInstance()
            && ((HeapAccessInst)i).mode().hasScopeCheck();
    }
    
    boolean hasInHeapCheck(Instruction i) {
        return i.opcode()==OpCode.InHeapCheck
            || (i instanceof HeapAccessInst &&
                ((HeapAccessInst)i).isStatic() &&
                ((HeapAccessInst)i).mode().hasScopeCheck());
    }
    
    void clearStoreCheck(Instruction i) {
        HeapAccessInst hai=(HeapAccessInst)i;
        hai.setMode(hai.mode().withoutScopeCheck());
    }
    
    void clearInHeapCheck(Instruction i) {
        if (i instanceof HeapAccessInst) {
            HeapAccessInst hai=(HeapAccessInst)i;
            hai.setMode(hai.mode().withoutScopeCheck());
        } else {
            i.remove();
        }
    }
    
    public void visitCode() {
	for (Header h : code.headers2()) {
	    for (Instruction o : h.instructions2()) {
                if (hasStoreCheck(o)) {
		    Arg trg=o.rhs(0);
		    Arg src=StoreSourceCalc.get(o);
		    
		    Header cont=h.split(o);
		    Header body=h.makeSimilar(o.di());
		    
		    h.setFooter(
			new Branch(
			    o.di(),OpCode.BranchNonZero,
			    new Arg[]{src},
			    cont,body));
			    
		    Var trgAge=code.addVar(Exectype.POINTER);
		    Var srcAge=code.addVar(Exectype.POINTER);
			    
		    loadAge(body,trgAge,trg);
		    loadAge(body,srcAge,src);
			    
		    Var pred1=code.addVar(Exectype.INT);
                    Var pred2=code.addVar(Exectype.INT);
			    
		    body.append(
			new SimpleInst(
			    o.di(),OpCode.ULessThan,
			    pred1,new Arg[]{
				srcAge,
				trgAge
			    }));
		    body.append(
			new SimpleInst(
			    o.di(),OpCode.Not,
			    pred2,new Arg[]{pred1}));
		    body.append(
			new TypeInst(
			    o.di(),OpCode.ThrowRTEOnZero,
			    Var.VOID,new Arg[]{pred2},
			    Runtime.illegalAssignmentError.asType()));
		    body.setFooter(
			new Jump(o.di(),cont));
			    
		    clearStoreCheck(o);
			    
		    setChangedCode();
		} else if (hasInHeapCheck(o)) {
		    Arg src=o.rhs(0);
			    
		    Header cont=h.split(o);
		    Header body=h.makeSimilar(o.di());
			    
		    h.setFooter(
			new Branch(
			    o.di(),OpCode.BranchNonZero,
			    new Arg[]{src},
			    cont,body));
			    
		    Var srcAge=code.addVar(Exectype.POINTER);
			    
		    loadAgeRaw(body,srcAge,src);
			    
		    Var pred1=code.addVar(Exectype.INT);
		    Var pred2=code.addVar(Exectype.INT);
			    
		    body.append(
			new SimpleInst(
			    o.di(),OpCode.ULessThan,
			    pred1,new Arg[]{
				srcAge,
				PointerConst.make(
				    1l<<(Global.pointerSize*8-2))
			    }));
		    body.append(
			new SimpleInst(
			    o.di(),OpCode.Not,
			    pred2,new Arg[]{pred1}));
		    if (Global.gcScopedMemory) {
			Var tmp0=code.addVar(Exectype.POINTER);
			Var tmp1=code.addVar(Exectype.POINTER);
			Var tmp2=code.addVar(Exectype.POINTER);
			Header stackcheck=h.makeSimilar(o.di());
			stackcheck.append(
		            new SimpleInst(
				o.di(),OpCode.Shl,
				tmp0,new Arg[]{
				    srcAge,IntConst.make(2)
				}));
			stackcheck.append(
			    new MemoryAccessInst(
				o.di(),OpCode.Load,
				tmp1,new Arg[]{tmp0},
				Type.POINTER));
			stackcheck.append(
			    new SimpleInst(
				o.di(),OpCode.And,
				tmp2,new Arg[]{tmp1,PointerConst.make(1)}));
			stackcheck.append(
			    new TypeInst(
				o.di(),OpCode.ThrowRTEOnZero,
				Var.VOID,new Arg[]{tmp2},
				Runtime.illegalAssignmentError.asType()));
			stackcheck.setFooter(
			    new Jump(o.di(),cont));	 
			body.setFooter(
			    new Branch(
				o.di(),OpCode.BranchZero,
				new Arg[]{pred2},
				cont,stackcheck));
		    } else {
			body.append(
			    new TypeInst(
				o.di(),OpCode.ThrowRTEOnZero,
				Var.VOID,new Arg[]{pred2},
				Runtime.illegalAssignmentError.asType()));
			body.setFooter(new Jump(o.di(),cont));
		    }
			    
                    clearInHeapCheck(o);

		    setChangedCode();
		} else if (o.opcode()==OpCode.ScopeReturnCheck) {
		    assert code.origin().origin().alloc==AllocationMechanism.STACK_ALLOC;
		    
		    Arg src=o.rhs(0);
		    
		    Header cont=h.split(o);
		    Header body=h.makeSimilar(o.di());
		    
		    h.setFooter(
			new Branch(
			    o.di(),OpCode.BranchNonZero,
			    new Arg[]{src},
			    cont,body));
		    
		    Var srcAge=code.addVar(Exectype.POINTER);
		    
		    loadAgeRaw(body,srcAge,src);
		    
		    Var tmp=code.addVar(Exectype.POINTER);
		    Var pred=code.addVar(Exectype.INT);
		    
		    body.append(
			new SimpleInst(
			    o.di(),OpCode.Ushr,
			    tmp,new Arg[]{
				Arg.ALLOC_FRAME,
				IntConst.make(2)
			    }));
		    body.append(
			new SimpleInst(
			    o.di(),OpCode.ULessThan,
			    pred,new Arg[]{
				tmp,
				srcAge
			    }));
		    body.append(
			new TypeInst(
			    o.di(),OpCode.ThrowRTEOnZero,
			    Var.VOID,new Arg[]{pred},
			    Runtime.illegalAssignmentError.asType()));
		    body.setFooter(
			new Jump(o.di(),cont));
		    
		    o.remove();
		    
		    setChangedCode();
		}
	    }
	}
	
	// do a quick sanity check
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		assert i.opcode()!=OpCode.InHeapCheck
		    && i.opcode()!=OpCode.ScopeReturnCheck;
		if (i instanceof HeapAccessInst) {
		    assert !((HeapAccessInst)i).mode().hasScopeCheck();
		}
	    }
	}
	
	if (changedCode()) {
	    code.killAllAnalyses();
	}
    }
}

