/*
 * FindSafelet.java
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;

import com.fiji.asm.ClassReader;
import com.fiji.asm.UTF8Sequence;
import com.fiji.asm.commons.EmptyVisitor;

import com.fiji.fivm.c1.ClassFileIterator;
import com.fiji.fivm.c1.Global;

public class FindSafelet {
    private static class SafeletCheck extends EmptyVisitor {
	private final String name;
	private Boolean found;

	public SafeletCheck(String name) {
	    this.name = name;
	    found = new Boolean(false);
	}

	public SafeletCheck(String name, Boolean found) {
	    this.name = name;
	    this.found = found;
	}

	public void visit(int version, int access, UTF8Sequence name,
			  UTF8Sequence signature, UTF8Sequence superName,
			  UTF8Sequence[] interfaces) {
	    for (UTF8Sequence s : interfaces) {
		if (s.toString().equals("javax/safetycritical/Safelet")) {
		    System.out.println(this.name);
		    found = true;
		    return;
		}
	    }
            /*
              FIXME: this is broken!
	    for (UTF8Sequence s : interfaces) {
		try {
		    new ClassReader(s.toString()).accept(
			new SafeletCheck(this.name, this.found),
			ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG|ClassReader.SKIP_CODE);
		} catch (IOException e) {
		}
		if (found) {
		    return;
		}
	    }
	    if (superName == null) {
		return;
	    }
	    try {
		new ClassReader(superName.toString()).accept(
		    new SafeletCheck(this.name, this.found),
		    ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG|ClassReader.SKIP_CODE);
	    } catch (IOException e) {
	    }
            */
	    if (found) {
		return;
	    }
	}
    }

    public static void main(String[] v) throws IOException {
	Global.verbosity=0;
	ClassFileIterator cfi=new ClassFileIterator(){
		public void addClass(final String className,byte[] bytecode) {
		    new ClassReader(bytecode).accept(
			new SafeletCheck(className),
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
