/*
 * RunBTA.java
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

package com.fiji.fivm.util;

import com.fiji.fivm.codegen.*;
import com.fiji.fivm.c1.*;
import com.fiji.asm.*;

public class RunBTA {
    private RunBTA() {}
    
    public static void main(String[] v) throws Throwable {
        System.out.println("Preparing to load bytecode...");
        
        MethodBytecodeExtractor mbe=
            new MethodBytecodeExtractor(Util.readCompletely(v[0]),new UTF8Sequence(v[1]));
        
        System.out.println("Running analysis...");
        
        BytecodeTypeAnalysis bta=null;

        // warmup
        for (int n=Integer.parseInt(v[2])/3;n-->0;) {
            bta=new BytecodeTypeAnalysis(mbe,Integer.parseInt(v[3]));
        }
        
        long before=System.currentTimeMillis();
        
        for (int n=Integer.parseInt(v[2]);n-->0;) {
            bta=new BytecodeTypeAnalysis(mbe,Integer.parseInt(v[3]));
        }
        
        long after=System.currentTimeMillis();
        
        System.out.println("Analysis ran for "+Integer.parseInt(v[2])+" iterations in "+(after-before)+" ms");
        
        System.out.println("Analysis ran successfully; dumping summary results...");
        
        for (int bcOffset : bta.knownPoints()) {
            System.out.println(""+bcOffset+":");
            System.out.println("   locals: "+BytecodeTypeAnalysis.typesToString(bta.localsAt(bcOffset)));
            System.out.println("    stack: "+BytecodeTypeAnalysis.typesToString(bta.stackAt(bcOffset)));
        }
        
        System.out.println("Dumping detailed results...");
        
        final BytecodeTypeAnalysis.ForwardHeavyCalc fhc=bta.new ForwardHeavyCalc();
        mbe.extract(new MethodAdapter(fhc){
                public void visitBCOffset(int bcOffset) {
                    super.visitBCOffset(bcOffset);
                    System.out.println(""+bcOffset+": "+
                                       BytecodeTypeAnalysis.typesToString(fhc.locals())+" "+
                                       BytecodeTypeAnalysis.typesToString(fhc.stack()));
                }
            });
        
        System.out.println("That seemed to work.  Time to analyze = "+((double)(after-before)/Integer.parseInt(v[2]))+" ms");
    }
}

