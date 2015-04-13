/*
 * LeafMethodAnalysis.java
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

public final class LeafMethodAnalysis {
    OutgoingCallMode fastPathOutgoingCallMode;
    OutgoingCallMode outgoingCallMode;
    FrameUseMode frameUseMode;
    
    public LeafMethodAnalysis(Code c) {
        fastPathOutgoingCallMode=OutgoingCallMode.NO_OUTGOING_CALLS;
        outgoingCallMode=OutgoingCallMode.NO_OUTGOING_CALLS;
        frameUseMode=FrameUseMode.FRAME_NOT_USED;
        
        for (Header h : c.likelyHeaders()) {
            for (Operation o : h.operations()) {
                switch (o.opcode()) {
                case InvokeDynamic:
                case InvokeStatic:
                case Invoke:
                case InvokeResolved:
                case InvokeIndirect:
                    fastPathOutgoingCallMode=
                        fastPathOutgoingCallMode.lub(
                            OutgoingCallMode.ARBITRARY_OUTGOING_CALLS);
                    break;
                case Call:
                case CallIndirect:
                    if (c.getSafepoints().get(o)) {
                        fastPathOutgoingCallMode=
                            fastPathOutgoingCallMode.lub(
                                OutgoingCallMode.ARBITRARY_OUTGOING_CALLS);
                    } else {
                        fastPathOutgoingCallMode=
                            fastPathOutgoingCallMode.lub(
                                OutgoingCallMode.OUTGOING_NO_SAFEPOINT_C_CALLS);
                    }
                    break;
                default:
                    break;
                }
            }
        }
        
        for (Header h : c.headers()) {
            for (Operation o : h.operations()) {
                // figure out use of frame
                for (int i=0;i<o.rhs().length;++i) {
                    if (o.rhs(i)==Arg.FRAME) {
                        if (o.opcode()==OpCode.PutCField) {
                            frameUseMode=frameUseMode.lub(FrameUseMode.FRAME_PUTCFIELD_ONLY);
                        } else {
                            frameUseMode=frameUseMode.lub(FrameUseMode.FRAME_USED_ARBITRARILY);
                        }
                    }
                }
                
                // figure out if there are calls
                switch (o.opcode()) {
                case InvokeDynamic:
                case InvokeStatic:
                case Invoke:
                case InvokeResolved:
                case InvokeIndirect:
                    outgoingCallMode=
                        outgoingCallMode.lub(
                            OutgoingCallMode.ARBITRARY_OUTGOING_CALLS);
                    break;
                case Call:
                case CallIndirect:
                    if (c.getSafepoints().get(o)) {
                        outgoingCallMode=
                            outgoingCallMode.lub(
                                OutgoingCallMode.ARBITRARY_OUTGOING_CALLS);
                    } else {
                        outgoingCallMode=
                            outgoingCallMode.lub(
                                OutgoingCallMode.OUTGOING_NO_SAFEPOINT_C_CALLS);
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }
    
    public OutgoingCallMode fastPathOutgoingCallMode() {
        return fastPathOutgoingCallMode;
    }
    
    public OutgoingCallMode outgoingCallMode() {
        return outgoingCallMode;
    }
    
    public FrameUseMode frameUseMode() {
        return frameUseMode;
    }
}

