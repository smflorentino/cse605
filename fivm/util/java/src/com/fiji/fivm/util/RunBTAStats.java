/*
 * RunBTAStats.java
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
import com.fiji.fivm.*;
import com.fiji.asm.*;
import com.fiji.asm.commons.*;
import com.fiji.util.*;

public class RunBTAStats {
    private RunBTAStats() {}
    
    public static void main(String[] v) throws Throwable {
        final ComboStats stackHeight=new ComboStats(0,1,1000);
        final ComboStats locals=new ComboStats(0,1,1000);
        final ComboStats stackHeightAfterCall=new ComboStats(0,1,1000);
        final ComboStats trycatch=new ComboStats(0,1,1000);
        final ComboStats maxPC=new ComboStats(0,1,100000);
        final ComboStats maxLineNumber=new ComboStats(0,1,100000);
        
        ClassFileIterator cfi=new ClassFileIterator() {
                public void addClass(String className,
                                     byte[] bytecode) {
                    ClassReader cr=new ClassReader(bytecode);
                    final boolean[] hasJSR=new boolean[1];
                    cr.accept(
                        new EmptyVisitor(){
                            public MethodVisitor visitMethod(int access,
                                                             UTF8Sequence name,
                                                             UTF8Sequence desc,
                                                             UTF8Sequence signature,
                                                             UTF8Sequence[] exceptions) {
                                return new EmptyVisitor() {
                                    int curTC;
                                    int curMaxPC;
                                    int curMaxLN;
                                    public void visitBCOffset(int bcOffset) {
                                        curMaxPC=Math.max(curMaxPC,bcOffset);
                                    }
                                    public void visitLineNumber(int line, Label start) {
                                        curMaxLN=Math.max(line, curMaxLN);
                                    }
                                    public void visitTryCatchBlock(Label start,
                                                                   Label end,
                                                                   Label handler,
                                                                   UTF8Sequence type) {
                                        curTC++;
                                    }
                                    public void visitVarInsn(int opcode,
                                                             int var) {
                                        if (opcode==Opcodes.RET) {
                                            hasJSR[0]=true;
                                        }
                                    }
                                    public void visitJumpInsn(int opcode,
                                                              Label label) {
                                        if (opcode==Opcodes.JSR) {
                                            hasJSR[0]=true;
                                        }
                                    }
                                    public void visitEnd() {
                                        trycatch.add(curTC);
                                        maxPC.add(curMaxPC);
                                        maxLineNumber.add(curMaxLN);
                                    }
                                };
                            }
                        },
                        ClassReader.SKIP_FRAMES);
                    if (hasJSR[0]) {
                        ClassWriter cw=new ClassWriter(0);
                        new ClassReader(bytecode).accept(
                            new ClassAdapter(cw) {
                                public MethodVisitor visitMethod(int access,
                                                                 UTF8Sequence name,
                                                                 UTF8Sequence desc,
                                                                 UTF8Sequence signature,
                                                                 UTF8Sequence[] exceptions) {
                                    return new JSRInlinerAdapter(
                                        super.visitMethod(
                                            access,name,desc,signature,exceptions),
                                        access,
                                        name.toString(),
                                        desc.toString(),
                                        signature==null?null:signature.toString(),
                                        UTF8Sequence.toString(exceptions));
                                }
                            },
                            0);
                        bytecode=cw.toByteArray();
                    }
                    final byte[] pureBytecode=bytecode;
                    new ClassReader(pureBytecode).accept(
                        new EmptyVisitor(){
                            public MethodVisitor visitMethod(int access,
                                                             UTF8Sequence name,
                                                             UTF8Sequence desc,
                                                             UTF8Sequence sig,
                                                             UTF8Sequence[] exceptions) {
                                if ((access&(Opcodes.ACC_ABSTRACT|
                                             Opcodes.ACC_NATIVE))==0) {
                                    final MethodBytecodeExtractor mbe=
                                        new MethodBytecodeExtractor(pureBytecode,name.plus(desc));
                                    BytecodeTypeAnalysis bta=
                                        new BytecodeTypeAnalysis(mbe);
                                
                                    locals.add(mbe.nLocals());
                                    final BytecodeTypeAnalysis.ForwardHeavyCalc fhc=
                                        bta.new ForwardHeavyCalc();
                                    mbe.extract(new MethodAdapter(fhc){
                                            public void visitBCOffset(int bcOffset) {
                                                super.visitBCOffset(bcOffset);
                                                stackHeight.add(fhc.stackHeight());
                                            }
                                            public void visitMethodInsn(int opcode,
                                                                        UTF8Sequence owner,
                                                                        UTF8Sequence name,
                                                                        UTF8Sequence desc) {
                                                super.visitMethodInsn(opcode,owner,name,desc);
                                                stackHeightAfterCall.add(
                                                    fhc.stackHeight()-
                                                    Types.cells(
                                                        (char)TypeParsing.getMethodReturn(
                                                            desc).byteAt(0)));
                                            }
                                        });
                                }
                                return null;
                            }
                        },
                        ClassReader.SKIP_FRAMES|
                        ClassReader.SKIP_DEBUG|
                        ClassReader.SKIP_CODE);
                }
            };
        
        for (String flnm : v) {
	    cfi.addClassOrJar(flnm);
	}

        System.out.println("Stack height = "+stackHeight);
        System.out.println("Locals = "+locals);
        System.out.println("Stack height after call = "+stackHeightAfterCall);
        System.out.println("Try catch = "+trycatch);
        System.out.println("Max PC = "+maxPC);
        System.out.println("Max Line Number = "+maxLineNumber);
        
        for (int i=0;i<=2;++i) {
            System.out.println("Probability of the stack being <="+i+": "+((double)stackHeight.histo().pdfAt(i)/stackHeight.histo().population()));
        }
        for (int i=0;i<=2;++i) {
            System.out.println("Probability of the stack height after call <="+i+": "+((double)stackHeightAfterCall.histo().pdfAt(i)/stackHeightAfterCall.histo().population()));
        }
        
        System.out.println("Probability of the max PC being <="+127+": "+((double)maxPC.histo().pdfAt(127)/maxPC.histo().population()));
        System.out.println("Probability of the max PC being <="+255+": "+((double)maxPC.histo().pdfAt(255)/maxPC.histo().population()));
        System.out.println("Probability of the max Line Number being <="+255+": "+((double)maxLineNumber.histo().pdfAt(255)/maxLineNumber.histo().population()));
        System.out.println("Probability of the max Line Number being <="+1023+": "+((double)maxLineNumber.histo().pdfAt(1023)/maxLineNumber.histo().population()));
        System.out.println("Probability of the max Line Number being <="+2047+": "+((double)maxLineNumber.histo().pdfAt(2047)/maxLineNumber.histo().population()));
        System.out.println("Probability of the max Line Number being <="+4095+": "+((double)maxLineNumber.histo().pdfAt(4095)/maxLineNumber.histo().population()));
    }
}

