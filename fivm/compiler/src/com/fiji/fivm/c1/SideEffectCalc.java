/*
 * SideEffectCalc.java
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

/** Tells you if an operation causes side-effects.  The point is that if
    an operation does not cause side-effects, then it is dead if the
    LHS is dead. */
public class SideEffectCalc {
    Code c;
    SideEffectCalc(Code c) { this.c=c; }
    
    public boolean get(Operation o) {
        return get(o,CallSideEffectMode.CALLS_TO_PURE_FUNCTIONS_ARE_PURE);
    }
    
    public boolean get(Operation o,
                       CallSideEffectMode mode) {
        if (!c.checksInserted && MayHaveChecksCalc.get(o)) {
            return true;
        }
        
        if (o instanceof HeapAccessInst &&
            ((HeapAccessInst)o).mode().hasChecks()) {
            return true;
        }

	// NOTE: what if this is an allocation?  maybe that is not really a side effect?  current answer is: it's not a side effect.
        // NB: except for NewArray!  that's a side-effect because it may throw NASE,
        // so if it's going to then we cannot kill it.
        // FIXME: factor out that check into a separate operation ...
	switch (o.opcode()) {
        case SaveDebugID: // it's a side-effect in the sense that it cannot be removed but for RCE, it kind of isn't, since it doesn't clobber anything.  blah!!
        case SaveRef: // ^^^ same as SaveDebugID ^^^
	case PutField:
	case PutStatic:
	case PollCheck:
	case ArrayStore:
	case Store:
	case PutCField:
	case WeakCASField:
	case WeakCASStatic:
	case WeakCASElement:
	case PutCVar:
	case Return:
	case RawReturn:
	case NotReached:
	case PatchPoint:
	case PatchPointFooter:
	case TypeCheck:
	case MonitorEnter:
	case MonitorExit:
	case New: // it's a side-effect in the sense that it modifies some state, I guess...  for RCE it's def a side-effect because it cannot be RCE'd.  blech.
	case NewArray: // due to NASE
	case MultiNewArray: // due to NASE
	case NullCheck:
	case ArrayBoundsCheck:
	case ArrayCheckStore:
	case Throw:
	case Rethrow:
	case ThrowRTEOnZero:
	case CheckException:
	case ClearException:
	case Fence:
	case CompilerFence:
        case HardCompilerFence:
        case PollcheckFence:
	case CheckDivisor:
	case StrongLoadCAS:
	case StrongCAS:
	case StrongVoidCAS:
	case CheckInit:
	case WeakCAS:
	case HardUse:
	case InHeapCheck:
        case GetMethodRec:
	case ScopeReturnCheck:
        case Memcpy:
            return true;
            
        case Load:
        case GetField:
        case GetStatic:
        case ArrayLoad:
            return ((Volatilable)o).volatility().isVolatile();

	case InvokeDynamic:
	case InvokeStatic:
	case Invoke:
        case InvokeResolved:
        case InvokeIndirect:
	    return mode==CallSideEffectMode.ALL_CALLS_ARE_SIDE_EFFECTS
                || ((MTSInstable)o).signature().sideEffect()!=SideEffectMode.PURE;

	case Call:
	    return mode==CallSideEffectMode.ALL_CALLS_ARE_SIDE_EFFECTS
                || ((Function)((CFieldInst)o).field()).sideEffect()!=SideEffectMode.PURE;
	    
	case CallIndirect:
	    return mode==CallSideEffectMode.ALL_CALLS_ARE_SIDE_EFFECTS
                || ((CallIndirectInst)o).signature().sideEffect()!=SideEffectMode.PURE;
	    
	default:
            return false;
	}
    }
    
    public boolean immutable(Operation o) {
        if (get(o,CallSideEffectMode.ALL_CALLS_ARE_SIDE_EFFECTS) ||
            !(o instanceof Instruction)) {
            return false;
        } else {
            switch (o.opcode()) {
            case GetField:
            case GetStatic:
            case GetCField:
            case GetCVar:
            case Load:
            case ArrayLoad:
            case GetException:
            case Ipsilon: // not really sure why this is here. ;-)
            case GetAllocSpace: // this operation is *really* bizarre
            case GetDebugID:
                return false;
            case Cast: {
                TypeInst ti=(TypeInst)o;
                return ti.rhs(0).type().isObject()==ti.getType().isObject();
            }
            default: return !c.getThrows().get(o);
            }
        }
    }
}

