/*
 * CLocal.java
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
import java.nio.*;

public class CLocal extends CField {
    
    CType ctype;
    
    CLocal(Basetype type, String name) {
	super(type,name);
	assert type!=Basetype.VOID;
	ctype=type.cType;
    }
    
    CLocal(CType ctype, String name) {
	super(ctype.basetype(),name);
	this.ctype=ctype;
    }
    
    protected CLocal(Basetype type, CType ctype, String name) {
	super(type,name);
	this.ctype=ctype;
    }
    
    public void generateDeclaration(PrintWriter w) {
	w.println("   "+ctype.asCCode()+" "+name+";");
    }
    
    public String toString() {
	return "CLocal["+type+" "+ctype+" "+name+"]";
    }
    
    protected int sizeofImpl() {
	return ctype.sizeofImpl();
    }
    
    public final int sizeof() {
	int result=sizeofImpl();
	assert result>=1;
	return result;
    }
    
    String declaration() {
        StringWriter sw=new StringWriter();
        PrintWriter pw=new PrintWriter(sw);
        generateDeclaration(pw);
        pw.flush();
        return sw.toString();
    }
    
    public int getNioSize() {
        return 1+Util.stringSize(name)+Util.stringSize(declaration());
    }
    
    public void writeTo(ByteBuffer buffer) {
        buffer.put((byte)type.descriptor);
        buffer.putInt(Global.ctypeCoding.codeFor(ctype));
        buffer.putInt(sizeofImpl());
        Util.writeString(buffer,name);
        Util.writeString(buffer,declaration());
    }
    
    public static CLocal readFrom(ByteBuffer buffer) {
        final Basetype type = Basetype.fromChar((char)buffer.get());
        final CType ctype   = Global.ctypeCoding.forCode(buffer.getInt());
        final int sizeof    = buffer.getInt();
        final String name   = Util.readString(buffer);
        final String decl   = Util.readString(buffer);
        
        return new CLocal(type,ctype,name) {
            public void generateDeclaration(PrintWriter w) {
                w.print(decl);
            }
            protected int sizeofImpl() {
        	return sizeof;
            }
        };
    }
    
}


