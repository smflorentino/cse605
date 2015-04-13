/*
 * FindMain.java
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
import com.fiji.asm.Opcodes;
import com.fiji.asm.UTF8Sequence;
import com.fiji.asm.commons.EmptyVisitor;

import com.fiji.fivm.c1.ClassFileIterator;
import com.fiji.fivm.c1.Global;

public class FindMain {
    public static void main(String[] v) throws IOException {
	Global.verbosity=0;
	ClassFileIterator cfi=new ClassFileIterator(){
		public void addClass(final String className,byte[] bytecode) {
		    new ClassReader(bytecode).accept(
			new EmptyVisitor(){
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
	BufferedReader br=
	    new BufferedReader(new InputStreamReader(new FileInputStream(v[0])));
	for (;;) {
	    String flnm=br.readLine();
	    if (flnm==null) {
		break;
	    }
	    if (flnm.equals("")) {
		continue;
	    }
	    cfi.addClassOrJar(flnm);
	}
    }
}

