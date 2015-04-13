/*
 * LocalDataConstant.java
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
import java.util.*;

public class LocalDataConstant extends DataConstant {
    // may be fundamental type or a Linkable, or Padding.  Linkables must be
    // pointer-aligned.  all data constants are typed in C as intptr_t arrays.
    private Object[] data;
    
    public LocalDataConstant(String name,Object[] data) {
	super(name);
	this.data=data;
    }
    
    public LocalDataConstant(String name,List< ? > data) {
	super(name);
	supplyData(data);
    }
    
    public LocalDataConstant(String name) {
	super(name);
    }
    
    public boolean isLocal() { return true; }
    
    private void comma(boolean[] first,
		       PrintWriter w) {
	if (first[0]) {
	    first[0]=false;
	    w.print(" ");
	} else {
	    w.print(",");
	}
    }
    
    private void purge(boolean[] first,
		       ByteBuffer buf,
		       PrintWriter w) {
	while (buf.position()<Global.pointerSize) {
	    buf.put((byte)0);
	}
	assert buf.position()>0;
	assert (buf.position()%Global.pointerSize)==0;
	buf.flip();
	if (Global.pointerSize==4) {
	    while (buf.hasRemaining()) {
		comma(first,w);
		w.println("  INT32_C("+buf.getInt()+")");
	    }
	} else {
	    assert Global.pointerSize==8;
	    comma(first,w);
	    w.println("  INT64_C("+buf.getLong()+")");
	}
	buf.clear();
    }
    
    public void generateDefinition(PrintWriter w) {
	ensureData();
	int size=0;
	for (int i=0;i<data.length;++i) {
            size+=Util.sizeof(data[i]);
	}
	int ptrSize=(size+Global.pointerSize-1)/Global.pointerSize;
	w.println("uintptr_t "+name+"["+ptrSize+"] = {");
	ByteBuffer buf=ByteBuffer.allocate(8);
	buf.order(Global.endianness);
	buf.clear();
	boolean[] first=new boolean[1];
	first[0]=true;
	for (int i=0;i<data.length;++i) {
	    Object o=data[i];
	    if (o instanceof Byte) {
		buf.put((Byte)o);
	    } else if (o instanceof Character) {
		buf.putChar((Character)o);
	    } else if (o instanceof Short) {
		buf.putShort((Short)o);
	    } else if (o instanceof Integer) {
		assert buf.position()==0 || Global.pointerSize==8;
		buf.putInt((Integer)o);
	    } else if (o instanceof Long) {
		assert buf.position()==0;
		buf.putLong((Long)o);
	    } else if (o instanceof Pointerable) {
		assert buf.position()==0;
		comma(first,w);
		w.println("(uintptr_t)("+((Pointerable)o).asCCode()+")");
	    } else if (o instanceof Padding) {
		int p=((Padding)o).size();
		while (p>0) {
		    if (!buf.hasRemaining()) {
			purge(first,buf,w);
		    }
		    buf.put((byte)0);
		    p--;
		}
	    } else {
		throw new Error();
	    }
	    if (buf.position()>=Global.pointerSize) {
		purge(first,buf,w);
	    }
	}
	if (buf.position()>0) {
	    purge(first,buf,w);
	}
	w.println("};");
    }
    
    public LinkableSet subLinkables() {
	ensureData();
	LinkableSet result=new LinkableSet();
	for (Object o : data) {
	    if (o instanceof Linkable) {
		result.add((Linkable)o);
	    }
	    if (o instanceof Pointerable) {
		result.addAll(((Pointerable)o).subLinkables());
	    }
	}
	return result;
    }
    
    protected void supplyData(Object[] data) {
	this.data=data;
    }
    protected void supplyData(List< ? > data) {
	this.data=new Object[data.size()];
	data.toArray(this.data);
    }

    protected void makeData() {
	throw new Error("if you do not initialize data, implement this method");
    }
    
    void ensureData() {
	if (data==null) makeData();
    }
}

