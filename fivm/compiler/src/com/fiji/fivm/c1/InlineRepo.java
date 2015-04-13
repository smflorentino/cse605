/*
 * InlineRepo.java
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

import java.util.*;
import java.io.*;

public class InlineRepo {
    private InlineRepo() {}
    
    static HashMap< VisibleMethod, Code > map=new HashMap< VisibleMethod, Code >();
    
    public static synchronized void putNoCopy(VisibleMethod vm,Code c) {
	map.put(vm,c);
    }
    
    public static void put(VisibleMethod vm, Code c) {
	assert vm.inlineMode()!=InlineMode.NO_INLINE;
        putNoCopy(vm,c.copy());
    }
    
    public static synchronized boolean has(VisibleMethod vm) {
	return map.containsKey(vm);
    }
    
    public static synchronized Collection< Code > codes() {
	return map.values();
    }
    
    public static synchronized Code getNoCopy(VisibleMethod vm) {
	Code result=map.get(vm);
	assert result!=null;
	return result;
    }
    
    public static Code get(VisibleMethod vm) {
	return getNoCopy(vm).copy();
    }
    
    public static synchronized void dump(String filename) throws IOException {
	Util.dumpSortedJNI(filename,map.keySet());
    }
    
    public static synchronized void dumpMustInlines(String filename) throws IOException {
	ArrayList< String > names=new ArrayList< String >();
	for (Map.Entry< VisibleMethod, Code > e : map.entrySet()) {
	    if (e.getValue().getMustInline().shouldInline()) {
		names.add(e.getKey().jniName());
	    }
	}
	Util.dumpSorted(filename,names);
    }
    
    public static synchronized void optimizeInlineables() {
        HashMap< VisibleMethod, Code > newMap=new HashMap< VisibleMethod, Code >();
        
        // FIXME parallelize this
        
        for (Map.Entry< VisibleMethod, Code > entry : map.entrySet()) {
            VisibleMethod vm=entry.getKey();
            Code code=entry.getValue().copy();
            
            long before=System.currentTimeMillis();
            code=InlineFixpoint.doit(
                code,
                new MustInlineFilter(),
                Global.omBlocksMax,
                -1);
            if (Global.verbosity>=2) {
                long after=System.currentTimeMillis();
                Global.log.println("optimized "+code.origin().origin().jniName()+" in "+(after-before)+" ms");
            }
            
            newMap.put(vm,code);
        }
        
        map=newMap;
    }
}


