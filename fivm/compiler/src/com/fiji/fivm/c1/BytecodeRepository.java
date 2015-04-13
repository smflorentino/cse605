/*
 * BytecodeRepository.java
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

public class BytecodeRepository {
    private BytecodeRepository() {}
    
    static DataBuilder db;
    
    static LinkedHashMap< VisibleClass, Long > offsets=
        new LinkedHashMap< VisibleClass, Long >();
    static long curOffset;
    
    public static void setDataBuilder(DataBuilder db) {
	BytecodeRepository.db=db;
    }
    
    public static synchronized long curOffset() {
        return curOffset;
    }
    
    public static synchronized List< VisibleClass > classesInOrder() {
        return new ArrayList< VisibleClass >(offsets.keySet());
    }
    
    public static synchronized long offsetFor(VisibleClass klass) {
        Long result=offsets.get(klass);
        if (result==null) {
            offsets.put(klass,result=curOffset);
            curOffset+=db.size(klass);
            assert (curOffset&(Global.pointerSize-1))==0;
        }
        return result;
    }
    
    public static Pointerable pointerTo(VisibleClass klass) {
        return new DisplacedLink(CTypesystemReferences.generated_bytecodeTable,Global.allocOffset+offsetFor(klass));
    }
    
    public static DataBuilder getDataBuilder() { return db; }
    
    public static abstract class DataBuilder {
	public abstract Linkable build(String symName);
        public abstract long size(VisibleClass klass);
    }
    
    public static LinkableSet all() {
	LinkableSet result=new LinkableSet();
        result.add(db.build(CTypesystemReferences.generated_bytecodeTable_name));
	return result;
    }
}

