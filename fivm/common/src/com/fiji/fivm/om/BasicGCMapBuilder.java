/*
 * BasicGCMapBuilder.java
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

package com.fiji.fivm.om;

import com.fiji.fivm.*;

public class BasicGCMapBuilder extends GCMapBuilder {
    
    public int computeThinGCMapOffset(OMField f) {
        return f.location();
    }
    
    public int thinMapOffsetToLocation(int offset) {
        return offset;
    }
    
    public < T extends OMConstant > T buildGCMap(ConstantAllocator< T > ca, OMClass c) {
	OMField[] fields=c.omAllLaidOutFields();
	int maxPtrOffset=-1;
	for (OMField f : fields) {
	    if (f.isTraced()) {
		maxPtrOffset=Math.max(maxPtrOffset,computeThinGCMapOffset(f));
	    }
	}
	if (maxPtrOffset/OMData.pointerSize()>(OMData.pointerSize()*8-1)) {
	    // use array of offsets
	    int numPtrs=0;
	    for (OMField f : fields) {
		if (f.isTraced()) {
		    numPtrs++;
		}
	    }
            int[] data=new int[numPtrs+1];
	    int curIdx=0;
	    data[curIdx++]=numPtrs;
	    for (OMField f : fields) {
		if (f.isTraced()) {
		    data[curIdx++]=f.location();
		}
	    }
	    return ca.allocList("gcmap",data);
	} else {
	    // use bitmap
	    long value=Constants.GCM_THIN;
	    for (OMField f : fields) {
		if (f.isTraced()) {
		    value|=(2<<(computeThinGCMapOffset(f)/OMData.pointerSize()));
		}
	    }
	    return ca.makePtrConst(value);
	}
    }
    
    public int[] decodeGCMap(PointerAPI api,int objSize) {
        long value=api.loadPointerLiteral();
        
        int[] result=IntUtil.newBitSet(objSize/OMData.pointerSize());
        
        if ((value&Constants.GCM_THIN)==0) {
            // fat GC map
            
            int len=api.loadInt();
            api.advance(OMData.pointerSize());
            
            for (int i=0;i<len;++i) {
                IntUtil.setBit(result,api.loadInt()/OMData.pointerSize());
                api.advance(OMData.pointerSize());
            }
        } else {
            value>>>=1;
            for (int i=0;i<63;++i,value>>>=1) {
                if ((value&1)!=0) {
                    IntUtil.setBit(result,thinMapOffsetToLocation(i*OMData.pointerSize())/OMData.pointerSize());
                }
            }
        }
        
        return result;
    }
    
    public String toString() {
        if (getClass()==BasicGCMapBuilder.class) {
            return "Basic Thin/Fat GC Map";
        } else {
            return "Customized Thin/Fat GC Map";
        }
    }
}

