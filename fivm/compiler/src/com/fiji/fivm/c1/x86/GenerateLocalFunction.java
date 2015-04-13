/*
 * GenerateLocalFunction.java
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

package com.fiji.fivm.c1.x86;

import java.io.*;
import java.util.*;
import com.fiji.fivm.*;
import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

public class GenerateLocalFunction extends LCodePhase {
    public GenerateLocalFunction(LCode c) { super(c); }
    
    public void visitCode() {}
    
    public LocalAsmFunction localFunction() {
        return new LocalAsmFunction(code.cname()) {
            public LinkableSet subLinkables() {
                LinkableSet result=new LinkableSet();
                for (LHeader h : code.headers()) {
                    for (LOp o : h.operations()) {
                        LinkableSet l=o.linkableSet();
                        if (l!=null) {
                            result.addAll(l);
                        }
                    }
                }
                return result;
            }
            
            public void generateAsm(PrintWriter w) {
                w.println("\t# "+code.origin().shortName());
                w.println("\t.globl FIVMR_SYMBOL("+name+")");
                w.println("FIVMR_SYMBOL("+name+"):");
                if (!Settings.OMIT_FRAME_POINTER) {
                    w.println("\tpush"+LType.ptr().asm()+" %"+Reg.BP.asm(LType.ptr()));
                    w.println("\tmov"+LType.ptr().asm()+" %"+Reg.SP.asm(LType.ptr())+", %"+Reg.BP.asm(LType.ptr()));
                }
                ArrayList< LHeader > heads=code.depthFirstHeaders();
                for (int i=0;i<heads.size();++i) {
                    LHeader h=heads.get(i);
                    
                    w.println("FIVMR_LOCAL_SYMBOL("+h.labelName()+"):\t\t# probableFrequency = "+h.probableFrequency());
                    
                    for (LOp o : h.instructions()) {
                        w.print(o.asm(false));
                    }
                    
                    LFooter f=h.footer();
                    LHeader defaultSuccessor;
                    if (f.opcode().reversibleBinaryBranch() &&
                        heads.size()>i+1 &&
                        heads.get(i+1)==f.conditionalSuccessor()) {
                        w.print(f.asm(true));
                        defaultSuccessor=f.conditionalSuccessor();
                    } else {
                        w.print(f.asm(false));
                        defaultSuccessor=f.defaultSuccessor();
                    }

                    if (defaultSuccessor!=null) {
                        if (heads.size()>i+1 && heads.get(i+1)==defaultSuccessor) {
                            // don't emit jump
                        } else {
                            w.println("\tjmp FIVMR_LOCAL_SYMBOL("+defaultSuccessor.labelName()+")");
                        }
                    }
                }
                if (Settings.ASSERTS_ON) {
                    w.println("\tint $3");
                }
            }
        };
    }
}

