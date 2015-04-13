/*
 * BasicCostFunction.java
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

/**
 * A really simple cost function for Fiji IR operations (which includes instructions
 * and block footers like control operations) based on the "Plus-Equivalent" (PE) metric.
 * An instruction has X PE if it takes X times longer than doing an addition.
 */
public class BasicCostFunction extends CostFunction {
    /**
     * Compute the cost of an Operation as a multiple of the "Plus-Equivalent" (PE) unit.
     * An operation will have X PE if it takes X times longer than doing an addition.
     * <p>
     * Note that the implementation is currently rather rough and should probably be
     * improved.
     */
    public double cost(Operation o) {
	switch (o.opcode()) {
	case GetDebugID:
	case Phantom:
	case CompilerFence:
        case HardCompilerFence:
        case PollcheckFence:
        case PollCheck:
	case GetCTypeSize:
        case GetCVarAddress:
	case GetCFieldOffset: return 0.0;
        case Mov:
        case Ipsilon:
        case Phi:
        case PutCVar:
        case GetCVar:
        case CastNonZero: return 0.1;
        case Cast:
        case CastExact: {
            Basetype src=o.rhs(0).type().effectiveBasetype();
            Basetype trg=((TypeInst)o).getType().effectiveBasetype();
            if (src==trg) {
                return 0.1;
            } else if (src.isFloat!=trg.isFloat) {
                return 2.0;
            } else if (src.isFloat) {
                return 1.5;
            } else if (src.bytes!=trg.bytes) {
                return 1.0;
            } else {
                return 0.1; /* it's an int-to-ptr cast on 32-bit or a
                               long-to-ptr cast on 64-bit */
            }
        }
	case Instanceof:
	case TypeCheck: return 2.0;
	case MonitorEnter:
	case MonitorExit: return 5.0;
	case Invoke:
	case InvokeStatic:
	case InvokeDynamic:
        case InvokeResolved:
        case InvokeIndirect:
	case Call:
	case CallIndirect: return 3.0;
	default: return 1.0;
	}
    }
}

