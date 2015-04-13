/*
 * OpCode.java
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

public enum OpCode {
    GetArg,
    GetCArg,
    GetCArgAddress,
    GetException,
    ClearException,
    GetDebugID, // takes no args, returns int.  what about live refs?
    Mov,
    Cast,   /* this can do integer casts, unchecked reference casts, and
    	       all manner of "fiats" between objects, pointers, and numbers.
	       other operators can only do "casts" in the sense that we
	       don't enforce class inheritance relationships between the
	       variables in the RHS and the parameter type of the operation */
    CastExact, /* this is a bizarre cast that forces analyses to assume that the
                  resulting object is *exactly* of the given type. */
    CastNonZero, /* even more bizarre cast that forces analyses to assume that
                    the resulting value is non-zero. */
    IntToPointerZeroFill,
    Fiat, /* bit reinterpretation */
        
    // only valid on 32-bit systems; gives you a POINTER for the lower and upper
    // parts of a 64-bit value
    FirstHalf,
    SecondHalf,
        
    Add,
    Sub,
    Mul,
    Div,
    Mod,
    CheckDivisor,
    Neg,
    Shl,
    Shr,
    Ushr,
    And,
    Or,
    Xor,
    Sqrt,
    CompareG, /* returns an int that is either -1, 0, or 1 */
    CompareL, /* returns an int that is either -1, 0, or 1 */
    LessThan,
    LessThanEq,
    ULessThan,
    ULessThanEq,
    Eq,
    Neq,

    Not, // NOTE: Eq(stuff, 0) should become Not
    Boolify, // NOTE: Neq(stuff, 0) should become Boolify
    BitNot,
    Instanceof,
    MonitorEnter,
    MonitorExit,
    InvokeStatic,
    Invoke,	/* "special" invocations are fully resolved to the exact method,
    		   so no further resolution is required. */
    ArrayLength,
    GetStatic,
    PutStatic,
    GetField,
    PutField,
    GetString,
    GetType,
    New,
    NewArray,
    MultiNewArray,	/* should have a pass that kills this (done) */
    NullCheck, /* should have a token that links this to the use; that'll allow
		  us to remove this from the SideEfect list and do better code
		  motion */
    PollCheck,
    ArrayBoundsCheck,
    ArrayCheckStore,
    ArrayLoad,
    ArrayStore,
	
    // check if the class is inited.  note that we can optimize a lot of these
    // away, but we must be careful.  if a CheckInit fails, subsequent CheckInits
    // will also fail.  so at the head of exception handlers, we need to assume
    // that classes that went through a CheckInit are not initialized.
    // ALSO: it is incorrect to insert these in places where they are obviously
    // not needed.  A CheckInit is "obviously not needed" if any of these is true:
    // 1) class.shouldCallCheckInit() returns false
    // 2) the code is associated with a method, and the method's class is a
    //    (non-strict) subclass of the class we're CheckIniting.
    // failing to follow this rule may break the runtime.  note the last one
    // does not refer to the owner of the code - this is encessary to make
    // @Export work correctly.
    CheckInit,

    // scope checks
    ScopeReturnCheck,
    InHeapCheck, // for static field accesses this will be part of the HeapAccessInst
	
    // calls virtual or interface methods.  it's an interface call if
    // the method associated with this op is an interface method; it's
    // a virtual call if the method is a class method.
    InvokeDynamic,

    // FIXME: add two new operations: InvokeResolved and InvokeIndirect.
    // InvokeResolved will be a CFieldInst, and take a Function.
    // InvokeDynamic will take an extra arg that is a function pointer.
    // Both will perform calling convention conversion, but no resolution.
    // This will make it easier to have multiple calling conventions, simplify
    // the calling convention as it stands now, and enable some magic for
    // making Class.newInstance faster.  ... and it could be used to make
    // all reflection faster, if we can guess the signature of the method.
    // essentially, if we can make a guess about what the types might be, then
    // we can turn the call into a fast path that asserts those types, and a
    // slow path that assumes the worst.
    InvokeResolved,
    InvokeIndirect,
	
    // check if the given object is a subtype of some type, or is null,
    // and throw the given exception if it isn't.
    TypeCheck,

    // FIXME: or, better yet, add an InstanceofNullable instruction.

    // field operations that we add to Java
    AddressOfStatic,
    AddressOfField,
    OffsetOfField, /* NOTE: some object models may reject this.  the
		      object model lowering may reject it with a static
		      error, since uses should be guarded with a branch
		      on GC type, which will get constant folded and dead
		      code eliminated. */
    AddressOfElement,
    OffsetOfElement, /* NOTE: some object models may reject this.  see
			above. */
    WeakCASStatic,
    WeakCASField,
    WeakCASElement,  // = ArrayWeakCAS?  naming foo-bar...

    // additional operations that Java doesn't support, but we do
    
    PoundDefined, // corresponds to #if defined(...)
	
    // FIXME: need HeapStore, HeapLoad, HeapWeakCAS (pointer store/load/cas that describes
    // which object is being targetted; the object ref is unused for tranformation except
    // to preserve the object ref's liveness)

    Store,
    Load,
    // FIXME: need VolatileStore, VolatileLoad
    StrongLoadCAS,
    StrongCAS,
    StrongVoidCAS,
    WeakCAS,
    Fence,
    PutCField,
    GetCField,
    GetCFieldAddress,
    GetCFieldOffset,
	
    PollcheckFence, // fence preserved only until pollcheck insertion
    CompilerFence, // fence preserved only until C generation
    HardCompilerFence, // fence preserved through C generation
    
    // This used to be an Arg, but it cannot be, since it is tied to a CodeOrigin.
    GetAllocSpace,
	
    GetCTypeSize,

    // these can be used for globals or locals
    PutCVar,
    GetCVar,
    GetCVarAddress,  /* also useful for getting function addresses */
        
    // low-level backend-specific float operations
    FXor,
    Float0,
    Double0,
        
    // memcpy ... what else
    Memcpy,
    
    // these additional operator work over C functions.  note that in the
    // generated C code, for Call we assume that the function is fully
    // declared (along with the types it takes and the types it returns),
    // whereas for CallIndirect we conjure a declaration by inferring how
    // you use it.
    Call,
    CallIndirect,
	
    // this is a "block internal" throw, which throws a runtime exception only
    // if the given value is zero.
    ThrowRTEOnZero,
	
    // checks if an exception is live, and if it is, jumps to the handler
    CheckException,
    
    // gets a vtable
    GetTypeData,
        
    // gets a method record
    GetMethodRec,
        
    // gets the vtable of the object
    GetTypeDataForObject,
        
    // high-level operations over FRAME - FIXME make these work.
    SaveDebugID,
    SaveRef,
	
    // a phatom use and def.  this is a nop, but it can have an arbitrary
    // lhs and rhs.
    Phantom,

    // a phatom "check" which is marked as throwing.
    PhantomCheck,
	
    LikelyZero,
    LikelyNonZero,
    SemanticallyLikelyZero,
    SemanticallyLikelyNonZero,
	
    // like Phantom, except that we never kill it, and it's a side-effect.
    HardUse,
        
    // The predecessor component of a Phi function.  If a block has a Phi
    // at the beginning, all of its predecessors should have an Ipsilon at
    // the end where the LHS of the Ipsilon corresponds to the LHS/RHS of
    // the Phi.  The RHS of the Ipsilon should be the actual source of the
    // value for the corresponding Phi function.
    // NOTE: the reason why this is called Ipsilon is because Ipsilon comes
    // before Phi in the alphabet, just like this operation - it comes before
    // Phi.
    Ipsilon,
	
    // a Phi function with a twist.  the LHS and RHS must be the same variable.
    // this is a nop, but it indicates that the variables value depends on
    // the incoming cfg arc.
    Phi,
	
    // terminal codes    
    Return,
    RawReturn, // requires that the C types match rather than the Java types
    Throw,
    Rethrow, /* indicates that we know that there is an exception pending,
		and we'd just like to rethrow it. */
    NotReached,

    // control codes
    Jump,
    BranchZero,
    BranchNonZero,
        
    Switch,
    AwesomeJump, // jump through a pointer, but advise the system where the target might be
	
    // patch point.  the idea is that a PatchPoint represents the calling of
    // the method in an inliner-friendly way.  the idea is that prior to
    // inlining, we can always safely replace any continuation with:
    //
    // X = PatchPoint(stuff)
    // Return(X)
    //
    // If this gets inlined, the Return(X) gets replaced with copy propagation
    // of X but the PatchPoint remains.  I.e. the caller of the inlined method
    // will end up calling out to a PatchPoint that returns the method's
    // result, if the need for patching ever arises.  If a method with a
    // PatchPoint is not inlined then this works naturally as well.  Either
    // way it just magically works.
    PatchPoint,

    PatchPointFooter;
    
    public final static OpCode[] table;
    
    static {
        table=OpCode.class.getEnumConstants();
        assert table!=null;
        for (int i=0;i<table.length;++i) {
            assert table[i].ordinal()==i;
        }
    }
}

