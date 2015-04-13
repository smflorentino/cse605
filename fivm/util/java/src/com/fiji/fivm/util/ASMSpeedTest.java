/*
 * ASMSpeedTest.java
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

import java.io.*;

import com.fiji.asm.ClassReader;
import com.fiji.asm.MethodVisitor;
import com.fiji.asm.AnnotationVisitor;
import com.fiji.asm.FieldVisitor;
import com.fiji.asm.Opcodes;
import com.fiji.asm.Label;
import com.fiji.asm.UTF8Sequence;
import com.fiji.asm.commons.EmptyVisitor;

import com.fiji.fivm.c1.ClassFileIterator;
import com.fiji.fivm.c1.Global;

import java.util.*;

public class ASMSpeedTest {
    public static void main(String[] v) throws IOException {
	Global.verbosity=0;
        
        final HashMap< String, byte[] > codes=new HashMap< String, byte[] >();
        final boolean[] foundSource=new boolean[1];
        
	ClassFileIterator cfi=new ClassFileIterator(){
		public void addClass(final String className,byte[] bytecode) {
                    codes.put(className,bytecode);
		    new ClassReader(bytecode).accept(
			new EmptyVisitor(){
                            public void visitSource(UTF8Sequence source,
                                                    UTF8Sequence debug) {
                                foundSource[0]=true;
                            }
			    public MethodVisitor visitMethod(int access,
							     UTF8Sequence name,
							     UTF8Sequence desc,
							     UTF8Sequence disgnature,
							     UTF8Sequence[] exceptions) {
				if ((access&Opcodes.ACC_STATIC)!=0 &&
				    name.toString().equals("main") &&
				    desc.toString().equals("([Ljava/lang/String;)V")) {
				    System.out.println(className);
				}
				return null;
			    }
			},
			ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG|ClassReader.SKIP_CODE);
		}
	    };
        
        long before,after;
        
        before=System.currentTimeMillis();
        for (String flnm : v) {
	    cfi.addClassOrJar(flnm);
	}
        after=System.currentTimeMillis();
        System.out.println("Found source data.");
        System.out.println("Initial parse took "+(after-before)+" ms");
        
        before=System.currentTimeMillis();
        final int[] stringLength=new int[1];
        for (byte[] bytecode : codes.values()) {
            new ClassReader(bytecode).accept(
                new EmptyVisitor() {
                    public void visit(int version,
                                      int access,
                                      UTF8Sequence name,
                                      UTF8Sequence signature,
                                      UTF8Sequence superName,
                                      UTF8Sequence[] interfaces) {
                        stringLength[0]+=name.length();
                        if (signature!=null) {
                            stringLength[0]+=signature.length();
                        }
                        if (superName!=null) {
                            stringLength[0]+=superName.length();
                        }
                        for (UTF8Sequence interfase : interfaces) {
                            stringLength[0]+=interfase.length();
                        }
                    }
                    public void visitSource(UTF8Sequence source,
                                            UTF8Sequence debug) {
                        if (source!=null) {
                            stringLength[0]+=source.length();
                        }
                    }
                    public AnnotationVisitor visitAnnotation(UTF8Sequence desc,
                                                             boolean visible) {
                        if (desc!=null) {
                            stringLength[0]+=desc.length();
                        }
                        return null;
                    }
                    public FieldVisitor visitField(int access,
                                                   UTF8Sequence name,
                                                   UTF8Sequence desc,
                                                   UTF8Sequence signature,
                                                   Object value) {
                        stringLength[0]+=name.length();
                        stringLength[0]+=desc.length();
                        if (signature!=null) {
                            stringLength[0]+=signature.length();
                        }
                        return null;
                    }
                    public MethodVisitor visitMethod(int access,
                                                     UTF8Sequence name,
                                                     UTF8Sequence desc,
                                                     UTF8Sequence signature,
                                                     UTF8Sequence[] exceptions) {
                        stringLength[0]+=name.length();
                        stringLength[0]+=desc.length();
                        if (signature!=null) {
                            stringLength[0]+=signature.length();
                        }
                        return new EmptyVisitor() {
                                public void visitFieldInsn(int opcode,
                                                           UTF8Sequence owner,
                                                           UTF8Sequence name,
                                                           UTF8Sequence desc) {
                                    stringLength[0]+=owner.length();
                                    stringLength[0]+=name.length();
                                    stringLength[0]+=desc.length();
                                }
                                public void visitIincInsn(int var,int increment) {
                                }
                                public void visitInsn(int opcode) {
                                }
                                public void visitIntInsn(int opcode,int operand) {
                                }
                                public void visitJumpInsn(int opcode,Label label) {
                                }
                                public void visitLdcInsn(Object cst) {
                                    if (cst instanceof String) {
                                        stringLength[0]+=((String)cst).length();
                                    } else if (cst instanceof com.fiji.asm.Type) {
                                        stringLength[0]+=
                                            ((com.fiji.asm.Type)cst).getDescriptor().length();
                                    }
                                }
                                public void visitLookupSwitchInsn(Label dflt,
                                                                  int[] keys,
                                                                  Label[] labels) {
                                }
                                public void visitMaxs(int maxStack,int maxLocals) {
                                }
                                public void visitMethodInsn(int opcode,
                                                            UTF8Sequence owner,
                                                            UTF8Sequence name,
                                                            UTF8Sequence desc) {
                                    stringLength[0]+=owner.length();
                                    stringLength[0]+=name.length();
                                    stringLength[0]+=desc.length();
                                }
                                public void visitMultiANewArrayInsn(UTF8Sequence desc,int dims) {
                                    stringLength[0]+=desc.length();
                                }
                                public void visitTableSwitchInsn(int min,
                                                                 int max,
                                                                 Label dflt,
                                                                 Label[] labels) {
                                }
                                public void visitTryCatchBlock(Label start,
                                                               Label end,
                                                               Label handler,
                                                               UTF8Sequence type) {
                                    if (type!=null) {
                                        stringLength[0]+=type.length();
                                    }
                                }
                                public void visitTypeInsn(int opcode,
                                                          UTF8Sequence type) {
                                    stringLength[0]+=type.length();
                                }
                                public void visitVarInsn(int opcode,
                                                         int var) {
                                }
                            };
                    }
                },
                ClassReader.SKIP_FRAMES);
        }
        after=System.currentTimeMillis();
        System.out.println("Full parse took "+(after-before)+" ms and found "+stringLength[0]+" characters");
    }
}

