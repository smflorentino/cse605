/*
 * NormalizePatchPoints.java
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

public class NormalizePatchPoints extends CodePhase {
    public NormalizePatchPoints(Code c) { super(c); }
    
    public void visitCode() {
        for (Header h : code.headers3()) {
            if (h.getFooter().opcode()==OpCode.PatchPointFooter) {
                Header h2;
                
                if (Settings.CLOSED_PATCH_POINTS) {
                    // under closed patch points, the patch point will just
                    // be an exception throw.  so we want it to be *inside*
                    // the exception handler chain that the patch point
                    // originally appeared in.
                    h2=h.makeSimilar(h.getFooter().di());
                } else {
                    assert Settings.OPEN_PATCH_POINTS;
                    // under open patch points, the patch point will always
                    // jump to generated code.  therefore, if it throws an
                    // exception, that exception should be handled by the
                    // code that the patch point generates dynamically.
                    h2=code.addHeader(h.getFooter().di());
                }
                
                Var result;
                
                if (code.result()==Type.VOID) {
                    result=Var.VOID;
                } else {
                    result=code.addVar(code.result().asExectype());
                }
                
                h2.append(
                    ((PatchPointFooter)h.getFooter()).makePatchPoint(result));
                
                if (code.result()==Type.VOID) {
                    h2.setFooter(
                        new Terminal(
                            h2.di(),OpCode.Return,Arg.EMPTY));
                } else {
                    h2.setFooter(
                        new Terminal(
                            h2.di(),OpCode.Return,new Arg[]{result}));
                }
                
                h.setFooter(
                    new Jump(
                        h2.di(),h2));
                
                setChangedCode("normalized patch point");
            }
        }
        
        if (changedCode()) code.killAllAnalyses();
    }
}

