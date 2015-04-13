/*
 * SCJWrapperGenerator.java
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

import com.fiji.asm.ClassWriter;
import com.fiji.asm.MethodVisitor;
import com.fiji.asm.Opcodes;
import com.fiji.asm.UTF8Sequence;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SCJWrapperGenerator {
    public static void main(String[] args)
	throws FileNotFoundException, IOException {
	
	//System.out.println("HELLO: This is SCJ Wrapper generator!");

	if (args.length != 3) {
	    System.err.println("SCJWrapper generator requires three arguments: filename classname backingstore_size");
            System.exit(1);
	}
	ClassWriter cw = new ClassWriter(0);
	cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, new UTF8Sequence("com/fiji/fivm/SCJMain"),
 		 null, new UTF8Sequence("java/lang/Object"), null);
 
 	MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                                          new UTF8Sequence("<init>"),
 					  new UTF8Sequence("()V"),
                                          null, null);
 	mv.visitVarInsn(Opcodes.ALOAD, 0);
 	mv.visitInsn(Opcodes.RETURN);
 	mv.visitMaxs(1, 1);
 	mv.visitEnd();
 
 	mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                            new UTF8Sequence("run"),
                            new UTF8Sequence("()V"),
                            null, null);
        mv.visitLdcInsn(Long.valueOf(args[2]));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           new UTF8Sequence("com/fiji/fivm/r1/MemoryAreas"),
                           new UTF8Sequence("allocScopeBacking"),
                           new UTF8Sequence("(J)V"));
	mv.visitTypeInsn(Opcodes.NEW, new UTF8Sequence(args[1]));
	mv.visitInsn(Opcodes.DUP);
	mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
			   new UTF8Sequence(args[1]),
			   new UTF8Sequence("<init>"), new UTF8Sequence("()V"));
	mv.visitInsn(Opcodes.DUP);
	mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
			   new UTF8Sequence("javax/safetycritical/Safelet"),
			   new UTF8Sequence("getSequencer"),
			   new UTF8Sequence("()Ljavax/safetycritical/MissionSequencer;"));
	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
			   new UTF8Sequence("javax/safetycritical/MissionSequencer"),
			   new UTF8Sequence("start"), new UTF8Sequence("()V"));
 	mv.visitInsn(Opcodes.RETURN);
 	mv.visitMaxs(2, 1);
 	mv.visitEnd();
 



 	mv = cw.visitMethod(Opcodes.ACC_PUBLIC
                            + Opcodes.ACC_STATIC,
                            new UTF8Sequence("main"),
                            new UTF8Sequence("([Ljava/lang/String;)V"),
                            null, null);
 	mv.visitLdcInsn(Long.valueOf(args[2]));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           new UTF8Sequence("com/fiji/fivm/r1/MemoryAreas"),
                           new UTF8Sequence("allocScopeBacking"),
                           new UTF8Sequence("(J)V"));
	mv.visitTypeInsn(Opcodes.NEW, new UTF8Sequence(args[1]));
	mv.visitInsn(Opcodes.DUP);
	mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
			   new UTF8Sequence(args[1]),
			   new UTF8Sequence("<init>"), new UTF8Sequence("()V"));
	mv.visitInsn(Opcodes.DUP);
	mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
			   new UTF8Sequence("javax/safetycritical/Safelet"),
			   new UTF8Sequence("setUp"), new UTF8Sequence("()V"));
	mv.visitInsn(Opcodes.DUP);
	mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
			   new UTF8Sequence("javax/safetycritical/Safelet"),
			   new UTF8Sequence("getSequencer"),
			   new UTF8Sequence("()Ljavax/safetycritical/MissionSequencer;"));
	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
			   new UTF8Sequence("javax/safetycritical/MissionSequencer"),
			   new UTF8Sequence("start"), new UTF8Sequence("()V"));
	mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
			   new UTF8Sequence("javax/safetycritical/Safelet"),
			   new UTF8Sequence("tearDown"), new UTF8Sequence("()V"));
	mv.visitInsn(Opcodes.RETURN);
	mv.visitMaxs(2, 1);
	mv.visitEnd();
	cw.visitEnd();

	FileOutputStream fos = new FileOutputStream(args[0]);
	fos.write(cw.toByteArray());
	fos.close();
    }
}

