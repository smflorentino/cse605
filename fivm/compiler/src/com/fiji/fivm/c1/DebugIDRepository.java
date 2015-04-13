/*
 * DebugIDRepository.java
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

import java.util.ArrayList;
import java.util.HashMap;

public class DebugIDRepository {
    private DebugIDRepository() {}
    
    static ArrayList< DebugIDInfo > debugInfoTable=
	new ArrayList< DebugIDInfo >();
    static HashMap< DebugIDInfo, Integer > debugInfoIDs=
	new HashMap< DebugIDInfo, Integer >();
    
    public static synchronized int id(DebugIDInfo di) {
	Integer result=debugInfoIDs.get(di);
	if (result==null) {
	    assert debugInfoTable.size()==debugInfoIDs.size();
	    result=new Integer(debugInfoTable.size());
	    debugInfoTable.add(di);
	    debugInfoIDs.put(di,result);
	}
	return result;
    }
    
    static Linkable methodRec(VisibleMethod vm) {
        if (vm.shouldExist()) {
            return new RemoteMethodRec(vm);
        } else {
            return new LocalCompactMethodRec(vm);
        }
    }
    
    static int inlinedRecCnt;
    
    static Pointerable methodEncoding(int idx,int rec,DebugInfo di) {
	if (di.isInlined()) {
            inlinedRecCnt++;
            
	    ArrayList<Object> data=new ArrayList<Object>();
	    
	    data.add(new Pointer(((long)di.caller().lineNumber()<<1)|
				 (di.caller().isInlined()?1:0)));
	    data.add(methodEncoding(idx,rec+1,di.caller()));
	    data.add(methodRec(di.origin().origin()));
	    return new LocalDataConstant(
		Global.name+"_InlineMethodRec_"+idx+"_"+rec,
		data);
	} else {
	    return methodRec(di.origin().origin());
	}
    }
    
    public static int size() {
        return debugInfoTable.size();
    }
    
    public static LinkableSet debugTableData() {
	// NOTE: there is really no good reason to do this using linkables,
	// other than I think it's easier than generating the code directly.
	
	int thinLineNumberBits;
        int thinBytecodePCBits;
	int thinRefMapBits;
	int maxThinLineNumber;
        int maxThinBytecodePC;
	
	switch (Global.pointerSize) {
	case 4:
            thinBytecodePCBits=8;
	    thinLineNumberBits=10;
	    thinRefMapBits=12;
	    break;
	case 8:
            thinBytecodePCBits=15;
	    thinLineNumberBits=16;
	    thinRefMapBits=31;
	    break;
	default: throw new Error();
	}
	
	maxThinLineNumber=(1<<thinLineNumberBits)-1;
        maxThinBytecodePC=(1<<thinBytecodePCBits)-1;
	
	LinkableSet set=new LinkableSet();
	
	ArrayList< Object > data=new ArrayList< Object >();
	
	for (int i=0;i<debugInfoTable.size();++i) {
	    DebugIDInfo didi=debugInfoTable.get(i);
	    
	    // encode ln_rm_c
	    if (didi.di().lineNumber()<0 || didi.di().lineNumber()>maxThinLineNumber ||
                didi.di().pc()<0 || didi.di().pc()>=maxThinBytecodePC ||
		Util.lastSetBit(didi.refsLive())>=thinRefMapBits) {
		ArrayList< Object > fatData=new ArrayList< Object >();
		
		fatData.add(didi.di().lineNumber());
                fatData.add(didi.di().pc());
		fatData.add(Util.numBitSetWords(didi.refsLive()));
		Util.bitSetWords(fatData,didi.refsLive());
		
		data.add(
		    new TaggedLink(
			new LocalDataConstant(Global.name+"_FatDebugData_"+i,fatData),
			(1<<0)|
			((didi.di().isInlined()?1:0)<<1)));
	    } else {
		data.add(
		    new Pointer(
			(0l<<0)|
			((didi.di().isInlined()?1l:0l)<<1)|
			((long)Util.bitSetWord(didi.refsLive())<<2)|
			((long)didi.di().lineNumber()<<
                         (Global.pointerSize==4?14:33))|
                        ((long)didi.di().pc()<<
                         (Global.pointerSize==4?24:49))));
	    }
	    
	    // encode the method
            if (didi.di().isPatchPoint()) {
                data.add(new TaggedLink(methodEncoding(i,0,didi.di()),
                                        1 /* MFL_PATCH_POINT */));
            } else {
                data.add(methodEncoding(i,0,didi.di()));
            }
	}
        
        if (Global.verbosity>=1) {
            Global.log.println("Debug table has "+debugInfoTable.size()+" entries.");
            Global.log.println("Generated "+inlinedRecCnt+" inlining records.");
        }
	
	set.add(new LocalDataConstant(CTypesystemReferences.generated_debugTable_name,data));
	
	return set;
    }
}

