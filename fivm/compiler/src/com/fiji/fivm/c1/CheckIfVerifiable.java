/*
 * CheckIfVerifiable.java
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

public class CheckIfVerifiable extends CodePhase {
    public CheckIfVerifiable(Code c) { super(c); }
    
    public void visitCode() {
        if (!code.simpleCallingConvention()) {
            code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                     "complex calling convention");
        }
        
        if (code.method()==null) {
            code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                     "synthetic method");
        } else {
            if (code.method().noNullCheckOnAccess() && code.method().isInstance()) {
                code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                         "receiver may be null due to @NoNullCheckOnAccess");
            }
            
            if (code.method().noSafetyChecks()) {
                code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                         "method has no safety checks due to @NoSafetyChecks");
            }
            
            if (code.method().noScopeChecks) {
                code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                         "method has no scope checks due to @NoScopeChecks");
            }
        }
        
        for (Header h : code.headers()) {
            for (Operation o : h.operations()) {
                if (o.uses(Arg.FRAME)) {
                    code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                             "uses Arg.FRAME");
                }
                switch (o.opcode()) {
                case OffsetOfField:
                case OffsetOfElement:
                case PollCheck:
                case AddressOfField:
                case AddressOfStatic:
                case AddressOfElement:
                case Call:
                case CallIndirect:
                case Load:
                case Store:
                case WeakCAS:
                case StrongLoadCAS:
                case StrongCAS:
                case StrongVoidCAS:
                case PutCField:
                case GetCField:
                case GetCFieldAddress:
                case GetCFieldOffset:
                case CastNonZero:
                case CastExact:
                case InvokeResolved:
                case InvokeIndirect:
                case GetCTypeSize:
                case PutCVar:
                case GetCVar:
                case GetCVarAddress:
                case GetTypeData:
                case GetMethodRec:
                case GetTypeDataForObject:
                case RawReturn:
                    code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                             "uses "+o.opcode());
                default:
                    break;
                }
            }
        }
        
        for (Var v : code.vars()) {
            if (v.type()==Exectype.POINTER) {
                code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                         "uses pointers");
            }
        }
    }
}

