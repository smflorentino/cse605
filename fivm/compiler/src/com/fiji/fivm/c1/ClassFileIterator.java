/*
 * ClassFileIterator.java
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

import java.io.*;
import java.util.zip.*;

public abstract class ClassFileIterator {
    public abstract void addClass(String name,byte[] bytecode);
    
    public void addClass(byte[] bytecode) {
	addClass(Util.getClassName(bytecode),bytecode);
    }
    
    public void addClassConditionally(String name,byte[] bytecode) {
	if (name.equals(Util.getClassName(bytecode))) {
	    addClass(name,bytecode);
	} else {
	    if (Global.verbosity>=2) {
		Global.log.println("Warning: observed class with name mismatch; filename says "+name+" but the bytecode says "+Util.getClassName(bytecode)+"; ignoring class.");
	    }
	}
    }

    /**
     * Add a Jar, but really a zip, file containing some classes.  Note that if
     * the filename of a class does not match the class name in the bytecode, the
     * class is ignored.
     */
    public void addJar(InputStream in) throws IOException {
	ZipInputStream jin=new ZipInputStream(in);
	for (;;) {
	    ZipEntry ent=jin.getNextEntry();
	    if (ent==null) {
		break;
	    }
	    String name=ent.getName();
	    if (!name.endsWith(".class")) {
		continue;
	    }
	    addClassConditionally(name.substring(0,name.length()-".class".length()),
				  Util.readCompletely(jin));
	}
    }

    public void addClass(String filename) throws IOException {
	long before=System.currentTimeMillis();
	FileInputStream fin=new FileInputStream(filename);
	try {
	    addClass(Util.readCompletely(fin));
	} finally {
	    fin.close();
	}
	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) {
	    Global.log.println("read "+filename+" in "+(after-before)+" ms");
	}
    }
    
    public void addJar(String filename) throws IOException {
	long before=System.currentTimeMillis();
	FileInputStream fin=new FileInputStream(filename);
	try {
	    addJar(fin);
	} finally {
	    fin.close();
	}
	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) {
	    Global.log.println("read "+filename+" in "+(after-before)+" ms");
	}
    }

    public void addClassOrJar(String filename) throws IOException {
	if (Util.isJarOrZip(filename)) {
	    addJar(filename);
	} else if (Util.isClassFile(filename)) {
	    addClass(filename);
	} else {
	    throw new BadBytecode("Given file is neither a Jar nor a class file: "+filename);
	}
    }
}

